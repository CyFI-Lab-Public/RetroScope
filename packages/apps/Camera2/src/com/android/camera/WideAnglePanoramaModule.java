/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.app.OrientationManager;
import com.android.camera.data.LocalData;
import com.android.camera.exif.ExifInterface;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.UsageStatistics;
import com.android.camera2.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TimeZone;

/**
 * Activity to handle panorama capturing.
 */
public class WideAnglePanoramaModule
        implements CameraModule, WideAnglePanoramaController,
        SurfaceTexture.OnFrameAvailableListener {

    public static final int DEFAULT_SWEEP_ANGLE = 160;
    public static final int DEFAULT_BLEND_MODE = Mosaic.BLENDTYPE_HORIZONTAL;
    public static final int DEFAULT_CAPTURE_PIXELS = 960 * 720;

    private static final int MSG_LOW_RES_FINAL_MOSAIC_READY = 1;
    private static final int MSG_GENERATE_FINAL_MOSAIC_ERROR = 2;
    private static final int MSG_END_DIALOG_RESET_TO_PREVIEW = 3;
    private static final int MSG_CLEAR_SCREEN_DELAY = 4;
    private static final int MSG_RESET_TO_PREVIEW = 5;

    private static final int SCREEN_DELAY = 2 * 60 * 1000;

    @SuppressWarnings("unused")
    private static final String TAG = "CAM_WidePanoModule";
    private static final int PREVIEW_STOPPED = 0;
    private static final int PREVIEW_ACTIVE = 1;
    public static final int CAPTURE_STATE_VIEWFINDER = 0;
    public static final int CAPTURE_STATE_MOSAIC = 1;

    // The unit of speed is degrees per frame.
    private static final float PANNING_SPEED_THRESHOLD = 2.5f;
    private static final boolean DEBUG = false;

    private ContentResolver mContentResolver;
    private WideAnglePanoramaUI mUI;

    private MosaicPreviewRenderer mMosaicPreviewRenderer;
    private Object mRendererLock = new Object();
    private Object mWaitObject = new Object();

    private String mPreparePreviewString;
    private String mDialogTitle;
    private String mDialogOkString;
    private String mDialogPanoramaFailedString;
    private String mDialogWaitingPreviousString;

    private int mPreviewUIWidth;
    private int mPreviewUIHeight;
    private boolean mUsingFrontCamera;
    private int mCameraPreviewWidth;
    private int mCameraPreviewHeight;
    private int mCameraState;
    private int mCaptureState;
    private PowerManager.WakeLock mPartialWakeLock;
    private MosaicFrameProcessor mMosaicFrameProcessor;
    private boolean mMosaicFrameProcessorInitialized;
    private AsyncTask <Void, Void, Void> mWaitProcessorTask;
    private long mTimeTaken;
    private Handler mMainHandler;
    private SurfaceTexture mCameraTexture;
    private boolean mThreadRunning;
    private boolean mCancelComputation;
    private float mHorizontalViewAngle;
    private float mVerticalViewAngle;

    // Prefer FOCUS_MODE_INFINITY to FOCUS_MODE_CONTINUOUS_VIDEO because of
    // getting a better image quality by the former.
    private String mTargetFocusMode = Parameters.FOCUS_MODE_INFINITY;

    private PanoOrientationEventListener mOrientationEventListener;
    // The value could be 0, 90, 180, 270 for the 4 different orientations measured in clockwise
    // respectively.
    private int mDeviceOrientation;
    private int mDeviceOrientationAtCapture;
    private int mCameraOrientation;
    private int mOrientationCompensation;

    private SoundClips.Player mSoundPlayer;

    private Runnable mOnFrameAvailableRunnable;

    private CameraActivity mActivity;
    private View mRootView;
    private CameraProxy mCameraDevice;
    private boolean mPaused;

    private LocationManager mLocationManager;
    private OrientationManager mOrientationManager;
    private ComboPreferences mPreferences;
    private boolean mMosaicPreviewConfigured;
    private boolean mPreviewFocused = true;

    @Override
    public void onPreviewUIReady() {
        configMosaicPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {

    }

    private class MosaicJpeg {
        public MosaicJpeg(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.isValid = true;
        }

        public MosaicJpeg() {
            this.data = null;
            this.width = 0;
            this.height = 0;
            this.isValid = false;
        }

        public final byte[] data;
        public final int width;
        public final int height;
        public final boolean isValid;
    }

    private class PanoOrientationEventListener extends OrientationEventListener {
        public PanoOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mDeviceOrientation = CameraUtil.roundOrientation(orientation, mDeviceOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mDeviceOrientation
                    + CameraUtil.getDisplayRotation(mActivity) % 360;
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
            }
        }
    }

    @Override
    public void init(CameraActivity activity, View parent) {
        mActivity = activity;
        mRootView = parent;

        mOrientationManager = new OrientationManager(activity);
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        mUI = new WideAnglePanoramaUI(mActivity, this, (ViewGroup) mRootView);
        mUI.setCaptureProgressOnDirectionChangeListener(
                new PanoProgressBar.OnDirectionChangeListener() {
                    @Override
                    public void onDirectionChange(int direction) {
                        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
                            mUI.showDirectionIndicators(direction);
                        }
                    }
                });

        mContentResolver = mActivity.getContentResolver();
        // This runs in UI thread.
        mOnFrameAvailableRunnable = new Runnable() {
            @Override
            public void run() {
                // Frames might still be available after the activity is paused.
                // If we call onFrameAvailable after pausing, the GL thread will crash.
                if (mPaused) return;

                MosaicPreviewRenderer renderer = null;
                synchronized (mRendererLock) {
                    if (mMosaicPreviewRenderer == null) {
                        return;
                    }
                    renderer = mMosaicPreviewRenderer;
                }
                if (mRootView.getVisibility() != View.VISIBLE) {
                    renderer.showPreviewFrameSync();
                    mRootView.setVisibility(View.VISIBLE);
                } else {
                    if (mCaptureState == CAPTURE_STATE_VIEWFINDER) {
                        renderer.showPreviewFrame();
                    } else {
                        renderer.alignFrameSync();
                        mMosaicFrameProcessor.processFrame();
                    }
                }
            }
        };

        PowerManager pm = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Panorama");

        mOrientationEventListener = new PanoOrientationEventListener(mActivity);

        mMosaicFrameProcessor = MosaicFrameProcessor.getInstance();

        Resources appRes = mActivity.getResources();
        mPreparePreviewString = appRes.getString(R.string.pano_dialog_prepare_preview);
        mDialogTitle = appRes.getString(R.string.pano_dialog_title);
        mDialogOkString = appRes.getString(R.string.dialog_ok);
        mDialogPanoramaFailedString = appRes.getString(R.string.pano_dialog_panorama_failed);
        mDialogWaitingPreviousString = appRes.getString(R.string.pano_dialog_waiting_previous);

        mPreferences = new ComboPreferences(mActivity);
        CameraSettings.upgradeGlobalPreferences(mPreferences.getGlobal());
        mLocationManager = new LocationManager(mActivity, null);

        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOW_RES_FINAL_MOSAIC_READY:
                        onBackgroundThreadFinished();
                        showFinalMosaic((Bitmap) msg.obj);
                        saveHighResMosaic();
                        break;
                    case MSG_GENERATE_FINAL_MOSAIC_ERROR:
                        onBackgroundThreadFinished();
                        if (mPaused) {
                            resetToPreviewIfPossible();
                        } else {
                            mUI.showAlertDialog(
                                    mDialogTitle, mDialogPanoramaFailedString,
                                    mDialogOkString, new Runnable() {
                                @Override
                                public void run() {
                                    resetToPreviewIfPossible();
                                }
                            });
                        }
                        clearMosaicFrameProcessorIfNeeded();
                        break;
                    case MSG_END_DIALOG_RESET_TO_PREVIEW:
                        onBackgroundThreadFinished();
                        resetToPreviewIfPossible();
                        clearMosaicFrameProcessorIfNeeded();
                        break;
                    case MSG_CLEAR_SCREEN_DELAY:
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.
                                FLAG_KEEP_SCREEN_ON);
                        break;
                    case MSG_RESET_TO_PREVIEW:
                        resetToPreviewIfPossible();
                        break;
                }
            }
        };
    }

    @Override
    public void onPreviewFocusChanged(boolean previewFocused) {
        mPreviewFocused = previewFocused;
        mUI.onPreviewFocusChanged(previewFocused);
    }

    @Override
    public boolean arePreviewControlsVisible() {
        return mUI.arePreviewControlsVisible();
    }

    /**
     * Opens camera and sets the parameters.
     *
     * @return Whether the camera was opened successfully.
     */
    private boolean setupCamera() {
        if (!openCamera()) {
            return false;
        }
        Parameters parameters = mCameraDevice.getParameters();
        setupCaptureParams(parameters);
        configureCamera(parameters);
        return true;
    }

    private void releaseCamera() {
        if (mCameraDevice != null) {
            CameraHolder.instance().release();
            mCameraDevice = null;
            mCameraState = PREVIEW_STOPPED;
        }
    }

    /**
     * Opens the camera device. The back camera has priority over the front
     * one.
     *
     * @return Whether the camera was opened successfully.
     */
    private boolean openCamera() {
        int cameraId = CameraHolder.instance().getBackCameraId();
        // If there is no back camera, use the first camera. Camera id starts
        // from 0. Currently if a camera is not back facing, it is front facing.
        // This is also forward compatible if we have a new facing other than
        // back or front in the future.
        if (cameraId == -1) cameraId = 0;
        mCameraDevice = CameraUtil.openCamera(mActivity, cameraId,
                mMainHandler, mActivity.getCameraOpenErrorCallback());
        if (mCameraDevice == null) {
            return false;
        }
        mCameraOrientation = CameraUtil.getCameraOrientation(cameraId);
        if (cameraId == CameraHolder.instance().getFrontCameraId()) mUsingFrontCamera = true;
        return true;
    }

    private boolean findBestPreviewSize(List<Size> supportedSizes, boolean need4To3,
            boolean needSmaller) {
        int pixelsDiff = DEFAULT_CAPTURE_PIXELS;
        boolean hasFound = false;
        for (Size size : supportedSizes) {
            int h = size.height;
            int w = size.width;
            // we only want 4:3 format.
            int d = DEFAULT_CAPTURE_PIXELS - h * w;
            if (needSmaller && d < 0) { // no bigger preview than 960x720.
                continue;
            }
            if (need4To3 && (h * 4 != w * 3)) {
                continue;
            }
            d = Math.abs(d);
            if (d < pixelsDiff) {
                mCameraPreviewWidth = w;
                mCameraPreviewHeight = h;
                pixelsDiff = d;
                hasFound = true;
            }
        }
        return hasFound;
    }

    private void setupCaptureParams(Parameters parameters) {
        List<Size> supportedSizes = parameters.getSupportedPreviewSizes();
        if (!findBestPreviewSize(supportedSizes, true, true)) {
            Log.w(TAG, "No 4:3 ratio preview size supported.");
            if (!findBestPreviewSize(supportedSizes, false, true)) {
                Log.w(TAG, "Can't find a supported preview size smaller than 960x720.");
                findBestPreviewSize(supportedSizes, false, false);
            }
        }
        Log.d(TAG, "camera preview h = "
                    + mCameraPreviewHeight + " , w = " + mCameraPreviewWidth);
        parameters.setPreviewSize(mCameraPreviewWidth, mCameraPreviewHeight);

        List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
        int last = frameRates.size() - 1;
        int minFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MIN_INDEX];
        int maxFps = (frameRates.get(last))[Parameters.PREVIEW_FPS_MAX_INDEX];
        parameters.setPreviewFpsRange(minFps, maxFps);
        Log.d(TAG, "preview fps: " + minFps + ", " + maxFps);

        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
        if (supportedFocusModes.indexOf(mTargetFocusMode) >= 0) {
            parameters.setFocusMode(mTargetFocusMode);
        } else {
            // Use the default focus mode and log a message
            Log.w(TAG, "Cannot set the focus mode to " + mTargetFocusMode +
                  " becuase the mode is not supported.");
        }

        parameters.set(CameraUtil.RECORDING_HINT, CameraUtil.FALSE);

        mHorizontalViewAngle = parameters.getHorizontalViewAngle();
        mVerticalViewAngle =  parameters.getVerticalViewAngle();
    }

    public int getPreviewBufSize() {
        PixelFormat pixelInfo = new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCameraDevice.getParameters().getPreviewFormat(), pixelInfo);
        // TODO: remove this extra 32 byte after the driver bug is fixed.
        return (mCameraPreviewWidth * mCameraPreviewHeight * pixelInfo.bitsPerPixel / 8) + 32;
    }

    private void configureCamera(Parameters parameters) {
        mCameraDevice.setParameters(parameters);
    }

    /**
     * Configures the preview renderer according to the dimension defined by
     * {@code mPreviewUIWidth} and {@code mPreviewUIHeight}.
     * Will stop the camera preview first.
     */
    private void configMosaicPreview() {
        if (mPreviewUIWidth == 0 || mPreviewUIHeight == 0
                || mUI.getSurfaceTexture() == null) {
            return;
        }

        stopCameraPreview();
        synchronized (mRendererLock) {
            if (mMosaicPreviewRenderer != null) {
                mMosaicPreviewRenderer.release();
            }
            mMosaicPreviewRenderer = null;
        }
        final boolean isLandscape =
                (mActivity.getResources().getConfiguration().orientation ==
                        Configuration.ORIENTATION_LANDSCAPE);
        mUI.flipPreviewIfNeeded();
        MosaicPreviewRenderer renderer = new MosaicPreviewRenderer(
                mUI.getSurfaceTexture(),
                mPreviewUIWidth, mPreviewUIHeight, isLandscape);
        synchronized (mRendererLock) {
            mMosaicPreviewRenderer = renderer;
            mCameraTexture = mMosaicPreviewRenderer.getInputSurfaceTexture();

            if (!mPaused && !mThreadRunning && mWaitProcessorTask == null) {
                mMainHandler.sendEmptyMessage(MSG_RESET_TO_PREVIEW);
            }
            mRendererLock.notifyAll();
        }
        mMosaicPreviewConfigured = true;
        resetToPreviewIfPossible();
    }

    /**
     * Receives the layout change event from the preview area. So we can
     * initialize the mosaic preview renderer.
     */
    @Override
    public void onPreviewUILayoutChange(int l, int t, int r, int b) {
        Log.d(TAG, "layout change: " + (r - l) + "/" + (b - t));
        mPreviewUIWidth = r - l;
        mPreviewUIHeight = b - t;
        configMosaicPreview();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surface) {
        /* This function may be called by some random thread,
         * so let's be safe and jump back to ui thread.
         * No OpenGL calls can be done here. */
        mActivity.runOnUiThread(mOnFrameAvailableRunnable);
    }

    public void startCapture() {
        // Reset values so we can do this again.
        mCancelComputation = false;
        mTimeTaken = System.currentTimeMillis();
        mActivity.setSwipingEnabled(false);
        mCaptureState = CAPTURE_STATE_MOSAIC;
        mUI.onStartCapture();

        mMosaicFrameProcessor.setProgressListener(new MosaicFrameProcessor.ProgressListener() {
            @Override
            public void onProgress(boolean isFinished, float panningRateX, float panningRateY,
                    float progressX, float progressY) {
                float accumulatedHorizontalAngle = progressX * mHorizontalViewAngle;
                float accumulatedVerticalAngle = progressY * mVerticalViewAngle;
                if (isFinished
                        || (Math.abs(accumulatedHorizontalAngle) >= DEFAULT_SWEEP_ANGLE)
                        || (Math.abs(accumulatedVerticalAngle) >= DEFAULT_SWEEP_ANGLE)) {
                    stopCapture(false);
                } else {
                    float panningRateXInDegree = panningRateX * mHorizontalViewAngle;
                    float panningRateYInDegree = panningRateY * mVerticalViewAngle;
                    mUI.updateCaptureProgress(panningRateXInDegree, panningRateYInDegree,
                            accumulatedHorizontalAngle, accumulatedVerticalAngle,
                            PANNING_SPEED_THRESHOLD);
                }
            }
        });

        mUI.resetCaptureProgress();
        // TODO: calculate the indicator width according to different devices to reflect the actual
        // angle of view of the camera device.
        mUI.setMaxCaptureProgress(DEFAULT_SWEEP_ANGLE);
        mUI.showCaptureProgress();
        mDeviceOrientationAtCapture = mDeviceOrientation;
        keepScreenOn();
        // TODO: mActivity.getOrientationManager().lockOrientation();
        mOrientationManager.lockOrientation();
        int degrees = CameraUtil.getDisplayRotation(mActivity);
        int cameraId = CameraHolder.instance().getBackCameraId();
        int orientation = CameraUtil.getDisplayOrientation(degrees, cameraId);
        mUI.setProgressOrientation(orientation);
    }

    private void stopCapture(boolean aborted) {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;
        mUI.onStopCapture();

        mMosaicFrameProcessor.setProgressListener(null);
        stopCameraPreview();

        mCameraTexture.setOnFrameAvailableListener(null);

        if (!aborted && !mThreadRunning) {
            mUI.showWaitingDialog(mPreparePreviewString);
            // Hide shutter button, shutter icon, etc when waiting for
            // panorama to stitch
            mUI.hideUI();
            runBackgroundThread(new Thread() {
                @Override
                public void run() {
                    MosaicJpeg jpeg = generateFinalMosaic(false);

                    if (jpeg != null && jpeg.isValid) {
                        Bitmap bitmap = null;
                        bitmap = BitmapFactory.decodeByteArray(jpeg.data, 0, jpeg.data.length);
                        mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                MSG_LOW_RES_FINAL_MOSAIC_READY, bitmap));
                    } else {
                        mMainHandler.sendMessage(mMainHandler.obtainMessage(
                                MSG_END_DIALOG_RESET_TO_PREVIEW));
                    }
                }
            });
        }
        keepScreenOnAwhile();
    }

    @Override
    public void onShutterButtonClick() {
        // If mCameraTexture == null then GL setup is not finished yet.
        // No buttons can be pressed.
        if (mPaused || mThreadRunning || mCameraTexture == null) {
            return;
        }
        // Since this button will stay on the screen when capturing, we need to check the state
        // right now.
        switch (mCaptureState) {
            case CAPTURE_STATE_VIEWFINDER:
                final long storageSpaceBytes = mActivity.getStorageSpaceBytes();
                if(storageSpaceBytes <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                    Log.w(TAG, "Low storage warning: " + storageSpaceBytes);
                    return;
                }
                mSoundPlayer.play(SoundClips.START_VIDEO_RECORDING);
                startCapture();
                break;
            case CAPTURE_STATE_MOSAIC:
                mSoundPlayer.play(SoundClips.STOP_VIDEO_RECORDING);
                stopCapture(false);
                break;
            default:
                Log.w(TAG, "Unknown capture state: " + mCaptureState);
                break;
        }
    }

    public void reportProgress() {
        mUI.resetSavingProgress();
        Thread t = new Thread() {
            @Override
            public void run() {
                while (mThreadRunning) {
                    final int progress = mMosaicFrameProcessor.reportProgress(
                            true, mCancelComputation);

                    try {
                        synchronized (mWaitObject) {
                            mWaitObject.wait(50);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Panorama reportProgress failed", e);
                    }
                    // Update the progress bar
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mUI.updateSavingProgress(progress);
                        }
                    });
                }
            }
        };
        t.start();
    }

    private int getCaptureOrientation() {
        // The panorama image returned from the library is oriented based on the
        // natural orientation of a camera. We need to set an orientation for the image
        // in its EXIF header, so the image can be displayed correctly.
        // The orientation is calculated from compensating the
        // device orientation at capture and the camera orientation respective to
        // the natural orientation of the device.
        int orientation;
        if (mUsingFrontCamera) {
            // mCameraOrientation is negative with respect to the front facing camera.
            // See document of android.hardware.Camera.Parameters.setRotation.
            orientation = (mDeviceOrientationAtCapture - mCameraOrientation + 360) % 360;
        } else {
            orientation = (mDeviceOrientationAtCapture + mCameraOrientation) % 360;
        }
        return orientation;
    }

    /** The orientation of the camera image. The value is the angle that the camera
     *  image needs to be rotated clockwise so it shows correctly on the display
     *  in its natural orientation. It should be 0, 90, 180, or 270.*/
    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    public void saveHighResMosaic() {
        runBackgroundThread(new Thread() {
            @Override
            public void run() {
                mPartialWakeLock.acquire();
                MosaicJpeg jpeg;
                try {
                    jpeg = generateFinalMosaic(true);
                } finally {
                    mPartialWakeLock.release();
                }

                if (jpeg == null) {  // Cancelled by user.
                    mMainHandler.sendEmptyMessage(MSG_END_DIALOG_RESET_TO_PREVIEW);
                } else if (!jpeg.isValid) {  // Error when generating mosaic.
                    mMainHandler.sendEmptyMessage(MSG_GENERATE_FINAL_MOSAIC_ERROR);
                } else {
                    int orientation = getCaptureOrientation();
                    final Uri uri = savePanorama(jpeg.data, jpeg.width, jpeg.height, orientation);
                    if (uri != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mActivity.notifyNewMedia(uri);
                            }
                        });
                    }
                    mMainHandler.sendMessage(
                            mMainHandler.obtainMessage(MSG_END_DIALOG_RESET_TO_PREVIEW));
                }
            }
        });
        reportProgress();
    }

    private void runBackgroundThread(Thread thread) {
        mThreadRunning = true;
        thread.start();
    }

    private void onBackgroundThreadFinished() {
        mThreadRunning = false;
        mUI.dismissAllDialogs();
    }

    private void cancelHighResComputation() {
        mCancelComputation = true;
        synchronized (mWaitObject) {
            mWaitObject.notify();
        }
    }

    // This function will be called upon the first camera frame is available.
    private void reset() {
        mCaptureState = CAPTURE_STATE_VIEWFINDER;

        mOrientationManager.unlockOrientation();
        mUI.reset();
        mActivity.setSwipingEnabled(true);
        // Orientation change will trigger onLayoutChange->configMosaicPreview->
        // resetToPreview. Do not show the capture UI in film strip.
        if (mPreviewFocused) {
            mUI.showPreviewUI();
        }
        mMosaicFrameProcessor.reset();
    }

    private void resetToPreviewIfPossible() {
        reset();
        if (!mMosaicFrameProcessorInitialized
                || mUI.getSurfaceTexture() == null
                || !mMosaicPreviewConfigured) {
            return;
        }
        if (!mPaused) {
            startCameraPreview();
        }
    }

    private void showFinalMosaic(Bitmap bitmap) {
        mUI.showFinalMosaic(bitmap, getCaptureOrientation());
    }

    private Uri savePanorama(byte[] jpegData, int width, int height, int orientation) {
        if (jpegData != null) {
            String filename = PanoUtil.createName(
                    mActivity.getResources().getString(R.string.pano_file_name_format), mTimeTaken);
            String filepath = Storage.generateFilepath(filename);

            UsageStatistics.onEvent(UsageStatistics.COMPONENT_PANORAMA,
                    UsageStatistics.ACTION_CAPTURE_DONE, null, 0,
                    UsageStatistics.hashFileName(filename + ".jpg"));

            Location loc = mLocationManager.getCurrentLocation();
            ExifInterface exif = new ExifInterface();
            try {
                exif.readExif(jpegData);
                exif.addGpsDateTimeStampTag(mTimeTaken);
                exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME, mTimeTaken,
                        TimeZone.getDefault());
                exif.setTag(exif.buildTag(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.getOrientationValueForRotation(orientation)));
                writeLocation(loc, exif);
                exif.writeExif(jpegData, filepath);
            } catch (IOException e) {
                Log.e(TAG, "Cannot set exif for " + filepath, e);
                Storage.writeFile(filepath, jpegData);
            }
            int jpegLength = (int) (new File(filepath).length());
            return Storage.addImage(mContentResolver, filename, mTimeTaken, loc, orientation,
                    jpegLength, filepath, width, height, LocalData.MIME_TYPE_JPEG);
        }
        return null;
    }

    private static void writeLocation(Location location, ExifInterface exif) {
        if (location == null) {
            return;
        }
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_PROCESSING_METHOD, location.getProvider()));
    }

    private void clearMosaicFrameProcessorIfNeeded() {
        if (!mPaused || mThreadRunning) return;
        // Only clear the processor if it is initialized by this activity
        // instance. Other activity instances may be using it.
        if (mMosaicFrameProcessorInitialized) {
            mMosaicFrameProcessor.clear();
            mMosaicFrameProcessorInitialized = false;
        }
    }

    private void initMosaicFrameProcessorIfNeeded() {
        if (mPaused || mThreadRunning) {
            return;
        }

        mMosaicFrameProcessor.initialize(
                mCameraPreviewWidth, mCameraPreviewHeight, getPreviewBufSize());
        mMosaicFrameProcessorInitialized = true;
    }

    @Override
    public void onPauseBeforeSuper() {
        mPaused = true;
        if (mLocationManager != null) mLocationManager.recordLocation(false);
        mOrientationManager.pause();
    }

    @Override
    public void onPauseAfterSuper() {
        mOrientationEventListener.disable();
        if (mCameraDevice == null) {
            // Camera open failed. Nothing should be done here.
            return;
        }
        // Stop the capturing first.
        if (mCaptureState == CAPTURE_STATE_MOSAIC) {
            stopCapture(true);
            reset();
        }
        mUI.showPreviewCover();
        releaseCamera();
        synchronized (mRendererLock) {
            mCameraTexture = null;

            // The preview renderer might not have a chance to be initialized
            // before onPause().
            if (mMosaicPreviewRenderer != null) {
                mMosaicPreviewRenderer.release();
                mMosaicPreviewRenderer = null;
            }
        }

        clearMosaicFrameProcessorIfNeeded();
        if (mWaitProcessorTask != null) {
            mWaitProcessorTask.cancel(true);
            mWaitProcessorTask = null;
        }
        resetScreenOn();
        mUI.removeDisplayChangeListener();
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
            mSoundPlayer = null;
        }
        System.gc();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mUI.onConfigurationChanged(newConfig, mThreadRunning);
    }

    @Override
    public void onOrientationChanged(int orientation) {
    }

    @Override
    public void onResumeBeforeSuper() {
        mPaused = false;
    }

    @Override
    public void onResumeAfterSuper() {
        mOrientationEventListener.enable();

        mCaptureState = CAPTURE_STATE_VIEWFINDER;

        if (!setupCamera()) {
            Log.e(TAG, "Failed to open camera, aborting");
            return;
        }

        // Set up sound playback for shutter button
        mSoundPlayer = SoundClips.getPlayer(mActivity);

        // Check if another panorama instance is using the mosaic frame processor.
        mUI.dismissAllDialogs();
        if (!mThreadRunning && mMosaicFrameProcessor.isMosaicMemoryAllocated()) {
            mUI.showWaitingDialog(mDialogWaitingPreviousString);
            // If stitching is still going on, make sure switcher and shutter button
            // are not showing
            mUI.hideUI();
            mWaitProcessorTask = new WaitProcessorTask().execute();
        } else {
            // Camera must be initialized before MosaicFrameProcessor is
            // initialized. The preview size has to be decided by camera device.
            initMosaicFrameProcessorIfNeeded();
            Point size = mUI.getPreviewAreaSize();
            mPreviewUIWidth = size.x;
            mPreviewUIHeight = size.y;
            configMosaicPreview();
            mActivity.updateStorageSpaceAndHint();
        }
        keepScreenOnAwhile();

        mOrientationManager.resume();
        // Initialize location service.
        boolean recordLocation = RecordLocationPreference.get(mPreferences,
                mContentResolver);
        mLocationManager.recordLocation(recordLocation);
        mUI.initDisplayChangeListener();
        UsageStatistics.onContentViewChanged(
                UsageStatistics.COMPONENT_CAMERA, "PanoramaModule");
    }

    /**
     * Generate the final mosaic image.
     *
     * @param highRes flag to indicate whether we want to get a high-res version.
     * @return a MosaicJpeg with its isValid flag set to true if successful; null if the generation
     *         process is cancelled; and a MosaicJpeg with its isValid flag set to false if there
     *         is an error in generating the final mosaic.
     */
    public MosaicJpeg generateFinalMosaic(boolean highRes) {
        int mosaicReturnCode = mMosaicFrameProcessor.createMosaic(highRes);
        if (mosaicReturnCode == Mosaic.MOSAIC_RET_CANCELLED) {
            return null;
        } else if (mosaicReturnCode == Mosaic.MOSAIC_RET_ERROR) {
            return new MosaicJpeg();
        }

        byte[] imageData = mMosaicFrameProcessor.getFinalMosaicNV21();
        if (imageData == null) {
            Log.e(TAG, "getFinalMosaicNV21() returned null.");
            return new MosaicJpeg();
        }

        int len = imageData.length - 8;
        int width = (imageData[len + 0] << 24) + ((imageData[len + 1] & 0xFF) << 16)
                + ((imageData[len + 2] & 0xFF) << 8) + (imageData[len + 3] & 0xFF);
        int height = (imageData[len + 4] << 24) + ((imageData[len + 5] & 0xFF) << 16)
                + ((imageData[len + 6] & 0xFF) << 8) + (imageData[len + 7] & 0xFF);
        Log.d(TAG, "ImLength = " + (len) + ", W = " + width + ", H = " + height);

        if (width <= 0 || height <= 0) {
            // TODO: pop up an error message indicating that the final result is not generated.
            Log.e(TAG, "width|height <= 0!!, len = " + (len) + ", W = " + width + ", H = " +
                    height);
            return new MosaicJpeg();
        }

        YuvImage yuvimage = new YuvImage(imageData, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        try {
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception in storing final mosaic", e);
            return new MosaicJpeg();
        }
        return new MosaicJpeg(out.toByteArray(), width, height);
    }

    private void startCameraPreview() {
        if (mCameraDevice == null) {
            // Camera open failed. Return.
            return;
        }

        if (mUI.getSurfaceTexture() == null) {
            // UI is not ready.
            return;
        }

        // This works around a driver issue. startPreview may fail if
        // stopPreview/setPreviewTexture/startPreview are called several times
        // in a row. mCameraTexture can be null after pressing home during
        // mosaic generation and coming back. Preview will be started later in
        // onLayoutChange->configMosaicPreview. This also reduces the latency.
        synchronized (mRendererLock) {
            if (mCameraTexture == null) return;

            // If we're previewing already, stop the preview first (this will
            // blank the screen).
            if (mCameraState != PREVIEW_STOPPED) stopCameraPreview();

            // Set the display orientation to 0, so that the underlying mosaic
            // library can always get undistorted mCameraPreviewWidth x mCameraPreviewHeight
            // image data from SurfaceTexture.
            mCameraDevice.setDisplayOrientation(0);

            mCameraTexture.setOnFrameAvailableListener(this);
            mCameraDevice.setPreviewTexture(mCameraTexture);
        }
        mCameraDevice.startPreview();
        mCameraState = PREVIEW_ACTIVE;
    }

    private void stopCameraPreview() {
        if (mCameraDevice != null && mCameraState != PREVIEW_STOPPED) {
            mCameraDevice.stopPreview();
        }
        mCameraState = PREVIEW_STOPPED;
    }

    @Override
    public void onUserInteraction() {
        if (mCaptureState != CAPTURE_STATE_MOSAIC) keepScreenOnAwhile();
    }

    @Override
    public boolean onBackPressed() {
        // If panorama is generating low res or high res mosaic, ignore back
        // key. So the activity will not be destroyed.
        if (mThreadRunning) return true;
        return false;
    }

    private void resetScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void keepScreenOnAwhile() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_DELAY, SCREEN_DELAY);
    }

    private void keepScreenOn() {
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_DELAY);
        mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private class WaitProcessorTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            synchronized (mMosaicFrameProcessor) {
                while (!isCancelled() && mMosaicFrameProcessor.isMosaicMemoryAllocated()) {
                    try {
                        mMosaicFrameProcessor.wait();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            mActivity.updateStorageSpace();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mWaitProcessorTask = null;
            mUI.dismissAllDialogs();
            // TODO (shkong): mGLRootView.setVisibility(View.VISIBLE);
            initMosaicFrameProcessorIfNeeded();
            Point size = mUI.getPreviewAreaSize();
            mPreviewUIWidth = size.x;
            mPreviewUIHeight = size.y;
            configMosaicPreview();
            resetToPreviewIfPossible();
            mActivity.updateStorageHint(mActivity.getStorageSpaceBytes());
        }
    }

    @Override
    public void cancelHighResStitching() {
        if (mPaused || mCameraTexture == null) return;
        cancelHighResComputation();
    }

    @Override
    public void onStop() {
    }

    @Override
    public void installIntentFilter() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
    }

    @Override
    public void onPreviewTextureCopied() {
    }

    @Override
    public void onCaptureTextureCopied() {
    }

    @Override
    public boolean updateStorageHintOnResume() {
        return false;
    }

    @Override
    public void onShowSwitcherPopup() {
    }

    @Override
    public void onMediaSaveServiceConnected(MediaSaveService s) {
        // do nothing.
    }
}
