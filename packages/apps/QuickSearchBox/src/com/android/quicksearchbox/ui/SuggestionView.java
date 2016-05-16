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

/**
 * Interface to be implemented by any view appearing in the list of suggestions.
 */
public interface SuggestionView {
    /**
     * Set the view's contents based on the given suggestion.
     */
    void bindAsSuggestion(Suggestion suggestion, String userQuery);

    /**
     * Binds this view to a list adapter.
     *
     * @param adapter The adapter of the list which the view is appearing in
     * @param position The position of this view with the list.
     */
    void bindAdapter(SuggestionsAdapter<?> adapter, long position);

}
