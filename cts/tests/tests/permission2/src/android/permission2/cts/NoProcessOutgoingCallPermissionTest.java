/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.permission2.cts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

/**
 * Verify that processing outgoing calls requires Permission.
 */
public class NoProcessOutgoingCallPermissionTest extends AndroidTestCase {

    // Time to wait for call to be placed.
    private static final int WAIT_TIME = 2 * 60 * 1000;

    private static final String LOG_TAG = "NoProcessOutgoingCallPermissionTest";

    private void callPhone() {
        Uri uri = Uri.parse("tel:123456");
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        Log.i(LOG_TAG, "Called phone: " + uri.toString());
    }

    /**
     * Verify that to process an outgoing call requires Permission.
     * <p>Tests Permission:
     *   {@link android.Manifest.permission#PROCESS_OUTGOING_CALL}
     */
    // TODO: add back to LargeTest when test can cancel initiated call
    public void testProcessOutgoingCall() {
        Log.i(LOG_TAG, "Beginning testProcessOutgoingCall");
        OutgoingCallBroadcastReceiver rcvr = new OutgoingCallBroadcastReceiver();
        Intent ntnt = mContext.registerReceiver(rcvr,
            new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL));
        Log.i(LOG_TAG, "registerReceiver --> " + ntnt);
        if (null != ntnt) {
            Bundle xtrs = ntnt.getExtras();
            Log.i(LOG_TAG, "extras --> " + xtrs.toString());
        }

        callPhone();
        synchronized(rcvr) {
            try {
                rcvr.wait(WAIT_TIME);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "wait for phone call interrupted");
            }
        }
        // TODO: Find a way to test that the call was made at all, and assertTrue.
        assertFalse("Outgoing call processed without proper permissions", rcvr.callReceived);
    }

    public class OutgoingCallBroadcastReceiver extends BroadcastReceiver {
        public boolean callReceived = false;

        public void onReceive(Context context, Intent intent) {
            Bundle xtrs = intent.getExtras();
            Log.e(LOG_TAG, xtrs.toString());
            callReceived = true;
        }
    }

}

