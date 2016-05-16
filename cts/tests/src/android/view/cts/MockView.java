/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.view.cts;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ContextMenu.ContextMenuInfo;

public class MockView extends View {
    private boolean mCalledOnCreateContextMenu = false;
    private boolean mCalledOnAnimationStart = false;
    private boolean mCalledOnAnimationEnd = false;
    private boolean mCalledOnAttachedToWindow = false;
    private boolean mCalledOnDetachedFromWindow = false;
    private boolean mCalledOnCreateDrawableState = false;
    private boolean mCalledDrawableStateChanged = false;
    private boolean mCalledOnDraw = false;
    private boolean mCalledDispatchDraw = false;
    private boolean mCalledOnFinishInflate = false;
    private boolean mCalledOnFocusChanged = false;
    private boolean mCalledOnKeyDown = false;
    private boolean mCalledOnKeyUp = false;
    private boolean mCalledOnKeyMultiple = false;
    private boolean mCalledOnKeyShortcut = false;
    private boolean mCalledOnLayout = false;
    private boolean mCalledOnMeasure = false;
    private boolean mCalledOnSizeChanged = false;
    private boolean mCalledOnSetAlpha = false;
    private boolean mCalledOnTouchEvent = false;
    private boolean mCalledOnTrackballEvent = false;
    private boolean mCalledOnWindowFocusChanged = false;
    private boolean mCalledDispatchRestoreInstanceState = false;
    private boolean mCalledDispatchSaveInstanceState = false;
    private boolean mCalledOnRestoreInstanceState = false;
    private boolean mCalledOnSaveInstanceState = false;
    private boolean mCalledOnWindowVisibilityChanged = false;
    private boolean mCalledDispatchUnhandledMove = false;
    private boolean mCalledDispatchWindowFocusChanged = false;
    private boolean mCalledDispatchWindowVisibilityChanged =false;
    private boolean mCalledOnScrollChanged = false;
    private boolean mCalledInvalidate = false;
    private boolean mCalledComputeScroll = false;
    private boolean mCalledDispatchKeyEventPreIme = false;
    private boolean mCalledOnKeyPreIme = false;

    private int mOldWidth = -1;
    private int mOldHeight = -1;

    public MockView(Context context) {
        super(context);
    }

    public MockView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public MockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        mCalledOnKeyShortcut = true;
        return super.onKeyShortcut(keyCode, event);
    }

    public boolean hasCalledOnKeyShortcut() {
        return mCalledOnKeyShortcut;
    }

    @Override
    public void invalidate() {
        super.invalidate();
        mCalledInvalidate = true;
    }

    public boolean hasCalledInvalidate() {
        return mCalledInvalidate;
    }

    public void setParent(ViewParent parent) {
        mParent = parent;
    }

    public static int[] getEnabledStateSet() {
        return ENABLED_STATE_SET;
    }

    public static int[] getPressedEnabledStateSet() {
        return PRESSED_ENABLED_STATE_SET;
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return super.isPaddingOffsetRequired();
    }

    @Override
    protected int getBottomPaddingOffset() {
        return super.getBottomPaddingOffset();
    }

    @Override
    protected int getLeftPaddingOffset() {
        return super.getLeftPaddingOffset();
    }

    @Override
    protected int getRightPaddingOffset() {
        return super.getRightPaddingOffset();
    }

    @Override
    protected int getTopPaddingOffset() {
        return super.getTopPaddingOffset();
    }

    @Override
    protected void onAnimationEnd() {
        super.onAnimationEnd();
        mCalledOnAnimationEnd = true;
    }

    @Override
    protected void onAnimationStart() {
        super.onAnimationStart();
        mCalledOnAnimationStart = true;
    }

    public boolean hasCalledOnAnimationStart() {
        return mCalledOnAnimationStart;
    }

    public boolean hasCalledOnAnimationEnd() {
        return mCalledOnAnimationEnd;
    }

    @Override
    protected void initializeFadingEdge(TypedArray a) {
        super.initializeFadingEdge(a);
    }

    @Override
    protected void initializeScrollbars(TypedArray a) {
        super.initializeScrollbars(a);
    }

    @Override
    protected int getHorizontalScrollbarHeight() {
        return super.getHorizontalScrollbarHeight();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        mCalledOnKeyDown = true;
        return super.onKeyDown(keyCode, event);
    }

    public boolean hasCalledOnKeyDown() {
        return mCalledOnKeyDown;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        mCalledOnKeyMultiple = true;
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    public boolean hasCalledOnKeyMultiple() {
        return mCalledOnKeyMultiple;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        mCalledOnKeyUp = true;
        return super.onKeyUp(keyCode, event);
    }

    public boolean hasCalledOnKeyUp() {
        return mCalledOnKeyUp;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCalledOnTouchEvent = true;
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        mCalledOnTrackballEvent = true;
        return super.onTrackballEvent(event);
    }

    public boolean hasCalledOnTouchEvent() {
        return mCalledOnTouchEvent;
    }

    public boolean hasCalledOnTrackballEvent() {
        return mCalledOnTrackballEvent;
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return super.getSuggestedMinimumHeight();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return super.getSuggestedMinimumWidth();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who);
    }

    @Override
    protected int computeHorizontalScrollExtent() {
        return super.computeHorizontalScrollExtent();
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return super.computeHorizontalScrollOffset();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        return super.getLeftFadingEdgeStrength();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return super.getRightFadingEdgeStrength();
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return super.getBottomFadingEdgeStrength();
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return super.getTopFadingEdgeStrength();
    }

    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return super.getContextMenuInfo();
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        mCalledOnCreateContextMenu = true;
    }

    public boolean hasCalledOnCreateContextMenu() {
        return mCalledOnCreateContextMenu;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        mCalledOnScrollChanged = true;
    }

    public boolean hasCalledOnScrollChanged() {
        return mCalledOnScrollChanged;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCalledOnSizeChanged = true;
        mOldWidth = oldw;
        mOldHeight = oldh;
    }

    public int getOldWOnSizeChanged() {
        return mOldWidth;
    }

    public int getOldHOnSizeChanged() {
        return mOldHeight;
    }

    public boolean hasCalledOnSizeChanged() {
        return mCalledOnSizeChanged;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mCalledOnLayout = true;
    }

    public boolean hasCalledOnLayout() {
        return mCalledOnLayout;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        mCalledComputeScroll = true;
    }

    public boolean hasCalledComputeScroll() {
        return mCalledComputeScroll;
    }

    @Override
    protected void dispatchSetSelected(boolean selected) {
        super.dispatchSetSelected(selected);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        super.dispatchSetPressed(pressed);
    }

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        super.dispatchWindowFocusChanged(hasFocus);
        mCalledDispatchWindowFocusChanged = true;
    }

    public boolean hasCalledDispatchWindowFocusChanged() {
        return mCalledDispatchWindowFocusChanged;
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        return super.fitSystemWindows(insets);
    }

    public void setMeasuredDimensionWrapper(int measuredWidth, int measuredHeight) {
        super.setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    public Handler getHandler() {
        return super.getHandler();
    }

    @Override
    protected int getWindowAttachCount() {
        return super.getWindowAttachCount();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        mCalledDispatchDraw = true;
    }

    public boolean hasCalledDispatchDraw() {
        return mCalledDispatchDraw;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        mCalledDispatchUnhandledMove = true;
        return super.dispatchUnhandledMove(focused, direction);
    }

    public boolean hasCalledDispatchUnhandledMove() {
        return mCalledDispatchUnhandledMove;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mCalledOnWindowVisibilityChanged = true;
    }

    public boolean hasCalledOnWindowVisibilityChanged() {
        return mCalledOnWindowVisibilityChanged;
    }

    @Override
    public void dispatchWindowVisibilityChanged(int visibility) {
        super.dispatchWindowVisibilityChanged(visibility);
        mCalledDispatchWindowVisibilityChanged = true;
    }

    public boolean hasCalledDispatchWindowVisibilityChanged() {
        return mCalledDispatchWindowVisibilityChanged;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        mCalledOnWindowFocusChanged = true;
        super.onWindowFocusChanged(hasWindowFocus);
    }

    public boolean hasCalledOnWindowFocusChanged() {
        return mCalledOnWindowFocusChanged;
    }

    protected int[] mergeDrawableStatesWrapper(int[] baseState, int[] additionalState) {
        return super.mergeDrawableStates(baseState, additionalState);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCalledOnAttachedToWindow = true;
    }

    public boolean hasCalledOnAttachedToWindow() {
        return mCalledOnAttachedToWindow;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCalledOnDetachedFromWindow = true;
    }

    public boolean hasCalledOnDetachedFromWindow() {
        return mCalledOnDetachedFromWindow;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCalledOnDraw = true;
    }

    public boolean hasCalledOnDraw() {
        return mCalledOnDraw;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCalledOnFinishInflate = true;
    }

    public boolean hasCalledOnFinishInflate() {
        return mCalledOnFinishInflate;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mCalledOnMeasure = true;
    }

    public boolean hasCalledOnMeasure() {
        return mCalledOnMeasure;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        mCalledOnSaveInstanceState = true;
        return super.onSaveInstanceState();
    }

    public boolean hasCalledOnSaveInstanceState() {
        return mCalledOnSaveInstanceState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        mCalledOnRestoreInstanceState = true;
    }

    public boolean hasCalledOnRestoreInstanceState() {
        return mCalledOnRestoreInstanceState;
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        mCalledOnSetAlpha = true;
        return super.onSetAlpha(alpha);
    }

    public boolean hasCalledOnSetAlpha() {
        return mCalledOnSetAlpha;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        mCalledOnFocusChanged = true;
    }

    public boolean hasCalledOnFocusChanged() {
        return mCalledOnFocusChanged;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        mCalledOnCreateDrawableState = true;
        return super.onCreateDrawableState(extraSpace);
    }

    public boolean hasCalledOnCreateDrawableState() {
        return mCalledOnCreateDrawableState;
    }

    @Override
    public void playSoundEffect(int soundConstant) {
        super.playSoundEffect(soundConstant);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        mCalledDispatchRestoreInstanceState = true;
    }

    public boolean hasCalledDispatchRestoreInstanceState() {
        return mCalledDispatchRestoreInstanceState;
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);
        mCalledDispatchSaveInstanceState = true;
    }

    public boolean hasCalledDispatchSaveInstanceState() {
        return mCalledDispatchSaveInstanceState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mCalledDrawableStateChanged = true;
    }

    public boolean hasCalledDrawableStateChanged() {
        return mCalledDrawableStateChanged;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        mCalledDispatchKeyEventPreIme = true;
        return super.dispatchKeyEventPreIme(event);
    }

    public boolean hasCalledDispatchKeyEventPreIme() {
        return mCalledDispatchKeyEventPreIme;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        mCalledOnKeyPreIme = true;
        return super.onKeyPreIme(keyCode, event);
    }

    public boolean hasCalledOnKeyPreIme() {
        return mCalledOnKeyPreIme;
    }

    public void reset() {
        mCalledOnCreateContextMenu = false;

        mCalledOnAnimationStart = false;
        mCalledOnAnimationEnd = false;
        mCalledOnAttachedToWindow = false;
        mCalledOnDetachedFromWindow = false;
        mCalledOnCreateDrawableState = false;
        mCalledDrawableStateChanged = false;
        mCalledOnDraw = false;
        mCalledDispatchDraw = false;
        mCalledOnFinishInflate = false;
        mCalledOnFocusChanged = false;
        mCalledOnKeyDown = false;
        mCalledOnKeyUp = false;
        mCalledOnKeyMultiple = false;
        mCalledOnKeyShortcut = false;
        mCalledOnLayout = false;
        mCalledOnMeasure = false;
        mCalledOnSizeChanged = false;
        mCalledOnSetAlpha = false;
        mCalledOnTouchEvent = false;
        mCalledOnTrackballEvent = false;
        mCalledOnWindowFocusChanged = false;
        mCalledDispatchRestoreInstanceState = false;
        mCalledDispatchSaveInstanceState = false;
        mCalledOnRestoreInstanceState = false;
        mCalledOnSaveInstanceState = false;
        mCalledDispatchUnhandledMove = false;
        mCalledDispatchWindowFocusChanged = false;
        mCalledDispatchWindowVisibilityChanged = false;
        mCalledOnWindowVisibilityChanged = false;
        mCalledOnScrollChanged = false;
        mCalledInvalidate = false;
        mCalledComputeScroll = false;
        mCalledDispatchKeyEventPreIme = false;
        mCalledOnKeyPreIme = false;

        mOldWidth = -1;
        mOldHeight = -1;
    }
}
