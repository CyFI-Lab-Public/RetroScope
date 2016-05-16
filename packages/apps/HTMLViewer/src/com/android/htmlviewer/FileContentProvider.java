/**
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.htmlviewer;

import java.io.FileNotFoundException;
import java.io.File;
import java.lang.UnsupportedOperationException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;

/**
 * WebView does not support file: loading. This class wraps a file load
 * with a content provider. 
 * As HTMLViewer does not have internet access nor does it allow
 * Javascript to be run, it is safe to load file based HTML content.
*/
public class FileContentProvider extends ContentProvider {
    
    public static final String BASE_URI = 
            "content://com.android.htmlfileprovider";

    @Override
    public String getType(Uri uri) {
        // If the mimetype is not appended to the uri, then return an empty string
        String mimetype = uri.getQuery();
        return mimetype == null ? "" : mimetype;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        // android:exported="false" is broken in older releases so we have to
        // manually enforce the calling identity.
        if (Process.myUid() != Binder.getCallingUid()) {
            throw new SecurityException("Permission denied");
        }
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Bad mode for " + uri + ": " + mode);
        }
        String filename = uri.getPath();
        return ParcelFileDescriptor.open(new File(filename),
            ParcelFileDescriptor.MODE_READ_ONLY);
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}
