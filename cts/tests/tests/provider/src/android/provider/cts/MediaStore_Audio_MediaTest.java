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
import android.os.Environment;
import android.provider.MediaStore.Audio.Media;
import android.provider.cts.MediaStoreAudioTestHelper.Audio1;
import android.provider.cts.MediaStoreAudioTestHelper.Audio2;
import android.test.InstrumentationTestCase;

public class MediaStore_Audio_MediaTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentResolver = getInstrumentation().getContext().getContentResolver();
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(
                Media.getContentUri(MediaStoreAudioTestHelper.INTERNAL_VOLUME_NAME), null, null,
                    null, null));
        c.close();
        assertNotNull(c = mContentResolver.query(
                Media.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME), null, null,
                    null, null));
        c.close();

        // can not accept any other volume names
        String volume = "faveVolume";
        assertNull(mContentResolver.query(Media.getContentUri(volume), null, null, null, null));
    }

    public void testGetContentUriForPath() {
        Cursor c = null;
        String externalPath = Environment.getExternalStorageDirectory().getPath();
        assertNotNull(c = mContentResolver.query(Media.getContentUriForPath(externalPath), null, null,
                null, null));
        c.close();

        String internalPath =
            getInstrumentation().getTargetContext().getFilesDir().getAbsolutePath();
        assertNotNull(c = mContentResolver.query(Media.getContentUriForPath(internalPath), null, null,
                null, null));
        c.close();
    }

    public void testStoreAudioMediaInternal() {
        testStoreAudioMedia(true);
    }

    public void testStoreAudioMediaExternal() {
        testStoreAudioMedia(false);
    }

    private void testStoreAudioMedia(boolean isInternal) {
        Audio1 audio1 = Audio1.getInstance();
        ContentValues values = audio1.getContentValues(isInternal);
        //insert
        Uri mediaUri = isInternal ? Media.INTERNAL_CONTENT_URI : Media.EXTERNAL_CONTENT_URI;
        Uri uri = mContentResolver.insert(mediaUri, values);
        assertNotNull(uri);

        try {
            // query
            // the following columns in the table are generated automatically when inserting:
            // _ID, DATE_ADDED, ALBUM_ID, ALBUM_KEY, ARTIST_ID, ARTIST_KEY, TITLE_KEY
            // the column DISPLAY_NAME will be ignored when inserting
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long id = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id > 0);
            String expected = isInternal ? Audio1.INTERNAL_DATA : Audio1.EXTERNAL_DATA;
            assertEquals(expected, c.getString(c.getColumnIndex(Media.DATA)));
            assertTrue(c.getLong(c.getColumnIndex(Media.DATE_ADDED)) > 0);
            assertEquals(Audio1.DATE_MODIFIED, c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)));
            assertEquals(Audio1.IS_DRM, c.getInt(c.getColumnIndex(Media.IS_DRM)));
            assertEquals(Audio1.FILE_NAME, c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals(Audio1.MIME_TYPE, c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals(Audio1.SIZE, c.getInt(c.getColumnIndex(Media.SIZE)));
            assertEquals(Audio1.TITLE, c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(Audio1.ALBUM, c.getString(c.getColumnIndex(Media.ALBUM)));
            String albumKey = c.getString(c.getColumnIndex(Media.ALBUM_KEY));
            assertNotNull(albumKey);
            long albumId = c.getLong(c.getColumnIndex(Media.ALBUM_ID));
            assertTrue(albumId > 0);
            assertEquals(Audio1.ARTIST, c.getString(c.getColumnIndex(Media.ARTIST)));
            String artistKey = c.getString(c.getColumnIndex(Media.ARTIST_KEY));
            assertNotNull(artistKey);
            long artistId = c.getLong(c.getColumnIndex(Media.ARTIST_ID));
            assertTrue(artistId > 0);
            assertEquals(Audio1.COMPOSER, c.getString(c.getColumnIndex(Media.COMPOSER)));
            assertEquals(Audio1.DURATION, c.getLong(c.getColumnIndex(Media.DURATION)));
            assertEquals(Audio1.IS_ALARM, c.getInt(c.getColumnIndex(Media.IS_ALARM)));
            assertEquals(Audio1.IS_MUSIC, c.getInt(c.getColumnIndex(Media.IS_MUSIC)));
            assertEquals(Audio1.IS_NOTIFICATION, c.getInt(c.getColumnIndex(Media.IS_NOTIFICATION)));
            assertEquals(Audio1.IS_RINGTONE, c.getInt(c.getColumnIndex(Media.IS_RINGTONE)));
            assertEquals(Audio1.TRACK, c.getInt(c.getColumnIndex(Media.TRACK)));
            assertEquals(Audio1.YEAR, c.getInt(c.getColumnIndex(Media.YEAR)));
            String titleKey = c.getString(c.getColumnIndex(Media.TITLE_KEY));
            assertNotNull(titleKey);
            c.close();

            // update
            // the column DISPLAY_NAME will not be ignored when updating
            Audio2 audio2 = Audio2.getInstance();
            values = audio2.getContentValues(isInternal);

            int result = mContentResolver.update(uri, values, null, null);
            assertEquals(1, result);
            c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long id2 = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id == id2);
            expected = isInternal ? Audio2.INTERNAL_DATA : Audio2.EXTERNAL_DATA;
            assertEquals(expected, c.getString(c.getColumnIndex(Media.DATA)));
            assertEquals(Audio2.DATE_MODIFIED, c.getLong(c.getColumnIndex(Media.DATE_MODIFIED)));
            assertEquals(Audio2.IS_DRM, c.getInt(c.getColumnIndex(Media.IS_DRM)));
            assertEquals(Audio2.DISPLAY_NAME, c.getString(c.getColumnIndex(Media.DISPLAY_NAME)));
            assertEquals(Audio2.MIME_TYPE, c.getString(c.getColumnIndex(Media.MIME_TYPE)));
            assertEquals(Audio2.SIZE, c.getInt(c.getColumnIndex(Media.SIZE)));
            assertEquals(Audio2.TITLE, c.getString(c.getColumnIndex(Media.TITLE)));
            assertEquals(Audio2.ALBUM, c.getString(c.getColumnIndex(Media.ALBUM)));
            assertFalse(albumKey.equals(c.getString(c.getColumnIndex(Media.ALBUM_KEY))));
            assertTrue(albumId !=  c.getLong(c.getColumnIndex(Media.ALBUM_ID)));
            assertEquals(Audio2.ARTIST, c.getString(c.getColumnIndex(Media.ARTIST)));
            assertFalse(artistKey.equals(c.getString(c.getColumnIndex(Media.ARTIST_KEY))));
            assertTrue(artistId !=  c.getLong(c.getColumnIndex(Media.ARTIST_ID)));
            assertEquals(Audio2.COMPOSER, c.getString(c.getColumnIndex(Media.COMPOSER)));
            assertEquals(Audio2.DURATION, c.getLong(c.getColumnIndex(Media.DURATION)));
            assertEquals(Audio2.IS_ALARM, c.getInt(c.getColumnIndex(Media.IS_ALARM)));
            assertEquals(Audio2.IS_MUSIC, c.getInt(c.getColumnIndex(Media.IS_MUSIC)));
            assertEquals(Audio2.IS_NOTIFICATION,
                    c.getInt(c.getColumnIndex(Media.IS_NOTIFICATION)));
            assertEquals(Audio2.IS_RINGTONE, c.getInt(c.getColumnIndex(Media.IS_RINGTONE)));
            assertEquals(Audio2.TRACK, c.getInt(c.getColumnIndex(Media.TRACK)));
            assertEquals(Audio2.YEAR, c.getInt(c.getColumnIndex(Media.YEAR)));
            assertTrue(titleKey.equals(c.getString(c.getColumnIndex(Media.TITLE_KEY))));
            c.close();

            // test filtering
            Uri baseUri = isInternal ? Media.INTERNAL_CONTENT_URI : Media.EXTERNAL_CONTENT_URI;
            Uri filterUri = baseUri.buildUpon()
                .appendQueryParameter("filter", Audio2.ARTIST).build();
            c = mContentResolver.query(filterUri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            long fid = c.getLong(c.getColumnIndex(Media._ID));
            assertTrue(id == fid);
            c.close();

            filterUri = baseUri.buildUpon().appendQueryParameter("filter", "xyzfoo").build();
            c = mContentResolver.query(filterUri, null, null, null, null);
            assertEquals(0, c.getCount());
            c.close();
        } finally {
            // delete
            int result = mContentResolver.delete(uri, null, null);
            assertEquals(1, result);
        }
    }
}
