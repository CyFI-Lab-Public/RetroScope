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

import junit.framework.TestCase;

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.LevelListDrawable;

public class DrawableContainerTest extends TestCase {
    private DrawableContainerState mDrawableContainerState;

    private MockDrawableContainer mDrawableContainer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // DrawableContainerState has no public constructor. Obtain an instance through
        // LevelListDrawable.getConstants(). This is fine for testing the final methods of
        // DrawableContainerState.
        mDrawableContainerState =
            (DrawableContainerState) new LevelListDrawable().getConstantState();
        assertNotNull(mDrawableContainerState);
        mDrawableContainer = new MockDrawableContainer();
    }

    public void testDraw() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.draw(null);
        mDrawableContainer.draw(new Canvas());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        dr.reset();
        mDrawableContainer.draw(null);
        assertTrue(dr.hasDrawCalled());

        dr.reset();
        mDrawableContainer.draw(new Canvas());
        assertTrue(dr.hasDrawCalled());
    }

    public void testGetChangingConfigurations() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.getChangingConfigurations();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setChangingConfigurations(0x001);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setChangingConfigurations(0x010);
        mDrawableContainerState.addChild(dr1);
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());

        // can not set mDrawableContainerState's ChangingConfigurations
        mDrawableContainer.setChangingConfigurations(0x100);
        assertEquals(0x111 | mDrawableContainerState.getChangingConfigurations(),
                mDrawableContainer.getChangingConfigurations());
    }

    public void testGetPadding() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        Rect result = new Rect(1, 1, 1, 1);
        try {
            mDrawableContainer.getPadding(result);
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setPadding(new Rect(1, 2, 0, 0));
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setPadding(new Rect(0, 0, 3, 4));
        mDrawableContainerState.addChild(dr1);
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());

        // use the current drawable's padding
        mDrawableContainerState.setVariablePadding(true);
        assertNull(mDrawableContainerState.getConstantPadding());
        assertTrue(mDrawableContainer.getPadding(result));
        assertEquals(new Rect(1, 2, 0, 0), result);

        // use constant state's padding
        mDrawableContainerState.setVariablePadding(false);
        assertNotNull(mDrawableContainerState.getConstantPadding());
        assertTrue(mDrawableContainer.getPadding(result));
        assertEquals(mDrawableContainerState.getConstantPadding(), result);

        // use default padding
        mDrawableContainer.selectDrawable(-1);
        assertNull(mDrawableContainer.getCurrent());
        mDrawableContainerState.setVariablePadding(true);
        assertNull(mDrawableContainerState.getConstantPadding());
        assertFalse(mDrawableContainer.getPadding(result));
        assertEquals(new Rect(0, 0, 0, 0), result);

        try {
            mDrawableContainer.getPadding(null);
            fail("Should throw NullPointerException if the padding is null.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetAlpha() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setAlpha(0);

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        // call current drawable's setAlpha if alpha is changed.
        dr.reset();
        mDrawableContainer.setAlpha(1);
        assertTrue(dr.hasSetAlphaCalled());

        // does not call it if alpha is not changed.
        dr.reset();
        mDrawableContainer.setAlpha(1);
        assertFalse(dr.hasSetAlphaCalled());
    }

    public void testSetDither() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        mDrawableContainer.setDither(false);
        mDrawableContainer.setDither(true);

        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        // call current drawable's setDither if dither is changed.
        dr.reset();
        mDrawableContainer.setDither(false);
        assertTrue(dr.hasSetDitherCalled());

        // does not call it if dither is not changed.
        dr.reset();
        mDrawableContainer.setDither(true);
        assertTrue(dr.hasSetDitherCalled());
    }

    public void testSetColorFilter() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setColorFilter(null);
        mDrawableContainer.setColorFilter(new ColorFilter());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        // call current drawable's setColorFilter if filter is changed.
        dr.reset();
        mDrawableContainer.setColorFilter(null);
        assertTrue(dr.hasSetColorFilterCalled());

        // does not call it if filter is not changed.
        dr.reset();
        mDrawableContainer.setColorFilter(new ColorFilter());
        assertTrue(dr.hasSetColorFilterCalled());
    }

    public void testOnBoundsChange() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.onBoundsChange(new Rect());
        mDrawableContainer.onBoundsChange(null);

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        dr.setBounds(new Rect());
        addAndSelectDrawable(dr);

        // set current drawable's bounds.
        dr.reset();
        assertEquals(new Rect(), dr.getBounds());
        mDrawableContainer.onBoundsChange(new Rect(1, 1, 1, 1));
        assertTrue(dr.hasOnBoundsChangedCalled());
        assertEquals(new Rect(1, 1, 1, 1), dr.getBounds());

        dr.reset();
        mDrawableContainer.onBoundsChange(new Rect(1, 1, 1, 1));
        assertFalse(dr.hasOnBoundsChangedCalled());
        assertEquals(new Rect(1, 1, 1, 1), dr.getBounds());

        try {
            mDrawableContainer.onBoundsChange(null);
            fail("Should throw NullPointerException if the bounds is null.");
        } catch (NullPointerException e) {
        }
    }

    public void testIsStateful() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.isStateful();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setStateful(true);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setStateful(false);
        mDrawableContainerState.addChild(dr1);

        // return result of constant state's isStateful
        assertEquals(mDrawableContainerState.isStateful(), mDrawableContainer.isStateful());
        assertEquals(true, mDrawableContainer.isStateful());

        mDrawableContainer.selectDrawable(1);
        assertEquals(mDrawableContainerState.isStateful(), mDrawableContainer.isStateful());
        assertEquals(true, mDrawableContainer.isStateful());
    }

    public void testOnStateChange() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        assertFalse(mDrawableContainer.onStateChange(new int[] { 0 }));
        assertFalse(mDrawableContainer.onStateChange(null));

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        dr.setState(new int[] { 0 });
        addAndSelectDrawable(dr);

        // set current drawable's state.
        dr.reset();
        assertNotNull(dr.getState());
        mDrawableContainer.onStateChange(null);
        assertTrue(dr.hasOnStateChangedCalled());
        assertNull(dr.getState());

        dr.reset();
        mDrawableContainer.onStateChange(new int[] { 0 });
        assertTrue(dr.hasOnStateChangedCalled());
        assertTrue(Arrays.equals(new int[] { 0 }, dr.getState()));

        dr.reset();
        assertFalse(mDrawableContainer.onStateChange(new int[] { 0 }));
        assertFalse(dr.hasOnStateChangedCalled());
        assertTrue(Arrays.equals(new int[] { 0 }, dr.getState()));
    }

    public void testOnLevelChange() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        assertFalse(mDrawableContainer.onLevelChange(Integer.MAX_VALUE));
        assertFalse(mDrawableContainer.onLevelChange(Integer.MIN_VALUE));

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        dr.setLevel(0);
        addAndSelectDrawable(dr);

        // set current drawable's level.
        dr.reset();
        assertEquals(0, dr.getLevel());
        mDrawableContainer.onLevelChange(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, dr.getLevel());
        assertTrue(dr.hasOnLevelChangedCalled());

        dr.reset();
        assertEquals(Integer.MAX_VALUE, dr.getLevel());
        mDrawableContainer.onLevelChange(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, dr.getLevel());
        assertTrue(dr.hasOnLevelChangedCalled());

        dr.reset();
        assertEquals(Integer.MIN_VALUE, dr.getLevel());
        assertFalse(mDrawableContainer.onLevelChange(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, dr.getLevel());
        assertFalse(dr.hasOnLevelChangedCalled());
    }

    public void testGetIntrinsicWidth() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.getIntrinsicWidth();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setIntrinsicWidth(1);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setIntrinsicWidth(2);
        mDrawableContainerState.addChild(dr1);

        // return result of constant state's getConstantWidth
        mDrawableContainerState.setConstantSize(true);
        assertEquals(mDrawableContainerState.getConstantWidth(),
                mDrawableContainer.getIntrinsicWidth());
        assertEquals(2, mDrawableContainer.getIntrinsicWidth());

        // return default value
        mDrawableContainerState.setConstantSize(false);
        assertNull(mDrawableContainer.getCurrent());
        assertEquals(-1, mDrawableContainer.getIntrinsicWidth());

        // return current drawable's getIntrinsicWidth
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());
        assertEquals(1, mDrawableContainer.getIntrinsicWidth());
    }

    public void testGetIntrinsicHeight() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.getIntrinsicHeight();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setIntrinsicHeight(1);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setIntrinsicHeight(2);
        mDrawableContainerState.addChild(dr1);

        // return result of constant state's getConstantHeight
        mDrawableContainerState.setConstantSize(true);
        assertEquals(mDrawableContainerState.getConstantHeight(),
                mDrawableContainer.getIntrinsicHeight());
        assertEquals(2, mDrawableContainer.getIntrinsicHeight());

        // return default value
        mDrawableContainerState.setConstantSize(false);
        assertNull(mDrawableContainer.getCurrent());
        assertEquals(-1, mDrawableContainer.getIntrinsicHeight());

        // return current drawable's getIntrinsicHeight
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());
        assertEquals(1, mDrawableContainer.getIntrinsicHeight());
    }

    public void testGetMinimumWidth() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.getMinimumWidth();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setMinimumWidth(1);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setMinimumWidth(2);
        mDrawableContainerState.addChild(dr1);

        // return result of constant state's getConstantMinimumWidth
        mDrawableContainerState.setConstantSize(true);
        assertEquals(mDrawableContainerState.getConstantMinimumWidth(),
                mDrawableContainer.getMinimumWidth());
        assertEquals(2, mDrawableContainer.getMinimumWidth());

        // return default value
        mDrawableContainerState.setConstantSize(false);
        assertNull(mDrawableContainer.getCurrent());
        assertEquals(0, mDrawableContainer.getMinimumWidth());

        // return current drawable's getMinimumWidth
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());
        assertEquals(1, mDrawableContainer.getMinimumWidth());
    }

    public void testGetMinimumHeight() {
        assertConstantStateNotSet();

        try {
            mDrawableContainer.getMinimumHeight();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setMinimumHeight(1);
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setMinimumHeight(2);
        mDrawableContainerState.addChild(dr1);

        // return result of constant state's getConstantMinimumHeight
        mDrawableContainerState.setConstantSize(true);
        assertEquals(mDrawableContainerState.getConstantMinimumHeight(),
                mDrawableContainer.getMinimumHeight());
        assertEquals(2, mDrawableContainer.getMinimumHeight());

        // return default value
        mDrawableContainerState.setConstantSize(false);
        assertNull(mDrawableContainer.getCurrent());
        assertEquals(0, mDrawableContainer.getMinimumHeight());

        // return current drawable's getMinimumHeight
        mDrawableContainer.selectDrawable(0);
        assertSame(dr0, mDrawableContainer.getCurrent());
        assertEquals(1, mDrawableContainer.getMinimumHeight());
    }

    public void testInvalidateDrawable() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setCallback(null);
        mDrawableContainer.invalidateDrawable(mDrawableContainer);
        mDrawableContainer.invalidateDrawable(null);

        MockCallBack callback = new MockCallBack();
        mDrawableContainer.setCallback(callback);

        callback.reset();
        mDrawableContainer.invalidateDrawable(mDrawableContainer);
        assertFalse(callback.hasInvalidateDrawableCalled());

        // the callback method can be called if the drawable passed in and the
        // current drawble are both null
        callback.reset();
        mDrawableContainer.invalidateDrawable(null);
        assertTrue(callback.hasInvalidateDrawableCalled());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        callback.reset();
        mDrawableContainer.invalidateDrawable(mDrawableContainer);
        assertFalse(callback.hasInvalidateDrawableCalled());

        callback.reset();
        mDrawableContainer.invalidateDrawable(null);
        assertFalse(callback.hasInvalidateDrawableCalled());

        // Call the callback method if the drawable is selected.
        callback.reset();
        mDrawableContainer.invalidateDrawable(dr);
        assertTrue(callback.hasInvalidateDrawableCalled());
    }

    public void testScheduleDrawable() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setCallback(null);
        mDrawableContainer.scheduleDrawable(mDrawableContainer, null, 0);
        mDrawableContainer.scheduleDrawable(null, new Runnable() {
                public void run() {
                }
            }, 0);

        MockCallBack callback = new MockCallBack();
        mDrawableContainer.setCallback(callback);

        callback.reset();
        mDrawableContainer.scheduleDrawable(mDrawableContainer, null, 0);
        assertFalse(callback.hasScheduleDrawableCalled());

        // the callback method can be called if the drawable passed in and the
        // current drawble are both null
        callback.reset();
        mDrawableContainer.scheduleDrawable(null, new Runnable() {
                public void run() {
                }
            }, 0);
        assertTrue(callback.hasScheduleDrawableCalled());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        callback.reset();
        mDrawableContainer.scheduleDrawable(mDrawableContainer, null, 0);
        assertFalse(callback.hasScheduleDrawableCalled());

        callback.reset();
        mDrawableContainer.scheduleDrawable(null, new Runnable() {
                public void run() {
                }
            }, 0);
        assertFalse(callback.hasScheduleDrawableCalled());

        // Call the callback method if the drawable is selected.
        callback.reset();
        mDrawableContainer.scheduleDrawable(dr, null, 0);
        assertTrue(callback.hasScheduleDrawableCalled());
    }

    public void testUnscheduleDrawable() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        mDrawableContainer.setCallback(null);
        mDrawableContainer.unscheduleDrawable(mDrawableContainer, null);
        mDrawableContainer.unscheduleDrawable(null, new Runnable() {
                public void run() {
                }
            });

        MockCallBack callback = new MockCallBack();
        mDrawableContainer.setCallback(callback);

        callback.reset();
        mDrawableContainer.unscheduleDrawable(mDrawableContainer, null);
        assertFalse(callback.hasUnscheduleDrawableCalled());

        // the callback method can be called if the drawable passed in and the
        // current drawble are both null
        callback.reset();
        mDrawableContainer.unscheduleDrawable(null, new Runnable() {
                public void run() {
                }
            });
        assertTrue(callback.hasUnscheduleDrawableCalled());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        callback.reset();
        mDrawableContainer.unscheduleDrawable(mDrawableContainer, null);
        assertFalse(callback.hasUnscheduleDrawableCalled());

        callback.reset();
        mDrawableContainer.unscheduleDrawable(null, new Runnable() {
                public void run() {
                }
            });
        assertFalse(callback.hasUnscheduleDrawableCalled());

        // Call the callback method if the drawable is selected.
        callback.reset();
        mDrawableContainer.unscheduleDrawable(dr, null);
        assertTrue(callback.hasUnscheduleDrawableCalled());
    }

    public void testSetVisible() {
        assertConstantStateNotSet();
        assertNull(mDrawableContainer.getCurrent());

        assertTrue(mDrawableContainer.isVisible());
        assertFalse(mDrawableContainer.setVisible(true, false));
        assertTrue(mDrawableContainer.setVisible(false, false));
        assertFalse(mDrawableContainer.setVisible(false, false));
        assertTrue(mDrawableContainer.setVisible(true, false));

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr = new MockDrawable();
        addAndSelectDrawable(dr);

        // set current drawable's visibility
        assertTrue(mDrawableContainer.isVisible());
        assertTrue(dr.isVisible());
        assertTrue(mDrawableContainer.setVisible(false, false));
        assertFalse(mDrawableContainer.isVisible());
        assertFalse(dr.isVisible());
    }

    public void testGetOpacity() {
        assertConstantStateNotSet();

        // there is no child, so the container is transparent
        assertEquals(PixelFormat.TRANSPARENT, mDrawableContainer.getOpacity());

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setOpacity(PixelFormat.OPAQUE);
        mDrawableContainerState.addChild(dr0);
        // no child selected yet
        assertEquals(PixelFormat.TRANSPARENT, mDrawableContainer.getOpacity());

        mDrawableContainer.selectDrawable(0);
        assertEquals(mDrawableContainerState.getOpacity(), mDrawableContainer.getOpacity());
        assertEquals(PixelFormat.OPAQUE, mDrawableContainer.getOpacity());

        MockDrawable dr1 = new MockDrawable();
        dr1.setOpacity(PixelFormat.TRANSLUCENT);
        mDrawableContainerState.addChild(dr1);

        mDrawableContainer.selectDrawable(1);
        assertEquals(mDrawableContainerState.getOpacity(), mDrawableContainer.getOpacity());
        assertEquals(PixelFormat.TRANSLUCENT, mDrawableContainer.getOpacity());
    }

    public void testAccessCurrentDrawable() {
        assertConstantStateNotSet();

        assertNull(mDrawableContainer.getCurrent());
        try {
            mDrawableContainer.selectDrawable(0);
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        dr0.setVisible(false, false);
        assertFalse(dr0.isVisible());
        mDrawableContainerState.addChild(dr0);
        MockDrawable dr1 = new MockDrawable();
        dr1.setVisible(false, false);
        assertFalse(dr1.isVisible());
        mDrawableContainerState.addChild(dr1);

        assertTrue(mDrawableContainer.selectDrawable(0));
        assertSame(dr0, mDrawableContainer.getCurrent());
        assertTrue(dr0.isVisible());

        assertFalse(mDrawableContainer.selectDrawable(0));

        assertTrue(mDrawableContainer.selectDrawable(1));
        assertSame(dr1, mDrawableContainer.getCurrent());
        assertTrue(dr1.isVisible());
        assertFalse(dr0.isVisible());

        assertFalse(mDrawableContainer.selectDrawable(1));

        assertTrue(mDrawableContainer.selectDrawable(-1));
        assertNull(mDrawableContainer.getCurrent());
        assertFalse(dr0.isVisible());
        assertFalse(dr1.isVisible());

        assertTrue(mDrawableContainer.selectDrawable(2));
        assertNull(mDrawableContainer.getCurrent());
        assertFalse(dr0.isVisible());
        assertFalse(dr1.isVisible());
    }

    public void testAccessConstantState() {
        try {
            mDrawableContainer.getConstantState();
            fail("Should throw NullPointerException if the constant state is not set.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        assertSame(mDrawableContainerState, mDrawableContainer.getConstantState());

        mDrawableContainer.setConstantState(null);
        assertConstantStateNotSet();
    }

    public void testMutate() {
        assertConstantStateNotSet();
        try {
            mDrawableContainer.mutate();
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        mDrawableContainer.setConstantState(mDrawableContainerState);
        MockDrawable dr0 = new MockDrawable();
        mDrawableContainerState.addChild(dr0);
        mDrawableContainer.mutate();
        assertTrue(dr0.hasMutateCalled());
    }

    private void addAndSelectDrawable(MockDrawable dr) {
        int pos = mDrawableContainerState.addChild(dr);
        mDrawableContainer.selectDrawable(pos);
        assertSame(dr, mDrawableContainer.getCurrent());
    }

    private void assertConstantStateNotSet() {
        try {
            mDrawableContainer.getConstantState();
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    private class MockDrawableContainer extends DrawableContainer {
        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
        }

        @Override
        protected boolean onLevelChange(int level) {
            return super.onLevelChange(level);
        }

        @Override
        protected boolean onStateChange(int[] state) {
            return super.onStateChange(state);
        }

        @Override
        protected void setConstantState(DrawableContainerState state) {
            super.setConstantState(state);
        }
    }

    private class MockDrawable extends Drawable {
        private boolean mHasCalledDraw;

        private boolean mHasCalledSetAlpha;

        private boolean mHasCalledSetColorFilter;

        private boolean mHasCalledSetDither;

        private boolean mHasCalledOnBoundsChanged;

        private boolean mHasCalledOnStateChanged;

        private boolean mHasCalledOnLevelChanged;

        private boolean mHasCalledMutate;

        private boolean mIsStatful;

        private Rect mPadding;

        private int mIntrinsicHeight;

        private int mIntrinsicWidth;

        private int mMinimumHeight;

        private int mMinimumWidth;

        private int mOpacity;

        @Override
        public int getOpacity() {
            return mOpacity;
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
            mPadding.set(rect);
        }

        @Override
        public boolean getPadding(Rect padding) {
            if (padding == null || mPadding == null) {
                return false;
            }
            padding.set(mPadding);
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

        public Drawable mutate() {
            mHasCalledMutate = true;
            return this;
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

        public void setOpacity(int opacity) {
            mOpacity = opacity;
        }

        public boolean hasDrawCalled() {
            return mHasCalledDraw;
        }

        public boolean hasSetAlphaCalled() {
            return mHasCalledSetAlpha;
        }

        public boolean hasSetColorFilterCalled() {
            return mHasCalledSetColorFilter;
        }

        public boolean hasSetDitherCalled() {
            return mHasCalledSetDither;
        }

        public boolean hasOnBoundsChangedCalled() {
            return mHasCalledOnBoundsChanged;
        }

        public boolean hasOnStateChangedCalled() {
            return mHasCalledOnStateChanged;
        }

        public boolean hasOnLevelChangedCalled() {
            return mHasCalledOnLevelChanged;
        }

        public boolean hasMutateCalled() {
            return mHasCalledMutate;
        }

        public void reset() {
            mHasCalledOnLevelChanged = false;
            mHasCalledOnStateChanged = false;
            mHasCalledOnBoundsChanged = false;
            mHasCalledSetDither = false;
            mHasCalledSetColorFilter = false;
            mHasCalledSetAlpha = false;
            mHasCalledDraw = false;
            mHasCalledMutate = false;
        }

        @Override
        public void draw(Canvas canvas) {
            mHasCalledDraw = true;
        }

        @Override
        public void setAlpha(int alpha) {
            mHasCalledSetAlpha = true;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mHasCalledSetColorFilter = true;
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mHasCalledOnBoundsChanged = true;
        }

        @Override
        protected boolean onLevelChange(int level) {
            boolean result = super.onLevelChange(level);
            mHasCalledOnLevelChanged = true;
            return result;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean result = super.onStateChange(state);
            mHasCalledOnStateChanged = true;
            return result;

        }

        @Override
        public void setDither(boolean dither) {
            super.setDither(dither);
            mHasCalledSetDither = true;
        }
    }

    private class MockCallBack implements Drawable.Callback {
        private boolean mCalledInvalidateDrawable;

        private boolean mCalledScheduleDrawable;

        private boolean mCalledUnscheduleDrawable;

        public boolean hasInvalidateDrawableCalled() {
            return mCalledInvalidateDrawable;
        }

        public boolean hasScheduleDrawableCalled() {
            return mCalledScheduleDrawable;
        }

        public boolean hasUnscheduleDrawableCalled() {
            return mCalledUnscheduleDrawable;
        }

        public void reset() {
            mCalledUnscheduleDrawable = false;
            mCalledScheduleDrawable = false;
            mCalledInvalidateDrawable = false;
        }

        public void invalidateDrawable(Drawable who) {
            mCalledInvalidateDrawable = true;
        }

        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mCalledScheduleDrawable = true;
        }

        public void unscheduleDrawable(Drawable who, Runnable what) {
            mCalledUnscheduleDrawable = true;
        }

        public int getResolvedLayoutDirection(Drawable who) {
            return 0;
        }
    }
}
