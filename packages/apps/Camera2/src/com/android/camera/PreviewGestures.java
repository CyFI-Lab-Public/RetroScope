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

package com.android.camera;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.RenderOverlay;
import com.android.camera.ui.ZoomRenderer;

/* PreviewGestures disambiguates touch events received on RenderOverlay
 * and dispatch them to the proper recipient (i.e. zoom renderer or pie renderer).
 * Touch events on CameraControls will be handled by framework.
 * */
public class PreviewGestures
        implements ScaleGestureDetector.OnScaleGestureListener {

    private static final String TAG = "CAM_gestures";

    private static final int MODE_NONE = 0;
    private static final int MODE_ZOOM = 2;

    public static final int DIR_UP = 0;
    public static final int DIR_DOWN = 1;
    public static final int DIR_LEFT = 2;
    public static final int DIR_RIGHT = 3;

    private SingleTapListener mTapListener;
    private RenderOverlay mOverlay;
    private PieRenderer mPie;
    private ZoomRenderer mZoom;
    private MotionEvent mDown;
    private MotionEvent mCurrent;
    private ScaleGestureDetector mScale;
    private int mMode;
    private boolean mZoomEnabled;
    private boolean mEnabled;
    private boolean mZoomOnly;
    private GestureDetector mGestureDetector;

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public void onLongPress (MotionEvent e) {
            // Open pie
            if (!mZoomOnly && mPie != null && !mPie.showsItems()) {
                openPie();
            }
        }

        @Override
        public boolean onSingleTapUp (MotionEvent e) {
            // Tap to focus when pie is not open
            if (mPie == null || !mPie.showsItems()) {
                mTapListener.onSingleTapUp(null, (int) e.getX(), (int) e.getY());
                return true;
            }
            return false;
        }

        @Override
        public boolean onScroll (MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1 == null) {
                // e1 can be null if for some cases.
                return false;
            }
            if (mZoomOnly || mMode == MODE_ZOOM) return false;
            int deltaX = (int) (e1.getX() - e2.getX());
            int deltaY = (int) (e1.getY() - e2.getY());
            if (deltaY > 2 * deltaX && deltaY > -2 * deltaX) {
                // Open pie on swipe up
                if (mPie != null && !mPie.showsItems()) {
                    openPie();
                    return true;
                }
            }
            return false;
        }
    };

    public interface SingleTapListener {
        public void onSingleTapUp(View v, int x, int y);
    }

    public PreviewGestures(CameraActivity ctx, SingleTapListener tapListener,
            ZoomRenderer zoom, PieRenderer pie) {
        mTapListener = tapListener;
        mPie = pie;
        mZoom = zoom;
        mMode = MODE_NONE;
        mScale = new ScaleGestureDetector(ctx, this);
        mEnabled = true;
        mGestureDetector = new GestureDetector(mGestureListener);
    }

    public void setRenderOverlay(RenderOverlay overlay) {
        mOverlay = overlay;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public void setZoomEnabled(boolean enable) {
        mZoomEnabled = enable;
    }

    public void setZoomOnly(boolean zoom) {
        mZoomOnly = zoom;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public boolean dispatchTouch(MotionEvent m) {
        if (!mEnabled) {
            return false;
        }
        mCurrent = m;
        if (MotionEvent.ACTION_DOWN == m.getActionMasked()) {
            mMode = MODE_NONE;
            mDown = MotionEvent.obtain(m);
        }

        // If pie is open, redirects all the touch events to pie.
        if (mPie != null && mPie.isOpen()) {
            return sendToPie(m);
        }

        // If pie is not open, send touch events to gesture detector and scale
        // listener to recognize the gesture.
        mGestureDetector.onTouchEvent(m);
        if (mZoom != null) {
            mScale.onTouchEvent(m);
            if (MotionEvent.ACTION_POINTER_DOWN == m.getActionMasked()) {
                mMode = MODE_ZOOM;
                if (mZoomEnabled) {
                    // Start showing zoom UI as soon as there is a second finger down
                    mZoom.onScaleBegin(mScale);
                }
            } else if (MotionEvent.ACTION_POINTER_UP == m.getActionMasked()) {
                mZoom.onScaleEnd(mScale);
            }
        }
        return true;
    }

    private MotionEvent makeCancelEvent(MotionEvent m) {
        MotionEvent c = MotionEvent.obtain(m);
        c.setAction(MotionEvent.ACTION_CANCEL);
        return c;
    }

    private void openPie() {
        mGestureDetector.onTouchEvent(makeCancelEvent(mDown));
        mScale.onTouchEvent(makeCancelEvent(mDown));
        mOverlay.directDispatchTouch(mDown, mPie);
    }

    private boolean sendToPie(MotionEvent m) {
        return mOverlay.directDispatchTouch(m, mPie);
    }

    // OnScaleGestureListener implementation
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        return mZoom.onScale(detector);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (mPie == null || !mPie.isOpen()) {
            mMode = MODE_ZOOM;
            mGestureDetector.onTouchEvent(makeCancelEvent(mCurrent));
            if (!mZoomEnabled) return false;
            return mZoom.onScaleBegin(detector);
        }
        return false;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mZoom.onScaleEnd(detector);
    }
}

