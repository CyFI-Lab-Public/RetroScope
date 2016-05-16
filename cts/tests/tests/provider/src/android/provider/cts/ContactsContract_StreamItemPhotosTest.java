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

package android.provider.cts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.ContactsContract.StreamItemPhotos;
import android.provider.ContactsContract.StreamItems;
import android.test.AndroidTestCase;

public class ContactsContract_StreamItemPhotosTest extends AndroidTestCase {

    private ContentResolver mResolver;

    private Uri mStreamItemUri;

    private long mStreamItemId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();

        long rawContactId = ContactsContract_StreamItemsTest.insertRawContact(mResolver);
        mStreamItemUri = ContactsContract_StreamItemsTest.insertViaContentDirectoryUri(mResolver,
                rawContactId);
        mStreamItemId = ContentUris.parseId(mStreamItemUri);
        assertTrue(mStreamItemId != -1);
    }

    public void testContentDirectoryUri() {
        byte[] photoData = PhotoUtil.getTestPhotoData(mContext);
        ContentValues values = new ContentValues();
        values.put(StreamItemPhotos.SORT_INDEX, 1);
        values.put(StreamItemPhotos.PHOTO, photoData);

        Uri insertUri = Uri.withAppendedPath(
                ContentUris.withAppendedId(StreamItems.CONTENT_URI, mStreamItemId),
                StreamItems.StreamItemPhotos.CONTENT_DIRECTORY);
        Uri uri = mResolver.insert(insertUri, values);
        long photoId = ContentUris.parseId(uri);
        assertTrue(photoId != -1);
        assertEquals(Uri.withAppendedPath(insertUri, Long.toString(photoId)), uri);
    }

    public void testContentPhotoUri() {
        byte[] photoData = PhotoUtil.getTestPhotoData(mContext);
        ContentValues values = new ContentValues();
        values.put(StreamItemPhotos.STREAM_ITEM_ID, mStreamItemId);
        values.put(StreamItemPhotos.SORT_INDEX, 1);
        values.put(StreamItemPhotos.PHOTO, photoData);

        Uri uri = mResolver.insert(StreamItems.CONTENT_PHOTO_URI, values);
        long photoId = ContentUris.parseId(uri);
        assertTrue(photoId != -1);
        assertEquals(Uri.withAppendedPath(StreamItems.CONTENT_PHOTO_URI,
                Long.toString(photoId)), uri);
    }
}
