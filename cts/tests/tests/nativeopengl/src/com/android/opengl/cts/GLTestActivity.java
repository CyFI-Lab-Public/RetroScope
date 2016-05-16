/*
 * Copyright 2012 The Android Open Source Project
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

package com.android.opengl.cts;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.test.wrappedgtest.WrappedGTestActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;

public class GLTestActivity extends WrappedGTestActivity {

    private SurfaceView mSurfaceView;
    private SurfaceHolder.Callback mHolderCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
             setSurface(holder.getSurface());
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
             setSurface(holder.getSurface());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };

    public void onCreate(Bundle data) {
        super.onCreate(data);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(mHolderCallback);
        setContentView(mSurfaceView);
        System.loadLibrary("nativeopengltests");
    }

    private static native void setSurface(Surface surface);
}
