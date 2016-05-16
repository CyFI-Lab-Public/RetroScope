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

#include <system/audio.h>

// use this flag to dump all recorded audio into a file
//#define MONITOR_RECORDING
#ifdef MONITOR_RECORDING
#define MONITOR_TARGET "/sdcard/monitor.raw"
#include <stdio.h>
static FILE* gMonitorFp = NULL;
#endif


#define KEY_RECORDING_SOURCE_PARAMSIZE  sizeof(SLuint32)
#define KEY_RECORDING_PRESET_PARAMSIZE  sizeof(SLuint32)

//-----------------------------------------------------------------------------
// Internal utility functions
//----------------------------

SLresult audioRecorder_setPreset(CAudioRecorder* ar, SLuint32 recordPreset) {
    SLresult result = SL_RESULT_SUCCESS;

    audio_source_t newRecordSource = AUDIO_SOURCE_DEFAULT;
    switch (recordPreset) {
    case SL_ANDROID_RECORDING_PRESET_GENERIC:
        newRecordSource = AUDIO_SOURCE_DEFAULT;
        break;
    case SL_ANDROID_RECORDING_PRESET_CAMCORDER:
        newRecordSource = AUDIO_SOURCE_CAMCORDER;
        break;
    case SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION:
        newRecordSource = AUDIO_SOURCE_VOICE_RECOGNITION;
        break;
    case SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION:
        newRecordSource = AUDIO_SOURCE_VOICE_COMMUNICATION;
        break;
    case SL_ANDROID_RECORDING_PRESET_NONE:
        // it is an error to set preset "none"
    default:
        SL_LOGE(ERROR_RECORDERPRESET_SET_UNKNOWN_PRESET);
        result = SL_RESULT_PARAMETER_INVALID;
    }

    // recording preset needs to be set before the object is realized
    // (ap->mAudioRecord is supposed to be 0 until then)
    if (SL_OBJECT_STATE_UNREALIZED != ar->mObject.mState) {
        SL_LOGE(ERROR_RECORDERPRESET_REALIZED);
        result = SL_RESULT_PRECONDITIONS_VIOLATED;
    } else {
        ar->mRecordSource = newRecordSource;
    }

    return result;
}


SLresult audioRecorder_getPreset(CAudioRecorder* ar, SLuint32* pPreset) {
    SLresult result = SL_RESULT_SUCCESS;

    switch (ar->mRecordSource) {
    case AUDIO_SOURCE_DEFAULT:
    case AUDIO_SOURCE_MIC:
        *pPreset = SL_ANDROID_RECORDING_PRESET_GENERIC;
        break;
    case AUDIO_SOURCE_VOICE_UPLINK:
    case AUDIO_SOURCE_VOICE_DOWNLINK:
    case AUDIO_SOURCE_VOICE_CALL:
        *pPreset = SL_ANDROID_RECORDING_PRESET_NONE;
        break;
    case AUDIO_SOURCE_VOICE_RECOGNITION:
        *pPreset = SL_ANDROID_RECORDING_PRESET_VOICE_RECOGNITION;
        break;
    case AUDIO_SOURCE_CAMCORDER:
        *pPreset = SL_ANDROID_RECORDING_PRESET_CAMCORDER;
        break;
    case AUDIO_SOURCE_VOICE_COMMUNICATION:
        *pPreset = SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION;
        break;
    default:
        *pPreset = SL_ANDROID_RECORDING_PRESET_NONE;
        result = SL_RESULT_INTERNAL_ERROR;
        break;
    }

    return result;
}


void audioRecorder_handleNewPos_lockRecord(CAudioRecorder* ar) {
    //SL_LOGV("received event EVENT_NEW_POS from AudioRecord");
    slRecordCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ar->mRecord);
    callback = ar->mRecord.mCallback;
    callbackPContext = ar->mRecord.mContext;
    interface_unlock_shared(&ar->mRecord);

    if (NULL != callback) {
        // getting this event implies SL_RECORDEVENT_HEADATNEWPOS was set in the event mask
        (*callback)(&ar->mRecord.mItf, callbackPContext, SL_RECORDEVENT_HEADATNEWPOS);
    }
}


void audioRecorder_handleMarker_lockRecord(CAudioRecorder* ar) {
    //SL_LOGV("received event EVENT_MARKER from AudioRecord");
    slRecordCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ar->mRecord);
    callback = ar->mRecord.mCallback;
    callbackPContext = ar->mRecord.mContext;
    interface_unlock_shared(&ar->mRecord);

    if (NULL != callback) {
        // getting this event implies SL_RECORDEVENT_HEADATMARKER was set in the event mask
        (*callback)(&ar->mRecord.mItf, callbackPContext, SL_RECORDEVENT_HEADATMARKER);
    }
}


void audioRecorder_handleOverrun_lockRecord(CAudioRecorder* ar) {
    //SL_LOGV("received event EVENT_OVERRUN from AudioRecord");
    slRecordCallback callback = NULL;
    void* callbackPContext = NULL;

    interface_lock_shared(&ar->mRecord);
    if (ar->mRecord.mCallbackEventsMask & SL_RECORDEVENT_HEADSTALLED) {
        callback = ar->mRecord.mCallback;
        callbackPContext = ar->mRecord.mContext;
    }
    interface_unlock_shared(&ar->mRecord);

    if (NULL != callback) {
        (*callback)(&ar->mRecord.mItf, callbackPContext, SL_RECORDEVENT_HEADSTALLED);
    }
}

//-----------------------------------------------------------------------------
SLresult android_audioRecorder_checkSourceSinkSupport(CAudioRecorder* ar) {

    const SLDataSource *pAudioSrc = &ar->mDataSource.u.mSource;
    const SLDataSink   *pAudioSnk = &ar->mDataSink.u.mSink;

    // Sink check:
    // only buffer queue sinks are supported, regardless of the data source
    if (SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE != *(SLuint32 *)pAudioSnk->pLocator) {
        SL_LOGE(ERROR_RECORDER_SINK_MUST_BE_ANDROIDSIMPLEBUFFERQUEUE);
        return SL_RESULT_PARAMETER_INVALID;
    } else {
        // only PCM buffer queues are supported
        SLuint32 formatType = *(SLuint32 *)pAudioSnk->pFormat;
        if (SL_DATAFORMAT_PCM == formatType) {
            SLDataFormat_PCM *df_pcm = (SLDataFormat_PCM *)ar->mDataSink.u.mSink.pFormat;
            ar->mSampleRateMilliHz = df_pcm->samplesPerSec;
            ar->mNumChannels = df_pcm->numChannels;
            SL_LOGV("AudioRecorder requested sample rate = %u mHz, %u channel(s)",
                    ar->mSampleRateMilliHz, ar->mNumChannels);
        }
        else {
            SL_LOGE(ERROR_RECORDER_SINK_FORMAT_MUST_BE_PCM);
            return SL_RESULT_PARAMETER_INVALID;
        }
    }

    // Source check:
    // only input device sources are supported
    // check it's an IO device
    if (SL_DATALOCATOR_IODEVICE != *(SLuint32 *)pAudioSrc->pLocator) {
        SL_LOGE(ERROR_RECORDER_SOURCE_MUST_BE_IODEVICE);
        return SL_RESULT_PARAMETER_INVALID;
    } else {

        // check it's an input device
        SLDataLocator_IODevice *dl_iod = (SLDataLocator_IODevice *) pAudioSrc->pLocator;
        if (SL_IODEVICE_AUDIOINPUT != dl_iod->deviceType) {
            SL_LOGE(ERROR_RECORDER_IODEVICE_MUST_BE_AUDIOINPUT);
            return SL_RESULT_PARAMETER_INVALID;
        }

        // check it's the default input device, others aren't supported here
        if (SL_DEFAULTDEVICEID_AUDIOINPUT != dl_iod->deviceID) {
            SL_LOGE(ERROR_RECORDER_INPUT_ID_MUST_BE_DEFAULT);
            return SL_RESULT_PARAMETER_INVALID;
        }
    }

    return SL_RESULT_SUCCESS;
}
//-----------------------------------------------------------------------------
static void audioRecorder_callback(int event, void* user, void *info) {
    //SL_LOGV("audioRecorder_callback(%d, %p, %p) entering", event, user, info);

    CAudioRecorder *ar = (CAudioRecorder *)user;
    void * callbackPContext = NULL;

    switch(event) {
    case android::AudioRecord::EVENT_MORE_DATA: {
        slBufferQueueCallback callback = NULL;
        android::AudioRecord::Buffer* pBuff = (android::AudioRecord::Buffer*)info;

        // push data to the buffer queue
        interface_lock_exclusive(&ar->mBufferQueue);

        if (ar->mBufferQueue.mState.count != 0) {
            assert(ar->mBufferQueue.mFront != ar->mBufferQueue.mRear);

            BufferHeader *oldFront = ar->mBufferQueue.mFront;
            BufferHeader *newFront = &oldFront[1];

            // FIXME handle 8bit based on buffer format
            short *pDest = (short*)((char *)oldFront->mBuffer + ar->mBufferQueue.mSizeConsumed);
            if (ar->mBufferQueue.mSizeConsumed + pBuff->size < oldFront->mSize) {
                // can't consume the whole or rest of the buffer in one shot
                ar->mBufferQueue.mSizeConsumed += pBuff->size;
                // leave pBuff->size untouched
                // consume data
                // FIXME can we avoid holding the lock during the copy?
                memcpy (pDest, pBuff->i16, pBuff->size);
#ifdef MONITOR_RECORDING
                if (NULL != gMonitorFp) { fwrite(pBuff->i16, pBuff->size, 1, gMonitorFp); }
#endif
            } else {
                // finish pushing the buffer or push the buffer in one shot
                pBuff->size = oldFront->mSize - ar->mBufferQueue.mSizeConsumed;
                ar->mBufferQueue.mSizeConsumed = 0;
                if (newFront == &ar->mBufferQueue.mArray[ar->mBufferQueue.mNumBuffers + 1]) {
                    newFront = ar->mBufferQueue.mArray;
                }
                ar->mBufferQueue.mFront = newFront;

                ar->mBufferQueue.mState.count--;
                ar->mBufferQueue.mState.playIndex++;
                // consume data
                // FIXME can we avoid holding the lock during the copy?
                memcpy (pDest, pBuff->i16, pBuff->size);
#ifdef MONITOR_RECORDING
                if (NULL != gMonitorFp) { fwrite(pBuff->i16, pBuff->size, 1, gMonitorFp); }
#endif
                // data has been copied to the buffer, and the buffer queue state has been updated
                // we will notify the client if applicable
                callback = ar->mBufferQueue.mCallback;
                // save callback data
                callbackPContext = ar->mBufferQueue.mContext;
            }
        } else {
            // no destination to push the data
            pBuff->size = 0;
        }

        interface_unlock_exclusive(&ar->mBufferQueue);
        // notify client
        if (NULL != callback) {
            (*callback)(&ar->mBufferQueue.mItf, callbackPContext);
        }
        }
        break;

    case android::AudioRecord::EVENT_OVERRUN:
        audioRecorder_handleOverrun_lockRecord(ar);
        break;

    case android::AudioRecord::EVENT_MARKER:
        audioRecorder_handleMarker_lockRecord(ar);
        break;

    case android::AudioRecord::EVENT_NEW_POS:
        audioRecorder_handleNewPos_lockRecord(ar);
        break;

    }
}


//-----------------------------------------------------------------------------
SLresult android_audioRecorder_create(CAudioRecorder* ar) {
    SL_LOGV("android_audioRecorder_create(%p) entering", ar);

    const SLDataSource *pAudioSrc = &ar->mDataSource.u.mSource;
    const SLDataSink *pAudioSnk = &ar->mDataSink.u.mSink;
    SLresult result = SL_RESULT_SUCCESS;

    const SLuint32 sourceLocatorType = *(SLuint32 *)pAudioSrc->pLocator;
    const SLuint32 sinkLocatorType = *(SLuint32 *)pAudioSnk->pLocator;

    //  the following platform-independent fields have been initialized in CreateAudioRecorder()
    //    ar->mNumChannels
    //    ar->mSampleRateMilliHz

    if ((SL_DATALOCATOR_IODEVICE == sourceLocatorType) &&
            (SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE == sinkLocatorType)) {
        // microphone to simple buffer queue
        ar->mAndroidObjType = AUDIORECORDER_FROM_MIC_TO_PCM_BUFFERQUEUE;
        ar->mAudioRecord.clear();
        ar->mRecordSource = AUDIO_SOURCE_DEFAULT;
    } else {
        result = SL_RESULT_CONTENT_UNSUPPORTED;
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioRecorder_setConfig(CAudioRecorder* ar, const SLchar *configKey,
        const void *pConfigValue, SLuint32 valueSize) {

    SLresult result;

    assert(NULL != ar && NULL != configKey && NULL != pConfigValue);
    if (strcmp((const char*)configKey, (const char*)SL_ANDROID_KEY_RECORDING_PRESET) == 0) {

        // recording preset
        if (KEY_RECORDING_PRESET_PARAMSIZE > valueSize) {
            SL_LOGE(ERROR_CONFIG_VALUESIZE_TOO_LOW);
            result = SL_RESULT_BUFFER_INSUFFICIENT;
        } else {
            result = audioRecorder_setPreset(ar, *(SLuint32*)pConfigValue);
        }

    } else {
        SL_LOGE(ERROR_CONFIG_UNKNOWN_KEY);
        result = SL_RESULT_PARAMETER_INVALID;
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioRecorder_getConfig(CAudioRecorder* ar, const SLchar *configKey,
        SLuint32* pValueSize, void *pConfigValue) {

    SLresult result;

    assert(NULL != ar && NULL != configKey && NULL != pValueSize);
    if (strcmp((const char*)configKey, (const char*)SL_ANDROID_KEY_RECORDING_PRESET) == 0) {

        // recording preset
        if (NULL == pConfigValue) {
            result = SL_RESULT_SUCCESS;
        } else if (KEY_RECORDING_PRESET_PARAMSIZE > *pValueSize) {
            SL_LOGE(ERROR_CONFIG_VALUESIZE_TOO_LOW);
            result = SL_RESULT_BUFFER_INSUFFICIENT;
        } else {
            result = audioRecorder_getPreset(ar, (SLuint32*)pConfigValue);
        }
        *pValueSize = KEY_RECORDING_PRESET_PARAMSIZE;

    } else {
        SL_LOGE(ERROR_CONFIG_UNKNOWN_KEY);
        result = SL_RESULT_PARAMETER_INVALID;
    }

    return result;
}


//-----------------------------------------------------------------------------
SLresult android_audioRecorder_realize(CAudioRecorder* ar, SLboolean async) {
    SL_LOGV("android_audioRecorder_realize(%p) entering", ar);

    SLresult result = SL_RESULT_SUCCESS;

    // initialize platform-independent CAudioRecorder fields
    if (SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE != ar->mDataSink.mLocator.mLocatorType) {
        SL_LOGE(ERROR_RECORDER_SINK_MUST_BE_ANDROIDSIMPLEBUFFERQUEUE);
        return SL_RESULT_CONTENT_UNSUPPORTED;
    }
    //  the following platform-independent fields have been initialized in CreateAudioRecorder()
    //    ar->mNumChannels
    //    ar->mSampleRateMilliHz

    SL_LOGV("new AudioRecord %u channels, %u mHz", ar->mNumChannels, ar->mSampleRateMilliHz);

    // currently nothing analogous to canUseFastTrack() for recording
    audio_input_flags_t policy = AUDIO_INPUT_FLAG_FAST;

    // initialize platform-specific CAudioRecorder fields
    ar->mAudioRecord = new android::AudioRecord();
    ar->mAudioRecord->set(ar->mRecordSource, // source
            sles_to_android_sampleRate(ar->mSampleRateMilliHz), // sample rate in Hertz
            AUDIO_FORMAT_PCM_16_BIT,   //FIXME use format from buffer queue sink
            sles_to_android_channelMaskIn(ar->mNumChannels, 0 /*no channel mask*/),
                                   // channel config
            0,                     //frameCount min
            audioRecorder_callback,// callback_t
            (void*)ar,             // user, callback data, here the AudioRecorder
            0,                     // notificationFrames
            false,                 // threadCanCallJava, note: this will prevent direct Java
                                   //   callbacks, but we don't want them in the recording loop
            0,                     // session ID
            android::AudioRecord::TRANSFER_CALLBACK,
                                   // transfer type
            policy);               // audio_input_flags_t

    if (android::NO_ERROR != ar->mAudioRecord->initCheck()) {
        SL_LOGE("android_audioRecorder_realize(%p) error creating AudioRecord object", ar);
        result = SL_RESULT_CONTENT_UNSUPPORTED;
    }

#ifdef MONITOR_RECORDING
    gMonitorFp = fopen(MONITOR_TARGET, "w");
    if (NULL == gMonitorFp) { SL_LOGE("error opening %s", MONITOR_TARGET); }
    else { SL_LOGE("recording to %s", MONITOR_TARGET); } // SL_LOGE so it's always displayed
#endif

    return result;
}


//-----------------------------------------------------------------------------
void android_audioRecorder_destroy(CAudioRecorder* ar) {
    SL_LOGV("android_audioRecorder_destroy(%p) entering", ar);

    if (ar->mAudioRecord != 0) {
        ar->mAudioRecord->stop();
    }
    // explicit destructor
    ar->mAudioRecord.~sp();

#ifdef MONITOR_RECORDING
    if (NULL != gMonitorFp) {
        fclose(gMonitorFp);
        gMonitorFp = NULL;
    }
#endif
}


//-----------------------------------------------------------------------------
void android_audioRecorder_setRecordState(CAudioRecorder* ar, SLuint32 state) {
    SL_LOGV("android_audioRecorder_setRecordState(%p, %u) entering", ar, state);

    if (ar->mAudioRecord == 0) {
        return;
    }

    switch (state) {
     case SL_RECORDSTATE_STOPPED:
         ar->mAudioRecord->stop();
         break;
     case SL_RECORDSTATE_PAUSED:
         // Note that pausing is treated like stop as this implementation only records to a buffer
         //  queue, so there is no notion of destination being "opened" or "closed" (See description
         //  of SL_RECORDSTATE in specification)
         ar->mAudioRecord->stop();
         break;
     case SL_RECORDSTATE_RECORDING:
         ar->mAudioRecord->start();
         break;
     default:
         break;
     }

}


//-----------------------------------------------------------------------------
void android_audioRecorder_useRecordEventMask(CAudioRecorder *ar) {
    IRecord *pRecordItf = &ar->mRecord;
    SLuint32 eventFlags = pRecordItf->mCallbackEventsMask;

    if (ar->mAudioRecord == 0) {
        return;
    }

    if ((eventFlags & SL_RECORDEVENT_HEADATMARKER) && (pRecordItf->mMarkerPosition != 0)) {
        ar->mAudioRecord->setMarkerPosition((uint32_t)((((int64_t)pRecordItf->mMarkerPosition
                * sles_to_android_sampleRate(ar->mSampleRateMilliHz)))/1000));
    } else {
        // clear marker
        ar->mAudioRecord->setMarkerPosition(0);
    }

    if (eventFlags & SL_RECORDEVENT_HEADATNEWPOS) {
        SL_LOGV("pos update period %d", pRecordItf->mPositionUpdatePeriod);
         ar->mAudioRecord->setPositionUpdatePeriod(
                (uint32_t)((((int64_t)pRecordItf->mPositionUpdatePeriod
                * sles_to_android_sampleRate(ar->mSampleRateMilliHz)))/1000));
    } else {
        // clear periodic update
        ar->mAudioRecord->setPositionUpdatePeriod(0);
    }

    if (eventFlags & SL_RECORDEVENT_HEADATLIMIT) {
        // FIXME support SL_RECORDEVENT_HEADATLIMIT
        SL_LOGD("[ FIXME: IRecord_SetCallbackEventsMask(SL_RECORDEVENT_HEADATLIMIT) on an "
                    "SL_OBJECTID_AUDIORECORDER to be implemented ]");
    }

    if (eventFlags & SL_RECORDEVENT_HEADMOVING) {
        // FIXME support SL_RECORDEVENT_HEADMOVING
        SL_LOGD("[ FIXME: IRecord_SetCallbackEventsMask(SL_RECORDEVENT_HEADMOVING) on an "
                "SL_OBJECTID_AUDIORECORDER to be implemented ]");
    }

    if (eventFlags & SL_RECORDEVENT_BUFFER_FULL) {
        // nothing to do for SL_RECORDEVENT_BUFFER_FULL since this will not be encountered on
        // recording to buffer queues
    }

    if (eventFlags & SL_RECORDEVENT_HEADSTALLED) {
        // nothing to do for SL_RECORDEVENT_HEADSTALLED, callback event will be checked against mask
        // when AudioRecord::EVENT_OVERRUN is encountered

    }

}


//-----------------------------------------------------------------------------
void android_audioRecorder_getPosition(CAudioRecorder *ar, SLmillisecond *pPosMsec) {
    if ((NULL == ar) || (ar->mAudioRecord == 0)) {
        *pPosMsec = 0;
    } else {
        uint32_t positionInFrames;
        ar->mAudioRecord->getPosition(&positionInFrames);
        if (ar->mSampleRateMilliHz == UNKNOWN_SAMPLERATE) {
            *pPosMsec = 0;
        } else {
            *pPosMsec = ((int64_t)positionInFrames * 1000) /
                    sles_to_android_sampleRate(ar->mSampleRateMilliHz);
        }
    }
}
