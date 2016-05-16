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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.Browser;
import android.provider.Browser.BookmarkColumns;
import android.provider.Browser.SearchColumns;
import android.provider.BrowserContract;
import android.provider.BrowserContract.Bookmarks;
import android.provider.BrowserContract.History;
import android.test.ActivityInstrumentationTestCase2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BrowserTest extends ActivityInstrumentationTestCase2<BrowserStubActivity> {
    public BrowserTest() {
        super("com.android.cts.stub", BrowserStubActivity.class);
    }

    private Context mContext;
    private ContentResolver mContentResolver;
    private ContentProviderClient mProvider;
    private BrowserStubActivity mActivity;
    private boolean mMasterSyncEnabled;

    // the backup for the 2 tables which we will modify in test cases
    private ArrayList<ContentValues> mBookmarksBackup;
    private ArrayList<ContentValues> mSearchesBackup;

    private static final String ADD_BOOKMARK_CLASS_NAME =
            "com.android.browser.AddBookmarkPage";
    private static final String COMPOSE_MESSAGE_CLASS_NAME =
            "com.android.mms.ui.ComposeMessageActivity";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getInstrumentation().getTargetContext();
        mContentResolver = mContext.getContentResolver();
        mProvider = mContentResolver.acquireContentProviderClient(
                Browser.BOOKMARKS_URI.getAuthority());
        mBookmarksBackup = new ArrayList<ContentValues>();
        mSearchesBackup = new ArrayList<ContentValues>();

        // Disable sync
        mMasterSyncEnabled = ContentResolver.getMasterSyncAutomatically();
        ContentResolver.setMasterSyncAutomatically(false);

        // backup the current contents in database
        Cursor cursor = mProvider.query(Bookmarks.CONTENT_URI, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            String[] colNames = cursor.getColumnNames();
            while (!cursor.isAfterLast()) {
                ContentValues value = new ContentValues();

                for (int i = 0; i < colNames.length; i++) {
                    if (Bookmarks.PARENT_SOURCE_ID.equals(colNames[i])
                            || Bookmarks.INSERT_AFTER_SOURCE_ID.equals(colNames[i])
                            || Bookmarks.TYPE.equals(colNames[i])) {
                        // These aren't actual columns, so skip them in the backup
                        continue;
                    }
                    switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_BLOB:
                        value.put(colNames[i], cursor.getBlob(i));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        value.put(colNames[i], cursor.getFloat(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        value.put(colNames[i], cursor.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        value.put(colNames[i], cursor.getString(i));
                        break;
                    }
                }
                mBookmarksBackup.add(value);

                cursor.moveToNext();
            };
        }
        cursor.close();

        cursor = mProvider.query(Browser.SEARCHES_URI, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                ContentValues value = new ContentValues();

                value.put(SearchColumns._ID, cursor.getInt(0));
                value.put(SearchColumns.SEARCH, cursor.getString(1));
                value.put(SearchColumns.DATE, cursor.getLong(2));
                mSearchesBackup.add(value);

                cursor.moveToNext();
            };
        }
        cursor.close();

        Uri uri = Bookmarks.CONTENT_URI.buildUpon()
                .appendQueryParameter(BrowserContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        mProvider.delete(uri, null, null);
        mProvider.delete(Browser.SEARCHES_URI, null, null);
        mProvider.delete(History.CONTENT_URI, null, null);

        mActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        try {

            // clear all new contents added in test cases.
            Uri uri = Bookmarks.CONTENT_URI.buildUpon()
                .appendQueryParameter(BrowserContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
            mProvider.delete(uri, null, null);
            mProvider.delete(Browser.SEARCHES_URI, null, null);

            // recover the old backup contents
            for (ContentValues value : mBookmarksBackup) {
                mProvider.insert(uri, value);
            }

            for (ContentValues value : mSearchesBackup) {
                mProvider.insert(Browser.SEARCHES_URI, value);
            }

            // Re-enable sync
            ContentResolver.setMasterSyncAutomatically(mMasterSyncEnabled);
        } finally {
            super.tearDown();
        }
    }

    public void testAccessSearches() {
        final String searchString = "search string";
        final String searchStringAnother = "another search string";
        Cursor cursor;

        try {
            Browser.addSearchUrl(mContentResolver, searchString);
            cursor = mProvider.query(
                    Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    null, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(searchString,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            long oldDate = cursor.getLong(Browser.SEARCHES_PROJECTION_DATE_INDEX);
            cursor.close();

            Browser.addSearchUrl(mContentResolver, searchString);
            cursor = mProvider.query(Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    null, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            long date = cursor.getLong(Browser.SEARCHES_PROJECTION_DATE_INDEX);
            assertTrue(date > oldDate);
            assertEquals(searchString,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            cursor.close();

            Browser.addSearchUrl(mContentResolver, searchStringAnother);
            cursor = mProvider.query(Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    null, null, null, null);
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(searchString,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            cursor.moveToNext();
            assertEquals(searchStringAnother,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            cursor.close();

            Browser.clearSearches(mContentResolver);
            cursor = mProvider.query(
                    Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    null, null, null, null);
            assertEquals(0, cursor.getCount());
        } catch (RemoteException e) {
            fail("Unexpected RemoteException");
        }
    }

    public void testGetAllBookmarks() {
        Cursor cursor;
        final String bookmarkUrl1 = "www.bookmark1.com";
        final String bookmarkUrl2 = "www.bookmark2.com";
        final String historyUrl = "www.history.com";

        try {
            cursor = Browser.getAllBookmarks(mContentResolver);
            assertEquals(0, cursor.getCount());
            cursor.close();

            ContentValues value = new ContentValues();
            value.put(BookmarkColumns.URL, bookmarkUrl1);
            value.put(BookmarkColumns.BOOKMARK, 1);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            value.put(BookmarkColumns.URL, bookmarkUrl2);
            value.put(BookmarkColumns.BOOKMARK, 1);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            value.put(BookmarkColumns.URL, historyUrl);
            value.put(BookmarkColumns.BOOKMARK, 0);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            cursor = Browser.getAllBookmarks(mContentResolver);
            assertEquals(2, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(bookmarkUrl1, cursor.getString(0));
            cursor.moveToNext();
            assertEquals(bookmarkUrl2, cursor.getString(0));
        } catch (RemoteException e) {
            fail("unexpected RemoteException");
        }
    }

    public void testGetAllVisitedUrls() {
        Cursor cursor;
        final String visitedUrl1 = "www.visited1.com";
        final String visitedUrl2 = "www.visited2.com";
        final String visitedUrl3 = "www.visited3.com";

        try {
            cursor = Browser.getAllVisitedUrls(mContentResolver);
            assertEquals(0, cursor.getCount());
            cursor.close();

            ContentValues value = new ContentValues();
            value.put(BookmarkColumns.URL, visitedUrl1);
            value.put(BookmarkColumns.BOOKMARK, 1);
            value.put(BookmarkColumns.VISITS, 1);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            value.put(BookmarkColumns.URL, visitedUrl2);
            value.put(BookmarkColumns.BOOKMARK, 0);
            value.put(BookmarkColumns.VISITS, 5);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            value.put(BookmarkColumns.URL, visitedUrl3);
            value.put(BookmarkColumns.BOOKMARK, 1);
            value.put(BookmarkColumns.VISITS, 0);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            cursor = Browser.getAllVisitedUrls(mContentResolver);
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(visitedUrl1, cursor.getString(0));
            cursor.moveToNext();
            assertEquals(visitedUrl2, cursor.getString(0));
            cursor.moveToNext();
            assertEquals(visitedUrl3, cursor.getString(0));
        } catch (RemoteException e) {
            fail("unexpected RemoteException");
        }
    }

    public void testUpdateVisitedHistory() {
        Cursor cursor;
        final String visitedHistoryUrl = "www.visited-history.com";
        long oldTime = 0;

        try {
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(0, cursor.getCount());
            cursor.close();
            Browser.updateVisitedHistory(mContentResolver, visitedHistoryUrl, true);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(visitedHistoryUrl, cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
            assertEquals(visitedHistoryUrl,
                    cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
            assertEquals(0, cursor.getInt(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX));
            assertEquals(1, cursor.getInt(Browser.HISTORY_PROJECTION_VISITS_INDEX));
            oldTime = cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX);
            cursor.close();

            Browser.updateVisitedHistory(mContentResolver, visitedHistoryUrl, true);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(visitedHistoryUrl, cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
            assertEquals(visitedHistoryUrl,
                    cursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
            assertEquals(0, cursor.getInt(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX));
            assertEquals(2, cursor.getInt(Browser.HISTORY_PROJECTION_VISITS_INDEX));
            assertTrue(oldTime < cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();
        } catch (RemoteException e1) {
            fail("unexpected RemoteException");
        }
    }

    public void testAccessHistory() {
        Cursor cursor;
        // NOTE: this value must keep same with the Browser.MAX_HISTORY_COUNT.
        final int MAX_HISTORY_COUNT = 250;
        final String bookmarkUrl = "www.visited-bookmark.com";
        final String historyUrlPrefix = "www.visited-history";
        final String historyUrlPostfix = ".com";
        final long TIME_BASE = new Date().getTime() - 1000;
            try {
            assertFalse(Browser.canClearHistory(mContentResolver));
            Browser.clearHistory(mContentResolver);
            assertFalse(Browser.canClearHistory(mContentResolver));

            // The total number of the history table's rows: MAX_HISTORY_COUNT,
            // but there is 1 row is a bookmark.
            // The date of the history is from 1 ~ (MAX_HISTORY_COUNT - 1).
            ContentValues value = new ContentValues();
            for (int i = 1; i <= MAX_HISTORY_COUNT - 1; i++) {
                value.put(BookmarkColumns.URL, historyUrlPrefix + i + historyUrlPostfix);
                value.put(BookmarkColumns.BOOKMARK, 0);
                value.put(BookmarkColumns.DATE, i + TIME_BASE);
                mProvider.insert(Browser.BOOKMARKS_URI, value);
            }
            value.put(BookmarkColumns.URL, bookmarkUrl);
            value.put(BookmarkColumns.BOOKMARK, 1);
            value.put(BookmarkColumns.VISITS, 5);
            value.put(BookmarkColumns.DATE, new Date().getTime());
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            value.clear();
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(MAX_HISTORY_COUNT, cursor.getCount());
            cursor.close();
            Browser.truncateHistory(mContentResolver);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(MAX_HISTORY_COUNT, cursor.getCount());
            cursor.close();

            // Add more one history which is not a bookmark,
            // then the history rows except bookmark is MAX_HISTORY_COUNT.
            value.put(BookmarkColumns.URL, historyUrlPrefix
                    + MAX_HISTORY_COUNT + historyUrlPostfix);
            value.put(BookmarkColumns.BOOKMARK, 0);
            value.put(BookmarkColumns.DATE, MAX_HISTORY_COUNT + TIME_BASE);
            mProvider.insert(Browser.BOOKMARKS_URI, value);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(MAX_HISTORY_COUNT + 1, cursor.getCount());
            cursor.close();
            Browser.truncateHistory(mContentResolver);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, null, null);
            assertEquals(MAX_HISTORY_COUNT + 1 - Browser.TRUNCATE_N_OLDEST, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(Browser.TRUNCATE_N_OLDEST + 1 + TIME_BASE,
                    cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();

            // Delete specified history
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            int historyCountBeforeDelete = cursor.getCount();
            cursor.moveToLast();
            assertEquals(MAX_HISTORY_COUNT + TIME_BASE,
                    cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();
            Browser.deleteFromHistory(mContentResolver,
                    historyUrlPrefix + MAX_HISTORY_COUNT + historyUrlPostfix);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            int historyCountAfterDelete = cursor.getCount();
            assertEquals(historyCountBeforeDelete - 1, historyCountAfterDelete);
            cursor.moveToLast();
            assertEquals(MAX_HISTORY_COUNT - 1 + TIME_BASE,
                    cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();

            // Specify a url which is not existed in current table.
            historyCountBeforeDelete = historyCountAfterDelete;
            Browser.deleteFromHistory(mContentResolver, "delete a not-existed url");
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            historyCountAfterDelete = cursor.getCount();
            assertEquals(historyCountBeforeDelete, historyCountAfterDelete);
            cursor.close();

            // Specify the history in a time frame to be deleted
            historyCountBeforeDelete = historyCountAfterDelete;
            long begin = 6 + TIME_BASE;
            long end = 20 + TIME_BASE;
            Browser.deleteHistoryTimeFrame(mContentResolver, begin, end);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            historyCountAfterDelete = cursor.getCount();
            assertEquals(historyCountBeforeDelete - (end - begin), historyCountAfterDelete);
            cursor.moveToFirst();
            assertEquals(end, cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();

            // Specify the history in a time frame (not specify begin) to be deleted.
            historyCountBeforeDelete = historyCountAfterDelete;
            long firstDate = end;
            begin = -1;
            end = 34 + TIME_BASE;
            Browser.deleteHistoryTimeFrame(mContentResolver, begin, end);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            historyCountAfterDelete = cursor.getCount();
            assertEquals(historyCountBeforeDelete - (end - firstDate), historyCountAfterDelete);
            cursor.moveToFirst();
            assertEquals(end, cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.moveToLast();
            long lastDate = cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX);
            cursor.close();

            // Specify the history in a time frame (not specify end) to be deleted.
            historyCountBeforeDelete = historyCountAfterDelete;
            begin = MAX_HISTORY_COUNT - 10 + TIME_BASE;
            end = -1;
            Browser.deleteHistoryTimeFrame(mContentResolver, begin, end);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    BookmarkColumns.BOOKMARK + " = 0",
                    null, BookmarkColumns.DATE, null);
            historyCountAfterDelete = cursor.getCount();
            assertEquals(historyCountBeforeDelete - (lastDate - begin + 1),
                    historyCountAfterDelete);
            cursor.moveToLast();
            assertEquals(begin - 1, cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();

            // Clear all history.
            assertTrue(Browser.canClearHistory(mContentResolver));
            Browser.clearHistory(mContentResolver);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    Browser.HISTORY_PROJECTION,
                    null, null, BookmarkColumns.DATE, null);
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(bookmarkUrl, cursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX));
            assertEquals(0, cursor.getInt(Browser.HISTORY_PROJECTION_VISITS_INDEX));
            assertEquals(0, cursor.getLong(Browser.HISTORY_PROJECTION_DATE_INDEX));
            cursor.close();
            assertFalse(Browser.canClearHistory(mContentResolver));
        } catch (RemoteException e) {
            fail("unexpected RemoteException");
        }
    }

    public void testRequestAllIcons() {
        Browser.requestAllIcons(mContentResolver, null, null);
    }

    public void testSaveBookmark() {
        // TODO: send KEYCODE_DPAD_CENTER to skip the resolve page, but no effect.
//        assertFalse(isRunning(ADD_BOOKMARK_CLASS_NAME));
//        Browser.saveBookmark(mActivity, "bookmark title", "www.bookmark.com");
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
//        assertStarted(ADD_BOOKMARK_CLASS_NAME, 6000);

        // TODO: how to finish the activity.
    }

    public void testSendString() {
        // assertFalse(isRunning(COMPOSE_MESSAGE_CLASS_NAME));
        // Browser.sendString(mActivity, "string to be sent");
        // assertStarted(COMPOSE_MESSAGE_CLASS_NAME, 5000);

        // TODO: how to finish the activity.
    }

    private boolean isRunning(String className) {
        ActivityManager activityManager = (ActivityManager) mActivity
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = activityManager.getRunningTasks(1);
        RunningTaskInfo info = list.get(0);
        if (null == info || null == info.topActivity) {
            return false;
        }

        if (className.equals(info.topActivity.getClassName())) {
            return true;
        }

        return false;
    }

    private void assertStarted(String className, long timeout) {
        final long timeSlice = 200;
        while (timeout > 0) {
            try {
                Thread.sleep(timeSlice);
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException");
            }
            if (isRunning(className)) {
                return;
            }
            timeout -= timeSlice;
        }
        fail("has not started BrowserActivity yet");
    }

    /**
     * Test case just for the actual content provider behavior on Bookmarks table.
     * It does not test any APIs in android.provider.Browser.java, so we cannot add
     * annotation for it.
     */
    public void testBookmarksTable() {
        final String[] BOOKMARKS_PROJECTION = new String[] {
                BookmarkColumns._ID, BookmarkColumns.URL, BookmarkColumns.VISITS,
                BookmarkColumns.DATE, BookmarkColumns.CREATED, BookmarkColumns.BOOKMARK,
                BookmarkColumns.TITLE, BookmarkColumns.FAVICON };
        final int ID_INDEX = 0;
        final int URL_INDEX = 1;
        final int VISITS_INDEX = 2;
        final int DATE_INDEX = 3;
        final int CREATED_INDEX = 4;
        final int BOOKMARK_INDEX = 5;
        final int TITLE_INDEX = 6;
        final int FAVICON_INDEX = 7;

        String insertBookmarkTitle = "bookmark_insert";
        String insertBookmarkUrl = "www.bookmark_insert.com";

        String updateBookmarkTitle = "bookmark_update";
        String updateBookmarkUrl = "www.bookmark_update.com";
        try {
            // Test: insert
            ContentValues value = new ContentValues();
            long createDate = new Date().getTime();
            value.put(BookmarkColumns.TITLE, insertBookmarkTitle);
            value.put(BookmarkColumns.URL, insertBookmarkUrl);
            value.put(BookmarkColumns.VISITS, 0);
            value.put(BookmarkColumns.DATE, createDate);
            value.put(BookmarkColumns.CREATED, createDate);
            value.put(BookmarkColumns.BOOKMARK, 0);

            Uri insertUri = mProvider.insert(Browser.BOOKMARKS_URI, value);
            Cursor cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    BOOKMARKS_PROJECTION,
                    BookmarkColumns.TITLE + " = ?",
                    new String[] {insertBookmarkTitle},
                    BookmarkColumns.DATE, null);
            assertTrue(cursor.moveToNext());
            assertEquals(insertBookmarkTitle, cursor.getString(TITLE_INDEX));
            assertEquals(insertBookmarkUrl,cursor.getString(URL_INDEX));
            assertEquals(0,cursor.getInt(VISITS_INDEX));
            assertEquals(createDate, cursor.getLong(DATE_INDEX));
            assertEquals(createDate, cursor.getLong(CREATED_INDEX));
            assertEquals(0, cursor.getInt(BOOKMARK_INDEX));
            assertNull(cursor.getBlob(FAVICON_INDEX));
            int Id = cursor.getInt(ID_INDEX);
            cursor.close();

            // Test: update
            value.clear();
            long updateDate = new Date().getTime();
            value.put(BookmarkColumns.TITLE, updateBookmarkTitle);
            value.put(BookmarkColumns.URL, updateBookmarkUrl);
            value.put(BookmarkColumns.VISITS, 1);
            value.put(BookmarkColumns.DATE, updateDate);

            mProvider.update(Browser.BOOKMARKS_URI, value,
                    BookmarkColumns.TITLE + " = ?",
                    new String[] {insertBookmarkTitle});
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    BOOKMARKS_PROJECTION,
                    BookmarkColumns._ID + " = " + Id,
                    null, null, null);
            assertTrue(cursor.moveToNext());
            assertEquals(updateBookmarkTitle, cursor.getString(TITLE_INDEX));
            assertEquals(updateBookmarkUrl,cursor.getString(URL_INDEX));
            assertEquals(1,cursor.getInt(VISITS_INDEX));
            assertEquals(updateDate, cursor.getLong(DATE_INDEX));
            assertEquals(createDate, cursor.getLong(CREATED_INDEX));
            assertEquals(0, cursor.getInt(BOOKMARK_INDEX));
            assertNull(cursor.getBlob(FAVICON_INDEX));
            assertEquals(Id, cursor.getInt(ID_INDEX));

            // Test: delete
            mProvider.delete(insertUri, null, null);
            cursor = mProvider.query(
                    Browser.BOOKMARKS_URI,
                    BOOKMARKS_PROJECTION,
                    BookmarkColumns._ID + " = " + Id,
                    null, null, null);
            assertEquals(0, cursor.getCount());
        } catch (RemoteException e) {
            fail("Unexpected RemoteException");
        }
    }

    /**
     * Test case just for the actual content provider behavior on Searches table.
     * It does not test any APIs in android.provider.Browser.java, so we cannot add
     * annotation for it.
     */
    public void testSearchesTable() {
        final int ID_INDEX = 0;
        String insertSearch = "search_insert";
        String updateSearch = "search_update";

        try {
            // Test: insert
            ContentValues value = new ContentValues();
            long createDate = new Date().getTime();
            value.put(SearchColumns.SEARCH, insertSearch);
            value.put(SearchColumns.DATE, createDate);

            Uri insertUri = mProvider.insert(Browser.SEARCHES_URI, value);
            Cursor cursor = mProvider.query(Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION, SearchColumns.SEARCH + " = ?",
                    new String[] {insertSearch}, null, null);
            assertTrue(cursor.moveToNext());
            assertEquals(insertSearch,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            assertEquals(createDate,
                    cursor.getLong(Browser.SEARCHES_PROJECTION_DATE_INDEX));
            int Id = cursor.getInt(ID_INDEX);
            cursor.close();

            // Test: update
            value.clear();
            long updateDate = new Date().getTime();
            value.put(SearchColumns.SEARCH, updateSearch);
            value.put(SearchColumns.DATE, updateDate);

            mProvider.update(Browser.SEARCHES_URI, value,
                    SearchColumns._ID + " = " + Id, null);
            cursor = mProvider.query(Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    SearchColumns._ID + " = " + Id, null, null, null);
            assertTrue(cursor.moveToNext());
            assertEquals(updateSearch,
                    cursor.getString(Browser.SEARCHES_PROJECTION_SEARCH_INDEX));
            assertEquals(updateDate,
                    cursor.getLong(Browser.SEARCHES_PROJECTION_DATE_INDEX));
            assertEquals(Id, cursor.getInt(ID_INDEX));

            // Test: delete
            mProvider.delete(insertUri, null, null);
            cursor = mProvider.query(Browser.SEARCHES_URI,
                    Browser.SEARCHES_PROJECTION,
                    SearchColumns._ID + " = " + Id, null, null, null);
            assertEquals(0, cursor.getCount());
        } catch (RemoteException e) {
            fail("Unexpected RemoteException");
        }
    }
}
