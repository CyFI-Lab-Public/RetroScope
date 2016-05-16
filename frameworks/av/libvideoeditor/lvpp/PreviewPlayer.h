/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef PREVIEW_PLAYER_H_

#define PREVIEW_PLAYER_H_

#include "TimedEventQueue.h"
#include "VideoEditorAudioPlayer.h"

#include <media/MediaPlayerInterface.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/TimeSource.h>
#include <utils/threads.h>
#include "NativeWindowRenderer.h"

namespace android {

struct VideoEditorAudioPlayer;
struct MediaExtractor;

struct PreviewPlayer {
    PreviewPlayer(NativeWindowRenderer* renderer);
    ~PreviewPlayer();

    void setListener(const wp<MediaPlayerBase> &listener);
    void reset();

    status_t play();
    status_t pause();

    bool isPlaying() const;
    void setSurface(const sp<Surface> &surface);
    void setSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer);
    status_t seekTo(int64_t timeUs);

    status_t getVideoDimensions(int32_t *width, int32_t *height) const;


    // FIXME: Sync between ...
    void acquireLock();
    void releaseLock();

    status_t prepare();
    status_t prepareAsync();
    status_t setDataSource(const char *path);
    status_t setDataSource(const sp<IStreamSource> &source);

    void setAudioSink(const sp<MediaPlayerBase::AudioSink> &audioSink);
    status_t setLooping(bool shouldLoop);
    status_t getDuration(int64_t *durationUs);
    status_t getPosition(int64_t *positionUs);

    uint32_t getSourceSeekFlags() const;

    void postAudioEOS(int64_t delayUs = 0ll);
    void postAudioSeekComplete();

    status_t loadEffectsSettings(M4VSS3GPP_EffectSettings* pEffectSettings,
                                 int nEffects);
    status_t loadAudioMixSettings(M4xVSS_AudioMixingSettings* pAudioMixSettings);
    status_t setAudioMixPCMFileHandle(M4OSA_Context pAudioMixPCMFileHandle);
    status_t setAudioMixStoryBoardParam(M4OSA_UInt32 audioMixStoryBoardTS,
                            M4OSA_UInt32 currentMediaBeginCutTime,
                            M4OSA_UInt32 currentMediaVolumeVol);

    status_t setPlaybackBeginTime(uint32_t msec);
    status_t setPlaybackEndTime(uint32_t msec);
    status_t setStoryboardStartTime(uint32_t msec);
    status_t setProgressCallbackInterval(uint32_t cbInterval);
    status_t setMediaRenderingMode(M4xVSS_MediaRendering mode,
                            M4VIDEOEDITING_VideoFrameSize outputVideoSize);

    status_t resetJniCallbackTimeStamp();
    status_t setImageClipProperties(uint32_t width, uint32_t height);
    status_t readFirstVideoFrame();
    status_t getLastRenderedTimeMs(uint32_t *lastRenderedTimeMs);
    status_t setAudioPlayer(VideoEditorAudioPlayer *audioPlayer);

private:
    enum {
        PLAYING             = 1,
        LOOPING             = 2,
        FIRST_FRAME         = 4,
        PREPARING           = 8,
        PREPARED            = 16,
        AT_EOS              = 32,
        PREPARE_CANCELLED   = 64,
        CACHE_UNDERRUN      = 128,
        AUDIO_AT_EOS        = 256,
        VIDEO_AT_EOS        = 512,
        AUTO_LOOPING        = 1024,
        INFORMED_AV_EOS     = 2048,

        // We are basically done preparing but are currently buffering
        // sufficient data to begin playback and finish the preparation phase
        // for good.
        PREPARING_CONNECTED = 2048,

        // We're triggering a single video event to display the first frame
        // after the seekpoint.
        SEEK_PREVIEW        = 4096,

        AUDIO_RUNNING       = 8192,
        AUDIOPLAYER_STARTED = 16384,

        INCOGNITO           = 32768,
    };

    mutable Mutex mLock;

    OMXClient mClient;
    TimedEventQueue mQueue;
    bool mQueueStarted;
    wp<MediaPlayerBase> mListener;

    sp<Surface> mSurface;
    sp<ANativeWindow> mNativeWindow;
    sp<MediaPlayerBase::AudioSink> mAudioSink;

    SystemTimeSource mSystemTimeSource;
    TimeSource *mTimeSource;

    String8 mUri;

    sp<MediaSource> mVideoTrack;
    sp<MediaSource> mVideoSource;
    bool mVideoRendererIsPreview;

    sp<MediaSource> mAudioTrack;
    sp<MediaSource> mAudioSource;
    VideoEditorAudioPlayer *mAudioPlayer;
    int64_t mDurationUs;

    int32_t mDisplayWidth;
    int32_t mDisplayHeight;

    uint32_t mFlags;
    uint32_t mExtractorFlags;

    int64_t mTimeSourceDeltaUs;
    int64_t mVideoTimeUs;

    enum SeekType {
        NO_SEEK,
        SEEK,
        SEEK_VIDEO_ONLY
    };
    SeekType mSeeking;

    bool mSeekNotificationSent;
    int64_t mSeekTimeUs;

    int64_t mBitrate;  // total bitrate of the file (in bps) or -1 if unknown.

    bool mWatchForAudioSeekComplete;
    bool mWatchForAudioEOS;

    sp<TimedEventQueue::Event> mVideoEvent;
    bool mVideoEventPending;
    sp<TimedEventQueue::Event> mStreamDoneEvent;
    bool mStreamDoneEventPending;
    sp<TimedEventQueue::Event> mCheckAudioStatusEvent;
    bool mAudioStatusEventPending;
    sp<TimedEventQueue::Event> mVideoLagEvent;
    bool mVideoLagEventPending;

    sp<TimedEventQueue::Event> mAsyncPrepareEvent;
    Condition mPreparedCondition;
    bool mIsAsyncPrepare;
    status_t mPrepareResult;
    status_t mStreamDoneStatus;

    MediaBuffer *mVideoBuffer;
    int64_t mLastVideoTimeUs;
    ARect mCropRect;
    int32_t mGivenWidth, mGivenHeight;


    bool mIsChangeSourceRequired;

    NativeWindowRenderer *mNativeWindowRenderer;
    RenderInput *mVideoRenderer;

    int32_t mVideoWidth, mVideoHeight;

    //Data structures used for audio and video effects
    M4VSS3GPP_EffectSettings* mEffectsSettings;
    M4xVSS_AudioMixingSettings* mPreviewPlayerAudioMixSettings;
    M4OSA_Context mAudioMixPCMFileHandle;
    M4OSA_UInt32 mAudioMixStoryBoardTS;
    M4OSA_UInt32 mCurrentMediaBeginCutTime;
    M4OSA_UInt32 mCurrentMediaVolumeValue;
    M4OSA_UInt32 mCurrFramingEffectIndex;

    uint32_t mNumberEffects;
    uint32_t mPlayBeginTimeMsec;
    uint32_t mPlayEndTimeMsec;
    uint64_t mDecodedVideoTs; // timestamp of current decoded video frame buffer
    uint64_t mDecVideoTsStoryBoard; // timestamp of frame relative to storyboard
    uint32_t mCurrentVideoEffect;
    uint32_t mProgressCbInterval;
    uint32_t mNumberDecVideoFrames; // Counter of number of video frames decoded
    sp<TimedEventQueue::Event> mProgressCbEvent;
    bool mProgressCbEventPending;
    sp<TimedEventQueue::Event> mOverlayUpdateEvent;
    bool mOverlayUpdateEventPending;
    bool mOverlayUpdateEventPosted;

    M4xVSS_MediaRendering mRenderingMode;
    uint32_t mOutputVideoWidth;
    uint32_t mOutputVideoHeight;

    uint32_t mStoryboardStartTimeMsec;

    bool mIsVideoSourceJpg;
    bool mIsFiftiesEffectStarted;
    int64_t mImageFrameTimeUs;
    bool mStartNextPlayer;
    mutable Mutex mLockControl;

    M4VIFI_UInt8*  mFrameRGBBuffer;
    M4VIFI_UInt8*  mFrameYUVBuffer;

    void cancelPlayerEvents_l(bool updateProgressCb = false);
    status_t setDataSource_l(const sp<MediaExtractor> &extractor);
    status_t setDataSource_l(const char *path);
    void setNativeWindow_l(const sp<ANativeWindow> &native);
    void reset_l();
    void clear_l();
    status_t play_l();
    status_t pause_l(bool at_eos = false);
    status_t initRenderer_l();
    status_t initAudioDecoder_l();
    status_t initVideoDecoder_l(uint32_t flags = 0);
    void notifyVideoSize_l();
    void notifyListener_l(int msg, int ext1 = 0, int ext2 = 0);
    void onVideoEvent();
    void onVideoLagUpdate();
    void onStreamDone();
    void onCheckAudioStatus();
    void onPrepareAsyncEvent();

    void finishAsyncPrepare_l();
    void abortPrepare(status_t err);

    status_t startAudioPlayer_l();
    void setVideoSource(const sp<MediaSource>& source);
    status_t finishSetDataSource_l();
    void setAudioSource(const sp<MediaSource>& source);

    status_t seekTo_l(int64_t timeUs);
    void seekAudioIfNecessary_l();
    void finishSeekIfNecessary(int64_t videoTimeUs);

    void postCheckAudioStatusEvent_l(int64_t delayUs);
    void postVideoLagEvent_l();
    void postStreamDoneEvent_l(status_t status);
    void postVideoEvent_l(int64_t delayUs = -1);
    void setVideoPostProcessingNode(
                    M4VSS3GPP_VideoEffectType type, M4OSA_Bool enable);
    void postProgressCallbackEvent_l();
    void shutdownVideoDecoder_l();
    void onProgressCbEvent();

    void postOverlayUpdateEvent_l();
    void onUpdateOverlayEvent();

    status_t setDataSource_l_jpg();
    status_t prepare_l();
    status_t prepareAsync_l();
    void updateBatteryUsage_l();
    void updateSizeToRender(sp<MetaData> meta);

    void setDuration_l(int64_t durationUs);
    void setPosition_l(int64_t timeUs);

    PreviewPlayer(const PreviewPlayer &);
    PreviewPlayer &operator=(const PreviewPlayer &);
};

}  // namespace android

#endif  // PREVIEW_PLAYER_H_

