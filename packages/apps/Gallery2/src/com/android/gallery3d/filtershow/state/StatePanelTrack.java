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

package com.android.gallery3d.filtershow.state;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;

public class StatePanelTrack extends LinearLayout implements PanelTrack {

    private static final String LOGTAG = "StatePanelTrack";
    private Point mTouchPoint;
    private StateView mCurrentView;
    private StateView mCurrentSelectedView;
    private boolean mExited = false;
    private boolean mStartedDrag = false;
    private StateAdapter mAdapter;
    private DragListener mDragListener = new DragListener(this);
    private float mDeleteSlope = 0.2f;
    private GestureDetector mGestureDetector;
    private int mElemWidth;
    private int mElemHeight;
    private int mElemSize;
    private int mElemEndSize;
    private int mEndElemWidth;
    private int mEndElemHeight;
    private long mTouchTime;
    private int mMaxTouchDelay = 300; // 300ms delay for touch
    private static final boolean ALLOWS_DRAG = false;
    private static final boolean ALLOWS_DUPLICATES = false;
    private DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            fillContent(false);
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
            fillContent(false);
        }
    };

    public StatePanelTrack(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.StatePanelTrack);
        mElemSize = a.getDimensionPixelSize(R.styleable.StatePanelTrack_elemSize, 0);
        mElemEndSize = a.getDimensionPixelSize(R.styleable.StatePanelTrack_elemEndSize, 0);
        if (getOrientation() == LinearLayout.HORIZONTAL) {
            mElemWidth = mElemSize;
            mElemHeight = LayoutParams.MATCH_PARENT;
            mEndElemWidth = mElemEndSize;
            mEndElemHeight = LayoutParams.MATCH_PARENT;
        } else {
            mElemWidth = LayoutParams.MATCH_PARENT;
            mElemHeight = mElemSize;
            mEndElemWidth = LayoutParams.MATCH_PARENT;
            mEndElemHeight = mElemEndSize;
        }
        GestureDetector.SimpleOnGestureListener simpleOnGestureListener
                = new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent e) {
                longPress(e);
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                addDuplicate(e);
                return true;
            }
        };
        mGestureDetector = new GestureDetector(context, simpleOnGestureListener);
    }

    private void addDuplicate(MotionEvent e) {
        if (!ALLOWS_DUPLICATES) {
            return;
        }
        if (mCurrentSelectedView == null) {
            return;
        }
        int pos = findChild(mCurrentSelectedView);
        if (pos != -1) {
            mAdapter.insert(new State(mCurrentSelectedView.getState()), pos);
            fillContent(true);
        }
    }

    private void longPress(MotionEvent e) {
        if (!ALLOWS_DUPLICATES) {
            return;
        }
        View view = findChildAt((int) e.getX(), (int) e.getY());
        if (view == null) {
            return;
        }
        if (view instanceof StateView) {
            StateView stateView = (StateView) view;
            stateView.setDuplicateButton(true);
        }
    }

    public void setAdapter(StateAdapter adapter) {
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mObserver);
        mAdapter.setOrientation(getOrientation());
        fillContent(false);
        requestLayout();
    }

    public StateView findChildWithState(State state) {
        for (int i = 0; i < getChildCount(); i++) {
            StateView view = (StateView) getChildAt(i);
            if (view.getState() == state) {
                return view;
            }
        }
        return null;
    }

    public void fillContent(boolean animate) {
        if (!animate) {
            this.setLayoutTransition(null);
        }
        int n = mAdapter.getCount();
        for (int i = 0; i < getChildCount(); i++) {
            StateView child = (StateView) getChildAt(i);
            child.resetPosition();
            if (!mAdapter.contains(child.getState())) {
                removeView(child);
            }
        }
        LayoutParams params = new LayoutParams(mElemWidth, mElemHeight);
        for (int i = 0; i < n; i++) {
            State s = mAdapter.getItem(i);
            if (findChildWithState(s) == null) {
                View view = mAdapter.getView(i, null, this);
                addView(view, i, params);
            }
        }

        for (int i = 0; i < n; i++) {
            State state = mAdapter.getItem(i);
            StateView view = (StateView) getChildAt(i);
            view.setState(state);
            if (i == 0) {
                view.setType(StateView.BEGIN);
            } else if (i == n - 1) {
                view.setType(StateView.END);
            } else {
                view.setType(StateView.DEFAULT);
            }
            view.resetPosition();
        }

        if (!animate) {
            this.setLayoutTransition(new LayoutTransition());
        }
    }

    public void onTouch(MotionEvent event, StateView view) {
        if (!view.isDraggable()) {
            return;
        }
        mCurrentView = view;
        if (mCurrentSelectedView == mCurrentView) {
            return;
        }
        if (mCurrentSelectedView != null) {
            mCurrentSelectedView.setSelected(false);
        }
        // We changed the current view -- let's reset the
        // gesture detector.
        MotionEvent cancelEvent = MotionEvent.obtain(event);
        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
        mGestureDetector.onTouchEvent(cancelEvent);
        mCurrentSelectedView = mCurrentView;
        // We have to send the event to the gesture detector
        mGestureDetector.onTouchEvent(event);
        mTouchTime = System.currentTimeMillis();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mCurrentView != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCurrentView == null) {
            return false;
        }
        if (mTouchTime == 0) {
            mTouchTime = System.currentTimeMillis();
        }
        mGestureDetector.onTouchEvent(event);
        if (mTouchPoint == null) {
            mTouchPoint = new Point();
            mTouchPoint.x = (int) event.getX();
            mTouchPoint.y = (int) event.getY();
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float translation = event.getY() - mTouchPoint.y;
            float alpha = 1.0f - (Math.abs(translation) / mCurrentView.getHeight());
            if (getOrientation() == LinearLayout.VERTICAL) {
                translation = event.getX() - mTouchPoint.x;
                alpha = 1.0f - (Math.abs(translation) / mCurrentView.getWidth());
                mCurrentView.setTranslationX(translation);
            } else {
                mCurrentView.setTranslationY(translation);
            }
            mCurrentView.setBackgroundAlpha(alpha);
            if (ALLOWS_DRAG && alpha < 0.7) {
                setOnDragListener(mDragListener);
                DragShadowBuilder shadowBuilder = new DragShadowBuilder(mCurrentView);
                mCurrentView.startDrag(null, shadowBuilder, mCurrentView, 0);
                mStartedDrag = true;
            }
        }
        if (!mExited && mCurrentView != null
                && mCurrentView.getBackgroundAlpha() > mDeleteSlope
                && event.getActionMasked() == MotionEvent.ACTION_UP
                && System.currentTimeMillis() - mTouchTime < mMaxTouchDelay) {
            FilterRepresentation representation = mCurrentView.getState().getFilterRepresentation();
            mCurrentView.setSelected(true);
            if (representation != MasterImage.getImage().getCurrentFilterRepresentation()) {
                FilterShowActivity activity = (FilterShowActivity) getContext();
                activity.showRepresentation(representation);
                mCurrentView.setSelected(false);
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || (!mStartedDrag && event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
            checkEndState();
            if (mCurrentView != null) {
                FilterRepresentation representation = mCurrentView.getState().getFilterRepresentation();
                if (representation.getEditorId() == ImageOnlyEditor.ID) {
                    mCurrentView.setSelected(false);
                }
            }
        }
        return true;
    }

    public void checkEndState() {
        mTouchPoint = null;
        mTouchTime = 0;
        if (mExited || mCurrentView.getBackgroundAlpha() < mDeleteSlope) {
            int origin = findChild(mCurrentView);
            if (origin != -1) {
                State current = mAdapter.getItem(origin);
                FilterRepresentation currentRep = MasterImage.getImage().getCurrentFilterRepresentation();
                FilterRepresentation removedRep = current.getFilterRepresentation();
                mAdapter.remove(current);
                fillContent(true);
                if (currentRep != null && removedRep != null
                        && currentRep.getFilterClass() == removedRep.getFilterClass()) {
                    FilterShowActivity activity = (FilterShowActivity) getContext();
                    activity.backToMain();
                    return;
                }
            }
        } else {
            mCurrentView.setBackgroundAlpha(1.0f);
            mCurrentView.setTranslationX(0);
            mCurrentView.setTranslationY(0);
        }
        if (mCurrentSelectedView != null) {
            mCurrentSelectedView.invalidate();
        }
        if (mCurrentView != null) {
            mCurrentView.invalidate();
        }
        mCurrentView = null;
        mExited = false;
        mStartedDrag = false;
    }

    public View findChildAt(int x, int y) {
        Rect frame = new Rect();
        int scrolledXInt = getScrollX() + x;
        int scrolledYInt = getScrollY() + y;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.getHitRect(frame);
            if (frame.contains(scrolledXInt, scrolledYInt)) {
                return child;
            }
        }
        return null;
    }

    public int findChild(View view) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == view) {
                return i;
            }
        }
        return -1;
    }

    public StateView getCurrentView() {
        return mCurrentView;
    }

    public void setCurrentView(View currentView) {
        mCurrentView = (StateView) currentView;
    }

    public void setExited(boolean value) {
        mExited = value;
    }

    public Point getTouchPoint() {
        return mTouchPoint;
    }

    public Adapter getAdapter() {
        return mAdapter;
    }
}
