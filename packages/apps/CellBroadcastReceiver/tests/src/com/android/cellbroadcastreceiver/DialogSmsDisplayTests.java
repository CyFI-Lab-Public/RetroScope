/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.cellbroadcastreceiver;

import java.io.UnsupportedEncodingException;

import android.content.Intent;
import android.provider.Telephony.Sms.Intents;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.gsm.GsmSmsCbMessage;
import com.android.internal.telephony.uicc.IccUtils;

/**
 * Various instrumentation tests for CellBroadcastReceiver.
 *
 * To run this test: runtest cellbroadcastreceiver
 * or: adb shell am instrument -w \
 *             com.android.cellbroadcastreceiver.tests/android.test.InstrumentationTestRunner
 *
 * TODO: write better test cases
 *
 */
public class DialogSmsDisplayTests
        extends ActivityInstrumentationTestCase2<CellBroadcastListActivity> {

    public static final String ACTION_SMS_SENT =
        "com.android.basicsmsreceiver.tests.SMS_SENT_ACTION";
    private static String TAG = "DialogSmsDisplayTests";

    private static final int DCS_7BIT_ENGLISH = 0x01;
    private static final int DCS_16BIT_UCS2 = 0x48;

    /* ETWS Test message including header */
    private static final byte[] etwsMessageNormal = IccUtils.hexStringToBytes("000011001101" +
            //   Line 1                  CRLFLine 2
            "EA307DCA602557309707901F58310D0A5BAE57CE770C531790E85C716CBF3044573065B930675730" +
            "9707767A751F30025F37304463FA308C306B5099304830664E0B30553044FF086C178C615E81FF09");

    private static final byte[] etwsMessageCancel = IccUtils.hexStringToBytes("000011001101" +
            //   Line 1                                  CRLFLine 2
            "EA307DCA602557309707901F5831002853D66D8800290D0A5148307B3069002800310030003A0035" +
            "00320029306E7DCA602557309707901F5831309253D66D883057307E3059FF086C178C615E81FF09");

    private static final byte[] etwsMessageTest = IccUtils.hexStringToBytes("000011031101" +
            //   Line 1                                  CRLFLine 2
            "EA3030108A137DF430117DCA602557309707573058310D0A5BAE57CE770C531790E85C716CBF3044" +
            "573065B9306757309707300263FA308C306B5099304830664E0B30553044FF086C178C615E81FF09");

    public DialogSmsDisplayTests() {
        super(CellBroadcastListActivity.class);
        Log.i(TAG, "DialogSmsDisplayTests");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    byte[] encodeCellBroadcast(int serialNumber, int messageId, int dcs, String message) {
        byte[] pdu = new byte[88];
        pdu[0] = (byte) ((serialNumber >> 8) & 0xff);
        pdu[1] = (byte) (serialNumber & 0xff);
        pdu[2] = (byte) ((messageId >> 8) & 0xff);
        pdu[3] = (byte) (messageId & 0xff);
        pdu[4] = (byte) (dcs & 0xff);
        pdu[5] = 0x11;  // single page message
        try {
            byte[] encodedString;
            if (dcs == DCS_16BIT_UCS2) {
                encodedString = message.getBytes("UTF-16");
                System.arraycopy(encodedString, 0, pdu, 6, encodedString.length);
            } else {
                // byte 0 of encodedString is the length in septets (don't copy)
                encodedString = GsmAlphabet.stringToGsm7BitPacked(message);
                System.arraycopy(encodedString, 1, pdu, 6, encodedString.length-1);
            }
            return pdu;
        } catch (EncodeException e) {
            Log.e(TAG, "Encode Exception");
            return null;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unsupported encoding exception for UTF-16");
            return null;
        }
    }

    private static final SmsCbLocation sEmptyLocation = new SmsCbLocation();

    private static SmsCbMessage createFromPdu(byte[] pdu) {
        try {
            byte[][] pdus = new byte[1][];
            pdus[0] = pdu;
            return GsmSmsCbMessage.createSmsCbMessage(sEmptyLocation, pdus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void testSendMessage7bit() throws Exception {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        byte[] pdu = encodeCellBroadcast(0, 0, DCS_7BIT_ENGLISH, "Hello in GSM 7 bit");
        intent.putExtra("message", createFromPdu(pdu));
        getActivity().sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public void testSendMessageUCS2() throws Exception {
        Intent intent = new Intent(Intents.SMS_CB_RECEIVED_ACTION);
        byte[] pdu = encodeCellBroadcast(0, 0, DCS_16BIT_UCS2, "Hello in UCS2");
        intent.putExtra("message", createFromPdu(pdu));
        getActivity().sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public void testSendEtwsMessageNormal() throws Exception {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageNormal));
        getActivity().sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }

    public void testSendEtwsMessageCancel() throws Exception {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageCancel));
        getActivity().sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }

    public void testSendEtwsMessageTest() throws Exception {
        Intent intent = new Intent(Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", createFromPdu(etwsMessageTest));
        getActivity().sendOrderedBroadcast(intent,
                "android.permission.RECEIVE_EMERGENCY_BROADCAST");
    }
}
