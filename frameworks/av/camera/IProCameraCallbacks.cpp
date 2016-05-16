/*
**
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "IProCameraCallbacks"
#include <utils/Log.h>
#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <utils/Mutex.h>

#include <camera/IProCameraCallbacks.h>

#include "camera/CameraMetadata.h"

namespace android {

enum {
    NOTIFY_CALLBACK = IBinder::FIRST_CALL_TRANSACTION,
    LOCK_STATUS_CHANGED,
    RESULT_RECEIVED,
};

class BpProCameraCallbacks: public BpInterface<IProCameraCallbacks>
{
public:
    BpProCameraCallbacks(const sp<IBinder>& impl)
        : BpInterface<IProCameraCallbacks>(impl)
    {
    }

    // generic callback from camera service to app
    void notifyCallback(int32_t msgType, int32_t ext1, int32_t ext2)
    {
        ALOGV("notifyCallback");
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraCallbacks::getInterfaceDescriptor());
        data.writeInt32(msgType);
        data.writeInt32(ext1);
        data.writeInt32(ext2);
        remote()->transact(NOTIFY_CALLBACK, data, &reply, IBinder::FLAG_ONEWAY);
    }

    void onLockStatusChanged(LockStatus newLockStatus) {
        ALOGV("onLockStatusChanged");
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraCallbacks::getInterfaceDescriptor());
        data.writeInt32(newLockStatus);
        remote()->transact(LOCK_STATUS_CHANGED, data, &reply,
                           IBinder::FLAG_ONEWAY);
    }

    void onResultReceived(int32_t requestId, camera_metadata* result) {
        ALOGV("onResultReceived");
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraCallbacks::getInterfaceDescriptor());
        data.writeInt32(requestId);
        CameraMetadata::writeToParcel(data, result);
        remote()->transact(RESULT_RECEIVED, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(ProCameraCallbacks,
                                        "android.hardware.IProCameraCallbacks");

// ----------------------------------------------------------------------

status_t BnProCameraCallbacks::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    ALOGV("onTransact - code = %d", code);
    switch(code) {
        case NOTIFY_CALLBACK: {
            ALOGV("NOTIFY_CALLBACK");
            CHECK_INTERFACE(IProCameraCallbacks, data, reply);
            int32_t msgType = data.readInt32();
            int32_t ext1 = data.readInt32();
            int32_t ext2 = data.readInt32();
            notifyCallback(msgType, ext1, ext2);
            return NO_ERROR;
        } break;
        case LOCK_STATUS_CHANGED: {
            ALOGV("LOCK_STATUS_CHANGED");
            CHECK_INTERFACE(IProCameraCallbacks, data, reply);
            LockStatus newLockStatus
                                 = static_cast<LockStatus>(data.readInt32());
            onLockStatusChanged(newLockStatus);
            return NO_ERROR;
        } break;
        case RESULT_RECEIVED: {
            ALOGV("RESULT_RECEIVED");
            CHECK_INTERFACE(IProCameraCallbacks, data, reply);
            int32_t requestId = data.readInt32();
            camera_metadata_t *result = NULL;
            CameraMetadata::readFromParcel(data, &result);
            onResultReceived(requestId, result);
            return NO_ERROR;
            break;
        }
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android

