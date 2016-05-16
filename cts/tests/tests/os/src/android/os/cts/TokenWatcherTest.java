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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.TokenWatcher;
import android.test.AndroidTestCase;

public class TokenWatcherTest extends AndroidTestCase {

    private static final String TAG = "TokenWatcherTest";
    private static final String EMPTY_SERVICE = "android.os.REMOTESERVICE";
    private static final int OVERTIME = 2000;
    private static final int DELAY = 500;
    private MockTokenWatcher mMockTokenWatcher;
    private Handler mHandler;
    private ServiceConnection mServiceConnection;
    private Intent mIntent;
    private IEmptyService mEmptyService;
    private Object mSync;
    private boolean mHasConnected;
    private boolean mHasDisconnected;
    private boolean mLooped;

    private Looper mLooper;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        new Thread() {
            public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLooped = true;
                Looper.loop();
            }
        }.start();

        while (!mLooped) {
            Thread.sleep(50);
        }

        mHandler = new Handler(mLooper);
        mSync = new Object();
        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                mEmptyService = IEmptyService.Stub.asInterface(service);
                synchronized (mSync) {
                    mHasConnected = true;
                    mSync.notify();
                }
            }
            public void onServiceDisconnected(ComponentName className) {
                mEmptyService = null;
                synchronized (mSync) {
                    mHasDisconnected = true;
                    mSync.notify();
                }
            }
        };
        mIntent = new Intent(EMPTY_SERVICE);
        getContext().startService(mIntent);
        getContext().bindService(new Intent(IEmptyService.class.getName()),
                mServiceConnection, Context.BIND_AUTO_CREATE);
        synchronized (mSync) {
            if (!mHasConnected) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mLooper.quit();
        if (!mHasDisconnected) {
            getContext().unbindService(mServiceConnection);
        }
        if (mIntent != null) {
            getContext().stopService(mIntent);
        }
    }

    public void testTokenWatcher() throws RemoteException, InterruptedException {
        // new the MockTokenWatcher instance, test constructor
        IBinder token = mEmptyService.getToken();
        mMockTokenWatcher = new MockTokenWatcher(mHandler, TAG);
        assertFalse(mMockTokenWatcher.isAcquired());
        assertFalse(mMockTokenWatcher.isAcquiredCalled);
        assertFalse(mMockTokenWatcher.isReleasedCalled);

        // test acquire() and acquired()
        mMockTokenWatcher.acquire(token, TAG);
        assertTrue(mMockTokenWatcher.isAcquired());
        assertTrue(waitUntilAcquired());
        // this time acquired() will not be called
        mMockTokenWatcher.isAcquiredCalled = false;
        mMockTokenWatcher.acquire(token, TAG);
        assertTrue(mMockTokenWatcher.isAcquired());
        assertFalse(waitUntilAcquired());

        // test release() and released()
        synchronized (mMockTokenWatcher) {
            mMockTokenWatcher.release(token);
            assertFalse(mMockTokenWatcher.isAcquired());
        }
        assertTrue(waitUntilReleased());
        // this time released() will not be called
        mMockTokenWatcher.isReleasedCalled =false;
        mMockTokenWatcher.release(token);
        assertFalse(mMockTokenWatcher.isAcquired());
        assertFalse(waitUntilReleased());
    }

    public void testCleanUp() throws RemoteException, InterruptedException {
        IBinder token = mEmptyService.getToken();
        mMockTokenWatcher = new MockTokenWatcher(mHandler, TAG);
        assertFalse(mMockTokenWatcher.isAcquired());
        assertFalse(mMockTokenWatcher.isAcquiredCalled);
        assertFalse(mMockTokenWatcher.isReleasedCalled);

        // test dump() and cleanup()
        mMockTokenWatcher.acquire(token, TAG);
        assertTrue(mMockTokenWatcher.isAcquired());
        assertTrue(waitUntilAcquired());
        mMockTokenWatcher.dump();
        synchronized (mMockTokenWatcher) {
            mMockTokenWatcher.cleanup(token, true);
            assertFalse(mMockTokenWatcher.isAcquired());
        }
        assertTrue(waitUntilReleased());
    }

    private boolean waitUntilAcquired() throws InterruptedException {
        long time = System.currentTimeMillis();
        while (System.currentTimeMillis() - time < OVERTIME) {
            if (mMockTokenWatcher.isAcquiredCalled) {
                return true;
            }
            Thread.sleep(DELAY);
        }
        return false;
    }

    private boolean waitUntilReleased() throws InterruptedException {
        long time = System.currentTimeMillis();
        while (System.currentTimeMillis() - time < OVERTIME) {
            if (mMockTokenWatcher.isReleasedCalled) {
                return true;
            }
            Thread.sleep(DELAY);
        }
        return false;
    }

    private static class MockTokenWatcher extends TokenWatcher {

        public boolean isAcquiredCalled;
        public boolean isReleasedCalled;

        public MockTokenWatcher(Handler h, String tag) {
            super(h, tag);
        }

        @Override
        public void acquired() {
            isAcquiredCalled = true;
        }

        @Override
        public synchronized void released() {
            isReleasedCalled = true;
        }
    }
}
