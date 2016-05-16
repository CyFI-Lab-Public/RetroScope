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

// #define LOG_NDEBUG 0
#define LOG_TAG "CameraDeviceFactory"
#include <utils/Log.h>

#include "CameraService.h"
#include "CameraDeviceFactory.h"
#include "common/CameraDeviceBase.h"
#include "device2/Camera2Device.h"
#include "device3/Camera3Device.h"

namespace android {

wp<CameraService> CameraDeviceFactory::sService;

sp<CameraDeviceBase> CameraDeviceFactory::createDevice(int cameraId) {

    sp<CameraService> svc = sService.promote();
    if (svc == 0) {
        ALOGE("%s: No service registered", __FUNCTION__);
        return NULL;
    }

    int deviceVersion = svc->getDeviceVersion(cameraId, /*facing*/NULL);

    sp<CameraDeviceBase> device;

    switch (deviceVersion) {
        case CAMERA_DEVICE_API_VERSION_2_0:
        case CAMERA_DEVICE_API_VERSION_2_1:
            device = new Camera2Device(cameraId);
            break;
        case CAMERA_DEVICE_API_VERSION_3_0:
            device = new Camera3Device(cameraId);
            break;
        default:
            ALOGE("%s: Camera %d: Unknown HAL device version %d",
                  __FUNCTION__, cameraId, deviceVersion);
            device = NULL;
            break;
    }

    ALOGV_IF(device != 0, "Created a new camera device for version %d",
                          deviceVersion);

    return device;
}

void CameraDeviceFactory::registerService(wp<CameraService> service) {
    ALOGV("%s: Registered service %p", __FUNCTION__,
          service.promote().get());

    sService = service;
}

}; // namespace android
