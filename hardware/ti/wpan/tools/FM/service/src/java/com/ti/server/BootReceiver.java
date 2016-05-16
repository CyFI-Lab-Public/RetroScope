/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Broadcast receiver that will handle the boot completed intent and start the
 * AccessoryService.
 *
 */
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, final Intent bootintent) {
        Log.i("BootReceiver", "Starting FM service");
        // Starting service at boot to speed up enable/disable of FM radio
        context.startService(new Intent("com.ti.server.FmService"));
    }
}
