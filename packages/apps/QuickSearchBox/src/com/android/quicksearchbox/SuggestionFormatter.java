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

import android.text.Spannable;

/**
 * Suggestion formatter interface. This is used to bold (or otherwise highlight) portions of a
 * suggestion which were not a part of the query.
 */
public abstract class SuggestionFormatter {

    private final TextAppearanceFactory mSpanFactory;

    protected SuggestionFormatter(TextAppearanceFactory spanFactory) {
        mSpanFactory = spanFactory;
    }

    /**
     * Formats a suggestion for display in the UI.
     *
     * @param query the query as entered by the user
     * @param suggestion the suggestion
     * @return Formatted suggestion text.
     */
    public abstract CharSequence formatSuggestion(String query, String suggestion);

    protected void applyQueryTextStyle(Spannable text, int start, int end) {
        if (start == end) return;
        setSpans(text, start, end, mSpanFactory.createSuggestionQueryTextAppearance());
    }

    protected void applySuggestedTextStyle(Spannable text, int start, int end) {
        if (start == end) return;
        setSpans(text, start, end, mSpanFactory.createSuggestionSuggestedTextAppearance());
    }

    private void setSpans(Spannable text, int start, int end, Object[] spans) {
        for (Object span : spans) {
            text.setSpan(span, start, end, 0);
        }
    }

}
