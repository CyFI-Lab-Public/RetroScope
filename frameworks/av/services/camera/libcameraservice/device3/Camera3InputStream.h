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

#ifndef ANDROID_SERVERS_CAMERA3_INPUT_STREAM_H
#define ANDROID_SERVERS_CAMERA3_INPUT_STREAM_H

#include <utils/RefBase.h>
#include <gui/Surface.h>
#include <gui/BufferItemConsumer.h>

#include "Camera3IOStreamBase.h"

namespace android {

namespace camera3 {

/**
 * A class for managing a single stream of input data to the camera device.
 *
 * This class serves as a consumer adapter for the HAL, and will consume the
 * buffers by feeding them into the HAL, as well as releasing the buffers back
 * the buffers once the HAL is done with them.
 */
class Camera3InputStream : public Camera3IOStreamBase {
  public:
    /**
     * Set up a stream for formats that have fixed size, such as RAW and YUV.
     */
    Camera3InputStream(int id, uint32_t width, uint32_t height, int format);
    ~Camera3InputStream();

    virtual void     dump(int fd, const Vector<String16> &args) const;

  private:

    typedef BufferItemConsumer::BufferItem BufferItem;

    sp<BufferItemConsumer> mConsumer;
    Vector<BufferItem> mBuffersInFlight;

    /**
     * Camera3IOStreamBase
     */
    virtual status_t returnBufferCheckedLocked(
            const camera3_stream_buffer &buffer,
            nsecs_t timestamp,
            bool output,
            /*out*/
            sp<Fence> *releaseFenceOut);

    /**
     * Camera3Stream interface
     */

    virtual status_t getInputBufferLocked(camera3_stream_buffer *buffer);
    virtual status_t returnInputBufferLocked(
            const camera3_stream_buffer &buffer);
    virtual status_t disconnectLocked();

    virtual status_t configureQueueLocked();

    virtual status_t getEndpointUsage(uint32_t *usage);

}; // class Camera3InputStream

}; // namespace camera3

}; // namespace android

#endif
