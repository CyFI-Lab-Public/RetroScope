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

package com.android.nfc.cardemulation;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.cardemulation.ApduServiceInfo;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import com.android.nfc.NfcService;
import com.android.nfc.cardemulation.RegisteredAidCache.AidResolveInfo;

import java.util.ArrayList;

public class HostEmulationManager {
    static final String TAG = "HostEmulationManager";
    static final boolean DBG = false;

    static final int STATE_IDLE = 0;
    static final int STATE_W4_SELECT = 1;
    static final int STATE_W4_SERVICE = 2;
    static final int STATE_W4_DEACTIVATE = 3;
    static final int STATE_XFER = 4;

    /** Minimum AID lenth as per ISO7816 */
    static final int MINIMUM_AID_LENGTH = 5;

    /** Length of Select APDU header including length byte */
    static final int SELECT_APDU_HDR_LENGTH = 5;

    static final byte INSTR_SELECT = (byte)0xA4;

    static final String ANDROID_HCE_AID = "A000000476416E64726F6964484345";
    static final byte[] ANDROID_HCE_RESPONSE = {0x14, (byte)0x81, 0x00, 0x00, (byte)0x90, 0x00};

    static final byte[] AID_NOT_FOUND = {0x6A, (byte)0x82};
    static final byte[] UNKNOWN_ERROR = {0x6F, 0x00};

    final Context mContext;
    final RegisteredAidCache mAidCache;
    final Messenger mMessenger = new Messenger (new MessageHandler());
    final KeyguardManager mKeyguard;
    final Object mLock;

    // All variables below protected by mLock

    /** Whether we need to clear the "next tap" service at the end
     *  of this transaction.
     */
    boolean mClearNextTapDefault;

    // Variables below are for a non-payment service,
    // that is typically only bound in the STATE_XFER state.
    Messenger mService;
    boolean mServiceBound;
    ComponentName mServiceName;

    // Variables below are for a payment service,
    // which is typically bound persistently to improve on
    // latency.
    Messenger mPaymentService;
    boolean mPaymentServiceBound;
    ComponentName mPaymentServiceName;

    // mActiveService denotes the service interface
    // that is the current active one, until a new SELECT AID
    // comes in that may be resolved to a different service.
    // On deactivation, mActiveService stops being valid.
    Messenger mActiveService;
    ComponentName mActiveServiceName;

    String mLastSelectedAid;
    int mState;
    byte[] mSelectApdu;

    public HostEmulationManager(Context context, RegisteredAidCache aidCache) {
        mContext = context;
        mLock = new Object();
        mAidCache = aidCache;
        mState = STATE_IDLE;
        mKeyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        SettingsObserver observer = new SettingsObserver(mHandler);
        context.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT),
                true, observer, UserHandle.USER_ALL);

        // Bind to payment default if existing
        int userId = ActivityManager.getCurrentUser();
        String name = Settings.Secure.getStringForUser(
                mContext.getContentResolver(), Settings.Secure.NFC_PAYMENT_DEFAULT_COMPONENT,
                userId);
        if (name != null) {
            ComponentName serviceComponent = ComponentName.unflattenFromString(name);
            bindPaymentServiceLocked(userId, serviceComponent);
        }
    }

    public void setDefaultForNextTap(ComponentName service) {
        synchronized (mLock) {
            if (service != null) {
                bindServiceIfNeededLocked(service);
            } else {
                unbindServiceIfNeededLocked();
            }
        }
    }

    public void notifyHostEmulationActivated() {
        Log.d(TAG, "notifyHostEmulationActivated");
        synchronized (mLock) {
            mClearNextTapDefault = mAidCache.isNextTapOverriden();
            // Regardless of what happens, if we're having a tap again
            // activity up, close it
            Intent intent = new Intent(TapAgainDialog.ACTION_CLOSE);
            intent.setPackage("com.android.nfc");
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            if (mState != STATE_IDLE) {
                Log.e(TAG, "Got activation event in non-idle state");
            }
            mState = STATE_W4_SELECT;
        }
    }

    public void notifyHostEmulationData(byte[] data) {
        Log.d(TAG, "notifyHostEmulationData");
        String selectAid = findSelectAid(data);
        ComponentName resolvedService = null;
        synchronized (mLock) {
            if (mState == STATE_IDLE) {
                Log.e(TAG, "Got data in idle state.");
                return;
            } else if (mState == STATE_W4_DEACTIVATE) {
                Log.e(TAG, "Dropping APDU in STATE_W4_DECTIVATE");
                return;
            }
            if (selectAid != null) {
                if (selectAid.equals(ANDROID_HCE_AID)) {
                    NfcService.getInstance().sendData(ANDROID_HCE_RESPONSE);
                    return;
                }
                AidResolveInfo resolveInfo = mAidCache.resolveAidPrefix(selectAid);
                if (resolveInfo == null || resolveInfo.services.size() == 0) {
                    // Tell the remote we don't handle this AID
                    NfcService.getInstance().sendData(AID_NOT_FOUND);
                    return;
                }
                mLastSelectedAid = resolveInfo.aid;
                if (resolveInfo.defaultService != null) {
                    // Resolve to default
                    // Check if resolvedService requires unlock
                    if (resolveInfo.defaultService.requiresUnlock() &&
                            mKeyguard.isKeyguardLocked() && mKeyguard.isKeyguardSecure()) {
                        String category = mAidCache.getCategoryForAid(resolveInfo.aid);
                        // Just ignore all future APDUs until next tap
                        mState = STATE_W4_DEACTIVATE;
                        launchTapAgain(resolveInfo.defaultService, category);
                        return;
                    }
                    // In no circumstance should this be an OffHostService -
                    // we should never get this AID on the host in the first place
                    if (!resolveInfo.defaultService.isOnHost()) {
                        Log.e(TAG, "AID that was meant to go off-host was routed to host." +
                                " Check routing table configuration.");
                        NfcService.getInstance().sendData(AID_NOT_FOUND);
                        return;
                    }
                    resolvedService = resolveInfo.defaultService.getComponent();
                } else if (mActiveServiceName != null) {
                    for (ApduServiceInfo service : resolveInfo.services) {
                        if (mActiveServiceName.equals(service.getComponent())) {
                            resolvedService = mActiveServiceName;
                            break;
                        }
                    }
                }
                if (resolvedService == null) {
                    // We have no default, and either one or more services.
                    // Ask the user to confirm.
                    // Get corresponding category
                    String category = mAidCache.getCategoryForAid(resolveInfo.aid);
                    // Just ignore all future APDUs until we resolve to only one
                    mState = STATE_W4_DEACTIVATE;
                    launchResolver((ArrayList<ApduServiceInfo>)resolveInfo.services, null, category);
                    return;
                }
            }
            switch (mState) {
            case STATE_W4_SELECT:
                if (selectAid != null) {
                    Messenger existingService = bindServiceIfNeededLocked(resolvedService);
                    if (existingService != null) {
                        Log.d(TAG, "Binding to existing service");
                        mState = STATE_XFER;
                        sendDataToServiceLocked(existingService, data);
                    } else {
                        // Waiting for service to be bound
                        Log.d(TAG, "Waiting for new service.");
                        // Queue SELECT APDU to be used
                        mSelectApdu = data;
                        mState = STATE_W4_SERVICE;
                    }
                } else {
                    Log.d(TAG, "Dropping non-select APDU in STATE_W4_SELECT");
                    NfcService.getInstance().sendData(UNKNOWN_ERROR);
                }
                break;
            case STATE_W4_SERVICE:
                Log.d(TAG, "Unexpected APDU in STATE_W4_SERVICE");
                break;
            case STATE_XFER:
                if (selectAid != null) {
                    Messenger existingService = bindServiceIfNeededLocked(resolvedService);
                    if (existingService != null) {
                        sendDataToServiceLocked(existingService, data);
                        mState = STATE_XFER;
                    } else {
                        // Waiting for service to be bound
                        mSelectApdu = data;
                        mState = STATE_W4_SERVICE;
                    }
                } else if (mActiveService != null) {
                    // Regular APDU data
                    sendDataToServiceLocked(mActiveService, data);
                } else {
                    // No SELECT AID and no active service.
                    Log.d(TAG, "Service no longer bound, dropping APDU");
                }
                break;
            }
        }
    }

    public void notifyNostEmulationDeactivated() {
        Log.d(TAG, "notifyHostEmulationDeactivated");
        synchronized (mLock) {
            if (mState == STATE_IDLE) {
                Log.e(TAG, "Got deactivation event while in idle state");
            }
            if (mClearNextTapDefault) {
                mAidCache.setDefaultForNextTap(ActivityManager.getCurrentUser(), null);
            }
            sendDeactivateToActiveServiceLocked(HostApduService.DEACTIVATION_LINK_LOSS);
            mActiveService = null;
            mActiveServiceName = null;
            unbindServiceIfNeededLocked();
            mState = STATE_IDLE;
        }
    }

    public void notifyOffHostAidSelected() {
        Log.d(TAG, "notifyOffHostAidSelected");
        synchronized (mLock) {
            if (mState != STATE_XFER || mActiveService == null) {
                // Don't bother telling, we're not bound to any service yet
            } else {
                sendDeactivateToActiveServiceLocked(HostApduService.DEACTIVATION_DESELECTED);
            }
            mActiveService = null;
            mActiveServiceName = null;
            unbindServiceIfNeededLocked();
            mState = STATE_W4_SELECT;
        }
    }

    Messenger bindServiceIfNeededLocked(ComponentName service) {
        if (mPaymentServiceBound && mPaymentServiceName.equals(service)) {
            Log.d(TAG, "Service already bound as payment service.");
            return mPaymentService;
        } else if (mServiceBound && mServiceName.equals(service)) {
            Log.d(TAG, "Service already bound as regular service.");
            return mService;
        } else {
            Log.d(TAG, "Binding to service " + service);
            unbindServiceIfNeededLocked();
            Intent aidIntent = new Intent(HostApduService.SERVICE_INTERFACE);
            aidIntent.setComponent(service);
            if (mContext.bindServiceAsUser(aidIntent, mConnection,
                    Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
            } else {
                Log.e(TAG, "Could not bind service.");
            }
            return null;
        }
    }

    void sendDataToServiceLocked(Messenger service, byte[] data) {
        if (service != mActiveService) {
            sendDeactivateToActiveServiceLocked(HostApduService.DEACTIVATION_DESELECTED);
            mActiveService = service;
            if (service.equals(mPaymentService)) {
                mActiveServiceName = mPaymentServiceName;
            } else {
                mActiveServiceName = mServiceName;
            }
        }
        Message msg = Message.obtain(null, HostApduService.MSG_COMMAND_APDU);
        Bundle dataBundle = new Bundle();
        dataBundle.putByteArray("data", data);
        msg.setData(dataBundle);
        msg.replyTo = mMessenger;
        try {
            mActiveService.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote service has died, dropping APDU");
        }
    }

    void sendDeactivateToActiveServiceLocked(int reason) {
        if (mActiveService == null) return;
        Message msg = Message.obtain(null, HostApduService.MSG_DEACTIVATED);
        msg.arg1 = reason;
        try {
            mActiveService.send(msg);
        } catch (RemoteException e) {
            // Don't care
        }
    }

    final Handler mHandler = new Handler(Looper.getMainLooper());

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            synchronized (mLock) {
                // Do it just for the current user. If it was in fact
                // a change made for another user, we'll sync it down
                // on user switch.
                int userId = ActivityManager.getCurrentUser();
                ComponentName paymentApp = mAidCache.getDefaultServiceForCategory(userId,
                        CardEmulation.CATEGORY_PAYMENT, true);
                if (paymentApp != null) {
                    bindPaymentServiceLocked(userId, paymentApp);
                } else {
                    unbindPaymentServiceLocked(userId);
                }
            }
        }
    };

    void unbindPaymentServiceLocked(int userId) {
        if (mPaymentServiceBound) {
            mContext.unbindService(mPaymentConnection);
            mPaymentServiceBound = false;
            mPaymentService = null;
            mPaymentServiceName = null;
        }
    }

    void bindPaymentServiceLocked(int userId, ComponentName service) {
        unbindPaymentServiceLocked(userId);

        Intent intent = new Intent(HostApduService.SERVICE_INTERFACE);
        intent.setComponent(service);
        if (!mContext.bindServiceAsUser(intent, mPaymentConnection,
                Context.BIND_AUTO_CREATE, new UserHandle(userId))) {
            Log.e(TAG, "Could not bind (persistent) payment service.");
        }
    }

    void unbindServiceIfNeededLocked() {
        if (mServiceBound) {
            Log.d(TAG, "Unbinding from service " + mServiceName);
            mContext.unbindService(mConnection);
            mServiceBound = false;
            mService = null;
            mServiceName = null;
        }
    }

    void launchTapAgain(ApduServiceInfo service, String category) {
        Intent dialogIntent = new Intent(mContext, TapAgainDialog.class);
        dialogIntent.putExtra(TapAgainDialog.EXTRA_CATEGORY, category);
        dialogIntent.putExtra(TapAgainDialog.EXTRA_APDU_SERVICE, service);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivityAsUser(dialogIntent, UserHandle.CURRENT);
    }

    void launchResolver(ArrayList<ApduServiceInfo> services, ComponentName failedComponent,
            String category) {
        Intent intent = new Intent(mContext, AppChooserActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putParcelableArrayListExtra(AppChooserActivity.EXTRA_APDU_SERVICES, services);
        intent.putExtra(AppChooserActivity.EXTRA_CATEGORY, category);
        if (failedComponent != null) {
            intent.putExtra(AppChooserActivity.EXTRA_FAILED_COMPONENT, failedComponent);
        }
        mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    String findSelectAid(byte[] data) {
        if (data == null || data.length < SELECT_APDU_HDR_LENGTH + MINIMUM_AID_LENGTH) {
            if (DBG) Log.d(TAG, "Data size too small for SELECT APDU");
            return null;
        }
        // To accept a SELECT AID for dispatch, we require the following:
        // Class byte must be 0x00: logical channel set to zero, no secure messaging, no chaining
        // Instruction byte must be 0xA4: SELECT instruction
        // P1: must be 0x04: select by application identifier
        // P2: File control information is only relevant for higher-level application,
        //     and we only support "first or only occurrence".
        if (data[0] == 0x00 && data[1] == INSTR_SELECT && data[2] == 0x04) {
            if (data[3] != 0x00) {
                Log.d(TAG, "Selecting next, last or previous AID occurrence is not supported");
            }
            int aidLength = data[4];
            if (data.length < SELECT_APDU_HDR_LENGTH + aidLength) {
                return null;
            }
            return bytesToString(data, SELECT_APDU_HDR_LENGTH, aidLength);
        }
        return null;
    }

    private ServiceConnection mPaymentConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                mPaymentServiceName = name;
                mPaymentService = new Messenger(service);
                mPaymentServiceBound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                mPaymentService = null;
                mPaymentServiceBound = false;
                mPaymentServiceName = null;
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (mLock) {
                mService = new Messenger(service);
                mServiceBound = true;
                mServiceName = name;
                Log.d(TAG, "Service bound");
                mState = STATE_XFER;
                // Send pending select APDU
                if (mSelectApdu != null) {
                    sendDataToServiceLocked(mService, mSelectApdu);
                    mSelectApdu = null;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                Log.d(TAG, "Service unbound");
                mService = null;
                mServiceBound = false;
            }
        }
    };

    class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            synchronized(mLock) {
                if (mActiveService == null) {
                    Log.d(TAG, "Dropping service response message; service no longer active.");
                    return;
                } else if (!msg.replyTo.getBinder().equals(mActiveService.getBinder())) {
                    Log.d(TAG, "Dropping service response message; service no longer bound.");
                    return;
                }
            }
            if (msg.what == HostApduService.MSG_RESPONSE_APDU) {
                Bundle dataBundle = msg.getData();
                if (dataBundle == null) {
                    return;
                }
                byte[] data = dataBundle.getByteArray("data");
                if (data == null || data.length == 0) {
                    Log.e(TAG, "Dropping empty R-APDU");
                    return;
                }
                int state;
                synchronized(mLock) {
                    state = mState;
                }
                if (state == STATE_XFER) {
                    Log.d(TAG, "Sending data");
                    NfcService.getInstance().sendData(data);
                } else {
                    Log.d(TAG, "Dropping data, wrong state " + Integer.toString(state));
                }
            } else if (msg.what == HostApduService.MSG_UNHANDLED) {
                synchronized (mLock) {
                    AidResolveInfo resolveInfo = mAidCache.resolveAidPrefix(mLastSelectedAid);
                    String category = mAidCache.getCategoryForAid(mLastSelectedAid);
                    if (resolveInfo.services.size() > 0) {
                        final ArrayList<ApduServiceInfo> services = new ArrayList<ApduServiceInfo>();
                        for (ApduServiceInfo service : resolveInfo.services) {
                            if (!service.getComponent().equals(mActiveServiceName)) {
                                services.add(service);
                            }
                        }
                        launchResolver(services, mActiveServiceName, category);
                    }
                }
            }
        }
    }

    static String bytesToString(byte[] bytes, int offset, int length) {
        final char[] hexChars = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] chars = new char[length * 2];
        int byteValue;
        for (int j = 0; j < length; j++) {
            byteValue = bytes[offset + j] & 0xFF;
            chars[j * 2] = hexChars[byteValue >>> 4];
            chars[j * 2 + 1] = hexChars[byteValue & 0x0F];
        }
        return new String(chars);
    }
}
