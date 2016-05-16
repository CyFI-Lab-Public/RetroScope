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

package android.effect.cts;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.opengl.GLES20;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.*;

public class GLEnv {

    private EGLContext mEGLContext;
    private EGLSurface mEGLSurface;
    private EGLDisplay mEGLDisplay;
    private EGLConfig  mEGLConfig;

    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;

    public GLEnv() {
        EGL10 egl = (EGL10)EGLContext.getEGL();

        mEGLDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        checkForEGLErrors("eglGetDisplay");

        int[] version = new int[2];
        egl.eglInitialize(mEGLDisplay, version);
        int[] configSpec = {
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        egl.eglChooseConfig(mEGLDisplay, configSpec, configs, 1, num_config);
        checkForEGLErrors("eglChooseConfig");
        if (num_config[0] < 1) {
            throw new RuntimeException("Could not find a suitable config for EGL context!");
        }
        mEGLConfig = configs[0];

        int[] attribs = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
        mEGLContext = egl.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, attribs);
        checkForEGLErrors("eglCreateContext");

        int[] surfaceSize = { EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE };
        mEGLSurface = egl.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, surfaceSize);
        checkForEGLErrors("eglCreatePbufferSurface");
    }

    public void makeCurrent() {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        egl.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
        checkForEGLErrors("eglMakeCurrent");
    }

    public int generateTexture() {
        int textures[] = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        return textures[0];
    }

    public int bitmapToTexture(Bitmap bitmap) {
        int texId = generateTexture();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        return texId;
    }

    public void releaseTexture(int texId) {
        int[] textures = new int[1];
        textures[0] = texId;
        GLES20.glDeleteTextures(1, textures, 0);
    }

    public void tearDown() {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        egl.eglDestroySurface(mEGLDisplay, mEGLSurface);
        egl.eglDestroyContext(mEGLDisplay, mEGLContext);
    }

    public void checkForEGLErrors(String operation) {
        EGL10 egl = (EGL10)EGLContext.getEGL();
        int error = egl.eglGetError();
        if (error != EGL10.EGL_SUCCESS) {
            throw new RuntimeException("Operation '" + operation + "' caused EGL error: " + error);
        }
    }

}
