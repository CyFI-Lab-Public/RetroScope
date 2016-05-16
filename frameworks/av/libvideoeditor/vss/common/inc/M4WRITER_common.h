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
 * @file    M4WRITER_common.h
 * @brief    VES writers shell interface.
 * @note    This file defines the types internally used by the VES to abstract writers
 ******************************************************************************
*/
#ifndef __M4WRITER_COMMON_H__
#define __M4WRITER_COMMON_H__

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include "M4OSA_Types.h"
#include "M4OSA_FileWriter.h"   /* for M4OSA_FileWriterPointer */
#include "M4OSA_FileReader.h"   /* for M4OSA_FileWriterPointer */
#include "M4OSA_OptionID.h"     /* for M4OSA_OPTION_ID_CREATE() */
#include "M4OSA_CoreID.h"       /* for M4WRITER_COMMON */

#include "M4SYS_Stream.h"       /* for M4SYS_StreamID */
#include "M4SYS_AccessUnit.h"   /* for M4SYS_AccessUnit */

/**
 ******************************************************************************
 * MP4W Errors & Warnings definition
 ******************************************************************************
*/
#define M4WAR_WRITER_STOP_REQ        M4OSA_ERR_CREATE(M4_WAR, M4WRITER_COMMON ,0x000001)

/**
 ******************************************************************************
 * enum        M4WRITER_OutputFileType
 * @brief    This enum defines the avalaible output file format.
 ******************************************************************************
*/
typedef enum
{
    M4WRITER_kUnknown=-1,
    M4WRITER_k3GPP=0,            /**< 3GPP compliant file */
    M4WRITER_kAVI=1,            /**< AVI file */
    M4WRITER_kAMR=2,            /**< AMR file */
    M4WRITER_kNETWORK3GPP=3,    /**< 3GPP via TCP */
    M4WRITER_kPCM=4,            /**< PCM file */
    M4WRITER_kJPEG=5,            /**< JPEG EXIF writer */
    M4WRITER_kMP3=6,            /**< MP3 writer */

    M4WRITER_kType_NB  /* number of writers, keep it as last enum entry */

} M4WRITER_OutputFileType;

/**
 ******************************************************************************
 * enum    M4WRITER_OptionID
 * @brief    This enums defines all avalaible options. All the reuturned values are in
 *           M4OSA_UInt32 type.
 ******************************************************************************
*/
typedef enum {
    M4WRITER_kMaxAUSize        = M4OSA_OPTION_ID_CREATE (M4_READ|M4_WRITE, M4WRITER_COMMON, 0x01),
    M4WRITER_kMaxChunckSize    = M4OSA_OPTION_ID_CREATE (M4_READ|M4_WRITE, M4WRITER_COMMON, 0x02),
    M4WRITER_kFileSize          = M4OSA_OPTION_ID_CREATE (M4_READ            , \
        M4WRITER_COMMON, 0x03),  /**< File size if the process was ended when we call the method */
    M4WRITER_kFileSizeAudioEstimated= M4OSA_OPTION_ID_CREATE (M4_READ    ,\
         M4WRITER_COMMON, 0x04),    /**< File size if the process was ended when we call the
                                     method, estimated size for audio */
    M4WRITER_kEmbeddedString  = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x05),    /**< String embedded at the end of the file(SW - VES) */
    M4WRITER_kEmbeddedVersion = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x06),    /**< Version embedded at the end of the file */
    M4WRITER_kIntegrationTag  = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x07),    /**< String embedded at the end of the file (char[60]
                                         for integration purpose) */
    M4WRITER_kMaxFileSize      = M4OSA_OPTION_ID_CREATE (M4_WRITE        , \
        M4WRITER_COMMON, 0x08),    /**< Maximum file size limitation */
    M4WRITER_kMaxFileDuration = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x09),    /**< Maximum file duration limitation */
    M4WRITER_kSetFtypBox      = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x0A),    /**< Set 'ftyp' atom */
    M4WRITER_kMetaData          = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x0B),    /**< Additionnal information to set in the file */
    M4WRITER_kDSI          = M4OSA_OPTION_ID_CREATE (M4_WRITE        , \
        M4WRITER_COMMON, 0x0C),    /**< To set DSI of the file (Decoder specifc info) */
    M4WRITER_kJpegReserveFPData     = M4OSA_OPTION_ID_CREATE (M4_WRITE        ,\
         M4WRITER_COMMON, 0x0D),    /**< Reserve some space in the file for JPEG fast
                                        processing data */
    M4WRITER_kJpegSetFPData     = M4OSA_OPTION_ID_CREATE (M4_WRITE        , \
        M4WRITER_COMMON, 0x0E),    /**< Write Fast Processing Data in the file*/
    /* + CRLV6775 -H.264 trimming */
    M4WRITER_kMUL_PPS_SPS       = M4OSA_OPTION_ID_CREATE (M4_WRITE        , M4WRITER_COMMON, 0x0F)
    /* - CRLV6775 -H.264 trimming */
} M4WRITER_OptionID;


/**
 ******************************************************************************
 * struct    M4WRITER_Header
 * @brief    This structure defines the buffer where an header is put.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8    pBuf;        /**< Buffer for the header */
    M4OSA_UInt32    Size;        /**< Size of the data */
} M4WRITER_Header;


/**
 ******************************************************************************
 * struct    M4WRITER_StreamVideoInfos
 * @brief    This structure defines the specific video stream infos, extension to
 *           M4SYS_StreamDescription.
 ******************************************************************************
*/
typedef struct {
    M4OSA_UInt32    height;                /**< Frame height */
    M4OSA_UInt32    width;                /**< Frame Width */
    M4OSA_Double    fps;                /**< Targetted framerate of the video */
    M4WRITER_Header    Header;                /**< Sequence header of the video stream,
                                        member set to NULL if no header present */
} M4WRITER_StreamVideoInfos;


/**
 ******************************************************************************
 * struct    M4WRITER_StreamAudioInfos
 * @brief    This structure defines the specific audio stream infos, extension to
             M4SYS_StreamDescription.
 ******************************************************************************
*/
typedef struct {
    M4OSA_UInt32    nbSamplesPerSec;    /**< Number of Samples per second */
    M4OSA_UInt16    nbBitsPerSample;    /**< Number of Bits in 1 sample */
    M4OSA_UInt16    nbChannels;            /**< Number of channels */
    M4WRITER_Header    Header;                /**< Decoder Specific Info of the audiostream,
                                             member set to NULL if no DSI present */
} M4WRITER_StreamAudioInfos;


/**
 ******************************************************************************
 * enum        M4WRITER_Orientation
 * @brief    This enum defines the possible orientation of a frame as described
 *            in the EXIF standard.
 ******************************************************************************
*/
typedef enum
{
    M4WRITER_OrientationUnknown = 0,
    M4WRITER_OrientationTopLeft,
    M4WRITER_OrientationTopRight,
    M4WRITER_OrientationBottomRight,
    M4WRITER_OrientationBottomLeft,
    M4WRITER_OrientationLeftTop,
    M4WRITER_OrientationRightTop,
    M4WRITER_OrientationRightBottom,
    M4WRITER_OrientationLeftBottom
}M4WRITER_Orientation ;

/**
 ******************************************************************************
 * struct    M4WRITER_MetaData
 * @brief    This structure defines all the meta data to store in the encoded file.
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Char*                Description ;
    M4OSA_Char*                PhoneManufacturer ;
    M4OSA_Char*                PhoneModel ;
    M4OSA_Char*                Artist ;
    M4OSA_Char*                Copyright ;
    M4OSA_Char*                Software ;
    M4OSA_Char*                CreationDate;
    M4WRITER_Orientation    Orientation ;

    M4OSA_UInt32            Width ;
    M4OSA_UInt32            Height ;

    M4OSA_UInt32            ThumbnailWidth ;
    M4OSA_UInt32            ThumbnailHeight ;
    M4OSA_Bool                ThumbnailPresence ;
}M4WRITER_MetaData;


typedef void* M4WRITER_Context;

typedef M4OSA_ERR (M4WRITER_openWrite)        (M4WRITER_Context* hContext,\
                                             void* outputFileDescriptor,\
                                             M4OSA_FileWriterPointer* pFileWriterPointer,\
                                             void* tempFileDescriptor, \
                                             M4OSA_FileReadPointer* pFileReaderPointer);
typedef M4OSA_ERR (M4WRITER_addStream)        (M4WRITER_Context  pContext,\
                                            M4SYS_StreamDescription*streamDescription);
typedef M4OSA_ERR (M4WRITER_startWriting)    (M4WRITER_Context  pContext);
typedef M4OSA_ERR (M4WRITER_closeWrite)        (M4WRITER_Context  pContext);
typedef M4OSA_ERR (M4WRITER_setOption)        (M4WRITER_Context  pContext, \
                                            M4OSA_UInt32 optionID, \
                                            M4OSA_DataOption optionValue);
typedef M4OSA_ERR (M4WRITER_getOption)        (M4WRITER_Context  pContext, \
                                            M4OSA_UInt32 optionID, \
                                            M4OSA_DataOption optionValue);


/**
 ******************************************************************************
 * struct    M4WRITER_GlobalInterface
 * @brief    Defines all the functions required for a writer shell.
 ******************************************************************************
*/
typedef struct _M4WRITER_GlobalInterface
{
    M4WRITER_openWrite*             pFctOpen;
    M4WRITER_addStream*                pFctAddStream;
    M4WRITER_startWriting*          pFctStartWriting;
    M4WRITER_closeWrite*            pFctCloseWrite;
    M4WRITER_setOption*                pFctSetOption;
    M4WRITER_getOption*                pFctGetOption;
} M4WRITER_GlobalInterface;

typedef M4OSA_ERR  M4WRITER_startAU(M4WRITER_Context pContext, M4SYS_StreamID streamID,\
                                     M4SYS_AccessUnit* pAU);
typedef M4OSA_ERR  M4WRITER_processAU(M4WRITER_Context pContext, M4SYS_StreamID streamID,\
                                     M4SYS_AccessUnit* pAU);

/**
 ******************************************************************************
 * struct    M4WRITER_DataInterface
 * @brief    Defines all the functions required to write data with a writer shell.
 ******************************************************************************
*/
typedef struct _M4WRITER_DataInterface
{
    M4WRITER_startAU*    pStartAU;
    M4WRITER_processAU* pProcessAU;

    M4WRITER_Context    pWriterContext;

} M4WRITER_DataInterface;

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /*__M4WRITER_COMMON_H__*/

