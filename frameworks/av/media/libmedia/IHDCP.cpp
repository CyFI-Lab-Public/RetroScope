/*
 * Copyright (C) 2012 The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "IHDCP"
#include <utils/Log.h>

#include <binder/Parcel.h>
#include <media/IHDCP.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/foundation/ADebug.h>

namespace android {

enum {
    OBSERVER_NOTIFY = IBinder::FIRST_CALL_TRANSACTION,
    HDCP_SET_OBSERVER,
    HDCP_INIT_ASYNC,
    HDCP_SHUTDOWN_ASYNC,
    HDCP_GET_CAPS,
    HDCP_ENCRYPT,
    HDCP_ENCRYPT_NATIVE,
    HDCP_DECRYPT,
};

struct BpHDCPObserver : public BpInterface<IHDCPObserver> {
    BpHDCPObserver(const sp<IBinder> &impl)
        : BpInterface<IHDCPObserver>(impl) {
    }

    virtual void notify(
            int msg, int ext1, int ext2, const Parcel *obj) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCPObserver::getInterfaceDescriptor());
        data.writeInt32(msg);
        data.writeInt32(ext1);
        data.writeInt32(ext2);
        if (obj && obj->dataSize() > 0) {
            data.appendFrom(const_cast<Parcel *>(obj), 0, obj->dataSize());
        }
        remote()->transact(OBSERVER_NOTIFY, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(HDCPObserver, "android.hardware.IHDCPObserver");

struct BpHDCP : public BpInterface<IHDCP> {
    BpHDCP(const sp<IBinder> &impl)
        : BpInterface<IHDCP>(impl) {
    }

    virtual status_t setObserver(const sp<IHDCPObserver> &observer) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        data.writeStrongBinder(observer->asBinder());
        remote()->transact(HDCP_SET_OBSERVER, data, &reply);
        return reply.readInt32();
    }

    virtual status_t initAsync(const char *host, unsigned port) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        data.writeCString(host);
        data.writeInt32(port);
        remote()->transact(HDCP_INIT_ASYNC, data, &reply);
        return reply.readInt32();
    }

    virtual status_t shutdownAsync() {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        remote()->transact(HDCP_SHUTDOWN_ASYNC, data, &reply);
        return reply.readInt32();
    }

    virtual uint32_t getCaps() {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        remote()->transact(HDCP_GET_CAPS, data, &reply);
        return reply.readInt32();
    }

    virtual status_t encrypt(
            const void *inData, size_t size, uint32_t streamCTR,
            uint64_t *outInputCTR, void *outData) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        data.writeInt32(size);
        data.write(inData, size);
        data.writeInt32(streamCTR);
        remote()->transact(HDCP_ENCRYPT, data, &reply);

        status_t err = reply.readInt32();

        if (err != OK) {
            *outInputCTR = 0;

            return err;
        }

        *outInputCTR = reply.readInt64();
        reply.read(outData, size);

        return err;
    }

    virtual status_t encryptNative(
            const sp<GraphicBuffer> &graphicBuffer,
            size_t offset, size_t size, uint32_t streamCTR,
            uint64_t *outInputCTR, void *outData) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        data.write(*graphicBuffer);
        data.writeInt32(offset);
        data.writeInt32(size);
        data.writeInt32(streamCTR);
        remote()->transact(HDCP_ENCRYPT_NATIVE, data, &reply);

        status_t err = reply.readInt32();

        if (err != OK) {
            *outInputCTR = 0;
            return err;
        }

        *outInputCTR = reply.readInt64();
        reply.read(outData, size);

        return err;
    }

    virtual status_t decrypt(
            const void *inData, size_t size,
            uint32_t streamCTR, uint64_t inputCTR,
            void *outData) {
        Parcel data, reply;
        data.writeInterfaceToken(IHDCP::getInterfaceDescriptor());
        data.writeInt32(size);
        data.write(inData, size);
        data.writeInt32(streamCTR);
        data.writeInt64(inputCTR);
        remote()->transact(HDCP_DECRYPT, data, &reply);

        status_t err = reply.readInt32();

        if (err != OK) {
            return err;
        }

        reply.read(outData, size);

        return err;
    }
};

IMPLEMENT_META_INTERFACE(HDCP, "android.hardware.IHDCP");

status_t BnHDCPObserver::onTransact(
        uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case OBSERVER_NOTIFY:
        {
            CHECK_INTERFACE(IHDCPObserver, data, reply);

            int msg = data.readInt32();
            int ext1 = data.readInt32();
            int ext2 = data.readInt32();

            Parcel obj;
            if (data.dataAvail() > 0) {
                obj.appendFrom(
                        const_cast<Parcel *>(&data),
                        data.dataPosition(),
                        data.dataAvail());
            }

            notify(msg, ext1, ext2, &obj);

            return OK;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

status_t BnHDCP::onTransact(
        uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case HDCP_SET_OBSERVER:
        {
            CHECK_INTERFACE(IHDCP, data, reply);

            sp<IHDCPObserver> observer =
                interface_cast<IHDCPObserver>(data.readStrongBinder());

            reply->writeInt32(setObserver(observer));
            return OK;
        }

        case HDCP_INIT_ASYNC:
        {
            CHECK_INTERFACE(IHDCP, data, reply);

            const char *host = data.readCString();
            unsigned port = data.readInt32();

            reply->writeInt32(initAsync(host, port));
            return OK;
        }

        case HDCP_SHUTDOWN_ASYNC:
        {
            CHECK_INTERFACE(IHDCP, data, reply);

            reply->writeInt32(shutdownAsync());
            return OK;
        }

        case HDCP_GET_CAPS:
        {
            CHECK_INTERFACE(IHDCP, data, reply);

            reply->writeInt32(getCaps());
            return OK;
        }

        case HDCP_ENCRYPT:
        {
            size_t size = data.readInt32();

            void *inData = malloc(2 * size);
            void *outData = (uint8_t *)inData + size;

            data.read(inData, size);

            uint32_t streamCTR = data.readInt32();
            uint64_t inputCTR;
            status_t err = encrypt(inData, size, streamCTR, &inputCTR, outData);

            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt64(inputCTR);
                reply->write(outData, size);
            }

            free(inData);
            inData = outData = NULL;

            return OK;
        }

        case HDCP_ENCRYPT_NATIVE:
        {
            CHECK_INTERFACE(IHDCP, data, reply);

            sp<GraphicBuffer> graphicBuffer = new GraphicBuffer();
            data.read(*graphicBuffer);
            size_t offset = data.readInt32();
            size_t size = data.readInt32();
            uint32_t streamCTR = data.readInt32();
            void *outData = malloc(size);
            uint64_t inputCTR;

            status_t err = encryptNative(graphicBuffer, offset, size,
                                         streamCTR, &inputCTR, outData);

            reply->writeInt32(err);

            if (err == OK) {
                reply->writeInt64(inputCTR);
                reply->write(outData, size);
            }

            free(outData);
            outData = NULL;

            return OK;
        }

        case HDCP_DECRYPT:
        {
            size_t size = data.readInt32();

            void *inData = malloc(2 * size);
            void *outData = (uint8_t *)inData + size;

            data.read(inData, size);

            uint32_t streamCTR = data.readInt32();
            uint64_t inputCTR = data.readInt64();
            status_t err = decrypt(inData, size, streamCTR, inputCTR, outData);

            reply->writeInt32(err);

            if (err == OK) {
                reply->write(outData, size);
            }

            free(inData);
            inData = outData = NULL;

            return OK;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}  // namespace android
