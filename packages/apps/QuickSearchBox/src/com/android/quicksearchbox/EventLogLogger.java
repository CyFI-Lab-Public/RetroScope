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

import android.content.Context;
import android.util.EventLog;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Logs events to {@link EventLog}.
 */
public class EventLogLogger implements Logger {

    private static final char LIST_SEPARATOR = '|';

    private final Context mContext;

    private final Config mConfig;

    private final String mPackageName;

    private final Random mRandom;

    public EventLogLogger(Context context, Config config) {
        mContext = context;
        mConfig = config;
        mPackageName = mContext.getPackageName();
        mRandom = new Random();
    }

    protected Context getContext() {
        return mContext;
    }

    protected int getVersionCode() {
        return QsbApplication.get(getContext()).getVersionCode();
    }

    protected Config getConfig() {
        return mConfig;
    }

    @Override
    public void logStart(int onCreateLatency, int latency, String intentSource) {
        // TODO: Add more info to startMethod
        String startMethod = intentSource;
        EventLogTags.writeQsbStart(mPackageName, getVersionCode(), startMethod,
                latency, null, null, onCreateLatency);
    }

    @Override
    public void logSuggestionClick(long id, SuggestionCursor suggestionCursor, int clickType) {
        String suggestions = getSuggestions(suggestionCursor);
        int numChars = suggestionCursor.getUserQuery().length();
        EventLogTags.writeQsbClick(id, suggestions, null, numChars,
                clickType);
    }

    @Override
    public void logSearch(int startMethod, int numChars) {
        EventLogTags.writeQsbSearch(null, startMethod, numChars);
    }

    @Override
    public void logVoiceSearch() {
        EventLogTags.writeQsbVoiceSearch(null);
    }

    @Override
    public void logExit(SuggestionCursor suggestionCursor, int numChars) {
        String suggestions = getSuggestions(suggestionCursor);
        EventLogTags.writeQsbExit(suggestions, numChars);
    }

    @Override
    public void logLatency(SourceResult result) {
    }

    private String getSuggestions(SuggestionCursor cursor) {
        StringBuilder sb = new StringBuilder();
        final int count = cursor == null ? 0 : cursor.getCount();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(LIST_SEPARATOR);
            cursor.moveTo(i);
            String source = cursor.getSuggestionSource().getName();
            String type = cursor.getSuggestionLogType();
            if (type == null) type = "";
            String shortcut = cursor.isSuggestionShortcut() ? "shortcut" : "";
            sb.append(source).append(':').append(type).append(':').append(shortcut);
        }
        return sb.toString();
    }

}
