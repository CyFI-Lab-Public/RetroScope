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


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.text.style.QuoteSpan;

import junit.framework.TestCase;

public class QuoteSpanTest extends TestCase {
    public void testConstructor() {
        new QuoteSpan();
        QuoteSpan q = new QuoteSpan(Color.RED);

        final Parcel p = Parcel.obtain();
        try {
            q.writeToParcel(p, 0);
            p.setDataPosition(0);
            new QuoteSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetLeadingMargin() {
        QuoteSpan quoteSpan = new QuoteSpan();

        quoteSpan.getLeadingMargin(true);
        quoteSpan.getLeadingMargin(false);
    }

    public void testGetColor() {
        QuoteSpan quoteSpan = new QuoteSpan(Color.BLACK);
        assertEquals(Color.BLACK, quoteSpan.getColor());

        quoteSpan = new QuoteSpan(Color.BLUE);
        assertEquals(Color.BLUE, quoteSpan.getColor());
    }

    public void testDrawLeadingMargin() {
        QuoteSpan quoteSpan = new QuoteSpan();

        Canvas c = new Canvas();
        Paint p = new Paint();
        quoteSpan.drawLeadingMargin(c, p, 0, 0, 0, 0, 0, null, 0, 0, true, null);

        try {
            quoteSpan.drawLeadingMargin(null, null, 0, 0, 0, 0, 0, null, 0, 0, true, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        QuoteSpan quoteSpan = new QuoteSpan(Color.RED);
        quoteSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        QuoteSpan quoteSpan = new QuoteSpan(Color.RED);
        quoteSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        Parcel p = Parcel.obtain();
        try {
            QuoteSpan quoteSpan = new QuoteSpan(Color.RED);
            quoteSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            QuoteSpan q = new QuoteSpan(p);
            assertEquals(Color.RED, q.getColor());
        } finally {
            p.recycle();
        }
        p = Parcel.obtain();
        try {
            QuoteSpan quoteSpan = new QuoteSpan(Color.MAGENTA);
            quoteSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            QuoteSpan q = new QuoteSpan(p);
            assertEquals(Color.MAGENTA, q.getColor());
        } finally {
            p.recycle();
        }
    }
}
