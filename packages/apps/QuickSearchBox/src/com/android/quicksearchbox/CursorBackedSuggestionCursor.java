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

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.util.Log;

public abstract class CursorBackedSuggestionCursor implements SuggestionCursor {

    private static final boolean DBG = false;
    protected static final String TAG = "QSB.CursorBackedSuggestionCursor";

    public static final String SUGGEST_COLUMN_LOG_TYPE = "suggest_log_type";

    private final String mUserQuery;

    /** The suggestions, or {@code null} if the suggestions query failed. */
    protected final Cursor mCursor;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_FORMAT} in @{link mCursor}. */
    private final int mFormatCol;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_TEXT_1} in @{link mCursor}. */
    private final int mText1Col;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_TEXT_2} in @{link mCursor}. */
    private final int mText2Col;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_TEXT_2_URL} in @{link mCursor}. */
    private final int mText2UrlCol;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_ICON_1} in @{link mCursor}. */
    private final int mIcon1Col;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_ICON_1} in @{link mCursor}. */
    private final int mIcon2Col;

    /** Column index of {@link SearchManager#SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING}
     * in @{link mCursor}.
     **/
    private final int mRefreshSpinnerCol;

    /** True if this result has been closed. */
    private boolean mClosed = false;

    public CursorBackedSuggestionCursor(String userQuery, Cursor cursor) {
        mUserQuery = userQuery;
        mCursor = cursor;
        mFormatCol = getColumnIndex(SearchManager.SUGGEST_COLUMN_FORMAT);
        mText1Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
        mText2Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2);
        mText2UrlCol = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_2_URL);
        mIcon1Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_1);
        mIcon2Col = getColumnIndex(SearchManager.SUGGEST_COLUMN_ICON_2);
        mRefreshSpinnerCol = getColumnIndex(SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING);
    }

    public String getUserQuery() {
        return mUserQuery;
    }

    public abstract Source getSuggestionSource();

    public String getSuggestionLogType() {
        return getStringOrNull(SUGGEST_COLUMN_LOG_TYPE);
    }

    public void close() {
        if (DBG) Log.d(TAG, "close()");
        if (mClosed) {
            throw new IllegalStateException("Double close()");
        }
        mClosed = true;
        if (mCursor != null) {
            try {
                mCursor.close();
            } catch (RuntimeException ex) {
                // all operations on cross-process cursors can throw random exceptions
                Log.e(TAG, "close() failed, ", ex);
            }
        }
    }

    @Override
    protected void finalize() {
        if (!mClosed) {
            Log.e(TAG, "LEAK! Finalized without being closed: " + toString());
        }
    }

    public int getCount() {
        if (mClosed) {
            throw new IllegalStateException("getCount() after close()");
        }
        if (mCursor == null) return 0;
        try {
            return mCursor.getCount();
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getCount() failed, ", ex);
            return 0;
        }
    }

    public void moveTo(int pos) {
        if (mClosed) {
            throw new IllegalStateException("moveTo(" + pos + ") after close()");
        }
        try {
            if (!mCursor.moveToPosition(pos)) {
                Log.e(TAG, "moveToPosition(" + pos + ") failed, count=" + getCount());
            }
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "moveToPosition() failed, ", ex);
        }
    }

    public boolean moveToNext() {
        if (mClosed) {
            throw new IllegalStateException("moveToNext() after close()");
        }
        try {
            return mCursor.moveToNext();
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "moveToNext() failed, ", ex);
            return false;
        }
    }

    public int getPosition() {
        if (mClosed) {
            throw new IllegalStateException("getPosition after close()");
        }
        try {
            return mCursor.getPosition();
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getPosition() failed, ", ex);
            return -1;
        }
    }

    public String getShortcutId() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
    }

    public String getSuggestionFormat() {
        return getStringOrNull(mFormatCol);
    }

    public String getSuggestionText1() {
        return getStringOrNull(mText1Col);
    }

    public String getSuggestionText2() {
        return getStringOrNull(mText2Col);
    }

    public String getSuggestionText2Url() {
        return getStringOrNull(mText2UrlCol);
    }

    public String getSuggestionIcon1() {
        return getStringOrNull(mIcon1Col);
    }

    public String getSuggestionIcon2() {
        return getStringOrNull(mIcon2Col);
    }

    public boolean isSpinnerWhileRefreshing() {
        return "true".equals(getStringOrNull(mRefreshSpinnerCol));
    }

    /**
     * Gets the intent action for the current suggestion.
     */
    public String getSuggestionIntentAction() {
        String action = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_ACTION);
        if (action != null) return action;
        return getSuggestionSource().getDefaultIntentAction();
    }

    public abstract ComponentName getSuggestionIntentComponent();

    /**
     * Gets the query for the current suggestion.
     */
    public String getSuggestionQuery() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_QUERY);
    }

    public String getSuggestionIntentDataString() {
         // use specific data if supplied, or default data if supplied
         String data = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_DATA);
         if (data == null) {
             data = getSuggestionSource().getDefaultIntentData();
         }
         // then, if an ID was provided, append it.
         if (data != null) {
             String id = getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
             if (id != null) {
                 data = data + "/" + Uri.encode(id);
             }
         }
         return data;
     }

    /**
     * Gets the intent extra data for the current suggestion.
     */
    public String getSuggestionIntentExtraData() {
        return getStringOrNull(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
    }

    public boolean isWebSearchSuggestion() {
        return Intent.ACTION_WEB_SEARCH.equals(getSuggestionIntentAction());
    }

    /**
     * Gets the index of a column in {@link #mCursor} by name.
     *
     * @return The index, or {@code -1} if the column was not found.
     */
    protected int getColumnIndex(String colName) {
        if (mCursor == null) return -1;
        try {
            return mCursor.getColumnIndex(colName);
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getColumnIndex() failed, ", ex);
            return -1;
        }
    }

    /**
     * Gets the string value of a column in {@link #mCursor} by column index.
     *
     * @param col Column index.
     * @return The string value, or {@code null}.
     */
    protected String getStringOrNull(int col) {
        if (mCursor == null) return null;
        if (col == -1) {
            return null;
        }
        try {
            return mCursor.getString(col);
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getString() failed, ", ex);
            return null;
        }
    }

    /**
     * Gets the string value of a column in {@link #mCursor} by column name.
     *
     * @param colName Column name.
     * @return The string value, or {@code null}.
     */
    protected String getStringOrNull(String colName) {
        int col = getColumnIndex(colName);
        return getStringOrNull(col);
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        // We don't watch Cursor-backed SuggestionCursors for changes
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        // We don't watch Cursor-backed SuggestionCursors for changes
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + mUserQuery + "]";
    }

}
