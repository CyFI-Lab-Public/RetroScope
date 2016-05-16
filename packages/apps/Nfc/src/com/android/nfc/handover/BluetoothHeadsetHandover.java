/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.nfc.handover;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.IAudioService;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.android.nfc.R;

/**
 * Connects / Disconnects from a Bluetooth headset (or any device that
 * might implement BT HSP, HFP or A2DP sink) when touched with NFC.
 *
 * This object is created on an NFC interaction, and determines what
 * sequence of Bluetooth actions to take, and executes them. It is not
 * designed to be re-used after the sequence has completed or timed out.
 * Subsequent NFC interactions should use new objects.
 *
 */
public class BluetoothHeadsetHandover implements BluetoothProfile.ServiceListener {
    static final String TAG = HandoverManager.TAG;
    static final boolean DBG = HandoverManager.DBG;

    static final String ACTION_ALLOW_CONNECT = "com.android.nfc.handover.action.ALLOW_CONNECT";
    static final String ACTION_DENY_CONNECT = "com.android.nfc.handover.action.DENY_CONNECT";

    static final int TIMEOUT_MS = 20000;

    static final int STATE_INIT = 0;
    static final int STATE_WAITING_FOR_PROXIES = 1;
    static final int STATE_INIT_COMPLETE = 2;
    static final int STATE_WAITING_FOR_BOND_CONFIRMATION = 3;
    static final int STATE_BONDING = 4;
    static final int STATE_CONNECTING = 5;
    static final int STATE_DISCONNECTING = 6;
    static final int STATE_COMPLETE = 7;

    static final int RESULT_PENDING = 0;
    static final int RESULT_CONNECTED = 1;
    static final int RESULT_DISCONNECTED = 2;

    static final int ACTION_INIT = 0;
    static final int ACTION_DISCONNECT = 1;
    static final int ACTION_CONNECT = 2;

    static final int MSG_TIMEOUT = 1;
    static final int MSG_NEXT_STEP = 2;

    final Context mContext;
    final BluetoothDevice mDevice;
    final String mName;
    final Callback mCallback;
    final BluetoothAdapter mBluetoothAdapter;

    final Object mLock = new Object();

    // only used on main thread
    int mAction;
    int mState;
    int mHfpResult;  // used only in STATE_CONNECTING and STATE_DISCONNETING
    int mA2dpResult; // used only in STATE_CONNECTING and STATE_DISCONNETING

    // protected by mLock
    BluetoothA2dp mA2dp;
    BluetoothHeadset mHeadset;

    public interface Callback {
        public void onBluetoothHeadsetHandoverComplete(boolean connected);
    }

    public BluetoothHeadsetHandover(Context context, BluetoothDevice device, String name,
            Callback callback) {
        checkMainThread();  // mHandler must get get constructed on Main Thread for toasts to work
        mContext = context;
        mDevice = device;
        mName = name;
        mCallback = callback;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_INIT;
    }

    public boolean hasStarted() {
        return mState != STATE_INIT;
    }

    /**
     * Main entry point. This method is usually called after construction,
     * to begin the BT sequence. Must be called on Main thread.
     */
    public void start() {
        checkMainThread();
        if (mState != STATE_INIT) return;
        if (mBluetoothAdapter == null) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(ACTION_ALLOW_CONNECT);
        filter.addAction(ACTION_DENY_CONNECT);

        mContext.registerReceiver(mReceiver, filter);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_TIMEOUT), TIMEOUT_MS);
        mAction = ACTION_INIT;
        nextStep();
    }

    /**
     * Called to execute next step in state machine
     */
    void nextStep() {
        if (mAction == ACTION_INIT) {
            nextStepInit();
        } else if (mAction == ACTION_CONNECT) {
            nextStepConnect();
        } else {
            nextStepDisconnect();
        }
    }

    /*
     * Enables bluetooth and gets the profile proxies
     */
    void nextStepInit() {
        switch (mState) {
            case STATE_INIT:
                if (mA2dp == null || mHeadset == null) {
                    mState = STATE_WAITING_FOR_PROXIES;
                    if (!getProfileProxys()) {
                        complete(false);
                    }
                    break;
                }
                // fall-through
            case STATE_WAITING_FOR_PROXIES:
                mState = STATE_INIT_COMPLETE;
                // Check connected devices and see if we need to disconnect
                synchronized(mLock) {
                    if (mA2dp.getConnectedDevices().contains(mDevice) ||
                            mHeadset.getConnectedDevices().contains(mDevice)) {
                        Log.i(TAG, "ACTION_DISCONNECT addr=" + mDevice + " name=" + mName);
                        mAction = ACTION_DISCONNECT;
                    } else {
                        Log.i(TAG, "ACTION_CONNECT addr=" + mDevice + " name=" + mName);
                        mAction = ACTION_CONNECT;
                    }
                }
                nextStep();
        }

    }

    void nextStepDisconnect() {
        switch (mState) {
            case STATE_INIT_COMPLETE:
                mState = STATE_DISCONNECTING;
                synchronized (mLock) {
                    if (mHeadset.getConnectionState(mDevice) != BluetoothProfile.STATE_DISCONNECTED) {
                        mHfpResult = RESULT_PENDING;
                        mHeadset.disconnect(mDevice);
                    } else {
                        mHfpResult = RESULT_DISCONNECTED;
                    }
                    if (mA2dp.getConnectionState(mDevice) != BluetoothProfile.STATE_DISCONNECTED) {
                        mA2dpResult = RESULT_PENDING;
                        mA2dp.disconnect(mDevice);
                    } else {
                        mA2dpResult = RESULT_DISCONNECTED;
                    }
                    if (mA2dpResult == RESULT_PENDING || mHfpResult == RESULT_PENDING) {
                        toast(mContext.getString(R.string.disconnecting_headset ) + " " +
                                mName + "...");
                        break;
                    }
                }
                // fall-through
            case STATE_DISCONNECTING:
                if (mA2dpResult == RESULT_PENDING || mHfpResult == RESULT_PENDING) {
                    // still disconnecting
                    break;
                }
                if (mA2dpResult == RESULT_DISCONNECTED && mHfpResult == RESULT_DISCONNECTED) {
                    toast(mContext.getString(R.string.disconnected_headset) + " " + mName);
                }
                complete(false);
                break;
        }

    }

    boolean getProfileProxys() {
        if(!mBluetoothAdapter.getProfileProxy(mContext, this, BluetoothProfile.HEADSET))
            return false;

        if(!mBluetoothAdapter.getProfileProxy(mContext, this, BluetoothProfile.A2DP))
            return false;

        return true;
    }

    void nextStepConnect() {
        switch (mState) {
            case STATE_INIT_COMPLETE:
                if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    requestPairConfirmation();
                    mState = STATE_WAITING_FOR_BOND_CONFIRMATION;

                    break;
                }
                // fall-through
            case STATE_WAITING_FOR_BOND_CONFIRMATION:
                if (mDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    startBonding();
                    break;
                }
                // fall-through
            case STATE_BONDING:
                // Bluetooth Profile service will correctly serialize
                // HFP then A2DP connect
                mState = STATE_CONNECTING;
                synchronized (mLock) {
                    if (mHeadset.getConnectionState(mDevice) != BluetoothProfile.STATE_CONNECTED) {
                        mHfpResult = RESULT_PENDING;
                        mHeadset.connect(mDevice);
                    } else {
                        mHfpResult = RESULT_CONNECTED;
                    }
                    if (mA2dp.getConnectionState(mDevice) != BluetoothProfile.STATE_CONNECTED) {
                        mA2dpResult = RESULT_PENDING;
                        mA2dp.connect(mDevice);
                    } else {
                        mA2dpResult = RESULT_CONNECTED;
                    }
                    if (mA2dpResult == RESULT_PENDING || mHfpResult == RESULT_PENDING) {
                        toast(mContext.getString(R.string.connecting_headset) + " " + mName + "...");
                        break;
                    }
                }
                // fall-through
            case STATE_CONNECTING:
                if (mA2dpResult == RESULT_PENDING || mHfpResult == RESULT_PENDING) {
                    // another connection type still pending
                    break;
                }
                if (mA2dpResult == RESULT_CONNECTED || mHfpResult == RESULT_CONNECTED) {
                    // we'll take either as success
                    toast(mContext.getString(R.string.connected_headset) + " " + mName);
                    if (mA2dpResult == RESULT_CONNECTED) startTheMusic();
                    complete(true);
                } else {
                    toast (mContext.getString(R.string.connect_headset_failed) + " " + mName);
                    complete(false);
                }
                break;
        }
    }

    void startBonding() {
        mState = STATE_BONDING;
        toast(mContext.getString(R.string.pairing_headset) + " " + mName + "...");
        if (!mDevice.createBond()) {
            toast(mContext.getString(R.string.pairing_headset_failed) + " " + mName);
            complete(false);
        }
    }

    void handleIntent(Intent intent) {
        String action = intent.getAction();
        // Everything requires the device to match...
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!mDevice.equals(device)) return;

        if (ACTION_ALLOW_CONNECT.equals(action)) {
            nextStepConnect();
        } else if (ACTION_DENY_CONNECT.equals(action)) {
            complete(false);
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action) && mState == STATE_BONDING) {
            int bond = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                    BluetoothAdapter.ERROR);
            if (bond == BluetoothDevice.BOND_BONDED) {
                nextStepConnect();
            } else if (bond == BluetoothDevice.BOND_NONE) {
                toast(mContext.getString(R.string.pairing_headset_failed) + " " + mName);
                complete(false);
            }
        } else if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action) &&
                (mState == STATE_CONNECTING || mState == STATE_DISCONNECTING)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                mHfpResult = RESULT_CONNECTED;
                nextStep();
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                mHfpResult = RESULT_DISCONNECTED;
                nextStep();
            }
        } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(action) &&
                (mState == STATE_CONNECTING || mState == STATE_DISCONNECTING)) {
            int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                mA2dpResult = RESULT_CONNECTED;
                nextStep();
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                mA2dpResult = RESULT_DISCONNECTED;
                nextStep();
            }
        }
    }

    void complete(boolean connected) {
        if (DBG) Log.d(TAG, "complete()");
        mState = STATE_COMPLETE;
        mContext.unregisterReceiver(mReceiver);
        mHandler.removeMessages(MSG_TIMEOUT);
        synchronized (mLock) {
            if (mA2dp != null) {
                mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mA2dp);
            }
            if (mHeadset != null) {
                mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mHeadset);
            }
            mA2dp = null;
            mHeadset = null;
        }
        mCallback.onBluetoothHeadsetHandoverComplete(connected);
    }

    void toast(CharSequence text) {
        Toast.makeText(mContext,  text, Toast.LENGTH_SHORT).show();
    }

    void startTheMusic() {
        IAudioService audioService = IAudioService.Stub.asInterface(
                ServiceManager.checkService(Context.AUDIO_SERVICE));
        if (audioService != null) {
            try {
                KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
                audioService.dispatchMediaKeyEvent(keyEvent);
                keyEvent = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY);
                audioService.dispatchMediaKeyEvent(keyEvent);
            } catch (RemoteException e) {
                Log.e(TAG, "dispatchMediaKeyEvent threw exception " + e);
            }
        } else {
            Log.w(TAG, "Unable to find IAudioService for media key event");
        }
    }

    void requestPairConfirmation() {
        Intent dialogIntent = new Intent(mContext, ConfirmConnectActivity.class);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        dialogIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mDevice);

        mContext.startActivity(dialogIntent);
    }

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_TIMEOUT:
                    if (mState == STATE_COMPLETE) return;
                    Log.i(TAG, "Timeout completing BT handover");
                    complete(false);
                    break;
                case MSG_NEXT_STEP:
                    nextStep();
                    break;
            }
        }
    };

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleIntent(intent);
        }
    };

    static void checkMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalThreadStateException("must be called on main thread");
        }
    }

    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        synchronized (mLock) {
            switch (profile) {
                case BluetoothProfile.HEADSET:
                    mHeadset = (BluetoothHeadset) proxy;
                    if (mA2dp != null) {
                        mHandler.sendEmptyMessage(MSG_NEXT_STEP);
                    }
                    break;
                case BluetoothProfile.A2DP:
                    mA2dp = (BluetoothA2dp) proxy;
                    if (mHeadset != null) {
                        mHandler.sendEmptyMessage(MSG_NEXT_STEP);
                    }
                    break;
            }
        }
    }

    @Override
    public void onServiceDisconnected(int profile) {
        // We can ignore these
    }
}
