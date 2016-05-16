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

#ifndef HDCP_H_

#define HDCP_H_

#include <media/IHDCP.h>
#include <utils/Mutex.h>

namespace android {

struct HDCP : public BnHDCP {
    HDCP(bool createEncryptionModule);
    virtual ~HDCP();

    virtual status_t setObserver(const sp<IHDCPObserver> &observer);
    virtual status_t initAsync(const char *host, unsigned port);
    virtual status_t shutdownAsync();
    virtual uint32_t getCaps();

    virtual status_t encrypt(
            const void *inData, size_t size, uint32_t streamCTR,
            uint64_t *outInputCTR, void *outData);

    virtual status_t encryptNative(
            const sp<GraphicBuffer> &graphicBuffer,
            size_t offset, size_t size, uint32_t streamCTR,
            uint64_t *outInputCTR, void *outData);

    virtual status_t decrypt(
            const void *inData, size_t size,
            uint32_t streamCTR, uint64_t outInputCTR, void *outData);

private:
    Mutex mLock;

    bool mIsEncryptionModule;

    void *mLibHandle;
    HDCPModule *mHDCPModule;
    sp<IHDCPObserver> mObserver;

    static void ObserveWrapper(void *me, int msg, int ext1, int ext2);
    void observe(int msg, int ext1, int ext2);

    DISALLOW_EVIL_CONSTRUCTORS(HDCP);
};

}  // namespace android

#endif  // HDCP_H_

