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
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Genres.Members;
import android.provider.cts.MediaStoreAudioTestHelper.Audio1;
import android.provider.cts.MediaStoreAudioTestHelper.Audio2;
import android.test.InstrumentationTestCase;

public class MediaStore_Audio_Genres_MembersTest extends InstrumentationTestCase {
    private ContentResolver mContentResolver;

    private long mAudioIdOfJam;

    private long mAudioIdOfJamLive;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContentResolver = getInstrumentation().getContext().getContentResolver();
        Uri uri = Audio1.getInstance().insertToExternal(mContentResolver);
        Cursor c = mContentResolver.query(uri, null, null, null, null);
        c.moveToFirst();
        mAudioIdOfJam = c.getLong(c.getColumnIndex(Media._ID));
        c.close();

        uri = Audio2.getInstance().insertToExternal(mContentResolver);
        c = mContentResolver.query(uri, null, null, null, null);
        c.moveToFirst();
        mAudioIdOfJamLive = c.getLong(c.getColumnIndex(Media._ID));
        c.close();
    }

    @Override
    protected void tearDown() throws Exception {
        // "jam" should already have been deleted as part of the test, but delete it again just
        // in case the test failed and aborted before that.
        mContentResolver.delete(Media.EXTERNAL_CONTENT_URI, Media._ID + "=" + mAudioIdOfJam, null);
        mContentResolver.delete(Media.EXTERNAL_CONTENT_URI, Media._ID + "=" + mAudioIdOfJamLive,
                null);
        super.tearDown();
    }

    public void testGetContentUri() {
        Cursor c = null;
        assertNotNull(c = mContentResolver.query(
                Members.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME, 1), null,
                    null, null, null));
        c.close();

        try {
            assertNotNull(c = mContentResolver.query(
                    Members.getContentUri(MediaStoreAudioTestHelper.INTERNAL_VOLUME_NAME, 1), null,
                        null, null, null));
            c.close();
            fail("Should throw SQLException as the internal datatbase has no genre");
        } catch (SQLException e) {
            // expected
        }

        // can not accept any other volume names
        String volume = "fakeVolume";
        assertNull(mContentResolver.query(Members.getContentUri(volume, 1), null, null, null,
                null));
    }

    public void testStoreAudioGenresMembersExternal() {
        ContentValues values = new ContentValues();
        values.put(Genres.NAME, Audio1.GENRE);
        Uri uri = mContentResolver.insert(Genres.EXTERNAL_CONTENT_URI, values);
        Cursor c = mContentResolver.query(uri, null, null, null, null);
        c.moveToFirst();

        long genreId = c.getLong(c.getColumnIndex(Genres._ID));
        long genre2Id = -1; // used later
        c.close();

        // verify that the Uri has the correct format and genre value
        assertEquals(uri.toString(), "content://media/external/audio/genres/" + genreId);

        // insert audio as the member of the genre
        values.clear();
        values.put(Members.AUDIO_ID, mAudioIdOfJam);
        Uri membersUri = Members.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME,
                genreId);
        assertNotNull(mContentResolver.insert(membersUri, values));

        try {
            // query, slow path
            c = mContentResolver.query(membersUri, null, null, null, null);

            assertEquals(1, c.getCount());
            c.moveToFirst();

            assertEquals(mAudioIdOfJam, c.getLong(c.getColumnIndex(Members.AUDIO_ID)));
            assertEquals(genreId, c.getLong(c.getColumnIndex(Members.GENRE_ID)));
            assertEquals(mAudioIdOfJam, c.getLong(c.getColumnIndex(Members._ID)));
            assertEquals(Audio1.EXTERNAL_DATA, c.getString(c.getColumnIndex(Members.DATA)));
            assertTrue(c.getLong(c.getColumnIndex(Members.DATE_ADDED)) > 0);
            assertEquals(Audio1.DATE_MODIFIED, c.getLong(c.getColumnIndex(Members.DATE_MODIFIED)));
            assertEquals(Audio1.FILE_NAME, c.getString(c.getColumnIndex(Members.DISPLAY_NAME)));
            assertEquals(Audio1.MIME_TYPE, c.getString(c.getColumnIndex(Members.MIME_TYPE)));
            assertEquals(Audio1.SIZE, c.getInt(c.getColumnIndex(Members.SIZE)));
            assertEquals(Audio1.TITLE, c.getString(c.getColumnIndex(Members.TITLE)));
            assertEquals(Audio1.ALBUM, c.getString(c.getColumnIndex(Members.ALBUM)));
            assertEquals(Audio1.IS_DRM, c.getInt(c.getColumnIndex(Members.IS_DRM)));
            String albumKey = c.getString(c.getColumnIndex(Members.ALBUM_KEY));
            assertNotNull(albumKey);
            long albumId = c.getLong(c.getColumnIndex(Members.ALBUM_ID));
            assertTrue(albumId > 0);
            assertEquals(Audio1.ARTIST, c.getString(c.getColumnIndex(Members.ARTIST)));
            String artistKey = c.getString(c.getColumnIndex(Members.ARTIST_KEY));
            assertNotNull(artistKey);
            long artistId = c.getLong(c.getColumnIndex(Members.ARTIST_ID));
            assertTrue(artistId > 0);
            assertEquals(Audio1.COMPOSER, c.getString(c.getColumnIndex(Members.COMPOSER)));
            assertEquals(Audio1.DURATION, c.getLong(c.getColumnIndex(Members.DURATION)));
            assertEquals(Audio1.IS_ALARM, c.getInt(c.getColumnIndex(Members.IS_ALARM)));
            assertEquals(Audio1.IS_MUSIC, c.getInt(c.getColumnIndex(Members.IS_MUSIC)));
            assertEquals(Audio1.IS_NOTIFICATION,
                    c.getInt(c.getColumnIndex(Members.IS_NOTIFICATION)));
            assertEquals(Audio1.IS_RINGTONE, c.getInt(c.getColumnIndex(Members.IS_RINGTONE)));
            assertEquals(Audio1.TRACK, c.getInt(c.getColumnIndex(Members.TRACK)));
            assertEquals(Audio1.YEAR, c.getInt(c.getColumnIndex(Members.YEAR)));
            String titleKey = c.getString(c.getColumnIndex(Members.TITLE_KEY));
            assertNotNull(titleKey);
            c.close();

            // query again, fast path
            c = mContentResolver.query(membersUri,
                    new String[] { Members.AUDIO_ID, Members.GENRE_ID},
                    null, null, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals(mAudioIdOfJam, c.getLong(c.getColumnIndex(Members.AUDIO_ID)));
            assertEquals(genreId, c.getLong(c.getColumnIndex(Members.GENRE_ID)));
            c.close();

            // Query with a constraint on _id. Note that _id corresponds to the _id
            // column in the audio table, not the one in the audio_genres_map table.
            // We need to preserve this behavior for backward compatibility.
            c = mContentResolver.query(membersUri, null,
                    Members._ID + "=?", new String[] {Long.toString(mAudioIdOfJam)}, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals(mAudioIdOfJam, c.getLong(c.getColumnIndex(Members._ID)));
            c.close();

            // Query members across all genres
            Uri allMembersUri = Uri.parse("content://media/external/audio/genres/all/members");
            c = mContentResolver.query(allMembersUri, null, null, null, null);
            int colidx = c.getColumnIndex(Members.AUDIO_ID);
            int jamcnt = 0;
            // The song should appear only once, for the genre we used when inserting it
            while(c.moveToNext()) {
                if (c.getLong(colidx) == mAudioIdOfJam) {
                    jamcnt++;
                    assertEquals(genreId, c.getLong(c.getColumnIndex(Members.GENRE_ID)));
                }
            }
            assertEquals(1, jamcnt);
            c.close();

            // Query the same Uri, but add a where clause to restrict it to the one entry we added
            c = mContentResolver.query(allMembersUri, null,
                    Members.AUDIO_ID + "=?", new String[] {Long.toString(mAudioIdOfJam)}, null);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            assertEquals(genreId, c.getLong(c.getColumnIndex(Members.GENRE_ID)));
            assertEquals(mAudioIdOfJam, c.getLong(c.getColumnIndex(Members.AUDIO_ID)));
            c.close();

            // create another genre
            values.clear();
            values.put(Genres.NAME, Audio1.GENRE + "-2");
            uri = mContentResolver.insert(Genres.EXTERNAL_CONTENT_URI, values);
            c = mContentResolver.query(uri, null, null, null, null);
            c.moveToFirst();
            genre2Id = c.getLong(c.getColumnIndex(Genres._ID));
            c.close();

            // insert the song into the second genre
            values.clear();
            values.put(Members.AUDIO_ID, mAudioIdOfJam);
            Uri members2Uri = Members.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME,
                genre2Id);
            assertNotNull(mContentResolver.insert(members2Uri, values));

            // Query members across all genres again
            c = mContentResolver.query(allMembersUri, null, null, null, null);
            colidx = c.getColumnIndex(Members.AUDIO_ID);
            int jamcnt1 = 0;
            int jamcnt2 = 0;
            // This time the song should appear twice, once for each genre
            while(c.moveToNext()) {
                if (c.getLong(colidx) == mAudioIdOfJam) {
                    long g = c.getLong(c.getColumnIndex(Members.GENRE_ID));
                    if (g == genreId) {
                        jamcnt1++;
                    } else if (g == genre2Id) {
                        jamcnt2++;
                    } else {
                        fail("wrong genre found");
                    }
                }
            }
            assertEquals(1, jamcnt1);
            assertEquals(1, jamcnt2);
            c.close();


            // update the member
            values.clear();
            values.put(Members.AUDIO_ID, mAudioIdOfJamLive);
            try {
                mContentResolver.update(membersUri, values, null, null);
                fail("Should throw SQLException because there is no column with name "
                        + "\"Members.AUDIO_ID\" in the table");
            } catch (SQLException e) {
                // expected
            }

            // Delete the members, note that this does not delete the genre itself
            assertEquals(1, mContentResolver.delete(membersUri, null, null)); // check number of rows deleted

            // verify the genre is now empty
            c = mContentResolver.query(membersUri, null, null, null, null);
            assertEquals(0, c.getCount());
            c.close();

            // same for 2nd genre
            assertEquals(1, mContentResolver.delete(members2Uri, null, null));
            c = mContentResolver.query(members2Uri, null, null, null, null);
            assertEquals(0, c.getCount());
            c.close();

            // insert again, then verify that deleting the audio entry cleans up its genre member
            // entry as well
            values.put(Members.AUDIO_ID, mAudioIdOfJam);
            membersUri = Members.getContentUri(MediaStoreAudioTestHelper.EXTERNAL_VOLUME_NAME,
                    genreId);
            assertNotNull(mContentResolver.insert(membersUri, values));
            // Query members across all genres
            c = mContentResolver.query(allMembersUri,
                    new String[] { Members.AUDIO_ID, Members.GENRE_ID}, null, null, null);
            colidx = c.getColumnIndex(Members.AUDIO_ID);
            jamcnt = 0;
            // The song should appear only once, for the genre we used when inserting it
            while(c.moveToNext()) {
                if (c.getLong(colidx) == mAudioIdOfJam) {
                    jamcnt++;
                    assertEquals(genreId, c.getLong(c.getColumnIndex(Members.GENRE_ID)));
                }
            }
            assertEquals(1, jamcnt);
            c.close();
            mContentResolver.delete(Media.EXTERNAL_CONTENT_URI,
                    Media._ID + "=" + mAudioIdOfJam, null);
            // Query members across all genres
            c = mContentResolver.query(allMembersUri,
                    new String[] { Members.AUDIO_ID, Members.GENRE_ID}, null, null, null);
            colidx = c.getColumnIndex(Members.AUDIO_ID);
            jamcnt = 0;
            // The song should no longer appear in the genre
            while(c.moveToNext()) {
                if (c.getLong(colidx) == mAudioIdOfJam) {
                    jamcnt++;
                }
            }
            assertEquals(0, jamcnt);
            c.close();
        } finally {
            // the members are deleted when deleting the genre which they belong to
            mContentResolver.delete(Genres.EXTERNAL_CONTENT_URI, Genres._ID + "=" + genreId, null);
            if (genre2Id >= 0) {
                mContentResolver.delete(Genres.EXTERNAL_CONTENT_URI, Genres._ID + "=" + genre2Id, null);
            }
            c = mContentResolver.query(membersUri, null, null, null, null);
            assertEquals(0, c.getCount());
            c.close();
        }
    }

    public void testStoreAudioGenresMembersInternal() {
        // the internal database can not have genres
        ContentValues values = new ContentValues();
        values.put(Genres.NAME, Audio1.GENRE);
        Uri uri = mContentResolver.insert(Genres.INTERNAL_CONTENT_URI, values);
        assertNull(uri);
    }
}
