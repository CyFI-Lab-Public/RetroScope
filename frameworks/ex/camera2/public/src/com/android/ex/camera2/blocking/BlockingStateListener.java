/*
 * Copyright 2013 The Android Open Source Project
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
package com.android.ex.camera2.blocking;

import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import com.android.ex.camera2.exceptions.TimeoutRuntimeException;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * A camera device listener that implements blocking operations on state changes.
 *
 * <p>Provides wait calls that block until the next unobserved state of the
 * requested type arrives. Unobserved states are states that have occurred since
 * the last wait, or that will be received from the camera device in the
 * future.</p>
 *
 * <p>Pass-through all StateListener changes to the proxy.</p>
 *
 */
public class BlockingStateListener extends CameraDevice.StateListener {
    private static final String TAG = "BlockingStateListener";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    private final CameraDevice.StateListener mProxy;

    // Guards mWaiting
    private final Object mLock = new Object();
    private boolean mWaiting = false;

    private final LinkedBlockingQueue<Integer> mRecentStates =
            new LinkedBlockingQueue<Integer>();

    private void setCurrentState(int state) {
        if (VERBOSE) Log.v(TAG, "Camera device state now " + stateToString(state));
        try {
            mRecentStates.put(state);
        } catch(InterruptedException e) {
            throw new RuntimeException("Unable to set device state", e);
        }
    }

    private static final String[] mStateNames = {
        "STATE_UNINITIALIZED",
        "STATE_OPENED",
        "STATE_UNCONFIGURED",
        "STATE_IDLE",
        "STATE_ACTIVE",
        "STATE_BUSY",
        "STATE_CLOSED",
        "STATE_DISCONNECTED",
        "STATE_ERROR"
    };

    /**
     * Device has not reported any state yet
     */
    public static final int STATE_UNINITIALIZED = -1;

    /**
     * Device is in the first-opened state (transitory)
     */
    public static final int STATE_OPENED = 0;

    /**
     * Device is unconfigured
     */
    public static final int STATE_UNCONFIGURED = 1;

    /**
     * Device is idle
     */
    public static final int STATE_IDLE = 2;

    /**
     * Device is active (transitory)
     */
    public static final int STATE_ACTIVE = 3;

    /**
     * Device is busy (transitory)
     */
    public static final int STATE_BUSY = 4;

    /**
     * Device is closed
     */
    public static final int STATE_CLOSED = 5;

    /**
     * Device is disconnected
     */
    public static final int STATE_DISCONNECTED = 6;

    /**
     * Device has encountered a fatal error
     */
    public static final int STATE_ERROR = 7;

    /**
     * Total number of reachable states
     */
    private static int NUM_STATES = 8;

    public BlockingStateListener() {
        mProxy = null;
    }

    public BlockingStateListener(CameraDevice.StateListener listener) {
        mProxy = listener;
    }

    @Override
    public void onOpened(CameraDevice camera) {
        setCurrentState(STATE_OPENED);
        if (mProxy != null) mProxy.onOpened(camera);
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        setCurrentState(STATE_DISCONNECTED);
        if (mProxy != null) mProxy.onDisconnected(camera);
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        setCurrentState(STATE_ERROR);
        if (mProxy != null) mProxy.onError(camera, error);
    }

    @Override
    public void onUnconfigured(CameraDevice camera) {
        setCurrentState(STATE_UNCONFIGURED);
        if (mProxy != null) mProxy.onUnconfigured(camera);
    }

    @Override
    public void onIdle(CameraDevice camera) {
        setCurrentState(STATE_IDLE);
        if (mProxy != null) mProxy.onIdle(camera);
    }

    @Override
    public void onActive(CameraDevice camera) {
        setCurrentState(STATE_ACTIVE);
        if (mProxy != null) mProxy.onActive(camera);
    }

    @Override
    public void onBusy(CameraDevice camera) {
        setCurrentState(STATE_BUSY);
        if (mProxy != null) mProxy.onBusy(camera);
    }

    @Override
    public void onClosed(CameraDevice camera) {
        setCurrentState(STATE_CLOSED);
        if (mProxy != null) mProxy.onClosed(camera);
    }

    /**
     * Wait until the desired state is observed, checking all state
     * transitions since the last state that was waited on.
     *
     * <p>Note: Only one waiter allowed at a time!</p>
     *
     * @param desired state to observe a transition to
     * @param timeout how long to wait in milliseconds
     *
     * @throws TimeoutRuntimeException if the desired state is not observed before timeout.
     */
    public void waitForState(int state, long timeout) {
        Integer[] stateArray = { state };

        waitForAnyOfStates(Arrays.asList(stateArray), timeout);
    }

    /**
     * Wait until the one of the desired states is observed, checking all
     * state transitions since the last state that was waited on.
     *
     * <p>Note: Only one waiter allowed at a time!</p>
     *
     * @param states Set of desired states to observe a transition to.
     * @param timeout how long to wait in milliseconds
     *
     * @return the state reached
     * @throws TimeoutRuntimeException if none of the states is observed before timeout.
     *
     */
    public int waitForAnyOfStates(Collection<Integer> states, final long timeout) {
        synchronized(mLock) {
            if (mWaiting) throw new IllegalStateException("Only one waiter allowed at a time");
            mWaiting = true;
        }
        if (VERBOSE) {
            StringBuilder s = new StringBuilder("Waiting for state(s) ");
            appendStates(s, states);
            Log.v(TAG, s.toString());
        }

        Integer nextState = null;
        long timeoutLeft = timeout;
        long startMs = SystemClock.elapsedRealtime();
        try {
            while ((nextState = mRecentStates.poll(timeoutLeft, TimeUnit.MILLISECONDS))
                    != null) {
                if (VERBOSE) {
                    Log.v(TAG, "  Saw transition to " + stateToString(nextState));
                }
                if (states.contains(nextState)) break;
                long endMs = SystemClock.elapsedRealtime();
                timeoutLeft -= (endMs - startMs);
                startMs = endMs;
            }
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException("Does not support interrupts on waits", e);
        }

        synchronized(mLock) {
            mWaiting = false;
        }

        if (!states.contains(nextState)) {
            StringBuilder s = new StringBuilder("Timed out after ");
            s.append(timeout);
            s.append(" ms waiting for state(s) ");
            appendStates(s, states);

            throw new TimeoutRuntimeException(s.toString());
        }

        return nextState;
    }

    /**
     * Convert state integer to a String
     */
    public static String stateToString(int state) {
        return mStateNames[state + 1];
    }

    /**
     * Append all states to string
     */
    public static void appendStates(StringBuilder s, Collection<Integer> states) {
        boolean start = true;
        for (Integer state: states) {
            if (!start) s.append(" ");
            s.append(stateToString(state));
            start = false;
        }
    }
}
