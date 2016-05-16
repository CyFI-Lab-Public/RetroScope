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

package android.widget.cts;

import com.android.cts.stub.R;


import org.xmlpull.v1.XmlPullParser;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.cts.util.PollingCheck;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.DialerFilter;
import android.widget.EditText;
import android.widget.RelativeLayout;

public class DialerFilterTest extends ActivityInstrumentationTestCase2<DialerFilterStubActivity> {
    private Activity mActivity;
    private Instrumentation mInstrumentation;
    private DialerFilter mDialerFilter;

    public DialerFilterTest() {
        super("com.android.cts.stub", DialerFilterStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
        new PollingCheck() {
            @Override
                protected boolean check() {
                return mActivity.hasWindowFocus();
            }
        }.run();
        mInstrumentation = getInstrumentation();

        mDialerFilter = (DialerFilter) mActivity.findViewById(R.id.dialer_filter);
    }

    public void testConstructor() {
        final XmlPullParser parser = mActivity.getResources().getXml(R.layout.dialerfilter_layout);
        final AttributeSet attrs = Xml.asAttributeSet(parser);

        new DialerFilter(mActivity);
        new DialerFilter(mActivity, attrs);
    }

    public void testIsQwertyKeyboard() {
        // Simply call the method. Return value may depend on the default keyboard.
        mDialerFilter.isQwertyKeyboard();
    }

    public void testOnKeyUpDown() {
        // The exact behavior depends on the implementation of DialerKeyListener and
        // TextKeyListener, but even that may be changed. Simply assert basic scenarios.

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mDialerFilter.setMode(DialerFilter.DIGITS_ONLY);
                mDialerFilter.requestFocus();
            }
        });
        mInstrumentation.waitForIdleSync();

        assertTrue(mDialerFilter.hasFocus());

        mInstrumentation.sendStringSync("123");
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("123", mDialerFilter.getDigits().toString());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mDialerFilter.clearText();
                mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
            }
        });
        mInstrumentation.waitForIdleSync();

        // 12-key support
        KeyCharacterMap keymap
                = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        if (keymap.getKeyboardType() == KeyCharacterMap.NUMERIC) {
            // "adg" in case of 12-key(NUMERIC) keyboard
            mInstrumentation.sendStringSync("234");
        }
        else {
            mInstrumentation.sendStringSync("adg");
        }
        assertEquals("ADG", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mDialerFilter.clearText();
                mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
            }
        });
        mInstrumentation.waitForIdleSync();

        // 12-key support
        if (keymap.getKeyboardType() == KeyCharacterMap.NUMERIC) {
            // "adg" in case of 12-key(NUMERIC) keyboard
            mInstrumentation.sendStringSync("234");
        }
        else {
            mInstrumentation.sendStringSync("adg");
        }
        assertEquals("ADG", mDialerFilter.getLetters().toString());
        // A, D, K may map to numbers on some keyboards. Don't test.

        mInstrumentation.runOnMainSync(new Runnable() {
            public void run() {
                mDialerFilter.clearText();
                mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
            }
        });
        mInstrumentation.waitForIdleSync();

        mInstrumentation.sendStringSync("123");
        // 1, 2, 3 may map to letters on some keyboards. Don't test.
        assertEquals("123", mDialerFilter.getDigits().toString());
    }

    @UiThreadTest
    public void testAccessMode() {
        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS_NO_LETTERS);
        assertEquals(DialerFilter.DIGITS_AND_LETTERS_NO_LETTERS, mDialerFilter.getMode());

        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        assertEquals(DialerFilter.DIGITS_AND_LETTERS, mDialerFilter.getMode());

        mDialerFilter.setMode(-1);
        assertEquals(-1, mDialerFilter.getMode());
    }

    @UiThreadTest
    public void testGetLetters() {
        assertEquals("", mDialerFilter.getLetters().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("ANDROID");
        assertEquals("ANDROID", mDialerFilter.getLetters().toString());
    }

    @UiThreadTest
    public void testGetDigits() {
        assertEquals("", mDialerFilter.getDigits().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("12345");
        assertEquals("12345", mDialerFilter.getDigits().toString());
    }

    @UiThreadTest
    public void testGetFilterText() {
        assertEquals("", mDialerFilter.getFilterText().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("CTS12345");
        assertEquals("CTS12345", mDialerFilter.getFilterText().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_ONLY);
        assertEquals("12345", mDialerFilter.getFilterText().toString());

        mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
        assertEquals("CTS12345", mDialerFilter.getFilterText().toString());
    }

    @UiThreadTest
    public void testAppend() {
        mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
        mDialerFilter.append("ANDROID");
        assertEquals("ANDROID", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());
        assertEquals("ANDROID", mDialerFilter.getFilterText().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_ONLY);
        mDialerFilter.append("123");
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("123", mDialerFilter.getDigits().toString());
        assertEquals("123", mDialerFilter.getFilterText().toString());

        mDialerFilter.clearText();
        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("ABC123DEF456GHI789");
        assertEquals("ABC123DEF456GHI789", mDialerFilter.getLetters().toString());
        assertEquals("123456789", mDialerFilter.getDigits().toString());
        assertEquals("ABC123DEF456GHI789", mDialerFilter.getFilterText().toString());

        mDialerFilter.clearText();
        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS_NO_DIGITS);
        mDialerFilter.append("ABC123DEF456GHI789");
        assertEquals("ABC123DEF456GHI789", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());
        assertEquals("ABC123DEF456GHI789", mDialerFilter.getFilterText().toString());

        mDialerFilter.clearText();
        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS_NO_LETTERS);
        mDialerFilter.append("ABC123DEF456GHI789");
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("123456789", mDialerFilter.getDigits().toString());
        assertEquals("", mDialerFilter.getFilterText().toString());

        mDialerFilter.clearText();
        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("");
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());
        assertEquals("", mDialerFilter.getFilterText().toString());

        try {
            mDialerFilter.append(null);
            fail("A NullPointerException should be thrown out.");
        } catch (final NullPointerException e) {
            // expected, test success.
        }
    }

    @UiThreadTest
    public void testClearText() {
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());

        mDialerFilter.setMode(DialerFilter.DIGITS_AND_LETTERS);
        mDialerFilter.append("CTS12345");
        assertEquals("CTS12345", mDialerFilter.getLetters().toString());
        assertEquals("12345", mDialerFilter.getDigits().toString());

        mDialerFilter.clearText();
        assertEquals("", mDialerFilter.getLetters().toString());
        assertEquals("", mDialerFilter.getDigits().toString());
        assertEquals(DialerFilter.DIGITS_AND_LETTERS, mDialerFilter.getMode());
    }

    @UiThreadTest
    public void testSetLettersWatcher() {
        MockTextWatcher tw = new MockTextWatcher("A");

        Spannable span = (Spannable) mDialerFilter.getLetters();
        assertEquals(-1, span.getSpanStart(tw));
        assertEquals(-1, span.getSpanEnd(tw));

        mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
        mDialerFilter.setLettersWatcher(tw);
        mDialerFilter.append("ANDROID");
        assertEquals("ANDROID", tw.getText());

        span = (Spannable) mDialerFilter.getLetters();
        assertEquals(0, span.getSpanStart(tw));
        assertEquals(mDialerFilter.getLetters().length(), span.getSpanEnd(tw));
        assertEquals("ANDROID", span.toString());

        tw = new MockTextWatcher("");
        mDialerFilter.setLettersWatcher(tw);
        mDialerFilter.append("");
        assertEquals("", tw.getText());

        try {
            mDialerFilter.setLettersWatcher(new MockTextWatcher(null));
            mDialerFilter.append(null);
            fail("A NullPointerException should be thrown out.");
        } catch (final NullPointerException e) {
            // expected, test success.
        }
    }

    @UiThreadTest
    public void testSetDigitsWatcher() {
        final MockTextWatcher tw = new MockTextWatcher("9");

        Spannable span = (Spannable) mDialerFilter.getDigits();
        assertEquals(-1, span.getSpanStart(tw));
        assertEquals(-1, span.getSpanEnd(tw));

        mDialerFilter.setDigitsWatcher(tw);
        assertEquals(0, span.getSpanStart(tw));
        assertEquals(mDialerFilter.getDigits().length(), span.getSpanEnd(tw));

        mDialerFilter.setMode(DialerFilter.DIGITS_ONLY);
        mDialerFilter.append("12345");
        assertEquals("12345", tw.getText());
    }

    @UiThreadTest
    public void testSetFilterWatcher() {
        final MockTextWatcher tw = new MockTextWatcher("A");

        Spannable span = (Spannable) mDialerFilter.getLetters();
        assertEquals(-1, span.getSpanStart(tw));
        assertEquals(-1, span.getSpanEnd(tw));

        mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
        mDialerFilter.setFilterWatcher(tw);
        mDialerFilter.append("ANDROID");
        assertEquals("ANDROID", tw.getText());
        span = (Spannable) mDialerFilter.getLetters();

        assertEquals(0, span.getSpanStart(tw));
        assertEquals(mDialerFilter.getLetters().length(), span.getSpanEnd(tw));

        mDialerFilter.setMode(DialerFilter.DIGITS_ONLY);
        mDialerFilter.setFilterWatcher(tw);
        mDialerFilter.append("12345");
        assertEquals("12345", tw.getText());
    }

    @UiThreadTest
    public void testRemoveFilterWatcher() {
        final MockTextWatcher tw = new MockTextWatcher("A");

        Spannable span = (Spannable) mDialerFilter.getLetters();
        assertEquals(-1, span.getSpanStart(tw));
        assertEquals(-1, span.getSpanEnd(tw));

        mDialerFilter.setMode(DialerFilter.LETTERS_ONLY);
        mDialerFilter.setFilterWatcher(tw);
        mDialerFilter.append("ANDROID");
        assertEquals("ANDROID", tw.getText());

        span = (Spannable) mDialerFilter.getLetters();
        assertEquals(0, span.getSpanStart(tw));
        assertEquals(mDialerFilter.getLetters().length(), span.getSpanEnd(tw));

        mDialerFilter.removeFilterWatcher(tw);
        mDialerFilter.append("GOLF");
        assertEquals("ANDROID", tw.getText());

        assertEquals(-1, span.getSpanStart(tw));
        assertEquals(-1, span.getSpanEnd(tw));
    }

    public void testOnFinishInflate() {
        // onFinishInflate() is implementation details, do NOT test
    }

    public void testOnFocusChanged() {
        // onFocusChanged() is implementation details, do NOT test
    }

    @UiThreadTest
    public void testOnModechange() {
        final MockDialerFilter dialerFilter = createMyDialerFilter();
        dialerFilter.onFinishInflate();

        assertEquals(0, dialerFilter.getOldMode());
        assertEquals(MockDialerFilter.DIGITS_AND_LETTERS, dialerFilter.getNewMode());

        dialerFilter.setMode(MockDialerFilter.DIGITS_AND_LETTERS_NO_LETTERS);
        assertEquals(MockDialerFilter.DIGITS_AND_LETTERS, dialerFilter.getOldMode());
        assertEquals(MockDialerFilter.DIGITS_AND_LETTERS_NO_LETTERS, dialerFilter.getNewMode());

        dialerFilter.setMode(MockDialerFilter.DIGITS_AND_LETTERS_NO_DIGITS);
        assertEquals(MockDialerFilter.DIGITS_AND_LETTERS_NO_LETTERS, dialerFilter.getOldMode());
        assertEquals(MockDialerFilter.DIGITS_AND_LETTERS_NO_DIGITS, dialerFilter.getNewMode());
    }

    private MockDialerFilter createMyDialerFilter() {
        final MockDialerFilter dialerFilter = new MockDialerFilter(mActivity);

        final EditText text1 = new EditText(mActivity);
        text1.setId(com.android.internal.R.id.hint);
        final EditText text2 = new EditText(mActivity);
        text2.setId(com.android.internal.R.id.primary);

        dialerFilter.addView(text1, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        dialerFilter.addView(text2, new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        return dialerFilter;
    }

    private class MockTextWatcher implements TextWatcher {
        private String mString;

        public MockTextWatcher(final String s) {
            mString = s;
        }

        public void beforeTextChanged(final CharSequence s, final int start, final int count,
                final int after) {
            Log.d("DialerFilterTest", "MockTextWatcher beforeTextChanged");
        }

        public void onTextChanged(final CharSequence s, final int start, final int before,
                final int count) {
            Log.d("DialerFilterTest", "MockTextWatcher onTextChanged");
            mString = s.toString();
        }

        public void afterTextChanged(final Editable s) {
            Log.d("DialerFilterTest", "MockTextWatcher afterTextChanged");
        }

        public String getText() {
            return mString;
        }
    }

    /**
     * MockDialerFilter for test
     */
    private class MockDialerFilter extends DialerFilter {
        private int mOldMode = 0;
        private int mNewMode = 0;

        public MockDialerFilter(Context context) {
            super(context);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
        }

        @Override
        protected void onModeChange(final int oldMode, final int newMode) {
            super.onModeChange(oldMode, newMode);
            mOldMode = oldMode;
            mNewMode = newMode;
        }

        public int getOldMode() {
            return mOldMode;
        }

        public int getNewMode() {
            return mNewMode;
        }
    }
}
