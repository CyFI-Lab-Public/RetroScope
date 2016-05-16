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


import com.android.cts.stub.R;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.cts.FileUtils;
import android.provider.MediaStore;
import android.provider.MediaStore.Video.Media;
import android.provider.MediaStore.Video.VideoColumns;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.IOException;

public class MediaStore_Video_MediaTest extends AndroidTestCase {
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentResolver = getContext().getContentResolver();
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(Media.getContentUri("internal"), null, null, null,
                null));
        c.close();
        assertNotNull(c = mContentResolver.query(Media.getContentUri("external"), null, null, null,
                null));
        c.close();

        // can not accept any other volume names
        String volume = "fakeVolume";
        assertNull(mContentResolver.query(Media.getContentUri(volume), null, null, null, null));
    }

    private void cleanExternalMediaFile(String path) {
        mContentResolver.delete(Media.EXTERNAL_CONTENT_URI, "_data=?", new String[] { path });
        new File(path).delete();
    }

    public void testStoreVideoMediaExternal() throws Exception {
        final String externalVideoPath = Environment.getExternalStorageDirectory().getPath() +
                 "/video/testvideo.3gp";
        final String externalVideoPath2 = Environment.getExternalStorageDirectory().getPath() +
                "/video/testvideo1.3gp";

        // clean up any potential left over entries from a previous aborted run
        cleanExternalMediaFile(externalVideoPath);
        cleanExternalMediaFile(externalVideoPath2);

        int numBytes = 1337;
        File videoFile = new File(externalVideoPath);
        FileUtils.createFile(videoFile, numBytes);

        ContentValues values = new ContentValues();
        values.put(Media.ALBUM, "cts");
        values.put(Media.ARTIST, "cts team");
        values.put(Media.CATEGORY, "test");
        long dateTaken = System.currentTimeMillis();
        values.put(Media.DATE_TAKEN, dateTaken);
        values.put(Media.DESCRIPTION, "This is a video");
        values.put(Media.DURATION, 8480);
        values.put(Media.LANGUAGE, "en");
        values.put(Media.LATITUDE, 40.689060d);
        values.put(Media.LONGITUDE, -74.044636d);
        values.put(Media.IS_PRIVATE, 1);
        values.put(Media.MINI_THUMB_MAGIC, 0);
        values.put(Media.RESOLUTION, "176x144");
        values.put(Media.TAGS, "cts, test");
        values.put(Media.DATA, externalVideoPath);
        values.put(Media.DISPLAY_NAME, "testvideo");
        values.put(Media.MIME_TYPE, "video/3gpp");
        values.put(Media.SIZE, numBytes);
        values.put(Media.TITLE, "testvideo");
        long dateAdded = System.currentTimeMillis() / 1000;
        values.put(Media.DATE_ADDED, dateAdded);
        long dateModified = videoFile.lastModified() / 1000;
        values.put(Media.DATE_MODIFIED, dateModified);

        // insert
        Uri uri = mContentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);

        try {
            // query
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id > 0);
            assertEquals("cts", c.getString(c.getColumnIndex(Media.ALBUM)));
            assertEquals("cts team", c.getString(c.getColumnIndex(Media.ARTIST)));
            assertEquals("test", c.getString(c.getColumnIndex(Media.CATEGORY)));
            assertEquals(dateTaken, c.getLong(c.getColumnIndex(Media.DATE_TAKEN)));
            assertEquals(8480, c.getInt(c.getColumnIndex(Media.DURATION)));
            assertEquals("This is a video",
                    c.getString(c.getColumnIndex(Media.DESCRIPTION)));
            assertEquals("en", c.getString(c.getColumnIndex(Media.LANGUAGE)));
            assertEquals(40.689060d, c.getDouble(c.getColumnIndex(Media.LATITUDE)), 0d);
            assertEquals(-74.044636d, c.getDouble(c.getColumnIndex(Media.LONGITUDE)), 0d);
            assertEquals(1, c.getInt(c.getColumnIndex(Media.IS_PRIVATE)));
            assertEquals(0, c.getLong(c.getColumnIndex(Media.MINI_THUMB_MAGIC)));
            assertEquals("176x144", c.getString(c.getColumnIndex(Media.RESOLUTION)));
            assertEquals("cts, test", c.getString(c.getColumnIndex(Media.TAGS)));
            assertEquals(externalVideoPath, c.getString(c.getColumnIndex(Media.DATA)));
            assertEquals("testvideo.3gp", c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals("video/3gpp", c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals("testvideo", c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(numBytes, c.getInt(c.getColumnIndex(Media.SIZE)));
            long realDateAdded = c.getLong(c.getColumnIndex(Media.DATE_ADDED));
            assertTrue(realDateAdded >= dateAdded);
            assertEquals(dateModified, c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)));
            c.close();

            // update
            values.clear();
            values.put(Media.ALBUM, "cts1");
            values.put(Media.ARTIST, "cts team1");
            values.put(Media.CATEGORY, "test1");
            dateTaken = System.currentTimeMillis();
            values.put(Media.DATE_TAKEN, dateTaken);
            values.put(Media.DESCRIPTION, "This is another video");
            values.put(Media.DURATION, 8481);
            values.put(Media.LANGUAGE, "cn");
            values.put(Media.LATITUDE, 41.689060d);
            values.put(Media.LONGITUDE, -75.044636d);
            values.put(Media.IS_PRIVATE, 0);
            values.put(Media.MINI_THUMB_MAGIC, 2);
            values.put(Media.RESOLUTION, "320x240");
            values.put(Media.TAGS, "cts1, test1");
            values.put(Media.DATA, externalVideoPath2);
            values.put(Media.DISPLAY_NAME, "testvideo1");
            values.put(Media.MIME_TYPE, "video/3gpp");
            values.put(Media.SIZE, 86854);
            values.put(Media.TITLE, "testvideo1");
            dateModified = System.currentTimeMillis();
            values.put(Media.DATE_MODIFIED, dateModified);
            assertEquals(1, mContentResolver.update(uri, values, null, null));

            c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals(id, c.getLong(c.getColumnIndex(Media._ID)));
            assertEquals("cts1", c.getString(c.getColumnIndex(Media.ALBUM)));
            assertEquals("cts team1", c.getString(c.getColumnIndex(Media.ARTIST)));
            assertEquals("test1", c.getString(c.getColumnIndex(Media.CATEGORY)));
            assertEquals(dateTaken, c.getLong(c.getColumnIndex(Media.DATE_TAKEN)));
            assertEquals(8481, c.getInt(c.getColumnIndex(Media.DURATION)));
            assertEquals("This is another video",
                    c.getString(c.getColumnIndex(Media.DESCRIPTION)));
            assertEquals("cn", c.getString(c.getColumnIndex(Media.LANGUAGE)));
            assertEquals(41.689060d, c.getDouble(c.getColumnIndex(Media.LATITUDE)), 0d);
            assertEquals(-75.044636d, c.getDouble(c.getColumnIndex(Media.LONGITUDE)), 0d);
            assertEquals(0, c.getInt(c.getColumnIndex(Media.IS_PRIVATE)));
            assertEquals(2, c.getLong(c.getColumnIndex(Media.MINI_THUMB_MAGIC)));
            assertEquals("320x240", c.getString(c.getColumnIndex(Media.RESOLUTION)));
            assertEquals("cts1, test1", c.getString(c.getColumnIndex(Media.TAGS)));
            assertEquals(externalVideoPath2,
                    c.getString(c.getColumnIndex(Media.DATA)));
            assertEquals("testvideo1", c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals("video/3gpp", c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals("testvideo1", c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(86854, c.getInt(c.getColumnIndex(Media.SIZE)));
            assertEquals(realDateAdded, c.getLong(c.getColumnIndex(Media.DATE_ADDED)));
            assertEquals(dateModified, c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)));
            c.close();
        } finally {
            // delete
            assertEquals(1, mContentResolver.delete(uri, null, null));
            new File(externalVideoPath).delete();
        }

        // check that the video file is removed when deleting the database entry
        Context context = getContext();
        Uri videoUri = insertVideo(context);
        File videofile = new File(Environment.getExternalStorageDirectory(), "testVideo.3gp");
        assertTrue(videofile.exists());
        mContentResolver.delete(videoUri, null, null);
        assertFalse(videofile.exists());

        // insert again, then delete with the "delete data" parameter set to false
        videoUri = insertVideo(context);
        assertTrue(videofile.exists());
        Uri.Builder builder = videoUri.buildUpon();
        builder.appendQueryParameter(MediaStore.PARAM_DELETE_DATA, "false");
        mContentResolver.delete(builder.build(), null, null);
        assertTrue(videofile.exists());
        videofile.delete();

    }

    public void testStoreVideoMediaInternal() {
        // can not insert any data, so other operations can not be tested
        try {
            mContentResolver.insert(Media.INTERNAL_CONTENT_URI, new ContentValues());
            fail("Should throw UnsupportedOperationException when inserting into internal "
                    + "database");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    private Uri insertVideo(Context context) throws IOException {
        File file = new File(Environment.getExternalStorageDirectory(), "testVideo.3gp");
        new FileCopyHelper(context).copyToExternalStorage(R.raw.testvideo, file);

        ContentValues values = new ContentValues();
        values.put(VideoColumns.DATA, file.getAbsolutePath());
        return context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
    }
}
