/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef SOFT_VIDEO_DECODER_OMX_COMPONENT_H_

#define SOFT_VIDEO_DECODER_OMX_COMPONENT_H_

#include "SimpleSoftOMXComponent.h"

#include <media/stagefright/foundation/AHandlerReflector.h>
#include <media/IOMX.h>

#include <utils/RefBase.h>
#include <utils/threads.h>
#include <utils/Vector.h>

#define ARRAY_SIZE(a) (sizeof(a) / sizeof(*(a)))

namespace android {

struct SoftVideoDecoderOMXComponent : public SimpleSoftOMXComponent {
    SoftVideoDecoderOMXComponent(
            const char *name,
            const char *componentRole,
            OMX_VIDEO_CODINGTYPE codingType,
            const CodecProfileLevel *profileLevels,
            size_t numProfileLevels,
            int32_t width,
            int32_t height,
            const OMX_CALLBACKTYPE *callbacks,
            OMX_PTR appData,
            OMX_COMPONENTTYPE **component);

protected:
    virtual void onPortEnableCompleted(OMX_U32 portIndex, bool enabled);
    virtual void onReset();

    virtual OMX_ERRORTYPE internalGetParameter(
            OMX_INDEXTYPE index, OMX_PTR params);

    virtual OMX_ERRORTYPE internalSetParameter(
            OMX_INDEXTYPE index, const OMX_PTR params);

    virtual OMX_ERRORTYPE getConfig(
            OMX_INDEXTYPE index, OMX_PTR params);

    void initPorts(OMX_U32 numInputBuffers,
            OMX_U32 inputBufferSize,
            OMX_U32 numOutputBuffers,
            const char *mimeType);

    virtual void updatePortDefinitions();

    enum {
        kInputPortIndex  = 0,
        kOutputPortIndex = 1,
        kMaxPortIndex = 1,
    };

    uint32_t mWidth, mHeight;
    uint32_t mCropLeft, mCropTop, mCropWidth, mCropHeight;

    enum {
        NONE,
        AWAITING_DISABLED,
        AWAITING_ENABLED
    } mOutputPortSettingsChange;

private:
    const char *mComponentRole;
    OMX_VIDEO_CODINGTYPE mCodingType;
    const CodecProfileLevel *mProfileLevels;
    size_t mNumProfileLevels;

    DISALLOW_EVIL_CONSTRUCTORS(SoftVideoDecoderOMXComponent);
};

}  // namespace android

#endif  // SOFT_VIDEO_DECODER_OMX_COMPONENT_H_
