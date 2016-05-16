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

package com.android.cts.jank.opengl;

import android.util.Log;

import com.android.cts.jank.CtsJankTestBase;
import com.android.uiautomator.platform.SurfaceFlingerHelper;

public class CtsDeviceJankOpenGl extends CtsJankTestBase {
    private final static String TAG = CtsDeviceJankOpenGl.class.getName();
    private final static String PACKAGE = "com.android.cts.opengl";
    private final static String COMPONENT =
            PACKAGE + "/" + PACKAGE + ".primitive.GLPrimitiveActivity";
    private static String APP_WINDOW_NAME = "SurfaceView";

    /**
     * Runs the full OpenGL ES 2.0 pipeline test.
     */
    public void testFullPipeline() throws Exception {
        runBenchmark("FullPipeline");
    }

    /**
     * Runs the pixel output test.
     */
    public void testPixelOutput() throws Exception {
        runBenchmark("PixelOutput");
    }

    /**
     * Runs the shader performance test.
     */
    public void testShaderPerf() throws Exception {
        runBenchmark("ShaderPerf");
    }

    /**
     * Runs the context switch overhead test.
     */
    public void testContextSwitch() throws Exception {
        runBenchmark("ContextSwitch");
    }

    /**
     * Runs the benchhmark for jank test.
     */
    public void runBenchmark(String benchmark) throws Exception {
        // Start activity command
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(START_CMD, COMPONENT));
        sb.append(String.format(INTENT_STRING_EXTRA, "benchmark_name", benchmark));
        sb.append(String.format(INTENT_BOOLEAN_EXTRA, "offscreen", false));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "num_frames", 200));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "num_iterations", 1));
        sb.append(String.format(INTENT_INTEGER_EXTRA, "timeout", 10000));
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

            // Start systrace
            startTrace(mTestCaseName, i);

            // Clear SurfaceFlinger buffer
            Log.i(TAG, "Clearing SurfaceFlinger buffer");
            SurfaceFlingerHelper.clearBuffer(APP_WINDOW_NAME);

            // This is where user interactions would go, in this case just sleep
            sleep(SLEEP_TIME);

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
