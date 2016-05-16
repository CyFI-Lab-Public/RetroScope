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
 * @file   M4MCS_Codecs.c
 * @brief  MCS implementation
 * @note   This file contains all functions related to audio/video
 *         codec manipulations.
 ************************************************************************
 */

/**
 ********************************************************************
 * Includes
 ********************************************************************
 */
#include "NXPSW_CompilerSwitches.h"
#include "M4OSA_Debug.h"            /* Include for OSAL debug services */
#include "M4MCS_InternalTypes.h"    /* Internal types of the MCS */


#ifdef M4MCS_SUPPORT_VIDEC_3GP
#include "M4_MPEG4VI_VideoHandler.h"  /*needed for renderer error codes*/
#endif


/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_clearInterfaceTables()
 * @brief    Clear encoders, decoders, reader and writers interfaces tables
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    The context is null
 ************************************************************************
 */
M4OSA_ERR   M4MCS_clearInterfaceTables(M4MCS_Context pContext)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4OSA_UInt8 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    /* Initialisation that will allow to check if registering twice */
    pC->pWriterGlobalFcts = M4OSA_NULL;
    pC->pWriterDataFcts = M4OSA_NULL;
    pC->pVideoEncoderGlobalFcts = M4OSA_NULL;
    pC->pAudioEncoderGlobalFcts = M4OSA_NULL;

    pC->pCurrentVideoEncoderExternalAPI = M4OSA_NULL;
    pC->pCurrentVideoEncoderUserData = M4OSA_NULL;

    for (i = 0; i < M4WRITER_kType_NB; i++ )
    {
        pC->WriterInterface[i].pGlobalFcts = M4OSA_NULL;
        pC->WriterInterface[i].pDataFcts = M4OSA_NULL;
    }

    for (i = 0; i < M4ENCODER_kVideo_NB; i++ )
    {
        pC->pVideoEncoderInterface[i] = M4OSA_NULL;
        pC->pVideoEncoderExternalAPITable[i] = M4OSA_NULL;
        pC->pVideoEncoderUserDataTable[i] = M4OSA_NULL;
    }

    for (i = 0; i < M4ENCODER_kAudio_NB; i++ )
    {
        pC->pAudioEncoderInterface[i] = M4OSA_NULL;
        pC->pAudioEncoderFlag[i] = M4OSA_FALSE;
        pC->pAudioEncoderUserDataTable[i] = M4OSA_NULL;
    }

    /* Initialisation that will allow to check if registering twice */
    pC->m_pReader = M4OSA_NULL;
    pC->m_pReaderDataIt   = M4OSA_NULL;
    pC->m_uiNbRegisteredReaders  = 0;

    for (i = 0; i < M4READER_kMediaType_NB; i++ )
    {
        pC->m_pReaderGlobalItTable[i] = M4OSA_NULL;
        pC->m_pReaderDataItTable[i]   = M4OSA_NULL;
    }

    pC->m_pVideoDecoder = M4OSA_NULL;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
    pC->m_pCurrentVideoDecoderUserData = M4OSA_NULL;
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */
    pC->m_uiNbRegisteredVideoDec = 0;
    for (i = 0; i < M4DECODER_kVideoType_NB; i++ )
    {
        pC->m_pVideoDecoderItTable[i] = M4OSA_NULL;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
        pC->m_pVideoDecoderUserDataTable[i] = M4OSA_NULL;
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */
    }

    pC->m_pAudioDecoder = M4OSA_NULL;
    for (i = 0; i < M4AD_kType_NB; i++ )
    {
        pC->m_pAudioDecoderItTable[i] = M4OSA_NULL;
        pC->m_pAudioDecoderFlagTable[i] = M4OSA_FALSE;
        pC->m_pAudioDecoderUserDataTable[i] = M4OSA_NULL;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR   M4MCS_registerWriter()
 * @brief    This function will register a specific file format writer.
 * @note    According to the Mediatype, this function will store in the internal context
 *          the writer context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return      M4ERR_PARAMETER     pContext,pWtrGlobalInterface or
 *                                  pWtrDataInterface is M4OSA_NULL
 *                                  (debug only), or invalid MediaType
 ******************************************************************************
 */
M4OSA_ERR   M4MCS_registerWriter(M4MCS_Context pContext, M4WRITER_OutputFileType MediaType,
                                 M4WRITER_GlobalInterface* pWtrGlobalInterface,
                                 M4WRITER_DataInterface* pWtrDataInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    /**
     *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL),M4ERR_PARAMETER,
         "MCS: context is M4OSA_NULL in M4MCS_registerWriter");
    M4OSA_DEBUG_IF2((pWtrGlobalInterface == M4OSA_NULL),M4ERR_PARAMETER,
         "pWtrGlobalInterface is M4OSA_NULL in M4MCS_registerWriter");
    M4OSA_DEBUG_IF2((pWtrDataInterface == M4OSA_NULL),M4ERR_PARAMETER,
         "pWtrDataInterface is M4OSA_NULL in M4MCS_registerWriter");

    M4OSA_TRACE3_3("MCS: M4MCS_registerWriter called with pContext=0x%x,\
     pWtrGlobalInterface=0x%x, pWtrDataInterface=0x%x", pC,pWtrGlobalInterface,
     pWtrDataInterface);

    if((MediaType == M4WRITER_kUnknown) || (MediaType >= M4WRITER_kType_NB))
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid media type");
        return M4ERR_PARAMETER;
    }

    if (pC->WriterInterface[MediaType].pGlobalFcts != M4OSA_NULL)
    {
      /* a writer corresponding to this media type has already been registered !*/
      M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "This media type has already been registered");
      return M4ERR_PARAMETER;
    }

    /*
     * Save writer interface in context */
    pC->WriterInterface[MediaType].pGlobalFcts = pWtrGlobalInterface;
    pC->WriterInterface[MediaType].pDataFcts = pWtrDataInterface;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR   M4MCS_registerEncoder()
 * @brief    This function will register a specific video encoder.
 * @note    According to the Mediatype, this function will store in the internal context
 *           the encoder context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext or pEncGlobalInterface is M4OSA_NULL (debug only),
 *                             or invalid MediaType
 ******************************************************************************
 */
M4OSA_ERR   M4MCS_registerVideoEncoder (
                    M4MCS_Context pContext,
                    M4ENCODER_Format MediaType,
                    M4ENCODER_GlobalInterface *pEncGlobalInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    /**
     *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL),M4ERR_PARAMETER,
         "MCS: context is M4OSA_NULL in M4MCS_registerVideoEncoder");
    M4OSA_DEBUG_IF2((pEncGlobalInterface == M4OSA_NULL),M4ERR_PARAMETER,
         "pEncGlobalInterface is M4OSA_NULL in M4MCS_registerVideoEncoder");

    M4OSA_TRACE3_2("MCS: M4MCS_registerVideoEncoder called with pContext=0x%x,\
         pEncGlobalInterface=0x%x", pC, pEncGlobalInterface);

    if (MediaType >= M4ENCODER_kVideo_NB)
    {
      M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid video encoder type");
      return M4ERR_PARAMETER;
    }

    if (pC->pVideoEncoderInterface[MediaType] != M4OSA_NULL)
    {
        /* can be legitimate, in cases where we have one version that can use external encoders
        but which still has the built-in one to be able to work without an external encoder; in
        this case the new encoder simply replaces the old one (i.e. we unregister it first). */
        free(pC->pVideoEncoderInterface[MediaType]);
        pC->pVideoEncoderInterface[MediaType] = M4OSA_NULL;
    }

    /*
     * Save encoder interface in context */
    pC->pVideoEncoderInterface[MediaType] = pEncGlobalInterface;
    /* The actual userData and external API will be set by the registration function in the case
    of an external encoder (add it as a parameter to this function in the long run?) */
    pC->pVideoEncoderUserDataTable[MediaType] = M4OSA_NULL;
    pC->pVideoEncoderExternalAPITable[MediaType] = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR   M4MCS_registerAudioEncoder()
 * @brief    This function will register a specific audio encoder.
 * @note    According to the Mediatype, this function will store in the internal context
 *           the encoder context.
 * @param    pContext:                (IN) Execution context.
 * @param    mediaType:                (IN) The media type.
 * @param    pEncGlobalInterface:    (OUT) the encoder interface functions.
 * @return    M4NO_ERROR: there is no error
 * @return   M4ERR_PARAMETER:   pContext or pEncGlobalInterface is
 *                              M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR   M4MCS_registerAudioEncoder(
                    M4MCS_Context pContext,
                    M4ENCODER_AudioFormat MediaType,
                    M4ENCODER_AudioGlobalInterface *pEncGlobalInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    /**
     *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL),M4ERR_PARAMETER,
         "MCS: context is M4OSA_NULL in M4MCS_registerAudioEncoder");
    M4OSA_DEBUG_IF2((pEncGlobalInterface == M4OSA_NULL),M4ERR_PARAMETER,
         "pEncGlobalInterface is M4OSA_NULL in M4MCS_registerAudioEncoder");

    M4OSA_TRACE3_2("MCS: M4MCS_registerAudioEncoder called with pContext=0x%x,\
         pEncGlobalInterface=0x%x", pC, pEncGlobalInterface);

    if (MediaType >= M4ENCODER_kAudio_NB)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid audio encoder type");
        return M4ERR_PARAMETER;
    }

    if(M4OSA_NULL != pC->pAudioEncoderInterface[MediaType])
    {
        free(pC->pAudioEncoderInterface[MediaType]);
        pC->pAudioEncoderInterface[MediaType] = M4OSA_NULL;

        if(M4OSA_NULL != pC->pAudioEncoderUserDataTable[MediaType])
        {
            free(pC->pAudioEncoderUserDataTable[MediaType]);
            pC->pAudioEncoderUserDataTable[MediaType] = M4OSA_NULL;
        }
    }

    /*
     * Save encoder interface in context */
    pC->pAudioEncoderInterface[MediaType] = pEncGlobalInterface;
    pC->pAudioEncoderFlag[MediaType] = M4OSA_FALSE; /* internal encoder */

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_registerReader()
 * @brief    Register reader.
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_registerReader(
                        M4MCS_Context pContext,
                        M4READER_MediaType mediaType,
                        M4READER_GlobalInterface *pRdrGlobalInterface,
                        M4READER_DataInterface *pRdrDataInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pRdrGlobalInterface),
         M4ERR_PARAMETER, "M4MCS_registerReader: invalid pointer on global interface");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pRdrDataInterface),
         M4ERR_PARAMETER, "M4MCS_registerReader: invalid pointer on data interface");

    if (mediaType == M4READER_kMediaTypeUnknown || mediaType >= M4READER_kMediaType_NB)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid media type");
        return M4ERR_PARAMETER;
    }

    if (pC->m_pReaderGlobalItTable[mediaType] != M4OSA_NULL)
    {
        /* a reader corresponding to this media type has already been registered !*/
      M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "This media type has already been registered");
      return M4ERR_PARAMETER;
    }

    pC->m_pReaderGlobalItTable[mediaType] = pRdrGlobalInterface;
    pC->m_pReaderDataItTable[mediaType]   = pRdrDataInterface;

    pC->m_uiNbRegisteredReaders++;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_registerVideoDecoder()
 * @brief    Register video decoder
 * @param    pContext                (IN/OUT) MCS context.
 * @param    decoderType            (IN) Decoder type
 * @param    pDecoderInterface    (IN) Decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only), or the decoder
 *                              type is invalid
 ************************************************************************
 */
M4OSA_ERR   M4MCS_registerVideoDecoder(
                            M4MCS_Context pContext,
                            M4DECODER_VideoType decoderType,
                            M4DECODER_VideoInterface *pDecoderInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pDecoderInterface), M4ERR_PARAMETER,
         "M4MCS_registerVideoDecoder: invalid pointer on decoder interface");

    if (decoderType >= M4DECODER_kVideoType_NB)
    {
      M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid video decoder type");
      return M4ERR_PARAMETER;
    }

    if (pC->m_pVideoDecoderItTable[decoderType] != M4OSA_NULL)
    {
#ifndef M4VSS_ENABLE_EXTERNAL_DECODERS
        /* a decoder corresponding to this media type has already been registered !*/
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Decoder has already been registered");
        return M4ERR_PARAMETER;
#else /* external decoders are possible */
        /* can be legitimate, in cases where we have one version that can use external decoders
        but which still has the built-in one to be able to work without an external decoder; in
        this case the new decoder simply replaces the old one (i.e. we unregister it first). */
        free(pC->m_pVideoDecoderItTable[decoderType]);
        pC->m_pVideoDecoderItTable[decoderType] = M4OSA_NULL;
        /* oh, and don't forget the user data, too. */
        if (pC->m_pVideoDecoderUserDataTable[decoderType] != M4OSA_NULL)
        {
            free(pC->m_pVideoDecoderUserDataTable[decoderType]);
            pC->m_pVideoDecoderUserDataTable[decoderType] = M4OSA_NULL;
        }
#endif /* are external decoders possible? */
    }

    pC->m_pVideoDecoderItTable[decoderType] = pDecoderInterface;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
    pC->m_pVideoDecoderUserDataTable[decoderType] = M4OSA_NULL;
    /* The actual userData will be set by the registration function in the case
    of an external decoder (add it as a parameter to this function in the long run?) */
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */
    pC->m_uiNbRegisteredVideoDec++;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_registerAudioDecoder()
 * @brief    Register audio decoder
 * @note        This function is used internaly by the MCS to
 *              register audio decoders,
 * @param    context                (IN/OUT) MCS context.
 * @param    decoderType            (IN) Audio decoder type
 * @param    pDecoderInterface    (IN) Audio decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return   M4ERR_PARAMETER:    A parameter is null, or the decoder type is invalid(in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_registerAudioDecoder(
                                    M4MCS_Context pContext,
                                    M4AD_Type decoderType,
                                    M4AD_Interface *pDecoderInterface)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pDecoderInterface), M4ERR_PARAMETER,
         "M4MCS_registerAudioDecoder: invalid pointer on decoder interface");

    if (decoderType >= M4AD_kType_NB)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid audio decoder type");
        return M4ERR_PARAMETER;
    }

    if(M4OSA_NULL != pC->m_pAudioDecoderItTable[decoderType])
    {
        free(pC->m_pAudioDecoderItTable[decoderType]);
        pC->m_pAudioDecoderItTable[decoderType] = M4OSA_NULL;

        if(M4OSA_NULL != pC->m_pAudioDecoderUserDataTable[decoderType])
        {
            free(pC->m_pAudioDecoderUserDataTable[decoderType]);
            pC->m_pAudioDecoderUserDataTable[decoderType] = M4OSA_NULL;
        }
    }
    pC->m_pAudioDecoderItTable[decoderType] = pDecoderInterface;
    pC->m_pAudioDecoderFlagTable[decoderType] = M4OSA_FALSE; /* internal decoder */

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_unRegisterAllWriters()
 * @brief    Unregister writer
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllWriters(M4MCS_Context pContext)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    for (i = 0; i < M4WRITER_kType_NB; i++)
    {
        if (pC->WriterInterface[i].pGlobalFcts != M4OSA_NULL)
        {
            free(pC->WriterInterface[i].pGlobalFcts );
            pC->WriterInterface[i].pGlobalFcts = M4OSA_NULL;
        }
        if (pC->WriterInterface[i].pDataFcts != M4OSA_NULL)
        {
            free(pC->WriterInterface[i].pDataFcts );
            pC->WriterInterface[i].pDataFcts = M4OSA_NULL;
        }
    }

    pC->pWriterGlobalFcts = M4OSA_NULL;
    pC->pWriterDataFcts = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_unRegisterAllEncoders()
 * @brief    Unregister the encoders
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllEncoders(M4MCS_Context pContext)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    for (i = 0; i < M4ENCODER_kVideo_NB; i++)
    {
        if (pC->pVideoEncoderInterface[i] != M4OSA_NULL)
        {
            free(pC->pVideoEncoderInterface[i] );
            pC->pVideoEncoderInterface[i] = M4OSA_NULL;
        }
    }

    for (i = 0; i < M4ENCODER_kAudio_NB; i++)
    {
        if (pC->pAudioEncoderInterface[i] != M4OSA_NULL)
        {
            /*Don't free external audio encoders interfaces*/
            if (M4OSA_FALSE == pC->pAudioEncoderFlag[i])
            {
                free(pC->pAudioEncoderInterface[i] );
            }
            pC->pAudioEncoderInterface[i] = M4OSA_NULL;
        }
    }

    pC->pVideoEncoderGlobalFcts = M4OSA_NULL;
    pC->pAudioEncoderGlobalFcts = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_unRegisterAllReaders()
 * @brief    Unregister reader
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllReaders(M4MCS_Context pContext)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    for (i = 0; i < M4READER_kMediaType_NB; i++)
    {
        if (pC->m_pReaderGlobalItTable[i] != M4OSA_NULL)
        {
            free(pC->m_pReaderGlobalItTable[i] );
            pC->m_pReaderGlobalItTable[i] = M4OSA_NULL;
        }
        if (pC->m_pReaderDataItTable[i] != M4OSA_NULL)
        {
            free(pC->m_pReaderDataItTable[i] );
            pC->m_pReaderDataItTable[i] = M4OSA_NULL;
        }
    }

    pC->m_uiNbRegisteredReaders = 0;
    pC->m_pReader = M4OSA_NULL;
    pC->m_pReaderDataIt = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_unRegisterAllDecoders()
 * @brief    Unregister the decoders
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllDecoders(M4MCS_Context pContext)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    for (i = 0; i < M4DECODER_kVideoType_NB; i++)
    {
        if (pC->m_pVideoDecoderItTable[i] != M4OSA_NULL)
        {
            free(pC->m_pVideoDecoderItTable[i] );
            pC->m_pVideoDecoderItTable[i] = M4OSA_NULL;
        }
    }

    for (i = 0; i < M4AD_kType_NB; i++)
    {
        if (pC->m_pAudioDecoderItTable[i] != M4OSA_NULL)
        {
            /*Don't free external audio decoders interfaces*/
            if (M4OSA_FALSE == pC->m_pAudioDecoderFlagTable[i])
            {
                free(pC->m_pAudioDecoderItTable[i] );
            }
            pC->m_pAudioDecoderItTable[i] = M4OSA_NULL;
        }
    }

    pC->m_uiNbRegisteredVideoDec = 0;
    pC->m_pVideoDecoder = M4OSA_NULL;

    pC->m_pAudioDecoder = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentWriter()
 * @brief    Set current writer
 * @param    pContext            (IN/OUT) MCS context.
 * @param    mediaType            (IN) Media type.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentWriter( M4MCS_Context pContext,
                                    M4VIDEOEDITING_FileType mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4WRITER_OutputFileType writerType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4VIDEOEDITING_kFileType_3GPP:
        case M4VIDEOEDITING_kFileType_MP4:
        case M4VIDEOEDITING_kFileType_M4V:
            writerType = M4WRITER_k3GPP;
            break;
        case M4VIDEOEDITING_kFileType_AMR:
            writerType = M4WRITER_kAMR;
            break;
        case M4VIDEOEDITING_kFileType_MP3:
            writerType = M4WRITER_kMP3;
            break;
        case M4VIDEOEDITING_kFileType_PCM:
            pC->b_isRawWriter = M4OSA_TRUE;
            writerType = M4WRITER_kPCM;
            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Writer type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->pWriterGlobalFcts = pC->WriterInterface[writerType].pGlobalFcts;
    pC->pWriterDataFcts = pC->WriterInterface[writerType].pDataFcts;

    if (pC->pWriterGlobalFcts == M4OSA_NULL || pC->pWriterDataFcts == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Writer type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->pWriterDataFcts->pWriterContext = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentVideoEncoder()
 * @brief    Set a video encoder
 * @param    pContext            (IN/OUT) MCS context.
 * @param    MediaType           (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentVideoEncoder(
                                M4MCS_Context pContext,
                                M4VIDEOEDITING_VideoFormat mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4ENCODER_Format encoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4VIDEOEDITING_kH263:
            encoderType = M4ENCODER_kH263;
            break;
        case M4VIDEOEDITING_kMPEG4:
            encoderType = M4ENCODER_kMPEG4;
            break;
        case M4VIDEOEDITING_kH264:
#ifdef M4VSS_SUPPORT_ENCODER_AVC
            encoderType = M4ENCODER_kH264;
        break;
#endif
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Video encoder type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->pVideoEncoderGlobalFcts = pC->pVideoEncoderInterface[encoderType];
    pC->pCurrentVideoEncoderExternalAPI = pC->pVideoEncoderExternalAPITable[encoderType];
    pC->pCurrentVideoEncoderUserData = pC->pVideoEncoderUserDataTable[encoderType];

    if (pC->pVideoEncoderGlobalFcts == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Video encoder type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentAudioEncoder()
 * @brief    Set an audio encoder
 * @param    context            (IN/OUT) MCS context.
 * @param    MediaType        (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentAudioEncoder(
                                M4MCS_Context pContext,
                                M4VIDEOEDITING_AudioFormat mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4ENCODER_AudioFormat encoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4VIDEOEDITING_kAMR_NB:
            encoderType = M4ENCODER_kAMRNB;
            break;
        case M4VIDEOEDITING_kAAC:
            encoderType = M4ENCODER_kAAC;
            break;
        case M4VIDEOEDITING_kMP3:
            encoderType = M4ENCODER_kMP3;
            break;
//EVRC
//        case M4VIDEOEDITING_kEVRC:
//            encoderType = M4ENCODER_kEVRC;
//            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Audio encoder type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->pAudioEncoderGlobalFcts = pC->pAudioEncoderInterface[encoderType];
    pC->pCurrentAudioEncoderUserData = pC->pAudioEncoderUserDataTable[encoderType];

    if (pC->pAudioEncoderGlobalFcts == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Audio encoder type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentReader()
 * @brief    Set current reader
 * @param    pContext            (IN/OUT) MCS context.
 * @param    mediaType            (IN) Media type.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentReader( M4MCS_Context pContext,
                                    M4VIDEOEDITING_FileType mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4READER_MediaType readerType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4VIDEOEDITING_kFileType_3GPP:
        case M4VIDEOEDITING_kFileType_MP4:
        case M4VIDEOEDITING_kFileType_M4V:
            readerType = M4READER_kMediaType3GPP;
            break;
        case M4VIDEOEDITING_kFileType_AMR:
            readerType = M4READER_kMediaTypeAMR;
            break;
        case M4VIDEOEDITING_kFileType_MP3:
            readerType = M4READER_kMediaTypeMP3;
            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Reader type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->m_pReader       = pC->m_pReaderGlobalItTable[readerType];
    pC->m_pReaderDataIt = pC->m_pReaderDataItTable[readerType];

    if (pC->m_pReader == M4OSA_NULL || pC->m_pReaderDataIt == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Reader type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }
    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentVideoDecoder()
 * @brief    Set a video decoder
 * @param    pContext            (IN/OUT) MCS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentVideoDecoder(   M4MCS_Context pContext,
                                            M4_StreamType mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4DECODER_VideoType decoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4DA_StreamTypeVideoMpeg4:
        case M4DA_StreamTypeVideoH263:
            decoderType = M4DECODER_kVideoTypeMPEG4;
            break;
        case M4DA_StreamTypeVideoMpeg4Avc:
            decoderType = M4DECODER_kVideoTypeAVC;
            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Video decoder type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->m_pVideoDecoder = pC->m_pVideoDecoderItTable[decoderType];
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS
    pC->m_pCurrentVideoDecoderUserData =
            pC->m_pVideoDecoderUserDataTable[decoderType];
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

    if (pC->m_pVideoDecoder == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Video decoder type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4MCS_setCurrentAudioDecoder()
 * @brief    Set an audio decoder
 * @param    context            (IN/OUT) MCS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentAudioDecoder(   M4MCS_Context pContext,
                                            M4_StreamType mediaType)
{
    M4MCS_InternalContext* pC = (M4MCS_InternalContext*)pContext;
    M4AD_Type decoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER, "invalid context pointer");

    switch (mediaType)
    {
        case M4DA_StreamTypeAudioAmrNarrowBand:
            decoderType = M4AD_kTypeAMRNB;
            break;
        case M4DA_StreamTypeAudioAac:
        case M4DA_StreamTypeAudioAacADTS:
        case M4DA_StreamTypeAudioAacADIF:
            decoderType = M4AD_kTypeAAC;
            break;
        case M4DA_StreamTypeAudioMp3:
            decoderType = M4AD_kTypeMP3;
            break;
//EVRC
//        case M4DA_StreamTypeAudioEvrc:
//            decoderType = M4AD_kTypeEVRC;
//            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
                 "Audio decoder type not supported");
            return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    pC->m_pAudioDecoder = pC->m_pAudioDecoderItTable[decoderType];
    pC->m_pCurrentAudioDecoderUserData =
                    pC->m_pAudioDecoderUserDataTable[decoderType];

    if (pC->m_pAudioDecoder == M4OSA_NULL)
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED,
             "Audio decoder type not supported");
        return M4MCS_WAR_MEDIATYPE_NOT_SUPPORTED;
    }

    return M4NO_ERROR;
}

