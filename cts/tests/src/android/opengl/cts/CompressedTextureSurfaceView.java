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

package android.opengl.cts;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.android.cts.stub.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class CompressedTextureSurfaceView extends GLSurfaceView {
    private static final String TAG = "CompressedTextureSurfaceView";
    private static final int SLEEP_TIME_MS = 1000;

    CompressedTextureRender mRenderer;

    public CompressedTextureSurfaceView(Context context,
                                        Bitmap base,
                                        CompressedTextureLoader.Texture compressed) {
        super(context);

        setEGLContextClientVersion(2);
        mRenderer = new CompressedTextureRender(context, base, compressed);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean getTestPassed() throws InterruptedException {
        return mRenderer.getTestPassed();
    }

    private static class CompressedTextureRender implements GLSurfaceView.Renderer {
        private static String TAG = "CompressedTextureRender";

        private static final int ALLOWED_DELTA = 25;
        private static final int FBO_PIXEL_SIZE_BYTES = 4;
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;

        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "  gl_Position = uMVPMatrix * aPosition;\n" +
                "  vTextureCoord = aTextureCoord.xy;\n" +
                "}\n";

        private final String mFragmentShader =
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;
        private int msTextureHandle;

        private int mColorTargetID;
        private int mFrameBufferObjectID;

        private boolean updateSurface = false;

        private boolean mTestPassed;
        private CountDownLatch mDoneSignal;

        Bitmap mBaseTexture;
        CompressedTextureLoader.Texture mCompressedTexture;

        int mWidth;
        int mHeight;

        ByteBuffer mReadBackBuffer;

        boolean getTestPassed() throws InterruptedException {
            if (!mDoneSignal.await(2000L, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("Coudn't finish drawing frames!");
            }

            return mTestPassed;
        }

        public CompressedTextureRender(Context context,
                                       Bitmap base,
                                       CompressedTextureLoader.Texture compressed) {
            mBaseTexture = base;
            mCompressedTexture = compressed;
            mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);

            int byteBufferSize = mBaseTexture.getWidth() *
                                 mBaseTexture.getHeight() *
                                 FBO_PIXEL_SIZE_BYTES;
            mReadBackBuffer = ByteBuffer.allocateDirect(byteBufferSize);

            mDoneSignal = new CountDownLatch(1);
        }

        private void renderQuad(int textureID) {
            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);

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

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
        }

        private int getUnsignedByte(byte val) {
            return 0xFF & ((int)val);
        }

        private boolean comparePixel(int x, int y) {
            int w = mBaseTexture.getWidth();
            int sampleStart = (y * w + x) * FBO_PIXEL_SIZE_BYTES;

            int R = getUnsignedByte(mReadBackBuffer.get(sampleStart));
            int G = getUnsignedByte(mReadBackBuffer.get(sampleStart + 1));
            int B = getUnsignedByte(mReadBackBuffer.get(sampleStart + 2));

            int original = mBaseTexture.getPixel(x, y);

            int deltaR = Math.abs(R - Color.red(original));
            int deltaG = Math.abs(G - Color.green(original));
            int deltaB = Math.abs(B - Color.blue(original));

            if (deltaR <= ALLOWED_DELTA &&
                deltaG <= ALLOWED_DELTA &&
                deltaB <= ALLOWED_DELTA) {
                return true;
            }

            Log.i("PIXEL DELTA", "R: " + deltaR + " G: " + deltaG + " B: " + deltaB);

            return false;
        }

        private void comparePixels() {
            int w = mBaseTexture.getWidth();
            int h = mBaseTexture.getWidth();
            int wOver4 = w / 4;
            int hOver4 = h / 4;

            // Sample 4 points in the image. Test is designed so that
            // sample areas are low frequency and easy to compare
            boolean sample1Matches = comparePixel(wOver4, hOver4);
            boolean sample2Matches = comparePixel(wOver4 * 3, hOver4);
            boolean sample3Matches = comparePixel(wOver4, hOver4 * 3);
            boolean sample4Matches = comparePixel(wOver4 * 3, hOver4 * 3);

            mTestPassed = sample1Matches && sample2Matches && sample3Matches && sample4Matches;
            mDoneSignal.countDown();
        }

        public void onDrawFrame(GL10 glUnused) {
            if (mProgram == 0) {
                return;
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObjectID);
            GLES20.glViewport(0, 0, mBaseTexture.getWidth(), mBaseTexture.getHeight());
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderQuad(mTextureID);
            GLES20.glReadPixels(0, 0, mBaseTexture.getWidth(), mBaseTexture.getHeight(),
                                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mReadBackBuffer);
            comparePixels();
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            GLES20.glViewport(0, 0, mWidth, mHeight);
            GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
            GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            renderQuad(mColorTargetID);

            GLES20.glFinish();
        }

        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        private void setupSamplers() {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        }

        private void initFBO() {
            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mColorTargetID = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mColorTargetID);
            checkGlError("glBindTexture mColorTargetID");
            setupSamplers();
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                                mBaseTexture.getWidth(), mBaseTexture.getHeight(), 0,
                                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            checkGlError("glTexImage2D mColorTargetID");

            GLES20.glGenFramebuffers(1, textures, 0);
            mFrameBufferObjectID = textures[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObjectID);

            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                                          GLES20.GL_TEXTURE_2D, mColorTargetID, 0);

            int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
            if(status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Failed to initialize framebuffer object");
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }

        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            if (mCompressedTexture != null && !mCompressedTexture.isSupported()) {
                mTestPassed = true;
                mDoneSignal.countDown();
                return;
            }

            initFBO();

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

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
            checkGlError("glBindTexture mTextureID");
            setupSamplers();

            if (mCompressedTexture == null) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBaseTexture, 0);
                checkGlError("texImage2D mBaseTexture");
            } else {
                GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D,
                                              0,
                                              mCompressedTexture.getFormat(),
                                              mCompressedTexture.getWidth(),
                                              mCompressedTexture.getHeight(),
                                              0,
                                              mCompressedTexture.getData().remaining(),
                                              mCompressedTexture.getData());
                checkGlError("glCompressedTexImage2D mTextureID");
            }
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            updateSurface = true;
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

    }  // End of class CompressedTextureRender.

}  // End of class CompressedTextureSurfaceView.
