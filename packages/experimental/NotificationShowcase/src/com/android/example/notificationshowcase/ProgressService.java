/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.example.notificationshowcase;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ProgressService extends IntentService {

    private static final String TAG = "ProgressService";

    private static final String ACTION_PROGRESS = "progress";

    private Handler handler;

    public ProgressService() {
        super(TAG);
    }
    public ProgressService(String name) {
        super(name);
    }

    class UpdateRunnable implements Runnable {

        private final int mId;
        private final long mWhen;
        private int mProgress;

        UpdateRunnable(int id, long when, int progress) {
            mId = id;
            mWhen = when;
            mProgress = progress;
        }

        @Override
        public void run() {            NotificationManager noMa = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
                Log.v(TAG, "id: " + mId + " when: " + mWhen + " progress: " + mProgress);
                noMa.notify(NotificationService.NOTIFICATION_ID + mId,
                        NotificationService.makeUploadNotification(
                                ProgressService.this, mProgress, mWhen));
                mProgress += 10;
            if (mProgress <= 100) {
                handler.postDelayed(this, 1000);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        final int id = intent.getIntExtra("id", 0);
        final long when = intent.getLongExtra("when", 0L);
        int progress = intent.getIntExtra("progress", 0);
        handler.postDelayed(new UpdateRunnable(id, when, progress), 1000);
    }

    public static void startProgressUpdater(Context context, int id, long when, int progress) {
        Intent progressIntent = new Intent(context, ProgressService.class);
        progressIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        progressIntent.setAction(ACTION_PROGRESS);
        progressIntent.putExtra("id", id);
        progressIntent.putExtra("when", when);
        progressIntent.putExtra("progress", progress);
        context.startService(progressIntent);
    }
}
