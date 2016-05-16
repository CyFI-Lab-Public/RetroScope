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
*************************************************************************
 * @file   M4_Common.h
 * @brief  Common data structure between shells
 * @note
*************************************************************************
*/
#ifndef __M4_COMMON_H__
#define __M4_COMMON_H__

#include "M4OSA_Types.h"

/**
 ************************************************************************
 * structure    _parameterSet
 * @brief        This structure defines the structure of parameters for the avc
 *               decoder specific info
 * @note
 ************************************************************************
*/
typedef struct _parameterSet
{
    M4OSA_UInt16 m_length;                /* Number of items*/
    M4OSA_UInt8* m_pParameterSetUnit;   /* Array of items*/
} ParameterSet ;

/**
 ************************************************************************
 * structure    _avcSpecificInfo
 * @brief        This structure defines the structure of specific info for the avc decoder
 * @note
 ************************************************************************
*/
typedef struct _avcSpecificInfo
{
    M4OSA_UInt8        m_nalUnitLength;                /* length in bytes of the NALUnitLength
                                                            field in a AVC sample */
    M4OSA_UInt8        m_numOfSequenceParameterSets;   /* Number of sequence parameter sets*/
    M4OSA_UInt8        m_numOfPictureParameterSets;    /* Number of picture parameter sets*/
    ParameterSet    *m_pSequenceParameterSet;        /* Sequence parameter sets array*/
    ParameterSet    *m_pPictureParameterSet;        /* Picture parameter sets array*/
} AvcSpecificInfo ;

/**
 ************************************************************************
 * structure    M4_SynthesisAudioInfo
 * @brief        This structure contains specific pointers used for synthesis audio format
 ************************************************************************
*/
typedef struct _synthesisAudioInfo
{
    M4OSA_Void*        m_pInputBuf;
    M4OSA_Void*        m_pInputInfo;
    M4OSA_UInt16    m_uiNbSubFramePerStep;
    M4OSA_UInt32    m_uiUsedBytes;
} M4_SynthesisAudioInfo;


/*
 ************************************************************************
 * enum     M4_AACDownsamplingMode
 * @brief   This enum states modes for Down sampling
 ************************************************************************
*/
typedef enum
{
    AAC_kDS_OFF    = 0,        /**< No Down sampling */
    AAC_kDS_BY_2   = 1,        /**< Down sampling by 2
                                 Profile = AAC :
                                            output sampling rate = aac_samp_freq/2
                                 Profile = HE_AAC and input is AAC:
                                            Output sampling rate = aac_samp_freq.(No downsamping).
                                 Profile = HE_AAC and input is HE_AAC:
                                            Output sampling rate = aac_samp_freq (Downsampling
                                            occurs in SBR tool).
                                 case profile = HE_AAC_v2 :
                                            Not Supported */
    AAC_kDS_BY_3   = 2,        /**< Down sampling by 3  - only for AAC profile */
    AAC_kDS_BY_4   = 3,        /**< Down sampling by 4  - only for AAC profile */
    AAC_kDS_BY_8   = 4        /**< Down sampling by 8  - only for AAC profile */

} M4_AACDownsamplingMode;


/*
 ************************************************************************
 * enum     M4_AACOutputMode
 * @brief   This enum defines the output mode
 ************************************************************************
*/
typedef enum
{
    AAC_kMono      = 0,    /**< Output is Mono  */
    AAC_kStereo    = 1     /**< Output is Stereo */
} M4_AACOutputMode;


/*
 ************************************************************************
 * enum     M4_AACDecProfile
 * @brief   This enum defines the AAC decoder profile
 ************************************************************************
*/
typedef enum
{
    AAC_kAAC       = 0,        /**< AAC profile (only AAC LC object are supported) */
    AAC_kHE_AAC    = 1,        /**< HE AAC or AAC+ profile (SBR in LP Mode)  */
    AAC_kHE_AAC_v2 = 2        /**< HE AAC v2 or Enhanced AAC+ profile (SBR Tool in HQ Mode) */
} M4_AACDecProfile;


/**
 ************************************************************************
 * structure    M4_AacDecoderConfig
 * @brief        This structure defines specific settings according to
 *                the user requirements
 ************************************************************************
*/
typedef struct
{
    M4_AACDecProfile        m_AACDecoderProfile;
    M4_AACDownsamplingMode    m_DownSamplingMode;
    M4_AACOutputMode        m_OutputMode;

} M4_AacDecoderConfig;


/**
 ************************************************************************
 * structure M4READER_AudioSbrUserdata
 * @brief    This structure defines the user's data needed to decode the
 *            AACplus stream
 * @note    The field m_pFirstAU is used in case of local files    and
 *            the field m_bIsSbrEnabled is used in streaming case.
 ************************************************************************
*/
typedef struct
{
  M4OSA_Void*            m_pFirstAU;                /**< The first AU from where SBR data are
                                                         extracted (local file case)*/
  M4OSA_Bool            m_bIsSbrEnabled;        /**< A boolean that indicates if the stream is
                                                    AACplus (streaming case)*/
  M4_AacDecoderConfig*    m_pAacDecoderUserConfig;/**< Decoder specific user setting */

} M4READER_AudioSbrUserdata;

#endif /* __M4_COMMON_H__*/

