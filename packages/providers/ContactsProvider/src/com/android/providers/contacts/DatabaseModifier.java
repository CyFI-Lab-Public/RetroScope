/*
 * Copyright (C) 2011 The Android Open Source Project

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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

/**
 * An interface which wraps key database modify operations (insert, update, delete) to perform
 * additional tasks before or after the database modification operation. A typical use case is to
 * generate notification when a database is modified.
 */
public interface DatabaseModifier {
    /**
     * Use this method to insert a value which you would otherwise do using the
     * {@link SQLiteDatabase#insert(String, String, ContentValues)} method.
     */
    public abstract long insert(String table, String nullColumnHack, ContentValues values);
    /**
     * Use this method to insert a value which you would otherwise do using the
     * {@link DatabaseUtils.InsertHelper#insert(ContentValues)} method.
     */
    public abstract long insert(ContentValues values);
    /**
     * Use this method to update a table which you would otherwise do using the
     * {@link SQLiteDatabase#update(String, ContentValues, String, String[])} method.
     */
    public abstract int update(String table, ContentValues values,
            String whereClause, String[] whereArgs);
    /**
     * Use this method to delete entries from a table which you would otherwise do using the
     * {@link SQLiteDatabase#delete(String, String, String[])} method.
     */
    public abstract int delete(String table, String whereClause, String[] whereArgs);
}
