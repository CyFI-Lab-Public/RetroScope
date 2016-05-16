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

#ifndef ANDROID_SENSOR_SERVICE_H
#define ANDROID_SENSOR_SERVICE_H

#include <stdint.h>
#include <sys/types.h>

#include <utils/Vector.h>
#include <utils/SortedVector.h>
#include <utils/KeyedVector.h>
#include <utils/threads.h>
#include <utils/RefBase.h>

#include <binder/BinderService.h>

#include <gui/Sensor.h>
#include <gui/BitTube.h>
#include <gui/ISensorServer.h>
#include <gui/ISensorEventConnection.h>

#include "SensorInterface.h"

// ---------------------------------------------------------------------------

#define DEBUG_CONNECTIONS   false
// Max size is 1 MB which is enough to accept a batch of about 10k events.
#define MAX_SOCKET_BUFFER_SIZE_BATCHED 1024 * 1024
#define SOCKET_BUFFER_SIZE_NON_BATCHED 4 * 1024

struct sensors_poll_device_t;
struct sensors_module_t;

namespace android {
// ---------------------------------------------------------------------------

class SensorService :
        public BinderService<SensorService>,
        public BnSensorServer,
        protected Thread
{
    friend class BinderService<SensorService>;

    static const char* WAKE_LOCK_NAME;

    static char const* getServiceName() ANDROID_API { return "sensorservice"; }
    SensorService() ANDROID_API;
    virtual ~SensorService();

    virtual void onFirstRef();

    // Thread interface
    virtual bool threadLoop();

    // ISensorServer interface
    virtual Vector<Sensor> getSensorList();
    virtual sp<ISensorEventConnection> createSensorEventConnection();
    virtual status_t dump(int fd, const Vector<String16>& args);


    class SensorEventConnection : public BnSensorEventConnection {
        virtual ~SensorEventConnection();
        virtual void onFirstRef();
        virtual sp<BitTube> getSensorChannel() const;
        virtual status_t enableDisable(int handle, bool enabled, nsecs_t samplingPeriodNs,
                                       nsecs_t maxBatchReportLatencyNs, int reservedFlags);
        virtual status_t setEventRate(int handle, nsecs_t samplingPeriodNs);
        virtual status_t flush();
        // Count the number of flush complete events which are about to be dropped in the buffer.
        // Increment mPendingFlushEventsToSend in mSensorInfo. These flush complete events will be
        // sent separately before the next batch of events.
        void countFlushCompleteEventsLocked(sensors_event_t* scratch, int numEventsDropped);

        sp<SensorService> const mService;
        sp<BitTube> mChannel;
        uid_t mUid;
        mutable Mutex mConnectionLock;

        struct FlushInfo {
            // The number of flush complete events dropped for this sensor is stored here.
            // They are sent separately before the next batch of events.
            int mPendingFlushEventsToSend;
            // Every activate is preceded by a flush. Only after the first flush complete is
            // received, the events for the sensor are sent on that *connection*.
            bool mFirstFlushPending;
            FlushInfo() : mPendingFlushEventsToSend(0), mFirstFlushPending(false) {}
        };
        // protected by SensorService::mLock. Key for this vector is the sensor handle.
        KeyedVector<int, FlushInfo> mSensorInfo;

    public:
        SensorEventConnection(const sp<SensorService>& service, uid_t uid);

        status_t sendEvents(sensors_event_t const* buffer, size_t count,
                sensors_event_t* scratch = NULL);
        bool hasSensor(int32_t handle) const;
        bool hasAnySensor() const;
        bool addSensor(int32_t handle);
        bool removeSensor(int32_t handle);
        void setFirstFlushPending(int32_t handle, bool value);
        void dump(String8& result);

        uid_t getUid() const { return mUid; }
    };

    class SensorRecord {
        SortedVector< wp<SensorEventConnection> > mConnections;
    public:
        SensorRecord(const sp<SensorEventConnection>& connection);
        bool addConnection(const sp<SensorEventConnection>& connection);
        bool removeConnection(const wp<SensorEventConnection>& connection);
        size_t getNumConnections() const { return mConnections.size(); }
    };

    SortedVector< wp<SensorEventConnection> > getActiveConnections() const;
    DefaultKeyedVector<int, SensorInterface*> getActiveVirtualSensors() const;

    String8 getSensorName(int handle) const;
    bool isVirtualSensor(int handle) const;
    void recordLastValue(sensors_event_t const * buffer, size_t count);
    static void sortEventBuffer(sensors_event_t* buffer, size_t count);
    Sensor registerSensor(SensorInterface* sensor);
    Sensor registerVirtualSensor(SensorInterface* sensor);
    status_t cleanupWithoutDisable(
            const sp<SensorEventConnection>& connection, int handle);
    status_t cleanupWithoutDisableLocked(
            const sp<SensorEventConnection>& connection, int handle);
    void cleanupAutoDisabledSensor(const sp<SensorEventConnection>& connection,
            sensors_event_t const* buffer, const int count);

    // constants
    Vector<Sensor> mSensorList;
    Vector<Sensor> mUserSensorListDebug;
    Vector<Sensor> mUserSensorList;
    DefaultKeyedVector<int, SensorInterface*> mSensorMap;
    Vector<SensorInterface *> mVirtualSensorList;
    status_t mInitCheck;
    size_t mSocketBufferSize;

    // protected by mLock
    mutable Mutex mLock;
    DefaultKeyedVector<int, SensorRecord*> mActiveSensors;
    DefaultKeyedVector<int, SensorInterface*> mActiveVirtualSensors;
    SortedVector< wp<SensorEventConnection> > mActiveConnections;

    // The size of this vector is constant, only the items are mutable
    KeyedVector<int32_t, sensors_event_t> mLastEventSeen;

public:
    void cleanupConnection(SensorEventConnection* connection);
    status_t enable(const sp<SensorEventConnection>& connection, int handle,
                    nsecs_t samplingPeriodNs,  nsecs_t maxBatchReportLatencyNs, int reservedFlags);
    status_t disable(const sp<SensorEventConnection>& connection, int handle);
    status_t setEventRate(const sp<SensorEventConnection>& connection, int handle, nsecs_t ns);
    status_t flushSensor(const sp<SensorEventConnection>& connection, int handle);
};

// ---------------------------------------------------------------------------
}; // namespace android

#endif // ANDROID_SENSOR_SERVICE_H
