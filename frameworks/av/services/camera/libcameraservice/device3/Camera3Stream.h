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

#ifndef ANDROID_SERVERS_CAMERA3_STREAM_H
#define ANDROID_SERVERS_CAMERA3_STREAM_H

#include <gui/Surface.h>
#include <utils/RefBase.h>
#include <utils/String8.h>
#include <utils/String16.h>
#include <utils/List.h>

#include "hardware/camera3.h"

#include "Camera3StreamBufferListener.h"
#include "Camera3StreamInterface.h"

namespace android {

namespace camera3 {

/**
 * A class for managing a single stream of input or output data from the camera
 * device.
 *
 * The stream has an internal state machine to track whether it's
 * connected/configured/etc.
 *
 * States:
 *
 *  STATE_ERROR: A serious error has occurred, stream is unusable. Outstanding
 *    buffers may still be returned.
 *
 *  STATE_CONSTRUCTED: The stream is ready for configuration, but buffers cannot
 *    be gotten yet. Not connected to any endpoint, no buffers are registered
 *    with the HAL.
 *
 *  STATE_IN_CONFIG: Configuration has started, but not yet concluded. During this
 *    time, the usage, max_buffers, and priv fields of camera3_stream returned by
 *    startConfiguration() may be modified.
 *
 *  STATE_IN_RE_CONFIG: Configuration has started, and the stream has been
 *    configured before. Need to track separately from IN_CONFIG to avoid
 *    re-registering buffers with HAL.
 *
 *  STATE_CONFIGURED: Stream is configured, and has registered buffers with the
 *    HAL. The stream's getBuffer/returnBuffer work. The priv pointer may still be
 *    modified.
 *
 * Transition table:
 *
 *    <none>               => STATE_CONSTRUCTED:
 *        When constructed with valid arguments
 *    <none>               => STATE_ERROR:
 *        When constructed with invalid arguments
 *    STATE_CONSTRUCTED    => STATE_IN_CONFIG:
 *        When startConfiguration() is called
 *    STATE_IN_CONFIG      => STATE_CONFIGURED:
 *        When finishConfiguration() is called
 *    STATE_IN_CONFIG      => STATE_ERROR:
 *        When finishConfiguration() fails to allocate or register buffers.
 *    STATE_CONFIGURED     => STATE_IN_RE_CONFIG:  *
 *        When startConfiguration() is called again, after making sure stream is
 *        idle with waitUntilIdle().
 *    STATE_IN_RE_CONFIG   => STATE_CONFIGURED:
 *        When finishConfiguration() is called.
 *    STATE_IN_RE_CONFIG   => STATE_ERROR:
 *        When finishConfiguration() fails to allocate or register buffers.
 *    STATE_CONFIGURED     => STATE_CONSTRUCTED:
 *        When disconnect() is called after making sure stream is idle with
 *        waitUntilIdle().
 */
class Camera3Stream :
        protected camera3_stream,
        public virtual Camera3StreamInterface,
        public virtual RefBase {
  public:

    virtual ~Camera3Stream();

    static Camera3Stream*       cast(camera3_stream *stream);
    static const Camera3Stream* cast(const camera3_stream *stream);

    /**
     * Get the stream's ID
     */
    int              getId() const;

    /**
     * Get the stream's dimensions and format
     */
    uint32_t         getWidth() const;
    uint32_t         getHeight() const;
    int              getFormat() const;

    /**
     * Start the stream configuration process. Returns a handle to the stream's
     * information to be passed into the HAL device's configure_streams call.
     *
     * Until finishConfiguration() is called, no other methods on the stream may be
     * called. The usage and max_buffers fields of camera3_stream may be modified
     * between start/finishConfiguration, but may not be changed after that.
     * The priv field of camera3_stream may be modified at any time after
     * startConfiguration.
     *
     * Returns NULL in case of error starting configuration.
     */
    camera3_stream*  startConfiguration();

    /**
     * Check if the stream is mid-configuration (start has been called, but not
     * finish).  Used for lazy completion of configuration.
     */
    bool             isConfiguring() const;

    /**
     * Completes the stream configuration process. During this call, the stream
     * may call the device's register_stream_buffers() method. The stream
     * information structure returned by startConfiguration() may no longer be
     * modified after this call, but can still be read until the destruction of
     * the stream.
     *
     * Returns:
     *   OK on a successful configuration
     *   NO_INIT in case of a serious error from the HAL device
     *   NO_MEMORY in case of an error registering buffers
     *   INVALID_OPERATION in case connecting to the consumer failed
     */
    status_t         finishConfiguration(camera3_device *hal3Device);

    /**
     * Fill in the camera3_stream_buffer with the next valid buffer for this
     * stream, to hand over to the HAL.
     *
     * This method may only be called once finishConfiguration has been called.
     * For bidirectional streams, this method applies to the output-side
     * buffers.
     *
     */
    status_t         getBuffer(camera3_stream_buffer *buffer);

    /**
     * Return a buffer to the stream after use by the HAL.
     *
     * This method may only be called for buffers provided by getBuffer().
     * For bidirectional streams, this method applies to the output-side buffers
     */
    status_t         returnBuffer(const camera3_stream_buffer &buffer,
            nsecs_t timestamp);

    /**
     * Fill in the camera3_stream_buffer with the next valid buffer for this
     * stream, to hand over to the HAL.
     *
     * This method may only be called once finishConfiguration has been called.
     * For bidirectional streams, this method applies to the input-side
     * buffers.
     *
     */
    status_t         getInputBuffer(camera3_stream_buffer *buffer);

    /**
     * Return a buffer to the stream after use by the HAL.
     *
     * This method may only be called for buffers provided by getBuffer().
     * For bidirectional streams, this method applies to the input-side buffers
     */
    status_t         returnInputBuffer(const camera3_stream_buffer &buffer);

    /**
     * Whether any of the stream's buffers are currently in use by the HAL,
     * including buffers that have been returned but not yet had their
     * release fence signaled.
     */
    bool             hasOutstandingBuffers() const;

    enum {
        TIMEOUT_NEVER = -1
    };

    /**
     * Set the status tracker to notify about idle transitions
     */
    virtual status_t setStatusTracker(sp<StatusTracker> statusTracker);

    /**
     * Disconnect stream from its non-HAL endpoint. After this,
     * start/finishConfiguration must be called before the stream can be used
     * again. This cannot be called if the stream has outstanding dequeued
     * buffers.
     */
    status_t         disconnect();

    /**
     * Debug dump of the stream's state.
     */
    virtual void     dump(int fd, const Vector<String16> &args) const = 0;

    void             addBufferListener(
            wp<Camera3StreamBufferListener> listener);
    void             removeBufferListener(
            const sp<Camera3StreamBufferListener>& listener);

  protected:
    const int mId;
    const String8 mName;
    // Zero for formats with fixed buffer size for given dimensions.
    const size_t mMaxSize;

    enum {
        STATE_ERROR,
        STATE_CONSTRUCTED,
        STATE_IN_CONFIG,
        STATE_IN_RECONFIG,
        STATE_CONFIGURED
    } mState;

    mutable Mutex mLock;

    Camera3Stream(int id, camera3_stream_type type,
            uint32_t width, uint32_t height, size_t maxSize, int format);

    /**
     * Interface to be implemented by derived classes
     */

    // getBuffer / returnBuffer implementations

    // Since camera3_stream_buffer includes a raw pointer to the stream,
    // cast to camera3_stream*, implementations must increment the
    // refcount of the stream manually in getBufferLocked, and decrement it in
    // returnBufferLocked.
    virtual status_t getBufferLocked(camera3_stream_buffer *buffer);
    virtual status_t returnBufferLocked(const camera3_stream_buffer &buffer,
            nsecs_t timestamp);
    virtual status_t getInputBufferLocked(camera3_stream_buffer *buffer);
    virtual status_t returnInputBufferLocked(
            const camera3_stream_buffer &buffer);
    virtual bool     hasOutstandingBuffersLocked() const = 0;
    // Can return -ENOTCONN when we are already disconnected (not an error)
    virtual status_t disconnectLocked() = 0;

    // Configure the buffer queue interface to the other end of the stream,
    // after the HAL has provided usage and max_buffers values. After this call,
    // the stream must be ready to produce all buffers for registration with
    // HAL.
    virtual status_t configureQueueLocked() = 0;

    // Get the total number of buffers in the queue
    virtual size_t   getBufferCountLocked() = 0;

    // Get the usage flags for the other endpoint, or return
    // INVALID_OPERATION if they cannot be obtained.
    virtual status_t getEndpointUsage(uint32_t *usage) = 0;

    // Tracking for idle state
    wp<StatusTracker> mStatusTracker;
    // Status tracker component ID
    int mStatusId;

  private:
    uint32_t oldUsage;
    uint32_t oldMaxBuffers;

    // Gets all buffers from endpoint and registers them with the HAL.
    status_t registerBuffersLocked(camera3_device *hal3Device);

    void fireBufferListenersLocked(const camera3_stream_buffer& buffer,
                                  bool acquired, bool output);
    List<wp<Camera3StreamBufferListener> > mBufferListenerList;

}; // class Camera3Stream

}; // namespace camera3

}; // namespace android

#endif
