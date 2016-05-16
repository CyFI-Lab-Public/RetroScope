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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2_ZSLPROCESSOR3_H
#define ANDROID_SERVERS_CAMERA_CAMERA2_ZSLPROCESSOR3_H

#include <utils/Thread.h>
#include <utils/String16.h>
#include <utils/Vector.h>
#include <utils/Mutex.h>
#include <utils/Condition.h>
#include <gui/BufferItemConsumer.h>
#include <camera/CameraMetadata.h>

#include "api1/client2/FrameProcessor.h"
#include "api1/client2/ZslProcessorInterface.h"
#include "device3/Camera3ZslStream.h"

namespace android {

class Camera2Client;

namespace camera2 {

class CaptureSequencer;
class Parameters;

/***
 * ZSL queue processing
 */
class ZslProcessor3 :
                    public ZslProcessorInterface,
                    public camera3::Camera3StreamBufferListener,
            virtual public Thread,
            virtual public FrameProcessor::FilteredListener {
  public:
    ZslProcessor3(sp<Camera2Client> client, wp<CaptureSequencer> sequencer);
    ~ZslProcessor3();

    // From FrameProcessor
    virtual void onFrameAvailable(int32_t requestId, const CameraMetadata &frame);

    /**
     ****************************************
     * ZslProcessorInterface implementation *
     ****************************************
     */

    virtual status_t updateStream(const Parameters &params);
    virtual status_t deleteStream();
    virtual int getStreamId() const;

    virtual status_t pushToReprocess(int32_t requestId);
    virtual status_t clearZslQueue();

    void dump(int fd, const Vector<String16>& args) const;

  protected:
    /**
     **********************************************
     * Camera3StreamBufferListener implementation *
     **********************************************
     */
    typedef camera3::Camera3StreamBufferListener::BufferInfo BufferInfo;
    // Buffer was acquired by the HAL
    virtual void onBufferAcquired(const BufferInfo& bufferInfo);
    // Buffer was released by the HAL
    virtual void onBufferReleased(const BufferInfo& bufferInfo);

  private:
    static const nsecs_t kWaitDuration = 10000000; // 10 ms

    enum {
        RUNNING,
        LOCKED
    } mState;

    wp<Camera2Client> mClient;
    wp<CaptureSequencer> mSequencer;

    const int mId;

    mutable Mutex mInputMutex;

    enum {
        NO_STREAM = -1
    };

    int mZslStreamId;
    sp<camera3::Camera3ZslStream> mZslStream;

    struct ZslPair {
        BufferItemConsumer::BufferItem buffer;
        CameraMetadata frame;
    };

    static const size_t kZslBufferDepth = 4;
    static const size_t kFrameListDepth = kZslBufferDepth * 2;
    Vector<CameraMetadata> mFrameList;
    size_t mFrameListHead;

    ZslPair mNextPair;

    Vector<ZslPair> mZslQueue;
    size_t mZslQueueHead;
    size_t mZslQueueTail;

    CameraMetadata mLatestCapturedRequest;

    virtual bool threadLoop();

    status_t clearZslQueueLocked();

    void dumpZslQueue(int id) const;

    nsecs_t getCandidateTimestampLocked(size_t* metadataIdx) const;
};


}; //namespace camera2
}; //namespace android

#endif
