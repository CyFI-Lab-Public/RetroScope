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

#define LOG_TAG "BufferQueue_test"
//#define LOG_NDEBUG 0

#include <gtest/gtest.h>

#include <utils/String8.h>
#include <utils/threads.h>

#include <ui/GraphicBuffer.h>
#include <ui/FramebufferNativeWindow.h>

#include <gui/BufferQueue.h>

namespace android {

class BufferQueueTest : public ::testing::Test {
protected:

    BufferQueueTest() {}

    virtual void SetUp() {
        const ::testing::TestInfo* const testInfo =
            ::testing::UnitTest::GetInstance()->current_test_info();
        ALOGV("Begin test: %s.%s", testInfo->test_case_name(),
                testInfo->name());

        mBQ = new BufferQueue();
    }

    virtual void TearDown() {
        mBQ.clear();

        const ::testing::TestInfo* const testInfo =
            ::testing::UnitTest::GetInstance()->current_test_info();
        ALOGV("End test:   %s.%s", testInfo->test_case_name(),
                testInfo->name());
    }

    sp<BufferQueue> mBQ;
};

struct DummyConsumer : public BnConsumerListener {
    virtual void onFrameAvailable() {}
    virtual void onBuffersReleased() {}
};

TEST_F(BufferQueueTest, AcquireBuffer_ExceedsMaxAcquireCount_Fails) {
    sp<DummyConsumer> dc(new DummyConsumer);
    mBQ->consumerConnect(dc, false);
    IGraphicBufferProducer::QueueBufferOutput qbo;
    mBQ->connect(NULL, NATIVE_WINDOW_API_CPU, false, &qbo);
    mBQ->setBufferCount(4);

    int slot;
    sp<Fence> fence;
    sp<GraphicBuffer> buf;
    IGraphicBufferProducer::QueueBufferInput qbi(0, false, Rect(0, 0, 1, 1),
            NATIVE_WINDOW_SCALING_MODE_FREEZE, 0, false, Fence::NO_FENCE);
    BufferQueue::BufferItem item;

    for (int i = 0; i < 2; i++) {
        ASSERT_EQ(IGraphicBufferProducer::BUFFER_NEEDS_REALLOCATION,
                mBQ->dequeueBuffer(&slot, &fence, false, 1, 1, 0,
                    GRALLOC_USAGE_SW_READ_OFTEN));
        ASSERT_EQ(OK, mBQ->requestBuffer(slot, &buf));
        ASSERT_EQ(OK, mBQ->queueBuffer(slot, qbi, &qbo));
        ASSERT_EQ(OK, mBQ->acquireBuffer(&item, 0));
    }

    ASSERT_EQ(IGraphicBufferProducer::BUFFER_NEEDS_REALLOCATION,
            mBQ->dequeueBuffer(&slot, &fence, false, 1, 1, 0,
                GRALLOC_USAGE_SW_READ_OFTEN));
    ASSERT_EQ(OK, mBQ->requestBuffer(slot, &buf));
    ASSERT_EQ(OK, mBQ->queueBuffer(slot, qbi, &qbo));

    // Acquire the third buffer, which should fail.
    ASSERT_EQ(INVALID_OPERATION, mBQ->acquireBuffer(&item, 0));
}

TEST_F(BufferQueueTest, SetMaxAcquiredBufferCountWithIllegalValues_ReturnsError) {
    sp<DummyConsumer> dc(new DummyConsumer);
    mBQ->consumerConnect(dc, false);

    ASSERT_EQ(BAD_VALUE, mBQ->setMaxAcquiredBufferCount(0));
    ASSERT_EQ(BAD_VALUE, mBQ->setMaxAcquiredBufferCount(-3));
    ASSERT_EQ(BAD_VALUE, mBQ->setMaxAcquiredBufferCount(
            BufferQueue::MAX_MAX_ACQUIRED_BUFFERS+1));
    ASSERT_EQ(BAD_VALUE, mBQ->setMaxAcquiredBufferCount(100));
}

TEST_F(BufferQueueTest, SetMaxAcquiredBufferCountWithLegalValues_Succeeds) {
    sp<DummyConsumer> dc(new DummyConsumer);
    mBQ->consumerConnect(dc, false);

    ASSERT_EQ(OK, mBQ->setMaxAcquiredBufferCount(1));
    ASSERT_EQ(OK, mBQ->setMaxAcquiredBufferCount(2));
    ASSERT_EQ(OK, mBQ->setMaxAcquiredBufferCount(
            BufferQueue::MAX_MAX_ACQUIRED_BUFFERS));
}

} // namespace android
