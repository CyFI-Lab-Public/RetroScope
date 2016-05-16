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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.graphics.Bitmap.Config;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.test.InstrumentationTestCase;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapDrawableTest extends InstrumentationTestCase {
    // The target context.
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getInstrumentation().getTargetContext();
    }

    @SuppressWarnings("deprecation")
    public void testConstructor() {
        // TODO: should default paint flags be left as an untested implementation detail?
        final int defaultPaintFlags = Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG |
            Paint.DEV_KERN_TEXT_FLAG | Paint.EMBEDDED_BITMAP_TEXT_FLAG;
        BitmapDrawable bitmapDrawable = new BitmapDrawable();
        assertNotNull(bitmapDrawable.getPaint());
        assertEquals(defaultPaintFlags,
                bitmapDrawable.getPaint().getFlags());
        assertNull(bitmapDrawable.getBitmap());

        Bitmap bitmap = Bitmap.createBitmap(200, 300, Config.ARGB_8888);
        bitmapDrawable = new BitmapDrawable(bitmap);
        assertNotNull(bitmapDrawable.getPaint());
        assertEquals(defaultPaintFlags,
                bitmapDrawable.getPaint().getFlags());
        assertEquals(bitmap, bitmapDrawable.getBitmap());

        new BitmapDrawable(mContext.getResources());

        new BitmapDrawable(mContext.getResources(), bitmap);

        new BitmapDrawable(mContext.getFilesDir().getPath());

        new BitmapDrawable(new ByteArrayInputStream("test constructor".getBytes()));

        // exceptional test
        new BitmapDrawable((Bitmap) null);

        new BitmapDrawable(mContext.getResources(), (String) null);

        new BitmapDrawable((String) null);

        new BitmapDrawable(mContext.getResources(), (InputStream) null);

        new BitmapDrawable((InputStream) null);
    }

    public void testAccessGravity() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertEquals(Gravity.FILL, bitmapDrawable.getGravity());

        bitmapDrawable.setGravity(Gravity.CENTER);
        assertEquals(Gravity.CENTER, bitmapDrawable.getGravity());

        bitmapDrawable.setGravity(-1);
        assertEquals(-1, bitmapDrawable.getGravity());

        bitmapDrawable.setGravity(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, bitmapDrawable.getGravity());
    }

    public void testSetAntiAlias() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertFalse(bitmapDrawable.getPaint().isAntiAlias());

        bitmapDrawable.setAntiAlias(true);
        assertTrue(bitmapDrawable.getPaint().isAntiAlias());

        bitmapDrawable.setAntiAlias(false);
        assertFalse(bitmapDrawable.getPaint().isAntiAlias());
    }

    public void testSetFilterBitmap() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertTrue(bitmapDrawable.getPaint().isFilterBitmap());

        bitmapDrawable.setFilterBitmap(false);
        assertFalse(bitmapDrawable.getPaint().isFilterBitmap());

        bitmapDrawable.setFilterBitmap(true);
        assertTrue(bitmapDrawable.getPaint().isFilterBitmap());
    }

    public void testSetDither() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertTrue(bitmapDrawable.getPaint().isDither());

        bitmapDrawable.setDither(false);
        assertFalse(bitmapDrawable.getPaint().isDither());

        bitmapDrawable.setDither(true);
        assertTrue(bitmapDrawable.getPaint().isDither());

    }

    public void testAccessTileMode() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertNull(bitmapDrawable.getTileModeX());
        assertNull(bitmapDrawable.getTileModeY());
        assertNull(bitmapDrawable.getPaint().getShader());

        bitmapDrawable.setTileModeX(TileMode.CLAMP);
        assertEquals(TileMode.CLAMP, bitmapDrawable.getTileModeX());
        assertNull(bitmapDrawable.getTileModeY());

        bitmapDrawable.draw(new Canvas());
        assertNotNull(bitmapDrawable.getPaint().getShader());
        Shader oldShader = bitmapDrawable.getPaint().getShader();

        bitmapDrawable.setTileModeY(TileMode.REPEAT);
        assertEquals(TileMode.CLAMP, bitmapDrawable.getTileModeX());
        assertEquals(TileMode.REPEAT, bitmapDrawable.getTileModeY());

        bitmapDrawable.draw(new Canvas());
        assertNotSame(oldShader, bitmapDrawable.getPaint().getShader());
        oldShader = bitmapDrawable.getPaint().getShader();

        bitmapDrawable.setTileModeXY(TileMode.REPEAT, TileMode.MIRROR);
        assertEquals(TileMode.REPEAT, bitmapDrawable.getTileModeX());
        assertEquals(TileMode.MIRROR, bitmapDrawable.getTileModeY());

        bitmapDrawable.draw(new Canvas());
        assertNotSame(oldShader, bitmapDrawable.getPaint().getShader());
        oldShader = bitmapDrawable.getPaint().getShader();

        bitmapDrawable.setTileModeX(TileMode.MIRROR);
        assertEquals(TileMode.MIRROR, bitmapDrawable.getTileModeX());
        assertEquals(TileMode.MIRROR, bitmapDrawable.getTileModeY());

        bitmapDrawable.draw(new Canvas());
        assertNotSame(oldShader, bitmapDrawable.getPaint().getShader());
    }

    public void testGetChangingConfigurations() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertEquals(0, bitmapDrawable.getChangingConfigurations());

        bitmapDrawable.setChangingConfigurations(1);
        assertEquals(1, bitmapDrawable.getChangingConfigurations());

        bitmapDrawable.setChangingConfigurations(2);
        assertEquals(2, bitmapDrawable.getChangingConfigurations());
    }

    public void testOnBoundsChange() {
        // Do not test this API. it is callbacks which:
        // 1. The callback machanism has been tested in super class
        // 2. The functionality is implmentation details, no need to test
    }

    public void testSetAlpha() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertEquals(255, bitmapDrawable.getPaint().getAlpha());

        bitmapDrawable.setAlpha(0);
        assertEquals(0, bitmapDrawable.getPaint().getAlpha());

        bitmapDrawable.setAlpha(100);
        assertEquals(100, bitmapDrawable.getPaint().getAlpha());

        // exceptional test
        bitmapDrawable.setAlpha(-1);
        assertEquals(255, bitmapDrawable.getPaint().getAlpha());

        bitmapDrawable.setAlpha(256);
        assertEquals(0, bitmapDrawable.getPaint().getAlpha());
    }

    public void testSetColorFilter() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        assertNull(bitmapDrawable.getPaint().getColorFilter());

        ColorFilter colorFilter = new ColorFilter();
        bitmapDrawable.setColorFilter(colorFilter);
        assertSame(colorFilter, bitmapDrawable.getPaint().getColorFilter());

        bitmapDrawable.setColorFilter(null);
        assertNull(bitmapDrawable.getPaint().getColorFilter());
    }

    public void testGetOpacity() {
        BitmapDrawable bitmapDrawable = new BitmapDrawable();
        assertEquals(Gravity.FILL, bitmapDrawable.getGravity());
        assertEquals(PixelFormat.TRANSLUCENT, bitmapDrawable.getOpacity());

        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        bitmapDrawable = new BitmapDrawable(source);
        assertEquals(Gravity.FILL, bitmapDrawable.getGravity());
        assertEquals(PixelFormat.OPAQUE, bitmapDrawable.getOpacity());
        bitmapDrawable.setGravity(Gravity.BOTTOM);
        assertEquals(PixelFormat.TRANSLUCENT, bitmapDrawable.getOpacity());

        bitmapDrawable = new BitmapDrawable(source);
        assertEquals(Gravity.FILL, bitmapDrawable.getGravity());
        assertEquals(PixelFormat.OPAQUE, bitmapDrawable.getOpacity());
        bitmapDrawable.setAlpha(120);
        assertEquals(PixelFormat.TRANSLUCENT, bitmapDrawable.getOpacity());
    }

    public void testGetConstantState() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);
        ConstantState constantState = bitmapDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(0, constantState.getChangingConfigurations());

        bitmapDrawable.setChangingConfigurations(1);
        constantState = bitmapDrawable.getConstantState();
        assertNotNull(constantState);
        assertEquals(1, constantState.getChangingConfigurations());
    }

    @SuppressWarnings("deprecation")
    public void testGetIntrinsicSize() {
        BitmapDrawable bitmapDrawable = new BitmapDrawable();
        assertEquals(0, bitmapDrawable.getIntrinsicWidth());
        assertEquals(0, bitmapDrawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(200, 300, Config.RGB_565);
        bitmapDrawable = new BitmapDrawable(bitmap);
        bitmapDrawable.setTargetDensity(bitmap.getDensity());
        assertEquals(200, bitmapDrawable.getIntrinsicWidth());
        assertEquals(300, bitmapDrawable.getIntrinsicHeight());

        InputStream source = mContext.getResources().openRawResource(R.drawable.size_48x48);
        bitmapDrawable = new BitmapDrawable(source);
        bitmapDrawable.setTargetDensity(mContext.getResources().getDisplayMetrics().densityDpi);
        assertEquals(48, bitmapDrawable.getIntrinsicWidth());
        assertEquals(48, bitmapDrawable.getIntrinsicHeight());
    }

    @SuppressWarnings("deprecation")
    public void testSetTargetDensity() {
        BitmapDrawable bitmapDrawable = new BitmapDrawable();

        Bitmap bitmap = Bitmap.createBitmap(200, 300, Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        bitmapDrawable = new BitmapDrawable(bitmap);
        bitmapDrawable.setTargetDensity(canvas.getDensity());
        assertEquals(200, bitmapDrawable.getIntrinsicWidth());
        assertEquals(300, bitmapDrawable.getIntrinsicHeight());

        DisplayMetrics disMetrics = new DisplayMetrics();
        disMetrics = getInstrumentation().getTargetContext().getResources().getDisplayMetrics();
        InputStream source = mContext.getResources().openRawResource(R.drawable.size_48x48);
        bitmapDrawable = new BitmapDrawable(source);
        bitmapDrawable.setTargetDensity(disMetrics.densityDpi);
        assertEquals(48, bitmapDrawable.getIntrinsicWidth());
        assertEquals(48, bitmapDrawable.getIntrinsicHeight());
    }

    @SuppressWarnings("deprecation")
    public void testInflate() throws IOException, XmlPullParserException {
        BitmapDrawable bitmapDrawable = new BitmapDrawable();

        XmlResourceParser parser = mContext.getResources().getXml(R.xml.bitmapdrawable);
        AttributeSet attrs = DrawableTestUtils.getAttributeSet(
                mContext.getResources().getXml(R.xml.bitmapdrawable), "bitmap_allattrs");
        bitmapDrawable.inflate(mContext.getResources(), parser, attrs);
        assertEquals(Gravity.TOP | Gravity.RIGHT, bitmapDrawable.getGravity());
        assertTrue(bitmapDrawable.getPaint().isDither());
        assertTrue(bitmapDrawable.getPaint().isAntiAlias());
        assertFalse(bitmapDrawable.getPaint().isFilterBitmap());
        assertEquals(TileMode.REPEAT, bitmapDrawable.getTileModeX());
        assertEquals(TileMode.REPEAT, bitmapDrawable.getTileModeY());

        bitmapDrawable = new BitmapDrawable();
        attrs = DrawableTestUtils.getAttributeSet(
                mContext.getResources().getXml(R.xml.bitmapdrawable), "bitmap_partattrs");
        // when parser is null
        bitmapDrawable.inflate(mContext.getResources(), null, attrs);
        assertEquals(Gravity.CENTER, bitmapDrawable.getGravity());
        assertEquals(TileMode.MIRROR, bitmapDrawable.getTileModeX());
        assertEquals(TileMode.MIRROR, bitmapDrawable.getTileModeY());
        // default value
        assertTrue(bitmapDrawable.getPaint().isDither());
        assertFalse(bitmapDrawable.getPaint().isAntiAlias());
        assertTrue(bitmapDrawable.getPaint().isFilterBitmap());

        attrs = DrawableTestUtils.getAttributeSet(
                mContext.getResources().getXml(R.xml.bitmapdrawable), "bitmap_wrongsrc");
        try {
            bitmapDrawable.inflate(mContext.getResources(), parser, attrs);
            fail("Should throw XmlPullParserException if the bitmap source can't be decoded.");
        } catch (XmlPullParserException e) {
        }

        attrs = DrawableTestUtils.getAttributeSet(
                mContext.getResources().getXml(R.xml.bitmapdrawable), "bitmap_nosrc");
        try {
            bitmapDrawable.inflate(mContext.getResources(), parser, attrs);
            fail("Should throw XmlPullParserException if the bitmap src doesn't be defined.");
        } catch (XmlPullParserException e) {
        }

        attrs = DrawableTestUtils.getAttributeSet(
                mContext.getResources().getXml(R.xml.bitmapdrawable), "bitmap_allattrs");
        try {
            bitmapDrawable.inflate(null, parser, attrs);
            fail("Should throw NullPointerException if resource is null");
        } catch (NullPointerException e) {
        }

        try {
            bitmapDrawable.inflate(mContext.getResources(), parser, null);
            fail("Should throw NullPointerException if attribute set is null");
        } catch (NullPointerException e) {
        }
    }

    public void testDraw() {
        InputStream source = mContext.getResources().openRawResource(R.raw.testimage);
        BitmapDrawable bitmapDrawable = new BitmapDrawable(source);

        // if the function draw() does not throw any exception, we think it is right.
        bitmapDrawable.draw(new Canvas());

        // input null as param
        try {
            bitmapDrawable.draw(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testMutate() {
        Resources resources = mContext.getResources();
        BitmapDrawable d1 = (BitmapDrawable) resources.getDrawable(R.drawable.testimage);
        BitmapDrawable d2 = (BitmapDrawable) resources.getDrawable(R.drawable.testimage);
        BitmapDrawable d3 = (BitmapDrawable) resources.getDrawable(R.drawable.testimage);

        d1.setAlpha(100);
        assertEquals(100, d1.getPaint().getAlpha());
        assertEquals(100, d2.getPaint().getAlpha());
        assertEquals(100, d3.getPaint().getAlpha());

        d1.mutate();
        d1.setAlpha(200);
        assertEquals(200, d1.getPaint().getAlpha());
        assertEquals(100, d2.getPaint().getAlpha());
        assertEquals(100, d3.getPaint().getAlpha());
        d2.setAlpha(50);
        assertEquals(200, d1.getPaint().getAlpha());
        assertEquals(50, d2.getPaint().getAlpha());
        assertEquals(50, d3.getPaint().getAlpha());
    }
}
