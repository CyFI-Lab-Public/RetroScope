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

package android.app.cts;


import android.app.Activity;
import android.app.Instrumentation;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.test.InstrumentationTestCase;
import android.widget.TabHost;

public class TabActivityTest extends InstrumentationTestCase {
    private Instrumentation mInstrumentation;
    private MockTabActivity mActivity;
    private Activity mChildActivity;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentation = super.getInstrumentation();
    }

    @Override
    protected void tearDown() throws Exception {
        if (mActivity != null) {
            if (!mActivity.isFinishing()) {
                mActivity.finish();
            } else if (mChildActivity != null) {
                if (!mChildActivity.isFinishing()) {
                    mChildActivity.finish();
                }
            }
        }
        super.tearDown();
    }

    public void testTabActivity() throws Throwable {
        // Test constructor
        new TabActivity();

        final String packageName = "com.android.cts.stub";
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(packageName, MockTabActivity.class.getName());
        mActivity = (MockTabActivity) mInstrumentation.startActivitySync(intent);
        // Test onPostCreate, onContentChanged. These two methods are invoked in starting
        // activity. Default values of isOnContentChangedCalled, isOnPostCreateCalled are false.
        assertTrue(mActivity.isOnContentChangedCalled);
        assertTrue(mActivity.isOnPostCreateCalled);

        // Can't get default value.
        final int defaultIndex = 1;
        mActivity.setDefaultTab(defaultIndex);
        final String defaultTab = "DefaultTab";
        mActivity.setDefaultTab(defaultTab);
        // Test getTabHost, getTabWidget
        final TabHost tabHost = mActivity.getTabHost();
        assertNotNull(tabHost);
        assertNotNull(tabHost.getTabWidget());
    }

    public void testChildTitleCallback() throws Exception {
        final Context context = mInstrumentation.getTargetContext();
        final Intent intent = new Intent(context, MockTabActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final MockTabActivity father = new MockTabActivity();
        final ComponentName componentName = new ComponentName(context, MockTabActivity.class);
        final ActivityInfo info = context.getPackageManager().getActivityInfo(componentName, 0);
        mChildActivity = mInstrumentation.newActivity(MockTabActivity.class, mInstrumentation
                .getTargetContext(), null, null, intent, info, MockTabActivity.class.getName(),
                father, null, null);

        assertNotNull(mChildActivity);
        final String newTitle = "New Title";
        mChildActivity.setTitle(newTitle);
        assertTrue(father.isOnChildTitleChangedCalled);
    }
}
