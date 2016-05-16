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

package com.android.gallery3d.filtershow.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FilterStackDBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "filterstacks.db";
    private static final String SQL_CREATE_TABLE = "CREATE TABLE ";

    public static interface FilterStack {
        /** The row uid */
        public static final String _ID = "_id";
        /** The table name */
        public static final String TABLE = "filterstack";
        /** The stack name */
        public static final String STACK_ID = "stack_id";
        /** A serialized stack of filters. */
        public static final String FILTER_STACK= "stack";
    }

    private static final String[][] CREATE_FILTER_STACK = {
            { FilterStack._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
            { FilterStack.STACK_ID, "TEXT" },
            { FilterStack.FILTER_STACK, "BLOB" },
    };

    public FilterStackDBHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public FilterStackDBHelper(Context context, String name) {
        this(context, name, DATABASE_VERSION);
    }

    public FilterStackDBHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, FilterStack.TABLE, CREATE_FILTER_STACK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTable(db, FilterStack.TABLE);
        onCreate(db);
    }

    protected static void createTable(SQLiteDatabase db, String table, String[][] columns) {
        StringBuilder create = new StringBuilder(SQL_CREATE_TABLE);
        create.append(table).append('(');
        boolean first = true;
        for (String[] column : columns) {
            if (!first) {
                create.append(',');
            }
            first = false;
            for (String val : column) {
                create.append(val).append(' ');
            }
        }
        create.append(')');
        db.beginTransaction();
        try {
            db.execSQL(create.toString());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    protected static void dropTable(SQLiteDatabase db, String table) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + table);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
