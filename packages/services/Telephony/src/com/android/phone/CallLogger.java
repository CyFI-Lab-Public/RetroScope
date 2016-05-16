/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.phone;

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.common.CallLogAsync;

import android.net.Uri;
import android.os.SystemProperties;
import android.provider.CallLog.Calls;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

/**
 * Helper class for interacting with the call log.
 */
class CallLogger {
    private static final String LOG_TAG = CallLogger.class.getSimpleName();
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 1) &&
        (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private PhoneGlobals mApplication;
    private CallLogAsync mCallLog;

    public CallLogger(PhoneGlobals application, CallLogAsync callLogAsync) {
        mApplication = application;
        mCallLog = callLogAsync;
    }

    /**
     * Logs a call to the call log based on the connection object passed in.
     *
     * @param c The connection object for the call being logged.
     * @param callLogType The type of call log entry.
     */
    public void logCall(Connection c, int callLogType) {
        final String number = c.getAddress();
        final long date = c.getCreateTime();
        final long duration = c.getDurationMillis();
        final Phone phone = c.getCall().getPhone();

        final CallerInfo ci = getCallerInfoFromConnection(c);  // May be null.
        final String logNumber = getLogNumber(c, ci);

        if (DBG) {
            log("- onDisconnect(): logNumber set to:" + PhoneUtils.toLogSafePhoneNumber(logNumber) +
                ", number set to: " + PhoneUtils.toLogSafePhoneNumber(number));
        }

        // TODO: In getLogNumber we use the presentation from
        // the connection for the CNAP. Should we use the one
        // below instead? (comes from caller info)

        // For international calls, 011 needs to be logged as +
        final int presentation = getPresentation(c, ci);

        final boolean isOtaspNumber = TelephonyCapabilities.supportsOtasp(phone)
                && phone.isOtaSpNumber(number);

        // Don't log OTASP calls.
        if (!isOtaspNumber) {
            logCall(ci, logNumber, presentation, callLogType, date, duration);
        }
    }

    /**
     * Came as logCall(Connection,int) but calculates the call type from the connection object.
     */
    public void logCall(Connection c) {
        final Connection.DisconnectCause cause = c.getDisconnectCause();

        // Set the "type" to be displayed in the call log (see constants in CallLog.Calls)
        final int callLogType;

        if (c.isIncoming()) {
            callLogType = (cause == Connection.DisconnectCause.INCOMING_MISSED ?
                           Calls.MISSED_TYPE : Calls.INCOMING_TYPE);
        } else {
            callLogType = Calls.OUTGOING_TYPE;
        }
        if (VDBG) log("- callLogType: " + callLogType + ", UserData: " + c.getUserData());

        logCall(c, callLogType);
    }

    /**
     * Logs a call to the call from the parameters passed in.
     */
    public void logCall(CallerInfo ci, String number, int presentation, int callType, long start,
                        long duration) {
        final boolean isEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(number,
                mApplication);

        // On some devices, to avoid accidental redialing of
        // emergency numbers, we *never* log emergency calls to
        // the Call Log.  (This behavior is set on a per-product
        // basis, based on carrier requirements.)
        final boolean okToLogEmergencyNumber =
            mApplication.getResources().getBoolean(
                        R.bool.allow_emergency_numbers_in_call_log);

        // Don't log emergency numbers if the device doesn't allow it,
        boolean isOkToLogThisCall = !isEmergencyNumber || okToLogEmergencyNumber;

        if (isOkToLogThisCall) {
            if (DBG) {
                log("sending Calllog entry: " + ci + ", " + PhoneUtils.toLogSafePhoneNumber(number)
                    + "," + presentation + ", " + callType + ", " + start + ", " + duration);
            }

            CallLogAsync.AddCallArgs args = new CallLogAsync.AddCallArgs(mApplication, ci, number,
                    presentation, callType, start, duration);
            mCallLog.addCall(args);
        }
    }

    /**
     * Get the caller info.
     *
     * @param conn The phone connection.
     * @return The CallerInfo associated with the connection. Maybe null.
     */
    private CallerInfo getCallerInfoFromConnection(Connection conn) {
        CallerInfo ci = null;
        Object o = conn.getUserData();

        if ((o == null) || (o instanceof CallerInfo)) {
            ci = (CallerInfo) o;
        } else if (o instanceof Uri) {
            ci = CallerInfo.getCallerInfo(mApplication.getApplicationContext(), (Uri) o);
        } else {
            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
        }
        return ci;
    }

    /**
     * Retrieve the phone number from the caller info or the connection.
     *
     * For incoming call the number is in the Connection object. For
     * outgoing call we use the CallerInfo phoneNumber field if
     * present. All the processing should have been done already (CDMA vs GSM numbers).
     *
     * If CallerInfo is missing the phone number, get it from the connection.
     * Apply the Call Name Presentation (CNAP) transform in the connection on the number.
     *
     * @param conn The phone connection.
     * @param callerInfo The CallerInfo. Maybe null.
     * @return the phone number.
     */
    private String getLogNumber(Connection conn, CallerInfo callerInfo) {
        String number = null;

        if (conn.isIncoming()) {
            number = conn.getAddress();
        } else {
            // For emergency and voicemail calls,
            // CallerInfo.phoneNumber does *not* contain a valid phone
            // number.  Instead it contains an I18N'd string such as
            // "Emergency Number" or "Voice Mail" so we get the number
            // from the connection.
            if (null == callerInfo || TextUtils.isEmpty(callerInfo.phoneNumber) ||
                callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
                if (conn.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    // In cdma getAddress() is not always equals to getOrigDialString().
                    number = conn.getOrigDialString();
                } else {
                    number = conn.getAddress();
                }
            } else {
                number = callerInfo.phoneNumber;
            }
        }

        if (null == number) {
            return null;
        } else {
            int presentation = conn.getNumberPresentation();

            // Do final CNAP modifications.
            String newNumber = PhoneUtils.modifyForSpecialCnapCases(mApplication, callerInfo,
                                                          number, presentation);

            if (!PhoneNumberUtils.isUriNumber(number)) {
                number = PhoneNumberUtils.stripSeparators(number);
            }
            if (VDBG) log("getLogNumber: " + number);
            return number;
        }
    }

    /**
     * Get the presentation from the callerinfo if not null otherwise,
     * get it from the connection.
     *
     * @param conn The phone connection.
     * @param callerInfo The CallerInfo. Maybe null.
     * @return The presentation to use in the logs.
     */
    private int getPresentation(Connection conn, CallerInfo callerInfo) {
        int presentation;

        if (null == callerInfo) {
            presentation = conn.getNumberPresentation();
        } else {
            presentation = callerInfo.numberPresentation;
            if (DBG) log("- getPresentation(): ignoring connection's presentation: " +
                         conn.getNumberPresentation());
        }
        if (DBG) log("- getPresentation: presentation: " + presentation);
        return presentation;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
