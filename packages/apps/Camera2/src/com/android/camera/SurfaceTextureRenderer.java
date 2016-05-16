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

package com.android.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceTextureRenderer {

    public interface FrameDrawer {
        public void onDrawFrame(GL10 gl);
    }

    private static final String TAG = "CAM_" + SurfaceTextureRenderer.class.getSimpleName();
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    private EGLConfig mEglConfig;
    private EGLDisplay mEglDisplay;
    private EGLContext mEglContext;
    private EGLSurface mEglSurface;
    private EGL10 mEgl;
    private GL10 mGl;

    private Handler mEglHandler;
    private FrameDrawer mFrameDrawer;

    private Object mRenderLock = new Object();
    private Runnable mRenderTask = new Runnable() {
        @Override
        public void run() {
            synchronized (mRenderLock) {
                if (mEglDisplay != null && mEglSurface != null) {
                    mFrameDrawer.onDrawFrame(mGl);
                    mEgl.eglSwapBuffers(mEglDisplay, mEglSurface);
                }
                mRenderLock.notifyAll();
            }
        }
    };

    public class RenderThread extends Thread {
        private Boolean mRenderStopped = false;

        @Override
        public void run() {
            while (true) {
                synchronized (mRenderStopped) {
                    if (mRenderStopped) return;
                }
                draw(true);
            }
        }

        public void stopRender() {
            synchronized (mRenderStopped) {
                mRenderStopped = true;
            }
        }
    }

    public SurfaceTextureRenderer(SurfaceTexture tex,
            Handler handler, FrameDrawer renderer) {
        mEglHandler = handler;
        mFrameDrawer = renderer;

        initialize(tex);
    }

    public RenderThread createRenderThread() {
        return new RenderThread();
    }

    public void release() {
        mEglHandler.post(new Runnable() {
            @Override
            public void run() {
                mEgl.eglDestroySurface(mEglDisplay, mEglSurface);
                mEgl.eglDestroyContext(mEglDisplay, mEglContext);
                mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT);
                mEgl.eglTerminate(mEglDisplay);
                mEglSurface = null;
                mEglContext = null;
                mEglDisplay = null;
            }
        });
    }

    /**
     * Posts a render request to the GL thread.
     * @param sync      set <code>true</code> if the caller needs it to be
     *                  a synchronous call.
     */
    public void draw(boolean sync) {
        synchronized (mRenderLock) {
            mEglHandler.post(mRenderTask);
            if (sync) {
                try {
                    mRenderLock.wait();
                } catch (InterruptedException ex) {
                    Log.v(TAG, "RenderLock.wait() interrupted");
                }
            }
        }
    }

    private void initialize(final SurfaceTexture target) {
        mEglHandler.post(new Runnable() {
            @Override
            public void run() {
                mEgl = (EGL10) EGLContext.getEGL();
                mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                    throw new RuntimeException("eglGetDisplay failed");
                }
                int[] version = new int[2];
                if (!mEgl.eglInitialize(mEglDisplay, version)) {
                    throw new RuntimeException("eglInitialize failed");
                } else {
                    Log.v(TAG, "EGL version: " + version[0] + '.' + version[1]);
                }
                int[] attribList = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
                mEglConfig = chooseConfig(mEgl, mEglDisplay);
                mEglContext = mEgl.eglCreateContext(
                        mEglDisplay, mEglConfig, EGL10.EGL_NO_CONTEXT, attribList);

                if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
                    throw new RuntimeException("failed to createContext");
                }
                mEglSurface = mEgl.eglCreateWindowSurface(
                        mEglDisplay, mEglConfig, target, null);
                if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
                    throw new RuntimeException("failed to createWindowSurface");
                }

                if (!mEgl.eglMakeCurrent(
                        mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
                    throw new RuntimeException("failed to eglMakeCurrent");
                }

                mGl = (GL10) mEglContext.getGL();
            }
        });
        waitDone();
    }

    private void waitDone() {
        final Object lock = new Object();
        synchronized (lock) {
            mEglHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException ex) {
                Log.v(TAG, "waitDone() interrupted");
            }
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        int error;
        while ((error = egl.eglGetError()) != EGL10.EGL_SUCCESS) {
            Log.e(TAG, String.format("%s: EGL error: 0x%x", prompt, error));
        }
    }

    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int[] CONFIG_SPEC = new int[] {
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 0,
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_NONE
    };

    private static EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] numConfig = new int[1];
        if (!egl.eglChooseConfig(display, CONFIG_SPEC, null, 0, numConfig)) {
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        int numConfigs = numConfig[0];
        if (numConfigs <= 0) {
            throw new IllegalArgumentException("No configs match configSpec");
        }

        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(
                display, CONFIG_SPEC, configs, numConfigs, numConfig)) {
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }

        return configs[0];
    }
}
