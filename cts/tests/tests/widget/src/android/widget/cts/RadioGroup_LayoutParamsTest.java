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

import com.android.internal.R;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.TypedArray;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RadioGroup;
import android.widget.RadioGroup.LayoutParams;

import java.io.IOException;

/**
 * Test {@link LayoutParams}.
 */
public class RadioGroup_LayoutParamsTest extends AndroidTestCase {
    private LayoutParams mLayoutParams;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mLayoutParams = null;
    }

    public void testConstructor() {
        mLayoutParams = new RadioGroup.LayoutParams(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, mLayoutParams.width);
        assertEquals(Integer.MAX_VALUE, mLayoutParams.height);
        assertEquals(0.0f, mLayoutParams.weight);

        mLayoutParams = new RadioGroup.LayoutParams(Integer.MAX_VALUE, Integer.MIN_VALUE);
        assertEquals(Integer.MAX_VALUE, mLayoutParams.width);
        assertEquals(Integer.MIN_VALUE, mLayoutParams.height);
        assertEquals(0.0f, mLayoutParams.weight);

        mLayoutParams = new RadioGroup.LayoutParams(Integer.MIN_VALUE, Integer.MAX_VALUE,
                Float.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, mLayoutParams.width);
        assertEquals(Integer.MAX_VALUE, mLayoutParams.height);
        assertEquals(Float.MAX_VALUE, mLayoutParams.weight);

        mLayoutParams = new RadioGroup.LayoutParams(Integer.MIN_VALUE, Integer.MAX_VALUE,
                Float.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, mLayoutParams.width);
        assertEquals(Integer.MAX_VALUE, mLayoutParams.height);
        assertEquals(Float.MIN_VALUE, mLayoutParams.weight);

        mLayoutParams = new RadioGroup.LayoutParams(new ViewGroup.LayoutParams(40, 60));
        assertEquals(40, mLayoutParams.width);
        assertEquals(60, mLayoutParams.height);
        assertEquals(0.0f, mLayoutParams.weight);

        try {
            new RadioGroup.LayoutParams((ViewGroup.LayoutParams) null);
            fail("The constructor should throw NullPointerException when param "
                    + "ViewGroup.LayoutParams is null.");
        } catch (NullPointerException e) {
        }

        mLayoutParams = new RadioGroup.LayoutParams(new MarginLayoutParams(100, 200));
        assertEquals(100, mLayoutParams.width);
        assertEquals(200, mLayoutParams.height);
        assertEquals(0.0f, mLayoutParams.weight);
        assertEquals(0, mLayoutParams.leftMargin);
        assertEquals(0, mLayoutParams.topMargin);
        assertEquals(0, mLayoutParams.rightMargin);
        assertEquals(0, mLayoutParams.bottomMargin);

        MarginLayoutParams source = new MarginLayoutParams(10, 20);
        source.leftMargin = 1;
        source.topMargin = 2;
        source.rightMargin = 3;
        source.bottomMargin = 4;

        mLayoutParams = new RadioGroup.LayoutParams(source);
        assertEquals(10, mLayoutParams.width);
        assertEquals(20, mLayoutParams.height);
        assertEquals(0.0f, mLayoutParams.weight);
        assertEquals(1, mLayoutParams.leftMargin);
        assertEquals(2, mLayoutParams.topMargin);
        assertEquals(3, mLayoutParams.rightMargin);
        assertEquals(4, mLayoutParams.bottomMargin);

        try {
            new RadioGroup.LayoutParams((MarginLayoutParams) null);
            fail("The constructor should throw NullPointerException when param "
                    + "MarginLayoutParams is null.");
        } catch (NullPointerException e) {
        }

        mLayoutParams = new LayoutParams(getContext(), 
                getAttributeSet(com.android.cts.stub.R.layout.radiogroup_1));
        assertNotNull(mLayoutParams);
        assertEquals(0.5, mLayoutParams.weight, 0);
        assertEquals(Gravity.BOTTOM, mLayoutParams.gravity);
        assertEquals(5, mLayoutParams.leftMargin);
        assertEquals(5, mLayoutParams.topMargin);
        assertEquals(5, mLayoutParams.rightMargin);
        assertEquals(5, mLayoutParams.bottomMargin);
        assertEquals(LayoutParams.MATCH_PARENT, mLayoutParams.width);
        assertEquals(LayoutParams.MATCH_PARENT, mLayoutParams.height);

        mLayoutParams = new RadioGroup.LayoutParams(getContext(), null);
        assertEquals(RadioGroup.LayoutParams.WRAP_CONTENT, mLayoutParams.width);
        assertEquals(RadioGroup.LayoutParams.WRAP_CONTENT, mLayoutParams.height);

        try {
            new RadioGroup.LayoutParams(null, 
                    getAttributeSet(com.android.cts.stub.R.layout.radiogroup_1));
            fail("The constructor should throw NullPointerException when param Context is null.");
        } catch (NullPointerException e) {
        }
    }

    public void testSetBaseAttributes() {
        MockLayoutParams layoutParams = new MockLayoutParams(getContext(), null);
        // default values
        assertEquals(LayoutParams.WRAP_CONTENT, layoutParams.width);
        assertEquals(LayoutParams.WRAP_CONTENT, layoutParams.height);

        AttributeSet attrs = getAttributeSet(com.android.cts.stub.R.layout.radiogroup_1);
        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.ViewGroup_MarginLayout);
        layoutParams.setBaseAttributes(a,
                R.styleable.ViewGroup_MarginLayout_layout_width,
                R.styleable.ViewGroup_MarginLayout_layout_height);
        // check the attributes from the layout file
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.height);
    }

    private AttributeSet getAttributeSet(int resId) {
        XmlPullParser parser = mContext.getResources().getLayout(resId);
        assertNotNull(parser);
        int type = 0;
        try {
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                // Empty
            }
        } catch (XmlPullParserException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        }

        assertEquals("No RadioGroup element found", XmlPullParser.START_TAG, type);
        assertEquals("The first element is not RadioGroup", "RadioGroup", parser.getName());
        return Xml.asAttributeSet(parser);
    }

    private class MockLayoutParams extends RadioGroup.LayoutParams {
        public MockLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            super.setBaseAttributes(a, widthAttr, heightAttr);
        }
    }
}
