/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "FragmentedMP4Parser"
#include <utils/Log.h>

#include "include/avc_utils.h"
#include "include/ESDS.h"
#include "include/FragmentedMP4Parser.h"
#include "TrackFragment.h"


#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/Utils.h>


namespace android {

static const char *Fourcc2String(uint32_t fourcc) {
    static char buffer[5];
    buffer[4] = '\0';
    buffer[0] = fourcc >> 24;
    buffer[1] = (fourcc >> 16) & 0xff;
    buffer[2] = (fourcc >> 8) & 0xff;
    buffer[3] = fourcc & 0xff;

    return buffer;
}

static const char *IndentString(size_t n) {
    static const char kSpace[] = "                              ";
    return kSpace + sizeof(kSpace) - 2 * n - 1;
}

// static
const FragmentedMP4Parser::DispatchEntry FragmentedMP4Parser::kDispatchTable[] = {
    { FOURCC('m', 'o', 'o', 'v'), 0, NULL },
    { FOURCC('t', 'r', 'a', 'k'), FOURCC('m', 'o', 'o', 'v'), NULL },
    { FOURCC('u', 'd', 't', 'a'), FOURCC('t', 'r', 'a', 'k'), NULL },
    { FOURCC('u', 'd', 't', 'a'), FOURCC('m', 'o', 'o', 'v'), NULL },
    { FOURCC('m', 'e', 't', 'a'), FOURCC('u', 'd', 't', 'a'), NULL },
    { FOURCC('i', 'l', 's', 't'), FOURCC('m', 'e', 't', 'a'), NULL },

    { FOURCC('t', 'k', 'h', 'd'), FOURCC('t', 'r', 'a', 'k'),
        &FragmentedMP4Parser::parseTrackHeader
    },

    { FOURCC('m', 'v', 'e', 'x'), FOURCC('m', 'o', 'o', 'v'), NULL },

    { FOURCC('t', 'r', 'e', 'x'), FOURCC('m', 'v', 'e', 'x'),
        &FragmentedMP4Parser::parseTrackExtends
    },

    { FOURCC('e', 'd', 't', 's'), FOURCC('t', 'r', 'a', 'k'), NULL },
    { FOURCC('m', 'd', 'i', 'a'), FOURCC('t', 'r', 'a', 'k'), NULL },

    { FOURCC('m', 'd', 'h', 'd'), FOURCC('m', 'd', 'i', 'a'),
        &FragmentedMP4Parser::parseMediaHeader
    },

    { FOURCC('h', 'd', 'l', 'r'), FOURCC('m', 'd', 'i', 'a'),
        &FragmentedMP4Parser::parseMediaHandler
    },

    { FOURCC('m', 'i', 'n', 'f'), FOURCC('m', 'd', 'i', 'a'), NULL },
    { FOURCC('d', 'i', 'n', 'f'), FOURCC('m', 'i', 'n', 'f'), NULL },
    { FOURCC('s', 't', 'b', 'l'), FOURCC('m', 'i', 'n', 'f'), NULL },
    { FOURCC('s', 't', 's', 'd'), FOURCC('s', 't', 'b', 'l'), NULL },

    { FOURCC('s', 't', 's', 'z'), FOURCC('s', 't', 'b', 'l'),
        &FragmentedMP4Parser::parseSampleSizes },

    { FOURCC('s', 't', 'z', '2'), FOURCC('s', 't', 'b', 'l'),
        &FragmentedMP4Parser::parseCompactSampleSizes },

    { FOURCC('s', 't', 's', 'c'), FOURCC('s', 't', 'b', 'l'),
        &FragmentedMP4Parser::parseSampleToChunk },

    { FOURCC('s', 't', 'c', 'o'), FOURCC('s', 't', 'b', 'l'),
        &FragmentedMP4Parser::parseChunkOffsets },

    { FOURCC('c', 'o', '6', '4'), FOURCC('s', 't', 'b', 'l'),
        &FragmentedMP4Parser::parseChunkOffsets64 },

    { FOURCC('a', 'v', 'c', 'C'), FOURCC('a', 'v', 'c', '1'),
        &FragmentedMP4Parser::parseAVCCodecSpecificData },

    { FOURCC('e', 's', 'd', 's'), FOURCC('m', 'p', '4', 'a'),
        &FragmentedMP4Parser::parseESDSCodecSpecificData },

    { FOURCC('e', 's', 'd', 's'), FOURCC('m', 'p', '4', 'v'),
        &FragmentedMP4Parser::parseESDSCodecSpecificData },

    { FOURCC('m', 'd', 'a', 't'), 0, &FragmentedMP4Parser::parseMediaData },

    { FOURCC('m', 'o', 'o', 'f'), 0, NULL },
    { FOURCC('t', 'r', 'a', 'f'), FOURCC('m', 'o', 'o', 'f'), NULL },

    { FOURCC('t', 'f', 'h', 'd'), FOURCC('t', 'r', 'a', 'f'),
        &FragmentedMP4Parser::parseTrackFragmentHeader
    },
    { FOURCC('t', 'r', 'u', 'n'), FOURCC('t', 'r', 'a', 'f'),
        &FragmentedMP4Parser::parseTrackFragmentRun
    },

    { FOURCC('m', 'f', 'r', 'a'), 0, NULL },

    { FOURCC('s', 'i', 'd', 'x'), 0, &FragmentedMP4Parser::parseSegmentIndex },
};

struct FileSource : public FragmentedMP4Parser::Source {
    FileSource(const char *filename)
        : mFile(fopen(filename, "rb")) {
            CHECK(mFile != NULL);
        }

    virtual ~FileSource() {
        fclose(mFile);
    }

    virtual ssize_t readAt(off64_t offset, void *data, size_t size) {
        fseek(mFile, offset, SEEK_SET);
        return fread(data, 1, size, mFile);
    }

    virtual bool isSeekable() {
        return true;
    }

    private:
    FILE *mFile;

    DISALLOW_EVIL_CONSTRUCTORS(FileSource);
};

struct ReadTracker : public RefBase {
    ReadTracker(off64_t size) {
        allocSize = 1 + size / 8192; // 1 bit per kilobyte
        bitmap = (char*) calloc(1, allocSize);
    }
    virtual ~ReadTracker() {
        dumpToLog();
        free(bitmap);
    }
    void mark(off64_t offset, size_t size) {
        int firstbit = offset / 1024;
        int lastbit = (offset + size - 1) / 1024;
        for (int i = firstbit; i <= lastbit; i++) {
            bitmap[i/8] |= (0x80 >> (i & 7));
        }
    }

 private:
    void dumpToLog() {
        // 96 chars per line, each char represents one kilobyte, 1 kb per bit
        int numlines = allocSize / 12;
        char buf[97];
        char *cur = bitmap;
        for (int i = 0; i < numlines; i++ && cur) {
            for (int j = 0; j < 12; j++) {
                for (int k = 0; k < 8; k++) {
                    buf[(j * 8) + k] = (*cur & (0x80 >> k)) ? 'X' : '.';
                }
                cur++;
            }
            buf[96] = '\0';
            ALOGI("%5dk: %s", i * 96, buf);
        }
    }

    size_t allocSize;
    char *bitmap;
};

struct DataSourceSource : public FragmentedMP4Parser::Source {
    DataSourceSource(sp<DataSource> &source)
        : mDataSource(source) {
            CHECK(mDataSource != NULL);
#if 0
            off64_t size;
            if (source->getSize(&size) == OK) {
                mReadTracker = new ReadTracker(size);
            } else {
                ALOGE("couldn't get data source size");
            }
#endif
        }

    virtual ssize_t readAt(off64_t offset, void *data, size_t size) {
        if (mReadTracker != NULL) {
            mReadTracker->mark(offset, size);
        }
        return mDataSource->readAt(offset, data, size);
    }

    virtual bool isSeekable() {
        return true;
    }

    private:
    sp<DataSource> mDataSource;
    sp<ReadTracker> mReadTracker;

    DISALLOW_EVIL_CONSTRUCTORS(DataSourceSource);
};

FragmentedMP4Parser::FragmentedMP4Parser()
    : mBufferPos(0),
      mSuspended(false),
      mDoneWithMoov(false),
      mFirstMoofOffset(0),
      mFinalResult(OK) {
}

FragmentedMP4Parser::~FragmentedMP4Parser() {
}

void FragmentedMP4Parser::start(const char *filename) {
    sp<AMessage> msg = new AMessage(kWhatStart, id());
    msg->setObject("source", new FileSource(filename));
    msg->post();
    ALOGV("Parser::start(%s)", filename);
}

void FragmentedMP4Parser::start(const sp<Source> &source) {
    sp<AMessage> msg = new AMessage(kWhatStart, id());
    msg->setObject("source", source);
    msg->post();
    ALOGV("Parser::start(Source)");
}

void FragmentedMP4Parser::start(sp<DataSource> &source) {
    sp<AMessage> msg = new AMessage(kWhatStart, id());
    msg->setObject("source", new DataSourceSource(source));
    msg->post();
    ALOGV("Parser::start(DataSource)");
}

sp<AMessage> FragmentedMP4Parser::getFormat(bool audio, bool synchronous) {

    while (true) {
        bool moovDone = mDoneWithMoov;
        sp<AMessage> msg = new AMessage(kWhatGetFormat, id());
        msg->setInt32("audio", audio);

        sp<AMessage> response;
        status_t err = msg->postAndAwaitResponse(&response);

        if (err != OK) {
            ALOGV("getFormat post failed: %d", err);
            return NULL;
        }

        if (response->findInt32("err", &err) && err != OK) {
            if (synchronous && err == -EWOULDBLOCK && !moovDone) {
                resumeIfNecessary();
                ALOGV("@getFormat parser not ready yet, retrying");
                usleep(10000);
                continue;
            }
            ALOGV("getFormat failed: %d", err);
            return NULL;
        }

        sp<AMessage> format;
        CHECK(response->findMessage("format", &format));

        ALOGV("returning format %s", format->debugString().c_str());
        return format;
    }
}

status_t FragmentedMP4Parser::seekTo(bool wantAudio, int64_t timeUs) {
    sp<AMessage> msg = new AMessage(kWhatSeekTo, id());
    msg->setInt32("audio", wantAudio);
    msg->setInt64("position", timeUs);

    sp<AMessage> response;
    status_t err = msg->postAndAwaitResponse(&response);
    return err;
}

bool FragmentedMP4Parser::isSeekable() const {
    while (mFirstMoofOffset == 0 && mFinalResult == OK) {
        usleep(10000);
    }
    bool seekable = mSource->isSeekable();
    for (size_t i = 0; seekable && i < mTracks.size(); i++) {
        const TrackInfo *info = &mTracks.valueAt(i);
        seekable &= !info->mSidx.empty();
    }
    return seekable;
}

status_t FragmentedMP4Parser::onSeekTo(bool wantAudio, int64_t position) {
    status_t err = -EINVAL;
    ssize_t trackIndex = findTrack(wantAudio);
    if (trackIndex < 0) {
        err = trackIndex;
    } else {
        TrackInfo *info = &mTracks.editValueAt(trackIndex);

        int numSidxEntries = info->mSidx.size();
        int64_t totalTime = 0;
        off_t totalOffset = mFirstMoofOffset;
        for (int i = 0; i < numSidxEntries; i++) {
            const SidxEntry *se = &info->mSidx[i];
            if (totalTime + se->mDurationUs > position) {
                mBuffer->setRange(0,0);
                mBufferPos = totalOffset;
                if (mFinalResult == ERROR_END_OF_STREAM) {
                    mFinalResult = OK;
                    mSuspended = true; // force resume
                    resumeIfNecessary();
                }
                info->mFragments.clear();
                info->mDecodingTime = totalTime * info->mMediaTimeScale / 1000000ll;
                return OK;
            }
            totalTime += se->mDurationUs;
            totalOffset += se->mSize;
        }
    }
    ALOGV("seekTo out of range");
    return err;
}

status_t FragmentedMP4Parser::dequeueAccessUnit(bool audio, sp<ABuffer> *accessUnit,
                                                bool synchronous) {

    while (true) {
        sp<AMessage> msg = new AMessage(kWhatDequeueAccessUnit, id());
        msg->setInt32("audio", audio);

        sp<AMessage> response;
        status_t err = msg->postAndAwaitResponse(&response);

        if (err != OK) {
            ALOGV("dequeue fail 1: %d", err);
            return err;
        }

        if (response->findInt32("err", &err) && err != OK) {
            if (synchronous && err == -EWOULDBLOCK) {
                resumeIfNecessary();
                ALOGV("Parser not ready yet, retrying");
                usleep(10000);
                continue;
            }
            ALOGV("dequeue fail 2: %d, %d", err, synchronous);
            return err;
        }

        CHECK(response->findBuffer("accessUnit", accessUnit));

        return OK;
    }
}

ssize_t FragmentedMP4Parser::findTrack(bool wantAudio) const {
    for (size_t i = 0; i < mTracks.size(); ++i) {
        const TrackInfo *info = &mTracks.valueAt(i);

        bool isAudio =
            info->mMediaHandlerType == FOURCC('s', 'o', 'u', 'n');

        bool isVideo =
            info->mMediaHandlerType == FOURCC('v', 'i', 'd', 'e');

        if ((wantAudio && isAudio) || (!wantAudio && !isAudio)) {
            if (info->mSampleDescs.empty()) {
                break;
            }

            return i;
        }
    }

    return -EWOULDBLOCK;
}

void FragmentedMP4Parser::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatStart:
        {
            sp<RefBase> obj;
            CHECK(msg->findObject("source", &obj));

            mSource = static_cast<Source *>(obj.get());

            mBuffer = new ABuffer(512 * 1024);
            mBuffer->setRange(0, 0);

            enter(0ll, 0, 0);

            (new AMessage(kWhatProceed, id()))->post();
            break;
        }

        case kWhatProceed:
        {
            CHECK(!mSuspended);

            status_t err = onProceed();

            if (err == OK) {
                if (!mSuspended) {
                    msg->post();
                }
            } else if (err != -EAGAIN) {
                ALOGE("onProceed returned error %d", err);
            }

            break;
        }

        case kWhatReadMore:
        {
            size_t needed;
            CHECK(msg->findSize("needed", &needed));

            memmove(mBuffer->base(), mBuffer->data(), mBuffer->size());
            mBufferPos += mBuffer->offset();
            mBuffer->setRange(0, mBuffer->size());

            size_t maxBytesToRead = mBuffer->capacity() - mBuffer->size();

            if (maxBytesToRead < needed) {
                ALOGV("resizing buffer.");

                sp<ABuffer> newBuffer =
                    new ABuffer((mBuffer->size() + needed + 1023) & ~1023);
                memcpy(newBuffer->data(), mBuffer->data(), mBuffer->size());
                newBuffer->setRange(0, mBuffer->size());

                mBuffer = newBuffer;
                maxBytesToRead = mBuffer->capacity() - mBuffer->size();
            }

            CHECK_GE(maxBytesToRead, needed);

            ssize_t n = mSource->readAt(
                    mBufferPos + mBuffer->size(),
                    mBuffer->data() + mBuffer->size(), needed);

            if (n < (ssize_t)needed) {
                ALOGV("Reached EOF when reading %d @ %d + %d", needed, mBufferPos, mBuffer->size());
                if (n < 0) {
                    mFinalResult = n;
                } else if (n == 0) {
                    mFinalResult = ERROR_END_OF_STREAM;
                } else {
                    mFinalResult = ERROR_IO;
                }
            } else {
                mBuffer->setRange(0, mBuffer->size() + n);
                (new AMessage(kWhatProceed, id()))->post();
            }

            break;
        }

        case kWhatGetFormat:
        {
            int32_t wantAudio;
            CHECK(msg->findInt32("audio", &wantAudio));

            status_t err = -EWOULDBLOCK;
            sp<AMessage> response = new AMessage;

            ssize_t trackIndex = findTrack(wantAudio);

            if (trackIndex < 0) {
                err = trackIndex;
            } else {
                TrackInfo *info = &mTracks.editValueAt(trackIndex);

                sp<AMessage> format = info->mSampleDescs.itemAt(0).mFormat;
                if (info->mSidxDuration) {
                    format->setInt64("durationUs", info->mSidxDuration);
                } else {
                    // this is probably going to be zero. Oh well...
                    format->setInt64("durationUs",
                                     1000000ll * info->mDuration / info->mMediaTimeScale);
                }
                response->setMessage(
                        "format", format);

                err = OK;
            }

            response->setInt32("err", err);

            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));

            response->postReply(replyID);
            break;
        }

        case kWhatDequeueAccessUnit:
        {
            int32_t wantAudio;
            CHECK(msg->findInt32("audio", &wantAudio));

            status_t err = -EWOULDBLOCK;
            sp<AMessage> response = new AMessage;

            ssize_t trackIndex = findTrack(wantAudio);

            if (trackIndex < 0) {
                err = trackIndex;
            } else {
                sp<ABuffer> accessUnit;
                err = onDequeueAccessUnit(trackIndex, &accessUnit);

                if (err == OK) {
                    response->setBuffer("accessUnit", accessUnit);
                }
            }

            response->setInt32("err", err);

            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));

            response->postReply(replyID);
            break;
        }

        case kWhatSeekTo:
        {
            ALOGV("kWhatSeekTo");
            int32_t wantAudio;
            CHECK(msg->findInt32("audio", &wantAudio));
            int64_t position;
            CHECK(msg->findInt64("position", &position));

            status_t err = -EWOULDBLOCK;
            sp<AMessage> response = new AMessage;

            ssize_t trackIndex = findTrack(wantAudio);

            if (trackIndex < 0) {
                err = trackIndex;
            } else {
                err = onSeekTo(wantAudio, position);
            }
            response->setInt32("err", err);
            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));
            response->postReply(replyID);
            break;
        }
        default:
            TRESPASS();
    }
}

status_t FragmentedMP4Parser::onProceed() {
    status_t err;

    if ((err = need(8)) != OK) {
        return err;
    }

    uint64_t size = readU32(0);
    uint32_t type = readU32(4);

    size_t offset = 8;

    if (size == 1) {
        if ((err = need(16)) != OK) {
            return err;
        }

        size = readU64(offset);
        offset += 8;
    }

    uint8_t userType[16];

    if (type == FOURCC('u', 'u', 'i', 'd')) {
        if ((err = need(offset + 16)) != OK) {
            return err;
        }

        memcpy(userType, mBuffer->data() + offset, 16);
        offset += 16;
    }

    CHECK(!mStack.isEmpty());
    uint32_t ptype = mStack.itemAt(mStack.size() - 1).mType;

    static const size_t kNumDispatchers =
        sizeof(kDispatchTable) / sizeof(kDispatchTable[0]);

    size_t i;
    for (i = 0; i < kNumDispatchers; ++i) {
        if (kDispatchTable[i].mType == type
                && kDispatchTable[i].mParentType == ptype) {
            break;
        }
    }

    // SampleEntry boxes are container boxes that start with a variable
    // amount of data depending on the media handler type.
    // We don't look inside 'hint' type SampleEntry boxes.

    bool isSampleEntryBox =
        (ptype == FOURCC('s', 't', 's', 'd'))
        && editTrack(mCurrentTrackID)->mMediaHandlerType
        != FOURCC('h', 'i', 'n', 't');

    if ((i < kNumDispatchers && kDispatchTable[i].mHandler == 0)
            || isSampleEntryBox || ptype == FOURCC('i', 'l', 's', 't')) {
        // This is a container box.
        if (type == FOURCC('m', 'o', 'o', 'f')) {
            if (mFirstMoofOffset == 0) {
                ALOGV("first moof @ %08x", mBufferPos + offset);
                mFirstMoofOffset = mBufferPos + offset - 8; // point at the size
            }
        }
        if (type == FOURCC('m', 'e', 't', 'a')) {
            if ((err = need(offset + 4)) < OK) {
                return err;
            }

            if (readU32(offset) != 0) {
                return -EINVAL;
            }

            offset += 4;
        } else if (type == FOURCC('s', 't', 's', 'd')) {
            if ((err = need(offset + 8)) < OK) {
                return err;
            }

            if (readU32(offset) != 0) {
                return -EINVAL;
            }

            if (readU32(offset + 4) == 0) {
                // We need at least some entries.
                return -EINVAL;
            }

            offset += 8;
        } else if (isSampleEntryBox) {
            size_t headerSize;

            switch (editTrack(mCurrentTrackID)->mMediaHandlerType) {
                case FOURCC('v', 'i', 'd', 'e'):
                {
                    // 8 bytes SampleEntry + 70 bytes VisualSampleEntry
                    headerSize = 78;
                    break;
                }

                case FOURCC('s', 'o', 'u', 'n'):
                {
                    // 8 bytes SampleEntry + 20 bytes AudioSampleEntry
                    headerSize = 28;
                    break;
                }

                case FOURCC('m', 'e', 't', 'a'):
                {
                    headerSize = 8;  // 8 bytes SampleEntry
                    break;
                }

                default:
                    TRESPASS();
            }

            if (offset + headerSize > size) {
                return -EINVAL;
            }

            if ((err = need(offset + headerSize)) != OK) {
                return err;
            }

            switch (editTrack(mCurrentTrackID)->mMediaHandlerType) {
                case FOURCC('v', 'i', 'd', 'e'):
                {
                    err = parseVisualSampleEntry(
                            type, offset, offset + headerSize);
                    break;
                }

                case FOURCC('s', 'o', 'u', 'n'):
                {
                    err = parseAudioSampleEntry(
                            type, offset, offset + headerSize);
                    break;
                }

                case FOURCC('m', 'e', 't', 'a'):
                {
                    err = OK;
                    break;
                }

                default:
                    TRESPASS();
            }

            if (err != OK) {
                return err;
            }

            offset += headerSize;
        }

        skip(offset);

        ALOGV("%sentering box of type '%s'",
                IndentString(mStack.size()), Fourcc2String(type));

        enter(mBufferPos - offset, type, size - offset);
    } else {
        if (!fitsContainer(size)) {
            return -EINVAL;
        }

        if (i < kNumDispatchers && kDispatchTable[i].mHandler != 0) {
            // We have a handler for this box type.

            if ((err = need(size)) != OK) {
                return err;
            }

            ALOGV("%sparsing box of type '%s'",
                    IndentString(mStack.size()), Fourcc2String(type));

            if ((err = (this->*kDispatchTable[i].mHandler)(
                            type, offset, size)) != OK) {
                return err;
            }
        } else {
            // Unknown box type

            ALOGV("%sskipping box of type '%s', size %llu",
                    IndentString(mStack.size()),
                    Fourcc2String(type), size);

        }

        skip(size);
    }

    return OK;
}

// static
int FragmentedMP4Parser::CompareSampleLocation(
        const SampleInfo &sample, const MediaDataInfo &mdatInfo) {
    if (sample.mOffset + sample.mSize < mdatInfo.mOffset) {
        return -1;
    }

    if (sample.mOffset >= mdatInfo.mOffset + mdatInfo.mBuffer->size()) {
        return 1;
    }

    // Otherwise make sure the sample is completely contained within this
    // media data block.

    CHECK_GE(sample.mOffset, mdatInfo.mOffset);

    CHECK_LE(sample.mOffset + sample.mSize,
             mdatInfo.mOffset + mdatInfo.mBuffer->size());

    return 0;
}

void FragmentedMP4Parser::resumeIfNecessary() {
    if (!mSuspended) {
        return;
    }

    ALOGV("resuming.");

    mSuspended = false;
    (new AMessage(kWhatProceed, id()))->post();
}

status_t FragmentedMP4Parser::getSample(
        TrackInfo *info, sp<TrackFragment> *fragment, SampleInfo *sampleInfo) {
    for (;;) {
        if (info->mFragments.empty()) {
            if (mFinalResult != OK) {
                return mFinalResult;
            }

            resumeIfNecessary();
            return -EWOULDBLOCK;
        }

        *fragment = *info->mFragments.begin();

        status_t err = (*fragment)->getSample(sampleInfo);

        if (err == OK) {
            return OK;
        } else if (err != ERROR_END_OF_STREAM) {
            return err;
        }

        // Really, end of this fragment...

        info->mFragments.erase(info->mFragments.begin());
    }
}

status_t FragmentedMP4Parser::onDequeueAccessUnit(
        size_t trackIndex, sp<ABuffer> *accessUnit) {
    TrackInfo *info = &mTracks.editValueAt(trackIndex);

    sp<TrackFragment> fragment;
    SampleInfo sampleInfo;
    status_t err = getSample(info, &fragment, &sampleInfo);

    if (err == -EWOULDBLOCK) {
        resumeIfNecessary();
        return err;
    } else if (err != OK) {
        return err;
    }

    err = -EWOULDBLOCK;

    bool checkDroppable = false;

    for (size_t i = 0; i < mMediaData.size(); ++i) {
        const MediaDataInfo &mdatInfo = mMediaData.itemAt(i);

        int cmp = CompareSampleLocation(sampleInfo, mdatInfo);

        if (cmp < 0 && !mSource->isSeekable()) {
            return -EPIPE;
        } else if (cmp == 0) {
            if (i > 0) {
                checkDroppable = true;
            }

            err = makeAccessUnit(info, sampleInfo, mdatInfo, accessUnit);
            break;
        }
    }

    if (err != OK) {
        return err;
    }

    fragment->advance();

    if (!mMediaData.empty() && checkDroppable) {
        size_t numDroppable = 0;
        bool done = false;

        // XXX FIXME: if one of the tracks is not advanced (e.g. if you play an audio+video
        // file with sf2), then mMediaData will not be pruned and keeps growing
        for (size_t i = 0; !done && i < mMediaData.size(); ++i) {
            const MediaDataInfo &mdatInfo = mMediaData.itemAt(i);

            for (size_t j = 0; j < mTracks.size(); ++j) {
                TrackInfo *info = &mTracks.editValueAt(j);

                sp<TrackFragment> fragment;
                SampleInfo sampleInfo;
                err = getSample(info, &fragment, &sampleInfo);

                if (err != OK) {
                    done = true;
                    break;
                }

                int cmp = CompareSampleLocation(sampleInfo, mdatInfo);

                if (cmp <= 0) {
                    done = true;
                    break;
                }
            }

            if (!done) {
                ++numDroppable;
            }
        }

        if (numDroppable > 0) {
            mMediaData.removeItemsAt(0, numDroppable);

            if (mMediaData.size() < 5) {
                resumeIfNecessary();
            }
        }
    }

    return err;
}

static size_t parseNALSize(size_t nalLengthSize, const uint8_t *data) {
    switch (nalLengthSize) {
        case 1:
            return *data;
        case 2:
            return U16_AT(data);
        case 3:
            return ((size_t)data[0] << 16) | U16_AT(&data[1]);
        case 4:
            return U32_AT(data);
    }

    // This cannot happen, mNALLengthSize springs to life by adding 1 to
    // a 2-bit integer.
    TRESPASS();

    return 0;
}

status_t FragmentedMP4Parser::makeAccessUnit(
        TrackInfo *info,
        const SampleInfo &sample,
        const MediaDataInfo &mdatInfo,
        sp<ABuffer> *accessUnit) {
    if (sample.mSampleDescIndex < 1
            || sample.mSampleDescIndex > info->mSampleDescs.size()) {
        return ERROR_MALFORMED;
    }

    int64_t presentationTimeUs =
        1000000ll * sample.mPresentationTime / info->mMediaTimeScale;

    const SampleDescription &sampleDesc =
        info->mSampleDescs.itemAt(sample.mSampleDescIndex - 1);

    size_t nalLengthSize;
    if (!sampleDesc.mFormat->findSize("nal-length-size", &nalLengthSize)) {
        *accessUnit = new ABuffer(sample.mSize);

        memcpy((*accessUnit)->data(),
               mdatInfo.mBuffer->data() + (sample.mOffset - mdatInfo.mOffset),
               sample.mSize);

        (*accessUnit)->meta()->setInt64("timeUs", presentationTimeUs);
        if (IsIDR(*accessUnit)) {
            (*accessUnit)->meta()->setInt32("is-sync-frame", 1);
        }

        return OK;
    }

    const uint8_t *srcPtr =
        mdatInfo.mBuffer->data() + (sample.mOffset - mdatInfo.mOffset);

    for (int i = 0; i < 2 ; ++i) {
        size_t srcOffset = 0;
        size_t dstOffset = 0;

        while (srcOffset < sample.mSize) {
            if (srcOffset + nalLengthSize > sample.mSize) {
                return ERROR_MALFORMED;
            }

            size_t nalSize = parseNALSize(nalLengthSize, &srcPtr[srcOffset]);
            srcOffset += nalLengthSize;

            if (srcOffset + nalSize > sample.mSize) {
                return ERROR_MALFORMED;
            }

            if (i == 1) {
                memcpy((*accessUnit)->data() + dstOffset,
                       "\x00\x00\x00\x01",
                       4);

                memcpy((*accessUnit)->data() + dstOffset + 4,
                       srcPtr + srcOffset,
                       nalSize);
            }

            srcOffset += nalSize;
            dstOffset += nalSize + 4;
        }

        if (i == 0) {
            (*accessUnit) = new ABuffer(dstOffset);
            (*accessUnit)->meta()->setInt64(
                    "timeUs", presentationTimeUs);
        }
    }
    if (IsIDR(*accessUnit)) {
        (*accessUnit)->meta()->setInt32("is-sync-frame", 1);
    }

    return OK;
}

status_t FragmentedMP4Parser::need(size_t size) {
    if (!fitsContainer(size)) {
        return -EINVAL;
    }

    if (size <= mBuffer->size()) {
        return OK;
    }

    sp<AMessage> msg = new AMessage(kWhatReadMore, id());
    msg->setSize("needed", size - mBuffer->size());
    msg->post();

    // ALOGV("need(%d) returning -EAGAIN, only have %d", size, mBuffer->size());

    return -EAGAIN;
}

void FragmentedMP4Parser::enter(off64_t offset, uint32_t type, uint64_t size) {
    Container container;
    container.mOffset = offset;
    container.mType = type;
    container.mExtendsToEOF = (size == 0);
    container.mBytesRemaining = size;

    mStack.push(container);
}

bool FragmentedMP4Parser::fitsContainer(uint64_t size) const {
    CHECK(!mStack.isEmpty());
    const Container &container = mStack.itemAt(mStack.size() - 1);

    return container.mExtendsToEOF || size <= container.mBytesRemaining;
}

uint16_t FragmentedMP4Parser::readU16(size_t offset) {
    CHECK_LE(offset + 2, mBuffer->size());

    const uint8_t *ptr = mBuffer->data() + offset;
    return (ptr[0] << 8) | ptr[1];
}

uint32_t FragmentedMP4Parser::readU32(size_t offset) {
    CHECK_LE(offset + 4, mBuffer->size());

    const uint8_t *ptr = mBuffer->data() + offset;
    return (ptr[0] << 24) | (ptr[1] << 16) | (ptr[2] << 8) | ptr[3];
}

uint64_t FragmentedMP4Parser::readU64(size_t offset) {
    return (((uint64_t)readU32(offset)) << 32) | readU32(offset + 4);
}

void FragmentedMP4Parser::skip(off_t distance) {
    CHECK(!mStack.isEmpty());
    for (size_t i = mStack.size(); i-- > 0;) {
        Container *container = &mStack.editItemAt(i);
        if (!container->mExtendsToEOF) {
            CHECK_LE(distance, (off_t)container->mBytesRemaining);

            container->mBytesRemaining -= distance;

            if (container->mBytesRemaining == 0) {
                ALOGV("%sleaving box of type '%s'",
                        IndentString(mStack.size() - 1),
                        Fourcc2String(container->mType));

#if 0
                if (container->mType == FOURCC('s', 't', 's', 'd')) {
                    TrackInfo *trackInfo = editTrack(mCurrentTrackID);
                    for (size_t i = 0;
                            i < trackInfo->mSampleDescs.size(); ++i) {
                        ALOGI("format #%d: %s",
                              i,
                              trackInfo->mSampleDescs.itemAt(i)
                                .mFormat->debugString().c_str());
                    }
                }
#endif

                if (container->mType == FOURCC('s', 't', 'b', 'l')) {
                    TrackInfo *trackInfo = editTrack(mCurrentTrackID);

                    trackInfo->mStaticFragment->signalCompletion();

                    CHECK(trackInfo->mFragments.empty());
                    trackInfo->mFragments.push_back(trackInfo->mStaticFragment);
                    trackInfo->mStaticFragment.clear();
                } else if (container->mType == FOURCC('t', 'r', 'a', 'f')) {
                    TrackInfo *trackInfo =
                        editTrack(mTrackFragmentHeaderInfo.mTrackID);

                    const sp<TrackFragment> &fragment =
                        *--trackInfo->mFragments.end();

                    static_cast<DynamicTrackFragment *>(
                            fragment.get())->signalCompletion();
                } else if (container->mType == FOURCC('m', 'o', 'o', 'v')) {
                    mDoneWithMoov = true;
                }

                container = NULL;
                mStack.removeItemsAt(i);
            }
        }
    }

    if (distance < (off_t)mBuffer->size()) {
        mBuffer->setRange(mBuffer->offset() + distance, mBuffer->size() - distance);
        mBufferPos += distance;
        return;
    }

    mBuffer->setRange(0, 0);
    mBufferPos += distance;
}

status_t FragmentedMP4Parser::parseTrackHeader(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 4 > size) {
        return -EINVAL;
    }

    uint32_t flags = readU32(offset);

    uint32_t version = flags >> 24;
    flags &= 0xffffff;

    uint32_t trackID;
    uint64_t duration;

    if (version == 1) {
        if (offset + 36 > size) {
            return -EINVAL;
        }

        trackID = readU32(offset + 20);
        duration = readU64(offset + 28);

        offset += 36;
    } else if (version == 0) {
        if (offset + 24 > size) {
            return -EINVAL;
        }

        trackID = readU32(offset + 12);
        duration = readU32(offset + 20);

        offset += 24;
    } else {
        return -EINVAL;
    }

    TrackInfo *info = editTrack(trackID, true /* createIfNecessary */);
    info->mFlags = flags;
    info->mDuration = duration;
    if (info->mDuration == 0xffffffff) {
        // ffmpeg sets this to -1, which is incorrect.
        info->mDuration = 0;
    }

    info->mStaticFragment = new StaticTrackFragment;

    mCurrentTrackID = trackID;

    return OK;
}

status_t FragmentedMP4Parser::parseMediaHeader(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 4 > size) {
        return -EINVAL;
    }

    uint32_t versionAndFlags = readU32(offset);

    if (versionAndFlags & 0xffffff) {
        return ERROR_MALFORMED;
    }

    uint32_t version = versionAndFlags >> 24;

    TrackInfo *info = editTrack(mCurrentTrackID);

    if (version == 1) {
        if (offset + 4 + 32 > size) {
            return -EINVAL;
        }
        info->mMediaTimeScale = U32_AT(mBuffer->data() + offset + 20);
    } else if (version == 0) {
        if (offset + 4 + 20 > size) {
            return -EINVAL;
        }
        info->mMediaTimeScale = U32_AT(mBuffer->data() + offset + 12);
    } else {
        return ERROR_MALFORMED;
    }

    return OK;
}

status_t FragmentedMP4Parser::parseMediaHandler(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 12 > size) {
        return -EINVAL;
    }

    if (readU32(offset) != 0) {
        return -EINVAL;
    }

    uint32_t handlerType = readU32(offset + 8);

    switch (handlerType) {
        case FOURCC('v', 'i', 'd', 'e'):
        case FOURCC('s', 'o', 'u', 'n'):
        case FOURCC('h', 'i', 'n', 't'):
        case FOURCC('m', 'e', 't', 'a'):
            break;

        default:
            return -EINVAL;
    }

    editTrack(mCurrentTrackID)->mMediaHandlerType = handlerType;

    return OK;
}

status_t FragmentedMP4Parser::parseVisualSampleEntry(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 78 > size) {
        return -EINVAL;
    }

    TrackInfo *trackInfo = editTrack(mCurrentTrackID);

    trackInfo->mSampleDescs.push();
    SampleDescription *sampleDesc =
        &trackInfo->mSampleDescs.editItemAt(
                trackInfo->mSampleDescs.size() - 1);

    sampleDesc->mType = type;
    sampleDesc->mDataRefIndex = readU16(offset + 6);

    sp<AMessage> format = new AMessage;

    switch (type) {
        case FOURCC('a', 'v', 'c', '1'):
            format->setString("mime", MEDIA_MIMETYPE_VIDEO_AVC);
            break;
        case FOURCC('m', 'p', '4', 'v'):
            format->setString("mime", MEDIA_MIMETYPE_VIDEO_MPEG4);
            break;
        case FOURCC('s', '2', '6', '3'):
        case FOURCC('h', '2', '6', '3'):
        case FOURCC('H', '2', '6', '3'):
            format->setString("mime", MEDIA_MIMETYPE_VIDEO_H263);
            break;
        default:
            format->setString("mime", "application/octet-stream");
            break;
    }

    format->setInt32("width", readU16(offset + 8 + 16));
    format->setInt32("height", readU16(offset + 8 + 18));

    sampleDesc->mFormat = format;

    return OK;
}

status_t FragmentedMP4Parser::parseAudioSampleEntry(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 28 > size) {
        return -EINVAL;
    }

    TrackInfo *trackInfo = editTrack(mCurrentTrackID);

    trackInfo->mSampleDescs.push();
    SampleDescription *sampleDesc =
        &trackInfo->mSampleDescs.editItemAt(
                trackInfo->mSampleDescs.size() - 1);

    sampleDesc->mType = type;
    sampleDesc->mDataRefIndex = readU16(offset + 6);

    sp<AMessage> format = new AMessage;

    format->setInt32("channel-count", readU16(offset + 8 + 8));
    format->setInt32("sample-size", readU16(offset + 8 + 10));
    format->setInt32("sample-rate", readU32(offset + 8 + 16) / 65536.0f);

    switch (type) {
        case FOURCC('m', 'p', '4', 'a'):
            format->setString("mime", MEDIA_MIMETYPE_AUDIO_AAC);
            break;

        case FOURCC('s', 'a', 'm', 'r'):
            format->setString("mime", MEDIA_MIMETYPE_AUDIO_AMR_NB);
            format->setInt32("channel-count", 1);
            format->setInt32("sample-rate", 8000);
            break;

        case FOURCC('s', 'a', 'w', 'b'):
            format->setString("mime", MEDIA_MIMETYPE_AUDIO_AMR_WB);
            format->setInt32("channel-count", 1);
            format->setInt32("sample-rate", 16000);
            break;
        default:
            format->setString("mime", "application/octet-stream");
            break;
    }

    sampleDesc->mFormat = format;

    return OK;
}

static void addCodecSpecificData(
        const sp<AMessage> &format, int32_t index,
        const void *data, size_t size,
        bool insertStartCode = false) {
    sp<ABuffer> csd = new ABuffer(insertStartCode ? size + 4 : size);

    memcpy(csd->data() + (insertStartCode ? 4 : 0), data, size);

    if (insertStartCode) {
        memcpy(csd->data(), "\x00\x00\x00\x01", 4);
    }

    csd->meta()->setInt32("csd", true);
    csd->meta()->setInt64("timeUs", 0ll);

    format->setBuffer(StringPrintf("csd-%d", index).c_str(), csd);
}

status_t FragmentedMP4Parser::parseSampleSizes(
        uint32_t type, size_t offset, uint64_t size) {
    return editTrack(mCurrentTrackID)->mStaticFragment->parseSampleSizes(
            this, type, offset, size);
}

status_t FragmentedMP4Parser::parseCompactSampleSizes(
        uint32_t type, size_t offset, uint64_t size) {
    return editTrack(mCurrentTrackID)->mStaticFragment->parseCompactSampleSizes(
            this, type, offset, size);
}

status_t FragmentedMP4Parser::parseSampleToChunk(
        uint32_t type, size_t offset, uint64_t size) {
    return editTrack(mCurrentTrackID)->mStaticFragment->parseSampleToChunk(
            this, type, offset, size);
}

status_t FragmentedMP4Parser::parseChunkOffsets(
        uint32_t type, size_t offset, uint64_t size) {
    return editTrack(mCurrentTrackID)->mStaticFragment->parseChunkOffsets(
            this, type, offset, size);
}

status_t FragmentedMP4Parser::parseChunkOffsets64(
        uint32_t type, size_t offset, uint64_t size) {
    return editTrack(mCurrentTrackID)->mStaticFragment->parseChunkOffsets64(
            this, type, offset, size);
}

status_t FragmentedMP4Parser::parseAVCCodecSpecificData(
        uint32_t type, size_t offset, uint64_t size) {
    TrackInfo *trackInfo = editTrack(mCurrentTrackID);

    SampleDescription *sampleDesc =
        &trackInfo->mSampleDescs.editItemAt(
                trackInfo->mSampleDescs.size() - 1);

    if (sampleDesc->mType != FOURCC('a', 'v', 'c', '1')) {
        return -EINVAL;
    }

    const uint8_t *ptr = mBuffer->data() + offset;

    size -= offset;
    offset = 0;

    if (size < 7 || ptr[0] != 0x01) {
        return ERROR_MALFORMED;
    }

    sampleDesc->mFormat->setSize("nal-length-size", 1 + (ptr[4] & 3));

    size_t numSPS = ptr[5] & 31;

    ptr += 6;
    size -= 6;

    for (size_t i = 0; i < numSPS; ++i) {
        if (size < 2) {
            return ERROR_MALFORMED;
        }

        size_t length = U16_AT(ptr);

        ptr += 2;
        size -= 2;

        if (size < length) {
            return ERROR_MALFORMED;
        }

        addCodecSpecificData(
                sampleDesc->mFormat, i, ptr, length,
                true /* insertStartCode */);

        ptr += length;
        size -= length;
    }

    if (size < 1) {
        return ERROR_MALFORMED;
    }

    size_t numPPS = *ptr;
    ++ptr;
    --size;

    for (size_t i = 0; i < numPPS; ++i) {
        if (size < 2) {
            return ERROR_MALFORMED;
        }

        size_t length = U16_AT(ptr);

        ptr += 2;
        size -= 2;

        if (size < length) {
            return ERROR_MALFORMED;
        }

        addCodecSpecificData(
                sampleDesc->mFormat, numSPS + i, ptr, length,
                true /* insertStartCode */);

        ptr += length;
        size -= length;
    }

    return OK;
}

status_t FragmentedMP4Parser::parseESDSCodecSpecificData(
        uint32_t type, size_t offset, uint64_t size) {
    TrackInfo *trackInfo = editTrack(mCurrentTrackID);

    SampleDescription *sampleDesc =
        &trackInfo->mSampleDescs.editItemAt(
                trackInfo->mSampleDescs.size() - 1);

    if (sampleDesc->mType != FOURCC('m', 'p', '4', 'a')
            && sampleDesc->mType != FOURCC('m', 'p', '4', 'v')) {
        return -EINVAL;
    }

    const uint8_t *ptr = mBuffer->data() + offset;

    size -= offset;
    offset = 0;

    if (size < 4) {
        return -EINVAL;
    }

    if (U32_AT(ptr) != 0) {
        return -EINVAL;
    }

    ptr += 4;
    size -=4;

    ESDS esds(ptr, size);

    uint8_t objectTypeIndication;
    if (esds.getObjectTypeIndication(&objectTypeIndication) != OK) {
        return ERROR_MALFORMED;
    }

    const uint8_t *csd;
    size_t csd_size;
    if (esds.getCodecSpecificInfo(
                (const void **)&csd, &csd_size) != OK) {
        return ERROR_MALFORMED;
    }

    addCodecSpecificData(sampleDesc->mFormat, 0, csd, csd_size);

    if (sampleDesc->mType != FOURCC('m', 'p', '4', 'a')) {
        return OK;
    }

    if (csd_size == 0) {
        // There's no further information, i.e. no codec specific data
        // Let's assume that the information provided in the mpeg4 headers
        // is accurate and hope for the best.

        return OK;
    }

    if (csd_size < 2) {
        return ERROR_MALFORMED;
    }

    uint32_t objectType = csd[0] >> 3;

    if (objectType == 31) {
        return ERROR_UNSUPPORTED;
    }

    uint32_t freqIndex = (csd[0] & 7) << 1 | (csd[1] >> 7);
    int32_t sampleRate = 0;
    int32_t numChannels = 0;
    if (freqIndex == 15) {
        if (csd_size < 5) {
            return ERROR_MALFORMED;
        }

        sampleRate = (csd[1] & 0x7f) << 17
                        | csd[2] << 9
                        | csd[3] << 1
                        | (csd[4] >> 7);

        numChannels = (csd[4] >> 3) & 15;
    } else {
        static uint32_t kSamplingRate[] = {
            96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
            16000, 12000, 11025, 8000, 7350
        };

        if (freqIndex == 13 || freqIndex == 14) {
            return ERROR_MALFORMED;
        }

        sampleRate = kSamplingRate[freqIndex];
        numChannels = (csd[1] >> 3) & 15;
    }

    if (numChannels == 0) {
        return ERROR_UNSUPPORTED;
    }

    sampleDesc->mFormat->setInt32("sample-rate", sampleRate);
    sampleDesc->mFormat->setInt32("channel-count", numChannels);

    return OK;
}

status_t FragmentedMP4Parser::parseMediaData(
        uint32_t type, size_t offset, uint64_t size) {
    ALOGV("skipping 'mdat' chunk at offsets 0x%08lx-0x%08llx.",
          mBufferPos + offset, mBufferPos + size);

    sp<ABuffer> buffer = new ABuffer(size - offset);
    memcpy(buffer->data(), mBuffer->data() + offset, size - offset);

    mMediaData.push();
    MediaDataInfo *info = &mMediaData.editItemAt(mMediaData.size() - 1);
    info->mBuffer = buffer;
    info->mOffset = mBufferPos + offset;

    if (mMediaData.size() > 10) {
        ALOGV("suspending for now.");
        mSuspended = true;
    }

    return OK;
}

status_t FragmentedMP4Parser::parseSegmentIndex(
        uint32_t type, size_t offset, uint64_t size) {
    ALOGV("sidx box type %d, offset %d, size %d", type, int(offset), int(size));
//    AString sidxstr;
//    hexdump(mBuffer->data() + offset, size, 0 /* indent */, &sidxstr);
//    ALOGV("raw sidx:");
//    ALOGV("%s", sidxstr.c_str());
    if (offset + 12 > size) {
        return -EINVAL;
    }

    uint32_t flags = readU32(offset);

    uint32_t version = flags >> 24;
    flags &= 0xffffff;

    ALOGV("sidx version %d", version);

    uint32_t referenceId = readU32(offset + 4);
    uint32_t timeScale = readU32(offset + 8);
    ALOGV("sidx refid/timescale: %d/%d", referenceId, timeScale);

    uint64_t earliestPresentationTime;
    uint64_t firstOffset;

    offset += 12;

    if (version == 0) {
        if (offset + 8 > size) {
            return -EINVAL;
        }
        earliestPresentationTime = readU32(offset);
        firstOffset = readU32(offset + 4);
        offset += 8;
    } else {
        if (offset + 16 > size) {
            return -EINVAL;
        }
        earliestPresentationTime = readU64(offset);
        firstOffset = readU64(offset + 8);
        offset += 16;
    }
    ALOGV("sidx pres/off: %Ld/%Ld", earliestPresentationTime, firstOffset);

    if (offset + 4 > size) {
        return -EINVAL;
    }
    if (readU16(offset) != 0) { // reserved
        return -EINVAL;
    }
    int32_t referenceCount = readU16(offset + 2);
    offset += 4;
    ALOGV("refcount: %d", referenceCount);

    if (offset + referenceCount * 12 > size) {
        return -EINVAL;
    }

    TrackInfo *info = editTrack(mCurrentTrackID);
    uint64_t total_duration = 0;
    for (int i = 0; i < referenceCount; i++) {
        uint32_t d1 = readU32(offset);
        uint32_t d2 = readU32(offset + 4);
        uint32_t d3 = readU32(offset + 8);

        if (d1 & 0x80000000) {
            ALOGW("sub-sidx boxes not supported yet");
        }
        bool sap = d3 & 0x80000000;
        bool saptype = d3 >> 28;
        if (!sap || saptype > 2) {
            ALOGW("not a stream access point, or unsupported type");
        }
        total_duration += d2;
        offset += 12;
        ALOGV(" item %d, %08x %08x %08x", i, d1, d2, d3);
        SidxEntry se;
        se.mSize = d1 & 0x7fffffff;
        se.mDurationUs = 1000000LL * d2 / timeScale;
        info->mSidx.add(se);
    }

    info->mSidxDuration = total_duration * 1000000 / timeScale;
    ALOGV("duration: %lld", info->mSidxDuration);
    return OK;
}

status_t FragmentedMP4Parser::parseTrackExtends(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 24 > size) {
        return -EINVAL;
    }

    if (readU32(offset) != 0) {
        return -EINVAL;
    }

    uint32_t trackID = readU32(offset + 4);

    TrackInfo *info = editTrack(trackID, true /* createIfNecessary */);
    info->mDefaultSampleDescriptionIndex = readU32(offset + 8);
    info->mDefaultSampleDuration = readU32(offset + 12);
    info->mDefaultSampleSize = readU32(offset + 16);
    info->mDefaultSampleFlags = readU32(offset + 20);

    return OK;
}

FragmentedMP4Parser::TrackInfo *FragmentedMP4Parser::editTrack(
        uint32_t trackID, bool createIfNecessary) {
    ssize_t i = mTracks.indexOfKey(trackID);

    if (i >= 0) {
        return &mTracks.editValueAt(i);
    }

    if (!createIfNecessary) {
        return NULL;
    }

    TrackInfo info;
    info.mTrackID = trackID;
    info.mFlags = 0;
    info.mDuration = 0xffffffff;
    info.mSidxDuration = 0;
    info.mMediaTimeScale = 0;
    info.mMediaHandlerType = 0;
    info.mDefaultSampleDescriptionIndex = 0;
    info.mDefaultSampleDuration = 0;
    info.mDefaultSampleSize = 0;
    info.mDefaultSampleFlags = 0;

    info.mDecodingTime = 0;

    mTracks.add(trackID, info);
    return &mTracks.editValueAt(mTracks.indexOfKey(trackID));
}

status_t FragmentedMP4Parser::parseTrackFragmentHeader(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 8 > size) {
        return -EINVAL;
    }

    uint32_t flags = readU32(offset);

    if (flags & 0xff000000) {
        return -EINVAL;
    }

    mTrackFragmentHeaderInfo.mFlags = flags;

    mTrackFragmentHeaderInfo.mTrackID = readU32(offset + 4);
    offset += 8;

    if (flags & TrackFragmentHeaderInfo::kBaseDataOffsetPresent) {
        if (offset + 8 > size) {
            return -EINVAL;
        }

        mTrackFragmentHeaderInfo.mBaseDataOffset = readU64(offset);
        offset += 8;
    }

    if (flags & TrackFragmentHeaderInfo::kSampleDescriptionIndexPresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        mTrackFragmentHeaderInfo.mSampleDescriptionIndex = readU32(offset);
        offset += 4;
    }

    if (flags & TrackFragmentHeaderInfo::kDefaultSampleDurationPresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        mTrackFragmentHeaderInfo.mDefaultSampleDuration = readU32(offset);
        offset += 4;
    }

    if (flags & TrackFragmentHeaderInfo::kDefaultSampleSizePresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        mTrackFragmentHeaderInfo.mDefaultSampleSize = readU32(offset);
        offset += 4;
    }

    if (flags & TrackFragmentHeaderInfo::kDefaultSampleFlagsPresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        mTrackFragmentHeaderInfo.mDefaultSampleFlags = readU32(offset);
        offset += 4;
    }

    if (!(flags & TrackFragmentHeaderInfo::kBaseDataOffsetPresent)) {
        // This should point to the position of the first byte of the
        // enclosing 'moof' container for the first track and
        // the end of the data of the preceding fragment for subsequent
        // tracks.

        CHECK_GE(mStack.size(), 2u);

        mTrackFragmentHeaderInfo.mBaseDataOffset =
            mStack.itemAt(mStack.size() - 2).mOffset;

        // XXX TODO: This does not do the right thing for the 2nd and
        // subsequent tracks yet.
    }

    mTrackFragmentHeaderInfo.mDataOffset =
        mTrackFragmentHeaderInfo.mBaseDataOffset;

    TrackInfo *trackInfo = editTrack(mTrackFragmentHeaderInfo.mTrackID);

    if (trackInfo->mFragments.empty()
            || (*trackInfo->mFragments.begin())->complete()) {
        trackInfo->mFragments.push_back(new DynamicTrackFragment);
    }

    return OK;
}

status_t FragmentedMP4Parser::parseTrackFragmentRun(
        uint32_t type, size_t offset, uint64_t size) {
    if (offset + 8 > size) {
        return -EINVAL;
    }

    enum {
        kDataOffsetPresent                  = 0x01,
        kFirstSampleFlagsPresent            = 0x04,
        kSampleDurationPresent              = 0x100,
        kSampleSizePresent                  = 0x200,
        kSampleFlagsPresent                 = 0x400,
        kSampleCompositionTimeOffsetPresent = 0x800,
    };

    uint32_t flags = readU32(offset);

    if (flags & 0xff000000) {
        return -EINVAL;
    }

    if ((flags & kFirstSampleFlagsPresent) && (flags & kSampleFlagsPresent)) {
        // These two shall not be used together.
        return -EINVAL;
    }

    uint32_t sampleCount = readU32(offset + 4);
    offset += 8;

    uint64_t dataOffset = mTrackFragmentHeaderInfo.mDataOffset;

    uint32_t firstSampleFlags = 0;

    if (flags & kDataOffsetPresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        int32_t dataOffsetDelta = (int32_t)readU32(offset);

        dataOffset = mTrackFragmentHeaderInfo.mBaseDataOffset + dataOffsetDelta;

        offset += 4;
    }

    if (flags & kFirstSampleFlagsPresent) {
        if (offset + 4 > size) {
            return -EINVAL;
        }

        firstSampleFlags = readU32(offset);
        offset += 4;
    }

    TrackInfo *info = editTrack(mTrackFragmentHeaderInfo.mTrackID);

    if (info == NULL) {
        return -EINVAL;
    }

    uint32_t sampleDuration = 0, sampleSize = 0, sampleFlags = 0,
             sampleCtsOffset = 0;

    size_t bytesPerSample = 0;
    if (flags & kSampleDurationPresent) {
        bytesPerSample += 4;
    } else if (mTrackFragmentHeaderInfo.mFlags
            & TrackFragmentHeaderInfo::kDefaultSampleDurationPresent) {
        sampleDuration = mTrackFragmentHeaderInfo.mDefaultSampleDuration;
    } else {
        sampleDuration = info->mDefaultSampleDuration;
    }

    if (flags & kSampleSizePresent) {
        bytesPerSample += 4;
    } else if (mTrackFragmentHeaderInfo.mFlags
            & TrackFragmentHeaderInfo::kDefaultSampleSizePresent) {
        sampleSize = mTrackFragmentHeaderInfo.mDefaultSampleSize;
    } else {
        sampleSize = info->mDefaultSampleSize;
    }

    if (flags & kSampleFlagsPresent) {
        bytesPerSample += 4;
    } else if (mTrackFragmentHeaderInfo.mFlags
            & TrackFragmentHeaderInfo::kDefaultSampleFlagsPresent) {
        sampleFlags = mTrackFragmentHeaderInfo.mDefaultSampleFlags;
    } else {
        sampleFlags = info->mDefaultSampleFlags;
    }

    if (flags & kSampleCompositionTimeOffsetPresent) {
        bytesPerSample += 4;
    } else {
        sampleCtsOffset = 0;
    }

    if (offset + sampleCount * bytesPerSample > size) {
        return -EINVAL;
    }

    uint32_t sampleDescIndex =
        (mTrackFragmentHeaderInfo.mFlags
            & TrackFragmentHeaderInfo::kSampleDescriptionIndexPresent)
            ? mTrackFragmentHeaderInfo.mSampleDescriptionIndex
            : info->mDefaultSampleDescriptionIndex;

    for (uint32_t i = 0; i < sampleCount; ++i) {
        if (flags & kSampleDurationPresent) {
            sampleDuration = readU32(offset);
            offset += 4;
        }

        if (flags & kSampleSizePresent) {
            sampleSize = readU32(offset);
            offset += 4;
        }

        if (flags & kSampleFlagsPresent) {
            sampleFlags = readU32(offset);
            offset += 4;
        }

        if (flags & kSampleCompositionTimeOffsetPresent) {
            sampleCtsOffset = readU32(offset);
            offset += 4;
        }

        ALOGV("adding sample at offset 0x%08llx, size %u, duration %u, "
              "sampleDescIndex=%u, flags 0x%08x",
                dataOffset, sampleSize, sampleDuration,
                sampleDescIndex,
                (flags & kFirstSampleFlagsPresent) && i == 0
                    ? firstSampleFlags : sampleFlags);

        const sp<TrackFragment> &fragment = *--info->mFragments.end();

        uint32_t decodingTime = info->mDecodingTime;
        info->mDecodingTime += sampleDuration;
        uint32_t presentationTime = decodingTime + sampleCtsOffset;

        static_cast<DynamicTrackFragment *>(
                fragment.get())->addSample(
                    dataOffset,
                    sampleSize,
                    presentationTime,
                    sampleDescIndex,
                    ((flags & kFirstSampleFlagsPresent) && i == 0)
                        ? firstSampleFlags : sampleFlags);

        dataOffset += sampleSize;
    }

    mTrackFragmentHeaderInfo.mDataOffset = dataOffset;

    return OK;
}

void FragmentedMP4Parser::copyBuffer(
        sp<ABuffer> *dst, size_t offset, uint64_t size) const {
    sp<ABuffer> buf = new ABuffer(size);
    memcpy(buf->data(), mBuffer->data() + offset, size);

    *dst = buf;
}

}  // namespace android
