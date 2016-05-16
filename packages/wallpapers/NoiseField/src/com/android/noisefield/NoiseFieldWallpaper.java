package com.android.noisefield;

import android.app.Service;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.service.wallpaper.WallpaperService;
import android.service.wallpaper.WallpaperService.Engine;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.util.Log;
import android.view.MotionEvent;

public class NoiseFieldWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new RenderScriptEngine();
    }

    private class RenderScriptEngine extends Engine {
        private RenderScriptGL mRenderScript = null;
        private NoiseFieldRS mWallpaperRS = null;
        private int mDensityDPI;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            surfaceHolder.setSizeFromLayout();
            surfaceHolder.setFormat(PixelFormat.RGB_888);

            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) getApplication().getSystemService(Service.WINDOW_SERVICE))
                    .getDefaultDisplay().getMetrics(metrics);
            mDensityDPI = metrics.densityDpi;
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
            mRenderScript = new RenderScriptGL(NoiseFieldWallpaper.this, sc);
            mRenderScript.setPriority(RenderScript.Priority.LOW);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            destroyRenderer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder,
                                     int format,
                                     int width,
                                     int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);

            if (mRenderScript != null) {
                mRenderScript.setSurface(surfaceHolder, width, height);
            }

            if (mWallpaperRS == null) {
                mWallpaperRS = new NoiseFieldRS();
                mWallpaperRS.init(mDensityDPI, mRenderScript, getResources(), width, height);
                mWallpaperRS.start();
            }

            mWallpaperRS.resize(width, height);
        }

        @Override
        public void onTouchEvent(MotionEvent ev) {
            super.onTouchEvent(ev);
            if (mWallpaperRS != null) {
                mWallpaperRS.onTouchEvent(ev);
            }
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
    }
}
