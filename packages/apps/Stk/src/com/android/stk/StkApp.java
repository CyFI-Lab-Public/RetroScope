/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.stk;

import android.app.Application;

import com.android.internal.telephony.cat.Duration;

/**
 * Top-level Application class for STK app.
 */
abstract class StkApp extends Application {
    // Application constants
    public static final boolean DBG = true;

    // Identifiers for option menu items
    static final int MENU_ID_END_SESSION = android.view.Menu.FIRST;
    static final int MENU_ID_BACK = android.view.Menu.FIRST + 1;
    static final int MENU_ID_HELP = android.view.Menu.FIRST + 2;

    // Display Text timeouts
    static final int DISP_TEXT_CLEAR_AFTER_DELAY_TIMEOUT = (15 * 1000);
    static final int DISP_TEXT_WAIT_FOR_USER_TIMEOUT = (60 * 1000);

    // UI timeout, 30 seconds - used for menues and input
    static final int UI_TIMEOUT = (30 * 1000);

    // Tone default timeout - 2 seconds
    static final int TONE_DFEAULT_TIMEOUT = (2 * 1000);

    public static final String TAG = "STK App";

    /**
     * This function calculate the time in MS from a duration instance.
     * returns zero when duration is null.
     */
    public static int calculateDurationInMilis(Duration duration) {
        int timeout = 0;
        if (duration != null) {
            switch (duration.timeUnit) {
            case MINUTE:
                timeout = 1000 * 60;
                break;
            case TENTH_SECOND:
                timeout = 1000 / 10;
                break;
            case SECOND:
            default:
                timeout = 1000;
                break;
            }
            timeout *= duration.timeInterval;
        }
        return timeout;
    }
}
