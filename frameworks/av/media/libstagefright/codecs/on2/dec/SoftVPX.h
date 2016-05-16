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

#ifndef SOFT_VPX_H_

#define SOFT_VPX_H_

#include "SoftVideoDecoderOMXComponent.h"

namespace android {

struct SoftVPX : public SoftVideoDecoderOMXComponent {
    SoftVPX(const char *name,
            const char *componentRole,
            OMX_VIDEO_CODINGTYPE codingType,
            const OMX_CALLBACKTYPE *callbacks,
            OMX_PTR appData,
            OMX_COMPONENTTYPE **component);

protected:
    virtual ~SoftVPX();

    virtual void onQueueFilled(OMX_U32 portIndex);

private:
    enum {
        kNumBuffers = 4
    };

    enum {
        MODE_VP8,
        MODE_VP9
    } mMode;

    void *mCtx;

    status_t initDecoder();

    DISALLOW_EVIL_CONSTRUCTORS(SoftVPX);
};

}  // namespace android

#endif  // SOFT_VPX_H_
