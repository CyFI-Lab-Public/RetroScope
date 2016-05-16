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

package com.android.providers.partnerbookmarks;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.TestCase;

public class PartnerBookmarksProviderTest extends AndroidTestCase {
    private static final String TAG = "PartnerBookmarksProviderTest";
    private static final long FIXED_ID_PARTNER_BOOKMARKS_ROOT =
            PartnerBookmarksContract.Bookmarks.BOOKMARK_PARENT_ROOT_ID + 1;
    private static final long NO_FOLDER_FILTER = -1;

    private int countBookmarksInFolder(long folderFilter) throws android.os.RemoteException {
        ContentProviderClient providerClient =
                getContext().getContentResolver().acquireContentProviderClient(
                        PartnerBookmarksContract.Bookmarks.CONTENT_URI);
        assertNotNull(
                "Failed to acquire " + PartnerBookmarksContract.Bookmarks.CONTENT_URI,
                providerClient);
        Cursor cursor = null;
        try {
            cursor = providerClient.query(PartnerBookmarksContract.Bookmarks.CONTENT_URI,
                    null, null, null, null);
            assertNotNull("Failed to query for bookmarks", cursor);
            int bookmarksCount = 0;
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(PartnerBookmarksContract.Bookmarks.ID));
                long parent = cursor.getLong(
                        cursor.getColumnIndexOrThrow(PartnerBookmarksContract.Bookmarks.PARENT));
                boolean isFolder = cursor.getInt(
                        cursor.getColumnIndexOrThrow(PartnerBookmarksContract.Bookmarks.TYPE))
                                == PartnerBookmarksContract.Bookmarks.BOOKMARK_TYPE_FOLDER;
                String url = cursor.getString(
                        cursor.getColumnIndexOrThrow(PartnerBookmarksContract.Bookmarks.URL));
                String title = cursor.getString(
                        cursor.getColumnIndexOrThrow(PartnerBookmarksContract.Bookmarks.TITLE));
                assertFalse("bookmark id must be non-zero", id == 0);
                assertFalse("url should not be empty if not a folder", !isFolder && url.isEmpty());
                assertFalse("title should never be empty", title.isEmpty());
                if (folderFilter == NO_FOLDER_FILTER || folderFilter == parent) {
                    bookmarksCount++;
                }
            }
            return bookmarksCount;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            providerClient.release();
        }
    }

    @SmallTest
    public void testExactlyOneRoot() throws android.os.RemoteException {
        int totalBookmarks = countBookmarksInFolder(NO_FOLDER_FILTER);
        if (totalBookmarks > 0) {
            assertEquals("There must be at most one root",
                    countBookmarksInFolder(
                            PartnerBookmarksContract.Bookmarks.BOOKMARK_PARENT_ROOT_ID),
                    1);
        }
    }

    @SmallTest
    public void testRootMustBeNonEmptyIfPresent() throws android.os.RemoteException {
        int totalBookmarks = countBookmarksInFolder(NO_FOLDER_FILTER);
        if (totalBookmarks > 0) {
            assertTrue("If the root exists, it must be non-empty",
                    countBookmarksInFolder(FIXED_ID_PARTNER_BOOKMARKS_ROOT) > 0);
        }
    }

    @SmallTest
    public void testDefaultPBPSupportsOnlyFlatListOfBookmarks()
            throws android.os.RemoteException {
        int totalBookmarks = countBookmarksInFolder(NO_FOLDER_FILTER);
        if (totalBookmarks > 0) {
            assertEquals("Default PBP supports only flat list of bookmarks",
                    countBookmarksInFolder(FIXED_ID_PARTNER_BOOKMARKS_ROOT),
                    totalBookmarks - 1);
        }
    }
}
