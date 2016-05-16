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

package com.android.example.imsframework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Example service to handle IMS setup after boot completed event.
 *
 * Setting {@code android:persistent="true"} in the manifest will cause
 * {@link ImsFrameworkApp#onCreate()} to be called at system startup,
 * before {@link Intent#ACTION_BOOT_COMPLETED} is broadcast, so early
 * initialization can be performed there, such as registering to receive
 * telephony state change broadcasts that can't be declared in the manifest.
 */
public class ImsFrameworkService extends Service {
    private static final String TAG = "ImsFrameworkService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.d(TAG, "Service starting for intent " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Received ACTION_BOOT_COMPLETED");
            handleBootCompleted();
        }
        stopSelf();     // stop service after handling the action
        return START_NOT_STICKY;
    }

    private void handleBootCompleted() {
        // Code to execute after boot completes, e.g. connecting to the IMS PDN
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;    // clients can't bind to this service
    }
}
