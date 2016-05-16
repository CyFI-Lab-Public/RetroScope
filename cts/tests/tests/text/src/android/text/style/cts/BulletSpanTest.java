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
import android.text.Html;
import android.text.Spanned;
import android.text.style.BulletSpan;

import junit.framework.TestCase;

public class BulletSpanTest extends TestCase {
    public void testConstructor() {
        new BulletSpan();
        new BulletSpan(BulletSpan.STANDARD_GAP_WIDTH);
        BulletSpan b = new BulletSpan(BulletSpan.STANDARD_GAP_WIDTH, Color.RED);

        final Parcel p = Parcel.obtain();
        try {
            b.writeToParcel(p, 0);
            p.setDataPosition(0);
            new BulletSpan(p);
        } finally {
            p.recycle();
        }
    }

    public void testGetLeadingMargin() {
        BulletSpan bulletSpan = new BulletSpan(1);
        int leadingMargin1 = bulletSpan.getLeadingMargin(true);

        bulletSpan = new BulletSpan(4);
        int leadingMargin2 = bulletSpan.getLeadingMargin(false);

        assertTrue(leadingMargin2 > leadingMargin1);
    }

    public void testDrawLeadingMargin() {
        BulletSpan bulletSpan = new BulletSpan(10, 20);

        Canvas canvas = new Canvas();
        Paint paint = new Paint();
        Spanned text = Html.fromHtml("<b>hello</b>");

        bulletSpan.drawLeadingMargin(canvas, paint, 10, 0, 10, 0, 20, text, 0, 0, true, null);
    }

    public void testDrawLeadingMarginFailure() {
        // new the BulletSpan instance
        BulletSpan bulletSpan = new BulletSpan(10, 20);

        try {
            String text = "cts test.";
            bulletSpan.drawLeadingMargin(null, null, 0, 0, 0, 0, 0, text, 0, 0, true, null);
            fail("did not throw ClassCastException when use a String as text");
        } catch (ClassCastException e) {
            // expected, test success.
        }

        try {
            bulletSpan.drawLeadingMargin(null, null, 0, 0, 0, 0, 0, null, 0, 0, false, null);
            fail("did not throw NullPointerException when text is null");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }

    public void testDescribeContents() {
        BulletSpan bulletSpan = new BulletSpan();
        bulletSpan.describeContents();
    }

    public void testGetSpanTypeId() {
        BulletSpan bulletSpan = new BulletSpan();
        bulletSpan.getSpanTypeId();
    }

    public void testWriteToParcel() {
        int leadingMargin1 = 0;
        int leadingMargin2 = 0;

        Parcel p = Parcel.obtain();
        try {
            BulletSpan bulletSpan = new BulletSpan(BulletSpan.STANDARD_GAP_WIDTH, Color.RED);
            bulletSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            BulletSpan b = new BulletSpan(p);
            leadingMargin1 = b.getLeadingMargin(true);
        } finally {
            p.recycle();
        }

        p = Parcel.obtain();
        try {
            BulletSpan bulletSpan = new BulletSpan(10, Color.BLACK);
            bulletSpan.writeToParcel(p, 0);
            p.setDataPosition(0);
            BulletSpan b = new BulletSpan(p);
            leadingMargin2 = b.getLeadingMargin(true);
        } finally {
            p.recycle();
        }

        assertTrue(leadingMargin2 > leadingMargin1);
        // TODO: Test color. How?
    }
}
