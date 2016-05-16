/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.zxing.client.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.IOException;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

  private static final String LOG_TAG = CaptureActivity.class.getCanonicalName();

  private CameraManager cameraManager;
  private CaptureActivityHandler handler;
  private ViewfinderView viewfinderView;
  private TextView statusView;
  private boolean hasSurface;
  private String characterSet;
  private InactivityTimer inactivityTimer;

  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }

  CameraManager getCameraManager() {
    return cameraManager;
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    statusView = (TextView) findViewById(R.id.status_view);
    handler = null;
    hasSurface = false;
    inactivityTimer = new InactivityTimer(this);
  }

  @Override
  protected void onResume() {
    super.onResume();

    // CameraManager must be initialized here, not in onCreate(). This is
    // necessary because we don't want to open the camera driver and measure the
    // screen size if we're going to show the help on first launch. That led to
    // bugs where the scanning rectangle was the wrong size and partially off
    // screen.
    cameraManager = new CameraManager(getApplication());
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    viewfinderView.setCameraManager(cameraManager);

    resetStatusView();

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists.
      // Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Intent intent = getIntent();
    String action = intent == null ? null : intent.getAction();
    String dataString = intent == null ? null : intent.getDataString();
    if (intent != null && action != null) {
      if (action.equals(Intents.Scan.ACTION)) {
        if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
          int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
          int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
          if (width > 0 && height > 0) {
            cameraManager.setManualFramingRect(width, height);
          }
        }
      }
      characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
    } else {
      finish();
    }

    inactivityTimer.onResume();
  }

  @Override
  protected void onPause() {
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    cameraManager.closeDriver();
    if (!hasSurface) {
      SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
      SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.removeCallback(this);
    }
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      setResult(RESULT_CANCELED);
      finish();
      return true;
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
      // Handle these events so they don't launch the Camera app
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    if (holder == null) {
      Log.e(LOG_TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
    }
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show
   * the results.
   *
   * @param rawResult The contents of the barcode.
   * @param barcode A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode) {
    inactivityTimer.onActivity();

    if (barcode == null) {
      Log.e(LOG_TAG, "Barcode not recognized");
      setResult(RESULT_CANCELED, null);
    } else {
      drawResultPoints(barcode, rawResult);
      Log.d(LOG_TAG, "Barcode is: " + rawResult.getText());
      Intent result = new Intent();
      result.putExtra("SCAN_RESULT", rawResult.getText());
      setResult(RESULT_OK, result);
    }
    finish();
  }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of
   * the barcode.
   *
   * @param barcode A bitmap of the captured image.
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_image_border));
      paint.setStrokeWidth(3.0f);
      paint.setStyle(Paint.Style.STROKE);
      Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
      canvas.drawRect(border, paint);

      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4.0f);
        drawLine(canvas, paint, points[0], points[1]);
      } else if (points.length == 4 && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A
          || rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
        // Hacky special case -- draw two lines, for the barcode and metadata
        drawLine(canvas, paint, points[0], points[1]);
        drawLine(canvas, paint, points[2], points[3]);
      } else {
        paint.setStrokeWidth(10.0f);
        for (ResultPoint point : points) {
          canvas.drawPoint(point.getX(), point.getY(), paint);
        }
      }
    }
  }

  private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
    canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
    try {
      cameraManager.openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a
      // RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, characterSet, cameraManager);
      }
    } catch (IOException ioe) {
      Log.w(LOG_TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(LOG_TAG, "Unexpected error initializing camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("");
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(android.R.string.ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  private void resetStatusView() {
    statusView.setText(R.string.msg_default_status);
    statusView.setVisibility(View.VISIBLE);
    viewfinderView.setVisibility(View.VISIBLE);
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }
}
