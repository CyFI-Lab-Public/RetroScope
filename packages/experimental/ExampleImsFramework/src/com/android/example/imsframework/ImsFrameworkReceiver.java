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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ImsFrameworkReceiver extends BroadcastReceiver {
    private static final String TAG = "ImsFrameworkReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, "starting service for ACTION_BOOT_COMPLETED");
            intent = new Intent(intent);
            intent.setClass(context, ImsFrameworkService.class);
            if (context.startService(intent) == null) {
                Log.e(TAG, "Can't start service");
            }
        } else if (Intent.ACTION_SHUTDOWN.equals(action)) {
            Log.d(TAG, "received ACTION_SHUTDOWN, shutting down IMS");
            // System is shutting down immediately: perform cleanup here
        } else {
            Log.e(TAG, "Received unknown intent: " + action);
        }
    }
}
