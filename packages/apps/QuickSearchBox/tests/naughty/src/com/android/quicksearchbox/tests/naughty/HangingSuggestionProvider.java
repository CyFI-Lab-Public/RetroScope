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

public class HangingSuggestionProvider extends NaughtySuggestionProvider {

    private static final String TAG = HangingSuggestionProvider.class.getSimpleName();

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
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query(" + uri + ")");

        List<String> path = uri.getPathSegments();
        String query = null;
        if (path.size() > 0) {
            query = path.get(path.size()-1);
        }

        if ("hang".equals(query)) {
            beNaughty();
            return null;
        } else {
            MatrixCursor cursor = new MatrixCursor(COLUMNS);
            if ("icon".equals(query)) {

                for (int i = 0; i < 100; i++) {
                    cursor.addRow(new Object[]{
                        i,
                        "Naughty suggestion " + i,
                        "content://com.android.quicksearchbox.tests.naughty.hanging/naughty/icon/" + i,
                        "Hanging icon 1",
                        null,
                        Intent.ACTION_VIEW,
                        "content://com.android.quicksearchbox.tests.naughty.hanging/naughty/" + i
                    });
                }
            } else if ("icon2".equals(query)) {

                for (int i = 0; i < 100; i++) {
                    cursor.addRow(new Object[]{
                        i,
                        "Naughty suggestion " + i,
                        R.drawable.hang,
                        "Hanging icon 2",
                        "content://com.android.quicksearchbox.tests.naughty.hanging/icon2/" + i,
                        Intent.ACTION_VIEW,
                        "content://com.android.quicksearchbox.naughty.crashing/" + i
                    });
                }
            } else {
                cursor.addRow(new Object[]{
                    0,
                    "Enter 'hang' or 'icon'",
                    R.drawable.hang,
                    "To make me hang",
                    null,
                    Intent.ACTION_VIEW,
                    "content://com.android.quicksearchbox.tests.naughty.hanging/naughty/0"
                });
            }
            return cursor;
        }
    }

    @Override
    protected void beNaughty() {
        try {
            Object o = new Object();
            synchronized(o) {
                o.wait();
            }
        } catch (InterruptedException e) {
        }
    }

}
