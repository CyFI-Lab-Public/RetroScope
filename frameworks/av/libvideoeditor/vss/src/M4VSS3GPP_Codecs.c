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
 * @file   M4VSS3GPP_Codecs.c
 * @brief  VSS implementation
 * @note   This file contains all functions related to audio/video
 *            codec manipulations.
 *************************************************************************
 */

#include "NXPSW_CompilerSwitches.h"

#include "M4OSA_Debug.h"             /**< Include for OSAL debug services */
#include "M4VSS3GPP_ErrorCodes.h"
#include "M4VSS3GPP_InternalTypes.h" /**< Internal types of the VSS */

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_clearInterfaceTables()
 * @brief    Clear encoders, decoders, reader and writers interfaces tables
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    The context is null
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_clearInterfaceTables( M4VSS3GPP_MediaAndCodecCtxt *pC )
{
    M4OSA_UInt8 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");

    /* Initialisation that will allow to check if registering twice */
    pC->pWriterGlobalFcts = M4OSA_NULL;
    pC->pWriterDataFcts = M4OSA_NULL;
    pC->pVideoEncoderGlobalFcts = M4OSA_NULL;
    pC->pAudioEncoderGlobalFcts = M4OSA_NULL;
    pC->pCurrentAudioEncoderUserData = M4OSA_NULL;
    pC->pCurrentAudioDecoderUserData = M4OSA_NULL;

    pC->pCurrentVideoEncoderExternalAPI = M4OSA_NULL;
    pC->pCurrentVideoEncoderUserData = M4OSA_NULL;

    for ( i = 0; i < M4WRITER_kType_NB; i++ )
    {
        pC->WriterInterface[i].pGlobalFcts = M4OSA_NULL;
        pC->WriterInterface[i].pDataFcts = M4OSA_NULL;
    }

    for ( i = 0; i < M4ENCODER_kVideo_NB; i++ )
    {
        pC->pVideoEncoderInterface[i] = M4OSA_NULL;
        pC->pVideoEncoderExternalAPITable[i] = M4OSA_NULL;
        pC->pVideoEncoderUserDataTable[i] = M4OSA_NULL;
    }

    for ( i = 0; i < M4ENCODER_kAudio_NB; i++ )
    {
        pC->pAudioEncoderInterface[i] = M4OSA_NULL;
        pC->pAudioEncoderFlag[i] = M4OSA_FALSE;
        pC->pAudioEncoderUserDataTable[i] = M4OSA_NULL;
    }

    /* Initialisation that will allow to check if registering twice */
    pC->m_pReader = M4OSA_NULL;
    pC->m_pReaderDataIt = M4OSA_NULL;
    pC->m_uiNbRegisteredReaders = 0;

    for ( i = 0; i < M4READER_kMediaType_NB; i++ )
    {
        pC->m_pReaderGlobalItTable[i] = M4OSA_NULL;
        pC->m_pReaderDataItTable[i] = M4OSA_NULL;
    }

    pC->m_pVideoDecoder = M4OSA_NULL;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

    pC->m_pCurrentVideoDecoderUserData = M4OSA_NULL;
#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

    pC->m_uiNbRegisteredVideoDec = 0;

    for ( i = 0; i < M4DECODER_kVideoType_NB; i++ )
    {
        pC->m_pVideoDecoderItTable[i] = M4OSA_NULL;
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

        pC->m_pVideoDecoderUserDataTable[i] = M4OSA_NULL;

#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

    }

    pC->m_pAudioDecoder = M4OSA_NULL;

    for ( i = 0; i < M4AD_kType_NB; i++ )
    {
        pC->m_pAudioDecoderItTable[i] = M4OSA_NULL;
        pC->m_pAudioDecoderFlagTable[i] = M4OSA_FALSE;
        pC->pAudioDecoderUserDataTable[i] = M4OSA_NULL;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerWriter()
 * @brief    This function will register a specific file format writer.
 * @note    According to the Mediatype, this function will store in the internal
 *        context the writer context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext,pWtrGlobalInterface or pWtrDataInterface is M4OSA_NULL
 *                          (debug only), or invalid MediaType
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerWriter( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                   M4WRITER_OutputFileType MediaType,
                                   M4WRITER_GlobalInterface *pWtrGlobalInterface,
                                   M4WRITER_DataInterface *pWtrDataInterface )
{
    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL), M4ERR_PARAMETER,
        "VSS: context is M4OSA_NULL in M4VSS3GPP_registerWriter");
    M4OSA_DEBUG_IF2((pWtrGlobalInterface == M4OSA_NULL), M4ERR_PARAMETER,
        "pWtrGlobalInterface is M4OSA_NULL in M4VSS3GPP_registerWriter");
    M4OSA_DEBUG_IF2((pWtrDataInterface == M4OSA_NULL), M4ERR_PARAMETER,
        "pWtrDataInterface is M4OSA_NULL in M4VSS3GPP_registerWriter");

    M4OSA_TRACE3_3(
        "VSS: M4VSS3GPP_registerWriter called with pContext=0x%x, pWtrGlobalInterface=0x%x,\
        pWtrDataInterface=0x%x",
        pC, pWtrGlobalInterface, pWtrDataInterface);

    if( ( MediaType == M4WRITER_kUnknown) || (MediaType >= M4WRITER_kType_NB) )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid media type");
        return M4ERR_PARAMETER;
    }

    if( pC->WriterInterface[MediaType].pGlobalFcts != M4OSA_NULL )
    {
        /* a writer corresponding to this media type has already been registered !*/
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "This media type has already been registered");
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
 * M4OSA_ERR   M4VSS3GPP_registerVideoEncoder()
 * @brief    This function will register a specific video encoder.
 * @note    According to the Mediatype, this function will store in the internal
 *        context the encoder context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext or pEncGlobalInterface is M4OSA_NULL (debug only),
 *                          or invalid MediaType
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerVideoEncoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                         M4ENCODER_Format MediaType,
                                         M4ENCODER_GlobalInterface *pEncGlobalInterface )
{
    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL), M4ERR_PARAMETER,
        "VSS: context is M4OSA_NULL in M4VSS3GPP_registerVideoEncoder");
    M4OSA_DEBUG_IF2((pEncGlobalInterface == M4OSA_NULL), M4ERR_PARAMETER,
        "pEncGlobalInterface is M4OSA_NULL in M4VSS3GPP_registerVideoEncoder");

    M4OSA_TRACE3_3(
        "VSS: M4VSS3GPP_registerEncoder called with pContext=0x%x, pEncGlobalInterface=0x%x,\
        MediaType=0x%x",
        pC, pEncGlobalInterface, MediaType);

    if( MediaType >= M4ENCODER_kVideo_NB )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "Invalid video encoder type");
        return M4ERR_PARAMETER;
    }

    if( pC->pVideoEncoderInterface[MediaType] != M4OSA_NULL )
    {
        /* can be legitimate, in cases where we have one version that can use external encoders
        but which still has the built-in one to be able to work without an external encoder; in
        this case the new encoder simply replaces the old one (i.e. we unregister it first). */
#ifdef M4VSS_SUPPORT_OMX_CODECS

        if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
        {

#endif

            free(pC->pVideoEncoderInterface[MediaType]);
#ifdef M4VSS_SUPPORT_OMX_CODECS

        }

#endif

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
 * M4OSA_ERR   M4VSS3GPP_registerAudioEncoder()
 * @brief    This function will register a specific audio encoder.
 * @note    According to the Mediatype, this function will store in the internal
 *        context the encoder context.
 * @param    pContext:                (IN) Execution context.
 * @param    mediaType:                (IN) The media type.
 * @param    pEncGlobalInterface:    (OUT) the encoder interface functions.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext or pEncGlobalInterface is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerAudioEncoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                         M4ENCODER_AudioFormat MediaType,
                                         M4ENCODER_AudioGlobalInterface *pEncGlobalInterface )
{
    /**
    *    Check input parameters */
    M4OSA_DEBUG_IF2((pC == M4OSA_NULL), M4ERR_PARAMETER,
        "VSS: context is M4OSA_NULL in M4VSS3GPP_registerAudioEncoder");
    M4OSA_DEBUG_IF2((pEncGlobalInterface == M4OSA_NULL), M4ERR_PARAMETER,
        "pEncGlobalInterface is M4OSA_NULL in M4VSS3GPP_registerAudioEncoder");

    M4OSA_TRACE3_3(
        "VSS: M4VSS3GPP_registerAudioEncoder called pContext=0x%x, pEncGlobalInterface=0x%x,\
        MediaType=0x%x",
        pC, pEncGlobalInterface, MediaType);

    if( MediaType >= M4ENCODER_kAudio_NB )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "Invalid audio encoder type");
        return M4ERR_PARAMETER;
    }

    if( pC->pAudioEncoderInterface[MediaType] != M4OSA_NULL )
    {
        free(pC->pAudioEncoderInterface[MediaType]);
        pC->pAudioEncoderInterface[MediaType] = M4OSA_NULL;
    }
    /*
    * Save encoder interface in context */
    pC->pAudioEncoderInterface[MediaType] = pEncGlobalInterface;
    pC->pAudioEncoderFlag[MediaType] = M4OSA_FALSE; /* internal encoder */
    pC->pAudioEncoderUserDataTable[MediaType] = M4OSA_NULL;

    M4OSA_TRACE3_2(
        "M4VSS3GPP_registerAudioEncoder: pC->pAudioEncoderInterface[0x%x] = 0x%x",
        MediaType, pC->pAudioEncoderInterface[MediaType]);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerReader()
 * @brief    Register reader.
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerReader( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                   M4READER_MediaType mediaType,
                                   M4READER_GlobalInterface *pRdrGlobalInterface,
                                   M4READER_DataInterface *pRdrDataInterface )
{
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pRdrGlobalInterface), M4ERR_PARAMETER,
        "M4VSS3GPP_registerReader: invalid pointer on global interface");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pRdrDataInterface), M4ERR_PARAMETER,
        "M4VSS3GPP_registerReader: invalid pointer on data interface");

    if( mediaType == M4READER_kMediaTypeUnknown
        || mediaType >= M4READER_kMediaType_NB )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER, "Invalid media type");
        return M4ERR_PARAMETER;
    }

    if( pC->m_pReaderGlobalItTable[mediaType] != M4OSA_NULL )
    {
        /* a reader corresponding to this media type has already been registered !*/
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "This media type has already been registered");
        return M4ERR_PARAMETER;
    }

    pC->m_pReaderGlobalItTable[mediaType] = pRdrGlobalInterface;
    pC->m_pReaderDataItTable[mediaType] = pRdrDataInterface;

    pC->m_uiNbRegisteredReaders++;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerVideoDecoder()
 * @brief    Register video decoder
 * @param    pContext                (IN/OUT) VSS context.
 * @param    decoderType            (IN) Decoder type
 * @param    pDecoderInterface    (IN) Decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only),
 *                                or the decoder type is invalid
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerVideoDecoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                         M4DECODER_VideoType decoderType,
                                         M4DECODER_VideoInterface *pDecoderInterface )
{
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pDecoderInterface), M4ERR_PARAMETER,
        "M4VSS3GPP_registerVideoDecoder: invalid pointer on decoder interface");

    if( decoderType >= M4DECODER_kVideoType_NB )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "Invalid video decoder type");
        return M4ERR_PARAMETER;
    }

    if( pC->m_pVideoDecoderItTable[decoderType] != M4OSA_NULL )
    {
#ifndef M4VSS_ENABLE_EXTERNAL_DECODERS
        /* a decoder corresponding to this media type has already been registered !*/

        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "Decoder has already been registered");
        return M4ERR_PARAMETER;

#else /* external decoders are possible */
        /* can be legitimate, in cases where we have one version that can use external decoders
        but which still has the built-in one to be able to work without an external decoder; in
        this case the new decoder simply replaces the old one (i.e. we unregister it first). */
#ifdef M4VSS_SUPPORT_OMX_CODECS

        if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
        {

#endif

            free(pC->m_pVideoDecoderItTable[decoderType]);
#ifdef M4VSS_SUPPORT_OMX_CODECS

        }

#endif

        pC->m_pVideoDecoderItTable[decoderType] = M4OSA_NULL;
        /* oh, and don't forget the user data, too. */
        if( pC->m_pVideoDecoderUserDataTable[decoderType] != M4OSA_NULL )
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
 * M4OSA_ERR   M4VSS3GPP_registerAudioDecoder()
 * @brief    Register audio decoder
 * @note    This function is used internaly by the VSS to register NXP audio decoders,
 * @param    context                (IN/OUT) VSS context.
 * @param    decoderType            (IN) Audio decoder type
 * @param    pDecoderInterface    (IN) Audio decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:   A parameter is null, or the decoder type is invalid(in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_registerAudioDecoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                         M4AD_Type decoderType, M4AD_Interface *pDecoderInterface)
{
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pDecoderInterface), M4ERR_PARAMETER,
        "M4VSS3GPP_registerAudioDecoder: invalid pointer on decoder interface");

    if( decoderType >= M4AD_kType_NB )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4ERR_PARAMETER,
            "Invalid audio decoder type");
        return M4ERR_PARAMETER;
    }
    if(M4OSA_NULL != pC->m_pAudioDecoderItTable[decoderType])
    {
        free(pC->m_pAudioDecoderItTable[decoderType]);
        pC->m_pAudioDecoderItTable[decoderType] = M4OSA_NULL;

        if(M4OSA_NULL != pC->m_pAudioDecoderItTable[decoderType])
        {
            free(pC->m_pAudioDecoderItTable[decoderType]);
            pC->m_pAudioDecoderItTable[decoderType] = M4OSA_NULL;
        }
    }



    pC->m_pAudioDecoderItTable[decoderType] = pDecoderInterface;
    pC->m_pAudioDecoderFlagTable[decoderType] =
        M4OSA_FALSE; /* internal decoder */
    pC->pAudioDecoderUserDataTable[decoderType] = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllWriters()
 * @brief    Unregister writer
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_unRegisterAllWriters( M4VSS3GPP_MediaAndCodecCtxt *pC )
{
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");

    for ( i = 0; i < M4WRITER_kType_NB; i++ )
    {
        if( pC->WriterInterface[i].pGlobalFcts != M4OSA_NULL )
        {
            free(pC->WriterInterface[i].pGlobalFcts);
            pC->WriterInterface[i].pGlobalFcts = M4OSA_NULL;
        }

        if( pC->WriterInterface[i].pDataFcts != M4OSA_NULL )
        {
            free(pC->WriterInterface[i].pDataFcts);
            pC->WriterInterface[i].pDataFcts = M4OSA_NULL;
        }
    }

    pC->pWriterGlobalFcts = M4OSA_NULL;
    pC->pWriterDataFcts = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllEncoders()
 * @brief    Unregister the encoders
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_unRegisterAllEncoders( M4VSS3GPP_MediaAndCodecCtxt *pC )
{
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_1("M4VSS3GPP_unRegisterAllEncoders: pC=0x%x", pC);

    for ( i = 0; i < M4ENCODER_kVideo_NB; i++ )
    {
        if( pC->pVideoEncoderInterface[i] != M4OSA_NULL )
        {
#ifdef M4VSS_SUPPORT_OMX_CODECS

            if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
            {

#endif

                free(pC->pVideoEncoderInterface[i]);
#ifdef M4VSS_SUPPORT_OMX_CODECS

            }

#endif

            pC->pVideoEncoderInterface[i] = M4OSA_NULL;
        }
    }

    for ( i = 0; i < M4ENCODER_kAudio_NB; i++ )
    {
        if( pC->pAudioEncoderInterface[i] != M4OSA_NULL )
        {
#ifdef M4VSS_SUPPORT_OMX_CODECS

            if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
            {

#endif
                /*Don't free external audio encoders interfaces*/

                if( M4OSA_FALSE == pC->pAudioEncoderFlag[i] )
                {
                    free(pC->pAudioEncoderInterface[i]);
                }
#ifdef M4VSS_SUPPORT_OMX_CODECS

            }

#endif

            pC->pAudioEncoderInterface[i] = M4OSA_NULL;
        }
    }

    pC->pVideoEncoderGlobalFcts = M4OSA_NULL;
    pC->pAudioEncoderGlobalFcts = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllReaders()
 * @brief    Unregister reader
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_unRegisterAllReaders( M4VSS3GPP_MediaAndCodecCtxt *pC )
{
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");

    for ( i = 0; i < M4READER_kMediaType_NB; i++ )
    {
        if( pC->m_pReaderGlobalItTable[i] != M4OSA_NULL )
        {
            free(pC->m_pReaderGlobalItTable[i]);
            pC->m_pReaderGlobalItTable[i] = M4OSA_NULL;
        }

        if( pC->m_pReaderDataItTable[i] != M4OSA_NULL )
        {
            free(pC->m_pReaderDataItTable[i]);
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
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllDecoders()
 * @brief    Unregister the decoders
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_unRegisterAllDecoders( M4VSS3GPP_MediaAndCodecCtxt *pC )
{
    M4OSA_Int32 i;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_1("M4VSS3GPP_unRegisterAllDecoders: pC=0x%x", pC);

    for ( i = 0; i < M4DECODER_kVideoType_NB; i++ )
    {
        if( pC->m_pVideoDecoderItTable[i] != M4OSA_NULL )
        {
#ifdef M4VSS_SUPPORT_OMX_CODECS

            if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
            {

#endif

                free(pC->m_pVideoDecoderItTable[i]);
#ifdef M4VSS_SUPPORT_OMX_CODECS

            }

#endif

            pC->m_pVideoDecoderItTable[i] = M4OSA_NULL;

        }
    }

    for ( i = 0; i < M4AD_kType_NB; i++ )
    {
        if( pC->m_pAudioDecoderItTable[i] != M4OSA_NULL )
        {
#ifdef M4VSS_SUPPORT_OMX_CODECS

            if( M4OSA_TRUE == pC->bAllowFreeingOMXCodecInterface )
            {

#endif
                /*Don't free external audio decoders interfaces*/

                if( M4OSA_FALSE == pC->m_pAudioDecoderFlagTable[i] )
                {
                    free(pC->m_pAudioDecoderItTable[i]);
                }
#ifdef M4VSS_SUPPORT_OMX_CODECS

            }

#endif

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
 * M4OSA_ERR   M4VSS3GPP_setCurrentWriter()
 * @brief    Set current writer
 * @param    pContext            (IN/OUT) VSS context.
 * @param    mediaType            (IN) Media type.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_VSS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentWriter( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                     M4VIDEOEDITING_FileType mediaType )
{
    M4WRITER_OutputFileType writerType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");

    switch( mediaType )
    {
        case M4VIDEOEDITING_kFileType_3GPP:
            writerType = M4WRITER_k3GPP;
            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4VSS3GPP_ERR_INVALID_FILE_TYPE,
                "Writer type not supported");
            return M4VSS3GPP_ERR_INVALID_FILE_TYPE;
    }

    pC->pWriterGlobalFcts = pC->WriterInterface[writerType].pGlobalFcts;
    pC->pWriterDataFcts = pC->WriterInterface[writerType].pDataFcts;

    if( pC->pWriterGlobalFcts == M4OSA_NULL
        || pC->pWriterDataFcts == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4VSS3GPP_ERR_INVALID_FILE_TYPE,
            "Writer type not supported");
        M4OSA_TRACE1_0("Writer type not supported");
        return M4VSS3GPP_ERR_INVALID_FILE_TYPE;
    }

    pC->pWriterDataFcts->pWriterContext = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_setCurrentVideoEncoder()
 * @brief    Set a video encoder
 * @param    pContext            (IN/OUT) VSS context.
 * @param    MediaType           (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_VSS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentVideoEncoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4SYS_StreamType mediaType )
{
    M4ENCODER_Format encoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_2("M4VSS3GPP_setCurrentVideoEncoder: pC=0x%x, mediaType=0x%x",
        pC, mediaType);

    switch( mediaType )
    {
        case M4SYS_kH263:
            encoderType = M4ENCODER_kH263;
            break;

        case M4SYS_kMPEG_4:
            encoderType = M4ENCODER_kMPEG4;
            break;

        case M4SYS_kH264:
            encoderType = M4ENCODER_kH264;
            break;

        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE,
                M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT,
                "Video encoder type not supported");
            return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT;
    }

    pC->pVideoEncoderGlobalFcts = pC->pVideoEncoderInterface[encoderType];
    pC->pCurrentVideoEncoderExternalAPI =
        pC->pVideoEncoderExternalAPITable[encoderType];
    pC->pCurrentVideoEncoderUserData =
        pC->pVideoEncoderUserDataTable[encoderType];

    if( pC->pVideoEncoderGlobalFcts == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE,
            M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT,
            "Video encoder type not supported");
        M4OSA_TRACE1_0("Video encoder type not supported");
        return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_VIDEO_FORMAT;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_setCurrentAudioEncoder()
 * @brief    Set an audio encoder
 * @param    context            (IN/OUT) VSS context.
 * @param    MediaType        (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentAudioEncoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4SYS_StreamType mediaType )
{
    M4ENCODER_AudioFormat encoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_2("M4VSS3GPP_setCurrentAudioEncoder: pC=0x%x, mediaType=0x%x",
        pC, mediaType);

    switch( mediaType )
    {
        case M4SYS_kAMR:
            M4OSA_TRACE3_0(
                "M4VSS3GPP_setCurrentAudioEncoder: encoder type AMR");
            encoderType = M4ENCODER_kAMRNB;
            break;

        case M4SYS_kAAC:
            M4OSA_TRACE3_0(
                "M4VSS3GPP_setCurrentAudioEncoder: encoder type AAC");
            encoderType = M4ENCODER_kAAC;
            break;

       default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE,
                M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT,
                "Audio encoder type not supported");
            return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT;
    }

    pC->pAudioEncoderGlobalFcts = pC->pAudioEncoderInterface[encoderType];
    pC->pCurrentAudioEncoderUserData =
        pC->pAudioEncoderUserDataTable[encoderType];

    M4OSA_TRACE3_3(
        "M4VSS3GPP_setCurrentAudioEncoder: pC->pAudioEncoderInterface[0x%x]=0x%x,\
        pC->pAudioEncoderGlobalFcts = 0x%x",
        encoderType, pC->pAudioEncoderInterface[encoderType],
        pC->pAudioEncoderGlobalFcts);

    if( pC->pAudioEncoderGlobalFcts == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE,
            M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT,
            "Audio encoder type not supported");
        M4OSA_TRACE1_0("Audio encoder type not supported");
        return M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_setCurrentReader()
 * @brief    Set current reader
 * @param    pContext            (IN/OUT) VSS context.
 * @param    mediaType            (IN) Media type.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_VSS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentReader( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                     M4VIDEOEDITING_FileType mediaType )
{
    M4READER_MediaType readerType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");

    switch( mediaType )
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

        case M4VIDEOEDITING_kFileType_PCM:
            readerType = M4READER_kMediaTypePCM;
            break;

        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE, M4VSS3GPP_ERR_INVALID_FILE_TYPE,
                "Reader type not supported");
            return M4VSS3GPP_ERR_INVALID_FILE_TYPE;
    }

    pC->m_pReader = pC->m_pReaderGlobalItTable[readerType];
    pC->m_pReaderDataIt = pC->m_pReaderDataItTable[readerType];

    if( pC->m_pReader == M4OSA_NULL || pC->m_pReaderDataIt == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE, M4VSS3GPP_ERR_INVALID_FILE_TYPE,
            "Reader type not supported");
        M4OSA_TRACE1_0("Reader type not supported");
        return M4VSS3GPP_ERR_INVALID_FILE_TYPE;
    }
    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_setCurrentVideoDecoder()
 * @brief    Set a video decoder
 * @param    pContext            (IN/OUT) VSS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_VSS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentVideoDecoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4_StreamType mediaType )
{
    M4DECODER_VideoType decoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_2("M4VSS3GPP_setCurrentVideoDecoder: pC=0x%x, mediaType=0x%x",
        pC, mediaType);

    switch( mediaType )
    {
        case M4DA_StreamTypeVideoMpeg4:
        case M4DA_StreamTypeVideoH263:
            decoderType = M4DECODER_kVideoTypeMPEG4;
            break;

        case M4DA_StreamTypeVideoMpeg4Avc:
            decoderType = M4DECODER_kVideoTypeAVC;
            break;
        case M4DA_StreamTypeVideoARGB8888:
            decoderType = M4DECODER_kVideoTypeYUV420P;
            break;
        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE,
                M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT,
                "Video decoder type not supported");
            return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT;
    }

    pC->m_pVideoDecoder = pC->m_pVideoDecoderItTable[decoderType];
#ifdef M4VSS_ENABLE_EXTERNAL_DECODERS

    pC->m_pCurrentVideoDecoderUserData =
        pC->m_pVideoDecoderUserDataTable[decoderType];

#endif /* M4VSS_ENABLE_EXTERNAL_DECODERS */

    if( pC->m_pVideoDecoder == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE,
            M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT,
            "Video decoder type not supported");
        M4OSA_TRACE1_0("Video decoder type not supported");
        return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT;
    }

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_setCurrentAudioDecoder()
 * @brief    Set an audio decoder
 * @param    context            (IN/OUT) VSS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR M4VSS3GPP_setCurrentAudioDecoder( M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4_StreamType mediaType )
{
    M4AD_Type decoderType;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_TRACE3_2("M4VSS3GPP_setCurrentAudioDecoder: pC=0x%x, mediaType=0x%x",
        pC, mediaType);

    switch( mediaType )
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

        case M4DA_StreamTypeAudioPcm:
            decoderType = M4AD_kTypePCM;
            break;

        default:
            M4OSA_DEBUG_IF1(M4OSA_TRUE,
                M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT,
                "Audio decoder type not supported");
            return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT;
    }

    pC->m_pAudioDecoder = pC->m_pAudioDecoderItTable[decoderType];
    pC->pCurrentAudioDecoderUserData =
        pC->pAudioDecoderUserDataTable[decoderType];

    if( pC->m_pAudioDecoder == M4OSA_NULL )
    {
        M4OSA_DEBUG_IF1(M4OSA_TRUE,
            M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT,
            "Audio decoder type not supported");
        M4OSA_TRACE1_0("Audio decoder type not supported");
        return M4VSS3GPP_ERR_UNSUPPORTED_INPUT_AUDIO_FORMAT;
    }

    return M4NO_ERROR;
}
