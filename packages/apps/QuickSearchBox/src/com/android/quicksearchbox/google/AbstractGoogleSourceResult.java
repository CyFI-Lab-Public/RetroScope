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
package com.android.quicksearchbox.google;

import com.android.quicksearchbox.R;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionExtras;

import android.content.ComponentName;
import android.database.DataSetObserver;

import java.util.Collection;

public abstract class AbstractGoogleSourceResult implements SourceResult {

    private final Source mSource;
    private final String mUserQuery;
    private int mPos = 0;

    public AbstractGoogleSourceResult(Source source, String userQuery) {
        mSource = source;
        mUserQuery = userQuery;
    }

    public abstract int getCount();

    public abstract String getSuggestionQuery();

    public Source getSource() {
        return mSource;
    }

    public void close() {
    }

    public int getPosition() {
        return mPos;
    }

    public String getUserQuery() {
        return mUserQuery;
    }

    public void moveTo(int pos) {
        mPos = pos;
    }

    public boolean moveToNext() {
        int size = getCount();
        if (mPos >= size) {
            // Already past the end
            return false;
        }
        mPos++;
        return mPos < size;
    }

    public void registerDataSetObserver(DataSetObserver observer) {
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    public String getSuggestionText1() {
        return getSuggestionQuery();
    }

    public Source getSuggestionSource() {
        return mSource;
    }

    public boolean isSuggestionShortcut() {
        return false;
    }

    public String getShortcutId() {
        return null;
    }

    public String getSuggestionFormat() {
        return null;
    }

    public String getSuggestionIcon1() {
        return String.valueOf(R.drawable.magnifying_glass);
    }

    public String getSuggestionIcon2() {
        return null;
    }

    public String getSuggestionIntentAction() {
        return mSource.getDefaultIntentAction();
    }

    public ComponentName getSuggestionIntentComponent() {
        return mSource.getIntentComponent();
    }

    public String getSuggestionIntentDataString() {
        return null;
    }

    public String getSuggestionIntentExtraData() {
        return null;
    }

    public String getSuggestionLogType() {
        return null;
    }

    public String getSuggestionText2() {
        return null;
    }

    public String getSuggestionText2Url() {
        return null;
    }

    public boolean isSpinnerWhileRefreshing() {
        return false;
    }

    public boolean isWebSearchSuggestion() {
        return true;
    }

    public boolean isHistorySuggestion() {
        return false;
    }

    public SuggestionExtras getExtras() {
        return null;
    }

    public Collection<String> getExtraColumns() {
        return null;
    }
}
