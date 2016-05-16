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

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.TextureView;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;

import static android.opengl.GLES20.*;

public class TextureViewTestActivity extends Activity implements TextureView.SurfaceTextureListener {
    public static int mFrames = -1;
    public static int mDelayMs = -1;

    private TextureView mTexView;
    private Thread mProducerThread;
    private final Semaphore mSemaphore = new Semaphore(0);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Assert.assertTrue(mFrames > 0);
        Assert.assertTrue(mDelayMs > 0);
        mTexView = new TextureView(this);
        mTexView.setSurfaceTextureListener(this);
        setContentView(mTexView);
        ObjectAnimator rotate = ObjectAnimator.ofFloat(mTexView, "rotationY", 180);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mTexView, "alpha", 0.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mTexView, "scaleY", 0.3f, 1f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(rotate).with(fadeIn).with(scaleY);
        animSet.setDuration(mFrames * mDelayMs);
        animSet.start();
    }

    public Boolean waitForCompletion() {
        Boolean success = false;
        int timeout = mFrames * mDelayMs * 4;
        try {
            success = mSemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Assert.fail();
        }
        return success;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mProducerThread = new GLProducerThread(surface, new GLRendererImpl(),
                mFrames, mDelayMs, mSemaphore);
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
    }

    public static class GLRendererImpl implements GLProducerThread.GLRenderer {
        private final int numColors = 4;
        private final float[][] color =
            { { 1.0f, 0.0f, 0.0f },
              { 0.0f, 1.0f, 0.0f },
              { 0.0f, 0.0f, 1.0f },
              { 1.0f, 1.0f, 1.0f } };

        @Override
        public void drawFrame(int frame) {
            int index = frame % numColors;
            glClearColor(color[index][0], color[index][1], color[index][2], 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }
    }
}
