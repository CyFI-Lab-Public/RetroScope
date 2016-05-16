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

package android.media.cts;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MediaScannerNotificationTest extends AndroidTestCase {

    public void testMediaScannerNotification() throws Exception {
        ScannerNotificationReceiver startedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_STARTED);
        ScannerNotificationReceiver finishedReceiver = new ScannerNotificationReceiver(
                Intent.ACTION_MEDIA_SCANNER_FINISHED);

        IntentFilter startedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
        startedIntentFilter.addDataScheme("file");
        IntentFilter finshedIntentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        finshedIntentFilter.addDataScheme("file");

        mContext.registerReceiver(startedReceiver, startedIntentFilter);
        mContext.registerReceiver(finishedReceiver, finshedIntentFilter);

        String [] temps = new String[] { "avi", "gif", "jpg", "dat", "mp3", "mp4", "txt" };
        String tmpPath = createTempFiles(temps);

        Bundle args = new Bundle();
        args.putString("volume", "external");
        Intent i = new Intent("android.media.IMediaScannerService").putExtras(args);
        i.setClassName("com.android.providers.media",
                "com.android.providers.media.MediaScannerService");
        mContext.startService(i);

        startedReceiver.waitForBroadcast();
        finishedReceiver.waitForBroadcast();

        checkTempFiles(tmpPath, temps);

        // add .nomedia file and scan again
        File noMedia = new File(tmpPath, ".nomedia");
        try {
            noMedia.createNewFile();
        } catch (IOException e) {
            fail("couldn't create .nomedia file");
        }
        startedReceiver.reset();
        finishedReceiver.reset();
        mContext.startService(i);
        startedReceiver.waitForBroadcast();
        finishedReceiver.waitForBroadcast();

        checkTempFiles(tmpPath, temps);
        assertTrue(noMedia.delete());
        deleteTempFiles(tmpPath, temps);

        // scan one more time just to clean everything up nicely
        startedReceiver.reset();
        finishedReceiver.reset();
        mContext.startService(i);
        startedReceiver.waitForBroadcast();
        finishedReceiver.waitForBroadcast();

    }

    String createTempFiles(String [] extensions) {
        String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File tmpDir = new File(externalPath, "" + System.nanoTime());
        String tmpPath = tmpDir.getAbsolutePath();
        assertFalse(tmpPath + " already exists", tmpDir.exists());
        assertTrue("failed to create " + tmpDir, tmpDir.mkdirs());

        for (int i = 0; i < extensions.length; i++) {
            File foo = new File(tmpPath, "foobar." + extensions[i]);
            try {
                // create a non-empty file
                foo.createNewFile();
                FileOutputStream out = new FileOutputStream(foo);
                out.write(0x12);
                out.flush();
                out.close();
                assertTrue(foo.length() != 0);
            } catch (IOException e) {
                fail("Error creating " + foo.getAbsolutePath() + ": " + e);
            }
        }
        return tmpPath;
    }

    void checkTempFiles(String tmpPath, String [] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            File foo = new File(tmpPath, "foobar." + extensions[i]);
            assertTrue(foo.getAbsolutePath() + " no longer exists or was truncated",
                    foo.length() != 0);
        }
    }

    void deleteTempFiles(String tmpPath, String [] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            assertTrue(new File(tmpPath, "foobar." + extensions[i]).delete());
        }
        assertTrue(new File(tmpPath).delete());
    }
}
