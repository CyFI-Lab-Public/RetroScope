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
 * @file    M4VPP_API.h
 * @brief    Video preprocessing API public functions prototypes.
 * @note
 ******************************************************************************
*/

#ifndef M4VPP_API_H
#define M4VPP_API_H

#include "M4OSA_Types.h"            /**< Include for common OSAL types */
#include "M4OSA_Error.h"            /**< Include for common OSAL errors */

/**
 *    Include Video filters interface definition (for the M4VIFI_ImagePlane type) */
#include "M4VIFI_FiltersAPI.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


/**
 ******************************************************************************
 * Public type of the Video Preprocessing execution context
 ******************************************************************************
*/
typedef M4OSA_Void*    M4VPP_Context;

typedef enum
{
    M4VPP_kIYUV420=0,    /**< YUV 4:2:0 planar (standard input for mpeg-4 video) */
    M4VPP_kIYUV422,        /**< YUV422 planar */
    M4VPP_kIYUYV,        /**< YUV422 interlaced, luma first */
    M4VPP_kIUYVY,        /**< YUV422 interlaced, chroma first */
    M4VPP_kIJPEG,        /**< JPEG compressed frames */
    M4VPP_kIRGB444,        /**< RGB 12 bits 4:4:4 */
    M4VPP_kIRGB555,        /**< RGB 15 bits 5:5:5 */
    M4VPP_kIRGB565,        /**< RGB 16 bits 5:6:5 */
    M4VPP_kIRGB24,        /**< RGB 24 bits 8:8:8 */
    M4VPP_kIRGB32,        /**< RGB 32 bits  */
    M4VPP_kIBGR444,        /**< BGR 12 bits 4:4:4 */
    M4VPP_kIBGR555,        /**< BGR 15 bits 5:5:5 */
    M4VPP_kIBGR565,        /**< BGR 16 bits 5:6:5 */
    M4VPP_kIBGR24,        /**< BGR 24 bits 8:8:8 */
    M4VPP_kIBGR32        /**< BGR 32 bits  */
} M4VPP_InputVideoFormat;


/**
 ******************************************************************************
 * @brief    Prototype of the main video preprocessing function
 * @note    Preprocess one frame
 * @param    pContext:    (IN) Execution context of the VPP.
 * @param    pPlaneIn:    (INOUT)    Input Image
 * @param    pPlaneOut:    (INOUT)    Output Image
 ******************************************************************************
*/
typedef M4OSA_ERR (M4VPP_apply_fct) (M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
                                     M4VIFI_ImagePlane* pPlaneOut);



/**
 ******************************************************************************
 * M4OSA_ERR M4VPP_initVideoPreprocessing(M4VPP_Context* pContext)
 * @brief    This function allocates a new execution context for the Video Preprocessing component.
 * @note
 * @param    pContext:    (OUT) Execution context allocated by the function.
 * @return    M4NO_ERROR: there is no error.
 * @return    M4ERR_ALLOC: there is no more available memory.
 * @return    M4ERR_PARAMETER: pContext is NULL (debug only).
 ******************************************************************************
*/
M4OSA_ERR M4VPP_initVideoPreprocessing(M4VPP_Context* pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4VPP_applyVideoPreprocessing(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
 *                                           M4VIFI_ImagePlane* pPlaneOut)
 * @brief    Preprocess one frame.
 * @note
 * @param    pContext:    (IN) Execution context.
 * @param    pPlaneIn:    (INOUT)    Input Image
 * @param    pPlaneOut:    (INOUT)    Output Image
 * @return    M4NO_ERROR: there is no error.
 * @return    M4ERR_PARAMETER: pContext or pPlaneIn or pPlaneOut is NULL (debug only).
 * @return    M4ERR_STATE: Video Preprocessing is not in an appropriate state for this function
 *                           to be called
 ******************************************************************************
*/
M4OSA_ERR M4VPP_applyVideoPreprocessing(M4VPP_Context pContext, M4VIFI_ImagePlane* pPlaneIn,
                                         M4VIFI_ImagePlane* pPlaneOut);

/**
 ******************************************************************************
 * M4OSA_ERR M4VPP_cleanUpVideoPreprocessing(M4VPP_Context pContext)
 * @brief    This method frees the execution context for the Video Preprocessing component.
 *            Any further usage of the context will lead to unpredictable result.
 * @note
 * @param    pContext:    (IN) Execution context.
 * @return    M4NO_ERROR: there is no error.
 * @return    M4ERR_PARAMETER: pContext is NULL (debug only).
 ******************************************************************************
*/
M4OSA_ERR M4VPP_cleanUpVideoPreprocessing(M4VPP_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4VPP_setVideoPreprocessingMode(M4VPP_Context pContext, M4VES_InputVideoFormat format)
 * @brief    This method apply the video preprocessing to the input plane. Result is put into the
 *           output plan.
 * @param    pContext:    (IN) Execution context.
 * @param    format  :    (IN) Format of input plane (rgb, yuv, ...)
 * @return    M4NO_ERROR: there is no error
 ******************************************************************************
*/
M4OSA_ERR M4VPP_setVideoPreprocessingMode(M4VPP_Context pContext, M4VPP_InputVideoFormat format);

/**
 ******************************************************************************
 * @brief    Definition of the errors specific to this module.
 ******************************************************************************
*/

/**< Input and output planes have incompatible properties */
#define M4VPP_ERR_IMCOMPATIBLE_IN_AND_OUT_PLANES    M4OSA_ERR_CREATE( M4_ERR,\
     M4PREPROCESS_VIDEO, 0x000001);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* M4VPP_API_H */

