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

package com.android.cts.ui;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Activity for measuring scrolling time of long list.
 */
public class ScrollingActivity extends ListActivity implements OnScrollListener {
    static final String TAG = "ScrollingActivity";
    private static final String NUM_ELEMENTS_EXTRA = "num_elements";
    private static final int NUM_ELEMENTS_DEFAULT = 10000;
    private static final int SCROLL_TIME_IN_MS = 1;
    private static final int WAIT_TIMEOUT_IN_SECS = 5 * 60;
    private String[] mItems;
    private CountDownLatch mLatchStop = null;
    private int mTargetLoc;
    private int mNumElements;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mNumElements = getIntent().getIntExtra(NUM_ELEMENTS_EXTRA, NUM_ELEMENTS_DEFAULT);
        mItems = new String[mNumElements];
        for (int i = 0; i < mNumElements; i++) {
            mItems[i] = Integer.toString(i);
        }
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mItems));
        ListView view = getListView();
        view.setOnScrollListener(this);
    }

    public boolean scrollToTop() {
        return doScroll(0);
    }
    public boolean scrollToBottom() {
        return doScroll(mNumElements - 1);
    }

    private boolean doScroll(final int loc) {
        mLatchStop = new CountDownLatch(1);
        mTargetLoc = loc;
        final ListView view = getListView();
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                view.smoothScrollToPositionFromTop(loc, 0, SCROLL_TIME_IN_MS);
            }
        });
        boolean result = false;
        try {
            result = mLatchStop.await(WAIT_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        mLatchStop = null;
        return result;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
            int visibleItemCount, int totalItemCount) {
        //Log.i(TAG, "onScroll " + firstVisibleItem + " " + visibleItemCount);
        if ((mTargetLoc >= firstVisibleItem) &&
                (mTargetLoc <= (firstVisibleItem + visibleItemCount))) {
            if (mLatchStop != null) {
                mLatchStop.countDown();
            }
        }
    }
}
