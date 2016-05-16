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


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.os.MessageQueue.IdleHandler;
import android.test.AndroidTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageQueueTest extends AndroidTestCase {

    private static final long TIMEOUT = 1000;

    public void testAddIdleHandler() throws InterruptedException {
        TestLooperThread looperThread = new TestLooperThread(Test.ADD_IDLE_HANDLER);
        looperThread.start();

        try {
            if (!looperThread.hasIdleHandlerBeenCalled()) {
                fail("IdleHandler#queueIdle was NOT called: " + looperThread.getTestProgress());
            }
        } finally {
            assertTrue("The looper should have been running.", looperThread.quit());
        }
    }

    public void testRemoveIdleHandler() throws InterruptedException {
        TestLooperThread looperThread = new TestLooperThread(Test.REMOVE_IDLE_HANDLER);
        looperThread.start();

        try {
            if (looperThread.hasIdleHandlerBeenCalled()) {
                fail("IdleHandler#queueIdle was called: " + looperThread.getTestProgress());
            }
        } finally {
            assertTrue("The looper should have been running.", looperThread.quit());
        }
    }

    private enum Test {ADD_IDLE_HANDLER, REMOVE_IDLE_HANDLER};

    /**
     * {@link HandlerThread} that adds or removes an idle handler depending on the {@link Test}
     * given. It uses a {@link CountDownLatch} with an initial count of 2. The first count down
     * occurs right before the looper's run thread had started running. The final count down
     * occurs when the idle handler was executed. Tests can call {@link #hasIdleHandlerBeenCalled()}
     * to see if the countdown reached to 0 or not.
     */
    private static class TestLooperThread extends HandlerThread {

        private final Test mTestMode;

        private final CountDownLatch mIdleLatch = new CountDownLatch(2);

        TestLooperThread(Test testMode) {
            super("TestLooperThread");
            mTestMode = testMode;
        }

        @Override
        protected void onLooperPrepared() {
            super.onLooperPrepared();

            IdleHandler idleHandler = new IdleHandler() {
                public boolean queueIdle() {
                    mIdleLatch.countDown();
                    return false;
                }
            };

            if (mTestMode == Test.ADD_IDLE_HANDLER) {
                Looper.myQueue().addIdleHandler(idleHandler);
            } else {
                Looper.myQueue().addIdleHandler(idleHandler);
                Looper.myQueue().removeIdleHandler(idleHandler);
            }
        }

        @Override
        public void run() {
            mIdleLatch.countDown();
            super.run();
        }

        public boolean hasIdleHandlerBeenCalled() throws InterruptedException {
            return mIdleLatch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        public long getTestProgress() {
            return mIdleLatch.getCount();
        }
    }

    /**
     * Use MessageQueue, send message by order
     */
    public void testMessageOrder() throws Exception {

        OrderTestHelper tester = new OrderTestHelper() {
            public void init() {
                super.init();
                long now = SystemClock.uptimeMillis() + 200;
                mLastMessage = 4;

                mHandler.sendMessageAtTime(mHandler.obtainMessage(2), now + 1);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(3), now + 2);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(4), now + 2);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(0), now + 0);
                mHandler.sendMessageAtTime(mHandler.obtainMessage(1), now + 0);
            }

        };
        tester.doTest(1000, 50);
    }

    /**
     * Use MessageQueue, send message at front of queue.
     */
    public void testAtFrontOfQueue() throws Exception {

        OrderTestHelper tester = new OrderTestHelper() {

            public void init() {
                super.init();
                long now = SystemClock.uptimeMillis() + 200;
                mLastMessage = 3;
                mHandler.sendMessageAtTime(mHandler.obtainMessage(3), now);
                mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(2));
                mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(0));
            }

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0) {
                    mHandler.sendMessageAtFrontOfQueue(mHandler.obtainMessage(1));
                }
            }
        };

        tester.doTest(1000, 50);
    }

    public void testSyncBarriers() throws Exception {
        OrderTestHelper tester = new OrderTestHelper() {
            private int mBarrierToken1;
            private int mBarrierToken2;

            public void init() {
                super.init();
                mLastMessage = 10;
                mHandler.sendEmptyMessage(0);
                mBarrierToken1 = Looper.myLooper().postSyncBarrier();
                mHandler.sendEmptyMessage(5);
                sendAsyncMessage(1);
                sendAsyncMessage(2);
                sendAsyncMessage(3);
                mHandler.sendEmptyMessage(6);
            }

            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 3) {
                    mHandler.sendEmptyMessage(7);
                    mBarrierToken2 = Looper.myLooper().postSyncBarrier();
                    sendAsyncMessage(4);
                    sendAsyncMessage(8);
                } else if (msg.what == 4) {
                    Looper.myLooper().removeSyncBarrier(mBarrierToken1);
                    sendAsyncMessage(9);
                    mHandler.sendEmptyMessage(10);
                } else if (msg.what == 8) {
                    Looper.myLooper().removeSyncBarrier(mBarrierToken2);
                }
            }

            private void sendAsyncMessage(int what) {
                Message msg = mHandler.obtainMessage(what);
                msg.setAsynchronous(true);
                mHandler.sendMessage(msg);
            }
        };

        tester.doTest(1000, 50);
    }

    public void testReleaseSyncBarrierThrowsIfTokenNotValid() throws Exception {
        // Invalid token
        try {
            Looper.myLooper().removeSyncBarrier(-1);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }

        // Token already removed.
        int barrierToken = Looper.myLooper().postSyncBarrier();
        Looper.myLooper().removeSyncBarrier(barrierToken);
        try {
            Looper.myLooper().removeSyncBarrier(barrierToken);
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    /**
     * Helper class used to test sending message to message queue.
     */
    private class OrderTestHelper {
        Handler mHandler;
        int mLastMessage;
        int mCount;
        private boolean mSuccess;
        private RuntimeException mFailure;
        private boolean mDone;
        private Looper mLooper;

        public void init() {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    OrderTestHelper.this.handleMessage(msg);
                }
            };
        }

        public void handleMessage(Message msg) {
            if (mCount <= mLastMessage) {
                if (msg.what != mCount) {
                    failure(new RuntimeException("Expected message #" + mCount + ", received #"
                            + msg.what));
                } else if (mCount == mLastMessage) {
                    success();
                }

                mCount++;
            } else {
                failure(new RuntimeException("Message received after done, #" + msg.what));
            }
        }

        public void doTest(long timeout, long interval) throws InterruptedException {
            (new LooperThread()).start();

            synchronized (this) {
                long now = System.currentTimeMillis();
                long endTime = now + timeout;
                while (!mDone && now < endTime) {
                    wait(interval);
                    now = System.currentTimeMillis();
                }
            }

            mLooper.quit();

            if (!mDone) {
                throw new RuntimeException("test timed out");
            }
            if (!mSuccess) {
                throw mFailure;
            }
        }

        class LooperThread extends HandlerThread {

            public LooperThread() {
                super("MessengerLooperThread");
            }

            public void onLooperPrepared() {
                init();
                mLooper = getLooper();
            }

            @Override
            public void run() {
                super.run();
                synchronized (OrderTestHelper.this) {
                    mDone = true;
                    if (!mSuccess && mFailure == null) {
                        mFailure = new RuntimeException("no failure exception set");
                    }
                    OrderTestHelper.this.notifyAll();
                }
            }
        }

        public void success() {
            synchronized (this) {
                mSuccess = true;
                quit();
            }
        }

        public void failure(RuntimeException failure) {
            synchronized (this) {
                mSuccess = false;
                mFailure = failure;
                quit();
            }
        }

        private void quit() {
            synchronized (this) {
                mDone = true;
                notifyAll();
            }
        }
    }
}
