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

package com.android.quicksearchbox.ui;

import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.Collections;

/**
 * Suggestion view factory that inflates views from XML.
 */
public class SuggestionViewInflater implements SuggestionViewFactory {

    private final String mViewType;
    private final Class<?> mViewClass;
    private final int mLayoutId;
    private final Context mContext;

    /**
     * @param viewType The unique type of views inflated by this factory
     * @param viewClass The expected type of view classes.
     * @param layoutId resource ID of layout to use.
     * @param context Context to use for inflating the views.
     */
    public SuggestionViewInflater(String viewType, Class<? extends SuggestionView> viewClass,
            int layoutId, Context context) {
        mViewType = viewType;
        mViewClass = viewClass;
        mLayoutId = layoutId;
        mContext = context;
    }

    protected LayoutInflater getInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public Collection<String> getSuggestionViewTypes() {
        return Collections.singletonList(mViewType);
    }

    public View getView(SuggestionCursor suggestion, String userQuery,
            View convertView, ViewGroup parent) {
        if (convertView == null || !convertView.getClass().equals(mViewClass)) {
            int layoutId = mLayoutId;
            convertView = getInflater().inflate(layoutId, parent, false);
        }
        if (!(convertView instanceof SuggestionView)) {
            throw new IllegalArgumentException("Not a SuggestionView: " + convertView);
        }
        ((SuggestionView) convertView).bindAsSuggestion(suggestion, userQuery);
        return convertView;
    }

    public String getViewType(Suggestion suggestion) {
        return mViewType;
    }

    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }

}
