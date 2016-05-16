/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.view.cts;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.cts.GLSurfaceViewStubActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.Display;
import android.view.WindowManager;
import android.util.Log;

import java.lang.InterruptedException;
import java.lang.Thread;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Test that the screen refresh rate claimed by
 * android.view.Display.getRefreshRate() matches the steady-state framerate
 * achieved by vsync-limited eglSwapBuffers(). The primary goal is to test
 * Display.getRefreshRate() -- using GL is just an easy and hopefully reliable
 * way of measuring the actual refresh rate.
 */
public class DisplayRefreshRateTest extends
        ActivityInstrumentationTestCase2<GLSurfaceViewStubActivity> {

    // The test passes if
    //   abs(measured_fps - Display.getRefreshRate()) <= FPS_TOLERANCE.
    // A smaller tolerance requires a more accurate measured_fps in order
    // to avoid false negatives.
    private static final float FPS_TOLERANCE = 2.0f;

    private static final String TAG = "DisplayRefreshRateTest";

    private class FpsResult {
        private float mFps;
        private boolean mValid = false;
        private boolean mRestartRequested = false;

        public final synchronized void notifyResult(float fps) {
            if (!mValid) {
                mFps = fps;
                mValid = true;
                notifyAll();
            }
        }

        public final synchronized float waitResult() {
            while (!mValid) {
                try {
                    wait();
                } catch (InterruptedException e) {/* ignore and retry */}
            }
            return mFps;
        }

        public synchronized void restart() {
            mRestartRequested = true;
            mValid = false;
        }
        public synchronized boolean restartNecessary() {
            return mRestartRequested;
        }
        public synchronized void ackRestart() {
            mRestartRequested = false;
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {
        // Measurement knobs.
        // NB: Some devices need a surprisingly long warmup period before the
        // framerate becomes stable.
        private static final float WARMUP_SECONDS = 2.0f;
        private static final float TEST_SECONDS   = 8.0f;

        // Test states
        private static final int STATE_START  = 0;
        private static final int STATE_WARMUP = 1;
        private static final int STATE_TEST   = 2;
        private static final int STATE_DONE   = 3;

        private FpsResult mResult;
        private int       mState     = STATE_START;
        private float     mStartTime = 0.0f;
        private int       mNumFrames = 0;

        public Renderer(FpsResult result) {
            mResult = result;
        }

        public void onDrawFrame(GL10 gl) {
            float t = (float)System.nanoTime() * 1.0e-9f;
            switch (mState) {
                case STATE_START:
                    mStartTime = t;
                    mState = STATE_WARMUP;
                    break;

                case STATE_WARMUP:
                    if ((t - mStartTime) >= WARMUP_SECONDS) {
                        mStartTime = t;
                        mNumFrames = 0;
                        mState = STATE_TEST;
                    }
                    break;

                case STATE_TEST:
                    mNumFrames++;
                    float elapsed = t - mStartTime;
                    if (elapsed >= TEST_SECONDS) {
                        mResult.notifyResult((float)mNumFrames / elapsed);
                        mState = STATE_DONE;
                    }
                    break;

                case STATE_DONE:
                    if (mResult.restartNecessary()) {
                        mResult.ackRestart();
                        mState = STATE_START;
                        Log.d(TAG, "restarting");
                    }
                    break;
            }

            // prevent unwanted optimizations or hidden costs (e.g. reading
            // previous frame on tilers).
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // Do nothing.
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // Do nothing.
        }
    }

    private FpsResult mResult;

    public DisplayRefreshRateTest() {
        super(GLSurfaceViewStubActivity.class);
        mResult = new FpsResult();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        GLSurfaceViewStubActivity.setRenderer(new Renderer(mResult));
        GLSurfaceViewStubActivity.setRenderMode(
                GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void testRefreshRate() throws java.lang.InterruptedException {
        boolean fpsOk = false;
        GLSurfaceViewStubActivity activity = getActivity();

        WindowManager wm = (WindowManager)activity
                .getView()
                .getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Display dpy = wm.getDefaultDisplay();
        float claimedFps = dpy.getRefreshRate();

        for (int i = 0; i < 3; i++) {
            float achievedFps = mResult.waitResult();
            Log.d(TAG, "claimed " + claimedFps + " fps, " +
                       "achieved " + achievedFps + " fps");
            fpsOk = Math.abs(claimedFps - achievedFps) <= FPS_TOLERANCE;
            if (fpsOk) {
                break;
            } else {
                // it could be other sctivity like bug report capturing for other failures
                // sleep for a while and re-try
                Thread.sleep(10000);
                mResult.restart();
            }
        }
        activity.finish();
        assertTrue(fpsOk);
    }

}
