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

#ifndef ANDROID_SERVERS_CAMERA_PHOTOGRAPHY_CAMERADEVICECLIENT_H
#define ANDROID_SERVERS_CAMERA_PHOTOGRAPHY_CAMERADEVICECLIENT_H

#include <camera/camera2/ICameraDeviceUser.h>
#include <camera/camera2/ICameraDeviceCallbacks.h>

#include "CameraService.h"
#include "common/FrameProcessorBase.h"
#include "common/Camera2ClientBase.h"

namespace android {

struct CameraDeviceClientBase :
        public CameraService::BasicClient, public BnCameraDeviceUser
{
    typedef ICameraDeviceCallbacks TCamCallbacks;

    const sp<ICameraDeviceCallbacks>& getRemoteCallback() {
        return mRemoteCallback;
    }
    
public:
    CameraDeviceClientBase();

protected:
    CameraDeviceClientBase(const sp<CameraService>& cameraService,
            const sp<ICameraDeviceCallbacks>& remoteCallback,
            const String16& clientPackageName,
            int cameraId,
            int cameraFacing,
            int clientPid,
            uid_t clientUid,
            int servicePid);

    sp<ICameraDeviceCallbacks> mRemoteCallback;
};

/**
 * Implements the binder ICameraDeviceUser API,
 * meant for HAL3-public implementation of
 * android.hardware.photography.CameraDevice
 */
class CameraDeviceClient :
        public Camera2ClientBase<CameraDeviceClientBase>,
        public camera2::FrameProcessorBase::FilteredListener
{
public:
    /**
     * ICameraDeviceUser interface (see ICameraDeviceUser for details)
     */

    // Note that the callee gets a copy of the metadata.
    virtual int           submitRequest(sp<CaptureRequest> request,
                                        bool streaming = false);
    virtual status_t      cancelRequest(int requestId);

    // Returns -EBUSY if device is not idle
    virtual status_t      deleteStream(int streamId);

    virtual status_t      createStream(
            int width,
            int height,
            int format,
            const sp<IGraphicBufferProducer>& bufferProducer);

    // Create a request object from a template.
    virtual status_t      createDefaultRequest(int templateId,
                                               /*out*/
                                               CameraMetadata* request);

    // Get the static metadata for the camera
    // -- Caller owns the newly allocated metadata
    virtual status_t      getCameraInfo(/*out*/CameraMetadata* info);

    // Wait until all the submitted requests have finished processing
    virtual status_t      waitUntilIdle();

    // Flush all active and pending requests as fast as possible
    virtual status_t      flush();

    /**
     * Interface used by CameraService
     */

    CameraDeviceClient();

    CameraDeviceClient(const sp<CameraService>& cameraService,
            const sp<ICameraDeviceCallbacks>& remoteCallback,
            const String16& clientPackageName,
            int cameraId,
            int cameraFacing,
            int clientPid,
            uid_t clientUid,
            int servicePid);
    virtual ~CameraDeviceClient();

    virtual status_t      initialize(camera_module_t *module);

    virtual status_t      dump(int fd, const Vector<String16>& args);

    /**
     * Device listener interface
     */

    virtual void notifyIdle();
    virtual void notifyError();
    virtual void notifyShutter(int requestId, nsecs_t timestamp);

    /**
     * Interface used by independent components of CameraDeviceClient.
     */
protected:
    /** FilteredListener implementation **/
    virtual void          onFrameAvailable(int32_t requestId,
                                           const CameraMetadata& frame);
    virtual void          detachDevice();

    // Calculate the ANativeWindow transform from android.sensor.orientation
    status_t              getRotationTransformLocked(/*out*/int32_t* transform);

private:
    /** ICameraDeviceUser interface-related private members */

    /** Preview callback related members */
    sp<camera2::FrameProcessorBase> mFrameProcessor;
    static const int32_t FRAME_PROCESSOR_LISTENER_MIN_ID = 0;
    static const int32_t FRAME_PROCESSOR_LISTENER_MAX_ID = 0x7fffffffL;

    /** Utility members */
    bool enforceRequestPermissions(CameraMetadata& metadata);

    // IGraphicsBufferProducer binder -> Stream ID
    KeyedVector<sp<IBinder>, int> mStreamMap;

    // Stream ID
    Vector<int> mStreamingRequestList;

    int32_t mRequestIdCounter;
};

    extern CameraDeviceClient * oneInstance;
}; // namespace android

#endif
