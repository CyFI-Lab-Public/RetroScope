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

import dalvik.annotation.KnownFailure;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.InstrumentationTestCase;
import android.util.AttributeSet;
import android.util.Xml;

import java.io.IOException;

public class NinePatchDrawableTest extends InstrumentationTestCase {
    private static final int MIN_CHUNK_SIZE = 32;

    private NinePatchDrawable mNinePatchDrawable;

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = getInstrumentation().getTargetContext().getResources();
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_0);
    }

    @SuppressWarnings("deprecation")
    public void testConstructors() {
        byte[] chunk = new byte[MIN_CHUNK_SIZE];
        chunk[MIN_CHUNK_SIZE - 1] = 1;

        Rect r = new Rect();

        Bitmap bmp = BitmapFactory.decodeResource(mResources, R.drawable.ninepatch_0);
        String name = mResources.getResourceName(R.drawable.ninepatch_0);

        new NinePatchDrawable(bmp, chunk, r, name);

        new NinePatchDrawable(new NinePatch(bmp, chunk, name));

        chunk = new byte[MIN_CHUNK_SIZE - 1];
        chunk[MIN_CHUNK_SIZE - 2] = 1;
        try {
            new NinePatchDrawable(bmp, chunk, r, name);
            fail("The constructor should check whether the chunk is illegal.");
        } catch (RuntimeException e) {
            // This exception is thrown by native method.
        }
    }

    public void testDraw() {
        Bitmap bmp = Bitmap.createBitmap(9, 9, Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        int ocean = Color.rgb(0, 0xFF, 0x80);

        mNinePatchDrawable.setBounds(0, 0, 9, 9);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 4, 4, Color.RED);
        assertColorFillRect(bmp, 5, 0, 4, 4, Color.BLUE);
        assertColorFillRect(bmp, 0, 5, 4, 4, ocean);
        assertColorFillRect(bmp, 5, 5, 4, 4, Color.YELLOW);
        assertColorFillRect(bmp, 4, 0, 1, 9, Color.WHITE);
        assertColorFillRect(bmp, 0, 4, 9, 1, Color.WHITE);

        bmp.eraseColor(0xff000000);

        mNinePatchDrawable.setBounds(0, 0, 3, 3);
        mNinePatchDrawable.draw(c);
        assertColorFillRect(bmp, 0, 0, 1, 1, Color.RED);
        assertColorFillRect(bmp, 2, 0, 1, 1, Color.BLUE);
        assertColorFillRect(bmp, 0, 2, 1, 1, ocean);
        assertColorFillRect(bmp, 2, 2, 1, 1, Color.YELLOW);
        assertColorFillRect(bmp, 1, 0, 1, 3, Color.WHITE);
        assertColorFillRect(bmp, 0, 1, 3, 1, Color.WHITE);

        try {
            mNinePatchDrawable.draw(null);
            fail("The method should check whether the canvas is null.");
        } catch (NullPointerException e) {
            // expected
        }
    }

    public void testGetChangingConfigurations() {
        ConstantState constantState = mNinePatchDrawable.getConstantState();

        // default
        assertEquals(0, constantState.getChangingConfigurations());
        assertEquals(0, mNinePatchDrawable.getChangingConfigurations());

        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0xff, mNinePatchDrawable.getChangingConfigurations());
        assertEquals(0, constantState.getChangingConfigurations());

        // the state's configuration get refreshed
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff,  constantState.getChangingConfigurations());

        // set a new configuration to drawable
        mNinePatchDrawable.setChangingConfigurations(0xff00);
        assertEquals(0xff,  constantState.getChangingConfigurations());
        assertEquals(0xffff,  mNinePatchDrawable.getChangingConfigurations());
    }

    public void testGetPadding() {
        Rect r = new Rect();
        NinePatchDrawable npd = (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatch_0);
        assertTrue(npd.getPadding(r));
        // exact padding unknown due to possible density scaling
        assertEquals(0, r.left);
        assertEquals(0, r.top);
        assertTrue(r.right > 0);
        assertTrue(r.bottom > 0);

        npd = (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatch_1);
        assertTrue(npd.getPadding(r));
        assertTrue(r.left > 0);
        assertTrue(r.top > 0);
        assertTrue(r.right > 0);
        assertTrue(r.bottom > 0);
    }

    public void testSetAlpha() {
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0);
        assertEquals(0, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(-1);
        assertEquals(0xff, mNinePatchDrawable.getPaint().getAlpha());

        mNinePatchDrawable.setAlpha(0xfffe);
        assertEquals(0xfe, mNinePatchDrawable.getPaint().getAlpha());
    }

    public void testSetColorFilter() {
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());

        MockColorFilter cf = new MockColorFilter();
        mNinePatchDrawable.setColorFilter(cf);
        assertSame(cf, mNinePatchDrawable.getPaint().getColorFilter());

        mNinePatchDrawable.setColorFilter(null);
        assertNull(mNinePatchDrawable.getPaint().getColorFilter());
    }

    public void testSetDither() {
        mNinePatchDrawable.setDither(false);
        assertFalse(mNinePatchDrawable.getPaint().isDither());

        mNinePatchDrawable.setDither(true);
        assertTrue(mNinePatchDrawable.getPaint().isDither());
    }

    public void testGetPaint() {
        Paint paint = mNinePatchDrawable.getPaint();
        assertNotNull(paint);

        assertSame(paint, mNinePatchDrawable.getPaint());
    }

    public void testGetIntrinsicWidth() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(5, mNinePatchDrawable.getIntrinsicWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getIntrinsicWidth());
        assertEquals(9, mNinePatchDrawable.getIntrinsicWidth());
    }

    public void testGetMinimumWidth() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(5, mNinePatchDrawable.getMinimumWidth());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getWidth(), mNinePatchDrawable.getMinimumWidth());
        assertEquals(9, mNinePatchDrawable.getMinimumWidth());
    }

    public void testGetIntrinsicHeight() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(5, mNinePatchDrawable.getIntrinsicHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getIntrinsicHeight());
        assertEquals(9, mNinePatchDrawable.getIntrinsicHeight());
    }

    public void testGetMinimumHeight() {
        Bitmap bmp = getBitmapUnscaled(R.drawable.ninepatch_0);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(5, mNinePatchDrawable.getMinimumHeight());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        bmp = getBitmapUnscaled(R.drawable.ninepatch_1);
        assertEquals(bmp.getHeight(), mNinePatchDrawable.getMinimumHeight());
        assertEquals(9, mNinePatchDrawable.getMinimumHeight());
    }

    @KnownFailure("Bug 2834281 - Bitmap#hasAlpha seems to return true for "
        + "images without alpha.")
    public void testGetOpacity() {
        assertEquals(PixelFormat.OPAQUE, mNinePatchDrawable.getOpacity());

        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        assertEquals(PixelFormat.TRANSLUCENT, mNinePatchDrawable.getOpacity());
    }

    public void testGetTransparentRegion() {
        // opaque image
        Region r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(0, 0, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        // translucent image
        mNinePatchDrawable = getNinePatchDrawable(R.drawable.ninepatch_1);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNull(r);

        mNinePatchDrawable.setBounds(1, 1, 7, 7);
        r = mNinePatchDrawable.getTransparentRegion();
        assertNotNull(r);
        assertEquals(new Rect(1, 1, 7, 7), r.getBounds());
    }

    public void testGetConstantState() {
        assertNotNull(mNinePatchDrawable.getConstantState());

        ConstantState constantState = mNinePatchDrawable.getConstantState();
        // change the drawable's configuration does not affect the state's configuration
        mNinePatchDrawable.setChangingConfigurations(0xff);
        assertEquals(0, constantState.getChangingConfigurations());
        // the state's configuration refreshed when getConstantState is called.
        constantState = mNinePatchDrawable.getConstantState();
        assertEquals(0xff, constantState.getChangingConfigurations());
    }

    public void testInflate() throws XmlPullParserException, IOException {
        final int width = 80;
        final int height = 120;
        final int[] COLOR = new int[width * height];
        Bitmap bitmap = Bitmap.createBitmap(COLOR, width, height, Bitmap.Config.RGB_565);
        NinePatchDrawable ninePatchDrawable =
            new NinePatchDrawable(mResources, bitmap, new byte[1000], null, "TESTNAME");

        assertEquals(height, ninePatchDrawable.getIntrinsicHeight());
        assertEquals(width, ninePatchDrawable.getIntrinsicWidth());
        XmlResourceParser parser = mResources.getXml(R.drawable.ninepatchdrawable);
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && type != XmlPullParser.START_TAG) {
        }
        AttributeSet attrs = Xml.asAttributeSet(parser);
        ninePatchDrawable.inflate(mResources, parser, attrs);

        assertTrue(ninePatchDrawable.getPaint().isDither());
        assertTrue(height != ninePatchDrawable.getIntrinsicHeight());
        assertTrue(width != ninePatchDrawable.getIntrinsicWidth());
    }

    public void testMutate() {
        NinePatchDrawable d1 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);
        NinePatchDrawable d2 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);
        NinePatchDrawable d3 =
            (NinePatchDrawable) mResources.getDrawable(R.drawable.ninepatchdrawable);

        // the state is not shared before mutate.
        d1.setDither(false);
        assertFalse(d1.getPaint().isDither());
        assertTrue(d2.getPaint().isDither());
        assertTrue(d3.getPaint().isDither());

        // cannot test if mutate worked, since state was not shared before
        d1.mutate();
    }

    private void assertColorFillRect(Bitmap bmp, int x, int y, int w, int h, int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                assertEquals(color, bmp.getPixel(i, j));
            }
        }
    }

    private NinePatchDrawable getNinePatchDrawable(int resId) {
        // jump through hoops to avoid scaling the tiny ninepatch, which would skew the results
        // depending on device density
        Bitmap bitmap = getBitmapUnscaled(resId);
        NinePatch np = new NinePatch(bitmap, bitmap.getNinePatchChunk(), null);
        return new NinePatchDrawable(mResources, np);
    }

    private Bitmap getBitmapUnscaled(int resId) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inDensity = opts.inTargetDensity = mResources.getDisplayMetrics().densityDpi;
        Bitmap bitmap = BitmapFactory.decodeResource(mResources, resId, opts);
        return bitmap;
    }

    private class MockColorFilter extends ColorFilter {
    }
}
