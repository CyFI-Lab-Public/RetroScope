package com.android.mail.browse;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebSettings;

/**
 * For zooming within a single message.
 */
public class MessageView extends MailWebView {

    public MessageView(Context c) {
        this(c, null);
    }

    public MessageView(Context c, AttributeSet attrs) {
        super(c, attrs);

        final WebSettings settings = getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        requestDisallowInterceptTouchEvent(true);
        return super.onTouchEvent(event);
    }

}
