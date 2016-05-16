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

#ifndef ANDROID_DISPSYNC_H
#define ANDROID_DISPSYNC_H

#include <stddef.h>

#include <utils/Mutex.h>
#include <utils/Timers.h>
#include <utils/RefBase.h>

namespace android {

class String8;
class Fence;
class DispSyncThread;

// DispSync maintains a model of the periodic hardware-based vsync events of a
// display and uses that model to execute period callbacks at specific phase
// offsets from the hardware vsync events.  The model is constructed by
// feeding consecutive hardware event timestamps to the DispSync object via
// the addResyncSample method.
//
// The model is validated using timestamps from Fence objects that are passed
// to the DispSync object via the addPresentFence method.  These fence
// timestamps should correspond to a hardware vsync event, but they need not
// be consecutive hardware vsync times.  If this method determines that the
// current model accurately represents the hardware event times it will return
// false to indicate that a resynchronization (via addResyncSample) is not
// needed.
class DispSync {

public:

    class Callback: public virtual RefBase {
    public:
        virtual ~Callback() {};
        virtual void onDispSyncEvent(nsecs_t when) = 0;
    };

    DispSync();
    ~DispSync();

    void reset();

    // addPresentFence adds a fence for use in validating the current vsync
    // event model.  The fence need not be signaled at the time
    // addPresentFence is called.  When the fence does signal, its timestamp
    // should correspond to a hardware vsync event.  Unlike the
    // addResyncSample method, the timestamps of consecutive fences need not
    // correspond to consecutive hardware vsync events.
    //
    // This method should be called with the retire fence from each HWComposer
    // set call that affects the display.
    bool addPresentFence(const sp<Fence>& fence);

    // The beginResync, addResyncSample, and endResync methods are used to re-
    // synchronize the DispSync's model to the hardware vsync events.  The re-
    // synchronization process involves first calling beginResync, then
    // calling addResyncSample with a sequence of consecutive hardware vsync
    // event timestamps, and finally calling endResync when addResyncSample
    // indicates that no more samples are needed by returning false.
    //
    // This resynchronization process should be performed whenever the display
    // is turned on (i.e. once immediately after it's turned on) and whenever
    // addPresentFence returns true indicating that the model has drifted away
    // from the hardware vsync events.
    void beginResync();
    bool addResyncSample(nsecs_t timestamp);
    void endResync();

    // The setPreiod method sets the vsync event model's period to a specific
    // value.  This should be used to prime the model when a display is first
    // turned on.  It should NOT be used after that.
    void setPeriod(nsecs_t period);

    // addEventListener registers a callback to be called repeatedly at the
    // given phase offset from the hardware vsync events.  The callback is
    // called from a separate thread and it should return reasonably quickly
    // (i.e. within a few hundred microseconds).
    status_t addEventListener(nsecs_t phase, const sp<Callback>& callback);

    // removeEventListener removes an already-registered event callback.  Once
    // this method returns that callback will no longer be called by the
    // DispSync object.
    status_t removeEventListener(const sp<Callback>& callback);

private:

    void updateModelLocked();
    void updateErrorLocked();
    void resetErrorLocked();

    enum { MAX_RESYNC_SAMPLES = 32 };
    enum { MIN_RESYNC_SAMPLES_FOR_UPDATE = 3 };
    enum { NUM_PRESENT_SAMPLES = 8 };
    enum { MAX_RESYNC_SAMPLES_WITHOUT_PRESENT = 12 };

    // mPeriod is the computed period of the modeled vsync events in
    // nanoseconds.
    nsecs_t mPeriod;

    // mPhase is the phase offset of the modeled vsync events.  It is the
    // number of nanoseconds from time 0 to the first vsync event.
    nsecs_t mPhase;

    // mError is the computed model error.  It is based on the difference
    // between the estimated vsync event times and those observed in the
    // mPresentTimes array.
    nsecs_t mError;

    // These member variables are the state used during the resynchronization
    // process to store information about the hardware vsync event times used
    // to compute the model.
    nsecs_t mResyncSamples[MAX_RESYNC_SAMPLES];
    size_t mFirstResyncSample;
    size_t mNumResyncSamples;
    int mNumResyncSamplesSincePresent;

    // These member variables store information about the present fences used
    // to validate the currently computed model.
    sp<Fence> mPresentFences[NUM_PRESENT_SAMPLES];
    nsecs_t mPresentTimes[NUM_PRESENT_SAMPLES];
    size_t mPresentSampleOffset;

    // mThread is the thread from which all the callbacks are called.
    sp<DispSyncThread> mThread;

    // mMutex is used to protect access to all member variables.
    mutable Mutex mMutex;
};

}

#endif // ANDROID_DISPSYNC_H
