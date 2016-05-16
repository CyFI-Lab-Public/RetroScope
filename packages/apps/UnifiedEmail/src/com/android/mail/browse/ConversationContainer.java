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
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.ScrollView;

import com.android.mail.R;
import com.android.mail.browse.ScrollNotifier.ScrollListener;
import com.android.mail.providers.UIProvider;
import com.android.mail.ui.ConversationViewFragment;
import com.android.mail.utils.DequeMap;
import com.android.mail.utils.InputSmoother;
import com.android.mail.utils.LogUtils;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * A specialized ViewGroup container for conversation view. It is designed to contain a single
 * {@link WebView} and a number of overlay views that draw on top of the WebView. In the Mail app,
 * the WebView contains all HTML message bodies in a conversation, and the overlay views are the
 * subject view, message headers, and attachment views. The WebView does all scroll handling, and
 * this container manages scrolling of the overlay views so that they move in tandem.
 *
 * <h5>INPUT HANDLING</h5>
 * Placing the WebView in the same container as the overlay views means we don't have to do a lot of
 * manual manipulation of touch events. We do have a
 * {@link #forwardFakeMotionEvent(MotionEvent, int)} method that deals with one WebView
 * idiosyncrasy: it doesn't react well when touch MOVE events stream in without a preceding DOWN.
 *
 * <h5>VIEW RECYCLING</h5>
 * Normally, it would make sense to put all overlay views into a {@link ListView}. But this view
 * sandwich has unique characteristics: the list items are scrolled based on an external controller,
 * and we happen to know all of the overlay positions up front. So it didn't make sense to shoehorn
 * a ListView in and instead, we rolled our own view recycler by borrowing key details from
 * ListView and AbsListView.
 *
 */
public class ConversationContainer extends ViewGroup implements ScrollListener {
    private static final String TAG = ConversationViewFragment.LAYOUT_TAG;

    private static final int[] BOTTOM_LAYER_VIEW_IDS = {
        R.id.webview,
        R.id.conversation_side_border_overlay
    };

    private static final int[] TOP_LAYER_VIEW_IDS = {
        R.id.conversation_topmost_overlay
    };

    /**
     * Maximum scroll speed (in dp/sec) at which the snap header animation will draw.
     * Anything faster than that, and drawing it creates visual artifacting (wagon-wheel effect).
     */
    private static final float SNAP_HEADER_MAX_SCROLL_SPEED = 600f;

    private ConversationAccountController mAccountController;
    private ConversationViewAdapter mOverlayAdapter;
    private OverlayPosition[] mOverlayPositions;
    private ConversationWebView mWebView;
    private MessageHeaderView mSnapHeader;
    private View mTopMostOverlay;

    /**
     * This is a hack.
     *
     * <p>Without this hack enabled, very fast scrolling can sometimes cause the top-most layers
     * to skip being drawn for a frame or two. It happens specifically when overlay views are
     * attached or added, and WebView happens to draw (on its own) immediately afterwards.
     *
     * <p>The workaround is to force an additional draw of the top-most overlay. Since the problem
     * only occurs when scrolling overlays are added, restrict the additional draw to only occur
     * if scrolling overlays were added since the last draw.
     */
    private boolean mAttachedOverlaySinceLastDraw;

    private final List<View> mNonScrollingChildren = Lists.newArrayList();

    /**
     * Current document zoom scale per {@link WebView#getScale()}. This is the ratio of actual
     * screen pixels to logical WebView HTML pixels. We use it to convert from one to the other.
     */
    private float mScale;
    /**
     * Set to true upon receiving the first touch event. Used to help reject invalid WebView scale
     * values.
     */
    private boolean mTouchInitialized;

    /**
     * System touch-slop distance per {@link ViewConfiguration#getScaledTouchSlop()}.
     */
    private final int mTouchSlop;
    /**
     * Current scroll position, as dictated by the background {@link WebView}.
     */
    private int mOffsetY;
    /**
     * Original pointer Y for slop calculation.
     */
    private float mLastMotionY;
    /**
     * Original pointer ID for slop calculation.
     */
    private int mActivePointerId;
    /**
     * Track pointer up/down state to know whether to send a make-up DOWN event to WebView.
     * WebView internal logic requires that a stream of {@link MotionEvent#ACTION_MOVE} events be
     * preceded by a {@link MotionEvent#ACTION_DOWN} event.
     */
    private boolean mTouchIsDown = false;
    /**
     * Remember if touch interception was triggered on a {@link MotionEvent#ACTION_POINTER_DOWN},
     * so we can send a make-up event in {@link #onTouchEvent(MotionEvent)}.
     */
    private boolean mMissedPointerDown;

    /**
     * A recycler that holds removed scrap views, organized by integer item view type. All views
     * in this data structure should be removed from their view parent prior to insertion.
     */
    private final DequeMap<Integer, View> mScrapViews = new DequeMap<Integer, View>();

    /**
     * The current set of overlay views in the view hierarchy. Looking through this map is faster
     * than traversing the view hierarchy.
     * <p>
     * WebView sometimes notifies of scroll changes during a draw (or display list generation), when
     * it's not safe to detach view children because ViewGroup is in the middle of iterating over
     * its child array. So we remove any child from this list immediately and queue up a task to
     * detach it later. Since nobody other than the detach task references that view in the
     * meantime, we don't need any further checks or synchronization.
     * <p>
     * We keep {@link OverlayView} wrappers instead of bare views so that when it's time to dispose
     * of all views (on data set or adapter change), we can at least recycle them into the typed
     * scrap piles for later reuse.
     */
    private final SparseArray<OverlayView> mOverlayViews;

    private int mWidthMeasureSpec;

    private boolean mDisableLayoutTracing;

    private final InputSmoother mVelocityTracker;

    private final DataSetObserver mAdapterObserver = new AdapterObserver();

    /**
     * The adapter index of the lowest overlay item that is above the top of the screen and reports
     * {@link ConversationOverlayItem#canPushSnapHeader()}. We calculate this after a pass through
     * {@link #positionOverlays(int, int)}.
     *
     */
    private int mSnapIndex;

    private boolean mSnapEnabled;

    /**
     * A View that fills the remaining vertical space when the overlays do not take
     * up the entire container. Otherwise, a card-like bottom white space appears.
     */
    private View mAdditionalBottomBorder;

    /**
     * A flag denoting whether the fake bottom border has been added to the container.
     */
    private boolean mAdditionalBottomBorderAdded;

    /**
     * An int containing the potential top value for the additional bottom border.
     * If this value is less than the height of the scroll container, the additional
     * bottom border will be drawn.
     */
    private int mAdditionalBottomBorderOverlayTop;

    /**
     * Child views of this container should implement this interface to be notified when they are
     * being detached.
     *
     */
    public interface DetachListener {
        /**
         * Called on a child view when it is removed from its parent as part of
         * {@link ConversationContainer} view recycling.
         */
        void onDetachedFromParent();
    }

    public static class OverlayPosition {
        public final int top;
        public final int bottom;

        public OverlayPosition(int top, int bottom) {
            this.top = top;
            this.bottom = bottom;
        }
    }

    private static class OverlayView {
        public View view;
        int itemType;

        public OverlayView(View view, int itemType) {
            this.view = view;
            this.itemType = itemType;
        }
    }

    public ConversationContainer(Context c) {
        this(c, null);
    }

    public ConversationContainer(Context c, AttributeSet attrs) {
        super(c, attrs);

        mOverlayViews = new SparseArray<OverlayView>();

        mVelocityTracker = new InputSmoother(c);

        mTouchSlop = ViewConfiguration.get(c).getScaledTouchSlop();

        // Disabling event splitting fixes pinch-zoom when the first pointer goes down on the
        // WebView and the second pointer goes down on an overlay view.
        // Intercepting ACTION_POINTER_DOWN events allows pinch-zoom to work when the first pointer
        // goes down on an overlay view.
        setMotionEventSplittingEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mWebView = (ConversationWebView) findViewById(R.id.webview);
        mWebView.addScrollListener(this);

        mTopMostOverlay = findViewById(R.id.conversation_topmost_overlay);

        mSnapHeader = (MessageHeaderView) findViewById(R.id.snap_header);
        mSnapHeader.setSnappy(true);

        for (int id : BOTTOM_LAYER_VIEW_IDS) {
            mNonScrollingChildren.add(findViewById(id));
        }
        for (int id : TOP_LAYER_VIEW_IDS) {
            mNonScrollingChildren.add(findViewById(id));
        }
    }

    public MessageHeaderView getSnapHeader() {
        return mSnapHeader;
    }

    public void setOverlayAdapter(ConversationViewAdapter a) {
        if (mOverlayAdapter != null) {
            mOverlayAdapter.unregisterDataSetObserver(mAdapterObserver);
            clearOverlays();
        }
        mOverlayAdapter = a;
        if (mOverlayAdapter != null) {
            mOverlayAdapter.registerDataSetObserver(mAdapterObserver);
        }
    }

    public Adapter getOverlayAdapter() {
        return mOverlayAdapter;
    }

    public void setAccountController(ConversationAccountController controller) {
        mAccountController = controller;

        mSnapEnabled = isSnapEnabled();
    }

    /**
     * Re-bind any existing views that correspond to the given adapter positions.
     *
     */
    public void onOverlayModelUpdate(List<Integer> affectedAdapterPositions) {
        for (Integer i : affectedAdapterPositions) {
            final ConversationOverlayItem item = mOverlayAdapter.getItem(i);
            final OverlayView overlay = mOverlayViews.get(i);
            if (overlay != null && overlay.view != null && item != null) {
                item.onModelUpdated(overlay.view);
            }
            // update the snap header too, but only it's showing if the current item
            if (i == mSnapIndex && mSnapHeader.isBoundTo(item)) {
                mSnapHeader.refresh();
            }
        }
    }

    /**
     * Return an overlay view for the given adapter item, or null if no matching view is currently
     * visible. This can happen as you scroll away from an overlay view.
     *
     */
    public View getViewForItem(ConversationOverlayItem item) {
        View result = null;
        int adapterPos = -1;
        for (int i = 0, len = mOverlayAdapter.getCount(); i < len; i++) {
            if (mOverlayAdapter.getItem(i) == item) {
                adapterPos = i;
                break;
            }
        }
        if (adapterPos != -1) {
            final OverlayView overlay = mOverlayViews.get(adapterPos);
            if (overlay != null) {
                result = overlay.view;
            }
        }
        return result;
    }

    private void clearOverlays() {
        for (int i = 0, len = mOverlayViews.size(); i < len; i++) {
            detachOverlay(mOverlayViews.valueAt(i));
        }
        mOverlayViews.clear();
    }

    private void onDataSetChanged() {
        // Recycle all views and re-bind them according to the current set of spacer coordinates.
        // This essentially resets the overlay views and re-renders them.
        // It's fast enough that it's okay to re-do all views on any small change, as long as
        // the change isn't too frequent (< ~1Hz).

        clearOverlays();
        // also unbind the snap header view, so this "reset" causes the snap header to re-create
        // its view, just like all other headers
        mSnapHeader.unbind();

        // also clear out the additional bottom border
        removeViewInLayout(mAdditionalBottomBorder);
        mAdditionalBottomBorderAdded = false;

        mSnapEnabled = isSnapEnabled();
        positionOverlays(0, mOffsetY);
    }

    private void forwardFakeMotionEvent(MotionEvent original, int newAction) {
        MotionEvent newEvent = MotionEvent.obtain(original);
        newEvent.setAction(newAction);
        mWebView.onTouchEvent(newEvent);
        LogUtils.v(TAG, "in Container.OnTouch. fake: action=%d x/y=%f/%f pointers=%d",
                newEvent.getActionMasked(), newEvent.getX(), newEvent.getY(),
                newEvent.getPointerCount());
    }

    /**
     * Touch slop code was copied from {@link ScrollView#onInterceptTouchEvent(MotionEvent)}.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (!mTouchInitialized) {
            mTouchInitialized = true;
        }

        // no interception when WebView handles the first DOWN
        if (mWebView.isHandlingTouch()) {
            return false;
        }

        boolean intercept = false;
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                LogUtils.d(TAG, "Container is intercepting non-primary touch!");
                intercept = true;
                mMissedPointerDown = true;
                requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_DOWN:
                mLastMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float y = ev.getY(pointerIndex);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop) {
                    mLastMotionY = y;
                    intercept = true;
                }
                break;
        }

//        LogUtils.v(TAG, "in Container.InterceptTouch. action=%d x/y=%f/%f pointers=%d result=%s",
//                ev.getActionMasked(), ev.getX(), ev.getY(), ev.getPointerCount(), intercept);
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mTouchIsDown = false;
        } else if (!mTouchIsDown &&
                (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN)) {

            forwardFakeMotionEvent(ev, MotionEvent.ACTION_DOWN);
            if (mMissedPointerDown) {
                forwardFakeMotionEvent(ev, MotionEvent.ACTION_POINTER_DOWN);
                mMissedPointerDown = false;
            }

            mTouchIsDown = true;
        }

        final boolean webViewResult = mWebView.onTouchEvent(ev);

//        LogUtils.v(TAG, "in Container.OnTouch. action=%d x/y=%f/%f pointers=%d",
//                ev.getActionMasked(), ev.getX(), ev.getY(), ev.getPointerCount());
        return webViewResult;
    }

    @Override
    public void onNotifierScroll(final int x, final int y) {
        mVelocityTracker.onInput(y);
        mDisableLayoutTracing = true;
        positionOverlays(x, y);
        mDisableLayoutTracing = false;
    }

    private void positionOverlays(int x, int y) {
        mOffsetY = y;

        /*
         * The scale value that WebView reports is inaccurate when measured during WebView
         * initialization. This bug is present in ICS, so to work around it, we ignore all
         * reported values and use a calculated expected value from ConversationWebView instead.
         * Only when the user actually begins to touch the view (to, say, begin a zoom) do we begin
         * to pay attention to WebView-reported scale values.
         */
        if (mTouchInitialized) {
            mScale = mWebView.getScale();
        } else if (mScale == 0) {
            mScale = mWebView.getInitialScale();
        }
        traceLayout("in positionOverlays, raw scale=%f, effective scale=%f", mWebView.getScale(),
                mScale);

        if (mOverlayPositions == null || mOverlayAdapter == null) {
            return;
        }

        // recycle scrolled-off views and add newly visible views

        // we want consecutive spacers/overlays to stack towards the bottom
        // so iterate from the bottom of the conversation up
        // starting with the last spacer bottom and the last adapter item, position adapter views
        // in a single stack until you encounter a non-contiguous expanded message header,
        // then decrement to the next spacer.

        traceLayout("IN positionOverlays, spacerCount=%d overlayCount=%d", mOverlayPositions.length,
                mOverlayAdapter.getCount());

        mSnapIndex = -1;
        mAdditionalBottomBorderOverlayTop = 0;

        int adapterLoopIndex = mOverlayAdapter.getCount() - 1;
        int spacerIndex = mOverlayPositions.length - 1;
        while (spacerIndex >= 0 && adapterLoopIndex >= 0) {

            final int spacerTop = getOverlayTop(spacerIndex);
            final int spacerBottom = getOverlayBottom(spacerIndex);

            final boolean flip;
            final int flipOffset;
            final int forceGravity;
            // flip direction from bottom->top to top->bottom traversal on the very first spacer
            // to facilitate top-aligned headers at spacer index = 0
            if (spacerIndex == 0) {
                flip = true;
                flipOffset = adapterLoopIndex;
                forceGravity = Gravity.TOP;
            } else {
                flip = false;
                flipOffset = 0;
                forceGravity = Gravity.NO_GRAVITY;
            }

            int adapterIndex = flip ? flipOffset - adapterLoopIndex : adapterLoopIndex;

            // always place at least one overlay per spacer
            ConversationOverlayItem adapterItem = mOverlayAdapter.getItem(adapterIndex);

            OverlayPosition itemPos = calculatePosition(adapterItem, spacerTop, spacerBottom,
                    forceGravity);

            traceLayout("in loop, spacer=%d overlay=%d t/b=%d/%d (%s)", spacerIndex, adapterIndex,
                    itemPos.top, itemPos.bottom, adapterItem);
            positionOverlay(adapterIndex, itemPos.top, itemPos.bottom);

            // and keep stacking overlays unconditionally if we are on the first spacer, or as long
            // as overlays are contiguous
            while (--adapterLoopIndex >= 0) {
                adapterIndex = flip ? flipOffset - adapterLoopIndex : adapterLoopIndex;
                adapterItem = mOverlayAdapter.getItem(adapterIndex);
                if (spacerIndex > 0 && !adapterItem.isContiguous()) {
                    // advance to the next spacer, but stay on this adapter item
                    break;
                }

                // place this overlay in the region of the spacer above or below the last item,
                // depending on direction of iteration
                final int regionTop = flip ? itemPos.bottom : spacerTop;
                final int regionBottom = flip ? spacerBottom : itemPos.top;
                itemPos = calculatePosition(adapterItem, regionTop, regionBottom, forceGravity);

                traceLayout("in contig loop, spacer=%d overlay=%d t/b=%d/%d (%s)", spacerIndex,
                        adapterIndex, itemPos.top, itemPos.bottom, adapterItem);
                positionOverlay(adapterIndex, itemPos.top, itemPos.bottom);
            }

            spacerIndex--;
        }

        positionSnapHeader(mSnapIndex);
        positionAdditionalBottomBorder();
    }

    /**
     * Adds an additional bottom border to the overlay views in case
     * the overlays do not fill the entire screen.
     */
    private void positionAdditionalBottomBorder() {
        final int lastBottom = mAdditionalBottomBorderOverlayTop;
        final int containerHeight = webPxToScreenPx(mWebView.getContentHeight());
        final int speculativeHeight = containerHeight - lastBottom;
        if (speculativeHeight > 0) {
            if (mAdditionalBottomBorder == null) {
                mAdditionalBottomBorder = mOverlayAdapter.getLayoutInflater().inflate(
                        R.layout.fake_bottom_border, this, false);
            }

            setAdditionalBottomBorderHeight(speculativeHeight);

            if (!mAdditionalBottomBorderAdded) {
                addViewInLayoutWrapper(mAdditionalBottomBorder);
                mAdditionalBottomBorderAdded = true;
            }

            measureOverlayView(mAdditionalBottomBorder);
            layoutOverlay(mAdditionalBottomBorder, lastBottom, containerHeight);
        } else {
            if (mAdditionalBottomBorder != null && mAdditionalBottomBorderAdded) {
                removeViewInLayout(mAdditionalBottomBorder);
                mAdditionalBottomBorderAdded = false;
            }
        }
    }

    private void setAdditionalBottomBorderHeight(int speculativeHeight) {
        LayoutParams params = mAdditionalBottomBorder.getLayoutParams();
        params.height = speculativeHeight;
        mAdditionalBottomBorder.setLayoutParams(params);
    }

    private static OverlayPosition calculatePosition(final ConversationOverlayItem adapterItem,
            final int withinTop, final int withinBottom, final int forceGravity) {
        if (adapterItem.getHeight() == 0) {
            // "place" invisible items at the bottom of their region to stay consistent with the
            // stacking algorithm in positionOverlays(), unless gravity is forced to the top
            final int y = (forceGravity == Gravity.TOP) ? withinTop : withinBottom;
            return new OverlayPosition(y, y);
        }

        final int v = ((forceGravity != Gravity.NO_GRAVITY) ?
                forceGravity : adapterItem.getGravity()) & Gravity.VERTICAL_GRAVITY_MASK;
        switch (v) {
            case Gravity.BOTTOM:
                return new OverlayPosition(withinBottom - adapterItem.getHeight(), withinBottom);
            case Gravity.TOP:
                return new OverlayPosition(withinTop, withinTop + adapterItem.getHeight());
            default:
                throw new UnsupportedOperationException("unsupported gravity: " + v);
        }
    }

    /**
     * Executes a measure pass over the specified child overlay view and returns the measured
     * height. The measurement uses whatever the current container's width measure spec is.
     * This method ignores view visibility and returns the height that the view would be if visible.
     *
     * @param overlayView an overlay view to measure. does not actually have to be attached yet.
     * @return height that the view would be if it was visible
     */
    public int measureOverlay(View overlayView) {
        measureOverlayView(overlayView);
        return overlayView.getMeasuredHeight();
    }

    /**
     * Copied/stolen from {@link ListView}.
     */
    private void measureOverlayView(View child) {
        MarginLayoutParams p = (MarginLayoutParams) child.getLayoutParams();
        if (p == null) {
            p = (MarginLayoutParams) generateDefaultLayoutParams();
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(mWidthMeasureSpec,
                getPaddingLeft() + getPaddingRight() + p.leftMargin + p.rightMargin, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    private void onOverlayScrolledOff(final int adapterIndex, final OverlayView overlay,
            int overlayTop, int overlayBottom) {
        // detach the view asynchronously, as scroll notification can happen during a draw, when
        // it's not safe to remove children

        // but immediately remove this view from the view set so future lookups don't find it
        mOverlayViews.remove(adapterIndex);

        post(new Runnable() {
            @Override
            public void run() {
                detachOverlay(overlay);
            }
        });

        // push it out of view immediately
        // otherwise this scrolled-off header will continue to draw until the runnable runs
        layoutOverlay(overlay.view, overlayTop, overlayBottom);
    }

    /**
     * Returns an existing scrap view, if available. The view will already be removed from the view
     * hierarchy. This method will not remove the view from the scrap heap.
     *
     */
    public View getScrapView(int type) {
        return mScrapViews.peek(type);
    }

    public void addScrapView(int type, View v) {
        mScrapViews.add(type, v);
    }

    private void detachOverlay(OverlayView overlay) {
        // Prefer removeViewInLayout over removeView. The typical followup layout pass is unneeded
        // because removing overlay views doesn't affect overall layout.
        removeViewInLayout(overlay.view);
        mScrapViews.add(overlay.itemType, overlay.view);
        if (overlay.view instanceof DetachListener) {
            ((DetachListener) overlay.view).onDetachedFromParent();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (LogUtils.isLoggable(TAG, LogUtils.DEBUG)) {
            LogUtils.d(TAG, "*** IN header container onMeasure spec for w/h=%s/%s",
                    MeasureSpec.toString(widthMeasureSpec),
                    MeasureSpec.toString(heightMeasureSpec));
        }

        for (View nonScrollingChild : mNonScrollingChildren) {
            if (nonScrollingChild.getVisibility() != GONE) {
                measureChildWithMargins(nonScrollingChild, widthMeasureSpec, 0 /* widthUsed */,
                        heightMeasureSpec, 0 /* heightUsed */);
            }
        }
        mWidthMeasureSpec = widthMeasureSpec;

        // onLayout will re-measure and re-position overlays for the new container size, but the
        // spacer offsets would still need to be updated to have them draw at their new locations.
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        LogUtils.d(TAG, "*** IN header container onLayout");

        for (View nonScrollingChild : mNonScrollingChildren) {
            if (nonScrollingChild.getVisibility() != GONE) {
                final int w = nonScrollingChild.getMeasuredWidth();
                final int h = nonScrollingChild.getMeasuredHeight();

                final MarginLayoutParams lp =
                        (MarginLayoutParams) nonScrollingChild.getLayoutParams();

                final int childLeft = lp.leftMargin;
                final int childTop = lp.topMargin;
                nonScrollingChild.layout(childLeft, childTop, childLeft + w, childTop + h);
            }
        }

        if (mOverlayAdapter != null) {
            // being in a layout pass means overlay children may require measurement,
            // so invalidate them
            for (int i = 0, len = mOverlayAdapter.getCount(); i < len; i++) {
                mOverlayAdapter.getItem(i).invalidateMeasurement();
            }
        }

        positionOverlays(0, mOffsetY);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mAttachedOverlaySinceLastDraw) {
            drawChild(canvas, mTopMostOverlay, getDrawingTime());
            mAttachedOverlaySinceLastDraw = false;
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams;
    }

    private int getOverlayTop(int spacerIndex) {
        return webPxToScreenPx(mOverlayPositions[spacerIndex].top);
    }

    private int getOverlayBottom(int spacerIndex) {
        return webPxToScreenPx(mOverlayPositions[spacerIndex].bottom);
    }

    private int webPxToScreenPx(int webPx) {
        // TODO: round or truncate?
        // TODO: refactor and unify with ConversationWebView.webPxToScreenPx()
        return (int) (webPx * mScale);
    }

    private void positionOverlay(int adapterIndex, int overlayTopY, int overlayBottomY) {
        final OverlayView overlay = mOverlayViews.get(adapterIndex);
        final ConversationOverlayItem item = mOverlayAdapter.getItem(adapterIndex);

        // save off the item's current top for later snap calculations
        item.setTop(overlayTopY);

        // is the overlay visible and does it have non-zero height?
        if (overlayTopY != overlayBottomY && overlayBottomY > mOffsetY
                && overlayTopY < mOffsetY + getHeight()) {
            View overlayView = overlay != null ? overlay.view : null;
            // show and/or move overlay
            if (overlayView == null) {
                overlayView = addOverlayView(adapterIndex);
                measureOverlayView(overlayView);
                item.markMeasurementValid();
                traceLayout("show/measure overlay %d", adapterIndex);
            } else {
                traceLayout("move overlay %d", adapterIndex);
                if (!item.isMeasurementValid()) {
                    item.rebindView(overlayView);
                    measureOverlayView(overlayView);
                    item.markMeasurementValid();
                    traceLayout("and (re)measure overlay %d, old/new heights=%d/%d", adapterIndex,
                            overlayView.getHeight(), overlayView.getMeasuredHeight());
                }
            }
            traceLayout("laying out overlay %d with h=%d", adapterIndex,
                    overlayView.getMeasuredHeight());
            final int childBottom = overlayTopY + overlayView.getMeasuredHeight();
            layoutOverlay(overlayView, overlayTopY, childBottom);
            mAdditionalBottomBorderOverlayTop = (childBottom > mAdditionalBottomBorderOverlayTop) ?
                    childBottom : mAdditionalBottomBorderOverlayTop;
        } else {
            // hide overlay
            if (overlay != null) {
                traceLayout("hide overlay %d", adapterIndex);
                onOverlayScrolledOff(adapterIndex, overlay, overlayTopY, overlayBottomY);
            } else {
                traceLayout("ignore non-visible overlay %d", adapterIndex);
            }
            mAdditionalBottomBorderOverlayTop = (overlayBottomY > mAdditionalBottomBorderOverlayTop)
                    ? overlayBottomY : mAdditionalBottomBorderOverlayTop;
        }

        if (overlayTopY <= mOffsetY && item.canPushSnapHeader()) {
            if (mSnapIndex == -1) {
                mSnapIndex = adapterIndex;
            } else if (adapterIndex > mSnapIndex) {
                mSnapIndex = adapterIndex;
            }
        }

    }

    // layout an existing view
    // need its top offset into the conversation, its height, and the scroll offset
    private void layoutOverlay(View child, int childTop, int childBottom) {
        final int top = childTop - mOffsetY;
        final int bottom = childBottom - mOffsetY;

        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        final int childLeft = getPaddingLeft() + lp.leftMargin;

        child.layout(childLeft, top, childLeft + child.getMeasuredWidth(), bottom);
    }

    private View addOverlayView(int adapterIndex) {
        final int itemType = mOverlayAdapter.getItemViewType(adapterIndex);
        final View convertView = mScrapViews.poll(itemType);

        final View view = mOverlayAdapter.getView(adapterIndex, convertView, this);
        mOverlayViews.put(adapterIndex, new OverlayView(view, itemType));

        if (convertView == view) {
            LogUtils.d(TAG, "want to REUSE scrolled-in view: index=%d obj=%s", adapterIndex, view);
        } else {
            LogUtils.d(TAG, "want to CREATE scrolled-in view: index=%d obj=%s", adapterIndex, view);
        }

        addViewInLayoutWrapper(view);

        return view;
    }

    private void addViewInLayoutWrapper(View view) {
        final int index = BOTTOM_LAYER_VIEW_IDS.length;
        addViewInLayout(view, index, view.getLayoutParams(), true /* preventRequestLayout */);
        mAttachedOverlaySinceLastDraw = true;
    }

    private boolean isSnapEnabled() {
        if (mAccountController == null || mAccountController.getAccount() == null
                || mAccountController.getAccount().settings == null) {
            return true;
        }
        final int snap = mAccountController.getAccount().settings.snapHeaders;
        return snap == UIProvider.SnapHeaderValue.ALWAYS ||
                (snap == UIProvider.SnapHeaderValue.PORTRAIT_ONLY && getResources()
                    .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    // render and/or re-position snap header
    private void positionSnapHeader(int snapIndex) {
        ConversationOverlayItem snapItem = null;
        if (mSnapEnabled && snapIndex != -1) {
            final ConversationOverlayItem item = mOverlayAdapter.getItem(snapIndex);
            if (item.canBecomeSnapHeader()) {
                snapItem = item;
            }
        }
        if (snapItem == null) {
            mSnapHeader.setVisibility(GONE);
            mSnapHeader.unbind();
            return;
        }

        snapItem.bindView(mSnapHeader, false /* measureOnly */);
        mSnapHeader.setVisibility(VISIBLE);

        // overlap is negative or zero; bump the snap header upwards by that much
        int overlap = 0;

        final ConversationOverlayItem next = findNextPushingOverlay(snapIndex + 1);
        if (next != null) {
            overlap = Math.min(0, next.getTop() - mSnapHeader.getHeight() - mOffsetY);

            // disable overlap drawing past a certain speed
            if (overlap < 0) {
                final Float v = mVelocityTracker.getSmoothedVelocity();
                if (v != null && v > SNAP_HEADER_MAX_SCROLL_SPEED) {
                    overlap = 0;
                }
            }
        }

        mSnapHeader.setTranslationY(overlap);
    }

    // find the next header that can push the snap header up
    private ConversationOverlayItem findNextPushingOverlay(int start) {
        for (int i = start, len = mOverlayAdapter.getCount(); i < len; i++) {
            final ConversationOverlayItem next = mOverlayAdapter.getItem(i);
            if (next.canPushSnapHeader()) {
                return next;
            }
        }
        return null;
    }

    /**
     * Return a collection of all currently visible overlay views, in no particular order.
     * Please don't mess with them too badly (e.g. remove from parent).
     *
     */
    public List<View> getOverlayViews() {
        final List<View> views = Lists.newArrayList();
        for (int i = 0, len = mOverlayViews.size(); i < len; i++) {
            views.add(mOverlayViews.valueAt(i).view);
        }
        return views;
    }

    /**
     * Prevents any layouts from happening until the next time
     * {@link #onGeometryChange(OverlayPosition[])} is
     * called. Useful when you know the HTML spacer coordinates are inconsistent with adapter items.
     * <p>
     * If you call this, you must ensure that a followup call to
     * {@link #onGeometryChange(OverlayPosition[])}
     * is made later, when the HTML spacer coordinates are updated.
     *
     */
    public void invalidateSpacerGeometry() {
        mOverlayPositions = null;
    }

    public void onGeometryChange(OverlayPosition[] overlayPositions) {
        traceLayout("*** got overlay spacer positions:");
        for (OverlayPosition pos : overlayPositions) {
            traceLayout("top=%d bottom=%d", pos.top, pos.bottom);
        }

        mOverlayPositions = overlayPositions;
        positionOverlays(0, mOffsetY);
    }

    private void traceLayout(String msg, Object... params) {
        if (mDisableLayoutTracing) {
            return;
        }
        LogUtils.d(TAG, msg, params);
    }

    private class AdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            onDataSetChanged();
        }
    }
}
