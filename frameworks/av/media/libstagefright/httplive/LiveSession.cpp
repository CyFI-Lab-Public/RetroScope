/*
 * Copyright (C) 2010 The Android Open Source Project
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
#define LOG_TAG "LiveSession"
#include <utils/Log.h>

#include "LiveSession.h"

#include "M3UParser.h"
#include "PlaylistFetcher.h"

#include "include/HTTPBase.h"
#include "mpeg2ts/AnotherPacketSource.h"

#include <cutils/properties.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/Utils.h>

#include <ctype.h>
#include <openssl/aes.h>
#include <openssl/md5.h>

namespace android {

LiveSession::LiveSession(
        const sp<AMessage> &notify, uint32_t flags, bool uidValid, uid_t uid)
    : mNotify(notify),
      mFlags(flags),
      mUIDValid(uidValid),
      mUID(uid),
      mInPreparationPhase(true),
      mHTTPDataSource(
              HTTPBase::Create(
                  (mFlags & kFlagIncognito)
                    ? HTTPBase::kFlagIncognito
                    : 0)),
      mPrevBandwidthIndex(-1),
      mStreamMask(0),
      mCheckBandwidthGeneration(0),
      mLastDequeuedTimeUs(0ll),
      mRealTimeBaseUs(0ll),
      mReconfigurationInProgress(false),
      mDisconnectReplyID(0) {
    if (mUIDValid) {
        mHTTPDataSource->setUID(mUID);
    }

    mPacketSources.add(
            STREAMTYPE_AUDIO, new AnotherPacketSource(NULL /* meta */));

    mPacketSources.add(
            STREAMTYPE_VIDEO, new AnotherPacketSource(NULL /* meta */));

    mPacketSources.add(
            STREAMTYPE_SUBTITLES, new AnotherPacketSource(NULL /* meta */));
}

LiveSession::~LiveSession() {
}

status_t LiveSession::dequeueAccessUnit(
        StreamType stream, sp<ABuffer> *accessUnit) {
    if (!(mStreamMask & stream)) {
        return UNKNOWN_ERROR;
    }

    sp<AnotherPacketSource> packetSource = mPacketSources.valueFor(stream);

    status_t finalResult;
    if (!packetSource->hasBufferAvailable(&finalResult)) {
        return finalResult == OK ? -EAGAIN : finalResult;
    }

    status_t err = packetSource->dequeueAccessUnit(accessUnit);

    const char *streamStr;
    switch (stream) {
        case STREAMTYPE_AUDIO:
            streamStr = "audio";
            break;
        case STREAMTYPE_VIDEO:
            streamStr = "video";
            break;
        case STREAMTYPE_SUBTITLES:
            streamStr = "subs";
            break;
        default:
            TRESPASS();
    }

    if (err == INFO_DISCONTINUITY) {
        int32_t type;
        CHECK((*accessUnit)->meta()->findInt32("discontinuity", &type));

        sp<AMessage> extra;
        if (!(*accessUnit)->meta()->findMessage("extra", &extra)) {
            extra.clear();
        }

        ALOGI("[%s] read discontinuity of type %d, extra = %s",
              streamStr,
              type,
              extra == NULL ? "NULL" : extra->debugString().c_str());
    } else if (err == OK) {
        if (stream == STREAMTYPE_AUDIO || stream == STREAMTYPE_VIDEO) {
            int64_t timeUs;
            CHECK((*accessUnit)->meta()->findInt64("timeUs",  &timeUs));
            ALOGV("[%s] read buffer at time %lld us", streamStr, timeUs);

            mLastDequeuedTimeUs = timeUs;
            mRealTimeBaseUs = ALooper::GetNowUs() - timeUs;
        } else if (stream == STREAMTYPE_SUBTITLES) {
            (*accessUnit)->meta()->setInt32(
                    "trackIndex", mPlaylist->getSelectedIndex());
            (*accessUnit)->meta()->setInt64("baseUs", mRealTimeBaseUs);
        }
    } else {
        ALOGI("[%s] encountered error %d", streamStr, err);
    }

    return err;
}

status_t LiveSession::getStreamFormat(StreamType stream, sp<AMessage> *format) {
    if (!(mStreamMask & stream)) {
        return UNKNOWN_ERROR;
    }

    sp<AnotherPacketSource> packetSource = mPacketSources.valueFor(stream);

    sp<MetaData> meta = packetSource->getFormat();

    if (meta == NULL) {
        return -EAGAIN;
    }

    return convertMetaDataToMessage(meta, format);
}

void LiveSession::connectAsync(
        const char *url, const KeyedVector<String8, String8> *headers) {
    sp<AMessage> msg = new AMessage(kWhatConnect, id());
    msg->setString("url", url);

    if (headers != NULL) {
        msg->setPointer(
                "headers",
                new KeyedVector<String8, String8>(*headers));
    }

    msg->post();
}

status_t LiveSession::disconnect() {
    sp<AMessage> msg = new AMessage(kWhatDisconnect, id());

    sp<AMessage> response;
    status_t err = msg->postAndAwaitResponse(&response);

    return err;
}

status_t LiveSession::seekTo(int64_t timeUs) {
    sp<AMessage> msg = new AMessage(kWhatSeek, id());
    msg->setInt64("timeUs", timeUs);

    sp<AMessage> response;
    status_t err = msg->postAndAwaitResponse(&response);

    return err;
}

void LiveSession::onMessageReceived(const sp<AMessage> &msg) {
    switch (msg->what()) {
        case kWhatConnect:
        {
            onConnect(msg);
            break;
        }

        case kWhatDisconnect:
        {
            CHECK(msg->senderAwaitsResponse(&mDisconnectReplyID));

            if (mReconfigurationInProgress) {
                break;
            }

            finishDisconnect();
            break;
        }

        case kWhatSeek:
        {
            uint32_t replyID;
            CHECK(msg->senderAwaitsResponse(&replyID));

            status_t err = onSeek(msg);

            sp<AMessage> response = new AMessage;
            response->setInt32("err", err);

            response->postReply(replyID);
            break;
        }

        case kWhatFetcherNotify:
        {
            int32_t what;
            CHECK(msg->findInt32("what", &what));

            switch (what) {
                case PlaylistFetcher::kWhatStarted:
                    break;
                case PlaylistFetcher::kWhatPaused:
                case PlaylistFetcher::kWhatStopped:
                {
                    if (what == PlaylistFetcher::kWhatStopped) {
                        AString uri;
                        CHECK(msg->findString("uri", &uri));
                        mFetcherInfos.removeItem(uri);
                    }

                    if (mContinuation != NULL) {
                        CHECK_GT(mContinuationCounter, 0);
                        if (--mContinuationCounter == 0) {
                            mContinuation->post();
                        }
                    }
                    break;
                }

                case PlaylistFetcher::kWhatDurationUpdate:
                {
                    AString uri;
                    CHECK(msg->findString("uri", &uri));

                    int64_t durationUs;
                    CHECK(msg->findInt64("durationUs", &durationUs));

                    FetcherInfo *info = &mFetcherInfos.editValueFor(uri);
                    info->mDurationUs = durationUs;
                    break;
                }

                case PlaylistFetcher::kWhatError:
                {
                    status_t err;
                    CHECK(msg->findInt32("err", &err));

                    ALOGE("XXX Received error %d from PlaylistFetcher.", err);

                    if (mInPreparationPhase) {
                        postPrepared(err);
                    }

                    mPacketSources.valueFor(STREAMTYPE_AUDIO)->signalEOS(err);

                    mPacketSources.valueFor(STREAMTYPE_VIDEO)->signalEOS(err);

                    mPacketSources.valueFor(
                            STREAMTYPE_SUBTITLES)->signalEOS(err);

                    sp<AMessage> notify = mNotify->dup();
                    notify->setInt32("what", kWhatError);
                    notify->setInt32("err", err);
                    notify->post();
                    break;
                }

                case PlaylistFetcher::kWhatTemporarilyDoneFetching:
                {
                    AString uri;
                    CHECK(msg->findString("uri", &uri));

                    FetcherInfo *info = &mFetcherInfos.editValueFor(uri);
                    info->mIsPrepared = true;

                    if (mInPreparationPhase) {
                        bool allFetchersPrepared = true;
                        for (size_t i = 0; i < mFetcherInfos.size(); ++i) {
                            if (!mFetcherInfos.valueAt(i).mIsPrepared) {
                                allFetchersPrepared = false;
                                break;
                            }
                        }

                        if (allFetchersPrepared) {
                            postPrepared(OK);
                        }
                    }
                    break;
                }

                default:
                    TRESPASS();
            }

            break;
        }

        case kWhatCheckBandwidth:
        {
            int32_t generation;
            CHECK(msg->findInt32("generation", &generation));

            if (generation != mCheckBandwidthGeneration) {
                break;
            }

            onCheckBandwidth();
            break;
        }

        case kWhatChangeConfiguration:
        {
            onChangeConfiguration(msg);
            break;
        }

        case kWhatChangeConfiguration2:
        {
            onChangeConfiguration2(msg);
            break;
        }

        case kWhatChangeConfiguration3:
        {
            onChangeConfiguration3(msg);
            break;
        }

        case kWhatFinishDisconnect2:
        {
            onFinishDisconnect2();
            break;
        }

        default:
            TRESPASS();
            break;
    }
}

// static
int LiveSession::SortByBandwidth(const BandwidthItem *a, const BandwidthItem *b) {
    if (a->mBandwidth < b->mBandwidth) {
        return -1;
    } else if (a->mBandwidth == b->mBandwidth) {
        return 0;
    }

    return 1;
}

void LiveSession::onConnect(const sp<AMessage> &msg) {
    AString url;
    CHECK(msg->findString("url", &url));

    KeyedVector<String8, String8> *headers = NULL;
    if (!msg->findPointer("headers", (void **)&headers)) {
        mExtraHeaders.clear();
    } else {
        mExtraHeaders = *headers;

        delete headers;
        headers = NULL;
    }

#if 1
    ALOGI("onConnect <URL suppressed>");
#else
    ALOGI("onConnect %s", url.c_str());
#endif

    mMasterURL = url;

    bool dummy;
    mPlaylist = fetchPlaylist(url.c_str(), NULL /* curPlaylistHash */, &dummy);

    if (mPlaylist == NULL) {
        ALOGE("unable to fetch master playlist '%s'.", url.c_str());

        postPrepared(ERROR_IO);
        return;
    }

    // We trust the content provider to make a reasonable choice of preferred
    // initial bandwidth by listing it first in the variant playlist.
    // At startup we really don't have a good estimate on the available
    // network bandwidth since we haven't tranferred any data yet. Once
    // we have we can make a better informed choice.
    size_t initialBandwidth = 0;
    size_t initialBandwidthIndex = 0;

    if (mPlaylist->isVariantPlaylist()) {
        for (size_t i = 0; i < mPlaylist->size(); ++i) {
            BandwidthItem item;

            item.mPlaylistIndex = i;

            sp<AMessage> meta;
            AString uri;
            mPlaylist->itemAt(i, &uri, &meta);

            unsigned long bandwidth;
            CHECK(meta->findInt32("bandwidth", (int32_t *)&item.mBandwidth));

            if (initialBandwidth == 0) {
                initialBandwidth = item.mBandwidth;
            }

            mBandwidthItems.push(item);
        }

        CHECK_GT(mBandwidthItems.size(), 0u);

        mBandwidthItems.sort(SortByBandwidth);

        for (size_t i = 0; i < mBandwidthItems.size(); ++i) {
            if (mBandwidthItems.itemAt(i).mBandwidth == initialBandwidth) {
                initialBandwidthIndex = i;
                break;
            }
        }
    } else {
        // dummy item.
        BandwidthItem item;
        item.mPlaylistIndex = 0;
        item.mBandwidth = 0;
        mBandwidthItems.push(item);
    }

    changeConfiguration(
            0ll /* timeUs */, initialBandwidthIndex, true /* pickTrack */);
}

void LiveSession::finishDisconnect() {
    // No reconfiguration is currently pending, make sure none will trigger
    // during disconnection either.
    cancelCheckBandwidthEvent();

    for (size_t i = 0; i < mFetcherInfos.size(); ++i) {
        mFetcherInfos.valueAt(i).mFetcher->stopAsync();
    }

    sp<AMessage> msg = new AMessage(kWhatFinishDisconnect2, id());

    mContinuationCounter = mFetcherInfos.size();
    mContinuation = msg;

    if (mContinuationCounter == 0) {
        msg->post();
    }
}

void LiveSession::onFinishDisconnect2() {
    mContinuation.clear();

    mPacketSources.valueFor(STREAMTYPE_AUDIO)->signalEOS(ERROR_END_OF_STREAM);
    mPacketSources.valueFor(STREAMTYPE_VIDEO)->signalEOS(ERROR_END_OF_STREAM);

    mPacketSources.valueFor(
            STREAMTYPE_SUBTITLES)->signalEOS(ERROR_END_OF_STREAM);

    sp<AMessage> response = new AMessage;
    response->setInt32("err", OK);

    response->postReply(mDisconnectReplyID);
    mDisconnectReplyID = 0;
}

sp<PlaylistFetcher> LiveSession::addFetcher(const char *uri) {
    ssize_t index = mFetcherInfos.indexOfKey(uri);

    if (index >= 0) {
        return NULL;
    }

    sp<AMessage> notify = new AMessage(kWhatFetcherNotify, id());
    notify->setString("uri", uri);

    FetcherInfo info;
    info.mFetcher = new PlaylistFetcher(notify, this, uri);
    info.mDurationUs = -1ll;
    info.mIsPrepared = false;
    looper()->registerHandler(info.mFetcher);

    mFetcherInfos.add(uri, info);

    return info.mFetcher;
}

status_t LiveSession::fetchFile(
        const char *url, sp<ABuffer> *out,
        int64_t range_offset, int64_t range_length) {
    *out = NULL;

    sp<DataSource> source;

    if (!strncasecmp(url, "file://", 7)) {
        source = new FileSource(url + 7);
    } else if (strncasecmp(url, "http://", 7)
            && strncasecmp(url, "https://", 8)) {
        return ERROR_UNSUPPORTED;
    } else {
        KeyedVector<String8, String8> headers = mExtraHeaders;
        if (range_offset > 0 || range_length >= 0) {
            headers.add(
                    String8("Range"),
                    String8(
                        StringPrintf(
                            "bytes=%lld-%s",
                            range_offset,
                            range_length < 0
                                ? "" : StringPrintf("%lld", range_offset + range_length - 1).c_str()).c_str()));
        }
        status_t err = mHTTPDataSource->connect(url, &headers);

        if (err != OK) {
            return err;
        }

        source = mHTTPDataSource;
    }

    off64_t size;
    status_t err = source->getSize(&size);

    if (err != OK) {
        size = 65536;
    }

    sp<ABuffer> buffer = new ABuffer(size);
    buffer->setRange(0, 0);

    for (;;) {
        size_t bufferRemaining = buffer->capacity() - buffer->size();

        if (bufferRemaining == 0) {
            bufferRemaining = 32768;

            ALOGV("increasing download buffer to %d bytes",
                 buffer->size() + bufferRemaining);

            sp<ABuffer> copy = new ABuffer(buffer->size() + bufferRemaining);
            memcpy(copy->data(), buffer->data(), buffer->size());
            copy->setRange(0, buffer->size());

            buffer = copy;
        }

        size_t maxBytesToRead = bufferRemaining;
        if (range_length >= 0) {
            int64_t bytesLeftInRange = range_length - buffer->size();
            if (bytesLeftInRange < maxBytesToRead) {
                maxBytesToRead = bytesLeftInRange;

                if (bytesLeftInRange == 0) {
                    break;
                }
            }
        }

        ssize_t n = source->readAt(
                buffer->size(), buffer->data() + buffer->size(),
                maxBytesToRead);

        if (n < 0) {
            return n;
        }

        if (n == 0) {
            break;
        }

        buffer->setRange(0, buffer->size() + (size_t)n);
    }

    *out = buffer;

    return OK;
}

sp<M3UParser> LiveSession::fetchPlaylist(
        const char *url, uint8_t *curPlaylistHash, bool *unchanged) {
    ALOGV("fetchPlaylist '%s'", url);

    *unchanged = false;

    sp<ABuffer> buffer;
    status_t err = fetchFile(url, &buffer);

    if (err != OK) {
        return NULL;
    }

    // MD5 functionality is not available on the simulator, treat all
    // playlists as changed.

#if defined(HAVE_ANDROID_OS)
    uint8_t hash[16];

    MD5_CTX m;
    MD5_Init(&m);
    MD5_Update(&m, buffer->data(), buffer->size());

    MD5_Final(hash, &m);

    if (curPlaylistHash != NULL && !memcmp(hash, curPlaylistHash, 16)) {
        // playlist unchanged
        *unchanged = true;

        ALOGV("Playlist unchanged, refresh state is now %d",
             (int)mRefreshState);

        return NULL;
    }

    if (curPlaylistHash != NULL) {
        memcpy(curPlaylistHash, hash, sizeof(hash));
    }
#endif

    sp<M3UParser> playlist =
        new M3UParser(url, buffer->data(), buffer->size());

    if (playlist->initCheck() != OK) {
        ALOGE("failed to parse .m3u8 playlist");

        return NULL;
    }

    return playlist;
}

static double uniformRand() {
    return (double)rand() / RAND_MAX;
}

size_t LiveSession::getBandwidthIndex() {
    if (mBandwidthItems.size() == 0) {
        return 0;
    }

#if 1
    char value[PROPERTY_VALUE_MAX];
    ssize_t index = -1;
    if (property_get("media.httplive.bw-index", value, NULL)) {
        char *end;
        index = strtol(value, &end, 10);
        CHECK(end > value && *end == '\0');

        if (index >= 0 && (size_t)index >= mBandwidthItems.size()) {
            index = mBandwidthItems.size() - 1;
        }
    }

    if (index < 0) {
        int32_t bandwidthBps;
        if (mHTTPDataSource != NULL
                && mHTTPDataSource->estimateBandwidth(&bandwidthBps)) {
            ALOGV("bandwidth estimated at %.2f kbps", bandwidthBps / 1024.0f);
        } else {
            ALOGV("no bandwidth estimate.");
            return 0;  // Pick the lowest bandwidth stream by default.
        }

        char value[PROPERTY_VALUE_MAX];
        if (property_get("media.httplive.max-bw", value, NULL)) {
            char *end;
            long maxBw = strtoul(value, &end, 10);
            if (end > value && *end == '\0') {
                if (maxBw > 0 && bandwidthBps > maxBw) {
                    ALOGV("bandwidth capped to %ld bps", maxBw);
                    bandwidthBps = maxBw;
                }
            }
        }

        // Consider only 80% of the available bandwidth usable.
        bandwidthBps = (bandwidthBps * 8) / 10;

        // Pick the highest bandwidth stream below or equal to estimated bandwidth.

        index = mBandwidthItems.size() - 1;
        while (index > 0 && mBandwidthItems.itemAt(index).mBandwidth
                                > (size_t)bandwidthBps) {
            --index;
        }
    }
#elif 0
    // Change bandwidth at random()
    size_t index = uniformRand() * mBandwidthItems.size();
#elif 0
    // There's a 50% chance to stay on the current bandwidth and
    // a 50% chance to switch to the next higher bandwidth (wrapping around
    // to lowest)
    const size_t kMinIndex = 0;

    static ssize_t mPrevBandwidthIndex = -1;

    size_t index;
    if (mPrevBandwidthIndex < 0) {
        index = kMinIndex;
    } else if (uniformRand() < 0.5) {
        index = (size_t)mPrevBandwidthIndex;
    } else {
        index = mPrevBandwidthIndex + 1;
        if (index == mBandwidthItems.size()) {
            index = kMinIndex;
        }
    }
    mPrevBandwidthIndex = index;
#elif 0
    // Pick the highest bandwidth stream below or equal to 1.2 Mbit/sec

    size_t index = mBandwidthItems.size() - 1;
    while (index > 0 && mBandwidthItems.itemAt(index).mBandwidth > 1200000) {
        --index;
    }
#elif 1
    char value[PROPERTY_VALUE_MAX];
    size_t index;
    if (property_get("media.httplive.bw-index", value, NULL)) {
        char *end;
        index = strtoul(value, &end, 10);
        CHECK(end > value && *end == '\0');

        if (index >= mBandwidthItems.size()) {
            index = mBandwidthItems.size() - 1;
        }
    } else {
        index = 0;
    }
#else
    size_t index = mBandwidthItems.size() - 1;  // Highest bandwidth stream
#endif

    CHECK_GE(index, 0);

    return index;
}

status_t LiveSession::onSeek(const sp<AMessage> &msg) {
    int64_t timeUs;
    CHECK(msg->findInt64("timeUs", &timeUs));

    if (!mReconfigurationInProgress) {
        changeConfiguration(timeUs, getBandwidthIndex());
    }

    return OK;
}

status_t LiveSession::getDuration(int64_t *durationUs) const {
    int64_t maxDurationUs = 0ll;
    for (size_t i = 0; i < mFetcherInfos.size(); ++i) {
        int64_t fetcherDurationUs = mFetcherInfos.valueAt(i).mDurationUs;

        if (fetcherDurationUs >= 0ll && fetcherDurationUs > maxDurationUs) {
            maxDurationUs = fetcherDurationUs;
        }
    }

    *durationUs = maxDurationUs;

    return OK;
}

bool LiveSession::isSeekable() const {
    int64_t durationUs;
    return getDuration(&durationUs) == OK && durationUs >= 0;
}

bool LiveSession::hasDynamicDuration() const {
    return false;
}

status_t LiveSession::getTrackInfo(Parcel *reply) const {
    return mPlaylist->getTrackInfo(reply);
}

status_t LiveSession::selectTrack(size_t index, bool select) {
    status_t err = mPlaylist->selectTrack(index, select);
    if (err == OK) {
        (new AMessage(kWhatChangeConfiguration, id()))->post();
    }
    return err;
}

void LiveSession::changeConfiguration(
        int64_t timeUs, size_t bandwidthIndex, bool pickTrack) {
    CHECK(!mReconfigurationInProgress);
    mReconfigurationInProgress = true;

    mPrevBandwidthIndex = bandwidthIndex;

    ALOGV("changeConfiguration => timeUs:%lld us, bwIndex:%d, pickTrack:%d",
          timeUs, bandwidthIndex, pickTrack);

    if (pickTrack) {
        mPlaylist->pickRandomMediaItems();
    }

    CHECK_LT(bandwidthIndex, mBandwidthItems.size());
    const BandwidthItem &item = mBandwidthItems.itemAt(bandwidthIndex);

    uint32_t streamMask = 0;

    AString audioURI;
    if (mPlaylist->getAudioURI(item.mPlaylistIndex, &audioURI)) {
        streamMask |= STREAMTYPE_AUDIO;
    }

    AString videoURI;
    if (mPlaylist->getVideoURI(item.mPlaylistIndex, &videoURI)) {
        streamMask |= STREAMTYPE_VIDEO;
    }

    AString subtitleURI;
    if (mPlaylist->getSubtitleURI(item.mPlaylistIndex, &subtitleURI)) {
        streamMask |= STREAMTYPE_SUBTITLES;
    }

    // Step 1, stop and discard fetchers that are no longer needed.
    // Pause those that we'll reuse.
    for (size_t i = 0; i < mFetcherInfos.size(); ++i) {
        const AString &uri = mFetcherInfos.keyAt(i);

        bool discardFetcher = true;

        // If we're seeking all current fetchers are discarded.
        if (timeUs < 0ll) {
            if (((streamMask & STREAMTYPE_AUDIO) && uri == audioURI)
                    || ((streamMask & STREAMTYPE_VIDEO) && uri == videoURI)
                    || ((streamMask & STREAMTYPE_SUBTITLES) && uri == subtitleURI)) {
                discardFetcher = false;
            }
        }

        if (discardFetcher) {
            mFetcherInfos.valueAt(i).mFetcher->stopAsync();
        } else {
            mFetcherInfos.valueAt(i).mFetcher->pauseAsync();
        }
    }

    sp<AMessage> msg = new AMessage(kWhatChangeConfiguration2, id());
    msg->setInt32("streamMask", streamMask);
    msg->setInt64("timeUs", timeUs);
    if (streamMask & STREAMTYPE_AUDIO) {
        msg->setString("audioURI", audioURI.c_str());
    }
    if (streamMask & STREAMTYPE_VIDEO) {
        msg->setString("videoURI", videoURI.c_str());
    }
    if (streamMask & STREAMTYPE_SUBTITLES) {
        msg->setString("subtitleURI", subtitleURI.c_str());
    }

    // Every time a fetcher acknowledges the stopAsync or pauseAsync request
    // we'll decrement mContinuationCounter, once it reaches zero, i.e. all
    // fetchers have completed their asynchronous operation, we'll post
    // mContinuation, which then is handled below in onChangeConfiguration2.
    mContinuationCounter = mFetcherInfos.size();
    mContinuation = msg;

    if (mContinuationCounter == 0) {
        msg->post();
    }
}

void LiveSession::onChangeConfiguration(const sp<AMessage> &msg) {
    if (!mReconfigurationInProgress) {
        changeConfiguration(-1ll /* timeUs */, getBandwidthIndex());
    } else {
        msg->post(1000000ll); // retry in 1 sec
    }
}

void LiveSession::onChangeConfiguration2(const sp<AMessage> &msg) {
    mContinuation.clear();

    // All fetchers are either suspended or have been removed now.

    uint32_t streamMask;
    CHECK(msg->findInt32("streamMask", (int32_t *)&streamMask));

    AString audioURI, videoURI, subtitleURI;
    if (streamMask & STREAMTYPE_AUDIO) {
        CHECK(msg->findString("audioURI", &audioURI));
        ALOGV("audioURI = '%s'", audioURI.c_str());
    }
    if (streamMask & STREAMTYPE_VIDEO) {
        CHECK(msg->findString("videoURI", &videoURI));
        ALOGV("videoURI = '%s'", videoURI.c_str());
    }
    if (streamMask & STREAMTYPE_SUBTITLES) {
        CHECK(msg->findString("subtitleURI", &subtitleURI));
        ALOGV("subtitleURI = '%s'", subtitleURI.c_str());
    }

    // Determine which decoders to shutdown on the player side,
    // a decoder has to be shutdown if either
    // 1) its streamtype was active before but now longer isn't.
    // or
    // 2) its streamtype was already active and still is but the URI
    //    has changed.
    uint32_t changedMask = 0;
    if (((mStreamMask & streamMask & STREAMTYPE_AUDIO)
                && !(audioURI == mAudioURI))
        || (mStreamMask & ~streamMask & STREAMTYPE_AUDIO)) {
        changedMask |= STREAMTYPE_AUDIO;
    }
    if (((mStreamMask & streamMask & STREAMTYPE_VIDEO)
                && !(videoURI == mVideoURI))
        || (mStreamMask & ~streamMask & STREAMTYPE_VIDEO)) {
        changedMask |= STREAMTYPE_VIDEO;
    }

    if (changedMask == 0) {
        // If nothing changed as far as the audio/video decoders
        // are concerned we can proceed.
        onChangeConfiguration3(msg);
        return;
    }

    // Something changed, inform the player which will shutdown the
    // corresponding decoders and will post the reply once that's done.
    // Handling the reply will continue executing below in
    // onChangeConfiguration3.
    sp<AMessage> notify = mNotify->dup();
    notify->setInt32("what", kWhatStreamsChanged);
    notify->setInt32("changedMask", changedMask);

    msg->setWhat(kWhatChangeConfiguration3);
    msg->setTarget(id());

    notify->setMessage("reply", msg);
    notify->post();
}

void LiveSession::onChangeConfiguration3(const sp<AMessage> &msg) {
    // All remaining fetchers are still suspended, the player has shutdown
    // any decoders that needed it.

    uint32_t streamMask;
    CHECK(msg->findInt32("streamMask", (int32_t *)&streamMask));

    AString audioURI, videoURI, subtitleURI;
    if (streamMask & STREAMTYPE_AUDIO) {
        CHECK(msg->findString("audioURI", &audioURI));
    }
    if (streamMask & STREAMTYPE_VIDEO) {
        CHECK(msg->findString("videoURI", &videoURI));
    }
    if (streamMask & STREAMTYPE_SUBTITLES) {
        CHECK(msg->findString("subtitleURI", &subtitleURI));
    }

    int64_t timeUs;
    CHECK(msg->findInt64("timeUs", &timeUs));

    if (timeUs < 0ll) {
        timeUs = mLastDequeuedTimeUs;
    }
    mRealTimeBaseUs = ALooper::GetNowUs() - timeUs;

    mStreamMask = streamMask;
    mAudioURI = audioURI;
    mVideoURI = videoURI;
    mSubtitleURI = subtitleURI;

    // Resume all existing fetchers and assign them packet sources.
    for (size_t i = 0; i < mFetcherInfos.size(); ++i) {
        const AString &uri = mFetcherInfos.keyAt(i);

        uint32_t resumeMask = 0;

        sp<AnotherPacketSource> audioSource;
        if ((streamMask & STREAMTYPE_AUDIO) && uri == audioURI) {
            audioSource = mPacketSources.valueFor(STREAMTYPE_AUDIO);
            resumeMask |= STREAMTYPE_AUDIO;
        }

        sp<AnotherPacketSource> videoSource;
        if ((streamMask & STREAMTYPE_VIDEO) && uri == videoURI) {
            videoSource = mPacketSources.valueFor(STREAMTYPE_VIDEO);
            resumeMask |= STREAMTYPE_VIDEO;
        }

        sp<AnotherPacketSource> subtitleSource;
        if ((streamMask & STREAMTYPE_SUBTITLES) && uri == subtitleURI) {
            subtitleSource = mPacketSources.valueFor(STREAMTYPE_SUBTITLES);
            resumeMask |= STREAMTYPE_SUBTITLES;
        }

        CHECK_NE(resumeMask, 0u);

        ALOGV("resuming fetchers for mask 0x%08x", resumeMask);

        streamMask &= ~resumeMask;

        mFetcherInfos.valueAt(i).mFetcher->startAsync(
                audioSource, videoSource, subtitleSource);
    }

    // streamMask now only contains the types that need a new fetcher created.

    if (streamMask != 0) {
        ALOGV("creating new fetchers for mask 0x%08x", streamMask);
    }

    while (streamMask != 0) {
        StreamType streamType = (StreamType)(streamMask & ~(streamMask - 1));

        AString uri;
        switch (streamType) {
            case STREAMTYPE_AUDIO:
                uri = audioURI;
                break;
            case STREAMTYPE_VIDEO:
                uri = videoURI;
                break;
            case STREAMTYPE_SUBTITLES:
                uri = subtitleURI;
                break;
            default:
                TRESPASS();
        }

        sp<PlaylistFetcher> fetcher = addFetcher(uri.c_str());
        CHECK(fetcher != NULL);

        sp<AnotherPacketSource> audioSource;
        if ((streamMask & STREAMTYPE_AUDIO) && uri == audioURI) {
            audioSource = mPacketSources.valueFor(STREAMTYPE_AUDIO);
            audioSource->clear();

            streamMask &= ~STREAMTYPE_AUDIO;
        }

        sp<AnotherPacketSource> videoSource;
        if ((streamMask & STREAMTYPE_VIDEO) && uri == videoURI) {
            videoSource = mPacketSources.valueFor(STREAMTYPE_VIDEO);
            videoSource->clear();

            streamMask &= ~STREAMTYPE_VIDEO;
        }

        sp<AnotherPacketSource> subtitleSource;
        if ((streamMask & STREAMTYPE_SUBTITLES) && uri == subtitleURI) {
            subtitleSource = mPacketSources.valueFor(STREAMTYPE_SUBTITLES);
            subtitleSource->clear();

            streamMask &= ~STREAMTYPE_SUBTITLES;
        }

        fetcher->startAsync(audioSource, videoSource, subtitleSource, timeUs);
    }

    // All fetchers have now been started, the configuration change
    // has completed.

    scheduleCheckBandwidthEvent();

    ALOGV("XXX configuration change completed.");

    mReconfigurationInProgress = false;

    if (mDisconnectReplyID != 0) {
        finishDisconnect();
    }
}

void LiveSession::scheduleCheckBandwidthEvent() {
    sp<AMessage> msg = new AMessage(kWhatCheckBandwidth, id());
    msg->setInt32("generation", mCheckBandwidthGeneration);
    msg->post(10000000ll);
}

void LiveSession::cancelCheckBandwidthEvent() {
    ++mCheckBandwidthGeneration;
}

void LiveSession::onCheckBandwidth() {
    if (mReconfigurationInProgress) {
        scheduleCheckBandwidthEvent();
        return;
    }

    size_t bandwidthIndex = getBandwidthIndex();
    if (mPrevBandwidthIndex < 0
            || bandwidthIndex != (size_t)mPrevBandwidthIndex) {
        changeConfiguration(-1ll /* timeUs */, bandwidthIndex);
    }

    // Handling the kWhatCheckBandwidth even here does _not_ automatically
    // schedule another one on return, only an explicit call to
    // scheduleCheckBandwidthEvent will do that.
    // This ensures that only one configuration change is ongoing at any
    // one time, once that completes it'll schedule another check bandwidth
    // event.
}

void LiveSession::postPrepared(status_t err) {
    CHECK(mInPreparationPhase);

    sp<AMessage> notify = mNotify->dup();
    if (err == OK || err == ERROR_END_OF_STREAM) {
        notify->setInt32("what", kWhatPrepared);
    } else {
        notify->setInt32("what", kWhatPreparationFailed);
        notify->setInt32("err", err);
    }

    notify->post();

    mInPreparationPhase = false;
}

}  // namespace android

