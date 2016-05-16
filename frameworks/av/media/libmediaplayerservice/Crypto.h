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

#ifndef CRYPTO_H_

#define CRYPTO_H_

#include <media/ICrypto.h>
#include <utils/threads.h>
#include <utils/KeyedVector.h>

#include "SharedLibrary.h"

namespace android {

struct CryptoFactory;
struct CryptoPlugin;

struct Crypto : public BnCrypto {
    Crypto();
    virtual ~Crypto();

    virtual status_t initCheck() const;

    virtual bool isCryptoSchemeSupported(const uint8_t uuid[16]);

    virtual status_t createPlugin(
            const uint8_t uuid[16], const void *data, size_t size);

    virtual status_t destroyPlugin();

    virtual bool requiresSecureDecoderComponent(
            const char *mime) const;

    virtual ssize_t decrypt(
            bool secure,
            const uint8_t key[16],
            const uint8_t iv[16],
            CryptoPlugin::Mode mode,
            const void *srcPtr,
            const CryptoPlugin::SubSample *subSamples, size_t numSubSamples,
            void *dstPtr,
            AString *errorDetailMsg);

private:
    mutable Mutex mLock;

    status_t mInitCheck;
    sp<SharedLibrary> mLibrary;
    CryptoFactory *mFactory;
    CryptoPlugin *mPlugin;

    static KeyedVector<Vector<uint8_t>, String8> mUUIDToLibraryPathMap;
    static KeyedVector<String8, wp<SharedLibrary> > mLibraryPathToOpenLibraryMap;
    static Mutex mMapLock;

    void findFactoryForScheme(const uint8_t uuid[16]);
    bool loadLibraryForScheme(const String8 &path, const uint8_t uuid[16]);
    void closeFactory();

    DISALLOW_EVIL_CONSTRUCTORS(Crypto);
};

}  // namespace android

#endif  // CRYPTO_H_
