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

package android.holo.cts;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

/**
 * {@link ListActivity} for picking themes.
 */
public class ThemePickerActivity extends ListActivity {

    static final String EXTRA_TASK = "task";

    private int mTestTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTestTask = getIntent().getIntExtra(EXTRA_TASK, -1);
        setListAdapter(new ThemeAdapter(getLayoutInflater()));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, LayoutPickerActivity.class);
        intent.putExtra(LayoutPickerActivity.EXTRA_THEME_INDEX, position);
        intent.putExtra(LayoutPickerActivity.EXTRA_TASK, mTestTask);
        startActivity(intent);
    }
}
