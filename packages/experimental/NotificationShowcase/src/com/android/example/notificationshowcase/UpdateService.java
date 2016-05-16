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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UpdateService extends IntentService {

    private static final String TAG = "UpdateService";

    private static final String ACTION_UPDATE = "update";

    public UpdateService() {
        super(TAG);
    }

    public UpdateService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            // allow the user close the shade, if they want to test that.
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        Log.v(TAG, "clicked a thing! intent=" + intent.toString());
        if (intent.hasExtra("id") && intent.hasExtra("when")) {
            final int id = intent.getIntExtra("id", 0);
                NotificationManager noMa =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                final int update = intent.getIntExtra("update", 0);
                final long when = intent.getLongExtra("when", 0L);
                Log.v(TAG, "id: " + id + " when: " + when + " update: " + update);
                noMa.notify(NotificationService.NOTIFICATION_ID + id,
                        NotificationService.makeBigTextNotification(this, update, id, when));
        } else {
            Log.v(TAG, "id extra was " + (intent.hasExtra("id") ? "present" : "missing"));
            Log.v(TAG, "when extra was " + (intent.hasExtra("when") ? "present"  : "missing"));
        }
    }

    public static PendingIntent getPendingIntent(Context context, int update, int id, long when) {
        Intent updateIntent = new Intent(context, UpdateService.class);
        updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        updateIntent.setAction(ACTION_UPDATE);
        updateIntent.putExtra("id", id);
        updateIntent.putExtra("when", when);
        updateIntent.putExtra("update", update);
        PendingIntent pi = PendingIntent.getService(
                context, 58, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
}
