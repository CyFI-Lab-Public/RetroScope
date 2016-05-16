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

package com.android.photos.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.OverScroller;

import java.util.ArrayList;

public class GalleryThumbnailView extends ViewGroup {

    public interface GalleryThumbnailAdapter extends ListAdapter {
        /**
         * @param position Position to get the intrinsic aspect ratio for
         * @return width / height
         */
        float getIntrinsicAspectRatio(int position);
    }

    private static final String TAG = "GalleryThumbnailView";
    private static final float ASPECT_RATIO = (float) Math.sqrt(1.5f);
    private static final int LAND_UNITS = 2;
    private static final int PORT_UNITS = 3;

    private GalleryThumbnailAdapter mAdapter;

    private final RecycleBin mRecycler = new RecycleBin();

    private final AdapterDataSetObserver mObserver = new AdapterDataSetObserver();

    private boolean mDataChanged;
    private int mOldItemCount;
    private int mItemCount;
    private boolean mHasStableIds;

    private int mFirstPosition;

    private boolean mPopulating;
    private boolean mInLayout;

    private int mTouchSlop;
    private int mMaximumVelocity;
    private int mFlingVelocity;
    private float mLastTouchX;
    private float mTouchRemainderX;
    private int mActivePointerId;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DRAGGING = 1;
    private static final int TOUCH_MODE_FLINGING = 2;

    private int mTouchMode;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final OverScroller mScroller;

    private final EdgeEffectCompat mLeftEdge;
    private final EdgeEffectCompat mRightEdge;

    private int mLargeColumnWidth;
    private int mSmallColumnWidth;
    private int mLargeColumnUnitCount = 8;
    private int mSmallColumnUnitCount = 10;

    public GalleryThumbnailView(Context context) {
        this(context, null);
    }

    public GalleryThumbnailView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GalleryThumbnailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
        mFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mScroller = new OverScroller(context);

        mLeftEdge = new EdgeEffectCompat(context);
        mRightEdge = new EdgeEffectCompat(context);
        setWillNotDraw(false);
        setClipToPadding(false);
    }

    @Override
    public void requestLayout() {
        if (!mPopulating) {
            super.requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            Log.e(TAG, "onMeasure: must have an exact width or match_parent! " +
                    "Using fallback spec of EXACTLY " + widthSize);
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            Log.e(TAG, "onMeasure: must have an exact height or match_parent! " +
                    "Using fallback spec of EXACTLY " + heightSize);
        }

        setMeasuredDimension(widthSize, heightSize);

        float portSpaces = mLargeColumnUnitCount / PORT_UNITS;
        float height = getMeasuredHeight() / portSpaces;
        mLargeColumnWidth = (int) (height / ASPECT_RATIO);
        portSpaces++;
        height = getMeasuredHeight() / portSpaces;
        mSmallColumnWidth = (int) (height / ASPECT_RATIO);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        populate();
        mInLayout = false;

        final int width = r - l;
        final int height = b - t;
        mLeftEdge.setSize(width, height);
        mRightEdge.setSize(width, height);
    }

    private void populate() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        // TODO: Handle size changing
//        final int colCount = mColCount;
//        if (mItemTops == null || mItemTops.length != colCount) {
//            mItemTops = new int[colCount];
//            mItemBottoms = new int[colCount];
//            final int top = getPaddingTop();
//            final int offset = top + Math.min(mRestoreOffset, 0);
//            Arrays.fill(mItemTops, offset);
//            Arrays.fill(mItemBottoms, offset);
//            mLayoutRecords.clear();
//            if (mInLayout) {
//                removeAllViewsInLayout();
//            } else {
//                removeAllViews();
//            }
//            mRestoreOffset = 0;
//        }

        mPopulating = true;
        layoutChildren(mDataChanged);
        fillRight(mFirstPosition + getChildCount(), 0);
        fillLeft(mFirstPosition - 1, 0);
        mPopulating = false;
        mDataChanged = false;
    }

    final void layoutChildren(boolean queryAdapter) {
// TODO
//        final int childCount = getChildCount();
//        for (int i = 0; i < childCount; i++) {
//            View child = getChildAt(i);
//
//            if (child.isLayoutRequested()) {
//                final int widthSpec = MeasureSpec.makeMeasureSpec(child.getMeasuredWidth(), MeasureSpec.EXACTLY);
//                final int heightSpec = MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(), MeasureSpec.EXACTLY);
//                child.measure(widthSpec, heightSpec);
//                child.layout(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
//            }
//
//            int childTop = mItemBottoms[col] > Integer.MIN_VALUE ?
//                    mItemBottoms[col] + mItemMargin : child.getTop();
//            if (span > 1) {
//                int lowest = childTop;
//                for (int j = col + 1; j < col + span; j++) {
//                    final int bottom = mItemBottoms[j] + mItemMargin;
//                    if (bottom > lowest) {
//                        lowest = bottom;
//                    }
//                }
//                childTop = lowest;
//            }
//            final int childHeight = child.getMeasuredHeight();
//            final int childBottom = childTop + childHeight;
//            final int childLeft = paddingLeft + col * (colWidth + itemMargin);
//            final int childRight = childLeft + child.getMeasuredWidth();
//            child.layout(childLeft, childTop, childRight, childBottom);
//        }
    }

    /**
     * Obtain the view and add it to our list of children. The view can be made
     * fresh, converted from an unused view, or used as is if it was in the
     * recycle bin.
     *
     * @param startPosition Logical position in the list to start from
     * @param x Left or right edge of the view to add
     * @param forward If true, align left edge to x and increase position.
     *                If false, align right edge to x and decrease position.
     * @return Number of views added
     */
    private int makeAndAddColumn(int startPosition, int x, boolean forward) {
        int columnWidth = mLargeColumnWidth;
        int addViews = 0;
        for (int remaining = mLargeColumnUnitCount, i = 0;
                remaining > 0 && startPosition + i >= 0 && startPosition + i < mItemCount;
                i += forward ? 1 : -1, addViews++) {
            if (mAdapter.getIntrinsicAspectRatio(startPosition + i) >= 1f) {
                // landscape
                remaining -= LAND_UNITS;
            } else {
                // portrait
                remaining -= PORT_UNITS;
                if (remaining < 0) {
                    remaining += (mSmallColumnUnitCount - mLargeColumnUnitCount);
                    columnWidth = mSmallColumnWidth;
                }
            }
        }
        int nextTop = 0;
        for (int i = 0; i < addViews; i++) {
            int position = startPosition + (forward ? i : -i);
            View child = obtainView(position, null);
            if (child.getParent() != this) {
                if (mInLayout) {
                    addViewInLayout(child, forward ? -1 : 0, child.getLayoutParams());
                } else {
                    addView(child, forward ? -1 : 0);
                }
            }
            int heightSize = (int) (.5f + (mAdapter.getIntrinsicAspectRatio(position) >= 1f
                    ? columnWidth / ASPECT_RATIO
                    : columnWidth * ASPECT_RATIO));
            int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
            int widthSpec = MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY);
            child.measure(widthSpec, heightSpec);
            int childLeft = forward ? x : x - columnWidth;
            child.layout(childLeft, nextTop, childLeft + columnWidth, nextTop + heightSize);
            nextTop += heightSize;
        }
        return addViews;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchX = ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderX = 0;
                if (mTouchMode == TOUCH_MODE_FLINGING) {
                    // Catch!
                    mTouchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
                            mActivePointerId + " - did StaggeredGridView receive an inconsistent " +
                            "event stream?");
                    return false;
                }
                final float x = MotionEventCompat.getX(ev, index);
                final float dx = x - mLastTouchX + mTouchRemainderX;
                final int deltaY = (int) dx;
                mTouchRemainderX = dx - deltaY;

                if (Math.abs(dx) > mTouchSlop) {
                    mTouchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchX = ev.getX();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderX = 0;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
                            mActivePointerId + " - did StaggeredGridView receive an inconsistent " +
                            "event stream?");
                    return false;
                }
                final float x = MotionEventCompat.getX(ev, index);
                final float dx = x - mLastTouchX + mTouchRemainderX;
                final int deltaX = (int) dx;
                mTouchRemainderX = dx - deltaX;

                if (Math.abs(dx) > mTouchSlop) {
                    mTouchMode = TOUCH_MODE_DRAGGING;
                }

                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    mLastTouchX = x;

                    if (!trackMotionScroll(deltaX, true)) {
                        // Break fling velocity if we impacted an edge.
                        mVelocityTracker.clear();
                    }
                }
            } break;

            case MotionEvent.ACTION_CANCEL:
                mTouchMode = TOUCH_MODE_IDLE;
                break;

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float velocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                        mActivePointerId);
                if (Math.abs(velocity) > mFlingVelocity) { // TODO
                    mTouchMode = TOUCH_MODE_FLINGING;
                    mScroller.fling(0, 0, (int) velocity, 0,
                            Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 0);
                    mLastTouchX = 0;
                    ViewCompat.postInvalidateOnAnimation(this);
                } else {
                    mTouchMode = TOUCH_MODE_IDLE;
                }

            } break;
        }
        return true;
    }

    /**
     *
     * @param deltaX Pixels that content should move by
     * @return true if the movement completed, false if it was stopped prematurely.
     */
    private boolean trackMotionScroll(int deltaX, boolean allowOverScroll) {
        final boolean contentFits = contentFits();
        final int allowOverhang = Math.abs(deltaX);

        final int overScrolledBy;
        final int movedBy;
        if (!contentFits) {
            final int overhang;
            final boolean up;
            mPopulating = true;
            if (deltaX > 0) {
                overhang = fillLeft(mFirstPosition - 1, allowOverhang);
                up = true;
            } else {
                overhang = fillRight(mFirstPosition + getChildCount(), allowOverhang);
                up = false;
            }
            movedBy = Math.min(overhang, allowOverhang);
            offsetChildren(up ? movedBy : -movedBy);
            recycleOffscreenViews();
            mPopulating = false;
            overScrolledBy = allowOverhang - overhang;
        } else {
            overScrolledBy = allowOverhang;
            movedBy = 0;
        }

        if (allowOverScroll) {
            final int overScrollMode = ViewCompat.getOverScrollMode(this);

            if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
                    (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits)) {

                if (overScrolledBy > 0) {
                    EdgeEffectCompat edge = deltaX > 0 ? mLeftEdge : mRightEdge;
                    edge.onPull((float) Math.abs(deltaX) / getWidth());
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        }

        return deltaX == 0 || movedBy != 0;
    }

    /**
     * Important: this method will leave offscreen views attached if they
     * are required to maintain the invariant that child view with index i
     * is always the view corresponding to position mFirstPosition + i.
     */
    private void recycleOffscreenViews() {
        final int height = getHeight();
        final int clearAbove = 0;
        final int clearBelow = height;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getTop() <= clearBelow)  {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            if (mInLayout) {
                removeViewsInLayout(i, 1);
            } else {
                removeViewAt(i);
            }

            mRecycler.addScrap(child);
        }

        while (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (child.getBottom() >= clearAbove) {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            if (mInLayout) {
                removeViewsInLayout(0, 1);
            } else {
                removeViewAt(0);
            }

            mRecycler.addScrap(child);
            mFirstPosition++;
        }
    }

    final void offsetChildren(int offset) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            child.layout(child.getLeft() + offset, child.getTop(),
                    child.getRight() + offset, child.getBottom());
        }
    }

    private boolean contentFits() {
        final int childCount = getChildCount();
        if (childCount == 0) return true;
        if (childCount != mItemCount) return false;

        return getChildAt(0).getLeft() >= getPaddingLeft() &&
                getChildAt(childCount - 1).getRight() <= getWidth() - getPaddingRight();
    }

    private void recycleAllViews() {
        for (int i = 0; i < getChildCount(); i++) {
            mRecycler.addScrap(getChildAt(i));
        }

        if (mInLayout) {
            removeAllViewsInLayout();
        } else {
            removeAllViews();
        }
    }

    private int fillRight(int pos, int overhang) {
        int end = (getRight() - getLeft()) + overhang;

        int nextLeft = getChildCount() == 0 ? 0 : getChildAt(getChildCount() - 1).getRight();
        while (nextLeft < end && pos < mItemCount) {
            pos += makeAndAddColumn(pos, nextLeft, true);
            nextLeft = getChildAt(getChildCount() - 1).getRight();
        }
        final int gridRight = getWidth() - getPaddingRight();
        return getChildAt(getChildCount() - 1).getRight() - gridRight;
    }

    private int fillLeft(int pos, int overhang) {
        int end = getPaddingLeft() - overhang;

        int nextRight = getChildAt(0).getLeft();
        while (nextRight > end && pos >= 0) {
            pos -= makeAndAddColumn(pos, nextRight, false);
            nextRight = getChildAt(0).getLeft();
        }

        mFirstPosition = pos + 1;
        return getPaddingLeft() - getChildAt(0).getLeft();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            final int x = mScroller.getCurrX();
            final int dx = (int) (x - mLastTouchX);
            mLastTouchX = x;
            final boolean stopped = !trackMotionScroll(dx, false);

            if (!stopped && !mScroller.isFinished()) {
                ViewCompat.postInvalidateOnAnimation(this);
            } else {
                if (stopped) {
                    final int overScrollMode = ViewCompat.getOverScrollMode(this);
                    if (overScrollMode != ViewCompat.OVER_SCROLL_NEVER) {
                        final EdgeEffectCompat edge;
                        if (dx > 0) {
                            edge = mLeftEdge;
                        } else {
                            edge = mRightEdge;
                        }
                        edge.onAbsorb(Math.abs((int) mScroller.getCurrVelocity()));
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                    mScroller.abortAnimation();
                }
                mTouchMode = TOUCH_MODE_IDLE;
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (!mLeftEdge.isFinished()) {
            final int restoreCount = canvas.save();
            final int height = getHeight() - getPaddingTop() - getPaddingBottom();

            canvas.rotate(270);
            canvas.translate(-height + getPaddingTop(), 0);
            mLeftEdge.setSize(height, getWidth());
            if (mLeftEdge.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
        if (!mRightEdge.isFinished()) {
            final int restoreCount = canvas.save();
            final int width = getWidth();
            final int height = getHeight() - getPaddingTop() - getPaddingBottom();

            canvas.rotate(90);
            canvas.translate(-getPaddingTop(), width);
            mRightEdge.setSize(height, width);
            if (mRightEdge.draw(canvas)) {
                postInvalidateOnAnimation();
            }
            canvas.restoreToCount(restoreCount);
        }
    }

    /**
     * Obtain a populated view from the adapter. If optScrap is non-null and is not
     * reused it will be placed in the recycle bin.
     *
     * @param position position to get view for
     * @param optScrap Optional scrap view; will be reused if possible
     * @return A new view, a recycled view from mRecycler, or optScrap
     */
    private final View obtainView(int position, View optScrap) {
        View view = mRecycler.getTransientStateView(position);
        if (view != null) {
            return view;
        }

        // Reuse optScrap if it's of the right type (and not null)
        final int optType = optScrap != null ?
                ((LayoutParams) optScrap.getLayoutParams()).viewType : -1;
        final int positionViewType = mAdapter.getItemViewType(position);
        final View scrap = optType == positionViewType ?
                optScrap : mRecycler.getScrapView(positionViewType);

        view = mAdapter.getView(position, scrap, this);

        if (view != scrap && scrap != null) {
            // The adapter didn't use it; put it back.
            mRecycler.addScrap(scrap);
        }

        ViewGroup.LayoutParams lp = view.getLayoutParams();

        if (view.getParent() != this) {
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            } else if (!checkLayoutParams(lp)) {
                lp = generateLayoutParams(lp);
            }
            view.setLayoutParams(lp);
        }

        final LayoutParams sglp = (LayoutParams) lp;
        sglp.position = position;
        sglp.viewType = positionViewType;

        return view;
    }

    public GalleryThumbnailAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(GalleryThumbnailAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        // TODO: If the new adapter says that there are stable IDs, remove certain layout records
        // and onscreen views if they have changed instead of removing all of the state here.
        clearAllState();
        mAdapter = adapter;
        mDataChanged = true;
        mOldItemCount = mItemCount = adapter != null ? adapter.getCount() : 0;
        if (adapter != null) {
            adapter.registerDataSetObserver(mObserver);
            mRecycler.setViewTypeCount(adapter.getViewTypeCount());
            mHasStableIds = adapter.hasStableIds();
        } else {
            mHasStableIds = false;
        }
        populate();
    }

    /**
     * Clear all state because the grid will be used for a completely different set of data.
     */
    private void clearAllState() {
        // Clear all layout records and views
        removeAllViews();

        // Reset to the top of the grid
        mFirstPosition = 0;

        // Clear recycler because there could be different view types now
        mRecycler.clear();
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private static final int[] LAYOUT_ATTRS = new int[] {
                android.R.attr.layout_span
        };

        private static final int SPAN_INDEX = 0;

        /**
         * The number of columns this item should span
         */
        public int span = 1;

        /**
         * Item position this view represents
         */
        int position;

        /**
         * Type of this view as reported by the adapter
         */
        int viewType;

        /**
         * The column this view is occupying
         */
        int column;

        /**
         * The stable ID of the item this view displays
         */
        long id = -1;

        public LayoutParams(int height) {
            super(MATCH_PARENT, height);

            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams width to " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams height to MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }

            TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            span = a.getInteger(SPAN_INDEX, 1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with width " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }
    }

    private class RecycleBin {
        private ArrayList<View>[] mScrapViews;
        private int mViewTypeCount;
        private int mMaxScrap;

        private SparseArray<View> mTransientStateViews;

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Must have at least one view type (" +
                        viewTypeCount + " types reported)");
            }
            if (viewTypeCount == mViewTypeCount) {
                return;
            }

            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mScrapViews = scrapViews;
        }

        public void clear() {
            final int typeCount = mViewTypeCount;
            for (int i = 0; i < typeCount; i++) {
                mScrapViews[i].clear();
            }
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        public void clearTransientViews() {
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        public void addScrap(View v) {
            final LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (ViewCompat.hasTransientState(v)) {
                if (mTransientStateViews == null) {
                    mTransientStateViews = new SparseArray<View>();
                }
                mTransientStateViews.put(lp.position, v);
                return;
            }

            final int childCount = getChildCount();
            if (childCount > mMaxScrap) {
                mMaxScrap = childCount;
            }

            ArrayList<View> scrap = mScrapViews[lp.viewType];
            if (scrap.size() < mMaxScrap) {
                scrap.add(v);
            }
        }

        public View getTransientStateView(int position) {
            if (mTransientStateViews == null) {
                return null;
            }

            final View result = mTransientStateViews.get(position);
            if (result != null) {
                mTransientStateViews.remove(position);
            }
            return result;
        }

        public View getScrapView(int type) {
            ArrayList<View> scrap = mScrapViews[type];
            if (scrap.isEmpty()) {
                return null;
            }

            final int index = scrap.size() - 1;
            final View result = scrap.get(index);
            scrap.remove(index);
            return result;
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();

            // TODO: Consider matching these back up if we have stable IDs.
            mRecycler.clearTransientViews();

            if (!mHasStableIds) {
                recycleAllViews();
            }

            // TODO: consider repopulating in a deferred runnable instead
            // (so that successive changes may still be batched)
            requestLayout();
        }

        @Override
        public void onInvalidated() {
        }
    }
}
