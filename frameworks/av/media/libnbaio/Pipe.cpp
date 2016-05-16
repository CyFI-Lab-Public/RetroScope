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

#define LOG_TAG "Pipe"
//#define LOG_NDEBUG 0

#include <cutils/atomic.h>
#include <cutils/compiler.h>
#include <utils/Log.h>
#include <media/nbaio/Pipe.h>
#include <media/nbaio/roundup.h>

namespace android {

Pipe::Pipe(size_t maxFrames, NBAIO_Format format) :
        NBAIO_Sink(format),
        mMaxFrames(roundup(maxFrames)),
        mBuffer(malloc(mMaxFrames * Format_frameSize(format))),
        mRear(0),
        mReaders(0)
{
}

Pipe::~Pipe()
{
    ALOG_ASSERT(android_atomic_acquire_load(&mReaders) == 0);
    free(mBuffer);
}

ssize_t Pipe::write(const void *buffer, size_t count)
{
    // count == 0 is unlikely and not worth checking for
    if (CC_UNLIKELY(!mNegotiated)) {
        return NEGOTIATE;
    }
    // write() is not multi-thread safe w.r.t. itself, so no mutex or atomic op needed to read mRear
    size_t rear = mRear & (mMaxFrames - 1);
    size_t written = mMaxFrames - rear;
    if (CC_LIKELY(written > count)) {
        written = count;
    }
    memcpy((char *) mBuffer + (rear << mBitShift), buffer, written << mBitShift);
    if (CC_UNLIKELY(rear + written == mMaxFrames)) {
        if (CC_UNLIKELY((count -= written) > rear)) {
            count = rear;
        }
        if (CC_LIKELY(count > 0)) {
            memcpy(mBuffer, (char *) buffer + (written << mBitShift), count << mBitShift);
            written += count;
        }
    }
    android_atomic_release_store(written + mRear, &mRear);
    mFramesWritten += written;
    return written;
}

}   // namespace android
