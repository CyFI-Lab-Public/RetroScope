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

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * {@link Activity} with a {@link GLSurfaceView} that chooses a specific configuration.
 */
public class EglConfigStubActivity extends Activity {

    public static final String CONFIG_ID_EXTRA = "eglConfigId";

    public static final String CONTEXT_CLIENT_VERSION_EXTRA = "eglContextClientVersion";

    private EglConfigGLSurfaceView mView;

    private CountDownLatch mFinishedDrawing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int configId = getConfigId();
        int contextClientVersion = getContextClientVersion();
        setTitle("EGL Config Id: " + configId + " Client Version: " + contextClientVersion);

        mFinishedDrawing = new CountDownLatch(1);
        mView = new EglConfigGLSurfaceView(this, configId, contextClientVersion, new Runnable() {
            @Override
            public void run() {
                mFinishedDrawing.countDown();
            }
        });
        setContentView(mView);
    }

    private int getConfigId() {
        Intent intent = getIntent();
        if (intent != null) {
            return intent.getIntExtra(CONFIG_ID_EXTRA, 0);
        } else {
            return 0;
        }
    }

    private int getContextClientVersion() {
        Intent intent = getIntent();
        if (intent != null) {
            return intent.getIntExtra(CONTEXT_CLIENT_VERSION_EXTRA, 0);
        } else {
            return 0;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.onPause();
    }

    public void waitToFinishDrawing() throws InterruptedException {
        if (!mFinishedDrawing.await(3, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Coudn't finish drawing frames!");
        }
    }
}
