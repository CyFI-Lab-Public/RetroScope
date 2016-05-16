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

import com.android.cts.stub.R;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import android.R.attr;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.StateSet;
import android.view.View;

public class LayerDrawableTest extends AndroidTestCase {
    @SuppressWarnings("deprecation")
    public void testConstructor() {
        Drawable bitmapDrawable = new BitmapDrawable();
        Drawable colorDrawable  = new ColorDrawable(Color.BLUE);
        Drawable[] array = new Drawable[] { bitmapDrawable, colorDrawable };
        LayerDrawable layerDrawable = new LayerDrawable(array);
        assertEquals(array.length, layerDrawable.getNumberOfLayers());
        assertSame(bitmapDrawable, layerDrawable.getDrawable(0));
        assertSame(colorDrawable, layerDrawable.getDrawable(1));

        array = new Drawable[0];
        layerDrawable = new LayerDrawable(array);
        assertEquals(0, layerDrawable.getNumberOfLayers());

        try {
            new LayerDrawable((Drawable[]) null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testInflate() throws XmlPullParserException, IOException {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);

        XmlResourceParser parser = mContext.getResources().getXml(R.xml.layerdrawable);
        AttributeSet attrs = DrawableTestUtils.getAttributeSet(parser, "layer-list_full");

        layerDrawable.inflate(mContext.getResources(), parser, attrs);

        assertEquals(4, layerDrawable.getNumberOfLayers());
        assertEquals(ColorDrawable.class, layerDrawable.getDrawable(0).getClass());
        assertEquals(0x88, (((ColorDrawable) layerDrawable.getDrawable(0)).getAlpha()));
        assertEquals(View.NO_ID, layerDrawable.getId(0));
        assertEquals(BitmapDrawable.class, layerDrawable.getDrawable(1).getClass());
        assertEquals(View.NO_ID, layerDrawable.getId(1));
        assertEquals(RotateDrawable.class, layerDrawable.getDrawable(2).getClass());
        assertEquals(View.NO_ID, layerDrawable.getId(2));
        assertEquals(GradientDrawable.class, layerDrawable.getDrawable(3).getClass());
        assertEquals(R.id.background, layerDrawable.getId(3));

        layerDrawable = new LayerDrawable(array);
        parser = mContext.getResources().getXml(R.xml.layerdrawable);
        attrs = DrawableTestUtils.getAttributeSet(parser, "layer-list_empty");
        layerDrawable.inflate(mContext.getResources(), parser, attrs);
        assertEquals(0, layerDrawable.getNumberOfLayers());

        parser = mContext.getResources().getXml(R.xml.layerdrawable);
        attrs = DrawableTestUtils.getAttributeSet(parser, "layer-list_exception");
        try {
            layerDrawable.inflate(mContext.getResources(), parser, attrs);
            fail("Should throw XmlPullParserException if neither 'drawable' attribute" +
                    " nor child tag defining a drawable in <item> tag.");
        } catch (XmlPullParserException e) {
        }

        try {
            layerDrawable.inflate(null, parser, attrs);
            fail("Should throw NullPointerException if resource is null");
        } catch (NullPointerException e) {
        }

        try {
            layerDrawable.inflate(mContext.getResources(), null, attrs);
            fail("Should throw NullPointerException if parser is null");
        } catch (NullPointerException e) {
        }

        try {
            layerDrawable.inflate(mContext.getResources(), parser, null);
            fail("Should throw NullPointerException if attribute set is null");
        } catch (NullPointerException e) {
        }
    }

    @SuppressWarnings("deprecation")
    public void testFindDrawableByLayerId() {
        Drawable bitmapDrawable = new BitmapDrawable();
        Drawable colorDrawable  = new ColorDrawable(Color.BLUE);
        Drawable[] array = new Drawable[] { bitmapDrawable, colorDrawable };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        layerDrawable.setId(0, 10);
        layerDrawable.setId(1, 20);
        assertSame(bitmapDrawable, layerDrawable.findDrawableByLayerId(10));
        assertSame(colorDrawable, layerDrawable.findDrawableByLayerId(20));
        assertNull(layerDrawable.findDrawableByLayerId(30));

        layerDrawable.setId(0, Integer.MIN_VALUE);
        layerDrawable.setId(1, Integer.MAX_VALUE);
        assertSame(bitmapDrawable, layerDrawable.findDrawableByLayerId(Integer.MIN_VALUE));
        assertSame(colorDrawable, layerDrawable.findDrawableByLayerId(Integer.MAX_VALUE));

        layerDrawable.setId(0, 10);
        layerDrawable.setId(1, 10);
        assertSame(colorDrawable, layerDrawable.findDrawableByLayerId(10));
        assertNull(layerDrawable.findDrawableByLayerId(30));
    }

    @SuppressWarnings("deprecation")
    public void testAccessId() {
        Drawable[] array = new Drawable[] { new BitmapDrawable(), new ColorDrawable(Color.BLUE) };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        layerDrawable.setId(0, 10);
        layerDrawable.setId(1, 20);
        assertEquals(10, layerDrawable.getId(0));
        assertEquals(20, layerDrawable.getId(1));

        layerDrawable.setId(0, Integer.MIN_VALUE);
        layerDrawable.setId(1, Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, layerDrawable.getId(0));
        assertEquals(Integer.MAX_VALUE, layerDrawable.getId(1));

        try {
            layerDrawable.setId(-1, 20);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            layerDrawable.setId(layerDrawable.getNumberOfLayers(), 20);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            layerDrawable.getId(-1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            layerDrawable.getId(layerDrawable.getNumberOfLayers());
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @SuppressWarnings("deprecation")
    public void testGetNumberOfLayers() {
        Drawable[] array = new Drawable[] { new BitmapDrawable(), new ColorDrawable(Color.BLUE) };
        LayerDrawable layerDrawable = new LayerDrawable(array);
        assertEquals(2, layerDrawable.getNumberOfLayers());

        array = new Drawable[5];
        for (int i = 0; i < 5; i++) {
            array[i] = new BitmapDrawable();
        }
        layerDrawable = new LayerDrawable(array);
        assertEquals(5, layerDrawable.getNumberOfLayers());

        array = new Drawable[0];
        layerDrawable = new LayerDrawable(array);
        assertEquals(0, layerDrawable.getNumberOfLayers());
    }

    @SuppressWarnings("deprecation")
    public void testAccessDrawable() {
        Drawable bitmapDrawable = new BitmapDrawable();
        Drawable colorDrawable  = new ColorDrawable(Color.BLUE);
        Drawable[] array = new Drawable[] { bitmapDrawable, colorDrawable };
        LayerDrawable layerDrawable = new LayerDrawable(array);
        assertSame(bitmapDrawable, layerDrawable.getDrawable(0));
        assertSame(colorDrawable, layerDrawable.getDrawable(1));

        layerDrawable.setId(0, 10);
        layerDrawable.setId(1, 20);
        Drawable d1 = new ColorDrawable(Color.GREEN);
        Drawable d2 = new BitmapDrawable();
        layerDrawable.setDrawableByLayerId(10, d1);
        layerDrawable.setDrawableByLayerId(20, d2);
        assertEquals(d1, layerDrawable.getDrawable(0));
        assertEquals(d2, layerDrawable.getDrawable(1));

        assertFalse(layerDrawable.setDrawableByLayerId(30, d1));

        try {
            layerDrawable.getDrawable(layerDrawable.getNumberOfLayers());
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            layerDrawable.getDrawable(-1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @SuppressWarnings("deprecation")
    public void testSetLayerInset() {
        Drawable[] array = new Drawable[] { new BitmapDrawable(), new ColorDrawable(Color.BLUE) };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        // set inset for layer 0
        int left = 10;
        int top = 20;
        int right = 30;
        int bottom = 40;
        layerDrawable.setLayerInset(0, left, top, right, bottom);
        assertEquals(layerDrawable.getDrawable(0).getIntrinsicWidth() + left + right,
                layerDrawable.getIntrinsicWidth());
        assertEquals(layerDrawable.getDrawable(0).getIntrinsicHeight() + top + bottom,
                layerDrawable.getIntrinsicHeight());

        // set bigger inset for layer 1
        left += 10;
        top += 10;
        right += 10;
        bottom += 10;
        layerDrawable.setLayerInset(1, left, top, right, bottom);
        assertEquals(layerDrawable.getDrawable(1).getIntrinsicWidth() + left + right,
                layerDrawable.getIntrinsicWidth());
        assertEquals(layerDrawable.getDrawable(1).getIntrinsicHeight() + top + bottom,
                layerDrawable.getIntrinsicHeight());

        try {
            layerDrawable.setLayerInset(-1, left, top, right, bottom);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @SuppressWarnings("deprecation")
    public void testInvalidateDrawable() {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);

        MockCallback cb = new MockCallback();
        layerDrawable.setCallback(cb);
        layerDrawable.invalidateDrawable(null);
        assertTrue(cb.hasCalledInvalidate());

        cb.reset();
        layerDrawable.invalidateDrawable(new BitmapDrawable());
        assertTrue(cb.hasCalledInvalidate());

        cb.reset();
        layerDrawable.setCallback(null);
        layerDrawable.invalidateDrawable(null);
        assertFalse(cb.hasCalledInvalidate());
    }

    @SuppressWarnings("deprecation")
    public void testScheduleDrawable() {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);

        MockCallback cb = new MockCallback();
        layerDrawable.setCallback(cb);
        layerDrawable.scheduleDrawable(null, null, 0);
        assertTrue(cb.hasCalledSchedule());

        cb.reset();
        layerDrawable.scheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        }, 1000L);
        assertTrue(cb.hasCalledSchedule());

        cb.reset();
        layerDrawable.setCallback(null);
        layerDrawable.scheduleDrawable(null, null, 0);
        assertFalse(cb.hasCalledSchedule());
    }

    @SuppressWarnings("deprecation")
    public void testUnscheduleDrawable() {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);

        MockCallback cb = new MockCallback();
        layerDrawable.setCallback(cb);
        layerDrawable.unscheduleDrawable(null, null);
        assertTrue(cb.hasCalledUnschedule());

        cb.reset();
        layerDrawable.unscheduleDrawable(new BitmapDrawable(), new Runnable() {
            public void run() {
            }
        });
        assertTrue(cb.hasCalledUnschedule());

        cb.reset();
        layerDrawable.setCallback(null);
        layerDrawable.unscheduleDrawable(null, null);
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
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        // this method will call each child's draw().
        layerDrawable.draw(new Canvas());
        assertTrue(mockDrawable1.hasCalledDraw());
        assertTrue(mockDrawable2.hasCalledDraw());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.draw(null);
        assertTrue(mockDrawable1.hasCalledDraw());
        assertTrue(mockDrawable2.hasCalledDraw());
    }

    @SuppressWarnings("deprecation")
    public void testGetChangingConfigurations() {
        final int superConfig = 1;
        final int bitmapDrawableConfig = 2;
        final int colorDrawableConfig = 4;
        final int childConfig = bitmapDrawableConfig | colorDrawableConfig;

        BitmapDrawable bitmapDrawable = new BitmapDrawable();
        bitmapDrawable.setChangingConfigurations(bitmapDrawableConfig);
        ColorDrawable colorDrawable = new ColorDrawable(Color.BLUE);
        colorDrawable.setChangingConfigurations(colorDrawableConfig);
        Drawable[] array = new Drawable[] { bitmapDrawable, colorDrawable };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        assertEquals(childConfig, layerDrawable.getChangingConfigurations());

        layerDrawable.setChangingConfigurations(superConfig);
        assertEquals(superConfig | childConfig, layerDrawable.getChangingConfigurations());
    }

    public void testGetPadding() {
        Drawable[] array = new Drawable[] { new ShapeDrawable(), new ShapeDrawable() };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        Rect rc = new Rect();
        layerDrawable.getPadding(rc);
        assertEquals(0, rc.left);
        assertEquals(0, rc.top);
        assertEquals(0, rc.right);
        assertEquals(0, rc.bottom);

        Rect padding0 = new Rect(10, 20, 30, 40);
        ((ShapeDrawable) layerDrawable.getDrawable(0)).setPadding(padding0);
        layerDrawable.getPadding(rc);
        assertEquals(padding0.left, rc.left);
        assertEquals(padding0.top, rc.top);
        assertEquals(padding0.right, rc.right);
        assertEquals(padding0.bottom, rc.bottom);

        Rect padding1 = new Rect(20, 30, 40, 50);
        ((ShapeDrawable) layerDrawable.getDrawable(1)).setPadding(padding1);
        layerDrawable.getPadding(rc);
        assertEquals(padding0.left + padding1.left, rc.left);
        assertEquals(padding0.top + padding1.top, rc.top);
        assertEquals(padding0.right + padding1.right, rc.right);
        assertEquals(padding0.bottom + padding1.bottom, rc.bottom);
    }

    @SuppressWarnings("deprecation")
    public void testSetVisible() {
        Drawable[] array = new Drawable[] { new BitmapDrawable(), new ColorDrawable(Color.BLUE) };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        assertTrue(layerDrawable.setVisible(false, true));
        assertFalse(layerDrawable.isVisible());
        assertFalse(layerDrawable.getDrawable(0).isVisible());
        assertFalse(layerDrawable.getDrawable(1).isVisible());

        assertFalse(layerDrawable.setVisible(false, false));

        assertTrue(layerDrawable.setVisible(true, false));
        assertTrue(layerDrawable.isVisible());
        assertTrue(layerDrawable.getDrawable(0).isVisible());
        assertTrue(layerDrawable.getDrawable(1).isVisible());
    }

    public void testSetDither() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        layerDrawable.setDither(true);
        assertTrue(mockDrawable1.hasCalledSetDither());
        assertTrue(mockDrawable2.hasCalledSetDither());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.setDither(false);
        assertTrue(mockDrawable1.hasCalledSetDither());
        assertTrue(mockDrawable2.hasCalledSetDither());
    }

    public void testSetAlpha() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        layerDrawable.setAlpha(0);
        assertTrue(mockDrawable1.hasCalledSetAlpha());
        assertTrue(mockDrawable2.hasCalledSetAlpha());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.setAlpha(Integer.MAX_VALUE);
        assertTrue(mockDrawable1.hasCalledSetAlpha());
        assertTrue(mockDrawable2.hasCalledSetAlpha());
    }

    public void testSetColorFilter() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        LayerDrawable layerDrawable = new LayerDrawable(array);

        layerDrawable.setColorFilter(new ColorFilter());
        assertTrue(mockDrawable1.hasCalledColorFilter());
        assertTrue(mockDrawable2.hasCalledColorFilter());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.setColorFilter(null);
        assertTrue(mockDrawable1.hasCalledColorFilter());
        assertTrue(mockDrawable2.hasCalledColorFilter());
    }

    public void testGetOpacity() {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);
        assertEquals(PixelFormat.TRANSPARENT, layerDrawable.getOpacity());

        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        array = new Drawable[] { mockDrawable1, mockDrawable2 };
        layerDrawable = new LayerDrawable(array);
        assertEquals(PixelFormat.OPAQUE, layerDrawable.getOpacity());

        layerDrawable = new LayerDrawable(array);
        mockDrawable2.setOpacity(PixelFormat.TRANSPARENT);
        assertEquals(PixelFormat.TRANSPARENT, layerDrawable.getOpacity());

        layerDrawable = new LayerDrawable(array);
        mockDrawable2.setOpacity(PixelFormat.TRANSPARENT);
        mockDrawable1.setOpacity(PixelFormat.TRANSLUCENT);
        assertEquals(PixelFormat.TRANSLUCENT, layerDrawable.getOpacity());

        layerDrawable = new LayerDrawable(array);
        mockDrawable1.setOpacity(PixelFormat.TRANSLUCENT);
        mockDrawable2.setOpacity(PixelFormat.UNKNOWN);
        assertEquals(PixelFormat.UNKNOWN, layerDrawable.getOpacity());
    }

    @SuppressWarnings("deprecation")
    public void testIsStateful() {
        Drawable[] array = new Drawable[0];
        LayerDrawable layerDrawable = new LayerDrawable(array);
        assertFalse(layerDrawable.isStateful());

        array = new Drawable[] { new BitmapDrawable(), new MockDrawable() };
        layerDrawable = new LayerDrawable(array);
        assertFalse(layerDrawable.isStateful());

        array = new Drawable[] { new BitmapDrawable(), new StateListDrawable() };
        layerDrawable = new LayerDrawable(array);
        assertTrue(layerDrawable.isStateful());
    }

    public void testOnStateChange() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        MockLayerDrawable layerDrawable = new MockLayerDrawable(array);

        // this method will call each child's setState().
        assertFalse(layerDrawable.onStateChange(StateSet.WILD_CARD));
        assertTrue(mockDrawable1.hasCalledSetState());
        assertTrue(mockDrawable2.hasCalledSetState());
        assertTrue(layerDrawable.hasCalledOnBoundsChange());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.reset();
        assertTrue(layerDrawable.onStateChange(null));
        assertTrue(mockDrawable1.hasCalledSetState());
        assertTrue(mockDrawable2.hasCalledSetState());
        assertTrue(layerDrawable.hasCalledOnBoundsChange());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.reset();
        assertTrue(layerDrawable.onStateChange(new int[] { attr.state_checked, attr.state_empty }));
        assertTrue(mockDrawable1.hasCalledSetState());
        assertTrue(mockDrawable2.hasCalledSetState());
        assertTrue(layerDrawable.hasCalledOnBoundsChange());
    }

    public void testOnLevelChange() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        MockLayerDrawable layerDrawable = new MockLayerDrawable(array);

        // this method will call each child's setLevel(),
        // but just when set a different level the child's onLevelChange will be called.
        assertFalse(layerDrawable.onLevelChange(0));
        assertFalse(mockDrawable1.hasCalledOnLevelChange());
        assertFalse(mockDrawable2.hasCalledOnLevelChange());
        assertFalse(layerDrawable.hasCalledOnBoundsChange());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.reset();
        assertTrue(layerDrawable.onLevelChange(Integer.MAX_VALUE));
        assertTrue(mockDrawable1.hasCalledOnLevelChange());
        assertTrue(mockDrawable2.hasCalledOnLevelChange());
        assertTrue(layerDrawable.hasCalledOnBoundsChange());

        mockDrawable1.reset();
        mockDrawable2.reset();
        layerDrawable.reset();
        assertTrue(layerDrawable.onLevelChange(Integer.MIN_VALUE));
        assertTrue(mockDrawable1.hasCalledOnLevelChange());
        assertTrue(mockDrawable2.hasCalledOnLevelChange());
        assertTrue(layerDrawable.hasCalledOnBoundsChange());
    }

    public void testOnBoundsChange() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        MockLayerDrawable layerDrawable = new MockLayerDrawable(array);

        Rect inset1 = new Rect(1, 2, 3, 4);
        Rect inset2 = new Rect(2, 4, 6, 7);
        Rect padding1 = new Rect(11, 22, 33, 44);
        Rect padding2 = new Rect(21, 32, 43, 54);
        layerDrawable.setLayerInset(0, inset1.left, inset1.top, inset1.right, inset1.bottom);
        layerDrawable.setLayerInset(1, inset2.left, inset2.top, inset2.right, inset2.bottom);
        mockDrawable1.setPadding(padding1);
        mockDrawable2.setPadding(padding2);
        layerDrawable.getPadding(new Rect());

        // the children's bounds before call onBoundsChange
        assertEquals(0, mockDrawable1.getBounds().left);
        assertEquals(0, mockDrawable1.getBounds().top);
        assertEquals(0, mockDrawable1.getBounds().right);
        assertEquals(0, mockDrawable1.getBounds().bottom);
        assertEquals(0, mockDrawable2.getBounds().left);
        assertEquals(0, mockDrawable2.getBounds().top);
        assertEquals(0, mockDrawable2.getBounds().right);
        assertEquals(0, mockDrawable2.getBounds().bottom);

        Rect bounds = new Rect(10, 20, 30, 40);
        layerDrawable.onBoundsChange(bounds);

        // all children's bounds will be changed after call onBoundsChange
        assertEquals(bounds.left + inset1.left, mockDrawable1.getBounds().left);
        assertEquals(bounds.top + inset1.top, mockDrawable1.getBounds().top);
        assertEquals(bounds.right - inset1.right, mockDrawable1.getBounds().right);
        assertEquals(bounds.bottom - inset1.bottom, mockDrawable1.getBounds().bottom);
        assertEquals(bounds.left + inset2.left + padding1.left, mockDrawable2.getBounds().left);
        assertEquals(bounds.top + inset2.top + padding1.top, mockDrawable2.getBounds().top);
        assertEquals(bounds.right - inset2.right - padding1.right,
                mockDrawable2.getBounds().right);
        assertEquals(bounds.bottom - inset2.bottom - padding1.bottom,
                mockDrawable2.getBounds().bottom);
    }

    public void testGetIntrinsicWidth() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        MockLayerDrawable layerDrawable = new MockLayerDrawable(array);
        assertEquals(mockDrawable1.getIntrinsicWidth(), layerDrawable.getIntrinsicWidth());

        Rect inset1 = new Rect(1, 2, 3, 4);
        Rect inset2 = new Rect(2, 4, 6, 7);
        Rect padding1 = new Rect(11, 22, 33, 44);
        Rect padding2 = new Rect(21, 32, 43, 54);
        layerDrawable.setLayerInset(0, inset1.left, inset1.top, inset1.right, inset1.bottom);
        layerDrawable.setLayerInset(1, inset2.left, inset2.top, inset2.right, inset2.bottom);
        mockDrawable1.setPadding(padding1);
        mockDrawable2.setPadding(padding2);
        layerDrawable.getPadding(new Rect());
        assertEquals(mockDrawable2.getIntrinsicWidth() + inset2.left
                + inset2.right + padding1.left + padding1.right,
                layerDrawable.getIntrinsicWidth());

        inset1 = new Rect(inset2.left + padding1.left + 1, inset2.top + padding1.top + 1,
                inset2.right + padding1.right + 1, inset2.bottom + padding1.bottom + 1);
        layerDrawable.setLayerInset(0, inset1.left, inset1.top, inset1.right, inset1.bottom);
        assertEquals(mockDrawable1.getIntrinsicWidth() + inset1.left + inset1.right,
                layerDrawable.getIntrinsicWidth());
    }

    public void testGetIntrinsicHeight() {
        MockDrawable mockDrawable1 = new MockDrawable();
        MockDrawable mockDrawable2 = new MockDrawable();
        Drawable[] array = new Drawable[] { mockDrawable1, mockDrawable2 };
        MockLayerDrawable layerDrawable = new MockLayerDrawable(array);
        assertEquals(mockDrawable1.getIntrinsicHeight(), layerDrawable.getIntrinsicHeight());

        Rect inset1 = new Rect(1, 2, 3, 4);
        Rect inset2 = new Rect(2, 4, 6, 7);
        Rect padding1 = new Rect(11, 22, 33, 44);
        Rect padding2 = new Rect(21, 32, 43, 54);
        layerDrawable.setLayerInset(0, inset1.left, inset1.top, inset1.right, inset1.bottom);
        layerDrawable.setLayerInset(1, inset2.left, inset2.top, inset2.right, inset2.bottom);
        mockDrawable1.setPadding(padding1);
        mockDrawable2.setPadding(padding2);
        layerDrawable.getPadding(new Rect());
        assertEquals(mockDrawable2.getIntrinsicHeight() + inset2.top
                + inset2.bottom + padding1.top + padding1.bottom,
                layerDrawable.getIntrinsicHeight());

        inset1 = new Rect(inset2.left + padding1.left + 1, inset2.top + padding1.top + 1,
                inset2.right + padding1.right + 1, inset2.bottom + padding1.bottom + 1);
        layerDrawable.setLayerInset(0, inset1.left, inset1.top, inset1.right, inset1.bottom);
        assertEquals(mockDrawable1.getIntrinsicHeight() + inset1.top + inset1.bottom,
                layerDrawable.getIntrinsicHeight());
    }

    @SuppressWarnings("deprecation")
    public void testGetConstantState() {
        Drawable[] array = new Drawable[] { new BitmapDrawable(), new ColorDrawable(Color.BLUE) };
        LayerDrawable layerDrawable = new LayerDrawable(array);
        ConstantState constantState = layerDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(0, constantState.getChangingConfigurations());

        layerDrawable.setChangingConfigurations(1);
        constantState = layerDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(1, constantState.getChangingConfigurations());
    }

    private static class MockDrawable extends Drawable {
        private boolean mCalledSetDither = false;
        private boolean mCalledSetAlpha = false;
        private boolean mCalledColorFilter = false;

        private boolean mCalledSetState = false;
        private boolean mCalledOnLevelChange = false;

        private boolean mCalledDraw = false;

        private int mOpacity = PixelFormat.OPAQUE;

        Rect mPadding = null;

        @Override
        public void draw(Canvas canvas) {
            mCalledDraw = true;
        }

        public boolean hasCalledDraw() {
            return mCalledDraw;
        }

        @Override
        public int getOpacity() {
            return mOpacity;
        }

        public void setOpacity(int opacity) {
            mOpacity = opacity;
        }

        @Override
        public void setAlpha(int alpha) {
            mCalledSetAlpha = true;
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            mCalledColorFilter = true;
        }

        @Override
        public void setDither(boolean dither) {
            mCalledSetDither = true;
        }

        public boolean hasCalledSetDither() {
            return mCalledSetDither;
        }

        public boolean hasCalledSetAlpha() {
            return mCalledSetAlpha;
        }

        public boolean hasCalledColorFilter() {
            return mCalledColorFilter;
        }

        public void reset() {
            mCalledSetDither = false;
            mCalledSetAlpha = false;
            mCalledColorFilter = false;

            mCalledSetState = false;
            mCalledOnLevelChange = false;

            mCalledDraw = false;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            return true;
        }

        private void increasePadding() {
            Rect padding = new Rect();
            getPadding(padding);
            padding.left++;
            padding.top++;
            padding.right++;
            padding.bottom++;

            setPadding(padding);
        }

        @Override
        protected boolean onLevelChange(int level) {
            increasePadding();
            mCalledOnLevelChange = true;
            return true;
        }

        public boolean hasCalledSetState() {
            return mCalledSetState;
        }

        public boolean setState(final int[] stateSet) {
            increasePadding();
            mCalledSetState = true;
            return super.setState(stateSet);
        }

        public boolean hasCalledOnLevelChange() {
            return mCalledOnLevelChange;
        }

        public void setPadding(Rect padding) {
            if (padding == null) {
                mPadding = null;
            } else {
                if (mPadding == null) {
                    mPadding = new Rect();
                }
                mPadding.set(padding);
            }
        }

        @Override
        public boolean getPadding(Rect padding) {
            if (mPadding != null) {
                padding.set(mPadding);
                return true;
            } else {
                return super.getPadding(padding);
            }
        }
    }

    private static class MockLayerDrawable extends LayerDrawable {
        private boolean mCalledOnBoundsChange = false;

        public MockLayerDrawable(Drawable[] array) {
            super(array);
        }

        // override protected methods
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

    public void testMutate() {
        Resources resources = mContext.getResources();
        LayerDrawable d1 = (LayerDrawable) resources.getDrawable(R.drawable.layerdrawable);
        LayerDrawable d2 = (LayerDrawable) resources.getDrawable(R.drawable.layerdrawable);
        LayerDrawable d3 = (LayerDrawable) resources.getDrawable(R.drawable.layerdrawable);

        d1.setAlpha(100);
        assertEquals(100, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());

        d1.mutate();
        d1.setAlpha(200);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(200, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable(0)).getPaint().getAlpha());
        assertEquals(100, ((BitmapDrawable) d3.getDrawable(0)).getPaint().getAlpha());

        d2.setAlpha(50);
        assertEquals(200, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(200, ((BitmapDrawable) d1.getDrawable(0)).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d2.getDrawable(0)).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d3.getDrawable(0)).getPaint().getAlpha());
        assertEquals(50, ((BitmapDrawable) d3.getDrawable(0)).getPaint().getAlpha());
    }
}
