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

package com.android.cts.taskswitching.appb;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Simple activity to notify completion via broadcast after onResume.
 * This is for measuring taskswitching time between two apps.
 */
public class AppBActivity extends ListActivity {
    static final String TAG = "AppBActivity";
    private static final int NUMBER_ELEMENTS = 1000;
    private static final String TASKSWITCHING_INTENT = "com.android.cts.taskswitching.appb";
    private Handler mHandler;

    private String[] mItems = new String[NUMBER_ELEMENTS];

    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        for (int i = 0; i < NUMBER_ELEMENTS; i++) {
            mItems[i] = "B" + Integer.toString(i);
        }
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mItems));
        ListView view = getListView();
        mHandler = new Handler();
    }

    public void onResume()
    {
        super.onResume();
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(TASKSWITCHING_INTENT);
                sendBroadcast(intent);
            }
        });
    }
}
