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

package com.android.nfc.snep;

import android.nfc.FormatException;
import android.nfc.NdefMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class SnepMessage {
    public static final byte VERSION_MAJOR = (byte) 0x1;
    public static final byte VERSION_MINOR = (byte) 0x0;
    public static final byte VERSION = (0xF0 & (VERSION_MAJOR << 4)) | (0x0F & VERSION_MINOR);

    public static final byte REQUEST_CONTINUE = (byte) 0x00;
    public static final byte REQUEST_GET = (byte) 0x01;
    public static final byte REQUEST_PUT = (byte) 0x02;
    public static final byte REQUEST_REJECT = (byte) 0x07;

    public static final byte RESPONSE_CONTINUE = (byte) 0x80;
    public static final byte RESPONSE_SUCCESS = (byte) 0x81;
    public static final byte RESPONSE_NOT_FOUND = (byte) 0xC0;
    public static final byte RESPONSE_EXCESS_DATA = (byte) 0xC1;
    public static final byte RESPONSE_BAD_REQUEST = (byte) 0xC2;
    public static final byte RESPONSE_NOT_IMPLEMENTED = (byte) 0xE0;
    public static final byte RESPONSE_UNSUPPORTED_VERSION = (byte) 0xE1;
    public static final byte RESPONSE_REJECT = (byte) 0xFF;

    private static final int HEADER_LENGTH = 6;
    private final byte mVersion;
    private final byte mField;
    private final int mLength;
    private final int mAcceptableLength;
    private final NdefMessage mNdefMessage;

    public static SnepMessage getGetRequest(int acceptableLength, NdefMessage ndef) {
        return new SnepMessage(VERSION, REQUEST_GET, 4 + ndef.toByteArray().length,
                acceptableLength, ndef);
    }

    public static SnepMessage getPutRequest(NdefMessage ndef) {
        return new SnepMessage(VERSION, REQUEST_PUT, ndef.toByteArray().length, 0, ndef);
    }

    public static SnepMessage getMessage(byte field) {
        return new SnepMessage(VERSION, field, 0, 0, null);
    }

    public static SnepMessage getSuccessResponse(NdefMessage ndef) {
        if (ndef == null) {
            return new SnepMessage(VERSION, RESPONSE_SUCCESS, 0, 0, null);
        } else {
            return new SnepMessage(VERSION, RESPONSE_SUCCESS, ndef.toByteArray().length, 0, ndef);
        }
    }

    public static SnepMessage fromByteArray(byte[] data) throws FormatException {
        return new SnepMessage(data);
    }

    private SnepMessage(byte[] data) throws FormatException {
        ByteBuffer input = ByteBuffer.wrap(data);
        int ndefOffset;
        int ndefLength;

        mVersion = input.get();
        mField = input.get();
        mLength = input.getInt();
        if (mField == REQUEST_GET) {
            mAcceptableLength = input.getInt();
            ndefOffset = HEADER_LENGTH + 4;
            ndefLength = mLength - 4;
        } else {
            mAcceptableLength = -1;
            ndefOffset = HEADER_LENGTH;
            ndefLength = mLength;
        }

        if (ndefLength > 0) {
            byte[] bytes = new byte[ndefLength];
            System.arraycopy(data, ndefOffset, bytes, 0, ndefLength);
            mNdefMessage = new NdefMessage(bytes);
        } else {
            mNdefMessage = null;
        }
    }

    SnepMessage(byte version, byte field, int length, int acceptableLength,
            NdefMessage ndefMessage) {
        mVersion = version;
        mField = field;
        mLength = length;
        mAcceptableLength = acceptableLength;
        mNdefMessage = ndefMessage;
    }

    public byte[] toByteArray() {
        byte[] bytes;
        if (mNdefMessage != null) {
            bytes = mNdefMessage.toByteArray();
        } else {
            bytes = new byte[0];
        }

        ByteArrayOutputStream buffer;
        try {
            if (mField == REQUEST_GET) {
                buffer = new ByteArrayOutputStream(bytes.length + HEADER_LENGTH + 4);
            } else {
                buffer = new ByteArrayOutputStream(bytes.length + HEADER_LENGTH);
            }

            DataOutputStream output = new DataOutputStream(buffer);
            output.writeByte(mVersion);
            output.writeByte(mField);
            if (mField == REQUEST_GET) {
                output.writeInt(bytes.length + 4);
                output.writeInt(mAcceptableLength);
            } else {
                output.writeInt(bytes.length);
            }
            output.write(bytes);
        } catch(IOException e) {
            return null;
        }

        return buffer.toByteArray();
    }

    public NdefMessage getNdefMessage() {
        return mNdefMessage;
    }

    public byte getField() {
        return mField;
    }

    public byte getVersion() {
        return mVersion;
    }

    public int getLength() {
        return mLength;
    }

    public int getAcceptableLength() {
        if (mField != REQUEST_GET) {
            throw new UnsupportedOperationException(
                    "Acceptable Length only available on get request messages.");
        }
        return mAcceptableLength;
    }
}
