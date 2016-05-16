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

package android.graphics.drawable.cts;


import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LevelListDrawable;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;

import junit.framework.TestCase;

public class DrawableContainerStateTest extends TestCase{
    private DrawableContainerState mDrawableContainerState;

    private DrawableContainer mDrawableContainer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // DrawableContainerState has no public constructor. Obtain an instance through
        // LevelListDrawable.getConstants(). This is fine for testing the final methods of
        // DrawableContainerState.
        mDrawableContainer = new LevelListDrawable();
        mDrawableContainerState = (DrawableContainerState) mDrawableContainer.getConstantState();
        assertNotNull(mDrawableContainerState);
    }

    public void testAddChild() {
        try {
            mDrawableContainerState.addChild(null);
            fail("Should throw NullPointerException if the drawable is null.");
        } catch (NullPointerException e) {
        }

        assertEquals(0, mDrawableContainerState.getChildCount());

        MockDrawable dr0 = new MockDrawable();
        dr0.setVisible(true, false);
        assertTrue(dr0.isVisible());
        assertEquals(0, mDrawableContainerState.addChild(dr0));
        assertEquals(1, mDrawableContainerState.getChildCount());
        Drawable[] children = mDrawableContainerState.getChildren();
        assertNotNull(children);
        assertTrue(children.length >= 1);
        assertSame(dr0, children[0]);
        assertNull(children[1]);
        assertFalse(dr0.isVisible());

        MockDrawable dr1 = new MockDrawable();
        dr1.setVisible(true, false);
        assertTrue(dr1.isVisible());
        assertEquals(1, mDrawableContainerState.addChild(dr1));
        assertEquals(2, mDrawableContainerState.getChildCount());
        children = mDrawableContainerState.getChildren();
        assertNotNull(children);
        assertTrue(children.length >= 2);
        assertSame(dr0, children[0]);
        assertSame(dr1, children[1]);
        assertNull(children[2]);
        assertFalse(dr1.isVisible());

        // Add the same object twice, is it OK?
        assertEquals(2, mDrawableContainerState.addChild(dr1));
        assertEquals(3, mDrawableContainerState.getChildCount());
        children = mDrawableContainerState.getChildren();
        assertNotNull(children);
        assertTrue(children.length >= 3);
        assertSame(dr1, children[1]);
        assertSame(dr1, children[2]);
    }

    public void testIsStateFul() {
        assertEquals(0, mDrawableContainerState.getChildCount());
        assertFalse(mDrawableContainerState.isStateful());

        MockDrawable dr0 = new MockDrawable();
        dr0.setStateful(false);
        mDrawableContainerState.addChild(dr0);
        assertEquals(1, mDrawableContainerState.getChildCount());
        assertFalse(mDrawableContainerState.isStateful());

        MockDrawable dr1 = new MockDrawable();
        dr1.setStateful(false);
        mDrawableContainerState.addChild(dr1);
        assertEquals(2, mDrawableContainerState.getChildCount());
        assertFalse(mDrawableContainerState.isStateful());

        MockDrawable dr2 = new MockDrawable();
        dr2.setStateful(true);
        mDrawableContainerState.addChild(dr2);
        assertEquals(3, mDrawableContainerState.getChildCount());
        assertTrue(mDrawableContainerState.isStateful());

        MockDrawable dr3 = new MockDrawable();
        dr3.setStateful(false);
        mDrawableContainerState.addChild(dr3);
        assertEquals(4, mDrawableContainerState.getChildCount());
        assertTrue(mDrawableContainerState.isStateful());
    }

    public void testAccessConstantSize() {
        mDrawableContainerState.setConstantSize(true);
        assertTrue(mDrawableContainerState.isConstantSize());

        mDrawableContainerState.setConstantSize(false);
        assertFalse(mDrawableContainerState.isConstantSize());
    }

    public void testAccessConstantPadding() {
        mDrawableContainerState.setVariablePadding(true);
        assertNull(mDrawableContainerState.getConstantPadding());

        /*
         * TODO: the behavior of getConstantPadding when variable padding is 
         * false is undefined
         * 
        mDrawableContainerState.setVariablePadding(false);
        Rect padding = mDrawableContainerState.getConstantPadding();
        assertNotNull(padding);
        assertEquals(new Rect(0, 0, 0, 0), padding);

        MockDrawable dr0 = new MockDrawable();
        dr0.setPadding(new Rect(1, 2, 0, 0));
        mDrawableContainerState.addChild(dr0);
        padding = mDrawableContainerState.getConstantPadding();
        assertNotNull(padding);
        assertEquals(new Rect(1, 2, 0, 0), padding);

        MockDrawable dr1 = new MockDrawable();
        dr1.setPadding(new Rect(0, 0, 3, 4));
        mDrawableContainerState.addChild(dr1);
        padding = mDrawableContainerState.getConstantPadding();
        assertNotNull(padding);
        assertEquals(new Rect(1, 2, 3, 4), padding);

        mDrawableContainerState.setVariablePadding(true);
        assertNull(mDrawableContainerState.getConstantPadding());
        */
    }

    public void testConstantHeightsAndWidths() {
        assertEquals(0, mDrawableContainerState.getChildCount());
        assertEquals(-1, mDrawableContainerState.getConstantHeight());
        assertEquals(-1, mDrawableContainerState.getConstantWidth());
        assertEquals(0, mDrawableContainerState.getConstantMinimumHeight());
        assertEquals(0, mDrawableContainerState.getConstantMinimumWidth());

        MockDrawable dr0 = new MockDrawable();
        dr0.setMinimumHeight(1);
        dr0.setMinimumWidth(2);
        dr0.setIntrinsicHeight(0);
        dr0.setIntrinsicWidth(0);
        mDrawableContainerState.addChild(dr0);
        assertEquals(1, mDrawableContainerState.getChildCount());
        assertEquals(0, mDrawableContainerState.getConstantHeight());
        assertEquals(0, mDrawableContainerState.getConstantWidth());
        assertEquals(1, mDrawableContainerState.getConstantMinimumHeight());
        assertEquals(2, mDrawableContainerState.getConstantMinimumWidth());

        MockDrawable dr1 = new MockDrawable();
        dr1.setMinimumHeight(0);
        dr1.setMinimumWidth(0);
        dr1.setIntrinsicHeight(3);
        dr1.setIntrinsicWidth(4);
        mDrawableContainerState.addChild(dr1);
        assertEquals(2, mDrawableContainerState.getChildCount());
        assertEquals(3, mDrawableContainerState.getConstantHeight());
        assertEquals(4, mDrawableContainerState.getConstantWidth());
        assertEquals(1, mDrawableContainerState.getConstantMinimumHeight());
        assertEquals(2, mDrawableContainerState.getConstantMinimumWidth());

        MockDrawable dr2 = new MockDrawable();
        dr2.setMinimumHeight(5);
        dr2.setMinimumWidth(5);
        dr2.setIntrinsicHeight(5);
        dr2.setIntrinsicWidth(5);
        mDrawableContainerState.addChild(dr2);
        assertEquals(3, mDrawableContainerState.getChildCount());
        assertEquals(5, mDrawableContainerState.getConstantHeight());
        assertEquals(5, mDrawableContainerState.getConstantWidth());
        assertEquals(5, mDrawableContainerState.getConstantMinimumHeight());
        assertEquals(5, mDrawableContainerState.getConstantMinimumWidth());
    }

    public void testGetOpacity() {
        assertEquals(0, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.TRANSPARENT, mDrawableContainerState.getOpacity());

        MockDrawable dr0 = new MockDrawable();
        dr0.setOpacity(PixelFormat.OPAQUE);
        mDrawableContainerState.addChild(dr0);
        assertEquals(1, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.OPAQUE, mDrawableContainerState.getOpacity());

        MockDrawable dr1 = new MockDrawable();
        dr1.setOpacity(PixelFormat.TRANSPARENT);
        mDrawableContainerState.addChild(dr1);
        assertEquals(2, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.TRANSPARENT, mDrawableContainerState.getOpacity());

        MockDrawable dr2 = new MockDrawable();
        dr2.setOpacity(PixelFormat.TRANSLUCENT);
        mDrawableContainerState.addChild(dr2);
        assertEquals(3, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.TRANSLUCENT, mDrawableContainerState.getOpacity());

        MockDrawable dr3 = new MockDrawable();
        dr3.setOpacity(PixelFormat.UNKNOWN);
        mDrawableContainerState.addChild(dr3);
        assertEquals(4, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.UNKNOWN, mDrawableContainerState.getOpacity());

        MockDrawable dr4 = new MockDrawable();
        dr4.setOpacity(PixelFormat.TRANSLUCENT);
        mDrawableContainerState.addChild(dr4);
        assertEquals(5, mDrawableContainerState.getChildCount());
        assertEquals(PixelFormat.UNKNOWN, mDrawableContainerState.getOpacity());
    }

    public void testCanConstantState(){
    }

    public void testGetChangingConfigurations(){
    }

    public void testGrowArray(){
    }

    private class MockDrawable extends Drawable {
        private boolean mIsStatful;

        private Rect mPadding;

        private int mIntrinsicHeight;

        private int mIntrinsicWidth;

        private int mMinimumHeight;

        private int mMinimumWidth;

        private int mOpacity;

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public int getOpacity() {
            return mOpacity;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public boolean isStateful() {
            return mIsStatful;
        }

        public void setStateful(boolean isStateful) {
            mIsStatful = isStateful;
        }

        public void setPadding(Rect rect) {
            if (mPadding == null) {
                mPadding = new Rect();
            }
            mPadding.left = rect.left;
            mPadding.right = rect.right;
            mPadding.top = rect.top;
            mPadding.bottom = rect.bottom;
        }

        @Override
        public boolean getPadding(Rect padding) {
            if (padding == null) {
                return false;
            }
            if (mPadding == null) {
                return false;
            }
            padding.left = mPadding.left;
            padding.top = mPadding.top;
            padding.right = mPadding.right;
            padding.bottom = mPadding.bottom;
            return true;
        }

        @Override
        public int getMinimumHeight() {
            return mMinimumHeight;
        }

        @Override
        public int getMinimumWidth() {
            return mMinimumWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }

        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }

        public void setMinimumHeight(int h) {
            mMinimumHeight = h;
        }

        public void setMinimumWidth(int w) {
            mMinimumWidth = w;
        }

        public void setIntrinsicHeight(int h) {
            mIntrinsicHeight = h;
        }

        public void setIntrinsicWidth(int w) {
            mIntrinsicWidth = w;
        }

        public void setOpacity(int opacity){
            mOpacity = opacity;
        }
    }
}
