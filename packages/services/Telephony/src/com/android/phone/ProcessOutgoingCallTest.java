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

package com.android.phone;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * ProcessOutgoingCallTest tests {@link OutgoingCallBroadcaster} by performing
 * a couple of simple modifications to outgoing calls, and by printing log
 * messages for each call.
 */
public class ProcessOutgoingCallTest extends BroadcastReceiver {
    private static final String TAG = "ProcessOutgoingCallTest";
    private static final String AREACODE = "617";

    private static final boolean LOGV = false;

    private static final boolean REDIRECT_411_TO_GOOG411 = true;
    private static final boolean SEVEN_DIGIT_DIALING = true;
    private static final boolean POUND_POUND_SEARCH = true;
    private static final boolean BLOCK_555 = true;

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (LOGV) Log.v(TAG, "Received intent " + intent + " (number = " + number + ".");
            /* Example of how to redirect calls from one number to another. */
            if (REDIRECT_411_TO_GOOG411 && number.equals("411")) {
                setResultData("18004664411");
            }

            /* Example of how to modify the phone number in flight. */
            if (SEVEN_DIGIT_DIALING && number.length() == 7) {
                setResultData(AREACODE + number);
            }

            /* Example of how to route a call to another Application. */
            if (POUND_POUND_SEARCH && number.startsWith("##")) {
                Intent newIntent = new Intent(Intent.ACTION_SEARCH);
                newIntent.putExtra(SearchManager.QUERY, number.substring(2));
                newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(newIntent);
                setResultData(null);
            }

            /* Example of how to deny calls to a particular number.
             * Note that no UI is displayed to the user -- the call simply 
             * does not happen.  It is the application's responaibility to
             * explain this to the user. */
            int length = number.length();
            if (BLOCK_555 && length >= 7) {
                String exchange = number.substring(length - 7, length - 4);
                Log.v(TAG, "exchange = " + exchange);
                if (exchange.equals("555")) {
                    setResultData(null);
                }
            }
        }
    }
}
