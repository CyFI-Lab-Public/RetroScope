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

package com.android.phone;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;

/**
 * OutgoingCallBroadcaster receives CALL and CALL_PRIVILEGED Intents, and
 * broadcasts the ACTION_NEW_OUTGOING_CALL intent which allows other
 * applications to monitor, redirect, or prevent the outgoing call.

 * After the other applications have had a chance to see the
 * ACTION_NEW_OUTGOING_CALL intent, it finally reaches the
 * {@link OutgoingCallReceiver}, which passes the (possibly modified)
 * intent on to the {@link SipCallOptionHandler}, which will
 * ultimately start the call using the CallController.placeCall() API.
 *
 * Emergency calls and calls where no number is present (like for a CDMA
 * "empty flash" or a nonexistent voicemail number) are exempt from being
 * broadcast.
 */
public class OutgoingCallBroadcaster extends Activity
        implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private static final String PERMISSION = android.Manifest.permission.PROCESS_OUTGOING_CALLS;
    private static final String TAG = "OutgoingCallBroadcaster";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    // Do not check in with VDBG = true, since that may write PII to the system log.
    private static final boolean VDBG = false;

    public static final String ACTION_SIP_SELECT_PHONE = "com.android.phone.SIP_SELECT_PHONE";
    public static final String EXTRA_ALREADY_CALLED = "android.phone.extra.ALREADY_CALLED";
    public static final String EXTRA_ORIGINAL_URI = "android.phone.extra.ORIGINAL_URI";
    public static final String EXTRA_NEW_CALL_INTENT = "android.phone.extra.NEW_CALL_INTENT";
    public static final String EXTRA_SIP_PHONE_URI = "android.phone.extra.SIP_PHONE_URI";
    public static final String EXTRA_ACTUAL_NUMBER_TO_DIAL =
            "android.phone.extra.ACTUAL_NUMBER_TO_DIAL";

    /**
     * Identifier for intent extra for sending an empty Flash message for
     * CDMA networks. This message is used by the network to simulate a
     * press/depress of the "hookswitch" of a landline phone. Aka "empty flash".
     *
     * TODO: Receiving an intent extra to tell the phone to send this flash is a
     * temporary measure. To be replaced with an external ITelephony call in the future.
     * TODO: Keep in sync with the string defined in TwelveKeyDialer.java in Contacts app
     * until this is replaced with the ITelephony API.
     */
    public static final String EXTRA_SEND_EMPTY_FLASH =
            "com.android.phone.extra.SEND_EMPTY_FLASH";

    // Dialog IDs
    private static final int DIALOG_NOT_VOICE_CAPABLE = 1;

    /** Note message codes < 100 are reserved for the PhoneApp. */
    private static final int EVENT_OUTGOING_CALL_TIMEOUT = 101;
    private static final int EVENT_DELAYED_FINISH = 102;

    private static final int OUTGOING_CALL_TIMEOUT_THRESHOLD = 2000; // msec
    private static final int DELAYED_FINISH_TIME = 2000; // msec

    /**
     * ProgressBar object with "spinner" style, which will be shown if we take more than
     * {@link #EVENT_OUTGOING_CALL_TIMEOUT} msec to handle the incoming Intent.
     */
    private ProgressBar mWaitingSpinner;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EVENT_OUTGOING_CALL_TIMEOUT) {
                Log.i(TAG, "Outgoing call takes too long. Showing the spinner.");
                mWaitingSpinner.setVisibility(View.VISIBLE);
            } else if (msg.what == EVENT_DELAYED_FINISH) {
                finish();
            } else {
                Log.wtf(TAG, "Unknown message id: " + msg.what);
            }
        }
    };

    /**
     * Starts the delayed finish() of OutgoingCallBroadcaster in order to give the UI
     * some time to start up.
     */
    private void startDelayedFinish() {
        mHandler.sendEmptyMessageDelayed(EVENT_DELAYED_FINISH, DELAYED_FINISH_TIME);
    }

    /**
     * OutgoingCallReceiver finishes NEW_OUTGOING_CALL broadcasts, starting
     * the InCallScreen if the broadcast has not been canceled, possibly with
     * a modified phone number and optional provider info (uri + package name + remote views.)
     */
    public class OutgoingCallReceiver extends BroadcastReceiver {
        private static final String TAG = "OutgoingCallReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.removeMessages(EVENT_OUTGOING_CALL_TIMEOUT);
            final boolean isAttemptingCall = doReceive(context, intent);
            if (DBG) Log.v(TAG, "OutgoingCallReceiver is going to finish the Activity itself.");

            // We cannot finish the activity immediately here because it would cause the temporary
            // black screen of OutgoingBroadcaster to go away and we need it to stay up until the
            // UI (in a different process) has time to come up.
            // However, if we know we are not attemping a call, we need to finish the activity
            // immediately so that subsequent CALL intents will retrigger a new
            // OutgoingCallReceiver. see b/10857203
            if (isAttemptingCall) {
                startDelayedFinish();
            } else {
                finish();
            }
        }


        /**
         * Handes receipt of ordered new_outgoing_call intent. Verifies that the return from the
         * ordered intent is valid.
         * @return true if the call is being attempted; false if we are canceling the call.
         */
        public boolean doReceive(Context context, Intent intent) {
            if (DBG) Log.v(TAG, "doReceive: " + intent);

            boolean alreadyCalled;
            String number;
            String originalUri;

            alreadyCalled = intent.getBooleanExtra(
                    OutgoingCallBroadcaster.EXTRA_ALREADY_CALLED, false);
            if (alreadyCalled) {
                if (DBG) Log.v(TAG, "CALL already placed -- returning.");
                return false;
            }

            // Once the NEW_OUTGOING_CALL broadcast is finished, the resultData
            // is used as the actual number to call. (If null, no call will be
            // placed.)

            number = getResultData();
            if (VDBG) Log.v(TAG, "- got number from resultData: '" + number + "'");

            final PhoneGlobals app = PhoneGlobals.getInstance();

            // OTASP-specific checks.
            // TODO: This should probably all happen in
            // OutgoingCallBroadcaster.onCreate(), since there's no reason to
            // even bother with the NEW_OUTGOING_CALL broadcast if we're going
            // to disallow the outgoing call anyway...
            if (TelephonyCapabilities.supportsOtasp(app.phone)) {
                boolean activateState = (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION);
                boolean dialogState = (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState
                        .OTA_STATUS_SUCCESS_FAILURE_DLG);
                boolean isOtaCallActive = false;

                // TODO: Need cleaner way to check if OTA is active.
                // Also, this check seems to be broken in one obscure case: if
                // you interrupt an OTASP call by pressing Back then Skip,
                // otaScreenState somehow gets left in either PROGRESS or
                // LISTENING.
                if ((app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_PROGRESS)
                        || (app.cdmaOtaScreenState.otaScreenState
                        == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING)) {
                    isOtaCallActive = true;
                }

                if (activateState || dialogState) {
                    // The OTASP sequence is active, but either (1) the call
                    // hasn't started yet, or (2) the call has ended and we're
                    // showing the success/failure screen.  In either of these
                    // cases it's OK to make a new outgoing call, but we need
                    // to take down any OTASP-related UI first.
                    if (dialogState) app.dismissOtaDialogs();
                    app.clearOtaState();
                } else if (isOtaCallActive) {
                    // The actual OTASP call is active.  Don't allow new
                    // outgoing calls at all from this state.
                    Log.w(TAG, "OTASP call is active: disallowing a new outgoing call.");
                    return false;
                }
            }

            if (number == null) {
                if (DBG) Log.v(TAG, "CALL cancelled (null number), returning...");
                return false;
            } else if (TelephonyCapabilities.supportsOtasp(app.phone)
                    && (app.phone.getState() != PhoneConstants.State.IDLE)
                    && (app.phone.isOtaSpNumber(number))) {
                if (DBG) Log.v(TAG, "Call is active, a 2nd OTA call cancelled -- returning.");
                return false;
            } else if (PhoneNumberUtils.isPotentialLocalEmergencyNumber(number, context)) {
                // Just like 3rd-party apps aren't allowed to place emergency
                // calls via the ACTION_CALL intent, we also don't allow 3rd
                // party apps to use the NEW_OUTGOING_CALL broadcast to rewrite
                // an outgoing call into an emergency number.
                Log.w(TAG, "Cannot modify outgoing call to emergency number " + number + ".");
                return false;
            }

            originalUri = intent.getStringExtra(
                    OutgoingCallBroadcaster.EXTRA_ORIGINAL_URI);
            if (originalUri == null) {
                Log.e(TAG, "Intent is missing EXTRA_ORIGINAL_URI -- returning.");
                return false;
            }

            Uri uri = Uri.parse(originalUri);

            // We already called convertKeypadLettersToDigits() and
            // stripSeparators() way back in onCreate(), before we sent out the
            // NEW_OUTGOING_CALL broadcast.  But we need to do it again here
            // too, since the number might have been modified/rewritten during
            // the broadcast (and may now contain letters or separators again.)
            number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
            number = PhoneNumberUtils.stripSeparators(number);

            if (DBG) Log.v(TAG, "doReceive: proceeding with call...");
            if (VDBG) Log.v(TAG, "- uri: " + uri);
            if (VDBG) Log.v(TAG, "- actual number to dial: '" + number + "'");

            startSipCallOptionHandler(context, intent, uri, number);

            return true;
        }
    }

    /**
     * Launch the SipCallOptionHandler, which is the next step(*) in the
     * outgoing-call sequence after the outgoing call broadcast is
     * complete.
     *
     * (*) We now know exactly what phone number we need to dial, so the next
     *     step is for the SipCallOptionHandler to decide which Phone type (SIP
     *     or PSTN) should be used.  (Depending on the user's preferences, this
     *     decision may also involve popping up a dialog to ask the user to
     *     choose what type of call this should be.)
     *
     * @param context used for the startActivity() call
     *
     * @param intent the intent from the previous step of the outgoing-call
     *   sequence.  Normally this will be the NEW_OUTGOING_CALL broadcast intent
     *   that came in to the OutgoingCallReceiver, although it can also be the
     *   original ACTION_CALL intent that started the whole sequence (in cases
     *   where we don't do the NEW_OUTGOING_CALL broadcast at all, like for
     *   emergency numbers or SIP addresses).
     *
     * @param uri the data URI from the original CALL intent, presumably either
     *   a tel: or sip: URI.  For tel: URIs, note that the scheme-specific part
     *   does *not* necessarily have separators and keypad letters stripped (so
     *   we might see URIs like "tel:(650)%20555-1234" or "tel:1-800-GOOG-411"
     *   here.)
     *
     * @param number the actual number (or SIP address) to dial.  This is
     *   guaranteed to be either a PSTN phone number with separators stripped
     *   out and keypad letters converted to digits (like "16505551234"), or a
     *   raw SIP address (like "user@example.com").
     */
    private void startSipCallOptionHandler(Context context, Intent intent,
            Uri uri, String number) {
        if (VDBG) {
            Log.i(TAG, "startSipCallOptionHandler...");
            Log.i(TAG, "- intent: " + intent);
            Log.i(TAG, "- uri: " + uri);
            Log.i(TAG, "- number: " + number);
        }

        // Create a copy of the original CALL intent that started the whole
        // outgoing-call sequence.  This intent will ultimately be passed to
        // CallController.placeCall() after the SipCallOptionHandler step.

        Intent newIntent = new Intent(Intent.ACTION_CALL, uri);
        newIntent.putExtra(EXTRA_ACTUAL_NUMBER_TO_DIAL, number);
        CallGatewayManager.checkAndCopyPhoneProviderExtras(intent, newIntent);

        // Finally, launch the SipCallOptionHandler, with the copy of the
        // original CALL intent stashed away in the EXTRA_NEW_CALL_INTENT
        // extra.

        Intent selectPhoneIntent = new Intent(ACTION_SIP_SELECT_PHONE, uri);
        selectPhoneIntent.setClass(context, SipCallOptionHandler.class);
        selectPhoneIntent.putExtra(EXTRA_NEW_CALL_INTENT, newIntent);
        selectPhoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (DBG) {
            Log.v(TAG, "startSipCallOptionHandler(): " +
                    "calling startActivity: " + selectPhoneIntent);
        }
        context.startActivity(selectPhoneIntent);
        // ...and see SipCallOptionHandler.onCreate() for the next step of the sequence.
    }

    /**
     * This method is the single point of entry for the CALL intent, which is used (by built-in
     * apps like Contacts / Dialer, as well as 3rd-party apps) to initiate an outgoing voice call.
     *
     *
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.outgoing_call_broadcaster);
        mWaitingSpinner = (ProgressBar) findViewById(R.id.spinner);

        Intent intent = getIntent();
        if (DBG) {
            final Configuration configuration = getResources().getConfiguration();
            Log.v(TAG, "onCreate: this = " + this + ", icicle = " + icicle);
            Log.v(TAG, " - getIntent() = " + intent);
            Log.v(TAG, " - configuration = " + configuration);
        }

        if (icicle != null) {
            // A non-null icicle means that this activity is being
            // re-initialized after previously being shut down.
            //
            // In practice this happens very rarely (because the lifetime
            // of this activity is so short!), but it *can* happen if the
            // framework detects a configuration change at exactly the
            // right moment; see bug 2202413.
            //
            // In this case, do nothing.  Our onCreate() method has already
            // run once (with icicle==null the first time), which means
            // that the NEW_OUTGOING_CALL broadcast for this new call has
            // already been sent.
            Log.i(TAG, "onCreate: non-null icicle!  "
                  + "Bailing out, not sending NEW_OUTGOING_CALL broadcast...");

            // No need to finish() here, since the OutgoingCallReceiver from
            // our original instance will do that.  (It'll actually call
            // finish() on our original instance, which apparently works fine
            // even though the ActivityManager has already shut that instance
            // down.  And note that if we *do* call finish() here, that just
            // results in an "ActivityManager: Duplicate finish request"
            // warning when the OutgoingCallReceiver runs.)

            return;
        }

        processIntent(intent);

        // isFinishing() return false when 1. broadcast is still ongoing, or 2. dialog is being
        // shown. Otherwise finish() is called inside processIntent(), is isFinishing() here will
        // return true.
        if (DBG) Log.v(TAG, "At the end of onCreate(). isFinishing(): " + isFinishing());
    }

    /**
     * Interprets a given Intent and starts something relevant to the Intent.
     *
     * This method will handle three kinds of actions:
     *
     * - CALL (action for usual outgoing voice calls)
     * - CALL_PRIVILEGED (can come from built-in apps like contacts / voice dialer / bluetooth)
     * - CALL_EMERGENCY (from the EmergencyDialer that's reachable from the lockscreen.)
     *
     * The exact behavior depends on the intent's data:
     *
     * - The most typical is a tel: URI, which we handle by starting the
     *   NEW_OUTGOING_CALL broadcast.  That broadcast eventually triggers
     *   the sequence OutgoingCallReceiver -> SipCallOptionHandler ->
     *   InCallScreen.
     *
     * - Or, with a sip: URI we skip the NEW_OUTGOING_CALL broadcast and
     *   go directly to SipCallOptionHandler, which then leads to the
     *   InCallScreen.
     *
     * - voicemail: URIs take the same path as regular tel: URIs.
     *
     * Other special cases:
     *
     * - Outgoing calls are totally disallowed on non-voice-capable
     *   devices (see handleNonVoiceCapable()).
     *
     * - A CALL intent with the EXTRA_SEND_EMPTY_FLASH extra (and
     *   presumably no data at all) means "send an empty flash" (which
     *   is only meaningful on CDMA devices while a call is already
     *   active.)
     *
     */
    private void processIntent(Intent intent) {
        if (DBG) {
            Log.v(TAG, "processIntent() = " + intent + ", thread: " + Thread.currentThread());
        }
        final Configuration configuration = getResources().getConfiguration();

        // Outgoing phone calls are only allowed on "voice-capable" devices.
        if (!PhoneGlobals.sVoiceCapable) {
            Log.i(TAG, "This device is detected as non-voice-capable device.");
            handleNonVoiceCapable(intent);
            return;
        }

        String action = intent.getAction();
        String number = PhoneNumberUtils.getNumberFromIntent(intent, this);
        // Check the number, don't convert for sip uri
        // TODO put uriNumber under PhoneNumberUtils
        if (number != null) {
            if (!PhoneNumberUtils.isUriNumber(number)) {
                number = PhoneNumberUtils.convertKeypadLettersToDigits(number);
                number = PhoneNumberUtils.stripSeparators(number);
            }
        } else {
            Log.w(TAG, "The number obtained from Intent is null.");
        }

        AppOpsManager appOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        int launchedFromUid;
        String launchedFromPackage;
        try {
            launchedFromUid = ActivityManagerNative.getDefault().getLaunchedFromUid(
                    getActivityToken());
            launchedFromPackage = ActivityManagerNative.getDefault().getLaunchedFromPackage(
                    getActivityToken());
        } catch (RemoteException e) {
            launchedFromUid = -1;
            launchedFromPackage = null;
        }
        if (appOps.noteOp(AppOpsManager.OP_CALL_PHONE, launchedFromUid, launchedFromPackage)
                != AppOpsManager.MODE_ALLOWED) {
            Log.w(TAG, "Rejecting call from uid " + launchedFromUid + " package "
                    + launchedFromPackage);
            finish();
            return;
        }

        // If true, this flag will indicate that the current call is a special kind
        // of call (most likely an emergency number) that 3rd parties aren't allowed
        // to intercept or affect in any way.  (In that case, we start the call
        // immediately rather than going through the NEW_OUTGOING_CALL sequence.)
        boolean callNow;

        if (getClass().getName().equals(intent.getComponent().getClassName())) {
            // If we were launched directly from the OutgoingCallBroadcaster,
            // not one of its more privileged aliases, then make sure that
            // only the non-privileged actions are allowed.
            if (!Intent.ACTION_CALL.equals(intent.getAction())) {
                Log.w(TAG, "Attempt to deliver non-CALL action; forcing to CALL");
                intent.setAction(Intent.ACTION_CALL);
            }
        }

        // Check whether or not this is an emergency number, in order to
        // enforce the restriction that only the CALL_PRIVILEGED and
        // CALL_EMERGENCY intents are allowed to make emergency calls.
        //
        // (Note that the ACTION_CALL check below depends on the result of
        // isPotentialLocalEmergencyNumber() rather than just plain
        // isLocalEmergencyNumber(), to be 100% certain that we *don't*
        // allow 3rd party apps to make emergency calls by passing in an
        // "invalid" number like "9111234" that isn't technically an
        // emergency number but might still result in an emergency call
        // with some networks.)
        final boolean isExactEmergencyNumber =
                (number != null) && PhoneNumberUtils.isLocalEmergencyNumber(number, this);
        final boolean isPotentialEmergencyNumber =
                (number != null) && PhoneNumberUtils.isPotentialLocalEmergencyNumber(number, this);
        if (VDBG) {
            Log.v(TAG, " - Checking restrictions for number '" + number + "':");
            Log.v(TAG, "     isExactEmergencyNumber     = " + isExactEmergencyNumber);
            Log.v(TAG, "     isPotentialEmergencyNumber = " + isPotentialEmergencyNumber);
        }

        /* Change CALL_PRIVILEGED into CALL or CALL_EMERGENCY as needed. */
        // TODO: This code is redundant with some code in InCallScreen: refactor.
        if (Intent.ACTION_CALL_PRIVILEGED.equals(action)) {
            // We're handling a CALL_PRIVILEGED intent, so we know this request came
            // from a trusted source (like the built-in dialer.)  So even a number
            // that's *potentially* an emergency number can safely be promoted to
            // CALL_EMERGENCY (since we *should* allow you to dial "91112345" from
            // the dialer if you really want to.)
            if (isPotentialEmergencyNumber) {
                Log.i(TAG, "ACTION_CALL_PRIVILEGED is used while the number is a potential"
                        + " emergency number. Use ACTION_CALL_EMERGENCY as an action instead.");
                action = Intent.ACTION_CALL_EMERGENCY;
            } else {
                action = Intent.ACTION_CALL;
            }
            if (DBG) Log.v(TAG, " - updating action from CALL_PRIVILEGED to " + action);
            intent.setAction(action);
        }

        if (Intent.ACTION_CALL.equals(action)) {
            if (isPotentialEmergencyNumber) {
                Log.w(TAG, "Cannot call potential emergency number '" + number
                        + "' with CALL Intent " + intent + ".");
                Log.i(TAG, "Launching default dialer instead...");

                Intent invokeFrameworkDialer = new Intent();

                // TwelveKeyDialer is in a tab so we really want
                // DialtactsActivity.  Build the intent 'manually' to
                // use the java resolver to find the dialer class (as
                // opposed to a Context which look up known android
                // packages only)
                final Resources resources = getResources();
                invokeFrameworkDialer.setClassName(
                        resources.getString(R.string.ui_default_package),
                        resources.getString(R.string.dialer_default_class));
                invokeFrameworkDialer.setAction(Intent.ACTION_DIAL);
                invokeFrameworkDialer.setData(intent.getData());
                if (DBG) Log.v(TAG, "onCreate(): calling startActivity for Dialer: "
                               + invokeFrameworkDialer);
                startActivity(invokeFrameworkDialer);
                finish();
                return;
            }
            callNow = false;
        } else if (Intent.ACTION_CALL_EMERGENCY.equals(action)) {
            // ACTION_CALL_EMERGENCY case: this is either a CALL_PRIVILEGED
            // intent that we just turned into a CALL_EMERGENCY intent (see
            // above), or else it really is an CALL_EMERGENCY intent that
            // came directly from some other app (e.g. the EmergencyDialer
            // activity built in to the Phone app.)
            // Make sure it's at least *possible* that this is really an
            // emergency number.
            if (!isPotentialEmergencyNumber) {
                Log.w(TAG, "Cannot call non-potential-emergency number " + number
                        + " with EMERGENCY_CALL Intent " + intent + "."
                        + " Finish the Activity immediately.");
                finish();
                return;
            }
            callNow = true;
        } else {
            Log.e(TAG, "Unhandled Intent " + intent + ". Finish the Activity immediately.");
            finish();
            return;
        }

        // Make sure the screen is turned on.  This is probably the right
        // thing to do, and more importantly it works around an issue in the
        // activity manager where we will not launch activities consistently
        // when the screen is off (since it is trying to keep them paused
        // and has...  issues).
        //
        // Also, this ensures the device stays awake while doing the following
        // broadcast; technically we should be holding a wake lock here
        // as well.
        PhoneGlobals.getInstance().wakeUpScreen();

        // If number is null, we're probably trying to call a non-existent voicemail number,
        // send an empty flash or something else is fishy.  Whatever the problem, there's no
        // number, so there's no point in allowing apps to modify the number.
        if (TextUtils.isEmpty(number)) {
            if (intent.getBooleanExtra(EXTRA_SEND_EMPTY_FLASH, false)) {
                Log.i(TAG, "onCreate: SEND_EMPTY_FLASH...");
                PhoneUtils.sendEmptyFlash(PhoneGlobals.getPhone());
                finish();
                return;
            } else {
                Log.i(TAG, "onCreate: null or empty number, setting callNow=true...");
                callNow = true;
            }
        }

        if (callNow) {
            // This is a special kind of call (most likely an emergency number)
            // that 3rd parties aren't allowed to intercept or affect in any way.
            // So initiate the outgoing call immediately.

            Log.i(TAG, "onCreate(): callNow case! Calling placeCall(): " + intent);

            // Initiate the outgoing call, and simultaneously launch the
            // InCallScreen to display the in-call UI:
            PhoneGlobals.getInstance().callController.placeCall(intent);

            // Note we do *not* "return" here, but instead continue and
            // send the ACTION_NEW_OUTGOING_CALL broadcast like for any
            // other outgoing call.  (But when the broadcast finally
            // reaches the OutgoingCallReceiver, we'll know not to
            // initiate the call again because of the presence of the
            // EXTRA_ALREADY_CALLED extra.)
        }

        // For now, SIP calls will be processed directly without a
        // NEW_OUTGOING_CALL broadcast.
        //
        // TODO: In the future, though, 3rd party apps *should* be allowed to
        // intercept outgoing calls to SIP addresses as well.  To do this, we should
        // (1) update the NEW_OUTGOING_CALL intent documentation to explain this
        // case, and (2) pass the outgoing SIP address by *not* overloading the
        // EXTRA_PHONE_NUMBER extra, but instead using a new separate extra to hold
        // the outgoing SIP address.  (Be sure to document whether it's a URI or just
        // a plain address, whether it could be a tel: URI, etc.)
        Uri uri = intent.getData();
        String scheme = uri.getScheme();
        if (Constants.SCHEME_SIP.equals(scheme) || PhoneNumberUtils.isUriNumber(number)) {
            Log.i(TAG, "The requested number was detected as SIP call.");
            startSipCallOptionHandler(this, intent, uri, number);
            finish();
            return;

            // TODO: if there's ever a way for SIP calls to trigger a
            // "callNow=true" case (see above), we'll need to handle that
            // case here too (most likely by just doing nothing at all.)
        }

        Intent broadcastIntent = new Intent(Intent.ACTION_NEW_OUTGOING_CALL);
        if (number != null) {
            broadcastIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, number);
        }
        CallGatewayManager.checkAndCopyPhoneProviderExtras(intent, broadcastIntent);
        broadcastIntent.putExtra(EXTRA_ALREADY_CALLED, callNow);
        broadcastIntent.putExtra(EXTRA_ORIGINAL_URI, uri.toString());
        // Need to raise foreground in-call UI as soon as possible while allowing 3rd party app
        // to intercept the outgoing call.
        broadcastIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        if (DBG) Log.v(TAG, " - Broadcasting intent: " + broadcastIntent + ".");

        // Set a timer so that we can prepare for unexpected delay introduced by the broadcast.
        // If it takes too much time, the timer will show "waiting" spinner.
        // This message will be removed when OutgoingCallReceiver#onReceive() is called before the
        // timeout.
        mHandler.sendEmptyMessageDelayed(EVENT_OUTGOING_CALL_TIMEOUT,
                OUTGOING_CALL_TIMEOUT_THRESHOLD);
        sendOrderedBroadcastAsUser(broadcastIntent, UserHandle.OWNER,
                PERMISSION, new OutgoingCallReceiver(),
                null,  // scheduler
                Activity.RESULT_OK,  // initialCode
                number,  // initialData: initial value for the result data
                null);  // initialExtras
    }

    @Override
    protected void onStop() {
        // Clean up (and dismiss if necessary) any managed dialogs.
        //
        // We don't do this in onPause() since we can be paused/resumed
        // due to orientation changes (in which case we don't want to
        // disturb the dialog), but we *do* need it here in onStop() to be
        // sure we clean up if the user hits HOME while the dialog is up.
        //
        // Note it's safe to call removeDialog() even if there's no dialog
        // associated with that ID.
        removeDialog(DIALOG_NOT_VOICE_CAPABLE);

        super.onStop();
    }

    /**
     * Handle the specified CALL or CALL_* intent on a non-voice-capable
     * device.
     *
     * This method may launch a different intent (if there's some useful
     * alternative action to take), or otherwise display an error dialog,
     * and in either case will finish() the current activity when done.
     */
    private void handleNonVoiceCapable(Intent intent) {
        if (DBG) Log.v(TAG, "handleNonVoiceCapable: handling " + intent
                       + " on non-voice-capable device...");

        // Just show a generic "voice calling not supported" dialog.
        showDialog(DIALOG_NOT_VOICE_CAPABLE);
        // ...and we'll eventually finish() when the user dismisses
        // or cancels the dialog.
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch(id) {
            case DIALOG_NOT_VOICE_CAPABLE:
                dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.not_voice_capable)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.ok, this)
                        .setOnCancelListener(this)
                        .create();
                break;
            default:
                Log.w(TAG, "onCreateDialog: unexpected ID " + id);
                dialog = null;
                break;
        }
        return dialog;
    }

    /** DialogInterface.OnClickListener implementation */
    @Override
    public void onClick(DialogInterface dialog, int id) {
        // DIALOG_NOT_VOICE_CAPABLE is the only dialog we ever use (so far
        // at least), and its only button is "OK".
        finish();
    }

    /** DialogInterface.OnCancelListener implementation */
    @Override
    public void onCancel(DialogInterface dialog) {
        // DIALOG_NOT_VOICE_CAPABLE is the only dialog we ever use (so far
        // at least), and canceling it is just like hitting "OK".
        finish();
    }

    /**
     * Implement onConfigurationChanged() purely for debugging purposes,
     * to make sure that the android:configChanges element in our manifest
     * is working properly.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DBG) Log.v(TAG, "onConfigurationChanged: newConfig = " + newConfig);
    }
}
