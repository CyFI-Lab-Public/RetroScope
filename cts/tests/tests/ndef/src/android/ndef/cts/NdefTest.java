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

package android.ndef.cts;

import java.nio.charset.Charset;
import java.util.Arrays;

import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.FormatException;

import junit.framework.TestCase;

/**
 * NDEF is a NFC-Forum defined data format.<p>
 * NDEF is often used with NFC, but it is just a data format and no NFC
 * hardware is required, so these API's are mandatory even on Android
 * devices without NFC hardware.
 */
public class NdefTest extends TestCase {
    static final Charset ASCII = Charset.forName("US-ASCII");
    static final Charset UTF8 = Charset.forName("UTF-8");

    public void testConstructor() {
        NdefRecord r;

        r = new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null);
        assertEquals(new byte[0], r.getId());
        assertEquals(new byte[0], r.getType());
        assertEquals(new byte[0], r.getPayload());
        assertEquals(NdefRecord.TNF_EMPTY, r.getTnf());
    }

    public void testEquals() {
        assertEquals(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null));
        assertEquals(
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9}),
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9}));
        assertNotEquals(
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9}),
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9,10}));
        assertEquals(
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})),
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})));
        assertNotEquals(
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})),
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9,10})));
        assertNotEquals(
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})),
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9}),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)));

        // test hashCode
        assertEquals(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null).hashCode(),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null).hashCode());
        assertEquals(
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})).hashCode(),
                new NdefMessage(new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE,
                new byte[] {1,2,3}, new byte[] {4,5,6}, new byte[] {7,8,9})).hashCode());
    }

    public void testInvalidParsing() throws FormatException {
        final byte[][] invalidNdefMessages = {
            {},                                    // too short
            {(byte)0xD0},                          // too short
            {(byte)0xD0, 0},                       // too short
            {(byte)0xD0, 0, 0, 0, 0},              // too long
            {(byte)0x50, 0, 0},                    // missing MB
            {(byte)0x90, 0, 0},                    // missing ME
            {(byte)0xC0, 0, 0, 0},                 // long record, too short
            {(byte)0xC0, 0, 0, 0, 0},              // long record, too short
            {(byte)0xC0, 0, 0, 0, 0, 0, 0},        // long record, too long
            {(byte)0xD8, 1, 3, 1, 0, 0, 0, 0},     // SR w/ payload&type&id, too short
            {(byte)0xD8, 1, 3, 1, 0, 0, 0, 0, 0, 0}, // SR w/ payload&type&id, too long
            {(byte)0xD8, 0, 0, 1, 0},              // TNF_EMPTY cannot have id field
            {(byte)0x90, 0, 0, (byte)0x10, 0, 0},  // 2 records, missing ME
            {(byte)0xF5, 0, 0},                    // CF and ME set
            {(byte)0xD6, 0, 0},                    // TNF_UNCHANGED without chunking
            {(byte)0xB6, 0, 1, 1, (byte)0x56, 0, 1, 2}, // TNF_UNCHANGED in first chunk
            {(byte)0xC5, 0, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF},  // heap-smash check
        };

        for (byte[] b : invalidNdefMessages) {
            try {
                new NdefMessage(b);
                fail("expected FormatException for input " + bytesToString(b));
            } catch (FormatException e) { }
        }
    }

    public void testValidParsing() throws FormatException {
        // short record
        assertEquals(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)),
                new NdefMessage(new byte[] {(byte)0xD0, 0, 0}));

        // full length record
        assertEquals(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)),
                new NdefMessage(new byte[] {(byte)0xC0, 0, 0, 0, 0, 0}));

        // SR with ID flag and 0-length id
        assertEquals(new NdefMessage(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)),
                new NdefMessage(new byte[] {(byte)0xD8, 0, 0, 0}));

        // SR with ID flag and 1-length id
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, null, new byte[] {0}, null)),
                new NdefMessage(new byte[] {(byte)0xD9, 0, 0, 1, 0}));

        // ID flag and 1-length id
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, null, new byte[] {0}, null)),
                new NdefMessage(new byte[] {(byte)0xC9, 0, 0, 0, 0, 0, 1, 0}));

        // SR with payload
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, null, null, new byte[] {1, 2, 3})),
                new NdefMessage(new byte[] {(byte)0xD1, 0, 3, 1, 2, 3}));

        // SR with payload and type
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN, new byte[] {9}, null, new byte[] {1, 2, 3})),
                new NdefMessage(new byte[] {(byte)0xD1, 1, 3, 9, 1, 2, 3}));

        // SR with payload, type and id
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN, new byte[] {8}, new byte[] {9}, new byte[] {1, 2, 3})),
                new NdefMessage(new byte[] {(byte)0xD9, 1, 3, 1, 8, 9, 1, 2, 3}));

        // payload, type and id
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN, new byte[] {8}, new byte[] {9}, new byte[] {1, 2, 3})),
                new NdefMessage(new byte[] {(byte)0xC9, 1, 0, 0, 0, 3, 1, 8, 9, 1, 2, 3}));

        // 2 records
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)),
                new NdefMessage(new byte[] {(byte)0x90, 0, 0, (byte)0x50, 0, 0}));

        // 3 records
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)),
                new NdefMessage(new byte[] {(byte)0x90, 0, 0, (byte)0x10, 0, 0, (byte)0x50, 0, 0}));

        // chunked record (2 chunks)
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, new byte[] {1, 2})),
                new NdefMessage(new byte[] {(byte)0xB5, 0, 1, 1, (byte)0x56, 0, 1, 2}));

        // chunked record (3 chunks)
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_UNKNOWN, null, null, new byte[] {1, 2})),
                new NdefMessage(
                        new byte[] {(byte)0xB5, 0, 0, (byte)0x36, 0, 1, 1, (byte)0x56, 0, 1, 2}));

        // chunked with id and type
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, new byte[] {8}, new byte[] {9}, new byte[] {1, 2})),
                new NdefMessage(new byte[] {(byte)0xBA, 1, 0, 1, 8, 9, (byte)0x36, 0, 1, 1,
                        (byte)0x56, 0, 1, 2}));

        // 3 records, 7 chunks
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, null, null, new byte[] {1,2,3,4}),
                new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, null, null, new byte[] {11,12,13,14})),
                new NdefMessage(new byte[] {
                        (byte)0xB4, 0, 1, 1, (byte)0x36, 0, 2, 2, 3, (byte)0x16, 0, 1, 4,
                        (byte)0x10, 0, 0,
                        (byte)0x32, 0, 2, 11, 12, (byte)0x36, 0, 1, 13, (byte)0x56, 0, 1, 14
                }));

        // 255 byte payload
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, new byte[] {
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7})),
                new NdefMessage(new byte[] {(byte)0xC5, 0, 0, 0, 0, (byte)0xFF,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7}));

        // 256 byte payload
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, new byte[] {
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,})),
                new NdefMessage(new byte[] {(byte)0xC5, 0, 0, 0, 1, 0,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,}));

        // 255 byte type
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, new byte[] {
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7},
                        null, null)),
                new NdefMessage(new byte[] {(byte)0xD2, (byte)0xFF, 0,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,}));

        // 255 byte id
        assertEquals(new NdefMessage(
                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, null, new byte[] {
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7},
                        null)),
                new NdefMessage(new byte[] {(byte)0xDA, 0, 0, (byte)0xFF,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                        1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,}));
                // NdefRecord parsing ignores incorrect MB
        assertEquals(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(new byte[] {(byte)0x50, 0, 0}));

        // NdefRecord parsing ignores incorrect ME
        assertEquals(new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null),
                new NdefRecord(new byte[] {(byte)0x90, 0, 0}));

        // NdefRecord parsing can handle chunking with incorrect MB, ME
        assertEquals(new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, new byte[] {1, 2}),
                new NdefRecord(new byte[] {(byte)0x35, 0, 1, 1, (byte)0x16, 0, 1, 2}));

        //A Smart Poster containing a URL and no text (nested NDEF Messages) */
        assertEquals(new NdefMessage(new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null,
                new NdefMessage(NdefRecord.createUri("http://www.google.com")).toByteArray())),
                new NdefMessage(new byte[] {
                (byte) 0xd1, (byte) 0x02, (byte) 0x0f, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
                (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67, (byte) 0x6f,
                (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e, (byte) 0x63,
                (byte) 0x6f, (byte) 0x6d}));
    }

    public void testCreateUri() {
        assertEquals(new byte[] {
                (byte)0xD1, 1, 8, 'U', (byte)0x01, 'n', 'f', 'c', '.', 'c', 'o', 'm'},
                new NdefMessage(
                        NdefRecord.createUri(Uri.parse("http://www.nfc.com"))).toByteArray());

        assertEquals(new byte[] {(byte)0xD1, 1, 13, 'U', (byte)0x05,
                '+', '3', '5', '8', '9', '1', '2', '3', '4', '5', '6', '7'},
                new NdefMessage(NdefRecord.createUri("tel:+35891234567")).toByteArray());

        assertEquals(new byte[] {
                (byte)0xD1, 1, 4, 'U', (byte)0x00, 'f', 'o', 'o'},
                new NdefMessage(NdefRecord.createUri("foo")).toByteArray());

        // make sure UTF-8 encoding is used
        assertEquals(new byte[] {
                (byte)0xD1, 1, 3, 'U', (byte)0x00, (byte)0xC2, (byte)0xA2},
                new NdefMessage(NdefRecord.createUri("\u00A2")).toByteArray());
    }

    public void testCreateMime() {
        assertEquals(
                new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(ASCII), null,
                        "foo".getBytes()),
                NdefRecord.createMime("text/plain",  "foo".getBytes()));

        try {
            NdefRecord.createMime("", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        try {
            NdefRecord.createMime("/", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        try {
            NdefRecord.createMime("a/", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        try {
            NdefRecord.createMime("/b", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        // The following are valid MIME types and should not throw
        NdefRecord.createMime("foo/bar", null);
        NdefRecord.createMime("   ^@#/*   ", null);
        NdefRecord.createMime("text/plain; charset=us_ascii", null);
    }

    public void testCreateExternal() {
        try {
            NdefRecord.createExternal("", "c", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        try {
            NdefRecord.createExternal("a", "", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        try {
            NdefRecord.createExternal("   ", "c", null);
            fail("IllegalArgumentException not throw");
        } catch (IllegalArgumentException e) { }

        assertEquals(
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, "a.b:c".getBytes(ASCII), null, null),
                NdefRecord.createExternal("a.b", "c", null));

        // test force lowercase
        assertEquals(
                new NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, "a.b:c!".getBytes(ASCII), null, null),
                NdefRecord.createExternal("A.b", "C!", null));
    }

    public void testCreateApplicationRecord() throws FormatException {
        NdefMessage m;
        NdefRecord r;

        // some failure cases
        try {
            NdefRecord.createApplicationRecord(null);
            fail("NullPointerException not thrown");
        } catch (NullPointerException e) {}
        try {
            NdefRecord.createApplicationRecord("");
            fail("IllegalArgumentException not thrown");
        } catch (IllegalArgumentException e) {}

        // create an AAR
        r = NdefRecord.createApplicationRecord("com.foo.bar");
        assertEquals(new byte[] {
                (byte)0xd4, (byte)0x0f, (byte)0x0b, (byte)0x61,
                (byte)0x6e, (byte)0x64, (byte)0x72, (byte)0x6f,
                (byte)0x69, (byte)0x64, (byte)0x2e, (byte)0x63,
                (byte)0x6f, (byte)0x6d, (byte)0x3a, (byte)0x70,
                (byte)0x6b, (byte)0x67, (byte)0x63, (byte)0x6f,
                (byte)0x6d, (byte)0x2e, (byte)0x66, (byte)0x6f,
                (byte)0x6f, (byte)0x2e, (byte)0x62, (byte)0x61,
                (byte)0x72,},
                r.toByteArray());

        // parse an AAR
        m = new NdefMessage(new byte[] {
                (byte)0xd4, (byte)0x0f, (byte)0x0b, (byte)0x61,
                (byte)0x6e, (byte)0x64, (byte)0x72, (byte)0x6f,
                (byte)0x69, (byte)0x64, (byte)0x2e, (byte)0x63,
                (byte)0x6f, (byte)0x6d, (byte)0x3a, (byte)0x70,
                (byte)0x6b, (byte)0x67, (byte)0x63, (byte)0x6f,
                (byte)0x6d, (byte)0x2e, (byte)0x66, (byte)0x6f,
                (byte)0x6f, (byte)0x2e, (byte)0x62, (byte)0x61,
                (byte)0x72});
        NdefRecord[] rs = m.getRecords();
        assertEquals(1, rs.length);
        r = rs[0];
        assertEquals(NdefRecord.TNF_EXTERNAL_TYPE, r.getTnf());
        assertEquals("android.com:pkg".getBytes(), r.getType());
        assertEquals(new byte[] {}, r.getId());
        assertEquals("com.foo.bar".getBytes(), r.getPayload());
    }

    public void testToByteArray() throws FormatException {
        NdefRecord r;

        // single short record
        assertEquals(new byte[] {(byte)0xD0, 0, 0},
                new NdefMessage(
                        new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)).toByteArray());

        // with id
        assertEquals(new byte[] {(byte)0xDD, 0, 0, 1, 9},
                new NdefMessage(new NdefRecord(
                        NdefRecord.TNF_UNKNOWN, null, new byte[] {9}, null)).toByteArray());

        // with type
        assertEquals(new byte[] {(byte)0xD4, 1, 0, 9},
                new NdefMessage(new NdefRecord(
                        NdefRecord.TNF_EXTERNAL_TYPE, new byte[] {9}, null, null)).toByteArray());

        // with payload
        assertEquals(new byte[] {(byte)0xD5, 0, 1, 9},
                new NdefMessage(new NdefRecord(
                        NdefRecord.TNF_UNKNOWN, null, null, new byte[] {9})).toByteArray());

        // 3 records
        r = new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null);
        assertEquals(new byte[] {(byte)0x90, 0, 0, (byte)0x10, 0, 0, (byte)0x50, 0, 0},
                new NdefMessage(r, r, r).toByteArray());

        // 256 byte payload
        assertEquals(new byte[] {(byte)0xC5, 0, 0, 0, 1, 0,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,},
                new NdefMessage(new NdefRecord(NdefRecord.TNF_UNKNOWN, null, null, new byte[] {
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,
                1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8,})).toByteArray());
    }

    public void testToUri() {
        // absolute uri
        assertEquals(Uri.parse("http://www.android.com"),
                new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                "http://www.android.com".getBytes(), null, null).toUri());
        // wkt uri
        assertEquals(Uri.parse("http://www.android.com"),
                NdefRecord.createUri("http://www.android.com").toUri());
        // smart poster with absolute uri
        assertEquals(Uri.parse("http://www.android.com"),
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null,
                new NdefMessage(new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI,
                "http://www.android.com".getBytes(), null, null)).toByteArray()).toUri());
        // smart poster with wkt uri
        assertEquals(Uri.parse("http://www.android.com"),
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null,
                new NdefMessage(
                NdefRecord.createUri("http://www.android.com")).toByteArray()).toUri());
        // smart poster with text and wkt uri
        assertEquals(Uri.parse("http://www.android.com"),
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_SMART_POSTER, null,
                new NdefMessage(new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, null,
                null), NdefRecord.createUri("http://www.android.com")).toByteArray()).toUri());
        // external type
        assertEquals(Uri.parse("vnd.android.nfc://ext/com.foo.bar:type"),
                NdefRecord.createExternal("com.foo.bar", "type", null).toUri());
        // check normalization
        assertEquals(Uri.parse("http://www.android.com"),
                new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, "HTTP://www.android.com".getBytes(),
                null, null).toUri());

        // not uri's
        assertEquals(null, NdefRecord.createMime("text/plain", null).toUri());
        assertEquals(null, new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null).toUri());
    }

    public void testToMimeType() {
        assertEquals(null, NdefRecord.createUri("http://www.android.com").toMimeType());
        assertEquals(null, new NdefRecord(NdefRecord.TNF_EMPTY, null, null, null).toMimeType());
        assertEquals(null, NdefRecord.createExternal("com.foo.bar", "type", null).toMimeType());

        assertEquals("a/b", NdefRecord.createMime("a/b", null).toMimeType());
        assertEquals("text/plain", new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                null, null).toMimeType());
        assertEquals("a/b", NdefRecord.createMime("A/B", null).toMimeType());
        assertEquals("a/b", new NdefRecord(NdefRecord.TNF_MIME_MEDIA, " A/B ".getBytes(),
                null, null).toMimeType());
    }

    static void assertEquals(byte[] expected, byte[] actual) {
        assertTrue("expected equals:<" + bytesToString(expected) + "> was:<" +
                bytesToString(actual) + ">", Arrays.equals(expected, actual));
    }

    static void assertNotEquals(Object expected, Object actual) {
        assertFalse("expected not equals:<" + expected + "> was:<" + actual + ">",
                expected.equals(actual));
    }

    static String bytesToString(byte[] bs) {
        StringBuilder s = new StringBuilder();
        for (byte b : bs) {
            s.append(String.format("%02X ", b));
        }
        return s.toString();
    }
}
