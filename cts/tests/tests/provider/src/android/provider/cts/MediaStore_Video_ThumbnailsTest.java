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

import com.android.cts.stub.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.MediaStore.Video.VideoColumns;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.IOException;

public class MediaStore_Video_ThumbnailsTest extends AndroidTestCase {

    private ContentResolver mResolver;

    private FileCopyHelper mFileHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResolver = mContext.getContentResolver();
        mFileHelper = new FileCopyHelper(mContext);
    }

    @Override
    protected void tearDown() throws Exception {
        mFileHelper.clear();
        super.tearDown();
    }

    public void testGetContentUri() {
        Uri internalUri = Thumbnails.getContentUri(MediaStoreAudioTestHelper.INTERNAL_VOLUME_NAME);
        Uri externalUri = Thumbnails.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME);
        assertEquals(Thumbnails.INTERNAL_CONTENT_URI, internalUri);
        assertEquals(Thumbnails.EXTERNAL_CONTENT_URI, externalUri);
    }

    public void testGetThumbnail() throws Exception {
        // Insert a video into the provider.
        Uri videoUri = insertVideo();
        long videoId = ContentUris.parseId(videoUri);
        assertTrue(videoId != -1);
        assertEquals(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, videoId),
                videoUri);

        // Get the current thumbnail count for future comparison.
        int count = getThumbnailCount(Thumbnails.EXTERNAL_CONTENT_URI);

        // Calling getThumbnail should generate a new thumbnail.
        assertNotNull(Thumbnails.getThumbnail(mResolver, videoId, Thumbnails.MINI_KIND, null));
        assertNotNull(Thumbnails.getThumbnail(mResolver, videoId, Thumbnails.MICRO_KIND, null));

        try {
            Thumbnails.getThumbnail(mResolver, videoId, Thumbnails.FULL_SCREEN_KIND, null);
            fail();
        } catch (IllegalArgumentException e) {
            // Full screen thumbnails not supported by getThumbnail...
        }

        // Check that an additional thumbnails have been registered.
        int count2 = getThumbnailCount(Thumbnails.EXTERNAL_CONTENT_URI);
        assertTrue(count2 > count);

        Cursor c = mResolver.query(Thumbnails.EXTERNAL_CONTENT_URI,
                new String[] { Thumbnails._ID, Thumbnails.DATA, Thumbnails.VIDEO_ID },
                null, null, null);

        if (c.moveToLast()) {
            long vid = c.getLong(2);
            assertEquals(videoId, vid);
            String path = c.getString(1);
            assertTrue("thumbnail file does not exist", new File(path).exists());
            long id = c.getLong(0);
            mResolver.delete(ContentUris.withAppendedId(Thumbnails.EXTERNAL_CONTENT_URI, id),
                    null, null);
            assertFalse("thumbnail file should no longer exist", new File(path).exists());
        }
        c.close();

        assertEquals(1, mResolver.delete(videoUri, null, null));
    }

    private Uri insertVideo() throws IOException {
        File file = new File(Environment.getExternalStorageDirectory(), "testVideo.3gp");
        // clean up any potential left over entries from a previous aborted run
        mResolver.delete(Media.EXTERNAL_CONTENT_URI,
                "_data=?", new String[] { file.getAbsolutePath() });
        file.delete();
        mFileHelper.copyToExternalStorage(R.raw.testvideo, file);

        ContentValues values = new ContentValues();
        values.put(VideoColumns.DATA, file.getAbsolutePath());
        return mResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
    }

    private int getThumbnailCount(Uri uri) {
        Cursor cursor = mResolver.query(uri, null, null, null, null);
        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
    }
}
