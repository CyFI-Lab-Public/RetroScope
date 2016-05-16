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

#define LOG_TAG "Camera3-OutputStream"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>
#include "Camera3OutputStream.h"

#ifndef container_of
#define container_of(ptr, type, member) \
    (type *)((char*)(ptr) - offsetof(type, member))
#endif

namespace android {

namespace camera3 {

Camera3OutputStream::Camera3OutputStream(int id,
        sp<ANativeWindow> consumer,
        uint32_t width, uint32_t height, int format) :
        Camera3IOStreamBase(id, CAMERA3_STREAM_OUTPUT, width, height,
                            /*maxSize*/0, format),
        mConsumer(consumer),
        mTransform(0) {

    if (mConsumer == NULL) {
        ALOGE("%s: Consumer is NULL!", __FUNCTION__);
        mState = STATE_ERROR;
    }
}

Camera3OutputStream::Camera3OutputStream(int id,
        sp<ANativeWindow> consumer,
        uint32_t width, uint32_t height, size_t maxSize, int format) :
        Camera3IOStreamBase(id, CAMERA3_STREAM_OUTPUT, width, height, maxSize,
                            format),
        mConsumer(consumer),
        mTransform(0) {

    if (format != HAL_PIXEL_FORMAT_BLOB) {
        ALOGE("%s: Bad format for size-only stream: %d", __FUNCTION__,
                format);
        mState = STATE_ERROR;
    }

    if (mConsumer == NULL) {
        ALOGE("%s: Consumer is NULL!", __FUNCTION__);
        mState = STATE_ERROR;
    }
}

Camera3OutputStream::Camera3OutputStream(int id, camera3_stream_type_t type,
                                         uint32_t width, uint32_t height,
                                         int format) :
        Camera3IOStreamBase(id, type, width, height,
                            /*maxSize*/0,
                            format),
        mTransform(0) {

    // Subclasses expected to initialize mConsumer themselves
}


Camera3OutputStream::~Camera3OutputStream() {
    disconnectLocked();
}

status_t Camera3OutputStream::getBufferLocked(camera3_stream_buffer *buffer) {
    ATRACE_CALL();
    status_t res;

    if ((res = getBufferPreconditionCheckLocked()) != OK) {
        return res;
    }

    ANativeWindowBuffer* anb;
    int fenceFd;

    /**
     * Release the lock briefly to avoid deadlock for below scenario:
     * Thread 1: StreamingProcessor::startStream -> Camera3Stream::isConfiguring().
     * This thread acquired StreamingProcessor lock and try to lock Camera3Stream lock.
     * Thread 2: Camera3Stream::returnBuffer->StreamingProcessor::onFrameAvailable().
     * This thread acquired Camera3Stream lock and bufferQueue lock, and try to lock
     * StreamingProcessor lock.
     * Thread 3: Camera3Stream::getBuffer(). This thread acquired Camera3Stream lock
     * and try to lock bufferQueue lock.
     * Then there is circular locking dependency.
     */
    sp<ANativeWindow> currentConsumer = mConsumer;
    mLock.unlock();

    res = currentConsumer->dequeueBuffer(currentConsumer.get(), &anb, &fenceFd);
    mLock.lock();
    if (res != OK) {
        ALOGE("%s: Stream %d: Can't dequeue next output buffer: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        return res;
    }

    /**
     * FenceFD now owned by HAL except in case of error,
     * in which case we reassign it to acquire_fence
     */
    handoutBufferLocked(*buffer, &(anb->handle), /*acquireFence*/fenceFd,
                        /*releaseFence*/-1, CAMERA3_BUFFER_STATUS_OK);

    return OK;
}

status_t Camera3OutputStream::returnBufferLocked(
        const camera3_stream_buffer &buffer,
        nsecs_t timestamp) {
    ATRACE_CALL();

    status_t res = returnAnyBufferLocked(buffer, timestamp, /*output*/true);

    if (res != OK) {
        return res;
    }

    mLastTimestamp = timestamp;

    return OK;
}

status_t Camera3OutputStream::returnBufferCheckedLocked(
            const camera3_stream_buffer &buffer,
            nsecs_t timestamp,
            bool output,
            /*out*/
            sp<Fence> *releaseFenceOut) {

    (void)output;
    ALOG_ASSERT(output, "Expected output to be true");

    status_t res;
    sp<Fence> releaseFence;

    /**
     * Fence management - calculate Release Fence
     */
    if (buffer.status == CAMERA3_BUFFER_STATUS_ERROR) {
        if (buffer.release_fence != -1) {
            ALOGE("%s: Stream %d: HAL should not set release_fence(%d) when "
                  "there is an error", __FUNCTION__, mId, buffer.release_fence);
            close(buffer.release_fence);
        }

        /**
         * Reassign release fence as the acquire fence in case of error
         */
        releaseFence = new Fence(buffer.acquire_fence);
    } else {
        res = native_window_set_buffers_timestamp(mConsumer.get(), timestamp);
        if (res != OK) {
            ALOGE("%s: Stream %d: Error setting timestamp: %s (%d)",
                  __FUNCTION__, mId, strerror(-res), res);
            return res;
        }

        releaseFence = new Fence(buffer.release_fence);
    }

    int anwReleaseFence = releaseFence->dup();

    /**
     * Release the lock briefly to avoid deadlock with
     * StreamingProcessor::startStream -> Camera3Stream::isConfiguring (this
     * thread will go into StreamingProcessor::onFrameAvailable) during
     * queueBuffer
     */
    sp<ANativeWindow> currentConsumer = mConsumer;
    mLock.unlock();

    /**
     * Return buffer back to ANativeWindow
     */
    if (buffer.status == CAMERA3_BUFFER_STATUS_ERROR) {
        // Cancel buffer
        res = currentConsumer->cancelBuffer(currentConsumer.get(),
                container_of(buffer.buffer, ANativeWindowBuffer, handle),
                anwReleaseFence);
        if (res != OK) {
            ALOGE("%s: Stream %d: Error cancelling buffer to native window:"
                  " %s (%d)", __FUNCTION__, mId, strerror(-res), res);
        }
    } else {
        res = currentConsumer->queueBuffer(currentConsumer.get(),
                container_of(buffer.buffer, ANativeWindowBuffer, handle),
                anwReleaseFence);
        if (res != OK) {
            ALOGE("%s: Stream %d: Error queueing buffer to native window: "
                  "%s (%d)", __FUNCTION__, mId, strerror(-res), res);
        }
    }
    mLock.lock();
    if (res != OK) {
        close(anwReleaseFence);
    }

    *releaseFenceOut = releaseFence;

    return res;
}

void Camera3OutputStream::dump(int fd, const Vector<String16> &args) const {
    (void) args;
    String8 lines;
    lines.appendFormat("    Stream[%d]: Output\n", mId);
    write(fd, lines.string(), lines.size());

    Camera3IOStreamBase::dump(fd, args);
}

status_t Camera3OutputStream::setTransform(int transform) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);
    return setTransformLocked(transform);
}

status_t Camera3OutputStream::setTransformLocked(int transform) {
    status_t res = OK;
    if (mState == STATE_ERROR) {
        ALOGE("%s: Stream in error state", __FUNCTION__);
        return INVALID_OPERATION;
    }

    mTransform = transform;
    if (mState == STATE_CONFIGURED) {
        res = native_window_set_buffers_transform(mConsumer.get(),
                transform);
        if (res != OK) {
            ALOGE("%s: Unable to configure stream transform to %x: %s (%d)",
                    __FUNCTION__, transform, strerror(-res), res);
        }
    }
    return res;
}

status_t Camera3OutputStream::configureQueueLocked() {
    status_t res;

    if ((res = Camera3IOStreamBase::configureQueueLocked()) != OK) {
        return res;
    }

    ALOG_ASSERT(mConsumer != 0, "mConsumer should never be NULL");

    // Configure consumer-side ANativeWindow interface
    res = native_window_api_connect(mConsumer.get(),
            NATIVE_WINDOW_API_CAMERA);
    if (res != OK) {
        ALOGE("%s: Unable to connect to native window for stream %d",
                __FUNCTION__, mId);
        return res;
    }

    res = native_window_set_usage(mConsumer.get(), camera3_stream::usage);
    if (res != OK) {
        ALOGE("%s: Unable to configure usage %08x for stream %d",
                __FUNCTION__, camera3_stream::usage, mId);
        return res;
    }

    res = native_window_set_scaling_mode(mConsumer.get(),
            NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
    if (res != OK) {
        ALOGE("%s: Unable to configure stream scaling: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        return res;
    }

    if (mMaxSize == 0) {
        // For buffers of known size
        res = native_window_set_buffers_geometry(mConsumer.get(),
                camera3_stream::width, camera3_stream::height,
                camera3_stream::format);
    } else {
        // For buffers with bounded size
        res = native_window_set_buffers_geometry(mConsumer.get(),
                mMaxSize, 1,
                camera3_stream::format);
    }
    if (res != OK) {
        ALOGE("%s: Unable to configure stream buffer geometry"
                " %d x %d, format %x for stream %d",
                __FUNCTION__, camera3_stream::width, camera3_stream::height,
                camera3_stream::format, mId);
        return res;
    }

    int maxConsumerBuffers;
    res = mConsumer->query(mConsumer.get(),
            NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &maxConsumerBuffers);
    if (res != OK) {
        ALOGE("%s: Unable to query consumer undequeued"
                " buffer count for stream %d", __FUNCTION__, mId);
        return res;
    }

    ALOGV("%s: Consumer wants %d buffers, HAL wants %d", __FUNCTION__,
            maxConsumerBuffers, camera3_stream::max_buffers);
    if (camera3_stream::max_buffers == 0) {
        ALOGE("%s: Camera HAL requested max_buffer count: %d, requires at least 1",
                __FUNCTION__, camera3_stream::max_buffers);
        return INVALID_OPERATION;
    }

    mTotalBufferCount = maxConsumerBuffers + camera3_stream::max_buffers;
    mDequeuedBufferCount = 0;
    mFrameCount = 0;
    mLastTimestamp = 0;

    res = native_window_set_buffer_count(mConsumer.get(),
            mTotalBufferCount);
    if (res != OK) {
        ALOGE("%s: Unable to set buffer count for stream %d",
                __FUNCTION__, mId);
        return res;
    }

    res = native_window_set_buffers_transform(mConsumer.get(),
            mTransform);
    if (res != OK) {
        ALOGE("%s: Unable to configure stream transform to %x: %s (%d)",
                __FUNCTION__, mTransform, strerror(-res), res);
    }

    return OK;
}

status_t Camera3OutputStream::disconnectLocked() {
    status_t res;

    if ((res = Camera3IOStreamBase::disconnectLocked()) != OK) {
        return res;
    }

    res = native_window_api_disconnect(mConsumer.get(),
                                       NATIVE_WINDOW_API_CAMERA);

    /**
     * This is not an error. if client calling process dies, the window will
     * also die and all calls to it will return DEAD_OBJECT, thus it's already
     * "disconnected"
     */
    if (res == DEAD_OBJECT) {
        ALOGW("%s: While disconnecting stream %d from native window, the"
                " native window died from under us", __FUNCTION__, mId);
    }
    else if (res != OK) {
        ALOGE("%s: Unable to disconnect stream %d from native window "
              "(error %d %s)",
              __FUNCTION__, mId, res, strerror(-res));
        mState = STATE_ERROR;
        return res;
    }

    mState = (mState == STATE_IN_RECONFIG) ? STATE_IN_CONFIG
                                           : STATE_CONSTRUCTED;
    return OK;
}

status_t Camera3OutputStream::getEndpointUsage(uint32_t *usage) {

    status_t res;
    int32_t u = 0;
    res = mConsumer->query(mConsumer.get(),
            NATIVE_WINDOW_CONSUMER_USAGE_BITS, &u);
    *usage = u;

    return res;
}

}; // namespace camera3

}; // namespace android
