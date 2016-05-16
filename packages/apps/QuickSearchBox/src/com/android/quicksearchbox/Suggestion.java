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
package com.android.quicksearchbox;

import android.content.ComponentName;

/**
 * Interface for individual suggestions.
 */
public interface Suggestion {

    /**
     * Gets the source that produced the current suggestion.
     */
    Source getSuggestionSource();

    /**
     * Gets the shortcut ID of the current suggestion.
     */
    String getShortcutId();

    /**
     * Whether to show a spinner while refreshing this shortcut.
     */
    boolean isSpinnerWhileRefreshing();

    /**
     * Gets the format of the text returned by {@link #getSuggestionText1()}
     * and {@link #getSuggestionText2()}.
     *
     * @return {@code null} or "html"
     */
    String getSuggestionFormat();

    /**
     * Gets the first text line for the current suggestion.
     */
    String getSuggestionText1();

    /**
     * Gets the second text line for the current suggestion.
     */
    String getSuggestionText2();

    /**
     * Gets the second text line URL for the current suggestion.
     */
    String getSuggestionText2Url();

    /**
     * Gets the left-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link Source#getIcon(String)}.
     */
    String getSuggestionIcon1();

    /**
     * Gets the right-hand-side icon for the current suggestion.
     *
     * @return A string that can be passed to {@link Source#getIcon(String)}.
     */
    String getSuggestionIcon2();

    /**
     * Gets the intent action for the current suggestion.
     */
    String getSuggestionIntentAction();

    /**
     * Gets the name of the activity that the intent for the current suggestion will be sent to.
     */
    ComponentName getSuggestionIntentComponent();

    /**
     * Gets the extra data associated with this suggestion's intent.
     */
    String getSuggestionIntentExtraData();

    /**
     * Gets the data associated with this suggestion's intent.
     */
    String getSuggestionIntentDataString();

    /**
     * Gets the query associated with this suggestion's intent.
     */
    String getSuggestionQuery();

    /**
     * Gets the suggestion log type for the current suggestion. This is logged together
     * with the value returned from {@link Source#getName()}.
     * The value is source-specific. Most sources return {@code null}.
     */
    String getSuggestionLogType();

    /**
     * Checks if this suggestion is a shortcut.
     */
    boolean isSuggestionShortcut();

    /**
     * Checks if this is a web search suggestion.
     */
    boolean isWebSearchSuggestion();

    /**
     * Checks whether this suggestion comes from the user's search history.
     */
    boolean isHistorySuggestion();

    /**
     * Returns any extras associated with this suggestion, or {@code null} if there are none.
     */
    SuggestionExtras getExtras();

}
