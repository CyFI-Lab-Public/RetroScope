/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.hardware.cts.helpers;

import junit.framework.Assert;

import java.util.concurrent.TimeUnit;

/**
 * Base test class that supports a basic test operation performed in a sensor.
 * The class follows a command patter as a base for its work.
 *
 * Remarks:
 * - The class wraps verifications and test checks that are needed to verify the operation.
 * - The operation runs in a background thread where it performs the bulk of its work.
 */
public abstract class SensorTestOperation {
    private final SensorTestExceptionHandler mExceptionHandler = new SensorTestExceptionHandler();

    protected final String LOG_TAG = "TestRunner";
    protected final long WAIT_TIMEOUT_IN_MILLISECONDS =
            TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private Thread mThread;

    protected int mIterationCount;

    /**
     * Public API definition.
     */
    public synchronized void start() throws Throwable {
        if(mThread != null) {
            throw new IllegalStateException("The operation has already been started.");
        }

        mThread = new Thread() {
            @Override
            public void run() {
                try {
                    doWork();
                } catch (Throwable e) {
                    // log the exception so it can be sent back to the appropriate test thread
                    this.getUncaughtExceptionHandler().uncaughtException(this, e);
                }
            }
        };

        ++mIterationCount;
        mThread.setUncaughtExceptionHandler(mExceptionHandler);
        mThread.start();
    }

    public synchronized void waitForCompletion() throws Throwable {
        if(mThread == null) {
            // let a wait on a stopped operation to be no-op
            return;
        }
        mThread.join(WAIT_TIMEOUT_IN_MILLISECONDS);
        if(mThread.isAlive()) {
            // the test is hung so collect the state of the system and fail
            String operationName = this.getClass().getSimpleName();
            String message = String.format(
                    "%s hung. %s. BugReport collected at: %s",
                    operationName,
                    this.toString(),
                    SensorCtsHelper.collectBugreport(operationName));
            Assert.fail(message);
        }
        mThread = null;
        mExceptionHandler.rethrow();
    }

    public void execute() throws Throwable {
        this.start();
        this.waitForCompletion();
    }

    @Override
    public String toString() {
        return String.format("ThreadId:%d, Iteration:%d", mThread.getId(), mIterationCount);
    }

    /**
     * Subclasses implement this method to perform the work associated with the operation they
     * represent.
     */
    protected abstract void doWork() throws Throwable;

    /**
     * Private helpers.
     */
    private class SensorTestExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Object mLock = new Object();

        private Throwable mThrowable;

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            synchronized(mLock) {
                // the fist exception is in general the one that is more interesting
                if(mThrowable != null) {
                    return;
                }
                mThrowable = throwable;
            }
        }

        public void rethrow() throws Throwable {
            Throwable throwable;
            synchronized(mLock) {
                throwable = mThrowable;
                mThrowable = null;
            }
            if(throwable != null) {
                throw throwable;
            }
        }
    }
}
