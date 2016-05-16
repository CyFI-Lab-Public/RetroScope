/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.wallpaper.walkaround;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.hardware.Camera;
import android.util.Log;
import android.util.DisplayMetrics;
import android.graphics.Canvas;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.io.IOException;
import java.util.List;

public class WalkAroundWallpaper extends WallpaperService {
    private static final String LOG_TAG = "WalkAround";

    private Camera mCamera;
    private WalkAroundEngine mOwner;

    public Engine onCreateEngine() {
        return mOwner = new WalkAroundEngine();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mCamera != null) {
            if (mCamera.previewEnabled()) {
                boolean portrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
                final Camera.Parameters params = mCamera.getParameters();
                params.set("orientation", portrait ? "portrait" : "landscape");
                mCamera.setParameters(params);

                if (mCamera.previewEnabled()) mCamera.stopPreview();
                mCamera.startPreview();
            }
        }
    }

    private void startCamera() {
        if (mCamera == null) {
            mCamera = Camera.open();
        } else {
            try {
                mCamera.reconnect();
            } catch (IOException e) {
                mCamera.release();
                mCamera = null;

                Log.e(LOG_TAG, "Error opening the camera", e);
            }
        }
    }
    
    private void stopCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // Ignore
            }

            try {
                mCamera.release();
            } catch (Exception e) {
                // Ignore
            }

            mCamera = null;
        }
    }

    class WalkAroundEngine extends Engine {
        private SurfaceHolder mHolder;

        WalkAroundEngine() {
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mHolder = surfaceHolder;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (!visible) {
                if (mOwner == this) {
                    stopCamera();
                }
            } else {
                try {
                    startCamera();
                    mCamera.setPreviewDisplay(mHolder);
                    startPreview();
                } catch (IOException e) {
                    mCamera.release();
                    mCamera = null;
    
                    Log.e(LOG_TAG, "Error opening the camera", e);                    
                }
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            if (holder.isCreating()) {
                try {
                    if (mCamera.previewEnabled()) mCamera.stopPreview();
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    mCamera.release();
                    mCamera = null;
    
                    Log.e(LOG_TAG, "Error opening the camera", e);
                }
            }

            if (isVisible()) startPreview();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
            startCamera();
        }

        private void startPreview() {
            final Resources resources = getResources();
            final boolean portrait = resources.getConfiguration().orientation ==
                    Configuration.ORIENTATION_PORTRAIT;

            final Camera.Parameters params = mCamera.getParameters();
            
            final DisplayMetrics metrics = resources.getDisplayMetrics();
            final List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            // Try to find a preview size that matches the screen first
            boolean found = false;
            for (Camera.Size size : sizes) {
                if ((portrait &&
                        size.width == metrics.heightPixels && size.height == metrics.widthPixels) ||
                    (!portrait &&
                        size.width == metrics.widthPixels && size.height == metrics.heightPixels)) {
                    params.setPreviewSize(size.width, size.height);
                    found = true;
                }
            }

            // If no suitable preview size was found, try to find something large enough 
            if (!found) {
                for (Camera.Size size : sizes) {
                    if (size.width >= metrics.widthPixels && size.height >= metrics.heightPixels) {
                        params.setPreviewSize(size.width, size.height);
                        found = true;
                    }
                }
            }

            // If no suitable preview size was found, pick the first one
            if (!found) {
                // Fill the canvas with black
                Canvas canvas = null;
                try {
                    canvas = mHolder.lockCanvas();
                    if (canvas != null) canvas.drawColor(0);
                } finally {
                    if (canvas != null) mHolder.unlockCanvasAndPost(canvas);
                }


                Camera.Size size = sizes.get(0);
                params.setPreviewSize(size.width, size.height);
            }

            params.set("orientation", portrait ? "portrait" : "landscape");
            mCamera.setParameters(params);

            mCamera.startPreview();
        }
    }
}
