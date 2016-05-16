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
* @file   VideoEditorMp3Reader.cpp
* @brief  StageFright shell MP3 Reader
*************************************************************************
*/
#define LOG_NDEBUG 1
#define LOG_TAG "VIDEOEDITOR_MP3READER"

/**
 * HEADERS
 *
 */
#include "M4OSA_Debug.h"
#include "M4SYS_AccessUnit.h"
#include "VideoEditorMp3Reader.h"
#include "VideoEditorUtils.h"

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
 **************************************************************************
 * structure VideoEditorMp3Reader_Context
 * @brief    This structure defines the context of the SF MP3 reader shell.
 **************************************************************************
 */
typedef struct {
    sp<DataSource>              mDataSource;
    sp<MediaExtractor>          mExtractor;
    sp<MediaSource>             mMediaSource;
    M4_AudioStreamHandler*      mAudioStreamHandler;
    M4SYS_AccessUnit            mAudioAu;
    M4OSA_Time                  mMaxDuration;
    M4OSA_UInt8                 mStreamNumber;
    M4OSA_Bool                  mSeeking;
    M4OSA_Time                  mSeekTime;
    uint32_t                    mExtractorFlags;
} VideoEditorMp3Reader_Context;

/**
 ****************************************************************************
 * @brief    create an instance of the MP3 reader
 * @note     allocates the context
 *
 * @param    pContext:        (OUT)    pointer on a reader context
 *
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_ALLOC                a memory allocation has failed
 * @return    M4ERR_PARAMETER            at least one parameter is not valid
 ****************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_create(M4OSA_Context *pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorMp3Reader_Context *pReaderContext = M4OSA_NULL;

    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    ALOGV("VideoEditorMp3Reader_create begin");

    /* Context allocation & initialization */
    SAFE_MALLOC(pReaderContext, VideoEditorMp3Reader_Context, 1,
        "VideoEditorMp3Reader");

    pReaderContext->mAudioStreamHandler  = M4OSA_NULL;
    pReaderContext->mAudioAu.dataAddress = M4OSA_NULL;
    pReaderContext->mMaxDuration = 0;
    *pContext = pReaderContext;

cleanUp:
    if (M4NO_ERROR == err) {
        ALOGV("VideoEditorMp3Reader_create no error");
    } else {
        ALOGV("VideoEditorMp3Reader_create ERROR 0x%X", err);
    }
    ALOGV("VideoEditorMp3Reader_create end");
    return err;
}

/**
 *******************************************************************************
 * @brief     destroy the instance of the MP3 reader
 * @note      after this call the context is invalid
 * @param     context:        (IN)    Context of the reader
 * @return    M4NO_ERROR                 there is no error
 * @return    M4ERR_PARAMETER            The input parameter is not properly set
 *******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_destroy(M4OSA_Context pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)pContext;

    VIDEOEDITOR_CHECK(M4OSA_NULL != pReaderContext, M4ERR_PARAMETER);
    ALOGV("VideoEditorMp3Reader_destroy begin");

    SAFE_FREE(pReaderContext);
cleanUp:
    if (M4NO_ERROR == err) {
        ALOGV("VideoEditorMp3Reader_destroy no error");
    } else {
        ALOGV("VideoEditorMp3Reader_destroy ERROR 0x%X", err);
    }
    ALOGV("VideoEditorMp3Reader_destroy end");
    return err;
}
/**
 ******************************************************************************
 * @brief    open the reader and initializes its created instance
 * @note    this function opens the MP3 file
 * @param    context:            (IN)    Context of the reader
 * @param    pFileDescriptor:    (IN)    Pointer to proprietary data identifying
 *                                       the media to open

 * @return    M4NO_ERROR                     there is no error
 * @return    M4ERR_PARAMETER                the context is NULL
 * @return    M4ERR_BAD_CONTEXT              provided context is not a valid one
 * @return    M4ERR_UNSUPPORTED_MEDIA_TYPE   the media is DRM protected
 ******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_open(M4OSA_Context context,
        M4OSA_Void* pFileDescriptor){
    VideoEditorMp3Reader_Context *pReaderContext =
    (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditorMp3Reader_open begin");
    /* Check function parameters*/
    M4OSA_DEBUG_IF1((M4OSA_NULL == pReaderContext),  M4ERR_PARAMETER,
        "VideoEditorMp3Reader_open: invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pFileDescriptor), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_open: invalid pointer pFileDescriptor");

    ALOGV("VideoEditorMp3Reader_open Datasource start %s",
        (char*)pFileDescriptor);
    pReaderContext->mDataSource = new FileSource ((char*)pFileDescriptor);
    ALOGV("VideoEditorMp3Reader_open Datasource end");

    if (pReaderContext->mDataSource == NULL) {
        ALOGV("VideoEditorMp3Reader_open Datasource error");
        return UNKNOWN_ERROR;
    }

    ALOGV("VideoEditorMp3Reader_open extractor start");
    pReaderContext->mExtractor = MediaExtractor::Create(
        pReaderContext->mDataSource,MEDIA_MIMETYPE_AUDIO_MPEG);
    ALOGV("VideoEditorMp3Reader_open extractor end");

    if (pReaderContext->mExtractor == NULL)    {
        ALOGV("VideoEditorMp3Reader_open extractor error");
        return UNKNOWN_ERROR;
    }
    pReaderContext->mStreamNumber = 0;

    int32_t isDRMProtected = 0;
    sp<MetaData> meta = pReaderContext->mExtractor->getMetaData();
    meta->findInt32(kKeyIsDRM, &isDRMProtected);
    if (isDRMProtected) {
        ALOGV("VideoEditorMp3Reader_open error - DRM Protected");
        return M4ERR_UNSUPPORTED_MEDIA_TYPE;
    }

    ALOGV("VideoEditorMp3Reader_open end");
    return err;
}
/**
 **************************************************************************
 * @brief    close the reader
 * @note    this function closes the MP3 reader
 * @param    context:        (IN)      Context of the reader
 * @return    M4NO_ERROR               there is no error
 * @return    M4ERR_PARAMETER          the context is NULL
 **************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_close(M4OSA_Context context) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditorMp3Reader_close begin");
    /* Check function parameters */
    M4OSA_DEBUG_IF1((M4OSA_NULL == pReaderContext), M4ERR_PARAMETER,
            "VideoEditorMp3Reader_close: invalid context pointer");

    if (pReaderContext->mAudioStreamHandler != NULL) {
        if (M4OSA_NULL != pReaderContext->mAudioStreamHandler->\
        m_basicProperties.m_pDecoderSpecificInfo) {
            free(pReaderContext->mAudioStreamHandler->\
                m_basicProperties.m_pDecoderSpecificInfo);
            pReaderContext->mAudioStreamHandler->m_basicProperties.\
                m_decoderSpecificInfoSize = 0;
            pReaderContext->mAudioStreamHandler->m_basicProperties.\
                m_pDecoderSpecificInfo = M4OSA_NULL;
        }

        /* Finally destroy the stream handler */
        free(pReaderContext->mAudioStreamHandler);
        pReaderContext->mAudioStreamHandler = M4OSA_NULL;

        if (pReaderContext->mAudioAu.dataAddress != NULL) {
            free(pReaderContext->mAudioAu.dataAddress);
            pReaderContext->mAudioAu.dataAddress = NULL;
        }
    }

    pReaderContext->mMediaSource->stop();
    pReaderContext->mMediaSource.clear();
    pReaderContext->mExtractor.clear();
    pReaderContext->mDataSource.clear();

    ALOGV("VideoEditorMp3Reader_close end ");
    return err;
}
/**
 ******************************************************************************
 * @brief    get an option value from the reader
 * @note
 *          it allows the caller to retrieve a property value:
 *
 * @param    context:        (IN)    Context of the reader
 * @param    optionId:       (IN)    indicates the option to get
 * @param    pValue:         (OUT)   pointer to structure or value (allocated
 *                                   by user) where option is stored
 *
 * @return    M4NO_ERROR             there is no error
 * @return    M4ERR_PARAMETER        at least one parameter is not properly set
 * @return    M4ERR_BAD_OPTION_ID    when the option ID is not a valid one
 ******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_getOption(M4OSA_Context context,
          M4OSA_OptionID optionId, M4OSA_DataOption pValue) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditorMp3Reader_getOption begin: optionId= %d ",(int)optionId);

    M4OSA_DEBUG_IF1((M4OSA_NULL == pReaderContext), M4ERR_PARAMETER,
        "invalid value pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER,
        "invalid value pointer");

    switch(optionId) {
    case M4READER_kOptionID_Duration:
        {
            ALOGV("Mp3Reader duration=%ld",pReaderContext->mMaxDuration);
            *(M4OSA_Time*)pValue = pReaderContext->mMaxDuration;
        }
        break;

    case M4READER_kOptionID_Bitrate:
        {
            M4OSA_UInt32* pBitrate = (M4OSA_UInt32*)pValue;
            if (M4OSA_NULL != pReaderContext->mAudioStreamHandler) {
                *pBitrate = pReaderContext->mAudioStreamHandler->\
                    m_basicProperties.m_averageBitRate;
            } else {
                pBitrate = 0;
                err = M4ERR_PARAMETER;
            }
        }
        break;

    case M4READER_kOptionID_Mp3Id3v1Tag:
        break;

    case M4READER_kOptionID_Mp3Id3v2Tag:
        break;

    case M4READER_kOptionID_GetMetadata:
        break;

    default :
        {
            ALOGV("VideoEditorMp3Reader_getOption:  M4ERR_BAD_OPTION_ID");
            err = M4ERR_BAD_OPTION_ID;
        }
    }
    ALOGV("VideoEditorMp3Reader_getOption end ");
    return err;
}
/**
 ******************************************************************************
 * @brief   set an option value of the reader
 * @note
 *          it allows the caller to set a property value:
 *
 * @param   context:    (IN)        Context of the reader
 * @param   optionId:   (IN)        Identifier indicating the option to set
 * @param   pValue:     (IN)        Pointer to structure or value (allocated
 *                                  by user) where option is stored
 *
 * @return  M4NO_ERROR              There is no error
 * @return  M4ERR_BAD_OPTION_ID     The option ID is not a valid one
 * @return  M4ERR_STATE             State automaton is not applied
 * @return  M4ERR_PARAMETER         The option parameter is invalid
 ******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_setOption(M4OSA_Context context,
        M4OSA_OptionID optionId, M4OSA_DataOption pValue) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditorMp3Reader_Context begin: optionId: %d Value: %d ",
        (int)optionId,(int)pValue);

    M4OSA_DEBUG_IF1((M4OSA_NULL == pReaderContext), M4ERR_PARAMETER,
        "invalid context pointer");
    M4OSA_DEBUG_IF1((M4OSA_NULL == pValue), M4ERR_PARAMETER,
        "invalid value pointer");

    switch(optionId) {
        case M4READER_kOptionID_SetOsaFileReaderFctsPtr:
        default :
        {
            err = M4NO_ERROR;
        }
    }
    ALOGV("VideoEditorMp3Reader_Context end ");
    return err;
}
/**
 ******************************************************************************
 * @brief    jump into the stream at the specified time
 * @note
 * @param    context:      (IN)   Context of the reader
 * @param    pStreamHandler(IN)   stream description of the stream to make jump
 * @param    pTime         (I/O)IN:the time to jump to (in ms)
 *                              OUT: the time to which the stream really jumped
 * @return    M4NO_ERROR           there is no error
 * @return    M4ERR_PARAMETER      at least one parameter is not properly set
 ******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_jump(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4OSA_Int32* pTime) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4SYS_StreamID streamIdArray[2];
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_AccessUnit* pAu;
    M4OSA_Time time64 = (M4OSA_Time)*pTime;

    ALOGV("VideoEditorMp3Reader_jump begin");
    M4OSA_DEBUG_IF1((pReaderContext == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_jump: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_jump: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pTime == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_jump: invalid time pointer");

    if(pStreamHandler == (M4_StreamHandler*)pReaderContext->\
        mAudioStreamHandler){
        pAu = &pReaderContext->mAudioAu;
    } else {
        ALOGV("VideoEditorMp3Reader_jump: passed StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;

    ALOGV("VideoEditorMp3Reader_jump time ms %ld ", time64);

    pAu->CTS = time64;
    pAu->DTS = time64;

    time64 = time64 * 1000; /* Convert the time into micro sec */
    ALOGV("VideoEditorMp3Reader_jump time us %ld ", time64);

    pReaderContext->mSeeking = M4OSA_TRUE;
    pReaderContext->mSeekTime = time64;

    time64 = time64 / 1000; /* Convert the time into milli sec */
    *pTime = (M4OSA_Int32)time64;
    ALOGV("VideoEditorMp3Reader_jump end ");
    return err;
}
/**
 *******************************************************************************
 * @brief   Get the next stream found in the media file
 *
 * @param    context:        (IN)  Context of the reader
 * @param    pMediaFamily:   (OUT) pointer to a user allocated
 *                                 M4READER_MediaFamily that will be filled with
 *                                 the media family of the found stream
 * @param    pStreamHandler: (OUT) pointer to a stream handler that will be
 *                                 allocated and filled with stream description
 *
 * @return    M4NO_ERROR             there is no error
 * @return    M4WAR_NO_MORE_STREAM   no more available stream in the media
 * @return    M4ERR_PARAMETER        at least one parameter is not properly set
 *******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_getNextStream(M4OSA_Context context,
        M4READER_MediaFamily *pMediaFamily,
        M4_StreamHandler **pStreamHandlerParam) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_StreamID streamIdArray[2];
    M4SYS_StreamDescription streamDesc;
    M4_AudioStreamHandler* pAudioStreamHandler;
    M4_StreamHandler* pStreamHandler;
    M4OSA_UInt8 type, temp;
    M4OSA_Bool haveAudio = M4OSA_FALSE;
    sp<MetaData> meta = NULL;
    int64_t Duration;

    ALOGV("VideoEditorMp3Reader_getNextStream begin");
    M4OSA_DEBUG_IF1((pReaderContext == 0),      M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextStream: invalid context");
    M4OSA_DEBUG_IF1((pMediaFamily == 0),        M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextStream: invalid pointer to MediaFamily");
    M4OSA_DEBUG_IF1((pStreamHandlerParam == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextStream: invalid pointer to StreamHandler");

    ALOGV("VideoEditorMp3Reader_getNextStream stream number = %d",
        pReaderContext->mStreamNumber);
    if (pReaderContext->mStreamNumber >= 1) {
        ALOGV("VideoEditorMp3Reader_getNextStream max number of stream reached");
        return M4WAR_NO_MORE_STREAM;
    }
    pReaderContext->mStreamNumber = pReaderContext->mStreamNumber + 1;
    ALOGV("VideoEditorMp3Reader_getNextStream number of Tracks%d",
        pReaderContext->mExtractor->countTracks());
    for (temp = 0; temp < pReaderContext->mExtractor->countTracks(); temp++) {
        meta = pReaderContext->mExtractor->getTrackMetaData(temp);
        const char *mime;
        CHECK(meta->findCString(kKeyMIMEType, &mime));

        if (!haveAudio && !strncasecmp(mime, "audio/", 6)) {
            pReaderContext->mMediaSource =
                pReaderContext->mExtractor->getTrack(temp);
            pReaderContext->mMediaSource->start();
            haveAudio = true;
        }

        if (haveAudio) {
            break;
        }
    }

    if (!haveAudio) {
        ALOGV("VideoEditorMp3Reader_getNextStream no more stream ");
        pReaderContext->mDataSource.clear();
        return M4WAR_NO_MORE_STREAM;
    }

    pReaderContext->mExtractorFlags = pReaderContext->mExtractor->flags();
    *pMediaFamily = M4READER_kMediaFamilyAudio;

    streamDesc.duration = meta->findInt64(kKeyDuration, &Duration);
    streamDesc.duration = (M4OSA_Time)Duration/1000;

    meta->findInt32(kKeyBitRate, (int32_t*)&streamDesc.averageBitrate);
    meta->findInt32(kKeySampleRate, (int32_t*)&streamDesc.timeScale);
    ALOGV("Bitrate = %d, SampleRate = %d duration = %lld",
        streamDesc.averageBitrate,streamDesc.timeScale,Duration/1000);

    streamDesc.streamType = M4SYS_kMP3;
    streamDesc.profileLevel = 0xFF ;
    streamDesc.streamID = pReaderContext->mStreamNumber;
    streamDesc.decoderSpecificInfo = M4OSA_NULL;
    streamDesc.decoderSpecificInfoSize = 0;
    streamDesc.maxBitrate = streamDesc.averageBitrate;

    /*    Allocate the audio stream handler and set its parameters    */
    pAudioStreamHandler = (M4_AudioStreamHandler*)M4OSA_32bitAlignedMalloc(
        sizeof(M4_AudioStreamHandler), M4READER_MP3,
        (M4OSA_Char*)"M4_AudioStreamHandler");

    if (pAudioStreamHandler == M4OSA_NULL) {
        ALOGV("VideoEditorMp3Reader_getNextStream malloc failed");
        pReaderContext->mMediaSource->stop();
        pReaderContext->mMediaSource.clear();
        pReaderContext->mDataSource.clear();

        return M4ERR_ALLOC;
    }
    pStreamHandler =(M4_StreamHandler*)(pAudioStreamHandler);
    *pStreamHandlerParam = pStreamHandler;
    pReaderContext->mAudioStreamHandler = pAudioStreamHandler;

    pAudioStreamHandler->m_structSize = sizeof(M4_AudioStreamHandler);

    if (meta == NULL) {
        ALOGV("VideoEditorMp3Reader_getNextStream meta is NULL");
    }

    pAudioStreamHandler->m_samplingFrequency = streamDesc.timeScale;
    pStreamHandler->m_pDecoderSpecificInfo =
        (M4OSA_UInt8*)(streamDesc.decoderSpecificInfo);
    pStreamHandler->m_decoderSpecificInfoSize =
        streamDesc.decoderSpecificInfoSize;

    meta->findInt32(kKeyChannelCount,
        (int32_t*)&pAudioStreamHandler->m_nbChannels);
    pAudioStreamHandler->m_byteFrameLength = 1152;
    pAudioStreamHandler->m_byteSampleSize = 2;

    pStreamHandler->m_pUserData = NULL;
    pStreamHandler->m_streamId = streamDesc.streamID;
    pStreamHandler->m_duration = streamDesc.duration;
    pReaderContext->mMaxDuration = streamDesc.duration;
    pStreamHandler->m_averageBitRate = streamDesc.averageBitrate;

    pStreamHandler->m_maxAUSize = 0;
    pStreamHandler->m_streamType = M4DA_StreamTypeAudioMp3;

    ALOGV("VideoEditorMp3Reader_getNextStream end ");
    return err;
}

/**
 *******************************************************************************
 * @brief    fill the access unit structure with initialization values
 * @param    context:        (IN)     Context of the reader
 * @param    pStreamHandler: (IN)     pointer to the stream handler to which
 *                                    the access unit will be associated
 * @param    pAccessUnit:    (IN/OUT) pointer to the access unit (allocated by
 *                                    the caller) to initialize
 * @return   M4NO_ERROR               there is no error
 * @return   M4ERR_PARAMETER          at least one parameter is not properly set
 *******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_fillAuStruct(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4_AccessUnit *pAccessUnit) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4SYS_AccessUnit *pAu;

    M4OSA_DEBUG_IF1((pReaderContext == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_fillAuStruct: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_fillAuStruct invalid pointer to StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
        "VideoEditorMp3Reader_fillAuStruct: invalid pointer to M4_AccessUnit");

    ALOGV("VideoEditorMp3Reader_fillAuStruct start ");
    if(pStreamHandler == (M4_StreamHandler*)pReaderContext->\
        mAudioStreamHandler){
        pAu = &pReaderContext->mAudioAu;
    } else {
        ALOGV("VideoEditorMp3Reader_fillAuStruct StreamHandler is not known");
        return M4ERR_PARAMETER;
    }

    /* Initialize pAu structure */
    pAu->dataAddress = M4OSA_NULL;
    pAu->size        = 0;
    pAu->CTS         = 0;
    pAu->DTS         = 0;
    pAu->attribute   = 0;
    pAu->nbFrag      = 0;

    /* Initialize pAccessUnit structure */
    pAccessUnit->m_size         = 0;
    pAccessUnit->m_CTS          = 0;
    pAccessUnit->m_DTS          = 0;
    pAccessUnit->m_attribute    = 0;
    pAccessUnit->m_dataAddress  = M4OSA_NULL;
    pAccessUnit->m_maxsize      = pStreamHandler->m_maxAUSize;
    pAccessUnit->m_streamID     = pStreamHandler->m_streamId;
    pAccessUnit->m_structSize   = sizeof(M4_AccessUnit);

    ALOGV("VideoEditorMp3Reader_fillAuStruct end");
    return M4NO_ERROR;
}

/**
 *******************************************************************************
 * @brief    reset the stream, i.e seek it to the beginning
 * @note
 * @param     context:          (IN)  Context of the reader
 * @param     pStreamHandler    (IN)  The stream handler of the stream to reset
 * @return    M4NO_ERROR              there is no error
 * @return    M4ERR_PARAMETER         at least one parameter is not properly set
 *******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_reset(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;

    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_StreamID streamIdArray[2];
    M4SYS_AccessUnit* pAu;
    M4OSA_Time time64 = 0;

    ALOGV("VideoEditorMp3Reader_reset start");
    M4OSA_DEBUG_IF1((pReaderContext == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_reset: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_reset: invalid pointer to M4_StreamHandler");

    if (pStreamHandler == (M4_StreamHandler*)pReaderContext->\
        mAudioStreamHandler) {
        pAu = &pReaderContext->mAudioAu;
    } else {
        ALOGV("VideoEditorMp3Reader_reset StreamHandler is not known");
        return M4ERR_PARAMETER;
    }
    streamIdArray[0] = pStreamHandler->m_streamId;
    streamIdArray[1] = 0;
    pAu->CTS = time64;
    pAu->DTS = time64;

    pReaderContext->mSeeking = M4OSA_TRUE;
    pReaderContext->mSeekTime = time64;

    ALOGV("VideoEditorMp3Reader_reset end");
    return err;
}
/**
 *******************************************************************************
 * @brief   Gets an access unit (AU) from the stream handler source.
 * @note    AU is the smallest possible amount of data to be decoded by decoder
 *
 * @param   context:       (IN) Context of the reader
 * @param   pStreamHandler (IN) The stream handler of the stream to make jump
 * @param   pAccessUnit    (I/O)Pointer to an access unit to fill with read data
 * @return    M4NO_ERROR        there is no error
 * @return    M4ERR_PARAMETER   at least one parameter is not properly set
 * @returns   M4ERR_ALLOC       memory allocation failed
 * @returns   M4WAR_NO_MORE_AU  there are no more access unit in the stream
 *******************************************************************************
*/
M4OSA_ERR VideoEditorMp3Reader_getNextAu(M4OSA_Context context,
        M4_StreamHandler *pStreamHandler, M4_AccessUnit *pAccessUnit) {
    VideoEditorMp3Reader_Context *pReaderContext =
        (VideoEditorMp3Reader_Context*)context;
    M4OSA_ERR err = M4NO_ERROR;
    M4SYS_AccessUnit* pAu;
    MediaBuffer *mAudioBuffer;
    MediaSource::ReadOptions options;

    ALOGV("VideoEditorMp3Reader_getNextAu start");
    M4OSA_DEBUG_IF1((pReaderContext == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextAu: invalid context");
    M4OSA_DEBUG_IF1((pStreamHandler == 0), M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextAu: invalid pointer to M4_StreamHandler");
    M4OSA_DEBUG_IF1((pAccessUnit == 0),    M4ERR_PARAMETER,
        "VideoEditorMp3Reader_getNextAu: invalid pointer to M4_AccessUnit");

    if (pStreamHandler == (M4_StreamHandler*)pReaderContext->\
        mAudioStreamHandler) {
        pAu = &pReaderContext->mAudioAu;
    } else {
        ALOGV("VideoEditorMp3Reader_getNextAu: StreamHandler is not known\n");
        return M4ERR_PARAMETER;
    }

    if (pReaderContext->mSeeking) {
        options.setSeekTo(pReaderContext->mSeekTime);
    }

    pReaderContext->mMediaSource->read(&mAudioBuffer, &options);

    if (mAudioBuffer != NULL) {
        if ((pAu->dataAddress == NULL) ||
            (pAu->size < mAudioBuffer->range_length())) {
            if (pAu->dataAddress != NULL) {
                free((M4OSA_Int32*)pAu->dataAddress);
                pAu->dataAddress = NULL;
            }
            pAu->dataAddress = (M4OSA_Int32*)M4OSA_32bitAlignedMalloc(
                (mAudioBuffer->range_length() + 3) & ~0x3,
                M4READER_MP3, (M4OSA_Char*)"pAccessUnit->m_dataAddress" );

            if (pAu->dataAddress == NULL) {
                ALOGV("VideoEditorMp3Reader_getNextAu malloc failed");
                pReaderContext->mMediaSource->stop();
                pReaderContext->mMediaSource.clear();
                pReaderContext->mDataSource.clear();

                return M4ERR_ALLOC;
            }
        }
        pAu->size = mAudioBuffer->range_length();
        memcpy((M4OSA_MemAddr8)pAu->dataAddress,
            (const char *)mAudioBuffer->data() + mAudioBuffer->range_offset(),
            mAudioBuffer->range_length());

        mAudioBuffer->meta_data()->findInt64(kKeyTime, (int64_t*)&pAu->CTS);


        pAu->CTS = pAu->CTS / 1000; /*converting the microsec to millisec */
        pAu->DTS  = pAu->CTS;
        pAu->attribute = M4SYS_kFragAttrOk;
        mAudioBuffer->release();

        ALOGV("VideoEditorMp3Reader_getNextAu AU CTS = %ld",pAu->CTS);

        pAccessUnit->m_dataAddress = (M4OSA_Int8*) pAu->dataAddress;
        pAccessUnit->m_size = pAu->size;
        pAccessUnit->m_CTS = pAu->CTS;
        pAccessUnit->m_DTS = pAu->DTS;
        pAccessUnit->m_attribute = pAu->attribute;
    } else {
        ALOGV("VideoEditorMp3Reader_getNextAu EOS reached.");
        pAccessUnit->m_size=0;
        err = M4WAR_NO_MORE_AU;
    }
    pAu->nbFrag = 0;

    options.clearSeekTo();
    pReaderContext->mSeeking = M4OSA_FALSE;
    mAudioBuffer = NULL;
    ALOGV("VideoEditorMp3Reader_getNextAu end");

    return err;
}

extern "C" {

M4OSA_ERR VideoEditorMp3Reader_getInterface(
        M4READER_MediaType *pMediaType,
        M4READER_GlobalInterface **pRdrGlobalInterface,
        M4READER_DataInterface **pRdrDataInterface) {
    M4OSA_ERR err = M4NO_ERROR;

    ALOGV("VideoEditorMp3Reader_getInterface: begin");
    /* Input parameters check */
    VIDEOEDITOR_CHECK(M4OSA_NULL != pMediaType,      M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pRdrGlobalInterface, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pRdrDataInterface, M4ERR_PARAMETER);

    SAFE_MALLOC(*pRdrGlobalInterface, M4READER_GlobalInterface, 1,
        "VideoEditorMp3Reader_getInterface");
    SAFE_MALLOC(*pRdrDataInterface, M4READER_DataInterface, 1,
        "VideoEditorMp3Reader_getInterface");

    *pMediaType = M4READER_kMediaTypeMP3;

    (*pRdrGlobalInterface)->m_pFctCreate       = VideoEditorMp3Reader_create;
    (*pRdrGlobalInterface)->m_pFctDestroy      = VideoEditorMp3Reader_destroy;
    (*pRdrGlobalInterface)->m_pFctOpen         = VideoEditorMp3Reader_open;
    (*pRdrGlobalInterface)->m_pFctClose        = VideoEditorMp3Reader_close;
    (*pRdrGlobalInterface)->m_pFctGetOption    = VideoEditorMp3Reader_getOption;
    (*pRdrGlobalInterface)->m_pFctSetOption    = VideoEditorMp3Reader_setOption;
    (*pRdrGlobalInterface)->m_pFctGetNextStream =
        VideoEditorMp3Reader_getNextStream;
    (*pRdrGlobalInterface)->m_pFctFillAuStruct =
        VideoEditorMp3Reader_fillAuStruct;
    (*pRdrGlobalInterface)->m_pFctStart        = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctStop         = M4OSA_NULL;
    (*pRdrGlobalInterface)->m_pFctJump         = VideoEditorMp3Reader_jump;
    (*pRdrGlobalInterface)->m_pFctReset        = VideoEditorMp3Reader_reset;
    (*pRdrGlobalInterface)->m_pFctGetPrevRapTime = M4OSA_NULL;

    (*pRdrDataInterface)->m_pFctGetNextAu      = VideoEditorMp3Reader_getNextAu;
    (*pRdrDataInterface)->m_readerContext      = M4OSA_NULL;

cleanUp:
    if( M4NO_ERROR == err )
    {
        ALOGV("VideoEditorMp3Reader_getInterface no error");
    }
    else
    {
        SAFE_FREE(*pRdrGlobalInterface);
        SAFE_FREE(*pRdrDataInterface);

        ALOGV("VideoEditorMp3Reader_getInterface ERROR 0x%X", err);
    }
    ALOGV("VideoEditorMp3Reader_getInterface: end");
    return err;
}
}  /* extern "C" */
}  /* namespace android */
