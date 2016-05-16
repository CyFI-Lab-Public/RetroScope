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

#define LOG_TAG "Camera3-IOStreamBase"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

// This is needed for stdint.h to define INT64_MAX in C++
#define __STDC_LIMIT_MACROS

#include <utils/Log.h>
#include <utils/Trace.h>
#include "device3/Camera3IOStreamBase.h"
#include "device3/StatusTracker.h"

namespace android {

namespace camera3 {

Camera3IOStreamBase::Camera3IOStreamBase(int id, camera3_stream_type_t type,
        uint32_t width, uint32_t height, size_t maxSize, int format) :
        Camera3Stream(id, type,
                width, height, maxSize, format),
        mTotalBufferCount(0),
        mDequeuedBufferCount(0),
        mFrameCount(0),
        mLastTimestamp(0) {

    mCombinedFence = new Fence();

    if (maxSize > 0 && format != HAL_PIXEL_FORMAT_BLOB) {
        ALOGE("%s: Bad format for size-only stream: %d", __FUNCTION__,
                format);
        mState = STATE_ERROR;
    }
}

Camera3IOStreamBase::~Camera3IOStreamBase() {
    disconnectLocked();
}

bool Camera3IOStreamBase::hasOutstandingBuffersLocked() const {
    nsecs_t signalTime = mCombinedFence->getSignalTime();
    ALOGV("%s: Stream %d: Has %d outstanding buffers,"
            " buffer signal time is %lld",
            __FUNCTION__, mId, mDequeuedBufferCount, signalTime);
    if (mDequeuedBufferCount > 0 || signalTime == INT64_MAX) {
        return true;
    }
    return false;
}

void Camera3IOStreamBase::dump(int fd, const Vector<String16> &args) const {
    (void) args;
    String8 lines;
    lines.appendFormat("      State: %d\n", mState);
    lines.appendFormat("      Dims: %d x %d, format 0x%x\n",
            camera3_stream::width, camera3_stream::height,
            camera3_stream::format);
    lines.appendFormat("      Max size: %d\n", mMaxSize);
    lines.appendFormat("      Usage: %d, max HAL buffers: %d\n",
            camera3_stream::usage, camera3_stream::max_buffers);
    lines.appendFormat("      Frames produced: %d, last timestamp: %lld ns\n",
            mFrameCount, mLastTimestamp);
    lines.appendFormat("      Total buffers: %d, currently dequeued: %d\n",
            mTotalBufferCount, mDequeuedBufferCount);
    write(fd, lines.string(), lines.size());
}

status_t Camera3IOStreamBase::configureQueueLocked() {
    status_t res;

    switch (mState) {
        case STATE_IN_RECONFIG:
            res = disconnectLocked();
            if (res != OK) {
                return res;
            }
            break;
        case STATE_IN_CONFIG:
            // OK
            break;
        default:
            ALOGE("%s: Bad state: %d", __FUNCTION__, mState);
            return INVALID_OPERATION;
    }

    return OK;
}

size_t Camera3IOStreamBase::getBufferCountLocked() {
    return mTotalBufferCount;
}

status_t Camera3IOStreamBase::disconnectLocked() {
    switch (mState) {
        case STATE_IN_RECONFIG:
        case STATE_CONFIGURED:
            // OK
            break;
        default:
            // No connection, nothing to do
            ALOGV("%s: Stream %d: Already disconnected",
                  __FUNCTION__, mId);
            return -ENOTCONN;
    }

    if (mDequeuedBufferCount > 0) {
        ALOGE("%s: Can't disconnect with %d buffers still dequeued!",
                __FUNCTION__, mDequeuedBufferCount);
        return INVALID_OPERATION;
    }

   return OK;
}

void Camera3IOStreamBase::handoutBufferLocked(camera3_stream_buffer &buffer,
                                              buffer_handle_t *handle,
                                              int acquireFence,
                                              int releaseFence,
                                              camera3_buffer_status_t status) {
    /**
     * Note that all fences are now owned by HAL.
     */

    // Handing out a raw pointer to this object. Increment internal refcount.
    incStrong(this);
    buffer.stream = this;
    buffer.buffer = handle;
    buffer.acquire_fence = acquireFence;
    buffer.release_fence = releaseFence;
    buffer.status = status;

    // Inform tracker about becoming busy
    if (mDequeuedBufferCount == 0 && mState != STATE_IN_CONFIG &&
            mState != STATE_IN_RECONFIG) {
        sp<StatusTracker> statusTracker = mStatusTracker.promote();
        if (statusTracker != 0) {
            statusTracker->markComponentActive(mStatusId);
        }
    }
    mDequeuedBufferCount++;
}

status_t Camera3IOStreamBase::getBufferPreconditionCheckLocked() const {
    // Allow dequeue during IN_[RE]CONFIG for registration
    if (mState != STATE_CONFIGURED &&
            mState != STATE_IN_CONFIG && mState != STATE_IN_RECONFIG) {
        ALOGE("%s: Stream %d: Can't get buffers in unconfigured state %d",
                __FUNCTION__, mId, mState);
        return INVALID_OPERATION;
    }

    // Only limit dequeue amount when fully configured
    if (mState == STATE_CONFIGURED &&
            mDequeuedBufferCount == camera3_stream::max_buffers) {
        ALOGE("%s: Stream %d: Already dequeued maximum number of simultaneous"
                " buffers (%d)", __FUNCTION__, mId,
                camera3_stream::max_buffers);
        return INVALID_OPERATION;
    }

    return OK;
}

status_t Camera3IOStreamBase::returnBufferPreconditionCheckLocked() const {
    // Allow buffers to be returned in the error state, to allow for disconnect
    // and in the in-config states for registration
    if (mState == STATE_CONSTRUCTED) {
        ALOGE("%s: Stream %d: Can't return buffers in unconfigured state %d",
                __FUNCTION__, mId, mState);
        return INVALID_OPERATION;
    }
    if (mDequeuedBufferCount == 0) {
        ALOGE("%s: Stream %d: No buffers outstanding to return", __FUNCTION__,
                mId);
        return INVALID_OPERATION;
    }

    return OK;
}

status_t Camera3IOStreamBase::returnAnyBufferLocked(
        const camera3_stream_buffer &buffer,
        nsecs_t timestamp,
        bool output) {
    status_t res;

    // returnBuffer may be called from a raw pointer, not a sp<>, and we'll be
    // decrementing the internal refcount next. In case this is the last ref, we
    // might get destructed on the decStrong(), so keep an sp around until the
    // end of the call - otherwise have to sprinkle the decStrong on all exit
    // points.
    sp<Camera3IOStreamBase> keepAlive(this);
    decStrong(this);

    if ((res = returnBufferPreconditionCheckLocked()) != OK) {
        return res;
    }

    sp<Fence> releaseFence;
    res = returnBufferCheckedLocked(buffer, timestamp, output,
                                    &releaseFence);
    // Res may be an error, but we still want to decrement our owned count
    // to enable clean shutdown. So we'll just return the error but otherwise
    // carry on

    if (releaseFence != 0) {
        mCombinedFence = Fence::merge(mName, mCombinedFence, releaseFence);
    }

    mDequeuedBufferCount--;
    if (mDequeuedBufferCount == 0 && mState != STATE_IN_CONFIG &&
            mState != STATE_IN_RECONFIG) {
        ALOGV("%s: Stream %d: All buffers returned; now idle", __FUNCTION__,
                mId);
        sp<StatusTracker> statusTracker = mStatusTracker.promote();
        if (statusTracker != 0) {
            statusTracker->markComponentIdle(mStatusId, mCombinedFence);
        }
    }

    mBufferReturnedSignal.signal();

    if (output) {
        mLastTimestamp = timestamp;
    }

    return res;
}



}; // namespace camera3

}; // namespace android
