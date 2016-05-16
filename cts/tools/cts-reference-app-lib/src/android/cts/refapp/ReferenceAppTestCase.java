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

package android.cts.refapp;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import java.lang.InterruptedException;
import java.lang.Thread;

/**
 * Base Class that provides common functionality for all Reference Application Tests.
 */
public class ReferenceAppTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    /** The time to wait for the test host to finish taking the snapshot. */
    private static final int SNAPSHOT_TIMEOUT_MS = 1000;
    /** The default time that the applicaiton has to start in. */
    private static final int DEFAULT_MAX_STATUP_TIME_MS = 5000;

    private int maxStartupTimeMs;

    /**
     * Create a ReferenceAppTestCase for the specified activity.
     *
     * @param pkg the java package the class is contained in.
     * @param activityClass the class of the activity to instrument.
     * @param maxStartupTimeMs the startup time the activity should start in.
     */
    public ReferenceAppTestCase(String pkg, Class<T> activityClass, int maxStartupTimeMs) {
        super(pkg, activityClass);
        this.maxStartupTimeMs = maxStartupTimeMs;
    }

    /**
     * Create a ReferenceAppTestCase for the specified activity.
     *
     * @param pkg the java package the class is contained in.
     * @param activityClass the class of the activity to instrument.
     */
    public ReferenceAppTestCase(String pkg, Class<T> activityClass) {
       this(pkg, activityClass, DEFAULT_MAX_STATUP_TIME_MS);
    }

    public void testActivityStartupTime() {
        // Test activity startup time.

        long start = System.currentTimeMillis();
        Activity activity = getActivity();
        long end = System.currentTimeMillis();

        long startupTime = end - start;
        assertTrue("Activity Startup took more than " + maxStartupTimeMs +
                   " ms",
                   startupTime <= maxStartupTimeMs);
    }

    /**
     * Allows tests to record screen snapshots for inclusion in the
     * CTS test report.
     *
     * @param name the name to save the snapshot under
     */
    public void takeSnapshot(String name) {
        // request a snapshot from the CTS host
        Log.d("ReferenceAppTestCase", "takeSnapshot:" + name);
        // Give the host enough time to take the picture
        try {
            Thread.sleep(SNAPSHOT_TIMEOUT_MS);
        } catch (InterruptedException e) {
            // ok
        }
    }
}
