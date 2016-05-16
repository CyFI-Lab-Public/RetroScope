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

package android.permission.cts;

import android.provider.Browser;
import android.provider.CallLog;
import android.provider.Contacts;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Tests Permissions related to reading from and writing to providers
 */
@MediumTest
public class ProviderPermissionTest extends AndroidTestCase {

    /**
     * Verify that read and write to contact requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#READ_CONTACTS}
     */
    public void testReadContacts() {
        assertReadingContentUriRequiresPermission(Contacts.People.CONTENT_URI,
                android.Manifest.permission.READ_CONTACTS);
    }

    /**
     * Verify that write to contact requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#WRITE_CONTACTS}
     */
    public void testWriteContacts() {
        assertWritingContentUriRequiresPermission(Contacts.People.CONTENT_URI,
                android.Manifest.permission.WRITE_CONTACTS);
    }

    /**
     * Verify that reading call logs requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#READ_CALL_LOG}
     */
    public void testReadCallLog() {
        assertReadingContentUriRequiresPermission(CallLog.CONTENT_URI,
                android.Manifest.permission.READ_CALL_LOG);
    }

    /**
     * Verify that writing call logs requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#WRITE_CALL_LOG}
     */
    public void testWriteCallLog() {
        assertWritingContentUriRequiresPermission(CallLog.CONTENT_URI,
                android.Manifest.permission.WRITE_CALL_LOG);
    }

    /**
     * Verify that write to settings requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#WRITE_SETTINGS}
     */
    public void testWriteSettings() {
        assertWritingContentUriRequiresPermission(android.provider.Settings.System.CONTENT_URI,
                android.Manifest.permission.WRITE_SETTINGS);
    }

    /**
     * Verify that read and write to browser bookmarks requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#READ_HISTORY_BOOKMARKS}
     */
    public void testReadBookmarks() {
        assertReadingContentUriRequiresPermission(Browser.BOOKMARKS_URI,
                android.Manifest.permission.READ_HISTORY_BOOKMARKS);
    }

    /**
     * Verify that read and write to browser bookmarks requires permissions.
     * <p>Tests Permission:
         {@link android.Manifest.permission#WRITE_HISTORY_BOOKMARKS}
     */
    public void testWriteBookmarks() {
        assertWritingContentUriRequiresPermission(Browser.BOOKMARKS_URI,
                android.Manifest.permission.WRITE_HISTORY_BOOKMARKS);
    }

    /**
     * Verify that read and write to browser history requires permissions.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#READ_HISTORY_BOOKMARKS}
     */
    public void testReadBrowserHistory() {
        assertReadingContentUriRequiresPermission(Browser.SEARCHES_URI,
                android.Manifest.permission.READ_HISTORY_BOOKMARKS);
    }

    /**
     * Verify that read and write to browser history requires permissions.
     * <p>Tests Permission:
         {@link android.Manifest.permission#WRITE_HISTORY_BOOKMARKS}
     */
    public void testWriteBrowserHistory() {
        assertWritingContentUriRequiresPermission(Browser.SEARCHES_URI,
                android.Manifest.permission.WRITE_HISTORY_BOOKMARKS);
    }
}

