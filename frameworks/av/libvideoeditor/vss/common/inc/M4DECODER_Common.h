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
 ************************************************************************
 * @file   M4DECODER_Common.h
 * @brief  Shell Decoder common interface declaration
 * @note   This file declares the common interfaces that decoder shells must implement
 *
 ************************************************************************
*/
#ifndef __M4DECODER_COMMON_H__
#define __M4DECODER_COMMON_H__

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_OptionID.h"
#include "M4OSA_CoreID.h"

#include "M4READER_Common.h"
#include "M4VIFI_FiltersAPI.h"

#include "M4_Utils.h"

/* ----- Errors and Warnings ----- */

/**
 * Warning: there is no new decoded frame to render since the last rendering
 */
#define M4WAR_VIDEORENDERER_NO_NEW_FRAME M4OSA_ERR_CREATE(M4_WAR, M4DECODER_COMMON, 0x0001)
/**
 * Warning: the deblocking filter is not implemented
 */
#define M4WAR_DEBLOCKING_FILTER_NOT_IMPLEMENTED M4OSA_ERR_CREATE(M4_WAR, M4DECODER_COMMON,\
                                                                     0x000002)


/* Error: Stream H263 profiles (other than  0) are not supported */
#define M4ERR_DECODER_H263_PROFILE_NOT_SUPPORTED            M4OSA_ERR_CREATE(M4_ERR,\
                                                                 M4DECODER_MPEG4, 0x0001)
/* Error: Stream H263 not baseline not supported (Supported sizes are CIF, QCIF or SQCIF) */
#define M4ERR_DECODER_H263_NOT_BASELINE                        M4OSA_ERR_CREATE(M4_ERR,\
                                                                 M4DECODER_MPEG4, 0x0002)

/**
 ************************************************************************
 * enum     M4DECODER_AVCProfileLevel
 * @brief    This enum defines the AVC decoder profile and level for the current instance
 * @note    This options can be read from decoder via M4DECODER_getOption_fct
 ************************************************************************
*/
typedef enum
{
    M4DECODER_AVC_kProfile_0_Level_1 = 0,
    M4DECODER_AVC_kProfile_0_Level_1b,
    M4DECODER_AVC_kProfile_0_Level_1_1,
    M4DECODER_AVC_kProfile_0_Level_1_2,
    M4DECODER_AVC_kProfile_0_Level_1_3,
    M4DECODER_AVC_kProfile_0_Level_2,
    M4DECODER_AVC_kProfile_0_Level_2_1,
    M4DECODER_AVC_kProfile_0_Level_2_2,
    M4DECODER_AVC_kProfile_0_Level_3,
    M4DECODER_AVC_kProfile_0_Level_3_1,
    M4DECODER_AVC_kProfile_0_Level_3_2,
    M4DECODER_AVC_kProfile_0_Level_4,
    M4DECODER_AVC_kProfile_0_Level_4_1,
    M4DECODER_AVC_kProfile_0_Level_4_2,
    M4DECODER_AVC_kProfile_0_Level_5,
    M4DECODER_AVC_kProfile_0_Level_5_1,
    M4DECODER_AVC_kProfile_and_Level_Out_Of_Range = 255
} M4DECODER_AVCProfileLevel;

/**
 ************************************************************************
 * enum     M4DECODER_OptionID
 * @brief    This enum defines the decoder options
 * @note    These options can be read from or written to a decoder via M4DECODER_getOption_fct
 ************************************************************************
*/
typedef enum
{
    /**
    Get the version of the core decoder
    */
    M4DECODER_kOptionID_Version        = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x01),
    /**
    Get the size of the currently decoded video
    */
    M4DECODER_kOptionID_VideoSize    = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x02),
    /**
    Set the conversion filter to use at rendering
    */
    M4DECODER_kOptionID_OutputFilter = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x03),
    /**
    Activate the Deblocking filter
    */
    M4DECODER_kOptionID_DeblockingFilter = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x04),
    /**
    Get nex rendered frame CTS
    */
    M4DECODER_kOptionID_NextRenderedFrameCTS = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON,\
                                                                         0x05),

    /**
    Set the YUV data to the dummy video decoder
    */
    M4DECODER_kOptionID_DecYuvData =
        M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x06),
    /**
    Set the YUV data with color effect applied to the dummy video decoder
    */
    M4DECODER_kOptionID_YuvWithEffectNonContiguous =
        M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x07),

    M4DECODER_kOptionID_YuvWithEffectContiguous =
        M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x08),

    M4DECODER_kOptionID_EnableYuvWithEffect =
        M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x09),

    /**
     * Get the supported video decoders and capabilities */
    M4DECODER_kOptionID_VideoDecodersAndCapabilities =
        M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_COMMON, 0x10),

    /* common to MPEG4 decoders */
    /**
     * Get the DecoderConfigInfo */
    M4DECODER_MPEG4_kOptionID_DecoderConfigInfo = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                         M4DECODER_MPEG4, 0x01),

    /* last decoded cts */
    M4DECODER_kOptionID_AVCLastDecodedFrameCTS = M4OSA_OPTION_ID_CREATE(M4_READ, M4DECODER_AVC,\
                                                                             0x01)
/* Last decoded cts */

} M4DECODER_OptionID;


/**
 ************************************************************************
 * struct    M4DECODER_MPEG4_DecoderConfigInfo
 * @brief    Contains info read from the MPEG-4 VideoObjectLayer.
 ************************************************************************
*/
typedef struct
{
    M4OSA_UInt8        uiProfile;                /**< profile and level as defined in the Visual
                                                         Object Sequence header, if present */
    M4OSA_UInt32    uiTimeScale;            /**< time scale as parsed in VOL header */
    M4OSA_UInt8        uiUseOfResynchMarker;    /**< Usage of resynchronization marker */
    M4OSA_Bool        bDataPartition;            /**< If 1 data partitioning is used. */
    M4OSA_Bool        bUseOfRVLC;                /**< Usage of RVLC for the stream */

} M4DECODER_MPEG4_DecoderConfigInfo;


/**
 ***********************************************************************
 * structure    M4DECODER_VideoSize
 * @brief        This structure defines the video size (width and height)
 * @note        This structure is used to retrieve via the M4DECODER_getOption_fct
 *                function the size of the current decoded video
 ************************************************************************
*/
typedef struct _M4DECODER_VideoSize
{
    M4OSA_UInt32   m_uiWidth;    /**< video width  in pixels */
    M4OSA_UInt32   m_uiHeight;    /**< video height in pixels */

} M4DECODER_VideoSize;

/**
 ************************************************************************
 * structure    M4DECODER_OutputFilter
 * @brief        This structure defines the conversion filter
 * @note        This structure is used to retrieve the filter function
 *                pointer and its user data via the function
 *                M4DECODER_getOption_fct    with the option
 *                M4DECODER_kOptionID_OutputFilter
 ************************************************************************
*/
typedef struct _M4DECODER_OutputFilter
{
    M4OSA_Void   *m_pFilterFunction;    /**< pointer to the filter function */
    M4OSA_Void   *m_pFilterUserData;    /**< user data of the filter        */

} M4DECODER_OutputFilter;

/**
 ************************************************************************
 * enum     M4DECODER_VideoType
 * @brief    This enum defines the video types used to create decoders
 * @note    This enum is used internally by the VPS to identify a currently supported
 *            video decoder interface. Each decoder is registered with one of this type associated.
 *            When a decoder instance is needed, this type is used to identify and
 *            and retrieve its interface.
 ************************************************************************
*/
typedef enum
{
    M4DECODER_kVideoTypeMPEG4 = 0,
    M4DECODER_kVideoTypeMJPEG,
    M4DECODER_kVideoTypeAVC,
    M4DECODER_kVideoTypeWMV,
    M4DECODER_kVideoTypeREAL,
    M4DECODER_kVideoTypeYUV420P,

    M4DECODER_kVideoType_NB  /* number of decoders, keep it as last enum entry */

} M4DECODER_VideoType ;

typedef struct {
    M4OSA_UInt32 mProfile;
    M4OSA_UInt32 mLevel;
} VideoProfileLevel;

typedef struct {
    VideoProfileLevel *profileLevel;
    M4OSA_UInt32 profileNumber;
} VideoComponentCapabilities;

typedef struct {
    M4_StreamType codec;
    VideoComponentCapabilities *component;
    M4OSA_UInt32 componentNumber;
} VideoDecoder;

typedef struct {
    VideoDecoder *decoder;
    M4OSA_UInt32 decoderNumber;
} M4DECODER_VideoDecoders;
/**
 ************************************************************************
 * @brief    creates an instance of the decoder
 * @note    allocates the context
 *
 * @param    pContext:        (OUT)    Context of the decoder
 * @param    pStreamHandler:    (IN)    Pointer to a video stream description
 * @param    pGlobalInterface:  (IN)    Pointer to the M4READER_GlobalInterface structure that must
 *                                       be used by the decoder to read data from the stream
 * @param    pDataInterface:    (IN)    Pointer to the M4READER_DataInterface structure that must
 *                                       be used by the decoder to read data from the stream
 * @param    pAccessUnit        (IN)    Pointer to an access unit (allocated by the caller)
 *                                      where the decoded data are stored
 *
 * @return    M4NO_ERROR                 there is no error
 * @return  M4ERR_STATE             State automaton is not applied
 * @return    M4ERR_ALLOC                a memory allocation has failed
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_create_fct)    (M4OSA_Context *pContext,
                                                 M4_StreamHandler *pStreamHandler,
                                                 M4READER_GlobalInterface *pGlobalInterface,
                                                 M4READER_DataInterface *pDataInterface,
                                                 M4_AccessUnit *pAccessUnit,
                                                 M4OSA_Void* pUserData);

/**
 ************************************************************************
 * @brief    destroy the instance of the decoder
 * @note    after this call the context is invalid
 *
 * @param    context:    (IN)    Context of the decoder
 *
 * @return    M4NO_ERROR             There is no error
 * @return  M4ERR_PARAMETER     The context is invalid (in DEBUG only)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_destroy_fct)    (M4OSA_Context context);

/**
 ************************************************************************
 * @brief    get an option value from the decoder
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to retrieve a property value:
 *          -the version number of the decoder
 *          -the size (widthxheight) of the image
 *
 * @param    context:    (IN)        Context of the decoder
 * @param    optionId:    (IN)        indicates the option to set
 * @param    pValue:        (IN/OUT)    pointer to structure or value (allocated by user) where
 *                                      option is stored
 * @return    M4NO_ERROR                 there is no error
 * @return  M4ERR_PARAMETER         The context is invalid (in DEBUG only)
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_getOption_fct)(M4OSA_Context context, M4OSA_OptionID optionId,
                                             M4OSA_DataOption pValue);

/**
 ************************************************************************
 * @brief   set an option value of the decoder
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to set a property value:
 *          -the conversion filter to use at rendering
 *
 * @param   context:    (IN)        Context of the decoder
 * @param   optionId:   (IN)        Identifier indicating the option to set
 * @param   pValue:     (IN)        Pointer to structure or value (allocated by user)
 *                                     where option is stored
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_BAD_OPTION_ID     The option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_PARAMETER         The option parameter is invalid
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_setOption_fct)(M4OSA_Context context, M4OSA_OptionID optionId,
                                                 M4OSA_DataOption pValue);

/**
 ************************************************************************
 * @brief   Decode Access Units up to a target time
 * @note    Parse and decode the stream until it is possible to output a decoded image for which
 *            the composition time is equal or greater to the passed targeted time
 *          The data are read from the reader data interface
 *
 * @param    context:    (IN)        Context of the decoder
 * @param    pTime:        (IN/OUT)    IN: Time to decode up to (in milli secondes)
 *                                    OUT:Time of the last decoded frame (in ms)
 * @param   bJump:      (IN)        0 if no jump occured just before this call
 *                                  1 if a a jump has just been made
 * @param   tolerance:      (IN)        We may decode an earlier frame within the tolerance.
 *                                      The time difference is specified in milliseconds.
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4WAR_NO_MORE_AU        there is no more access unit to decode (end of stream)
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_decode_fct)    (M4OSA_Context context, M4_MediaTime* pTime,
                                                 M4OSA_Bool bJump, M4OSA_UInt32 tolerance);

/**
 ************************************************************************
 * @brief    Renders the video at the specified time.
 * @note
 * @param    context:     (IN)        Context of the decoder
 * @param   pTime:       (IN/OUT)   IN: Time to render to (in milli secondes)
 *                OUT:Time of the actually rendered frame (in ms)
 * @param    pOutputPlane:(OUT)        Output plane filled with decoded data (converted)
 * @param   bForceRender:(IN)       1 if the image must be rendered even it has already been
 *                                  0 if not (in which case the function can return
 *                                       M4WAR_VIDEORENDERER_NO_NEW_FRAME)
 * @return    M4NO_ERROR                 There is no error
 * @return    M4ERR_PARAMETER            At least one parameter is not properly set
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_ALLOC             There is no more available memory
 * @return    M4WAR_VIDEORENDERER_NO_NEW_FRAME    If the frame to render has already been rendered
 ************************************************************************
*/
typedef M4OSA_ERR  (M4DECODER_render_fct)    (M4OSA_Context context, M4_MediaTime* pTime,
                                              M4VIFI_ImagePlane* pOutputPlane,
                                              M4OSA_Bool bForceRender);

/**
 ************************************************************************
 * structure    M4DECODER_VideoInterface
 * @brief        This structure defines the generic video decoder interface
 * @note        This structure stores the pointers to functions of one video decoder type.
 *                The decoder type is one of the M4DECODER_VideoType
 ************************************************************************
*/
typedef struct _M4DECODER_VideoInterface
{
    M4DECODER_create_fct*        m_pFctCreate;
    M4DECODER_destroy_fct*        m_pFctDestroy;
    M4DECODER_getOption_fct*    m_pFctGetOption;
    M4DECODER_setOption_fct*    m_pFctSetOption;
    M4DECODER_decode_fct*        m_pFctDecode;
    M4DECODER_render_fct*        m_pFctRender;
} M4DECODER_VideoInterface;

#endif /*__M4DECODER_COMMON_H__*/
