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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Settings panel for photo flipping dream.
 */
public class AlbumDataAdapter extends ArrayAdapter<PhotoSource.AlbumData> {
    private static final String TAG = "AlbumDataAdapter";
    private static final boolean DEBUG = false;

    public static final String ALBUM_SET = "Enabled Album Set";

    private final AlbumSettings mSettings;
    private final LayoutInflater mInflater;
    private final int mLayout;
    private final ItemClickListener mListener;
    private final HashSet<String> mValidAlbumIds;

    public AlbumDataAdapter(Context context, SharedPreferences settings,
            int resource, List<PhotoSource.AlbumData> objects) {
        super(context, resource, objects);
        mSettings = AlbumSettings.getAlbumSettings(settings);
        mLayout = resource;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mListener = new ItemClickListener();

        mValidAlbumIds = new HashSet<String>(objects.size());
        for (PhotoSource.AlbumData albumData: objects) {
            mValidAlbumIds.add(albumData.id);
        }
        mSettings.pruneObsoleteSettings(mValidAlbumIds);
    }

    public boolean isSelected(int position) {
        PhotoSource.AlbumData data = getItem(position);
        return mSettings.isAlbumEnabled(data.id);
    }

    public boolean areAllSelected() {
        return mSettings.areAllEnabled(mValidAlbumIds);
    }

    public void selectAll(boolean select) {
        if (select) {
            mSettings.enableAllAlbums(mValidAlbumIds);
        } else {
            mSettings.disableAllAlbums();
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View item = convertView;
        if (item == null) {
            item = mInflater.inflate(mLayout, parent, false);
        }
        PhotoSource.AlbumData data = getItem(position);

        View vCheckBox = item.findViewById(R.id.enabled);
        if (vCheckBox != null && vCheckBox instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) vCheckBox;
            checkBox.setChecked(isSelected(position));
            checkBox.setTag(R.id.data_payload, data);
        }

        View vTextView = item.findViewById(R.id.title);
        if (vTextView != null && vTextView instanceof TextView) {
            TextView textView = (TextView) vTextView;
            textView.setText(data.title);
        }

        item.setOnClickListener(mListener);
        return item;
    }

    public static class AccountComparator implements Comparator<PhotoSource.AlbumData> {
        private final RecencyComparator recency;
        public AccountComparator() {
            recency = new RecencyComparator();
        }

        @Override
        public int compare(PhotoSource.AlbumData a, PhotoSource.AlbumData b) {
            if (a.account == b.account) {
                return recency.compare(a, b);
            } else {
                String typeAString = a.getType();
                String typeBString = b.getType();
                int typeA = 1;
                int typeB = 1;

                if (typeAString.equals(LocalSource.class.getName())) {
                    typeA = 0;
                }
                if (typeBString.equals(LocalSource.class.getName())) {
                    typeB = 0;
                }

                if (typeAString.equals(StockSource.class.getName())) {
                    typeA = 2;
                }
                if (typeBString.equals(StockSource.class.getName())) {
                    typeB = 2;
                }

                if (typeA == typeB) {
                    return a.account.compareTo(b.account);
                } else {
                    return (int) Math.signum(typeA - typeB);
                }
            }
        }
    }

    public static class RecencyComparator implements Comparator<PhotoSource.AlbumData> {
        private final TitleComparator title;
        public RecencyComparator() {
            title = new TitleComparator();
        }

        @Override
        public int compare(PhotoSource.AlbumData a, PhotoSource.AlbumData b) {
            if (a.updated == b.updated) {
                return title.compare(a, b);
            } else {
                return (int) Math.signum(b.updated - a.updated);
            }
        }
    }

    public static class TitleComparator implements Comparator<PhotoSource.AlbumData> {
        @Override
        public int compare(PhotoSource.AlbumData a, PhotoSource.AlbumData b) {
            return a.title.compareTo(b.title);
        }
    }

    private class ItemClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            final View vCheckBox = v.findViewById(R.id.enabled);
            if (vCheckBox != null && vCheckBox instanceof CheckBox) {
                final CheckBox checkBox = (CheckBox) vCheckBox;
                final PhotoSource.AlbumData data =
                    (PhotoSource.AlbumData) checkBox.getTag(R.id.data_payload);
                final boolean isChecked = !checkBox.isChecked();
                checkBox.setChecked(isChecked);
                mSettings.setAlbumEnabled(data.id, isChecked);
                notifyDataSetChanged();
                if (DEBUG) Log.i(TAG, data.title + " is " +
                                 (isChecked ? "" : "not") + " enabled");
            } else {
                if (DEBUG) Log.w(TAG, "no checkbox found in settings row!");
            }
            v.setPressed(true);
        }
    }
}
