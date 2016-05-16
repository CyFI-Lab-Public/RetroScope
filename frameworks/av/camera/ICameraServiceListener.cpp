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

#include <stdint.h>
#include <sys/types.h>

#include <binder/Parcel.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>

#include <camera/ICameraServiceListener.h>

namespace android {

namespace {
    enum {
        STATUS_CHANGED = IBinder::FIRST_CALL_TRANSACTION,
    };
}; // namespace anonymous

class BpCameraServiceListener: public BpInterface<ICameraServiceListener>
{

public:
    BpCameraServiceListener(const sp<IBinder>& impl)
        : BpInterface<ICameraServiceListener>(impl)
    {
    }

    virtual void onStatusChanged(Status status, int32_t cameraId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(
                              ICameraServiceListener::getInterfaceDescriptor());

        data.writeInt32(static_cast<int32_t>(status));
        data.writeInt32(cameraId);

        remote()->transact(STATUS_CHANGED,
                           data,
                           &reply,
                           IBinder::FLAG_ONEWAY);

        reply.readExceptionCode();
    }
};

IMPLEMENT_META_INTERFACE(CameraServiceListener,
                         "android.hardware.ICameraServiceListener");

// ----------------------------------------------------------------------

status_t BnCameraServiceListener::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case STATUS_CHANGED: {
            CHECK_INTERFACE(ICameraServiceListener, data, reply);

            Status status = static_cast<Status>(data.readInt32());
            int32_t cameraId = data.readInt32();

            onStatusChanged(status, cameraId);
            reply->writeNoException();

            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
