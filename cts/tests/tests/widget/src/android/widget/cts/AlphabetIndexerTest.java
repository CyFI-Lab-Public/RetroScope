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

package android.widget.cts;


import android.database.Cursor;
import android.database.MatrixCursor;
import android.test.AndroidTestCase;
import android.widget.AlphabetIndexer;

public class AlphabetIndexerTest extends AndroidTestCase {
    private static final String[] COUNTRIES_LIST = new String[]
        {"Argentina", "Australia", "China", "France", "Germany", "Italy", "Japan", "United States"};
    private static final String[] NAMES_LIST = new String[]
        {"Andy", "Bergkamp", "David", "Jacky", "Kevin", "Messi", "Michael", "Steven"};
    private static final String ALPHABET = " ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SORTED_COLUMN_INDEX = 1;

    private static final int INDEX_OF_ARGENTINA = 0;
    private static final int INDEX_OF_CHINA = 2;
    private static final int INDEX_OF_UNITED_STATES = 7;

    private static final int INDEX_OF_BERGKAMP = 1;
    private static final int INDEX_OF_MESSI = 5;
    private static final int INDEX_OF_STEVEN = 7;

    public void testAlphabetIndexer() {
        Cursor c1 = createCursor("Country", COUNTRIES_LIST);

        AlphabetIndexer indexer = new AlphabetIndexer(c1, SORTED_COLUMN_INDEX, ALPHABET);

        // test getSections
        Object[] sections = indexer.getSections();
        assertTrue(sections instanceof String[]);
        assertEquals(ALPHABET.length(), sections.length);
        assertEquals(ALPHABET.charAt(0), ((String[]) sections)[0].charAt(0));
        assertEquals(ALPHABET.charAt(1), ((String[]) sections)[1].charAt(0));
        assertEquals(ALPHABET.charAt(ALPHABET.length() - 2),
                ((String[]) sections)[ALPHABET.length() - 2].charAt(0));
        assertEquals(ALPHABET.charAt(ALPHABET.length() - 1),
                ((String[]) sections)[ALPHABET.length() - 1].charAt(0));

        // test getPositionForSection
        // search for 'A'
        int index = ALPHABET.indexOf('A');
        assertEquals(INDEX_OF_ARGENTINA, indexer.getPositionForSection(index));

        // search for 'C'
        index = ALPHABET.indexOf('C');
        assertEquals(INDEX_OF_CHINA, indexer.getPositionForSection(index));

        // search for 'T', and it should search for "United States"
        index = ALPHABET.indexOf('T');
        assertEquals(INDEX_OF_UNITED_STATES, indexer.getPositionForSection(index));

        // search for 'X', return the length
        index = ALPHABET.indexOf('X');
        assertEquals(COUNTRIES_LIST.length, indexer.getPositionForSection(index));

        // test getSectionForPosition
        assertEquals(ALPHABET.indexOf('A'), indexer.getSectionForPosition(0));
        assertEquals(ALPHABET.indexOf('C'), indexer.getSectionForPosition(2));
        assertEquals(ALPHABET.indexOf('G'), indexer.getSectionForPosition(4));
        assertEquals(ALPHABET.indexOf('J'), indexer.getSectionForPosition(6));

        // test setCursor
        Cursor c2 = createCursor("Name", NAMES_LIST);

        indexer.setCursor(c2);

        // test getPositionForSection in Name Cursor
        // search for 'B'
        index = ALPHABET.indexOf('B');
        assertEquals(INDEX_OF_BERGKAMP, indexer.getPositionForSection(index));

        // search for 'M'
        index = ALPHABET.indexOf('M');
        assertEquals(INDEX_OF_MESSI, indexer.getPositionForSection(index));

        // search for 'P', and it should search for "Steven"
        index = ALPHABET.indexOf('P');
        assertEquals(INDEX_OF_STEVEN, indexer.getPositionForSection(index));

        // search for 'T', return the length
        index = ALPHABET.indexOf('T');
        assertEquals(NAMES_LIST.length, indexer.getPositionForSection(index));
    }

    public void testCompare() {
        Cursor cursor = createCursor("Country", COUNTRIES_LIST);

        MyAlphabetIndexer indexer = new MyAlphabetIndexer(cursor, SORTED_COLUMN_INDEX, ALPHABET);
        assertEquals(0, indexer.compare("Golfresort", "G"));
        assertTrue(indexer.compare("Golfresort", "F") > 0);
        assertTrue(indexer.compare("Golfresort", "H") < 0);
    }

    @SuppressWarnings("unchecked")
    private Cursor createCursor(String listName, String[] listData) {
        String[] columns = { "_id", listName };

        MatrixCursor cursor = new MatrixCursor(columns, listData.length);
        for (int i = 0; i < listData.length; i++) {
            cursor.addRow(new Object[] { i, listData[i] });
        }
        return cursor;
    }

    /**
     * MyAlphabetIndexer for test
     */
    private static class MyAlphabetIndexer extends AlphabetIndexer {
        public MyAlphabetIndexer(Cursor cursor, int sortedColumnIndex, CharSequence alphabet) {
            super(cursor, sortedColumnIndex, alphabet);
        }

        protected int compare(String word, String letter) {
            return super.compare(word, letter);
        }
    }
}

