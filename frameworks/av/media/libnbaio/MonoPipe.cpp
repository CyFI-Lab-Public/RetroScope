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

#define LOG_TAG "MonoPipe"
//#define LOG_NDEBUG 0

#include <common_time/cc_helper.h>
#include <cutils/atomic.h>
#include <cutils/compiler.h>
#include <utils/LinearTransform.h>
#include <utils/Log.h>
#include <utils/Trace.h>
#include <media/AudioBufferProvider.h>
#include <media/nbaio/MonoPipe.h>
#include <media/nbaio/roundup.h>


namespace android {

MonoPipe::MonoPipe(size_t reqFrames, NBAIO_Format format, bool writeCanBlock) :
        NBAIO_Sink(format),
        mUpdateSeq(0),
        mReqFrames(reqFrames),
        mMaxFrames(roundup(reqFrames)),
        mBuffer(malloc(mMaxFrames * Format_frameSize(format))),
        mFront(0),
        mRear(0),
        mWriteTsValid(false),
        // mWriteTs
        mSetpoint((reqFrames * 11) / 16),
        mWriteCanBlock(writeCanBlock),
        mIsShutdown(false),
        // mTimestampShared
        mTimestampMutator(&mTimestampShared),
        mTimestampObserver(&mTimestampShared)
{
    CCHelper tmpHelper;
    status_t res;
    uint64_t N, D;

    mNextRdPTS = AudioBufferProvider::kInvalidPTS;

    mSamplesToLocalTime.a_zero = 0;
    mSamplesToLocalTime.b_zero = 0;
    mSamplesToLocalTime.a_to_b_numer = 0;
    mSamplesToLocalTime.a_to_b_denom = 0;

    D = Format_sampleRate(format);
    if (OK != (res = tmpHelper.getLocalFreq(&N))) {
        ALOGE("Failed to fetch local time frequency when constructing a"
              " MonoPipe (res = %d).  getNextWriteTimestamp calls will be"
              " non-functional", res);
        return;
    }

    LinearTransform::reduce(&N, &D);
    static const uint64_t kSignedHiBitsMask   = ~(0x7FFFFFFFull);
    static const uint64_t kUnsignedHiBitsMask = ~(0xFFFFFFFFull);
    if ((N & kSignedHiBitsMask) || (D & kUnsignedHiBitsMask)) {
        ALOGE("Cannot reduce sample rate to local clock frequency ratio to fit"
              " in a 32/32 bit rational.  (max reduction is 0x%016llx/0x%016llx"
              ").  getNextWriteTimestamp calls will be non-functional", N, D);
        return;
    }

    mSamplesToLocalTime.a_to_b_numer = static_cast<int32_t>(N);
    mSamplesToLocalTime.a_to_b_denom = static_cast<uint32_t>(D);
}

MonoPipe::~MonoPipe()
{
    free(mBuffer);
}

ssize_t MonoPipe::availableToWrite() const
{
    if (CC_UNLIKELY(!mNegotiated)) {
        return NEGOTIATE;
    }
    // uses mMaxFrames not mReqFrames, so allows "over-filling" the pipe beyond requested limit
    ssize_t ret = mMaxFrames - (mRear - android_atomic_acquire_load(&mFront));
    ALOG_ASSERT((0 <= ret) && (ret <= mMaxFrames));
    return ret;
}

ssize_t MonoPipe::write(const void *buffer, size_t count)
{
    if (CC_UNLIKELY(!mNegotiated)) {
        return NEGOTIATE;
    }
    size_t totalFramesWritten = 0;
    while (count > 0) {
        // can't return a negative value, as we already checked for !mNegotiated
        size_t avail = availableToWrite();
        size_t written = avail;
        if (CC_LIKELY(written > count)) {
            written = count;
        }
        size_t rear = mRear & (mMaxFrames - 1);
        size_t part1 = mMaxFrames - rear;
        if (part1 > written) {
            part1 = written;
        }
        if (CC_LIKELY(part1 > 0)) {
            memcpy((char *) mBuffer + (rear << mBitShift), buffer, part1 << mBitShift);
            if (CC_UNLIKELY(rear + part1 == mMaxFrames)) {
                size_t part2 = written - part1;
                if (CC_LIKELY(part2 > 0)) {
                    memcpy(mBuffer, (char *) buffer + (part1 << mBitShift), part2 << mBitShift);
                }
            }
            android_atomic_release_store(written + mRear, &mRear);
            totalFramesWritten += written;
        }
        if (!mWriteCanBlock || mIsShutdown) {
            break;
        }
        count -= written;
        buffer = (char *) buffer + (written << mBitShift);
        // Simulate blocking I/O by sleeping at different rates, depending on a throttle.
        // The throttle tries to keep the mean pipe depth near the setpoint, with a slight jitter.
        uint32_t ns;
        if (written > 0) {
            size_t filled = (mMaxFrames - avail) + written;
            // FIXME cache these values to avoid re-computation
            if (filled <= mSetpoint / 2) {
                // pipe is (nearly) empty, fill quickly
                ns = written * ( 500000000 / Format_sampleRate(mFormat));
            } else if (filled <= (mSetpoint * 3) / 4) {
                // pipe is below setpoint, fill at slightly faster rate
                ns = written * ( 750000000 / Format_sampleRate(mFormat));
            } else if (filled <= (mSetpoint * 5) / 4) {
                // pipe is at setpoint, fill at nominal rate
                ns = written * (1000000000 / Format_sampleRate(mFormat));
            } else if (filled <= (mSetpoint * 3) / 2) {
                // pipe is above setpoint, fill at slightly slower rate
                ns = written * (1150000000 / Format_sampleRate(mFormat));
            } else if (filled <= (mSetpoint * 7) / 4) {
                // pipe is overflowing, fill slowly
                ns = written * (1350000000 / Format_sampleRate(mFormat));
            } else {
                // pipe is severely overflowing
                ns = written * (1750000000 / Format_sampleRate(mFormat));
            }
        } else {
            ns = count * (1350000000 / Format_sampleRate(mFormat));
        }
        if (ns > 999999999) {
            ns = 999999999;
        }
        struct timespec nowTs;
        bool nowTsValid = !clock_gettime(CLOCK_MONOTONIC, &nowTs);
        // deduct the elapsed time since previous write() completed
        if (nowTsValid && mWriteTsValid) {
            time_t sec = nowTs.tv_sec - mWriteTs.tv_sec;
            long nsec = nowTs.tv_nsec - mWriteTs.tv_nsec;
            ALOGE_IF(sec < 0 || (sec == 0 && nsec < 0),
                    "clock_gettime(CLOCK_MONOTONIC) failed: was %ld.%09ld but now %ld.%09ld",
                    mWriteTs.tv_sec, mWriteTs.tv_nsec, nowTs.tv_sec, nowTs.tv_nsec);
            if (nsec < 0) {
                --sec;
                nsec += 1000000000;
            }
            if (sec == 0) {
                if ((long) ns > nsec) {
                    ns -= nsec;
                } else {
                    ns = 0;
                }
            }
        }
        if (ns > 0) {
            const struct timespec req = {0, ns};
            nanosleep(&req, NULL);
        }
        // record the time that this write() completed
        if (nowTsValid) {
            mWriteTs = nowTs;
            if ((mWriteTs.tv_nsec += ns) >= 1000000000) {
                mWriteTs.tv_nsec -= 1000000000;
                ++mWriteTs.tv_sec;
            }
        }
        mWriteTsValid = nowTsValid;
    }
    mFramesWritten += totalFramesWritten;
    return totalFramesWritten;
}

void MonoPipe::setAvgFrames(size_t setpoint)
{
    mSetpoint = setpoint;
}

status_t MonoPipe::getNextWriteTimestamp(int64_t *timestamp)
{
    int32_t front;

    ALOG_ASSERT(NULL != timestamp);

    if (0 == mSamplesToLocalTime.a_to_b_denom)
        return UNKNOWN_ERROR;

    observeFrontAndNRPTS(&front, timestamp);

    if (AudioBufferProvider::kInvalidPTS != *timestamp) {
        // If we have a valid read-pointer and next read timestamp pair, then
        // use the current value of the write pointer to figure out how many
        // frames are in the buffer, and offset the timestamp by that amt.  Then
        // next time we write to the MonoPipe, the data will hit the speakers at
        // the next read timestamp plus the current amount of data in the
        // MonoPipe.
        size_t pendingFrames = (mRear - front) & (mMaxFrames - 1);
        *timestamp = offsetTimestampByAudioFrames(*timestamp, pendingFrames);
    }

    return OK;
}

void MonoPipe::updateFrontAndNRPTS(int32_t newFront, int64_t newNextRdPTS)
{
    // Set the MSB of the update sequence number to indicate that there is a
    // multi-variable update in progress.  Use an atomic store with an "acquire"
    // barrier to make sure that the next operations cannot be re-ordered and
    // take place before the change to mUpdateSeq is commited..
    int32_t tmp = mUpdateSeq | 0x80000000;
    android_atomic_acquire_store(tmp, &mUpdateSeq);

    // Update mFront and mNextRdPTS
    mFront = newFront;
    mNextRdPTS = newNextRdPTS;

    // We are finished with the update.  Compute the next sequnce number (which
    // should be the old sequence number, plus one, and with the MSB cleared)
    // and then store it in mUpdateSeq using an atomic store with a "release"
    // barrier so our update operations cannot be re-ordered past the update of
    // the sequence number.
    tmp = (tmp + 1) & 0x7FFFFFFF;
    android_atomic_release_store(tmp, &mUpdateSeq);
}

void MonoPipe::observeFrontAndNRPTS(int32_t *outFront, int64_t *outNextRdPTS)
{
    // Perform an atomic observation of mFront and mNextRdPTS.  Basically,
    // atomically observe the sequence number, then observer the variables, then
    // atomically observe the sequence number again.  If the two observations of
    // the sequence number match, and the update-in-progress bit was not set,
    // then we know we have a successful atomic observation.  Otherwise, we loop
    // around and try again.
    //
    // Note, it is very important that the observer be a lower priority thread
    // than the updater.  If the updater is lower than the observer, or they are
    // the same priority and running with SCHED_FIFO (implying that quantum
    // based premption is disabled) then we run the risk of deadlock.
    int32_t seqOne, seqTwo;

    do {
        seqOne        = android_atomic_acquire_load(&mUpdateSeq);
        *outFront     = mFront;
        *outNextRdPTS = mNextRdPTS;
        seqTwo        = android_atomic_release_load(&mUpdateSeq);
    } while ((seqOne != seqTwo) || (seqOne & 0x80000000));
}

int64_t MonoPipe::offsetTimestampByAudioFrames(int64_t ts, size_t audFrames)
{
    if (0 == mSamplesToLocalTime.a_to_b_denom)
        return AudioBufferProvider::kInvalidPTS;

    if (ts == AudioBufferProvider::kInvalidPTS)
        return AudioBufferProvider::kInvalidPTS;

    int64_t frame_lt_duration;
    if (!mSamplesToLocalTime.doForwardTransform(audFrames,
                                                &frame_lt_duration)) {
        // This should never fail, but if there is a bug which is causing it
        // to fail, this message would probably end up flooding the logs
        // because the conversion would probably fail forever.  Log the
        // error, but then zero out the ratio in the linear transform so
        // that we don't try to do any conversions from now on.  This
        // MonoPipe's getNextWriteTimestamp is now broken for good.
        ALOGE("Overflow when attempting to convert %d audio frames to"
              " duration in local time.  getNextWriteTimestamp will fail from"
              " now on.", audFrames);
        mSamplesToLocalTime.a_to_b_numer = 0;
        mSamplesToLocalTime.a_to_b_denom = 0;
        return AudioBufferProvider::kInvalidPTS;
    }

    return ts + frame_lt_duration;
}

void MonoPipe::shutdown(bool newState)
{
    mIsShutdown = newState;
}

bool MonoPipe::isShutdown()
{
    return mIsShutdown;
}

status_t MonoPipe::getTimestamp(AudioTimestamp& timestamp)
{
    if (mTimestampObserver.poll(timestamp)) {
        return OK;
    }
    return INVALID_OPERATION;
}

}   // namespace android
