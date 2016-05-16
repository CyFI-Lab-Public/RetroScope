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

#ifndef ANDROID_RS_GRALLOC_CONSUMER_H
#define ANDROID_RS_GRALLOC_CONSUMER_H

#include <gui/ConsumerBase.h>

#include <ui/GraphicBuffer.h>

#include <utils/String8.h>
#include <utils/Vector.h>
#include <utils/threads.h>


// ---------------------------------------------------------------------------
namespace android {
namespace renderscript {

class Allocation;

/**
 * CpuConsumer is a BufferQueue consumer endpoint that allows direct CPU
 * access to the underlying gralloc buffers provided by BufferQueue. Multiple
 * buffers may be acquired by it at once, to be used concurrently by the
 * CpuConsumer owner. Sets gralloc usage flags to be software-read-only.
 * This queue is synchronous by default.
 */
class GrallocConsumer : public ConsumerBase
{
  public:
    typedef ConsumerBase::FrameAvailableListener FrameAvailableListener;

    GrallocConsumer(Allocation *, const sp<IGraphicBufferConsumer>& bq);

    virtual ~GrallocConsumer();
    status_t lockNextBuffer();
    status_t unlockBuffer();

  private:
    status_t releaseAcquiredBufferLocked();
    Allocation *mAlloc;

    // Tracking for buffers acquired by the user
    struct AcquiredBuffer {
        // Need to track the original mSlot index and the buffer itself because
        // the mSlot entry may be freed/reused before the acquired buffer is
        // released.
        int mSlot;
        sp<GraphicBuffer> mGraphicBuffer;
        void *mBufferPointer;

        AcquiredBuffer() :
                mSlot(BufferQueue::INVALID_BUFFER_SLOT),
                mBufferPointer(NULL) {
        }
    };
    AcquiredBuffer mAcquiredBuffer;
};

} // namespace renderscript
} // namespace android

#endif // ANDROID_RS_GRALLOC_CONSUMER_H

