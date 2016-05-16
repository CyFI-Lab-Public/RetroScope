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

#ifndef __M4VSS3GPP_API_H__
#define __M4VSS3GPP_API_H__

/**
 ******************************************************************************
 * @file    M4VSS3GPP_API.h
 * @brief   Video Studio Service 3GPP public API.
 * @note    VSS allows editing 3GPP files.
 *          It is a straightforward and fully synchronous API.
 ******************************************************************************
 */

/**
 *  OSAL basic types and errors */
#include "M4OSA_Types.h"
#include "M4OSA_Error.h"

/**
 *  OSAL types for file access */
#include "M4OSA_FileReader.h"
#include "M4OSA_FileWriter.h"

/**
 *  Definition of M4_VersionInfo */
#include "M4TOOL_VersionInfo.h"

/**
 * Image planes definition */
#include "M4VIFI_FiltersAPI.h"

/**
 * Common definitions of video editing components */
#include "M4_VideoEditingCommon.h"
#include "M4ENCODER_AudioCommon.h"
#include "M4AD_Common.h"
#include "M4DA_Types.h"

/**
 * Extended API (xVSS) */
#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES
#include "M4VSS3GPP_Extended_API.h"
#endif

//#include "M4VD_HW_API.h"
//#include "M4VE_API.h"


#ifdef __cplusplus
extern "C" {
#endif



/**
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 *
 *      Edition Feature
 *
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 */

/**
 *  Public type of the VSS edit context */
typedef M4OSA_Void* M4VSS3GPP_EditContext;


/**
 ******************************************************************************
 * enum     M4VSS3GPP_VideoEffectType
 * @brief   This enumeration defines the video effect types of the VSS3GPP
 ******************************************************************************
 */
typedef enum
{
    M4VSS3GPP_kVideoEffectType_None           = 0,  /**< No video effect */
    M4VSS3GPP_kVideoEffectType_FadeFromBlack  = 8,  /**< Intended for begin effect */
    M4VSS3GPP_kVideoEffectType_FadeToBlack    = 16, /**< Intended for end effect */
    M4VSS3GPP_kVideoEffectType_External       = 256 /**< External effect function is used */
    /* reserved 256 + n */                          /**< External effect number n */

} M4VSS3GPP_VideoEffectType;


/**
 ******************************************************************************
 * enum     M4VSS3GPP_AudioEffectType
 * @brief   This enumeration defines the audio effect types of the VSS3GPP
 ******************************************************************************
 */
typedef enum
{
    M4VSS3GPP_kAudioEffectType_None    = 0,
    M4VSS3GPP_kAudioEffectType_FadeIn  = 8, /**< Intended for begin effect */
    M4VSS3GPP_kAudioEffectType_FadeOut = 16 /**< Intended for end effect */

} M4VSS3GPP_AudioEffectType;


/**
 ******************************************************************************
 * enum     M4VSS3GPP_VideoTransitionType
 * @brief   This enumeration defines the video effect that can be applied during a transition.
 ******************************************************************************
 */
typedef enum
{
    M4VSS3GPP_kVideoTransitionType_None      = 0,
    M4VSS3GPP_kVideoTransitionType_CrossFade = 1,
    M4VSS3GPP_kVideoTransitionType_External  = 256
    /* reserved 256 + n */                          /**< External transition number n */

} M4VSS3GPP_VideoTransitionType;


/**
 ******************************************************************************
 * enum     M4VSS3GPP_AudioTransitionType
 * @brief   This enumeration defines the audio effect that can be applied during a transition.
 ******************************************************************************
 */
typedef enum
{
    M4VSS3GPP_kAudioTransitionType_None = 0,
    M4VSS3GPP_kAudioTransitionType_CrossFade

} M4VSS3GPP_AudioTransitionType;


/**
 ******************************************************************************
 * struct   M4VSS3GPP_ExternalProgress
 * @brief   This structure contains information provided to the external Effect
 *          and Transition functions
 * @note    The uiProgress value should be enough for most cases
 ******************************************************************************
 */
typedef struct
{
    /**< Progress of the Effect or the Transition, from 0 to 1000 (one thousand) */
    M4OSA_UInt32    uiProgress;
    /**< Index of the current clip (first clip in case of a Transition), from 0 to N */
    //M4OSA_UInt8     uiCurrentClip;
    /**< Current time, in milliseconds, in the current clip time-line */
    M4OSA_UInt32    uiClipTime;
    /**< Current time, in milliseconds, in the output clip time-line */
    M4OSA_UInt32    uiOutputTime;
    M4OSA_Bool        bIsLast;

} M4VSS3GPP_ExternalProgress;


/**
 ************************************************************************
 * enum     M4VSS3GPP_codecType
 * @brief    This enum defines the codec types used to create interfaces
 * @note    This enum is used internally by the VSS3GPP services to identify
 *             a currently supported codec interface. Each codec is
 *            registered with one of this type associated.
 *            When a codec instance is needed, this type is used to
 *            identify and retrieve its interface.
 *            This can be extended for other codecs.
 ************************************************************************
 */
typedef enum
{
    /* Video Decoder Types */
    M4VSS3GPP_kVideoDecMPEG4 = 0,
    M4VSS3GPP_kVideoDecH264,

    /* Video Encoder Types */
    M4VSS3GPP_kVideoEncMPEG4,
    M4VSS3GPP_kVideoEncH263,
    M4VSS3GPP_kVideoEncH264,

    /* Audio Decoder Types */
    M4VSS3GPP_kAudioDecAMRNB,
    M4VSS3GPP_kAudioDecAAC,
    M4VSS3GPP_kAudioDecMP3,

    /* Audio Encoder Types */
    M4VSS3GPP_kAudioEncAMRNB,
    M4VSS3GPP_kAudioEncAAC,

    /* number of codecs, keep it as last enum entry, before invlaid type */
    M4VSS3GPP_kCodecType_NB,
    /* invalid codec type */
    M4VSS3GPP_kCodecTypeInvalid = 255

} M4VSS3GPP_codecType;


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_editVideoEffectFct
 * @brief       Begin and End video effect functions implemented by the integrator
 *              must match this prototype.
 * @note        The function is provided with the original image of the clip.
 *              It must apply the video effect to build the output image.
 *              The progress of the effect is given, on a scale from 0 to 1000.
 *              When the effect function is called, all the image plane structures
 *              and buffers are valid and owned by the VSS 3GPP.
 *
 * @param   pFunctionContext    (IN) The function context, previously set by the integrator
 * @param   pInputPlanes        (IN) Input YUV420 image: pointer to an array of three valid
                                     image planes (Y, U and V)
 * @param   pOutputPlanes       (IN/OUT) Output (filtered) YUV420 image: pointer to an array
                                         of three valid image planes (Y, U and V)
 * @param   pProgress           (IN) Set of information about the video transition progress.
 * @param   uiExternalEffectId  (IN) Which effect function should be used (for external effects)
 *
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 ******************************************************************************
 */
typedef M4OSA_ERR (*M4VSS3GPP_editVideoEffectFct)
(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pInputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiExternalEffectId
);


/**
 ******************************************************************************
 * prototype    M4VSS3GPP_editVideoTransitionFct
 * @brief       External transition functions implemented by the integrator
 *              must match this prototype.
 * @note        The function is provided with the image of the first clip and
 *              the image of the second clip. It must build the output image
 *              from the two input images.
 *              The progress of the transition is given, on a scale from 0 to 1000.
 *              When the external function is called, all the image plane
 *              structures and buffers are valid and owned by the VSS 3GPP.
 *
 * @param   pFunctionContext    (IN) The function context, previously set by the integrator
 * @param   pClip1InputPlanes   (IN) First input YUV420 image: pointer to an array of three
                                     valid image planes (Y, U and V)
 * @param   pClip2InputPlanes   (IN) Second input YUV420 image: pointer to an array of three
                                     valid image planes (Y, U and V)
 * @param   pOutputPlanes       (IN/OUT) Output (filtered) YUV420 image: pointer to an array
                                         of three valid image planes (Y, U and V)
 * @param   pProgress           (IN) Set of information about the video effect progress.
 * @param   uiExternalTransitionId    (IN) Which transition function should be used
                                            (for external transitions)
 *
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 ******************************************************************************
 */
typedef M4OSA_ERR (*M4VSS3GPP_editVideoTransitionFct)
(
    M4OSA_Void *pFunctionContext,
    M4VIFI_ImagePlane *pClip1InputPlanes,
    M4VIFI_ImagePlane *pClip2InputPlanes,
    M4VIFI_ImagePlane *pOutputPlanes,
    M4VSS3GPP_ExternalProgress *pProgress,
    M4OSA_UInt32 uiExternalTransitionId
);


/**
 ******************************************************************************
 * struct   M4VSS3GPP_EffectSettings
 * @brief   This structure defines an audio/video effect for the edition.
 * @note    Effect start time is relative to output clip.
 ******************************************************************************
 */
typedef struct
{
    M4OSA_UInt32                 uiStartTime;           /**< In ms */
    M4OSA_UInt32                 uiDuration;            /**< In ms */
    M4VSS3GPP_VideoEffectType    VideoEffectType;       /**< None, FadeIn, FadeOut, etc. */
    M4VSS3GPP_editVideoEffectFct ExtVideoEffectFct;     /**< External effect function */
    M4OSA_Void                  *pExtVideoEffectFctCtxt;/**< Context given to the external
                                                             effect function */
    M4VSS3GPP_AudioEffectType    AudioEffectType;       /**< None, FadeIn, FadeOut */

#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES
    M4xVSS_EffectSettings         xVSS;
#endif

} M4VSS3GPP_EffectSettings;


/**
 ******************************************************************************
 * enum        M4VSS3GPP_TransitionBehaviour
 * @brief    Transition behavior
 ******************************************************************************
 */
typedef enum
{
    M4VSS3GPP_TransitionBehaviour_SpeedUp = 0,
    M4VSS3GPP_TransitionBehaviour_Linear,
    M4VSS3GPP_TransitionBehaviour_SpeedDown,
    M4VSS3GPP_TransitionBehaviour_SlowMiddle,
    M4VSS3GPP_TransitionBehaviour_FastMiddle
} M4VSS3GPP_TransitionBehaviour;


/**
 ******************************************************************************
 * struct   M4VSS3GPP_TransitionSettings
 * @brief   This structure defines the transition to be applied when assembling two clips.
 ******************************************************************************
 */
typedef struct
{
    /**< Duration of the transition, in milliseconds (set to 0 to get no transition) */
    M4OSA_UInt32                     uiTransitionDuration;

    /**< Type of the video transition */
    M4VSS3GPP_VideoTransitionType    VideoTransitionType;

    /**< External transition video effect function */
    M4VSS3GPP_editVideoTransitionFct ExtVideoTransitionFct;

    /**< Context of the external transition video effect function */
    M4OSA_Void                      *pExtVideoTransitionFctCtxt;
    M4VSS3GPP_AudioTransitionType    AudioTransitionType;   /**< Type of the audio transition */
    M4VSS3GPP_TransitionBehaviour     TransitionBehaviour;    /**<Transition behaviour*/

#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES
    M4xVSS_TransitionSettings        xVSS;
#endif

} M4VSS3GPP_TransitionSettings;


/**
 ******************************************************************************
 * struct   M4VSS3GPP_ClipSettings
 * @brief   This structure defines an input clip for the edition.
 * @note    It also contains the settings for the cut and begin/end effects applied to the clip.
 ******************************************************************************
 */
typedef struct
{
    M4OSA_Void                     *pFile;            /**< Clip file descriptor */
    M4VIDEOEDITING_FileType         FileType;         /**< .3gp, .amr, .mp3     */
    M4OSA_UInt32                    filePathSize;      /**< Clip path size
                                                           (add because of UTF16 conversion)*/
    M4VIDEOEDITING_ClipProperties   ClipProperties;   /**< Clip analysis previously computed
                                                       with M4VSS3GPP_editAnalyseClip */
    M4OSA_UInt32                    uiBeginCutTime;   /**< Begin cut time, in milliseconds */
    M4OSA_UInt32                    uiEndCutTime;     /**< End cut time, in milliseconds */
    M4OSA_Bool                      bTranscodingRequired;

#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES
    M4xVSS_ClipSettings             xVSS;
#endif

} M4VSS3GPP_ClipSettings;


/**
 ******************************************************************************
 * struct   M4VSS3GPP_EditSettings
 * @brief   This structure gathers all the information needed to define a complete
 *          edition operation
 ******************************************************************************
 */
typedef struct
{
      /**< Number of element of the clip list pClipList */
    M4OSA_UInt8                      uiClipNumber;
    /**< The properties of this clip will be used as a reference for compatibility checking */
    M4OSA_UInt8                      uiMasterClip;
    /**< List of the input clips settings. Pointer to an array of uiClipNumber
     clip settings pointers */
    M4VSS3GPP_ClipSettings           **pClipList;
    /**< List of the transition settings. Pointer to an array of uiClipNumber-1
     transition settings pointers */
    M4VSS3GPP_TransitionSettings     **pTransitionList;
    M4VSS3GPP_EffectSettings         *Effects;         /**< List of effects */
    M4OSA_UInt8                         nbEffects;     /**< Number of effects in the above list */
    /**< Frame rate at which the modified video sections will be encoded */
    M4VIDEOEDITING_VideoFramerate    videoFrameRate;
    M4OSA_Void                       *pOutputFile;      /**< Output 3GPP clip file descriptor */
    M4OSA_UInt32                     uiOutputPathSize;    /**< Output file path size*/
    /**< Temporary file to store metadata ("moov.bin") */
    M4OSA_Void                       *pTemporaryFile;

#ifdef M4VSS_SUPPORT_EXTENDED_FEATURES
    M4xVSS_EditSettings              xVSS;
#endif
    M4OSA_Float                    PTVolLevel;
} M4VSS3GPP_EditSettings;


/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editAnalyseClip()
 * @brief   This function allows checking if a clip is compatible with VSS 3GPP editing
 * @note    It also fills a ClipAnalysis structure, which can be used to check if two
 *          clips are compatible
 * @param   pClip               (IN) File descriptor of the input 3GPP/MP3 clip file.
 * @param   pClipProperties     (IN) Pointer to a valid ClipProperties structure.
 * @param   FileType            (IN) Type of the input file (.3gp, .amr, .mp3)
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return   M4VSS3GPP_ERR_H263_PROFILE_NOT_SUPPORTED
 * @return   M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION
 * @return   M4VSS3GPP_ERR_AMR_EDITING_UNSUPPORTED
 * @return   M4VSS3GPP_ERR_EDITING_UNSUPPORTED_H263_PROFILE
 * @return   M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_PROFILE
 * @return   M4VSS3GPP_ERR_EDITING_UNSUPPORTED_MPEG4_RVLC
 * @return   M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT
 * @return   M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_VIDEO_STREAM_IN_FILE
 * @return   M4VSS3GPP_ERR_EDITING_UNSUPPORTED_AUDIO_FORMAT
 * @return   M4VSS3GPP_ERR_EDITING_NO_SUPPORTED_STREAM_IN_FILE
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editAnalyseClip(M4OSA_Void *pClip, M4VIDEOEDITING_FileType FileType,
                                    M4VIDEOEDITING_ClipProperties  *pClipProperties,
                                    M4OSA_FileReadPointer *pFileReadPtrFct);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editCheckClipCompatibility()
 * @brief   This function allows checking if two clips are compatible with each other
 *          for VSS 3GPP editing assembly feature.
 * @note
 * @param   pClip1Properties        (IN) Clip analysis of the first clip
 * @param   pClip2Properties        (IN) Clip analysis of the second clip
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_VERSION
 * @return  M4VSS3GPP_ERR_INVALID_CLIP_ANALYSIS_PLATFORM
 * @return  M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FORMAT
 * @return  M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_FRAME_SIZE
 * @return  M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_TIME_SCALE
 * @return  M4VSS3GPP_ERR_INCOMPATIBLE_VIDEO_DATA_PARTITIONING
 * @return  M4VSS3GPP_ERR_UNSUPPORTED_MP3_ASSEMBLY
 * @return  M4VSS3GPP_ERR_UNSUPPORTED_INPUT_VIDEO_FORMAT
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editCheckClipCompatibility(M4VIDEOEDITING_ClipProperties  *pClip1Properties,
                                               M4VIDEOEDITING_ClipProperties  *pClip2Properties);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editInit()
 * @brief    Initializes the VSS 3GPP edit operation (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the VSS 3GPP edit context to allocate
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return    M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editInit(
    M4VSS3GPP_EditContext* pContext,
    M4OSA_FileReadPointer* pFileReadPtrFct,
    M4OSA_FileWriterPointer* pFileWritePtrFct );

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editCreateClipSettings()
 * @brief    Allows filling a clip settings structure with default values
 *
 * @note    WARNING: pClipSettings->pFile      will be allocated in this function.
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   pFile               (IN) Clip file name
 * @param   filePathSize        (IN) Size of the clip path (needed for UTF16 conversion)
 * @param    nbEffects           (IN) Nb of effect settings to allocate
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editCreateClipSettings(M4VSS3GPP_ClipSettings *pClipSettings,
                                           M4OSA_Void* pFile, M4OSA_UInt32 filePathSize,
                                           M4OSA_UInt8 nbEffects);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editDuplicateClipSettings()
 * @brief    Duplicates a clip settings structure, performing allocations if required
 *
 * @param    pClipSettingsDest    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param    pClipSettingsOrig    (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @param   bCopyEffects        (IN) Flag to know if we have to duplicate effects (deprecated)
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editDuplicateClipSettings(M4VSS3GPP_ClipSettings *pClipSettingsDest,
                                              M4VSS3GPP_ClipSettings *pClipSettingsOrig,
                                              M4OSA_Bool bCopyEffects);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editFreeClipSettings()
 * @brief    Free the pointers allocated in the ClipSetting structure (pFile, Effects).
 *
 * @param    pClipSettings        (IN) Pointer to a valid M4VSS3GPP_ClipSettings structure
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pClipSettings is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editFreeClipSettings(M4VSS3GPP_ClipSettings *pClipSettings);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editOpen()
 * @brief   Set the VSS 3GPP input and output files, and set the settings.
 * @note
 * @param   pContext            (IN) VSS 3GPP edit context
 * @param   pSettings           (IN) Edit settings
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        VSS is not in an appropriate state for this function to be called
 * @return  M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editOpen(M4VSS3GPP_EditContext pContext, M4VSS3GPP_EditSettings *pSettings);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editStep()
 * @brief   Perform one step of editing.
 * @note
 * @param   pContext                (IN) VSS 3GPP edit context
 * @param   pProgress               (OUT) Progress percentage (0 to 100) of the editing operation
 * @return  M4NO_ERROR:             No error
 * @return  M4ERR_PARAMETER:        pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:            VSS 3GPP is not in an appropriate state for this function to
 *                                  be called
 * @return  M4VSS3GPP_WAR_EDITING_DONE:Edition is done, user should now call M4VSS3GPP_editClose()
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editStep(M4VSS3GPP_EditContext pContext, M4OSA_UInt8 *pProgress);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editClose()
 * @brief   Finish the VSS 3GPP edit operation.
 * @note    The output 3GPP file is ready to be played after this call
 * @param   pContext            (IN) VSS 3GPP edit context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        VSS 3GPP is not in an appropriate state for this function
 *                              to be called
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editClose(M4VSS3GPP_EditContext pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_editCleanUp()
 * @brief   Free all resources used by the VSS 3GPP edit operation.
 * @note    The context is no more valid after this call
 * @param   pContext            (IN) VSS 3GPP edit context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_editCleanUp(M4VSS3GPP_EditContext pContext);

/**
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 *
 *      Audio Mixing Feature
 *
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 */
/**
 *  Public type of the VSS audio mixing context */
typedef M4OSA_Void* M4VSS3GPP_AudioMixingContext;


/**
 ******************************************************************************
 * struct   M4VSS3GPP_AudioMixingSettings
 * @brief   This structure defines the settings of the audio mixing operation.
 ******************************************************************************
 */
typedef struct {
    M4OSA_Void*                             pOriginalClipFile;      /**< Input 3GPP clip file */
    M4OSA_Void*                             pAddedAudioTrackFile;   /**< New audio track */
    M4VIDEOEDITING_FileType                 AddedAudioFileType;     /**< File Format of the new audio file */
    M4OSA_UInt32                            uiAddCts;               /**< Time, in milliseconds,
                                                                    at which the added audio track is inserted */
    M4OSA_UInt32                            uiAddVolume;            /**< Volume, in percentage,
                                                                        of the added audio track */
    M4OSA_UInt32                            uiBeginLoop;            /**< Describes in milli-second the
                                                                        start time of the loop */
    M4OSA_UInt32                            uiEndLoop;              /**< Describes in milli-second the end
                                                                    time of the loop (0 means no loop) */
    M4OSA_Bool                              bRemoveOriginal;      /**< If true, the original audio track
                                                                     is not taken into account */
    M4OSA_Void*                             pOutputClipFile;      /**< Output 3GPP clip file */
    M4OSA_Void*                             pTemporaryFile;       /**< Temporary file to store metadata
                                                     ("moov.bin") */
    /**< The following parameters are optionnal. They are just used in case of MP3 replacement. */
    M4VIDEOEDITING_AudioSamplingFrequency   outputASF;         /**< Output sampling frequency */
    M4VIDEOEDITING_AudioFormat              outputAudioFormat; /**< Output audio codec(AAC/AMR)*/
    M4VIDEOEDITING_Bitrate                  outputAudioBitrate; /**< Output audio bitrate */
    M4OSA_UInt8                             outputNBChannels; /**< Output audio nb of channels */
    M4OSA_Bool                              b_DuckingNeedeed;
    M4OSA_Int32                             InDucking_threshold;
    M4OSA_Float                             fBTVolLevel;
    M4OSA_Float                             fPTVolLevel;
    M4OSA_Float                             InDucking_lowVolume;
    M4OSA_Bool                              bLoop;
    M4OSA_UInt32                            uiSamplingFrequency;
    M4OSA_UInt32                            uiNumChannels;
} M4VSS3GPP_AudioMixingSettings;

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingInit(M4VSS3GPP_AudioMixingContext* pContext,
 *                                     M4VSS3GPP_AudioMixingSettings* pSettings)
 * @brief    Initializes the VSS audio mixing operation (allocates an execution context).
 * @note
 * @param    pContext        (OUT) Pointer on the VSS audio mixing context to allocate
 * @param    pSettings        (IN) Pointer to valid audio mixing settings
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @param   pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return    M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_audioMixingInit(
    M4VSS3GPP_AudioMixingContext* pContext,
    M4VSS3GPP_AudioMixingSettings* pSettings,
    M4OSA_FileReadPointer* pFileReadPtrFct,
    M4OSA_FileWriterPointer* pFileWritePtrFct );

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingStep()
 * @brief   Perform one step of audio mixing.
 * @note
 * @param   pContext                        (IN) VSS 3GPP audio mixing context
 * @return  M4NO_ERROR:                     No error
 * @return  M4ERR_PARAMETER:                pContext is M4OSA_NULL (debug only)
 * @param   pProgress                       (OUT) Progress percentage (0 to 100)
                                                  of the finalization operation
 * @return  M4ERR_STATE:                    VSS is not in an appropriate state for
                                            this function to be called
 * @return  M4VSS3GPP_WAR_END_OF_AUDIO_MIXING: Audio mixing is over, user should
                                               now call M4VSS3GPP_audioMixingCleanUp()
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_audioMixingStep(M4VSS3GPP_AudioMixingContext pContext,
                                     M4OSA_UInt8 *pProgress);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_audioMixingCleanUp()
 * @brief   Free all resources used by the VSS audio mixing operation.
 * @note    The context is no more valid after this call
 * @param   pContext            (IN) VSS 3GPP audio mixing context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_audioMixingCleanUp(M4VSS3GPP_AudioMixingContext pContext);


/**
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 *
 *      Extract Picture Feature
 *
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 */
/**
 *  Public type of the VSS extract picture context */
typedef M4OSA_Void* M4VSS3GPP_ExtractPictureContext;

/**
 ******************************************************************************
 * struct   M4VSS3GPP_ExtractPictureSettings
 * @brief   This structure defines the settings of the extract picture audio operation.
 ******************************************************************************
 */
typedef struct {
    M4OSA_Void*                         pInputClipFile;  /**< Input 3GPP clip file */
    M4OSA_Int32                         iExtractionTime; /**< frame time (in ms) to be extracted */
    M4OSA_Void*                         pOutputYuvPic;   /**< Output YUV picture name */
} M4VSS3GPP_ExtractPictureSettings;


/******************************************************************************
 * M4OSA_ERR M4VSS3GPP_extractPictureInit()
 * @brief    Initializes the VSS extract picture operation (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the VSS extract picture context to allocate
 * @param    pSettings            (IN) Pointer to valid extract picture settings
 * @param    pWidth                (OUT) video stream width
 * @param    pHeight                (OUT) video stream height
 * @param    pFileReadPtrFct        (IN) Pointer to OSAL file reader functions
 * @return    M4NO_ERROR:                        No error
 * @return    M4ERR_PARAMETER:                At least one parameter is M4OSA_NULL (debug only)
 * @return    M4ERR_ALLOC:                    There is no more available memory
 * @return    M4VSS3GPP_ERR_INVALID_CLIP1:    The input clip is empty
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_extractPictureInit(
        M4VSS3GPP_ExtractPictureContext* pContext,
        M4VSS3GPP_ExtractPictureSettings* pSettings,
        M4OSA_UInt32 *pWidth,
        M4OSA_UInt32 *pHeight,
        M4OSA_FileReadPointer* pFileReadPtrFct );

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_extractPictureStep()
 * @brief   Perform one step of picture extraction.
 * @note
 * @param   pContext                        (IN) VSS extract picture context
 * @return  M4NO_ERROR:                     No error
 * @return  M4ERR_PARAMETER:                pContext is M4OSA_NULL (debug only)
 * @param   pDecPlanes                      (OUT) Plane in wich the extracted picture is copied
 * @param   pProgress                       (OUT) Progress percentage (0 to 100)
                                                 of the picture extraction
 * @return  M4ERR_STATE:                    VSS is not in an appropriate state for this
                                            function to be called
 * @return  VSS_WAR_END_OF_EXTRACT_PICTURE: Picture extraction  is over, user should now
                                            call M4VSS3GPP_extractPictureCleanUp()
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_extractPictureStep(M4VSS3GPP_ExtractPictureContext pContext,
                                       M4VIFI_ImagePlane *pDecPlanes, M4OSA_UInt8 *pProgress);

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_extractPictureCleanUp()
 * @brief   Free all resources used by the VSS picture extraction.
 * @note    The context is no more valid after this call
 * @param   pContext            (IN) VSS extract picture context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_extractPictureCleanUp(M4VSS3GPP_ExtractPictureContext pContext);

/**
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 *
 *      Common features
 *
 ******************************************************************************
 ******************************************************************************
 ******************************************************************************
 */

/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_GetVersion()
 * @brief   Get the VSS version.
 * @note    Can be called anytime. Do not need any context.
 * @param   pVersionInfo        (OUT) Pointer to a version info structure
 * @return  M4NO_ERROR:         No error
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_GetVersion(M4_VersionInfo* pVersionInfo);


#ifdef WIN32
/**
 ******************************************************************************
 * M4OSA_ERR M4VSS3GPP_GetErrorMessage()
 * @brief   Return a string describing the given error code
 * @note    The input string must be already allocated (and long enough!)
 * @param   err             (IN) Error code to get the description from
 * @param   sMessage        (IN/OUT) Allocated string in which the description will be copied
 * @return  M4NO_ERROR:     Input error is from the VSS3GPP module
 * @return  M4ERR_PARAMETER:Input error is not from the VSS3GPP module
 ******************************************************************************
 */
M4OSA_ERR M4VSS3GPP_GetErrorMessage(M4OSA_ERR err, M4OSA_Char* sMessage);
#endif /**< WIN32 */


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4VSS3GPP_API_H__ */

