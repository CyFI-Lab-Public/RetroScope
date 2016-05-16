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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.res.Resources;
import android.view.MotionEvent;

/**
 * Detect and dispatch edge events.
 */
public class DragGestureDetector {
    @SuppressWarnings("unused")
    private static final String TAG = "DragGestureDetector";

    private final PhotoTable mTable;
    private final float mTouchGain;

    private float[] mLast;
    private float[] mCurrent;
    private boolean mDrag;

    public DragGestureDetector(Context context, PhotoTable table) {
        Resources res = context.getResources();
        mTouchGain = res.getInteger(R.integer.generalized_touch_gain) / 1000000f;
        mTable = table;
        mLast = new float[2];
        mCurrent = new float[2];
    }

    private void computeAveragePosition(MotionEvent event, float[] position) {
        computeAveragePosition(event, position, -1);
    }

    private void computeAveragePosition(MotionEvent event, float[] position, int ignore) {
        final int pointerCount = event.getPointerCount();
        position[0] = 0f;
        position[1] = 0f;
        float count = 0f;
        for (int p = 0; p < pointerCount; p++) {
            if (p != ignore) {
                position[0] += event.getX(p);
                position[1] += event.getY(p);
                count += 1f;
            }
        }
        position[0] /= count;
        position[1] /= count;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int index = event.getActionIndex();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                computeAveragePosition(event, mLast);
                mDrag = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                mDrag = mTable.hasFocus();
                computeAveragePosition(event, mLast);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                computeAveragePosition(event, mLast, index);
                break;

            case MotionEvent.ACTION_MOVE:
                computeAveragePosition(event, mCurrent);
                if (mDrag) {
                    move(event, false);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mDrag) {
                    move(event, true);
                }
                mDrag = false;
                break;
        }

        if (mDrag) {
            mTable.refreshFocus();
        }

        return mDrag;
    }

    private void move(MotionEvent event, boolean drop) {
        mTable.move(mTable.getFocus(),
                mTouchGain * (mCurrent[0] - mLast[0]),
                mTouchGain * (mCurrent[1] - mLast[1]),
                drop);
        mLast[0] = mCurrent[0];
        mLast[1] = mCurrent[1];
    }
}

