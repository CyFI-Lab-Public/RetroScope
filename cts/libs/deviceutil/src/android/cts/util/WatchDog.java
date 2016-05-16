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
package android.cts.util;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.util.Log;

import junit.framework.Assert;

/**
 * class for checking if rendering function is alive or not.
 * panic if watch-dog is not reset over certain amount of time
 */
public class WatchDog implements Runnable {
    private static final String TAG = "WatchDog";
    private Thread mThread;
    private Semaphore mSemaphore;
    private volatile boolean mStopRequested;
    private final long mTimeoutInMilliSecs;
    private TimeoutCallback mCallback = null;

    public WatchDog(long timeoutInMilliSecs) {
        mTimeoutInMilliSecs = timeoutInMilliSecs;
    }

    public WatchDog(long timeoutInMilliSecs, TimeoutCallback callback) {
        this(timeoutInMilliSecs);
        mCallback = callback;
    }

    /** start watch-dog */
    public void start() {
        Log.i(TAG, "start");
        mStopRequested = false;
        mSemaphore = new Semaphore(0);
        mThread = new Thread(this);
        mThread.start();
    }

    /** stop watch-dog */
    public void stop() {
        Log.i(TAG, "stop");
        if (mThread == null) {
            return; // already finished
        }
        mStopRequested = true;
        mSemaphore.release();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        mThread = null;
        mSemaphore = null;
    }

    /** resets watch-dog, thus prevent it from panic */
    public void reset() {
        if (!mStopRequested) { // stop requested, but rendering still on-going
            mSemaphore.release();
        }
    }

    @Override
    public void run() {
        while (!mStopRequested) {
            try {
                boolean success = mSemaphore.tryAcquire(mTimeoutInMilliSecs, TimeUnit.MILLISECONDS);
                if (mCallback == null) {
                    Assert.assertTrue("Watchdog timed-out", success);
                } else if (!success) {
                    mCallback.onTimeout();
                }
            } catch (InterruptedException e) {
                // this thread will not be interrupted,
                // but if it happens, just check the exit condition.
            }
        }
    }

    /**
     * Called by the Watchdog when it has timed out.
     */
    public interface TimeoutCallback {

        public void onTimeout();
    }
}
