/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * {@link Activity} that queries the device's display attributes to determine what version of
 * OpenGL ES is supported and returns what the GL version string reports.
 */
public class OpenGlEsVersionStubActivity extends Activity {

    private static final String EGL_CONTEXT_CLIENT_VERSION = "eglContextClientVersion";

    /** Timeout to wait for the surface to be created and the version queried. */
    private static final int TIMEOUT_SECONDS = 10;

    /** Version string reported by glGetString. */
    private String mVersionString;

    /** Latch that is unlocked when the activity is done finding the version. */
    private CountDownLatch mSurfaceCreatedLatch = new CountDownLatch(1);

    public static Intent createIntent(int eglContextClientVersion) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.putExtra(EGL_CONTEXT_CLIENT_VERSION, eglContextClientVersion);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GLSurfaceView view = new GLSurfaceView(this);

        Intent intent = getIntent();
        int eglContextClientVersion = intent.getIntExtra(EGL_CONTEXT_CLIENT_VERSION, -1);
        if (eglContextClientVersion > 0) {
            view.setEGLContextClientVersion(eglContextClientVersion);
        }

        view.setRenderer(new Renderer());
        setContentView(view);
    }

    public String getVersionString() throws InterruptedException {
        mSurfaceCreatedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        synchronized (this) {
            return mVersionString;
        }
    }

    private class Renderer implements GLSurfaceView.Renderer {

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            synchronized (OpenGlEsVersionStubActivity.this) {
                try {
                    mVersionString = gl.glGetString(GL10.GL_VERSION);
                } finally {
                    mSurfaceCreatedLatch.countDown();
                }
            }
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
        }

        public void onDrawFrame(GL10 gl) {
        }
    }
}
