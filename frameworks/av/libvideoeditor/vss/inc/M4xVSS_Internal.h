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

#ifndef __M4XVSS_INTERNAL_H__
#define __M4XVSS_INTERNAL_H__

/**
 ******************************************************************************
 * @file    M4xVSS_Internal.h
 * @brief    Internal of Video Authoring.
 * @note
 ******************************************************************************
*/

#include "NXPSW_CompilerSwitches.h"

#include "M4MCS_API.h"
#include "M4MCS_ErrorCodes.h"

#include "M4PTO3GPP_API.h"
#include "M4PTO3GPP_ErrorCodes.h"

#include "M4AIR_API.h"

#ifdef __cplusplus
extern "C" {
#endif

#define M4_xVSS_MAJOR        1
#define M4_xVSS_MINOR        5
#define M4_xVSS_REVISION    5

/* The following defines describe the max dimensions of an input JPG */
#define M4XVSS_MX_JPG_NB_OF_PIXELS    3926016

/*Size of the UTF temporary conversion buffer keep in the VA internal context and
allocate at the initialization*/
#define UTF_CONVERSION_BUFFER_SIZE            2048

/* Max path length size */
#define  M4XVSS_MAX_PATH_LEN 256

/** Determine absolute value of a. */
#define M4xVSS_ABS(a)               ( ( (a) < (0) ) ? (-(a)) : (a) )

/** Y,U,V values in case of black borders rendering */
#define Y_PLANE_BORDER_VALUE    0x00
#define U_PLANE_BORDER_VALUE    0x80
#define V_PLANE_BORDER_VALUE    0x80

/**
 ******************************************************************************
 * struct    M4xVSS_EffectsAlphaBlending
 * @brief    Internal effects alpha blending parameters
 * @note    This structure contains all internal informations to create an alpha
 *            blending for the effects text and framing
 ******************************************************************************
*/
typedef struct
{
    M4OSA_UInt8                    m_fadeInTime;        /*Start percentage of Alpha blending*/
    M4OSA_UInt8                    m_fadeOutTime;        /*Middle percentage of Alpha blending*/
    M4OSA_UInt8                    m_end;            /*End percentage of Alpha blending*/
    M4OSA_UInt8                    m_middle;    /*Duration, in percentage of effect duration,
                                                 of the FadeIn phase*/
    M4OSA_UInt8                    m_start;    /*Duration, in percentage of effect duration,
                                                of the FadeOut phase*/

} M4xVSS_internalEffectsAlphaBlending;

/**
 ******************************************************************************
 * THIS STRUCTURE MUST NOT BE MODIFIED
 * struct    M4xVSS_FramingStruct
 * @brief    It is used internally by xVSS for framing effect, and by VPS for previewing
 ******************************************************************************
*/
typedef struct
{
    M4VIFI_ImagePlane *FramingRgb;                /**< decoded BGR565 plane */
    M4VIFI_ImagePlane *FramingYuv;                /**< converted YUV420 planar plane */
    M4OSA_Int32 duration;                        /**< Duration of the frame */
    M4OSA_Int32 previousClipTime;                /**< Previous clip time, used by framing
                                                     filter for SAVING */
    M4OSA_Int32 previewOffsetClipTime;            /**< Previous clip time, used by framing
                                                     filter for PREVIEW */
    M4OSA_Int32 previewClipTime;                /**< Current clip time, used by framing
                                                     filter for PREVIEW */
    M4OSA_Void* pCurrent;                        /**< Current M4xVSS_FramingStruct used by
                                                         framing filter */
    M4OSA_Void* pNext;                            /**< Next M4xVSS_FramingStruct, if no more,
                                                         point on current M4xVSS_FramingStruct */
    M4OSA_UInt32 topleft_x;                        /**< The top-left X coordinate in the output
                                                         picture of the first decoded pixel */
    M4OSA_UInt32 topleft_y;                        /**< The top-left Y coordinate in the output
                                                         picture of the first decoded pixel */
    M4xVSS_internalEffectsAlphaBlending* alphaBlendingStruct; /* Alpha blending Struct */
/*To support ARGB8888 : get the width and height in case of file ARGB888 used in framing
 as video effect */
    M4OSA_UInt32                width;   /*width of the ARGB8888 clip
                                        .Used only if video effect is framming */
    M4OSA_UInt32                height; /*height of the ARGB8888 clip .
                                        Used only if video effect is framming */

} M4xVSS_FramingStruct;

#ifdef DECODE_GIF_ON_SAVING
/**
 ******************************************************************************
 * THIS STRUCTURE MUST NOT BE MODIFIED
 * struct    M4xVSS_FramingContext
 * @brief    It is used internally by xVSS for framing effect, when the flag
                DECODE_GIF_ON_SAVING is activated
 ******************************************************************************
*/
typedef struct
{
    M4xVSS_FramingStruct*            aFramingCtx;        /**<Framing struct for the decoding
                                                            of the current frame of the gif*/
    M4xVSS_FramingStruct*            aFramingCtx_last;    /**<Framing struct for the decoding of
                                                             the previous frame of the gif*/
    M4OSA_FileReadPointer*            pFileReadPtr;    /**< Pointer on OSAL file read functions */
    M4OSA_FileWriterPointer*        pFileWritePtr;     /**< Pointer on OSAL file write functions */
    M4OSA_Void*                        pSPSContext;        /**<SPS context for the GIF decoding*/
    //M4SPS_Stream                    inputStream;        /**<GIF input stream buffer pointer*/
    M4OSA_Void*                        pEffectFilePath;    /**<file path of the gif*/
    M4VIDEOEDITING_VideoFrameSize    outputVideoSize;    /**< Output video size RC */
    //M4SPS_DisposalMode                disposal;            /**<previous frame GIF disposal*/
    M4OSA_UInt16                    b_animated;            /**<Is the GIF animated?*/
    M4OSA_Bool                        bEffectResize;        /**<Is the gif resize*/
    M4OSA_UInt32                    topleft_x;            /**< The top-left X coordinate in the
                                                                 output picture of the first
                                                                 decoded pixel */
    M4OSA_UInt32                    topleft_y;            /**< The top-left Y coordinate in the
                                                                 output picture of the first
                                                                 decoded pixel */
    M4OSA_UInt32                    width;                /**<GIF width, fill during the
                                                                initialization with the SPS*/
    M4OSA_UInt32                    height;                /**<GIF height, fill during the
                                                                 initialization with the SPS*/
    M4OSA_UInt32                    effectDuration;        /**<Effect duration*/
    M4OSA_Int32                        effectStartTime;    /**<Effect start time*/
    M4OSA_UInt32                    clipTime;            /**<current output clip time for the
                                                                current frame*/
    M4OSA_UInt32                    last_clipTime;        /**<previous output clip time for the
                                                                previous frame*/
    M4OSA_UInt32                    lastStepDuration;    /**<Time interval between the previous
                                                             frame and the current frame*/
    M4OSA_Bool                        b_IsFileGif;        /**<Is the framing using a gif file*/
    M4OSA_UInt32                    last_width;            /**<Last frame width*/
    M4OSA_UInt32                    last_height;        /**<Last frame height*/
    M4OSA_UInt32                    last_topleft_x;        /**<Last frame x topleft*/
    M4OSA_UInt32                    last_topleft_y;        /**<Last frame y topleft*/
    M4OSA_UInt32                    current_gif_time;    /**< Current time os the GIF in output
                                                              file time */
    M4OSA_Float                        frameDurationRatio;    /**< Frame duration ratio */
    M4xVSS_internalEffectsAlphaBlending*    alphaBlendingStruct;/*Alpha blending structure*/
#ifdef DEBUG_GIF
    M4OSA_UInt8                        uiDebug_fileCounter;/**<for debug purpose,
                                                                 count the frame of the gif*/
#endif /*DEBUG_GIF*/
}M4xVSS_FramingContext;
#endif /*DECODE_GIF_ON_SAVING*/

/**
 ******************************************************************************
 * struct    M4xVSS_Pto3GPP_params
 * @brief    Internal xVSS parameter for Pto3GPP module
 * @note    This structure is filled by M4xVSS_sendCommand function,
 * @note    and is used during M4xVSS_Step function to initialize Pto3GPP module
 * @note    All the JPG files to transform to 3GP are chained
 ******************************************************************************
*/
typedef struct {
    M4OSA_Char*                        pFileIn;
    M4OSA_Char*                        pFileOut;
    M4OSA_Char*                        pFileTemp;            /**< temporary file used for
                                                                 metadata writing, NULL is cstmem
                                                                 writer not used */
    M4OSA_UInt32                    duration;
    M4VIDEOEDITING_FileType            InputFileType;
    M4OSA_Bool                        isCreated;            /**< This boolean is used to know if
                                                                    the output file is already
                                                                    created or not */
    M4OSA_Bool                        isPanZoom;            /**< RC: Boolean used to know if the
                                                                pan and zoom mode is enabled */
    M4OSA_UInt16                    PanZoomXa;            /**< RC */
    M4OSA_UInt16                    PanZoomTopleftXa;    /**< RC */
    M4OSA_UInt16                    PanZoomTopleftYa;    /**< RC */
    M4OSA_UInt16                    PanZoomXb;            /**< RC */
    M4OSA_UInt16                    PanZoomTopleftXb;    /**< RC */
    M4OSA_UInt16                    PanZoomTopleftYb;    /**< RC */
    M4xVSS_MediaRendering            MediaRendering;        /**< FB: to render or not picture
                                                                aspect ratio */
    M4VIDEOEDITING_VideoFramerate    framerate;            /**< RC */
    M4OSA_Void*                pNext;                /**< Address of next M4xVSS_Pto3GPP_params*
                                                             element */
    /*To support ARGB8888:width and height */
    M4OSA_UInt32            width;
    M4OSA_UInt32             height;

} M4xVSS_Pto3GPP_params;

/**
 ******************************************************************************
 * struct    M4xVSS_fiftiesStruct
 * @brief    It is used internally by xVSS for fifties effect
 ******************************************************************************
*/
typedef struct
{
    M4OSA_UInt32 fiftiesEffectDuration;    /**< Duration of the same effect in a video */
    M4OSA_Int32 previousClipTime;          /**< Previous clip time, used by framing filter
                                                for SAVING */
    M4OSA_UInt32 shiftRandomValue;                /**< Vertical shift of the image */
      M4OSA_UInt32 stripeRandomValue;                /**< Horizontal position of the stripe */

} M4xVSS_FiftiesStruct;

/**
 ******************************************************************************
 * struct    M4xVSS_ColorRGB16
 * @brief    It is used internally by xVSS for RGB16 color effect
 ******************************************************************************
*/
typedef struct
{
    M4xVSS_VideoEffectType colorEffectType;    /*Color type of effect*/
    M4OSA_UInt16    rgb16ColorData;            /*RGB16 color only for the RGB16 color effect*/
} M4xVSS_ColorStruct;


/**
 ******************************************************************************
 * struct    M4xVSS_PictureCallbackCtxt
 * @brief    The Callback Context parameters for Pto3GPP
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Char*                m_FileIn;
    M4OSA_UInt32            m_NbImage;
    M4OSA_UInt32            m_ImageCounter;
    M4OSA_Double            m_timeDuration;
    M4OSA_FileReadPointer*  m_pFileReadPtr;
    M4VIFI_ImagePlane*        m_pDecodedPlane; /* Used for Pan and Zoom only */
    M4xVSS_Pto3GPP_params*    m_pPto3GPPparams;
    M4OSA_Context            m_air_context;
    M4xVSS_MediaRendering    m_mediaRendering;

} M4xVSS_PictureCallbackCtxt;

/**
 ******************************************************************************
 * enum        M4xVSS_State
 * @brief    Internal State of the xVSS
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kStateInitialized = 0,
    M4xVSS_kStateAnalyzing,
    M4xVSS_kStateOpened,
    //M4xVSS_kStateGeneratingPreview,
    //M4xVSS_kStatePreview,
    M4xVSS_kStateSaving,
    M4xVSS_kStateSaved

} M4xVSS_State;

/**
 ******************************************************************************
 * enum        M4xVSS_editMicroState
 * @brief    Internal Micro state of the xVSS for previewing/saving states
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kMicroStateEditing = 0,
    M4xVSS_kMicroStateAudioMixing

} M4xVSS_editMicroState;

/**
 ******************************************************************************
 * enum        M4xVSS_editMicroState
 * @brief    Internal Micro state of the xVSS for analyzing states
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kMicroStateAnalysePto3GPP = 0,
    M4xVSS_kMicroStateConvertPto3GPP,
    M4xVSS_kMicroStateAnalyzeMCS,
    M4xVSS_kMicroStateTranscodeMCS

} M4xVSS_analyseMicroState;


/**
 ******************************************************************************
 * struct    M4xVSS_MCS_params
 * @brief    Internal xVSS parameter for MCS module
 * @note    This structure is filled by M4xVSS_sendCommand function,
 * @note    and is used during M4xVSS_Step function to initialize MCS module
 * @note    All the input files to transcode are chained
 ******************************************************************************
*/
typedef struct {
    M4OSA_Void*                                pFileIn;
    M4OSA_Void*                                pFileOut;
    /**< temporary file used for metadata writing, NULL is cstmem writer not used */
    M4OSA_Void*                             pFileTemp;
    M4VIDEOEDITING_FileType                    InputFileType;
    M4VIDEOEDITING_FileType                    OutputFileType;
    M4VIDEOEDITING_VideoFormat                OutputVideoFormat;
    M4VIDEOEDITING_VideoFrameSize            OutputVideoFrameSize;
    M4VIDEOEDITING_VideoFramerate            OutputVideoFrameRate;
    M4VIDEOEDITING_AudioFormat                OutputAudioFormat;
    M4VIDEOEDITING_AudioSamplingFrequency    OutputAudioSamplingFrequency;
    M4OSA_Bool                                bAudioMono;
    M4VIDEOEDITING_Bitrate                    OutputVideoBitrate;
    M4VIDEOEDITING_Bitrate                    OutputAudioBitrate;
    M4OSA_Bool                                isBGM;
    /**< This boolean is used to know if the output file is already created or not */
    M4OSA_Bool                                isCreated;
    /**< Address of next M4xVSS_MCS_params* element */
    M4OSA_Void*                                pNext;

    /*FB: transcoding per parts*/
    M4OSA_UInt32                         BeginCutTime;    /**< Beginning cut time in input file */
    M4OSA_UInt32                         EndCutTime;      /**< End cut time in input file */
    M4OSA_UInt32                         OutputVideoTimescale;    /*Output timescale*/

    M4MCS_MediaRendering                 MediaRendering;   /**< FB: to crop, resize, or render
                                                                black borders*/
    M4OSA_UInt32                         videoclipnumber;
    M4OSA_UInt32  outputVideoProfile;
    M4OSA_UInt32  outputVideoLevel;
} M4xVSS_MCS_params;

/**
 ******************************************************************************
 * struct    M4xVSS_internal_AlphaMagicSettings
 * @brief    This structure defines the alpha magic transition settings
 ******************************************************************************
*/
typedef struct {
    M4VIFI_ImagePlane    *pPlane;
    M4OSA_Int32         blendingthreshold;    /**< Blending Range */
    M4OSA_Bool            isreverse;            /**< direct effect or reverse */

} M4xVSS_internal_AlphaMagicSettings;


/**
 ******************************************************************************
 * struct    M4xVSS_internal_SlideTransitionSettings
 * @brief    This structure defines the internal slide transition settings
 * @note    This type happens to match the external transition settings
 *            structure (i.e. the one which is given by the application), but are
 *            conceptually different types, so that if (or rather when) some day
 *            translation needs to occur when loading the settings from the app,
 *            this separate type will already be ready.
 ******************************************************************************
*/

typedef M4xVSS_SlideTransitionSettings    M4xVSS_internal_SlideTransitionSettings;

/**
 ******************************************************************************
 * struct    M4xVSS_internalJpegChunkMode
 * @brief    This structure defines the parameters of the chunk callback to decode
 *            a JPEG by chunk mode.
 ******************************************************************************
*/

/**
 ******************************************************************************
 * struct    M4xVSS_UTFConversionContext
 * @brief    Internal UTF conversion context
 * @note    This structure contains the UTF conversion informations
 *            needed by the xVSS to manage the different formats (UTF8/16/ASCII)
 ******************************************************************************
*/
typedef struct
{
    /*Function pointer on an external text conversion function */
    M4xVSS_toUTF8Fct                pConvToUTF8Fct;
    /*Function pointer on an external text conversion function */
    M4xVSS_fromUTF8Fct                pConvFromUTF8Fct;
    /*Temporary buffer that contains the result of each conversion*/
    M4OSA_Void*                        pTempOutConversionBuffer;
    /*Size of the previous buffer, the size is prederminated*/
    M4OSA_UInt32                    m_TempOutConversionSize;
} M4xVSS_UTFConversionContext;



/**
 ******************************************************************************
 * struct    M4xVSS_Context
 * @brief    Internal context of the xVSS
 * @note    This structure contains all internal informations needed by the xVSS
 ******************************************************************************
*/
typedef struct {
    /**< Pointer on OSAL file read functions */
    M4OSA_FileReadPointer*            pFileReadPtr;
    /**< Pointer on OSAL file write functions */
    M4OSA_FileWriterPointer*        pFileWritePtr;
    /**< Local copy of video editor settings */
    M4VSS3GPP_EditSettings*            pSettings;
    /**< Current Settings of video editor to use in step functions for preview/save */
    M4VSS3GPP_EditSettings*            pCurrentEditSettings;
    /**< Current context of video editor to use in step functions for preview/save */
    M4VSS3GPP_EditContext            pCurrentEditContext;
    /**< This is to know if a previous M4xVSS_sendCommand has already been called */
    M4OSA_UInt8                        previousClipNumber;
    /**< Audio mixing settings, needed to free it in M4xVSS_internalCloseAudioMixedFile function*/
    M4VSS3GPP_AudioMixingSettings*    pAudioMixSettings;
    /**< Audio mixing context */
    M4VSS3GPP_AudioMixingContext    pAudioMixContext;
    /**< File path for PCM output file: used for preview, given to user */
    M4OSA_Char*                        pcmPreviewFile;
    /**< Duplication of output file pointer, to be able to use audio mixing */
    M4OSA_Char*                        pOutputFile;
    /**< Duplication of temporary file pointer, to be able to use audio mixing */
    M4OSA_Char*                        pTemporaryFile;
    /**< Micro state for Saving/Previewing state */
    M4xVSS_editMicroState            editingStep;
    /**< Micro state for Analyzing state */
    M4xVSS_analyseMicroState        analyseStep;
    /**< Nb of step for analysis or save/preview. Used to compute progression
         of analysis or save/preview */
    M4OSA_UInt8                        nbStepTotal;
    /**< Current step number for analysis or save/preview */
    M4OSA_UInt8                        currentStep;
    /**< To be able to free pEffects during preview close */
    M4xVSS_PreviewSettings*            pPreviewSettings;
    /**< Temporary file path: all temporary files are created here */
    M4OSA_Char*                        pTempPath;
    /**< Current state of xVSS */
    M4xVSS_State                    m_state;
    /**< List of still pictures input to convert to 3GP with parameters */
    M4xVSS_Pto3GPP_params*            pPTo3GPPparamsList;
    /**< Current element of the above chained list beeing processd by the Pto3GPP */
    M4xVSS_Pto3GPP_params*            pPTo3GPPcurrentParams;
    /**< Current Pto3GPP context, needed to call Pto3GPP_step function in M4xVSS_step function */
    M4PTO3GPP_Context                pM4PTO3GPP_Ctxt;
    /**< Pointer on the callback function of the Pto3GPP module */
    M4xVSS_PictureCallbackCtxt*        pCallBackCtxt;
    /**< List of files to transcode with parameters */
    M4xVSS_MCS_params*                pMCSparamsList;
    /**< Current element of the above chained list beeing processd by the MCS */
    M4xVSS_MCS_params*                pMCScurrentParams;
    /**< Current MCS context, needed to call MCS_step function in M4xVSS_step function*/
    M4MCS_Context                    pMCS_Ctxt;
    /**< Index to have unique temporary filename */
    M4OSA_UInt32                    tempFileIndex;
    /**< In case of MMS use case, targeted bitrate to reach output file size */
    M4OSA_UInt32                    targetedBitrate;
    /**< If the sendCommand fct is called twice or more, the first computed timescale
        recorded here must be reused */
    M4OSA_UInt32                    targetedTimescale;

    /*UTF Conversion support*/
    M4xVSS_UTFConversionContext    UTFConversionContext;    /*UTF conversion context structure*/

} M4xVSS_Context;

/**
 * Internal function prototypes */

M4OSA_ERR M4xVSS_internalStartTranscoding(M4OSA_Context pContext,
                                          M4OSA_UInt32 *rotationDegree);

M4OSA_ERR M4xVSS_internalStopTranscoding(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalDecodeJPG(M4OSA_Void* pFileIn, M4OSA_FileReadPointer* pFileReadPtr,
                                   M4VIFI_ImagePlane** pImagePlanes);

M4OSA_ERR M4xVSS_internalConvertARGB8888toYUV420(M4OSA_Void* pFileIn,
                                                 M4OSA_FileReadPointer* pFileReadPtr,
                                                 M4VIFI_ImagePlane** pImagePlanes,
                                                 M4OSA_UInt32 width,M4OSA_UInt32 height);
M4OSA_ERR M4xVSS_internalDecodeAndResizeJPG(M4OSA_Void* pFileIn,
                                            M4OSA_FileReadPointer* pFileReadPtr,
                                            M4VIFI_ImagePlane* pImagePlanes);
M4OSA_ERR M4xVSS_internalConvertAndResizeARGB8888toYUV420(M4OSA_Void* pFileIn,
                                                          M4OSA_FileReadPointer* pFileReadPtr,
                                                          M4VIFI_ImagePlane* pImagePlanes,
                                                          M4OSA_UInt32 width,M4OSA_UInt32 height);

M4OSA_ERR M4xVSS_internalStartConvertPictureTo3gp(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalStopConvertPictureTo3gp(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalConvertRGBtoYUV(M4xVSS_FramingStruct* framingCtx);

#ifdef DECODE_GIF_ON_SAVING
M4OSA_ERR M4xVSS_internalDecodeGIF(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalDecodeGIF_Initialization(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalDecodeGIF_Cleaning(M4OSA_Context pContext);

#else
M4OSA_ERR M4xVSS_internalDecodeGIF(M4OSA_Context pContext, M4VSS3GPP_EffectSettings* pEffect,
                                   M4xVSS_FramingStruct* framingCtx);
#endif /*DECODE_GIF_ON_SAVING*/

M4OSA_ERR M4xVSS_internalConvertARGB888toYUV420_FrammingEffect(M4OSA_Context pContext,
                                                               M4VSS3GPP_EffectSettings* pEffect,
                                                               M4xVSS_FramingStruct* framingCtx,
                                                               M4VIDEOEDITING_VideoFrameSize \
                                                                    OutputVideoResolution);

M4OSA_ERR M4xVSS_internalGenerateEditedFile(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalCloseEditedFile(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalGenerateAudioMixFile(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalCloseAudioMixedFile(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalFreePreview(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalFreeSaving(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_freeSettings(M4VSS3GPP_EditSettings* pSettings);

M4OSA_ERR M4xVSS_freeCommand(M4OSA_Context pContext);

M4OSA_ERR M4xVSS_internalGetProperties(M4OSA_Context pContext, M4OSA_Char* pFile,
                                         M4VIDEOEDITING_ClipProperties *pFileProperties);

M4OSA_ERR M4xVSS_AlphaMagic( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                             M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                             M4VSS3GPP_ExternalProgress *pProgress,
                             M4OSA_UInt32 uiTransitionKind);

M4OSA_ERR M4xVSS_AlphaMagicBlending( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                     M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                                     M4VSS3GPP_ExternalProgress *pProgress,
                                     M4OSA_UInt32 uiTransitionKind);

M4OSA_ERR M4xVSS_SlideTransition( M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                  M4VIFI_ImagePlane PlaneIn2[3], M4VIFI_ImagePlane *PlaneOut,
                                  M4VSS3GPP_ExternalProgress *pProgress,
                                  M4OSA_UInt32 uiTransitionKind);

M4OSA_ERR M4xVSS_FadeBlackTransition(M4OSA_Void *userData, M4VIFI_ImagePlane PlaneIn1[3],
                                     M4VIFI_ImagePlane PlaneIn2[3],M4VIFI_ImagePlane *PlaneOut,
                                     M4VSS3GPP_ExternalProgress *pProgress,
                                     M4OSA_UInt32 uiTransitionKind);

M4OSA_ERR M4xVSS_internalGetTargetedTimeScale(M4OSA_Context pContext,
                                              M4VSS3GPP_EditSettings* pSettings,
                                              M4OSA_UInt32* pTargetedTimeScale);

M4OSA_ERR M4xVSS_internalConvertToUTF8(M4OSA_Context pContext, M4OSA_Void* pBufferIn,
                                       M4OSA_Void* pBufferOut, M4OSA_UInt32* convertedSize);


M4OSA_ERR M4xVSS_internalConvertFromUTF8(M4OSA_Context pContext, M4OSA_Void* pBufferIn,
                                         M4OSA_Void* pBufferOut, M4OSA_UInt32* convertedSize);
#ifdef __cplusplus
}
#endif /* __cplusplus */


#endif /* __M4XVSS_INTERNAL_H__ */

