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
import android.database.Cursor;

import com.android.quicksearchbox.google.GoogleSource;

import java.util.Collection;

public class CursorBackedSourceResult extends CursorBackedSuggestionCursor
        implements SourceResult {

    private final GoogleSource mSource;

    public CursorBackedSourceResult(GoogleSource source, String userQuery) {
        this(source, userQuery, null);
    }

    public CursorBackedSourceResult(GoogleSource source, String userQuery, Cursor cursor) {
        super(userQuery, cursor);
        mSource = source;
    }

    public GoogleSource getSource() {
        return mSource;
    }

    @Override
    public GoogleSource getSuggestionSource() {
        return mSource;
    }

    @Override
    public ComponentName getSuggestionIntentComponent() {
        return mSource.getIntentComponent();
    }

    public boolean isSuggestionShortcut() {
        return false;
    }

    public boolean isHistorySuggestion() {
        return false;
    }

    @Override
    public String toString() {
        return mSource + "[" + getUserQuery() + "]";
    }

    @Override
    public SuggestionExtras getExtras() {
        if (mCursor == null) return null;
        return CursorBackedSuggestionExtras.createExtrasIfNecessary(mCursor, getPosition());
    }

    public Collection<String> getExtraColumns() {
        if (mCursor == null) return null;
        return CursorBackedSuggestionExtras.getExtraColumns(mCursor);
    }

}