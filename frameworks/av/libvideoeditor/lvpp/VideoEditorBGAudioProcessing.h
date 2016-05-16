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

#ifndef VE_BACKGROUND_AUDIO_PROC_H
#define VE_BACKGROUND_AUDIO_PROC_H

#include "M4OSA_Error.h"
#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Export.h"
#include "M4OSA_CoreID.h"


namespace android {

typedef struct {
    M4OSA_UInt16*   m_dataAddress; // Android SRC needs a Int16 pointer
    M4OSA_UInt32    m_bufferSize;
} M4AM_Buffer16;    // Structure contains Int16_t pointer

enum AudioFormat {
    MONO_16_BIT,
    STEREO_16_BIT
};

// Following struct will be used by app to supply the PT and BT properties
// along with ducking values
typedef struct {
    M4OSA_Int32 lvInSampleRate; // Sampling audio freq (8000,16000 or more )
    M4OSA_Int32 lvOutSampleRate; //Sampling audio freq (8000,16000 or more )
    AudioFormat lvBTFormat;

    M4OSA_Int32 lvInDucking_threshold;
    M4OSA_Float lvInDucking_lowVolume;
    M4OSA_Bool lvInDucking_enable;
    M4OSA_Float lvPTVolLevel;
    M4OSA_Float lvBTVolLevel;
    M4OSA_Int32 lvBTChannelCount;
    M4OSA_Int32 lvPTChannelCount;
} AudioMixSettings;

// This class is defined to get SF SRC access
class VideoEditorBGAudioProcessing {
public:
    VideoEditorBGAudioProcessing();
    ~VideoEditorBGAudioProcessing() {}

    void setMixParams(const AudioMixSettings& params);

    M4OSA_Int32 mixAndDuck(
                    void* primaryTrackBuffer,
                    void* backgroundTrackBuffer,
                    void* mixedOutputBuffer);

private:
    enum {
        kProcessingWindowSize = 10,
    };

    M4OSA_Int32 mInSampleRate;
    M4OSA_Int32 mOutSampleRate;
    AudioFormat mBTFormat;

    M4OSA_Bool mIsSSRCneeded;
    M4OSA_Int32 mBTChannelCount;
    M4OSA_Int32 mPTChannelCount;
    M4OSA_UInt8 mChannelConversion;

    M4OSA_UInt32 mDucking_threshold;
    M4OSA_Float mDucking_lowVolume;
    M4OSA_Float mDuckingFactor ;
    M4OSA_Bool mDucking_enable;
    M4OSA_Int32 mAudioVolumeArray[kProcessingWindowSize];
    M4OSA_Int32 mAudVolArrIndex;
    M4OSA_Bool mDoDucking;
    M4OSA_Float mPTVolLevel;
    M4OSA_Float mBTVolLevel;

    M4AM_Buffer16 mBTBuffer;

    M4OSA_Int32 getDecibelSound(M4OSA_UInt32 value);
    M4OSA_Bool  isThresholdBreached(M4OSA_Int32* averageValue,
                    M4OSA_Int32 storeCount, M4OSA_Int32 thresholdValue);

    // This returns the size of buffer which needs to allocated
    // before resampling is called
    M4OSA_Int32 calculateOutResampleBufSize();

    // Don't call me.
    VideoEditorBGAudioProcessing(const VideoEditorBGAudioProcessing&);
    VideoEditorBGAudioProcessing& operator=(
            const VideoEditorBGAudioProcessing&);
};

}  // namespace android

#endif // VE_BACKGROUND_AUDIO_PROC_H
