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

import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.Consumers;
import com.android.quicksearchbox.util.NamedTask;
import com.android.quicksearchbox.util.NamedTaskExecutor;

import android.os.Handler;
import android.util.Log;

/**
 * A task that gets suggestions from a corpus.
 */
public class QueryTask<C extends SuggestionCursor> implements NamedTask {
    private static final String TAG = "QSB.QueryTask";
    private static final boolean DBG = false;

    private final String mQuery;
    private final int mQueryLimit;
    private final SuggestionCursorProvider<C> mProvider;
    private final Handler mHandler;
    private final Consumer<C> mConsumer;

    /**
     * Creates a new query task.
     *
     * @param query Query to run.
     * @param queryLimit The number of suggestions to ask each provider for.
     * @param provider The provider to ask for suggestions.
     * @param handler Handler that {@link Consumer#consume} will
     *        get called on. If null, the method is called on the query thread.
     * @param consumer Consumer to notify when the suggestions have been returned.
     */
    public QueryTask(String query, int queryLimit, SuggestionCursorProvider<C> provider,
            Handler handler, Consumer<C> consumer) {
        mQuery = query;
        mQueryLimit = queryLimit;
        mProvider = provider;
        mHandler = handler;
        mConsumer = consumer;
    }

    @Override
    public String getName() {
        return mProvider.getName();
    }

    @Override
    public void run() {
        final C cursor = mProvider.getSuggestions(mQuery, mQueryLimit);
        if (DBG) Log.d(TAG, "Suggestions from " + mProvider + " = " + cursor);
        Consumers.consumeCloseableAsync(mHandler, mConsumer, cursor);
    }

    @Override
    public String toString() {
        return mProvider + "[" + mQuery + "]";
    }

    public static <C extends SuggestionCursor> void startQuery(String query,
            int maxResults,
            SuggestionCursorProvider<C> provider,
            NamedTaskExecutor executor, Handler handler,
            Consumer<C> consumer) {

        QueryTask<C> task = new QueryTask<C>(query, maxResults, provider, handler,
                consumer);
        executor.execute(task);
    }
}
