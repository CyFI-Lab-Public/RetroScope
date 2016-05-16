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

/**
 * Listener interface for clicks on suggestions.
 */
public interface SuggestionClickListener {

    /**
     * Called when a suggestion is clicked.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param suggestionId The ID of the suggestion clicked. If the suggestion list is flat, this
     *      will be the position within the list.
     */
    void onSuggestionClicked(SuggestionsAdapter<?> adapter, long suggestionId);

    /**
     * Called when the "query refine" button of a suggestion is clicked.
     *
     * @param adapter Adapter that contains the clicked suggestion.
     * @param suggestionId The ID of the suggestion clicked. If the suggestion list is flat, this
     *      will be the position within the list.
     */
    void onSuggestionQueryRefineClicked(SuggestionsAdapter<?> adapter, long suggestionId);
}
