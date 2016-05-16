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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Pair;

import com.android.nfc.R;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class HandoverService extends Service implements HandoverTransfer.Callback,
        BluetoothHeadsetHandover.Callback {

    static final String TAG = "HandoverService";
    static final boolean DBG = true;

    static final int MSG_REGISTER_CLIENT = 0;
    static final int MSG_DEREGISTER_CLIENT = 1;
    static final int MSG_START_INCOMING_TRANSFER = 2;
    static final int MSG_START_OUTGOING_TRANSFER = 3;
    static final int MSG_HEADSET_HANDOVER = 4;

    static final String BUNDLE_TRANSFER = "transfer";

    static final String EXTRA_HEADSET_DEVICE = "device";
    static final String EXTRA_HEADSET_NAME = "headsetname";

    static final String ACTION_CANCEL_HANDOVER_TRANSFER =
            "com.android.nfc.handover.action.CANCEL_HANDOVER_TRANSFER";

    static final String EXTRA_SOURCE_ADDRESS =
            "com.android.nfc.handover.extra.SOURCE_ADDRESS";

    static final String EXTRA_INCOMING =
            "com.android.nfc.handover.extra.INCOMING";

    static final String ACTION_HANDOVER_STARTED =
            "android.btopp.intent.action.BT_OPP_HANDOVER_STARTED";

    static final String ACTION_BT_OPP_TRANSFER_PROGRESS =
            "android.btopp.intent.action.BT_OPP_TRANSFER_PROGRESS";

    static final String ACTION_BT_OPP_TRANSFER_DONE =
            "android.btopp.intent.action.BT_OPP_TRANSFER_DONE";

    static final String EXTRA_BT_OPP_TRANSFER_STATUS =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_STATUS";

    static final String EXTRA_BT_OPP_TRANSFER_MIMETYPE =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_MIMETYPE";

    static final String EXTRA_BT_OPP_ADDRESS =
            "android.btopp.intent.extra.BT_OPP_ADDRESS";

    static final int HANDOVER_TRANSFER_STATUS_SUCCESS = 0;

    static final int HANDOVER_TRANSFER_STATUS_FAILURE = 1;

    static final String EXTRA_BT_OPP_TRANSFER_DIRECTION =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_DIRECTION";

    static final int DIRECTION_BLUETOOTH_INCOMING = 0;

    static final int DIRECTION_BLUETOOTH_OUTGOING = 1;

    static final String EXTRA_BT_OPP_TRANSFER_ID =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_ID";

    static final String EXTRA_BT_OPP_TRANSFER_PROGRESS =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_PROGRESS";

    static final String EXTRA_BT_OPP_TRANSFER_URI =
            "android.btopp.intent.extra.BT_OPP_TRANSFER_URI";

    public static final String EXTRA_BT_OPP_OBJECT_COUNT =
            "android.btopp.intent.extra.BT_OPP_OBJECT_COUNT";

    // permission needed to be able to receive handover status requests
    static final String HANDOVER_STATUS_PERMISSION =
            "com.android.permission.HANDOVER_STATUS";

    // Variables below only accessed on main thread
    final Queue<BluetoothOppHandover> mPendingOutTransfers;
    final HashMap<Pair<String, Boolean>, HandoverTransfer> mTransfers;
    final Messenger mMessenger;

    SoundPool mSoundPool;
    int mSuccessSound;

    BluetoothAdapter mBluetoothAdapter;
    Messenger mClient;
    Handler mHandler;
    BluetoothHeadsetHandover mBluetoothHeadsetHandover;
    boolean mBluetoothHeadsetConnected;
    boolean mBluetoothEnabledByNfc;

    public HandoverService() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPendingOutTransfers = new LinkedList<BluetoothOppHandover>();
        mTransfers = new HashMap<Pair<String, Boolean>, HandoverTransfer>();
        mHandler = new MessageHandler();
        mMessenger = new Messenger(mHandler);
        mBluetoothHeadsetConnected = false;
        mBluetoothEnabledByNfc = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSoundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        mSuccessSound = mSoundPool.load(this, R.raw.end, 1);

        IntentFilter filter = new IntentFilter(ACTION_BT_OPP_TRANSFER_DONE);
        filter.addAction(ACTION_BT_OPP_TRANSFER_PROGRESS);
        filter.addAction(ACTION_CANCEL_HANDOVER_TRANSFER);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(ACTION_HANDOVER_STARTED);
        registerReceiver(mReceiver, filter, HANDOVER_STATUS_PERMISSION, mHandler);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSoundPool != null) {
            mSoundPool.release();
        }
        unregisterReceiver(mReceiver);
    }

    void doOutgoingTransfer(Message msg) {
        Bundle msgData = msg.getData();

        msgData.setClassLoader(getClassLoader());
        PendingHandoverTransfer pendingTransfer = (PendingHandoverTransfer)
                msgData.getParcelable(BUNDLE_TRANSFER);
        createHandoverTransfer(pendingTransfer);

        // Create the actual transfer
        BluetoothOppHandover handover = new BluetoothOppHandover(HandoverService.this,
                pendingTransfer.remoteDevice, pendingTransfer.uris,
                pendingTransfer.remoteActivating);
        if (mBluetoothAdapter.isEnabled()) {
            // Start the transfer
            handover.start();
        } else {
            if (!enableBluetooth()) {
                Log.e(TAG, "Error enabling Bluetooth.");
                notifyClientTransferComplete(pendingTransfer.id);
                return;
            }
            if (DBG) Log.d(TAG, "Queueing out transfer " + Integer.toString(pendingTransfer.id));
            mPendingOutTransfers.add(handover);
            // Queue the transfer and enable Bluetooth - when it is enabled
            // the transfer will be started.
        }
    }

    void doIncomingTransfer(Message msg) {
        Bundle msgData = msg.getData();

        msgData.setClassLoader(getClassLoader());
        PendingHandoverTransfer pendingTransfer = (PendingHandoverTransfer)
                msgData.getParcelable(BUNDLE_TRANSFER);
        if (!mBluetoothAdapter.isEnabled() && !enableBluetooth()) {
            Log.e(TAG, "Error enabling Bluetooth.");
            notifyClientTransferComplete(pendingTransfer.id);
            return;
        }
        createHandoverTransfer(pendingTransfer);
        // Remote device will connect and finish the transfer
    }

    void doHeadsetHandover(Message msg) {
        Bundle msgData = msg.getData();
        BluetoothDevice device = (BluetoothDevice) msgData.getParcelable(EXTRA_HEADSET_DEVICE);
        String name = (String) msgData.getString(EXTRA_HEADSET_NAME);
        if (mBluetoothHeadsetHandover != null) {
           Log.d(TAG, "Ignoring pairing request, existing handover in progress.");
           return;
        }
        mBluetoothHeadsetHandover = new BluetoothHeadsetHandover(HandoverService.this,
                device, name, HandoverService.this);
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothHeadsetHandover.start();
        } else {
            // Once BT is enabled, the headset pairing will be started
            if (!enableBluetooth()) {
                Log.e(TAG, "Error enabling Bluetooth.");
                mBluetoothHeadsetHandover = null;
            }
        }
    }

    void startPendingTransfers() {
        while (!mPendingOutTransfers.isEmpty()) {
             BluetoothOppHandover handover = mPendingOutTransfers.remove();
             handover.start();
        }
    }

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    // Restore state from previous instance
                    mBluetoothEnabledByNfc = msg.arg1 != 0;
                    mBluetoothHeadsetConnected = msg.arg2 != 0;
                    break;
                case MSG_DEREGISTER_CLIENT:
                    mClient = null;
                    break;
                case MSG_START_INCOMING_TRANSFER:
                    doIncomingTransfer(msg);
                    break;
                case MSG_START_OUTGOING_TRANSFER:
                    doOutgoingTransfer(msg);
                    break;
                case MSG_HEADSET_HANDOVER:
                    doHeadsetHandover(msg);
                    break;
            }
        }
    }

    boolean enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothEnabledByNfc = true;
            return mBluetoothAdapter.enableNoAutoConnect();
        }
        return true;
    }

    void disableBluetoothIfNeeded() {
        if (!mBluetoothEnabledByNfc) return;

        if (mTransfers.size() == 0 && !mBluetoothHeadsetConnected) {
            mBluetoothAdapter.disable();
            mBluetoothEnabledByNfc = false;
        }
    }

    void createHandoverTransfer(PendingHandoverTransfer pendingTransfer) {
        Pair<String, Boolean> key = new Pair<String, Boolean>(
                pendingTransfer.remoteDevice.getAddress(), pendingTransfer.incoming);
        if (mTransfers.containsKey(key)) {
            HandoverTransfer transfer = mTransfers.get(key);
            if (!transfer.isRunning()) {
                mTransfers.remove(key); // new one created below
            } else {
                // There is already a transfer running to this
                // device - it will automatically get combined
                // with the existing transfer.
                notifyClientTransferComplete(pendingTransfer.id);
                return;
            }
        }

        HandoverTransfer transfer = new HandoverTransfer(this, this, pendingTransfer);
        mTransfers.put(key, transfer);
        transfer.updateNotification();
    }

    HandoverTransfer findHandoverTransfer(String sourceAddress, boolean incoming) {
        Pair<String, Boolean> key = new Pair<String, Boolean>(sourceAddress, incoming);
        if (mTransfers.containsKey(key)) {
            HandoverTransfer transfer = mTransfers.get(key);
            if (transfer.isRunning()) {
                return transfer;
            }
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
       return mMessenger.getBinder();
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    // If there is a pending headset pairing, start it
                    if (mBluetoothHeadsetHandover != null &&
                            !mBluetoothHeadsetHandover.hasStarted()) {
                        mBluetoothHeadsetHandover.start();
                    }

                    // Start any pending file transfers
                    startPendingTransfers();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    mBluetoothEnabledByNfc = false;
                    mBluetoothHeadsetConnected = false;
                }
            } else if (action.equals(ACTION_CANCEL_HANDOVER_TRANSFER)) {
                String sourceAddress = intent.getStringExtra(EXTRA_SOURCE_ADDRESS);
                boolean incoming = (intent.getIntExtra(EXTRA_INCOMING, 1)) == 1;
                HandoverTransfer transfer = findHandoverTransfer(sourceAddress, incoming);
                if (transfer != null) {
                    if (DBG) Log.d(TAG, "Cancelling transfer " +
                            Integer.toString(transfer.mTransferId));
                    transfer.cancel();
                }
            } else if (action.equals(ACTION_BT_OPP_TRANSFER_PROGRESS) ||
                    action.equals(ACTION_BT_OPP_TRANSFER_DONE) ||
                    action.equals(ACTION_HANDOVER_STARTED)) {
                int direction = intent.getIntExtra(EXTRA_BT_OPP_TRANSFER_DIRECTION, -1);
                int id = intent.getIntExtra(EXTRA_BT_OPP_TRANSFER_ID, -1);
                if (action.equals(ACTION_HANDOVER_STARTED)) {
                    // This is always for incoming transfers
                    direction = DIRECTION_BLUETOOTH_INCOMING;
                }
                String sourceAddress = intent.getStringExtra(EXTRA_BT_OPP_ADDRESS);

                if (direction == -1 || sourceAddress == null) return;
                boolean incoming = (direction == DIRECTION_BLUETOOTH_INCOMING);

                HandoverTransfer transfer = findHandoverTransfer(sourceAddress, incoming);
                if (transfer == null) {
                    // There is no transfer running for this source address; most likely
                    // the transfer was cancelled. We need to tell BT OPP to stop transferring.
                    if (id != -1) {
                        if (DBG) Log.d(TAG, "Didn't find transfer, stopping");
                        Intent cancelIntent = new Intent(
                                "android.btopp.intent.action.STOP_HANDOVER_TRANSFER");
                        cancelIntent.putExtra(EXTRA_BT_OPP_TRANSFER_ID, id);
                        sendBroadcast(cancelIntent);
                    }
                    return;
                }
                if (action.equals(ACTION_BT_OPP_TRANSFER_DONE)) {
                    int handoverStatus = intent.getIntExtra(EXTRA_BT_OPP_TRANSFER_STATUS,
                            HANDOVER_TRANSFER_STATUS_FAILURE);
                    if (handoverStatus == HANDOVER_TRANSFER_STATUS_SUCCESS) {
                        String uriString = intent.getStringExtra(EXTRA_BT_OPP_TRANSFER_URI);
                        String mimeType = intent.getStringExtra(EXTRA_BT_OPP_TRANSFER_MIMETYPE);
                        Uri uri = Uri.parse(uriString);
                        if (uri.getScheme() == null) {
                            uri = Uri.fromFile(new File(uri.getPath()));
                        }
                        transfer.finishTransfer(true, uri, mimeType);
                    } else {
                        transfer.finishTransfer(false, null, null);
                    }
                } else if (action.equals(ACTION_BT_OPP_TRANSFER_PROGRESS)) {
                    float progress = intent.getFloatExtra(EXTRA_BT_OPP_TRANSFER_PROGRESS, 0.0f);
                    transfer.updateFileProgress(progress);
                } else if (action.equals(ACTION_HANDOVER_STARTED)) {
                    int count = intent.getIntExtra(EXTRA_BT_OPP_OBJECT_COUNT, 0);
                    if (count > 0) {
                        transfer.setObjectCount(count);
                    }
                }
            }
        }
    };

    void notifyClientTransferComplete(int transferId) {
        if (mClient != null) {
            Message msg = Message.obtain(null, HandoverManager.MSG_HANDOVER_COMPLETE);
            msg.arg1 = transferId;
            try {
                mClient.send(msg);
            } catch (RemoteException e) {
                // Ignore
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // prevent any future callbacks to the client, no rebind call needed.
        mClient = null;
        return false;
    }

    @Override
    public void onTransferComplete(HandoverTransfer transfer, boolean success) {
        // Called on the main thread

        // First, remove the transfer from our list
        Iterator it = mTransfers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry hashPair = (Map.Entry)it.next();
            HandoverTransfer transferEntry = (HandoverTransfer) hashPair.getValue();
            if (transferEntry == transfer) {
                it.remove();
            }
        }

        // Notify any clients of the service
        notifyClientTransferComplete(transfer.getTransferId());

        // Play success sound
        if (success) {
            mSoundPool.play(mSuccessSound, 1.0f, 1.0f, 0, 0, 1.0f);
        } else {
            if (DBG) Log.d(TAG, "Transfer failed, final state: " +
                    Integer.toString(transfer.mState));
        }
        disableBluetoothIfNeeded();
    }

    @Override
    public void onBluetoothHeadsetHandoverComplete(boolean connected) {
        // Called on the main thread
        mBluetoothHeadsetHandover = null;
        mBluetoothHeadsetConnected = connected;
        if (mClient != null) {
            Message msg = Message.obtain(null,
                    connected ? HandoverManager.MSG_HEADSET_CONNECTED
                              : HandoverManager.MSG_HEADSET_NOT_CONNECTED);
            msg.arg1 = mBluetoothEnabledByNfc ? 1 : 0;
            try {
                mClient.send(msg);
            } catch (RemoteException e) {
                // Ignore
            }
        }
        disableBluetoothIfNeeded();
    }
}
