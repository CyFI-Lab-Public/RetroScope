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
package com.android.quicksearchbox.tests;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * A content provider that crashes when something is requested.
 */
public class CrashingIconProvider extends ContentProvider {

    private static final String TAG = "QSB." + CrashingIconProvider.class.getSimpleName();
    private static final boolean DBG = false;

    public static final String AUTHORITY = "com.android.quicksearchbox.tests.iconcrash";

    @Override
    public boolean onCreate() {
        Log.i(TAG, "onCreate");
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        if (DBG) Log.d(TAG, "openFile(" + uri + ", " + mode + ")");
        throw new CrashException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (DBG) Log.d(TAG, "delete(" + uri + ", " + selection + ", " + selectionArgs + ")");
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        if (DBG) Log.d(TAG, "getType(" + uri + ")");
        return "image/png";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (DBG) Log.d(TAG, "insert(" + uri + ", " + values + ")");
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (DBG) Log.d(TAG, "query(" + uri + ")");
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (DBG) Log.d(TAG, "update(" + uri + ")");
        throw new UnsupportedOperationException();
    }

    private static class CrashException extends RuntimeException {
    }

}
