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
 * @file    M4DECODER_Null.c
 * @brief   Implementation of the Null decoder public interface
 * @note    This file implements a "null" video decoder, i.e. a decoder
 *          that does nothing
*************************************************************************
*/
#include "NXPSW_CompilerSwitches.h"

#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"
#include "M4TOOL_VersionInfo.h"
#include "M4DA_Types.h"
#include "M4DECODER_Common.h"
#include "M4DECODER_Null.h"

/**
 ************************************************************************
 * NULL Video Decoder version information
 ************************************************************************
*/
/* CHANGE_VERSION_HERE */
#define M4DECODER_NULL_MAJOR    1
#define M4DECODER_NULL_MINOR    0
#define M4DECODER_NULL_REVISION 0

/**
 ************************************************************************
 * structure    M4_VideoHandler_Context
 * @brief       Defines the internal context of a video decoder instance
 * @note        The context is allocated and freed by the video decoder
 ************************************************************************
*/
typedef struct {
    void*                    m_pLibrary;            // Core library identifier
    M4OSA_Int32              m_DecoderId;           // Core decoder identifier
    M4OSA_Int32              m_RendererId;          // Core renderer identifier
    M4_VideoStreamHandler*   m_pVideoStreamhandler; // Video stream description
    M4_AccessUnit*           m_pNextAccessUnitToDecode; // Access unit used to
                                                        // read and decode one frame
    void*                    m_pUserData;           // Pointer to any user data
    M4READER_DataInterface*  m_pReader;             // Reader data interface
    M4OSA_Bool               m_bDoRendering;        // Decides if render required
    M4OSA_Int32              m_structSize;          // Size of the structure

    M4DECODER_OutputFilter* m_pVideoFilter;         // Color conversion filter
    M4VIFI_ImagePlane       *pDecYuvData;           // Pointer to Yuv data plane
    M4VIFI_ImagePlane       *pDecYuvWithEffect;     // Pointer to Yuv plane with color effect
    M4OSA_Bool               bYuvWithEffectSet;     // Original Yuv data OR Yuv with color effect

} M4_VideoHandler_Context;

/***********************************************************************/
/************** M4DECODER_VideoInterface implementation ****************/
/***********************************************************************/

/**
 ************************************************************************
 * @brief   Creates an instance of the decoder
 * @note    Allocates the context
 *
 * @param   pContext:       (OUT)   Context of the decoder
 * @param   pStreamHandler: (IN)    Pointer to a video stream description
 * @param   pSrcInterface:  (IN)    Pointer to the M4READER_DataInterface
 *                                  structure that must be used by the
 *                                  decoder to read data from the stream
 * @param   pAccessUnit     (IN)    Pointer to an access unit
 *                                  (allocated by the caller) where decoded data
 *                                  are stored
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_ALLOC             A memory allocation has failed
 * @return  M4ERR_PARAMETER         At least one input parameter is not proper
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_create(M4OSA_Context *pContext,
                                M4_StreamHandler *pStreamHandler,
                                M4READER_GlobalInterface *pReaderGlobalInterface,
                                M4READER_DataInterface *pReaderDataInterface,
                                M4_AccessUnit* pAccessUnit,
                                M4OSA_Void* pUserData) {

    M4_VideoHandler_Context* pStreamContext = M4OSA_NULL;

    *pContext = M4OSA_NULL;
    pStreamContext = (M4_VideoHandler_Context*)M4OSA_32bitAlignedMalloc (
                        sizeof(M4_VideoHandler_Context), M4DECODER_MPEG4,
                        (M4OSA_Char *)"M4_VideoHandler_Context");
    if (pStreamContext == 0) {
        return M4ERR_ALLOC;
    }

    pStreamContext->m_structSize = sizeof(M4_VideoHandler_Context);
    pStreamContext->m_pNextAccessUnitToDecode = M4OSA_NULL;
    pStreamContext->m_pLibrary              = M4OSA_NULL;
    pStreamContext->m_pVideoStreamhandler   = M4OSA_NULL;
    pStreamContext->m_DecoderId             = -1;
    pStreamContext->m_RendererId            = -1;

    pStreamContext->m_pUserData = M4OSA_NULL;
    pStreamContext->m_bDoRendering = M4OSA_TRUE;
    pStreamContext->m_pVideoFilter = M4OSA_NULL;
    pStreamContext->bYuvWithEffectSet = M4OSA_FALSE;

    *pContext=pStreamContext;
    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Destroy the instance of the decoder
 * @note    After this call the context is invalid
 *
 * @param   context:    (IN)    Context of the decoder
 *
 * @return  M4NO_ERROR          There is no error
 * @return  M4ERR_PARAMETER     The context is invalid
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_destroy(M4OSA_Context pContext) {

    M4_VideoHandler_Context* pStreamContext = (M4_VideoHandler_Context*)pContext;

    M4OSA_DEBUG_IF1((M4OSA_NULL == pStreamContext),
        M4ERR_PARAMETER, "M4DECODER_NULL_destroy: invalid context pointer");

    free(pStreamContext);

    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Get an option value from the decoder
 * @note    This function allows the caller to retrieve a property value:
 *
 * @param   context:    (IN)        Context of the decoder
 * @param   optionId:   (IN)        Indicates the option to get
 * @param   pValue:     (IN/OUT)    Pointer to structure or value where
 *                                  option is stored
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_PARAMETER         The context is invalid (in DEBUG only)
 * @return  M4ERR_BAD_OPTION_ID     When the option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_NOT_IMPLEMENTED   Function not implemented
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_getOption(M4OSA_Context context,
                                   M4OSA_OptionID optionId,
                                   M4OSA_DataOption  pValue) {

    return M4ERR_NOT_IMPLEMENTED;
}

/**
 ************************************************************************
 * @brief   Set an option value of the decoder
 * @note    Allows the caller to set a property value:
 *
 * @param   context:    (IN)        Context of the decoder
 * @param   optionId:   (IN)        Identifier indicating the option to set
 * @param   pValue:     (IN)        Pointer to structure or value
 *                                  where option is stored
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_BAD_OPTION_ID     The option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_PARAMETER         The option parameter is invalid
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_setOption(M4OSA_Context context,
                                   M4OSA_OptionID optionId,
                                   M4OSA_DataOption pValue) {

    M4DECODER_OutputFilter *pFilterOption;

    M4_VideoHandler_Context *pStreamContext =
        (M4_VideoHandler_Context*)context;

    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 height = 0;
    M4OSA_UInt8 *p_src,*p_des;
    M4VIFI_ImagePlane* pTempDecYuvData = M4OSA_NULL;

    switch (optionId) {
        case M4DECODER_kOptionID_DecYuvData:
            pStreamContext->pDecYuvData = (M4VIFI_ImagePlane *)pValue;
            break;

        case M4DECODER_kOptionID_YuvWithEffectContiguous:
            pStreamContext->pDecYuvWithEffect = (M4VIFI_ImagePlane *)pValue;
            break;

        case M4DECODER_kOptionID_EnableYuvWithEffect:
            pStreamContext->bYuvWithEffectSet = (M4OSA_Bool)pValue;
            break;

        case M4DECODER_kOptionID_YuvWithEffectNonContiguous:
            pTempDecYuvData =  (M4VIFI_ImagePlane *)pValue;

            p_des = pStreamContext->pDecYuvWithEffect[0].pac_data +
                 pStreamContext->pDecYuvWithEffect[0].u_topleft;
            p_src = pTempDecYuvData[0].pac_data +
                 pTempDecYuvData[0].u_topleft;

            for (height = 0; height<pStreamContext->pDecYuvWithEffect[0].u_height;
             height++) {
                memcpy((void *)p_des, (void *)p_src,
                 pStreamContext->pDecYuvWithEffect[0].u_width);

                p_des += pStreamContext->pDecYuvWithEffect[0].u_stride;
                p_src += pTempDecYuvData[0].u_stride;
            }

            p_des = pStreamContext->pDecYuvWithEffect[1].pac_data +
             pStreamContext->pDecYuvWithEffect[1].u_topleft;
            p_src = pTempDecYuvData[1].pac_data +
             pTempDecYuvData[1].u_topleft;

            for (height = 0; height<pStreamContext->pDecYuvWithEffect[1].u_height;
             height++) {
                memcpy((void *)p_des, (void *)p_src,
                 pStreamContext->pDecYuvWithEffect[1].u_width);

                p_des += pStreamContext->pDecYuvWithEffect[1].u_stride;
                p_src += pTempDecYuvData[1].u_stride;
            }

            p_des = pStreamContext->pDecYuvWithEffect[2].pac_data +
             pStreamContext->pDecYuvWithEffect[2].u_topleft;
            p_src = pTempDecYuvData[2].pac_data +
             pTempDecYuvData[2].u_topleft;

            for (height = 0; height<pStreamContext->pDecYuvWithEffect[2].u_height;
             height++) {
                memcpy((void *)p_des, (void *)p_src,
                 pStreamContext->pDecYuvWithEffect[2].u_width);

                p_des += pStreamContext->pDecYuvWithEffect[2].u_stride;
                p_src += pTempDecYuvData[2].u_stride;
            }
            break;

        case M4DECODER_kOptionID_OutputFilter:
            pFilterOption = (M4DECODER_OutputFilter*)pValue;
            break;

        case M4DECODER_kOptionID_DeblockingFilter:
            err = M4ERR_BAD_OPTION_ID;
            break;

        default:
            err = M4ERR_BAD_OPTION_ID;
            break;
    }
    return err;
}

/**
 ************************************************************************
 * @brief   Decode video Access Units up to a target time
 * @note    Parse and decode the video until it can output a decoded image
 *          for which the composition time is equal or greater to the
 *          passed targeted time.
 *          The data are read from the reader data interface passed to
 *          M4DECODER_MPEG4_create.
 *
 * @param   context:    (IN)        Context of the decoder
 * @param   pTime:      (IN/OUT)    IN: Time to decode up to (in msec)
 *                                  OUT:Time of the last decoded frame (in msec)
 * @param   bJump:      (IN)        0 if no jump occured just before this call
 *                                  1 if a a jump has just been made
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_PARAMETER         at least one parameter is not properly set
 * @return  M4WAR_NO_MORE_AU        there is no more access unit to decode (EOS)
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_decode(M4OSA_Context context,
                                M4_MediaTime* pTime, M4OSA_Bool bJump,
                                M4OSA_UInt32 tolerance) {

    // Do nothing; input time stamp itself returned
    return M4NO_ERROR;
}

/**
 ************************************************************************
 * @brief   Renders the video at the specified time.
 * @note
 * @param   context:     (IN)       Context of the decoder
 * @param   pTime:       (IN/OUT)   IN: Time to render to (in msecs)
 *                                  OUT:Time of the rendered frame (in ms)
 * @param   pOutputPlane:(OUT)      Output plane filled with decoded data
 * @param   bForceRender:(IN)       1 if the image must be rendered even it
 *                                  has been rendered already
 *                                  0 if not
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_PARAMETER         At least one parameter is not properly set
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_ALLOC             There is no more available memory
 * @return  M4WAR_VIDEORENDERER_NO_NEW_FRAME    If the frame has already been rendered
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_render(M4OSA_Context context, M4_MediaTime* pTime,
                                M4VIFI_ImagePlane* pOutputPlane,
                                M4OSA_Bool bForceRender) {

    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt32 height;
    M4OSA_UInt8 *p_src,*p_des;
    M4_VideoHandler_Context*    pStreamContext =
        (M4_VideoHandler_Context*)context;

    if (pStreamContext->bYuvWithEffectSet == M4OSA_TRUE) {

        p_des = pOutputPlane[0].pac_data + pOutputPlane[0].u_topleft;
        p_src = pStreamContext->pDecYuvWithEffect[0].pac_data +
         pStreamContext->pDecYuvWithEffect[0].u_topleft;

        for (height = 0; height<pOutputPlane[0].u_height; height++) {
            memcpy((void *)p_des, (void *)p_src, pOutputPlane[0].u_width);
            p_des += pOutputPlane[0].u_stride;
            p_src += pStreamContext->pDecYuvWithEffect[0].u_stride;
        }

        p_des = pOutputPlane[1].pac_data + pOutputPlane[1].u_topleft;
        p_src = pStreamContext->pDecYuvWithEffect[1].pac_data +
         pStreamContext->pDecYuvWithEffect[1].u_topleft;

        for (height = 0; height<pOutputPlane[1].u_height; height++) {
            memcpy((void *)p_des, (void *)p_src, pOutputPlane[1].u_width);
            p_des += pOutputPlane[1].u_stride;
            p_src += pStreamContext->pDecYuvWithEffect[1].u_stride;
        }

        p_des = pOutputPlane[2].pac_data + pOutputPlane[2].u_topleft;
        p_src = pStreamContext->pDecYuvWithEffect[2].pac_data +
         pStreamContext->pDecYuvWithEffect[2].u_topleft;

        for (height = 0; height<pOutputPlane[2].u_height; height++) {
            memcpy((void *)p_des, (void *)p_src, pOutputPlane[2].u_width);
            p_des += pOutputPlane[2].u_stride;
            p_src += pStreamContext->pDecYuvWithEffect[2].u_stride;
        }
    } else {

        p_des = pOutputPlane[0].pac_data + pOutputPlane[0].u_topleft;
        p_src = pStreamContext->pDecYuvData[0].pac_data +
         pStreamContext->pDecYuvData[0].u_topleft;

        for (height = 0; height<pOutputPlane[0].u_height; height++) {
            memcpy((void *)p_des, (void *)p_src, pOutputPlane[0].u_width);
            p_des += pOutputPlane[0].u_stride;
            p_src += pStreamContext->pDecYuvData[0].u_stride;
        }

        p_des = pOutputPlane[1].pac_data + pOutputPlane[1].u_topleft;
        p_src = pStreamContext->pDecYuvData[1].pac_data +
         pStreamContext->pDecYuvData[1].u_topleft;

        for (height = 0; height<pOutputPlane[1].u_height; height++) {
            memcpy((void *)p_des, (void *)p_src, pOutputPlane[1].u_width);
            p_des += pOutputPlane[1].u_stride;
            p_src += pStreamContext->pDecYuvData[1].u_stride;
        }

        p_des = pOutputPlane[2].pac_data + pOutputPlane[2].u_topleft;
        p_src = pStreamContext->pDecYuvData[2].pac_data +
         pStreamContext->pDecYuvData[2].u_topleft;

        for (height = 0; height<pOutputPlane[2].u_height; height++) {
            memcpy((void *)p_des,(void *)p_src,pOutputPlane[2].u_width);
            p_des += pOutputPlane[2].u_stride;
            p_src += pStreamContext->pDecYuvData[2].u_stride;
        }
    }
    return err;
}

/**
 ************************************************************************
 * @brief Retrieves the interface implemented by the decoder
 * @param pDecoderType        : Pointer to a M4DECODER_VideoType
 *                             (allocated by the caller)
 *                             that will be filled with the decoder type
 * @param pDecoderInterface   : Address of a pointer that will be set to
 *                              the interface implemented by this decoder.
 *                              The interface is a structure allocated by
 *                              this function and must be freed by the caller.
 *
 * @returns : M4NO_ERROR  if OK
 *            M4ERR_ALLOC if allocation failed
 ************************************************************************
*/
M4OSA_ERR M4DECODER_NULL_getInterface (M4DECODER_VideoType *pDecoderType,
                            M4DECODER_VideoInterface **pDecoderInterface) {

    *pDecoderInterface =
        (M4DECODER_VideoInterface*)M4OSA_32bitAlignedMalloc(
         sizeof(M4DECODER_VideoInterface),
         M4DECODER_MPEG4, (M4OSA_Char *)"M4DECODER_VideoInterface");

    if (M4OSA_NULL == *pDecoderInterface) {
        return M4ERR_ALLOC;
    }

    *pDecoderType = M4DECODER_kVideoTypeYUV420P;

    (*pDecoderInterface)->m_pFctCreate    = M4DECODER_NULL_create;
    (*pDecoderInterface)->m_pFctDestroy   = M4DECODER_NULL_destroy;
    (*pDecoderInterface)->m_pFctGetOption = M4DECODER_NULL_getOption;
    (*pDecoderInterface)->m_pFctSetOption = M4DECODER_NULL_setOption;
    (*pDecoderInterface)->m_pFctDecode    = M4DECODER_NULL_decode;
    (*pDecoderInterface)->m_pFctRender    = M4DECODER_NULL_render;

    return M4NO_ERROR;
}
