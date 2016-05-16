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

#ifndef ANDROID_SERVERS_CAMERA_PROCAMERA2CLIENT_H
#define ANDROID_SERVERS_CAMERA_PROCAMERA2CLIENT_H

#include "CameraService.h"
#include "common/FrameProcessorBase.h"
#include "common/Camera2ClientBase.h"
#include "device2/Camera2Device.h"

namespace android {

class IMemory;
/**
 * Implements the binder IProCameraUser API,
 * meant for HAL2-level private API access.
 */
class ProCamera2Client :
        public Camera2ClientBase<CameraService::ProClient>,
        public camera2::FrameProcessorBase::FilteredListener
{
public:
    /**
     * IProCameraUser interface (see IProCameraUser for details)
     */
    virtual status_t      exclusiveTryLock();
    virtual status_t      exclusiveLock();
    virtual status_t      exclusiveUnlock();

    virtual bool          hasExclusiveLock();

    // Note that the callee gets a copy of the metadata.
    virtual int           submitRequest(camera_metadata_t* metadata,
                                        bool streaming = false);
    virtual status_t      cancelRequest(int requestId);

    virtual status_t      deleteStream(int streamId);

    virtual status_t      createStream(
            int width,
            int height,
            int format,
            const sp<IGraphicBufferProducer>& bufferProducer,
            /*out*/
            int* streamId);

    // Create a request object from a template.
    // -- Caller owns the newly allocated metadata
    virtual status_t      createDefaultRequest(int templateId,
                                               /*out*/
                                               camera_metadata** request);

    // Get the static metadata for the camera
    // -- Caller owns the newly allocated metadata
    virtual status_t      getCameraInfo(int cameraId,
                                        /*out*/
                                        camera_metadata** info);

    /**
     * Interface used by CameraService
     */

    ProCamera2Client(const sp<CameraService>& cameraService,
            const sp<IProCameraCallbacks>& remoteCallback,
            const String16& clientPackageName,
            int cameraId,
            int cameraFacing,
            int clientPid,
            uid_t clientUid,
            int servicePid);
    virtual ~ProCamera2Client();

    virtual status_t      initialize(camera_module_t *module);

    virtual status_t      dump(int fd, const Vector<String16>& args);

    // Callbacks from camera service
    virtual void onExclusiveLockStolen();

    /**
     * Interface used by independent components of ProCamera2Client.
     */

protected:
    /** FilteredListener implementation **/
    virtual void          onFrameAvailable(int32_t requestId,
                                           const CameraMetadata& frame);
    virtual void          detachDevice();

private:
    /** IProCameraUser interface-related private members */

    /** Preview callback related members */
    sp<camera2::FrameProcessorBase> mFrameProcessor;
    static const int32_t FRAME_PROCESSOR_LISTENER_MIN_ID = 0;
    static const int32_t FRAME_PROCESSOR_LISTENER_MAX_ID = 0x7fffffffL;

    /** Utility members */
    bool enforceRequestPermissions(CameraMetadata& metadata);

    // Whether or not we have an exclusive lock on the device
    // - if no we can't modify the request queue.
    // note that creating/deleting streams we own is still OK
    bool mExclusiveLock;
};

}; // namespace android

#endif
