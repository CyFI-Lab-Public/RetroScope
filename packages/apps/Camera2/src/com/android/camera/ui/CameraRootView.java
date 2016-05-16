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

package com.android.camera.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;

@SuppressLint("NewApi")
public class CameraRootView extends FrameLayout {

    private int mTopMargin = 0;
    private int mBottomMargin = 0;
    private int mLeftMargin = 0;
    private int mRightMargin = 0;
    private final Rect mCurrentInsets = new Rect(0, 0, 0, 0);
    private int mOffset = 0;
    private Object mDisplayListener;
    private MyDisplayListener mListener;

    public interface MyDisplayListener {
        public void onDisplayChanged();
    }

    public CameraRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDisplayListener();
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        // insets include status bar, navigation bar, etc
        // In this case, we are only concerned with the size of nav bar
        if (mCurrentInsets.equals(insets)) {
            // Local copy of the insets is up to date. No need to do anything.
            return false;
        }

        if (mOffset == 0) {
            if (insets.bottom > 0) {
                mOffset = insets.bottom;
            } else if (insets.right > 0) {
                mOffset = insets.right;
            }
        }
        mCurrentInsets.set(insets);
        // Make sure onMeasure will be called to adapt to the new insets.
        requestLayout();
        return false;
    }

    public void initDisplayListener() {
        if (ApiHelper.HAS_DISPLAY_LISTENER) {
            mDisplayListener = new DisplayListener() {

                @Override
                public void onDisplayAdded(int arg0) {}

                @Override
                public void onDisplayChanged(int arg0) {
                    if (mListener != null) {
                        mListener.onDisplayChanged();
                    }
                }

                @Override
                public void onDisplayRemoved(int arg0) {}
            };
        }
    }

    public void removeDisplayChangeListener() {
        mListener = null;
    }

    public void setDisplayChangeListener(MyDisplayListener listener) {
        mListener = listener;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (ApiHelper.HAS_DISPLAY_LISTENER) {
            ((DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE))
            .registerDisplayListener((DisplayListener) mDisplayListener, null);
        }
    }

    @Override
    public void onDetachedFromWindow () {
        super.onDetachedFromWindow();
        if (ApiHelper.HAS_DISPLAY_LISTENER) {
            ((DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE))
            .unregisterDisplayListener((DisplayListener) mDisplayListener);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int rotation = CameraUtil.getDisplayRotation((Activity) getContext());
        // all the layout code assumes camera device orientation to be portrait
        // adjust rotation for landscape
        int orientation = getResources().getConfiguration().orientation;
        int camOrientation = (rotation % 180 == 0) ? Configuration.ORIENTATION_PORTRAIT
                : Configuration.ORIENTATION_LANDSCAPE;
        if (camOrientation != orientation) {
            rotation = (rotation + 90) % 360;
        }
        // calculate margins
        mLeftMargin = 0;
        mRightMargin = 0;
        mBottomMargin = 0;
        mTopMargin = 0;
        switch (rotation) {
            case 0:
                mBottomMargin += mOffset;
                break;
            case 90:
                mRightMargin += mOffset;
                break;
            case 180:
                mTopMargin += mOffset;
                break;
            case 270:
                mLeftMargin += mOffset;
                break;
        }
        if (mCurrentInsets != null) {
            if (mCurrentInsets.right > 0) {
                // navigation bar on the right
                mRightMargin = mRightMargin > 0 ? mRightMargin : mCurrentInsets.right;
            } else {
                // navigation bar on the bottom
                mBottomMargin = mBottomMargin > 0 ? mBottomMargin : mCurrentInsets.bottom;
            }
        }
        // make sure all the children are resized
        super.onMeasure(widthMeasureSpec - mLeftMargin - mRightMargin,
                heightMeasureSpec - mTopMargin - mBottomMargin);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        r -= l;
        b -= t;
        l = 0;
        t = 0;
        int orientation = getResources().getConfiguration().orientation;
        // Lay out children
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof CameraControls) {
                // Lay out camera controls to center on the short side of the screen
                // so that they stay in place during rotation
                int width = v.getMeasuredWidth();
                int height = v.getMeasuredHeight();
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    int left = (l + r - width) / 2;
                    v.layout(left, t + mTopMargin, left + width, b - mBottomMargin);
                } else {
                    int top = (t + b - height) / 2;
                    v.layout(l + mLeftMargin, top, r - mRightMargin, top + height);
                }
            } else {
                v.layout(l + mLeftMargin, t + mTopMargin, r - mRightMargin, b - mBottomMargin);
            }
        }
    }
}
