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

package android.view.cts;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.test.InstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import android.widget.LinearLayout;
import com.android.internal.util.XmlUtils;
import com.android.cts.stub.R;


public class ViewGroup_MarginLayoutParamsTest extends InstrumentationTestCase {

    private ViewGroup.MarginLayoutParams mMarginLayoutParams;
    private Context mContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mMarginLayoutParams = null;
        mContext = getInstrumentation().getTargetContext();
    }

    public void testConstructor() {
        mMarginLayoutParams = null;
        // create a new MarginLayoutParams instance
        XmlResourceParser p = mContext.getResources().getLayout(
                R.layout.viewgroup_margin_layout);
        try {
            XmlUtils.beginDocument(p, "LinearLayout");
        } catch (Exception e) {
            fail("Fail in preparing AttibuteSet.");
        }
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(mContext, p);
        assertNotNull(mMarginLayoutParams);

        mMarginLayoutParams = null;
        // create a new MarginLayoutParams instance
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(320, 480);
        assertNotNull(mMarginLayoutParams);

        mMarginLayoutParams = null;
        // create a new MarginLayoutParams instance
        MarginLayoutParams temp = new ViewGroup.MarginLayoutParams(320, 480);
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(temp);
        assertNotNull(mMarginLayoutParams);

        mMarginLayoutParams = null;
        // create a new MarginLayoutParams instance
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(320, 480);
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(lp);
        assertNotNull(mMarginLayoutParams);

    }

    public void testSetMargins() {
        // create a new MarginLayoutParams instance
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(320, 480);
        mMarginLayoutParams.setMargins(20, 30, 120, 140);
        assertEquals(20, mMarginLayoutParams.leftMargin);
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.rightMargin);
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(20, mMarginLayoutParams.getMarginStart());
        assertEquals(120, mMarginLayoutParams.getMarginEnd());

        assertEquals(false, mMarginLayoutParams.isMarginRelative());
    }

    public void testSetMarginsRelative() {
        // create a new MarginLayoutParams instance
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(320, 480);
        mMarginLayoutParams.setMarginsRelative(20, 30, 120, 140);
        assertEquals(20, mMarginLayoutParams.getMarginStart());
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.getMarginEnd());
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(0, mMarginLayoutParams.leftMargin);
        assertEquals(0, mMarginLayoutParams.rightMargin);

        assertEquals(true, mMarginLayoutParams.isMarginRelative());
    }

    public void testResolveMarginsRelative() {
        ViewGroup vg = new LinearLayout(mContext);

        // LTR / normal margin case
        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(320, 480);
        mMarginLayoutParams.setMargins(20, 30, 120, 140);
        vg.setLayoutParams(mMarginLayoutParams);

        assertEquals(20, mMarginLayoutParams.leftMargin);
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.rightMargin);
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(20, mMarginLayoutParams.getMarginStart());
        assertEquals(120, mMarginLayoutParams.getMarginEnd());

        assertEquals(false, mMarginLayoutParams.isMarginRelative());

        // LTR / relative margin case
        mMarginLayoutParams.setMarginsRelative(20, 30, 120, 140);
        vg.setLayoutParams(mMarginLayoutParams);

        assertEquals(20, mMarginLayoutParams.getMarginStart());
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.getMarginEnd());
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(20, mMarginLayoutParams.leftMargin);
        assertEquals(120, mMarginLayoutParams.rightMargin);

        assertEquals(true, mMarginLayoutParams.isMarginRelative());

        // RTL / normal margin case
        vg.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        mMarginLayoutParams = new ViewGroup.MarginLayoutParams(320, 480);
        mMarginLayoutParams.setMargins(20, 30, 120, 140);
        vg.setLayoutParams(mMarginLayoutParams);

        assertEquals(20, mMarginLayoutParams.leftMargin);
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.rightMargin);
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(120, mMarginLayoutParams.getMarginStart());
        assertEquals(20, mMarginLayoutParams.getMarginEnd());

        assertEquals(false, mMarginLayoutParams.isMarginRelative());

        // RTL / relative margin case
        mMarginLayoutParams.setMarginsRelative(20, 30, 120, 140);
        vg.setLayoutParams(mMarginLayoutParams);

        assertEquals(20, mMarginLayoutParams.getMarginStart());
        assertEquals(30, mMarginLayoutParams.topMargin);
        assertEquals(120, mMarginLayoutParams.getMarginEnd());
        assertEquals(140, mMarginLayoutParams.bottomMargin);

        assertEquals(120, mMarginLayoutParams.leftMargin);
        assertEquals(20, mMarginLayoutParams.rightMargin);

        assertEquals(true, mMarginLayoutParams.isMarginRelative());
    }
}
