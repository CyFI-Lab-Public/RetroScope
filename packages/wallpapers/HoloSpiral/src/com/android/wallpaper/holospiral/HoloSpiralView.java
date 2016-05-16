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

package com.android.wallpaper.holospiral;

import android.content.Context;
import android.graphics.PixelFormat;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;

public class HoloSpiralView extends RSSurfaceView {
    private static final String LOG_TAG = "HoloRSView";
    private boolean mDragging = false;
    private float mStartX = 0;
    private float mCurrentPosition = 0;
    private float mWallpaperWidth = 0;

    public HoloSpiralView(Context context) {
        super(context);
    }

    private RenderScriptGL mRenderScript = null;
    private HoloSpiralRS mWallpaperRS = null;

    public void destroyRenderer() {
        if (mWallpaperRS != null) {
            mWallpaperRS.stop();
            mWallpaperRS = null;
        }

        if (mRenderScript != null) {
            mRenderScript.setSurface(null, 0, 0);
            mRenderScript = null;
            destroyRenderScriptGL();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);

        Surface surface = null;
        while (surface == null) {
            surface = surfaceHolder.getSurface();
        }
        RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
        mRenderScript = createRenderScriptGL(sc);
        mRenderScript.setPriority(RenderScript.Priority.LOW);

        surfaceHolder.setSizeFromLayout();
        surfaceHolder.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
        destroyRenderer();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        super.surfaceChanged(surfaceHolder, format, width, height);
        if (mRenderScript != null) {
            mRenderScript.setSurface(surfaceHolder, width, height);
        }

        if (mWallpaperRS == null) {
            mWallpaperRS = new HoloSpiralRS(mRenderScript, getResources());
            mWallpaperRS.start();
        }

        mWallpaperRS.resize(width, height);
        mWallpaperWidth = width;
    }

    public void onResume() {
        resume();
        if (mWallpaperRS != null) {
            mWallpaperRS.start();
        }
    }

    public void onPause() {
        pause();
        if (mWallpaperRS != null) {
            mWallpaperRS.stop();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroyRenderer();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionIndex() == 0) {
            float realPosition = mCurrentPosition + (event.getX() - mStartX);
            if (realPosition < 0.0f) {
                realPosition = 0.0f;
            } else if (realPosition > mWallpaperWidth * 4) {
                realPosition = mWallpaperWidth * 4;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mDragging = true;
                mStartX = event.getX(0);
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                mDragging = false;
                mCurrentPosition = realPosition;
            } else if (mDragging) {
                float ratio = realPosition / (mWallpaperWidth * 4);
                mWallpaperRS.setOffset(ratio, 0, 0, 0);
            }
            return true;
        }
        return false;
    }
}
