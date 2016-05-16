/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.cts.uiautomator;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

public class MainActivity extends FragmentActivity implements TestListFragment.Callbacks {

    private boolean mTwoPane;
    public static final String LOG_TAG = "UiAutomatorApp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the device is locked, this attempts to dismiss the KeyGuard
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.list_activity);

        if (findViewById(R.id.test_detail_container) != null) {
            mTwoPane = true;
            ((TestListFragment) getSupportFragmentManager().findFragmentById(R.id.item_list))
                    .setActivateOnItemClick(true);
        }
    }

    @Override
    public void onItemSelected(String id) {
        if (mTwoPane) {
            Fragment fragment = TestItems.getFragment(id);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.test_detail_container, fragment).commit();
        } else {
            Intent detailIntent = new Intent(this, SinglePaneDetailActivity.class);
            detailIntent.putExtra("item_id", id);
            startActivity(detailIntent);
        }
    }
}
