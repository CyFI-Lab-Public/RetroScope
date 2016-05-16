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

import android.os.Handler;
import android.util.Log;

import com.android.quicksearchbox.util.BatchingNamedTaskExecutor;
import com.android.quicksearchbox.util.Consumer;
import com.android.quicksearchbox.util.NamedTaskExecutor;
import com.android.quicksearchbox.util.NoOpConsumer;

/**
 * Suggestions provider implementation.
 *
 * The provider will only handle a single query at a time. If a new query comes
 * in, the old one is cancelled.
 */
public class SuggestionsProviderImpl implements SuggestionsProvider {

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsProviderImpl";

    private final Config mConfig;

    private final NamedTaskExecutor mQueryExecutor;

    private final Handler mPublishThread;

    private final Logger mLogger;

    public SuggestionsProviderImpl(Config config,
            NamedTaskExecutor queryExecutor,
            Handler publishThread,
            Logger logger) {
        mConfig = config;
        mQueryExecutor = queryExecutor;
        mPublishThread = publishThread;
        mLogger = logger;
    }

    @Override
    public void close() {
    }

    @Override
    public Suggestions getSuggestions(String query, Source sourceToQuery) {
        if (DBG) Log.d(TAG, "getSuggestions(" + query + ")");
        final Suggestions suggestions = new Suggestions(query, sourceToQuery);
        Log.i(TAG, "chars:" + query.length() + ",source:" + sourceToQuery);

        Consumer<SourceResult> receiver;
        if (shouldDisplayResults(query)) {
            receiver = new SuggestionCursorReceiver(suggestions);
        } else {
            receiver = new NoOpConsumer<SourceResult>();
            suggestions.done();
        }

        int maxResults = mConfig.getMaxResultsPerSource();
        QueryTask.startQuery(query, maxResults, sourceToQuery, mQueryExecutor,
                mPublishThread, receiver);

        return suggestions;
    }

    private boolean shouldDisplayResults(String query) {
        if (query.length() == 0 && !mConfig.showSuggestionsForZeroQuery()) {
            // Note that even though we don't display such results, it's
            // useful to run the query itself because it warms up the network
            // connection.
            return false;
        }
        return true;
    }


    private class SuggestionCursorReceiver implements Consumer<SourceResult> {
        private final Suggestions mSuggestions;

        public SuggestionCursorReceiver(Suggestions suggestions) {
            mSuggestions = suggestions;
        }

        @Override
        public boolean consume(SourceResult cursor) {
            if (DBG) {
                Log.d(TAG, "SuggestionCursorReceiver.consume(" + cursor + ") corpus=" +
                        cursor.getSource() + " count = " + cursor.getCount());
            }
            // publish immediately
            if (DBG) Log.d(TAG, "Publishing results");
            mSuggestions.addResults(cursor);
            if (cursor != null && mLogger != null) {
                mLogger.logLatency(cursor);
            }
            return true;
        }

    }
}
