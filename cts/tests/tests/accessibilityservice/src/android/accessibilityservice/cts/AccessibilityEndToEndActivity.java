/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.accessibilityservice.cts;

import com.android.cts.accessibilityservice.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This class is an {@link Activity} used to perform end-to-end
 * testing of the accessibility feature by interaction with the
 * UI widgets.
 */
public class AccessibilityEndToEndActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.end_to_end_test);

        ListAdapter listAdapter = new BaseAdapter() {
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) View
                        .inflate(AccessibilityEndToEndActivity.this, R.layout.list_view_row, null);
                textView.setText((String) getItem(position));
                return textView;
            }

            public long getItemId(int position) {
                return position;
            }

            public Object getItem(int position) {
                if (position == 0) {
                    return AccessibilityEndToEndActivity.this.getString(R.string.first_list_item);
                } else {
                    return AccessibilityEndToEndActivity.this.getString(R.string.second_list_item);
                }
            }

            public int getCount() {
                return 2;
            }
        };

        ListView listView = (ListView) findViewById(R.id.listview);
        listView.setAdapter(listAdapter);
    }
}
