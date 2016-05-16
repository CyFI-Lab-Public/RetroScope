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

import android.app.Dialog;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.widget.TextView;

import java.nio.charset.Charset;

/**
 * Test activity that sends a particular NDEF Push message to another NFC device.
 */
public class NdefPushSenderActivity extends PassFailButtons.Activity {

    static final NdefMessage TEST_MESSAGE = getTestMessage();

    private static final int NFC_NOT_ENABLED_DIALOG_ID = 1;
    private static final int NDEF_PUSH_NOT_ENABLED_DIALOG_ID = 2;

    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_text);
        setInfoResources(R.string.nfc_ndef_push_sender, R.string.nfc_ndef_push_sender_info, 0);
        setPassFailButtonClickListeners();

        TextView text = (TextView) findViewById(R.id.text);
        text.setText(R.string.nfc_ndef_push_sender_instructions);

        NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
        mNfcAdapter = nfcManager.getDefaultAdapter();
    }

    private static NdefMessage getTestMessage() {
        byte[] mimeBytes = "application/com.android.cts.verifier.nfc"
                .getBytes(Charset.forName("US-ASCII"));
        byte[] id = new byte[] {1, 3, 3, 7};
        byte[] payload = "CTS Verifier NDEF Push Tag".getBytes(Charset.forName("US-ASCII"));
        return new NdefMessage(new NdefRecord[] {
                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, id, payload)
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mNfcAdapter.isEnabled()) {
            showDialog(NFC_NOT_ENABLED_DIALOG_ID);
        } else if (!mNfcAdapter.isNdefPushEnabled()) {
            /* Sender must have NDEF push enabled */
            showDialog(NDEF_PUSH_NOT_ENABLED_DIALOG_ID);
        }

        mNfcAdapter.enableForegroundNdefPush(this, TEST_MESSAGE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundNdefPush(this);
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case NFC_NOT_ENABLED_DIALOG_ID:
                return NfcDialogs.createNotEnabledDialog(this);
            case NDEF_PUSH_NOT_ENABLED_DIALOG_ID:
                return NfcDialogs.createNdefPushNotEnabledDialog(this);
            default:
                return super.onCreateDialog(id, args);
        }
    }
}
