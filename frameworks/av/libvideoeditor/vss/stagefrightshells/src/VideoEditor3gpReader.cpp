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
*************************************************************************
* @file   VideoEditor3gpReader.cpp
* @brief  StageFright shell 3GP Reader
*************************************************************************
*/

#define LOG_NDEBUG 1
#define LOG_TAG "VIDEOEDITOR_3GPREADER"

/**
 * HEADERS
 *
 */
#define VIDEOEDITOR_BITSTREAM_PARSER

#include "M4OSA_Debug.h"
#include "VideoEditor3gpReader.h"
#include "M4SYS_AccessUnit.h"
#include "VideoEditorUtils.h"
#include "M4READER_3gpCom.h"
#include "M4_Common.h"
#include "M4OSA_FileWriter.h"

#ifdef VIDEOEDITOR_BITSTREAM_PARSER
#include "M4OSA_CoreID.h"
#include "M4OSA_Error.h"
#include "M4OSA_Memory.h"
#include "M4_Utils.h"
#endif

#include "ESDS.h"
#include "utils/Log.h"
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaExtractor.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

/**
 * SOURCE CLASS
 */
namespace android {
/**
 * ENGINE INTERFACE
 */

/**
 ************************************************************************
 * @brief   Array of AMR NB/WB bitrates
 * @note    Array to match the mode and the bit rate
 ************************************************************************
*/
const M4OSA_UInt32 VideoEditor3gpReader_AmrBitRate [2 /* 8kHz / 16kHz     */]
                                                   [9 /* the bitrate mode */] =
{
    {4750, 5150, 5900,  6700,  7400,  7950,  10200, 12200, 0},
    {6600, 8850, 12650, 14250, 15850, 18250, 19850, 23050, 23850}
};

/**
 *******************************************************************************
 * structure VideoEditor3gpReader_Context
 * @brief:This structure defines the context of the StageFright 3GP shell Reader
 *******************************************************************************
*/
typedef struct {
    sp<DataSource>              mDataSource;
    sp<MediaExtractor>          mExtractor;
    sp<MediaSource>             mAudioSource;
    sp<MediaSource>             mVideoSource;
    M4_StreamHandler*           mAudioStreamHandler;
    M4_StreamHandler*           mVideoStreamHandler;
    M4SYS_AccessUnit            mAudioAu;
    M4SYS_AccessUnit            mVideoAu;
    M4OSA_Time                  mMaxDuration;
    int64_t                     mFileSize;
    M4_StreamType               mStreamType;
    M4OSA_UInt32                mStreamId;
    int32_t                     mTracks;
    int32_t                     mCurrTrack;
    M4OSA_Bool                  mAudioSeeking;
    M4OSA_Time                  mAudioSeekTime;
    M4OSA_Bool                  mVideoSeeking;
    M4OSA_Time                  mVideoSeekTime;

} VideoEditor3gpReader_Context;

#ifdef VIDEOEDITOR_BITSTREAM_PARSER
/**
 ************************************************************************
 * structure    VideoEditor3gpReader_BitStreamParserContext
 * @brief       Internal BitStreamParser context
 ************************************************************************
*/
typedef struct {
    M4OSA_UInt32*   mPbitStream;   /**< bitstream pointer (32bits aligned) */
    M4OSA_Int32     mSize;         /**< bitstream size in bytes */
    M4OSA_Int32     mIndex;        /**< byte index */
    M4OSA_Int32     mBitIndex;     /**< bit index */
    M4OSA_Int32     mStructSize;   /**< size of structure */
} VideoEditor3gpReader_BitStreamParserContext;

/**
 *******************************************************************************
 * @brief   Allocates the context and initializes internal data.
 * @param   pContext    (OUT)  Pointer to the BitStreamParser context to create.
 * @param   bitStream   A pointer to the bitstream
 * @param   size        The size of the bitstream in bytes
 *******************************************************************************
*/
static void VideoEditor3gpReader_BitStreamParserInit(void** pContext,
        void* pBitStream, M4OSA_Int32 size) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext;

    *pContext=M4OSA_NULL;
    pStreamContext = (VideoEditor3gpReader_BitStreamParserContext*)M4OSA_32bitAlignedMalloc(
        sizeof(VideoEditor3gpReader_BitStreamParserContext), M4READER_3GP,
            (M4OSA_Char*)"3GP BitStreamParser Context");
    if (M4OSA_NULL == pStreamContext) {
        return;
    }
    pStreamContext->mPbitStream=(M4OSA_UInt32*)pBitStream;
    pStreamContext->mSize=size;
    pStreamContext->mIndex=0;
    pStreamContext->mBitIndex=0;
    pStreamContext->mStructSize =
        sizeof(VideoEditor3gpReader_BitStreamParserContext);

    *pContext=pStreamContext;
}
/**
 **********************************************************************
 * @brief   Clean up context
 * @param   pContext    (IN/OUT)  BitStreamParser context.
 **********************************************************************
*/
static void VideoEditor3gpReader_BitStreamParserCleanUp(void* pContext) {
    free((M4OSA_Int32*)pContext);
}
/**
 *****************************************************************************
 * @brief   Read the next <length> bits in the bitstream.
 * @note    The function does not update the bitstream pointer.
 * @param   pContext    (IN/OUT) BitStreamParser context.
 * @param   length      (IN) The number of bits to extract from the bitstream
 * @return  the read bits
 *****************************************************************************
*/
static M4OSA_UInt32 VideoEditor3gpReader_BitStreamParserShowBits(void* pContext,
        M4OSA_Int32 length) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext =
        (VideoEditor3gpReader_BitStreamParserContext*)pContext;

    M4OSA_UInt32 u_mask;
    M4OSA_UInt32 retval;
    M4OSA_Int32 i_ovf;

    M4OSA_DEBUG_IF1((M4OSA_NULL==pStreamContext), 0,
        "VideoEditor3gpReader_BitStreamParserShowBits:invalid context pointer");

    retval=(M4OSA_UInt32)GET_MEMORY32(pStreamContext->\
        mPbitStream[ pStreamContext->mIndex ]);
    i_ovf = pStreamContext->mBitIndex + length - 32;
    u_mask = (length >= 32) ? 0xffffffff: (1 << length) - 1;

    /* do we have enough bits availble in the current word(32bits)*/
    if (i_ovf <= 0) {
        retval=(retval >> (- i_ovf)) & u_mask;
    } else {
        M4OSA_UInt32 u_nextword = (M4OSA_UInt32)GET_MEMORY32(
            pStreamContext->mPbitStream[ pStreamContext->mIndex + 1 ]);
        M4OSA_UInt32 u_msb_mask, u_msb_value, u_lsb_mask, u_lsb_value;

        u_msb_mask = ((1 << (32 - pStreamContext->mBitIndex)) - 1) << i_ovf;
        u_msb_value = retval << i_ovf;
        u_lsb_mask = (1 << i_ovf) - 1;
        u_lsb_value = u_nextword >> (32 - i_ovf);
        retval= (u_msb_value & u_msb_mask ) | (u_lsb_value & u_lsb_mask);
    }
    /* return the bits...*/
    return retval;
}
/**
 ************************************************************************
 * @brief   Increment the bitstream pointer of <length> bits.
 * @param   pContext    (IN/OUT) BitStreamParser context.
 * @param   length      (IN) The number of bit to shift the bitstream
 ************************************************************************
*/
static void VideoEditor3gpReader_BitStreamParserFlushBits(void* pContext,
        M4OSA_Int32 length) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext=(
        VideoEditor3gpReader_BitStreamParserContext*)pContext;
    M4OSA_Int32 val;

    if (M4OSA_NULL == pStreamContext) {
        return;
    }
    val=pStreamContext->mBitIndex + length;
    /* update the bits...*/
    pStreamContext->mBitIndex += length;

    if (val - 32 >= 0) {
        /* update the bits...*/
        pStreamContext->mBitIndex -= 32;
        /* update the words*/
        pStreamContext->mIndex++;
    }
}

static M4OSA_UInt32 VideoEditor3gpReader_BitStreamParserGetBits(
        void* pContext,M4OSA_Int32 bitPos, M4OSA_Int32 bitLength) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext =
        (VideoEditor3gpReader_BitStreamParserContext*)pContext;

    M4OSA_Int32 bitLocation, bitIndex;
    M4OSA_UInt32 retval=0;

    M4OSA_DEBUG_IF1((M4OSA_NULL==pStreamContext), 0,
        "VideoEditor3gpReader_BitStreamParserGetBits: invalid context pointer");

    /* computes the word location*/
    bitLocation=bitPos/32;
    bitIndex=(bitPos) % 32;

    if (bitLocation < pStreamContext->mSize) {
        M4OSA_UInt32 u_mask;
        M4OSA_Int32 i_ovf = bitIndex + bitLength - 32;
        retval=(M4OSA_UInt32)GET_MEMORY32(
            pStreamContext->mPbitStream[ bitLocation ]);

        u_mask = (bitLength >= 32) ? 0xffffffff: (1 << bitLength) - 1;

        if (i_ovf <= 0) {
            retval=(retval >> (- i_ovf)) & u_mask;
        } else {
            M4OSA_UInt32 u_nextword = (M4OSA_UInt32)GET_MEMORY32(
                pStreamContext->mPbitStream[ bitLocation + 1 ]);
            M4OSA_UInt32 u_msb_mask, u_msb_value, u_lsb_mask, u_lsb_value;

            u_msb_mask = ((1 << (32 - bitIndex)) - 1) << i_ovf;
            u_msb_value = retval << i_ovf;
            u_lsb_mask = (1 << i_ovf) - 1;
            u_lsb_value = u_nextword >> (32 - i_ovf);
            retval= (u_msb_value & u_msb_mask ) | (u_lsb_value & u_lsb_mask);
        }
    }
    return retval;
}

static void VideoEditor3gpReader_BitStreamParserRestart(void* pContext) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext =
        (VideoEditor3gpReader_BitStreamParserContext*)pContext;

    if (M4OSA_NULL == pStreamContext) {
        return;
    }
    /* resets the bitstream pointers*/
    pStreamContext->mIndex=0;
    pStreamContext->mBitIndex=0;
}
/**
 *******************************************************************************
 * @brief  Get a pointer to the current byte pointed by the bitstream pointer.
 * @note   It should be used carefully as the pointer is in the bitstream itself
 *         and no copy is made.
 * @param  pContext    (IN/OUT)  BitStreamParser context.
 * @return Pointer to the current location in the bitstream
 *******************************************************************************
*/
static M4OSA_UInt8*  VideoEditor3gpReader_GetCurrentbitStreamPointer(
        void* pContext) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext =
        (VideoEditor3gpReader_BitStreamParserContext*)pContext;
    M4OSA_DEBUG_IF1((M4OSA_NULL==pStreamContext), 0, "invalid context pointer");

    return (M4OSA_UInt8*)((M4OSA_UInt8*)pStreamContext->mPbitStream + \
        pStreamContext->mIndex * sizeof(M4OSA_UInt32) + \
        pStreamContext->mBitIndex/8) ;
}

static M4OSA_Int32 VideoEditor3gpReader_BitStreamParserGetSize(void* pContext) {
    VideoEditor3gpReader_BitStreamParserContext* pStreamContext =
        (VideoEditor3gpReader_BitStreamParserContext*)pContext;
    M4OSA_DEBUG_IF1((M4OSA_NULL==pStreamContext), 0, "invalid context pointer");

    return pStreamContext->mSize;
}


static void VideoEditor3gpReader_MPEG4BitStreamParserInit(void** pContext,
        void* pBitStream, M4OSA_Int32 size) {
    VideoEditor3gpReader_BitStreamParserInit(pContext, pBitStream, size);
}
static M4OSA_Int32 VideoEditor3gpReader_GetMpegLengthFromInteger(void* pContext,
        M4OSA_UInt32 val) {
    M4OSA_UInt32 length=0;
    M4OSA_UInt32 numBytes=0;
    M4OSA_UInt32 b=0;

    M4OSA_DEBUG_IF1((M4OSA_NULL==pContext), 0, "invalid context pointer");

    /* the length is encoded as a sequence of bytes. The highest bit is used
    to indicate that the length continues on the next byte.

    The length can be: 0x80 0x80 0x80 0x22
    of just            0x22 (highest bit not set)

    */

    do {
        b=(val & ((0xff)<< (8 * numBytes)))>> (8 * numBytes);
        length=(length << 7) | (b & 0x7f);
        numBytes++;
    } while ((b & 0x80) && numBytes < 4);

    return length;
}

/**
 *******************************************************************************
 * @brief  Decode an MPEG4 Systems descriptor size from an encoded SDL size data
 * @note   The value is read from the current bitstream location.
 * @param  pContext    (IN/OUT)  BitStreamParser context.
 * @return Size in a human readable form
 *******************************************************************************
*/
static M4OSA_Int32 VideoEditor3gpReader_GetMpegLengthFromStream(void* pContext){
    M4OSA_UInt32 length=0;
    M4OSA_UInt32 numBytes=0;
    M4OSA_UInt32 b=0;

    M4OSA_DEBUG_IF1((M4OSA_NULL==pContext), 0, "invalid context pointer");

    /* the length is encoded as a sequence of bytes. The highest bit is used
    to indicate that the length continues on the next byte.

    The length can be: 0x80 0x80 0x80 0x22
    of just            0x22 (highest bit not set)
    */

    do {
        b=VideoEditor3gpReader_BitStreamParserShowBits(pContext, 8);
        VideoEditor3gpReader_BitStreamParserFlushBits(pContext, 8);
        length=(length << 7) | (b & 0x7f);
        numBytes++;
    } while ((b & 0x80) && numBytes < 4);

    return length;
}
#endif /* VIDEOEDITOR_BITSTREAM_PARSER */
/**
************************************************************************
* @brief    create an instance of the 3gp reader
 * @note    allocates the context
 *
 * @param   pContext:       (OUT)   pointer on a reader context
 *
 * @return  M4NO_ERROR              there is no error
 * @return  M4ERR_ALLOC             a memory allocation has failed
 * @return  M4ERR_PARAMETER         at least one parameter is not valid
************************************************************************
*/

M4OSA_ERR VideoEditor3gpReader_create(M4OSA_Context *pContext) {
    VideoEditor3gpReader_Context* pC = NULL;
    M4OSA_ERR err = M4NO_ERROR;
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext , M4ERR_PARAMETER);

    ALOGV("VideoEditor3gpReader_create begin");

    /* Context allocation & initialization */
    SAFE_MALLOC(pC, VideoEditor3gpReader_Context, 1, "VideoEditor3gpReader");

    memset(pC, sizeof(VideoEditor3gpReader_Context), 0);

    pC->mAudioStreamHandler  = M4OSA_NULL;
    pC->mAudioAu.dataAddress = M4OSA_NULL;
    pC->mVideoStreamHandler  = M4OSA_NULL;
    pC->mVideoAu.dataAddress = M4OSA_NULL;

    pC->mAudioSeeking = M4OSA_FALSE;
    pC->mAudioSeekTime = 0;

    pC->mVideoSeeking = M4OSA_FALSE;
    pC->mVideoSeekTime = 0;

    pC->mMaxDuration = 0;

    *pContext=pC;

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditor3gpReader_create no error");
    } else {
        ALOGV("VideoEditor3gpReader_create ERROR 0x%X", err);
    }
    ALOGV("VideoEditor3gpReader_create end ");
    return err;
}

/**
**************************************************************************
* @brief    destroy the instance of the 3gp reader
* @note after this call the context is invalid
* @param    context:        (IN)    Context of the reader
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_PARAMETER         pContext parameter is not properly set
**************************************************************************
*/

M4OSA_ERR VideoEditor3gpReader_destroy(M4OSA_Context pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditor3gpReader_Context* pC = M4OSA_NULL;

    ALOGV("VideoEditor3gpReader_destroy begin");

    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    pC = (VideoEditor3gpReader_Context*)pContext;

    SAFE_FREE(pC->mAudioAu.dataAddress);
    pC->mAudioAu.dataAddress = M4OSA_NULL;
    SAFE_FREE(pC->mVideoAu.dataAddress);
    pC->mVideoAu.dataAddress = M4OSA_NULL;
    SAFE_FREE(pC);
    pContext = M4OSA_NULL;

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditor3gpReader_destroy no error");
    }
    else
    {
        ALOGV("VideoEditor3gpReader_destroy ERROR 0x%X", err);
    }

    ALOGV("VideoEditor3gpReader_destroy end ");
    return err;
}

/**
************************************************************************
* @brief    open the reader and initializes its created instance
* @note     this function open the media file
* @param    context:            (IN)    Context of the reader
* @param    pFileDescriptor:    (IN)    Pointer to proprietary data identifying
*                                       the media to open
* @return   M4NO_ERROR                  there is no error
* @return   M4ERR_PARAMETER             the context is NULL
* @return   M4ERR_UNSUPPORTED_MEDIA_TYPE
*                                       the media is DRM protected
************************************************************************
*/

M4OSA_ERR VideoEditor3gpReader_open(M4OSA_Context pContext,
        M4OSA_Void* pFileDescriptor) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)pContext;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditor3gpReader_open start ");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC),  M4ERR_PARAMETER,
        "VideoEditor3gpReader_open: invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pFileDescriptor), M4ERR_PARAMETER,
        "VideoEditor3gpReader_open: invalid pointer pFileDescriptor");

    ALOGV("VideoEditor3gpReader_open Datasource start %s",
        (char*)pFileDescriptor);
    //pC->mDataSource = DataSource::CreateFromURI((char*)pFileDescriptor);
    pC->mDataSource = new FileSource ((char*)pFileDescriptor);

    if (pC->mDataSource == NULL) {
        ALOGV("VideoEditor3gpReader_open Datasource error");
        return M4ERR_PARAMETER;
    }

    pC->mExtractor = MediaExtractor::Create(pC->mDataSource,
        MEDIA_MIMETYPE_CONTAINER_MPEG4);

    if (pC->mExtractor == NULL) {
        ALOGV("VideoEditor3gpReader_open extractor error");
        return M4ERR_PARAMETER;
    }

    int32_t isDRMProtected = 0;
    sp<MetaData> meta = pC->mExtractor->getMetaData();
    meta->findInt32(kKeyIsDRM, &isDRMProtected);
    if (isDRMProtected) {
        ALOGV("VideoEditorMp3Reader_open error - DRM Protected");
        return M4ERR_UNSUPPORTED_MEDIA_TYPE;
    }

    ALOGV("VideoEditor3gpReader_open end ");
    return err;
}

/**
************************************************************************
* @brief    close the reader
* @note     close the 3GP file
* @param    context:        (IN)    Context of the reader
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_PARAMETER         the context is NULL
* @return   M4ERR_BAD_CONTEXT       provided context is not a valid one
************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_close(M4OSA_Context context) {
    VideoEditor3gpReader_Context *pC = (VideoEditor3gpReader_Context*)context;
    M4READER_AudioSbrUserdata *pAudioSbrUserData;
    M4_AccessUnit *pAU;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditor3gpReader_close begin");

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "VideoEditor3gpReader_close: invalid context pointer");

    if (pC->mAudioStreamHandler) {
        ALOGV("VideoEditor3gpReader_close Audio");

        if (M4OSA_NULL != pC->mAudioStreamHandler->m_pDecoderSpecificInfo) {
            free(pC->mAudioStreamHandler->\
                m_pDecoderSpecificInfo);
            pC->mAudioStreamHandler->m_decoderSpecificInfoSize = 0;
            pC->mAudioStreamHandler->m_pDecoderSpecificInfo = M4OSA_NULL;
        }

        if ((M4DA_StreamTypeAudioAac == pC->mAudioStreamHandler->m_streamType)
            && (M4OSA_NULL != pC->mAudioStreamHandler->m_pUserData)) {
            pAudioSbrUserData = (M4READER_AudioSbrUserdata*)(\
                pC->mAudioStreamHandler->m_pUserData);

            pAU = (M4_AccessUnit*)pAudioSbrUserData->m_pFirstAU;
            if (M4OSA_NULL != pAU) {
                free(pAU);
            }

            if (M4OSA_NULL != pAudioSbrUserData->m_pAacDecoderUserConfig) {
                free(pAudioSbrUserData->\
                    m_pAacDecoderUserConfig);
            }
            free(pAudioSbrUserData);
            pC->mAudioStreamHandler->m_pUserData = M4OSA_NULL;
        }

        if (pC->mAudioStreamHandler->m_pESDSInfo != M4OSA_NULL) {
            free(pC->mAudioStreamHandler->m_pESDSInfo);
            pC->mAudioStreamHandler->m_pESDSInfo = M4OSA_NULL;
            pC->mAudioStreamHandler->m_ESDSInfoSize = 0;
        }
        /* Finally destroy the stream handler */
        free(pC->mAudioStreamHandler);
        pC->mAudioStreamHandler = M4OSA_NULL;

        pC->mAudioSource->stop();
        pC->mAudioSource.clear();
    }
    if (pC->mVideoStreamHandler) {
        ALOGV("VideoEditor3gpReader_close Video ");

        if(M4OSA_NULL != pC->mVideoStreamHandler->m_pDecoderSpecificInfo) {
            free(pC->mVideoStreamHandler->\
                m_pDecoderSpecificInfo);
            pC->mVideoStreamHandler->m_decoderSpecificInfoSize = 0;
            pC->mVideoStreamHandler->m_pDecoderSpecificInfo = M4OSA_NULL;
        }

        if(M4OSA_NULL != pC->mVideoStreamHandler->m_pH264DecoderSpecificInfo) {
            free(pC->mVideoStreamHandler->\
                m_pH264DecoderSpecificInfo);
            pC->mVideoStreamHandler->m_H264decoderSpecificInfoSize = 0;
            pC->mVideoStreamHandler->m_pH264DecoderSpecificInfo = M4OSA_NULL;
        }

        if(pC->mVideoStreamHandler->m_pESDSInfo != M4OSA_NULL) {
            free(pC->mVideoStreamHandler->m_pESDSInfo);
            pC->mVideoStreamHandler->m_pESDSInfo = M4OSA_NULL;
            pC->mVideoStreamHandler->m_ESDSInfoSize = 0;
        }

        /* Finally destroy the stream handler */
        free(pC->mVideoStreamHandler);
        pC->mVideoStreamHandler = M4OSA_NULL;

        pC->mVideoSource->stop();
        pC->mVideoSource.clear();
    }
    pC->mExtractor.clear();
    pC->mDataSource.clear();

    ALOGV("VideoEditor3gpReader_close end");
    return err;
}

/**
************************************************************************
* @brief    get an option from the 3gp reader
* @note     it allows the caller to retrieve a property value:
*
* @param    context:        (IN)    Context of the reader
* @param    optionId:       (IN)    indicates the option to get
* @param    pValue:         (OUT)   pointer to structure or value (allocated
*                                   by user) where option is stored
*
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_BAD_CONTEXT       provided context is not a valid one
* @return   M4ERR_PARAMETER         at least one parameter is not properly set
* @return   M4ERR_BAD_OPTION_ID     when the option ID is not a valid one
* @return   M4ERR_VIDEO_NOT_H263    No video stream H263 in file.
* @return   M4ERR_NO_VIDEO_STREAM_RETRIEVED_YET
*           Function 3gpReader_getNextStreamHandler must be called before
************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_getOption(M4OSA_Context context,
        M4OSA_OptionID optionId, M4OSA_DataOption pValue) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditor3gpReader_getOption begin %d", optionId);

    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getOption: invalid pointer on value");

    switch (optionId) {
    case M4READER_kOptionID_Duration:
        {
            ALOGV("VideoEditor3gpReader_getOption duration %d",pC->mMaxDuration);
            *(M4OSA_Time*)pValue = pC->mMaxDuration;
        }
        break;
    case M4READER_kOptionID_Version:
        /* not used */
        ALOGV("VideoEditor3gpReader_getOption: M4READER_kOptionID_Version");
        break;

    case M4READER_kOptionID_Copyright:
        /* not used */
        ALOGV(">>>>>>>   M4READER_kOptionID_Copyright");
        break;

    case M4READER_kOptionID_CreationTime:
        /* not used */
        ALOGV("VideoEditor3gpReader_getOption M4READER_kOptionID_CreationTime");
    break;

    case M4READER_kOptionID_Bitrate:
        {
            M4OSA_UInt32* pBitrate = (M4OSA_UInt32*)pValue;

            if (pC->mMaxDuration != 0) {
                M4OSA_UInt32 ui32Tmp = (M4OSA_UInt32)pC->mMaxDuration;
                *pBitrate = (M4OSA_UInt32)(pC->mFileSize * 8000.0 / pC->mMaxDuration);
            }
            ALOGV("VideoEditor3gpReader_getOption bitrate %ld", *pBitrate);
        }
    break;
    case M4READER_3GP_kOptionID_H263Properties:
        {
            if(M4OSA_NULL == pC->mVideoStreamHandler) {
                ALOGV("VideoEditor3gpReader_getOption no videoStream retrieved");

                err = M4ERR_NO_VIDEO_STREAM_RETRIEVED_YET;
                break;
            }
            if((M4DA_StreamTypeVideoH263 != pC->mVideoStreamHandler->\
                m_streamType) || (pC->mVideoStreamHandler->\
                m_decoderSpecificInfoSize < 7)) {
                ALOGV("VideoEditor3gpReader_getOption DSI Size %d",
                    pC->mVideoStreamHandler->m_decoderSpecificInfoSize);

                err = M4ERR_VIDEO_NOT_H263;
                break;
            }

            /* MAGICAL in the decoder confi H263: the 7th byte is the profile
             * number, 6th byte is the level number */
            ((M4READER_3GP_H263Properties *)pValue)->uiProfile =
                pC->mVideoStreamHandler->m_pDecoderSpecificInfo[6];
            ((M4READER_3GP_H263Properties *)pValue)->uiLevel =
                pC->mVideoStreamHandler->m_pDecoderSpecificInfo[5];
            ALOGV("VideoEditor3gpReader_getOption M4READER_3GP_kOptionID_\
            H263Properties end");
        }
        break;
    case M4READER_3GP_kOptionID_PurpleLabsDrm:
        ALOGV("VideoEditor3gpReaderOption M4READER_3GP_kOptionID_PurpleLabsDrm");
        /* not used */
        break;

    case M4READER_kOptionID_GetNumberOfAudioAu:
        /* not used */
        ALOGV("VideoEditor3gpReadeOption M4READER_kOptionID_GetNumberOfAudioAu");
    break;

    case M4READER_kOptionID_GetNumberOfVideoAu:
        /* not used */
        ALOGV("VideoEditor3gpReader_getOption :GetNumberOfVideoAu");
    break;

    case M4READER_kOptionID_GetMetadata:
        /* not used */
        ALOGV("VideoEditor3gpReader_getOption M4READER_kOptionID_GetMetadata");
    break;

    case M4READER_kOptionID_3gpFtypBox:
        /* used only for SEMC */
        ALOGV("VideoEditor3gpReader_getOption M4READER_kOptionID_3gpFtypBox");
        err = M4ERR_BAD_OPTION_ID; //check this
        break;

#ifdef OPTIONID_GET_NEXT_VIDEO_CTS
    case M4READER_3GP_kOptionID_getNextVideoCTS:
        /* not used */
        ALOGV("VideoEditor3gpReader_getOption: getNextVideoCTS");
        break;
#endif
    default:
        {
            err = M4ERR_BAD_OPTION_ID;
            ALOGV("VideoEditor3gpReader_getOption M4ERR_BAD_OPTION_ID");
        }
        break;
    }
    ALOGV("VideoEditor3gpReader_getOption end: optionID: x%x", optionId);
    return err;
}
/**
************************************************************************
* @brief    set an option on the 3gp reader
* @note No option can be set yet.
* @param    context:        (IN)    Context of the reader
* @param    optionId:       (IN)    indicates the option to set
* @param    pValue:         (IN)    pointer to structure or value (allocated
*                                   by user) where option is stored
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_BAD_CONTEXT       provided context is not a valid one
* @return   M4ERR_PARAMETER         at least one parameter is not properly set
* @return   M4ERR_BAD_OPTION_ID     when the option ID is not a valid one
************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_setOption(M4OSA_Context context,
        M4OSA_OptionID optionId, M4OSA_DataOption pValue) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pC), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER,
        "invalid value pointer");

    ALOGV("VideoEditor3gpReader_setOption begin %d",optionId);

    switch(optionId) {
        case M4READER_kOptionID_SetOsaFileReaderFctsPtr:
        break;

        case M4READER_3GP_kOptionID_AudioOnly:
        break;

        case M4READER_3GP_kOptionID_VideoOnly:
        break;

        case M4READER_3GP_kOptionID_FastOpenMode:
        break;

        case M4READER_kOptionID_MaxMetadataSize:
        break;

        default:
        {
            ALOGV("VideoEditor3gpReader_setOption: returns M4ERR_BAD_OPTION_ID");
            err = M4ERR_BAD_OPTION_ID;
        }
        break;
    }
    ALOGV("VideoEditor3gpReader_setOption end ");
    return err;
}
/**
 ************************************************************************
 * @brief   fill the access unit structure with initialization values
 * @param   context:        (IN)     Context of the reader
 * @param   pStreamHandler: (IN)     pointer to the stream handler to which
 *                                   the access unit will be associated
 * @param   pAccessUnit:    (IN/OUT) pointer to the access unit (allocated
 *                                   by the caller) to initialize
 * @return  M4NO_ERROR               there is no error
 * @return  M4ERR_PARAMETER          at least one parameter is not properly set
 ************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_fillAuStruct(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4_AccessUnit *pAccessUnit) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err= M4NO_ERROR;

    M4OSA_DEBUG_IF1((pC == 0),             M4ERR_PARAMETER,
        "VideoEditor3gpReader_fillAuStruct: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_fillAuStruc invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
        "VideoEditor3gpReader_fillAuStruct: invalid pointer to M4_AccessUnit");

    ALOGV("VideoEditor3gpReader_fillAuStruct begin");

    /* Initialize pAccessUnit structure */
    pAccessUnit->m_size         = 0;
    pAccessUnit->m_CTS          = 0;
    pAccessUnit->m_DTS          = 0;
    pAccessUnit->m_attribute    = 0;
    pAccessUnit->m_dataAddress  = M4OSA_NULL;
    pAccessUnit->m_maxsize      = pStreamHandler->m_maxAUSize;
    pAccessUnit->m_streamID     = pStreamHandler->m_streamId;
    pAccessUnit->m_structSize   = sizeof(M4_AccessUnit);

    ALOGV("VideoEditor3gpReader_fillAuStruct end");
    return M4NO_ERROR;
}

/**
********************************************************************************
* @brief    jump into the stream at the specified time
* @note
* @param    context:        (IN)   Context of the reader
* @param    pStreamHandler  (IN)   the stream handler of the stream to make jump
* @param    pTime           (I/O)IN  the time to jump to (in ms)
*                                OUT the time to which the stream really jumped
* @return   M4NO_ERROR             there is no error
* @return   M4ERR_PARAMETER        at least one parameter is not properly set
********************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_jump(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4OSA_Int32* pTime) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_AccessUnit* pAu;
    M4OSA_Time time64;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_jump: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_jump: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pTime == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_jump: invalid time pointer");

    ALOGV("VideoEditor3gpReader_jump begin");

    if (*pTime == (pStreamHandler->m_duration)) {
        *pTime -= 1;
    }
    time64 = (M4OSA_Time)*pTime;

    ALOGV("VideoEditor3gpReader_jump time us %ld ", time64);

    if ((pC->mAudioStreamHandler != M4OSA_NULL) &&
            (pStreamHandler->m_streamId == pC->mAudioStreamHandler->m_streamId))
            {
        pAu = &pC->mAudioAu;
        pAu->CTS = time64;
        pAu->DTS = time64;

        time64 = time64 * 1000; /* Convert the time into micro sec */
        pC->mAudioSeeking = M4OSA_TRUE;
        pC->mAudioSeekTime = time64;
        ALOGV("VideoEditor3gpReader_jump AUDIO time us %ld ", time64);
    } else if ((pC->mVideoStreamHandler != M4OSA_NULL) &&
            (pStreamHandler->m_streamId == pC->mVideoStreamHandler->m_streamId))
            {
        pAu = &pC->mVideoAu;
        pAu->CTS = time64;
        pAu->DTS = time64;

        time64 = time64 * 1000; /* Convert the time into micro sec */
        pC->mVideoSeeking = M4OSA_TRUE;
        pC->mVideoSeekTime = time64;
        ALOGV("VideoEditor3gpReader_jump VIDEO time us %ld ", time64);
    } else {
        ALOGV("VideoEditor3gpReader_jump passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }
    time64 = time64 / 1000; /* Convert the time into milli sec */
    ALOGV("VideoEditor3gpReader_jump time ms before seekset %ld ", time64);

    *pTime = (M4OSA_Int32)time64;

    ALOGV("VideoEditor3gpReader_jump end");
    err = M4NO_ERROR;
    return err;
}
/**
********************************************************************************
* @brief    reset the stream, that is seek it to beginning and make it ready
* @note
* @param    context:        (IN)    Context of the reader
* @param    pStreamHandler  (IN)    The stream handler of the stream to reset
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_PARAMETER         at least one parameter is not properly set
********************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_reset(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler) {
    VideoEditor3gpReader_Context* pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_StreamID streamIdArray[2];
    M4SYS_AccessUnit* pAu;
    M4OSA_Time time64 = 0;

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_reset: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_reset: invalid pointer to M4_StreamHandler");

    ALOGV("VideoEditor3gpReader_reset begin");

    if (pStreamHandler == (M4_StreamHandler*)pC->mAudioStreamHandler) {
        pAu = &pC->mAudioAu;
    } else if (pStreamHandler == (M4_StreamHandler*)pC->mVideoStreamHandler) {
        pAu = &pC->mVideoAu;
    } else {
        ALOGV("VideoEditor3gpReader_reset passed StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    pAu->CTS = time64;
    pAu->DTS = time64;

    ALOGV("VideoEditor3gpReader_reset end");
    return err;
}

/**
********************************************************************************
* @brief  Gets an access unit (AU) from the stream handler source.
* @note   An AU is the smallest possible amount of data to be decoded by decoder
*
* @param    context:        (IN) Context of the reader
* @param    pStreamHandler  (IN) The stream handler of the stream to make jump
* @param    pAccessUnit     (IO) Pointer to access unit to fill with read data
* @return   M4NO_ERROR           there is no error
* @return   M4ERR_PARAMETER      at least one parameter is not properly set
* @returns  M4ERR_ALLOC          memory allocation failed
* @returns  M4WAR_NO_MORE_AU     there are no more access unit in the stream
********************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_getNextAu(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4_AccessUnit *pAccessUnit) {
    VideoEditor3gpReader_Context* pC=(VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_AccessUnit* pAu;
    int64_t tempTime64 = 0;
    MediaBuffer *mMediaBuffer = NULL;
    MediaSource::ReadOptions options;
    M4OSA_Bool flag = M4OSA_FALSE;
    status_t error;
    int32_t i32Tmp = 0;

    M4OSA_DEBUG_IF1(( pC== 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getNextAu: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getNextAu: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
        "VideoEditor3gpReader_getNextAu: invalid pointer to M4_AccessUnit");

    ALOGV("VideoEditor3gpReader_getNextAu begin");

    if (pStreamHandler == (M4_StreamHandler*)pC->mAudioStreamHandler) {
        ALOGV("VideoEditor3gpReader_getNextAu audio stream");
        pAu = &pC->mAudioAu;
        if (pC->mAudioSeeking == M4OSA_TRUE) {
            ALOGV("VideoEditor3gpReader_getNextAu audio seek time: %ld",
                pC->mAudioSeekTime);
            options.setSeekTo(pC->mAudioSeekTime);
            pC->mAudioSource->read(&mMediaBuffer, &options);

            mMediaBuffer->meta_data()->findInt64(kKeyTime,
                (int64_t*)&tempTime64);
            options.clearSeekTo();
            pC->mAudioSeeking = M4OSA_FALSE;
            flag = M4OSA_TRUE;
        } else {
            ALOGV("VideoEditor3gpReader_getNextAu audio no seek:");
            pC->mAudioSource->read(&mMediaBuffer, &options);
            if (mMediaBuffer != NULL) {
                mMediaBuffer->meta_data()->findInt64(kKeyTime,
                    (int64_t*)&tempTime64);
            }
        }
    } else if (pStreamHandler == (M4_StreamHandler*)pC->mVideoStreamHandler) {
        ALOGV("VideoEditor3gpReader_getNextAu video steram ");
        pAu = &pC->mVideoAu;
        if(pC->mVideoSeeking == M4OSA_TRUE) {
            flag = M4OSA_TRUE;
            ALOGV("VideoEditor3gpReader_getNextAu seek: %ld",pC->mVideoSeekTime);
            options.setSeekTo(pC->mVideoSeekTime,
                MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
            do
            {
                if (mMediaBuffer != NULL) {
                    ALOGV("VideoEditor3gpReader_getNextAu free the MediaBuffer");
                    mMediaBuffer->release();
                }
                error = pC->mVideoSource->read(&mMediaBuffer, &options);
                ALOGV("VE3gpReader_getNextAu MediaBuffer %x , error %d",
                    mMediaBuffer, error);
                if (mMediaBuffer != NULL)
                {
                    if (mMediaBuffer->meta_data()->findInt32(kKeyIsSyncFrame,
                        &i32Tmp) && i32Tmp) {
                            ALOGV("SYNC FRAME FOUND--%d", i32Tmp);
                        pAu->attribute = AU_RAP;
                    }
                    else {
                        pAu->attribute = AU_P_Frame;
                    }
                    mMediaBuffer->meta_data()->findInt64(kKeyTime,
                        (int64_t*)&tempTime64);
                } else {
                    break;
                }
                options.clearSeekTo();
            } while(tempTime64 < pC->mVideoSeekTime);

            ALOGV("VE3gpReader_getNextAu: video  time with seek  = %lld:",
                tempTime64);
            pC->mVideoSeeking = M4OSA_FALSE;
        } else {
            ALOGV("VideoEditor3gpReader_getNextAu video no seek:");
            pC->mVideoSource->read(&mMediaBuffer, &options);

            if(mMediaBuffer != NULL) {
                if (mMediaBuffer->meta_data()->findInt32(kKeyIsSyncFrame,
                    &i32Tmp) && i32Tmp) {
                    ALOGV("SYNC FRAME FOUND--%d", i32Tmp);
                    pAu->attribute = AU_RAP;
                }
                else {
                    pAu->attribute = AU_P_Frame;
                }
                mMediaBuffer->meta_data()->findInt64(kKeyTime,
                    (int64_t*)&tempTime64);
                ALOGV("VE3gpReader_getNextAu: video no seek time = %lld:",
                    tempTime64);
            }else {
                ALOGV("VE3gpReader_getNextAu:video no seek time buffer is NULL");
            }
        }
    } else {
        ALOGV("VideoEditor3gpReader_getNextAu M4ERR_PARAMETER");
        return M4ERR_PARAMETER;
    }

    if (mMediaBuffer != NULL) {
        if( (pAu->dataAddress == NULL) ||  (pAu->size < \
            mMediaBuffer->range_length())) {
            if(pAu->dataAddress != NULL) {
                free((M4OSA_Int32*)pAu->dataAddress);
                pAu->dataAddress = NULL;
            }
            ALOGV("Buffer lenght = %d ,%d",(mMediaBuffer->range_length() +\
                3) & ~0x3,(mMediaBuffer->range_length()));

            pAu->dataAddress = (M4OSA_Int32*)M4OSA_32bitAlignedMalloc(
                (mMediaBuffer->range_length() + 3) & ~0x3,M4READER_3GP,
                    (M4OSA_Char*)"pAccessUnit->m_dataAddress" );
            if(pAu->dataAddress == NULL) {
                ALOGV("VideoEditor3gpReader_getNextAu malloc failed");
                return M4ERR_ALLOC;
            }
        }
        pAu->size = mMediaBuffer->range_length();

        memcpy((void *)pAu->dataAddress,
            (void *)((const char *)mMediaBuffer->data() + mMediaBuffer->range_offset()),
            mMediaBuffer->range_length());

        if( (pStreamHandler == (M4_StreamHandler*)pC->mVideoStreamHandler)  &&
            (pStreamHandler->m_streamType == M4DA_StreamTypeVideoMpeg4Avc) ) {
            M4OSA_UInt32 size = mMediaBuffer->range_length();
            M4OSA_UInt8 *lbuffer;

            lbuffer = (M4OSA_UInt8 *) pAu->dataAddress;
            ALOGV("pAccessUnit->m_dataAddress size = %x",size);

            lbuffer[0] = (size >> 24) & 0xFF;
            lbuffer[1] = (size >> 16) & 0xFF;
            lbuffer[2] = (size >> 8) & 0xFF;
            lbuffer[3] = (size) & 0xFF;
        }

        pAu->CTS = tempTime64;

        pAu->CTS = pAu->CTS / 1000; //converting the microsec to millisec
        ALOGV("VideoEditor3gpReader_getNextAu CTS = %ld",pAu->CTS);

        pAu->DTS  = pAu->CTS;
        if (pStreamHandler == (M4_StreamHandler*)pC->mAudioStreamHandler) {
            pAu->attribute = M4SYS_kFragAttrOk;
        }
        mMediaBuffer->release();

        pAccessUnit->m_dataAddress = (M4OSA_Int8*) pAu->dataAddress;
        pAccessUnit->m_size = pAu->size;
        pAccessUnit->m_maxsize = pAu->size;
        pAccessUnit->m_CTS = pAu->CTS;
        pAccessUnit->m_DTS = pAu->DTS;
        pAccessUnit->m_attribute = pAu->attribute;

    } else {
        ALOGV("VideoEditor3gpReader_getNextAu: M4WAR_NO_MORE_AU (EOS) reached");
        pAccessUnit->m_size = 0;
        err = M4WAR_NO_MORE_AU;
    }
    options.clearSeekTo();

    pAu->nbFrag = 0;
    mMediaBuffer = NULL;
    ALOGV("VideoEditor3gpReader_getNextAu end ");

    return err;
}
/**
 *******************************************************************************
 * @brief   Split the AVC DSI in its different components and write it in
 *          ONE memory buffer
 * @note
 * @param   pStreamHandler:         (IN/OUT) The MPEG4-AVC stream
 * @param   pDecoderConfigLocal:    (IN) The DSI buffer
 * @param   decoderConfigSizeLocal: (IN) The DSI buffer size
 * @return  M4NO_ERROR              there is no error
 * @return  ERR_FILE_SYNTAX_ERROR   pDecoderConfigLocal is NULL
 *******************************************************************************
*/
static M4OSA_ERR VideoEditor3gpReader_AnalyseAvcDsi(
        M4_StreamHandler *pStreamHandler, M4OSA_Int32* pDecoderConfigLocal,
        M4OSA_Int32 decoderConfigSizeLocal) {
    struct _avcSpecificInfo *pAvcSpecInfo = M4OSA_NULL;
    M4OSA_UInt32 uiSpecInfoSize;
    M4OSA_Context pBitParserContext = M4OSA_NULL;
    M4OSA_MemAddr8 pPos;

    /**
     * First parsing to get the total allocation size (we must not do
     * multiple malloc, but only one instead) */
    {
        M4OSA_Int32 val;
        M4OSA_UInt32 i,j;
        M4OSA_UInt8 nalUnitLength;
        M4OSA_UInt8  numOfSequenceParameterSets;
        M4OSA_UInt32 uiTotalSizeOfSPS = 0;
        M4OSA_UInt8  numOfPictureParameterSets;
        M4OSA_UInt32 uiTotalSizeOfPPS = 0;
        M4OSA_UInt32 uiSize;
        struct _avcSpecificInfo avcSpIf;

        avcSpIf.m_nalUnitLength = 0;

        if (M4OSA_NULL == pDecoderConfigLocal) {
            return M4ERR_READER3GP_DECODER_CONFIG_ERROR;
        }

        VideoEditor3gpReader_MPEG4BitStreamParserInit(&pBitParserContext,
            pDecoderConfigLocal, decoderConfigSizeLocal);

        if (M4OSA_NULL == pBitParserContext) {
            return M4ERR_ALLOC;
        }

        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
                                       /* 8 bits -- configuration version */
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
                                       /* 8 bits -- avc profile indication*/
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
                                       /* 8 bits -- profile compatibility */
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
                                       /* 8 bits -- avc level indication*/
        val=VideoEditor3gpReader_BitStreamParserShowBits(pBitParserContext, 8);
                       /* 6 bits reserved 111111b 2 bits length Size minus one*/
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
                                       /* m_nalUnitLength */

        nalUnitLength = (M4OSA_UInt8)((val & 0x03) + 1);/*0b11111100*/
        if (nalUnitLength > 4) {
            pStreamHandler->m_decoderSpecificInfoSize = 0;
            pStreamHandler->m_pDecoderSpecificInfo = M4OSA_NULL;
            VideoEditor3gpReader_BitStreamParserCleanUp(pBitParserContext);
        } else {
            /**
             * SPS table */
            val=VideoEditor3gpReader_BitStreamParserShowBits(pBitParserContext,
            8);/* 3 bits-reserved 111b-5 bits number of sequence parameter set*/
            numOfSequenceParameterSets = val & 0x1F;
            /*1F instead of E0*/ /*0b11100000*/ /*Number of seq parameter sets*/
            VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            for (i=0; i < numOfSequenceParameterSets; i++) {
                /**
                 * Get the size of this element */
                uiSize =
                    (M4OSA_UInt32)VideoEditor3gpReader_BitStreamParserShowBits(
                    pBitParserContext, 16);
                uiTotalSizeOfSPS += uiSize;
                VideoEditor3gpReader_BitStreamParserFlushBits(
                    pBitParserContext, 16);
                /**
                 *Read the element(dont keep it, we only want size right now) */
                for (j=0; j<uiSize; j++) {
                    VideoEditor3gpReader_BitStreamParserFlushBits(
                        pBitParserContext, 8);
                }
            }

            /**
             * SPS table */
            numOfPictureParameterSets=(M4OSA_UInt8)\
                VideoEditor3gpReader_BitStreamParserShowBits(pBitParserContext,
                    8);
            VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            for (i=0; i < numOfPictureParameterSets; i++) {
                /**
                 * Get the size of this element */
                uiSize = (M4OSA_UInt32)
                    VideoEditor3gpReader_BitStreamParserShowBits(
                    pBitParserContext, 16);
                uiTotalSizeOfPPS += uiSize;
                VideoEditor3gpReader_BitStreamParserFlushBits(
                    pBitParserContext, 16);
                /**
                 *Read the element(dont keep it,we only want size right now)*/
                for (j=0; j<uiSize; j++) {
                    VideoEditor3gpReader_BitStreamParserFlushBits(
                        pBitParserContext, 8);
                }
            }

            /**
             * Compute the size of the full buffer */
            uiSpecInfoSize = sizeof(struct _avcSpecificInfo) +
                     numOfSequenceParameterSets * sizeof(struct _parameterSet)
                     + /**< size of the table of SPS elements */
                     numOfPictureParameterSets  * sizeof(struct _parameterSet)
                     + /**< size of the table of PPS elements */
                     uiTotalSizeOfSPS +
                     uiTotalSizeOfPPS;
            /**
             * Allocate the buffer */
            pAvcSpecInfo =(struct _avcSpecificInfo*)M4OSA_32bitAlignedMalloc(uiSpecInfoSize,
                M4READER_3GP, (M4OSA_Char*)"MPEG-4 AVC DecoderSpecific");
            if (M4OSA_NULL == pAvcSpecInfo) {
                VideoEditor3gpReader_BitStreamParserCleanUp(pBitParserContext);
                return M4ERR_ALLOC;
            }

            /**
             * Set the pointers to the correct part of the buffer */
            pAvcSpecInfo->m_nalUnitLength = nalUnitLength;
            pAvcSpecInfo->m_numOfSequenceParameterSets =
                numOfSequenceParameterSets;
            pAvcSpecInfo->m_numOfPictureParameterSets  =
                numOfPictureParameterSets;

            /* We place the SPS param sets table after m_pPictureParameterSet */
            pAvcSpecInfo->m_pSequenceParameterSet= (struct _parameterSet*)(
                (M4OSA_MemAddr8)(&pAvcSpecInfo->m_pPictureParameterSet) +
                sizeof(pAvcSpecInfo->m_pPictureParameterSet));
            /*We place the PPS param sets table after the SPS param sets table*/
            pAvcSpecInfo->m_pPictureParameterSet = (struct _parameterSet*)(
                (M4OSA_MemAddr8)(pAvcSpecInfo->m_pSequenceParameterSet) +
                (numOfSequenceParameterSets * sizeof(struct _parameterSet)));
            /**< The data will be placed after the PPS param sets table */
            pPos = (M4OSA_MemAddr8)pAvcSpecInfo->m_pPictureParameterSet +
                (numOfPictureParameterSets * sizeof(struct _parameterSet));

            /**
             * reset the bit parser */
            VideoEditor3gpReader_BitStreamParserCleanUp(pBitParserContext);
        }
    }

    /**
     * Second parsing to copy the data */
    if (M4OSA_NULL != pAvcSpecInfo) {
        M4OSA_Int32 i,j;

        VideoEditor3gpReader_MPEG4BitStreamParserInit(&pBitParserContext,
            pDecoderConfigLocal, decoderConfigSizeLocal);

        if (M4OSA_NULL == pBitParserContext) {
            free(pAvcSpecInfo);
            return M4ERR_ALLOC;
        }

        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* 8 bits -- configuration version */
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* 8 bits -- avc profile indication*/
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* 8 bits -- profile compatibility */
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* 8 bits -- avc level indication*/
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* m_nalUnitLength */
        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
        /* 3 bits -- reserved 111b -- 5 bits number of sequence parameter set*/

        for (i=0; i < pAvcSpecInfo->m_numOfSequenceParameterSets; i++) {
            pAvcSpecInfo->m_pSequenceParameterSet[i].m_length =
                (M4OSA_UInt16)VideoEditor3gpReader_BitStreamParserShowBits(
                pBitParserContext, 16);
            VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext,16);

            pAvcSpecInfo->m_pSequenceParameterSet[i].m_pParameterSetUnit =
                (M4OSA_UInt8*)pPos;  /**< current position in the buffer */
            pPos += pAvcSpecInfo->m_pSequenceParameterSet[i].m_length;
                /**< increment the position in the buffer */
            for (j=0; j<pAvcSpecInfo->m_pSequenceParameterSet[i].m_length;j++){
                pAvcSpecInfo->m_pSequenceParameterSet[i].m_pParameterSetUnit[j]=
                    (M4OSA_UInt8)VideoEditor3gpReader_BitStreamParserShowBits(
                    pBitParserContext, 8);
                VideoEditor3gpReader_BitStreamParserFlushBits(
                    pBitParserContext, 8);
            }
        }

        VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext, 8);
            /* number of pîcture parameter set*/

        for (i=0; i < pAvcSpecInfo->m_numOfPictureParameterSets; i++) {
            pAvcSpecInfo->m_pPictureParameterSet[i].m_length =
                (M4OSA_UInt16)VideoEditor3gpReader_BitStreamParserShowBits(
                pBitParserContext, 16);
            VideoEditor3gpReader_BitStreamParserFlushBits(pBitParserContext,16);

            pAvcSpecInfo->m_pPictureParameterSet[i].m_pParameterSetUnit =
                (M4OSA_UInt8*)pPos;   /**< current position in the buffer */
            pPos += pAvcSpecInfo->m_pPictureParameterSet[i].m_length;
                /**< increment the position in the buffer */
            for (j=0; j<pAvcSpecInfo->m_pPictureParameterSet[i].m_length; j++) {
                pAvcSpecInfo->m_pPictureParameterSet[i].m_pParameterSetUnit[j] =
                    (M4OSA_UInt8)VideoEditor3gpReader_BitStreamParserShowBits(
                    pBitParserContext, 8);
                VideoEditor3gpReader_BitStreamParserFlushBits(
                    pBitParserContext, 8);
            }
        }
        VideoEditor3gpReader_BitStreamParserCleanUp(pBitParserContext);
        pStreamHandler->m_decoderSpecificInfoSize = uiSpecInfoSize;
        pStreamHandler->m_pDecoderSpecificInfo = (M4OSA_UInt8*)pAvcSpecInfo;
    }
    pStreamHandler->m_H264decoderSpecificInfoSize  =  decoderConfigSizeLocal;
    pStreamHandler->m_pH264DecoderSpecificInfo  = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
        decoderConfigSizeLocal, M4READER_3GP,
        (M4OSA_Char*)"MPEG-4 AVC DecoderSpecific");
    if (M4OSA_NULL == pStreamHandler->m_pH264DecoderSpecificInfo) {
        goto cleanup;
    }

    memcpy((void * ) pStreamHandler->m_pH264DecoderSpecificInfo,
        (void * )pDecoderConfigLocal,
        pStreamHandler->m_H264decoderSpecificInfoSize);
    return M4NO_ERROR;
cleanup:
    VideoEditor3gpReader_BitStreamParserCleanUp(pBitParserContext);
    return M4ERR_READER3GP_DECODER_CONFIG_ERROR;
}
/**
********************************************************************************
* @brief    Get the next stream found in the 3gp file
* @note
* @param    context:     (IN)    Context of the reader
* @param    pMediaFamily: OUT)   pointer to a user allocated
*                                M4READER_MediaFamily that will be filled
*                                with the media family of the found stream
* @param    pStreamHandler:(OUT) pointer to StreamHandler that will be allocated
*                                and filled with the found stream description
* @return   M4NO_ERROR              there is no error
* @return   M4ERR_BAD_CONTEXT       provided context is not a valid one
* @return   M4ERR_PARAMETER         at least one parameter is not properly set
* @return   M4WAR_NO_MORE_STREAM    no more available stream in the media
********************************************************************************
*/
M4OSA_ERR VideoEditor3gpReader_getNextStreamHandler(M4OSA_Context context,
        M4READER_MediaFamily *pMediaFamily,
        M4_StreamHandler **pStreamHandler) {
    VideoEditor3gpReader_Context* pC=(VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_StreamID streamIdArray[2];
    M4SYS_StreamDescription streamDesc;
    M4_AudioStreamHandler* pAudioStreamHandler;
    M4_VideoStreamHandler* pVideoStreamHandler;
    M4OSA_Int8 *DecoderSpecificInfo = M4OSA_NULL;
    M4OSA_Int32 decoderSpecificInfoSize =0, maxAUSize = 0;

    M4_StreamType streamType = M4DA_StreamTypeUnknown;
    M4OSA_UInt8 temp, i, trackCount;
    M4OSA_Bool haveAudio = M4OSA_FALSE;
    M4OSA_Bool haveVideo = M4OSA_FALSE;
    sp<MetaData> meta  = NULL;
    int64_t Duration = 0;
    M4OSA_UInt8* DecoderSpecific = M4OSA_NULL ;
    uint32_t type;
    const void *data;
    size_t size;
    const void *codec_specific_data;
    size_t codec_specific_data_size;
    M4OSA_Int32  ptempTime;
    M4OSA_Int32  avgFPS=0;

    ALOGV("VideoEditor3gpReader_getNextStreamHandler begin");

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getNextStreamHandler: invalid context");
    M4OSA_DEBUG_IF1((pMediaFamily   == 0), M4ERR_PARAMETER,
        "getNextStreamHandler: invalid pointer to MediaFamily");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "getNextStreamHandler: invalid pointer to StreamHandler");

    trackCount = pC->mExtractor->countTracks();
    temp = pC->mCurrTrack;

    if(temp >= trackCount) {
        ALOGV("VideoEditor3gpReader_getNextStreamHandler error = %d",
            M4WAR_NO_MORE_STREAM);
        return (M4WAR_NO_MORE_STREAM);
    } else {
        const char *mime;
        meta = pC->mExtractor->getTrackMetaData(temp);
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!haveVideo && !strncasecmp(mime, "video/", 6)) {
            pC->mVideoSource = pC->mExtractor->getTrack(temp);
            pC->mVideoSource->start();

            *pMediaFamily = M4READER_kMediaFamilyVideo;
            haveVideo = true;
            ALOGV("VideoEditor3gpReader_getNextStreamHandler getTrack called");
            if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_AVC)) {
                streamType = M4DA_StreamTypeVideoMpeg4Avc;
            } else if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_H263)) {
                streamType = M4DA_StreamTypeVideoH263;
            } else if (!strcasecmp(mime, MEDIA_MIMETYPE_VIDEO_MPEG4)) {
                streamType = M4DA_StreamTypeVideoMpeg4;
            } else {
                ALOGV("VideoEditor3gpReaderGetNextStreamHandler streamTypeNONE");
            }
            ALOGV("VideoEditor3gpReader_getNextStreamHandler: stream type: %d ",
                streamType);

            if(streamType != M4DA_StreamTypeUnknown) {
                pC->mStreamType = streamType;
                pC->mStreamId = pC->mCurrTrack;

                pVideoStreamHandler = (M4_VideoStreamHandler*)M4OSA_32bitAlignedMalloc
                    (sizeof(M4_VideoStreamHandler), M4READER_3GP,
                    (M4OSA_Char*)"M4_VideoStreamHandler");
                if (M4OSA_NULL == pVideoStreamHandler) {
                    return M4ERR_ALLOC;
                }
                pVideoStreamHandler->m_structSize=sizeof(M4_VideoStreamHandler);

                meta->findInt32(kKeyWidth,
                    (int32_t*)&(pVideoStreamHandler->m_videoWidth));
                meta->findInt32(kKeyHeight,
                    (int32_t*)&(pVideoStreamHandler->m_videoHeight));

                (*pStreamHandler)  = (M4_StreamHandler*)(pVideoStreamHandler);
                meta->findInt64(kKeyDuration, (int64_t*)&(Duration));
                ((*pStreamHandler)->m_duration) = (int32_t)((Duration)/1000); // conversion to mS
                pC->mMaxDuration = ((*pStreamHandler)->m_duration);
                if (pC->mMaxDuration == 0) {
                    ALOGE("Video is too short: %lld Us", Duration);
                    delete pVideoStreamHandler;
                    pVideoStreamHandler = NULL;
                    return M4ERR_PARAMETER;
                }
                ALOGV("VideoEditor3gpReader_getNextStreamHandler m_duration %d",
                    (*pStreamHandler)->m_duration);

                off64_t fileSize = 0;
                pC->mDataSource->getSize(&fileSize);
                pC->mFileSize  = fileSize;

                ALOGV("VideoEditor3gpReader_getNextStreamHandler m_fileSize %d",
                    pC->mFileSize);

                meta->findInt32(kKeyMaxInputSize, (int32_t*)&(maxAUSize));
                if(maxAUSize == 0) {
                    maxAUSize = 70000;
                }
                (*pStreamHandler)->m_maxAUSize = maxAUSize;
                ALOGV("<<<<<<<<<<   video: mMaxAUSize from MP4 extractor: %d",
                    (*pStreamHandler)->m_maxAUSize);

                ((M4_StreamHandler*)pVideoStreamHandler)->m_averageBitRate =
                        (pC->mFileSize * 8000)/pC->mMaxDuration;
                ALOGV("VideoEditor3gpReader_getNextStreamHandler m_averageBitrate %d",
                    ((M4_StreamHandler*)pVideoStreamHandler)->m_averageBitRate);


                meta->findInt32(kKeyFrameRate,
                    (int32_t*)&(avgFPS));
                ALOGV("<<<<<<<<<<   video: Average FPS from MP4 extractor: %d",
                    avgFPS);

                pVideoStreamHandler->m_averageFrameRate =(M4OSA_Float) avgFPS;
                ALOGV("<<<<<<<<<<   video: Average FPS from MP4 extractor in FLOAT: %f",
                    pVideoStreamHandler->m_averageFrameRate);

                // Get the video rotation degree
                int32_t rotationDegree;
                if(!meta->findInt32(kKeyRotation, &rotationDegree)) {
                    rotationDegree = 0;
                }
                pVideoStreamHandler->videoRotationDegrees = rotationDegree;

                pC->mVideoStreamHandler =
                    (M4_StreamHandler*)(pVideoStreamHandler);

                /* Get the DSI info */
                if(M4DA_StreamTypeVideoH263 == streamType) {
                    if (meta->findData(kKeyD263, &type, &data, &size)) {
                        (*pStreamHandler)->m_decoderSpecificInfoSize = size;
                        if ((*pStreamHandler)->m_decoderSpecificInfoSize != 0) {
                            DecoderSpecific = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                                (*pStreamHandler)->m_decoderSpecificInfoSize,
                                M4READER_3GP,(M4OSA_Char*)"H263 DSI");
                            if (M4OSA_NULL == DecoderSpecific) {
                                return M4ERR_ALLOC;
                            }
                            memcpy((void *)DecoderSpecific,
                                (void *)data, size);
                            (*pStreamHandler)->m_pDecoderSpecificInfo =
                                DecoderSpecific;
                        }
                        else {
                            (*pStreamHandler)->m_pDecoderSpecificInfo =
                                M4OSA_NULL;
                            (*pStreamHandler)->m_decoderSpecificInfoSize = 0;
                        }
                        (*pStreamHandler)->m_pESDSInfo = M4OSA_NULL;
                        (*pStreamHandler)->m_ESDSInfoSize = 0;
                        (*pStreamHandler)->m_pH264DecoderSpecificInfo = M4OSA_NULL;
                        (*pStreamHandler)->m_H264decoderSpecificInfoSize = 0;
                    } else {
                        ALOGV("VE_getNextStreamHandler: H263 dsi not found");
                        (*pStreamHandler)->m_pDecoderSpecificInfo = M4OSA_NULL;
                        (*pStreamHandler)->m_decoderSpecificInfoSize = 0;
                        (*pStreamHandler)->m_H264decoderSpecificInfoSize = 0;
                        (*pStreamHandler)->m_pH264DecoderSpecificInfo =
                            M4OSA_NULL;
                        (*pStreamHandler)->m_pESDSInfo = M4OSA_NULL;
                        (*pStreamHandler)->m_ESDSInfoSize = 0;
                    }
                }
                else if(M4DA_StreamTypeVideoMpeg4Avc == streamType) {
                    if(meta->findData(kKeyAVCC, &type, &data, &size)) {
                        decoderSpecificInfoSize = size;
                        if (decoderSpecificInfoSize != 0) {
                            DecoderSpecificInfo = (M4OSA_Int8*)M4OSA_32bitAlignedMalloc(
                                decoderSpecificInfoSize, M4READER_3GP,
                                (M4OSA_Char*)"H264 DecoderSpecific" );
                            if (M4OSA_NULL == DecoderSpecificInfo) {
                                ALOGV("VideoEditor3gp_getNextStream is NULL ");
                                return M4ERR_ALLOC;
                            }
                            memcpy((void *)DecoderSpecificInfo,
                                (void *)data, decoderSpecificInfoSize);
                        } else {
                            ALOGV("DSI Size %d", decoderSpecificInfoSize);
                            DecoderSpecificInfo = M4OSA_NULL;
                        }
                    }
                    (*pStreamHandler)->m_pESDSInfo = M4OSA_NULL;
                    (*pStreamHandler)->m_ESDSInfoSize = 0;

                    err = VideoEditor3gpReader_AnalyseAvcDsi(*pStreamHandler,
                    (M4OSA_Int32*)DecoderSpecificInfo, decoderSpecificInfoSize);

                    if (M4NO_ERROR != err) {
                        return err;
                    }
                    ALOGV("decsize %d, h264decsize %d: %d", (*pStreamHandler)->\
                        m_decoderSpecificInfoSize, (*pStreamHandler)->\
                        m_H264decoderSpecificInfoSize);

                    if(M4OSA_NULL != DecoderSpecificInfo) {
                        free(DecoderSpecificInfo);
                        DecoderSpecificInfo = M4OSA_NULL;
                    }
                } else if( (M4DA_StreamTypeVideoMpeg4 == streamType) ) {
                    if (meta->findData(kKeyESDS, &type, &data, &size)) {
                        ESDS esds((const char *)data, size);
                        CHECK_EQ(esds.InitCheck(), (status_t)OK);

                        (*pStreamHandler)->m_ESDSInfoSize = size;
                        (*pStreamHandler)->m_pESDSInfo = (M4OSA_UInt8*)\
                        M4OSA_32bitAlignedMalloc((*pStreamHandler)->m_ESDSInfoSize,
                        M4READER_3GP, (M4OSA_Char*)"M4V DecoderSpecific" );
                        if (M4OSA_NULL == (*pStreamHandler)->m_pESDSInfo) {
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)(*pStreamHandler)->\
                            m_pESDSInfo, (void *)data, size);

                        esds.getCodecSpecificInfo(&codec_specific_data,
                            &codec_specific_data_size);
                        ALOGV("VE MP4 dsisize: %d, %x", codec_specific_data_size,
                            codec_specific_data);

                        (*pStreamHandler)->m_decoderSpecificInfoSize =
                            codec_specific_data_size;
                        if ((*pStreamHandler)->m_decoderSpecificInfoSize != 0) {
                            DecoderSpecific = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                                (*pStreamHandler)->m_decoderSpecificInfoSize,
                                M4READER_3GP, (M4OSA_Char*)" DecoderSpecific" );
                            if (M4OSA_NULL == DecoderSpecific) {
                                return M4ERR_ALLOC;
                            }
                            memcpy((void *)DecoderSpecific,
                                (void *)codec_specific_data,
                                codec_specific_data_size);
                            (*pStreamHandler)->m_pDecoderSpecificInfo =
                                DecoderSpecific;
                        }
                        else {
                            (*pStreamHandler)->m_pDecoderSpecificInfo =
                                M4OSA_NULL;
                        }
                        (*pStreamHandler)->m_pH264DecoderSpecificInfo =
                            M4OSA_NULL;
                        (*pStreamHandler)->m_H264decoderSpecificInfoSize = 0;
                    }
                } else {
                    ALOGV("VideoEditor3gpReader_getNextStream NO video stream");
                    return M4ERR_READER_UNKNOWN_STREAM_TYPE;
                }
            }
            else {
                ALOGV("VideoEditor3gpReader_getNextStream NO video stream");
                return M4ERR_READER_UNKNOWN_STREAM_TYPE;
            }

        } else if (!haveAudio && !strncasecmp(mime, "audio/", 6)) {
            ALOGV("VideoEditor3gpReader_getNextStream audio getTrack called");
            pC->mAudioSource = pC->mExtractor->getTrack(pC->mCurrTrack);
            pC->mAudioSource->start();
            *pMediaFamily = M4READER_kMediaFamilyAudio;

            if(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_NB)) {
                streamType = M4DA_StreamTypeAudioAmrNarrowBand;
            } else if(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AMR_WB)) {
                streamType = M4DA_StreamTypeAudioAmrWideBand;
            }
            else if(!strcasecmp(mime, MEDIA_MIMETYPE_AUDIO_AAC)) {
                streamType = M4DA_StreamTypeAudioAac;
            } else {
                ALOGV("VideoEditor3gpReader_getNextStrea streamtype Unknown ");
            }
            if(streamType != M4DA_StreamTypeUnknown) {
                pC->mStreamType = streamType;
                pC->mStreamId = pC->mCurrTrack;

                ALOGV("VE streamtype %d ,id %d",  streamType, pC->mCurrTrack);

                pAudioStreamHandler = (M4_AudioStreamHandler*)M4OSA_32bitAlignedMalloc
                    (sizeof(M4_AudioStreamHandler), M4READER_3GP,
                    (M4OSA_Char*)"M4_AudioStreamHandler");
                if (M4OSA_NULL == pAudioStreamHandler) {
                    return M4ERR_ALLOC;
                }
                pAudioStreamHandler->m_structSize=sizeof(M4_AudioStreamHandler);
                pAudioStreamHandler->m_byteSampleSize   = 0;
                pAudioStreamHandler->m_nbChannels       = 0;
                pAudioStreamHandler->m_samplingFrequency= 0;
                pAudioStreamHandler->m_byteFrameLength  = 0;

                (*pStreamHandler) = (M4_StreamHandler*)(pAudioStreamHandler);
                pC->mAudioStreamHandler =
                    (M4_StreamHandler*)(pAudioStreamHandler);
                (*pStreamHandler)->m_averageBitRate = 0;
                haveAudio = true;
                pC->mAudioStreamHandler=(M4_StreamHandler*)pAudioStreamHandler;
                pC->mAudioStreamHandler->m_pESDSInfo = M4OSA_NULL;
                pC->mAudioStreamHandler->m_ESDSInfoSize = 0;

                meta->findInt32(kKeyMaxInputSize, (int32_t*)&(maxAUSize));
                if(maxAUSize == 0) {
                    maxAUSize = 70000;
                }
                (*pStreamHandler)->m_maxAUSize = maxAUSize;
                ALOGV("VE Audio mMaxAUSize from MP4 extractor: %d", maxAUSize);
            }
            if((M4DA_StreamTypeAudioAmrNarrowBand == streamType) ||
                (M4DA_StreamTypeAudioAmrWideBand == streamType)) {
                M4OSA_UInt32 freqIndex = 0; /**< AMR NB */
                M4OSA_UInt32 modeSet;
                M4OSA_UInt32 i;
                M4OSA_Context pBitParserContext = M4OSA_NULL;

                if(M4DA_StreamTypeAudioAmrWideBand == streamType) {
                    freqIndex = 1; /**< AMR WB */
                }

                if (meta->findData(kKeyESDS, &type, &data, &size)) {
                    ESDS esds((const char *)data, size);
                    CHECK_EQ(esds.InitCheck(), (status_t)OK);

                    esds.getCodecSpecificInfo(&codec_specific_data,
                        &codec_specific_data_size);
                    (*pStreamHandler)->m_decoderSpecificInfoSize =
                        codec_specific_data_size;

                    if ((*pStreamHandler)->m_decoderSpecificInfoSize != 0) {
                        DecoderSpecific = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                            (*pStreamHandler)->m_decoderSpecificInfoSize,
                            M4READER_3GP, (M4OSA_Char*)"AMR DecoderSpecific" );
                        if (M4OSA_NULL == DecoderSpecific) {
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)DecoderSpecific,
                            (void *)codec_specific_data,
                            codec_specific_data_size);
                        (*pStreamHandler)->m_pDecoderSpecificInfo =
                            DecoderSpecific;
                    } else {
                        (*pStreamHandler)->m_pDecoderSpecificInfo = M4OSA_NULL;
                    }
                } else {
                    M4OSA_UChar AmrDsi[] =
                        {'P','H','L','P',0x00, 0x00, 0x80, 0x00, 0x01,};
                    (*pStreamHandler)->m_decoderSpecificInfoSize = 9;
                    DecoderSpecific = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                        (*pStreamHandler)->m_decoderSpecificInfoSize,
                        M4READER_3GP, (M4OSA_Char*)"PHLP DecoderSpecific" );
                    if (M4OSA_NULL == DecoderSpecific) {
                        return M4ERR_ALLOC;
                    }
                    if(freqIndex ==0) {
                        AmrDsi[8] = 0x01;
                    } else {
                        AmrDsi[8] = 0x02;
                    }
                    for(i = 0; i< 9; i++) {
                        DecoderSpecific[i] = AmrDsi[i];
                    }
                    (*pStreamHandler)->m_pDecoderSpecificInfo = DecoderSpecific;
                }
                (*pStreamHandler)->m_averageBitRate =
                    VideoEditor3gpReader_AmrBitRate[freqIndex][7];
            } else if((M4DA_StreamTypeAudioAac == streamType)) {
                if (meta->findData(kKeyESDS, &type, &data, &size)) {
                    ESDS esds((const char *)data, size);
                    CHECK_EQ(esds.InitCheck(), (status_t)OK);

                    (*pStreamHandler)->m_ESDSInfoSize = size;
                    (*pStreamHandler)->m_pESDSInfo = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                        (*pStreamHandler)->m_ESDSInfoSize, M4READER_3GP,
                        (M4OSA_Char*)"AAC DecoderSpecific" );
                    if (M4OSA_NULL == (*pStreamHandler)->m_pESDSInfo) {
                        return M4ERR_ALLOC;
                    }
                    memcpy((void *)(*pStreamHandler)->m_pESDSInfo,
                    (void *)data, size);
                    esds.getCodecSpecificInfo(&codec_specific_data,
                        &codec_specific_data_size);

                    ALOGV("VEdsi %d,%x",codec_specific_data_size,
                        codec_specific_data);

                    (*pStreamHandler)->m_decoderSpecificInfoSize =
                        codec_specific_data_size;
                    if ((*pStreamHandler)->m_decoderSpecificInfoSize != 0) {
                        DecoderSpecific = (M4OSA_UInt8*)M4OSA_32bitAlignedMalloc(
                            (*pStreamHandler)->m_decoderSpecificInfoSize,
                            M4READER_3GP, (M4OSA_Char*)"AAC DecoderSpecific" );
                        if (M4OSA_NULL == DecoderSpecific) {
                            return M4ERR_ALLOC;
                        }
                        memcpy((void *)DecoderSpecific,
                            (void *)codec_specific_data,
                            codec_specific_data_size);
                        (*pStreamHandler)->m_pDecoderSpecificInfo =
                            DecoderSpecific;
                    } else {
                        (*pStreamHandler)->m_pDecoderSpecificInfo = M4OSA_NULL;
                    }
                }
            } else {
                ALOGV("VideoEditor3gpReader_getNextStream mStreamType: none ");
                return M4ERR_READER_UNKNOWN_STREAM_TYPE;
            }
        } else {
            ALOGV("VE noaudio-video stream:pC->mCurrTrack = %d ",pC->mCurrTrack);
            pC->mCurrTrack++; //Increment current track to get the next track
            return M4ERR_READER_UNKNOWN_STREAM_TYPE;
        }
        ALOGV("VE StreamType: %d, stremhandler %x",streamType, *pStreamHandler );
        (*pStreamHandler)->m_streamType = streamType;
        (*pStreamHandler)->m_streamId   = pC->mStreamId;
        (*pStreamHandler)->m_pUserData  = M4OSA_NULL;
        (*pStreamHandler)->m_structSize = sizeof(M4_StreamHandler);
        (*pStreamHandler)->m_bStreamIsOK = M4OSA_TRUE;

        meta->findInt64(kKeyDuration,
            (int64_t*)&(Duration));

        (*pStreamHandler)->m_duration = (int32_t)(Duration / 1000);

        pC->mMaxDuration = ((*pStreamHandler)->m_duration);
        ALOGV("VE str duration duration: %d ", (*pStreamHandler)->m_duration);

        /* In AAC case: Put the first AU in pAudioStreamHandler->m_pUserData
         *since decoder has to know if stream contains SBR data(Implicit sig) */
        if(M4DA_StreamTypeAudioAac == (*pStreamHandler)->m_streamType) {
            M4READER_AudioSbrUserdata*  pAudioSbrUserdata;

            pAudioSbrUserdata = (M4READER_AudioSbrUserdata*)M4OSA_32bitAlignedMalloc(
                sizeof(M4READER_AudioSbrUserdata),M4READER_3GP,
                (M4OSA_Char*)"M4READER_AudioSbrUserdata");
            if (M4OSA_NULL == pAudioSbrUserdata) {
                err = M4ERR_ALLOC;
                goto Error;
            }
            (*pStreamHandler)->m_pUserData = pAudioSbrUserdata;
            pAudioSbrUserdata->m_bIsSbrEnabled = M4OSA_FALSE;

            pAudioSbrUserdata->m_pFirstAU = (M4_AccessUnit*)M4OSA_32bitAlignedMalloc(
                sizeof(M4_AccessUnit),M4READER_3GP, (M4OSA_Char*)"1st AAC AU");
            if (M4OSA_NULL == pAudioSbrUserdata->m_pFirstAU) {
                pAudioSbrUserdata->m_pAacDecoderUserConfig = M4OSA_NULL;
                err = M4ERR_ALLOC;
                goto Error;
            }
            pAudioSbrUserdata->m_pAacDecoderUserConfig = (M4_AacDecoderConfig*)\
                M4OSA_32bitAlignedMalloc(sizeof(M4_AacDecoderConfig),M4READER_3GP,
                (M4OSA_Char*)"m_pAacDecoderUserConfig");
            if (M4OSA_NULL == pAudioSbrUserdata->m_pAacDecoderUserConfig) {
                err = M4ERR_ALLOC;
                goto Error;
            }
        }
        if(M4DA_StreamTypeAudioAac == (*pStreamHandler)->m_streamType) {
            M4_AudioStreamHandler* pAudioStreamHandler =
                (M4_AudioStreamHandler*)(*pStreamHandler);
            M4READER_AudioSbrUserdata* pUserData = (M4READER_AudioSbrUserdata*)\
                (pAudioStreamHandler->m_basicProperties.m_pUserData);

            err = VideoEditor3gpReader_fillAuStruct(pC, (*pStreamHandler),
                (M4_AccessUnit*)pUserData->m_pFirstAU);
            if (M4NO_ERROR != err) {
                goto Error;
            }
            err = VideoEditor3gpReader_getNextAu(pC, (*pStreamHandler),
                (M4_AccessUnit*)pUserData->m_pFirstAU);

            /*
             * 1. "M4WAR_NO_MORE_AU == err" indicates that there is no more
             * access unit from the current track. In other words, there
             * is only a single access unit from the current track, and
             * the parsing of this track has reached EOS. The reason why
             * the first access unit needs to be parsed here is because for
             * some audio codec (like AAC), the very first access unit
             * must be decoded before its configuration/encoding parameters
             * (such as # of channels and sample rate) can be correctly
             * determined.
             *
             * 2. "trackCount > pC->mCurrTrack" indicates that there are other
             * tracks to be parsed, in addition to the current track.
             *
             * When both conditions 1 & 2 hold, other tracks should be
             * parsed. Thus, we should not bail out.
             */
            if (M4WAR_NO_MORE_AU == err && trackCount > pC->mCurrTrack) {
                err = M4NO_ERROR;
            }

            if (M4NO_ERROR != err) {
                goto Error;
            }
            err = VideoEditor3gpReader_reset(pC, (*pStreamHandler));
            if (M4NO_ERROR != err) {
                goto Error;
            }
        }
    }
    pC->mCurrTrack++; //Increment the current track to get next track
    ALOGV("pC->mCurrTrack = %d",pC->mCurrTrack);

    if (!haveAudio && !haveVideo) {
        *pMediaFamily=M4READER_kMediaFamilyUnknown;
        return M4ERR_READER_UNKNOWN_STREAM_TYPE;
    }
Error:
    ALOGV("VideoEditor3gpReader_getNextStreamHandler end error = %d",err);
    return err;
}

M4OSA_ERR VideoEditor3gpReader_getPrevRapTime(M4OSA_Context context,
    M4_StreamHandler *pStreamHandler, M4OSA_Int32* pTime)
{
    VideoEditor3gpReader_Context *pC = (VideoEditor3gpReader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    MediaBuffer *mMediaBuffer = M4OSA_NULL;
    MediaSource::ReadOptions options;
    M4OSA_Time time64;
    int64_t tempTime64 = 0;
    status_t error;

    ALOGV("VideoEditor3gpReader_getPrevRapTime begin");

    M4OSA_DEBUG_IF1((pC == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getPrevRapTime: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getPrevRapTime invalid pointer to StreamHandler");
    M4OSA_DEBUG_IF1((pTime == 0), M4ERR_PARAMETER,
        "VideoEditor3gpReader_getPrevRapTime: invalid time pointer");
    if (*pTime == (pStreamHandler->m_duration)) {
        *pTime -= 1;
    }

    time64 = (M4OSA_Time)*pTime * 1000;

    ALOGV("VideoEditor3gpReader_getPrevRapTime seek time: %ld",time64);
    options.setSeekTo(time64, MediaSource::ReadOptions::SEEK_PREVIOUS_SYNC);
    error = pC->mVideoSource->read(&mMediaBuffer, &options);
    if (error != OK) {
        //Can not get the previous Sync.
        //Must be end of stream.
        return M4WAR_NO_MORE_AU;
    }

    mMediaBuffer->meta_data()->findInt64(kKeyTime, (int64_t*)&tempTime64);
    ALOGV("VideoEditor3gpReader_getPrevRapTime read time %ld, %x", tempTime64,
        mMediaBuffer);

    *pTime = (M4OSA_Int32)(tempTime64 / 1000);

    if(mMediaBuffer != M4OSA_NULL) {
        ALOGV(" mMediaBuffer size = %d length %d", mMediaBuffer->size(),
            mMediaBuffer->range_length());
        mMediaBuffer->release();
        mMediaBuffer = M4OSA_NULL;
    }
    options.clearSeekTo();

    if(error != OK) {
        ALOGV("VideoEditor3gpReader_getPrevRapTime end \
            M4WAR_READER_INFORMATION_NOT_PRESENT");
        return M4WAR_READER_INFORMATION_NOT_PRESENT;
    } else {
        ALOGV("VideoEditor3gpReader_getPrevRapTime end: err %x", err);
        err = M4NO_ERROR;
        return err;
    }
}

extern "C" {
M4OSA_ERR VideoEditor3gpReader_getInterface(M4READER_MediaType *pMediaType,
        M4READER_GlobalInterface **pRdrGlobalInterface,
        M4READER_DataInterface **pRdrDataInterface) {

    M4OSA_ERR err = M4NO_ERROR;

    VIDEOEDITOR_CHECK(M4OSA_NULL != pMediaType,      M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pRdrGlobalInterface, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pRdrDataInterface, M4ERR_PARAMETER);

    ALOGV("VideoEditor3gpReader_getInterface begin");
    ALOGV("VideoEditor3gpReader_getInterface %d 0x%x 0x%x", *pMediaType,
        *pRdrGlobalInterface,*pRdrDataInterface);

    SAFE_MALLOC(*pRdrGlobalInterface, M4READER_GlobalInterface, 1,
        "VideoEditor3gpReader_getInterface");
    SAFE_MALLOC(*pRdrDataInterface, M4READER_DataInterface, 1,
        "VideoEditor3gpReader_getInterface");

    *pMediaType = M4READER_kMediaType3GPP;

    (*pRdrGlobalInterface)->m_pFctCreate       = VideoEditor3gpReader_create;
    (*pRdrGlobalInterface)->m_pFctDestroy      = VideoEditor3gpReader_destroy;
    (*pRdrGlobalInterface)->m_pFctOpen         = VideoEditor3gpReader_open;
    (*pRdrGlobalInterface)->m_pFctClose        = VideoEditor3gpReader_close;
    (*pRdrGlobalInterface)->m_pFctGetOption    = VideoEditor3gpReader_getOption;
    (*pRdrGlobalInterface)->m_pFctSetOption    = VideoEditor3gpReader_setOption;
    (*pRdrGlobalInterface)->m_pFctGetNextStream =
        VideoEditor3gpReader_getNextStreamHandler;
    (*pRdrGlobalInterface)->m_pFctFillAuStruct =
        VideoEditor3gpReader_fillAuStruct;
    (*pRdrGlobalInterface)->m_pFctStart        = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctStop         = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctJump         = VideoEditor3gpReader_jump;
    (*pRdrGlobalInterface)->m_pFctReset        = VideoEditor3gpReader_reset;
    (*pRdrGlobalInterface)->m_pFctGetPrevRapTime =
        VideoEditor3gpReader_getPrevRapTime;
    (*pRdrDataInterface)->m_pFctGetNextAu      = VideoEditor3gpReader_getNextAu;
    (*pRdrDataInterface)->m_readerContext      = M4OSA_NULL;

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditor3gpReader_getInterface no error");
    } else {
        SAFE_FREE(*pRdrGlobalInterface);
        SAFE_FREE(*pRdrDataInterface);

        ALOGV("VideoEditor3gpReader_getInterface ERROR 0x%X", err);
    }
    ALOGV("VideoEditor3gpReader_getInterface end");
    return err;
}

}  /* extern "C" */

}  /* namespace android */


