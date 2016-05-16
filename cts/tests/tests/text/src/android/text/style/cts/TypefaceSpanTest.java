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

package android.text.style.cts;


import android.graphics.Typeface;
import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import junit.framework.TestCase;

public class TypefaceSpanTest extends TestCase {
    private static final String FAMILY = "monospace";

    public void testConstructor() {
        TypefaceSpan t = new TypefaceSpan(FAMILY);

        final Parcel p = Parcel.obtain();
        try {
            t.writeToParcel(p, 0);
            p.setDataPosition(0);
            new TypefaceSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetFamily() {
        TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);
        assertEquals(FAMILY, typefaceSpan.getFamily());
    }

    public void testUpdateMeasureState() {
        TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);

        TextPaint tp = new TextPaint();
        assertNull(tp.getTypeface());

        typefaceSpan.updateMeasureState(tp);

        assertNotNull(tp.getTypeface());
        // the style should be default style.
        assertEquals(Typeface.NORMAL, tp.getTypeface().getStyle());

        try {
            typefaceSpan.updateMeasureState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testUpdateDrawState() {
        TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);

        TextPaint tp = new TextPaint();
        assertNull(tp.getTypeface());

        typefaceSpan.updateDrawState(tp);

        assertNotNull(tp.getTypeface());
        // the style should be default style.
        assertEquals(Typeface.NORMAL, tp.getTypeface().getStyle());

        try {
            typefaceSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);
        typefaceSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);
        typefaceSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        try {
            TypefaceSpan typefaceSpan = new TypefaceSpan(FAMILY);
            typefaceSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            TypefaceSpan t = new TypefaceSpan(p);
            assertEquals(FAMILY, t.getFamily());
        } finally {
            p.recycle();
        }
    }
}
