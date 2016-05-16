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

#define LOG_TAG "MediaLog"
//#define LOG_NDEBUG 0

#include <sys/mman.h>
#include <utils/Log.h>
#include <binder/PermissionCache.h>
#include <media/nbaio/NBLog.h>
#include <private/android_filesystem_config.h>
#include "MediaLogService.h"

namespace android {

void MediaLogService::registerWriter(const sp<IMemory>& shared, size_t size, const char *name)
{
    if (IPCThreadState::self()->getCallingUid() != AID_MEDIA || shared == 0 ||
            size < kMinSize || size > kMaxSize || name == NULL ||
            shared->size() < NBLog::Timeline::sharedSize(size)) {
        return;
    }
    sp<NBLog::Reader> reader(new NBLog::Reader(size, shared));
    NamedReader namedReader(reader, name);
    Mutex::Autolock _l(mLock);
    mNamedReaders.add(namedReader);
}

void MediaLogService::unregisterWriter(const sp<IMemory>& shared)
{
    if (IPCThreadState::self()->getCallingUid() != AID_MEDIA || shared == 0) {
        return;
    }
    Mutex::Autolock _l(mLock);
    for (size_t i = 0; i < mNamedReaders.size(); ) {
        if (mNamedReaders[i].reader()->isIMemory(shared)) {
            mNamedReaders.removeAt(i);
        } else {
            i++;
        }
    }
}

status_t MediaLogService::dump(int fd, const Vector<String16>& args)
{
    // FIXME merge with similar but not identical code at services/audioflinger/ServiceUtilities.cpp
    static const String16 sDump("android.permission.DUMP");
    if (!(IPCThreadState::self()->getCallingUid() == AID_MEDIA ||
            PermissionCache::checkCallingPermission(sDump))) {
        fdprintf(fd, "Permission Denial: can't dump media.log from pid=%d, uid=%d\n",
                IPCThreadState::self()->getCallingPid(),
                IPCThreadState::self()->getCallingUid());
        return NO_ERROR;
    }

    Vector<NamedReader> namedReaders;
    {
        Mutex::Autolock _l(mLock);
        namedReaders = mNamedReaders;
    }
    for (size_t i = 0; i < namedReaders.size(); i++) {
        const NamedReader& namedReader = namedReaders[i];
        if (fd >= 0) {
            fdprintf(fd, "\n%s:\n", namedReader.name());
        } else {
            ALOGI("%s:", namedReader.name());
        }
        namedReader.reader()->dump(fd, 0 /*indent*/);
    }
    return NO_ERROR;
}

status_t MediaLogService::onTransact(uint32_t code, const Parcel& data, Parcel* reply,
        uint32_t flags)
{
    return BnMediaLogService::onTransact(code, data, reply, flags);
}

}   // namespace android
