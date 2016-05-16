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

import com.google.common.annotations.VisibleForTesting;

import android.content.ComponentName;
import android.content.Intent;


/**
 * Holds data for each suggest item including the display data and how to launch the result.
 * Used for passing from the provider to the suggest cursor.
 */
public class SuggestionData implements Suggestion {

    private final Source mSource;
    private String mFormat;
    private String mText1;
    private String mText2;
    private String mText2Url;
    private String mIcon1;
    private String mIcon2;
    private String mShortcutId;
    private boolean mSpinnerWhileRefreshing;
    private String mIntentAction;
    private String mIntentData;
    private String mIntentExtraData;
    private String mSuggestionQuery;
    private String mLogType;
    private boolean mIsShortcut;
    private boolean mIsHistory;
    private SuggestionExtras mExtras;

    public SuggestionData(Source source) {
        mSource = source;
    }

    public Source getSuggestionSource() {
        return mSource;
    }

    public String getSuggestionFormat() {
        return mFormat;
    }

    public String getSuggestionText1() {
        return mText1;
    }

    public String getSuggestionText2() {
        return mText2;
    }

    public String getSuggestionText2Url() {
        return mText2Url;
    }

    public String getSuggestionIcon1() {
        return mIcon1;
    }

    public String getSuggestionIcon2() {
        return mIcon2;
    }

    public boolean isSpinnerWhileRefreshing() {
        return mSpinnerWhileRefreshing;
    }

    public String getIntentExtraData() {
        return mIntentExtraData;
    }

    public String getShortcutId() {
        return mShortcutId;
    }

    public String getSuggestionIntentAction() {
        if (mIntentAction != null) return mIntentAction;
        return mSource.getDefaultIntentAction();
    }

    public ComponentName getSuggestionIntentComponent() {
        return mSource.getIntentComponent();
    }

    public String getSuggestionIntentDataString() {
        return mIntentData;
    }

    public String getSuggestionIntentExtraData() {
        return mIntentExtraData;
    }

    public String getSuggestionQuery() {
        return mSuggestionQuery;
    }

    public String getSuggestionLogType() {
        return mLogType;
    }

    public boolean isSuggestionShortcut() {
        return mIsShortcut;
    }

    public boolean isWebSearchSuggestion() {
        return Intent.ACTION_WEB_SEARCH.equals(getSuggestionIntentAction());
    }

    public boolean isHistorySuggestion() {
        return mIsHistory;
    }

    @VisibleForTesting
    public SuggestionData setFormat(String format) {
        mFormat = format;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setText1(String text1) {
        mText1 = text1;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setText2(String text2) {
        mText2 = text2;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setText2Url(String text2Url) {
        mText2Url = text2Url;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIcon1(String icon1) {
        mIcon1 = icon1;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIcon2(String icon2) {
        mIcon2 = icon2;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIntentAction(String intentAction) {
        mIntentAction = intentAction;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIntentData(String intentData) {
        mIntentData = intentData;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIntentExtraData(String intentExtraData) {
        mIntentExtraData = intentExtraData;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setSuggestionQuery(String suggestionQuery) {
        mSuggestionQuery = suggestionQuery;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setShortcutId(String shortcutId) {
        mShortcutId = shortcutId;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setSpinnerWhileRefreshing(boolean spinnerWhileRefreshing) {
        mSpinnerWhileRefreshing = spinnerWhileRefreshing;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setSuggestionLogType(String logType) {
        mLogType = logType;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIsShortcut(boolean isShortcut) {
        mIsShortcut = isShortcut;
        return this;
    }

    @VisibleForTesting
    public SuggestionData setIsHistory(boolean isHistory) {
        mIsHistory = isHistory;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mFormat == null) ? 0 : mFormat.hashCode());
        result = prime * result + ((mIcon1 == null) ? 0 : mIcon1.hashCode());
        result = prime * result + ((mIcon2 == null) ? 0 : mIcon2.hashCode());
        result = prime * result + ((mIntentAction == null) ? 0 : mIntentAction.hashCode());
        result = prime * result + ((mIntentData == null) ? 0 : mIntentData.hashCode());
        result = prime * result + ((mIntentExtraData == null) ? 0 : mIntentExtraData.hashCode());
        result = prime * result + ((mLogType == null) ? 0 : mLogType.hashCode());
        result = prime * result + ((mShortcutId == null) ? 0 : mShortcutId.hashCode());
        result = prime * result + ((mSource == null) ? 0 : mSource.hashCode());
        result = prime * result + (mSpinnerWhileRefreshing ? 1231 : 1237);
        result = prime * result + ((mSuggestionQuery == null) ? 0 : mSuggestionQuery.hashCode());
        result = prime * result + ((mText1 == null) ? 0 : mText1.hashCode());
        result = prime * result + ((mText2 == null) ? 0 : mText2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SuggestionData other = (SuggestionData)obj;
        if (mFormat == null) {
            if (other.mFormat != null)
                return false;
        } else if (!mFormat.equals(other.mFormat))
            return false;
        if (mIcon1 == null) {
            if (other.mIcon1 != null)
                return false;
        } else if (!mIcon1.equals(other.mIcon1))
            return false;
        if (mIcon2 == null) {
            if (other.mIcon2 != null)
                return false;
        } else if (!mIcon2.equals(other.mIcon2))
            return false;
        if (mIntentAction == null) {
            if (other.mIntentAction != null)
                return false;
        } else if (!mIntentAction.equals(other.mIntentAction))
            return false;
        if (mIntentData == null) {
            if (other.mIntentData != null)
                return false;
        } else if (!mIntentData.equals(other.mIntentData))
            return false;
        if (mIntentExtraData == null) {
            if (other.mIntentExtraData != null)
                return false;
        } else if (!mIntentExtraData.equals(other.mIntentExtraData))
            return false;
        if (mLogType == null) {
            if (other.mLogType != null)
                return false;
        } else if (!mLogType.equals(other.mLogType))
            return false;
        if (mShortcutId == null) {
            if (other.mShortcutId != null)
                return false;
        } else if (!mShortcutId.equals(other.mShortcutId))
            return false;
        if (mSource == null) {
            if (other.mSource != null)
                return false;
        } else if (!mSource.equals(other.mSource))
            return false;
        if (mSpinnerWhileRefreshing != other.mSpinnerWhileRefreshing)
            return false;
        if (mSuggestionQuery == null) {
            if (other.mSuggestionQuery != null)
                return false;
        } else if (!mSuggestionQuery.equals(other.mSuggestionQuery))
            return false;
        if (mText1 == null) {
            if (other.mText1 != null)
                return false;
        } else if (!mText1.equals(other.mText1))
            return false;
        if (mText2 == null) {
            if (other.mText2 != null)
                return false;
        } else if (!mText2.equals(other.mText2))
            return false;
        return true;
    }

    /**
     * Returns a string representation of the contents of this SuggestionData,
     * for debugging purposes.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("SuggestionData(");
        appendField(builder, "source", mSource.getName());
        appendField(builder, "text1", mText1);
        appendField(builder, "intentAction", mIntentAction);
        appendField(builder, "intentData", mIntentData);
        appendField(builder, "query", mSuggestionQuery);
        appendField(builder, "shortcutid", mShortcutId);
        appendField(builder, "logtype", mLogType);
        return builder.toString();
    }

    private void appendField(StringBuilder builder, String name, String value) {
        if (value != null) {
            builder.append(",").append(name).append("=").append(value);
        }
    }

    @VisibleForTesting
    public void setExtras(SuggestionExtras extras) {
        mExtras = extras;
    }

    public SuggestionExtras getExtras() {
        return mExtras;
    }

}
