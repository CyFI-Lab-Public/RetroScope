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

#ifndef ANDROID_HARDWARE_IPROCAMERA_CALLBACKS_H
#define ANDROID_HARDWARE_IPROCAMERA_CALLBACKS_H

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <binder/IMemory.h>
#include <utils/Timers.h>
#include <system/camera.h>

struct camera_metadata;

namespace android {

class IProCameraCallbacks : public IInterface
{
    /**
     * Keep up-to-date with IProCameraCallbacks.aidl in frameworks/base
     */
public:
    DECLARE_META_INTERFACE(ProCameraCallbacks);

    virtual void            notifyCallback(int32_t msgType,
                                           int32_t ext1,
                                           int32_t ext2) = 0;

    enum LockStatus {
        LOCK_ACQUIRED,
        LOCK_RELEASED,
        LOCK_STOLEN,
    };

    virtual void            onLockStatusChanged(LockStatus newLockStatus) = 0;

    /** Missing by design: implementation is client-side in ProCamera.cpp **/
    // virtual void onBufferReceived(int streamId,
    //                               const CpuConsumer::LockedBufer& buf);
    virtual void            onResultReceived(int32_t requestId,
                                             camera_metadata* result) = 0;
};

// ----------------------------------------------------------------------------

class BnProCameraCallbacks : public BnInterface<IProCameraCallbacks>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
