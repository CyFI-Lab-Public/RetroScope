/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.soundrecorder;

import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class VUMeter extends View {
    static final float PIVOT_RADIUS = 3.5f;
    static final float PIVOT_Y_OFFSET = 10f;
    static final float SHADOW_OFFSET = 2.0f;
    static final float DROPOFF_STEP = 0.18f;
    static final float SURGE_STEP = 0.35f;
    static final long  ANIMATION_INTERVAL = 70;
    
    Paint mPaint, mShadow;
    float mCurrentAngle;
    
    Recorder mRecorder;

    public VUMeter(Context context) {
        super(context);
        init(context);
    }

    public VUMeter(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    void init(Context context) {
        Drawable background = context.getResources().getDrawable(R.drawable.vumeter);
        setBackgroundDrawable(background);
        
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.WHITE);
        mShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadow.setColor(Color.argb(60, 0, 0, 0));
        
        mRecorder = null;
        
        mCurrentAngle = 0;
    }

    public void setRecorder(Recorder recorder) {
    	mRecorder = recorder;
    	invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final float minAngle = (float)Math.PI/8;
        final float maxAngle = (float)Math.PI*7/8;
                
        float angle = minAngle;
        if (mRecorder != null)
        	angle += (float)(maxAngle - minAngle)*mRecorder.getMaxAmplitude()/32768;

        if (angle > mCurrentAngle)
            mCurrentAngle = angle;
        else
            mCurrentAngle = Math.max(angle, mCurrentAngle - DROPOFF_STEP);

        mCurrentAngle = Math.min(maxAngle, mCurrentAngle);

        float w = getWidth();
        float h = getHeight();
        float pivotX = w/2;
        float pivotY = h - PIVOT_RADIUS - PIVOT_Y_OFFSET;
        float l = h*4/5;
        float sin = (float) Math.sin(mCurrentAngle);
        float cos = (float) Math.cos(mCurrentAngle);
        float x0 = pivotX - l*cos;
        float y0 = pivotY - l*sin;
        canvas.drawLine(x0 + SHADOW_OFFSET, y0 + SHADOW_OFFSET, pivotX + SHADOW_OFFSET, pivotY + SHADOW_OFFSET, mShadow);
        canvas.drawCircle(pivotX + SHADOW_OFFSET, pivotY + SHADOW_OFFSET, PIVOT_RADIUS, mShadow);
        canvas.drawLine(x0, y0, pivotX, pivotY, mPaint);
        canvas.drawCircle(pivotX, pivotY, PIVOT_RADIUS, mPaint);
        
        if (mRecorder != null && mRecorder.state() == Recorder.RECORDING_STATE)
        	postInvalidateDelayed(ANIMATION_INTERVAL);
    }
}
