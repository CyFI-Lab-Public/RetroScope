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

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class AbsoluteLayoutTest extends ActivityInstrumentationTestCase2<StubActivity> {
    private static final int DEFAULT_X      = 5;
    private static final int DEFAULT_Y      = 10;
    private static final int DEFAULT_WIDTH  = 20;
    private static final int DEFAULT_HEIGHT = 30;

    private Activity mActivity;
    private MyAbsoluteLayout mMyAbsoluteLayout;
    private LayoutParams mAbsoluteLayoutParams;

    public AbsoluteLayoutTest() {
        super("com.android.cts.stub", StubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mMyAbsoluteLayout = new MyAbsoluteLayout(mActivity);
        mAbsoluteLayoutParams = new LayoutParams(DEFAULT_WIDTH, DEFAULT_HEIGHT,
                DEFAULT_X, DEFAULT_Y);
    }

    private AttributeSet getAttributeSet() throws XmlPullParserException, IOException {
        XmlPullParser parser = mActivity.getResources().getLayout(R.layout.absolute_layout);
        WidgetTestUtils.beginDocument(parser, "LinearLayout");
        return Xml.asAttributeSet(parser);
    }

    public void testConstructor() throws XmlPullParserException, IOException {
        AttributeSet attrs = getAttributeSet();

        new AbsoluteLayout(mActivity);
        new AbsoluteLayout(mActivity, attrs);
        new AbsoluteLayout(mActivity, attrs, 0);
        new AbsoluteLayout(mActivity, null, 1);
        new AbsoluteLayout(mActivity, attrs, -1);
    }

    public void testOnMeasure() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testOnLayout() {
        // onMeasure() is implementation details, do NOT test
    }

    public void testCheckLayoutParams() {
        assertTrue(mMyAbsoluteLayout.checkLayoutParams(mAbsoluteLayoutParams));

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(1, 2);
        assertFalse(mMyAbsoluteLayout.checkLayoutParams(layoutParams));
        assertFalse(mMyAbsoluteLayout.checkLayoutParams(null));
    }

    public void testGenerateLayoutParams1() throws Throwable {
        runTestOnUiThread(new Runnable() {
            public void run() {
                mActivity.setContentView(R.layout.absolute_layout);
            }
        });
        getInstrumentation().waitForIdleSync();
        AbsoluteLayout layout = (AbsoluteLayout) mActivity.findViewById(R.id.absolute_view);
        LayoutParams params = (LayoutParams) layout.generateLayoutParams(getAttributeSet());

        assertNotNull(params);
        assertEquals(LayoutParams.MATCH_PARENT, params.width);
        assertEquals(LayoutParams.MATCH_PARENT, params.height);
        assertEquals(0, params.x);
        assertEquals(0, params.y);
    }

    public void testGenerateLayoutParams2() {
        LayoutParams params =
            (LayoutParams) mMyAbsoluteLayout.generateLayoutParams(mAbsoluteLayoutParams);

        assertEquals(DEFAULT_WIDTH, params.width);
        assertEquals(DEFAULT_HEIGHT, params.height);
        assertEquals(0, params.x);
        assertEquals(0, params.y);

        try {
            mMyAbsoluteLayout.generateLayoutParams((LayoutParams) null);
            fail("did not throw NullPointerException when ViewGroup.LayoutParams is null.");
        } catch (NullPointerException e) {
            // expected, test success
        }
    }

    public void testGenerateDefaultLayoutParams() {
        LayoutParams params = (LayoutParams) mMyAbsoluteLayout.generateDefaultLayoutParams();

        assertEquals(LayoutParams.WRAP_CONTENT, params.width);
        assertEquals(LayoutParams.WRAP_CONTENT, params.height);
        assertEquals(0, params.x);
        assertEquals(0, params.y);
    }

    private static class MyAbsoluteLayout extends AbsoluteLayout {
        public MyAbsoluteLayout(Context context) {
            super(context);
        }

        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return super.checkLayoutParams(p);
        }

        @Override
        protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
            return super.generateDefaultLayoutParams();
        }

        @Override
        protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return super.generateLayoutParams(p);
        }
    }
}
