/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.cts.verifier.camera.orientation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests for manual verification of the CDD-required camera output formats
 * for preview callbacks
 */
public class CameraOrientationActivity extends PassFailButtons.Activity
implements OnClickListener, SurfaceHolder.Callback {

    private static final String TAG = "CameraOrientation";
    private static final int STATE_OFF = 0;
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_CAPTURE = 2;
    private static final int NUM_ORIENTATIONS = 4;
    private static final String STAGE_INDEX_EXTRA = "stageIndex";

    private Button mPassButton;
    private Button mFailButton;
    private Button mTakePictureButton;

    private SurfaceView mCameraView;
    private ImageView mFormatView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private List<Camera.Size> mPreviewSizes;
    private Camera.Size mOptimalSize;
    private List<Integer> mPreviewOrientations;
    private int mNextPreviewOrientation;
    private int mNumCameras;
    private int mCurrentCameraId = -1;
    private int mState = STATE_OFF;
    private boolean mSizeAdjusted;

    private StringBuilder mReportBuilder = new StringBuilder();
    private final TreeSet<String> mTestedCombinations = new TreeSet<String>();
    private final TreeSet<String> mUntestedCombinations = new TreeSet<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.co_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.camera_orientation, R.string.co_info, -1);
        mNumCameras = Camera.getNumberOfCameras();

        mPassButton         = (Button) findViewById(R.id.pass_button);
        mFailButton         = (Button) findViewById(R.id.fail_button);
        mTakePictureButton  = (Button) findViewById(R.id.take_picture_button);
        mFormatView         = (ImageView) findViewById(R.id.format_view);
        mCameraView         = (SurfaceView) findViewById(R.id.camera_view);

        mFormatView.setOnClickListener(this);
        mCameraView.setOnClickListener(this);
        mTakePictureButton.setOnClickListener(this);

        mSurfaceHolder = mCameraView.getHolder();
        mSurfaceHolder.addCallback(this);

        mPreviewOrientations = new ArrayList<Integer>();
        mPreviewOrientations.add(0);
        mPreviewOrientations.add(90);
        mPreviewOrientations.add(180);
        mPreviewOrientations.add(270);

        // This activity is reused multiple times
        // to test each camera/orientation combination
        final int stageIndex = getIntent().getIntExtra(STAGE_INDEX_EXTRA, 0);
        Settings settings = getSettings(stageIndex);

        // Hitting the pass button goes to the next test activity.
        // Only the last one uses the PassFailButtons click callback function,
        // which gracefully terminates the activity.
        if (stageIndex + 1 < mNumCameras * NUM_ORIENTATIONS) {
            setPassButtonGoesToNextStage(stageIndex);
        }

        String[] availableOrientations = new String[NUM_ORIENTATIONS];
        for (int i=0; i<availableOrientations.length; i++) {
            // append degree symbol
            availableOrientations[i] = Integer.toString(i * 90) + "\u00b0";
        }

        resetButtons();

        // Set initial values
        mSizeAdjusted = false;
        mCurrentCameraId = settings.mCameraId;
        TextView cameraLabel = (TextView) findViewById(R.id.camera_text);
        cameraLabel.setText(
                getString(R.string.co_camera_label)
                + " " + (mCurrentCameraId+1) + " of " + mNumCameras);

        mNextPreviewOrientation = settings.mOrientation;
        TextView orientationLabel =
                (TextView) findViewById(R.id.orientation_text);
        orientationLabel.setText(
                getString(R.string.co_orientation_label)
                + " "
                + Integer.toString(mNextPreviewOrientation+1)
                + " of "
                + Integer.toString(NUM_ORIENTATIONS)
                + ": "
                + mPreviewOrientations.get(mNextPreviewOrientation) + "\u00b0"
                + " "
                + getString(R.string.co_orientation_direction_label)
                );

        TextView instructionLabel =
                (TextView) findViewById(R.id.instruction_text);
        instructionLabel.setText(R.string.co_instruction_text_photo_label);

        mTakePictureButton.setEnabled(false);
        setUpCamera(mCurrentCameraId);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpCamera(mCurrentCameraId);
    }

    @Override
    public void onPause() {
        super.onPause();
        shutdownCamera();
    }

    @Override
    public String getTestDetails() {
        return mReportBuilder.toString();
    }

    private void setUpCamera(int id) {
        shutdownCamera();

        Log.v(TAG, "Setting up Camera " + id);
        mCurrentCameraId = id;

        try {
            mCamera = Camera.open(id);
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera");
        }

        Camera.Parameters p = mCamera.getParameters();

        // Get preview resolutions
        List<Camera.Size> unsortedSizes = p.getSupportedPreviewSizes();
        class SizeCompare implements Comparator<Camera.Size> {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width < rhs.width) return -1;
                if (lhs.width > rhs.width) return 1;
                if (lhs.height < rhs.height) return -1;
                if (lhs.height > rhs.height) return 1;
                return 0;
            }
        }
        SizeCompare s = new SizeCompare();
        TreeSet<Camera.Size> sortedResolutions = new TreeSet<Camera.Size>(s);
        sortedResolutions.addAll(unsortedSizes);
        mPreviewSizes = new ArrayList<Camera.Size>(sortedResolutions);

        startPreview();
    }

    private void shutdownCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            mState = STATE_OFF;
        }
    }

    private void startPreview() {
        if (mState != STATE_OFF) {
            // Stop for a while to drain callbacks
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mState = STATE_OFF;
            Handler h = new Handler();
            Runnable mDelayedPreview = new Runnable() {
                @Override
                public void run() {
                    startPreview();
                }
            };
            h.postDelayed(mDelayedPreview, 300);
            return;
        }

        mCamera.setPreviewCallback(mPreviewCallback);

        try {
            mCamera.setPreviewDisplay(mCameraView.getHolder());
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to connect camera to display");
        }

        Camera.Parameters p = mCamera.getParameters();
        Log.v(TAG, "Initializing picture format");
        p.setPictureFormat(ImageFormat.JPEG);
        mOptimalSize = getOptimalPreviewSize(mPreviewSizes, 640, 480);
        Log.v(TAG, "Initializing picture size to "
                + mOptimalSize.width + "x" + mOptimalSize.height);
        p.setPictureSize(mOptimalSize.width, mOptimalSize.height);
        Log.v(TAG, "Initializing preview size to "
                + mOptimalSize.width + "x" + mOptimalSize.height);
        p.setPreviewSize(mOptimalSize.width, mOptimalSize.height);

        Log.v(TAG, "Setting camera parameters");
        mCamera.setParameters(p);
        Log.v(TAG, "Setting color filter");
        mFormatView.setColorFilter(null);
        Log.v(TAG, "Starting preview");
        try {
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Cannot start preview", e);
        }

        // set preview orientation
        int degrees = mPreviewOrientations.get(mNextPreviewOrientation);
        mCamera.setDisplayOrientation(degrees);

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(mCurrentCameraId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            TextView cameraExtraLabel =
                    (TextView) findViewById(R.id.instruction_extra_text);
            cameraExtraLabel.setText(
                    getString(R.string.co_instruction_text_extra_label));
        }

        mState = STATE_PREVIEW;
    }

    @Override
    public void onClick(View view) {
        Log.v(TAG, "Click detected");

        if (view == mFormatView || view == mTakePictureButton) {
            if(mState == STATE_PREVIEW) {
                mTakePictureButton.setEnabled(false);
                Log.v(TAG, "Taking picture");
                mCamera.takePicture(null, null, null, mCameraCallback);
                mState = STATE_CAPTURE;
            }
        }

        if(view == mPassButton || view == mFailButton) {
            final int stageIndex =
                    getIntent().getIntExtra(STAGE_INDEX_EXTRA, 0);
            String[] cameraNames = new String[mNumCameras];
            int counter = 0;
            for (int i = 0; i < mNumCameras; i++) {
                cameraNames[i] = "Camera " + i;

                for(int j = 0; j < mPreviewOrientations.size(); j++) {
                    String combination = cameraNames[i] + ", "
                            + mPreviewOrientations.get(j)
                            + "\u00b0"
                            + "\n";

                    if(counter < stageIndex) {
                        // test already passed, or else wouldn't have made
                        // it to current stageIndex
                        mTestedCombinations.add(combination);
                    }

                    if(counter == stageIndex) {
                        // current test configuration
                        if(view == mPassButton) {
                            mTestedCombinations.add(combination);
                        }
                        else if(view == mFailButton) {
                            mUntestedCombinations.add(combination);
                        }
                    }

                    if(counter > stageIndex) {
                        // test not passed yet, since haven't made it to
                        // stageIndex
                        mUntestedCombinations.add(combination);
                    }

                    counter++;
                }
            }

            mReportBuilder = new StringBuilder();
            mReportBuilder.append("Passed combinations:\n");
            for (String combination : mTestedCombinations) {
                mReportBuilder.append(combination);
            }
            mReportBuilder.append("Failed/untested combinations:\n");
            for (String combination : mUntestedCombinations) {
                mReportBuilder.append(combination);
            }

            if(view == mPassButton) {
                TestResult.setPassedResult(this, "CameraOrientationActivity",
                        getTestDetails());
            }
            if(view == mFailButton) {
                TestResult.setFailedResult(this, "CameraOrientationActivity",
                        getTestDetails());
            }

            // restart activity to test next orientation
            Intent intent = new Intent(CameraOrientationActivity.this,
                    CameraOrientationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            intent.putExtra(STAGE_INDEX_EXTRA, stageIndex + 1);
            startActivity(intent);
        }
    }

    private void resetButtons() {
        enablePassFailButtons(false);
    }

    private void enablePassFailButtons(boolean enable) {
        mPassButton.setEnabled(enable);
        mFailButton.setEnabled(enable);
    }

    // find a supported size with ratio less than tolerance threshold, and
    // which is closest to height and width of given dimensions without
    // being larger than either of given dimensions
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w,
            int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) 640 / (double) 480;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        int minDiff = Integer.MAX_VALUE;
        int curDiff;

        int targetHeight = h;
        int targetWidth = w;

        boolean aspectRatio = true;
        while(true) {
            for (Camera.Size size : sizes) {
                if(aspectRatio) {
                    double ratio = (double) size.width / size.height;
                    if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                        continue;
                    }
                }
                curDiff = Math.abs(size.height - targetHeight) +
                        Math.abs(size.width - targetWidth);
                if (curDiff < minDiff
                        && size.height <= targetHeight
                        && size.width <= targetWidth) {
                    optimalSize = size;
                    minDiff = curDiff;
                }
            }
            if (optimalSize == null) {
                // Cannot find a match, so repeat search and
                // ignore aspect ratio requirement
                aspectRatio = false;
                continue;
            }
            else {
                break;
            }
        }

        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        startPreview();
    }

    private void setTestedConfiguration(int cameraId, int orientation) {
        String combination = "Camera " + cameraId + ", "
                + orientation
                + "\u00b0"
                + "\n";
        if (!mTestedCombinations.contains(combination)) {
            mTestedCombinations.add(combination);
            mUntestedCombinations.remove(combination);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Auto-generated method stub
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Auto-generated method stub
    }

    private final Camera.PreviewCallback mPreviewCallback =
            new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // adjust camera preview to match output image's aspect ratio
            if(!mSizeAdjusted && mState == STATE_PREVIEW) {
                int viewWidth = mFormatView.getWidth();
                int viewHeight = mFormatView.getHeight();
                int newWidth, newHeight;

                if (mPreviewOrientations.get(mNextPreviewOrientation) == 0
                    || mPreviewOrientations.get(mNextPreviewOrientation) == 180) {
                    // make preview width same as output image width,
                    // then calculate height using output image's height/width ratio
                    newWidth = viewWidth;
                    newHeight = (int) (viewWidth * ((double) mOptimalSize.height /
                            (double) mOptimalSize.width));
                }
                else {
                    newHeight = viewHeight;
                    newWidth = (int) (viewHeight * ((double) mOptimalSize.height /
                            (double) mOptimalSize.width));
                }

                LayoutParams layoutParams = new LayoutParams(newWidth, newHeight);
                mCameraView.setLayoutParams(layoutParams);
                mSizeAdjusted = true;
                mTakePictureButton.setEnabled(true);
            }
        }
    };

    private final Camera.PictureCallback mCameraCallback =
            new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera mCamera) {
            if (data != null) {
                Bitmap inputImage;
                inputImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                int degrees = mPreviewOrientations.get(mNextPreviewOrientation);
                android.hardware.Camera.CameraInfo info =
                        new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(mCurrentCameraId, info);
                float mirrorX[];
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    // mirror the image along vertical axis
                    mirrorX = new float[] {-1, 0, 0, 0, 1, 1, 0, 0, 1};
                    degrees = (360 - degrees) % 360; // compensate the mirror
                } else {
                    // leave image the same via identity matrix
                    mirrorX = new float[] {1, 0, 0, 0, 1, 0, 0, 0, 1};
                }

                // use matrix to transform the image
                Matrix matrixMirrorX = new Matrix();
                matrixMirrorX.setValues(mirrorX);
                Matrix mat = new Matrix();
                mat.postRotate(degrees);
                mat.postConcat(matrixMirrorX);

                Bitmap inputImageAdjusted = Bitmap.createBitmap(inputImage,
                        0,
                        0,
                        inputImage.getWidth(),
                        inputImage.getHeight(),
                        mat,
                        true);
                mFormatView.setImageBitmap(inputImageAdjusted);

                Log.v(TAG, "Output image set");
                enablePassFailButtons(true);

                TextView instructionLabel =
                        (TextView) findViewById(R.id.instruction_text);
                instructionLabel.setText(
                        R.string.co_instruction_text_passfail_label);
            }

            startPreview();
        }
    };

    private void setPassButtonGoesToNextStage(final int stageIndex) {
        findViewById(R.id.pass_button).setOnClickListener(this);
    }

    private Settings getSettings(int stageIndex) {
        int curCameraId = stageIndex / NUM_ORIENTATIONS;
        int curOrientation = stageIndex % NUM_ORIENTATIONS;
        return new Settings(stageIndex, curCameraId, curOrientation);
    }

    // Bundle of settings for testing a particular
    // camera/orientation combination
    class Settings {
        int mCameraId;
        int mOrientation;

        Settings(int stageIndex, int cameraId, int orientation) {
            mCameraId = cameraId;
            mOrientation = orientation;
        }
    }
}
