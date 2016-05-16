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

import com.android.internal.telephony.cat.CatLog;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Application installer for SIM Toolkit.
 *
 */
abstract class StkAppInstaller {
    private StkAppInstaller() {}

    static void install(Context context) {
        setAppState(context, true);
    }

    static void unInstall(Context context) {
        setAppState(context, false);
    }

    private static void setAppState(Context context, boolean install) {
        if (context == null) {
            return;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return;
        }
        // check that STK app package is known to the PackageManager
        ComponentName cName = new ComponentName("com.android.stk",
                "com.android.stk.StkLauncherActivity");
        int state = install ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        try {
            pm.setComponentEnabledSetting(cName, state,
                    PackageManager.DONT_KILL_APP);
        } catch (Exception e) {
            CatLog.d("StkAppInstaller", "Could not change STK app state");
        }
    }
}
