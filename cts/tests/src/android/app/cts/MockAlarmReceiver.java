/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * this class  receive alarm from AlarmManagerTest
 */
public class MockAlarmReceiver extends BroadcastReceiver {
    public boolean alarmed = false;
    private Object mSync = new Object();
    public static final String MOCKACTION = "android.app.AlarmManagerTest.TEST_ALARMRECEIVER";

    public long elapsedTime;
    public long rtcTime;

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(MOCKACTION)) {
            synchronized (mSync) {
                alarmed = true;
                elapsedTime = SystemClock.elapsedRealtime();
                rtcTime = System.currentTimeMillis();
            }
        }
    }

    public void setAlarmedFalse() {
        synchronized (mSync) {
            alarmed = false;
        }
    }
}
