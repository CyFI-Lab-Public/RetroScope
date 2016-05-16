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
 * @file    M4AD_Null.c
 * @brief   Implementation of the MP3 decoder public interface
 * @note    This file implements a "null" audio decoder, that is a decoder
 *          that do nothing except getting AU from the reader
*************************************************************************
*/
#include "M4OSA_Debug.h"
#include "M4OSA_Error.h"
#include "M4OSA_Debug.h"
#include "M4TOOL_VersionInfo.h"
#include "M4AD_Common.h"
#include "M4AD_Null.h"

#define M4AD_FORCE_16BITS

/**
 ************************************************************************
 * NULL Audio Decoder version information
 ************************************************************************
*/
/* CHANGE_VERSION_HERE */
#define M4AD_NULL_MAJOR    1
#define M4AD_NULL_MINOR    1
#define M4AD_NULL_REVISION 4

/**
 ************************************************************************
 * structure    M4AD_NullContext
 * @brief        Internal null decoder context
 ************************************************************************
*/
typedef struct
{
    /**< Pointer to the stream handler provided by the user */
    M4_AudioStreamHandler*    m_pAudioStreamhandler;
} M4AD_NullContext;


/**
 ************************************************************************
 * NXP MP3 decoder functions definition
 ************************************************************************
*/

/**
 ************************************************************************
 * @brief   Creates an instance of the null decoder
 * @note    Allocates the context
 *
 * @param    pContext:        (OUT)    Context of the decoder
 * @param    pStreamHandler: (IN)    Pointer to an audio stream description
 * @param    pUserData:        (IN)    Pointer to User data
 *
 * @return    M4NO_ERROR              there is no error
 * @return    M4ERR_STATE             State automaton is not applied
 * @return    M4ERR_ALLOC             a memory allocation has failed
 * @return    M4ERR_PARAMETER         at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR    M4AD_NULL_create(  M4AD_Context* pContext,
                                M4_AudioStreamHandler *pStreamHandler,
                                void* pUserData)
{
    M4AD_NullContext* pC;

    M4OSA_DEBUG_IF1((pContext == 0), M4ERR_PARAMETER,
                "M4AD_NULL_create: invalid context pointer");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
                "M4AD_NULL_create: invalid pointer pStreamHandler");

    pC = (M4AD_NullContext*)M4OSA_32bitAlignedMalloc(sizeof(M4AD_NullContext),
                 M4DECODER_AUDIO, (M4OSA_Char *)"M4AD_NullContext");
    if (pC == (M4AD_NullContext*)0)
    {
        M4OSA_TRACE1_0("Can not allocate null decoder context");
        return M4ERR_ALLOC;
    }

    *pContext = pC;

    pC->m_pAudioStreamhandler = pStreamHandler;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief    Destroys the instance of the null decoder
 * @note     After this call the context is invalid
 *
 * @param    context:    (IN)    Context of the decoder
 *
 * @return   M4NO_ERROR            There is no error
 * @return   M4ERR_PARAMETER     The context is invalid (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR    M4AD_NULL_destroy(M4AD_Context context)
{
    M4AD_NullContext* pC = (M4AD_NullContext*)context;

    M4OSA_DEBUG_IF1((context == M4OSA_NULL), M4ERR_PARAMETER, "M4AD_NULL_destroy: invalid context");

    free(pC);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Simply output the given audio data
 * @note
 *
 * @param   context:          (IN)    Context of the decoder
 * @param   pInputBuffer:     (IN/OUT)Input Data buffer. It contains at least one audio frame.
 *                                    The size of the buffer must be updated inside the function
 *                                    to reflect the size of the actually decoded data.
 *                                    (e.g. the first frame in pInputBuffer)
 * @param   pDecodedPCMBuffer: (OUT)  Output PCM buffer (decoded data).
 * @param   jumping:           (IN)   M4OSA_TRUE if a jump was just done, M4OSA_FALSE otherwise.
 * @return    M4NO_ERROR              there is no error
 * @return    M4ERR_PARAMETER         at least one parameter is not properly set
 ************************************************************************
*/
M4OSA_ERR    M4AD_NULL_step(M4AD_Context context, M4AD_Buffer *pInputBuffer,
                            M4AD_Buffer *pDecodedPCMBuffer, M4OSA_Bool jumping)
{
    M4AD_NullContext* pC = (M4AD_NullContext*)context;

    /*The VPS sends a zero buffer at the end*/
    if (0 == pInputBuffer->m_bufferSize)
    {
        return M4WAR_NO_MORE_AU;
    }

    if (pInputBuffer->m_bufferSize > pDecodedPCMBuffer->m_bufferSize)
    {
        return M4ERR_PARAMETER;
    }
#ifdef M4AD_FORCE_16BITS
    /*if read samples are 8 bits, complete them to 16 bits*/
    if (pC->m_pAudioStreamhandler->m_byteSampleSize == 1)
    {
        M4OSA_UInt32 i;
        M4OSA_Int16  val;

        for (i = 0; i < pInputBuffer->m_bufferSize; i++)
        {
            val = (M4OSA_Int16)((M4OSA_UInt8)(pInputBuffer->m_dataAddress[i]) - 128);

            pDecodedPCMBuffer->m_dataAddress[i*2]   = (M4OSA_Int8)(val>>8);
            pDecodedPCMBuffer->m_dataAddress[i*2+1] = (M4OSA_Int8)(val&0x00ff);
        }
    }
    else
    {
        memcpy((void *)pDecodedPCMBuffer->m_dataAddress, (void *)pInputBuffer->m_dataAddress,
                    pInputBuffer->m_bufferSize );
    }
#else /*M4AD_FORCE_16BITS*/
    memcpy((void *)pDecodedPCMBuffer->m_dataAddress, (void *)pInputBuffer->m_dataAddress,
                    pInputBuffer->m_bufferSize );
#endif /*M4AD_FORCE_16BITS*/

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Gets the decoder version
 * @note    The version is given in a M4_VersionInfo structure
 *
 * @param   pValue:     (OUT)       Pointer to the version structure
 *
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_PARAMETER         pVersionInfo pointer is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR    M4AD_NULL_getVersion(M4_VersionInfo* pVersionInfo)
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_DEBUG_IF1((pVersionInfo == 0), M4ERR_PARAMETER,
        "M4AD_NULL_getVersion: invalid pointer pVersionInfo");

    /* Up until now, the null decoder version is not available */

    /* CHANGE_VERSION_HERE */
    pVersionInfo->m_major        = M4AD_NULL_MAJOR;      /*major version of the component*/
    pVersionInfo->m_minor        = M4AD_NULL_MINOR;      /*minor version of the component*/
    pVersionInfo->m_revision    = M4AD_NULL_REVISION;    /*revision version of the component*/
    pVersionInfo->m_structSize=sizeof(M4_VersionInfo);

    return err;
}


/**
 ************************************************************************
 * getInterface function definitions of NXP MP3 decoder
 ************************************************************************
*/

/**
 ************************************************************************
 * @brief Retrieves the interface implemented by the decoder
 * @param pDecoderType        : pointer on an M4AD_Type (allocated by the caller)
 *                              that will be filled with the decoder type supported by
 *                              this decoder
 * @param pDecoderInterface   : address of a pointer that will be set to the interface
 *                              implemented by this decoder. The interface is a structure
 *                              allocated by the function and must be un-allocated by the
 *                              caller.
 *
 * @return    M4NO_ERROR  if OK
 * @return    M4ERR_ALLOC if allocation failed
 ************************************************************************
*/
M4OSA_ERR M4AD_NULL_getInterface( M4AD_Type *pDecoderType, M4AD_Interface **pDecoderInterface)
{
    *pDecoderInterface = (  M4AD_Interface*)M4OSA_32bitAlignedMalloc( sizeof(M4AD_Interface),
                            M4DECODER_AUDIO, (M4OSA_Char *)"M4AD_Interface" );
    if (M4OSA_NULL == *pDecoderInterface)
    {
        return M4ERR_ALLOC;
    }

    *pDecoderType = M4AD_kTypePCM;

    (*pDecoderInterface)->m_pFctCreateAudioDec       = M4AD_NULL_create;
    (*pDecoderInterface)->m_pFctDestroyAudioDec      = M4AD_NULL_destroy;
    (*pDecoderInterface)->m_pFctStepAudioDec         = M4AD_NULL_step;
    (*pDecoderInterface)->m_pFctGetVersionAudioDec   = M4AD_NULL_getVersion;
    (*pDecoderInterface)->m_pFctStartAudioDec        = M4OSA_NULL;
    (*pDecoderInterface)->m_pFctResetAudioDec        = M4OSA_NULL;
    (*pDecoderInterface)->m_pFctSetOptionAudioDec    = M4OSA_NULL;
    (*pDecoderInterface)->m_pFctGetOptionAudioDec    = M4OSA_NULL;

    return M4NO_ERROR;
}

