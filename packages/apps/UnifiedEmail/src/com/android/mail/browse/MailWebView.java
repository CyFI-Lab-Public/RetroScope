package com.android.mail.browse;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class MailWebView extends WebView {

    // NARROW_COLUMNS reflow can trigger the document to change size, so notify interested parties.
    // This is also a good trigger to know when to alter scroll position.
    public interface ContentSizeChangeListener {
        void onHeightChange(int h);
    }

    private int mCachedContentHeight;

    private ContentSizeChangeListener mSizeChangeListener;

    public MailWebView(Context c) {
        this(c, null);
    }

    public MailWebView(Context c, AttributeSet attrs) {
        super(c, attrs);
    }

    @Override
    public int computeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    public int computeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    @Override
    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    public void setContentSizeChangeListener(ContentSizeChangeListener l) {
        mSizeChangeListener = l;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (mSizeChangeListener != null) {
            final int contentHeight = getContentHeight();
            if (contentHeight != mCachedContentHeight) {
                mCachedContentHeight = contentHeight;
                mSizeChangeListener.onHeightChange(contentHeight);
            }
        }
    }

}
