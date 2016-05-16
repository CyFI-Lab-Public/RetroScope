/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef ANDROID_HARDWARE_CAMERA_H
#define ANDROID_HARDWARE_CAMERA_H

#include <utils/Timers.h>
#include <gui/IGraphicBufferProducer.h>
#include <system/camera.h>
#include <camera/ICameraClient.h>
#include <camera/ICameraRecordingProxy.h>
#include <camera/ICameraRecordingProxyListener.h>
#include <camera/ICameraService.h>
#include <camera/ICamera.h>
#include <camera/CameraBase.h>

namespace android {

class Surface;
class String8;
class String16;

// ref-counted object for callbacks
class CameraListener: virtual public RefBase
{
public:
    virtual void notify(int32_t msgType, int32_t ext1, int32_t ext2) = 0;
    virtual void postData(int32_t msgType, const sp<IMemory>& dataPtr,
                          camera_frame_metadata_t *metadata) = 0;
    virtual void postDataTimestamp(nsecs_t timestamp, int32_t msgType, const sp<IMemory>& dataPtr) = 0;
};

class Camera;

template <>
struct CameraTraits<Camera>
{
    typedef CameraListener        TCamListener;
    typedef ICamera               TCamUser;
    typedef ICameraClient         TCamCallbacks;
    typedef status_t (ICameraService::*TCamConnectService)(const sp<ICameraClient>&,
                                                           int, const String16&, int,
                                                           /*out*/
                                                           sp<ICamera>&);
    static TCamConnectService     fnConnectService;
};


class Camera :
    public CameraBase<Camera>,
    public BnCameraClient
{
public:
    enum {
        USE_CALLING_UID = ICameraService::USE_CALLING_UID
    };

            // construct a camera client from an existing remote
    static  sp<Camera>  create(const sp<ICamera>& camera);
    static  sp<Camera>  connect(int cameraId,
                                const String16& clientPackageName,
                                int clientUid);

            virtual     ~Camera();

            status_t    reconnect();
            status_t    lock();
            status_t    unlock();

            // pass the buffered IGraphicBufferProducer to the camera service
            status_t    setPreviewTarget(const sp<IGraphicBufferProducer>& bufferProducer);

            // start preview mode, must call setPreviewTarget first
            status_t    startPreview();

            // stop preview mode
            void        stopPreview();

            // get preview state
            bool        previewEnabled();

            // start recording mode, must call setPreviewTarget first
            status_t    startRecording();

            // stop recording mode
            void        stopRecording();

            // get recording state
            bool        recordingEnabled();

            // release a recording frame
            void        releaseRecordingFrame(const sp<IMemory>& mem);

            // autoFocus - status returned from callback
            status_t    autoFocus();

            // cancel auto focus
            status_t    cancelAutoFocus();

            // take a picture - picture returned from callback
            status_t    takePicture(int msgType);

            // set preview/capture parameters - key/value pairs
            status_t    setParameters(const String8& params);

            // get preview/capture parameters - key/value pairs
            String8     getParameters() const;

            // send command to camera driver
            status_t    sendCommand(int32_t cmd, int32_t arg1, int32_t arg2);

            // tell camera hal to store meta data or real YUV in video buffers.
            status_t    storeMetaDataInBuffers(bool enabled);

            void        setListener(const sp<CameraListener>& listener);
            void        setRecordingProxyListener(const sp<ICameraRecordingProxyListener>& listener);

            // Configure preview callbacks to app. Only one of the older
            // callbacks or the callback surface can be active at the same time;
            // enabling one will disable the other if active. Flags can be
            // disabled by calling it with CAMERA_FRAME_CALLBACK_FLAG_NOOP, and
            // Target by calling it with a NULL interface.
            void        setPreviewCallbackFlags(int preview_callback_flag);
            status_t    setPreviewCallbackTarget(
                    const sp<IGraphicBufferProducer>& callbackProducer);

            sp<ICameraRecordingProxy> getRecordingProxy();

    // ICameraClient interface
    virtual void        notifyCallback(int32_t msgType, int32_t ext, int32_t ext2);
    virtual void        dataCallback(int32_t msgType, const sp<IMemory>& dataPtr,
                                     camera_frame_metadata_t *metadata);
    virtual void        dataCallbackTimestamp(nsecs_t timestamp, int32_t msgType, const sp<IMemory>& dataPtr);

    class RecordingProxy : public BnCameraRecordingProxy
    {
    public:
        RecordingProxy(const sp<Camera>& camera);

        // ICameraRecordingProxy interface
        virtual status_t startRecording(const sp<ICameraRecordingProxyListener>& listener);
        virtual void stopRecording();
        virtual void releaseRecordingFrame(const sp<IMemory>& mem);

    private:
        sp<Camera>         mCamera;
    };

protected:
                        Camera(int cameraId);
                        Camera(const Camera&);
                        Camera& operator=(const Camera);

    sp<ICameraRecordingProxyListener>  mRecordingProxyListener;

    friend class        CameraBase;
};

}; // namespace android

#endif
