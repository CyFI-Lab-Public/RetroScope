/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

import com.android.mail.R;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConversationWebView extends MailWebView implements ScrollNotifier {
    /** The initial delay when rendering in hardware layer. */
    private final int mWebviewInitialDelay;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private boolean mUseSoftwareLayer;
    /**
     * Whether this view is user-visible; we don't bother doing supplemental software drawing
     * if the view is off-screen.
     */
    private boolean mVisible;

    /** {@link Runnable} to be run when the page is rendered in hardware layer. */
    private final Runnable mNotifyPageRenderedInHardwareLayer = new Runnable() {
        @Override
        public void run() {
            // Switch to hardware layer.
            mUseSoftwareLayer = false;
            destroyBitmap();
            invalidate();
        }
    };

    @Override
    public void onDraw(Canvas canvas) {
        // Always render in hardware layer to avoid flicker when switch.
        super.onDraw(canvas);

        // Render in software layer on top if needed, and we're visible (i.e. it's worthwhile to
        // do all this)
        if (mUseSoftwareLayer && mVisible && getWidth() > 0 && getHeight() > 0) {
            if (mBitmap == null) {
                try {
                    // Create an offscreen bitmap.
                    mBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
                    mCanvas = new Canvas(mBitmap);
                } catch (OutOfMemoryError e) {
                    // just give up
                    mBitmap = null;
                    mCanvas = null;
                    mUseSoftwareLayer = false;
                }
            }

            if (mBitmap != null) {
                final int x = getScrollX();
                final int y = getScrollY();

                mCanvas.save();
                mCanvas.translate(-x, -y);
                super.onDraw(mCanvas);
                mCanvas.restore();

                canvas.drawBitmap(mBitmap, x, y, null /* paint */);
            }
        }
    }

    @Override
    public void destroy() {
        destroyBitmap();
        removeCallbacks(mNotifyPageRenderedInHardwareLayer);

        super.destroy();
    }

    /**
     * Destroys the {@link Bitmap} used for software layer.
     */
    private void destroyBitmap() {
        if (mBitmap != null) {
            mBitmap = null;
            mCanvas = null;
        }
    }

    /**
     * Enable this WebView to also draw to an internal software canvas until
     * {@link #onRenderComplete()} is called. The software draw will happen every time
     * a normal {@link #onDraw(Canvas)} happens, and will overwrite whatever is normally drawn
     * (i.e. drawn in hardware) with the results of software rendering.
     * <p>
     * This is useful when you know that the WebView draws sooner to a software layer than it does
     * to its normal hardware layer.
     */
    public void setUseSoftwareLayer(boolean useSoftware) {
        mUseSoftwareLayer = useSoftware;
    }

    /**
     * Notifies the {@link ConversationWebView} that it has become visible. It can use this signal
     * to switch between software and hardware layer.
     */
    public void onRenderComplete() {
        if (mUseSoftwareLayer) {
            // Schedule to switch from software layer to hardware layer in 1s.
            postDelayed(mNotifyPageRenderedInHardwareLayer, mWebviewInitialDelay);
        }
    }

    public void onUserVisibilityChanged(boolean visible) {
        mVisible = visible;
    }

    private ScaleGestureDetector mScaleDetector;

    private final int mViewportWidth;
    private final float mDensity;

    private final Set<ScrollListener> mScrollListeners =
            new CopyOnWriteArraySet<ScrollListener>();

    /**
     * True when WebView is handling a touch-- in between POINTER_DOWN and
     * POINTER_UP/POINTER_CANCEL.
     */
    private boolean mHandlingTouch;
    private boolean mIgnoringTouch;

    private static final String LOG_TAG = LogTag.getLogTag();

    public ConversationWebView(Context c) {
        this(c, null);
    }

    public ConversationWebView(Context c, AttributeSet attrs) {
        super(c, attrs);

        final Resources r = getResources();
        mViewportWidth = r.getInteger(R.integer.conversation_webview_viewport_px);
        mWebviewInitialDelay = r.getInteger(R.integer.webview_initial_delay);
        mDensity = r.getDisplayMetrics().density;
    }

    @Override
    public void addScrollListener(ScrollListener l) {
        mScrollListeners.add(l);
    }

    @Override
    public void removeScrollListener(ScrollListener l) {
        mScrollListeners.remove(l);
    }

    public void setOnScaleGestureListener(OnScaleGestureListener l) {
        if (l == null) {
            mScaleDetector = null;
        } else {
            mScaleDetector = new ScaleGestureDetector(getContext(), l);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        for (ScrollListener listener : mScrollListeners) {
            listener.onNotifierScroll(l, t);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHandlingTouch = true;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                LogUtils.d(LOG_TAG, "WebView disabling intercepts: POINTER_DOWN");
                requestDisallowInterceptTouchEvent(true);
                if (mScaleDetector != null) {
                    mIgnoringTouch = true;
                    final MotionEvent fakeCancel = MotionEvent.obtain(ev);
                    fakeCancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.onTouchEvent(fakeCancel);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mHandlingTouch = false;
                mIgnoringTouch = false;
                break;
        }

        final boolean handled = mIgnoringTouch || super.onTouchEvent(ev);

        if (mScaleDetector != null) {
            mScaleDetector.onTouchEvent(ev);
        }

        return handled;
    }

    public boolean isHandlingTouch() {
        return mHandlingTouch;
    }

    public int getViewportWidth() {
        return mViewportWidth;
    }

    /**
     * Similar to {@link #getScale()}, except that it returns the initially expected scale, as
     * determined by the ratio of actual screen pixels to logical HTML pixels.
     * <p>This assumes that we are able to control the logical HTML viewport with a meta-viewport
     * tag.
     */
    public float getInitialScale() {
        // an HTML meta-viewport width of "device-width" and unspecified (medium) density means
        // that the default scale is effectively the screen density.
        return mDensity;
    }

    public int screenPxToWebPx(int screenPx) {
        return (int) (screenPx / getInitialScale());
    }

    public int webPxToScreenPx(int webPx) {
        return (int) (webPx * getInitialScale());
    }

    public float screenPxToWebPxError(int screenPx) {
        return screenPx / getInitialScale() - screenPxToWebPx(screenPx);
    }

    public float webPxToScreenPxError(int webPx) {
        return webPx * getInitialScale() - webPxToScreenPx(webPx);
    }

}
