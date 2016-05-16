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

package android.media.cts;

import android.opengl.GLES20;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;


/**
 * Tests synchronized frame presentation.
 *
 * SurfaceFlinger allows a "desired presentation time" value to be passed along with buffers of
 * data.  This exercises that feature.
 */
public class PresentationSyncTest extends ActivityInstrumentationTestCase2<MediaStubActivity>
        implements SurfaceHolder.Callback {
    private static final String TAG = "PresentationSyncTest";
    private static final boolean VERBOSE = false;           // lots of logging
    private static final int FRAME_COUNT = 128;             // ~2 sec @ 60fps

    // message values
    private static final int START_TEST = 0;
    private static final int END_TEST = 1;

    // width and height of the Surface we're given to draw on
    private int mWidth;
    private int mHeight;

    public PresentationSyncTest() {
        super(MediaStubActivity.class);
    }

    /**
     * Tests whether the output frame rate can be limited by the presentation time.
     * <p>
     * Generates and displays the same series of images three times.  The first run uses "now"
     * as the desired presentation time to establish an estimate of the refresh time.  Later
     * runs set the presentation time to (start_time + frame_number * refresh_time * multiplier),
     * with the expectation that a multiplier of 2 will cause the animation to render at
     * half speed.
     * <p>
     * This test does not use Choreographer.  The longer the test runs, the farther out of
     * phase the test will become with respect to the actual vsync timing.
     * <p>
     * Setting the presentation time for a frame is most easily done through an EGL extension,
     * so we render each frame through GL.
     *
     * @throws Exception
     */
    public void testThroughput() throws Exception {
        // Get the Surface from the SurfaceView.
        // TODO: is it safe to assume that it's ready?
        SurfaceHolder holder = getActivity().getSurfaceHolder();
        holder.addCallback(this);

        // We use the width/height to render a simple series of patterns.  If we get this
        // wrong it shouldn't really matter -- some driver optimizations might make things
        // faster, but it shouldn't affect how long it takes the frame to be displayed.
        //
        // We can get this from the View or from the EGLSurface.  We don't have easy direct
        // access to any of those things, so just ask our InputSurface to get it from EGL,
        // since that's where we're drawing.
        //
        // Note: InputSurface was intended for a different purpose, but it's 99% right for our
        // needs.  Maybe rename it to "RecordableSurface"?  Or trivially wrap it with a
        // subclass that suppresses the EGL_RECORDABLE_ANDROID flag?
        InputSurface output = new InputSurface(holder.getSurface());
        mWidth = output.getWidth();
        mHeight = output.getHeight();
        Log.d(TAG, "Surface w=" + mWidth + " h=" + mHeight);
        output.makeCurrent();

        // Run a test with no presentation times specified.  Assuming nothing else is
        // fighting us for resources, all frames should display as quickly as possible,
        // and we can estimate the refresh rate of the device.
        long baseTimeNsec = runThroughputTest(output, 0L, -1.0f);
        long refreshNsec = baseTimeNsec / FRAME_COUNT;
        Log.i(TAG, "Using " + refreshNsec + "ns as refresh rate");

        // Run tests with times specified, at 1.3x, 1x, 1/2x, and 1/4x speed.
        //
        // One particular device is overly aggressive at reducing clock frequencies, and it
        // will slow things to the point where we can't push frames quickly enough in the
        // faster test.  By adding an artificial workload in a second thread we can make the
        // system run faster.  (This could have a detrimental effect on a single-core CPU,
        // so it's a no-op there.)
        CpuWaster cpuWaster = new CpuWaster();
        try {
            cpuWaster.start();
            runThroughputTest(output, refreshNsec, 0.75f);
            cpuWaster.stop();
            runThroughputTest(output, refreshNsec, 1.0f);
            runThroughputTest(output, refreshNsec, 2.0f);
            runThroughputTest(output, refreshNsec, 4.0f);
        } finally {
            cpuWaster.stop();
        }

        output.release();
    }

    /**
     * Runs the throughput test on the provided surface with the specified time values.
     * <p>
     * If mult is -1, the test runs in "training" mode, rendering frames as quickly as
     * possible.  This can be used to establish a baseline.
     * <p>
     * @return the test duration, in nanoseconds
     */
    private long runThroughputTest(InputSurface output, long frameTimeNsec, float mult) {
        Log.d(TAG, "runThroughputTest: " + mult);

        // Sleep briefly.  This is strangely necessary on some devices to allow the GPU to
        // catch up (b/10898363).  It also provides an easily-visible break in the systrace
        // output.
        try { Thread.sleep(50); }
        catch (InterruptedException ignored) {}

        long startNsec = System.nanoTime();
        long showNsec = 0;

        if (true) {
            // Output a frame that creates a "marker" in the --latency output
            drawFrame(0, mult);
            output.setPresentationTime(startNsec - 16700000L * 100);
            Trace.beginSection("TEST BEGIN");
            output.swapBuffers();
            Trace.endSection();
            startNsec = System.nanoTime();
        }

        for (int frameNum = 0; frameNum < FRAME_COUNT; frameNum++) {
            if (mult != -1.0f) {
                showNsec = startNsec + (long) (frameNum * frameTimeNsec * mult);
            }
            drawFrame(frameNum, mult);
            output.setPresentationTime(showNsec);
            Trace.beginSection("swapbuf " + frameNum);
            output.swapBuffers();
            Trace.endSection();
        }

        long endNsec = System.nanoTime();
        long actualNsec = endNsec - startNsec;

        if (mult != -1) {
            // Some variation is inevitable, but we should be within a few percent of expected.
            long expectedNsec = (long) (frameTimeNsec * FRAME_COUNT * mult);
            long deltaNsec = Math.abs(expectedNsec - actualNsec);
            double delta = (double) deltaNsec / expectedNsec;
            final double MAX_DELTA = 0.05;
            if (delta > MAX_DELTA) {
                throw new RuntimeException("Time delta exceeds tolerance (" + MAX_DELTA +
                        "): mult=" + mult + ": expected=" + expectedNsec +
                        " actual=" + actualNsec + " p=" + delta);

            } else {
                Log.d(TAG, "mult=" + mult + ": expected=" + expectedNsec +
                        " actual=" + actualNsec + " p=" + delta);
            }
        }
        return endNsec - startNsec;
    }


    /**
     * Exercises the test code, driving it off of Choreographer.  The animation is driven at
     * full speed, but with rendering requested at a future time.  With each run the distance
     * into the future is increased.
     * <p>
     * Loopers can't be reused once they quit, so it's easiest to create a new thread for
     * each run.
     * <p>
     * (This isn't exactly a test -- it's primarily a way to exercise the code.  Evaluate the
     * results with "dumpsys SurfaceFlinger --latency SurfaceView" for each multiplier.
     * The idea is to see frames where the desired-present is as close as possible to the
     * actual-present, while still minimizing frame-ready.  If we go too far into the future
     * the BufferQueue will start to back up.)
     * <p>
     * @throws Exception
     */
    public void suppressed_testChoreographed() throws Throwable {
        // Get the Surface from the SurfaceView.
        // TODO: is it safe to assume that it's ready?
        SurfaceHolder holder = getActivity().getSurfaceHolder();
        holder.addCallback(this);

        InputSurface output = new InputSurface(holder.getSurface());
        mWidth = output.getWidth();
        mHeight = output.getHeight();
        Log.d(TAG, "Surface w=" + mWidth + " h=" + mHeight);

        for (int i = 1; i < 5; i++) {
            ChoreographedWrapper.runTest(this, output, i);
        }

        output.release();
    }

    /**
     * Shifts the test to a new thread, so we can manage our own Looper.  Any exception
     * thrown on the new thread is propagated to the caller.
     */
    private static class ChoreographedWrapper implements Runnable {
        private final PresentationSyncTest mTest;
        private final InputSurface mOutput;
        private final int mFrameDelay;
        private Throwable mThrowable;

        private ChoreographedWrapper(PresentationSyncTest test, InputSurface output,
                int frameDelay) {
            mTest = test;
            mOutput = output;
            mFrameDelay = frameDelay;
        }

        @Override
        public void run() {
            try {
                mTest.runChoreographedTest(mOutput, mFrameDelay);
            } catch (Throwable th) {
                mThrowable = th;
            }
        }

        /** Entry point. */
        public static void runTest(PresentationSyncTest obj, InputSurface output,
                int frameDelay) throws Throwable {
            ChoreographedWrapper wrapper = new ChoreographedWrapper(obj, output, frameDelay);
            Thread th = new Thread(wrapper, "sync test");
            th.start();
            th.join();
            if (wrapper.mThrowable != null) {
                throw wrapper.mThrowable;
            }
        }
    }

    /**
     * Runs the test, driven by callbacks from the Looper we define here.
     */
    private void runChoreographedTest(InputSurface output, int frameDelay) {
        Log.d(TAG, "runChoreographedTest");

        output.makeCurrent();
        final ChoRunner chore = new ChoRunner(output);

        Looper.prepare();
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case START_TEST:
                        Log.d(TAG, "Starting test");
                        chore.start(this, msg.arg1 /*frameDelay*/);
                        break;
                    case END_TEST:
                        Log.d(TAG, "Ending test");
                        Looper.myLooper().quitSafely();
                        break;
                    default:
                        Log.d(TAG, "unknown message " + msg.what);
                        break;
                }
            }
        };

        handler.sendMessage(Message.obtain(handler, START_TEST, frameDelay, 0));

        Log.d(TAG, "looping (frameDelay=" + frameDelay + ")");
        long startNanos = System.nanoTime();
        Trace.beginSection("TEST BEGIN fd=" + frameDelay);
        Looper.loop();
        Trace.endSection();
        long durationNanos = System.nanoTime() - startNanos;
        Log.d(TAG, "loop exiting after " + durationNanos +
                " (" + durationNanos / FRAME_COUNT + "ns)");

        output.makeUnCurrent();
    }


    private class ChoRunner implements Choreographer.FrameCallback {
        private final InputSurface mOutput;
        private int mFrameDelay;
        private Handler mHandler;
        private int mCurFrame;
        private Choreographer mChocho;
        private long mPrevFrameTimeNanos;
        private long mFrameDiff;

        public ChoRunner(InputSurface output) {
            mOutput = output;
        }

        public void start(Handler handler, int frameDelay) {
            mHandler = handler;
            mFrameDelay = frameDelay;

            mCurFrame = 0;
            mChocho = Choreographer.getInstance();
            mChocho.postFrameCallback(this);
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            if (mPrevFrameTimeNanos != 0) {
                // Update our vsync rate guess every frame so that, if we start with a
                // stutter, we don't carry it for the whole test.
                assertTrue(frameTimeNanos > mPrevFrameTimeNanos);
                long prevDiff = frameTimeNanos - mPrevFrameTimeNanos;
                if (mFrameDiff == 0 || mFrameDiff > prevDiff) {
                    mFrameDiff = prevDiff;
                    Log.d(TAG, "refresh rate approx " + mFrameDiff + "ns");
                }

                // If the current diff is >= 2x the expected frame time diff, we stuttered
                // and need to drop a frame.  (We might even need to drop more than one
                // frame; ignoring that for now.)
                if (prevDiff > mFrameDiff * 1.9) {
                    Log.d(TAG, "skip " + mCurFrame + " diff=" + prevDiff);
                    mCurFrame++;
                }
            }
            mPrevFrameTimeNanos = frameTimeNanos;

            if (mFrameDiff != 0) {
                // set desired display time to N frames in the future, rather than ASAP.
                //
                // Note this is a "don't open until Xmas" feature.  If vsyncs are happening
                // at times T1, T2, T3, and we want the frame to appear onscreen when the
                // buffers flip at T2, then we can theoretically request any time value
                // in [T1, T2).
                mOutput.setPresentationTime(frameTimeNanos + (mFrameDiff * mFrameDelay));
            }

            drawFrame(mCurFrame, mFrameDelay);
            Trace.beginSection("swapbuf " + mCurFrame);
            mOutput.swapBuffers();
            Trace.endSection();

            if (++mCurFrame < FRAME_COUNT) {
                mChocho.postFrameCallback(this);
            } else {
                mHandler.sendMessage(Message.obtain(mHandler, END_TEST));
            }
        }
    }

    /**
     * Draws a frame with GLES in the current context.
     */
    private void drawFrame(int num, float mult) {
        num %= 64;
        float colorVal;

        if (mult > 0) {
            colorVal = 1.0f / mult;
        } else {
            colorVal = 0.1f;
        }

        int startX, startY;
        startX = (num % 16) * (mWidth / 16);
        startY = (num / 16) * (mHeight / 4);
        if ((num >= 16 && num < 32) || (num >= 48)) {
            // reverse direction
            startX = (mWidth - mWidth/16) - startX;
        }

        // clear screen
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // draw rect
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, mWidth / 16, mHeight / 4);
        GLES20.glClearColor(colorVal, 1 - colorVal, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // This doesn't seem to happen in practice with current test framework -- Surface is
        // already created before we start, and the orientation is locked.
        Log.d(TAG, "surfaceChanged f=" + format + " w=" + width + " h=" + height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
    }


    /**
     * Wastes CPU time.
     * <p>
     * The start() and stop() methods must be called from the same thread.
     */
    private static class CpuWaster {
        volatile boolean mRunning = false;

        public void start() {
            if (mRunning) {
                throw new IllegalStateException("already running");
            }

            if (Runtime.getRuntime().availableProcessors() < 2) {
                return;
            }

            mRunning = true;

            new Thread("Stupid") {
                @Override
                public void run() {
                    while (mRunning) { /* spin! */ }
                }
            }.start();

            // sleep briefly while the system re-evaluates its load (might want to spin)
            try { Thread.sleep(10); }
            catch (InterruptedException ignored) {}
        }

        public void stop() {
            if (mRunning) {
                mRunning = false;

                // give the system a chance to slow back down
                try { Thread.sleep(10); }
                catch (InterruptedException ignored) {}
            }
        }
    }
}
