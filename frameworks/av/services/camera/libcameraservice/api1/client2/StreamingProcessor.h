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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2_STREAMINGPROCESSOR_H
#define ANDROID_SERVERS_CAMERA_CAMERA2_STREAMINGPROCESSOR_H

#include <utils/Mutex.h>
#include <utils/String16.h>
#include <gui/BufferItemConsumer.h>

#include "camera/CameraMetadata.h"

namespace android {

class Camera2Client;
class CameraDeviceBase;
class IMemory;

namespace camera2 {

class Parameters;
class Camera2Heap;

/**
 * Management and processing for preview and recording streams
 */
class StreamingProcessor:
            public Thread, public BufferItemConsumer::FrameAvailableListener {
  public:
    StreamingProcessor(sp<Camera2Client> client);
    ~StreamingProcessor();

    status_t setPreviewWindow(sp<ANativeWindow> window);

    bool haveValidPreviewWindow() const;

    status_t updatePreviewRequest(const Parameters &params);
    status_t updatePreviewStream(const Parameters &params);
    status_t deletePreviewStream();
    int getPreviewStreamId() const;

    status_t setRecordingBufferCount(size_t count);
    status_t updateRecordingRequest(const Parameters &params);
    status_t updateRecordingStream(const Parameters &params);
    status_t deleteRecordingStream();
    int getRecordingStreamId() const;

    enum StreamType {
        NONE,
        PREVIEW,
        RECORD
    };
    status_t startStream(StreamType type,
            const Vector<int32_t> &outputStreams);

    // Toggle between paused and unpaused. Stream must be started first.
    status_t togglePauseStream(bool pause);

    status_t stopStream();

    // Returns the request ID for the currently streaming request
    // Returns 0 if there is no active request.
    status_t getActiveRequestId() const;
    status_t incrementStreamingIds();

    // Callback for new recording frames from HAL
    virtual void onFrameAvailable();
    // Callback from stagefright which returns used recording frames
    void releaseRecordingFrame(const sp<IMemory>& mem);

    status_t dump(int fd, const Vector<String16>& args);

  private:
    mutable Mutex mMutex;

    enum {
        NO_STREAM = -1
    };

    wp<Camera2Client> mClient;
    wp<CameraDeviceBase> mDevice;
    int mId;

    StreamType mActiveRequest;
    bool mPaused;

    Vector<int32_t> mActiveStreamIds;

    // Preview-related members
    int32_t mPreviewRequestId;
    int mPreviewStreamId;
    CameraMetadata mPreviewRequest;
    sp<ANativeWindow> mPreviewWindow;

    // Recording-related members
    static const nsecs_t kWaitDuration = 50000000; // 50 ms

    int32_t mRecordingRequestId;
    int mRecordingStreamId;
    int mRecordingFrameCount;
    sp<BufferItemConsumer> mRecordingConsumer;
    sp<ANativeWindow>  mRecordingWindow;
    CameraMetadata mRecordingRequest;
    sp<camera2::Camera2Heap> mRecordingHeap;

    bool mRecordingFrameAvailable;
    Condition mRecordingFrameAvailableSignal;

    static const size_t kDefaultRecordingHeapCount = 8;
    size_t mRecordingHeapCount;
    Vector<BufferItemConsumer::BufferItem> mRecordingBuffers;
    size_t mRecordingHeapHead, mRecordingHeapFree;

    virtual bool threadLoop();

    status_t processRecordingFrame();

    // Unilaterally free any buffers still outstanding to stagefright
    void releaseAllRecordingFramesLocked();

    // Determine if the specified stream is currently in use
    static bool isStreamActive(const Vector<int32_t> &streams,
            int32_t recordingStreamId);
};


}; // namespace camera2
}; // namespace android

#endif
