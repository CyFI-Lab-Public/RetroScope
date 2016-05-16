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

package com.android.phone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsApplication;

import java.util.ArrayList;

/**
 * Helper class to manage the "Respond via Message" feature for incoming calls.
 *
 * @see com.android.phone.InCallScreen.internalRespondViaSms()
 */
public class RejectWithTextMessageManager {
    private static final String TAG = RejectWithTextMessageManager.class.getSimpleName();
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    /** SharedPreferences file name for our persistent settings. */
    private static final String SHARED_PREFERENCES_NAME = "respond_via_sms_prefs";

    // Preference keys for the 4 "canned responses"; see RespondViaSmsManager$Settings.
    // Since (for now at least) the number of messages is fixed at 4, and since
    // SharedPreferences can't deal with arrays anyway, just store the messages
    // as 4 separate strings.
    private static final int NUM_CANNED_RESPONSES = 4;
    private static final String KEY_CANNED_RESPONSE_PREF_1 = "canned_response_pref_1";
    private static final String KEY_CANNED_RESPONSE_PREF_2 = "canned_response_pref_2";
    private static final String KEY_CANNED_RESPONSE_PREF_3 = "canned_response_pref_3";
    private static final String KEY_CANNED_RESPONSE_PREF_4 = "canned_response_pref_4";

    /**
     * Read the (customizable) canned responses from SharedPreferences,
     * or from defaults if the user has never actually brought up
     * the Settings UI.
     *
     * This method does disk I/O (reading the SharedPreferences file)
     * so don't call it from the main thread.
     *
     * @see com.android.phone.RejectWithTextMessageManager.Settings
     */
    public static ArrayList<String> loadCannedResponses() {
        if (DBG) log("loadCannedResponses()...");

        final SharedPreferences prefs = PhoneGlobals.getInstance().getSharedPreferences(
                SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        final Resources res = PhoneGlobals.getInstance().getResources();

        final ArrayList<String> responses = new ArrayList<String>(NUM_CANNED_RESPONSES);

        // Note the default values here must agree with the corresponding
        // android:defaultValue attributes in respond_via_sms_settings.xml.

        responses.add(0, prefs.getString(KEY_CANNED_RESPONSE_PREF_1,
                                       res.getString(R.string.respond_via_sms_canned_response_1)));
        responses.add(1, prefs.getString(KEY_CANNED_RESPONSE_PREF_2,
                                       res.getString(R.string.respond_via_sms_canned_response_2)));
        responses.add(2, prefs.getString(KEY_CANNED_RESPONSE_PREF_3,
                                       res.getString(R.string.respond_via_sms_canned_response_3)));
        responses.add(3, prefs.getString(KEY_CANNED_RESPONSE_PREF_4,
                                       res.getString(R.string.respond_via_sms_canned_response_4)));
        return responses;
    }

    private static void showMessageSentToast(final String phoneNumber) {
        // ...and show a brief confirmation to the user (since
        // otherwise it's hard to be sure that anything actually
        // happened.)
        // Ugly hack to show a toaster from a service.
        (new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                Handler innerHandler = new Handler() {
                    @Override
                    public void handleMessage(Message message) {
                        final Resources res = PhoneGlobals.getInstance().getResources();
                        final String formatString = res.getString(
                                R.string.respond_via_sms_confirmation_format);
                        final String confirmationMsg = String.format(formatString, phoneNumber);
                        Toast.makeText(PhoneGlobals.getInstance(), confirmationMsg,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void dispatchMessage(Message message) {
                        handleMessage(message);
                    }
                };

                Message message = innerHandler.obtainMessage();
                innerHandler.dispatchMessage(message);
                Looper.loop();
            }
        })).start();

        // TODO: If the device is locked, this toast won't actually ever
        // be visible!  (That's because we're about to dismiss the call
        // screen, which means that the device will return to the
        // keyguard.  But toasts aren't visible on top of the keyguard.)
        // Possible fixes:
        // (1) Is it possible to allow a specific Toast to be visible
        //     on top of the keyguard?
        // (2) Artificially delay the dismissCallScreen() call by 3
        //     seconds to allow the toast to be seen?
        // (3) Don't use a toast at all; instead use a transient state
        //     of the InCallScreen (perhaps via the InCallUiState
        //     progressIndication feature), and have that state be
        //     visible for 3 seconds before calling dismissCallScreen().
    }

    /**
     * Reject the call with the specified message. If message is null this call is ignored.
     */
    public static void rejectCallWithMessage(String phoneNumber, String message) {
        if (message != null) {
            final ComponentName component =
                    SmsApplication.getDefaultRespondViaMessageApplication(
                            PhoneGlobals.getInstance(), true /*updateIfNeeded*/);
            if (component != null) {
                // Build and send the intent
                final Uri uri = Uri.fromParts(Constants.SCHEME_SMSTO, phoneNumber, null);
                final Intent intent = new Intent(TelephonyManager.ACTION_RESPOND_VIA_MESSAGE, uri);
                intent.putExtra(Intent.EXTRA_TEXT, message);
                showMessageSentToast(phoneNumber);
                intent.setComponent(component);
                PhoneGlobals.getInstance().startService(intent);
            }
        }
    }

    /**
     * @return true if the "Respond via SMS" feature should be enabled
     * for the specified incoming call.
     *
     * The general rule is that we *do* allow "Respond via SMS" except for
     * the few (relatively rare) cases where we know for sure it won't
     * work, namely:
     *   - a bogus or blank incoming number
     *   - a call from a SIP address
     *   - a "call presentation" that doesn't allow the number to be revealed
     *
     * In all other cases, we allow the user to respond via SMS.
     *
     * Note that this behavior isn't perfect; for example we have no way
     * to detect whether the incoming call is from a landline (with most
     * networks at least), so we still enable this feature even though
     * SMSes to that number will silently fail.
     */
    public static boolean allowRespondViaSmsForCall(
            com.android.services.telephony.common.Call call, Connection conn) {
        if (DBG) log("allowRespondViaSmsForCall(" + call + ")...");

        // First some basic sanity checks:
        if (call == null) {
            Log.w(TAG, "allowRespondViaSmsForCall: null ringingCall!");
            return false;
        }
        if (!(call.getState() == com.android.services.telephony.common.Call.State.INCOMING) &&
                !(call.getState() ==
                        com.android.services.telephony.common.Call.State.CALL_WAITING)) {
            // The call is in some state other than INCOMING or WAITING!
            // (This should almost never happen, but it *could*
            // conceivably happen if the ringing call got disconnected by
            // the network just *after* we got it from the CallManager.)
            Log.w(TAG, "allowRespondViaSmsForCall: ringingCall not ringing! state = "
                    + call.getState());
            return false;
        }

        if (conn == null) {
            // The call doesn't have any connections! (Again, this can
            // happen if the ringing call disconnects at the exact right
            // moment, but should almost never happen in practice.)
            Log.w(TAG, "allowRespondViaSmsForCall: null Connection!");
            return false;
        }

        // Check the incoming number:
        final String number = conn.getAddress();
        if (DBG) log("- number: '" + number + "'");
        if (TextUtils.isEmpty(number)) {
            Log.w(TAG, "allowRespondViaSmsForCall: no incoming number!");
            return false;
        }
        if (PhoneNumberUtils.isUriNumber(number)) {
            // The incoming number is actually a URI (i.e. a SIP address),
            // not a regular PSTN phone number, and we can't send SMSes to
            // SIP addresses.
            // (TODO: That might still be possible eventually, though. Is
            // there some SIP-specific equivalent to sending a text message?)
            Log.i(TAG, "allowRespondViaSmsForCall: incoming 'number' is a SIP address.");
            return false;
        }

        // Finally, check the "call presentation":
        int presentation = conn.getNumberPresentation();
        if (DBG) log("- presentation: " + presentation);
        if (presentation == PhoneConstants.PRESENTATION_RESTRICTED) {
            // PRESENTATION_RESTRICTED means "caller-id blocked".
            // The user isn't allowed to see the number in the first
            // place, so obviously we can't let you send an SMS to it.
            Log.i(TAG, "allowRespondViaSmsForCall: PRESENTATION_RESTRICTED.");
            return false;
        }

        // Is there a valid SMS application on the phone?
        if (SmsApplication.getDefaultRespondViaMessageApplication(PhoneGlobals.getInstance(),
                true /*updateIfNeeded*/) == null) {
            return false;
        }

        // TODO: with some carriers (in certain countries) you *can* actually
        // tell whether a given number is a mobile phone or not. So in that
        // case we could potentially return false here if the incoming call is
        // from a land line.

        // If none of the above special cases apply, it's OK to enable the
        // "Respond via SMS" feature.
        return true;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
