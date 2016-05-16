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

#include <stdint.h>
#include <sys/types.h>

#include <utils/Errors.h>
#include <utils/String8.h>
#include <utils/Flattenable.h>

#include <hardware/sensors.h>

#include <gui/Sensor.h>

// ----------------------------------------------------------------------------
namespace android {
// ----------------------------------------------------------------------------

Sensor::Sensor()
    : mHandle(0), mType(0),
      mMinValue(0), mMaxValue(0), mResolution(0),
      mPower(0), mMinDelay(0), mFifoReservedEventCount(0), mFifoMaxEventCount(0)
{
}

Sensor::Sensor(struct sensor_t const* hwSensor, int halVersion)
{
    mName = hwSensor->name;
    mVendor = hwSensor->vendor;
    mVersion = hwSensor->version;
    mHandle = hwSensor->handle;
    mType = hwSensor->type;
    mMinValue = 0;                      // FIXME: minValue
    mMaxValue = hwSensor->maxRange;     // FIXME: maxValue
    mResolution = hwSensor->resolution;
    mPower = hwSensor->power;
    mMinDelay = hwSensor->minDelay;
    // Set fifo event count zero for older devices which do not support batching. Fused
    // sensors also have their fifo counts set to zero.
    if (halVersion >= SENSORS_DEVICE_API_VERSION_1_1) {
        mFifoReservedEventCount = hwSensor->fifoReservedEventCount;
        mFifoMaxEventCount = hwSensor->fifoMaxEventCount;
    } else {
        mFifoReservedEventCount = 0;
        mFifoMaxEventCount = 0;
    }
}

Sensor::~Sensor()
{
}

const String8& Sensor::getName() const {
    return mName;
}

const String8& Sensor::getVendor() const {
    return mVendor;
}

int32_t Sensor::getHandle() const {
    return mHandle;
}

int32_t Sensor::getType() const {
    return mType;
}

float Sensor::getMinValue() const {
    return mMinValue;
}

float Sensor::getMaxValue() const {
    return mMaxValue;
}

float Sensor::getResolution() const {
    return mResolution;
}

float Sensor::getPowerUsage() const {
    return mPower;
}

int32_t Sensor::getMinDelay() const {
    return mMinDelay;
}

nsecs_t Sensor::getMinDelayNs() const {
    return getMinDelay() * 1000;
}

int32_t Sensor::getVersion() const {
    return mVersion;
}

int32_t Sensor::getFifoReservedEventCount() const {
    return mFifoReservedEventCount;
}

int32_t Sensor::getFifoMaxEventCount() const {
    return mFifoMaxEventCount;
}

size_t Sensor::getFlattenedSize() const
{
    size_t fixedSize =
            sizeof(int32_t) * 3 +
            sizeof(float) * 4 +
            sizeof(int32_t) * 3;

    size_t variableSize =
            sizeof(int32_t) + FlattenableUtils::align<4>(mName.length()) +
            sizeof(int32_t) + FlattenableUtils::align<4>(mVendor.length());

    return fixedSize + variableSize;
}

status_t Sensor::flatten(void* buffer, size_t size) const {
    if (size < getFlattenedSize()) {
        return NO_MEMORY;
    }

    FlattenableUtils::write(buffer, size, mName.length());
    memcpy(static_cast<char*>(buffer), mName.string(), mName.length());
    FlattenableUtils::advance(buffer, size, FlattenableUtils::align<4>(mName.length()));

    FlattenableUtils::write(buffer, size, mVendor.length());
    memcpy(static_cast<char*>(buffer), mVendor.string(), mVendor.length());
    FlattenableUtils::advance(buffer, size, FlattenableUtils::align<4>(mVendor.length()));

    FlattenableUtils::write(buffer, size, mVersion);
    FlattenableUtils::write(buffer, size, mHandle);
    FlattenableUtils::write(buffer, size, mType);
    FlattenableUtils::write(buffer, size, mMinValue);
    FlattenableUtils::write(buffer, size, mMaxValue);
    FlattenableUtils::write(buffer, size, mResolution);
    FlattenableUtils::write(buffer, size, mPower);
    FlattenableUtils::write(buffer, size, mMinDelay);
    FlattenableUtils::write(buffer, size, mFifoReservedEventCount);
    FlattenableUtils::write(buffer, size, mFifoMaxEventCount);
    return NO_ERROR;
}

status_t Sensor::unflatten(void const* buffer, size_t size) {
    size_t len;

    if (size < sizeof(size_t)) {
        return NO_MEMORY;
    }
    FlattenableUtils::read(buffer, size, len);
    if (size < len) {
        return NO_MEMORY;
    }
    mName.setTo(static_cast<char const*>(buffer), len);
    FlattenableUtils::advance(buffer, size, FlattenableUtils::align<4>(len));


    if (size < sizeof(size_t)) {
        return NO_MEMORY;
    }
    FlattenableUtils::read(buffer, size, len);
    if (size < len) {
        return NO_MEMORY;
    }
    mVendor.setTo(static_cast<char const*>(buffer), len);
    FlattenableUtils::advance(buffer, size, FlattenableUtils::align<4>(len));

    size_t fixedSize =
            sizeof(int32_t) * 3 +
            sizeof(float) * 4 +
            sizeof(int32_t) * 3;

    if (size < fixedSize) {
        return NO_MEMORY;
    }

    FlattenableUtils::read(buffer, size, mVersion);
    FlattenableUtils::read(buffer, size, mHandle);
    FlattenableUtils::read(buffer, size, mType);
    FlattenableUtils::read(buffer, size, mMinValue);
    FlattenableUtils::read(buffer, size, mMaxValue);
    FlattenableUtils::read(buffer, size, mResolution);
    FlattenableUtils::read(buffer, size, mPower);
    FlattenableUtils::read(buffer, size, mMinDelay);
    FlattenableUtils::read(buffer, size, mFifoReservedEventCount);
    FlattenableUtils::read(buffer, size, mFifoMaxEventCount);
    return NO_ERROR;
}

// ----------------------------------------------------------------------------
}; // namespace android
