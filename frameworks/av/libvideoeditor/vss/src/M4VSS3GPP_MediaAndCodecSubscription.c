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
 * @file    M4VSS3GPP_MediaAndCodecSubscription.c
 * @brief    Media readers and codecs subscription
 * @note    This file implements the subscription of supported media
 *            readers and decoders for the VSS. Potential support can
 *            be activated or de-activated
 *            using compilation flags set in the projects settings.
 *************************************************************************
 */

#include "NXPSW_CompilerSwitches.h"


#include "M4OSA_Debug.h"
#include "M4VSS3GPP_InternalTypes.h"                /**< Include for VSS specific types */
#include "M4VSS3GPP_InternalFunctions.h"            /**< Registration module */

/* _______________________ */
/*|                       |*/
/*|  reader subscription  |*/
/*|_______________________|*/

/* Reader registration : at least one reader must be defined */
#ifndef M4VSS_SUPPORT_READER_3GP
#ifndef M4VSS_SUPPORT_READER_AMR
#ifndef M4VSS_SUPPORT_READER_MP3
#ifndef M4VSS_SUPPORT_READER_PCM
#ifndef M4VSS_SUPPORT_AUDEC_NULL
#error "no reader registered"
#endif /* M4VSS_SUPPORT_AUDEC_NULL */
#endif /* M4VSS_SUPPORT_READER_PCM */
#endif /* M4VSS_SUPPORT_READER_MP3 */
#endif /* M4VSS_SUPPORT_READER_AMR */
#endif /* M4VSS_SUPPORT_READER_3GP */

/* There must be at least one MPEG4 decoder */
#if !defined(M4VSS_SUPPORT_VIDEC_3GP) && !defined(M4VSS_ENABLE_EXTERNAL_DECODERS)
#error "Wait, what?"
/* "Hey, this is the VSS3GPP speaking. Pray tell, how the heck do you expect me to be able to do
any editing without a built-in video decoder, nor the possibility to receive an external one?!
Seriously, I'd love to know." */
#endif

/* Include files for each reader to subscribe */
#ifdef M4VSS_SUPPORT_READER_3GP
#include "VideoEditor3gpReader.h"
#endif
#ifdef M4VSS_SUPPORT_READER_AMR
#include "M4READER_Amr.h"
#endif
#ifdef M4VSS_SUPPORT_READER_MP3
#include "VideoEditorMp3Reader.h"
#endif
#ifdef M4VSS_SUPPORT_READER_PCM
#include "M4READER_Pcm.h"
#endif


/* ______________________________ */
/*|                              |*/
/*|  audio decoder subscription  |*/
/*|______________________________|*/

#include "VideoEditorAudioDecoder.h"
#include "VideoEditorVideoDecoder.h"
#include "M4DECODER_Null.h"
#ifdef M4VSS_SUPPORT_AUDEC_NULL
#include "M4AD_Null.h"
#endif

/* _______________________ */
/*|                       |*/
/*|  writer subscription  |*/
/*|_______________________|*/

/* Writer registration : at least one writer must be defined */
//#ifndef M4VSS_SUPPORT_WRITER_AMR
#ifndef M4VSS_SUPPORT_WRITER_3GPP
#error "no writer registered"
#endif /* M4VSS_SUPPORT_WRITER_3GPP */
//#endif /* M4VSS_SUPPORT_WRITER_AMR */

/* Include files for each writer to subscribe */
//#ifdef M4VSS_SUPPORT_WRITER_AMR
/*extern M4OSA_ERR M4WRITER_AMR_getInterfaces( M4WRITER_OutputFileType* Type,
M4WRITER_GlobalInterface** SrcGlobalInterface,
M4WRITER_DataInterface** SrcDataInterface);*/
//#endif
#ifdef M4VSS_SUPPORT_WRITER_3GPP
extern M4OSA_ERR M4WRITER_3GP_getInterfaces( M4WRITER_OutputFileType* Type,
                                            M4WRITER_GlobalInterface** SrcGlobalInterface,
                                            M4WRITER_DataInterface** SrcDataInterface);
#endif

/* ______________________________ */
/*|                              |*/
/*|  video encoder subscription  |*/
/*|______________________________|*/
#include "VideoEditorAudioEncoder.h"
#include "VideoEditorVideoEncoder.h"


/* ______________________________ */
/*|                              |*/
/*|  audio encoder subscription  |*/
/*|______________________________|*/


#define M4ERR_CHECK_NULL_RETURN_VALUE(retval, pointer) if ((pointer) == M4OSA_NULL)\
    return ((M4OSA_ERR)(retval));

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_SubscribeMediaAndCodec()
 * @brief    This function registers the reader, decoders, writers and encoders
 *          in the VSS.
 * @note
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext is NULL
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_subscribeMediaAndCodec(M4VSS3GPP_MediaAndCodecCtxt *pContext)
{
    M4OSA_ERR                   err = M4NO_ERROR;

    M4READER_MediaType          readerMediaType;
    M4READER_GlobalInterface*   pReaderGlobalInterface;
    M4READER_DataInterface*     pReaderDataInterface;

    M4WRITER_OutputFileType     writerMediaType;
    M4WRITER_GlobalInterface*   pWriterGlobalInterface;
    M4WRITER_DataInterface*     pWriterDataInterface;

    M4AD_Type                   audioDecoderType;
    M4ENCODER_AudioFormat       audioCodecType;
    M4ENCODER_AudioGlobalInterface* pAudioCodecInterface;
    M4AD_Interface*             pAudioDecoderInterface;

    M4DECODER_VideoType         videoDecoderType;
    M4ENCODER_Format            videoCodecType;
    M4ENCODER_GlobalInterface*  pVideoCodecInterface;
    M4DECODER_VideoInterface*   pVideoDecoderInterface;

    M4ERR_CHECK_NULL_RETURN_VALUE(M4ERR_PARAMETER, pContext);

    /* _______________________ */
    /*|                       |*/
    /*|  reader subscription  |*/
    /*|_______________________|*/

    /* --- 3GP --- */

#ifdef M4VSS_SUPPORT_READER_3GP
    err = VideoEditor3gpReader_getInterface( &readerMediaType, &pReaderGlobalInterface,
         &pReaderDataInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4READER_3GP interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerReader( pContext, readerMediaType, pReaderGlobalInterface,
        pReaderDataInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register 3GP reader");
#endif /* M4VSS_SUPPORT_READER_3GP */

    /* --- AMR --- */

#ifdef M4VSS_SUPPORT_READER_AMR
    err = M4READER_AMR_getInterfaces( &readerMediaType, &pReaderGlobalInterface,
        &pReaderDataInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4READER_AMR interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerReader( pContext, readerMediaType, pReaderGlobalInterface,
        pReaderDataInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register AMR reader");
#endif /* M4VSS_SUPPORT_READER_AMR */

    /* --- MP3 --- */

#ifdef M4VSS_SUPPORT_READER_MP3
    err = VideoEditorMp3Reader_getInterface( &readerMediaType, &pReaderGlobalInterface,
         &pReaderDataInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4READER_MP3 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerReader( pContext, readerMediaType, pReaderGlobalInterface,
        pReaderDataInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register MP3 reader");
#endif /* M4VSS_SUPPORT_READER_MP3 */

    /* --- PCM --- */

#ifdef M4VSS_SUPPORT_READER_PCM
    err = M4READER_PCM_getInterfaces( &readerMediaType, &pReaderGlobalInterface,
        &pReaderDataInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4READER_PCM interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerReader( pContext, readerMediaType, pReaderGlobalInterface,
        pReaderDataInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register PCM reader");
#endif /* M4VSS_SUPPORT_READER_PCM */

    /* ______________________________ */
    /*|                              |*/
    /*|  video decoder subscription  |*/
    /*|______________________________|*/

    /* --- MPEG4 & H263 --- */

#ifdef M4VSS_SUPPORT_VIDEC_3GP
    err = VideoEditorVideoDecoder_getInterface_MPEG4(&videoDecoderType, (M4OSA_Void *)&pVideoDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4DECODER_MPEG4 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoDecoder( pContext, videoDecoderType, pVideoDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register MPEG4 decoder");
#endif /* M4VSS_SUPPORT_VIDEC_3GP */

#ifdef M4VSS_SUPPORT_VIDEO_AVC
    err = VideoEditorVideoDecoder_getInterface_H264(&videoDecoderType, (M4OSA_Void *)&pVideoDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4DECODER_H264 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoDecoder( pContext, videoDecoderType, pVideoDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register H264 decoder");
#endif /* M4VSS_SUPPORT_VIDEC_3GP */

#ifdef M4VSS_SUPPORT_VIDEC_NULL
    err = M4DECODER_NULL_getInterface(
              &videoDecoderType, &pVideoDecoderInterface);
    if (M4NO_ERROR != err) {
        M4OSA_TRACE1_0("M4VD NULL Decoder interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoDecoder(
              pContext, videoDecoderType, pVideoDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err, "M4VSS3GPP_subscribeMediaAndCodec: \
        can't register video NULL decoder");
#endif
    /* ______________________________ */
    /*|                              |*/
    /*|  audio decoder subscription  |*/
    /*|______________________________|*/

    /* --- AMRNB --- */

#ifdef M4VSS_SUPPORT_AUDEC_AMRNB
    err = VideoEditorAudioDecoder_getInterface_AMRNB(&audioDecoderType, &pAudioDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4 AMRNB interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioDecoder( pContext, audioDecoderType, pAudioDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register AMRNB decoder");
#endif /* M4VSS_SUPPORT_AUDEC_AMRNB */

    /* --- AAC --- */

#ifdef M4VSS_SUPPORT_AUDEC_AAC
    err = VideoEditorAudioDecoder_getInterface_AAC(&audioDecoderType, &pAudioDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4 AAC interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioDecoder( pContext, audioDecoderType, pAudioDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register AAC decoder");
#endif /* M4VSS_SUPPORT_AUDEC_AAC */

    /* --- MP3 --- */

#ifdef M4VSS_SUPPORT_AUDEC_MP3
    err = VideoEditorAudioDecoder_getInterface_MP3(&audioDecoderType, &pAudioDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4 MP3 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioDecoder( pContext, audioDecoderType, pAudioDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register MP3 decoder");
#endif  /* M4VSS_SUPPORT_AUDEC_MP3 */


    /* --- NULL --- */

#ifdef M4VSS_SUPPORT_AUDEC_NULL
    err = M4AD_NULL_getInterface( &audioDecoderType, &pAudioDecoderInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4AD NULL Decoder interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioDecoder( pContext, audioDecoderType, pAudioDecoderInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register EVRC decoder");
#endif  /* M4VSS_SUPPORT_AUDEC_NULL */

    /* _______________________ */
    /*|                       |*/
    /*|  writer subscription  |*/
    /*|_______________________|*/


    /* --- 3GPP --- */

#ifdef M4VSS_SUPPORT_WRITER_3GPP
    /* retrieves the 3GPP writer media type and pointer to functions*/
    err = M4WRITER_3GP_getInterfaces( &writerMediaType, &pWriterGlobalInterface,
        &pWriterDataInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4WRITER_3GP interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerWriter( pContext, writerMediaType, pWriterGlobalInterface,
        pWriterDataInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register 3GPP writer");
#endif /* M4VSS_SUPPORT_WRITER_3GPP */

    /* ______________________________ */
    /*|                              |*/
    /*|  video encoder subscription  |*/
    /*|______________________________|*/

    /* --- MPEG4 --- */

#ifdef M4VSS_SUPPORT_ENCODER_MPEG4
    /* retrieves the MPEG4 encoder type and pointer to functions*/
    err = VideoEditorVideoEncoder_getInterface_MPEG4(&videoCodecType, &pVideoCodecInterface,
         M4ENCODER_OPEN_ADVANCED);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4MP4E_MPEG4 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoEncoder( pContext, videoCodecType, pVideoCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register video MPEG4 encoder");
#endif /* M4VSS_SUPPORT_ENCODER_MPEG4 */

    /* --- H263 --- */

#ifdef M4VSS_SUPPORT_ENCODER_MPEG4
    /* retrieves the H263 encoder type and pointer to functions*/
    err = VideoEditorVideoEncoder_getInterface_H263(&videoCodecType, &pVideoCodecInterface,
         M4ENCODER_OPEN_ADVANCED);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4MP4E_H263 interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoEncoder( pContext, videoCodecType, pVideoCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register video H263 encoder");
#endif /* M4VSS_SUPPORT_ENCODER_MPEG4 */

#ifdef M4VSS_SUPPORT_ENCODER_AVC
    /* retrieves the H264 encoder type and pointer to functions*/
    err = VideoEditorVideoEncoder_getInterface_H264(&videoCodecType, &pVideoCodecInterface,
         M4ENCODER_OPEN_ADVANCED);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4VSS3GPP_subscribeMediaAndCodec: M4H264E interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerVideoEncoder( pContext, videoCodecType, pVideoCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register video H264 encoder");
#endif /* M4VSS_SUPPORT_ENCODER_AVC */

    /* ______________________________ */
    /*|                              |*/
    /*|  audio encoder subscription  |*/
    /*|______________________________|*/

    /* --- AMR --- */

#ifdef M4VSS_SUPPORT_ENCODER_AMR
    /* retrieves the AMR encoder type and pointer to functions*/
    err = VideoEditorAudioEncoder_getInterface_AMRNB(&audioCodecType, &pAudioCodecInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4AMR interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioEncoder( pContext, audioCodecType, pAudioCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register audio AMR encoder");
#endif /* M4VSS_SUPPORT_ENCODER_AMR */

    /* --- AAC --- */

#ifdef M4VSS_SUPPORT_ENCODER_AAC
    /* retrieves the AAC encoder type and pointer to functions*/
    err = VideoEditorAudioEncoder_getInterface_AAC(&audioCodecType, &pAudioCodecInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4AAC interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioEncoder( pContext, audioCodecType, pAudioCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register audio AAC encoder");
#endif /* M4VSS_SUPPORT_ENCODER_AAC */

    /* --- EVRC --- */

#ifdef M4VSS_SUPPORT_ENCODER_EVRC
    /* retrieves the EVRC encoder type and pointer to functions*/
    err = M4EVRC_getInterfaces( &audioCodecType, &pAudioCodecInterface);
    if (M4NO_ERROR != err)
    {
        M4OSA_TRACE1_0("M4EVRC interface allocation error");
        return err;
    }
    err = M4VSS3GPP_registerAudioEncoder( pContext, audioCodecType, pAudioCodecInterface);
    M4OSA_DEBUG_IF1((err != M4NO_ERROR), err,
        "M4VSS3GPP_subscribeMediaAndCodec: can't register audio EVRC encoder");
#endif /* M4VSS_SUPPORT_ENCODER_EVRC */

#ifdef M4VSS_SUPPORT_OMX_CODECS
    pContext->bAllowFreeingOMXCodecInterface = M4OSA_TRUE;   /* when NXP SW codecs are registered,
                                                               then allow unregistration*/
#endif


    return err;
}

