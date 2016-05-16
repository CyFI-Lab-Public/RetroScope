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


import android.app.Activity;
import android.app.ActivityGroup;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

/**
 * Test {@link TabHost}.
 */
public class TabHostTest extends ActivityInstrumentationTestCase2<TabHostStubActivity> {
    private static final String TAG_TAB1 = "tab 1";
    private static final String TAG_TAB2 = "tab 2";
    private static final int TAB_HOST_ID = android.R.id.tabhost;

    private TabHostStubActivity mActivity;

    public TabHostTest() {
        super("com.android.cts.stub", TabHostStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testConstructor() {
        new TabHost(mActivity);

        new TabHost(mActivity, null);
    }

    public void testNewTabSpec() {
        TabHost tabHost = new TabHost(mActivity);

        assertNotNull(tabHost.newTabSpec(TAG_TAB2));

        assertNotNull(tabHost.newTabSpec(null));
    }

    /*
     * Check points:
     * 1. the tabWidget view and tabContent view associated with tabHost are created.
     * 2. no exception occurs when doing normal operation after setup().
     */
    public void testSetup1() throws Throwable {
        final Activity activity = launchActivity("com.android.cts.stub", StubActivity.class, null);

        runTestOnUiThread(new Runnable() {
            public void run() {
                activity.setContentView(R.layout.tabhost_layout);

                TabHost tabHost = (TabHost) activity.findViewById(TAB_HOST_ID);
                assertNull(tabHost.getTabWidget());
                assertNull(tabHost.getTabContentView());
                tabHost.setup();
                assertNotNull(tabHost.getTabWidget());
                assertNotNull(tabHost.getTabContentView());

                TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB1);
                tabSpec.setIndicator(TAG_TAB1);
                tabSpec.setContent(new MyTabContentFactoryList());
                tabHost.addTab(tabSpec);
                tabHost.setCurrentTab(0);
            }
        });
        getInstrumentation().waitForIdleSync();

        activity.finish();
    }

    /*
     * Check points:
     * 1. the tabWidget view and tabContent view associated with tabHost are created.
     * 2. no exception occurs when uses TabSpec.setContent(android.content.Intent) after setup().
     */
    public void testSetup2() throws Throwable {
        final ActivityGroup activity = launchActivity("com.android.cts.stub",
                ActivityGroup.class, null);


        runTestOnUiThread(new Runnable() {
            public void run() {
                activity.setContentView(R.layout.tabhost_layout);

                TabHost tabHost = (TabHost) activity.findViewById(TAB_HOST_ID);
                assertNull(tabHost.getTabWidget());
                assertNull(tabHost.getTabContentView());
                tabHost.setup(activity.getLocalActivityManager());
                assertNotNull(tabHost.getTabWidget());
                assertNotNull(tabHost.getTabContentView());

                TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB1);
                tabSpec.setIndicator(TAG_TAB1);
                Intent intent = new Intent(Intent.ACTION_VIEW, null,
                        mActivity, StubActivity.class);
                tabSpec.setContent(intent);
                tabHost.addTab(tabSpec);
                tabHost.setCurrentTab(0);
            }
        });
        getInstrumentation().waitForIdleSync();

        activity.finish();
    }

    public void testOnTouchModeChanged() {
        // implementation details
    }

    @UiThreadTest
    public void testAddTab() {
        TabHost tabHost = mActivity.getTabHost();
        // there is a initial tab
        assertEquals(1, tabHost.getTabWidget().getChildCount());

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryList());
        tabHost.addTab(tabSpec);
        assertEquals(2, tabHost.getTabWidget().getChildCount());
        tabHost.setCurrentTab(1);
        assertTrue(tabHost.getCurrentView() instanceof ListView);
        assertEquals(TAG_TAB2, tabHost.getCurrentTabTag());

        try {
            tabHost.addTab(tabHost.newTabSpec("tab 3"));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            tabHost.addTab(tabHost.newTabSpec("tab 3").setIndicator("tab 3"));
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        try {
            tabHost.addTab(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    @UiThreadTest
    public void testClearAllTabs() {
        TabHost tabHost = mActivity.getTabHost();
        MyTabContentFactoryText tcf = new MyTabContentFactoryText();
        // add two additional tabs
        tabHost.addTab(tabHost.newTabSpec(TAG_TAB1).setIndicator(TAG_TAB1).setContent(tcf));
        tabHost.addTab(tabHost.newTabSpec(TAG_TAB2).setIndicator(TAG_TAB2).setContent(tcf));
        assertEquals(3, tabHost.getTabWidget().getChildCount());
        assertEquals(3, tabHost.getTabContentView().getChildCount());
        assertEquals(0, tabHost.getCurrentTab());
        assertNotNull(tabHost.getCurrentView());

        /*
        TODO: Uncomment after fixing clearAllTabs() issue.
        The code below throws a NullPointerException in clearAllTabs(). The method throwing the
        exception is TabWidget.onFocusChange().

        tabHost.clearAllTabs();

        assertEquals(0, tabHost.getTabWidget().getChildCount());
        assertEquals(0, tabHost.getTabContentView().getChildCount());
        assertEquals(-1, tabHost.getCurrentTab());
        assertNull(tabHost.getCurrentView());
        */
    }

    public void testGetTabWidget() {
        TabHost tabHost = mActivity.getTabHost();

        // The attributes defined in tabhost_layout.xml
        assertEquals(android.R.id.tabs, tabHost.getTabWidget().getId());
        WidgetTestUtils.assertScaledPixels(1, tabHost.getTabWidget().getPaddingLeft(),
                getActivity());
        WidgetTestUtils.assertScaledPixels(1, tabHost.getTabWidget().getPaddingRight(),
                getActivity());
        WidgetTestUtils.assertScaledPixels(4, tabHost.getTabWidget().getPaddingTop(),
                getActivity());
    }

    @UiThreadTest
    public void testAccessCurrentTab() {
        TabHost tabHost = mActivity.getTabHost();
        assertEquals(0, tabHost.getCurrentTab());

        // normal value
        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryText());
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(1);
        assertEquals(1, tabHost.getCurrentTab());
        tabHost.setCurrentTab(0);
        assertEquals(0, tabHost.getCurrentTab());

        // exceptional value
        tabHost.setCurrentTab(tabHost.getTabWidget().getChildCount() + 1);
        assertEquals(0, tabHost.getCurrentTab());
        tabHost.setCurrentTab(-1);
        assertEquals(0, tabHost.getCurrentTab());
    }

    @UiThreadTest
    public void testGetCurrentTabView() {
        TabHost tabHost = mActivity.getTabHost();
        // current tab view is the first child of tabWidget.
        assertSame(tabHost.getTabWidget().getChildAt(0), tabHost.getCurrentTabView());

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryText());
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(1);
        // current tab view is the second child of tabWidget.
        assertSame(tabHost.getTabWidget().getChildAt(1), tabHost.getCurrentTabView());
    }

    @UiThreadTest
    public void testGetCurrentView() {
        TabHost tabHost = mActivity.getTabHost();
        TextView textView = (TextView) tabHost.getCurrentView();
        assertEquals(TabHostStubActivity.INITIAL_VIEW_TEXT, textView.getText().toString());

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryList());
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(1);
        assertTrue(tabHost.getCurrentView() instanceof ListView);
    }

    @UiThreadTest
    public void testSetCurrentTabByTag() {
        TabHost tabHost = mActivity.getTabHost();

        // set CurrentTab
        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryText());
        tabHost.addTab(tabSpec);

        tabHost.setCurrentTabByTag(TAG_TAB2);
        assertEquals(1, tabHost.getCurrentTab());

        tabHost.setCurrentTabByTag(TabHostStubActivity.INITIAL_TAB_TAG);
        assertEquals(0, tabHost.getCurrentTab());

        // exceptional value
        tabHost.setCurrentTabByTag(null);
        assertEquals(0, tabHost.getCurrentTab());

        tabHost.setCurrentTabByTag("unknown tag");
        assertEquals(0, tabHost.getCurrentTab());
    }

    @UiThreadTest
    public void testGetTabContentView() {
        TabHost tabHost = mActivity.getTabHost();
        assertEquals(3, tabHost.getTabContentView().getChildCount());

        TextView child0 = (TextView) tabHost.getTabContentView().getChildAt(0);
        assertEquals(mActivity.getResources().getString(R.string.hello_world),
                child0.getText().toString());
        assertTrue(tabHost.getTabContentView().getChildAt(1) instanceof ListView);
        TextView child2 = (TextView) tabHost.getTabContentView().getChildAt(2);
        tabHost.setCurrentTab(0);
        assertEquals(TabHostStubActivity.INITIAL_VIEW_TEXT, child2.getText().toString());

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryList());
        tabHost.addTab(tabSpec);
        assertEquals(3, tabHost.getTabContentView().getChildCount());
        tabHost.setCurrentTab(1);
        assertEquals(4, tabHost.getTabContentView().getChildCount());

        child0 = (TextView) tabHost.getTabContentView().getChildAt(0);
        assertEquals(mActivity.getResources().getString(R.string.hello_world),
                child0.getText().toString());
        assertTrue(tabHost.getTabContentView().getChildAt(1) instanceof ListView);
        child2 = (TextView) tabHost.getTabContentView().getChildAt(2);
        tabHost.setCurrentTab(0);
        assertEquals(TabHostStubActivity.INITIAL_VIEW_TEXT, child2.getText().toString());
    }

    @UiThreadTest
    public void testDispatchKeyEvent() {
        // Implementation details.
    }

    @UiThreadTest
    public void testDispatchWindowFocusChanged() {
        // Implementation details
    }

    /**
     * Check points:
     * 1. the specified callback should be invoked when the selected state of any of the items
     * in this list changes
     */
    @UiThreadTest
    public void testSetOnTabChangedListener() {
        TabHost tabHost = mActivity.getTabHost();

        // add a tab, and change current tab to the new tab
        MockOnTabChangeListener listener = new MockOnTabChangeListener();
        tabHost.setOnTabChangedListener(listener);

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryList());
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(1);
        assertTrue(listener.hasCalledOnTabChanged());

        // change current tab to the first one
        listener.reset();
        tabHost.setCurrentTab(0);
        assertTrue(listener.hasCalledOnTabChanged());

        // set the same tab
        listener.reset();
        tabHost.setCurrentTab(0);
        assertFalse(listener.hasCalledOnTabChanged());
    }

    @UiThreadTest
    public void testGetCurrentTabTag() {
        TabHost tabHost = mActivity.getTabHost();
        assertEquals(TabHostStubActivity.INITIAL_TAB_TAG, tabHost.getCurrentTabTag());

        TabSpec tabSpec = tabHost.newTabSpec(TAG_TAB2);
        tabSpec.setIndicator(TAG_TAB2);
        tabSpec.setContent(new MyTabContentFactoryList());
        tabHost.addTab(tabSpec);
        tabHost.setCurrentTab(1);
        assertEquals(TAG_TAB2, tabHost.getCurrentTabTag());
    }

    @UiThreadTest
    public void testOnAttachedToAndDetachedFromWindow() {
        // implementation details
    }

    private class MyTabContentFactoryText implements TabHost.TabContentFactory {
        public View createTabContent(String tag) {
            final TextView tv = new TextView(mActivity);
            tv.setText(tag);
            return tv;
        }
    }

    private class MyTabContentFactoryList implements TabHost.TabContentFactory {
        public View createTabContent(String tag) {
            final ListView lv = new ListView(mActivity);
            return lv;
        }
    }

    private class MockOnTabChangeListener implements OnTabChangeListener {
        private boolean mCalledOnTabChanged = false;

        boolean hasCalledOnTabChanged() {
            return mCalledOnTabChanged;
        }

        void reset() {
            mCalledOnTabChanged = false;
        }

        public void onTabChanged(String tabId) {
            mCalledOnTabChanged = true;
        }
    }

}
