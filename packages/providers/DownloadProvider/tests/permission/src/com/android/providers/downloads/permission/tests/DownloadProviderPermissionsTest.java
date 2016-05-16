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

package com.android.providers.downloads.permission.tests;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.provider.Downloads;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

/**
 * Verify that protected Download provider actions require specific permissions.
 *
 * TODO: consider adding test where app has ACCESS_DOWNLOAD_MANAGER, but not
 * ACCESS_DOWNLOAD_MANAGER_ADVANCED
 */
public class DownloadProviderPermissionsTest extends AndroidTestCase {

    private ContentResolver mContentResolver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getContext().getContentResolver();
    }

    /**
     * Test that an app cannot access the /cache filesystem
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#ACCESS_CACHE_FILESYSTEM}
     */
    @MediumTest
    public void testAccessCacheFilesystem() throws IOException {
        try {
            String filePath = "/cache/this-should-not-exist.txt";
            FileOutputStream strm = new FileOutputStream(filePath);
            strm.write("Oops!".getBytes());
            strm.flush();
            strm.close();
            fail("Was able to create and write to " + filePath);
        } catch (SecurityException e) {
            // expected
        } catch (FileNotFoundException e) {
            // also could be expected
        }
    }

    /**
     * Test that an untrusted app cannot write to the download provider
     * <p>Tests Permission:
     *   {@link com.android.providers.downloads.Manifest.permission#ACCESS_DOWNLOAD_MANAGER}
     *   and
     *   {@link android.Manifest.permission#INTERNET}
     */
    @MediumTest
    public void testWriteDownloadProvider() {
        try {
            ContentValues values = new ContentValues();
            values.put(Downloads.Impl.COLUMN_URI, "foo");
            mContentResolver.insert(Downloads.Impl.CONTENT_URI, values);
            fail("write to provider did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Test that an untrusted app cannot access the download service
     * <p>Tests Permission:
     *   {@link com.android.providers.downloads.Manifest.permission#ACCESS_DOWNLOAD_MANAGER}
     */
    @MediumTest
    public void testStartDownloadService() {
        try {
            Intent downloadServiceIntent = new Intent();
            downloadServiceIntent.setClassName("com.android.providers.downloads",
                    "com.android.providers.downloads.DownloadService");
            getContext().startService(downloadServiceIntent);
            fail("starting download service did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }
}
