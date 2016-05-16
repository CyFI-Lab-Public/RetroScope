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

import com.android.cts.verifier.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Shows the picture taken and lets the user specify the field of view (FOV).
 */
public class DetermineFovActivity extends Activity {

    private static final float FOV_ADJUSTMENT_RANGE = 20;
    private static final int SEEKBAR_MAX_VALUE = 100;
    private static final float TEXT_SIZE = 16;
    private static final float TEXT_PADDING = 0.2f;
    private static final String DEFAULT_MARKER_DISTANCE = "36.8";
    private static final String DEFAULT_TARGET_DISTANCE = "99.7";

    private float mMarkerDistanceCm;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap mPhotoBitmap;
    private float mFovMinDegrees;
    private float mFovMaxDegrees;
    private float mFovDegrees;
    private float mReportedFovDegrees;
    private SeekBar mSeekBar;
    private Button mDoneButton;
    private float mTargetDistanceCm;
    private String mMeasuredText;
    private String mReportedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_fov_calibration_determine_fov);
        File pictureFile = PhotoCaptureActivity.getPictureFile(this);
        try {
            mPhotoBitmap =
                    BitmapFactory.decodeStream(new FileInputStream(pictureFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mSurfaceView = (SurfaceView) findViewById(R.id.camera_fov_photo_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            drawContents();
        }

        @Override
        public void surfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            drawContents();
        }
        });

        mSeekBar = (SeekBar) findViewById(R.id.camera_fov_seekBar);
        mSeekBar.setMax(SEEKBAR_MAX_VALUE);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(
                    SeekBar seekBar, int progress, boolean fromUser) {
                mFovDegrees = seekBarProgressToFovDegrees(progress);
                drawContents();
            }
        });

        mDoneButton = (Button) findViewById(R.id.camera_fov_fov_done);
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                CtsTestHelper.storeCtsTestResult(DetermineFovActivity.this,
                        mReportedFovDegrees, mFovDegrees);
                finish();
            }
        });
    }

    private int fovToSeekBarProgress(float fovDegrees) {
        return Math.round((fovDegrees - mFovMinDegrees)
                / (mFovMaxDegrees - mFovMinDegrees) * SEEKBAR_MAX_VALUE);
    }

    private float seekBarProgressToFovDegrees(int progress) {
        float degrees = mFovMinDegrees + (float) progress / SEEKBAR_MAX_VALUE
                * (mFovMaxDegrees - mFovMinDegrees);
        // keep only 2 decimal places.
        return (int) (degrees * 100) / 100.0f;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setResult(RESULT_CANCELED);
        mMarkerDistanceCm = getMarkerDistance();
        mTargetDistanceCm = getTargetDistance();
        mReportedFovDegrees = PhotoCaptureActivity.getReportedFovDegrees();

        mFovDegrees = mReportedFovDegrees > 80 ? 60 : mReportedFovDegrees;
        mFovMaxDegrees = mFovDegrees + FOV_ADJUSTMENT_RANGE / 2;
        mFovMinDegrees = mFovDegrees - FOV_ADJUSTMENT_RANGE / 2;

        mMeasuredText = getResources().getString(R.string.camera_fov_displayed_fov_label);
        mReportedText = getResources().getString(R.string.camera_fov_reported_fov_label);

        mSeekBar.setProgress(fovToSeekBarProgress(mFovDegrees));
        drawContents();
    }

    private float getMarkerDistance() {
        // Get the marker distance from the preferences.
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        return Float.parseFloat(prefs.getString(
                CalibrationPreferenceActivity.OPTION_MARKER_DISTANCE,
                DEFAULT_MARKER_DISTANCE));
    }

    private float getTargetDistance() {
        // Get the marker distance from the preferences.
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
        return Float.parseFloat(prefs.getString(
                CalibrationPreferenceActivity.OPTION_TARGET_DISTANCE,
                DEFAULT_TARGET_DISTANCE));
    }

    private float focalLengthPixels(float fovDegrees, float imageWidth) {
        return (float) (imageWidth
                / (2 * Math.tan(fovDegrees / 2 * Math.PI / 180.0f)));
    }

    private void drawContents() {
        SurfaceHolder holder = mSurfaceView.getHolder();
        Canvas canvas = holder.lockCanvas();
        if (canvas == null || mPhotoBitmap == null) {
            return;
        }

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int photoWidth = mPhotoBitmap.getWidth();
        int photoHeight = mPhotoBitmap.getHeight();
        RectF drawRect = new RectF();

        // Determine if the canvas aspect ratio is larger than that of the photo.
        float scale = (float) canvasWidth / photoWidth;
        int scaledHeight = (int) (scale * photoHeight);
        if (scaledHeight < canvasHeight) {
            // If the aspect ratio is smaller, set the destination rectangle to pad
            // vertically.
            int pad = (canvasHeight - scaledHeight) / 2;
            drawRect.set(0, pad, canvasWidth, pad + scaledHeight - 1);
        } else {
            // Set the destination rectangle to pad horizontally.
            scale = (float) canvasHeight / photoHeight;
            float scaledWidth = scale * photoWidth;
            float pad = (canvasWidth - scaledWidth) / 2;
            drawRect.set(pad, 0, pad + scaledWidth - 1, canvasHeight);
        }

        // Draw the photo.
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(mPhotoBitmap, null, drawRect, null);

        // Draw the fov indicator text.
        Paint paint = new Paint();
        paint.setColor(0xffffffff);
        float textSize = TEXT_SIZE * DetermineFovActivity.this.getResources()
                .getDisplayMetrics().scaledDensity;
        paint.setTextSize(textSize);
        canvas.drawText(mMeasuredText + " " + mFovDegrees + " degrees.", textSize,
                2 * textSize * (1.0f + TEXT_PADDING), paint);
        canvas.drawText(mReportedText + " " + mReportedFovDegrees + " degrees.",
                textSize, textSize * (1.0f + TEXT_PADDING), paint);

        // Draw the image center circle.
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        float dstWidth = drawRect.right - drawRect.left + 1;
        float dstHeight = drawRect.bottom - drawRect.top + 1;
        float centerX = drawRect.left + dstWidth / 2;
        canvas.drawLine(centerX, drawRect.top, centerX, drawRect.bottom, paint);

        // Project the markers into the scaled image with the given field of view.
        float markerX = mMarkerDistanceCm / 2;
        float markerZ = mTargetDistanceCm;
        float focalLength = focalLengthPixels(mFovDegrees, dstWidth);
        float dx = markerX / markerZ * focalLength;
        float projectedMarkerLeft = dstWidth / 2 - dx;
        float projectedMarkerRight = dstWidth / 2 + dx;

        // Draw the marker lines over the image.
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(2);
        float markerImageLeft = projectedMarkerLeft + drawRect.left;
        canvas.drawLine(
                markerImageLeft, drawRect.top, markerImageLeft, drawRect.bottom, paint);
        float markerImageRight = projectedMarkerRight + drawRect.left;
        canvas.drawLine(markerImageRight, drawRect.top, markerImageRight,
                        drawRect.bottom, paint);

        holder.unlockCanvasAndPost(canvas);
    }
}
