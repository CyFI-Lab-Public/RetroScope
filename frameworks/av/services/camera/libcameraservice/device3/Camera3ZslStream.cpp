/*
 * Copyright (C) 2013 The Android Open Source Project
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

#define LOG_TAG "Camera3-ZslStream"
#define ATRACE_TAG ATRACE_TAG_CAMERA
//#define LOG_NDEBUG 0

#include <utils/Log.h>
#include <utils/Trace.h>
#include "Camera3ZslStream.h"

typedef android::RingBufferConsumer::PinnedBufferItem PinnedBufferItem;

namespace android {

namespace camera3 {

namespace {
struct TimestampFinder : public RingBufferConsumer::RingBufferComparator {
    typedef RingBufferConsumer::BufferInfo BufferInfo;

    enum {
        SELECT_I1 = -1,
        SELECT_I2 = 1,
        SELECT_NEITHER = 0,
    };

    TimestampFinder(nsecs_t timestamp) : mTimestamp(timestamp) {}
    ~TimestampFinder() {}

    template <typename T>
    static void swap(T& a, T& b) {
        T tmp = a;
        a = b;
        b = tmp;
    }

    /**
     * Try to find the best candidate for a ZSL buffer.
     * Match priority from best to worst:
     *  1) Timestamps match.
     *  2) Timestamp is closest to the needle (and lower).
     *  3) Timestamp is closest to the needle (and higher).
     *
     */
    virtual int compare(const BufferInfo *i1,
                        const BufferInfo *i2) const {
        // Try to select non-null object first.
        if (i1 == NULL) {
            return SELECT_I2;
        } else if (i2 == NULL) {
            return SELECT_I1;
        }

        // Best result: timestamp is identical
        if (i1->mTimestamp == mTimestamp) {
            return SELECT_I1;
        } else if (i2->mTimestamp == mTimestamp) {
            return SELECT_I2;
        }

        const BufferInfo* infoPtrs[2] = {
            i1,
            i2
        };
        int infoSelectors[2] = {
            SELECT_I1,
            SELECT_I2
        };

        // Order i1,i2 so that always i1.timestamp < i2.timestamp
        if (i1->mTimestamp > i2->mTimestamp) {
            swap(infoPtrs[0], infoPtrs[1]);
            swap(infoSelectors[0], infoSelectors[1]);
        }

        // Second best: closest (lower) timestamp
        if (infoPtrs[1]->mTimestamp < mTimestamp) {
            return infoSelectors[1];
        } else if (infoPtrs[0]->mTimestamp < mTimestamp) {
            return infoSelectors[0];
        }

        // Worst: closest (higher) timestamp
        return infoSelectors[0];

        /**
         * The above cases should cover all the possibilities,
         * and we get an 'empty' result only if the ring buffer
         * was empty itself
         */
    }

    const nsecs_t mTimestamp;
}; // struct TimestampFinder
} // namespace anonymous

Camera3ZslStream::Camera3ZslStream(int id, uint32_t width, uint32_t height,
        int depth) :
        Camera3OutputStream(id, CAMERA3_STREAM_BIDIRECTIONAL,
                            width, height,
                            HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED),
        mDepth(depth) {

    sp<BufferQueue> bq = new BufferQueue();
    mProducer = new RingBufferConsumer(bq, GRALLOC_USAGE_HW_CAMERA_ZSL, depth);
    mConsumer = new Surface(bq);
}

Camera3ZslStream::~Camera3ZslStream() {
}

status_t Camera3ZslStream::getInputBufferLocked(camera3_stream_buffer *buffer) {
    ATRACE_CALL();

    status_t res;

    // TODO: potentially register from inputBufferLocked
    // this should be ok, registerBuffersLocked only calls getBuffer for now
    // register in output mode instead of input mode for ZSL streams.
    if (mState == STATE_IN_CONFIG || mState == STATE_IN_RECONFIG) {
        ALOGE("%s: Stream %d: Buffer registration for input streams"
              " not implemented (state %d)",
              __FUNCTION__, mId, mState);
        return INVALID_OPERATION;
    }

    if ((res = getBufferPreconditionCheckLocked()) != OK) {
        return res;
    }

    ANativeWindowBuffer* anb;
    int fenceFd;

    assert(mProducer != 0);

    sp<PinnedBufferItem> bufferItem;
    {
        List<sp<RingBufferConsumer::PinnedBufferItem> >::iterator it, end;
        it = mInputBufferQueue.begin();
        end = mInputBufferQueue.end();

        // Need to call enqueueInputBufferByTimestamp as a prerequisite
        if (it == end) {
            ALOGE("%s: Stream %d: No input buffer was queued",
                    __FUNCTION__, mId);
            return INVALID_OPERATION;
        }
        bufferItem = *it;
        mInputBufferQueue.erase(it);
    }

    anb = bufferItem->getBufferItem().mGraphicBuffer->getNativeBuffer();
    assert(anb != NULL);
    fenceFd = bufferItem->getBufferItem().mFence->dup();

    /**
     * FenceFD now owned by HAL except in case of error,
     * in which case we reassign it to acquire_fence
     */
    handoutBufferLocked(*buffer, &(anb->handle), /*acquireFence*/fenceFd,
                         /*releaseFence*/-1, CAMERA3_BUFFER_STATUS_OK);

    mBuffersInFlight.push_back(bufferItem);

    return OK;
}

status_t Camera3ZslStream::returnBufferCheckedLocked(
            const camera3_stream_buffer &buffer,
            nsecs_t timestamp,
            bool output,
            /*out*/
            sp<Fence> *releaseFenceOut) {

    if (output) {
        // Output stream path
        return Camera3OutputStream::returnBufferCheckedLocked(buffer,
                                                              timestamp,
                                                              output,
                                                              releaseFenceOut);
    }

    /**
     * Input stream path
     */
    bool bufferFound = false;
    sp<PinnedBufferItem> bufferItem;
    {
        // Find the buffer we are returning
        Vector<sp<PinnedBufferItem> >::iterator it, end;
        for (it = mBuffersInFlight.begin(), end = mBuffersInFlight.end();
             it != end;
             ++it) {

            const sp<PinnedBufferItem>& tmp = *it;
            ANativeWindowBuffer *anb =
                    tmp->getBufferItem().mGraphicBuffer->getNativeBuffer();
            if (anb != NULL && &(anb->handle) == buffer.buffer) {
                bufferFound = true;
                bufferItem = tmp;
                mBuffersInFlight.erase(it);
                break;
            }
        }
    }
    if (!bufferFound) {
        ALOGE("%s: Stream %d: Can't return buffer that wasn't sent to HAL",
              __FUNCTION__, mId);
        return INVALID_OPERATION;
    }

    int releaseFenceFd = buffer.release_fence;

    if (buffer.status == CAMERA3_BUFFER_STATUS_ERROR) {
        if (buffer.release_fence != -1) {
            ALOGE("%s: Stream %d: HAL should not set release_fence(%d) when "
                  "there is an error", __FUNCTION__, mId, buffer.release_fence);
            close(buffer.release_fence);
        }

        /**
         * Reassign release fence as the acquire fence incase of error
         */
        releaseFenceFd = buffer.acquire_fence;
    }

    /**
     * Unconditionally return buffer to the buffer queue.
     * - Fwk takes over the release_fence ownership
     */
    sp<Fence> releaseFence = new Fence(releaseFenceFd);
    bufferItem->getBufferItem().mFence = releaseFence;
    bufferItem.clear(); // dropping last reference unpins buffer

    *releaseFenceOut = releaseFence;

    return OK;
}

status_t Camera3ZslStream::returnInputBufferLocked(
        const camera3_stream_buffer &buffer) {
    ATRACE_CALL();

    status_t res = returnAnyBufferLocked(buffer, /*timestamp*/0,
                                         /*output*/false);

    return res;
}

void Camera3ZslStream::dump(int fd, const Vector<String16> &args) const {
    (void) args;

    String8 lines;
    lines.appendFormat("    Stream[%d]: ZSL\n", mId);
    write(fd, lines.string(), lines.size());

    Camera3IOStreamBase::dump(fd, args);

    lines = String8();
    lines.appendFormat("      Input buffers pending: %d, in flight %d\n",
            mInputBufferQueue.size(), mBuffersInFlight.size());
    write(fd, lines.string(), lines.size());
}

status_t Camera3ZslStream::enqueueInputBufferByTimestamp(
        nsecs_t timestamp,
        nsecs_t* actualTimestamp) {

    Mutex::Autolock l(mLock);

    TimestampFinder timestampFinder = TimestampFinder(timestamp);

    sp<RingBufferConsumer::PinnedBufferItem> pinnedBuffer =
            mProducer->pinSelectedBuffer(timestampFinder,
                                        /*waitForFence*/false);

    if (pinnedBuffer == 0) {
        ALOGE("%s: No ZSL buffers were available yet", __FUNCTION__);
        return NO_BUFFER_AVAILABLE;
    }

    nsecs_t actual = pinnedBuffer->getBufferItem().mTimestamp;

    if (actual != timestamp) {
        ALOGW("%s: ZSL buffer candidate search didn't find an exact match --"
              " requested timestamp = %lld, actual timestamp = %lld",
              __FUNCTION__, timestamp, actual);
    }

    mInputBufferQueue.push_back(pinnedBuffer);

    if (actualTimestamp != NULL) {
        *actualTimestamp = actual;
    }

    return OK;
}

status_t Camera3ZslStream::clearInputRingBuffer() {
    Mutex::Autolock l(mLock);

    mInputBufferQueue.clear();

    return mProducer->clear();
}

status_t Camera3ZslStream::setTransform(int /*transform*/) {
    ALOGV("%s: Not implemented", __FUNCTION__);
    return INVALID_OPERATION;
}

}; // namespace camera3

}; // namespace android
