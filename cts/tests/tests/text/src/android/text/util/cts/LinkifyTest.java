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

package android.text.util.cts;


import android.test.AndroidTestCase;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.text.util.Linkify.MatchFilter;
import android.text.util.Linkify.TransformFilter;
import android.widget.TextView;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test {@link Linkify}.
 */
public class LinkifyTest extends AndroidTestCase {
    private static final Pattern LINKIFY_TEST_PATTERN = Pattern.compile(
            "(test:)?[a-zA-Z0-9]+(\\.pattern)?");

    private MatchFilter mMatchFilterStartWithDot = new MatchFilter() {
        public final boolean acceptMatch(final CharSequence s, final int start, final int end) {
            if (start == 0) {
                return true;
            }

            if (s.charAt(start - 1) == '.') {
                return false;
            }

            return true;
        }
    };

    private TransformFilter mTransformFilterUpperChar = new TransformFilter() {
        public final String transformUrl(final Matcher match, String url) {
            StringBuilder buffer = new StringBuilder();
            String matchingRegion = match.group();

            for (int i = 0, size = matchingRegion.length(); i < size; i++) {
                char character = matchingRegion.charAt(i);

                if (character == '.' || Character.isLowerCase(character)
                        || Character.isDigit(character)) {
                    buffer.append(character);
                }
            }
            return buffer.toString();
        }
    };

    public void testConstructor() {
        new Linkify();
    }

    public void testAddLinks1() {
        SpannableString spannable = new SpannableString("name@gmail.com, "
                + "www.google.com, http://www.google.com/language_tools?hl=en, ");

        assertTrue(Linkify.addLinks(spannable, Linkify.WEB_URLS));
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());
        assertEquals("http://www.google.com/language_tools?hl=en", spans[1].getURL());

        assertTrue(Linkify.addLinks(spannable, Linkify.EMAIL_ADDRESSES));
        spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("mailto:name@gmail.com", spans[0].getURL());

        try {
            Linkify.addLinks((Spannable) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertFalse(Linkify.addLinks((Spannable) null, 0));
    }

    public void testAddLinks2() {
        String text = "www.google.com, name@gmail.com";
        TextView tv = new TextView(mContext);
        tv.setText(text);

        assertTrue(Linkify.addLinks(tv, Linkify.WEB_URLS));
        URLSpan[] spans = ((Spannable)tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("http://www.google.com", spans[0].getURL());

        SpannableString spannable = SpannableString.valueOf(text);
        tv.setText(spannable);
        assertTrue(Linkify.addLinks(tv, Linkify.EMAIL_ADDRESSES));
        spans = ((Spannable)tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(1, spans.length);
        assertEquals("mailto:name@gmail.com", spans[0].getURL());

        try {
            Linkify.addLinks((TextView)null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertFalse(Linkify.addLinks((TextView)null, 0));
    }

    public void testAddLinks3() {
        String text = "Alan, Charlie";
        TextView tv = new TextView(mContext);
        tv.setText(text);

        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:");
        URLSpan[] spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:Alan", spans[0].getURL());
        assertEquals("test:Charlie", spans[1].getURL());

        text = "google.pattern, test:AZ0101.pattern";
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:");
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());

        try {
            Linkify.addLinks((TextView) null, LINKIFY_TEST_PATTERN, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            Linkify.addLinks(tv, null, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        tv = new TextView(mContext);
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, null);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());
    }

    public void testAddLinks4() {
        TextView tv = new TextView(mContext);

        String text =  "FilterUpperCase.pattern, 12.345.pattern";
        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        URLSpan[] spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());

        try {
            Linkify.addLinks((TextView) null, LINKIFY_TEST_PATTERN, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            Linkify.addLinks(tv, null, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, null,
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("ilterpperase.pattern", spans[0].getURL());
        assertEquals("12", spans[1].getURL());

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:", null, mTransformFilterUpperChar);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(3, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
        assertEquals("test:345.pattern", spans[2].getURL());

        tv.setText(text);
        Linkify.addLinks(tv, LINKIFY_TEST_PATTERN, "Test:", mMatchFilterStartWithDot, null);
        spans = ((Spannable) tv.getText()).getSpans(0, text.length(), URLSpan.class);
        assertEquals(2, spans.length);
        assertEquals("test:FilterUpperCase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
    }

    public void testAddLinks5() {
        String text = "google.pattern, test:AZ0101.pattern";

        SpannableString spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:");
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());

        try {
            Linkify.addLinks((Spannable)null, LINKIFY_TEST_PATTERN, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        try {
            Linkify.addLinks(spannable, null, "Test:");
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
        }

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("google.pattern", spans[0].getURL());
        assertEquals("test:AZ0101.pattern", spans[1].getURL());
    }

    public void testAddLinks6() {
        String text = "FilterUpperCase.pattern, 12.345.pattern";

        SpannableString spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:",
                mMatchFilterStartWithDot, mTransformFilterUpperChar);
        URLSpan[] spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());

        try {
            Linkify.addLinks((Spannable)null, LINKIFY_TEST_PATTERN, "Test:",
                    mMatchFilterStartWithDot, mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        try {
            Linkify.addLinks(spannable, null, "Test:", mMatchFilterStartWithDot,
                    mTransformFilterUpperChar);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, null, mMatchFilterStartWithDot,
                mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("ilterpperase.pattern", spans[0].getURL());
        assertEquals("12", spans[1].getURL());

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:", null, mTransformFilterUpperChar);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(3, spans.length);
        assertEquals("test:ilterpperase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
        assertEquals("test:345.pattern", spans[2].getURL());

        spannable = new SpannableString(text);
        Linkify.addLinks(spannable, LINKIFY_TEST_PATTERN, "Test:", mMatchFilterStartWithDot, null);
        spans = (spannable.getSpans(0, spannable.length(), URLSpan.class));
        assertEquals(2, spans.length);
        assertEquals("test:FilterUpperCase.pattern", spans[0].getURL());
        assertEquals("test:12", spans[1].getURL());
    }

    public void testAddLinks7() {
        String numbersInvalid = "123456789 not a phone number";
        String numbersUKLocal = "tel:(0812)1234560 (0812)1234561";
        String numbersUSLocal = "tel:(812)1234562 (812)123.4563 "
                + " tel:(800)5551210 (800) 555-1211 555-1212";
        String numbersIntl = "tel:+4408121234564 +44-0812-123-4565"
                + " tel:+18005551213 +1-800-555-1214";
        SpannableString spannable = new SpannableString(
                numbersInvalid
                + " " + numbersUKLocal
                + " " + numbersUSLocal
                + " " + numbersIntl);

        // phonenumber linkify is locale-dependent
        if (Locale.US.equals(Locale.getDefault())) {
            assertTrue(Linkify.addLinks(spannable, Linkify.PHONE_NUMBERS));
            URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
            assertEquals(9, spans.length);
            assertEquals("tel:8121234562", spans[0].getURL());
            assertEquals("tel:8121234563", spans[1].getURL());
            assertEquals("tel:8005551210", spans[2].getURL());
            assertEquals("tel:8005551211", spans[3].getURL());
            assertEquals("tel:5551212", spans[4].getURL());
            assertEquals("tel:+4408121234564", spans[5].getURL());
            assertEquals("tel:+4408121234565", spans[6].getURL());
            assertEquals("tel:+18005551213", spans[7].getURL());
            assertEquals("tel:+18005551214", spans[8].getURL());
        }

        try {
            Linkify.addLinks((Spannable) null, Linkify.WEB_URLS);
            fail("Should throw NullPointerException!");
        } catch (NullPointerException e) {
            // expect
        }

        assertFalse(Linkify.addLinks((Spannable) null, 0));
    }
}
