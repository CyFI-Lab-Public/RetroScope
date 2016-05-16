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

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.util.Arrays;

/** Class with utility methods for testing equality of messages and displaying byte payloads. */
public class NfcUtils {

    public static boolean areMessagesEqual(NdefMessage message, NdefMessage otherMessage) {
        return message != null && otherMessage != null
                && areRecordArraysEqual(message.getRecords(), otherMessage.getRecords());
    }

    private static boolean areRecordArraysEqual(NdefRecord[] records, NdefRecord[] otherRecords) {
        if (records.length == otherRecords.length) {
            for (int i = 0; i < records.length; i++) {
                if (!areRecordsEqual(records[i], otherRecords[i])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean areRecordsEqual(NdefRecord record, NdefRecord otherRecord) {
        return Arrays.equals(record.toByteArray(), otherRecord.toByteArray());
    }

    static CharSequence displayByteArray(byte[] bytes) {
        StringBuilder builder = new StringBuilder().append("[");
        for (int i = 0; i < bytes.length; i++) {
            builder.append(Byte.toString(bytes[i]));
            if (i + 1 < bytes.length) {
                builder.append(", ");
            }
        }
        return builder.append("]");
    }
}
