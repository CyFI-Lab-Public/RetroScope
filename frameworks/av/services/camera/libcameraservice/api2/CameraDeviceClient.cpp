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

#define LOG_TAG "CameraDeviceClient"
#define ATRACE_TAG ATRACE_TAG_CAMERA
// #define LOG_NDEBUG 0

#include <cutils/properties.h>
#include <utils/Log.h>
#include <utils/Trace.h>
#include <gui/Surface.h>
#include <camera/camera2/CaptureRequest.h>

#include "common/CameraDeviceBase.h"
#include "api2/CameraDeviceClient.h"



namespace android {

CameraDeviceClient::CameraDeviceClient(): Camera2ClientBase() {
}

CameraDeviceClientBase::CameraDeviceClientBase(){
} 

CameraDeviceClient * oneInstance = new CameraDeviceClient();

using namespace camera2;

CameraDeviceClientBase::CameraDeviceClientBase(
        const sp<CameraService>& cameraService,
        const sp<ICameraDeviceCallbacks>& remoteCallback,
        const String16& clientPackageName,
        int cameraId,
        int cameraFacing,
        int clientPid,
        uid_t clientUid,
        int servicePid) :
    BasicClient(cameraService, remoteCallback->asBinder(), clientPackageName,
                cameraId, cameraFacing, clientPid, clientUid, servicePid),
    mRemoteCallback(remoteCallback) {
}

// Interface used by CameraService

CameraDeviceClient::CameraDeviceClient(const sp<CameraService>& cameraService,
                                   const sp<ICameraDeviceCallbacks>& remoteCallback,
                                   const String16& clientPackageName,
                                   int cameraId,
                                   int cameraFacing,
                                   int clientPid,
                                   uid_t clientUid,
                                   int servicePid) :
    Camera2ClientBase(cameraService, remoteCallback, clientPackageName,
                cameraId, cameraFacing, clientPid, clientUid, servicePid),
    mRequestIdCounter(0) {

    ATRACE_CALL();
    ALOGI("CameraDeviceClient %d: Opened", cameraId);
}

status_t CameraDeviceClient::initialize(camera_module_t *module)
{
    ATRACE_CALL();
    status_t res;

    res = Camera2ClientBase::initialize(module);
    if (res != OK) {
        return res;
    }

    String8 threadName;
    mFrameProcessor = new FrameProcessorBase(mDevice);
    threadName = String8::format("CDU-%d-FrameProc", mCameraId);
    mFrameProcessor->run(threadName.string());

    mFrameProcessor->registerListener(FRAME_PROCESSOR_LISTENER_MIN_ID,
                                      FRAME_PROCESSOR_LISTENER_MAX_ID,
                                      /*listener*/this,
                                      /*quirkSendPartials*/true);

    return OK;
}

CameraDeviceClient::~CameraDeviceClient() {
}

status_t CameraDeviceClient::submitRequest(sp<CaptureRequest> request,
                                         bool streaming) {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    status_t res;

    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    if (request == 0) {
        ALOGE("%s: Camera %d: Sent null request. Rejecting request.",
              __FUNCTION__, mCameraId);
        return BAD_VALUE;
    }

    CameraMetadata metadata(request->mMetadata);

    if (metadata.isEmpty()) {
        ALOGE("%s: Camera %d: Sent empty metadata packet. Rejecting request.",
               __FUNCTION__, mCameraId);
        return BAD_VALUE;
    } else if (request->mSurfaceList.size() == 0) {
        ALOGE("%s: Camera %d: Requests must have at least one surface target. "
              "Rejecting request.", __FUNCTION__, mCameraId);
        return BAD_VALUE;
    }

    if (!enforceRequestPermissions(metadata)) {
        // Callee logs
        return PERMISSION_DENIED;
    }

    /**
     * Write in the output stream IDs which we calculate from
     * the capture request's list of surface targets
     */
    Vector<int32_t> outputStreamIds;
    outputStreamIds.setCapacity(request->mSurfaceList.size());
    for (size_t i = 0; i < request->mSurfaceList.size(); ++i) {
        sp<Surface> surface = request->mSurfaceList[i];

        if (surface == 0) continue;

        sp<IGraphicBufferProducer> gbp = surface->getIGraphicBufferProducer();
        int idx = mStreamMap.indexOfKey(gbp->asBinder());

        // Trying to submit request with surface that wasn't created
        if (idx == NAME_NOT_FOUND) {
            ALOGE("%s: Camera %d: Tried to submit a request with a surface that"
                  " we have not called createStream on",
                  __FUNCTION__, mCameraId);
            return BAD_VALUE;
        }

        int streamId = mStreamMap.valueAt(idx);
        outputStreamIds.push_back(streamId);
        ALOGV("%s: Camera %d: Appending output stream %d to request",
              __FUNCTION__, mCameraId, streamId);
    }

    metadata.update(ANDROID_REQUEST_OUTPUT_STREAMS, &outputStreamIds[0],
                    outputStreamIds.size());

    int32_t requestId = mRequestIdCounter++;
    metadata.update(ANDROID_REQUEST_ID, &requestId, /*size*/1);
    ALOGV("%s: Camera %d: Submitting request with ID %d",
          __FUNCTION__, mCameraId, requestId);

    if (streaming) {
        res = mDevice->setStreamingRequest(metadata);
        if (res != OK) {
            ALOGE("%s: Camera %d:  Got error %d after trying to set streaming "
                  "request", __FUNCTION__, mCameraId, res);
        } else {
            mStreamingRequestList.push_back(requestId);
        }
    } else {
        res = mDevice->capture(metadata);
        if (res != OK) {
            ALOGE("%s: Camera %d: Got error %d after trying to set capture",
                  __FUNCTION__, mCameraId, res);
        }
    }

    ALOGV("%s: Camera %d: End of function", __FUNCTION__, mCameraId);
    if (res == OK) {
        return requestId;
    }

    return res;
}

status_t CameraDeviceClient::cancelRequest(int requestId) {
    ATRACE_CALL();
    ALOGV("%s, requestId = %d", __FUNCTION__, requestId);

    status_t res;

    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    Vector<int>::iterator it, end;
    for (it = mStreamingRequestList.begin(), end = mStreamingRequestList.end();
         it != end; ++it) {
        if (*it == requestId) {
            break;
        }
    }

    if (it == end) {
        ALOGE("%s: Camera%d: Did not find request id %d in list of streaming "
              "requests", __FUNCTION__, mCameraId, requestId);
        return BAD_VALUE;
    }

    res = mDevice->clearStreamingRequest();

    if (res == OK) {
        ALOGV("%s: Camera %d: Successfully cleared streaming request",
              __FUNCTION__, mCameraId);
        mStreamingRequestList.erase(it);
    }

    return res;
}

status_t CameraDeviceClient::deleteStream(int streamId) {
    ATRACE_CALL();
    ALOGV("%s (streamId = 0x%x)", __FUNCTION__, streamId);

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    // Guard against trying to delete non-created streams
    ssize_t index = NAME_NOT_FOUND;
    for (size_t i = 0; i < mStreamMap.size(); ++i) {
        if (streamId == mStreamMap.valueAt(i)) {
            index = i;
            break;
        }
    }

    if (index == NAME_NOT_FOUND) {
        ALOGW("%s: Camera %d: Invalid stream ID (%d) specified, no stream "
              "created yet", __FUNCTION__, mCameraId, streamId);
        return BAD_VALUE;
    }

    // Also returns BAD_VALUE if stream ID was not valid
    res = mDevice->deleteStream(streamId);

    if (res == BAD_VALUE) {
        ALOGE("%s: Camera %d: Unexpected BAD_VALUE when deleting stream, but we"
              " already checked and the stream ID (%d) should be valid.",
              __FUNCTION__, mCameraId, streamId);
    } else if (res == OK) {
        mStreamMap.removeItemsAt(index);

        ALOGV("%s: Camera %d: Successfully deleted stream ID (%d)",
              __FUNCTION__, mCameraId, streamId);
    }

    return res;
}

status_t CameraDeviceClient::createStream(int width, int height, int format,
                      const sp<IGraphicBufferProducer>& bufferProducer)
{
    ATRACE_CALL();
    ALOGV("%s (w = %d, h = %d, f = 0x%x)", __FUNCTION__, width, height, format);

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    // Don't create multiple streams for the same target surface
    {
        ssize_t index = mStreamMap.indexOfKey(bufferProducer->asBinder());
        if (index != NAME_NOT_FOUND) {
            ALOGW("%s: Camera %d: Buffer producer already has a stream for it "
                  "(ID %d)",
                  __FUNCTION__, mCameraId, index);
            return ALREADY_EXISTS;
        }
    }

    // HACK b/10949105
    // Query consumer usage bits to set async operation mode for
    // GLConsumer using controlledByApp parameter.
    bool useAsync = false;
    int32_t consumerUsage;
    if ((res = bufferProducer->query(NATIVE_WINDOW_CONSUMER_USAGE_BITS,
            &consumerUsage)) != OK) {
        ALOGE("%s: Camera %d: Failed to query consumer usage", __FUNCTION__,
              mCameraId);
        return res;
    }
    if (consumerUsage & GraphicBuffer::USAGE_HW_TEXTURE) {
        ALOGW("%s: Camera %d: Forcing asynchronous mode for stream",
                __FUNCTION__, mCameraId);
        useAsync = true;
    }

    sp<IBinder> binder;
    sp<ANativeWindow> anw;
    if (bufferProducer != 0) {
        binder = bufferProducer->asBinder();
        anw = new Surface(bufferProducer, useAsync);
    }

    // TODO: remove w,h,f since we are ignoring them

    if ((res = anw->query(anw.get(), NATIVE_WINDOW_WIDTH, &width)) != OK) {
        ALOGE("%s: Camera %d: Failed to query Surface width", __FUNCTION__,
              mCameraId);
        return res;
    }
    if ((res = anw->query(anw.get(), NATIVE_WINDOW_HEIGHT, &height)) != OK) {
        ALOGE("%s: Camera %d: Failed to query Surface height", __FUNCTION__,
              mCameraId);
        return res;
    }
    if ((res = anw->query(anw.get(), NATIVE_WINDOW_FORMAT, &format)) != OK) {
        ALOGE("%s: Camera %d: Failed to query Surface format", __FUNCTION__,
              mCameraId);
        return res;
    }

    // FIXME: remove this override since the default format should be
    //       IMPLEMENTATION_DEFINED. b/9487482
    if (format >= HAL_PIXEL_FORMAT_RGBA_8888 &&
        format <= HAL_PIXEL_FORMAT_BGRA_8888) {
        ALOGW("%s: Camera %d: Overriding format 0x%x to IMPLEMENTATION_DEFINED",
              __FUNCTION__, mCameraId, format);
        format = HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED;
    }

    // TODO: add startConfigure/stopConfigure call to CameraDeviceBase
    // this will make it so Camera3Device doesn't call configure_streams
    // after each call, but only once we are done with all.

    int streamId = -1;
    if (format == HAL_PIXEL_FORMAT_BLOB) {
        // JPEG buffers need to be sized for maximum possible compressed size
        CameraMetadata staticInfo = mDevice->info();
        camera_metadata_entry_t entry = staticInfo.find(ANDROID_JPEG_MAX_SIZE);
        if (entry.count == 0) {
            ALOGE("%s: Camera %d: Can't find maximum JPEG size in "
                    "static metadata!", __FUNCTION__, mCameraId);
            return INVALID_OPERATION;
        }
        int32_t maxJpegSize = entry.data.i32[0];
        res = mDevice->createStream(anw, width, height, format, maxJpegSize,
                &streamId);
    } else {
        // All other streams are a known size
        res = mDevice->createStream(anw, width, height, format, /*size*/0,
                &streamId);
    }

    if (res == OK) {
        mStreamMap.add(bufferProducer->asBinder(), streamId);

        ALOGV("%s: Camera %d: Successfully created a new stream ID %d",
              __FUNCTION__, mCameraId, streamId);

        /**
         * Set the stream transform flags to automatically
         * rotate the camera stream for preview use cases.
         */
        int32_t transform = 0;
        res = getRotationTransformLocked(&transform);

        if (res != OK) {
            // Error logged by getRotationTransformLocked.
            return res;
        }

        res = mDevice->setStreamTransform(streamId, transform);
        if (res != OK) {
            ALOGE("%s: Failed to set stream transform (stream id %d)",
                  __FUNCTION__, streamId);
            return res;
        }

        return streamId;
    }

    return res;
}

// Create a request object from a template.
status_t CameraDeviceClient::createDefaultRequest(int templateId,
                                                  /*out*/
                                                  CameraMetadata* request)
{
    ATRACE_CALL();
    ALOGV("%s (templateId = 0x%x)", __FUNCTION__, templateId);

    status_t res;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    CameraMetadata metadata;
    if ( (res = mDevice->createDefaultRequest(templateId, &metadata) ) == OK &&
        request != NULL) {

        request->swap(metadata);
    }

    return res;
}

status_t CameraDeviceClient::getCameraInfo(/*out*/CameraMetadata* info)
{
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    status_t res = OK;

    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    if (info != NULL) {
        *info = mDevice->info(); // static camera metadata
        // TODO: merge with device-specific camera metadata
    }

    return res;
}

status_t CameraDeviceClient::waitUntilIdle()
{
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    status_t res = OK;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    // FIXME: Also need check repeating burst.
    if (!mStreamingRequestList.isEmpty()) {
        ALOGE("%s: Camera %d: Try to waitUntilIdle when there are active streaming requests",
              __FUNCTION__, mCameraId);
        return INVALID_OPERATION;
    }
    res = mDevice->waitUntilDrained();
    ALOGV("%s Done", __FUNCTION__);

    return res;
}

status_t CameraDeviceClient::flush() {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    status_t res = OK;
    if ( (res = checkPid(__FUNCTION__) ) != OK) return res;

    Mutex::Autolock icl(mBinderSerializationLock);

    if (!mDevice.get()) return DEAD_OBJECT;

    return mDevice->flush();
}

status_t CameraDeviceClient::dump(int fd, const Vector<String16>& args) {
    String8 result;
    result.appendFormat("CameraDeviceClient[%d] (%p) PID: %d, dump:\n",
            mCameraId,
            getRemoteCallback()->asBinder().get(),
            mClientPid);
    result.append("  State: ");

    // TODO: print dynamic/request section from most recent requests
    mFrameProcessor->dump(fd, args);

    return dumpDevice(fd, args);
}


void CameraDeviceClient::notifyError() {
    // Thread safe. Don't bother locking.
    sp<ICameraDeviceCallbacks> remoteCb = getRemoteCallback();

    if (remoteCb != 0) {
        remoteCb->onDeviceError(ICameraDeviceCallbacks::ERROR_CAMERA_DEVICE);
    }
}

void CameraDeviceClient::notifyIdle() {
    // Thread safe. Don't bother locking.
    sp<ICameraDeviceCallbacks> remoteCb = getRemoteCallback();

    if (remoteCb != 0) {
        remoteCb->onDeviceIdle();
    }
}

void CameraDeviceClient::notifyShutter(int requestId,
        nsecs_t timestamp) {
    // Thread safe. Don't bother locking.
    sp<ICameraDeviceCallbacks> remoteCb = getRemoteCallback();
    if (remoteCb != 0) {
        remoteCb->onCaptureStarted(requestId, timestamp);
    }
}

// TODO: refactor the code below this with IProCameraUser.
// it's 100% copy-pasted, so lets not change it right now to make it easier.

void CameraDeviceClient::detachDevice() {
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
void CameraDeviceClient::onFrameAvailable(int32_t requestId,
        const CameraMetadata& frame) {
    ATRACE_CALL();
    ALOGV("%s", __FUNCTION__);

    // Thread-safe. No lock necessary.
    sp<ICameraDeviceCallbacks> remoteCb = mRemoteCallback;
    if (remoteCb != NULL) {
        ALOGV("%s: frame = %p ", __FUNCTION__, &frame);
        remoteCb->onResultReceived(requestId, frame);
    }
}

// TODO: move to Camera2ClientBase
bool CameraDeviceClient::enforceRequestPermissions(CameraMetadata& metadata) {

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

status_t CameraDeviceClient::getRotationTransformLocked(int32_t* transform) {
    ALOGV("%s: begin", __FUNCTION__);

    if (transform == NULL) {
        ALOGW("%s: null transform", __FUNCTION__);
        return BAD_VALUE;
    }

    *transform = 0;

    const CameraMetadata& staticInfo = mDevice->info();
    camera_metadata_ro_entry_t entry = staticInfo.find(ANDROID_SENSOR_ORIENTATION);
    if (entry.count == 0) {
        ALOGE("%s: Camera %d: Can't find android.sensor.orientation in "
                "static metadata!", __FUNCTION__, mCameraId);
        return INVALID_OPERATION;
    }

    int32_t& flags = *transform;

    int orientation = entry.data.i32[0];
    switch (orientation) {
        case 0:
            flags = 0;
            break;
        case 90:
            flags = NATIVE_WINDOW_TRANSFORM_ROT_90;
            break;
        case 180:
            flags = NATIVE_WINDOW_TRANSFORM_ROT_180;
            break;
        case 270:
            flags = NATIVE_WINDOW_TRANSFORM_ROT_270;
            break;
        default:
            ALOGE("%s: Invalid HAL android.sensor.orientation value: %d",
                  __FUNCTION__, orientation);
            return INVALID_OPERATION;
    }

    /**
     * This magic flag makes surfaceflinger un-rotate the buffers
     * to counter the extra global device UI rotation whenever the user
     * physically rotates the device.
     *
     * By doing this, the camera buffer always ends up aligned
     * with the physical camera for a "see through" effect.
     *
     * In essence, the buffer only gets rotated during preview use-cases.
     * The user is still responsible to re-create streams of the proper
     * aspect ratio, or the preview will end up looking non-uniformly
     * stretched.
     */
    flags |= NATIVE_WINDOW_TRANSFORM_INVERSE_DISPLAY;

    ALOGV("%s: final transform = 0x%x", __FUNCTION__, flags);

    return OK;
}

} // namespace android
