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

package com.android.quicksearchbox.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.SuggestionPosition;
import com.android.quicksearchbox.Suggestions;

/**
 * Uses a {@link Suggestions} object to back a {@link SuggestionsView}.
 */
public class SuggestionsListAdapter extends SuggestionsAdapterBase<ListAdapter> {

    private Adapter mAdapter;

    public SuggestionsListAdapter(SuggestionViewFactory viewFactory) {
        super(viewFactory);
        mAdapter = new Adapter();
    }

    @Override
    public boolean isEmpty() {
        return mAdapter.getCount() == 0;
    }

    @Override
    public SuggestionPosition getSuggestion(long suggestionId) {
        return new SuggestionPosition(getCurrentSuggestions(), (int) suggestionId);
    }

    @Override
    public BaseAdapter getListAdapter() {
        return mAdapter;
    }

    @Override
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        mAdapter.notifyDataSetInvalidated();
    }

    class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            SuggestionCursor s = getCurrentSuggestions();
            return s == null ? 0 : s.getCount();
        }

        @Override
        public Object getItem(int position) {
            return getSuggestion(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return SuggestionsListAdapter.this.getView(
                    getCurrentSuggestions(), position, position, convertView, parent);
        }

        @Override
        public int getItemViewType(int position) {
            return getSuggestionViewType(getCurrentSuggestions(), position);
        }

        @Override
        public int getViewTypeCount() {
            return getSuggestionViewTypeCount();
        }

    }

}
