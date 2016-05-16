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

package com.android.providers.downloads.public_api_access_tests;

import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.Downloads;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * DownloadProvider allows apps without permission ACCESS_DOWNLOAD_MANAGER to access it -- this is
 * how the public API works.  But such access is subject to strict constraints on what can be
 * inserted.  This test suite checks those constraints.
 */
@MediumTest
public class PublicApiAccessTest extends AndroidTestCase {
    private static final String[] DISALLOWED_COLUMNS = new String[] {
                    Downloads.Impl.COLUMN_COOKIE_DATA,
                    Downloads.Impl.COLUMN_REFERER,
                    Downloads.Impl.COLUMN_USER_AGENT,
                    Downloads.Impl.COLUMN_NO_INTEGRITY,
                    Downloads.Impl.COLUMN_NOTIFICATION_CLASS,
                    Downloads.Impl.COLUMN_NOTIFICATION_EXTRAS,
                    Downloads.Impl.COLUMN_OTHER_UID,
                    Downloads.Impl.COLUMN_APP_DATA,
                    Downloads.Impl.COLUMN_CONTROL,
                    Downloads.Impl.COLUMN_STATUS,
            };

    private ContentResolver mContentResolver;
    private DownloadManager mManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getContext().getContentResolver();
        mManager = new DownloadManager(mContentResolver, getContext().getPackageName());
    }

    @Override
    protected void tearDown() throws Exception {
        if (mContentResolver != null) {
            mContentResolver.delete(Downloads.Impl.CONTENT_URI, null, null);
        }
        super.tearDown();
    }

    public void testMinimalValidWrite() {
        mContentResolver.insert(Downloads.Impl.CONTENT_URI, buildValidValues());
    }

    public void testMaximalValidWrite() {
        ContentValues values = buildValidValues();
        values.put(Downloads.Impl.COLUMN_TITLE, "foo");
        values.put(Downloads.Impl.COLUMN_DESCRIPTION, "foo");
        values.put(Downloads.Impl.COLUMN_MIME_TYPE, "foo");
        values.put(Downloads.Impl.COLUMN_NOTIFICATION_PACKAGE, "foo");
        values.put(Downloads.Impl.COLUMN_ALLOWED_NETWORK_TYPES, 0);
        values.put(Downloads.Impl.COLUMN_ALLOW_ROAMING, true);
        values.put(Downloads.Impl.RequestHeaders.INSERT_KEY_PREFIX + "0", "X-Some-Header: value");
        mContentResolver.insert(Downloads.Impl.CONTENT_URI, values);
    }

    private ContentValues buildValidValues() {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_URI, "foo");
        values.put(Downloads.Impl.COLUMN_DESTINATION,
                Downloads.Impl.DESTINATION_CACHE_PARTITION_PURGEABLE);
        values.put(Downloads.Impl.COLUMN_VISIBILITY, Downloads.Impl.VISIBILITY_VISIBLE);
        values.put(Downloads.Impl.COLUMN_IS_PUBLIC_API, true);
        return values;
    }

    public void testNoPublicApi() {
        ContentValues values = buildValidValues();
        values.remove(Downloads.Impl.COLUMN_IS_PUBLIC_API);
        testInvalidValues(values);
    }

    public void testInvalidDestination() {
        ContentValues values = buildValidValues();
        values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_EXTERNAL);
        testInvalidValues(values);
        values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_CACHE_PARTITION);
        testInvalidValues(values);
    }

    public void testInvalidVisibility() {
        ContentValues values = buildValidValues();
        values.put(Downloads.Impl.COLUMN_VISIBILITY,
                Downloads.Impl.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        testInvalidValues(values);

        values.put(Downloads.Impl.COLUMN_VISIBILITY, Downloads.Impl.VISIBILITY_HIDDEN);
        testInvalidValues(values);

        values.remove(Downloads.Impl.COLUMN_VISIBILITY);
        testInvalidValues(values);
    }

    public void testDisallowedColumns() {
        for (String column : DISALLOWED_COLUMNS) {
            ContentValues values = buildValidValues();
            values.put(column, 1);
            testInvalidValues(values);
        }
    }

    public void testFileUriWithoutExternalPermission() {
        ContentValues values = buildValidValues();
        values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_FILE_URI);
        values.put(Downloads.Impl.COLUMN_FILE_NAME_HINT, "file:///sdcard/foo");
        testInvalidValues(values);
    }

    private void testInvalidValues(ContentValues values) {
        try {
            mContentResolver.insert(Downloads.Impl.CONTENT_URI, values);
            fail("Didn't get SecurityException as expected");
        } catch (SecurityException exc) {
            // expected
        }
    }

    public void testDownloadManagerRequest() {
        // first try a minimal request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://localhost/path"));
        mManager.enqueue(request);

        // now set everything we can, save for external destintion (for which we lack permission)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        request.setAllowedOverRoaming(false);
        request.setTitle("test");
        request.setDescription("test");
        request.setMimeType("text/html");
        request.addRequestHeader("X-Some-Header", "value");
        mManager.enqueue(request);
    }
}
