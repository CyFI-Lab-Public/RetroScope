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
 * @file        M4VIFI_FiltersAPI.h
 * @brief        External API and Data definitions for the video filter library
 * @note        This file defines and declares data common to the video filter library:
 *                    -# data types
 *                    -# error codes
 *                    -# external API's
 *                    -# API level structure definition
 ******************************************************************************
*/

#ifndef _M4VIFI_FILTERSAPI_H_

#define _M4VIFI_FILTERSAPI_H_

#ifdef __cplusplus

extern "C" {

#endif /* __cplusplus */

    /**
     ***********************************************************
     *                    Data types definition
     ***********************************************************
    */

    typedef unsigned char M4VIFI_UInt8;
    typedef char M4VIFI_Int8;
    typedef unsigned short M4VIFI_UInt16;
    typedef unsigned long M4VIFI_UInt32;
    typedef short M4VIFI_Int16;
    typedef long M4VIFI_Int32;
    typedef float M4VIFI_Float;
    typedef double M4VIFI_Double;
    typedef unsigned char M4VIFI_ErrorCode;

/**
 ***********************************************************
 *                    Error codes definition
 ***********************************************************
*/
#define M4VIFI_OK                        0
#define M4VIFI_INVALID_PARAM            7
#define M4VIFI_ILLEGAL_FRAME_HEIGHT        8
#define M4VIFI_ILLEGAL_FRAME_WIDTH        9

/**
 ***********************************************************
 *                    Other basic definitions
 ***********************************************************
*/
#define CNST    const
#define EXTERN    extern

#ifndef NULL
#define NULL    0

#endif
#ifndef FALSE
#define FALSE    0
#define TRUE    !FALSE

#endif

/**
 ***********************************************************
 *                    Structures definition
 ***********************************************************
*/

/**
 ******************************************************************************
 * structure    M4VIFI_ImagePlane
 * @brief        Texture (YUV) planes structure
 * @note        This structure details the image planes for the output textures:
 *                sizes (in pixels) are luma plane sizes, the 3 pointers point
 *                to the Y, U and V buffers which store data in planar format.
 ******************************************************************************
*/

    typedef struct
        {
        M4VIFI_UInt32 u_width;   /**< Width of luma in pixel unit */
        M4VIFI_UInt32 u_height;  /**< Height of luma in pixel unit */
        M4VIFI_UInt32 u_topleft; /**< Pointer to first texture active pixel */
        M4VIFI_UInt32 u_stride;  /**< Stride value */
        M4VIFI_UInt8 *pac_data;  /**< Pointer to the data */
        } M4VIFI_ImagePlane;

/**
 ******************************************************************************
 * structure    M4VIFI_FramingData
 * @brief        Data necessary to add an overlay on an image
 * @note        This structure details the position and the data of the overlay
 ******************************************************************************
*/
    typedef struct
        {
        M4VIFI_UInt32
            m_xPosStep; /**< X positioning of the overlay vs main picture.
                                  X positioning is expressed in percentage vs the main
                                   picture width.
                                  m_xPosStep must be expressed by step of 1% and between
                                  -50/+50%.
                                  0% means overlay is centered vs main picture on
                                   X abscissa. */
        M4VIFI_UInt32
            m_yPosStep; /**< Y positioning of the overlay vs main picture.
                                  Y positioning is expressed in percentage vs the main
                                   picture width.
                                  m_xPosStep must be expressed by step of 1% and between
                                   -50/+50%.
                                  0% means overlay is centered vs main picture on
                                   Y abscissa. */

        M4VIFI_ImagePlane
            *
                m_imagePlane; /**< Pointer to the framing image with alpha channel */
        } M4VIFI_FramingData;

/**
 ******************************************************************************
 * structure    M4VIFI_HLSoffset
 * @brief        HLS offset structure
 * @note        This structure have the hue, saturation and lightness value
 *                for quality enhancement. Range of values neccessarily be
 *                hue = -360 to 360, sat = 0 to 100 and light = 0 t0 100
 ******************************************************************************
*/
    typedef struct
        {
        M4VIFI_Int16 hue;   /**< Hue offset */
        M4VIFI_Int16 sat;   /**< Saturation offset */
        M4VIFI_Int16 light; /**< Light offset */
        } M4VIFI_HLSoffset;

/**
 ******************************************************************************
 * structure    M4VIFI_Tranformation
 * @brief        Image Tranformation Structure
 * @note        Image Tranformation Request
 *                rotation : 1 -> +90deg Rotation
 *                          -1 -> -90deg Rotation
 *                           0 ->  No Rotation
 ******************************************************************************
*/
    typedef struct
        {
        M4VIFI_Int32 i32_rotation; /**< Rotation Flag        */
        } M4VIFI_Tranformation;

/**
 ******************************************************************************
 * structure    M4VIFI_pContext
 * @brief        New Structures
 * @note        -# Structure of M4VIFI_HLSoffset
 ******************************************************************************
*/
    typedef struct
        {
        M4VIFI_HLSoffset hlsOffset; /**< HLS offset structure */
        } M4VIFI_pContext;

    /*
     *****************************************************
     *                    External API functions
     *****************************************************
    */

    /**< Effect filters */
    M4VIFI_UInt8 M4VIFI_SepiaYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_GrayscaleYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_ContrastYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_NegativeYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_FlipYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_MirrorYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_Rotate180YUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_Rotate90RightYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_Rotate90LeftYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_ColorRYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_ColorGYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_ColorBYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_FramingRGB565toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_FramingYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_SetHueInYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_ColdYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    M4VIFI_UInt8 M4VIFI_WarmYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

/*                ADS Compiler                */

/*        Generic ARM assembly functions        */
#if defined ADS_ARM

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear RGB888toRGB888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear RGB565toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** RGB565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB565toYUV420AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BGR565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR565toYUV420AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV422 to YUV420 */
    M4VIFI_UInt8 M4VIFI_UYVYtoYUV420AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420 to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toRGB565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightAdsArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftAdsArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightAdsArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftAdsArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in RGB565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinRGB565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in BGR565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinBGR565AdsArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

#define M4VIFI_RGB565toYUV420                                M4VIFI_RGB565toYUV420AdsArm
#define M4VIFI_BGR565toYUV420                                M4VIFI_BGR565toYUV420AdsArm
#define M4VIFI_UYVYtoYUV420                                    M4VIFI_UYVYtoYUV420AdsArm
#define M4VIFI_YUV420toRGB565                                M4VIFI_YUV420toRGB565AdsArm
#define M4VIFI_YUV420toBGR565                                M4VIFI_YUV420toBGR565AdsArm
#define M4VIFI_ResizeBilinearYUV420toRGB565             \
                           M4VIFI_ResizeBilinearYUV420toRGB565AdsArm

#define M4VIFI_ResizeBilinearYUV420toBGR565             \
                           M4VIFI_ResizeBilinearYUV420toBGR565AdsArm

#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight \
                           M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightAdsArm

#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft  \
                           M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftAdsArm

#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedRight \
                           M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightAdsArm

#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeft  \
                           M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftAdsArm

#define M4VIFI_SetHLSinRGB565                                M4VIFI_SetHLSinRGB565AdsArm
#define M4VIFI_SetHLSinBGR565                                M4VIFI_SetHLSinBGR565AdsArm

/*        ARM9E assembly functions        */
#elif defined ADS_ARM9E

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV888toYUV888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV565toYUV565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** RGB565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB565toYUV420AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BGR565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR565toYUV420AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV422 to YUV420 */
    M4VIFI_UInt8 M4VIFI_UYVYtoYUV420AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420 to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toRGB565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightAdsArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftAdsArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightAdsArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftAdsArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in RGB565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinRGB565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in BGR565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinBGR565AdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize YUV420toYUV420 from QCIF to QVGA*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoYUV420QVGAAdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGAAdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA with rotation +90*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RRAdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA with rotation -90*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RLAdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
/** Resizes YUV420 Planar Image and stores in YUV420 Linear format with/without +or-90 rotation*/
    M4VIFI_UInt8 M4VIFI_YUV420PlanartoYUV420LinearAdsArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

#define M4VIFI_RGB565toYUV420                                M4VIFI_RGB565toYUV420AdsArm9E
#define M4VIFI_BGR565toYUV420                                M4VIFI_BGR565toYUV420AdsArm9E
#define M4VIFI_UYVYtoYUV420                                    M4VIFI_UYVYtoYUV420AdsArm9E
#define M4VIFI_YUV420toRGB565                                M4VIFI_YUV420toRGB565AdsArm9E
#define M4VIFI_YUV420toBGR565                                M4VIFI_YUV420toBGR565AdsArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565 \
                           M4VIFI_ResizeBilinearYUV420toRGB565AdsArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565 \
                           M4VIFI_ResizeBilinearYUV420toBGR565AdsArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight \
                           M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightAdsArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft \
                           M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftAdsArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedRight \
                           M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightAdsArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeft \
                           M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftAdsArm9E
#define M4VIFI_SetHLSinRGB565                            M4VIFI_SetHLSinRGB565AdsArm9E
#define M4VIFI_SetHLSinBGR565                            M4VIFI_SetHLSinBGR565AdsArm9E
#define M4VIFI_YUV420QCIFtoYUV420QVGA                    M4VIFI_YUV420QCIFtoYUV420QVGAAdsArm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA                    M4VIFI_YUV420QCIFtoRGB565QVGAAdsArm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA_RR                 M4VIFI_YUV420QCIFtoRGB565QVGA_RRAdsArm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA_RL                 M4VIFI_YUV420QCIFtoRGB565QVGA_RLAdsArm9E
#define M4VIFI_YUV420PlanartoYUV420Linear                M4VIFI_YUV420PlanartoYUV420LinearAdsArm9E
/*                GCC Compiler                */
/*        Generic ARM assembly functions        */

#elif defined GCC_ARM

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV888toYUV888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV565toYUV565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** RGB565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB565toYUV420GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BGR565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR565toYUV420GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420 to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toBGR565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightGccArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftGccArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toBGR565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightGccArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftGccArm(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Modify HLS in RGB565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinRGB565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Modify HLS in BGR565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinBGR565GccArm(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

#define M4VIFI_RGB565toYUV420                                M4VIFI_RGB565toYUV420GccArm
#define M4VIFI_BGR565toYUV420                                M4VIFI_BGR565toYUV420GccArm
#define M4VIFI_YUV420toRGB565                                M4VIFI_YUV420toRGB565GccArm
#define M4VIFI_YUV420toBGR565                                M4VIFI_YUV420toBGR565GccArm
#define M4VIFI_ResizeBilinearYUV420toRGB565 \
                               M4VIFI_ResizeBilinearYUV420toRGB565GccArm
#define M4VIFI_ResizeBilinearYUV420toBGR565 \
                               M4VIFI_ResizeBilinearYUV420toBGR565GccArm
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight \
                               M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightGccArm
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft \
                               M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftGccArm
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedRight \
                               M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightGccArm
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeft \
                               M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftGccArm
#define M4VIFI_SetHLSinRGB565                                M4VIFI_SetHLSinRGB565GccArm
#define M4VIFI_SetHLSinBGR565                                M4VIFI_SetHLSinBGR565GccArm

/*        ARM9E assembly functions        */
#elif defined GCC_ARM9E

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV888toYUV888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV565toYUV565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** RGB565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB565toYUV420GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BGR565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR565toYUV420GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420 to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toRGB565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightGccArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftGccArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize Bilinear YUV420toBGR565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightGccArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftGccArm9E(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in RGB565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinRGB565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in BGR565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinBGR565GccArm9E(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

#define M4VIFI_RGB565toYUV420                                M4VIFI_RGB565toYUV420GccArm9E
#define M4VIFI_BGR565toYUV420                                M4VIFI_BGR565toYUV420GccArm9E
#define M4VIFI_YUV420toRGB565                                M4VIFI_YUV420toRGB565GccArm9E
#define M4VIFI_YUV420toBGR565                                M4VIFI_YUV420toBGR565GccArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565 \
                                   M4VIFI_ResizeBilinearYUV420toRGB565GccArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565 \
                                   M4VIFI_ResizeBilinearYUV420toBGR565GccArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight \
                                   M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightGccArm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft \
                                   M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftGccArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedRight \
                                   M4VIFI_ResizeBilinearYUV420toBGR565RotatedRightGccArm9E
#define M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeft \
                                   M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeftGccArm9E
#define M4VIFI_SetHLSinBGR565                                M4VIFI_SetHLSinBGR565GccArm9E
#define M4VIFI_SetHLSinRGB565                                M4VIFI_SetHLSinRGB565GccArm9E

/* TI CCS assembly files */
#elif defined TI411_ARM9E

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV888toYUV888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV565toYUV565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** YUV420 (Planar) to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 (Planar) to Resized RGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 (Planar) to Resized RGB888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420(Planar) to Resized and Rotated (-90) RGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420(Planar) to Resized and Rotated (+90) RGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420(Planar) to Resized YUV420(Planar) */
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoYUV420QVGA(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** Resize YUV420(Planar) of QCIF to RGB565 of QVGA resolution */
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

/** Resize YUV420(Planar) of QCIF to RGB565 of QVGA resolution with rotation(-90) */
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RL(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

/** Resize YUV420(Planar) of QCIF to RGB565 of QVGA resolution with rotation(+90) */
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RR(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

#define M4VIFI_YUV420toRGB565                             M4VIFI_YUV420toRGB565Ti411Arm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565 \
                                M4VIFI_ResizeBilinearYUV420toRGB565Ti411Arm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft \
                               M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeftTi411Arm9E
#define M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight \
                               M4VIFI_ResizeBilinearYUV420toRGB565RotatedRightTi411Arm9E

#define M4VIFI_YUV420QCIFtoYUV420QVGA       M4VIFI_YUV420QCIFtoYUV420QVGATi411Arm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA       M4VIFI_YUV420QCIFtoRGB565QVGATi411Arm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA_RL  M4VIFI_YUV420QCIFtoRGB565QVGA_RLTi411Arm9E
#define M4VIFI_YUV420QCIFtoRGB565QVGA_RR  M4VIFI_YUV420QCIFtoRGB565QVGA_RRTi411Arm9E

/*        ANSI C Functions        */
#else

    /** Apply grayscale effect RGB565toRGB565 */

    M4VIFI_UInt8 M4VIFI_GrayscaleRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV888toYUV888 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB888toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV565toYUV565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearRGB565toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

    /** RGB565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB565toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BRG565 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR565toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** BRG888 to YUV420 */
    M4VIFI_UInt8 M4VIFI_BGR888toYUV420(void *pUserData,
        M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane PlaneOut[3]);
    /** RGB888 to YUV420 */
    M4VIFI_UInt8 M4VIFI_RGB888toYUV420(void *pUserData,
        M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane PlaneOut[3]);

    /** YUV422 to YUV420 */
    M4VIFI_UInt8 M4VIFI_UYVYtoYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);

    /** YUV420 to RGB565 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565RotatedLeft(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** YUV420 to BGR565 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR565RotatedRight(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** YUV420 to BGR24 */
    M4VIFI_UInt8 M4VIFI_YUV420toBGR24(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** YUV420 to RGB24 */
    M4VIFI_UInt8 M4VIFI_YUV420toRGB24(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** Resize Bilinear YUV420toYUV420 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toYUV420(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /** Resize Bilinear YUV420toRGB565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB888(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toRGB565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedRight(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toRGB565RotatedLeft(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Resize Bilinear YUV420toBGR565 with rotation +90 or -90 */
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedRight(
        void *pUserData,
            M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_ResizeBilinearYUV420toBGR565RotatedLeft(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in RGB565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinRGB565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /** Modify HLS in BGR565 */
    M4VIFI_UInt8 M4VIFI_SetHLSinBGR565(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *PlaneOut);
    /**Resize YUV420toYUV420 from QCIF to QVGA*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoYUV420QVGA(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA with rotation +90*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RR(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
    /**Resize YUV420toRGB565 from QCIF to QVGA with rotation -90*/
    M4VIFI_UInt8 M4VIFI_YUV420QCIFtoRGB565QVGA_RL(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
/** Resizes YUV420 Planar Image and stores in YUV420 Linear format with/without +or-90 rotation*/
    M4VIFI_UInt8 M4VIFI_YUV420PlanartoYUV420Linear(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);

/** Resizes YUV420 Planar Image and stores in YUV422 Interleaved format
     with/without +or-90 rotation*/
    M4VIFI_UInt8 M4VIFI_YUV420PlanartoYUV422Interleaved(void *pUserData,
        M4VIFI_ImagePlane *pPlaneIn, M4VIFI_ImagePlane *pPlaneOut);
#endif

    /** definition of the converter function types */

    typedef M4VIFI_UInt8 M4VIFI_PlanConverterFunctionType(void
        *pContext, M4VIFI_ImagePlane* in, M4VIFI_ImagePlane* out);

    /** definition of the preprocessing function types */
    typedef M4VIFI_UInt8 M4VIFI_PreprocessFunctionType(void
        *pContext, M4VIFI_ImagePlane* in, M4VIFI_ImagePlane* out);

    M4VIFI_UInt8 M4VIFI_YUV420toYUV420(void *user_data,
        M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_YUV420PlanarToYUV420Semiplanar(void *user_data,
        M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut);
    M4VIFI_UInt8 M4VIFI_SemiplanarYUV420toYUV420(void *user_data,
        M4VIFI_ImagePlane *PlaneIn, M4VIFI_ImagePlane *PlaneOut);
#ifdef __cplusplus

}

#endif /* __cplusplus */

#endif /* _M4VIFI_FILTERSAPI_H_ */

/* End of file M4VIFI_FiltersAPI.h */
