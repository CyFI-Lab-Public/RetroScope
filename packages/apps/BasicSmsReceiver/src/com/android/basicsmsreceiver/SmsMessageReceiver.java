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

package com.android.basicsmsreceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsMessageReceiver extends BroadcastReceiver {
    /** Tag string for our debug logs */
    private static final String LOG_TAG = "SmsMessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Log.i(LOG_TAG, "onReceive");
        if (extras == null)
            return;

        Object[] pdus = (Object[]) extras.get("pdus");

        for (int i = 0; i < pdus.length; i++) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
            String fromAddress = message.getOriginatingAddress();
            String messageBody = message.getMessageBody().toString();

            Log.i(LOG_TAG, "From: " + fromAddress + " message: " + messageBody);

            addNotification(context, fromAddress, messageBody);
        }
    }

    private void addNotification(Context context, String fromAddress, String message) {
        int notificationId = BasicSmsReceiverApp.getBasicSmsReceiverApp().getNextNotificationId();

        Notification.Builder notification = new Notification.Builder(context)
            .setTicker(message)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(fromAddress)
            .setContentText(message)
            .setSmallIcon(R.drawable.stat_notify_sms)
            .setContentIntent(createDisplayMessageIntent(context, fromAddress, message,
                    notificationId));

        Log.i(LOG_TAG, "addNotification notificationId: " + notificationId);

        NotificationManager notificationManager =
            (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notification.getNotification());
    }

    private PendingIntent createDisplayMessageIntent(Context context, String fromAddress,
            String message, int notificationId) {
        // Trigger the main activity to fire up a dialog that shows the received messages
        Intent di = new Intent();
        di.setClass(context, DialogSmsDisplay.class);
        di.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        di.putExtra(DialogSmsDisplay.SMS_FROM_ADDRESS_EXTRA, fromAddress);
        di.putExtra(DialogSmsDisplay.SMS_MESSAGE_EXTRA, message);
        di.putExtra(DialogSmsDisplay.SMS_NOTIFICATION_ID_EXTRA, notificationId);

        // This line is needed to make this intent compare differently than the other intents
        // created here for other messages. Without this line, the PendingIntent always gets the
        // intent of a previous message and notification.
        di.setType(Integer.toString(notificationId));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, di, 0);
        return pendingIntent;
    }

}
