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
#define LOG_TAG "ICameraDeviceUser"
#include <utils/Log.h>
#include <stdint.h>
#include <sys/types.h>
#include <binder/Parcel.h>
#include <camera/camera2/ICameraDeviceUser.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <camera/CameraMetadata.h>
#include <camera/camera2/CaptureRequest.h>

namespace android {

typedef Parcel::WritableBlob WritableBlob;
typedef Parcel::ReadableBlob ReadableBlob;

enum {
    DISCONNECT = IBinder::FIRST_CALL_TRANSACTION,
    SUBMIT_REQUEST,
    CANCEL_REQUEST,
    DELETE_STREAM,
    CREATE_STREAM,
    CREATE_DEFAULT_REQUEST,
    GET_CAMERA_INFO,
    WAIT_UNTIL_IDLE,
    FLUSH
};

namespace {
    // Read empty strings without printing a false error message.
    String16 readMaybeEmptyString16(const Parcel& parcel) {
        size_t len;
        const char16_t* str = parcel.readString16Inplace(&len);
        if (str != NULL) {
            return String16(str, len);
        } else {
            return String16();
        }
    }
};

class BpCameraDeviceUser : public BpInterface<ICameraDeviceUser>
{
public:
    BpCameraDeviceUser(const sp<IBinder>& impl)
        : BpInterface<ICameraDeviceUser>(impl)
    {
    }

    // disconnect from camera service
    void disconnect()
    {
        ALOGV("disconnect");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        remote()->transact(DISCONNECT, data, &reply);
        reply.readExceptionCode();
    }

    virtual int submitRequest(sp<CaptureRequest> request, bool streaming)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());

        // arg0 = CaptureRequest
        if (request != 0) {
            data.writeInt32(1);
            request->writeToParcel(&data);
        } else {
            data.writeInt32(0);
        }

        // arg1 = streaming (bool)
        data.writeInt32(streaming);

        remote()->transact(SUBMIT_REQUEST, data, &reply);

        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual status_t cancelRequest(int requestId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        data.writeInt32(requestId);

        remote()->transact(CANCEL_REQUEST, data, &reply);

        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual status_t deleteStream(int streamId)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        data.writeInt32(streamId);

        remote()->transact(DELETE_STREAM, data, &reply);

        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual status_t createStream(int width, int height, int format,
                          const sp<IGraphicBufferProducer>& bufferProducer)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        data.writeInt32(width);
        data.writeInt32(height);
        data.writeInt32(format);

        data.writeInt32(1); // marker that bufferProducer is not null
        data.writeString16(String16("unknown_name")); // name of surface
        sp<IBinder> b(bufferProducer->asBinder());
        data.writeStrongBinder(b);

        remote()->transact(CREATE_STREAM, data, &reply);

        reply.readExceptionCode();
        return reply.readInt32();
    }

    // Create a request object from a template.
    virtual status_t createDefaultRequest(int templateId,
                                          /*out*/
                                          CameraMetadata* request)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        data.writeInt32(templateId);
        remote()->transact(CREATE_DEFAULT_REQUEST, data, &reply);

        reply.readExceptionCode();
        status_t result = reply.readInt32();

        CameraMetadata out;
        if (reply.readInt32() != 0) {
            out.readFromParcel(&reply);
        }

        if (request != NULL) {
            request->swap(out);
        }
        return result;
    }


    virtual status_t getCameraInfo(CameraMetadata* info)
    {
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        remote()->transact(GET_CAMERA_INFO, data, &reply);

        reply.readExceptionCode();
        status_t result = reply.readInt32();

        CameraMetadata out;
        if (reply.readInt32() != 0) {
            out.readFromParcel(&reply);
        }

        if (info != NULL) {
            info->swap(out);
        }

        return result;
    }

    virtual status_t waitUntilIdle()
    {
        ALOGV("waitUntilIdle");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        remote()->transact(WAIT_UNTIL_IDLE, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual status_t flush()
    {
        ALOGV("flush");
        Parcel data, reply;
        data.writeInterfaceToken(ICameraDeviceUser::getInterfaceDescriptor());
        remote()->transact(FLUSH, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

private:


};

IMPLEMENT_META_INTERFACE(CameraDeviceUser,
                         "android.hardware.camera2.ICameraDeviceUser");

// ----------------------------------------------------------------------

status_t BnCameraDeviceUser::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case DISCONNECT: {
            ALOGV("DISCONNECT");
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            disconnect();
            reply->writeNoException();
            return NO_ERROR;
        } break;
        case SUBMIT_REQUEST: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);

            // arg0 = request
            sp<CaptureRequest> request;
            if (data.readInt32() != 0) {
                request = new CaptureRequest();
                request->readFromParcel(const_cast<Parcel*>(&data));
            }

            // arg1 = streaming (bool)
            bool streaming = data.readInt32();

            // return code: requestId (int32)
            reply->writeNoException();
            reply->writeInt32(submitRequest(request, streaming));

            return NO_ERROR;
        } break;
        case CANCEL_REQUEST: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            int requestId = data.readInt32();
            reply->writeNoException();
            reply->writeInt32(cancelRequest(requestId));
            return NO_ERROR;
        } break;
        case DELETE_STREAM: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            int streamId = data.readInt32();
            reply->writeNoException();
            reply->writeInt32(deleteStream(streamId));
            return NO_ERROR;
        } break;
        case CREATE_STREAM: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            int width, height, format;

            width = data.readInt32();
            ALOGV("%s: CREATE_STREAM: width = %d", __FUNCTION__, width);
            height = data.readInt32();
            ALOGV("%s: CREATE_STREAM: height = %d", __FUNCTION__, height);
            format = data.readInt32();
            ALOGV("%s: CREATE_STREAM: format = %d", __FUNCTION__, format);

            sp<IGraphicBufferProducer> bp;
            if (data.readInt32() != 0) {
                String16 name = readMaybeEmptyString16(data);
                bp = interface_cast<IGraphicBufferProducer>(
                        data.readStrongBinder());

                ALOGV("%s: CREATE_STREAM: bp = %p, name = %s", __FUNCTION__,
                      bp.get(), String8(name).string());
            } else {
                ALOGV("%s: CREATE_STREAM: bp = unset, name = unset",
                      __FUNCTION__);
            }

            status_t ret;
            ret = createStream(width, height, format, bp);

            reply->writeNoException();
            ALOGV("%s: CREATE_STREAM: write noException", __FUNCTION__);
            reply->writeInt32(ret);
            ALOGV("%s: CREATE_STREAM: write ret = %d", __FUNCTION__, ret);

            return NO_ERROR;
        } break;

        case CREATE_DEFAULT_REQUEST: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);

            int templateId = data.readInt32();

            CameraMetadata request;
            status_t ret;
            ret = createDefaultRequest(templateId, &request);

            reply->writeNoException();
            reply->writeInt32(ret);

            // out-variables are after exception and return value
            reply->writeInt32(1); // to mark presence of metadata object
            request.writeToParcel(const_cast<Parcel*>(reply));

            return NO_ERROR;
        } break;
        case GET_CAMERA_INFO: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);

            CameraMetadata info;
            status_t ret;
            ret = getCameraInfo(&info);

            reply->writeNoException();
            reply->writeInt32(ret);

            // out-variables are after exception and return value
            reply->writeInt32(1); // to mark presence of metadata object
            info.writeToParcel(reply);

            return NO_ERROR;
        } break;
        case WAIT_UNTIL_IDLE: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            reply->writeNoException();
            reply->writeInt32(waitUntilIdle());
            return NO_ERROR;
        } break;
        case FLUSH: {
            CHECK_INTERFACE(ICameraDeviceUser, data, reply);
            reply->writeNoException();
            reply->writeInt32(flush());
            return NO_ERROR;
        }
        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

// ----------------------------------------------------------------------------

}; // namespace android
