/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.dreams.basic;

import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Choreographer;
import android.os.SystemClock;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import android.opengl.EGL14;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.*;

/**
 * The OpenGL renderer for the {@link Colors} dream.
 * <p>
 * This class is single-threaded.  Its methods must only be called on the
 * rendering thread.
 * </p>
 */
final class ColorsGLRenderer implements Choreographer.FrameCallback {
    static final String TAG = ColorsGLRenderer.class.getSimpleName();
    static final boolean DEBUG = false;

    private static void LOG(String fmt, Object... args) {
        if (!DEBUG) return;
        Log.v(TAG, String.format(fmt, args));
    }

    private final SurfaceTexture mSurface;
    private int mWidth;
    private int mHeight;

    private final Choreographer mChoreographer;

    private Square mSquare;
    private long mLastFrameTime;
    private int mFrameNum = 0;

    // It's so easy to use OpenGLES 2.0!
    private EGL10 mEgl;
    private EGLDisplay mEglDisplay;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;

    public ColorsGLRenderer(SurfaceTexture surface, int width, int height) {
        mSurface = surface;
        mWidth = width;
        mHeight = height;

        mChoreographer = Choreographer.getInstance();
    }

    public void start() {
        initGL();
        mSquare = new Square();

        mFrameNum = 0;
        mChoreographer.postFrameCallback(this);
    }

    public void stop() {
        mChoreographer.removeFrameCallback(this);

        mSquare = null;
        finishGL();
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        mFrameNum += 1;

        // Clear on first frame.
        if (mFrameNum == 1) {
            glClearColor(1f, 0f, 0f, 1.0f);
            if (DEBUG) {
                mLastFrameTime = frameTimeNanos;
            }
        }

        // Draw new frame.
        checkCurrent();

        glViewport(0, 0, mWidth, mHeight);

        if (DEBUG) {
            final long dt = frameTimeNanos - mLastFrameTime;
            final int fps = (int) (1e9f / dt);
            if (0 == (mFrameNum % 10)) {
                LOG("frame %d fps=%d", mFrameNum, fps);
            }
            if (fps < 40) {
                LOG("JANK! (%d ms)", dt);
            }
            mLastFrameTime = frameTimeNanos;
        }

        glClear(GL_COLOR_BUFFER_BIT);
        checkGlError();

        mSquare.draw();

        if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            throw new RuntimeException("Cannot swap buffers");
        }
        checkEglError();

        // Animate.  Post callback to run on next vsync.
        mChoreographer.postFrameCallback(this);
    }

    private void checkCurrent() {
        if (!mEglContext.equals(mEgl.eglGetCurrentContext()) ||
                !mEglSurface.equals(mEgl.eglGetCurrentSurface(EGL10.EGL_DRAW))) {
            if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(mEgl.eglGetError()));
            }
        }
    }

    private void initGL() {
        mEgl = (EGL10) EGLContext.getEGL();

        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        int[] version = new int[2];
        if (!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }

        EGLConfig eglConfig = chooseEglConfig();
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        mEglContext = createContext(mEgl, mEglDisplay, eglConfig);

        mEglSurface = mEgl.eglCreateWindowSurface(mEglDisplay, eglConfig, mSurface, null);

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                return;
            }
            throw new RuntimeException("createWindowSurface failed "
                    + GLUtils.getEGLErrorString(error));
        }

        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new RuntimeException("eglMakeCurrent failed "
                    + GLUtils.getEGLErrorString(mEgl.eglGetError()));
        }
    }

    private void finishGL() {
        mEgl.eglDestroyContext(mEglDisplay, mEglContext);
        mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
    }

    private static EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attrib_list = { EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();
        if (!mEgl.eglChooseConfig(mEglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    GLUtils.getEGLErrorString(mEgl.eglGetError()));
        } else if (configsCount[0] > 0) {
            return configs[0];
        }
        return null;
    }

    private static int[] getConfig() {
        return new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 0,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };
    }

    private static int buildProgram(String vertex, String fragment) {
        int vertexShader = buildShader(vertex, GL_VERTEX_SHADER);
        if (vertexShader == 0) return 0;

        int fragmentShader = buildShader(fragment, GL_FRAGMENT_SHADER);
        if (fragmentShader == 0) return 0;

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        checkGlError();

        glAttachShader(program, fragmentShader);
        checkGlError();

        glLinkProgram(program);
        checkGlError();

        int[] status = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            String error = glGetProgramInfoLog(program);
            Log.d(TAG, "Error while linking program:\n" + error);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            return 0;
        }

        return program;
    }

    private static int buildShader(String source, int type) {
        int shader = glCreateShader(type);

        glShaderSource(shader, source);
        checkGlError();

        glCompileShader(shader);
        checkGlError();

        int[] status = new int[1];
        glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
        if (status[0] != GL_TRUE) {
            String error = glGetShaderInfoLog(shader);
            Log.d(TAG, "Error while compiling shader:\n" + error);
            glDeleteShader(shader);
            return 0;
        }

        return shader;
    }

    private void checkEglError() {
        int error = mEgl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            Log.w(TAG, "EGL error = 0x" + Integer.toHexString(error));
        }
    }

    private static void checkGlError() {
        checkGlError("");
    }

    private static void checkGlError(String what) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            Log.w(TAG, "GL error: (" + what + ") = 0x" + Integer.toHexString(error));
        }
    }

    private final static class Square {
        // Straight from the API guide
        private final String vertexShaderCode =
            "attribute vec4 a_position;" +
            "attribute vec4 a_color;" +
            "varying vec4 v_color;" +
            "void main() {" +
            "  gl_Position = a_position;" +
            "  v_color = a_color;" +
            "}";

        private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec4 v_color;" +
            "void main() {" +
            "  gl_FragColor = v_color;" +
            "}";

        private final FloatBuffer vertexBuffer;
        private final FloatBuffer colorBuffer;
        private final int mProgram;
        private int mPositionHandle;
        private int mColorHandle;

        private ShortBuffer drawListBuffer;


        // number of coordinates per vertex in this array
        final int COORDS_PER_VERTEX = 3;
        float squareCoords[] = { -1f,  1f, 0f,   // top left
                                 -1f, -1f, 0f,   // bottom left
                                  1f, -1f, 0f,   // bottom right
                                  1f,  1f, 0f }; // top right

        private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices (CCW)

        private final float HUES[] = { // reverse order due to CCW winding
                60,  // yellow
                120, // green
                343, // red
                200, // blue
        };

        private final int vertexCount = squareCoords.length / COORDS_PER_VERTEX;
        private final int vertexStride = COORDS_PER_VERTEX * 4; // bytes per vertex

        private float cornerFrequencies[] = new float[vertexCount];
        private int cornerRotation;

        final int COLOR_PLANES_PER_VERTEX = 4;
        private final int colorStride = COLOR_PLANES_PER_VERTEX * 4; // bytes per vertex

        // Set color with red, green, blue and alpha (opacity) values
        // float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

        public Square() {
            for (int i=0; i<vertexCount; i++) {
                cornerFrequencies[i] = 1f + (float)(Math.random() * 5);
            }
            cornerRotation = (int)(Math.random() * vertexCount);
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 4 bytes per float)
                    squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            bb = ByteBuffer.allocateDirect(vertexCount * colorStride);
            bb.order(ByteOrder.nativeOrder());
            colorBuffer = bb.asFloatBuffer();

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 2 bytes per short)
                    drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);

            mProgram = buildProgram(vertexShaderCode, fragmentShaderCode);

            // Add program to OpenGL environment
            glUseProgram(mProgram);
            checkGlError("glUseProgram(" + mProgram + ")");

            // get handle to vertex shader's a_position member
            mPositionHandle = glGetAttribLocation(mProgram, "a_position");
            checkGlError("glGetAttribLocation(a_position)");

            // Enable a handle to the triangle vertices
            glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                    GL_FLOAT, false,
                    vertexStride, vertexBuffer);

            mColorHandle = glGetAttribLocation(mProgram, "a_color");
            checkGlError("glGetAttribLocation(a_color)");
            glEnableVertexAttribArray(mColorHandle);
            checkGlError("glEnableVertexAttribArray");
        }

        final float[] _tmphsv = new float[3];
        public void draw() {
            // same thing for colors
            long now = SystemClock.uptimeMillis();
            colorBuffer.clear();
            final float t = now / 4000f; // set the base period to 4sec
            for(int i=0; i<vertexCount; i++) {
                final float freq = (float) Math.sin(2 * Math.PI * t / cornerFrequencies[i]);
                _tmphsv[0] = HUES[(i + cornerRotation) % vertexCount];
                _tmphsv[1] = 1f;
                _tmphsv[2] = freq * 0.25f + 0.75f;
                final int c = Color.HSVToColor(_tmphsv);
                colorBuffer.put((float)((c & 0xFF0000) >> 16) / 0xFF);
                colorBuffer.put((float)((c & 0x00FF00) >> 8) / 0xFF);
                colorBuffer.put((float)(c & 0x0000FF) / 0xFF);
                colorBuffer.put(/*a*/ 1f);
            }
            colorBuffer.position(0);
            glVertexAttribPointer(mColorHandle, COLOR_PLANES_PER_VERTEX,
                    GL_FLOAT, false,
                    colorStride, colorBuffer);
            checkGlError("glVertexAttribPointer");

            // Draw the triangle
            glDrawArrays(GL_TRIANGLE_FAN, 0, vertexCount);
        }
    }
}
