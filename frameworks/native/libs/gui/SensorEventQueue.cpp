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

#define LOG_TAG "Sensors"

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/RefBase.h>
#include <utils/Looper.h>

#include <gui/Sensor.h>
#include <gui/BitTube.h>
#include <gui/SensorEventQueue.h>
#include <gui/ISensorEventConnection.h>

#include <android/sensor.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

SensorEventQueue::SensorEventQueue(const sp<ISensorEventConnection>& connection)
    : mSensorEventConnection(connection), mRecBuffer(NULL), mAvailable(0), mConsumed(0) {
    mRecBuffer = new ASensorEvent[MAX_RECEIVE_BUFFER_EVENT_COUNT];
}

SensorEventQueue::~SensorEventQueue() {
    delete [] mRecBuffer;
}

void SensorEventQueue::onFirstRef()
{
    mSensorChannel = mSensorEventConnection->getSensorChannel();
}

int SensorEventQueue::getFd() const
{
    return mSensorChannel->getFd();
}


ssize_t SensorEventQueue::write(const sp<BitTube>& tube,
        ASensorEvent const* events, size_t numEvents) {
    return BitTube::sendObjects(tube, events, numEvents);
}

ssize_t SensorEventQueue::read(ASensorEvent* events, size_t numEvents) {
    if (mAvailable == 0) {
        ssize_t err = BitTube::recvObjects(mSensorChannel,
                mRecBuffer, MAX_RECEIVE_BUFFER_EVENT_COUNT);
        if (err < 0) {
            return err;
        }
        mAvailable = err;
        mConsumed = 0;
    }
    size_t count = numEvents < mAvailable ? numEvents : mAvailable;
    memcpy(events, mRecBuffer + mConsumed, count*sizeof(ASensorEvent));
    mAvailable -= count;
    mConsumed += count;
    return count;
}

sp<Looper> SensorEventQueue::getLooper() const
{
    Mutex::Autolock _l(mLock);
    if (mLooper == 0) {
        mLooper = new Looper(true);
        mLooper->addFd(getFd(), getFd(), ALOOPER_EVENT_INPUT, NULL, NULL);
    }
    return mLooper;
}

status_t SensorEventQueue::waitForEvent() const
{
    const int fd = getFd();
    sp<Looper> looper(getLooper());

    int events;
    int32_t result;
    do {
        result = looper->pollOnce(-1, NULL, &events, NULL);
        if (result == ALOOPER_POLL_ERROR) {
            ALOGE("SensorEventQueue::waitForEvent error (errno=%d)", errno);
            result = -EPIPE; // unknown error, so we make up one
            break;
        }
        if (events & ALOOPER_EVENT_HANGUP) {
            // the other-side has died
            ALOGE("SensorEventQueue::waitForEvent error HANGUP");
            result = -EPIPE; // unknown error, so we make up one
            break;
        }
    } while (result != fd);

    return  (result == fd) ? status_t(NO_ERROR) : result;
}

status_t SensorEventQueue::wake() const
{
    sp<Looper> looper(getLooper());
    looper->wake();
    return NO_ERROR;
}

status_t SensorEventQueue::enableSensor(Sensor const* sensor) const {
    return mSensorEventConnection->enableDisable(sensor->getHandle(), true, 0, 0, false);
}

status_t SensorEventQueue::disableSensor(Sensor const* sensor) const {
    return mSensorEventConnection->enableDisable(sensor->getHandle(), false, 0, 0, false);
}

status_t SensorEventQueue::enableSensor(int32_t handle, int32_t samplingPeriodUs,
                                        int maxBatchReportLatencyUs, int reservedFlags) const {
    return mSensorEventConnection->enableDisable(handle, true, us2ns(samplingPeriodUs),
                                                 us2ns(maxBatchReportLatencyUs), reservedFlags);
}

status_t SensorEventQueue::flush() const {
    return mSensorEventConnection->flush();
}

status_t SensorEventQueue::disableSensor(int32_t handle) const {
    return mSensorEventConnection->enableDisable(handle, false, 0, 0, false);
}

status_t SensorEventQueue::setEventRate(Sensor const* sensor, nsecs_t ns) const {
    return mSensorEventConnection->setEventRate(sensor->getHandle(), ns);
}

// ----------------------------------------------------------------------------
}; // namespace android

