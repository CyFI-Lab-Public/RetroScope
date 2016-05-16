/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.os.cts;


import android.app.cts.ISecondary;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.test.AndroidTestCase;

public class RemoteCallbackListTest extends AndroidTestCase {
    private static final String SERVICE_ACTION = "android.app.REMOTESERVICE";

    private ISecondary mSecondaryService = null;
    // Lock object
    private Sync mSync = new Sync();
    private Intent mIntent;
    private Context mContext;
    private ServiceConnection mSecondaryConnection;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mSecondaryConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mSecondaryService = ISecondary.Stub.asInterface(service);
                synchronized (mSync) {
                    mSync.mIsConnected = true;
                    mSync.notify();
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                mSecondaryService = null;
                synchronized (mSync) {
                    mSync.mIsDisConnected = true;
                    mSync.notify();
                }
            }
        };
        mIntent = new Intent(SERVICE_ACTION);
        assertTrue(mContext.bindService(new Intent(ISecondary.class.getName()),
                mSecondaryConnection, Context.BIND_AUTO_CREATE));

    }

    private static class Sync {
        public boolean mIsConnected;
        public boolean mIsDisConnected;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (mSecondaryConnection != null) {
            mContext.unbindService(mSecondaryConnection);
        }
        if (mIntent != null) {
            mContext.stopService(mIntent);
        }
    }

    public void testRemoteCallbackList() throws Exception {
        // Test constructor(default one).
        MockRemoteCallbackList<IInterface> rc = new MockRemoteCallbackList<IInterface>();
        synchronized (mSync) {
            if (!mSync.mIsConnected) {
                mSync.wait();
            }
        }

        try {
            rc.register(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // excepted
        }

        try {
            rc.unregister(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }

        int servicePid = mSecondaryService.getPid();
        // Test beginBroadcast, register, unregister. There is only one service binded.
        assertTrue(rc.register(mSecondaryService));
        int index = rc.beginBroadcast();
        assertEquals(1, index);
        IInterface actual = rc.getBroadcastItem(index - 1);
        assertNotNull(actual);
        assertSame(mSecondaryService, actual);
        // Test finishBroadcast(Is it valid to use rc.getBroadcastItem after finishBroadcast)
        rc.finishBroadcast();
        assertTrue(rc.unregister(mSecondaryService));

        rc.register(mSecondaryService);
        rc.beginBroadcast();
        // Process killed. No need to call finishBroadcast, unregister
        android.os.Process.killProcess(servicePid);

        synchronized (mSync) {
            if (!mSync.mIsDisConnected) {
                mSync.wait();
            }
        }
        // sleep some time to wait for onCallbackDied called.
        Thread.sleep(1000);
        // Test onCallbackDied
        assertTrue(rc.isOnCallbackDiedCalled);
    }

    public void testKill() {
        MockRemoteCallbackList<IInterface> rc = new MockRemoteCallbackList<IInterface>();
        synchronized (mSync) {
            if (!mSync.mIsConnected) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                    fail("Throw InterruptedException: " + e.getMessage());
                }
            }
        }

        rc.register(mSecondaryService);
        rc.beginBroadcast();
        rc.finishBroadcast();
        rc.kill();
        // kill() should unregister the callback (beginBroadcast()
        // should return 0) and not allow registering the service again.
        assertEquals(0, rc.beginBroadcast());
        assertFalse(rc.register(mSecondaryService));
    }

    private class MockRemoteCallbackList<E extends IInterface> extends RemoteCallbackList<E> {
        public boolean isOnCallbackDiedCalled;

        @Override
        public void onCallbackDied(E callback) {
            isOnCallbackDiedCalled = true;
            super.onCallbackDied(callback);
        }
    }
}
