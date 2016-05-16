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

//#define LOG_NDEBUG 0
#define LOG_TAG "IDrm"
#include <utils/Log.h>

#include <binder/Parcel.h>
#include <media/IDrm.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AString.h>

namespace android {

enum {
    INIT_CHECK = IBinder::FIRST_CALL_TRANSACTION,
    IS_CRYPTO_SUPPORTED,
    CREATE_PLUGIN,
    DESTROY_PLUGIN,
    OPEN_SESSION,
    CLOSE_SESSION,
    GET_KEY_REQUEST,
    PROVIDE_KEY_RESPONSE,
    REMOVE_KEYS,
    RESTORE_KEYS,
    QUERY_KEY_STATUS,
    GET_PROVISION_REQUEST,
    PROVIDE_PROVISION_RESPONSE,
    GET_SECURE_STOPS,
    RELEASE_SECURE_STOPS,
    GET_PROPERTY_STRING,
    GET_PROPERTY_BYTE_ARRAY,
    SET_PROPERTY_STRING,
    SET_PROPERTY_BYTE_ARRAY,
    SET_CIPHER_ALGORITHM,
    SET_MAC_ALGORITHM,
    ENCRYPT,
    DECRYPT,
    SIGN,
    VERIFY,
    SET_LISTENER
};

struct BpDrm : public BpInterface<IDrm> {
    BpDrm(const sp<IBinder> &impl)
        : BpInterface<IDrm>(impl) {
    }

    virtual status_t initCheck() const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        remote()->transact(INIT_CHECK, data, &reply);

        return reply.readInt32();
    }

    virtual bool isCryptoSchemeSupported(const uint8_t uuid[16], const String8 &mimeType) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        data.write(uuid, 16);
        data.writeString8(mimeType);
        remote()->transact(IS_CRYPTO_SUPPORTED, data, &reply);

        return reply.readInt32() != 0;
    }

    virtual status_t createPlugin(const uint8_t uuid[16]) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        data.write(uuid, 16);

        remote()->transact(CREATE_PLUGIN, data, &reply);

        return reply.readInt32();
    }

    virtual status_t destroyPlugin() {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        remote()->transact(DESTROY_PLUGIN, data, &reply);

        return reply.readInt32();
    }

    virtual status_t openSession(Vector<uint8_t> &sessionId) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        remote()->transact(OPEN_SESSION, data, &reply);
        readVector(reply, sessionId);

        return reply.readInt32();
    }

    virtual status_t closeSession(Vector<uint8_t> const &sessionId) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        remote()->transact(CLOSE_SESSION, data, &reply);

        return reply.readInt32();
    }

    virtual status_t
        getKeyRequest(Vector<uint8_t> const &sessionId,
                      Vector<uint8_t> const &initData,
                      String8 const &mimeType, DrmPlugin::KeyType keyType,
                      KeyedVector<String8, String8> const &optionalParameters,
                      Vector<uint8_t> &request, String8 &defaultUrl) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, initData);
        data.writeString8(mimeType);
        data.writeInt32((uint32_t)keyType);

        data.writeInt32(optionalParameters.size());
        for (size_t i = 0; i < optionalParameters.size(); ++i) {
            data.writeString8(optionalParameters.keyAt(i));
            data.writeString8(optionalParameters.valueAt(i));
        }
        remote()->transact(GET_KEY_REQUEST, data, &reply);

        readVector(reply, request);
        defaultUrl = reply.readString8();

        return reply.readInt32();
    }

    virtual status_t provideKeyResponse(Vector<uint8_t> const &sessionId,
                                        Vector<uint8_t> const &response,
                                        Vector<uint8_t> &keySetId) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        writeVector(data, sessionId);
        writeVector(data, response);
        remote()->transact(PROVIDE_KEY_RESPONSE, data, &reply);
        readVector(reply, keySetId);

        return reply.readInt32();
    }

    virtual status_t removeKeys(Vector<uint8_t> const &keySetId) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, keySetId);
        remote()->transact(REMOVE_KEYS, data, &reply);

        return reply.readInt32();
    }

    virtual status_t restoreKeys(Vector<uint8_t> const &sessionId,
                                 Vector<uint8_t> const &keySetId) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, keySetId);
        remote()->transact(RESTORE_KEYS, data, &reply);

        return reply.readInt32();
    }

    virtual status_t queryKeyStatus(Vector<uint8_t> const &sessionId,
                                        KeyedVector<String8, String8> &infoMap) const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        remote()->transact(QUERY_KEY_STATUS, data, &reply);

        infoMap.clear();
        size_t count = reply.readInt32();
        for (size_t i = 0; i < count; i++) {
            String8 key = reply.readString8();
            String8 value = reply.readString8();
            infoMap.add(key, value);
        }
        return reply.readInt32();
    }

    virtual status_t getProvisionRequest(Vector<uint8_t> &request,
                                         String8 &defaultUrl) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        remote()->transact(GET_PROVISION_REQUEST, data, &reply);

        readVector(reply, request);
        defaultUrl = reply.readString8();

        return reply.readInt32();
    }

    virtual status_t provideProvisionResponse(Vector<uint8_t> const &response) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, response);
        remote()->transact(PROVIDE_PROVISION_RESPONSE, data, &reply);

        return reply.readInt32();
    }

    virtual status_t getSecureStops(List<Vector<uint8_t> > &secureStops) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        remote()->transact(GET_SECURE_STOPS, data, &reply);

        secureStops.clear();
        uint32_t count = reply.readInt32();
        for (size_t i = 0; i < count; i++) {
            Vector<uint8_t> secureStop;
            readVector(reply, secureStop);
            secureStops.push_back(secureStop);
        }
        return reply.readInt32();
    }

    virtual status_t releaseSecureStops(Vector<uint8_t> const &ssRelease) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, ssRelease);
        remote()->transact(RELEASE_SECURE_STOPS, data, &reply);

        return reply.readInt32();
    }

    virtual status_t getPropertyString(String8 const &name, String8 &value) const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        data.writeString8(name);
        remote()->transact(GET_PROPERTY_STRING, data, &reply);

        value = reply.readString8();
        return reply.readInt32();
    }

    virtual status_t getPropertyByteArray(String8 const &name, Vector<uint8_t> &value) const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        data.writeString8(name);
        remote()->transact(GET_PROPERTY_BYTE_ARRAY, data, &reply);

        readVector(reply, value);
        return reply.readInt32();
    }

    virtual status_t setPropertyString(String8 const &name, String8 const &value) const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        data.writeString8(name);
        data.writeString8(value);
        remote()->transact(SET_PROPERTY_STRING, data, &reply);

        return reply.readInt32();
    }

    virtual status_t setPropertyByteArray(String8 const &name,
                                          Vector<uint8_t> const &value) const {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        data.writeString8(name);
        writeVector(data, value);
        remote()->transact(SET_PROPERTY_BYTE_ARRAY, data, &reply);

        return reply.readInt32();
    }


    virtual status_t setCipherAlgorithm(Vector<uint8_t> const &sessionId,
                                        String8 const &algorithm) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        data.writeString8(algorithm);
        remote()->transact(SET_CIPHER_ALGORITHM, data, &reply);
        return reply.readInt32();
    }

    virtual status_t setMacAlgorithm(Vector<uint8_t> const &sessionId,
                                     String8 const &algorithm) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        data.writeString8(algorithm);
        remote()->transact(SET_MAC_ALGORITHM, data, &reply);
        return reply.readInt32();
    }

    virtual status_t encrypt(Vector<uint8_t> const &sessionId,
                             Vector<uint8_t> const &keyId,
                             Vector<uint8_t> const &input,
                             Vector<uint8_t> const &iv,
                             Vector<uint8_t> &output) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, keyId);
        writeVector(data, input);
        writeVector(data, iv);

        remote()->transact(ENCRYPT, data, &reply);
        readVector(reply, output);

        return reply.readInt32();
    }

    virtual status_t decrypt(Vector<uint8_t> const &sessionId,
                             Vector<uint8_t> const &keyId,
                             Vector<uint8_t> const &input,
                             Vector<uint8_t> const &iv,
                             Vector<uint8_t> &output) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, keyId);
        writeVector(data, input);
        writeVector(data, iv);

        remote()->transact(DECRYPT, data, &reply);
        readVector(reply, output);

        return reply.readInt32();
    }

    virtual status_t sign(Vector<uint8_t> const &sessionId,
                          Vector<uint8_t> const &keyId,
                          Vector<uint8_t> const &message,
                          Vector<uint8_t> &signature) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, keyId);
        writeVector(data, message);

        remote()->transact(SIGN, data, &reply);
        readVector(reply, signature);

        return reply.readInt32();
    }

    virtual status_t verify(Vector<uint8_t> const &sessionId,
                            Vector<uint8_t> const &keyId,
                            Vector<uint8_t> const &message,
                            Vector<uint8_t> const &signature,
                            bool &match) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());

        writeVector(data, sessionId);
        writeVector(data, keyId);
        writeVector(data, message);
        writeVector(data, signature);

        remote()->transact(VERIFY, data, &reply);
        match = (bool)reply.readInt32();
        return reply.readInt32();
    }

    virtual status_t setListener(const sp<IDrmClient>& listener) {
        Parcel data, reply;
        data.writeInterfaceToken(IDrm::getInterfaceDescriptor());
        data.writeStrongBinder(listener->asBinder());
        remote()->transact(SET_LISTENER, data, &reply);
        return reply.readInt32();
    }

private:
    void readVector(Parcel &reply, Vector<uint8_t> &vector) const {
        uint32_t size = reply.readInt32();
        vector.insertAt((size_t)0, size);
        reply.read(vector.editArray(), size);
    }

    void writeVector(Parcel &data, Vector<uint8_t> const &vector) const {
        data.writeInt32(vector.size());
        data.write(vector.array(), vector.size());
    }

    DISALLOW_EVIL_CONSTRUCTORS(BpDrm);
};

IMPLEMENT_META_INTERFACE(Drm, "android.drm.IDrm");

////////////////////////////////////////////////////////////////////////////////

void BnDrm::readVector(const Parcel &data, Vector<uint8_t> &vector) const {
    uint32_t size = data.readInt32();
    vector.insertAt((size_t)0, size);
    data.read(vector.editArray(), size);
}

void BnDrm::writeVector(Parcel *reply, Vector<uint8_t> const &vector) const {
    reply->writeInt32(vector.size());
    reply->write(vector.array(), vector.size());
}

status_t BnDrm::onTransact(
    uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags) {
    switch (code) {
        case INIT_CHECK:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            reply->writeInt32(initCheck());
            return OK;
        }

        case IS_CRYPTO_SUPPORTED:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            uint8_t uuid[16];
            data.read(uuid, sizeof(uuid));
            String8 mimeType = data.readString8();
            reply->writeInt32(isCryptoSchemeSupported(uuid, mimeType));

            return OK;
        }

        case CREATE_PLUGIN:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            uint8_t uuid[16];
            data.read(uuid, sizeof(uuid));
            reply->writeInt32(createPlugin(uuid));
            return OK;
        }

        case DESTROY_PLUGIN:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            reply->writeInt32(destroyPlugin());
            return OK;
        }

        case OPEN_SESSION:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId;
            status_t result = openSession(sessionId);
            writeVector(reply, sessionId);
            reply->writeInt32(result);
            return OK;
        }

        case CLOSE_SESSION:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId;
            readVector(data, sessionId);
            reply->writeInt32(closeSession(sessionId));
            return OK;
        }

        case GET_KEY_REQUEST:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, initData;

            readVector(data, sessionId);
            readVector(data, initData);
            String8 mimeType = data.readString8();
            DrmPlugin::KeyType keyType = (DrmPlugin::KeyType)data.readInt32();

            KeyedVector<String8, String8> optionalParameters;
            uint32_t count = data.readInt32();
            for (size_t i = 0; i < count; ++i) {
                String8 key, value;
                key = data.readString8();
                value = data.readString8();
                optionalParameters.add(key, value);
            }

            Vector<uint8_t> request;
            String8 defaultUrl;

            status_t result = getKeyRequest(sessionId, initData,
                                            mimeType, keyType,
                                            optionalParameters,
                                            request, defaultUrl);
            writeVector(reply, request);
            reply->writeString8(defaultUrl);
            reply->writeInt32(result);
            return OK;
        }

        case PROVIDE_KEY_RESPONSE:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, response, keySetId;
            readVector(data, sessionId);
            readVector(data, response);
            uint32_t result = provideKeyResponse(sessionId, response, keySetId);
            writeVector(reply, keySetId);
            reply->writeInt32(result);
            return OK;
        }

        case REMOVE_KEYS:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> keySetId;
            readVector(data, keySetId);
            reply->writeInt32(removeKeys(keySetId));
            return OK;
        }

        case RESTORE_KEYS:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, keySetId;
            readVector(data, sessionId);
            readVector(data, keySetId);
            reply->writeInt32(restoreKeys(sessionId, keySetId));
            return OK;
        }

        case QUERY_KEY_STATUS:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId;
            readVector(data, sessionId);
            KeyedVector<String8, String8> infoMap;
            status_t result = queryKeyStatus(sessionId, infoMap);
            size_t count = infoMap.size();
            reply->writeInt32(count);
            for (size_t i = 0; i < count; ++i) {
                reply->writeString8(infoMap.keyAt(i));
                reply->writeString8(infoMap.valueAt(i));
            }
            reply->writeInt32(result);
            return OK;
        }

        case GET_PROVISION_REQUEST:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> request;
            String8 defaultUrl;
            status_t result = getProvisionRequest(request, defaultUrl);
            writeVector(reply, request);
            reply->writeString8(defaultUrl);
            reply->writeInt32(result);
            return OK;
        }

        case PROVIDE_PROVISION_RESPONSE:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> response;
            readVector(data, response);
            reply->writeInt32(provideProvisionResponse(response));
            return OK;
        }

        case GET_SECURE_STOPS:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            List<Vector<uint8_t> > secureStops;
            status_t result = getSecureStops(secureStops);
            size_t count = secureStops.size();
            reply->writeInt32(count);
            List<Vector<uint8_t> >::iterator iter = secureStops.begin();
            while(iter != secureStops.end()) {
                size_t size = iter->size();
                reply->writeInt32(size);
                reply->write(iter->array(), iter->size());
                iter++;
            }
            reply->writeInt32(result);
            return OK;
        }

        case RELEASE_SECURE_STOPS:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> ssRelease;
            readVector(data, ssRelease);
            reply->writeInt32(releaseSecureStops(ssRelease));
            return OK;
        }

        case GET_PROPERTY_STRING:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            String8 name = data.readString8();
            String8 value;
            status_t result = getPropertyString(name, value);
            reply->writeString8(value);
            reply->writeInt32(result);
            return OK;
        }

        case GET_PROPERTY_BYTE_ARRAY:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            String8 name = data.readString8();
            Vector<uint8_t> value;
            status_t result = getPropertyByteArray(name, value);
            writeVector(reply, value);
            reply->writeInt32(result);
            return OK;
        }

        case SET_PROPERTY_STRING:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            String8 name = data.readString8();
            String8 value = data.readString8();
            reply->writeInt32(setPropertyString(name, value));
            return OK;
        }

        case SET_PROPERTY_BYTE_ARRAY:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            String8 name = data.readString8();
            Vector<uint8_t> value;
            readVector(data, value);
            reply->writeInt32(setPropertyByteArray(name, value));
            return OK;
        }

        case SET_CIPHER_ALGORITHM:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId;
            readVector(data, sessionId);
            String8 algorithm = data.readString8();
            reply->writeInt32(setCipherAlgorithm(sessionId, algorithm));
            return OK;
        }

        case SET_MAC_ALGORITHM:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId;
            readVector(data, sessionId);
            String8 algorithm = data.readString8();
            reply->writeInt32(setMacAlgorithm(sessionId, algorithm));
            return OK;
        }

        case ENCRYPT:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, keyId, input, iv, output;
            readVector(data, sessionId);
            readVector(data, keyId);
            readVector(data, input);
            readVector(data, iv);
            uint32_t result = encrypt(sessionId, keyId, input, iv, output);
            writeVector(reply, output);
            reply->writeInt32(result);
            return OK;
        }

        case DECRYPT:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, keyId, input, iv, output;
            readVector(data, sessionId);
            readVector(data, keyId);
            readVector(data, input);
            readVector(data, iv);
            uint32_t result = decrypt(sessionId, keyId, input, iv, output);
            writeVector(reply, output);
            reply->writeInt32(result);
            return OK;
        }

        case SIGN:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, keyId, message, signature;
            readVector(data, sessionId);
            readVector(data, keyId);
            readVector(data, message);
            uint32_t result = sign(sessionId, keyId, message, signature);
            writeVector(reply, signature);
            reply->writeInt32(result);
            return OK;
        }

        case VERIFY:
        {
            CHECK_INTERFACE(IDrm, data, reply);
            Vector<uint8_t> sessionId, keyId, message, signature;
            readVector(data, sessionId);
            readVector(data, keyId);
            readVector(data, message);
            readVector(data, signature);
            bool match;
            uint32_t result = verify(sessionId, keyId, message, signature, match);
            reply->writeInt32(match);
            reply->writeInt32(result);
            return OK;
        }

    case SET_LISTENER: {
        CHECK_INTERFACE(IDrm, data, reply);
        sp<IDrmClient> listener =
            interface_cast<IDrmClient>(data.readStrongBinder());
        reply->writeInt32(setListener(listener));
        return NO_ERROR;
    } break;

    default:
        return BBinder::onTransact(code, data, reply, flags);
    }
}

}  // namespace android

