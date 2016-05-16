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

#ifndef ANDROID_SERVERS_CAMERA_CAMERA2_CAPTURESEQUENCER_H
#define ANDROID_SERVERS_CAMERA_CAMERA2_CAPTURESEQUENCER_H

#include <binder/MemoryBase.h>
#include <utils/Thread.h>
#include <utils/String16.h>
#include <utils/Vector.h>
#include <utils/Mutex.h>
#include <utils/Condition.h>
#include "camera/CameraMetadata.h"
#include "Parameters.h"
#include "FrameProcessor.h"

namespace android {

class Camera2Client;

namespace camera2 {

class ZslProcessorInterface;
class BurstCapture;

/**
 * Manages the still image capture process for
 * zero-shutter-lag, regular, and video snapshots.
 */
class CaptureSequencer:
            virtual public Thread,
            virtual public FrameProcessor::FilteredListener {
  public:
    CaptureSequencer(wp<Camera2Client> client);
    ~CaptureSequencer();

    // Get reference to the ZslProcessor, which holds the ZSL buffers and frames
    void setZslProcessor(wp<ZslProcessorInterface> processor);

    // Begin still image capture
    status_t startCapture(int msgType);

    // Wait until current image capture completes; returns immediately if no
    // capture is active. Returns TIMED_OUT if capture does not complete during
    // the specified duration.
    status_t waitUntilIdle(nsecs_t timeout);

    // Notifications about AE state changes
    void notifyAutoExposure(uint8_t newState, int triggerId);

    // Notifications from the frame processor
    virtual void onFrameAvailable(int32_t requestId, const CameraMetadata &frame);

    // Notifications from the JPEG processor
    void onCaptureAvailable(nsecs_t timestamp, sp<MemoryBase> captureBuffer);

    void dump(int fd, const Vector<String16>& args);

  private:
    /**
     * Accessed by other threads
     */
    Mutex mInputMutex;

    bool mStartCapture;
    bool mBusy;
    Condition mStartCaptureSignal;

    bool mNewAEState;
    uint8_t mAEState;
    int mAETriggerId;
    Condition mNewNotifySignal;

    bool mNewFrameReceived;
    int32_t mNewFrameId;
    CameraMetadata mNewFrame;
    Condition mNewFrameSignal;

    bool mNewCaptureReceived;
    nsecs_t mCaptureTimestamp;
    sp<MemoryBase> mCaptureBuffer;
    Condition mNewCaptureSignal;

    bool mShutterNotified;

    /**
     * Internal to CaptureSequencer
     */
    static const nsecs_t kWaitDuration = 100000000; // 100 ms
    static const int kMaxTimeoutsForPrecaptureStart = 10; // 1 sec
    static const int kMaxTimeoutsForPrecaptureEnd = 20;  // 2 sec
    static const int kMaxTimeoutsForCaptureEnd    = 40;  // 4 sec

    wp<Camera2Client> mClient;
    wp<ZslProcessorInterface> mZslProcessor;
    sp<BurstCapture> mBurstCapture;

    enum CaptureState {
        IDLE,
        START,
        ZSL_START,
        ZSL_WAITING,
        ZSL_REPROCESSING,
        STANDARD_START,
        STANDARD_PRECAPTURE_WAIT,
        STANDARD_CAPTURE,
        STANDARD_CAPTURE_WAIT,
        BURST_CAPTURE_START,
        BURST_CAPTURE_WAIT,
        DONE,
        ERROR,
        NUM_CAPTURE_STATES
    } mCaptureState;
    static const char* kStateNames[];
    int mStateTransitionCount;
    Mutex mStateMutex; // Guards mCaptureState
    Condition mStateChanged;

    typedef CaptureState (CaptureSequencer::*StateManager)(sp<Camera2Client> &client);
    static const StateManager kStateManagers[];

    CameraMetadata mCaptureRequest;

    int mTriggerId;
    int mTimeoutCount;
    bool mAeInPrecapture;

    int32_t mCaptureId;
    int mMsgType;

    // Main internal methods

    virtual bool threadLoop();

    CaptureState manageIdle(sp<Camera2Client> &client);
    CaptureState manageStart(sp<Camera2Client> &client);

    CaptureState manageZslStart(sp<Camera2Client> &client);
    CaptureState manageZslWaiting(sp<Camera2Client> &client);
    CaptureState manageZslReprocessing(sp<Camera2Client> &client);

    CaptureState manageStandardStart(sp<Camera2Client> &client);
    CaptureState manageStandardPrecaptureWait(sp<Camera2Client> &client);
    CaptureState manageStandardCapture(sp<Camera2Client> &client);
    CaptureState manageStandardCaptureWait(sp<Camera2Client> &client);

    CaptureState manageBurstCaptureStart(sp<Camera2Client> &client);
    CaptureState manageBurstCaptureWait(sp<Camera2Client> &client);

    CaptureState manageDone(sp<Camera2Client> &client);

    // Utility methods

    status_t updateCaptureRequest(const Parameters &params,
            sp<Camera2Client> &client);

    // Emit Shutter/Raw callback to java, and maybe play a shutter sound
    static void shutterNotifyLocked(const Parameters &params,
            sp<Camera2Client> client, int msgType);
};

}; // namespace camera2
}; // namespace android

#endif
