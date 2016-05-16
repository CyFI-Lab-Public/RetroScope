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
import android.database.Cursor;
import android.net.Uri;
import android.provider.UserDictionary;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Locale;

public class UserDictionary_WordsTest extends AndroidTestCase {

    private ContentResolver mContentResolver;
    private ArrayList<Uri> mAddedBackup;

    private static final String[] WORDS_PROJECTION = new String[] {
            UserDictionary.Words._ID,
            UserDictionary.Words.WORD,
            UserDictionary.Words.FREQUENCY,
            UserDictionary.Words.LOCALE };

    private static final int ID_INDEX = 0;
    private static final int WORD_INDEX = 1;
    private static final int FREQUENCY_INDEX = 2;
    private static final int LOCALE_INDEX = 3;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = mContext.getContentResolver();
        mAddedBackup = new ArrayList<Uri>();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Uri row : mAddedBackup) {
            mContentResolver.delete(row, null, null);
        }
        mAddedBackup.clear();

        super.tearDown();
    }

    @SuppressWarnings("deprecation")
    public void testAddWord_deprecated() throws Exception {
        String word = "UserDictionary_WordsTest";
        int frequency = 1;
        UserDictionary.Words.addWord(getContext(), word, frequency,
                UserDictionary.Words.LOCALE_TYPE_ALL);

        Cursor cursor = mContentResolver.query(UserDictionary.Words.CONTENT_URI, WORDS_PROJECTION,
                UserDictionary.Words.WORD + "='" + word + "'", null, null);
        assertTrue(cursor.moveToFirst());
        mAddedBackup.add(Uri.withAppendedPath(UserDictionary.Words.CONTENT_URI,
                cursor.getString(ID_INDEX)));

        assertEquals(1, cursor.getCount());
        assertEquals(word, cursor.getString(WORD_INDEX));
        assertEquals(frequency, cursor.getInt(FREQUENCY_INDEX));
        assertNull(cursor.getString(LOCALE_INDEX));
        cursor.close();
    }

    public void testAddWord() throws Exception {
        assertWord("testWord1", 42, null, Locale.KOREA, 42);
        assertWord("testWord2", -3007, "tw2", Locale.JAPAN, 0);
        assertWord("testWord3", 1337, "tw3", Locale.US, 255);
    }

    private void assertWord(String word, int frequency, String shortcut, Locale locale,
            int expectedFrequency) {

        UserDictionary.Words.addWord(mContext, word, frequency, shortcut, locale);

        Cursor cursor = mContentResolver.query(UserDictionary.Words.CONTENT_URI, WORDS_PROJECTION,
                UserDictionary.Words.WORD + "='" + word + "'", null, null);
        assertTrue(cursor.moveToFirst());
        mAddedBackup.add(Uri.withAppendedPath(UserDictionary.Words.CONTENT_URI,
                cursor.getString(ID_INDEX)));

        assertEquals(1, cursor.getCount());
        assertEquals(word, cursor.getString(WORD_INDEX));
        assertEquals(expectedFrequency, cursor.getInt(FREQUENCY_INDEX));
        assertEquals(locale.toString(), cursor.getString(LOCALE_INDEX));
        cursor.close();
    }
}
