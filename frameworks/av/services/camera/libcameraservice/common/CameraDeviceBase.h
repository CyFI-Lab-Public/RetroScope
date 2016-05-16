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

#ifndef ANDROID_SERVERS_CAMERA_CAMERADEVICEBASE_H
#define ANDROID_SERVERS_CAMERA_CAMERADEVICEBASE_H

#include <utils/RefBase.h>
#include <utils/String8.h>
#include <utils/String16.h>
#include <utils/Vector.h>
#include <utils/Timers.h>

#include "hardware/camera2.h"
#include "camera/CameraMetadata.h"

namespace android {

/**
 * Base interface for version >= 2 camera device classes, which interface to
 * camera HAL device versions >= 2.
 */
class CameraDeviceBase : public virtual RefBase {
  public:
    virtual ~CameraDeviceBase();

    /**
     * The device's camera ID
     */
    virtual int      getId() const = 0;

    virtual status_t initialize(camera_module_t *module) = 0;
    virtual status_t disconnect() = 0;

    virtual status_t dump(int fd, const Vector<String16>& args) = 0;

    /**
     * The device's static characteristics metadata buffer
     */
    virtual const CameraMetadata& info() const = 0;

    /**
     * Submit request for capture. The CameraDevice takes ownership of the
     * passed-in buffer.
     */
    virtual status_t capture(CameraMetadata &request) = 0;

    /**
     * Submit request for streaming. The CameraDevice makes a copy of the
     * passed-in buffer and the caller retains ownership.
     */
    virtual status_t setStreamingRequest(const CameraMetadata &request) = 0;

    /**
     * Clear the streaming request slot.
     */
    virtual status_t clearStreamingRequest() = 0;

    /**
     * Wait until a request with the given ID has been dequeued by the
     * HAL. Returns TIMED_OUT if the timeout duration is reached. Returns
     * immediately if the latest request received by the HAL has this id.
     */
    virtual status_t waitUntilRequestReceived(int32_t requestId,
            nsecs_t timeout) = 0;

    /**
     * Create an output stream of the requested size and format.
     *
     * If format is CAMERA2_HAL_PIXEL_FORMAT_OPAQUE, then the HAL device selects
     * an appropriate format; it can be queried with getStreamInfo.
     *
     * If format is HAL_PIXEL_FORMAT_COMPRESSED, the size parameter must be
     * equal to the size in bytes of the buffers to allocate for the stream. For
     * other formats, the size parameter is ignored.
     */
    virtual status_t createStream(sp<ANativeWindow> consumer,
            uint32_t width, uint32_t height, int format, size_t size,
            int *id) = 0;

    /**
     * Create an input reprocess stream that uses buffers from an existing
     * output stream.
     */
    virtual status_t createReprocessStreamFromStream(int outputId, int *id) = 0;

    /**
     * Get information about a given stream.
     */
    virtual status_t getStreamInfo(int id,
            uint32_t *width, uint32_t *height, uint32_t *format) = 0;

    /**
     * Set stream gralloc buffer transform
     */
    virtual status_t setStreamTransform(int id, int transform) = 0;

    /**
     * Delete stream. Must not be called if there are requests in flight which
     * reference that stream.
     */
    virtual status_t deleteStream(int id) = 0;

    /**
     * Delete reprocess stream. Must not be called if there are requests in
     * flight which reference that stream.
     */
    virtual status_t deleteReprocessStream(int id) = 0;

    /**
     * Create a metadata buffer with fields that the HAL device believes are
     * best for the given use case
     */
    virtual status_t createDefaultRequest(int templateId,
            CameraMetadata *request) = 0;

    /**
     * Wait until all requests have been processed. Returns INVALID_OPERATION if
     * the streaming slot is not empty, or TIMED_OUT if the requests haven't
     * finished processing in 10 seconds.
     */
    virtual status_t waitUntilDrained() = 0;

    /**
     * Abstract class for HAL notification listeners
     */
    class NotificationListener {
      public:
        // The set of notifications is a merge of the notifications required for
        // API1 and API2.

        // Required for API 1 and 2
        virtual void notifyError(int errorCode, int arg1, int arg2) = 0;

        // Required only for API2
        virtual void notifyIdle() = 0;
        virtual void notifyShutter(int requestId,
                nsecs_t timestamp) = 0;

        // Required only for API1
        virtual void notifyAutoFocus(uint8_t newState, int triggerId) = 0;
        virtual void notifyAutoExposure(uint8_t newState, int triggerId) = 0;
        virtual void notifyAutoWhitebalance(uint8_t newState,
                int triggerId) = 0;
      protected:
        virtual ~NotificationListener();
    };

    /**
     * Connect HAL notifications to a listener. Overwrites previous
     * listener. Set to NULL to stop receiving notifications.
     */
    virtual status_t setNotifyCallback(NotificationListener *listener) = 0;

    /**
     * Whether the device supports calling notifyAutofocus, notifyAutoExposure,
     * and notifyAutoWhitebalance; if this returns false, the client must
     * synthesize these notifications from received frame metadata.
     */
    virtual bool     willNotify3A() = 0;

    /**
     * Wait for a new frame to be produced, with timeout in nanoseconds.
     * Returns TIMED_OUT when no frame produced within the specified duration
     * May be called concurrently to most methods, except for getNextFrame
     */
    virtual status_t waitForNextFrame(nsecs_t timeout) = 0;

    /**
     * Get next metadata frame from the frame queue. Returns NULL if the queue
     * is empty; caller takes ownership of the metadata buffer.
     * May be called concurrently to most methods, except for waitForNextFrame
     */
    virtual status_t getNextFrame(CameraMetadata *frame) = 0;

    /**
     * Trigger auto-focus. The latest ID used in a trigger autofocus or cancel
     * autofocus call will be returned by the HAL in all subsequent AF
     * notifications.
     */
    virtual status_t triggerAutofocus(uint32_t id) = 0;

    /**
     * Cancel auto-focus. The latest ID used in a trigger autofocus/cancel
     * autofocus call will be returned by the HAL in all subsequent AF
     * notifications.
     */
    virtual status_t triggerCancelAutofocus(uint32_t id) = 0;

    /**
     * Trigger pre-capture metering. The latest ID used in a trigger pre-capture
     * call will be returned by the HAL in all subsequent AE and AWB
     * notifications.
     */
    virtual status_t triggerPrecaptureMetering(uint32_t id) = 0;

    /**
     * Abstract interface for clients that want to listen to reprocess buffer
     * release events
     */
    struct BufferReleasedListener : public virtual RefBase {
        virtual void onBufferReleased(buffer_handle_t *handle) = 0;
    };

    /**
     * Push a buffer to be reprocessed into a reprocessing stream, and
     * provide a listener to call once the buffer is returned by the HAL
     */
    virtual status_t pushReprocessBuffer(int reprocessStreamId,
            buffer_handle_t *buffer, wp<BufferReleasedListener> listener) = 0;

    /**
     * Flush all pending and in-flight requests. Blocks until flush is
     * complete.
     */
    virtual status_t flush() = 0;

};

}; // namespace android

#endif
