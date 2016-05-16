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

package com.android.cts.verifier.camera.fov;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity for showing the camera preview and taking a picture.
 */
public class PhotoCaptureActivity extends Activity
        implements PictureCallback, SurfaceHolder.Callback {
    private static final String TAG = PhotoCaptureActivity.class.getSimpleName();
    private static final int FOV_REQUEST_CODE = 1006;
    private static final String PICTURE_FILENAME = "photo.jpg";
    private static float mReportedFovDegrees = 0;
    private float mReportedFovPrePictureTaken = -1;

    private SurfaceView mPreview;
    private SurfaceHolder mSurfaceHolder;
    private Spinner mResolutionSpinner;
    private List<SelectableResolution> mSupportedResolutions;
    private ArrayAdapter<SelectableResolution> mAdapter;

    private int mCameraId;
    private Camera mCamera;
    private Size mSurfaceSize;
    private boolean mCameraInitialized = false;
    private boolean mPreviewActive = false;
    private int mResolutionSpinnerIndex = -1;
    private WakeLock mWakeLock;
    private long shutterStartTime;

    private ArrayList<Integer> mPreviewSizeCamerasToProcess = new ArrayList<Integer>();

    private Dialog mActiveDialog;

    /**
     * Selected preview size per camera. If null, preview size should be
     * automatically detected.
     */
    private Size[] mPreviewSizes = null;

    public static File getPictureFile(Context context) {
        return new File(context.getExternalCacheDir(), PICTURE_FILENAME);
    }

    public static float getReportedFovDegrees() {
        return mReportedFovDegrees;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_fov_calibration_photo_capture);

        mPreview = (SurfaceView) findViewById(R.id.camera_fov_camera_preview);
        mSurfaceHolder = mPreview.getHolder();
        mSurfaceHolder.addCallback(this);

        // This is required for older versions of Android hardware.
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        TextView textView = (TextView) findViewById(R.id.camera_fov_tap_to_take_photo);
        textView.setTextColor(Color.WHITE);

        Button setupButton = (Button) findViewById(R.id.camera_fov_settings_button);
        setupButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(
                        PhotoCaptureActivity.this, CalibrationPreferenceActivity.class));
            }
        });

        Button changePreviewSizeButton = (Button) findViewById(
                R.id.camera_fov_change_preview_size_button);
        changePreviewSizeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Stop camera until preview sizes have been obtained.
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                }

                mPreviewSizeCamerasToProcess.clear();
                mPreviewSizes =  new Size[Camera.getNumberOfCameras()];
                for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); ++cameraId) {
                    mPreviewSizeCamerasToProcess.add(cameraId);
                }
                showNextDialogToChoosePreviewSize();
            }
        });

        View previewView = findViewById(R.id.camera_fov_preview_overlay);
        previewView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shutterStartTime = System.currentTimeMillis();

                mCamera.takePicture(new ShutterCallback() {
                    @Override
                    public void onShutter() {
                        long dT = System.currentTimeMillis() - shutterStartTime;
                        Log.d("CTS", "Shutter Lag: " + dT);
                    }
                }, null, PhotoCaptureActivity.this);
            }
        });

        mResolutionSpinner = (Spinner) findViewById(R.id.camera_fov_resolution_selector);
        mResolutionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (mSupportedResolutions != null) {
                    SelectableResolution resolution = mSupportedResolutions.get(position);

                    switchToCamera(resolution.cameraId, false);

                    Camera.Parameters params = mCamera.getParameters();
                    params.setPictureSize(resolution.width, resolution.height);
                    mCamera.setParameters(params);

                    // It should be guaranteed that the FOV is correctly updated after setParameters().
                    mReportedFovPrePictureTaken = mCamera.getParameters().getHorizontalViewAngle();

                    mResolutionSpinnerIndex = position;
                    initializeCamera();
                }
            }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Keep the device from going to sleep.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, TAG);
        mWakeLock.acquire();

        if (mSupportedResolutions == null) {
            mSupportedResolutions = new ArrayList<SelectableResolution>();
            int numCameras = Camera.getNumberOfCameras();
            for (int cameraId = 0; cameraId < numCameras; ++cameraId) {
                Camera camera = Camera.open(cameraId);

                // Get the supported picture sizes and fill the spinner.
                List<Camera.Size> supportedSizes =
                        camera.getParameters().getSupportedPictureSizes();
                for (Camera.Size size : supportedSizes) {
                    mSupportedResolutions.add(
                            new SelectableResolution(cameraId, size.width, size.height));
                }
                camera.release();
            }
        }

        // Find the first untested entry.
        for (mResolutionSpinnerIndex = 0;
                mResolutionSpinnerIndex < mSupportedResolutions.size();
                mResolutionSpinnerIndex++) {
            if (!mSupportedResolutions.get(mResolutionSpinnerIndex).tested) {
                break;
            }
        }

        mAdapter = new ArrayAdapter<SelectableResolution>(
                this, android.R.layout.simple_spinner_dropdown_item,
                mSupportedResolutions);
        mResolutionSpinner.setAdapter(mAdapter);

        mResolutionSpinner.setSelection(mResolutionSpinnerIndex);
        setResult(RESULT_CANCELED);
    }

    @Override
    public void onPause() {
        if (mCamera != null) {
            if (mPreviewActive) {
                mCamera.stopPreview();
            }

            mCamera.release();
            mCamera = null;
        }
        mPreviewActive = false;
        mWakeLock.release();
        super.onPause();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getPictureFile(this);
        Camera.Parameters params = mCamera.getParameters();
        mReportedFovDegrees = params.getHorizontalViewAngle();

        // Show error if FOV does not match the value reported before takePicture().
        if (mReportedFovPrePictureTaken != mReportedFovDegrees) {
            mSupportedResolutions.get(mResolutionSpinnerIndex).tested = true;
            mSupportedResolutions.get(mResolutionSpinnerIndex).passed = false;

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.camera_fov_reported_fov_problem);
            dialogBuilder.setNeutralButton(
                    android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mActiveDialog != null) {
                        mActiveDialog.dismiss();
                        mActiveDialog = null;
                        initializeCamera();
                    }
                }
            });

            String message  = getResources().getString(R.string.camera_fov_reported_fov_problem_message);
            dialogBuilder.setMessage(String.format(message, mReportedFovPrePictureTaken, mReportedFovDegrees));
            mActiveDialog = dialogBuilder.show();
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Log.d(TAG, "File saved to " + pictureFile.getAbsolutePath());

            // Start activity which will use the taken picture to determine the
            // FOV.
            startActivityForResult(new Intent(this, DetermineFovActivity.class),
                    FOV_REQUEST_CODE + mResolutionSpinnerIndex, null);
        } catch (IOException e) {
            Log.e(TAG, "Could not save picture file.", e);
            Toast.makeText(this, "Could not save picture file: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        int testIndex = requestCode - FOV_REQUEST_CODE;
        SelectableResolution res = mSupportedResolutions.get(testIndex);
        res.tested = true;
        float reportedFOV = CtsTestHelper.getReportedFOV(data);
        float measuredFOV = CtsTestHelper.getMeasuredFOV(data);
        res.measuredFOV = measuredFOV;
        if (CtsTestHelper.isResultPassed(reportedFOV, measuredFOV)) {
            res.passed = true;
        }

        boolean allTested = true;
        for (int i = 0; i < mSupportedResolutions.size(); i++) {
            if (!mSupportedResolutions.get(i).tested) {
                allTested = false;
                break;
            }
        }
        if (!allTested) {
            mAdapter.notifyDataSetChanged();
            return;
        }

        boolean allPassed = true;
        for (int i = 0; i < mSupportedResolutions.size(); i++) {
            if (!mSupportedResolutions.get(i).passed) {
                allPassed = false;
                break;
            }
        }
        if (allPassed) {
            TestResult.setPassedResult(this, getClass().getName(),
                    CtsTestHelper.getTestDetails(mSupportedResolutions));
        } else {
            TestResult.setFailedResult(this, getClass().getName(),
                    CtsTestHelper.getTestDetails(mSupportedResolutions));
        }
        finish();
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder, int format, int width, int height) {
        mSurfaceSize = new Size(width, height);
        initializeCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Nothing to do.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Nothing to do.
    }

    private void showNextDialogToChoosePreviewSize() {
        final int cameraId = mPreviewSizeCamerasToProcess.remove(0);

        Camera camera = Camera.open(cameraId);
        final List<Camera.Size> sizes = camera.getParameters()
                .getSupportedPreviewSizes();
        String[] choices = new String[sizes.size()];
        for (int i = 0; i < sizes.size(); ++i) {
            Camera.Size size = sizes.get(i);
            choices[i] = size.width + " x " + size.height;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String dialogTitle = String.format(
                getResources().getString(R.string.camera_fov_choose_preview_size_for_camera),
                cameraId);
        builder.setTitle(
                dialogTitle).
                setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        // User cancelled preview size selection.
                        mPreviewSizes = null;
                        switchToCamera(mCameraId, true);
                    }
                }).
                setSingleChoiceItems(choices, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Camera.Size size = sizes.get(which);
                        mPreviewSizes[cameraId] = new Size(
                                size.width, size.height);
                        dialog.dismiss();

                        if (mPreviewSizeCamerasToProcess.isEmpty()) {
                            // We're done, re-initialize camera.
                            switchToCamera(mCameraId, true);
                        } else {
                            // Process other cameras.
                            showNextDialogToChoosePreviewSize();
                        }
                    }
                }).create().show();
        camera.release();
    }

    private void initializeCamera() {
        if (mCamera == null || mSurfaceHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (Throwable t) {
            Log.e("TAG", "Could not set preview display", t);
            Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Camera.Parameters params = setCameraParams(mCamera);

        // Either use chosen preview size for current camera or automatically
        // choose preview size based on view dimensions.
        Size selectedPreviewSize = (mPreviewSizes != null) ? mPreviewSizes[mCameraId] :
            getBestPreviewSize(mSurfaceSize.width, mSurfaceSize.height, params);
        if (selectedPreviewSize != null) {
            params.setPreviewSize(selectedPreviewSize.width, selectedPreviewSize.height);
            mCamera.setParameters(params);
            mCameraInitialized = true;
        }
        startPreview();
    }

    private void startPreview() {
        if (mCameraInitialized && mCamera != null) {
            mCamera.startPreview();
            mPreviewActive = true;
        }
    }

    private void switchToCamera(int cameraId, boolean initializeCamera) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
        mCameraId = cameraId;
        mCamera = Camera.open(cameraId);

        if (initializeCamera){
          initializeCamera();
        }
    }

    /**
     * Get the best supported focus mode.
     *
     * @param camera - Android camera object.
     * @return the best supported focus mode.
     */
    private static String getFocusMode(Camera camera) {
        List<String> modes = camera.getParameters().getSupportedFocusModes();
        if (modes != null) {
            if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                Log.v(TAG, "Using Focus mode infinity");
                return Camera.Parameters.FOCUS_MODE_INFINITY;
            }
            if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                Log.v(TAG, "Using Focus mode fixed");
                return Camera.Parameters.FOCUS_MODE_FIXED;
            }
        }
        Log.v(TAG, "Using Focus mode auto.");
        return Camera.Parameters.FOCUS_MODE_AUTO;
    }

    /**
     * Set the common camera parameters on the given camera and returns the
     * parameter object for further modification, if needed.
     */
    private static Camera.Parameters setCameraParams(Camera camera) {
        // The picture size is taken and set from the spinner selection
        // callback.
        Camera.Parameters params = camera.getParameters();
        params.setJpegThumbnailSize(0, 0);
        params.setJpegQuality(100);
        params.setFocusMode(getFocusMode(camera));
        params.setZoom(0);
        return params;
    }

    private Size getBestPreviewSize(
            int width, int height, Camera.Parameters parameters) {
        Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = new Size(size.width, size.height);
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = new Size(size.width, size.height);
                    }
                }
            }
        }
        return result;
    }
}
