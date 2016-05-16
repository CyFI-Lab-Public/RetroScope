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
 * limitations under the License
 */

package com.android.providers.contacts;/*
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
 * limitations under the License
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

/**
 * Receiver that handles boot completion broadcast.
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = BootCompletedReceiver.class.getSimpleName();
    public static final String DATABASE_TIME_CREATED = "database_time_created";

    public void onReceive(Context context, Intent intent) {
        // This notification is sent here instead of ContactsDatabaseHelper.onCreate() because it
        // needs to be sent after boot completes. onCreate() starts before boot complete happens
        // and the notification would be dropped by the framework.

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String prefTime = prefs.getString(DATABASE_TIME_CREATED, "");

        String dbTime = ContactsDatabaseHelper.getInstance(context).getProperty(
                ContactsDatabaseHelper.DbProperties.DATABASE_TIME_CREATED, "");
        int count = 0;

        // db time should already exist because it's always created by the ContactsUpgradeReceiver
        // pre boot. But in the event that it may be a bit slow, wait a bit.
        while (TextUtils.isEmpty(dbTime) && count != 3) {
            SystemClock.sleep(1000);
            dbTime = ContactsDatabaseHelper.getInstance(context).getProperty(
                    ContactsDatabaseHelper.DbProperties.DATABASE_TIME_CREATED, "");
            count++;
        }

        if (TextUtils.isEmpty(prefTime) || !prefTime.equals(dbTime)) {
            Log.i(TAG, "Boot complete, newly created contact database detected. Pref Time: " +
                    prefTime + " DB Time: " + dbTime);

            final Intent broadcast = new Intent(ContactsContract.Intents.CONTACTS_DATABASE_CREATED);
            broadcast.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            context.sendBroadcast(broadcast, android.Manifest.permission.READ_CONTACTS);

            prefs.edit().putString(DATABASE_TIME_CREATED, dbTime).commit();
        }
    }

}
