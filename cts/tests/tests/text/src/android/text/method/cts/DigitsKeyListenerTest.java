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

package android.text.method.cts;

import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.cts.KeyListenerTestCase;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;

/**
 * Test {@link DigitsKeyListener}.
 */
public class DigitsKeyListenerTest extends KeyListenerTestCase {
    public void testConstructor() {
        new DigitsKeyListener();

        new DigitsKeyListener(true, true);
    }

    /*
     * Check point:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'.
     * 1. filter "123456", return null.
     * 2. filter "a1b2c3d", return "123"
     * 3. filter "-a1.b2c3d", return "123"
     * 4. filter "+a1.b2c3d", return "123"
     * 5. filter Spanned("-a1.b2c3d"), return Spanned("123") and copy spans.
     * 6. filter "", return null
     */
    public void testFilter1() {
        String source = "123456";
        String destString = "dest string";

        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance();
        SpannableString dest = new SpannableString(destString);
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "a1b2c3d";
        assertEquals("123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "-a1.b2c3d";
        assertEquals("123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "+a1.b2c3d";
        assertEquals("123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        Object what = new Object();
        Spannable spannableSource = new SpannableString(source);
        spannableSource.setSpan(what, 0, spannableSource.length(), Spanned.SPAN_POINT_POINT);
        Spanned filtered = (Spanned) digitsKeyListener.filter(spannableSource,
                0, spannableSource.length(), dest, 0, dest.length());
        assertEquals("123", filtered.toString());
        assertEquals(Spanned.SPAN_POINT_POINT, filtered.getSpanFlags(what));
        assertEquals(0, filtered.getSpanStart(what));
        assertEquals("123".length(), filtered.getSpanEnd(what));

        assertNull(digitsKeyListener.filter("", 0, 0, dest, 0, dest.length()));
        assertEquals(destString, dest.toString());
    }

    /*
     * Check point:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+'.
     * 1. filter "-123456", return null
     * 2. filter "+123456", return null
     * 3. filter "-a1.b2c3d", return "-123"
     * 4. filter "-a1-b2c3d", return "-123"
     * 5. filter "+a1-b2c3d", return "+123"
     * 6. filter "5-a1-b2c3d", return "5123"
     * 7. filter "5-a1+b2c3d", return "5123"
     * 8. filter "+5-a1+b2c3d", return "+5123"
     * 9. filter Spanned("5-a1-b2c3d"), return Spanned("5123") and copy spans.
     * 10. filter "", return null
     * 11. filter "-123456" but dest has '-' after dend, return ""
     * 12. filter "-123456" but dest has '+' after dend, return ""
     * 13. filter "-123456" but dest has '-' before dstart, return "123456"
     * 14. filter "+123456" but dest has '-' before dstart, return "123456"
     */
    public void testFilter2() {
        String source = "-123456";
        String destString = "dest string without sign and decimal";

        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(true, false);
        SpannableString dest = new SpannableString(destString);
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "+123456";
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "-a1.b2c3d";
        assertEquals("-123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "-a1-b2c3d";
        assertEquals("-123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "+a1-b2c3d";
        assertEquals("+123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "5-a1-b2c3d";
        assertEquals("5123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "5-a1+b2c3d";
        assertEquals("5123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "+5-a1+b2c3d";
        assertEquals("+5123", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "5-a1+b2c3d";
        Object what = new Object();
        Spannable spannableSource = new SpannableString(source);
        spannableSource.setSpan(what, 0, spannableSource.length(), Spanned.SPAN_POINT_POINT);
        Spanned filtered = (Spanned) digitsKeyListener.filter(spannableSource,
                0, spannableSource.length(), dest, 0, dest.length());
        assertEquals("5123", filtered.toString());
        assertEquals(Spanned.SPAN_POINT_POINT, filtered.getSpanFlags(what));
        assertEquals(0, filtered.getSpanStart(what));
        assertEquals("5123".length(), filtered.getSpanEnd(what));

        assertNull(digitsKeyListener.filter("", 0, 0, dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "-123456";
        String endSign = "789-";
        dest = new SpannableString(endSign);
        assertEquals("", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endSign, dest.toString());

        endSign = "789+";
        dest = new SpannableString(endSign);
        assertEquals("", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endSign, dest.toString());

        String startSign = "-789";
        dest = new SpannableString(startSign);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startSign, dest.toString());

        source = "+123456";
        dest = new SpannableString(startSign);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startSign, dest.toString());
    }

    /*
     * Check point:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'.
     * 1. filter "123.456", return null
     * 2. filter "-a1.b2c3d", return "1.23"
     * 3. filter "+a1.b2c3d", return "1.23"
     * 4. filter "a1.b2c3d.", return "123."
     * 5. filter "5.a1.b2c3d", return "51.23"
     * 6. filter Spanned("5.a1.b2c3d"), return Spanned("51.23") and copy spans.
     * 7. filter "", return null
     * 8. filter "123.456" but dest has '.' after dend, return "123456"
     * 9. filter "123.456" but dest has '.' before dstart, return "123456"
     */
    public void testFilter3() {
        String source = "123.456";
        String destString = "dest string without sign and decimal";

        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(false, true);
        SpannableString dest = new SpannableString(destString);
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "-a1.b2c3d";
        assertEquals("1.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "+a1.b2c3d";
        assertEquals("1.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "a1.b2c3d.";
        assertEquals("123.", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "5.a1.b2c3d";
        assertEquals("51.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        Object what = new Object();
        Spannable spannableSource = new SpannableString(source);
        spannableSource.setSpan(what, 0, spannableSource.length(), Spanned.SPAN_POINT_POINT);
        Spanned filtered = (Spanned) digitsKeyListener.filter(spannableSource,
                0, spannableSource.length(), dest, 0, dest.length());
        assertEquals("51.23", filtered.toString());
        assertEquals(Spanned.SPAN_POINT_POINT, filtered.getSpanFlags(what));
        assertEquals(0, filtered.getSpanStart(what));
        assertEquals("51.23".length(), filtered.getSpanEnd(what));

        assertNull(digitsKeyListener.filter("", 0, 0, dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "123.456";
        String endDecimal = "789.";
        dest = new SpannableString(endDecimal);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endDecimal, dest.toString());

        String startDecimal = ".789";
        dest = new SpannableString(startDecimal);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startDecimal, dest.toString());
    }

    /*
     * Check point:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '-',
     * '+'.
     * 1. filter "-123.456", return null
     * 2. filter "+123.456", return null
     * 3. filter "-a1.b2c3d", return "-1.23"
     * 4. filter "+a1.b2c3d", return "+1.23"
     * 5. filter "a1.b-2c+3d.", return "123."
     * 6. filter "-5.a1.b2c+3d", return "-51.23"
     * 7. filter "+5.a1.b2c-3d", return "+51.23"
     * 8. filter Spanned("-5.a1.b2c3d"), return Spanned("-51.23") and copy spans.
     * 9. filter "", return null
     * 10. filter "-123.456" but dest has '.' after dend, return "-123456"
     * 11. filter "-123.456" but dest has '.' before dstart, return "123456"
     * 12. filter "+123.456" but dest has '.' after dend, return "+123456"
     * 13. filter "+123.456" but dest has '.' before dstart, return "123456"
     * 14. filter "-123.456" but dest has '-' after dend, return ""
     * 15. filter "-123.456" but dest has '+' after dend, return ""
     * 16. filter "-123.456" but dest has '-' before dstart, return "123.456"
     * 17. filter "+123.456" but dest has '-' before dstart, return "123.456"
     */
    public void testFilter4() {
        String source = "-123.456";
        String destString = "dest string without sign and decimal";

        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(true, true);
        SpannableString dest = new SpannableString(destString);
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "+123.456";
        assertNull(digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "-a1.b2c3d";
        assertEquals("-1.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "a1.b-2c+3d.";
        assertEquals("123.", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "-5.a1.b2c+3d";
        assertEquals("-51.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "+5.a1.b2c-3d";
        assertEquals("+51.23", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length())).toString());
        assertEquals(destString, dest.toString());

        source = "-5.a1.b2c+3d";
        Object what = new Object();
        Spannable spannableSource = new SpannableString(source);
        spannableSource.setSpan(what, 0, spannableSource.length(), Spanned.SPAN_POINT_POINT);
        Spanned filtered = (Spanned) digitsKeyListener.filter(spannableSource,
                0, spannableSource.length(), dest, 0, dest.length());
        assertEquals("-51.23", filtered.toString());
        assertEquals(Spanned.SPAN_POINT_POINT, filtered.getSpanFlags(what));
        assertEquals(0, filtered.getSpanStart(what));
        assertEquals("-51.23".length(), filtered.getSpanEnd(what));

        assertNull(digitsKeyListener.filter("", 0, 0, dest, 0, dest.length()));
        assertEquals(destString, dest.toString());

        source = "-123.456";
        String endDecimal = "789.";
        dest = new SpannableString(endDecimal);
        assertEquals("-123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endDecimal, dest.toString());

        String startDecimal = ".789";
        dest = new SpannableString(startDecimal);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startDecimal, dest.toString());

        source = "+123.456";
        endDecimal = "789.";
        dest = new SpannableString(endDecimal);
        assertEquals("+123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endDecimal, dest.toString());

        startDecimal = ".789";
        dest = new SpannableString(startDecimal);
        assertEquals("123456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startDecimal, dest.toString());

        source = "-123.456";
        String endSign = "789-";
        dest = new SpannableString(endSign);
        assertEquals("", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endSign, dest.toString());

        endSign = "789+";
        dest = new SpannableString(endSign);
        assertEquals("", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 0, dest.length() - 1)).toString());
        assertEquals(endSign, dest.toString());

        String startSign = "-789";
        dest = new SpannableString(startSign);
        assertEquals("123.456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startSign, dest.toString());

        source = "+123.456";
        dest = new SpannableString(startSign);
        assertEquals("123.456", (digitsKeyListener.filter(source, 0, source.length(),
                dest, 1, dest.length())).toString());
        assertEquals(startSign, dest.toString());
    }

    /*
     * Scenario description:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'.
     *  1. Press '-' key and this key could not be accepted.
     *  2. Press '1' key and check if the content of TextView becomes "1"
     *  3. Press '.' key and this key could not be accepted.
     *  4. Press '2' key and check if the content of TextView becomes "12"
     */
    public void testDigitsKeyListener1() {
        final DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance();

        setKeyListenerSync(digitsKeyListener);
        assertEquals("", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("1", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("1", mTextView.getText().toString());

        // press '2' key.
        sendKeys(KeyEvent.KEYCODE_2);
        assertEquals("12", mTextView.getText().toString());
    }

    /*
     * Scenario description:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+'.
     *  1. Press '-' key and check if the content of TextView becomes "-"
     *  2. Press '1' key and check if the content of TextView becomes "-1"
     *  3. Press '.' key and this key could not be accepted.
     *  4. Press '+' key and this key could not be accepted.
     *  5. Press '2' key and check if the content of TextView becomes "-12"
     *  6. Press '-' key and this key could not be accepted,
     *     because text view accepts minus sign iff it at the beginning.
     */
    public void testDigitsKeyListener2() {
        final DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(true, false);

        setKeyListenerSync(digitsKeyListener);
        assertEquals("", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("-", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("-1", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("-1", mTextView.getText().toString());

        // press '+' key.
        sendKeys(KeyEvent.KEYCODE_PLUS);
        assertEquals("-1", mTextView.getText().toString());

        // press '2' key.
        sendKeys(KeyEvent.KEYCODE_2);
        assertEquals("-12", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("-12", mTextView.getText().toString());
    }

    /*
     * Scenario description:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.'.
     *  1. Press '-' key and check if the content of TextView becomes ""
     *  2. Press '+' key and check if the content of TextView becomes ""
     *  3. Press '1' key and check if the content of TextView becomes "1"
     *  4. Press '.' key and check if the content of TextView becomes "1."
     *  5. Press '2' key and check if the content of TextView becomes "1.2"
     *  6. Press '.' key and this key could not be accepted,
     *     because text view accepts only one decimal point per field.
     */
    public void testDigitsKeyListener3() {
        final DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(false, true);

        setKeyListenerSync(digitsKeyListener);
        assertEquals("", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("", mTextView.getText().toString());

        // press '+' key.
        sendKeys(KeyEvent.KEYCODE_PLUS);
        assertEquals("", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("1", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("1.", mTextView.getText().toString());

        // press '2' key.
        sendKeys(KeyEvent.KEYCODE_2);
        assertEquals("1.2", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("1.2", mTextView.getText().toString());
    }

    /*
     * Scenario description:
     * Current accepted characters are '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+',
     * '.'.
     *  1. Press '+' key and check if the content of TextView becomes "+"
     *  2. Press '1' key and check if the content of TextView becomes "+1"
     *  3. Press '.' key and this key could not be accepted.
     *  4. Press '2' key and check if the content of TextView becomes "+12"
     *  5. Press '-' key and this key could not be accepted,
     *     because text view accepts minus sign iff it at the beginning.
     *  6. Press '.' key and this key could not be accepted,
     *     because text view accepts only one decimal point per field.
     */
    public void testDigitsKeyListener4() {
        final DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(true, true);

        setKeyListenerSync(digitsKeyListener);
        assertEquals("", mTextView.getText().toString());

        // press '+' key.
        sendKeys(KeyEvent.KEYCODE_PLUS);
        assertEquals("+", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("+1", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("+1.", mTextView.getText().toString());

        // press '2' key.
        sendKeys(KeyEvent.KEYCODE_2);
        assertEquals("+1.2", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("+1.2", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("+1.2", mTextView.getText().toString());
    }

    /*
     * Scenario description:
     * Current accepted characters are '5', '6', '7', '8', '9'.
     *  1. Press '1' key and this key could not be accepted.
     *  2. Press '5' key and check if the content of TextView becomes "5"
     *  3. Press '.' key and this key could not be accepted.
     *  4. Press '-' key and this key could not be accepted.
     *  5. remove DigitsKeyListener and Press '5' key, this key will not be accepted
     */
    public void testDigitsKeyListener5() {
        final String accepted = "56789";
        final DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(accepted);

        setKeyListenerSync(digitsKeyListener);
        assertEquals("", mTextView.getText().toString());

        // press '1' key.
        sendKeys(KeyEvent.KEYCODE_1);
        assertEquals("", mTextView.getText().toString());

        // press '5' key.
        sendKeys(KeyEvent.KEYCODE_5);
        assertEquals("5", mTextView.getText().toString());

        // press '.' key.
        sendKeys(KeyEvent.KEYCODE_PERIOD);
        assertEquals("5", mTextView.getText().toString());

        // press '-' key.
        sendKeys(KeyEvent.KEYCODE_MINUS);
        assertEquals("5", mTextView.getText().toString());

        // remove DigitsKeyListener
        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setKeyListener(null);
                mTextView.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();
        assertEquals("5", mTextView.getText().toString());

        // press '5' key.
        sendKeys(KeyEvent.KEYCODE_5);
        assertEquals("5", mTextView.getText().toString());
    }

    public void testGetInstance1() {
        DigitsKeyListener listener1 = DigitsKeyListener.getInstance();
        DigitsKeyListener listener2 = DigitsKeyListener.getInstance();

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);
    }

    public void testGetInstance2() {
        DigitsKeyListener listener1 = DigitsKeyListener.getInstance(true, true);
        DigitsKeyListener listener2 = DigitsKeyListener.getInstance(true, true);

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);

        listener1 = DigitsKeyListener.getInstance(true, false);
        listener2 = DigitsKeyListener.getInstance(true, false);

        assertNotNull(listener1);
        assertNotNull(listener2);
        assertSame(listener1, listener2);
    }

    public void testGetInstance3() {
        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance("abcdefg");
        assertNotNull(digitsKeyListener);

        digitsKeyListener = DigitsKeyListener.getInstance("Android Test");
        assertNotNull(digitsKeyListener);
    }

    public void testGetAcceptedChars() {
        MockDigitsKeyListener mockDigitsKeyListener = new MockDigitsKeyListener();

        final char[][] expected = new char[][] {
            new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' },
            new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+' },
            new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' },
            new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '+', '.' },
        };

        TextMethodUtils.assertEquals(expected[0], mockDigitsKeyListener.getAcceptedChars());

        mockDigitsKeyListener = new MockDigitsKeyListener(true, false);
        TextMethodUtils.assertEquals(expected[1], mockDigitsKeyListener.getAcceptedChars());

        mockDigitsKeyListener = new MockDigitsKeyListener(false, true);
        TextMethodUtils.assertEquals(expected[2], mockDigitsKeyListener.getAcceptedChars());

        mockDigitsKeyListener = new MockDigitsKeyListener(true, true);
        TextMethodUtils.assertEquals(expected[3], mockDigitsKeyListener.getAcceptedChars());
    }

    public void testGetInputType() {
        DigitsKeyListener digitsKeyListener = DigitsKeyListener.getInstance(false, false);
        int expected = InputType.TYPE_CLASS_NUMBER;
        assertEquals(expected, digitsKeyListener.getInputType());

        digitsKeyListener = DigitsKeyListener.getInstance(true, false);
        expected = InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED;
        assertEquals(expected, digitsKeyListener.getInputType());

        digitsKeyListener = DigitsKeyListener.getInstance(false, true);
        expected = InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        assertEquals(expected, digitsKeyListener.getInputType());

        digitsKeyListener = DigitsKeyListener.getInstance(true, true);
        expected = InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_SIGNED
                | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        assertEquals(expected, digitsKeyListener.getInputType());
    }

    /**
     * A mocked {@link android.text.method.DigitsKeyListener} for testing purposes.
     *
     * Allows {@link DigitsKeyListenerTest} to call
     * {@link android.text.method.DigitsKeyListener#getAcceptedChars()}.
     */
    private class MockDigitsKeyListener extends DigitsKeyListener {
        public MockDigitsKeyListener() {
            super();
        }

        public MockDigitsKeyListener(boolean sign, boolean decimal) {
            super(sign, decimal);
        }

        @Override
        protected char[] getAcceptedChars() {
            return super.getAcceptedChars();
        }
    }
}
