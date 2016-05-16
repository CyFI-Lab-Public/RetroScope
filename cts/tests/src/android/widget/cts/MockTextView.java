/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.widget.cts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.widget.TextView;

public class MockTextView extends TextView {
    private boolean mHasCalledOnCreateContextMenu;
    private boolean mHasCalledOnFocusChanged;
    private boolean mHasCalledOnMeasure;
    private boolean mHasCalledOnTextChanged;
    private boolean mHasCalledDrawableStateChanged;
    private boolean mHasCalledOnWindowFocusChanged;
    private boolean mHasCalledOnPrivateIMECommand;
    private boolean mHasCalledOnKeyMultiple;

    public MockTextView(Context context) {
        super(context);
    }

    public MockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MockTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean hasCalledOnWindowFocusChanged() {
        return mHasCalledOnWindowFocusChanged;
    }

    public boolean hasCalledOnCreateContextMenu() {
        return mHasCalledOnCreateContextMenu;
    }

    public boolean hasCalledDrawableStateChanged() {
        return mHasCalledDrawableStateChanged;
    }

    public boolean hasCalledOnFocusChanged() {
        return mHasCalledOnFocusChanged;
    }

    public boolean hasCalledOnMeasure() {
        return mHasCalledOnMeasure;
    }

    public boolean hasCalledOnTextChanged() {
        return mHasCalledOnTextChanged;
    }

    public boolean hasCalledOnPrivateIMECommand() {
        return mHasCalledOnPrivateIMECommand;
    }

    public boolean hasCalledOnKeyMultiple(){
        return mHasCalledOnKeyMultiple;
    }

    public void reset() {
        mHasCalledOnWindowFocusChanged = false;
        mHasCalledDrawableStateChanged = false;
        mHasCalledOnCreateContextMenu = false;
        mHasCalledOnFocusChanged = false;
        mHasCalledOnMeasure = false;
        mHasCalledOnTextChanged = false;
        mHasCalledOnPrivateIMECommand = false;
        mHasCalledOnKeyMultiple = false;
    }

    public int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    public int computeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mHasCalledDrawableStateChanged = true;
    }

    public boolean getDefaultEditable() {
        return super.getDefaultEditable();
    }

    public MovementMethod getDefaultMovementMethod() {
        return super.getDefaultMovementMethod();
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        super.onCreateContextMenu(menu);
        mHasCalledOnCreateContextMenu = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        mHasCalledOnFocusChanged = true;
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        mHasCalledOnKeyMultiple = true;
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHasCalledOnMeasure = true;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        mHasCalledOnTextChanged = true;
    }

    public boolean setFrame(int l, int t, int r, int b) {
        return super.setFrame(l, t, r, b);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mHasCalledOnWindowFocusChanged = true;
    }

    public float getLeftFadingEdgeStrength() {
        return super.getLeftFadingEdgeStrength();
    }

    public float getRightFadingEdgeStrength() {
        return super.getRightFadingEdgeStrength();
    }

    @Override
    public boolean onPrivateIMECommand(String action, Bundle data) {
        mHasCalledOnPrivateIMECommand = true;
        return super.onPrivateIMECommand(action, data);
    }

    public int getFrameLeft() {
        return mLeft;
    }

    public int getFrameTop() {
        return mTop;
    }

    public int getFrameRight() {
        return mRight;
    }

    public int getFrameBottom() {
        return mBottom;
    }

    public int getBottomPaddingOffset() {
        return super.getBottomPaddingOffset();
    }

    public int getLeftPaddingOffset() {
        return super.getLeftPaddingOffset();
    }

    public int getRightPaddingOffset() {
        return super.getRightPaddingOffset();
    }

    public int getTopPaddingOffset() {
        return super.getTopPaddingOffset();
    }

    public boolean isPaddingOffsetRequired() {
        return super.isPaddingOffsetRequired();
    }

    public boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who);
    }

    public int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }
}
