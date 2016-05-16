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
package com.android.cts.opengl.primitive;

import com.android.cts.opengl.GLActivityIntentKeys;
import android.cts.util.CtsActivityInstrumentationTestCase2;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;

import android.content.Intent;
import com.android.cts.util.TimeoutReq;

/**
 * Runs the Primitive OpenGL ES 2.0 Benchmarks.
 */
public class GLPrimitiveBenchmark extends CtsActivityInstrumentationTestCase2<GLPrimitiveActivity> {

    private static final int NUM_FRAMES = 100;
    private static final int NUM_ITERATIONS = 8;
    private static final int TIMEOUT = 1000000;

    public GLPrimitiveBenchmark() {
        super(GLPrimitiveActivity.class);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test offscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testFullPipelineOffscreen() throws Exception {
        runBenchmark(BenchmarkName.FullPipeline, true, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the full OpenGL ES 2.0 pipeline test onscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testFullPipelineOnscreen() throws Exception {
        runBenchmark(BenchmarkName.FullPipeline, false, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the pixel output test offscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testPixelOutputOffscreen() throws Exception {
        runBenchmark(BenchmarkName.PixelOutput, true, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the pixel output test onscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testPixelOutputOnscreen() throws Exception {
        runBenchmark(BenchmarkName.PixelOutput, false, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the shader performance test offscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testShaderPerfOffscreen() throws Exception {
        runBenchmark(BenchmarkName.ShaderPerf, true, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the shader performance test onscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testShaderPerfOnscreen() throws Exception {
        runBenchmark(BenchmarkName.ShaderPerf, false, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the context switch overhead test offscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testContextSwitchOffscreen() throws Exception {
        runBenchmark(BenchmarkName.ContextSwitch, true, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the context switch overhead test onscreen.
     */
    @TimeoutReq(minutes = 100)
    public void testContextSwitchOnscreen() throws Exception {
        runBenchmark(BenchmarkName.ContextSwitch, false, NUM_FRAMES, NUM_ITERATIONS, TIMEOUT);
    }

    /**
     * Runs the specified test.
     *
     * @param benchmark An enum representing the benchmark to run.
     * @param offscreen Whether to render to an offscreen framebuffer rather than the screen.
     * @param numFrames The number of frames to render.
     * @param numIterations The number of iterations to run, each iteration has a bigger workload.
     * @param timeout The milliseconds to wait for an iteration of the benchmark before timing out.
     * @throws Exception If the benchmark could not be run.
     */
    private void runBenchmark(BenchmarkName benchmark, boolean offscreen, int numFrames,
            int numIterations, int timeout) throws Exception {
        String benchmarkName = benchmark.toString();
        Intent intent = new Intent();
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_BENCHMARK_NAME, benchmarkName);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_OFFSCREEN, offscreen);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, numFrames);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_ITERATIONS, numIterations);
        intent.putExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, timeout);

        GLPrimitiveActivity activity = null;
        setActivityIntent(intent);
        activity = getActivity();
        if (activity != null) {
            activity.waitForCompletion();
            double[] fpsValues = activity.mFpsValues;
            double score = 0;
            for (double d : fpsValues) {
                score += d;
            }
            score /= numIterations;// Average.

            // TODO: maybe standard deviation / RMSE will be useful?

            getReportLog().printArray(
                    "Fps Values", fpsValues, ResultType.HIGHER_BETTER, ResultUnit.FPS);
            getReportLog().printSummary(
                    "Average Frames Per Second", score, ResultType.HIGHER_BETTER,
                    ResultUnit.SCORE);
        }
    }
}
