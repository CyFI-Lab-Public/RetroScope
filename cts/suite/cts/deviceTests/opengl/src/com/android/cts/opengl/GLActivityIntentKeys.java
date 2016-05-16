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
package com.android.cts.opengl;

public class GLActivityIntentKeys {
    /**
     * Holds the name of the benchmark to run.
     */
    public final static String INTENT_EXTRA_BENCHMARK_NAME = "benchmark_name";
    /**
     * Holds whether or not the benchmark is to be run offscreen.
     */
    public final static String INTENT_EXTRA_OFFSCREEN = "offscreen";
    /**
     * The number of frames to render for each workload.
     */
    public final static String INTENT_EXTRA_NUM_FRAMES = "num_frames";
    /**
     * The number of iterations to run, the workload increases with each iteration.
     */
    public final static String INTENT_EXTRA_NUM_ITERATIONS = "num_iterations";
    /**
     * The number of milliseconds to wait before timing out.
     */
    public final static String INTENT_EXTRA_TIMEOUT = "timeout";
}
