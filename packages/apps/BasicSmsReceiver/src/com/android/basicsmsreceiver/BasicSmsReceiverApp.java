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

package com.android.basicsmsreceiver;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

public class BasicSmsReceiverApp extends Application {
    public static final String LOG_TAG = "BasicSmsReceiverApp";
    public static final String SMS_LITE_PREFS_KEY = "sms_lite_prefs";
    public static final String PREF_KEY_NOTIFICATION_ID = "notification_id";

    static BasicSmsReceiverApp gBasicSmsReceiverApp;

    @Override
    public void onCreate() {
        super.onCreate();

        gBasicSmsReceiverApp = this;
    }

    public static BasicSmsReceiverApp getBasicSmsReceiverApp() {
        return gBasicSmsReceiverApp;
    }

    // Each incoming sms gets its own notification. We have to use a new unique notification id
    // for each one.
    public int getNextNotificationId() {
        SharedPreferences prefs = getSharedPreferences(SMS_LITE_PREFS_KEY,
                Activity.MODE_PRIVATE);
        int notificationId = prefs.getInt(PREF_KEY_NOTIFICATION_ID, 0);
        ++notificationId;
        if (notificationId > 32765) {
            notificationId = 1;     // wrap around before it gets dangerous
        }

        // Save the updated notificationId in SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_KEY_NOTIFICATION_ID, notificationId);
        editor.apply();

        Log.d(LOG_TAG, "getNextNotificationId: " + notificationId);

        return notificationId;
    }
}
