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

package com.android.quicksearchbox;

import android.os.SystemClock;

/**
 * Tracks latency in wall-clock time. Since {@link #getLatency} returns an {@code int},
 * latencies over 2^31 ms (~ 25 days) cannot be measured.
 * This class uses {@link SystemClock#uptimeMillis} which does not advance during deep sleep.
 */
public class LatencyTracker {

    /**
     * Start time, in milliseconds as returned by {@link SystemClock#uptimeMillis}.
     */
    private long mStartTime;

    /**
     * Creates a new latency tracker and sets the start time.
     */
    public LatencyTracker() {
        mStartTime = SystemClock.uptimeMillis();
    }

    /**
     * Resets the start time.
     */
    public void reset() {
        mStartTime = SystemClock.uptimeMillis();
    }

    /**
     * Gets the number of milliseconds since the object was created, or {@link #reset} was called.
     */
    public int getLatency() {
        long now = SystemClock.uptimeMillis();
        return (int) (now - mStartTime);
    }

}
