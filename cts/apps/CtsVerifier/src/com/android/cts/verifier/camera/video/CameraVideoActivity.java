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
package com.android.cts.verifier.camera.video;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;


/**
 * Tests for manual verification of camera video capture
 */
public class CameraVideoActivity extends PassFailButtons.Activity
        implements TextureView.SurfaceTextureListener {

    private static final String TAG = "CtsCameraVideo";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private static final int VIDEO_LENGTH = 3000; // in ms

    private TextureView mPreviewView;
    private SurfaceTexture mPreviewTexture;
    private int mPreviewTexWidth;
    private int mPreviewTexHeight;
    private int mPreviewRotation;

    private VideoView mPlaybackView;

    private Spinner mCameraSpinner;
    private Spinner mResolutionSpinner;

    private int mCurrentCameraId = -1;
    private Camera mCamera;

    private MediaRecorder mMediaRecorder;

    private List<Size> mPreviewSizes;
    private Size mNextPreviewSize;
    private Size mPreviewSize;
    private List<Integer> mVideoSizeIds;
    private int mCurrentVideoSizeId;

    private boolean isRecording = false;
    private boolean isPlayingBack = false;
    private Button captureButton;
    private Button mPassButton;
    private Button mFailButton;

    private TextView mStatusLabel;

    private TreeSet<String> mTestedCombinations = new TreeSet<String>();
    private TreeSet<String> mUntestedCombinations = new TreeSet<String>();

    private File outputVideoFile;

    /**
     * @see #MEDIA_TYPE_IMAGE
     * @see #MEDIA_TYPE_VIDEO
     */
    private static File getOutputMediaFile(int type) {
        // Question: why do I need to comment this to get it working?
        // Logcat says "external storage not ready"
        // if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
        //     Log.e(TAG, "external storage not ready");
        //     return null;
        // }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), TAG);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
            if (VERBOSE) {
                Log.v(TAG, "getOutputMediaFile: output file " + mediaFile.getPath());
            }
        } else {
            return null;
        }

        return mediaFile;
    }

    private boolean prepareVideoRecorder() {

        mMediaRecorder = new MediaRecorder();

        // Step 1: unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: set a CamcorderProfile
        mMediaRecorder.setProfile(CamcorderProfile.get(mCurrentCameraId, mCurrentVideoSizeId));

        // Step 4: set output file
        outputVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        mMediaRecorder.setOutputFile(outputVideoFile.toString());

        // Step 5: set preview output
        // This is not necessary since preview has been taken care of

        // Step 6: prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "IOException preparing MediaRecorder: ", e);
            releaseMediaRecorder();
            throw new AssertionError(e);
        }

        mMediaRecorder.setOnErrorListener(
                new MediaRecorder.OnErrorListener() {
                    @Override
                    public void onError(MediaRecorder mr, int what, int extra) {
                        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                            Log.e(TAG, "unknown error in media recorder, error: " + extra);
                        } else {
                            Log.e(TAG, "media recorder server died, error: " + extra);
                        }

                        failTest("Media recorder error.");
                    }
                });

        if (VERBOSE) {
            Log.v(TAG, "prepareVideoRecorder: prepared configured MediaRecorder");
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera_video);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.camera_video, R.string.video_info, /*viewId*/-1);

        mPreviewView = (TextureView) findViewById(R.id.video_capture);
        mPlaybackView = (VideoView) findViewById(R.id.video_playback);
        mPlaybackView.setOnCompletionListener(mPlaybackViewListener);

        captureButton = (Button) findViewById(R.id.record_button);
        mPassButton = (Button) findViewById(R.id.pass_button);
        mFailButton = (Button) findViewById(R.id.fail_button);
        mPassButton.setEnabled(false);
        mFailButton.setEnabled(true);

        mPreviewView.setSurfaceTextureListener(this);

        int numCameras = Camera.getNumberOfCameras();
        String[] cameraNames = new String[numCameras];
        for (int i = 0; i < numCameras; i++) {
            cameraNames[i] = "Camera " + i;
            mUntestedCombinations.add("All combinations for Camera " + i + "\n");
        }
        if (VERBOSE) {
            Log.v(TAG, "onCreate: number of cameras=" + numCameras);
        }
        mCameraSpinner = (Spinner) findViewById(R.id.cameras_selection);
        mCameraSpinner.setAdapter(
            new ArrayAdapter<String>(
                this, R.layout.cf_format_list_item, cameraNames));
        mCameraSpinner.setOnItemSelectedListener(mCameraSpinnerListener);

        mResolutionSpinner = (Spinner) findViewById(R.id.resolution_selection);
        mResolutionSpinner.setOnItemSelectedListener(mResolutionSelectedListener);

        mStatusLabel = (TextView) findViewById(R.id.status_label);
    }

    @Override
    public void onResume() {
        super.onResume();

        setUpCamera(mCameraSpinner.getSelectedItemPosition());
        if (VERBOSE) {
            Log.v(TAG, "onResume: camera has been setup");
        }

        setUpCaptureButton();
        if (VERBOSE) {
            Log.v(TAG, "onResume: captureButton has been setup");
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        releaseMediaRecorder();
        shutdownCamera();
        mPreviewTexture = null;
    }

    private MediaPlayer.OnCompletionListener mPlaybackViewListener =
            new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    isPlayingBack = false;
                    captureButton.setEnabled(true);
                    mStatusLabel.setText(getResources().getString(R.string.status_ready));
                }

    };

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock(); // check here, lock camera for later use
        }
    }

    @Override
    public String getTestDetails() {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Tested combinations:\n");
        for (String combination : mTestedCombinations) {
            reportBuilder.append(combination);
        }
        reportBuilder.append("Untested combinations:\n");
        for (String combination : mUntestedCombinations) {
            reportBuilder.append(combination);
        }
        return reportBuilder.toString();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface,
            int width, int height) {
        mPreviewTexture = surface;
        mPreviewTexWidth = width;
        mPreviewTexHeight = height;
        if (mCamera != null) {
            startPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }


    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    private AdapterView.OnItemSelectedListener mCameraSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                        View view, int pos, long id) {
                    if (mCurrentCameraId != pos) {
                        setUpCamera(pos);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Intentionally left blank
                }

            };

    private AdapterView.OnItemSelectedListener mResolutionSelectedListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent,
                        View view, int position, long id) {
                    if (mVideoSizeIds.get(position) != mCurrentVideoSizeId) {
                        mCurrentVideoSizeId = mVideoSizeIds.get(position);
                        if (VERBOSE) {
                            Log.v(TAG, "onItemSelected: mCurrentVideoSizeId = " +
                                    mCurrentVideoSizeId);
                        }
                        mNextPreviewSize = matchPreviewRecordSize();
                        if (VERBOSE) {
                            Log.v(TAG, "onItemSelected: setting preview size "
                                    + mNextPreviewSize.width + "x" + mNextPreviewSize.height);
                        }

                        startPreview();
                        if (VERBOSE) {
                            Log.v(TAG, "onItemSelected: started new preview");
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Intentionally left blank
                }

            };


    private void setUpCaptureButton() {
        captureButton.setOnClickListener (
                new View.OnClickListener() {
                    @Override
                    public void onClick(View V) {
                        if ((!isRecording) && (!isPlayingBack)) {
                            if (prepareVideoRecorder()) {
                                mMediaRecorder.start();
                                if (VERBOSE) {
                                    Log.v(TAG, "onClick: started mMediaRecorder");
                                }
                                isRecording = true;
                                captureButton.setEnabled(false);
                                mStatusLabel.setText(getResources()
                                        .getString(R.string.status_recording));
                            } else {
                                releaseMediaRecorder();
                                Log.e(TAG, "media recorder cannot be set up");
                                failTest("Unable to set up media recorder.");
                            }
                            Handler h = new Handler();
                            Runnable mDelayedPreview = new Runnable() {
                                @Override
                                public void run() {
                                    mMediaRecorder.stop();
                                    releaseMediaRecorder();

                                    mPlaybackView.setVideoPath(outputVideoFile.getPath());
                                    mPlaybackView.start();
                                    isRecording = false;
                                    isPlayingBack = true;
                                    mStatusLabel.setText(getResources()
                                            .getString(R.string.status_playback));
                                    String combination = "Camera " + mCurrentCameraId + ", " +
                                            mCurrentVideoSizeId + "\n";
                                    mUntestedCombinations.remove(combination);
                                    mTestedCombinations.add(combination);

                                    if (mUntestedCombinations.isEmpty()) {
                                        mPassButton.setEnabled(true);
                                        if (VERBOSE) {
                                            Log.v(TAG, "run: test success");
                                        }
                                    }
                                }
                            };
                            h.postDelayed(mDelayedPreview, VIDEO_LENGTH);
                        }

                    }
                }
        );
    }

    private class VideoSizeNamePair {
        private int sizeId;
        private String sizeName;

        public VideoSizeNamePair(int id, String name) {
            sizeId = id;
            sizeName = name;
        }

        public int getSizeId() {
            return sizeId;
        }

        public String getSizeName() {
            return sizeName;
        }
    }

    private ArrayList<VideoSizeNamePair> getVideoSizeNamePairs(int cameraId) {
        int[] qualityArray = {
                CamcorderProfile.QUALITY_LOW,
                CamcorderProfile.QUALITY_HIGH,
                CamcorderProfile.QUALITY_QCIF,
                CamcorderProfile.QUALITY_CIF,
                CamcorderProfile.QUALITY_480P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_1080P
        };

        String[] nameArray = {
                "LOW",
                "HIGH",
                "QCIF",
                "CIF",
                "480P",
                "720P",
                "1080P"
        };

        ArrayList<VideoSizeNamePair> availableSizes =
                new ArrayList<VideoSizeNamePair> ();

        for (int i = 0; i < qualityArray.length; i++) {
            if (CamcorderProfile.hasProfile(cameraId, qualityArray[i])) {
                VideoSizeNamePair pair = new VideoSizeNamePair(qualityArray[i], nameArray[i]);
                availableSizes.add(pair);
            }
        }
        return availableSizes;
    }

    static class ResolutionQuality {
        private int videoSizeId;
        private int width;
        private int height;

        public ResolutionQuality() {
            // intentionally left blank
        }
        public ResolutionQuality(int newSizeId, int newWidth, int newHeight) {
            videoSizeId = newSizeId;
            width = newWidth;
            height = newHeight;
        }
    }

    private Size findRecordSize() {
        int[] possibleQuality = {
                CamcorderProfile.QUALITY_QCIF,
                CamcorderProfile.QUALITY_CIF,
                CamcorderProfile.QUALITY_480P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_1080P
        };

        Size[] sizes = new Size[] {
                mCamera.new Size(176, 144),
                mCamera.new Size(352, 288),
                mCamera.new Size(720, 480),
                mCamera.new Size(1280, 720),
                mCamera.new Size(1920, 1080)
        };

        Size minSize = mCamera.new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Size maxSize = mCamera.new Size(0, 0);

        for (int i = 0; i < possibleQuality.length; i++) {
            if (mVideoSizeIds.contains(possibleQuality[i])) {
                if (sizes[i].height < minSize.height) {
                    minSize = sizes[i];
                }
                if (sizes[i].height > maxSize.height) {
                    maxSize = sizes[i];
                }
            }
        }

        ArrayList<ResolutionQuality> qualityList = new ArrayList<ResolutionQuality>();
        qualityList.add(new ResolutionQuality(CamcorderProfile.QUALITY_LOW, minSize.width,
                minSize.height));
        qualityList.add(new ResolutionQuality(CamcorderProfile.QUALITY_HIGH, maxSize.width,
                maxSize.height));
        for (int i = 0; i < possibleQuality.length; i++) {
            qualityList.add(new ResolutionQuality(possibleQuality[i], sizes[i].width,
                    sizes[i].height));
        }

        Size recordSize = null;
        for (int i = 0; i < qualityList.size(); i++) {
            if (mCurrentVideoSizeId == qualityList.get(i).videoSizeId) {
                recordSize = mCamera.new Size(qualityList.get(i).width,
                        qualityList.get(i).height);
                break;
            }
        }

        if (recordSize == null) {
            Log.e(TAG, "findRecordSize: did not find a match");
            failTest("Cannot find video size");
        }
        return recordSize;
    }

    // Match preview size with current recording size mCurrentVideoSizeId
    private Size matchPreviewRecordSize() {
        Size recordSize = findRecordSize();

        Size matchedSize = null;
        // First try to find exact match in size
        for (int i = 0; i < mPreviewSizes.size(); i++) {
            if (mPreviewSizes.get(i).equals(recordSize)) {
                matchedSize = mCamera.new Size(recordSize.width, recordSize.height);
                break;
            }
        }
        // Second try to find one with similar if not the same aspect ratio
        if (matchedSize == null) {
            for (int i = mPreviewSizes.size() - 1; i >= 0; i--) {
                if (Math.abs((float)mPreviewSizes.get(i).width * recordSize.height /
                        mPreviewSizes.get(i).height / recordSize.width - 1) < 0.1) {
                    matchedSize = mCamera.new Size(mPreviewSizes.get(i).width,
                            mPreviewSizes.get(i).height);
                    break;
                }
            }
        }
        // Last resort, just use the first preview size
        if (matchedSize == null) {
            matchedSize = mCamera.new Size(mPreviewSizes.get(0).width,
                    mPreviewSizes.get(0).height);
        }

        if (VERBOSE) {
            Log.v(TAG, "matchPreviewRecordSize " + matchedSize.width + "x" + matchedSize.height);
        }

        return matchedSize;
    }

    private void setUpCamera(int id) {
        shutdownCamera();

        mCurrentCameraId = id;
        try {
            mCamera = Camera.open(id);
        }
        catch (Exception e) {
            Log.e(TAG, "camera is not available", e);
            failTest("camera not available" + e.getMessage());
            return;
        }

        Camera.Parameters p = mCamera.getParameters();
        if (VERBOSE) {
            Log.v(TAG, "setUpCamera: setUpCamera got camera parameters");
        }

        // Get preview resolutions
        List<Size> unsortedSizes = p.getSupportedPreviewSizes();

        class SizeCompare implements Comparator<Size> {
            @Override
            public int compare(Size lhs, Size rhs) {
                if (lhs.width < rhs.width) return -1;
                if (lhs.width > rhs.width) return 1;
                if (lhs.height < rhs.height) return -1;
                if (lhs.height > rhs.height) return 1;
                return 0;
            }
        };

        SizeCompare s = new SizeCompare();
        TreeSet<Size> sortedResolutions = new TreeSet<Size>(s);
        sortedResolutions.addAll(unsortedSizes);

        mPreviewSizes = new ArrayList<Size>(sortedResolutions);

        ArrayList<VideoSizeNamePair> availableVideoSizes = getVideoSizeNamePairs(id);
        String[] availableVideoSizeNames = new String[availableVideoSizes.size()];
        mVideoSizeIds = new ArrayList<Integer>();
        for (int i = 0; i < availableVideoSizes.size(); i++) {
            availableVideoSizeNames[i] = availableVideoSizes.get(i).getSizeName();
            mVideoSizeIds.add(availableVideoSizes.get(i).getSizeId());
        }

        mResolutionSpinner.setAdapter(
            new ArrayAdapter<String>(
                this, R.layout.cf_format_list_item, availableVideoSizeNames));

        // Update untested
        mUntestedCombinations.remove("All combinations for Camera " + id + "\n");
        for (int videoSizeId: mVideoSizeIds) {
            String combination = "Camera " + id + ", " + videoSizeId + "\n";
            if (!mTestedCombinations.contains(combination)) {
                mUntestedCombinations.add(combination);
            }
        }

        // Set initial values
        mCurrentVideoSizeId = mVideoSizeIds.get(0);
        mNextPreviewSize = matchPreviewRecordSize();
        mResolutionSpinner.setSelection(0);

        // Set up correct display orientation
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(id, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mPreviewRotation = (info.orientation + degrees) % 360;
            mPreviewRotation = (360 - mPreviewRotation) % 360;  // compensate the mirror
        } else {  // back-facing
            mPreviewRotation = (info.orientation - degrees + 360) % 360;
        }
        if (mPreviewRotation != 0 && mPreviewRotation != 180) {
            Log.w(TAG,
                "Display orientation correction is not 0 or 180, as expected!");
        }

        mCamera.setDisplayOrientation(mPreviewRotation);

        // Start up preview if display is ready
        if (mPreviewTexture != null) {
            startPreview();
        }
    }

    private void shutdownCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * starts capturing and drawing frames on screen
     */
    private void startPreview() {

        mCamera.stopPreview();

        Matrix transform = new Matrix();
        float widthRatio = mNextPreviewSize.width / (float)mPreviewTexWidth;
        float heightRatio = mNextPreviewSize.height / (float)mPreviewTexHeight;
        if (VERBOSE) {
            Log.v(TAG, "startPreview: widthRatio=" + widthRatio + " " + "heightRatio=" +
                    heightRatio);
        }

        if (heightRatio < widthRatio) {
            transform.setScale(1, heightRatio / widthRatio);
            transform.postTranslate(0,
                    mPreviewTexHeight * (1 - heightRatio / widthRatio) / 2);
            if (VERBOSE) {
                Log.v(TAG, "startPreview: shrink vertical by " + heightRatio / widthRatio);
            }
        } else {
            transform.setScale(widthRatio / heightRatio, 1);
            transform.postTranslate(mPreviewTexWidth * (1 - widthRatio / heightRatio) / 2, 0);
            if (VERBOSE) {
                Log.v(TAG, "startPreview: shrink horizontal by " + widthRatio / heightRatio);
            }
        }

        mPreviewView.setTransform(transform);

        mPreviewSize = mNextPreviewSize;

        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        mCamera.setParameters(p);

        try {
            mCamera.setPreviewTexture(mPreviewTexture);
            if (mPreviewTexture == null) {
                Log.e(TAG, "preview texture is null.");
            }
            if (VERBOSE) {
                Log.v(TAG, "startPreview: set preview texture in startPreview");
            }
            mCamera.startPreview();
            if (VERBOSE) {
                Log.v(TAG, "startPreview: started preview in startPreview");
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to start up preview", ioe);
            // Show a dialog box to tell user test failed
            failTest("Unable to start preview.");
        }
    }

    private void failTest(String failMessage) {
        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                PassFailButtons.setTestResultAndFinish(CameraVideoActivity.this,
                                        CameraVideoActivity.this.getTestId(),
                                        CameraVideoActivity.this.getTestDetails(),
                                        /* passed */false);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

        AlertDialog.Builder builder = new AlertDialog.Builder(CameraVideoActivity.this);
        builder.setMessage(getString(R.string.dialog_fail_test) + ". " + failMessage)
                .setPositiveButton(R.string.fail_quit, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .show();
    }

}
