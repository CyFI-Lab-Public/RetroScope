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
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;

import com.android.cts.verifier.R;

/**
 * Service discovery requester test case to removeServiceRequest and clearServiceResusts
 * have no effect against another channel.
 */
public class ServReqMultiClientTestCase03 extends ServReqTestCase {

    public ServReqMultiClientTestCase03(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        notifyTestMsg(R.string.p2p_checking_serv_capab);

        ActionListenerTest actionListener = new ActionListenerTest();

        WifiP2pDnsSdServiceRequest bonjourReq =
                WifiP2pDnsSdServiceRequest.newInstance();

        /*
         * add Bonjour request to channel1.
         */
        mP2pMgr.addServiceRequest(mChannel, bonjourReq, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }

        /*
         * Try to remove the bonjour request of channel1 on channel2.
         * However, it should have no efferct.
         */
        mP2pMgr.removeServiceRequest(mSubChannel, bonjourReq, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_remove_service_request_error);
            return false;
        }

        /*
         * clear the all requests on channel2.
         * However, it should have no efferct.
         */
        mP2pMgr.clearServiceRequests(mSubChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_clear_service_requests_error);
            return false;
        }

        /*
         * initialize listener test.
         */
        UPnPServiceResponseListenerTest upnpCh1Listener =
                new UPnPServiceResponseListenerTest(mTargetAddress);
        DnsSdResponseListenerTest dnsCh1Listener =
                new DnsSdResponseListenerTest(mTargetAddress);
        DnsSdTxtRecordListenerTest txtCh1Listener =
                new DnsSdTxtRecordListenerTest(mTargetAddress);
        UPnPServiceResponseListenerTest upnpCh2Listener =
                new UPnPServiceResponseListenerTest(mTargetAddress);
        DnsSdResponseListenerTest dnsCh2Listener =
                new DnsSdResponseListenerTest(mTargetAddress);
        DnsSdTxtRecordListenerTest txtCh2Listener =
                new DnsSdTxtRecordListenerTest(mTargetAddress);

        /*
         * set service response listener callback.
         */
        mP2pMgr.setUpnpServiceResponseListener(mChannel, upnpCh1Listener);
        mP2pMgr.setDnsSdResponseListeners(mChannel, dnsCh1Listener, txtCh1Listener);
        mP2pMgr.setUpnpServiceResponseListener(mSubChannel, upnpCh2Listener);
        mP2pMgr.setDnsSdResponseListeners(mSubChannel, dnsCh2Listener, txtCh2Listener);

        /*
         * discover services
         */
        mP2pMgr.discoverServices(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_discover_services_error);
            return false;
        }

        /*
         * check that bonjour response can be received on channel1
         * check that no UPnP response is received on channel1
         */
        Timeout t = new Timeout(TIMEOUT);
        if (!dnsCh1Listener.check(DnsSdResponseListenerTest.ALL_DNS_PTR,
                t.getRemainTime())) {
            mReason = getListenerError(dnsCh1Listener);
            return false;
        }
        if (!txtCh1Listener.check(DnsSdTxtRecordListenerTest.ALL_DNS_TXT,
                t.getRemainTime())) {
            mReason = getListenerError(txtCh1Listener);
            return false;
        }
        if (!upnpCh1Listener.check(
                UPnPServiceResponseListenerTest.NO_UPNP_SERVICES,
                t.getRemainTime())) {
            mReason = getListenerError(upnpCh1Listener);
            return false;
        }

        return true;
    }

    @Override
    public String getTestName() {
        return "Multiple clients test 03";
    }
}
