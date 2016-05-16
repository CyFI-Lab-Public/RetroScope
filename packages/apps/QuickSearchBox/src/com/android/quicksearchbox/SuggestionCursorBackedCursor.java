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

import android.app.SearchManager;
import android.database.AbstractCursor;
import android.database.CursorIndexOutOfBoundsException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SuggestionCursorBackedCursor extends AbstractCursor {

    // This array also used in CursorBackedSuggestionExtras to avoid duplication.
    public static final String[] COLUMNS = {
        "_id",  // 0, This will contain the row number. CursorAdapter, used by SuggestionsAdapter,
                // used by SearchDialog, expects an _id column.
        SearchManager.SUGGEST_COLUMN_TEXT_1,  // 1
        SearchManager.SUGGEST_COLUMN_TEXT_2,  // 2
        SearchManager.SUGGEST_COLUMN_TEXT_2_URL,  // 3
        SearchManager.SUGGEST_COLUMN_ICON_1,  // 4
        SearchManager.SUGGEST_COLUMN_ICON_2,  // 5
        SearchManager.SUGGEST_COLUMN_INTENT_ACTION,  // 6
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,  // 7
        SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,  // 8
        SearchManager.SUGGEST_COLUMN_QUERY,  // 9
        SearchManager.SUGGEST_COLUMN_FORMAT,  // 10
        SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,  // 11
        SearchManager.SUGGEST_COLUMN_SPINNER_WHILE_REFRESHING,  // 12
    };

    private static final int COLUMN_INDEX_ID = 0;
    private static final int COLUMN_INDEX_TEXT1 = 1;
    private static final int COLUMN_INDEX_TEXT2 = 2;
    private static final int COLUMN_INDEX_TEXT2_URL = 3;
    private static final int COLUMN_INDEX_ICON1 = 4;
    private static final int COLUMN_INDEX_ICON2 = 5;
    private static final int COLUMN_INDEX_INTENT_ACTION = 6;
    private static final int COLUMN_INDEX_INTENT_DATA = 7;
    private static final int COLUMN_INDEX_INTENT_EXTRA_DATA = 8;
    private static final int COLUMN_INDEX_QUERY = 9;
    private static final int COLUMN_INDEX_FORMAT = 10;
    private static final int COLUMN_INDEX_SHORTCUT_ID = 11;
    private static final int COLUMN_INDEX_SPINNER_WHILE_REFRESHING = 12;

    private final SuggestionCursor mCursor;
    private ArrayList<String> mExtraColumns;

    public SuggestionCursorBackedCursor(SuggestionCursor cursor) {
        mCursor = cursor;
    }

    @Override
    public void close() {
        super.close();
        mCursor.close();
    }

    @Override
    public String[] getColumnNames() {
        Collection<String> extraColumns = mCursor.getExtraColumns();
        if (extraColumns != null) {
            ArrayList<String> allColumns = new ArrayList<String>(COLUMNS.length +
                    extraColumns.size());
            mExtraColumns = new ArrayList<String>(extraColumns);
            allColumns.addAll(Arrays.asList(COLUMNS));
            allColumns.addAll(mExtraColumns);
            return allColumns.toArray(new String[allColumns.size()]);
        } else {
            return COLUMNS;
        }
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    private Suggestion get() {
        mCursor.moveTo(getPosition());
        return mCursor;
    }

    private String getExtra(int columnIdx) {
        int extraColumn = columnIdx - COLUMNS.length;
        SuggestionExtras extras = get().getExtras();
        if (extras != null) {
            return extras.getExtra(mExtraColumns.get(extraColumn));
        } else {
            return null;
        }
    }

    @Override
    public int getInt(int column) {
        if (column == COLUMN_INDEX_ID) {
            return getPosition();
        } else {
            try {
                return Integer.valueOf(getString(column));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    @Override
    public String getString(int column) {
        if (column < COLUMNS.length) {
            switch (column) {
                case COLUMN_INDEX_ID:
                    return String.valueOf(getPosition());
                case COLUMN_INDEX_TEXT1:
                    return get().getSuggestionText1();
                case COLUMN_INDEX_TEXT2:
                    return get().getSuggestionText2();
                case COLUMN_INDEX_TEXT2_URL:
                    return get().getSuggestionText2Url();
                case COLUMN_INDEX_ICON1:
                    return get().getSuggestionIcon1();
                case COLUMN_INDEX_ICON2:
                    return get().getSuggestionIcon2();
                case COLUMN_INDEX_INTENT_ACTION:
                    return get().getSuggestionIntentAction();
                case COLUMN_INDEX_INTENT_DATA:
                    return get().getSuggestionIntentDataString();
                case COLUMN_INDEX_INTENT_EXTRA_DATA:
                    return get().getSuggestionIntentExtraData();
                case COLUMN_INDEX_QUERY:
                    return get().getSuggestionQuery();
                case COLUMN_INDEX_FORMAT:
                    return get().getSuggestionFormat();
                case COLUMN_INDEX_SHORTCUT_ID:
                    return get().getShortcutId();
                case COLUMN_INDEX_SPINNER_WHILE_REFRESHING:
                    return String.valueOf(get().isSpinnerWhileRefreshing());
                default:
                    throw new CursorIndexOutOfBoundsException("Requested column " + column
                            + " of " + COLUMNS.length);
            }
        } else {
            return getExtra(column);
        }
    }

    @Override
    public long getLong(int column) {
        try {
            return Long.valueOf(getString(column));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public boolean isNull(int column) {
        return getString(column) == null;
    }

    @Override
    public short getShort(int column) {
        try {
            return Short.valueOf(getString(column));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public double getDouble(int column) {
        try {
            return Double.valueOf(getString(column));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public float getFloat(int column) {
        try {
            return Float.valueOf(getString(column));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
