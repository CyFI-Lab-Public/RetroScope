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
 * Interface for objects that can produce a SuggestionCursor given a query.
 */
public interface SuggestionCursorProvider<C extends SuggestionCursor> {

    /**
     * Gets the name of the provider. This is used for logging and
     * to execute tasks on the queue for the provider.
     */
    String getName();

    /**
     * Gets suggestions from the provider.
     *
     * @param query The user query.
     * @param queryLimit An advisory maximum number of results that the source should return.
     * @return The suggestion results. Must not be {@code null}.
     */
    C getSuggestions(String query, int queryLimit);
}
