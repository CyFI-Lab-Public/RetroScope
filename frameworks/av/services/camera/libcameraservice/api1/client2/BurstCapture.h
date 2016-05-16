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

#ifndef ANDROID_SERVERS_CAMERA_BURST_CAPTURE_H
#define ANDROID_SERVERS_CAMERA_BURST_CAPTURE_H

#include <camera/CameraMetadata.h>
#include <binder/MemoryBase.h>
#include <binder/MemoryHeapBase.h>
#include <gui/CpuConsumer.h>

#include "device2/Camera2Device.h"

namespace android {

class Camera2Client;

namespace camera2 {

class CaptureSequencer;

class BurstCapture : public virtual Thread,
                     public virtual CpuConsumer::FrameAvailableListener
{
public:
    BurstCapture(wp<Camera2Client> client, wp<CaptureSequencer> sequencer);
    virtual ~BurstCapture();

    virtual void onFrameAvailable();
    virtual status_t start(Vector<CameraMetadata> &metadatas, int32_t firstCaptureId);

protected:
    Mutex mInputMutex;
    bool mInputChanged;
    Condition mInputSignal;
    int mCaptureStreamId;
    wp<Camera2Client> mClient;
    wp<CaptureSequencer> mSequencer;

    // Should only be accessed by processing thread
    enum {
        NO_STREAM = -1
    };

    CpuConsumer::LockedBuffer* jpegEncode(
        CpuConsumer::LockedBuffer *imgBuffer,
        int quality);

    virtual status_t processFrameAvailable(sp<Camera2Client> &client);

private:
    virtual bool threadLoop();
    static const nsecs_t kWaitDuration = 10000000; // 10 ms
};

} // namespace camera2
} // namespace android

#endif
