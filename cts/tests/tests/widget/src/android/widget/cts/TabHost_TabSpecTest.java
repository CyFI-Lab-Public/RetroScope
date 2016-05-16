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
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.text.style.cts.MockURLSpanTestActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

/**
 * Test {@link TabSpec}.
 */
public class TabHost_TabSpecTest extends ActivityInstrumentationTestCase2<TabHostStubActivity> {
    private static final String TAG_TAB2 = "tab 2";

    private TabHost mTabHost;
    private TabHostStubActivity mActivity;

    public TabHost_TabSpecTest() {
        super("com.android.cts.stub", TabHostStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mTabHost = mActivity.getTabHost();
    }

    @UiThreadTest
    public void testSetIndicator1() {
        TabSpec tabSpec = mTabHost.newTabSpec(TAG_TAB2);

        // normal value
        tabSpec.setIndicator(TAG_TAB2).setContent(new MockTabContentFactoryText());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(1);
        View currentTabView = mTabHost.getCurrentTabView();
        int idTitle = com.android.internal.R.id.title;
        TextView tvTitle = (TextView) currentTabView.findViewById(idTitle);
        assertEquals(TAG_TAB2, tvTitle.getText().toString());

        // exceptional
        tabSpec = mTabHost.newTabSpec("tab 3");
        tabSpec.setIndicator((CharSequence)null).setContent(new MockTabContentFactoryList());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(2);
        currentTabView = mTabHost.getCurrentTabView();
        tvTitle = (TextView) currentTabView.findViewById(idTitle);
        assertEquals("", tvTitle.getText().toString());
    }

    @UiThreadTest
    public void testSetIndicator2() {
        TabSpec tabSpec = mTabHost.newTabSpec(TAG_TAB2);

        // normal value
        Drawable d = new ColorDrawable(Color.GRAY);
        tabSpec.setIndicator("", d);
        tabSpec.setContent(new MockTabContentFactoryText());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(1);
        View currentTabView = mTabHost.getCurrentTabView();
        int idTitle = com.android.internal.R.id.title;
        int idIcon = com.android.internal.R.id.icon;
        TextView tvTitle = (TextView) currentTabView.findViewById(idTitle);
        ImageView ivIcon = ((ImageView) currentTabView.findViewById(idIcon));
        assertEquals("", tvTitle.getText().toString());
        assertSame(d, ivIcon.getDrawable());

        // exceptional
        tabSpec = mTabHost.newTabSpec("tab 3");
        tabSpec.setIndicator(null, d);
        tabSpec.setContent(new MockTabContentFactoryList());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(2);
        currentTabView = mTabHost.getCurrentTabView();
        tvTitle = (TextView) currentTabView.findViewById(idTitle);
        ivIcon = ((ImageView) currentTabView.findViewById(idIcon));
        assertEquals("", tvTitle.getText().toString());
        assertSame(d, ivIcon.getDrawable());

        tabSpec = mTabHost.newTabSpec("tab 4");
        tabSpec.setIndicator(null, null);
        tabSpec.setContent(new MockTabContentFactoryList());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(3);
        currentTabView = mTabHost.getCurrentTabView();
        tvTitle = (TextView) currentTabView.findViewById(idTitle);
        ivIcon = ((ImageView) currentTabView.findViewById(idIcon));
        assertEquals("", tvTitle.getText().toString());
        assertNull(ivIcon.getDrawable());

        tabSpec = mTabHost.newTabSpec("tab 5");
        tabSpec.setIndicator("half cube", d);
        tabSpec.setContent(new MockTabContentFactoryList());
        mTabHost.addTab(tabSpec);
        mTabHost.setCurrentTab(4);
        currentTabView = mTabHost.getCurrentTabView();
        tvTitle = (TextView) currentTabView.findViewById(idTitle);
        ivIcon = ((ImageView) currentTabView.findViewById(idIcon));
        assertEquals("half cube", tvTitle.getText().toString());
        assertNull(ivIcon.getDrawable());
    }

    @UiThreadTest
    public void testSetContent1() {
        TabSpec tabSpec2 = mTabHost.newTabSpec("tab spec 2");
        tabSpec2.setIndicator("tab 2");
        // TabContentFactory to create a TextView as the content of the tab.
        tabSpec2.setContent(com.android.cts.stub.R.id.tabhost_textview);
        mTabHost.addTab(tabSpec2);
        mTabHost.setCurrentTab(1);
        TextView currentView = (TextView) mTabHost.getCurrentView();
        assertEquals(mActivity.getResources().getString(R.string.hello_world),
                currentView.getText().toString());

        TabSpec tabSpec3 = mTabHost.newTabSpec("tab spec 3");
        tabSpec3.setIndicator("tab 3");
        // TabContentFactory to create a ListView as the content of the tab.
        tabSpec3.setContent(com.android.cts.stub.R.id.tabhost_listview);
        mTabHost.addTab(tabSpec3);
        mTabHost.setCurrentTab(2);
        assertTrue(mTabHost.getCurrentView() instanceof ListView);
    }

    @UiThreadTest
    public void testSetContent2() {
        TabSpec tabSpec2 = mTabHost.newTabSpec("tab spec 2");
        tabSpec2.setIndicator("tab 2");
        // TabContentFactory to create a TextView as the content of the tab.
        tabSpec2.setContent(new MockTabContentFactoryText());
        mTabHost.addTab(tabSpec2);
        mTabHost.setCurrentTab(1);
        TextView currentView = (TextView) mTabHost.getCurrentView();
        assertEquals("tab spec 2", currentView.getText().toString());

        TabSpec tabSpec3 = mTabHost.newTabSpec("tab spec 3");
        tabSpec3.setIndicator("tab 3");
        // TabContentFactory to create a ListView as the content of the tab.
        tabSpec3.setContent(new MockTabContentFactoryList());
        mTabHost.addTab(tabSpec3);
        mTabHost.setCurrentTab(2);
        assertTrue(mTabHost.getCurrentView() instanceof ListView);
    }

    public void testSetContent3() {
        // The scheme of uri string must be "ctstest" to launch MockURLSpanTestActivity
        Uri uri = Uri.parse("ctstest://tabhost_tabspec/test");
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                TabSpec tabSpec = mTabHost.newTabSpec("tab spec");
                tabSpec.setIndicator("tab");
                tabSpec.setContent(intent);
                mTabHost.addTab(tabSpec);
                mTabHost.setCurrentTab(1);
            }
        });

        Instrumentation instrumentation = getInstrumentation();
        ActivityMonitor am = instrumentation.addMonitor(MockURLSpanTestActivity.class.getName(),
                null, false);

        Activity newActivity = am.waitForActivityWithTimeout(5000);
        assertNotNull(newActivity);
        newActivity.finish();
    }

    private class MockTabContentFactoryText implements TabHost.TabContentFactory {
        public View createTabContent(String tag) {
            final TextView tv = new TextView(mActivity);
            tv.setText(tag);
            return tv;
        }
    }

    private class MockTabContentFactoryList implements TabHost.TabContentFactory {
        public View createTabContent(String tag) {
            final ListView lv = new ListView(mActivity);
            return lv;
        }
    }
}
