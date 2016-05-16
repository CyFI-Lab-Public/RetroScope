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

//#define LOG_NDEBUG 0
#define LOG_TAG "Camera2-BurstCapture"

#include <utils/Log.h>
#include <utils/Trace.h>

#include "BurstCapture.h"

#include "api1/Camera2Client.h"
#include "api1/client2/JpegCompressor.h"

namespace android {
namespace camera2 {

BurstCapture::BurstCapture(wp<Camera2Client> client, wp<CaptureSequencer> sequencer):
    mCaptureStreamId(NO_STREAM),
    mClient(client),
    mSequencer(sequencer)
{
}

BurstCapture::~BurstCapture() {
}

status_t BurstCapture::start(Vector<CameraMetadata> &/*metadatas*/,
                             int32_t /*firstCaptureId*/) {
    ALOGE("Not completely implemented");
    return INVALID_OPERATION;
}

void BurstCapture::onFrameAvailable() {
    ALOGV("%s", __FUNCTION__);
    Mutex::Autolock l(mInputMutex);
    if(!mInputChanged) {
        mInputChanged = true;
        mInputSignal.signal();
    }
}

bool BurstCapture::threadLoop() {
    status_t res;
    {
        Mutex::Autolock l(mInputMutex);
        while(!mInputChanged) {
            res = mInputSignal.waitRelative(mInputMutex, kWaitDuration);
            if(res == TIMED_OUT) return true;
        }
        mInputChanged = false;
    }

    do {
        sp<Camera2Client> client = mClient.promote();
        if(client == 0) return false;
        ALOGV("%s: Calling processFrameAvailable()", __FUNCTION__);
        res = processFrameAvailable(client);
    } while(res == OK);

    return true;
}

CpuConsumer::LockedBuffer* BurstCapture::jpegEncode(
    CpuConsumer::LockedBuffer *imgBuffer,
    int /*quality*/)
{
    ALOGV("%s", __FUNCTION__);

    CpuConsumer::LockedBuffer *imgEncoded = new CpuConsumer::LockedBuffer;
    uint8_t *data = new uint8_t[ANDROID_JPEG_MAX_SIZE];
    imgEncoded->data = data;
    imgEncoded->width = imgBuffer->width;
    imgEncoded->height = imgBuffer->height;
    imgEncoded->stride = imgBuffer->stride;

    Vector<CpuConsumer::LockedBuffer*> buffers;
    buffers.push_back(imgBuffer);
    buffers.push_back(imgEncoded);

    sp<JpegCompressor> jpeg = new JpegCompressor();
    jpeg->start(buffers, 1);

    bool success = jpeg->waitForDone(10 * 1e9);
    if(success) {
        return buffers[1];
    }
    else {
        ALOGE("%s: JPEG encode timed out", __FUNCTION__);
        return NULL;  // TODO: maybe change function return value to status_t
    }
}

status_t BurstCapture::processFrameAvailable(sp<Camera2Client> &/*client*/) {
    ALOGE("Not implemented");
    return INVALID_OPERATION;
}

} // namespace camera2
} // namespace android
