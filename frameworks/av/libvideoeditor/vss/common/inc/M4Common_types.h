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
 * @file   M4Common_Types.h
 * @brief  defines common structures
 * @note
 *
 ************************************************************************
*/
#ifndef M4COMMON_TYPES_H
#define M4COMMON_TYPES_H

#include "M4OSA_Types.h"
#include "M4OSA_Memory.h"

/**
 ************************************************************************
 * structure M4COMMON_MetadataType
 ************************************************************************
*/
typedef enum
{
    M4COMMON_kUnknownMetaDataType,
    /* Local files */
    M4COMMON_kTagID3v1,                /**<  Metadata from TAG ID3 V1 */
    M4COMMON_kTagID3v2,                /**<  Metadata from TAG ID3 V2 */
    M4COMMON_kASFContentDesc,        /**<  Metadata from ASF content description  */

    M4COMMON_k3GppAssetMovieBox,    /**<  Metadata from a 3gpp file (movie box) */
    M4COMMON_k3GppAssetTrackBox,    /**<  Metadata from a 3gpp file (track box) */

    /* Streaming */
    M4COMMON_kMetaDataSdpSession,    /**<  Metadata from an SDP file (Session level) */
    M4COMMON_kMetaDataSdpAudio,        /**<  Metadata from an SDP file (media audio level) */
    M4COMMON_kMetaDataSdpVideo,        /**<  Metadata from an SDP file (media video level) */

    M4COMMON_kJpegExif                /**< EXIF in JPEG */
} M4COMMON_MetadataType;

/**
 ************************************************************************
 * enumeration    M4VPS_EncodingFormat
 * @brief        Text encoding format
 ************************************************************************
*/
typedef enum
{
    M4COMMON_kEncFormatUnknown    = 0,      /**< Unknown format                                 */
    M4COMMON_kEncFormatASCII    = 1,        /**< ISO-8859-1. Terminated with $00                */
    M4COMMON_kEncFormatUTF8        = 2,     /**< UTF-8 encoded Unicode . Terminated with $00    */
    M4COMMON_kEncFormatUTF16    = 3         /**< UTF-16 encoded Unicode. Terminated with $00 00 */
}  M4COMMON_EncodingFormat;

/**
 ************************************************************************
 * structure    M4VPS_String
 * @brief        This structure defines string attribute
 ************************************************************************
*/
typedef struct
{
    M4OSA_Void*            m_pString;                /**< Pointer to text        */
    M4OSA_UInt32        m_uiSize;                /**< Text size in bytes        */
    M4COMMON_EncodingFormat    m_EncodingFormat;    /**< Text encoding format    */

} M4COMMON_String;

/**
 ************************************************************************
 * structure    M4COMMON_Buffer
 * @brief        This structure defines generic buffer attribute
 ************************************************************************
*/
typedef struct
{
    M4OSA_MemAddr8         m_pBuffer;        /**< Pointer to buffer        */
    M4OSA_UInt32        m_size;            /**< size of buffer in bytes    */
} M4COMMON_Buffer;

typedef enum
{
    M4COMMON_kMimeType_NONE,
    M4COMMON_kMimeType_JPG,
    M4COMMON_kMimeType_PNG,
    M4COMMON_kMimeType_BMP,   /* bitmap, with header */
    M4COMMON_kMimeType_RGB24, /* raw RGB 24 bits */
    M4COMMON_kMimeType_RGB565, /* raw, RGB 16 bits */
    M4COMMON_kMimeType_YUV420,
    M4COMMON_kMimeType_MPEG4_IFrame /* RC: to support PV art */

} M4COMMON_MimeType;

/* picture type definition from id3v2 tag*/
typedef enum
{
    M4COMMON_kPicType_Other                = 0x00,
    M4COMMON_kPicType_32_32_Icon            = 0x01,
    M4COMMON_kPicType_Other_Icon            = 0x02,
    M4COMMON_kPicType_FrontCover            = 0x03,
    M4COMMON_kPicType_BackCover            = 0x04,
    M4COMMON_kPicType_LeafletPage            = 0x05,
    M4COMMON_kPicType_Media                = 0x06,
    M4COMMON_kPicType_LeadArtist            = 0x07,
    M4COMMON_kPicType_Artist                = 0x08,
    M4COMMON_kPicType_Conductor            = 0x09,
    M4COMMON_kPicType_Orchestra            = 0x0A,
    M4COMMON_kPicType_Composer            = 0x0B,
    M4COMMON_kPicType_Lyricist            = 0x0C,
    M4COMMON_kPicType_RecordingLocation    = 0x0D,
    M4COMMON_kPicType_DuringRecording        = 0x0E,
    M4COMMON_kPicType_DuringPerformance    = 0x0F,
    M4COMMON_kPicType_MovieScreenCapture    = 0x10,
    M4COMMON_kPicType_BrightColouredFish    = 0x11,
    M4COMMON_kPicType_Illustration        = 0x12,
    M4COMMON_kPicType_ArtistLogo            = 0x13,
    M4COMMON_kPicType_StudioLogo            = 0x14
} M4COMMON_PictureType;

/**
 ******************************************************************************
 * enum        M4COMMON_Orientation
 * @brief        This enum defines the possible orientation of a frame as described
 *            in the EXIF standard.
 ******************************************************************************
*/
typedef enum
{
    M4COMMON_kOrientationUnknown = 0,
    M4COMMON_kOrientationTopLeft,
    M4COMMON_kOrientationTopRight,
    M4COMMON_kOrientationBottomRight,
    M4COMMON_kOrientationBottomLeft,
    M4COMMON_kOrientationLeftTop,
    M4COMMON_kOrientationRightTop,
    M4COMMON_kOrientationRightBottom,
    M4COMMON_kOrientationLeftBottom
}M4COMMON_Orientation ;

/**
 ******************************************************************************
 * structure    M4EXIFC_Location
 * @brief        The Image GPS location (example : 48°52.21' )
 ******************************************************************************
*/
typedef struct
{
    M4OSA_Float    degrees;
    M4OSA_Float    minsec;
} M4COMMON_Location;

/**
 ************************************************************************
 * structure    M4COMMON_MetaDataAlbumArt
 * @brief        This structure defines fields of a album art
 ************************************************************************
*/
typedef struct
{
    M4COMMON_MimeType    m_mimeType;
    M4OSA_UInt32        m_uiSize;
    M4OSA_Void*            m_pData;

    M4COMMON_String        m_pDescription;

} M4COMMON_MetaDataAlbumArt;

/**
 ************************************************************************
 * structure    M4COMMON_MetaDataFields
 * @brief        This structure defines fields of metadata information
 ************************************************************************
*/
typedef struct
{
    M4COMMON_MetadataType    m_MetadataType;

    /* Meta data fields */
    M4COMMON_String    m_pTitle;            /**< Title for the media  */
    M4COMMON_String    m_pArtist;            /**< Performer or artist */
    M4COMMON_String    m_pAlbum;            /**< Album title for the media */
    M4COMMON_String    m_pAuthor;            /**< Author of the media */
    M4COMMON_String    m_pGenre;            /**< Genre (category and style) of the media */
    M4COMMON_String    m_pDescription;        /**< Caption or description for the media */
    M4COMMON_String    m_pCopyRights;        /**< Notice about organization holding copyright
                                                     for the media file */
    M4COMMON_String    m_pRecordingYear;    /**< Recording year for the media */
    M4COMMON_String    m_pRating;            /**< Media rating */

    M4COMMON_String    m_pClassification;    /**< Classification of the media */
    M4COMMON_String    m_pKeyWords;        /**< Media keywords */
    M4COMMON_String    m_pLocation;        /**< Location information */
    M4COMMON_String    m_pUrl;                /**< Reference of the resource */

    M4OSA_UInt8        m_uiTrackNumber;    /**< Track number for the media*/
    M4OSA_UInt32    m_uiDuration;        /**< The track duration in milliseconds */

    M4COMMON_MetaDataAlbumArt    m_albumArt;    /**< AlbumArt description */
    M4COMMON_String                m_pMood;    /**< Mood of the media */

    /**< Modifs ACO 4/12/07 : add Exif specific infos */
    M4COMMON_String    m_pCreationDateTime;    /**< date and time original image was generated */
    M4COMMON_String    m_pLastChangeDateTime;    /**< file change date and time */
    M4COMMON_String    m_pManufacturer;        /**< manufacturer of image input equipment */
    M4COMMON_String    m_pModel;                /**< model of image input equipment */
    M4COMMON_String    m_pSoftware;            /**< software used */
    M4COMMON_Orientation m_Orientation;        /**< Orientation of the picture */

    /**< Modifs FS 29/08/08 : additionnal Exif infos */
    M4OSA_UInt32    m_width;            /**< image width in pixels */
    M4OSA_UInt32    m_height;            /**< image height in pixels */
    M4OSA_UInt32    m_thumbnailSize;    /**< size of the thumbnail */
    M4COMMON_String    m_pLatitudeRef;        /**< Latitude reference */
    M4COMMON_Location m_latitude;        /**< Latitude */
    M4COMMON_String    m_pLongitudeRef;    /**< Longitude reference */
    M4COMMON_Location m_longitude;        /**< Longitude  */

} M4COMMON_MetaDataFields;


#endif /*M4COMMON_TYPES_H*/

