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

package com.android.phone;

import android.app.AlertDialog;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;
import android.view.WindowManager;

/**
 * Helper class for displaying the DisplayInfo sent by CDMA network.
 */
public class CdmaDisplayInfo {
    private static final String LOG_TAG = "CdmaDisplayInfo";
    private static final boolean DBG = (SystemProperties.getInt("ro.debuggable", 0) == 1);

    /** CDMA DisplayInfo dialog */
    private static AlertDialog sDisplayInfoDialog = null;

    /**
     * Handle the DisplayInfo record and display the alert dialog with
     * the network message.
     *
     * @param context context to get strings.
     * @param infoMsg Text message from Network.
     */
    public static void displayInfoRecord(Context context, String infoMsg) {

        if (DBG) log("displayInfoRecord: infoMsg=" + infoMsg);

        if (sDisplayInfoDialog != null) {
            sDisplayInfoDialog.dismiss();
        }

        // displaying system alert dialog on the screen instead of
        // using another activity to display the message.  This
        // places the message at the forefront of the UI.
        sDisplayInfoDialog = new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(context.getText(R.string.network_message))
                .setMessage(infoMsg)
                .setCancelable(true)
                .create();

        sDisplayInfoDialog.getWindow().setType(
                WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        sDisplayInfoDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        sDisplayInfoDialog.show();
        PhoneGlobals.getInstance().wakeUpScreen();

    }

    /**
     * Dismiss the DisplayInfo record
     */
    public static void dismissDisplayInfoRecord() {

        if (DBG) log("Dissmissing Display Info Record...");

        if (sDisplayInfoDialog != null) {
            sDisplayInfoDialog.dismiss();
            sDisplayInfoDialog = null;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, "[CdmaDisplayInfo] " + msg);
    }
}
