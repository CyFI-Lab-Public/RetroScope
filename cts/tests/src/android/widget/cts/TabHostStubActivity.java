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

package android.widget.cts;

import android.app.TabActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import com.android.cts.stub.R;

/**
 * A minimal application for TabHost test.
 * It contains an initial tab whose tag is INITIAL_TAB_TAG and label is INITIAL_TAB_LABEL.
 */
public class TabHostStubActivity extends TabActivity {
    public static final String INITIAL_TAB_TAG = "initial tag";
    public static final String INITIAL_TAB_LABEL = "initial label";
    public static final String INITIAL_VIEW_TEXT = "initial view text";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabhost_layout);

        TabHost tabHost = getTabHost();

        // at least one tab
        tabHost.addTab(tabHost.newTabSpec(INITIAL_TAB_TAG)
                .setIndicator(INITIAL_TAB_LABEL)
                .setContent(new MyTabContentFactory()));
    }

    private class MyTabContentFactory implements TabHost.TabContentFactory {
        public View createTabContent(String tag) {
            final TextView tv = new TextView(getApplicationContext());
            tv.setText(INITIAL_VIEW_TEXT);
            return tv;
        }
    }
}
