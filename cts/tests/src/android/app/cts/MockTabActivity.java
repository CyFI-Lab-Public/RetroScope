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
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class MockTabActivity extends TabActivity {

    private static final String TAB1 = "tab1";
    private static final String TAB2 = "tab2";
    private static final String TAB3 = "tab3";

    public boolean isOnChildTitleChangedCalled;
    public boolean isOnPostCreateCalled;
    public boolean isOnSaveInstanceStateCalled;
    public boolean isOnContentChangedCalled;
    public static boolean isOnRestoreInstanceStateCalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec(TAB1).setIndicator(TAB1)
                .setContent(new Intent(this, ChildTabActivity.class)));

        tabHost.addTab(tabHost.newTabSpec(TAB2).setIndicator(TAB2)
                .setContent(new Intent(this, MockActivity.class)));

        tabHost.addTab(tabHost.newTabSpec(TAB3).setIndicator(TAB3).setContent(
                new Intent(this, AppStubActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

    }

    @Override
    protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
        super.onChildTitleChanged(childActivity, title);
        isOnChildTitleChangedCalled = true;
    }

    @Override
    protected void onPostCreate(Bundle icicle) {
        super.onPostCreate(icicle);
        isOnPostCreateCalled = true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        isOnRestoreInstanceStateCalled = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isOnSaveInstanceStateCalled = true;
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        isOnContentChangedCalled = true;
    }
}
