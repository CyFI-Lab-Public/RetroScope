package com.android.phasebeam;

import android.app.Service;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class PhaseBeamWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new RenderScriptEngine();
    }

    private class RenderScriptEngine extends Engine {
        private RenderScriptGL mRenderScript = null;
        private PhaseBeamRS mWallpaperRS = null;
        private int mDensityDPI;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            surfaceHolder.setSizeFromLayout();
            surfaceHolder.setFormat(PixelFormat.OPAQUE);

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
            mRenderScript = new RenderScriptGL(PhaseBeamWallpaper.this, sc);
            mRenderScript.setPriority(RenderScript.Priority.NORMAL);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder);
            destroyRenderer();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);

            if (mRenderScript != null) {
                mRenderScript.setSurface(surfaceHolder, width, height);
            }

            if (mWallpaperRS == null) {
                mWallpaperRS = new PhaseBeamRS();
                mWallpaperRS.init(mDensityDPI, mRenderScript, getResources(), width, height);
                mWallpaperRS.start();
            }

            mWallpaperRS.resize(width, height);
        }

        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
                boolean resultRequested) {
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
            // TODO: Uncomment this once we can work out framerate issues
            //mWallpaperRS.setOffset(xOffset, yOffset, xPixelOffset, yPixelOffset);
        }
    }
}