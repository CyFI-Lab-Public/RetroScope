/* 
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.internal.telephony.cdma;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.Rlog;

import com.android.internal.telephony.CommandsInterface;

import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.SMSDispatcher;
import com.android.internal.telephony.SmsBroadcastUndelivered;
import com.android.internal.telephony.gsm.GsmSMSDispatcher;
import com.android.internal.telephony.gsm.SmsMessage;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class CDMALTEPhone extends CDMAPhone {
    static final String LOG_LTE_TAG = "CDMALTEPhone";
    private static final boolean DBG = true;

    /** CdmaLtePhone in addition to RuimRecords available from
     * PhoneBase needs access to SIMRecords and IsimUiccRecords
     */
    private SIMRecords mSimRecords;
    private IsimUiccRecords mIsimUiccRecords;

    /**
     * Small container class used to hold information relevant to
     * the carrier selection process. operatorNumeric can be ""
     * if we are looking for automatic selection. operatorAlphaLong is the
     * corresponding operator name.
     */
    private static class NetworkSelectMessage {
        public Message message;
        public String operatorNumeric;
        public String operatorAlphaLong;
    }

    // Constructors
    public CDMALTEPhone(Context context, CommandsInterface ci, PhoneNotifier notifier) {
        super(context, ci, notifier, false);
    }

    @Override
    public void handleMessage (Message msg) {
        switch (msg.what) {
            // handle the select network completion callbacks.
            case EVENT_SET_NETWORK_MANUAL_COMPLETE:
                handleSetSelectNetwork((AsyncResult) msg.obj);
                break;

            default:
                super.handleMessage(msg);
        }
    }

    @Override
    protected void initSstIcc() {
        mSST = new CdmaLteServiceStateTracker(this);
    }

    @Override
    public void dispose() {
        synchronized(PhoneProxy.lockForRadioTechnologyChange) {
            super.dispose();
        }
    }

    @Override
    public void removeReferences() {
        super.removeReferences();
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;

        if (mSST == null) {
            // Radio Technology Change is ongoing, dispose() and
            // removeReferences() have already been called

            ret = PhoneConstants.DataState.DISCONNECTED;
        } else if (mDcTracker.isApnTypeEnabled(apnType) == false) {
            ret = PhoneConstants.DataState.DISCONNECTED;
        } else {
            switch (mDcTracker.getState(apnType)) {
                case RETRYING:
                case FAILED:
                case IDLE:
                    ret = PhoneConstants.DataState.DISCONNECTED;
                    break;

                case CONNECTED:
                case DISCONNECTING:
                    if (mCT.mState != PhoneConstants.State.IDLE &&
                            !mSST.isConcurrentVoiceAndDataAllowed()) {
                        ret = PhoneConstants.DataState.SUSPENDED;
                    } else {
                        ret = PhoneConstants.DataState.CONNECTED;
                    }
                    break;

                case CONNECTING:
                case SCANNING:
                    ret = PhoneConstants.DataState.CONNECTING;
                    break;
            }
        }

        log("getDataConnectionState apnType=" + apnType + " ret=" + ret);
        return ret;
    }

    @Override
    public void
    selectNetworkManually(OperatorInfo network,
            Message response) {
        // wrap the response message in our own message along with
        // the operator's id.
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = network.getOperatorNumeric();
        nsm.operatorAlphaLong = network.getOperatorAlphaLong();

        // get the message
        Message msg = obtainMessage(EVENT_SET_NETWORK_MANUAL_COMPLETE, nsm);

        mCi.setNetworkSelectionModeManual(network.getOperatorNumeric(), msg);
    }

    /**
     * Used to track the settings upon completion of the network change.
     */
    private void handleSetSelectNetwork(AsyncResult ar) {
        // look for our wrapper within the asyncresult, skip the rest if it
        // is null.
        if (!(ar.userObj instanceof NetworkSelectMessage)) {
            loge("unexpected result from user object.");
            return;
        }

        NetworkSelectMessage nsm = (NetworkSelectMessage) ar.userObj;

        // found the object, now we send off the message we had originally
        // attached to the request.
        if (nsm.message != null) {
            if (DBG) log("sending original message to recipient");
            AsyncResult.forMessage(nsm.message, ar.result, ar.exception);
            nsm.message.sendToTarget();
        }

        // open the shared preferences editor, and write the value.
        // nsm.operatorNumeric is "" if we're in automatic.selection.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(NETWORK_SELECTION_KEY, nsm.operatorNumeric);
        editor.putString(NETWORK_SELECTION_NAME_KEY, nsm.operatorAlphaLong);

        // commit and log the result.
        if (! editor.commit()) {
            loge("failed to commit network selection preference");
        }

    }


    /**
     * Sets the "current" field in the telephony provider according to the
     * build-time operator numeric property
     *
     * @return true for success; false otherwise.
     */
    @Override
    boolean updateCurrentCarrierInProvider(String operatorNumeric) {
        boolean retVal;
        if (mUiccController.getUiccCardApplication(UiccController.APP_FAM_3GPP) == null) {
            if (DBG) log("updateCurrentCarrierInProvider APP_FAM_3GPP == null");
            retVal = super.updateCurrentCarrierInProvider(operatorNumeric);
        } else {
            if (DBG) log("updateCurrentCarrierInProvider not updated");
            retVal = true;
        }
        if (DBG) log("updateCurrentCarrierInProvider X retVal=" + retVal);
        return retVal;
    }

    @Override
    public boolean updateCurrentCarrierInProvider() {
        if (mSimRecords != null) {
            try {
                Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                ContentValues map = new ContentValues();
                String operatorNumeric = mSimRecords.getOperatorNumeric();
                map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                if (DBG) log("updateCurrentCarrierInProvider from UICC: numeric=" +
                        operatorNumeric);
                mContext.getContentResolver().insert(uri, map);
                return true;
            } catch (SQLException e) {
                loge("Can't store current operator ret false", e);
            }
        } else {
            if (DBG) log("updateCurrentCarrierInProvider mIccRecords == null ret false");
        }
        return false;
    }

    // return IMSI from USIM as subscriber ID.
    @Override
    public String getSubscriberId() {
        return (mSimRecords != null) ? mSimRecords.getIMSI() : "";
    }

    // return GID1 from USIM
    @Override
    public String getGroupIdLevel1() {
        return (mSimRecords != null) ? mSimRecords.getGid1() : "";
    }

    @Override
    public String getImei() {
        return mImei;
    }

    @Override
    public String getDeviceSvn() {
        return mImeiSv;
    }

    @Override
    public IsimRecords getIsimRecords() {
        return mIsimUiccRecords;
    }

    @Override
    public String getMsisdn() {
        return (mSimRecords != null) ? mSimRecords.getMsisdnNumber() : null;
    }

    @Override
    public void getAvailableNetworks(Message response) {
        mCi.getAvailableNetworks(response);
    }

    @Override
    public void requestIsimAuthentication(String nonce, Message result) {
        mCi.requestIsimAuthentication(nonce, result);
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        // Update IsimRecords
        UiccCardApplication newUiccApplication =
                mUiccController.getUiccCardApplication(UiccController.APP_FAM_IMS);
        IsimUiccRecords newIsimUiccRecords = null;

        if (newUiccApplication != null) {
            newIsimUiccRecords = (IsimUiccRecords)newUiccApplication.getIccRecords();
        }
        mIsimUiccRecords = newIsimUiccRecords;

        // Update UsimRecords
        newUiccApplication = mUiccController.getUiccCardApplication(UiccController.APP_FAM_3GPP);
        SIMRecords newSimRecords = null;
        if (newUiccApplication != null) {
            newSimRecords = (SIMRecords)newUiccApplication.getIccRecords();
        }
        if (mSimRecords != newSimRecords) {
            if (mSimRecords != null) {
                log("Removing stale SIMRecords object.");
                mSimRecords = null;
            }
            if (newSimRecords != null) {
                log("New SIMRecords found");
                mSimRecords = newSimRecords;
            }
        }

        super.onUpdateIccAvailability();
    }

    @Override
    protected void log(String s) {
            Rlog.d(LOG_LTE_TAG, s);
    }

    protected void loge(String s) {
            Rlog.e(LOG_LTE_TAG, s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(LOG_LTE_TAG, s, e);
}

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CDMALTEPhone extends:");
        super.dump(fd, pw, args);
    }
}
