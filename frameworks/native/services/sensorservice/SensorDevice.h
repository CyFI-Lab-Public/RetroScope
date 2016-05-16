/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef ANDROID_SENSOR_DEVICE_H
#define ANDROID_SENSOR_DEVICE_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/KeyedVector.h>
#include <utils/Singleton.h>
#include <utils/String8.h>

#include <gui/Sensor.h>

// ---------------------------------------------------------------------------

namespace android {
// ---------------------------------------------------------------------------

class SensorDevice : public Singleton<SensorDevice> {
    friend class Singleton<SensorDevice>;
    sensors_poll_device_1_t* mSensorDevice;
    struct sensors_module_t* mSensorModule;
    static const nsecs_t MINIMUM_EVENTS_PERIOD =   1000000; // 1000 Hz
    mutable Mutex mLock; // protect mActivationCount[].batchParams
    // fixed-size array after construction

    // Struct to store all the parameters(samplingPeriod, maxBatchReportLatency and flags) from
    // batch call. For continous mode clients, maxBatchReportLatency is set to zero.
    struct BatchParams {
      int flags;
      nsecs_t batchDelay, batchTimeout;
      BatchParams() : flags(0), batchDelay(0), batchTimeout(0) {}
      BatchParams(int flag, nsecs_t delay, nsecs_t timeout): flags(flag), batchDelay(delay),
          batchTimeout(timeout) { }
      bool operator != (const BatchParams& other) {
          return other.batchDelay != batchDelay || other.batchTimeout != batchTimeout ||
                 other.flags != flags;
      }
    };

    // Store batch parameters in the KeyedVector and the optimal batch_rate and timeout in
    // bestBatchParams. For every batch() call corresponding params are stored in batchParams
    // vector. A continuous mode request is batch(... timeout=0 ..) followed by activate(). A batch
    // mode request is batch(... timeout > 0 ...) followed by activate().
    // Info is a per-sensor data structure which contains the batch parameters for each client that
    // has registered for this sensor.
    struct Info {
        BatchParams bestBatchParams;
        // Key is the unique identifier(ident) for each client, value is the batch parameters
        // requested by the client.
        KeyedVector<void*, BatchParams> batchParams;

        Info() : bestBatchParams(-1, -1, -1) {}
        // Sets batch parameters for this ident. Returns error if this ident is not already present
        // in the KeyedVector above.
        status_t setBatchParamsForIdent(void* ident, int flags, int64_t samplingPeriodNs,
                                        int64_t maxBatchReportLatencyNs);
        // Finds the optimal parameters for batching and stores them in bestBatchParams variable.
        void selectBatchParams();
        // Removes batchParams for an ident and re-computes bestBatchParams. Returns the index of
        // the removed ident. If index >=0, ident is present and successfully removed.
        ssize_t removeBatchParamsForIdent(void* ident);
    };
    DefaultKeyedVector<int, Info> mActivationCount;

    SensorDevice();
public:
    ssize_t getSensorList(sensor_t const** list);
    status_t initCheck() const;
    int getHalDeviceVersion() const;
    ssize_t poll(sensors_event_t* buffer, size_t count);
    status_t activate(void* ident, int handle, int enabled);
    status_t batch(void* ident, int handle, int flags, int64_t samplingPeriodNs,
                   int64_t maxBatchReportLatencyNs);
    // Call batch with timeout zero instead of calling setDelay() for newer devices.
    status_t setDelay(void* ident, int handle, int64_t ns);
    status_t flush(void* ident, int handle);
    void autoDisable(void *ident, int handle);
    void dump(String8& result);
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SENSOR_DEVICE_H
