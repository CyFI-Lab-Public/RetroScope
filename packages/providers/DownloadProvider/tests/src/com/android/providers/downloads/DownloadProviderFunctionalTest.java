/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.providers.downloads;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static java.net.HttpURLConnection.HTTP_OK;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Downloads;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * This test exercises the entire download manager working together -- it requests downloads through
 * the {@link DownloadProvider}, just like a normal client would, and runs the
 * {@link DownloadService} with start intents.  It sets up a {@link MockWebServer} running on the
 * device to serve downloads.
 */
@LargeTest
public class DownloadProviderFunctionalTest extends AbstractDownloadProviderFunctionalTest {
    private static final String TAG = "DownloadManagerFunctionalTest";

    public DownloadProviderFunctionalTest() {
        super(new FakeSystemFacade());
    }

    public void testDownloadTextFile() throws Exception {
        enqueueResponse(buildResponse(HTTP_OK, FILE_CONTENT));

        String path = "/download_manager_test_path";
        Uri downloadUri = requestDownload(path);
        assertEquals(Downloads.Impl.STATUS_PENDING, getDownloadStatus(downloadUri));
        assertTrue(mTestContext.mHasServiceBeenStarted);

        runUntilStatus(downloadUri, Downloads.Impl.STATUS_SUCCESS);
        RecordedRequest request = takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals(path, request.getPath());
        assertEquals(FILE_CONTENT, getDownloadContents(downloadUri));
        assertStartsWith(Environment.getExternalStorageDirectory().getPath(),
                         getDownloadFilename(downloadUri));
    }

    public void testDownloadToCache() throws Exception {
        enqueueResponse(buildResponse(HTTP_OK, FILE_CONTENT));

        Uri downloadUri = requestDownload("/path");
        updateDownload(downloadUri, Downloads.Impl.COLUMN_DESTINATION,
                       Integer.toString(Downloads.Impl.DESTINATION_CACHE_PARTITION));
        runUntilStatus(downloadUri, Downloads.Impl.STATUS_SUCCESS);
        assertEquals(FILE_CONTENT, getDownloadContents(downloadUri));
        assertStartsWith(getContext().getCacheDir().getAbsolutePath(),
                         getDownloadFilename(downloadUri));
    }

    public void testRoaming() throws Exception {
        enqueueResponse(buildResponse(HTTP_OK, FILE_CONTENT));
        enqueueResponse(buildResponse(HTTP_OK, FILE_CONTENT));

        mSystemFacade.mActiveNetworkType = ConnectivityManager.TYPE_MOBILE;
        mSystemFacade.mIsRoaming = true;

        // for a normal download, roaming is fine
        Uri downloadUri = requestDownload("/path");
        runUntilStatus(downloadUri, Downloads.Impl.STATUS_SUCCESS);

        // when roaming is disallowed, the download should pause...
        downloadUri = requestDownload("/path");
        updateDownload(downloadUri, Downloads.Impl.COLUMN_DESTINATION,
                       Integer.toString(Downloads.Impl.DESTINATION_CACHE_PARTITION_NOROAMING));
        runUntilStatus(downloadUri, Downloads.Impl.STATUS_WAITING_FOR_NETWORK);

        // ...and pick up when we're off roaming
        mSystemFacade.mIsRoaming = false;
        runUntilStatus(downloadUri, Downloads.Impl.STATUS_SUCCESS);
    }

    /**
     * Read a downloaded file from disk.
     */
    private String getDownloadContents(Uri downloadUri) throws Exception {
        InputStream inputStream = mResolver.openInputStream(downloadUri);
        try {
            return readStream(inputStream);
        } finally {
            inputStream.close();
        }
    }

    private void runUntilStatus(Uri downloadUri, int expected) throws Exception {
        startService(null);
        
        int actual = -1;

        final long timeout = SystemClock.elapsedRealtime() + (15 * SECOND_IN_MILLIS);
        while (SystemClock.elapsedRealtime() < timeout) {
            actual = getDownloadStatus(downloadUri);
            if (expected == actual) {
                return;
            }

            SystemClock.sleep(100);
        }

        throw new TimeoutException("Expected status " + expected + "; only reached " + actual);
    }

    protected int getDownloadStatus(Uri downloadUri) {
        return Integer.valueOf(getDownloadField(downloadUri, Downloads.Impl.COLUMN_STATUS));
    }

    private String getDownloadFilename(Uri downloadUri) {
        return getDownloadField(downloadUri, Downloads.Impl._DATA);
    }

    private String getDownloadField(Uri downloadUri, String column) {
        final String[] columns = new String[] {column};
        Cursor cursor = mResolver.query(downloadUri, columns, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            return cursor.getString(0);
        } finally {
            cursor.close();
        }
    }

    /**
     * Request a download from the Download Manager.
     */
    private Uri requestDownload(String path) throws MalformedURLException, UnknownHostException {
        ContentValues values = new ContentValues();
        values.put(Downloads.Impl.COLUMN_URI, getServerUri(path));
        values.put(Downloads.Impl.COLUMN_DESTINATION, Downloads.Impl.DESTINATION_EXTERNAL);
        return mResolver.insert(Downloads.Impl.CONTENT_URI, values);
    }

    /**
     * Update one field of a download in the provider.
     */
    private void updateDownload(Uri downloadUri, String column, String value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        int numChanged = mResolver.update(downloadUri, values, null, null);
        assertEquals(1, numChanged);
    }
}
