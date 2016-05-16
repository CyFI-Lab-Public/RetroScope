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

#ifndef ANDROID_SERVERS_CAMERA3_ZSL_STREAM_H
#define ANDROID_SERVERS_CAMERA3_ZSL_STREAM_H

#include <utils/RefBase.h>
#include <gui/Surface.h>
#include <gui/RingBufferConsumer.h>

#include "Camera3OutputStream.h"

namespace android {

namespace camera3 {

/**
 * A class for managing a single opaque ZSL stream to/from the camera device.
 * This acts as a bidirectional stream at the HAL layer, caching and discarding
 * most output buffers, and when directed, pushes a buffer back to the HAL for
 * processing.
 */
class Camera3ZslStream :
        public Camera3OutputStream {
  public:
    /**
     * Set up a ZSL stream of a given resolution. Depth is the number of buffers
     * cached within the stream that can be retrieved for input.
     */
    Camera3ZslStream(int id, uint32_t width, uint32_t height, int depth);
    ~Camera3ZslStream();

    virtual void     dump(int fd, const Vector<String16> &args) const;

    enum { NO_BUFFER_AVAILABLE = BufferQueue::NO_BUFFER_AVAILABLE };

    /**
     * Locate a buffer matching this timestamp in the RingBufferConsumer,
     * and mark it to be queued at the next getInputBufferLocked invocation.
     *
     * Errors: Returns NO_BUFFER_AVAILABLE if we could not find a match.
     *
     */
    status_t enqueueInputBufferByTimestamp(nsecs_t timestamp,
                                           nsecs_t* actualTimestamp);

    /**
     * Clears the buffers that can be used by enqueueInputBufferByTimestamp
     */
    status_t clearInputRingBuffer();

  protected:

    /**
     * Camera3OutputStreamInterface implementation
     */
    status_t setTransform(int transform);

  private:

    int mDepth;
    // Input buffers pending to be queued into HAL
    List<sp<RingBufferConsumer::PinnedBufferItem> > mInputBufferQueue;
    sp<RingBufferConsumer>                          mProducer;

    // Input buffers in flight to HAL
    Vector<sp<RingBufferConsumer::PinnedBufferItem> > mBuffersInFlight;

    /**
     * Camera3Stream interface
     */

    // getInputBuffer/returnInputBuffer operate the input stream side of the
    // ZslStream.
    virtual status_t getInputBufferLocked(camera3_stream_buffer *buffer);
    virtual status_t returnInputBufferLocked(
            const camera3_stream_buffer &buffer);

    // Actual body to return either input or output buffers
    virtual status_t returnBufferCheckedLocked(
            const camera3_stream_buffer &buffer,
            nsecs_t timestamp,
            bool output,
            /*out*/
            sp<Fence> *releaseFenceOut);
}; // class Camera3ZslStream

}; // namespace camera3

}; // namespace android

#endif
