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
 * @file   M4READER_Wav.c
 * @brief  Generic encapsulation of the core pcm reader
 * @note   This file implements the generic M4READER interface
 *         on top of the PCM reader
 ************************************************************************
*/

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"
#include "M4OSA_CoreID.h"
#include "M4TOOL_VersionInfo.h"
#include "M4PCMR_CoreReader.h"
#include "M4READER_Pcm.h"
/**
 ************************************************************************
 * structure    M4READER_WAV_Context
 * @brief       This structure defines the internal context of a wav reader instance
 * @note        The context is allocated and de-allocated by the reader
 ************************************************************************
 */
typedef struct _M4READER_PCM_Context
{
    M4OSA_Context           m_coreContext;        /**< core wav reader context */
    M4_StreamHandler*       m_pAudioStream;       /**< pointer on the audio stream description
                                                        returned by the core */
    M4SYS_AccessUnit        m_audioAu;            /**< audio access unit to be filled by the core */
    M4OSA_FileReadPointer*  m_pOsaFileReaderFcts; /**< OSAL file read functions */

} M4READER_PCM_Context;


/**
 ************************************************************************
 * @brief   Creates a wav reader instance
 * @note    allocates the context
 * @param   pContext:            (OUT)  Pointer to a wav reader context
 * @return  M4NO_ERROR:                 there is no error
 * @return  M4ERR_ALLOC:                a memory allocation has failed
 * @return  M4ERR_PARAMETER:            at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_create(M4OSA_Context* pContext)
{
    M4READER_PCM_Context*   pReaderContext;

    M4OSA_DEBUG_IF1((pContext == 0),       M4ERR_PARAMETER,
         "M4READER_PCM_create: invalid context pointer");

    pReaderContext = (M4READER_PCM_Context*)M4OSA_32bitAlignedMalloc(sizeof(M4READER_PCM_Context),
         M4READER_WAV, (M4OSA_Char *)"M4READER_PCM_Context");
    if (pReaderContext == M4OSA_NULL)
    {
        return M4ERR_ALLOC;
    }

    pReaderContext->m_coreContext         = M4OSA_NULL;
    pReaderContext->m_pAudioStream        = M4OSA_NULL;
    pReaderContext->m_audioAu.dataAddress = M4OSA_NULL;
    pReaderContext->m_pOsaFileReaderFcts  = M4OSA_NULL;

    *pContext = pReaderContext;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Destroy the instance of the reader
 * @note    the context is un-allocated
 * @param   context:         (IN) context of the network reader
 * @return  M4NO_ERROR:           there is no error
 * @return  M4ERR_PARAMETER:      at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_destroy(M4OSA_Context context)
{
    M4READER_PCM_Context* pC = (M4READER_PCM_Context*)context;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
         "M4READER_PCM_destroy: invalid context pointer");

    free(pC);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Initializes the reader instance
 * @param   context:           (IN)    context of the network reader
 * @param   pFileDescriptor:   (IN)    Pointer to proprietary data identifying the media to open
 * @return  M4NO_ERROR:                there is no error
 * @return  M4ERR_PARAMETER:           at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_open(M4OSA_Context context, M4OSA_Void* pFileDescriptor)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4OSA_ERR               err;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
         "M4READER_PCM_open: invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pFileDescriptor),   M4ERR_PARAMETER,
         "M4READER_PCM_open: invalid pointer pFileDescriptor");

    err = M4PCMR_openRead(&(pC->m_coreContext), (M4OSA_Char*)pFileDescriptor,
         pC->m_pOsaFileReaderFcts);

    return err;
}

/**
 ************************************************************************
 * @brief     close the reader
 * @note
 * @param     context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            the context is NULL
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_close(M4OSA_Context context)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4OSA_ERR               err;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
         "M4READER_PCM_close: invalid context pointer");

    /* Free audio AU and audio stream */
    if (M4OSA_NULL != pC->m_pAudioStream)
    {
        if (M4OSA_NULL != pC->m_audioAu.dataAddress)
        {
            err = M4PCMR_freeAU(pC->m_coreContext, pC->m_pAudioStream->m_streamId,
                 &pC->m_audioAu);
            if (err != M4NO_ERROR)
            {
                M4OSA_TRACE1_0("M4READER_PCM_close: Error when freeing audio access unit");
                return err;
            }
        }
        free(pC->m_pAudioStream);
        pC->m_pAudioStream = M4OSA_NULL;
    }


    if (M4OSA_NULL != pC->m_coreContext)
    {
        /* Close tha PCM file */
       err = M4PCMR_closeRead(pC->m_coreContext);
       pC->m_coreContext = M4OSA_NULL;
    }


    return err;
}

/**
 ************************************************************************
 * @brief   set en option value of the reader
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to set a property value:
 * @param    context:        (IN)    Context of the reader
 * @param    optionId:       (IN)    indicates the option to set
 * @param    pValue:         (IN)    pointer to structure or value (allocated by user)
 *                                    where option is stored
 *
 * @return    M4NO_ERROR             there is no error
 * @return    M4ERR_BAD_CONTEXT      provided context is not a valid one
 * @return    M4ERR_PARAMETER        at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID    when the option ID is not a valid one
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_setOption(M4OSA_Context context, M4OSA_OptionID optionId, void* pValue)
{
    M4READER_PCM_Context* pC = (M4READER_PCM_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC),     M4ERR_PARAMETER,
         "M4READER_PCM_setOption: invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER,
         "M4READER_PCM_setOption: invalid value pointer");

    switch(optionId)
    {
    case M4READER_kOptionID_SetOsaFileReaderFctsPtr :
        {
            pC->m_pOsaFileReaderFcts = (M4OSA_FileReadPointer*)pValue;
        }
        break;
    default :
        {
            err = M4ERR_PARAMETER;
        }
    }

    return err;
}

/**
 ************************************************************************
 * @brief   Retrieves the an option value from the reader, given an option ID.
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to retrieve a property value:
 *
 * @param   context:  (IN) context of the network reader
 * @param   optionId: (IN) option identificator whose option value is to be retrieved.
 * @param   pValue:  (OUT) option value retrieved.
 *
 * @return  M4NO_ERROR:          there is no error
 * @return  M4ERR_PARAMETER:     at least one parameter is not properly set (in DEBUG only)
 * @return  M4ERR_BAD_OPTION_ID: the required option identificator is unknown
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_getOption(M4OSA_Context context, M4OSA_OptionID optionId, void* pValue)
{
    M4READER_PCM_Context*   pContext = (M4READER_PCM_Context*)context;
    M4OSA_ERR               err      = M4NO_ERROR;

    /* no check of context at this level because some option does not need it */
    M4OSA_DEBUG_IF1((pValue == 0), M4ERR_PARAMETER,
         "M4READER_PCM_getOption: invalid pointer on value");

    switch (optionId)
    {
    case M4READER_kOptionID_Duration:
        *((M4OSA_UInt32*)pValue) = pContext->m_pAudioStream->m_duration;
        break;

    case M4READER_kOptionID_Version:
        err = M4PCMR_getVersion((M4_VersionInfo*)pValue);
        break;

    case M4READER_kOptionID_Copyright:
        return M4ERR_NOT_IMPLEMENTED;
        break;

    case M4READER_kOptionID_Bitrate:
        {
            M4OSA_UInt32* pBitrate = (M4OSA_UInt32*)pValue;
            if (M4OSA_NULL != pContext->m_pAudioStream)
            {
                *pBitrate = pContext->m_pAudioStream->m_averageBitRate;
            }
            else
            {
                pBitrate = 0;
                err = M4ERR_PARAMETER;
            }
        }
        break;

    default:
        err = M4ERR_BAD_OPTION_ID;
        M4OSA_TRACE1_0("M4READER_PCM_getOption: unsupported optionId");
        break;
    }

    return err;
}

/**
 ************************************************************************
 * @brief   Get the next stream found in the media
 * @note
 *
 * @param   context:        (IN)  context of the network reader
 * @param   pMediaFamily:   (OUT) pointer to a user allocated M4READER_MediaFamily that will
 *                                be filled
 * @param   pStreamHandler: (OUT) pointer to a stream handler that will be allocated and filled
 *                                with the found stream description
 *
 * @return  M4NO_ERROR:       there is no error.
 * @return  M4ERR_PARAMETER:  at least one parameter is not properly set (in DEBUG only)
 * @return  M4WAR_NO_MORE_STREAM    no more available stream in the media (all streams found)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_getNextStream(M4OSA_Context context, M4READER_MediaFamily *pMediaFamily,
                                     M4_StreamHandler **pStreamHandler)
{
    M4READER_PCM_Context*   pC=(M4READER_PCM_Context*)context;
    M4OSA_ERR               err;
/*    M4_StreamHandler*       pStreamHandler = M4OSA_NULL;*/
    M4SYS_StreamDescription streamDesc;
    M4_AudioStreamHandler*  pAudioStreamHandler;
    M4OSA_Double            fDuration;
    M4SYS_StreamID          streamIdArray[2];
    M4PCMC_DecoderSpecificInfo* pDsi;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
         "M4READER_PCM_getNextStream: invalid context");
    M4OSA_DEBUG_IF1((pMediaFamily == 0),   M4ERR_PARAMETER,
         "M4READER_PCM_getNextStream: invalid pointer to MediaFamily");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_PCM_getNextStream: invalid pointer to StreamHandler");

    err = M4PCMR_getNextStream( pC->m_coreContext, &streamDesc);
    if (err == M4WAR_NO_MORE_STREAM)
    {
        streamIdArray[0] = 0;
        streamIdArray[1] = 0;
        err = M4PCMR_startReading(pC->m_coreContext, streamIdArray); /*to put in open function*/

        return M4WAR_NO_MORE_STREAM;
    }
    else if (M4NO_ERROR != err)
    {
        return err; /*also return M4WAR_NO_MORE_STREAM*/
    }

    switch (streamDesc.streamType)
    {
        case M4SYS_kAudioUnknown:
        case M4SYS_kPCM_16bitsS:
        case M4SYS_kPCM_16bitsU:
        case M4SYS_kPCM_8bitsU:
            *pMediaFamily = M4READER_kMediaFamilyAudio;
            M4OSA_TRACE2_0("M4READER_PCM_getNextStream: found audio stream");
            break;
        default:
            *pMediaFamily = M4READER_kMediaFamilyUnknown;
            M4OSA_TRACE2_0("M4READER_PCM_getNextStream: found UNKNOWN stream");
            return M4NO_ERROR;
    }

    pAudioStreamHandler = (M4_AudioStreamHandler*)M4OSA_32bitAlignedMalloc(sizeof(M4_AudioStreamHandler),
         M4READER_WAV, (M4OSA_Char *)"M4_AudioStreamHandler");
    if (pAudioStreamHandler == M4OSA_NULL)
    {
        return M4ERR_ALLOC;
    }
    pAudioStreamHandler->m_structSize = sizeof(M4_AudioStreamHandler);
    pC->m_pAudioStream = (M4_StreamHandler*)(pAudioStreamHandler);

    pDsi = (M4PCMC_DecoderSpecificInfo*)(streamDesc.decoderSpecificInfo);
    M4OSA_DEBUG_IF1((pDsi == 0), M4ERR_PARAMETER,
         "M4READER_PCM_getNextStream: invalid decoder specific info in stream");

    pAudioStreamHandler->m_samplingFrequency = pDsi->SampleFrequency;
    pAudioStreamHandler->m_byteSampleSize    = (M4OSA_UInt32)(pDsi->BitsPerSample/8);
    /* m_byteFrameLength is badly named: it is not in bytes but in samples number */
    if(pAudioStreamHandler->m_samplingFrequency == 8000)
    {
        /* AMR case */
        pAudioStreamHandler->m_byteFrameLength   =
             (((streamDesc.averageBitrate/8)/50)/pDsi->nbChannels)\
                /pAudioStreamHandler->m_byteSampleSize;/*/50 to get around 20 ms of audio*/
    }
    else
    {
        /* AAC Case */
        pAudioStreamHandler->m_byteFrameLength =
             (M4OSA_UInt32)(((streamDesc.averageBitrate/8)/15.625)/pDsi->nbChannels)\
                /pAudioStreamHandler->m_byteSampleSize;
    }

    pAudioStreamHandler->m_nbChannels        = pDsi->nbChannels;

    M4OSA_TIME_TO_MS( fDuration, streamDesc.duration, streamDesc.timeScale);
    pC->m_pAudioStream->m_duration                = (M4OSA_Int32)fDuration;
    pC->m_pAudioStream->m_pDecoderSpecificInfo    = (M4OSA_UInt8*)(streamDesc.decoderSpecificInfo);
    pC->m_pAudioStream->m_decoderSpecificInfoSize = streamDesc.decoderSpecificInfoSize;
    pC->m_pAudioStream->m_streamId                = streamDesc.streamID;
    pC->m_pAudioStream->m_pUserData               =
        (void*)streamDesc.timeScale; /*trick to change*/
    pC->m_pAudioStream->m_averageBitRate          = streamDesc.averageBitrate;
    pC->m_pAudioStream->m_maxAUSize               =
         pAudioStreamHandler->m_byteFrameLength*pAudioStreamHandler->m_byteSampleSize\
            *pAudioStreamHandler->m_nbChannels;
    pC->m_pAudioStream->m_streamType              = M4DA_StreamTypeAudioPcm;

    *pStreamHandler = pC->m_pAudioStream;
    return err;
}

/**
 ************************************************************************
 * @brief   fill the access unit structure with initialization values
 * @note
 *
 * @param   context:        (IN) context of the network reader
 * @param   pStreamHandler: (IN) pointer to the stream handler to which the access unit will
 *                                 be associated
 * @param   pAccessUnit:    (IN) pointer to the access unit(allocated by the caller) to initialize
 * @return  M4NO_ERROR:       there is no error.
 * @return  M4ERR_PARAMETER:  at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_fillAuStruct(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                                     M4_AccessUnit *pAccessUnit)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4SYS_AccessUnit*       pAu;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
         "M4READER_PCM_fillAuStruct: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_PCM_fillAuStruct: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
         "M4READER_PCM_fillAuStruct: invalid pointer to M4_AccessUnit");

    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_PCM_fillAuStruct: passed StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    pAu->dataAddress = M4OSA_NULL;
    pAu->size        = 0;
    pAu->CTS         = 0;
    pAu->DTS         = 0;
    pAu->attribute   = 0;
    pAu->nbFrag      = 0;

    pAccessUnit->m_size         = 0;
    pAccessUnit->m_CTS          = 0;
    pAccessUnit->m_DTS          = 0;
    pAccessUnit->m_attribute    = 0;
    pAccessUnit->m_dataAddress  = M4OSA_NULL;/*pBuffer;*/
    pAccessUnit->m_maxsize      = pStreamHandler->m_maxAUSize;
    pAccessUnit->m_streamID     = pStreamHandler->m_streamId;
    pAccessUnit->m_structSize   = sizeof(M4_AccessUnit);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   reset the stream, that is: seek it to beginning and make it ready to be read
 * @note
 * @param   context:        (IN) context of the network reader
 * @param   pStreamHandler: (IN) The stream handler of the stream to reset
 * @return  M4NO_ERROR: there is no error.
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_reset(M4OSA_Context context, M4_StreamHandler *pStreamHandler)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4SYS_StreamID          streamIdArray[2];
    M4OSA_ERR               err;
    M4SYS_AccessUnit*       pAu;
    M4OSA_Time                time64 = 0;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER, "M4READER_PCM_reset: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_PCM_reset: invalid pointer to M4_StreamHandler");

    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_PCM_reset: passed StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    if (pAu->dataAddress != M4OSA_NULL)
    {
        err = M4PCMR_freeAU(pC->m_coreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_PCM_reset: error when freeing access unit");
            return err;
        }
        pAu->dataAddress = M4OSA_NULL;
    }

    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;

    pAu->CTS = 0;
    pAu->DTS = 0;

    /* This call is needed only when replay during playback */
    err = M4PCMR_seek(pC->m_coreContext, streamIdArray, time64, M4SYS_kBeginning, &time64);

    return err;
}

/**
 ************************************************************************
 * @brief   Get the next access unit of the specified stream
 * @note
 * @param   context:        (IN)        Context of the reader
 * @param   pStreamHandler  (IN)        The stream handler of the stream to make jump
 * @param   pAccessUnit     (IN/OUT)    Pointer to an access unit to fill with read data
 *                                      (the au structure is allocated by the user, and must be
 *                                        initialized
 *                                      by calling M4READER_fillAuStruct_fct after creation)
 * @return  M4NO_ERROR                  there is no error
 * @return  M4ERR_BAD_CONTEXT           provided context is not a valid one
 * @return  M4ERR_PARAMETER             at least one parameter is not properly set
 * @returns M4ERR_ALLOC                 memory allocation failed
 * @returns M4ERR_BAD_STREAM_ID         at least one of the stream Id. does not exist.
 * @returns M4WAR_NO_DATA_YET           there is no enough data on the stream for new access unit
 * @returns M4WAR_NO_MORE_AU            there are no more access unit in the stream (end of stream)
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_getNextAu(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                                 M4_AccessUnit *pAccessUnit)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4OSA_ERR               err = M4NO_ERROR;
    M4SYS_AccessUnit*       pAu;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
         "M4READER_PCM_getNextAu: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_PCM_getNextAu: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
         "M4READER_PCM_getNextAu: invalid pointer to M4_AccessUnit");

    /* keep trace of the allocated buffers in AU to be able to free them at destroy()
       but be aware that system is risky and would need upgrade if more than
       one video and one audio AU is needed */
    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_PCM_getNextAu: passed StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    if (pAu->dataAddress != M4OSA_NULL)
    {
        err = M4PCMR_freeAU(pC->m_coreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_PCM_getNextAu: error when freeing access unit");
            return err;
        }
    }

    pAu->nbFrag = 0;
    err = M4PCMR_nextAU(pC->m_coreContext, pStreamHandler->m_streamId, pAu);

    if (err == M4NO_ERROR)
    {
        pAccessUnit->m_dataAddress = (M4OSA_MemAddr8)pAu->dataAddress;
        pAccessUnit->m_size = pAu->size;
        pAccessUnit->m_CTS  = (M4OSA_Double)pAu->CTS;
        pAccessUnit->m_DTS  = (M4OSA_Double)pAu->DTS;
        pAccessUnit->m_attribute = pAu->attribute;
    }
    else
    {
        pAccessUnit->m_size=0;
    }

    return err;
}


/**
 ************************************************************************
 * @brief   jump into the stream at the specified time
 * @note
 * @param   context:        (IN)     Context of the reader
 * @param   pStreamHandler  (IN)     the stream handler of the stream to make jump
 * @param   pTime           (IN/OUT) IN:  the time to jump to (in ms)
 *                                   OUT: the time to which the stream really jumped
 *                                        But in this reader, we do not modify the time
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_BAD_CONTEXT       provided context is not a valid one
 * @return  M4ERR_PARAMETER         at least one parameter is not properly set
 * @return  M4ERR_ALLOC             there is no more memory available
 * @return  M4ERR_BAD_STREAM_ID     the streamID does not exist
 ************************************************************************
 */
M4OSA_ERR M4READER_PCM_jump(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
     M4OSA_Int32* pTime)
{
    M4READER_PCM_Context*   pC = (M4READER_PCM_Context*)context;
    M4SYS_StreamID          streamIdArray[2];
    M4OSA_ERR               err;
    M4SYS_AccessUnit*       pAu;
    M4OSA_Time                time64;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER, "M4READER_PCM_jump: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_PCM_jump: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pTime == 0), M4ERR_PARAMETER, "M4READER_PCM_jump: invalid time pointer");

    time64 = (M4OSA_Time)*pTime;

    if (pStreamHandler == pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_PCM_jump: passed StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    if (pAu->dataAddress != M4OSA_NULL)
    {
        err = M4PCMR_freeAU(pC->m_coreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_PCM_jump: Error when freeing access unit");
            return err;
        }
        pAu->dataAddress = M4OSA_NULL;
    }

    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;

    pAu->CTS = time64;
    pAu->DTS = time64;

    err = M4PCMR_seek(pC->m_coreContext, streamIdArray, time64, M4SYS_kBeginning, &time64);

    *pTime = (M4OSA_Int32)time64;

    return err;
}

/**
 *************************************************************************
 * @brief Retrieves the generic interfaces implemented by the reader
 *
 * @param pMediaType          : Pointer on a M4READER_MediaType (allocated by the caller)
 *                              that will be filled with the media type supported by this reader
 * @param pRdrGlobalInterface : Address of a pointer that will be set to the global interface
 *                              implemented by this reader. The interface is a structure allocated
 *                              by the function and must be un-allocated by the caller.
 * @param pRdrDataInterface   : Address of a pointer that will be set to the data interface
 *                              implemented by this reader. The interface is a structure allocated
 *                              by the function and must be un-allocated by the caller.
 *
 * @returns : M4NO_ERROR     if OK
 *            ERR_ALLOC      if an allocation failed
 *            ERR_PARAMETER  at least one parameter is not properly set (in DEBUG only)
 *************************************************************************
 */
M4OSA_ERR   M4READER_PCM_getInterfaces(M4READER_MediaType *pMediaType,
                                       M4READER_GlobalInterface **pRdrGlobalInterface,
                                       M4READER_DataInterface **pRdrDataInterface)
/************************************************************************/
{
    M4OSA_DEBUG_IF1((pMediaType == 0),          M4ERR_PARAMETER,
         "M4READER_PCM_getInterfaces: invalid pointer to MediaType passed");
    M4OSA_DEBUG_IF1((pRdrGlobalInterface == 0), M4ERR_PARAMETER,
         "M4READER_PCM_getInterfaces: invalid pointer to M4READER_GlobalInterface");
    M4OSA_DEBUG_IF1((pRdrDataInterface == 0),   M4ERR_PARAMETER,
         "M4READER_PCM_getInterfaces: invalid pointer to M4READER_DataInterface");

    *pRdrGlobalInterface =
         (M4READER_GlobalInterface*)M4OSA_32bitAlignedMalloc( sizeof(M4READER_GlobalInterface), M4READER_WAV,
             (M4OSA_Char *)"M4READER_PCM GlobalInterface");
    if (M4OSA_NULL == *pRdrGlobalInterface)
    {
        return M4ERR_ALLOC;
    }
    *pRdrDataInterface =
         (M4READER_DataInterface*)M4OSA_32bitAlignedMalloc( sizeof(M4READER_DataInterface), M4READER_WAV,
            (M4OSA_Char *) "M4READER_PCM DataInterface");
    if (M4OSA_NULL == *pRdrDataInterface)
    {
        free(*pRdrGlobalInterface);
        return M4ERR_ALLOC;
    }

    *pMediaType = M4READER_kMediaTypePCM;

    (*pRdrGlobalInterface)->m_pFctCreate           = M4READER_PCM_create;
    (*pRdrGlobalInterface)->m_pFctDestroy          = M4READER_PCM_destroy;
    (*pRdrGlobalInterface)->m_pFctOpen             = M4READER_PCM_open;
    (*pRdrGlobalInterface)->m_pFctClose            = M4READER_PCM_close;
    (*pRdrGlobalInterface)->m_pFctStart            = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctStop             = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctGetOption        = M4READER_PCM_getOption;
    (*pRdrGlobalInterface)->m_pFctSetOption        = M4READER_PCM_setOption;
    (*pRdrGlobalInterface)->m_pFctGetNextStream    = M4READER_PCM_getNextStream;
    (*pRdrGlobalInterface)->m_pFctFillAuStruct     = M4READER_PCM_fillAuStruct;
    (*pRdrGlobalInterface)->m_pFctJump             = M4READER_PCM_jump;
    (*pRdrGlobalInterface)->m_pFctReset            = M4READER_PCM_reset;
    (*pRdrGlobalInterface)->m_pFctGetPrevRapTime   = M4OSA_NULL; /*all AUs are RAP*/

    (*pRdrDataInterface)->m_pFctGetNextAu          = M4READER_PCM_getNextAu;

    (*pRdrDataInterface)->m_readerContext = M4OSA_NULL;

    return M4NO_ERROR;
}


