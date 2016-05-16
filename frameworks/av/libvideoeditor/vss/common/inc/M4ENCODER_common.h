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
 * @file    M4ENCODER_common.h
 * @note    This file defines the types internally used by the VES to abstract encoders

 ******************************************************************************
*/
#ifndef __M4ENCODER_COMMON_H__
#define __M4ENCODER_COMMON_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */


/**
 * Video preprocessing common interface */
#include "M4VPP_API.h"

/**
 * Writer common interface */
#include "M4WRITER_common.h"

/* IMAGE STAB */
/* percentage of image suppressed (computed from the standard dimension).*/
#define M4ENCODER_STAB_FILTER_CROP_PERCENTAGE 10
        /* WARNING: take the inferior even dimension, ex: 10% for QCIF output => 192x158 */

/**
 ******************************************************************************
 * enum        M4ENCODER_OpenMode
 * @brief    Definition of open mode for the encoder.
 * @note    DEFAULT  : pointer to M4ENCODER_open() which use default parameters
 *          ADVANCED : pointer to M4ENCODER_open_advanced() which allow to customize
 *                     various encoding parameters
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_OPEN_DEFAULT,
    M4ENCODER_OPEN_ADVANCED
} M4ENCODER_OpenMode;

 /**
 ******************************************************************************
 * enum        M4ENCODER_FrameRate
 * @brief    Thie enum defines the encoded video framerates.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_k5_FPS,
    M4ENCODER_k7_5_FPS,
    M4ENCODER_k10_FPS,
    M4ENCODER_k12_5_FPS,
    M4ENCODER_k15_FPS,
    M4ENCODER_k20_FPS,
    M4ENCODER_k25_FPS,
    M4ENCODER_k30_FPS,
    M4ENCODER_kVARIABLE_FPS,            /**< Variable video bitrate */
    M4ENCODER_kUSE_TIMESCALE            /**< Advanced encoding, use timescale indication rather
                                                than framerate */
} M4ENCODER_FrameRate;

/**
 ******************************************************************************
 * enum        M4ENCODER_InputFormat
 * @brief    Thie enum defines the video format of the grabbing.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kIYUV420=0, /**< YUV 4:2:0 planar (standard input for mpeg-4 video) */
    M4ENCODER_kIYUV422,   /**< YUV422 planar */
    M4ENCODER_kIYUYV,     /**< YUV422 interlaced, luma first */
    M4ENCODER_kIUYVY,     /**< YUV422 interlaced, chroma first */
    M4ENCODER_kIJPEG,     /**< JPEG compressed frames */
    M4ENCODER_kIRGB444,   /**< RGB 12 bits 4:4:4 */
    M4ENCODER_kIRGB555,   /**< RGB 15 bits 5:5:5 */
    M4ENCODER_kIRGB565,   /**< RGB 16 bits 5:6:5 */
    M4ENCODER_kIRGB24,    /**< RGB 24 bits 8:8:8 */
    M4ENCODER_kIRGB32,    /**< RGB 32 bits  */
    M4ENCODER_kIBGR444,   /**< BGR 12 bits 4:4:4 */
    M4ENCODER_kIBGR555,   /**< BGR 15 bits 5:5:5 */
    M4ENCODER_kIBGR565,   /**< BGR 16 bits 5:6:5 */
    M4ENCODER_kIBGR24,    /**< BGR 24 bits 8:8:8 */
    M4ENCODER_kIBGR32     /**< BGR 32 bits  */
} M4ENCODER_InputFormat;

/**
 ******************************************************************************
 * enum        M4ENCODER_Format
 * @brief    Thie enum defines the video compression formats.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kMPEG4 = 0,
    M4ENCODER_kH263,
    M4ENCODER_kH264,
    M4ENCODER_kJPEG,
    M4ENCODER_kMJPEG,
    M4ENCODER_kNULL,
    M4ENCODER_kYUV420,            /**< No compression */
    M4ENCODER_kYUV422,            /**< No compression */

    M4ENCODER_kVideo_NB /* number of decoders, keep it as last enum entry */
} M4ENCODER_Format;

/**
 ******************************************************************************
 * enum        M4ENCODER_FrameWidth
 * @brief    Thie enum defines the avalaible frame Width.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_SQCIF_Width = 128, /**< SQCIF 128x96 */
    M4ENCODER_QQVGA_Width = 160, /**< QQVGA 160x120 */
    M4ENCODER_QCIF_Width  = 176, /**< QCIF 176x144 */
    M4ENCODER_QVGA_Width  = 320, /**< QVGA 320x240 */
    M4ENCODER_CIF_Width   = 352, /**< CIF 352x288 */
    M4ENCODER_VGA_Width   = 640, /**< VGA 640x480 */
    M4ENCODER_SVGA_Width  = 800, /**< SVGA 800x600 */
    M4ENCODER_XGA_Width   = 1024, /**< XGA 1024x768 */
    M4ENCODER_XVGA_Width  = 1280, /**< XVGA 1280x1024 */
/* +PR LV5807 */
    M4ENCODER_WVGA_Width  = 800, /**< WVGA 800 x 480 */
    M4ENCODER_NTSC_Width  = 720, /**< NTSC 720 x 480 */
/* -PR LV5807 */

/* +CR Google */
    M4ENCODER_640_360_Width   = 640,  /**< 640x360 */
    // StageFright encoders require %16 resolution
    M4ENCODER_854_480_Width   = 848,  /**< 848x480 */
    M4ENCODER_1280_720_Width  = 1280, /**< 720p 1280x720 */
    // StageFright encoders require %16 resolution
    M4ENCODER_1080_720_Width  = 1088, /**< 720p 1088x720 */
    M4ENCODER_960_720_Width   = 960,  /**< 720p 960x720 */
    M4ENCODER_1920_1080_Width = 1920  /**< 1080p 1920x1080 */
/* -CR Google */

} M4ENCODER_FrameWidth;

/**
 ******************************************************************************
 * enum        M4ENCODER_FrameHeight
 * @brief    Thie enum defines the avalaible frame Height.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_SQCIF_Height = 96,  /**< SQCIF 128x96 */
    M4ENCODER_QQVGA_Height = 120, /**< QQVGA 160x120 */
    M4ENCODER_QCIF_Height  = 144, /**< QCIF 176x144 */
    M4ENCODER_QVGA_Height  = 240, /**< QVGA 320x240 */
    M4ENCODER_CIF_Height   = 288, /**< CIF 352x288 */
    M4ENCODER_VGA_Height   = 480, /**< VGA 340x480 */
    M4ENCODER_SVGA_Height  = 600, /**< SVGA 800x600 */
    M4ENCODER_XGA_Height   = 768, /**< XGA 1024x768 */
    M4ENCODER_XVGA_Height  = 1024, /**< XVGA 1280x1024 */
/* +PR LV5807 */
    M4ENCODER_WVGA_Height  = 480, /**< WVGA 800 x 480 */
    M4ENCODER_NTSC_Height  = 480, /**< NTSC 720 x 480 */
/* -PR LV5807 */

/* +CR Google */
    M4ENCODER_640_360_Height  = 360, /**< 640x360 */
    M4ENCODER_854_480_Height  = 480, /**< 854x480 */
    M4ENCODER_1280_720_Height = 720, /**< 720p 1280x720 */
    M4ENCODER_1080_720_Height = 720, /**< 720p 1080x720 */
    M4ENCODER_960_720_Height  = 720, /**< 720p 960x720 */
    // StageFright encoders require %16 resolution
    M4ENCODER_1920_1080_Height = 1088 /**< 1080p 1920x1080 */
/* -CR Google */
} M4ENCODER_FrameHeight;

/**
 ******************************************************************************
 * enum        M4ENCODER_Bitrate
 * @brief    Thie enum defines the avalaible bitrates.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_k28_KBPS  = 28000,
    M4ENCODER_k40_KBPS  = 40000,
    M4ENCODER_k64_KBPS  = 64000,
    M4ENCODER_k96_KBPS  = 96000,
    M4ENCODER_k128_KBPS = 128000,
    M4ENCODER_k192_KBPS = 192000,
    M4ENCODER_k256_KBPS = 256000,
    M4ENCODER_k384_KBPS = 384000,
    M4ENCODER_k512_KBPS = 512000,
    M4ENCODER_k800_KBPS = 800000

} M4ENCODER_Bitrate;

/* IMAGE STAB */

/**
 ******************************************************************************
 * enum            M4ENCODER_StabMode
 * @brief        The current mode of the stabilization filter.
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kStabOff = 0,        /**< stabilization filter is disabled */
    M4ENCODER_kStabCentered,    /**< stabilization filter is enabled. */
                                /**< Video input and output must have the same dimensions. Output
                                    image will have black borders */
    M4ENCODER_kStabGrabMore        /**< stabilization filter is enabled. */
                                /**< Video input dimensions must be bigger than output. The ratio
                                        is indicated by M4ENCODER_STAB_FILTER_CROP_PERCENTAGE */

} M4ENCODER_StabMode;

/**
 ******************************************************************************
 * enum            M4ENCODER_FrameMode
 * @brief        Values to drive the encoder behaviour (type of frames produced)
 ******************************************************************************
*/
typedef enum
{
    M4ENCODER_kNormalFrame = 0,   /**< let the encoder decide which type of frame to encode */
    M4ENCODER_kLastFrame   = 1,   /**< force encoder the flush all its buffers because it is
                                         last frame  */
    M4ENCODER_kIFrame      = 2    /**< force encoder to generate an I frame */

} M4ENCODER_FrameMode;

/**
 ******************************************************************************
 * struct    M4ENCODER_Params
 * @brief    This structure defines all the settings avalaible when encoding.
 ******************************************************************************
*/
typedef struct
{
    /* Input */
    M4ENCODER_InputFormat    InputFormat;        /**< Input video format (grabbing) */
    M4ENCODER_FrameWidth    InputFrameWidth;    /**< Input Frame width (grabbing) */
    M4ENCODER_FrameHeight    InputFrameHeight;    /**< Input Frame height (grabbing) */

    /* Output */
    M4ENCODER_FrameWidth    FrameWidth;            /**< Frame width  */
    M4ENCODER_FrameHeight    FrameHeight;        /**< Frame height  */
    M4ENCODER_Bitrate        Bitrate;            /**< Bitrate, see enum  */
    M4ENCODER_FrameRate        FrameRate;            /**< Framerate, see enum  */
    M4ENCODER_Format        Format;                /**< Video compression format, H263, MPEG4,
                                                         MJPEG ...  */
    M4OSA_Int32            videoProfile; /** video profile */
    M4OSA_Int32            videoLevel;   /** video level */
} M4ENCODER_Params;

/**
 ******************************************************************************
 * struct    M4ENCODER_AdvancedParams
 * @brief    This structure defines the advanced settings available for MPEG-4 encoding.
 ******************************************************************************
*/
typedef struct
{
    /**
     * Input parameters (grabber coupled with encoder): */
    M4ENCODER_InputFormat    InputFormat;                /**< Input video format */
    M4ENCODER_FrameWidth    InputFrameWidth;            /**< Input Frame width */
    M4ENCODER_FrameHeight    InputFrameHeight;            /**< Input Frame height */

    /**
     * Common settings for H263 and MPEG-4: */
    M4ENCODER_FrameWidth    FrameWidth;                    /**< Frame width  */
    M4ENCODER_FrameHeight    FrameHeight;                /**< Frame height  */
    M4OSA_UInt32            Bitrate;                    /**< Free value for the bitrate */
    /**< Framerate (if set to M4ENCODER_kUSE_TIMESCALE use uiRateFactor & uiTimeScale instead) */
    M4ENCODER_FrameRate        FrameRate;
    /**< Video compression format: H263 or MPEG4 */
    M4ENCODER_Format        Format;
    M4OSA_Int32            videoProfile; /** output video profile */
    M4OSA_Int32            videoLevel;   /** output video level */
    M4OSA_UInt32            uiHorizontalSearchRange; /**< Set to 0 will use default value (15) */
    M4OSA_UInt32            uiVerticalSearchRange;   /**< Set to 0 will use default value (15) */
    /**< Set to 0 will use default value (0x7FFF i.e. let engine decide when to put an I) */
    M4OSA_UInt32            uiStartingQuantizerValue;
    /**< Enable if priority is quality, Disable if priority is framerate */
    M4OSA_Bool                bInternalRegulation;
    /**< Ratio between the encoder frame rate and the actual frame rate */
    M4OSA_UInt8                uiRateFactor;
    /**< I frames periodicity, set to 0 will use default value */
    M4OSA_UInt32            uiIVopPeriod;
    /**< Motion estimation [default=0 (all tools), disable=8 (no tool)] */
    M4OSA_UInt8             uiMotionEstimationTools;

    /**
     * Settings for MPEG-4 only: */
    M4OSA_UInt32            uiTimeScale;                /**< Free value for the timescale */
    M4OSA_Bool                bErrorResilience;           /**< Disabled by default */
    /**< Disabled by default (if enabled, bErrorResilience should be enabled too!) */
    M4OSA_Bool                bDataPartitioning;
    M4OSA_Bool              bAcPrediction;           /**< AC prediction [default=1, disable=0] */

} M4ENCODER_AdvancedParams;

/**
 ******************************************************************************
 * struct    M4ENCODER_StillPictureParams
 * @brief    This structure defines all the settings avalaible when encoding still
 *            picture.
 ******************************************************************************
*/
typedef struct
{
    M4ENCODER_FrameWidth    FrameWidth;            /**< Frame width  */
    M4ENCODER_FrameHeight    FrameHeight;        /**< Frame height  */
    M4OSA_UInt32            Quality;            /**< Bitrate, see enum  */
    M4ENCODER_Format        InputFormat;        /**< YUV 420 or 422  */
    M4ENCODER_Format        Format;                /**< Video compression format, H263, MPEG4,
                                                         MJPEG ...  */
    M4OSA_Bool                PreProcessNeeded;    /**< Is the call to the VPP is necessary */
    M4OSA_Bool                EncodingPerStripes;    /**< Is encoding per stripes */

} M4ENCODER_StillPictureParams;

/**
 ******************************************************************************
 * struct    M4ENCODER_Header
 * @brief    This structure defines the buffer where the sequence header is put.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8    pBuf;        /**< Buffer for the header */
    M4OSA_UInt32    Size;        /**< Size of the data */

} M4ENCODER_Header;

/**
 ******************************************************************************
 * enum    M4ENCODER_OptionID
 * @brief This enums defines all avalaible options.
 ******************************************************************************
*/
typedef enum
{
    /**< set the fragment size, option value is M4OSA_UInt32 type */
    M4ENCODER_kOptionID_VideoFragmentSize    = M4OSA_OPTION_ID_CREATE (M4_WRITE,\
                                                     M4ENCODER_COMMON, 0x01),

    /**< set the stabilization filtering, option value is M4ENCODER_StabMode type */
    M4ENCODER_kOptionID_ImageStabilization    = M4OSA_OPTION_ID_CREATE (M4_WRITE,\
                                                          M4ENCODER_COMMON, 0x02),

    /**< prevent writting of any AU, option value is M4OSA_Bool type */
    M4ENCODER_kOptionID_InstantStop            = M4OSA_OPTION_ID_CREATE (M4_WRITE,\
                                                         M4ENCODER_COMMON, 0x03),

    /**< get the DSI (encoder header) generated by the encoder */
    M4ENCODER_kOptionID_EncoderHeader        = M4OSA_OPTION_ID_CREATE (M4_READ ,\
                                                             M4ENCODER_COMMON, 0x04),
/*+ CR LV6775 -H.264 Trimming  */

    M4ENCODER_kOptionID_SetH264ProcessNALUfctsPtr= M4OSA_OPTION_ID_CREATE (M4_READ ,\
                                                             M4ENCODER_COMMON, 0x05),
    M4ENCODER_kOptionID_H264ProcessNALUContext        = M4OSA_OPTION_ID_CREATE (M4_READ ,\
                                                             M4ENCODER_COMMON, 0x06)
/*-CR LV6775 -H.264 Trimming  */
} M4ENCODER_OptionID;

/*+ CR LV6775 -H.264 Trimming  */
typedef M4OSA_ERR (H264MCS_ProcessEncodedNALU_fct)(M4OSA_Void*ainstance,M4OSA_UInt8* inbuff,
                               M4OSA_Int32  inbuf_size,
                               M4OSA_UInt8 *outbuff, M4OSA_Int32 *outbuf_size);
//*- CR LV6775 -H.264 Trimming  */

typedef M4OSA_Void* M4ENCODER_Context;

typedef M4OSA_ERR (M4ENCODER_init) (
        M4ENCODER_Context* pContext,
        M4WRITER_DataInterface* pWriterDataInterface,
        M4VPP_apply_fct* pVPPfct,
        M4VPP_Context pVPPctxt,
        M4OSA_Void* pExternalAPI,
        M4OSA_Void* pUserData
);

typedef M4OSA_ERR (M4ENCODER_open) (
        M4ENCODER_Context pContext,
        M4SYS_AccessUnit* pAU,
        M4OSA_Void* pParams     /* Can be M4ENCODER_Params, M4ENCODER_AdvancedParams or
                                    M4ENCODER_StillPictureParams */
);

typedef M4OSA_ERR (M4ENCODER_start) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_stop) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_pause) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_resume) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_close) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_cleanup) (M4ENCODER_Context pContext);
typedef M4OSA_ERR (M4ENCODER_regulBitRate) (M4ENCODER_Context pContext);

typedef M4OSA_ERR (M4ENCODER_encode) (
        M4ENCODER_Context pContext,
        M4VIFI_ImagePlane* pInPlane,
        M4OSA_Double Cts,
        M4ENCODER_FrameMode FrameMode
);

typedef M4OSA_ERR (M4ENCODER_setOption)    (
        M4ENCODER_Context pContext,
        M4OSA_UInt32 optionID,
        M4OSA_DataOption optionValue
);

typedef M4OSA_ERR (M4ENCODER_getOption)    (
        M4ENCODER_Context pContext,
        M4OSA_UInt32 optionID,
        M4OSA_DataOption optionValue
);

/**
 ******************************************************************************
 * struct    M4ENCODER_GlobalInterface
 * @brief    Defines all the functions required for an encoder shell.
 ******************************************************************************
*/

typedef struct _M4ENCODER_GlobalInterface
{
    M4ENCODER_init*                pFctInit;
    M4ENCODER_open*                pFctOpen;

    M4ENCODER_start*            pFctStart;          /* Grabber mode */
    M4ENCODER_stop*                pFctStop;           /* Grabber mode */

    M4ENCODER_pause*            pFctPause;          /* Grabber mode */
    M4ENCODER_resume*            pFctResume;         /* Grabber mode */

    M4ENCODER_close*            pFctClose;
    M4ENCODER_cleanup*            pFctCleanup;

    M4ENCODER_regulBitRate*     pFctRegulBitRate;
    M4ENCODER_encode*            pFctEncode;         /* Standalone mode */

    M4ENCODER_setOption*        pFctSetOption;
    M4ENCODER_getOption*        pFctGetOption;
} M4ENCODER_GlobalInterface;


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4ENCODER_COMMON_H__*/

