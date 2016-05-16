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

/**
 * Thread class for executing a Runnable containing assertions in a separate thread.
 * Uncaught exceptions in the Runnable are rethrown in the context of the the thread
 * calling the <code>runTest()</code> method.
 */
public final class TestThread extends Thread {
    private Throwable mThrowable;
    private Runnable mTarget;

    public TestThread(Runnable target) {
        mTarget = target;
    }

    @Override
    public final void run() {
        try {
            mTarget.run();
        } catch (Throwable t) {
            mThrowable = t;
        }
    }

    /**
     * Run the target Runnable object and wait until the test finish or throw
     * out Exception if test fail.
     *
     * @param runTime
     * @throws Throwable
     */
    public void runTest(long runTime) throws Throwable {
        start();
        joinAndCheck(runTime);
    }

    /**
     * Get the Throwable object which is thrown when test running
     * @return  The Throwable object
     */
    public Throwable getThrowable() {
        return mThrowable;
    }

    /**
     * Set the Throwable object which is thrown when test running
     * @param t The Throwable object
     */
    public void setThrowable(Throwable t) {
        mThrowable = t;
    }

    /**
     * Wait for the test thread to complete and throw the stored exception if there is one.
     *
     * @param runTime The time to wait for the test thread to complete.
     * @throws Throwable
     */
    public void joinAndCheck(long runTime) throws Throwable {
        this.join(runTime);
        if (this.isAlive()) {
            this.interrupt();
            this.join(runTime);
            throw new Exception("Thread did not finish within allotted time.");
        }
        checkException();
    }

    /**
     * Check whether there is an exception when running Runnable object.
     * @throws Throwable
     */
    public void checkException() throws Throwable {
        if (mThrowable != null) {
            throw mThrowable;
        }
    }
}
