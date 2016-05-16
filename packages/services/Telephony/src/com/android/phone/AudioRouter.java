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

import com.google.common.collect.Lists;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.MediaStore.Audio;
import android.util.Log;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.BluetoothManager.BluetoothIndicatorListener;
import com.android.phone.WiredHeadsetManager.WiredHeadsetListener;
import com.android.services.telephony.common.AudioMode;

import java.util.List;

/**
 * Responsible for Routing in-call audio and maintaining routing state.
 */
/* package */ class AudioRouter implements BluetoothIndicatorListener, WiredHeadsetListener {

    private static String LOG_TAG = AudioRouter.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private static final boolean ON = true;
    private static final boolean OFF = false;

    private final Context mContext;
    private final BluetoothManager mBluetoothManager;
    private final WiredHeadsetManager mWiredHeadsetManager;
    private final CallManager mCallManager;
    private final List<AudioModeListener> mListeners = Lists.newArrayList();
    private int mAudioMode = AudioMode.EARPIECE;
    private int mPreviousMode = AudioMode.EARPIECE;
    private int mSupportedModes = AudioMode.ALL_MODES;

    public AudioRouter(Context context, BluetoothManager bluetoothManager,
            WiredHeadsetManager wiredHeadsetManager, CallManager callManager) {
        mContext = context;
        mBluetoothManager = bluetoothManager;
        mWiredHeadsetManager = wiredHeadsetManager;
        mCallManager = callManager;

        init();
    }

    /**
     * Return the current audio mode.
     */
    public int getAudioMode() {
        return mAudioMode;
    }

    /**
     * Returns the currently supported audio modes.
     */
    public int getSupportedAudioModes() {
        return mSupportedModes;
    }

    /**
     * Returns the current mute state.
     */
    public boolean getMute() {
        return PhoneUtils.getMute();
    }

    /**
     * Add a listener to audio mode changes.
     */
    public void addAudioModeListener(AudioModeListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);

            // For first notification, mPreviousAudioMode doesn't make sense.
            listener.onAudioModeChange(mAudioMode, getMute());
            listener.onSupportedAudioModeChange(mSupportedModes);
        }
    }

    /**
     * Remove  listener.
     */
    public void removeAudioModeListener(AudioModeListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    /**
     * Sets the audio mode to the mode that is passed in.
     */
    public void setAudioMode(int mode) {
        logD("setAudioMode " + AudioMode.toString(mode));
        boolean error = false;

        // changes WIRED_OR_EARPIECE to appropriate single entry WIRED_HEADSET or EARPIECE
        mode = selectWiredOrEarpiece(mode);

        // If mode is unsupported, do nothing.
        if ((calculateSupportedModes() | mode) == 0) {
            Log.wtf(LOG_TAG, "Asking to set to a mode that is unsupported: " + mode);
            return;
        }

        if (AudioMode.SPEAKER == mode) {
            turnOnOffBluetooth(OFF);
            turnOnOffSpeaker(ON);

        } else if (AudioMode.BLUETOOTH == mode) {
            if (mBluetoothManager.isBluetoothAvailable()) {
                // Manually turn the speaker phone off, instead of allowing the
                // Bluetooth audio routing to handle it, since there's other
                // important state-updating that needs to happen in the
                // PhoneUtils.turnOnSpeaker() method.
                // (Similarly, whenever the user turns *on* the speaker, we
                // manually disconnect the active bluetooth headset;
                // see toggleSpeaker() and/or switchInCallAudio().)
                turnOnOffSpeaker(OFF);
                if (!turnOnOffBluetooth(ON)) {
                    error = true;
                }
            } else {
                Log.e(LOG_TAG, "Asking to turn on bluetooth when no bluetooth available. " +
                        "supportedModes: " + AudioMode.toString(calculateSupportedModes()));
                error = true;
            }

        // Wired headset and earpiece work the same way
        } else if (AudioMode.EARPIECE == mode || AudioMode.WIRED_HEADSET == mode) {
            turnOnOffBluetooth(OFF);
            turnOnOffSpeaker(OFF);

        } else {
            error = true;
        }

        if (error) {
            mode = calculateModeFromCurrentState();
            Log.e(LOG_TAG, "There was an error in setting new audio mode. " +
                    "Resetting mode to " + AudioMode.toString(mode) + ".");

        }

        updateAudioModeTo(mode);
    }

    /**
     * Turns on speaker.
     */
    public void setSpeaker(boolean on) {
        logD("setSpeaker " + on);

        if (on) {
            setAudioMode(AudioMode.SPEAKER);
        } else {
            setAudioMode(AudioMode.WIRED_OR_EARPIECE);
        }
    }

    public void onMuteChange(boolean muted) {
        logD("onMuteChange: " + muted);
        notifyListeners();
    }

    /**
     * Called when the bluetooth connection changes.
     * We adjust the audio mode according to the state we receive.
     */
    @Override
    public void onBluetoothIndicationChange(boolean isConnected, BluetoothManager btManager) {
        logD("onBluetoothIndicationChange " + isConnected);

        // this will read the new bluetooth mode appropriately
        updateAudioModeTo(calculateModeFromCurrentState());
    }

    /**
     * Called when the state of the wired headset changes.
     */
    @Override
    public void onWiredHeadsetConnection(boolean pluggedIn) {
        logD("onWireHeadsetConnection " + pluggedIn);

        // Since the presence of a wired headset or bluetooth affects the
        // speakerphone, update the "speaker" state.  We ONLY want to do
        // this on the wired headset connect / disconnect events for now
        // though.
        final boolean isOffhook = (mCallManager.getState() == PhoneConstants.State.OFFHOOK);

        int newMode = mAudioMode;

        // Change state only if we are not using bluetooth
        if (!mBluetoothManager.isBluetoothHeadsetAudioOn()) {

            // Do special logic with speakerphone if we have an ongoing (offhook) call.
            if (isOffhook) {
                if (!pluggedIn) {
                    // if the state is "not connected", restore the speaker state.
                    PhoneUtils.restoreSpeakerMode(mContext);

                    if (PhoneUtils.isSpeakerOn(mContext)) {
                        newMode = AudioMode.SPEAKER;
                    } else {
                        newMode = AudioMode.EARPIECE;
                    }
                } else {
                    // if the state is "connected", force the speaker off without
                    // storing the state.
                    PhoneUtils.turnOnSpeaker(mContext, false, false);

                    newMode = AudioMode.WIRED_HEADSET;
                }

            // if we are outside of a phone call, the logic is simpler
            } else {
                newMode = pluggedIn ? AudioMode.WIRED_HEADSET : AudioMode.EARPIECE;
            }
        }

        updateAudioModeTo(newMode);
    }

    /**
     * Changes WIRED_OR_EARPIECE to appropriate single entry WIRED_HEADSET or EARPIECE.
     * If mode passed it is not WIRED_OR_EARPIECE, this is a no-op and simply returns
     * the unchanged mode parameter.
     */
    private int selectWiredOrEarpiece(int mode) {
        // Since they are mutually exclusive and one is ALWAYS valid, we allow a special input of
        // WIRED_OR_EARPIECE so that callers dont have to make a call to check which is supported
        // before calling setAudioMode.
        if (mode == AudioMode.WIRED_OR_EARPIECE) {
            mode = AudioMode.WIRED_OR_EARPIECE & mSupportedModes;

            if (mode == 0) {
                Log.wtf(LOG_TAG, "One of wired headset or earpiece should always be valid.");
                // assume earpiece in this case.
                mode = AudioMode.EARPIECE;
            }
        }

        return mode;
    }

    /**
     * Turns on/off bluetooth.  If bluetooth is already in the correct mode, this does
     * nothing.
     */
    private boolean turnOnOffBluetooth(boolean onOff) {
        if (mBluetoothManager.isBluetoothAvailable()) {
            final boolean isAlreadyOn = mBluetoothManager.isBluetoothAudioConnected();

            if (onOff && !isAlreadyOn) {
                mBluetoothManager.connectBluetoothAudio();
            } else if (!onOff && isAlreadyOn) {
                mBluetoothManager.disconnectBluetoothAudio();
            }
        } else if (onOff) {
            Log.e(LOG_TAG, "Asking to turn on bluetooth, but there is no bluetooth availabled.");
            return false;
        }

        return true;
    }

    /**
     * Turn on/off speaker.
     */
    private void turnOnOffSpeaker(boolean onOff) {
        if (PhoneUtils.isSpeakerOn(mContext) != onOff) {
            PhoneUtils.turnOnSpeaker(mContext, onOff, true /* storeState */);
        }
    }

    private void init() {
        mBluetoothManager.addBluetoothIndicatorListener(this);
        mWiredHeadsetManager.addWiredHeadsetListener(this);
    }

    /**
     * Reads the state of the world to determine Audio mode.
     */
    private int calculateModeFromCurrentState() {

        int mode = AudioMode.EARPIECE;

        // There is a very specific ordering here
        if (mBluetoothManager.showBluetoothIndication()) {
            mode = AudioMode.BLUETOOTH;
        } else if (PhoneUtils.isSpeakerOn(mContext)) {
            mode = AudioMode.SPEAKER;
        } else if (mWiredHeadsetManager.isHeadsetPlugged()) {
            mode = AudioMode.WIRED_HEADSET;
        }

        logD("calculateModeFromCurrentState " + AudioMode.toString(mode));

        return mode;
    }

    /**
     * Changes the audio mode to the mode in the parameter.
     */
    private void updateAudioModeTo(int mode) {
        int oldSupportedModes = mSupportedModes;

        mSupportedModes = calculateSupportedModes();

        // This is a weird state that shouldn't happen, but we get called here
        // once we've changed the audio layers so lets log the error, but assume
        // that it went through. If it happens it is likely it is a race condition
        // that will resolve itself when we get updates on the mode change.
        if ((mSupportedModes & mode) == 0) {
            Log.e(LOG_TAG, "Setting audio mode to an unsupported mode: " +
                    AudioMode.toString(mode) + ", supported (" +
                    AudioMode.toString(mSupportedModes) + ")");
        }

        boolean doNotify = oldSupportedModes != mSupportedModes;

        // only update if it really changed.
        if (mAudioMode != mode) {
            Log.i(LOG_TAG, "Audio mode changing to " + AudioMode.toString(mode));
            doNotify = true;
        }

        mPreviousMode = mAudioMode;
        mAudioMode = mode;

        if (doNotify) {
            notifyListeners();
        }
    }

    /**
     * Gets the updates supported modes from the state of the audio systems.
     */
    private int calculateSupportedModes() {
        // speaker phone always a supported state
        int supportedModes = AudioMode.SPEAKER;

        if (mWiredHeadsetManager.isHeadsetPlugged()) {
            supportedModes |= AudioMode.WIRED_HEADSET;
        } else {
            supportedModes |= AudioMode.EARPIECE;
        }

        if (mBluetoothManager.isBluetoothAvailable()) {
            supportedModes |= AudioMode.BLUETOOTH;
        }

        return supportedModes;
    }

    private void notifyListeners() {
        logD("AudioMode: " + AudioMode.toString(mAudioMode));
        logD("Supported AudioMode: " + AudioMode.toString(mSupportedModes));

        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onAudioModeChange(mAudioMode, getMute());
            mListeners.get(i).onSupportedAudioModeChange(mSupportedModes);
        }
    }

    public interface AudioModeListener {
        void onAudioModeChange(int newMode, boolean muted);
        void onSupportedAudioModeChange(int modeMask);
    }

    private void logD(String msg) {
        if (DBG) {
            Log.d(LOG_TAG, msg);
        }
    }
}
