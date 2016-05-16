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
#define LOG_TAG "PlaylistFetcher"
#include <utils/Log.h>

#include "PlaylistFetcher.h"

#include "LiveDataSource.h"
#include "LiveSession.h"
#include "M3UParser.h"

#include "include/avc_utils.h"
#include "include/HTTPBase.h"
#include "include/ID3.h"
#include "mpeg2ts/AnotherPacketSource.h"

#include <media/IStreamSource.h>
#include <media/stagefright/foundation/ABitReader.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>

#include <ctype.h>
#include <openssl/aes.h>
#include <openssl/md5.h>

namespace android {

// static
const int64_t PlaylistFetcher::kMinBufferedDurationUs = 10000000ll;

PlaylistFetcher::PlaylistFetcher(
        const sp<AMessage> &notify,
        const sp<LiveSession> &session,
        const char *uri)
    : mNotify(notify),
      mSession(session),
      mURI(uri),
      mStreamTypeMask(0),
      mStartTimeUs(-1ll),
      mLastPlaylistFetchTimeUs(-1ll),
      mSeqNumber(-1),
      mNumRetries(0),
      mStartup(true),
      mNextPTSTimeUs(-1ll),
      mMonitorQueueGeneration(0),
      mRefreshState(INITIAL_MINIMUM_RELOAD_DELAY),
      mFirstPTSValid(false),
      mAbsoluteTimeAnchorUs(0ll) {
    memset(mPlaylistHash, 0, sizeof(mPlaylistHash));
}

PlaylistFetcher::~PlaylistFetcher() {
}

int64_t PlaylistFetcher::getSegmentStartTimeUs(int32_t seqNumber) const {
    CHECK(mPlaylist != NULL);

    int32_t firstSeqNumberInPlaylist;
    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &firstSeqNumberInPlaylist)) {
        firstSeqNumberInPlaylist = 0;
    }

    int32_t lastSeqNumberInPlaylist =
        firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;

    CHECK_GE(seqNumber, firstSeqNumberInPlaylist);
    CHECK_LE(seqNumber, lastSeqNumberInPlaylist);

    int64_t segmentStartUs = 0ll;
    for (int32_t index = 0;
            index < seqNumber - firstSeqNumberInPlaylist; ++index) {
        sp<AMessage> itemMeta;
        CHECK(mPlaylist->itemAt(
                    index, NULL /* uri */, &itemMeta));

        int64_t itemDurationUs;
        CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

        segmentStartUs += itemDurationUs;
    }

    return segmentStartUs;
}

bool PlaylistFetcher::timeToRefreshPlaylist(int64_t nowUs) const {
    if (mPlaylist == NULL) {
        CHECK_EQ((int)mRefreshState, (int)INITIAL_MINIMUM_RELOAD_DELAY);
        return true;
    }

    int32_t targetDurationSecs;
    CHECK(mPlaylist->meta()->findInt32("target-duration", &targetDurationSecs));

    int64_t targetDurationUs = targetDurationSecs * 1000000ll;

    int64_t minPlaylistAgeUs;

    switch (mRefreshState) {
        case INITIAL_MINIMUM_RELOAD_DELAY:
        {
            size_t n = mPlaylist->size();
            if (n > 0) {
                sp<AMessage> itemMeta;
                CHECK(mPlaylist->itemAt(n - 1, NULL /* uri */, &itemMeta));

                int64_t itemDurationUs;
                CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

                minPlaylistAgeUs = itemDurationUs;
                break;
            }

            // fall through
        }

        case FIRST_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = targetDurationUs / 2;
            break;
        }

        case SECOND_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = (targetDurationUs * 3) / 2;
            break;
        }

        case THIRD_UNCHANGED_RELOAD_ATTEMPT:
        {
            minPlaylistAgeUs = targetDurationUs * 3;
            break;
        }

        default:
            TRESPASS();
            break;
    }

    return mLastPlaylistFetchTimeUs + minPlaylistAgeUs <= nowUs;
}

status_t PlaylistFetcher::decryptBuffer(
        size_t playlistIndex, const sp<ABuffer> &buffer) {
    sp<AMessage> itemMeta;
    bool found = false;
    AString method;

    for (ssize_t i = playlistIndex; i >= 0; --i) {
        AString uri;
        CHECK(mPlaylist->itemAt(i, &uri, &itemMeta));

        if (itemMeta->findString("cipher-method", &method)) {
            found = true;
            break;
        }
    }

    if (!found) {
        method = "NONE";
    }

    if (method == "NONE") {
        return OK;
    } else if (!(method == "AES-128")) {
        ALOGE("Unsupported cipher method '%s'", method.c_str());
        return ERROR_UNSUPPORTED;
    }

    AString keyURI;
    if (!itemMeta->findString("cipher-uri", &keyURI)) {
        ALOGE("Missing key uri");
        return ERROR_MALFORMED;
    }

    ssize_t index = mAESKeyForURI.indexOfKey(keyURI);

    sp<ABuffer> key;
    if (index >= 0) {
        key = mAESKeyForURI.valueAt(index);
    } else {
        status_t err = mSession->fetchFile(keyURI.c_str(), &key);

        if (err != OK) {
            ALOGE("failed to fetch cipher key from '%s'.", keyURI.c_str());
            return ERROR_IO;
        } else if (key->size() != 16) {
            ALOGE("key file '%s' wasn't 16 bytes in size.", keyURI.c_str());
            return ERROR_MALFORMED;
        }

        mAESKeyForURI.add(keyURI, key);
    }

    AES_KEY aes_key;
    if (AES_set_decrypt_key(key->data(), 128, &aes_key) != 0) {
        ALOGE("failed to set AES decryption key.");
        return UNKNOWN_ERROR;
    }

    unsigned char aes_ivec[16];

    AString iv;
    if (itemMeta->findString("cipher-iv", &iv)) {
        if ((!iv.startsWith("0x") && !iv.startsWith("0X"))
                || iv.size() != 16 * 2 + 2) {
            ALOGE("malformed cipher IV '%s'.", iv.c_str());
            return ERROR_MALFORMED;
        }

        memset(aes_ivec, 0, sizeof(aes_ivec));
        for (size_t i = 0; i < 16; ++i) {
            char c1 = tolower(iv.c_str()[2 + 2 * i]);
            char c2 = tolower(iv.c_str()[3 + 2 * i]);
            if (!isxdigit(c1) || !isxdigit(c2)) {
                ALOGE("malformed cipher IV '%s'.", iv.c_str());
                return ERROR_MALFORMED;
            }
            uint8_t nibble1 = isdigit(c1) ? c1 - '0' : c1 - 'a' + 10;
            uint8_t nibble2 = isdigit(c2) ? c2 - '0' : c2 - 'a' + 10;

            aes_ivec[i] = nibble1 << 4 | nibble2;
        }
    } else {
        memset(aes_ivec, 0, sizeof(aes_ivec));
        aes_ivec[15] = mSeqNumber & 0xff;
        aes_ivec[14] = (mSeqNumber >> 8) & 0xff;
        aes_ivec[13] = (mSeqNumber >> 16) & 0xff;
        aes_ivec[12] = (mSeqNumber >> 24) & 0xff;
    }

    AES_cbc_encrypt(
            buffer->data(), buffer->data(), buffer->size(),
            &aes_key, aes_ivec, AES_DECRYPT);

    // hexdump(buffer->data(), buffer->size());

    size_t n = buffer->size();
    CHECK_GT(n, 0u);

    size_t pad = buffer->data()[n - 1];

    CHECK_GT(pad, 0u);
    CHECK_LE(pad, 16u);
    CHECK_GE((size_t)n, pad);
    for (size_t i = 0; i < pad; ++i) {
        CHECK_EQ((unsigned)buffer->data()[n - 1 - i], pad);
    }

    n -= pad;

    buffer->setRange(buffer->offset(), n);

    return OK;
}

void PlaylistFetcher::postMonitorQueue(int64_t delayUs) {
    sp<AMessage> msg = new AMessage(kWhatMonitorQueue, id());
    msg->setInt32("generation", mMonitorQueueGeneration);
    msg->post(delayUs);
}

void PlaylistFetcher::cancelMonitorQueue() {
    ++mMonitorQueueGeneration;
}

void PlaylistFetcher::startAsync(
        const sp<AnotherPacketSource> &audioSource,
        const sp<AnotherPacketSource> &videoSource,
        const sp<AnotherPacketSource> &subtitleSource,
        int64_t startTimeUs) {
    sp<AMessage> msg = new AMessage(kWhatStart, id());

    uint32_t streamTypeMask = 0ul;

    if (audioSource != NULL) {
        msg->setPointer("audioSource", audioSource.get());
        streamTypeMask |= LiveSession::STREAMTYPE_AUDIO;
    }

    if (videoSource != NULL) {
        msg->setPointer("videoSource", videoSource.get());
        streamTypeMask |= LiveSession::STREAMTYPE_VIDEO;
    }

    if (subtitleSource != NULL) {
        msg->setPointer("subtitleSource", subtitleSource.get());
        streamTypeMask |= LiveSession::STREAMTYPE_SUBTITLES;
    }

    msg->setInt32("streamTypeMask", streamTypeMask);
    msg->setInt64("startTimeUs", startTimeUs);
    msg->post();
}

void PlaylistFetcher::pauseAsync() {
    (new AMessage(kWhatPause, id()))->post();
}

void PlaylistFetcher::stopAsync() {
    (new AMessage(kWhatStop, id()))->post();
}

void PlaylistFetcher::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatStart:
        {
            status_t err = onStart(msg);

            sp<AMessage> notify = mNotify->dup();
            notify->setInt32("what", kWhatStarted);
            notify->setInt32("err", err);
            notify->post();
            break;
        }

        case kWhatPause:
        {
            onPause();

            sp<AMessage> notify = mNotify->dup();
            notify->setInt32("what", kWhatPaused);
            notify->post();
            break;
        }

        case kWhatStop:
        {
            onStop();

            sp<AMessage> notify = mNotify->dup();
            notify->setInt32("what", kWhatStopped);
            notify->post();
            break;
        }

        case kWhatMonitorQueue:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mMonitorQueueGeneration) {
                // Stale event
                break;
            }

            onMonitorQueue();
            break;
        }

        default:
            TRESPASS();
    }
}

status_t PlaylistFetcher::onStart(const sp<AMessage> &msg) {
    mPacketSources.clear();

    uint32_t streamTypeMask;
    CHECK(msg->findInt32("streamTypeMask", (int32_t *)&streamTypeMask));

    int64_t startTimeUs;
    CHECK(msg->findInt64("startTimeUs", &startTimeUs));

    if (streamTypeMask & LiveSession::STREAMTYPE_AUDIO) {
        void *ptr;
        CHECK(msg->findPointer("audioSource", &ptr));

        mPacketSources.add(
                LiveSession::STREAMTYPE_AUDIO,
                static_cast<AnotherPacketSource *>(ptr));
    }

    if (streamTypeMask & LiveSession::STREAMTYPE_VIDEO) {
        void *ptr;
        CHECK(msg->findPointer("videoSource", &ptr));

        mPacketSources.add(
                LiveSession::STREAMTYPE_VIDEO,
                static_cast<AnotherPacketSource *>(ptr));
    }

    if (streamTypeMask & LiveSession::STREAMTYPE_SUBTITLES) {
        void *ptr;
        CHECK(msg->findPointer("subtitleSource", &ptr));

        mPacketSources.add(
                LiveSession::STREAMTYPE_SUBTITLES,
                static_cast<AnotherPacketSource *>(ptr));
    }

    mStreamTypeMask = streamTypeMask;
    mStartTimeUs = startTimeUs;

    if (mStartTimeUs >= 0ll) {
        mSeqNumber = -1;
        mStartup = true;
    }

    postMonitorQueue();

    return OK;
}

void PlaylistFetcher::onPause() {
    cancelMonitorQueue();

    mPacketSources.clear();
    mStreamTypeMask = 0;
}

void PlaylistFetcher::onStop() {
    cancelMonitorQueue();

    for (size_t i = 0; i < mPacketSources.size(); ++i) {
        mPacketSources.valueAt(i)->clear();
    }

    mPacketSources.clear();
    mStreamTypeMask = 0;
}

void PlaylistFetcher::notifyError(status_t err) {
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatError);
    notify->setInt32("err", err);
    notify->post();
}

void PlaylistFetcher::queueDiscontinuity(
        ATSParser::DiscontinuityType type, const sp<AMessage> &extra) {
    for (size_t i = 0; i < mPacketSources.size(); ++i) {
        mPacketSources.valueAt(i)->queueDiscontinuity(type, extra);
    }
}

void PlaylistFetcher::onMonitorQueue() {
    bool downloadMore = false;

    status_t finalResult;
    if (mStreamTypeMask == LiveSession::STREAMTYPE_SUBTITLES) {
        sp<AnotherPacketSource> packetSource =
            mPacketSources.valueFor(LiveSession::STREAMTYPE_SUBTITLES);

        int64_t bufferedDurationUs =
                packetSource->getBufferedDurationUs(&finalResult);

        downloadMore = (bufferedDurationUs < kMinBufferedDurationUs);
        finalResult = OK;
    } else {
        bool first = true;
        int64_t minBufferedDurationUs = 0ll;

        for (size_t i = 0; i < mPacketSources.size(); ++i) {
            if ((mStreamTypeMask & mPacketSources.keyAt(i)) == 0) {
                continue;
            }

            int64_t bufferedDurationUs =
                mPacketSources.valueAt(i)->getBufferedDurationUs(&finalResult);

            if (first || bufferedDurationUs < minBufferedDurationUs) {
                minBufferedDurationUs = bufferedDurationUs;
                first = false;
            }
        }

        downloadMore =
            !first && (minBufferedDurationUs < kMinBufferedDurationUs);
    }

    if (finalResult == OK && downloadMore) {
        onDownloadNext();
    } else {
        // Nothing to do yet, try again in a second.

        sp<AMessage> msg = mNotify->dup();
        msg->setInt32("what", kWhatTemporarilyDoneFetching);
        msg->post();

        postMonitorQueue(1000000ll);
    }
}

void PlaylistFetcher::onDownloadNext() {
    int64_t nowUs = ALooper::GetNowUs();

    if (mLastPlaylistFetchTimeUs < 0ll
            || (!mPlaylist->isComplete() && timeToRefreshPlaylist(nowUs))) {
        bool unchanged;
        sp<M3UParser> playlist = mSession->fetchPlaylist(
                mURI.c_str(), mPlaylistHash, &unchanged);

        if (playlist == NULL) {
            if (unchanged) {
                // We succeeded in fetching the playlist, but it was
                // unchanged from the last time we tried.

                if (mRefreshState != THIRD_UNCHANGED_RELOAD_ATTEMPT) {
                    mRefreshState = (RefreshState)(mRefreshState + 1);
                }
            } else {
                ALOGE("failed to load playlist at url '%s'", mURI.c_str());
                notifyError(ERROR_IO);
                return;
            }
        } else {
            mRefreshState = INITIAL_MINIMUM_RELOAD_DELAY;
            mPlaylist = playlist;

            if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
                updateDuration();
            }
        }

        mLastPlaylistFetchTimeUs = ALooper::GetNowUs();
    }

    int32_t firstSeqNumberInPlaylist;
    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &firstSeqNumberInPlaylist)) {
        firstSeqNumberInPlaylist = 0;
    }

    bool seekDiscontinuity = false;
    bool explicitDiscontinuity = false;

    const int32_t lastSeqNumberInPlaylist =
        firstSeqNumberInPlaylist + (int32_t)mPlaylist->size() - 1;

    if (mSeqNumber < 0) {
        CHECK_GE(mStartTimeUs, 0ll);

        if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
            mSeqNumber = getSeqNumberForTime(mStartTimeUs);
        } else {
            // If this is a live session, start 3 segments from the end.
            mSeqNumber = lastSeqNumberInPlaylist - 3;
            if (mSeqNumber < firstSeqNumberInPlaylist) {
                mSeqNumber = firstSeqNumberInPlaylist;
            }
        }

        mStartTimeUs = -1ll;
    }

    if (mSeqNumber < firstSeqNumberInPlaylist
            || mSeqNumber > lastSeqNumberInPlaylist) {
        if (!mPlaylist->isComplete() && mNumRetries < kMaxNumRetries) {
            ++mNumRetries;

            if (mSeqNumber > lastSeqNumberInPlaylist) {
                mLastPlaylistFetchTimeUs = -1;
                postMonitorQueue(3000000ll);
                return;
            }

            // we've missed the boat, let's start from the lowest sequence
            // number available and signal a discontinuity.

            ALOGI("We've missed the boat, restarting playback.");
            mSeqNumber = lastSeqNumberInPlaylist;
            explicitDiscontinuity = true;

            // fall through
        } else {
            ALOGE("Cannot find sequence number %d in playlist "
                 "(contains %d - %d)",
                 mSeqNumber, firstSeqNumberInPlaylist,
                 firstSeqNumberInPlaylist + mPlaylist->size() - 1);

            notifyError(ERROR_END_OF_STREAM);
            return;
        }
    }

    mNumRetries = 0;

    AString uri;
    sp<AMessage> itemMeta;
    CHECK(mPlaylist->itemAt(
                mSeqNumber - firstSeqNumberInPlaylist,
                &uri,
                &itemMeta));

    int32_t val;
    if (itemMeta->findInt32("discontinuity", &val) && val != 0) {
        explicitDiscontinuity = true;
    }

    int64_t range_offset, range_length;
    if (!itemMeta->findInt64("range-offset", &range_offset)
            || !itemMeta->findInt64("range-length", &range_length)) {
        range_offset = 0;
        range_length = -1;
    }

    ALOGV("fetching segment %d from (%d .. %d)",
          mSeqNumber, firstSeqNumberInPlaylist, lastSeqNumberInPlaylist);

    ALOGV("fetching '%s'", uri.c_str());

    sp<ABuffer> buffer;
    status_t err = mSession->fetchFile(
            uri.c_str(), &buffer, range_offset, range_length);

    if (err != OK) {
        ALOGE("failed to fetch .ts segment at url '%s'", uri.c_str());
        notifyError(err);
        return;
    }

    CHECK(buffer != NULL);

    err = decryptBuffer(mSeqNumber - firstSeqNumberInPlaylist, buffer);

    if (err != OK) {
        ALOGE("decryptBuffer failed w/ error %d", err);

        notifyError(err);
        return;
    }

    if (mStartup || seekDiscontinuity || explicitDiscontinuity) {
        // Signal discontinuity.

        if (mPlaylist->isComplete() || mPlaylist->isEvent()) {
            // If this was a live event this made no sense since
            // we don't have access to all the segment before the current
            // one.
            mNextPTSTimeUs = getSegmentStartTimeUs(mSeqNumber);
        }

        if (seekDiscontinuity || explicitDiscontinuity) {
            ALOGI("queueing discontinuity (seek=%d, explicit=%d)",
                 seekDiscontinuity, explicitDiscontinuity);

            queueDiscontinuity(
                    explicitDiscontinuity
                        ? ATSParser::DISCONTINUITY_FORMATCHANGE
                        : ATSParser::DISCONTINUITY_SEEK,
                    NULL /* extra */);
        }
    }

    err = extractAndQueueAccessUnits(buffer, itemMeta);

    if (err != OK) {
        notifyError(err);
        return;
    }

    ++mSeqNumber;

    postMonitorQueue();

    mStartup = false;
}

int32_t PlaylistFetcher::getSeqNumberForTime(int64_t timeUs) const {
    int32_t firstSeqNumberInPlaylist;
    if (mPlaylist->meta() == NULL || !mPlaylist->meta()->findInt32(
                "media-sequence", &firstSeqNumberInPlaylist)) {
        firstSeqNumberInPlaylist = 0;
    }

    size_t index = 0;
    int64_t segmentStartUs = 0;
    while (index < mPlaylist->size()) {
        sp<AMessage> itemMeta;
        CHECK(mPlaylist->itemAt(
                    index, NULL /* uri */, &itemMeta));

        int64_t itemDurationUs;
        CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

        if (timeUs < segmentStartUs + itemDurationUs) {
            break;
        }

        segmentStartUs += itemDurationUs;
        ++index;
    }

    if (index >= mPlaylist->size()) {
        index = mPlaylist->size() - 1;
    }

    return firstSeqNumberInPlaylist + index;
}

status_t PlaylistFetcher::extractAndQueueAccessUnits(
        const sp<ABuffer> &buffer, const sp<AMessage> &itemMeta) {
    if (buffer->size() > 0 && buffer->data()[0] == 0x47) {
        // Let's assume this is an MPEG2 transport stream.

        if ((buffer->size() % 188) != 0) {
            ALOGE("MPEG2 transport stream is not an even multiple of 188 "
                  "bytes in length.");
            return ERROR_MALFORMED;
        }

        if (mTSParser == NULL) {
            mTSParser = new ATSParser;
        }

        if (mNextPTSTimeUs >= 0ll) {
            sp<AMessage> extra = new AMessage;
            extra->setInt64(IStreamListener::kKeyMediaTimeUs, mNextPTSTimeUs);

            mTSParser->signalDiscontinuity(
                    ATSParser::DISCONTINUITY_SEEK, extra);

            mNextPTSTimeUs = -1ll;
        }

        size_t offset = 0;
        while (offset < buffer->size()) {
            status_t err = mTSParser->feedTSPacket(buffer->data() + offset, 188);

            if (err != OK) {
                return err;
            }

            offset += 188;
        }

        for (size_t i = mPacketSources.size(); i-- > 0;) {
            sp<AnotherPacketSource> packetSource = mPacketSources.valueAt(i);

            ATSParser::SourceType type;
            switch (mPacketSources.keyAt(i)) {
                case LiveSession::STREAMTYPE_VIDEO:
                    type = ATSParser::VIDEO;
                    break;

                case LiveSession::STREAMTYPE_AUDIO:
                    type = ATSParser::AUDIO;
                    break;

                case LiveSession::STREAMTYPE_SUBTITLES:
                {
                    ALOGE("MPEG2 Transport streams do not contain subtitles.");
                    return ERROR_MALFORMED;
                    break;
                }

                default:
                    TRESPASS();
            }

            sp<AnotherPacketSource> source =
                static_cast<AnotherPacketSource *>(
                        mTSParser->getSource(type).get());

            if (source == NULL) {
                ALOGW("MPEG2 Transport stream does not contain %s data.",
                      type == ATSParser::VIDEO ? "video" : "audio");

                mStreamTypeMask &= ~mPacketSources.keyAt(i);
                mPacketSources.removeItemsAt(i);
                continue;
            }

            sp<ABuffer> accessUnit;
            status_t finalResult;
            while (source->hasBufferAvailable(&finalResult)
                    && source->dequeueAccessUnit(&accessUnit) == OK) {
                // Note that we do NOT dequeue any discontinuities.

                packetSource->queueAccessUnit(accessUnit);
            }

            if (packetSource->getFormat() == NULL) {
                packetSource->setFormat(source->getFormat());
            }
        }

        return OK;
    } else if (buffer->size() >= 7 && !memcmp("WEBVTT\n", buffer->data(), 7)) {
        if (mStreamTypeMask != LiveSession::STREAMTYPE_SUBTITLES) {
            ALOGE("This stream only contains subtitles.");
            return ERROR_MALFORMED;
        }

        const sp<AnotherPacketSource> packetSource =
            mPacketSources.valueFor(LiveSession::STREAMTYPE_SUBTITLES);

        int64_t durationUs;
        CHECK(itemMeta->findInt64("durationUs", &durationUs));
        buffer->meta()->setInt64("timeUs", getSegmentStartTimeUs(mSeqNumber));
        buffer->meta()->setInt64("durationUs", durationUs);

        packetSource->queueAccessUnit(buffer);
        return OK;
    }

    if (mNextPTSTimeUs >= 0ll) {
        mFirstPTSValid = false;
        mAbsoluteTimeAnchorUs = mNextPTSTimeUs;
        mNextPTSTimeUs = -1ll;
    }

    // This better be an ISO 13818-7 (AAC) or ISO 13818-1 (MPEG) audio
    // stream prefixed by an ID3 tag.

    bool firstID3Tag = true;
    uint64_t PTS = 0;

    for (;;) {
        // Make sure to skip all ID3 tags preceding the audio data.
        // At least one must be present to provide the PTS timestamp.

        ID3 id3(buffer->data(), buffer->size(), true /* ignoreV1 */);
        if (!id3.isValid()) {
            if (firstID3Tag) {
                ALOGE("Unable to parse ID3 tag.");
                return ERROR_MALFORMED;
            } else {
                break;
            }
        }

        if (firstID3Tag) {
            bool found = false;

            ID3::Iterator it(id3, "PRIV");
            while (!it.done()) {
                size_t length;
                const uint8_t *data = it.getData(&length);

                static const char *kMatchName =
                    "com.apple.streaming.transportStreamTimestamp";
                static const size_t kMatchNameLen = strlen(kMatchName);

                if (length == kMatchNameLen + 1 + 8
                        && !strncmp((const char *)data, kMatchName, kMatchNameLen)) {
                    found = true;
                    PTS = U64_AT(&data[kMatchNameLen + 1]);
                }

                it.next();
            }

            if (!found) {
                ALOGE("Unable to extract transportStreamTimestamp from ID3 tag.");
                return ERROR_MALFORMED;
            }
        }

        // skip the ID3 tag
        buffer->setRange(
                buffer->offset() + id3.rawSize(), buffer->size() - id3.rawSize());

        firstID3Tag = false;
    }

    if (!mFirstPTSValid) {
        mFirstPTSValid = true;
        mFirstPTS = PTS;
    }
    PTS -= mFirstPTS;

    int64_t timeUs = (PTS * 100ll) / 9ll + mAbsoluteTimeAnchorUs;

    if (mStreamTypeMask != LiveSession::STREAMTYPE_AUDIO) {
        ALOGW("This stream only contains audio data!");

        mStreamTypeMask &= LiveSession::STREAMTYPE_AUDIO;

        if (mStreamTypeMask == 0) {
            return OK;
        }
    }

    sp<AnotherPacketSource> packetSource =
        mPacketSources.valueFor(LiveSession::STREAMTYPE_AUDIO);

    if (packetSource->getFormat() == NULL && buffer->size() >= 7) {
        ABitReader bits(buffer->data(), buffer->size());

        // adts_fixed_header

        CHECK_EQ(bits.getBits(12), 0xfffu);
        bits.skipBits(3);  // ID, layer
        bool protection_absent = bits.getBits(1) != 0;

        unsigned profile = bits.getBits(2);
        CHECK_NE(profile, 3u);
        unsigned sampling_freq_index = bits.getBits(4);
        bits.getBits(1);  // private_bit
        unsigned channel_configuration = bits.getBits(3);
        CHECK_NE(channel_configuration, 0u);
        bits.skipBits(2);  // original_copy, home

        sp<MetaData> meta = MakeAACCodecSpecificData(
                profile, sampling_freq_index, channel_configuration);

        meta->setInt32(kKeyIsADTS, true);

        packetSource->setFormat(meta);
    }

    int64_t numSamples = 0ll;
    int32_t sampleRate;
    CHECK(packetSource->getFormat()->findInt32(kKeySampleRate, &sampleRate));

    size_t offset = 0;
    while (offset < buffer->size()) {
        const uint8_t *adtsHeader = buffer->data() + offset;
        CHECK_LT(offset + 5, buffer->size());

        unsigned aac_frame_length =
            ((adtsHeader[3] & 3) << 11)
            | (adtsHeader[4] << 3)
            | (adtsHeader[5] >> 5);

        CHECK_LE(offset + aac_frame_length, buffer->size());

        sp<ABuffer> unit = new ABuffer(aac_frame_length);
        memcpy(unit->data(), adtsHeader, aac_frame_length);

        int64_t unitTimeUs = timeUs + numSamples * 1000000ll / sampleRate;
        unit->meta()->setInt64("timeUs", unitTimeUs);

        // Each AAC frame encodes 1024 samples.
        numSamples += 1024;

        packetSource->queueAccessUnit(unit);

        offset += aac_frame_length;
    }

    return OK;
}

void PlaylistFetcher::updateDuration() {
    int64_t durationUs = 0ll;
    for (size_t index = 0; index < mPlaylist->size(); ++index) {
        sp<AMessage> itemMeta;
        CHECK(mPlaylist->itemAt(
                    index, NULL /* uri */, &itemMeta));

        int64_t itemDurationUs;
        CHECK(itemMeta->findInt64("durationUs", &itemDurationUs));

        durationUs += itemDurationUs;
    }

    sp<AMessage> msg = mNotify->dup();
    msg->setInt32("what", kWhatDurationUpdate);
    msg->setInt64("durationUs", durationUs);
    msg->post();
}

}  // namespace android
