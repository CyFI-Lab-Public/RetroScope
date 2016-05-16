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


import android.app.Presentation;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests to check if MediaCodec encoding works with composition of multiple virtual displays
 * The test also tries to destroy and create virtual displays repeatedly to
 * detect any issues. The test itself does not check the output as it is already done in other
 * tests.
 */
public class EncodeVirtualDisplayWithCompositionTest extends AndroidTestCase {
    private static final String TAG = "EncodeVirtualDisplayWithCompositionTest";
    private static final boolean DBG = false;
    private static final String MIME_TYPE = "video/avc";
    private Handler mHandler;
    private Surface mSurface;
    private CodecInfo mCodecInfo;
    private volatile boolean mCodecConfigReceived = false;
    private volatile boolean mCodecBufferReceived = false;
    private EncoderEventListener mEncoderEventListener = new EncoderEventListener() {
        @Override
        public void onCodecConfig(ByteBuffer data, MediaCodec.BufferInfo info) {
            mCodecConfigReceived = true;
        }
        @Override
        public void onBufferReady(ByteBuffer data, MediaCodec.BufferInfo info) {
            mCodecBufferReceived = true;
        }
        @Override
        public void onError(String errorMessage) {
            fail(errorMessage);
        }
    };

    @Override
    protected void setUp() {
        mHandler = new Handler(Looper.getMainLooper());
        mCodecInfo = getAvcSupportedFormatInfo();
    }

    public void testSingleVirtualDisplay() throws Exception {
        doTestVirtualDisplays(1);
    }

    public void testMultipleVirtualDisplays() throws Exception {
        doTestVirtualDisplays(3);
    }

    void doTestVirtualDisplays(int numDisplays) throws Exception {
        final int NUM_CODEC_CREATION = 10;
        final int NUM_DISPLAY_CREATION = 20;
        final int NUM_RENDERING = 10;
        VirtualDisplayPresentation[] virtualDisplays = new VirtualDisplayPresentation[numDisplays];
        for (int i = 0; i < NUM_CODEC_CREATION; i++) {
            mCodecConfigReceived = false;
            mCodecBufferReceived = false;
            if (DBG) {
                Log.i(TAG, "start encoding");
            }
            EncodingHelper encodingHelper = new EncodingHelper();
            mSurface = encodingHelper.startEncoding(mCodecInfo, mEncoderEventListener);
            GlCompositor compositor = new GlCompositor();
            if (DBG) {
                Log.i(TAG, "start composition");
            }
            compositor.startComposition(mSurface, mCodecInfo.mMaxW, mCodecInfo.mMaxH,
                    numDisplays);
            for (int j = 0; j < NUM_DISPLAY_CREATION; j++) {
                if (DBG) {
                    Log.i(TAG, "create display");
                }
                for (int k = 0; k < numDisplays; k++) {
                    virtualDisplays[k] =
                        new VirtualDisplayPresentation(getContext(),
                                compositor.getWindowSurface(k),
                                mCodecInfo.mMaxW/numDisplays, mCodecInfo.mMaxH,
                                VirtualDisplayPresentation.RENDERING_VIEW_HIERARCHY);
                    virtualDisplays[k].createVirtualDisplay();
                    virtualDisplays[k].createPresentation();
                }
                if (DBG) {
                    Log.i(TAG, "start rendering");
                }
                for (int k = 0; k < NUM_RENDERING; k++) {
                    for (int l = 0; l < numDisplays; l++) {
                        virtualDisplays[l].doRendering();
                    }
                    // do not care how many frames are actually rendered.
                    Thread.sleep(1);
                }
                for (int k = 0; k < numDisplays; k++) {
                    virtualDisplays[k].dismissPresentation();
                    virtualDisplays[k].destroyVirtualDisplay();
                }
                compositor.recreateWindows();
            }
            if (DBG) {
                Log.i(TAG, "stop composition");
            }
            compositor.stopComposition();
            if (DBG) {
                Log.i(TAG, "stop encoding");
            }
            encodingHelper.stopEncoding();
            assertTrue(mCodecConfigReceived);
            assertTrue(mCodecBufferReceived);
        }
    }

    interface EncoderEventListener {
        public void onCodecConfig(ByteBuffer data, MediaCodec.BufferInfo info);
        public void onBufferReady(ByteBuffer data, MediaCodec.BufferInfo info);
        public void onError(String errorMessage);
    }

    private class EncodingHelper {
        private MediaCodec mEncoder;
        private volatile boolean mStopEncoding = false;
        private EncoderEventListener mEventListener;
        private CodecInfo mCodecInfo;
        private Thread mEncodingThread;
        private Surface mSurface;
        private static final int IFRAME_INTERVAL = 10;
        private Semaphore mInitCompleted = new Semaphore(0);

        Surface startEncoding(CodecInfo codecInfo, EncoderEventListener eventListener) {
            mCodecInfo = codecInfo;
            mEventListener = eventListener;
            mEncodingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        doEncoding();
                    } catch (Exception e) {
                        mEventListener.onError(e.toString());
                    }
                }
            });
            mEncodingThread.start();
            try {
                if (DBG) {
                    Log.i(TAG, "wait for encoder init");
                }
                mInitCompleted.acquire();
                if (DBG) {
                    Log.i(TAG, "wait for encoder done");
                }
            } catch (InterruptedException e) {
                fail("should not happen");
            }
            return mSurface;
        }

        void stopEncoding() {
            try {
                mStopEncoding = true;
                mEncodingThread.join();
            } catch(InterruptedException e) {
                // just ignore
            } finally {
                mEncodingThread = null;
            }
        }

        private void doEncoding() throws Exception {
            final int TIMEOUT_USEC_NORMAL = 1000000;
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mCodecInfo.mMaxW,
                    mCodecInfo.mMaxH);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, mCodecInfo.mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, mCodecInfo.mFps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.  Request a Surface to use for input.
            mEncoder = MediaCodec.createByCodecName(mCodecInfo.mCodecName);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
            mEncoder.start();
            mInitCompleted.release();
            try {
                ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                while (!mStopEncoding) {
                    int index = mEncoder.dequeueOutputBuffer(info, TIMEOUT_USEC_NORMAL);
                    if (index >= 0) {
                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.i(TAG, "codec config data");
                            ByteBuffer encodedData = encoderOutputBuffers[index];
                            encodedData.position(info.offset);
                            encodedData.limit(info.offset + info.size);
                            mEventListener.onCodecConfig(encodedData, info);
                            mEncoder.releaseOutputBuffer(index, false);
                        } else if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break;
                        } else {
                            ByteBuffer encodedData = encoderOutputBuffers[index];
                            encodedData.position(info.offset);
                            encodedData.limit(info.offset + info.size);
                            mEventListener.onBufferReady(encodedData, info);
                            mEncoder.releaseOutputBuffer(index, false);
                        }
                    }
                }
            } finally {
                mEncoder.stop();
                mEncoder.release();
                mEncoder = null;
            }
        }
    }

    /**
     * Handles composition of multiple SurfaceTexture into a single Surface
     */
    private class GlCompositor implements SurfaceTexture.OnFrameAvailableListener {
        private Surface mSurface;
        private int mWidth;
        private int mHeight;
        private int mNumWindows;
        private ArrayList<GlWindow> mWindows = new ArrayList<GlWindow>();
        private HashMap<SurfaceTexture, GlWindow> mSurfaceTextureToWindowMap =
                new HashMap<SurfaceTexture, GlWindow>();
        private Thread mCompositionThread;
        private Semaphore mStartCompletionSemaphore;
        private Semaphore mRecreationCompletionSemaphore;
        private Looper mLooper;
        private Handler mHandler;
        private InputSurface mEglHelper;
        private int mGlProgramId = 0;
        private int mGluMVPMatrixHandle;
        private int mGluSTMatrixHandle;
        private int mGlaPositionHandle;
        private int mGlaTextureHandle;
        private float[] mMVPMatrix = new float[16];

        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix * aPosition;\n" +
                "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";

        private static final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        void startComposition(Surface surface, int w, int h, int numWindows) {
            mSurface = surface;
            mWidth = w;
            mHeight = h;
            mNumWindows = numWindows;
            mCompositionThread = new Thread(new CompositionRunnable());
            mStartCompletionSemaphore = new Semaphore(0);
            mCompositionThread.start();
            waitForStartCompletion();
        }

        void stopComposition() {
            try {
                if (mLooper != null) {
                    mLooper.quit();
                    mCompositionThread.join();
                }
            } catch (InterruptedException e) {
                // don't care
            }
            mCompositionThread = null;
            mSurface = null;
            mStartCompletionSemaphore = null;
        }

        Surface getWindowSurface(int windowIndex) {
            return mWindows.get(windowIndex).getSurface();
        }

        void recreateWindows() throws Exception {
            mRecreationCompletionSemaphore = new Semaphore(0);
            Message msg = mHandler.obtainMessage(CompositionHandler.DO_RECREATE_WINDOWS);
            mHandler.sendMessage(msg);
            mRecreationCompletionSemaphore.acquire();
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surface) {
            if (DBG) {
                Log.i(TAG, "onFrameAvailable " + surface);
            }
            GlWindow w = mSurfaceTextureToWindowMap.get(surface);
            if (w != null) {
                w.markTextureUpdated();
                requestUpdate();
            } else {
                Log.w(TAG, "cannot map Surface " + surface + " to window");
            }
        }

        private void requestUpdate() {
            Message msg = mHandler.obtainMessage(CompositionHandler.DO_RENDERING);
            mHandler.sendMessage(msg);
        }

        private int loadShader(int shaderType, String source) throws GlException {
            int shader = GLES20.glCreateShader(shaderType);
            checkGlError("glCreateShader type=" + shaderType);
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":");
                Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) throws GlException {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            checkGlError("glCreateProgram");
            if (program == 0) {
                Log.e(TAG, "Could not create program");
            }
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
            return program;
        }

        private void initGl() throws GlException {
            mEglHelper = new InputSurface(mSurface);
            mEglHelper.makeCurrent();
            mGlProgramId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            mGlaPositionHandle = GLES20.glGetAttribLocation(mGlProgramId, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (mGlaPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            mGlaTextureHandle = GLES20.glGetAttribLocation(mGlProgramId, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (mGlaTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }
            mGluMVPMatrixHandle = GLES20.glGetUniformLocation(mGlProgramId, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (mGluMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }
            mGluSTMatrixHandle = GLES20.glGetUniformLocation(mGlProgramId, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (mGluSTMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }
            Matrix.setIdentityM(mMVPMatrix, 0);
            Log.i(TAG, "initGl w:" + mWidth + " h:" + mHeight);
            GLES20.glViewport(0, 0, mWidth, mHeight);
            float[] vMatrix = new float[16];
            float[] projMatrix = new float[16];
            // max window is from (0,0) to (mWidth - 1, mHeight - 1)
            float wMid = mWidth / 2f;
            float hMid = mHeight / 2f;
            // look from positive z to hide windows in lower z
            Matrix.setLookAtM(vMatrix, 0, wMid, hMid, 5f, wMid, hMid, 0f, 0f, 1.0f, 0.0f);
            Matrix.orthoM(projMatrix, 0, -wMid, wMid, -hMid, hMid, 1, 10);
            Matrix.multiplyMM(mMVPMatrix, 0, projMatrix, 0, vMatrix, 0);
            createWindows();
        }

        private void createWindows() throws GlException {
            // windows placed horizontally
            int windowWidth = mWidth / mNumWindows;
            for (int i = 0; i < mNumWindows; i++) {
                GlWindow window = new GlWindow(this, i * windowWidth, 0, windowWidth, mHeight);
                window.init();
                mSurfaceTextureToWindowMap.put(window.getSurfaceTexture(), window);
                mWindows.add(window);
            }
        }

        private void cleanupGl() {
            for (GlWindow w: mWindows) {
                w.cleanup();
            }
            mWindows.clear();
            mSurfaceTextureToWindowMap.clear();
            if (mEglHelper != null) {
                mEglHelper.release();
            }
        }

        private void doGlRendering() throws GlException {
            if (DBG) {
                Log.i(TAG, "doGlRendering");
            }
            for (GlWindow w: mWindows) {
                w.updateTexImageIfNecessary();
            }
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mGlProgramId);
            for (GlWindow w: mWindows) {
                GLES20.glUniformMatrix4fv(mGluMVPMatrixHandle, 1, false, mMVPMatrix, 0);
                w.onDraw(mGluSTMatrixHandle, mGlaPositionHandle, mGlaTextureHandle);
                checkGlError("window draw");
            }
            mEglHelper.swapBuffers();
        }
        private void doRecreateWindows() throws GlException {
            for (GlWindow w: mWindows) {
                w.cleanup();
            }
            mWindows.clear();
            mSurfaceTextureToWindowMap.clear();
            createWindows();
            mRecreationCompletionSemaphore.release();
        }

        private void waitForStartCompletion() {
            try {
                mStartCompletionSemaphore.acquire();
            } catch (InterruptedException e) {
                //ignore
            }
            mStartCompletionSemaphore = null;
        }

        private class CompositionRunnable implements Runnable {
            @Override
            public void run() {
                try {
                    initGl();
                    Looper.prepare();
                    mLooper = Looper.myLooper();
                    mHandler = new CompositionHandler();
                    // init done
                    mStartCompletionSemaphore.release();
                    Looper.loop();
                } catch (GlException e) {
                    // ignore and clean-up
                } finally {
                    cleanupGl();
                    mHandler = null;
                    mLooper = null;
                }
            }
        }

        private class CompositionHandler extends Handler {
            private static final int DO_RENDERING = 1;
            private static final int DO_RECREATE_WINDOWS = 2;

            @Override
            public void handleMessage(Message msg) {
                try {
                    switch(msg.what) {
                        case DO_RENDERING: {
                            doGlRendering();
                        } break;
                        case DO_RECREATE_WINDOWS: {
                            doRecreateWindows();
                        } break;
                    }
                } catch (GlException e) {
                    // should stop rendering
                    mLooper.quit();
                }
            }
        }

        private class GlWindow {
            private static final int FLOAT_SIZE_BYTES = 4;
            private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
            private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
            private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
            private int mBlX;
            private int mBlY;
            private int mWidth;
            private int mHeight;
            private int mTextureId = 0; // 0 is invalid
            private volatile SurfaceTexture mSurfaceTexture;
            private volatile Surface mSurface;
            private FloatBuffer mVerticesData;
            private float[] mSTMatrix = new float[16];
            private AtomicBoolean mTextureUpdated = new AtomicBoolean(false);
            private GlCompositor mCompositor;

            /**
             * @param blX X coordinate of bottom-left point of window
             * @param blY Y coordinate of bottom-left point of window
             * @param w window width
             * @param h window height
             */
            public GlWindow(GlCompositor compositor, int blX, int blY, int w, int h) {
                mCompositor = compositor;
                mBlX = blX;
                mBlY = blY;
                mWidth = w;
                mHeight = h;
                int trX = blX + w;
                int trY = blY + h;
                float[] vertices = new float[] {
                        // x, y, z, u, v
                        mBlX, mBlY, 0, 0, 0,
                        trX, mBlY, 0, 1, 0,
                        mBlX, trY, 0, 0, 1,
                        trX, trY, 0, 1, 1
                };
                Log.i(TAG, "create window " + this + " blX:" + mBlX + " blY:" + mBlY + " trX:" +
                        trX + " trY:" + trY);
                mVerticesData = ByteBuffer.allocateDirect(
                        vertices.length * FLOAT_SIZE_BYTES)
                                .order(ByteOrder.nativeOrder()).asFloatBuffer();
                mVerticesData.put(vertices).position(0);
            }

            /**
             * initialize the window for composition. counter-part is cleanup()
             * @throws GlException
             */
            public void init() throws GlException {
                int[] textures = new int[1];
                GLES20.glGenTextures(1, textures, 0);

                mTextureId = textures[0];
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
                checkGlError("glBindTexture mTextureID");

                GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                        GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                        GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                        GLES20.GL_CLAMP_TO_EDGE);
                checkGlError("glTexParameter");
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
                mSurface = new Surface(mSurfaceTexture);
                mSurfaceTexture.setOnFrameAvailableListener(mCompositor);
            }

            public void cleanup() {
                mTextureUpdated.set(false);
                if (mTextureId != 0) {
                    int[] textures = new int[] {
                            mTextureId
                    };
                    GLES20.glDeleteTextures(1, textures, 0);
                }
                GLES20.glFinish();
                mSurface.release();
                mSurface = null;
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }

            /**
             * make texture as updated so that it can be updated in the next rendering.
             */
            public void markTextureUpdated() {
                mTextureUpdated.set(true);
            }

            /**
             * update texture for rendering if it is updated.
             */
            public void updateTexImageIfNecessary() {
                if (mTextureUpdated.getAndSet(false)) {
                    if (DBG) {
                        Log.i(TAG, "updateTexImageIfNecessary " + this);
                    }
                    mSurfaceTexture.updateTexImage();
                    mSurfaceTexture.getTransformMatrix(mSTMatrix);
                }
            }

            /**
             * draw the window. It will not be drawn at all if the window is not visible.
             * @param uSTMatrixHandle shader handler for the STMatrix for texture coordinates
             * mapping
             * @param aPositionHandle shader handle for vertex position.
             * @param aTextureHandle shader handle for texture
             */
            public void onDraw(int uSTMatrixHandle, int aPositionHandle, int aTextureHandle) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);
                mVerticesData.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
                GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mVerticesData);
                GLES20.glEnableVertexAttribArray(aPositionHandle);

                mVerticesData.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
                GLES20.glVertexAttribPointer(aTextureHandle, 2, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mVerticesData);
                GLES20.glEnableVertexAttribArray(aTextureHandle);
                GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, mSTMatrix, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            }

            public SurfaceTexture getSurfaceTexture() {
                return mSurfaceTexture;
            }

            public Surface getSurface() {
                return mSurface;
            }
        }
    }

    static void checkGlError(String op) throws GlException {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new GlException(op + ": glError " + error);
        }
    }

    public static class GlException extends Exception {
        public GlException(String msg) {
            super(msg);
        }
    }

    private class VirtualDisplayPresentation {
        public static final int RENDERING_OPENGL = 0;
        public static final int RENDERING_VIEW_HIERARCHY = 1;

        private Context mContext;
        private Surface mSurface;
        private int mWidth;
        private int mHeight;
        private int mRenderingType;
        private final DisplayManager mDisplayManager;
        private VirtualDisplay mVirtualDisplay;
        private TestPresentation mPresentation;

        VirtualDisplayPresentation(Context context, Surface surface, int w, int h,
                int renderingType) {
            mContext = context;
            mSurface = surface;
            mWidth = w;
            mHeight = h;
            mRenderingType = renderingType;
            mDisplayManager = (DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
        }

        void createVirtualDisplay() {
            runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mVirtualDisplay = mDisplayManager.createVirtualDisplay(
                            TAG, mWidth, mHeight, 200, mSurface, 0);
                }
            });
        }

        void destroyVirtualDisplay() {
            runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mVirtualDisplay.release();
                }
            });
        }

        void createPresentation() {
            runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mPresentation = new TestPresentation(getContext(),
                            mVirtualDisplay.getDisplay());
                    mPresentation.show();
                }
            });
        }

        void dismissPresentation() {
            runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mPresentation.dismiss();
                }
            });
        }

        void doRendering() {
            runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    mPresentation.doRendering();
                }
            });
        }

        private class TestPresentation extends Presentation {
            private TextView mTextView;
            private int mRenderingCount = 0;

            public TestPresentation(Context outerContext, Display display) {
                super(outerContext, display);
                getWindow().setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION);
            }

            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                if (VirtualDisplayPresentation.this.mRenderingType == RENDERING_OPENGL) {
                    //TODO add init for opengl renderer
                } else {
                    mTextView = new TextView(getContext());
                    mTextView.setTextSize(14);
                    mTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    mTextView.setText(Integer.toString(mRenderingCount));
                    setContentView(mTextView);
                }
            }

            public void doRendering() {
                if (VirtualDisplayPresentation.this.mRenderingType == RENDERING_OPENGL) {
                    //TODO add opengl rendering
                } else {
                    mRenderingCount++;
                    mTextView.setText(Integer.toString(mRenderingCount));
                }
            }
        }
    }

    private static class CodecInfo {
        public int mMaxW;
        public int mMaxH;
        public int mFps;
        public int mBitRate;
        public String mCodecName;
    };
    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static CodecInfo getAvcSupportedFormatInfo() {
        MediaCodecInfo mediaCodecInfo = selectCodec(MIME_TYPE);
        CodecCapabilities cap = mediaCodecInfo.getCapabilitiesForType(MIME_TYPE);
        if (cap == null) { // not supported
            return null;
        }
        CodecInfo info = new CodecInfo();
        int highestLevel = 0;
        for (CodecProfileLevel lvl : cap.profileLevels) {
            if (lvl.level > highestLevel) {
                highestLevel = lvl.level;
            }
        }
        int maxW = 0;
        int maxH = 0;
        int bitRate = 0;
        int fps = 0; // frame rate for the max resolution
        switch(highestLevel) {
            // Do not support Level 1 to 2.
            case CodecProfileLevel.AVCLevel1:
            case CodecProfileLevel.AVCLevel11:
            case CodecProfileLevel.AVCLevel12:
            case CodecProfileLevel.AVCLevel13:
            case CodecProfileLevel.AVCLevel1b:
            case CodecProfileLevel.AVCLevel2:
                return null;
            case CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                fps = 25;
                break;
            case CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                fps = 15;
                break;
            case CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                fps = 60;
                break;
            case CodecProfileLevel.AVCLevel4: // only try up to 1080p
            default:
                maxW = 1920;
                maxH = 1080;
                bitRate = 20000000;
                fps = 30;
                break;
        }
        info.mMaxW = maxW;
        info.mMaxH = maxH;
        info.mFps = fps;
        info.mBitRate = bitRate;
        info.mCodecName = mediaCodecInfo.getName();
        Log.i(TAG, "AVC Level 0x" + Integer.toHexString(highestLevel) + " bit rate " + bitRate +
                " fps " + info.mFps + " w " + maxW + " h " + maxH);

        return info;
    }

    public void runOnMainSync(Runnable runner) {
        SyncRunnable sr = new SyncRunnable(runner);
        mHandler.post(sr);
        sr.waitForComplete();
    }

    private static final class SyncRunnable implements Runnable {
        private final Runnable mTarget;
        private boolean mComplete;

        public SyncRunnable(Runnable target) {
            mTarget = target;
        }

        public void run() {
            mTarget.run();
            synchronized (this) {
                mComplete = true;
                notifyAll();
            }
        }

        public void waitForComplete() {
            synchronized (this) {
                while (!mComplete) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
