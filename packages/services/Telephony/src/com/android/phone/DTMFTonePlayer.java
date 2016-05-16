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

import com.google.common.collect.ImmutableMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection.PostDialState;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.services.telephony.common.Call;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Playing DTMF tones through the CallManager.
 */
public class DTMFTonePlayer implements CallModeler.Listener {
    private static final String LOG_TAG = DTMFTonePlayer.class.getSimpleName();
    private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final int DTMF_SEND_CNF = 100;
    private static final int DTMF_STOP = 101;

    /** Hash Map to map a character to a tone*/
    private static final Map<Character, Integer> mToneMap =
            ImmutableMap.<Character, Integer>builder()
                    .put('1', ToneGenerator.TONE_DTMF_1)
                    .put('2', ToneGenerator.TONE_DTMF_2)
                    .put('3', ToneGenerator.TONE_DTMF_3)
                    .put('4', ToneGenerator.TONE_DTMF_4)
                    .put('5', ToneGenerator.TONE_DTMF_5)
                    .put('6', ToneGenerator.TONE_DTMF_6)
                    .put('7', ToneGenerator.TONE_DTMF_7)
                    .put('8', ToneGenerator.TONE_DTMF_8)
                    .put('9', ToneGenerator.TONE_DTMF_9)
                    .put('0', ToneGenerator.TONE_DTMF_0)
                    .put('#', ToneGenerator.TONE_DTMF_P)
                    .put('*', ToneGenerator.TONE_DTMF_S)
                    .build();

    private final CallManager mCallManager;
    private final CallModeler mCallModeler;
    private final Object mToneGeneratorLock = new Object();
    private ToneGenerator mToneGenerator;
    private boolean mLocalToneEnabled;

    // indicates that we are using automatically shortened DTMF tones
    boolean mShortTone;

    // indicate if the confirmation from TelephonyFW is pending.
    private boolean mDTMFBurstCnfPending = false;

    // Queue to queue the short dtmf characters.
    private Queue<Character> mDTMFQueue = new LinkedList<Character>();

    //  Short Dtmf tone duration
    private static final int DTMF_DURATION_MS = 120;

    /**
     * Our own handler to take care of the messages from the phone state changes
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DTMF_SEND_CNF:
                    logD("dtmf confirmation received from FW.");
                    // handle burst dtmf confirmation
                    handleBurstDtmfConfirmation();
                    break;
                case DTMF_STOP:
                    logD("dtmf stop received");
                    stopDtmfTone();
                    break;
            }
        }
    };

    public DTMFTonePlayer(CallManager callManager, CallModeler callModeler) {
        mCallManager = callManager;
        mCallModeler = callModeler;
        mCallModeler.addListener(this);
    }

    @Override
    public void onDisconnect(Call call) {
        logD("Call disconnected");
        checkCallState();
    }

    @Override
    public void onIncoming(Call call) {
    }

    @Override
    public void onUpdate(List<Call> calls) {
        logD("Call updated");
        checkCallState();
    }

    @Override
    public void onPostDialAction(PostDialState state, int callId, String remainingChars,
            char currentChar) {
        switch (state) {
            case STARTED:
                stopLocalToneIfNeeded();
                if (!mToneMap.containsKey(currentChar)) {
                    return;
                }
                startLocalToneIfNeeded(currentChar);
                break;
            case PAUSE:
            case WAIT:
            case WILD:
            case COMPLETE:
                stopLocalToneIfNeeded();
                break;
            default:
                break;
        }
    }

    /**
     * Allocates some resources we keep around during a "dialer session".
     *
     * (Currently, a "dialer session" just means any situation where we
     * might need to play local DTMF tones, which means that we need to
     * keep a ToneGenerator instance around.  A ToneGenerator instance
     * keeps an AudioTrack resource busy in AudioFlinger, so we don't want
     * to keep it around forever.)
     *
     * Call {@link stopDialerSession} to release the dialer session
     * resources.
     */
    public void startDialerSession() {
        logD("startDialerSession()... this = " + this);

        // see if we need to play local tones.
        if (PhoneGlobals.getInstance().getResources().getBoolean(R.bool.allow_local_dtmf_tones)) {
            mLocalToneEnabled = Settings.System.getInt(
                    PhoneGlobals.getInstance().getContentResolver(),
                    Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
        } else {
            mLocalToneEnabled = false;
        }
        logD("- startDialerSession: mLocalToneEnabled = " + mLocalToneEnabled);

        // create the tone generator
        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        if (mLocalToneEnabled) {
            synchronized (mToneGeneratorLock) {
                if (mToneGenerator == null) {
                    try {
                        mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 80);
                    } catch (RuntimeException e) {
                        Log.e(LOG_TAG, "Exception caught while creating local tone generator", e);
                        mToneGenerator = null;
                    }
                }
            }
        }
    }

    /**
     * Releases resources we keep around during a "dialer session"
     * (see {@link startDialerSession}).
     *
     * It's safe to call this even without a corresponding
     * startDialerSession call.
     */
    public void stopDialerSession() {
        // release the tone generator.
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }

        mHandler.removeMessages(DTMF_SEND_CNF);
        synchronized (mDTMFQueue) {
            mDTMFBurstCnfPending = false;
            mDTMFQueue.clear();
        }
    }

    /**
     * Starts playback of the dtmf tone corresponding to the parameter.
     */
    public void playDtmfTone(char c, boolean timedShortTone) {
        // Only play the tone if it exists.
        if (!mToneMap.containsKey(c)) {
            return;
        }

        if (!okToDialDtmfTones()) {
            return;
        }

        PhoneGlobals.getInstance().pokeUserActivity();

        // Read the settings as it may be changed by the user during the call
        Phone phone = mCallManager.getFgPhone();

        // Before we go ahead and start a tone, we need to make sure that any pending
        // stop-tone message is processed.
        if (mHandler.hasMessages(DTMF_STOP)) {
            mHandler.removeMessages(DTMF_STOP);
            stopDtmfTone();
        }

        mShortTone = useShortDtmfTones(phone, phone.getContext());
        logD("startDtmfTone()...");

        // For Short DTMF we need to play the local tone for fixed duration
        if (mShortTone) {
            sendShortDtmfToNetwork(c);
        } else {
            // Pass as a char to be sent to network
            logD("send long dtmf for " + c);
            mCallManager.startDtmf(c);

            // If it is a timed tone, queue up the stop command in DTMF_DURATION_MS.
            if (timedShortTone) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(DTMF_STOP), DTMF_DURATION_MS);
            }
        }

        startLocalToneIfNeeded(c);
    }

    /**
     * Sends the dtmf character over the network for short DTMF settings
     * When the characters are entered in quick succession,
     * the characters are queued before sending over the network.
     */
    private void sendShortDtmfToNetwork(char dtmfDigit) {
        synchronized (mDTMFQueue) {
            if (mDTMFBurstCnfPending == true) {
                // Insert the dtmf char to the queue
                mDTMFQueue.add(new Character(dtmfDigit));
            } else {
                String dtmfStr = Character.toString(dtmfDigit);
                mCallManager.sendBurstDtmf(dtmfStr, 0, 0, mHandler.obtainMessage(DTMF_SEND_CNF));
                // Set flag to indicate wait for Telephony confirmation.
                mDTMFBurstCnfPending = true;
            }
        }
    }

    /**
     * Handles Burst Dtmf Confirmation from the Framework.
     */
    void handleBurstDtmfConfirmation() {
        Character dtmfChar = null;
        synchronized (mDTMFQueue) {
            mDTMFBurstCnfPending = false;
            if (!mDTMFQueue.isEmpty()) {
                dtmfChar = mDTMFQueue.remove();
                Log.i(LOG_TAG, "The dtmf character removed from queue" + dtmfChar);
            }
        }
        if (dtmfChar != null) {
            sendShortDtmfToNetwork(dtmfChar);
        }
    }

    public void stopDtmfTone() {
        if (!mShortTone) {
            mCallManager.stopDtmf();
            stopLocalToneIfNeeded();
        }
    }

    /**
     * Plays the local tone based the phone type, optionally forcing a short
     * tone.
     */
    private void startLocalToneIfNeeded(char c) {
        if (mLocalToneEnabled) {
            synchronized (mToneGeneratorLock) {
                if (mToneGenerator == null) {
                    logD("startDtmfTone: mToneGenerator == null, tone: " + c);
                } else {
                    logD("starting local tone " + c);
                    int toneDuration = -1;
                    if (mShortTone) {
                        toneDuration = DTMF_DURATION_MS;
                    }
                    mToneGenerator.startTone(mToneMap.get(c), toneDuration);
                }
            }
        }
    }

    /**
     * Stops the local tone based on the phone type.
     */
    public void stopLocalToneIfNeeded() {
        if (!mShortTone) {
            // if local tone playback is enabled, stop it.
            logD("trying to stop local tone...");
            if (mLocalToneEnabled) {
                synchronized (mToneGeneratorLock) {
                    if (mToneGenerator == null) {
                        logD("stopLocalTone: mToneGenerator == null");
                    } else {
                        logD("stopping local tone.");
                        mToneGenerator.stopTone();
                    }
                }
            }
        }
    }

    private boolean okToDialDtmfTones() {
        boolean hasActiveCall = false;
        boolean hasIncomingCall = false;

        final List<Call> calls = mCallModeler.getFullList();
        final int len = calls.size();

        for (int i = 0; i < len; i++) {
            // We can also dial while in DIALING state because there are
            // some connections that never update to an ACTIVE state (no
            // indication from the network).
            hasActiveCall |= (calls.get(i).getState() == Call.State.ACTIVE)
                    || (calls.get(i).getState() == Call.State.DIALING);
            hasIncomingCall |= (calls.get(i).getState() == Call.State.INCOMING);
        }

        return hasActiveCall && !hasIncomingCall;
    }

    /**
     * On GSM devices, we never use short tones.
     * On CDMA devices, it depends upon the settings.
     */
    private static boolean useShortDtmfTones(Phone phone, Context context) {
        int phoneType = phone.getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            return false;
        } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            int toneType = android.provider.Settings.System.getInt(
                    context.getContentResolver(),
                    Settings.System.DTMF_TONE_TYPE_WHEN_DIALING,
                    Constants.DTMF_TONE_TYPE_NORMAL);
            if (toneType == Constants.DTMF_TONE_TYPE_NORMAL) {
                return true;
            } else {
                return false;
            }
        } else if (phoneType == PhoneConstants.PHONE_TYPE_SIP) {
            return false;
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }
    }

    /**
     * Checks to see if there are any active calls. If there are, then we want to allocate the tone
     * resources for playing DTMF tone, otherwise release them.
     */
    private void checkCallState() {
        logD("checkCallState");
        if (mCallModeler.hasOutstandingActiveOrDialingCall()) {
            startDialerSession();
        } else {
            stopDialerSession();
        }
    }

    /**
     * static logging method
     */
    private static void logD(String msg) {
        if (DBG) {
            Log.d(LOG_TAG, msg);
        }
    }
}
