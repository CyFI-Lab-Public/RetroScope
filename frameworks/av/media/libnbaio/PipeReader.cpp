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

#define LOG_TAG "PipeReader"
//#define LOG_NDEBUG 0

#include <cutils/compiler.h>
#include <utils/Log.h>
#include <media/nbaio/PipeReader.h>

namespace android {

PipeReader::PipeReader(Pipe& pipe) :
        NBAIO_Source(pipe.mFormat),
        mPipe(pipe),
        // any data already in the pipe is not visible to this PipeReader
        mFront(android_atomic_acquire_load(&pipe.mRear)),
        mFramesOverrun(0),
        mOverruns(0)
{
    android_atomic_inc(&pipe.mReaders);
}

PipeReader::~PipeReader()
{
    int32_t readers = android_atomic_dec(&mPipe.mReaders);
    ALOG_ASSERT(readers > 0);
}

ssize_t PipeReader::availableToRead()
{
    if (CC_UNLIKELY(!mNegotiated)) {
        return NEGOTIATE;
    }
    int32_t rear = android_atomic_acquire_load(&mPipe.mRear);
    // read() is not multi-thread safe w.r.t. itself, so no mutex or atomic op needed to read mFront
    size_t avail = rear - mFront;
    if (CC_UNLIKELY(avail > mPipe.mMaxFrames)) {
        // Discard 1/16 of the most recent data in pipe to avoid another overrun immediately
        int32_t oldFront = mFront;
        mFront = rear - mPipe.mMaxFrames + (mPipe.mMaxFrames >> 4);
        mFramesOverrun += (size_t) (mFront - oldFront);
        ++mOverruns;
        return OVERRUN;
    }
    return avail;
}

ssize_t PipeReader::read(void *buffer, size_t count, int64_t readPTS)
{
    ssize_t avail = availableToRead();
    if (CC_UNLIKELY(avail <= 0)) {
        return avail;
    }
    // An overrun can occur from here on and be silently ignored,
    // but it will be caught at next read()
    if (CC_LIKELY(count > (size_t) avail)) {
        count = avail;
    }
    size_t front = mFront & (mPipe.mMaxFrames - 1);
    size_t red = mPipe.mMaxFrames - front;
    if (CC_LIKELY(red > count)) {
        red = count;
    }
    // In particular, an overrun during the memcpy will result in reading corrupt data
    memcpy(buffer, (char *) mPipe.mBuffer + (front << mBitShift), red << mBitShift);
    // We could re-read the rear pointer here to detect the corruption, but why bother?
    if (CC_UNLIKELY(front + red == mPipe.mMaxFrames)) {
        if (CC_UNLIKELY((count -= red) > front)) {
            count = front;
        }
        if (CC_LIKELY(count > 0)) {
            memcpy((char *) buffer + (red << mBitShift), mPipe.mBuffer, count << mBitShift);
            red += count;
        }
    }
    mFront += red;
    mFramesRead += red;
    return red;
}

}   // namespace android
