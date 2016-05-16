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
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Genres.Members;
import android.provider.cts.MediaStoreAudioTestHelper.Audio1;
import android.test.InstrumentationTestCase;

public class MediaStore_Audio_GenresTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentResolver = getInstrumentation().getContext().getContentResolver();
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(
                Genres.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME), null, null,
                    null, null));
        c.close();
        try {
            assertNotNull(c = mContentResolver.query(
                    Genres.getContentUri(MediaStoreAudioTestHelper.INTERNAL_VOLUME_NAME), null,
                        null, null, null));
            c.close();
            fail("Should throw SQLException as the internal datatbase has no genre");
        } catch (SQLException e) {
            // expected
        }

        // can not accept any other volume names
        String volume = "fakeVolume";
        assertNull(mContentResolver.query(Genres.getContentUri(volume), null, null, null, null));
    }

    public void testStoreAudioGenresExternal() {
        // insert
        ContentValues values = new ContentValues();
        values.put(Genres.NAME, "POP");
        Uri uri = mContentResolver.insert(Genres.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);

        try {
            // query
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals("POP", c.getString(c.getColumnIndex(Genres.NAME)));
            assertTrue(c.getLong(c.getColumnIndex(Genres._ID)) > 0);
            c.close();

            // update
            values.clear();
            values.put(Genres.NAME, "ROCK");
            assertEquals(1, mContentResolver.update(uri, values, null, null));
            c = mContentResolver.query(uri, null, null, null, null);
            c.moveToFirst();
            assertEquals("ROCK", c.getString(c.getColumnIndex(Genres.NAME)));
            c.close();
        } finally {
            assertEquals(1, mContentResolver.delete(uri, null, null));
        }
    }

    public void testStoreAudioGenresInternal() {
        // the internal database does not have genres
        ContentValues values = new ContentValues();
        values.put(Genres.NAME, "POP");
        Uri uri = mContentResolver.insert(Genres.INTERNAL_CONTENT_URI, values);
        assertNull(uri);
    }

    public void testGetContentUriForAudioId() {
        // Insert an audio file into the content provider.
        ContentValues values = Audio1.getInstance().getContentValues(true);
        Uri audioUri = mContentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(audioUri);
        long audioId = ContentUris.parseId(audioUri);
        assertTrue(audioId != -1);

        // Insert a genre into the content provider.
        values.clear();
        values.put(Genres.NAME, "Soda Pop");
        Uri genreUri = mContentResolver.insert(Genres.EXTERNAL_CONTENT_URI, values);
        assertNotNull(genreUri);
        long genreId = ContentUris.parseId(genreUri);
        assertTrue(genreId != -1);

        Cursor cursor = null;
        try {
            String volumeName = MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME;

            // Check that the audio file has no genres yet.
            Uri audioGenresUri = Genres.getContentUriForAudioId(volumeName, (int) audioId);
            cursor = mContentResolver.query(audioGenresUri, null, null, null, null);
            assertFalse(cursor.moveToNext());

            // Link the audio file to the genre.
            values.clear();
            values.put(Members.AUDIO_ID, audioId);
            Uri membersUri = Members.getContentUri(volumeName, genreId);
            assertNotNull(mContentResolver.insert(membersUri, values));

            // Check that the audio file has the genre it was linked to.
            cursor = mContentResolver.query(audioGenresUri, null, null, null, null);
            assertTrue(cursor.moveToNext());
            assertEquals(genreId, cursor.getLong(cursor.getColumnIndex(Genres._ID)));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            assertEquals(1, mContentResolver.delete(audioUri, null, null));
            assertEquals(1, mContentResolver.delete(genreUri, null, null));
        }
    }
}
