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

#ifndef ANDROID_HARDWARE_PHOTOGRAPHY_ICAMERADEVICEUSER_H
#define ANDROID_HARDWARE_PHOTOGRAPHY_ICAMERADEVICEUSER_H

#include <binder/IInterface.h>
#include <binder/Parcel.h>

struct camera_metadata;

namespace android {

class ICameraDeviceUserClient;
class IGraphicBufferProducer;
class Surface;
class CaptureRequest;
class CameraMetadata;

class ICameraDeviceUser : public IInterface
{
    /**
     * Keep up-to-date with ICameraDeviceUser.aidl in frameworks/base
     */
public:
    DECLARE_META_INTERFACE(CameraDeviceUser);

    virtual void            disconnect() = 0;

    /**
     * Request Handling
     **/

    virtual int             submitRequest(sp<CaptureRequest> request,
                                          bool streaming = false) = 0;
    virtual status_t        cancelRequest(int requestId) = 0;

    virtual status_t        deleteStream(int streamId) = 0;
    virtual status_t        createStream(
            int width, int height, int format,
            const sp<IGraphicBufferProducer>& bufferProducer) = 0;

    // Create a request object from a template.
    virtual status_t        createDefaultRequest(int templateId,
                                                 /*out*/
                                                 CameraMetadata* request) = 0;
    // Get static camera metadata
    virtual status_t        getCameraInfo(/*out*/
                                          CameraMetadata* info) = 0;

    // Wait until all the submitted requests have finished processing
    virtual status_t        waitUntilIdle() =  0;

    // Flush all pending and in-progress work as quickly as possible.
    virtual status_t        flush() = 0;
};

// ----------------------------------------------------------------------------

class BnCameraDeviceUser: public BnInterface<ICameraDeviceUser>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
