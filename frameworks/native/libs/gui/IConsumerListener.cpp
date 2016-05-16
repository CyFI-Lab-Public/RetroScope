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

#include <stdint.h>
#include <sys/types.h>

#include <binder/IInterface.h>
#include <binder/Parcel.h>

#include <gui/IConsumerListener.h>

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

enum {
    ON_FRAME_AVAILABLE = IBinder::FIRST_CALL_TRANSACTION,
    ON_BUFFER_RELEASED
};

class BpConsumerListener : public BpInterface<IConsumerListener>
{
public:
    BpConsumerListener(const sp<IBinder>& impl)
        : BpInterface<IConsumerListener>(impl) {
    }

    virtual void onFrameAvailable() {
        Parcel data, reply;
        data.writeInterfaceToken(IConsumerListener::getInterfaceDescriptor());
        remote()->transact(ON_FRAME_AVAILABLE, data, &reply, IBinder::FLAG_ONEWAY);
    }

    virtual void onBuffersReleased() {
        Parcel data, reply;
        data.writeInterfaceToken(IConsumerListener::getInterfaceDescriptor());
        remote()->transact(ON_BUFFER_RELEASED, data, &reply, IBinder::FLAG_ONEWAY);
    }
};

IMPLEMENT_META_INTERFACE(ConsumerListener, "android.gui.IConsumerListener");

// ----------------------------------------------------------------------

status_t BnConsumerListener::onTransact(
    uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{
    switch(code) {
        case ON_FRAME_AVAILABLE:
            CHECK_INTERFACE(IConsumerListener, data, reply);
            onFrameAvailable();
            return NO_ERROR;
        case ON_BUFFER_RELEASED:
            CHECK_INTERFACE(IConsumerListener, data, reply);
            onBuffersReleased();
            return NO_ERROR;
    }
    return BBinder::onTransact(code, data, reply, flags);
}


// ---------------------------------------------------------------------------
}; // namespace android
// ---------------------------------------------------------------------------
