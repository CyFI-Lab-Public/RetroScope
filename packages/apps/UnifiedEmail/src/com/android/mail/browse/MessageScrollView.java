/*
 * Copyright (C) 2013 Google Inc.
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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

import com.android.mail.utils.LogUtils;

/**
 * A container that tries to play nice with an internally scrollable {@link Touchable} child view.
 * The assumption is that the child view can scroll horizontally, but not vertically, so any
 * touch events on that child view should ALSO be sent here so it can simultaneously vertically
 * scroll (not the standard either/or behavior).
 * <p>
 * Touch events on any other child of this ScrollView are intercepted in the standard fashion.
 */
public class MessageScrollView extends ScrollView {

    /**
     * A View that reports whether onTouchEvent() was recently called.
     */
    public interface Touchable {
        boolean wasTouched();
        void clearTouched();
    }

    /**
     * True when performing "special" interception.
     */
    private boolean mWantToIntercept;
    /**
     * Whether to perform the standard touch interception procedure. This is set to true when we
     * want to intercept a touch stream from any child OTHER than {@link #mTouchableChild}.
     */
    private boolean mInterceptNormally;
    /**
     * The special child that we want to NOT intercept from in the normal way. Instead, this child
     * will continue to receive the touch event stream (so it can handle the horizontal component)
     * while this parent will additionally handle the events to perform vertical scrolling.
     */
    private Touchable mTouchableChild;

    public static final String LOG_TAG = "MsgScroller";

    public MessageScrollView(Context c) {
        this(c, null);
    }

    public MessageScrollView(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    public void setInnerScrollableView(Touchable child) {
        mTouchableChild = child;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mInterceptNormally) {
            LogUtils.d(LOG_TAG, "IN ScrollView.onIntercept, NOW stealing. ev=%s", ev);
            return true;
        } else if (mWantToIntercept) {
            LogUtils.d(LOG_TAG, "IN ScrollView.onIntercept, already stealing. ev=%s", ev);
            return false;
        }

        mWantToIntercept = super.onInterceptTouchEvent(ev);
        LogUtils.d(LOG_TAG, "OUT ScrollView.onIntercept, steal=%s ev=%s", mWantToIntercept, ev);
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                LogUtils.d(LOG_TAG, "IN ScrollView.dispatchTouch, clearing flags");
                mWantToIntercept = false;
                mInterceptNormally = false;
                break;
        }
        if (mTouchableChild != null) {
            mTouchableChild.clearTouched();
        }
        final boolean handled = super.dispatchTouchEvent(ev);
        LogUtils.d(LOG_TAG, "OUT ScrollView.dispatchTouch, handled=%s ev=%s", handled, ev);

        if (mWantToIntercept) {
            final boolean touchedChild = (mTouchableChild != null && mTouchableChild.wasTouched());
            if (touchedChild) {
                // also give the event to this scroll view if the WebView got the event
                // and didn't stop any parent interception
                LogUtils.d(LOG_TAG, "IN extra ScrollView.onTouch, ev=%s", ev);
                onTouchEvent(ev);
            } else {
                mInterceptNormally = true;
                mWantToIntercept = false;
            }
        }

        return handled;
    }

}
