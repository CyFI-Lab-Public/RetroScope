/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.telephony.cts;


import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;

public class SmsMessageTest extends AndroidTestCase{

    private TelephonyManager mTelephonyManager;
    private PackageManager mPackageManager;

    private static final String DISPLAY_MESSAGE_BODY = "test subject /test body";
    private static final String DMB = "{ testBody[^~\\] }";
    private static final String EMAIL_ADD = "foo@example.com";
    private static final String EMAIL_FROM = "foo@example.com";
    private static final String MB = DMB;
    private static final String MESSAGE_BODY1 = "Test";
    private static final String MESSAGE_BODY2 = "(Subject)Test";
    private static final String MESSAGE_BODY3 = "\u2122\u00a9\u00aehello";
    private static final String MESSAGE_BODY4 = " ";
    private static final String MESSAGE_BODY5 = " ";
    private static final String OA = "foo@example.com";
    private static final String OA1 = "+14154255486";
    private static final String OA2 = "+15122977683";
    private static final String OA3 = "_@";
    private static final String OA4 = "\u0394@";
    // pseudo subject will always be empty
    private static final String PSEUDO_SUBJECT = "";
    private static final String SCA1 = "+16466220020";
    private static final String SCA2 = "+12063130012";
    private static final String SCA3 = "+14155551212";
    private static final String SCA4 = "+14155551212";
    private static final int NOT_CREATE_FROM_SIM = -1;
    private static final int NOT_CREATE_FROM_ICC = -1;
    private static final int PROTOCOL_IDENTIFIER = 0;
    private static final int SMS_NUMBER1 = 1;
    private static final int SMS_NUMBER2 = 1;
    private static final int SMS_NUMBER3 = 1;
    private static final int STATUS = 0;
    private static final int STATUS_ON_SIM_DEF = -1;
    private static final int STATUS_ON_ICC_DEF = -1;
    private static final int TPLAYER_LENGTH_FOR_PDU = 23;
    private static final long TIMESTAMP_MILLIS = 1149631383000l;
    private static final int SEPTETS_SKT = 80;
    private static final int SEPTETS_KT = 90;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTelephonyManager =
            (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        mPackageManager = getContext().getPackageManager();
    }

    @SuppressWarnings("deprecation")
    public void testCreateFromPdu() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            // TODO: temp workaround, need to adjust test to use CDMA pdus
            return;
        }

        String pdu = "07916164260220F0040B914151245584F600006060605130308A04D4F29C0E";
        SmsMessage sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertEquals(SCA1, sms.getServiceCenterAddress());
        assertEquals(OA1, sms.getOriginatingAddress());
        assertEquals(MESSAGE_BODY1, sms.getMessageBody());
        assertEquals(TPLAYER_LENGTH_FOR_PDU, SmsMessage.getTPLayerLengthForPDU(pdu));
        int[] result = SmsMessage.calculateLength(sms.getMessageBody(), true);
        assertEquals(SMS_NUMBER1, result[0]);
        assertEquals(sms.getMessageBody().length(), result[1]);
        assertRemaining(sms.getMessageBody().length(), result[2]);
        assertEquals(SmsMessage.ENCODING_7BIT, result[3]);
        assertEquals(pdu, toHexString(sms.getPdu()));

        assertEquals(NOT_CREATE_FROM_SIM, sms.getIndexOnSim());
        assertEquals(NOT_CREATE_FROM_ICC, sms.getIndexOnIcc());
        assertEquals(PROTOCOL_IDENTIFIER, sms.getProtocolIdentifier());
        assertFalse(sms.isEmail());
        assertFalse(sms.isReplyPathPresent());
        assertFalse(sms.isStatusReportMessage());
        assertFalse(sms.isCphsMwiMessage());
        assertEquals(SmsMessage.MessageClass.UNKNOWN, sms.getMessageClass());
        assertEquals(STATUS, sms.getStatus());
        assertEquals(STATUS_ON_SIM_DEF, sms.getStatusOnSim());
        assertEquals(STATUS_ON_ICC_DEF, sms.getStatusOnIcc());
        assertEquals(TIMESTAMP_MILLIS, sms.getTimestampMillis());

        // Test create from null Pdu
        sms = SmsMessage.createFromPdu(null);
        assertNotNull(sms);

        //Test create from long Pdu
        pdu = "07912160130310F2040B915121927786F300036060924180008A0DA"
            + "8695DAC2E8FE9296A794E07";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertEquals(SCA2, sms.getServiceCenterAddress());
        assertEquals(OA2, sms.getOriginatingAddress());
        assertEquals(MESSAGE_BODY2, sms.getMessageBody());
        CharSequence msgBody = sms.getMessageBody();
        result = SmsMessage.calculateLength(msgBody, false);
        assertEquals(SMS_NUMBER2, result[0]);
        assertEquals(sms.getMessageBody().length(), result[1]);
        assertRemaining(sms.getMessageBody().length(), result[2]);
        assertEquals(SmsMessage.ENCODING_7BIT, result[3]);

        // Test createFromPdu Ucs to Sms
        pdu = "07912160130300F4040B914151245584"
            + "F600087010807121352B10212200A900AE00680065006C006C006F";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertEquals(MESSAGE_BODY3, sms.getMessageBody());
        result = SmsMessage.calculateLength(sms.getMessageBody(), true);
        assertEquals(SMS_NUMBER3, result[0]);
        assertEquals(sms.getMessageBody().length(), result[1]);
        assertRemaining(sms.getMessageBody().length(), result[2]);
        assertEquals(SmsMessage.ENCODING_7BIT, result[3]);
    }

    private void assertRemaining(int messageLength, int remaining) {
        if (TelephonyUtils.isSkt(mTelephonyManager)) {
            assertTrue(checkRemaining(SEPTETS_SKT, messageLength, remaining)
                    || checkRemaining(SmsMessage.MAX_USER_DATA_SEPTETS, messageLength, remaining));
        } else if (TelephonyUtils.isKt(mTelephonyManager)) {
            assertTrue(checkRemaining(SEPTETS_KT, messageLength, remaining)
                    || checkRemaining(SmsMessage.MAX_USER_DATA_SEPTETS, messageLength, remaining));
        } else {
            assertTrue(checkRemaining(SmsMessage.MAX_USER_DATA_SEPTETS, messageLength, remaining));
        }
    }

    private boolean checkRemaining(int total, int messageLength, int remaining) {
        return total - messageLength == remaining;
    }

    public void testCPHSVoiceMail() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            // TODO: temp workaround, need to adjust test to use CDMA pdus
            return;
        }

        // "set MWI flag"
        String pdu = "07912160130310F20404D0110041006060627171118A0120";
        SmsMessage sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertTrue(sms.isReplace());
        assertEquals(OA3, sms.getOriginatingAddress());
        assertEquals(MESSAGE_BODY4, sms.getMessageBody());
        assertTrue(sms.isMWISetMessage());

        // "clear mwi flag"
        pdu = "07912160130310F20404D0100041006021924193352B0120";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertTrue(sms.isMWIClearMessage());

        // "clear MWI flag"
        pdu = "07912160130310F20404D0100041006060627161058A0120";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertTrue(sms.isReplace());
        assertEquals(OA4, sms.getOriginatingAddress());
        assertEquals(MESSAGE_BODY5, sms.getMessageBody());
        assertTrue(sms.isMWIClearMessage());

        // "set MWI flag"
        pdu = "07912180958750F84401800500C87020026195702B06040102000200";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertTrue(sms.isMWISetMessage());
        assertTrue(sms.isMwiDontStore());

        // "clear mwi flag"
        pdu = "07912180958750F84401800500C07020027160112B06040102000000";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));

        assertTrue(sms.isMWIClearMessage());
        assertTrue(sms.isMwiDontStore());
    }

    public void testGetUserData() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            // TODO: temp workaround, need to adjust test to use CDMA pdus
            return;
        }

        String pdu = "07914140279510F6440A8111110301003BF56080207130138A8C0B05040B8423F"
            + "000032A02010106276170706C69636174696F6E2F766E642E7761702E6D6D732D"
            + "6D65737361676500AF848D0185B4848C8298524E453955304A6D7135514141426"
            + "66C414141414D7741414236514141414141008D908918802B3135313232393737"
            + "3638332F545950453D504C4D4E008A808E022B918805810306977F83687474703"
            + "A2F2F36";
        SmsMessage sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        byte[] userData = sms.getUserData();
        assertNotNull(userData);
    }

    public void testGetSubmitPdu() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return;
        }

        String scAddress = null, destinationAddress = null;
        String message = null;
        boolean statusReportRequested = false;

        try {
            // null message, null destination
            SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested);
            fail("Should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }

        message = "This is a test message";
        try {
            // non-null message
            SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested);
            fail("Should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }

        if (mTelephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            // TODO: temp workaround, OCTET encoding for EMS not properly supported
            return;
        }

        scAddress = "1650253000";
        destinationAddress = "18004664411";
        message = "This is a test message";
        statusReportRequested = false;
        SmsMessage.SubmitPdu smsPdu =
            SmsMessage.getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested);
        assertNotNull(smsPdu);

        smsPdu = SmsMessage.getSubmitPdu(scAddress, destinationAddress, (short)80,
                message.getBytes(), statusReportRequested);
        assertNotNull(smsPdu);
    }

    public void testEmailGateway() throws Exception {
        if (!mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || mPackageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_CDMA)) {
            // TODO: temp workaround, need to adjust test to use CDMA pdus
            return;
        }

        String pdu = "07914151551512f204038105f300007011103164638a28e6f71b50c687db" +
                         "7076d9357eb7412f7a794e07cdeb6275794c07bde8e5391d247e93f3";

        SmsMessage sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertEquals(SCA4, sms.getServiceCenterAddress());
        assertTrue(sms.isEmail());
        assertEquals(EMAIL_ADD, sms.getEmailFrom());
        assertEquals(EMAIL_ADD, sms.getDisplayOriginatingAddress());
        assertEquals(PSEUDO_SUBJECT, sms.getPseudoSubject());

        assertEquals(DISPLAY_MESSAGE_BODY, sms.getDisplayMessageBody());
        assertEquals(DISPLAY_MESSAGE_BODY, sms.getEmailBody());

        pdu = "07914151551512f204038105f400007011103105458a29e6f71b50c687db" +
                        "7076d9357eb741af0d0a442fcfe9c23739bfe16d289bdee6b5f1813629";
        sms = SmsMessage.createFromPdu(hexStringToByteArray(pdu));
        assertEquals(SCA3, sms.getServiceCenterAddress());
        assertTrue(sms.isEmail());
        assertEquals(OA, sms.getDisplayOriginatingAddress());
        assertEquals(EMAIL_FROM, sms.getEmailFrom());
        assertEquals(DMB, sms.getDisplayMessageBody());
        assertEquals(MB, sms.getEmailBody());
    }

    private final static char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };

    public static String toHexString(byte[] array) {
        int length = array.length;
        char[] buf = new char[length * 2];

        int bufIndex = 0;
        for (int i = 0 ; i < length; i++)
        {
            byte b = array[i];
            buf[bufIndex++] = HEX_DIGITS[(b >>> 4) & 0x0F];
            buf[bufIndex++] = HEX_DIGITS[b & 0x0F];
        }

        return new String(buf);
    }

    private static int toByte(char c) {
        if (c >= '0' && c <= '9') return (c - '0');
        if (c >= 'A' && c <= 'F') return (c - 'A' + 10);
        if (c >= 'a' && c <= 'f') return (c - 'a' + 10);

        throw new RuntimeException ("Invalid hex char '" + c + "'");
    }

    private static byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] buffer = new byte[length / 2];

        for (int i = 0 ; i < length ; i += 2) {
            buffer[i / 2] =
                (byte)((toByte(hexString.charAt(i)) << 4) | toByte(hexString.charAt(i+1)));
        }

        return buffer;
    }
}
