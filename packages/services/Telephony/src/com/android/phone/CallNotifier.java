/*
 * Copyright (C) 2006 The Android Open Source Project
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

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaDisplayInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaSignalInfoRec;
import com.android.internal.telephony.cdma.SignalToneUtil;

import android.app.ActivityManagerNative;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;

/**
 * Phone app module that listens for phone state changes and various other
 * events from the telephony layer, and triggers any resulting UI behavior
 * (like starting the Ringer and Incoming Call UI, playing in-call tones,
 * updating notifications, writing call log entries, etc.)
 */
public class CallNotifier extends Handler
        implements CallerInfoAsyncQuery.OnQueryCompleteListener {
    private static final String LOG_TAG = "CallNotifier";
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    // Maximum time we allow the CallerInfo query to run,
    // before giving up and falling back to the default ringtone.
    private static final int RINGTONE_QUERY_WAIT_TIME = 500;  // msec

    // Timers related to CDMA Call Waiting
    // 1) For displaying Caller Info
    // 2) For disabling "Add Call" menu option once User selects Ignore or CW Timeout occures
    private static final int CALLWAITING_CALLERINFO_DISPLAY_TIME = 20000; // msec
    private static final int CALLWAITING_ADDCALL_DISABLE_TIME = 30000; // msec

    // Time to display the  DisplayInfo Record sent by CDMA network
    private static final int DISPLAYINFO_NOTIFICATION_TIME = 2000; // msec

    /** The singleton instance. */
    private static CallNotifier sInstance;

    // Boolean to keep track of whether or not a CDMA Call Waiting call timed out.
    //
    // This is CDMA-specific, because with CDMA we *don't* get explicit
    // notification from the telephony layer that a call-waiting call has
    // stopped ringing.  Instead, when a call-waiting call first comes in we
    // start a 20-second timer (see CALLWAITING_CALLERINFO_DISPLAY_DONE), and
    // if the timer expires we clean up the call and treat it as a missed call.
    //
    // If this field is true, that means that the current Call Waiting call
    // "timed out" and should be logged in Call Log as a missed call.  If it's
    // false when we reach onCdmaCallWaitingReject(), we can assume the user
    // explicitly rejected this call-waiting call.
    //
    // This field is reset to false any time a call-waiting call first comes
    // in, and after cleaning up a missed call-waiting call.  It's only ever
    // set to true when the CALLWAITING_CALLERINFO_DISPLAY_DONE timer fires.
    //
    // TODO: do we really need a member variable for this?  Don't we always
    // know at the moment we call onCdmaCallWaitingReject() whether this is an
    // explicit rejection or not?
    // (Specifically: when we call onCdmaCallWaitingReject() from
    // PhoneUtils.hangupRingingCall() that means the user deliberately rejected
    // the call, and if we call onCdmaCallWaitingReject() because of a
    // CALLWAITING_CALLERINFO_DISPLAY_DONE event that means that it timed
    // out...)
    private boolean mCallWaitingTimeOut = false;

    // values used to track the query state
    private static final int CALLERINFO_QUERY_READY = 0;
    private static final int CALLERINFO_QUERYING = -1;

    // the state of the CallerInfo Query.
    private int mCallerInfoQueryState;

    // object used to synchronize access to mCallerInfoQueryState
    private Object mCallerInfoQueryStateGuard = new Object();

    // Event used to indicate a query timeout.
    private static final int RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT = 100;

    // Events generated internally:
    private static final int PHONE_MWI_CHANGED = 21;
    private static final int CALLWAITING_CALLERINFO_DISPLAY_DONE = 22;
    private static final int CALLWAITING_ADDCALL_DISABLE_TIMEOUT = 23;
    private static final int DISPLAYINFO_NOTIFICATION_DONE = 24;
    private static final int CDMA_CALL_WAITING_REJECT = 26;
    private static final int UPDATE_IN_CALL_NOTIFICATION = 27;

    // Emergency call related defines:
    private static final int EMERGENCY_TONE_OFF = 0;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_VIBRATE = 2;

    private PhoneGlobals mApplication;
    private CallManager mCM;
    private Ringer mRinger;
    private BluetoothHeadset mBluetoothHeadset;
    private CallLogger mCallLogger;
    private CallModeler mCallModeler;
    private boolean mSilentRingerRequested;

    // ToneGenerator instance for playing SignalInfo tones
    private ToneGenerator mSignalInfoToneGenerator;

    // The tone volume relative to other sounds in the stream SignalInfo
    private static final int TONE_RELATIVE_VOLUME_SIGNALINFO = 80;

    private Call.State mPreviousCdmaCallState;
    private boolean mVoicePrivacyState = false;
    private boolean mIsCdmaRedialCall = false;

    // Emergency call tone and vibrate:
    private int mIsEmergencyToneOn;
    private int mCurrentEmergencyToneState = EMERGENCY_TONE_OFF;
    private EmergencyTonePlayerVibrator mEmergencyTonePlayerVibrator;

    // Ringback tone player
    private InCallTonePlayer mInCallRingbackTonePlayer;

    // Call waiting tone player
    private InCallTonePlayer mCallWaitingTonePlayer;

    // Cached AudioManager
    private AudioManager mAudioManager;

    private final BluetoothManager mBluetoothManager;

    /**
     * Initialize the singleton CallNotifier instance.
     * This is only done once, at startup, from PhoneApp.onCreate().
     */
    /* package */ static CallNotifier init(PhoneGlobals app, Phone phone, Ringer ringer,
            CallLogger callLogger, CallStateMonitor callStateMonitor,
            BluetoothManager bluetoothManager, CallModeler callModeler) {
        synchronized (CallNotifier.class) {
            if (sInstance == null) {
                sInstance = new CallNotifier(app, phone, ringer, callLogger, callStateMonitor,
                        bluetoothManager, callModeler);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private CallNotifier(PhoneGlobals app, Phone phone, Ringer ringer, CallLogger callLogger,
            CallStateMonitor callStateMonitor, BluetoothManager bluetoothManager,
            CallModeler callModeler) {
        mApplication = app;
        mCM = app.mCM;
        mCallLogger = callLogger;
        mBluetoothManager = bluetoothManager;
        mCallModeler = callModeler;

        mAudioManager = (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);

        callStateMonitor.addListener(this);

        createSignalInfoToneGenerator();

        mRinger = ringer;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(mApplication.getApplicationContext(),
                                    mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }

        TelephonyManager telephonyManager = (TelephonyManager)app.getSystemService(
                Context.TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
                | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);
    }

    private void createSignalInfoToneGenerator() {
        // Instantiate the ToneGenerator for SignalInfo and CallWaiting
        // TODO: We probably don't need the mSignalInfoToneGenerator instance
        // around forever. Need to change it so as to create a ToneGenerator instance only
        // when a tone is being played and releases it after its done playing.
        if (mSignalInfoToneGenerator == null) {
            try {
                mSignalInfoToneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,
                        TONE_RELATIVE_VOLUME_SIGNALINFO);
                Log.d(LOG_TAG, "CallNotifier: mSignalInfoToneGenerator created when toneplay");
            } catch (RuntimeException e) {
                Log.w(LOG_TAG, "CallNotifier: Exception caught while creating " +
                        "mSignalInfoToneGenerator: " + e);
                mSignalInfoToneGenerator = null;
            }
        } else {
            Log.d(LOG_TAG, "mSignalInfoToneGenerator created already, hence skipping");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case CallStateMonitor.PHONE_NEW_RINGING_CONNECTION:
                log("RINGING... (new)");
                onNewRingingConnection((AsyncResult) msg.obj);
                mSilentRingerRequested = false;
                break;

            case CallStateMonitor.PHONE_INCOMING_RING:
                // repeat the ring when requested by the RIL, and when the user has NOT
                // specifically requested silence.
                if (msg.obj != null && ((AsyncResult) msg.obj).result != null) {
                    PhoneBase pb =  (PhoneBase)((AsyncResult)msg.obj).result;

                    if ((pb.getState() == PhoneConstants.State.RINGING)
                            && (mSilentRingerRequested == false)) {
                        if (DBG) log("RINGING... (PHONE_INCOMING_RING event)");
                        mRinger.ring();
                    } else {
                        if (DBG) log("RING before NEW_RING, skipping");
                    }
                }
                break;

            case CallStateMonitor.PHONE_STATE_CHANGED:
                onPhoneStateChanged((AsyncResult) msg.obj);
                break;

            case CallStateMonitor.PHONE_DISCONNECT:
                if (DBG) log("DISCONNECT");
                onDisconnect((AsyncResult) msg.obj);
                break;

            case CallStateMonitor.PHONE_UNKNOWN_CONNECTION_APPEARED:
                onUnknownConnectionAppeared((AsyncResult) msg.obj);
                break;

            case RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT:
                onCustomRingtoneQueryTimeout((Connection) msg.obj);
                break;

            case PHONE_MWI_CHANGED:
                onMwiChanged(mApplication.phone.getMessageWaitingIndicator());
                break;

            case CallStateMonitor.PHONE_CDMA_CALL_WAITING:
                if (DBG) log("Received PHONE_CDMA_CALL_WAITING event");
                onCdmaCallWaiting((AsyncResult) msg.obj);
                break;

            case CDMA_CALL_WAITING_REJECT:
                Log.i(LOG_TAG, "Received CDMA_CALL_WAITING_REJECT event");
                onCdmaCallWaitingReject();
                break;

            case CALLWAITING_CALLERINFO_DISPLAY_DONE:
                Log.i(LOG_TAG, "Received CALLWAITING_CALLERINFO_DISPLAY_DONE event");
                mCallWaitingTimeOut = true;
                onCdmaCallWaitingReject();
                break;

            case CALLWAITING_ADDCALL_DISABLE_TIMEOUT:
                if (DBG) log("Received CALLWAITING_ADDCALL_DISABLE_TIMEOUT event ...");
                // Set the mAddCallMenuStateAfterCW state to true
                mApplication.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(true);
                break;

            case CallStateMonitor.PHONE_STATE_DISPLAYINFO:
                if (DBG) log("Received PHONE_STATE_DISPLAYINFO event");
                onDisplayInfo((AsyncResult) msg.obj);
                break;

            case CallStateMonitor.PHONE_STATE_SIGNALINFO:
                if (DBG) log("Received PHONE_STATE_SIGNALINFO event");
                onSignalInfo((AsyncResult) msg.obj);
                break;

            case DISPLAYINFO_NOTIFICATION_DONE:
                if (DBG) log("Received Display Info notification done event ...");
                CdmaDisplayInfo.dismissDisplayInfoRecord();
                break;

            case CallStateMonitor.EVENT_OTA_PROVISION_CHANGE:
                if (DBG) log("EVENT_OTA_PROVISION_CHANGE...");
                mApplication.handleOtaspEvent(msg);
                break;

            case CallStateMonitor.PHONE_ENHANCED_VP_ON:
                if (DBG) log("PHONE_ENHANCED_VP_ON...");
                if (!mVoicePrivacyState) {
                    int toneToPlay = InCallTonePlayer.TONE_VOICE_PRIVACY;
                    new InCallTonePlayer(toneToPlay).start();
                    mVoicePrivacyState = true;
                }
                break;

            case CallStateMonitor.PHONE_ENHANCED_VP_OFF:
                if (DBG) log("PHONE_ENHANCED_VP_OFF...");
                if (mVoicePrivacyState) {
                    int toneToPlay = InCallTonePlayer.TONE_VOICE_PRIVACY;
                    new InCallTonePlayer(toneToPlay).start();
                    mVoicePrivacyState = false;
                }
                break;

            case CallStateMonitor.PHONE_RINGBACK_TONE:
                onRingbackTone((AsyncResult) msg.obj);
                break;

            case CallStateMonitor.PHONE_RESEND_MUTE:
                onResendMute();
                break;

            default:
                // super.handleMessage(msg);
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            onMwiChanged(mwi);
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            onCfiChanged(cfi);
        }
    };

    /**
     * Handles a "new ringing connection" event from the telephony layer.
     */
    private void onNewRingingConnection(AsyncResult r) {
        Connection c = (Connection) r.result;
        log("onNewRingingConnection(): state = " + mCM.getState() + ", conn = { " + c + " }");
        Call ringing = c.getCall();
        Phone phone = ringing.getPhone();

        // Check for a few cases where we totally ignore incoming calls.
        if (ignoreAllIncomingCalls(phone)) {
            // Immediately reject the call, without even indicating to the user
            // that an incoming call occurred.  (This will generally send the
            // caller straight to voicemail, just as if we *had* shown the
            // incoming-call UI and the user had declined the call.)
            PhoneUtils.hangupRingingCall(ringing);
            return;
        }

        if (!c.isRinging()) {
            Log.i(LOG_TAG, "CallNotifier.onNewRingingConnection(): connection not ringing!");
            // This is a very strange case: an incoming call that stopped
            // ringing almost instantly after the onNewRingingConnection()
            // event.  There's nothing we can do here, so just bail out
            // without doing anything.  (But presumably we'll log it in
            // the call log when the disconnect event comes in...)
            return;
        }

        // Stop any signalInfo tone being played on receiving a Call
        stopSignalInfoTone();

        Call.State state = c.getState();
        // State will be either INCOMING or WAITING.
        if (VDBG) log("- connection is ringing!  state = " + state);
        // if (DBG) PhoneUtils.dumpCallState(mPhone);

        // No need to do any service state checks here (like for
        // "emergency mode"), since in those states the SIM won't let
        // us get incoming connections in the first place.

        // TODO: Consider sending out a serialized broadcast Intent here
        // (maybe "ACTION_NEW_INCOMING_CALL"), *before* starting the
        // ringer and going to the in-call UI.  The intent should contain
        // the caller-id info for the current connection, and say whether
        // it would be a "call waiting" call or a regular ringing call.
        // If anybody consumed the broadcast, we'd bail out without
        // ringing or bringing up the in-call UI.
        //
        // This would give 3rd party apps a chance to listen for (and
        // intercept) new ringing connections.  An app could reject the
        // incoming call by consuming the broadcast and doing nothing, or
        // it could "pick up" the call (without any action by the user!)
        // via some future TelephonyManager API.
        //
        // See bug 1312336 for more details.
        // We'd need to protect this with a new "intercept incoming calls"
        // system permission.

        // Obtain a partial wake lock to make sure the CPU doesn't go to
        // sleep before we finish bringing up the InCallScreen.
        // (This will be upgraded soon to a full wake lock; see
        // showIncomingCall().)
        if (VDBG) log("Holding wake lock on new incoming connection.");
        mApplication.requestWakeState(PhoneGlobals.WakeState.PARTIAL);

        // - don't ring for call waiting connections
        // - do this before showing the incoming call panel
        startIncomingCallQuery(c);



        // Note we *don't* post a status bar notification here, since
        // we're not necessarily ready to actually show the incoming call
        // to the user.  (For calls in the INCOMING state, at least, we
        // still need to run a caller-id query, and we may not even ring
        // at all if the "send directly to voicemail" flag is set.)
        //
        // Instead, we update the notification (and potentially launch the
        // InCallScreen) from the showIncomingCall() method, which runs
        // when the caller-id query completes or times out.

        if (VDBG) log("- onNewRingingConnection() done.");
    }

    /**
     * Determines whether or not we're allowed to present incoming calls to the
     * user, based on the capabilities and/or current state of the device.
     *
     * If this method returns true, that means we should immediately reject the
     * current incoming call, without even indicating to the user that an
     * incoming call occurred.
     *
     * (We only reject incoming calls in a few cases, like during an OTASP call
     * when we can't interrupt the user, or if the device hasn't completed the
     * SetupWizard yet.  We also don't allow incoming calls on non-voice-capable
     * devices.  But note that we *always* allow incoming calls while in ECM.)
     *
     * @return true if we're *not* allowed to present an incoming call to
     * the user.
     */
    private boolean ignoreAllIncomingCalls(Phone phone) {
        // Incoming calls are totally ignored on non-voice-capable devices.
        if (!PhoneGlobals.sVoiceCapable) {
            // ...but still log a warning, since we shouldn't have gotten this
            // event in the first place!  (Incoming calls *should* be blocked at
            // the telephony layer on non-voice-capable capable devices.)
            Log.w(LOG_TAG, "Got onNewRingingConnection() on non-voice-capable device! Ignoring...");
            return true;
        }

        // In ECM (emergency callback mode), we ALWAYS allow incoming calls
        // to get through to the user.  (Note that ECM is applicable only to
        // voice-capable CDMA devices).
        if (PhoneUtils.isPhoneInEcm(phone)) {
            if (DBG) log("Incoming call while in ECM: always allow...");
            return false;
        }

        // Incoming calls are totally ignored if the device isn't provisioned yet.
        boolean provisioned = Settings.Global.getInt(mApplication.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        if (!provisioned) {
            Log.i(LOG_TAG, "Ignoring incoming call: not provisioned");
            return true;
        }

        // Incoming calls are totally ignored if an OTASP call is active.
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            boolean activateState = (mApplication.cdmaOtaScreenState.otaScreenState
                    == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION);
            boolean dialogState = (mApplication.cdmaOtaScreenState.otaScreenState
                    == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG);
            boolean spcState = mApplication.cdmaOtaProvisionData.inOtaSpcState;

            if (spcState) {
                Log.i(LOG_TAG, "Ignoring incoming call: OTA call is active");
                return true;
            } else if (activateState || dialogState) {
                // We *are* allowed to receive incoming calls at this point.
                // But clear out any residual OTASP UI first.
                // TODO: It's an MVC violation to twiddle the OTA UI state here;
                // we should instead provide a higher-level API via OtaUtils.
                if (dialogState) mApplication.dismissOtaDialogs();
                mApplication.clearOtaState();
                return false;
            }
        }

        // Normal case: allow this call to be presented to the user.
        return false;
    }

    /**
     * Helper method to manage the start of incoming call queries
     */
    private void startIncomingCallQuery(Connection c) {
        // TODO: cache the custom ringer object so that subsequent
        // calls will not need to do this query work.  We can keep
        // the MRU ringtones in memory.  We'll still need to hit
        // the database to get the callerinfo to act as a key,
        // but at least we can save the time required for the
        // Media player setup.  The only issue with this is that
        // we may need to keep an eye on the resources the Media
        // player uses to keep these ringtones around.

        // make sure we're in a state where we can be ready to
        // query a ringtone uri.
        boolean shouldStartQuery = false;
        synchronized (mCallerInfoQueryStateGuard) {
            if (mCallerInfoQueryState == CALLERINFO_QUERY_READY) {
                mCallerInfoQueryState = CALLERINFO_QUERYING;
                shouldStartQuery = true;
            }
        }
        if (shouldStartQuery) {
            // Reset the ringtone to the default first.
            mRinger.setCustomRingtoneUri(Settings.System.DEFAULT_RINGTONE_URI);

            // query the callerinfo to try to get the ringer.
            PhoneUtils.CallerInfoToken cit = PhoneUtils.startGetCallerInfo(
                    mApplication, c, this, c);

            // if this has already been queried then just ring, otherwise
            // we wait for the alloted time before ringing.
            if (cit.isFinal) {
                if (VDBG) log("- CallerInfo already up to date, using available data");
                onQueryComplete(0, c, cit.currentInfo);
            } else {
                if (VDBG) log("- Starting query, posting timeout message.");

                // Phone number (via getAddress()) is stored in the message to remember which
                // number is actually used for the look up.
                sendMessageDelayed(
                        Message.obtain(this, RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT, c),
                        RINGTONE_QUERY_WAIT_TIME);
            }
            // The call to showIncomingCall() will happen after the
            // queries are complete (or time out).
        } else {
            // This should never happen; its the case where an incoming call
            // arrives at the same time that the query is still being run,
            // and before the timeout window has closed.
            EventLog.writeEvent(EventLogTags.PHONE_UI_MULTIPLE_QUERY);

            ringAndNotifyOfIncomingCall(c);
        }
    }

    /**
     * Performs the final steps of the onNewRingingConnection sequence:
     * starts the ringer, and brings up the "incoming call" UI.
     *
     * Normally, this is called when the CallerInfo query completes (see
     * onQueryComplete()).  In this case, onQueryComplete() has already
     * configured the Ringer object to use the custom ringtone (if there
     * is one) for this caller.  So we just tell the Ringer to start, and
     * proceed to the InCallScreen.
     *
     * But this method can *also* be called if the
     * RINGTONE_QUERY_WAIT_TIME timeout expires, which means that the
     * CallerInfo query is taking too long.  In that case, we log a
     * warning but otherwise we behave the same as in the normal case.
     * (We still tell the Ringer to start, but it's going to use the
     * default ringtone.)
     */
    private void onCustomRingQueryComplete(Connection c) {
        boolean isQueryExecutionTimeExpired = false;
        synchronized (mCallerInfoQueryStateGuard) {
            if (mCallerInfoQueryState == CALLERINFO_QUERYING) {
                mCallerInfoQueryState = CALLERINFO_QUERY_READY;
                isQueryExecutionTimeExpired = true;
            }
        }
        if (isQueryExecutionTimeExpired) {
            // There may be a problem with the query here, since the
            // default ringtone is playing instead of the custom one.
            Log.w(LOG_TAG, "CallerInfo query took too long; falling back to default ringtone");
            EventLog.writeEvent(EventLogTags.PHONE_UI_RINGER_QUERY_ELAPSED);
        }

        // Make sure we still have an incoming call!
        //
        // (It's possible for the incoming call to have been disconnected
        // while we were running the query.  In that case we better not
        // start the ringer here, since there won't be any future
        // DISCONNECT event to stop it!)
        //
        // Note we don't have to worry about the incoming call going away
        // *after* this check but before we call mRinger.ring() below,
        // since in that case we *will* still get a DISCONNECT message sent
        // to our handler.  (And we will correctly stop the ringer when we
        // process that event.)
        if (mCM.getState() != PhoneConstants.State.RINGING) {
            Log.i(LOG_TAG, "onCustomRingQueryComplete: No incoming call! Bailing out...");
            // Don't start the ringer *or* bring up the "incoming call" UI.
            // Just bail out.
            return;
        }

        // If the ringing call still does not have any connection anymore, do not send the
        // notification to the CallModeler.
        final Call ringingCall = mCM.getFirstActiveRingingCall();

        if (ringingCall != null && ringingCall.getLatestConnection() == c) {
            ringAndNotifyOfIncomingCall(c);
        }
    }

    private void onUnknownConnectionAppeared(AsyncResult r) {
        PhoneConstants.State state = mCM.getState();

        if (state == PhoneConstants.State.OFFHOOK) {
            if (DBG) log("unknown connection appeared...");

            onPhoneStateChanged(r);
        }
    }

    /**
     * Notifies the Call Modeler that there is a new ringing connection.
     * If it is not a waiting call (there is no other active call in foreground), we will ring the
     * ringtone. Otherwise we will play the call waiting tone instead.
     * @param c The new ringing connection.
     */
    private void ringAndNotifyOfIncomingCall(Connection c) {
        if (PhoneUtils.isRealIncomingCall(c.getState())) {
            mRinger.ring();
        } else {
            if (VDBG) log("- starting call waiting tone...");
            if (mCallWaitingTonePlayer == null) {
                mCallWaitingTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_CALL_WAITING);
                mCallWaitingTonePlayer.start();
            }
        }
        mCallModeler.onNewRingingConnection(c);
    }

    /**
     * Updates the phone UI in response to phone state changes.
     *
     * Watch out: certain state changes are actually handled by their own
     * specific methods:
     *   - see onNewRingingConnection() for new incoming calls
     *   - see onDisconnect() for calls being hung up or disconnected
     */
    private void onPhoneStateChanged(AsyncResult r) {
        PhoneConstants.State state = mCM.getState();
        if (VDBG) log("onPhoneStateChanged: state = " + state);

        // Turn status bar notifications on or off depending upon the state
        // of the phone.  Notification Alerts (audible or vibrating) should
        // be on if and only if the phone is IDLE.
        mApplication.notificationMgr.statusBarHelper
                .enableNotificationAlerts(state == PhoneConstants.State.IDLE);

        Phone fgPhone = mCM.getFgPhone();
        if (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if ((fgPhone.getForegroundCall().getState() == Call.State.ACTIVE)
                    && ((mPreviousCdmaCallState == Call.State.DIALING)
                    ||  (mPreviousCdmaCallState == Call.State.ALERTING))) {
                if (mIsCdmaRedialCall) {
                    int toneToPlay = InCallTonePlayer.TONE_REDIAL;
                    new InCallTonePlayer(toneToPlay).start();
                }
                // Stop any signal info tone when call moves to ACTIVE state
                stopSignalInfoTone();
            }
            mPreviousCdmaCallState = fgPhone.getForegroundCall().getState();
        }

        // Have the PhoneApp recompute its mShowBluetoothIndication
        // flag based on the (new) telephony state.
        // There's no need to force a UI update since we update the
        // in-call notification ourselves (below), and the InCallScreen
        // listens for phone state changes itself.
        // TODO: Have BluetoothManager listen to CallModeler instead of relying on
        // CallNotifier
        mBluetoothManager.updateBluetoothIndication();


        // Update the phone state and other sensor/lock.
        mApplication.updatePhoneState(state);

        if (state == PhoneConstants.State.OFFHOOK) {
            // stop call waiting tone if needed when answering
            if (mCallWaitingTonePlayer != null) {
                mCallWaitingTonePlayer.stopTone();
                mCallWaitingTonePlayer = null;
            }

            if (VDBG) log("onPhoneStateChanged: OFF HOOK");
            // make sure audio is in in-call mode now
            PhoneUtils.setAudioMode(mCM);

            // Since we're now in-call, the Ringer should definitely *not*
            // be ringing any more.  (This is just a sanity-check; we
            // already stopped the ringer explicitly back in
            // PhoneUtils.answerCall(), before the call to phone.acceptCall().)
            // TODO: Confirm that this call really *is* unnecessary, and if so,
            // remove it!
            if (DBG) log("stopRing()... (OFFHOOK state)");
            mRinger.stopRing();
        }

        if (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            Connection c = fgPhone.getForegroundCall().getLatestConnection();
            if ((c != null) && (PhoneNumberUtils.isLocalEmergencyNumber(c.getAddress(),
                                                                        mApplication))) {
                if (VDBG) log("onPhoneStateChanged: it is an emergency call.");
                Call.State callState = fgPhone.getForegroundCall().getState();
                if (mEmergencyTonePlayerVibrator == null) {
                    mEmergencyTonePlayerVibrator = new EmergencyTonePlayerVibrator();
                }

                if (callState == Call.State.DIALING || callState == Call.State.ALERTING) {
                    mIsEmergencyToneOn = Settings.Global.getInt(
                            mApplication.getContentResolver(),
                            Settings.Global.EMERGENCY_TONE, EMERGENCY_TONE_OFF);
                    if (mIsEmergencyToneOn != EMERGENCY_TONE_OFF &&
                        mCurrentEmergencyToneState == EMERGENCY_TONE_OFF) {
                        if (mEmergencyTonePlayerVibrator != null) {
                            mEmergencyTonePlayerVibrator.start();
                        }
                    }
                } else if (callState == Call.State.ACTIVE) {
                    if (mCurrentEmergencyToneState != EMERGENCY_TONE_OFF) {
                        if (mEmergencyTonePlayerVibrator != null) {
                            mEmergencyTonePlayerVibrator.stop();
                        }
                    }
                }
            }
        }

        if ((fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)
                || (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)) {
            Call.State callState = mCM.getActiveFgCallState();
            if (!callState.isDialing()) {
                // If call get activated or disconnected before the ringback
                // tone stops, we have to stop it to prevent disturbing.
                if (mInCallRingbackTonePlayer != null) {
                    mInCallRingbackTonePlayer.stopTone();
                    mInCallRingbackTonePlayer = null;
                }
            }
        }
    }

    void updateCallNotifierRegistrationsAfterRadioTechnologyChange() {
        if (DBG) Log.d(LOG_TAG, "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");

        // Clear ringback tone player
        mInCallRingbackTonePlayer = null;

        // Clear call waiting tone player
        mCallWaitingTonePlayer = null;

        // Instantiate mSignalInfoToneGenerator
        createSignalInfoToneGenerator();
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.  If called with this
     * class itself, it is assumed that we have been waiting for the ringtone
     * and direct to voicemail settings to update.
     */
    @Override
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (cookie instanceof Long) {
            if (VDBG) log("CallerInfo query complete, posting missed call notification");

            mApplication.notificationMgr.notifyMissedCall(ci.name, ci.phoneNumber,
                    ci.numberPresentation, ci.phoneLabel, ci.cachedPhoto, ci.cachedPhotoIcon,
                    ((Long) cookie).longValue());
        } else if (cookie instanceof Connection) {
            final Connection c = (Connection) cookie;
            if (VDBG) log("CallerInfo query complete (for CallNotifier), "
                    + "updating state for incoming call..");

            // get rid of the timeout messages
            removeMessages(RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT);

            boolean isQueryExecutionTimeOK = false;
            synchronized (mCallerInfoQueryStateGuard) {
                if (mCallerInfoQueryState == CALLERINFO_QUERYING) {
                    mCallerInfoQueryState = CALLERINFO_QUERY_READY;
                    isQueryExecutionTimeOK = true;
                }
            }
            //if we're in the right state
            if (isQueryExecutionTimeOK) {

                // send directly to voicemail.
                if (ci.shouldSendToVoicemail) {
                    if (DBG) log("send to voicemail flag detected. hanging up.");
                    final Call ringingCall = mCM.getFirstActiveRingingCall();
                    if (ringingCall != null && ringingCall.getLatestConnection() == c) {
                        PhoneUtils.hangupRingingCall(ringingCall);
                        return;
                    }
                }

                // set the ringtone uri to prepare for the ring.
                if (ci.contactRingtoneUri != null) {
                    if (DBG) log("custom ringtone found, setting up ringer.");
                    Ringer r = mRinger;
                    r.setCustomRingtoneUri(ci.contactRingtoneUri);
                }
                // ring, and other post-ring actions.
                onCustomRingQueryComplete(c);
            }
        }
    }

    /**
     * Called when asynchronous CallerInfo query is taking too long (more than
     * {@link #RINGTONE_QUERY_WAIT_TIME} msec), but we cannot wait any more.
     *
     * This looks up in-memory fallback cache and use it when available. If not, it just calls
     * {@link #onCustomRingQueryComplete()} with default ringtone ("Send to voicemail" flag will
     * be just ignored).
     *
     * @param number The phone number used for the async query. This method will take care of
     * formatting or normalization of the number.
     */
    private void onCustomRingtoneQueryTimeout(Connection c) {
        // First of all, this case itself should be rare enough, though we cannot avoid it in
        // some situations (e.g. IPC is slow due to system overload, database is in sync, etc.)
        Log.w(LOG_TAG, "CallerInfo query took too long; look up local fallback cache.");

        // This method is intentionally verbose for now to detect possible bad side-effect for it.
        // TODO: Remove the verbose log when it looks stable and reliable enough.


        if (c != null) {
            final CallerInfoCache.CacheEntry entry =
                    mApplication.callerInfoCache.getCacheEntry(c.getAddress());
            if (entry != null) {
                if (entry.sendToVoicemail) {
                    log("send to voicemail flag detected (in fallback cache). hanging up.");
                    if (mCM.getFirstActiveRingingCall().getLatestConnection() == c) {
                        PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
                        return;
                    }
                }

                if (entry.customRingtone != null) {
                    log("custom ringtone found (in fallback cache), setting up ringer: "
                            + entry.customRingtone);
                    this.mRinger.setCustomRingtoneUri(Uri.parse(entry.customRingtone));
                }
            } else {
                // In this case we call onCustomRingQueryComplete(), just
                // like if the query had completed normally.  (But we're
                // going to get the default ringtone, since we never got
                // the chance to call Ringer.setCustomRingtoneUri()).
                log("Failed to find fallback cache. Use default ringer tone.");
            }
        }

        onCustomRingQueryComplete(c);
    }

    private void onDisconnect(AsyncResult r) {
        if (VDBG) log("onDisconnect()...  CallManager state: " + mCM.getState());

        mVoicePrivacyState = false;
        Connection c = (Connection) r.result;
        if (c != null) {
            log("onDisconnect: cause = " + c.getDisconnectCause()
                  + ", incoming = " + c.isIncoming()
                  + ", date = " + c.getCreateTime());
        } else {
            Log.w(LOG_TAG, "onDisconnect: null connection");
        }

        int autoretrySetting = 0;
        if ((c != null) && (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA)) {
            autoretrySetting = android.provider.Settings.Global.getInt(mApplication.
                    getContentResolver(),android.provider.Settings.Global.CALL_AUTO_RETRY, 0);
        }

        // Stop any signalInfo tone being played when a call gets ended
        stopSignalInfoTone();

        if ((c != null) && (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA)) {
            // Resetting the CdmaPhoneCallState members
            mApplication.cdmaPhoneCallState.resetCdmaPhoneCallState();

            // Remove Call waiting timers
            removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
            removeMessages(CALLWAITING_ADDCALL_DISABLE_TIMEOUT);
        }

        // Stop the ringer if it was ringing (for an incoming call that
        // either disconnected by itself, or was rejected by the user.)
        //
        // TODO: We technically *shouldn't* stop the ringer if the
        // foreground or background call disconnects while an incoming call
        // is still ringing, but that's a really rare corner case.
        // It's safest to just unconditionally stop the ringer here.

        // CDMA: For Call collision cases i.e. when the user makes an out going call
        // and at the same time receives an Incoming Call, the Incoming Call is given
        // higher preference. At this time framework sends a disconnect for the Out going
        // call connection hence we should *not* be stopping the ringer being played for
        // the Incoming Call
        Call ringingCall = mCM.getFirstActiveRingingCall();
        if (ringingCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if (PhoneUtils.isRealIncomingCall(ringingCall.getState())) {
                // Also we need to take off the "In Call" icon from the Notification
                // area as the Out going Call never got connected
                if (DBG) log("cancelCallInProgressNotifications()... (onDisconnect)");
                mApplication.notificationMgr.cancelCallInProgressNotifications();
            } else {
                if (DBG) log("stopRing()... (onDisconnect)");
                mRinger.stopRing();
            }
        } else { // GSM
            if (DBG) log("stopRing()... (onDisconnect)");
            mRinger.stopRing();
        }

        // stop call waiting tone if needed when disconnecting
        if (mCallWaitingTonePlayer != null) {
            mCallWaitingTonePlayer.stopTone();
            mCallWaitingTonePlayer = null;
        }

        // If this is the end of an OTASP call, pass it on to the PhoneApp.
        if (c != null && TelephonyCapabilities.supportsOtasp(c.getCall().getPhone())) {
            final String number = c.getAddress();
            if (c.getCall().getPhone().isOtaSpNumber(number)) {
                if (DBG) log("onDisconnect: this was an OTASP call!");
                mApplication.handleOtaspDisconnect();
            }
        }

        // Check for the various tones we might need to play (thru the
        // earpiece) after a call disconnects.
        int toneToPlay = InCallTonePlayer.TONE_NONE;

        // The "Busy" or "Congestion" tone is the highest priority:
        if (c != null) {
            Connection.DisconnectCause cause = c.getDisconnectCause();
            if (cause == Connection.DisconnectCause.BUSY) {
                if (DBG) log("- need to play BUSY tone!");
                toneToPlay = InCallTonePlayer.TONE_BUSY;
            } else if (cause == Connection.DisconnectCause.CONGESTION) {
                if (DBG) log("- need to play CONGESTION tone!");
                toneToPlay = InCallTonePlayer.TONE_CONGESTION;
            } else if (((cause == Connection.DisconnectCause.NORMAL)
                    || (cause == Connection.DisconnectCause.LOCAL))
                    && (mApplication.isOtaCallInActiveState())) {
                if (DBG) log("- need to play OTA_CALL_END tone!");
                toneToPlay = InCallTonePlayer.TONE_OTA_CALL_END;
            } else if (cause == Connection.DisconnectCause.CDMA_REORDER) {
                if (DBG) log("- need to play CDMA_REORDER tone!");
                toneToPlay = InCallTonePlayer.TONE_REORDER;
            } else if (cause == Connection.DisconnectCause.CDMA_INTERCEPT) {
                if (DBG) log("- need to play CDMA_INTERCEPT tone!");
                toneToPlay = InCallTonePlayer.TONE_INTERCEPT;
            } else if (cause == Connection.DisconnectCause.CDMA_DROP) {
                if (DBG) log("- need to play CDMA_DROP tone!");
                toneToPlay = InCallTonePlayer.TONE_CDMA_DROP;
            } else if (cause == Connection.DisconnectCause.OUT_OF_SERVICE) {
                if (DBG) log("- need to play OUT OF SERVICE tone!");
                toneToPlay = InCallTonePlayer.TONE_OUT_OF_SERVICE;
            } else if (cause == Connection.DisconnectCause.UNOBTAINABLE_NUMBER) {
                if (DBG) log("- need to play TONE_UNOBTAINABLE_NUMBER tone!");
                toneToPlay = InCallTonePlayer.TONE_UNOBTAINABLE_NUMBER;
            } else if (cause == Connection.DisconnectCause.ERROR_UNSPECIFIED) {
                if (DBG) log("- DisconnectCause is ERROR_UNSPECIFIED: play TONE_CALL_ENDED!");
                toneToPlay = InCallTonePlayer.TONE_CALL_ENDED;
            }
        }

        // If we don't need to play BUSY or CONGESTION, then play the
        // "call ended" tone if this was a "regular disconnect" (i.e. a
        // normal call where one end or the other hung up) *and* this
        // disconnect event caused the phone to become idle.  (In other
        // words, we *don't* play the sound if one call hangs up but
        // there's still an active call on the other line.)
        // TODO: We may eventually want to disable this via a preference.
        if ((toneToPlay == InCallTonePlayer.TONE_NONE)
            && (mCM.getState() == PhoneConstants.State.IDLE)
            && (c != null)) {
            Connection.DisconnectCause cause = c.getDisconnectCause();
            if ((cause == Connection.DisconnectCause.NORMAL)  // remote hangup
                || (cause == Connection.DisconnectCause.LOCAL)) {  // local hangup
                if (VDBG) log("- need to play CALL_ENDED tone!");
                toneToPlay = InCallTonePlayer.TONE_CALL_ENDED;
                mIsCdmaRedialCall = false;
            }
        }

        // All phone calls are disconnected.
        if (mCM.getState() == PhoneConstants.State.IDLE) {
            // Don't reset the audio mode or bluetooth/speakerphone state
            // if we still need to let the user hear a tone through the earpiece.
            if (toneToPlay == InCallTonePlayer.TONE_NONE) {
                resetAudioStateAfterDisconnect();
            }

            mApplication.notificationMgr.cancelCallInProgressNotifications();
        }

        if (c != null) {
            mCallLogger.logCall(c);

            final String number = c.getAddress();
            final Phone phone = c.getCall().getPhone();
            final boolean isEmergencyNumber =
                    PhoneNumberUtils.isLocalEmergencyNumber(number, mApplication);

            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                if ((isEmergencyNumber)
                        && (mCurrentEmergencyToneState != EMERGENCY_TONE_OFF)) {
                    if (mEmergencyTonePlayerVibrator != null) {
                        mEmergencyTonePlayerVibrator.stop();
                    }
                }
            }

            final long date = c.getCreateTime();
            final Connection.DisconnectCause cause = c.getDisconnectCause();
            final boolean missedCall = c.isIncoming() &&
                    (cause == Connection.DisconnectCause.INCOMING_MISSED);
            if (missedCall) {
                // Show the "Missed call" notification.
                // (Note we *don't* do this if this was an incoming call that
                // the user deliberately rejected.)
                showMissedCallNotification(c, date);
            }

            // Possibly play a "post-disconnect tone" thru the earpiece.
            // We do this here, rather than from the InCallScreen
            // activity, since we need to do this even if you're not in
            // the Phone UI at the moment the connection ends.
            if (toneToPlay != InCallTonePlayer.TONE_NONE) {
                if (VDBG) log("- starting post-disconnect tone (" + toneToPlay + ")...");
                new InCallTonePlayer(toneToPlay).start();

                // TODO: alternatively, we could start an InCallTonePlayer
                // here with an "unlimited" tone length,
                // and manually stop it later when this connection truly goes
                // away.  (The real connection over the network was closed as soon
                // as we got the BUSY message.  But our telephony layer keeps the
                // connection open for a few extra seconds so we can show the
                // "busy" indication to the user.  We could stop the busy tone
                // when *that* connection's "disconnect" event comes in.)
            }

            if (((mPreviousCdmaCallState == Call.State.DIALING)
                    || (mPreviousCdmaCallState == Call.State.ALERTING))
                    && (!isEmergencyNumber)
                    && (cause != Connection.DisconnectCause.INCOMING_MISSED )
                    && (cause != Connection.DisconnectCause.NORMAL)
                    && (cause != Connection.DisconnectCause.LOCAL)
                    && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {
                if (!mIsCdmaRedialCall) {
                    if (autoretrySetting == InCallScreen.AUTO_RETRY_ON) {
                        // TODO: (Moto): The contact reference data may need to be stored and use
                        // here when redialing a call. For now, pass in NULL as the URI parameter.
                        final int status =
                                PhoneUtils.placeCall(mApplication, phone, number, null, false);
                        if (status != PhoneUtils.CALL_STATUS_FAILED) {
                            mIsCdmaRedialCall = true;
                        }
                    } else {
                        mIsCdmaRedialCall = false;
                    }
                } else {
                    mIsCdmaRedialCall = false;
                }
            }
        }
    }

    /**
     * Resets the audio mode and speaker state when a call ends.
     */
    private void resetAudioStateAfterDisconnect() {
        if (VDBG) log("resetAudioStateAfterDisconnect()...");

        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }

        // call turnOnSpeaker() with state=false and store=true even if speaker
        // is already off to reset user requested speaker state.
        PhoneUtils.turnOnSpeaker(mApplication, false, true);

        PhoneUtils.setAudioMode(mCM);
    }

    private void onMwiChanged(boolean visible) {
        if (VDBG) log("onMwiChanged(): " + visible);

        // "Voicemail" is meaningless on non-voice-capable devices,
        // so ignore MWI events.
        if (!PhoneGlobals.sVoiceCapable) {
            // ...but still log a warning, since we shouldn't have gotten this
            // event in the first place!
            // (PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR events
            // *should* be blocked at the telephony layer on non-voice-capable
            // capable devices.)
            Log.w(LOG_TAG, "Got onMwiChanged() on non-voice-capable device! Ignoring...");
            return;
        }

        mApplication.notificationMgr.updateMwi(visible);
    }

    /**
     * Posts a delayed PHONE_MWI_CHANGED event, to schedule a "retry" for a
     * failed NotificationMgr.updateMwi() call.
     */
    /* package */ void sendMwiChangedDelayed(long delayMillis) {
        Message message = Message.obtain(this, PHONE_MWI_CHANGED);
        sendMessageDelayed(message, delayMillis);
    }

    private void onCfiChanged(boolean visible) {
        if (VDBG) log("onCfiChanged(): " + visible);
        mApplication.notificationMgr.updateCfi(visible);
    }

    /**
     * Indicates whether or not this ringer is ringing.
     */
    boolean isRinging() {
        return mRinger.isRinging();
    }

    /**
     * Stops the current ring, and tells the notifier that future
     * ring requests should be ignored.
     */
    void silenceRinger() {
        mSilentRingerRequested = true;
        if (DBG) log("stopRing()... (silenceRinger)");
        mRinger.stopRing();
    }

    /**
     * Restarts the ringer after having previously silenced it.
     *
     * (This is a no-op if the ringer is actually still ringing, or if the
     * incoming ringing call no longer exists.)
     */
    /* package */ void restartRinger() {
        if (DBG) log("restartRinger()...");
        // Already ringing or Silent requested; no need to restart.
        if (isRinging() || mSilentRingerRequested) return;

        final Call ringingCall = mCM.getFirstActiveRingingCall();
        // Don't check ringingCall.isRinging() here, since that'll be true
        // for the WAITING state also.  We only allow the ringer for
        // regular INCOMING calls.
        if (DBG) log("- ringingCall state: " + ringingCall.getState());
        if (ringingCall.getState() == Call.State.INCOMING) {
            mRinger.ring();
        }
    }

    /**
     * Helper class to play tones through the earpiece (or speaker / BT)
     * during a call, using the ToneGenerator.
     *
     * To use, just instantiate a new InCallTonePlayer
     * (passing in the TONE_* constant for the tone you want)
     * and start() it.
     *
     * When we're done playing the tone, if the phone is idle at that
     * point, we'll reset the audio routing and speaker state.
     * (That means that for tones that get played *after* a call
     * disconnects, like "busy" or "congestion" or "call ended", you
     * should NOT call resetAudioStateAfterDisconnect() yourself.
     * Instead, just start the InCallTonePlayer, which will automatically
     * defer the resetAudioStateAfterDisconnect() call until the tone
     * finishes playing.)
     */
    private class InCallTonePlayer extends Thread {
        private int mToneId;
        private int mState;
        // The possible tones we can play.
        public static final int TONE_NONE = 0;
        public static final int TONE_CALL_WAITING = 1;
        public static final int TONE_BUSY = 2;
        public static final int TONE_CONGESTION = 3;
        public static final int TONE_CALL_ENDED = 4;
        public static final int TONE_VOICE_PRIVACY = 5;
        public static final int TONE_REORDER = 6;
        public static final int TONE_INTERCEPT = 7;
        public static final int TONE_CDMA_DROP = 8;
        public static final int TONE_OUT_OF_SERVICE = 9;
        public static final int TONE_REDIAL = 10;
        public static final int TONE_OTA_CALL_END = 11;
        public static final int TONE_RING_BACK = 12;
        public static final int TONE_UNOBTAINABLE_NUMBER = 13;

        // The tone volume relative to other sounds in the stream
        static final int TONE_RELATIVE_VOLUME_EMERGENCY = 100;
        static final int TONE_RELATIVE_VOLUME_HIPRI = 80;
        static final int TONE_RELATIVE_VOLUME_LOPRI = 50;

        // Buffer time (in msec) to add on to tone timeout value.
        // Needed mainly when the timeout value for a tone is the
        // exact duration of the tone itself.
        static final int TONE_TIMEOUT_BUFFER = 20;

        // The tone state
        static final int TONE_OFF = 0;
        static final int TONE_ON = 1;
        static final int TONE_STOPPED = 2;

        InCallTonePlayer(int toneId) {
            super();
            mToneId = toneId;
            mState = TONE_OFF;
        }

        @Override
        public void run() {
            log("InCallTonePlayer.run(toneId = " + mToneId + ")...");

            int toneType = 0;  // passed to ToneGenerator.startTone()
            int toneVolume;  // passed to the ToneGenerator constructor
            int toneLengthMillis;
            int phoneType = mCM.getFgPhone().getPhoneType();

            switch (mToneId) {
                case TONE_CALL_WAITING:
                    toneType = ToneGenerator.TONE_SUP_CALL_WAITING;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    // Call waiting tone is stopped by stopTone() method
                    toneLengthMillis = Integer.MAX_VALUE - TONE_TIMEOUT_BUFFER;
                    break;
                case TONE_BUSY:
                    if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        toneType = ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT;
                        toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                        toneLengthMillis = 1000;
                    } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                            || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                        toneType = ToneGenerator.TONE_SUP_BUSY;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 4000;
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    break;
                case TONE_CONGESTION:
                    toneType = ToneGenerator.TONE_SUP_CONGESTION;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;

                case TONE_CALL_ENDED:
                    toneType = ToneGenerator.TONE_PROP_PROMPT;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 200;
                    break;
                 case TONE_OTA_CALL_END:
                    if (mApplication.cdmaOtaConfigData.otaPlaySuccessFailureTone ==
                            OtaUtils.OTA_PLAY_SUCCESS_FAILURE_TONE_ON) {
                        toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 750;
                    } else {
                        toneType = ToneGenerator.TONE_PROP_PROMPT;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 200;
                    }
                    break;
                case TONE_VOICE_PRIVACY:
                    toneType = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 5000;
                    break;
                case TONE_REORDER:
                    toneType = ToneGenerator.TONE_CDMA_REORDER;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_INTERCEPT:
                    toneType = ToneGenerator.TONE_CDMA_ABBR_INTERCEPT;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 500;
                    break;
                case TONE_CDMA_DROP:
                case TONE_OUT_OF_SERVICE:
                    toneType = ToneGenerator.TONE_CDMA_CALLDROP_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 375;
                    break;
                case TONE_REDIAL:
                    toneType = ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 5000;
                    break;
                case TONE_RING_BACK:
                    toneType = ToneGenerator.TONE_SUP_RINGTONE;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    // Call ring back tone is stopped by stopTone() method
                    toneLengthMillis = Integer.MAX_VALUE - TONE_TIMEOUT_BUFFER;
                    break;
                case TONE_UNOBTAINABLE_NUMBER:
                    toneType = ToneGenerator.TONE_SUP_ERROR;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                default:
                    throw new IllegalArgumentException("Bad toneId: " + mToneId);
            }

            // If the mToneGenerator creation fails, just continue without it.  It is
            // a local audio signal, and is not as important.
            ToneGenerator toneGenerator;
            try {
                int stream;
                if (mBluetoothHeadset != null) {
                    stream = mBluetoothHeadset.isAudioOn() ? AudioManager.STREAM_BLUETOOTH_SCO:
                        AudioManager.STREAM_VOICE_CALL;
                } else {
                    stream = AudioManager.STREAM_VOICE_CALL;
                }
                toneGenerator = new ToneGenerator(stream, toneVolume);
                // if (DBG) log("- created toneGenerator: " + toneGenerator);
            } catch (RuntimeException e) {
                Log.w(LOG_TAG,
                      "InCallTonePlayer: Exception caught while creating ToneGenerator: " + e);
                toneGenerator = null;
            }

            // Using the ToneGenerator (with the CALL_WAITING / BUSY /
            // CONGESTION tones at least), the ToneGenerator itself knows
            // the right pattern of tones to play; we do NOT need to
            // manually start/stop each individual tone, or manually
            // insert the correct delay between tones.  (We just start it
            // and let it run for however long we want the tone pattern to
            // continue.)
            //
            // TODO: When we stop the ToneGenerator in the middle of a
            // "tone pattern", it sounds bad if we cut if off while the
            // tone is actually playing.  Consider adding API to the
            // ToneGenerator to say "stop at the next silent part of the
            // pattern", or simply "play the pattern N times and then
            // stop."
            boolean needToStopTone = true;
            boolean okToPlayTone = false;

            if (toneGenerator != null) {
                int ringerMode = mAudioManager.getRingerMode();
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    if (toneType == ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD) {
                        if ((ringerMode != AudioManager.RINGER_MODE_SILENT) &&
                                (ringerMode != AudioManager.RINGER_MODE_VIBRATE)) {
                            if (DBG) log("- InCallTonePlayer: start playing call tone=" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if ((toneType == ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT) ||
                            (toneType == ToneGenerator.TONE_CDMA_REORDER) ||
                            (toneType == ToneGenerator.TONE_CDMA_ABBR_REORDER) ||
                            (toneType == ToneGenerator.TONE_CDMA_ABBR_INTERCEPT) ||
                            (toneType == ToneGenerator.TONE_CDMA_CALLDROP_LITE)) {
                        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
                            if (DBG) log("InCallTonePlayer:playing call fail tone:" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if ((toneType == ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE) ||
                               (toneType == ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE)) {
                        if ((ringerMode != AudioManager.RINGER_MODE_SILENT) &&
                                (ringerMode != AudioManager.RINGER_MODE_VIBRATE)) {
                            if (DBG) log("InCallTonePlayer:playing tone for toneType=" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else { // For the rest of the tones, always OK to play.
                        okToPlayTone = true;
                    }
                } else {  // Not "CDMA"
                    okToPlayTone = true;
                }

                synchronized (this) {
                    if (okToPlayTone && mState != TONE_STOPPED) {
                        mState = TONE_ON;
                        toneGenerator.startTone(toneType);
                        try {
                            wait(toneLengthMillis + TONE_TIMEOUT_BUFFER);
                        } catch  (InterruptedException e) {
                            Log.w(LOG_TAG,
                                  "InCallTonePlayer stopped: " + e);
                        }
                        if (needToStopTone) {
                            toneGenerator.stopTone();
                        }
                    }
                    // if (DBG) log("- InCallTonePlayer: done playing.");
                    toneGenerator.release();
                    mState = TONE_OFF;
                }
            }

            // Finally, do the same cleanup we otherwise would have done
            // in onDisconnect().
            //
            // (But watch out: do NOT do this if the phone is in use,
            // since some of our tones get played *during* a call (like
            // CALL_WAITING) and we definitely *don't*
            // want to reset the audio mode / speaker / bluetooth after
            // playing those!
            // This call is really here for use with tones that get played
            // *after* a call disconnects, like "busy" or "congestion" or
            // "call ended", where the phone has already become idle but
            // we need to defer the resetAudioStateAfterDisconnect() call
            // till the tone finishes playing.)
            if (mCM.getState() == PhoneConstants.State.IDLE) {
                resetAudioStateAfterDisconnect();
            }
        }

        public void stopTone() {
            synchronized (this) {
                if (mState == TONE_ON) {
                    notify();
                }
                mState = TONE_STOPPED;
            }
        }
    }

    /**
     * Displays a notification when the phone receives a DisplayInfo record.
     */
    private void onDisplayInfo(AsyncResult r) {
        // Extract the DisplayInfo String from the message
        CdmaDisplayInfoRec displayInfoRec = (CdmaDisplayInfoRec)(r.result);

        if (displayInfoRec != null) {
            String displayInfo = displayInfoRec.alpha;
            if (DBG) log("onDisplayInfo: displayInfo=" + displayInfo);
            CdmaDisplayInfo.displayInfoRecord(mApplication, displayInfo);

            // start a 2 second timer
            sendEmptyMessageDelayed(DISPLAYINFO_NOTIFICATION_DONE,
                    DISPLAYINFO_NOTIFICATION_TIME);
        }
    }

    /**
     * Helper class to play SignalInfo tones using the ToneGenerator.
     *
     * To use, just instantiate a new SignalInfoTonePlayer
     * (passing in the ToneID constant for the tone you want)
     * and start() it.
     */
    private class SignalInfoTonePlayer extends Thread {
        private int mToneId;

        SignalInfoTonePlayer(int toneId) {
            super();
            mToneId = toneId;
        }

        @Override
        public void run() {
            log("SignalInfoTonePlayer.run(toneId = " + mToneId + ")...");

            if (mSignalInfoToneGenerator != null) {
                //First stop any ongoing SignalInfo tone
                mSignalInfoToneGenerator.stopTone();

                //Start playing the new tone if its a valid tone
                mSignalInfoToneGenerator.startTone(mToneId);
            }
        }
    }

    /**
     * Plays a tone when the phone receives a SignalInfo record.
     */
    private void onSignalInfo(AsyncResult r) {
        // Signal Info are totally ignored on non-voice-capable devices.
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w(LOG_TAG, "Got onSignalInfo() on non-voice-capable device! Ignoring...");
            return;
        }

        if (PhoneUtils.isRealIncomingCall(mCM.getFirstActiveRingingCall().getState())) {
            // Do not start any new SignalInfo tone when Call state is INCOMING
            // and stop any previous SignalInfo tone which is being played
            stopSignalInfoTone();
        } else {
            // Extract the SignalInfo String from the message
            CdmaSignalInfoRec signalInfoRec = (CdmaSignalInfoRec)(r.result);
            // Only proceed if a Signal info is present.
            if (signalInfoRec != null) {
                boolean isPresent = signalInfoRec.isPresent;
                if (DBG) log("onSignalInfo: isPresent=" + isPresent);
                if (isPresent) {// if tone is valid
                    int uSignalType = signalInfoRec.signalType;
                    int uAlertPitch = signalInfoRec.alertPitch;
                    int uSignal = signalInfoRec.signal;

                    if (DBG) log("onSignalInfo: uSignalType=" + uSignalType + ", uAlertPitch=" +
                            uAlertPitch + ", uSignal=" + uSignal);
                    //Map the Signal to a ToneGenerator ToneID only if Signal info is present
                    int toneID = SignalToneUtil.getAudioToneFromSignalInfo
                            (uSignalType, uAlertPitch, uSignal);

                    //Create the SignalInfo tone player and pass the ToneID
                    new SignalInfoTonePlayer(toneID).start();
                }
            }
        }
    }

    /**
     * Stops a SignalInfo tone in the following condition
     * 1 - On receiving a New Ringing Call
     * 2 - On disconnecting a call
     * 3 - On answering a Call Waiting Call
     */
    /* package */ void stopSignalInfoTone() {
        if (DBG) log("stopSignalInfoTone: Stopping SignalInfo tone player");
        new SignalInfoTonePlayer(ToneGenerator.TONE_CDMA_SIGNAL_OFF).start();
    }

    /**
     * Plays a Call waiting tone if it is present in the second incoming call.
     */
    private void onCdmaCallWaiting(AsyncResult r) {
        // Remove any previous Call waiting timers in the queue
        removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
        removeMessages(CALLWAITING_ADDCALL_DISABLE_TIMEOUT);

        // Set the Phone Call State to SINGLE_ACTIVE as there is only one connection
        // else we would not have received Call waiting
        mApplication.cdmaPhoneCallState.setCurrentCallState(
                CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);

        // Start timer for CW display
        mCallWaitingTimeOut = false;
        sendEmptyMessageDelayed(CALLWAITING_CALLERINFO_DISPLAY_DONE,
                CALLWAITING_CALLERINFO_DISPLAY_TIME);

        // Set the mAddCallMenuStateAfterCW state to false
        mApplication.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(false);

        // Start the timer for disabling "Add Call" menu option
        sendEmptyMessageDelayed(CALLWAITING_ADDCALL_DISABLE_TIMEOUT,
                CALLWAITING_ADDCALL_DISABLE_TIME);

        // Extract the Call waiting information
        CdmaCallWaitingNotification infoCW = (CdmaCallWaitingNotification) r.result;
        int isPresent = infoCW.isPresent;
        if (DBG) log("onCdmaCallWaiting: isPresent=" + isPresent);
        if (isPresent == 1 ) {//'1' if tone is valid
            int uSignalType = infoCW.signalType;
            int uAlertPitch = infoCW.alertPitch;
            int uSignal = infoCW.signal;
            if (DBG) log("onCdmaCallWaiting: uSignalType=" + uSignalType + ", uAlertPitch="
                    + uAlertPitch + ", uSignal=" + uSignal);
            //Map the Signal to a ToneGenerator ToneID only if Signal info is present
            int toneID =
                SignalToneUtil.getAudioToneFromSignalInfo(uSignalType, uAlertPitch, uSignal);

            //Create the SignalInfo tone player and pass the ToneID
            new SignalInfoTonePlayer(toneID).start();
        }

        mCallModeler.onCdmaCallWaiting(infoCW);
    }

    /**
     * Posts a event causing us to clean up after rejecting (or timing-out) a
     * CDMA call-waiting call.
     *
     * This method is safe to call from any thread.
     * @see #onCdmaCallWaitingReject()
     */
    /* package */ void sendCdmaCallWaitingReject() {
        sendEmptyMessage(CDMA_CALL_WAITING_REJECT);
    }

    /**
     * Performs Call logging based on Timeout or Ignore Call Waiting Call for CDMA,
     * and finally calls Hangup on the Call Waiting connection.
     *
     * This method should be called only from the UI thread.
     * @see #sendCdmaCallWaitingReject()
     */
    private void onCdmaCallWaitingReject() {
        final Call ringingCall = mCM.getFirstActiveRingingCall();

        // Call waiting timeout scenario
        if (ringingCall.getState() == Call.State.WAITING) {
            // Code for perform Call logging and missed call notification
            Connection c = ringingCall.getLatestConnection();

            if (c != null) {
                final int callLogType = mCallWaitingTimeOut ?
                        Calls.MISSED_TYPE : Calls.INCOMING_TYPE;

                // TODO: This callLogType override is not ideal. Connection should be astracted away
                // at a telephony-phone layer that can understand and edit the callTypes within
                // the abstraction for CDMA devices.
                mCallLogger.logCall(c, callLogType);

                final long date = c.getCreateTime();
                if (callLogType == Calls.MISSED_TYPE) {
                    // Add missed call notification
                    showMissedCallNotification(c, date);
                } else {
                    // Remove Call waiting 20 second display timer in the queue
                    removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
                }

                // Hangup the RingingCall connection for CW
                PhoneUtils.hangup(c);
            }

            //Reset the mCallWaitingTimeOut boolean
            mCallWaitingTimeOut = false;
        }

        // Call modeler needs to know about this event regardless of the
        // state conditionals in the previous code.
        mCallModeler.onCdmaCallWaitingReject();
    }

    /**
     * Return the private variable mPreviousCdmaCallState.
     */
    /* package */ Call.State getPreviousCdmaCallState() {
        return mPreviousCdmaCallState;
    }

    /**
     * Return the private variable mVoicePrivacyState.
     */
    /* package */ boolean getVoicePrivacyState() {
        return mVoicePrivacyState;
    }

    /**
     * Return the private variable mIsCdmaRedialCall.
     */
    /* package */ boolean getIsCdmaRedialCall() {
        return mIsCdmaRedialCall;
    }

    /**
     * Helper function used to show a missed call notification.
     */
    private void showMissedCallNotification(Connection c, final long date) {
        PhoneUtils.CallerInfoToken info =
                PhoneUtils.startGetCallerInfo(mApplication, c, this, Long.valueOf(date));
        if (info != null) {
            // at this point, we've requested to start a query, but it makes no
            // sense to log this missed call until the query comes back.
            if (VDBG) log("showMissedCallNotification: Querying for CallerInfo on missed call...");
            if (info.isFinal) {
                // it seems that the query we have actually is up to date.
                // send the notification then.
                CallerInfo ci = info.currentInfo;

                // Check number presentation value; if we have a non-allowed presentation,
                // then display an appropriate presentation string instead as the missed
                // call.
                String name = ci.name;
                String number = ci.phoneNumber;
                if (ci.numberPresentation == PhoneConstants.PRESENTATION_RESTRICTED) {
                    name = mApplication.getString(R.string.private_num);
                } else if (ci.numberPresentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    name = mApplication.getString(R.string.unknown);
                } else {
                    number = PhoneUtils.modifyForSpecialCnapCases(mApplication,
                            ci, number, ci.numberPresentation);
                }
                mApplication.notificationMgr.notifyMissedCall(name, number, ci.numberPresentation,
                        ci.phoneLabel, ci.cachedPhoto, ci.cachedPhotoIcon, date);
            }
        } else {
            // getCallerInfo() can return null in rare cases, like if we weren't
            // able to get a valid phone number out of the specified Connection.
            Log.w(LOG_TAG, "showMissedCallNotification: got null CallerInfo for Connection " + c);
        }
    }

    /**
     *  Inner class to handle emergency call tone and vibrator
     */
    private class EmergencyTonePlayerVibrator {
        private final int EMG_VIBRATE_LENGTH = 1000;  // ms.
        private final int EMG_VIBRATE_PAUSE  = 1000;  // ms.
        private final long[] mVibratePattern =
                new long[] { EMG_VIBRATE_LENGTH, EMG_VIBRATE_PAUSE };

        private ToneGenerator mToneGenerator;
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this vibrator
        // object will be isolated from others.
        private Vibrator mEmgVibrator = new SystemVibrator();
        private int mInCallVolume;

        /**
         * constructor
         */
        public EmergencyTonePlayerVibrator() {
        }

        /**
         * Start the emergency tone or vibrator.
         */
        private void start() {
            if (VDBG) log("call startEmergencyToneOrVibrate.");
            int ringerMode = mAudioManager.getRingerMode();

            if ((mIsEmergencyToneOn == EMERGENCY_TONE_ALERT) &&
                    (ringerMode == AudioManager.RINGER_MODE_NORMAL)) {
                log("EmergencyTonePlayerVibrator.start(): emergency tone...");
                mToneGenerator = new ToneGenerator (AudioManager.STREAM_VOICE_CALL,
                        InCallTonePlayer.TONE_RELATIVE_VOLUME_EMERGENCY);
                if (mToneGenerator != null) {
                    mInCallVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                            mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                            0);
                    mToneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK);
                    mCurrentEmergencyToneState = EMERGENCY_TONE_ALERT;
                }
            } else if (mIsEmergencyToneOn == EMERGENCY_TONE_VIBRATE) {
                log("EmergencyTonePlayerVibrator.start(): emergency vibrate...");
                if (mEmgVibrator != null) {
                    mEmgVibrator.vibrate(mVibratePattern, 0);
                    mCurrentEmergencyToneState = EMERGENCY_TONE_VIBRATE;
                }
            }
        }

        /**
         * If the emergency tone is active, stop the tone or vibrator accordingly.
         */
        private void stop() {
            if (VDBG) log("call stopEmergencyToneOrVibrate.");

            if ((mCurrentEmergencyToneState == EMERGENCY_TONE_ALERT)
                    && (mToneGenerator != null)) {
                mToneGenerator.stopTone();
                mToneGenerator.release();
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        mInCallVolume,
                        0);
            } else if ((mCurrentEmergencyToneState == EMERGENCY_TONE_VIBRATE)
                    && (mEmgVibrator != null)) {
                mEmgVibrator.cancel();
            }
            mCurrentEmergencyToneState = EMERGENCY_TONE_OFF;
        }
    }

     private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
        new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothHeadset = (BluetoothHeadset) proxy;
            if (VDBG) log("- Got BluetoothHeadset: " + mBluetoothHeadset);
        }

        public void onServiceDisconnected(int profile) {
            mBluetoothHeadset = null;
        }
    };

    private void onRingbackTone(AsyncResult r) {
        boolean playTone = (Boolean)(r.result);

        if (playTone == true) {
            // Only play when foreground call is in DIALING or ALERTING.
            // to prevent a late coming playtone after ALERTING.
            // Don't play ringback tone if it is in play, otherwise it will cut
            // the current tone and replay it
            if (mCM.getActiveFgCallState().isDialing() &&
                mInCallRingbackTonePlayer == null) {
                mInCallRingbackTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_RING_BACK);
                mInCallRingbackTonePlayer.start();
            }
        } else {
            if (mInCallRingbackTonePlayer != null) {
                mInCallRingbackTonePlayer.stopTone();
                mInCallRingbackTonePlayer = null;
            }
        }
    }

    /**
     * Toggle mute and unmute requests while keeping the same mute state
     */
    private void onResendMute() {
        boolean muteState = PhoneUtils.getMute();
        PhoneUtils.setMute(!muteState);
        PhoneUtils.setMute(muteState);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
