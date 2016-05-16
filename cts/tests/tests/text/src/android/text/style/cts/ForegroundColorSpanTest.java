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


import android.graphics.Color;
import android.os.Parcel;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

import junit.framework.TestCase;

public class ForegroundColorSpanTest extends TestCase {
    public void testConstructor() {
        ForegroundColorSpan f = new ForegroundColorSpan(Color.GREEN);

        final Parcel p = Parcel.obtain();
        try {
            f.writeToParcel(p, 0);
            p.setDataPosition(0);
            new ForegroundColorSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetForegroundColor() {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.BLUE);
        assertEquals(Color.BLUE, foregroundColorSpan.getForegroundColor());

        foregroundColorSpan = new ForegroundColorSpan(Color.BLACK);
        assertEquals(Color.BLACK, foregroundColorSpan.getForegroundColor());
    }

    public void testUpdateDrawState() {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.CYAN);

        TextPaint tp = new TextPaint();
        tp.setColor(0);
        assertEquals(0, tp.getColor());
        foregroundColorSpan.updateDrawState(tp);
        assertEquals(Color.CYAN, tp.getColor());

        foregroundColorSpan = new ForegroundColorSpan(Color.DKGRAY);
        foregroundColorSpan.updateDrawState(tp);
        assertEquals(Color.DKGRAY, tp.getColor());

        try {
            foregroundColorSpan.updateDrawState(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        foregroundColorSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
        foregroundColorSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        try {
            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
            foregroundColorSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            ForegroundColorSpan f = new ForegroundColorSpan(p);
            assertEquals(Color.RED, f.getForegroundColor());
        } finally {
            p.recycle();
        }

        p = Parcel.obtain();
        try {
            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.MAGENTA);
            foregroundColorSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            ForegroundColorSpan f = new ForegroundColorSpan(p);
            assertEquals(Color.MAGENTA, f.getForegroundColor());
        } finally {
            p.recycle();
        }
    }
}
