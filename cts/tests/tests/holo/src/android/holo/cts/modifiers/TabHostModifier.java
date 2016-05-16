/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts.modifiers;

import com.android.cts.holo.R;

import android.view.View;
import android.widget.TabHost;

public class TabHostModifier extends AbstractLayoutModifier {

    @Override
    public View modifyView(View view) {
        TabHost tabHost = (TabHost) view;
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("1")
                .setIndicator("Tab 1")
                .setContent(R.id.tab_inner_view));

        tabHost.addTab(tabHost.newTabSpec("2")
                .setIndicator("Tab 2")
                .setContent(R.id.tab_inner_view));

        tabHost.addTab(tabHost.newTabSpec("3")
                .setIndicator("Tab 3")
                .setContent(R.id.tab_inner_view));

        tabHost.setCurrentTab(2);

        return view;
    }
}
