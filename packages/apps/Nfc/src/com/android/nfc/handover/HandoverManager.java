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

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

/**
 * Manages handover of NFC to other technologies.
 */
public class HandoverManager {
    static final String TAG = "NfcHandover";
    static final boolean DBG = true;

    static final String ACTION_WHITELIST_DEVICE =
            "android.btopp.intent.action.WHITELIST_DEVICE";

    static final byte[] TYPE_NOKIA = "nokia.com:bt".getBytes(Charset.forName("US_ASCII"));
    static final byte[] TYPE_BT_OOB = "application/vnd.bluetooth.ep.oob".
            getBytes(Charset.forName("US_ASCII"));

    static final byte[] RTD_COLLISION_RESOLUTION = {0x63, 0x72}; // "cr";

    static final int CARRIER_POWER_STATE_INACTIVE = 0;
    static final int CARRIER_POWER_STATE_ACTIVE = 1;
    static final int CARRIER_POWER_STATE_ACTIVATING = 2;
    static final int CARRIER_POWER_STATE_UNKNOWN = 3;

    static final int MSG_HANDOVER_COMPLETE = 0;
    static final int MSG_HEADSET_CONNECTED = 1;
    static final int MSG_HEADSET_NOT_CONNECTED = 2;

    final Context mContext;
    final BluetoothAdapter mBluetoothAdapter;
    final Messenger mMessenger = new Messenger (new MessageHandler());

    final Object mLock = new Object();
    // Variables below synchronized on mLock
    HashMap<Integer, PendingHandoverTransfer> mPendingTransfers;
    ArrayList<Message> mPendingServiceMessages;
    boolean mBluetoothHeadsetPending;
    boolean mBluetoothHeadsetConnected;
    boolean mBluetoothEnabledByNfc;
    int mHandoverTransferId;
    Messenger mService = null;
    boolean mBinding = false;
    boolean mBound;
    String mLocalBluetoothAddress;
    boolean mEnabled;

    static class BluetoothHandoverData {
        public boolean valid = false;
        public BluetoothDevice device;
        public String name;
        public boolean carrierActivating = false;
    }

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            synchronized (mLock) {
                switch (msg.what) {
                    case MSG_HANDOVER_COMPLETE:
                        int transferId = msg.arg1;
                        Log.d(TAG, "Completed transfer id: " + Integer.toString(transferId));
                        if (mPendingTransfers.containsKey(transferId)) {
                            mPendingTransfers.remove(transferId);
                        } else {
                            Log.e(TAG, "Could not find completed transfer id: " + Integer.toString(transferId));
                        }
                        break;
                    case MSG_HEADSET_CONNECTED:
                        mBluetoothEnabledByNfc = msg.arg1 != 0;
                        mBluetoothHeadsetConnected = true;
                        mBluetoothHeadsetPending = false;
                        break;
                    case MSG_HEADSET_NOT_CONNECTED:
                        mBluetoothEnabledByNfc = false; // No need to maintain this state any longer
                        mBluetoothHeadsetConnected = false;
                        mBluetoothHeadsetPending = false;
                        break;
                    default:
                        break;
                }
                unbindServiceIfNeededLocked(false);
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                mService = new Messenger(service);
                mBinding = false;
                mBound = true;
                // Register this client and transfer last known service state
                Message msg = Message.obtain(null, HandoverService.MSG_REGISTER_CLIENT);
                msg.arg1 = mBluetoothEnabledByNfc ? 1 : 0;
                msg.arg2 = mBluetoothHeadsetConnected ? 1 : 0;
                msg.replyTo = mMessenger;
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to register client");
                }
                // Send all queued messages
                while (!mPendingServiceMessages.isEmpty()) {
                    msg = mPendingServiceMessages.remove(0);
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to send queued message to service");
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                Log.d(TAG, "Service disconnected");
                if (mService != null) {
                    try {
                        Message msg = Message.obtain(null, HandoverService.MSG_DEREGISTER_CLIENT);
                        msg.replyTo = mMessenger;
                        mService.send(msg);
                    } catch (RemoteException e) {
                        // Service may have crashed - ignore
                    }
                }
                mService = null;
                mBound = false;
                mBluetoothHeadsetPending = false;
                mPendingTransfers.clear();
                mPendingServiceMessages.clear();
            }
        }
    };

    public HandoverManager(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mPendingTransfers = new HashMap<Integer, PendingHandoverTransfer>();
        mPendingServiceMessages = new ArrayList<Message>();

        IntentFilter filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(mReceiver, filter, null, null);

        mEnabled = true;
        mBluetoothEnabledByNfc = false;
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_SWITCHED)) {
                // Just force unbind the service.
                unbindServiceIfNeededLocked(true);
            }
        }
    };

    /**
     * @return whether the service was bound to successfully
     */
    boolean bindServiceIfNeededLocked() {
        if (!mBinding) {
            Log.d(TAG, "Binding to handover service");
            boolean bindSuccess = mContext.bindServiceAsUser(new Intent(mContext,
                    HandoverService.class), mConnection, Context.BIND_AUTO_CREATE,
                    UserHandle.CURRENT);
            mBinding = bindSuccess;
            return bindSuccess;
        } else {
           // A previous bind is pending
           return true;
        }
    }

    void unbindServiceIfNeededLocked(boolean force) {
        // If no service operation is pending, unbind
        if (mBound && (force || (!mBluetoothHeadsetPending && mPendingTransfers.isEmpty()))) {
            Log.d(TAG, "Unbinding from service.");
            mContext.unbindService(mConnection);
            mBound = false;
            mPendingServiceMessages.clear();
            mBluetoothHeadsetPending = false;
            mPendingTransfers.clear();
        }
        return;
    }

    static NdefRecord createCollisionRecord() {
        byte[] random = new byte[2];
        new Random().nextBytes(random);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, RTD_COLLISION_RESOLUTION, null, random);
    }

    NdefRecord createBluetoothAlternateCarrierRecord(boolean activating) {
        byte[] payload = new byte[4];
        payload[0] = (byte) (activating ? CARRIER_POWER_STATE_ACTIVATING :
            CARRIER_POWER_STATE_ACTIVE);  // Carrier Power State: Activating or active
        payload[1] = 1;   // length of carrier data reference
        payload[2] = 'b'; // carrier data reference: ID for Bluetooth OOB data record
        payload[3] = 0;  // Auxiliary data reference count
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_ALTERNATIVE_CARRIER, null, payload);
    }

    NdefRecord createBluetoothOobDataRecord() {
        byte[] payload = new byte[8];
        // Note: this field should be little-endian per the BTSSP spec
        // The Android 4.1 implementation used big-endian order here.
        // No single Android implementation has ever interpreted this
        // length field when parsing this record though.
        payload[0] = (byte) (payload.length & 0xFF);
        payload[1] = (byte) ((payload.length >> 8) & 0xFF);

        synchronized (mLock) {
            if (mLocalBluetoothAddress == null) {
                mLocalBluetoothAddress = mBluetoothAdapter.getAddress();
            }

            byte[] addressBytes = addressToReverseBytes(mLocalBluetoothAddress);
            System.arraycopy(addressBytes, 0, payload, 2, 6);
        }

        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, TYPE_BT_OOB, new byte[]{'b'}, payload);
    }

    public void setEnabled(boolean enabled) {
        synchronized (mLock) {
            mEnabled = enabled;
        }
    }
    public boolean isHandoverSupported() {
        return (mBluetoothAdapter != null);
    }

    public NdefMessage createHandoverRequestMessage() {
        if (mBluetoothAdapter == null) return null;

        return new NdefMessage(createHandoverRequestRecord(), createBluetoothOobDataRecord());
    }

    NdefMessage createHandoverSelectMessage(boolean activating) {
        return new NdefMessage(createHandoverSelectRecord(activating), createBluetoothOobDataRecord());
    }

    NdefRecord createHandoverSelectRecord(boolean activating) {
        NdefMessage nestedMessage = new NdefMessage(createBluetoothAlternateCarrierRecord(activating));
        byte[] nestedPayload = nestedMessage.toByteArray();

        ByteBuffer payload = ByteBuffer.allocate(nestedPayload.length + 1);
        payload.put((byte)0x12);  // connection handover v1.2
        payload.put(nestedPayload);

        byte[] payloadBytes = new byte[payload.position()];
        payload.position(0);
        payload.get(payloadBytes);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_HANDOVER_SELECT, null,
                payloadBytes);
    }

    NdefRecord createHandoverRequestRecord() {
        NdefMessage nestedMessage = new NdefMessage(createCollisionRecord(),
                createBluetoothAlternateCarrierRecord(false));
        byte[] nestedPayload = nestedMessage.toByteArray();

        ByteBuffer payload = ByteBuffer.allocate(nestedPayload.length + 1);
        payload.put((byte)0x12);  // connection handover v1.2
        payload.put(nestedMessage.toByteArray());

        byte[] payloadBytes = new byte[payload.position()];
        payload.position(0);
        payload.get(payloadBytes);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_HANDOVER_REQUEST, null,
                payloadBytes);
    }

    /**
     * Return null if message is not a Handover Request,
     * return the Handover Select response if it is.
     */
    public NdefMessage tryHandoverRequest(NdefMessage m) {
        if (m == null) return null;
        if (mBluetoothAdapter == null) return null;

        if (DBG) Log.d(TAG, "tryHandoverRequest():" + m.toString());

        NdefRecord r = m.getRecords()[0];
        if (r.getTnf() != NdefRecord.TNF_WELL_KNOWN) return null;
        if (!Arrays.equals(r.getType(), NdefRecord.RTD_HANDOVER_REQUEST)) return null;

        // we have a handover request, look for BT OOB record
        BluetoothHandoverData bluetoothData = null;
        for (NdefRecord oob : m.getRecords()) {
            if (oob.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                    Arrays.equals(oob.getType(), TYPE_BT_OOB)) {
                bluetoothData = parseBtOob(ByteBuffer.wrap(oob.getPayload()));
                break;
            }
        }
        if (bluetoothData == null) return null;

        // Note: there could be a race where we conclude
        // that Bluetooth is already enabled, and shortly
        // after the user turns it off. That will cause
        // the transfer to fail, but there's nothing
        // much we can do about it anyway. It shouldn't
        // be common for the user to be changing BT settings
        // while waiting to receive a picture.
        boolean bluetoothActivating = !mBluetoothAdapter.isEnabled();
        synchronized (mLock) {
            if (!mEnabled) return null;

            Message msg = Message.obtain(null, HandoverService.MSG_START_INCOMING_TRANSFER);
            PendingHandoverTransfer transfer = registerInTransferLocked(bluetoothData.device);
            Bundle transferData = new Bundle();
            transferData.putParcelable(HandoverService.BUNDLE_TRANSFER, transfer);
            msg.setData(transferData);

            if (!sendOrQueueMessageLocked(msg)) {
                removeTransferLocked(transfer.id);
                return null;
            }
        }
        // BT OOB found, whitelist it for incoming OPP data
        whitelistOppDevice(bluetoothData.device);

        // return BT OOB record so they can perform handover
        NdefMessage selectMessage = (createHandoverSelectMessage(bluetoothActivating));
        if (DBG) Log.d(TAG, "Waiting for incoming transfer, [" +
                bluetoothData.device.getAddress() + "]->[" + mLocalBluetoothAddress + "]");

        return selectMessage;
    }

    public boolean sendOrQueueMessageLocked(Message msg) {
        if (!mBound || mService == null) {
            // Need to start service, let us know if we can queue the message
            if (!bindServiceIfNeededLocked()) {
                Log.e(TAG, "Could not start service");
                return false;
            }
            // Queue the message to send when the service is bound
            mPendingServiceMessages.add(msg);
        } else {
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Could not connect to handover service");
                return false;
            }
        }
        return true;
    }

    public boolean tryHandover(NdefMessage m) {
        if (m == null) return false;
        if (mBluetoothAdapter == null) return false;

        if (DBG) Log.d(TAG, "tryHandover(): " + m.toString());

        BluetoothHandoverData handover = parse(m);
        if (handover == null) return false;
        if (!handover.valid) return true;

        synchronized (mLock) {
            if (!mEnabled) return false;

            if (mBluetoothAdapter == null) {
                if (DBG) Log.d(TAG, "BT handover, but BT not available");
                return true;
            }

            Message msg = Message.obtain(null, HandoverService.MSG_HEADSET_HANDOVER, 0, 0);
            Bundle headsetData = new Bundle();
            headsetData.putParcelable(HandoverService.EXTRA_HEADSET_DEVICE, handover.device);
            headsetData.putString(HandoverService.EXTRA_HEADSET_NAME, handover.name);
            msg.setData(headsetData);
            return sendOrQueueMessageLocked(msg);
        }
    }

    // This starts sending an Uri over BT
    public void doHandoverUri(Uri[] uris, NdefMessage m) {
        if (mBluetoothAdapter == null) return;

        BluetoothHandoverData data = parse(m);
        if (data != null && data.valid) {
            // Register a new handover transfer object
            synchronized (mLock) {
                Message msg = Message.obtain(null, HandoverService.MSG_START_OUTGOING_TRANSFER, 0, 0);
                PendingHandoverTransfer transfer = registerOutTransferLocked(data, uris);
                Bundle transferData = new Bundle();
                transferData.putParcelable(HandoverService.BUNDLE_TRANSFER, transfer);
                msg.setData(transferData);
                if (DBG) Log.d(TAG, "Initiating outgoing transfer, [" + mLocalBluetoothAddress +
                        "]->[" + data.device.getAddress() + "]");
                sendOrQueueMessageLocked(msg);
            }
        }
    }

    PendingHandoverTransfer registerInTransferLocked(BluetoothDevice remoteDevice) {
        PendingHandoverTransfer transfer = new PendingHandoverTransfer(
                mHandoverTransferId++, true, remoteDevice, false, null);
        mPendingTransfers.put(transfer.id, transfer);

        return transfer;
    }

    PendingHandoverTransfer registerOutTransferLocked(BluetoothHandoverData data,
            Uri[] uris) {
        PendingHandoverTransfer transfer = new PendingHandoverTransfer(
                mHandoverTransferId++, false, data.device, data.carrierActivating, uris);
        mPendingTransfers.put(transfer.id, transfer);
        return transfer;
    }

    void removeTransferLocked(int id) {
        mPendingTransfers.remove(id);
    }

    void whitelistOppDevice(BluetoothDevice device) {
        if (DBG) Log.d(TAG, "Whitelisting " + device + " for BT OPP");
        Intent intent = new Intent(ACTION_WHITELIST_DEVICE);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    boolean isCarrierActivating(NdefRecord handoverRec, byte[] carrierId) {
        byte[] payload = handoverRec.getPayload();
        if (payload == null || payload.length <= 1) return false;
        // Skip version
        byte[] payloadNdef = new byte[payload.length - 1];
        System.arraycopy(payload, 1, payloadNdef, 0, payload.length - 1);
        NdefMessage msg;
        try {
            msg = new NdefMessage(payloadNdef);
        } catch (FormatException e) {
            return false;
        }

        for (NdefRecord alt : msg.getRecords()) {
            byte[] acPayload = alt.getPayload();
            if (acPayload != null) {
                ByteBuffer buf = ByteBuffer.wrap(acPayload);
                int cps = buf.get() & 0x03; // Carrier Power State is in lower 2 bits
                int carrierRefLength = buf.get() & 0xFF;
                if (carrierRefLength != carrierId.length) return false;

                byte[] carrierRefId = new byte[carrierRefLength];
                buf.get(carrierRefId);
                if (Arrays.equals(carrierRefId, carrierId)) {
                    // Found match, returning whether power state is activating
                    return (cps == CARRIER_POWER_STATE_ACTIVATING);
                }
            }
        }

        return true;
    }

    BluetoothHandoverData parseHandoverSelect(NdefMessage m) {
        // TODO we could parse this a lot more strictly; right now
        // we just search for a BT OOB record, and try to cross-reference
        // the carrier state inside the 'hs' payload.
        for (NdefRecord oob : m.getRecords()) {
            if (oob.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                    Arrays.equals(oob.getType(), TYPE_BT_OOB)) {
                BluetoothHandoverData data = parseBtOob(ByteBuffer.wrap(oob.getPayload()));
                if (data != null && isCarrierActivating(m.getRecords()[0], oob.getId())) {
                    data.carrierActivating = true;
                }
                return data;
            }
        }

        return null;
    }

    BluetoothHandoverData parse(NdefMessage m) {
        NdefRecord r = m.getRecords()[0];
        short tnf = r.getTnf();
        byte[] type = r.getType();

        // Check for BT OOB record
        if (r.getTnf() == NdefRecord.TNF_MIME_MEDIA && Arrays.equals(r.getType(), TYPE_BT_OOB)) {
            return parseBtOob(ByteBuffer.wrap(r.getPayload()));
        }

        // Check for Handover Select, followed by a BT OOB record
        if (tnf == NdefRecord.TNF_WELL_KNOWN &&
                Arrays.equals(type, NdefRecord.RTD_HANDOVER_SELECT)) {
            return parseHandoverSelect(m);
        }

        // Check for Nokia BT record, found on some Nokia BH-505 Headsets
        if (tnf == NdefRecord.TNF_EXTERNAL_TYPE && Arrays.equals(type, TYPE_NOKIA)) {
            return parseNokia(ByteBuffer.wrap(r.getPayload()));
        }

        return null;
    }

    BluetoothHandoverData parseNokia(ByteBuffer payload) {
        BluetoothHandoverData result = new BluetoothHandoverData();
        result.valid = false;

        try {
            payload.position(1);
            byte[] address = new byte[6];
            payload.get(address);
            result.device = mBluetoothAdapter.getRemoteDevice(address);
            result.valid = true;
            payload.position(14);
            int nameLength = payload.get();
            byte[] nameBytes = new byte[nameLength];
            payload.get(nameBytes);
            result.name = new String(nameBytes, Charset.forName("UTF-8"));
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "nokia: invalid BT address");
        } catch (BufferUnderflowException e) {
            Log.i(TAG, "nokia: payload shorter than expected");
        }
        if (result.valid && result.name == null) result.name = "";
        return result;
    }

    BluetoothHandoverData parseBtOob(ByteBuffer payload) {
        BluetoothHandoverData result = new BluetoothHandoverData();
        result.valid = false;

        try {
            payload.position(2);
            byte[] address = new byte[6];
            payload.get(address);
            // ByteBuffer.order(LITTLE_ENDIAN) doesn't work for
            // ByteBuffer.get(byte[]), so manually swap order
            for (int i = 0; i < 3; i++) {
                byte temp = address[i];
                address[i] = address[5 - i];
                address[5 - i] = temp;
            }
            result.device = mBluetoothAdapter.getRemoteDevice(address);
            result.valid = true;

            while (payload.remaining() > 0) {
                byte[] nameBytes;
                int len = payload.get();
                int type = payload.get();
                switch (type) {
                    case 0x08:  // short local name
                        nameBytes = new byte[len - 1];
                        payload.get(nameBytes);
                        result.name = new String(nameBytes, Charset.forName("UTF-8"));
                        break;
                    case 0x09:  // long local name
                        if (result.name != null) break;  // prefer short name
                        nameBytes = new byte[len - 1];
                        payload.get(nameBytes);
                        result.name = new String(nameBytes, Charset.forName("UTF-8"));
                        break;
                    default:
                        payload.position(payload.position() + len - 1);
                        break;
                }
            }
        } catch (IllegalArgumentException e) {
            Log.i(TAG, "BT OOB: invalid BT address");
        } catch (BufferUnderflowException e) {
            Log.i(TAG, "BT OOB: payload shorter than expected");
        }
        if (result.valid && result.name == null) result.name = "";
        return result;
    }

    static byte[] addressToReverseBytes(String address) {
        String[] split = address.split(":");
        byte[] result = new byte[split.length];

        for (int i = 0; i < split.length; i++) {
            // need to parse as int because parseByte() expects a signed byte
            result[split.length - 1 - i] = (byte)Integer.parseInt(split[i], 16);
        }

        return result;
    }
}
