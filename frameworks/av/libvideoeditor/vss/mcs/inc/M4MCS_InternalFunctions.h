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
 * @file    M4MCS_InternalFunctions.h
 * @brief   This file contains all functions declarations internal
 *          to the MCS.
 *************************************************************************
 */

#ifndef __M4MCS_INTERNALFUNCTIONS_H__
#define __M4MCS_INTERNALFUNCTIONS_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "M4VPP_API.h"
#include "M4ENCODER_common.h"

/**
 **************************************************************************
 * M4OSA_ERR M4MCS_intApplyVPP( M4VPP_Context pContext,
 *                              M4VIFI_ImagePlane* pPlaneIn,
 *                              M4VIFI_ImagePlane* pPlaneOut)
 * @brief   Do the video rendering and the resize (if needed)
 * @note    It is called by the video encoder
 * @param   pContext    (IN)     VPP context, which actually is the MCS
 *                               internal context in our case
 * @param   pPlaneIn    (IN)     Contains the image
 * @param   pPlaneOut   (IN/OUT) Pointer to an array of 3 planes that will
 *                               contain the output YUV420 image
 * @return  M4NO_ERROR:                 No error
 * @return  ERR_MCS_VIDEO_DECODE_ERROR: the video decoding failed
 * @return  ERR_MCS_RESIZE_ERROR:       the resizing failed
 * @return  Any error returned by an underlaying module
 **************************************************************************
 */
M4OSA_ERR M4MCS_intApplyVPP(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
                            M4VIFI_ImagePlane* pPlaneOut);

/**
 **************************************************************************
 * M4OSA_ERR M4MCS_SubscribeMediaAndCodec(M4MCS_Context pContext);
 * @brief    This function registers the reader, decoders, writers and encoders
 *           in the MCS.
 * @note
 * @param    pContext:    (IN) Execution context.
 * @return   M4NO_ERROR:        there is no error
 * @return   M4ERR_PARAMETER    pContext is NULL
 **************************************************************************
 */
M4OSA_ERR M4MCS_subscribeMediaAndCodec(M4MCS_Context pContext);

/**
 **************************************************************************
 * @brief    Clear encoders, decoders, reader and writers interfaces tables
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    The context is null
 **************************************************************************
 */
M4OSA_ERR   M4MCS_clearInterfaceTables(M4MCS_Context pContext);

/**
 **************************************************************************
 * M4OSA_ERR   M4MCS_registerWriter(M4MCS_Context pContext,
 *                                  M4VIDEOEDITING_FileType MediaType,
 *                                  M4WRITER_GlobalInterface *pWtrGlobalInterface,
 *                                  M4WRITER_DataInterface *pWtrDataInterface)
 * @brief   This function will register a specific file format writer.
 * @note    According to the Mediatype, this function will store in the internal
 *          context the writer context.
 * @param   pContext:    (IN) Execution context.
 * @return  M4NO_ERROR:         there is no error
 * @return  M4ERR_PARAMETER     pContext,pWtrGlobalInterface or pWtrDataInterface
 *                              is M4OSA_NULL (debug only), or invalid MediaType
 **************************************************************************
 */
M4OSA_ERR   M4MCS_registerWriter(
                        M4MCS_Context pContext,
                        M4WRITER_OutputFileType MediaType,
                        M4WRITER_GlobalInterface* pWtrGlobalInterface,
                        M4WRITER_DataInterface* pWtrDataInterface);

/**
 ******************************************************************************
 * M4OSA_ERR   M4MCS_registerEncoder(   M4MCS_Context pContext,
 *                                      M4VIDEOEDITING_VideoFormat mediaType,
 *                                      M4ENCODER_GlobalInterface *pEncGlobalInterface)
 * @brief   This function will register a specific video encoder.
 * @note    According to the Mediatype, this function will store in the internal
 *          context the encoder context.
 * @param   pContext:    (IN) Execution context.
 * @return  M4NO_ERROR:         there is no error
 * @return  M4ERR_PARAMETER     pContext or pEncGlobalInterface is
 *                              M4OSA_NULL (debug only), or invalid MediaType
 ******************************************************************************
 */
M4OSA_ERR   M4MCS_registerVideoEncoder(
                        M4MCS_Context pContext,
                        M4ENCODER_Format MediaType,
                        M4ENCODER_GlobalInterface *pEncGlobalInterface);

/**
 ******************************************************************************
 * M4OSA_ERR   M4MCS_registerAudioEncoder(  M4MCS_Context pContext,
 *                                          M4ENCODER_AudioFormat mediaType,
 *                                          M4ENCODER_AudioGlobalInterface *pEncGlobalInterface)
 * @brief   This function will register a specific audio encoder.
 * @note    According to the Mediatype, this function will store in the internal
 *          context the encoder context.
 * @param   pContext:               (IN)   Execution context.
 * @param   mediaType:              (IN)   The media type.
 * @param   pEncGlobalInterface:    (OUT)  The encoder interface functions.
 * @return  M4NO_ERROR:       there is no error
 * @return  M4ERR_PARAMETER:  pContext or pEncGlobalInterface is
 *                              M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR   M4MCS_registerAudioEncoder(
                        M4MCS_Context pContext,
                        M4ENCODER_AudioFormat MediaType,
                        M4ENCODER_AudioGlobalInterface *pEncGlobalInterface);

/**
 **************************************************************************
 * @brief    Register reader.
 * @param    pContext            (IN/OUT) MCS context.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 **************************************************************************
 */
M4OSA_ERR   M4MCS_registerReader(   M4MCS_Context pContext,
                                    M4READER_MediaType mediaType,
                                    M4READER_GlobalInterface *pRdrGlobalInterface,
                                    M4READER_DataInterface *pRdrDataInterface);

/**
 **************************************************************************
 * @brief   Register video decoder
 * @param   pContext             (IN/OUT) MCS context.
 * @param   decoderType          (IN) Decoder type
 * @param   pDecoderInterface    (IN) Decoder interface.
 * @return  M4NO_ERROR:            No error
 * @return  M4ERR_PARAMETER:    A parameter is null (in DEBUG only),or the
 *                              decoder type is invalid
 **************************************************************************
 */
M4OSA_ERR   M4MCS_registerVideoDecoder( M4MCS_Context pContext,
                                        M4DECODER_VideoType decoderType,
                                        M4DECODER_VideoInterface *pDecoderInterface);

/**
 ************************************************************************
 * @brief   Register audio decoder
 * @note    This function is used internaly by the MCS to register Core audio decoders,
 * @param   context            (IN/OUT) MCS context.
 * @param   decoderType        (IN)     Audio decoder type
 * @param   pDecoderInterface  (IN)     Audio decoder interface.
 * @return  M4NO_ERROR:        No error
 * @return  M4ERR_PARAMETER:   A parameter is null, or the decoder type is invalid(in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_registerAudioDecoder(M4MCS_Context pContext, M4AD_Type decoderType,
                                        M4AD_Interface *pDecoderInterface);

/**
 ************************************************************************
 * @brief   Unregister writer
 * @param   pContext            (IN/OUT) MCS context.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllWriters(M4MCS_Context pContext);

/**
 ************************************************************************
 * @brief   Unregister the encoders
 * @param   pContext            (IN/OUT) MCS context.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllEncoders(M4MCS_Context pContext);

/**
 ************************************************************************
 * @brief   Unregister reader
 * @param   pContext            (IN/OUT) MCS context.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllReaders(M4MCS_Context pContext);

/**
 ************************************************************************
 * @brief   Unregister the decoders
 * @param   pContext            (IN/OUT) MCS context.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_unRegisterAllDecoders(M4MCS_Context pContext);

/**
 ************************************************************************
 * @brief   Set current writer
 * @param   pContext            (IN/OUT) MCS context.
 * @param   mediaType           (IN) Media type.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return  M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:  Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentWriter( M4MCS_Context pContext,
                                    M4VIDEOEDITING_FileType mediaType);

/**
 ************************************************************************
 * @brief    Set a video encoder
 * @param    pContext            (IN/OUT) MCS context.
 * @param    MediaType           (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:                    A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentVideoEncoder(   M4MCS_Context pContext,
                                            M4VIDEOEDITING_VideoFormat mediaType);

/**
 ************************************************************************
 * @brief    Set an audio encoder
 * @param    context            (IN/OUT) MCS context.
 * @param    MediaType        (IN) Encoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentAudioEncoder(   M4MCS_Context pContext,
                                            M4VIDEOEDITING_AudioFormat mediaType);

/**
 ************************************************************************
 * @brief    Set current reader
 * @param    pContext            (IN/OUT) MCS context.
 * @param    mediaType           (IN) Media type.
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:   A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentReader( M4MCS_Context pContext,
                                    M4VIDEOEDITING_FileType mediaType);

/**
 ************************************************************************
 * @brief    Set a video decoder
 * @param    pContext           (IN/OUT) MCS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:       A parameter is null (in DEBUG only)
 * @return    M4WAR_MCS_MEDIATYPE_NOT_SUPPORTED:    Media type not supported
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentVideoDecoder(   M4MCS_Context pContext,
                                            M4_StreamType mediaType);

/**
 ************************************************************************
 * @brief    Set an audio decoder
 * @param    context            (IN/OUT) MCS context.
 * @param    decoderType        (IN) Decoder type
 * @return    M4NO_ERROR:         No error
 * @return    M4ERR_PARAMETER:    A parameter is null (in DEBUG only)
 ************************************************************************
 */
M4OSA_ERR   M4MCS_setCurrentAudioDecoder(M4MCS_Context pContext, M4_StreamType mediaType);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_intCheckAudioEffects(M4MCS_InternalContext* pContext)
 * @brief    Check if an effect has to be applied currently
 * @note     It is called by the stepEncoding function
 * @param    pContext    (IN)   MCS internal context
 * @return   M4NO_ERROR:        No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_intCheckAudioEffects(M4MCS_InternalContext* pC);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_editAudioEffectFct_FadeIn()
 * @brief    Apply audio effect FadeIn to pPCMdata
 * @param    pC           (IN/OUT) Internal edit context
 * @param    pPCMdata     (IN/OUT) Input and Output PCM audio data
 * @param    uiPCMsize    (IN)     Size of pPCMdata
 * @param    pProgress    (IN)     Effect progress
 * @return   M4NO_ERROR:           No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_editAudioEffectFct_FadeIn(  M4OSA_Void *pFunctionContext,
                                            M4OSA_Int16 *pPCMdata,
                                            M4OSA_UInt32 uiPCMsize,
                                            M4MCS_ExternalProgress *pProgress);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_editAudioEffectFct_FadeIn()
 * @brief    Apply audio effect FadeIn to pPCMdata
 * @param    pC           (IN/OUT) Internal edit context
 * @param    pPCMdata     (IN/OUT) Input and Output PCM audio data
 * @param    uiPCMsize    (IN)     Size of pPCMdata
 * @param    pProgress    (IN)     Effect progress
 * @return   M4NO_ERROR:           No error
 ******************************************************************************
 */
M4OSA_ERR M4MCS_editAudioEffectFct_FadeOut( M4OSA_Void *pFunctionContext,
                                            M4OSA_Int16 *pPCMdata,
                                            M4OSA_UInt32 uiPCMsize,
                                            M4MCS_ExternalProgress *pProgress);

#ifdef __cplusplus
}
#endif

#endif /* __M4MCS_INTERNALFUNCTIONS_H__ */

