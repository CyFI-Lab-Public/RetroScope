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

import android.graphics.SurfaceTexture;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.TextureView;
import android.os.Handler;
import android.os.HandlerThread;

/**
 * Plays a delightful show of colors.
 * <p>
 * This dream performs its rendering using OpenGL on a separate rendering thread.
 * </p>
 */
public class Colors extends DreamService implements TextureView.SurfaceTextureListener {
    static final String TAG = Colors.class.getSimpleName();
    static final boolean DEBUG = false;

    public static void LOG(String fmt, Object... args) {
        if (!DEBUG) return;
        Log.v(TAG, String.format(fmt, args));
    }

    private TextureView mTextureView;

    // The handler thread and handler on which the GL renderer is running.
    private HandlerThread mRendererHandlerThread;
    private Handler mRendererHandler;

    // The current GL renderer, or null if the dream is not running.
    private ColorsGLRenderer mRenderer;

    @Override
    public void onCreate() {
        super.onCreate();

        setInteractive(false);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);

        if (mRendererHandlerThread == null) {
            mRendererHandlerThread = new HandlerThread(TAG);
            mRendererHandlerThread.start();
            mRendererHandler = new Handler(mRendererHandlerThread.getLooper());
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setLowProfile(true);
        setFullscreen(true);
        setContentView(mTextureView);
    }

    @Override
    public void onSurfaceTextureAvailable(final SurfaceTexture surface,
            final int width, final int height) {
        LOG("onSurfaceTextureAvailable(%s, %d, %d)", surface, width, height);

        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.stop();
                }
                mRenderer = new ColorsGLRenderer(surface, width, height);
                mRenderer.start();
            }
        });
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
            final int width, final int height) {
        LOG("onSurfaceTextureSizeChanged(%s, %d, %d)", surface, width, height);

        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.setSize(width, height);
                }
            }
        });
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        LOG("onSurfaceTextureDestroyed(%s)", surface);

        mRendererHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRenderer != null) {
                    mRenderer.stop();
                    mRenderer = null;
                }
                mRendererHandlerThread.quit();
            }
        });

        try {
            mRendererHandlerThread.join();
        } catch (InterruptedException e) {
            LOG("Error while waiting for renderer", e);
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        LOG("onSurfaceTextureUpdated(%s)", surface);
    }
}
