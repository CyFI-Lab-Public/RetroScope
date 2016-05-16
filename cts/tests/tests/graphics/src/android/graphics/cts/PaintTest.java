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

package android.graphics.cts;


import android.graphics.ColorFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rasterizer;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Xfermode;
import android.os.Build;
import android.test.AndroidTestCase;
import android.text.SpannedString;

import java.util.Locale;

public class PaintTest extends AndroidTestCase {

    private static final Typeface[] TYPEFACES = new Typeface[] {
            Typeface.DEFAULT,
            Typeface.DEFAULT_BOLD,
            Typeface.MONOSPACE,
            Typeface.SANS_SERIF,
            Typeface.SERIF,
    };

    public void testConstructor() {
        new Paint();

        new Paint(1);

        Paint p = new Paint();
        new Paint(p);
    }

    public void testBreakText() {
        String text = "HIJKLMN";
        char[] textChars = text.toCharArray();
        SpannedString textSpan = new SpannedString(text);

        Paint p = new Paint();

        // We need to turn off kerning in order to get accurate comparisons
        p.setFlags(p.getFlags() & ~Paint.DEV_KERN_TEXT_FLAG);

        float[] widths = new float[text.length()];
        assertEquals(text.length(), p.getTextWidths(text, widths));

        float totalWidth = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            totalWidth += widths[i];
        }

        float[] measured = new float[1];
        for (int i = 0; i < text.length(); i++) {
            assertBreakText(text, textChars, textSpan, i, i + 1, true, totalWidth, 1, widths[i]);
        }

        // Measure empty string
        assertBreakText(text, textChars, textSpan, 0, 0, true, totalWidth, 0, 0);

        // Measure substring from front: "HIJ"
        assertBreakText(text, textChars, textSpan, 0, 3, true, totalWidth,
                3, widths[0] + widths[1] + widths[2]);

        // Reverse measure substring from front: "HIJ"
        assertBreakText(text, textChars, textSpan, 0, 3, false, totalWidth,
                3, widths[0] + widths[1] + widths[2]);

        // Measure substring from back: "MN"
        assertBreakText(text, textChars, textSpan, 5, 7, true, totalWidth,
                2, widths[5] + widths[6]);

        // Reverse measure substring from back: "MN"
        assertBreakText(text, textChars, textSpan, 5, 7, false, totalWidth,
                2, widths[5] + widths[6]);

        // Measure substring in the middle: "JKL"
        assertBreakText(text, textChars, textSpan, 2, 5, true, totalWidth,
                3, widths[2] + widths[3] + widths[4]);

        // Reverse measure substring in the middle: "JKL"
        assertBreakText(text, textChars, textSpan, 2, 5, false, totalWidth,
                3, widths[2] + widths[3] + widths[4]);

        // Measure substring in the middle and restrict width to the first 2 characters.
        assertBreakText(text, textChars, textSpan, 2, 5, true, widths[2] + widths[3],
                2, widths[2] + widths[3]);

        // Reverse measure substring in the middle and restrict width to the last 2 characters.
        assertBreakText(text, textChars, textSpan, 2, 5, false, widths[3] + widths[4],
                2, widths[3] + widths[4]);
    }

    private void assertBreakText(String text, char[] textChars, SpannedString textSpan,
            int start, int end, boolean measureForwards, float maxWidth, int expectedCount,
            float expectedWidth) {
        Paint p = new Paint();

        // We need to turn off kerning in order to get accurate comparisons
        p.setFlags(p.getFlags() & ~Paint.DEV_KERN_TEXT_FLAG);

        int count = end - start;
        if (!measureForwards) {
            count = -count;
        }

        float[][] measured = new float[][] {
            new float[1],
            new float[1],
            new float[1]
        };
        String textSlice = text.substring(start, end);
        assertEquals(expectedCount, p.breakText(textSlice, measureForwards, maxWidth, measured[0]));
        assertEquals(expectedCount, p.breakText(textChars, start, count, maxWidth, measured[1]));
        assertEquals(expectedCount, p.breakText(textSpan, start, end, measureForwards, maxWidth,
                measured[2]));

        for (int i = 0; i < measured.length; i++) {
            assertEquals("i: " + i, expectedWidth, measured[i][0]);
        }
    }

    public void testSet() {
        Paint p  = new Paint();
        Paint p2 = new Paint();
        ColorFilter c = new ColorFilter();
        MaskFilter m  = new MaskFilter();
        PathEffect e  = new PathEffect();
        Rasterizer r  = new Rasterizer();
        Shader s      = new Shader();
        Typeface t    = Typeface.DEFAULT;
        Xfermode x = new Xfermode();

        p.setColorFilter(c);
        p.setMaskFilter(m);
        p.setPathEffect(e);
        p.setRasterizer(r);
        p.setShader(s);
        p.setTypeface(t);
        p.setXfermode(x);
        p2.set(p);
        assertEquals(c, p2.getColorFilter());
        assertEquals(m, p2.getMaskFilter());
        assertEquals(e, p2.getPathEffect());
        assertEquals(r, p2.getRasterizer());
        assertNotSame(s, p2.getShader());
        assertEquals(t, p2.getTypeface());
        assertEquals(x, p2.getXfermode());

        p2.set(p2);
        assertEquals(c, p2.getColorFilter());
        assertEquals(m, p2.getMaskFilter());
        assertEquals(e, p2.getPathEffect());
        assertEquals(r, p2.getRasterizer());
        assertNotSame(s, p2.getShader());
        assertEquals(t, p2.getTypeface());
        assertEquals(x, p2.getXfermode());

        p.setColorFilter(null);
        p.setMaskFilter(null);
        p.setPathEffect(null);
        p.setRasterizer(null);
        p.setShader(null);
        p.setTypeface(null);
        p.setXfermode(null);
        p2.set(p);
        assertNull(p2.getColorFilter());
        assertNull(p2.getMaskFilter());
        assertNull(p2.getPathEffect());
        assertNull(p2.getRasterizer());
        assertNull(p2.getShader());
        assertNull(p2.getTypeface());
        assertNull(p2.getXfermode());

        p2.set(p2);
        assertNull(p2.getColorFilter());
        assertNull(p2.getMaskFilter());
        assertNull(p2.getPathEffect());
        assertNull(p2.getRasterizer());
        assertNull(p2.getShader());
        assertNull(p2.getTypeface());
        assertNull(p2.getXfermode());
    }

    public void testAccessStrokeCap() {
        Paint p = new Paint();

        p.setStrokeCap(Cap.BUTT);
        assertEquals(Cap.BUTT, p.getStrokeCap());

        p.setStrokeCap(Cap.ROUND);
        assertEquals(Cap.ROUND, p.getStrokeCap());

        p.setStrokeCap(Cap.SQUARE);
        assertEquals(Cap.SQUARE, p.getStrokeCap());

        try {
            p.setStrokeCap(null);
            fail("Should throw an Exception");
        } catch (RuntimeException e) {
            //except here
        }
    }

    public void testAccessXfermode() {
        Paint p = new Paint();
        Xfermode x = new Xfermode();

        assertEquals(x, p.setXfermode(x));
        assertEquals(x, p.getXfermode());

        assertNull(p.setXfermode(null));
        assertNull(p.getXfermode());
    }

    public void testAccessShader() {
        Paint p = new Paint();
        Shader s = new Shader();

        assertEquals(s, p.setShader(s));
        assertEquals(s, p.getShader());

        assertNull(p.setShader(null));
        assertNull(p.getShader());
    }

    public void testSetAntiAlias() {
        Paint p = new Paint();

        p.setAntiAlias(true);
        assertTrue(p.isAntiAlias());

        p.setAntiAlias(false);
        assertFalse(p.isAntiAlias());

    }


    public void testAccessTypeface() {
        Paint p = new Paint();

        assertEquals(Typeface.DEFAULT, p.setTypeface(Typeface.DEFAULT));
        assertEquals(Typeface.DEFAULT, p.getTypeface());

        assertEquals(Typeface.DEFAULT_BOLD, p.setTypeface(Typeface.DEFAULT_BOLD));
        assertEquals(Typeface.DEFAULT_BOLD, p.getTypeface());

        assertEquals(Typeface.MONOSPACE, p.setTypeface(Typeface.MONOSPACE));
        assertEquals(Typeface.MONOSPACE, p.getTypeface());

        assertNull(p.setTypeface(null));
        assertNull(p.getTypeface());
    }

    public void testAccessPathEffect() {
        Paint p = new Paint();
        PathEffect e = new PathEffect();

        assertEquals(e, p.setPathEffect(e));
        assertEquals(e, p.getPathEffect());

        assertNull(p.setPathEffect(null));
        assertNull(p.getPathEffect());
    }

    public void testSetFakeBoldText() {
        Paint p = new Paint();

        p.setFakeBoldText(true);
        assertTrue(p.isFakeBoldText());

        p.setFakeBoldText(false);
        assertFalse(p.isFakeBoldText());
    }

    public void testAccessStrokeJoin() {
        Paint p = new Paint();

        p.setStrokeJoin(Join.BEVEL);
        assertEquals(Join.BEVEL, p.getStrokeJoin());

        p.setStrokeJoin(Join.MITER);
        assertEquals(Join.MITER, p.getStrokeJoin());

        p.setStrokeJoin(Join.ROUND);
        assertEquals(Join.ROUND, p.getStrokeJoin());

        try {
            p.setStrokeJoin(null);
            fail("Should throw an Exception");
        } catch (RuntimeException e) {
            //except here
        }
    }

    public void testAccessStyle() {
        Paint p = new Paint();

        p.setStyle(Style.FILL);
        assertEquals(Style.FILL, p.getStyle());

        p.setStyle(Style.FILL_AND_STROKE);
        assertEquals(Style.FILL_AND_STROKE, p.getStyle());

        p.setStyle(Style.STROKE);
        assertEquals(Style.STROKE, p.getStyle());

        try {
            p.setStyle(null);
            fail("Should throw an Exception");
        } catch (RuntimeException e) {
            //except here
        }
    }

    public void testGetFontSpacing() {
        Paint p = new Paint();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            float spacing10 = p.getFontSpacing();
            assertTrue(spacing10 > 0);

            p.setTextSize(20);
            float spacing20 = p.getFontSpacing();
            assertTrue(spacing20 > spacing10);
        }
    }

    public void testSetSubpixelText() {
        Paint p = new Paint();

        p.setSubpixelText(true);
        assertTrue(p.isSubpixelText());

        p.setSubpixelText(false);
        assertFalse(p.isSubpixelText());
    }

    public void testAccessTextScaleX() {
        Paint p = new Paint();

        p.setTextScaleX(2.0f);
        assertEquals(2.0f, p.getTextScaleX());

        p.setTextScaleX(1.0f);
        assertEquals(1.0f, p.getTextScaleX());

        p.setTextScaleX(0.0f);
        assertEquals(0.0f, p.getTextScaleX());

    }

    public void testAccessMaskFilter() {
        Paint p = new Paint();
        MaskFilter m = new MaskFilter();

        assertEquals(m, p.setMaskFilter(m));
        assertEquals(m, p.getMaskFilter());

        assertNull(p.setMaskFilter(null));
        assertNull(p.getMaskFilter());
    }

    public void testAccessColorFilter() {
        Paint p = new Paint();
        ColorFilter c = new ColorFilter();

        assertEquals(c, p.setColorFilter(c));
        assertEquals(c, p.getColorFilter());

        assertNull(p.setColorFilter(null));
        assertNull(p.getColorFilter());
    }

    public void testAccessRasterizer() {
        Paint p = new Paint();
        Rasterizer r = new Rasterizer();

        assertEquals(r, p.setRasterizer(r));
        assertEquals(r, p.getRasterizer());

        assertNull(p.setRasterizer(null));
        assertNull(p.getRasterizer());
    }

    public void testSetARGB() {
        Paint p = new Paint();

        p.setARGB(0, 0, 0, 0);
        assertEquals(0, p.getColor());

        p.setARGB(3, 3, 3, 3);
        assertEquals((3 << 24) | (3 << 16) | (3 << 8) | 3, p.getColor());

    }

    public void testAscent() {
        Paint p = new Paint();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            float ascent10 = p.ascent();
            assertTrue(ascent10 < 0);

            p.setTextSize(20);
            float ascent20 = p.ascent();
            assertTrue(ascent20 < ascent10);
        }
    }

    public void testAccessTextSkewX() {
        Paint p = new Paint();

        p.setTextSkewX(1.0f);
        assertEquals(1.0f, p.getTextSkewX());

        p.setTextSkewX(0.0f);
        assertEquals(0.0f, p.getTextSkewX());

        p.setTextSkewX(-0.25f);
        assertEquals(-0.25f, p.getTextSkewX());
    }

    public void testAccessTextSize() {
        Paint p = new Paint();

        p.setTextSize(1.0f);
        assertEquals(1.0f, p.getTextSize());

        p.setTextSize(2.0f);
        assertEquals(2.0f, p.getTextSize());

        // text size should be greater than 0, so set -1 has no effect
        p.setTextSize(-1.0f);
        assertEquals(2.0f, p.getTextSize());

        // text size should be greater than or equals to 0
        p.setTextSize(0.0f);
        assertEquals(0.0f, p.getTextSize());
    }

    public void testGetTextWidths() throws Exception {
        String text = "HIJKLMN";
        char[] textChars = text.toCharArray();
        SpannedString textSpan = new SpannedString(text);

        // Test measuring the widths of the entire text
        assertGetTextWidths(text, textChars, textSpan, 0, 7);

        // Test measuring a substring of the text
        assertGetTextWidths(text, textChars, textSpan, 1, 3);

        // Test measuring a substring of zero length.
        assertGetTextWidths(text, textChars, textSpan, 3, 3);

        // Test measuring substrings from the front and back
        assertGetTextWidths(text, textChars, textSpan, 0, 2);
        assertGetTextWidths(text, textChars, textSpan, 4, 7);
    }

    /** Tests all four overloads of getTextWidths are the same. */
    private void assertGetTextWidths(String text, char[] textChars, SpannedString textSpan,
            int start, int end) {
        Paint p = new Paint();
        int count = end - start;
        float[][] widths = new float[][] {
            new float[count],
            new float[count],
            new float[count],
            new float[count]
        };

        String textSlice = text.substring(start, end);
        assertEquals(count, p.getTextWidths(textSlice, widths[0]));
        assertEquals(count, p.getTextWidths(textChars, start, count, widths[1]));
        assertEquals(count, p.getTextWidths(textSpan, start, end, widths[2]));
        assertEquals(count, p.getTextWidths(text, start, end, widths[3]));

        // Check that the widths returned by the overloads are the same.
        for (int i = 0; i < count; i++) {
            assertEquals(widths[0][i], widths[1][i]);
            assertEquals(widths[1][i], widths[2][i]);
            assertEquals(widths[2][i], widths[3][i]);
        }
    }

    public void testSetStrikeThruText() {
        Paint p = new Paint();

        p.setStrikeThruText(true);
        assertTrue(p.isStrikeThruText());

        p.setStrikeThruText(false);
        assertFalse(p.isStrikeThruText());
    }

    public void testAccessTextAlign() {
        Paint p = new Paint();

        p.setTextAlign(Align.CENTER);
        assertEquals(Align.CENTER, p.getTextAlign());

        p.setTextAlign(Align.LEFT);
        assertEquals(Align.LEFT, p.getTextAlign());

        p.setTextAlign(Align.RIGHT);
        assertEquals(Align.RIGHT, p.getTextAlign());
    }

    public void testAccessTextLocale() {
        Paint p = new Paint();

        final Locale defaultLocale = Locale.getDefault();

        // Check default
        assertEquals(defaultLocale, p.getTextLocale());

        // Check setter / getter
        p.setTextLocale(Locale.US);
        assertEquals(Locale.US, p.getTextLocale());

        p.setTextLocale(Locale.CHINESE);
        assertEquals(Locale.CHINESE, p.getTextLocale());

        p.setTextLocale(Locale.JAPANESE);
        assertEquals(Locale.JAPANESE, p.getTextLocale());

        p.setTextLocale(Locale.KOREAN);
        assertEquals(Locale.KOREAN, p.getTextLocale());

        // Check reverting back to default
        p.setTextLocale(defaultLocale);
        assertEquals(defaultLocale, p.getTextLocale());

        // Check that we cannot pass a null locale
        try {
            p.setTextLocale(null);
            assertFalse(true);
        }
        catch (IllegalArgumentException iae) {
            // OK !!
        }
    }

    public void testGetFillPath() {
        Paint p = new Paint();
        Path path1 = new Path();
        Path path2 = new Path();

        assertTrue(path1.isEmpty());
        assertTrue(path2.isEmpty());
        p.getFillPath(path1, path2);
        assertTrue(path1.isEmpty());
        assertTrue(path2.isEmpty());

        // No setter

    }

    public void testAccessAlpha() {
        Paint p = new Paint();

        p.setAlpha(0);
        assertEquals(0, p.getAlpha());

        p.setAlpha(255);
        assertEquals(255, p.getAlpha());

        // set value should between 0 and 255, so 266 is rounded to 10
        p.setAlpha(266);
        assertEquals(10, p.getAlpha());

        // set value should between 0 and 255, so -20 is rounded to 236
        p.setAlpha(-20);
        assertEquals(236, p.getAlpha());
    }

    public void testSetFilterBitmap() {
        Paint p = new Paint();

        p.setFilterBitmap(true);
        assertTrue(p.isFilterBitmap());

        p.setFilterBitmap(false);
        assertFalse(p.isFilterBitmap());
    }

    public void testAccessColor() {
        Paint p = new Paint();

        p.setColor(1);
        assertEquals(1, p.getColor());

        p.setColor(0);
        assertEquals(0, p.getColor());

        p.setColor(255);
        assertEquals(255, p.getColor());

        p.setColor(-1);
        assertEquals(-1, p.getColor());

        p.setColor(256);
        assertEquals(256, p.getColor());
    }

    public void testSetShadowLayer() {
        new Paint().setShadowLayer(10, 1, 1, 0);
    }

    public void testGetFontMetrics1() {
        Paint p = new Paint();
        Paint.FontMetrics fm = new Paint.FontMetrics();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            float spacing10 = p.getFontMetrics(fm);
            assertEquals(p.ascent(), fm.ascent);
            assertEquals(p.descent(), fm.descent);

            p.setTextSize(20);
            float spacing20 = p.getFontMetrics(fm);
            assertEquals(p.ascent(), fm.ascent);
            assertEquals(p.descent(), fm.descent);
        }
    }

    public void testGetFontMetrics2() {
        Paint p = new Paint();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            Paint.FontMetrics fm = p.getFontMetrics();
            assertEquals(p.ascent(), fm.ascent);
            assertEquals(p.descent(), fm.descent);

            p.setTextSize(20);
            fm = p.getFontMetrics();
            assertEquals(p.ascent(), fm.ascent);
            assertEquals(p.descent(), fm.descent);
        }
    }

    public void testAccessStrokeMiter() {
        Paint p = new Paint();

        p.setStrokeMiter(0.0f);
        assertEquals(0.0f, p.getStrokeMiter());

        p.setStrokeMiter(10.0f);
        assertEquals(10.0f, p.getStrokeMiter());

        // set value should be greater or equal to 0, set to -10.0f has no effect
        p.setStrokeMiter(-10.0f);
        assertEquals(10.0f, p.getStrokeMiter());
    }

    public void testClearShadowLayer() {
        new Paint().clearShadowLayer();
    }

    public void testSetUnderlineText() {
        Paint p = new Paint();

        p.setUnderlineText(true);
        assertTrue(p.isUnderlineText());

        p.setUnderlineText(false);
        assertFalse(p.isUnderlineText());
    }

    public void testSetDither() {
        Paint p = new Paint();

        p.setDither(true);
        assertTrue(p.isDither());

        p.setDither(false);
        assertFalse(p.isDither());
    }

    public void testDescent() {
        Paint p = new Paint();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            float descent10 = p.descent();
            assertTrue(descent10 > 0);

            p.setTextSize(20);
            float descent20 = p.descent();
            assertTrue(descent20 > descent10);
        }
    }

    public void testAccessFlags() {
        Paint p = new Paint();

        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        assertEquals(Paint.ANTI_ALIAS_FLAG, p.getFlags());

        p.setFlags(Paint.DEV_KERN_TEXT_FLAG);
        assertEquals(Paint.DEV_KERN_TEXT_FLAG, p.getFlags());
    }

    public void testAccessStrokeWidth() {
        Paint p = new Paint();

        p.setStrokeWidth(0.0f);
        assertEquals(0.0f, p.getStrokeWidth());

        p.setStrokeWidth(10.0f);
        assertEquals(10.0f, p.getStrokeWidth());

        // set value must greater or equal to 0, set -10.0f has no effect
        p.setStrokeWidth(-10.0f);
        assertEquals(10.0f, p.getStrokeWidth());
    }

    public void testReset() {

        Paint p  = new Paint();
        ColorFilter c = new ColorFilter();
        MaskFilter m  = new MaskFilter();
        PathEffect e  = new PathEffect();
        Rasterizer r  = new Rasterizer();
        Shader s      = new Shader();
        Typeface t    = Typeface.DEFAULT;
        Xfermode x = new Xfermode();

        p.setColorFilter(c);
        p.setMaskFilter(m);
        p.setPathEffect(e);
        p.setRasterizer(r);
        p.setShader(s);
        p.setTypeface(t);
        p.setXfermode(x);
        p.setFlags(Paint.ANTI_ALIAS_FLAG);
        assertEquals(c, p.getColorFilter());
        assertEquals(m, p.getMaskFilter());
        assertEquals(e, p.getPathEffect());
        assertEquals(r, p.getRasterizer());
        assertEquals(s, p.getShader());
        assertEquals(t, p.getTypeface());
        assertEquals(x, p.getXfermode());
        assertEquals(Paint.ANTI_ALIAS_FLAG, p.getFlags());

        p.reset();
        assertEquals(Paint.DEV_KERN_TEXT_FLAG | Paint.EMBEDDED_BITMAP_TEXT_FLAG, p.getFlags());
        assertEquals(null, p.getColorFilter());
        assertEquals(null, p.getMaskFilter());
        assertEquals(null, p.getPathEffect());
        assertEquals(null, p.getRasterizer());
        assertEquals(null, p.getShader());
        assertEquals(null, p.getTypeface());
        assertEquals(null, p.getXfermode());

    }

    public void testSetLinearText() {
        Paint p = new Paint();

        p.setLinearText(true);
        assertTrue(p.isLinearText());

        p.setLinearText(false);
        assertFalse(p.isLinearText());
    }

    public void testGetFontMetricsInt1() {
        Paint p = new Paint();
        Paint.FontMetricsInt fmi = new Paint.FontMetricsInt();

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            p.getFontMetricsInt(fmi);
            assertEquals(Math.round(p.ascent()), fmi.ascent);
            assertEquals(Math.round(p.descent()), fmi.descent);

            p.setTextSize(20);
            p.getFontMetricsInt(fmi);
            assertEquals(Math.round(p.ascent()), fmi.ascent);
            assertEquals(Math.round(p.descent()), fmi.descent);
        }
    }

    public void testGetFontMetricsInt2() {
        Paint p = new Paint();
        Paint.FontMetricsInt fmi;

        for (Typeface typeface : TYPEFACES) {
            p.setTypeface(typeface);

            p.setTextSize(10);
            fmi = p.getFontMetricsInt();
            assertEquals(Math.round(p.ascent()), fmi.ascent);
            assertEquals(Math.round(p.descent()), fmi.descent);

            p.setTextSize(20);
            fmi = p.getFontMetricsInt();
            assertEquals(Math.round(p.ascent()), fmi.ascent);
            assertEquals(Math.round(p.descent()), fmi.descent);
        }
    }

    public void testMeasureText() {
        String text = "HIJKLMN";
        char[] textChars = text.toCharArray();
        SpannedString textSpan = new SpannedString(text);

        Paint p = new Paint();

        // We need to turn off kerning in order to get accurate comparisons
        p.setFlags(p.getFlags() & ~Paint.DEV_KERN_TEXT_FLAG);

        float[] widths = new float[text.length()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = p.measureText(text, i, i + 1);
        }

        float totalWidth = 0;
        for (int i = 0; i < widths.length; i++) {
            totalWidth += widths[i];
        }

        // Test measuring the widths of the entire text
        assertMeasureText(text, textChars, textSpan, 0, 7, totalWidth);

        // Test measuring a substring of the text
        assertMeasureText(text, textChars, textSpan, 1, 3, widths[1] + widths[2]);

        // Test measuring a substring of zero length.
        assertMeasureText(text, textChars, textSpan, 3, 3, 0);

        // Test measuring substrings from the front and back
        assertMeasureText(text, textChars, textSpan, 0, 2, widths[0] + widths[1]);
        assertMeasureText(text, textChars, textSpan, 4, 7, widths[4] + widths[5] + widths[6]);
    }

    public void testMeasureTextWithLongText() {
        // This test is not compatible with 4.0.3
        if ("4.0.3".equals(Build.VERSION.RELEASE)) {
            return;
        }

        final int MAX_COUNT = 65535;
        char[] longText = new char[MAX_COUNT];
        for (int n = 0; n < MAX_COUNT; n++) {
            longText[n] = 'm';
        }

        Paint p = new Paint();
        float width = p.measureText(longText, 0, 1);
        assertEquals(true, width > 0);
    }

    /** Tests that all four overloads of measureText are the same and match some value. */
    private void assertMeasureText(String text, char[] textChars, SpannedString textSpan,
            int start, int end, float expectedWidth) {
        Paint p = new Paint();

        // We need to turn off kerning in order to get accurate comparisons
        p.setFlags(p.getFlags() & ~Paint.DEV_KERN_TEXT_FLAG);

        int count = end - start;
        float[] widths = new float[] {-1, -1, -1, -1};

        String textSlice = text.substring(start, end);
        widths[0] = p.measureText(textSlice);
        widths[1] = p.measureText(textChars, start, count);
        widths[2] = p.measureText(textSpan, start, end);
        widths[3] = p.measureText(text, start, end);

        // Check that the widths returned by the overloads are the same.
        assertEquals(widths[0], widths[1]);
        assertEquals(widths[1], widths[2]);
        assertEquals(widths[2], widths[3]);
        assertEquals(widths[3], expectedWidth);
    }

    public void testGetTextPath1() {
        Paint p = new Paint();
        char[] chars = {'H', 'I', 'J', 'K', 'L', 'M', 'N'};
        Path path = new Path();

        assertTrue(path.isEmpty());
        p.getTextPath(chars, 0, 7, 0, 0, path);
        assertFalse(path.isEmpty());

        try {
            p.getTextPath(chars, -2, 7, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

        try {
            p.getTextPath(chars, 0, -3, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

        try {
            p.getTextPath(chars, 3, 7, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

    }

    public void testGetTextPath2() {
        Paint p = new Paint();
        String string = "HIJKLMN";
        Path path = new Path();

        assertTrue(path.isEmpty());
        p.getTextPath(string, 0, 7, 0, 0, path);
        assertFalse(path.isEmpty());

        try {
            p.getTextPath(string, -2, 7, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

        try {
            p.getTextPath(string, 0, -3, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

        try {
            p.getTextPath(string, 7, 3, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }

        try {
            p.getTextPath(string, 3, 9, 0, 0, path);
            fail("Should throw an exception here");
        } catch (RuntimeException e) {
        }
    }

}
