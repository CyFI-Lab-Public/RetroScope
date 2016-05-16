/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef SOFT_MPEG4_ENCODER_H_
#define SOFT_MPEG4_ENCODER_H_

#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/foundation/ABase.h>
#include "SimpleSoftOMXComponent.h"
#include "mp4enc_api.h"


namespace android {

struct MediaBuffer;

struct SoftMPEG4Encoder : public SimpleSoftOMXComponent {
    SoftMPEG4Encoder(
            const char *name,
            const OMX_CALLBACKTYPE *callbacks,
            OMX_PTR appData,
            OMX_COMPONENTTYPE **component);

    // Override SimpleSoftOMXComponent methods
    virtual OMX_ERRORTYPE internalGetParameter(
            OMX_INDEXTYPE index, OMX_PTR params);

    virtual OMX_ERRORTYPE internalSetParameter(
            OMX_INDEXTYPE index, const OMX_PTR params);

    virtual void onQueueFilled(OMX_U32 portIndex);

    // Override SoftOMXComponent methods

    virtual OMX_ERRORTYPE getExtensionIndex(
            const char *name, OMX_INDEXTYPE *index);

protected:
    virtual ~SoftMPEG4Encoder();

private:
    enum {
        kNumBuffers = 2,
    };

    enum {
        kStoreMetaDataExtensionIndex = OMX_IndexVendorStartUnused + 1
    };

    // OMX input buffer's timestamp and flags
    typedef struct {
        int64_t mTimeUs;
        int32_t mFlags;
    } InputBufferInfo;

    MP4EncodingMode mEncodeMode;
    int32_t  mVideoWidth;
    int32_t  mVideoHeight;
    int32_t  mVideoFrameRate;
    int32_t  mVideoBitRate;
    int32_t  mVideoColorFormat;
    bool     mStoreMetaDataInBuffers;
    int32_t  mIDRFrameRefreshIntervalInSec;

    int64_t  mNumInputFrames;
    bool     mStarted;
    bool     mSawInputEOS;
    bool     mSignalledError;

    tagvideoEncControls   *mHandle;
    tagvideoEncOptions    *mEncParams;
    uint8_t               *mInputFrameData;
    Vector<InputBufferInfo> mInputBufferInfoVec;

    void initPorts();
    OMX_ERRORTYPE initEncParams();
    OMX_ERRORTYPE initEncoder();
    OMX_ERRORTYPE releaseEncoder();

    uint8_t* extractGrallocData(void *data, buffer_handle_t *buffer);
    void releaseGrallocData(buffer_handle_t buffer);

    DISALLOW_EVIL_CONSTRUCTORS(SoftMPEG4Encoder);
};

}  // namespace android

#endif  // SOFT_MPEG4_ENCODER_H_
