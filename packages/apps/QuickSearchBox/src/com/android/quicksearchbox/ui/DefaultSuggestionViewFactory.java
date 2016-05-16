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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.android.quicksearchbox.Suggestion;
import com.android.quicksearchbox.SuggestionCursor;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Suggestion view factory for Google suggestions.
 */
public class DefaultSuggestionViewFactory implements SuggestionViewFactory {

    private final LinkedList<SuggestionViewFactory> mFactories
            = new LinkedList<SuggestionViewFactory>();
    private final SuggestionViewFactory mDefaultFactory;
    private HashSet<String> mViewTypes;

    public DefaultSuggestionViewFactory(Context context) {
        mDefaultFactory = new DefaultSuggestionView.Factory(context);
        addFactory(new WebSearchSuggestionView.Factory(context));
    }

    /**
     * Must only be called from the constructor
     */
    protected final void addFactory(SuggestionViewFactory factory) {
        mFactories.addFirst(factory);
    }

    @Override
    public Collection<String> getSuggestionViewTypes() {
        if (mViewTypes == null) {
            mViewTypes = new HashSet<String>();
            mViewTypes.addAll(mDefaultFactory.getSuggestionViewTypes());
            for (SuggestionViewFactory factory : mFactories) {
                mViewTypes.addAll(factory.getSuggestionViewTypes());
            }
        }
        return mViewTypes;
    }

    @Override
    public View getView(SuggestionCursor suggestion, String userQuery,
            View convertView, ViewGroup parent) {
        for (SuggestionViewFactory factory : mFactories) {
            if (factory.canCreateView(suggestion)) {
                return factory.getView(suggestion, userQuery, convertView, parent);
            }
        }
        return mDefaultFactory.getView(suggestion, userQuery, convertView, parent);
    }

    @Override
    public String getViewType(Suggestion suggestion) {
        for (SuggestionViewFactory factory : mFactories) {
            if (factory.canCreateView(suggestion)) {
                return factory.getViewType(suggestion);
            }
        }
        return mDefaultFactory.getViewType(suggestion);
    }

    @Override
    public boolean canCreateView(Suggestion suggestion) {
        return true;
    }

}
