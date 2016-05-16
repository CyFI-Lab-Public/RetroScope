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

import android.database.DataSetObserver;

import java.util.Collection;

/**
 * A suggestion cursor that delegates all methods to another SuggestionCursor.
 */
public class SuggestionCursorWrapper extends AbstractSuggestionCursorWrapper {

    private final SuggestionCursor mCursor;

    public SuggestionCursorWrapper(String userQuery, SuggestionCursor cursor) {
        super(userQuery);
        mCursor = cursor;
    }

    public void close() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor == null ? 0 : mCursor.getCount();
    }

    public int getPosition() {
        return mCursor == null ? 0 : mCursor.getPosition();
    }

    public void moveTo(int pos) {
        if (mCursor != null) {
            mCursor.moveTo(pos);
        }
    }

    public boolean moveToNext() {
        if (mCursor != null) {
            return mCursor.moveToNext();
        } else {
            return false;
        }
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        if (mCursor != null) {
            mCursor.registerDataSetObserver(observer);
        }
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (mCursor != null) {
            mCursor.unregisterDataSetObserver(observer);
        }
    }

    @Override
    protected SuggestionCursor current() {
        return mCursor;
    }

    public Collection<String> getExtraColumns() {
        return mCursor.getExtraColumns();
    }

}
