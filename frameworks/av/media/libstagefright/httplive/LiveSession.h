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

#ifndef LIVE_SESSION_H_

#define LIVE_SESSION_H_

#include <media/stagefright/foundation/AHandler.h>

#include <utils/String8.h>

namespace android {

struct ABuffer;
struct AnotherPacketSource;
struct DataSource;
struct HTTPBase;
struct LiveDataSource;
struct M3UParser;
struct PlaylistFetcher;
struct Parcel;

struct LiveSession : public AHandler {
    enum Flags {
        // Don't log any URLs.
        kFlagIncognito = 1,
    };
    LiveSession(
            const sp<AMessage> &notify,
            uint32_t flags = 0, bool uidValid = false, uid_t uid = 0);

    enum StreamType {
        STREAMTYPE_AUDIO        = 1,
        STREAMTYPE_VIDEO        = 2,
        STREAMTYPE_SUBTITLES    = 4,
    };
    status_t dequeueAccessUnit(StreamType stream, sp<ABuffer> *accessUnit);

    status_t getStreamFormat(StreamType stream, sp<AMessage> *format);

    void connectAsync(
            const char *url,
            const KeyedVector<String8, String8> *headers = NULL);

    status_t disconnect();

    // Blocks until seek is complete.
    status_t seekTo(int64_t timeUs);

    status_t getDuration(int64_t *durationUs) const;
    status_t getTrackInfo(Parcel *reply) const;
    status_t selectTrack(size_t index, bool select);

    bool isSeekable() const;
    bool hasDynamicDuration() const;

    enum {
        kWhatStreamsChanged,
        kWhatError,
        kWhatPrepared,
        kWhatPreparationFailed,
    };

protected:
    virtual ~LiveSession();

    virtual void onMessageReceived(const sp<AMessage> &msg);

private:
    friend struct PlaylistFetcher;

    enum {
        kWhatConnect                    = 'conn',
        kWhatDisconnect                 = 'disc',
        kWhatSeek                       = 'seek',
        kWhatFetcherNotify              = 'notf',
        kWhatCheckBandwidth             = 'bndw',
        kWhatChangeConfiguration        = 'chC0',
        kWhatChangeConfiguration2       = 'chC2',
        kWhatChangeConfiguration3       = 'chC3',
        kWhatFinishDisconnect2          = 'fin2',
    };

    struct BandwidthItem {
        size_t mPlaylistIndex;
        unsigned long mBandwidth;
    };

    struct FetcherInfo {
        sp<PlaylistFetcher> mFetcher;
        int64_t mDurationUs;
        bool mIsPrepared;
    };

    sp<AMessage> mNotify;
    uint32_t mFlags;
    bool mUIDValid;
    uid_t mUID;

    bool mInPreparationPhase;

    sp<HTTPBase> mHTTPDataSource;
    KeyedVector<String8, String8> mExtraHeaders;

    AString mMasterURL;

    Vector<BandwidthItem> mBandwidthItems;
    ssize_t mPrevBandwidthIndex;

    sp<M3UParser> mPlaylist;

    KeyedVector<AString, FetcherInfo> mFetcherInfos;
    AString mAudioURI, mVideoURI, mSubtitleURI;
    uint32_t mStreamMask;

    KeyedVector<StreamType, sp<AnotherPacketSource> > mPacketSources;

    int32_t mCheckBandwidthGeneration;

    size_t mContinuationCounter;
    sp<AMessage> mContinuation;

    int64_t mLastDequeuedTimeUs;
    int64_t mRealTimeBaseUs;

    bool mReconfigurationInProgress;
    uint32_t mDisconnectReplyID;

    sp<PlaylistFetcher> addFetcher(const char *uri);

    void onConnect(const sp<AMessage> &msg);
    status_t onSeek(const sp<AMessage> &msg);
    void onFinishDisconnect2();

    status_t fetchFile(
            const char *url, sp<ABuffer> *out,
            int64_t range_offset = 0, int64_t range_length = -1);

    sp<M3UParser> fetchPlaylist(
            const char *url, uint8_t *curPlaylistHash, bool *unchanged);

    size_t getBandwidthIndex();

    static int SortByBandwidth(const BandwidthItem *, const BandwidthItem *);

    void changeConfiguration(
            int64_t timeUs, size_t bandwidthIndex, bool pickTrack = false);
    void onChangeConfiguration(const sp<AMessage> &msg);
    void onChangeConfiguration2(const sp<AMessage> &msg);
    void onChangeConfiguration3(const sp<AMessage> &msg);

    void scheduleCheckBandwidthEvent();
    void cancelCheckBandwidthEvent();

    void onCheckBandwidth();

    void finishDisconnect();

    void postPrepared(status_t err);

    DISALLOW_EVIL_CONSTRUCTORS(LiveSession);
};

}  // namespace android

#endif  // LIVE_SESSION_H_
