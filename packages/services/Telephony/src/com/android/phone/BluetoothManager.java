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

import com.google.android.collect.Lists;
import com.google.common.base.Preconditions;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.services.telephony.common.Call;

import java.util.List;

/**
 * Listens to and caches bluetooth headset state.  Used By the AudioRouter for maintaining
 * overall audio state for use in the UI layer. Also provides method for connecting the bluetooth
 * headset to the phone call.
 */
public class BluetoothManager implements CallModeler.Listener {
    private static final String LOG_TAG = BluetoothManager.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);

    private final BluetoothAdapter mBluetoothAdapter;
    private final CallManager mCallManager;
    private final Context mContext;
    private final CallModeler mCallModeler;

    private BluetoothHeadset mBluetoothHeadset;
    private int mBluetoothHeadsetState = BluetoothProfile.STATE_DISCONNECTED;
    private int mBluetoothHeadsetAudioState = BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    private boolean mShowBluetoothIndication = false;
    private boolean mBluetoothConnectionPending = false;
    private long mBluetoothConnectionRequestTime;

    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new BluetoothBroadcastReceiver();

    private final List<BluetoothIndicatorListener> mListeners = Lists.newArrayList();

    public BluetoothManager(Context context, CallManager callManager, CallModeler callModeler) {
        mContext = context;
        mCallManager = callManager;
        mCallModeler = callModeler;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        init(mContext);
    }

    /* package */ boolean isBluetoothHeadsetAudioOn() {
        return (mBluetoothHeadsetAudioState != BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
    }

    //
    // Bluetooth helper methods.
    //
    // - BluetoothAdapter is the Bluetooth system service.  If
    //   getDefaultAdapter() returns null
    //   then the device is not BT capable.  Use BluetoothDevice.isEnabled()
    //   to see if BT is enabled on the device.
    //
    // - BluetoothHeadset is the API for the control connection to a
    //   Bluetooth Headset.  This lets you completely connect/disconnect a
    //   headset (which we don't do from the Phone UI!) but also lets you
    //   get the address of the currently active headset and see whether
    //   it's currently connected.

    /**
     * @return true if the Bluetooth on/off switch in the UI should be
     *         available to the user (i.e. if the device is BT-capable
     *         and a headset is connected.)
     */
    /* package */ boolean isBluetoothAvailable() {
        if (VDBG) log("isBluetoothAvailable()...");

        // There's no need to ask the Bluetooth system service if BT is enabled:
        //
        //    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //    if ((adapter == null) || !adapter.isEnabled()) {
        //        if (DBG) log("  ==> FALSE (BT not enabled)");
        //        return false;
        //    }
        //    if (DBG) log("  - BT enabled!  device name " + adapter.getName()
        //                 + ", address " + adapter.getAddress());
        //
        // ...since we already have a BluetoothHeadset instance.  We can just
        // call isConnected() on that, and assume it'll be false if BT isn't
        // enabled at all.

        // Check if there's a connected headset, using the BluetoothHeadset API.
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

            if (deviceList.size() > 0) {
                BluetoothDevice device = deviceList.get(0);
                isConnected = true;

                if (VDBG) log("  - headset state = " +
                              mBluetoothHeadset.getConnectionState(device));
                if (VDBG) log("  - headset address: " + device);
                if (VDBG) log("  - isConnected: " + isConnected);
            }
        }

        if (VDBG) log("  ==> " + isConnected);
        return isConnected;
    }

    /**
     * @return true if a BT Headset is available, and its audio is currently connected.
     */
    /* package */ boolean isBluetoothAudioConnected() {
        if (mBluetoothHeadset == null) {
            if (VDBG) log("isBluetoothAudioConnected: ==> FALSE (null mBluetoothHeadset)");
            return false;
        }
        List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

        if (deviceList.isEmpty()) {
            return false;
        }
        BluetoothDevice device = deviceList.get(0);
        boolean isAudioOn = mBluetoothHeadset.isAudioConnected(device);
        if (VDBG) log("isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn);
        return isAudioOn;
    }

    /**
     * Helper method used to control the onscreen "Bluetooth" indication;
     * see InCallControlState.bluetoothIndicatorOn.
     *
     * @return true if a BT device is available and its audio is currently connected,
     *              <b>or</b> if we issued a BluetoothHeadset.connectAudio()
     *              call within the last 5 seconds (which presumably means
     *              that the BT audio connection is currently being set
     *              up, and will be connected soon.)
     */
    /* package */ boolean isBluetoothAudioConnectedOrPending() {
        if (isBluetoothAudioConnected()) {
            if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)");
            return true;
        }

        // If we issued a connectAudio() call "recently enough", even
        // if BT isn't actually connected yet, let's still pretend BT is
        // on.  This makes the onscreen indication more responsive.
        if (mBluetoothConnectionPending) {
            long timeSinceRequest =
                    SystemClock.elapsedRealtime() - mBluetoothConnectionRequestTime;
            if (timeSinceRequest < 5000 /* 5 seconds */) {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (requested "
                             + timeSinceRequest + " msec ago)");
                return true;
            } else {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: "
                             + timeSinceRequest + " msec ago)");
                mBluetoothConnectionPending = false;
                return false;
            }
        }

        if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE");
        return false;
    }

    /**
     * @return true if the onscreen UI should currently be showing the
     * special "bluetooth is active" indication in a couple of places (in
     * which UI elements turn blue and/or show the bluetooth logo.)
     *
     * This depends on the BluetoothHeadset state *and* the current
     * telephony state; see shouldShowBluetoothIndication().
     *
     * @see CallCard
     * @see NotificationMgr.updateInCallNotification
     */
    /* package */ boolean showBluetoothIndication() {
        return mShowBluetoothIndication;
    }

    /**
     * Recomputes the mShowBluetoothIndication flag based on the current
     * bluetooth state and current telephony state.
     *
     * This needs to be called any time the bluetooth headset state or the
     * telephony state changes.
     */
    /* package */ void updateBluetoothIndication() {
        mShowBluetoothIndication = shouldShowBluetoothIndication(mBluetoothHeadsetState,
                                                                 mBluetoothHeadsetAudioState,
                                                                 mCallManager);

        notifyListeners(mShowBluetoothIndication);
    }

    public void addBluetoothIndicatorListener(BluetoothIndicatorListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeBluetoothIndicatorListener(BluetoothIndicatorListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    private void notifyListeners(boolean showBluetoothOn) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onBluetoothIndicationChange(showBluetoothOn, this);
        }
    }

    private void init(Context context) {
        Preconditions.checkNotNull(context);

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getProfileProxy(context, mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }

        // Register for misc other intent broadcasts.
        IntentFilter intentFilter =
                new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        context.registerReceiver(mReceiver, intentFilter);

        mCallModeler.addListener(this);
    }

    private void tearDown() {
        if (mBluetoothHeadset != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
            mBluetoothHeadset = null;
        }
    }

    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
             new BluetoothProfile.ServiceListener() {
         @Override
         public void onServiceConnected(int profile, BluetoothProfile proxy) {
             mBluetoothHeadset = (BluetoothHeadset) proxy;
             if (VDBG) log("- Got BluetoothHeadset: " + mBluetoothHeadset);
         }

         @Override
         public void onServiceDisconnected(int profile) {
             mBluetoothHeadset = null;
         }
    };

    /**
     * UI policy helper function for the couple of places in the UI that
     * have some way of indicating that "bluetooth is in use."
     *
     * @return true if the onscreen UI should indicate that "bluetooth is in use",
     *         based on the specified bluetooth headset state, and the
     *         current state of the phone.
     * @see showBluetoothIndication()
     */
    private static boolean shouldShowBluetoothIndication(int bluetoothState,
                                                         int bluetoothAudioState,
                                                         CallManager cm) {
        // We want the UI to indicate that "bluetooth is in use" in two
        // slightly different cases:
        //
        // (a) The obvious case: if a bluetooth headset is currently in
        //     use for an ongoing call.
        //
        // (b) The not-so-obvious case: if an incoming call is ringing,
        //     and we expect that audio *will* be routed to a bluetooth
        //     headset once the call is answered.

        switch (cm.getState()) {
            case OFFHOOK:
                // This covers normal active calls, and also the case if
                // the foreground call is DIALING or ALERTING.  In this
                // case, bluetooth is considered "active" if a headset
                // is connected *and* audio is being routed to it.
                return ((bluetoothState == BluetoothHeadset.STATE_CONNECTED)
                        && (bluetoothAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED));

            case RINGING:
                // If an incoming call is ringing, we're *not* yet routing
                // audio to the headset (since there's no in-call audio
                // yet!)  In this case, if a bluetooth headset is
                // connected at all, we assume that it'll become active
                // once the user answers the phone.
                return (bluetoothState == BluetoothHeadset.STATE_CONNECTED);

            default:  // Presumably IDLE
                return false;
        }
    }

    private void dumpBluetoothState() {
        log("============== dumpBluetoothState() =============");
        log("= isBluetoothAvailable: " + isBluetoothAvailable());
        log("= isBluetoothAudioConnected: " + isBluetoothAudioConnected());
        log("= isBluetoothAudioConnectedOrPending: " + isBluetoothAudioConnectedOrPending());
        log("= PhoneApp.showBluetoothIndication: "
            + showBluetoothIndication());
        log("=");
        if (mBluetoothAdapter != null) {
            if (mBluetoothHeadset != null) {
                List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

                if (deviceList.size() > 0) {
                    BluetoothDevice device = deviceList.get(0);
                    log("= BluetoothHeadset.getCurrentDevice: " + device);
                    log("= BluetoothHeadset.State: "
                        + mBluetoothHeadset.getConnectionState(device));
                    log("= BluetoothHeadset audio connected: " +
                        mBluetoothHeadset.isAudioConnected(device));
                }
            } else {
                log("= mBluetoothHeadset is null");
            }
        } else {
            log("= mBluetoothAdapter is null; device is not BT capable");
        }
    }

    /* package */ void connectBluetoothAudio() {
        if (VDBG) log("connectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            // TODO(BT) check return
            mBluetoothHeadset.connectAudio();
        }

        // Watch out: The bluetooth connection doesn't happen instantly;
        // the connectAudio() call returns instantly but does its real
        // work in another thread.  The mBluetoothConnectionPending flag
        // is just a little trickery to ensure that the onscreen UI updates
        // instantly. (See isBluetoothAudioConnectedOrPending() above.)
        mBluetoothConnectionPending = true;
        mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
    }

    /* package */ void disconnectBluetoothAudio() {
        if (VDBG) log("disconnectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }
        mBluetoothConnectionPending = false;
    }

    /**
     * Receiver for misc intent broadcasts the BluetoothManager cares about.
     */
    private class BluetoothBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                                          BluetoothHeadset.STATE_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
                // Also update any visible UI if necessary
                updateBluetoothIndication();
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                mBluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                           BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetAudioState);
                updateBluetoothIndication();
            }
        }
    }

    @Override
    public void onDisconnect(Call call) {
        updateBluetoothIndication();
    }

    @Override
    public void onIncoming(Call call) {
        // An incoming call can affect bluetooth indicator, so we update it whenever there is
        // a change to any of the calls.
        updateBluetoothIndication();
    }

    @Override
    public void onUpdate(List<Call> calls) {
        updateBluetoothIndication();
    }

    @Override
    public void onPostDialAction(Connection.PostDialState state, int callId, String chars, char c) {
        // no-op
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /* package */ interface BluetoothIndicatorListener {
        public void onBluetoothIndicationChange(boolean isConnected, BluetoothManager manager);
    }
}
