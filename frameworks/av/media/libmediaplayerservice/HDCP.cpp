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
#define LOG_TAG "HDCP"
#include <utils/Log.h>

#include "HDCP.h"

#include <media/stagefright/foundation/ADebug.h>

#include <dlfcn.h>

namespace android {

HDCP::HDCP(bool createEncryptionModule)
    : mIsEncryptionModule(createEncryptionModule),
      mLibHandle(NULL),
      mHDCPModule(NULL) {
    mLibHandle = dlopen("libstagefright_hdcp.so", RTLD_NOW);

    if (mLibHandle == NULL) {
        ALOGE("Unable to locate libstagefright_hdcp.so");
        return;
    }

    typedef HDCPModule *(*CreateHDCPModuleFunc)(
            void *, HDCPModule::ObserverFunc);

    CreateHDCPModuleFunc createHDCPModule =
        mIsEncryptionModule
            ? (CreateHDCPModuleFunc)dlsym(mLibHandle, "createHDCPModule")
            : (CreateHDCPModuleFunc)dlsym(
                    mLibHandle, "createHDCPModuleForDecryption");

    if (createHDCPModule == NULL) {
        ALOGE("Unable to find symbol 'createHDCPModule'.");
    } else if ((mHDCPModule = createHDCPModule(
                    this, &HDCP::ObserveWrapper)) == NULL) {
        ALOGE("createHDCPModule failed.");
    }
}

HDCP::~HDCP() {
    Mutex::Autolock autoLock(mLock);

    if (mHDCPModule != NULL) {
        delete mHDCPModule;
        mHDCPModule = NULL;
    }

    if (mLibHandle != NULL) {
        dlclose(mLibHandle);
        mLibHandle = NULL;
    }
}

status_t HDCP::setObserver(const sp<IHDCPObserver> &observer) {
    Mutex::Autolock autoLock(mLock);

    if (mHDCPModule == NULL) {
        return NO_INIT;
    }

    mObserver = observer;

    return OK;
}

status_t HDCP::initAsync(const char *host, unsigned port) {
    Mutex::Autolock autoLock(mLock);

    if (mHDCPModule == NULL) {
        return NO_INIT;
    }

    return mHDCPModule->initAsync(host, port);
}

status_t HDCP::shutdownAsync() {
    Mutex::Autolock autoLock(mLock);

    if (mHDCPModule == NULL) {
        return NO_INIT;
    }

    return mHDCPModule->shutdownAsync();
}

uint32_t HDCP::getCaps() {
    Mutex::Autolock autoLock(mLock);

    if (mHDCPModule == NULL) {
        return NO_INIT;
    }

    // TO-DO:
    // Only support HDCP_CAPS_ENCRYPT (byte-array to byte-array) for now.
    // use mHDCPModule->getCaps() when the HDCP libraries get updated.
    //return mHDCPModule->getCaps();
    return HDCPModule::HDCP_CAPS_ENCRYPT;
}

status_t HDCP::encrypt(
        const void *inData, size_t size, uint32_t streamCTR,
        uint64_t *outInputCTR, void *outData) {
    Mutex::Autolock autoLock(mLock);

    CHECK(mIsEncryptionModule);

    if (mHDCPModule == NULL) {
        *outInputCTR = 0;

        return NO_INIT;
    }

    return mHDCPModule->encrypt(inData, size, streamCTR, outInputCTR, outData);
}

status_t HDCP::encryptNative(
        const sp<GraphicBuffer> &graphicBuffer,
        size_t offset, size_t size, uint32_t streamCTR,
        uint64_t *outInputCTR, void *outData) {
    Mutex::Autolock autoLock(mLock);

    CHECK(mIsEncryptionModule);

    if (mHDCPModule == NULL) {
        *outInputCTR = 0;

        return NO_INIT;
    }

    return mHDCPModule->encryptNative(graphicBuffer->handle,
                    offset, size, streamCTR, outInputCTR, outData);
}

status_t HDCP::decrypt(
        const void *inData, size_t size,
        uint32_t streamCTR, uint64_t outInputCTR, void *outData) {
    Mutex::Autolock autoLock(mLock);

    CHECK(!mIsEncryptionModule);

    if (mHDCPModule == NULL) {
        return NO_INIT;
    }

    return mHDCPModule->decrypt(inData, size, streamCTR, outInputCTR, outData);
}

// static
void HDCP::ObserveWrapper(void *me, int msg, int ext1, int ext2) {
    static_cast<HDCP *>(me)->observe(msg, ext1, ext2);
}

void HDCP::observe(int msg, int ext1, int ext2) {
    Mutex::Autolock autoLock(mLock);

    if (mObserver != NULL) {
        mObserver->notify(msg, ext1, ext2, NULL /* obj */);
    }
}

}  // namespace android

