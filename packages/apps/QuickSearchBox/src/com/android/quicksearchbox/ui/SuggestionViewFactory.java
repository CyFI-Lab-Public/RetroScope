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

import android.view.View;
import android.view.ViewGroup;

import java.util.Collection;

/**
 * Factory interface for suggestion views.
 */
public interface SuggestionViewFactory {

    /**
     * Returns all the view types that are used by this factory. Each view type corresponds to a
     * specific layout that is used to display suggestions. The returned set must have at least one
     * item in it.
     *
     * View types must be unique across all suggestion view factories.
     */
    Collection<String> getSuggestionViewTypes();

    /**
     * Returns the view type to be used for displaying the given suggestion. This MUST correspond to
     * one of the view types returned by {@link #getSuggestionViewTypes()}.
     */
    String getViewType(Suggestion suggestion);

    /**
     * Gets a view corresponding to the current suggestion in the given cursor.
     *
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     * @param parent The parent that this view will eventually be attached to
     * @return A View corresponding to the data within this suggestion.
     */
    View getView(SuggestionCursor suggestion, String userQuery, View convertView, ViewGroup parent);

    /**
     * Checks whether this factory can create views for the given suggestion.
     */
    boolean canCreateView(Suggestion suggestion);

}
