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
/**
 ******************************************************************************
 * @file    M4ENCODER_AudioCommon.h
 * @brief    VES audio encoders shell interface.
 * @note    This file defines the types internally used by the VES to abstract audio encoders
 ******************************************************************************
*/
#ifndef __M4ENCODER_AUDIOCOMMON_H__
#define __M4ENCODER_AUDIOCOMMON_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "M4OSA_OptionID.h"     /* for M4OSA_OPTION_ID_CREATE() */
#include "M4OSA_CoreID.h"

#define M4ENCODER_AUDIO_NB_CHANNELS_MAX 2
/* WARNING: this value must be equal to the number of samples grabbed */
//#define M4ENCODER_AUDIO_PCM_SAMPLE_NUMBER 960    /* imposed by the AAC encoder. */
#define M4ENCODER_AUDIO_PCM_SAMPLE_NUMBER 1024    /* imposed by the AAC encoder. */


/**
 ******************************************************************************
 * enumeration    M4ENCODER_Audio_OptionID
 * @brief        This enum defines the core AAC shell encoder options
 ******************************************************************************
*/
typedef enum
{
 /* Maximum generated AU size */
    M4ENCODER_Audio_maxAUsize     = M4OSA_OPTION_ID_CREATE(M4_READ,M4ENCODER_AUDIO, 0x01)

} M4ENCODER_Audio_OptionID;


 /**
 ******************************************************************************
 * enum        M4ENCODER_SamplingFrequency
 * @brief    Thie enum defines the audio sampling frequency.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_k8000Hz = 8000,
    M4ENCODER_k11025Hz = 11025,
    M4ENCODER_k12000Hz = 12000,
    M4ENCODER_k16000Hz = 16000,
    M4ENCODER_k22050Hz = 22050,
    M4ENCODER_k24000Hz = 24000,
    M4ENCODER_k32000Hz = 32000,
    M4ENCODER_k44100Hz = 44100,
    M4ENCODER_k48000Hz = 48000
} M4ENCODER_SamplingFrequency;


/**
 ******************************************************************************
 * enum        M4ENCODER_AudioFormat
 * @brief    This enum defines the audio compression formats.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kAMRNB = 0,
    M4ENCODER_kAAC,
    M4ENCODER_kAudioNULL,    /**< No compression */
    M4ENCODER_kMP3,
    M4ENCODER_kAudio_NB        /* number of encoders, keep it as last enum entry */

} M4ENCODER_AudioFormat;

/**
 ******************************************************************************
 * enum        M4ENCODER_ChannelNumber
 * @brief    Thie enum defines the number of audio channels.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kMono  = 0,
    M4ENCODER_kStereo,
    M4ENCODER_kStereoNoInterleave
} M4ENCODER_ChannelNumber;

/**
 ******************************************************************************
 * enum        M4ENCODER_AudioBitrate
 * @brief    Thie enum defines the avalaible bitrates.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kAudio_4_75_KBPS    = 4750,
    M4ENCODER_kAudio_5_15_KBPS    = 5150,
    M4ENCODER_kAudio_5_9_KBPS    = 5900,
    M4ENCODER_kAudio_6_7_KBPS    = 6700,
    M4ENCODER_kAudio_7_4_KBPS    = 7400,
    M4ENCODER_kAudio_7_95_KBPS    = 7950,
    M4ENCODER_kAudio_8_KBPS        = 8000,
    M4ENCODER_kAudio_10_2_KBPS    = 10200,
    M4ENCODER_kAudio_12_2_KBPS    = 12200,
    M4ENCODER_kAudio_16_KBPS    = 16000,
    M4ENCODER_kAudio_24_KBPS    = 24000,
    M4ENCODER_kAudio_32_KBPS    = 32000,
    M4ENCODER_kAudio_40_KBPS    = 40000,
    M4ENCODER_kAudio_48_KBPS    = 48000,
    M4ENCODER_kAudio_56_KBPS    = 56000,
    M4ENCODER_kAudio_64_KBPS    = 64000,
    M4ENCODER_kAudio_80_KBPS    = 80000,
    M4ENCODER_kAudio_96_KBPS    = 96000,
    M4ENCODER_kAudio_112_KBPS    = 112000,
    M4ENCODER_kAudio_128_KBPS    = 128000,
    M4ENCODER_kAudio_144_KBPS    = 144000,
    M4ENCODER_kAudio_160_KBPS    = 160000,
    M4ENCODER_kAudio_192_KBPS    = 192000,
    M4ENCODER_kAudio_224_KBPS    = 224000,
    M4ENCODER_kAudio_256_KBPS    = 256000,
    M4ENCODER_kAudio_320_KBPS    = 320000
} M4ENCODER_AudioBitrate;


/**
 ******************************************************************************
 * enum            M4ENCODER_AacRegulation
 * @brief        The current mode of the bitrate regulation.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kAacRegulNone = 0,    /**< no bitrate regulation */
    M4ENCODER_kAacBitReservoir        /**< better quality, but more CPU consumed */
} M4ENCODER_AacRegulation;

/**
 ******************************************************************************
 * enum        M4ENCODER_AmrSID
 * @brief    This enum defines the SID of the AMR encoder.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kAmrNoSID = 0     /**< no SID */
} M4ENCODER_AmrSID;

/**
 ******************************************************************************
 * struct    M4ENCODER_AacParams
 * @brief    This structure defines all the settings specific to the AAC encoder.
 ******************************************************************************
*/
typedef struct
{
    M4ENCODER_AacRegulation    Regulation;
    M4OSA_Bool                bHighSpeed;
    M4OSA_Bool                bTNS;
    M4OSA_Bool                bPNS;
    M4OSA_Bool                bIS;
    M4OSA_Bool                bMS;
} M4ENCODER_AacParams;

/**
 ******************************************************************************
 * struct    M4ENCODER_AudioParams
 * @brief    This structure defines all the settings avalaible when encoding audio.
 ******************************************************************************
*/
typedef struct s_M4ENCODER_AudioParams
{
    M4ENCODER_SamplingFrequency    Frequency;    /**< the sampling frequency */
    M4ENCODER_ChannelNumber        ChannelNum;    /**< the numbe of channels (mono, stereo, ..) */
    M4ENCODER_AudioBitrate        Bitrate;    /**<  bitrate, see enum  */
    M4ENCODER_AudioFormat        Format;        /**<  audio compression format, AMR, AAC ...  */
    union {
        M4ENCODER_AacParams        AacParam;
        M4ENCODER_AmrSID        AmrSID;
    } SpecifParam;                            /**< the audio encoder specific parameters */
} M4ENCODER_AudioParams;

/**
 ******************************************************************************
 * struct    M4ENCODER_AudioDecSpecificInfo
 * @brief    This structure describes the decoder specific info buffer.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8    pInfo;        /**< the buffer adress */
    M4OSA_UInt32    infoSize;    /**< the buffer size in bytes */
} M4ENCODER_AudioDecSpecificInfo;

/**
 ******************************************************************************
 * struct    M4ENCODER_AudioBuffer
 * @brief    This structure defines the data buffer.
 ******************************************************************************
*/
typedef struct
{
    /**< the table of buffers (unused buffers are set to NULL) */
    M4OSA_MemAddr8    pTableBuffer[M4ENCODER_AUDIO_NB_CHANNELS_MAX];
    /**< the table of the size of corresponding buffer at same index */
    M4OSA_UInt32    pTableBufferSize[M4ENCODER_AUDIO_NB_CHANNELS_MAX];
} M4ENCODER_AudioBuffer;

typedef M4OSA_ERR (M4AE_init)        (M4OSA_Context* hContext, M4OSA_Void* pUserData);
typedef M4OSA_ERR (M4AE_cleanUp)    (M4OSA_Context pContext);
typedef M4OSA_ERR (M4AE_open)        (M4OSA_Context pContext, M4ENCODER_AudioParams *params,
                                        M4ENCODER_AudioDecSpecificInfo *decSpecInfo,
                                        M4OSA_Context grabberContext);
typedef M4OSA_ERR (M4AE_close)        (M4OSA_Context pContext);
typedef M4OSA_ERR (M4AE_step)         (M4OSA_Context pContext, M4ENCODER_AudioBuffer *inBuffer,
                                        M4ENCODER_AudioBuffer *outBuffer);
typedef M4OSA_ERR (M4AE_getOption)    (M4OSA_Context pContext, M4OSA_OptionID    option,
                                        M4OSA_DataOption *valuePtr);
/**
 ******************************************************************************
 * struct    M4ENCODER_AudioGlobalInterface
 * @brief    Defines all the functions required for an audio encoder shell.
 ******************************************************************************
*/
typedef struct _M4ENCODER_AudioGlobalInterface
{
    M4AE_init*        pFctInit;
    M4AE_cleanUp*    pFctCleanUp;
    M4AE_open*        pFctOpen;
    M4AE_close*        pFctClose;
    M4AE_step*        pFctStep;
    M4AE_getOption*    pFctGetOption;
} M4ENCODER_AudioGlobalInterface;


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4ENCODER_AUDIOCOMMON_H__*/

