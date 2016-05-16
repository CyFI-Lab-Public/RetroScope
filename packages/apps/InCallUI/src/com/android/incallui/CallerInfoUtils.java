package com.android.incallui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.CallIdentification;

import java.util.Arrays;

/**
 * Utility methods for contact and caller info related functionality
 */
public class CallerInfoUtils {

    private static final String TAG = CallerInfoUtils.class.getSimpleName();

    /** Define for not a special CNAP string */
    private static final int CNAP_SPECIAL_CASE_NO = -1;

    private static final String VIEW_NOTIFICATION_ACTION =
            "com.android.contacts.VIEW_NOTIFICATION";
    private static final String VIEW_NOTIFICATION_PACKAGE = "com.android.contacts";
    private static final String VIEW_NOTIFICATION_CLASS =
            "com.android.contacts.ViewNotificationService";

    public CallerInfoUtils() {
    }

    private static final int QUERY_TOKEN = -1;

    /**
     * This is called to get caller info for a call. This will return a CallerInfo
     * object immediately based off information in the call, but
     * more information is returned to the OnQueryCompleteListener (which contains
     * information about the phone number label, user's name, etc).
     */
    public static CallerInfo getCallerInfoForCall(Context context, CallIdentification call,
            CallerInfoAsyncQuery.OnQueryCompleteListener listener) {
        CallerInfo info = buildCallerInfo(context, call);
        String number = info.phoneNumber;

        // TODO: Have phoneapp send a Uri when it knows the contact that triggered this call.

        if (info.numberPresentation == Call.PRESENTATION_ALLOWED) {
            // Start the query with the number provided from the call.
            Log.d(TAG, "==> Actually starting CallerInfoAsyncQuery.startQuery()...");
            CallerInfoAsyncQuery.startQuery(QUERY_TOKEN, context, number, listener, call);
        }
        return info;
    }

    public static CallerInfo buildCallerInfo(Context context, CallIdentification identification) {
        CallerInfo info = new CallerInfo();

        // Store CNAP information retrieved from the Connection (we want to do this
        // here regardless of whether the number is empty or not).
        info.cnapName = identification.getCnapName();
        info.name = info.cnapName;
        info.numberPresentation = identification.getNumberPresentation();
        info.namePresentation = identification.getCnapNamePresentation();

        String number = identification.getNumber();
        if (!TextUtils.isEmpty(number)) {
            final String[] numbers = number.split("&");
            number = numbers[0];
            if (numbers.length > 1) {
                info.forwardingNumber = numbers[1];
            }

            number = modifyForSpecialCnapCases(context, info, number, info.numberPresentation);
            info.phoneNumber = number;
        }
        return info;
    }

    /**
     * Handles certain "corner cases" for CNAP. When we receive weird phone numbers
     * from the network to indicate different number presentations, convert them to
     * expected number and presentation values within the CallerInfo object.
     * @param number number we use to verify if we are in a corner case
     * @param presentation presentation value used to verify if we are in a corner case
     * @return the new String that should be used for the phone number
     */
    /* package */static String modifyForSpecialCnapCases(Context context, CallerInfo ci,
            String number, int presentation) {
        // Obviously we return number if ci == null, but still return number if
        // number == null, because in these cases the correct string will still be
        // displayed/logged after this function returns based on the presentation value.
        if (ci == null || number == null) return number;

        Log.d(TAG, "modifyForSpecialCnapCases: initially, number="
                + toLogSafePhoneNumber(number)
                + ", presentation=" + presentation + " ci " + ci);

        // "ABSENT NUMBER" is a possible value we could get from the network as the
        // phone number, so if this happens, change it to "Unknown" in the CallerInfo
        // and fix the presentation to be the same.
        final String[] absentNumberValues =
                context.getResources().getStringArray(R.array.absent_num);
        if (Arrays.asList(absentNumberValues).contains(number)
                && presentation == Call.PRESENTATION_ALLOWED) {
            number = context.getString(R.string.unknown);
            ci.numberPresentation = Call.PRESENTATION_UNKNOWN;
        }

        // Check for other special "corner cases" for CNAP and fix them similarly. Corner
        // cases only apply if we received an allowed presentation from the network, so check
        // if we think we have an allowed presentation, or if the CallerInfo presentation doesn't
        // match the presentation passed in for verification (meaning we changed it previously
        // because it's a corner case and we're being called from a different entry point).
        if (ci.numberPresentation == Call.PRESENTATION_ALLOWED
                || (ci.numberPresentation != presentation
                        && presentation == Call.PRESENTATION_ALLOWED)) {
            int cnapSpecialCase = checkCnapSpecialCases(number);
            if (cnapSpecialCase != CNAP_SPECIAL_CASE_NO) {
                // For all special strings, change number & numberPresentation.
                if (cnapSpecialCase == Call.PRESENTATION_RESTRICTED) {
                    number = context.getString(R.string.private_num);
                } else if (cnapSpecialCase == Call.PRESENTATION_UNKNOWN) {
                    number = context.getString(R.string.unknown);
                }
                Log.d(TAG, "SpecialCnap: number=" + toLogSafePhoneNumber(number)
                        + "; presentation now=" + cnapSpecialCase);
                ci.numberPresentation = cnapSpecialCase;
            }
        }
        Log.d(TAG, "modifyForSpecialCnapCases: returning number string="
                + toLogSafePhoneNumber(number));
        return number;
    }

    /**
     * Based on the input CNAP number string,
     * @return _RESTRICTED or _UNKNOWN for all the special CNAP strings.
     * Otherwise, return CNAP_SPECIAL_CASE_NO.
     */
    private static int checkCnapSpecialCases(String n) {
        if (n.equals("PRIVATE") ||
                n.equals("P") ||
                n.equals("RES")) {
            Log.d(TAG, "checkCnapSpecialCases, PRIVATE string: " + n);
            return Call.PRESENTATION_RESTRICTED;
        } else if (n.equals("UNAVAILABLE") ||
                n.equals("UNKNOWN") ||
                n.equals("UNA") ||
                n.equals("U")) {
            Log.d(TAG, "checkCnapSpecialCases, UNKNOWN string: " + n);
            return Call.PRESENTATION_UNKNOWN;
        } else {
            Log.d(TAG, "checkCnapSpecialCases, normal str. number: " + n);
            return CNAP_SPECIAL_CASE_NO;
        }
    }

    /* package */static String toLogSafePhoneNumber(String number) {
        // For unknown number, log empty string.
        if (number == null) {
            return "";
        }

        // Todo: Figure out an equivalent for VDBG
        if (false) {
            // When VDBG is true we emit PII.
            return number;
        }

        // Do exactly same thing as Uri#toSafeString() does, which will enable us to compare
        // sanitized phone numbers.
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c == '-' || c == '@' || c == '.' || c == '&') {
                builder.append(c);
            } else {
                builder.append('x');
            }
        }
        return builder.toString();
    }

    /**
     * Send a notification that that we are viewing a particular contact, so that the high-res
     * photo is downloaded by the sync adapter.
     */
    public static void sendViewNotification(Context context, Uri contactUri) {
        final Intent intent = new Intent(VIEW_NOTIFICATION_ACTION, contactUri);
        intent.setClassName(VIEW_NOTIFICATION_PACKAGE, VIEW_NOTIFICATION_CLASS);
        context.startService(intent);
    }
}
