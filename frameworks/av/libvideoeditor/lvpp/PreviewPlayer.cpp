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


// #define LOG_NDEBUG 0
#define LOG_TAG "PreviewPlayer"
#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>
#include <media/IMediaPlayerService.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXCodec.h>
#include <media/stagefright/foundation/ADebug.h>
#include <gui/Surface.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>

#include "VideoEditorPreviewController.h"
#include "DummyAudioSource.h"
#include "DummyVideoSource.h"
#include "VideoEditorSRC.h"
#include "PreviewPlayer.h"

namespace android {


void addBatteryData(uint32_t params) {
    sp<IBinder> binder =
        defaultServiceManager()->getService(String16("media.player"));
    sp<IMediaPlayerService> service = interface_cast<IMediaPlayerService>(binder);
    CHECK(service.get() != NULL);

    service->addBatteryData(params);
}

struct PreviewPlayerEvent : public TimedEventQueue::Event {
    PreviewPlayerEvent(
            PreviewPlayer *player,
            void (PreviewPlayer::*method)())
        : mPlayer(player),
          mMethod(method) {
    }

protected:
    virtual ~PreviewPlayerEvent() {}

    virtual void fire(TimedEventQueue *queue, int64_t /* now_us */) {
        (mPlayer->*mMethod)();
    }

private:
    PreviewPlayer *mPlayer;
    void (PreviewPlayer::*mMethod)();

    PreviewPlayerEvent(const PreviewPlayerEvent &);
    PreviewPlayerEvent &operator=(const PreviewPlayerEvent &);
};

PreviewPlayer::PreviewPlayer(NativeWindowRenderer* renderer)
    : mQueueStarted(false),
      mTimeSource(NULL),
      mVideoRendererIsPreview(false),
      mAudioPlayer(NULL),
      mDisplayWidth(0),
      mDisplayHeight(0),
      mFlags(0),
      mExtractorFlags(0),
      mVideoBuffer(NULL),
      mLastVideoTimeUs(-1),
      mNativeWindowRenderer(renderer),
      mCurrFramingEffectIndex(0),
      mFrameRGBBuffer(NULL),
      mFrameYUVBuffer(NULL) {

    CHECK_EQ(mClient.connect(), (status_t)OK);
    DataSource::RegisterDefaultSniffers();


    mVideoRenderer = NULL;
    mEffectsSettings = NULL;
    mAudioPlayer = NULL;
    mAudioMixStoryBoardTS = 0;
    mCurrentMediaBeginCutTime = 0;
    mCurrentMediaVolumeValue = 0;
    mNumberEffects = 0;
    mDecodedVideoTs = 0;
    mDecVideoTsStoryBoard = 0;
    mCurrentVideoEffect = VIDEO_EFFECT_NONE;
    mProgressCbInterval = 0;
    mNumberDecVideoFrames = 0;
    mOverlayUpdateEventPosted = false;
    mIsChangeSourceRequired = true;

    mVideoEvent = new PreviewPlayerEvent(this, &PreviewPlayer::onVideoEvent);
    mVideoEventPending = false;
    mVideoLagEvent = new PreviewPlayerEvent(this, &PreviewPlayer::onVideoLagUpdate);
    mVideoEventPending = false;
    mCheckAudioStatusEvent = new PreviewPlayerEvent(
            this, &PreviewPlayer::onCheckAudioStatus);
    mAudioStatusEventPending = false;
    mStreamDoneEvent = new PreviewPlayerEvent(
            this, &PreviewPlayer::onStreamDone);
    mStreamDoneEventPending = false;
    mProgressCbEvent = new PreviewPlayerEvent(this,
         &PreviewPlayer::onProgressCbEvent);

    mOverlayUpdateEvent = new PreviewPlayerEvent(this,
        &PreviewPlayer::onUpdateOverlayEvent);
    mProgressCbEventPending = false;

    mOverlayUpdateEventPending = false;
    mRenderingMode = (M4xVSS_MediaRendering)MEDIA_RENDERING_INVALID;
    mIsFiftiesEffectStarted = false;
    reset();
}

PreviewPlayer::~PreviewPlayer() {

    if (mQueueStarted) {
        mQueue.stop();
    }

    reset();

    if (mVideoRenderer) {
        mNativeWindowRenderer->destroyRenderInput(mVideoRenderer);
    }

    Mutex::Autolock lock(mLock);
    clear_l();
    mClient.disconnect();
}

void PreviewPlayer::cancelPlayerEvents_l(bool updateProgressCb) {
    mQueue.cancelEvent(mVideoEvent->eventID());
    mVideoEventPending = false;
    mQueue.cancelEvent(mStreamDoneEvent->eventID());
    mStreamDoneEventPending = false;
    mQueue.cancelEvent(mCheckAudioStatusEvent->eventID());
    mAudioStatusEventPending = false;
    mQueue.cancelEvent(mVideoLagEvent->eventID());
    mVideoLagEventPending = false;
    if (updateProgressCb) {
        mQueue.cancelEvent(mProgressCbEvent->eventID());
        mProgressCbEventPending = false;
    }
}

status_t PreviewPlayer::setDataSource(const char *path) {
    Mutex::Autolock autoLock(mLock);
    return setDataSource_l(path);
}

status_t PreviewPlayer::setDataSource_l(const char *path) {
    reset_l();

    mUri = path;

    // The actual work will be done during preparation in the call to
    // ::finishSetDataSource_l to avoid blocking the calling thread in
    // setDataSource for any significant time.
    return OK;
}

status_t PreviewPlayer::setDataSource_l(const sp<MediaExtractor> &extractor) {
    bool haveAudio = false;
    bool haveVideo = false;
    for (size_t i = 0; i < extractor->countTracks(); ++i) {
        sp<MetaData> meta = extractor->getTrackMetaData(i);

        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!haveVideo && !strncasecmp(mime, "video/", 6)) {
            setVideoSource(extractor->getTrack(i));
            haveVideo = true;
        } else if (!haveAudio && !strncasecmp(mime, "audio/", 6)) {
            setAudioSource(extractor->getTrack(i));
            haveAudio = true;

            if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_VORBIS)) {
                // Only do this for vorbis audio, none of the other audio
                // formats even support this ringtone specific hack and
                // retrieving the metadata on some extractors may turn out
                // to be very expensive.
                sp<MetaData> fileMeta = extractor->getMetaData();
                int32_t loop;
                if (fileMeta != NULL
                        && fileMeta->findInt32(kKeyAutoLoop, &loop)
                         && loop != 0) {
                    mFlags |= AUTO_LOOPING;
                }
            }
        }

        if (haveAudio && haveVideo) {
            break;
        }
    }

    /* Add the support for Dummy audio*/
    if( !haveAudio ){
        mAudioTrack = DummyAudioSource::Create(32000, 2, 20000,
                                              ((mPlayEndTimeMsec)*1000LL));
        if(mAudioTrack != NULL) {
            haveAudio = true;
        }
    }

    if (!haveAudio && !haveVideo) {
        return UNKNOWN_ERROR;
    }

    mExtractorFlags = extractor->flags();
    return OK;
}

status_t PreviewPlayer::setDataSource_l_jpg() {
    ALOGV("setDataSource_l_jpg");

    M4OSA_ERR err = M4NO_ERROR;

    mAudioSource = DummyAudioSource::Create(32000, 2, 20000,
                                          ((mPlayEndTimeMsec)*1000LL));
    if(mAudioSource != NULL) {
        setAudioSource(mAudioSource);
    }
    status_t error = mAudioSource->start();
    if (error != OK) {
        ALOGE("Error starting dummy audio source");
        mAudioSource.clear();
        return err;
    }

    mDurationUs = (mPlayEndTimeMsec - mPlayBeginTimeMsec)*1000LL;

    mVideoSource = DummyVideoSource::Create(mVideoWidth, mVideoHeight,
                                            mDurationUs, mUri);

    updateSizeToRender(mVideoSource->getFormat());
    setVideoSource(mVideoSource);
    status_t err1 = mVideoSource->start();
    if (err1 != OK) {
        mVideoSource.clear();
        return err;
    }

    mIsVideoSourceJpg = true;
    return OK;
}

void PreviewPlayer::reset_l() {

    if (mFlags & PREPARING) {
        mFlags |= PREPARE_CANCELLED;
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    cancelPlayerEvents_l();
    mAudioTrack.clear();
    mVideoTrack.clear();

    // Shutdown audio first, so that the respone to the reset request
    // appears to happen instantaneously as far as the user is concerned
    // If we did this later, audio would continue playing while we
    // shutdown the video-related resources and the player appear to
    // not be as responsive to a reset request.
    if (mAudioPlayer == NULL && mAudioSource != NULL) {
        // If we had an audio player, it would have effectively
        // taken possession of the audio source and stopped it when
        // _it_ is stopped. Otherwise this is still our responsibility.
        mAudioSource->stop();
    }
    mAudioSource.clear();

    mTimeSource = NULL;

    //Single audio player instance used
    //So donot delete it here
    //It is deleted from PreviewController class
    //delete mAudioPlayer;
    mAudioPlayer = NULL;

    if (mVideoBuffer) {
        mVideoBuffer->release();
        mVideoBuffer = NULL;
    }

    if (mVideoSource != NULL) {
        mVideoSource->stop();

        // The following hack is necessary to ensure that the OMX
        // component is completely released by the time we may try
        // to instantiate it again.
        wp<MediaSource> tmp = mVideoSource;
        mVideoSource.clear();
        while (tmp.promote() != NULL) {
            usleep(1000);
        }
        IPCThreadState::self()->flushCommands();
    }

    mDurationUs = -1;
    mFlags = 0;
    mExtractorFlags = 0;
    mVideoWidth = mVideoHeight = -1;
    mTimeSourceDeltaUs = 0;
    mVideoTimeUs = 0;

    mSeeking = NO_SEEK;
    mSeekNotificationSent = false;
    mSeekTimeUs = 0;

    mUri.setTo("");

    mCurrentVideoEffect = VIDEO_EFFECT_NONE;
    mIsVideoSourceJpg = false;
    mFrameRGBBuffer = NULL;
    if(mFrameYUVBuffer != NULL) {
        free(mFrameYUVBuffer);
        mFrameYUVBuffer = NULL;
    }
}

status_t PreviewPlayer::play() {
    ALOGV("play");
    Mutex::Autolock autoLock(mLock);

    mFlags &= ~CACHE_UNDERRUN;
    mFlags &= ~INFORMED_AV_EOS;
    return play_l();
}

status_t PreviewPlayer::startAudioPlayer_l() {
    ALOGV("startAudioPlayer_l");
    CHECK(!(mFlags & AUDIO_RUNNING));

    if (mAudioSource == NULL || mAudioPlayer == NULL) {
        return OK;
    }

    if (!(mFlags & AUDIOPLAYER_STARTED)) {
        mFlags |= AUDIOPLAYER_STARTED;

        // We've already started the MediaSource in order to enable
        // the prefetcher to read its data.
        status_t err = mAudioPlayer->start(
                true /* sourceAlreadyStarted */);

        if (err != OK) {
            notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
            return err;
        }
    } else {
        mAudioPlayer->resume();
    }

    mFlags |= AUDIO_RUNNING;

    mWatchForAudioEOS = true;

    return OK;
}

status_t PreviewPlayer::setAudioPlayer(VideoEditorAudioPlayer *audioPlayer) {
    ALOGV("setAudioPlayer");
    Mutex::Autolock autoLock(mLock);
    CHECK(!(mFlags & PLAYING));
    mAudioPlayer = audioPlayer;

    ALOGV("SetAudioPlayer");
    mIsChangeSourceRequired = true;

    // check if the new and old source are dummy
    sp<MediaSource> anAudioSource = mAudioPlayer->getSource();
    if (anAudioSource == NULL) {
        // Audio player does not have any source set.
        ALOGV("setAudioPlayer: Audio player does not have any source set");
        return OK;
    }

    // If new video source is not dummy, then always change source
    // Else audio player continues using old audio source and there are
    // frame drops to maintain AV sync
    sp<MetaData> meta;
    if (mVideoSource != NULL) {
        meta = mVideoSource->getFormat();
        const char *pVidSrcType;
        if (meta->findCString(kKeyDecoderComponent, &pVidSrcType)) {
            if (strcmp(pVidSrcType, "DummyVideoSource") != 0) {
                ALOGV(" Video clip with silent audio; need to change source");
                return OK;
            }
        }
    }

    const char *pSrcType1;
    const char *pSrcType2;
    meta = anAudioSource->getFormat();

    if (meta->findCString(kKeyDecoderComponent, &pSrcType1)) {
        if (strcmp(pSrcType1, "DummyAudioSource") == 0) {
            meta = mAudioSource->getFormat();
            if (meta->findCString(kKeyDecoderComponent, &pSrcType2)) {
                if (strcmp(pSrcType2, "DummyAudioSource") == 0) {
                    mIsChangeSourceRequired = false;
                    // Just set the new play duration for the existing source
                    MediaSource *pMediaSrc = anAudioSource.get();
                    DummyAudioSource *pDummyAudioSource = (DummyAudioSource*)pMediaSrc;
                    //Increment the duration of audio source
                    pDummyAudioSource->setDuration(
                        (int64_t)((mPlayEndTimeMsec)*1000LL));

                    // Stop the new audio source
                    // since we continue using old source
                    ALOGV("setAudioPlayer: stop new audio source");
                    mAudioSource->stop();
                }
            }
        }
    }

    return OK;
}

void PreviewPlayer::onStreamDone() {
    ALOGV("onStreamDone");
    // Posted whenever any stream finishes playing.

    Mutex::Autolock autoLock(mLock);
    if (!mStreamDoneEventPending) {
        return;
    }
    mStreamDoneEventPending = false;

    if (mStreamDoneStatus != ERROR_END_OF_STREAM) {
        ALOGV("MEDIA_ERROR %d", mStreamDoneStatus);

        notifyListener_l(
                MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, mStreamDoneStatus);

        pause_l(true /* at eos */);

        mFlags |= AT_EOS;
        return;
    }

    const bool allDone =
        (mVideoSource == NULL || (mFlags & VIDEO_AT_EOS))
            && (mAudioSource == NULL || (mFlags & AUDIO_AT_EOS));

    if (!allDone) {
        return;
    }

    if (mFlags & (LOOPING | AUTO_LOOPING)) {
        seekTo_l(0);

        if (mVideoSource != NULL) {
            postVideoEvent_l();
        }
    } else {
        ALOGV("MEDIA_PLAYBACK_COMPLETE");
        //pause before sending event
        pause_l(true /* at eos */);

        //This lock is used to syncronize onStreamDone() in PreviewPlayer and
        //stopPreview() in PreviewController
        Mutex::Autolock autoLock(mLockControl);
        /* Make sure PreviewPlayer only notifies MEDIA_PLAYBACK_COMPLETE once for each clip!
         * It happens twice in following scenario.
         * To make the clips in preview storyboard are played and switched smoothly,
         * PreviewController uses two PreviewPlayer instances and one AudioPlayer.
         * The two PreviewPlayer use the same AudioPlayer to play the audio,
         * and change the audio source of the AudioPlayer.
         * If the audio source of current playing clip and next clip are dummy
         * audio source(image or video without audio), it will not change the audio source
         * to avoid the "audio glitch", and keep using the current audio source.
         * When the video of current clip reached the EOS, PreviewPlayer will set EOS flag
         * for video and audio, and it will notify MEDIA_PLAYBACK_COMPLETE.
         * But the audio(dummy audio source) is still playing(for next clip),
         * and when it reached the EOS, and video reached EOS,
         * PreviewPlayer will notify MEDIA_PLAYBACK_COMPLETE again. */
        if (!(mFlags & INFORMED_AV_EOS)) {
            notifyListener_l(MEDIA_PLAYBACK_COMPLETE);
            mFlags |= INFORMED_AV_EOS;
        }
        mFlags |= AT_EOS;
        ALOGV("onStreamDone end");
        return;
    }
}


status_t PreviewPlayer::play_l() {
    ALOGV("play_l");

    mFlags &= ~SEEK_PREVIEW;

    if (mFlags & PLAYING) {
        return OK;
    }
    mStartNextPlayer = false;

    if (!(mFlags & PREPARED)) {
        status_t err = prepare_l();

        if (err != OK) {
            return err;
        }
    }

    mFlags |= PLAYING;
    mFlags |= FIRST_FRAME;

    bool deferredAudioSeek = false;

    if (mAudioSource != NULL) {
        if (mAudioPlayer == NULL) {
            if (mAudioSink != NULL) {

                mAudioPlayer = new VideoEditorAudioPlayer(mAudioSink, this);
                mAudioPlayer->setSource(mAudioSource);

                mAudioPlayer->setAudioMixSettings(
                 mPreviewPlayerAudioMixSettings);

                mAudioPlayer->setAudioMixPCMFileHandle(
                 mAudioMixPCMFileHandle);

                mAudioPlayer->setAudioMixStoryBoardSkimTimeStamp(
                 mAudioMixStoryBoardTS, mCurrentMediaBeginCutTime,
                 mCurrentMediaVolumeValue);

                 mFlags |= AUDIOPLAYER_STARTED;
                // We've already started the MediaSource in order to enable
                // the prefetcher to read its data.
                status_t err = mAudioPlayer->start(
                        true /* sourceAlreadyStarted */);

                if (err != OK) {
                    //delete mAudioPlayer;
                    mAudioPlayer = NULL;

                    mFlags &= ~(PLAYING | FIRST_FRAME);
                    return err;
                }

                mTimeSource = mAudioPlayer;
                mFlags |= AUDIO_RUNNING;
                deferredAudioSeek = true;
                mWatchForAudioSeekComplete = false;
                mWatchForAudioEOS = true;
            }
        } else {
            bool isAudioPlayerStarted = mAudioPlayer->isStarted();

            if (mIsChangeSourceRequired == true) {
                ALOGV("play_l: Change audio source required");

                if (isAudioPlayerStarted == true) {
                    mAudioPlayer->pause();
                }

                mAudioPlayer->setSource(mAudioSource);
                mAudioPlayer->setObserver(this);

                mAudioPlayer->setAudioMixSettings(
                 mPreviewPlayerAudioMixSettings);

                mAudioPlayer->setAudioMixStoryBoardSkimTimeStamp(
                    mAudioMixStoryBoardTS, mCurrentMediaBeginCutTime,
                    mCurrentMediaVolumeValue);

                if (isAudioPlayerStarted == true) {
                    mAudioPlayer->resume();
                } else {
                    status_t err = OK;
                    err = mAudioPlayer->start(true);
                    if (err != OK) {
                        mAudioPlayer = NULL;
                        mAudioPlayer = NULL;

                        mFlags &= ~(PLAYING | FIRST_FRAME);
                        return err;
                    }
                }
            } else {
                ALOGV("play_l: No Source change required");
                mAudioPlayer->setAudioMixStoryBoardSkimTimeStamp(
                    mAudioMixStoryBoardTS, mCurrentMediaBeginCutTime,
                    mCurrentMediaVolumeValue);

                mAudioPlayer->resume();
            }

            mFlags |= AUDIOPLAYER_STARTED;
            mFlags |= AUDIO_RUNNING;
            mTimeSource = mAudioPlayer;
            deferredAudioSeek = true;
            mWatchForAudioSeekComplete = false;
            mWatchForAudioEOS = true;
        }
    }

    if (mTimeSource == NULL && mAudioPlayer == NULL) {
        mTimeSource = &mSystemTimeSource;
    }

    // Set the seek option for Image source files and read.
    // This resets the timestamping for image play
    if (mIsVideoSourceJpg) {
        MediaSource::ReadOptions options;
        MediaBuffer *aLocalBuffer;
        options.setSeekTo(mSeekTimeUs);
        mVideoSource->read(&aLocalBuffer, &options);
        aLocalBuffer->release();
    }

    if (mVideoSource != NULL) {
        // Kick off video playback
        postVideoEvent_l();
    }

    if (deferredAudioSeek) {
        // If there was a seek request while we were paused
        // and we're just starting up again, honor the request now.
        seekAudioIfNecessary_l();
    }

    if (mFlags & AT_EOS) {
        // Legacy behaviour, if a stream finishes playing and then
        // is started again, we play from the start...
        seekTo_l(0);
    }

    return OK;
}


status_t PreviewPlayer::initRenderer_l() {
    if (mSurface != NULL) {
        if(mVideoRenderer == NULL) {
            mVideoRenderer = mNativeWindowRenderer->createRenderInput();
            if (mVideoSource != NULL) {
                updateSizeToRender(mVideoSource->getFormat());
            }
        }
    }
    return OK;
}


status_t PreviewPlayer::seekTo(int64_t timeUs) {
    Mutex::Autolock autoLock(mLock);
    if ((mExtractorFlags & MediaExtractor::CAN_SEEK) || (mIsVideoSourceJpg)) {
        return seekTo_l(timeUs);
    }

    return OK;
}


status_t PreviewPlayer::getVideoDimensions(
        int32_t *width, int32_t *height) const {
    Mutex::Autolock autoLock(mLock);

    if (mVideoWidth < 0 || mVideoHeight < 0) {
        return UNKNOWN_ERROR;
    }

    *width = mVideoWidth;
    *height = mVideoHeight;

    return OK;
}


status_t PreviewPlayer::initAudioDecoder_l() {
    sp<MetaData> meta = mAudioTrack->getFormat();
    const char *mime;
    CHECK(meta->findCString(kKeyMIMEType, &mime));

    if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW)) {
        mAudioSource = mAudioTrack;
    } else {
        sp<MediaSource> aRawSource;
        aRawSource = OMXCodec::Create(
                mClient.interface(), mAudioTrack->getFormat(),
                false, // createEncoder
                mAudioTrack);

        if(aRawSource != NULL) {
            mAudioSource = new VideoEditorSRC(aRawSource);
        }
    }

    if (mAudioSource != NULL) {
        int64_t durationUs;
        if (mAudioTrack->getFormat()->findInt64(kKeyDuration, &durationUs)) {
            setDuration_l(durationUs);
        }
        status_t err = mAudioSource->start();

        if (err != OK) {
            mAudioSource.clear();
            return err;
        }
    } else if (!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_QCELP)) {
        // For legacy reasons we're simply going to ignore the absence
        // of an audio decoder for QCELP instead of aborting playback
        // altogether.
        return OK;
    }

    return mAudioSource != NULL ? OK : UNKNOWN_ERROR;
}

status_t PreviewPlayer::initVideoDecoder_l(uint32_t flags) {
    initRenderer_l();

    if (mVideoRenderer == NULL) {
        ALOGE("Cannot create renderer");
        return UNKNOWN_ERROR;
    }

    mVideoSource = OMXCodec::Create(
            mClient.interface(), mVideoTrack->getFormat(),
            false,
            mVideoTrack,
            NULL, flags, mVideoRenderer->getTargetWindow());

    if (mVideoSource != NULL) {
        int64_t durationUs;
        if (mVideoTrack->getFormat()->findInt64(kKeyDuration, &durationUs)) {
            setDuration_l(durationUs);
        }

        updateSizeToRender(mVideoTrack->getFormat());

        status_t err = mVideoSource->start();

        if (err != OK) {
            mVideoSource.clear();
            return err;
        }
    }

    return mVideoSource != NULL ? OK : UNKNOWN_ERROR;
}


void PreviewPlayer::onVideoEvent() {
    uint32_t i=0;
    M4OSA_ERR err1 = M4NO_ERROR;
    int64_t imageFrameTimeUs = 0;

    Mutex::Autolock autoLock(mLock);
    if (!mVideoEventPending) {
        // The event has been cancelled in reset_l() but had already
        // been scheduled for execution at that time.
        return;
    }
    mVideoEventPending = false;

    if (mFlags & SEEK_PREVIEW) {
        mFlags &= ~SEEK_PREVIEW;
        return;
    }

    TimeSource *ts_st =  &mSystemTimeSource;
    int64_t timeStartUs = ts_st->getRealTimeUs();

    if (mSeeking != NO_SEEK) {

        if(mAudioSource != NULL) {

            // We're going to seek the video source first, followed by
            // the audio source.
            // In order to avoid jumps in the DataSource offset caused by
            // the audio codec prefetching data from the old locations
            // while the video codec is already reading data from the new
            // locations, we'll "pause" the audio source, causing it to
            // stop reading input data until a subsequent seek.

            if (mAudioPlayer != NULL && (mFlags & AUDIO_RUNNING)) {
                mAudioPlayer->pause();
                mFlags &= ~AUDIO_RUNNING;
            }
            mAudioSource->pause();
        }
    }

    if (!mVideoBuffer) {
        MediaSource::ReadOptions options;
        if (mSeeking != NO_SEEK) {
            ALOGV("LV PLAYER seeking to %lld us (%.2f secs)", mSeekTimeUs,
                                                      mSeekTimeUs / 1E6);

            options.setSeekTo(
                    mSeekTimeUs, MediaSource::ReadOptions::SEEK_CLOSEST);
        }
        for (;;) {
            status_t err = mVideoSource->read(&mVideoBuffer, &options);
            options.clearSeekTo();

            if (err != OK) {
                CHECK(!mVideoBuffer);

                if (err == INFO_FORMAT_CHANGED) {
                    ALOGV("LV PLAYER VideoSource signalled format change");
                    notifyVideoSize_l();

                    if (mVideoRenderer != NULL) {
                        mVideoRendererIsPreview = false;
                        err = initRenderer_l();
                        if (err != OK) {
                            postStreamDoneEvent_l(err);
                        }

                    }

                    updateSizeToRender(mVideoSource->getFormat());
                    continue;
                }
                // So video playback is complete, but we may still have
                // a seek request pending that needs to be applied to the audio track
                if (mSeeking != NO_SEEK) {
                    ALOGV("video stream ended while seeking!");
                }
                finishSeekIfNecessary(-1);
                ALOGV("PreviewPlayer: onVideoEvent EOS reached.");
                mFlags |= VIDEO_AT_EOS;
                mFlags |= AUDIO_AT_EOS;
                mOverlayUpdateEventPosted = false;
                postStreamDoneEvent_l(err);
                // Set the last decoded timestamp to duration
                mDecodedVideoTs = (mPlayEndTimeMsec*1000LL);
                return;
            }

            if (mVideoBuffer->range_length() == 0) {
                // Some decoders, notably the PV AVC software decoder
                // return spurious empty buffers that we just want to ignore.

                mVideoBuffer->release();
                mVideoBuffer = NULL;
                continue;
            }

            int64_t videoTimeUs;
            CHECK(mVideoBuffer->meta_data()->findInt64(kKeyTime, &videoTimeUs));

            if (mSeeking != NO_SEEK) {
                if (videoTimeUs < mSeekTimeUs) {
                    // buffers are before seek time
                    // ignore them
                    mVideoBuffer->release();
                    mVideoBuffer = NULL;
                    continue;
                }
            } else {
                if((videoTimeUs/1000) < mPlayBeginTimeMsec) {
                    // Frames are before begin cut time
                    // Donot render
                    mVideoBuffer->release();
                    mVideoBuffer = NULL;
                    continue;
                }
            }
            break;
        }
    }

    mNumberDecVideoFrames++;

    int64_t timeUs;
    CHECK(mVideoBuffer->meta_data()->findInt64(kKeyTime, &timeUs));
    setPosition_l(timeUs);

    if (!mStartNextPlayer) {
        int64_t playbackTimeRemaining = (mPlayEndTimeMsec * 1000LL) - timeUs;
        if (playbackTimeRemaining <= 1500000) {
            //When less than 1.5 sec of playback left
            // send notification to start next player

            mStartNextPlayer = true;
            notifyListener_l(0xAAAAAAAA);
        }
    }

    SeekType wasSeeking = mSeeking;
    finishSeekIfNecessary(timeUs);
    if (mAudioPlayer != NULL && !(mFlags & (AUDIO_RUNNING))) {
        status_t err = startAudioPlayer_l();
        if (err != OK) {
            ALOGE("Starting the audio player failed w/ err %d", err);
            return;
        }
    }

    TimeSource *ts = (mFlags & AUDIO_AT_EOS) ? &mSystemTimeSource : mTimeSource;

    if(ts == NULL) {
        mVideoBuffer->release();
        mVideoBuffer = NULL;
        return;
    }

    if(!mIsVideoSourceJpg) {
        if (mFlags & FIRST_FRAME) {
            mFlags &= ~FIRST_FRAME;

            mTimeSourceDeltaUs = ts->getRealTimeUs() - timeUs;
        }

        int64_t realTimeUs, mediaTimeUs;
        if (!(mFlags & AUDIO_AT_EOS) && mAudioPlayer != NULL
            && mAudioPlayer->getMediaTimeMapping(&realTimeUs, &mediaTimeUs)) {
            mTimeSourceDeltaUs = realTimeUs - mediaTimeUs;
        }

        int64_t nowUs = ts->getRealTimeUs() - mTimeSourceDeltaUs;

        int64_t latenessUs = nowUs - timeUs;

        if (wasSeeking != NO_SEEK) {
            // Let's display the first frame after seeking right away.
            latenessUs = 0;
        }
        ALOGV("Audio time stamp = %lld and video time stamp = %lld",
                                            ts->getRealTimeUs(),timeUs);
        if (latenessUs > 40000) {
            // We're more than 40ms late.

            ALOGV("LV PLAYER we're late by %lld us (%.2f secs)",
                                           latenessUs, latenessUs / 1E6);

            mVideoBuffer->release();
            mVideoBuffer = NULL;
            postVideoEvent_l(0);
            return;
        }

        if (latenessUs < -25000) {
            // We're more than 25ms early.
            ALOGV("We're more than 25ms early, lateness %lld", latenessUs);

            postVideoEvent_l(25000);
            return;
        }
    }

    if (mVideoRendererIsPreview || mVideoRenderer == NULL) {
        mVideoRendererIsPreview = false;

        status_t err = initRenderer_l();
        if (err != OK) {
            postStreamDoneEvent_l(err);
        }
    }

    // If timestamp exceeds endCutTime of clip, donot render
    if((timeUs/1000) > mPlayEndTimeMsec) {
        mVideoBuffer->release();
        mVideoBuffer = NULL;
        mFlags |= VIDEO_AT_EOS;
        mFlags |= AUDIO_AT_EOS;
        ALOGV("PreviewPlayer: onVideoEvent timeUs > mPlayEndTime; send EOS..");
        mOverlayUpdateEventPosted = false;
        // Set the last decoded timestamp to duration
        mDecodedVideoTs = (mPlayEndTimeMsec*1000LL);
        postStreamDoneEvent_l(ERROR_END_OF_STREAM);
        return;
    }
    // Capture the frame timestamp to be rendered
    mDecodedVideoTs = timeUs;

    // Post processing to apply video effects
    for(i=0;i<mNumberEffects;i++) {
        // First check if effect starttime matches the clip being previewed
        if((mEffectsSettings[i].uiStartTime < (mDecVideoTsStoryBoard/1000)) ||
        (mEffectsSettings[i].uiStartTime >=
         ((mDecVideoTsStoryBoard/1000) + mPlayEndTimeMsec - mPlayBeginTimeMsec)))
        {
            // This effect doesn't belong to this clip, check next one
            continue;
        }
        // Check if effect applies to this particular frame timestamp
        if((mEffectsSettings[i].uiStartTime <=
         (((timeUs+mDecVideoTsStoryBoard)/1000)-mPlayBeginTimeMsec)) &&
            ((mEffectsSettings[i].uiStartTime+mEffectsSettings[i].uiDuration) >=
             (((timeUs+mDecVideoTsStoryBoard)/1000)-mPlayBeginTimeMsec))
              && (mEffectsSettings[i].uiDuration != 0)) {
            setVideoPostProcessingNode(
             mEffectsSettings[i].VideoEffectType, TRUE);
        }
        else {
            setVideoPostProcessingNode(
             mEffectsSettings[i].VideoEffectType, FALSE);
        }
    }

    //Provide the overlay Update indication when there is an overlay effect
    if (mCurrentVideoEffect & VIDEO_EFFECT_FRAMING) {
        mCurrentVideoEffect &= ~VIDEO_EFFECT_FRAMING; //never apply framing here.
        if (!mOverlayUpdateEventPosted) {
            // Find the effect in effectSettings array
            M4OSA_UInt32 index;
            for (index = 0; index < mNumberEffects; index++) {
                M4OSA_UInt32 timeMs = mDecodedVideoTs/1000;
                M4OSA_UInt32 timeOffset = mDecVideoTsStoryBoard/1000;
                if(mEffectsSettings[index].VideoEffectType ==
                    (M4VSS3GPP_VideoEffectType)M4xVSS_kVideoEffectType_Framing) {
                    if (((mEffectsSettings[index].uiStartTime + 1) <=
                        timeMs + timeOffset - mPlayBeginTimeMsec) &&
                        ((mEffectsSettings[index].uiStartTime - 1 +
                        mEffectsSettings[index].uiDuration) >=
                        timeMs + timeOffset - mPlayBeginTimeMsec))
                    {
                        break;
                    }
                }
            }
            if (index < mNumberEffects) {
                mCurrFramingEffectIndex = index;
                mOverlayUpdateEventPosted = true;
                postOverlayUpdateEvent_l();
                ALOGV("Framing index = %ld", mCurrFramingEffectIndex);
            } else {
                ALOGV("No framing effects found");
            }
        }

    } else if (mOverlayUpdateEventPosted) {
        //Post the event when the overlay is no more valid
        ALOGV("Overlay is Done");
        mOverlayUpdateEventPosted = false;
        postOverlayUpdateEvent_l();
    }

    if (mVideoRenderer != NULL) {
        mVideoRenderer->render(mVideoBuffer, mCurrentVideoEffect,
                mRenderingMode, mIsVideoSourceJpg);
    }

    mVideoBuffer->release();
    mVideoBuffer = NULL;

    // Post progress callback based on callback interval set
    if(mNumberDecVideoFrames >= mProgressCbInterval) {
        postProgressCallbackEvent_l();
        mNumberDecVideoFrames = 0;  // reset counter
    }

    // if reached EndCutTime of clip, post EOS event
    if((timeUs/1000) >= mPlayEndTimeMsec) {
        ALOGV("PreviewPlayer: onVideoEvent EOS.");
        mFlags |= VIDEO_AT_EOS;
        mFlags |= AUDIO_AT_EOS;
        mOverlayUpdateEventPosted = false;
        // Set the last decoded timestamp to duration
        mDecodedVideoTs = (mPlayEndTimeMsec*1000LL);
        postStreamDoneEvent_l(ERROR_END_OF_STREAM);
    }
    else {
        if ((wasSeeking != NO_SEEK) && (mFlags & SEEK_PREVIEW)) {
            mFlags &= ~SEEK_PREVIEW;
            return;
        }

        if(!mIsVideoSourceJpg) {
            postVideoEvent_l(0);
        }
        else {
            postVideoEvent_l(33000);
        }
    }
}

status_t PreviewPlayer::prepare() {
    ALOGV("prepare");
    Mutex::Autolock autoLock(mLock);
    return prepare_l();
}

status_t PreviewPlayer::prepare_l() {
    ALOGV("prepare_l");
    if (mFlags & PREPARED) {
        return OK;
    }

    if (mFlags & PREPARING) {
        return UNKNOWN_ERROR;
    }

    mIsAsyncPrepare = false;
    status_t err = prepareAsync_l();

    if (err != OK) {
        return err;
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    return mPrepareResult;
}

status_t PreviewPlayer::prepareAsync() {
    ALOGV("prepareAsync");
    Mutex::Autolock autoLock(mLock);
    return prepareAsync_l();
}

status_t PreviewPlayer::prepareAsync_l() {
    ALOGV("prepareAsync_l");
    if (mFlags & PREPARING) {
        return UNKNOWN_ERROR;  // async prepare already pending
    }

    if (!mQueueStarted) {
        mQueue.start();
        mQueueStarted = true;
    }

    mFlags |= PREPARING;
    mAsyncPrepareEvent = new PreviewPlayerEvent(
            this, &PreviewPlayer::onPrepareAsyncEvent);

    mQueue.postEvent(mAsyncPrepareEvent);

    return OK;
}

status_t PreviewPlayer::finishSetDataSource_l() {
    sp<DataSource> dataSource;
    sp<MediaExtractor> extractor;

    dataSource = DataSource::CreateFromURI(mUri.string(), NULL);

    if (dataSource == NULL) {
        return UNKNOWN_ERROR;
    }

    //If file type is .rgb, then no need to check for Extractor
    int uriLen = strlen(mUri);
    int startOffset = uriLen - 4;
    if(!strncasecmp(mUri+startOffset, ".rgb", 4)) {
        extractor = NULL;
    }
    else {
        extractor = MediaExtractor::Create(dataSource,
                                        MEDIA_MIMETYPE_CONTAINER_MPEG4);
    }

    if (extractor == NULL) {
        ALOGV("finishSetDataSource_l: failed to create extractor");
        return setDataSource_l_jpg();
    }

    return setDataSource_l(extractor);
}

void PreviewPlayer::onPrepareAsyncEvent() {
    Mutex::Autolock autoLock(mLock);
    ALOGV("onPrepareAsyncEvent");

    if (mFlags & PREPARE_CANCELLED) {
        ALOGV("prepare was cancelled before doing anything");
        abortPrepare(UNKNOWN_ERROR);
        return;
    }

    if (mUri.size() > 0) {
        status_t err = finishSetDataSource_l();

        if (err != OK) {
            abortPrepare(err);
            return;
        }
    }

    if (mVideoTrack != NULL && mVideoSource == NULL) {
        status_t err = initVideoDecoder_l(OMXCodec::kHardwareCodecsOnly);

        if (err != OK) {
            abortPrepare(err);
            return;
        }
    }

    if (mAudioTrack != NULL && mAudioSource == NULL) {
        status_t err = initAudioDecoder_l();

        if (err != OK) {
            abortPrepare(err);
            return;
        }
    }
    finishAsyncPrepare_l();

}

void PreviewPlayer::finishAsyncPrepare_l() {
    ALOGV("finishAsyncPrepare_l");
    if (mIsAsyncPrepare) {
        if (mVideoSource == NULL) {
            notifyListener_l(MEDIA_SET_VIDEO_SIZE, 0, 0);
        } else {
            notifyVideoSize_l();
        }
        notifyListener_l(MEDIA_PREPARED);
    }

    mPrepareResult = OK;
    mFlags &= ~(PREPARING|PREPARE_CANCELLED);
    mFlags |= PREPARED;
    mAsyncPrepareEvent = NULL;
    mPreparedCondition.broadcast();
}

void PreviewPlayer::acquireLock() {
    ALOGV("acquireLock");
    mLockControl.lock();
}

void PreviewPlayer::releaseLock() {
    ALOGV("releaseLock");
    mLockControl.unlock();
}

status_t PreviewPlayer::loadEffectsSettings(
        M4VSS3GPP_EffectSettings* pEffectSettings, int nEffects) {

    ALOGV("loadEffectsSettings");
    mNumberEffects = nEffects;
    mEffectsSettings = pEffectSettings;
    return OK;
}

status_t PreviewPlayer::loadAudioMixSettings(
        M4xVSS_AudioMixingSettings* pAudioMixSettings) {

    ALOGV("loadAudioMixSettings");
    mPreviewPlayerAudioMixSettings = pAudioMixSettings;
    return OK;
}

status_t PreviewPlayer::setAudioMixPCMFileHandle(
        M4OSA_Context pAudioMixPCMFileHandle) {

    ALOGV("setAudioMixPCMFileHandle");
    mAudioMixPCMFileHandle = pAudioMixPCMFileHandle;
    return OK;
}

status_t PreviewPlayer::setAudioMixStoryBoardParam(
        M4OSA_UInt32 audioMixStoryBoardTS,
        M4OSA_UInt32 currentMediaBeginCutTime,
        M4OSA_UInt32 primaryTrackVolValue ) {

    ALOGV("setAudioMixStoryBoardParam");
    mAudioMixStoryBoardTS = audioMixStoryBoardTS;
    mCurrentMediaBeginCutTime = currentMediaBeginCutTime;
    mCurrentMediaVolumeValue = primaryTrackVolValue;
    return OK;
}

status_t PreviewPlayer::setPlaybackBeginTime(uint32_t msec) {

    mPlayBeginTimeMsec = msec;
    return OK;
}

status_t PreviewPlayer::setPlaybackEndTime(uint32_t msec) {

    mPlayEndTimeMsec = msec;
    return OK;
}

status_t PreviewPlayer::setStoryboardStartTime(uint32_t msec) {

    mStoryboardStartTimeMsec = msec;
    mDecVideoTsStoryBoard = mStoryboardStartTimeMsec * 1000LL;
    return OK;
}

status_t PreviewPlayer::setProgressCallbackInterval(uint32_t cbInterval) {

    mProgressCbInterval = cbInterval;
    return OK;
}


status_t PreviewPlayer::setMediaRenderingMode(
        M4xVSS_MediaRendering mode,
        M4VIDEOEDITING_VideoFrameSize outputVideoSize) {

    mRenderingMode = mode;

    /* get the video width and height by resolution */
    return getVideoSizeByResolution(
                outputVideoSize,
                    &mOutputVideoWidth, &mOutputVideoHeight);

}

status_t PreviewPlayer::resetJniCallbackTimeStamp() {

    mDecVideoTsStoryBoard = mStoryboardStartTimeMsec * 1000LL;
    return OK;
}

void PreviewPlayer::postProgressCallbackEvent_l() {
    if (mProgressCbEventPending) {
        return;
    }
    mProgressCbEventPending = true;

    mQueue.postEvent(mProgressCbEvent);
}


void PreviewPlayer::onProgressCbEvent() {
    Mutex::Autolock autoLock(mLock);
    if (!mProgressCbEventPending) {
        return;
    }
    mProgressCbEventPending = false;
    // If playback starts from previous I-frame,
    // then send frame storyboard duration
    if ((mDecodedVideoTs/1000) < mPlayBeginTimeMsec) {
        notifyListener_l(MEDIA_INFO, 0, mDecVideoTsStoryBoard/1000);
    } else {
        notifyListener_l(MEDIA_INFO, 0,
        (((mDecodedVideoTs+mDecVideoTsStoryBoard)/1000)-mPlayBeginTimeMsec));
    }
}

void PreviewPlayer::postOverlayUpdateEvent_l() {
    if (mOverlayUpdateEventPending) {
        return;
    }
    mOverlayUpdateEventPending = true;
    mQueue.postEvent(mOverlayUpdateEvent);
}

void PreviewPlayer::onUpdateOverlayEvent() {
    Mutex::Autolock autoLock(mLock);

    if (!mOverlayUpdateEventPending) {
        return;
    }
    mOverlayUpdateEventPending = false;

    int updateState = mOverlayUpdateEventPosted? 1: 0;
    notifyListener_l(0xBBBBBBBB, updateState, mCurrFramingEffectIndex);
}


void PreviewPlayer::setVideoPostProcessingNode(
        M4VSS3GPP_VideoEffectType type, M4OSA_Bool enable) {

    uint32_t effect = VIDEO_EFFECT_NONE;

    //Map M4VSS3GPP_VideoEffectType to local enum
    switch(type) {
        case M4VSS3GPP_kVideoEffectType_FadeFromBlack:
            effect = VIDEO_EFFECT_FADEFROMBLACK;
            break;

        case M4VSS3GPP_kVideoEffectType_FadeToBlack:
            effect = VIDEO_EFFECT_FADETOBLACK;
            break;

        case M4xVSS_kVideoEffectType_BlackAndWhite:
            effect = VIDEO_EFFECT_BLACKANDWHITE;
            break;

        case M4xVSS_kVideoEffectType_Pink:
            effect = VIDEO_EFFECT_PINK;
            break;

        case M4xVSS_kVideoEffectType_Green:
            effect = VIDEO_EFFECT_GREEN;
            break;

        case M4xVSS_kVideoEffectType_Sepia:
            effect = VIDEO_EFFECT_SEPIA;
            break;

        case M4xVSS_kVideoEffectType_Negative:
            effect = VIDEO_EFFECT_NEGATIVE;
            break;

        case M4xVSS_kVideoEffectType_Framing:
            effect = VIDEO_EFFECT_FRAMING;
            break;

        case M4xVSS_kVideoEffectType_Fifties:
            effect = VIDEO_EFFECT_FIFTIES;
            break;

        case M4xVSS_kVideoEffectType_ColorRGB16:
            effect = VIDEO_EFFECT_COLOR_RGB16;
            break;

        case M4xVSS_kVideoEffectType_Gradient:
            effect = VIDEO_EFFECT_GRADIENT;
            break;

        default:
            effect = VIDEO_EFFECT_NONE;
            break;
    }

    if (enable == M4OSA_TRUE) {
        //If already set, then no need to set again
        if (!(mCurrentVideoEffect & effect)) {
            mCurrentVideoEffect |= effect;
            if (effect == VIDEO_EFFECT_FIFTIES) {
                mIsFiftiesEffectStarted = true;
            }
        }
    } else  {
        //Reset only if already set
        if (mCurrentVideoEffect & effect) {
            mCurrentVideoEffect &= ~effect;
        }
    }
}

status_t PreviewPlayer::setImageClipProperties(uint32_t width,uint32_t height) {
    mVideoWidth = width;
    mVideoHeight = height;
    return OK;
}

status_t PreviewPlayer::readFirstVideoFrame() {
    ALOGV("readFirstVideoFrame");

    if (!mVideoBuffer) {
        MediaSource::ReadOptions options;
        if (mSeeking != NO_SEEK) {
            ALOGV("seeking to %lld us (%.2f secs)", mSeekTimeUs,
                    mSeekTimeUs / 1E6);

            options.setSeekTo(
                    mSeekTimeUs, MediaSource::ReadOptions::SEEK_CLOSEST);
        }
        for (;;) {
            status_t err = mVideoSource->read(&mVideoBuffer, &options);
            options.clearSeekTo();

            if (err != OK) {
                CHECK(!mVideoBuffer);

                if (err == INFO_FORMAT_CHANGED) {
                    ALOGV("VideoSource signalled format change");
                    notifyVideoSize_l();

                    if (mVideoRenderer != NULL) {
                        mVideoRendererIsPreview = false;
                        err = initRenderer_l();
                        if (err != OK) {
                            postStreamDoneEvent_l(err);
                        }
                    }

                    updateSizeToRender(mVideoSource->getFormat());
                    continue;
                }
                ALOGV("EOS reached.");
                mFlags |= VIDEO_AT_EOS;
                mFlags |= AUDIO_AT_EOS;
                postStreamDoneEvent_l(err);
                return OK;
            }

            if (mVideoBuffer->range_length() == 0) {
                // Some decoders, notably the PV AVC software decoder
                // return spurious empty buffers that we just want to ignore.

                mVideoBuffer->release();
                mVideoBuffer = NULL;
                continue;
            }

            int64_t videoTimeUs;
            CHECK(mVideoBuffer->meta_data()->findInt64(kKeyTime, &videoTimeUs));
            if (mSeeking != NO_SEEK) {
                if (videoTimeUs < mSeekTimeUs) {
                    // buffers are before seek time
                    // ignore them
                    mVideoBuffer->release();
                    mVideoBuffer = NULL;
                    continue;
                }
            } else {
                if ((videoTimeUs/1000) < mPlayBeginTimeMsec) {
                    // buffers are before begin cut time
                    // ignore them
                    mVideoBuffer->release();
                    mVideoBuffer = NULL;
                    continue;
                }
            }
            break;
        }
    }

    int64_t timeUs;
    CHECK(mVideoBuffer->meta_data()->findInt64(kKeyTime, &timeUs));
    setPosition_l(timeUs);

    mDecodedVideoTs = timeUs;

    return OK;

}

status_t PreviewPlayer::getLastRenderedTimeMs(uint32_t *lastRenderedTimeMs) {
    *lastRenderedTimeMs = (((mDecodedVideoTs+mDecVideoTsStoryBoard)/1000)-mPlayBeginTimeMsec);
    return OK;
}

void PreviewPlayer::updateSizeToRender(sp<MetaData> meta) {
    if (mVideoRenderer) {
        mVideoRenderer->updateVideoSize(meta);
    }
}

void PreviewPlayer::setListener(const wp<MediaPlayerBase> &listener) {
    Mutex::Autolock autoLock(mLock);
    mListener = listener;
}

status_t PreviewPlayer::setDataSource(const sp<IStreamSource> &source) {
    return INVALID_OPERATION;
}

void PreviewPlayer::reset() {
    Mutex::Autolock autoLock(mLock);
    reset_l();
}

void PreviewPlayer::clear_l() {
    mDisplayWidth = 0;
    mDisplayHeight = 0;

    if (mFlags & PLAYING) {
        updateBatteryUsage_l();
    }

    if (mFlags & PREPARING) {
        mFlags |= PREPARE_CANCELLED;

        if (mFlags & PREPARING_CONNECTED) {
            // We are basically done preparing, we're just buffering
            // enough data to start playback, we can safely interrupt that.
            finishAsyncPrepare_l();
        }
    }

    while (mFlags & PREPARING) {
        mPreparedCondition.wait(mLock);
    }

    cancelPlayerEvents_l(true);

    mAudioTrack.clear();
    mVideoTrack.clear();

    // Shutdown audio first, so that the respone to the reset request
    // appears to happen instantaneously as far as the user is concerned
    // If we did this later, audio would continue playing while we
    // shutdown the video-related resources and the player appear to
    // not be as responsive to a reset request.
    if (mAudioPlayer == NULL && mAudioSource != NULL) {
        // If we had an audio player, it would have effectively
        // taken possession of the audio source and stopped it when
        // _it_ is stopped. Otherwise this is still our responsibility.
        mAudioSource->stop();
    }
    mAudioSource.clear();

    mTimeSource = NULL;

    delete mAudioPlayer;
    mAudioPlayer = NULL;

    if (mVideoSource != NULL) {
        shutdownVideoDecoder_l();
    }

    mDurationUs = -1;
    mFlags = 0;
    mExtractorFlags = 0;
    mTimeSourceDeltaUs = 0;
    mVideoTimeUs = 0;

    mSeeking = NO_SEEK;
    mSeekNotificationSent = false;
    mSeekTimeUs = 0;

    mUri.setTo("");

    mBitrate = -1;
    mLastVideoTimeUs = -1;
}

void PreviewPlayer::notifyListener_l(int msg, int ext1, int ext2) {
    if (mListener != NULL) {
        sp<MediaPlayerBase> listener = mListener.promote();

        if (listener != NULL) {
            listener->sendEvent(msg, ext1, ext2);
        }
    }
}

void PreviewPlayer::onVideoLagUpdate() {
    Mutex::Autolock autoLock(mLock);
    if (!mVideoLagEventPending) {
        return;
    }
    mVideoLagEventPending = false;

    int64_t audioTimeUs = mAudioPlayer->getMediaTimeUs();
    int64_t videoLateByUs = audioTimeUs - mVideoTimeUs;

    if (!(mFlags & VIDEO_AT_EOS) && videoLateByUs > 300000ll) {
        ALOGV("video late by %lld ms.", videoLateByUs / 1000ll);

        notifyListener_l(
                MEDIA_INFO,
                MEDIA_INFO_VIDEO_TRACK_LAGGING,
                videoLateByUs / 1000ll);
    }

    postVideoLagEvent_l();
}

void PreviewPlayer::notifyVideoSize_l() {
    sp<MetaData> meta = mVideoSource->getFormat();

    int32_t vWidth, vHeight;
    int32_t cropLeft, cropTop, cropRight, cropBottom;

    CHECK(meta->findInt32(kKeyWidth, &vWidth));
    CHECK(meta->findInt32(kKeyHeight, &vHeight));

    mGivenWidth = vWidth;
    mGivenHeight = vHeight;

    if (!meta->findRect(
                kKeyCropRect, &cropLeft, &cropTop, &cropRight, &cropBottom)) {

        cropLeft = cropTop = 0;
        cropRight = vWidth - 1;
        cropBottom = vHeight - 1;

        ALOGD("got dimensions only %d x %d", vWidth, vHeight);
    } else {
        ALOGD("got crop rect %d, %d, %d, %d",
             cropLeft, cropTop, cropRight, cropBottom);
    }

    mCropRect.left = cropLeft;
    mCropRect.right = cropRight;
    mCropRect.top = cropTop;
    mCropRect.bottom = cropBottom;

    int32_t displayWidth;
    if (meta->findInt32(kKeyDisplayWidth, &displayWidth)) {
        ALOGV("Display width changed (%d=>%d)", mDisplayWidth, displayWidth);
        mDisplayWidth = displayWidth;
    }
    int32_t displayHeight;
    if (meta->findInt32(kKeyDisplayHeight, &displayHeight)) {
        ALOGV("Display height changed (%d=>%d)", mDisplayHeight, displayHeight);
        mDisplayHeight = displayHeight;
    }

    int32_t usableWidth = cropRight - cropLeft + 1;
    int32_t usableHeight = cropBottom - cropTop + 1;
    if (mDisplayWidth != 0) {
        usableWidth = mDisplayWidth;
    }
    if (mDisplayHeight != 0) {
        usableHeight = mDisplayHeight;
    }

    int32_t rotationDegrees;
    if (!mVideoTrack->getFormat()->findInt32(
                kKeyRotation, &rotationDegrees)) {
        rotationDegrees = 0;
    }

    if (rotationDegrees == 90 || rotationDegrees == 270) {
        notifyListener_l(
                MEDIA_SET_VIDEO_SIZE, usableHeight, usableWidth);
    } else {
        notifyListener_l(
                MEDIA_SET_VIDEO_SIZE, usableWidth, usableHeight);
    }
}

status_t PreviewPlayer::pause() {
    Mutex::Autolock autoLock(mLock);

    mFlags &= ~CACHE_UNDERRUN;

    return pause_l();
}

status_t PreviewPlayer::pause_l(bool at_eos) {
    if (!(mFlags & PLAYING)) {
        return OK;
    }

    cancelPlayerEvents_l();

    if (mAudioPlayer != NULL && (mFlags & AUDIO_RUNNING)) {
        if (at_eos) {
            // If we played the audio stream to completion we
            // want to make sure that all samples remaining in the audio
            // track's queue are played out.
            mAudioPlayer->pause(true /* playPendingSamples */);
        } else {
            mAudioPlayer->pause();
        }

        mFlags &= ~AUDIO_RUNNING;
    }

    mFlags &= ~PLAYING;
    updateBatteryUsage_l();

    return OK;
}

bool PreviewPlayer::isPlaying() const {
    return (mFlags & PLAYING) || (mFlags & CACHE_UNDERRUN);
}

void PreviewPlayer::setSurface(const sp<Surface> &surface) {
    Mutex::Autolock autoLock(mLock);

    mSurface = surface;
    setNativeWindow_l(surface);
}

void PreviewPlayer::setSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer) {
    Mutex::Autolock autoLock(mLock);

    mSurface.clear();
    if (bufferProducer != NULL) {
        setNativeWindow_l(new Surface(bufferProducer));
    }
}

void PreviewPlayer::shutdownVideoDecoder_l() {
    if (mVideoBuffer) {
        mVideoBuffer->release();
        mVideoBuffer = NULL;
    }

    mVideoSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    wp<MediaSource> tmp = mVideoSource;
    mVideoSource.clear();
    while (tmp.promote() != NULL) {
        usleep(1000);
    }
    IPCThreadState::self()->flushCommands();
}

void PreviewPlayer::setNativeWindow_l(const sp<ANativeWindow> &native) {
    mNativeWindow = native;

    if (mVideoSource == NULL) {
        return;
    }

    ALOGI("attempting to reconfigure to use new surface");

    bool wasPlaying = (mFlags & PLAYING) != 0;

    pause_l();

    shutdownVideoDecoder_l();

    CHECK_EQ(initVideoDecoder_l(), (status_t)OK);

    if (mLastVideoTimeUs >= 0) {
        mSeeking = SEEK;
        mSeekNotificationSent = true;
        mSeekTimeUs = mLastVideoTimeUs;
        mFlags &= ~(AT_EOS | AUDIO_AT_EOS | VIDEO_AT_EOS);
    }

    if (wasPlaying) {
        play_l();
    }
}

void PreviewPlayer::setAudioSink(
        const sp<MediaPlayerBase::AudioSink> &audioSink) {
    Mutex::Autolock autoLock(mLock);

    mAudioSink = audioSink;
}

status_t PreviewPlayer::setLooping(bool shouldLoop) {
    Mutex::Autolock autoLock(mLock);

    mFlags = mFlags & ~LOOPING;

    if (shouldLoop) {
        mFlags |= LOOPING;
    }

    return OK;
}

void PreviewPlayer::setDuration_l(int64_t durationUs) {
    if (mDurationUs < 0 || durationUs > mDurationUs) {
        mDurationUs = durationUs;
    }
}

status_t PreviewPlayer::getDuration(int64_t *durationUs) {
    Mutex::Autolock autoLock(mLock);
    if (mDurationUs < 0) {
        return UNKNOWN_ERROR;
    }

    *durationUs = mDurationUs;
    return OK;
}

status_t PreviewPlayer::getPosition(int64_t *positionUs) {
    Mutex::Autolock autoLock(mLock);

    if (mSeeking != NO_SEEK) {
        *positionUs = mSeekTimeUs;
    } else if (mVideoSource != NULL
            && (mAudioPlayer == NULL || !(mFlags & VIDEO_AT_EOS))) {
        *positionUs = mVideoTimeUs;
    } else if (mAudioPlayer != NULL) {
        *positionUs = mAudioPlayer->getMediaTimeUs();
    } else {
        *positionUs = 0;
    }

    return OK;
}

void PreviewPlayer::setPosition_l(int64_t timeUs) {
    mVideoTimeUs = timeUs;
}

status_t PreviewPlayer::seekTo_l(int64_t timeUs) {
    ALOGV("seekTo_l");
    if (mFlags & CACHE_UNDERRUN) {
        mFlags &= ~CACHE_UNDERRUN;
        play_l();
    }

    if ((mFlags & PLAYING) && mVideoSource != NULL && (mFlags & VIDEO_AT_EOS)) {
        // Video playback completed before, there's no pending
        // video event right now. In order for this new seek
        // to be honored, we need to post one.

        postVideoEvent_l();
    }

    mSeeking = SEEK;
    mSeekNotificationSent = false;
    mSeekTimeUs = timeUs;
    mFlags &= ~(AT_EOS | AUDIO_AT_EOS | VIDEO_AT_EOS);

    seekAudioIfNecessary_l();

    if (!(mFlags & PLAYING)) {
        ALOGV("seeking while paused, sending SEEK_COMPLETE notification"
             " immediately.");

        notifyListener_l(MEDIA_SEEK_COMPLETE);
        mSeekNotificationSent = true;

        if ((mFlags & PREPARED) && mVideoSource != NULL) {
            mFlags |= SEEK_PREVIEW;
            postVideoEvent_l();
        }
    }

    return OK;
}

void PreviewPlayer::seekAudioIfNecessary_l() {
    if (mSeeking != NO_SEEK && mVideoSource == NULL && mAudioPlayer != NULL) {
        mAudioPlayer->seekTo(mSeekTimeUs);

        mWatchForAudioSeekComplete = true;
        mWatchForAudioEOS = true;
    }
}

void PreviewPlayer::setAudioSource(const sp<MediaSource>& source) {
    CHECK(source != NULL);
    mAudioTrack = source;
}

void PreviewPlayer::setVideoSource(const sp<MediaSource>& source) {
    CHECK(source != NULL);
    mVideoTrack = source;
}

void PreviewPlayer::finishSeekIfNecessary(int64_t videoTimeUs) {
    if (mSeeking == SEEK_VIDEO_ONLY) {
        mSeeking = NO_SEEK;
        return;
    }

    if (mSeeking == NO_SEEK || (mFlags & SEEK_PREVIEW)) {
        return;
    }

    if (mAudioPlayer != NULL) {
        ALOGV("seeking audio to %lld us (%.2f secs).", videoTimeUs, videoTimeUs / 1E6);

        // If we don't have a video time, seek audio to the originally
        // requested seek time instead.

        mAudioPlayer->seekTo(videoTimeUs < 0 ? mSeekTimeUs : videoTimeUs);
        mWatchForAudioSeekComplete = true;
        mWatchForAudioEOS = true;
    } else if (!mSeekNotificationSent) {
        // If we're playing video only, report seek complete now,
        // otherwise audio player will notify us later.
        notifyListener_l(MEDIA_SEEK_COMPLETE);
        mSeekNotificationSent = true;
    }

    mFlags |= FIRST_FRAME;
    mSeeking = NO_SEEK;
}

void PreviewPlayer::onCheckAudioStatus() {
    Mutex::Autolock autoLock(mLock);
    if (!mAudioStatusEventPending) {
        // Event was dispatched and while we were blocking on the mutex,
        // has already been cancelled.
        return;
    }

    mAudioStatusEventPending = false;

    if (mWatchForAudioSeekComplete && !mAudioPlayer->isSeeking()) {
        mWatchForAudioSeekComplete = false;

        if (!mSeekNotificationSent) {
            notifyListener_l(MEDIA_SEEK_COMPLETE);
            mSeekNotificationSent = true;
        }

        mSeeking = NO_SEEK;
    }

    status_t finalStatus;
    if (mWatchForAudioEOS && mAudioPlayer->reachedEOS(&finalStatus)) {
        mWatchForAudioEOS = false;
        mFlags |= AUDIO_AT_EOS;
        mFlags |= FIRST_FRAME;
        postStreamDoneEvent_l(finalStatus);
    }
}

void PreviewPlayer::postVideoEvent_l(int64_t delayUs) {
    if (mVideoEventPending) {
        return;
    }

    mVideoEventPending = true;
    mQueue.postEventWithDelay(mVideoEvent, delayUs < 0 ? 10000 : delayUs);
}

void PreviewPlayer::postStreamDoneEvent_l(status_t status) {
    if (mStreamDoneEventPending) {
        return;
    }
    mStreamDoneEventPending = true;

    mStreamDoneStatus = status;
    mQueue.postEvent(mStreamDoneEvent);
}

void PreviewPlayer::postVideoLagEvent_l() {
    if (mVideoLagEventPending) {
        return;
    }
    mVideoLagEventPending = true;
    mQueue.postEventWithDelay(mVideoLagEvent, 1000000ll);
}

void PreviewPlayer::postCheckAudioStatusEvent_l(int64_t delayUs) {
    if (mAudioStatusEventPending) {
        return;
    }
    mAudioStatusEventPending = true;
    mQueue.postEventWithDelay(mCheckAudioStatusEvent, delayUs);
}

void PreviewPlayer::abortPrepare(status_t err) {
    CHECK(err != OK);

    if (mIsAsyncPrepare) {
        notifyListener_l(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN, err);
    }

    mPrepareResult = err;
    mFlags &= ~(PREPARING|PREPARE_CANCELLED|PREPARING_CONNECTED);
    mAsyncPrepareEvent = NULL;
    mPreparedCondition.broadcast();
}

uint32_t PreviewPlayer::getSourceSeekFlags() const {
    Mutex::Autolock lock(mLock);
    return mExtractorFlags;
}

void PreviewPlayer::postAudioEOS(int64_t delayUs) {
    Mutex::Autolock autoLock(mLock);
    postCheckAudioStatusEvent_l(delayUs);
}

void PreviewPlayer::postAudioSeekComplete() {
    Mutex::Autolock autoLock(mLock);
    postCheckAudioStatusEvent_l(0 /* delayUs */);
}

void PreviewPlayer::updateBatteryUsage_l() {
    uint32_t params = IMediaPlayerService::kBatteryDataTrackDecoder;
    if ((mAudioSource != NULL) && (mAudioSource != mAudioTrack)) {
        params |= IMediaPlayerService::kBatteryDataTrackAudio;
    }
    if (mVideoSource != NULL) {
        params |= IMediaPlayerService::kBatteryDataTrackVideo;
    }
    addBatteryData(params);
}

}  // namespace android
