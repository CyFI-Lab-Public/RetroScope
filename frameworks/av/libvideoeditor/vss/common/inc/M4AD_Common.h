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
 ************************************************************************
 * @fil        M4AD_Common.h
 * @brief    Audio Shell Decoder common interface declaration
 * @note    This file declares the common interfaces that audio decoder shells must implement
 ************************************************************************
*/
#ifndef __M4AD_COMMON_H__
#define __M4AD_COMMON_H__

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_OptionID.h"
#include "M4OSA_CoreID.h"
#include "M4DA_Types.h"
#include "M4TOOL_VersionInfo.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef M4OSA_Void* M4AD_Context;

/**
 ************************************************************************
 * enum     M4AD_OptionID
 * @brief    This enum defines the Audio decoder options.
 * @note    These options can be read from or written to a decoder via
 *            M4AD_getOption_fct/M4AD_setOption_fct
 ************************************************************************
*/
typedef enum
{
    /**
     * Set the flag of presence of protection */
    M4AD_kOptionID_ProtectionAbsent = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4DECODER_AUDIO, 0x01),

    /**
     * Set the number of frames per bloc */
    M4AD_kOptionID_NbFramePerBloc    = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4DECODER_AUDIO, 0x02),

    /**
     * Set the AAC decoder user parameters */
    M4AD_kOptionID_UserParam        = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4DECODER_AUDIO, 0x03),


    /**
     * Get the AAC steam type */
    M4AD_kOptionID_StreamType        = M4OSA_OPTION_ID_CREATE(M4_READ , M4DECODER_AUDIO, 0x10),

    /**
     * Get the number of used bytes in the latest decode
     (used only when decoding AAC from ADIF file) */
    M4AD_kOptionID_UsedBytes        = M4OSA_OPTION_ID_CREATE(M4_READ , M4DECODER_AUDIO, 0x11),

    /* Reader Interface */
    M4AD_kOptionID_3gpReaderInterface = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x012),

    /* Audio Access Unit */
    M4AD_kOptionID_AudioAU = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x13),

    /* Reader error code */
    M4AD_kOptionID_GetAudioAUErrCode = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x14),

    /* Number of channels */
    M4AD_kOptionID_AudioNbChannels = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x15),

    /* Sampling frequency */
    M4AD_kOptionID_AudioSampFrequency = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x16),

    /* Audio AU CTS */
    M4AD_kOptionID_AuCTS = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AUDIO, 0x17)

} M4AD_OptionID;



typedef enum
{
    M4_kUnknown = 0,    /* Unknown stream type */
    M4_kAAC,            /* M4_kAAC_MAIN or M4_kAAC_LC or M4_kAAC_SSR or M4_kAAC_LTP    */
    M4_kAACplus,        /* Decoder type is AAC plus */
    M4_keAACplus        /* Decoder type is enhanced AAC plus */
} M4_AACType;

/**
 ************************************************************************
 * enum     M4AD_Type
 * @brief    This enum defines the audio types used to create decoders
 * @note    This enum is used internally by the VPS to identify a currently supported
 *            audio decoder interface. Each decoder is registered with one of this type associated.
 *            When a decoder instance is needed, this type is used to identify
 *            and retrieve its interface.
 ************************************************************************
*/
typedef enum
{
    M4AD_kTypeAMRNB = 0,
    M4AD_kTypeAMRWB,
    M4AD_kTypeAAC,
    M4AD_kTypeMP3,
    M4AD_kTypePCM,
    M4AD_kTypeBBMusicEngine,
    M4AD_kTypeWMA,
    M4AD_kTypeRMA,
    M4AD_kTypeADPCM,
    M4AD_kType_NB  /* number of decoders, keep it as last enum entry */

} M4AD_Type ;



/**
 ************************************************************************
 * structure    M4AD_Buffer
 * @brief        Structure to describe a buffer
 ************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8    m_dataAddress;
    M4OSA_UInt32    m_bufferSize;
    int64_t         m_timeStampUs;
} M4AD_Buffer;

/**
 ************************************************************************
 * @brief    Creates an instance of the decoder
 * @note    Allocates the context
 *
 * @param    pContext:        (OUT)    Context of the decoder
 * @param    pStreamHandler:    (IN)    Pointer to an audio stream description
 * @param    pUserData:        (IN)    Pointer to User data
 *
 * @return    M4NO_ERROR                 there is no error
 * @return  M4ERR_STATE             State automaton is not applied
 * @return    M4ERR_ALLOC                a memory allocation has failed
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
*/

typedef M4OSA_ERR  (M4AD_create_fct)(M4AD_Context *pContext,
                                     M4_AudioStreamHandler *pStreamHandler, void* pUserData);


/**
 ************************************************************************
 * @brief    Destroys the instance of the decoder
 * @note    After this call the context is invalid
 *
 * @param    context:    (IN)    Context of the decoder
 *
 * @return    M4NO_ERROR             There is no error
 * @return  M4ERR_PARAMETER     The context is invalid (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4AD_destroy_fct)    (M4AD_Context context);

/**
 ************************************************************************
 * @brief   Decodes the given audio data
 * @note    Parses and decodes the next audio frame, from the given buffer.
 *            This function changes pInputBufferSize value according to the amount
 *            of data actually read.
 *
 * @param    context:            (IN)    Context of the decoder
 * @param    inputBuffer:        (IN/OUT)Input Data buffer. It contains at least one audio frame.
 *                                       The size of the buffer must be updated inside the
 *                                       function to reflect the size of the actually decoded data.
 *                                       (e.g. the first frame in pInputBuffer)
 * @param   decodedPCMBuffer:    (OUT)   Output PCM buffer (decoded data).
 * @param   jumping:            (IN)    M4OSA_TRUE if a jump was just done, M4OSA_FALSE otherwise.
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 ************************************************************************
*/
typedef M4OSA_ERR  (M4AD_step_fct)    (M4AD_Context context, M4AD_Buffer *pInputBuffer,
                                     M4AD_Buffer *pDecodedPCMBuffer, M4OSA_Bool jumping);

/**
 ************************************************************************
 * @brief    Gets the decoder version
 * @note    The version is given in a M4_VersionInfo structure
 *
 * @param    pValue:        (OUT)        Pointer to the version structure
 *
 * @return    M4NO_ERROR                 there is no error
 * @return  M4ERR_PARAMETER         The given pointer is null (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4AD_getVersion_fct)(M4_VersionInfo* pVersionInfo);


/**
 ************************************************************************
 * @brief    This function creates the AAC core decoder according to
 *            the stream properties and to the options that may
 *            have been set using M4AD_setOption_fct
 * @note    Creates an instance of the AAC decoder
 * @note    This function is used especially by the AAC decoder
 *
 * @param    pContext:        (IN/OUT)    Context of the decoder
 * @param    pStreamHandler:    (IN)    Pointer to an audio stream description
 *
 * @return    M4NO_ERROR                 there is no error
 * @return  M4ERR_STATE             State automaton is not applied
 * @return    M4ERR_ALLOC                a memory allocation has failed
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4AD_start_fct)    (M4AD_Context pContext);

/**
 ************************************************************************
 * @brief    Reset the instance of the decoder
 *
 * @param    context:    (IN)    Context of the decoder
 *
 * @return    M4NO_ERROR             There is no error
 * @return  M4ERR_PARAMETER     The context is invalid (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4AD_reset_fct)    (M4AD_Context context);


/**
 ************************************************************************
 * @brief   set en option value of the audio decoder
 *
 * @param    context:        (IN)    Context of the decoder
 * @param    optionId:        (IN)    indicates the option to set
 * @param    pValue:            (IN)    pointer to structure or value (allocated by user)
 *                                  where option is stored
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4AD_setOption_fct) (M4AD_Context context,
                                         M4OSA_OptionID optionId, M4OSA_DataOption pValue);

/**
 ************************************************************************
 * @brief   Get en option value of the audio decoder
 *
 * @param    context:        (IN)    Context of the decoder
 * @param    optionId:        (IN)    indicates the option to set
 * @param    pValue:            (OUT)    pointer to structure or value (allocated by user)
 *                                  where option is stored
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4AD_getOption_fct) (M4AD_Context context, M4OSA_OptionID optionId,
                                         M4OSA_DataOption pValue);
/**
 ************************************************************************
 * structure    M4AD_Interface
 * @brief        This structure defines the generic audio decoder interface
 * @note        This structure stores the pointers to functions of one audio decoder type.
 *                The decoder type is one of the M4AD_Type
 ************************************************************************
*/
typedef struct _M4AD_Interface
{

    M4AD_create_fct*        m_pFctCreateAudioDec;
    M4AD_start_fct*            m_pFctStartAudioDec;
    M4AD_step_fct*            m_pFctStepAudioDec;
    M4AD_getVersion_fct*    m_pFctGetVersionAudioDec;
    M4AD_destroy_fct*        m_pFctDestroyAudioDec;
    M4AD_reset_fct*            m_pFctResetAudioDec;
    M4AD_setOption_fct*        m_pFctSetOptionAudioDec;
    M4AD_getOption_fct*        m_pFctGetOptionAudioDec;

} M4AD_Interface;

#ifdef __cplusplus
}
#endif

#endif /*__M4AD_COMMON_H__*/

