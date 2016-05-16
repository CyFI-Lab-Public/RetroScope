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

package com.android.cellbroadcastreceiver.tests;

import android.app.Activity;
import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.util.Log;

import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.gsm.GsmSmsCbMessage;
import com.android.internal.telephony.uicc.IccUtils;

import java.io.UnsupportedEncodingException;

/**
 * Send test messages.
 */
public class SendTestMessages {

    private static String TAG = "SendTestMessages";

    private static final int DCS_7BIT_ENGLISH = 0x01;
    private static final int DCS_16BIT_UCS2 = 0x48;

    /* ETWS Test message including header */
    private static final byte[] etwsMessageNormal = IccUtils.hexStringToBytes("000011001101" +
            "0D0A5BAE57CE770C531790E85C716CBF3044573065B930675730" +
            "9707767A751F30025F37304463FA308C306B5099304830664E0B30553044FF086C178C615E81FF09" +
            "0000000000000000000000000000");

    private static final byte[] etwsMessageCancel = IccUtils.hexStringToBytes("000011001101" +
            "0D0A5148307B3069002800310030003A0035" +
            "00320029306E7DCA602557309707901F5831309253D66D883057307E3059FF086C178C615E81FF09" +
            "00000000000000000000000000000000000000000000");

    private static final byte[] etwsMessageTest = IccUtils.hexStringToBytes("000011031101" +
            "0D0A5BAE57CE770C531790E85C716CBF3044" +
            "573065B9306757309707300263FA308C306B5099304830664E0B30553044FF086C178C615E81FF09" +
            "00000000000000000000000000000000000000000000");

    private static final byte[] gsm7BitTest = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x40, (byte)0x11, (byte)0x41,
            (byte)0xD0, (byte)0x71, (byte)0xDA, (byte)0x04, (byte)0x91, (byte)0xCB, (byte)0xE6,
            (byte)0x70, (byte)0x9D, (byte)0x4D, (byte)0x07, (byte)0x85, (byte)0xD9, (byte)0x70,
            (byte)0x74, (byte)0x58, (byte)0x5C, (byte)0xA6, (byte)0x83, (byte)0xDA, (byte)0xE5,
            (byte)0xF9, (byte)0x3C, (byte)0x7C, (byte)0x2E, (byte)0x83, (byte)0xEE, (byte)0x69,
            (byte)0x3A, (byte)0x1A, (byte)0x34, (byte)0x0E, (byte)0xCB, (byte)0xE5, (byte)0xE9,
            (byte)0xF0, (byte)0xB9, (byte)0x0C, (byte)0x92, (byte)0x97, (byte)0xE9, (byte)0x75,
            (byte)0xB9, (byte)0x1B, (byte)0x04, (byte)0x0F, (byte)0x93, (byte)0xC9, (byte)0x69,
            (byte)0xF7, (byte)0xB9, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00
    };

    private static final byte[] gsm7BitTestUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x40,

            (byte)0x01,

            (byte)0x41, (byte)0xD0, (byte)0x71, (byte)0xDA, (byte)0x04, (byte)0x91,
            (byte)0xCB, (byte)0xE6, (byte)0x70, (byte)0x9D, (byte)0x4D, (byte)0x07,
            (byte)0x85, (byte)0xD9, (byte)0x70, (byte)0x74, (byte)0x58, (byte)0x5C,
            (byte)0xA6, (byte)0x83, (byte)0xDA, (byte)0xE5, (byte)0xF9, (byte)0x3C,
            (byte)0x7C, (byte)0x2E, (byte)0x83, (byte)0xEE, (byte)0x69, (byte)0x3A,
            (byte)0x1A, (byte)0x34, (byte)0x0E, (byte)0xCB, (byte)0xE5, (byte)0xE9,
            (byte)0xF0, (byte)0xB9, (byte)0x0C, (byte)0x92, (byte)0x97, (byte)0xE9,
            (byte)0x75, (byte)0xB9, (byte)0x1B, (byte)0x04, (byte)0x0F, (byte)0x93,
            (byte)0xC9, (byte)0x69, (byte)0xF7, (byte)0xB9, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00,

            (byte)0x34
    };

    private static final byte[] gsm7BitTestMultipageUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x40,

            (byte)0x02,

            (byte)0xC6, (byte)0xB4, (byte)0x7C, (byte)0x4E, (byte)0x07, (byte)0xC1,
            (byte)0xC3, (byte)0xE7, (byte)0xF2, (byte)0xAA, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A,
            (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34,
            (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00,

            (byte)0x0A,

            (byte)0xD3, (byte)0xF2, (byte)0xF8, (byte)0xED, (byte)0x26, (byte)0x83,
            (byte)0xE0, (byte)0xE1, (byte)0x73, (byte)0xB9, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A,
            (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34,
            (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00,

            (byte)0x0A
    };

    private static final byte[] gsm7BitTestMultipage1 = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x40,
            (byte)0xC6, (byte)0xB4, (byte)0x7C, (byte)0x4E, (byte)0x07, (byte)0xC1,
            (byte)0xC3, (byte)0xE7, (byte)0xF2, (byte)0xAA, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A,
            (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34,
            (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00
    };

    private static final byte[] gsm7BitTestMultipage2 = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x40,
            (byte)0xD3, (byte)0xF2, (byte)0xF8, (byte)0xED, (byte)0x26, (byte)0x83,
            (byte)0xE0, (byte)0xE1, (byte)0x73, (byte)0xB9, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A,
            (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34,
            (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68,
            (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00
    };

    private static final byte[] gsm7BitTestNoPadding = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x40, (byte)0x11, (byte)0x41,
            (byte)0xD0, (byte)0x71, (byte)0xDA, (byte)0x04, (byte)0x91, (byte)0xCB, (byte)0xE6,
            (byte)0x70, (byte)0x9D, (byte)0x4D, (byte)0x07, (byte)0x85, (byte)0xD9, (byte)0x70,
            (byte)0x74, (byte)0x58, (byte)0x5C, (byte)0xA6, (byte)0x83, (byte)0xDA, (byte)0xE5,
            (byte)0xF9, (byte)0x3C, (byte)0x7C, (byte)0x2E, (byte)0x83, (byte)0xC4, (byte)0xE5,
            (byte)0xB4, (byte)0xFB, (byte)0x0C, (byte)0x2A, (byte)0xE3, (byte)0xC3, (byte)0x63,
            (byte)0x3A, (byte)0x3B, (byte)0x0F, (byte)0xCA, (byte)0xCD, (byte)0x40, (byte)0x63,
            (byte)0x74, (byte)0x58, (byte)0x1E, (byte)0x1E, (byte)0xD3, (byte)0xCB, (byte)0xF2,
            (byte)0x39, (byte)0x88, (byte)0xFD, (byte)0x76, (byte)0x9F, (byte)0x59, (byte)0xA0,
            (byte)0x76, (byte)0x39, (byte)0xEC, (byte)0x4E, (byte)0xBB, (byte)0xCF, (byte)0x20,
            (byte)0x3A, (byte)0xBA, (byte)0x2C, (byte)0x2F, (byte)0x83, (byte)0xD2, (byte)0x73,
            (byte)0x90, (byte)0xFB, (byte)0x0D, (byte)0x82, (byte)0x87, (byte)0xC9, (byte)0xE4,
            (byte)0xB4, (byte)0xFB, (byte)0x1C, (byte)0x02
    };

    private static final byte[] gsm7BitTestNoPaddingUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x40,

            (byte)0x01,

            (byte)0x41, (byte)0xD0, (byte)0x71, (byte)0xDA, (byte)0x04, (byte)0x91,
            (byte)0xCB, (byte)0xE6, (byte)0x70, (byte)0x9D, (byte)0x4D, (byte)0x07,
            (byte)0x85, (byte)0xD9, (byte)0x70, (byte)0x74, (byte)0x58, (byte)0x5C,
            (byte)0xA6, (byte)0x83, (byte)0xDA, (byte)0xE5, (byte)0xF9, (byte)0x3C,
            (byte)0x7C, (byte)0x2E, (byte)0x83, (byte)0xC4, (byte)0xE5, (byte)0xB4,
            (byte)0xFB, (byte)0x0C, (byte)0x2A, (byte)0xE3, (byte)0xC3, (byte)0x63,
            (byte)0x3A, (byte)0x3B, (byte)0x0F, (byte)0xCA, (byte)0xCD, (byte)0x40,
            (byte)0x63, (byte)0x74, (byte)0x58, (byte)0x1E, (byte)0x1E, (byte)0xD3,
            (byte)0xCB, (byte)0xF2, (byte)0x39, (byte)0x88, (byte)0xFD, (byte)0x76,
            (byte)0x9F, (byte)0x59, (byte)0xA0, (byte)0x76, (byte)0x39, (byte)0xEC,
            (byte)0x4E, (byte)0xBB, (byte)0xCF, (byte)0x20, (byte)0x3A, (byte)0xBA,
            (byte)0x2C, (byte)0x2F, (byte)0x83, (byte)0xD2, (byte)0x73, (byte)0x90,
            (byte)0xFB, (byte)0x0D, (byte)0x82, (byte)0x87, (byte)0xC9, (byte)0xE4,
            (byte)0xB4, (byte)0xFB, (byte)0x1C, (byte)0x02,

            (byte)0x52
    };

    private static final byte[] gsm7BitTestWithLanguage = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x04, (byte)0x11, (byte)0x41,
            (byte)0xD0, (byte)0x71, (byte)0xDA, (byte)0x04, (byte)0x91, (byte)0xCB, (byte)0xE6,
            (byte)0x70, (byte)0x9D, (byte)0x4D, (byte)0x07, (byte)0x85, (byte)0xD9, (byte)0x70,
            (byte)0x74, (byte)0x58, (byte)0x5C, (byte)0xA6, (byte)0x83, (byte)0xDA, (byte)0xE5,
            (byte)0xF9, (byte)0x3C, (byte)0x7C, (byte)0x2E, (byte)0x83, (byte)0xEE, (byte)0x69,
            (byte)0x3A, (byte)0x1A, (byte)0x34, (byte)0x0E, (byte)0xCB, (byte)0xE5, (byte)0xE9,
            (byte)0xF0, (byte)0xB9, (byte)0x0C, (byte)0x92, (byte)0x97, (byte)0xE9, (byte)0x75,
            (byte)0xB9, (byte)0x1B, (byte)0x04, (byte)0x0F, (byte)0x93, (byte)0xC9, (byte)0x69,
            (byte)0xF7, (byte)0xB9, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00
    };

    private static final byte[] gsm7BitTestWithLanguageInBody = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x10, (byte)0x11, (byte)0x73,
            (byte)0x7B, (byte)0x23, (byte)0x08, (byte)0x3A, (byte)0x4E, (byte)0x9B, (byte)0x20,
            (byte)0x72, (byte)0xD9, (byte)0x1C, (byte)0xAE, (byte)0xB3, (byte)0xE9, (byte)0xA0,
            (byte)0x30, (byte)0x1B, (byte)0x8E, (byte)0x0E, (byte)0x8B, (byte)0xCB, (byte)0x74,
            (byte)0x50, (byte)0xBB, (byte)0x3C, (byte)0x9F, (byte)0x87, (byte)0xCF, (byte)0x65,
            (byte)0xD0, (byte)0x3D, (byte)0x4D, (byte)0x47, (byte)0x83, (byte)0xC6, (byte)0x61,
            (byte)0xB9, (byte)0x3C, (byte)0x1D, (byte)0x3E, (byte)0x97, (byte)0x41, (byte)0xF2,
            (byte)0x32, (byte)0xBD, (byte)0x2E, (byte)0x77, (byte)0x83, (byte)0xE0, (byte)0x61,
            (byte)0x32, (byte)0x39, (byte)0xED, (byte)0x3E, (byte)0x37, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00
    };

    private static final byte[] gsm7BitTestWithLanguageInBodyUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x10,

            (byte)0x01,

            (byte)0x73, (byte)0x7B, (byte)0x23, (byte)0x08, (byte)0x3A, (byte)0x4E,
            (byte)0x9B, (byte)0x20, (byte)0x72, (byte)0xD9, (byte)0x1C, (byte)0xAE,
            (byte)0xB3, (byte)0xE9, (byte)0xA0, (byte)0x30, (byte)0x1B, (byte)0x8E,
            (byte)0x0E, (byte)0x8B, (byte)0xCB, (byte)0x74, (byte)0x50, (byte)0xBB,
            (byte)0x3C, (byte)0x9F, (byte)0x87, (byte)0xCF, (byte)0x65, (byte)0xD0,
            (byte)0x3D, (byte)0x4D, (byte)0x47, (byte)0x83, (byte)0xC6, (byte)0x61,
            (byte)0xB9, (byte)0x3C, (byte)0x1D, (byte)0x3E, (byte)0x97, (byte)0x41,
            (byte)0xF2, (byte)0x32, (byte)0xBD, (byte)0x2E, (byte)0x77, (byte)0x83,
            (byte)0xE0, (byte)0x61, (byte)0x32, (byte)0x39, (byte)0xED, (byte)0x3E,
            (byte)0x37, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3, (byte)0xD1,
            (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46, (byte)0xA3,
            (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D, (byte)0x46,
            (byte)0xA3, (byte)0xD1, (byte)0x68, (byte)0x34, (byte)0x1A, (byte)0x8D,
            (byte)0x46, (byte)0xA3, (byte)0xD1, (byte)0x00,

            (byte)0x37
    };

    private static final byte[] gsmUcs2Test = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x48, (byte)0x11, (byte)0x00,
            (byte)0x41, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x55, (byte)0x00, (byte)0x43,
            (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x20, (byte)0x00,
            (byte)0x6D, (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x73,
            (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x65, (byte)0x00,
            (byte)0x20, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x6F, (byte)0x00, (byte)0x6E,
            (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x69, (byte)0x00,
            (byte)0x6E, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x67,
            (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x20, (byte)0x04,
            (byte)0x34, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x68,
            (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x61, (byte)0x00,
            (byte)0x63, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x72,
            (byte)0x00, (byte)0x0D, (byte)0x00, (byte)0x0D
    };

    private static final byte[] gsmUcs2TestUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x48,

            (byte)0x01,

            (byte)0x00, (byte)0x41, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x55,
            (byte)0x00, (byte)0x43, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x32,
            (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x6D, (byte)0x00, (byte)0x65,
            (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x61,
            (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x20,
            (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x6F, (byte)0x00, (byte)0x6E,
            (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x69,
            (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x6E,
            (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x61,
            (byte)0x00, (byte)0x20, (byte)0x04, (byte)0x34, (byte)0x00, (byte)0x20,
            (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x68, (byte)0x00, (byte)0x61,
            (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x63,
            (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x72,
            (byte)0x00, (byte)0x0D, (byte)0x00, (byte)0x0D,

            (byte)0x4E
    };

    private static final byte[] gsmUcs2TestMultipageUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x48,

            (byte)0x02,

            (byte)0x00, (byte)0x41, (byte)0x00, (byte)0x41, (byte)0x00, (byte)0x41,
            (byte)0x00, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,

            (byte)0x06,

            (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x42,
            (byte)0x00, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,
            (byte)0x0D, (byte)0x0D, (byte)0x0D, (byte)0x0D,

            (byte)0x06
    };

    private static final byte[] gsmUcs2TestWithLanguageInBody = {
            (byte)0xC0, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x11, (byte)0x11, (byte)0x78,
            (byte)0x3C, (byte)0x00, (byte)0x41, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x55,
            (byte)0x00, (byte)0x43, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x32, (byte)0x00,
            (byte)0x20, (byte)0x00, (byte)0x6D, (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x73,
            (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x67, (byte)0x00,
            (byte)0x65, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x6F,
            (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x61, (byte)0x00,
            (byte)0x69, (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x6E,
            (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x61, (byte)0x00,
            (byte)0x20, (byte)0x04, (byte)0x34, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x63,
            (byte)0x00, (byte)0x68, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x72, (byte)0x00,
            (byte)0x61, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x65,
            (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x0D
    };

    private static final byte[] gsmUcs2TestWithLanguageInBodyUmts = {
            (byte)0x01, (byte)0x00, (byte)0x01, (byte)0xC0, (byte)0x00, (byte)0x11,

            (byte)0x01,

            (byte)0x78, (byte)0x3C, (byte)0x00, (byte)0x41, (byte)0x00, (byte)0x20,
            (byte)0x00, (byte)0x55, (byte)0x00, (byte)0x43, (byte)0x00, (byte)0x53,
            (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x6D,
            (byte)0x00, (byte)0x65, (byte)0x00, (byte)0x73, (byte)0x00, (byte)0x73,
            (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x65,
            (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x6F,
            (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x61,
            (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x69,
            (byte)0x00, (byte)0x6E, (byte)0x00, (byte)0x67, (byte)0x00, (byte)0x20,
            (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x20, (byte)0x04, (byte)0x34,
            (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x68,
            (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x61,
            (byte)0x00, (byte)0x63, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x65,
            (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x0D,

            (byte)0x50
    };

    private static final SmsCbLocation sEmptyLocation = new SmsCbLocation();

    private static SmsCbMessage createFromPdu(byte[] pdu, int serialNumber, int category) {
        byte[][] pdus = new byte[1][];
        pdus[0] = pdu;
        return createFromPdus(pdus, serialNumber, category);
    }

    private static SmsCbMessage createFromPdus(byte[][] pdus, int serialNumber, int category) {
        try {
            for (byte[] pdu : pdus) {
                if (pdu.length <= 88) {
                    // GSM format cell broadcast
                    Log.d(TAG, "setting GSM serial number to " + serialNumber);
                    pdu[0] = (byte) ((serialNumber >>> 8) & 0xff);
                    pdu[1] = (byte) (serialNumber & 0xff);
                    if (category != 0) {
                        Log.d(TAG, "setting GSM message identifier to " + category);
                        pdu[2] = (byte) ((category >>> 8) & 0xff);
                        pdu[3] = (byte) (category & 0xff);
                    }
                } else {
                    // UMTS format cell broadcast
                    Log.d(TAG, "setting UMTS serial number to " + serialNumber);
                    pdu[3] = (byte) ((serialNumber >>> 8) & 0xff);
                    pdu[4] = (byte) (serialNumber & 0xff);
                    if (category != 0) {
                        Log.d(TAG, "setting UMTS message identifier to " + category);
                        pdu[1] = (byte) ((category >>> 8) & 0xff);
                        pdu[2] = (byte) (category & 0xff);
                    }
                }
            }
            return GsmSmsCbMessage.createSmsCbMessage(sEmptyLocation, pdus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void testSendMessage7bit(Activity activity, int serialNumber, int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTest, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitUmts(Activity activity, int serialNumber, int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestUmts, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitNoPadding(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestNoPadding, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitNoPaddingUmts(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestNoPaddingUmts, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitMultipageGsm(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        byte[][] pdus = new byte[2][];
        pdus[0] = gsm7BitTestMultipage1;
        pdus[1] = gsm7BitTestMultipage2;
        intent.putExtra("message", createFromPdus(pdus, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitMultipageUmts(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestMultipageUmts, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitWithLanguage(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestWithLanguage, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitWithLanguageInBody(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestWithLanguageInBody, serialNumber,
                category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessage7bitWithLanguageInBodyUmts(Activity activity,
            int serialNumber, int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsm7BitTestWithLanguageInBodyUmts, serialNumber,
                category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessageUcs2(Activity activity, int serialNumber, int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsmUcs2Test, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessageUcs2Umts(Activity activity, int serialNumber, int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsmUcs2TestUmts, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessageUcs2MultipageUmts(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsmUcs2TestMultipageUmts, serialNumber, category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessageUcs2WithLanguageInBody(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsmUcs2TestWithLanguageInBody, serialNumber,
                category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendMessageUcs2WithLanguageUmts(Activity activity, int serialNumber,
            int category) {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(gsmUcs2TestWithLanguageInBodyUmts, serialNumber,
                category));
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendEtwsMessageNormal(Activity activity, int serialNumber) {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageNormal, serialNumber, 0));
        activity.sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }

    public static void testSendEtwsMessageCancel(Activity activity, int serialNumber) {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageCancel, serialNumber, 0));
        activity.sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }

    public static void testSendEtwsMessageTest(Activity activity, int serialNumber) {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageTest, serialNumber, 0));
        activity.sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }
}
