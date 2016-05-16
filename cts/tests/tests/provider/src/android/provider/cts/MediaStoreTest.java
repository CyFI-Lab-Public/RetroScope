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

package android.provider.cts;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.test.InstrumentationTestCase;

public class MediaStoreTest extends InstrumentationTestCase {
    private static final String TEST_VOLUME_NAME = "volume_for_cts";

    private static final String[] PROJECTION = new String[] { MediaStore.MEDIA_SCANNER_VOLUME };

    private Uri mScannerUri;

    private String mVolumnBackup;

    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        mScannerUri = MediaStore.getMediaScannerUri();
        mContentResolver = getInstrumentation().getContext().getContentResolver();
        Cursor c = mContentResolver.query(mScannerUri, PROJECTION, null, null, null);
        if (c != null) {
            c.moveToFirst();
            mVolumnBackup = c.getString(0);
            c.close();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        // restore initial values
        if (mVolumnBackup != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MEDIA_SCANNER_VOLUME, mVolumnBackup);
            mContentResolver.insert(mScannerUri, values);
        }
        super.tearDown();
    }


    public void testGetMediaScannerUri() {
        ContentValues values = new ContentValues();
        String selection = MediaStore.MEDIA_SCANNER_VOLUME + "=?";
        String[] selectionArgs = new String[] { TEST_VOLUME_NAME };

        // assert there is no item with name TEST_VOLUME_NAME
        assertNull(mContentResolver.query(mScannerUri, PROJECTION,
                selection, selectionArgs, null));

        // insert
        values.put(MediaStore.MEDIA_SCANNER_VOLUME, TEST_VOLUME_NAME);
        assertEquals(MediaStore.getMediaScannerUri(),
                mContentResolver.insert(mScannerUri, values));

        // query
        Cursor c = mContentResolver.query(mScannerUri, PROJECTION,
                selection, selectionArgs, null);
        assertEquals(1, c.getCount());
        c.moveToFirst();
        assertEquals(TEST_VOLUME_NAME, c.getString(0));
        c.close();

        // delete
        assertEquals(1, mContentResolver.delete(mScannerUri, null, null));
        assertNull(mContentResolver.query(mScannerUri, PROJECTION, null, null, null));
    }

    public void testGetVersion() {
        // Could be a version string or null...just check it doesn't blow up.
        MediaStore.getVersion(getInstrumentation().getTargetContext());
    }
}
