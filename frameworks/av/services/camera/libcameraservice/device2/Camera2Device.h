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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2DEVICE_H
#define ANDROID_SERVERS_CAMERA_CAMERA2DEVICE_H

#include <utils/Condition.h>
#include <utils/Errors.h>
#include <utils/List.h>
#include <utils/Mutex.h>

#include "common/CameraDeviceBase.h"

namespace android {

/**
 * CameraDevice for HAL devices with version CAMERA_DEVICE_API_VERSION_2_0
 *
 * TODO for camera2 API implementation:
 * Does not produce notifyShutter / notifyIdle callbacks to NotificationListener
 * Use waitUntilDrained for idle.
 */
class Camera2Device: public CameraDeviceBase {
  public:
    Camera2Device(int id);

    virtual ~Camera2Device();

    /**
     * CameraDevice interface
     */
    virtual int      getId() const;
    virtual status_t initialize(camera_module_t *module);
    virtual status_t disconnect();
    virtual status_t dump(int fd, const Vector<String16>& args);
    virtual const CameraMetadata& info() const;
    virtual status_t capture(CameraMetadata &request);
    virtual status_t setStreamingRequest(const CameraMetadata &request);
    virtual status_t clearStreamingRequest();
    virtual status_t waitUntilRequestReceived(int32_t requestId, nsecs_t timeout);
    virtual status_t createStream(sp<ANativeWindow> consumer,
            uint32_t width, uint32_t height, int format, size_t size,
            int *id);
    virtual status_t createReprocessStreamFromStream(int outputId, int *id);
    virtual status_t getStreamInfo(int id,
            uint32_t *width, uint32_t *height, uint32_t *format);
    virtual status_t setStreamTransform(int id, int transform);
    virtual status_t deleteStream(int id);
    virtual status_t deleteReprocessStream(int id);
    virtual status_t createDefaultRequest(int templateId, CameraMetadata *request);
    virtual status_t waitUntilDrained();
    virtual status_t setNotifyCallback(NotificationListener *listener);
    virtual bool     willNotify3A();
    virtual status_t waitForNextFrame(nsecs_t timeout);
    virtual status_t getNextFrame(CameraMetadata *frame);
    virtual status_t triggerAutofocus(uint32_t id);
    virtual status_t triggerCancelAutofocus(uint32_t id);
    virtual status_t triggerPrecaptureMetering(uint32_t id);
    virtual status_t pushReprocessBuffer(int reprocessStreamId,
            buffer_handle_t *buffer, wp<BufferReleasedListener> listener);
    // Flush implemented as just a wait
    virtual status_t flush();
  private:
    const int mId;
    camera2_device_t *mHal2Device;

    CameraMetadata mDeviceInfo;
    vendor_tag_query_ops_t *mVendorTagOps;

    /**
     * Queue class for both sending requests to a camera2 device, and for
     * receiving frames from a camera2 device.
     */
    class MetadataQueue: public camera2_request_queue_src_ops_t,
                         public camera2_frame_queue_dst_ops_t {
      public:
        MetadataQueue();
        ~MetadataQueue();

        // Interface to camera2 HAL device, either for requests (device is
        // consumer) or for frames (device is producer)
        const camera2_request_queue_src_ops_t*   getToConsumerInterface();
        void setFromConsumerInterface(camera2_device_t *d);

        // Connect queue consumer endpoint to a camera2 device
        status_t setConsumerDevice(camera2_device_t *d);
        // Connect queue producer endpoint to a camera2 device
        status_t setProducerDevice(camera2_device_t *d);

        const camera2_frame_queue_dst_ops_t* getToProducerInterface();

        // Real interfaces. On enqueue, queue takes ownership of buffer pointer
        // On dequeue, user takes ownership of buffer pointer.
        status_t enqueue(camera_metadata_t *buf);
        status_t dequeue(camera_metadata_t **buf, bool incrementCount = false);
        int      getBufferCount();
        status_t waitForBuffer(nsecs_t timeout);
        // Wait until a buffer with the given ID is dequeued. Will return
        // immediately if the latest buffer dequeued has that ID.
        status_t waitForDequeue(int32_t id, nsecs_t timeout);

        // Set repeating buffer(s); if the queue is empty on a dequeue call, the
        // queue copies the contents of the stream slot into the queue, and then
        // dequeues the first new entry. The metadata buffers passed in are
        // copied.
        status_t setStreamSlot(camera_metadata_t *buf);
        status_t setStreamSlot(const List<camera_metadata_t*> &bufs);

        // Clear the request queue and the streaming slot
        status_t clear();

        status_t dump(int fd, const Vector<String16>& args);

      private:
        status_t signalConsumerLocked();
        status_t freeBuffers(List<camera_metadata_t*>::iterator start,
                List<camera_metadata_t*>::iterator end);

        camera2_device_t *mHal2Device;

        Mutex mMutex;
        Condition notEmpty;

        int mFrameCount;
        int32_t mLatestRequestId;
        Condition mNewRequestId;

        int mCount;
        List<camera_metadata_t*> mEntries;
        int mStreamSlotCount;
        List<camera_metadata_t*> mStreamSlot;

        bool mSignalConsumer;

        static MetadataQueue* getInstance(
            const camera2_frame_queue_dst_ops_t *q);
        static MetadataQueue* getInstance(
            const camera2_request_queue_src_ops_t *q);

        static int consumer_buffer_count(
            const camera2_request_queue_src_ops_t *q);

        static int consumer_dequeue(const camera2_request_queue_src_ops_t *q,
            camera_metadata_t **buffer);

        static int consumer_free(const camera2_request_queue_src_ops_t *q,
                camera_metadata_t *old_buffer);

        static int producer_dequeue(const camera2_frame_queue_dst_ops_t *q,
                size_t entries, size_t bytes,
                camera_metadata_t **buffer);

        static int producer_cancel(const camera2_frame_queue_dst_ops_t *q,
            camera_metadata_t *old_buffer);

        static int producer_enqueue(const camera2_frame_queue_dst_ops_t *q,
                camera_metadata_t *filled_buffer);

    }; // class MetadataQueue

    MetadataQueue mRequestQueue;
    MetadataQueue mFrameQueue;

    /**
     * Adapter from an ANativeWindow interface to camera2 device stream ops.
     * Also takes care of allocating/deallocating stream in device interface
     */
    class StreamAdapter: public camera2_stream_ops, public virtual RefBase {
      public:
        StreamAdapter(camera2_device_t *d);

        ~StreamAdapter();

        /**
         * Create a HAL device stream of the requested size and format.
         *
         * If format is CAMERA2_HAL_PIXEL_FORMAT_OPAQUE, then the HAL device
         * selects an appropriate format; it can be queried with getFormat.
         *
         * If format is HAL_PIXEL_FORMAT_COMPRESSED, the size parameter must
         * be equal to the size in bytes of the buffers to allocate for the
         * stream. For other formats, the size parameter is ignored.
         */
        status_t connectToDevice(sp<ANativeWindow> consumer,
                uint32_t width, uint32_t height, int format, size_t size);

        status_t release();

        status_t setTransform(int transform);

        // Get stream parameters.
        // Only valid after a successful connectToDevice call.
        int      getId() const     { return mId; }
        uint32_t getWidth() const  { return mWidth; }
        uint32_t getHeight() const { return mHeight; }
        uint32_t getFormat() const { return mFormat; }

        // Dump stream information
        status_t dump(int fd, const Vector<String16>& args);

      private:
        enum {
            ERROR = -1,
            RELEASED = 0,
            ALLOCATED,
            CONNECTED,
            ACTIVE
        } mState;

        sp<ANativeWindow> mConsumerInterface;
        camera2_device_t *mHal2Device;

        uint32_t mId;
        uint32_t mWidth;
        uint32_t mHeight;
        uint32_t mFormat;
        size_t   mSize;
        uint32_t mUsage;
        uint32_t mMaxProducerBuffers;
        uint32_t mMaxConsumerBuffers;
        uint32_t mTotalBuffers;
        int mFormatRequested;

        /** Debugging information */
        uint32_t mActiveBuffers;
        uint32_t mFrameCount;
        int64_t  mLastTimestamp;

        const camera2_stream_ops *getStreamOps();

        static ANativeWindow* toANW(const camera2_stream_ops_t *w);

        static int dequeue_buffer(const camera2_stream_ops_t *w,
                buffer_handle_t** buffer);

        static int enqueue_buffer(const camera2_stream_ops_t* w,
                int64_t timestamp,
                buffer_handle_t* buffer);

        static int cancel_buffer(const camera2_stream_ops_t* w,
                buffer_handle_t* buffer);

        static int set_crop(const camera2_stream_ops_t* w,
                int left, int top, int right, int bottom);
    }; // class StreamAdapter

    typedef List<sp<StreamAdapter> > StreamList;
    StreamList mStreams;

    /**
     * Adapter from an ANativeWindow interface to camera2 device stream ops.
     * Also takes care of allocating/deallocating stream in device interface
     */
    class ReprocessStreamAdapter: public camera2_stream_in_ops, public virtual RefBase {
      public:
        ReprocessStreamAdapter(camera2_device_t *d);

        ~ReprocessStreamAdapter();

        /**
         * Create a HAL device reprocess stream based on an existing output stream.
         */
        status_t connectToDevice(const sp<StreamAdapter> &outputStream);

        status_t release();

        /**
         * Push buffer into stream for reprocessing. Takes ownership until it notifies
         * that the buffer has been released
         */
        status_t pushIntoStream(buffer_handle_t *handle,
                const wp<BufferReleasedListener> &releaseListener);

        /**
         * Get stream parameters.
         * Only valid after a successful connectToDevice call.
         */
        int      getId() const     { return mId; }
        uint32_t getWidth() const  { return mWidth; }
        uint32_t getHeight() const { return mHeight; }
        uint32_t getFormat() const { return mFormat; }

        // Dump stream information
        status_t dump(int fd, const Vector<String16>& args);

      private:
        enum {
            ERROR = -1,
            RELEASED = 0,
            ACTIVE
        } mState;

        sp<ANativeWindow> mConsumerInterface;
        wp<StreamAdapter> mBaseStream;

        struct QueueEntry {
            buffer_handle_t *handle;
            wp<BufferReleasedListener> releaseListener;
        };

        List<QueueEntry> mQueue;

        List<QueueEntry> mInFlightQueue;

        camera2_device_t *mHal2Device;

        uint32_t mId;
        uint32_t mWidth;
        uint32_t mHeight;
        uint32_t mFormat;

        /** Debugging information */
        uint32_t mActiveBuffers;
        uint32_t mFrameCount;
        int64_t  mLastTimestamp;

        const camera2_stream_in_ops *getStreamOps();

        static int acquire_buffer(const camera2_stream_in_ops_t *w,
                buffer_handle_t** buffer);

        static int release_buffer(const camera2_stream_in_ops_t* w,
                buffer_handle_t* buffer);

    }; // class ReprocessStreamAdapter

    typedef List<sp<ReprocessStreamAdapter> > ReprocessStreamList;
    ReprocessStreamList mReprocessStreams;

    // Receives HAL notifications and routes them to the NotificationListener
    static void notificationCallback(int32_t msg_type,
            int32_t ext1,
            int32_t ext2,
            int32_t ext3,
            void *user);

}; // class Camera2Device

}; // namespace android

#endif
