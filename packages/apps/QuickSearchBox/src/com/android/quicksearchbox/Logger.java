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

package com.android.quicksearchbox;



/**
 * Interface for logging implementations.
 */
public interface Logger {

    public static final int SEARCH_METHOD_BUTTON = 0;
    public static final int SEARCH_METHOD_KEYBOARD = 1;

    public static final int SUGGESTION_CLICK_TYPE_LAUNCH = 0;
    public static final int SUGGESTION_CLICK_TYPE_REFINE = 1;
    public static final int SUGGESTION_CLICK_TYPE_QUICK_CONTACT = 2;

    /**
     * Called when QSB has started.
     *
     * @param latency User-visible start-up latency in milliseconds.
     */
    void logStart(int onCreateLatency, int latency, String intentSource);

    /**
     * Called when a suggestion is clicked.
     *
     * @param suggestionId Suggestion ID; 0-based position of the suggestion in the UI if the list
     *      is flat.
     * @param suggestionCursor all the suggestions shown in the UI.
     * @param clickType One of the SUGGESTION_CLICK_TYPE constants.
     */
    void logSuggestionClick(long suggestionId, SuggestionCursor suggestionCursor,  int clickType);

    /**
     * The user launched a search.
     *
     * @param startMethod One of {@link #SEARCH_METHOD_BUTTON} or {@link #SEARCH_METHOD_KEYBOARD}.
     * @param numChars The number of characters in the query.
     */
    void logSearch(int startMethod, int numChars);

    /**
     * The user launched a voice search.
     */
    void logVoiceSearch();

    /**
     * The user left QSB without performing any action (click suggestions, search or voice search).
     *
     * @param suggestionCursor all the suggestions shown in the UI when the user left
     * @param numChars The number of characters in the query typed when the user left.
     */
    void logExit(SuggestionCursor suggestionCursor, int numChars);

    /**
     * Logs the latency of a suggestion query to a specific source.
     *
     * @param result The result of the query.
     */
    void logLatency(SourceResult result);

}
