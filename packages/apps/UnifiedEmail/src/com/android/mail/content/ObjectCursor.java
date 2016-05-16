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

package com.android.mail.content;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.util.SparseArray;

/**
 * A cursor-backed type that can return an object for each row of the cursor. This class is most
 * useful when:
 * 1. The cursor is returned in conjuction with an AsyncTaskLoader and created off the UI thread.
 * 2. A single row in the cursor specifies everything for an object.
 */
public class ObjectCursor <T> extends CursorWrapper {
    /** The cache for objects in the underlying cursor. */
    private final SparseArray<T> mCache;
    /** An object that knows how to construct {@link T} objects using cursors. */
    private final CursorCreator<T> mFactory;

    /**
     * Creates a new object cursor.
     * @param cursor the underlying cursor this wraps.
     */
    public ObjectCursor(Cursor cursor, CursorCreator<T> factory) {
        super(cursor);
        if (cursor != null) {
            mCache = new SparseArray<T>(cursor.getCount());
        } else {
            mCache = null;
        }
        mFactory = factory;
    }

    /**
     * Create a concrete object at the current cursor position. There is no guarantee on object
     * creation: an object might have been previously created, or the cache might be populated
     * by calling {@link #fillCache()}. In both these cases, the previously created object is
     * returned.
     * @return a model
     */
    public final T getModel() {
        final Cursor c = getWrappedCursor();
        if (c == null ) {
            return null;
        }
        final int currentPosition = c.getPosition();
        // The cache contains this object, return it.
        final T prev = mCache.get(currentPosition);
        if (prev != null) {
            return prev;
        }
        // Get the object at the current position and add it to the cache.
        final T model = mFactory.createFromCursor(c);
        mCache.put(currentPosition, model);
        return model;
    }

    /**
     * Reads the entire cursor to populate the objects in the cache. Subsequent calls to {@link
     * #getModel()} will return the cached objects as far as the underlying cursor does not change.
     */
    final void fillCache() {
        final Cursor c = getWrappedCursor();
        if (c == null || !c.moveToFirst()) {
            return;
        }
        do {
            // As a side effect of getModel, the model is cached away.
            getModel();
        } while (c.moveToNext());
    }

    @Override
    public void close() {
        super.close();
        mCache.clear();
    }

}
