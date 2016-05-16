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

package com.android.cts.verifier.p2p.testcase;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import com.android.cts.verifier.R;

/**
 * Test case to join a p2p group with wps pincode.
 */
public class P2pClientPinTestCase extends ConnectReqTestCase {

    public P2pClientPinTestCase(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        if (!checkBonjourService()) {
            return false;
        }

        return connectTest(true, WpsInfo.DISPLAY);
    }

    /**
     * Check Bonjour service on GO through service discovery request.
     * @return true if success.
     * @throws InterruptedException
     */
    private boolean checkBonjourService() throws InterruptedException {
        notifyTestMsg(R.string.p2p_checking_serv_capab);
        ActionListenerTest actionListener = new ActionListenerTest();

        /*
         * add Bonjour request.
         */
        WifiP2pDnsSdServiceRequest bonjourReq =
            WifiP2pDnsSdServiceRequest.newInstance();
        mP2pMgr.addServiceRequest(mSubChannel, bonjourReq, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }

        /*
         * Initialize listener test objects.
         */
        UPnPServiceResponseListenerTest upnpListener =
                new UPnPServiceResponseListenerTest(mTargetAddress);
        DnsSdResponseListenerTest dnsListener =
                new DnsSdResponseListenerTest(mTargetAddress);
        DnsSdTxtRecordListenerTest txtListener =
                new DnsSdTxtRecordListenerTest(mTargetAddress);

        /*
         * set service response listener callback.
         */
        mP2pMgr.setUpnpServiceResponseListener(mSubChannel, upnpListener);
        mP2pMgr.setDnsSdResponseListeners(mSubChannel, dnsListener, txtListener);

        /*
         * discover services
         */
        mP2pMgr.discoverServices(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_discover_services_error);
            return false;
        }

        /*
         * Receive only Bonjour service.
         */
        Timeout t = new Timeout(TIMEOUT);
        if (!dnsListener.check(DnsSdResponseListenerTest.ALL_DNS_PTR,
                t.getRemainTime())) {
            mReason = getListenerError(dnsListener);
            return false;
        }
        if (!txtListener.check(DnsSdTxtRecordListenerTest.ALL_DNS_TXT,
                t.getRemainTime())) {
            mReason = getListenerError(txtListener);
            return false;
        }
        if (!upnpListener.check(
                UPnPServiceResponseListenerTest.NO_UPNP_SERVICES,
                t.getRemainTime())) {
            mReason = getListenerError(upnpListener);
            return false;
        }

        return true;
    }

    private String getListenerError(ListenerTest listener) {
        StringBuilder sb = new StringBuilder();
        sb.append(mContext.getText(R.string.p2p_receive_invalid_response_error));
        sb.append(listener.getReason());
        return sb.toString();
    }

    @Override
    public String getTestName() {
        return "Join p2p group test (PIN)";
    }
}
