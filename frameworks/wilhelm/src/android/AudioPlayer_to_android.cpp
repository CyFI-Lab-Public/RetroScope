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

#include "sles_allinclusive.h"
#include "android_prompts.h"
#include "android/android_AudioToCbRenderer.h"
#include "android/android_StreamPlayer.h"
#include "android/android_LocAVPlayer.h"
#include "android/include/AacBqToPcmCbRenderer.h"

#include <fcntl.h>
#include <sys/stat.h>

#include <system/audio.h>

template class android::KeyedVector<SLuint32, android::AudioEffect* > ;

#define KEY_STREAM_TYPE_PARAMSIZE  sizeof(SLint32)

#define AUDIOTRACK_MIN_PLAYBACKRATE_PERMILLE  500
#define AUDIOTRACK_MAX_PLAYBACKRATE_PERMILLE 2000

#define MEDIAPLAYER_MIN_PLAYBACKRATE_PERMILLE AUDIOTRACK_MIN_PLAYBACKRATE_PERMILLE
#define MEDIAPLAYER_MAX_PLAYBACKRATE_PERMILLE AUDIOTRACK_MAX_PLAYBACKRATE_PERMILLE

//-----------------------------------------------------------------------------
// FIXME this method will be absorbed into android_audioPlayer_setPlayState() once
//       bufferqueue and uri/fd playback are moved under the GenericPlayer C++ object
SLresult aplayer_setPlayState(const android::sp<android::GenericPlayer> &ap, SLuint32 playState,
        AndroidObjectState* pObjState) {
    SLresult result = SL_RESULT_SUCCESS;
    AndroidObjectState objState = *pObjState;

    switch (playState) {
     case SL_PLAYSTATE_STOPPED:
         SL_LOGV("setting GenericPlayer to SL_PLAYSTATE_STOPPED");
         ap->stop();
         break;
     case SL_PLAYSTATE_PAUSED:
         SL_LOGV("setting GenericPlayer to SL_PLAYSTATE_PAUSED");
         switch(objState) {
         case ANDROID_UNINITIALIZED:
             *pObjState = ANDROID_PREPARING;
             ap->prepare();
             break;
         case ANDROID_PREPARING:
             break;
         case ANDROID_READY:
             ap->pause();
             break;
         default:
             SL_LOGE(ERROR_PLAYERSETPLAYSTATE_INVALID_OBJECT_STATE_D, playState);
             result = SL_RESULT_INTERNAL_ERROR;
             break;
         }
         break;
     case SL_PLAYSTATE_PLAYING: {
         SL_LOGV("setting GenericPlayer to SL_PLAYSTATE_PLAYING");
         switch(objState) {
         case ANDROID_UNINITIALIZED:
             *pObjState = ANDROID_PREPARING;
             ap->prepare();
             // intended fall through
         case ANDROID_PREPARING:
             // intended fall through
         case ANDROID_READY:
             ap->play();
             break;
         default:
             SL_LOGE(ERROR_PLAYERSETPLAYSTATE_INVALID_OBJECT_STATE_D, playState);
             result = SL_RESULT_INTERNAL_ERROR;
             break;
         }
         }
         break;
     default:
         // checked by caller, should not happen
         SL_LOGE(ERROR_SHOULDNT_BE_HERE_S, "aplayer_setPlayState");
         result = SL_RESULT_INTERNAL_ERROR;
         break;
     }

    return result;
}


//-----------------------------------------------------------------------------
// Callback associated with a AudioToCbRenderer of an SL ES AudioPlayer that gets its data
// from a URI or FD, to write the decoded audio data to a buffer queue
static size_t adecoder_writeToBufferQueue(const uint8_t *data, size_t size, CAudioPlayer* ap) {
    if (!android::CallbackProtector::enterCbIfOk(ap->mCallbackProtector)) {
        // it is not safe to enter the callback (the player is about to go away)
        return 0;
    }
    size_t sizeConsumed = 0;
    SL_LOGD("received %d bytes from decoder", size);
    slBufferQueueCallback callback = NULL;
    void * callbackPContext = NULL;

    // push decoded data to the buffer queue
    object_lock_exclusive(&ap->mObject);

    if (ap->mBufferQueue.mState.count != 0) {
        assert(ap->mBufferQueue.mFront != ap->mBufferQueue.mRear);

        BufferHeader *oldFront = ap->mBufferQueue.mFront;
        BufferHeader *newFront = &oldFront[1];

        uint8_t *pDest = (uint8_t *)oldFront->mBuffer + ap->mBufferQueue.mSizeConsumed;
        if (ap->mBufferQueue.mSizeConsumed + size < oldFront->mSize) {
            // room to consume the whole or rest of the decoded data in one shot
            ap->mBufferQueue.mSizeConsumed += size;
            // consume data but no callback to the BufferQueue interface here
            memcpy (pDest, data, size);
            sizeConsumed = size;
        } else {
            // push as much as possible of the decoded data into the buffer queue
            sizeConsumed = oldFront->mSize - ap->mBufferQueue.mSizeConsumed;

            // the buffer at the head of the buffer queue is full, update the state
            ap->mBufferQueue.mSizeConsumed = 0;
            if (newFront == &ap->mBufferQueue.mArray[ap->mBufferQueue.mNumBuffers + 1]) {
                newFront = ap->mBufferQueue.mArray;
            }
            ap->mBufferQueue.mFront = newFront;

            ap->mBufferQueue.mState.count--;
            ap->mBufferQueue.mState.playIndex++;
            // consume data
            memcpy (pDest, data, sizeConsumed);
            // data has been copied to the buffer, and the buffer queue state has been updated
            // we will notify the client if applicable
            callback = ap->mBufferQueue.mCallback;
            // save callback data
            callbackPContext = ap->mBufferQueue.mContext;
        }

    } else {
        // no available buffers in the queue to write the decoded data
        sizeConsumed = 0;
    }

    object_unlock_exclusive(&ap->mObject);
    // notify client
    if (NULL != callback) {
        (*callback)(&ap->mBufferQueue.mItf, callbackPContext);
    }

    ap->mCallbackProtector->exitCb();
    return sizeConsumed;
}


//-----------------------------------------------------------------------------
#define LEFT_CHANNEL_MASK  0x1 << 0
#define RIGHT_CHANNEL_MASK 0x1 << 1

void android_audioPlayer_volumeUpdate(CAudioPlayer* ap)
{
    assert(ap != NULL);

    // the source's channel count, where zero means unknown
    SLuint8 channelCount = ap->mNumChannels;

    // whether each channel is audible
    bool leftAudibilityFactor, rightAudibilityFactor;

    // mute has priority over solo
    if (channelCount >= STEREO_CHANNELS) {
        if (ap->mMuteMask & LEFT_CHANNEL_MASK) {
            // left muted
            leftAudibilityFactor = false;
        } else {
            // left not muted
            if (ap->mSoloMask & LEFT_CHANNEL_MASK) {
                // left soloed
                leftAudibilityFactor = true;
            } else {
                // left not soloed
                if (ap->mSoloMask & RIGHT_CHANNEL_MASK) {
                    // right solo silences left
                    leftAudibilityFactor = false;
                } else {
                    // left and right are not soloed, and left is not muted
                    leftAudibilityFactor = true;
                }
            }
        }

        if (ap->mMuteMask & RIGHT_CHANNEL_MASK) {
            // right muted
            rightAudibilityFactor = false;
        } else {
            // right not muted
            if (ap->mSoloMask & RIGHT_CHANNEL_MASK) {
                // right soloed
                rightAudibilityFactor = true;
            } else {
                // right not soloed
                if (ap->mSoloMask & LEFT_CHANNEL_MASK) {
                    // left solo silences right
                    rightAudibilityFactor = false;
                } else {
                    // left and right are not soloed, and right is not muted
                    rightAudibilityFactor = true;
                }
            }
        }

    // channel mute and solo are ignored for mono and unknown channel count sources
    } else {
        leftAudibilityFactor = true;
        rightAudibilityFactor = true;
    }

    // compute volumes without setting
    const bool audibilityFactors[2] = {leftAudibilityFactor, rightAudibilityFactor};
    float volumes[2];
    android_player_volumeUpdate(volumes, &ap->mVolume, channelCount, ap->mAmplFromDirectLevel,
            audibilityFactors);
    float leftVol = volumes[0], rightVol = volumes[1];

    // set volume on the underlying media player or audio track
    if (ap->mAPlayer != 0) {
        ap->mAPlayer->setVolume(leftVol, rightVol);
    } else if (ap->mAudioTrack != 0) {
        ap->mAudioTrack->setVolume(leftVol, rightVol);
    }

    // changes in the AudioPlayer volume must be reflected in the send level:
    //  in SLEffectSendItf or in SLAndroidEffectSendItf?
    // FIXME replace interface test by an internal API once we have one.
    if (NULL != ap->mEffectSend.mItf) {
        for (unsigned int i=0 ; i<AUX_MAX ; i++) {
            if (ap->mEffectSend.mEnableLevels[i].mEnable) {
                android_fxSend_setSendLevel(ap,
                        ap->mEffectSend.mEnableLevels[i].mSendLevel + ap->mVolume.mLevel);
                // there's a single aux bus on Android, so we can stop looking once the first
                // aux effect is found.
                break;
            }
        }
    } else if (NULL != ap->mAndroidEffectSend.mItf) {
        android_fxSend_setSendLevel(ap, ap->mAndroidEffectSend.mSendLevel + ap->mVolume.mLevel);
    }
}

// Called by android_audioPlayer_volumeUpdate and android_mediaPlayer_volumeUpdate to compute
// volumes, but setting volumes is handled by the caller.

void android_player_volumeUpdate(float *pVolumes /*[2]*/, const IVolume *volumeItf, unsigned
channelCount, float amplFromDirectLevel, const bool *audibilityFactors /*[2]*/)
{
    assert(pVolumes != NULL);
    assert(volumeItf != NULL);
    // OK for audibilityFactors to be NULL

    bool leftAudibilityFactor, rightAudibilityFactor;

    // apply player mute factor
    // note that AudioTrack has mute() but not MediaPlayer, so it's easier to use volume
    // to mute for both rather than calling mute() for AudioTrack

    // player is muted
    if (volumeItf->mMute) {
        leftAudibilityFactor = false;
        rightAudibilityFactor = false;
    // player isn't muted, and channel mute/solo audibility factors are available (AudioPlayer)
    } else if (audibilityFactors != NULL) {
        leftAudibilityFactor = audibilityFactors[0];
        rightAudibilityFactor = audibilityFactors[1];
    // player isn't muted, and channel mute/solo audibility factors aren't available (MediaPlayer)
    } else {
        leftAudibilityFactor = true;
        rightAudibilityFactor = true;
    }

    // compute amplification as the combination of volume level and stereo position
    //   amplification (or attenuation) from volume level
    float amplFromVolLevel = sles_to_android_amplification(volumeItf->mLevel);
    //   amplification from direct level (changed in SLEffectSendtItf and SLAndroidEffectSendItf)
    float leftVol  = amplFromVolLevel * amplFromDirectLevel;
    float rightVol = leftVol;

    // amplification from stereo position
    if (volumeItf->mEnableStereoPosition) {
        // Left/right amplification (can be attenuations) factors derived for the StereoPosition
        float amplFromStereoPos[STEREO_CHANNELS];
        // panning law depends on content channel count: mono to stereo panning vs stereo balance
        if (1 == channelCount) {
            // mono to stereo panning
            double theta = (1000+volumeItf->mStereoPosition)*M_PI_4/1000.0f; // 0 <= theta <= Pi/2
            amplFromStereoPos[0] = cos(theta);
            amplFromStereoPos[1] = sin(theta);
        // channel count is 0 (unknown), 2 (stereo), or > 2 (multi-channel)
        } else {
            // stereo balance
            if (volumeItf->mStereoPosition > 0) {
                amplFromStereoPos[0] = (1000-volumeItf->mStereoPosition)/1000.0f;
                amplFromStereoPos[1] = 1.0f;
            } else {
                amplFromStereoPos[0] = 1.0f;
                amplFromStereoPos[1] = (1000+volumeItf->mStereoPosition)/1000.0f;
            }
        }
        leftVol  *= amplFromStereoPos[0];
        rightVol *= amplFromStereoPos[1];
    }

    // apply audibility factors
    if (!leftAudibilityFactor) {
        leftVol = 0.0;
    }
    if (!rightAudibilityFactor) {
        rightVol = 0.0;
    }

    // return the computed volumes
    pVolumes[0] = leftVol;
    pVolumes[1] = rightVol;
}

//-----------------------------------------------------------------------------
void audioTrack_handleMarker_lockPlay(CAudioPlayer* ap) {
    //SL_LOGV("received event EVENT_MARKER from AudioTrack");
    slPlayCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ap->mPlay);
    callback = ap->mPlay.mCallback;
    callbackPContext = ap->mPlay.mContext;
    interface_unlock_shared(&ap->mPlay);

    if (NULL != callback) {
        // getting this event implies SL_PLAYEVENT_HEADATMARKER was set in the event mask
        (*callback)(&ap->mPlay.mItf, callbackPContext, SL_PLAYEVENT_HEADATMARKER);
    }
}

//-----------------------------------------------------------------------------
void audioTrack_handleNewPos_lockPlay(CAudioPlayer* ap) {
    //SL_LOGV("received event EVENT_NEW_POS from AudioTrack");
    slPlayCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ap->mPlay);
    callback = ap->mPlay.mCallback;
    callbackPContext = ap->mPlay.mContext;
    interface_unlock_shared(&ap->mPlay);

    if (NULL != callback) {
        // getting this event implies SL_PLAYEVENT_HEADATNEWPOS was set in the event mask
        (*callback)(&ap->mPlay.mItf, callbackPContext, SL_PLAYEVENT_HEADATNEWPOS);
    }
}


//-----------------------------------------------------------------------------
void audioTrack_handleUnderrun_lockPlay(CAudioPlayer* ap) {
    slPlayCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ap->mPlay);
    callback = ap->mPlay.mCallback;
    callbackPContext = ap->mPlay.mContext;
    bool headStalled = (ap->mPlay.mEventFlags & SL_PLAYEVENT_HEADSTALLED) != 0;
    interface_unlock_shared(&ap->mPlay);

    if ((NULL != callback) && headStalled) {
        (*callback)(&ap->mPlay.mItf, callbackPContext, SL_PLAYEVENT_HEADSTALLED);
    }
}


//-----------------------------------------------------------------------------
/**
 * post-condition: play state of AudioPlayer is SL_PLAYSTATE_PAUSED if setPlayStateToPaused is true
 *
 * note: a conditional flag, setPlayStateToPaused, is used here to specify whether the play state
 *       needs to be changed when the player reaches the end of the content to play. This is
 *       relative to what the specification describes for buffer queues vs the
 *       SL_PLAYEVENT_HEADATEND event. In the OpenSL ES specification 1.0.1:
 *        - section 8.12 SLBufferQueueItf states "In the case of starvation due to insufficient
 *          buffers in the queue, the playing of audio data stops. The player remains in the
 *          SL_PLAYSTATE_PLAYING state."
 *        - section 9.2.31 SL_PLAYEVENT states "SL_PLAYEVENT_HEADATEND Playback head is at the end
 *          of the current content and the player has paused."
 */
void audioPlayer_dispatch_headAtEnd_lockPlay(CAudioPlayer *ap, bool setPlayStateToPaused,
        bool needToLock) {
    //SL_LOGV("ap=%p, setPlayStateToPaused=%d, needToLock=%d", ap, setPlayStateToPaused,
    //        needToLock);
    slPlayCallback playCallback = NULL;
    void * playContext = NULL;
    // SLPlayItf callback or no callback?
    if (needToLock) {
        interface_lock_exclusive(&ap->mPlay);
    }
    if (ap->mPlay.mEventFlags & SL_PLAYEVENT_HEADATEND) {
        playCallback = ap->mPlay.mCallback;
        playContext = ap->mPlay.mContext;
    }
    if (setPlayStateToPaused) {
        ap->mPlay.mState = SL_PLAYSTATE_PAUSED;
    }
    if (needToLock) {
        interface_unlock_exclusive(&ap->mPlay);
    }
    // enqueue callback with no lock held
    if (NULL != playCallback) {
#ifndef USE_ASYNCHRONOUS_PLAY_CALLBACK
        (*playCallback)(&ap->mPlay.mItf, playContext, SL_PLAYEVENT_HEADATEND);
#else
        SLresult result = EnqueueAsyncCallback_ppi(ap, playCallback, &ap->mPlay.mItf, playContext,
                SL_PLAYEVENT_HEADATEND);
        if (SL_RESULT_SUCCESS != result) {
            ALOGW("Callback %p(%p, %p, SL_PLAYEVENT_HEADATEND) dropped", playCallback,
                    &ap->mPlay.mItf, playContext);
        }
#endif
    }

}


//-----------------------------------------------------------------------------
SLresult audioPlayer_setStreamType(CAudioPlayer* ap, SLint32 type) {
    SLresult result = SL_RESULT_SUCCESS;
    SL_LOGV("type %d", type);

    audio_stream_type_t newStreamType = ANDROID_DEFAULT_OUTPUT_STREAM_TYPE;
    switch(type) {
    case SL_ANDROID_STREAM_VOICE:
        newStreamType = AUDIO_STREAM_VOICE_CALL;
        break;
    case SL_ANDROID_STREAM_SYSTEM:
        newStreamType = AUDIO_STREAM_SYSTEM;
        break;
    case SL_ANDROID_STREAM_RING:
        newStreamType = AUDIO_STREAM_RING;
        break;
    case SL_ANDROID_STREAM_MEDIA:
        newStreamType = AUDIO_STREAM_MUSIC;
        break;
    case SL_ANDROID_STREAM_ALARM:
        newStreamType = AUDIO_STREAM_ALARM;
        break;
    case SL_ANDROID_STREAM_NOTIFICATION:
        newStreamType = AUDIO_STREAM_NOTIFICATION;
        break;
    default:
        SL_LOGE(ERROR_PLAYERSTREAMTYPE_SET_UNKNOWN_TYPE);
        result = SL_RESULT_PARAMETER_INVALID;
        break;
    }

    // stream type needs to be set before the object is realized
    // (ap->mAudioTrack is supposed to be NULL until then)
    if (SL_OBJECT_STATE_UNREALIZED != ap->mObject.mState) {
        SL_LOGE(ERROR_PLAYERSTREAMTYPE_REALIZED);
        result = SL_RESULT_PRECONDITIONS_VIOLATED;
    } else {
        ap->mStreamType = newStreamType;
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult audioPlayer_getStreamType(CAudioPlayer* ap, SLint32 *pType) {
    SLresult result = SL_RESULT_SUCCESS;

    switch(ap->mStreamType) {
    case AUDIO_STREAM_VOICE_CALL:
        *pType = SL_ANDROID_STREAM_VOICE;
        break;
    case AUDIO_STREAM_SYSTEM:
        *pType = SL_ANDROID_STREAM_SYSTEM;
        break;
    case AUDIO_STREAM_RING:
        *pType = SL_ANDROID_STREAM_RING;
        break;
    case AUDIO_STREAM_DEFAULT:
    case AUDIO_STREAM_MUSIC:
        *pType = SL_ANDROID_STREAM_MEDIA;
        break;
    case AUDIO_STREAM_ALARM:
        *pType = SL_ANDROID_STREAM_ALARM;
        break;
    case AUDIO_STREAM_NOTIFICATION:
        *pType = SL_ANDROID_STREAM_NOTIFICATION;
        break;
    default:
        result = SL_RESULT_INTERNAL_ERROR;
        *pType = SL_ANDROID_STREAM_MEDIA;
        break;
    }

    return result;
}


//-----------------------------------------------------------------------------
void audioPlayer_auxEffectUpdate(CAudioPlayer* ap) {
    if ((ap->mAudioTrack != 0) && (ap->mAuxEffect != 0)) {
        android_fxSend_attach(ap, true, ap->mAuxEffect, ap->mVolume.mLevel + ap->mAuxSendLevel);
    }
}


//-----------------------------------------------------------------------------
/*
 * returns true if the given data sink is supported by AudioPlayer that doesn't
 *   play to an OutputMix object, false otherwise
 *
 * pre-condition: the locator of the audio sink is not SL_DATALOCATOR_OUTPUTMIX
 */
bool audioPlayer_isSupportedNonOutputMixSink(const SLDataSink* pAudioSink) {
    bool result = true;
    const SLuint32 sinkLocatorType = *(SLuint32 *)pAudioSink->pLocator;
    const SLuint32 sinkFormatType = *(SLuint32 *)pAudioSink->pFormat;

    switch (sinkLocatorType) {

    case SL_DATALOCATOR_BUFFERQUEUE:
    case SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE:
        if (SL_DATAFORMAT_PCM != sinkFormatType) {
            SL_LOGE("Unsupported sink format 0x%x, expected SL_DATAFORMAT_PCM",
                    (unsigned)sinkFormatType);
            result = false;
        }
        // it's no use checking the PCM format fields because additional characteristics
        // such as the number of channels, or sample size are unknown to the player at this stage
        break;

    default:
        SL_LOGE("Unsupported sink locator type 0x%x", (unsigned)sinkLocatorType);
        result = false;
        break;
    }

    return result;
}


//-----------------------------------------------------------------------------
/*
 * returns the Android object type if the locator type combinations for the source and sinks
 *   are supported by this implementation, INVALID_TYPE otherwise
 */
AndroidObjectType audioPlayer_getAndroidObjectTypeForSourceSink(CAudioPlayer *ap) {

    const SLDataSource *pAudioSrc = &ap->mDataSource.u.mSource;
    const SLDataSink *pAudioSnk = &ap->mDataSink.u.mSink;
    const SLuint32 sourceLocatorType = *(SLuint32 *)pAudioSrc->pLocator;
    const SLuint32 sinkLocatorType = *(SLuint32 *)pAudioSnk->pLocator;
    AndroidObjectType type = INVALID_TYPE;

    //--------------------------------------
    // Sink / source matching check:
    // the following source / sink combinations are supported
    //     SL_DATALOCATOR_BUFFERQUEUE                / SL_DATALOCATOR_OUTPUTMIX
    //     SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE   / SL_DATALOCATOR_OUTPUTMIX
    //     SL_DATALOCATOR_URI                        / SL_DATALOCATOR_OUTPUTMIX
    //     SL_DATALOCATOR_ANDROIDFD                  / SL_DATALOCATOR_OUTPUTMIX
    //     SL_DATALOCATOR_ANDROIDBUFFERQUEUE         / SL_DATALOCATOR_OUTPUTMIX
    //     SL_DATALOCATOR_ANDROIDBUFFERQUEUE         / SL_DATALOCATOR_BUFFERQUEUE
    //     SL_DATALOCATOR_URI                        / SL_DATALOCATOR_BUFFERQUEUE
    //     SL_DATALOCATOR_ANDROIDFD                  / SL_DATALOCATOR_BUFFERQUEUE
    //     SL_DATALOCATOR_URI                        / SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE
    //     SL_DATALOCATOR_ANDROIDFD                  / SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE
    switch (sinkLocatorType) {

    case SL_DATALOCATOR_OUTPUTMIX: {
        switch (sourceLocatorType) {

        //   Buffer Queue to AudioTrack
        case SL_DATALOCATOR_BUFFERQUEUE:
        case SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE:
            type = AUDIOPLAYER_FROM_PCM_BUFFERQUEUE;
            break;

        //   URI or FD to MediaPlayer
        case SL_DATALOCATOR_URI:
        case SL_DATALOCATOR_ANDROIDFD:
            type = AUDIOPLAYER_FROM_URIFD;
            break;

        //   Android BufferQueue to MediaPlayer (shared memory streaming)
        case SL_DATALOCATOR_ANDROIDBUFFERQUEUE:
            type = AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE;
            break;

        default:
            SL_LOGE("Source data locator 0x%x not supported with SL_DATALOCATOR_OUTPUTMIX sink",
                    (unsigned)sourceLocatorType);
            break;
        }
        }
        break;

    case SL_DATALOCATOR_BUFFERQUEUE:
    case SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE:
        switch (sourceLocatorType) {

        //   URI or FD decoded to PCM in a buffer queue
        case SL_DATALOCATOR_URI:
        case SL_DATALOCATOR_ANDROIDFD:
            type = AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE;
            break;

        //   AAC ADTS Android buffer queue decoded to PCM in a buffer queue
        case SL_DATALOCATOR_ANDROIDBUFFERQUEUE:
            type = AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE;
            break;

        default:
            SL_LOGE("Source data locator 0x%x not supported with SL_DATALOCATOR_BUFFERQUEUE sink",
                    (unsigned)sourceLocatorType);
            break;
        }
        break;

    default:
        SL_LOGE("Sink data locator 0x%x not supported", (unsigned)sinkLocatorType);
        break;
    }

    return type;
}


//-----------------------------------------------------------------------------
/*
 * Callback associated with an SfPlayer of an SL ES AudioPlayer that gets its data
 * from a URI or FD, for prepare, prefetch, and play events
 */
static void sfplayer_handlePrefetchEvent(int event, int data1, int data2, void* user) {

    // FIXME see similar code and comment in player_handleMediaPlayerEventNotifications

    if (NULL == user) {
        return;
    }

    CAudioPlayer *ap = (CAudioPlayer *)user;
    if (!android::CallbackProtector::enterCbIfOk(ap->mCallbackProtector)) {
        // it is not safe to enter the callback (the track is about to go away)
        return;
    }
    union {
        char c[sizeof(int)];
        int i;
    } u;
    u.i = event;
    SL_LOGV("sfplayer_handlePrefetchEvent(event='%c%c%c%c' (%d), data1=%d, data2=%d, user=%p) from "
            "SfAudioPlayer", u.c[3], u.c[2], u.c[1], u.c[0], event, data1, data2, user);
    switch(event) {

    case android::GenericPlayer::kEventPrepared: {
        SL_LOGV("Received GenericPlayer::kEventPrepared for CAudioPlayer %p", ap);

        // assume no callback
        slPrefetchCallback callback = NULL;
        void* callbackPContext;
        SLuint32 events;

        object_lock_exclusive(&ap->mObject);

        // mark object as prepared; same state is used for successful or unsuccessful prepare
        assert(ap->mAndroidObjState == ANDROID_PREPARING);
        ap->mAndroidObjState = ANDROID_READY;

        if (PLAYER_SUCCESS == data1) {
            // Most of successful prepare completion for ap->mAPlayer
            // is handled by GenericPlayer and its subclasses.
        } else {
            // SfPlayer prepare() failed prefetching, there is no event in SLPrefetchStatus to
            //  indicate a prefetch error, so we signal it by sending simultaneously two events:
            //  - SL_PREFETCHEVENT_FILLLEVELCHANGE with a level of 0
            //  - SL_PREFETCHEVENT_STATUSCHANGE with a status of SL_PREFETCHSTATUS_UNDERFLOW
            SL_LOGE(ERROR_PLAYER_PREFETCH_d, data1);
            if (IsInterfaceInitialized(&(ap->mObject), MPH_PREFETCHSTATUS)) {
                ap->mPrefetchStatus.mLevel = 0;
                ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_UNDERFLOW;
                if (!(~ap->mPrefetchStatus.mCallbackEventsMask &
                        (SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE))) {
                    callback = ap->mPrefetchStatus.mCallback;
                    callbackPContext = ap->mPrefetchStatus.mContext;
                    events = SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE;
                }
            }
        }

        object_unlock_exclusive(&ap->mObject);

        // callback with no lock held
        if (NULL != callback) {
            (*callback)(&ap->mPrefetchStatus.mItf, callbackPContext, events);
        }

    }
    break;

    case android::GenericPlayer::kEventPrefetchFillLevelUpdate : {
        if (!IsInterfaceInitialized(&(ap->mObject), MPH_PREFETCHSTATUS)) {
            break;
        }
        slPrefetchCallback callback = NULL;
        void* callbackPContext = NULL;

        // SLPrefetchStatusItf callback or no callback?
        interface_lock_exclusive(&ap->mPrefetchStatus);
        if (ap->mPrefetchStatus.mCallbackEventsMask & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
            callback = ap->mPrefetchStatus.mCallback;
            callbackPContext = ap->mPrefetchStatus.mContext;
        }
        ap->mPrefetchStatus.mLevel = (SLpermille)data1;
        interface_unlock_exclusive(&ap->mPrefetchStatus);

        // callback with no lock held
        if (NULL != callback) {
            (*callback)(&ap->mPrefetchStatus.mItf, callbackPContext,
                    SL_PREFETCHEVENT_FILLLEVELCHANGE);
        }
    }
    break;

    case android::GenericPlayer::kEventPrefetchStatusChange: {
        if (!IsInterfaceInitialized(&(ap->mObject), MPH_PREFETCHSTATUS)) {
            break;
        }
        slPrefetchCallback callback = NULL;
        void* callbackPContext = NULL;

        // SLPrefetchStatusItf callback or no callback?
        object_lock_exclusive(&ap->mObject);
        if (ap->mPrefetchStatus.mCallbackEventsMask & SL_PREFETCHEVENT_STATUSCHANGE) {
            callback = ap->mPrefetchStatus.mCallback;
            callbackPContext = ap->mPrefetchStatus.mContext;
        }
        if (data1 >= android::kStatusIntermediate) {
            ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_SUFFICIENTDATA;
        } else if (data1 < android::kStatusIntermediate) {
            ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_UNDERFLOW;
        }
        object_unlock_exclusive(&ap->mObject);

        // callback with no lock held
        if (NULL != callback) {
            (*callback)(&ap->mPrefetchStatus.mItf, callbackPContext, SL_PREFETCHEVENT_STATUSCHANGE);
        }
        }
        break;

    case android::GenericPlayer::kEventEndOfStream: {
        audioPlayer_dispatch_headAtEnd_lockPlay(ap, true /*set state to paused?*/, true);
        if ((ap->mAudioTrack != 0) && (!ap->mSeek.mLoopEnabled)) {
            ap->mAudioTrack->stop();
        }
        }
        break;

    case android::GenericPlayer::kEventChannelCount: {
        object_lock_exclusive(&ap->mObject);
        if (UNKNOWN_NUMCHANNELS == ap->mNumChannels && UNKNOWN_NUMCHANNELS != data1) {
            ap->mNumChannels = data1;
            android_audioPlayer_volumeUpdate(ap);
        }
        object_unlock_exclusive(&ap->mObject);
        }
        break;

    case android::GenericPlayer::kEventPlay: {
        slPlayCallback callback = NULL;
        void* callbackPContext = NULL;

        interface_lock_shared(&ap->mPlay);
        callback = ap->mPlay.mCallback;
        callbackPContext = ap->mPlay.mContext;
        interface_unlock_shared(&ap->mPlay);

        if (NULL != callback) {
            SLuint32 event = (SLuint32) data1;  // SL_PLAYEVENT_HEAD*
#ifndef USE_ASYNCHRONOUS_PLAY_CALLBACK
            // synchronous callback requires a synchronous GetPosition implementation
            (*callback)(&ap->mPlay.mItf, callbackPContext, event);
#else
            // asynchronous callback works with any GetPosition implementation
            SLresult result = EnqueueAsyncCallback_ppi(ap, callback, &ap->mPlay.mItf,
                    callbackPContext, event);
            if (SL_RESULT_SUCCESS != result) {
                ALOGW("Callback %p(%p, %p, 0x%x) dropped", callback,
                        &ap->mPlay.mItf, callbackPContext, event);
            }
#endif
        }
        }
        break;

      case android::GenericPlayer::kEventErrorAfterPrepare: {
        SL_LOGV("kEventErrorAfterPrepare");

        // assume no callback
        slPrefetchCallback callback = NULL;
        void* callbackPContext = NULL;

        object_lock_exclusive(&ap->mObject);
        if (IsInterfaceInitialized(&ap->mObject, MPH_PREFETCHSTATUS)) {
            ap->mPrefetchStatus.mLevel = 0;
            ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_UNDERFLOW;
            if (!(~ap->mPrefetchStatus.mCallbackEventsMask &
                    (SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE))) {
                callback = ap->mPrefetchStatus.mCallback;
                callbackPContext = ap->mPrefetchStatus.mContext;
            }
        }
        object_unlock_exclusive(&ap->mObject);

        // FIXME there's interesting information in data1, but no API to convey it to client
        SL_LOGE("Error after prepare: %d", data1);

        // callback with no lock held
        if (NULL != callback) {
            (*callback)(&ap->mPrefetchStatus.mItf, callbackPContext,
                    SL_PREFETCHEVENT_FILLLEVELCHANGE | SL_PREFETCHEVENT_STATUSCHANGE);
        }

      }
      break;

    case android::GenericPlayer::kEventHasVideoSize:
        //SL_LOGW("Unexpected kEventHasVideoSize");
        break;

    default:
        break;
    }

    ap->mCallbackProtector->exitCb();
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_checkSourceSink(CAudioPlayer *pAudioPlayer)
{
    // verify that the locator types for the source / sink combination is supported
    pAudioPlayer->mAndroidObjType = audioPlayer_getAndroidObjectTypeForSourceSink(pAudioPlayer);
    if (INVALID_TYPE == pAudioPlayer->mAndroidObjType) {
        return SL_RESULT_PARAMETER_INVALID;
    }

    const SLDataSource *pAudioSrc = &pAudioPlayer->mDataSource.u.mSource;
    const SLDataSink *pAudioSnk = &pAudioPlayer->mDataSink.u.mSink;

    // format check:
    const SLuint32 sourceLocatorType = *(SLuint32 *)pAudioSrc->pLocator;
    const SLuint32 sinkLocatorType = *(SLuint32 *)pAudioSnk->pLocator;
    const SLuint32 sourceFormatType = *(SLuint32 *)pAudioSrc->pFormat;
    const SLuint32 sinkFormatType = *(SLuint32 *)pAudioSnk->pFormat;

    switch (sourceLocatorType) {
    //------------------
    //   Buffer Queues
    case SL_DATALOCATOR_BUFFERQUEUE:
    case SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE:
        {
        SLDataLocator_BufferQueue *dl_bq = (SLDataLocator_BufferQueue *) pAudioSrc->pLocator;

        // Buffer format
        switch (sourceFormatType) {
        //     currently only PCM buffer queues are supported,
        case SL_DATAFORMAT_PCM: {
            SLDataFormat_PCM *df_pcm = (SLDataFormat_PCM *) pAudioSrc->pFormat;
            switch (df_pcm->numChannels) {
            case 1:
            case 2:
                break;
            default:
                // this should have already been rejected by checkDataFormat
                SL_LOGE("Cannot create audio player: unsupported " \
                    "PCM data source with %u channels", (unsigned) df_pcm->numChannels);
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            switch (df_pcm->samplesPerSec) {
            case SL_SAMPLINGRATE_8:
            case SL_SAMPLINGRATE_11_025:
            case SL_SAMPLINGRATE_12:
            case SL_SAMPLINGRATE_16:
            case SL_SAMPLINGRATE_22_05:
            case SL_SAMPLINGRATE_24:
            case SL_SAMPLINGRATE_32:
            case SL_SAMPLINGRATE_44_1:
            case SL_SAMPLINGRATE_48:
                break;
            case SL_SAMPLINGRATE_64:
            case SL_SAMPLINGRATE_88_2:
            case SL_SAMPLINGRATE_96:
            case SL_SAMPLINGRATE_192:
            default:
                SL_LOGE("Cannot create audio player: unsupported sample rate %u milliHz",
                    (unsigned) df_pcm->samplesPerSec);
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            switch (df_pcm->bitsPerSample) {
            case SL_PCMSAMPLEFORMAT_FIXED_8:
            case SL_PCMSAMPLEFORMAT_FIXED_16:
                break;
                // others
            default:
                // this should have already been rejected by checkDataFormat
                SL_LOGE("Cannot create audio player: unsupported sample bit depth %u",
                        (SLuint32)df_pcm->bitsPerSample);
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            switch (df_pcm->containerSize) {
            case 8:
            case 16:
                break;
                // others
            default:
                SL_LOGE("Cannot create audio player: unsupported container size %u",
                    (unsigned) df_pcm->containerSize);
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            // df_pcm->channelMask: the earlier platform-independent check and the
            //     upcoming check by sles_to_android_channelMaskOut are sufficient
            switch (df_pcm->endianness) {
            case SL_BYTEORDER_LITTLEENDIAN:
                break;
            case SL_BYTEORDER_BIGENDIAN:
                SL_LOGE("Cannot create audio player: unsupported big-endian byte order");
                return SL_RESULT_CONTENT_UNSUPPORTED;
                // native is proposed but not yet in spec
            default:
                SL_LOGE("Cannot create audio player: unsupported byte order %u",
                    (unsigned) df_pcm->endianness);
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            } //case SL_DATAFORMAT_PCM
            break;
        case SL_DATAFORMAT_MIME:
        case XA_DATAFORMAT_RAWIMAGE:
            SL_LOGE("Cannot create audio player with buffer queue data source "
                "without SL_DATAFORMAT_PCM format");
            return SL_RESULT_CONTENT_UNSUPPORTED;
        default:
            // invalid data format is detected earlier
            assert(false);
            return SL_RESULT_INTERNAL_ERROR;
        } // switch (sourceFormatType)
        } // case SL_DATALOCATOR_BUFFERQUEUE or SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE
        break;
    //------------------
    //   URI
    case SL_DATALOCATOR_URI:
        {
        SLDataLocator_URI *dl_uri = (SLDataLocator_URI *) pAudioSrc->pLocator;
        if (NULL == dl_uri->URI) {
            return SL_RESULT_PARAMETER_INVALID;
        }
        // URI format
        switch (sourceFormatType) {
        case SL_DATAFORMAT_MIME:
            break;
        case SL_DATAFORMAT_PCM:
        case XA_DATAFORMAT_RAWIMAGE:
            SL_LOGE("Cannot create audio player with SL_DATALOCATOR_URI data source without "
                "SL_DATAFORMAT_MIME format");
            return SL_RESULT_CONTENT_UNSUPPORTED;
        } // switch (sourceFormatType)
        // decoding format check
        if ((sinkLocatorType != SL_DATALOCATOR_OUTPUTMIX) &&
                !audioPlayer_isSupportedNonOutputMixSink(pAudioSnk)) {
            return SL_RESULT_CONTENT_UNSUPPORTED;
        }
        } // case SL_DATALOCATOR_URI
        break;
    //------------------
    //   File Descriptor
    case SL_DATALOCATOR_ANDROIDFD:
        {
        // fd is already non null
        switch (sourceFormatType) {
        case SL_DATAFORMAT_MIME:
            break;
        case SL_DATAFORMAT_PCM:
            // FIXME implement
            SL_LOGD("[ FIXME implement PCM FD data sources ]");
            break;
        case XA_DATAFORMAT_RAWIMAGE:
            SL_LOGE("Cannot create audio player with SL_DATALOCATOR_ANDROIDFD data source "
                "without SL_DATAFORMAT_MIME or SL_DATAFORMAT_PCM format");
            return SL_RESULT_CONTENT_UNSUPPORTED;
        default:
            // invalid data format is detected earlier
            assert(false);
            return SL_RESULT_INTERNAL_ERROR;
        } // switch (sourceFormatType)
        if ((sinkLocatorType != SL_DATALOCATOR_OUTPUTMIX) &&
                !audioPlayer_isSupportedNonOutputMixSink(pAudioSnk)) {
            return SL_RESULT_CONTENT_UNSUPPORTED;
        }
        } // case SL_DATALOCATOR_ANDROIDFD
        break;
    //------------------
    //   Stream
    case SL_DATALOCATOR_ANDROIDBUFFERQUEUE:
    {
        switch (sourceFormatType) {
        case SL_DATAFORMAT_MIME:
        {
            SLDataFormat_MIME *df_mime = (SLDataFormat_MIME *) pAudioSrc->pFormat;
            if (NULL == df_mime) {
                SL_LOGE("MIME type null invalid");
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
            SL_LOGD("source MIME is %s", (char*)df_mime->mimeType);
            switch(df_mime->containerType) {
            case SL_CONTAINERTYPE_MPEG_TS:
                if (strcasecmp((char*)df_mime->mimeType, (const char *)XA_ANDROID_MIME_MP2TS)) {
                    SL_LOGE("Invalid MIME (%s) for container SL_CONTAINERTYPE_MPEG_TS, expects %s",
                            (char*)df_mime->mimeType, XA_ANDROID_MIME_MP2TS);
                    return SL_RESULT_CONTENT_UNSUPPORTED;
                }
                break;
            case SL_CONTAINERTYPE_RAW:
            case SL_CONTAINERTYPE_AAC:
                if (strcasecmp((char*)df_mime->mimeType, (const char *)SL_ANDROID_MIME_AACADTS) &&
                        strcasecmp((char*)df_mime->mimeType,
                                ANDROID_MIME_AACADTS_ANDROID_FRAMEWORK)) {
                    SL_LOGE("Invalid MIME (%s) for container type %d, expects %s",
                            (char*)df_mime->mimeType, df_mime->containerType,
                            SL_ANDROID_MIME_AACADTS);
                    return SL_RESULT_CONTENT_UNSUPPORTED;
                }
                break;
            default:
                SL_LOGE("Cannot create player with SL_DATALOCATOR_ANDROIDBUFFERQUEUE data source "
                                        "that is not fed MPEG-2 TS data or AAC ADTS data");
                return SL_RESULT_CONTENT_UNSUPPORTED;
            }
        }
        break;
        default:
            SL_LOGE("Cannot create player with SL_DATALOCATOR_ANDROIDBUFFERQUEUE data source "
                    "without SL_DATAFORMAT_MIME format");
            return SL_RESULT_CONTENT_UNSUPPORTED;
        }
    }
    break; // case SL_DATALOCATOR_ANDROIDBUFFERQUEUE
    //------------------
    //   Address
    case SL_DATALOCATOR_ADDRESS:
    case SL_DATALOCATOR_IODEVICE:
    case SL_DATALOCATOR_OUTPUTMIX:
    case XA_DATALOCATOR_NATIVEDISPLAY:
    case SL_DATALOCATOR_MIDIBUFFERQUEUE:
        SL_LOGE("Cannot create audio player with data locator type 0x%x",
                (unsigned) sourceLocatorType);
        return SL_RESULT_CONTENT_UNSUPPORTED;
    default:
        SL_LOGE("Cannot create audio player with invalid data locator type 0x%x",
                (unsigned) sourceLocatorType);
        return SL_RESULT_PARAMETER_INVALID;
    }// switch (locatorType)

    return SL_RESULT_SUCCESS;
}


//-----------------------------------------------------------------------------
// Callback associated with an AudioTrack of an SL ES AudioPlayer that gets its data
// from a buffer queue. This will not be called once the AudioTrack has been destroyed.
static void audioTrack_callBack_pullFromBuffQueue(int event, void* user, void *info) {
    CAudioPlayer *ap = (CAudioPlayer *)user;

    if (!android::CallbackProtector::enterCbIfOk(ap->mCallbackProtector)) {
        // it is not safe to enter the callback (the track is about to go away)
        return;
    }

    void * callbackPContext = NULL;
    switch(event) {

    case android::AudioTrack::EVENT_MORE_DATA: {
        //SL_LOGV("received event EVENT_MORE_DATA from AudioTrack TID=%d", gettid());
        slBufferQueueCallback callback = NULL;
        slPrefetchCallback prefetchCallback = NULL;
        void *prefetchContext = NULL;
        SLuint32 prefetchEvents = SL_PREFETCHEVENT_NONE;
        android::AudioTrack::Buffer* pBuff = (android::AudioTrack::Buffer*)info;

        // retrieve data from the buffer queue
        interface_lock_exclusive(&ap->mBufferQueue);

        if (ap->mBufferQueue.mState.count != 0) {
            //SL_LOGV("nbBuffers in queue = %u",ap->mBufferQueue.mState.count);
            assert(ap->mBufferQueue.mFront != ap->mBufferQueue.mRear);

            BufferHeader *oldFront = ap->mBufferQueue.mFront;
            BufferHeader *newFront = &oldFront[1];

            // declared as void * because this code supports both 8-bit and 16-bit PCM data
            void *pSrc = (char *)oldFront->mBuffer + ap->mBufferQueue.mSizeConsumed;
            if (ap->mBufferQueue.mSizeConsumed + pBuff->size < oldFront->mSize) {
                // can't consume the whole or rest of the buffer in one shot
                ap->mBufferQueue.mSizeConsumed += pBuff->size;
                // leave pBuff->size untouched
                // consume data
                // FIXME can we avoid holding the lock during the copy?
                memcpy (pBuff->raw, pSrc, pBuff->size);
            } else {
                // finish consuming the buffer or consume the buffer in one shot
                pBuff->size = oldFront->mSize - ap->mBufferQueue.mSizeConsumed;
                ap->mBufferQueue.mSizeConsumed = 0;

                if (newFront ==
                        &ap->mBufferQueue.mArray
                            [ap->mBufferQueue.mNumBuffers + 1])
                {
                    newFront = ap->mBufferQueue.mArray;
                }
                ap->mBufferQueue.mFront = newFront;

                ap->mBufferQueue.mState.count--;
                ap->mBufferQueue.mState.playIndex++;

                // consume data
                // FIXME can we avoid holding the lock during the copy?
                memcpy (pBuff->raw, pSrc, pBuff->size);

                // data has been consumed, and the buffer queue state has been updated
                // we will notify the client if applicable
                callback = ap->mBufferQueue.mCallback;
                // save callback data
                callbackPContext = ap->mBufferQueue.mContext;
            }
        } else { // empty queue
            // signal no data available
            pBuff->size = 0;

            // signal we're at the end of the content, but don't pause (see note in function)
            audioPlayer_dispatch_headAtEnd_lockPlay(ap, false /*set state to paused?*/, false);

            // signal underflow to prefetch status itf
            if (IsInterfaceInitialized(&(ap->mObject), MPH_PREFETCHSTATUS)) {
                ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_UNDERFLOW;
                ap->mPrefetchStatus.mLevel = 0;
                // callback or no callback?
                prefetchEvents = ap->mPrefetchStatus.mCallbackEventsMask &
                        (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE);
                if (SL_PREFETCHEVENT_NONE != prefetchEvents) {
                    prefetchCallback = ap->mPrefetchStatus.mCallback;
                    prefetchContext  = ap->mPrefetchStatus.mContext;
                }
            }

            // stop the track so it restarts playing faster when new data is enqueued
            ap->mAudioTrack->stop();
        }
        interface_unlock_exclusive(&ap->mBufferQueue);

        // notify client
        if (NULL != prefetchCallback) {
            assert(SL_PREFETCHEVENT_NONE != prefetchEvents);
            // spec requires separate callbacks for each event
            if (prefetchEvents & SL_PREFETCHEVENT_STATUSCHANGE) {
                (*prefetchCallback)(&ap->mPrefetchStatus.mItf, prefetchContext,
                        SL_PREFETCHEVENT_STATUSCHANGE);
            }
            if (prefetchEvents & SL_PREFETCHEVENT_FILLLEVELCHANGE) {
                (*prefetchCallback)(&ap->mPrefetchStatus.mItf, prefetchContext,
                        SL_PREFETCHEVENT_FILLLEVELCHANGE);
            }
        }
        if (NULL != callback) {
            (*callback)(&ap->mBufferQueue.mItf, callbackPContext);
        }
    }
    break;

    case android::AudioTrack::EVENT_MARKER:
        //SL_LOGI("received event EVENT_MARKER from AudioTrack");
        audioTrack_handleMarker_lockPlay(ap);
        break;

    case android::AudioTrack::EVENT_NEW_POS:
        //SL_LOGI("received event EVENT_NEW_POS from AudioTrack");
        audioTrack_handleNewPos_lockPlay(ap);
        break;

    case android::AudioTrack::EVENT_UNDERRUN:
        //SL_LOGI("received event EVENT_UNDERRUN from AudioTrack");
        audioTrack_handleUnderrun_lockPlay(ap);
        break;

    case android::AudioTrack::EVENT_BUFFER_END:
    case android::AudioTrack::EVENT_LOOP_END:
        // These are unexpected so fall through
    default:
        // FIXME where does the notification of SL_PLAYEVENT_HEADMOVING fit?
        SL_LOGE("Encountered unknown AudioTrack event %d for CAudioPlayer %p", event,
                (CAudioPlayer *)user);
        break;
    }

    ap->mCallbackProtector->exitCb();
}


//-----------------------------------------------------------------------------
void android_audioPlayer_create(CAudioPlayer *pAudioPlayer) {

    // pAudioPlayer->mAndroidObjType has been set in android_audioPlayer_checkSourceSink()
    // and if it was == INVALID_TYPE, then IEngine_CreateAudioPlayer would never call us
    assert(INVALID_TYPE != pAudioPlayer->mAndroidObjType);

    // These initializations are in the same order as the field declarations in classes.h

    // FIXME Consolidate initializations (many of these already in IEngine_CreateAudioPlayer)
    // mAndroidObjType: see above comment
    pAudioPlayer->mAndroidObjState = ANDROID_UNINITIALIZED;
    pAudioPlayer->mSessionId = android::AudioSystem::newAudioSessionId();

    // placeholder: not necessary yet as session ID lifetime doesn't extend beyond player
    // android::AudioSystem::acquireAudioSessionId(pAudioPlayer->mSessionId);

    pAudioPlayer->mStreamType = ANDROID_DEFAULT_OUTPUT_STREAM_TYPE;

    // mAudioTrack
    pAudioPlayer->mCallbackProtector = new android::CallbackProtector();
    // mAPLayer
    // mAuxEffect

    pAudioPlayer->mAuxSendLevel = 0;
    pAudioPlayer->mAmplFromDirectLevel = 1.0f; // matches initial mDirectLevel value
    pAudioPlayer->mDeferredStart = false;

    // This section re-initializes interface-specific fields that
    // can be set or used regardless of whether the interface is
    // exposed on the AudioPlayer or not

    switch (pAudioPlayer->mAndroidObjType) {
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        pAudioPlayer->mPlaybackRate.mMinRate = AUDIOTRACK_MIN_PLAYBACKRATE_PERMILLE;
        pAudioPlayer->mPlaybackRate.mMaxRate = AUDIOTRACK_MAX_PLAYBACKRATE_PERMILLE;
        break;
    case AUDIOPLAYER_FROM_URIFD:
        pAudioPlayer->mPlaybackRate.mMinRate = MEDIAPLAYER_MIN_PLAYBACKRATE_PERMILLE;
        pAudioPlayer->mPlaybackRate.mMaxRate = MEDIAPLAYER_MAX_PLAYBACKRATE_PERMILLE;
        break;
    default:
        // use the default range
        break;
    }

}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_setConfig(CAudioPlayer *ap, const SLchar *configKey,
        const void *pConfigValue, SLuint32 valueSize) {

    SLresult result;

    assert(NULL != ap && NULL != configKey && NULL != pConfigValue);
    if (strcmp((const char*)configKey, (const char*)SL_ANDROID_KEY_STREAM_TYPE) == 0) {

        // stream type
        if (KEY_STREAM_TYPE_PARAMSIZE > valueSize) {
            SL_LOGE(ERROR_CONFIG_VALUESIZE_TOO_LOW);
            result = SL_RESULT_BUFFER_INSUFFICIENT;
        } else {
            result = audioPlayer_setStreamType(ap, *(SLuint32*)pConfigValue);
        }

    } else {
        SL_LOGE(ERROR_CONFIG_UNKNOWN_KEY);
        result = SL_RESULT_PARAMETER_INVALID;
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_getConfig(CAudioPlayer* ap, const SLchar *configKey,
        SLuint32* pValueSize, void *pConfigValue) {

    SLresult result;

    assert(NULL != ap && NULL != configKey && NULL != pValueSize);
    if (strcmp((const char*)configKey, (const char*)SL_ANDROID_KEY_STREAM_TYPE) == 0) {

        // stream type
        if (NULL == pConfigValue) {
            result = SL_RESULT_SUCCESS;
        } else if (KEY_STREAM_TYPE_PARAMSIZE > *pValueSize) {
            SL_LOGE(ERROR_CONFIG_VALUESIZE_TOO_LOW);
            result = SL_RESULT_BUFFER_INSUFFICIENT;
        } else {
            result = audioPlayer_getStreamType(ap, (SLint32*)pConfigValue);
        }
        *pValueSize = KEY_STREAM_TYPE_PARAMSIZE;

    } else {
        SL_LOGE(ERROR_CONFIG_UNKNOWN_KEY);
        result = SL_RESULT_PARAMETER_INVALID;
    }

    return result;
}


// Called from android_audioPlayer_realize for a PCM buffer queue player
// to determine if it can use a fast track.
static bool canUseFastTrack(CAudioPlayer *pAudioPlayer)
{
    assert(pAudioPlayer->mAndroidObjType == AUDIOPLAYER_FROM_PCM_BUFFERQUEUE);

    // no need to check the buffer queue size, application side
    // double-buffering (and more) is not a requirement for using fast tracks

    // Check a blacklist of interfaces that are incompatible with fast tracks.
    // The alternative, to check a whitelist of compatible interfaces, is
    // more maintainable but is too slow.  As a compromise, in a debug build
    // we use both methods and warn if they produce different results.
    // In release builds, we only use the blacklist method.
    // If a blacklisted interface is added after realization using
    // DynamicInterfaceManagement::AddInterface,
    // then this won't be detected but the interface will be ineffective.
    bool blacklistResult = true;
    static const unsigned blacklist[] = {
        MPH_BASSBOOST,
        MPH_EFFECTSEND,
        MPH_ENVIRONMENTALREVERB,
        MPH_EQUALIZER,
        MPH_PLAYBACKRATE,
        MPH_PRESETREVERB,
        MPH_VIRTUALIZER,
        MPH_ANDROIDEFFECT,
        MPH_ANDROIDEFFECTSEND,
        // FIXME The problem with a blacklist is remembering to add new interfaces here
    };
    for (unsigned i = 0; i < sizeof(blacklist)/sizeof(blacklist[0]); ++i) {
        if (IsInterfaceInitialized(&pAudioPlayer->mObject, blacklist[i])) {
            blacklistResult = false;
            break;
        }
    }
#if LOG_NDEBUG == 0
    bool whitelistResult = true;
    static const unsigned whitelist[] = {
        MPH_BUFFERQUEUE,
        MPH_DYNAMICINTERFACEMANAGEMENT,
        MPH_METADATAEXTRACTION,
        MPH_MUTESOLO,
        MPH_OBJECT,
        MPH_PLAY,
        MPH_PREFETCHSTATUS,
        MPH_VOLUME,
        MPH_ANDROIDCONFIGURATION,
        MPH_ANDROIDSIMPLEBUFFERQUEUE,
        MPH_ANDROIDBUFFERQUEUESOURCE,
    };
    for (unsigned mph = MPH_MIN; mph < MPH_MAX; ++mph) {
        for (unsigned i = 0; i < sizeof(whitelist)/sizeof(whitelist[0]); ++i) {
            if (mph == whitelist[i]) {
                goto compatible;
            }
        }
        if (IsInterfaceInitialized(&pAudioPlayer->mObject, mph)) {
            whitelistResult = false;
            break;
        }
compatible: ;
    }
    if (whitelistResult != blacklistResult) {
        ALOGW("whitelistResult != blacklistResult");
        // and use blacklistResult below
    }
#endif
    return blacklistResult;
}


//-----------------------------------------------------------------------------
// FIXME abstract out the diff between CMediaPlayer and CAudioPlayer
SLresult android_audioPlayer_realize(CAudioPlayer *pAudioPlayer, SLboolean async) {

    SLresult result = SL_RESULT_SUCCESS;
    SL_LOGV("Realize pAudioPlayer=%p", pAudioPlayer);

    AudioPlayback_Parameters app;
    app.sessionId = pAudioPlayer->mSessionId;
    app.streamType = pAudioPlayer->mStreamType;

    switch (pAudioPlayer->mAndroidObjType) {

    //-----------------------------------
    // AudioTrack
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        {
        // initialize platform-specific CAudioPlayer fields

        SLDataLocator_BufferQueue *dl_bq = (SLDataLocator_BufferQueue *)
                pAudioPlayer->mDynamicSource.mDataSource;
        SLDataFormat_PCM *df_pcm = (SLDataFormat_PCM *)
                pAudioPlayer->mDynamicSource.mDataSource->pFormat;

        uint32_t sampleRate = sles_to_android_sampleRate(df_pcm->samplesPerSec);

        audio_output_flags_t policy;
        if (canUseFastTrack(pAudioPlayer)) {
            policy = AUDIO_OUTPUT_FLAG_FAST;
        } else {
            policy = AUDIO_OUTPUT_FLAG_NONE;
        }

        pAudioPlayer->mAudioTrack = new android::AudioTrack(
                pAudioPlayer->mStreamType,                           // streamType
                sampleRate,                                          // sampleRate
                sles_to_android_sampleFormat(df_pcm->bitsPerSample), // format
                sles_to_android_channelMaskOut(df_pcm->numChannels, df_pcm->channelMask),
                                                                     // channel mask
                0,                                                   // frameCount
                policy,                                              // flags
                audioTrack_callBack_pullFromBuffQueue,               // callback
                (void *) pAudioPlayer,                               // user
                0,     // FIXME find appropriate frame count         // notificationFrame
                pAudioPlayer->mSessionId);
        android::status_t status = pAudioPlayer->mAudioTrack->initCheck();
        if (status != android::NO_ERROR) {
            SL_LOGE("AudioTrack::initCheck status %u", status);
            result = SL_RESULT_CONTENT_UNSUPPORTED;
            pAudioPlayer->mAudioTrack.clear();
            return result;
        }

        // initialize platform-independent CAudioPlayer fields

        pAudioPlayer->mNumChannels = df_pcm->numChannels;
        pAudioPlayer->mSampleRateMilliHz = df_pcm->samplesPerSec; // Note: bad field name in SL ES

        // This use case does not have a separate "prepare" step
        pAudioPlayer->mAndroidObjState = ANDROID_READY;
        }
        break;

    //-----------------------------------
    // MediaPlayer
    case AUDIOPLAYER_FROM_URIFD: {
        pAudioPlayer->mAPlayer = new android::LocAVPlayer(&app, false /*hasVideo*/);
        pAudioPlayer->mAPlayer->init(sfplayer_handlePrefetchEvent,
                        (void*)pAudioPlayer /*notifUSer*/);

        switch (pAudioPlayer->mDataSource.mLocator.mLocatorType) {
            case SL_DATALOCATOR_URI: {
                // The legacy implementation ran Stagefright within the application process, and
                // so allowed local pathnames specified by URI that were openable by
                // the application but were not openable by mediaserver.
                // The current implementation runs Stagefright (mostly) within mediaserver,
                // which runs as a different UID and likely a different current working directory.
                // For backwards compatibility with any applications which may have relied on the
                // previous behavior, we convert an openable file URI into an FD.
                // Note that unlike SL_DATALOCATOR_ANDROIDFD, this FD is owned by us
                // and so we close it as soon as we've passed it (via Binder dup) to mediaserver.
                const char *uri = (const char *)pAudioPlayer->mDataSource.mLocator.mURI.URI;
                if (!isDistantProtocol(uri)) {
                    // don't touch the original uri, we may need it later
                    const char *pathname = uri;
                    // skip over an optional leading file:// prefix
                    if (!strncasecmp(pathname, "file://", 7)) {
                        pathname += 7;
                    }
                    // attempt to open it as a file using the application's credentials
                    int fd = ::open(pathname, O_RDONLY);
                    if (fd >= 0) {
                        // if open is successful, then check to see if it's a regular file
                        struct stat statbuf;
                        if (!::fstat(fd, &statbuf) && S_ISREG(statbuf.st_mode)) {
                            // treat similarly to an FD data locator, but
                            // let setDataSource take responsibility for closing fd
                            pAudioPlayer->mAPlayer->setDataSource(fd, 0, statbuf.st_size, true);
                            break;
                        }
                        // we were able to open it, but it's not a file, so let mediaserver try
                        (void) ::close(fd);
                    }
                }
                // if either the URI didn't look like a file, or open failed, or not a file
                pAudioPlayer->mAPlayer->setDataSource(uri);
                } break;
            case SL_DATALOCATOR_ANDROIDFD: {
                int64_t offset = (int64_t)pAudioPlayer->mDataSource.mLocator.mFD.offset;
                pAudioPlayer->mAPlayer->setDataSource(
                        (int)pAudioPlayer->mDataSource.mLocator.mFD.fd,
                        offset == SL_DATALOCATOR_ANDROIDFD_USE_FILE_SIZE ?
                                (int64_t)PLAYER_FD_FIND_FILE_SIZE : offset,
                        (int64_t)pAudioPlayer->mDataSource.mLocator.mFD.length);
                }
                break;
            default:
                SL_LOGE(ERROR_PLAYERREALIZE_UNKNOWN_DATASOURCE_LOCATOR);
                break;
        }

        }
        break;

    //-----------------------------------
    // StreamPlayer
    case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE: {
        android::StreamPlayer* splr = new android::StreamPlayer(&app, false /*hasVideo*/,
                &pAudioPlayer->mAndroidBufferQueue, pAudioPlayer->mCallbackProtector);
        pAudioPlayer->mAPlayer = splr;
        splr->init(sfplayer_handlePrefetchEvent, (void*)pAudioPlayer);
        }
        break;

    //-----------------------------------
    // AudioToCbRenderer
    case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE: {
        android::AudioToCbRenderer* decoder = new android::AudioToCbRenderer(&app);
        pAudioPlayer->mAPlayer = decoder;
        // configures the callback for the sink buffer queue
        decoder->setDataPushListener(adecoder_writeToBufferQueue, pAudioPlayer);
        // configures the callback for the notifications coming from the SF code
        decoder->init(sfplayer_handlePrefetchEvent, (void*)pAudioPlayer);

        switch (pAudioPlayer->mDataSource.mLocator.mLocatorType) {
        case SL_DATALOCATOR_URI:
            decoder->setDataSource(
                    (const char*)pAudioPlayer->mDataSource.mLocator.mURI.URI);
            break;
        case SL_DATALOCATOR_ANDROIDFD: {
            int64_t offset = (int64_t)pAudioPlayer->mDataSource.mLocator.mFD.offset;
            decoder->setDataSource(
                    (int)pAudioPlayer->mDataSource.mLocator.mFD.fd,
                    offset == SL_DATALOCATOR_ANDROIDFD_USE_FILE_SIZE ?
                            (int64_t)PLAYER_FD_FIND_FILE_SIZE : offset,
                            (int64_t)pAudioPlayer->mDataSource.mLocator.mFD.length);
            }
            break;
        default:
            SL_LOGE(ERROR_PLAYERREALIZE_UNKNOWN_DATASOURCE_LOCATOR);
            break;
        }

        }
        break;

    //-----------------------------------
    // AacBqToPcmCbRenderer
    case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE: {
        android::AacBqToPcmCbRenderer* bqtobq = new android::AacBqToPcmCbRenderer(&app,
                &pAudioPlayer->mAndroidBufferQueue);
        // configures the callback for the sink buffer queue
        bqtobq->setDataPushListener(adecoder_writeToBufferQueue, pAudioPlayer);
        pAudioPlayer->mAPlayer = bqtobq;
        // configures the callback for the notifications coming from the SF code,
        // but also implicitly configures the AndroidBufferQueue from which ADTS data is read
        pAudioPlayer->mAPlayer->init(sfplayer_handlePrefetchEvent, (void*)pAudioPlayer);
        }
        break;

    //-----------------------------------
    default:
        SL_LOGE(ERROR_PLAYERREALIZE_UNEXPECTED_OBJECT_TYPE_D, pAudioPlayer->mAndroidObjType);
        result = SL_RESULT_INTERNAL_ERROR;
        break;
    }

    // proceed with effect initialization
    // initialize EQ
    // FIXME use a table of effect descriptors when adding support for more effects
    if (memcmp(SL_IID_EQUALIZER, &pAudioPlayer->mEqualizer.mEqDescriptor.type,
            sizeof(effect_uuid_t)) == 0) {
        SL_LOGV("Need to initialize EQ for AudioPlayer=%p", pAudioPlayer);
        android_eq_init(pAudioPlayer->mSessionId, &pAudioPlayer->mEqualizer);
    }
    // initialize BassBoost
    if (memcmp(SL_IID_BASSBOOST, &pAudioPlayer->mBassBoost.mBassBoostDescriptor.type,
            sizeof(effect_uuid_t)) == 0) {
        SL_LOGV("Need to initialize BassBoost for AudioPlayer=%p", pAudioPlayer);
        android_bb_init(pAudioPlayer->mSessionId, &pAudioPlayer->mBassBoost);
    }
    // initialize Virtualizer
    if (memcmp(SL_IID_VIRTUALIZER, &pAudioPlayer->mVirtualizer.mVirtualizerDescriptor.type,
               sizeof(effect_uuid_t)) == 0) {
        SL_LOGV("Need to initialize Virtualizer for AudioPlayer=%p", pAudioPlayer);
        android_virt_init(pAudioPlayer->mSessionId, &pAudioPlayer->mVirtualizer);
    }

    // initialize EffectSend
    // FIXME initialize EffectSend

    return result;
}


//-----------------------------------------------------------------------------
/**
 * Called with a lock on AudioPlayer, and blocks until safe to destroy
 */
SLresult android_audioPlayer_preDestroy(CAudioPlayer *pAudioPlayer) {
    SL_LOGD("android_audioPlayer_preDestroy(%p)", pAudioPlayer);
    SLresult result = SL_RESULT_SUCCESS;

    bool disableCallbacksBeforePreDestroy;
    switch (pAudioPlayer->mAndroidObjType) {
    // Not yet clear why this order is important, but it reduces detected deadlocks
    case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
        disableCallbacksBeforePreDestroy = true;
        break;
    // Use the old behavior for all other use cases until proven
    // case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
    default:
        disableCallbacksBeforePreDestroy = false;
        break;
    }

    if (disableCallbacksBeforePreDestroy) {
        object_unlock_exclusive(&pAudioPlayer->mObject);
        if (pAudioPlayer->mCallbackProtector != 0) {
            pAudioPlayer->mCallbackProtector->requestCbExitAndWait();
        }
        object_lock_exclusive(&pAudioPlayer->mObject);
    }

    if (pAudioPlayer->mAPlayer != 0) {
        pAudioPlayer->mAPlayer->preDestroy();
    }
    SL_LOGD("android_audioPlayer_preDestroy(%p) after mAPlayer->preDestroy()", pAudioPlayer);

    if (!disableCallbacksBeforePreDestroy) {
        object_unlock_exclusive(&pAudioPlayer->mObject);
        if (pAudioPlayer->mCallbackProtector != 0) {
            pAudioPlayer->mCallbackProtector->requestCbExitAndWait();
        }
        object_lock_exclusive(&pAudioPlayer->mObject);
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_destroy(CAudioPlayer *pAudioPlayer) {
    SLresult result = SL_RESULT_SUCCESS;
    SL_LOGV("android_audioPlayer_destroy(%p)", pAudioPlayer);
    switch (pAudioPlayer->mAndroidObjType) {

    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        // We own the audio track for PCM buffer queue players
        if (pAudioPlayer->mAudioTrack != 0) {
            pAudioPlayer->mAudioTrack->stop();
            // Note that there may still be another reference in post-unlock phase of SetPlayState
            pAudioPlayer->mAudioTrack.clear();
        }
        break;

    case AUDIOPLAYER_FROM_URIFD:     // intended fall-through
    case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:    // intended fall-through
    case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE: // intended fall-through
    case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        pAudioPlayer->mAPlayer.clear();
        break;
    //-----------------------------------
    default:
        SL_LOGE(ERROR_PLAYERDESTROY_UNEXPECTED_OBJECT_TYPE_D, pAudioPlayer->mAndroidObjType);
        result = SL_RESULT_INTERNAL_ERROR;
        break;
    }

    // placeholder: not necessary yet as session ID lifetime doesn't extend beyond player
    // android::AudioSystem::releaseAudioSessionId(pAudioPlayer->mSessionId);

    pAudioPlayer->mCallbackProtector.clear();

    // explicit destructor
    pAudioPlayer->mAudioTrack.~sp();
    // note that SetPlayState(PLAYING) may still hold a reference
    pAudioPlayer->mCallbackProtector.~sp();
    pAudioPlayer->mAuxEffect.~sp();
    pAudioPlayer->mAPlayer.~sp();

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_setPlaybackRateAndConstraints(CAudioPlayer *ap, SLpermille rate,
        SLuint32 constraints) {
    SLresult result = SL_RESULT_SUCCESS;
    switch(ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE: {
        // these asserts were already checked by the platform-independent layer
        assert((AUDIOTRACK_MIN_PLAYBACKRATE_PERMILLE <= rate) &&
                (rate <= AUDIOTRACK_MAX_PLAYBACKRATE_PERMILLE));
        assert(constraints & SL_RATEPROP_NOPITCHCORAUDIO);
        // get the content sample rate
        uint32_t contentRate = sles_to_android_sampleRate(ap->mSampleRateMilliHz);
        // apply the SL ES playback rate on the AudioTrack as a factor of its content sample rate
        if (ap->mAudioTrack != 0) {
            ap->mAudioTrack->setSampleRate(contentRate * (rate/1000.0f));
        }
        }
        break;
    case AUDIOPLAYER_FROM_URIFD: {
        assert((MEDIAPLAYER_MIN_PLAYBACKRATE_PERMILLE <= rate) &&
                        (rate <= MEDIAPLAYER_MAX_PLAYBACKRATE_PERMILLE));
        assert(constraints & SL_RATEPROP_NOPITCHCORAUDIO);
        // apply the SL ES playback rate on the GenericPlayer
        if (ap->mAPlayer != 0) {
            ap->mAPlayer->setPlaybackRate((int16_t)rate);
        }
        }
        break;

    default:
        SL_LOGE("Unexpected object type %d", ap->mAndroidObjType);
        result = SL_RESULT_FEATURE_UNSUPPORTED;
        break;
    }
    return result;
}


//-----------------------------------------------------------------------------
// precondition
//  called with no lock held
//  ap != NULL
//  pItemCount != NULL
SLresult android_audioPlayer_metadata_getItemCount(CAudioPlayer *ap, SLuint32 *pItemCount) {
    if (ap->mAPlayer == 0) {
        return SL_RESULT_PARAMETER_INVALID;
    }
    switch(ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        {
            android::AudioSfDecoder* decoder =
                    static_cast<android::AudioSfDecoder*>(ap->mAPlayer.get());
            *pItemCount = decoder->getPcmFormatKeyCount();
        }
        break;
      default:
        *pItemCount = 0;
        break;
    }
    return SL_RESULT_SUCCESS;
}


//-----------------------------------------------------------------------------
// precondition
//  called with no lock held
//  ap != NULL
//  pKeySize != NULL
SLresult android_audioPlayer_metadata_getKeySize(CAudioPlayer *ap,
        SLuint32 index, SLuint32 *pKeySize) {
    if (ap->mAPlayer == 0) {
        return SL_RESULT_PARAMETER_INVALID;
    }
    SLresult res = SL_RESULT_SUCCESS;
    switch(ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        {
            android::AudioSfDecoder* decoder =
                    static_cast<android::AudioSfDecoder*>(ap->mAPlayer.get());
            SLuint32 keyNameSize = 0;
            if (!decoder->getPcmFormatKeySize(index, &keyNameSize)) {
                res = SL_RESULT_PARAMETER_INVALID;
            } else {
                // *pKeySize is the size of the region used to store the key name AND
                //   the information about the key (size, lang, encoding)
                *pKeySize = keyNameSize + sizeof(SLMetadataInfo);
            }
        }
        break;
      default:
        *pKeySize = 0;
        res = SL_RESULT_PARAMETER_INVALID;
        break;
    }
    return res;
}


//-----------------------------------------------------------------------------
// precondition
//  called with no lock held
//  ap != NULL
//  pKey != NULL
SLresult android_audioPlayer_metadata_getKey(CAudioPlayer *ap,
        SLuint32 index, SLuint32 size, SLMetadataInfo *pKey) {
    if (ap->mAPlayer == 0) {
        return SL_RESULT_PARAMETER_INVALID;
    }
    SLresult res = SL_RESULT_SUCCESS;
    switch(ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        {
            android::AudioSfDecoder* decoder =
                    static_cast<android::AudioSfDecoder*>(ap->mAPlayer.get());
            if ((size < sizeof(SLMetadataInfo) ||
                    (!decoder->getPcmFormatKeyName(index, size - sizeof(SLMetadataInfo),
                            (char*)pKey->data)))) {
                res = SL_RESULT_PARAMETER_INVALID;
            } else {
                // successfully retrieved the key value, update the other fields
                pKey->encoding = SL_CHARACTERENCODING_UTF8;
                memcpy((char *) pKey->langCountry, "en", 3);
                pKey->size = strlen((char*)pKey->data) + 1;
            }
        }
        break;
      default:
        res = SL_RESULT_PARAMETER_INVALID;
        break;
    }
    return res;
}


//-----------------------------------------------------------------------------
// precondition
//  called with no lock held
//  ap != NULL
//  pValueSize != NULL
SLresult android_audioPlayer_metadata_getValueSize(CAudioPlayer *ap,
        SLuint32 index, SLuint32 *pValueSize) {
    if (ap->mAPlayer == 0) {
        return SL_RESULT_PARAMETER_INVALID;
    }
    SLresult res = SL_RESULT_SUCCESS;
    switch(ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        {
            android::AudioSfDecoder* decoder =
                    static_cast<android::AudioSfDecoder*>(ap->mAPlayer.get());
            SLuint32 valueSize = 0;
            if (!decoder->getPcmFormatValueSize(index, &valueSize)) {
                res = SL_RESULT_PARAMETER_INVALID;
            } else {
                // *pValueSize is the size of the region used to store the key value AND
                //   the information about the value (size, lang, encoding)
                *pValueSize = valueSize + sizeof(SLMetadataInfo);
            }
        }
        break;
      default:
          *pValueSize = 0;
          res = SL_RESULT_PARAMETER_INVALID;
          break;
    }
    return res;
}


//-----------------------------------------------------------------------------
// precondition
//  called with no lock held
//  ap != NULL
//  pValue != NULL
SLresult android_audioPlayer_metadata_getValue(CAudioPlayer *ap,
        SLuint32 index, SLuint32 size, SLMetadataInfo *pValue) {
    if (ap->mAPlayer == 0) {
        return SL_RESULT_PARAMETER_INVALID;
    }
    SLresult res = SL_RESULT_SUCCESS;
    switch(ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        {
            android::AudioSfDecoder* decoder =
                    static_cast<android::AudioSfDecoder*>(ap->mAPlayer.get());
            pValue->encoding = SL_CHARACTERENCODING_BINARY;
            memcpy((char *) pValue->langCountry, "en", 3); // applicable here?
            SLuint32 valueSize = 0;
            if ((size < sizeof(SLMetadataInfo)
                    || (!decoder->getPcmFormatValueSize(index, &valueSize))
                    || (!decoder->getPcmFormatKeyValue(index, size - sizeof(SLMetadataInfo),
                            (SLuint32*)pValue->data)))) {
                res = SL_RESULT_PARAMETER_INVALID;
            } else {
                pValue->size = valueSize;
            }
        }
        break;
      default:
        res = SL_RESULT_PARAMETER_INVALID;
        break;
    }
    return res;
}

//-----------------------------------------------------------------------------
// preconditions
//  ap != NULL
//  mutex is locked
//  play state has changed
void android_audioPlayer_setPlayState(CAudioPlayer *ap) {

    SLuint32 playState = ap->mPlay.mState;
    AndroidObjectState objState = ap->mAndroidObjState;

    switch(ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        switch (playState) {
        case SL_PLAYSTATE_STOPPED:
            SL_LOGV("setting AudioPlayer to SL_PLAYSTATE_STOPPED");
            if (ap->mAudioTrack != 0) {
                ap->mAudioTrack->stop();
            }
            break;
        case SL_PLAYSTATE_PAUSED:
            SL_LOGV("setting AudioPlayer to SL_PLAYSTATE_PAUSED");
            if (ap->mAudioTrack != 0) {
                ap->mAudioTrack->pause();
            }
            break;
        case SL_PLAYSTATE_PLAYING:
            SL_LOGV("setting AudioPlayer to SL_PLAYSTATE_PLAYING");
            if (ap->mAudioTrack != 0) {
                // instead of ap->mAudioTrack->start();
                ap->mDeferredStart = true;
            }
            break;
        default:
            // checked by caller, should not happen
            break;
        }
        break;

    case AUDIOPLAYER_FROM_URIFD:      // intended fall-through
    case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:     // intended fall-through
    case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:  // intended fall-through
    case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        // FIXME report and use the return code to the lock mechanism, which is where play state
        //   changes are updated (see object_unlock_exclusive_attributes())
        aplayer_setPlayState(ap->mAPlayer, playState, &(ap->mAndroidObjState));
        break;
    default:
        SL_LOGE(ERROR_PLAYERSETPLAYSTATE_UNEXPECTED_OBJECT_TYPE_D, ap->mAndroidObjType);
        break;
    }
}


//-----------------------------------------------------------------------------
// call when either player event flags, marker position, or position update period changes
void android_audioPlayer_usePlayEventMask(CAudioPlayer *ap) {
    IPlay *pPlayItf = &ap->mPlay;
    SLuint32 eventFlags = pPlayItf->mEventFlags;
    /*switch(ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:*/

    if (ap->mAPlayer != 0) {
        assert(ap->mAudioTrack == 0);
        ap->mAPlayer->setPlayEvents((int32_t) eventFlags, (int32_t) pPlayItf->mMarkerPosition,
                (int32_t) pPlayItf->mPositionUpdatePeriod);
        return;
    }

    if (ap->mAudioTrack == 0) {
        return;
    }

    if (eventFlags & SL_PLAYEVENT_HEADATMARKER) {
        ap->mAudioTrack->setMarkerPosition((uint32_t)((((int64_t)pPlayItf->mMarkerPosition
                * sles_to_android_sampleRate(ap->mSampleRateMilliHz)))/1000));
    } else {
        // clear marker
        ap->mAudioTrack->setMarkerPosition(0);
    }

    if (eventFlags & SL_PLAYEVENT_HEADATNEWPOS) {
         ap->mAudioTrack->setPositionUpdatePeriod(
                (uint32_t)((((int64_t)pPlayItf->mPositionUpdatePeriod
                * sles_to_android_sampleRate(ap->mSampleRateMilliHz)))/1000));
    } else {
        // clear periodic update
        ap->mAudioTrack->setPositionUpdatePeriod(0);
    }

    if (eventFlags & SL_PLAYEVENT_HEADATEND) {
        // nothing to do for SL_PLAYEVENT_HEADATEND, callback event will be checked against mask
    }

    if (eventFlags & SL_PLAYEVENT_HEADMOVING) {
        // FIXME support SL_PLAYEVENT_HEADMOVING
        SL_LOGD("[ FIXME: IPlay_SetCallbackEventsMask(SL_PLAYEVENT_HEADMOVING) on an "
            "SL_OBJECTID_AUDIOPLAYER to be implemented ]");
    }
    if (eventFlags & SL_PLAYEVENT_HEADSTALLED) {
        // nothing to do for SL_PLAYEVENT_HEADSTALLED, callback event will be checked against mask
    }

}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_getDuration(IPlay *pPlayItf, SLmillisecond *pDurMsec) {
    CAudioPlayer *ap = (CAudioPlayer *)pPlayItf->mThis;
    switch(ap->mAndroidObjType) {

      case AUDIOPLAYER_FROM_URIFD:  // intended fall-through
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE: {
        int32_t durationMsec = ANDROID_UNKNOWN_TIME;
        if (ap->mAPlayer != 0) {
            ap->mAPlayer->getDurationMsec(&durationMsec);
        }
        *pDurMsec = durationMsec == ANDROID_UNKNOWN_TIME ? SL_TIME_UNKNOWN : durationMsec;
        break;
      }

      case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE: // intended fall-through
      case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
      default: {
        *pDurMsec = SL_TIME_UNKNOWN;
      }
    }
    return SL_RESULT_SUCCESS;
}


//-----------------------------------------------------------------------------
void android_audioPlayer_getPosition(IPlay *pPlayItf, SLmillisecond *pPosMsec) {
    CAudioPlayer *ap = (CAudioPlayer *)pPlayItf->mThis;
    switch(ap->mAndroidObjType) {

      case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        if ((ap->mSampleRateMilliHz == UNKNOWN_SAMPLERATE) || (ap->mAudioTrack == 0)) {
            *pPosMsec = 0;
        } else {
            uint32_t positionInFrames;
            ap->mAudioTrack->getPosition(&positionInFrames);
            *pPosMsec = ((int64_t)positionInFrames * 1000) /
                    sles_to_android_sampleRate(ap->mSampleRateMilliHz);
        }
        break;

      case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:    // intended fall-through
      case AUDIOPLAYER_FROM_URIFD:
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE: {
        int32_t posMsec = ANDROID_UNKNOWN_TIME;
        if (ap->mAPlayer != 0) {
            ap->mAPlayer->getPositionMsec(&posMsec);
        }
        *pPosMsec = posMsec == ANDROID_UNKNOWN_TIME ? 0 : posMsec;
        break;
      }

      default:
        *pPosMsec = 0;
    }
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_seek(CAudioPlayer *ap, SLmillisecond posMsec) {
    SLresult result = SL_RESULT_SUCCESS;

    switch(ap->mAndroidObjType) {

      case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:      // intended fall-through
      case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:
      case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
        result = SL_RESULT_FEATURE_UNSUPPORTED;
        break;

      case AUDIOPLAYER_FROM_URIFD:                   // intended fall-through
      case AUDIOPLAYER_FROM_URIFD_TO_PCM_BUFFERQUEUE:
        if (ap->mAPlayer != 0) {
            ap->mAPlayer->seek(posMsec);
        }
        break;

      default:
        break;
    }
    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_loop(CAudioPlayer *ap, SLboolean loopEnable) {
    SLresult result = SL_RESULT_SUCCESS;

    switch (ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_URIFD:
    // case AUDIOPLAY_FROM_URIFD_TO_PCM_BUFFERQUEUE:
    //      would actually work, but what's the point?
      if (ap->mAPlayer != 0) {
        ap->mAPlayer->loop((bool)loopEnable);
      }
      break;
    default:
      result = SL_RESULT_FEATURE_UNSUPPORTED;
      break;
    }
    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioPlayer_setBufferingUpdateThresholdPerMille(CAudioPlayer *ap,
        SLpermille threshold) {
    SLresult result = SL_RESULT_SUCCESS;

    switch (ap->mAndroidObjType) {
      case AUDIOPLAYER_FROM_URIFD:
        if (ap->mAPlayer != 0) {
            ap->mAPlayer->setBufferingUpdateThreshold(threshold / 10);
        }
        break;

      default: {}
    }

    return result;
}


//-----------------------------------------------------------------------------
void android_audioPlayer_bufferQueue_onRefilled_l(CAudioPlayer *ap) {
    // the AudioTrack associated with the AudioPlayer receiving audio from a PCM buffer
    // queue was stopped when the queue become empty, we restart as soon as a new buffer
    // has been enqueued since we're in playing state
    if (ap->mAudioTrack != 0) {
        // instead of ap->mAudioTrack->start();
        ap->mDeferredStart = true;
    }

    // when the queue became empty, an underflow on the prefetch status itf was sent. Now the queue
    // has received new data, signal it has sufficient data
    if (IsInterfaceInitialized(&(ap->mObject), MPH_PREFETCHSTATUS)) {
        // we wouldn't have been called unless we were previously in the underflow state
        assert(SL_PREFETCHSTATUS_UNDERFLOW == ap->mPrefetchStatus.mStatus);
        assert(0 == ap->mPrefetchStatus.mLevel);
        ap->mPrefetchStatus.mStatus = SL_PREFETCHSTATUS_SUFFICIENTDATA;
        ap->mPrefetchStatus.mLevel = 1000;
        // callback or no callback?
        SLuint32 prefetchEvents = ap->mPrefetchStatus.mCallbackEventsMask &
                (SL_PREFETCHEVENT_STATUSCHANGE | SL_PREFETCHEVENT_FILLLEVELCHANGE);
        if (SL_PREFETCHEVENT_NONE != prefetchEvents) {
            ap->mPrefetchStatus.mDeferredPrefetchCallback = ap->mPrefetchStatus.mCallback;
            ap->mPrefetchStatus.mDeferredPrefetchContext  = ap->mPrefetchStatus.mContext;
            ap->mPrefetchStatus.mDeferredPrefetchEvents   = prefetchEvents;
        }
    }
}


//-----------------------------------------------------------------------------
/*
 * BufferQueue::Clear
 */
SLresult android_audioPlayer_bufferQueue_onClear(CAudioPlayer *ap) {
    SLresult result = SL_RESULT_SUCCESS;

    switch (ap->mAndroidObjType) {
    //-----------------------------------
    // AudioTrack
    case AUDIOPLAYER_FROM_PCM_BUFFERQUEUE:
        if (ap->mAudioTrack != 0) {
            ap->mAudioTrack->flush();
        }
        break;
    default:
        result = SL_RESULT_INTERNAL_ERROR;
        break;
    }

    return result;
}


//-----------------------------------------------------------------------------
void android_audioPlayer_androidBufferQueue_clear_l(CAudioPlayer *ap) {
    switch (ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:
      if (ap->mAPlayer != 0) {
        android::StreamPlayer* splr = static_cast<android::StreamPlayer*>(ap->mAPlayer.get());
        splr->appClear_l();
      } break;
    case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
      // nothing to do here, fall through
    default:
      break;
    }
}

void android_audioPlayer_androidBufferQueue_onRefilled_l(CAudioPlayer *ap) {
    switch (ap->mAndroidObjType) {
    case AUDIOPLAYER_FROM_TS_ANDROIDBUFFERQUEUE:
      if (ap->mAPlayer != 0) {
        android::StreamPlayer* splr = static_cast<android::StreamPlayer*>(ap->mAPlayer.get());
        splr->queueRefilled();
      } break;
    case AUDIOPLAYER_FROM_ADTS_ABQ_TO_PCM_BUFFERQUEUE:
      // FIXME this may require waking up the decoder if it is currently starved and isn't polling
    default:
      break;
    }
}
