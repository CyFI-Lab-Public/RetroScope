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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

/**
 * Common implementation for sources that load images from a cursor.
 */
public abstract class CursorPhotoSource extends PhotoSource {

    // An invalid cursor position to represent the uninitialized state.
    protected static final int UNINITIALIZED = -1;
    // An invalid cursor position to represent the error state.
    protected static final int INVALID = -2;

    public CursorPhotoSource(Context context, SharedPreferences settings) {
        super(context, settings);
    }

    public CursorPhotoSource(Context context, SharedPreferences settings, PhotoSource fallback) {
      super(context, settings, fallback);
    }

    @Override
    protected ImageData naturalNext(ImageData current) {
        if (current.cursor == null || current.cursor.isClosed()) {
            openCursor(current);
        }
        findPosition(current);
        current.cursor.moveToPosition(current.position);
        current.cursor.moveToNext();
        ImageData data = null;
        if (!current.cursor.isAfterLast()) {
            data = unpackImageData(current.cursor, null);
            data.cursor = current.cursor;
            data.position = current.cursor.getPosition();
        }
        return data;
    }

    @Override
    protected ImageData naturalPrevious(ImageData current) {
        if (current.cursor == null || current.cursor.isClosed()) {
            openCursor(current);
        }
        findPosition(current);
        current.cursor.moveToPosition(current.position);
        current.cursor.moveToPrevious();
        ImageData data = null;
        if (!current.cursor.isBeforeFirst()) {
            data = unpackImageData(current.cursor, null);
            data.cursor = current.cursor;
            data.position = current.cursor.getPosition();
        }
        return data;
    }

    @Override
    protected void donePaging(ImageData current) {
        if (current.cursor != null && !current.cursor.isClosed()) {
            current.cursor.close();
        }
    }

    protected abstract void openCursor(ImageData data);
    protected abstract void findPosition(ImageData data);
    protected abstract ImageData unpackImageData(Cursor cursor, ImageData data);
}

