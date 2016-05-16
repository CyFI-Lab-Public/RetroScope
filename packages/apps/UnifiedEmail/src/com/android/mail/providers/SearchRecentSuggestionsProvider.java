/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.providers;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.android.mail.R;

import java.util.ArrayList;

public class SearchRecentSuggestionsProvider extends ContentProvider {
    /*
     * String used to delimit different parts of a query.
     */
    public static final String QUERY_TOKEN_SEPARATOR = " ";

    // client-provided configuration values
    private String mAuthority;
    private int mMode;

    // general database configuration and tables
    private SQLiteOpenHelper mOpenHelper;
    private static final String sDatabaseName = "suggestions.db";
    private static final String sSuggestions = "suggestions";
    private static final String ORDER_BY = "date DESC";
    private static final String NULL_COLUMN = "query";

    // Table of database versions.  Don't forget to update!
    // NOTE:  These version values are shifted left 8 bits (x 256) in order to create space for
    // a small set of mode bitflags in the version int.
    //
    // 1      original implementation with queries, and 1 or 2 display columns
    // 1->2   added UNIQUE constraint to display1 column
    private static final int DATABASE_VERSION = 2 * 256;

    /**
     * This mode bit configures the database to record recent queries.  <i>required</i>
     *
     * @see #setupSuggestions(String, int)
     */
    public static final int DATABASE_MODE_QUERIES = 1;

    // Uri and query support
    private static final int URI_MATCH_SUGGEST = 1;

    private Uri mSuggestionsUri;
    private UriMatcher mUriMatcher;

    private String mSuggestSuggestionClause;
    private String[] mSuggestionProjection;

    /**
     * Builds the database.  This version has extra support for using the version field
     * as a mode flags field, and configures the database columns depending on the mode bits
     * (features) requested by the extending class.
     *
     * @hide
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context, int newVersion) {
            super(context, sDatabaseName, null, newVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE suggestions (" +
                    "_id INTEGER PRIMARY KEY" +
                    ",display1 TEXT UNIQUE ON CONFLICT REPLACE" +
                    ",query TEXT" +
                    ",date LONG" +
                    ");");
            db.execSQL(builder.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS suggestions");
            onCreate(db);
        }
    }

    /**
     * In order to use this class, you must extend it, and call this setup function from your
     * constructor.  In your application or activities, you must provide the same values when
     * you create the {@link android.provider.SearchRecentSuggestions} helper.
     *
     * @param authority This must match the authority that you've declared in your manifest.
     * @param mode You can use mode flags here to determine certain functional aspects of your
     * database.  Note, this value should not change from run to run, because when it does change,
     * your suggestions database may be wiped.
     *
     * @see #DATABASE_MODE_QUERIES
     */
    protected void setupSuggestions(String authority, int mode) {
        if (TextUtils.isEmpty(authority) ||
                ((mode & DATABASE_MODE_QUERIES) == 0)) {
            throw new IllegalArgumentException();
        }

        // saved values
        mAuthority = new String(authority);
        mMode = mode;

        // derived values
        mSuggestionsUri = Uri.parse("content://" + mAuthority + "/suggestions");
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(mAuthority, SearchManager.SUGGEST_URI_PATH_QUERY, URI_MATCH_SUGGEST);

        // The URI of the icon that we will include on every suggestion here.
        final String historicalIcon = ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getContext().getPackageName() + "/" + R.drawable.ic_history_holo_light;

        mSuggestSuggestionClause = "display1 LIKE ?";
        mSuggestionProjection = new String [] {
                "_id",
                "display1 AS " + SearchManager.SUGGEST_COLUMN_TEXT_1,
                "query AS " + SearchManager.SUGGEST_COLUMN_QUERY,
                "'" + historicalIcon + "' AS " + SearchManager.SUGGEST_COLUMN_ICON_1
        };
    }

    /**
     * This method is provided for use by the ContentResolver.  Do not override, or directly
     * call from your own code.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        final int length = uri.getPathSegments().size();
        if (length != 1) {
            throw new IllegalArgumentException("Unknown Uri");
        }

        final String base = uri.getPathSegments().get(0);
        int count = 0;
        if (base.equals(sSuggestions)) {
            count = db.delete(sSuggestions, selection, selectionArgs);
        } else {
            throw new IllegalArgumentException("Unknown Uri");
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /**
     * This method is provided for use by the ContentResolver.  Do not override, or directly
     * call from your own code.
     */
    @Override
    public String getType(Uri uri) {
        if (mUriMatcher.match(uri) == URI_MATCH_SUGGEST) {
            return SearchManager.SUGGEST_MIME_TYPE;
        }
        int length = uri.getPathSegments().size();
        if (length >= 1) {
            String base = uri.getPathSegments().get(0);
            if (base.equals(sSuggestions)) {
                if (length == 1) {
                    return "vnd.android.cursor.dir/suggestion";
                } else if (length == 2) {
                    return "vnd.android.cursor.item/suggestion";
                }
            }
        }
        throw new IllegalArgumentException("Unknown Uri");
    }

    /**
     * This method is provided for use by the ContentResolver.  Do not override, or directly
     * call from your own code.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int length = uri.getPathSegments().size();
        if (length < 1) {
            throw new IllegalArgumentException("Unknown Uri");
        }
        // Note:  This table has on-conflict-replace semantics, so insert() may actually replace()
        long rowID = -1;
        String base = uri.getPathSegments().get(0);
        Uri newUri = null;
        if (base.equals(sSuggestions)) {
            if (length == 1) {
                rowID = db.insert(sSuggestions, NULL_COLUMN, values);
                if (rowID > 0) {
                    newUri = Uri.withAppendedPath(mSuggestionsUri, String.valueOf(rowID));
                }
            }
        }
        if (rowID < 0) {
            throw new IllegalArgumentException("Unknown Uri");
        }
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    /**
     * This method is provided for use by the ContentResolver.  Do not override, or directly
     * call from your own code.
     */
    @Override
    public boolean onCreate() {
        if (mAuthority == null || mMode == 0) {
            throw new IllegalArgumentException("Provider not configured");
        }
        int mWorkingDbVersion = DATABASE_VERSION + mMode;
        mOpenHelper = new DatabaseHelper(getContext(), mWorkingDbVersion);

        return true;
    }

    private ArrayList<String> mFullQueryTerms;

    /**
     *  Copy the projection, and change the query field alone.
     * @param selectionArgs
     * @return projection
     */
    private String[] createProjection(String[] selectionArgs) {
        String[] newProjection = new String[mSuggestionProjection.length];
        String queryAs;
        int fullSize = (mFullQueryTerms != null) ? mFullQueryTerms.size() : 0;
        if (fullSize > 0) {
            String realQuery = "'";
            for (int i = 0; i < fullSize; i++) {
                realQuery+= mFullQueryTerms.get(i);
                if (i < fullSize -1) {
                    realQuery += QUERY_TOKEN_SEPARATOR;
                }
            }
            queryAs = realQuery + " ' || query AS " + SearchManager.SUGGEST_COLUMN_QUERY;
        } else {
            queryAs = "query AS " + SearchManager.SUGGEST_COLUMN_QUERY;
        }
        for (int i = 0; i < mSuggestionProjection.length; i++) {
            newProjection[i] = mSuggestionProjection[i];
        }
        // Assumes that newProjection[length-2] is the query field.
        newProjection[mSuggestionProjection.length - 2] = queryAs;
        return newProjection;
    }

    /**
     * Set the other query terms to be included in the user's query.
     * These are in addition to what is being looked up for suggestions.
     * @param terms
     */
    public void setFullQueryTerms(ArrayList<String> terms) {
        mFullQueryTerms = terms;
    }

    /**
     * This method is provided for use by the ContentResolver. Do not override,
     * or directly call from your own code.
     */
    // TODO: Confirm no injection attacks here, or rewrite.
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // special case for actual suggestions (from search manager)
        String suggestSelection;
        String[] myArgs;
        if (TextUtils.isEmpty(selectionArgs[0])) {
            suggestSelection = null;
            myArgs = null;
        } else {
            String like = "%" + selectionArgs[0] + "%";
            myArgs = new String[] { like };
            suggestSelection = mSuggestSuggestionClause;
        }
        // Suggestions are always performed with the default sort order
        // Add this to the query:
        // "select 'real_query' as SearchManager.SUGGEST_COLUMN_QUERY.
        // rest of query
        // real query will then show up in the suggestion
        Cursor c = db.query(sSuggestions, createProjection(selectionArgs), suggestSelection, myArgs,
                null, null, ORDER_BY, null);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    /**
     * This method is provided for use by the ContentResolver.  Do not override, or directly
     * call from your own code.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not implemented");
    }
}