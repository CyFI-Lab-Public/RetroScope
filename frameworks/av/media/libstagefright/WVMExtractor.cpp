/*
 * Copyright (C) 2010 The Android Open Source Project
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

#define LOG_TAG "WVMExtractor"
#include <utils/Log.h>

#include "include/WVMExtractor.h"

#include <arpa/inet.h>
#include <utils/String8.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/Utils.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MetaData.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaBuffer.h>
#include <dlfcn.h>

#include <utils/Errors.h>

/* The extractor lifetime is short - just long enough to get
 * the media sources constructed - so the shared lib needs to remain open
 * beyond the lifetime of the extractor.  So keep the handle as a global
 * rather than a member of the extractor
 */
void *gVendorLibHandle = NULL;

namespace android {

static Mutex gWVMutex;

WVMExtractor::WVMExtractor(const sp<DataSource> &source)
    : mDataSource(source)
{
    Mutex::Autolock autoLock(gWVMutex);

    if (!getVendorLibHandle()) {
        return;
    }

    typedef WVMLoadableExtractor *(*GetInstanceFunc)(sp<DataSource>);
    GetInstanceFunc getInstanceFunc =
        (GetInstanceFunc) dlsym(gVendorLibHandle,
                "_ZN7android11GetInstanceENS_2spINS_10DataSourceEEE");

    if (getInstanceFunc) {
        if (source->DrmInitialization(
                MEDIA_MIMETYPE_CONTAINER_WVM) != NULL) {
            mImpl = (*getInstanceFunc)(source);
            CHECK(mImpl != NULL);
            setDrmFlag(true);
        } else {
            ALOGE("Drm manager failed to initialize.");
        }
    } else {
        ALOGE("Failed to locate GetInstance in libwvm.so");
    }
}

static void init_routine()
{
    gVendorLibHandle = dlopen("libwvm.so", RTLD_NOW);
    if (gVendorLibHandle == NULL) {
        ALOGE("Failed to open libwvm.so");
    }
}

bool WVMExtractor::getVendorLibHandle()
{
    static pthread_once_t sOnceControl = PTHREAD_ONCE_INIT;
    pthread_once(&sOnceControl, init_routine);

    return gVendorLibHandle != NULL;
}

WVMExtractor::~WVMExtractor() {
}

size_t WVMExtractor::countTracks() {
    return (mImpl != NULL) ? mImpl->countTracks() : 0;
}

sp<MediaSource> WVMExtractor::getTrack(size_t index) {
    if (mImpl == NULL) {
        return NULL;
    }
    return mImpl->getTrack(index);
}

sp<MetaData> WVMExtractor::getTrackMetaData(size_t index, uint32_t flags) {
    if (mImpl == NULL) {
        return NULL;
    }
    return mImpl->getTrackMetaData(index, flags);
}

sp<MetaData> WVMExtractor::getMetaData() {
    if (mImpl == NULL) {
        return NULL;
    }
    return mImpl->getMetaData();
}

int64_t WVMExtractor::getCachedDurationUs(status_t *finalStatus) {
    if (mImpl == NULL) {
        return 0;
    }

    return mImpl->getCachedDurationUs(finalStatus);
}

status_t WVMExtractor::getEstimatedBandwidthKbps(int32_t *kbps) {
    if (mImpl == NULL) {
        return UNKNOWN_ERROR;
    }

    return mImpl->getEstimatedBandwidthKbps(kbps);
}


void WVMExtractor::setAdaptiveStreamingMode(bool adaptive) {
    if (mImpl != NULL) {
        mImpl->setAdaptiveStreamingMode(adaptive);
    }
}

void WVMExtractor::setCryptoPluginMode(bool cryptoPluginMode) {
    if (mImpl != NULL) {
        mImpl->setCryptoPluginMode(cryptoPluginMode);
    }
}

void WVMExtractor::setUID(uid_t uid) {
    if (mImpl != NULL) {
        mImpl->setUID(uid);
    }
}

status_t WVMExtractor::getError() {
    if (mImpl == NULL) {
       return UNKNOWN_ERROR;
    }

    return mImpl->getError();
}

void WVMExtractor::setError(status_t err) {
    if (mImpl != NULL) {
        mImpl->setError(err);
    }
}

bool SniffWVM(
    const sp<DataSource> &source, String8 *mimeType, float *confidence,
        sp<AMessage> *) {

    Mutex::Autolock autoLock(gWVMutex);

    if (!WVMExtractor::getVendorLibHandle()) {
        return false;
    }

    typedef WVMLoadableExtractor *(*SnifferFunc)(const sp<DataSource>&);
    SnifferFunc snifferFunc =
        (SnifferFunc) dlsym(gVendorLibHandle,
                            "_ZN7android15IsWidevineMediaERKNS_2spINS_10DataSourceEEE");

    if (snifferFunc) {
        if ((*snifferFunc)(source)) {
            *mimeType = MEDIA_MIMETYPE_CONTAINER_WVM;
            *confidence = 10.0f;
            return true;
        }
    } else {
        ALOGE("IsWidevineMedia not found in libwvm.so");
    }

    return false;
}

} //namespace android

