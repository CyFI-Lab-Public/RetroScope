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

/**
 * Provides a set of suggestion results for a query..
 *
 */
public interface SuggestionsProvider {

    /**
     * Gets suggestions for a query.
     *
     * @param query The query.
     * @param source The source to query. Must be non-null.
     */
    Suggestions getSuggestions(String query, Source source);

    void close();
}
