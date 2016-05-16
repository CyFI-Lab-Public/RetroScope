/*
 * Copyright (C) 2011 The Android Open Source Project
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

#ifndef ANDROID_SURFACE_FLINGER_EVENT_THREAD_H
#define ANDROID_SURFACE_FLINGER_EVENT_THREAD_H

#include <stdint.h>
#include <sys/types.h>

#include <gui/DisplayEventReceiver.h>
#include <gui/IDisplayEventConnection.h>

#include <utils/Errors.h>
#include <utils/threads.h>
#include <utils/SortedVector.h>

#include "DisplayDevice.h"
#include "DisplayHardware/PowerHAL.h"

// ---------------------------------------------------------------------------
namespace android {
// ---------------------------------------------------------------------------

class SurfaceFlinger;
class String8;

// ---------------------------------------------------------------------------


class VSyncSource : public virtual RefBase {
public:
    class Callback: public virtual RefBase {
    public:
        virtual ~Callback() {}
        virtual void onVSyncEvent(nsecs_t when) = 0;
    };

    virtual ~VSyncSource() {}
    virtual void setVSyncEnabled(bool enable) = 0;
    virtual void setCallback(const sp<Callback>& callback) = 0;
};

class EventThread : public Thread, private VSyncSource::Callback {
    class Connection : public BnDisplayEventConnection {
    public:
        Connection(const sp<EventThread>& eventThread);
        status_t postEvent(const DisplayEventReceiver::Event& event);

        // count >= 1 : continuous event. count is the vsync rate
        // count == 0 : one-shot event that has not fired
        // count ==-1 : one-shot event that fired this round / disabled
        int32_t count;

    private:
        virtual ~Connection();
        virtual void onFirstRef();
        virtual sp<BitTube> getDataChannel() const;
        virtual void setVsyncRate(uint32_t count);
        virtual void requestNextVsync();    // asynchronous
        sp<EventThread> const mEventThread;
        sp<BitTube> const mChannel;
    };

public:

    EventThread(const sp<VSyncSource>& src);

    sp<Connection> createEventConnection() const;
    status_t registerDisplayEventConnection(const sp<Connection>& connection);

    void setVsyncRate(uint32_t count, const sp<Connection>& connection);
    void requestNextVsync(const sp<Connection>& connection);

    // called before the screen is turned off from main thread
    void onScreenReleased();

    // called after the screen is turned on from main thread
    void onScreenAcquired();

    // called when receiving a hotplug event
    void onHotplugReceived(int type, bool connected);

    Vector< sp<EventThread::Connection> > waitForEvent(
            DisplayEventReceiver::Event* event);

    void dump(String8& result) const;

private:
    virtual bool        threadLoop();
    virtual void        onFirstRef();

    virtual void onVSyncEvent(nsecs_t timestamp);

    void removeDisplayEventConnection(const wp<Connection>& connection);
    void enableVSyncLocked();
    void disableVSyncLocked();

    // constants
    sp<VSyncSource> mVSyncSource;
    PowerHAL mPowerHAL;

    mutable Mutex mLock;
    mutable Condition mCondition;

    // protected by mLock
    SortedVector< wp<Connection> > mDisplayEventConnections;
    Vector< DisplayEventReceiver::Event > mPendingEvents;
    DisplayEventReceiver::Event mVSyncEvent[DisplayDevice::NUM_BUILTIN_DISPLAY_TYPES];
    bool mUseSoftwareVSync;
    bool mVsyncEnabled;

    // for debugging
    bool mDebugVsyncEnabled;
};

// ---------------------------------------------------------------------------

}; // namespace android

// ---------------------------------------------------------------------------

#endif /* ANDROID_SURFACE_FLINGER_EVENT_THREAD_H */
