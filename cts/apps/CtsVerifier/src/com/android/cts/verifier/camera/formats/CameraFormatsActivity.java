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
package com.android.cts.verifier.camera.formats;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.Surface;
import android.view.TextureView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.Math;
import java.lang.Thread;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * Tests for manual verification of the CDD-required camera output formats
 * for preview callbacks
 */
public class CameraFormatsActivity extends PassFailButtons.Activity
        implements TextureView.SurfaceTextureListener, Camera.PreviewCallback {

    private static final String TAG = "CameraFormats";

    private TextureView mPreviewView;
    private SurfaceTexture mPreviewTexture;
    private int mPreviewTexWidth;
    private int mPreviewTexHeight;
    private int mPreviewRotation;

    private ImageView mFormatView;

    private Spinner mCameraSpinner;
    private Spinner mFormatSpinner;
    private Spinner mResolutionSpinner;

    private int mCurrentCameraId = -1;
    private Camera mCamera;

    private List<Camera.Size> mPreviewSizes;
    private Camera.Size mNextPreviewSize;
    private Camera.Size mPreviewSize;
    private List<Integer> mPreviewFormats;
    private int mNextPreviewFormat;
    private int mPreviewFormat;
    private SparseArray<String> mPreviewFormatNames;

    private ColorMatrixColorFilter mYuv2RgbFilter;

    private Bitmap mCallbackBitmap;
    private int[] mRgbData;
    private int mRgbWidth;
    private int mRgbHeight;

    private static final int STATE_OFF = 0;
    private static final int STATE_PREVIEW = 1;
    private static final int STATE_NO_CALLBACKS = 2;
    private int mState = STATE_OFF;
    private boolean mProcessInProgress = false;
    private boolean mProcessingFirstFrame = false;

    private TreeSet<String> mTestedCombinations = new TreeSet<String>();
    private TreeSet<String> mUntestedCombinations = new TreeSet<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cf_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.camera_format, R.string.cf_info, -1);

        mPreviewView = (TextureView) findViewById(R.id.preview_view);
        mFormatView = (ImageView) findViewById(R.id.format_view);

        mPreviewView.setSurfaceTextureListener(this);

        int numCameras = Camera.getNumberOfCameras();
        String[] cameraNames = new String[numCameras];
        for (int i = 0; i < numCameras; i++) {
            cameraNames[i] = "Camera " + i;
            mUntestedCombinations.add("All combinations for Camera " + i + "\n");
        }
        mCameraSpinner = (Spinner) findViewById(R.id.cameras_selection);
        mCameraSpinner.setAdapter(
            new ArrayAdapter<String>(
                this, R.layout.cf_format_list_item, cameraNames));
        mCameraSpinner.setOnItemSelectedListener(mCameraSpinnerListener);

        mFormatSpinner = (Spinner) findViewById(R.id.format_selection);
        mFormatSpinner.setOnItemSelectedListener(mFormatSelectedListener);

        mResolutionSpinner = (Spinner) findViewById(R.id.resolution_selection);
        mResolutionSpinner.setOnItemSelectedListener(mResolutionSelectedListener);

        // Must be kept in sync with android.graphics.ImageFormat manually
        mPreviewFormatNames = new SparseArray(7);
        mPreviewFormatNames.append(ImageFormat.JPEG, "JPEG");
        mPreviewFormatNames.append(ImageFormat.NV16, "NV16");
        mPreviewFormatNames.append(ImageFormat.NV21, "NV21");
        mPreviewFormatNames.append(ImageFormat.RGB_565, "RGB_565");
        mPreviewFormatNames.append(ImageFormat.UNKNOWN, "UNKNOWN");
        mPreviewFormatNames.append(ImageFormat.YUY2, "YUY2");
        mPreviewFormatNames.append(ImageFormat.YV12, "YV12");

        // Need YUV->RGB conversion in many cases

        ColorMatrix y2r = new ColorMatrix();
        y2r.setYUV2RGB();
        float[] yuvOffset = new float[] {
            1.f, 0.f, 0.f, 0.f, 0.f,
            0.f, 1.f, 0.f, 0.f, -128.f,
            0.f, 0.f, 1.f, 0.f, -128.f,
            0.f, 0.f, 0.f, 1.f, 0.f
        };

        ColorMatrix yOffset = new ColorMatrix(yuvOffset);

        ColorMatrix yTotal = new ColorMatrix();
        yTotal.setConcat(y2r, yOffset);

        mYuv2RgbFilter = new ColorMatrixColorFilter(yTotal);
    }

    @Override
    public void onResume() {
        super.onResume();

        setUpCamera(mCameraSpinner.getSelectedItemPosition());
    }

    @Override
    public void onPause() {
        super.onPause();

        shutdownCamera();
        mPreviewTexture = null;
    }

    @Override
    public String getTestDetails() {
        StringBuilder reportBuilder = new StringBuilder();
        reportBuilder.append("Tested combinations:\n");
        for (String combination: mTestedCombinations) {
            reportBuilder.append(combination);
        }
        reportBuilder.append("Untested combinations:\n");
        for (String combination: mUntestedCombinations) {
            reportBuilder.append(combination);
        }
        return reportBuilder.toString();
    }


    public void onSurfaceTextureAvailable(SurfaceTexture surface,
            int width, int height) {
        mPreviewTexture = surface;
        mPreviewTexWidth = width;
        mPreviewTexHeight = height;
        if (mCamera != null) {
            startPreview();
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    private AdapterView.OnItemSelectedListener mCameraSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,
                        View view, int pos, long id) {
                    if (mCurrentCameraId != pos) {
                        setUpCamera(pos);
                    }
                }

                public void onNothingSelected(AdapterView parent) {

                }

            };

    private AdapterView.OnItemSelectedListener mResolutionSelectedListener =
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,
                        View view, int position, long id) {
                    if (mPreviewSizes.get(position) != mPreviewSize) {
                        mNextPreviewSize = mPreviewSizes.get(position);
                        startPreview();
                    }
                }

                public void onNothingSelected(AdapterView parent) {

                }

            };


    private AdapterView.OnItemSelectedListener mFormatSelectedListener =
            new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent,
                        View view, int position, long id) {
                    if (mPreviewFormats.get(position) != mNextPreviewFormat) {
                        mNextPreviewFormat = mPreviewFormats.get(position);
                        startPreview();
                    }
                }

                public void onNothingSelected(AdapterView parent) {

                }

            };



    private void setUpCamera(int id) {
        shutdownCamera();

        mCurrentCameraId = id;
        mCamera = Camera.open(id);
        Camera.Parameters p = mCamera.getParameters();

        // Get preview resolutions

        List<Camera.Size> unsortedSizes = p.getSupportedPreviewSizes();

        class SizeCompare implements Comparator<Camera.Size> {
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                if (lhs.width < rhs.width) return -1;
                if (lhs.width > rhs.width) return 1;
                if (lhs.height < rhs.height) return -1;
                if (lhs.height > rhs.height) return 1;
                return 0;
            }
        };

        SizeCompare s = new SizeCompare();
        TreeSet<Camera.Size> sortedResolutions = new TreeSet<Camera.Size>(s);
        sortedResolutions.addAll(unsortedSizes);

        mPreviewSizes = new ArrayList<Camera.Size>(sortedResolutions);

        String[] availableSizeNames = new String[mPreviewSizes.size()];
        for (int i = 0; i < mPreviewSizes.size(); i++) {
            availableSizeNames[i] =
                    Integer.toString(mPreviewSizes.get(i).width) + " x " +
                    Integer.toString(mPreviewSizes.get(i).height);
        }
        mResolutionSpinner.setAdapter(
            new ArrayAdapter<String>(
                this, R.layout.cf_format_list_item, availableSizeNames));

        // Get preview formats

        mPreviewFormats =  p.getSupportedPreviewFormats();

        String[] availableFormatNames = new String[mPreviewFormats.size()];
        for (int i = 0; i < mPreviewFormats.size(); i++) {
            availableFormatNames[i] =
                    mPreviewFormatNames.get(mPreviewFormats.get(i));
        }
        mFormatSpinner.setAdapter(
            new ArrayAdapter<String>(
                this, R.layout.cf_format_list_item, availableFormatNames));

        // Update untested entries

        mUntestedCombinations.remove("All combinations for Camera " + id + "\n");
        for (Camera.Size previewSize: mPreviewSizes) {
            for (int previewFormat: mPreviewFormats) {
                String combination = "Camera " + id + ", "
                        + previewSize.width + "x" + previewSize.height
                        + ", " + mPreviewFormatNames.get(previewFormat)
                        + "\n";
                if (!mTestedCombinations.contains(combination)) {
                    mUntestedCombinations.add(combination);
                }
            }
        }

        // Set initial values

        mNextPreviewSize = mPreviewSizes.get(0);
        mResolutionSpinner.setSelection(0);

        mNextPreviewFormat = mPreviewFormats.get(0);
        mFormatSpinner.setSelection(0);


        // Set up correct display orientation

        CameraInfo info =
            new CameraInfo();
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
                public void run() {
                    startPreview();
                }
            };
            h.postDelayed(mDelayedPreview, 300);
            return;
        }
        mState = STATE_PREVIEW;

        Matrix transform = new Matrix();
        float widthRatio = mNextPreviewSize.width / (float)mPreviewTexWidth;
        float heightRatio = mNextPreviewSize.height / (float)mPreviewTexHeight;

        transform.setScale(1, heightRatio/widthRatio);
        transform.postTranslate(0,
                mPreviewTexHeight * (1 - heightRatio/widthRatio)/2);

        mPreviewView.setTransform(transform);

        mPreviewFormat = mNextPreviewFormat;
        mPreviewSize   = mNextPreviewSize;

        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewFormat(mPreviewFormat);
        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        mCamera.setParameters(p);

        mCamera.setPreviewCallback(this);
        switch (mPreviewFormat) {
            case ImageFormat.NV16:
            case ImageFormat.NV21:
            case ImageFormat.YUY2:
            case ImageFormat.YV12:
                mFormatView.setColorFilter(mYuv2RgbFilter);
                break;
            default:
                mFormatView.setColorFilter(null);
                break;
        }

        // Filter out currently untestable formats
        switch (mPreviewFormat) {
            case ImageFormat.NV16:
            case ImageFormat.RGB_565:
            case ImageFormat.UNKNOWN:
            case ImageFormat.JPEG:
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(CameraFormatsActivity.this);
                builder.setMessage("Unsupported format " +
                        mPreviewFormatNames.get(mPreviewFormat) +
                        "; consider this combination as pass. ")
                        .setTitle("Missing test" )
                        .setNeutralButton("Back", null);
                builder.show();
                mState = STATE_NO_CALLBACKS;
                mCamera.setPreviewCallback(null);
                break;
            default:
                // supported
                break;
        }

        mProcessingFirstFrame = true;
        try {
            mCamera.setPreviewTexture(mPreviewTexture);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
            Log.e(TAG, "Unable to start up preview");
        }
    }

    private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Boolean> {
        protected Boolean doInBackground(byte[]... datas) {
            byte[] data = datas[0];
            try {
                if (mRgbData == null ||
                        mPreviewSize.width != mRgbWidth ||
                        mPreviewSize.height != mRgbHeight) {

                    mRgbData = new int[mPreviewSize.width * mPreviewSize.height * 4];
                    mRgbWidth = mPreviewSize.width;
                    mRgbHeight = mPreviewSize.height;
                }
                switch(mPreviewFormat) {
                    case ImageFormat.NV21:
                        convertFromNV21(data, mRgbData);
                        break;
                    case ImageFormat.YV12:
                        convertFromYV12(data, mRgbData);
                        break;
                    case ImageFormat.YUY2:
                        convertFromYUY2(data, mRgbData);
                        break;
                    case ImageFormat.NV16:
                    case ImageFormat.RGB_565:
                    case ImageFormat.UNKNOWN:
                    case ImageFormat.JPEG:
                    default:
                        convertFromUnknown(data, mRgbData);
                        break;
                }

                if (mCallbackBitmap == null ||
                        mRgbWidth != mCallbackBitmap.getWidth() ||
                        mRgbHeight != mCallbackBitmap.getHeight() ) {
                    mCallbackBitmap =
                            Bitmap.createBitmap(
                                mRgbWidth, mRgbHeight,
                                Bitmap.Config.ARGB_8888);
                }
                mCallbackBitmap.setPixels(mRgbData, 0, mRgbWidth,
                        0, 0, mRgbWidth, mRgbHeight);
            } catch (OutOfMemoryError o) {
                Log.e(TAG, "Out of memory trying to process preview data");
                return false;
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                mFormatView.setImageBitmap(mCallbackBitmap);
                if (mProcessingFirstFrame) {
                    mProcessingFirstFrame = false;
                    String combination = "Camera " + mCurrentCameraId + ", "
                            + mPreviewSize.width + "x" + mPreviewSize.height
                            + ", " + mPreviewFormatNames.get(mPreviewFormat)
                            + "\n";
                    mUntestedCombinations.remove(combination);
                    mTestedCombinations.add(combination);
                }
            }
            mProcessInProgress = false;
        }

    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mProcessInProgress || mState != STATE_PREVIEW) return;

        int expectedBytes;
        switch (mPreviewFormat) {
            case ImageFormat.YV12:
                // YV12 may have stride != width.
                int w = mPreviewSize.width;
                int h = mPreviewSize.height;
                int yStride = (int)Math.ceil(w / 16.0) * 16;
                int uvStride = (int)Math.ceil(yStride / 2 / 16.0) * 16;
                int ySize = yStride * h;
                int uvSize = uvStride * h / 2;
                expectedBytes = ySize + uvSize * 2;
                break;
            case ImageFormat.NV21:
            case ImageFormat.YUY2:
            default:
                expectedBytes = mPreviewSize.width * mPreviewSize.height *
                        ImageFormat.getBitsPerPixel(mPreviewFormat) / 8;
                break;
        }
        if (expectedBytes != data.length) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(CameraFormatsActivity.this);
            builder.setMessage("Mismatched size of buffer! Expected " +
                    expectedBytes + ", but got " +
                    data.length + " bytes instead!")
                    .setTitle("Error trying to use format "
                            + mPreviewFormatNames.get(mPreviewFormat))
                    .setNeutralButton("Back", null);

            builder.show();

            mState = STATE_NO_CALLBACKS;
            mCamera.setPreviewCallback(null);
            return;
        }

        mProcessInProgress = true;
        new ProcessPreviewDataTask().execute(data);
    }

    private void convertFromUnknown(byte[] data, int[] rgbData) {
        int w = mPreviewSize.width;
        int h = mPreviewSize.height;
        // RGBA output
        int rgbInc = 1;
        if (mPreviewRotation == 180) {
            rgbInc = -1;
        }
        int index = 0;
        for (int y = 0; y < h; y++) {
            int rgbIndex = y * w;
            if (mPreviewRotation == 180) {
                rgbIndex = w * (h - y) - 1;
            }
            for (int x = 0; x < mPreviewSize.width/3; x++) {
                int r = data[index + 0] & 0xFF;
                int g = data[index + 1] & 0xFF;
                int b = data[index + 2] & 0xFF;
                rgbData[rgbIndex] = Color.rgb(r,g,b);
                rgbIndex += rgbInc;
                index += 3;
            }
        }
    }

    // NV21 is a semi-planar 4:2:0 format, in the order YVU, which means we have:
    // a W x H-size 1-byte-per-pixel Y plane, then
    // a W/2 x H/2-size 2-byte-per-pixel plane, where each pixel has V then U.
    private void convertFromNV21(byte[] data, int rgbData[]) {
        int w = mPreviewSize.width;
        int h = mPreviewSize.height;
        // RGBA output
        int rgbIndex = 0;
        int rgbInc = 1;
        if (mPreviewRotation == 180) {
            rgbIndex = h * w - 1;
            rgbInc = -1;
        }
        int yIndex = 0;
        int uvRowIndex = w*h;
        int uvRowInc = 0;
        for (int y = 0; y < h; y++) {
            int uvInc = 0;
            int vIndex = uvRowIndex;
            int uIndex = uvRowIndex + 1;

            uvRowIndex += uvRowInc * w;
            uvRowInc = (uvRowInc + 1) & 0x1;

            for (int x = 0; x < w; x++) {
                int yv = data[yIndex] & 0xFF;
                int uv = data[uIndex] & 0xFF;
                int vv = data[vIndex] & 0xFF;
                rgbData[rgbIndex] =
                        Color.rgb(yv, uv, vv);

                rgbIndex += rgbInc;
                yIndex += 1;
                uIndex += uvInc;
                vIndex += uvInc;
                uvInc = (uvInc + 2) & 0x2;
            }
        }
    }

    // YV12 is a planar 4:2:0 format, in the order YVU, which means we have:
    // a W x H-size 1-byte-per-pixel Y plane, then
    // a W/2 x H/2-size 1-byte-per-pixel V plane, then
    // a W/2 x H/2-size 1-byte-per-pixel U plane
    // The stride may not be equal to width, since it has to be a multiple of
    // 16 pixels for both the Y and UV planes.
    private void convertFromYV12(byte[] data, int rgbData[]) {
        int w = mPreviewSize.width;
        int h = mPreviewSize.height;
        // RGBA output
        int rgbIndex = 0;
        int rgbInc = 1;
        if (mPreviewRotation == 180) {
            rgbIndex = h * w - 1;
            rgbInc = -1;
        }

        int yStride = (int)Math.ceil(w / 16.0) * 16;
        int uvStride = (int)Math.ceil(yStride/2/16.0) * 16;
        int ySize = yStride * h;
        int uvSize = uvStride * h / 2;

        int uRowIndex = ySize + uvSize;
        int vRowIndex = ySize;

        int uv_w = w/2;
        for (int y = 0; y < h; y++) {
            int yIndex = yStride * y;
            int uIndex = uRowIndex;
            int vIndex = vRowIndex;

            if ( (y & 0x1) == 1) {
                uRowIndex += uvStride;
                vRowIndex += uvStride;
            }

            int uv = 0, vv = 0;
            for (int x = 0; x < w; x++) {
                if ( (x & 0x1)  == 0) {
                    uv = data[uIndex] & 0xFF;
                    vv = data[vIndex] & 0xFF;
                    uIndex++;
                    vIndex++;
                }
                int yv = data[yIndex] & 0xFF;
                rgbData[rgbIndex] =
                        Color.rgb(yv, uv, vv);

                rgbIndex += rgbInc;
                yIndex += 1;
            }
        }
    }

    // YUY2 is an interleaved 4:2:2 format: YU,YV,YU,YV
    private void convertFromYUY2(byte[] data, int[] rgbData) {
        int w = mPreviewSize.width;
        int h = mPreviewSize.height;
        // RGBA output
        int yIndex = 0;
        int uIndex = 1;
        int vIndex = 3;
        int rgbIndex = 0;
        int rgbInc = 1;
        if (mPreviewRotation == 180) {
            rgbIndex = h * w - 1;
            rgbInc = -1;
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int yv = data[yIndex] & 0xFF;
                int uv = data[uIndex] & 0xFF;
                int vv = data[vIndex] & 0xFF;
                rgbData[rgbIndex] = Color.rgb(yv,uv,vv);
                rgbIndex += rgbInc;
                yIndex += 2;
                if ( (x & 0x1) == 1 ) {
                    uIndex += 4;
                    vIndex += 4;
                }
            }
        }
    }

}