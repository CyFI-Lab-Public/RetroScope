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

#define LOG_NDEBUG 1
#define LOG_TAG "VideoEditorAudioPlayer"
#include <utils/Log.h>

#include <binder/IPCThreadState.h>
#include <media/AudioTrack.h>
#include <VideoEditorAudioPlayer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

#include <system/audio.h>

#include "PreviewPlayer.h"
namespace android {

VideoEditorAudioPlayer::VideoEditorAudioPlayer(
        const sp<MediaPlayerBase::AudioSink> &audioSink,
        PreviewPlayer *observer)
    : mInputBuffer(NULL),
      mSampleRate(0),
      mLatencyUs(0),
      mFrameSize(0),
      mNumFramesPlayed(0),
      mPositionTimeMediaUs(-1),
      mPositionTimeRealUs(-1),
      mSeeking(false),
      mReachedEOS(false),
      mFinalStatus(OK),
      mStarted(false),
      mIsFirstBuffer(false),
      mFirstBufferResult(OK),
      mFirstBuffer(NULL),
      mAudioSink(audioSink),
      mObserver(observer) {

    ALOGV("Constructor");
    mBGAudioPCMFileHandle = NULL;
    mAudioProcess = NULL;
    mBGAudioPCMFileLength = 0;
    mBGAudioPCMFileTrimmedLength = 0;
    mBGAudioPCMFileDuration = 0;
    mBGAudioPCMFileSeekPoint = 0;
    mBGAudioPCMFileOriginalSeekPoint = 0;
    mBGAudioStoryBoardSkimTimeStamp = 0;
    mBGAudioStoryBoardCurrentMediaBeginCutTS = 0;
    mBGAudioStoryBoardCurrentMediaVolumeVal = 0;
    mSeekTimeUs = 0;
    mSource = NULL;
}

VideoEditorAudioPlayer::~VideoEditorAudioPlayer() {

    ALOGV("Destructor");
    if (mStarted) {
        reset();
    }
    if (mAudioProcess != NULL) {
        delete mAudioProcess;
        mAudioProcess = NULL;
    }
}

void VideoEditorAudioPlayer::pause(bool playPendingSamples) {
    ALOGV("pause: playPendingSamples=%d", playPendingSamples);
    CHECK(mStarted);

    if (playPendingSamples) {
        if (mAudioSink.get() != NULL) {
            mAudioSink->stop();
        } else {
            mAudioTrack->stop();
        }
    } else {
        if (mAudioSink.get() != NULL) {
            mAudioSink->pause();
        } else {
            mAudioTrack->pause();
        }
    }
}

void VideoEditorAudioPlayer::clear() {
    ALOGV("clear");
    if (!mStarted) {
        return;
    }

    if (mAudioSink.get() != NULL) {
        mAudioSink->stop();
        mAudioSink->close();
    } else {
        mAudioTrack->stop();

        mAudioTrack.clear();
    }

    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().

    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        ALOGV("AudioPlayerBase releasing input buffer.");

        mInputBuffer->release();
        mInputBuffer = NULL;
    }

    mSource->stop();

    // The following hack is necessary to ensure that the OMX
    // component is completely released by the time we may try
    // to instantiate it again.
    wp<MediaSource> tmp = mSource;
    mSource.clear();
    while (tmp.promote() != NULL) {
        usleep(1000);
    }
    IPCThreadState::self()->flushCommands();

    mNumFramesPlayed = 0;
    mPositionTimeMediaUs = -1;
    mPositionTimeRealUs = -1;
    mSeeking = false;
    mReachedEOS = false;
    mFinalStatus = OK;
    mStarted = false;
}

status_t VideoEditorAudioPlayer::resume() {
    ALOGV("resume");

    AudioMixSettings audioMixSettings;

    // Single audio player is used;
    // Pass on the audio ducking parameters
    // which might have changed with new audio source
    audioMixSettings.lvInDucking_threshold =
        mAudioMixSettings->uiInDucking_threshold;
    audioMixSettings.lvInDucking_lowVolume =
        ((M4OSA_Float)mAudioMixSettings->uiInDucking_lowVolume) / 100.0;
    audioMixSettings.lvInDucking_enable =
        mAudioMixSettings->bInDucking_enable;
    audioMixSettings.lvPTVolLevel =
        ((M4OSA_Float)mBGAudioStoryBoardCurrentMediaVolumeVal) / 100.0;
    audioMixSettings.lvBTVolLevel =
        ((M4OSA_Float)mAudioMixSettings->uiAddVolume) / 100.0;
    audioMixSettings.lvBTChannelCount = mAudioMixSettings->uiBTChannelCount;
    audioMixSettings.lvPTChannelCount = mAudioMixSettings->uiNbChannels;

    // Call to Audio mix param setting
    mAudioProcess->setMixParams(audioMixSettings);

    CHECK(mStarted);

    if (mAudioSink.get() != NULL) {
        mAudioSink->start();
    } else {
        mAudioTrack->start();
    }
    return OK;
}

status_t VideoEditorAudioPlayer::seekTo(int64_t time_us) {
    ALOGV("seekTo: %lld", time_us);
    Mutex::Autolock autoLock(mLock);

    mSeeking = true;
    mPositionTimeRealUs = mPositionTimeMediaUs = -1;
    mReachedEOS = false;
    mSeekTimeUs = time_us;

    if (mAudioSink != NULL) {
        mAudioSink->flush();
    } else {
        mAudioTrack->flush();
    }

    return OK;
}

bool VideoEditorAudioPlayer::isSeeking() {
    Mutex::Autolock lock(mLock);
    ALOGV("isSeeking: mSeeking=%d", mSeeking);
    return mSeeking;
}

bool VideoEditorAudioPlayer::reachedEOS(status_t *finalStatus) {
    ALOGV("reachedEOS: status=%d", mFinalStatus);
    *finalStatus = OK;

    Mutex::Autolock autoLock(mLock);
    *finalStatus = mFinalStatus;
    return mReachedEOS;
}

int64_t VideoEditorAudioPlayer::getRealTimeUs() {
    Mutex::Autolock autoLock(mLock);
    return getRealTimeUs_l();
}

int64_t VideoEditorAudioPlayer::getRealTimeUs_l() {
    return -mLatencyUs + (mNumFramesPlayed * 1000000) / mSampleRate;
}

int64_t VideoEditorAudioPlayer::getMediaTimeUs() {
    ALOGV("getMediaTimeUs");
    Mutex::Autolock autoLock(mLock);

    if (mPositionTimeMediaUs < 0 || mPositionTimeRealUs < 0) {
        if (mSeeking) {
            return mSeekTimeUs;
        }

        return 0;
    }

    int64_t realTimeOffset = getRealTimeUs_l() - mPositionTimeRealUs;
    if (realTimeOffset < 0) {
        realTimeOffset = 0;
    }

    return mPositionTimeMediaUs + realTimeOffset;
}

bool VideoEditorAudioPlayer::getMediaTimeMapping(
        int64_t *realtime_us, int64_t *mediatime_us) {
    ALOGV("getMediaTimeMapping");
    Mutex::Autolock autoLock(mLock);

    *realtime_us = mPositionTimeRealUs;
    *mediatime_us = mPositionTimeMediaUs;

    return mPositionTimeRealUs != -1 && mPositionTimeMediaUs != -1;
}

void VideoEditorAudioPlayer::setSource(const sp<MediaSource> &source) {
    Mutex::Autolock autoLock(mLock);

    // Before setting source, stop any existing source.
    // Make sure to release any buffer we hold onto so that the
    // source is able to stop().

    if (mFirstBuffer != NULL) {
        mFirstBuffer->release();
        mFirstBuffer = NULL;
    }

    if (mInputBuffer != NULL) {
        ALOGV("VideoEditorAudioPlayer releasing input buffer.");

        mInputBuffer->release();
        mInputBuffer = NULL;
    }

    if (mSource != NULL) {
        mSource->stop();
        mSource.clear();
    }

    mSource = source;
    mReachedEOS = false;
}

sp<MediaSource> VideoEditorAudioPlayer::getSource() {
    Mutex::Autolock autoLock(mLock);
    return mSource;
}

void VideoEditorAudioPlayer::setObserver(PreviewPlayer *observer) {
    ALOGV("setObserver");
    //CHECK(!mStarted);
    mObserver = observer;
}

bool VideoEditorAudioPlayer::isStarted() {
    return mStarted;
}

// static
void VideoEditorAudioPlayer::AudioCallback(int event, void *user, void *info) {
    static_cast<VideoEditorAudioPlayer *>(user)->AudioCallback(event, info);
}


void VideoEditorAudioPlayer::AudioCallback(int event, void *info) {
    if (event != AudioTrack::EVENT_MORE_DATA) {
        return;
    }

    AudioTrack::Buffer *buffer = (AudioTrack::Buffer *)info;
    size_t numBytesWritten = fillBuffer(buffer->raw, buffer->size);

    buffer->size = numBytesWritten;
}

status_t VideoEditorAudioPlayer::start(bool sourceAlreadyStarted) {
    Mutex::Autolock autoLock(mLock);
    CHECK(!mStarted);
    CHECK(mSource != NULL);
    ALOGV("Start");
    status_t err;
    M4OSA_ERR result = M4NO_ERROR;
    M4OSA_UInt32 startTime = 0;
    M4OSA_UInt32 seekTimeStamp = 0;
    M4OSA_Bool bStoryBoardTSBeyondBTEndCutTime = M4OSA_FALSE;

    if (!sourceAlreadyStarted) {
        err = mSource->start();
        if (err != OK) {
            return err;
        }
    }

    // Create the BG Audio handler
    mAudioProcess = new VideoEditorBGAudioProcessing();
    AudioMixSettings audioMixSettings;

    // Pass on the audio ducking parameters
    audioMixSettings.lvInDucking_threshold =
        mAudioMixSettings->uiInDucking_threshold;
    audioMixSettings.lvInDucking_lowVolume =
        ((M4OSA_Float)mAudioMixSettings->uiInDucking_lowVolume) / 100.0;
    audioMixSettings.lvInDucking_enable =
        mAudioMixSettings->bInDucking_enable;
    audioMixSettings.lvPTVolLevel =
        ((M4OSA_Float)mBGAudioStoryBoardCurrentMediaVolumeVal) / 100.0;
    audioMixSettings.lvBTVolLevel =
        ((M4OSA_Float)mAudioMixSettings->uiAddVolume) / 100.0;
    audioMixSettings.lvBTChannelCount = mAudioMixSettings->uiBTChannelCount;
    audioMixSettings.lvPTChannelCount = mAudioMixSettings->uiNbChannels;

    // Call to Audio mix param setting
    mAudioProcess->setMixParams(audioMixSettings);

    // Get the BG Audio PCM file details
    if ( mBGAudioPCMFileHandle ) {

        // TODO : 32bits required for OSAL, to be updated once OSAL is updated
        M4OSA_UInt32 tmp32 = 0;
        result = M4OSA_fileReadGetOption(mBGAudioPCMFileHandle,
                                        M4OSA_kFileReadGetFileSize,
                                        (M4OSA_Void**)&tmp32);
        mBGAudioPCMFileLength = tmp32;
        mBGAudioPCMFileTrimmedLength = mBGAudioPCMFileLength;


        ALOGV("VideoEditorAudioPlayer::start M4OSA_kFileReadGetFileSize = %lld",
                            mBGAudioPCMFileLength);

        // Get the duration in time of the audio BT
        if ( result == M4NO_ERROR ) {
         ALOGV("VEAP: channels = %d freq = %d",
         mAudioMixSettings->uiNbChannels,  mAudioMixSettings->uiSamplingFrequency);

            // No trim
            mBGAudioPCMFileDuration = ((
                    (int64_t)(mBGAudioPCMFileLength/sizeof(M4OSA_UInt16)/
                    mAudioMixSettings->uiNbChannels))*1000 ) /
                    mAudioMixSettings->uiSamplingFrequency;

            ALOGV("VideoEditorAudioPlayer:: beginCutMs %d , endCutMs %d",
                    (unsigned int) mAudioMixSettings->beginCutMs,
                    (unsigned int) mAudioMixSettings->endCutMs);

            // Remove the trim part
            if ((mAudioMixSettings->beginCutMs == 0) &&
                (mAudioMixSettings->endCutMs != 0)) {
                // End time itself the file duration
                mBGAudioPCMFileDuration = mAudioMixSettings->endCutMs;
                // Limit the file length also
                mBGAudioPCMFileTrimmedLength = ((
                     (int64_t)(mBGAudioPCMFileDuration *
                     mAudioMixSettings->uiSamplingFrequency) *
                     mAudioMixSettings->uiNbChannels) *
                     sizeof(M4OSA_UInt16)) / 1000;
            }
            else if ((mAudioMixSettings->beginCutMs != 0) &&
                     (mAudioMixSettings->endCutMs == mBGAudioPCMFileDuration)) {
                // End time itself the file duration
                mBGAudioPCMFileDuration = mBGAudioPCMFileDuration -
                      mAudioMixSettings->beginCutMs;
                // Limit the file length also
                mBGAudioPCMFileTrimmedLength = ((
                     (int64_t)(mBGAudioPCMFileDuration *
                     mAudioMixSettings->uiSamplingFrequency) *
                     mAudioMixSettings->uiNbChannels) *
                     sizeof(M4OSA_UInt16)) / 1000;
            }
            else if ((mAudioMixSettings->beginCutMs != 0) &&
                    (mAudioMixSettings->endCutMs != 0)) {
                // End time itself the file duration
                mBGAudioPCMFileDuration = mAudioMixSettings->endCutMs -
                    mAudioMixSettings->beginCutMs;
                // Limit the file length also
                mBGAudioPCMFileTrimmedLength = ((
                    (int64_t)(mBGAudioPCMFileDuration *
                    mAudioMixSettings->uiSamplingFrequency) *
                    mAudioMixSettings->uiNbChannels) *
                    sizeof(M4OSA_UInt16)) / 1000; /*make to sec from ms*/
            }

            ALOGV("VideoEditorAudioPlayer: file duration recorded : %lld",
                    mBGAudioPCMFileDuration);
        }

        // Last played location to be seeked at for next media item
        if ( result == M4NO_ERROR ) {
            ALOGV("VideoEditorAudioPlayer::mBGAudioStoryBoardSkimTimeStamp %lld",
                    mBGAudioStoryBoardSkimTimeStamp);
            ALOGV("VideoEditorAudioPlayer::uiAddCts %d",
                    mAudioMixSettings->uiAddCts);
            if (mBGAudioStoryBoardSkimTimeStamp >= mAudioMixSettings->uiAddCts) {
                startTime = (mBGAudioStoryBoardSkimTimeStamp -
                 mAudioMixSettings->uiAddCts);
            }
            else {
                // do nothing
            }

            ALOGV("VideoEditorAudioPlayer::startTime %d", startTime);
            seekTimeStamp = 0;
            if (startTime) {
                if (startTime >= mBGAudioPCMFileDuration) {
                    // The BG track should be looped and started again
                    if (mAudioMixSettings->bLoop) {
                        // Add begin cut time to the mod value
                        seekTimeStamp = ((startTime%mBGAudioPCMFileDuration) +
                        mAudioMixSettings->beginCutMs);
                    }else {
                        // Looping disabled, donot do BT Mix , set to file end
                        seekTimeStamp = (mBGAudioPCMFileDuration +
                        mAudioMixSettings->beginCutMs);
                    }
                }else {
                    // BT still present , just seek to story board time
                    seekTimeStamp = startTime + mAudioMixSettings->beginCutMs;
                }
            }
            else {
                seekTimeStamp = mAudioMixSettings->beginCutMs;
            }

            // Convert the seekTimeStamp to file location
            mBGAudioPCMFileOriginalSeekPoint = (
                                        (int64_t)(mAudioMixSettings->beginCutMs)
                                        * mAudioMixSettings->uiSamplingFrequency
                                        * mAudioMixSettings->uiNbChannels
                                        * sizeof(M4OSA_UInt16))/ 1000 ; /*make to sec from ms*/

            mBGAudioPCMFileSeekPoint = ((int64_t)(seekTimeStamp)
                                        * mAudioMixSettings->uiSamplingFrequency
                                        * mAudioMixSettings->uiNbChannels
                                        * sizeof(M4OSA_UInt16))/ 1000 ;
        }
    }

    // We allow an optional INFO_FORMAT_CHANGED at the very beginning
    // of playback, if there is one, getFormat below will retrieve the
    // updated format, if there isn't, we'll stash away the valid buffer
    // of data to be used on the first audio callback.

    CHECK(mFirstBuffer == NULL);

    mFirstBufferResult = mSource->read(&mFirstBuffer);
    if (mFirstBufferResult == INFO_FORMAT_CHANGED) {
        ALOGV("INFO_FORMAT_CHANGED!!!");

        CHECK(mFirstBuffer == NULL);
        mFirstBufferResult = OK;
        mIsFirstBuffer = false;
    } else {
        mIsFirstBuffer = true;
    }

    sp<MetaData> format = mSource->getFormat();
    const char *mime;
    bool success = format->findCString(kKeyMIMEType, &mime);
    CHECK(success);
    CHECK(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_RAW));

    success = format->findInt32(kKeySampleRate, &mSampleRate);
    CHECK(success);

    int32_t numChannels;
    success = format->findInt32(kKeyChannelCount, &numChannels);
    CHECK(success);

    if (mAudioSink.get() != NULL) {
        status_t err = mAudioSink->open(
                mSampleRate, numChannels, CHANNEL_MASK_USE_CHANNEL_ORDER, AUDIO_FORMAT_PCM_16_BIT,
                DEFAULT_AUDIOSINK_BUFFERCOUNT,
                &VideoEditorAudioPlayer::AudioSinkCallback, this);
        if (err != OK) {
            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioSink->latency() * 1000;
        mFrameSize = mAudioSink->frameSize();

        mAudioSink->start();
    } else {
        mAudioTrack = new AudioTrack(
                AUDIO_STREAM_MUSIC, mSampleRate, AUDIO_FORMAT_PCM_16_BIT,
                audio_channel_out_mask_from_count(numChannels),
                0, AUDIO_OUTPUT_FLAG_NONE, &AudioCallback, this, 0);

        if ((err = mAudioTrack->initCheck()) != OK) {
            mAudioTrack.clear();

            if (mFirstBuffer != NULL) {
                mFirstBuffer->release();
                mFirstBuffer = NULL;
            }

            if (!sourceAlreadyStarted) {
                mSource->stop();
            }

            return err;
        }

        mLatencyUs = (int64_t)mAudioTrack->latency() * 1000;
        mFrameSize = mAudioTrack->frameSize();

        mAudioTrack->start();
    }

    mStarted = true;

    return OK;
}


void VideoEditorAudioPlayer::reset() {

    ALOGV("reset");
    clear();

    // Capture the current seek point
    mBGAudioPCMFileSeekPoint = 0;
    mBGAudioStoryBoardSkimTimeStamp =0;
    mBGAudioStoryBoardCurrentMediaBeginCutTS=0;
}

size_t VideoEditorAudioPlayer::AudioSinkCallback(
        MediaPlayerBase::AudioSink *audioSink,
        void *buffer, size_t size, void *cookie,
        MediaPlayerBase::AudioSink::cb_event_t event) {
    VideoEditorAudioPlayer *me = (VideoEditorAudioPlayer *)cookie;

    if (event == MediaPlayerBase::AudioSink::CB_EVENT_FILL_BUFFER ) {
        return me->fillBuffer(buffer, size);
    } else {
        return 0;
    }
}


size_t VideoEditorAudioPlayer::fillBuffer(void *data, size_t size) {

    if (mReachedEOS) {
        return 0;
    }

    size_t size_done = 0;
    size_t size_remaining = size;

    M4OSA_ERR err = M4NO_ERROR;
    M4AM_Buffer16 bgFrame = {NULL, 0};
    M4AM_Buffer16 mixFrame = {NULL, 0};
    M4AM_Buffer16 ptFrame = {NULL, 0};
    int64_t currentSteamTS = 0;
    int64_t startTimeForBT = 0;
    M4OSA_Float fPTVolLevel =
     ((M4OSA_Float)mBGAudioStoryBoardCurrentMediaVolumeVal)/100;
    M4OSA_Int16     *pPTMdata=NULL;
    M4OSA_UInt32     uiPCMsize = 0;

    bool postSeekComplete = false;
    bool postEOS = false;

    while ((size_remaining > 0)&&(err==M4NO_ERROR)) {
        MediaSource::ReadOptions options;

        {
            Mutex::Autolock autoLock(mLock);
            if (mSeeking) {
                if (mIsFirstBuffer) {
                    if (mFirstBuffer != NULL) {
                        mFirstBuffer->release();
                        mFirstBuffer = NULL;
                    }
                    mIsFirstBuffer = false;
                }

                options.setSeekTo(mSeekTimeUs);

                if (mInputBuffer != NULL) {
                    mInputBuffer->release();
                    mInputBuffer = NULL;
                }

                mSeeking = false;

                if (mObserver) {
                    postSeekComplete = true;
                }
            }
        }

        if (mInputBuffer == NULL) {
            status_t status = OK;

            if (mIsFirstBuffer) {
                mInputBuffer = mFirstBuffer;
                mFirstBuffer = NULL;
                status = mFirstBufferResult;

                mIsFirstBuffer = false;
            } else {

                {
                    Mutex::Autolock autoLock(mLock);
                    status = mSource->read(&mInputBuffer, &options);
                }
                // Data is Primary Track, mix with background track
                // after reading same size from Background track PCM file
                if (status == OK)
                {
                    // Mix only when skim point is after startTime of BT
                    if (((mBGAudioStoryBoardSkimTimeStamp* 1000) +
                          (mPositionTimeMediaUs - mSeekTimeUs)) >=
                          (int64_t)(mAudioMixSettings->uiAddCts * 1000)) {

                        ALOGV("VideoEditorAudioPlayer::INSIDE MIXING");
                        ALOGV("Checking %lld <= %lld",
                            mBGAudioPCMFileSeekPoint-mBGAudioPCMFileOriginalSeekPoint,
                            mBGAudioPCMFileTrimmedLength);


                        M4OSA_Void* ptr;
                        ptr = (M4OSA_Void*)((unsigned int)mInputBuffer->data() +
                        mInputBuffer->range_offset());

                        M4OSA_UInt32 len = mInputBuffer->range_length();
                        M4OSA_Context fp = M4OSA_NULL;

                        uiPCMsize = (mInputBuffer->range_length())/2;
                        pPTMdata = (M4OSA_Int16*) ((uint8_t*) mInputBuffer->data()
                                + mInputBuffer->range_offset());

                        ALOGV("mix with background malloc to do len %d", len);

                        bgFrame.m_dataAddress = (M4OSA_UInt16*)M4OSA_32bitAlignedMalloc( len, 1,
                                                       (M4OSA_Char*)"bgFrame");
                        bgFrame.m_bufferSize = len;

                        mixFrame.m_dataAddress = (M4OSA_UInt16*)M4OSA_32bitAlignedMalloc(len, 1,
                                                    (M4OSA_Char*)"mixFrame");
                        mixFrame.m_bufferSize = len;

                        ALOGV("mix with bgm with size %lld", mBGAudioPCMFileLength);

                        CHECK(mInputBuffer->meta_data()->findInt64(kKeyTime,
                                         &mPositionTimeMediaUs));

                        if (mBGAudioPCMFileSeekPoint -
                             mBGAudioPCMFileOriginalSeekPoint <=
                              (mBGAudioPCMFileTrimmedLength - len)) {

                            ALOGV("Checking mBGAudioPCMFileHandle %d",
                                (unsigned int)mBGAudioPCMFileHandle);

                            if (mBGAudioPCMFileHandle != M4OSA_NULL) {
                                ALOGV("fillBuffer seeking file to %lld",
                                    mBGAudioPCMFileSeekPoint);

                            // TODO : 32bits required for OSAL
                                M4OSA_UInt32 tmp32 =
                                    (M4OSA_UInt32)mBGAudioPCMFileSeekPoint;
                                err = M4OSA_fileReadSeek(mBGAudioPCMFileHandle,
                                                M4OSA_kFileSeekBeginning,
                                                (M4OSA_FilePosition*)&tmp32);

                                mBGAudioPCMFileSeekPoint = tmp32;

                                if (err != M4NO_ERROR){
                                    ALOGE("M4OSA_fileReadSeek err %d",(int)err);
                                }

                                err = M4OSA_fileReadData(mBGAudioPCMFileHandle,
                                       (M4OSA_Int8*)bgFrame.m_dataAddress,
                                       (M4OSA_UInt32*)&len);
                                if (err == M4WAR_NO_DATA_YET ) {

                                    ALOGV("fillBuffer End of file reached");
                                    err = M4NO_ERROR;

                                    // We reached the end of file
                                    // move to begin cut time equal value
                                    if (mAudioMixSettings->bLoop) {
                                        mBGAudioPCMFileSeekPoint =
                                         (((int64_t)(mAudioMixSettings->beginCutMs) *
                                          mAudioMixSettings->uiSamplingFrequency) *
                                          mAudioMixSettings->uiNbChannels *
                                           sizeof(M4OSA_UInt16)) / 1000;
                                        ALOGV("fillBuffer Looping \
                                            to mBGAudioPCMFileSeekPoint %lld",
                                            mBGAudioPCMFileSeekPoint);
                                    }
                                    else {
                                            // No mixing;
                                            // take care of volume of primary track
                                        if (fPTVolLevel < 1.0) {
                                            setPrimaryTrackVolume(pPTMdata,
                                             uiPCMsize, fPTVolLevel);
                                        }
                                    }
                                } else if (err != M4NO_ERROR ) {
                                     ALOGV("fileReadData for audio err %d", err);
                                } else {
                                    mBGAudioPCMFileSeekPoint += len;
                                    ALOGV("fillBuffer mBGAudioPCMFileSeekPoint \
                                         %lld", mBGAudioPCMFileSeekPoint);

                                    // Assign the ptr data to primary track
                                    ptFrame.m_dataAddress = (M4OSA_UInt16*)ptr;
                                    ptFrame.m_bufferSize = len;

                                    // Call to mix and duck
                                    mAudioProcess->mixAndDuck(
                                         &ptFrame, &bgFrame, &mixFrame);

                                        // Overwrite the decoded buffer
                                    memcpy((void *)ptr,
                                         (void *)mixFrame.m_dataAddress, len);
                                }
                            }
                        } else if (mAudioMixSettings->bLoop){
                            // Move to begin cut time equal value
                            mBGAudioPCMFileSeekPoint =
                                mBGAudioPCMFileOriginalSeekPoint;
                        } else {
                            // No mixing;
                            // take care of volume level of primary track
                            if(fPTVolLevel < 1.0) {
                                setPrimaryTrackVolume(
                                      pPTMdata, uiPCMsize, fPTVolLevel);
                            }
                        }
                        if (bgFrame.m_dataAddress) {
                            free(bgFrame.m_dataAddress);
                        }
                        if (mixFrame.m_dataAddress) {
                            free(mixFrame.m_dataAddress);
                        }
                    } else {
                        // No mixing;
                        // take care of volume level of primary track
                        if(fPTVolLevel < 1.0) {
                            setPrimaryTrackVolume(pPTMdata, uiPCMsize,
                                                 fPTVolLevel);
                        }
                    }
                }
            }

            CHECK((status == OK && mInputBuffer != NULL)
                   || (status != OK && mInputBuffer == NULL));

            Mutex::Autolock autoLock(mLock);

            if (status != OK) {
                ALOGV("fillBuffer: mSource->read returned err %d", status);
                if (mObserver && !mReachedEOS) {
                    postEOS = true;
                }

                mReachedEOS = true;
                mFinalStatus = status;
                break;
            }

            CHECK(mInputBuffer->meta_data()->findInt64(
                        kKeyTime, &mPositionTimeMediaUs));

            mPositionTimeRealUs =
                ((mNumFramesPlayed + size_done / mFrameSize) * 1000000)
                    / mSampleRate;

            ALOGV("buffer->size() = %d, "
                     "mPositionTimeMediaUs=%.2f mPositionTimeRealUs=%.2f",
                 mInputBuffer->range_length(),
                 mPositionTimeMediaUs / 1E6, mPositionTimeRealUs / 1E6);
        }

        if (mInputBuffer->range_length() == 0) {
            mInputBuffer->release();
            mInputBuffer = NULL;

            continue;
        }

        size_t copy = size_remaining;
        if (copy > mInputBuffer->range_length()) {
            copy = mInputBuffer->range_length();
        }

        memcpy((char *)data + size_done,
           (const char *)mInputBuffer->data() + mInputBuffer->range_offset(),
               copy);

        mInputBuffer->set_range(mInputBuffer->range_offset() + copy,
                            mInputBuffer->range_length() - copy);

        size_done += copy;
        size_remaining -= copy;
    }

    {
        Mutex::Autolock autoLock(mLock);
        mNumFramesPlayed += size_done / mFrameSize;
    }

    if (postEOS) {
        mObserver->postAudioEOS();
    }

    if (postSeekComplete) {
        mObserver->postAudioSeekComplete();
    }

    return size_done;
}

void VideoEditorAudioPlayer::setAudioMixSettings(
                            M4xVSS_AudioMixingSettings* pAudioMixSettings) {
    mAudioMixSettings = pAudioMixSettings;
}

void VideoEditorAudioPlayer::setAudioMixPCMFileHandle(
                            M4OSA_Context pBGAudioPCMFileHandle){
    mBGAudioPCMFileHandle = pBGAudioPCMFileHandle;
}

void VideoEditorAudioPlayer::setAudioMixStoryBoardSkimTimeStamp(
                            M4OSA_UInt32 pBGAudioStoryBoardSkimTimeStamp,
                            M4OSA_UInt32 pBGAudioCurrentMediaBeginCutTS,
                            M4OSA_UInt32 pBGAudioCurrentMediaVolumeVal) {

    mBGAudioStoryBoardSkimTimeStamp = pBGAudioStoryBoardSkimTimeStamp;
    mBGAudioStoryBoardCurrentMediaBeginCutTS = pBGAudioCurrentMediaBeginCutTS;
    mBGAudioStoryBoardCurrentMediaVolumeVal = pBGAudioCurrentMediaVolumeVal;
}

void VideoEditorAudioPlayer::setPrimaryTrackVolume(
    M4OSA_Int16 *data, M4OSA_UInt32 size, M4OSA_Float volLevel) {

    while(size-- > 0) {
        *data = (M4OSA_Int16)((*data)*volLevel);
        data++;
    }
}

}
