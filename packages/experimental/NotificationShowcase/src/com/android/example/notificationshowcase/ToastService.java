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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class ToastService extends IntentService {

    private static final String TAG = "ToastService";

    private static final String ACTION_TOAST = "toast";

    private Handler handler;

    public ToastService() {
        super(TAG);
    }
    public ToastService(String name) {
        super(name);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "clicked a thing! intent=" + intent.toString());
        if (intent.hasExtra("text")) {
            final String text = intent.getStringExtra("text");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ToastService.this, text, Toast.LENGTH_LONG).show();
                    Log.v(TAG, "toast " + text);
                }
            });
        }
    }

    public static PendingIntent getPendingIntent(Context context, String text) {
        Intent toastIntent = new Intent(context, ToastService.class);
        toastIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        toastIntent.setAction(ACTION_TOAST + ":" + text); // one per toast message
        toastIntent.putExtra("text", text);
        PendingIntent pi = PendingIntent.getService(
                context, 58, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }
}
