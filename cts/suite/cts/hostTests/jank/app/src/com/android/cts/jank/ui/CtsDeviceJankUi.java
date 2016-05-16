/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.cts.jank.ui;

import android.util.Log;
import android.widget.ListView;

import com.android.cts.jank.CtsJankTestBase;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.platform.SurfaceFlingerHelper;

public class CtsDeviceJankUi extends CtsJankTestBase {
    private final static String TAG = CtsDeviceJankUi.class.getName();
    private final static String PACKAGE = "com.android.cts.ui";
    private final static String COMPONENT =
            PACKAGE + "/" + PACKAGE + ".ScrollingActivity";
    private final static int NUM_ELEMENTS = 1000;
    private static String APP_WINDOW_NAME = COMPONENT;

    // TODO(stuartscott): expand deviceTests/ui app to have a more complex UI
    /**
     * Runs the ScrollingActivity and measures jank during a scroll.
     */
    public void testScrolling() throws Exception {
        // Start activity command
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(START_CMD, COMPONENT));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "num_elements", NUM_ELEMENTS));
        final String startCommand = sb.toString();
        final String stopCommand = String.format(STOP_CMD, PACKAGE);

        Log.i(TAG, "Start command: " + startCommand);
        Log.i(TAG, "Stop command: " + stopCommand);

        setIteration(NUM_ITERATIONS);
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            // Stop any existing instances
            runShellCommand(stopCommand);
            // Start activity
            runShellCommand(startCommand);

            // Wait for the activity to start
            sleep(SLEEP_TIME / 2);

            UiScrollable list = new UiScrollable(
                    new UiSelector().className(ListView.class.getName()));

            // Start systrace
            startTrace(mTestCaseName, i);

            // Clear SurfaceFlinger buffer
            Log.i(TAG, "Clearing SurfaceFlinger buffer");
            SurfaceFlingerHelper.clearBuffer(APP_WINDOW_NAME);

            list.flingToEnd(2);

            // Dump SurfaceFlinger buffer
            Log.i(TAG, "Dumping SurfaceFlinger buffer");
            boolean result = SurfaceFlingerHelper.dumpFrameLatency(APP_WINDOW_NAME, true);
            assertTrue("SurfaceFlingerHelper could not get timestamps", result);

            // Stop systrace
            endTrace();

            // Record results
            recordResults(mTestCaseName, i);
        }
        // Save aggregated results
        saveResults(mTestCaseName);
        // Stop any remaining instances
        runShellCommand(stopCommand);
    }
}
