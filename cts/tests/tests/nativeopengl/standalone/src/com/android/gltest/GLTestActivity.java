/*
 * Copyright 2013 The Android Open Source Project
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
package com.android.gltest;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;

public class GLTestActivity extends Activity {

    private class TestThread extends Thread {

        public void run() {
            // it is possible to set the GTest filter flag from here
            // for example "GLTest.ClearColorTest" to run that specific test only
            runTests(null);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            finish();
            System.exit(0);
        }
    }

    private SurfaceView mSurfaceView;

    private SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            setSurface(holder.getSurface());
        }

        public void surfaceCreated(SurfaceHolder holder) {
            setSurface(holder.getSurface());
            Thread t = new TestThread();
            t.start();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (checkCallingOrSelfPermission(android.Manifest.permission.DISABLE_KEYGUARD)
                == PackageManager.PERMISSION_GRANTED) {
            KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.newKeyguardLock("gltest").disableKeyguard();
        }

        super.onCreate(savedInstanceState);

        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(mHolderCallback);
        setContentView(mSurfaceView);
        System.loadLibrary("stlport_shared");
        System.loadLibrary("nativeopengltests");
    }

    private static native void setSurface(Surface surface);
    private static native void runTests(String filter);
}
