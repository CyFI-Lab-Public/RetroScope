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
import org.xmlpull.v1.XmlPullParserException;

import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import java.io.IOException;

public class FrameLayout_LayoutParamsTest extends AndroidTestCase {

    private AttributeSet getAttributeSet() throws XmlPullParserException, IOException {
        XmlPullParser parser = mContext.getResources().getLayout(R.layout.framelayout_layout);
        WidgetTestUtils.beginDocument(parser, "LinearLayout");
        return Xml.asAttributeSet(parser);
    }

    public void testConstructor() throws XmlPullParserException, IOException {
        AttributeSet attrs = getAttributeSet();

        new LayoutParams(mContext, attrs);
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0);
        new LayoutParams(new ViewGroup.LayoutParams(mContext, attrs));
        new LayoutParams(new LayoutParams(mContext, attrs));
        new LayoutParams(new MarginLayoutParams(mContext, attrs));

        try {
            new LayoutParams(null, null);
            fail("did not throw NullPointerException when context and attrs are null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        new LayoutParams(-1, -1);
        new LayoutParams(-1, -1, -1);

        try {
            new LayoutParams((ViewGroup.LayoutParams) null);
            fail("did not throw NullPointerException when ViewGroup.LayoutParams is null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new LayoutParams((ViewGroup.MarginLayoutParams) null);
            fail("did not throw NullPointerException when ViewGroup.MarginLayoutParams is null.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
