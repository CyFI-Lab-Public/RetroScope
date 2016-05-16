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
 * @file    M4PTO3GPP_API.h
 * @brief    The Pictures to 3GPP Converter.
 * @note    M4PTO3GPP produces 3GPP compliant audio/video  files
 *            from an AMR NB audio file and raw pictures into a MPEG-4/h263 3GPP file.
 ******************************************************************************
 */

#ifndef __M4PTO3GPP_API_H__
#define __M4PTO3GPP_API_H__

/**
 *    OSAL basic types and errors */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/**
 *    OSAL types for file access */
#include "M4OSA_FileReader.h"
#include "M4OSA_FileWriter.h"

/**
 *    Definition of M4_VersionInfo */
#include "M4TOOL_VersionInfo.h"

/**
 * Definitions of M4VIFI_ImagePlane */
#include "M4VIFI_FiltersAPI.h"

/**
 * Common definitions of video editing components */
#include "M4_VideoEditingCommon.h"


#ifdef __cplusplus
extern "C" {
#endif

/**
 *    Public type of the M4PTO3GPP context */
typedef M4OSA_Void* M4PTO3GPP_Context;


/**
 ******************************************************************************
 * enum        M4PTO3GPP_ReplaceAudioMode
 * @brief    This enumeration defines the way the audio is managed if it is shorter than the video
 ******************************************************************************
 */
typedef enum
{
    M4PTO3GPP_kAudioPaddingMode_None = 0,  /**< Audio track is kept shorter than the video track*/
    M4PTO3GPP_kAudioPaddingMode_Silence,   /**< If audio is shorter, silence is added at the end*/
    M4PTO3GPP_kAudioPaddingMode_Loop       /**< If audio is shorter, loop back to the beginning
                                                when the whole track has been processed */
} M4PTO3GPP_AudioPaddingMode;


/**
 ******************************************************************************
 * struct    M4PTO3GPP_OutputFileMaxSize
 * @brief    Defines the maximum size of the 3GPP file produced by the PTO3GPP
 ******************************************************************************
 */
typedef enum
{
    M4PTO3GPP_k50_KB,            /**< Output 3GPP file size is limited to 50 Kbytes  */
    M4PTO3GPP_k75_KB,            /**< Output 3GPP file size is limited to 75 Kbytes  */
    M4PTO3GPP_k100_KB,           /**< Output 3GPP file size is limited to 100 Kbytes */
    M4PTO3GPP_k150_KB,           /**< Output 3GPP file size is limited to 150 Kbytes */
    M4PTO3GPP_k200_KB,           /**< Output 3GPP file size is limited to 200 Kbytes */
    M4PTO3GPP_k300_KB,           /**< Output 3GPP file size is limited to 300 Kbytes */
    M4PTO3GPP_k400_KB,           /**< Output 3GPP file size is limited to 400 Kbytes */
    M4PTO3GPP_k500_KB,           /**< Output 3GPP file size is limited to 500 Kbytes */
    M4PTO3GPP_kUNLIMITED=-1      /**< Output 3GPP file size is not limited           */
} M4PTO3GPP_OutputFileMaxSize;

/**
 ******************************************************************************
 * M4OSA_ERR (M4PTO3GPP_PictureCallbackFct) (M4OSA_Void* pPictureCtxt,
 * M4VIFI_ImagePlane* pImagePlanes, M4OSA_Double* pPictureDuration);
 * @brief    The integrator must implement a function following this prototype.
 *            Its goal is to feed the PTO3GPP with YUV420 pictures.
 *
 * @note    This function is given to the PTO3GPP in the M4PTO3GPP_Params structure
 * @param    pContext    (IN) The integrator own context
 * @param    pImagePlanes(IN/OUT) Pointer to an array of three valid image planes
 * @param    pPictureDuration(OUT) Duration of the returned picture
 *
 * @return    M4NO_ERROR:    No error
 * @return    M4PTO3GPP_WAR_LAST_PICTURE: The returned image is the last one
 * @return    M4ERR_PARAMETER: At least one of the function parameters is null (bebug only)
 ******************************************************************************
 */
typedef M4OSA_ERR (M4PTO3GPP_PictureCallbackFct) (M4OSA_Void* pPictureCtxt,
                                                  M4VIFI_ImagePlane* pImagePlanes,
                                                  M4OSA_Double* pPictureDuration);


/**
 ******************************************************************************
 * struct    M4PTO3GPP_Params
 * @brief    M4PTO3GPP parameters definition
 ******************************************************************************
 */
typedef struct
{
    /**< Output video compression format, H263 or MPEG4 */
    M4VIDEOEDITING_VideoFormat      OutputVideoFormat;
    /**< Output frame size : SQCIF to VGA*/
    M4VIDEOEDITING_VideoFrameSize   OutputVideoFrameSize;
    /**< Targeted Output bit-rate, see enum*/
    M4VIDEOEDITING_Bitrate          OutputVideoBitrate;
    /**< Maximum size of the output 3GPP file, see enum */
    M4PTO3GPP_OutputFileMaxSize     OutputFileMaxSize;
    /**< Callback function to be called by the PTO3GPP to get the input pictures*/
    M4PTO3GPP_PictureCallbackFct*   pPictureCallbackFct;
    /**< Context to be given as third argument of the picture callback function call*/
    M4OSA_Void*                     pPictureCallbackCtxt;
    /**< File descriptor of the input audio track file */
    M4OSA_Void*                     pInputAudioTrackFile;
    /**< Format of the audio file */
    M4VIDEOEDITING_FileType         AudioFileFormat;
    /**< Type of processing to apply when audio is shorter than video*/
    M4PTO3GPP_AudioPaddingMode      AudioPaddingMode;
    /**< File descriptor of the output 3GPP file */
    M4OSA_Void*                     pOutput3gppFile;
     /**< File descriptor of the temporary file to store metadata ("moov.bin") */
    M4OSA_Void*                     pTemporaryFile;
    /**< Number of input YUV frames to encode */
    M4OSA_UInt32                    NbVideoFrames;
    M4OSA_Int32   videoProfile;
    M4OSA_Int32   videoLevel;
} M4PTO3GPP_Params;

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_GetVersion(M4_VersionInfo* pVersionInfo);
 * @brief    Get the M4PTO3GPP version.
 * @note    Can be called anytime. Do not need any context.
 * @param    pVersionInfo        (OUT) Pointer to a version info structure
 * @return    M4NO_ERROR:            No error
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_GetVersion(M4_VersionInfo* pVersionInfo);

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Init(M4PTO3GPP_Context* pContext);
 * @brief    Initializes the M4PTO3GPP (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the M4PTO3GPP context to allocate
 * @param   pFileReadPtrFct     (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL
 * @return    M4ERR_ALLOC:        The context structure could not be allocated
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_Init(M4PTO3GPP_Context* pContext, M4OSA_FileReadPointer* pFileReadPtrFct,
                         M4OSA_FileWriterPointer* pFileWritePtrFct);

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Open(M4PTO3GPP_Context pContext, M4PTO3GPP_Params* pParams);
 * @brief    Set the M4PTO3GPP input and output files.
 * @note    It opens the input file, but the output file may not be created yet.
 * @param    pContext            (IN) M4PTO3GPP context
 * @param    pParams                (IN) Pointer to the parameters for the PTO3GPP.
 * @note    The pointed structure can be de-allocated after this function returns because
 *            it is internally copied by the PTO3GPP
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        M4PTO3GPP is not in an appropriate state
 *                                for this function to be called
 * @return    M4ERR_ALLOC:        There is no more available memory
 * @return    ERR_PTO3GPP_INVALID_VIDEO_FRAME_SIZE_FOR_H263 The output video frame
 *                                size parameter is incompatible with H263 encoding
 * @return    ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FORMAT
 *                          The output video format  parameter is undefined
 * @return    ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_BITRATE
 *                        The output video bit-rate parameter is undefined
 * @return    ERR_PTO3GPP_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE
 *                        The output video frame size parameter is undefined
 * @return    ERR_PTO3GPP_UNDEFINED_OUTPUT_FILE_SIZE
 *                          The output file size parameter is undefined
 * @return    ERR_PTO3GPP_UNDEFINED_AUDIO_PADDING
 *                        The output audio padding parameter is undefined
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_Open(M4PTO3GPP_Context pContext, M4PTO3GPP_Params* pParams);

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Step(M4PTO3GPP_Context pContext);
 * @brief    Perform one step of trancoding.
 * @note
 * @param    pContext            (IN) M4PTO3GPP context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL
 * @return    M4ERR_STATE:        M4PTO3GPP is not in an appropriate state
 *                                for this function to be called
 * @return    M4PTO3GPP_WAR_END_OF_PROCESSING:    Encoding completed
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_Step(M4PTO3GPP_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_Close(M4PTO3GPP_Context pContext);
 * @brief    Finish the M4PTO3GPP transcoding.
 * @note    The output 3GPP file is ready to be played after this call
 * @param    pContext            (IN) M4PTO3GPP context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL
 * @return    M4ERR_STATE:        M4PTO3GPP is not in an appropriate state
 *                                for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_Close(M4PTO3GPP_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4PTO3GPP_CleanUp(M4PTO3GPP_Context pContext);
 * @brief    Free all resources used by the M4PTO3GPP.
 * @note    The context is no more valid after this call
 * @param    pContext            (IN) M4PTO3GPP context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL
 ******************************************************************************
 */
M4OSA_ERR M4PTO3GPP_CleanUp(M4PTO3GPP_Context pContext);


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4PTO3GPP_API_H__ */

