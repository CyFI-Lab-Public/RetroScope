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
 * @file    M4VSS3GPP_InternalFunctions.h
 * @brief    This file contains all function prototypes not visible to the external world.
 * @note
 ******************************************************************************
*/


#ifndef __M4VSS3GPP_INTERNALFUNCTIONS_H__
#define __M4VSS3GPP_INTERNALFUNCTIONS_H__

#include "NXPSW_CompilerSwitches.h"
/**
 *    VSS public API and types */
#include "M4VSS3GPP_API.h"

/**
 *    VSS private types */
#include "M4VSS3GPP_InternalTypes.h"


#include "M4READER_Common.h" /**< for M4_AccessUnit definition */

#ifdef __cplusplus
extern "C" {
#endif

/* All errors are fatal in the VSS */
#define M4ERR_CHECK_RETURN(err) if(M4NO_ERROR!=err) return err;

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditStepVideo()
 * @brief    One step of video processing
 * @param   pC    (IN/OUT) Internal edit context
  ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intEditStepVideo(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditStepAudio()
 * @brief    One step of audio processing
 * @param   pC    (IN/OUT) Internal edit context
  ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intEditStepAudio(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditStepMP3()
 * @brief    One step of audio processing for the MP3 clip
 * @param   pC    (IN/OUT) Internal edit context
  ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intEditStepMP3(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intOpenClip()
 * @brief    Open next clip
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intOpenClip(M4VSS3GPP_InternalEditContext *pC, M4VSS3GPP_ClipContext **hClip,
                                 M4VSS3GPP_ClipSettings *pClipSettings);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intDestroyVideoEncoder()
 * @brief    Destroy the video encoder
 * @note
  ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intDestroyVideoEncoder(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreateVideoEncoder()
 * @brief    Creates the video encoder
 * @note
  ******************************************************************************
*/
M4OSA_ERR  M4VSS3GPP_intCreateVideoEncoder(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intReachedEndOfVideo()
 * @brief    Do what to do when the end of a clip video track is reached
 * @note    If there is audio on the current clip, process it, else switch to the next clip
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intReachedEndOfVideo(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intReachedEndOfAudio()
 * @brief    Do what to do when the end of a clip audio track is reached
 * @param   pC            (IN/OUT) Internal edit context
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intReachedEndOfAudio(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCheckClipCompatibleWithVssEditing()
 * @brief    Check if the clip is compatible with VSS editing
 * @note
 * @param   pClipCtxt            (IN) internal clip context
 * @param    pClipProperties     (OUT) Pointer to a valid ClipProperties structure.
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intCheckClipCompatibleWithVssEditing(M4VIDEOEDITING_ClipProperties \
                                                            *pClipProperties);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipOpen()
 * @brief    Open a clip. Creates a clip context.
 * @note
 * @param   hClipCtxt            (OUT) Return the internal clip context
 * @param   pClipSettings        (IN) Edit settings of this clip. The module will keep a
 *                                        reference to this pointer
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param    bSkipAudioTrack        (IN) If true, do not open the audio
 * @param    bFastOpenMode        (IN) If true, use the fast mode of the 3gpp reader
 *                                            (only the first AU is read)
 * @return    M4NO_ERROR:                No error
 * @return    M4ERR_ALLOC:            There is no more available memory
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipInit (
    M4VSS3GPP_ClipContext **hClipCtxt,
    M4OSA_FileReadPointer *pFileReadPtrFct
);

M4OSA_ERR M4VSS3GPP_intClipOpen (
    M4VSS3GPP_ClipContext *pClipCtxt,
    M4VSS3GPP_ClipSettings *pClipSettings,
    M4OSA_Bool bSkipAudioTrack,
    M4OSA_Bool bFastOpenMode,
    M4OSA_Bool bAvoidOpeningVideoDec
);


/**
 ******************************************************************************
 * M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack()
 * @brief    Delete the audio track. Clip will be like if it had no audio track
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 ******************************************************************************
*/
M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack(M4VSS3GPP_ClipContext *pClipCtxt);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipDecodeVideoUpToCurrentTime()
 * @brief    Jump to the previous RAP and decode up to the current video time
 * @param   pClipCtxt    (IN) Internal clip context
 * @param   iCts        (IN) Target CTS
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipDecodeVideoUpToCts(M4VSS3GPP_ClipContext* pClipCtxt, M4OSA_Int32 iCts);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipReadNextAudioFrame()
 * @brief    Read one AU frame in the clip
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipReadNextAudioFrame(M4VSS3GPP_ClipContext *pClipCtxt);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipDecodeCurrentAudioFrame()
 * @brief    Decode the current AUDIO frame.
 * @note
 * @param   pClipCtxt        (IN) internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipDecodeCurrentAudioFrame(M4VSS3GPP_ClipContext *pClipCtxt);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipJumpAudioAt()
 * @brief    Jump in the audio track of the clip.
 * @note
 * @param   pClipCtxt            (IN) internal clip context
 * @param   pJumpCts            (IN/OUT) in:target CTS, out: reached CTS
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipJumpAudioAt(M4VSS3GPP_ClipContext *pClipCtxt, M4OSA_Int32 *pJumpCts);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intClipClose()
 * @brief    Close a clip. Destroy the context.
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intClipClose(M4VSS3GPP_ClipContext *pClipCtxt);

M4OSA_ERR M4VSS3GPP_intClipCleanUp(M4VSS3GPP_ClipContext *pClipCtxt);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intEditJumpMP3()
 * @brief    One step of jumping processing for the MP3 clip.
 * @note    On one step, the jump of several AU is done
 * @param   pC    (IN/OUT) Internal edit context
  ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intEditJumpMP3(M4VSS3GPP_InternalEditContext *pC);

/**
 ******************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerWriter()
 * @brief    This function will register a specific file format writer.
 * @note    According to the Mediatype, this function will store in the internal context
 *             the writer context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext,pWtrGlobalInterface or pWtrDataInterface is
 *                                 M4OSA_NULL (debug only), or invalid MediaType
 ******************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerWriter(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                     M4WRITER_OutputFileType MediaType,
                                     M4WRITER_GlobalInterface* pWtrGlobalInterface,
                                     M4WRITER_DataInterface* pWtrDataInterface);

/**
 ******************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerEncoder()
 * @brief    This function will register a specific video encoder.
 * @note    According to the Mediatype, this function will store in the internal context
 *            the encoder context.
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER    pContext or pEncGlobalInterface is M4OSA_NULL (debug only),
 *                                 or invalid MediaType
 ******************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerVideoEncoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4ENCODER_Format MediaType,
                                           M4ENCODER_GlobalInterface *pEncGlobalInterface);

/**
 ******************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerAudioEncoder()
 * @brief    This function will register a specific audio encoder.
 * @note    According to the Mediatype, this function will store in the internal context
 *             the encoder context.
 * @param    pContext:                (IN) Execution context.
 * @param    mediaType:                (IN) The media type.
 * @param    pEncGlobalInterface:    (OUT) the encoder interface functions.
 * @return    M4NO_ERROR: there is no error
 * @return    M4ERR_PARAMETER: pContext or pEncGlobalInterface is M4OSA_NULL (debug only)
 ******************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerAudioEncoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                             M4ENCODER_AudioFormat MediaType,
                                             M4ENCODER_AudioGlobalInterface *pEncGlobalInterface);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerReader()
 * @brief    Register reader.
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerReader(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                     M4READER_MediaType mediaType,
                                     M4READER_GlobalInterface *pRdrGlobalInterface,
                                     M4READER_DataInterface *pRdrDataInterface);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerVideoDecoder()
 * @brief    Register video decoder
 * @param    pContext                (IN/OUT) VSS context.
 * @param    decoderType            (IN) Decoder type
 * @param    pDecoderInterface    (IN) Decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only), or the decoder type
 *                                    is invalid
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerVideoDecoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                            M4DECODER_VideoType decoderType,
                                            M4DECODER_VideoInterface *pDecoderInterface);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_registerAudioDecoder()
 * @brief    Register audio decoder
 * @note    This function is used internaly by the VSS to register audio decoders,
 * @param    context                (IN/OUT) VSS context.
 * @param    decoderType            (IN) Audio decoder type
 * @param    pDecoderInterface    (IN) Audio decoder interface.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null, or the decoder type is invalid
 *                                 (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_registerAudioDecoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                           M4AD_Type decoderType,
                                           M4AD_Interface *pDecoderInterface);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllWriters()
 * @brief    Unregister writer
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_unRegisterAllWriters(M4VSS3GPP_MediaAndCodecCtxt *pC);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllEncoders()
 * @brief    Unregister the encoders
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_unRegisterAllEncoders(M4VSS3GPP_MediaAndCodecCtxt *pC);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllReaders()
 * @brief    Unregister reader
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_unRegisterAllReaders(M4VSS3GPP_MediaAndCodecCtxt *pC);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_unRegisterAllDecoders()
 * @brief    Unregister the decoders
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_unRegisterAllDecoders(M4VSS3GPP_MediaAndCodecCtxt *pC);

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
M4OSA_ERR   M4VSS3GPP_setCurrentWriter(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                        M4VIDEOEDITING_FileType mediaType);

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
M4OSA_ERR   M4VSS3GPP_setCurrentVideoEncoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                                M4SYS_StreamType mediaType);

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
M4OSA_ERR   M4VSS3GPP_setCurrentAudioEncoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                             M4SYS_StreamType mediaType);

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
M4OSA_ERR   M4VSS3GPP_setCurrentReader(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                         M4VIDEOEDITING_FileType mediaType);

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
M4OSA_ERR   M4VSS3GPP_setCurrentVideoDecoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                             M4_StreamType mediaType);

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
M4OSA_ERR   M4VSS3GPP_setCurrentAudioDecoder(M4VSS3GPP_MediaAndCodecCtxt *pC,
                                             M4_StreamType mediaType);

/**
 ************************************************************************
 * M4OSA_ERR   M4VSS3GPP_clearInterfaceTables()
 * @brief    Clear encoders, decoders, reader and writers interfaces tables
 * @param    pContext            (IN/OUT) VSS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    The context is null
 ************************************************************************
*/
M4OSA_ERR   M4VSS3GPP_clearInterfaceTables(M4VSS3GPP_MediaAndCodecCtxt *pC);

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
M4OSA_ERR M4VSS3GPP_subscribeMediaAndCodec(M4VSS3GPP_MediaAndCodecCtxt *pContext);

/**
 ******************************************************************************
 * M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_AMRNB()
 * @brief   Return the length, in bytes, of the AMR Narrow-Band frame contained in the given buffer
 * @note
 * @param   pAudioFrame   (IN) AMRNB frame
 * @return  M4NO_ERROR: No error
 ******************************************************************************
*/
M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_AMRNB(M4OSA_MemAddr8 pAudioFrame);

/**
 ******************************************************************************
 * M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_EVRC()
 * @brief   Return the length, in bytes, of the EVRC frame contained in the given buffer
 * @note
 *     0 1 2 3
 *    +-+-+-+-+
 *    |fr type|              RFC 3558
 *    +-+-+-+-+
 *
 * Frame Type: 4 bits
 *    The frame type indicates the type of the corresponding codec data
 *    frame in the RTP packet.
 *
 * For EVRC and SMV codecs, the frame type values and size of the
 * associated codec data frame are described in the table below:
 *
 * Value   Rate      Total codec data frame size (in octets)
 * ---------------------------------------------------------
 *   0     Blank      0    (0 bit)
 *   1     1/8        2    (16 bits)
 *   2     1/4        5    (40 bits; not valid for EVRC)
 *   3     1/2       10    (80 bits)
 *   4     1         22    (171 bits; 5 padded at end with zeros)
 *   5     Erasure    0    (SHOULD NOT be transmitted by sender)
 *
 * @param   pCpAudioFrame   (IN) EVRC frame
 * @return  M4NO_ERROR: No error
 ******************************************************************************
*/
M4OSA_UInt32 M4VSS3GPP_intGetFrameSize_EVRC(M4OSA_MemAddr8 pAudioFrame);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intBuildAnalysis()
 * @brief    Get video and audio properties from the clip streams
 * @note    This function must return fatal errors only (errors that should not happen in the
 *             final integrated product).
 * @param   pClipCtxt            (IN) internal clip context
 * @param    pClipProperties        (OUT) Pointer to a valid ClipProperties structure.
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intBuildAnalysis(M4VSS3GPP_ClipContext *pClipCtxt,
                                     M4VIDEOEDITING_ClipProperties *pClipProperties);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreateAudioEncoder()
 * @brief    Reset the audio encoder (Create it if needed)
 * @note
  ******************************************************************************
*/
M4OSA_ERR  M4VSS3GPP_intCreateAudioEncoder(M4VSS3GPP_EncodeWriteContext *pC_ewc,
                                             M4VSS3GPP_MediaAndCodecCtxt *pC_ShellAPI,
                                             M4OSA_UInt32 uiAudioBitrate);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intCreate3GPPOutputFile()
 * @brief    Creates and prepare the output MP3 file
 * @note    Creates the writer, Creates the output file, Adds the streams, Readies the
 *            writing process
 * @param   pC    (IN/OUT) Internal edit context
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intCreate3GPPOutputFile(M4VSS3GPP_EncodeWriteContext *pC_ewc,
                                            M4VSS3GPP_MediaAndCodecCtxt *pC_ShellAPI,
                                            M4OSA_FileWriterPointer *pOsaFileWritPtr,
                                            M4OSA_Void* pOutputFile,
                                            M4OSA_FileReadPointer *pOsaFileReadPtr,
                                            M4OSA_Void* pTempFile,
                                            M4OSA_UInt32 maxOutputFileSize);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intAudioMixingCompatibility()
 * @brief    This function allows checking if two clips are compatible with each other for
 *             VSS 3GPP audio mixing feature.
 * @note
 * @param    pC                            (IN) Context of the audio mixer
 * @param    pInputClipProperties        (IN) Clip analysis of the first clip
 * @param    pAddedClipProperties        (IN) Clip analysis of the second clip
 * @return    M4NO_ERROR:            No error
 * @return    M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION
 * @return  M4VSS3GPP_ERR_INPUT_CLIP_IS_NOT_A_3GPP
 * @return  M4NO_ERROR
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_intAudioMixingCompatibility(M4VSS3GPP_InternalAudioMixingContext *pC,
                                                 M4VIDEOEDITING_ClipProperties \
                                                 *pInputClipProperties,
                                                 M4VIDEOEDITING_ClipProperties  \
                                                 *pAddedClipProperties);

/**
 ******************************************************************************
 * M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack()
 * @brief    Delete the audio track. Clip will be like if it had no audio track
 * @note
 * @param   pClipCtxt            (IN) Internal clip context
 ******************************************************************************
*/
M4OSA_Void M4VSS3GPP_intClipDeleteAudioTrack(M4VSS3GPP_ClipContext *pClipCtxt);

/******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intStartAU()
 * @brief    StartAU writer-like interface used for the VSS 3GPP only
 * @note
 * @param    pContext: (IN) It is the VSS 3GPP context in our case
 * @param    streamID: (IN) Id of the stream to which the Access Unit is related.
 * @param    pAU:      (IN/OUT) Access Unit to be prepared.
 * @return    M4NO_ERROR: there is no error
 ******************************************************************************
*/
M4OSA_ERR  M4VSS3GPP_intStartAU(M4WRITER_Context pContext, M4SYS_StreamID streamID,
                                 M4SYS_AccessUnit* pAU);

/******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intProcessAU()
 * @brief    ProcessAU writer-like interface used for the VSS 3GPP only
 * @note
 * @param    pContext: (IN) It is the VSS 3GPP context in our case
 * @param    streamID: (IN) Id of the stream to which the Access Unit is related.
 * @param    pAU:      (IN/OUT) Access Unit to be written
 * @return    M4NO_ERROR: there is no error
 ******************************************************************************
*/
M4OSA_ERR  M4VSS3GPP_intProcessAU(M4WRITER_Context pContext, M4SYS_StreamID streamID,
                                     M4SYS_AccessUnit* pAU);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_intVPP()
 * @brief    We implement our own VideoPreProcessing function
 * @note    It is called by the video encoder
 * @param    pContext    (IN) VPP context, which actually is the VSS 3GPP context in our case
 * @param    pPlaneIn    (IN)
 * @param    pPlaneOut    (IN/OUT) Pointer to an array of 3 planes that will contain the
 *                             output YUV420 image
 * @return    M4NO_ERROR:    No error
 ******************************************************************************
*/
M4OSA_ERR  M4VSS3GPP_intVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
                             M4VIFI_ImagePlane* pPlaneOut);

#ifdef __cplusplus
}
#endif

#endif /* __M4VSS3GPP_INTERNALFUNCTIONS_H__ */

