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
package com.android.cts.opengl.reference;

import com.android.cts.opengl.GLActivityIntentKeys;

import android.cts.util.CtsActivityInstrumentationTestCase2;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.TimeoutReq;

import android.content.Intent;

/**
 * Runs the Reference OpenGL ES 2.0 Benchmark.
 */
public class GLReferenceBenchmark extends CtsActivityInstrumentationTestCase2<GLReferenceActivity> {

    private static final int NUM_FRAMES_PER_SCENE = 500;
    private static final int NUM_SCENES = 2;
    private static final int NUM_FRAMES = NUM_FRAMES_PER_SCENE * NUM_SCENES;
    private static final int TIMEOUT = 1000000;

    public GLReferenceBenchmark() {
        super(GLReferenceActivity.class);
    }

    /**
     * Runs the reference benchmark.
     */
    @TimeoutReq(minutes = 30)
    public void testReferenceBenchmark() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, NUM_FRAMES);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, TIMEOUT);

        GLReferenceActivity activity = null;
        setActivityIntent(intent);
        activity = getActivity();
        if (activity != null) {
            activity.waitForCompletion();
            double totalTime = 0;
            double[] setUpTimes = activity.mSetUpTimes;
            double[] updateTimes = activity.mUpdateTimes;
            double[] renderTimes = activity.mRenderTimes;

            // Calculate update and render average.
            double updateSum = updateTimes[0];
            double renderSum = renderTimes[0];
            for (int i = 0; i < NUM_FRAMES - 1; i++) {
                updateSum += updateTimes[i + 1];
                renderSum += renderTimes[i + 1];
            }
            double updateAverage = updateSum / NUM_FRAMES;
            double renderAverage = renderSum / NUM_FRAMES;

            getReportLog().printArray(
                    "Set Up Times", setUpTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
            getReportLog().printArray(
                    "Update Times", updateTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
            getReportLog().printValue(
                    "Update Time Average", updateAverage, ResultType.LOWER_BETTER,
                    ResultUnit.MS);
            getReportLog().printArray(
                    "Render Times", renderTimes, ResultType.LOWER_BETTER, ResultUnit.MS);
            getReportLog().printValue(
                    "Render Time Average", renderAverage, ResultType.LOWER_BETTER,
                    ResultUnit.MS);
            totalTime = setUpTimes[0] + setUpTimes[1] + setUpTimes[2] +
                    setUpTimes[3] + updateAverage + renderAverage;
            getReportLog().printSummary(
                    "Total Time", totalTime, ResultType.LOWER_BETTER, ResultUnit.MS);
        }
    }
}
