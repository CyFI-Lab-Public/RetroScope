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

import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * SuggestionExtras taking values from the extra columns in a suggestion cursor.
 */
public class CursorBackedSuggestionExtras extends AbstractSuggestionExtras {
    private static final String TAG = "QSB.CursorBackedSuggestionExtras";

    private static final HashSet<String> DEFAULT_COLUMNS = new HashSet<String>();
    static {
        DEFAULT_COLUMNS.addAll(Arrays.asList(SuggestionCursorBackedCursor.COLUMNS));
    }

    private final Cursor mCursor;
    private final int mCursorPosition;
    private final List<String> mExtraColumns;

    static CursorBackedSuggestionExtras createExtrasIfNecessary(Cursor cursor, int position) {
        List<String> extraColumns = getExtraColumns(cursor);
        if (extraColumns != null) {
            return new CursorBackedSuggestionExtras(cursor, position, extraColumns);
        } else {
            return null;
        }
    }

    static String[] getCursorColumns(Cursor cursor) {
        try {
            return cursor.getColumnNames();
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getColumnNames() failed, ", ex);
            return null;
        }
    }

    static boolean cursorContainsExtras(Cursor cursor) {
        String[] columns = getCursorColumns(cursor);
        for (String cursorColumn : columns) {
            if (!DEFAULT_COLUMNS.contains(cursorColumn)) {
                return true;
            }
        }
        return false;
    }

    static List<String> getExtraColumns(Cursor cursor) {
        String[] columns = getCursorColumns(cursor);
        if (columns == null) return null;
        List<String> extraColumns = null;
        for (String cursorColumn : columns) {
            if (!DEFAULT_COLUMNS.contains(cursorColumn)) {
                if (extraColumns == null) {
                    extraColumns = new ArrayList<String>();
                }
                extraColumns.add(cursorColumn);
            }
        }
        return extraColumns;
    }

    private CursorBackedSuggestionExtras(Cursor cursor, int position, List<String> extraColumns) {
        super(null);
        mCursor = cursor;
        mCursorPosition = position;
        mExtraColumns = extraColumns;
    }

    @Override
    public String doGetExtra(String columnName) {
        try {
            mCursor.moveToPosition(mCursorPosition);
            int columnIdx = mCursor.getColumnIndex(columnName);
            if (columnIdx < 0) return null;
            return mCursor.getString(columnIdx);
        } catch (RuntimeException ex) {
            // all operations on cross-process cursors can throw random exceptions
            Log.e(TAG, "getExtra(" + columnName + ") failed, ", ex);
            return null;
        }
    }

    @Override
    public List<String> doGetExtraColumnNames() {
        return mExtraColumns;
    }

}
