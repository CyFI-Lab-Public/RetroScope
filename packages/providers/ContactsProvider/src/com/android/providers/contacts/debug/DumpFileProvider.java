/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.providers.contacts.debug;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Provider used to read dump files created by {@link DataExporter}.
 *
 * We send content: URI to sender apps (such as gmail).  This provider implement the URI.
 */
public class DumpFileProvider extends ContentProvider {
    public static final String AUTHORITY = "com.android.contacts.dumpfile";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Not needed.
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Not needed.
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Not needed.
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType(Uri uri) {
        return DataExporter.ZIP_MIME_TYPE;
    }

    /** @return the path part of a URI, without the beginning "/". */
    private static String extractFileName(Uri uri) {
        final String path = uri.getPath();
        return path.startsWith("/") ? path.substring(1) : path;
    }

    /** @return file content */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new UnsupportedOperationException();
        }

        final String fileName = extractFileName(uri);
        DataExporter.ensureValidFileName(fileName);
        final File file = DataExporter.getOutputFile(getContext(), fileName);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    /**
     * Used to provide {@link OpenableColumns#DISPLAY_NAME} and {@link OpenableColumns#SIZE}
     * for a URI.
     */
    @Override
    public Cursor query(Uri uri, String[] inProjection, String selection, String[] selectionArgs,
            String sortOrder) {
        final String fileName = extractFileName(uri);
        DataExporter.ensureValidFileName(fileName);

        final String[] projection = (inProjection != null) ? inProjection
                : new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};

        final MatrixCursor c = new MatrixCursor(projection);

        // Result will always have one row.
        final MatrixCursor.RowBuilder b = c.newRow();

        for (int i = 0; i < c.getColumnCount(); i++) {
            final String column = projection[i];
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                // Just return the requested path as the display name.  We don't care if the file
                // really exists.
                b.add(fileName);
            } else if (OpenableColumns.SIZE.equals(column)) {
                final File file = DataExporter.getOutputFile(getContext(), fileName);

                if (file.exists()) {
                    b.add(file.length());
                } else {
                    // File doesn't exist -- return null for "unknown".
                    b.add(null);
                }
            } else {
                throw new IllegalArgumentException("Unknown column " + column);
            }
        }

        return c;
    }

}
