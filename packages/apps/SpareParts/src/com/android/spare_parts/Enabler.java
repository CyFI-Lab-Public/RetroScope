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

package com.android.spare_parts;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

public final class Enabler extends BroadcastReceiver {
    private static final String TAG = "SpareParts.Enabler";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean enabled = intent.getIntExtra("state",
                Settings.System.ADVANCED_SETTINGS_DEFAULT) != 0;
        final PackageManager pm = context.getPackageManager();
        final ComponentName componentName = new ComponentName(context, SpareParts.class);
        final int value = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        pm.setComponentEnabledSetting(componentName, value, PackageManager.DONT_KILL_APP);
    }
}

