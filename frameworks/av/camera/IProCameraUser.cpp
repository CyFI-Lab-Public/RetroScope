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

// #define LOG_NDEBUG 0
#define LOG_TAG "IProCameraUser"
#include <utils/Log.h>
#include <stdint.h>
#include <sys/types.h>
#include <binder/Parcel.h>
#include <camera/IProCameraUser.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include "camera/CameraMetadata.h"

namespace android {

enum {
    DISCONNECT = IBinder::FIRST_CALL_TRANSACTION,
    CONNECT,
    EXCLUSIVE_TRY_LOCK,
    EXCLUSIVE_LOCK,
    EXCLUSIVE_UNLOCK,
    HAS_EXCLUSIVE_LOCK,
    SUBMIT_REQUEST,
    CANCEL_REQUEST,
    DELETE_STREAM,
    CREATE_STREAM,
    CREATE_DEFAULT_REQUEST,
    GET_CAMERA_INFO,
};

class BpProCameraUser: public BpInterface<IProCameraUser>
{
public:
    BpProCameraUser(const sp<IBinder>& impl)
        : BpInterface<IProCameraUser>(impl)
    {
    }

    // disconnect from camera service
    void disconnect()
    {
        ALOGV("disconnect");
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        remote()->transact(DISCONNECT, data, &reply);
        reply.readExceptionCode();
    }

    virtual status_t connect(const sp<IProCameraCallbacks>& cameraClient)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeStrongBinder(cameraClient->asBinder());
        remote()->transact(CONNECT, data, &reply);
        return reply.readInt32();
    }

    /* Shared ProCameraUser */

    virtual status_t exclusiveTryLock()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        remote()->transact(EXCLUSIVE_TRY_LOCK, data, &reply);
        return reply.readInt32();
    }
    virtual status_t exclusiveLock()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        remote()->transact(EXCLUSIVE_LOCK, data, &reply);
        return reply.readInt32();
    }

    virtual status_t exclusiveUnlock()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        remote()->transact(EXCLUSIVE_UNLOCK, data, &reply);
        return reply.readInt32();
    }

    virtual bool hasExclusiveLock()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        remote()->transact(HAS_EXCLUSIVE_LOCK, data, &reply);
        return !!reply.readInt32();
    }

    virtual int submitRequest(camera_metadata_t* metadata, bool streaming)
    {

        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());

        // arg0+arg1
        CameraMetadata::writeToParcel(data, metadata);

        // arg2 = streaming (bool)
        data.writeInt32(streaming);

        remote()->transact(SUBMIT_REQUEST, data, &reply);
        return reply.readInt32();
    }

    virtual status_t cancelRequest(int requestId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeInt32(requestId);

        remote()->transact(CANCEL_REQUEST, data, &reply);
        return reply.readInt32();
    }

    virtual status_t deleteStream(int streamId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeInt32(streamId);

        remote()->transact(DELETE_STREAM, data, &reply);
        return reply.readInt32();
    }

    virtual status_t createStream(int width, int height, int format,
                          const sp<IGraphicBufferProducer>& bufferProducer,
                          /*out*/
                          int* streamId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeInt32(width);
        data.writeInt32(height);
        data.writeInt32(format);

        sp<IBinder> b(bufferProducer->asBinder());
        data.writeStrongBinder(b);

        remote()->transact(CREATE_STREAM, data, &reply);

        int sId = reply.readInt32();
        if (streamId) {
            *streamId = sId;
        }
        return reply.readInt32();
    }

    // Create a request object from a template.
    virtual status_t createDefaultRequest(int templateId,
                                 /*out*/
                                  camera_metadata** request)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeInt32(templateId);
        remote()->transact(CREATE_DEFAULT_REQUEST, data, &reply);
        CameraMetadata::readFromParcel(reply, /*out*/request);
        return reply.readInt32();
    }


    virtual status_t getCameraInfo(int cameraId, camera_metadata** info)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IProCameraUser::getInterfaceDescriptor());
        data.writeInt32(cameraId);
        remote()->transact(GET_CAMERA_INFO, data, &reply);
        CameraMetadata::readFromParcel(reply, /*out*/info);
        return reply.readInt32();
    }


private:


};

IMPLEMENT_META_INTERFACE(ProCameraUser, "android.hardware.IProCameraUser");

// ----------------------------------------------------------------------

status_t BnProCameraUser::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case DISCONNECT: {
            ALOGV("DISCONNECT");
            CHECK_INTERFACE(IProCameraUser, data, reply);
            disconnect();
            reply->writeNoException();
            return NO_ERROR;
        } break;
        case CONNECT: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            sp<IProCameraCallbacks> cameraClient =
                   interface_cast<IProCameraCallbacks>(data.readStrongBinder());
            reply->writeInt32(connect(cameraClient));
            return NO_ERROR;
        } break;

        /* Shared ProCameraUser */
        case EXCLUSIVE_TRY_LOCK: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            reply->writeInt32(exclusiveTryLock());
            return NO_ERROR;
        } break;
        case EXCLUSIVE_LOCK: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            reply->writeInt32(exclusiveLock());
            return NO_ERROR;
        } break;
        case EXCLUSIVE_UNLOCK: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            reply->writeInt32(exclusiveUnlock());
            return NO_ERROR;
        } break;
        case HAS_EXCLUSIVE_LOCK: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            reply->writeInt32(hasExclusiveLock());
            return NO_ERROR;
        } break;
        case SUBMIT_REQUEST: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            camera_metadata_t* metadata;
            CameraMetadata::readFromParcel(data, /*out*/&metadata);

            // arg2 = streaming (bool)
            bool streaming = data.readInt32();

            // return code: requestId (int32)
            reply->writeInt32(submitRequest(metadata, streaming));

            return NO_ERROR;
        } break;
        case CANCEL_REQUEST: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            int requestId = data.readInt32();
            reply->writeInt32(cancelRequest(requestId));
            return NO_ERROR;
        } break;
        case DELETE_STREAM: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            int streamId = data.readInt32();
            reply->writeInt32(deleteStream(streamId));
            return NO_ERROR;
        } break;
        case CREATE_STREAM: {
            CHECK_INTERFACE(IProCameraUser, data, reply);
            int width, height, format;

            width = data.readInt32();
            height = data.readInt32();
            format = data.readInt32();

            sp<IGraphicBufferProducer> bp =
               interface_cast<IGraphicBufferProducer>(data.readStrongBinder());

            int streamId = -1;
            status_t ret;
            ret = createStream(width, height, format, bp, &streamId);

            reply->writeInt32(streamId);
            reply->writeInt32(ret);

            return NO_ERROR;
        } break;

        case CREATE_DEFAULT_REQUEST: {
            CHECK_INTERFACE(IProCameraUser, data, reply);

            int templateId = data.readInt32();

            camera_metadata_t* request = NULL;
            status_t ret;
            ret = createDefaultRequest(templateId, &request);

            CameraMetadata::writeToParcel(*reply, request);
            reply->writeInt32(ret);

            free_camera_metadata(request);

            return NO_ERROR;
        } break;
        case GET_CAMERA_INFO: {
            CHECK_INTERFACE(IProCameraUser, data, reply);

            int cameraId = data.readInt32();

            camera_metadata_t* info = NULL;
            status_t ret;
            ret = getCameraInfo(cameraId, &info);

            CameraMetadata::writeToParcel(*reply, info);
            reply->writeInt32(ret);

            free_camera_metadata(info);

            return NO_ERROR;
        } break;
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
