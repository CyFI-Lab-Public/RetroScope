/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.mail.utils;

import android.database.MatrixCursor;

import java.util.HashMap;
import java.util.Map;

public class MatrixCursorWithCachedColumns extends MatrixCursor {
    /** A mapping of column names to column indices, to speed up lookups */
    private final Map<String, Integer> mColumnNameMap;

    public MatrixCursorWithCachedColumns(final String[] columnNames, final int initialCapacity) {
        super(columnNames, initialCapacity);

        final int columnCount = columnNames.length;
        mColumnNameMap = new HashMap<String, Integer>(columnCount, 1);
        for (int i = 0; i < columnCount; i++) {
            mColumnNameMap.put(columnNames[i], i);
        }
    }

    public MatrixCursorWithCachedColumns(final String[] columnNames) {
        this(columnNames, 16);
    }

    @Override
    public int getColumnIndex(String columnName) {
        final Integer i = mColumnNameMap.get(columnName);
        if (i != null) {
            return i.intValue();
        }

        return -1;
    }
}
