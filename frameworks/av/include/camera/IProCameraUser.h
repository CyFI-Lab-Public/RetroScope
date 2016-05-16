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

#ifndef ANDROID_HARDWARE_IPROCAMERAUSER_H
#define ANDROID_HARDWARE_IPROCAMERAUSER_H

#include <utils/RefBase.h>
#include <binder/IInterface.h>
#include <binder/Parcel.h>
#include <binder/IMemory.h>
#include <utils/String8.h>
#include <camera/IProCameraCallbacks.h>

struct camera_metadata;

namespace android {

class IProCameraUserClient;
class IGraphicBufferProducer;
class Surface;

class IProCameraUser: public IInterface
{
    /**
     * Keep up-to-date with IProCameraUser.aidl in frameworks/base
     */
public:
    DECLARE_META_INTERFACE(ProCameraUser);

    virtual void            disconnect() = 0;

    // connect to the service, given a callbacks listener
    virtual status_t        connect(const sp<IProCameraCallbacks>& callbacks)
                                                                            = 0;

    /**
     * Locking
     **/
    virtual status_t        exclusiveTryLock() = 0;
    virtual status_t        exclusiveLock() = 0;
    virtual status_t        exclusiveUnlock() = 0;

    virtual bool            hasExclusiveLock() = 0;

    /**
     * Request Handling
     **/

    // Note that the callee gets a copy of the metadata.
    virtual int             submitRequest(struct camera_metadata* metadata,
                                          bool streaming = false) = 0;
    virtual status_t        cancelRequest(int requestId) = 0;

    virtual status_t        deleteStream(int streamId) = 0;
    virtual status_t        createStream(
                                      int width, int height, int format,
                                      const sp<IGraphicBufferProducer>& bufferProducer,
                                      /*out*/
                                      int* streamId) = 0;

    // Create a request object from a template.
    virtual status_t        createDefaultRequest(int templateId,
                                                 /*out*/
                                                 camera_metadata** request)
                                                                           = 0;

    // Get static camera metadata
    virtual status_t        getCameraInfo(int cameraId,
                                          /*out*/
                                          camera_metadata** info) = 0;

};

// ----------------------------------------------------------------------------

class BnProCameraUser: public BnInterface<IProCameraUser>
{
public:
    virtual status_t    onTransact( uint32_t code,
                                    const Parcel& data,
                                    Parcel* reply,
                                    uint32_t flags = 0);
};

}; // namespace android

#endif
