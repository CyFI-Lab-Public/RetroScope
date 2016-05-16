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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.android.quicksearchbox.AbstractInternalSource;
import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.R;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursor;
import com.android.quicksearchbox.util.NamedTaskExecutor;

/**
 * Special source implementation for Google suggestions.
 */
public abstract class AbstractGoogleSource extends AbstractInternalSource implements GoogleSource {

    /*
     * This name corresponds to what was used in previous version of quick search box. We use the
     * same name so that shortcuts continue to work after an upgrade. (It also makes logging more
     * consistent).
     */
    private static final String GOOGLE_SOURCE_NAME =
        "com.android.quicksearchbox/.google.GoogleSearch";

    public AbstractGoogleSource(Context context, Handler uiThread, NamedTaskExecutor iconLoader) {
        super(context, uiThread, iconLoader);
    }

    @Override
    public abstract ComponentName getIntentComponent();

    @Override
    public abstract SuggestionCursor refreshShortcut(String shortcutId, String extraData);

    /**
     * Called by QSB to get web suggestions for a query.
     */
    @Override
    public abstract SourceResult queryInternal(String query);

    /**
     * Called by external apps to get web suggestions for a query.
     */
    @Override
    public abstract SourceResult queryExternal(String query);

    @Override
    public Intent createVoiceSearchIntent(Bundle appData) {
        return createVoiceWebSearchIntent(appData);
    }

    @Override
    public String getDefaultIntentAction() {
        return Intent.ACTION_WEB_SEARCH;
    }

    @Override
    public CharSequence getHint() {
        return getContext().getString(R.string.google_search_hint);
    }

    @Override
    public CharSequence getLabel() {
        return getContext().getString(R.string.google_search_label);
    }

    @Override
    public String getName() {
        return GOOGLE_SOURCE_NAME;
    }

    @Override
    public CharSequence getSettingsDescription() {
        return getContext().getString(R.string.google_search_description);
    }

    @Override
    protected int getSourceIconResource() {
        return R.mipmap.google_icon;
    }

    @Override
    public SourceResult getSuggestions(String query, int queryLimit) {
        return emptyIfNull(queryInternal(query), query);
    }

    public SourceResult getSuggestionsExternal(String query) {
        return emptyIfNull(queryExternal(query), query);
    }

    private SourceResult emptyIfNull(SourceResult result, String query) {
        return result == null ? new CursorBackedSourceResult(this, query) : result;
    }

    @Override
    public boolean voiceSearchEnabled() {
        return true;
    }

    @Override
    public boolean includeInAll() {
        return true;
    }

}
