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

package android.net.wifi.cts;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.test.AndroidTestCase;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class NsdManagerTest extends AndroidTestCase {

    private static final String TAG = "NsdManagerTest";
    private static final String SERVICE_TYPE = "_nmt._tcp";
    private static final int TIMEOUT = 2000;

    private static final boolean DBG = false;

    NsdManager mNsdManager;

    NsdManager.RegistrationListener mRegistrationListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.ResolveListener mResolveListener;

    public NsdManagerTest() {
        initRegistrationListener();
        initDiscoveryListener();
        initResolveListener();
    }

    private void initRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                setEvent("onRegistrationFailed", errorCode);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                setEvent("onUnregistrationFailed", errorCode);
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                setEvent("onServiceRegistered", serviceInfo);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                setEvent("onServiceUnregistered", serviceInfo);
            }
        };
    }

    private void initDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                setEvent("onStartDiscoveryFailed", errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                setEvent("onStopDiscoveryFailed", errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                NsdServiceInfo info = new NsdServiceInfo();
                info.setServiceType(serviceType);
                setEvent("onDiscoveryStarted", info);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                NsdServiceInfo info = new NsdServiceInfo();
                info.setServiceType(serviceType);
                setEvent("onDiscoveryStopped", info);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                setEvent("onServiceFound", serviceInfo);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                setEvent("onServiceLost", serviceInfo);
            }
        };
    }

    private void initResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                setEvent("onResolveFailed", errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                setEvent("onServiceResolved", serviceInfo);
            }
        };
    }



    private final class EventData {
        EventData(String callbackName, NsdServiceInfo info) {
            mCallbackName = callbackName;
            mSucceeded = true;
            mErrorCode = 0;
            mInfo = info;
        }
        EventData(String callbackName, int errorCode) {
            mCallbackName = callbackName;
            mSucceeded = false;
            mErrorCode = errorCode;
            mInfo = null;
        }
        private final String mCallbackName;
        private final boolean mSucceeded;
        private final int mErrorCode;
        private final NsdServiceInfo mInfo;
    }

    private final List<EventData> mEventCache = new ArrayList<EventData>();

    private void setEvent(String callbackName, int errorCode) {
        if (DBG) Log.d(TAG, callbackName + " failed with " + String.valueOf(errorCode));
        EventData eventData = new EventData(callbackName, errorCode);
        synchronized (mEventCache) {
            mEventCache.add(eventData);
            mEventCache.notify();
        }
    }

    private void setEvent(String callbackName, NsdServiceInfo info) {
        if (DBG) Log.d(TAG, "Received event " + callbackName + " for " + info.getServiceName());
        EventData eventData = new EventData(callbackName, info);
        synchronized (mEventCache) {
            mEventCache.add(eventData);
            mEventCache.notify();
        }
    }

    void clearEventCache() {
        synchronized(mEventCache) {
            mEventCache.clear();
        }
    }

    int eventCacheSize() {
        synchronized(mEventCache) {
            return mEventCache.size();
        }
    }

    private int mWaitId = 0;
    private EventData waitForCallback(String callbackName) {

        synchronized(mEventCache) {

            mWaitId ++;
            if (DBG) Log.d(TAG, "Waiting for " + callbackName + ", id=" + String.valueOf(mWaitId));

            try {
                long startTime = android.os.SystemClock.uptimeMillis();
                long elapsedTime = 0;
                int index = 0;
                while (elapsedTime < TIMEOUT ) {
                    // first check if we've received that event
                    for (; index < mEventCache.size(); index++) {
                        EventData e = mEventCache.get(index);
                        if (e.mCallbackName.equals(callbackName)) {
                            if (DBG) Log.d(TAG, "exiting wait id=" + String.valueOf(mWaitId));
                            return e;
                        }
                    }

                    // Not yet received, just wait
                    mEventCache.wait(TIMEOUT - elapsedTime);
                    elapsedTime = android.os.SystemClock.uptimeMillis() - startTime;
                }
                // we exited the loop because of TIMEOUT; fail the call
                if (DBG) Log.d(TAG, "timed out waiting id=" + String.valueOf(mWaitId));
                return null;
            } catch (InterruptedException e) {
                return null;                       // wait timed out!
            }
        }
    }

    private EventData waitForNewEvents() throws InterruptedException {
        if (DBG) Log.d(TAG, "Waiting for a bit, id=" + String.valueOf(mWaitId));

        long startTime = android.os.SystemClock.uptimeMillis();
        long elapsedTime = 0;
        synchronized (mEventCache) {
            int index = mEventCache.size();
            while (elapsedTime < TIMEOUT ) {
                // first check if we've received that event
                for (; index < mEventCache.size(); index++) {
                    EventData e = mEventCache.get(index);
                    return e;
                }

                // Not yet received, just wait
                mEventCache.wait(TIMEOUT - elapsedTime);
                elapsedTime = android.os.SystemClock.uptimeMillis() - startTime;
            }
        }

        return null;
    }

    private String mServiceName;

    @Override
    public void setUp() {
        if (DBG) Log.d(TAG, "Setup test ...");
        mNsdManager = (NsdManager) getContext().getSystemService(Context.NSD_SERVICE);

        Random rand = new Random();
        mServiceName = new String("NsdTest");
        for (int i = 0; i < 4; i++) {
            mServiceName = mServiceName + String.valueOf(rand.nextInt(10));
        }
    }

    @Override
    public void tearDown() {
        if (DBG) Log.d(TAG, "Tear down test ...");
    }

    public void runTest() throws Exception {
        NsdServiceInfo si = new NsdServiceInfo();
        si.setServiceType(SERVICE_TYPE);
        si.setServiceName(mServiceName);

        EventData lastEvent = null;

        if (DBG) Log.d(TAG, "Starting test ...");

        ServerSocket socket;
        int localPort;

        try {
            socket = new ServerSocket(0);
            localPort = socket.getLocalPort();
            si.setPort(localPort);
        } catch (IOException e) {
            if (DBG) Log.d(TAG, "Could not open a local socket");
            assertTrue(false);
            return;
        }

        if (DBG) Log.d(TAG, "Port = " + String.valueOf(localPort));

        clearEventCache();

        mNsdManager.registerService(si, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        lastEvent = waitForCallback("onServiceRegistered");                 // id = 1
        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);
        assertTrue(eventCacheSize() == 1);

        // We may not always get the name that we tried to register;
        // This events tells us the name that was registered.
        String registeredName = lastEvent.mInfo.getServiceName();
        si.setServiceName(registeredName);

        clearEventCache();

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                mDiscoveryListener);

        // Expect discovery started
        lastEvent = waitForCallback("onDiscoveryStarted");                  // id = 2

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);

        // Remove this event, so accounting becomes easier later
        synchronized (mEventCache) {
            mEventCache.remove(lastEvent);
        }

        // Expect a service record to be discovered (and filter the ones
        // that are unrelated to this test)
        boolean found = false;
        for (int i = 0; i < 32; i++) {

            lastEvent = waitForCallback("onServiceFound");                  // id = 3
            if (lastEvent == null) {
                // no more onServiceFound events are being reported!
                break;
            }

            assertTrue(lastEvent.mSucceeded);

            if (DBG) Log.d(TAG, "id = " + String.valueOf(mWaitId) + ": ServiceName = " +
                    lastEvent.mInfo.getServiceName());

            if (lastEvent.mInfo.getServiceName().equals(registeredName)) {
                // Save it, as it will get overwritten with new serviceFound events
                si = lastEvent.mInfo;
                found = true;
            }

            // Remove this event from the event cache, so it won't be found by subsequent
            // calls to waitForCallback
            synchronized (mEventCache) {
                mEventCache.remove(lastEvent);
            }
        }

        assertTrue(found);

        // We've removed all serviceFound events, and we've removed the discoveryStarted
        // event as well, so now the event cache should be empty!
        assertTrue(eventCacheSize() == 0);

        // Resolve the service
        clearEventCache();
        mNsdManager.resolveService(si, mResolveListener);
        lastEvent = waitForCallback("onServiceResolved");                   // id = 4

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);

        if (DBG) Log.d(TAG, "id = " + String.valueOf(mWaitId) + ": Port = " +
                String.valueOf(lastEvent.mInfo.getPort()));

        assertTrue(lastEvent.mInfo.getPort() == localPort);
        assertTrue(eventCacheSize() == 1);

        assertTrue(checkForAdditionalEvents());
        clearEventCache();

        // Unregister the service
        mNsdManager.unregisterService(mRegistrationListener);
        lastEvent = waitForCallback("onServiceUnregistered");               // id = 5

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);

        // Expect a callback for service lost
        lastEvent = waitForCallback("onServiceLost");                       // id = 6

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mInfo.getServiceName().equals(registeredName));

        assertTrue(eventCacheSize() == 2);

        // Register service again to see if we discover it
        checkForAdditionalEvents();
        clearEventCache();

        si = new NsdServiceInfo();
        si.setServiceType(SERVICE_TYPE);
        si.setServiceName(mServiceName);
        si.setPort(localPort);

        // Create a new registration listener and register same service again
        initRegistrationListener();

        mNsdManager.registerService(si, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

        lastEvent = waitForCallback("onServiceRegistered");                 // id = 7

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);

        registeredName = lastEvent.mInfo.getServiceName();

        // Expect a record to be discovered
        lastEvent = waitForCallback("onServiceFound");                      // id = 8

        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);

        if (DBG) Log.d(TAG, "id = " + String.valueOf(mWaitId) + ": ServiceName = " +
                lastEvent.mInfo.getServiceName());

        assertTrue(lastEvent.mInfo.getServiceName().equals(registeredName));
        assertTrue(checkCacheSize(2));

        checkForAdditionalEvents();
        clearEventCache();

        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        lastEvent = waitForCallback("onDiscoveryStopped");                  // id = 9
        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);
        assertTrue(checkCacheSize(1));

        checkForAdditionalEvents();
        clearEventCache();

        mNsdManager.unregisterService(mRegistrationListener);

        lastEvent =  waitForCallback("onServiceUnregistered");              // id = 10
        assertTrue(lastEvent != null);
        assertTrue(lastEvent.mSucceeded);
        assertTrue(checkCacheSize(1));
    }

    boolean checkCacheSize(int size) {
        synchronized (mEventCache) {
            int cacheSize = mEventCache.size();
            if (cacheSize != size) {
                Log.d(TAG, "id = " + mWaitId + ": event cache size = " + cacheSize);
                for (int i = 0; i < cacheSize; i++) {
                    EventData e = mEventCache.get(i);
                    String sname = (e.mInfo != null) ? "(" + e.mInfo.getServiceName() + ")" : "";
                    Log.d(TAG, "eventName is " + e.mCallbackName + sname);
                }
            }
            return (cacheSize == size);
        }
    }

    boolean checkForAdditionalEvents() {
        try {
            EventData e = waitForNewEvents();
            if (e != null) {
                String sname = (e.mInfo != null) ? "(" + e.mInfo.getServiceName() + ")" : "";
                Log.d(TAG, "ignoring unexpected event " + e.mCallbackName + sname);
            }
            return (e == null);
        }
        catch (InterruptedException ex) {
            return false;
        }
    }
}

