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

package com.android.quicksearchbox.tests.naughty;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.util.List;

public class CrashingSuggestionProvider extends NaughtySuggestionProvider {

    private static final String TAG = CrashingSuggestionProvider.class.getSimpleName();

    private static final String[] COLUMNS = {
        "_id",
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_ICON_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_ICON_2,
        SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query(" + uri + ")");

        List<String> path = uri.getPathSegments();
        String query = null;
        if (path.size() > 0) {
            query = path.get(path.size()-1);
        }

        if ("crash".equals(query)) {
            beNaughty();
            return null;
        } else if ("exit".equals(query)) {
            System.exit(0);
            return null;
        } else {
            MatrixCursor cursor = new MatrixCursor(COLUMNS);
            if ("icon".equals(query)) {

                for (int i = 0; i < 100; i++) {
                    cursor.addRow(new Object[]{
                        i,
                        "Naughty suggestion " + i,
                        "content://com.android.quicksearchbox.tests.naughty.crashing/icon/" + i,
                        "Crashing icon 1",
                        null,
                        Intent.ACTION_VIEW,
                        "content://com.android.quicksearchbox.naughty.crashing/" + i
                    });
                }
            } else if ("icon2".equals(query)) {

                for (int i = 0; i < 100; i++) {
                    cursor.addRow(new Object[]{
                        i,
                        "Naughty suggestion " + i,
                        R.drawable.crash,
                        "Crashing icon 2",
                        "content://com.android.quicksearchbox.tests.naughty.crashing/icon2/" + i,
                        Intent.ACTION_VIEW,
                        "content://com.android.quicksearchbox.naughty.crashing/" + i
                    });
                }
            } else {
                cursor.addRow(new Object[]{
                    0,
                    "Enter 'crash', 'exit', 'icon' or 'icon2'",
                    R.drawable.crash,
                    "To make me crash",
                    null,
                    Intent.ACTION_VIEW,
                    "content://com.android.quicksearchbox.naughty.crashing/0"
                });
            }
            return cursor;
        }
    }

    @Override
    protected void beNaughty() {
        throw new NaughtyException();
    }

    private static class NaughtyException extends RuntimeException {
    }

}
