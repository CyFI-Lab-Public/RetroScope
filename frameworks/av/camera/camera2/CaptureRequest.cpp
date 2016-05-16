/*
**
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

// #define LOG_NDEBUG 0
#define LOG_TAG "CameraRequest"
#include <utils/Log.h>

#include <camera/camera2/CaptureRequest.h>

#include <binder/Parcel.h>
#include <gui/Surface.h>

namespace android {

status_t CaptureRequest::readFromParcel(Parcel* parcel) {
    if (parcel == NULL) {
        ALOGE("%s: Null parcel", __FUNCTION__);
        return BAD_VALUE;
    }

    mMetadata.clear();
    mSurfaceList.clear();

    status_t err;

    if ((err = mMetadata.readFromParcel(parcel)) != OK) {
        ALOGE("%s: Failed to read metadata from parcel", __FUNCTION__);
        return err;
    }
    ALOGV("%s: Read metadata from parcel", __FUNCTION__);

    int32_t size;
    if ((err = parcel->readInt32(&size)) != OK) {
        ALOGE("%s: Failed to read surface list size from parcel", __FUNCTION__);
        return err;
    }
    ALOGV("%s: Read surface list size = %d", __FUNCTION__, size);

    // Do not distinguish null arrays from 0-sized arrays.
    for (int i = 0; i < size; ++i) {
        // Parcel.writeParcelableArray
        size_t len;
        const char16_t* className = parcel->readString16Inplace(&len);
        ALOGV("%s: Read surface class = %s", __FUNCTION__,
              className != NULL ? String8(className).string() : "<null>");

        if (className == NULL) {
            continue;
        }

        // Surface.writeToParcel
        String16 name = parcel->readString16();
        ALOGV("%s: Read surface name = %s",
              __FUNCTION__, String8(name).string());
        sp<IBinder> binder(parcel->readStrongBinder());
        ALOGV("%s: Read surface binder = %p",
              __FUNCTION__, binder.get());

        sp<Surface> surface;

        if (binder != NULL) {
            sp<IGraphicBufferProducer> gbp =
                    interface_cast<IGraphicBufferProducer>(binder);
            surface = new Surface(gbp);
        }

        mSurfaceList.push_back(surface);
    }

    return OK;
}

status_t CaptureRequest::writeToParcel(Parcel* parcel) const {
    if (parcel == NULL) {
        ALOGE("%s: Null parcel", __FUNCTION__);
        return BAD_VALUE;
    }

    status_t err;

    if ((err = mMetadata.writeToParcel(parcel)) != OK) {
        return err;
    }

    int32_t size = static_cast<int32_t>(mSurfaceList.size());

    // Send 0-sized arrays when it's empty. Do not send null arrays.
    parcel->writeInt32(size);

    for (int32_t i = 0; i < size; ++i) {
        sp<Surface> surface = mSurfaceList[i];

        sp<IBinder> binder;
        if (surface != 0) {
            binder = surface->getIGraphicBufferProducer()->asBinder();
        }

        // not sure if readParcelableArray does this, hard to tell from source
        parcel->writeString16(String16("android.view.Surface"));

        // Surface.writeToParcel
        parcel->writeString16(String16("unknown_name"));
        // Surface.nativeWriteToParcel
        parcel->writeStrongBinder(binder);
    }

    return OK;
}

}; // namespace android
