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
 * @file    M4xVSS_internal.c
 * @brief    Internal functions of extended Video Studio Service (Video Studio 2.1)
 * @note
 ******************************************************************************
 */
#include "M4OSA_Debug.h"
#include "M4OSA_CharStar.h"

#include "NXPSW_CompilerSwitches.h"

#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_ErrorCodes.h"

#include "M4xVSS_API.h"
#include "M4xVSS_Internal.h"

/*for rgb16 color effect*/
#include "M4VIFI_Defines.h"
#include "M4VIFI_Clip.h"

/**
 * component includes */
#include "M4VFL_transition.h"            /**< video effects */

/* Internal header file of VSS is included because of MMS use case */
#include "M4VSS3GPP_InternalTypes.h"

/*Exif header files to add image rendering support (cropping, black borders)*/
#include "M4EXIFC_CommonAPI.h"
// StageFright encoders require %16 resolution
#include "M4ENCODER_common.h"

#define TRANSPARENT_COLOR 0x7E0

/* Prototype of M4VIFI_xVSS_RGB565toYUV420 function (avoid green effect of transparency color) */
M4VIFI_UInt8 M4VIFI_xVSS_RGB565toYUV420(void *pUserData, M4VIFI_ImagePlane *pPlaneIn,
                                        M4VIFI_ImagePlane *pPlaneOut);


/*special MCS function used only in VideoArtist and VideoStudio to open the media in the normal
 mode. That way the media duration is accurate*/
extern M4OSA_ERR M4MCS_open_normalMode(M4MCS_Context pContext, M4OSA_Void* pFileIn,
                                         M4VIDEOEDITING_FileType InputFileType,
                                         M4OSA_Void* pFileOut, M4OSA_Void* pTempFile);


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalStartTranscoding(M4OSA_Context pContext)
 * @brief        This function initializes MCS (3GP transcoder) with the given
 *                parameters
 * @note        The transcoding parameters are given by the internal xVSS context.
 *                This context contains a pointer on the current element of the
 *                chained list of MCS parameters.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalStartTranscoding(M4OSA_Context pContext,
                                          M4OSA_UInt32 *rotationDegree)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;
    M4MCS_Context mcs_context;
    M4MCS_OutputParams Params;
    M4MCS_EncodingParams Rates;
    M4OSA_UInt32 i;
    M4VIDEOEDITING_ClipProperties clipProps;

    err = M4MCS_init(&mcs_context, xVSS_context->pFileReadPtr, xVSS_context->pFileWritePtr);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_init: 0x%x", err);
        return err;
    }

    err = M4MCS_open(mcs_context, xVSS_context->pMCScurrentParams->pFileIn,
         xVSS_context->pMCScurrentParams->InputFileType,
             xVSS_context->pMCScurrentParams->pFileOut,
             xVSS_context->pMCScurrentParams->pFileTemp);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_open: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    /** Get the clip properties
     */
    err = M4MCS_getInputFileProperties(mcs_context, &clipProps);
    if (err != M4NO_ERROR) {
        M4OSA_TRACE1_1("Error in M4MCS_getInputFileProperties: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }
    *rotationDegree = clipProps.videoRotationDegrees;

    /**
     * Fill MCS parameters with the parameters contained in the current element of the
       MCS parameters chained list */
    Params.OutputFileType = xVSS_context->pMCScurrentParams->OutputFileType;
    Params.OutputVideoFormat = xVSS_context->pMCScurrentParams->OutputVideoFormat;
    Params.outputVideoProfile= xVSS_context->pMCScurrentParams->outputVideoProfile;
    Params.outputVideoLevel = xVSS_context->pMCScurrentParams->outputVideoLevel;
    Params.OutputVideoFrameSize = xVSS_context->pMCScurrentParams->OutputVideoFrameSize;
    Params.OutputVideoFrameRate = xVSS_context->pMCScurrentParams->OutputVideoFrameRate;
    Params.OutputAudioFormat = xVSS_context->pMCScurrentParams->OutputAudioFormat;
    Params.OutputAudioSamplingFrequency =
         xVSS_context->pMCScurrentParams->OutputAudioSamplingFrequency;
    Params.bAudioMono = xVSS_context->pMCScurrentParams->bAudioMono;
    Params.pOutputPCMfile = M4OSA_NULL;
    /*FB 2008/10/20: add media rendering parameter to keep aspect ratio*/
    switch(xVSS_context->pMCScurrentParams->MediaRendering)
    {
    case M4xVSS_kResizing:
        Params.MediaRendering = M4MCS_kResizing;
        break;
    case M4xVSS_kCropping:
        Params.MediaRendering = M4MCS_kCropping;
        break;
    case M4xVSS_kBlackBorders:
        Params.MediaRendering = M4MCS_kBlackBorders;
        break;
    default:
        break;
    }
    /**/
    // new params after integrating MCS 2.0
    // Set the number of audio effects; 0 for now.
    Params.nbEffects = 0;

    // Set the audio effect; null for now.
    Params.pEffects = NULL;

    // Set the audio effect; null for now.
    Params.bDiscardExif = M4OSA_FALSE;

    // Set the audio effect; null for now.
    Params.bAdjustOrientation = M4OSA_FALSE;
    // new params after integrating MCS 2.0

    /**
     * Set output parameters */
    err = M4MCS_setOutputParams(mcs_context, &Params);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_setOutputParams: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    Rates.OutputVideoBitrate = xVSS_context->pMCScurrentParams->OutputVideoBitrate;
    Rates.OutputAudioBitrate = xVSS_context->pMCScurrentParams->OutputAudioBitrate;
    Rates.BeginCutTime = 0;
    Rates.EndCutTime = 0;
    Rates.OutputFileSize = 0;

    /*FB: transcoding per parts*/
    Rates.BeginCutTime = xVSS_context->pMCScurrentParams->BeginCutTime;
    Rates.EndCutTime = xVSS_context->pMCScurrentParams->EndCutTime;
    Rates.OutputVideoTimescale = xVSS_context->pMCScurrentParams->OutputVideoTimescale;

    err = M4MCS_setEncodingParams(mcs_context, &Rates);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_setEncodingParams: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    err = M4MCS_checkParamsAndStart(mcs_context);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_checkParamsAndStart: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    /**
     * Save MCS context to be able to call MCS step function in M4xVSS_step function */
    xVSS_context->pMCS_Ctxt = mcs_context;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalStopTranscoding(M4OSA_Context pContext)
 * @brief        This function cleans up MCS (3GP transcoder)
 * @note
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalStopTranscoding(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;

    err = M4MCS_close(xVSS_context->pMCS_Ctxt);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalStopTranscoding: Error in M4MCS_close: 0x%x", err);
        M4MCS_abort(xVSS_context->pMCS_Ctxt);
        return err;
    }

    /**
     * Free this MCS instance */
    err = M4MCS_cleanUp(xVSS_context->pMCS_Ctxt);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalStopTranscoding: Error in M4MCS_cleanUp: 0x%x", err);
        return err;
    }

    xVSS_context->pMCS_Ctxt = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_internalConvertAndResizeARGB8888toYUV420(M4OSA_Void* pFileIn,
 *                                             M4OSA_FileReadPointer* pFileReadPtr,
 *                                                M4VIFI_ImagePlane* pImagePlanes,
 *                                                 M4OSA_UInt32 width,
 *                                                M4OSA_UInt32 height);
 * @brief    It Coverts and resizes a ARGB8888 image to YUV420
 * @note
 * @param    pFileIn            (IN) The Image input file
 * @param    pFileReadPtr    (IN) Pointer on filesystem functions
 * @param    pImagePlanes    (IN/OUT) Pointer on YUV420 output planes allocated by the user
 *                            ARGB8888 image  will be converted and resized  to output
 *                             YUV420 plane size
 *@param    width        (IN) width of the ARGB8888
 *@param    height            (IN) height of the ARGB8888
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_ALLOC: memory error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */

M4OSA_ERR M4xVSS_internalConvertAndResizeARGB8888toYUV420(M4OSA_Void* pFileIn,
                                                          M4OSA_FileReadPointer* pFileReadPtr,
                                                          M4VIFI_ImagePlane* pImagePlanes,
                                                          M4OSA_UInt32 width,M4OSA_UInt32 height)
{
    M4OSA_Context pARGBIn;
    M4VIFI_ImagePlane rgbPlane1 ,rgbPlane2;
    M4OSA_UInt32 frameSize_argb=(width * height * 4);
    M4OSA_UInt32 frameSize = (width * height * 3); //Size of RGB888 data.
    M4OSA_UInt32 i = 0,j= 0;
    M4OSA_ERR err=M4NO_ERROR;


    M4OSA_UInt8 *pTmpData = (M4OSA_UInt8*) M4OSA_32bitAlignedMalloc(frameSize_argb,
         M4VS, (M4OSA_Char*)"Image argb data");
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 Entering :");
    if(pTmpData == M4OSA_NULL) {
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 :\
            Failed to allocate memory for Image clip");
        return M4ERR_ALLOC;
    }

    M4OSA_TRACE1_2("M4xVSS_internalConvertAndResizeARGB8888toYUV420 :width and height %d %d",
        width ,height);
    /* Get file size (mandatory for chunk decoding) */
    err = pFileReadPtr->openRead(&pARGBIn, pFileIn, M4OSA_kFileRead);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertAndResizeARGB8888toYUV420 :\
            Can't open input ARGB8888 file %s, error: 0x%x\n",pFileIn, err);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        goto cleanup;
    }

    err = pFileReadPtr->readData(pARGBIn,(M4OSA_MemAddr8)pTmpData, &frameSize_argb);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertAndResizeARGB8888toYUV420 Can't close ARGB8888\
             file %s, error: 0x%x\n",pFileIn, err);
        pFileReadPtr->closeRead(pARGBIn);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        goto cleanup;
    }

    err = pFileReadPtr->closeRead(pARGBIn);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertAndResizeARGB8888toYUV420 Can't close ARGB8888 \
             file %s, error: 0x%x\n",pFileIn, err);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        goto cleanup;
    }

    rgbPlane1.pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(frameSize, M4VS,
         (M4OSA_Char*)"Image clip RGB888 data");
    if(rgbPlane1.pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 \
            Failed to allocate memory for Image clip");
        free(pTmpData);
        return M4ERR_ALLOC;
    }

        rgbPlane1.u_height = height;
        rgbPlane1.u_width = width;
        rgbPlane1.u_stride = width*3;
        rgbPlane1.u_topleft = 0;


    /** Remove the alpha channel */
    for (i=0, j = 0; i < frameSize_argb; i++) {
        if ((i % 4) == 0) continue;
        rgbPlane1.pac_data[j] = pTmpData[i];
        j++;
    }
        free(pTmpData);

    /* To Check if resizing is required with color conversion */
    if(width != pImagePlanes->u_width || height != pImagePlanes->u_height)
    {
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 Resizing :");
        frameSize =  ( pImagePlanes->u_width * pImagePlanes->u_height * 3);
        rgbPlane2.pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(frameSize, M4VS,
             (M4OSA_Char*)"Image clip RGB888 data");
        if(rgbPlane2.pac_data == M4OSA_NULL)
        {
            M4OSA_TRACE1_0("Failed to allocate memory for Image clip");
            free(pTmpData);
            return M4ERR_ALLOC;
        }
            rgbPlane2.u_height =  pImagePlanes->u_height;
            rgbPlane2.u_width = pImagePlanes->u_width;
            rgbPlane2.u_stride = pImagePlanes->u_width*3;
            rgbPlane2.u_topleft = 0;

        /* Resizing RGB888 to RGB888 */
        err = M4VIFI_ResizeBilinearRGB888toRGB888(M4OSA_NULL, &rgbPlane1, &rgbPlane2);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("error when converting from Resize RGB888 to RGB888: 0x%x\n", err);
            free(rgbPlane2.pac_data);
            free(rgbPlane1.pac_data);
            return err;
        }
        /*Converting Resized RGB888 to YUV420 */
        err = M4VIFI_RGB888toYUV420(M4OSA_NULL, &rgbPlane2, pImagePlanes);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("error when converting from RGB888 to YUV: 0x%x\n", err);
            free(rgbPlane2.pac_data);
            free(rgbPlane1.pac_data);
            return err;
        }
            free(rgbPlane2.pac_data);
            free(rgbPlane1.pac_data);

            M4OSA_TRACE1_0("RGB to YUV done");


    }
    else
    {
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 NO  Resizing :");
        err = M4VIFI_RGB888toYUV420(M4OSA_NULL, &rgbPlane1, pImagePlanes);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("error when converting from RGB to YUV: 0x%x\n", err);
        }
            free(rgbPlane1.pac_data);

            M4OSA_TRACE1_0("RGB to YUV done");
    }
cleanup:
    M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 leaving :");
    return err;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_internalConvertARGB8888toYUV420(M4OSA_Void* pFileIn,
 *                                             M4OSA_FileReadPointer* pFileReadPtr,
 *                                                M4VIFI_ImagePlane* pImagePlanes,
 *                                                 M4OSA_UInt32 width,
 *                                                M4OSA_UInt32 height);
 * @brief    It Coverts a ARGB8888 image to YUV420
 * @note
 * @param    pFileIn            (IN) The Image input file
 * @param    pFileReadPtr    (IN) Pointer on filesystem functions
 * @param    pImagePlanes    (IN/OUT) Pointer on YUV420 output planes allocated by the user
 *                            ARGB8888 image  will be converted and resized  to output
 *                            YUV420 plane size
 * @param    width        (IN) width of the ARGB8888
 * @param    height            (IN) height of the ARGB8888
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_ALLOC: memory error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */

M4OSA_ERR M4xVSS_internalConvertARGB8888toYUV420(M4OSA_Void* pFileIn,
                                                 M4OSA_FileReadPointer* pFileReadPtr,
                                                 M4VIFI_ImagePlane** pImagePlanes,
                                                 M4OSA_UInt32 width,M4OSA_UInt32 height)
{
    M4OSA_ERR err = M4NO_ERROR;
    M4VIFI_ImagePlane *yuvPlane = M4OSA_NULL;

    yuvPlane = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane),
                M4VS, (M4OSA_Char*)"M4xVSS_internalConvertRGBtoYUV: Output plane YUV");
    if(yuvPlane == M4OSA_NULL) {
        M4OSA_TRACE1_0("M4xVSS_internalConvertAndResizeARGB8888toYUV420 :\
            Failed to allocate memory for Image clip");
        return M4ERR_ALLOC;
    }
    yuvPlane[0].u_height = height;
    yuvPlane[0].u_width = width;
    yuvPlane[0].u_stride = width;
    yuvPlane[0].u_topleft = 0;
    yuvPlane[0].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(yuvPlane[0].u_height \
        * yuvPlane[0].u_width * 1.5, M4VS, (M4OSA_Char*)"imageClip YUV data");

    yuvPlane[1].u_height = yuvPlane[0].u_height >>1;
    yuvPlane[1].u_width = yuvPlane[0].u_width >> 1;
    yuvPlane[1].u_stride = yuvPlane[1].u_width;
    yuvPlane[1].u_topleft = 0;
    yuvPlane[1].pac_data = (M4VIFI_UInt8*)(yuvPlane[0].pac_data + yuvPlane[0].u_height \
        * yuvPlane[0].u_width);

    yuvPlane[2].u_height = yuvPlane[0].u_height >>1;
    yuvPlane[2].u_width = yuvPlane[0].u_width >> 1;
    yuvPlane[2].u_stride = yuvPlane[2].u_width;
    yuvPlane[2].u_topleft = 0;
    yuvPlane[2].pac_data = (M4VIFI_UInt8*)(yuvPlane[1].pac_data + yuvPlane[1].u_height \
        * yuvPlane[1].u_width);
    err = M4xVSS_internalConvertAndResizeARGB8888toYUV420( pFileIn,pFileReadPtr,
                                                          yuvPlane, width, height);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalConvertAndResizeARGB8888toYUV420 return error: 0x%x\n", err);
        free(yuvPlane);
        return err;
    }

        *pImagePlanes = yuvPlane;

    M4OSA_TRACE1_0("M4xVSS_internalConvertARGB8888toYUV420 :Leaving");
    return err;

}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_PictureCallbackFct (M4OSA_Void* pPictureCtxt,
 *                                        M4VIFI_ImagePlane* pImagePlanes,
 *                                        M4OSA_UInt32* pPictureDuration);
 * @brief    It feeds the PTO3GPP with YUV420 pictures.
 * @note    This function is given to the PTO3GPP in the M4PTO3GPP_Params structure
 * @param    pContext    (IN) The integrator own context
 * @param    pImagePlanes(IN/OUT) Pointer to an array of three valid image planes
 * @param    pPictureDuration(OUT) Duration of the returned picture
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4PTO3GPP_WAR_LAST_PICTURE: The returned image is the last one
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_PictureCallbackFct(M4OSA_Void* pPictureCtxt, M4VIFI_ImagePlane* pImagePlanes,
                                     M4OSA_Double* pPictureDuration)
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_UInt8    last_frame_flag = 0;
    M4xVSS_PictureCallbackCtxt* pC = (M4xVSS_PictureCallbackCtxt*) (pPictureCtxt);

    /*Used for pan&zoom*/
    M4OSA_UInt8 tempPanzoomXa = 0;
    M4OSA_UInt8 tempPanzoomXb = 0;
    M4AIR_Params Params;
    /**/

    /*Used for cropping and black borders*/
    M4OSA_Context    pPictureContext = M4OSA_NULL;
    M4OSA_FilePosition    pictureSize = 0 ;
    M4OSA_UInt8*    pictureBuffer = M4OSA_NULL;
    //M4EXIFC_Context pExifContext = M4OSA_NULL;
    M4EXIFC_BasicTags pBasicTags;
    M4VIFI_ImagePlane pImagePlanes1 = pImagePlanes[0];
    M4VIFI_ImagePlane pImagePlanes2 = pImagePlanes[1];
    M4VIFI_ImagePlane pImagePlanes3 = pImagePlanes[2];
    /**/

    /**
     * Check input parameters */
    M4OSA_DEBUG_IF2((M4OSA_NULL==pPictureCtxt),        M4ERR_PARAMETER,
         "M4xVSS_PictureCallbackFct: pPictureCtxt is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL==pImagePlanes),        M4ERR_PARAMETER,
         "M4xVSS_PictureCallbackFct: pImagePlanes is M4OSA_NULL");
    M4OSA_DEBUG_IF2((M4OSA_NULL==pPictureDuration), M4ERR_PARAMETER,
         "M4xVSS_PictureCallbackFct: pPictureDuration is M4OSA_NULL");
    M4OSA_TRACE1_0("M4xVSS_PictureCallbackFct :Entering");
    /*PR P4ME00003181 In case the image number is 0, pan&zoom can not be used*/
    if(M4OSA_TRUE == pC->m_pPto3GPPparams->isPanZoom && pC->m_NbImage == 0)
    {
        pC->m_pPto3GPPparams->isPanZoom = M4OSA_FALSE;
    }

    /*If no cropping/black borders or pan&zoom, just decode and resize the picture*/
    if(pC->m_mediaRendering == M4xVSS_kResizing && M4OSA_FALSE == pC->m_pPto3GPPparams->isPanZoom)
    {
        /**
         * Convert and resize input ARGB8888 file to YUV420 */
        /*To support ARGB8888 : */
        M4OSA_TRACE1_2("M4xVSS_PictureCallbackFct 1: width and heght %d %d",
            pC->m_pPto3GPPparams->width,pC->m_pPto3GPPparams->height);
        err = M4xVSS_internalConvertAndResizeARGB8888toYUV420(pC->m_FileIn,
             pC->m_pFileReadPtr, pImagePlanes,pC->m_pPto3GPPparams->width,
                pC->m_pPto3GPPparams->height);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct: Error when decoding JPEG: 0x%x\n", err);
            return err;
        }
    }
    /*In case of cropping, black borders or pan&zoom, call the EXIF reader and the AIR*/
    else
    {
        /**
         * Computes ratios */
        if(pC->m_pDecodedPlane == M4OSA_NULL)
        {
            /**
             * Convert input ARGB8888 file to YUV420 */
             M4OSA_TRACE1_2("M4xVSS_PictureCallbackFct 2: width and heght %d %d",
                pC->m_pPto3GPPparams->width,pC->m_pPto3GPPparams->height);
            err = M4xVSS_internalConvertARGB8888toYUV420(pC->m_FileIn, pC->m_pFileReadPtr,
                &(pC->m_pDecodedPlane),pC->m_pPto3GPPparams->width,pC->m_pPto3GPPparams->height);
            if(err != M4NO_ERROR)
            {
                M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct: Error when decoding JPEG: 0x%x\n", err);
                if(pC->m_pDecodedPlane != M4OSA_NULL)
                {
                    /* YUV420 planar is returned but allocation is made only once
                        (contigous planes in memory) */
                    if(pC->m_pDecodedPlane->pac_data != M4OSA_NULL)
                    {
                        free(pC->m_pDecodedPlane->pac_data);
                    }
                    free(pC->m_pDecodedPlane);
                    pC->m_pDecodedPlane = M4OSA_NULL;
                }
                return err;
            }
        }

        /*Initialize AIR Params*/
        Params.m_inputCoord.m_x = 0;
        Params.m_inputCoord.m_y = 0;
        Params.m_inputSize.m_height = pC->m_pDecodedPlane->u_height;
        Params.m_inputSize.m_width = pC->m_pDecodedPlane->u_width;
        Params.m_outputSize.m_width = pImagePlanes->u_width;
        Params.m_outputSize.m_height = pImagePlanes->u_height;
        Params.m_bOutputStripe = M4OSA_FALSE;
        Params.m_outputOrientation = M4COMMON_kOrientationTopLeft;

        /*Initialize Exif params structure*/
        pBasicTags.orientation = M4COMMON_kOrientationUnknown;

        /**
        Pan&zoom params*/
        if(M4OSA_TRUE == pC->m_pPto3GPPparams->isPanZoom)
        {
            /*Save ratio values, they can be reused if the new ratios are 0*/
            tempPanzoomXa = (M4OSA_UInt8)pC->m_pPto3GPPparams->PanZoomXa;
            tempPanzoomXb = (M4OSA_UInt8)pC->m_pPto3GPPparams->PanZoomXb;
            /*Check that the ratio is not 0*/
            /*Check (a) parameters*/
            if(pC->m_pPto3GPPparams->PanZoomXa == 0)
            {
                M4OSA_UInt8 maxRatio = 0;
                if(pC->m_pPto3GPPparams->PanZoomTopleftXa >=
                     pC->m_pPto3GPPparams->PanZoomTopleftYa)
                {
                    /*The ratio is 0, that means the area of the picture defined with (a)
                    parameters is bigger than the image size*/
                    if(pC->m_pPto3GPPparams->PanZoomTopleftXa + tempPanzoomXa > 1000)
                    {
                        /*The oversize is maxRatio*/
                        maxRatio = pC->m_pPto3GPPparams->PanZoomTopleftXa + tempPanzoomXa - 1000;
                    }
                }
                else
                {
                    /*The ratio is 0, that means the area of the picture defined with (a)
                     parameters is bigger than the image size*/
                    if(pC->m_pPto3GPPparams->PanZoomTopleftYa + tempPanzoomXa > 1000)
                    {
                        /*The oversize is maxRatio*/
                        maxRatio = pC->m_pPto3GPPparams->PanZoomTopleftYa + tempPanzoomXa - 1000;
                    }
                }
                /*Modify the (a) parameters:*/
                if(pC->m_pPto3GPPparams->PanZoomTopleftXa >= maxRatio)
                {
                    /*The (a) topleft parameters can be moved to keep the same area size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftXa -= maxRatio;
                }
                else
                {
                    /*Move the (a) topleft parameter to 0 but the ratio will be also further
                    modified to match the image size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftXa = 0;
                }
                if(pC->m_pPto3GPPparams->PanZoomTopleftYa >= maxRatio)
                {
                    /*The (a) topleft parameters can be moved to keep the same area size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftYa -= maxRatio;
                }
                else
                {
                    /*Move the (a) topleft parameter to 0 but the ratio will be also further
                     modified to match the image size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftYa = 0;
                }
                /*The new ratio is the original one*/
                pC->m_pPto3GPPparams->PanZoomXa = tempPanzoomXa;
                if(pC->m_pPto3GPPparams->PanZoomXa + pC->m_pPto3GPPparams->PanZoomTopleftXa > 1000)
                {
                    /*Change the ratio if the area of the picture defined with (a) parameters is
                    bigger than the image size*/
                    pC->m_pPto3GPPparams->PanZoomXa = 1000 - pC->m_pPto3GPPparams->PanZoomTopleftXa;
                }
                if(pC->m_pPto3GPPparams->PanZoomXa + pC->m_pPto3GPPparams->PanZoomTopleftYa > 1000)
                {
                    /*Change the ratio if the area of the picture defined with (a) parameters is
                    bigger than the image size*/
                    pC->m_pPto3GPPparams->PanZoomXa = 1000 - pC->m_pPto3GPPparams->PanZoomTopleftYa;
                }
            }
            /*Check (b) parameters*/
            if(pC->m_pPto3GPPparams->PanZoomXb == 0)
            {
                M4OSA_UInt8 maxRatio = 0;
                if(pC->m_pPto3GPPparams->PanZoomTopleftXb >=
                     pC->m_pPto3GPPparams->PanZoomTopleftYb)
                {
                    /*The ratio is 0, that means the area of the picture defined with (b)
                     parameters is bigger than the image size*/
                    if(pC->m_pPto3GPPparams->PanZoomTopleftXb + tempPanzoomXb > 1000)
                    {
                        /*The oversize is maxRatio*/
                        maxRatio = pC->m_pPto3GPPparams->PanZoomTopleftXb + tempPanzoomXb - 1000;
                    }
                }
                else
                {
                    /*The ratio is 0, that means the area of the picture defined with (b)
                     parameters is bigger than the image size*/
                    if(pC->m_pPto3GPPparams->PanZoomTopleftYb + tempPanzoomXb > 1000)
                    {
                        /*The oversize is maxRatio*/
                        maxRatio = pC->m_pPto3GPPparams->PanZoomTopleftYb + tempPanzoomXb - 1000;
                    }
                }
                /*Modify the (b) parameters:*/
                if(pC->m_pPto3GPPparams->PanZoomTopleftXb >= maxRatio)
                {
                    /*The (b) topleft parameters can be moved to keep the same area size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftXb -= maxRatio;
                }
                else
                {
                    /*Move the (b) topleft parameter to 0 but the ratio will be also further
                     modified to match the image size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftXb = 0;
                }
                if(pC->m_pPto3GPPparams->PanZoomTopleftYb >= maxRatio)
                {
                    /*The (b) topleft parameters can be moved to keep the same area size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftYb -= maxRatio;
                }
                else
                {
                    /*Move the (b) topleft parameter to 0 but the ratio will be also further
                    modified to match the image size*/
                    pC->m_pPto3GPPparams->PanZoomTopleftYb = 0;
                }
                /*The new ratio is the original one*/
                pC->m_pPto3GPPparams->PanZoomXb = tempPanzoomXb;
                if(pC->m_pPto3GPPparams->PanZoomXb + pC->m_pPto3GPPparams->PanZoomTopleftXb > 1000)
                {
                    /*Change the ratio if the area of the picture defined with (b) parameters is
                    bigger than the image size*/
                    pC->m_pPto3GPPparams->PanZoomXb = 1000 - pC->m_pPto3GPPparams->PanZoomTopleftXb;
                }
                if(pC->m_pPto3GPPparams->PanZoomXb + pC->m_pPto3GPPparams->PanZoomTopleftYb > 1000)
                {
                    /*Change the ratio if the area of the picture defined with (b) parameters is
                    bigger than the image size*/
                    pC->m_pPto3GPPparams->PanZoomXb = 1000 - pC->m_pPto3GPPparams->PanZoomTopleftYb;
                }
            }

            /**
             * Computes AIR parameters */
/*        Params.m_inputCoord.m_x = (M4OSA_UInt32)(pC->m_pDecodedPlane->u_width *
            (pC->m_pPto3GPPparams->PanZoomTopleftXa +
            (M4OSA_Int16)((pC->m_pPto3GPPparams->PanZoomTopleftXb \
                - pC->m_pPto3GPPparams->PanZoomTopleftXa) *
            pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage)) / 100;
        Params.m_inputCoord.m_y = (M4OSA_UInt32)(pC->m_pDecodedPlane->u_height *
            (pC->m_pPto3GPPparams->PanZoomTopleftYa +
            (M4OSA_Int16)((pC->m_pPto3GPPparams->PanZoomTopleftYb\
                 - pC->m_pPto3GPPparams->PanZoomTopleftYa) *
            pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage)) / 100;

        Params.m_inputSize.m_width = (M4OSA_UInt32)(pC->m_pDecodedPlane->u_width *
            (pC->m_pPto3GPPparams->PanZoomXa +
            (M4OSA_Int16)((pC->m_pPto3GPPparams->PanZoomXb - pC->m_pPto3GPPparams->PanZoomXa) *
            pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage)) / 100;

        Params.m_inputSize.m_height =  (M4OSA_UInt32)(pC->m_pDecodedPlane->u_height *
            (pC->m_pPto3GPPparams->PanZoomXa +
            (M4OSA_Int16)((pC->m_pPto3GPPparams->PanZoomXb - pC->m_pPto3GPPparams->PanZoomXa) *
            pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage)) / 100;
 */
            // Instead of using pC->m_NbImage we have to use (pC->m_NbImage-1) as pC->m_ImageCounter
            // will be x-1 max for x no. of frames
            Params.m_inputCoord.m_x = (M4OSA_UInt32)((((M4OSA_Double)pC->m_pDecodedPlane->u_width *
                (pC->m_pPto3GPPparams->PanZoomTopleftXa +
                (M4OSA_Double)((M4OSA_Double)(pC->m_pPto3GPPparams->PanZoomTopleftXb\
                     - pC->m_pPto3GPPparams->PanZoomTopleftXa) *
                pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage-1)) / 1000));
            Params.m_inputCoord.m_y =
                 (M4OSA_UInt32)((((M4OSA_Double)pC->m_pDecodedPlane->u_height *
                (pC->m_pPto3GPPparams->PanZoomTopleftYa +
                (M4OSA_Double)((M4OSA_Double)(pC->m_pPto3GPPparams->PanZoomTopleftYb\
                     - pC->m_pPto3GPPparams->PanZoomTopleftYa) *
                pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage-1)) / 1000));

            Params.m_inputSize.m_width =
                 (M4OSA_UInt32)((((M4OSA_Double)pC->m_pDecodedPlane->u_width *
                (pC->m_pPto3GPPparams->PanZoomXa +
                (M4OSA_Double)((M4OSA_Double)(pC->m_pPto3GPPparams->PanZoomXb\
                     - pC->m_pPto3GPPparams->PanZoomXa) *
                pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage-1)) / 1000));

            Params.m_inputSize.m_height =
                 (M4OSA_UInt32)((((M4OSA_Double)pC->m_pDecodedPlane->u_height *
                (pC->m_pPto3GPPparams->PanZoomXa +
                (M4OSA_Double)((M4OSA_Double)(pC->m_pPto3GPPparams->PanZoomXb \
                    - pC->m_pPto3GPPparams->PanZoomXa) *
                pC->m_ImageCounter) / (M4OSA_Double)pC->m_NbImage-1)) / 1000));

            if((Params.m_inputSize.m_width + Params.m_inputCoord.m_x)\
                 > pC->m_pDecodedPlane->u_width)
            {
                Params.m_inputSize.m_width = pC->m_pDecodedPlane->u_width \
                    - Params.m_inputCoord.m_x;
            }

            if((Params.m_inputSize.m_height + Params.m_inputCoord.m_y)\
                 > pC->m_pDecodedPlane->u_height)
            {
                Params.m_inputSize.m_height = pC->m_pDecodedPlane->u_height\
                     - Params.m_inputCoord.m_y;
            }



            Params.m_inputSize.m_width = (Params.m_inputSize.m_width>>1)<<1;
            Params.m_inputSize.m_height = (Params.m_inputSize.m_height>>1)<<1;
        }



    /**
        Picture rendering: Black borders*/

        if(pC->m_mediaRendering == M4xVSS_kBlackBorders)
        {
            memset((void *)pImagePlanes[0].pac_data,Y_PLANE_BORDER_VALUE,
                (pImagePlanes[0].u_height*pImagePlanes[0].u_stride));
            memset((void *)pImagePlanes[1].pac_data,U_PLANE_BORDER_VALUE,
                (pImagePlanes[1].u_height*pImagePlanes[1].u_stride));
            memset((void *)pImagePlanes[2].pac_data,V_PLANE_BORDER_VALUE,
                (pImagePlanes[2].u_height*pImagePlanes[2].u_stride));

            /**
            First without pan&zoom*/
            if(M4OSA_FALSE == pC->m_pPto3GPPparams->isPanZoom)
            {
                switch(pBasicTags.orientation)
                {
                default:
                case M4COMMON_kOrientationUnknown:
                    Params.m_outputOrientation = M4COMMON_kOrientationTopLeft;
                case M4COMMON_kOrientationTopLeft:
                case M4COMMON_kOrientationTopRight:
                case M4COMMON_kOrientationBottomRight:
                case M4COMMON_kOrientationBottomLeft:
                    if((M4OSA_UInt32)((pC->m_pDecodedPlane->u_height * pImagePlanes->u_width)\
                         /pC->m_pDecodedPlane->u_width) <= pImagePlanes->u_height)
                         //Params.m_inputSize.m_height < Params.m_inputSize.m_width)
                    {
                        /*it is height so black borders will be on the top and on the bottom side*/
                        Params.m_outputSize.m_width = pImagePlanes->u_width;
                        Params.m_outputSize.m_height =
                             (M4OSA_UInt32)((pC->m_pDecodedPlane->u_height \
                                * pImagePlanes->u_width) /pC->m_pDecodedPlane->u_width);
                        /*number of lines at the top*/
                        pImagePlanes[0].u_topleft =
                            (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_height\
                                -Params.m_outputSize.m_height)>>1))*pImagePlanes[0].u_stride;
                        pImagePlanes[0].u_height = Params.m_outputSize.m_height;
                        pImagePlanes[1].u_topleft =
                             (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_height\
                                -(Params.m_outputSize.m_height>>1)))>>1)*pImagePlanes[1].u_stride;
                        pImagePlanes[1].u_height = Params.m_outputSize.m_height>>1;
                        pImagePlanes[2].u_topleft =
                             (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_height\
                                -(Params.m_outputSize.m_height>>1)))>>1)*pImagePlanes[2].u_stride;
                        pImagePlanes[2].u_height = Params.m_outputSize.m_height>>1;
                    }
                    else
                    {
                        /*it is width so black borders will be on the left and right side*/
                        Params.m_outputSize.m_height = pImagePlanes->u_height;
                        Params.m_outputSize.m_width =
                             (M4OSA_UInt32)((pC->m_pDecodedPlane->u_width \
                                * pImagePlanes->u_height) /pC->m_pDecodedPlane->u_height);

                        pImagePlanes[0].u_topleft =
                            (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_width\
                                -Params.m_outputSize.m_width)>>1));
                        pImagePlanes[0].u_width = Params.m_outputSize.m_width;
                        pImagePlanes[1].u_topleft =
                             (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_width\
                                -(Params.m_outputSize.m_width>>1)))>>1);
                        pImagePlanes[1].u_width = Params.m_outputSize.m_width>>1;
                        pImagePlanes[2].u_topleft =
                             (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_width\
                                -(Params.m_outputSize.m_width>>1)))>>1);
                        pImagePlanes[2].u_width = Params.m_outputSize.m_width>>1;
                    }
                    break;
                case M4COMMON_kOrientationLeftTop:
                case M4COMMON_kOrientationLeftBottom:
                case M4COMMON_kOrientationRightTop:
                case M4COMMON_kOrientationRightBottom:
                        if((M4OSA_UInt32)((pC->m_pDecodedPlane->u_width * pImagePlanes->u_width)\
                             /pC->m_pDecodedPlane->u_height) < pImagePlanes->u_height)
                             //Params.m_inputSize.m_height > Params.m_inputSize.m_width)
                        {
                            /*it is height so black borders will be on the top and on
                             the bottom side*/
                            Params.m_outputSize.m_height = pImagePlanes->u_width;
                            Params.m_outputSize.m_width =
                                 (M4OSA_UInt32)((pC->m_pDecodedPlane->u_width \
                                    * pImagePlanes->u_width) /pC->m_pDecodedPlane->u_height);
                            /*number of lines at the top*/
                            pImagePlanes[0].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_height\
                                    -Params.m_outputSize.m_width))>>1)*pImagePlanes[0].u_stride)+1;
                            pImagePlanes[0].u_height = Params.m_outputSize.m_width;
                            pImagePlanes[1].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_height\
                                    -(Params.m_outputSize.m_width>>1)))>>1)\
                                        *pImagePlanes[1].u_stride)+1;
                            pImagePlanes[1].u_height = Params.m_outputSize.m_width>>1;
                            pImagePlanes[2].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_height\
                                    -(Params.m_outputSize.m_width>>1)))>>1)\
                                        *pImagePlanes[2].u_stride)+1;
                            pImagePlanes[2].u_height = Params.m_outputSize.m_width>>1;
                        }
                        else
                        {
                            /*it is width so black borders will be on the left and right side*/
                            Params.m_outputSize.m_width = pImagePlanes->u_height;
                            Params.m_outputSize.m_height =
                                 (M4OSA_UInt32)((pC->m_pDecodedPlane->u_height\
                                     * pImagePlanes->u_height) /pC->m_pDecodedPlane->u_width);

                            pImagePlanes[0].u_topleft =
                                 ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_width\
                                    -Params.m_outputSize.m_height))>>1))+1;
                            pImagePlanes[0].u_width = Params.m_outputSize.m_height;
                            pImagePlanes[1].u_topleft =
                                 ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_width\
                                    -(Params.m_outputSize.m_height>>1)))>>1))+1;
                            pImagePlanes[1].u_width = Params.m_outputSize.m_height>>1;
                            pImagePlanes[2].u_topleft =
                                 ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_width\
                                    -(Params.m_outputSize.m_height>>1)))>>1))+1;
                            pImagePlanes[2].u_width = Params.m_outputSize.m_height>>1;
                        }
                    break;
                }
            }

            /**
            Secondly with pan&zoom*/
            else
            {
                switch(pBasicTags.orientation)
                {
                default:
                case M4COMMON_kOrientationUnknown:
                    Params.m_outputOrientation = M4COMMON_kOrientationTopLeft;
                case M4COMMON_kOrientationTopLeft:
                case M4COMMON_kOrientationTopRight:
                case M4COMMON_kOrientationBottomRight:
                case M4COMMON_kOrientationBottomLeft:
                    /*NO ROTATION*/
                    if((M4OSA_UInt32)((pC->m_pDecodedPlane->u_height * pImagePlanes->u_width)\
                         /pC->m_pDecodedPlane->u_width) <= pImagePlanes->u_height)
                            //Params.m_inputSize.m_height < Params.m_inputSize.m_width)
                    {
                        /*Black borders will be on the top and bottom of the output video*/
                        /*Maximum output height if the input image aspect ratio is kept and if
                        the output width is the screen width*/
                        M4OSA_UInt32 tempOutputSizeHeight =
                            (M4OSA_UInt32)((pC->m_pDecodedPlane->u_height\
                                 * pImagePlanes->u_width) /pC->m_pDecodedPlane->u_width);
                        M4OSA_UInt32 tempInputSizeHeightMax = 0;
                        M4OSA_UInt32 tempFinalInputHeight = 0;
                        /*The output width is the screen width*/
                        Params.m_outputSize.m_width = pImagePlanes->u_width;
                        tempOutputSizeHeight = (tempOutputSizeHeight>>1)<<1;

                        /*Maximum input height according to the maximum output height
                        (proportional to the maximum output height)*/
                        tempInputSizeHeightMax = (pImagePlanes->u_height\
                            *Params.m_inputSize.m_height)/tempOutputSizeHeight;
                        tempInputSizeHeightMax = (tempInputSizeHeightMax>>1)<<1;

                        /*Check if the maximum possible input height is contained into the
                        input image height*/
                        if(tempInputSizeHeightMax <= pC->m_pDecodedPlane->u_height)
                        {
                            /*The maximum possible input height is contained in the input
                            image height,
                            that means no black borders, the input pan zoom area will be extended
                            so that the input AIR height will be the maximum possible*/
                            if(((tempInputSizeHeightMax - Params.m_inputSize.m_height)>>1)\
                                 <= Params.m_inputCoord.m_y
                                && ((tempInputSizeHeightMax - Params.m_inputSize.m_height)>>1)\
                                     <= pC->m_pDecodedPlane->u_height -(Params.m_inputCoord.m_y\
                                         + Params.m_inputSize.m_height))
                            {
                                /*The input pan zoom area can be extended symmetrically on the
                                top and bottom side*/
                                Params.m_inputCoord.m_y -= ((tempInputSizeHeightMax \
                                    - Params.m_inputSize.m_height)>>1);
                            }
                            else if(Params.m_inputCoord.m_y < pC->m_pDecodedPlane->u_height\
                                -(Params.m_inputCoord.m_y + Params.m_inputSize.m_height))
                            {
                                /*There is not enough place above the input pan zoom area to
                                extend it symmetrically,
                                so extend it to the maximum on the top*/
                                Params.m_inputCoord.m_y = 0;
                            }
                            else
                            {
                                /*There is not enough place below the input pan zoom area to
                                extend it symmetrically,
                                so extend it to the maximum on the bottom*/
                                Params.m_inputCoord.m_y = pC->m_pDecodedPlane->u_height \
                                    - tempInputSizeHeightMax;
                            }
                            /*The input height of the AIR is the maximum possible height*/
                            Params.m_inputSize.m_height = tempInputSizeHeightMax;
                        }
                        else
                        {
                            /*The maximum possible input height is greater than the input
                            image height,
                            that means black borders are necessary to keep aspect ratio
                            The input height of the AIR is all the input image height*/
                            Params.m_outputSize.m_height =
                                (tempOutputSizeHeight*pC->m_pDecodedPlane->u_height)\
                                    /Params.m_inputSize.m_height;
                            Params.m_outputSize.m_height = (Params.m_outputSize.m_height>>1)<<1;
                            Params.m_inputCoord.m_y = 0;
                            Params.m_inputSize.m_height = pC->m_pDecodedPlane->u_height;
                            pImagePlanes[0].u_topleft =
                                 (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_height\
                                    -Params.m_outputSize.m_height)>>1))*pImagePlanes[0].u_stride;
                            pImagePlanes[0].u_height = Params.m_outputSize.m_height;
                            pImagePlanes[1].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_height\
                                    -(Params.m_outputSize.m_height>>1)))>>1)\
                                        *pImagePlanes[1].u_stride);
                            pImagePlanes[1].u_height = Params.m_outputSize.m_height>>1;
                            pImagePlanes[2].u_topleft =
                                 ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_height\
                                    -(Params.m_outputSize.m_height>>1)))>>1)\
                                        *pImagePlanes[2].u_stride);
                            pImagePlanes[2].u_height = Params.m_outputSize.m_height>>1;
                        }
                    }
                    else
                    {
                        /*Black borders will be on the left and right side of the output video*/
                        /*Maximum output width if the input image aspect ratio is kept and if the
                         output height is the screen height*/
                        M4OSA_UInt32 tempOutputSizeWidth =
                             (M4OSA_UInt32)((pC->m_pDecodedPlane->u_width \
                                * pImagePlanes->u_height) /pC->m_pDecodedPlane->u_height);
                        M4OSA_UInt32 tempInputSizeWidthMax = 0;
                        M4OSA_UInt32 tempFinalInputWidth = 0;
                        /*The output height is the screen height*/
                        Params.m_outputSize.m_height = pImagePlanes->u_height;
                        tempOutputSizeWidth = (tempOutputSizeWidth>>1)<<1;

                        /*Maximum input width according to the maximum output width
                        (proportional to the maximum output width)*/
                        tempInputSizeWidthMax =
                             (pImagePlanes->u_width*Params.m_inputSize.m_width)\
                                /tempOutputSizeWidth;
                        tempInputSizeWidthMax = (tempInputSizeWidthMax>>1)<<1;

                        /*Check if the maximum possible input width is contained into the input
                         image width*/
                        if(tempInputSizeWidthMax <= pC->m_pDecodedPlane->u_width)
                        {
                            /*The maximum possible input width is contained in the input
                            image width,
                            that means no black borders, the input pan zoom area will be extended
                            so that the input AIR width will be the maximum possible*/
                            if(((tempInputSizeWidthMax - Params.m_inputSize.m_width)>>1) \
                                <= Params.m_inputCoord.m_x
                                && ((tempInputSizeWidthMax - Params.m_inputSize.m_width)>>1)\
                                     <= pC->m_pDecodedPlane->u_width -(Params.m_inputCoord.m_x \
                                        + Params.m_inputSize.m_width))
                            {
                                /*The input pan zoom area can be extended symmetrically on the
                                     right and left side*/
                                Params.m_inputCoord.m_x -= ((tempInputSizeWidthMax\
                                     - Params.m_inputSize.m_width)>>1);
                            }
                            else if(Params.m_inputCoord.m_x < pC->m_pDecodedPlane->u_width\
                                -(Params.m_inputCoord.m_x + Params.m_inputSize.m_width))
                            {
                                /*There is not enough place above the input pan zoom area to
                                    extend it symmetrically,
                                so extend it to the maximum on the left*/
                                Params.m_inputCoord.m_x = 0;
                            }
                            else
                            {
                                /*There is not enough place below the input pan zoom area
                                    to extend it symmetrically,
                                so extend it to the maximum on the right*/
                                Params.m_inputCoord.m_x = pC->m_pDecodedPlane->u_width \
                                    - tempInputSizeWidthMax;
                            }
                            /*The input width of the AIR is the maximum possible width*/
                            Params.m_inputSize.m_width = tempInputSizeWidthMax;
                        }
                        else
                        {
                            /*The maximum possible input width is greater than the input
                            image width,
                            that means black borders are necessary to keep aspect ratio
                            The input width of the AIR is all the input image width*/
                            Params.m_outputSize.m_width =\
                                 (tempOutputSizeWidth*pC->m_pDecodedPlane->u_width)\
                                    /Params.m_inputSize.m_width;
                            Params.m_outputSize.m_width = (Params.m_outputSize.m_width>>1)<<1;
                            Params.m_inputCoord.m_x = 0;
                            Params.m_inputSize.m_width = pC->m_pDecodedPlane->u_width;
                            pImagePlanes[0].u_topleft =
                                 (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_width\
                                    -Params.m_outputSize.m_width)>>1));
                            pImagePlanes[0].u_width = Params.m_outputSize.m_width;
                            pImagePlanes[1].u_topleft =
                                 (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_width\
                                    -(Params.m_outputSize.m_width>>1)))>>1);
                            pImagePlanes[1].u_width = Params.m_outputSize.m_width>>1;
                            pImagePlanes[2].u_topleft =
                                 (M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_width\
                                    -(Params.m_outputSize.m_width>>1)))>>1);
                            pImagePlanes[2].u_width = Params.m_outputSize.m_width>>1;
                        }
                    }
                    break;
                case M4COMMON_kOrientationLeftTop:
                case M4COMMON_kOrientationLeftBottom:
                case M4COMMON_kOrientationRightTop:
                case M4COMMON_kOrientationRightBottom:
                    /*ROTATION*/
                    if((M4OSA_UInt32)((pC->m_pDecodedPlane->u_width * pImagePlanes->u_width)\
                         /pC->m_pDecodedPlane->u_height) < pImagePlanes->u_height)
                         //Params.m_inputSize.m_height > Params.m_inputSize.m_width)
                    {
                        /*Black borders will be on the left and right side of the output video*/
                        /*Maximum output height if the input image aspect ratio is kept and if
                        the output height is the screen width*/
                        M4OSA_UInt32 tempOutputSizeHeight =
                        (M4OSA_UInt32)((pC->m_pDecodedPlane->u_width * pImagePlanes->u_width)\
                             /pC->m_pDecodedPlane->u_height);
                        M4OSA_UInt32 tempInputSizeHeightMax = 0;
                        M4OSA_UInt32 tempFinalInputHeight = 0;
                        /*The output width is the screen height*/
                        Params.m_outputSize.m_height = pImagePlanes->u_width;
                        Params.m_outputSize.m_width= pImagePlanes->u_height;
                        tempOutputSizeHeight = (tempOutputSizeHeight>>1)<<1;

                        /*Maximum input height according to the maximum output height
                             (proportional to the maximum output height)*/
                        tempInputSizeHeightMax =
                            (pImagePlanes->u_height*Params.m_inputSize.m_width)\
                                /tempOutputSizeHeight;
                        tempInputSizeHeightMax = (tempInputSizeHeightMax>>1)<<1;

                        /*Check if the maximum possible input height is contained into the
                             input image width (rotation included)*/
                        if(tempInputSizeHeightMax <= pC->m_pDecodedPlane->u_width)
                        {
                            /*The maximum possible input height is contained in the input
                            image width (rotation included),
                            that means no black borders, the input pan zoom area will be extended
                            so that the input AIR width will be the maximum possible*/
                            if(((tempInputSizeHeightMax - Params.m_inputSize.m_width)>>1) \
                                <= Params.m_inputCoord.m_x
                                && ((tempInputSizeHeightMax - Params.m_inputSize.m_width)>>1)\
                                     <= pC->m_pDecodedPlane->u_width -(Params.m_inputCoord.m_x \
                                        + Params.m_inputSize.m_width))
                            {
                                /*The input pan zoom area can be extended symmetrically on the
                                 right and left side*/
                                Params.m_inputCoord.m_x -= ((tempInputSizeHeightMax \
                                    - Params.m_inputSize.m_width)>>1);
                            }
                            else if(Params.m_inputCoord.m_x < pC->m_pDecodedPlane->u_width\
                                -(Params.m_inputCoord.m_x + Params.m_inputSize.m_width))
                            {
                                /*There is not enough place on the left of the input pan
                                zoom area to extend it symmetrically,
                                so extend it to the maximum on the left*/
                                Params.m_inputCoord.m_x = 0;
                            }
                            else
                            {
                                /*There is not enough place on the right of the input pan zoom
                                 area to extend it symmetrically,
                                so extend it to the maximum on the right*/
                                Params.m_inputCoord.m_x =
                                     pC->m_pDecodedPlane->u_width - tempInputSizeHeightMax;
                            }
                            /*The input width of the AIR is the maximum possible width*/
                            Params.m_inputSize.m_width = tempInputSizeHeightMax;
                        }
                        else
                        {
                            /*The maximum possible input height is greater than the input
                            image width (rotation included),
                            that means black borders are necessary to keep aspect ratio
                            The input width of the AIR is all the input image width*/
                            Params.m_outputSize.m_width =
                            (tempOutputSizeHeight*pC->m_pDecodedPlane->u_width)\
                                /Params.m_inputSize.m_width;
                            Params.m_outputSize.m_width = (Params.m_outputSize.m_width>>1)<<1;
                            Params.m_inputCoord.m_x = 0;
                            Params.m_inputSize.m_width = pC->m_pDecodedPlane->u_width;
                            pImagePlanes[0].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_height\
                                    -Params.m_outputSize.m_width))>>1)*pImagePlanes[0].u_stride)+1;
                            pImagePlanes[0].u_height = Params.m_outputSize.m_width;
                            pImagePlanes[1].u_topleft =
                            ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_height\
                                -(Params.m_outputSize.m_width>>1)))>>1)\
                                    *pImagePlanes[1].u_stride)+1;
                            pImagePlanes[1].u_height = Params.m_outputSize.m_width>>1;
                            pImagePlanes[2].u_topleft =
                            ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_height\
                                -(Params.m_outputSize.m_width>>1)))>>1)\
                                    *pImagePlanes[2].u_stride)+1;
                            pImagePlanes[2].u_height = Params.m_outputSize.m_width>>1;
                        }
                    }
                    else
                    {
                        /*Black borders will be on the top and bottom of the output video*/
                        /*Maximum output width if the input image aspect ratio is kept and if
                         the output width is the screen height*/
                        M4OSA_UInt32 tempOutputSizeWidth =
                        (M4OSA_UInt32)((pC->m_pDecodedPlane->u_height * pImagePlanes->u_height)\
                             /pC->m_pDecodedPlane->u_width);
                        M4OSA_UInt32 tempInputSizeWidthMax = 0;
                        M4OSA_UInt32 tempFinalInputWidth = 0, tempFinalOutputWidth = 0;
                        /*The output height is the screen width*/
                        Params.m_outputSize.m_width = pImagePlanes->u_height;
                        Params.m_outputSize.m_height= pImagePlanes->u_width;
                        tempOutputSizeWidth = (tempOutputSizeWidth>>1)<<1;

                        /*Maximum input width according to the maximum output width
                         (proportional to the maximum output width)*/
                        tempInputSizeWidthMax =
                        (pImagePlanes->u_width*Params.m_inputSize.m_height)/tempOutputSizeWidth;
                        tempInputSizeWidthMax = (tempInputSizeWidthMax>>1)<<1;

                        /*Check if the maximum possible input width is contained into the input
                         image height (rotation included)*/
                        if(tempInputSizeWidthMax <= pC->m_pDecodedPlane->u_height)
                        {
                            /*The maximum possible input width is contained in the input
                             image height (rotation included),
                            that means no black borders, the input pan zoom area will be extended
                            so that the input AIR height will be the maximum possible*/
                            if(((tempInputSizeWidthMax - Params.m_inputSize.m_height)>>1) \
                                <= Params.m_inputCoord.m_y
                                && ((tempInputSizeWidthMax - Params.m_inputSize.m_height)>>1)\
                                     <= pC->m_pDecodedPlane->u_height -(Params.m_inputCoord.m_y \
                                        + Params.m_inputSize.m_height))
                            {
                                /*The input pan zoom area can be extended symmetrically on
                                the right and left side*/
                                Params.m_inputCoord.m_y -= ((tempInputSizeWidthMax \
                                    - Params.m_inputSize.m_height)>>1);
                            }
                            else if(Params.m_inputCoord.m_y < pC->m_pDecodedPlane->u_height\
                                -(Params.m_inputCoord.m_y + Params.m_inputSize.m_height))
                            {
                                /*There is not enough place on the top of the input pan zoom
                                area to extend it symmetrically,
                                so extend it to the maximum on the top*/
                                Params.m_inputCoord.m_y = 0;
                            }
                            else
                            {
                                /*There is not enough place on the bottom of the input pan zoom
                                 area to extend it symmetrically,
                                so extend it to the maximum on the bottom*/
                                Params.m_inputCoord.m_y = pC->m_pDecodedPlane->u_height\
                                     - tempInputSizeWidthMax;
                            }
                            /*The input height of the AIR is the maximum possible height*/
                            Params.m_inputSize.m_height = tempInputSizeWidthMax;
                        }
                        else
                        {
                            /*The maximum possible input width is greater than the input\
                             image height (rotation included),
                            that means black borders are necessary to keep aspect ratio
                            The input height of the AIR is all the input image height*/
                            Params.m_outputSize.m_height =
                                (tempOutputSizeWidth*pC->m_pDecodedPlane->u_height)\
                                    /Params.m_inputSize.m_height;
                            Params.m_outputSize.m_height = (Params.m_outputSize.m_height>>1)<<1;
                            Params.m_inputCoord.m_y = 0;
                            Params.m_inputSize.m_height = pC->m_pDecodedPlane->u_height;
                            pImagePlanes[0].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[0].u_width\
                                    -Params.m_outputSize.m_height))>>1))+1;
                            pImagePlanes[0].u_width = Params.m_outputSize.m_height;
                            pImagePlanes[1].u_topleft =
                                ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[1].u_width\
                                    -(Params.m_outputSize.m_height>>1)))>>1))+1;
                            pImagePlanes[1].u_width = Params.m_outputSize.m_height>>1;
                            pImagePlanes[2].u_topleft =
                                 ((M4xVSS_ABS((M4OSA_Int32)(pImagePlanes[2].u_width\
                                    -(Params.m_outputSize.m_height>>1)))>>1))+1;
                            pImagePlanes[2].u_width = Params.m_outputSize.m_height>>1;
                        }
                    }
                    break;
                }
            }

            /*Width and height have to be even*/
            Params.m_outputSize.m_width = (Params.m_outputSize.m_width>>1)<<1;
            Params.m_outputSize.m_height = (Params.m_outputSize.m_height>>1)<<1;
            Params.m_inputSize.m_width = (Params.m_inputSize.m_width>>1)<<1;
            Params.m_inputSize.m_height = (Params.m_inputSize.m_height>>1)<<1;
            pImagePlanes[0].u_width = (pImagePlanes[0].u_width>>1)<<1;
            pImagePlanes[1].u_width = (pImagePlanes[1].u_width>>1)<<1;
            pImagePlanes[2].u_width = (pImagePlanes[2].u_width>>1)<<1;
            pImagePlanes[0].u_height = (pImagePlanes[0].u_height>>1)<<1;
            pImagePlanes[1].u_height = (pImagePlanes[1].u_height>>1)<<1;
            pImagePlanes[2].u_height = (pImagePlanes[2].u_height>>1)<<1;

            /*Check that values are coherent*/
            if(Params.m_inputSize.m_height == Params.m_outputSize.m_height)
            {
                Params.m_inputSize.m_width = Params.m_outputSize.m_width;
            }
            else if(Params.m_inputSize.m_width == Params.m_outputSize.m_width)
            {
                Params.m_inputSize.m_height = Params.m_outputSize.m_height;
            }
        }

        /**
        Picture rendering: Resizing and Cropping*/
        if(pC->m_mediaRendering != M4xVSS_kBlackBorders)
        {
            switch(pBasicTags.orientation)
            {
            default:
            case M4COMMON_kOrientationUnknown:
                Params.m_outputOrientation = M4COMMON_kOrientationTopLeft;
            case M4COMMON_kOrientationTopLeft:
            case M4COMMON_kOrientationTopRight:
            case M4COMMON_kOrientationBottomRight:
            case M4COMMON_kOrientationBottomLeft:
                Params.m_outputSize.m_height = pImagePlanes->u_height;
                Params.m_outputSize.m_width = pImagePlanes->u_width;
                break;
            case M4COMMON_kOrientationLeftTop:
            case M4COMMON_kOrientationLeftBottom:
            case M4COMMON_kOrientationRightTop:
            case M4COMMON_kOrientationRightBottom:
                Params.m_outputSize.m_height = pImagePlanes->u_width;
                Params.m_outputSize.m_width = pImagePlanes->u_height;
                break;
            }
        }

        /**
        Picture rendering: Cropping*/
        if(pC->m_mediaRendering == M4xVSS_kCropping)
        {
            if((Params.m_outputSize.m_height * Params.m_inputSize.m_width)\
                 /Params.m_outputSize.m_width<Params.m_inputSize.m_height)
            {
                M4OSA_UInt32 tempHeight = Params.m_inputSize.m_height;
                /*height will be cropped*/
                Params.m_inputSize.m_height =  (M4OSA_UInt32)((Params.m_outputSize.m_height \
                    * Params.m_inputSize.m_width) /Params.m_outputSize.m_width);
                Params.m_inputSize.m_height =  (Params.m_inputSize.m_height>>1)<<1;
                if(M4OSA_FALSE == pC->m_pPto3GPPparams->isPanZoom)
                {
                    Params.m_inputCoord.m_y = (M4OSA_Int32)((M4OSA_Int32)\
                        ((pC->m_pDecodedPlane->u_height - Params.m_inputSize.m_height))>>1);
                }
                else
                {
                    Params.m_inputCoord.m_y += (M4OSA_Int32)((M4OSA_Int32)\
                        ((tempHeight - Params.m_inputSize.m_height))>>1);
                }
            }
            else
            {
                M4OSA_UInt32 tempWidth= Params.m_inputSize.m_width;
                /*width will be cropped*/
                Params.m_inputSize.m_width =  (M4OSA_UInt32)((Params.m_outputSize.m_width \
                    * Params.m_inputSize.m_height) /Params.m_outputSize.m_height);
                Params.m_inputSize.m_width =  (Params.m_inputSize.m_width>>1)<<1;
                if(M4OSA_FALSE == pC->m_pPto3GPPparams->isPanZoom)
                {
                    Params.m_inputCoord.m_x = (M4OSA_Int32)((M4OSA_Int32)\
                        ((pC->m_pDecodedPlane->u_width - Params.m_inputSize.m_width))>>1);
                }
                else
                {
                    Params.m_inputCoord.m_x += (M4OSA_Int32)\
                        (((M4OSA_Int32)(tempWidth - Params.m_inputSize.m_width))>>1);
                }
            }
        }



        /**
         * Call AIR functions */
        if(M4OSA_NULL == pC->m_air_context)
        {
            err = M4AIR_create(&pC->m_air_context, M4AIR_kYUV420P);
            if(err != M4NO_ERROR)
            {
                free(pC->m_pDecodedPlane[0].pac_data);
                free(pC->m_pDecodedPlane);
                pC->m_pDecodedPlane = M4OSA_NULL;
                M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct:\
                     Error when initializing AIR: 0x%x", err);
                return err;
            }
        }

        err = M4AIR_configure(pC->m_air_context, &Params);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct:\
                 Error when configuring AIR: 0x%x", err);
            M4AIR_cleanUp(pC->m_air_context);
            free(pC->m_pDecodedPlane[0].pac_data);
            free(pC->m_pDecodedPlane);
            pC->m_pDecodedPlane = M4OSA_NULL;
            return err;
        }

        err = M4AIR_get(pC->m_air_context, pC->m_pDecodedPlane, pImagePlanes);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct: Error when getting AIR plane: 0x%x", err);
            M4AIR_cleanUp(pC->m_air_context);
            free(pC->m_pDecodedPlane[0].pac_data);
            free(pC->m_pDecodedPlane);
            pC->m_pDecodedPlane = M4OSA_NULL;
            return err;
        }
        pImagePlanes[0] = pImagePlanes1;
        pImagePlanes[1] = pImagePlanes2;
        pImagePlanes[2] = pImagePlanes3;
    }


    /**
     * Increment the image counter */
    pC->m_ImageCounter++;

    /**
     * Check end of sequence */
    last_frame_flag    = (pC->m_ImageCounter >= pC->m_NbImage);

    /**
     * Keep the picture duration */
    *pPictureDuration = pC->m_timeDuration;

    if (1 == last_frame_flag)
    {
        if(M4OSA_NULL != pC->m_air_context)
        {
            err = M4AIR_cleanUp(pC->m_air_context);
            if(err != M4NO_ERROR)
            {
                M4OSA_TRACE1_1("M4xVSS_PictureCallbackFct: Error when cleaning AIR: 0x%x", err);
                return err;
            }
        }
        if(M4OSA_NULL != pC->m_pDecodedPlane)
        {
            free(pC->m_pDecodedPlane[0].pac_data);
            free(pC->m_pDecodedPlane);
            pC->m_pDecodedPlane = M4OSA_NULL;
        }
        return M4PTO3GPP_WAR_LAST_PICTURE;
    }

    M4OSA_TRACE1_0("M4xVSS_PictureCallbackFct: Leaving ");
    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_internalStartConvertPictureTo3gp(M4OSA_Context pContext)
 * @brief    This function initializes Pto3GPP with the given parameters
 * @note    The "Pictures to 3GPP" parameters are given by the internal xVSS
 *            context. This context contains a pointer on the current element
 *            of the chained list of Pto3GPP parameters.
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4PTO3GPP_WAR_LAST_PICTURE: The returned image is the last one
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalStartConvertPictureTo3gp(M4OSA_Context pContext)
{
    /************************************************************************/
    /* Definitions to generate dummy AMR file used to add AMR silence in files generated
     by Pto3GPP */
    #define M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE     13
    /* This constant is defined in M4VSS3GPP_InternalConfig.h */
    extern const M4OSA_UInt8\
         M4VSS3GPP_AMR_AU_SILENCE_FRAME_048[M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE];

    /* AMR silent frame used to compute dummy AMR silence file */
    #define M4VSS3GPP_AMR_HEADER_SIZE 6
    const M4OSA_UInt8 M4VSS3GPP_AMR_HEADER[M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE] =
    { 0x23, 0x21, 0x41, 0x4d, 0x52, 0x0a };
    /************************************************************************/

    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;
    M4PTO3GPP_Context pM4PTO3GPP_Ctxt = M4OSA_NULL;
    M4PTO3GPP_Params Params;
     M4xVSS_PictureCallbackCtxt*    pCallBackCtxt;
    M4OSA_Bool cmpResult=M4OSA_FALSE;
    M4OSA_Context pDummyAMRFile;
    M4OSA_Char out_amr[M4XVSS_MAX_PATH_LEN];
    /*UTF conversion support*/
    M4OSA_Char* pDecodedPath = M4OSA_NULL;
    M4OSA_UInt32 i;

    /**
     * Create a M4PTO3GPP instance */
    err = M4PTO3GPP_Init( &pM4PTO3GPP_Ctxt, xVSS_context->pFileReadPtr,
         xVSS_context->pFileWritePtr);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalStartConvertPictureTo3gp returned %ld\n",err);
        return err;
    }

    pCallBackCtxt = (M4xVSS_PictureCallbackCtxt*)M4OSA_32bitAlignedMalloc(sizeof(M4xVSS_PictureCallbackCtxt),
         M4VS,(M4OSA_Char *) "Pto3gpp callback struct");
    if(pCallBackCtxt == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalStartConvertPictureTo3gp");
        return M4ERR_ALLOC;
    }

    Params.OutputVideoFrameSize = xVSS_context->pSettings->xVSS.outputVideoSize;
    Params.OutputVideoFormat = xVSS_context->pSettings->xVSS.outputVideoFormat;
    Params.videoProfile = xVSS_context->pSettings->xVSS.outputVideoProfile;
    Params.videoLevel = xVSS_context->pSettings->xVSS.outputVideoLevel;

    /**
     * Generate "dummy" amr file containing silence in temporary folder */
    M4OSA_chrNCopy(out_amr, xVSS_context->pTempPath, M4XVSS_MAX_PATH_LEN - 1);
    strncat((char *)out_amr, (const char *)"dummy.amr\0", 10);

    /**
     * UTF conversion: convert the temporary path into the customer format*/
    pDecodedPath = out_amr;

    if(xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
    {
        M4OSA_UInt32 length = 0;
        err = M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void*) out_amr,
             (M4OSA_Void*) xVSS_context->UTFConversionContext.pTempOutConversionBuffer, &length);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalStartConvertPictureTo3gp:\
                 M4xVSS_internalConvertFromUTF8 returns err: 0x%x",err);
            return err;
        }
        pDecodedPath = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
    }

    /**
    * End of the conversion, now use the converted path*/

    err = xVSS_context->pFileWritePtr->openWrite(&pDummyAMRFile, pDecodedPath, M4OSA_kFileWrite);

    /*Commented because of the use of the UTF conversion see above*/
/*    err = xVSS_context->pFileWritePtr->openWrite(&pDummyAMRFile, out_amr, M4OSA_kFileWrite);
 */
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertPictureTo3gp: Can't open output dummy amr file %s,\
             error: 0x%x\n",out_amr, err);
        return err;
    }

    err =  xVSS_context->pFileWritePtr->writeData(pDummyAMRFile,
        (M4OSA_Int8*)M4VSS3GPP_AMR_HEADER, M4VSS3GPP_AMR_HEADER_SIZE);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertPictureTo3gp: Can't write output dummy amr file %s,\
             error: 0x%x\n",out_amr, err);
        return err;
    }

    err =  xVSS_context->pFileWritePtr->writeData(pDummyAMRFile,
         (M4OSA_Int8*)M4VSS3GPP_AMR_AU_SILENCE_FRAME_048, M4VSS3GPP_AMR_AU_SILENCE_FRAME_048_SIZE);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertPictureTo3gp: \
            Can't write output dummy amr file %s, error: 0x%x\n",out_amr, err);
        return err;
    }

    err =  xVSS_context->pFileWritePtr->closeWrite(pDummyAMRFile);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("M4xVSS_internalConvertPictureTo3gp: \
            Can't close output dummy amr file %s, error: 0x%x\n",out_amr, err);
        return err;
    }

    /**
     * Fill parameters for Pto3GPP with the parameters contained in the current element of the
     * Pto3GPP parameters chained list and with default parameters */
/*+ New Encoder bitrates */
    if(xVSS_context->pSettings->xVSS.outputVideoBitrate == 0) {
        Params.OutputVideoBitrate    = M4VIDEOEDITING_kVARIABLE_KBPS;
    }
    else {
          Params.OutputVideoBitrate = xVSS_context->pSettings->xVSS.outputVideoBitrate;
    }
    M4OSA_TRACE1_1("M4xVSS_internalStartConvertPicTo3GP: video bitrate = %d",
        Params.OutputVideoBitrate);
/*- New Encoder bitrates */
    Params.OutputFileMaxSize    = M4PTO3GPP_kUNLIMITED;
    Params.pPictureCallbackFct    = M4xVSS_PictureCallbackFct;
    Params.pPictureCallbackCtxt    = pCallBackCtxt;
    /*FB: change to use the converted path (UTF conversion) see the conversion above*/
    /*Fix :- Adding Audio Track in Image as input :AudioTarckFile Setting to NULL */
    Params.pInputAudioTrackFile    = M4OSA_NULL;//(M4OSA_Void*)pDecodedPath;//out_amr;
    Params.AudioPaddingMode        = M4PTO3GPP_kAudioPaddingMode_Loop;
    Params.AudioFileFormat        = M4VIDEOEDITING_kFileType_AMR;
    Params.pOutput3gppFile        = xVSS_context->pPTo3GPPcurrentParams->pFileOut;
    Params.pTemporaryFile        = xVSS_context->pPTo3GPPcurrentParams->pFileTemp;
    /*+PR No:  blrnxpsw#223*/
    /*Increasing frequency of Frame, calculating Nos of Frame = duration /FPS */
    /*Other changes made is @ M4xVSS_API.c @ line 3841 in M4xVSS_SendCommand*/
    /*If case check for PanZoom removed */
    Params.NbVideoFrames            = (M4OSA_UInt32)
        (xVSS_context->pPTo3GPPcurrentParams->duration \
            / xVSS_context->pPTo3GPPcurrentParams->framerate); /* */
    pCallBackCtxt->m_timeDuration    = xVSS_context->pPTo3GPPcurrentParams->framerate;
    /*-PR No:  blrnxpsw#223*/
    pCallBackCtxt->m_ImageCounter    = 0;
    pCallBackCtxt->m_FileIn            = xVSS_context->pPTo3GPPcurrentParams->pFileIn;
    pCallBackCtxt->m_NbImage        = Params.NbVideoFrames;
    pCallBackCtxt->m_pFileReadPtr    = xVSS_context->pFileReadPtr;
    pCallBackCtxt->m_pDecodedPlane    = M4OSA_NULL;
    pCallBackCtxt->m_pPto3GPPparams    = xVSS_context->pPTo3GPPcurrentParams;
    pCallBackCtxt->m_air_context    = M4OSA_NULL;
    pCallBackCtxt->m_mediaRendering = xVSS_context->pPTo3GPPcurrentParams->MediaRendering;

    /**
     * Set the input and output files */
    err = M4PTO3GPP_Open(pM4PTO3GPP_Ctxt, &Params);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Open returned: 0x%x\n",err);
        if(pCallBackCtxt != M4OSA_NULL)
        {
            free(pCallBackCtxt);
            pCallBackCtxt = M4OSA_NULL;
        }
        M4PTO3GPP_CleanUp(pM4PTO3GPP_Ctxt);
        return err;
    }

    /**
     * Save context to be able to call Pto3GPP step function in M4xVSS_step function */
    xVSS_context->pM4PTO3GPP_Ctxt = pM4PTO3GPP_Ctxt;
    xVSS_context->pCallBackCtxt = pCallBackCtxt;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_internalStopConvertPictureTo3gp(M4OSA_Context pContext)
 * @brief    This function cleans up Pto3GPP
 * @note
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalStopConvertPictureTo3gp(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;
    M4OSA_Char out_amr[M4XVSS_MAX_PATH_LEN];
    /*UTF conversion support*/
    M4OSA_Char* pDecodedPath = M4OSA_NULL;

    /**
    * Free the PTO3GPP callback context */
    if(M4OSA_NULL != xVSS_context->pCallBackCtxt)
    {
        free(xVSS_context->pCallBackCtxt);
        xVSS_context->pCallBackCtxt = M4OSA_NULL;
    }

    /**
     * Finalize the output file */
    err = M4PTO3GPP_Close(xVSS_context->pM4PTO3GPP_Ctxt);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_Close returned 0x%x\n",err);
        M4PTO3GPP_CleanUp(xVSS_context->pM4PTO3GPP_Ctxt);
        return err;
    }

    /**
     * Free this M4PTO3GPP instance */
    err = M4PTO3GPP_CleanUp(xVSS_context->pM4PTO3GPP_Ctxt);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4PTO3GPP_CleanUp returned 0x%x\n",err);
        return err;
    }

    /**
     * Remove dummy.amr file */
    M4OSA_chrNCopy(out_amr, xVSS_context->pTempPath, M4XVSS_MAX_PATH_LEN - 1);
    strncat((char *)out_amr, (const char *)"dummy.amr\0", 10);

    /**
     * UTF conversion: convert the temporary path into the customer format*/
    pDecodedPath = out_amr;

    if(xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
    {
        M4OSA_UInt32 length = 0;
        err = M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void*) out_amr,
             (M4OSA_Void*) xVSS_context->UTFConversionContext.pTempOutConversionBuffer, &length);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalStopConvertPictureTo3gp:\
                 M4xVSS_internalConvertFromUTF8 returns err: 0x%x",err);
            return err;
        }
        pDecodedPath = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
    }
    /**
    * End of the conversion, now use the decoded path*/
    remove((const char *)pDecodedPath);

    /*Commented because of the use of the UTF conversion*/
/*    remove(out_amr);
 */

    xVSS_context->pM4PTO3GPP_Ctxt = M4OSA_NULL;
    xVSS_context->pCallBackCtxt = M4OSA_NULL;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx)
 * @brief    This function converts an RGB565 plane to YUV420 planar
 * @note    It is used only for framing effect
 *            It allocates output YUV planes
 * @param    framingCtx    (IN) The framing struct containing input RGB565 plane
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 * @return    M4ERR_ALLOC: Allocation error (no more memory)
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx)
{
    M4OSA_ERR err;

    /**
     * Allocate output YUV planes */
    framingCtx->FramingYuv = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane),
         M4VS, (M4OSA_Char *)"M4xVSS_internalConvertRGBtoYUV: Output plane YUV");
    if(framingCtx->FramingYuv == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[0].u_width = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].u_height = framingCtx->FramingRgb->u_height;
    framingCtx->FramingYuv[0].u_topleft = 0;
    framingCtx->FramingYuv[0].u_stride = framingCtx->FramingRgb->u_width;
    framingCtx->FramingYuv[0].pac_data =
         (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc((framingCtx->FramingYuv[0].u_width\
            *framingCtx->FramingYuv[0].u_height*3)>>1, M4VS, (M4OSA_Char *)\
                "Alloc for the Convertion output YUV");;
    if(framingCtx->FramingYuv[0].pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertRGBtoYUV");
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[1].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[1].u_topleft = 0;
    framingCtx->FramingYuv[1].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[1].pac_data = framingCtx->FramingYuv[0].pac_data \
        + framingCtx->FramingYuv[0].u_width * framingCtx->FramingYuv[0].u_height;
    framingCtx->FramingYuv[2].u_width = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].u_height = (framingCtx->FramingRgb->u_height)>>1;
    framingCtx->FramingYuv[2].u_topleft = 0;
    framingCtx->FramingYuv[2].u_stride = (framingCtx->FramingRgb->u_width)>>1;
    framingCtx->FramingYuv[2].pac_data = framingCtx->FramingYuv[1].pac_data \
        + framingCtx->FramingYuv[1].u_width * framingCtx->FramingYuv[1].u_height;

    /**
     * Convert input RGB 565 to YUV 420 to be able to merge it with output video in framing
      effect */
    err = M4VIFI_xVSS_RGB565toYUV420(M4OSA_NULL, framingCtx->FramingRgb, framingCtx->FramingYuv);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalConvertRGBtoYUV:\
             error when converting from RGB to YUV: 0x%x\n", err);
    }

    framingCtx->duration = 0;
    framingCtx->previousClipTime = -1;
    framingCtx->previewOffsetClipTime = -1;

    /**
     * Only one element in the chained list (no animated image with RGB buffer...) */
    framingCtx->pCurrent = framingCtx;
    framingCtx->pNext = framingCtx;

    return M4NO_ERROR;
}

M4OSA_ERR M4xVSS_internalSetPlaneTransparent(M4OSA_UInt8* planeIn, M4OSA_UInt32 size)
{
    M4OSA_UInt32 i;
    M4OSA_UInt8* plane = planeIn;
    M4OSA_UInt8 transparent1 = (M4OSA_UInt8)((TRANSPARENT_COLOR & 0xFF00)>>8);
    M4OSA_UInt8 transparent2 = (M4OSA_UInt8)TRANSPARENT_COLOR;

    for(i=0; i<(size>>1); i++)
    {
        *plane++ = transparent1;
        *plane++ = transparent2;
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype M4OSA_ERR M4xVSS_internalConvertARBG888toYUV420_FrammingEffect(M4OSA_Context pContext,
 *                                                M4VSS3GPP_EffectSettings* pEffect,
 *                                                M4xVSS_FramingStruct* framingCtx,
                                                  M4VIDEOEDITING_VideoFrameSize OutputVideoResolution)
 *
 * @brief    This function converts ARGB8888 input file  to YUV420 whenused for framming effect
 * @note    The input ARGB8888 file path is contained in the pEffect structure
 *            If the ARGB8888 must be resized to fit output video size, this function
 *            will do it.
 * @param    pContext    (IN) The integrator own context
 * @param    pEffect        (IN) The effect structure containing all informations on
 *                        the file to decode, resizing ...
 * @param    framingCtx    (IN/OUT) Structure in which the output RGB will be stored
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 * @return    M4ERR_ALLOC: Allocation error (no more memory)
 * @return    M4ERR_FILE_NOT_FOUND: File not found.
 ******************************************************************************
 */


M4OSA_ERR M4xVSS_internalConvertARGB888toYUV420_FrammingEffect(M4OSA_Context pContext,
                                                               M4VSS3GPP_EffectSettings* pEffect,
                                                               M4xVSS_FramingStruct* framingCtx,
                                                               M4VIDEOEDITING_VideoFrameSize\
                                                               OutputVideoResolution)
{
    M4OSA_ERR err = M4NO_ERROR;
    M4OSA_Context pARGBIn;
    M4OSA_UInt32 file_size;
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_UInt32 width, height, width_out, height_out;
    M4OSA_Void* pFile = pEffect->xVSS.pFramingFilePath;
    M4OSA_UInt8 transparent1 = (M4OSA_UInt8)((TRANSPARENT_COLOR & 0xFF00)>>8);
    M4OSA_UInt8 transparent2 = (M4OSA_UInt8)TRANSPARENT_COLOR;
    /*UTF conversion support*/
    M4OSA_Char* pDecodedPath = M4OSA_NULL;
    M4OSA_UInt32 i = 0,j = 0;
    M4VIFI_ImagePlane rgbPlane;
    M4OSA_UInt32 frameSize_argb=(framingCtx->width * framingCtx->height * 4);
    M4OSA_UInt32 frameSize;
    M4OSA_UInt32 tempAlphaPercent = 0;
    M4VIFI_UInt8* TempPacData = M4OSA_NULL;
    M4OSA_UInt16 *ptr = M4OSA_NULL;
    M4OSA_UInt32 z = 0;

    M4OSA_TRACE3_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect: Entering ");

    M4OSA_TRACE1_2("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect width and height %d %d ",
        framingCtx->width,framingCtx->height);

    M4OSA_UInt8 *pTmpData = (M4OSA_UInt8*) M4OSA_32bitAlignedMalloc(frameSize_argb, M4VS, (M4OSA_Char*)\
        "Image argb data");
    if(pTmpData == M4OSA_NULL) {
        M4OSA_TRACE1_0("Failed to allocate memory for Image clip");
        return M4ERR_ALLOC;
    }
    /**
     * UTF conversion: convert the file path into the customer format*/
    pDecodedPath = pFile;

    if(xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
            && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
    {
        M4OSA_UInt32 length = 0;
        err = M4xVSS_internalConvertFromUTF8(xVSS_context, (M4OSA_Void*) pFile,
             (M4OSA_Void*) xVSS_context->UTFConversionContext.pTempOutConversionBuffer, &length);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalDecodePNG:\
                 M4xVSS_internalConvertFromUTF8 returns err: 0x%x",err);
            free(pTmpData);
            pTmpData = M4OSA_NULL;
            return err;
        }
        pDecodedPath = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
    }

    /**
    * End of the conversion, now use the decoded path*/

     /* Open input ARGB8888 file and store it into memory */
    err = xVSS_context->pFileReadPtr->openRead(&pARGBIn, pDecodedPath, M4OSA_kFileRead);

    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("Can't open input ARGB8888 file %s, error: 0x%x\n",pFile, err);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        return err;
    }

    err = xVSS_context->pFileReadPtr->readData(pARGBIn,(M4OSA_MemAddr8)pTmpData, &frameSize_argb);
    if(err != M4NO_ERROR)
    {
        xVSS_context->pFileReadPtr->closeRead(pARGBIn);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        return err;
    }


    err =  xVSS_context->pFileReadPtr->closeRead(pARGBIn);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_2("Can't close input png file %s, error: 0x%x\n",pFile, err);
        free(pTmpData);
        pTmpData = M4OSA_NULL;
        return err;
    }


    rgbPlane.u_height = framingCtx->height;
    rgbPlane.u_width = framingCtx->width;
    rgbPlane.u_stride = rgbPlane.u_width*3;
    rgbPlane.u_topleft = 0;

    frameSize = (rgbPlane.u_width * rgbPlane.u_height * 3); //Size of RGB888 data
    rgbPlane.pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(((frameSize)+ (2 * framingCtx->width)),
         M4VS, (M4OSA_Char*)"Image clip RGB888 data");
    if(rgbPlane.pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Failed to allocate memory for Image clip");
        free(pTmpData);
        return M4ERR_ALLOC;
    }

    M4OSA_TRACE1_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:\
          Remove the alpha channel  ");

    /* premultiplied alpha % on RGB */
    for (i=0, j = 0; i < frameSize_argb; i += 4) {
        /* this is alpha value */
        if ((i % 4) == 0)
        {
            tempAlphaPercent = pTmpData[i];
        }

        /* R */
        rgbPlane.pac_data[j] = pTmpData[i+1];
        j++;

        /* G */
        if (tempAlphaPercent > 0) {
            rgbPlane.pac_data[j] = pTmpData[i+2];
            j++;
        } else {/* In case of alpha value 0, make GREEN to 255 */
            rgbPlane.pac_data[j] = 255; //pTmpData[i+2];
            j++;
        }

        /* B */
        rgbPlane.pac_data[j] = pTmpData[i+3];
        j++;
    }

    free(pTmpData);
    pTmpData = M4OSA_NULL;

    /* convert RGB888 to RGB565 */

    /* allocate temp RGB 565 buffer */
    TempPacData = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(frameSize +
                       (4 * (framingCtx->width + framingCtx->height + 1)),
                        M4VS, (M4OSA_Char*)"Image clip RGB565 data");
    if (TempPacData == M4OSA_NULL) {
        M4OSA_TRACE1_0("Failed to allocate memory for Image clip RGB565 data");
        free(rgbPlane.pac_data);
        return M4ERR_ALLOC;
    }

    ptr = (M4OSA_UInt16 *)TempPacData;
    z = 0;

    for (i = 0; i < j ; i += 3)
    {
        ptr[z++] = PACK_RGB565(0,   rgbPlane.pac_data[i],
                                    rgbPlane.pac_data[i+1],
                                    rgbPlane.pac_data[i+2]);
    }

    /* free the RBG888 and assign RGB565 */
    free(rgbPlane.pac_data);
    rgbPlane.pac_data = TempPacData;

    /**
     * Check if output sizes are odd */
    if(rgbPlane.u_height % 2 != 0)
    {
        M4VIFI_UInt8* output_pac_data = rgbPlane.pac_data;
        M4OSA_UInt32 i;
        M4OSA_TRACE1_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:\
             output height is odd  ");
        output_pac_data +=rgbPlane.u_width * rgbPlane.u_height*2;

        for(i=0;i<rgbPlane.u_width;i++)
        {
            *output_pac_data++ = transparent1;
            *output_pac_data++ = transparent2;
        }

        /**
         * We just add a white line to the PNG that will be transparent */
        rgbPlane.u_height++;
    }
    if(rgbPlane.u_width % 2 != 0)
    {
        /**
         * We add a new column of white (=transparent), but we need to parse all RGB lines ... */
        M4OSA_UInt32 i;
        M4VIFI_UInt8* newRGBpac_data;
        M4VIFI_UInt8* output_pac_data, *input_pac_data;

        rgbPlane.u_width++;
        M4OSA_TRACE1_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect: \
             output width is odd  ");
        /**
         * We need to allocate a new RGB output buffer in which all decoded data
          + white line will be copied */
        newRGBpac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(rgbPlane.u_height*rgbPlane.u_width*2\
            *sizeof(M4VIFI_UInt8), M4VS, (M4OSA_Char *)"New Framing GIF Output pac_data RGB");

        if(newRGBpac_data == M4OSA_NULL)
        {
            M4OSA_TRACE1_0("Allocation error in \
                M4xVSS_internalConvertARGB888toYUV420_FrammingEffect");
            free(rgbPlane.pac_data);
            return M4ERR_ALLOC;
        }

        output_pac_data= newRGBpac_data;
        input_pac_data = rgbPlane.pac_data;

        for(i=0;i<rgbPlane.u_height;i++)
        {
            memcpy((void *)output_pac_data, (void *)input_pac_data,
                 (rgbPlane.u_width-1)*2);

            output_pac_data += ((rgbPlane.u_width-1)*2);
            /* Put the pixel to transparency color */
            *output_pac_data++ = transparent1;
            *output_pac_data++ = transparent2;

            input_pac_data += ((rgbPlane.u_width-1)*2);
        }
        free(rgbPlane.pac_data);
        rgbPlane.pac_data = newRGBpac_data;
    }

    /* reset stride */
    rgbPlane.u_stride = rgbPlane.u_width*2;

    /**
     * Initialize chained list parameters */
    framingCtx->duration = 0;
    framingCtx->previousClipTime = -1;
    framingCtx->previewOffsetClipTime = -1;

    /**
     * Only one element in the chained list (no animated image ...) */
    framingCtx->pCurrent = framingCtx;
    framingCtx->pNext = framingCtx;

    /**
     * Get output width/height */
     switch(OutputVideoResolution)
    //switch(xVSS_context->pSettings->xVSS.outputVideoSize)
    {
    case M4VIDEOEDITING_kSQCIF:
        width_out = 128;
        height_out = 96;
        break;
    case M4VIDEOEDITING_kQQVGA:
        width_out = 160;
        height_out = 120;
        break;
    case M4VIDEOEDITING_kQCIF:
        width_out = 176;
        height_out = 144;
        break;
    case M4VIDEOEDITING_kQVGA:
        width_out = 320;
        height_out = 240;
        break;
    case M4VIDEOEDITING_kCIF:
        width_out = 352;
        height_out = 288;
        break;
    case M4VIDEOEDITING_kVGA:
        width_out = 640;
        height_out = 480;
        break;
    case M4VIDEOEDITING_kWVGA:
        width_out = 800;
        height_out = 480;
        break;
    case M4VIDEOEDITING_kNTSC:
        width_out = 720;
        height_out = 480;
        break;
    case M4VIDEOEDITING_k640_360:
        width_out = 640;
        height_out = 360;
        break;
    case M4VIDEOEDITING_k854_480:
        // StageFright encoders require %16 resolution
        width_out = M4ENCODER_854_480_Width;
        height_out = 480;
        break;
    case M4VIDEOEDITING_k1280_720:
        width_out = 1280;
        height_out = 720;
        break;
    case M4VIDEOEDITING_k1080_720:
        // StageFright encoders require %16 resolution
        width_out = M4ENCODER_1080_720_Width;
        height_out = 720;
        break;
    case M4VIDEOEDITING_k960_720:
        width_out = 960;
        height_out = 720;
        break;
    case M4VIDEOEDITING_k1920_1080:
        width_out = 1920;
        height_out = M4ENCODER_1920_1080_Height;
        break;
    /**
     * If output video size is not given, we take QCIF size,
     * should not happen, because already done in M4xVSS_sendCommand */
    default:
        width_out = 176;
        height_out = 144;
        break;
    }

    /**
     * Allocate output planes structures */
    framingCtx->FramingRgb = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(sizeof(M4VIFI_ImagePlane), M4VS,
         (M4OSA_Char *)"Framing Output plane RGB");
    if(framingCtx->FramingRgb == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertARGB888toYUV420_FrammingEffect");
        return M4ERR_ALLOC;
    }
    /**
     * Resize RGB if needed */
    if((pEffect->xVSS.bResize) &&
         (rgbPlane.u_width != width_out || rgbPlane.u_height != height_out))
    {
        width = width_out;
        height = height_out;

        M4OSA_TRACE1_2("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect: \
             New Width and height %d %d  ",width,height);

        framingCtx->FramingRgb->u_height = height_out;
        framingCtx->FramingRgb->u_width = width_out;
        framingCtx->FramingRgb->u_stride = framingCtx->FramingRgb->u_width*2;
        framingCtx->FramingRgb->u_topleft = 0;

        framingCtx->FramingRgb->pac_data =
             (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(framingCtx->FramingRgb->u_height*framingCtx->\
                FramingRgb->u_width*2*sizeof(M4VIFI_UInt8), M4VS,
                  (M4OSA_Char *)"Framing Output pac_data RGB");

        if(framingCtx->FramingRgb->pac_data == M4OSA_NULL)
        {
            M4OSA_TRACE1_0("Allocation error in \
                M4xVSS_internalConvertARGB888toYUV420_FrammingEffect");
            free(framingCtx->FramingRgb);
            free(rgbPlane.pac_data);
            return M4ERR_ALLOC;
        }

        M4OSA_TRACE1_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:  Resizing Needed ");
        M4OSA_TRACE1_2("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:\
              rgbPlane.u_height & rgbPlane.u_width %d %d",rgbPlane.u_height,rgbPlane.u_width);

        //err = M4VIFI_ResizeBilinearRGB888toRGB888(M4OSA_NULL, &rgbPlane,framingCtx->FramingRgb);
        err = M4VIFI_ResizeBilinearRGB565toRGB565(M4OSA_NULL, &rgbPlane,framingCtx->FramingRgb);

        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect :\
                when resizing RGB plane: 0x%x\n", err);
            return err;
        }

        if(rgbPlane.pac_data != M4OSA_NULL)
        {
            free(rgbPlane.pac_data);
            rgbPlane.pac_data = M4OSA_NULL;
        }
    }
    else
    {

        M4OSA_TRACE1_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:\
              Resizing Not Needed ");

        width = rgbPlane.u_width;
        height = rgbPlane.u_height;
        framingCtx->FramingRgb->u_height = height;
        framingCtx->FramingRgb->u_width = width;
        framingCtx->FramingRgb->u_stride = framingCtx->FramingRgb->u_width*2;
        framingCtx->FramingRgb->u_topleft = 0;
        framingCtx->FramingRgb->pac_data = rgbPlane.pac_data;
    }


    if(pEffect->xVSS.bResize)
    {
        /**
         * Force topleft to 0 for pure framing effect */
        framingCtx->topleft_x = 0;
        framingCtx->topleft_y = 0;
    }


    /**
     * Convert  RGB output to YUV 420 to be able to merge it with output video in framing
     effect */
    framingCtx->FramingYuv = (M4VIFI_ImagePlane*)M4OSA_32bitAlignedMalloc(3*sizeof(M4VIFI_ImagePlane), M4VS,
         (M4OSA_Char *)"Framing Output plane YUV");
    if(framingCtx->FramingYuv == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertARGB888toYUV420_FrammingEffect");
        free(framingCtx->FramingRgb->pac_data);
        return M4ERR_ALLOC;
    }

    // Alloc for Y, U and V planes
    framingCtx->FramingYuv[0].u_width = ((width+1)>>1)<<1;
    framingCtx->FramingYuv[0].u_height = ((height+1)>>1)<<1;
    framingCtx->FramingYuv[0].u_topleft = 0;
    framingCtx->FramingYuv[0].u_stride = ((width+1)>>1)<<1;
    framingCtx->FramingYuv[0].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc
        ((framingCtx->FramingYuv[0].u_width*framingCtx->FramingYuv[0].u_height), M4VS,
            (M4OSA_Char *)"Alloc for the output Y");
    if(framingCtx->FramingYuv[0].pac_data == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertARGB888toYUV420_FrammingEffect");
        free(framingCtx->FramingYuv);
        free(framingCtx->FramingRgb->pac_data);
        return M4ERR_ALLOC;
    }
    framingCtx->FramingYuv[1].u_width = (((width+1)>>1)<<1)>>1;
    framingCtx->FramingYuv[1].u_height = (((height+1)>>1)<<1)>>1;
    framingCtx->FramingYuv[1].u_topleft = 0;
    framingCtx->FramingYuv[1].u_stride = (((width+1)>>1)<<1)>>1;


    framingCtx->FramingYuv[1].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(
        framingCtx->FramingYuv[1].u_width * framingCtx->FramingYuv[1].u_height, M4VS,
        (M4OSA_Char *)"Alloc for the output U");
    if (framingCtx->FramingYuv[1].pac_data == M4OSA_NULL) {
        free(framingCtx->FramingYuv[0].pac_data);
        free(framingCtx->FramingYuv);
        free(framingCtx->FramingRgb->pac_data);
        return M4ERR_ALLOC;
    }

    framingCtx->FramingYuv[2].u_width = (((width+1)>>1)<<1)>>1;
    framingCtx->FramingYuv[2].u_height = (((height+1)>>1)<<1)>>1;
    framingCtx->FramingYuv[2].u_topleft = 0;
    framingCtx->FramingYuv[2].u_stride = (((width+1)>>1)<<1)>>1;


    framingCtx->FramingYuv[2].pac_data = (M4VIFI_UInt8*)M4OSA_32bitAlignedMalloc(
        framingCtx->FramingYuv[2].u_width * framingCtx->FramingYuv[0].u_height, M4VS,
        (M4OSA_Char *)"Alloc for the  output V");
    if (framingCtx->FramingYuv[2].pac_data == M4OSA_NULL) {
        free(framingCtx->FramingYuv[1].pac_data);
        free(framingCtx->FramingYuv[0].pac_data);
        free(framingCtx->FramingYuv);
        free(framingCtx->FramingRgb->pac_data);
        return M4ERR_ALLOC;
    }

    M4OSA_TRACE3_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:\
        convert RGB to YUV ");

    //err = M4VIFI_RGB888toYUV420(M4OSA_NULL, framingCtx->FramingRgb,  framingCtx->FramingYuv);
    err = M4VIFI_RGB565toYUV420(M4OSA_NULL, framingCtx->FramingRgb,  framingCtx->FramingYuv);

    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("SPS png: error when converting from RGB to YUV: 0x%x\n", err);
    }
    M4OSA_TRACE3_0("M4xVSS_internalConvertARGB888toYUV420_FrammingEffect:  Leaving ");
    return err;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalGenerateEditedFile(M4OSA_Context pContext)
 *
 * @brief    This function prepares VSS for editing
 * @note    It also set special xVSS effect as external effects for the VSS
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 * @return    M4ERR_ALLOC: Allocation error (no more memory)
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalGenerateEditedFile(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4VSS3GPP_EditContext pVssCtxt;
    M4OSA_UInt32 i,j;
    M4OSA_ERR err;

    /**
     * Create a VSS 3GPP edition instance */
    err = M4VSS3GPP_editInit( &pVssCtxt, xVSS_context->pFileReadPtr, xVSS_context->pFileWritePtr);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGenerateEditedFile: M4VSS3GPP_editInit returned 0x%x\n",
            err);
        M4VSS3GPP_editCleanUp(pVssCtxt);
        /**
         * Set the VSS context to NULL */
        xVSS_context->pCurrentEditContext = M4OSA_NULL;
        return err;
    }

        M4VSS3GPP_InternalEditContext* pVSSContext =
            (M4VSS3GPP_InternalEditContext*)pVssCtxt;
        pVSSContext->xVSS.outputVideoFormat =
            xVSS_context->pSettings->xVSS.outputVideoFormat;
        pVSSContext->xVSS.outputVideoSize =
            xVSS_context->pSettings->xVSS.outputVideoSize ;
        pVSSContext->xVSS.outputAudioFormat =
            xVSS_context->pSettings->xVSS.outputAudioFormat;
        pVSSContext->xVSS.outputAudioSamplFreq =
            xVSS_context->pSettings->xVSS.outputAudioSamplFreq;
        pVSSContext->xVSS.outputVideoBitrate =
            xVSS_context->pSettings->xVSS.outputVideoBitrate ;
        pVSSContext->xVSS.outputAudioBitrate =
            xVSS_context->pSettings->xVSS.outputAudioBitrate ;
        pVSSContext->xVSS.bAudioMono =
            xVSS_context->pSettings->xVSS.bAudioMono;
        pVSSContext->xVSS.outputVideoProfile =
            xVSS_context->pSettings->xVSS.outputVideoProfile;
        pVSSContext->xVSS.outputVideoLevel =
            xVSS_context->pSettings->xVSS.outputVideoLevel;
    /* In case of MMS use case, we fill directly into the VSS context the targeted bitrate */
    if(xVSS_context->targetedBitrate != 0)
    {
        M4VSS3GPP_InternalEditContext* pVSSContext = (M4VSS3GPP_InternalEditContext*)pVssCtxt;

        pVSSContext->bIsMMS = M4OSA_TRUE;
        pVSSContext->uiMMSVideoBitrate = xVSS_context->targetedBitrate;
        pVSSContext->MMSvideoFramerate = xVSS_context->pSettings->videoFrameRate;
    }

    /*Warning: since the adding of the UTF conversion, pSettings has been changed in the next
    part in  pCurrentEditSettings (there is a specific current editing structure for the saving,
     as for the preview)*/

    /**
     * Set the external video effect functions, for saving mode (to be moved to
      M4xVSS_saveStart() ?)*/
    for (i=0; i<xVSS_context->pCurrentEditSettings->uiClipNumber; i++)
    {
        for (j=0; j<xVSS_context->pCurrentEditSettings->nbEffects; j++)
        {
            if (M4xVSS_kVideoEffectType_BlackAndWhite ==
            xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_BlackAndWhite;
                /*commented FB*/
                /**
                 * We do not need to set the color context, it is already set
                 during sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Pink ==
                xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_Pink; /**< we don't
                // use any function context */
                /*commented FB*/
                /**
                 * We do not need to set the color context,
                  it is already set during sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Green ==
                 xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                    M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                    // (M4OSA_Void*)M4xVSS_kVideoEffectType_Green;
                     /**< we don't use any function context */
                /*commented FB*/
                /**
                 * We do not need to set the color context, it is already set during
                  sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Sepia ==
                 xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_Sepia;
                /**< we don't use any function context */
                /*commented FB*/
                /**
                 * We do not need to set the color context, it is already set during
                 sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Fifties ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectFifties;
                /**
                 * We do not need to set the framing context, it is already set during
                 sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Negative ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_Negative;
                 /**< we don't use any function context */
                /*commented FB*/
                /**
                 * We do not need to set the color context, it is already set during
                  sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Framing ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectFraming;
                /**
                 * We do not need to set the framing context, it is already set during
                 sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_ZoomIn ==
             xVSS_context->pSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectZoom;
                xVSS_context->pCurrentEditSettings->Effects[j].pExtVideoEffectFctCtxt =
                 (M4OSA_Void*)M4xVSS_kVideoEffectType_ZoomIn; /**< we don't use any
                 function context */
            }
            if (M4xVSS_kVideoEffectType_ZoomOut ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectZoom;
                xVSS_context->pCurrentEditSettings->Effects[j].pExtVideoEffectFctCtxt =
                 (M4OSA_Void*)M4xVSS_kVideoEffectType_ZoomOut; /**< we don't use any
                 function context */
            }
            if (M4xVSS_kVideoEffectType_ColorRGB16 ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_ColorRGB16;
                /**< we don't use any function context */
                /**
                 * We do not need to set the color context, it is already set during
                 sendCommand function */
            }
            if (M4xVSS_kVideoEffectType_Gradient ==
             xVSS_context->pCurrentEditSettings->Effects[j].VideoEffectType)
            {
                xVSS_context->pCurrentEditSettings->Effects[j].ExtVideoEffectFct =
                 M4VSS3GPP_externalVideoEffectColor;
                //xVSS_context->pSettings->Effects[j].pExtVideoEffectFctCtxt =
                // (M4OSA_Void*)M4xVSS_kVideoEffectType_ColorRGB16;
                /**< we don't use any function context */
                /**
                 * We do not need to set the color context, it is already set during
                 sendCommand function */
            }

        }
    }

    /**
     * Open the VSS 3GPP */
    err = M4VSS3GPP_editOpen(pVssCtxt, xVSS_context->pCurrentEditSettings);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGenerateEditedFile:\
             M4VSS3GPP_editOpen returned 0x%x\n",err);
        M4VSS3GPP_editCleanUp(pVssCtxt);
        /**
         * Set the VSS context to NULL */
        xVSS_context->pCurrentEditContext = M4OSA_NULL;
        return err;
    }

    /**
     * Save VSS context to be able to close / free VSS later */
    xVSS_context->pCurrentEditContext = pVssCtxt;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalCloseEditedFile(M4OSA_Context pContext)
 *
 * @brief    This function cleans up VSS
 * @note
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalCloseEditedFile(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4VSS3GPP_EditContext pVssCtxt = xVSS_context->pCurrentEditContext;
    M4OSA_ERR err;

    if(xVSS_context->pCurrentEditContext != M4OSA_NULL)
    {
        /**
         * Close the VSS 3GPP */
        err = M4VSS3GPP_editClose(pVssCtxt);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalCloseEditedFile:\
                 M4VSS3GPP_editClose returned 0x%x\n",err);
            M4VSS3GPP_editCleanUp(pVssCtxt);
            /**
             * Set the VSS context to NULL */
            xVSS_context->pCurrentEditContext = M4OSA_NULL;
            return err;
        }

        /**
         * Free this VSS3GPP edition instance */
        err = M4VSS3GPP_editCleanUp(pVssCtxt);
        /**
         * Set the VSS context to NULL */
        xVSS_context->pCurrentEditContext = M4OSA_NULL;
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalCloseEditedFile: \
                M4VSS3GPP_editCleanUp returned 0x%x\n",err);
            return err;
        }
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalGenerateAudioMixFile(M4OSA_Context pContext)
 *
 * @brief    This function prepares VSS for audio mixing
 * @note    It takes its parameters from the BGM settings in the xVSS internal context
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 * @return    M4ERR_ALLOC: Allocation error (no more memory)
 ******************************************************************************
 */
/***
 * FB: the function has been modified since the structure used for the saving is now the
 *  pCurrentEditSettings and not the pSettings
 * This change has been added for the UTF support
 * All the "xVSS_context->pSettings" has been replaced by "xVSS_context->pCurrentEditSettings"
 ***/
M4OSA_ERR M4xVSS_internalGenerateAudioMixFile(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4VSS3GPP_AudioMixingSettings* pAudioMixSettings;
    M4VSS3GPP_AudioMixingContext pAudioMixingCtxt;
    M4OSA_ERR err;
    M4VIDEOEDITING_ClipProperties fileProperties;

    /**
     * Allocate audio mixing settings structure and fill it with BGM parameters */
    pAudioMixSettings = (M4VSS3GPP_AudioMixingSettings*)M4OSA_32bitAlignedMalloc
        (sizeof(M4VSS3GPP_AudioMixingSettings), M4VS, (M4OSA_Char *)"pAudioMixSettings");
    if(pAudioMixSettings == M4OSA_NULL)
    {
        M4OSA_TRACE1_0("Allocation error in M4xVSS_internalGenerateAudioMixFile");
        return M4ERR_ALLOC;
    }

    if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->FileType ==
         M4VIDEOEDITING_kFileType_3GPP)
    {
        err = M4xVSS_internalGetProperties((M4OSA_Context)xVSS_context,
             (M4OSA_Char*)xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile,
                 &fileProperties);
        if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalGenerateAudioMixFile:\
                 impossible to retrieve audio BGM properties ->\
                     reencoding audio background music", err);
            fileProperties.AudioStreamType =
                 xVSS_context->pCurrentEditSettings->xVSS.outputAudioFormat+1;
                  /* To force BGM encoding */
        }
    }

    pAudioMixSettings->bRemoveOriginal = M4OSA_FALSE;
    pAudioMixSettings->AddedAudioFileType =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->FileType;
    pAudioMixSettings->pAddedAudioTrackFile =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile;
    pAudioMixSettings->uiAddVolume =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->uiAddVolume;

    pAudioMixSettings->outputAudioFormat = xVSS_context->pSettings->xVSS.outputAudioFormat;
    pAudioMixSettings->outputASF = xVSS_context->pSettings->xVSS.outputAudioSamplFreq;
    pAudioMixSettings->outputAudioBitrate = xVSS_context->pSettings->xVSS.outputAudioBitrate;
    pAudioMixSettings->uiSamplingFrequency =
     xVSS_context->pSettings->xVSS.pBGMtrack->uiSamplingFrequency;
    pAudioMixSettings->uiNumChannels = xVSS_context->pSettings->xVSS.pBGMtrack->uiNumChannels;

    pAudioMixSettings->b_DuckingNeedeed =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->b_DuckingNeedeed;
    pAudioMixSettings->fBTVolLevel =
     (M4OSA_Float )xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->uiAddVolume/100;
    pAudioMixSettings->InDucking_threshold =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->InDucking_threshold;
    pAudioMixSettings->InDucking_lowVolume =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->lowVolume/100;
    pAudioMixSettings->fPTVolLevel =
     (M4OSA_Float)xVSS_context->pSettings->PTVolLevel/100;
    pAudioMixSettings->bLoop = xVSS_context->pSettings->xVSS.pBGMtrack->bLoop;

    if(xVSS_context->pSettings->xVSS.bAudioMono)
    {
        pAudioMixSettings->outputNBChannels = 1;
    }
    else
    {
        pAudioMixSettings->outputNBChannels = 2;
    }

    /**
     * Fill audio mix settings with BGM parameters */
    pAudioMixSettings->uiBeginLoop =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->uiBeginLoop;
    pAudioMixSettings->uiEndLoop =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->uiEndLoop;
    pAudioMixSettings->uiAddCts =
     xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->uiAddCts;

    /**
     * Output file of the audio mixer will be final file (audio mixing is the last step) */
    pAudioMixSettings->pOutputClipFile = xVSS_context->pOutputFile;
    pAudioMixSettings->pTemporaryFile = xVSS_context->pTemporaryFile;

    /**
     * Input file of the audio mixer is a temporary file containing all audio/video editions */
    pAudioMixSettings->pOriginalClipFile = xVSS_context->pCurrentEditSettings->pOutputFile;

    /**
     * Save audio mixing settings pointer to be able to free it in
     M4xVSS_internalCloseAudioMixedFile function */
    xVSS_context->pAudioMixSettings = pAudioMixSettings;

    /**
     * Create a VSS 3GPP audio mixing instance */
    err = M4VSS3GPP_audioMixingInit(&pAudioMixingCtxt, pAudioMixSettings,
         xVSS_context->pFileReadPtr, xVSS_context->pFileWritePtr);

    /**
     * Save audio mixing context to be able to call audio mixing step function in
      M4xVSS_step function */
    xVSS_context->pAudioMixContext = pAudioMixingCtxt;

    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGenerateAudioMixFile:\
             M4VSS3GPP_audioMixingInit returned 0x%x\n",err);
        //M4VSS3GPP_audioMixingCleanUp(pAudioMixingCtxt);
        return err;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalCloseAudioMixedFile(M4OSA_Context pContext)
 *
 * @brief    This function cleans up VSS for audio mixing
 * @note
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalCloseAudioMixedFile(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;

    /**
     * Free this VSS3GPP audio mixing instance */
    if(xVSS_context->pAudioMixContext != M4OSA_NULL)
    {
        err = M4VSS3GPP_audioMixingCleanUp(xVSS_context->pAudioMixContext);
        if (err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalCloseAudioMixedFile:\
                 M4VSS3GPP_audioMixingCleanUp returned 0x%x\n",err);
            return err;
        }
    }

    /**
     * Free VSS audio mixing settings */
    if(xVSS_context->pAudioMixSettings != M4OSA_NULL)
    {
        free(xVSS_context->pAudioMixSettings);
        xVSS_context->pAudioMixSettings = M4OSA_NULL;
    }

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalFreePreview(M4OSA_Context pContext)
 *
 * @brief    This function cleans up preview edition structure used to generate
 *            preview.3gp file given to the VPS
 * @note    It also free the preview structure given to the VPS
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalFreePreview(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_UInt8 i;

    /**
     * Free clip/transition settings */
    for(i=0; i<xVSS_context->pCurrentEditSettings->uiClipNumber; i++)
    {
        M4xVSS_FreeClipSettings(xVSS_context->pCurrentEditSettings->pClipList[i]);

        free((xVSS_context->pCurrentEditSettings->pClipList[i]));
        xVSS_context->pCurrentEditSettings->pClipList[i] = M4OSA_NULL;

        /**
         * Because there is 1 less transition than clip number */
        if(i != xVSS_context->pCurrentEditSettings->uiClipNumber-1)
        {
            free((xVSS_context->pCurrentEditSettings->pTransitionList[i]));
            xVSS_context->pCurrentEditSettings->pTransitionList[i] = M4OSA_NULL;
        }
    }

    /**
     * Free clip/transition list */
    if(xVSS_context->pCurrentEditSettings->pClipList != M4OSA_NULL)
    {
        free((xVSS_context->pCurrentEditSettings->pClipList));
        xVSS_context->pCurrentEditSettings->pClipList = M4OSA_NULL;
    }
    if(xVSS_context->pCurrentEditSettings->pTransitionList != M4OSA_NULL)
    {
        free((xVSS_context->pCurrentEditSettings->pTransitionList));
        xVSS_context->pCurrentEditSettings->pTransitionList = M4OSA_NULL;
    }

    /**
     * Free output preview file path */
    if(xVSS_context->pCurrentEditSettings->pOutputFile != M4OSA_NULL)
    {
        free(xVSS_context->pCurrentEditSettings->pOutputFile);
        xVSS_context->pCurrentEditSettings->pOutputFile = M4OSA_NULL;
    }

    /**
     * Free temporary preview file path */
    if(xVSS_context->pCurrentEditSettings->pTemporaryFile != M4OSA_NULL)
    {
        remove((const char *)xVSS_context->pCurrentEditSettings->pTemporaryFile);
        free(xVSS_context->pCurrentEditSettings->pTemporaryFile);
        xVSS_context->pCurrentEditSettings->pTemporaryFile = M4OSA_NULL;
    }

    /**
     * Free "local" BGM settings */
    if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack != M4OSA_NULL)
    {
        if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile != M4OSA_NULL)
        {
            free(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile);
            xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile = M4OSA_NULL;
        }
        free(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack);
        xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack = M4OSA_NULL;
    }

    /**
     * Free current edit settings structure */
    if(xVSS_context->pCurrentEditSettings != M4OSA_NULL)
    {
        free(xVSS_context->pCurrentEditSettings);
        xVSS_context->pCurrentEditSettings = M4OSA_NULL;
    }

    /**
     * Free preview effects given to application */
    if(M4OSA_NULL != xVSS_context->pPreviewSettings->Effects)
    {
        free(xVSS_context->pPreviewSettings->Effects);
        xVSS_context->pPreviewSettings->Effects = M4OSA_NULL;
        xVSS_context->pPreviewSettings->nbEffects = 0;
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalFreeSaving(M4OSA_Context pContext)
 *
 * @brief    This function cleans up saving edition structure used to generate
 *            output.3gp file given to the VPS
 * @note
 * @param    pContext    (IN) The integrator own context
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalFreeSaving(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_UInt8 i;

    if(xVSS_context->pCurrentEditSettings != M4OSA_NULL)
    {
        /**
         * Free clip/transition settings */
        for(i=0; i<xVSS_context->pCurrentEditSettings->uiClipNumber; i++)
        {
            M4xVSS_FreeClipSettings(xVSS_context->pCurrentEditSettings->pClipList[i]);

            free((xVSS_context->pCurrentEditSettings->pClipList[i]));
            xVSS_context->pCurrentEditSettings->pClipList[i] = M4OSA_NULL;

            /**
             * Because there is 1 less transition than clip number */
            if(i != xVSS_context->pCurrentEditSettings->uiClipNumber-1)
            {
                free(\
                    (xVSS_context->pCurrentEditSettings->pTransitionList[i]));
                xVSS_context->pCurrentEditSettings->pTransitionList[i] = M4OSA_NULL;
            }
        }

        /**
         * Free clip/transition list */
        if(xVSS_context->pCurrentEditSettings->pClipList != M4OSA_NULL)
        {
            free((xVSS_context->pCurrentEditSettings->pClipList));
            xVSS_context->pCurrentEditSettings->pClipList = M4OSA_NULL;
        }
        if(xVSS_context->pCurrentEditSettings->pTransitionList != M4OSA_NULL)
        {
            free((xVSS_context->pCurrentEditSettings->pTransitionList));
            xVSS_context->pCurrentEditSettings->pTransitionList = M4OSA_NULL;
        }

        if(xVSS_context->pCurrentEditSettings->Effects != M4OSA_NULL)
        {
            free((xVSS_context->pCurrentEditSettings->Effects));
            xVSS_context->pCurrentEditSettings->Effects = M4OSA_NULL;
            xVSS_context->pCurrentEditSettings->nbEffects = 0;
        }

        /**
         * Free output saving file path */
        if(xVSS_context->pCurrentEditSettings->pOutputFile != M4OSA_NULL)
        {
            if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack != M4OSA_NULL)
            {
                remove((const char *)xVSS_context->pCurrentEditSettings->pOutputFile);
                free(xVSS_context->pCurrentEditSettings->pOutputFile);
            }
            if(xVSS_context->pOutputFile != M4OSA_NULL)
            {
                free(xVSS_context->pOutputFile);
                xVSS_context->pOutputFile = M4OSA_NULL;
            }
            xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
            xVSS_context->pCurrentEditSettings->pOutputFile = M4OSA_NULL;
        }

        /**
         * Free temporary saving file path */
        if(xVSS_context->pCurrentEditSettings->pTemporaryFile != M4OSA_NULL)
        {
            remove((const char *)xVSS_context->pCurrentEditSettings->pTemporaryFile);
            free(xVSS_context->pCurrentEditSettings->pTemporaryFile);
            xVSS_context->pCurrentEditSettings->pTemporaryFile = M4OSA_NULL;
        }

        /**
         * Free "local" BGM settings */
        if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack != M4OSA_NULL)
        {
            if(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile != M4OSA_NULL)
            {
                free(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile);
                xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack->pFile = M4OSA_NULL;
            }
            free(xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack);
            xVSS_context->pCurrentEditSettings->xVSS.pBGMtrack = M4OSA_NULL;
        }

        /**
         * Free current edit settings structure */
        free(xVSS_context->pCurrentEditSettings);
        xVSS_context->pCurrentEditSettings = M4OSA_NULL;
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_freeSettings(M4OSA_Context pContext)
 *
 * @brief    This function cleans up an M4VSS3GPP_EditSettings structure
 * @note
 * @param    pSettings    (IN) Pointer on M4VSS3GPP_EditSettings structure to free
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_freeSettings(M4VSS3GPP_EditSettings* pSettings)
{
    M4OSA_UInt8 i,j;

    /**
     * For each clip ... */
    for(i=0; i<pSettings->uiClipNumber; i++)
    {
        /**
         * ... free clip settings */
        if(pSettings->pClipList[i] != M4OSA_NULL)
        {
            M4xVSS_FreeClipSettings(pSettings->pClipList[i]);

            free((pSettings->pClipList[i]));
            pSettings->pClipList[i] = M4OSA_NULL;
        }

        /**
         * ... free transition settings */
        if(i < pSettings->uiClipNumber-1) /* Because there is 1 less transition than clip number */
        {
            if(pSettings->pTransitionList[i] != M4OSA_NULL)
            {
                switch (pSettings->pTransitionList[i]->VideoTransitionType)
                {
                    case M4xVSS_kVideoTransitionType_AlphaMagic:

                        /**
                         * In case of Alpha Magic transition,
                          some extra parameters need to be freed */
                        if(pSettings->pTransitionList[i]->pExtVideoTransitionFctCtxt\
                             != M4OSA_NULL)
                        {
                            free((((M4xVSS_internal_AlphaMagicSettings*)\
                                pSettings->pTransitionList[i]->pExtVideoTransitionFctCtxt)->\
                                    pPlane->pac_data));
                            ((M4xVSS_internal_AlphaMagicSettings*)pSettings->pTransitionList[i\
                                ]->pExtVideoTransitionFctCtxt)->pPlane->pac_data = M4OSA_NULL;

                            free((((M4xVSS_internal_AlphaMagicSettings*)\
                                pSettings->pTransitionList[i]->\
                                    pExtVideoTransitionFctCtxt)->pPlane));
                            ((M4xVSS_internal_AlphaMagicSettings*)pSettings->pTransitionList[i]\
                                ->pExtVideoTransitionFctCtxt)->pPlane = M4OSA_NULL;

                            free((pSettings->pTransitionList[i]->\
                                pExtVideoTransitionFctCtxt));
                            pSettings->pTransitionList[i]->pExtVideoTransitionFctCtxt = M4OSA_NULL;

                            for(j=i+1;j<pSettings->uiClipNumber-1;j++)
                            {
                                if(pSettings->pTransitionList[j] != M4OSA_NULL)
                                {
                                    if(pSettings->pTransitionList[j]->VideoTransitionType ==
                                     M4xVSS_kVideoTransitionType_AlphaMagic)
                                    {
                                        M4OSA_UInt32 pCmpResult=0;
                                        pCmpResult = strcmp((const char *)pSettings->pTransitionList[i]->\
                                            xVSS.transitionSpecific.pAlphaMagicSettings->\
                                                pAlphaFilePath,
                                                (const char *)pSettings->pTransitionList[j]->\
                                                xVSS.transitionSpecific.pAlphaMagicSettings->\
                                                pAlphaFilePath);
                                        if(pCmpResult == 0)
                                        {
                                            /* Free extra internal alpha magic structure and put
                                            it to NULL to avoid refreeing it */
                                            free((pSettings->\
                                                pTransitionList[j]->pExtVideoTransitionFctCtxt));
                                            pSettings->pTransitionList[j]->\
                                                pExtVideoTransitionFctCtxt = M4OSA_NULL;
                                        }
                                    }
                                }
                            }
                        }

                        if(pSettings->pTransitionList[i]->\
                            xVSS.transitionSpecific.pAlphaMagicSettings != M4OSA_NULL)
                        {
                            if(pSettings->pTransitionList[i]->\
                                xVSS.transitionSpecific.pAlphaMagicSettings->\
                                    pAlphaFilePath != M4OSA_NULL)
                            {
                                free(pSettings->\
                                    pTransitionList[i]->\
                                        xVSS.transitionSpecific.pAlphaMagicSettings->\
                                            pAlphaFilePath);
                                pSettings->pTransitionList[i]->\
                                    xVSS.transitionSpecific.pAlphaMagicSettings->\
                                        pAlphaFilePath = M4OSA_NULL;
                            }
                            free(pSettings->pTransitionList[i]->\
                                xVSS.transitionSpecific.pAlphaMagicSettings);
                            pSettings->pTransitionList[i]->\
                                xVSS.transitionSpecific.pAlphaMagicSettings = M4OSA_NULL;

                        }

                    break;


                    case M4xVSS_kVideoTransitionType_SlideTransition:
                        if (M4OSA_NULL != pSettings->pTransitionList[i]->\
                            xVSS.transitionSpecific.pSlideTransitionSettings)
                        {
                            free(pSettings->pTransitionList[i]->\
                                xVSS.transitionSpecific.pSlideTransitionSettings);
                            pSettings->pTransitionList[i]->\
                                xVSS.transitionSpecific.pSlideTransitionSettings = M4OSA_NULL;
                        }
                        if(pSettings->pTransitionList[i]->pExtVideoTransitionFctCtxt != M4OSA_NULL)
                        {
                            free((pSettings->pTransitionList[i]->\
                                pExtVideoTransitionFctCtxt));
                            pSettings->pTransitionList[i]->pExtVideoTransitionFctCtxt = M4OSA_NULL;
                        }
                    break;
                                        default:
                    break;

                }
                /**
                 * Free transition settings structure */
                free((pSettings->pTransitionList[i]));
                pSettings->pTransitionList[i] = M4OSA_NULL;
            }
        }
    }

    /**
     * Free clip list */
    if(pSettings->pClipList != M4OSA_NULL)
    {
        free((pSettings->pClipList));
        pSettings->pClipList = M4OSA_NULL;
    }

    /**
     * Free transition list */
    if(pSettings->pTransitionList != M4OSA_NULL)
    {
        free((pSettings->pTransitionList));
        pSettings->pTransitionList = M4OSA_NULL;
    }

    /**
     * RC: Free effects list */
    if(pSettings->Effects != M4OSA_NULL)
    {
        for(i=0; i<pSettings->nbEffects; i++)
        {
            /**
             * For each clip, free framing structure if needed */
            if(pSettings->Effects[i].VideoEffectType == M4xVSS_kVideoEffectType_Framing
                || pSettings->Effects[i].VideoEffectType == M4xVSS_kVideoEffectType_Text)
            {
#ifdef DECODE_GIF_ON_SAVING
                M4xVSS_FramingContext* framingCtx = pSettings->Effects[i].pExtVideoEffectFctCtxt;
#else
                M4xVSS_FramingStruct* framingCtx = pSettings->Effects[i].pExtVideoEffectFctCtxt;
                M4xVSS_FramingStruct* framingCtx_save;
                M4xVSS_Framing3102Struct* framingCtx_first = framingCtx;
#endif

#ifdef DECODE_GIF_ON_SAVING
                if(framingCtx != M4OSA_NULL) /* Bugfix 1.2.0: crash, trying to free non existant
                 pointer */
                {
                    if(framingCtx->aFramingCtx != M4OSA_NULL)
                    {
                        {
                            if(framingCtx->aFramingCtx->FramingRgb != M4OSA_NULL)
                            {
                                free(framingCtx->aFramingCtx->\
                                    FramingRgb->pac_data);
                                framingCtx->aFramingCtx->FramingRgb->pac_data = M4OSA_NULL;
                                free(framingCtx->aFramingCtx->FramingRgb);
                                framingCtx->aFramingCtx->FramingRgb = M4OSA_NULL;
                            }
                        }
                        if(framingCtx->aFramingCtx->FramingYuv != M4OSA_NULL)
                        {
                            free(framingCtx->aFramingCtx->\
                                FramingYuv[0].pac_data);
                            framingCtx->aFramingCtx->FramingYuv[0].pac_data = M4OSA_NULL;
                           free(framingCtx->aFramingCtx->\
                                FramingYuv[1].pac_data);
                            framingCtx->aFramingCtx->FramingYuv[1].pac_data = M4OSA_NULL;
                           free(framingCtx->aFramingCtx->\
                                FramingYuv[2].pac_data);
                            framingCtx->aFramingCtx->FramingYuv[2].pac_data = M4OSA_NULL;
                            free(framingCtx->aFramingCtx->FramingYuv);
                            framingCtx->aFramingCtx->FramingYuv = M4OSA_NULL;
                        }
                        free(framingCtx->aFramingCtx);
                        framingCtx->aFramingCtx = M4OSA_NULL;
                    }
                    if(framingCtx->aFramingCtx_last != M4OSA_NULL)
                    {
                        if(framingCtx->aFramingCtx_last->FramingRgb != M4OSA_NULL)
                        {
                            free(framingCtx->aFramingCtx_last->\
                                FramingRgb->pac_data);
                            framingCtx->aFramingCtx_last->FramingRgb->pac_data = M4OSA_NULL;
                            free(framingCtx->aFramingCtx_last->\
                                FramingRgb);
                            framingCtx->aFramingCtx_last->FramingRgb = M4OSA_NULL;
                        }
                        if(framingCtx->aFramingCtx_last->FramingYuv != M4OSA_NULL)
                        {
                            free(framingCtx->aFramingCtx_last->\
                                FramingYuv[0].pac_data);
                            framingCtx->aFramingCtx_last->FramingYuv[0].pac_data = M4OSA_NULL;
                            free(framingCtx->aFramingCtx_last->FramingYuv);
                            framingCtx->aFramingCtx_last->FramingYuv = M4OSA_NULL;
                        }
                        free(framingCtx->aFramingCtx_last);
                        framingCtx->aFramingCtx_last = M4OSA_NULL;
                    }
                    if(framingCtx->pEffectFilePath != M4OSA_NULL)
                    {
                        free(framingCtx->pEffectFilePath);
                        framingCtx->pEffectFilePath = M4OSA_NULL;
                    }
                    /*In case there are still allocated*/
                    if(framingCtx->pSPSContext != M4OSA_NULL)
                    {
                    //    M4SPS_destroy(framingCtx->pSPSContext);
                        framingCtx->pSPSContext = M4OSA_NULL;
                    }
                    /*Alpha blending structure*/
                    if(framingCtx->alphaBlendingStruct  != M4OSA_NULL)
                    {
                        free(framingCtx->alphaBlendingStruct);
                        framingCtx->alphaBlendingStruct = M4OSA_NULL;
                    }

                    free(framingCtx);
                    framingCtx = M4OSA_NULL;
                }
#else
                do
                {
                    if(framingCtx != M4OSA_NULL) /* Bugfix 1.2.0: crash, trying to free non
                    existant pointer */
                    {
                        if(framingCtx->FramingRgb != M4OSA_NULL)
                        {
                            free(framingCtx->FramingRgb->pac_data);
                            framingCtx->FramingRgb->pac_data = M4OSA_NULL;
                            free(framingCtx->FramingRgb);
                            framingCtx->FramingRgb = M4OSA_NULL;
                        }
                        if(framingCtx->FramingYuv != M4OSA_NULL)
                        {
                            free(framingCtx->FramingYuv[0].pac_data);
                            framingCtx->FramingYuv[0].pac_data = M4OSA_NULL;
                            free(framingCtx->FramingYuv);
                            framingCtx->FramingYuv = M4OSA_NULL;
                        }
                        framingCtx_save = framingCtx->pNext;
                        free(framingCtx);
                        framingCtx = M4OSA_NULL;
                        framingCtx = framingCtx_save;
                    }
                    else
                    {
                        /*FB: bug fix P4ME00003002*/
                        break;
                    }
                } while(framingCtx_first != framingCtx);
#endif
            }
            else if( M4xVSS_kVideoEffectType_Fifties == pSettings->Effects[i].VideoEffectType)
            {
                /* Free Fifties context */
                M4xVSS_FiftiesStruct* FiftiesCtx = pSettings->Effects[i].pExtVideoEffectFctCtxt;

                if(FiftiesCtx != M4OSA_NULL)
                {
                    free(FiftiesCtx);
                    FiftiesCtx = M4OSA_NULL;
                }

            }
            else if( M4xVSS_kVideoEffectType_ColorRGB16 == pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_BlackAndWhite == pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_Pink == pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_Green == pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_Sepia == pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_Negative== pSettings->Effects[i].VideoEffectType
                || M4xVSS_kVideoEffectType_Gradient== pSettings->Effects[i].VideoEffectType)
            {
                /* Free Color context */
                M4xVSS_ColorStruct* ColorCtx = pSettings->Effects[i].pExtVideoEffectFctCtxt;

                if(ColorCtx != M4OSA_NULL)
                {
                    free(ColorCtx);
                    ColorCtx = M4OSA_NULL;
                }
            }

            /* Free simple fields */
            if(pSettings->Effects[i].xVSS.pFramingFilePath != M4OSA_NULL)
            {
                free(pSettings->Effects[i].xVSS.pFramingFilePath);
                pSettings->Effects[i].xVSS.pFramingFilePath = M4OSA_NULL;
            }
            if(pSettings->Effects[i].xVSS.pFramingBuffer != M4OSA_NULL)
            {
                free(pSettings->Effects[i].xVSS.pFramingBuffer);
                pSettings->Effects[i].xVSS.pFramingBuffer = M4OSA_NULL;
            }
            if(pSettings->Effects[i].xVSS.pTextBuffer != M4OSA_NULL)
            {
                free(pSettings->Effects[i].xVSS.pTextBuffer);
                pSettings->Effects[i].xVSS.pTextBuffer = M4OSA_NULL;
            }
        }
        free(pSettings->Effects);
        pSettings->Effects = M4OSA_NULL;
    }

    return M4NO_ERROR;
}

M4OSA_ERR M4xVSS_freeCommand(M4OSA_Context pContext)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
//    M4OSA_UInt8 i,j;

    /* Free "local" BGM settings */
    if(xVSS_context->pSettings->xVSS.pBGMtrack != M4OSA_NULL)
    {
        if(xVSS_context->pSettings->xVSS.pBGMtrack->pFile != M4OSA_NULL)
        {
            free(xVSS_context->pSettings->xVSS.pBGMtrack->pFile);
            xVSS_context->pSettings->xVSS.pBGMtrack->pFile = M4OSA_NULL;
        }
        free(xVSS_context->pSettings->xVSS.pBGMtrack);
        xVSS_context->pSettings->xVSS.pBGMtrack = M4OSA_NULL;
    }

    M4xVSS_freeSettings(xVSS_context->pSettings);

    if(xVSS_context->pPTo3GPPparamsList != M4OSA_NULL)
    {
        M4xVSS_Pto3GPP_params* pParams = xVSS_context->pPTo3GPPparamsList;
        M4xVSS_Pto3GPP_params* pParams_sauv;

        while(pParams != M4OSA_NULL)
        {
            if(pParams->pFileIn != M4OSA_NULL)
            {
                free(pParams->pFileIn);
                pParams->pFileIn = M4OSA_NULL;
            }
            if(pParams->pFileOut != M4OSA_NULL)
            {
                /* Delete temporary file */
                remove((const char *)pParams->pFileOut);
                free(pParams->pFileOut);
                pParams->pFileOut = M4OSA_NULL;
            }
            if(pParams->pFileTemp != M4OSA_NULL)
            {
                /* Delete temporary file */
#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE
                remove((const char *)pParams->pFileTemp);
                free(pParams->pFileTemp);
#endif/*M4xVSS_RESERVED_MOOV_DISK_SPACE*/
                pParams->pFileTemp = M4OSA_NULL;
            }
            pParams_sauv = pParams;
            pParams = pParams->pNext;
            free(pParams_sauv);
            pParams_sauv = M4OSA_NULL;
        }
    }

    if(xVSS_context->pMCSparamsList != M4OSA_NULL)
    {
        M4xVSS_MCS_params* pParams = xVSS_context->pMCSparamsList;
        M4xVSS_MCS_params* pParams_sauv;

        while(pParams != M4OSA_NULL)
        {
            if(pParams->pFileIn != M4OSA_NULL)
            {
                free(pParams->pFileIn);
                pParams->pFileIn = M4OSA_NULL;
            }
            if(pParams->pFileOut != M4OSA_NULL)
            {
                /* Delete temporary file */
                remove((const char *)pParams->pFileOut);
                free(pParams->pFileOut);
                pParams->pFileOut = M4OSA_NULL;
            }
            if(pParams->pFileTemp != M4OSA_NULL)
            {
                /* Delete temporary file */
#ifdef M4xVSS_RESERVED_MOOV_DISK_SPACE
                remove((const char *)pParams->pFileTemp);
                free(pParams->pFileTemp);
#endif/*M4xVSS_RESERVED_MOOV_DISK_SPACE*/
                pParams->pFileTemp = M4OSA_NULL;
            }
            pParams_sauv = pParams;
            pParams = pParams->pNext;
            free(pParams_sauv);
            pParams_sauv = M4OSA_NULL;
        }
    }

    if(xVSS_context->pcmPreviewFile != M4OSA_NULL)
    {
        free(xVSS_context->pcmPreviewFile);
        xVSS_context->pcmPreviewFile = M4OSA_NULL;
    }
    if(xVSS_context->pSettings->pOutputFile != M4OSA_NULL
        && xVSS_context->pOutputFile != M4OSA_NULL)
    {
        free(xVSS_context->pSettings->pOutputFile);
        xVSS_context->pSettings->pOutputFile = M4OSA_NULL;
        xVSS_context->pOutputFile = M4OSA_NULL;
    }

    /* Reinit all context variables */
    xVSS_context->previousClipNumber = 0;
    xVSS_context->editingStep = M4xVSS_kMicroStateEditing;
    xVSS_context->analyseStep = M4xVSS_kMicroStateAnalysePto3GPP;
    xVSS_context->pPTo3GPPparamsList = M4OSA_NULL;
    xVSS_context->pPTo3GPPcurrentParams = M4OSA_NULL;
    xVSS_context->pMCSparamsList = M4OSA_NULL;
    xVSS_context->pMCScurrentParams = M4OSA_NULL;
    xVSS_context->tempFileIndex = 0;
    xVSS_context->targetedTimescale = 0;

    return M4NO_ERROR;
}

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalGetProperties(M4OSA_Context pContext,
 *                                    M4OSA_Char* pFile,
 *                                    M4VIDEOEDITING_ClipProperties *pFileProperties)
 *
 * @brief    This function retrieve properties of an input 3GP file using MCS
 * @note
 * @param    pContext        (IN) The integrator own context
 * @param    pFile            (IN) 3GP file to analyse
 * @param    pFileProperties    (IN/OUT) Pointer on a structure that will contain
 *                            the 3GP file properties
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalGetProperties(M4OSA_Context pContext, M4OSA_Char* pFile,
                                       M4VIDEOEDITING_ClipProperties *pFileProperties)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;
    M4MCS_Context mcs_context;

    err = M4MCS_init(&mcs_context, xVSS_context->pFileReadPtr, xVSS_context->pFileWritePtr);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGetProperties: Error in M4MCS_init: 0x%x", err);
        return err;
    }

    /*open the MCS in the "normal opening" mode to retrieve the exact duration*/
    err = M4MCS_open_normalMode(mcs_context, pFile, M4VIDEOEDITING_kFileType_3GPP,
        M4OSA_NULL, M4OSA_NULL);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGetProperties: Error in M4MCS_open: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    err = M4MCS_getInputFileProperties(mcs_context, pFileProperties);
    if(err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("Error in M4MCS_getInputFileProperties: 0x%x", err);
        M4MCS_abort(mcs_context);
        return err;
    }

    err = M4MCS_abort(mcs_context);
    if (err != M4NO_ERROR)
    {
        M4OSA_TRACE1_1("M4xVSS_internalGetProperties: Error in M4MCS_abort: 0x%x", err);
        return err;
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalGetTargetedTimeScale(M4OSA_Context pContext,
 *                                                M4OSA_UInt32* pTargetedTimeScale)
 *
 * @brief    This function retrieve targeted time scale
 * @note
 * @param    pContext            (IN)    The integrator own context
 * @param    pTargetedTimeScale    (OUT)    Targeted time scale
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalGetTargetedTimeScale(M4OSA_Context pContext,
                                                 M4VSS3GPP_EditSettings* pSettings,
                                                  M4OSA_UInt32* pTargetedTimeScale)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;
    M4OSA_UInt32 totalDuration = 0;
    M4OSA_UInt8 i = 0;
    M4OSA_UInt32 tempTimeScale = 0, tempDuration = 0;

    for(i=0;i<pSettings->uiClipNumber;i++)
    {
        /*search timescale only in mpeg4 case*/
        if(pSettings->pClipList[i]->FileType == M4VIDEOEDITING_kFileType_3GPP
            || pSettings->pClipList[i]->FileType == M4VIDEOEDITING_kFileType_MP4
            || pSettings->pClipList[i]->FileType == M4VIDEOEDITING_kFileType_M4V)
        {
            M4VIDEOEDITING_ClipProperties fileProperties;

            /*UTF conversion support*/
            M4OSA_Char* pDecodedPath = M4OSA_NULL;

            /**
            * UTF conversion: convert into the customer format, before being used*/
            pDecodedPath = pSettings->pClipList[i]->pFile;

            if(xVSS_context->UTFConversionContext.pConvToUTF8Fct != M4OSA_NULL
                && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
            {
                M4OSA_UInt32 length = 0;
                err = M4xVSS_internalConvertFromUTF8(xVSS_context,
                     (M4OSA_Void*) pSettings->pClipList[i]->pFile,
                        (M4OSA_Void*) xVSS_context->UTFConversionContext.pTempOutConversionBuffer,
                             &length);
                if(err != M4NO_ERROR)
                {
                    M4OSA_TRACE1_1("M4xVSS_Init:\
                         M4xVSS_internalConvertToUTF8 returns err: 0x%x",err);
                    return err;
                }
                pDecodedPath = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
            }

            /*End of the conversion: use the decoded path*/
            err = M4xVSS_internalGetProperties(xVSS_context, pDecodedPath, &fileProperties);

            /*get input file properties*/
            /*err = M4xVSS_internalGetProperties(xVSS_context, pSettings->\
                pClipList[i]->pFile, &fileProperties);*/
            if(M4NO_ERROR != err)
            {
                M4OSA_TRACE1_1("M4xVSS_internalGetTargetedTimeScale:\
                     M4xVSS_internalGetProperties returned: 0x%x", err);
                return err;
            }
            if(fileProperties.VideoStreamType == M4VIDEOEDITING_kMPEG4)
            {
                if(pSettings->pClipList[i]->uiEndCutTime > 0)
                {
                    if(tempDuration < (pSettings->pClipList[i]->uiEndCutTime \
                        - pSettings->pClipList[i]->uiBeginCutTime))
                    {
                        tempTimeScale = fileProperties.uiVideoTimeScale;
                        tempDuration = (pSettings->pClipList[i]->uiEndCutTime\
                             - pSettings->pClipList[i]->uiBeginCutTime);
                    }
                }
                else
                {
                    if(tempDuration < (fileProperties.uiClipDuration\
                         - pSettings->pClipList[i]->uiBeginCutTime))
                    {
                        tempTimeScale = fileProperties.uiVideoTimeScale;
                        tempDuration = (fileProperties.uiClipDuration\
                             - pSettings->pClipList[i]->uiBeginCutTime);
                    }
                }
            }
        }
        if(pSettings->pClipList[i]->FileType == M4VIDEOEDITING_kFileType_ARGB8888)
        {
            /*the timescale is 30 for PTO3GP*/
            *pTargetedTimeScale = 30;
            return M4NO_ERROR;

        }
    }

    if(tempTimeScale >= 30)/*Define a minimum time scale, otherwise if the timescale is not
    enough, there will be an infinite loop in the shell encoder*/
    {
        *pTargetedTimeScale = tempTimeScale;
    }
    else
    {
        *pTargetedTimeScale = 30;
    }

    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function apply a color effect on an input YUV420 planar frame
 * @note
 * @param    pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext,
                                             M4VIFI_ImagePlane *PlaneIn,
                                             M4VIFI_ImagePlane *PlaneOut,
                                             M4VSS3GPP_ExternalProgress *pProgress,
                                             M4OSA_UInt32 uiEffectKind)
{
    M4VIFI_Int32 plane_number;
    M4VIFI_UInt32 i,j;
    M4VIFI_UInt8 *p_buf_src, *p_buf_dest;
    M4xVSS_ColorStruct* ColorContext = (M4xVSS_ColorStruct*)pFunctionContext;

    for (plane_number = 0; plane_number < 3; plane_number++)
    {
        p_buf_src = &(PlaneIn[plane_number].pac_data[PlaneIn[plane_number].u_topleft]);
        p_buf_dest = &(PlaneOut[plane_number].pac_data[PlaneOut[plane_number].u_topleft]);
        for (i = 0; i < PlaneOut[plane_number].u_height; i++)
        {
            /**
             * Chrominance */
            if(plane_number==1 || plane_number==2)
            {
                //switch ((M4OSA_UInt32)pFunctionContext)
                // commented because a structure for the effects context exist
                switch (ColorContext->colorEffectType)
                {
                    case M4xVSS_kVideoEffectType_BlackAndWhite:
                        memset((void *)p_buf_dest,128,
                         PlaneIn[plane_number].u_width);
                        break;
                    case M4xVSS_kVideoEffectType_Pink:
                        memset((void *)p_buf_dest,255,
                         PlaneIn[plane_number].u_width);
                        break;
                    case M4xVSS_kVideoEffectType_Green:
                        memset((void *)p_buf_dest,0,
                         PlaneIn[plane_number].u_width);
                        break;
                    case M4xVSS_kVideoEffectType_Sepia:
                        if(plane_number==1)
                        {
                            memset((void *)p_buf_dest,117,
                             PlaneIn[plane_number].u_width);
                        }
                        else
                        {
                            memset((void *)p_buf_dest,139,
                             PlaneIn[plane_number].u_width);
                        }
                        break;
                    case M4xVSS_kVideoEffectType_Negative:
                        memcpy((void *)p_buf_dest,
                         (void *)p_buf_src ,PlaneOut[plane_number].u_width);
                        break;

                    case M4xVSS_kVideoEffectType_ColorRGB16:
                        {
                            M4OSA_UInt16 r = 0,g = 0,b = 0,y = 0,u = 0,v = 0;

                            /*first get the r, g, b*/
                            b = (ColorContext->rgb16ColorData &  0x001f);
                            g = (ColorContext->rgb16ColorData &  0x07e0)>>5;
                            r = (ColorContext->rgb16ColorData &  0xf800)>>11;

                            /*keep y, but replace u and v*/
                            if(plane_number==1)
                            {
                                /*then convert to u*/
                                u = U16(r, g, b);
                                memset((void *)p_buf_dest,(M4OSA_UInt8)u,
                                 PlaneIn[plane_number].u_width);
                            }
                            if(plane_number==2)
                            {
                                /*then convert to v*/
                                v = V16(r, g, b);
                                memset((void *)p_buf_dest, (M4OSA_UInt8)v,
                                 PlaneIn[plane_number].u_width);
                            }
                        }
                        break;
                    case M4xVSS_kVideoEffectType_Gradient:
                        {
                            M4OSA_UInt16 r = 0,g = 0,b = 0,y = 0,u = 0,v = 0;

                            /*first get the r, g, b*/
                            b = (ColorContext->rgb16ColorData &  0x001f);
                            g = (ColorContext->rgb16ColorData &  0x07e0)>>5;
                            r = (ColorContext->rgb16ColorData &  0xf800)>>11;

                            /*for color gradation*/
                            b = (M4OSA_UInt16)( b - ((b*i)/PlaneIn[plane_number].u_height));
                            g = (M4OSA_UInt16)(g - ((g*i)/PlaneIn[plane_number].u_height));
                            r = (M4OSA_UInt16)(r - ((r*i)/PlaneIn[plane_number].u_height));

                            /*keep y, but replace u and v*/
                            if(plane_number==1)
                            {
                                /*then convert to u*/
                                u = U16(r, g, b);
                                memset((void *)p_buf_dest,(M4OSA_UInt8)u,
                                 PlaneIn[plane_number].u_width);
                            }
                            if(plane_number==2)
                            {
                                /*then convert to v*/
                                v = V16(r, g, b);
                                memset((void *)p_buf_dest,(M4OSA_UInt8)v,
                                 PlaneIn[plane_number].u_width);
                            }
                        }
                        break;
                        default:
                        break;
                }
            }
            /**
             * Luminance */
            else
            {
                //switch ((M4OSA_UInt32)pFunctionContext)
                // commented because a structure for the effects context exist
                switch (ColorContext->colorEffectType)
                {
                case M4xVSS_kVideoEffectType_Negative:
                    for(j=0;j<PlaneOut[plane_number].u_width;j++)
                    {
                            p_buf_dest[j] = 255 - p_buf_src[j];
                    }
                    break;
                default:
                    memcpy((void *)p_buf_dest,
                     (void *)p_buf_src ,PlaneOut[plane_number].u_width);
                    break;
                }
            }
            p_buf_src += PlaneIn[plane_number].u_stride;
            p_buf_dest += PlaneOut[plane_number].u_stride;
        }
    }

    return M4VIFI_OK;
}

/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectFraming(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function add a fixed or animated image on an input YUV420 planar frame
 * @note
 * @param    pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_externalVideoEffectFraming( M4OSA_Void *userData,
                                                M4VIFI_ImagePlane PlaneIn[3],
                                                M4VIFI_ImagePlane *PlaneOut,
                                                M4VSS3GPP_ExternalProgress *pProgress,
                                                M4OSA_UInt32 uiEffectKind )
{
    M4VIFI_UInt32 x,y;

    M4VIFI_UInt8 *p_in_Y = PlaneIn[0].pac_data;
    M4VIFI_UInt8 *p_in_U = PlaneIn[1].pac_data;
    M4VIFI_UInt8 *p_in_V = PlaneIn[2].pac_data;

    M4xVSS_FramingStruct* Framing = M4OSA_NULL;
    M4xVSS_FramingStruct* currentFraming = M4OSA_NULL;
    M4VIFI_UInt8 *FramingRGB = M4OSA_NULL;

    M4VIFI_UInt8 *p_out0;
    M4VIFI_UInt8 *p_out1;
    M4VIFI_UInt8 *p_out2;

    M4VIFI_UInt32 topleft[2];

    M4OSA_UInt8 transparent1 = (M4OSA_UInt8)((TRANSPARENT_COLOR & 0xFF00)>>8);
    M4OSA_UInt8 transparent2 = (M4OSA_UInt8)TRANSPARENT_COLOR;

#ifndef DECODE_GIF_ON_SAVING
    Framing = (M4xVSS_FramingStruct *)userData;
    currentFraming = (M4xVSS_FramingStruct *)Framing->pCurrent;
    FramingRGB = Framing->FramingRgb->pac_data;
#endif /*DECODE_GIF_ON_SAVING*/

    /*FB*/
#ifdef DECODE_GIF_ON_SAVING
    M4OSA_ERR err;
    Framing = (M4xVSS_FramingStruct *)((M4xVSS_FramingContext*)userData)->aFramingCtx;
    currentFraming = (M4xVSS_FramingStruct *)Framing;
    FramingRGB = Framing->FramingRgb->pac_data;
#endif /*DECODE_GIF_ON_SAVING*/
    /*end FB*/

    /**
     * Initialize input / output plane pointers */
    p_in_Y += PlaneIn[0].u_topleft;
    p_in_U += PlaneIn[1].u_topleft;
    p_in_V += PlaneIn[2].u_topleft;

    p_out0 = PlaneOut[0].pac_data;
    p_out1 = PlaneOut[1].pac_data;
    p_out2 = PlaneOut[2].pac_data;

    /**
     * Depending on time, initialize Framing frame to use */
    if(Framing->previousClipTime == -1)
    {
        Framing->previousClipTime = pProgress->uiOutputTime;
    }

    /**
     * If the current clip time has reach the duration of one frame of the framing picture
     * we need to step to next framing picture */

    Framing->previousClipTime = pProgress->uiOutputTime;
    FramingRGB = currentFraming->FramingRgb->pac_data;
    topleft[0] = currentFraming->topleft_x;
    topleft[1] = currentFraming->topleft_y;

    for( x=0 ;x < PlaneIn[0].u_height ; x++)
    {
        for( y=0 ;y < PlaneIn[0].u_width ; y++)
        {
            /**
             * To handle framing with input size != output size
             * Framing is applyed if coordinates matches between framing/topleft and input plane */
            if( y < (topleft[0] + currentFraming->FramingYuv[0].u_width)  &&
                y >= topleft[0] &&
                x < (topleft[1] + currentFraming->FramingYuv[0].u_height) &&
                x >= topleft[1])
            {
                /*Alpha blending support*/
                M4OSA_Float alphaBlending = 1;
                M4xVSS_internalEffectsAlphaBlending*  alphaBlendingStruct =\
                 (M4xVSS_internalEffectsAlphaBlending*)\
                    ((M4xVSS_FramingContext*)userData)->alphaBlendingStruct;

                if(alphaBlendingStruct != M4OSA_NULL)
                {
                    if(pProgress->uiProgress \
                    < (M4OSA_UInt32)(alphaBlendingStruct->m_fadeInTime*10))
                    {
                        if(alphaBlendingStruct->m_fadeInTime == 0) {
                            alphaBlending = alphaBlendingStruct->m_start / 100;
                        } else {
                            alphaBlending = ((M4OSA_Float)(alphaBlendingStruct->m_middle\
                             - alphaBlendingStruct->m_start)\
                                *pProgress->uiProgress/(alphaBlendingStruct->m_fadeInTime*10));
                            alphaBlending += alphaBlendingStruct->m_start;
                            alphaBlending /= 100;
                        }
                    }
                    else if(pProgress->uiProgress >= (M4OSA_UInt32)(alphaBlendingStruct->\
                    m_fadeInTime*10) && pProgress->uiProgress < 1000\
                     - (M4OSA_UInt32)(alphaBlendingStruct->m_fadeOutTime*10))
                    {
                        alphaBlending = (M4OSA_Float)\
                        ((M4OSA_Float)alphaBlendingStruct->m_middle/100);
                    }
                    else if(pProgress->uiProgress >= 1000 - (M4OSA_UInt32)\
                    (alphaBlendingStruct->m_fadeOutTime*10))
                    {
                        if(alphaBlendingStruct->m_fadeOutTime == 0) {
                            alphaBlending = alphaBlendingStruct->m_end / 100;
                        } else {
                            alphaBlending = ((M4OSA_Float)(alphaBlendingStruct->m_middle \
                            - alphaBlendingStruct->m_end))*(1000 - pProgress->uiProgress)\
                            /(alphaBlendingStruct->m_fadeOutTime*10);
                            alphaBlending += alphaBlendingStruct->m_end;
                            alphaBlending /= 100;
                        }
                    }
                }
                /**/

                if((*(FramingRGB)==transparent1) && (*(FramingRGB+1)==transparent2))
                {
                    *( p_out0+y+x*PlaneOut[0].u_stride)=(*(p_in_Y+y+x*PlaneIn[0].u_stride));
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=
                        (*(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride));
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=
                        (*(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride));
                }
                else
                {
                    *( p_out0+y+x*PlaneOut[0].u_stride)=
                        (*(currentFraming->FramingYuv[0].pac_data+(y-topleft[0])\
                            +(x-topleft[1])*currentFraming->FramingYuv[0].u_stride))*alphaBlending;
                    *( p_out0+y+x*PlaneOut[0].u_stride)+=
                        (*(p_in_Y+y+x*PlaneIn[0].u_stride))*(1-alphaBlending);
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=
                        (*(currentFraming->FramingYuv[1].pac_data+((y-topleft[0])>>1)\
                            +((x-topleft[1])>>1)*currentFraming->FramingYuv[1].u_stride))\
                                *alphaBlending;
                    *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)+=
                        (*(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride))*(1-alphaBlending);
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=
                        (*(currentFraming->FramingYuv[2].pac_data+((y-topleft[0])>>1)\
                            +((x-topleft[1])>>1)*currentFraming->FramingYuv[2].u_stride))\
                                *alphaBlending;
                    *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)+=
                        (*(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride))*(1-alphaBlending);
                }
                if( PlaneIn[0].u_width < (topleft[0] + currentFraming->FramingYuv[0].u_width) &&
                    y == PlaneIn[0].u_width-1)
                {
                    FramingRGB = FramingRGB + 2 \
                        * (topleft[0] + currentFraming->FramingYuv[0].u_width \
                            - PlaneIn[0].u_width + 1);
                }
                else
                {
                    FramingRGB = FramingRGB + 2;
                }
            }
            /**
             * Just copy input plane to output plane */
            else
            {
                *( p_out0+y+x*PlaneOut[0].u_stride)=*(p_in_Y+y+x*PlaneIn[0].u_stride);
                *( p_out1+(y>>1)+(x>>1)*PlaneOut[1].u_stride)=
                    *(p_in_U+(y>>1)+(x>>1)*PlaneIn[1].u_stride);
                *( p_out2+(y>>1)+(x>>1)*PlaneOut[2].u_stride)=
                    *(p_in_V+(y>>1)+(x>>1)*PlaneIn[2].u_stride);
            }
        }
    }


    return M4VIFI_OK;
}


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectFifties(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function make a video look as if it was taken in the fifties
 * @note
 * @param    pUserData       (IN) Context
 * @param    pPlaneIn        (IN) Input YUV420 planar
 * @param    pPlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:            No error
 * @return  M4ERR_PARAMETER:    pFiftiesData, pPlaneOut or pProgress are NULL (DEBUG only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_externalVideoEffectFifties( M4OSA_Void *pUserData,
                                                M4VIFI_ImagePlane *pPlaneIn,
                                                M4VIFI_ImagePlane *pPlaneOut,
                                                M4VSS3GPP_ExternalProgress *pProgress,
                                                M4OSA_UInt32 uiEffectKind )
{
    M4VIFI_UInt32 x, y, xShift;
    M4VIFI_UInt8 *pInY = pPlaneIn[0].pac_data;
    M4VIFI_UInt8 *pOutY, *pInYbegin;
    M4VIFI_UInt8 *pInCr,* pOutCr;
    M4VIFI_Int32 plane_number;

    /* Internal context*/
    M4xVSS_FiftiesStruct* p_FiftiesData = (M4xVSS_FiftiesStruct *)pUserData;

    /* Check the inputs (debug only) */
    M4OSA_DEBUG_IF2((p_FiftiesData == M4OSA_NULL),M4ERR_PARAMETER,
         "xVSS: p_FiftiesData is M4OSA_NULL in M4VSS3GPP_externalVideoEffectFifties");
    M4OSA_DEBUG_IF2((pPlaneOut == M4OSA_NULL),M4ERR_PARAMETER,
         "xVSS: p_PlaneOut is M4OSA_NULL in M4VSS3GPP_externalVideoEffectFifties");
    M4OSA_DEBUG_IF2((pProgress == M4OSA_NULL),M4ERR_PARAMETER,
        "xVSS: p_Progress is M4OSA_NULL in M4VSS3GPP_externalVideoEffectFifties");

    /* Initialize input / output plane pointers */
    pInY += pPlaneIn[0].u_topleft;
    pOutY = pPlaneOut[0].pac_data;
    pInYbegin  = pInY;

    /* Initialize the random */
    if(p_FiftiesData->previousClipTime < 0)
    {
        M4OSA_randInit();
        M4OSA_rand((M4OSA_Int32 *)&(p_FiftiesData->shiftRandomValue), (pPlaneIn[0].u_height) >> 4);
        M4OSA_rand((M4OSA_Int32 *)&(p_FiftiesData->stripeRandomValue), (pPlaneIn[0].u_width)<< 2);
        p_FiftiesData->previousClipTime = pProgress->uiOutputTime;
    }

    /* Choose random values if we have reached the duration of a partial effect */
    else if( (pProgress->uiOutputTime - p_FiftiesData->previousClipTime)\
         > p_FiftiesData->fiftiesEffectDuration)
    {
        M4OSA_rand((M4OSA_Int32 *)&(p_FiftiesData->shiftRandomValue), (pPlaneIn[0].u_height) >> 4);
        M4OSA_rand((M4OSA_Int32 *)&(p_FiftiesData->stripeRandomValue), (pPlaneIn[0].u_width)<< 2);
        p_FiftiesData->previousClipTime = pProgress->uiOutputTime;
    }

    /* Put in Sepia the chrominance */
    for (plane_number = 1; plane_number < 3; plane_number++)
    {
        pInCr  = pPlaneIn[plane_number].pac_data  + pPlaneIn[plane_number].u_topleft;
        pOutCr = pPlaneOut[plane_number].pac_data + pPlaneOut[plane_number].u_topleft;

        for (x = 0; x < pPlaneOut[plane_number].u_height; x++)
        {
            if (1 == plane_number)
                memset((void *)pOutCr, 117,pPlaneIn[plane_number].u_width); /* U value */
            else
                memset((void *)pOutCr, 139,pPlaneIn[plane_number].u_width); /* V value */

            pInCr  += pPlaneIn[plane_number].u_stride;
            pOutCr += pPlaneOut[plane_number].u_stride;
        }
    }

    /* Compute the new pixels values */
    for( x = 0 ; x < pPlaneIn[0].u_height ; x++)
    {
        M4VIFI_UInt8 *p_outYtmp, *p_inYtmp;

        /* Compute the xShift (random value) */
        if (0 == (p_FiftiesData->shiftRandomValue % 5 ))
            xShift = (x + p_FiftiesData->shiftRandomValue ) % (pPlaneIn[0].u_height - 1);
        else
            xShift = (x + (pPlaneIn[0].u_height - p_FiftiesData->shiftRandomValue) ) \
                % (pPlaneIn[0].u_height - 1);

        /* Initialize the pointers */
        p_outYtmp = pOutY + 1;                                    /* yShift of 1 pixel */
        p_inYtmp  = pInYbegin + (xShift * pPlaneIn[0].u_stride);  /* Apply the xShift */

        for( y = 0 ; y < pPlaneIn[0].u_width ; y++)
        {
            /* Set Y value */
            if (xShift > (pPlaneIn[0].u_height - 4))
                *p_outYtmp = 40;        /* Add some horizontal black lines between the
                                        two parts of the image */
            else if ( y == p_FiftiesData->stripeRandomValue)
                *p_outYtmp = 90;        /* Add a random vertical line for the bulk */
            else
                *p_outYtmp = *p_inYtmp;


            /* Go to the next pixel */
            p_outYtmp++;
            p_inYtmp++;

            /* Restart at the beginning of the line for the last pixel*/
            if (y == (pPlaneIn[0].u_width - 2))
                p_outYtmp = pOutY;
        }

        /* Go to the next line */
        pOutY += pPlaneOut[0].u_stride;
    }

    return M4VIFI_OK;
}

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_externalVideoEffectZoom( )
 * @brief    Zoom in/out video effect functions.
 * @note    The external video function is used only if VideoEffectType is set to
 * M4VSS3GPP_kVideoEffectType_ZoomIn or M4VSS3GPP_kVideoEffectType_ZoomOut.
 *
 * @param   pFunctionContext    (IN) The function context, previously set by the integrator
 * @param    pInputPlanes        (IN) Input YUV420 image: pointer to an array of three valid
 *                                    image planes (Y, U and V)
 * @param    pOutputPlanes        (IN/OUT) Output (filtered) YUV420 image: pointer to an array of
 *                                        three valid image planes (Y, U and V)
 * @param    pProgress            (IN) Set of information about the video transition progress.
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 ******************************************************************************
 */

M4OSA_ERR M4VSS3GPP_externalVideoEffectZoom(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind
)
{
    M4OSA_UInt32 boxWidth;
    M4OSA_UInt32 boxHeight;
    M4OSA_UInt32 boxPosX;
    M4OSA_UInt32 boxPosY;
    M4OSA_UInt32 ratio = 0;
    /*  * 1.189207 between ratio */
    /* zoom between x1 and x16 */
    M4OSA_UInt32 ratiotab[17] ={1024,1218,1448,1722,2048,2435,2896,3444,4096,4871,5793,\
                                6889,8192,9742,11585,13777,16384};
    M4OSA_UInt32 ik;

    M4VIFI_ImagePlane boxPlane[3];

    if(M4xVSS_kVideoEffectType_ZoomOut == (M4OSA_UInt32)pFunctionContext)
    {
        //ratio = 16 - (15 * pProgress->uiProgress)/1000;
        ratio = 16 - pProgress->uiProgress / 66 ;
    }
    else if(M4xVSS_kVideoEffectType_ZoomIn == (M4OSA_UInt32)pFunctionContext)
    {
        //ratio = 1 + (15 * pProgress->uiProgress)/1000;
        ratio = 1 + pProgress->uiProgress / 66 ;
    }

    for(ik=0;ik<3;ik++){

        boxPlane[ik].u_stride = pInputPlanes[ik].u_stride;
        boxPlane[ik].pac_data = pInputPlanes[ik].pac_data;

        boxHeight = ( pInputPlanes[ik].u_height << 10 ) / ratiotab[ratio];
        boxWidth = ( pInputPlanes[ik].u_width << 10 ) / ratiotab[ratio];
        boxPlane[ik].u_height = (boxHeight)&(~1);
        boxPlane[ik].u_width = (boxWidth)&(~1);

        boxPosY = (pInputPlanes[ik].u_height >> 1) - (boxPlane[ik].u_height >> 1);
        boxPosX = (pInputPlanes[ik].u_width >> 1) - (boxPlane[ik].u_width >> 1);
        boxPlane[ik].u_topleft = boxPosY * boxPlane[ik].u_stride + boxPosX;
    }

    M4VIFI_ResizeBilinearYUV420toYUV420(M4OSA_NULL, (M4VIFI_ImagePlane*)&boxPlane, pOutputPlanes);

    /**
     * Return */
    return(M4NO_ERROR);
}

/**
 ******************************************************************************
 * prototype    M4xVSS_AlphaMagic( M4OSA_Void *userData,
 *                                    M4VIFI_ImagePlane PlaneIn1[3],
 *                                    M4VIFI_ImagePlane PlaneIn2[3],
 *                                    M4VIFI_ImagePlane *PlaneOut,
 *                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                    M4OSA_UInt32 uiTransitionKind)
 *
 * @brief    This function apply a color effect on an input YUV420 planar frame
 * @note
 * @param    userData        (IN) Contains a pointer on a settings structure
 * @param    PlaneIn1        (IN) Input YUV420 planar from video 1
 * @param    PlaneIn2        (IN) Input YUV420 planar from video 2
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiTransitionKind(IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_AlphaMagic( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                             M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                             M4VSS3GPP_ExternalProgress *pProgress, M4OSA_UInt32 uiTransitionKind)
{

    M4OSA_ERR err;

    M4xVSS_internal_AlphaMagicSettings* alphaContext;
    M4VIFI_Int32 alphaProgressLevel;

    M4VIFI_ImagePlane* planeswap;
    M4VIFI_UInt32 x,y;

    M4VIFI_UInt8 *p_out0;
    M4VIFI_UInt8 *p_out1;
    M4VIFI_UInt8 *p_out2;
    M4VIFI_UInt8 *alphaMask;
    /* "Old image" */
    M4VIFI_UInt8 *p_in1_Y;
    M4VIFI_UInt8 *p_in1_U;
    M4VIFI_UInt8 *p_in1_V;
    /* "New image" */
    M4VIFI_UInt8 *p_in2_Y;
    M4VIFI_UInt8 *p_in2_U;
    M4VIFI_UInt8 *p_in2_V;

    err = M4NO_ERROR;

    alphaContext = (M4xVSS_internal_AlphaMagicSettings*)userData;

    alphaProgressLevel = (pProgress->uiProgress * 128)/1000;

    if( alphaContext->isreverse != M4OSA_FALSE)
    {
        alphaProgressLevel = 128 - alphaProgressLevel;
        planeswap = PlaneIn1;
        PlaneIn1 = PlaneIn2;
        PlaneIn2 = planeswap;
    }

    p_out0 = PlaneOut[0].pac_data;
    p_out1 = PlaneOut[1].pac_data;
    p_out2 = PlaneOut[2].pac_data;

    alphaMask = alphaContext->pPlane->pac_data;

    /* "Old image" */
    p_in1_Y = PlaneIn1[0].pac_data;
    p_in1_U = PlaneIn1[1].pac_data;
    p_in1_V = PlaneIn1[2].pac_data;
    /* "New image" */
    p_in2_Y = PlaneIn2[0].pac_data;
    p_in2_U = PlaneIn2[1].pac_data;
    p_in2_V = PlaneIn2[2].pac_data;

     /**
     * For each column ... */
    for( y=0; y<PlaneOut->u_height; y++ )
    {
        /**
         * ... and each row of the alpha mask */
        for( x=0; x<PlaneOut->u_width; x++ )
        {
            /**
             * If the value of the current pixel of the alpha mask is > to the current time
             * ( current time is normalized on [0-255] ) */
            if( alphaProgressLevel < alphaMask[x+y*PlaneOut->u_width] )
            {
                /* We keep "old image" in output plane */
                *( p_out0+x+y*PlaneOut[0].u_stride)=*(p_in1_Y+x+y*PlaneIn1[0].u_stride);
                *( p_out1+(x>>1)+(y>>1)*PlaneOut[1].u_stride)=
                    *(p_in1_U+(x>>1)+(y>>1)*PlaneIn1[1].u_stride);
                *( p_out2+(x>>1)+(y>>1)*PlaneOut[2].u_stride)=
                    *(p_in1_V+(x>>1)+(y>>1)*PlaneIn1[2].u_stride);
            }
            else
            {
                /* We take "new image" in output plane */
                *( p_out0+x+y*PlaneOut[0].u_stride)=*(p_in2_Y+x+y*PlaneIn2[0].u_stride);
                *( p_out1+(x>>1)+(y>>1)*PlaneOut[1].u_stride)=
                    *(p_in2_U+(x>>1)+(y>>1)*PlaneIn2[1].u_stride);
                *( p_out2+(x>>1)+(y>>1)*PlaneOut[2].u_stride)=
                    *(p_in2_V+(x>>1)+(y>>1)*PlaneIn2[2].u_stride);
            }
        }
    }

    return(err);
}

/**
 ******************************************************************************
 * prototype    M4xVSS_AlphaMagicBlending( M4OSA_Void *userData,
 *                                    M4VIFI_ImagePlane PlaneIn1[3],
 *                                    M4VIFI_ImagePlane PlaneIn2[3],
 *                                    M4VIFI_ImagePlane *PlaneOut,
 *                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                    M4OSA_UInt32 uiTransitionKind)
 *
 * @brief    This function apply a color effect on an input YUV420 planar frame
 * @note
 * @param    userData        (IN) Contains a pointer on a settings structure
 * @param    PlaneIn1        (IN) Input YUV420 planar from video 1
 * @param    PlaneIn2        (IN) Input YUV420 planar from video 2
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiTransitionKind(IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_AlphaMagicBlending( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                     M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                                     M4VSS3GPP_ExternalProgress *pProgress,
                                     M4OSA_UInt32 uiTransitionKind)
{
    M4OSA_ERR err;

    M4xVSS_internal_AlphaMagicSettings* alphaContext;
    M4VIFI_Int32 alphaProgressLevel;
    M4VIFI_Int32 alphaBlendLevelMin;
    M4VIFI_Int32 alphaBlendLevelMax;
    M4VIFI_Int32 alphaBlendRange;

    M4VIFI_ImagePlane* planeswap;
    M4VIFI_UInt32 x,y;
    M4VIFI_Int32 alphaMaskValue;

    M4VIFI_UInt8 *p_out0;
    M4VIFI_UInt8 *p_out1;
    M4VIFI_UInt8 *p_out2;
    M4VIFI_UInt8 *alphaMask;
    /* "Old image" */
    M4VIFI_UInt8 *p_in1_Y;
    M4VIFI_UInt8 *p_in1_U;
    M4VIFI_UInt8 *p_in1_V;
    /* "New image" */
    M4VIFI_UInt8 *p_in2_Y;
    M4VIFI_UInt8 *p_in2_U;
    M4VIFI_UInt8 *p_in2_V;


    err = M4NO_ERROR;

    alphaContext = (M4xVSS_internal_AlphaMagicSettings*)userData;

    alphaProgressLevel = (pProgress->uiProgress * 128)/1000;

    if( alphaContext->isreverse != M4OSA_FALSE)
    {
        alphaProgressLevel = 128 - alphaProgressLevel;
        planeswap = PlaneIn1;
        PlaneIn1 = PlaneIn2;
        PlaneIn2 = planeswap;
    }

    alphaBlendLevelMin = alphaProgressLevel-alphaContext->blendingthreshold;

    alphaBlendLevelMax = alphaProgressLevel+alphaContext->blendingthreshold;

    alphaBlendRange = (alphaContext->blendingthreshold)*2;

    p_out0 = PlaneOut[0].pac_data;
    p_out1 = PlaneOut[1].pac_data;
    p_out2 = PlaneOut[2].pac_data;

    alphaMask = alphaContext->pPlane->pac_data;

    /* "Old image" */
    p_in1_Y = PlaneIn1[0].pac_data;
    p_in1_U = PlaneIn1[1].pac_data;
    p_in1_V = PlaneIn1[2].pac_data;
    /* "New image" */
    p_in2_Y = PlaneIn2[0].pac_data;
    p_in2_U = PlaneIn2[1].pac_data;
    p_in2_V = PlaneIn2[2].pac_data;

    /* apply Alpha Magic on each pixel */
       for( y=0; y<PlaneOut->u_height; y++ )
    {
        for( x=0; x<PlaneOut->u_width; x++ )
        {
            alphaMaskValue = alphaMask[x+y*PlaneOut->u_width];
            if( alphaBlendLevelMax < alphaMaskValue )
            {
                /* We keep "old image" in output plane */
                *( p_out0+x+y*PlaneOut[0].u_stride)=*(p_in1_Y+x+y*PlaneIn1[0].u_stride);
                *( p_out1+(x>>1)+(y>>1)*PlaneOut[1].u_stride)=
                    *(p_in1_U+(x>>1)+(y>>1)*PlaneIn1[1].u_stride);
                *( p_out2+(x>>1)+(y>>1)*PlaneOut[2].u_stride)=
                    *(p_in1_V+(x>>1)+(y>>1)*PlaneIn1[2].u_stride);
            }
            else if( (alphaBlendLevelMin < alphaMaskValue)&&
                    (alphaMaskValue <= alphaBlendLevelMax ) )
            {
                /* We blend "old and new image" in output plane */
                *( p_out0+x+y*PlaneOut[0].u_stride)=(M4VIFI_UInt8)
                    (( (alphaMaskValue-alphaBlendLevelMin)*( *(p_in1_Y+x+y*PlaneIn1[0].u_stride))
                        +(alphaBlendLevelMax-alphaMaskValue)\
                            *( *(p_in2_Y+x+y*PlaneIn2[0].u_stride)) )/alphaBlendRange );

                *( p_out1+(x>>1)+(y>>1)*PlaneOut[1].u_stride)=(M4VIFI_UInt8)\
                    (( (alphaMaskValue-alphaBlendLevelMin)*( *(p_in1_U+(x>>1)+(y>>1)\
                        *PlaneIn1[1].u_stride))
                            +(alphaBlendLevelMax-alphaMaskValue)*( *(p_in2_U+(x>>1)+(y>>1)\
                                *PlaneIn2[1].u_stride)) )/alphaBlendRange );

                *( p_out2+(x>>1)+(y>>1)*PlaneOut[2].u_stride)=
                    (M4VIFI_UInt8)(( (alphaMaskValue-alphaBlendLevelMin)\
                        *( *(p_in1_V+(x>>1)+(y>>1)*PlaneIn1[2].u_stride))
                                +(alphaBlendLevelMax-alphaMaskValue)*( *(p_in2_V+(x>>1)+(y>>1)\
                                    *PlaneIn2[2].u_stride)) )/alphaBlendRange );

            }
            else
            {
                /* We take "new image" in output plane */
                *( p_out0+x+y*PlaneOut[0].u_stride)=*(p_in2_Y+x+y*PlaneIn2[0].u_stride);
                *( p_out1+(x>>1)+(y>>1)*PlaneOut[1].u_stride)=
                    *(p_in2_U+(x>>1)+(y>>1)*PlaneIn2[1].u_stride);
                *( p_out2+(x>>1)+(y>>1)*PlaneOut[2].u_stride)=
                    *(p_in2_V+(x>>1)+(y>>1)*PlaneIn2[2].u_stride);
            }
        }
    }

    return(err);
}

#define M4XXX_SampleAddress(plane, x, y)  ( (plane).pac_data + (plane).u_topleft + (y)\
     * (plane).u_stride + (x) )

static void M4XXX_CopyPlane(M4VIFI_ImagePlane* dest, M4VIFI_ImagePlane* source)
{
    M4OSA_UInt32    height, width, sourceStride, destStride, y;
    M4OSA_MemAddr8    sourceWalk, destWalk;

    /* cache the vars used in the loop so as to avoid them being repeatedly fetched and
     recomputed from memory. */
    height = dest->u_height;
    width = dest->u_width;

    sourceWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(*source, 0, 0);
    sourceStride = source->u_stride;

    destWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(*dest, 0, 0);
    destStride = dest->u_stride;

    for (y=0; y<height; y++)
    {
        memcpy((void *)destWalk, (void *)sourceWalk, width);
        destWalk += destStride;
        sourceWalk += sourceStride;
    }
}

static M4OSA_ERR M4xVSS_VerticalSlideTransition(M4VIFI_ImagePlane* topPlane,
                                                M4VIFI_ImagePlane* bottomPlane,
                                                M4VIFI_ImagePlane *PlaneOut,
                                                M4OSA_UInt32    shiftUV)
{
    M4OSA_UInt32 i;

    /* Do three loops, one for each plane type, in order to avoid having too many buffers
    "hot" at the same time (better for cache). */
    for (i=0; i<3; i++)
    {
        M4OSA_UInt32    topPartHeight, bottomPartHeight, width, sourceStride, destStride, y;
        M4OSA_MemAddr8    sourceWalk, destWalk;

        /* cache the vars used in the loop so as to avoid them being repeatedly fetched and
         recomputed from memory. */
        if (0 == i) /* Y plane */
        {
            bottomPartHeight = 2*shiftUV;
        }
        else /* U and V planes */
        {
            bottomPartHeight = shiftUV;
        }
        topPartHeight = PlaneOut[i].u_height - bottomPartHeight;
        width = PlaneOut[i].u_width;

        sourceWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(topPlane[i], 0, bottomPartHeight);
        sourceStride = topPlane[i].u_stride;

        destWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(PlaneOut[i], 0, 0);
        destStride = PlaneOut[i].u_stride;

        /* First the part from the top source clip frame. */
        for (y=0; y<topPartHeight; y++)
        {
            memcpy((void *)destWalk, (void *)sourceWalk, width);
            destWalk += destStride;
            sourceWalk += sourceStride;
        }

        /* and now change the vars to copy the part from the bottom source clip frame. */
        sourceWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(bottomPlane[i], 0, 0);
        sourceStride = bottomPlane[i].u_stride;

        /* destWalk is already at M4XXX_SampleAddress(PlaneOut[i], 0, topPartHeight) */

        for (y=0; y<bottomPartHeight; y++)
        {
            memcpy((void *)destWalk, (void *)sourceWalk, width);
            destWalk += destStride;
            sourceWalk += sourceStride;
        }
    }
    return M4NO_ERROR;
}

static M4OSA_ERR M4xVSS_HorizontalSlideTransition(M4VIFI_ImagePlane* leftPlane,
                                                  M4VIFI_ImagePlane* rightPlane,
                                                  M4VIFI_ImagePlane *PlaneOut,
                                                  M4OSA_UInt32    shiftUV)
{
    M4OSA_UInt32 i, y;
    /* If we shifted by exactly 0, or by the width of the target image, then we would get the left
    frame or the right frame, respectively. These cases aren't handled too well by the general
    handling, since they result in 0-size memcopies, so might as well particularize them. */

    if (0 == shiftUV)    /* output left frame */
    {
        for (i = 0; i<3; i++) /* for each YUV plane */
        {
            M4XXX_CopyPlane(&(PlaneOut[i]), &(leftPlane[i]));
        }

        return M4NO_ERROR;
    }

    if (PlaneOut[1].u_width == shiftUV) /* output right frame */
    {
        for (i = 0; i<3; i++) /* for each YUV plane */
        {
            M4XXX_CopyPlane(&(PlaneOut[i]), &(rightPlane[i]));
        }

        return M4NO_ERROR;
    }


    /* Do three loops, one for each plane type, in order to avoid having too many buffers
    "hot" at the same time (better for cache). */
    for (i=0; i<3; i++)
    {
        M4OSA_UInt32    height, leftPartWidth, rightPartWidth;
        M4OSA_UInt32    leftStride,    rightStride,    destStride;
        M4OSA_MemAddr8    leftWalk,    rightWalk,    destWalkLeft, destWalkRight;

        /* cache the vars used in the loop so as to avoid them being repeatedly fetched
        and recomputed from memory. */
        height = PlaneOut[i].u_height;

        if (0 == i) /* Y plane */
        {
            rightPartWidth = 2*shiftUV;
        }
        else /* U and V planes */
        {
            rightPartWidth = shiftUV;
        }
        leftPartWidth = PlaneOut[i].u_width - rightPartWidth;

        leftWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(leftPlane[i], rightPartWidth, 0);
        leftStride = leftPlane[i].u_stride;

        rightWalk = (M4OSA_MemAddr8)M4XXX_SampleAddress(rightPlane[i], 0, 0);
        rightStride = rightPlane[i].u_stride;

        destWalkLeft = (M4OSA_MemAddr8)M4XXX_SampleAddress(PlaneOut[i], 0, 0);
        destWalkRight = (M4OSA_MemAddr8)M4XXX_SampleAddress(PlaneOut[i], leftPartWidth, 0);
        destStride = PlaneOut[i].u_stride;

        for (y=0; y<height; y++)
        {
            memcpy((void *)destWalkLeft, (void *)leftWalk, leftPartWidth);
            leftWalk += leftStride;

            memcpy((void *)destWalkRight, (void *)rightWalk, rightPartWidth);
            rightWalk += rightStride;

            destWalkLeft += destStride;
            destWalkRight += destStride;
        }
    }

    return M4NO_ERROR;
}


M4OSA_ERR M4xVSS_SlideTransition( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                  M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                                  M4VSS3GPP_ExternalProgress *pProgress,
                                  M4OSA_UInt32 uiTransitionKind)
{
    M4xVSS_internal_SlideTransitionSettings* settings =
         (M4xVSS_internal_SlideTransitionSettings*)userData;
    M4OSA_UInt32    shiftUV;

    M4OSA_TRACE1_0("inside M4xVSS_SlideTransition");
    if ((M4xVSS_SlideTransition_RightOutLeftIn == settings->direction)
        || (M4xVSS_SlideTransition_LeftOutRightIn == settings->direction) )
    {
        /* horizontal slide */
        shiftUV = ((PlaneOut[1]).u_width * pProgress->uiProgress)/1000;
        M4OSA_TRACE1_2("M4xVSS_SlideTransition upper: shiftUV = %d,progress = %d",
            shiftUV,pProgress->uiProgress );
        if (M4xVSS_SlideTransition_RightOutLeftIn == settings->direction)
        {
            /* Put the previous clip frame right, the next clip frame left, and reverse shiftUV
            (since it's a shift from the left frame) so that we start out on the right
            (i.e. not left) frame, it
            being from the previous clip. */
            return M4xVSS_HorizontalSlideTransition(PlaneIn2, PlaneIn1, PlaneOut,
                 (PlaneOut[1]).u_width - shiftUV);
        }
        else /* Left out, right in*/
        {
            return M4xVSS_HorizontalSlideTransition(PlaneIn1, PlaneIn2, PlaneOut, shiftUV);
        }
    }
    else
    {
        /* vertical slide */
        shiftUV = ((PlaneOut[1]).u_height * pProgress->uiProgress)/1000;
        M4OSA_TRACE1_2("M4xVSS_SlideTransition bottom: shiftUV = %d,progress = %d",shiftUV,
            pProgress->uiProgress );
        if (M4xVSS_SlideTransition_TopOutBottomIn == settings->direction)
        {
            /* Put the previous clip frame top, the next clip frame bottom. */
            return M4xVSS_VerticalSlideTransition(PlaneIn1, PlaneIn2, PlaneOut, shiftUV);
        }
        else /* Bottom out, top in */
        {
            return M4xVSS_VerticalSlideTransition(PlaneIn2, PlaneIn1, PlaneOut,
                (PlaneOut[1]).u_height - shiftUV);
        }
    }

    /* Note: it might be worthwhile to do some parameter checking, see if dimensions match, etc.,
    at least in debug mode. */
}


/**
 ******************************************************************************
 * prototype    M4xVSS_FadeBlackTransition(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function apply a fade to black and then a fade from black
 * @note
 * @param    pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_FadeBlackTransition(M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                     M4VIFI_ImagePlane PlaneIn2[3],
                                     M4VIFI_ImagePlane *PlaneOut,
                                     M4VSS3GPP_ExternalProgress *pProgress,
                                     M4OSA_UInt32 uiTransitionKind)
{
    M4OSA_Int32 tmp = 0;
    M4OSA_ERR err = M4NO_ERROR;


    if((pProgress->uiProgress) < 500)
    {
        /**
         * Compute where we are in the effect (scale is 0->1024) */
        tmp = (M4OSA_Int32)((1.0 - ((M4OSA_Float)(pProgress->uiProgress*2)/1000)) * 1024 );

        /**
         * Apply the darkening effect */
        err = M4VFL_modifyLumaWithScale( (M4ViComImagePlane*)PlaneIn1,
             (M4ViComImagePlane*)PlaneOut, tmp, M4OSA_NULL);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4xVSS_FadeBlackTransition: M4VFL_modifyLumaWithScale returns\
                 error 0x%x, returning M4VSS3GPP_ERR_LUMA_FILTER_ERROR", err);
            return M4VSS3GPP_ERR_LUMA_FILTER_ERROR;
        }
    }
    else
    {
        /**
         * Compute where we are in the effect (scale is 0->1024). */
        tmp = (M4OSA_Int32)( (((M4OSA_Float)(((pProgress->uiProgress-500)*2))/1000)) * 1024 );

        /**
         * Apply the darkening effect */
        err = M4VFL_modifyLumaWithScale((M4ViComImagePlane*)PlaneIn2,
             (M4ViComImagePlane*)PlaneOut, tmp, M4OSA_NULL);
        if (M4NO_ERROR != err)
        {
            M4OSA_TRACE1_1("M4xVSS_FadeBlackTransition:\
                 M4VFL_modifyLumaWithScale returns error 0x%x,\
                     returning M4VSS3GPP_ERR_LUMA_FILTER_ERROR", err);
            return M4VSS3GPP_ERR_LUMA_FILTER_ERROR;
        }
    }


    return M4VIFI_OK;
}


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalConvertToUTF8(M4OSA_Context pContext,
 *                                                        M4OSA_Void* pBufferIn,
 *                                                        M4OSA_Void* pBufferOut,
 *                                                        M4OSA_UInt32* convertedSize)
 *
 * @brief    This function convert from the customer format to UTF8
 * @note
 * @param    pContext        (IN)    The integrator own context
 * @param    pBufferIn        (IN)    Buffer to convert
 * @param    pBufferOut        (OUT)    Converted buffer
 * @param    convertedSize    (OUT)    Size of the converted buffer
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalConvertToUTF8(M4OSA_Context pContext, M4OSA_Void* pBufferIn,
                                       M4OSA_Void* pBufferOut, M4OSA_UInt32* convertedSize)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;

    pBufferOut = pBufferIn;
    if(xVSS_context->UTFConversionContext.pConvToUTF8Fct != M4OSA_NULL
        && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
    {
        M4OSA_UInt32 ConvertedSize = xVSS_context->UTFConversionContext.m_TempOutConversionSize;

        memset((void *)xVSS_context->UTFConversionContext.pTempOutConversionBuffer,0
            ,(M4OSA_UInt32)xVSS_context->UTFConversionContext.m_TempOutConversionSize);

        err = xVSS_context->UTFConversionContext.pConvToUTF8Fct((M4OSA_Void*)pBufferIn,
            (M4OSA_UInt8*)xVSS_context->UTFConversionContext.pTempOutConversionBuffer,
                 (M4OSA_UInt32*)&ConvertedSize);
        if(err == M4xVSSWAR_BUFFER_OUT_TOO_SMALL)
        {
            M4OSA_TRACE2_1("M4xVSS_internalConvertToUTF8: pConvToUTF8Fct return 0x%x",err);

            /*free too small buffer*/
            free(xVSS_context->\
                UTFConversionContext.pTempOutConversionBuffer);

            /*re-allocate the buffer*/
            xVSS_context->UTFConversionContext.pTempOutConversionBuffer    =
                 (M4OSA_Void*)M4OSA_32bitAlignedMalloc(ConvertedSize*sizeof(M4OSA_UInt8), M4VA,
                     (M4OSA_Char *)"M4xVSS_internalConvertToUTF8: UTF conversion buffer");
            if(M4OSA_NULL == xVSS_context->UTFConversionContext.pTempOutConversionBuffer)
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertToUTF8");
                return M4ERR_ALLOC;
            }
            xVSS_context->UTFConversionContext.m_TempOutConversionSize = ConvertedSize;

            memset((void *)xVSS_context->\
                UTFConversionContext.pTempOutConversionBuffer,0,(M4OSA_UInt32)xVSS_context->\
                    UTFConversionContext.m_TempOutConversionSize);

            err = xVSS_context->UTFConversionContext.pConvToUTF8Fct((M4OSA_Void*)pBufferIn,
                (M4OSA_Void*)xVSS_context->UTFConversionContext.pTempOutConversionBuffer,
                    (M4OSA_UInt32*)&ConvertedSize);
            if(err != M4NO_ERROR)
            {
                M4OSA_TRACE1_1("M4xVSS_internalConvertToUTF8: pConvToUTF8Fct return 0x%x",err);
                return err;
            }
        }
        else if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalConvertToUTF8: pConvToUTF8Fct return 0x%x",err);
            return err;
        }
        /*decoded path*/
        pBufferOut = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        (*convertedSize) = ConvertedSize;
    }
    return M4NO_ERROR;
}


/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_internalConvertFromUTF8(M4OSA_Context pContext)
 *
 * @brief    This function convert from UTF8 to the customer format
 * @note
 * @param    pContext    (IN) The integrator own context
 * @param    pBufferIn        (IN)    Buffer to convert
 * @param    pBufferOut        (OUT)    Converted buffer
 * @param    convertedSize    (OUT)    Size of the converted buffer
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null
 ******************************************************************************
 */
M4OSA_ERR M4xVSS_internalConvertFromUTF8(M4OSA_Context pContext, M4OSA_Void* pBufferIn,
                                        M4OSA_Void* pBufferOut, M4OSA_UInt32* convertedSize)
{
    M4xVSS_Context* xVSS_context = (M4xVSS_Context*)pContext;
    M4OSA_ERR err;

    pBufferOut = pBufferIn;
    if(xVSS_context->UTFConversionContext.pConvFromUTF8Fct != M4OSA_NULL
        && xVSS_context->UTFConversionContext.pTempOutConversionBuffer != M4OSA_NULL)
    {
        M4OSA_UInt32 ConvertedSize = xVSS_context->UTFConversionContext.m_TempOutConversionSize;

        memset((void *)xVSS_context->\
            UTFConversionContext.pTempOutConversionBuffer,0,(M4OSA_UInt32)xVSS_context->\
                UTFConversionContext.m_TempOutConversionSize);

        err = xVSS_context->UTFConversionContext.pConvFromUTF8Fct\
            ((M4OSA_Void*)pBufferIn,(M4OSA_UInt8*)xVSS_context->\
                UTFConversionContext.pTempOutConversionBuffer, (M4OSA_UInt32*)&ConvertedSize);
        if(err == M4xVSSWAR_BUFFER_OUT_TOO_SMALL)
        {
            M4OSA_TRACE2_1("M4xVSS_internalConvertFromUTF8: pConvFromUTF8Fct return 0x%x",err);

            /*free too small buffer*/
            free(xVSS_context->\
                UTFConversionContext.pTempOutConversionBuffer);

            /*re-allocate the buffer*/
            xVSS_context->UTFConversionContext.pTempOutConversionBuffer    =
                (M4OSA_Void*)M4OSA_32bitAlignedMalloc(ConvertedSize*sizeof(M4OSA_UInt8), M4VA,
                     (M4OSA_Char *)"M4xVSS_internalConvertFromUTF8: UTF conversion buffer");
            if(M4OSA_NULL == xVSS_context->UTFConversionContext.pTempOutConversionBuffer)
            {
                M4OSA_TRACE1_0("Allocation error in M4xVSS_internalConvertFromUTF8");
                return M4ERR_ALLOC;
            }
            xVSS_context->UTFConversionContext.m_TempOutConversionSize = ConvertedSize;

            memset((void *)xVSS_context->\
                UTFConversionContext.pTempOutConversionBuffer,0,(M4OSA_UInt32)xVSS_context->\
                    UTFConversionContext.m_TempOutConversionSize);

            err = xVSS_context->UTFConversionContext.pConvFromUTF8Fct((M4OSA_Void*)pBufferIn,
                (M4OSA_Void*)xVSS_context->UTFConversionContext.pTempOutConversionBuffer,
                     (M4OSA_UInt32*)&ConvertedSize);
            if(err != M4NO_ERROR)
            {
                M4OSA_TRACE1_1("M4xVSS_internalConvertFromUTF8: pConvFromUTF8Fct return 0x%x",err);
                return err;
            }
        }
        else if(err != M4NO_ERROR)
        {
            M4OSA_TRACE1_1("M4xVSS_internalConvertFromUTF8: pConvFromUTF8Fct return 0x%x",err);
            return err;
        }
        /*decoded path*/
        pBufferOut = xVSS_context->UTFConversionContext.pTempOutConversionBuffer;
        (*convertedSize) = ConvertedSize;
    }


    return M4NO_ERROR;
}
