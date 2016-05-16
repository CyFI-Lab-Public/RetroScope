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

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.test.InstrumentationTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.TableLayout;

import com.android.internal.R;
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParser;

/**
 * Test {@link TableLayout.LayoutParams}.
 */
public class TableLayout_LayoutParamsTest extends InstrumentationTestCase {
    private Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTargetContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        new TableLayout.LayoutParams(mTargetContext, null);

        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(200, 300);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(300, layoutParams.height);
        TableLayout.LayoutParams oldParams = layoutParams;

        layoutParams = new TableLayout.LayoutParams(200, 300, 1.2f);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(300, layoutParams.height);
        assertEquals(1.2f, layoutParams.weight);
        TableLayout.LayoutParams oldMarginParams = layoutParams;

        new TableLayout.LayoutParams();

        layoutParams = new TableLayout.LayoutParams(oldParams);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(300, layoutParams.height);

        layoutParams = new TableLayout.LayoutParams(oldMarginParams);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(300, layoutParams.height);
    }

    public void testSetBaseAttributes() {
        MockTableLayout_LayoutParams mockLayoutParams = new MockTableLayout_LayoutParams(200, 300);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(300, mockLayoutParams.height);

        // base_attr_pixel: layout_width = 400px, layout_height = 600px
        AttributeSet attrs = getAttrs("base_attr_pixel");
        TypedArray a = mTargetContext.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_width,
                R.styleable.ViewGroup_Layout_layout_height);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(600, mockLayoutParams.height);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_height,
                R.styleable.ViewGroup_Layout_layout_width);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(400, mockLayoutParams.height);

        a.recycle();
        // base_attr_fillwrap: layout_width = "match_parent", layout_height = "wrap_content"
        attrs = getAttrs("base_attr_fillwrap");
        a = mTargetContext.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_width,
                R.styleable.ViewGroup_Layout_layout_height);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(TableLayout.LayoutParams.WRAP_CONTENT, mockLayoutParams.height);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_height,
                R.styleable.ViewGroup_Layout_layout_width);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.height);

        a.recycle();
        // base_attr_noheight: layout_width = 600px, no layout_height.
        attrs = getAttrs("base_attr_noheight");
        a = mTargetContext.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_width,
                R.styleable.ViewGroup_Layout_layout_height);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(TableLayout.LayoutParams.WRAP_CONTENT, mockLayoutParams.height);

        mockLayoutParams.setBaseAttributes(a, R.styleable.ViewGroup_Layout_layout_height,
                R.styleable.ViewGroup_Layout_layout_width);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(600, mockLayoutParams.height);

        try {
            mockLayoutParams.setBaseAttributes(null, R.styleable.ViewGroup_Layout_layout_width,
                    R.styleable.ViewGroup_Layout_layout_height);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }

        mockLayoutParams.setBaseAttributes(a, -1, R.styleable.ViewGroup_Layout_layout_height);
        assertEquals(TableLayout.LayoutParams.MATCH_PARENT, mockLayoutParams.width);
        assertEquals(TableLayout.LayoutParams.WRAP_CONTENT, mockLayoutParams.height);

        try {
            mockLayoutParams.setBaseAttributes(null, -1, -1);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    private AttributeSet getAttrs(String searchedNodeName) {
        XmlResourceParser parser = null;
        AttributeSet attrs = null;
        try {
            parser = mTargetContext.getResources()
                    .getXml(com.android.cts.stub.R.xml.base_attributes);

            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"alias".equals(nodeName)) {
                throw new RuntimeException();
            }
            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }
                nodeName = parser.getName();
                if (searchedNodeName.equals(nodeName)) {
                    outerDepth = parser.getDepth();
                    while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                            && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                        if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                            continue;
                        }
                        nodeName = parser.getName();
                        if ("Attributes".equals(nodeName)) {
                            attrs = Xml.asAttributeSet(parser);
                            break;
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    break;
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (Exception e) {
        }
        return attrs;
    }

    /*
     * Mock class for TableLayout.LayoutParams to test protected methods
     */
    private class MockTableLayout_LayoutParams extends TableLayout.LayoutParams {
        public MockTableLayout_LayoutParams(int w, int h) {
            super(w, h);
        }

        @Override
        protected void setBaseAttributes(TypedArray a, int widthAttr,
                int heightAttr) {
            super.setBaseAttributes(a, widthAttr, heightAttr);
        }
    }
}
