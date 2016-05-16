package com.android.mail.utils;

import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Given a View to monitor draws on, an instance of this class will notify a listener of state
 * changes between {@link #STATE_ACTIVE} and {@link #STATE_IDLE}.
 * <p>
 * Any drawing will instantly trigger {@link #STATE_ACTIVE}. {@link #STATE_IDLE} is only
 * subsequently triggered if {@link #IDLE_WINDOW_MS} continuous milliseconds elapse with zero draws.
 */
public class DrawIdler {

    public static final int STATE_IDLE = 0;
    public static final int STATE_ACTIVE = 1;

    private static final int IDLE_WINDOW_MS = 100;

    private int mState = STATE_IDLE;

    private View mRoot;
    private IdleListener mListener;
    private final IdleReader mIdleReader = new IdleReader();
    private final Handler mHandler = new Handler();

    public interface IdleListener {
        void onStateChanged(DrawIdler idler, int newState);
    }

    public void setListener(IdleListener listener) {
        mListener = listener;
        if (mListener != null) {
            mListener.onStateChanged(this, mState);
        }
    }

    public void setRootView(View rootView) {
        if (mRoot == rootView) {
            return;
        } else if (mRoot != null) {
            mRoot.getViewTreeObserver().removeOnPreDrawListener(mIdleReader);
        }

        mRoot = rootView;

        if (mRoot != null) {
            mRoot.getViewTreeObserver().addOnPreDrawListener(mIdleReader);
        }
    }

    public int getCurrentState() {
        return mState;
    }

    private void setState(int newState) {
        if (mState == newState) {
            return;
        }
        mState = newState;
        if (mListener != null) {
            mListener.onStateChanged(this, newState);
        }
    }

    // this inner class keeps implementation details private
    // (we use OnPreDrawListener instead of OnDrawListener because the latter is only JB+)
    private class IdleReader implements Runnable, ViewTreeObserver.OnPreDrawListener {

        @Override
        public void run() {
            setState(STATE_IDLE);
        }

        @Override
        public boolean onPreDraw() {
            setState(STATE_ACTIVE);
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, IDLE_WINDOW_MS);
            return true; // always allow the draw; we're only here to observe
        }

    }

}
