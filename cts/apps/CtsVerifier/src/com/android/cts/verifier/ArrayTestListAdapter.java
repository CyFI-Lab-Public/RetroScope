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

package com.android.cts.verifier;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TestListAdapter} that works like {@link android.widget.ArrayAdapter}
 * where items can be added by calling {@link #add(TestListItem)} repeatedly.
 */
public class ArrayTestListAdapter extends TestListAdapter {

    private final List<TestListItem> mRows = new ArrayList<TestListItem>();

    public ArrayTestListAdapter(Context context) {
        super(context);
    }

    public void add(TestListItem item) {
        mRows.add(item);
        notifyDataSetChanged();
    }

    @Override
    protected List<TestListItem> getRows() {
        return mRows;
    }
}
