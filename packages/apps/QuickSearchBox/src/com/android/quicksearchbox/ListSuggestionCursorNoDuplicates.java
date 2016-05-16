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

import android.util.Log;

import java.util.HashSet;

/**
 * A SuggestionCursor that is backed by a list of SuggestionPosition objects
 * and doesn't allow duplicate suggestions.
 */
public class ListSuggestionCursorNoDuplicates extends ListSuggestionCursor {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.ListSuggestionCursorNoDuplicates";

    private final HashSet<String> mSuggestionKeys;

    public ListSuggestionCursorNoDuplicates(String userQuery) {
        super(userQuery);
        mSuggestionKeys = new HashSet<String>();
    }

    @Override
    public boolean add(Suggestion suggestion) {
        String key = SuggestionUtils.getSuggestionKey(suggestion);
        if (mSuggestionKeys.add(key)) {
            return super.add(suggestion);
        } else {
            if (DBG) Log.d(TAG, "Rejecting duplicate " + key);
            return false;
        }
    }

}
