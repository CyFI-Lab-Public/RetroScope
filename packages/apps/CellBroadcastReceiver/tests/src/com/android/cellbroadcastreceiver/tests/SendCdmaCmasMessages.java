/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.os.Parcel;
import android.provider.Telephony;
import android.telephony.SmsCbCmasInfo;
import android.telephony.SmsCbLocation;
import android.telephony.SmsCbMessage;
import android.test.AndroidTestCase;
import android.util.Log;

import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cdma.sms.BearerData;
import com.android.internal.telephony.cdma.sms.CdmaSmsAddress;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.cdma.sms.UserData;
import com.android.internal.util.BitwiseOutputStream;

import java.util.Arrays;
import java.util.Random;

/**
 * Send some test CDMA CMAS warning notifications.
 */
public class SendCdmaCmasMessages {

    private static final String TEST_TEXT = "This is a test CDMA cell broadcast message..."
            + "678901234567890123456789012345678901234567890";

    private static final String PRES_ALERT =
            "THE PRESIDENT HAS ISSUED AN EMERGENCY ALERT. CHECK LOCAL MEDIA FOR MORE DETAILS";

    private static final String EXTREME_ALERT = "FLASH FLOOD WARNING FOR SOUTH COCONINO COUNTY"
            + " - NORTH CENTRAL ARIZONA UNTIL 415 PM MST";

    private static final String SEVERE_ALERT = "SEVERE WEATHER WARNING FOR SOMERSET COUNTY"
            + " - NEW JERSEY UNTIL 415 PM MST";

    private static final String AMBER_ALERT =
            "AMBER ALERT:Mountain View,CA VEH'07 Blue Honda Civic CA LIC 5ABC123";

    private static final String MONTHLY_TEST_ALERT = "This is a test of the emergency alert system."
            + " This is only a test. 89012345678901234567890";

    private static final String IS91_TEXT = "IS91 SHORT MSG";   // max length 14 chars

    public static void testSendCmasPresAlert(Activity activity, int messageId) {
        SmsCbMessage cbMessage = createCmasSmsMessage(
                SmsEnvelope.SERVICE_CATEGORY_CMAS_PRESIDENTIAL_LEVEL_ALERT, messageId, "en",
                PRES_ALERT, SmsCbCmasInfo.CMAS_CATEGORY_GEO,
                SmsCbCmasInfo.CMAS_RESPONSE_TYPE_PREPARE, SmsCbCmasInfo.CMAS_SEVERITY_EXTREME,
                SmsCbCmasInfo.CMAS_URGENCY_EXPECTED, SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY);

        Intent intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", cbMessage);
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendCmasExtremeAlert(Activity activity, int messageId) {
        SmsCbMessage cbMessage = createCmasSmsMessage(
                SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT, messageId, "en",
                EXTREME_ALERT, SmsCbCmasInfo.CMAS_CATEGORY_MET,
                SmsCbCmasInfo.CMAS_RESPONSE_TYPE_PREPARE, SmsCbCmasInfo.CMAS_SEVERITY_EXTREME,
                SmsCbCmasInfo.CMAS_URGENCY_EXPECTED, SmsCbCmasInfo.CMAS_CERTAINTY_OBSERVED);

        Intent intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", cbMessage);
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendCmasSevereAlert(Activity activity, int messageId) {
        SmsCbMessage cbMessage = createCmasSmsMessage(
                SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT, messageId, "en",
                SEVERE_ALERT, SmsCbCmasInfo.CMAS_CATEGORY_HEALTH,
                SmsCbCmasInfo.CMAS_RESPONSE_TYPE_AVOID, SmsCbCmasInfo.CMAS_SEVERITY_SEVERE,
                SmsCbCmasInfo.CMAS_URGENCY_IMMEDIATE, SmsCbCmasInfo.CMAS_CERTAINTY_LIKELY);

        Intent intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", cbMessage);
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendCmasAmberAlert(Activity activity, int messageId) {
        SmsCbMessage cbMessage = createCmasSmsMessage(
                SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY, messageId, "en",
                AMBER_ALERT, SmsCbCmasInfo.CMAS_CATEGORY_UNKNOWN,
                SmsCbCmasInfo.CMAS_RESPONSE_TYPE_UNKNOWN, SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN,
                SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN, SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN);

        Intent intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", cbMessage);
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    public static void testSendCmasMonthlyTest(Activity activity, int messageId) {
        SmsCbMessage cbMessage = createCmasSmsMessage(
                SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE, messageId, "en",
                MONTHLY_TEST_ALERT, SmsCbCmasInfo.CMAS_CATEGORY_UNKNOWN,
                SmsCbCmasInfo.CMAS_RESPONSE_TYPE_UNKNOWN, SmsCbCmasInfo.CMAS_SEVERITY_UNKNOWN,
                SmsCbCmasInfo.CMAS_URGENCY_UNKNOWN, SmsCbCmasInfo.CMAS_CERTAINTY_UNKNOWN);

        Intent intent = new Intent(Telephony.Sms.Intents.SMS_EMERGENCY_CB_RECEIVED_ACTION);
        intent.putExtra("message", cbMessage);
        activity.sendOrderedBroadcast(intent, "android.permission.RECEIVE_SMS");
    }

    /**
     * Create a new SmsCbMessage for testing CDMA CMAS support.
     * @param serviceCategory the CDMA service category
     * @param messageId the 16-bit message identifier
     * @param language message language code
     * @param body message body
     * @param cmasCategory CMAS category (or -1 to skip adding CMAS type 1 elements record)
     * @param responseType CMAS response type
     * @param severity CMAS severity
     * @param urgency CMAS urgency
     * @param certainty CMAS certainty
     * @return the newly created SmsMessage object
     */
    private static SmsCbMessage createCmasSmsMessage(int serviceCategory, int messageId,
            String language, String body, int cmasCategory, int responseType, int severity,
            int urgency, int certainty) {
        int cmasMessageClass = serviceCategoryToCmasMessageClass(serviceCategory);
        SmsCbCmasInfo cmasInfo = new SmsCbCmasInfo(cmasMessageClass, cmasCategory, responseType,
                severity, urgency, certainty);
        return new SmsCbMessage(SmsCbMessage.MESSAGE_FORMAT_3GPP2,
                SmsCbMessage.GEOGRAPHICAL_SCOPE_PLMN_WIDE, messageId, new SmsCbLocation("123456"),
                serviceCategory, language, body, SmsCbMessage.MESSAGE_PRIORITY_EMERGENCY, null,
                cmasInfo);
    }

    /**
     * Convert CDMA service category to CMAS message class. Copied from {@code BearerData}.
     * @param serviceCategory CDMA service category
     * @return CMAS message class
     */
    private static int serviceCategoryToCmasMessageClass(int serviceCategory) {
        switch (serviceCategory) {
            case SmsEnvelope.SERVICE_CATEGORY_CMAS_PRESIDENTIAL_LEVEL_ALERT:
                return SmsCbCmasInfo.CMAS_CLASS_PRESIDENTIAL_LEVEL_ALERT;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_EXTREME_THREAT:
                return SmsCbCmasInfo.CMAS_CLASS_EXTREME_THREAT;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_SEVERE_THREAT:
                return SmsCbCmasInfo.CMAS_CLASS_SEVERE_THREAT;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_CHILD_ABDUCTION_EMERGENCY:
                return SmsCbCmasInfo.CMAS_CLASS_CHILD_ABDUCTION_EMERGENCY;

            case SmsEnvelope.SERVICE_CATEGORY_CMAS_TEST_MESSAGE:
                return SmsCbCmasInfo.CMAS_CLASS_REQUIRED_MONTHLY_TEST;

            default:
                return SmsCbCmasInfo.CMAS_CLASS_UNKNOWN;
        }
    }
}
