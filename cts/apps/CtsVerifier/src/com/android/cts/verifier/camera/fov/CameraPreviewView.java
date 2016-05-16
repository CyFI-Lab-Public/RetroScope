// Copyright 2013 Google Inc. All Rights Reserved.
package com.android.cts.verifier.camera.fov;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * View that draws an overlay on the camera preview.
 * @author settinger@google.com(Scott Ettinger)
 */
class CameraPreviewView extends View {

    private static final int GRID_ALPHA = 50;
    private static final int GRID_WIDTH = 50;
    private Paint mPaint = new Paint();

    public CameraPreviewView(Context context) {
        super(context);
        this.setWillNotDraw(false);
    }

    public CameraPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setWillNotDraw(false);
    }

    public CameraPreviewView(
            Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setWillNotDraw(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw a single vertical line on the center of the image to help align
        // the camera when setting up.
        float centerX = canvas.getWidth() / 2.0f;
        float centerY = canvas.getHeight() / 2.0f;
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(3);
        canvas.drawLine(centerX, 0, centerX, canvas.getHeight(), mPaint);

        // Draw the transparent grid.
        mPaint.setAlpha(GRID_ALPHA);
        int vertLines = canvas.getWidth() / 2 / GRID_WIDTH; 
        int horizLines = canvas.getHeight() / 2 / GRID_WIDTH; 
        for (int i = 0; i < horizLines; ++i) { 
            int y = (int) centerY - i * GRID_WIDTH;
            canvas.drawLine(0, y, canvas.getWidth(), y, mPaint);
            y = (int) centerY + i * GRID_WIDTH;
            canvas.drawLine(0, y, canvas.getWidth(), y, mPaint);
        }
        for (int i = 0; i < vertLines; ++i) { 
            int x = (int) centerX - i * GRID_WIDTH;
            canvas.drawLine(x, 0, x, canvas.getHeight(), mPaint);
            x = (int) centerX + i * GRID_WIDTH;
            canvas.drawLine(x, 0, x, canvas.getHeight(), mPaint);
        }
    }
}
