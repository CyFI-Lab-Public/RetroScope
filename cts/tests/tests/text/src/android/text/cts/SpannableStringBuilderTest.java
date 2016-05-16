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
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.TabStopSpan;
import android.text.style.UnderlineSpan;

/**
 * Test {@link SpannableStringBuilder}.
 */
public class SpannableStringBuilderTest extends AndroidTestCase {

    private StrikethroughSpan mStrikethroughSpan;
    private UnderlineSpan mUnderlineSpan;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mUnderlineSpan = new UnderlineSpan();
        mStrikethroughSpan = new StrikethroughSpan();
    }

    public void testConstructor1() {
        @SuppressWarnings("unused")
        SpannableStringBuilder dummy = new SpannableStringBuilder();
        dummy = new SpannableStringBuilder("test");

        try {
            dummy = new SpannableStringBuilder(null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testConstructor2() {
        @SuppressWarnings("unused")
        SpannableStringBuilder dummy = new SpannableStringBuilder("Text", 0, "Text".length());
        dummy = new SpannableStringBuilder(new SpannableString("test"), 0, "Text".length());

        try {
            dummy = new SpannableStringBuilder("Text", 0, 10);
            fail("should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            dummy = new SpannableStringBuilder("Text", -3, 3);
            fail("should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            dummy = new SpannableStringBuilder("Text", 3, 0);
            fail("should throw StringIndexOutOfBoundsException");
        } catch (StringIndexOutOfBoundsException e) {
            // expected exception
        }
    }

    public void testGetSpanFlags() {
        SpannableStringBuilder builder = new SpannableStringBuilder("spannable string");
        assertEquals(0, builder.getSpanFlags(mUnderlineSpan));

        builder.setSpan(mUnderlineSpan, 2, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        builder.setSpan(mUnderlineSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        builder.setSpan(mStrikethroughSpan, 5, 7, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        assertEquals(Spanned.SPAN_EXCLUSIVE_INCLUSIVE, builder.getSpanFlags(mUnderlineSpan));
        assertEquals(Spanned.SPAN_INCLUSIVE_EXCLUSIVE, builder.getSpanFlags(mStrikethroughSpan));
        assertEquals(0, builder.getSpanFlags(new Object()));
    }

    public void testNextSpanTransition() {
        SpannableStringBuilder builder = new SpannableStringBuilder("spannable string");

        builder.setSpan(mUnderlineSpan, 1, 2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        builder.setSpan(mUnderlineSpan, 3, 4, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        builder.setSpan(mStrikethroughSpan, 5, 6, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        builder.setSpan(mStrikethroughSpan, 8, 9, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        assertEquals(8, builder.nextSpanTransition(0, 10, StrikethroughSpan.class));
        assertEquals(10, builder.nextSpanTransition(0, 10, TabStopSpan.class));
        assertEquals(3, builder.nextSpanTransition(0, 5, null));
        assertEquals(100, builder.nextSpanTransition(-5, 100, TabStopSpan.class));
        assertEquals(1, builder.nextSpanTransition(3, 1, UnderlineSpan.class));
    }

    public void testSetSpan() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        try {
            builder.setSpan(mUnderlineSpan, 4, 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.setSpan(mUnderlineSpan, -1, 100, Spanned.SPAN_POINT_POINT);
            fail("should throw ..IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        builder.setSpan(null, 1, 4, Spanned.SPAN_POINT_POINT);
        assertEquals(1, builder.getSpanStart(null));
        assertEquals(4, builder.getSpanEnd(null));
        assertEquals(Spanned.SPAN_POINT_POINT, builder.getSpanFlags(null));

        builder.setSpan(mUnderlineSpan, 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        assertEquals(0, builder.getSpanStart(mUnderlineSpan));
        assertEquals(2, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE, builder.getSpanFlags(mUnderlineSpan));
    }

    public void testValueOf() {
        try {
            SpannableStringBuilder.valueOf(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }

        try {
            SpannableStringBuilder.valueOf((SpannableStringBuilder) null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }

        assertNotNull(SpannableStringBuilder.valueOf("hello, string"));

        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        assertSame(builder, SpannableStringBuilder.valueOf(builder));
    }

    public void testReplace1() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world!");
        CharSequence text = "hi";
        builder.replace(0, 5, text);
        assertEquals("hi, world!", builder.toString());

        builder = new SpannableStringBuilder("hello, world!");
        builder.replace(7, 12, "google");
        assertEquals("hello, google!", builder.toString());

        try {
            builder.replace(4, 2, text);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.replace(-4, 100, text);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.replace(0, 1, null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testReplace2() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        CharSequence text = "ahiabc";
        builder.replace(0, 5, text, 3, text.length());
        assertEquals("abc, world", builder.toString());

        builder = new SpannableStringBuilder("hello, world");
        builder.replace(3, 5, text, 3, text.length());
        assertEquals("helabc, world", builder.toString());

        builder = new SpannableStringBuilder("hello, world");
        builder.replace(0, 5, text, 0, text.length());
        assertEquals("ahiabc, world", builder.toString());

        // Replacing by an empty string (identical target indexes)
        builder = new SpannableStringBuilder("hello, world");
        builder.replace(4, 6, "", 0, 0);
        assertEquals("hell world", builder.toString());

        builder = new SpannableStringBuilder("hello, world");
        builder.replace(4, 6, "any string", 5, 5);
        assertEquals("hell world", builder.toString());

        // Inserting in place (no deletion)
        builder = new SpannableStringBuilder("hello, world");
        builder.replace(3, 3, "any string", 0, 0);
        assertEquals("hello, world", builder.toString());

        builder = new SpannableStringBuilder("hello, world");
        builder.replace(7, 7, "nice ", 0, 5);
        assertEquals("hello, nice world", builder.toString());

        builder = new SpannableStringBuilder("hello, world");
        builder.replace(0, 0, "say ", 1, 4);
        assertEquals("ay hello, world", builder.toString());

        try {
            builder.replace(0, 5, text, 10, 3);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.replace(0, 5, text, -1, 100);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.replace(-1, 100, text, 10, 3);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.replace(3, 1, text, -1, 100);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        // unexpected IndexOutOfBoundsException
        try {
            builder.replace(0, 5, null, 1, 2);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }

    public void testSubSequence() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        CharSequence text = builder.subSequence(0, 2);
        assertNotNull(text);
        assertTrue(text instanceof SpannableStringBuilder);
        assertEquals("he", text.toString());
        try {
            builder.subSequence(2, 0);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }

    public void testGetChars() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        char[] buf = new char[4];
        buf[0] = 'x';

        builder.getChars(0, 3, buf, 1);
        assertEquals("xhel", String.valueOf(buf));

        builder.getChars(1, 5, buf, 0);
        assertEquals("ello", String.valueOf(buf));

        try {
            builder.getChars(-1, 10, buf, 1);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.getChars(3, 2, buf, 0);
            fail("should throw IndexOutOfBoundsException here");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.getChars(1, 2, null, 0);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testAppend1() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.append(",world");
        assertEquals("hello,world", builder.toString());
        try {
            builder.append(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testAppend2() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.append(",world", 1, 3);
        assertEquals("hellowo", builder.toString());

        builder = new SpannableStringBuilder("hello");
        builder.append(",world", 0, 4);
        assertEquals("hello,wor", builder.toString());

        try {
            builder.append(null, 0, 1);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }

        try {
            builder.append(",world", -1, 10);
            fail("should throw StringIndexOutOfBoundsException here");
        } catch (StringIndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.append(",world", 4, 1);
            fail("should throw StringIndexOutOfBoundsException here");
        } catch (StringIndexOutOfBoundsException e) {
            // expected exception
        }
    }

    public void testAppend3() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.append('a');
        builder.append('b');
        builder.append('c');

        assertEquals("helloabc", builder.toString());
        try {
            builder.append(null);
            fail("should throw NullPointerException here");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testClearSpans() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");

        builder.setSpan(mUnderlineSpan, 0, 2, 2);
        assertEquals(0, builder.getSpanStart(mUnderlineSpan));
        assertEquals(2, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(2, builder.getSpanFlags(mUnderlineSpan));

        builder.clearSpans();
        assertEquals(-1, builder.getSpanStart(mUnderlineSpan));
        assertEquals(-1, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(0, builder.getSpanFlags(mUnderlineSpan));
    }

    public void testGetSpanStart() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.setSpan(mUnderlineSpan, 1, 3, 0);
        assertEquals(1, builder.getSpanStart(mUnderlineSpan));
        assertEquals(-1, builder.getSpanStart(mStrikethroughSpan));
        assertEquals(-1, builder.getSpanStart(null));
    }

    public void testAccessFilters() {
        InputFilter[] filters = new InputFilter[100];
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.setFilters(filters);
        assertSame(filters, builder.getFilters());

        try {
            builder.setFilters(null);
            fail("should throw IllegalArgumentException here");
        } catch (IllegalArgumentException e) {
            // expected exception
        }
    }

    public void testRemoveSpan() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");

        builder.setSpan(mUnderlineSpan, 0, 2, 2);
        assertEquals(0, builder.getSpanStart(mUnderlineSpan));
        assertEquals(2, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(2, builder.getSpanFlags(mUnderlineSpan));

        builder.removeSpan(new Object());
        assertEquals(0, builder.getSpanStart(mUnderlineSpan));
        assertEquals(2, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(2, builder.getSpanFlags(mUnderlineSpan));

        builder.removeSpan(mUnderlineSpan);
        assertEquals(-1, builder.getSpanStart(mUnderlineSpan));
        assertEquals(-1, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(0, builder.getSpanFlags(mUnderlineSpan));

        builder.removeSpan(mUnderlineSpan);
        builder.removeSpan(null);
    }

    public void testToString() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        assertEquals("hello", builder.toString());

        builder = new SpannableStringBuilder();
        assertEquals("", builder.toString());
    }

    public void testGetSpanEnd() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.setSpan(mUnderlineSpan, 1, 3, 0);
        assertEquals(3, builder.getSpanEnd(mUnderlineSpan));
        assertEquals(-1, builder.getSpanEnd(mStrikethroughSpan));
        assertEquals(-1, builder.getSpanEnd(null));
    }

    public void testCharAt() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        assertEquals('h', builder.charAt(0));
        assertEquals('e', builder.charAt(1));
        try {
            builder.charAt(10);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.charAt(-1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }

    public void testInsert1() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.insert(1, "abcd", 1, 3);
        assertEquals("hbcello", builder.toString());

        builder = new SpannableStringBuilder("hello");
        builder.insert(2, "abcd", 0, 4);
        assertEquals("heabcdllo", builder.toString());

        try {
            builder.insert(-1, "abcd", 1, 3);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(100, "abcd", 1, 3);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(1, "abcd", 3, 2);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(1, "abcd", -3, 2);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(0, null, 0, 1);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testInsert2() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        builder.insert(1, "abcd");
        assertEquals("habcdello", builder.toString());

        builder = new SpannableStringBuilder("hello");
        builder.insert(5, "abcd");
        assertEquals("helloabcd", builder.toString());

        try {
            builder.insert(-1, "abcd");
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(100, "abcd");
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.insert(0, null);
            fail("should throw NullPointerException");
        } catch (NullPointerException e) {
            // expected exception
        }
    }

    public void testClear() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        assertEquals("hello", builder.toString());
        builder.clear();
        assertEquals("", builder.toString());
    }

    public void testGetSpans() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello, world");
        UnderlineSpan span1 = new UnderlineSpan();
        UnderlineSpan span2 = new UnderlineSpan();
        builder.setSpan(span1, 1, 2, Spanned.SPAN_POINT_POINT);
        builder.setSpan(span2, 4, 8, Spanned.SPAN_MARK_POINT);
        UnderlineSpan[] underlineSpans = builder.getSpans(0, 10, UnderlineSpan.class);
        assertEquals(2, underlineSpans.length);
        assertSame(span1, underlineSpans[0]);
        assertSame(span2, underlineSpans[1]);

        StrikethroughSpan[] strikeSpans = builder.getSpans(0, 10, StrikethroughSpan.class);
        assertEquals(0, strikeSpans.length);

        builder.getSpans(-1, 100, UnderlineSpan.class);

        builder.getSpans(4, 1, UnderlineSpan.class);
    }

    public void testLength() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello");
        assertEquals(5, builder.length());
        builder.clear();
        assertEquals(0, builder.length());
    }

    public void testDelete() {
        SpannableStringBuilder builder = new SpannableStringBuilder("hello,world");
        assertEquals("hello,world", builder.toString());
        builder.delete(0, 5);
        assertEquals(",world", builder.toString());

        builder = new SpannableStringBuilder("hello,world");
        assertEquals("hello,world", builder.toString());
        builder.delete(2, 4);
        assertEquals("heo,world", builder.toString());

        try {
            builder.delete(-1, 100);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }

        try {
            builder.delete(4, 1);
            fail("should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // expected exception
        }
    }
}
