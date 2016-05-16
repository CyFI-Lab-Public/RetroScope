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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2_ZSLPROCESSORINTERFACE_H
#define ANDROID_SERVERS_CAMERA_CAMERA2_ZSLPROCESSORINTERFACE_H

#include <utils/Errors.h>
#include <utils/RefBase.h>

namespace android {
namespace camera2 {

class Parameters;

class ZslProcessorInterface : virtual public RefBase {
public:

    // Get ID for use with android.request.outputStreams / inputStreams
    virtual int getStreamId() const = 0;

    // Update the streams by recreating them if the size/format has changed
    virtual status_t updateStream(const Parameters& params) = 0;

    // Delete the underlying CameraDevice streams
    virtual status_t deleteStream() = 0;

    /**
     * Submits a ZSL capture request (id = requestId)
     *
     * An appropriate ZSL buffer is selected by the closest timestamp,
     * then we push that buffer to be reprocessed by the HAL.
     * A capture request is created and submitted on behalf of the client.
     */
    virtual status_t pushToReprocess(int32_t requestId) = 0;

    // Flush the ZSL buffer queue, freeing up all the buffers
    virtual status_t clearZslQueue() = 0;

    // (Debugging only) Dump the current state to the specified file descriptor
    virtual void dump(int fd, const Vector<String16>& args) const = 0;
};

}; //namespace camera2
}; //namespace android

#endif
