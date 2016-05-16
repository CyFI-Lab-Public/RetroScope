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

package com.android.basicsmsreceiver;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

/**
 * Various instrumentation tests for BasicSmsReceiver.
 *
 * To run this test: runtest basicsmsreceiver
 *
 * TODO: write tests that verify that notifications get created. As of now, I don't see a way
 * to query the NotificationManager for what notifications are there. I only see methods to
 * post and cancel notifications.
 *
 */
public class DialogSmsDisplayTests
        extends ActivityInstrumentationTestCase2<DialogSmsDisplay> {

    private boolean mHasSms;
    private String mMyNumber;       // phone number of this device
    public static final String ACTION_SMS_SENT =
        "com.android.basicsmsreceiver.tests.SMS_SENT_ACTION";
    private static String TAG = "DialogSmsDisplayTests";
    private List<String> mReceivedMessages = new ArrayList<String>();
    private String mReceivedSender;
    private DialogSmsDisplay dialogSmsDisplayActivity;
    private int mMessageReceivedCount;
    private BroadcastReceiver mSmsSenderReceiver;
    private BroadcastReceiver mSmsReceiverReceiver;

    public DialogSmsDisplayTests() {
        super("com.android.basicsmsreceiver", DialogSmsDisplay.class);
        Log.i(TAG, "DialogSmsDisplayTests");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        dialogSmsDisplayActivity = (DialogSmsDisplay)getActivity();

        TelephonyManager telephonyManager = ((TelephonyManager)dialogSmsDisplayActivity
                .getSystemService(Context.TELEPHONY_SERVICE));
        mHasSms = true;     //telephonyManager.isSmsCapable();
        mMyNumber = telephonyManager.getLine1Number();

        Log.i(TAG, "hasSms: " + mHasSms + " my number: " + mMyNumber);

        assertTrue("SMS must be enabled on the device", mHasSms);
        assertNotNull("Device does not have a phone number", mMyNumber);

        if (mHasSms) {
            // Register broadcast receivers for SMS sent and delivered intents
            mSmsSenderReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = null;
                    boolean error = true;
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            message = "Message sent!";
                            error = false;
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            message = "Error.";
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            message = "Error: No SMS service.";
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            message = "Error: Null PDU.";
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            message = "Error: Radio off.";
                            break;
                    }
                    assertFalse(message, error);
                }
            };
            dialogSmsDisplayActivity.registerReceiver(mSmsSenderReceiver,
                    new IntentFilter(ACTION_SMS_SENT));

            // Register broadcast receivers for received SMS
            mSmsReceiverReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Bundle extras = intent.getExtras();
                    Log.i(TAG, "onReceive");
                    if (extras == null)
                        return;

                    Object[] pdus = (Object[]) extras.get("pdus");

                    for (int i = 0; i < pdus.length; i++) {
                        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        String sender = message.getOriginatingAddress();
                        if (mReceivedSender != null) {
                            assertEquals(mReceivedSender, sender);
                        } else {
                            mReceivedSender = sender;
                        }
                        mReceivedMessages.add(message.getMessageBody().toString());

                        Log.i(TAG, "From: " + mReceivedSender + " message: " +
                                mReceivedMessages.get(mReceivedMessages.size() - 1));
                    }
                }
            };
            dialogSmsDisplayActivity.registerReceiver(mSmsReceiverReceiver,
                    new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mSmsSenderReceiver != null) {
            dialogSmsDisplayActivity.unregisterReceiver(mSmsSenderReceiver);
            mSmsSenderReceiver = null;
        }
        if (mSmsReceiverReceiver != null) {
            dialogSmsDisplayActivity.unregisterReceiver(mSmsReceiverReceiver);
            mSmsReceiverReceiver = null;
        }
        super.tearDown();
    }

    // Returns the broken up list of messages for long messages or the original message in
    // element 0.
    private List<String> sendSmsMessageInternal(String messageOut) {
        if (!mHasSms) {
            fail("no sms on device");
            return null;
        }
        SmsManager sms = SmsManager.getDefault();

        List<String> messages = sms.divideMessage(messageOut);
        mMessageReceivedCount = 0;
        mReceivedSender = null;
        mReceivedMessages.clear();

        for (String message : messages) {
            Log.i(TAG, "sendSmsMessage: " + messageOut + " to: " + mMyNumber);
            sms.sendTextMessage(mMyNumber, null, message, PendingIntent.getBroadcast(
                    dialogSmsDisplayActivity, 0, new Intent(ACTION_SMS_SENT), 0), null);
        }
        return messages;
    }

    // returns true if "messageCount" sms messages are received, false if timeout
    private boolean waitForSms(int messageCount) {
        // wait up to two minutes for the sent message to be received
        long now = System.currentTimeMillis();
        boolean success = true;
        Log.i(TAG, "waitForSms -- waiting for: " + messageCount + " parts");
        while (mReceivedMessages.size() < messageCount) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
            if (System.currentTimeMillis() - now > 1000 * 2 * 60) {
                // Give up after two minutes
                success = false;
                break;
            }
        }
        if (success) {
            // Wait 5 seconds for the dialog to launch and update
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
            }
        }
        return success;
    }

    private void sendMessageTest(String message) {
        List<String> messages = sendSmsMessageInternal(message);
        Log.i(TAG, "sendMessageTest -- message broken into " + messages.size() + "parts");

        boolean receivedSms = waitForSms(messages.size());
        assertTrue("sms not received after two minutes", receivedSms);

        // Check to see if message/# matches
        assertEquals(mMyNumber, mReceivedSender);
        assertEquals(messages.size(), mReceivedMessages.size());
        int i = 0;
        for (String messageFrag : messages) {
            assertEquals(messageFrag, mReceivedMessages.get(i++));
        }
    }

    public void testSendingSmallSmsMessage() {
        sendMessageTest("This is a regular size message that might be sent");
    }

    public void testSendingLargeSmsMessage() {
        sendMessageTest("This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. " +
                "This is a long long message. ");
    }

    public void testOnNewIntentSmall() {
        // Test a small message and a non-numeric phone number
        sendOnNewIntent("this is a big fat test", "xyzzy", 2000);
    }

    public void testOnNewIntentLarge() {
        // A long message like this should never really happen because SMS messages are limited
        // to about 140 characters.
        sendOnNewIntent("now is the time for all good men to come to the aid of their country1" +
                "now is the time for all good men to come to the aid of their country2" +
                "now is the time for all good men to come to the aid of their country3",
                "513-891-7823", 2001);
    }

    public void sendOnNewIntent(String message, String dest, int notificationId) {
        Intent di = new Intent();
        di.setClass(dialogSmsDisplayActivity, DialogSmsDisplay.class);
        di.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        di.putExtra(DialogSmsDisplay.SMS_FROM_ADDRESS_EXTRA, dest);
        di.putExtra(DialogSmsDisplay.SMS_MESSAGE_EXTRA, message);
        di.putExtra(DialogSmsDisplay.SMS_NOTIFICATION_ID_EXTRA, notificationId);
        dialogSmsDisplayActivity.onNewIntent(di);

        // Check to see if message/# matches
        assertEquals(dest, dialogSmsDisplayActivity.mFromAddress);
        assertEquals(message, dialogSmsDisplayActivity.mMessage);
    }
}
