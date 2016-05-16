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

#include <gtest/gtest.h>
#include <iostream>

#include <binder/IPCThreadState.h>
#include <utils/Thread.h>

#include "Camera.h"
#include "ProCamera.h"
#include <utils/Vector.h>
#include <utils/Mutex.h>
#include <utils/Condition.h>

#include <gui/SurfaceComposerClient.h>
#include <gui/Surface.h>

#include <system/camera_metadata.h>
#include <hardware/camera2.h> // for CAMERA2_TEMPLATE_PREVIEW only
#include <camera/CameraMetadata.h>

#include <camera/ICameraServiceListener.h>

namespace android {
namespace camera2 {
namespace tests {
namespace client {

#define CAMERA_ID 0
#define TEST_DEBUGGING 0

#define TEST_LISTENER_TIMEOUT 1000000000 // 1 second listener timeout
#define TEST_FORMAT HAL_PIXEL_FORMAT_Y16 //TODO: YUY2 instead

#define TEST_FORMAT_MAIN HAL_PIXEL_FORMAT_Y8
#define TEST_FORMAT_DEPTH HAL_PIXEL_FORMAT_Y16

// defaults for display "test"
#define TEST_DISPLAY_FORMAT HAL_PIXEL_FORMAT_Y8
#define TEST_DISPLAY_WIDTH 320
#define TEST_DISPLAY_HEIGHT 240

#define TEST_CPU_FRAME_COUNT 2
#define TEST_CPU_HEAP_COUNT 5

#define TEST_FRAME_PROCESSING_DELAY_US 200000 // 200 ms

#if TEST_DEBUGGING
#define dout std::cerr
#else
#define dout if (0) std::cerr
#endif

#define EXPECT_OK(x) EXPECT_EQ(OK, (x))
#define ASSERT_OK(x) ASSERT_EQ(OK, (x))

class ProCameraTest;

struct ServiceListener : public BnCameraServiceListener {

    ServiceListener() :
        mLatestStatus(STATUS_UNKNOWN),
        mPrevStatus(STATUS_UNKNOWN)
    {
    }

    void onStatusChanged(Status status, int32_t cameraId) {
        dout << "On status changed: 0x" << std::hex
             << (unsigned int) status << " cameraId " << cameraId
             << std::endl;

        Mutex::Autolock al(mMutex);

        mLatestStatus = status;
        mCondition.broadcast();
    }

    status_t waitForStatusChange(Status& newStatus) {
        Mutex::Autolock al(mMutex);

        if (mLatestStatus != mPrevStatus) {
            newStatus = mLatestStatus;
            mPrevStatus = mLatestStatus;
            return OK;
        }

        status_t stat = mCondition.waitRelative(mMutex,
                                               TEST_LISTENER_TIMEOUT);

        if (stat == OK) {
            newStatus = mLatestStatus;
            mPrevStatus = mLatestStatus;
        }

        return stat;
    }

    Condition mCondition;
    Mutex mMutex;

    Status mLatestStatus;
    Status mPrevStatus;
};

enum ProEvent {
    UNKNOWN,
    ACQUIRED,
    RELEASED,
    STOLEN,
    FRAME_RECEIVED,
    RESULT_RECEIVED,
};

inline int ProEvent_Mask(ProEvent e) {
    return (1 << static_cast<int>(e));
}

typedef Vector<ProEvent> EventList;

class ProCameraTestThread : public Thread
{
public:
    ProCameraTestThread() {
    }

    virtual bool threadLoop() {
        mProc = ProcessState::self();
        mProc->startThreadPool();

        IPCThreadState *ptr = IPCThreadState::self();

        ptr->joinThreadPool();

        return false;
    }

    sp<ProcessState> mProc;
};

class ProCameraTestListener : public ProCameraListener {

public:
    static const int EVENT_MASK_ALL = 0xFFFFFFFF;

    ProCameraTestListener() {
        mEventMask = EVENT_MASK_ALL;
        mDropFrames = false;
    }

    status_t WaitForEvent() {
        Mutex::Autolock cal(mConditionMutex);

        {
            Mutex::Autolock al(mListenerMutex);

            if (mProEventList.size() > 0) {
                return OK;
            }
        }

        return mListenerCondition.waitRelative(mConditionMutex,
                                               TEST_LISTENER_TIMEOUT);
    }

    /* Read events into out. Existing queue is flushed */
    void ReadEvents(EventList& out) {
        Mutex::Autolock al(mListenerMutex);

        for (size_t i = 0; i < mProEventList.size(); ++i) {
            out.push(mProEventList[i]);
        }

        mProEventList.clear();
    }

    /**
      * Dequeue 1 event from the event queue.
      * Returns UNKNOWN if queue is empty
      */
    ProEvent ReadEvent() {
        Mutex::Autolock al(mListenerMutex);

        if (mProEventList.size() == 0) {
            return UNKNOWN;
        }

        ProEvent ev = mProEventList[0];
        mProEventList.removeAt(0);

        return ev;
    }

    void SetEventMask(int eventMask) {
        Mutex::Autolock al(mListenerMutex);
        mEventMask = eventMask;
    }

    // Automatically acquire/release frames as they are available
    void SetDropFrames(bool dropFrames) {
        Mutex::Autolock al(mListenerMutex);
        mDropFrames = dropFrames;
    }

private:
    void QueueEvent(ProEvent ev) {
        bool eventAdded = false;
        {
            Mutex::Autolock al(mListenerMutex);

            // Drop events not part of mask
            if (ProEvent_Mask(ev) & mEventMask) {
                mProEventList.push(ev);
                eventAdded = true;
            }
        }

        if (eventAdded) {
            mListenerCondition.broadcast();
        }
    }

protected:

    //////////////////////////////////////////////////
    ///////// ProCameraListener //////////////////////
    //////////////////////////////////////////////////


    // Lock has been acquired. Write operations now available.
    virtual void onLockAcquired() {
        QueueEvent(ACQUIRED);
    }
    // Lock has been released with exclusiveUnlock
    virtual void onLockReleased() {
        QueueEvent(RELEASED);
    }

    // Lock has been stolen by another client.
    virtual void onLockStolen() {
        QueueEvent(STOLEN);
    }

    // Lock free.
    virtual void onTriggerNotify(int32_t ext1, int32_t ext2, int32_t ext3) {

        dout << "Trigger notify: " << ext1 << " " << ext2
             << " " << ext3 << std::endl;
    }

    virtual void onFrameAvailable(int streamId,
                                  const sp<CpuConsumer>& consumer) {

        QueueEvent(FRAME_RECEIVED);

        Mutex::Autolock al(mListenerMutex);
        if (mDropFrames) {
            CpuConsumer::LockedBuffer buf;
            status_t ret;

            if (OK == (ret = consumer->lockNextBuffer(&buf))) {

                dout << "Frame received on streamId = " << streamId <<
                        ", dataPtr = " << (void*)buf.data <<
                        ", timestamp = " << buf.timestamp << std::endl;

                EXPECT_OK(consumer->unlockBuffer(buf));
            }
        } else {
            dout << "Frame received on streamId = " << streamId << std::endl;
        }
    }

    virtual void onResultReceived(int32_t requestId,
                                  camera_metadata* request) {
        dout << "Result received requestId = " << requestId
             << ", requestPtr = " << (void*)request << std::endl;
        QueueEvent(RESULT_RECEIVED);
        free_camera_metadata(request);
    }

    virtual void notify(int32_t msg, int32_t ext1, int32_t ext2) {
        dout << "Notify received: msg " << std::hex << msg
             << ", ext1: " << std::hex << ext1 << ", ext2: " << std::hex << ext2
             << std::endl;
    }

    Vector<ProEvent> mProEventList;
    Mutex             mListenerMutex;
    Mutex             mConditionMutex;
    Condition         mListenerCondition;
    int               mEventMask;
    bool              mDropFrames;
};

class ProCameraTest : public ::testing::Test {

public:
    ProCameraTest() {
        char* displaySecsEnv = getenv("TEST_DISPLAY_SECS");
        if (displaySecsEnv != NULL) {
            mDisplaySecs = atoi(displaySecsEnv);
            if (mDisplaySecs < 0) {
                mDisplaySecs = 0;
            }
        } else {
            mDisplaySecs = 0;
        }

        char* displayFmtEnv = getenv("TEST_DISPLAY_FORMAT");
        if (displayFmtEnv != NULL) {
            mDisplayFmt = FormatFromString(displayFmtEnv);
        } else {
            mDisplayFmt = TEST_DISPLAY_FORMAT;
        }

        char* displayWidthEnv = getenv("TEST_DISPLAY_WIDTH");
        if (displayWidthEnv != NULL) {
            mDisplayW = atoi(displayWidthEnv);
            if (mDisplayW < 0) {
                mDisplayW = 0;
            }
        } else {
            mDisplayW = TEST_DISPLAY_WIDTH;
        }

        char* displayHeightEnv = getenv("TEST_DISPLAY_HEIGHT");
        if (displayHeightEnv != NULL) {
            mDisplayH = atoi(displayHeightEnv);
            if (mDisplayH < 0) {
                mDisplayH = 0;
            }
        } else {
            mDisplayH = TEST_DISPLAY_HEIGHT;
        }
    }

    static void SetUpTestCase() {
        // Binder Thread Pool Initialization
        mTestThread = new ProCameraTestThread();
        mTestThread->run("ProCameraTestThread");
    }

    virtual void SetUp() {
        mCamera = ProCamera::connect(CAMERA_ID);
        ASSERT_NE((void*)NULL, mCamera.get());

        mListener = new ProCameraTestListener();
        mCamera->setListener(mListener);
    }

    virtual void TearDown() {
        ASSERT_NE((void*)NULL, mCamera.get());
        mCamera->disconnect();
    }

protected:
    sp<ProCamera> mCamera;
    sp<ProCameraTestListener> mListener;

    static sp<Thread> mTestThread;

    int mDisplaySecs;
    int mDisplayFmt;
    int mDisplayW;
    int mDisplayH;

    sp<SurfaceComposerClient> mComposerClient;
    sp<SurfaceControl> mSurfaceControl;

    sp<SurfaceComposerClient> mDepthComposerClient;
    sp<SurfaceControl> mDepthSurfaceControl;

    int getSurfaceWidth() {
        return 512;
    }
    int getSurfaceHeight() {
        return 512;
    }

    void createOnScreenSurface(sp<Surface>& surface) {
        mComposerClient = new SurfaceComposerClient;
        ASSERT_EQ(NO_ERROR, mComposerClient->initCheck());

        mSurfaceControl = mComposerClient->createSurface(
                String8("ProCameraTest StreamingImage Surface"),
                getSurfaceWidth(), getSurfaceHeight(),
                PIXEL_FORMAT_RGB_888, 0);

        mSurfaceControl->setPosition(0, 0);

        ASSERT_TRUE(mSurfaceControl != NULL);
        ASSERT_TRUE(mSurfaceControl->isValid());

        SurfaceComposerClient::openGlobalTransaction();
        ASSERT_EQ(NO_ERROR, mSurfaceControl->setLayer(0x7FFFFFFF));
        ASSERT_EQ(NO_ERROR, mSurfaceControl->show());
        SurfaceComposerClient::closeGlobalTransaction();

        sp<ANativeWindow> window = mSurfaceControl->getSurface();
        surface = mSurfaceControl->getSurface();

        ASSERT_NE((void*)NULL, surface.get());
    }

    void createDepthOnScreenSurface(sp<Surface>& surface) {
        mDepthComposerClient = new SurfaceComposerClient;
        ASSERT_EQ(NO_ERROR, mDepthComposerClient->initCheck());

        mDepthSurfaceControl = mDepthComposerClient->createSurface(
                String8("ProCameraTest StreamingImage Surface"),
                getSurfaceWidth(), getSurfaceHeight(),
                PIXEL_FORMAT_RGB_888, 0);

        mDepthSurfaceControl->setPosition(640, 0);

        ASSERT_TRUE(mDepthSurfaceControl != NULL);
        ASSERT_TRUE(mDepthSurfaceControl->isValid());

        SurfaceComposerClient::openGlobalTransaction();
        ASSERT_EQ(NO_ERROR, mDepthSurfaceControl->setLayer(0x7FFFFFFF));
        ASSERT_EQ(NO_ERROR, mDepthSurfaceControl->show());
        SurfaceComposerClient::closeGlobalTransaction();

        sp<ANativeWindow> window = mDepthSurfaceControl->getSurface();
        surface = mDepthSurfaceControl->getSurface();

        ASSERT_NE((void*)NULL, surface.get());
    }

    template <typename T>
    static bool ExistsItem(T needle, T* array, size_t count) {
        if (!array) {
            return false;
        }

        for (size_t i = 0; i < count; ++i) {
            if (array[i] == needle) {
                return true;
            }
        }
        return false;
    }


    static int FormatFromString(const char* str) {
        std::string s(str);

#define CMP_STR(x, y)                               \
        if (s == #x) return HAL_PIXEL_FORMAT_ ## y;
#define CMP_STR_SAME(x) CMP_STR(x, x)

        CMP_STR_SAME( Y16);
        CMP_STR_SAME( Y8);
        CMP_STR_SAME( YV12);
        CMP_STR(NV16, YCbCr_422_SP);
        CMP_STR(NV21, YCrCb_420_SP);
        CMP_STR(YUY2, YCbCr_422_I);
        CMP_STR(RAW,  RAW_SENSOR);
        CMP_STR(RGBA, RGBA_8888);

        std::cerr << "Unknown format string " << str << std::endl;
        return -1;

    }

    /**
     * Creating a streaming request for these output streams from a template,
     *  and submit it
     */
    void createSubmitRequestForStreams(int32_t* streamIds, size_t count, int requestCount=-1) {

        ASSERT_NE((void*)NULL, streamIds);
        ASSERT_LT(0u, count);

        camera_metadata_t *requestTmp = NULL;
        EXPECT_OK(mCamera->createDefaultRequest(CAMERA2_TEMPLATE_PREVIEW,
                                                /*out*/&requestTmp));
        ASSERT_NE((void*)NULL, requestTmp);
        CameraMetadata request(requestTmp);

        // set the output streams. default is empty

        uint32_t tag = static_cast<uint32_t>(ANDROID_REQUEST_OUTPUT_STREAMS);
        request.update(tag, streamIds, count);

        requestTmp = request.release();

        if (requestCount < 0) {
            EXPECT_OK(mCamera->submitRequest(requestTmp, /*streaming*/true));
        } else {
            for (int i = 0; i < requestCount; ++i) {
                EXPECT_OK(mCamera->submitRequest(requestTmp,
                                                 /*streaming*/false));
            }
        }
        request.acquire(requestTmp);
    }
};

sp<Thread> ProCameraTest::mTestThread;

TEST_F(ProCameraTest, AvailableFormats) {
    if (HasFatalFailure()) {
        return;
    }

    CameraMetadata staticInfo = mCamera->getCameraInfo(CAMERA_ID);
    ASSERT_FALSE(staticInfo.isEmpty());

    uint32_t tag = static_cast<uint32_t>(ANDROID_SCALER_AVAILABLE_FORMATS);
    EXPECT_TRUE(staticInfo.exists(tag));
    camera_metadata_entry_t entry = staticInfo.find(tag);

    EXPECT_TRUE(ExistsItem<int32_t>(HAL_PIXEL_FORMAT_YV12,
                                                  entry.data.i32, entry.count));
    EXPECT_TRUE(ExistsItem<int32_t>(HAL_PIXEL_FORMAT_YCrCb_420_SP,
                                                  entry.data.i32, entry.count));
}

// test around exclusiveTryLock (immediate locking)
TEST_F(ProCameraTest, LockingImmediate) {

    if (HasFatalFailure()) {
        return;
    }

    mListener->SetEventMask(ProEvent_Mask(ACQUIRED) |
                            ProEvent_Mask(STOLEN)   |
                            ProEvent_Mask(RELEASED));

    EXPECT_FALSE(mCamera->hasExclusiveLock());
    EXPECT_EQ(OK, mCamera->exclusiveTryLock());
    // at this point we definitely have the lock

    EXPECT_EQ(OK, mListener->WaitForEvent());
    EXPECT_EQ(ACQUIRED, mListener->ReadEvent());

    EXPECT_TRUE(mCamera->hasExclusiveLock());
    EXPECT_EQ(OK, mCamera->exclusiveUnlock());

    EXPECT_EQ(OK, mListener->WaitForEvent());
    EXPECT_EQ(RELEASED, mListener->ReadEvent());

    EXPECT_FALSE(mCamera->hasExclusiveLock());
}

// test around exclusiveLock (locking at some future point in time)
TEST_F(ProCameraTest, LockingAsynchronous) {

    if (HasFatalFailure()) {
        return;
    }


    mListener->SetEventMask(ProEvent_Mask(ACQUIRED) |
                            ProEvent_Mask(STOLEN)   |
                            ProEvent_Mask(RELEASED));

    // TODO: Add another procamera that has a lock here.
    // then we can be test that the lock wont immediately be acquired

    EXPECT_FALSE(mCamera->hasExclusiveLock());
    EXPECT_EQ(OK, mCamera->exclusiveTryLock());
    // at this point we definitely have the lock

    EXPECT_EQ(OK, mListener->WaitForEvent());
    EXPECT_EQ(ACQUIRED, mListener->ReadEvent());

    EXPECT_TRUE(mCamera->hasExclusiveLock());
    EXPECT_EQ(OK, mCamera->exclusiveUnlock());

    EXPECT_EQ(OK, mListener->WaitForEvent());
    EXPECT_EQ(RELEASED, mListener->ReadEvent());

    EXPECT_FALSE(mCamera->hasExclusiveLock());
}

// Stream directly to the screen.
TEST_F(ProCameraTest, DISABLED_StreamingImageSingle) {
    if (HasFatalFailure()) {
        return;
    }

    sp<Surface> surface;
    if (mDisplaySecs > 0) {
        createOnScreenSurface(/*out*/surface);
    }
    else {
        dout << "Skipping, will not render to screen" << std::endl;
        return;
    }

    int depthStreamId = -1;

    sp<ServiceListener> listener = new ServiceListener();
    EXPECT_OK(ProCamera::addServiceListener(listener));

    ServiceListener::Status currentStatus;

    // when subscribing a new listener,
    // we immediately get a callback to the current status
    while (listener->waitForStatusChange(/*out*/currentStatus) != OK);
    EXPECT_EQ(ServiceListener::STATUS_PRESENT, currentStatus);

    dout << "Will now stream and resume infinitely..." << std::endl;
    while (true) {

        if (currentStatus == ServiceListener::STATUS_PRESENT) {

            ASSERT_OK(mCamera->createStream(mDisplayW, mDisplayH, mDisplayFmt,
                                            surface,
                                            &depthStreamId));
            EXPECT_NE(-1, depthStreamId);

            EXPECT_OK(mCamera->exclusiveTryLock());

            int32_t streams[] = { depthStreamId };
            ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(
                                                 streams,
                                                 /*count*/1));
        }

        ServiceListener::Status stat = ServiceListener::STATUS_UNKNOWN;

        // TODO: maybe check for getch every once in a while?
        while (listener->waitForStatusChange(/*out*/stat) != OK);

        if (currentStatus != stat) {
            if (stat == ServiceListener::STATUS_PRESENT) {
                dout << "Reconnecting to camera" << std::endl;
                mCamera = ProCamera::connect(CAMERA_ID);
            } else if (stat == ServiceListener::STATUS_NOT_AVAILABLE) {
                dout << "Disconnecting from camera" << std::endl;
                mCamera->disconnect();
            } else if (stat == ServiceListener::STATUS_NOT_PRESENT) {
                dout << "Camera unplugged" << std::endl;
                mCamera = NULL;
            } else {
                dout << "Unknown status change "
                     << std::hex << stat << std::endl;
            }

            currentStatus = stat;
        }
    }

    EXPECT_OK(ProCamera::removeServiceListener(listener));
    EXPECT_OK(mCamera->deleteStream(depthStreamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

// Stream directly to the screen.
TEST_F(ProCameraTest, DISABLED_StreamingImageDual) {
    if (HasFatalFailure()) {
        return;
    }
    sp<Surface> surface;
    sp<Surface> depthSurface;
    if (mDisplaySecs > 0) {
        createOnScreenSurface(/*out*/surface);
        createDepthOnScreenSurface(/*out*/depthSurface);
    }

    int streamId = -1;
    EXPECT_OK(mCamera->createStream(/*width*/1280, /*height*/960,
              TEST_FORMAT_MAIN, surface, &streamId));
    EXPECT_NE(-1, streamId);

    int depthStreamId = -1;
    EXPECT_OK(mCamera->createStream(/*width*/320, /*height*/240,
              TEST_FORMAT_DEPTH, depthSurface, &depthStreamId));
    EXPECT_NE(-1, depthStreamId);

    EXPECT_OK(mCamera->exclusiveTryLock());
    /*
    */
    /* iterate in a loop submitting requests every frame.
     *  what kind of requests doesnt really matter, just whatever.
     */

    // it would probably be better to use CameraMetadata from camera service.
    camera_metadata_t *request = NULL;
    EXPECT_OK(mCamera->createDefaultRequest(CAMERA2_TEMPLATE_PREVIEW,
              /*out*/&request));
    EXPECT_NE((void*)NULL, request);

    /*FIXME: dont need this later, at which point the above should become an
             ASSERT_NE*/
    if(request == NULL) request = allocate_camera_metadata(10, 100);

    // set the output streams to just this stream ID

    // wow what a verbose API.
    int32_t allStreams[] = { streamId, depthStreamId };
    // IMPORTANT. bad things will happen if its not a uint8.
    size_t streamCount = sizeof(allStreams) / sizeof(allStreams[0]);
    camera_metadata_entry_t entry;
    uint32_t tag = static_cast<uint32_t>(ANDROID_REQUEST_OUTPUT_STREAMS);
    int find = find_camera_metadata_entry(request, tag, &entry);
    if (find == -ENOENT) {
        if (add_camera_metadata_entry(request, tag, &allStreams,
                                      /*data_count*/streamCount) != OK) {
            camera_metadata_t *tmp = allocate_camera_metadata(1000, 10000);
            ASSERT_OK(append_camera_metadata(tmp, request));
            free_camera_metadata(request);
            request = tmp;

            ASSERT_OK(add_camera_metadata_entry(request, tag, &allStreams,
                                                /*data_count*/streamCount));
        }
    } else {
        ASSERT_OK(update_camera_metadata_entry(request, entry.index,
                  &allStreams, /*data_count*/streamCount, &entry));
    }

    EXPECT_OK(mCamera->submitRequest(request, /*streaming*/true));

    dout << "will sleep now for " << mDisplaySecs << std::endl;
    sleep(mDisplaySecs);

    free_camera_metadata(request);

    for (size_t i = 0; i < streamCount; ++i) {
        EXPECT_OK(mCamera->deleteStream(allStreams[i]));
    }
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, CpuConsumerSingle) {
    if (HasFatalFailure()) {
        return;
    }

    mListener->SetEventMask(ProEvent_Mask(ACQUIRED) |
                            ProEvent_Mask(STOLEN)   |
                            ProEvent_Mask(RELEASED) |
                            ProEvent_Mask(FRAME_RECEIVED));
    mListener->SetDropFrames(true);

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/320, /*height*/240,
                TEST_FORMAT_DEPTH, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());
    EXPECT_EQ(OK, mListener->WaitForEvent());
    EXPECT_EQ(ACQUIRED, mListener->ReadEvent());
    /* iterate in a loop submitting requests every frame.
     *  what kind of requests doesnt really matter, just whatever.
     */

    // it would probably be better to use CameraMetadata from camera service.
    camera_metadata_t *request = NULL;
    EXPECT_OK(mCamera->createDefaultRequest(CAMERA2_TEMPLATE_PREVIEW,
        /*out*/&request));
    EXPECT_NE((void*)NULL, request);

    /*FIXME: dont need this later, at which point the above should become an
      ASSERT_NE*/
    if(request == NULL) request = allocate_camera_metadata(10, 100);

    // set the output streams to just this stream ID

    int32_t allStreams[] = { streamId };
    camera_metadata_entry_t entry;
    uint32_t tag = static_cast<uint32_t>(ANDROID_REQUEST_OUTPUT_STREAMS);
    int find = find_camera_metadata_entry(request, tag, &entry);
    if (find == -ENOENT) {
        if (add_camera_metadata_entry(request, tag, &allStreams,
                /*data_count*/1) != OK) {
            camera_metadata_t *tmp = allocate_camera_metadata(1000, 10000);
            ASSERT_OK(append_camera_metadata(tmp, request));
            free_camera_metadata(request);
            request = tmp;

            ASSERT_OK(add_camera_metadata_entry(request, tag, &allStreams,
                /*data_count*/1));
        }
    } else {
        ASSERT_OK(update_camera_metadata_entry(request, entry.index,
            &allStreams, /*data_count*/1, &entry));
    }

    EXPECT_OK(mCamera->submitRequest(request, /*streaming*/true));

    // Consume a couple of frames
    for (int i = 0; i < TEST_CPU_FRAME_COUNT; ++i) {
        EXPECT_EQ(OK, mListener->WaitForEvent());
        EXPECT_EQ(FRAME_RECEIVED, mListener->ReadEvent());
    }

    // Done: clean up
    free_camera_metadata(request);
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, CpuConsumerDual) {
    if (HasFatalFailure()) {
        return;
    }

    mListener->SetEventMask(ProEvent_Mask(FRAME_RECEIVED));
    mListener->SetDropFrames(true);

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    int depthStreamId = -1;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/320, /*height*/240,
            TEST_FORMAT_DEPTH, TEST_CPU_HEAP_COUNT, &consumer, &depthStreamId));
    EXPECT_NE(-1, depthStreamId);

    EXPECT_OK(mCamera->exclusiveTryLock());
    /*
    */
    /* iterate in a loop submitting requests every frame.
     *  what kind of requests doesnt really matter, just whatever.
     */

    // it would probably be better to use CameraMetadata from camera service.
    camera_metadata_t *request = NULL;
    EXPECT_OK(mCamera->createDefaultRequest(CAMERA2_TEMPLATE_PREVIEW,
                                            /*out*/&request));
    EXPECT_NE((void*)NULL, request);

    if(request == NULL) request = allocate_camera_metadata(10, 100);

    // set the output streams to just this stream ID

    // wow what a verbose API.
    int32_t allStreams[] = { streamId, depthStreamId };
    size_t streamCount = 2;
    camera_metadata_entry_t entry;
    uint32_t tag = static_cast<uint32_t>(ANDROID_REQUEST_OUTPUT_STREAMS);
    int find = find_camera_metadata_entry(request, tag, &entry);
    if (find == -ENOENT) {
        if (add_camera_metadata_entry(request, tag, &allStreams,
                                      /*data_count*/streamCount) != OK) {
            camera_metadata_t *tmp = allocate_camera_metadata(1000, 10000);
            ASSERT_OK(append_camera_metadata(tmp, request));
            free_camera_metadata(request);
            request = tmp;

            ASSERT_OK(add_camera_metadata_entry(request, tag, &allStreams,
                                                   /*data_count*/streamCount));
        }
    } else {
        ASSERT_OK(update_camera_metadata_entry(request, entry.index,
                              &allStreams, /*data_count*/streamCount, &entry));
    }

    EXPECT_OK(mCamera->submitRequest(request, /*streaming*/true));

    // Consume a couple of frames
    for (int i = 0; i < TEST_CPU_FRAME_COUNT; ++i) {
        // stream id 1
        EXPECT_EQ(OK, mListener->WaitForEvent());
        EXPECT_EQ(FRAME_RECEIVED, mListener->ReadEvent());

        // stream id 2
        EXPECT_EQ(OK, mListener->WaitForEvent());
        EXPECT_EQ(FRAME_RECEIVED, mListener->ReadEvent());

        //TODO: events should be a struct with some data like the stream id
    }

    // Done: clean up
    free_camera_metadata(request);
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, ResultReceiver) {
    if (HasFatalFailure()) {
        return;
    }

    mListener->SetEventMask(ProEvent_Mask(RESULT_RECEIVED));
    mListener->SetDropFrames(true);
    //FIXME: if this is run right after the previous test we get FRAME_RECEIVED
    // need to filter out events at read time

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());
    /*
    */
    /* iterate in a loop submitting requests every frame.
     *  what kind of requests doesnt really matter, just whatever.
     */

    camera_metadata_t *request = NULL;
    EXPECT_OK(mCamera->createDefaultRequest(CAMERA2_TEMPLATE_PREVIEW,
                                            /*out*/&request));
    EXPECT_NE((void*)NULL, request);

    /*FIXME*/
    if(request == NULL) request = allocate_camera_metadata(10, 100);

    // set the output streams to just this stream ID

    int32_t allStreams[] = { streamId };
    size_t streamCount = 1;
    camera_metadata_entry_t entry;
    uint32_t tag = static_cast<uint32_t>(ANDROID_REQUEST_OUTPUT_STREAMS);
    int find = find_camera_metadata_entry(request, tag, &entry);
    if (find == -ENOENT) {
        if (add_camera_metadata_entry(request, tag, &allStreams,
                                      /*data_count*/streamCount) != OK) {
            camera_metadata_t *tmp = allocate_camera_metadata(1000, 10000);
            ASSERT_OK(append_camera_metadata(tmp, request));
            free_camera_metadata(request);
            request = tmp;

            ASSERT_OK(add_camera_metadata_entry(request, tag, &allStreams,
                                                /*data_count*/streamCount));
        }
    } else {
        ASSERT_OK(update_camera_metadata_entry(request, entry.index,
                               &allStreams, /*data_count*/streamCount, &entry));
    }

    EXPECT_OK(mCamera->submitRequest(request, /*streaming*/true));

    // Consume a couple of results
    for (int i = 0; i < TEST_CPU_FRAME_COUNT; ++i) {
        EXPECT_EQ(OK, mListener->WaitForEvent());
        EXPECT_EQ(RESULT_RECEIVED, mListener->ReadEvent());
    }

    // Done: clean up
    free_camera_metadata(request);
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

// FIXME: This is racy and sometimes fails on waitForFrameMetadata
TEST_F(ProCameraTest, DISABLED_WaitForResult) {
    if (HasFatalFailure()) {
        return;
    }

    mListener->SetDropFrames(true);

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                 TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());

    int32_t streams[] = { streamId };
    ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(streams, /*count*/1));

    // Consume a couple of results
    for (int i = 0; i < TEST_CPU_FRAME_COUNT; ++i) {
        EXPECT_OK(mCamera->waitForFrameMetadata());
        CameraMetadata meta = mCamera->consumeFrameMetadata();
        EXPECT_FALSE(meta.isEmpty());
    }

    // Done: clean up
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, WaitForSingleStreamBuffer) {
    if (HasFatalFailure()) {
        return;
    }

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                  TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());

    int32_t streams[] = { streamId };
    ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(streams, /*count*/1,
                                            /*requests*/TEST_CPU_FRAME_COUNT));

    // Consume a couple of results
    for (int i = 0; i < TEST_CPU_FRAME_COUNT; ++i) {
        EXPECT_EQ(1, mCamera->waitForFrameBuffer(streamId));

        CpuConsumer::LockedBuffer buf;
        EXPECT_OK(consumer->lockNextBuffer(&buf));

        dout << "Buffer synchronously received on streamId = " << streamId <<
                ", dataPtr = " << (void*)buf.data <<
                ", timestamp = " << buf.timestamp << std::endl;

        EXPECT_OK(consumer->unlockBuffer(buf));
    }

    // Done: clean up
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

// FIXME: This is racy and sometimes fails on waitForFrameMetadata
TEST_F(ProCameraTest, DISABLED_WaitForDualStreamBuffer) {
    if (HasFatalFailure()) {
        return;
    }

    const int REQUEST_COUNT = TEST_CPU_FRAME_COUNT * 10;

    // 15 fps
    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                 TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    // 30 fps
    int depthStreamId = -1;
    sp<CpuConsumer> depthConsumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/320, /*height*/240,
       TEST_FORMAT_DEPTH, TEST_CPU_HEAP_COUNT, &depthConsumer, &depthStreamId));
    EXPECT_NE(-1, depthStreamId);

    EXPECT_OK(mCamera->exclusiveTryLock());

    int32_t streams[] = { streamId, depthStreamId };
    ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(streams, /*count*/2,
                                                    /*requests*/REQUEST_COUNT));

    int depthFrames = 0;
    int greyFrames = 0;

    // Consume two frames simultaneously. Unsynchronized by timestamps.
    for (int i = 0; i < REQUEST_COUNT; ++i) {

        // Exhaust event queue so it doesn't keep growing
        while (mListener->ReadEvent() != UNKNOWN);

        // Get the metadata
        EXPECT_OK(mCamera->waitForFrameMetadata());
        CameraMetadata meta = mCamera->consumeFrameMetadata();
        EXPECT_FALSE(meta.isEmpty());

        // Get the buffers

        EXPECT_EQ(1, mCamera->waitForFrameBuffer(depthStreamId));

        /**
          * Guaranteed to be able to consume the depth frame,
          * since we waited on it.
          */
        CpuConsumer::LockedBuffer depthBuffer;
        EXPECT_OK(depthConsumer->lockNextBuffer(&depthBuffer));

        dout << "Depth Buffer synchronously received on streamId = " <<
                streamId <<
                ", dataPtr = " << (void*)depthBuffer.data <<
                ", timestamp = " << depthBuffer.timestamp << std::endl;

        EXPECT_OK(depthConsumer->unlockBuffer(depthBuffer));

        depthFrames++;


        /** Consume Greyscale frames if there are any.
          * There may not be since it runs at half FPS */
        CpuConsumer::LockedBuffer greyBuffer;
        while (consumer->lockNextBuffer(&greyBuffer) == OK) {

            dout << "GRAY Buffer synchronously received on streamId = " <<
                streamId <<
                ", dataPtr = " << (void*)greyBuffer.data <<
                ", timestamp = " << greyBuffer.timestamp << std::endl;

            EXPECT_OK(consumer->unlockBuffer(greyBuffer));

            greyFrames++;
        }
    }

    dout << "Done, summary: depth frames " << std::dec << depthFrames
         << ", grey frames " << std::dec << greyFrames << std::endl;

    // Done: clean up
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, WaitForSingleStreamBufferAndDropFramesSync) {
    if (HasFatalFailure()) {
        return;
    }

    const int NUM_REQUESTS = 20 * TEST_CPU_FRAME_COUNT;

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                  TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT,
                  /*synchronousMode*/true, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());

    int32_t streams[] = { streamId };
    ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(streams, /*count*/1,
                                                     /*requests*/NUM_REQUESTS));

    // Consume a couple of results
    for (int i = 0; i < NUM_REQUESTS; ++i) {
        int numFrames;
        EXPECT_TRUE((numFrames = mCamera->waitForFrameBuffer(streamId)) > 0);

        // Drop all but the newest framebuffer
        EXPECT_EQ(numFrames-1, mCamera->dropFrameBuffer(streamId, numFrames-1));

        dout << "Dropped " << (numFrames - 1) << " frames" << std::endl;

        // Skip the counter ahead, don't try to consume these frames again
        i += numFrames-1;

        // "Consume" the buffer
        CpuConsumer::LockedBuffer buf;
        EXPECT_OK(consumer->lockNextBuffer(&buf));

        dout << "Buffer synchronously received on streamId = " << streamId <<
                ", dataPtr = " << (void*)buf.data <<
                ", timestamp = " << buf.timestamp << std::endl;

        // Process at 10fps, stream is at 15fps.
        // This means we will definitely fill up the buffer queue with
        // extra buffers and need to drop them.
        usleep(TEST_FRAME_PROCESSING_DELAY_US);

        EXPECT_OK(consumer->unlockBuffer(buf));
    }

    // Done: clean up
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}

TEST_F(ProCameraTest, WaitForSingleStreamBufferAndDropFramesAsync) {
    if (HasFatalFailure()) {
        return;
    }

    const int NUM_REQUESTS = 20 * TEST_CPU_FRAME_COUNT;

    int streamId = -1;
    sp<CpuConsumer> consumer;
    EXPECT_OK(mCamera->createStreamCpu(/*width*/1280, /*height*/960,
                  TEST_FORMAT_MAIN, TEST_CPU_HEAP_COUNT,
                  /*synchronousMode*/false, &consumer, &streamId));
    EXPECT_NE(-1, streamId);

    EXPECT_OK(mCamera->exclusiveTryLock());

    int32_t streams[] = { streamId };
    ASSERT_NO_FATAL_FAILURE(createSubmitRequestForStreams(streams, /*count*/1,
                                                     /*requests*/NUM_REQUESTS));

    uint64_t lastFrameNumber = 0;
    int numFrames;

    // Consume a couple of results
    int i;
    for (i = 0; i < NUM_REQUESTS && lastFrameNumber < NUM_REQUESTS; ++i) {
        EXPECT_LT(0, (numFrames = mCamera->waitForFrameBuffer(streamId)));

        dout << "Dropped " << (numFrames - 1) << " frames" << std::endl;

        // Skip the counter ahead, don't try to consume these frames again
        i += numFrames-1;

        // "Consume" the buffer
        CpuConsumer::LockedBuffer buf;

        EXPECT_EQ(OK, consumer->lockNextBuffer(&buf));

        lastFrameNumber = buf.frameNumber;

        dout << "Buffer asynchronously received on streamId = " << streamId <<
                ", dataPtr = " << (void*)buf.data <<
                ", timestamp = " << buf.timestamp <<
                ", framenumber = " << buf.frameNumber << std::endl;

        // Process at 10fps, stream is at 15fps.
        // This means we will definitely fill up the buffer queue with
        // extra buffers and need to drop them.
        usleep(TEST_FRAME_PROCESSING_DELAY_US);

        EXPECT_OK(consumer->unlockBuffer(buf));
    }

    dout << "Done after " << i << " iterations " << std::endl;

    // Done: clean up
    EXPECT_OK(mCamera->deleteStream(streamId));
    EXPECT_OK(mCamera->exclusiveUnlock());
}



//TODO: refactor into separate file
TEST_F(ProCameraTest, ServiceListenersSubscribe) {

    ASSERT_EQ(4u, sizeof(ServiceListener::Status));

    sp<ServiceListener> listener = new ServiceListener();

    EXPECT_EQ(BAD_VALUE, ProCamera::removeServiceListener(listener));
    EXPECT_OK(ProCamera::addServiceListener(listener));

    EXPECT_EQ(ALREADY_EXISTS, ProCamera::addServiceListener(listener));
    EXPECT_OK(ProCamera::removeServiceListener(listener));

    EXPECT_EQ(BAD_VALUE, ProCamera::removeServiceListener(listener));
}

//TODO: refactor into separate file
TEST_F(ProCameraTest, ServiceListenersFunctional) {

    sp<ServiceListener> listener = new ServiceListener();

    EXPECT_OK(ProCamera::addServiceListener(listener));

    sp<Camera> cam = Camera::connect(CAMERA_ID,
                                     /*clientPackageName*/String16(),
                                     -1);
    EXPECT_NE((void*)NULL, cam.get());

    ServiceListener::Status stat = ServiceListener::STATUS_UNKNOWN;
    EXPECT_OK(listener->waitForStatusChange(/*out*/stat));

    EXPECT_EQ(ServiceListener::STATUS_NOT_AVAILABLE, stat);

    if (cam.get()) {
        cam->disconnect();
    }

    EXPECT_OK(listener->waitForStatusChange(/*out*/stat));
    EXPECT_EQ(ServiceListener::STATUS_PRESENT, stat);

    EXPECT_OK(ProCamera::removeServiceListener(listener));
}



}
}
}
}
