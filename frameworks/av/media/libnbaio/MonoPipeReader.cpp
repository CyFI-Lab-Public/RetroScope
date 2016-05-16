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

#define LOG_TAG "MonoPipeReader"
//#define LOG_NDEBUG 0

#include <cutils/compiler.h>
#include <utils/Log.h>
#include <media/nbaio/MonoPipeReader.h>

namespace android {

MonoPipeReader::MonoPipeReader(MonoPipe* pipe) :
        NBAIO_Source(pipe->mFormat),
        mPipe(pipe)
{
}

MonoPipeReader::~MonoPipeReader()
{
}

ssize_t MonoPipeReader::availableToRead()
{
    if (CC_UNLIKELY(!mNegotiated)) {
        return NEGOTIATE;
    }
    ssize_t ret = android_atomic_acquire_load(&mPipe->mRear) - mPipe->mFront;
    ALOG_ASSERT((0 <= ret) && (ret <= mMaxFrames));
    return ret;
}

ssize_t MonoPipeReader::read(void *buffer, size_t count, int64_t readPTS)
{
    // Compute the "next read PTS" and cache it.  Callers of read pass a read
    // PTS indicating the local time for which they are requesting data along
    // with a count (which is the number of audio frames they are going to
    // ultimately pass to the next stage of the pipeline).  Offsetting readPTS
    // by the duration of count will give us the readPTS which will be passed to
    // us next time, assuming they system continues to operate in steady state
    // with no discontinuities.  We stash this value so it can be used by the
    // MonoPipe writer to imlement getNextWriteTimestamp.
    int64_t nextReadPTS;
    nextReadPTS = mPipe->offsetTimestampByAudioFrames(readPTS, count);

    // count == 0 is unlikely and not worth checking for explicitly; will be handled automatically
    ssize_t red = availableToRead();
    if (CC_UNLIKELY(red <= 0)) {
        // Uh-oh, looks like we are underflowing.  Update the next read PTS and
        // get out.
        mPipe->updateFrontAndNRPTS(mPipe->mFront, nextReadPTS);
        return red;
    }
    if (CC_LIKELY((size_t) red > count)) {
        red = count;
    }
    size_t front = mPipe->mFront & (mPipe->mMaxFrames - 1);
    size_t part1 = mPipe->mMaxFrames - front;
    if (part1 > (size_t) red) {
        part1 = red;
    }
    if (CC_LIKELY(part1 > 0)) {
        memcpy(buffer, (char *) mPipe->mBuffer + (front << mBitShift), part1 << mBitShift);
        if (CC_UNLIKELY(front + part1 == mPipe->mMaxFrames)) {
            size_t part2 = red - part1;
            if (CC_LIKELY(part2 > 0)) {
                memcpy((char *) buffer + (part1 << mBitShift), mPipe->mBuffer, part2 << mBitShift);
            }
        }
        mPipe->updateFrontAndNRPTS(red + mPipe->mFront, nextReadPTS);
        mFramesRead += red;
    }
    return red;
}

void MonoPipeReader::onTimestamp(const AudioTimestamp& timestamp)
{
    mPipe->mTimestampMutator.push(timestamp);
}

}   // namespace android
