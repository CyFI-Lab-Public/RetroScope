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

package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import junit.framework.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestUtils {
    private TestUtils() {
    }

    /** Convenient method to create a ContentValues */
    public static ContentValues cv(Object... namesAndValues) {
        Assert.assertTrue((namesAndValues.length % 2) == 0);

        final ContentValues ret = new ContentValues();
        for (int i = 1; i < namesAndValues.length; i += 2) {
            final String name = namesAndValues[i - 1].toString();
            final Object value =  namesAndValues[i];
            if (value == null) {
                ret.putNull(name);
            } else if (value instanceof String) {
                ret.put(name, (String) value);
            } else if (value instanceof Integer) {
                ret.put(name, (Integer) value);
            } else if (value instanceof Long) {
                ret.put(name, (Long) value);
            } else {
                Assert.fail("Unsupported type: " + value.getClass().getSimpleName());
            }
        }
        return ret;
    }

    /**
     * Writes the content of a cursor to the log.
     */
    public static final void dumpCursor(Cursor c) {
        final String TAG = "contacts";

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            if (sb.length() > 0) sb.append("|");
            sb.append(c.getColumnName(i));
        }
        Log.i(TAG, sb.toString());

        c.moveToPosition(-1);
        while (c.moveToNext()) {
            sb.setLength(0);
            for (int i = 0; i < c.getColumnCount(); i++) {
                if (sb.length() > 0) sb.append("|");

                if (c.getType(i) == Cursor.FIELD_TYPE_BLOB) {
                    byte[] blob = c.getBlob(i);
                    sb.append("([blob] ");
                    sb.append(blob == null ? "null" : blob.length + "b");
                    sb.append(")");
                } else {
                    sb.append(c.getString(i));
                }
            }
            Log.i(TAG, sb.toString());
        }
    }

    /**
     * Writes an arbitrary byte array to the test apk's cache directory.
     */
    public static final String dumpToCacheDir(Context context, String prefix, String suffix,
            byte[] data) {
        try {
            File file = File.createTempFile(prefix, suffix, context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            return "[Failed to write to file: " + e.getMessage() + "]";
        }
    }
}
