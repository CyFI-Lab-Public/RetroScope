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

import android.database.AbstractCursor;

import java.util.ArrayList;

/**
 * Mock cursor providing suggestions data.
 */
public class MockSuggestionProviderCursor extends AbstractCursor {

    private final String[] mColumns;
    private final ArrayList<Row> mRows;

    public MockSuggestionProviderCursor(String[] columns) {
        mColumns = columns;
        mRows = new ArrayList<Row>();
    }

    public Row addRow(Object... values) {
        Row row = new Row(values);
        mRows.add(row);
        return row;
    }

    public Object get(int column) {
        return mRows.get(getPosition()).get(column);
    }

    @Override
    public String[] getColumnNames() {
        return mColumns;
    }

    @Override
    public int getCount() {
        return mRows.size();
    }

    @Override
    public double getDouble(int column) {
        return mRows.get(getPosition()).getDouble(column);
    }

    @Override
    public float getFloat(int column) {
        return mRows.get(getPosition()).getFloat(column);
    }

    @Override
    public int getInt(int column) {
        return mRows.get(getPosition()).getInt(column);
    }

    @Override
    public long getLong(int column) {
        return mRows.get(getPosition()).getLong(column);
    }

    @Override
    public short getShort(int column) {
        return mRows.get(getPosition()).getShort(column);
    }

    @Override
    public String getString(int column) {
        return mRows.get(getPosition()).getString(column);
    }

    @Override
    public boolean isNull(int column) {
        return mRows.get(getPosition()).isNull(column);
    }

    private class Row {
        private final Object[] mValues;
        public Row(Object... values) {
            if (values.length > mColumns.length) {
                throw new IllegalArgumentException("Too many columns");
            }
            mValues = new Object[mColumns.length];
            for (int i = 0; i < values.length; ++i) {
                setColumn(i, values[i]);
            }
        }

        public Row setColumn(int column, Object value) {
            mValues[column] = value;
            return this;
        }

        public Object get(int column) {
            return mValues[column];
        }

        public double getDouble(int column) {
            Object o = mValues[column];
            if (o == null) return 0d;
            if (o instanceof Double) return (Double) o;
            return Double.valueOf(o.toString());
        }

        public float getFloat(int column) {
            Object o = mValues[column];
            if (o == null) return 0f;
            if (o instanceof Float) return (Float) o;
            return Float.valueOf(o.toString());
        }

        public int getInt(int column) {
            Object o = mValues[column];
            if (o == null) return 0;
            return Integer.valueOf(o.toString());
        }

        public long getLong(int column) {
            Object o = mValues[column];
            if (o == null) return 0;
            return Long.valueOf(o.toString());
        }

        public short getShort(int column) {
            Object o = mValues[column];
            if (o == null) return 0;
            return Short.valueOf(o.toString());
        }

        public String getString(int column) {
            Object o = mValues[column];
            if (o == null) return null;
            if (o instanceof String) return (String) o;
            return o.toString();
        }

        public boolean isNull(int column) {
            return (mValues[column] == null);
        }

    }

}
