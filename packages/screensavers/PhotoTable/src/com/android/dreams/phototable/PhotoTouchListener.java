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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Touch listener that implements phototable interactions.
 */
public class PhotoTouchListener implements View.OnTouchListener {
    private static final String TAG = "PhotoTouchListener";
    private static final boolean DEBUG = false;
    private static final int INVALID_POINTER = -1;
    private static final int MAX_POINTER_COUNT = 10;
    private final int mTouchSlop;
    private final int mTapTimeout;
    private final PhotoTable mTable;
    private final float mBeta;
    private final boolean mEnableFling;
    private final boolean mManualImageRotation;
    private long mLastEventTime;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private float mInitialTouchA;
    private long mInitialTouchTime;
    private float mInitialTargetX;
    private float mInitialTargetY;
    private float mInitialTargetA;
    private float mDX;
    private float mDY;
    private int mA = INVALID_POINTER;
    private int mB = INVALID_POINTER;
    private float[] pts = new float[MAX_POINTER_COUNT];

    public PhotoTouchListener(Context context, PhotoTable table) {
        mTable = table;
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mTapTimeout = ViewConfiguration.getTapTimeout();
        final Resources resources = context.getResources();
        mBeta = resources.getInteger(R.integer.table_damping) / 1000000f;
        mEnableFling = resources.getBoolean(R.bool.enable_fling);
        mManualImageRotation = resources.getBoolean(R.bool.enable_manual_image_rotation);
    }

    /** Get angle defined by first two touches, in degrees */
    private float getAngle(View target, MotionEvent ev) {
        float alpha = 0f;
        int a = ev.findPointerIndex(mA);
        int b = ev.findPointerIndex(mB);
        if (a >=0 && b >=0) {
            alpha = (float) (Math.atan2(pts[2*a + 1] - pts[2*b + 1],
                                        pts[2*a] - pts[2*b]) *
                             180f / Math.PI);
        }
        return alpha;
    }

    private void resetTouch(View target) {
        mInitialTouchX = -1;
        mInitialTouchY = -1;
        mInitialTouchA = 0f;
        mInitialTargetX = (float) target.getX();
        mInitialTargetY = (float) target.getY();
        mInitialTargetA = (float) target.getRotation();
    }

    public void onFling(View target, float dX, float dY) {
        if (!mEnableFling) {
            return;
        }
        log("fling " + dX + ", " + dY);

        // convert to pixel per frame
        dX /= 60f;
        dY /= 60f;

        // starting position compionents in global corrdinate frame
        final float x0 = pts[0];
        final float y0 = pts[1];

        // velocity
        final float v = (float) Math.hypot(dX, dY);

        if (v == 0f) {
            return;
        }

        // number of steps to come to a stop
        final float n = (float) Math.max(1.0, (- Math.log(v) / Math.log(mBeta)));
        // distance travelled before stopping
        final float s = (float) Math.max(0.0, (v * (1f - Math.pow(mBeta, n)) / (1f - mBeta)));

        // ending posiiton after stopping
        final float x1 = x0 + s * dX / v;
        final float y1 = y0 + s * dY / v;

        mTable.fling(target, x1 - x0, y1 - y0, (int) (1000f * n / 60f), false);
    }

    @Override
    public boolean onTouch(View target, MotionEvent ev) {
        final int action = ev.getActionMasked();

        // compute raw coordinates
        for(int i = 0; i < 10 && i < ev.getPointerCount(); i++) {
            pts[i*2] = ev.getX(i);
            pts[i*2 + 1] = ev.getY(i);
        }
        target.getMatrix().mapPoints(pts);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mTable.moveToTopOfPile(target);
            mInitialTouchTime = ev.getEventTime();
            mA = ev.getPointerId(ev.getActionIndex());
            resetTouch(target);
            break;

        case MotionEvent.ACTION_POINTER_DOWN:
            if (mB == INVALID_POINTER) {
                mB = ev.getPointerId(ev.getActionIndex());
                mInitialTouchA = getAngle(target, ev);
            }
            break;

        case MotionEvent.ACTION_POINTER_UP:
            if (mB == ev.getPointerId(ev.getActionIndex())) {
                mB = INVALID_POINTER;
                mInitialTargetA = (float) target.getRotation();
            }
            if (mA == ev.getPointerId(ev.getActionIndex())) {
                log("primary went up!");
                mA = mB;
                resetTouch(target);
                mB = INVALID_POINTER;
            }
            break;

        case MotionEvent.ACTION_MOVE: {
                if (mA != INVALID_POINTER) {
                    int idx = ev.findPointerIndex(mA);
                    float x = pts[2 * idx];
                    float y = pts[2 * idx + 1];
                    if (mInitialTouchX == -1 && mInitialTouchY == -1) {
                        mInitialTouchX = x;
                        mInitialTouchY = y;
                    } else {
                        float dt = (float) (ev.getEventTime() - mLastEventTime) / 1000f;
                        float tmpDX = (x - mLastTouchX) / dt;
                        float tmpDY = (y - mLastTouchY) / dt;
                        if (dt > 0f && (Math.abs(tmpDX) > 5f || Math.abs(tmpDY) > 5f)) {
                            // work around odd bug with multi-finger flings
                            mDX = tmpDX;
                            mDY = tmpDY;
                        }
                        log("move " + mDX + ", " + mDY);

                        mLastEventTime = ev.getEventTime();
                        mLastTouchX = x;
                        mLastTouchY = y;
                    }

                    if (!mTable.hasSelection()) {
                        float rotation = target.getRotation();
                        if (mManualImageRotation && mB != INVALID_POINTER) {
                            float a = getAngle(target, ev);
                            rotation = mInitialTargetA + a - mInitialTouchA;
                        }
                        mTable.move(target,
                                    mInitialTargetX + x - mInitialTouchX,
                                    mInitialTargetY + y - mInitialTouchY,
                                    rotation);
                    }
                }
            }
            break;

        case MotionEvent.ACTION_UP: {
                if (mA != INVALID_POINTER) {
                    int idx = ev.findPointerIndex(mA);
                    float x0 = pts[2 * idx];
                    float y0 = pts[2 * idx + 1];
                    if (mInitialTouchX == -1 && mInitialTouchY == -1) {
                        mInitialTouchX = x0;
                        mInitialTouchY = y0;
                    }
                    double distance = Math.hypot(x0 - mInitialTouchX,
                                                 y0 - mInitialTouchY);
                    if (mTable.hasSelection()) {
                        if (distance < mTouchSlop) {
                          mTable.clearSelection();
                        } else {
                          if ((x0 - mInitialTouchX) > 0f) {
                            mTable.selectPrevious();
                          } else {
                            mTable.selectNext();
                          }
                        }
                    } else if ((ev.getEventTime() - mInitialTouchTime) < mTapTimeout &&
                               distance < mTouchSlop) {
                        // tap
                        mTable.setSelection(target);
                    } else {
                        onFling(target, mDX, mDY);
                    }
                    mA = INVALID_POINTER;
                    mB = INVALID_POINTER;
                }
            }
            break;

        case MotionEvent.ACTION_CANCEL:
            log("action cancel!");
            break;
        }

        return true;
    }

    private static void log(String message) {
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }
}
