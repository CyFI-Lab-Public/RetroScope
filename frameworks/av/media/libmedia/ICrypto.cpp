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
#define LOG_TAG "ICrypto"
#include <utils/Log.h>

#include <binder/Parcel.h>
#include <media/ICrypto.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AString.h>

namespace android {

enum {
    INIT_CHECK = IBinder::FIRST_CALL_TRANSACTION,
    IS_CRYPTO_SUPPORTED,
    CREATE_PLUGIN,
    DESTROY_PLUGIN,
    REQUIRES_SECURE_COMPONENT,
    DECRYPT,
};

struct BpCrypto : public BpInterface<ICrypto> {
    BpCrypto(const sp<IBinder> &impl)
        : BpInterface<ICrypto>(impl) {
    }

    virtual status_t initCheck() const {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        remote()->transact(INIT_CHECK, data, &reply);

        return reply.readInt32();
    }

    virtual bool isCryptoSchemeSupported(const uint8_t uuid[16]) {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        data.write(uuid, 16);
        remote()->transact(IS_CRYPTO_SUPPORTED, data, &reply);

        return reply.readInt32() != 0;
    }

    virtual status_t createPlugin(
            const uint8_t uuid[16], const void *opaqueData, size_t opaqueSize) {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        data.write(uuid, 16);
        data.writeInt32(opaqueSize);

        if (opaqueSize > 0) {
            data.write(opaqueData, opaqueSize);
        }

        remote()->transact(CREATE_PLUGIN, data, &reply);

        return reply.readInt32();
    }

    virtual status_t destroyPlugin() {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        remote()->transact(DESTROY_PLUGIN, data, &reply);

        return reply.readInt32();
    }

    virtual bool requiresSecureDecoderComponent(
            const char *mime) const {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        data.writeCString(mime);
        remote()->transact(REQUIRES_SECURE_COMPONENT, data, &reply);

        return reply.readInt32() != 0;
    }

    virtual ssize_t decrypt(
            bool secure,
            const uint8_t key[16],
            const uint8_t iv[16],
            CryptoPlugin::Mode mode,
            const void *srcPtr,
            const CryptoPlugin::SubSample *subSamples, size_t numSubSamples,
            void *dstPtr,
            AString *errorDetailMsg) {
        Parcel data, reply;
        data.writeInterfaceToken(ICrypto::getInterfaceDescriptor());
        data.writeInt32(secure);
        data.writeInt32(mode);

        static const uint8_t kDummy[16] = { 0 };

        if (key == NULL) {
            key = kDummy;
        }

        if (iv == NULL) {
            iv = kDummy;
        }

        data.write(key, 16);
        data.write(iv, 16);

        size_t totalSize = 0;
        for (size_t i = 0; i < numSubSamples; ++i) {
            totalSize += subSamples[i].mNumBytesOfEncryptedData;
            totalSize += subSamples[i].mNumBytesOfClearData;
        }

        data.writeInt32(totalSize);
        data.write(srcPtr, totalSize);

        data.writeInt32(numSubSamples);
        data.write(subSamples, sizeof(CryptoPlugin::SubSample) * numSubSamples);

        if (secure) {
            data.writeIntPtr((intptr_t)dstPtr);
        }

        remote()->transact(DECRYPT, data, &reply);

        ssize_t result = reply.readInt32();

        if (result >= ERROR_DRM_VENDOR_MIN && result <= ERROR_DRM_VENDOR_MAX) {
            errorDetailMsg->setTo(reply.readCString());
        }

        if (!secure && result >= 0) {
            reply.read(dstPtr, result);
        }

        return result;
    }

private:
    DISALLOW_EVIL_CONSTRUCTORS(BpCrypto);
};

IMPLEMENT_META_INTERFACE(Crypto, "android.hardware.ICrypto");

////////////////////////////////////////////////////////////////////////////////

status_t BnCrypto::onTransact(
    uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case INIT_CHECK:
        {
            CHECK_INTERFACE(ICrypto, data, reply);
            reply->writeInt32(initCheck());

            return OK;
        }

        case IS_CRYPTO_SUPPORTED:
        {
            CHECK_INTERFACE(ICrypto, data, reply);
            uint8_t uuid[16];
            data.read(uuid, sizeof(uuid));
            reply->writeInt32(isCryptoSchemeSupported(uuid));

            return OK;
        }

        case CREATE_PLUGIN:
        {
            CHECK_INTERFACE(ICrypto, data, reply);

            uint8_t uuid[16];
            data.read(uuid, sizeof(uuid));

            size_t opaqueSize = data.readInt32();
            void *opaqueData = NULL;

            if (opaqueSize > 0) {
                opaqueData = malloc(opaqueSize);
                data.read(opaqueData, opaqueSize);
            }

            reply->writeInt32(createPlugin(uuid, opaqueData, opaqueSize));

            if (opaqueData != NULL) {
                free(opaqueData);
                opaqueData = NULL;
            }

            return OK;
        }

        case DESTROY_PLUGIN:
        {
            CHECK_INTERFACE(ICrypto, data, reply);
            reply->writeInt32(destroyPlugin());

            return OK;
        }

        case REQUIRES_SECURE_COMPONENT:
        {
            CHECK_INTERFACE(ICrypto, data, reply);

            const char *mime = data.readCString();
            reply->writeInt32(requiresSecureDecoderComponent(mime));

            return OK;
        }

        case DECRYPT:
        {
            CHECK_INTERFACE(ICrypto, data, reply);

            bool secure = data.readInt32() != 0;
            CryptoPlugin::Mode mode = (CryptoPlugin::Mode)data.readInt32();

            uint8_t key[16];
            data.read(key, sizeof(key));

            uint8_t iv[16];
            data.read(iv, sizeof(iv));

            size_t totalSize = data.readInt32();
            void *srcData = malloc(totalSize);
            data.read(srcData, totalSize);

            int32_t numSubSamples = data.readInt32();

            CryptoPlugin::SubSample *subSamples =
                new CryptoPlugin::SubSample[numSubSamples];

            data.read(
                    subSamples,
                    sizeof(CryptoPlugin::SubSample) * numSubSamples);

            void *dstPtr;
            if (secure) {
                dstPtr = (void *)data.readIntPtr();
            } else {
                dstPtr = malloc(totalSize);
            }

            AString errorDetailMsg;
            ssize_t result = decrypt(
                    secure,
                    key,
                    iv,
                    mode,
                    srcData,
                    subSamples, numSubSamples,
                    dstPtr,
                    &errorDetailMsg);

            reply->writeInt32(result);

            if (result >= ERROR_DRM_VENDOR_MIN
                && result <= ERROR_DRM_VENDOR_MAX) {
                reply->writeCString(errorDetailMsg.c_str());
            }

            if (!secure) {
                if (result >= 0) {
                    CHECK_LE(result, static_cast<ssize_t>(totalSize));
                    reply->write(dstPtr, result);
                }
                free(dstPtr);
                dstPtr = NULL;
            }

            delete[] subSamples;
            subSamples = NULL;

            free(srcData);
            srcData = NULL;

            return OK;
        }

        default:
            return BBinder::onTransact(code, data, reply, flags);
    }
}

}  // namespace android

