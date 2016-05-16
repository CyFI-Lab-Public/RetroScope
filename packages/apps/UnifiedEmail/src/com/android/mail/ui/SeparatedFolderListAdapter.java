/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class SeparatedFolderListAdapter extends BaseAdapter {

    private final ArrayList<FolderSelectorAdapter> mSections =
            new ArrayList<FolderSelectorAdapter>();
    public final static int TYPE_SECTION_HEADER = 0;
    public final static int TYPE_ITEM = 1;

    public void addSection(FolderSelectorAdapter adapter) {
        mSections.add(adapter);
    }

    @Override
    public Object getItem(int position) {
        for (FolderSelectorAdapter adapter : mSections) {
            int size = adapter.getCount();

            // check if position inside this section
            if (position == 0 || position < size)
                return adapter.getItem(position);

            // otherwise jump into next section
            position -= size;
        }
        return null;
    }

    @Override
    public int getCount() {
        // total together all sections, plus one for each section header
        int total = 0;
        for (FolderSelectorAdapter adapter : mSections) {
            total += adapter.getCount();
        }
        return total;
    }

    @Override
    public int getViewTypeCount() {
        // assume that headers count as one, then total all sections
        int total = 0;
        for (Adapter adapter : mSections)
            total += adapter.getViewTypeCount();
        return total;
    }

    @Override
    public int getItemViewType(int position) {
        int type = 0;
        for (FolderSelectorAdapter adapter : mSections) {
            int size = adapter.getCount();
            // check if position inside this section
            if (position == 0 || position < size) {
                return type + adapter.getItemViewType(position);
            }

            // otherwise jump into next section
            position -= size;
            type += adapter.getViewTypeCount();
        }
        return -1;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return (getItemViewType(position) != TYPE_SECTION_HEADER);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        for (FolderSelectorAdapter adapter : mSections) {
            int size = adapter.getCount();
            if (position == 0 || position < size) {
                return adapter.getView(position, convertView, parent);
            }
            // otherwise jump into next section
            position -= size;
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}
