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

#ifndef ANDROID_HARDWARE_PRO_CAMERA_H
#define ANDROID_HARDWARE_PRO_CAMERA_H

#include <utils/Timers.h>
#include <utils/KeyedVector.h>
#include <gui/IGraphicBufferProducer.h>
#include <system/camera.h>
#include <camera/IProCameraCallbacks.h>
#include <camera/IProCameraUser.h>
#include <camera/Camera.h>
#include <camera/CameraMetadata.h>
#include <camera/ICameraService.h>
#include <gui/CpuConsumer.h>

#include <gui/Surface.h>

#include <utils/Condition.h>
#include <utils/Mutex.h>

#include <camera/CameraBase.h>

struct camera_metadata;

namespace android {

// All callbacks on this class are concurrent
// (they come from separate threads)
class ProCameraListener : virtual public RefBase
{
public:
    virtual void notify(int32_t msgType, int32_t ext1, int32_t ext2) = 0;

    // Lock has been acquired. Write operations now available.
    virtual void onLockAcquired() = 0;
    // Lock has been released with exclusiveUnlock.
    virtual void onLockReleased() = 0;
    // Lock has been stolen by another client.
    virtual void onLockStolen() = 0;

    // Lock free.
    virtual void onTriggerNotify(int32_t msgType, int32_t ext1, int32_t ext2)
                                                                            = 0;
    // onFrameAvailable and OnResultReceived can come in with any order,
    // use android.sensor.timestamp and LockedBuffer.timestamp to correlate them

    /**
      * A new metadata buffer has been received.
      * -- Ownership of request passes on to the callee, free with
      *    free_camera_metadata.
      */
    virtual void onResultReceived(int32_t frameId, camera_metadata* result) = 0;

    // TODO: make onFrameAvailable pure virtual

    // A new frame buffer has been received for this stream.
    // -- This callback only fires for createStreamCpu streams
    // -- A buffer may be obtained by calling cpuConsumer->lockNextBuffer
    // -- Use buf.timestamp to correlate with result's android.sensor.timestamp
    // -- The buffer should be accessed with CpuConsumer::lockNextBuffer
    //      and CpuConsumer::unlockBuffer
    virtual void onFrameAvailable(int /*streamId*/,
                                  const sp<CpuConsumer>& /*cpuConsumer*/) {
    }

};

class ProCamera;

template <>
struct CameraTraits<ProCamera>
{
    typedef ProCameraListener     TCamListener;
    typedef IProCameraUser        TCamUser;
    typedef IProCameraCallbacks   TCamCallbacks;
    typedef status_t (ICameraService::*TCamConnectService)(const sp<IProCameraCallbacks>&,
                                                           int, const String16&, int,
                                                           /*out*/
                                                           sp<IProCameraUser>&);
    static TCamConnectService     fnConnectService;
};


class ProCamera :
    public CameraBase<ProCamera>,
    public BnProCameraCallbacks
{
public:
    /**
     * Connect a shared camera. By default access is restricted to read only
     * (Lock free) operations. To be able to submit custom requests a lock needs
     * to be acquired with exclusive[Try]Lock.
     */
    static sp<ProCamera> connect(int cameraId);
    virtual ~ProCamera();

    /**
     * Exclusive Locks:
     * - We may request exclusive access to a camera if no other
     *   clients are using the camera. This works as a traditional
     *   client, writing/reading any camera state.
     * - An application opening the camera (a regular 'Camera') will
     *   always steal away the exclusive lock from a ProCamera,
     *   this will call onLockReleased.
     * - onLockAcquired will be called again once it is possible
     *   to again exclusively lock the camera.
     *
     */

    /**
     * All exclusiveLock/unlock functions are asynchronous. The remote endpoint
     * shall not block while waiting to acquire the lock. Instead the lock
     * notifications will come in asynchronously on the listener.
     */

    /**
      * Attempt to acquire the lock instantly (non-blocking)
      * - If this succeeds, you do not need to wait for onLockAcquired
      *   but the event will still be fired
      *
      * Returns -EBUSY if already locked. 0 on success.
      */
    status_t exclusiveTryLock();
    // always returns 0. wait for onLockAcquired before lock is acquired.
    status_t exclusiveLock();
    // release a lock if we have one, or cancel the lock request.
    status_t exclusiveUnlock();

    // exclusive lock = do whatever we want. no lock = read only.
    bool hasExclusiveLock();

    /**
     * < 0 error, >= 0 the request ID. streaming to have the request repeat
     *    until cancelled.
     * The request queue is flushed when a lock is released or stolen
     *    if not locked will return PERMISSION_DENIED
     */
    int submitRequest(const struct camera_metadata* metadata,
                                                        bool streaming = false);
    // if not locked will return PERMISSION_DENIED, BAD_VALUE if requestId bad
    status_t cancelRequest(int requestId);

    /**
     * Ask for a stream to be enabled.
     * Lock free. Service maintains counter of streams.
     */
    status_t requestStream(int streamId);
// TODO: remove requestStream, its useless.

    /**
      * Delete a stream.
      * Lock free.
      *
      * NOTE: As a side effect this cancels ALL streaming requests.
      *
      * Errors: BAD_VALUE if unknown stream ID.
      *         PERMISSION_DENIED if the stream wasn't yours
      */
    status_t deleteStream(int streamId);

    /**
      * Create a new HW stream, whose sink will be the window.
      * Lock free. Service maintains counter of streams.
      * Errors: -EBUSY if too many streams created
      */
    status_t createStream(int width, int height, int format,
                          const sp<Surface>& surface,
                          /*out*/
                          int* streamId);

    /**
      * Create a new HW stream, whose sink will be the SurfaceTexture.
      * Lock free. Service maintains counter of streams.
      * Errors: -EBUSY if too many streams created
      */
    status_t createStream(int width, int height, int format,
                          const sp<IGraphicBufferProducer>& bufferProducer,
                          /*out*/
                          int* streamId);
    status_t createStreamCpu(int width, int height, int format,
                          int heapCount,
                          /*out*/
                          sp<CpuConsumer>* cpuConsumer,
                          int* streamId);
    status_t createStreamCpu(int width, int height, int format,
                          int heapCount,
                          bool synchronousMode,
                          /*out*/
                          sp<CpuConsumer>* cpuConsumer,
                          int* streamId);

    // Create a request object from a template.
    status_t createDefaultRequest(int templateId,
                                 /*out*/
                                  camera_metadata** request) const;

    // Get static camera metadata
    camera_metadata* getCameraInfo(int cameraId);

    // Blocks until a frame is available (CPU streams only)
    // - Obtain the frame data by calling CpuConsumer::lockNextBuffer
    // - Release the frame data after use with CpuConsumer::unlockBuffer
    // Return value:
    // - >0 - number of frames available to be locked
    // - <0 - error (refer to error codes)
    // Error codes:
    // -ETIMEDOUT if it took too long to get a frame
    int waitForFrameBuffer(int streamId);

    // Blocks until a metadata result is available
    // - Obtain the metadata by calling consumeFrameMetadata()
    // Error codes:
    // -ETIMEDOUT if it took too long to get a frame
    status_t waitForFrameMetadata();

    // Get the latest metadata. This is destructive.
    // - Calling this repeatedly will produce empty metadata objects.
    // - Use waitForFrameMetadata to sync until new data is available.
    CameraMetadata consumeFrameMetadata();

    // Convenience method to drop frame buffers (CPU streams only)
    // Return values:
    //  >=0 - number of frames dropped (up to count)
    //  <0  - error code
    // Error codes:
    //   BAD_VALUE - invalid streamId or count passed
    int dropFrameBuffer(int streamId, int count);

protected:
    ////////////////////////////////////////////////////////
    // IProCameraCallbacks implementation
    ////////////////////////////////////////////////////////
    virtual void        notifyCallback(int32_t msgType,
                                       int32_t ext,
                                       int32_t ext2);

    virtual void        onLockStatusChanged(
                                IProCameraCallbacks::LockStatus newLockStatus);

    virtual void        onResultReceived(int32_t requestId,
                                         camera_metadata* result);
private:
    ProCamera(int cameraId);

    class ProFrameListener : public CpuConsumer::FrameAvailableListener {
    public:
        ProFrameListener(wp<ProCamera> camera, int streamID) {
            mCamera = camera;
            mStreamId = streamID;
        }

    protected:
        virtual void onFrameAvailable() {
            sp<ProCamera> c = mCamera.promote();
            if (c.get() != NULL) {
                c->onFrameAvailable(mStreamId);
            }
        }

    private:
        wp<ProCamera> mCamera;
        int mStreamId;
    };
    friend class ProFrameListener;

    struct StreamInfo
    {
        StreamInfo(int streamId) {
            this->streamID = streamId;
            cpuStream = false;
            frameReady = 0;
        }

        StreamInfo() {
            streamID = -1;
            cpuStream = false;
        }

        int  streamID;
        bool cpuStream;
        sp<CpuConsumer> cpuConsumer;
        bool synchronousMode;
        sp<ProFrameListener> frameAvailableListener;
        sp<Surface> stc;
        int frameReady;
    };

    Condition mWaitCondition;
    Mutex     mWaitMutex;
    static const nsecs_t mWaitTimeout = 1000000000; // 1sec
    KeyedVector<int, StreamInfo> mStreams;
    bool mMetadataReady;
    CameraMetadata mLatestMetadata;

    void onFrameAvailable(int streamId);

    StreamInfo& getStreamInfo(int streamId);

    friend class CameraBase;
};

}; // namespace android

#endif
