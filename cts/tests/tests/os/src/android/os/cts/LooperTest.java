/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.util.Printer;
import android.util.StringBuilderPrinter;

public class LooperTest extends AndroidTestCase {

    public static final long WAIT_TIME = 1000;

    private boolean mHasRun;

    private Looper mLooper = null;
    private boolean mHasQuit;
    private Handler mLoopHandler;

    public void testDump() {
        StringBuilderPrinter printer = new StringBuilderPrinter(new StringBuilder());
        final String prefix = "LooperTest";
        Looper.myLooper().dump(printer, prefix);
    }

    public void testGetMainLooper() {
        Looper looper = Looper.getMainLooper();
        assertNotNull(looper);
    }

    public void testLoop() throws Throwable {
        Thread t = new Thread(new Runnable() {
            public void run() {
                Looper.prepare();

                MockRunnable run = new MockRunnable();

                Handler handler = new Handler();
                Message msg = Message.obtain(handler, run);
                handler.sendMessageAtTime(msg, 0);
                assertFalse(run.runCalled);
                Looper.loop();
                assertTrue(run.runCalled);
            }
        });
        t.start();
        t.join();
    }

    public void testMyLooper() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                assertNull(Looper.myLooper());
                Looper.prepare();
                assertNotNull(Looper.myLooper());
            }
        });

        t.runTest(WAIT_TIME);
    }

    public void testMyQueue() throws Throwable {
        MessageQueue mq = Looper.myQueue();
        assertNotNull(mq);

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                try {
                    assertNull(Looper.myQueue());
                    fail("should throw exception");
                } catch (Throwable e) {
                    // expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    public void testPrepare() throws Throwable {
        try {
            Looper.prepare();
            fail("should throw exception because current thread already has a looper");
        } catch (RuntimeException e) {
            //expected
        }

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                try {
                    Looper.prepare();
                    fail("should throw exception");
                } catch (Throwable e) {
                    //expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    public void testPrepareMainLooper() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                try {
                    Looper.prepareMainLooper();
                    fail("should throw exception because the main thread was already prepared");
                } catch (Throwable e) {
                    //expected
                }
            }
        });

        t.runTest(WAIT_TIME);
    }

    public void testQuit() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                mHasQuit = false;
                Looper.prepare();
                mLoopHandler = new Handler();
                mLooper = Looper.myLooper();
                Looper.loop();
                mHasQuit = true;
            }
        });

        // Here doesn't call runTest() because we don't want to wait the runTest finish.
        // Just need to handle Looper#quit();
        t.start();
        Thread.sleep(WAIT_TIME);
        assertSame(t, mLooper.getThread());
        int time = 100;
        // Send message before Looper has quit.
        assertTrue(mLoopHandler.sendEmptyMessageAtTime(0, SystemClock.uptimeMillis() + time));
        Thread.sleep(WAIT_TIME);

        mLooper.quit();
        Thread.sleep(WAIT_TIME);
        // Send message after Looper has quit.
        assertFalse(mLoopHandler.sendEmptyMessageAtTime(1, SystemClock.uptimeMillis() + time));
        assertTrue(mHasQuit);

        t.joinAndCheck(WAIT_TIME);
    }

    public void testSetMessageLogging() throws Throwable {
        mHasRun = false;

        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();
                MockPrinter mp = new MockPrinter();
                Looper.myLooper().setMessageLogging(mp);
                MockRunnable run = new MockRunnable();

                Handler handler = new Handler();
                Message msg = Message.obtain(handler, run);
                handler.sendMessageAtTime(msg, 0);

                Looper.loop();
                assertNotNull(mp.str);
                mHasRun = true;
            }
        });

        t.runTest(WAIT_TIME);

        assertTrue(mHasRun);
    }

    public void testToString() {
        assertNotNull(Looper.myLooper().toString());
    }

    class MockPrinter implements Printer {
        public String str;

        public void println(String x) {
            str = x;
        }
    }

    private class MockRunnable implements Runnable {
        public boolean runCalled = false;

        public void run() {
            runCalled = true;
            Looper.myLooper().quit();
        }

        public void stop() {
            Looper.myLooper().quit();
        }
    }
}
