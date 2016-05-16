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

package com.android.quicksearchbox.util;

import android.database.sqlite.SQLiteDatabase;

/**
 * Abstract helper base class for asynchronous SQLite queries.
 *
 * @param <A> The type of the result of the query.
 */
public abstract class SQLiteAsyncQuery<A> {

    /**
     * Performs a query and computes some value from the result
     *
     * @param db A readable database.
     * @return The result of the query.
     */
    protected abstract A performQuery(SQLiteDatabase db);

    /**
     * Runs the query against the database and passes the result to the consumer.
     */
    public void run(SQLiteDatabase db, Consumer<A> consumer) {
        A result = performQuery(db);
        consumer.consume(result);
    }
}
