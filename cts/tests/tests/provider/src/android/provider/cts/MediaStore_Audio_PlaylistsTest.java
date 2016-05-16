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
import android.database.SQLException;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.Audio.Playlists;
import android.test.InstrumentationTestCase;

import java.util.regex.Pattern;

public class MediaStore_Audio_PlaylistsTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentResolver = getInstrumentation().getContext().getContentResolver();
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(
                Playlists.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME), null, null,
                null, null));
        c.close();

        // can not accept any other volume names
        try {
            assertNotNull(c = mContentResolver.query(
                    Playlists.getContentUri(MediaStoreAudioTestHelper.INTERNAL_VOLUME_NAME), null,
                    null, null, null));
            c.close();
            fail("Should throw SQLException as the internal datatbase has no playlist");
        } catch (SQLException e) {
            // expected
        }

        String volume = "fakeVolume";
        assertNull(mContentResolver.query(Playlists.getContentUri(volume), null, null, null,
                null));
    }

    public void testStoreAudioPlaylistsExternal() {
        final String externalPlaylistPath = Environment.getExternalStorageDirectory().getPath() +
            "/my_favorites.pl";
        ContentValues values = new ContentValues();
        values.put(Playlists.NAME, "My favourites");
        values.put(Playlists.DATA, externalPlaylistPath);
        long dateAdded = System.currentTimeMillis() / 1000;
        long dateModified = System.currentTimeMillis() / 1000;
        values.put(Playlists.DATE_MODIFIED, dateModified);
        // insert
        Uri uri = mContentResolver.insert(Playlists.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);

        try {
            // query
            Cursor c = mContentResolver.query(uri, null, null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals("My favourites", c.getString(c.getColumnIndex(Playlists.NAME)));
            assertEquals(externalPlaylistPath,
                    c.getString(c.getColumnIndex(Playlists.DATA)));

            long realDateAdded = c.getLong(c.getColumnIndex(Playlists.DATE_ADDED));
            assertTrue(realDateAdded >= dateAdded);
            assertEquals(dateModified, c.getLong(c.getColumnIndex(Playlists.DATE_MODIFIED)));
            assertTrue(c.getLong(c.getColumnIndex(Playlists._ID)) > 0);
            c.close();

            // update
            values.clear();
            values.put(Playlists.NAME, "xxx");
            dateModified = System.currentTimeMillis();
            values.put(Playlists.DATE_MODIFIED, dateModified);
            assertEquals(1, mContentResolver.update(uri, values, null, null));
            c = mContentResolver.query(uri, null, null, null, null);
            c.moveToFirst();
            assertEquals("xxx", c.getString(c.getColumnIndex(Playlists.NAME)));
            assertEquals(externalPlaylistPath,
                    c.getString(c.getColumnIndex(Playlists.DATA)));

            assertEquals(realDateAdded, c.getLong(c.getColumnIndex(Playlists.DATE_ADDED)));
            assertEquals(dateModified, c.getLong(c.getColumnIndex(Playlists.DATE_MODIFIED)));
            c.close();
        } finally {
            assertEquals(1, mContentResolver.delete(uri, null, null));
        }
    }

    public void testStoreAudioPlaylistsInternal() {
        ContentValues values = new ContentValues();
        values.put(Playlists.NAME, "My favourites");
        values.put(Playlists.DATA, "/data/data/com.android.cts.stub/files/my_favorites.pl");
        long dateAdded = System.currentTimeMillis();
        values.put(Playlists.DATE_ADDED, dateAdded);
        long dateModified = System.currentTimeMillis();
        values.put(Playlists.DATE_MODIFIED, dateModified);
        // insert
        Uri uri = mContentResolver.insert(Playlists.INTERNAL_CONTENT_URI, values);
        assertNotNull(uri);

        try {
            assertTrue(Pattern.matches("content://media/internal/audio/playlists/\\d+",
                    uri.toString()));
        } finally {
            // delete the playlists
            assertEquals(1, mContentResolver.delete(uri, null, null));
        }
    }
}
