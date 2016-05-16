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
 * @file   M4READER_Common.h
 * @brief  Shell Reader common interface declaration
 * @note   This file declares the common interfaces that reader shells must implement
 *
 ************************************************************************
*/
#ifndef __M4READER_COMMON_H__
#define __M4READER_COMMON_H__

#include "M4OSA_Types.h"
#include "M4OSA_Error.h"
#include "M4OSA_FileReader.h"
#include "M4OSA_CoreID.h"
#include "M4DA_Types.h"
#include "M4Common_types.h"

/* ERRORS */
#define M4ERR_READER_UNKNOWN_STREAM_TYPE        M4OSA_ERR_CREATE(M4_ERR, M4READER_COMMON, 0x0001)

/* WARNINGS */
#define M4WAR_READER_NO_METADATA                M4OSA_ERR_CREATE(M4_WAR, M4READER_COMMON, 0x0001)
#define M4WAR_READER_INFORMATION_NOT_PRESENT    M4OSA_ERR_CREATE(M4_WAR, M4READER_COMMON, 0x0002)


/**
 ************************************************************************
 * enum        M4READER_MediaType
 * @brief    This enum defines the Media types used to create media readers
 * @note    This enum is used internally by the VPS to identify a currently supported
 *          media reader interface. Each reader is registered with one of this type associated.
 *          When a reader instance is needed, this type is used to identify and
 *          and retrieve its interface.
 ************************************************************************
*/
typedef enum
{
    M4READER_kMediaTypeUnknown        = -1,    /**< Unknown media type */
    M4READER_kMediaType3GPP            = 0,    /**< 3GPP file media type */
    M4READER_kMediaTypeAVI            = 1,    /**< AVI file media type */
    M4READER_kMediaTypeAMR            = 2,    /**< AMR file media type */
    M4READER_kMediaTypeMP3            = 3,    /**< MP3 file media type */
    M4READER_kMediaTypeRTSP            = 4,    /**< RTSP network accessed media type */
    M4READER_kMediaType3GPPHTTP        = 5,    /**< Progressively downloaded 3GPP file media type */
    M4READER_kMediaTypePVHTTP        = 6,    /**< Packet Video HTTP proprietary type */
    M4READER_kMediaTypeWAV            = 7,    /**< WAV file media type */
    M4READER_kMediaType3GPEXTHTTP    = 8,    /**< An external progressively downloaded 3GPP file
                                                     media type */
    M4READER_kMediaTypeAAC            = 9,    /**< ADTS and ADIF AAC support */
    M4READER_kMediaTypeREAL            = 10,    /**< REAL Media type */
    M4READER_kMediaTypeASF            = 11,    /**< ASF Media type */
    M4READER_kMediaTypeFLEXTIME        = 12,    /**< FlexTime Media type */
    M4READER_kMediaTypeBBA            = 13,    /**< Beatbrew audio Media type */
    M4READER_kMediaTypeSYNTHAUDIO    = 14,    /**< Synthesis audio Media type */
    M4READER_kMediaTypePCM            = 15,    /**< PCM Media type */
    M4READER_kMediaTypeJPEG            = 16,    /**< JPEG Media type */
    M4READER_kMediaTypeGIF            = 17,    /**< GIF Media type */
    M4READER_kMediaTypeADIF            = 18,    /**< AAC-ADTS Media type */
    M4READER_kMediaTypeADTS            = 19,    /**< AAC-ADTS Media type */

    M4READER_kMediaType_NB  /* number of readers, keep it as last enum entry */

} M4READER_MediaType;

/**
 ************************************************************************
 * enum        M4READER_MediaFamily
 * @brief    This enum defines the Media family of a stream
 * @note    This enum is used internally by the VPS to identify what kind of stream
 *          has been retrieved via getNextStream() function.
 ************************************************************************
*/
typedef enum
{
    M4READER_kMediaFamilyUnknown   = -1,
    M4READER_kMediaFamilyVideo     = 0,
    M4READER_kMediaFamilyAudio     = 1,
    M4READER_kMediaFamilyText      = 2
} M4READER_MediaFamily;



/**
 ************************************************************************
 * enum        M4READER_OptionID
 * @brief    This enum defines the reader options
 * @note    These options can be read from a reader via M4READER_getOption_fct
 ************************************************************************
*/
typedef enum
{
    /**
    Get the duration of the movie (in ms)
    */
    M4READER_kOptionID_Duration = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0),

    /**
    Get the version of the core reader
    */
    M4READER_kOptionID_Version  = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 1),

    /**
    Get the copyright from the media (if present)
    (currently implemented for 3GPP only: copyright get from the cprt atom in the udta if present)
    */
    M4READER_kOptionID_Copyright= M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 2),


    /**
    Set the OSAL file reader functions to the reader (type of value: M4OSA_FileReadPointer*)
    */
    M4READER_kOptionID_SetOsaFileReaderFctsPtr = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                     M4READER_COMMON, 3),

    /**
    Set the OSAL file writer functions to the reader (type of value: M4OSA_FileWriterPointer*)
    */
    M4READER_kOptionID_SetOsaFileWriterFctsPtr = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                     M4READER_COMMON, 4),

    /**
    Set the OSAL file writer functions to the reader (type of value: M4OSA_NetFunction*)
    */
    M4READER_kOptionID_SetOsaNetFctsPtr = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 5),

    /**
    Creation time in sec. since midnight, Jan. 1, 1970 (type of value: M4OSA_UInt32*)
    (available only for 3GPP content, including PGD)
    */
    M4READER_kOptionID_CreationTime = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 6),

    /**
    Bitrate in bps (type of value: M4OSA_Double*)
    */
    M4READER_kOptionID_Bitrate = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 7),

    /**
    Tag ID3v1 of MP3 source (type of value: M4MP3R_ID3Tag*)
    */
    M4READER_kOptionID_Mp3Id3v1Tag = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 8),

    /**
    Tag ID3v2 of MP3 source (type of value: M4MP3R_ID3Tag*)
    */
    M4READER_kOptionID_Mp3Id3v2Tag = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 9),

    /**
    Number of Access Unit in the Audio stream (type of value: M4OSA_UInt32*)
    */
    M4READER_kOptionID_GetNumberOfAudioAu = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0xA),

    /**
    Number of frames per bloc
    */
    M4READER_kOptionID_GetNbframePerBloc    = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                             M4READER_COMMON, 0xB),

    /**
    Flag for protection presence
    */
    M4READER_kOptionID_GetProtectPresence    = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                             M4READER_COMMON, 0xC),

    /**
    Set DRM Context
    */
    M4READER_kOptionID_SetDRMContext    = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0xD),

    /**
    Get ASF Content Description Object
    */
    M4READER_kOptionID_ContentDescription = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0xE),

    /**
    Get ASF Content Description Object
    */
    M4READER_kOptionID_ExtendedContentDescription = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                             M4READER_COMMON, 0xF),

    /**
    Get Asset 3gpp Fields
    */
    M4READER_kOptionID_3gpAssetFields = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x10),

    /**
    Set the max metadata size supported in the reader
    Only relevant in 3gp parser till now, but can be used for other readers
    */
    M4READER_kOptionID_MaxMetadataSize = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4READER_COMMON, 0x11),

    M4READER_kOptionID_GetMetadata = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x12),
    /**
    Get 3gpp 'ftyp' atom
    */
    M4READER_kOptionID_3gpFtypBox  = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x13),


    /* value is M4OSA_Bool* */
    /* return the drm protection status of the file*/
    M4READER_kOptionID_isProtected = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x14),

    /* value is a void* */
    /* return the aggregate rights of the file*/
    /* The buffer must be allocated by the application and must be big enough*/
    /* By default, the size for WMDRM is 76 bytes */
    M4READER_kOptionID_getAggregateRights = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x15),
    /**
    Get ASF Content Description Object
    */
    M4READER_kOptionID_ExtendedContentEncryption = M4OSA_OPTION_ID_CREATE(M4_READ,\
                                                         M4READER_COMMON, 0x16),

    /**
    Number of Access Unit in the Video stream (type of value: M4OSA_UInt32*)
    */
    M4READER_kOptionID_GetNumberOfVideoAu = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x17),

    /**
    Chunk mode activation  size in case of JPG reader */
    M4READER_kOptionID_JpegChunckSize = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x18),

    /**
    Check if ASF file contains video */
    M4READER_kOptionID_hasVideo = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x19),

    /**
     Set specific read mode for Random Access JPEG */
    M4READER_kOptionID_JpegRAMode = M4OSA_OPTION_ID_CREATE(M4_WRITE, M4READER_COMMON, 0x20),

    /**
    Get Thumbnail buffer in case of JPG reader */
    M4READER_kOptionID_JpegThumbnail = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x21),

    /**
    Get FPDATA buffer in case of JPG reader */
    M4READER_kOptionID_JpegFPData = M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x22),

    /**
    Get JPEG info (progressive, subsampling) */
    M4READER_kOptionID_JpegInfo= M4OSA_OPTION_ID_CREATE(M4_READ, M4READER_COMMON, 0x23)


/*****************************************/
} M4READER_OptionID;
/*****************************************/

/**
 ************************************************************************
 * structure    M4READER_CopyRight
 * @brief        This structure defines a copyRight description
 * @note        This structure is used to retrieve the copyRight of the media
 *              (if present) via the getOption() function
 ************************************************************************
*/
typedef struct _M4READER_CopyRight
{
    /**
    Pointer to copyright data (allocated by user)
    */
    M4OSA_UInt8*   m_pCopyRight;

    /**
    Pointer to copyright size. The pCopyRightSize must
    be Initialized with the size available in the pCopyRight buffer
    */
    M4OSA_UInt32   m_uiCopyRightSize;

} M4READER_CopyRight;



/**
 ************************************************************************
 * structure    M4READER_StreamDataOption
 * @brief        This structure defines a generic stream data option
 * @note        It is used is used to set or get a stream specific data defined
 *              by a relevant reader option ID.
 ************************************************************************
*/
typedef struct _M4READER_StreamDataOption
{
    M4_StreamHandler*     m_pStreamHandler; /**< identifier of the stream */
    M4OSA_Void*           m_pOptionValue;   /**< value of the data option to get or to set */

} M4READER_StreamDataOption;

/**
 ************************************************************************
 * enumeration    M4_EncodingFormat
 * @brief        Text encoding format
 ************************************************************************
*/
// typedef enum
// {
//     M4_kEncFormatUnknown    = 0,    /**< Unknown format                                    */
//     M4_kEncFormatASCII        = 1,  /**< ISO-8859-1. Terminated with $00                   */
//     M4_kEncFormatUTF8        = 2,   /**< UTF-8 encoded Unicode . Terminated with $00       */
//     M4_kEncFormatUTF16        = 3   /**< UTF-16 encoded Unicode. Terminated with $00 00    */
/*}  M4_EncodingFormat;*/

/**
 ************************************************************************
 * structure    M4_StringAttributes
 * @brief        This structure defines string attribute
 ************************************************************************
*/
// typedef struct
// {
//     M4OSA_Void*            m_pString;            /**< Pointer to text        */
//     M4OSA_UInt32        m_uiSize;            /**< Size of text            */
//     M4_EncodingFormat    m_EncodingFormat;    /**< Text encoding format    */
// } M4_StringAttributes;


/**
 ************************************************************************
 * structure    M4READER_Buffer
 * @brief        This structure defines a buffer in all readers
 ************************************************************************
*/
typedef struct
{
    M4OSA_UInt8*   m_pData;
    M4OSA_UInt32   m_uiBufferSize;
} M4READER_Buffer;

typedef struct
{
     M4OSA_UInt32            m_uiSessionId;
    M4OSA_UInt32            m_uiMediaId;
    M4OSA_UInt32            m_uiNbInstance;
    M4OSA_Char**            m_pInstance;
} M4_SdpAssetInstance;
/*
typedef enum
{
     M4READER_kUnknownFormat    = 0,
     M4READER_kTagID3V1,
     M4READER_kTagID3V2,
    M4READER_kASFContentDesc,
    M4READER_k3GppAssetBoxFromUDTA,
    M4READER_k3GppAssetBoxFromSDP,
    M4READER_kJpegExif
} M4READER_MetaDataType;*/


/**
 ************************************************************************
 * structure    M4_3gpAssetFields
 * @brief        This structure defines fields of a 3gpp asset information
 ************************************************************************
*/
typedef struct
{
    M4COMMON_MetaDataFields    m_metadata;

    M4OSA_UInt32            m_uiSessionID;    /* For SDP */
    M4OSA_UInt32            m_uiMediaID;    /* For SDP */


    /* Note: The two following fields were added for internal use
        (For Music manager project..) !! */
    M4_StreamType       m_VideoStreamType;    /**< Video stream type */
    M4_StreamType       m_AudioStreamType;    /**< Audio stream type */

} M4_MetaDataFields;


#define M4_METADATA_STR_NB    22 /* one string in album art structure*/

typedef struct
{
    M4OSA_UInt32            m_uiNbBuffer;
    M4_SdpAssetInstance*    m_pAssetInfoInst;    /* Set of 3gpp asset boxes */
    M4COMMON_MetaDataAlbumArt        m_albumArt;            /* RC: PV specific album art:added
                                                               here because this type is used by
                                                               union below in streaming */

} M4READER_netInfos;


typedef union
{
    M4READER_Buffer        m_pTagID3Buffer[2];        /* Tag ID3 V1, V2 */
    struct
    {
        M4READER_Buffer        m_pAsfDescContent;    /* ASF description content buffer */
        M4READER_Buffer        m_pAsfExtDescContent; /* ASF extended description content buffer */
    } m_asf;
    M4_MetaDataFields    m_pMetadataFields;      /* Already parsed and filled 3gpp asset fields */
    M4READER_netInfos    m_pAssetInfoInstance;   /* Set of 3gpp asset boxes in the sdp file */

} M4_MetadataBuffer;




/*********** READER GLOBAL Interface ************************************/

/**
 ************************************************************************
 * @brief    create an instance of the reader
 * @note    create the context
 * @param    pContext:            (OUT)    pointer on a reader context
 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_PARAMETER                at least one parameter is not properly set
 * @return    M4ERR_ALLOC                    a memory allocation has failed
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_create_fct)          (M4OSA_Context* pContext);

/**
 ************************************************************************
 * @brief    destroy the instance of the reader
 * @note    after this call the context is invalid
 * @param    context:            (IN)    Context of the reader
 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_PARAMETER                at least one parameter is not properly set
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_destroy_fct)         (M4OSA_Context context);


/**
 ************************************************************************
 * @brief    open the reader and initializes its created instance
 * @note    this function, for the network reader, sends the DESCRIBE
 * @param    context:            (IN)    Context of the reader
 * @param    pFileDescriptor:    (IN)    Pointer to proprietary data identifying the media to open
 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_PARAMETER                the context is NULL
 * @return    M4ERR_BAD_CONTEXT            provided context is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_open_fct)    (M4OSA_Context context, M4OSA_Void* pFileDescriptor);


/**
 ************************************************************************
 * @brief    close the reader
 * @note
 * @param    context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            the context is NULL
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR   (M4READER_close_fct)    (M4OSA_Context context);



/**
 ************************************************************************
 * @brief    Get the next stream found in the media
 * @note
 * @param    context:        (IN)    Context of the reader
 * @param    pMediaFamily:    (OUT)    pointer to a user allocated M4READER_MediaFamily that will
 *                                     be filled with the media family of the found stream
 * @param    pStreamHandler:    (OUT)    pointer to a stream handler that will be allocated and
 *                                       filled with the found stream description
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4WAR_NO_MORE_STREAM    no more available stream in the media (all streams found)
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_getNextStream_fct)   (M4OSA_Context context,
                                                     M4READER_MediaFamily *pMediaFamily,
                                                     M4_StreamHandler **pStreamHandler);


/**
 ************************************************************************
 * @brief    fill the access unit structure with initialization values
 * @note
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler:    (IN)     pointer to the stream handler to which the access unit
 *                                           will be associated
 * @param    pAccessUnit:    (IN/OUT) pointer to the access unit (allocated by the caller)
 *                                           to initialize
 * @return    M4NO_ERROR                  there is no error
 * @return    M4ERR_BAD_CONTEXT         provided context is not a valid one
 * @return    M4ERR_PARAMETER             at least one parameter is not properly set
 * @return    M4ERR_ALLOC                 there is no more memory available
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_fillAuStruct_fct)    (M4OSA_Context context,
                                                   M4_StreamHandler *pStreamHandler,
                                                   M4_AccessUnit *pAccessUnit);

/**
 ************************************************************************
 * @brief    starts the instance of the reader
 * @note    only needed for network until now...
 * @param    context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            the context is NULL
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_start_fct)   (M4OSA_Context context);

/**
 ************************************************************************
 * @brief    stop reading
 * @note    only needed for network until now... (makes a pause)
 * @param    context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            the context is NULL
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_stop_fct)   (M4OSA_Context context);


/**
 ************************************************************************
 * @brief    get an option value from the reader
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to retrieve a property value:
 *          -the duration of the longest stream of the media
 *          -the version number of the reader
 *
 * @param    context:        (IN)    Context of the reader
 * @param    optionId:        (IN)    indicates the option to get
 * @param    pValue:            (OUT)    pointer to structure or value (allocated by user)
 *                                          where option is stored
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_getOption_fct)       (M4OSA_Context context, M4OSA_OptionID optionId,
                                                     M4OSA_DataOption pValue);


/**
 ************************************************************************
 * @brief   set en option value of the readder
 * @note    this function follows the set/get option mechanism described in OSAL 3.0
 *          it allows the caller to set a property value:
 *          - nothing for the moment
 *
 * @param    context:        (IN)    Context of the reader
 * @param    optionId:        (IN)    indicates the option to set
 * @param    pValue:            (IN)    pointer to structure or value (allocated by user) where
 *                                          option is stored
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID        when the option ID is not a valid one
 ************************************************************************
*/
typedef M4OSA_ERR (M4READER_setOption_fct)       (M4OSA_Context context, M4OSA_OptionID optionId,
                                                     M4OSA_DataOption pValue);


/**
 ************************************************************************
 * @brief    jump into the stream at the specified time
 * @note
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler    (IN)     the stream handler of the stream to make jump
 * @param    pTime            (IN/OUT) IN:  the time to jump to (in ms)
 *                                     OUT: the time to which the stream really jumped
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_ALLOC                there is no more memory available
 * @return    M4ERR_BAD_STREAM_ID        the streamID does not exist
 ************************************************************************
*/
typedef M4OSA_ERR   (M4READER_jump_fct)     (M4OSA_Context context,
                                                M4_StreamHandler *pStreamHandler,
                                                M4OSA_Int32* pTime);


/**
 ************************************************************************
 * @brief    reset the stream, that is seek it to beginning and make it ready to be read
 * @note
 * @param    context:        (IN)    Context of the reader
 * @param    pStreamHandler    (IN)    The stream handler of the stream to reset
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_ALLOC                there is no more memory available
 * @return    M4ERR_BAD_STREAM_ID        the streamID does not exist
 ************************************************************************
*/
typedef M4OSA_ERR   (M4READER_reset_fct)    (M4OSA_Context context,
                                                M4_StreamHandler *pStreamHandler);


/**
 ************************************************************************
 * @brief    get the time of the closest RAP access unit before the given time
 * @note
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler    (IN)     the stream handler of the stream to search
 * @param    pTime            (IN/OUT) IN:  the time to search from (in ms)
 *                                     OUT: the time (cts) of the preceding RAP AU.
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_BAD_CONTEXT        provided context is not a valid one
 * @return    M4ERR_PARAMETER            at least one parameter is not properly set
 * @return    M4ERR_BAD_STREAM_ID        the streamID does not exist
 ************************************************************************
*/
typedef M4OSA_ERR   (M4READER_getPrevRapTime_fct) (M4OSA_Context context,
                                                    M4_StreamHandler *pStreamHandler,
                                                    M4OSA_Int32* pTime);


/**
 ************************************************************************
 * structure    M4READER_GlobalInterface
 * @brief        This structure defines the generic media reader GLOBAL interface
 * @note        This structure stores the pointers to functions concerning
 *                creation and control of one reader type.
 *                The reader type is one of the M4READER_MediaType
 ************************************************************************
*/
typedef struct _M4READER_GlobalInterface
/*****************************************/
{
    M4READER_create_fct*            m_pFctCreate;
    M4READER_destroy_fct*           m_pFctDestroy;
    M4READER_open_fct*              m_pFctOpen;
    M4READER_close_fct*             m_pFctClose;
    M4READER_getOption_fct*         m_pFctGetOption;
    M4READER_setOption_fct*         m_pFctSetOption;
    M4READER_getNextStream_fct*     m_pFctGetNextStream;
    M4READER_fillAuStruct_fct*      m_pFctFillAuStruct;
    M4READER_start_fct*             m_pFctStart;
    M4READER_stop_fct*              m_pFctStop;
    M4READER_jump_fct*              m_pFctJump;
    M4READER_reset_fct*             m_pFctReset;
    M4READER_getPrevRapTime_fct*    m_pFctGetPrevRapTime;

} M4READER_GlobalInterface;


/************* READER DATA Interface ************************************/



/**
 ************************************************************************
 * @brief    Gets an access unit (AU) from the stream handler source.
 * @note    An AU is the smallest possible amount of data to be decoded by a decoder (audio/video).
 *
 * @param    context:        (IN)        Context of the reader
 * @param    pStreamHandler    (IN)        The stream handler of the stream to make jump
 * @param    pAccessUnit        (IN/OUT)   Pointer to an access unit to fill with read data
 *                                         (the au structure is allocated by the user, and must be
 *                                         initialized by calling M4READER_fillAuStruct_fct after
 *                                         creation)
 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_BAD_CONTEXT            provided context is not a valid one
 * @return    M4ERR_PARAMETER                at least one parameter is not properly set
 * @returns    M4ERR_ALLOC                    memory allocation failed
 * @returns    M4ERR_BAD_STREAM_ID            at least one of the stream Id. does not exist.
 * @returns    M4WAR_NO_DATA_YET            there is no enough data on the stream for new
 *                                          access unit
 * @returns    M4WAR_NO_MORE_AU            there are no more access unit in the stream
 *                                          (end of stream)
 ************************************************************************
*/
typedef M4OSA_ERR   (M4READER_getNextAu_fct)(M4OSA_Context context,
                                             M4_StreamHandler *pStreamHandler,
                                             M4_AccessUnit *pAccessUnit);


/**
 ************************************************************************
 * structure    M4READER_DataInterface
 * @brief        This structure defines the generic media reader DATA interface
 * @note        This structure stores the pointers to functions concerning
 *                data access for one reader type.(those functions are typically called from
 *                a decoder) The reader type is one of the M4READER_MediaType
 ************************************************************************
*/
typedef struct _M4READER_DataInterface
{
    M4READER_getNextAu_fct*   m_pFctGetNextAu;

    /**
    stores the context created by the M4READER_create_fct() function
    so it is accessible without  decoder
    */
    M4OSA_Context m_readerContext;
/*****************************************/
} M4READER_DataInterface;
/*****************************************/


#endif /*__M4READER_COMMON_H__*/

