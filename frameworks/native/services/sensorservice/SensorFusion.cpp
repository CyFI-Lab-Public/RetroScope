/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include "SensorDevice.h"
#include "SensorFusion.h"
#include "SensorService.h"

namespace android {
// ---------------------------------------------------------------------------

ANDROID_SINGLETON_STATIC_INSTANCE(SensorFusion)

SensorFusion::SensorFusion()
    : mSensorDevice(SensorDevice::getInstance()),
      mEnabled(false), mGyroTime(0)
{
    sensor_t const* list;
    Sensor uncalibratedGyro;
    ssize_t count = mSensorDevice.getSensorList(&list);
    if (count > 0) {
        for (size_t i=0 ; i<size_t(count) ; i++) {
            if (list[i].type == SENSOR_TYPE_ACCELEROMETER) {
                mAcc = Sensor(list + i);
            }
            if (list[i].type == SENSOR_TYPE_MAGNETIC_FIELD) {
                mMag = Sensor(list + i);
            }
            if (list[i].type == SENSOR_TYPE_GYROSCOPE) {
                mGyro = Sensor(list + i);
            }
            if (list[i].type == SENSOR_TYPE_GYROSCOPE_UNCALIBRATED) {
                uncalibratedGyro = Sensor(list + i);
            }
        }

        // Use the uncalibrated gyroscope for sensor fusion when available
        if (uncalibratedGyro.getType() == SENSOR_TYPE_GYROSCOPE_UNCALIBRATED) {
            mGyro = uncalibratedGyro;
        }

        // 200 Hz for gyro events is a good compromise between precision
        // and power/cpu usage.
        mEstimatedGyroRate = 200;
        mTargetDelayNs = 1000000000LL/mEstimatedGyroRate;
        mFusion.init();
    }
}

void SensorFusion::process(const sensors_event_t& event) {
    if (event.type == mGyro.getType()) {
        if (mGyroTime != 0) {
            const float dT = (event.timestamp - mGyroTime) / 1000000000.0f;
            mFusion.handleGyro(vec3_t(event.data), dT);
            // here we estimate the gyro rate (useful for debugging)
            const float freq = 1 / dT;
            if (freq >= 100 && freq<1000) { // filter values obviously wrong
                const float alpha = 1 / (1 + dT); // 1s time-constant
                mEstimatedGyroRate = freq + (mEstimatedGyroRate - freq)*alpha;
            }
        }
        mGyroTime = event.timestamp;
    } else if (event.type == SENSOR_TYPE_MAGNETIC_FIELD) {
        const vec3_t mag(event.data);
        mFusion.handleMag(mag);
    } else if (event.type == SENSOR_TYPE_ACCELEROMETER) {
        const vec3_t acc(event.data);
        mFusion.handleAcc(acc);
        mAttitude = mFusion.getAttitude();
    }
}

template <typename T> inline T min(T a, T b) { return a<b ? a : b; }
template <typename T> inline T max(T a, T b) { return a>b ? a : b; }

status_t SensorFusion::activate(void* ident, bool enabled) {

    ALOGD_IF(DEBUG_CONNECTIONS,
            "SensorFusion::activate(ident=%p, enabled=%d)",
            ident, enabled);

    const ssize_t idx = mClients.indexOf(ident);
    if (enabled) {
        if (idx < 0) {
            mClients.add(ident);
        }
    } else {
        if (idx >= 0) {
            mClients.removeItemsAt(idx);
        }
    }

    if (enabled) {
        ALOGD_IF(DEBUG_CONNECTIONS, "SensorFusion calling batch ident=%p ", ident);
        // Activating a sensor in continuous mode is equivalent to calling batch with the default
        // period and timeout equal to ZERO, followed by a call to activate.
        mSensorDevice.batch(ident, mAcc.getHandle(), 0, DEFAULT_EVENTS_PERIOD, 0);
        mSensorDevice.batch(ident, mMag.getHandle(), 0, DEFAULT_EVENTS_PERIOD, 0);
        mSensorDevice.batch(ident, mGyro.getHandle(), 0, DEFAULT_EVENTS_PERIOD, 0);
    }

    mSensorDevice.activate(ident, mAcc.getHandle(), enabled);
    mSensorDevice.activate(ident, mMag.getHandle(), enabled);
    mSensorDevice.activate(ident, mGyro.getHandle(), enabled);

    const bool newState = mClients.size() != 0;
    if (newState != mEnabled) {
        mEnabled = newState;
        if (newState) {
            mFusion.init();
            mGyroTime = 0;
        }
    }
    return NO_ERROR;
}

status_t SensorFusion::setDelay(void* ident, int64_t ns) {
    mSensorDevice.setDelay(ident, mAcc.getHandle(), ns);
    mSensorDevice.setDelay(ident, mMag.getHandle(), ms2ns(20));
    mSensorDevice.setDelay(ident, mGyro.getHandle(), mTargetDelayNs);
    return NO_ERROR;
}


float SensorFusion::getPowerUsage() const {
    float power =   mAcc.getPowerUsage() +
                    mMag.getPowerUsage() +
                    mGyro.getPowerUsage();
    return power;
}

int32_t SensorFusion::getMinDelay() const {
    return mAcc.getMinDelay();
}

void SensorFusion::dump(String8& result) {
    const Fusion& fusion(mFusion);
    result.appendFormat("9-axis fusion %s (%d clients), gyro-rate=%7.2fHz, "
            "q=< %g, %g, %g, %g > (%g), "
            "b=< %g, %g, %g >\n",
            mEnabled ? "enabled" : "disabled",
            mClients.size(),
            mEstimatedGyroRate,
            fusion.getAttitude().x,
            fusion.getAttitude().y,
            fusion.getAttitude().z,
            fusion.getAttitude().w,
            length(fusion.getAttitude()),
            fusion.getBias().x,
            fusion.getBias().y,
            fusion.getBias().z);
}

// ---------------------------------------------------------------------------
}; // namespace android
