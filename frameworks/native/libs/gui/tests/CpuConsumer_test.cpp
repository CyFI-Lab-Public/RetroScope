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

#define LOG_TAG "CpuConsumer_test"
//#define LOG_NDEBUG 0
//#define LOG_NNDEBUG 0

#ifdef LOG_NNDEBUG
#define ALOGVV(...) ALOGV(__VA_ARGS__)
#else
#define ALOGVV(...) ((void)0)
#endif

#include <gtest/gtest.h>
#include <gui/CpuConsumer.h>
#include <gui/Surface.h>
#include <ui/GraphicBuffer.h>
#include <utils/String8.h>
#include <utils/Thread.h>
#include <utils/Mutex.h>
#include <utils/Condition.h>

#include <ui/FramebufferNativeWindow.h>

#define CPU_CONSUMER_TEST_FORMAT_RAW 0
#define CPU_CONSUMER_TEST_FORMAT_Y8 0
#define CPU_CONSUMER_TEST_FORMAT_Y16 0
#define CPU_CONSUMER_TEST_FORMAT_RGBA_8888 1

namespace android {

struct CpuConsumerTestParams {
    uint32_t width;
    uint32_t height;
    int maxLockedBuffers;
    PixelFormat format;
};

::std::ostream& operator<<(::std::ostream& os, const CpuConsumerTestParams& p) {
    return os << "[ (" << p.width << ", " << p.height << "), B:"
              << p.maxLockedBuffers << ", F:0x"
              << ::std::hex << p.format << "]";
}

class CpuConsumerTest : public ::testing::TestWithParam<CpuConsumerTestParams> {
protected:

    virtual void SetUp() {
        const ::testing::TestInfo* const test_info =
                ::testing::UnitTest::GetInstance()->current_test_info();
        CpuConsumerTestParams params = GetParam();
        ALOGV("** Starting test %s (%d x %d, %d, 0x%x)",
                test_info->name(),
                params.width, params.height,
                params.maxLockedBuffers, params.format);
        sp<BufferQueue> bq = new BufferQueue();
        mCC = new CpuConsumer(bq, params.maxLockedBuffers);
        String8 name("CpuConsumer_Under_Test");
        mCC->setName(name);
        mSTC = new Surface(bq);
        mANW = mSTC;
    }

    virtual void TearDown() {
        mANW.clear();
        mSTC.clear();
        mCC.clear();
    }

    class FrameWaiter : public CpuConsumer::FrameAvailableListener {
    public:
        FrameWaiter():
                mPendingFrames(0) {
        }

        void waitForFrame() {
            Mutex::Autolock lock(mMutex);
            while (mPendingFrames == 0) {
                mCondition.wait(mMutex);
            }
            mPendingFrames--;
        }

        virtual void onFrameAvailable() {
            Mutex::Autolock lock(mMutex);
            mPendingFrames++;
            mCondition.signal();
        }

        int mPendingFrames;
        Mutex mMutex;
        Condition mCondition;
    };

    // Note that SurfaceTexture will lose the notifications
    // onBuffersReleased and onFrameAvailable as there is currently
    // no way to forward the events.  This DisconnectWaiter will not let the
    // disconnect finish until finishDisconnect() is called.  It will
    // also block until a disconnect is called
    class DisconnectWaiter : public BufferQueue::ConsumerListener {
    public:
        DisconnectWaiter () :
            mWaitForDisconnect(false),
            mPendingFrames(0) {
        }

        void waitForFrame() {
            Mutex::Autolock lock(mMutex);
            while (mPendingFrames == 0) {
                mFrameCondition.wait(mMutex);
            }
            mPendingFrames--;
        }

        virtual void onFrameAvailable() {
            Mutex::Autolock lock(mMutex);
            mPendingFrames++;
            mFrameCondition.signal();
        }

        virtual void onBuffersReleased() {
            Mutex::Autolock lock(mMutex);
            while (!mWaitForDisconnect) {
                mDisconnectCondition.wait(mMutex);
            }
        }

        void finishDisconnect() {
            Mutex::Autolock lock(mMutex);
            mWaitForDisconnect = true;
            mDisconnectCondition.signal();
        }

    private:
        Mutex mMutex;

        bool mWaitForDisconnect;
        Condition mDisconnectCondition;

        int mPendingFrames;
        Condition mFrameCondition;
    };

    sp<CpuConsumer> mCC;
    sp<Surface> mSTC;
    sp<ANativeWindow> mANW;
};

#define ASSERT_NO_ERROR(err, msg) \
    ASSERT_EQ(NO_ERROR, err) << msg << strerror(-err)

void checkPixel(const CpuConsumer::LockedBuffer &buf,
        uint32_t x, uint32_t y, uint32_t r, uint32_t g=0, uint32_t b=0) {
    // Ignores components that don't exist for given pixel
    switch(buf.format) {
        case HAL_PIXEL_FORMAT_RAW_SENSOR: {
            String8 msg;
            uint16_t *bPtr = (uint16_t*)buf.data;
            bPtr += y * buf.stride + x;
            // GRBG Bayer mosaic; only check the matching channel
            switch( ((y & 1) << 1) | (x & 1) ) {
                case 0: // G
                case 3: // G
                    EXPECT_EQ(g, *bPtr);
                    break;
                case 1: // R
                    EXPECT_EQ(r, *bPtr);
                    break;
                case 2: // B
                    EXPECT_EQ(b, *bPtr);
                    break;
            }
            break;
        }
        // ignores g,b
        case HAL_PIXEL_FORMAT_Y8: {
            uint8_t *bPtr = (uint8_t*)buf.data;
            bPtr += y * buf.stride + x;
            EXPECT_EQ(r, *bPtr) << "at x = " << x << " y = " << y;
            break;
        }
        // ignores g,b
        case HAL_PIXEL_FORMAT_Y16: {
            // stride is in pixels, not in bytes
            uint16_t *bPtr = ((uint16_t*)buf.data) + y * buf.stride + x;

            EXPECT_EQ(r, *bPtr) << "at x = " << x << " y = " << y;
            break;
        }
        case HAL_PIXEL_FORMAT_RGBA_8888: {
            const int bytesPerPixel = 4;
            uint8_t *bPtr = (uint8_t*)buf.data;
            bPtr += (y * buf.stride + x) * bytesPerPixel;

            EXPECT_EQ(r, bPtr[0]) << "at x = " << x << " y = " << y;
            EXPECT_EQ(g, bPtr[1]) << "at x = " << x << " y = " << y;
            EXPECT_EQ(b, bPtr[2]) << "at x = " << x << " y = " << y;
            break;
        }
        default: {
            ADD_FAILURE() << "Unknown format for check:" << buf.format;
            break;
        }
    }
}

// Fill a YV12 buffer with a multi-colored checkerboard pattern
void fillYV12Buffer(uint8_t* buf, int w, int h, int stride);

// Fill a Y8/Y16 buffer with a multi-colored checkerboard pattern
template <typename T> // T == uint8_t or uint16_t
void fillGreyscaleBuffer(T* buf, int w, int h, int stride, int bpp) {
    const int blockWidth = w > 16 ? w / 16 : 1;
    const int blockHeight = h > 16 ? h / 16 : 1;
    const int yuvTexOffsetY = 0;

    ASSERT_TRUE(bpp == 8 || bpp == 16);
    ASSERT_TRUE(sizeof(T)*8 == bpp);

    // stride is in pixels, not in bytes
    int yuvTexStrideY = stride;
    for (int x = 0; x < w; x++) {
        for (int y = 0; y < h; y++) {
            int parityX = (x / blockWidth) & 1;
            int parityY = (y / blockHeight) & 1;
            T intensity = (parityX ^ parityY) ? 63 : 191;
            buf[yuvTexOffsetY + (y * yuvTexStrideY) + x] = intensity;
        }
    }
}

inline uint8_t chooseColorRgba8888(int blockX, int blockY, uint8_t channel) {
    const int colorVariations = 3;
    uint8_t color = ((blockX % colorVariations) + (blockY % colorVariations))
                        % (colorVariations) == channel ? 191: 63;

    return color;
}

// Fill a RGBA8888 buffer with a multi-colored checkerboard pattern
void fillRgba8888Buffer(uint8_t* buf, int w, int h, int stride)
{
    const int blockWidth = w > 16 ? w / 16 : 1;
    const int blockHeight = h > 16 ? h / 16 : 1;
    const int bytesPerPixel = 4;

    // stride is in pixels, not in bytes
    for (int x = 0; x < w; ++x) {
        for (int y = 0; y < h; ++y) {
            int blockX = (x / blockWidth);
            int blockY = (y / blockHeight);

            uint8_t r = chooseColorRgba8888(blockX, blockY, 0);
            uint8_t g = chooseColorRgba8888(blockX, blockY, 1);
            uint8_t b = chooseColorRgba8888(blockX, blockY, 2);

            buf[(y*stride + x)*bytesPerPixel + 0] = r;
            buf[(y*stride + x)*bytesPerPixel + 1] = g;
            buf[(y*stride + x)*bytesPerPixel + 2] = b;
            buf[(y*stride + x)*bytesPerPixel + 3] = 255;
        }
    }
}

// Fill a RAW sensor buffer with a multi-colored checkerboard pattern.
// Assumes GRBG mosaic ordering. Result should be a grid in a 2x2 pattern
// of [ R, B; G, W]
void fillBayerRawBuffer(uint8_t* buf, int w, int h, int stride) {
    ALOGVV("fillBayerRawBuffer: %p with %d x %d, stride %d", buf, w, h ,stride);
    // Blocks need to be even-width/height, aim for 8-wide otherwise
    const int blockWidth = (w > 16 ? w / 8 : 2) & ~0x1;
    const int blockHeight = (h > 16 ? h / 8 : 2) & ~0x1;
    for (int y = 0; y < h; y+=2) {
        uint16_t *bPtr1 = ((uint16_t*)buf) + stride*y;
        uint16_t *bPtr2 = bPtr1 + stride;
        for (int x = 0; x < w; x+=2) {
            int blockX = (x / blockWidth ) & 1;
            int blockY = (y / blockHeight) & 1;
            unsigned short r = (blockX == blockY) ? 1000 : 200;
            unsigned short g = blockY ? 1000: 200;
            unsigned short b = blockX ? 1000: 200;
            // GR row
            *bPtr1++ = g;
            *bPtr1++ = r;
            // BG row
            *bPtr2++ = b;
            *bPtr2++ = g;
        }
    }

}

template<typename T> // uint8_t or uint16_t
void checkGreyscaleBuffer(const CpuConsumer::LockedBuffer &buf) {
    uint32_t w = buf.width;
    uint32_t h = buf.height;
    const int blockWidth = w > 16 ? w / 16 : 1;
    const int blockHeight = h > 16 ? h / 16 : 1;
    const int blockRows = h / blockHeight;
    const int blockCols = w / blockWidth;

    // Top-left square is bright
    checkPixel(buf, 0, 0, 191);
    checkPixel(buf, 1, 0, 191);
    checkPixel(buf, 0, 1, 191);
    checkPixel(buf, 1, 1, 191);

    // One-right square is dark
    checkPixel(buf, blockWidth,     0, 63);
    checkPixel(buf, blockWidth + 1, 0, 63);
    checkPixel(buf, blockWidth,     1, 63);
    checkPixel(buf, blockWidth + 1, 1, 63);

    // One-down square is dark
    checkPixel(buf, 0, blockHeight, 63);
    checkPixel(buf, 1, blockHeight, 63);
    checkPixel(buf, 0, blockHeight + 1, 63);
    checkPixel(buf, 1, blockHeight + 1, 63);

    // One-diag square is bright
    checkPixel(buf, blockWidth,     blockHeight, 191);
    checkPixel(buf, blockWidth + 1, blockHeight, 191);
    checkPixel(buf, blockWidth,     blockHeight + 1, 191);
    checkPixel(buf, blockWidth + 1, blockHeight + 1, 191);

    // Test bottom-right pixel
    const int maxBlockX = ((w-1 + (blockWidth-1)) / blockWidth) & 0x1;
    const int maxBlockY = ((h-1 + (blockHeight-1)) / blockHeight) & 0x1;
    uint32_t pixelValue = ((maxBlockX % 2) == (maxBlockY % 2)) ? 191 : 63;
    checkPixel(buf, w-1, h-1, pixelValue);
}

void checkRgba8888Buffer(const CpuConsumer::LockedBuffer &buf) {
    uint32_t w = buf.width;
    uint32_t h = buf.height;
    const int blockWidth = w > 16 ? w / 16 : 1;
    const int blockHeight = h > 16 ? h / 16 : 1;
    const int blockRows = h / blockHeight;
    const int blockCols = w / blockWidth;

    // Top-left square is bright red
    checkPixel(buf, 0, 0, 191, 63, 63);
    checkPixel(buf, 1, 0, 191, 63, 63);
    checkPixel(buf, 0, 1, 191, 63, 63);
    checkPixel(buf, 1, 1, 191, 63, 63);

    // One-right square is bright green
    checkPixel(buf, blockWidth,     0, 63, 191, 63);
    checkPixel(buf, blockWidth + 1, 0, 63, 191, 63);
    checkPixel(buf, blockWidth,     1, 63, 191, 63);
    checkPixel(buf, blockWidth + 1, 1, 63, 191, 63);

    // One-down square is bright green
    checkPixel(buf, 0, blockHeight, 63, 191, 63);
    checkPixel(buf, 1, blockHeight, 63, 191, 63);
    checkPixel(buf, 0, blockHeight + 1, 63, 191, 63);
    checkPixel(buf, 1, blockHeight + 1, 63, 191, 63);

    // One-diag square is bright blue
    checkPixel(buf, blockWidth,     blockHeight, 63, 63, 191);
    checkPixel(buf, blockWidth + 1, blockHeight, 63, 63, 191);
    checkPixel(buf, blockWidth,     blockHeight + 1, 63, 63, 191);
    checkPixel(buf, blockWidth + 1, blockHeight + 1, 63, 63, 191);

    // Test bottom-right pixel
    {
        const int maxBlockX = ((w-1) / blockWidth);
        const int maxBlockY = ((h-1) / blockHeight);
        uint8_t r = chooseColorRgba8888(maxBlockX, maxBlockY, 0);
        uint8_t g = chooseColorRgba8888(maxBlockX, maxBlockY, 1);
        uint8_t b = chooseColorRgba8888(maxBlockX, maxBlockY, 2);
        checkPixel(buf, w-1, h-1, r, g, b);
    }
}

void checkBayerRawBuffer(const CpuConsumer::LockedBuffer &buf) {
    uint32_t w = buf.width;
    uint32_t h = buf.height;
    const int blockWidth = (w > 16 ? w / 8 : 2) & ~0x1;
    const int blockHeight = (h > 16 ? h / 8 : 2) & ~0x1;
    const int blockRows = h / blockHeight;
    const int blockCols = w / blockWidth;

    // Top-left square is red
    checkPixel(buf, 0, 0, 1000, 200, 200);
    checkPixel(buf, 1, 0, 1000, 200, 200);
    checkPixel(buf, 0, 1, 1000, 200, 200);
    checkPixel(buf, 1, 1, 1000, 200, 200);

    // One-right square is blue
    checkPixel(buf, blockWidth,     0, 200, 200, 1000);
    checkPixel(buf, blockWidth + 1, 0, 200, 200, 1000);
    checkPixel(buf, blockWidth,     1, 200, 200, 1000);
    checkPixel(buf, blockWidth + 1, 1, 200, 200, 1000);

    // One-down square is green
    checkPixel(buf, 0, blockHeight, 200, 1000, 200);
    checkPixel(buf, 1, blockHeight, 200, 1000, 200);
    checkPixel(buf, 0, blockHeight + 1, 200, 1000, 200);
    checkPixel(buf, 1, blockHeight + 1, 200, 1000, 200);

    // One-diag square is white
    checkPixel(buf, blockWidth,     blockHeight, 1000, 1000, 1000);
    checkPixel(buf, blockWidth + 1, blockHeight, 1000, 1000, 1000);
    checkPixel(buf, blockWidth,     blockHeight + 1, 1000, 1000, 1000);
    checkPixel(buf, blockWidth + 1, blockHeight + 1, 1000, 1000, 1000);

    // Test bottom-right pixel
    const int maxBlockX = ((w-1) / blockWidth) & 0x1;
    const int maxBlockY = ((w-1) / blockHeight) & 0x1;
    unsigned short maxR = (maxBlockX == maxBlockY) ? 1000 : 200;
    unsigned short maxG = maxBlockY ? 1000: 200;
    unsigned short maxB = maxBlockX ? 1000: 200;
    checkPixel(buf, w-1, h-1, maxR, maxG, maxB);
}

void checkAnyBuffer(const CpuConsumer::LockedBuffer &buf, int format) {
    switch (format) {
        case HAL_PIXEL_FORMAT_RAW_SENSOR:
            checkBayerRawBuffer(buf);
            break;
        case HAL_PIXEL_FORMAT_Y8:
            checkGreyscaleBuffer<uint8_t>(buf);
            break;
        case HAL_PIXEL_FORMAT_Y16:
            checkGreyscaleBuffer<uint16_t>(buf);
            break;
        case HAL_PIXEL_FORMAT_RGBA_8888:
            checkRgba8888Buffer(buf);
            break;
    }
}

void fillYV12BufferRect(uint8_t* buf, int w, int h, int stride,
        const android_native_rect_t& rect);

void fillRGBA8Buffer(uint8_t* buf, int w, int h, int stride);

void fillRGBA8BufferSolid(uint8_t* buf, int w, int h, int stride, uint8_t r,
        uint8_t g, uint8_t b, uint8_t a);

// Configures the ANativeWindow producer-side interface based on test parameters
void configureANW(const sp<ANativeWindow>& anw,
        const CpuConsumerTestParams& params,
        int maxBufferSlack) {
    status_t err;
    err = native_window_set_buffers_geometry(anw.get(),
            params.width, params.height, params.format);
    ASSERT_NO_ERROR(err, "set_buffers_geometry error: ");

    err = native_window_set_usage(anw.get(),
            GRALLOC_USAGE_SW_WRITE_OFTEN);
    ASSERT_NO_ERROR(err, "set_usage error: ");

    int minUndequeuedBuffers;
    err = anw.get()->query(anw.get(),
            NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS,
            &minUndequeuedBuffers);
    ASSERT_NO_ERROR(err, "query error: ");

    ALOGVV("Setting buffer count to %d",
            maxBufferSlack + 1 + minUndequeuedBuffers);
    err = native_window_set_buffer_count(anw.get(),
            maxBufferSlack + 1 + minUndequeuedBuffers);
    ASSERT_NO_ERROR(err, "set_buffer_count error: ");

}

// Produce one frame of image data; assumes format and resolution configuration
// is already done.
void produceOneFrame(const sp<ANativeWindow>& anw,
        const CpuConsumerTestParams& params,
        int64_t timestamp, uint32_t *stride) {
    status_t err;
    ANativeWindowBuffer* anb;
    ALOGVV("Dequeue buffer from %p", anw.get());
    err = native_window_dequeue_buffer_and_wait(anw.get(), &anb);
    ASSERT_NO_ERROR(err, "dequeueBuffer error: ");

    ASSERT_TRUE(anb != NULL);

    sp<GraphicBuffer> buf(new GraphicBuffer(anb, false));

    *stride = buf->getStride();
    uint8_t* img = NULL;

    ALOGVV("Lock buffer from %p for write", anw.get());
    err = buf->lock(GRALLOC_USAGE_SW_WRITE_OFTEN, (void**)(&img));
    ASSERT_NO_ERROR(err, "lock error: ");

    switch (params.format) {
        case HAL_PIXEL_FORMAT_YV12:
            fillYV12Buffer(img, params.width, params.height, *stride);
            break;
        case HAL_PIXEL_FORMAT_RAW_SENSOR:
            fillBayerRawBuffer(img, params.width, params.height, buf->getStride());
            break;
        case HAL_PIXEL_FORMAT_Y8:
            fillGreyscaleBuffer<uint8_t>(img, params.width, params.height,
                                         buf->getStride(), /*bpp*/8);
            break;
        case HAL_PIXEL_FORMAT_Y16:
            fillGreyscaleBuffer<uint16_t>((uint16_t*)img, params.width,
                                          params.height, buf->getStride(),
                                          /*bpp*/16);
            break;
        case HAL_PIXEL_FORMAT_RGBA_8888:
            fillRgba8888Buffer(img, params.width, params.height, buf->getStride());
            break;
        default:
            FAIL() << "Unknown pixel format under test!";
            break;
    }
    ALOGVV("Unlock buffer from %p", anw.get());
    err = buf->unlock();
    ASSERT_NO_ERROR(err, "unlock error: ");

    ALOGVV("Set timestamp to %p", anw.get());
    err = native_window_set_buffers_timestamp(anw.get(), timestamp);
    ASSERT_NO_ERROR(err, "set_buffers_timestamp error: ");

    ALOGVV("Queue buffer to %p", anw.get());
    err = anw->queueBuffer(anw.get(), buf->getNativeBuffer(), -1);
    ASSERT_NO_ERROR(err, "queueBuffer error:");
};

// This test is disabled because the HAL_PIXEL_FORMAT_RAW_SENSOR format is not
// supported on all devices.
TEST_P(CpuConsumerTest, FromCpuSingle) {
    status_t err;
    CpuConsumerTestParams params = GetParam();

    // Set up

    ASSERT_NO_FATAL_FAILURE(configureANW(mANW, params, 1));

    // Produce

    const int64_t time = 12345678L;
    uint32_t stride;
    ASSERT_NO_FATAL_FAILURE(produceOneFrame(mANW, params, time,
                    &stride));

    // Consume

    CpuConsumer::LockedBuffer b;
    err = mCC->lockNextBuffer(&b);
    ASSERT_NO_ERROR(err, "getNextBuffer error: ");

    ASSERT_TRUE(b.data != NULL);
    EXPECT_EQ(params.width,  b.width);
    EXPECT_EQ(params.height, b.height);
    EXPECT_EQ(params.format, b.format);
    EXPECT_EQ(stride, b.stride);
    EXPECT_EQ(time, b.timestamp);

    checkAnyBuffer(b, GetParam().format);
    mCC->unlockBuffer(b);
}

// This test is disabled because the HAL_PIXEL_FORMAT_RAW_SENSOR format is not
// supported on all devices.
TEST_P(CpuConsumerTest, FromCpuManyInQueue) {
    status_t err;
    CpuConsumerTestParams params = GetParam();

    const int numInQueue = 5;
    // Set up

    ASSERT_NO_FATAL_FAILURE(configureANW(mANW, params, numInQueue));

    // Produce

    const int64_t time[numInQueue] = { 1L, 2L, 3L, 4L, 5L};
    uint32_t stride[numInQueue];

    for (int i = 0; i < numInQueue; i++) {
        ALOGV("Producing frame %d", i);
        ASSERT_NO_FATAL_FAILURE(produceOneFrame(mANW, params, time[i],
                        &stride[i]));
    }

    // Consume

    for (int i = 0; i < numInQueue; i++) {
        ALOGV("Consuming frame %d", i);
        CpuConsumer::LockedBuffer b;
        err = mCC->lockNextBuffer(&b);
        ASSERT_NO_ERROR(err, "getNextBuffer error: ");

        ASSERT_TRUE(b.data != NULL);
        EXPECT_EQ(params.width,  b.width);
        EXPECT_EQ(params.height, b.height);
        EXPECT_EQ(params.format, b.format);
        EXPECT_EQ(stride[i], b.stride);
        EXPECT_EQ(time[i], b.timestamp);

        checkAnyBuffer(b, GetParam().format);

        mCC->unlockBuffer(b);
    }
}

// This test is disabled because the HAL_PIXEL_FORMAT_RAW_SENSOR format is not
// supported on all devices.
TEST_P(CpuConsumerTest, FromCpuLockMax) {
    status_t err;
    CpuConsumerTestParams params = GetParam();

    // Set up

    ASSERT_NO_FATAL_FAILURE(configureANW(mANW, params, params.maxLockedBuffers + 1));

    // Produce

    const int64_t time = 1234L;
    uint32_t stride;

    for (int i = 0; i < params.maxLockedBuffers + 1; i++) {
        ALOGV("Producing frame %d", i);
        ASSERT_NO_FATAL_FAILURE(produceOneFrame(mANW, params, time,
                        &stride));
    }

    // Consume

    CpuConsumer::LockedBuffer *b = new CpuConsumer::LockedBuffer[params.maxLockedBuffers];
    for (int i = 0; i < params.maxLockedBuffers; i++) {
        ALOGV("Locking frame %d", i);
        err = mCC->lockNextBuffer(&b[i]);
        ASSERT_NO_ERROR(err, "getNextBuffer error: ");

        ASSERT_TRUE(b[i].data != NULL);
        EXPECT_EQ(params.width,  b[i].width);
        EXPECT_EQ(params.height, b[i].height);
        EXPECT_EQ(params.format, b[i].format);
        EXPECT_EQ(stride, b[i].stride);
        EXPECT_EQ(time, b[i].timestamp);

        checkAnyBuffer(b[i], GetParam().format);
    }

    ALOGV("Locking frame %d (too many)", params.maxLockedBuffers);
    CpuConsumer::LockedBuffer bTooMuch;
    err = mCC->lockNextBuffer(&bTooMuch);
    ASSERT_TRUE(err == INVALID_OPERATION) << "Allowing too many locks";

    ALOGV("Unlocking frame 0");
    err = mCC->unlockBuffer(b[0]);
    ASSERT_NO_ERROR(err, "Could not unlock buffer 0: ");

    ALOGV("Locking frame %d (should work now)", params.maxLockedBuffers);
    err = mCC->lockNextBuffer(&bTooMuch);
    ASSERT_NO_ERROR(err, "Did not allow new lock after unlock");

    ASSERT_TRUE(bTooMuch.data != NULL);
    EXPECT_EQ(params.width,  bTooMuch.width);
    EXPECT_EQ(params.height, bTooMuch.height);
    EXPECT_EQ(params.format, bTooMuch.format);
    EXPECT_EQ(stride, bTooMuch.stride);
    EXPECT_EQ(time, bTooMuch.timestamp);

    checkAnyBuffer(bTooMuch, GetParam().format);

    ALOGV("Unlocking extra buffer");
    err = mCC->unlockBuffer(bTooMuch);
    ASSERT_NO_ERROR(err, "Could not unlock extra buffer: ");

    ALOGV("Locking frame %d (no more available)", params.maxLockedBuffers + 1);
    err = mCC->lockNextBuffer(&b[0]);
    ASSERT_EQ(BAD_VALUE, err) << "Not out of buffers somehow";

    for (int i = 1; i < params.maxLockedBuffers; i++) {
        mCC->unlockBuffer(b[i]);
    }

    delete[] b;

}

CpuConsumerTestParams y8TestSets[] = {
    { 512,   512, 1, HAL_PIXEL_FORMAT_Y8},
    { 512,   512, 3, HAL_PIXEL_FORMAT_Y8},
    { 2608, 1960, 1, HAL_PIXEL_FORMAT_Y8},
    { 2608, 1960, 3, HAL_PIXEL_FORMAT_Y8},
    { 100,   100, 1, HAL_PIXEL_FORMAT_Y8},
    { 100,   100, 3, HAL_PIXEL_FORMAT_Y8},
};

CpuConsumerTestParams y16TestSets[] = {
    { 512,   512, 1, HAL_PIXEL_FORMAT_Y16},
    { 512,   512, 3, HAL_PIXEL_FORMAT_Y16},
    { 2608, 1960, 1, HAL_PIXEL_FORMAT_Y16},
    { 2608, 1960, 3, HAL_PIXEL_FORMAT_Y16},
    { 100,   100, 1, HAL_PIXEL_FORMAT_Y16},
    { 100,   100, 3, HAL_PIXEL_FORMAT_Y16},
};

CpuConsumerTestParams rawTestSets[] = {
    { 512,   512, 1, HAL_PIXEL_FORMAT_RAW_SENSOR},
    { 512,   512, 3, HAL_PIXEL_FORMAT_RAW_SENSOR},
    { 2608, 1960, 1, HAL_PIXEL_FORMAT_RAW_SENSOR},
    { 2608, 1960, 3, HAL_PIXEL_FORMAT_RAW_SENSOR},
    { 100,   100, 1, HAL_PIXEL_FORMAT_RAW_SENSOR},
    { 100,   100, 3, HAL_PIXEL_FORMAT_RAW_SENSOR},
};

CpuConsumerTestParams rgba8888TestSets[] = {
    { 512,   512, 1, HAL_PIXEL_FORMAT_RGBA_8888},
    { 512,   512, 3, HAL_PIXEL_FORMAT_RGBA_8888},
    { 2608, 1960, 1, HAL_PIXEL_FORMAT_RGBA_8888},
    { 2608, 1960, 3, HAL_PIXEL_FORMAT_RGBA_8888},
    { 100,   100, 1, HAL_PIXEL_FORMAT_RGBA_8888},
    { 100,   100, 3, HAL_PIXEL_FORMAT_RGBA_8888},
};

#if CPU_CONSUMER_TEST_FORMAT_Y8
INSTANTIATE_TEST_CASE_P(Y8Tests,
        CpuConsumerTest,
        ::testing::ValuesIn(y8TestSets));
#endif

#if CPU_CONSUMER_TEST_FORMAT_Y16
INSTANTIATE_TEST_CASE_P(Y16Tests,
        CpuConsumerTest,
        ::testing::ValuesIn(y16TestSets));
#endif

#if CPU_CONSUMER_TEST_FORMAT_RAW
INSTANTIATE_TEST_CASE_P(RawTests,
        CpuConsumerTest,
        ::testing::ValuesIn(rawTestSets));
#endif

#if CPU_CONSUMER_TEST_FORMAT_RGBA_8888
INSTANTIATE_TEST_CASE_P(Rgba8888Tests,
        CpuConsumerTest,
        ::testing::ValuesIn(rgba8888TestSets));
#endif



} // namespace android
