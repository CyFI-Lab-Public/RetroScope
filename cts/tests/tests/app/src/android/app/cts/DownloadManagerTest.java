/*
 * Copyright (C) 2011 The Android Open Source Project
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
package android.app.cts;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.cts.util.PollingCheck;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.text.format.DateUtils;
import android.webkit.cts.CtsTestServer;

import com.google.android.collect.Sets;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DownloadManagerTest extends AndroidTestCase {

    /**
     * According to the CDD Section 7.6.1, the DownloadManager implementation must be able to
     * download individual files of 55 MB.
     */
    private static final int MINIMUM_DOWNLOAD_BYTES = 55 * 1024 * 1024;

    private static final long SHORT_TIMEOUT = 5 * DateUtils.SECOND_IN_MILLIS;
    private static final long LONG_TIMEOUT = 2 * DateUtils.MINUTE_IN_MILLIS;

    private DownloadManager mDownloadManager;

    private CtsTestServer mWebServer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDownloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        mWebServer = new CtsTestServer(mContext);
        clearDownloads();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mWebServer.shutdown();
    }

    public void testDownloadManager() throws Exception {
        final DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
        try {
            IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(receiver, intentFilter);

            long goodId = mDownloadManager.enqueue(new Request(getGoodUrl()));
            long badId = mDownloadManager.enqueue(new Request(getBadUrl()));

            int allDownloads = getTotalNumberDownloads();
            assertEquals(2, allDownloads);

            assertDownloadQueryableById(goodId);
            assertDownloadQueryableById(badId);

            receiver.waitForDownloadComplete(SHORT_TIMEOUT, goodId, badId);

            assertDownloadQueryableByStatus(DownloadManager.STATUS_SUCCESSFUL);
            assertDownloadQueryableByStatus(DownloadManager.STATUS_FAILED);

            assertRemoveDownload(goodId, allDownloads - 1);
            assertRemoveDownload(badId, allDownloads - 2);
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    public void testMinimumDownload() throws Exception {
        final DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
        try {
            IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(receiver, intentFilter);

            long id = mDownloadManager.enqueue(new Request(getMinimumDownloadUrl()));
            receiver.waitForDownloadComplete(LONG_TIMEOUT, id);

            ParcelFileDescriptor fileDescriptor = mDownloadManager.openDownloadedFile(id);
            assertEquals(MINIMUM_DOWNLOAD_BYTES, fileDescriptor.getStatSize());

            Cursor cursor = null;
            try {
                cursor = mDownloadManager.query(new Query().setFilterById(id));
                assertTrue(cursor.moveToNext());
                assertEquals(DownloadManager.STATUS_SUCCESSFUL, cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                assertEquals(MINIMUM_DOWNLOAD_BYTES, cursor.getInt(
                        cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)));
                assertFalse(cursor.moveToNext());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            assertRemoveDownload(id, 0);
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    /**
     * Set download locations and verify that file is downloaded to correct location.
     *
     * Checks three different methods of setting location: directly via setDestinationUri, and
     * indirectly through setDestinationInExternalFilesDir and setDestinationinExternalPublicDir.
     */
    public void testDownloadManagerDestination() throws Exception {
        File uriLocation = new File(mContext.getExternalFilesDir(null), "uriFile.bin");
        if (uriLocation.exists()) {
            assertTrue(uriLocation.delete());
        }

        File extFileLocation = new File(mContext.getExternalFilesDir(null), "extFile.bin");
        if (extFileLocation.exists()) {
            assertTrue(extFileLocation.delete());
        }

        File publicLocation = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "publicFile.bin");
        if (publicLocation.exists()) {
            assertTrue(publicLocation.delete());
        }

        final DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
        try {
            IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(receiver, intentFilter);

            Request requestUri = new Request(getGoodUrl());
            requestUri.setDestinationUri(Uri.fromFile(uriLocation));
            long uriId = mDownloadManager.enqueue(requestUri);

            Request requestExtFile = new Request(getGoodUrl());
            requestExtFile.setDestinationInExternalFilesDir(mContext, null, "extFile.bin");
            long extFileId = mDownloadManager.enqueue(requestExtFile);

            Request requestPublic = new Request(getGoodUrl());
            requestPublic.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                    "publicFile.bin");
            long publicId = mDownloadManager.enqueue(requestPublic);

            int allDownloads = getTotalNumberDownloads();
            assertEquals(3, allDownloads);

            receiver.waitForDownloadComplete(SHORT_TIMEOUT, uriId, extFileId, publicId);

            assertSuccessfulDownload(uriId, uriLocation);
            assertSuccessfulDownload(extFileId, extFileLocation);
            assertSuccessfulDownload(publicId, publicLocation);

            assertRemoveDownload(uriId, allDownloads - 1);
            assertRemoveDownload(extFileId, allDownloads - 2);
            assertRemoveDownload(publicId, allDownloads - 3);
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    /**
     * Set the download location and verify that the extension of the file name is left unchanged.
     */
    public void testDownloadManagerDestinationExtension() throws Exception {
        String noExt = "noiseandchirps";
        File noExtLocation = new File(mContext.getExternalFilesDir(null), noExt);
        if (noExtLocation.exists()) {
            assertTrue(noExtLocation.delete());
        }

        String wrongExt = "noiseandchirps.wrong";
        File wrongExtLocation = new File(mContext.getExternalFilesDir(null), wrongExt);
        if (wrongExtLocation.exists()) {
            assertTrue(wrongExtLocation.delete());
        }

        final DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
        try {
            IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            mContext.registerReceiver(receiver, intentFilter);

            Request requestNoExt = new Request(getAssetUrl(noExt));
            requestNoExt.setDestinationUri(Uri.fromFile(noExtLocation));
            long noExtId = mDownloadManager.enqueue(requestNoExt);

            Request requestWrongExt = new Request(getAssetUrl(wrongExt));
            requestWrongExt.setDestinationUri(Uri.fromFile(wrongExtLocation));
            long wrongExtId = mDownloadManager.enqueue(requestWrongExt);

            int allDownloads = getTotalNumberDownloads();
            assertEquals(2, allDownloads);

            receiver.waitForDownloadComplete(SHORT_TIMEOUT, noExtId, wrongExtId);

            assertSuccessfulDownload(noExtId, noExtLocation);
            assertSuccessfulDownload(wrongExtId, wrongExtLocation);

            assertRemoveDownload(noExtId, allDownloads - 1);
            assertRemoveDownload(wrongExtId, allDownloads - 2);
        } finally {
            mContext.unregisterReceiver(receiver);
        }
    }

    private class DownloadCompleteReceiver extends BroadcastReceiver {
        private HashSet<Long> mCompleteIds = Sets.newHashSet();

        public DownloadCompleteReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mCompleteIds) {
                mCompleteIds.add(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));
                mCompleteIds.notifyAll();
            }
        }

        private boolean isCompleteLocked(long... ids) {
            for (long id : ids) {
                if (!mCompleteIds.contains(id)) {
                    return false;
                }
            }
            return true;
        }

        public void waitForDownloadComplete(long timeoutMillis, long... waitForIds)
                throws InterruptedException {
            if (waitForIds.length == 0) {
                throw new IllegalArgumentException("Missing IDs to wait for");
            }

            final long startTime = SystemClock.elapsedRealtime();
            do {
                synchronized (mCompleteIds) {
                    mCompleteIds.wait(timeoutMillis);
                    if (isCompleteLocked(waitForIds)) return;
                }
            } while ((SystemClock.elapsedRealtime() - startTime) < timeoutMillis);

            throw new InterruptedException("Timeout waiting for IDs " + Arrays.toString(waitForIds)
                    + "; received " + mCompleteIds.toString()
                    + ".  Make sure you have WiFi or some other connectivity for this test.");
        }
    }

    private void clearDownloads() {
        if (getTotalNumberDownloads() > 0) {
            Cursor cursor = null;
            try {
                Query query = new Query();
                cursor = mDownloadManager.query(query);
                int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                long[] removeIds = new long[cursor.getCount()];
                for (int i = 0; cursor.moveToNext(); i++) {
                    removeIds[i] = cursor.getLong(columnIndex);
                }
                assertEquals(removeIds.length, mDownloadManager.remove(removeIds));
                assertEquals(0, getTotalNumberDownloads());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private Uri getGoodUrl() {
        return Uri.parse(mWebServer.getTestDownloadUrl("cts-good-download", 0));
    }

    private Uri getBadUrl() {
        return Uri.parse(mWebServer.getBaseUri() + "/nosuchurl");
    }

    private Uri getMinimumDownloadUrl() {
        return Uri.parse(mWebServer.getTestDownloadUrl("cts-minimum-download",
                MINIMUM_DOWNLOAD_BYTES));
    }

    private Uri getAssetUrl(String asset) {
        return Uri.parse(mWebServer.getAssetUrl(asset));
    }

    private int getTotalNumberDownloads() {
        Cursor cursor = null;
        try {
            Query query = new Query();
            cursor = mDownloadManager.query(query);
            return cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void assertDownloadQueryableById(long downloadId) {
        Cursor cursor = null;
        try {
            Query query = new Query().setFilterById(downloadId);
            cursor = mDownloadManager.query(query);
            assertEquals(1, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void assertDownloadQueryableByStatus(final int status) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                Cursor cursor= null;
                try {
                    Query query = new Query().setFilterByStatus(status);
                    cursor = mDownloadManager.query(query);
                    return 1 == cursor.getCount();
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        }.run();
    }

    private void assertSuccessfulDownload(long id, File location) {
        Cursor cursor = null;
        try {
            cursor = mDownloadManager.query(new Query().setFilterById(id));
            assertTrue(cursor.moveToNext());
            assertEquals(DownloadManager.STATUS_SUCCESSFUL, cursor.getInt(
                    cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)));
            assertEquals(Uri.fromFile(location).toString(),
                    cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
            assertTrue(location.exists());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void assertRemoveDownload(long removeId, int expectedNumDownloads) {
        Cursor cursor = null;
        try {
            assertEquals(1, mDownloadManager.remove(removeId));
            Query query = new Query();
            cursor = mDownloadManager.query(query);
            assertEquals(expectedNumDownloads, cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
