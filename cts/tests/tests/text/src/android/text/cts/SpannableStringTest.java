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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;

public class SpannableStringTest extends AndroidTestCase {

    public void testConstructor() {
        new SpannableString("test");

        try {
            new SpannableString(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
        }
    }

    public void testValueOf() {
        String text = "test valueOf";
        SpannableString spannable = SpannableString.valueOf(text);
        assertNotNull(spannable);
        assertEquals(text, spannable.toString());

        spannable = new SpannableString(text);
        assertSame(spannable, SpannableString.valueOf(spannable));

        try {
            SpannableString.valueOf(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
        }
    }

    public void testSetSpan() {
        String text = "hello, world";
        SpannableString spannable = new SpannableString(text);

        spannable.setSpan(null, 1, 4, SpannableString.SPAN_POINT_POINT);
        assertEquals(1, spannable.getSpanStart(null));
        assertEquals(4, spannable.getSpanEnd(null));
        assertEquals(SpannableString.SPAN_POINT_POINT, spannable.getSpanFlags(null));

        UnderlineSpan underlineSpan = new UnderlineSpan();
        spannable.setSpan(underlineSpan, 0, 2, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(0, spannable.getSpanStart(underlineSpan));
        assertEquals(2, spannable.getSpanEnd(underlineSpan));
        assertEquals(SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
                spannable.getSpanFlags(underlineSpan));

        try {
            spannable.setSpan(null, 4, 1, SpannableString.SPAN_POINT_POINT);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
        }

        try {
            spannable.setSpan(underlineSpan, -1, text.length() + 1,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testRemoveSpan() {
        SpannableString spannable = new SpannableString("hello, world");

        spannable.removeSpan(null);

        UnderlineSpan underlineSpan = new UnderlineSpan();
        spannable.setSpan(underlineSpan, 0, 2, 2);
        assertEquals(0, spannable.getSpanStart(underlineSpan));
        assertEquals(2, spannable.getSpanEnd(underlineSpan));
        assertEquals(2, spannable.getSpanFlags(underlineSpan));

        spannable.removeSpan(underlineSpan);
        assertEquals(-1, spannable.getSpanStart(underlineSpan));
        assertEquals(-1, spannable.getSpanEnd(underlineSpan));
        assertEquals(0, spannable.getSpanFlags(underlineSpan));

        // try to test removeSpan when there are no spans
        spannable.removeSpan(underlineSpan);
        assertEquals(-1, spannable.getSpanStart(underlineSpan));
        assertEquals(-1, spannable.getSpanEnd(underlineSpan));
        assertEquals(0, spannable.getSpanFlags(underlineSpan));
    }

    public void testSubSequence() {
        String text = "hello, world";
        SpannableString spannable = new SpannableString(text);

        CharSequence subSequence = spannable.subSequence(0, 2);
        assertEquals("he", subSequence.toString());

        subSequence = spannable.subSequence(0, text.length());
        assertEquals(text, subSequence.toString());

        try {
            spannable.subSequence(-1, text.length() + 1);
            fail("subSequence failed when index is out of bounds");
        } catch (StringIndexOutOfBoundsException e) {
        }

        try {
            spannable.subSequence(2, 0);
            fail("subSequence failed on invalid index");
        } catch (StringIndexOutOfBoundsException e) {
        }
    }
}
