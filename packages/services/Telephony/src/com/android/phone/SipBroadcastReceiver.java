/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.sip.SipProfileDb;
import com.android.phone.sip.SipSharedPreferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.telephony.Rlog;
import java.util.List;

/**
 * Broadcast receiver that handles SIP-related intents.
 */
public class SipBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = SipBroadcastReceiver.class.getSimpleName();
    private static final boolean DBG = true;
    private SipSharedPreferences mSipSharedPreferences;

    @Override
    public void onReceive(Context context, final Intent intent) {
        String action = intent.getAction();

        if (!PhoneUtils.isVoipSupported()) {
            if (DBG) log("SIP VOIP not supported: " + action);
            return;
        }
        mSipSharedPreferences = new SipSharedPreferences(context);

        if (action.equals(SipManager.ACTION_SIP_INCOMING_CALL)) {
            takeCall(intent);
        } else if (action.equals(SipManager.ACTION_SIP_ADD_PHONE)) {
            String localSipUri = intent.getStringExtra(SipManager.EXTRA_LOCAL_URI);
            SipPhone phone = PhoneFactory.makeSipPhone(localSipUri);
            if (phone != null) {
                CallManager.getInstance().registerPhone(phone);
            }
            if (DBG) log("onReceive: add phone" + localSipUri + " #phones="
                    + CallManager.getInstance().getAllPhones().size());
        } else if (action.equals(SipManager.ACTION_SIP_REMOVE_PHONE)) {
            String localSipUri = intent.getStringExtra(SipManager.EXTRA_LOCAL_URI);
            removeSipPhone(localSipUri);
            if (DBG) log("onReceive: remove phone: " + localSipUri + " #phones="
                    + CallManager.getInstance().getAllPhones().size());
        } else if (action.equals(SipManager.ACTION_SIP_SERVICE_UP)) {
            if (DBG) log("onReceive: start auto registration");
            registerAllProfiles();
        } else {
            if (DBG) log("onReceive: action not processed: " + action);
            return;
        }
    }

    private void removeSipPhone(String sipUri) {
        for (Phone phone : CallManager.getInstance().getAllPhones()) {
            if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                if (((SipPhone) phone).getSipUri().equals(sipUri)) {
                    CallManager.getInstance().unregisterPhone(phone);
                    return;
                }
            }
        }
        if (DBG) log("RemoveSipPhone: failed:cannot find phone with uri " + sipUri);
    }

    private void takeCall(Intent intent) {
        Context phoneContext = PhoneGlobals.getInstance();
        try {
            SipAudioCall sipAudioCall = SipManager.newInstance(phoneContext)
                    .takeAudioCall(intent, null);
            for (Phone phone : CallManager.getInstance().getAllPhones()) {
                if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                   if (((SipPhone) phone).canTake(sipAudioCall)) {
                       if (DBG) log("takeCall: SIP call: " + intent);
                       return;
                   }
                }
            }
            if (DBG) log("takeCall: not taken, drop SIP call: " + intent);
        } catch (SipException e) {
            loge("takeCall: error incoming SIP call", e);
        }
    }

    private void registerAllProfiles() {
        final Context context = PhoneGlobals.getInstance();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SipManager sipManager = SipManager.newInstance(context);
                SipProfileDb profileDb = new SipProfileDb(context);
                List<SipProfile> sipProfileList =
                        profileDb.retrieveSipProfileList();
                for (SipProfile profile : sipProfileList) {
                    try {
                        if (!profile.getAutoRegistration() &&
                                !profile.getUriString().equals(
                                mSipSharedPreferences.getPrimaryAccount())) {
                            continue;
                        }
                        sipManager.open(profile,
                                SipUtil.createIncomingCallPendingIntent(),
                                null);
                        if (DBG) log("registerAllProfiles: profile=" + profile);
                    } catch (SipException e) {
                        loge("registerAllProfiles: failed" + profile.getProfileName(), e);
                    }
                }
            }}
        ).start();
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }
}
