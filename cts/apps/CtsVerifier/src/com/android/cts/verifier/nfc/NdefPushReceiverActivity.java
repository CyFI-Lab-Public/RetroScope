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

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.tech.NfcUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;

/**
 * Test activity that waits to receive a particular NDEF Push message from another NFC device.
 */
public class NdefPushReceiverActivity extends PassFailButtons.Activity {

    private static final int NFC_NOT_ENABLED_DIALOG_ID = 1;

    private static final int RESULT_DIALOG_ID = 2;

    private static final String IS_MATCH_ARG = "isMatch";

    private NfcAdapter mNfcAdapter;

    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setInfoResources(R.string.nfc_ndef_push_receiver, R.string.nfc_ndef_push_receiver_info, 0);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(R.string.nfc_ndef_push_receiver_instructions);

        NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
        mNfcAdapter = nfcManager.getDefaultAdapter();
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mNfcAdapter.isEnabled()) {
            showDialog(NFC_NOT_ENABLED_DIALOG_ID);
        }

        /* Only the sender requires mNfcAdapter.isNdefPushEnabled == true,
         * so no need to check it here in the receiver. */

        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        NdefMessage[] messages = getNdefMessages(intent);
        boolean isMatch = messages != null
                && messages.length > 0
                && NfcUtils.areMessagesEqual(messages[0], NdefPushSenderActivity.TEST_MESSAGE);

        getPassButton().setEnabled(isMatch);

        Bundle args = new Bundle();
        args.putBoolean(IS_MATCH_ARG, isMatch);
        showDialog(RESULT_DIALOG_ID, args);
    }

    private NdefMessage[] getNdefMessages(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }
            return messages;
        } else {
            return null;
        }
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case NFC_NOT_ENABLED_DIALOG_ID:
                return NfcDialogs.createNotEnabledDialog(this);

            case RESULT_DIALOG_ID:
                // Set placeholder titles and messages for now. Final titles and messages will
                // be set in onPrepareDialog.
                return new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.nfc_result_failure)
                        .setMessage("")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();

            default:
                return super.onCreateDialog(id, args);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
            case RESULT_DIALOG_ID:
                boolean isMatch = args.getBoolean(IS_MATCH_ARG);
                AlertDialog alert = (AlertDialog) dialog;
                alert.setTitle(isMatch
                        ? R.string.nfc_result_success
                        : R.string.nfc_result_failure);
                alert.setMessage(isMatch
                        ? getString(R.string.nfc_ndef_push_receive_success)
                        : getString(R.string.nfc_ndef_push_receive_failure));
                break;

            default:
                super.onPrepareDialog(id, dialog, args);
                break;
        }
    }
}
