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

import com.android.quicksearchbox.util.QuietlyCloseable;

import android.database.DataSetObserver;

import java.util.Collection;

/**
 * A sequence of suggestions, with a current position.
 */
public interface SuggestionCursor extends Suggestion, QuietlyCloseable {

    /**
     * Gets the query that the user typed to get this suggestion.
     */
    String getUserQuery();

    /**
     * Gets the number of suggestions in this result.
     *
     * @return The number of suggestions, or {@code 0} if this result represents a failed query.
     */
    int getCount();

    /**
     * Moves to a given suggestion.
     *
     * @param pos The position to move to.
     * @throws IndexOutOfBoundsException if {@code pos < 0} or {@code pos >= getCount()}.
     */
    void moveTo(int pos);

    /**
     * Moves to the next suggestion, if there is one.
     *
     * @return {@code false} if there is no next suggestion.
     */
    boolean moveToNext();

    /**
     * Gets the current position within the cursor.
     */
    int getPosition();

    /**
     * Frees any resources used by this cursor.
     */
    @Override
    void close();

    /**
     * Register an observer that is called when changes happen to this data set.
     *
     * @param observer gets notified when the data set changes.
     */
    void registerDataSetObserver(DataSetObserver observer);

    /**
     * Unregister an observer that has previously been registered with 
     * {@link #registerDataSetObserver(DataSetObserver)}
     *
     * @param observer the observer to unregister.
     */
    void unregisterDataSetObserver(DataSetObserver observer);

    /**
     * Return the extra columns present in this cursor, or null if none exist.
     */
    Collection<String> getExtraColumns();
}
