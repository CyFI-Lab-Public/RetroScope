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
package android.textureview.cts;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;

import static android.opengl.GLES20.*;

public class TextureViewSnapshotTestActivity extends Activity
        implements TextureView.SurfaceTextureListener {
    public static int mMaxWaitDelayMs = -1;

    private TextureView mTexView;
    private Thread mProducerThread;
    private final Semaphore mSemaphore = new Semaphore(0);
    private final AtomicBoolean mShouldRender = new AtomicBoolean(true);
    private boolean mPostedSnapshotGrab = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Assert.assertTrue(mMaxWaitDelayMs > 0);
        mTexView = new TextureView(this);
        mTexView.setSurfaceTextureListener(this);
        setContentView(mTexView);
    }

    public Boolean waitForCompletion() {
        Boolean success = false;
        try {
            success = mSemaphore.tryAcquire(mMaxWaitDelayMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }
        return success;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mProducerThread = new GLProducerThread(surface, new GLRendererImpl(),
                mShouldRender, 1000/48, mSemaphore);
        mProducerThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mProducerThread = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (!mPostedSnapshotGrab) {
            Bitmap bitmap = mTexView.getBitmap();
            Assert.assertNotNull(bitmap);
            Assert.assertEquals(mTexView.getWidth(), bitmap.getWidth());
            Assert.assertEquals(mTexView.getHeight(), bitmap.getHeight());
            Assert.assertEquals(Color.RED, bitmap.getPixel(0, 0));
            mShouldRender.set(false);
            mPostedSnapshotGrab = true;
        }
    }

    private static class GLRendererImpl implements GLProducerThread.GLRenderer {
        @Override
        public void drawFrame(int frame) {
            glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
    }
}