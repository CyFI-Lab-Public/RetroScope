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

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.service.wallpaper.WallpaperService;
import android.view.Surface;
import android.view.SurfaceHolder;

public class HoloSpiralWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new RenderScriptEngine();
    }

    private class RenderScriptEngine extends Engine {
        private RenderScriptGL mRenderScript = null;
        private HoloSpiralRS mWallpaperRS = null;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            surfaceHolder.setSizeFromLayout();
            surfaceHolder.setFormat(PixelFormat.RGBA_8888);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            destroyRenderer();
        }

        public void destroyRenderer() {
            if (mWallpaperRS != null) {
                mWallpaperRS.stop();
                mWallpaperRS = null;
            }

            if (mRenderScript != null) {
                mRenderScript.setSurface(null, 0, 0);
                mRenderScript.destroy();
                mRenderScript = null;
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);

            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            mRenderScript = new RenderScriptGL(HoloSpiralWallpaper.this, sc);
            mRenderScript.setPriority(RenderScript.Priority.LOW);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            destroyRenderer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format,
                int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);

            if (mRenderScript != null) {
                mRenderScript.setSurface(surfaceHolder, width, height);
            }

            if (mWallpaperRS == null) {
                mWallpaperRS = new HoloSpiralRS(mRenderScript, getResources());
                mWallpaperRS.start();
            }

            mWallpaperRS.resize(width, height);
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
                boolean resultRequested) {
            if (mWallpaperRS != null) {
                return mWallpaperRS.onCommand(action, x, y, z, extras, resultRequested);
            }
            return null;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (mWallpaperRS != null) {
                if (visible) {
                    mWallpaperRS.start();
                } else {
                    mWallpaperRS.stop();
                }
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            mWallpaperRS.setOffset(xOffset, yOffset, xPixelOffset, yPixelOffset);
        }
    }
}
