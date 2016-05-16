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

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.telephony.CallManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Dedicated Call state monitoring class.  This class communicates directly with
 * the call manager to listen for call state events and notifies registered
 * handlers.
 * It works as an inverse multiplexor for all classes wanted Call State updates
 * so that there exists only one channel to the telephony layer.
 *
 * TODO: Add manual phone state checks (getState(), etc.).
 */
class CallStateMonitor extends Handler {
    private static final String LOG_TAG = CallStateMonitor.class.getSimpleName();
    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);

    // Events from the Phone object:
    public static final int PHONE_STATE_CHANGED = 1;
    public static final int PHONE_NEW_RINGING_CONNECTION = 2;
    public static final int PHONE_DISCONNECT = 3;
    public static final int PHONE_UNKNOWN_CONNECTION_APPEARED = 4;
    public static final int PHONE_INCOMING_RING = 5;
    public static final int PHONE_STATE_DISPLAYINFO = 6;
    public static final int PHONE_STATE_SIGNALINFO = 7;
    public static final int PHONE_CDMA_CALL_WAITING = 8;
    public static final int PHONE_ENHANCED_VP_ON = 9;
    public static final int PHONE_ENHANCED_VP_OFF = 10;
    public static final int PHONE_RINGBACK_TONE = 11;
    public static final int PHONE_RESEND_MUTE = 12;
    public static final int PHONE_ON_DIAL_CHARS = 13;

    // Other events from call manager
    public static final int EVENT_OTA_PROVISION_CHANGE = 20;

    private CallManager callManager;
    private ArrayList<Handler> registeredHandlers;

    // Events generated internally:
    public CallStateMonitor(CallManager callManager) {
        this.callManager = callManager;
        registeredHandlers = new ArrayList<Handler>();

        registerForNotifications();
    }

    /**
     * Register for call state notifications with the CallManager.
     */
    private void registerForNotifications() {
        callManager.registerForNewRingingConnection(this, PHONE_NEW_RINGING_CONNECTION, null);
        callManager.registerForPreciseCallStateChanged(this, PHONE_STATE_CHANGED, null);
        callManager.registerForDisconnect(this, PHONE_DISCONNECT, null);
        callManager.registerForUnknownConnection(this, PHONE_UNKNOWN_CONNECTION_APPEARED, null);
        callManager.registerForIncomingRing(this, PHONE_INCOMING_RING, null);
        callManager.registerForCdmaOtaStatusChange(this, EVENT_OTA_PROVISION_CHANGE, null);
        callManager.registerForCallWaiting(this, PHONE_CDMA_CALL_WAITING, null);
        callManager.registerForDisplayInfo(this, PHONE_STATE_DISPLAYINFO, null);
        callManager.registerForSignalInfo(this, PHONE_STATE_SIGNALINFO, null);
        callManager.registerForInCallVoicePrivacyOn(this, PHONE_ENHANCED_VP_ON, null);
        callManager.registerForInCallVoicePrivacyOff(this, PHONE_ENHANCED_VP_OFF, null);
        callManager.registerForRingbackTone(this, PHONE_RINGBACK_TONE, null);
        callManager.registerForResendIncallMute(this, PHONE_RESEND_MUTE, null);
        callManager.registerForPostDialCharacter(this, PHONE_ON_DIAL_CHARS, null);
    }

    public void addListener(Handler handler) {
        if (handler != null && !registeredHandlers.contains(handler)) {
            if (DBG) {
                Log.d(LOG_TAG, "Adding Handler: " + handler);
            }
            registeredHandlers.add(handler);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (DBG) {
            Log.d(LOG_TAG, "handleMessage(" + msg.what + ")");
        }

        for (Handler handler : registeredHandlers) {
            handler.handleMessage(msg);
        }
    }

    /**
     * When radio technology changes, we need to to reregister for all the events which are
     * all tied to the old radio.
     */
    public void updateAfterRadioTechnologyChange() {
        if (DBG) Log.d(LOG_TAG, "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");

        // Unregister all events from the old obsolete phone
        callManager.unregisterForNewRingingConnection(this);
        callManager.unregisterForPreciseCallStateChanged(this);
        callManager.unregisterForDisconnect(this);
        callManager.unregisterForUnknownConnection(this);
        callManager.unregisterForIncomingRing(this);
        callManager.unregisterForCallWaiting(this);
        callManager.unregisterForDisplayInfo(this);
        callManager.unregisterForSignalInfo(this);
        callManager.unregisterForCdmaOtaStatusChange(this);
        callManager.unregisterForRingbackTone(this);
        callManager.unregisterForResendIncallMute(this);
        callManager.unregisterForInCallVoicePrivacyOn(this);
        callManager.unregisterForInCallVoicePrivacyOff(this);
        callManager.unregisterForPostDialCharacter(this);

        registerForNotifications();
    }

}
