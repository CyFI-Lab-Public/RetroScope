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
 * limitations under the License
 */

package com.android.incallui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.PowerManager;

import com.android.incallui.AudioModeProvider.AudioModeListener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.services.telephony.common.AudioMode;
import com.google.common.base.Objects;

/**
 * Class manages the proximity sensor for the in-call UI.
 * We enable the proximity sensor while the user in a phone call. The Proximity sensor turns off
 * the touchscreen and display when the user is close to the screen to prevent user's cheek from
 * causing touch events.
 * The class requires special knowledge of the activity and device state to know when the proximity
 * sensor should be enabled and disabled. Most of that state is fed into this class through
 * public methods.
 */
public class ProximitySensor implements AccelerometerListener.OrientationListener,
        InCallStateListener, AudioModeListener {
    private static final String TAG = ProximitySensor.class.getSimpleName();

    private final PowerManager mPowerManager;
    private final PowerManager.WakeLock mProximityWakeLock;
    private final AudioModeProvider mAudioModeProvider;
    private final AccelerometerListener mAccelerometerListener;
    private int mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
    private boolean mUiShowing = false;
    private boolean mIsPhoneOffhook = false;
    private boolean mDialpadVisible;

    // True if the keyboard is currently *not* hidden
    // Gets updated whenever there is a Configuration change
    private boolean mIsHardKeyboardOpen;

    public ProximitySensor(Context context, AudioModeProvider audioModeProvider) {
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (mPowerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = mPowerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, TAG);
        } else {
            mProximityWakeLock = null;
        }
        Log.d(this, "onCreate: mProximityWakeLock: ", mProximityWakeLock);

        mAccelerometerListener = new AccelerometerListener(context, this);
        mAudioModeProvider = audioModeProvider;
        mAudioModeProvider.addListener(this);
    }

    public void tearDown() {
        mAudioModeProvider.removeListener(this);

        mAccelerometerListener.enable(false);

        if (mProximityWakeLock != null && mProximityWakeLock.isHeld()) {
            mProximityWakeLock.release();
        }
    }

    /**
     * Called to identify when the device is laid down flat.
     */
    @Override
    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode();
    }

    /**
     * Called to keep track of the overall UI state.
     */
    @Override
    public void onStateChange(InCallState state, CallList callList) {
        // We ignore incoming state because we do not want to enable proximity
        // sensor during incoming call screen
        boolean isOffhook = (InCallState.INCALL == state
                || InCallState.OUTGOING == state);

        if (isOffhook != mIsPhoneOffhook) {
            mIsPhoneOffhook = isOffhook;

            mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
            mAccelerometerListener.enable(mIsPhoneOffhook);

            updateProximitySensorMode();
        }
    }

    @Override
    public void onSupportedAudioMode(int modeMask) {
    }

    @Override
    public void onMute(boolean muted) {
    }

    /**
     * Called when the audio mode changes during a call.
     */
    @Override
    public void onAudioMode(int mode) {
        updateProximitySensorMode();
    }

    public void onDialpadVisible(boolean visible) {
        mDialpadVisible = visible;
        updateProximitySensorMode();
    }

    /**
     * Called by InCallActivity to listen for hard keyboard events.
     */
    public void onConfigurationChanged(Configuration newConfig) {
        mIsHardKeyboardOpen = newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;

        // Update the Proximity sensor based on keyboard state
        updateProximitySensorMode();
    }

    /**
     * Used to save when the UI goes in and out of the foreground.
     */
    public void onInCallShowing(boolean showing) {
        if (showing) {
            mUiShowing = true;

        // We only consider the UI not showing for instances where another app took the foreground.
        // If we stopped showing because the screen is off, we still consider that showing.
        } else if (mPowerManager.isScreenOn()) {
            mUiShowing = false;
        }
        updateProximitySensorMode();
    }

    /**
     * TODO: There is no way to determine if a screen is off due to proximity or if it is
     * legitimately off, but if ever we can do that in the future, it would be useful here.
     * Until then, this function will simply return true of the screen is off.
     */
    public boolean isScreenReallyOff() {
        return !mPowerManager.isScreenOn();
    }

    /**
     * @return true if this device supports the "proximity sensor
     * auto-lock" feature while in-call (see updateProximitySensorMode()).
     */
    private boolean proximitySensorModeEnabled() {
        // TODO: Do we disable notification's expanded view when app is in foreground and
        // proximity sensor is on? Is it even possible to do this any more?
        return (mProximityWakeLock != null);
    }

    /**
     * Updates the wake lock used to control proximity sensor behavior,
     * based on the current state of the phone.
     *
     * On devices that have a proximity sensor, to avoid false touches
     * during a call, we hold a PROXIMITY_SCREEN_OFF_WAKE_LOCK wake lock
     * whenever the phone is off hook.  (When held, that wake lock causes
     * the screen to turn off automatically when the sensor detects an
     * object close to the screen.)
     *
     * This method is a no-op for devices that don't have a proximity
     * sensor.
     *
     * Proximity wake lock will *not* be held if any one of the
     * conditions is true while on a call:
     * 1) If the audio is routed via Bluetooth
     * 2) If a wired headset is connected
     * 3) if the speaker is ON
     * 4) If the slider is open(i.e. the hardkeyboard is *not* hidden)
     */
    private void updateProximitySensorMode() {
        if (proximitySensorModeEnabled()) {
            synchronized (mProximityWakeLock) {

                final int audioMode = mAudioModeProvider.getAudioMode();

                // turn proximity sensor off and turn screen on immediately if
                // we are using a headset, the keyboard is open, or the device
                // is being held in a horizontal position.
                boolean screenOnImmediately = (AudioMode.WIRED_HEADSET == audioMode
                        || AudioMode.SPEAKER == audioMode
                        || AudioMode.BLUETOOTH == audioMode
                        || mIsHardKeyboardOpen);

                // We do not keep the screen off when the user is outside in-call screen and we are
                // horizontal, but we do not force it on when we become horizontal until the
                // proximity sensor goes negative.
                final boolean horizontal =
                        (mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL);
                screenOnImmediately |= !mUiShowing && horizontal;

                // We do not keep the screen off when dialpad is visible, we are horizontal, and
                // the in-call screen is being shown.
                // At that moment we're pretty sure users want to use it, instead of letting the
                // proximity sensor turn off the screen by their hands.
                screenOnImmediately |= mDialpadVisible && horizontal;

                Log.v(this, "screenonImmediately: ", screenOnImmediately);

                Log.i(this, Objects.toStringHelper(this)
                        .add("keybrd", mIsHardKeyboardOpen ? 1 : 0)
                        .add("dpad", mDialpadVisible ? 1 : 0)
                        .add("offhook", mIsPhoneOffhook ? 1 : 0)
                        .add("hor", horizontal ? 1 : 0)
                        .add("ui", mUiShowing ? 1 : 0)
                        .add("aud", AudioMode.toString(audioMode)).toString());

                if (mIsPhoneOffhook && !screenOnImmediately) {
                    final String logStr = "turning on proximity sensor: ";
                    // Phone is in use!  Arrange for the screen to turn off
                    // automatically when the sensor detects a close object.
                    if (!mProximityWakeLock.isHeld()) {
                        Log.i(this, logStr + "acquiring");
                        mProximityWakeLock.acquire();
                    } else {
                        Log.i(this, logStr + "already acquired");
                    }
                } else {
                    final String logStr = "turning off proximity sensor: ";
                    // Phone is either idle, or ringing.  We don't want any
                    // special proximity sensor behavior in either case.
                    if (mProximityWakeLock.isHeld()) {
                        Log.i(this, logStr + "releasing");
                        // Wait until user has moved the phone away from his head if we are
                        // releasing due to the phone call ending.
                        // Qtherwise, turn screen on immediately
                        int flags =
                            (screenOnImmediately ? 0 : PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE);
                        mProximityWakeLock.release(flags);
                    } else {
                        Log.i(this, logStr + "already released");
                    }
                }
            }
        }
    }

}
