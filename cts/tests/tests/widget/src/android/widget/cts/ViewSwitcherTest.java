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

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ViewSwitcher;
import android.widget.ViewSwitcher.ViewFactory;

import com.android.cts.stub.R;


/**
 * Test {@link ViewSwitcher}.
 */
public class ViewSwitcherTest extends AndroidTestCase {
    private ViewSwitcher mViewSwitcher;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mViewSwitcher = null;
    }

    public void testConstructor() {
        new ViewSwitcher(getContext());

        new ViewSwitcher(getContext(), null);

        XmlPullParser parser = getContext().getResources().getXml(R.layout.viewswitcher_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        new ViewSwitcher(getContext(), attrs);
    }

    public void testSetFactory() {
        mViewSwitcher = new ViewSwitcher(getContext());

        MockViewFactory factory = new MockViewFactory();
        mViewSwitcher.setFactory(factory);
        assertTrue(factory.hasMakeViewCalled());
    }

    public void testReset() {
        mViewSwitcher = new ViewSwitcher(getContext());

        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        assertEquals(View.VISIBLE, lv1.getVisibility());
        assertEquals(View.VISIBLE, lv2.getVisibility());
        mViewSwitcher.addView(lv1, 0);
        mViewSwitcher.addView(lv2, 1);

        mViewSwitcher.reset();
        assertEquals(View.GONE, lv1.getVisibility());
        assertEquals(View.GONE, lv2.getVisibility());
    }

    public void testGetNextView() {
        mViewSwitcher = new ViewSwitcher(getContext());

        ListView lv1 = new ListView(getContext());
        ListView lv2 = new ListView(getContext());
        mViewSwitcher.addView(lv1, 0, new ViewGroup.LayoutParams(20, 25));
        assertSame(lv1, mViewSwitcher.getChildAt(0));
        assertNull(mViewSwitcher.getNextView());

        mViewSwitcher.addView(lv2, 1, new ViewGroup.LayoutParams(20, 25));
        assertSame(lv2, mViewSwitcher.getChildAt(1));
        assertSame(lv2, mViewSwitcher.getNextView());

        mViewSwitcher.setDisplayedChild(1);
        assertSame(lv1, mViewSwitcher.getNextView());

        try {
            ListView lv3 = new ListView(getContext());
            mViewSwitcher.addView(lv3, 2, null);
            fail("Should throw IllegalStateException here.");
        } catch (IllegalStateException e) {
        }
    }

    private class MockViewFactory implements ViewFactory {
        private boolean mMakeViewCalled = false;

        public View makeView() {
            mMakeViewCalled = true;
            return new ListView(getContext());
        }

        public boolean hasMakeViewCalled() {
            return mMakeViewCalled;
        }
    }
}
