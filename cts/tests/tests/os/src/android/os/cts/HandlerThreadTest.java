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

import junit.framework.TestCase;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

public class HandlerThreadTest extends TestCase {

    private static final int SLEEPTIME = 100;

    public void testConstructor() {
        // new the HandlerThread instance
        new HandlerThread("test");
        // new the HandlerThread instance
        new HandlerThread("test", Thread.MAX_PRIORITY);
    }

    public void testGetThreadId() {
        MockHandlerThread ht = new MockHandlerThread("test");
        assertEquals(-1, ht.getThreadId());
        ht.start();
        sleep(SLEEPTIME);

        assertEquals(ht.getMyTid(), ht.getThreadId());

        assertTrue(ht.isRunCalled());

        assertTrue(ht.isOnLooperPreparedCalled());

        assertNotNull(ht.getLooper());
        Looper looper = ht.getLooper();
        assertNotNull(looper);
        assertSame(ht.getMyLooper(), looper);
    }

    private void sleep(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
    }

    static class MockHandlerThread extends HandlerThread {

        private boolean mIsOnLooperPreparedCalled;
        private int mMyTid;
        private Looper mLooper;
        private boolean mIsRunCalled;

        public MockHandlerThread(String name) {
            super(name);
        }

        public boolean isRunCalled() {
            return mIsRunCalled;
        }

        @Override
        public void onLooperPrepared() {
            mIsOnLooperPreparedCalled = true;
            mMyTid = Process.myTid();
            mLooper = getLooper();
            super.onLooperPrepared();
        }

        @Override
        public void run() {
            mIsRunCalled = true;
            super.run();
        }

        public boolean isOnLooperPreparedCalled() {
            return mIsOnLooperPreparedCalled;
        }

        public int getMyTid() {
            return mMyTid;
        }

        public Looper getMyLooper() {
            return mLooper;
        }
    }

}
