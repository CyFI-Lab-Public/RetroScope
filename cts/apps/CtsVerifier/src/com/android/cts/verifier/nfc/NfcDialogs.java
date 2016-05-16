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

package com.android.cts.verifier.nfc;

import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;

/** Class containing methods to create common dialogs for NFC activities. */
public class NfcDialogs {

    static AlertDialog createNotEnabledDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.nfc_not_enabled)
                .setMessage(R.string.nfc_not_enabled_message)
                .setPositiveButton(R.string.nfc_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .create();
    }

    static AlertDialog createNdefPushNotEnabledDialog(final Context context) {
        return new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.ndef_push_not_enabled)
                .setMessage(R.string.ndef_push_not_enabled_message)
                .setPositiveButton(R.string.ndef_push_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .create();
    }

    public static AlertDialog createHceTapReaderDialog(final Context context, String message) {
        String baseString = context.getString(R.string.nfc_hce_tap_reader_message);
        return new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.nfc_hce_tap_reader_title)
                .setMessage(message != null ? message + "\n\n" + baseString : baseString)
                .setPositiveButton("OK", null)
                .create();
    }
    private NfcDialogs() {
    }
}
