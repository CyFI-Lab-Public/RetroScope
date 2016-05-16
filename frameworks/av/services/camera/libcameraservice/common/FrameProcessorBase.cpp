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

#define LOG_TAG "Camera2-FrameProcessorBase"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>

#include "common/FrameProcessorBase.h"
#include "common/CameraDeviceBase.h"

namespace android {
namespace camera2 {

FrameProcessorBase::FrameProcessorBase(wp<CameraDeviceBase> device) :
    Thread(/*canCallJava*/false),
    mDevice(device) {
}

FrameProcessorBase::~FrameProcessorBase() {
    ALOGV("%s: Exit", __FUNCTION__);
}

status_t FrameProcessorBase::registerListener(int32_t minId,
        int32_t maxId, wp<FilteredListener> listener, bool quirkSendPartials) {
    Mutex::Autolock l(mInputMutex);
    ALOGV("%s: Registering listener for frame id range %d - %d",
            __FUNCTION__, minId, maxId);
    RangeListener rListener = { minId, maxId, listener, quirkSendPartials };
    mRangeListeners.push_back(rListener);
    return OK;
}

status_t FrameProcessorBase::removeListener(int32_t minId,
                                           int32_t maxId,
                                           wp<FilteredListener> listener) {
    Mutex::Autolock l(mInputMutex);
    List<RangeListener>::iterator item = mRangeListeners.begin();
    while (item != mRangeListeners.end()) {
        if (item->minId == minId &&
                item->maxId == maxId &&
                item->listener == listener) {
            item = mRangeListeners.erase(item);
        } else {
            item++;
        }
    }
    return OK;
}

void FrameProcessorBase::dump(int fd, const Vector<String16>& /*args*/) {
    String8 result("    Latest received frame:\n");
    write(fd, result.string(), result.size());

    CameraMetadata lastFrame;
    {
        // Don't race while dumping metadata
        Mutex::Autolock al(mLastFrameMutex);
        lastFrame = CameraMetadata(mLastFrame);
    }
    lastFrame.dump(fd, 2, 6);
}

bool FrameProcessorBase::threadLoop() {
    status_t res;

    sp<CameraDeviceBase> device;
    {
        device = mDevice.promote();
        if (device == 0) return false;
    }

    res = device->waitForNextFrame(kWaitDuration);
    if (res == OK) {
        processNewFrames(device);
    } else if (res != TIMED_OUT) {
        ALOGE("FrameProcessorBase: Error waiting for new "
                "frames: %s (%d)", strerror(-res), res);
    }

    return true;
}

void FrameProcessorBase::processNewFrames(const sp<CameraDeviceBase> &device) {
    status_t res;
    ATRACE_CALL();
    CameraMetadata frame;

    ALOGV("%s: Camera %d: Process new frames", __FUNCTION__, device->getId());

    while ( (res = device->getNextFrame(&frame)) == OK) {

        camera_metadata_entry_t entry;

        entry = frame.find(ANDROID_REQUEST_FRAME_COUNT);
        if (entry.count == 0) {
            ALOGE("%s: Camera %d: Error reading frame number",
                    __FUNCTION__, device->getId());
            break;
        }
        ATRACE_INT("cam2_frame", entry.data.i32[0]);

        if (!processSingleFrame(frame, device)) {
            break;
        }

        if (!frame.isEmpty()) {
            Mutex::Autolock al(mLastFrameMutex);
            mLastFrame.acquire(frame);
        }
    }
    if (res != NOT_ENOUGH_DATA) {
        ALOGE("%s: Camera %d: Error getting next frame: %s (%d)",
                __FUNCTION__, device->getId(), strerror(-res), res);
        return;
    }

    return;
}

bool FrameProcessorBase::processSingleFrame(CameraMetadata &frame,
                                           const sp<CameraDeviceBase> &device) {
    ALOGV("%s: Camera %d: Process single frame (is empty? %d)",
          __FUNCTION__, device->getId(), frame.isEmpty());
    return processListeners(frame, device) == OK;
}

status_t FrameProcessorBase::processListeners(const CameraMetadata &frame,
        const sp<CameraDeviceBase> &device) {
    ATRACE_CALL();
    camera_metadata_ro_entry_t entry;

    // Quirks: Don't deliver partial results to listeners that don't want them
    bool quirkIsPartial = false;
    entry = frame.find(ANDROID_QUIRKS_PARTIAL_RESULT);
    if (entry.count != 0 &&
            entry.data.u8[0] == ANDROID_QUIRKS_PARTIAL_RESULT_PARTIAL) {
        ALOGV("%s: Camera %d: Not forwarding partial result to listeners",
                __FUNCTION__, device->getId());
        quirkIsPartial = true;
    }

    entry = frame.find(ANDROID_REQUEST_ID);
    if (entry.count == 0) {
        ALOGE("%s: Camera %d: Error reading frame id",
                __FUNCTION__, device->getId());
        return BAD_VALUE;
    }
    int32_t requestId = entry.data.i32[0];

    List<sp<FilteredListener> > listeners;
    {
        Mutex::Autolock l(mInputMutex);

        List<RangeListener>::iterator item = mRangeListeners.begin();
        while (item != mRangeListeners.end()) {
            if (requestId >= item->minId &&
                    requestId < item->maxId &&
                    (!quirkIsPartial || item->quirkSendPartials) ) {
                sp<FilteredListener> listener = item->listener.promote();
                if (listener == 0) {
                    item = mRangeListeners.erase(item);
                    continue;
                } else {
                    listeners.push_back(listener);
                }
            }
            item++;
        }
    }
    ALOGV("Got %d range listeners out of %d", listeners.size(), mRangeListeners.size());
    List<sp<FilteredListener> >::iterator item = listeners.begin();
    for (; item != listeners.end(); item++) {
        (*item)->onFrameAvailable(requestId, frame);
    }
    return OK;
}

}; // namespace camera2
}; // namespace android
