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

import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.app.DownloadManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

/**
 * Code common to tests that use the download manager public API.
 */
public abstract class AbstractPublicApiTest extends AbstractDownloadProviderFunctionalTest {

    class Download {
        final long mId;

        private Download(long downloadId) {
            this.mId = downloadId;
        }

        public int getStatus() {
            return (int) getLongField(DownloadManager.COLUMN_STATUS);
        }

        public int getReason() {
            return (int) getLongField(DownloadManager.COLUMN_REASON);
        }

        public int getStatusIfExists() {
            Cursor cursor = mManager.query(new DownloadManager.Query().setFilterById(mId));
            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    return (int) cursor.getLong(cursor.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_STATUS));
                } else {
                    // the row doesn't exist
                    return -1;
                }
            } finally {
                cursor.close();
            }
        }

        String getStringField(String field) {
            Cursor cursor = mManager.query(new DownloadManager.Query().setFilterById(mId));
            try {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndexOrThrow(field));
            } finally {
                cursor.close();
            }
        }

        long getLongField(String field) {
            Cursor cursor = mManager.query(new DownloadManager.Query().setFilterById(mId));
            try {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                return cursor.getLong(cursor.getColumnIndexOrThrow(field));
            } finally {
                cursor.close();
            }
        }

        String getContents() throws Exception {
            ParcelFileDescriptor downloadedFile = mManager.openDownloadedFile(mId);
            assertTrue("Invalid file descriptor: " + downloadedFile,
                       downloadedFile.getFileDescriptor().valid());
            final InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(
                    downloadedFile);
            try {
                return readStream(stream);
            } finally {
                stream.close();
            }
        }

        void runUntilStatus(int status) throws TimeoutException {
            final long startMillis = mSystemFacade.currentTimeMillis();
            startService(null);
            waitForStatus(status, startMillis);
        }

        void runUntilStatus(int status, long timeout) throws TimeoutException {
            final long startMillis = mSystemFacade.currentTimeMillis();
            startService(null);
            waitForStatus(status, startMillis, timeout);
        }

        void waitForStatus(int expected, long afterMillis) throws TimeoutException {
            waitForStatus(expected, afterMillis, 15 * SECOND_IN_MILLIS);
        }

        void waitForStatus(int expected, long afterMillis, long timeout) throws TimeoutException {
            int actual = -1;

            final long elapsedTimeout = SystemClock.elapsedRealtime() + timeout;
            while (SystemClock.elapsedRealtime() < elapsedTimeout) {
                if (getLongField(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP) >= afterMillis) {
                    actual = getStatus();
                    if (actual == STATUS_SUCCESSFUL || actual == STATUS_FAILED) {
                        assertEquals(expected, actual);
                        return;
                    } else if (actual == expected) {
                        return;
                    }

                    if (timeout > MINUTE_IN_MILLIS) {
                        final int percent = (int) (100
                                * getLongField(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                / getLongField(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        Log.d(LOG_TAG, percent + "% complete");
                    }
                }

                if (timeout > MINUTE_IN_MILLIS) {
                    SystemClock.sleep(SECOND_IN_MILLIS * 3);
                } else {
                    SystemClock.sleep(100);
                }
            }

            throw new TimeoutException("Expected status " + expected + "; only reached " + actual);
        }

        // max time to wait before giving up on the current download operation.
        private static final int MAX_TIME_TO_WAIT_FOR_OPERATION = 5;
        // while waiting for the above time period, sleep this long to yield to the
        // download thread
        private static final int TIME_TO_SLEEP = 1000;

        // waits until progress_so_far is >= (progress)%
        boolean runUntilProgress(int progress) throws InterruptedException {
            startService(null);

            int sleepCounter = MAX_TIME_TO_WAIT_FOR_OPERATION * 1000 / TIME_TO_SLEEP;
            int numBytesReceivedSoFar = 0;
            int totalBytes = 0;
            for (int i = 0; i < sleepCounter; i++) {
                Cursor cursor = mManager.query(new DownloadManager.Query().setFilterById(mId));
                try {
                    assertEquals(1, cursor.getCount());
                    cursor.moveToFirst();
                    numBytesReceivedSoFar = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    totalBytes = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                } finally {
                    cursor.close();
                }
                Log.i(LOG_TAG, "in runUntilProgress, numBytesReceivedSoFar: " +
                        numBytesReceivedSoFar + ", totalBytes: " + totalBytes);
                if (totalBytes == 0) {
                    fail("total_bytes should not be zero");
                    return false;
                } else {
                    if (numBytesReceivedSoFar * 100 / totalBytes >= progress) {
                        // progress_so_far is >= progress%. we are done
                        return true;
                    }
                }
                // download not done yet. sleep a while and try again
                Thread.sleep(TIME_TO_SLEEP);
            }
            Log.i(LOG_TAG, "FAILED in runUntilProgress, numBytesReceivedSoFar: " +
                    numBytesReceivedSoFar + ", totalBytes: " + totalBytes);
            return false; // failed
        }
    }

    protected static final String PACKAGE_NAME = "my.package.name";
    protected static final String REQUEST_PATH = "/path";

    protected DownloadManager mManager;

    public AbstractPublicApiTest(FakeSystemFacade systemFacade) {
        super(systemFacade);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mManager = new DownloadManager(mResolver, PACKAGE_NAME);
    }

    protected DownloadManager.Request getRequest()
            throws MalformedURLException, UnknownHostException {
        return getRequest(getServerUri(REQUEST_PATH));
    }

    protected DownloadManager.Request getRequest(String path) {
        return new DownloadManager.Request(Uri.parse(path));
    }

    protected Download enqueueRequest(DownloadManager.Request request) {
        return new Download(mManager.enqueue(request));
    }
}
