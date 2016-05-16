/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.content.Context;
import android.cts.util.PollingCheck;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchRecentSuggestions;
import android.test.ProviderTestCase2;

public class SearchRecentSuggestionsTest extends
        ProviderTestCase2<TestSearchRecentSuggestionsProvider> {
    private final static String AUTHORITY_HEAD = "content://"
            + TestSearchRecentSuggestionsProvider.AUTHORITY;

    private Uri mTestUri;
    private TestSearchRecentSuggestionsProvider mTestSRSProvider;
    private Context mProviderContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mTestUri = Uri.parse(AUTHORITY_HEAD + "/suggestions");
        mTestSRSProvider = getProvider();
        mProviderContext = mTestSRSProvider.getContext();
    }

    public SearchRecentSuggestionsTest() {
        super(TestSearchRecentSuggestionsProvider.class,
                TestSearchRecentSuggestionsProvider.AUTHORITY);
    }

    public void testConstructor() {
        new SearchRecentSuggestions(mProviderContext, TestSearchRecentSuggestionsProvider.AUTHORITY,
                TestSearchRecentSuggestionsProvider.MODE);
    }

    public void testSearchRecentSuggestions() {
        SearchRecentSuggestions srs = new MySearchRecentSuggestions(mProviderContext,
                TestSearchRecentSuggestionsProvider.AUTHORITY,
                TestSearchRecentSuggestionsProvider.MODE);
        Cursor c = mTestSRSProvider.query(mTestUri, null, null, null, null);

        try {
            assertNotNull(c);
            assertEquals(0, c.getCount());
            c.close();

            // insert three rows
            String query1 = "query1";
            String line1 = "line1";
            srs.saveRecentQuery(query1, line1);

            waitForCursorCount(mTestUri, SearchRecentSuggestions.QUERIES_PROJECTION_2LINE, 1);

            c = mTestSRSProvider.query(mTestUri, SearchRecentSuggestions.QUERIES_PROJECTION_2LINE,
                    null, null, null);
            c.moveToFirst();
            assertEquals(query1, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_QUERY_INDEX));
            assertEquals(line1, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_DISPLAY2_INDEX));
            c.close();

            String query2 = "query2";
            String line2 = "line2";
            srs.saveRecentQuery(query2, line2);
            waitForCursorCount(mTestUri, null, 2);

            String query3 = "query3";
            String line3 = "line3";
            srs.saveRecentQuery(query3, line3);
            waitForCursorCount(mTestUri, null, 3);

            // truncateHistory will delete the oldest one record
            ContentResolver cr = mProviderContext.getContentResolver();
            ((MySearchRecentSuggestions) srs).truncateHistory(cr, 2);

            waitForCursorCount(mTestUri, SearchRecentSuggestions.QUERIES_PROJECTION_2LINE, 2);

            c = mTestSRSProvider.query(mTestUri, SearchRecentSuggestions.QUERIES_PROJECTION_2LINE,
                    null, null, null);

            // and the left two should be: test2 and test3, test1 should be delete
            c.moveToFirst();
            assertEquals(query2, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_QUERY_INDEX));
            assertEquals(line2, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_DISPLAY2_INDEX));
            c.moveToNext();
            assertEquals(query3, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_QUERY_INDEX));
            assertEquals(line3, c
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_DISPLAY2_INDEX));
            c.close();

            // clear all history
            srs.clearHistory();
            waitForCursorCount(mTestUri, null, 0);
        } finally {
            c.close();
        }
    }

    public void testSuggestionsTable() {
        String insertDisplay1 = "display1_insert";
        String insertDisplay2 = "display2_insert";
        String insertQuery = "query_insert";

        String updateDisplay1 = "display1_update";
        String updateDisplay2 = "display2_update";
        String updateQuery = "query_update";

        // Test: insert
        ContentValues value = new ContentValues();
        value.put("display1", insertDisplay1);
        value.put("display2", insertDisplay2);
        value.put("query", insertQuery);
        value.put("date", 1);

        mTestSRSProvider.insert(mTestUri, value);

        Cursor cursor = mTestSRSProvider.query(mTestUri,
                SearchRecentSuggestions.QUERIES_PROJECTION_2LINE,
                "display1=\"" + insertDisplay1 + "\"", null, null);
        try {
            assertNotNull(cursor);
            assertEquals(1, cursor.getCount());
            assertTrue(cursor.moveToFirst());
            assertEquals(insertDisplay2, cursor
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_DISPLAY2_INDEX));
            assertEquals(insertQuery, cursor
                    .getString(SearchRecentSuggestions.QUERIES_PROJECTION_QUERY_INDEX));
            assertEquals(1, cursor.getInt(SearchRecentSuggestions.QUERIES_PROJECTION_DATE_INDEX));
            cursor.close();

            // Test: update
            /**
             * SearchRecentSuggestionsProvider.update is not implement, always
             * throw an UnsupportedOperationException.
             */
            value.clear();
            value.put("display1", updateDisplay1);
            value.put("display2", updateDisplay2);
            value.put("query", updateQuery);
            value.put("date", 2);

            try {
                mTestSRSProvider.update(mTestUri, value, "display1=\"" + insertDisplay1 + "\"",
                        null);
                fail("There should be an UnsupportedOperationException thrown out.");
            } catch (UnsupportedOperationException e) {
                // expected, test success.
            }

            // Test: delete
            mTestSRSProvider.delete(mTestUri, "display1=\"" + insertDisplay1 + "\"", null);
            cursor = mTestSRSProvider.query(mTestUri,
                    SearchRecentSuggestions.QUERIES_PROJECTION_2LINE, "display1=\""
                            + insertDisplay1 + "\"", null, null);
            assertNotNull(cursor);
            assertEquals(0, cursor.getCount());
        } finally {
            cursor.close();
        }
    }

    private class MySearchRecentSuggestions extends SearchRecentSuggestions {
        public MySearchRecentSuggestions(Context context, String authority, int mode) {
            super(context, authority, mode);
        }

        @Override
        protected void truncateHistory(ContentResolver cr, int maxEntries) {
            super.truncateHistory(cr, maxEntries);
        }
    }

    private void waitForCursorCount(final Uri uri, final String[] projection,
            final int expectedCount) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                Cursor cursor = null;
                try {
                    cursor = mTestSRSProvider.query(uri, projection, null, null, null);
                    return cursor != null && cursor.getCount() == expectedCount;
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }.run();
    }
}
