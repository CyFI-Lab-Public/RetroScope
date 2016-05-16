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
 * @file    M4MCS_API.h
 * @brief   Media Conversion Service public API.
 * @note    MCS allows transcoding a 3gp/mp4 file into a new 3gp/mp4 file changing the
 *          video and audio encoding settings.
 *          It is a straightforward and fully synchronous API.
 ******************************************************************************
 */

#ifndef __M4MCS_API_H__
#define __M4MCS_API_H__

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
 * Common definitions of video editing components */
#include "M4_VideoEditingCommon.h"

/**
 * To enable external audio codecs registering*/
#include "M4AD_Common.h"
#include "M4ENCODER_AudioCommon.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 *    Public type of the MCS context */
typedef M4OSA_Void* M4MCS_Context;


/**
 ******************************************************************************
 * enum        M4MCS_MediaRendering
 * @brief    This enum defines different media rendering
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kResizing = 0,    /**< The media is resized, the aspect ratio can be
                              different from the original one.
                              All of the media is rendered */
    M4MCS_kCropping,        /**< The media is cropped, the aspect ratio is the
                              same as the original one.
                              The media is not rendered entirely */
    M4MCS_kBlackBorders     /**< Black borders are rendered in order to keep the
                              original aspect ratio. All the media is rendered */
} M4MCS_MediaRendering;


/**
 ******************************************************************************
 * struct   M4MCS_ExternalProgress
 * @brief   This structure contains information provided to the external Effect functions
 * @note    The uiProgress value should be enough for most cases
 ******************************************************************************
 */
typedef struct
{
    M4OSA_UInt32    uiProgress;     /**< Progress of the Effect from 0 to 1000 (one thousand) */
    M4OSA_UInt32    uiClipTime;     /**< Current time, in milliseconds,
                                          in the current clip time-line */
    M4OSA_UInt32    uiOutputTime;   /**< Current time, in milliseconds,
                                          in the output clip time-line */

} M4MCS_ExternalProgress;


/**
 ******************************************************************************
 * enum     M4MCS_AudioEffectType
 * @brief   This enumeration defines the audio effect types of the MCS
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kAudioEffectType_None    = 0,
    M4MCS_kAudioEffectType_FadeIn  = 8, /**< Intended for begin effect */
    M4MCS_kAudioEffectType_FadeOut = 16, /**< Intended for end effect */
    M4MCS_kAudioEffectType_External = 256

} M4MCS_AudioEffectType;


/**
 ******************************************************************************
 * prototype    M4MCS_editAudioEffectFct
 * @brief       Audio effect functions implemented by the integrator
 *              must match this prototype.
 * @note        The function is provided with the original PCM data buffer and its size.
 *              Audio effect have to be applied on it.
 *              The progress of the effect is given, on a scale from 0 to 1000.
 *              When the effect function is called, all the buffers are valid and
 *              owned by the MCS.
 *
 * @param   pFunctionContext    (IN) The function context, previously set by the integrator
 * @param   pPCMdata            (IN/OUT) valid PCM data buffer
 * @param   uiPCMsize           (IN/OUT) PCM data buffer corresponding size
 * @param   pProgress           (IN) Set of information about the audio effect progress.
 *
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 ******************************************************************************
 */
typedef M4OSA_ERR (*M4MCS_editAudioEffectFct)
(
    M4OSA_Void *pFunctionContext,
    M4OSA_Int16 *pPCMdata,
    M4OSA_UInt32 uiPCMsize,
    M4MCS_ExternalProgress *pProgress
);


/**
 ******************************************************************************
 * struct   M4MCS_EffectSettings
 * @brief   This structure defines an audio effect for the edition.
 ******************************************************************************
 */
typedef struct
{
    M4OSA_UInt32                 uiStartTime;              /**< In ms */
    M4OSA_UInt32                 uiDuration;               /**< In ms */
    M4MCS_editAudioEffectFct     ExtAudioEffectFct;        /**< External effect function */
    M4OSA_Void                   *pExtAudioEffectFctCtxt;  /**< Context given to the external
                                                                effect function */
    M4MCS_AudioEffectType        AudioEffectType;         /**< None, FadeIn, FadeOut */

} M4MCS_EffectSettings;


/**
 ******************************************************************************
 * struct    M4MCS_OutputParams
 * @brief    MCS Output parameters
 * @note     Following parameters are used for still picture inputs :
 *             - OutputFileType (must be set to M4VIDEOEDITING_kFileType_JPG)
 *             - bDiscardExif must be set to M4OSA_TRUE or M4OSA_FALSE
 *             - bAdjustOrientation must be set to M4OSA_TRUE or M4OSA_FALSE
 *             - (MediaRendering is not handled : output image resolution is always
                 set according to BestFit criteria)
 *            bDiscardExif and bAdjustOrientation are still picture only parameters
 ******************************************************************************
 */
typedef struct
{
    /**< Format of the output file */
    M4VIDEOEDITING_FileType                 OutputFileType;
    /**< Output video compression format, see enum */
    M4VIDEOEDITING_VideoFormat              OutputVideoFormat;
    /**< Output frame size : QQVGA, QCIF or SQCIF */
    M4VIDEOEDITING_VideoFrameSize           OutputVideoFrameSize;
    /**< Targeted Output framerate, see enum */
    M4VIDEOEDITING_VideoFramerate           OutputVideoFrameRate;
    /**< Format of the audio in the stream */
    M4VIDEOEDITING_AudioFormat              OutputAudioFormat;
    /**< Sampling frequency of the audio in the stream */
    M4VIDEOEDITING_AudioSamplingFrequency   OutputAudioSamplingFrequency;
    /**< Set to M4OSA_TRUE if the output audio is mono */
    M4OSA_Bool                              bAudioMono;
    /**< Output PCM file if not NULL */
    M4OSA_Char                              *pOutputPCMfile;
    /**< To crop, resize, or render black borders*/
    M4MCS_MediaRendering                    MediaRendering;
    /**< List of effects */
    M4MCS_EffectSettings                    *pEffects;
    /**< Number of effects in the above list */
    M4OSA_UInt8                             nbEffects;

    /*--- STILL PICTURE ---*/
    /**< TRUE: Even if the input file contains an EXIF section,
    the output file won't contain any EXIF section.*/
    M4OSA_Bool                              bDiscardExif ;

    /**< =TRUE : picture must be rotated if Exif tags hold a rotation info
    (and rotation info is set to 0)*/
    M4OSA_Bool                              bAdjustOrientation ;
    /*--- STILL PICTURE ---*/
    M4OSA_Int32 outputVideoProfile;
    M4OSA_Int32 outputVideoLevel;
} M4MCS_OutputParams;

/*--- STILL PICTURE ---*/
/**
 ******************************************************************************
 * enum      M4MCS_SPOutputResolution
 * @brief    Still picture specific : MCS output targeted file resolution
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kResSameAsInput       = 0x00, /*width x height*/
    M4MCS_kResQVGA              = 0x01, /*320x240*/
    M4MCS_kResVGA               = 0x02, /*640x480*/
    M4MCS_kResWQVGA             = 0x03, /*400x240*/
    M4MCS_kResWVGA              = 0x04, /*800x480*/
    M4MCS_kResXGA               = 0x05, /*1024x768*/
    M4MCS_kResCustom            = 0xFF  /*Size is set via StillPictureCustomWidth/Height*/
} M4MCS_SPOutputResolution ;


/**
 ******************************************************************************
 * enum      M4MCS_SPStrategy
 * @brief    Still picture specific : MCS strategy to configure the encoding parameters
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kFileSizeOnlyFixed            = 0x00, /*StillPictureResolution and
                                                 QualityFactor are ignored*/
    M4MCS_kFileSizeAndResFixed          = 0x01, /*QualityFactor is ignored*/
    M4MCS_kQualityAndResFixed           = 0x02  /*OutputFileSize is ignored*/
} M4MCS_SPStrategy ;


/**
 ******************************************************************************
 * enum      M4MCS_SPCrop
 * @brief    Still picture specific : indicate whether cropping should be done
                                     before changing the resolution
 ******************************************************************************
 */
typedef enum
{
    M4MCS_kNoCrop                = 0x00, /*No Cropping is performed*/
    M4MCS_kCropBeforeResize      = 0x01  /*Input image is cropped (before changing resolution)*/
} M4MCS_SPCrop ;


/**
 ******************************************************************************
 * struct    M4MCS_EncodingParams
 * @brief    MCS file size, bitrate and cut parameters
 * @note     Following parameters are used for still picture inputs :
 *             - OutputFileSize
 *             - StillPictureResolution
 *             - QualityFactor
 *             - StillPictureStrategy
 *             - StillPictureCustomWidth/Height (if StillPictureResolution==M4MCS_kResCustom)
 *            Still picture only parameters : StillPictureResolution, QualityFactor,
 *            StillPictureStrategy and StillPictureCustomWidth/Height
 ******************************************************************************
 */
typedef struct
{
    M4VIDEOEDITING_Bitrate    OutputVideoBitrate;     /**< Targeted video bitrate */
    M4VIDEOEDITING_Bitrate    OutputAudioBitrate;     /**< Targeted audio bitrate */
    M4OSA_UInt32              BeginCutTime;           /**< Beginning cut time in input file */
    M4OSA_UInt32              EndCutTime;             /**< End cut time in input file */
    M4OSA_UInt32              OutputFileSize;         /**< Expected resulting file size */
    M4OSA_UInt32              OutputVideoTimescale;   /**< Optional parameter used to fix a
                                                           timescale during transcoding */

    /*--- STILL PICTURE ---*/
    M4OSA_Int32               QualityFactor ;         /**< =-1 (undefined) or 0(lowest)..
                                                            50(best) : This parameter is the
                                                            quality indication for the JPEG output
                                                            file (if =-1 the MCS will set quality
                                                            automatically)*/
    M4MCS_SPStrategy            StillPictureStrategy ; /**< Defines which input parameters
                                                            will be taken into account by MCS*/
    M4MCS_SPOutputResolution    StillPictureResolution;/**< Desired output resolution for
                                                            a still picture file */
    /**< (only if Resolution==M4MCS_kResCustom) : Custom output image width */
    M4OSA_UInt32                StillPictureCustomWidth;
    /**< (only if Resolution==M4MCS_kResCustom) : Custom output image height */
    M4OSA_UInt32                StillPictureCustomHeight;
    /**< Indicate whether Crop should be performed */
    M4MCS_SPCrop                StillPictureCrop;
    /**< (only if cropping) X coordinate of topleft corner of the crop window */
    M4OSA_UInt32                StillPictureCrop_X;
    /**< (only if cropping) Y coordinate of topleft corner of the crop window */
    M4OSA_UInt32                StillPictureCrop_Y;
    /**< (only if cropping) Width of the crop window (in pixels) */
    M4OSA_UInt32                StillPictureCrop_W;
    /**< (only if cropping) Height of the crop window (in pixels) */
    M4OSA_UInt32                StillPictureCrop_H;
    /*--- STILL PICTURE ---*/
} M4MCS_EncodingParams;

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getVersion(M4_VersionInfo* pVersionInfo);
 * @brief    Get the MCS version.
 * @note Can be called anytime. Do not need any context.
 * @param    pVersionInfo        (OUT) Pointer to a version info structure
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pVersionInfo is M4OSA_NULL (If Debug Level >= 2)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getVersion(M4_VersionInfo* pVersionInfo);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_init(M4MCS_Context* pContext, M4OSA_FileReadPointer* pFileReadPtrFct,
                        M4OSA_FileWriterPointer* pFileWritePtrFct);
 * @brief    Initializes the MCS (allocates an execution context).
 * @note
 * @param    pContext            (OUT) Pointer on the MCS context to allocate
 * @param    pFileReadPtrFct     (IN) Pointer to OSAL file reader functions
 * @param    pFileWritePtrFct    (IN) Pointer to OSAL file writer functions
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_ALLOC:        There is no more available memory
 ******************************************************************************
 */
M4OSA_ERR M4MCS_init(M4MCS_Context* pContext, M4OSA_FileReadPointer* pFileReadPtrFct,
                      M4OSA_FileWriterPointer* pFileWritePtrFct);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_open(M4MCS_Context pContext, M4OSA_Void* pFileIn, M4OSA_Void* pFileOut,
                          M4OSA_UInt32 uiMaxMetadataSize);
 * @brief   Set the MCS input and output files.
 * @note    It opens the input file, but the output file is not created yet.
 *          In case of still picture, four InputFileType are possible
 *          (M4VIDEOEDITING_kFileType_JPG/BMP/GIF/PNG
 *          If one of them is set, the OutputFileType SHALL be set to M4VIDEOEDITING_kFileType_JPG
 * @param   pContext            (IN) MCS context
 * @param   pFileIn             (IN) Input file to transcode (The type of this parameter
 *                                    (URL, pipe...) depends on the OSAL implementation).
 * @param   mediaType           (IN) Container type (.3gp,.amr, ...) of input file.
 * @param   pFileOut            (IN) Output file to create  (The type of this parameter
 *                                    (URL, pipe...) depends on the OSAL implementation).
 * @param   pTempFile           (IN) Temporary file for the constant memory writer to store
 *                                    metadata ("moov.bin").
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4ERR_ALLOC:        There is no more available memory
 * @return  M4ERR_FILE_NOT_FOUND:   The input file has not been found
 * @return  M4MCS_ERR_INVALID_INPUT_FILE:   The input file is not a valid file, or is corrupted
 * @return  M4MCS_ERR_INPUT_FILE_CONTAINS_NO_SUPPORTED_STREAM:  The input file contains no
 *                                                               supported audio or video stream
 ******************************************************************************
 */
M4OSA_ERR M4MCS_open(M4MCS_Context pContext, M4OSA_Void* pFileIn,
                     M4VIDEOEDITING_FileType InputFileType,
                     M4OSA_Void* pFileOut, M4OSA_Void* pTempFile);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_step(M4MCS_Context pContext, M4OSA_UInt8 *pProgress);
 * @brief   Perform one step of trancoding.
 * @note
 * @param   pContext            (IN) MCS context
 * @param   pProgress           (OUT) Progress percentage (0 to 100) of the transcoding
 * @note    pProgress must be a valid address.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    One of the parameters is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_WAR_TRANSCODING_DONE: Transcoding is over, user should now call M4MCS_close()
 * @return  M4MCS_ERR_AUDIO_CONVERSION_FAILED: The audio conversion (AAC to AMR-NB, MP3) failed
 * @return  M4MCS_ERR_INVALID_AAC_SAMPLING_FREQUENCY: The input file contains an AAC audio track
 *                                                     with an invalid sampling frequency
 *                                                     (should never happen)
 * @return  M4MCS_WAR_PICTURE_AUTO_RESIZE: Picture will be automatically resized to fit
 *                                          into requirements
 ******************************************************************************
 */
M4OSA_ERR M4MCS_step(M4MCS_Context pContext, M4OSA_UInt8 *pProgress);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_pause(M4MCS_Context pContext);
 * @brief   Pause the transcoding i.e. release the (external hardware) video decoder.
 * @note    This function is not needed if no hardware accelerators are used.
 *          In that case, pausing the MCS is simply achieved by temporarily suspending
 *          the M4MCS_step function calls.
 * @param   pContext            (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_pause(M4MCS_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_resume(M4MCS_Context pContext);
 * @brief   Resume the transcoding after a pause (see M4MCS_pause).
 * @note    This function is not needed if no hardware accelerators are used.
 *          In that case, resuming the MCS is simply achieved by calling
 *          the M4MCS_step function.
 * @param   pContext            (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_resume(M4MCS_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_close(M4MCS_Context pContext);
 * @brief    Finish the MCS transcoding.
 * @note The output 3GPP file is ready to be played after this call
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_close(M4MCS_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_cleanUp(M4MCS_Context pContext);
 * @brief    Free all resources used by the MCS.
 * @note The context is no more valid after this call
 * @param    pContext            (IN) MCS context
 * @return   M4NO_ERROR:         No error
 * @return   M4ERR_PARAMETER:    pContext is M4OSA_NULL (If Debug Level >= 2)
 * @return   M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_cleanUp(M4MCS_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_abort(M4MCS_Context pContext);
 * @brief    Finish the MCS transcoding and free all resources used by the MCS
 *          whatever the state is.
 * @note    The context is no more valid after this call
 * @param    pContext            (IN) MCS context
 * @return    M4NO_ERROR:            No error
 * @return    M4ERR_PARAMETER:    pContext is M4OSA_NULL (debug only)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_abort(M4MCS_Context pContext);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getInputFileProperties(M4MCS_Context pContext,
 *                                          M4VIDEOEDITING_ClipProperties* pFileProperties);
 * @brief   Retrieves the properties of the audio and video streams from the input file.
 * @param   pContext            (IN) MCS context
 * @param   pProperties         (OUT) Pointer on an allocated M4VIDEOEDITING_ClipProperties
                                structure which is filled with the input stream properties.
 * @note    The structure pProperties must be allocated and further de-allocated
            by the application. The function must be called in the opened state.
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getInputFileProperties(M4MCS_Context pContext,
                                        M4VIDEOEDITING_ClipProperties *pFileProperties);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_setOutputParams(M4MCS_Context pContext, M4MCS_OutputParams* pParams);
 * @brief   Set the MCS video output parameters.
 * @note    Must be called after M4MCS_open. Must be called before M4MCS_step.
 * @param   pContext            (IN) MCS context
 * @param   pParams             (IN/OUT) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_INVALID_VIDEO_FRAME_SIZE_FOR_H263 : Output video frame size parameter is
 *                                                          incompatible with H263 encoding
 * @return  M4MCS_ERR_INVALID_VIDEO_FRAME_RATE_FOR_H263 : Output video frame size parameter is
 *                                                          incompatible with H263 encoding
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FORMAT     : Undefined output video format parameter
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_SIZE : Undefined output video frame size
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_VIDEO_FRAME_RATE : Undefined output video frame rate
 * @return  M4MCS_ERR_UNDEFINED_OUTPUT_AUDIO_FORMAT : Undefined output audio format parameter
 * @return  M4MCS_ERR_DURATION_IS_NULL : Specified output parameters define a null duration stream
 *                                        (no audio and video)
 ******************************************************************************
 */
M4OSA_ERR M4MCS_setOutputParams(M4MCS_Context pContext, M4MCS_OutputParams* pParams);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_setEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates)
 * @brief   Set the values of the encoding parameters
 * @note    Must be called before M4MCS_checkParamsAndStart().
 * @param   pContext           (IN) MCS context
 * @param   pRates             (IN) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_HIGH: Audio bitrate too high (we limit to 96 kbps)
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_LOW: Audio bitrate is too low (16 kbps min for aac,
 *                                           12.2 for amr, 8 for mp3)
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT: Begin cut and End cut are equals
 * @return  M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION: Begin cut time is larger than
 *                                                     the input clip duration
 * @return  M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT: End cut time is smaller than begin cut time
 * @return  M4MCS_ERR_MAXFILESIZE_TOO_SMALL: Not enough space to store whole output
 *                                            file at given bitrates
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_HIGH: Video bitrate too high (we limit to 800 kbps)
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_LOW: Video bitrate too low
 ******************************************************************************
 */
M4OSA_ERR M4MCS_setEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_getExtendedEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates)
 * @brief   Get the extended values of the encoding parameters
 * @note    Could be called after M4MCS_setEncodingParams.
 * @param   pContext           (IN) MCS context
 * @param   pRates             (OUT) Transcoding parameters
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT: Encoding settings would produce a
 *                                              null duration clip = encoding is impossible
 ******************************************************************************
 */
M4OSA_ERR M4MCS_getExtendedEncodingParams(M4MCS_Context pContext, M4MCS_EncodingParams* pRates);

/**
 ******************************************************************************
 * M4OSA_ERR M4MCS_checkParamsAndStart(M4MCS_Context pContext)
 * @brief
 * @note
 * @param   pContext           (IN) MCS context
 * @return  M4NO_ERROR:         No error
 * @return  M4ERR_PARAMETER:    At least one parameter is M4OSA_NULL (debug only)
 * @return  M4ERR_STATE:        MCS is not in an appropriate state for this function to be called
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_HIGH: Audio bitrate too high (we limit to 96 kbps)
 * @return  M4MCS_ERR_AUDIOBITRATE_TOO_LOW: Audio bitrate is too low (16 kbps min for aac,
 *                                           12.2 for amr, 8 for mp3)
 * @return  M4MCS_ERR_BEGIN_CUT_EQUALS_END_CUT: Begin cut and End cut are equals
 * @return  M4MCS_ERR_BEGIN_CUT_LARGER_THAN_DURATION: Begin cut time is larger than
 *                                                    the input clip duration
 * @return  M4MCS_ERR_END_CUT_SMALLER_THAN_BEGIN_CUT: End cut time is smaller than begin cut time
 * @return  M4MCS_ERR_MAXFILESIZE_TOO_SMALL: Not enough space to store whole output
 *                                            file at given bitrates
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_HIGH: Video bitrate too high (we limit to 800 kbps)
 * @return  M4MCS_ERR_VIDEOBITRATE_TOO_LOW:  Video bitrate too low
 ******************************************************************************
 */
M4OSA_ERR M4MCS_checkParamsAndStart(M4MCS_Context pContext);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* __M4MCS_API_H__ */

