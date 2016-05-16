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

#ifndef ANDROID_SERVERS_CAMERA3_OUTPUT_STREAM_INTERFACE_H
#define ANDROID_SERVERS_CAMERA3_OUTPUT_STREAM_INTERFACE_H

#include "Camera3StreamInterface.h"

namespace android {

namespace camera3 {

/**
 * An interface for managing a single stream of output data from the camera
 * device.
 */
class Camera3OutputStreamInterface : public virtual Camera3StreamInterface {
  public:
    /**
     * Set the transform on the output stream; one of the
     * HAL_TRANSFORM_* / NATIVE_WINDOW_TRANSFORM_* constants.
     */
    virtual status_t setTransform(int transform) = 0;
};

} // namespace camera3

} // namespace android

#endif
