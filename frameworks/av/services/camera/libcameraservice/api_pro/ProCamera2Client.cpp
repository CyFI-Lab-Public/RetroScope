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

#define LOG_TAG "ProCamera2Client"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>

#include <cutils/properties.h>
#include <gui/Surface.h>
#include <gui/Surface.h>

#include "api_pro/ProCamera2Client.h"
#include "common/CameraDeviceBase.h"

namespace android {
using namespace camera2;

// Interface used by CameraService

ProCamera2Client::ProCamera2Client(const sp<CameraService>& cameraService,
                                   const sp<IProCameraCallbacks>& remoteCallback,
                                   const String16& clientPackageName,
                                   int cameraId,
                                   int cameraFacing,
                                   int clientPid,
                                   uid_t clientUid,
                                   int servicePid) :
    Camera2ClientBase(cameraService, remoteCallback, clientPackageName,
                cameraId, cameraFacing, clientPid, clientUid, servicePid)
{
    ATRACE_CALL();
    ALOGI("ProCamera %d: Opened", cameraId);

    mExclusiveLock = false;
}

status_t ProCamera2Client::initialize(camera_module_t *module)
{
    ATRACE_CALL();
    status_t res;

    res = Camera2ClientBase::initialize(module);
    if (res != OK) {
        return res;
    }

    String8 threadName;
    mFrameProcessor = new FrameProcessorBase(mDevice);
    threadName = String8::format("PC2-%d-FrameProc", mCameraId);
    mFrameProcessor->run(threadName.string());

    mFrameProcessor->registerListener(FRAME_PROCESSOR_LISTENER_MIN_ID,
                                      FRAME_PROCESSOR_LISTENER_MAX_ID,
                                      /*listener*/this);

    return OK;
}

ProCamera2Client::~ProCamera2Client() {
}

status_t ProCamera2Client::exclusiveTryLock() {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);
    SharedCameraCallbacks::Lock l(mSharedCameraCallbacks);

    if (!mDevice.get()) return PERMISSION_DENIED;

    if (!mExclusiveLock) {
        mExclusiveLock = true;

        if (mRemoteCallback != NULL) {
            mRemoteCallback->onLockStatusChanged(
                              IProCameraCallbacks::LOCK_ACQUIRED);
        }

        ALOGV("%s: exclusive lock acquired", __FUNCTION__);

        return OK;
    }

    // TODO: have a PERMISSION_DENIED case for when someone else owns the lock

    // don't allow recursive locking
    ALOGW("%s: exclusive lock already exists - recursive locking is not"
          "allowed", __FUNCTION__);

    return ALREADY_EXISTS;
}

status_t ProCamera2Client::exclusiveLock() {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);
    SharedCameraCallbacks::Lock l(mSharedCameraCallbacks);

    if (!mDevice.get()) return PERMISSION_DENIED;

    /**
     * TODO: this should asynchronously 'wait' until the lock becomes available
     * if another client already has an exclusive lock.
     *
     * once we have proper sharing support this will need to do
     * more than just return immediately
     */
    if (!mExclusiveLock) {
        mExclusiveLock = true;

        if (mRemoteCallback != NULL) {
            mRemoteCallback->onLockStatusChanged(IProCameraCallbacks::LOCK_ACQUIRED);
        }

        ALOGV("%s: exclusive lock acquired", __FUNCTION__);

        return OK;
    }

    // don't allow recursive locking
    ALOGW("%s: exclusive lock already exists - recursive locking is not allowed"
                                                                , __FUNCTION__);
    return ALREADY_EXISTS;
}

status_t ProCamera2Client::exclusiveUnlock() {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);
    SharedCameraCallbacks::Lock l(mSharedCameraCallbacks);

    // don't allow unlocking if we have no lock
    if (!mExclusiveLock) {
        ALOGW("%s: cannot unlock, no lock was held in the first place",
              __FUNCTION__);
        return BAD_VALUE;
    }

    mExclusiveLock = false;
    if (mRemoteCallback != NULL ) {
        mRemoteCallback->onLockStatusChanged(
                                       IProCameraCallbacks::LOCK_RELEASED);
    }
    ALOGV("%s: exclusive lock released", __FUNCTION__);

    return OK;
}

bool ProCamera2Client::hasExclusiveLock() {
    Mutex::Autolock icl(mBinderSerializationLock);
    return mExclusiveLock;
}

void ProCamera2Client::onExclusiveLockStolen() {
    ALOGV("%s: ProClient lost exclusivity (id %d)",
          __FUNCTION__, mCameraId);

    Mutex::Autolock icl(mBinderSerializationLock);
    SharedCameraCallbacks::Lock l(mSharedCameraCallbacks);

    if (mExclusiveLock && mRemoteCallback.get() != NULL) {
        mRemoteCallback->onLockStatusChanged(
                                       IProCameraCallbacks::LOCK_STOLEN);
    }

    mExclusiveLock = false;

    //TODO: we should not need to detach the device, merely reset it.
    detachDevice();
}

status_t ProCamera2Client::submitRequest(camera_metadata_t* request,
                                         bool streaming) {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    if (!mExclusiveLock) {
        return PERMISSION_DENIED;
    }

    CameraMetadata metadata(request);

    if (!enforceRequestPermissions(metadata)) {
        return PERMISSION_DENIED;
    }

    if (streaming) {
        return mDevice->setStreamingRequest(metadata);
    } else {
        return mDevice->capture(metadata);
    }

    // unreachable. thx gcc for a useless warning
    return OK;
}

status_t ProCamera2Client::cancelRequest(int requestId) {
    (void)requestId;
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    if (!mExclusiveLock) {
        return PERMISSION_DENIED;
    }

    // TODO: implement
    ALOGE("%s: not fully implemented yet", __FUNCTION__);
    return INVALID_OPERATION;
}

status_t ProCamera2Client::deleteStream(int streamId) {
    ATRACE_CALL();
    ALOGV("%s (streamId = 0x%x)", __FUNCTION__, streamId);

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;
    mDevice->clearStreamingRequest();

    status_t code;
    if ((code = mDevice->waitUntilDrained()) != OK) {
        ALOGE("%s: waitUntilDrained failed with code 0x%x", __FUNCTION__, code);
    }

    return mDevice->deleteStream(streamId);
}

status_t ProCamera2Client::createStream(int width, int height, int format,
                      const sp<IGraphicBufferProducer>& bufferProducer,
                      /*out*/
                      int* streamId)
{
    if (streamId) {
        *streamId = -1;
    }

    ATRACE_CALL();
    ALOGV("%s (w = %d, h = %d, f = 0x%x)", __FUNCTION__, width, height, format);

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    sp<IBinder> binder;
    sp<ANativeWindow> window;
    if (bufferProducer != 0) {
        binder = bufferProducer->asBinder();
        window = new Surface(bufferProducer);
    }

    return mDevice->createStream(window, width, height, format, /*size*/1,
                                 streamId);
}

// Create a request object from a template.
// -- Caller owns the newly allocated metadata
status_t ProCamera2Client::createDefaultRequest(int templateId,
                             /*out*/
                              camera_metadata** request)
{
    ATRACE_CALL();
    ALOGV("%s (templateId = 0x%x)", __FUNCTION__, templateId);

    if (request) {
        *request = NULL;
    }

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    CameraMetadata metadata;
    if ( (res = mDevice->createDefaultRequest(templateId, &metadata) ) == OK) {
        *request = metadata.release();
    }

    return res;
}

status_t ProCamera2Client::getCameraInfo(int cameraId,
                                         /*out*/
                                         camera_metadata** info)
{
    if (cameraId != mCameraId) {
        return INVALID_OPERATION;
    }

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    CameraMetadata deviceInfo = mDevice->info();
    *info = deviceInfo.release();

    return OK;
}

status_t ProCamera2Client::dump(int fd, const Vector<String16>& args) {
    String8 result;
    result.appendFormat("ProCamera2Client[%d] (%p) PID: %d, dump:\n",
            mCameraId,
            getRemoteCallback()->asBinder().get(),
            mClientPid);
    result.append("  State: ");

    // TODO: print dynamic/request section from most recent requests
    mFrameProcessor->dump(fd, args);

    return dumpDevice(fd, args);
}

// IProCameraUser interface

void ProCamera2Client::detachDevice() {
    if (mDevice == 0) return;

    ALOGV("Camera %d: Stopping processors", mCameraId);

    mFrameProcessor->removeListener(FRAME_PROCESSOR_LISTENER_MIN_ID,
                                    FRAME_PROCESSOR_LISTENER_MAX_ID,
                                    /*listener*/this);
    mFrameProcessor->requestExit();
    ALOGV("Camera %d: Waiting for threads", mCameraId);
    mFrameProcessor->join();
    ALOGV("Camera %d: Disconnecting device", mCameraId);

    // WORKAROUND: HAL refuses to disconnect while there's streams in flight
    {
        mDevice->clearStreamingRequest();

        status_t code;
        if ((code = mDevice->waitUntilDrained()) != OK) {
            ALOGE("%s: waitUntilDrained failed with code 0x%x", __FUNCTION__,
                  code);
        }
    }

    Camera2ClientBase::detachDevice();
}

/** Device-related methods */
void ProCamera2Client::onFrameAvailable(int32_t requestId,
                                        const CameraMetadata& frame) {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    Mutex::Autolock icl(mBinderSerializationLock);
    SharedCameraCallbacks::Lock l(mSharedCameraCallbacks);

    if (mRemoteCallback != NULL) {
        CameraMetadata tmp(frame);
        camera_metadata_t* meta = tmp.release();
        ALOGV("%s: meta = %p ", __FUNCTION__, meta);
        mRemoteCallback->onResultReceived(requestId, meta);
        tmp.acquire(meta);
    }

}

bool ProCamera2Client::enforceRequestPermissions(CameraMetadata& metadata) {

    const int pid = IPCThreadState::self()->getCallingPid();
    const int selfPid = getpid();
    camera_metadata_entry_t entry;

    /**
     * Mixin default important security values
     * - android.led.transmit = defaulted ON
     */
    CameraMetadata staticInfo = mDevice->info();
    entry = staticInfo.find(ANDROID_LED_AVAILABLE_LEDS);
    for(size_t i = 0; i < entry.count; ++i) {
        uint8_t led = entry.data.u8[i];

        switch(led) {
            case ANDROID_LED_AVAILABLE_LEDS_TRANSMIT: {
                uint8_t transmitDefault = ANDROID_LED_TRANSMIT_ON;
                if (!metadata.exists(ANDROID_LED_TRANSMIT)) {
                    metadata.update(ANDROID_LED_TRANSMIT,
                                    &transmitDefault, 1);
                }
                break;
            }
        }
    }

    // We can do anything!
    if (pid == selfPid) {
        return true;
    }

    /**
     * Permission check special fields in the request
     * - android.led.transmit = android.permission.CAMERA_DISABLE_TRANSMIT
     */
    entry = metadata.find(ANDROID_LED_TRANSMIT);
    if (entry.count > 0 && entry.data.u8[0] != ANDROID_LED_TRANSMIT_ON) {
        String16 permissionString =
            String16("android.permission.CAMERA_DISABLE_TRANSMIT_LED");
        if (!checkCallingPermission(permissionString)) {
            const int uid = IPCThreadState::self()->getCallingUid();
            ALOGE("Permission Denial: "
                  "can't disable transmit LED pid=%d, uid=%d", pid, uid);
            return false;
        }
    }

    return true;
}

} // namespace android
