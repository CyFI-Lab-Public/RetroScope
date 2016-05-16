package com.android.mail.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

public class EmptyConversationListView extends TextView {

    public EmptyConversationListView(Context context) {
        this(context, null);
    }

    public EmptyConversationListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // In order for users to perform swipe down in this text view to trigger
        // refresh, we always return true here so that ACTION_MOVE and ACTION_UP
        // events would be passed to parent view ConversationListView, which is
        // where swipe to refresh detecting happens.
        return true;
    }
}
