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


import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;

import junit.framework.TestCase;

public class AbsoluteSizeSpanTest extends TestCase {
    public void testConstructor() {
        new AbsoluteSizeSpan(0);
        new AbsoluteSizeSpan(-5);

        AbsoluteSizeSpan asp = new AbsoluteSizeSpan(10);
        final Parcel p = Parcel.obtain();
        try {
            asp.writeToParcel(p, 0);
            p.setDataPosition(0);
            new AbsoluteSizeSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetSize() {
        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(5);
        assertEquals(5, absoluteSizeSpan.getSize());

        absoluteSizeSpan = new AbsoluteSizeSpan(-5);
        assertEquals(-5, absoluteSizeSpan.getSize());
    }

    public void testUpdateMeasureState() {
        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(1);

        TextPaint tp = new TextPaint();
        absoluteSizeSpan.updateMeasureState(tp);
        assertEquals(1.0f, tp.getTextSize());

        absoluteSizeSpan = new AbsoluteSizeSpan(10);
        absoluteSizeSpan.updateMeasureState(tp);
        assertEquals(10.0f, tp.getTextSize());

        try {
            absoluteSizeSpan.updateMeasureState(null);
            fail("should throw NullPointerException when TextPaint is null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testUpdateDrawState() {
        // new the AbsoluteSizeSpan instance
        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(2);

        TextPaint tp = new TextPaint();
        absoluteSizeSpan.updateDrawState(tp);
        assertEquals(2.0f, tp.getTextSize());

        // new the AbsoluteSizeSpan instance
        absoluteSizeSpan = new AbsoluteSizeSpan(20);
        absoluteSizeSpan.updateDrawState(tp);
        assertEquals(20.0f, tp.getTextSize());

        try {
            absoluteSizeSpan.updateDrawState(null);
            fail("should throw NullPointerException when TextPaint is null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(2);
        absoluteSizeSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(2);
        absoluteSizeSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        try {
            AbsoluteSizeSpan asp = new AbsoluteSizeSpan(2);
            asp.writeToParcel(p, 0);
            p.setDataPosition(0);
            AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(p);
            assertEquals(2, absoluteSizeSpan.getSize());
        } finally {
            p.recycle();
        }

        p = Parcel.obtain();
        try {
            AbsoluteSizeSpan asp = new AbsoluteSizeSpan(-5);
            asp.writeToParcel(p, 0);
            p.setDataPosition(0);
            AbsoluteSizeSpan absoluteSizeSpan = new AbsoluteSizeSpan(p);
            assertEquals(-5, absoluteSizeSpan.getSize());
        } finally {
            p.recycle();
        }
    }
}
