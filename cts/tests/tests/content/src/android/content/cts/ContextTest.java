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

package android.content.cts;

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.content.res.Resources.NotFoundException;
import android.content.res.Resources.Theme;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;

import java.io.IOException;

public class ContextTest extends AndroidTestCase {
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mContext.setTheme(R.style.Test_Theme);
    }

    public void testGetString() {
        String testString = mContext.getString(R.string.context_test_string1);
        assertEquals("This is %s string.", testString);

        testString = mContext.getString(R.string.context_test_string1, "expected");
        assertEquals("This is expected string.", testString);

        testString = mContext.getString(R.string.context_test_string2);
        assertEquals("This is test string.", testString);

        // Test wrong resource id
        try {
            testString = mContext.getString(0, "expected");
            fail("Wrong resource id should not be accepted.");
        } catch (NotFoundException e) {
        }

        // Test wrong resource id
        try {
            testString = mContext.getString(0);
            fail("Wrong resource id should not be accepted.");
        } catch (NotFoundException e) {
        }
    }

    public void testGetText() {
        CharSequence testCharSequence = mContext.getText(R.string.context_test_string2);
        assertEquals("This is test string.", testCharSequence.toString());

        // Test wrong resource id
        try {
            testCharSequence = mContext.getText(0);
            fail("Wrong resource id should not be accepted.");
        } catch (NotFoundException e) {
        }
    }

    public void testAccessTheme() {
        mContext.setTheme(R.style.Test_Theme);
        final Theme testTheme = mContext.getTheme();
        assertNotNull(testTheme);

        int[] attrs = {
            android.R.attr.windowNoTitle,
            android.R.attr.panelColorForeground,
            android.R.attr.panelColorBackground
        };
        TypedArray attrArray = null;
        try {
            attrArray = testTheme.obtainStyledAttributes(attrs);
            assertTrue(attrArray.getBoolean(0, false));
            assertEquals(0xff000000, attrArray.getColor(1, 0));
            assertEquals(0xffffffff, attrArray.getColor(2, 0));
        } finally {
            if (attrArray != null) {
                attrArray.recycle();
                attrArray = null;
            }
        }

        // setTheme only works for the first time
        mContext.setTheme(android.R.style.Theme_Black);
        assertSame(testTheme, mContext.getTheme());
    }

    public void testObtainStyledAttributes() {
        // Test obtainStyledAttributes(int[])
        TypedArray testTypedArray = mContext
                .obtainStyledAttributes(android.R.styleable.View);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 2);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();

        // Test obtainStyledAttributes(int, int[])
        testTypedArray = mContext.obtainStyledAttributes(android.R.style.TextAppearance_Small,
                android.R.styleable.TextAppearance);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 2);
        testTypedArray.recycle();

        // Test wrong null array pointer
        try {
            testTypedArray = mContext.obtainStyledAttributes(-1, null);
            fail("obtainStyledAttributes will throw a NullPointerException here.");
        } catch (NullPointerException e) {
        }

        // Test obtainStyledAttributes(AttributeSet, int[]) with unavailable resource id.
        int testInt[] = { 0, 0 };
        testTypedArray = mContext.obtainStyledAttributes(-1, testInt);
        // fail("Wrong resource id should not be accepted.");
        assertNotNull(testTypedArray);
        assertEquals(2, testTypedArray.length());
        testTypedArray.recycle();

        // Test obtainStyledAttributes(AttributeSet, int[])
        int[] attrs = android.R.styleable.DatePicker;
        testTypedArray = mContext.obtainStyledAttributes(getAttributeSet(R.layout.context_layout),
                attrs);
        assertNotNull(testTypedArray);
        assertEquals(attrs.length, testTypedArray.length());
        testTypedArray.recycle();

        // Test obtainStyledAttributes(AttributeSet, int[], int, int)
        testTypedArray = mContext.obtainStyledAttributes(getAttributeSet(R.layout.context_layout),
                attrs, 0, 0);
        assertNotNull(testTypedArray);
        assertEquals(attrs.length, testTypedArray.length());
        testTypedArray.recycle();
    }

    private AttributeSet getAttributeSet(int resourceId) {
        final XmlResourceParser parser = getContext().getResources().getXml(
                resourceId);

        try {
            XmlUtils.beginDocument(parser, "RelativeLayout");
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final AttributeSet attr = Xml.asAttributeSet(parser);
        assertNotNull(attr);
        return attr;
    }
}
