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

#ifndef ANDROID_BATTERYSERVICE_H
#define ANDROID_BATTERYSERVICE_H

#include <binder/Parcel.h>
#include <utils/Errors.h>
#include <utils/String8.h>

namespace android {

// must be kept in sync with definitions in BatteryManager.java
enum {
    BATTERY_STATUS_UNKNOWN = 1, // equals BatteryManager.BATTERY_STATUS_UNKNOWN constant
    BATTERY_STATUS_CHARGING = 2, // equals BatteryManager.BATTERY_STATUS_CHARGING constant
    BATTERY_STATUS_DISCHARGING = 3, // equals BatteryManager.BATTERY_STATUS_DISCHARGING constant
    BATTERY_STATUS_NOT_CHARGING = 4, // equals BatteryManager.BATTERY_STATUS_NOT_CHARGING constant
    BATTERY_STATUS_FULL = 5, // equals BatteryManager.BATTERY_STATUS_FULL constant
};

// must be kept in sync with definitions in BatteryManager.java
enum {
    BATTERY_HEALTH_UNKNOWN = 1, // equals BatteryManager.BATTERY_HEALTH_UNKNOWN constant
    BATTERY_HEALTH_GOOD = 2, // equals BatteryManager.BATTERY_HEALTH_GOOD constant
    BATTERY_HEALTH_OVERHEAT = 3, // equals BatteryManager.BATTERY_HEALTH_OVERHEAT constant
    BATTERY_HEALTH_DEAD = 4, // equals BatteryManager.BATTERY_HEALTH_DEAD constant
    BATTERY_HEALTH_OVER_VOLTAGE = 5, // equals BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE constant
    BATTERY_HEALTH_UNSPECIFIED_FAILURE = 6, // equals BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE constant
    BATTERY_HEALTH_COLD = 7, // equals BatteryManager.BATTERY_HEALTH_COLD constant
};

struct BatteryProperties {
    bool chargerAcOnline;
    bool chargerUsbOnline;
    bool chargerWirelessOnline;
    int batteryStatus;
    int batteryHealth;
    bool batteryPresent;
    int batteryLevel;
    int batteryVoltage;
    int batteryCurrentNow;
    int batteryChargeCounter;
    int batteryTemperature;
    String8 batteryTechnology;

    status_t writeToParcel(Parcel* parcel) const;
    status_t readFromParcel(Parcel* parcel);
};

}; // namespace android

#endif // ANDROID_BATTERYSERVICE_H
