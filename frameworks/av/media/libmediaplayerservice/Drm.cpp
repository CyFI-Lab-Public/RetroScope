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
#define LOG_TAG "Drm"
#include <utils/Log.h>

#include <dirent.h>
#include <dlfcn.h>

#include "Drm.h"

#include <media/drm/DrmAPI.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AString.h>
#include <media/stagefright/foundation/hexdump.h>
#include <media/stagefright/MediaErrors.h>

namespace android {

KeyedVector<Vector<uint8_t>, String8> Drm::mUUIDToLibraryPathMap;
KeyedVector<String8, wp<SharedLibrary> > Drm::mLibraryPathToOpenLibraryMap;
Mutex Drm::mMapLock;

static bool operator<(const Vector<uint8_t> &lhs, const Vector<uint8_t> &rhs) {
    if (lhs.size() < rhs.size()) {
        return true;
    } else if (lhs.size() > rhs.size()) {
        return false;
    }

    return memcmp((void *)lhs.array(), (void *)rhs.array(), rhs.size()) < 0;
}

Drm::Drm()
    : mInitCheck(NO_INIT),
      mListener(NULL),
      mFactory(NULL),
      mPlugin(NULL) {
}

Drm::~Drm() {
    delete mPlugin;
    mPlugin = NULL;
    closeFactory();
}

void Drm::closeFactory() {
    delete mFactory;
    mFactory = NULL;
    mLibrary.clear();
}

status_t Drm::initCheck() const {
    return mInitCheck;
}

status_t Drm::setListener(const sp<IDrmClient>& listener)
{
    Mutex::Autolock lock(mEventLock);
    if (mListener != NULL){
        mListener->asBinder()->unlinkToDeath(this);
    }
    if (listener != NULL) {
        listener->asBinder()->linkToDeath(this);
    }
    mListener = listener;
    return NO_ERROR;
}

void Drm::sendEvent(DrmPlugin::EventType eventType, int extra,
                    Vector<uint8_t> const *sessionId,
                    Vector<uint8_t> const *data)
{
    mEventLock.lock();
    sp<IDrmClient> listener = mListener;
    mEventLock.unlock();

    if (listener != NULL) {
        Parcel obj;
        if (sessionId && sessionId->size()) {
            obj.writeInt32(sessionId->size());
            obj.write(sessionId->array(), sessionId->size());
        } else {
            obj.writeInt32(0);
        }

        if (data && data->size()) {
            obj.writeInt32(data->size());
            obj.write(data->array(), data->size());
        } else {
            obj.writeInt32(0);
        }

        Mutex::Autolock lock(mNotifyLock);
        listener->notify(eventType, extra, &obj);
    }
}

/*
 * Search the plugins directory for a plugin that supports the scheme
 * specified by uuid
 *
 * If found:
 *    mLibrary holds a strong pointer to the dlopen'd library
 *    mFactory is set to the library's factory method
 *    mInitCheck is set to OK
 *
 * If not found:
 *    mLibrary is cleared and mFactory are set to NULL
 *    mInitCheck is set to an error (!OK)
 */
void Drm::findFactoryForScheme(const uint8_t uuid[16]) {

    closeFactory();

    // lock static maps
    Mutex::Autolock autoLock(mMapLock);

    // first check cache
    Vector<uint8_t> uuidVector;
    uuidVector.appendArray(uuid, sizeof(uuid));
    ssize_t index = mUUIDToLibraryPathMap.indexOfKey(uuidVector);
    if (index >= 0) {
        if (loadLibraryForScheme(mUUIDToLibraryPathMap[index], uuid)) {
            mInitCheck = OK;
            return;
        } else {
            ALOGE("Failed to load from cached library path!");
            mInitCheck = ERROR_UNSUPPORTED;
            return;
        }
    }

    // no luck, have to search
    String8 dirPath("/vendor/lib/mediadrm");
    DIR* pDir = opendir(dirPath.string());

    if (pDir == NULL) {
        mInitCheck = ERROR_UNSUPPORTED;
        ALOGE("Failed to open plugin directory %s", dirPath.string());
        return;
    }


    struct dirent* pEntry;
    while ((pEntry = readdir(pDir))) {

        String8 pluginPath = dirPath + "/" + pEntry->d_name;

        if (pluginPath.getPathExtension() == ".so") {

            if (loadLibraryForScheme(pluginPath, uuid)) {
                mUUIDToLibraryPathMap.add(uuidVector, pluginPath);
                mInitCheck = OK;
                closedir(pDir);
                return;
            }
        }
    }

    closedir(pDir);

    ALOGE("Failed to find drm plugin");
    mInitCheck = ERROR_UNSUPPORTED;
}

bool Drm::loadLibraryForScheme(const String8 &path, const uint8_t uuid[16]) {

    // get strong pointer to open shared library
    ssize_t index = mLibraryPathToOpenLibraryMap.indexOfKey(path);
    if (index >= 0) {
        mLibrary = mLibraryPathToOpenLibraryMap[index].promote();
    } else {
        index = mLibraryPathToOpenLibraryMap.add(path, NULL);
    }

    if (!mLibrary.get()) {
        mLibrary = new SharedLibrary(path);
        if (!*mLibrary) {
            return false;
        }

        mLibraryPathToOpenLibraryMap.replaceValueAt(index, mLibrary);
    }

    typedef DrmFactory *(*CreateDrmFactoryFunc)();

    CreateDrmFactoryFunc createDrmFactory =
        (CreateDrmFactoryFunc)mLibrary->lookup("createDrmFactory");

    if (createDrmFactory == NULL ||
        (mFactory = createDrmFactory()) == NULL ||
        !mFactory->isCryptoSchemeSupported(uuid)) {
        closeFactory();
        return false;
    }
    return true;
}

bool Drm::isCryptoSchemeSupported(const uint8_t uuid[16], const String8 &mimeType) {

    Mutex::Autolock autoLock(mLock);

    if (!mFactory || !mFactory->isCryptoSchemeSupported(uuid)) {
        findFactoryForScheme(uuid);
        if (mInitCheck != OK) {
            return false;
        }
    }

    if (mimeType != "") {
        return mFactory->isContentTypeSupported(mimeType);
    }

    return true;
}

status_t Drm::createPlugin(const uint8_t uuid[16]) {
    Mutex::Autolock autoLock(mLock);

    if (mPlugin != NULL) {
        return -EINVAL;
    }

    if (!mFactory || !mFactory->isCryptoSchemeSupported(uuid)) {
        findFactoryForScheme(uuid);
    }

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    status_t result = mFactory->createDrmPlugin(uuid, &mPlugin);
    mPlugin->setListener(this);
    return result;
}

status_t Drm::destroyPlugin() {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    delete mPlugin;
    mPlugin = NULL;

    return OK;
}

status_t Drm::openSession(Vector<uint8_t> &sessionId) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->openSession(sessionId);
}

status_t Drm::closeSession(Vector<uint8_t> const &sessionId) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->closeSession(sessionId);
}

status_t Drm::getKeyRequest(Vector<uint8_t> const &sessionId,
                            Vector<uint8_t> const &initData,
                            String8 const &mimeType, DrmPlugin::KeyType keyType,
                            KeyedVector<String8, String8> const &optionalParameters,
                            Vector<uint8_t> &request, String8 &defaultUrl) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->getKeyRequest(sessionId, initData, mimeType, keyType,
                                  optionalParameters, request, defaultUrl);
}

status_t Drm::provideKeyResponse(Vector<uint8_t> const &sessionId,
                                 Vector<uint8_t> const &response,
                                 Vector<uint8_t> &keySetId) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->provideKeyResponse(sessionId, response, keySetId);
}

status_t Drm::removeKeys(Vector<uint8_t> const &keySetId) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->removeKeys(keySetId);
}

status_t Drm::restoreKeys(Vector<uint8_t> const &sessionId,
                          Vector<uint8_t> const &keySetId) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->restoreKeys(sessionId, keySetId);
}

status_t Drm::queryKeyStatus(Vector<uint8_t> const &sessionId,
                             KeyedVector<String8, String8> &infoMap) const {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->queryKeyStatus(sessionId, infoMap);
}

status_t Drm::getProvisionRequest(Vector<uint8_t> &request, String8 &defaultUrl) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->getProvisionRequest(request, defaultUrl);
}

status_t Drm::provideProvisionResponse(Vector<uint8_t> const &response) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->provideProvisionResponse(response);
}


status_t Drm::getSecureStops(List<Vector<uint8_t> > &secureStops) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->getSecureStops(secureStops);
}

status_t Drm::releaseSecureStops(Vector<uint8_t> const &ssRelease) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->releaseSecureStops(ssRelease);
}

status_t Drm::getPropertyString(String8 const &name, String8 &value ) const {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->getPropertyString(name, value);
}

status_t Drm::getPropertyByteArray(String8 const &name, Vector<uint8_t> &value ) const {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->getPropertyByteArray(name, value);
}

status_t Drm::setPropertyString(String8 const &name, String8 const &value ) const {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->setPropertyString(name, value);
}

status_t Drm::setPropertyByteArray(String8 const &name,
                                   Vector<uint8_t> const &value ) const {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->setPropertyByteArray(name, value);
}


status_t Drm::setCipherAlgorithm(Vector<uint8_t> const &sessionId,
                                 String8 const &algorithm) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->setCipherAlgorithm(sessionId, algorithm);
}

status_t Drm::setMacAlgorithm(Vector<uint8_t> const &sessionId,
                              String8 const &algorithm) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->setMacAlgorithm(sessionId, algorithm);
}

status_t Drm::encrypt(Vector<uint8_t> const &sessionId,
                      Vector<uint8_t> const &keyId,
                      Vector<uint8_t> const &input,
                      Vector<uint8_t> const &iv,
                      Vector<uint8_t> &output) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->encrypt(sessionId, keyId, input, iv, output);
}

status_t Drm::decrypt(Vector<uint8_t> const &sessionId,
                      Vector<uint8_t> const &keyId,
                      Vector<uint8_t> const &input,
                      Vector<uint8_t> const &iv,
                      Vector<uint8_t> &output) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->decrypt(sessionId, keyId, input, iv, output);
}

status_t Drm::sign(Vector<uint8_t> const &sessionId,
                   Vector<uint8_t> const &keyId,
                   Vector<uint8_t> const &message,
                   Vector<uint8_t> &signature) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->sign(sessionId, keyId, message, signature);
}

status_t Drm::verify(Vector<uint8_t> const &sessionId,
                     Vector<uint8_t> const &keyId,
                     Vector<uint8_t> const &message,
                     Vector<uint8_t> const &signature,
                     bool &match) {
    Mutex::Autolock autoLock(mLock);

    if (mInitCheck != OK) {
        return mInitCheck;
    }

    if (mPlugin == NULL) {
        return -EINVAL;
    }

    return mPlugin->verify(sessionId, keyId, message, signature, match);
}

void Drm::binderDied(const wp<IBinder> &the_late_who)
{
    delete mPlugin;
    mPlugin = NULL;
    closeFactory();
    mListener.clear();
}

}  // namespace android
