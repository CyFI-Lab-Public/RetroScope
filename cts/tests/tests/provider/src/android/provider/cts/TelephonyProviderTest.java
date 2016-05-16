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

package android.provider.cts;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Telephony.Carriers;
import android.test.InstrumentationTestCase;

import java.lang.reflect.Field;

import java.io.FileDescriptor;

// To run the tests in this file w/o running all the cts tests:
// make cts
// cts-tradefed
// run cts -c android.provider.cts.TelephonyProviderTest

public class TelephonyProviderTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;
    private static final String[] APN_PROJECTION = {
        Carriers.TYPE,            // 0
        Carriers.MMSC,            // 1
        Carriers.MMSPROXY,        // 2
        Carriers.MMSPORT          // 3
    };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getInstrumentation().getContext().getContentResolver();
    }

    // Test that the TelephonyProvider doesn't allow clients to update _data column data and
    // if they can, that they can't abuse the provider to open an arbitrary file.
    public void testOpeningAnyFile() {
        Uri uri = Uri.parse("content://mms/100/part");
        try {
            ContentValues values2 = new ContentValues();
            values2.put("_data", "/dev/urandom");
            Uri uri2 = mContentResolver.insert(uri, values2);
            assertEquals("The code was able to insert the _data column", null, uri2);
            if (uri2 == null) {
                return;
            }
            ContentValues values = new ContentValues();
            values.put("_data", "/dev/urandom");
            int rowCnt = mContentResolver.update(uri2, values, null, null);
            assertEquals("Was able to update the _data column", 0, rowCnt);

            ParcelFileDescriptor pfd = mContentResolver.openFileDescriptor(uri2, "rw");
            pfd.getFileDescriptor();
            FileDescriptor fd = pfd.getFileDescriptor();
            Field fld = fd.getClass().getDeclaredField("descriptor");
            fld.setAccessible(true);
            int fint  = fld.getInt(fd);
            fail("The code was able to abuse the MmsProvider to open any file");
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    // In JB MR1 access to the TelephonyProvider's Carriers table was clamped down and would
    // throw a SecurityException when queried. That was fixed in JB MR2. Verify that 3rd parties
    // can access the APN info the carriers table, after JB MR1.
    public void testAccessToApns() {
        try {
            String selection = Carriers.CURRENT + " IS NOT NULL";
            String[] selectionArgs = null;
            Cursor cursor = mContentResolver.query(Carriers.CONTENT_URI,
                    APN_PROJECTION, selection, selectionArgs, null);
        } catch (SecurityException e) {
            fail("No access to current APN");
        }
    }
}
