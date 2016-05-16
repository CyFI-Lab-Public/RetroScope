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

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * {@link GLSurfaceView} that uses the EGL configuration specified. Draws a couple frames of a red
 * triangle and then calls the callback.
 */
public class EglConfigGLSurfaceView extends GLSurfaceView {

    private static final String TAG = EglConfigGLSurfaceView.class.getName();

    private final int mConfigId;

    private final Runnable mCallback;

    public EglConfigGLSurfaceView(Context context, int configId, int contextClientVersion,
            Runnable callback) {
        super(context);
        mConfigId = configId;
        mCallback = callback;
        setEGLConfigChooser(new ConfigChooser());
        setEGLContextClientVersion(contextClientVersion);
        setRenderer(contextClientVersion == 1
                ? new Renderer()
                : new Renderer20());
    }

    private class ConfigChooser implements GLSurfaceView.EGLConfigChooser {

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] attributeList = new int[] {
                    EGL10.EGL_CONFIG_ID, mConfigId,
                    EGL10.EGL_NONE
            };

            EGLConfig[] configs = new EGLConfig[1];
            if (egl.eglChooseConfig(display, attributeList, configs, 1, new int[] {1})) {
                // Print out the configuration since we may crash...
                printConfig(egl, display, configs[0]);
                return configs[0];
            } else {
                throw new IllegalStateException("Could not get EGL config...");
            }
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {

        private int mNumFrames;

        private FloatBuffer mFloatBuffer;

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            float[] triangleVertices = {
                    0.0f, 1.0f, -1.0f,
                    -1.0f, -1.0f, -1.0f,
                    1.0f, -1.0f, -1.0f
            };

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(triangleVertices.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());

            mFloatBuffer = ByteBuffer.allocateDirect(triangleVertices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mFloatBuffer.put(triangleVertices).position(0);

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        }

        public void onDrawFrame(GL10 gl) {
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            gl.glColor4f(1.0f, 0, 0, 0);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFloatBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 9);

            if (++mNumFrames == 10) {
                post(mCallback);
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
        }
    }

    private class Renderer20 implements GLSurfaceView.Renderer {

        private FloatBuffer mFloatBuffer;

        private final String mVertexShader =
            "attribute vec4 aPosition;\n" +
            "void main() {\n" +
            "  gl_Position = aPosition;\n" +
            "}\n";

        private final String mFragmentShader =
            "void main() {\n" +
            "  gl_FragColor = vec4(0.0, 0.0, 1.0, 1.0);\n" +
            "}\n";

        private int mProgram;

        private int maPositionHandle;

        private int mNumFrames;

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                throw new RuntimeException("Could not create program");
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }

            float[] triangleVertices = {
                    0.0f, 1.0f, 0.0f,
                    -1.0f, -1.0f, 0.0f,
                    1.0f, -1.0f, 0.0f
            };

            mFloatBuffer = ByteBuffer.allocateDirect(triangleVertices.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            mFloatBuffer.put(triangleVertices).position(0);
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

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);

            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    0, mFloatBuffer);
            checkGlError("glVertexAttribPointer maPosition");

            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
            checkGlError("glDrawArrays");

            if (++mNumFrames == 10) {
                post(mCallback);
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }
    }

    /** Ripped from the NDK sample GL2JNIView class. */
    private static void printConfig(EGL10 egl, EGLDisplay display,
            EGLConfig config) {
        int[] attributes = {
                EGL10.EGL_BUFFER_SIZE,
                EGL10.EGL_ALPHA_SIZE,
                EGL10.EGL_BLUE_SIZE,
                EGL10.EGL_GREEN_SIZE,
                EGL10.EGL_RED_SIZE,
                EGL10.EGL_DEPTH_SIZE,
                EGL10.EGL_STENCIL_SIZE,
                EGL10.EGL_CONFIG_CAVEAT,
                EGL10.EGL_CONFIG_ID,
                EGL10.EGL_LEVEL,
                EGL10.EGL_MAX_PBUFFER_HEIGHT,
                EGL10.EGL_MAX_PBUFFER_PIXELS,
                EGL10.EGL_MAX_PBUFFER_WIDTH,
                EGL10.EGL_NATIVE_RENDERABLE,
                EGL10.EGL_NATIVE_VISUAL_ID,
                EGL10.EGL_NATIVE_VISUAL_TYPE,
                0x3030, // EGL10.EGL_PRESERVED_RESOURCES,
                EGL10.EGL_SAMPLES,
                EGL10.EGL_SAMPLE_BUFFERS,
                EGL10.EGL_SURFACE_TYPE,
                EGL10.EGL_TRANSPARENT_TYPE,
                EGL10.EGL_TRANSPARENT_RED_VALUE,
                EGL10.EGL_TRANSPARENT_GREEN_VALUE,
                EGL10.EGL_TRANSPARENT_BLUE_VALUE,
                0x3039, // EGL10.EGL_BIND_TO_TEXTURE_RGB,
                0x303A, // EGL10.EGL_BIND_TO_TEXTURE_RGBA,
                0x303B, // EGL10.EGL_MIN_SWAP_INTERVAL,
                0x303C, // EGL10.EGL_MAX_SWAP_INTERVAL,
                EGL10.EGL_LUMINANCE_SIZE,
                EGL10.EGL_ALPHA_MASK_SIZE,
                EGL10.EGL_COLOR_BUFFER_TYPE,
                EGL10.EGL_RENDERABLE_TYPE,
                0x3042 // EGL10.EGL_CONFORMANT
        };
        String[] names = {
                "EGL_BUFFER_SIZE",
                "EGL_ALPHA_SIZE",
                "EGL_BLUE_SIZE",
                "EGL_GREEN_SIZE",
                "EGL_RED_SIZE",
                "EGL_DEPTH_SIZE",
                "EGL_STENCIL_SIZE",
                "EGL_CONFIG_CAVEAT",
                "EGL_CONFIG_ID",
                "EGL_LEVEL",
                "EGL_MAX_PBUFFER_HEIGHT",
                "EGL_MAX_PBUFFER_PIXELS",
                "EGL_MAX_PBUFFER_WIDTH",
                "EGL_NATIVE_RENDERABLE",
                "EGL_NATIVE_VISUAL_ID",
                "EGL_NATIVE_VISUAL_TYPE",
                "EGL_PRESERVED_RESOURCES",
                "EGL_SAMPLES",
                "EGL_SAMPLE_BUFFERS",
                "EGL_SURFACE_TYPE",
                "EGL_TRANSPARENT_TYPE",
                "EGL_TRANSPARENT_RED_VALUE",
                "EGL_TRANSPARENT_GREEN_VALUE",
                "EGL_TRANSPARENT_BLUE_VALUE",
                "EGL_BIND_TO_TEXTURE_RGB",
                "EGL_BIND_TO_TEXTURE_RGBA",
                "EGL_MIN_SWAP_INTERVAL",
                "EGL_MAX_SWAP_INTERVAL",
                "EGL_LUMINANCE_SIZE",
                "EGL_ALPHA_MASK_SIZE",
                "EGL_COLOR_BUFFER_TYPE",
                "EGL_RENDERABLE_TYPE",
                "EGL_CONFORMANT"
        };
        int[] value = new int[1];
        for (int i = 0; i < attributes.length; i++) {
            int attribute = attributes[i];
            String name = names[i];
            if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
                Log.w(TAG, String.format("  %s: %d\n", name, value[0]));
            } else {
                // Log.w(TAG, String.format("  %s: failed\n", name));
                while (egl.eglGetError() != EGL10.EGL_SUCCESS);
            }
        }
    }
}
