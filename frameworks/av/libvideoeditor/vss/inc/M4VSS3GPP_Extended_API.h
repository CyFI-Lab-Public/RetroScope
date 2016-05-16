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

#ifndef __M4VSS3GPP_EXTENDED_API_H__
#define __M4VSS3GPP_EXTENDED_API_H__

/**
 ******************************************************************************
 * @file    M4VSS3GPP_Extended_API.h
 * @brief    API of xVSS
 * @note
 ******************************************************************************
*/

#ifndef M4VSS_SUPPORT_EXTENDED_FEATURES
#error "*** the flag M4VSS_SUPPORT_EXTENDED_FEATURES should be activated in CompilerSwitches\
             for VideoStudio ***"
#endif

/**
 ******************************************************************************
 * prototype    M4xVSS_getTextRgbBufferFct
 * @brief        External text to RGB buffer functions implemented by the integrator
 *                must match this prototype.
 * @note        The function is provided with the renderingData, the text buffer and
 *                its size. It must build the output RGB image plane containing the text.
 *
 * @param   pRenderingData    (IN) The data given by the user in M4xVSS_EffectSettings
 * @param    pTextBuffer        (IN) Text buffer given by the user in M4xVSS_EffectSettings
 * @param    textBufferSize    (IN) Text buffer size given by the user in M4xVSS_EffectSettings
 * @param    pOutputPlane    (IN/OUT) Output RGB565 image
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 ******************************************************************************
*/
typedef M4OSA_ERR (*M4xVSS_getTextRgbBufferFct)
(
    M4OSA_Void *pRenderingData,
    M4OSA_Void *pTextBuffer,
    M4OSA_UInt32 textBufferSize,
    M4VIFI_ImagePlane **pOutputPlane
);

/**
 ******************************************************************************
 * struct    M4xVSS_BGMSettings
 * @brief    This structure gathers all the information needed to add Background music to 3gp file
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Void                  *pFile;         /**< Input file path */
    M4VIDEOEDITING_FileType     FileType;       /**< .3gp, .amr, .mp3     */
    M4OSA_UInt32                uiAddCts;       /**< Time, in milliseconds, at which the added
                                                      audio track is inserted */
    M4OSA_UInt32                uiAddVolume;     /**< Volume, in percentage, of the added audio track */
    M4OSA_UInt32                uiBeginLoop;    /**< Describes in milli-second the start time
                                                     of the loop */
    M4OSA_UInt32                uiEndLoop;      /**< Describes in milli-second the end time of the
                                                     loop (0 means no loop) */
    M4OSA_Bool                  b_DuckingNeedeed;
    M4OSA_Int32                 InDucking_threshold;  /**< Threshold value at which background
                                                            music shall duck */
    M4OSA_Float                 lowVolume;       /**< lower the background track to this factor
                                                 and increase the primary track to inverse of this factor */
    M4OSA_Bool                  bLoop;
    M4OSA_UInt32                uiSamplingFrequency;
    M4OSA_UInt32                uiNumChannels;
} M4xVSS_BGMSettings;


/**
 ******************************************************************************
 * enum     M4VSS3GPP_VideoEffectType
 * @brief   This enumeration defines the video effect types of the VSS3GPP
 ******************************************************************************
*/
typedef enum
{
    M4VSS3GPP_kRGB888           = 0,  /**< RGB888 data type */
    M4VSS3GPP_kRGB565           = 1  /**< RGB565 data type */

} M4VSS3GPP_RGBType;

/**
 ******************************************************************************
 * struct   M4xVSS_EffectSettings
 * @brief   This structure defines an audio/video effect for the edition.
 ******************************************************************************
*/
typedef struct
{
    /**< In percent of the cut clip duration */
    M4OSA_UInt32               uiStartPercent;
    /**< In percent of the ((clip duration) - (effect starttime)) */
    M4OSA_UInt32               uiDurationPercent;
    /**< Framing file path (GIF/PNG file), used only if VideoEffectType == framing */
    M4OSA_Void                 *pFramingFilePath;
    /**< Framing RGB565 buffer,  used only if VideoEffectType == framing */
    M4VIFI_ImagePlane          *pFramingBuffer;
    /**<RGB Buffer type,used only if VideoEffectType == framing */
    M4VSS3GPP_RGBType          rgbType;
    /**< The top-left X coordinate in the output picture where the added frame will be displayed.
     Used only if VideoEffectType == framing || VideoEffectType == text */
    M4OSA_UInt32               topleft_x;
    /**< The top-left Y coordinate in the output picture where the added frame will be displayed.
     Used only if VideoEffectType == framing || VideoEffectType == text */
    M4OSA_UInt32               topleft_y;
    /**< Does framing image is resized to output video size.
     Used only if VideoEffectType == framing */
    M4OSA_Bool                 bResize;
    M4VIDEOEDITING_VideoFrameSize framingScaledSize;
/**< Size to which the the framing file needs to be resized */
    /**< Text buffer. Used only if VideoEffectType == text */
    M4OSA_Void*                pTextBuffer;
    /**< Text buffer size. Used only if VideoEffectType == text */
    M4OSA_UInt32               textBufferSize;
    /**< Pointer containing specific data used by the font engine (size, color...) */
    M4OSA_Void*                pRenderingData;
    /**< Text plane width. Used only if VideoEffectType == text */
    M4OSA_UInt32               uiTextBufferWidth;
    /**< Text plane height. Used only if VideoEffectType == text */
    M4OSA_UInt32               uiTextBufferHeight;
    /**< Processing rate of the effect added when using the Fifties effect */
    M4OSA_UInt32               uiFiftiesOutFrameRate;
    /**< RGB16 input color of the effect added when using the rgb16 color effect */
    M4OSA_UInt16               uiRgb16InputColor;

    M4OSA_UInt8                uialphaBlendingStart;       /*Start percentage of Alpha blending*/
    M4OSA_UInt8                uialphaBlendingMiddle;      /*Middle percentage of Alpha blending*/
    M4OSA_UInt8                uialphaBlendingEnd;         /*End percentage of Alpha blending*/
    M4OSA_UInt8                uialphaBlendingFadeInTime;  /*Duration, in percentage of
                                                            effect duration, of the FadeIn phase*/
    M4OSA_UInt8                uialphaBlendingFadeOutTime;   /*Duration, in percentage of effect
                                                                duration, of the FadeOut phase*/
    M4OSA_UInt32                width;   /*width of the ARGB8888 clip .
                                            Used only if video effect is framming */
    M4OSA_UInt32                height; /*height of the ARGB8888 clip .
                                            Used only if video effect is framming */
} M4xVSS_EffectSettings;

/**
 ******************************************************************************
 * struct    M4xVSS_AlphaMagicSettings
 * @brief    This structure defines the alpha magic transition settings
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Void*            pAlphaFilePath;        /**< Alpha file path (JPG file)  */
    M4OSA_Int32            blendingPercent;    /**< Blending Percentage between 0 and 100 */
    M4OSA_Bool             isreverse;            /**< direct effect or reverse */
    /*To support ARGB8888 : get the width and height */
    M4OSA_UInt32            width;
    M4OSA_UInt32            height;
} M4xVSS_AlphaMagicSettings;

/**
 ******************************************************************************
 * enum        M4xVSS_SlideTransition_Direction
 * @brief    Defines directions for the slide transition
 ******************************************************************************
*/

typedef enum {
    M4xVSS_SlideTransition_RightOutLeftIn,
    M4xVSS_SlideTransition_LeftOutRightIn,
    M4xVSS_SlideTransition_TopOutBottomIn,
    M4xVSS_SlideTransition_BottomOutTopIn
} M4xVSS_SlideTransition_Direction;

/**
 ******************************************************************************
 * struct    M4xVSS_AlphaMagicSettings
 * @brief    This structure defines the slide transition settings
 ******************************************************************************
*/

typedef struct
{
    M4xVSS_SlideTransition_Direction direction; /* direction of the slide */
} M4xVSS_SlideTransitionSettings;

/**
 ******************************************************************************
 * struct   M4xVSS_TransitionSettings
 * @brief   This structure defines additional transition settings specific to
 *            xVSS, which are appended to the VSS3GPP transition settings
 *            structure.
 ******************************************************************************
*/
typedef struct
{
    /* Anything xVSS-specific, but common to all transitions, would go here,
    before the union. */
    union {
        /**< AlphaMagic settings, used only if VideoTransitionType ==
            M4xVSS_kVideoTransitionType_AlphaMagic */
        M4xVSS_AlphaMagicSettings        *pAlphaMagicSettings;
        /* only in case of slide transition. */
        M4xVSS_SlideTransitionSettings    *pSlideTransitionSettings;
    } transitionSpecific;
} M4xVSS_TransitionSettings;


/**
 ******************************************************************************
 * enum        M4xVSS_MediaRendering
 * @brief    This enum defines different media rendering using exif orientation
 ******************************************************************************
*/
typedef enum
{
    M4xVSS_kResizing = 0,        /*The picture is resized, the aspect ratio can be different
                                    from the original one. All of the picture is rendered*/
    M4xVSS_kCropping,            /*The picture is cropped, the aspect ratio is the same as
                                    the original one. The picture is not rendered entirely*/
    M4xVSS_kBlackBorders        /*Black borders are rendered in order to keep the original
                                    aspect ratio. All the picture is rendered*/

} M4xVSS_MediaRendering;


/**
 ******************************************************************************
 * struct   M4xVSS_ClipSettings
 * @brief   This structure defines an input clip for the edition.
 * @note    It also contains the settings for the cut and begin/end effects applied to the clip.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_UInt32                    uiBeginCutPercent;    /**< Begin cut time, in percent of clip
                                                                duration (only for 3GPP clip !) */
    M4OSA_UInt32                    uiEndCutPercent;    /**< End cut time, in percent of clip
                                                             duration (only for 3GPP clip !) */
    M4OSA_UInt32                    uiDuration;            /**< Duration of the clip, if different
                                                                from 0, has priority on
                                                                uiEndCutTime or uiEndCutPercent */
    M4OSA_Bool                        isPanZoom;            /**< RC: Boolean used to know if the
                                                                 pan and zoom mode is enabled */
    M4OSA_UInt16                    PanZoomXa;            /**< RC */
    M4OSA_UInt16                    PanZoomTopleftXa;    /**< RC */
    M4OSA_UInt16                    PanZoomTopleftYa;    /**< RC */
    M4OSA_UInt16                    PanZoomXb;            /**< RC */
    M4OSA_UInt16                    PanZoomTopleftXb;    /**< RC */
    M4OSA_UInt16                    PanZoomTopleftYb;    /**< RC */
    M4xVSS_MediaRendering            MediaRendering;        /**< FB only used with JPEG: to crop,
                                                                 resize, or render black borders*/

} M4xVSS_ClipSettings;

/**
 ******************************************************************************
 * struct   M4xVSS_EditSettings
 * @brief   This structure gathers all the information needed to define a complete
 *          edition operation
 ******************************************************************************
*/
typedef struct
{
    /**< Output video size */
    M4VIDEOEDITING_VideoFrameSize             outputVideoSize;
    /**< Output video format (MPEG4 / H263) */
    M4VIDEOEDITING_VideoFormat                outputVideoFormat;
    /**< Output audio format (AAC, AMRNB ...) */
    M4VIDEOEDITING_AudioFormat                outputAudioFormat;
    /**< Output audio sampling freq (8000Hz,...) */
    M4VIDEOEDITING_AudioSamplingFrequency     outputAudioSamplFreq;
    /**< Maximum output file size in BYTES (if set to 0, no limit */
    M4OSA_UInt32                              outputFileSize;
    /**< Is output audio must be Mono ? Valid only for AAC */
    M4OSA_Bool                                bAudioMono;
    /**< Output video bitrate*/
    M4OSA_UInt32                              outputVideoBitrate;
    /**< Output audio bitrate*/
    M4OSA_UInt32                              outputAudioBitrate;
    /**< Background music track settings */
    M4xVSS_BGMSettings                        *pBGMtrack;
    /**< Function pointer on text rendering engine, if not used, must be set to NULL !! */
    M4xVSS_getTextRgbBufferFct                pTextRenderingFct;
    /** output video profile and level*/
    M4OSA_Int32   outputVideoProfile;
    M4OSA_Int32   outputVideoLevel;

} M4xVSS_EditSettings;

#endif /* __M4VSS3GPP_EXTENDED_API_H__ */

