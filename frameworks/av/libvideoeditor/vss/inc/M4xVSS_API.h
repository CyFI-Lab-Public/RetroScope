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

#ifndef __M4XVSS_API_H__
#define __M4XVSS_API_H__

#ifdef __cplusplus
extern "C" {
#endif
/**
 ******************************************************************************
 * @file    M4xVSS_API.h
 * @brief    API of Video Studio 2.1
 * @note
 ******************************************************************************
*/

#define M4VSS_SUPPORT_EXTENDED_FEATURES

#include "M4VSS3GPP_API.h"
#include "M4VSS3GPP_Extended_API.h"
#include "M4DECODER_Common.h"
/* Errors codes */

/**
 * End of analyzing => the user can call M4xVSS_PreviewStart or M4xVSS_SaveStart */
#define M4VSS3GPP_WAR_ANALYZING_DONE                  M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0001)

/**
 * End of preview generating => the user can launch vps to see preview. Once preview is over,
   the user must call M4xVSS_PreviewStop() to be able to save edited file, or to call another
   M4xVSS_SendCommand() */
#define M4VSS3GPP_WAR_PREVIEW_READY                   M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0002)

/**
 * End of saved file generation => the user must call M4xVSS_SaveStop() */
#define M4VSS3GPP_WAR_SAVING_DONE                     M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0003)

/**
 * Transcoding is necessary to go further -> if the user does not want to continue,
  he must call M4xVSS_sendCommand() */
#define M4VSS3GPP_WAR_TRANSCODING_NECESSARY           M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0004)

/**
 * In case of MMS, the output file size won't be reached */
#define M4VSS3GPP_WAR_OUTPUTFILESIZE_EXCEED           M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0005)

/**
 * JPG input file dimensions are too high */
#define M4VSS3GPP_ERR_JPG_TOO_BIG                     M4OSA_ERR_CREATE( M4_ERR, M4VS, 0x0001)

/**
 * UTF Conversion, warning on the size of the temporary converted buffer*/
#define M4xVSSWAR_BUFFER_OUT_TOO_SMALL                M4OSA_ERR_CREATE( M4_WAR, M4VS, 0x0006)

/**
 * SWIKAR :Error whan NO_MORE_SPACE*/
#define M4xVSSERR_NO_MORE_SPACE                       M4OSA_ERR_CREATE( M4_ERR, M4VS, 0x0007)

/**
 ******************************************************************************
 * enum     M4xVSS_VideoEffectType
 * @brief   This enumeration defines the video effect types of the xVSS
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kVideoEffectType_BlackAndWhite = M4VSS3GPP_kVideoEffectType_External+1, /* 257 */
    M4xVSS_kVideoEffectType_Pink,                                                  /* 258 */
    M4xVSS_kVideoEffectType_Green,                                                 /* 259 */
    M4xVSS_kVideoEffectType_Sepia,                                                 /* 260 */
    M4xVSS_kVideoEffectType_Negative,                                              /* 261 */
    M4xVSS_kVideoEffectType_Framing,                                               /* 262 */
    M4xVSS_kVideoEffectType_Text, /* Text overlay */                               /* 263 */
    M4xVSS_kVideoEffectType_ZoomIn,                                                /* 264 */
    M4xVSS_kVideoEffectType_ZoomOut,                                               /* 265 */
    M4xVSS_kVideoEffectType_Fifties,                                                /*266 */
    M4xVSS_kVideoEffectType_ColorRGB16,                                                /*267 */
    M4xVSS_kVideoEffectType_Gradient                                                /*268*/
} M4xVSS_VideoEffectType;

/**
 ******************************************************************************
 * enum     M4xVSS_VideoTransitionType
 * @brief   This enumeration defines the video effect that can be applied during a transition.
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kVideoTransitionType_External = M4VSS3GPP_kVideoTransitionType_External, /*256*/
    M4xVSS_kVideoTransitionType_AlphaMagic,
    M4xVSS_kVideoTransitionType_SlideTransition,
    M4xVSS_kVideoTransitionType_FadeBlack

} M4xVSS_VideoTransitionType;

/**
 ******************************************************************************
 * struct    M4xVSS_PreviewSettings
 * @brief    This structure gathers all the information needed by the VPS for preview
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Void                                *p3gpPreviewFile;
    M4OSA_Void                                *pPCMFile;
    M4VIDEOEDITING_AudioSamplingFrequency    outPCM_ASF;
    M4OSA_Bool                                bAudioMono;
    M4VSS3GPP_EffectSettings                   *Effects;
    M4OSA_UInt8                                nbEffects;

} M4xVSS_PreviewSettings;

/**
 ******************************************************************************
 * prototype    M4xVSS_toUTF8Fct
 * @brief        This prototype defines the function implemented by the integrator
 *                to convert a string encoded in any format to an UTF8 string.
 * @note
 *
 * @param    pBufferIn        IN            Buffer containing the string to convert to UTF8
 * @param    pBufferOut        IN            Buffer containing the UTF8 converted string
 * @param    bufferOutSize    IN/OUT    IN:     Size of the given output buffer
 *                                    OUT: Size of the converted buffer
 *
 ******************************************************************************
*/
typedef M4OSA_ERR (*M4xVSS_toUTF8Fct)
(
    M4OSA_Void            *pBufferIn,
    M4OSA_UInt8            *pBufferOut,
    M4OSA_UInt32        *bufferOutSize
);


/**
 ******************************************************************************
 * prototype    M4xVSS_fromUTF8Fct
 * @brief        This prototype defines the function implemented by the integrator
 *                to convert an UTF8 string to a string encoded in any format.
 * @note
 *
 * @param    pBufferIn        IN            Buffer containing the UTF8 string to convert
 *                                        to the desired format.
 * @param    pBufferOut        IN            Buffer containing the converted string
 * @param    bufferOutSize    IN/OUT    IN:     Size of the given output buffer
 *                                    OUT: Size of the converted buffer
 *
 ******************************************************************************
*/
typedef M4OSA_ERR (*M4xVSS_fromUTF8Fct)
(
    M4OSA_UInt8            *pBufferIn,
    M4OSA_Void            *pBufferOut,
    M4OSA_UInt32        *bufferOutSize
);




/**
 ******************************************************************************
 * struct    M4xVSS_InitParams
 * @brief    This structure defines parameters for xVSS.
 * @note
 ******************************************************************************
*/
typedef struct
{
    M4OSA_FileReadPointer*            pFileReadPtr;
    M4OSA_FileWriterPointer*        pFileWritePtr;
    M4OSA_Void*                        pTempPath;
    /*Function pointer on an external text conversion function */
    M4xVSS_toUTF8Fct                pConvToUTF8Fct;
    /*Function pointer on an external text conversion function */
    M4xVSS_fromUTF8Fct                pConvFromUTF8Fct;



} M4xVSS_InitParams;

/**
 ******************************************************************************
 * prototype    M4xVSS_Init
 * @brief        This function initializes the xVSS
 * @note        Initializes the xVSS edit operation (allocates an execution context).
 *
 * @param    pContext            (OUT) Pointer on the xVSS edit context to allocate
 * @param    params                (IN) Parameters mandatory for xVSS
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_Init(M4OSA_Context* pContext, M4xVSS_InitParams* params);

/**
 ******************************************************************************
 * prototype    M4xVSS_ReduceTranscode
 * @brief        This function changes the given editing structure in order to
 *                minimize the transcoding time.
 * @note        The xVSS analyses this structure, and if needed, changes the
 *                output parameters (Video codec, video size, audio codec,
 *                audio nb of channels) to minimize the transcoding time.
 *
 * @param    pContext            (OUT) Pointer on the xVSS edit context to allocate
 * @param    pSettings            (IN) Edition settings (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_ReduceTranscode(M4OSA_Context pContext, M4VSS3GPP_EditSettings* pSettings);

/**
 ******************************************************************************
 * prototype    M4xVSS_SendCommand
 * @brief        This function gives to the xVSS an editing structure
 * @note        The xVSS analyses this structure, and prepare edition
 *                This function must be called after M4xVSS_Init, after
 *                M4xVSS_CloseCommand, or after M4xVSS_PreviewStop.
 *                After this function, the user must call M4xVSS_Step until
 *                it returns another error than M4NO_ERROR.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    pSettings            (IN) Edition settings (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_SendCommand(M4OSA_Context pContext, M4VSS3GPP_EditSettings* pSettings);

/**
 ******************************************************************************
 * prototype    M4xVSS_PreviewStart
 * @brief        This function prepare the preview
 * @note        The xVSS create 3GP preview file and fill pPreviewSettings with
 *                preview parameters.
 *                This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_ANALYZING_DONE
 *                After this function, the user must call M4xVSS_Step until
 *                it returns another error than M4NO_ERROR.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    pPreviewSettings    (IN) Preview settings (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_PreviewStart(M4OSA_Context pContext, M4xVSS_PreviewSettings* pPreviewSettings);

/**
 ******************************************************************************
 * prototype    M4xVSS_PreviewStop
 * @brief        This function unallocate preview ressources and change xVSS
 *                internal state to allow saving or resend an editing command
 * @note        This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_PREVIEW_READY
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_PreviewStop(M4OSA_Context pContext);

/**
 ******************************************************************************
 * prototype    M4xVSS_SaveStart
 * @brief        This function prepare the save
 * @note        The xVSS create 3GP edited final file
 *                This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_ANALYZING_DONE
 *                After this function, the user must call M4xVSS_Step until
 *                it returns another error than M4NO_ERROR.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    pFilePath            (IN) If the user wants to provide a different
 *                                output filename, else can be NULL (allocated by the user)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        Memory allocation has failed
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_SaveStart(M4OSA_Context pContext, M4OSA_Void* pFilePath,
                            M4OSA_UInt32 filePathSize);

/**
 ******************************************************************************
 * prototype    M4xVSS_SaveStop
 * @brief        This function unallocate save ressources and change xVSS
 *                internal state.
 * @note        This function must be called once M4xVSS_Step has returned
 *                M4VSS3GPP_WAR_SAVING_DONE
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_SaveStop(M4OSA_Context pContext);

/**
 ******************************************************************************
 * prototype    M4xVSS_Step
 * @brief        This function executes differents tasks, depending of xVSS
 *                internal state.
 * @note        This function:
 *                    - analyses editing structure if called after M4xVSS_SendCommand
 *                    - generates preview file if called after M4xVSS_PreviewStart
 *                    - generates final edited file if called after M4xVSS_SaveStart
 *
 * @param    pContext                        (IN) Pointer on the xVSS edit context
 * @param    pContext                        (OUT) Progress indication from 0 to 100
 * @return    M4NO_ERROR:                        No error, the user must call M4xVSS_Step again
 * @return    M4ERR_PARAMETER:                At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:                    This function cannot not be called at this time
 * @return    M4VSS3GPP_WAR_PREVIEW_READY:    Preview file is generated
 * @return    M4VSS3GPP_WAR_SAVING_DONE:        Final edited file is generated
 * @return    M4VSS3GPP_WAR_ANALYZING_DONE:    Analyse is done
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_Step(M4OSA_Context pContext, M4OSA_UInt8 *pProgress);

/**
 ******************************************************************************
 * prototype    M4xVSS_CloseCommand
 * @brief        This function deletes current editing profile, unallocate
 *                ressources and change xVSS internal state.
 * @note        After this function, the user can call a new M4xVSS_SendCommand
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_CloseCommand(M4OSA_Context pContext);

/**
 ******************************************************************************
 * prototype    M4xVSS_CleanUp
 * @brief        This function deletes all xVSS ressources
 * @note        This function must be called after M4xVSS_CloseCommand.
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_CleanUp(M4OSA_Context pContext);

/**
 ******************************************************************************
 * prototype    M4xVSS_GetVersion(M4_VersionInfo *pVersion)
 * @brief        This function get the version of the Video Studio 2.1
 *
 * @param    pVersion            (IN) Pointer on the version info struct
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_GetVersion(M4_VersionInfo *pVersion);

/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectColor(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function apply a color effect on an input YUV420 planar frame
 * @note    The prototype of this effect function is exposed because it needs to
 *            called by the VPS during the preview
 * @param    pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectColor
(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind
);

/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectFraming(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function add a fixed or animated image on an input YUV420 planar frame
 * @note    The prototype of this effect function is exposed because it needs to
 *            called by the VPS during the preview
 * @param    pFunctionContext(IN) Contains which color to apply (not very clean ...)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectFraming
(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind
);

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
M4OSA_ERR M4VSS3GPP_externalVideoEffectFifties
(
    M4OSA_Void *pUserData,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pPlaneOut,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind
);


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_externalVideoEffectZoom(M4OSA_Void *pFunctionContext,
 *                                                    M4VIFI_ImagePlane *PlaneIn,
 *                                                    M4VIFI_ImagePlane *PlaneOut,
 *                                                    M4VSS3GPP_ExternalProgress *pProgress,
 *                                                    M4OSA_UInt32 uiEffectKind)
 *
 * @brief    This function add a fixed or animated image on an input YUV420 planar frame
 * @note    The prototype of this effect function is exposed because it needs to
 *            called by the VPS during the preview
 * @param    pFunctionContext(IN) Contains which zoom to apply (In/Out)
 * @param    PlaneIn            (IN) Input YUV420 planar
 * @param    PlaneOut        (IN/OUT) Output YUV420 planar
 * @param    pProgress        (IN/OUT) Progress indication (0-100)
 * @param    uiEffectKind    (IN) Unused
 *
 * @return    M4VIFI_OK:    No error
 ******************************************************************************
*/
M4OSA_ERR M4VSS3GPP_externalVideoEffectZoom
(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiEffectKind
);

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_CreateClipSettings()
 * @brief    Allows filling a clip settings structure with default values
 *
 * @note    WARNING: pClipSettings->Effects[ ] will be allocated in this function.
 *                   pClipSettings->pFile      will be allocated in this function.
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   pFile               (IN) Clip file name
 * @param   filePathSize        (IN) Size of the clip path (needed for the UTF16 conversion)
 * @param    nbEffects           (IN) Nb of effect settings to allocate
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_CreateClipSettings(M4VSS3GPP_ClipSettings *pClipSettings, M4OSA_Void* pFile,
                                    M4OSA_UInt32 filePathSize, M4OSA_UInt8 nbEffects);

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_DuplicateClipSettings()
 * @brief    Duplicates a clip settings structure, performing allocations if required
 *
 * @param    pClipSettingsDest    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param    pClipSettingsOrig    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   bCopyEffects        (IN) Flag to know if we have to duplicate effects
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_DuplicateClipSettings(M4VSS3GPP_ClipSettings *pClipSettingsDest,
                                         M4VSS3GPP_ClipSettings *pClipSettingsOrig,
                                         M4OSA_Bool bCopyEffects);

/**
 ******************************************************************************
 * M4OSA_ERR M4xVSS_FreeClipSettings()
 * @brief    Free the pointers allocated in the ClipSetting structure (pFile, Effects).
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_FreeClipSettings(M4VSS3GPP_ClipSettings *pClipSettings);

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_getMCSContext(M4OSA_Context pContext, M4OSA_Context* mcsContext)
 * @brief        This function returns the MCS context within the xVSS internal context
 * @note        This function must be called only after VSS state has moved to analyzing state
 *                or beyond
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    mcsContext        (OUT) Pointer to pointer of mcs context to return
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_getMCSContext(M4OSA_Context pContext, M4OSA_Context* mcsContext);

/**
 ******************************************************************************
 * prototype    M4OSA_ERR M4xVSS_getVSS3GPPContext(M4OSA_Context pContext,
 *                                                     M4OSA_Context* mcsContext)
 * @brief        This function returns the VSS3GPP context within the xVSS internal context
 * @note        This function must be called only after VSS state has moved to Generating
 *                preview or beyond
 *
 * @param    pContext            (IN) Pointer on the xVSS edit context
 * @param    vss3gppContext        (OUT) Pointer to pointer of vss3gpp context to return
 * @return    M4NO_ERROR:        No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_STATE:        This function cannot not be called at this time
 ******************************************************************************
*/
M4OSA_ERR M4xVSS_getVSS3GPPContext(M4OSA_Context pContext, M4OSA_Context* vss3gppContext);

// Get supported video decoders and capabilities.
M4OSA_ERR M4xVSS_getVideoDecoderCapabilities(M4DECODER_VideoDecoders **decoders);
#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __M4XVSS_API_H__ */

