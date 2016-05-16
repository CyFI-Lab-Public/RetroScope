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

#define LOG_TAG "Camera3-Stream"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>
#include "device3/Camera3Stream.h"
#include "device3/StatusTracker.h"

namespace android {

namespace camera3 {

Camera3Stream::~Camera3Stream() {
    sp<StatusTracker> statusTracker = mStatusTracker.promote();
    if (statusTracker != 0 && mStatusId != StatusTracker::NO_STATUS_ID) {
        statusTracker->removeComponent(mStatusId);
    }
}

Camera3Stream* Camera3Stream::cast(camera3_stream *stream) {
    return static_cast<Camera3Stream*>(stream);
}

const Camera3Stream* Camera3Stream::cast(const camera3_stream *stream) {
    return static_cast<const Camera3Stream*>(stream);
}

Camera3Stream::Camera3Stream(int id,
        camera3_stream_type type,
        uint32_t width, uint32_t height, size_t maxSize, int format) :
    camera3_stream(),
    mId(id),
    mName(String8::format("Camera3Stream[%d]", id)),
    mMaxSize(maxSize),
    mState(STATE_CONSTRUCTED),
    mStatusId(StatusTracker::NO_STATUS_ID) {

    camera3_stream::stream_type = type;
    camera3_stream::width = width;
    camera3_stream::height = height;
    camera3_stream::format = format;
    camera3_stream::usage = 0;
    camera3_stream::max_buffers = 0;
    camera3_stream::priv = NULL;

    if (format == HAL_PIXEL_FORMAT_BLOB && maxSize == 0) {
        ALOGE("%s: BLOB format with size == 0", __FUNCTION__);
        mState = STATE_ERROR;
    }
}

int Camera3Stream::getId() const {
    return mId;
}

uint32_t Camera3Stream::getWidth() const {
    return camera3_stream::width;
}

uint32_t Camera3Stream::getHeight() const {
    return camera3_stream::height;
}

int Camera3Stream::getFormat() const {
    return camera3_stream::format;
}

camera3_stream* Camera3Stream::startConfiguration() {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);
    status_t res;

    switch (mState) {
        case STATE_ERROR:
            ALOGE("%s: In error state", __FUNCTION__);
            return NULL;
        case STATE_CONSTRUCTED:
            // OK
            break;
        case STATE_IN_CONFIG:
        case STATE_IN_RECONFIG:
            // Can start config again with no trouble; but don't redo
            // oldUsage/oldMaxBuffers
            return this;
        case STATE_CONFIGURED:
            if (stream_type == CAMERA3_STREAM_INPUT) {
                ALOGE("%s: Cannot configure an input stream twice",
                        __FUNCTION__);
                return NULL;
            } else if (hasOutstandingBuffersLocked()) {
                ALOGE("%s: Cannot configure stream; has outstanding buffers",
                        __FUNCTION__);
                return NULL;
            }
            break;
        default:
            ALOGE("%s: Unknown state %d", __FUNCTION__, mState);
            return NULL;
    }

    oldUsage = camera3_stream::usage;
    oldMaxBuffers = camera3_stream::max_buffers;

    res = getEndpointUsage(&(camera3_stream::usage));
    if (res != OK) {
        ALOGE("%s: Cannot query consumer endpoint usage!",
                __FUNCTION__);
        return NULL;
    }

    // Stop tracking if currently doing so
    if (mStatusId != StatusTracker::NO_STATUS_ID) {
        sp<StatusTracker> statusTracker = mStatusTracker.promote();
        if (statusTracker != 0) {
            statusTracker->removeComponent(mStatusId);
        }
        mStatusId = StatusTracker::NO_STATUS_ID;
    }

    if (mState == STATE_CONSTRUCTED) {
        mState = STATE_IN_CONFIG;
    } else { // mState == STATE_CONFIGURED
        mState = STATE_IN_RECONFIG;
    }

    return this;
}

bool Camera3Stream::isConfiguring() const {
    Mutex::Autolock l(mLock);
    return (mState == STATE_IN_CONFIG) || (mState == STATE_IN_RECONFIG);
}

status_t Camera3Stream::finishConfiguration(camera3_device *hal3Device) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);
    switch (mState) {
        case STATE_ERROR:
            ALOGE("%s: In error state", __FUNCTION__);
            return INVALID_OPERATION;
        case STATE_IN_CONFIG:
        case STATE_IN_RECONFIG:
            // OK
            break;
        case STATE_CONSTRUCTED:
        case STATE_CONFIGURED:
            ALOGE("%s: Cannot finish configuration that hasn't been started",
                    __FUNCTION__);
            return INVALID_OPERATION;
        default:
            ALOGE("%s: Unknown state", __FUNCTION__);
            return INVALID_OPERATION;
    }

    // Register for idle tracking
    sp<StatusTracker> statusTracker = mStatusTracker.promote();
    if (statusTracker != 0) {
        mStatusId = statusTracker->addComponent();
    }

    // Check if the stream configuration is unchanged, and skip reallocation if
    // so. As documented in hardware/camera3.h:configure_streams().
    if (mState == STATE_IN_RECONFIG &&
            oldUsage == camera3_stream::usage &&
            oldMaxBuffers == camera3_stream::max_buffers) {
        mState = STATE_CONFIGURED;
        return OK;
    }

    status_t res;
    res = configureQueueLocked();
    if (res != OK) {
        ALOGE("%s: Unable to configure stream %d queue: %s (%d)",
                __FUNCTION__, mId, strerror(-res), res);
        mState = STATE_ERROR;
        return res;
    }

    res = registerBuffersLocked(hal3Device);
    if (res != OK) {
        ALOGE("%s: Unable to register stream buffers with HAL: %s (%d)",
                __FUNCTION__, strerror(-res), res);
        mState = STATE_ERROR;
        return res;
    }

    mState = STATE_CONFIGURED;

    return res;
}

status_t Camera3Stream::getBuffer(camera3_stream_buffer *buffer) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);

    status_t res = getBufferLocked(buffer);
    if (res == OK) {
        fireBufferListenersLocked(*buffer, /*acquired*/true, /*output*/true);
    }

    return res;
}

status_t Camera3Stream::returnBuffer(const camera3_stream_buffer &buffer,
        nsecs_t timestamp) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);

    status_t res = returnBufferLocked(buffer, timestamp);
    if (res == OK) {
        fireBufferListenersLocked(buffer, /*acquired*/false, /*output*/true);
    }

    return res;
}

status_t Camera3Stream::getInputBuffer(camera3_stream_buffer *buffer) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);

    status_t res = getInputBufferLocked(buffer);
    if (res == OK) {
        fireBufferListenersLocked(*buffer, /*acquired*/true, /*output*/false);
    }

    return res;
}

status_t Camera3Stream::returnInputBuffer(const camera3_stream_buffer &buffer) {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);

    status_t res = returnInputBufferLocked(buffer);
    if (res == OK) {
        fireBufferListenersLocked(buffer, /*acquired*/false, /*output*/false);
    }
    return res;
}

void Camera3Stream::fireBufferListenersLocked(
        const camera3_stream_buffer& /*buffer*/, bool acquired, bool output) {
    List<wp<Camera3StreamBufferListener> >::iterator it, end;

    // TODO: finish implementing

    Camera3StreamBufferListener::BufferInfo info =
        Camera3StreamBufferListener::BufferInfo();
    info.mOutput = output;
    // TODO: rest of fields

    for (it = mBufferListenerList.begin(), end = mBufferListenerList.end();
         it != end;
         ++it) {

        sp<Camera3StreamBufferListener> listener = it->promote();
        if (listener != 0) {
            if (acquired) {
                listener->onBufferAcquired(info);
            } else {
                listener->onBufferReleased(info);
            }
        }
    }
}

bool Camera3Stream::hasOutstandingBuffers() const {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);
    return hasOutstandingBuffersLocked();
}

status_t Camera3Stream::setStatusTracker(sp<StatusTracker> statusTracker) {
    Mutex::Autolock l(mLock);
    sp<StatusTracker> oldTracker = mStatusTracker.promote();
    if (oldTracker != 0 && mStatusId != StatusTracker::NO_STATUS_ID) {
        oldTracker->removeComponent(mStatusId);
    }
    mStatusId = StatusTracker::NO_STATUS_ID;
    mStatusTracker = statusTracker;

    return OK;
}

status_t Camera3Stream::disconnect() {
    ATRACE_CALL();
    Mutex::Autolock l(mLock);
    ALOGV("%s: Stream %d: Disconnecting...", __FUNCTION__, mId);
    status_t res = disconnectLocked();

    if (res == -ENOTCONN) {
        // "Already disconnected" -- not an error
        return OK;
    } else {
        return res;
    }
}

status_t Camera3Stream::registerBuffersLocked(camera3_device *hal3Device) {
    ATRACE_CALL();
    status_t res;

    size_t bufferCount = getBufferCountLocked();

    Vector<buffer_handle_t*> buffers;
    buffers.insertAt(NULL, 0, bufferCount);

    camera3_stream_buffer_set bufferSet = camera3_stream_buffer_set();
    bufferSet.stream = this;
    bufferSet.num_buffers = bufferCount;
    bufferSet.buffers = buffers.editArray();

    Vector<camera3_stream_buffer_t> streamBuffers;
    streamBuffers.insertAt(camera3_stream_buffer_t(), 0, bufferCount);

    // Register all buffers with the HAL. This means getting all the buffers
    // from the stream, providing them to the HAL with the
    // register_stream_buffers() method, and then returning them back to the
    // stream in the error state, since they won't have valid data.
    //
    // Only registered buffers can be sent to the HAL.

    uint32_t bufferIdx = 0;
    for (; bufferIdx < bufferCount; bufferIdx++) {
        res = getBufferLocked( &streamBuffers.editItemAt(bufferIdx) );
        if (res != OK) {
            ALOGE("%s: Unable to get buffer %d for registration with HAL",
                    __FUNCTION__, bufferIdx);
            // Skip registering, go straight to cleanup
            break;
        }

        sp<Fence> fence = new Fence(streamBuffers[bufferIdx].acquire_fence);
        fence->waitForever("Camera3Stream::registerBuffers");

        buffers.editItemAt(bufferIdx) = streamBuffers[bufferIdx].buffer;
    }
    if (bufferIdx == bufferCount) {
        // Got all buffers, register with HAL
        ALOGV("%s: Registering %d buffers with camera HAL",
                __FUNCTION__, bufferCount);
        ATRACE_BEGIN("camera3->register_stream_buffers");
        res = hal3Device->ops->register_stream_buffers(hal3Device,
                &bufferSet);
        ATRACE_END();
    }

    // Return all valid buffers to stream, in ERROR state to indicate
    // they weren't filled.
    for (size_t i = 0; i < bufferIdx; i++) {
        streamBuffers.editItemAt(i).release_fence = -1;
        streamBuffers.editItemAt(i).status = CAMERA3_BUFFER_STATUS_ERROR;
        returnBufferLocked(streamBuffers[i], 0);
    }

    return res;
}

status_t Camera3Stream::getBufferLocked(camera3_stream_buffer *) {
    ALOGE("%s: This type of stream does not support output", __FUNCTION__);
    return INVALID_OPERATION;
}
status_t Camera3Stream::returnBufferLocked(const camera3_stream_buffer &,
                                           nsecs_t) {
    ALOGE("%s: This type of stream does not support output", __FUNCTION__);
    return INVALID_OPERATION;
}
status_t Camera3Stream::getInputBufferLocked(camera3_stream_buffer *) {
    ALOGE("%s: This type of stream does not support input", __FUNCTION__);
    return INVALID_OPERATION;
}
status_t Camera3Stream::returnInputBufferLocked(
        const camera3_stream_buffer &) {
    ALOGE("%s: This type of stream does not support input", __FUNCTION__);
    return INVALID_OPERATION;
}

void Camera3Stream::addBufferListener(
        wp<Camera3StreamBufferListener> listener) {
    Mutex::Autolock l(mLock);
    mBufferListenerList.push_back(listener);
}

void Camera3Stream::removeBufferListener(
        const sp<Camera3StreamBufferListener>& listener) {
    Mutex::Autolock l(mLock);

    bool erased = true;
    List<wp<Camera3StreamBufferListener> >::iterator it, end;
    for (it = mBufferListenerList.begin(), end = mBufferListenerList.end();
         it != end;
         ) {

        if (*it == listener) {
            it = mBufferListenerList.erase(it);
            erased = true;
        } else {
            ++it;
        }
    }

    if (!erased) {
        ALOGW("%s: Could not find listener to remove, already removed",
              __FUNCTION__);
    }
}

}; // namespace camera3

}; // namespace android
