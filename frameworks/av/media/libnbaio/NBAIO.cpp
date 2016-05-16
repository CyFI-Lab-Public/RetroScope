/*
 * Copyright (C) 2012 The Android Open Source Project
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

#define LOG_TAG "NBAIO"
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <media/nbaio/NBAIO.h>

namespace android {

size_t Format_frameSize(NBAIO_Format format)
{
    return Format_channelCount(format) * sizeof(short);
}

size_t Format_frameBitShift(NBAIO_Format format)
{
    // sizeof(short) == 2, so frame size == 1 << channels
    return Format_channelCount(format);
}

enum {
    Format_SR_8000,
    Format_SR_11025,
    Format_SR_16000,
    Format_SR_22050,
    Format_SR_24000,
    Format_SR_32000,
    Format_SR_44100,
    Format_SR_48000,
    Format_SR_Mask = 7
};

enum {
    Format_C_1 = 0x08,
    Format_C_2 = 0x10,
    Format_C_Mask = 0x18
};

unsigned Format_sampleRate(NBAIO_Format format)
{
    if (format == Format_Invalid) {
        return 0;
    }
    switch (format & Format_SR_Mask) {
    case Format_SR_8000:
        return 8000;
    case Format_SR_11025:
        return 11025;
    case Format_SR_16000:
        return 16000;
    case Format_SR_22050:
        return 22050;
    case Format_SR_24000:
        return 24000;
    case Format_SR_32000:
        return 32000;
    case Format_SR_44100:
        return 44100;
    case Format_SR_48000:
        return 48000;
    default:
        return 0;
    }
}

unsigned Format_channelCount(NBAIO_Format format)
{
    if (format == Format_Invalid) {
        return 0;
    }
    switch (format & Format_C_Mask) {
    case Format_C_1:
        return 1;
    case Format_C_2:
        return 2;
    default:
        return 0;
    }
}

NBAIO_Format Format_from_SR_C(unsigned sampleRate, unsigned channelCount)
{
    NBAIO_Format format;
    switch (sampleRate) {
    case 8000:
        format = Format_SR_8000;
        break;
    case 11025:
        format = Format_SR_11025;
        break;
    case 16000:
        format = Format_SR_16000;
        break;
    case 22050:
        format = Format_SR_22050;
        break;
    case 24000:
        format = Format_SR_24000;
        break;
    case 32000:
        format = Format_SR_32000;
        break;
    case 44100:
        format = Format_SR_44100;
        break;
    case 48000:
        format = Format_SR_48000;
        break;
    default:
        return Format_Invalid;
    }
    switch (channelCount) {
    case 1:
        format |= Format_C_1;
        break;
    case 2:
        format |= Format_C_2;
        break;
    default:
        return Format_Invalid;
    }
    return format;
}

// This is a default implementation; it is expected that subclasses will optimize this.
ssize_t NBAIO_Sink::writeVia(writeVia_t via, size_t total, void *user, size_t block)
{
    if (!mNegotiated) {
        return (ssize_t) NEGOTIATE;
    }
    static const size_t maxBlock = 32;
    size_t frameSize = Format_frameSize(mFormat);
    ALOG_ASSERT(frameSize > 0 && frameSize <= 8);
    // double guarantees alignment for stack similar to what malloc() gives for heap
    if (block == 0 || block > maxBlock) {
        block = maxBlock;
    }
    double buffer[((frameSize * block) + sizeof(double) - 1) / sizeof(double)];
    size_t accumulator = 0;
    while (accumulator < total) {
        size_t count = total - accumulator;
        if (count > block) {
            count = block;
        }
        ssize_t ret = via(user, buffer, count);
        if (ret > 0) {
            ALOG_ASSERT((size_t) ret <= count);
            size_t maxRet = ret;
            ret = write(buffer, maxRet);
            if (ret > 0) {
                ALOG_ASSERT((size_t) ret <= maxRet);
                accumulator += ret;
                continue;
            }
        }
        return accumulator > 0 ? accumulator : ret;
    }
    return accumulator;
}

// This is a default implementation; it is expected that subclasses will optimize this.
ssize_t NBAIO_Source::readVia(readVia_t via, size_t total, void *user,
                              int64_t readPTS, size_t block)
{
    if (!mNegotiated) {
        return (ssize_t) NEGOTIATE;
    }
    static const size_t maxBlock = 32;
    size_t frameSize = Format_frameSize(mFormat);
    ALOG_ASSERT(frameSize > 0 && frameSize <= 8);
    // double guarantees alignment for stack similar to what malloc() gives for heap
    if (block == 0 || block > maxBlock) {
        block = maxBlock;
    }
    double buffer[((frameSize * block) + sizeof(double) - 1) / sizeof(double)];
    size_t accumulator = 0;
    while (accumulator < total) {
        size_t count = total - accumulator;
        if (count > block) {
            count = block;
        }
        ssize_t ret = read(buffer, count, readPTS);
        if (ret > 0) {
            ALOG_ASSERT((size_t) ret <= count);
            size_t maxRet = ret;
            ret = via(user, buffer, maxRet, readPTS);
            if (ret > 0) {
                ALOG_ASSERT((size_t) ret <= maxRet);
                accumulator += ret;
                continue;
            }
        }
        return accumulator > 0 ? accumulator : ret;
    }
    return accumulator;
}

// Default implementation that only accepts my mFormat
ssize_t NBAIO_Port::negotiate(const NBAIO_Format offers[], size_t numOffers,
                                  NBAIO_Format counterOffers[], size_t& numCounterOffers)
{
    ALOGV("negotiate offers=%p numOffers=%u countersOffers=%p numCounterOffers=%u",
            offers, numOffers, counterOffers, numCounterOffers);
    if (mFormat != Format_Invalid) {
        for (size_t i = 0; i < numOffers; ++i) {
            if (offers[i] == mFormat) {
                mNegotiated = true;
                return i;
            }
        }
        if (numCounterOffers > 0) {
            counterOffers[0] = mFormat;
        }
        numCounterOffers = 1;
    } else {
        numCounterOffers = 0;
    }
    return (ssize_t) NEGOTIATE;
}

}   // namespace android
