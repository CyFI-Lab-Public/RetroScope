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

package android.media.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ScannerNotificationReceiver extends BroadcastReceiver {

    private static final int TIMEOUT_MS = 4 * 60 * 1000;

    private final String mAction;
    private CountDownLatch mLatch = new CountDownLatch(1);

    ScannerNotificationReceiver(String action) {
        mAction = action;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(mAction)) {
            mLatch.countDown();
        }
    }

    public void waitForBroadcast() throws InterruptedException {
        if (!mLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            int numFiles = countFiles(Environment.getExternalStorageDirectory());
            MediaScannerTest.fail("Failed to receive broadcast in " + TIMEOUT_MS + "ms for "
                    + mAction + " while trying to scan " + numFiles + " files!");
        }
    }

    void reset() {
        mLatch = new CountDownLatch(1);
    }

    private int countFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += countFiles(file);
                } else {
                    count++;
                }
            }
        }
        return count;
    }
}
