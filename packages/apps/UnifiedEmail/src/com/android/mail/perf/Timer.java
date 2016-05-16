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
package com.android.mail.perf;

import android.os.Debug;
import android.os.SystemClock;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance timing utilities for Gmail.
 *
 * There are two main ways to log performance. For simple, one off events, the static methods
 * {@link #startTiming} and {@link #stopTiming} are sufficient:
 *
 * <pre>
 * Timer.startTiming("myEvent");
 * ... code for myEvent ...
 * Timer.stopTiming("myEvent");
 * </pre>
 *
 * The other way is to instantiate a timer that can be passed around, and started and paused. The
 * timer will accumulate the results for each tag, and dump the results when asked.
 *
 * <pre>
 * Timer timer = new Timer();
 *
 * for (int i = 0; i < lots; i++) {
 *   timer.start("tagA");
 *   ... code for tagA ...
 *   timer.pause("tagA");
 *   ... code that isn't relevant to timing ...
 * }
 *
 * timer.dumpResults();
 * </pre>
 *
 */
public class Timer {
    private static final String LOG_TAG = LogTag.getLogTag();

    // set this to false to compile out all timer calls
    public static final boolean ENABLE_TIMER = false;
    // Set to true to enable logging of performance stats.
    private static final boolean LOG_PERFORMANCE_STATS = true;

    /** An internal structure used for performance markers. */
    private static class PerformancePoint {
        public final long mCpuTimeNanos;
        public final long mWallTimeMillis;

        public PerformancePoint() {
            mCpuTimeNanos = Debug.threadCpuTimeNanos();
            mWallTimeMillis = SystemClock.uptimeMillis();
        }
    }

    private final Map<String, ArrayList<PerformancePoint>> mPoints = Maps.newHashMap();
    private final Map<String, Integer> mCounts = Maps.newHashMap();
    private final boolean mEnabled;

    public Timer() {
        this(false);
    }

    public Timer(boolean enable) {
        mEnabled = enable;
    }

    @SuppressWarnings("unused")
    public boolean isEnabled() {
        return ENABLE_TIMER && mEnabled;
    }

    /**
     * Starts timing an event indicated by the {@code tag}.
     */
    @SuppressWarnings("unused")
    public void start(String tag) {
        if (ENABLE_TIMER && mEnabled) {
            ArrayList<PerformancePoint> values = mPoints.get(tag);
            if (values == null) {
                values = new ArrayList<PerformancePoint>();
                mPoints.put(tag, values);
            }
            if (values.size() % 2 == 0) {
                values.add(new PerformancePoint());
            }
        }
    }

    /**
     * Stops timing an event indicated by the {@code tag}
     */
    @SuppressWarnings("unused")
    public void pause(String tag) {
        if (ENABLE_TIMER && mEnabled) {
            ArrayList<PerformancePoint> values = mPoints.get(tag);
            if (values != null && values.size() % 2 == 1) {
                values.add(new PerformancePoint());
            }
        }
    }

    @SuppressWarnings("unused")
    public void count(String tag) {
        if (ENABLE_TIMER && mEnabled) {
            Integer counts = mCounts.get(tag);
            if (counts == null) {
                counts = 0;
            }
            mCounts.put(tag, counts + 1);
        }
    }

    @SuppressWarnings("unused")
    public void clear() {
        if (ENABLE_TIMER && mEnabled) {
            mPoints.clear();
            mCounts.clear();
        }
    }

    /**
     * Dumps cumulative timing results for all tags recognized by this timer.
     */
    @SuppressWarnings("unused")
    public void dumpResults() {
        if (ENABLE_TIMER && mEnabled) {
            for (Map.Entry<String, ArrayList<PerformancePoint>> entry : mPoints.entrySet()) {
                String tag = entry.getKey();
                ArrayList<PerformancePoint> values = entry.getValue();
                long cpuDurationNanos = 0;
                long wallDurationMillis = 0;

                for (int i = 0; i < values.size() - 1; i += 2) {
                    PerformancePoint startPoint = values.get(i);
                    PerformancePoint endPoint = values.get(i + 1);

                    cpuDurationNanos += endPoint.mCpuTimeNanos - startPoint.mCpuTimeNanos;
                    wallDurationMillis += endPoint.mWallTimeMillis - startPoint.mWallTimeMillis;
                }

                if (cpuDurationNanos == 0) {
                    cpuDurationNanos = 1;
                }

                dumpTimings(tag, 1, cpuDurationNanos, wallDurationMillis);
            }

            if (LOG_PERFORMANCE_STATS) {
                for (Map.Entry<String, Integer> entry : mCounts.entrySet()) {
                    LogUtils.d(LOG_TAG, "Perf %s count: %d", entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * Used for timing one off events.
     */
    private static Map<String, PerformancePoint> sPerformanceCollector =
            new ConcurrentHashMap<String, PerformancePoint>();

    /**
     * Starts a one-off timer for an event. The event is denoted by {@code tag} and only one event
     * of that tag may be timed at a given time.
     */
    public static void startTiming(String tag) {
        if (ENABLE_TIMER) {
            sPerformanceCollector.put(tag, new PerformancePoint());
        }
    }

    /**
     * Stops a one-off timer for an event indicated by {@code tag}.
     */
    public static void stopTiming(String tag) {
        if (ENABLE_TIMER) {
            stopTiming(tag, 1 /* one subiteration */);
        }
    }

    /**
     * Stops a one-off timer for an event indicated by {@code tag}, and indicates that the event
     * consisted of {@code numSubIterations} sub-events, so that performance output will be denoted
     * as such.
     */
    public static void stopTiming(String tag, int numSubIterations) {
        if (ENABLE_TIMER) {
            PerformancePoint endPoint = new PerformancePoint();
            PerformancePoint startPoint = sPerformanceCollector.get(tag);
            if (startPoint == null) {
                return;
            }
            long cpuDurationNanos = endPoint.mCpuTimeNanos - startPoint.mCpuTimeNanos;
            long wallDurationMillis = endPoint.mWallTimeMillis - startPoint.mWallTimeMillis;
            // Make sure cpu Duration is non 0
            if (cpuDurationNanos == 0) {
                cpuDurationNanos = 1;
            }

            dumpTimings(tag, numSubIterations, cpuDurationNanos, wallDurationMillis);
        }
    }

    private static void dumpTimings(String tag, int numSubIterations,
            long cpuDurationNanos, long wallDurationMillis) {

        if (LOG_PERFORMANCE_STATS) {
            LogUtils.d(LOG_TAG, "Perf %s wall: %d cpu: %d",
                    tag, wallDurationMillis, (cpuDurationNanos / 1000000));
            // print out the average time for each sub iteration
            if (numSubIterations > 1) {
                LogUtils.d(LOG_TAG, "Perf/operation %s wall: %d cpu: %d", tag,
                        (wallDurationMillis / numSubIterations),
                        ((cpuDurationNanos / 1000000) / numSubIterations));
            }
        }
    }

}
