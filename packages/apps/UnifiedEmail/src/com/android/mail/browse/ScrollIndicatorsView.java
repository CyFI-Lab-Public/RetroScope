// Copyright 2011 Google Inc. All Rights Reserved.

package com.android.mail.browse;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.mail.browse.ScrollNotifier.ScrollListener;

/**
 * An overlay to sit on top of WebView, message headers, and snap header to display scrollbars.
 * It has to sit on top of all other views that compose the conversation so that the scrollbars are
 * not obscured.
 *
 */
public class ScrollIndicatorsView extends View implements ScrollListener {

    private ScrollNotifier mSource;

    public ScrollIndicatorsView(Context context) {
        super(context);
    }

    public ScrollIndicatorsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSourceView(ScrollNotifier notifier) {
        mSource = notifier;
        mSource.addScrollListener(this);
    }

    @Override
    protected int computeVerticalScrollRange() {
        return mSource.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return mSource.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return mSource.computeVerticalScrollExtent();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return mSource.computeHorizontalScrollRange();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return mSource.computeHorizontalScrollOffset();
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return mSource.computeHorizontalScrollExtent();
    }

    @Override
    public void onNotifierScroll(int left, int top) {
        awakenScrollBars();
    }
}
