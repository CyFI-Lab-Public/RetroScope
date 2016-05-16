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
* @file   VideoEditorVideoEncoder.cpp
* @brief  StageFright shell video encoder
*************************************************************************
*/
#define LOG_NDEBUG 1
#define LOG_TAG "VIDEOEDITOR_VIDEOENCODER"

/*******************
 *     HEADERS     *
 *******************/
#include "M4OSA_Debug.h"
#include "M4SYS_AccessUnit.h"
#include "VideoEditorVideoEncoder.h"
#include "VideoEditorUtils.h"
#include "MediaBufferPuller.h"
#include <I420ColorConverter.h>

#include <unistd.h>
#include "utils/Log.h"
#include "utils/Vector.h"
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/OMXCodec.h>
#include <media/MediaProfiles.h>
#include "OMX_Video.h"

/********************
 *   DEFINITIONS    *
 ********************/

// Force using hardware encoder
#define VIDEOEDITOR_FORCECODEC kHardwareCodecsOnly

#if !defined(VIDEOEDITOR_FORCECODEC)
    #error "Cannot force DSI retrieval if codec type is not fixed"
#endif

/********************
 *   SOURCE CLASS   *
 ********************/

namespace android {

struct VideoEditorVideoEncoderSource : public MediaSource {
    public:
        static sp<VideoEditorVideoEncoderSource> Create(
            const sp<MetaData> &format);
        virtual status_t start(MetaData *params = NULL);
        virtual status_t stop();
        virtual sp<MetaData> getFormat();
        virtual status_t read(MediaBuffer **buffer,
            const ReadOptions *options = NULL);
        virtual int32_t storeBuffer(MediaBuffer *buffer);
        virtual int32_t getNumberOfBuffersInQueue();

    protected:
        virtual ~VideoEditorVideoEncoderSource();

    private:
        struct MediaBufferChain {
            MediaBuffer* buffer;
            MediaBufferChain* nextLink;
        };
        enum State {
            CREATED,
            STARTED,
            ERROR
        };
        VideoEditorVideoEncoderSource(const sp<MetaData> &format);

        // Don't call me
        VideoEditorVideoEncoderSource(const VideoEditorVideoEncoderSource &);
        VideoEditorVideoEncoderSource &operator=(
                const VideoEditorVideoEncoderSource &);

        MediaBufferChain* mFirstBufferLink;
        MediaBufferChain* mLastBufferLink;
        int32_t           mNbBuffer;
        bool              mIsEOS;
        State             mState;
        sp<MetaData>      mEncFormat;
        Mutex             mLock;
        Condition         mBufferCond;
};

sp<VideoEditorVideoEncoderSource> VideoEditorVideoEncoderSource::Create(
    const sp<MetaData> &format) {

    sp<VideoEditorVideoEncoderSource> aSource =
        new VideoEditorVideoEncoderSource(format);
    return aSource;
}

VideoEditorVideoEncoderSource::VideoEditorVideoEncoderSource(
    const sp<MetaData> &format):
        mFirstBufferLink(NULL),
        mLastBufferLink(NULL),
        mNbBuffer(0),
        mIsEOS(false),
        mState(CREATED),
        mEncFormat(format) {
    ALOGV("VideoEditorVideoEncoderSource::VideoEditorVideoEncoderSource");
}

VideoEditorVideoEncoderSource::~VideoEditorVideoEncoderSource() {

    // Safety clean up
    if( STARTED == mState ) {
        stop();
    }
}

status_t VideoEditorVideoEncoderSource::start(MetaData *meta) {
    status_t err = OK;

    ALOGV("VideoEditorVideoEncoderSource::start() begin");

    if( CREATED != mState ) {
        ALOGV("VideoEditorVideoEncoderSource::start: invalid state %d", mState);
        return UNKNOWN_ERROR;
    }
    mState = STARTED;

    ALOGV("VideoEditorVideoEncoderSource::start() END (0x%x)", err);
    return err;
}

status_t VideoEditorVideoEncoderSource::stop() {
    status_t err = OK;

    ALOGV("VideoEditorVideoEncoderSource::stop() begin");

    if( STARTED != mState ) {
        ALOGV("VideoEditorVideoEncoderSource::stop: invalid state %d", mState);
        return UNKNOWN_ERROR;
    }

    // Release the buffer chain
    int32_t i = 0;
    MediaBufferChain* tmpLink = NULL;
    while( mFirstBufferLink ) {
        i++;
        tmpLink = mFirstBufferLink;
        mFirstBufferLink = mFirstBufferLink->nextLink;
        delete tmpLink;
    }
    ALOGV("VideoEditorVideoEncoderSource::stop : %d buffer remained", i);
    mFirstBufferLink = NULL;
    mLastBufferLink = NULL;

    mState = CREATED;

    ALOGV("VideoEditorVideoEncoderSource::stop() END (0x%x)", err);
    return err;
}

sp<MetaData> VideoEditorVideoEncoderSource::getFormat() {

    ALOGV("VideoEditorVideoEncoderSource::getFormat");
    return mEncFormat;
}

status_t VideoEditorVideoEncoderSource::read(MediaBuffer **buffer,
        const ReadOptions *options) {
    Mutex::Autolock autolock(mLock);
    MediaSource::ReadOptions readOptions;
    status_t err = OK;
    MediaBufferChain* tmpLink = NULL;

    ALOGV("VideoEditorVideoEncoderSource::read() begin");

    if ( STARTED != mState ) {
        ALOGV("VideoEditorVideoEncoderSource::read: invalid state %d", mState);
        return UNKNOWN_ERROR;
    }

    while (mFirstBufferLink == NULL && !mIsEOS) {
        mBufferCond.wait(mLock);
    }

    // End of stream?
    if (mFirstBufferLink == NULL) {
        *buffer = NULL;
        ALOGV("VideoEditorVideoEncoderSource::read : EOS");
        return ERROR_END_OF_STREAM;
    }

    // Get a buffer from the chain
    *buffer = mFirstBufferLink->buffer;
    tmpLink = mFirstBufferLink;
    mFirstBufferLink = mFirstBufferLink->nextLink;

    if ( NULL == mFirstBufferLink ) {
        mLastBufferLink = NULL;
    }
    delete tmpLink;
    mNbBuffer--;

    ALOGV("VideoEditorVideoEncoderSource::read() END (0x%x)", err);
    return err;
}

int32_t VideoEditorVideoEncoderSource::storeBuffer(MediaBuffer *buffer) {
    Mutex::Autolock autolock(mLock);
    status_t err = OK;

    ALOGV("VideoEditorVideoEncoderSource::storeBuffer() begin");

    if( NULL == buffer ) {
        ALOGV("VideoEditorVideoEncoderSource::storeBuffer : reached EOS");
        mIsEOS = true;
    } else {
        MediaBufferChain* newLink = new MediaBufferChain;
        newLink->buffer = buffer;
        newLink->nextLink = NULL;
        if( NULL != mLastBufferLink ) {
            mLastBufferLink->nextLink = newLink;
        } else {
            mFirstBufferLink = newLink;
        }
        mLastBufferLink = newLink;
        mNbBuffer++;
    }
    mBufferCond.signal();
    ALOGV("VideoEditorVideoEncoderSource::storeBuffer() end");
    return mNbBuffer;
}

int32_t VideoEditorVideoEncoderSource::getNumberOfBuffersInQueue() {
    Mutex::Autolock autolock(mLock);
    return mNbBuffer;
}

/**
 ******************************************************************************
 * structure VideoEditorVideoEncoder_Context
 * @brief    This structure defines the context of the StageFright video encoder
 *           shell
 ******************************************************************************
*/
typedef enum {
    CREATED   = 0x1,
    OPENED    = 0x2,
    STARTED   = 0x4,
    BUFFERING = 0x8,
    READING   = 0x10
} VideoEditorVideoEncoder_State;

typedef struct {
    VideoEditorVideoEncoder_State     mState;
    M4ENCODER_Format                  mFormat;
    M4WRITER_DataInterface*           mWriterDataInterface;
    M4VPP_apply_fct*                  mPreProcFunction;
    M4VPP_Context                     mPreProcContext;
    M4SYS_AccessUnit*                 mAccessUnit;
    M4ENCODER_Params*                 mCodecParams;
    M4ENCODER_Header                  mHeader;
    H264MCS_ProcessEncodedNALU_fct*   mH264NALUPostProcessFct;
    M4OSA_Context                     mH264NALUPostProcessCtx;
    M4OSA_UInt32                      mLastCTS;
    sp<VideoEditorVideoEncoderSource> mEncoderSource;
    OMXClient                         mClient;
    sp<MediaSource>                   mEncoder;
    OMX_COLOR_FORMATTYPE              mEncoderColorFormat;
    MediaBufferPuller*                mPuller;
    I420ColorConverter*               mI420ColorConverter;

    uint32_t                          mNbInputFrames;
    double                            mFirstInputCts;
    double                            mLastInputCts;
    uint32_t                          mNbOutputFrames;
    int64_t                           mFirstOutputCts;
    int64_t                           mLastOutputCts;

    MediaProfiles *mVideoEditorProfile;
    int32_t mMaxPrefetchFrames;
} VideoEditorVideoEncoder_Context;

/********************
 *      TOOLS       *
 ********************/

M4OSA_ERR VideoEditorVideoEncoder_getDSI(M4ENCODER_Context pContext,
        sp<MetaData> metaData) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context*  pEncoderContext = M4OSA_NULL;
    status_t result = OK;
    int32_t nbBuffer = 0;
    int32_t stride = 0;
    int32_t height = 0;
    int32_t framerate = 0;
    int32_t isCodecConfig = 0;
    size_t size = 0;
    uint32_t codecFlags = 0;
    MediaBuffer* inputBuffer = NULL;
    MediaBuffer* outputBuffer = NULL;
    sp<VideoEditorVideoEncoderSource> encoderSource = NULL;
    sp<MediaSource> encoder = NULL;;
    OMXClient client;

    ALOGV("VideoEditorVideoEncoder_getDSI begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext,       M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != metaData.get(), M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    VIDEOEDITOR_CHECK(CREATED == pEncoderContext->mState, M4ERR_STATE);

    // Create the encoder source
    encoderSource = VideoEditorVideoEncoderSource::Create(metaData);
    VIDEOEDITOR_CHECK(NULL != encoderSource.get(), M4ERR_STATE);

    // Connect to the OMX client
    result = client.connect();
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);

    // Create the OMX codec
    // VIDEOEDITOR_FORCECODEC MUST be defined here
    codecFlags |= OMXCodec::VIDEOEDITOR_FORCECODEC;
    encoder = OMXCodec::Create(client.interface(), metaData, true,
        encoderSource, NULL, codecFlags);
    VIDEOEDITOR_CHECK(NULL != encoder.get(), M4ERR_STATE);

    /**
     * Send fake frames and retrieve the DSI
     */
    // Send a fake frame to the source
    metaData->findInt32(kKeyStride,     &stride);
    metaData->findInt32(kKeyHeight,     &height);
    metaData->findInt32(kKeySampleRate, &framerate);
    size = (size_t)(stride*height*3)/2;
    inputBuffer = new MediaBuffer(size);
    inputBuffer->meta_data()->setInt64(kKeyTime, 0);
    nbBuffer = encoderSource->storeBuffer(inputBuffer);
    encoderSource->storeBuffer(NULL); // Signal EOS

    // Call read once to get the DSI
    result = encoder->start();;
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);
    result = encoder->read(&outputBuffer, NULL);
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);
    VIDEOEDITOR_CHECK(outputBuffer->meta_data()->findInt32(
        kKeyIsCodecConfig, &isCodecConfig) && isCodecConfig, M4ERR_STATE);

    VIDEOEDITOR_CHECK(M4OSA_NULL == pEncoderContext->mHeader.pBuf, M4ERR_STATE);
    if ( M4ENCODER_kH264 == pEncoderContext->mFormat ) {
        // For H264, format the DSI
        result = buildAVCCodecSpecificData(
            (uint8_t**)(&(pEncoderContext->mHeader.pBuf)),
            (size_t*)(&(pEncoderContext->mHeader.Size)),
            (const uint8_t*)outputBuffer->data() + outputBuffer->range_offset(),
            outputBuffer->range_length(), encoder->getFormat().get());
        outputBuffer->release();
        VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);
    } else {
        // For MPEG4, just copy the DSI
        pEncoderContext->mHeader.Size =
            (M4OSA_UInt32)outputBuffer->range_length();
        SAFE_MALLOC(pEncoderContext->mHeader.pBuf, M4OSA_Int8,
            pEncoderContext->mHeader.Size, "Encoder header");
        memcpy((void *)pEncoderContext->mHeader.pBuf,
            (void *)((M4OSA_MemAddr8)(outputBuffer->data())+outputBuffer->range_offset()),
            pEncoderContext->mHeader.Size);
        outputBuffer->release();
    }

    result = encoder->stop();
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);

cleanUp:
    // Destroy the graph
    if ( encoder != NULL ) { encoder.clear(); }
    client.disconnect();
    if ( encoderSource != NULL ) { encoderSource.clear(); }
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_getDSI no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_getDSI ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_getDSI end");
    return err;
}
/********************
 * ENGINE INTERFACE *
 ********************/

M4OSA_ERR VideoEditorVideoEncoder_cleanup(M4ENCODER_Context pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;

    ALOGV("VideoEditorVideoEncoder_cleanup begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    VIDEOEDITOR_CHECK(CREATED == pEncoderContext->mState, M4ERR_STATE);

    // Release memory
    SAFE_FREE(pEncoderContext->mHeader.pBuf);
    SAFE_FREE(pEncoderContext);
    pContext = M4OSA_NULL;

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_cleanup no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_cleanup ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_cleanup end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_init(M4ENCODER_Format format,
        M4ENCODER_Context* pContext,
        M4WRITER_DataInterface* pWriterDataInterface,
        M4VPP_apply_fct* pVPPfct, M4VPP_Context pVPPctxt,
        M4OSA_Void* pExternalAPI, M4OSA_Void* pUserData) {

    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    int encoderInput = OMX_COLOR_FormatYUV420Planar;

    ALOGV("VideoEditorVideoEncoder_init begin: format  %d", format);
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pWriterDataInterface, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pVPPfct, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pVPPctxt, M4ERR_PARAMETER);

    // Context allocation & initialization
    SAFE_MALLOC(pEncoderContext, VideoEditorVideoEncoder_Context, 1,
        "VideoEditorVideoEncoder");
    pEncoderContext->mState = CREATED;
    pEncoderContext->mFormat = format;
    pEncoderContext->mWriterDataInterface = pWriterDataInterface;
    pEncoderContext->mPreProcFunction = pVPPfct;
    pEncoderContext->mPreProcContext = pVPPctxt;
    pEncoderContext->mPuller = NULL;

    // Get color converter and determine encoder input format
    pEncoderContext->mI420ColorConverter = new I420ColorConverter;
    if (pEncoderContext->mI420ColorConverter->isLoaded()) {
        encoderInput = pEncoderContext->mI420ColorConverter->getEncoderInputFormat();
    }
    if (encoderInput == OMX_COLOR_FormatYUV420Planar) {
        delete pEncoderContext->mI420ColorConverter;
        pEncoderContext->mI420ColorConverter = NULL;
    }
    pEncoderContext->mEncoderColorFormat = (OMX_COLOR_FORMATTYPE)encoderInput;
    ALOGI("encoder input format = 0x%X\n", encoderInput);

    *pContext = pEncoderContext;

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_init no error");
    } else {
        VideoEditorVideoEncoder_cleanup(pEncoderContext);
        *pContext = M4OSA_NULL;
        ALOGV("VideoEditorVideoEncoder_init ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_init end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_init_H263(M4ENCODER_Context* pContext,
        M4WRITER_DataInterface* pWriterDataInterface, M4VPP_apply_fct* pVPPfct,
        M4VPP_Context pVPPctxt, M4OSA_Void* pExternalAPI, M4OSA_Void* pUserData)
        {

    return VideoEditorVideoEncoder_init(M4ENCODER_kH263, pContext,
        pWriterDataInterface, pVPPfct, pVPPctxt, pExternalAPI, pUserData);
}


M4OSA_ERR VideoEditorVideoEncoder_init_MPEG4(M4ENCODER_Context* pContext,
        M4WRITER_DataInterface* pWriterDataInterface, M4VPP_apply_fct* pVPPfct,
        M4VPP_Context pVPPctxt, M4OSA_Void* pExternalAPI, M4OSA_Void* pUserData)
        {

    return VideoEditorVideoEncoder_init(M4ENCODER_kMPEG4, pContext,
        pWriterDataInterface, pVPPfct, pVPPctxt, pExternalAPI, pUserData);
}


M4OSA_ERR VideoEditorVideoEncoder_init_H264(M4ENCODER_Context* pContext,
        M4WRITER_DataInterface* pWriterDataInterface, M4VPP_apply_fct* pVPPfct,
        M4VPP_Context pVPPctxt, M4OSA_Void* pExternalAPI, M4OSA_Void* pUserData)
        {

    return VideoEditorVideoEncoder_init(M4ENCODER_kH264, pContext,
        pWriterDataInterface, pVPPfct, pVPPctxt, pExternalAPI, pUserData);
}

M4OSA_ERR VideoEditorVideoEncoder_close(M4ENCODER_Context pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;

    ALOGV("VideoEditorVideoEncoder_close begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    VIDEOEDITOR_CHECK(OPENED == pEncoderContext->mState, M4ERR_STATE);

    // Release memory
    SAFE_FREE(pEncoderContext->mCodecParams);

    // Destroy the graph
    pEncoderContext->mEncoder.clear();
    pEncoderContext->mClient.disconnect();
    pEncoderContext->mEncoderSource.clear();

    delete pEncoderContext->mPuller;
    pEncoderContext->mPuller = NULL;

    delete pEncoderContext->mI420ColorConverter;
    pEncoderContext->mI420ColorConverter = NULL;

    // Set the new state
    pEncoderContext->mState = CREATED;

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_close no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_close ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_close end");
    return err;
}


M4OSA_ERR VideoEditorVideoEncoder_open(M4ENCODER_Context pContext,
        M4SYS_AccessUnit* pAU, M4OSA_Void* pParams) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    M4ENCODER_Params* pCodecParams = M4OSA_NULL;
    status_t result = OK;
    sp<MetaData> encoderMetadata = NULL;
    const char* mime = NULL;
    int32_t iProfile = 0;
    int32_t iLevel = 0;

    int32_t iFrameRate = 0;
    uint32_t codecFlags = 0;

    ALOGV(">>> VideoEditorVideoEncoder_open begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pAU,      M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pParams,  M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    pCodecParams = (M4ENCODER_Params*)pParams;
    VIDEOEDITOR_CHECK(CREATED == pEncoderContext->mState, M4ERR_STATE);

    // Context initialization
    pEncoderContext->mAccessUnit = pAU;
    pEncoderContext->mVideoEditorProfile = MediaProfiles::getInstance();
    pEncoderContext->mMaxPrefetchFrames =
        pEncoderContext->mVideoEditorProfile->getVideoEditorCapParamByName(
        "maxPrefetchYUVFrames");

    // Allocate & initialize the encoding parameters
    SAFE_MALLOC(pEncoderContext->mCodecParams, M4ENCODER_Params, 1,
        "VideoEditorVideoEncoder");


    pEncoderContext->mCodecParams->InputFormat = pCodecParams->InputFormat;
    pEncoderContext->mCodecParams->InputFrameWidth =
        pCodecParams->InputFrameWidth;
    pEncoderContext->mCodecParams->InputFrameHeight =
        pCodecParams->InputFrameHeight;
    pEncoderContext->mCodecParams->FrameWidth = pCodecParams->FrameWidth;
    pEncoderContext->mCodecParams->FrameHeight = pCodecParams->FrameHeight;
    pEncoderContext->mCodecParams->Bitrate = pCodecParams->Bitrate;
    pEncoderContext->mCodecParams->FrameRate = pCodecParams->FrameRate;
    pEncoderContext->mCodecParams->Format = pCodecParams->Format;
    pEncoderContext->mCodecParams->videoProfile = pCodecParams->videoProfile;
    pEncoderContext->mCodecParams->videoLevel= pCodecParams->videoLevel;

    // Check output format consistency and resolution
    VIDEOEDITOR_CHECK(
        pEncoderContext->mCodecParams->Format == pEncoderContext->mFormat,
        M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(0 == pEncoderContext->mCodecParams->FrameWidth  % 16,
        M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(0 == pEncoderContext->mCodecParams->FrameHeight % 16,
        M4ERR_PARAMETER);

    /**
     * StageFright graph building
     */

    // Create the meta data for the encoder
    encoderMetadata = new MetaData;
    switch( pEncoderContext->mCodecParams->Format ) {
        case M4ENCODER_kH263:
            mime     = MEDIA_MIMETYPE_VIDEO_H263;
            break;
        case M4ENCODER_kMPEG4:
            mime     = MEDIA_MIMETYPE_VIDEO_MPEG4;
            break;
        case M4ENCODER_kH264:
            mime     = MEDIA_MIMETYPE_VIDEO_AVC;
            break;
        default:
            VIDEOEDITOR_CHECK(!"VideoEncoder_open : incorrect input format",
                M4ERR_PARAMETER);
            break;
    }
    iProfile = pEncoderContext->mCodecParams->videoProfile;
    iLevel = pEncoderContext->mCodecParams->videoLevel;
    ALOGV("Encoder mime %s profile %d, level %d",
        mime,iProfile, iLevel);
    ALOGV("Encoder w %d, h %d, bitrate %d, fps %d",
        pEncoderContext->mCodecParams->FrameWidth,
        pEncoderContext->mCodecParams->FrameHeight,
        pEncoderContext->mCodecParams->Bitrate,
        pEncoderContext->mCodecParams->FrameRate);
    CHECK(iProfile != 0x7fffffff);
    CHECK(iLevel != 0x7fffffff);

    encoderMetadata->setCString(kKeyMIMEType, mime);
    encoderMetadata->setInt32(kKeyVideoProfile, iProfile);
    //FIXME:
    // Temp: Do not set the level for Mpeg4 / H.263 Enc
    // as OMX.Nvidia.mp4.encoder and OMX.Nvidia.h263.encoder
    // return 0x80001019
    if (pEncoderContext->mCodecParams->Format == M4ENCODER_kH264) {
        encoderMetadata->setInt32(kKeyVideoLevel, iLevel);
    }
    encoderMetadata->setInt32(kKeyWidth,
        (int32_t)pEncoderContext->mCodecParams->FrameWidth);
    encoderMetadata->setInt32(kKeyStride,
        (int32_t)pEncoderContext->mCodecParams->FrameWidth);
    encoderMetadata->setInt32(kKeyHeight,
        (int32_t)pEncoderContext->mCodecParams->FrameHeight);
    encoderMetadata->setInt32(kKeySliceHeight,
        (int32_t)pEncoderContext->mCodecParams->FrameHeight);

    switch( pEncoderContext->mCodecParams->FrameRate ) {
        case M4ENCODER_k5_FPS:    iFrameRate = 5;  break;
        case M4ENCODER_k7_5_FPS:  iFrameRate = 8;  break;
        case M4ENCODER_k10_FPS:   iFrameRate = 10; break;
        case M4ENCODER_k12_5_FPS: iFrameRate = 13; break;
        case M4ENCODER_k15_FPS:   iFrameRate = 15; break;
        case M4ENCODER_k20_FPS:   iFrameRate = 20; break;
        case M4ENCODER_k25_FPS:   iFrameRate = 25; break;
        case M4ENCODER_k30_FPS:   iFrameRate = 30; break;
        case M4ENCODER_kVARIABLE_FPS:
            iFrameRate = 30;
            ALOGI("Frame rate set to M4ENCODER_kVARIABLE_FPS: set to 30");
          break;
        case M4ENCODER_kUSE_TIMESCALE:
            iFrameRate = 30;
            ALOGI("Frame rate set to M4ENCODER_kUSE_TIMESCALE:  set to 30");
            break;

        default:
            VIDEOEDITOR_CHECK(!"VideoEncoder_open:incorrect framerate",
                M4ERR_STATE);
            break;
    }
    encoderMetadata->setInt32(kKeyFrameRate, iFrameRate);
    encoderMetadata->setInt32(kKeyBitRate,
        (int32_t)pEncoderContext->mCodecParams->Bitrate);
    encoderMetadata->setInt32(kKeyIFramesInterval, 1);

    encoderMetadata->setInt32(kKeyColorFormat,
        pEncoderContext->mEncoderColorFormat);

    if (pEncoderContext->mCodecParams->Format != M4ENCODER_kH263) {
        // Get the encoder DSI
        err = VideoEditorVideoEncoder_getDSI(pEncoderContext, encoderMetadata);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);
    }

    // Create the encoder source
    pEncoderContext->mEncoderSource = VideoEditorVideoEncoderSource::Create(
        encoderMetadata);
    VIDEOEDITOR_CHECK(
        NULL != pEncoderContext->mEncoderSource.get(), M4ERR_STATE);

    // Connect to the OMX client
    result = pEncoderContext->mClient.connect();
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);

    // Create the OMX codec
#ifdef VIDEOEDITOR_FORCECODEC
    codecFlags |= OMXCodec::VIDEOEDITOR_FORCECODEC;
#endif /* VIDEOEDITOR_FORCECODEC */
    pEncoderContext->mEncoder = OMXCodec::Create(
        pEncoderContext->mClient.interface(), encoderMetadata, true,
        pEncoderContext->mEncoderSource, NULL, codecFlags);
    VIDEOEDITOR_CHECK(NULL != pEncoderContext->mEncoder.get(), M4ERR_STATE);
    ALOGV("VideoEditorVideoEncoder_open : DONE");
    pEncoderContext->mPuller = new MediaBufferPuller(
        pEncoderContext->mEncoder);

    // Set the new state
    pEncoderContext->mState = OPENED;

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_open no error");
    } else {
        VideoEditorVideoEncoder_close(pEncoderContext);
        ALOGV("VideoEditorVideoEncoder_open ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_open end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_processInputBuffer(
        M4ENCODER_Context pContext, M4OSA_Double Cts,
        M4OSA_Bool bReachedEOS) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    M4VIFI_ImagePlane pOutPlane[3];
    MediaBuffer* buffer = NULL;
    int32_t nbBuffer = 0;

    ALOGV("VideoEditorVideoEncoder_processInputBuffer begin: cts  %f", Cts);
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    pOutPlane[0].pac_data = M4OSA_NULL;
    pOutPlane[1].pac_data = M4OSA_NULL;
    pOutPlane[2].pac_data = M4OSA_NULL;

    if ( M4OSA_FALSE == bReachedEOS ) {
        M4OSA_UInt32 sizeY = pEncoderContext->mCodecParams->FrameWidth *
            pEncoderContext->mCodecParams->FrameHeight;
        M4OSA_UInt32 sizeU = sizeY >> 2;
        M4OSA_UInt32 size  = sizeY + 2*sizeU;
        M4OSA_UInt8* pData = M4OSA_NULL;
        buffer = new MediaBuffer((size_t)size);
        pData = (M4OSA_UInt8*)buffer->data() + buffer->range_offset();

        // Prepare the output image for pre-processing
        pOutPlane[0].u_width   = pEncoderContext->mCodecParams->FrameWidth;
        pOutPlane[0].u_height  = pEncoderContext->mCodecParams->FrameHeight;
        pOutPlane[0].u_topleft = 0;
        pOutPlane[0].u_stride  = pOutPlane[0].u_width;
        pOutPlane[1].u_width   = pOutPlane[0].u_width/2;
        pOutPlane[1].u_height  = pOutPlane[0].u_height/2;
        pOutPlane[1].u_topleft = 0;
        pOutPlane[1].u_stride  = pOutPlane[0].u_stride/2;
        pOutPlane[2].u_width   = pOutPlane[1].u_width;
        pOutPlane[2].u_height  = pOutPlane[1].u_height;
        pOutPlane[2].u_topleft = 0;
        pOutPlane[2].u_stride  = pOutPlane[1].u_stride;

        pOutPlane[0].pac_data = pData;
        pOutPlane[1].pac_data = pData + sizeY;
        pOutPlane[2].pac_data = pData + sizeY + sizeU;

        // Apply pre-processing
        err = pEncoderContext->mPreProcFunction(
            pEncoderContext->mPreProcContext, M4OSA_NULL, pOutPlane);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

        // Convert MediaBuffer to the encoder input format if necessary
        if (pEncoderContext->mI420ColorConverter) {
            I420ColorConverter* converter = pEncoderContext->mI420ColorConverter;
            int actualWidth = pEncoderContext->mCodecParams->FrameWidth;
            int actualHeight = pEncoderContext->mCodecParams->FrameHeight;

            int encoderWidth, encoderHeight;
            ARect encoderRect;
            int encoderBufferSize;

            if (converter->getEncoderInputBufferInfo(
                actualWidth, actualHeight,
                &encoderWidth, &encoderHeight,
                &encoderRect, &encoderBufferSize) == 0) {

                MediaBuffer* newBuffer = new MediaBuffer(encoderBufferSize);

                if (converter->convertI420ToEncoderInput(
                    pData,  // srcBits
                    actualWidth, actualHeight,
                    encoderWidth, encoderHeight,
                    encoderRect,
                    (uint8_t*)newBuffer->data() + newBuffer->range_offset()) < 0) {
                    ALOGE("convertI420ToEncoderInput failed");
                }

                // switch to new buffer
                buffer->release();
                buffer = newBuffer;
            }
        }

        // Set the metadata
        buffer->meta_data()->setInt64(kKeyTime, (int64_t)(Cts*1000));
    }

    // Push the buffer to the source, a NULL buffer, notifies the source of EOS
    nbBuffer = pEncoderContext->mEncoderSource->storeBuffer(buffer);

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_processInputBuffer error 0x%X", err);
    } else {
        if( NULL != buffer ) {
            buffer->release();
        }
        ALOGV("VideoEditorVideoEncoder_processInputBuffer ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_processInputBuffer end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_processOutputBuffer(
        M4ENCODER_Context pContext, MediaBuffer* buffer) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    M4OSA_UInt32 Cts = 0;
    int32_t i32Tmp = 0;
    int64_t i64Tmp = 0;
    status_t result = OK;

    ALOGV("VideoEditorVideoEncoder_processOutputBuffer begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != buffer,   M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;

    // Process the returned AU
    if ( 0 == buffer->range_length() ) {
        // Encoder has no data yet, nothing unusual
        ALOGV("VideoEditorVideoEncoder_processOutputBuffer : buffer is empty");
        goto cleanUp;
    }
    VIDEOEDITOR_CHECK(0 == ((M4OSA_UInt32)buffer->data())%4, M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(buffer->meta_data().get(), M4ERR_PARAMETER);
    if ( buffer->meta_data()->findInt32(kKeyIsCodecConfig, &i32Tmp) && i32Tmp ){
        {   // Display the DSI
            ALOGV("VideoEditorVideoEncoder_processOutputBuffer DSI %d",
                buffer->range_length());
            uint8_t* tmp = (uint8_t*)(buffer->data());
            for( uint32_t i=0; i<buffer->range_length(); i++ ) {
                ALOGV("DSI [%d] %.2X", i, tmp[i]);
            }
        }
    } else {
        // Check the CTS
        VIDEOEDITOR_CHECK(buffer->meta_data()->findInt64(kKeyTime, &i64Tmp),
            M4ERR_STATE);

        pEncoderContext->mNbOutputFrames++;
        if ( 0 > pEncoderContext->mFirstOutputCts ) {
            pEncoderContext->mFirstOutputCts = i64Tmp;
        }
        pEncoderContext->mLastOutputCts = i64Tmp;

        Cts = (M4OSA_Int32)(i64Tmp/1000);
        ALOGV("[TS_CHECK] VI/ENC WRITE frame %d @ %lld -> %d (last %d)",
            pEncoderContext->mNbOutputFrames, i64Tmp, Cts,
            pEncoderContext->mLastCTS);
        if ( Cts < pEncoderContext->mLastCTS ) {
            ALOGV("VideoEncoder_processOutputBuffer WARNING : Cts is going "
            "backwards %d < %d", Cts, pEncoderContext->mLastCTS);
            goto cleanUp;
        }
        ALOGV("VideoEditorVideoEncoder_processOutputBuffer : %d %d",
            Cts, pEncoderContext->mLastCTS);

        // Retrieve the AU container
        err = pEncoderContext->mWriterDataInterface->pStartAU(
            pEncoderContext->mWriterDataInterface->pWriterContext,
            pEncoderContext->mAccessUnit->stream->streamID,
            pEncoderContext->mAccessUnit);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

        // Format the AU
        VIDEOEDITOR_CHECK(
            buffer->range_length() <= pEncoderContext->mAccessUnit->size,
            M4ERR_PARAMETER);
        // Remove H264 AU start code
        if ( M4ENCODER_kH264 == pEncoderContext->mFormat ) {
            if (!memcmp((const uint8_t *)buffer->data() + \
                    buffer->range_offset(), "\x00\x00\x00\x01", 4) ) {
                buffer->set_range(buffer->range_offset() + 4,
                    buffer->range_length() - 4);
            }
        }

        if ( (M4ENCODER_kH264 == pEncoderContext->mFormat) &&
            (M4OSA_NULL != pEncoderContext->mH264NALUPostProcessFct) ) {
        // H264 trimming case, NALU post processing is needed
        M4OSA_Int32 outputSize = pEncoderContext->mAccessUnit->size;
        err = pEncoderContext->mH264NALUPostProcessFct(
            pEncoderContext->mH264NALUPostProcessCtx,
            (M4OSA_UInt8*)buffer->data()+buffer->range_offset(),
            buffer->range_length(),
            (M4OSA_UInt8*)pEncoderContext->mAccessUnit->dataAddress,
            &outputSize);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);
        pEncoderContext->mAccessUnit->size = (M4OSA_UInt32)outputSize;
        } else {
            // The AU can just be copied
            memcpy((void *)pEncoderContext->mAccessUnit->\
                dataAddress, (void *)((M4OSA_MemAddr8)(buffer->data())+buffer->\
                range_offset()), buffer->range_length());
            pEncoderContext->mAccessUnit->size =
                (M4OSA_UInt32)buffer->range_length();
        }

        if ( buffer->meta_data()->findInt32(kKeyIsSyncFrame,&i32Tmp) && i32Tmp){
            pEncoderContext->mAccessUnit->attribute = AU_RAP;
        } else {
            pEncoderContext->mAccessUnit->attribute = AU_P_Frame;
        }
        pEncoderContext->mLastCTS = Cts;
        pEncoderContext->mAccessUnit->CTS = Cts;
        pEncoderContext->mAccessUnit->DTS = Cts;

        ALOGV("VideoEditorVideoEncoder_processOutputBuffer: AU @ 0x%X 0x%X %d %d",
            pEncoderContext->mAccessUnit->dataAddress,
            *pEncoderContext->mAccessUnit->dataAddress,
            pEncoderContext->mAccessUnit->size,
            pEncoderContext->mAccessUnit->CTS);

        // Write the AU
        err = pEncoderContext->mWriterDataInterface->pProcessAU(
            pEncoderContext->mWriterDataInterface->pWriterContext,
            pEncoderContext->mAccessUnit->stream->streamID,
            pEncoderContext->mAccessUnit);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);
    }

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_processOutputBuffer no error");
    } else {
        SAFE_FREE(pEncoderContext->mHeader.pBuf);
        pEncoderContext->mHeader.Size = 0;
        ALOGV("VideoEditorVideoEncoder_processOutputBuffer ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_processOutputBuffer end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_encode(M4ENCODER_Context pContext,
        M4VIFI_ImagePlane* pInPlane, M4OSA_Double Cts,
        M4ENCODER_FrameMode FrameMode) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    status_t result = OK;
    MediaBuffer* outputBuffer = NULL;

    ALOGV("VideoEditorVideoEncoder_encode 0x%X %f %d", pInPlane, Cts, FrameMode);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    if ( STARTED == pEncoderContext->mState ) {
        pEncoderContext->mState = BUFFERING;
    }
    VIDEOEDITOR_CHECK(
        (BUFFERING | READING) & pEncoderContext->mState, M4ERR_STATE);

    pEncoderContext->mNbInputFrames++;
    if ( 0 > pEncoderContext->mFirstInputCts ) {
        pEncoderContext->mFirstInputCts = Cts;
    }
    pEncoderContext->mLastInputCts = Cts;

    ALOGV("VideoEditorVideoEncoder_encode 0x%X %d %f (%d)", pInPlane, FrameMode,
        Cts, pEncoderContext->mLastCTS);

    // Push the input buffer to the encoder source
    err = VideoEditorVideoEncoder_processInputBuffer(pEncoderContext, Cts,
        M4OSA_FALSE);
    VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

    // Notify the source in case of EOS
    if ( M4ENCODER_kLastFrame == FrameMode ) {
        err = VideoEditorVideoEncoder_processInputBuffer(
            pEncoderContext, 0, M4OSA_TRUE);
        VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);
    }

    if ( BUFFERING == pEncoderContext->mState ) {
        // Prefetch is complete, start reading
        pEncoderContext->mState = READING;
    }
    // Read
    while (1)  {
        MediaBuffer *outputBuffer =
                pEncoderContext->mPuller->getBufferNonBlocking();

        if (outputBuffer == NULL) {
            int32_t YUVBufferNumber =
                    pEncoderContext->mEncoderSource->getNumberOfBuffersInQueue();
            /* Make sure that the configured maximum number of prefetch YUV frames is
             * not exceeded. This is to limit the amount of memory usage of video editor engine.
             * The value of maximum prefetch Yuv frames is defined in media_profiles.xml */
            if ((YUVBufferNumber < pEncoderContext->mMaxPrefetchFrames) ||
                (pEncoderContext->mPuller->hasMediaSourceReturnedError()
                    == true)) {
                break;
            }
        } else {
            // Provide the encoded AU to the writer
            err = VideoEditorVideoEncoder_processOutputBuffer(pEncoderContext,
                outputBuffer);
            VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

            pEncoderContext->mPuller->putBuffer(outputBuffer);
        }
    }

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_encode no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_encode ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_encode end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_start(M4ENCODER_Context pContext) {
    M4OSA_ERR                  err             = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    status_t                   result          = OK;

    ALOGV("VideoEditorVideoEncoder_start begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;
    VIDEOEDITOR_CHECK(OPENED == pEncoderContext->mState, M4ERR_STATE);

    pEncoderContext->mNbInputFrames  = 0;
    pEncoderContext->mFirstInputCts  = -1.0;
    pEncoderContext->mLastInputCts   = -1.0;
    pEncoderContext->mNbOutputFrames = 0;
    pEncoderContext->mFirstOutputCts = -1;
    pEncoderContext->mLastOutputCts  = -1;

    result = pEncoderContext->mEncoder->start();
    VIDEOEDITOR_CHECK(OK == result, M4ERR_STATE);

    pEncoderContext->mPuller->start();

    // Set the new state
    pEncoderContext->mState = STARTED;

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_start no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_start ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_start end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_stop(M4ENCODER_Context pContext) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;
    MediaBuffer* outputBuffer = NULL;
    status_t result = OK;

    ALOGV("VideoEditorVideoEncoder_stop begin");
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;

    // Send EOS again to make sure the source doesn't block.
    err = VideoEditorVideoEncoder_processInputBuffer(pEncoderContext, 0,
        M4OSA_TRUE);
    VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

    // Process the remaining buffers if necessary
    if ( (BUFFERING | READING) & pEncoderContext->mState ) {
        while (1)  {
            MediaBuffer *outputBuffer =
                pEncoderContext->mPuller->getBufferBlocking();

            if (outputBuffer == NULL) break;

            err = VideoEditorVideoEncoder_processOutputBuffer(
                pEncoderContext, outputBuffer);
            VIDEOEDITOR_CHECK(M4NO_ERROR == err, err);

            pEncoderContext->mPuller->putBuffer(outputBuffer);
        }

        pEncoderContext->mState = STARTED;
    }

    // Stop the graph module if necessary
    if ( STARTED == pEncoderContext->mState ) {
        pEncoderContext->mPuller->stop();
        pEncoderContext->mEncoder->stop();
        pEncoderContext->mState = OPENED;
    }

    if (pEncoderContext->mNbInputFrames != pEncoderContext->mNbOutputFrames) {
        ALOGW("Some frames were not encoded: input(%d) != output(%d)",
            pEncoderContext->mNbInputFrames, pEncoderContext->mNbOutputFrames);
    }

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_stop no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_stop ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_stop end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_regulBitRate(M4ENCODER_Context pContext) {
    ALOGW("regulBitRate is not implemented");
    return M4NO_ERROR;
}

M4OSA_ERR VideoEditorVideoEncoder_setOption(M4ENCODER_Context pContext,
        M4OSA_UInt32 optionID, M4OSA_DataOption optionValue) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;

    ALOGV("VideoEditorVideoEncoder_setOption start optionID 0x%X", optionID);
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);

    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;

    switch( optionID ) {
        case M4ENCODER_kOptionID_SetH264ProcessNALUfctsPtr:
            pEncoderContext->mH264NALUPostProcessFct =
                (H264MCS_ProcessEncodedNALU_fct*)optionValue;
            break;
        case M4ENCODER_kOptionID_H264ProcessNALUContext:
            pEncoderContext->mH264NALUPostProcessCtx =
                (M4OSA_Context)optionValue;
            break;
        default:
            ALOGV("VideoEditorVideoEncoder_setOption: unsupported optionId 0x%X",
                optionID);
            VIDEOEDITOR_CHECK(M4OSA_FALSE, M4ERR_BAD_OPTION_ID);
            break;
    }

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_setOption no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_setOption ERROR 0x%X", err);
    }
    ALOGV("VideoEditorVideoEncoder_setOption end");
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_getOption(M4ENCODER_Context pContext,
        M4OSA_UInt32 optionID, M4OSA_DataOption optionValue) {
    M4OSA_ERR err = M4NO_ERROR;
    VideoEditorVideoEncoder_Context* pEncoderContext = M4OSA_NULL;

    ALOGV("VideoEditorVideoEncoder_getOption begin optinId 0x%X", optionID);
    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pContext, M4ERR_PARAMETER);
    pEncoderContext = (VideoEditorVideoEncoder_Context*)pContext;

    switch( optionID ) {
        case M4ENCODER_kOptionID_EncoderHeader:
            VIDEOEDITOR_CHECK(
                    M4OSA_NULL != pEncoderContext->mHeader.pBuf, M4ERR_STATE);
            *(M4ENCODER_Header**)optionValue = &(pEncoderContext->mHeader);
            break;
        default:
            ALOGV("VideoEditorVideoEncoder_getOption: unsupported optionId 0x%X",
                optionID);
            VIDEOEDITOR_CHECK(M4OSA_FALSE, M4ERR_BAD_OPTION_ID);
            break;
    }

cleanUp:
    if ( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_getOption no error");
    } else {
        ALOGV("VideoEditorVideoEncoder_getOption ERROR 0x%X", err);
    }
    return err;
}

M4OSA_ERR VideoEditorVideoEncoder_getInterface(M4ENCODER_Format format,
        M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode){
    M4OSA_ERR err = M4NO_ERROR;

    // Input parameters check
    VIDEOEDITOR_CHECK(M4OSA_NULL != pFormat,           M4ERR_PARAMETER);
    VIDEOEDITOR_CHECK(M4OSA_NULL != pEncoderInterface, M4ERR_PARAMETER);

    ALOGV("VideoEditorVideoEncoder_getInterface begin 0x%x 0x%x %d", pFormat,
        pEncoderInterface, mode);

    SAFE_MALLOC(*pEncoderInterface, M4ENCODER_GlobalInterface, 1,
        "VideoEditorVideoEncoder");

    *pFormat = format;

    switch( format ) {
        case M4ENCODER_kH263:
            {
                (*pEncoderInterface)->pFctInit =
                    VideoEditorVideoEncoder_init_H263;
                break;
            }
        case M4ENCODER_kMPEG4:
            {
                (*pEncoderInterface)->pFctInit =
                    VideoEditorVideoEncoder_init_MPEG4;
                break;
            }
        case M4ENCODER_kH264:
            {
                (*pEncoderInterface)->pFctInit =
                    VideoEditorVideoEncoder_init_H264;
                break;
            }
        default:
            ALOGV("VideoEditorVideoEncoder_getInterface : unsupported format %d",
                format);
            VIDEOEDITOR_CHECK(M4OSA_FALSE, M4ERR_PARAMETER);
        break;
    }
    (*pEncoderInterface)->pFctOpen         = VideoEditorVideoEncoder_open;
    (*pEncoderInterface)->pFctStart        = VideoEditorVideoEncoder_start;
    (*pEncoderInterface)->pFctStop         = VideoEditorVideoEncoder_stop;
    (*pEncoderInterface)->pFctPause        = M4OSA_NULL;
    (*pEncoderInterface)->pFctResume       = M4OSA_NULL;
    (*pEncoderInterface)->pFctClose        = VideoEditorVideoEncoder_close;
    (*pEncoderInterface)->pFctCleanup      = VideoEditorVideoEncoder_cleanup;
    (*pEncoderInterface)->pFctRegulBitRate =
        VideoEditorVideoEncoder_regulBitRate;
    (*pEncoderInterface)->pFctEncode       = VideoEditorVideoEncoder_encode;
    (*pEncoderInterface)->pFctSetOption    = VideoEditorVideoEncoder_setOption;
    (*pEncoderInterface)->pFctGetOption    = VideoEditorVideoEncoder_getOption;

cleanUp:
    if( M4NO_ERROR == err ) {
        ALOGV("VideoEditorVideoEncoder_getInterface no error");
    } else {
        *pEncoderInterface = M4OSA_NULL;
        ALOGV("VideoEditorVideoEncoder_getInterface ERROR 0x%X", err);
    }
    return err;
}

extern "C" {

M4OSA_ERR VideoEditorVideoEncoder_getInterface_H263(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode){
    return VideoEditorVideoEncoder_getInterface(M4ENCODER_kH263, pFormat,
            pEncoderInterface, mode);
}

M4OSA_ERR VideoEditorVideoEncoder_getInterface_MPEG4(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode){
    return VideoEditorVideoEncoder_getInterface(M4ENCODER_kMPEG4, pFormat,
           pEncoderInterface, mode);
}

M4OSA_ERR VideoEditorVideoEncoder_getInterface_H264(M4ENCODER_Format* pFormat,
        M4ENCODER_GlobalInterface** pEncoderInterface, M4ENCODER_OpenMode mode){
    return VideoEditorVideoEncoder_getInterface(M4ENCODER_kH264, pFormat,
           pEncoderInterface, mode);

}

}  // extern "C"

}  // namespace android
