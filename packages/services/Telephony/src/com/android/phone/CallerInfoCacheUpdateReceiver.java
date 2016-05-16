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

package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;

/**
 * BroadcastReceiver responsible for (periodic) update of {@link CallerInfoCache}.
 *
 * This broadcast can be sent from Contacts edit screen, implying relevant settings have changed
 * and the cache may become obsolete.
 */
public class CallerInfoCacheUpdateReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = CallerInfoCacheUpdateReceiver.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    public static final String ACTION_UPDATE_CALLER_INFO_CACHE =
            "com.android.phone.UPDATE_CALLER_INFO_CACHE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DBG) log("CallerInfoCacheUpdateReceiver#onReceive(). Intent: " + intent);
        PhoneGlobals.getInstance().callerInfoCache.startAsyncCache();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}