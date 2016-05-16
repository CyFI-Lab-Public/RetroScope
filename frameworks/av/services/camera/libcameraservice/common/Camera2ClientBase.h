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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2CLIENT_BASE_H
#define ANDROID_SERVERS_CAMERA_CAMERA2CLIENT_BASE_H

#include "common/CameraDeviceBase.h"

namespace android {

class IMemory;

class CameraService;

template <typename TClientBase>
class Camera2ClientBase :
        public TClientBase,
        public CameraDeviceBase::NotificationListener
{
public:
    typedef typename TClientBase::TCamCallbacks TCamCallbacks;

    /**
     * Base binder interface (see ICamera/IProCameraUser for details)
     */
    virtual status_t      connect(const sp<TCamCallbacks>& callbacks);
    virtual void          disconnect();

    /**
     * Interface used by CameraService
     */

    Camera2ClientBase();

    // TODO: too many params, move into a ClientArgs<T>
    Camera2ClientBase(const sp<CameraService>& cameraService,
                      const sp<TCamCallbacks>& remoteCallback,
                      const String16& clientPackageName,
                      int cameraId,
                      int cameraFacing,
                      int clientPid,
                      uid_t clientUid,
                      int servicePid);
    virtual ~Camera2ClientBase();

    virtual status_t      initialize(camera_module_t *module);
    virtual status_t      dump(int fd, const Vector<String16>& args);

    /**
     * CameraDeviceBase::NotificationListener implementation
     */

    virtual void          notifyError(int errorCode, int arg1, int arg2);
    virtual void          notifyIdle();
    virtual void          notifyShutter(int requestId, nsecs_t timestamp);
    virtual void          notifyAutoFocus(uint8_t newState, int triggerId);
    virtual void          notifyAutoExposure(uint8_t newState, int triggerId);
    virtual void          notifyAutoWhitebalance(uint8_t newState,
                                                 int triggerId);


    int                   getCameraId() const;
    const sp<CameraDeviceBase>&
                          getCameraDevice();
    const sp<CameraService>&
                          getCameraService();

    /**
     * Interface used by independent components of CameraClient2Base.
     */

    // Simple class to ensure that access to TCamCallbacks is serialized
    // by requiring mRemoteCallbackLock to be locked before access to
    // mRemoteCallback is possible.
    class SharedCameraCallbacks {
      public:
        class Lock {
          public:
            Lock(SharedCameraCallbacks &client);
            ~Lock();
            sp<TCamCallbacks> &mRemoteCallback;
          private:
            SharedCameraCallbacks &mSharedClient;
        };
        SharedCameraCallbacks(const sp<TCamCallbacks>& client);
        SharedCameraCallbacks();
        SharedCameraCallbacks& operator=(const sp<TCamCallbacks>& client);
        void clear();
      private:
        sp<TCamCallbacks> mRemoteCallback;
        mutable Mutex mRemoteCallbackLock;
    } mSharedCameraCallbacks;

protected:

    virtual sp<IBinder> asBinderWrapper() {
        return IInterface::asBinder();
    }

    virtual status_t      dumpDevice(int fd, const Vector<String16>& args);

    /** Binder client interface-related private members */

    // Mutex that must be locked by methods implementing the binder client
    // interface. Ensures serialization between incoming client calls.
    // All methods in this class hierarchy that append 'L' to the name assume
    // that mBinderSerializationLock is locked when they're called
    mutable Mutex         mBinderSerializationLock;

    /** CameraDeviceBase instance wrapping HAL2+ entry */

    sp<CameraDeviceBase>  mDevice;

    /** Utility members */

    // Verify that caller is the owner of the camera
    status_t              checkPid(const char *checkLocation) const;

    virtual void          detachDevice();
};

}; // namespace android

#endif
