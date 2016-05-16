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

package android.telephony.gsm.cts;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.telephony.gsm.SmsManager;

@SuppressWarnings("deprecation")
public class SmsManagerTest extends android.telephony.cts.SmsManagerTest {

    @Override
    public void testGetDefault() {
        assertNotNull(getSmsManager());
    }

    @Override
    protected ArrayList<String> divideMessage(String text) {
        return getSmsManager().divideMessage(text);
    }

    private android.telephony.gsm.SmsManager getSmsManager() {
        return android.telephony.gsm.SmsManager.getDefault();
    }

    @Override
    protected void sendMultiPartTextMessage(String destAddr, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents) {
        getSmsManager().sendMultipartTextMessage(destAddr, null, parts, sentIntents, deliveryIntents);
    }

    @Override
    protected void sendDataMessage(String destAddr,short port, byte[] data, PendingIntent sentIntent, PendingIntent deliveredIntent) {
        getSmsManager().sendDataMessage(destAddr, null, port, data, sentIntent, deliveredIntent);
    }

    @Override
    protected void sendTextMessage(String destAddr, String text, PendingIntent sentIntent, PendingIntent deliveredIntent) {
        getSmsManager().sendTextMessage(destAddr, null, text, sentIntent, deliveredIntent);
    }
}