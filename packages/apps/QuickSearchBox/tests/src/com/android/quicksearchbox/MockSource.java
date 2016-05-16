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

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import com.android.quicksearchbox.util.Now;
import com.android.quicksearchbox.util.NowOrLater;

/**
 * Mock implementation of {@link Source}.
 *
 */
public class MockSource implements Source {

    public static final MockSource SOURCE_1 = new MockSource("SOURCE_1");

    public static final MockSource SOURCE_2 = new MockSource("SOURCE_2");

    public static final MockSource SOURCE_3 = new MockSource("SOURCE_3");

    public static final MockSource WEB_SOURCE = new MockSource("WEB") {
        @Override
        public SuggestionData createSuggestion(String query) {
            return new SuggestionData(this)
                    .setText1(query)
                    .setIntentAction(Intent.ACTION_WEB_SEARCH)
                    .setSuggestionQuery(query);
        }

    };

    private final String mName;

    public MockSource(String name) {
        this(name, 0);
    }

    public MockSource(String name, int versionCode) {
        mName = name;
    }

    public ComponentName getIntentComponent() {
        // Not an activity, but no code should treat it as one.
        return new ComponentName("com.android.quicksearchbox",
                getClass().getName() + "." + mName);
    }

    public String getSuggestUri() {
        return null;
    }

    public String getName() {
        return getIntentComponent().flattenToShortString();
    }

    public String getDefaultIntentAction() {
        return Intent.ACTION_SEARCH;
    }

    public String getDefaultIntentData() {
        return null;
    }

    @Override
    public NowOrLater<Drawable> getIcon(String drawableId) {
        return new Now<Drawable>(null);
    }

    public Uri getIconUri(String drawableId) {
        return null;
    }

    public String getLabel() {
        return "MockSource " + mName;
    }

    public int getQueryThreshold() {
        return 0;
    }

    public CharSequence getHint() {
        return null;
    }

    public String getSettingsDescription() {
        return "Suggestions from MockSource " + mName;
    }

    public Drawable getSourceIcon() {
        return null;
    }

    public Uri getSourceIconUri() {
        return null;
    }

    public boolean canRead() {
        return true;
    }

    public SourceResult getSuggestions(String query, int queryLimit) {
        if (query.length() == 0) {
            return null;
        }
        ListSuggestionCursor cursor = new ListSuggestionCursor(query);
        cursor.add(createSuggestion(query + "_1"));
        cursor.add(createSuggestion(query + "_2"));
        return new Result(query, cursor);
    }

    public SuggestionData createSuggestion(String query) {
        Uri data = new Uri.Builder().scheme("content").authority(mName).path(query).build();
        return new SuggestionData(this)
                .setText1(query)
                .setIntentAction(Intent.ACTION_VIEW)
                .setIntentData(data.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o.getClass().equals(this.getClass())) {
            MockSource s = (MockSource) o;
            return s.mName.equals(mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

    private class Result extends SuggestionCursorWrapper implements SourceResult {

        public Result(String userQuery, SuggestionCursor cursor) {
            super(userQuery, cursor);
        }

        public Source getSource() {
            return MockSource.this;
        }

    }

    public boolean queryAfterZeroResults() {
        return false;
    }

    public Intent createSearchIntent(String query, Bundle appData) {
        return null;
    }

    public Intent createVoiceSearchIntent(Bundle appData) {
        return null;
    }

    public boolean voiceSearchEnabled() {
        return false;
    }

    public boolean includeInAll() {
        return true;
    }

    public Source getRoot() {
        return this;
    }

}
