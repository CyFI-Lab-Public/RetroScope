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

package com.android.cts.verifier.nfc.tech;

import com.android.cts.verifier.R;

import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * {@link TagTester} for NDEF tags. It writes a semi-random NDEF tag with a random id but
 * constant mime type and payload.
 */
public class NdefTagTester implements TagTester {

    private static final String TAG = NdefTagTester.class.getSimpleName();

    private static final String MIME_TYPE = "application/com.android.cts.verifier.nfc";

    private static final String PAYLOAD = "CTS Verifier NDEF Tag";

    private final Context mContext;

    public NdefTagTester(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isTestableTag(Tag tag) {
        if (tag != null) {
            for (String tech : tag.getTechList()) {
                if (tech.equals(Ndef.class.getName())) {
                    Ndef ndef = Ndef.get(tag);
                    return ndef != null && ndef.isWritable();
                }
            }
        }
        return false;
    }

    @Override
    public TagVerifier writeTag(Tag tag) throws IOException, FormatException {
        Random random = new Random();
        NdefRecord mimeRecord = createRandomMimeRecord(random);
        NdefRecord[] expectedRecords = new NdefRecord[] {mimeRecord};

        final NdefMessage expectedMessage = new NdefMessage(expectedRecords);
        writeMessage(tag, expectedMessage);

        final String expectedContent = mContext.getString(R.string.nfc_ndef_content,
                NfcUtils.displayByteArray(mimeRecord.getId()), MIME_TYPE, PAYLOAD);

        return new TagVerifier() {
            @Override
            public Result verifyTag(Tag tag) throws IOException, FormatException {
                String actualContent;
                NdefMessage message = readMessage(tag);
                NdefRecord[] records = message.getRecords();

                if (records.length > 0) {
                    NdefRecord record = records[0];
                    actualContent = mContext.getString(R.string.nfc_ndef_content,
                            NfcUtils.displayByteArray(record.getId()),
                            new String(record.getType(), Charset.forName("US-ASCII")),
                            new String(record.getPayload(), Charset.forName("US-ASCII")));
                } else {
                    actualContent = null;
                }

                return new Result(expectedContent, actualContent,
                        NfcUtils.areMessagesEqual(message, expectedMessage));
            }
        };
    }

    private NdefRecord createRandomMimeRecord(Random random) {
        byte[] mimeBytes = MIME_TYPE.getBytes(Charset.forName("US-ASCII"));
        byte[] id = new byte[4];
        random.nextBytes(id);
        byte[] payload = PAYLOAD.getBytes(Charset.forName("US-ASCII"));
        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeBytes, id, payload);
    }

    private void writeMessage(Tag tag, NdefMessage message) throws IOException, FormatException {
        Ndef ndef = null;
        try {
            ndef = Ndef.get(tag);
            ndef.connect();
            ndef.writeNdefMessage(message);
        } finally {
            if (ndef != null) {
                try {
                    ndef.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException while closing NDEF...", e);
                }
            }
        }
    }

    private NdefMessage readMessage(Tag tag) throws IOException, FormatException {
        Ndef ndef = null;
        try {
            ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                return ndef.getNdefMessage();
            }
        } finally {
            if (ndef != null) {
                try {
                    ndef.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing Ndef...", e);
                }
            }
        }
        return null;
    }
}