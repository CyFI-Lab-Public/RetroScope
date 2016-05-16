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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

/**
 * Settings panel for photo flipping dream.
 */
public class SectionedAlbumDataAdapter extends DataSetObserver implements ListAdapter {
    private static final String TAG = "SectionedAlbumDataAdapter";
    private static final boolean DEBUG = false;

    private final LayoutInflater mInflater;
    private final int mLayout;
    private final AlbumDataAdapter mAlbumData;
    private int[] sections;

    public SectionedAlbumDataAdapter(Context context, SharedPreferences settings,
            int headerLayout, int itemLayout, List<PhotoSource.AlbumData> objects) {
        mLayout = headerLayout;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAlbumData = new AlbumDataAdapter(context, settings, itemLayout, objects);
        mAlbumData.sort(new AlbumDataAdapter.AccountComparator());
        onChanged();
        mAlbumData.registerDataSetObserver(this);
    }

    boolean areAllSelected() {
        return mAlbumData != null && mAlbumData.areAllSelected();
    }

    void selectAll(boolean select) {
        if (mAlbumData != null) {
            mAlbumData.selectAll(select);
        }
    }

    // DataSetObserver

    @Override
    public void onChanged() {
        if (DEBUG) Log.i(TAG, "onChanged");
        int numSections = 0;
        String previous = "";
        if (DEBUG) Log.i(TAG, "numAlbums = " + mAlbumData.getCount());
        for (int i = 0; i < mAlbumData.getCount(); i++) {
            PhotoSource.AlbumData item = mAlbumData.getItem(i);
            if (previous.isEmpty() || !previous.equals(item.account)) {
                if (DEBUG) Log.i(TAG, "previous = " + previous +", title = " + item.account);
                previous = item.account;
                numSections++;
            }
        }

        if (DEBUG) Log.i(TAG, "numSections = " + numSections);
        sections = new int[numSections];
        numSections = 0;
        previous = "";

        for (int i = 0; i < mAlbumData.getCount(); i++) {
            PhotoSource.AlbumData item = mAlbumData.getItem(i);
            if (previous.isEmpty() || !previous.equals(item.account)) {
                previous = item.account;
                sections[numSections] = i;
                numSections++;
            }
        }

        for (int i = 0; i < sections.length; i++) {
            sections[i] += i;
            if (DEBUG) Log.i(TAG, i + ": " + sections[i]);
        }
    }

    @Override
    public void onInvalidated() {
        onChanged();
    }

    // ListAdapter

    @Override
    public boolean areAllItemsEnabled() {
        return mAlbumData.areAllItemsEnabled();
    }

    @Override
    public boolean isEnabled(int position) {
        if (isHeader(position)) {
            return false;
        } else {
            return mAlbumData.isEnabled(internalPosition(position));
        }
    }

    // Adapter

    @Override
    public int getCount() {
        return mAlbumData.getCount() + sections.length;
    }

    @Override
    public Object getItem(int position) {
        if (isHeader(position)) {
            return mAlbumData.getItem(internalPosition(position+1)).account;
        } else {
            return mAlbumData.getItem(internalPosition(position));
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return mAlbumData.getViewTypeCount();
        } else {
            return mAlbumData.getItemViewType(internalPosition(position));
        }
    }

    @Override
    public int getViewTypeCount() {
        return mAlbumData.getViewTypeCount() + 1;
    }

    @Override
    public boolean hasStableIds() {
        return mAlbumData.hasStableIds();
    }

    @Override
    public boolean isEmpty() {
        return mAlbumData.isEmpty();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mAlbumData.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mAlbumData.unregisterDataSetObserver(observer);
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
        if (isHeader(position)) {
            if (DEBUG) Log.i(TAG, "header at " + position);
            View item = convertView;
            if (item == null) {
                item = mInflater.inflate(mLayout, parent, false);
            }
            View vTextView = item.findViewById(R.id.title);
            if (vTextView != null && vTextView instanceof TextView) {
                TextView textView = (TextView) vTextView;
                textView.setText((String) getItem(position));
            }
            return item;
        } else {
            if (DEBUG) Log.i(TAG, "non-header at " + position +
                             " fetching " + internalPosition(position));
            View item = mAlbumData.getView(internalPosition(position), convertView, parent);
            return item;
        }
    }

    // internal

    private boolean isHeader(int position) {
        return (Arrays.binarySearch(sections, position) >= 0);
    }

    private int internalPosition(int position) {
        int offset = Arrays.binarySearch(sections, position);
        if (offset < 0) {
            offset = -(offset + 1);
        }
        return position - offset;
    }
}
