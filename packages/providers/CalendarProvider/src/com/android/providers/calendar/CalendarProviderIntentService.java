/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.providers.calendar;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class CalendarProviderIntentService extends IntentService {

    private static final String TAG = CalendarProvider2.TAG;
    private static final String REMOVE_ALARMS_VALUE = "removeAlarms";

    public CalendarProviderIntentService() {
        super("CalendarProviderIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Received Intent: " + intent);
        }
        final String action = intent.getAction();
        if (!CalendarAlarmManager.ACTION_CHECK_NEXT_ALARM.equals(action)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
               Log.d(TAG, "Invalid Intent action: " + action);
            }
            return;
        }
        final CalendarProvider2 provider = CalendarProvider2.getInstance();
        // Schedule the next alarm
        final boolean removeAlarms = intent.getBooleanExtra(REMOVE_ALARMS_VALUE, false);
        provider.getOrCreateCalendarAlarmManager().runScheduleNextAlarm(removeAlarms, provider);
        // Release the wake lock that was set in the Broadcast Receiver
        provider.getOrCreateCalendarAlarmManager().releaseScheduleNextAlarmWakeLock();
    }
}
