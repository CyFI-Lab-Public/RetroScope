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

#define LOG_TAG "Camera2-Device"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0
//#define LOG_NNDEBUG 0  // Per-frame verbose logging

#ifdef LOG_NNDEBUG
#define ALOGVV(...) ALOGV(__VA_ARGS__)
#else
#define ALOGVV(...) ((void)0)
#endif

#include <utils/Log.h>
#include <utils/Trace.h>
#include <utils/Timers.h>
#include "Camera2Device.h"

namespace android {

Camera2Device::Camera2Device(int id):
        mId(id),
        mHal2Device(NULL)
{
    ATRACE_CALL();
    ALOGV("%s: Created device for camera %d", __FUNCTION__, id);
}

Camera2Device::~Camera2Device()
{
    ATRACE_CALL();
    ALOGV("%s: Tearing down for camera id %d", __FUNCTION__, mId);
    disconnect();
}

int Camera2Device::getId() const {
    return mId;
}

status_t Camera2Device::initialize(camera_module_t *module)
{
    ATRACE_CALL();
    ALOGV("%s: Initializing device for camera %d", __FUNCTION__, mId);
    if (mHal2Device != NULL) {
        ALOGE("%s: Already initialized!", __FUNCTION__);
        return INVALID_OPERATION;
    }

    status_t res;
    char name[10];
    snprintf(name, sizeof(name), "%d", mId);

    camera2_device_t *device;

    res = module->common.methods->open(&module->common, name,
            reinterpret_cast<hw_device_t**>(&device));

    if (res != OK) {
        ALOGE("%s: Could not open camera %d: %s (%d)", __FUNCTION__,
                mId, strerror(-res), res);
        return res;
    }

    if (device->common.version != CAMERA_DEVICE_API_VERSION_2_0) {
        ALOGE("%s: Could not open camera %d: "
                "Camera device is not version %x, reports %x instead",
                __FUNCTION__, mId, CAMERA_DEVICE_API_VERSION_2_0,
                device->common.version);
        device->common.close(&device->common);
        return BAD_VALUE;
    }

    camera_info info;
    res = module->get_camera_info(mId, &info);
    if (res != OK ) return res;

    if (info.device_version != device->common.version) {
        ALOGE("%s: HAL reporting mismatched camera_info version (%x)"
                " and device version (%x).", __FUNCTION__,
                device->common.version, info.device_version);
        device->common.close(&device->common);
        return BAD_VALUE;
    }

    res = mRequestQueue.setConsumerDevice(device);
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to connect request queue to device: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        device->common.close(&device->common);
        return res;
    }
    res = mFrameQueue.setProducerDevice(device);
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to connect frame queue to device: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        device->common.close(&device->common);
        return res;
    }

    res = device->ops->get_metadata_vendor_tag_ops(device, &mVendorTagOps);
    if (res != OK ) {
        ALOGE("%s: Camera %d: Unable to retrieve tag ops from device: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        device->common.close(&device->common);
        return res;
    }
    res = set_camera_metadata_vendor_tag_ops(mVendorTagOps);
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to set tag ops: %s (%d)",
            __FUNCTION__, mId, strerror(-res), res);
        device->common.close(&device->common);
        return res;
    }
    res = device->ops->set_notify_callback(device, notificationCallback,
            NULL);
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to initialize notification callback!",
                __FUNCTION__, mId);
        device->common.close(&device->common);
        return res;
    }

    mDeviceInfo = info.static_camera_characteristics;
    mHal2Device = device;

    return OK;
}

status_t Camera2Device::disconnect() {
    ATRACE_CALL();
    status_t res = OK;
    if (mHal2Device) {
        ALOGV("%s: Closing device for camera %d", __FUNCTION__, mId);

        int inProgressCount = mHal2Device->ops->get_in_progress_count(mHal2Device);
        if (inProgressCount > 0) {
            ALOGW("%s: Closing camera device %d with %d requests in flight!",
                    __FUNCTION__, mId, inProgressCount);
        }
        mReprocessStreams.clear();
        mStreams.clear();
        res = mHal2Device->common.close(&mHal2Device->common);
        if (res != OK) {
            ALOGE("%s: Could not close camera %d: %s (%d)",
                    __FUNCTION__,
                    mId, strerror(-res), res);
        }
        mHal2Device = NULL;
        ALOGV("%s: Shutdown complete", __FUNCTION__);
    }
    return res;
}

status_t Camera2Device::dump(int fd, const Vector<String16>& args) {
    ATRACE_CALL();
    String8 result;
    int detailLevel = 0;
    int n = args.size();
    String16 detailOption("-d");
    for (int i = 0; i + 1 < n; i++) {
        if (args[i] == detailOption) {
            String8 levelStr(args[i+1]);
            detailLevel = atoi(levelStr.string());
        }
    }

    result.appendFormat("  Camera2Device[%d] dump (detail level %d):\n",
            mId, detailLevel);

    if (detailLevel > 0) {
        result = "    Request queue contents:\n";
        write(fd, result.string(), result.size());
        mRequestQueue.dump(fd, args);

        result = "    Frame queue contents:\n";
        write(fd, result.string(), result.size());
        mFrameQueue.dump(fd, args);
    }

    result = "    Active streams:\n";
    write(fd, result.string(), result.size());
    for (StreamList::iterator s = mStreams.begin(); s != mStreams.end(); s++) {
        (*s)->dump(fd, args);
    }

    result = "    HAL device dump:\n";
    write(fd, result.string(), result.size());

    status_t res;
    res = mHal2Device->ops->dump(mHal2Device, fd);

    return res;
}

const CameraMetadata& Camera2Device::info() const {
    ALOGVV("%s: E", __FUNCTION__);

    return mDeviceInfo;
}

status_t Camera2Device::capture(CameraMetadata &request) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);

    mRequestQueue.enqueue(request.release());
    return OK;
}


status_t Camera2Device::setStreamingRequest(const CameraMetadata &request) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    CameraMetadata streamRequest(request);
    return mRequestQueue.setStreamSlot(streamRequest.release());
}

status_t Camera2Device::clearStreamingRequest() {
    ATRACE_CALL();
    return mRequestQueue.setStreamSlot(NULL);
}

status_t Camera2Device::waitUntilRequestReceived(int32_t requestId, nsecs_t timeout) {
    ATRACE_CALL();
    return mRequestQueue.waitForDequeue(requestId, timeout);
}

status_t Camera2Device::createStream(sp<ANativeWindow> consumer,
        uint32_t width, uint32_t height, int format, size_t size, int *id) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: E", __FUNCTION__);

    sp<StreamAdapter> stream = new StreamAdapter(mHal2Device);

    res = stream->connectToDevice(consumer, width, height, format, size);
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to create stream (%d x %d, format %x):"
                "%s (%d)",
                __FUNCTION__, mId, width, height, format, strerror(-res), res);
        return res;
    }

    *id = stream->getId();

    mStreams.push_back(stream);
    return OK;
}

status_t Camera2Device::createReprocessStreamFromStream(int outputId, int *id) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: E", __FUNCTION__);

    bool found = false;
    StreamList::iterator streamI;
    for (streamI = mStreams.begin();
         streamI != mStreams.end(); streamI++) {
        if ((*streamI)->getId() == outputId) {
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Output stream %d doesn't exist; can't create "
                "reprocess stream from it!", __FUNCTION__, mId, outputId);
        return BAD_VALUE;
    }

    sp<ReprocessStreamAdapter> stream = new ReprocessStreamAdapter(mHal2Device);

    res = stream->connectToDevice((*streamI));
    if (res != OK) {
        ALOGE("%s: Camera %d: Unable to create reprocessing stream from "\
                "stream %d: %s (%d)", __FUNCTION__, mId, outputId,
                strerror(-res), res);
        return res;
    }

    *id = stream->getId();

    mReprocessStreams.push_back(stream);
    return OK;
}


status_t Camera2Device::getStreamInfo(int id,
        uint32_t *width, uint32_t *height, uint32_t *format) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    bool found = false;
    StreamList::iterator streamI;
    for (streamI = mStreams.begin();
         streamI != mStreams.end(); streamI++) {
        if ((*streamI)->getId() == id) {
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Stream %d does not exist",
                __FUNCTION__, mId, id);
        return BAD_VALUE;
    }

    if (width) *width = (*streamI)->getWidth();
    if (height) *height = (*streamI)->getHeight();
    if (format) *format = (*streamI)->getFormat();

    return OK;
}

status_t Camera2Device::setStreamTransform(int id,
        int transform) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    bool found = false;
    StreamList::iterator streamI;
    for (streamI = mStreams.begin();
         streamI != mStreams.end(); streamI++) {
        if ((*streamI)->getId() == id) {
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Stream %d does not exist",
                __FUNCTION__, mId, id);
        return BAD_VALUE;
    }

    return (*streamI)->setTransform(transform);
}

status_t Camera2Device::deleteStream(int id) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    bool found = false;
    for (StreamList::iterator streamI = mStreams.begin();
         streamI != mStreams.end(); streamI++) {
        if ((*streamI)->getId() == id) {
            status_t res = (*streamI)->release();
            if (res != OK) {
                ALOGE("%s: Unable to release stream %d from HAL device: "
                        "%s (%d)", __FUNCTION__, id, strerror(-res), res);
                return res;
            }
            mStreams.erase(streamI);
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Unable to find stream %d to delete",
                __FUNCTION__, mId, id);
        return BAD_VALUE;
    }
    return OK;
}

status_t Camera2Device::deleteReprocessStream(int id) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    bool found = false;
    for (ReprocessStreamList::iterator streamI = mReprocessStreams.begin();
         streamI != mReprocessStreams.end(); streamI++) {
        if ((*streamI)->getId() == id) {
            status_t res = (*streamI)->release();
            if (res != OK) {
                ALOGE("%s: Unable to release reprocess stream %d from "
                        "HAL device: %s (%d)", __FUNCTION__, id,
                        strerror(-res), res);
                return res;
            }
            mReprocessStreams.erase(streamI);
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Unable to find stream %d to delete",
                __FUNCTION__, mId, id);
        return BAD_VALUE;
    }
    return OK;
}


status_t Camera2Device::createDefaultRequest(int templateId,
        CameraMetadata *request) {
    ATRACE_CALL();
    status_t err;
    ALOGV("%s: E", __FUNCTION__);
    camera_metadata_t *rawRequest;
    err = mHal2Device->ops->construct_default_request(
        mHal2Device, templateId, &rawRequest);
    request->acquire(rawRequest);
    return err;
}

status_t Camera2Device::waitUntilDrained() {
    ATRACE_CALL();
    static const uint32_t kSleepTime = 50000; // 50 ms
    static const uint32_t kMaxSleepTime = 10000000; // 10 s
    ALOGV("%s: Camera %d: Starting wait", __FUNCTION__, mId);
    if (mRequestQueue.getBufferCount() ==
            CAMERA2_REQUEST_QUEUE_IS_BOTTOMLESS) return INVALID_OPERATION;

    // TODO: Set up notifications from HAL, instead of sleeping here
    uint32_t totalTime = 0;
    while (mHal2Device->ops->get_in_progress_count(mHal2Device) > 0) {
        usleep(kSleepTime);
        totalTime += kSleepTime;
        if (totalTime > kMaxSleepTime) {
            ALOGE("%s: Waited %d us, %d requests still in flight", __FUNCTION__,
                    totalTime, mHal2Device->ops->get_in_progress_count(mHal2Device));
            return TIMED_OUT;
        }
    }
    ALOGV("%s: Camera %d: HAL is idle", __FUNCTION__, mId);
    return OK;
}

status_t Camera2Device::setNotifyCallback(NotificationListener *listener) {
    ATRACE_CALL();
    status_t res;
    res = mHal2Device->ops->set_notify_callback(mHal2Device, notificationCallback,
            reinterpret_cast<void*>(listener) );
    if (res != OK) {
        ALOGE("%s: Unable to set notification callback!", __FUNCTION__);
    }
    return res;
}

bool Camera2Device::willNotify3A() {
    return true;
}

void Camera2Device::notificationCallback(int32_t msg_type,
        int32_t ext1,
        int32_t ext2,
        int32_t ext3,
        void *user) {
    ATRACE_CALL();
    NotificationListener *listener = reinterpret_cast<NotificationListener*>(user);
    ALOGV("%s: Notification %d, arguments %d, %d, %d", __FUNCTION__, msg_type,
            ext1, ext2, ext3);
    if (listener != NULL) {
        switch (msg_type) {
            case CAMERA2_MSG_ERROR:
                listener->notifyError(ext1, ext2, ext3);
                break;
            case CAMERA2_MSG_SHUTTER: {
                // TODO: Only needed for camera2 API, which is unsupported
                // by HAL2 directly.
                // nsecs_t timestamp = (nsecs_t)ext2 | ((nsecs_t)(ext3) << 32 );
                // listener->notifyShutter(requestId, timestamp);
                break;
            }
            case CAMERA2_MSG_AUTOFOCUS:
                listener->notifyAutoFocus(ext1, ext2);
                break;
            case CAMERA2_MSG_AUTOEXPOSURE:
                listener->notifyAutoExposure(ext1, ext2);
                break;
            case CAMERA2_MSG_AUTOWB:
                listener->notifyAutoWhitebalance(ext1, ext2);
                break;
            default:
                ALOGE("%s: Unknown notification %d (arguments %d, %d, %d)!",
                        __FUNCTION__, msg_type, ext1, ext2, ext3);
        }
    }
}

status_t Camera2Device::waitForNextFrame(nsecs_t timeout) {
    return mFrameQueue.waitForBuffer(timeout);
}

status_t Camera2Device::getNextFrame(CameraMetadata *frame) {
    ATRACE_CALL();
    status_t res;
    camera_metadata_t *rawFrame;
    res = mFrameQueue.dequeue(&rawFrame);
    if (rawFrame  == NULL) {
        return NOT_ENOUGH_DATA;
    } else if (res == OK) {
        frame->acquire(rawFrame);
    }
    return res;
}

status_t Camera2Device::triggerAutofocus(uint32_t id) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: Triggering autofocus, id %d", __FUNCTION__, id);
    res = mHal2Device->ops->trigger_action(mHal2Device,
            CAMERA2_TRIGGER_AUTOFOCUS, id, 0);
    if (res != OK) {
        ALOGE("%s: Error triggering autofocus (id %d)",
                __FUNCTION__, id);
    }
    return res;
}

status_t Camera2Device::triggerCancelAutofocus(uint32_t id) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: Canceling autofocus, id %d", __FUNCTION__, id);
    res = mHal2Device->ops->trigger_action(mHal2Device,
            CAMERA2_TRIGGER_CANCEL_AUTOFOCUS, id, 0);
    if (res != OK) {
        ALOGE("%s: Error canceling autofocus (id %d)",
                __FUNCTION__, id);
    }
    return res;
}

status_t Camera2Device::triggerPrecaptureMetering(uint32_t id) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: Triggering precapture metering, id %d", __FUNCTION__, id);
    res = mHal2Device->ops->trigger_action(mHal2Device,
            CAMERA2_TRIGGER_PRECAPTURE_METERING, id, 0);
    if (res != OK) {
        ALOGE("%s: Error triggering precapture metering (id %d)",
                __FUNCTION__, id);
    }
    return res;
}

status_t Camera2Device::pushReprocessBuffer(int reprocessStreamId,
        buffer_handle_t *buffer, wp<BufferReleasedListener> listener) {
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    bool found = false;
    status_t res = OK;
    for (ReprocessStreamList::iterator streamI = mReprocessStreams.begin();
         streamI != mReprocessStreams.end(); streamI++) {
        if ((*streamI)->getId() == reprocessStreamId) {
            res = (*streamI)->pushIntoStream(buffer, listener);
            if (res != OK) {
                ALOGE("%s: Unable to push buffer to reprocess stream %d: %s (%d)",
                        __FUNCTION__, reprocessStreamId, strerror(-res), res);
                return res;
            }
            found = true;
            break;
        }
    }
    if (!found) {
        ALOGE("%s: Camera %d: Unable to find reprocess stream %d",
                __FUNCTION__, mId, reprocessStreamId);
        res = BAD_VALUE;
    }
    return res;
}

status_t Camera2Device::flush() {
    ATRACE_CALL();

    mRequestQueue.clear();
    return waitUntilDrained();
}

/**
 * Camera2Device::MetadataQueue
 */

Camera2Device::MetadataQueue::MetadataQueue():
            mHal2Device(NULL),
            mFrameCount(0),
            mLatestRequestId(0),
            mCount(0),
            mStreamSlotCount(0),
            mSignalConsumer(true)
{
    ATRACE_CALL();
    camera2_request_queue_src_ops::dequeue_request = consumer_dequeue;
    camera2_request_queue_src_ops::request_count = consumer_buffer_count;
    camera2_request_queue_src_ops::free_request = consumer_free;

    camera2_frame_queue_dst_ops::dequeue_frame = producer_dequeue;
    camera2_frame_queue_dst_ops::cancel_frame = producer_cancel;
    camera2_frame_queue_dst_ops::enqueue_frame = producer_enqueue;
}

Camera2Device::MetadataQueue::~MetadataQueue() {
    ATRACE_CALL();
    clear();
}

// Connect to camera2 HAL as consumer (input requests/reprocessing)
status_t Camera2Device::MetadataQueue::setConsumerDevice(camera2_device_t *d) {
    ATRACE_CALL();
    status_t res;
    res = d->ops->set_request_queue_src_ops(d,
            this);
    if (res != OK) return res;
    mHal2Device = d;
    return OK;
}

status_t Camera2Device::MetadataQueue::setProducerDevice(camera2_device_t *d) {
    ATRACE_CALL();
    status_t res;
    res = d->ops->set_frame_queue_dst_ops(d,
            this);
    return res;
}

// Real interfaces
status_t Camera2Device::MetadataQueue::enqueue(camera_metadata_t *buf) {
    ATRACE_CALL();
    ALOGVV("%s: E", __FUNCTION__);
    Mutex::Autolock l(mMutex);

    mCount++;
    mEntries.push_back(buf);

    return signalConsumerLocked();
}

int Camera2Device::MetadataQueue::getBufferCount() {
    ATRACE_CALL();
    Mutex::Autolock l(mMutex);
    if (mStreamSlotCount > 0) {
        return CAMERA2_REQUEST_QUEUE_IS_BOTTOMLESS;
    }
    return mCount;
}

status_t Camera2Device::MetadataQueue::dequeue(camera_metadata_t **buf,
        bool incrementCount)
{
    ATRACE_CALL();
    ALOGVV("%s: E", __FUNCTION__);
    status_t res;
    Mutex::Autolock l(mMutex);

    if (mCount == 0) {
        if (mStreamSlotCount == 0) {
            ALOGVV("%s: Empty", __FUNCTION__);
            *buf = NULL;
            mSignalConsumer = true;
            return OK;
        }
        ALOGVV("%s: Streaming %d frames to queue", __FUNCTION__,
              mStreamSlotCount);

        for (List<camera_metadata_t*>::iterator slotEntry = mStreamSlot.begin();
                slotEntry != mStreamSlot.end();
                slotEntry++ ) {
            size_t entries = get_camera_metadata_entry_count(*slotEntry);
            size_t dataBytes = get_camera_metadata_data_count(*slotEntry);

            camera_metadata_t *copy =
                    allocate_camera_metadata(entries, dataBytes);
            append_camera_metadata(copy, *slotEntry);
            mEntries.push_back(copy);
        }
        mCount = mStreamSlotCount;
    }
    ALOGVV("MetadataQueue: deque (%d buffers)", mCount);
    camera_metadata_t *b = *(mEntries.begin());
    mEntries.erase(mEntries.begin());

    if (incrementCount) {
        ATRACE_INT("cam2_request", mFrameCount);
        camera_metadata_entry_t frameCount;
        res = find_camera_metadata_entry(b,
                ANDROID_REQUEST_FRAME_COUNT,
                &frameCount);
        if (res != OK) {
            ALOGE("%s: Unable to add frame count: %s (%d)",
                    __FUNCTION__, strerror(-res), res);
        } else {
            *frameCount.data.i32 = mFrameCount;
        }
        mFrameCount++;
    }

    // Check for request ID, and if present, signal waiters.
    camera_metadata_entry_t requestId;
    res = find_camera_metadata_entry(b,
            ANDROID_REQUEST_ID,
            &requestId);
    if (res == OK) {
        mLatestRequestId = requestId.data.i32[0];
        mNewRequestId.signal();
    }

    *buf = b;
    mCount--;

    return OK;
}

status_t Camera2Device::MetadataQueue::waitForBuffer(nsecs_t timeout)
{
    Mutex::Autolock l(mMutex);
    status_t res;
    while (mCount == 0) {
        res = notEmpty.waitRelative(mMutex,timeout);
        if (res != OK) return res;
    }
    return OK;
}

status_t Camera2Device::MetadataQueue::waitForDequeue(int32_t id,
        nsecs_t timeout) {
    Mutex::Autolock l(mMutex);
    status_t res;
    while (mLatestRequestId != id) {
        nsecs_t startTime = systemTime();

        res = mNewRequestId.waitRelative(mMutex, timeout);
        if (res != OK) return res;

        timeout -= (systemTime() - startTime);
    }

    return OK;
}

status_t Camera2Device::MetadataQueue::setStreamSlot(camera_metadata_t *buf)
{
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    Mutex::Autolock l(mMutex);
    if (buf == NULL) {
        freeBuffers(mStreamSlot.begin(), mStreamSlot.end());
        mStreamSlotCount = 0;
        return OK;
    }
    camera_metadata_t *buf2 = clone_camera_metadata(buf);
    if (!buf2) {
        ALOGE("%s: Unable to clone metadata buffer!", __FUNCTION__);
        return NO_MEMORY;
    }

    if (mStreamSlotCount > 1) {
        List<camera_metadata_t*>::iterator deleter = ++mStreamSlot.begin();
        freeBuffers(++mStreamSlot.begin(), mStreamSlot.end());
        mStreamSlotCount = 1;
    }
    if (mStreamSlotCount == 1) {
        free_camera_metadata( *(mStreamSlot.begin()) );
        *(mStreamSlot.begin()) = buf2;
    } else {
        mStreamSlot.push_front(buf2);
        mStreamSlotCount = 1;
    }
    return signalConsumerLocked();
}

status_t Camera2Device::MetadataQueue::setStreamSlot(
        const List<camera_metadata_t*> &bufs)
{
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);
    Mutex::Autolock l(mMutex);

    if (mStreamSlotCount > 0) {
        freeBuffers(mStreamSlot.begin(), mStreamSlot.end());
    }
    mStreamSlotCount = 0;
    for (List<camera_metadata_t*>::const_iterator r = bufs.begin();
         r != bufs.end(); r++) {
        camera_metadata_t *r2 = clone_camera_metadata(*r);
        if (!r2) {
            ALOGE("%s: Unable to clone metadata buffer!", __FUNCTION__);
            return NO_MEMORY;
        }
        mStreamSlot.push_back(r2);
        mStreamSlotCount++;
    }
    return signalConsumerLocked();
}

status_t Camera2Device::MetadataQueue::clear()
{
    ATRACE_CALL();
    ALOGV("%s: E", __FUNCTION__);

    Mutex::Autolock l(mMutex);

    // Clear streaming slot
    freeBuffers(mStreamSlot.begin(), mStreamSlot.end());
    mStreamSlotCount = 0;

    // Clear request queue
    freeBuffers(mEntries.begin(), mEntries.end());
    mCount = 0;
    return OK;
}

status_t Camera2Device::MetadataQueue::dump(int fd,
        const Vector<String16>& /*args*/) {
    ATRACE_CALL();
    String8 result;
    status_t notLocked;
    notLocked = mMutex.tryLock();
    if (notLocked) {
        result.append("    (Unable to lock queue mutex)\n");
    }
    result.appendFormat("      Current frame number: %d\n", mFrameCount);
    if (mStreamSlotCount == 0) {
        result.append("      Stream slot: Empty\n");
        write(fd, result.string(), result.size());
    } else {
        result.appendFormat("      Stream slot: %d entries\n",
                mStreamSlot.size());
        int i = 0;
        for (List<camera_metadata_t*>::iterator r = mStreamSlot.begin();
             r != mStreamSlot.end(); r++) {
            result = String8::format("       Stream slot buffer %d:\n", i);
            write(fd, result.string(), result.size());
            dump_indented_camera_metadata(*r, fd, 2, 10);
            i++;
        }
    }
    if (mEntries.size() == 0) {
        result = "      Main queue is empty\n";
        write(fd, result.string(), result.size());
    } else {
        result = String8::format("      Main queue has %d entries:\n",
                mEntries.size());
        int i = 0;
        for (List<camera_metadata_t*>::iterator r = mEntries.begin();
             r != mEntries.end(); r++) {
            result = String8::format("       Queue entry %d:\n", i);
            write(fd, result.string(), result.size());
            dump_indented_camera_metadata(*r, fd, 2, 10);
            i++;
        }
    }

    if (notLocked == 0) {
        mMutex.unlock();
    }

    return OK;
}

status_t Camera2Device::MetadataQueue::signalConsumerLocked() {
    ATRACE_CALL();
    status_t res = OK;
    notEmpty.signal();
    if (mSignalConsumer && mHal2Device != NULL) {
        mSignalConsumer = false;

        mMutex.unlock();
        ALOGV("%s: Signaling consumer", __FUNCTION__);
        res = mHal2Device->ops->notify_request_queue_not_empty(mHal2Device);
        mMutex.lock();
    }
    return res;
}

status_t Camera2Device::MetadataQueue::freeBuffers(
        List<camera_metadata_t*>::iterator start,
        List<camera_metadata_t*>::iterator end)
{
    ATRACE_CALL();
    while (start != end) {
        free_camera_metadata(*start);
        start = mStreamSlot.erase(start);
    }
    return OK;
}

Camera2Device::MetadataQueue* Camera2Device::MetadataQueue::getInstance(
        const camera2_request_queue_src_ops_t *q)
{
    const MetadataQueue* cmq = static_cast<const MetadataQueue*>(q);
    return const_cast<MetadataQueue*>(cmq);
}

Camera2Device::MetadataQueue* Camera2Device::MetadataQueue::getInstance(
        const camera2_frame_queue_dst_ops_t *q)
{
    const MetadataQueue* cmq = static_cast<const MetadataQueue*>(q);
    return const_cast<MetadataQueue*>(cmq);
}

int Camera2Device::MetadataQueue::consumer_buffer_count(
        const camera2_request_queue_src_ops_t *q)
{
    MetadataQueue *queue = getInstance(q);
    return queue->getBufferCount();
}

int Camera2Device::MetadataQueue::consumer_dequeue(
        const camera2_request_queue_src_ops_t *q,
        camera_metadata_t **buffer)
{
    MetadataQueue *queue = getInstance(q);
    return queue->dequeue(buffer, true);
}

int Camera2Device::MetadataQueue::consumer_free(
        const camera2_request_queue_src_ops_t *q,
        camera_metadata_t *old_buffer)
{
    ATRACE_CALL();
    MetadataQueue *queue = getInstance(q);
    (void)queue;
    free_camera_metadata(old_buffer);
    return OK;
}

int Camera2Device::MetadataQueue::producer_dequeue(
        const camera2_frame_queue_dst_ops_t * /*q*/,
        size_t entries, size_t bytes,
        camera_metadata_t **buffer)
{
    ATRACE_CALL();
    camera_metadata_t *new_buffer =
            allocate_camera_metadata(entries, bytes);
    if (new_buffer == NULL) return NO_MEMORY;
    *buffer = new_buffer;
        return OK;
}

int Camera2Device::MetadataQueue::producer_cancel(
        const camera2_frame_queue_dst_ops_t * /*q*/,
        camera_metadata_t *old_buffer)
{
    ATRACE_CALL();
    free_camera_metadata(old_buffer);
    return OK;
}

int Camera2Device::MetadataQueue::producer_enqueue(
        const camera2_frame_queue_dst_ops_t *q,
        camera_metadata_t *filled_buffer)
{
    MetadataQueue *queue = getInstance(q);
    return queue->enqueue(filled_buffer);
}

/**
 * Camera2Device::StreamAdapter
 */

#ifndef container_of
#define container_of(ptr, type, member) \
    (type *)((char*)(ptr) - offsetof(type, member))
#endif

Camera2Device::StreamAdapter::StreamAdapter(camera2_device_t *d):
        mState(RELEASED),
        mHal2Device(d),
        mId(-1),
        mWidth(0), mHeight(0), mFormat(0), mSize(0), mUsage(0),
        mMaxProducerBuffers(0), mMaxConsumerBuffers(0),
        mTotalBuffers(0),
        mFormatRequested(0),
        mActiveBuffers(0),
        mFrameCount(0),
        mLastTimestamp(0)
{
    camera2_stream_ops::dequeue_buffer = dequeue_buffer;
    camera2_stream_ops::enqueue_buffer = enqueue_buffer;
    camera2_stream_ops::cancel_buffer = cancel_buffer;
    camera2_stream_ops::set_crop = set_crop;
}

Camera2Device::StreamAdapter::~StreamAdapter() {
    ATRACE_CALL();
    if (mState != RELEASED) {
        release();
    }
}

status_t Camera2Device::StreamAdapter::connectToDevice(
        sp<ANativeWindow> consumer,
        uint32_t width, uint32_t height, int format, size_t size) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: E", __FUNCTION__);

    if (mState != RELEASED) return INVALID_OPERATION;
    if (consumer == NULL) {
        ALOGE("%s: Null consumer passed to stream adapter", __FUNCTION__);
        return BAD_VALUE;
    }

    ALOGV("%s: New stream parameters %d x %d, format 0x%x, size %d",
            __FUNCTION__, width, height, format, size);

    mConsumerInterface = consumer;
    mWidth = width;
    mHeight = height;
    mSize = (format == HAL_PIXEL_FORMAT_BLOB) ? size : 0;
    mFormatRequested = format;

    // Allocate device-side stream interface

    uint32_t id;
    uint32_t formatActual;
    uint32_t usage;
    uint32_t maxBuffers = 2;
    res = mHal2Device->ops->allocate_stream(mHal2Device,
            mWidth, mHeight, mFormatRequested, getStreamOps(),
            &id, &formatActual, &usage, &maxBuffers);
    if (res != OK) {
        ALOGE("%s: Device stream allocation failed: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }

    ALOGV("%s: Allocated stream id %d, actual format 0x%x, "
            "usage 0x%x, producer wants %d buffers", __FUNCTION__,
            id, formatActual, usage, maxBuffers);

    mId = id;
    mFormat = formatActual;
    mUsage = usage;
    mMaxProducerBuffers = maxBuffers;

    mState = ALLOCATED;

    // Configure consumer-side ANativeWindow interface
    res = native_window_api_connect(mConsumerInterface.get(),
            NATIVE_WINDOW_API_CAMERA);
    if (res != OK) {
        ALOGE("%s: Unable to connect to native window for stream %d",
                __FUNCTION__, mId);

        return res;
    }

    mState = CONNECTED;

    res = native_window_set_usage(mConsumerInterface.get(), mUsage);
    if (res != OK) {
        ALOGE("%s: Unable to configure usage %08x for stream %d",
                __FUNCTION__, mUsage, mId);
        return res;
    }

    res = native_window_set_scaling_mode(mConsumerInterface.get(),
            NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
    if (res != OK) {
        ALOGE("%s: Unable to configure stream scaling: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }

    res = setTransform(0);
    if (res != OK) {
        return res;
    }

    if (mFormat == HAL_PIXEL_FORMAT_BLOB) {
        res = native_window_set_buffers_geometry(mConsumerInterface.get(),
                mSize, 1, mFormat);
        if (res != OK) {
            ALOGE("%s: Unable to configure compressed stream buffer geometry"
                    " %d x %d, size %d for stream %d",
                    __FUNCTION__, mWidth, mHeight, mSize, mId);
            return res;
        }
    } else {
        res = native_window_set_buffers_geometry(mConsumerInterface.get(),
                mWidth, mHeight, mFormat);
        if (res != OK) {
            ALOGE("%s: Unable to configure stream buffer geometry"
                    " %d x %d, format 0x%x for stream %d",
                    __FUNCTION__, mWidth, mHeight, mFormat, mId);
            return res;
        }
    }

    int maxConsumerBuffers;
    res = mConsumerInterface->query(mConsumerInterface.get(),
            NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &maxConsumerBuffers);
    if (res != OK) {
        ALOGE("%s: Unable to query consumer undequeued"
                " buffer count for stream %d", __FUNCTION__, mId);
        return res;
    }
    mMaxConsumerBuffers = maxConsumerBuffers;

    ALOGV("%s: Consumer wants %d buffers", __FUNCTION__,
            mMaxConsumerBuffers);

    mTotalBuffers = mMaxConsumerBuffers + mMaxProducerBuffers;
    mActiveBuffers = 0;
    mFrameCount = 0;
    mLastTimestamp = 0;

    res = native_window_set_buffer_count(mConsumerInterface.get(),
            mTotalBuffers);
    if (res != OK) {
        ALOGE("%s: Unable to set buffer count for stream %d",
                __FUNCTION__, mId);
        return res;
    }

    // Register allocated buffers with HAL device
    buffer_handle_t *buffers = new buffer_handle_t[mTotalBuffers];
    ANativeWindowBuffer **anwBuffers = new ANativeWindowBuffer*[mTotalBuffers];
    uint32_t bufferIdx = 0;
    for (; bufferIdx < mTotalBuffers; bufferIdx++) {
        res = native_window_dequeue_buffer_and_wait(mConsumerInterface.get(),
                &anwBuffers[bufferIdx]);
        if (res != OK) {
            ALOGE("%s: Unable to dequeue buffer %d for initial registration for "
                    "stream %d", __FUNCTION__, bufferIdx, mId);
            goto cleanUpBuffers;
        }

        buffers[bufferIdx] = anwBuffers[bufferIdx]->handle;
        ALOGV("%s: Buffer %p allocated", __FUNCTION__, (void*)buffers[bufferIdx]);
    }

    ALOGV("%s: Registering %d buffers with camera HAL", __FUNCTION__, mTotalBuffers);
    res = mHal2Device->ops->register_stream_buffers(mHal2Device,
            mId,
            mTotalBuffers,
            buffers);
    if (res != OK) {
        ALOGE("%s: Unable to register buffers with HAL device for stream %d",
                __FUNCTION__, mId);
    } else {
        mState = ACTIVE;
    }

cleanUpBuffers:
    ALOGV("%s: Cleaning up %d buffers", __FUNCTION__, bufferIdx);
    for (uint32_t i = 0; i < bufferIdx; i++) {
        res = mConsumerInterface->cancelBuffer(mConsumerInterface.get(),
                anwBuffers[i], -1);
        if (res != OK) {
            ALOGE("%s: Unable to cancel buffer %d after registration",
                    __FUNCTION__, i);
        }
    }
    delete[] anwBuffers;
    delete[] buffers;

    return res;
}

status_t Camera2Device::StreamAdapter::release() {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: Releasing stream %d (%d x %d, format %d)", __FUNCTION__, mId,
            mWidth, mHeight, mFormat);
    if (mState >= ALLOCATED) {
        res = mHal2Device->ops->release_stream(mHal2Device, mId);
        if (res != OK) {
            ALOGE("%s: Unable to release stream %d",
                    __FUNCTION__, mId);
            return res;
        }
    }
    if (mState >= CONNECTED) {
        res = native_window_api_disconnect(mConsumerInterface.get(),
                NATIVE_WINDOW_API_CAMERA);

        /* this is not an error. if client calling process dies,
           the window will also die and all calls to it will return
           DEAD_OBJECT, thus it's already "disconnected" */
        if (res == DEAD_OBJECT) {
            ALOGW("%s: While disconnecting stream %d from native window, the"
                  " native window died from under us", __FUNCTION__, mId);
        }
        else if (res != OK) {
            ALOGE("%s: Unable to disconnect stream %d from native window (error %d %s)",
                    __FUNCTION__, mId, res, strerror(-res));
            return res;
        }
    }
    mId = -1;
    mState = RELEASED;
    return OK;
}

status_t Camera2Device::StreamAdapter::setTransform(int transform) {
    ATRACE_CALL();
    status_t res;
    if (mState < CONNECTED) {
        ALOGE("%s: Cannot set transform on unconnected stream", __FUNCTION__);
        return INVALID_OPERATION;
    }
    res = native_window_set_buffers_transform(mConsumerInterface.get(),
                                              transform);
    if (res != OK) {
        ALOGE("%s: Unable to configure stream transform to %x: %s (%d)",
                __FUNCTION__, transform, strerror(-res), res);
    }
    return res;
}

status_t Camera2Device::StreamAdapter::dump(int fd,
        const Vector<String16>& /*args*/) {
    ATRACE_CALL();
    String8 result = String8::format("      Stream %d: %d x %d, format 0x%x\n",
            mId, mWidth, mHeight, mFormat);
    result.appendFormat("        size %d, usage 0x%x, requested format 0x%x\n",
            mSize, mUsage, mFormatRequested);
    result.appendFormat("        total buffers: %d, dequeued buffers: %d\n",
            mTotalBuffers, mActiveBuffers);
    result.appendFormat("        frame count: %d, last timestamp %lld\n",
            mFrameCount, mLastTimestamp);
    write(fd, result.string(), result.size());
    return OK;
}

const camera2_stream_ops *Camera2Device::StreamAdapter::getStreamOps() {
    return static_cast<camera2_stream_ops *>(this);
}

ANativeWindow* Camera2Device::StreamAdapter::toANW(
        const camera2_stream_ops_t *w) {
    return static_cast<const StreamAdapter*>(w)->mConsumerInterface.get();
}

int Camera2Device::StreamAdapter::dequeue_buffer(const camera2_stream_ops_t *w,
        buffer_handle_t** buffer) {
    ATRACE_CALL();
    int res;
    StreamAdapter* stream =
            const_cast<StreamAdapter*>(static_cast<const StreamAdapter*>(w));
    if (stream->mState != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, stream->mState);
        return INVALID_OPERATION;
    }

    ANativeWindow *a = toANW(w);
    ANativeWindowBuffer* anb;
    res = native_window_dequeue_buffer_and_wait(a, &anb);
    if (res != OK) {
        ALOGE("Stream %d dequeue: Error from native_window: %s (%d)", stream->mId,
                strerror(-res), res);
        return res;
    }

    *buffer = &(anb->handle);
    stream->mActiveBuffers++;

    ALOGVV("Stream %d dequeue: Buffer %p dequeued", stream->mId, (void*)(**buffer));
    return res;
}

int Camera2Device::StreamAdapter::enqueue_buffer(const camera2_stream_ops_t* w,
        int64_t timestamp,
        buffer_handle_t* buffer) {
    ATRACE_CALL();
    StreamAdapter *stream =
            const_cast<StreamAdapter*>(static_cast<const StreamAdapter*>(w));
    stream->mFrameCount++;
    ALOGVV("Stream %d enqueue: Frame %d (%p) captured at %lld ns",
            stream->mId, stream->mFrameCount, (void*)(*buffer), timestamp);
    int state = stream->mState;
    if (state != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, state);
        return INVALID_OPERATION;
    }
    ANativeWindow *a = toANW(w);
    status_t err;

    err = native_window_set_buffers_timestamp(a, timestamp);
    if (err != OK) {
        ALOGE("%s: Error setting timestamp on native window: %s (%d)",
                __FUNCTION__, strerror(-err), err);
        return err;
    }
    err = a->queueBuffer(a,
            container_of(buffer, ANativeWindowBuffer, handle), -1);
    if (err != OK) {
        ALOGE("%s: Error queueing buffer to native window: %s (%d)",
                __FUNCTION__, strerror(-err), err);
        return err;
    }

    stream->mActiveBuffers--;
    stream->mLastTimestamp = timestamp;
    return OK;
}

int Camera2Device::StreamAdapter::cancel_buffer(const camera2_stream_ops_t* w,
        buffer_handle_t* buffer) {
    ATRACE_CALL();
    StreamAdapter *stream =
            const_cast<StreamAdapter*>(static_cast<const StreamAdapter*>(w));
    ALOGVV("Stream %d cancel: Buffer %p",
            stream->mId, (void*)(*buffer));
    if (stream->mState != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, stream->mState);
        return INVALID_OPERATION;
    }

    ANativeWindow *a = toANW(w);
    int err = a->cancelBuffer(a,
            container_of(buffer, ANativeWindowBuffer, handle), -1);
    if (err != OK) {
        ALOGE("%s: Error canceling buffer to native window: %s (%d)",
                __FUNCTION__, strerror(-err), err);
        return err;
    }

    stream->mActiveBuffers--;
    return OK;
}

int Camera2Device::StreamAdapter::set_crop(const camera2_stream_ops_t* w,
        int left, int top, int right, int bottom) {
    ATRACE_CALL();
    int state = static_cast<const StreamAdapter*>(w)->mState;
    if (state != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, state);
        return INVALID_OPERATION;
    }
    ANativeWindow *a = toANW(w);
    android_native_rect_t crop = { left, top, right, bottom };
    return native_window_set_crop(a, &crop);
}

/**
 * Camera2Device::ReprocessStreamAdapter
 */

#ifndef container_of
#define container_of(ptr, type, member) \
    (type *)((char*)(ptr) - offsetof(type, member))
#endif

Camera2Device::ReprocessStreamAdapter::ReprocessStreamAdapter(camera2_device_t *d):
        mState(RELEASED),
        mHal2Device(d),
        mId(-1),
        mWidth(0), mHeight(0), mFormat(0),
        mActiveBuffers(0),
        mFrameCount(0)
{
    ATRACE_CALL();
    camera2_stream_in_ops::acquire_buffer = acquire_buffer;
    camera2_stream_in_ops::release_buffer = release_buffer;
}

Camera2Device::ReprocessStreamAdapter::~ReprocessStreamAdapter() {
    ATRACE_CALL();
    if (mState != RELEASED) {
        release();
    }
}

status_t Camera2Device::ReprocessStreamAdapter::connectToDevice(
        const sp<StreamAdapter> &outputStream) {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: E", __FUNCTION__);

    if (mState != RELEASED) return INVALID_OPERATION;
    if (outputStream == NULL) {
        ALOGE("%s: Null base stream passed to reprocess stream adapter",
                __FUNCTION__);
        return BAD_VALUE;
    }

    mBaseStream = outputStream;
    mWidth = outputStream->getWidth();
    mHeight = outputStream->getHeight();
    mFormat = outputStream->getFormat();

    ALOGV("%s: New reprocess stream parameters %d x %d, format 0x%x",
            __FUNCTION__, mWidth, mHeight, mFormat);

    // Allocate device-side stream interface

    uint32_t id;
    res = mHal2Device->ops->allocate_reprocess_stream_from_stream(mHal2Device,
            outputStream->getId(), getStreamOps(),
            &id);
    if (res != OK) {
        ALOGE("%s: Device reprocess stream allocation failed: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }

    ALOGV("%s: Allocated reprocess stream id %d based on stream %d",
            __FUNCTION__, id, outputStream->getId());

    mId = id;

    mState = ACTIVE;

    return OK;
}

status_t Camera2Device::ReprocessStreamAdapter::release() {
    ATRACE_CALL();
    status_t res;
    ALOGV("%s: Releasing stream %d", __FUNCTION__, mId);
    if (mState >= ACTIVE) {
        res = mHal2Device->ops->release_reprocess_stream(mHal2Device, mId);
        if (res != OK) {
            ALOGE("%s: Unable to release stream %d",
                    __FUNCTION__, mId);
            return res;
        }
    }

    List<QueueEntry>::iterator s;
    for (s = mQueue.begin(); s != mQueue.end(); s++) {
        sp<BufferReleasedListener> listener = s->releaseListener.promote();
        if (listener != 0) listener->onBufferReleased(s->handle);
    }
    for (s = mInFlightQueue.begin(); s != mInFlightQueue.end(); s++) {
        sp<BufferReleasedListener> listener = s->releaseListener.promote();
        if (listener != 0) listener->onBufferReleased(s->handle);
    }
    mQueue.clear();
    mInFlightQueue.clear();

    mState = RELEASED;
    return OK;
}

status_t Camera2Device::ReprocessStreamAdapter::pushIntoStream(
    buffer_handle_t *handle, const wp<BufferReleasedListener> &releaseListener) {
    ATRACE_CALL();
    // TODO: Some error checking here would be nice
    ALOGV("%s: Pushing buffer %p to stream", __FUNCTION__, (void*)(*handle));

    QueueEntry entry;
    entry.handle = handle;
    entry.releaseListener = releaseListener;
    mQueue.push_back(entry);
    return OK;
}

status_t Camera2Device::ReprocessStreamAdapter::dump(int fd,
        const Vector<String16>& /*args*/) {
    ATRACE_CALL();
    String8 result =
            String8::format("      Reprocess stream %d: %d x %d, fmt 0x%x\n",
                    mId, mWidth, mHeight, mFormat);
    result.appendFormat("        acquired buffers: %d\n",
            mActiveBuffers);
    result.appendFormat("        frame count: %d\n",
            mFrameCount);
    write(fd, result.string(), result.size());
    return OK;
}

const camera2_stream_in_ops *Camera2Device::ReprocessStreamAdapter::getStreamOps() {
    return static_cast<camera2_stream_in_ops *>(this);
}

int Camera2Device::ReprocessStreamAdapter::acquire_buffer(
    const camera2_stream_in_ops_t *w,
        buffer_handle_t** buffer) {
    ATRACE_CALL();

    ReprocessStreamAdapter* stream =
            const_cast<ReprocessStreamAdapter*>(
                static_cast<const ReprocessStreamAdapter*>(w));
    if (stream->mState != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, stream->mState);
        return INVALID_OPERATION;
    }

    if (stream->mQueue.empty()) {
        *buffer = NULL;
        return OK;
    }

    QueueEntry &entry = *(stream->mQueue.begin());

    *buffer = entry.handle;

    stream->mInFlightQueue.push_back(entry);
    stream->mQueue.erase(stream->mQueue.begin());

    stream->mActiveBuffers++;

    ALOGV("Stream %d acquire: Buffer %p acquired", stream->mId,
            (void*)(**buffer));
    return OK;
}

int Camera2Device::ReprocessStreamAdapter::release_buffer(
    const camera2_stream_in_ops_t* w,
    buffer_handle_t* buffer) {
    ATRACE_CALL();
    ReprocessStreamAdapter *stream =
            const_cast<ReprocessStreamAdapter*>(
                static_cast<const ReprocessStreamAdapter*>(w) );
    stream->mFrameCount++;
    ALOGV("Reprocess stream %d release: Frame %d (%p)",
            stream->mId, stream->mFrameCount, (void*)*buffer);
    int state = stream->mState;
    if (state != ACTIVE) {
        ALOGE("%s: Called when in bad state: %d", __FUNCTION__, state);
        return INVALID_OPERATION;
    }
    stream->mActiveBuffers--;

    List<QueueEntry>::iterator s;
    for (s = stream->mInFlightQueue.begin(); s != stream->mInFlightQueue.end(); s++) {
        if ( s->handle == buffer ) break;
    }
    if (s == stream->mInFlightQueue.end()) {
        ALOGE("%s: Can't find buffer %p in in-flight list!", __FUNCTION__,
                buffer);
        return INVALID_OPERATION;
    }

    sp<BufferReleasedListener> listener = s->releaseListener.promote();
    if (listener != 0) {
        listener->onBufferReleased(s->handle);
    } else {
        ALOGE("%s: Can't free buffer - missing listener", __FUNCTION__);
    }
    stream->mInFlightQueue.erase(s);

    return OK;
}

}; // namespace android
