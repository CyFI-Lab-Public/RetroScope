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

#define LOG_TAG "IBatteryPropertiesRegistrar"
//#define LOG_NDEBUG 0
#include <utils/Log.h>

#include <batteryservice/IBatteryPropertiesListener.h>
#include <batteryservice/IBatteryPropertiesRegistrar.h>
#include <stdint.h>
#include <sys/types.h>
#include <binder/Parcel.h>

namespace android {

class BpBatteryPropertiesRegistrar : public BpInterface<IBatteryPropertiesRegistrar> {
public:
    BpBatteryPropertiesRegistrar(const sp<IBinder>& impl)
        : BpInterface<IBatteryPropertiesRegistrar>(impl) {}

        void registerListener(const sp<IBatteryPropertiesListener>& listener) {
            Parcel data;
            data.writeInterfaceToken(IBatteryPropertiesRegistrar::getInterfaceDescriptor());
            data.writeStrongBinder(listener->asBinder());
            remote()->transact(REGISTER_LISTENER, data, NULL);
        }

        void unregisterListener(const sp<IBatteryPropertiesListener>& listener) {
            Parcel data;
            data.writeInterfaceToken(IBatteryPropertiesRegistrar::getInterfaceDescriptor());
            data.writeStrongBinder(listener->asBinder());
            remote()->transact(UNREGISTER_LISTENER, data, NULL);
        }
};

IMPLEMENT_META_INTERFACE(BatteryPropertiesRegistrar, "android.os.IBatteryPropertiesRegistrar");

status_t BnBatteryPropertiesRegistrar::onTransact(uint32_t code,
                                                  const Parcel& data,
                                                  Parcel* reply,
                                                  uint32_t flags)
{
    switch(code) {
        case REGISTER_LISTENER: {
            CHECK_INTERFACE(IBatteryPropertiesRegistrar, data, reply);
            sp<IBatteryPropertiesListener> listener =
                interface_cast<IBatteryPropertiesListener>(data.readStrongBinder());
            registerListener(listener);
            return OK;
        }

        case UNREGISTER_LISTENER: {
            CHECK_INTERFACE(IBatteryPropertiesRegistrar, data, reply);
            sp<IBatteryPropertiesListener> listener =
                interface_cast<IBatteryPropertiesListener>(data.readStrongBinder());
            unregisterListener(listener);
            return OK;
        }
    }
    return BBinder::onTransact(code, data, reply, flags);
};

// ----------------------------------------------------------------------------

}; // namespace android
