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

package com.android.phone;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipManager;

public class SipUtil {
    private SipUtil() {
    }

    public static PendingIntent createIncomingCallPendingIntent() {
        Context phoneContext = PhoneGlobals.getInstance();
        Intent intent = new Intent(phoneContext, SipBroadcastReceiver.class);
        intent.setAction(SipManager.ACTION_SIP_INCOMING_CALL);
        return PendingIntent.getBroadcast(phoneContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
