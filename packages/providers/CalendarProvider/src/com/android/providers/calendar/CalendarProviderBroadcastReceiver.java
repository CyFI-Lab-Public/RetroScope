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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CalendarProviderBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!CalendarAlarmManager.ACTION_CHECK_NEXT_ALARM.equals(intent.getAction())) {
            setResultCode(Activity.RESULT_CANCELED);
            return;
        }
        final CalendarProvider2 provider = CalendarProvider2.getInstance();
        // Acquire a wake lock that will be released when the launched Service is doing its work
        provider.getOrCreateCalendarAlarmManager().acquireScheduleNextAlarmWakeLock();
        // Set the result code
        setResultCode(Activity.RESULT_OK);
        // Launch the Service
        intent.setClass(context, CalendarProviderIntentService.class);
        context.startService(intent);
    }
}
