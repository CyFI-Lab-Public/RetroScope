/*
 * Copyright (C) 2008 The Android Open Source Project.
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

import com.android.cts.stub.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.ScaleDrawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.view.Gravity;

public class ScaleDrawableTest extends AndroidTestCase {

    @SuppressWarnings("deprecation")
    public void testConstructor() {
        Drawable d = new BitmapDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(d, Gravity.CENTER, 100, 200);
        assertSame(d, scaleDrawable.getDrawable());

        new ScaleDrawable(null, -1, Float.MAX_VALUE, Float.MIN_VALUE);
    }

    @SuppressWarnings("deprecation")
    public void testInvalidateDrawable() {
        ScaleDrawable scaleDrawable = new ScaleDrawable(new BitmapDrawable(),
                Gravity.CENTER, 100, 200);

        MockCallback cb = new MockCallback();
        scaleDrawable.setCallback(cb);
        scaleDrawable.invalidateDrawable(null);
        assertTrue(cb.hasCalledInvalidate());

        cb.reset();
        scaleDrawable.invalidateDrawable(new BitmapDrawable());
        assertTrue(cb.hasCalledInvalidate());

        cb.reset();
        scaleDrawable.setCallback(null);
        scaleDrawable.invalidateDrawable(null);
        assertFalse(cb.hasCalledInvalidate());
    }

    @SuppressWarnings("deprecation")
    public void testScheduleDrawable() {
        ScaleDrawable scaleDrawable = new ScaleDrawable(new BitmapDrawable(),
                Gravity.CENTER, 100, 200);

        MockCallback cb = new MockCallback();
        scaleDrawable.setCallback(cb);
        scaleDrawable.scheduleDrawable(null, null, 0);
        assertTrue(cb.hasCalledSchedule());

        cb.reset();
        scaleDrawable.scheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        }, 1000L);
        assertTrue(cb.hasCalledSchedule());

        cb.reset();
        scaleDrawable.setCallback(null);
        scaleDrawable.scheduleDrawable(null, null, 0);
        assertFalse(cb.hasCalledSchedule());
    }

    @SuppressWarnings("deprecation")
    public void testUnscheduleDrawable() {
        ScaleDrawable scaleDrawable = new ScaleDrawable(new BitmapDrawable(),
                Gravity.CENTER, 100, 200);

        MockCallback cb = new MockCallback();
        scaleDrawable.setCallback(cb);
        scaleDrawable.unscheduleDrawable(null, null);
        assertTrue(cb.hasCalledUnschedule());

        cb.reset();
        scaleDrawable.unscheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        });
        assertTrue(cb.hasCalledUnschedule());

        cb.reset();
        scaleDrawable.setCallback(null);
        scaleDrawable.unscheduleDrawable(null, null);
        assertFalse(cb.hasCalledUnschedule());
    }

    private static class MockCallback implements Drawable.Callback {
        private boolean mCalledInvalidate;
        private boolean mCalledSchedule;
        private boolean mCalledUnschedule;

        public void invalidateDrawable(Drawable who) {
            mCalledInvalidate = true;
        }

        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mCalledSchedule = true;
        }

        public void unscheduleDrawable(Drawable who, Runnable what) {
            mCalledUnschedule = true;
        }

        public boolean hasCalledInvalidate() {
            return mCalledInvalidate;
        }

        public boolean hasCalledSchedule() {
            return mCalledSchedule;
        }

        public boolean hasCalledUnschedule() {
            return mCalledUnschedule;
        }

        public int getResolvedLayoutDirection(Drawable who) {
            return 0;
        }

        public void reset() {
            mCalledInvalidate = false;
            mCalledSchedule = false;
            mCalledUnschedule = false;
        }
    }

    public void testDraw() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        scaleDrawable.draw(new Canvas());
        assertFalse(mockDrawable.hasCalledDraw());

        // this method will call the contained drawable's draw method
        // if the contained drawable's level doesn't equal 0.
        mockDrawable.setLevel(1);
        scaleDrawable.draw(new Canvas());
        assertTrue(mockDrawable.hasCalledDraw());

        mockDrawable.reset();
        scaleDrawable.draw(null);
        assertTrue(mockDrawable.hasCalledDraw());
    }

    public void testGetChangingConfigurations() {
        final int SUPER_CONFIG = 1;
        final int CONTAINED_DRAWABLE_CONFIG = 2;

        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        assertEquals(0, scaleDrawable.getChangingConfigurations());

        mockDrawable.setChangingConfigurations(CONTAINED_DRAWABLE_CONFIG);
        assertEquals(CONTAINED_DRAWABLE_CONFIG, scaleDrawable.getChangingConfigurations());

        scaleDrawable.setChangingConfigurations(SUPER_CONFIG);
        assertEquals(SUPER_CONFIG | CONTAINED_DRAWABLE_CONFIG,
                scaleDrawable.getChangingConfigurations());
    }

    public void testGetPadding() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's getPadding method.
        scaleDrawable.getPadding(new Rect());
        assertTrue(mockDrawable.hasCalledGetPadding());

        // input null as param
        try {
            scaleDrawable.getPadding(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testSetVisible() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);
        assertTrue(scaleDrawable.isVisible());

        assertTrue(scaleDrawable.setVisible(false, false));
        assertFalse(scaleDrawable.isVisible());
        assertTrue(mockDrawable.hasCalledSetVisible());

        mockDrawable.reset();
        assertFalse(scaleDrawable.setVisible(false, false));
        assertFalse(scaleDrawable.isVisible());
        assertTrue(mockDrawable.hasCalledSetVisible());

        mockDrawable.reset();
        assertTrue(scaleDrawable.setVisible(true, false));
        assertTrue(scaleDrawable.isVisible());
        assertTrue(mockDrawable.hasCalledSetVisible());
    }

    public void testSetAlpha() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's setAlpha method.
        scaleDrawable.setAlpha(100);
        assertTrue(mockDrawable.hasCalledSetAlpha());

        mockDrawable.reset();
        scaleDrawable.setAlpha(Integer.MAX_VALUE);
        assertTrue(mockDrawable.hasCalledSetAlpha());

        mockDrawable.reset();
        scaleDrawable.setAlpha(-1);
        assertTrue(mockDrawable.hasCalledSetAlpha());
    }

    public void testSetColorFilter() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's setColorFilter method.
        scaleDrawable.setColorFilter(new ColorFilter());
        assertTrue(mockDrawable.hasCalledSetColorFilter());

        mockDrawable.reset();
        scaleDrawable.setColorFilter(null);
        assertTrue(mockDrawable.hasCalledSetColorFilter());
    }

    public void testGetOpacity() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's getOpacity method.
        scaleDrawable.getOpacity();
        assertTrue(mockDrawable.hasCalledGetOpacity());
    }

    public void testIsStateful() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's isStateful method.
        scaleDrawable.isStateful();
        assertTrue(mockDrawable.hasCalledIsStateful());
    }

    public void testOnStateChange() {
        MockDrawable mockDrawable = new MockDrawable();
        MockScaleDrawable mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.CENTER, 100, 200);

        assertFalse(mockScaleDrawable.onStateChange(StateSet.WILD_CARD));
        assertTrue(mockDrawable.hasCalledSetState());
        assertTrue(mockScaleDrawable.hasCalledOnBoundsChange());

        mockDrawable.reset();
        mockScaleDrawable.reset();
        assertFalse(mockScaleDrawable.onStateChange(null));
        assertTrue(mockDrawable.hasCalledSetState());
        assertTrue(mockScaleDrawable.hasCalledOnBoundsChange());
    }

    public void testOnLevelChange() {
        MockDrawable mockDrawable = new MockDrawable();
        MockScaleDrawable mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.CENTER, 100, 200);

        assertTrue(mockScaleDrawable.onLevelChange(0));
        assertFalse(mockDrawable.hasCalledOnLevelChange());
        assertTrue(mockScaleDrawable.hasCalledOnBoundsChange());

        mockDrawable.reset();
        mockScaleDrawable.reset();
        assertTrue(mockScaleDrawable.onLevelChange(Integer.MIN_VALUE));
        assertTrue(mockDrawable.hasCalledOnLevelChange());
        assertTrue(mockScaleDrawable.hasCalledOnBoundsChange());
    }

    public void testOnBoundsChange() {
        MockDrawable mockDrawable = new MockDrawable();
        float scaleWidth = 0.3f;
        float scaleHeight = 0.3f;
        MockScaleDrawable mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.LEFT, scaleWidth, scaleHeight);
        Rect bounds = new Rect(2, 2, 26, 32);
        mockDrawable.setBounds(bounds);
        mockScaleDrawable.onBoundsChange(bounds);
        Rect expected = new Rect();
        Gravity.apply(Gravity.LEFT, bounds.width() - (int) (bounds.width() * scaleWidth),
                bounds.height() - (int) (bounds.height() * scaleHeight), bounds, expected);
        assertEquals(expected.left, mockDrawable.getBounds().left);
        assertEquals(expected.top, mockDrawable.getBounds().top);
        assertEquals(expected.right, mockDrawable.getBounds().right);
        assertEquals(expected.bottom, mockDrawable.getBounds().bottom);

        scaleWidth = 0.6f;
        scaleHeight = 0.7f;
        int level = 4000;
        mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.BOTTOM | Gravity.RIGHT, scaleWidth, scaleHeight);
        mockDrawable.setBounds(bounds);
        mockScaleDrawable.setLevel(level);
        mockScaleDrawable.onBoundsChange(bounds);
        Gravity.apply(Gravity.BOTTOM | Gravity.RIGHT,
                bounds.width() - (int) (bounds.width() * scaleWidth * (10000 - level) / 10000),
                bounds.height() - (int) (bounds.height() * scaleHeight * (10000 - level) / 10000),
                bounds, expected);
        assertEquals(expected.left, mockDrawable.getBounds().left);
        assertEquals(expected.top, mockDrawable.getBounds().top);
        assertEquals(expected.right, mockDrawable.getBounds().right);
        assertEquals(expected.bottom, mockDrawable.getBounds().bottom);

        scaleWidth = 0f;
        scaleHeight = -0.3f;
        mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.BOTTOM | Gravity.RIGHT, scaleWidth, scaleHeight);
        mockDrawable.setBounds(bounds);
        mockScaleDrawable.onBoundsChange(bounds);
        assertEquals(bounds.left, mockDrawable.getBounds().left);
        assertEquals(bounds.top, mockDrawable.getBounds().top);
        assertEquals(bounds.right, mockDrawable.getBounds().right);
        assertEquals(bounds.bottom, mockDrawable.getBounds().bottom);

        scaleWidth = 1f;
        scaleHeight = 1.7f;
        mockScaleDrawable = new MockScaleDrawable(
                mockDrawable, Gravity.BOTTOM | Gravity.RIGHT, scaleWidth, scaleHeight);
        mockDrawable.setBounds(bounds);
        mockScaleDrawable.onBoundsChange(bounds);
        assertEquals(bounds.left, mockDrawable.getBounds().left);
        assertEquals(bounds.top, mockDrawable.getBounds().top);
        assertEquals(bounds.right, mockDrawable.getBounds().right);
        assertEquals(bounds.bottom, mockDrawable.getBounds().bottom);
    }

    public void testGetIntrinsicWidth() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's getIntrinsicWidth method.
        scaleDrawable.getIntrinsicWidth();
        assertTrue(mockDrawable.hasCalledGetIntrinsicWidth());
    }

    public void testGetIntrinsicHeight() {
        MockDrawable mockDrawable = new MockDrawable();
        ScaleDrawable scaleDrawable = new ScaleDrawable(mockDrawable, Gravity.CENTER, 100, 200);

        // this method will call contained drawable's getIntrinsicHeight method.
        scaleDrawable.getIntrinsicHeight();
        assertTrue(mockDrawable.hasCalledGetIntrinsicHeight());
    }

    @SuppressWarnings("deprecation")
    public void testGetConstantState() {
        ScaleDrawable scaleDrawable = new ScaleDrawable(new BitmapDrawable(),
                Gravity.CENTER, 100, 200);

        ConstantState constantState = scaleDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(0, constantState.getChangingConfigurations());

        scaleDrawable.setChangingConfigurations(1);
        constantState = scaleDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(1, constantState.getChangingConfigurations());
    }

    @SuppressWarnings("deprecation")
    public void testInflate() throws XmlPullParserException, IOException {
        ScaleDrawable scaleDrawable = new ScaleDrawable(new BitmapDrawable(),
                Gravity.RIGHT, 100, 200);

        Resources res = mContext.getResources();
        XmlResourceParser parser = res.getXml(R.xml.scaledrawable);
        AttributeSet attrs = DrawableTestUtils.getAttributeSet(parser, "scale_allattrs");
        scaleDrawable.inflate(res, parser, attrs);
        int bitmapSize = (int) Math.ceil(48.0 *
                res.getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        assertEquals(bitmapSize, scaleDrawable.getIntrinsicWidth());
        assertEquals(bitmapSize, scaleDrawable.getIntrinsicHeight());

        parser = res.getXml(R.xml.scaledrawable);
        attrs = DrawableTestUtils.getAttributeSet(parser, "scale_nodrawable");
        try {
            scaleDrawable.inflate(res, parser, attrs);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            scaleDrawable.inflate(null, parser, attrs);
            fail("Should throw NullPointerException if resource is null");
        } catch (NullPointerException e) {
        }

        try {
            scaleDrawable.inflate(res, null, attrs);
            fail("Should throw NullPointerException if parser is null");
        } catch (NullPointerException e) {
        }

        try {
            scaleDrawable.inflate(res, parser, null);
            fail("Should throw NullPointerException if attribute set is null");
        } catch (NullPointerException e) {
        }
    }

    public void testMutate() {
        Resources resources = mContext.getResources();
        ScaleDrawable d1 = (ScaleDrawable) resources.getDrawable(R.drawable.scaledrawable);
        ScaleDrawable d2 = (ScaleDrawable) resources.getDrawable(R.drawable.scaledrawable);
        ScaleDrawable d3 = (ScaleDrawable) resources.getDrawable(R.drawable.scaledrawable);

        d1.setAlpha(100);
        assertEquals(100, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());

        d1.mutate();
        d1.setAlpha(200);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());

        d2.setAlpha(50);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable()).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d2.getDrawable()).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d3.getDrawable()).getPaint().getAlpha());
    }

    private static class MockDrawable extends Drawable {
        private boolean mCalledDraw = false;
        private boolean mCalledGetPadding = false;
        private boolean mCalledSetVisible = false;
        private boolean mCalledSetAlpha = false;
        private boolean mCalledGetOpacity = false;
        private boolean mCalledSetColorFilter = false;
        private boolean mCalledIsStateful = false;
        private boolean mCalledGetIntrinsicWidth = false;
        private boolean mCalledGetIntrinsicHeight = false;
        private boolean mCalledSetState = false;
        private boolean mCalledOnLevelChange = false;

        @Override
        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        @Override
        public int getOpacity() {
            mCalledGetOpacity = true;
            return 0;
        }

        @Override
        public void setAlpha(int alpha) {
            mCalledSetAlpha = true;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mCalledSetColorFilter = true;
        }

        @Override
        public boolean getPadding(Rect padding) {
            mCalledGetPadding = true;
            return super.getPadding(padding);
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            mCalledSetVisible = true;
            return super.setVisible(visible, restart);
        }

        @Override
        public boolean isStateful() {
            mCalledIsStateful = true;
            return super.isStateful();
        }

        @Override
        public int getIntrinsicWidth() {
            mCalledGetIntrinsicWidth = true;
            return super.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            mCalledGetIntrinsicHeight = true;
            return super.getIntrinsicHeight();

        }

        @Override
        public boolean setState(final int[] stateSet) {
            mCalledSetState = true;
            return super.setState(stateSet);
        }

        @Override
        protected boolean onLevelChange(int level) {
            mCalledOnLevelChange = true;
            return super.onLevelChange(level);
        }

        public boolean hasCalledDraw() {
            return mCalledDraw;
        }

        public boolean hasCalledGetPadding() {
            return mCalledGetPadding;
        }

        public boolean hasCalledSetVisible() {
            return mCalledSetVisible;
        }

        public boolean hasCalledSetAlpha() {
            return mCalledSetAlpha;
        }

        public boolean hasCalledGetOpacity() {
            return mCalledGetOpacity;
        }

        public boolean hasCalledSetColorFilter() {
            return mCalledSetColorFilter;
        }

        public boolean hasCalledIsStateful() {
            return mCalledIsStateful;
        }

        public boolean hasCalledGetIntrinsicWidth() {
            return mCalledGetIntrinsicWidth;
        }

        public boolean hasCalledGetIntrinsicHeight() {
            return mCalledGetIntrinsicHeight;
        }

        public boolean hasCalledSetState() {
            return mCalledSetState;
        }

        public boolean hasCalledOnLevelChange() {
            return mCalledOnLevelChange;
        }

        public void reset() {
            mCalledDraw = false;
            mCalledGetPadding = false;
            mCalledSetVisible = false;
            mCalledSetAlpha = false;
            mCalledGetOpacity = false;
            mCalledSetColorFilter = false;
            mCalledIsStateful = false;
            mCalledGetIntrinsicWidth = false;
            mCalledGetIntrinsicHeight = false;
            mCalledSetState = false;
            mCalledOnLevelChange = false;
        }
    }

    private static class MockScaleDrawable extends ScaleDrawable {
        private boolean mCalledOnBoundsChange = false;

        MockScaleDrawable() {
            super(null, Gravity.CENTER, 100, 200);
        }

        public MockScaleDrawable(Drawable drawable, int gravity,
                float scaleWidth, float scaleHeight) {
            super(drawable, gravity, scaleWidth, scaleHeight);
        }

        @Override
        protected boolean onStateChange(int[] state) {
            return super.onStateChange(state);
        }

        @Override
        protected boolean onLevelChange(int level) {
            return super.onLevelChange(level);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            mCalledOnBoundsChange = true;
            super.onBoundsChange(bounds);
        }

        public boolean hasCalledOnBoundsChange() {
            return mCalledOnBoundsChange;
        }

        public void reset() {
            mCalledOnBoundsChange = false;
        }
    }
}
