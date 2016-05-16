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
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.MultiAutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView.CommaTokenizer;
import android.widget.MultiAutoCompleteTextView.Tokenizer;

public class MultiAutoCompleteTextViewTest extends ActivityInstrumentationTestCase2
        <MultiAutoCompleteTextViewStubActivity> {
    private MultiAutoCompleteTextView mMultiAutoCompleteTextView_country;
    private MultiAutoCompleteTextView mMultiAutoCompleteTextView_name;
    private Activity mActivity;

    public MultiAutoCompleteTextViewTest() {
        super("com.android.cts.stub", MultiAutoCompleteTextViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();

        mMultiAutoCompleteTextView_country = (MultiAutoCompleteTextView)mActivity
                .findViewById(R.id.country_edit);
        mMultiAutoCompleteTextView_name = (MultiAutoCompleteTextView)mActivity
                .findViewById(R.id.name_edit);
    }

    public void testConstructor() {
        XmlPullParser parser = mActivity.getResources()
                .getXml(R.layout.multi_auto_complete_text_view_layout);
        AttributeSet attr = Xml.asAttributeSet(parser);

        new MultiAutoCompleteTextView(mActivity);
        new MultiAutoCompleteTextView(mActivity, attr);
        new MultiAutoCompleteTextView(mActivity, attr, 0);

        try {
            new MultiAutoCompleteTextView(null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        try {
            new MultiAutoCompleteTextView(null, null);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success
        }

        try {
            new MultiAutoCompleteTextView(null, null, -1);
            fail("There should be a NullPointerException thrown out.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    @UiThreadTest
    private void setText(MultiAutoCompleteTextView m, CharSequence c) {
        m.setText(c);
        m.setSelection(0, c.length());
    }

    @UiThreadTest
    public void testMultiAutoCompleteTextView() {
        mMultiAutoCompleteTextView_country.setTokenizer(new CommaTokenizer());
        mMultiAutoCompleteTextView_name.setTokenizer(new CommaTokenizer());

        mMultiAutoCompleteTextView_country.setThreshold(3);
        mMultiAutoCompleteTextView_name.setThreshold(2);

        assertFalse(mMultiAutoCompleteTextView_country.enoughToFilter());
        assertFalse(mMultiAutoCompleteTextView_name.enoughToFilter());

        setText(mMultiAutoCompleteTextView_country, "Ar");
        assertFalse(mMultiAutoCompleteTextView_country.enoughToFilter());

        setText(mMultiAutoCompleteTextView_country, "Arg");
        assertTrue(mMultiAutoCompleteTextView_country.enoughToFilter());

        setText(mMultiAutoCompleteTextView_country, "Argentina");
        assertTrue(mMultiAutoCompleteTextView_country.enoughToFilter());

        setText(mMultiAutoCompleteTextView_name, "J");
        assertFalse(mMultiAutoCompleteTextView_name.enoughToFilter());

        setText(mMultiAutoCompleteTextView_name, "Ja");
        assertTrue(mMultiAutoCompleteTextView_name.enoughToFilter());

        setText(mMultiAutoCompleteTextView_name, "Jacky");
        assertTrue(mMultiAutoCompleteTextView_name.enoughToFilter());

        MockValidator v = new MockValidator();
        v.setValid(true);
        mMultiAutoCompleteTextView_name.setValidator(v);

        // There will be an endless loop when using CommaTokenizer as the Tokenizer
        // mMultiAutoCompleteTextView_name.performValidation();
        mMultiAutoCompleteTextView_name.setValidator(null);
    }

    @UiThreadTest
    public void testPerformValidation() {
        MockValidator v = new MockValidator();
        v.setValid(true);
        mMultiAutoCompleteTextView_country.setValidator(v);
        MockTokenizer t = new MockTokenizer();
        mMultiAutoCompleteTextView_country.setTokenizer(t);
        String str = new String("Foo, Android Test, OH");
        mMultiAutoCompleteTextView_country.setText(str);
        mMultiAutoCompleteTextView_country.performValidation();
        assertEquals(str, mMultiAutoCompleteTextView_country.getText().toString());

        v.setValid(false);
        mMultiAutoCompleteTextView_country.performValidation();
        assertEquals(str + ", ", mMultiAutoCompleteTextView_country.getText().toString());
    }

    public void testPerformFiltering() {
        MyMultiAutoCompleteTextView multiAutoCompleteTextView =
            new MyMultiAutoCompleteTextView(mActivity);
        CommaTokenizer t = new CommaTokenizer();
        multiAutoCompleteTextView.setTokenizer(t);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
                R.layout.simple_dropdown_item_1line);
        assertNotNull(adapter);

        multiAutoCompleteTextView.setAdapter(adapter);
        assertNotNull(multiAutoCompleteTextView.getFilter());

        String text = "Android test.";
        multiAutoCompleteTextView.setText(text);
        multiAutoCompleteTextView.setSelection(0, 12);

        multiAutoCompleteTextView.performFiltering(text, KeyEvent.KEYCODE_0);
        assertNotNull(multiAutoCompleteTextView.getFilter());

        multiAutoCompleteTextView.performFiltering(text, 0, text.length(), KeyEvent.KEYCODE_E);
        assertNotNull(multiAutoCompleteTextView.getFilter());
    }

    public void testReplaceText() {
        MyMultiAutoCompleteTextView multiAutoCompleteTextView =
            new MyMultiAutoCompleteTextView(mActivity);
        CommaTokenizer t = new CommaTokenizer();
        multiAutoCompleteTextView.setTokenizer(t);

        String text = "CTS.";
        multiAutoCompleteTextView.setText(text);
        assertEquals(text, multiAutoCompleteTextView.getText().toString());
        multiAutoCompleteTextView.setSelection(0, text.length());

        // set the selection range.
        multiAutoCompleteTextView.replaceText("Android Test.");
        assertEquals("Android Test., ", multiAutoCompleteTextView.getText().toString());

        // do not set the selection range.
        multiAutoCompleteTextView.replaceText("replace test");
        assertEquals("Android Test., replace test, ",
                multiAutoCompleteTextView.getText().toString());
    }

    private class MockTokenizer implements Tokenizer {
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            while (i > 0 && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == ',')) {
                i--;
            }

            while (i > 0 && text.charAt(i - 1) != ',') {
                i--;
            }

            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (text.charAt(i) == ',') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            if (i > 0 && text.charAt(i - 1) == ',') {
                return text;
            } else {
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + ", ");
                    TextUtils.copySpansFrom((Spanned)text, 0, text.length(), Object.class, sp, 0);
                    return sp;
                } else {
                    return text + ", ";
                }
            }
        }
    }

    private class MockValidator implements MultiAutoCompleteTextView.Validator {
        private boolean mIsValid;

        public void setValid(boolean b) {
            mIsValid = b;
        }

        public boolean isValid(CharSequence text) {
            return mIsValid;
        }

        public CharSequence fixText(CharSequence invalidText) {
            return invalidText;
        }
    }

    /**
     * MyMultiAutoCompleteTextView
     */
    private class MyMultiAutoCompleteTextView extends MultiAutoCompleteTextView {
        public MyMultiAutoCompleteTextView(Context c) {
            super(c);
        }

        protected void performFiltering(CharSequence text, int keyCode) {
            super.performFiltering(text, keyCode);
        }

        protected void performFiltering(CharSequence text, int start, int end, int keyCode) {
            super.performFiltering(text, start, end, keyCode);
        }

        protected void replaceText(CharSequence text) {
            super.replaceText(text);
        }

        protected Filter getFilter() {
            return super.getFilter();
        }
    }
}
