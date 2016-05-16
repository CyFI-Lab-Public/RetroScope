/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Used to display an error dialog from within the Telephony service when an outgoing call fails
 */
public class ErrorDialogActivity extends Activity {
    private static final String TAG = ErrorDialogActivity.class.getSimpleName();

    public static final String SHOW_MISSING_VOICEMAIL_NO_DIALOG_EXTRA = "show_missing_voicemail";
    public static final String ERROR_MESSAGE_ID_EXTRA = "error_message_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final boolean showVoicemailDialog = getIntent().getBooleanExtra(
                SHOW_MISSING_VOICEMAIL_NO_DIALOG_EXTRA, false);

        if (showVoicemailDialog) {
            showMissingVoicemailErrorDialog();
        } else {
            final int error = getIntent().getIntExtra(ERROR_MESSAGE_ID_EXTRA, -1);
            if (error == -1) {
                Log.e(TAG, "ErrorDialogActivity called with no error type extra.");
                finish();
            }
            showGenericErrorDialog(error);
        }
    }

    private void showGenericErrorDialog(int resid) {
        final CharSequence msg = getResources().getText(resid);

        final DialogInterface.OnClickListener clickListener;

        final DialogInterface.OnCancelListener cancelListener;
        clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
        cancelListener = new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        };

        final AlertDialog errorDialog = new AlertDialog.Builder(this)
                .setMessage(msg).setPositiveButton(R.string.ok, clickListener)
                        .setOnCancelListener(cancelListener).create();

        errorDialog.show();
    }

    private void showMissingVoicemailErrorDialog() {
        final AlertDialog missingVoicemailDialog = new AlertDialog.Builder(this)
        .setTitle(R.string.no_vm_number)
        .setMessage(R.string.no_vm_number_msg)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dontAddVoiceMailNumber();
                }})
        .setNegativeButton(R.string.add_vm_number_str, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    addVoiceMailNumberPanel(dialog);
                }})
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dontAddVoiceMailNumber();
                }}).show();
    }


    private void addVoiceMailNumberPanel(DialogInterface dialog) {
        if (dialog != null) {
            dialog.dismiss();
        }

        // navigate to the Voicemail setting in the Call Settings activity.
        Intent intent = new Intent(CallFeaturesSetting.ACTION_ADD_VOICEMAIL);
        intent.setClass(this, CallFeaturesSetting.class);
        startActivity(intent);
        finish();
    }

    private void dontAddVoiceMailNumber() {
        finish();
    }
}
