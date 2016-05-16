/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import java.util.Arrays;
import java.util.List;

/**
 * An adapter that combines items from multiple provided adapters into a single list.
 *
 * @param <T> the class of each constituent adapter
 */
public class MergedAdapter<T extends MergedAdapter.ListSpinnerAdapter> extends BaseAdapter {

    private List<T> mAdapters;
    private final DataSetObserver mObserver;

    /**
     * A Mergeable adapter must implement both ListAdapter and SpinnerAdapter to be useful in lists
     * and spinners.
     */
    public interface ListSpinnerAdapter extends ListAdapter, SpinnerAdapter {
    }

    public static class LocalAdapterPosition<T extends ListSpinnerAdapter> {
        public final T mAdapter;
        public final int mLocalPosition;

        public LocalAdapterPosition(T adapter, int offset) {
            mAdapter = adapter;
            mLocalPosition = offset;
        }
    }

    public MergedAdapter() {
        mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }
        };
    }

    public void setAdapters(T... adapters) {
        if (mAdapters != null) {
            for (T adapter : mAdapters) {
                adapter.unregisterDataSetObserver(mObserver);
            }
        }

        mAdapters = Arrays.asList(adapters);

        for (T adapter : mAdapters) {
            adapter.registerDataSetObserver(mObserver);
        }
    }

    public int getSubAdapterCount() {
        return mAdapters.size();
    }

    public T getSubAdapter(int index) {
        return mAdapters.get(index);
    }

    @Override
    public int getCount() {
        int count = 0;
        for (T adapter : mAdapters) {
            count += adapter.getCount();
        }
        return count;
        // TODO: cache counts until next onChanged
    }

    /**
     * For a given merged position, find the corresponding Adapter and local position within that
     * Adapter by iterating through Adapters and summing their counts until the merged position is
     * found.
     *
     * @param position a merged (global) position
     * @return the matching Adapter and local position, or null if not found
     */
    public LocalAdapterPosition<T> getAdapterOffsetForItem(final int position) {
        final int adapterCount = mAdapters.size();
        int i = 0;
        int count = 0;

        while (i < adapterCount) {
            T a = mAdapters.get(i);
            int newCount = count + a.getCount();
            if (position < newCount) {
                return new LocalAdapterPosition<T>(a, position - count);
            }
            count = newCount;
            i++;
        }
        return null;
    }

    @Override
    public Object getItem(int position) {
        LocalAdapterPosition<T> result = getAdapterOffsetForItem(position);
        if (result == null) {
            return null;
        }
        return result.mAdapter.getItem(result.mLocalPosition);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        int count = 0;
        for (T adapter : mAdapters) {
            count += adapter.getViewTypeCount();
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        LocalAdapterPosition<T> result = getAdapterOffsetForItem(position);
        int otherViewTypeCount = 0;
        for (T adapter : mAdapters) {
            if (adapter == result.mAdapter) {
                break;
            }
            otherViewTypeCount += adapter.getViewTypeCount();
        }
        int type = result.mAdapter.getItemViewType(result.mLocalPosition);
        // Headers (negative types) are in a separate global namespace and their values should not
        // be affected by preceding adapter view types.
        if (type >= 0) {
            type += otherViewTypeCount;
        }
        return type;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LocalAdapterPosition<T> result = getAdapterOffsetForItem(position);
        return result.mAdapter.getView(result.mLocalPosition, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LocalAdapterPosition<T> result = getAdapterOffsetForItem(position);
        return result.mAdapter.getDropDownView(result.mLocalPosition, convertView, parent);
    }

    @Override
    public boolean areAllItemsEnabled() {
        boolean enabled = true;
        for (T adapter : mAdapters) {
            enabled &= adapter.areAllItemsEnabled();
        }
        return enabled;
    }

    @Override
    public boolean isEnabled(int position) {
        LocalAdapterPosition<T> result = getAdapterOffsetForItem(position);
        return result.mAdapter.isEnabled(result.mLocalPosition);
    }

}