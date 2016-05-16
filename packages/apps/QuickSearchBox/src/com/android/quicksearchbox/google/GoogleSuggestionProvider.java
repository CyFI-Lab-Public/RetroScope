/*
 * Copyright (C) 2008 The Android Open Source Project
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

import com.android.quicksearchbox.CursorBackedSourceResult;
import com.android.quicksearchbox.QsbApplication;
import com.android.quicksearchbox.Source;
import com.android.quicksearchbox.SourceResult;
import com.android.quicksearchbox.SuggestionCursorBackedCursor;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * A suggestion provider which provides content from Genie, a service that offers
 * a superset of the content provided by Google Suggest.
 */
public class GoogleSuggestionProvider extends ContentProvider {
    private static final boolean DBG = false;
    private static final String TAG = "QSB.GoogleSuggestionProvider";

    // UriMatcher constants
    private static final int SEARCH_SUGGEST = 0;
    private static final int SEARCH_SHORTCUT = 1;

    private UriMatcher mUriMatcher;

    private GoogleSource mSource;

    @Override
    public boolean onCreate() {
        mSource = QsbApplication.get(getContext()).getGoogleSource();
        mUriMatcher = buildUriMatcher(getContext());
        return true;
    }

    /**
     * This will always return {@link SearchManager#SUGGEST_MIME_TYPE} as this
     * provider is purely to provide suggestions.
     */
    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    private SourceResult emptyIfNull(SourceResult result, GoogleSource source, String query) {
        return result == null ? new CursorBackedSourceResult(source, query) : result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        if (DBG) Log.d(TAG, "query uri=" + uri);
        int match = mUriMatcher.match(uri);

        if (match == SEARCH_SUGGEST) {
            String query = getQuery(uri);
            return new SuggestionCursorBackedCursor(
                    emptyIfNull(mSource.queryExternal(query), mSource, query));
        } else if (match == SEARCH_SHORTCUT) {
            String shortcutId = getQuery(uri);
            String extraData =
                uri.getQueryParameter(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
            return new SuggestionCursorBackedCursor(mSource.refreshShortcut(shortcutId, extraData));
        } else {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    /**
     * Gets the search text from a uri.
     */
    private String getQuery(Uri uri) {
        if (uri.getPathSegments().size() > 1) {
            return uri.getLastPathSegment();
        } else {
            return "";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private UriMatcher buildUriMatcher(Context context) {
        String authority = getAuthority(context);
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                SEARCH_SUGGEST);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT,
                SEARCH_SHORTCUT);
        matcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*",
                SEARCH_SHORTCUT);
        return matcher;
    }

    protected String getAuthority(Context context) {
        return context.getPackageName() + ".google";
    }

}
