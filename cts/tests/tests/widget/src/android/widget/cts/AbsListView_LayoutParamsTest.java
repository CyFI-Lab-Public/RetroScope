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

import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;

public class AbsListView_LayoutParamsTest extends AndroidTestCase {
    private AttributeSet mAttributeSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        XmlPullParser parser = mContext.getResources().getXml(R.layout.abslistview_layout);
        WidgetTestUtils.beginDocument(parser, "ViewGroup_Layout");
        mAttributeSet = Xml.asAttributeSet(parser);
    }

    public void testConstructors() {
        int TEST_WIDTH = 25;
        int TEST_HEIGHT = 25;
        int TEST_HEIGHT2 = 30;
        AbsListView.LayoutParams layoutParams;

        layoutParams = new AbsListView.LayoutParams(getContext(), mAttributeSet);
        assertEquals(TEST_WIDTH, layoutParams.width);
        assertEquals(TEST_HEIGHT, layoutParams.height);

        layoutParams = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.height);

        layoutParams = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT, 0);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.width);
        assertEquals(LayoutParams.MATCH_PARENT, layoutParams.height);

        AbsListView.LayoutParams tmpParams = new AbsListView.LayoutParams(TEST_WIDTH, TEST_HEIGHT2);
        layoutParams = new AbsListView.LayoutParams(tmpParams);
        assertEquals(TEST_WIDTH, layoutParams.width);
        assertEquals(TEST_HEIGHT2, layoutParams.height);
    }
}
