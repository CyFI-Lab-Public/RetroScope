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

#ifndef ANDROID_SERVERS_CAMERA_CAMERADEVICEFACTORY_H
#define ANDROID_SERVERS_CAMERA_CAMERADEVICEFACTORY_H

#include <utils/RefBase.h>

namespace android {

class CameraDeviceBase;
class CameraService;

/**
 * Create the right instance of Camera2Device or Camera3Device
 * automatically based on the device version.
 */
class CameraDeviceFactory : public virtual RefBase {
  public:
    static void registerService(wp<CameraService> service);

    // Prerequisite: Call registerService.
    static sp<CameraDeviceBase> createDevice(int cameraId);
  private:
    CameraDeviceFactory(wp<CameraService> service);

    static wp<CameraService> sService;
};

}; // namespace android

#endif
