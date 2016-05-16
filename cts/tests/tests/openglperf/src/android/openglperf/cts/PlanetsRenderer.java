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

package android.openglperf.cts;

import android.content.Context;
import android.cts.util.WatchDog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGl renderer rendering given number of planets with different GL configuration.
 */
public class PlanetsRenderer implements GLSurfaceViewCustom.Renderer {

    private static final String TAG = "PlanetsRenderer";
    // texture is from
    // http://en.wikipedia.org/wiki/File:Mercator_projection_SW.jpg
    private static final String TEXTURE_FILE = "world_512_512.jpg";
    private static final long EGL_SWAP_BUFFERS_WAIT_TIME_IN_NS = 100 * 1000 * 1000 * 1000L;

    private final Context mContext;
    private final PlanetsRenderingParam mParam;
    private final RenderCompletionListener mListener;
    private final WatchDog mWatchDog;

    private final Sphere[] mSpheres;
    private final int mNumSpheres;
    private final int mNumIndices;
    private final int mVboVertices[];
    private final int mVboIndices[];

    // configurations for sun and planets
    private static final int SPHERE_SLICES = 180;
    private static final float RADIUS_SUN = 0.4f;
    private static final float RADIUS_PLANET = 0.08f;
    private static final float RADIUS_ORBIT = 0.9f;

    private int mWidth;
    private int mHeight;

    private int mFrameCount = 0;
    private static final int FPS_DISPLAY_INTERVAL = 50;
    private long mLastFPSTime;
    private long mLastRenderingTime;
    // for total FPS measurement
    private long mRenderingStartTime;
    private long mMeasurementStartTime;
    private int[] mFrameInterval = null;

    private int mProgram; // shader program
    private int mMVPMatrixHandle;
    private float[] mMVPMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mProjMatrix = new float[16];

    private int mOffsetHandle;
    private static final float[] mDefaultOffset = { 0f, 0f, 0f, 1f };
    private int mPositionHandle;
    private int mTexCoord0Handle;
    private int mTextureHandle;
    private int mTextureId;

    /**
     * @param numSlices
     *            complexity of sphere used. A sphere will have (numSlices + 1)
     *            x (numSlices x 1) much of vertices
     * @param useVbo
     *            whether to use Vertex Buffer Object in rendering or not
     * @param framesToGo
     *            number of frames to render before calling completion to
     *            listener
     * @param listener
     */
    public PlanetsRenderer(Context context, PlanetsRenderingParam param,
            RenderCompletionListener listener, WatchDog watchDog) {
        resetTimer();
        mContext = context;
        mParam = param;
        mWatchDog = watchDog;
        mNumSpheres = mParam.mNumPlanets + 1; // 1 for sun
        mNumIndices = mNumSpheres * mParam.mNumIndicesPerVertex;
        mSpheres = new Sphere[mNumSpheres];

        if (mParam.mNumFrames > 0) {
            mFrameInterval = new int[mParam.mNumFrames];
        }
        printParams();

        // for big model, this construction phase takes time...
        mSpheres[0] = new Sphere(SPHERE_SLICES, 0f, 0f, 0f, RADIUS_SUN,
                mParam.mNumIndicesPerVertex);
        for (int i = 1; i < mNumSpheres; i++) {
            mSpheres[i] = new Sphere(SPHERE_SLICES,
                    RADIUS_ORBIT * (float) Math.sin(((float) i) / (mNumSpheres - 1) * 2 * Math.PI),
                    RADIUS_ORBIT * (float) Math.cos(((float) i) / (mNumSpheres - 1) * 2 * Math.PI),
                    0f, RADIUS_PLANET, mParam.mNumIndicesPerVertex);
        }
        mVboVertices = new int[mNumSpheres];
        mVboIndices = new int[mNumIndices];
        mListener = listener;
        measureTime("construction");
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        mProgram = createProgram(getVertexShader(), getFragmentShader());
        if (mProgram == 0) {
            // error, cannot proceed
            throw new IllegalStateException("createProgram failed");
        }
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mOffsetHandle = GLES20.glGetUniformLocation(mProgram, "uOffset");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mTexCoord0Handle = GLES20.glGetAttribLocation(mProgram, "vTexCoord0");
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");

        // Load the texture
        mTextureId = createTexture2D();
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        mWatchDog.reset();
        long currentTime = System.currentTimeMillis();
        mFrameCount++;
        mLastRenderingTime = currentTime;

        float angle = 0.090f * ((int) (currentTime % 4000L));
        Matrix.setRotateM(mMMatrix, 0, angle, 0, 0, 1.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

        GLES20.glUseProgram(mProgram);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Apply a ModelView Projection transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glUniform4f(mOffsetHandle, mDefaultOffset[0], mDefaultOffset[1],
                mDefaultOffset[2], mDefaultOffset[3]);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoord0Handle);

        // Bind the texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        // Set the sampler texture unit to 0
        GLES20.glUniform1i(mTextureHandle, 0);

        for (int i = 0; i < mNumSpheres; i++) {
            if (mParam.mUseVboForVertices) {
                // generating VBOs for each sphere is not efficient way for drawing
                // multiple spheres
                // But this is done for testing performance with big VBO buffers.
                // So please do not copy this code as it is.
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboVertices[i]);
                // Load the vertex position
                GLES20.glVertexAttribPointer(mPositionHandle, 3,
                        GLES20.GL_FLOAT, false, mSpheres[i].getVeticesStride(),
                        0);
                // Load the texture coordinate
                GLES20.glVertexAttribPointer(mTexCoord0Handle, 3,
                        GLES20.GL_FLOAT, false, mSpheres[i].getVeticesStride(),
                        3 * Sphere.FLOAT_SIZE);
            } else {
                // Load the vertex position
                GLES20.glVertexAttribPointer(mPositionHandle, 3,
                        GLES20.GL_FLOAT, false, mSpheres[i].getVeticesStride(),
                        mSpheres[i].getVertices());
                // Load the texture coordinate
                GLES20.glVertexAttribPointer(mTexCoord0Handle, 3,
                        GLES20.GL_FLOAT, false, mSpheres[i].getVeticesStride(),
                        mSpheres[i].getVertices().duplicate().position(3));
            }
            int[] numIndices = mSpheres[i].getNumIndices();
            ShortBuffer[] indices = mSpheres[i].getIndices();
            if (mParam.mUseVboForIndices) {
                int indexVboBase = i * mParam.mNumIndicesPerVertex;
                for (int j = 0; j < numIndices.length; j++) {
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                            mVboIndices[indexVboBase + j]);
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                            numIndices[j], GLES20.GL_UNSIGNED_SHORT,
                            0);
                }
            } else {
                for (int j = 0; j < numIndices.length; j++) {
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                            numIndices[j], GLES20.GL_UNSIGNED_SHORT,
                            indices[j]);
                }
            }
        }
    }

    @Override
    public void onEglSwapBuffers() {
        if (!OpenGlPerfNative.waitForEglCompletion(EGL_SWAP_BUFFERS_WAIT_TIME_IN_NS)) {
            Log.w(TAG, "time-out or error while waiting for eglSwapBuffers completion");
        }
        long currentTime = System.currentTimeMillis();
        if (mFrameCount == 0) {
            mRenderingStartTime = currentTime;
        }
        if (mFrameCount < mParam.mNumFrames) {
            mFrameInterval[mFrameCount] = (int)(currentTime - mLastRenderingTime);
        }

        if ((mFrameCount == mParam.mNumFrames) && (mParam.mNumFrames > 0)) {
            long timePassed = currentTime - mRenderingStartTime;
            float fps = ((float) mParam.mNumFrames) / ((float) timePassed) * 1000.0f;
            printGlInfos();
            printParams();
            int numTriangles = mNumSpheres * mSpheres[0].getTotalIndices() / 3;
            Log.i(TAG, "Final FPS " + fps + " Num triangles " + numTriangles + " start time " +
                    mRenderingStartTime + " finish time " + currentTime);
            if (mListener != null) {
                mListener.onRenderCompletion(fps, numTriangles, mFrameInterval);
                mFrameCount++; // to prevent entering here again
                return;
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        mWidth = width;
        mHeight = height;
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        Matrix.setLookAtM(mVMatrix, 0, 0, 3, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        createVbo();

        // reset timer to remove delays added for FPS calculation.
        mLastFPSTime = System.currentTimeMillis();
        mRenderingStartTime = System.currentTimeMillis();
    }

    protected final String getVertexShader() {
        // simple shader with MVP matrix and text coord
        final String vShaderStr =
                  "uniform mat4 uMVPMatrix;                               \n"
                + "uniform vec4 uOffset;                                  \n"
                + "attribute vec4 vPosition;                              \n"
                + "attribute vec2 vTexCoord0;                             \n"
                + "varying vec2 vTexCoord;                                \n"
                + "void main()                                            \n"
                + "{                                                      \n"
                + "   gl_Position = uMVPMatrix * (vPosition + uOffset);   \n"
                + "   vTexCoord = vTexCoord0;                             \n"
                + "}                                                      \n";
        return vShaderStr;
    }

    protected final String getFragmentShader() {
        // simple shader with one texture for color
        final String fShaderStr =
                  "precision mediump float;                          \n"
                + "varying vec2 vTexCoord;                           \n"
                + "uniform sampler2D sTexture;                       \n"
                + "void main()                                       \n"
                + "{                                                 \n"
                + "  gl_FragColor = texture2D( sTexture, vTexCoord );\n"
                + "}                                                 \n";
        return fShaderStr;
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

    private int createTexture2D() {
        // Texture object handle
        int[] textureId = new int[1];

        InputStream in = null;
        try {
            in = mContext.getAssets().open(TEXTURE_FILE);
            Bitmap bitmap = BitmapFactory.decodeStream(in);

            // Generate a texture object
            GLES20.glGenTextures(1, textureId, 0);

            // Bind the texture object
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

            // Set the filtering mode
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        } catch (IOException e) {
            throw new IllegalStateException("Couldn't load texture '" + TEXTURE_FILE
                    + "'", e);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (IOException e) {
                }
        }

        return textureId[0];
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new IllegalStateException(op + ": glError " + error);
        }
    }

    private void createVbo() {
        resetTimer();
        if (mParam.mUseVboForVertices) {
            GLES20.glGenBuffers(mNumSpheres, mVboVertices, 0);
            checkGlError("glGenBuffers Vertex");
            for (int i = 0; i < mNumSpheres; i++) {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboVertices[i]);
                checkGlError("glBindBuffer Vertex");
                FloatBuffer vertices = mSpheres[i].getVertices();
                GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.limit()
                        * Sphere.FLOAT_SIZE, vertices, GLES20.GL_STATIC_DRAW);
                checkGlError("glBufferData Vertex");
            }
        }
        if (mParam.mUseVboForIndices) {
            GLES20.glGenBuffers(mNumIndices, mVboIndices, 0);
            checkGlError("glGenBuffers Index");
            for (int i = 0; i < mNumSpheres; i++) {
                int[] numIndices = mSpheres[i].getNumIndices();
                ShortBuffer[] indices = mSpheres[i].getIndices();
                int indexVboBase = i * mParam.mNumIndicesPerVertex;
                for (int j = 0; j < numIndices.length; j++) {
                    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                            mVboIndices[indexVboBase + j]);
                    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
                            indices[j].limit() * Sphere.SHORT_SIZE, indices[j],
                            GLES20.GL_STATIC_DRAW);
                    checkGlError("glBufferData Index");

                }
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        measureTime("VBO creation");
    }

    private void resetTimer() {
        mMeasurementStartTime = System.currentTimeMillis();
    }

    private void measureTime(String description) {
        long currentTime = System.currentTimeMillis();
        float timePassedInSecs = (float) (currentTime - mMeasurementStartTime) / 1000f;
        Log.i(TAG, description + " time in secs: " + timePassedInSecs);
    }

    private void printGlInfos() {
        Log.i(TAG, "Vendor " + GLES20.glGetString(GLES20.GL_VENDOR));
        Log.i(TAG, "Version " + GLES20.glGetString(GLES20.GL_VERSION));
        Log.i(TAG, "Renderer " + GLES20.glGetString(GLES20.GL_RENDERER));
        Log.i(TAG, "Extensions " + GLES20.glGetString(GLES20.GL_EXTENSIONS));
    }
    private void printParams() {
        Log.i(TAG, "UseVboForVertex " + mParam.mUseVboForVertices);
        Log.i(TAG, "UseVboForIndex " + mParam.mUseVboForIndices);
        Log.i(TAG, "No Spheres " + mNumSpheres);
        Log.i(TAG, "No indices buffer per vertex " + mParam.mNumIndicesPerVertex);
    }
}
