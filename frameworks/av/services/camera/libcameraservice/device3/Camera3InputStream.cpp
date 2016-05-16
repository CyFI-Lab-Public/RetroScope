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

#define LOG_TAG "Camera3-InputStream"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>
#include "Camera3InputStream.h"

namespace android {

namespace camera3 {

Camera3InputStream::Camera3InputStream(int id,
        uint32_t width, uint32_t height, int format) :
        Camera3IOStreamBase(id, CAMERA3_STREAM_INPUT, width, height,
                            /*maxSize*/0, format) {

    if (format == HAL_PIXEL_FORMAT_BLOB) {
        ALOGE("%s: Bad format, BLOB not supported", __FUNCTION__);
        mState = STATE_ERROR;
    }
}

Camera3InputStream::~Camera3InputStream() {
    disconnectLocked();
}

status_t Camera3InputStream::getInputBufferLocked(
        camera3_stream_buffer *buffer) {
    ATRACE_CALL();
    status_t res;

    // FIXME: will not work in (re-)registration
    if (mState == STATE_IN_CONFIG || mState == STATE_IN_RECONFIG) {
        ALOGE("%s: Stream %d: Buffer registration for input streams"
              " not implemented (state %d)",
              __FUNCTION__, mId, mState);
        return INVALID_OPERATION;
    }

    if ((res = getBufferPreconditionCheckLocked()) != OK) {
        return res;
    }

    ANativeWindowBuffer* anb;
    int fenceFd;

    assert(mConsumer != 0);

    BufferItem bufferItem;
    res = mConsumer->acquireBuffer(&bufferItem, /*waitForFence*/false);

    if (res != OK) {
        ALOGE("%s: Stream %d: Can't acquire next output buffer: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        return res;
    }

    anb = bufferItem.mGraphicBuffer->getNativeBuffer();
    assert(anb != NULL);
    fenceFd = bufferItem.mFence->dup();

    /**
     * FenceFD now owned by HAL except in case of error,
     * in which case we reassign it to acquire_fence
     */
    handoutBufferLocked(*buffer, &(anb->handle), /*acquireFence*/fenceFd,
                        /*releaseFence*/-1, CAMERA3_BUFFER_STATUS_OK);
    mBuffersInFlight.push_back(bufferItem);

    return OK;
}

status_t Camera3InputStream::returnBufferCheckedLocked(
            const camera3_stream_buffer &buffer,
            nsecs_t timestamp,
            bool output,
            /*out*/
            sp<Fence> *releaseFenceOut) {

    (void)timestamp;
    (void)output;
    ALOG_ASSERT(!output, "Expected output to be false");

    status_t res;

    bool bufferFound = false;
    BufferItem bufferItem;
    {
        // Find the buffer we are returning
        Vector<BufferItem>::iterator it, end;
        for (it = mBuffersInFlight.begin(), end = mBuffersInFlight.end();
             it != end;
             ++it) {

            const BufferItem& tmp = *it;
            ANativeWindowBuffer *anb = tmp.mGraphicBuffer->getNativeBuffer();
            if (anb != NULL && &(anb->handle) == buffer.buffer) {
                bufferFound = true;
                bufferItem = tmp;
                mBuffersInFlight.erase(it);
            }
        }
    }
    if (!bufferFound) {
        ALOGE("%s: Stream %d: Can't return buffer that wasn't sent to HAL",
              __FUNCTION__, mId);
        return INVALID_OPERATION;
    }

    if (buffer.status == CAMERA3_BUFFER_STATUS_ERROR) {
        if (buffer.release_fence != -1) {
            ALOGE("%s: Stream %d: HAL should not set release_fence(%d) when "
                  "there is an error", __FUNCTION__, mId, buffer.release_fence);
            close(buffer.release_fence);
        }

        /**
         * Reassign release fence as the acquire fence incase of error
         */
        const_cast<camera3_stream_buffer*>(&buffer)->release_fence =
                buffer.acquire_fence;
    }

    /**
     * Unconditionally return buffer to the buffer queue.
     * - Fwk takes over the release_fence ownership
     */
    sp<Fence> releaseFence = new Fence(buffer.release_fence);
    res = mConsumer->releaseBuffer(bufferItem, releaseFence);
    if (res != OK) {
        ALOGE("%s: Stream %d: Error releasing buffer back to buffer queue:"
                " %s (%d)", __FUNCTION__, mId, strerror(-res), res);
    }

    *releaseFenceOut = releaseFence;

    return res;
}

status_t Camera3InputStream::returnInputBufferLocked(
        const camera3_stream_buffer &buffer) {
    ATRACE_CALL();

    return returnAnyBufferLocked(buffer, /*timestamp*/0, /*output*/false);
}

status_t Camera3InputStream::disconnectLocked() {

    status_t res;

    if ((res = Camera3IOStreamBase::disconnectLocked()) != OK) {
        return res;
    }

    assert(mBuffersInFlight.size() == 0);

    /**
     *  no-op since we can't disconnect the producer from the consumer-side
     */

    mState = (mState == STATE_IN_RECONFIG) ? STATE_IN_CONFIG
                                           : STATE_CONSTRUCTED;
    return OK;
}

void Camera3InputStream::dump(int fd, const Vector<String16> &args) const {
    (void) args;
    String8 lines;
    lines.appendFormat("    Stream[%d]: Input\n", mId);
    write(fd, lines.string(), lines.size());

    Camera3IOStreamBase::dump(fd, args);
}

status_t Camera3InputStream::configureQueueLocked() {
    status_t res;

    if ((res = Camera3IOStreamBase::configureQueueLocked()) != OK) {
        return res;
    }

    assert(mMaxSize == 0);
    assert(camera3_stream::format != HAL_PIXEL_FORMAT_BLOB);

    mTotalBufferCount = BufferQueue::MIN_UNDEQUEUED_BUFFERS +
                        camera3_stream::max_buffers;
    mDequeuedBufferCount = 0;
    mFrameCount = 0;

    if (mConsumer.get() == 0) {
        sp<BufferQueue> bq = new BufferQueue();
        mConsumer = new BufferItemConsumer(bq, camera3_stream::usage,
                                           mTotalBufferCount);
        mConsumer->setName(String8::format("Camera3-InputStream-%d", mId));
    }

    res = mConsumer->setDefaultBufferSize(camera3_stream::width,
                                          camera3_stream::height);
    if (res != OK) {
        ALOGE("%s: Stream %d: Could not set buffer dimensions %dx%d",
              __FUNCTION__, mId, camera3_stream::width, camera3_stream::height);
        return res;
    }
    res = mConsumer->setDefaultBufferFormat(camera3_stream::format);
    if (res != OK) {
        ALOGE("%s: Stream %d: Could not set buffer format %d",
              __FUNCTION__, mId, camera3_stream::format);
        return res;
    }

    return OK;
}

status_t Camera3InputStream::getEndpointUsage(uint32_t *usage) {
    // Per HAL3 spec, input streams have 0 for their initial usage field.
    *usage = 0;
    return OK;
}

}; // namespace camera3

}; // namespace android
