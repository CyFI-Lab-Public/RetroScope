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
 * A pointer to a suggestion in a {@link SuggestionCursor}.
 *
 */
public class SuggestionPosition extends AbstractSuggestionWrapper {

    private final SuggestionCursor mCursor;

    private final int mPosition;

    public SuggestionPosition(SuggestionCursor cursor) {
        this(cursor, cursor.getPosition());
    }

    public SuggestionPosition(SuggestionCursor cursor, int suggestionPos) {
        mCursor = cursor;
        mPosition = suggestionPos;
    }

    public SuggestionCursor getCursor() {
        return mCursor;
    }

    /**
     * Gets the suggestion cursor, moved to point to the right suggestion.
     */
    @Override
    protected Suggestion current() {
        mCursor.moveTo(mPosition);
        return mCursor;
    }

    public int getPosition() {
        return mPosition;
    }

    @Override
    public String toString() {
        return mCursor + ":" + mPosition;
    }

}
