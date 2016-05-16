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
 * @file   M4READER_Amr.c
 * @brief  Generic encapsulation of the core amr reader
 * @note   This file implements the generic M4READER interface
 *         on top of the AMR reader
 ************************************************************************
*/
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4OSA_Debug.h"
#include "M4OSA_CoreID.h"

#include "M4_Utils.h"

#include "M4AMRR_CoreReader.h"
#include "M4READER_Amr.h"

/**
 ************************************************************************
 * structure    M4READER_AMR_Context
 * @brief       This structure defines the internal context of a amr reader instance
 * @note        The context is allocated and de-allocated by the reader
 ************************************************************************
*/
typedef struct _M4READER_AMR_Context
{
    M4OSA_Context           m_pCoreContext;     /**< core amr reader context */
    M4_AudioStreamHandler*  m_pAudioStream;     /**< pointer on the audio stream
                                                 description returned by the core */
    M4SYS_AccessUnit        m_audioAu;          /**< audio access unit to be filled by the core */
    M4OSA_Time              m_maxDuration;      /**< duration of the audio stream */
    M4OSA_FileReadPointer*    m_pOsaFileReaderFcts;    /**< OSAL file read functions */

} M4READER_AMR_Context;


/**
 ************************************************************************
 * @brief    create an instance of the reader
 * @note     allocates the context
 * @param    pContext:        (OUT)    pointer on a reader context
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_ALLOC                a memory allocation has failed
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_create(M4OSA_Context *pContext)
{
    M4READER_AMR_Context* pReaderContext;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((pContext == 0), M4ERR_PARAMETER,
         "M4READER_AMR_create: invalid context pointer");

    pReaderContext = (M4READER_AMR_Context*)M4OSA_32bitAlignedMalloc(sizeof(M4READER_AMR_Context),
         M4READER_AMR, (M4OSA_Char *)"M4READER_AMR_Context");
    if (pReaderContext == M4OSA_NULL)
    {
        return M4ERR_ALLOC;
    }

    pReaderContext->m_pAudioStream  = M4OSA_NULL;
    pReaderContext->m_audioAu.dataAddress = M4OSA_NULL;
    pReaderContext->m_maxDuration = 0;
    pReaderContext->m_pCoreContext = M4OSA_NULL;
    pReaderContext->m_pOsaFileReaderFcts = M4OSA_NULL;

    *pContext = pReaderContext;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief    destroy the instance of the reader
 * @note     after this call the context is invalid
 *
 * @param    context:        (IN)    Context of the reader
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_destroy(M4OSA_Context context)
{
    M4READER_AMR_Context*   pC=(M4READER_AMR_Context*)context;

    /* Check function parameters*/
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
         "M4READER_AMR_destroy: invalid context pointer");

    /**
     *    Check input parameter */
    if (M4OSA_NULL == pC)
    {
        M4OSA_TRACE1_0("M4READER_AMR_destroy(): M4READER_AMR_destroy: context is M4OSA_NULL,\
             returning M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    free(pC);

    return M4NO_ERROR;
}


/**
 ************************************************************************
 * @brief    open the reader and initializes its created instance
 * @note     this function opens the AMR file
 * @param    context:            (IN)    Context of the reader
 * @param    pFileDescriptor:    (IN)    Pointer to proprietary data identifying the media to open
 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_PARAMETER                the context is NULL
 * @return    M4ERR_BAD_CONTEXT            provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_open(M4OSA_Context context, M4OSA_Void* pFileDescriptor)
{
    M4READER_AMR_Context*    pC = (M4READER_AMR_Context*)context;
    M4OSA_ERR                err;

    /* Check function parameters*/
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC),              M4ERR_PARAMETER,
         "M4READER_AMR_open: invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pFileDescriptor), M4ERR_PARAMETER,
         "M4READER_AMR_open: invalid pointer pFileDescriptor");

    err = M4AMRR_openRead( &pC->m_pCoreContext, pFileDescriptor, pC->m_pOsaFileReaderFcts);

    return err;
}



/**
 ************************************************************************
 * @brief    close the reader
 * @note
 * @param    context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            the context is NULL
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 ************************************************************************
*/
M4OSA_ERR   M4READER_AMR_close(M4OSA_Context context)
{
    M4READER_AMR_Context*    pC = (M4READER_AMR_Context*)context;
    M4OSA_ERR                err;
    M4AMRR_State State;

    /* Check function parameters*/
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
         "M4READER_AMR_close: invalid context pointer");

    /**
     *    Check input parameter */
    if (M4OSA_NULL == pC)
    {
        M4OSA_TRACE1_0("M4READER_AMR_close(): M4READER_AMR_close: context is M4OSA_NULL,\
             returning M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    if (M4OSA_NULL != pC->m_pAudioStream)
    {
        err = M4AMRR_getState(pC->m_pCoreContext, &State,
                ((M4_StreamHandler*)pC->m_pAudioStream)->m_streamId);
        if(M4NO_ERROR != err)
        {
            M4OSA_TRACE1_0("M4READER_AMR_close: error when calling M4AMRR_getState\n");
            return err;
        }

        if (M4AMRR_kReading_nextAU == State)
        {
            err = M4AMRR_freeAU(pC->m_pCoreContext,
                ((M4_StreamHandler*)pC->m_pAudioStream)->m_streamId,  &pC->m_audioAu);
            if (err != M4NO_ERROR)
            {
                M4OSA_TRACE1_0("M4READER_AMR_close: error when freeing access unit\n");
                return err;
            }
        }

        /* Delete the DSI if needed */
        if(M4OSA_NULL != pC->m_pAudioStream->m_basicProperties.m_pDecoderSpecificInfo)
        {
            free(\
                pC->m_pAudioStream->m_basicProperties.m_pDecoderSpecificInfo);

            pC->m_pAudioStream->m_basicProperties.m_decoderSpecificInfoSize = 0;
            pC->m_pAudioStream->m_basicProperties.m_pDecoderSpecificInfo = M4OSA_NULL;
        }

        /* Finally destroy the stream handler */
        free(pC->m_pAudioStream);
        pC->m_pAudioStream = M4OSA_NULL;
    }

    if (M4OSA_NULL != pC->m_pCoreContext)
    {
        err = M4AMRR_closeRead(pC->m_pCoreContext);
        pC->m_pCoreContext = M4OSA_NULL;
    }

    return err;
}

/**
 ************************************************************************
 * @brief    Get the next stream found in the media
 * @note    current version needs to translate M4SYS_Stream to M4_StreamHandler
 *
 * @param    context:        (IN)   Context of the reader
 * @param    pMediaFamily:   (OUT)  pointer to a user allocated M4READER_MediaFamily
 *                                  that will be filled with the media family of the found stream
 * @param    pStreamHandler: (OUT)  pointer to a stream handler that will be
 *                                  allocated and filled with the found stream description
 *
 * @return    M4NO_ERROR            there is no error
 * @return    M4WAR_NO_MORE_STREAM  no more available stream in the media (all streams found)
 * @return    M4ERR_PARAMETER       at least one parameter is not properly set (in DEBUG mode only)
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_getNextStream(M4OSA_Context context, M4READER_MediaFamily *pMediaFamily,
                                     M4_StreamHandler **pStreamHandlerParam)
{
    M4READER_AMR_Context*   pC=(M4READER_AMR_Context*)context;
    M4OSA_ERR               err;
    M4SYS_StreamID          streamIdArray[2];
    M4SYS_StreamDescription streamDesc;
    M4_AudioStreamHandler*  pAudioStreamHandler;
    M4_StreamHandler*       pStreamHandler;

    M4OSA_DEBUG_IF1((pC == 0),                  M4ERR_PARAMETER,
                "M4READER_AMR_getNextStream: invalid context");
    M4OSA_DEBUG_IF1((pMediaFamily == 0),        M4ERR_PARAMETER,
                "M4READER_AMR_getNextStream: invalid pointer to MediaFamily");
    M4OSA_DEBUG_IF1((pStreamHandlerParam == 0), M4ERR_PARAMETER,
                "M4READER_AMR_getNextStream: invalid pointer to StreamHandler");

    err = M4AMRR_getNextStream( pC->m_pCoreContext, &streamDesc);
    if (err == M4WAR_NO_MORE_STREAM)
    {
        streamIdArray[0] = 0;
        streamIdArray[1] = 0;
        err = M4AMRR_startReading(pC->m_pCoreContext, streamIdArray);
        if ((M4OSA_UInt32)M4ERR_ALLOC == err)
        {
            M4OSA_TRACE2_0("M4READER_AMR_getNextStream: M4AMRR_startReading returns M4ERR_ALLOC!");
            return err;
        }
        return M4WAR_NO_MORE_STREAM;
    }
    else if (err != M4NO_ERROR)
    {
        return err;
    }

    *pMediaFamily = M4READER_kMediaFamilyAudio;

    pAudioStreamHandler = (M4_AudioStreamHandler*)M4OSA_32bitAlignedMalloc(sizeof(M4_AudioStreamHandler),
                        M4READER_AMR, (M4OSA_Char *)"M4_AudioStreamHandler");
    if (pAudioStreamHandler == M4OSA_NULL)
    {
        return M4ERR_ALLOC;
    }
    pStreamHandler =(M4_StreamHandler*)(pAudioStreamHandler);
    *pStreamHandlerParam = pStreamHandler;
    pC->m_pAudioStream = pAudioStreamHandler;

    pAudioStreamHandler->m_structSize = sizeof(M4_AudioStreamHandler);

    /*
     * Audio stream handler fields are initialised with 0 value.
     * They will be properly set by the AMR decoder
     */
    pAudioStreamHandler->m_samplingFrequency = 0;
    pAudioStreamHandler->m_byteFrameLength   = 0;
    pAudioStreamHandler->m_byteSampleSize    = 0;
    pAudioStreamHandler->m_nbChannels        = 0;

    pStreamHandler->m_pDecoderSpecificInfo    = (M4OSA_UInt8*)(streamDesc.decoderSpecificInfo);
    pStreamHandler->m_decoderSpecificInfoSize = streamDesc.decoderSpecificInfoSize;
    pStreamHandler->m_streamId                = streamDesc.streamID;
    pStreamHandler->m_duration                = streamDesc.duration;
    pStreamHandler->m_pUserData               = (void*)streamDesc.timeScale; /*trick to change*/

    if (streamDesc.duration > pC->m_maxDuration)
    {
        pC->m_maxDuration = streamDesc.duration;
    }
    pStreamHandler->m_averageBitRate          = streamDesc.averageBitrate;

    M4AMRR_getmaxAUsize(pC->m_pCoreContext, &pStreamHandler->m_maxAUSize);

    switch (streamDesc.streamType)
    {
    case M4SYS_kAMR:
        pStreamHandler->m_streamType = M4DA_StreamTypeAudioAmrNarrowBand;
        break;
    case M4SYS_kAMR_WB:
        pStreamHandler->m_streamType = M4DA_StreamTypeAudioAmrWideBand;
        break;
    default:
        break;
    }

    return err;
}

/**
 ************************************************************************
 * @brief    fill the access unit structure with initialization values
 * @note
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler: (IN)     pointer to the stream handler to
 *                                    which the access unit will be associated
 * @param    pAccessUnit:    (IN/OUT) pointer to the access unit (allocated by the caller)
 *                                      to initialize
 *
 * @return    M4NO_ERROR              there is no error
 * @return    M4ERR_PARAMETER         at least one parameter is not properly set
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_fillAuStruct(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                                     M4_AccessUnit *pAccessUnit)
{
    M4READER_AMR_Context*   pC = (M4READER_AMR_Context*)context;
    M4SYS_AccessUnit*       pAu;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
         "M4READER_AMR_fillAuStruct: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_AMR_fillAuStruct: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
         "M4READER_AMR_fillAuStruct: invalid pointer to M4_AccessUnit");

    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_AMR_fillAuStruct: passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    pAu->dataAddress = M4OSA_NULL;
    pAu->size        = 0;
    /* JC: bug fix 1197 (set CTS to -20 in order the first AU CTS is 0) */
    pAu->CTS         = -20;
    pAu->DTS         = -20;
    pAu->attribute   = 0;
    pAu->nbFrag      = 0;

    pAccessUnit->m_size         = 0;
    /* JC: bug fix 1197 (set CTS to -20 in order the first AU CTS is 0) */
    pAccessUnit->m_CTS          = -20;
    pAccessUnit->m_DTS          = -20;
    pAccessUnit->m_attribute    = 0;
    pAccessUnit->m_dataAddress  = M4OSA_NULL;/*pBuffer;*/
    pAccessUnit->m_maxsize      = pStreamHandler->m_maxAUSize;
    pAccessUnit->m_streamID     = pStreamHandler->m_streamId;
    pAccessUnit->m_structSize   = sizeof(M4_AccessUnit);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief    get an option value from the reader
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to retrieve a property value:
 *          - the duration of the longest stream of the media
 *          - the version number of the reader (not implemented yet)
 *
 * @param    context:        (IN)    Context of the reader
 * @param    optionId:        (IN)    indicates the option to get
 * @param    pValue:            (OUT)    pointer to structure or value (allocated by user)
 *                                       where option is stored
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_getOption(M4OSA_Context context, M4OSA_OptionID optionId,
                                 M4OSA_DataOption pValue)

{
    M4READER_AMR_Context* pC = (M4READER_AMR_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC),     M4ERR_PARAMETER, "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER, "invalid value pointer");

    switch(optionId)
    {
    case M4READER_kOptionID_Duration :
        {
            *(M4OSA_Time*)pValue = pC->m_maxDuration;
        }
        break;

    case M4READER_kOptionID_Bitrate:
        {
            M4OSA_UInt32* pBitrate = (M4OSA_UInt32*)pValue;
            if (M4OSA_NULL != pC->m_pAudioStream)
            {
                *pBitrate = pC->m_pAudioStream->m_basicProperties.m_averageBitRate;
            }
            else
            {
                pBitrate = 0;
                err = M4ERR_PARAMETER;
            }

        }
        break;
    case M4READER_kOptionID_Version:
        {
            err = M4AMRR_getVersion((M4_VersionInfo*)pValue);
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
 * @brief   set en option value of the readder
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to set a property value:
 *          - the OSAL file read functions
 *
 * @param   context:    (IN)        Context of the decoder
 * @param   optionId:   (IN)        Identifier indicating the option to set
 * @param   pValue:     (IN)        Pointer to structure or value (allocated by user)
 *                                  where option is stored
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_BAD_OPTION_ID     The option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_PARAMETER         The option parameter is invalid
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_setOption(M4OSA_Context context, M4OSA_OptionID optionId,
                                 M4OSA_DataOption pValue)
{
    M4READER_AMR_Context* pC = (M4READER_AMR_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC),     M4ERR_PARAMETER, "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER, "invalid value pointer");

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
 * @brief    reset the stream, that is seek it to beginning and make it ready to be read
 * @note    this function is to be deprecated in next versions
 *
 * @param    context:        (IN)    Context of the reader
 * @param    pStreamHandler    (IN)    The stream handler of the stream to reset
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_ALLOC                there is no more memory available
 * @return    M4ERR_BAD_STREAM_ID        the streamID does not exist
 * @return    M4ERR_STATE    this function cannot be called now
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4WAR_INVALID_TIME        beginning of the stream can not be reached
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_reset(M4OSA_Context context, M4_StreamHandler *pStreamHandler)
{
    M4READER_AMR_Context*   pC = (M4READER_AMR_Context*)context;
    M4SYS_StreamID          streamIdArray[2];
    M4OSA_ERR               err;
    M4SYS_AccessUnit*       pAu;
    M4OSA_Time              time64 = 0;
    M4AMRR_State            State;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER, "M4READER_AMR_reset: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_AMR_reset: invalid pointer to M4_StreamHandler");

    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_AMR_reset: passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    err = M4AMRR_getState(pC->m_pCoreContext, &State, pStreamHandler->m_streamId);
    if (M4AMRR_kReading_nextAU == State)
    {
        err = M4AMRR_freeAU(pC->m_pCoreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_AMR_reset: error when freeing access unit\n");
            return err;
        }
        pAu->dataAddress = M4OSA_NULL;
    }

    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;

    err = M4NO_ERROR;

    /* for reset during playback */
    /* (set CTS to -20 in order the first AU CTS is 0) */
    pAu->CTS = -20;
    pAu->DTS = -20;

    err = M4AMRR_seek(pC->m_pCoreContext, streamIdArray, time64, M4SYS_kBeginning, &time64);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_0("M4READER_AMR_reset: error when calling M4AMRR_seek()\n");
        return err;
    }

    return err;
}

/**
 ************************************************************************
 * @brief    jump into the stream at the specified time
 * @note
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler    (IN)     the stream description of the stream to make jump
 * @param    pTime            (IN/OUT) IN:  the time to jump to (in ms)
 *                                     OUT: the time to which the stream really jumped
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_ALLOC                there is no more memory available
 * @return    M4WAR_INVALID_TIME        the time can not be reached
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_jump(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                             M4OSA_Int32* pTime)
{
    M4READER_AMR_Context*   pC = (M4READER_AMR_Context*)context;
    M4SYS_StreamID          streamIdArray[2];
    M4OSA_ERR               err;
    M4SYS_AccessUnit*       pAu;
    M4OSA_Time              time64 = (M4OSA_Time)*pTime;
    M4AMRR_State            State;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER, "M4READER_AMR_reset: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_AMR_reset: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pTime == 0), M4ERR_PARAMETER, "M4READER_3GP_jump: invalid time pointer");

    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_AMR_jump: passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    err = M4AMRR_getState(pC->m_pCoreContext, &State, pStreamHandler->m_streamId);
    if (M4AMRR_kReading_nextAU == State)
    {
        err = M4AMRR_freeAU(pC->m_pCoreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_AMR_jump: error when freeing access unit\n");
            return err;
        }
        pAu->dataAddress = M4OSA_NULL;
    }

    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;

    pAu->CTS = time64;
    pAu->DTS = time64;
    err = M4AMRR_seek(pC->m_pCoreContext, streamIdArray, time64, M4SYS_kNoRAPprevious, &time64);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_0("M4READER_AMR_jump: error when calling M4AMRR_seek()\n");
        return err;
    }

    *pTime = (M4OSA_Int32)time64;

    return err;
}

/**
 ************************************************************************
 * @brief   Gets an access unit (AU) from the stream handler source.
 * @note    An AU is the smallest possible amount of data to be decoded by a decoder (audio/video).
 *          In the current version, we need to translate M4OSA_AccessUnit to M4_AccessUnit
 *
 * @param    context:        (IN)        Context of the reader
 * @param    pStreamHandler  (IN)        The stream handler of the stream to make jump
 * @param    pAccessUnit     (IN/OUT)    Pointer to an access unit to fill with read data (the au
                                         structure is allocated by the user, and must be
                                         initialized by calling M4READER_fillAuStruct_fct after
                                         creation)
 * @return    M4NO_ERROR              there is no error
 * @return    M4ERR_BAD_CONTEXT       provided context is not a valid one
 * @return    M4ERR_PARAMETER         at least one parameter is not properly set
 * @return    M4ERR_ALLOC             memory allocation failed
 * @return    M4ERR_BAD_STREAM_ID     at least one of the stream Id. does not exist.
 * @return    M4WAR_NO_MORE_AU        there are no more access unit in the stream (end of stream)
 ************************************************************************
*/
M4OSA_ERR M4READER_AMR_getNextAu(M4OSA_Context context, M4_StreamHandler *pStreamHandler,
                                M4_AccessUnit *pAccessUnit)
{
    M4READER_AMR_Context*   pC = (M4READER_AMR_Context*)context;
    M4OSA_ERR               err = M4NO_ERROR;
    M4SYS_AccessUnit*       pAu;
    M4_MediaTime            timeScale;
    M4AMRR_State            State;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
         "M4READER_AMR_getNextAu: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
         "M4READER_AMR_getNextAu: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
         "M4READER_AMR_getNextAu: invalid pointer to M4_AccessUnit");

    /* keep trace of the allocated buffers in AU to be able to free them at destroy()
       but be aware that system is risky and would need upgrade if more than
       one video and one audio AU is needed */
    if (pStreamHandler == (M4_StreamHandler*)pC->m_pAudioStream)
    {
        pAu = &pC->m_audioAu;
    }
    else
    {
        M4OSA_TRACE1_0("M4READER_AMR_getNextAu: passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    err = M4AMRR_getState(pC->m_pCoreContext, &State, pStreamHandler->m_streamId);
    if (M4AMRR_kReading_nextAU == State)
    {
        err = M4AMRR_freeAU(pC->m_pCoreContext, pStreamHandler->m_streamId, pAu);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_0("M4READER_AVI_getNextAu: error when freeing access unit\n");
            return err;
        }
        pAu->dataAddress = M4OSA_NULL;
    }

    pAu->nbFrag = 0;
    err = M4AMRR_nextAU(pC->m_pCoreContext, pStreamHandler->m_streamId, pAu);

    if (err == M4NO_ERROR)
    {
        timeScale = (M4OSA_Float)(M4OSA_Int32)(pStreamHandler->m_pUserData)/1000;
        pAccessUnit->m_dataAddress = (M4OSA_MemAddr8)pAu->dataAddress;
        pAccessUnit->m_size = pAu->size;
        pAccessUnit->m_CTS  = (M4_MediaTime)pAu->CTS/*/timeScale*/;
        pAccessUnit->m_DTS  = (M4_MediaTime)pAu->DTS/*/timeScale*/;
        pAccessUnit->m_attribute = pAu->attribute;
    }
    else
    {
        pAccessUnit->m_size=0;
    }

    return err;
}

/**
*************************************************************************
* @brief Retrieves the generic interfaces implemented by the reader
*
* @param pMediaType          : Pointer on a M4READER_MediaType (allocated by the caller)
*                              that will be filled with the media type supported by this reader
* @param pRdrGlobalInterface : Address of a pointer that will be set to the global interface implemented
*                              by this reader. The interface is a structure allocated by the function and must
*                              be un-allocated by the caller.
* @param pRdrDataInterface   : Address of a pointer that will be set to the data interface implemented
*                              by this reader. The interface is a structure allocated by the function and must
*                              be un-allocated by the caller.
*
* @returns : M4NO_ERROR     if OK
*            ERR_ALLOC      if an allocation failed
*            ERR_PARAMETER  at least one parameter is not properly set (in DEBUG only)
*************************************************************************
*/
M4OSA_ERR   M4READER_AMR_getInterfaces(M4READER_MediaType *pMediaType,
                                         M4READER_GlobalInterface **pRdrGlobalInterface,
                                         M4READER_DataInterface **pRdrDataInterface)
{
    M4OSA_DEBUG_IF1((pMediaType == 0),          M4ERR_PARAMETER,
         "M4READER_AMR_getInterfaces: invalid pointer to MediaType");
    M4OSA_DEBUG_IF1((pRdrGlobalInterface == 0), M4ERR_PARAMETER,
         "M4READER_AMR_getInterfaces: invalid pointer to M4READER_GlobalInterface");
    M4OSA_DEBUG_IF1((pRdrDataInterface == 0),   M4ERR_PARAMETER,
         "M4READER_AMR_getInterfaces: invalid pointer to M4READER_DataInterface");

    *pRdrGlobalInterface =
         (M4READER_GlobalInterface*)M4OSA_32bitAlignedMalloc( sizeof(M4READER_GlobalInterface),
             M4READER_AMR, (M4OSA_Char *)"M4READER_GlobalInterface" );
    if (M4OSA_NULL == *pRdrGlobalInterface)
    {
        *pRdrDataInterface = M4OSA_NULL;
        return M4ERR_ALLOC;
    }
    *pRdrDataInterface = (M4READER_DataInterface*)M4OSA_32bitAlignedMalloc( sizeof(M4READER_DataInterface),
         M4READER_AMR, (M4OSA_Char *)"M4READER_DataInterface");
    if (M4OSA_NULL == *pRdrDataInterface)
    {
        free(*pRdrGlobalInterface);
        *pRdrGlobalInterface = M4OSA_NULL;
        return M4ERR_ALLOC;
    }

    *pMediaType = M4READER_kMediaTypeAMR;

    (*pRdrGlobalInterface)->m_pFctCreate           = M4READER_AMR_create;
    (*pRdrGlobalInterface)->m_pFctDestroy          = M4READER_AMR_destroy;
    (*pRdrGlobalInterface)->m_pFctOpen             = M4READER_AMR_open;
    (*pRdrGlobalInterface)->m_pFctClose            = M4READER_AMR_close;
    (*pRdrGlobalInterface)->m_pFctGetOption        = M4READER_AMR_getOption;
    (*pRdrGlobalInterface)->m_pFctSetOption        = M4READER_AMR_setOption;
    (*pRdrGlobalInterface)->m_pFctGetNextStream    = M4READER_AMR_getNextStream;
    (*pRdrGlobalInterface)->m_pFctFillAuStruct     = M4READER_AMR_fillAuStruct;
    (*pRdrGlobalInterface)->m_pFctStart            = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctStop             = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctJump             = M4READER_AMR_jump;
    (*pRdrGlobalInterface)->m_pFctReset            = M4READER_AMR_reset;
    (*pRdrGlobalInterface)->m_pFctGetPrevRapTime   = M4OSA_NULL; /*all AUs are RAP*/

    (*pRdrDataInterface)->m_pFctGetNextAu          = M4READER_AMR_getNextAu;

    (*pRdrDataInterface)->m_readerContext = M4OSA_NULL;

    return M4NO_ERROR;
}

