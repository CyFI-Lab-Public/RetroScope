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

package com.android.phone;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.TelephonyProperties;
import com.android.phone.OtaUtils.CdmaOtaInCallScreenUiState.State;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Handles all OTASP Call related logic and UI functionality.
 * The InCallScreen interacts with this class to perform an OTASP Call.
 *
 * OTASP is a CDMA-specific feature:
 *   OTA or OTASP == Over The Air service provisioning
 *   SPC == Service Programming Code
 *   TODO: Include pointer to more detailed documentation.
 *
 * TODO: This is Over The Air Service Provisioning (OTASP)
 *       A better name would be OtaspUtils.java.
 */
public class OtaUtils {
    private static final String LOG_TAG = "OtaUtils";
    private static final boolean DBG = false;

    public static final int OTA_SHOW_ACTIVATION_SCREEN_OFF = 0;
    public static final int OTA_SHOW_ACTIVATION_SCREEN_ON = 1;
    public static final int OTA_SHOW_LISTENING_SCREEN_OFF =0;
    public static final int OTA_SHOW_LISTENING_SCREEN_ON =1;
    public static final int OTA_SHOW_ACTIVATE_FAIL_COUNT_OFF = 0;
    public static final int OTA_SHOW_ACTIVATE_FAIL_COUNT_THREE = 3;
    public static final int OTA_PLAY_SUCCESS_FAILURE_TONE_OFF = 0;
    public static final int OTA_PLAY_SUCCESS_FAILURE_TONE_ON = 1;

    // SPC Timeout is 60 seconds
    public final int OTA_SPC_TIMEOUT = 60;
    public final int OTA_FAILURE_DIALOG_TIMEOUT = 2;

    // Constants for OTASP-related Intents and intent extras.
    // Watch out: these must agree with the corresponding constants in
    // apps/SetupWizard!

    // Intent action to launch an OTASP call.
    public static final String ACTION_PERFORM_CDMA_PROVISIONING =
           "com.android.phone.PERFORM_CDMA_PROVISIONING";

    // Intent action to launch activation on a non-voice capable device
    public static final String ACTION_PERFORM_VOICELESS_CDMA_PROVISIONING =
            "com.android.phone.PERFORM_VOICELESS_CDMA_PROVISIONING";

    // Intent action to display the InCallScreen in the OTASP "activation" state.
    public static final String ACTION_DISPLAY_ACTIVATION_SCREEN =
            "com.android.phone.DISPLAY_ACTIVATION_SCREEN";

    // boolean voiceless provisioning extra that enables a "don't show this again" checkbox
    // the user can check to never see the activity upon bootup again
    public static final String EXTRA_VOICELESS_PROVISIONING_OFFER_DONTSHOW =
            "com.android.phone.VOICELESS_PROVISIONING_OFFER_DONTSHOW";

    // Activity result codes for the ACTION_PERFORM_CDMA_PROVISIONING intent
    // (see the InCallScreenShowActivation activity.)
    //
    // Note: currently, our caller won't ever actually receive the
    // RESULT_INTERACTIVE_OTASP_STARTED result code; see comments in
    // InCallScreenShowActivation.onCreate() for details.

    public static final int RESULT_INTERACTIVE_OTASP_STARTED = Activity.RESULT_FIRST_USER;
    public static final int RESULT_NONINTERACTIVE_OTASP_STARTED = Activity.RESULT_FIRST_USER + 1;
    public static final int RESULT_NONINTERACTIVE_OTASP_FAILED = Activity.RESULT_FIRST_USER + 2;

    // Testing: Extra for the ACTION_PERFORM_CDMA_PROVISIONING intent that
    // allows the caller to manually enable/disable "interactive mode" for
    // the OTASP call.   Only available in userdebug or eng builds.
    public static final String EXTRA_OVERRIDE_INTERACTIVE_MODE =
            "ota_override_interactive_mode";

    // Extra for the ACTION_PERFORM_CDMA_PROVISIONING intent, holding a
    // PendingIntent which the phone app can use to send a result code
    // back to the caller.
    public static final String EXTRA_OTASP_RESULT_CODE_PENDING_INTENT =
            "otasp_result_code_pending_intent";

    // Extra attached to the above PendingIntent that indicates
    // success or failure.
    public static final String EXTRA_OTASP_RESULT_CODE = "otasp_result_code";

    // Extra attached to the above PendingIntent that contains an error code.
    public static final String EXTRA_OTASP_ERROR_CODE = "otasp_error_code";

    public static final int OTASP_UNKNOWN = 0;
    public static final int OTASP_USER_SKIPPED = 1;  // Only meaningful with interactive OTASP
    public static final int OTASP_SUCCESS = 2;
    public static final int OTASP_FAILURE = 3;
    // failed due to CDMA_OTA_PROVISION_STATUS_SPC_RETRIES_EXCEEDED
    public static final int OTASP_FAILURE_SPC_RETRIES = 4;
    // TODO: Distinguish between interactive and non-interactive success
    // and failure.  Then, have the PendingIntent be sent after
    // interactive OTASP as well (so the caller can find out definitively
    // when interactive OTASP completes.)

    private static final String OTASP_NUMBER = "*228";
    private static final String OTASP_NUMBER_NON_INTERACTIVE = "*22899";

    private Context mContext;
    private PhoneGlobals mApplication;
    private OtaWidgetData mOtaWidgetData;

    private static boolean sIsWizardMode = true;

    // How many times do we retry maybeDoOtaCall() if the LTE state is not known yet,
    // and how long do we wait between retries
    private static final int OTA_CALL_LTE_RETRIES_MAX = 5;
    private static final int OTA_CALL_LTE_RETRY_PERIOD = 3000;
    private static int sOtaCallLteRetries = 0;

    // In "interactive mode", the OtaUtils object is tied to an
    // InCallScreen instance, where we display a bunch of UI specific to
    // the OTASP call.  But on devices that are not "voice capable", the
    // OTASP call runs in a non-interactive mode, and we don't have
    // an InCallScreen or CallCard or any OTASP UI elements at all.
    private boolean mInteractive = true;

    // used when setting speakerphone
    private final BluetoothManager mBluetoothManager;

    /**
     * OtaWidgetData class represent all OTA UI elements
     *
     * TODO(OTASP): It's really ugly for the OtaUtils object to reach into the
     *     InCallScreen like this and directly manipulate its widgets.
     *
     *     Instead, the model/view separation should be more clear: OtaUtils
     *     should only know about a higher-level abstraction of the
     *     OTASP-specific UI state (just like how the CallController uses the
     *     InCallUiState object), and the InCallScreen itself should translate
     *     that higher-level abstraction into actual onscreen views and widgets.
     */
    private class OtaWidgetData {
        public Button otaEndButton;
        public Button otaActivateButton;
        public Button otaSkipButton;
        public Button otaNextButton;
        public ToggleButton otaSpeakerButton;
        public ViewGroup otaUpperWidgets;
        public View callCardOtaButtonsFailSuccess;
        public ProgressBar otaTextProgressBar;
        public TextView otaTextSuccessFail;
        public View callCardOtaButtonsActivate;
        public View callCardOtaButtonsListenProgress;
        public TextView otaTextActivate;
        public TextView otaTextListenProgress;
        public AlertDialog spcErrorDialog;
        public AlertDialog otaFailureDialog;
        public AlertDialog otaSkipConfirmationDialog;
        public TextView otaTitle;
        public Button otaTryAgainButton;
    }

    /**
     * OtaUtils constructor.
     *
     * @param context the Context of the calling Activity or Application
     * @param interactive if true, use the InCallScreen to display the progress
     *                    and result of the OTASP call.  In practice this is
     *                    true IFF the current device is a voice-capable phone.
     *
     * Note if interactive is true, you must also call updateUiWidgets() as soon
     * as the InCallScreen instance is ready.
     */
    public OtaUtils(Context context, boolean interactive, BluetoothManager bluetoothManager) {
        if (DBG) log("OtaUtils constructor...");
        mApplication = PhoneGlobals.getInstance();
        mContext = context;
        mInteractive = interactive;
        mBluetoothManager = bluetoothManager;
    }

    /**
     * Starts the OTA provisioning call.  If the MIN isn't available yet, it returns false and adds
     * an event to return the request to the calling app when it becomes available.
     *
     * @param context
     * @param handler
     * @param request
     * @return true if we were able to launch Ota activity or it's not required; false otherwise
     */
    public static boolean maybeDoOtaCall(Context context, Handler handler, int request) {
        PhoneGlobals app = PhoneGlobals.getInstance();
        Phone phone = app.phone;

        if (ActivityManager.isRunningInTestHarness()) {
            Log.i(LOG_TAG, "Don't run provisioning when in test harness");
            return true;
        }

        if (!TelephonyCapabilities.supportsOtasp(phone)) {
            // Presumably not a CDMA phone.
            if (DBG) log("maybeDoOtaCall: OTASP not supported on this device");
            return true;  // Nothing to do here.
        }

        if (!phone.isMinInfoReady()) {
            if (DBG) log("MIN is not ready. Registering to receive notification.");
            phone.registerForSubscriptionInfoReady(handler, request, null);
            return false;
        }
        phone.unregisterForSubscriptionInfoReady(handler);

        if (getLteOnCdmaMode(context) == PhoneConstants.LTE_ON_CDMA_UNKNOWN) {
            if (sOtaCallLteRetries < OTA_CALL_LTE_RETRIES_MAX) {
                if (DBG) log("maybeDoOtaCall: LTE state still unknown: retrying");
                handler.sendEmptyMessageDelayed(request, OTA_CALL_LTE_RETRY_PERIOD);
                sOtaCallLteRetries++;
                return false;
            } else {
                Log.w(LOG_TAG, "maybeDoOtaCall: LTE state still unknown: giving up");
                return true;
            }
        }

        boolean phoneNeedsActivation = phone.needsOtaServiceProvisioning();
        if (DBG) log("phoneNeedsActivation is set to " + phoneNeedsActivation);

        int otaShowActivationScreen = context.getResources().getInteger(
                R.integer.OtaShowActivationScreen);
        if (DBG) log("otaShowActivationScreen: " + otaShowActivationScreen);

        // Run the OTASP call in "interactive" mode only if
        // this is a non-LTE "voice capable" device.
        if (PhoneGlobals.sVoiceCapable && getLteOnCdmaMode(context) == PhoneConstants.LTE_ON_CDMA_FALSE) {
            if (phoneNeedsActivation
                    && (otaShowActivationScreen == OTA_SHOW_ACTIVATION_SCREEN_ON)) {
                app.cdmaOtaProvisionData.isOtaCallIntentProcessed = false;
                sIsWizardMode = false;

                if (DBG) Log.d(LOG_TAG, "==> Starting interactive CDMA provisioning...");
                OtaUtils.startInteractiveOtasp(context);

                if (DBG) log("maybeDoOtaCall: voice capable; activation started.");
            } else {
                if (DBG) log("maybeDoOtaCall: voice capable; activation NOT started.");
            }
        } else {
            if (phoneNeedsActivation) {
                app.cdmaOtaProvisionData.isOtaCallIntentProcessed = false;
                Intent newIntent = new Intent(ACTION_PERFORM_VOICELESS_CDMA_PROVISIONING);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                newIntent.putExtra(EXTRA_VOICELESS_PROVISIONING_OFFER_DONTSHOW, true);
                try {
                    context.startActivity(newIntent);
                } catch (ActivityNotFoundException e) {
                    loge("No activity Handling PERFORM_VOICELESS_CDMA_PROVISIONING!");
                    return false;
                }
                if (DBG) log("maybeDoOtaCall: non-interactive; activation intent sent.");
            } else {
                if (DBG) log("maybeDoOtaCall: non-interactive, no need for OTASP.");
            }
        }
        return true;
    }

    /**
     * Starts a normal "interactive" OTASP call (i.e. CDMA activation
     * for regular voice-capable phone devices.)
     *
     * This method is called from the InCallScreenShowActivation activity when
     * handling the ACTION_PERFORM_CDMA_PROVISIONING intent.
     */
    public static void startInteractiveOtasp(Context context) {
        if (DBG) log("startInteractiveOtasp()...");
        PhoneGlobals app = PhoneGlobals.getInstance();

        // There are two ways to start OTASP on voice-capable devices:
        //
        // (1) via the PERFORM_CDMA_PROVISIONING intent
        //     - this is triggered by the "Activate device" button in settings,
        //       or can be launched automatically upon boot if the device
        //       thinks it needs to be provisioned.
        //     - the intent is handled by InCallScreenShowActivation.onCreate(),
        //       which calls this method
        //     - we prepare for OTASP by initializing the OtaUtils object
        //     - we bring up the InCallScreen in the ready-to-activate state
        //     - when the user presses the "Activate" button we launch the
        //       call by calling CallController.placeCall() via the
        //       otaPerformActivation() method.
        //
        // (2) by manually making an outgoing call to a special OTASP number
        //     like "*228" or "*22899".
        //     - That sequence does NOT involve this method (OtaUtils.startInteractiveOtasp()).
        //       Instead, the outgoing call request goes straight to CallController.placeCall().
        //     - CallController.placeCall() notices that it's an OTASP
        //       call, and initializes the OtaUtils object.
        //     - The InCallScreen is launched (as the last step of
        //       CallController.placeCall()).  The InCallScreen notices that
        //       OTASP is active and shows the correct UI.

        // Here, we start sequence (1):
        // Do NOT immediately start the call.  Instead, bring up the InCallScreen
        // in the special "activate" state (see OtaUtils.otaShowActivateScreen()).
        // We won't actually make the call until the user presses the "Activate"
        // button.

        Intent activationScreenIntent = new Intent().setClass(context, InCallScreen.class)
                .setAction(ACTION_DISPLAY_ACTIVATION_SCREEN);

        // Watch out: in the scenario where OTASP gets triggered from the
        // BOOT_COMPLETED broadcast (see OtaStartupReceiver.java), we might be
        // running in the PhoneApp's context right now.
        // So the FLAG_ACTIVITY_NEW_TASK flag is required here.
        activationScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // We're about to start the OTASP sequence, so create and initialize the
        // OtaUtils instance.  (This needs to happen before bringing up the
        // InCallScreen.)
        OtaUtils.setupOtaspCall(activationScreenIntent);

        // And bring up the InCallScreen...
        Log.i(LOG_TAG, "startInteractiveOtasp: launching InCallScreen in 'activate' state: "
              + activationScreenIntent);
        context.startActivity(activationScreenIntent);
    }

    /**
     * Starts the OTASP call *without* involving the InCallScreen or
     * displaying any UI.
     *
     * This is used on data-only devices, which don't support any kind of
     * in-call phone UI.
     *
     * @return PhoneUtils.CALL_STATUS_DIALED if we successfully
     *         dialed the OTASP number, or one of the other
     *         CALL_STATUS_* constants if there was a failure.
     */
    public static int startNonInteractiveOtasp(Context context) {
        if (DBG) log("startNonInteractiveOtasp()...");
        PhoneGlobals app = PhoneGlobals.getInstance();

        if (app.otaUtils != null) {
            // An OtaUtils instance already exists, presumably from a previous OTASP call.
            Log.i(LOG_TAG, "startNonInteractiveOtasp: "
                  + "OtaUtils already exists; nuking the old one and starting again...");
        }

        // Create the OtaUtils instance.
        app.otaUtils = new OtaUtils(context, false /* non-interactive mode */,
                app.getBluetoothManager());
        if (DBG) log("- created OtaUtils: " + app.otaUtils);

        // ... and kick off the OTASP call.
        // TODO(InCallScreen redesign): This should probably go through
        // the CallController, rather than directly calling
        // PhoneUtils.placeCall().
        Phone phone = PhoneGlobals.getPhone();
        String number = OTASP_NUMBER_NON_INTERACTIVE;
        Log.i(LOG_TAG, "startNonInteractiveOtasp: placing call to '" + number + "'...");
        int callStatus = PhoneUtils.placeCall(context,
                                              phone,
                                              number,
                                              null,   // contactRef
                                              false); //isEmergencyCall

        if (callStatus == PhoneUtils.CALL_STATUS_DIALED) {
            if (DBG) log("  ==> successful return from placeCall(): callStatus = " + callStatus);
        } else {
            Log.w(LOG_TAG, "Failure from placeCall() for OTA number '"
                  + number + "': code " + callStatus);
            return callStatus;
        }

        // TODO: Any other special work to do here?
        // Such as:
        //
        // - manually kick off progress updates, either using TelephonyRegistry
        //   or else by sending PendingIntents directly to our caller?
        //
        // - manually silence the in-call audio?  (Probably unnecessary
        //   if Stingray truly has no audio path from phone baseband
        //   to the device's speakers.)
        //

        return callStatus;
    }

    /**
     * @return true if the specified Intent is a CALL action that's an attempt
     * to initate an OTASP call.
     *
     * OTASP is a CDMA-specific concept, so this method will always return false
     * on GSM phones.
     *
     * This code was originally part of the InCallScreen.checkIsOtaCall() method.
     */
    public static boolean isOtaspCallIntent(Intent intent) {
        if (DBG) log("isOtaspCallIntent(" + intent + ")...");
        PhoneGlobals app = PhoneGlobals.getInstance();
        Phone phone = app.mCM.getDefaultPhone();

        if (intent == null) {
            return false;
        }
        if (!TelephonyCapabilities.supportsOtasp(phone)) {
            return false;
        }

        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        if (!action.equals(Intent.ACTION_CALL)) {
            if (DBG) log("isOtaspCallIntent: not a CALL action: '" + action + "' ==> not OTASP");
            return false;
        }

        if ((app.cdmaOtaScreenState == null) || (app.cdmaOtaProvisionData == null)) {
            // Uh oh -- something wrong with our internal OTASP state.
            // (Since this is an OTASP-capable device, these objects
            // *should* have already been created by PhoneApp.onCreate().)
            throw new IllegalStateException("isOtaspCallIntent: "
                                            + "app.cdmaOta* objects(s) not initialized");
        }

        // This is an OTASP call iff the number we're trying to dial is one of
        // the magic OTASP numbers.
        String number;
        try {
            number = PhoneUtils.getInitialNumber(intent);
        } catch (PhoneUtils.VoiceMailNumberMissingException ex) {
            // This was presumably a "voicemail:" intent, so it's
            // obviously not an OTASP number.
            if (DBG) log("isOtaspCallIntent: VoiceMailNumberMissingException => not OTASP");
            return false;
        }
        if (phone.isOtaSpNumber(number)) {
            if (DBG) log("isOtaSpNumber: ACTION_CALL to '" + number + "' ==> OTASP call!");
            return true;
        }
        return false;
    }

    /**
     * Set up for an OTASP call.
     *
     * This method is called as part of the CallController placeCall() sequence
     * before initiating an outgoing OTASP call.
     *
     * The purpose of this method is mainly to create and initialize the
     * OtaUtils instance, along with some other misc pre-OTASP cleanup.
     */
    public static void setupOtaspCall(Intent intent) {
        if (DBG) log("setupOtaspCall(): preparing for OTASP call to " + intent);
        PhoneGlobals app = PhoneGlobals.getInstance();

        if (app.otaUtils != null) {
            // An OtaUtils instance already exists, presumably from a prior OTASP call.
            // Nuke the old one and start this call with a fresh instance.
            Log.i(LOG_TAG, "setupOtaspCall: "
                  + "OtaUtils already exists; replacing with new instance...");
        }

        // Create the OtaUtils instance.
        app.otaUtils = new OtaUtils(app.getApplicationContext(), true /* interactive */,
                app.getBluetoothManager());
        if (DBG) log("- created OtaUtils: " + app.otaUtils);

        // NOTE we still need to call OtaUtils.updateUiWidgets() once the
        // InCallScreen instance is ready; see InCallScreen.checkOtaspStateOnResume()

        // Make sure the InCallScreen knows that it needs to switch into OTASP mode.
        //
        // NOTE in gingerbread and earlier, we used to do
        //     setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
        // directly in the InCallScreen, back when this check happened inside the InCallScreen.
        //
        // But now, set the global CdmaOtaInCallScreenUiState object into
        // NORMAL mode, which will then cause the InCallScreen (when it
        // comes up) to realize that an OTA call is active.

        app.otaUtils.setCdmaOtaInCallScreenUiState(
            OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL);

        // TODO(OTASP): note app.inCallUiState.inCallScreenMode and
        // app.cdmaOtaInCallScreenUiState.state are mostly redundant.  Combine them.
        // app.inCallUiState.inCallScreenMode = InCallUiState.InCallScreenMode.OTA_NORMAL;

        // TODO(OTASP / bug 5092031): we ideally should call
        // otaShowListeningScreen() here to make sure that the DTMF dialpad
        // becomes visible at the start of the "*228" call:
        //
        //  // ...and get the OTASP-specific UI into the right state.
        //  app.otaUtils.otaShowListeningScreen();
        //  if (app.otaUtils.mInCallScreen != null) {
        //      app.otaUtils.mInCallScreen.requestUpdateScreen();
        //  }
        //
        // But this doesn't actually work; the call to otaShowListeningScreen()
        // *doesn't* actually bring up the listening screen, since the
        // cdmaOtaConfigData.otaShowListeningScreen config parameter hasn't been
        // initialized (we haven't run readXmlSettings() yet at this point!)

        // Also, since the OTA call is now just starting, clear out
        // the "committed" flag in app.cdmaOtaProvisionData.
        if (app.cdmaOtaProvisionData != null) {
            app.cdmaOtaProvisionData.isOtaCallCommitted = false;
        }
    }

    private void setSpeaker(boolean state) {
        if (DBG) log("setSpeaker : " + state );

        if (!mInteractive) {
            if (DBG) log("non-interactive mode, ignoring setSpeaker.");
            return;
        }

        if (state == PhoneUtils.isSpeakerOn(mContext)) {
            if (DBG) log("no change. returning");
            return;
        }

        if (state && mBluetoothManager.isBluetoothAvailable()
                && mBluetoothManager.isBluetoothAudioConnected()) {
            mBluetoothManager.disconnectBluetoothAudio();
        }
        PhoneUtils.turnOnSpeaker(mContext, state, true);
    }

    /**
     * Handles OTA Provision events from the telephony layer.
     * These events come in to this method whether or not
     * the InCallScreen is visible.
     *
     * Possible events are:
     * OTA Commit Event - OTA provisioning was successful
     * SPC retries exceeded - SPC failure retries has exceeded, and Phone needs to
     *    power down.
     */
    public void onOtaProvisionStatusChanged(AsyncResult r) {
        int OtaStatus[] = (int[]) r.result;
        if (DBG) log("Provision status event!");
        if (DBG) log("onOtaProvisionStatusChanged(): status = "
                     + OtaStatus[0] + " ==> " + otaProvisionStatusToString(OtaStatus[0]));

        // In practice, in a normal successful OTASP call, events come in as follows:
        //   - SPL_UNLOCKED within a couple of seconds after the call starts
        //   - then a delay of around 45 seconds
        //   - then PRL_DOWNLOADED and MDN_DOWNLOADED and COMMITTED within a span of 2 seconds

        switch(OtaStatus[0]) {
            case Phone.CDMA_OTA_PROVISION_STATUS_SPC_RETRIES_EXCEEDED:
                if (DBG) log("onOtaProvisionStatusChanged(): RETRIES EXCEEDED");
                updateOtaspProgress();
                mApplication.cdmaOtaProvisionData.otaSpcUptime = SystemClock.elapsedRealtime();
                if (mInteractive) {
                    otaShowSpcErrorNotice(OTA_SPC_TIMEOUT);
                } else {
                    sendOtaspResult(OTASP_FAILURE_SPC_RETRIES);
                }
                // Power.shutdown();
                break;

            case Phone.CDMA_OTA_PROVISION_STATUS_COMMITTED:
                if (DBG) {
                    log("onOtaProvisionStatusChanged(): DONE, isOtaCallCommitted set to true");
                }
                mApplication.cdmaOtaProvisionData.isOtaCallCommitted = true;
                if (mApplication.cdmaOtaScreenState.otaScreenState !=
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED) {
                    updateOtaspProgress();
                }

                break;

            case Phone.CDMA_OTA_PROVISION_STATUS_SPL_UNLOCKED:
            case Phone.CDMA_OTA_PROVISION_STATUS_A_KEY_EXCHANGED:
            case Phone.CDMA_OTA_PROVISION_STATUS_SSD_UPDATED:
            case Phone.CDMA_OTA_PROVISION_STATUS_NAM_DOWNLOADED:
            case Phone.CDMA_OTA_PROVISION_STATUS_MDN_DOWNLOADED:
            case Phone.CDMA_OTA_PROVISION_STATUS_IMSI_DOWNLOADED:
            case Phone.CDMA_OTA_PROVISION_STATUS_PRL_DOWNLOADED:
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STARTED:
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED:
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_ABORTED:
                // Only update progress when OTA call is in normal state
                if (getCdmaOtaInCallScreenUiState() == CdmaOtaInCallScreenUiState.State.NORMAL) {
                    if (DBG) log("onOtaProvisionStatusChanged(): change to ProgressScreen");
                    updateOtaspProgress();
                }
                break;

            default:
                if (DBG) log("onOtaProvisionStatusChanged(): Ignoring OtaStatus " + OtaStatus[0]);
                break;
        }
    }

    /**
     * Handle a disconnect event from the OTASP call.
     */
    public void onOtaspDisconnect() {
        if (DBG) log("onOtaspDisconnect()...");
        // We only handle this event explicitly in non-interactive mode.
        // (In interactive mode, the InCallScreen does any post-disconnect
        // cleanup.)
        if (!mInteractive) {
            // Send a success or failure indication back to our caller.
            updateNonInteractiveOtaSuccessFailure();
        }
    }

    private void otaShowHome() {
        if (DBG) log("otaShowHome()...");
        mApplication.cdmaOtaScreenState.otaScreenState =
                CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED;
        // mInCallScreen.endInCallScreenSession();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory (Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        return;
    }

    private void otaSkipActivation() {
        if (DBG) log("otaSkipActivation()...");

        sendOtaspResult(OTASP_USER_SKIPPED);

        // if (mInteractive) mInCallScreen.finish();
        return;
    }

    /**
     * Actually initiate the OTASP call.  This method is triggered by the
     * onscreen "Activate" button, and is only used in interactive mode.
     */
    private void otaPerformActivation() {
        if (DBG) log("otaPerformActivation()...");
        if (!mInteractive) {
            // We shouldn't ever get here in non-interactive mode!
            Log.w(LOG_TAG, "otaPerformActivation: not interactive!");
            return;
        }

        if (!mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            // Place an outgoing call to the special OTASP number:
            Intent newIntent = new Intent(Intent.ACTION_CALL);
            newIntent.setData(Uri.fromParts(Constants.SCHEME_TEL, OTASP_NUMBER, null));

            // Initiate the outgoing call:
            mApplication.callController.placeCall(newIntent);

            // ...and get the OTASP-specific UI into the right state.
            otaShowListeningScreen();
            // mInCallScreen.requestUpdateScreen();
        }
        return;
    }

    /**
     * Show Activation Screen when phone powers up and OTA provision is
     * required. Also shown when activation fails and user needs
     * to re-attempt it. Contains ACTIVATE and SKIP buttons
     * which allow user to start OTA activation or skip the activation process.
     */
    public void otaShowActivateScreen() {
        if (DBG) log("otaShowActivateScreen()...");
        if (mApplication.cdmaOtaConfigData.otaShowActivationScreen
                == OTA_SHOW_ACTIVATION_SCREEN_ON) {
            if (DBG) log("otaShowActivateScreen(): show activation screen");
            if (!isDialerOpened()) {
                otaScreenInitialize();
                mOtaWidgetData.otaSkipButton.setVisibility(sIsWizardMode ?
                        View.VISIBLE : View.INVISIBLE);
                mOtaWidgetData.otaTextActivate.setVisibility(View.VISIBLE);
                mOtaWidgetData.callCardOtaButtonsActivate.setVisibility(View.VISIBLE);
            }
            mApplication.cdmaOtaScreenState.otaScreenState =
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
        } else {
            if (DBG) log("otaShowActivateScreen(): show home screen");
            otaShowHome();
        }
     }

    /**
     * Show "Listen for Instruction" screen during OTA call. Shown when OTA Call
     * is initiated and user needs to listen for network instructions and press
     * appropriate DTMF digits to proceed to the "Programming in Progress" phase.
     */
    private void otaShowListeningScreen() {
        if (DBG) log("otaShowListeningScreen()...");
        if (!mInteractive) {
            // We shouldn't ever get here in non-interactive mode!
            Log.w(LOG_TAG, "otaShowListeningScreen: not interactive!");
            return;
        }

        if (mApplication.cdmaOtaConfigData.otaShowListeningScreen
                == OTA_SHOW_LISTENING_SCREEN_ON) {
            if (DBG) log("otaShowListeningScreen(): show listening screen");
            if (!isDialerOpened()) {
                otaScreenInitialize();
                mOtaWidgetData.otaTextListenProgress.setVisibility(View.VISIBLE);
                mOtaWidgetData.otaTextListenProgress.setText(R.string.ota_listen);
                // mOtaWidgetData.otaDtmfDialerView.setVisibility(View.VISIBLE);
                mOtaWidgetData.callCardOtaButtonsListenProgress.setVisibility(View.VISIBLE);
                mOtaWidgetData.otaSpeakerButton.setVisibility(View.VISIBLE);
                boolean speakerOn = PhoneUtils.isSpeakerOn(mContext);
                mOtaWidgetData.otaSpeakerButton.setChecked(speakerOn);
            }
            mApplication.cdmaOtaScreenState.otaScreenState =
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING;
        } else {
            if (DBG) log("otaShowListeningScreen(): show progress screen");
            otaShowInProgressScreen();
        }
    }

    /**
     * Do any necessary updates (of onscreen UI, for example)
     * based on the latest status of the OTASP call.
     */
    private void updateOtaspProgress() {
        if (DBG) log("updateOtaspProgress()...  mInteractive = " + mInteractive);
        if (mInteractive) {
            // On regular phones we just call through to
            // otaShowInProgressScreen(), which updates the
            // InCallScreen's onscreen UI.
            otaShowInProgressScreen();
        } else {
            // We're not using the InCallScreen to show OTA progress.

            // For now, at least, there's nothing to do here.
            // The overall "success" or "failure" indication we send back
            // (to our caller) is triggered by the DISCONNECT event;
            // see updateNonInteractiveOtaSuccessFailure().

            // But if we ever need to send *intermediate* progress updates back
            // to our caller, we'd do that here, possbily using the same
            // PendingIntent that we already use to indicate success or failure.
        }
    }

    /**
     * When a non-interactive OTASP call completes, send a success or
     * failure indication back to our caller.
     *
     * This is basically the non-interactive equivalent of
     * otaShowSuccessFailure().
     */
    private void updateNonInteractiveOtaSuccessFailure() {
        // This is basically the same logic as otaShowSuccessFailure(): we
        // check the isOtaCallCommitted bit, and if that's true it means
        // that activation was successful.

        if (DBG) log("updateNonInteractiveOtaSuccessFailure(): isOtaCallCommitted = "
                     + mApplication.cdmaOtaProvisionData.isOtaCallCommitted);
        int resultCode =
                mApplication.cdmaOtaProvisionData.isOtaCallCommitted
                ? OTASP_SUCCESS : OTASP_FAILURE;
        sendOtaspResult(resultCode);
    }

    /**
     * Sends the specified OTASP result code back to our caller (presumably
     * SetupWizard) via the PendingIntent that they originally sent along with
     * the ACTION_PERFORM_CDMA_PROVISIONING intent.
     */
    private void sendOtaspResult(int resultCode) {
        if (DBG) log("sendOtaspResult: resultCode = " + resultCode);

        // Pass the success or failure indication back to our caller by
        // adding an additional extra to the PendingIntent we already
        // have.
        // (NB: there's a PendingIntent send() method that takes a resultCode
        // directly, but we can't use that here since that call is only
        // meaningful for pending intents that are actually used as activity
        // results.)

        Intent extraStuff = new Intent();
        extraStuff.putExtra(EXTRA_OTASP_RESULT_CODE, resultCode);
        // When we call PendingIntent.send() below, the extras from this
        // intent will get merged with any extras already present in
        // cdmaOtaScreenState.otaspResultCodePendingIntent.

        if (mApplication.cdmaOtaScreenState == null) {
            Log.e(LOG_TAG, "updateNonInteractiveOtaSuccessFailure: no cdmaOtaScreenState object!");
            return;
        }
        if (mApplication.cdmaOtaScreenState.otaspResultCodePendingIntent == null) {
            Log.w(LOG_TAG, "updateNonInteractiveOtaSuccessFailure: "
                  + "null otaspResultCodePendingIntent!");
            return;
        }

        try {
            if (DBG) log("- sendOtaspResult:  SENDING PENDING INTENT: " +
                         mApplication.cdmaOtaScreenState.otaspResultCodePendingIntent);
            mApplication.cdmaOtaScreenState.otaspResultCodePendingIntent.send(
                    mContext,
                    0, /* resultCode (unused) */
                    extraStuff);
        } catch (CanceledException e) {
            // should never happen because no code cancels the pending intent right now,
            Log.e(LOG_TAG, "PendingIntent send() failed: " + e);
        }
    }

    /**
     * Show "Programming In Progress" screen during OTA call. Shown when OTA
     * provisioning is in progress after user has selected an option.
     */
    private void otaShowInProgressScreen() {
        if (DBG) log("otaShowInProgressScreen()...");
        if (!mInteractive) {
            // We shouldn't ever get here in non-interactive mode!
            Log.w(LOG_TAG, "otaShowInProgressScreen: not interactive!");
            return;
        }

        mApplication.cdmaOtaScreenState.otaScreenState =
            CdmaOtaScreenState.OtaScreenState.OTA_STATUS_PROGRESS;

        if ((mOtaWidgetData == null) /* || (mInCallScreen == null) */) {
            Log.w(LOG_TAG, "otaShowInProgressScreen: UI widgets not set up yet!");

            // TODO(OTASP): our CdmaOtaScreenState is now correct; we just set
            // it to OTA_STATUS_PROGRESS.  But we still need to make sure that
            // when the InCallScreen eventually comes to the foreground, it
            // notices that state and does all the same UI updating we do below.
            return;
        }

        if (!isDialerOpened()) {
            otaScreenInitialize();
            mOtaWidgetData.otaTextListenProgress.setVisibility(View.VISIBLE);
            mOtaWidgetData.otaTextListenProgress.setText(R.string.ota_progress);
            mOtaWidgetData.otaTextProgressBar.setVisibility(View.VISIBLE);
            mOtaWidgetData.callCardOtaButtonsListenProgress.setVisibility(View.VISIBLE);
            mOtaWidgetData.otaSpeakerButton.setVisibility(View.VISIBLE);
            boolean speakerOn = PhoneUtils.isSpeakerOn(mContext);
            mOtaWidgetData.otaSpeakerButton.setChecked(speakerOn);
        }
    }

    /**
     * Show programming failure dialog when OTA provisioning fails.
     * If OTA provisioning attempts fail more than 3 times, then unsuccessful
     * dialog is shown. Otherwise a two-second notice is shown with unsuccessful
     * information. When notice expires, phone returns to activation screen.
     */
    private void otaShowProgramFailure(int length) {
        if (DBG) log("otaShowProgramFailure()...");
        mApplication.cdmaOtaProvisionData.activationCount++;
        if ((mApplication.cdmaOtaProvisionData.activationCount <
                mApplication.cdmaOtaConfigData.otaShowActivateFailTimes)
                && (mApplication.cdmaOtaConfigData.otaShowActivationScreen ==
                OTA_SHOW_ACTIVATION_SCREEN_ON)) {
            if (DBG) log("otaShowProgramFailure(): activationCount"
                    + mApplication.cdmaOtaProvisionData.activationCount);
            if (DBG) log("otaShowProgramFailure(): show failure notice");
            otaShowProgramFailureNotice(length);
        } else {
            if (DBG) log("otaShowProgramFailure(): show failure dialog");
            otaShowProgramFailureDialog();
        }
    }

    /**
     * Show either programming success dialog when OTA provisioning succeeds, or
     * programming failure dialog when it fails. See {@link #otaShowProgramFailure}
     * for more details.
     */
    public void otaShowSuccessFailure() {
        if (DBG) log("otaShowSuccessFailure()...");
        if (!mInteractive) {
            // We shouldn't ever get here in non-interactive mode!
            Log.w(LOG_TAG, "otaShowSuccessFailure: not interactive!");
            return;
        }

        otaScreenInitialize();
        if (DBG) log("otaShowSuccessFailure(): isOtaCallCommitted"
                + mApplication.cdmaOtaProvisionData.isOtaCallCommitted);
        if (mApplication.cdmaOtaProvisionData.isOtaCallCommitted) {
            if (DBG) log("otaShowSuccessFailure(), show success dialog");
            otaShowProgramSuccessDialog();
        } else {
            if (DBG) log("otaShowSuccessFailure(), show failure dialog");
            otaShowProgramFailure(OTA_FAILURE_DIALOG_TIMEOUT);
        }
        return;
    }

    /**
     * Show programming failure dialog when OTA provisioning fails more than 3
     * times.
     */
    private void otaShowProgramFailureDialog() {
        if (DBG) log("otaShowProgramFailureDialog()...");
        mApplication.cdmaOtaScreenState.otaScreenState =
                CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG;
        mOtaWidgetData.otaTitle.setText(R.string.ota_title_problem_with_activation);
        mOtaWidgetData.otaTextSuccessFail.setVisibility(View.VISIBLE);
        mOtaWidgetData.otaTextSuccessFail.setText(R.string.ota_unsuccessful);
        mOtaWidgetData.callCardOtaButtonsFailSuccess.setVisibility(View.VISIBLE);
        mOtaWidgetData.otaTryAgainButton.setVisibility(View.VISIBLE);
        //close the dialer if open
        // if (isDialerOpened()) {
        //     mOtaCallCardDtmfDialer.closeDialer(false);
        // }
    }

    /**
     * Show programming success dialog when OTA provisioning succeeds.
     */
    private void otaShowProgramSuccessDialog() {
        if (DBG) log("otaShowProgramSuccessDialog()...");
        mApplication.cdmaOtaScreenState.otaScreenState =
                CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG;
        mOtaWidgetData.otaTitle.setText(R.string.ota_title_activate_success);
        mOtaWidgetData.otaTextSuccessFail.setVisibility(View.VISIBLE);
        mOtaWidgetData.otaTextSuccessFail.setText(R.string.ota_successful);
        mOtaWidgetData.callCardOtaButtonsFailSuccess.setVisibility(View.VISIBLE);
        mOtaWidgetData.otaNextButton.setVisibility(View.VISIBLE);
        //close the dialer if open
        // if (isDialerOpened()) {
        //     mOtaCallCardDtmfDialer.closeDialer(false);
        // }
    }

    /**
     * Show SPC failure notice when SPC attempts exceed 15 times.
     * During OTA provisioning, if SPC code is incorrect OTA provisioning will
     * fail. When SPC attempts are over 15, it shows SPC failure notice for one minute and
     * then phone will power down.
     */
    private void otaShowSpcErrorNotice(int length) {
        if (DBG) log("otaShowSpcErrorNotice()...");
        if (mOtaWidgetData.spcErrorDialog == null) {
            mApplication.cdmaOtaProvisionData.inOtaSpcState = true;
            DialogInterface.OnKeyListener keyListener;
            keyListener = new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    log("Ignoring key events...");
                    return true;
                }};
            mOtaWidgetData.spcErrorDialog = new AlertDialog.Builder(null /* mInCallScreen */)
                    .setMessage(R.string.ota_spc_failure)
                    .setOnKeyListener(keyListener)
                    .create();
            mOtaWidgetData.spcErrorDialog.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mOtaWidgetData.spcErrorDialog.show();
            //close the dialer if open
            // if (isDialerOpened()) {
            //     mOtaCallCardDtmfDialer.closeDialer(false);
            // }
            long noticeTime = length*1000;
            if (DBG) log("otaShowSpcErrorNotice(), remaining SPC noticeTime" + noticeTime);
            // mInCallScreen.requestCloseSpcErrorNotice(noticeTime);
        }
    }

    /**
     * When SPC notice times out, force phone to power down.
     */
    public void onOtaCloseSpcNotice() {
        if (DBG) log("onOtaCloseSpcNotice(), send shutdown intent");
        Intent shutdown = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
        shutdown.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
        shutdown.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(shutdown);
    }

    /**
     * Show two-second notice when OTA provisioning fails and number of failed attempts
     * is less then 3.
     */
    private void otaShowProgramFailureNotice(int length) {
        if (DBG) log("otaShowProgramFailureNotice()...");
        if (mOtaWidgetData.otaFailureDialog == null) {
            mOtaWidgetData.otaFailureDialog = new AlertDialog.Builder(null /* mInCallScreen */)
                    .setMessage(R.string.ota_failure)
                    .create();
            mOtaWidgetData.otaFailureDialog.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mOtaWidgetData.otaFailureDialog.show();

            long noticeTime = length*1000;
            // mInCallScreen.requestCloseOtaFailureNotice(noticeTime);
        }
    }

    /**
     * Handle OTA unsuccessful notice expiry. Dismisses the
     * two-second notice and shows the activation screen.
     */
    public void onOtaCloseFailureNotice() {
        if (DBG) log("onOtaCloseFailureNotice()...");
        if (mOtaWidgetData.otaFailureDialog != null) {
            mOtaWidgetData.otaFailureDialog.dismiss();
            mOtaWidgetData.otaFailureDialog = null;
        }
        otaShowActivateScreen();
    }

    /**
     * Initialize all OTA UI elements to be gone. Also set inCallPanel,
     * callCard and the dialpad handle to be gone. This is called before any OTA screen
     * gets drawn.
     */
    private void otaScreenInitialize() {
        if (DBG) log("otaScreenInitialize()...");

        if (!mInteractive) {
            // We should never be doing anything with UI elements in
            // non-interactive mode.
            Log.w(LOG_TAG, "otaScreenInitialize: not interactive!");
            return;
        }

        // if (mInCallTouchUi != null) mInCallTouchUi.setVisibility(View.GONE);
        // if (mCallCard != null) {
        //     mCallCard.setVisibility(View.GONE);
        //     // TODO: try removing this.
        //     mCallCard.hideCallCardElements();
        // }

        mOtaWidgetData.otaTitle.setText(R.string.ota_title_activate);
        mOtaWidgetData.otaTextActivate.setVisibility(View.GONE);
        mOtaWidgetData.otaTextListenProgress.setVisibility(View.GONE);
        mOtaWidgetData.otaTextProgressBar.setVisibility(View.GONE);
        mOtaWidgetData.otaTextSuccessFail.setVisibility(View.GONE);
        mOtaWidgetData.callCardOtaButtonsActivate.setVisibility(View.GONE);
        mOtaWidgetData.callCardOtaButtonsListenProgress.setVisibility(View.GONE);
        mOtaWidgetData.callCardOtaButtonsFailSuccess.setVisibility(View.GONE);
        // mOtaWidgetData.otaDtmfDialerView.setVisibility(View.GONE);
        mOtaWidgetData.otaSpeakerButton.setVisibility(View.GONE);
        mOtaWidgetData.otaTryAgainButton.setVisibility(View.GONE);
        mOtaWidgetData.otaNextButton.setVisibility(View.GONE);
        mOtaWidgetData.otaUpperWidgets.setVisibility(View.VISIBLE);
        mOtaWidgetData.otaSkipButton.setVisibility(View.VISIBLE);
    }

    public void hideOtaScreen() {
        if (DBG) log("hideOtaScreen()...");

        mOtaWidgetData.callCardOtaButtonsActivate.setVisibility(View.GONE);
        mOtaWidgetData.callCardOtaButtonsListenProgress.setVisibility(View.GONE);
        mOtaWidgetData.callCardOtaButtonsFailSuccess.setVisibility(View.GONE);
        mOtaWidgetData.otaUpperWidgets.setVisibility(View.GONE);
    }

    public boolean isDialerOpened() {
        // boolean retval = (mOtaCallCardDtmfDialer != null && mOtaCallCardDtmfDialer.isOpened());
        boolean retval = false;
        if (DBG) log("- isDialerOpened() ==> " + retval);
        return retval;
    }

    /**
     * Show the appropriate OTA screen based on the current state of OTA call.
     *
     * This is called from the InCallScreen when the screen needs to be
     * refreshed (and thus is only ever used in interactive mode.)
     *
     * Since this is called as part of the InCallScreen.updateScreen() sequence,
     * this method does *not* post an mInCallScreen.requestUpdateScreen()
     * request.
     */
    public void otaShowProperScreen() {
        if (DBG) log("otaShowProperScreen()...");
        if (!mInteractive) {
            // We shouldn't ever get here in non-interactive mode!
            Log.w(LOG_TAG, "otaShowProperScreen: not interactive!");
            return;
        }

        // if ((mInCallScreen != null) && mInCallScreen.isForegroundActivity()) {
        //     if (DBG) log("otaShowProperScreen(): InCallScreen in foreground, currentstate = "
        //             + mApplication.cdmaOtaScreenState.otaScreenState);
        //     if (mInCallTouchUi != null) {
        //         mInCallTouchUi.setVisibility(View.GONE);
        //     }
        //     if (mCallCard != null) {
        //         mCallCard.setVisibility(View.GONE);
        //     }
        //     if (mApplication.cdmaOtaScreenState.otaScreenState
        //             == CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION) {
        //         otaShowActivateScreen();
        //     } else if (mApplication.cdmaOtaScreenState.otaScreenState
        //             == CdmaOtaScreenState.OtaScreenState.OTA_STATUS_LISTENING) {
        //         otaShowListeningScreen();
        //     } else if (mApplication.cdmaOtaScreenState.otaScreenState
        //             == CdmaOtaScreenState.OtaScreenState.OTA_STATUS_PROGRESS) {
        //         otaShowInProgressScreen();
        //     }

        //     if (mApplication.cdmaOtaProvisionData.inOtaSpcState) {
        //         otaShowSpcErrorNotice(getOtaSpcDisplayTime());
        //     }
        // }
    }

    /**
     * Read configuration values for each OTA screen from config.xml.
     * These configuration values control visibility of each screen.
     */
    private void readXmlSettings() {
        if (DBG) log("readXmlSettings()...");
        if (mApplication.cdmaOtaConfigData.configComplete) {
            return;
        }

        mApplication.cdmaOtaConfigData.configComplete = true;
        int tmpOtaShowActivationScreen =
                mContext.getResources().getInteger(R.integer.OtaShowActivationScreen);
        mApplication.cdmaOtaConfigData.otaShowActivationScreen = tmpOtaShowActivationScreen;
        if (DBG) log("readXmlSettings(), otaShowActivationScreen = "
                + mApplication.cdmaOtaConfigData.otaShowActivationScreen);

        int tmpOtaShowListeningScreen =
                mContext.getResources().getInteger(R.integer.OtaShowListeningScreen);
        mApplication.cdmaOtaConfigData.otaShowListeningScreen = tmpOtaShowListeningScreen;
        if (DBG) log("readXmlSettings(), otaShowListeningScreen = "
                + mApplication.cdmaOtaConfigData.otaShowListeningScreen);

        int tmpOtaShowActivateFailTimes =
                mContext.getResources().getInteger(R.integer.OtaShowActivateFailTimes);
        mApplication.cdmaOtaConfigData.otaShowActivateFailTimes = tmpOtaShowActivateFailTimes;
        if (DBG) log("readXmlSettings(), otaShowActivateFailTimes = "
                + mApplication.cdmaOtaConfigData.otaShowActivateFailTimes);

        int tmpOtaPlaySuccessFailureTone =
                mContext.getResources().getInteger(R.integer.OtaPlaySuccessFailureTone);
        mApplication.cdmaOtaConfigData.otaPlaySuccessFailureTone = tmpOtaPlaySuccessFailureTone;
        if (DBG) log("readXmlSettings(), otaPlaySuccessFailureTone = "
                + mApplication.cdmaOtaConfigData.otaPlaySuccessFailureTone);
    }

    /**
     * Handle the click events for OTA buttons.
     */
    public void onClickHandler(int id) {
        switch (id) {
            case R.id.otaEndButton:
                onClickOtaEndButton();
                break;

            case R.id.otaSpeakerButton:
                onClickOtaSpeakerButton();
                break;

            case R.id.otaActivateButton:
                onClickOtaActivateButton();
                break;

            case R.id.otaSkipButton:
                onClickOtaActivateSkipButton();
                break;

            case R.id.otaNextButton:
                onClickOtaActivateNextButton();
                break;

            case R.id.otaTryAgainButton:
                onClickOtaTryAgainButton();
                break;

            default:
                if (DBG) log ("onClickHandler: received a click event for unrecognized id");
                break;
        }
    }

    private void onClickOtaTryAgainButton() {
        if (DBG) log("Activation Try Again Clicked!");
        if (!mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            otaShowActivateScreen();
        }
    }

    private void onClickOtaEndButton() {
        if (DBG) log("Activation End Call Button Clicked!");
        if (!mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            if (PhoneUtils.hangup(mApplication.mCM) == false) {
                // If something went wrong when placing the OTA call,
                // the screen is not updated by the call disconnect
                // handler and we have to do it here
                setSpeaker(false);
                // mInCallScreen.handleOtaCallEnd();
            }
        }
    }

    private void onClickOtaSpeakerButton() {
        if (DBG) log("OTA Speaker button Clicked!");
        if (!mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            boolean isChecked = !PhoneUtils.isSpeakerOn(mContext);
            setSpeaker(isChecked);
        }
    }

    private void onClickOtaActivateButton() {
        if (DBG) log("Call Activation Clicked!");
        otaPerformActivation();
    }

    private void onClickOtaActivateSkipButton() {
        if (DBG) log("Activation Skip Clicked!");
        DialogInterface.OnKeyListener keyListener;
        keyListener = new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode,
                    KeyEvent event) {
                if (DBG) log("Ignoring key events...");
                return true;
            }
        };
        mOtaWidgetData.otaSkipConfirmationDialog = new AlertDialog.Builder(null /* mInCallScreen */)
                .setTitle(R.string.ota_skip_activation_dialog_title)
                .setMessage(R.string.ota_skip_activation_dialog_message)
                .setPositiveButton(
                    android.R.string.ok,
                    // "OK" means "skip activation".
                    new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            otaSkipActivation();
                        }
                    })
                .setNegativeButton(
                    android.R.string.cancel,
                    // "Cancel" means just dismiss the dialog.
                    // Don't actually start an activation call.
                    null)
                .setOnKeyListener(keyListener)
                .create();
        mOtaWidgetData.otaSkipConfirmationDialog.show();
    }

    private void onClickOtaActivateNextButton() {
        if (DBG) log("Dialog Next Clicked!");
        if (!mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            mApplication.cdmaOtaScreenState.otaScreenState =
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED;
            otaShowHome();
        }
    }

    public void dismissAllOtaDialogs() {
        if (mOtaWidgetData != null) {
            if (mOtaWidgetData.spcErrorDialog != null) {
                if (DBG) log("- DISMISSING mSpcErrorDialog.");
                mOtaWidgetData.spcErrorDialog.dismiss();
                mOtaWidgetData.spcErrorDialog = null;
            }
            if (mOtaWidgetData.otaFailureDialog != null) {
                if (DBG) log("- DISMISSING mOtaFailureDialog.");
                mOtaWidgetData.otaFailureDialog.dismiss();
                mOtaWidgetData.otaFailureDialog = null;
            }
        }
    }

    private int getOtaSpcDisplayTime() {
        if (DBG) log("getOtaSpcDisplayTime()...");
        int tmpSpcTime = 1;
        if (mApplication.cdmaOtaProvisionData.inOtaSpcState) {
            long tmpOtaSpcRunningTime = 0;
            long tmpOtaSpcLeftTime = 0;
            tmpOtaSpcRunningTime = SystemClock.elapsedRealtime();
            tmpOtaSpcLeftTime =
                tmpOtaSpcRunningTime - mApplication.cdmaOtaProvisionData.otaSpcUptime;
            if (tmpOtaSpcLeftTime >= OTA_SPC_TIMEOUT*1000) {
                tmpSpcTime = 1;
            } else {
                tmpSpcTime = OTA_SPC_TIMEOUT - (int)tmpOtaSpcLeftTime/1000;
            }
        }
        if (DBG) log("getOtaSpcDisplayTime(), time for SPC error notice: " + tmpSpcTime);
        return tmpSpcTime;
    }

    /**
     * Initialize the OTA widgets for all OTA screens.
     */
    private void initOtaInCallScreen() {
        if (DBG) log("initOtaInCallScreen()...");
        // mOtaWidgetData.otaTitle = (TextView) mInCallScreen.findViewById(R.id.otaTitle);
        // mOtaWidgetData.otaTextActivate = (TextView) mInCallScreen.findViewById(R.id.otaActivate);
        mOtaWidgetData.otaTextActivate.setVisibility(View.GONE);
        // mOtaWidgetData.otaTextListenProgress =
        //         (TextView) mInCallScreen.findViewById(R.id.otaListenProgress);
        // mOtaWidgetData.otaTextProgressBar =
        //         (ProgressBar) mInCallScreen.findViewById(R.id.progress_large);
        mOtaWidgetData.otaTextProgressBar.setIndeterminate(true);
        // mOtaWidgetData.otaTextSuccessFail =
        //         (TextView) mInCallScreen.findViewById(R.id.otaSuccessFailStatus);

        // mOtaWidgetData.otaUpperWidgets =
        //         (ViewGroup) mInCallScreen.findViewById(R.id.otaUpperWidgets);
        // mOtaWidgetData.callCardOtaButtonsListenProgress =
        //         (View) mInCallScreen.findViewById(R.id.callCardOtaListenProgress);
        // mOtaWidgetData.callCardOtaButtonsActivate =
        //         (View) mInCallScreen.findViewById(R.id.callCardOtaActivate);
        // mOtaWidgetData.callCardOtaButtonsFailSuccess =
        //         (View) mInCallScreen.findViewById(R.id.callCardOtaFailOrSuccessful);

        // mOtaWidgetData.otaEndButton = (Button) mInCallScreen.findViewById(R.id.otaEndButton);
        // mOtaWidgetData.otaEndButton.setOnClickListener(mInCallScreen);
        // mOtaWidgetData.otaSpeakerButton =
        //         (ToggleButton) mInCallScreen.findViewById(R.id.otaSpeakerButton);
        // mOtaWidgetData.otaSpeakerButton.setOnClickListener(mInCallScreen);
        // mOtaWidgetData.otaActivateButton =
        //         (Button) mInCallScreen.findViewById(R.id.otaActivateButton);
        // mOtaWidgetData.otaActivateButton.setOnClickListener(mInCallScreen);
        // mOtaWidgetData.otaSkipButton = (Button) mInCallScreen.findViewById(R.id.otaSkipButton);
        // mOtaWidgetData.otaSkipButton.setOnClickListener(mInCallScreen);
        // mOtaWidgetData.otaNextButton = (Button) mInCallScreen.findViewById(R.id.otaNextButton);
        // mOtaWidgetData.otaNextButton.setOnClickListener(mInCallScreen);
        // mOtaWidgetData.otaTryAgainButton =
        //         (Button) mInCallScreen.findViewById(R.id.otaTryAgainButton);
        // mOtaWidgetData.otaTryAgainButton.setOnClickListener(mInCallScreen);

        // mOtaWidgetData.otaDtmfDialerView =
        //         (DTMFTwelveKeyDialerView) mInCallScreen.findViewById(R.id.otaDtmfDialerView);
        // Sanity-check: the otaDtmfDialerView widget should *always* be present.
        // if (mOtaWidgetData.otaDtmfDialerView == null) {
        //     throw new IllegalStateException("initOtaInCallScreen: couldn't find otaDtmfDialerView");
        // }

        // Create a new DTMFTwelveKeyDialer instance purely for use by the
        // DTMFTwelveKeyDialerView ("otaDtmfDialerView") that comes from
        // otacall_card.xml.
        // mOtaCallCardDtmfDialer = new DTMFTwelveKeyDialer(mInCallScreen,
        //                                                  mOtaWidgetData.otaDtmfDialerView);

        // Initialize the new DTMFTwelveKeyDialer instance.  This is
        // needed to play local DTMF tones.
        // mOtaCallCardDtmfDialer.startDialerSession();

        // mOtaWidgetData.otaDtmfDialerView.setDialer(mOtaCallCardDtmfDialer);
    }

    /**
     * Clear out all OTA UI widget elements. Needs to get called
     * when OTA call ends or InCallScreen is destroyed.
     * @param disableSpeaker parameter control whether Speaker should be turned off.
     */
    public void cleanOtaScreen(boolean disableSpeaker) {
        if (DBG) log("OTA ends, cleanOtaScreen!");

        mApplication.cdmaOtaScreenState.otaScreenState =
                CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED;
        mApplication.cdmaOtaProvisionData.isOtaCallCommitted = false;
        mApplication.cdmaOtaProvisionData.isOtaCallIntentProcessed = false;
        mApplication.cdmaOtaProvisionData.inOtaSpcState = false;
        mApplication.cdmaOtaProvisionData.activationCount = 0;
        mApplication.cdmaOtaProvisionData.otaSpcUptime = 0;
        mApplication.cdmaOtaInCallScreenUiState.state = State.UNDEFINED;

        if (mInteractive && (mOtaWidgetData != null)) {
            // if (mInCallTouchUi != null) mInCallTouchUi.setVisibility(View.VISIBLE);
            // if (mCallCard != null) {
            //     mCallCard.setVisibility(View.VISIBLE);
            //     mCallCard.hideCallCardElements();
            // }

            // Free resources from the DTMFTwelveKeyDialer instance we created
            // in initOtaInCallScreen().
            // if (mOtaCallCardDtmfDialer != null) {
            //     mOtaCallCardDtmfDialer.stopDialerSession();
            // }

            mOtaWidgetData.otaTextActivate.setVisibility(View.GONE);
            mOtaWidgetData.otaTextListenProgress.setVisibility(View.GONE);
            mOtaWidgetData.otaTextProgressBar.setVisibility(View.GONE);
            mOtaWidgetData.otaTextSuccessFail.setVisibility(View.GONE);
            mOtaWidgetData.callCardOtaButtonsActivate.setVisibility(View.GONE);
            mOtaWidgetData.callCardOtaButtonsListenProgress.setVisibility(View.GONE);
            mOtaWidgetData.callCardOtaButtonsFailSuccess.setVisibility(View.GONE);
            mOtaWidgetData.otaUpperWidgets.setVisibility(View.GONE);
            // mOtaWidgetData.otaDtmfDialerView.setVisibility(View.GONE);
            mOtaWidgetData.otaNextButton.setVisibility(View.GONE);
            mOtaWidgetData.otaTryAgainButton.setVisibility(View.GONE);
        }

        // turn off the speaker in case it was turned on
        // but the OTA call could not be completed
        if (disableSpeaker) {
            setSpeaker(false);
        }
    }

    /**
     * Defines OTA information that needs to be maintained during
     * an OTA call when display orientation changes.
     */
    public static class CdmaOtaProvisionData {
        public boolean isOtaCallCommitted;
        public boolean isOtaCallIntentProcessed;
        public boolean inOtaSpcState;
        public int activationCount;
        public long otaSpcUptime;
    }

    /**
     * Defines OTA screen configuration items read from config.xml
     * and used to control OTA display.
     */
    public static class CdmaOtaConfigData {
        public int otaShowActivationScreen;
        public int otaShowListeningScreen;
        public int otaShowActivateFailTimes;
        public int otaPlaySuccessFailureTone;
        public boolean configComplete;
        public CdmaOtaConfigData() {
            if (DBG) log("CdmaOtaConfigData constructor!");
            otaShowActivationScreen = OTA_SHOW_ACTIVATION_SCREEN_OFF;
            otaShowListeningScreen = OTA_SHOW_LISTENING_SCREEN_OFF;
            otaShowActivateFailTimes = OTA_SHOW_ACTIVATE_FAIL_COUNT_OFF;
            otaPlaySuccessFailureTone = OTA_PLAY_SUCCESS_FAILURE_TONE_OFF;
        }
    }

    /**
     * The state of the OTA InCallScreen UI.
     */
    public static class CdmaOtaInCallScreenUiState {
        public enum State {
            UNDEFINED,
            NORMAL,
            ENDED
        }

        public State state;

        public CdmaOtaInCallScreenUiState() {
            if (DBG) log("CdmaOtaInCallScreenState: constructor init to UNDEFINED");
            state = CdmaOtaInCallScreenUiState.State.UNDEFINED;
        }
    }

    /**
     * Save the Ota InCallScreen UI state
     */
    public void setCdmaOtaInCallScreenUiState(CdmaOtaInCallScreenUiState.State state) {
        if (DBG) log("setCdmaOtaInCallScreenState: " + state);
        mApplication.cdmaOtaInCallScreenUiState.state = state;
    }

    /**
     * Get the Ota InCallScreen UI state
     */
    public CdmaOtaInCallScreenUiState.State getCdmaOtaInCallScreenUiState() {
        if (DBG) log("getCdmaOtaInCallScreenState: "
                     + mApplication.cdmaOtaInCallScreenUiState.state);
        return mApplication.cdmaOtaInCallScreenUiState.state;
    }

    /**
     * The OTA screen state machine.
     */
    public static class CdmaOtaScreenState {
        public enum OtaScreenState {
            OTA_STATUS_UNDEFINED,
            OTA_STATUS_ACTIVATION,
            OTA_STATUS_LISTENING,
            OTA_STATUS_PROGRESS,
            OTA_STATUS_SUCCESS_FAILURE_DLG
        }

        public OtaScreenState otaScreenState;

        public CdmaOtaScreenState() {
            otaScreenState = OtaScreenState.OTA_STATUS_UNDEFINED;
        }

        /**
         * {@link PendingIntent} used to report an OTASP result status code
         * back to our caller. Can be null.
         *
         * Our caller (presumably SetupWizard) may create this PendingIntent,
         * pointing back at itself, and passes it along as an extra with the
         * ACTION_PERFORM_CDMA_PROVISIONING intent.  Then, when there's an
         * OTASP result to report, we send that PendingIntent back, adding an
         * extra called EXTRA_OTASP_RESULT_CODE to indicate the result.
         *
         * Possible result values are the OTASP_RESULT_* constants.
         */
        public PendingIntent otaspResultCodePendingIntent;
    }

    /** @see com.android.internal.telephony.Phone */
    private static String otaProvisionStatusToString(int status) {
        switch (status) {
            case Phone.CDMA_OTA_PROVISION_STATUS_SPL_UNLOCKED:
                return "SPL_UNLOCKED";
            case Phone.CDMA_OTA_PROVISION_STATUS_SPC_RETRIES_EXCEEDED:
                return "SPC_RETRIES_EXCEEDED";
            case Phone.CDMA_OTA_PROVISION_STATUS_A_KEY_EXCHANGED:
                return "A_KEY_EXCHANGED";
            case Phone.CDMA_OTA_PROVISION_STATUS_SSD_UPDATED:
                return "SSD_UPDATED";
            case Phone.CDMA_OTA_PROVISION_STATUS_NAM_DOWNLOADED:
                return "NAM_DOWNLOADED";
            case Phone.CDMA_OTA_PROVISION_STATUS_MDN_DOWNLOADED:
                return "MDN_DOWNLOADED";
            case Phone.CDMA_OTA_PROVISION_STATUS_IMSI_DOWNLOADED:
                return "IMSI_DOWNLOADED";
            case Phone.CDMA_OTA_PROVISION_STATUS_PRL_DOWNLOADED:
                return "PRL_DOWNLOADED";
            case Phone.CDMA_OTA_PROVISION_STATUS_COMMITTED:
                return "COMMITTED";
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STARTED:
                return "OTAPA_STARTED";
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_STOPPED:
                return "OTAPA_STOPPED";
            case Phone.CDMA_OTA_PROVISION_STATUS_OTAPA_ABORTED:
                return "OTAPA_ABORTED";
            default:
                return "<unknown status" + status + ">";
        }
    }

    private static int getLteOnCdmaMode(Context context) {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        // If the telephony manager is not available yet, or if it doesn't know the answer yet,
        // try falling back on the system property that may or may not be there
        if (telephonyManager == null
                || telephonyManager.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_UNKNOWN) {
            return SystemProperties.getInt(TelephonyProperties.PROPERTY_LTE_ON_CDMA_DEVICE,
                    PhoneConstants.LTE_ON_CDMA_UNKNOWN);
        }
        return telephonyManager.getLteOnCdmaMode();
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private static void loge(String msg) {
        Log.e(LOG_TAG, msg);
    }
}
