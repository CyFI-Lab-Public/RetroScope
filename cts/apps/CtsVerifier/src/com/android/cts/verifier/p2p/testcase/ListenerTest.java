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

package com.android.cts.verifier.p2p.testcase;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

/**
 * The utility class for callback function.
 */
public abstract class ListenerTest {

    private static final String TAG = "ListenerTest";

    /**
     * stop flag
     */
    protected boolean mStopFlag = false;

    /**
     * if true, error occurred.
     */
    protected boolean mIsError = false;

    /**
     * The expected argument list.
     */
    private List<ListenerArgument> mExArgList = null;

    /**
     * The received argument list before check() is called.
     */
    private List<ListenerArgument> mReceivedArgList = new ArrayList<ListenerArgument>();

    /**
     * The reason of failure.
     */
    private String mReason;

    /**
     * Check the callback with the expected argument is invoked successfully within
     * the specified timeout.
     *
     * @param arg the expected callback argument.
     * @param msec timeout.
     * @return if true, the appropriate callback is invoked successfully.
     * @throws InterruptedException
     */
    public boolean check(ListenerArgument arg, long msec) throws InterruptedException {
        ArrayList<ListenerArgument> argList = new ArrayList<ListenerArgument>();
        argList.add(arg);
        return check(argList, msec);
    }

    /**
     * Check the callbacks with the expected argument are all invoked successfully
     * within the specified timeout.
     *
     * @param argList the expected callback argument list.
     * @param msec timeout.
     * @return if true, the all appropriate callbacks are invoked successfully.
     * @throws InterruptedException
     */
    public synchronized boolean check(List<ListenerArgument> argList, long msec)
        throws InterruptedException {

        StringBuilder sb = new StringBuilder();
        createResultString(sb, "Expected", argList);

        try {
            mStopFlag = false;
            mExArgList = new ArrayList<ListenerArgument>(argList);
            if (mReceivedArgList.size() > 0) {
                for (ListenerArgument arg: mReceivedArgList) {
                    if (!mExArgList.remove(arg)) {
                        // the invalid callback is invoked before.
                        Log.d(TAG, " received an invalid response. " + arg);
                        return false;
                    }
                }
                if (mExArgList.isEmpty()) {
                    return true;
                }
            }

            waitCallback(msec);

            if (mIsError) {
                return false;
            }
            if (!mExArgList.isEmpty()) {
                Log.d(TAG, "could not received the expected response. " + mExArgList);
                return false;
            }
        } finally {
            createResultString(sb, "Received", mReceivedArgList);
            mReason = sb.toString();
            mReceivedArgList.clear();
            mExArgList = null;
        }

        return true;
    }

    /**
     * Get the reason of the failure.
     * @return
     */
    public synchronized String getReason() {
        return mReason;
    }

    /**
     * This function must be called from sub class on the callback invocation.
     * @param arg
     */
    protected synchronized void receiveCallback(ListenerArgument arg) {
        mReceivedArgList.add(arg);
        if (mExArgList == null) {
            return;
        }

        if (!mExArgList.remove(arg)) {
            Log.d(TAG, "received invalid response. " + arg);
            mIsError = true;
            wakeup();
            return;
        }
        if (mExArgList.isEmpty()) {
            wakeup();
        }
    }

    /**
     * Wake up main thread.
     */
    private synchronized void wakeup() {
        mStopFlag = true;
        notifyAll();
    }

    /**
     * Wait for the result.
     * @param msec
     */
    private synchronized void waitCallback(long msec) throws InterruptedException {
        Timeout t = new Timeout(msec);
        while (!t.isTimeout()) {
            if (mStopFlag) {
                return;
            }
            wait(t.getRemainTime());
        }
    }

    private void createResultString(StringBuilder sb, String tag,
            List<ListenerArgument> args) {
        sb.append(tag);
        sb.append(": ");
        if (args.size() == 0) {
            sb.append("None\n");
        }
        boolean isInitialTry = true;
        for (ListenerArgument arg: args) {
            if (!isInitialTry) {
                sb.append(",\n");
            }
            isInitialTry = false;
            sb.append("\"");
            sb.append(arg);
            sb.append("\"");
        }
        sb.append("\n\n");
    }

    static abstract class ListenerArgument {
    }
}
