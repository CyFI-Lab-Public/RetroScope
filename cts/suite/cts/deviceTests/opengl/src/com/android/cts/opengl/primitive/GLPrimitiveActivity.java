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

import android.app.Activity;
import android.content.Intent;
import android.cts.util.WatchDog;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;

import com.android.cts.opengl.GLActivityIntentKeys;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class GLPrimitiveActivity extends Activity {

    public final static String TAG = "GLPrimitiveActivity";

    private volatile Exception mException;
    private volatile Surface mSurface = null;
    private CountDownLatch mStartSignal = new CountDownLatch(1);
    private Semaphore mSemaphore = new Semaphore(0);

    private BenchmarkName mBenchmark;
    private boolean mOffscreen;
    private int mNumFrames;
    private int mNumIterations;
    private int mTimeout;
    public double[] mFpsValues;

    @Override
    public void onCreate(Bundle data) {
        super.onCreate(data);
        System.loadLibrary("ctsopengl_jni");
        Intent intent = getIntent();
        mBenchmark = BenchmarkName.valueOf(
                intent.getStringExtra(GLActivityIntentKeys.INTENT_EXTRA_BENCHMARK_NAME));
        mOffscreen = intent.getBooleanExtra(GLActivityIntentKeys.INTENT_EXTRA_OFFSCREEN, false);
        mNumFrames = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_FRAMES, 0);
        mNumIterations = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_NUM_ITERATIONS, 0);
        mTimeout = intent.getIntExtra(GLActivityIntentKeys.INTENT_EXTRA_TIMEOUT, 0);
        mFpsValues = new double[mNumIterations];

        Log.i(TAG, "Benchmark: " + mBenchmark);
        Log.i(TAG, "Offscreen: " + mOffscreen);
        Log.i(TAG, "Num Frames: " + mNumFrames);
        Log.i(TAG, "Num Iterations: " + mNumIterations);
        Log.i(TAG, "Time Out: " + mTimeout);

        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mSurface = holder.getSurface();
                mStartSignal.countDown();
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });
        setContentView(surfaceView);
        // Spawns a worker to run the benchmark.
        Worker worker = new Worker();
        worker.start();
    }

    public void waitForCompletion() throws Exception {
        // Wait for semiphore.
        mSemaphore.acquire();
        if (mException != null) {
            throw mException;
        }
    }

    private void complete() {
        // Release semiphore.
        mSemaphore.release();
        finish();
    }

    private synchronized void setException(Exception e) {
        if (mException == null) {
            mException = e;
        }
    }

    private static native void setupFullPipelineBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupPixelOutputBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupShaderPerfBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native void setupContextSwitchBenchmark(
            Surface surface, boolean offscreen, int workload);

    private static native boolean startBenchmark(int numFrames, double[] frameTimes);

    /**
     * This thread runs the benchmarks, freeing the UI thread.
     */
    private class Worker extends Thread implements WatchDog.TimeoutCallback {

        private WatchDog watchDog;
        private volatile boolean success = true;

        @Override
        public void run() {
            try {
                mStartSignal.await();
            } catch (InterruptedException e) {
                setException(e);
                complete();
                return;
            }
            Log.i(TAG, mBenchmark + " Benchmark Started");
            // Creates a watchdog to ensure a iteration doesn't exceed the timeout.
            watchDog = new WatchDog(mTimeout, this);
            // Used to record the start and end time of the iteration.
            double[] times = new double[2];
            for (int i = 0; i < mNumIterations && success; i++) {
                // The workload to use for this iteration.
                int workload = i + 1;
                // Setup the benchmark.
                switch (mBenchmark) {
                    case FullPipeline:
                        setupFullPipelineBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case PixelOutput:
                        setupPixelOutputBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case ShaderPerf:
                        setupShaderPerfBenchmark(mSurface, mOffscreen, workload);
                        break;
                    case ContextSwitch:
                        setupContextSwitchBenchmark(mSurface, mOffscreen, workload);
                        break;
                }
                watchDog.start();
                // Start benchmark.
                success = startBenchmark(mNumFrames, times);
                watchDog.stop();

                if (!success) {
                    setException(new Exception("Benchmark failed to run"));
                } else {
                    // Calculate FPS.
                    mFpsValues[i] = mNumFrames * 1000.0f / (times[1] - times[0]);
                }
            }
            complete();
            Log.i(TAG, mBenchmark + " Benchmark Completed");
        }

        public void onTimeout() {
            setException(new Exception("Benchmark timed out"));
            complete();
        }

    }
}
