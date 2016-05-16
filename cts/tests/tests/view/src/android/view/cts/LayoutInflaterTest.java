/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.cts.stub.R;
import com.android.internal.util.XmlUtils;


import org.xmlpull.v1.XmlPullParser;

import android.app.cts.MockActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater.Factory;
import android.view.LayoutInflater.Filter;
import android.widget.LinearLayout;

public class LayoutInflaterTest extends AndroidTestCase {

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private final Factory mFactory = new Factory() {
        public View onCreateView(String name, Context context,
                AttributeSet attrs) {

            return null;
        }
    };
    private boolean isOnLoadClass;
    private final Filter mFilter = new Filter() {

        @SuppressWarnings("unchecked")
        public boolean onLoadClass(Class clazz) {
            isOnLoadClass = true;
            return true;
        }

    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void testFrom() {
        mLayoutInflater = null;
        mLayoutInflater = LayoutInflater.from(mContext);
        assertNotNull(mLayoutInflater);
        mLayoutInflater = null;
        mLayoutInflater = new MockLayoutInflater(mContext);
        assertNotNull(mLayoutInflater);

        LayoutInflater layoutInflater = new MockLayoutInflater(mLayoutInflater,
                mContext);
        assertNotNull(layoutInflater);
    }

    public void testAccessLayoutInflaterProperties() {
        mLayoutInflater.setFilter(mFilter);
        assertSame(mFilter, mLayoutInflater.getFilter());
        mLayoutInflater.setFactory(mFactory);
        assertSame(mFactory, mLayoutInflater.getFactory());
        mLayoutInflater=new MockLayoutInflater(mContext);
        assertSame(mContext, mLayoutInflater.getContext());
    }

    private AttributeSet getAttrs() {
        XmlResourceParser parser = null;
        AttributeSet attrs = null;
        ActivityInfo ai = null;
        ComponentName mComponentName = new ComponentName(mContext,
                MockActivity.class);
        try {
            ai = mContext.getPackageManager().getActivityInfo(mComponentName,
                    PackageManager.GET_META_DATA);
            parser = ai.loadXmlMetaData(mContext.getPackageManager(),
                    "android.widget.layout");
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"alias".equals(nodeName)) {
                throw new InflateException();
            }
            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }
                nodeName = parser.getName();
                if ("AbsoluteLayout".equals(nodeName)) {
                    attrs = Xml.asAttributeSet(parser);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        } catch (Exception e) {
        }
        return attrs;
    }

    public void testCreateView() {

        AttributeSet attrs = getAttrs();
        isOnLoadClass = false;
        View view = null;
        try {
            view = mLayoutInflater
                    .createView("testthrow", "com.android", attrs);
            fail("should throw exception");
        } catch (InflateException e) {
        } catch (ClassNotFoundException e) {
        }
        assertFalse(isOnLoadClass);
        assertNull(view);
        mLayoutInflater = null;
        mLayoutInflater = LayoutInflater.from(mContext);
        isOnLoadClass = false;
        mLayoutInflater.setFilter(new Filter() {
            @SuppressWarnings("unchecked")
            public boolean onLoadClass(Class clazz) {
                isOnLoadClass = true;
                return false;
            }
        });
        try {
            view = mLayoutInflater.createView("MockActivity",
                    "com.android.app.", attrs);
            fail("should throw exception");
        } catch (InflateException e) {
        } catch (ClassNotFoundException e) {
        }
        assertFalse(isOnLoadClass);
        assertNull(view);

        isOnLoadClass = false;
        // allowedState is false
        try {
            view = mLayoutInflater.createView(MockActivity.class.getName(),
                    MockActivity.class.getPackage().toString(), attrs);
            fail("should throw exception");
        } catch (InflateException e) {
        } catch (ClassNotFoundException e) {
        }
        assertFalse(isOnLoadClass);
        assertNull(view);
        mLayoutInflater = null;
        mLayoutInflater = LayoutInflater.from(mContext);
        try {
            mLayoutInflater.setFilter(null);
            view = mLayoutInflater.createView("com.android.app.MockActivity",
                    null, attrs);
            assertNotNull(view);
            assertFalse(isOnLoadClass);
            mLayoutInflater = null;
            mLayoutInflater = LayoutInflater.from(mContext);
            mLayoutInflater.setFilter(null);

            view = mLayoutInflater.createView(MockActivity.class.getName(),
                    MockActivity.class.getPackage().toString(), attrs);
            assertNotNull(view);
            assertFalse(isOnLoadClass);
            mLayoutInflater.setFilter(mFilter);
            view = mLayoutInflater.createView(MockActivity.class.getName(),
                    MockActivity.class.getPackage().toString(), attrs);
            assertNotNull(view);
            assertTrue(isOnLoadClass);
            // allowedState!=null
            view = mLayoutInflater.createView(MockActivity.class.getName(),
                    MockActivity.class.getPackage().toString(), attrs);
            assertNotNull(view);
            assertTrue(isOnLoadClass);
        } catch (InflateException e) {
        } catch (ClassNotFoundException e) {
        }
    }

    public void testInflate() {
        View view = mLayoutInflater.inflate(
                com.android.cts.stub.R.layout.inflater_layout, null);
        assertNotNull(view);
        view = null;
        try {
            view = mLayoutInflater.inflate(-1, null);
            fail("should throw exception");
        } catch (Resources.NotFoundException e) {
        }
        LinearLayout mLayout;
        mLayout = new LinearLayout(mContext);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setHorizontalGravity(Gravity.LEFT);
        mLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        assertEquals(0, mLayout.getChildCount());
        view = mLayoutInflater.inflate(R.layout.inflater_layout,
                mLayout);
        assertNotNull(view);
        assertEquals(1, mLayout.getChildCount());
    }

    public void testInflate2() {
        View view = mLayoutInflater.inflate(
                R.layout.inflater_layout, null, false);
        assertNotNull(view);
        view = null;
        try {
            view = mLayoutInflater.inflate(-1, null, false);
            fail("should throw exception");
        } catch (Resources.NotFoundException e) {

        }
        LinearLayout mLayout;
        mLayout = new LinearLayout(mContext);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setHorizontalGravity(Gravity.LEFT);
        mLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        assertEquals(0, mLayout.getChildCount());
        view = mLayoutInflater.inflate(R.layout.inflater_layout,
                mLayout, false);
        assertNotNull(view);
        assertEquals(0, mLayout.getChildCount());

        view = null;
        view = mLayoutInflater.inflate(R.layout.inflater_layout,
                mLayout, true);
        assertNotNull(view);
        assertEquals(1, mLayout.getChildCount());
    }

    public void testInflate3() {
        XmlResourceParser parser = getContext().getResources().getLayout(
                R.layout.inflater_layout);
        View view = mLayoutInflater.inflate(parser, null);
        assertNotNull(view);
        view = null;
        try {
            view = mLayoutInflater.inflate(null, null);
            fail("should throw exception");
        } catch (NullPointerException e) {
        }
        LinearLayout mLayout;
        mLayout = new LinearLayout(mContext);
        mLayout.setOrientation(LinearLayout.VERTICAL);
        mLayout.setHorizontalGravity(Gravity.LEFT);
        mLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        assertEquals(0, mLayout.getChildCount());

        try {
            view = mLayoutInflater.inflate(parser, mLayout);
            fail("should throw exception");
        } catch (NullPointerException e) {
        }
        parser = getContext().getResources().getLayout(
                R.layout.inflater_layout);
        view = mLayoutInflater.inflate(parser, mLayout);
        assertNotNull(view);
        assertEquals(1, mLayout.getChildCount());
        parser = getContext().getResources().getLayout(
                R.layout.inflater_layout);
        view = mLayoutInflater.inflate(parser, mLayout);
        assertNotNull(view);
        assertEquals(2, mLayout.getChildCount());

        parser = null;
        view = null;
        parser = getParser();

        view = mLayoutInflater.inflate(parser, mLayout);
        assertNotNull(view);
        assertEquals(3, mLayout.getChildCount());
    }

    public void testInflate4() {
       XmlResourceParser parser = getContext().getResources().getLayout(
               R.layout.inflater_layout);
       View view = mLayoutInflater.inflate(parser, null, false);
       assertNotNull(view);
       view = null;
       try {
           view = mLayoutInflater.inflate(null, null, false);
           fail("should throw exception");
       } catch (NullPointerException e) {
       }
       LinearLayout mLayout;
       mLayout = new LinearLayout(mContext);
       mLayout.setOrientation(LinearLayout.VERTICAL);
       mLayout.setHorizontalGravity(Gravity.LEFT);
       mLayout.setLayoutParams(new ViewGroup.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.MATCH_PARENT));
       assertEquals(0, mLayout.getChildCount());

       try {
           view = mLayoutInflater.inflate(parser, mLayout, false);
           fail("should throw exception");
       } catch (NullPointerException e) {
       }
       parser = getContext().getResources().getLayout(
               R.layout.inflater_layout);
       view = mLayoutInflater.inflate(parser, mLayout, false);
       assertNull(view.getParent());
       assertNotNull(view);
       assertEquals(0, mLayout.getChildCount());
       parser = getContext().getResources().getLayout(
               R.layout.inflater_layout);
       assertEquals(0, mLayout.getChildCount());
       view = mLayoutInflater.inflate(parser, mLayout, true);
       assertNotNull(view);
       assertNull(view.getParent());
       assertEquals(1, mLayout.getChildCount());

       parser = null;
       parser = getParser();
       try {
           view = mLayoutInflater.inflate(parser, mLayout, false);
           fail("should throw exception");
       } catch (InflateException e) {
       }

       parser = null;
       view = null;
       parser = getParser();

       view = mLayoutInflater.inflate(parser, mLayout, true);
       assertNotNull(view);
       assertEquals(2, mLayout.getChildCount());
   }

    static class MockLayoutInflater extends LayoutInflater {

        public MockLayoutInflater(Context c) {
            super(c);
        }

        public MockLayoutInflater(LayoutInflater original, Context newContext) {
            super(original, newContext);
        }

        @Override
        public View onCreateView(String name, AttributeSet attrs)
                throws ClassNotFoundException {
            return super.onCreateView(name, attrs);
        }

        @Override
        public LayoutInflater cloneInContext(Context newContext) {
            return null;
        }
    }

    private XmlResourceParser getParser() {
        XmlResourceParser parser = null;
        ActivityInfo ai = null;
        ComponentName mComponentName = new ComponentName(mContext,
                MockActivity.class);
        try {
            ai = mContext.getPackageManager().getActivityInfo(mComponentName,
                    PackageManager.GET_META_DATA);
            parser = ai.loadXmlMetaData(mContext.getPackageManager(),
                    "android.view.merge");
        } catch (Exception e) {
        }
        return parser;
    }
}
