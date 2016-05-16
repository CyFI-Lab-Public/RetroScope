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

#ifndef DRM_H_

#define DRM_H_

#include "SharedLibrary.h"

#include <media/IDrm.h>
#include <media/IDrmClient.h>
#include <utils/threads.h>

namespace android {

struct DrmFactory;
struct DrmPlugin;

struct Drm : public BnDrm,
             public IBinder::DeathRecipient,
             public DrmPluginListener {
    Drm();
    virtual ~Drm();

    virtual status_t initCheck() const;

    virtual bool isCryptoSchemeSupported(const uint8_t uuid[16], const String8 &mimeType);

    virtual status_t createPlugin(const uint8_t uuid[16]);

    virtual status_t destroyPlugin();

    virtual status_t openSession(Vector<uint8_t> &sessionId);

    virtual status_t closeSession(Vector<uint8_t> const &sessionId);

    virtual status_t
        getKeyRequest(Vector<uint8_t> const &sessionId,
                      Vector<uint8_t> const &initData,
                      String8 const &mimeType, DrmPlugin::KeyType keyType,
                      KeyedVector<String8, String8> const &optionalParameters,
                      Vector<uint8_t> &request, String8 &defaultUrl);

    virtual status_t provideKeyResponse(Vector<uint8_t> const &sessionId,
                                        Vector<uint8_t> const &response,
                                        Vector<uint8_t> &keySetId);

    virtual status_t removeKeys(Vector<uint8_t> const &keySetId);

    virtual status_t restoreKeys(Vector<uint8_t> const &sessionId,
                                 Vector<uint8_t> const &keySetId);

    virtual status_t queryKeyStatus(Vector<uint8_t> const &sessionId,
                                    KeyedVector<String8, String8> &infoMap) const;

    virtual status_t getProvisionRequest(Vector<uint8_t> &request,
                                         String8 &defaulUrl);

    virtual status_t provideProvisionResponse(Vector<uint8_t> const &response);

    virtual status_t getSecureStops(List<Vector<uint8_t> > &secureStops);

    virtual status_t releaseSecureStops(Vector<uint8_t> const &ssRelease);

    virtual status_t getPropertyString(String8 const &name, String8 &value ) const;
    virtual status_t getPropertyByteArray(String8 const &name,
                                          Vector<uint8_t> &value ) const;
    virtual status_t setPropertyString(String8 const &name, String8 const &value ) const;
    virtual status_t setPropertyByteArray(String8 const &name,
                                          Vector<uint8_t> const &value ) const;

    virtual status_t setCipherAlgorithm(Vector<uint8_t> const &sessionId,
                                        String8 const &algorithm);

    virtual status_t setMacAlgorithm(Vector<uint8_t> const &sessionId,
                                     String8 const &algorithm);

    virtual status_t encrypt(Vector<uint8_t> const &sessionId,
                             Vector<uint8_t> const &keyId,
                             Vector<uint8_t> const &input,
                             Vector<uint8_t> const &iv,
                             Vector<uint8_t> &output);

    virtual status_t decrypt(Vector<uint8_t> const &sessionId,
                             Vector<uint8_t> const &keyId,
                             Vector<uint8_t> const &input,
                             Vector<uint8_t> const &iv,
                             Vector<uint8_t> &output);

    virtual status_t sign(Vector<uint8_t> const &sessionId,
                          Vector<uint8_t> const &keyId,
                          Vector<uint8_t> const &message,
                          Vector<uint8_t> &signature);

    virtual status_t verify(Vector<uint8_t> const &sessionId,
                            Vector<uint8_t> const &keyId,
                            Vector<uint8_t> const &message,
                            Vector<uint8_t> const &signature,
                            bool &match);

    virtual status_t setListener(const sp<IDrmClient>& listener);

    virtual void sendEvent(DrmPlugin::EventType eventType, int extra,
                           Vector<uint8_t> const *sessionId,
                           Vector<uint8_t> const *data);

    virtual void binderDied(const wp<IBinder> &the_late_who);

private:
    mutable Mutex mLock;

    status_t mInitCheck;

    sp<IDrmClient> mListener;
    mutable Mutex mEventLock;
    mutable Mutex mNotifyLock;

    sp<SharedLibrary> mLibrary;
    DrmFactory *mFactory;
    DrmPlugin *mPlugin;

    static KeyedVector<Vector<uint8_t>, String8> mUUIDToLibraryPathMap;
    static KeyedVector<String8, wp<SharedLibrary> > mLibraryPathToOpenLibraryMap;
    static Mutex mMapLock;

    void findFactoryForScheme(const uint8_t uuid[16]);
    bool loadLibraryForScheme(const String8 &path, const uint8_t uuid[16]);
    void closeFactory();


    DISALLOW_EVIL_CONSTRUCTORS(Drm);
};

}  // namespace android

#endif  // CRYPTO_H_
