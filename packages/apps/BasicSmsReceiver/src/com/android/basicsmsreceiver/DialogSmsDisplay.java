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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DialogSmsDisplay extends Activity {
    private static final String LOG_TAG = "SmsReceivedDialog";

    private static final int DIALOG_SHOW_MESSAGE = 1;

    public static final String SMS_FROM_ADDRESS_EXTRA =
                                                "com.android.basicsmsreceiver.SMS_FROM_ADDRESS";
    public static final String SMS_MESSAGE_EXTRA =
                                                "com.android.basicsmsreceiver.SMS_MESSAGE";
    public static final String SMS_NOTIFICATION_ID_EXTRA =
                                                "com.android.basicsmsreceiver.NOTIFICATION_ID";

    // Visible to unit tests
    String mFromAddress;
    String mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        parseIntent(getIntent());
    }

    private void parseIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        mFromAddress = extras.getString(SMS_FROM_ADDRESS_EXTRA);
        mMessage = extras.getString(SMS_MESSAGE_EXTRA);
        int notificationId = extras.getInt(SMS_NOTIFICATION_ID_EXTRA);

        Log.i(LOG_TAG, "notificationId: " + notificationId);

        // Dismiss the notification that brought us here.
        NotificationManager notificationManager =
            (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);

        showDialog(DIALOG_SHOW_MESSAGE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        removeDialog(DIALOG_SHOW_MESSAGE);

        parseIntent(intent);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_SHOW_MESSAGE:
                return new AlertDialog.Builder(this)
                        .setTitle(String.format(getString(R.string.sms_message_from_format),
                                mFromAddress))
                        .setMessage(mMessage)
                        .setCancelable(true)
                        .setOnCancelListener(new AlertDialog.OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNeutralButton(R.string.sms_done_button,
                                new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .create();
        }
        return null;
    }
}
