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
* @file   VideoEditorVideoDecoder_Internal.h
* @brief  StageFright shell video decoder internal header file*
*************************************************************************
*/

#include "M4OSA_Types.h"
#include "M4OSA_Debug.h"
#include "M4OSA_Memory.h"
#include "M4_Common.h"
#include "M4OSA_CoreID.h"

#include "M4DA_Types.h"
#include "M4READER_Common.h"
#include "M4VIFI_FiltersAPI.h"
#include "M4TOOL_VersionInfo.h"
#include "M4DECODER_Common.h"
#include "M4OSA_Semaphore.h"
#include "VideoEditorBuffer.h"
#include "M4VD_Tools.h"
#include "I420ColorConverter.h"

#include <utils/RefBase.h>
#include <android/rect.h>
#include <OMX_Video.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/OMXCodec.h>
#include <media/stagefright/OMXClient.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaBufferGroup.h>
#include <media/stagefright/MediaDefs.h>

#define VIDEOEDITOR_VIDEC_SHELL_VER_MAJOR     0
#define VIDEOEDITOR_VIDEC_SHELL_VER_MINOR     0
#define VIDEOEDITOR_VIDEC_SHELL_VER_REVISION  1

/* ERRORS */
#define M4ERR_SF_DECODER_RSRC_FAIL M4OSA_ERR_CREATE(M4_ERR, 0xFF, 0x0001)

namespace android {

typedef enum {
    VIDEOEDITOR_kMpeg4VideoDec,
    VIDEOEDITOR_kH263VideoDec,
    VIDEOEDITOR_kH264VideoDec
} VIDEOEDITOR_CodecType;


/*typedef struct{
    M4OSA_UInt32 stream_byte;
    M4OSA_UInt32 stream_index;
    M4OSA_MemAddr8 in;

} VIDEOEDITOR_VIDEO_Bitstream_ctxt;*/

typedef M4VS_Bitstream_ctxt VIDEOEDITOR_VIDEO_Bitstream_ctxt;

typedef struct {

    /** Stagefrigth params */
    OMXClient               mClient; /**< OMX Client session instance. */
    sp<MediaSource>         mVideoDecoder; /**< Stagefright decoder instance */
    sp<MediaSource>         mReaderSource; /**< Reader access > */

    /* READER */
    M4READER_GlobalInterface *m_pReaderGlobal;
    M4READER_DataInterface  *m_pReader;
    M4_AccessUnit           *m_pNextAccessUnitToDecode;

    /* STREAM PARAMS */
    M4_VideoStreamHandler*  m_pVideoStreamhandler;

    /* User filter params. */
    M4VIFI_PlanConverterFunctionType *m_pFilter;
    M4OSA_Void              *m_pFilterUserData;

    M4_MediaTime            m_lastDecodedCTS;
    M4_MediaTime            m_lastRenderCts;
    M4OSA_Bool              mReachedEOS;
    VIDEOEDITOR_CodecType   mDecoderType;
    M4DECODER_VideoSize     m_VideoSize;
    M4DECODER_MPEG4_DecoderConfigInfo m_Dci; /**< Decoder Config info */
    VIDEOEDITOR_BUFFER_Pool *m_pDecBufferPool; /**< Decoded buffer pool */
    OMX_COLOR_FORMATTYPE    decOuputColorFormat;

    M4OSA_UInt32            mNbInputFrames;
    M4OSA_Double            mFirstInputCts;
    M4OSA_Double            mLastInputCts;
    M4OSA_UInt32            mNbRenderedFrames;
    M4OSA_Double            mFirstRenderedCts;
    M4OSA_Double            mLastRenderedCts;
    M4OSA_UInt32            mNbOutputFrames;
    M4OSA_Double            mFirstOutputCts;
    M4OSA_Double            mLastOutputCts;
    M4OSA_Int32             mGivenWidth, mGivenHeight; //Used in case of
                                                       //INFO_FORMAT_CHANGED
    ARect                   mCropRect;  // These are obtained from kKeyCropRect.
    I420ColorConverter*     mI420ColorConverter;

    // Time interval between two consequtive/neighboring video frames.
    M4_MediaTime            mFrameIntervalMs;

} VideoEditorVideoDecoder_Context;

} //namespace android
