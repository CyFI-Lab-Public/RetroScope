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

package android.hardware.cts;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.opengl.cts.GLSurfaceViewStubActivity;

import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import android.test.ActivityInstrumentationTestCase2;
import android.test.MoreAsserts;
import android.test.UiThreadTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.android.cts.util.TimeoutReq;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * This test case must run with hardware. It can't be tested in emulator
 */
@LargeTest
public class CameraGLTest extends ActivityInstrumentationTestCase2<GLSurfaceViewStubActivity> {
    private static final String TAG = "CameraGLTest";
    private static final String PACKAGE = "com.android.cts.stub";
    private static final boolean LOGV = false;
    private static final boolean LOGVV = false;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;

    private boolean mSurfaceTextureCallbackResult = false;

    private static final int WAIT_FOR_COMMAND_TO_COMPLETE = 5000;  // Milliseconds.
    private static final int WAIT_FOR_FOCUS_TO_COMPLETE = 5000;
    private static final int WAIT_FOR_SNAPSHOT_TO_COMPLETE = 5000;

    private SurfaceTextureCallback mSurfaceTextureCallback = new SurfaceTextureCallback();
    private SurfaceTextureBurstCallback mSurfaceTextureBurstCallback = new SurfaceTextureBurstCallback();
    private PreviewCallback mPreviewCallback = new PreviewCallback();

    private Looper mLooper = null;
    private final ConditionVariable mSurfaceTextureDone = new ConditionVariable();
    private final ConditionVariable mPreviewDone = new ConditionVariable();

    Camera mCamera;
    SurfaceTexture mSurfaceTexture;
    Renderer mRenderer;
    GLSurfaceView mGLView;

    public CameraGLTest() {
        super(PACKAGE, GLSurfaceViewStubActivity.class);
        if (LOGV) Log.v(TAG, "CameraGLTest Constructor");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Set up renderer instance
        mRenderer = this.new Renderer();
        GLSurfaceViewStubActivity.setRenderer(mRenderer);
        GLSurfaceViewStubActivity.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        GLSurfaceViewStubActivity.setGlVersion(2);
        // Start CameraStubActivity.
        GLSurfaceViewStubActivity stubActivity = getActivity();
        // Store a link to the view so we can redraw it when needed
        mGLView = stubActivity.getView();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mCamera != null) {
            terminateMessageLooper();
        }
        // Clean up static values in stub so it can be reused
        GLSurfaceViewStubActivity.resetRenderMode();
        GLSurfaceViewStubActivity.resetRenderer();
        GLSurfaceViewStubActivity.resetGlVersion();

        super.tearDown();
    }

    /**
     * Initializes the message looper so that the Camera object can
     * receive the callback messages.
     */
    private void initializeMessageLooper(final int cameraId) {
        final ConditionVariable startDone = new ConditionVariable();
        new Thread() {
            @Override
            public void run() {
                Log.v(TAG, "Start camera/surfacetexture thread");
                // Set up a looper to be used by camera.
                Looper.prepare();
                // Save the looper so that we can terminate this thread
                // after we are done with it.
                mLooper = Looper.myLooper();
                // These must be instantiated outside the UI thread, since the
                // UI thread will be doing a lot of waiting, stopping callbacks.
                mCamera = Camera.open(cameraId);
                mSurfaceTexture = new SurfaceTexture(mRenderer.getTextureID());
                Log.v(TAG, "Camera " + cameraId + " is opened.");
                startDone.open();
                Looper.loop(); // Blocks forever until Looper.quit() is called.
                Log.v(TAG, "Stop camera/surfacetexture thread");
            }
        }.start();

        Log.v(TAG, "start waiting for looper");
        if (!startDone.block(WAIT_FOR_COMMAND_TO_COMPLETE)) {
            fail("initializeMessageLooper: start timeout");
        }
    }

    /**
     * Terminates the message looper thread.
     */
    private void terminateMessageLooper() throws Exception {
        if (LOGV) Log.v(TAG, "Shutting down camera");
        mCamera.release();
        mLooper.quit();
        // Looper.quit() is asynchronous. The looper may still has some
        // preview callbacks in the queue after quit is called. The preview
        // callback still uses the camera object (setHasPreviewCallback).
        // After camera is released, RuntimeException will be thrown from
        // the method. So we need to join the looper thread here.
        mLooper.getThread().join();
        mCamera = null;
        mSurfaceTexture = null;
        if (LOGV) Log.v(TAG, "Shutdown of camera complete.");
    }

    /** The camera preview callback. Stops capture after the first callback */
    private final class PreviewCallback
            implements android.hardware.Camera.PreviewCallback {
        public void onPreviewFrame(byte [] data, Camera camera) {
            if (LOGV) Log.v(TAG, "PreviewCallback");
            assertNotNull(data);
            Size size = camera.getParameters().getPreviewSize();
            assertEquals(size.width * size.height * 3 / 2, data.length);
            mCamera.stopPreview();
            mPreviewDone.open();
        }
    }

    /** A simple SurfaceTexture listener callback, meant to be used together with the camera preview
     * callback */
    private final class SurfaceTextureCallback
            implements android.graphics.SurfaceTexture.OnFrameAvailableListener {
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (LOGV) Log.v(TAG, "SurfaceTextureCallback");
            mSurfaceTextureDone.open();
            // Assumes preview is stopped elsewhere
        }
    }

    /** A burst SurfaceTexture listener callback, used for multiple-frame capture */
    private final class SurfaceTextureBurstCallback
            implements android.graphics.SurfaceTexture.OnFrameAvailableListener {

        public void setBurstCount(int burstCount) {
            mBurstCount = burstCount;
        }

        public int getBurstCount() {
            return mBurstCount;
        }

        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (LOGVV) Log.v(TAG, "SurfaceTextureBurstCallback, frame #" + mBurstCount);
            mBurstCount--;
            if (!mSurfaceTextureCallbackResult) {
                if (mBurstCount <= 0) {
                    if (LOGV) Log.v(TAG, "SurfaceTextureBurstCallback stopping preview.");
                    mCamera.stopPreview();
                    if (LOGVV) Log.v(TAG, "SurfaceTextureBurstCallback preview stopped.");
                    mSurfaceTextureCallbackResult = true;
                }
                mSurfaceTextureDone.open();
            }
        }

        private int mBurstCount = 0;
    }

    /** Waits until surface texture callbacks have fired */
    private boolean waitForSurfaceTextureDone() {
        if (LOGVV) Log.v(TAG, "Wait for surface texture callback");
        if (!mSurfaceTextureDone.block(WAIT_FOR_COMMAND_TO_COMPLETE)) {
            // timeout could be expected or unexpected. The caller will decide.
            Log.v(TAG, "waitForSurfaceTextureDone: timeout");
            return false;
        }
        mSurfaceTextureDone.close();
        return true;
    }

    /** Waits until the camera preview callback has fired */
    private boolean waitForPreviewDone() {
        if (LOGVV) Log.v(TAG, "Wait for preview callback");
        if (!mPreviewDone.block(WAIT_FOR_COMMAND_TO_COMPLETE)) {
            // timeout could be expected or unexpected. The caller will decide.
            Log.v(TAG, "waitForPreviewDone: timeout");
            return false;
        }
        mPreviewDone.close();
        return true;
    }

    /** @return OpenGL ES major version 1 or 2 or some negative number for error */
    private static int getDetectedVersion() {
        /*
         * Get all the device configurations and check if any of the attributes specify the
         * the EGL_OPENGL_ES2_BIT to determine whether the device supports 2.0.
         */
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] numConfigs = new int[1];

        if (egl.eglInitialize(display, null)) {
            try {
                if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                    EGLConfig[] configs = new EGLConfig[numConfigs[0]];
                    if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                        int[] value = new int[1];
                        for (int i = 0; i < numConfigs[0]; i++) {
                            if (egl.eglGetConfigAttrib(display, configs[i],
                                    EGL10.EGL_RENDERABLE_TYPE, value)) {
                                if ((value[0] & EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
                                    return 2;
                                }
                            } else {
                                Log.w(TAG, "Getting config attribute with "
                                        + "EGL10#eglGetConfigAttrib failed "
                                        + "(" + i + "/" + numConfigs[0] + "): "
                                        + egl.eglGetError());
                            }
                        }
                        return 1;
                    } else {
                        Log.e(TAG, "Getting configs with EGL10#eglGetConfigs failed: "
                                + egl.eglGetError());
                        return -1;
                    }
                } else {
                    Log.e(TAG, "Getting number of configs with EGL10#eglGetConfigs failed: "
                            + egl.eglGetError());
                    return -2;
                }
              } finally {
                  egl.eglTerminate(display);
              }
        } else {
            Log.e(TAG, "Couldn't initialize EGL.");
            return -3;
        }
    }

    /** Generic per-camera test interface */
    private interface RunPerCamera {
        void run(int cameraId) throws Exception;
    }

    /** Generic camera test runner, to minimize boilerplace duplication */
    private void runForAllCameras(RunPerCamera test) throws Exception {
        /* Currently OpenGL ES 2.0 is supported for this test, so just skip it
           if only 1.0 is available. */
        int glVersion = getDetectedVersion();
        assertTrue(glVersion > 0);
        if (glVersion != 2) {
            Log.w(TAG, "Skipping test because OpenGL ES 2 is not supported");
            return;
        }

        /* Make sure the screen stays on while testing - otherwise the OpenGL context may disappear */
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "CameraGLTest");
        wl.acquire();
        try {
            /* Run the requested test per camera */
            int nCameras = Camera.getNumberOfCameras();
            for (int id = 0; id < nCameras; id++) {
                Log.v(TAG, "Camera id=" + id);
                test.run(id);
            }
        } finally {
            wl.release();
            // If an assert failed, camera might still be active. Clean up before next test.
            if (mCamera != null) {
                terminateMessageLooper();
            }
        }
    }

    /** Test Camera.setPreviewTexture in conjunction with the standard Camera preview callback */
    @UiThreadTest
    public void testSetPreviewTexturePreviewCallback() throws Exception {
        runForAllCameras(testSetPreviewTexturePreviewCallbackByCamera);
    }

    private RunPerCamera testSetPreviewTexturePreviewCallbackByCamera = new RunPerCamera() {
        public void run(int cameraId) throws Exception {
            boolean noTimeout;
            // Check the order: startPreview->setPreviewTexture, with a PreviewCallback as well
            mPreviewDone.close();
            initializeMessageLooper(cameraId);
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);
            noTimeout = waitForPreviewDone();
            assertTrue("Timeout waiting for new preview callback!", noTimeout);
            terminateMessageLooper();

            // Check the order: setPreviewTexture->startPreview.
            initializeMessageLooper(cameraId);
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();
            noTimeout = waitForPreviewDone();
            assertTrue("Timeout waiting for new preview callback!", noTimeout);

            // Check the order: setting preview display to null->startPreview->
            // setPreviewTexture.
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mCamera.setPreviewTexture(null);
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);
            noTimeout = waitForPreviewDone();
            assertTrue("Timeout waiting for new preview callback!", noTimeout);
            terminateMessageLooper();
        }
    };

    /** Test Camera.setPreviewTexture in conjunction with both the standard Camera preview callback,
        and the SurfaceTexture onFrameAvailable callback */
    @UiThreadTest
    public void testSetPreviewTextureBothCallbacks() throws Exception {
        runForAllCameras(testSetPreviewTextureBothCallbacksByCamera);
    }

    private RunPerCamera testSetPreviewTextureBothCallbacksByCamera = new RunPerCamera() {
        public void run(int cameraId) throws Exception {
            boolean noTimeout;
            // Check SurfaceTexture callback together with preview callback
            // Check the order: setPreviewTexture->startPreview
            mSurfaceTextureDone.close();
            initializeMessageLooper(cameraId);
            mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureCallback);
            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();

            noTimeout = waitForSurfaceTextureDone();
            assertTrue("Timeout waiting for new frame from SurfaceTexture!", noTimeout);
            noTimeout = waitForPreviewDone();
            assertTrue("Timeout waiting for new preview callback!",noTimeout);

            mGLView.requestRender();
            terminateMessageLooper();

            // Check the order: startPreview->setPreviewTexture
            mSurfaceTextureDone.close();
            initializeMessageLooper(cameraId);
            mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureCallback);
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);

            noTimeout = waitForSurfaceTextureDone();
            assertTrue("Timeout waiting for new frame from SurfaceTexture!", noTimeout);
            noTimeout = waitForPreviewDone();
            assertTrue("Timeout waiting for new preview callback!", noTimeout);

            mGLView.requestRender();

            // Check the order: setting preview to null->startPreview->setPreviewTexture
            mCamera.setOneShotPreviewCallback(mPreviewCallback);
            mCamera.setPreviewTexture(null);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureCallback);
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);
            noTimeout = waitForPreviewDone();
            assertTrue(noTimeout);
            terminateMessageLooper();
        }
    };

    /** Test Camera.setPreviewTexture in conjunction with just the SurfaceTexture onFrameAvailable callback */
    @UiThreadTest
    public void testSetPreviewTextureTextureCallback() throws Exception {
        runForAllCameras(testSetPreviewTextureTextureCallbackByCamera);
    }

    private RunPerCamera testSetPreviewTextureTextureCallbackByCamera = new RunPerCamera() {
        public void run(int cameraId) throws Exception {
            boolean noTimeout;
            // Check that SurfaceTexture callbacks work with no standard
            // preview callback
            mSurfaceTextureCallbackResult = false;
            mSurfaceTextureDone.close();
            initializeMessageLooper(cameraId);
            mSurfaceTextureBurstCallback.setBurstCount(1);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureBurstCallback);
            mCamera.setPreviewTexture(mSurfaceTexture);
            mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
            mCamera.startPreview();

            noTimeout = waitForSurfaceTextureDone();
            mGLView.requestRender();
            assertTrue(noTimeout);

            terminateMessageLooper();
            assertTrue(mSurfaceTextureCallbackResult);

            // Check that SurfaceTexture callbacks also work with
            // startPreview->setPreviewTexture
            mSurfaceTextureCallbackResult = false;
            mSurfaceTextureDone.close();
            initializeMessageLooper(cameraId);
            mSurfaceTextureBurstCallback.setBurstCount(1);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureBurstCallback);
            mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);

            noTimeout = waitForSurfaceTextureDone();
            assertTrue(noTimeout);

            terminateMessageLooper();
            assertTrue(mSurfaceTextureCallbackResult);

            // Check that SurfaceTexture callbacks also work with
            // null->startPreview->setPreviewTexture
            mSurfaceTextureCallbackResult = false;
            mSurfaceTextureDone.close();
            initializeMessageLooper(cameraId);
            mSurfaceTextureBurstCallback.setBurstCount(1);
            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureBurstCallback);
            mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
            mCamera.setPreviewTexture(null);
            mCamera.startPreview();
            mCamera.setPreviewTexture(mSurfaceTexture);

            noTimeout = waitForSurfaceTextureDone();
            assertTrue(noTimeout);

            terminateMessageLooper();
            assertTrue(mSurfaceTextureCallbackResult);
        }
    };

    /** Test all preview sizes and framerates along with SurfaceTexture-provided metadata (texture
     * transforms and timestamps).
     * TODO: This should be made stricter once SurfaceTexture timestamps are generated by the drivers.
     */
    @UiThreadTest
    @TimeoutReq(minutes = 20)
    public void testCameraToSurfaceTextureMetadata() throws Exception {
        runForAllCameras(testCameraToSurfaceTextureMetadataByCamera);
    }

    private RunPerCamera testCameraToSurfaceTextureMetadataByCamera = new RunPerCamera() {
        public void run(int cameraId) throws Exception {
            // Number of frames to test over
            int kLoopCount = 100;
            // Number of frames that can be out of bounds before calling this a failure
            int kMaxOutOfBoundsFrames = kLoopCount / 25; // 4% of frames
            // Ignore timestamp issues before this frame
            int kFirstTestedFrame = 10;
            // Slop in timestamp testing, needed because timestamps are not
            // currently being set by driver-level code so are subject to
            // user-space timing variability
            float kTestSlopMargin = 20; // ms

            boolean noTimeout;
            initializeMessageLooper(cameraId);
            Parameters parameters = mCamera.getParameters();

            mSurfaceTexture.setOnFrameAvailableListener(mSurfaceTextureBurstCallback);
            mCamera.setPreviewTexture(mSurfaceTexture);

            for (Size size: parameters.getSupportedPreviewSizes()) {
                for (int[] fps: parameters.getSupportedPreviewFpsRange()) {
                    if (LOGV) {
                        Log.v(TAG, "Testing camera #" + cameraId +
                              ", preview size:" + size.width + "x" + size.height +
                              ", frame rate range: [" +
                              (fps[Parameters.PREVIEW_FPS_MIN_INDEX] / 1000.) + "," +
                              (fps[Parameters.PREVIEW_FPS_MAX_INDEX] / 1000.) + "]");
                    }
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setPreviewFpsRange(fps[Parameters.PREVIEW_FPS_MIN_INDEX],
                                                  fps[Parameters.PREVIEW_FPS_MAX_INDEX]);
                    mCamera.setParameters(parameters);

                    assertEquals(size, mCamera.getParameters().getPreviewSize());

                    int[] actualFps = new int[2];
                    mCamera.getParameters().getPreviewFpsRange(actualFps);
                    assertEquals(fps[Parameters.PREVIEW_FPS_MIN_INDEX],
                                 actualFps[Parameters.PREVIEW_FPS_MIN_INDEX]);
                    assertEquals(fps[Parameters.PREVIEW_FPS_MAX_INDEX],
                                 actualFps[Parameters.PREVIEW_FPS_MAX_INDEX]);

                    mSurfaceTextureBurstCallback.
                            setBurstCount(kLoopCount + kFirstTestedFrame);
                    mSurfaceTextureCallbackResult = false;
                    mSurfaceTextureDone.close();

                    mRenderer.setCameraSizing(mCamera.getParameters().getPreviewSize());
                    if (LOGV) Log.v(TAG, "Starting preview");
                    mCamera.startPreview();
                    if (LOGVV) Log.v(TAG, "Preview started");

                    long[] timestamps = new long[kLoopCount];
                    for (int i = 0; i < kLoopCount + kFirstTestedFrame; i++) {
                        noTimeout = waitForSurfaceTextureDone();
                        assertTrue("Timeout waiting for frame " + i +
                                   " (burst callback thinks " +
                                   (kLoopCount - mSurfaceTextureBurstCallback.getBurstCount()) +
                                   ")! (Size " + size.width + "x" + size.height + ", fps [" +
                                   (fps[Parameters.PREVIEW_FPS_MIN_INDEX] / 1000.) + ", " +
                                   (fps[Parameters.PREVIEW_FPS_MAX_INDEX] / 1000.) + "])",
                                   noTimeout);

                        if (LOGVV) Log.v(TAG, "Frame #" + i + " completed");
                        // Draw the frame (and update the SurfaceTexture)
                        mGLView.requestRender();
                        // Wait until frame is drawn, so that the SurfaceTexture has new
                        // metadata
                        noTimeout = mRenderer.waitForDrawDone();
                        assertTrue(noTimeout);

                        // Store timestamps for later
                        if (i >= kFirstTestedFrame) {
                            timestamps[i - kFirstTestedFrame] =
                                    mSurfaceTexture.getTimestamp();
                        }
                        // Verify that the surfaceTexture transform has at least one non-zero
                        // entry
                        float[] transform = new float[16];
                        mSurfaceTexture.getTransformMatrix(transform);
                        boolean nonZero = false;
                        for (int k = 0; k < 16; k++) {
                            if (transform[k] != 0.f) {
                                nonZero = true;
                                break;
                            }
                        }
                        assertTrue(nonZero);
                    }
                    assertTrue(mSurfaceTextureCallbackResult);

                    float expectedMaxFrameDurationMs = 1000.f * 1000.f /
                            fps[Parameters.PREVIEW_FPS_MIN_INDEX];
                    float slopMaxFrameDurationMs = expectedMaxFrameDurationMs +
                            kTestSlopMargin;
                    float expectedMinFrameDurationMs = 1000.f * 1000.f /
                            fps[Parameters.PREVIEW_FPS_MAX_INDEX];
                    float slopMinFrameDurationMs = expectedMinFrameDurationMs  -
                            kTestSlopMargin;

                    int outOfBoundsCount = 0;
                    for (int i = 1; i < kLoopCount; i++) {
                        float frameDurationMs =
                                (timestamps[i] - timestamps[i-1]) / 1000000.f;
                        if (LOGVV) {
                            Log.v(TAG, "Frame " + i + " duration: " + frameDurationMs +
                                  " ms, expecting [" + expectedMinFrameDurationMs + "," +
                                    expectedMaxFrameDurationMs + "], slop range [" +
                                    slopMinFrameDurationMs + "," + slopMaxFrameDurationMs + "].");
                        }
                        if ( frameDurationMs > slopMaxFrameDurationMs ||
                                frameDurationMs < slopMinFrameDurationMs ) {
                            if (LOGVV) {
                                Log.v(TAG, "  Out of bounds!!");
                            }
                            outOfBoundsCount++;
                        }
                    }
                    assertTrue(
                            "Too many frame intervals out of frame rate bounds: "
                            + outOfBoundsCount +
                            ", limit " + kMaxOutOfBoundsFrames,
                            outOfBoundsCount <= kMaxOutOfBoundsFrames);
                }
            }
            terminateMessageLooper();
        } // void run(int cameraId)
    };

    /** Basic OpenGL ES 2.0 renderer to draw SurfaceTexture-sourced frames to the screen */
    private class Renderer implements GLSurfaceView.Renderer {
        public Renderer() {
            mTriangleVertices =
                    ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).
                    order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
            Matrix.setIdentityM(mMMatrix, 0);

            mTextureID = 0;
        }

        public void setCameraSizing(Camera.Size previewSize) {
            mCameraRatio = (float)previewSize.width/previewSize.height;
        }

        public boolean waitForDrawDone() {
            if (!mDrawDone.block(WAIT_FOR_COMMAND_TO_COMPLETE) ) {
                // timeout could be expected or unexpected. The caller will decide.
                Log.e(TAG, "waitForDrawDone: timeout");
                return false;
            }
            mDrawDone.close();
            return true;
        }

        private final ConditionVariable mDrawDone = new ConditionVariable();

        public void onDrawFrame(GL10 glUnused) {
            if (LOGVV) Log.v(TAG, "onDrawFrame()");
            if (CameraGLTest.this.mSurfaceTexture != null) {
                CameraGLTest.this.mSurfaceTexture.updateTexImage();
                CameraGLTest.this.mSurfaceTexture.getTransformMatrix(mSTMatrix);
                mDrawDone.open();
            }

            // Ignore the passed-in GL10 interface, and use the GLES20
            // class's static methods instead.
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                                         TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                                         TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
            GLES20.glUniform1f(muCRatioHandle, mCameraRatio);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
        }

        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
            if (LOGV) Log.v(TAG, "onSurfaceChanged()");
            // Ignore the passed-in GL10 interface, and use the GLES20
            // class's static methods instead.
            GLES20.glViewport(0, 0, width, height);
            mRatio = (float) width / height;
            Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);
        }

        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            if (LOGV) Log.v(TAG, "onSurfaceCreated()");
            // Ignore the passed-in GL10 interface, and use the GLES20
            // class's static methods instead.

            /* Set up shaders and handles to their variables */
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                return;
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }

            muCRatioHandle = GLES20.glGetUniformLocation(mProgram, "uCRatio");
            checkGlError("glGetUniformLocation uCRatio");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uCRatio");
            }

            /*
             * Create our texture. This has to be done each time the
             * surface is created.
             */

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            // Can't do mipmapping with camera source
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                                   GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                                   GLES20.GL_LINEAR);
            // Clamp to edge is the only option
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                                   GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                                   GLES20.GL_CLAMP_TO_EDGE);
            checkGlError("glTexParameteri mTextureID");

            Matrix.setLookAtM(mVMatrix, 0, 0, 0, 4f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        }

        public int getTextureID() {
            return mTextureID;
        }

        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                checkGlError("glAttachShader");
                GLES20.glAttachShader(program, pixelShader);
                checkGlError("glAttachShader");
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,   1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;

        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "uniform float uCRatio;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = vec4(uCRatio,1,1,1) * uMVPMatrix * aPosition;\n" +
                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";

        private final String mFragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mProjMatrix = new float[16];
        private float[] mMMatrix = new float[16];
        private float[] mVMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int muCRatioHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        private float mRatio = 1.0f;
        private float mCameraRatio = 1.0f;

        private Context mContext;
        private static final String TAG = "CameraGLTest.Renderer";

        // Magic key
        private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    }

}
