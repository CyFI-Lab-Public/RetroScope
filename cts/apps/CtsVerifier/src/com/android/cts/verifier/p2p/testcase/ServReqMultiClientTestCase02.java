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
 * Service discovery requester test case to search bonjour IPP PTR service
 * on channel1 and search AFP TXT service on channel2.
 */
public class ServReqMultiClientTestCase02 extends ServReqTestCase {

    public ServReqMultiClientTestCase02(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        notifyTestMsg(R.string.p2p_checking_serv_capab);

        ActionListenerTest actionListener = new ActionListenerTest();

        /*
         * create bonjour IPP PTR request and AFP TXT request.
         */
        WifiP2pDnsSdServiceRequest bonjourIppPtrReq =
                WifiP2pDnsSdServiceRequest.newInstance("_ipp._tcp");
        WifiP2pDnsSdServiceRequest bonjourAfpTxtReq =
                WifiP2pDnsSdServiceRequest.newInstance("Example", "_afpovertcp._tcp");

        /*
         * add bonjour IPP PTR request to channel1.
         */
        mP2pMgr.addServiceRequest(mChannel, bonjourIppPtrReq, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }

        /*
         * add bonjour AFP TXT request to channel2.
         */
        mP2pMgr.addServiceRequest(mSubChannel, bonjourAfpTxtReq, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }

        /*
         * initialize test listener
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
         * channel1 receive only Bonjour IPP PTR.
         */
        Timeout t = new Timeout(TIMEOUT);
        if (!dnsCh1Listener.check(DnsSdResponseListenerTest.IPP_DNS_PTR,
                t.getRemainTime())) {
            mReason = getListenerError(dnsCh1Listener);
            return false;
        }
        if (!txtCh1Listener.check(DnsSdTxtRecordListenerTest.NO_DNS_TXT,
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

        /*
         * channel2 receive only Bonjour AFP TXT.
         */
        if (!dnsCh2Listener.check(DnsSdResponseListenerTest.NO_DNS_PTR,
                t.getRemainTime())) {
            mReason = getListenerError(dnsCh2Listener);
            return false;
        }
        if (!txtCh2Listener.check(DnsSdTxtRecordListenerTest.AFP_DNS_TXT,
                t.getRemainTime())) {
            mReason = getListenerError(txtCh2Listener);
            return false;
        }
        if (!upnpCh2Listener.check(
                UPnPServiceResponseListenerTest.NO_UPNP_SERVICES,
                t.getRemainTime())) {
            mReason = getListenerError(upnpCh2Listener);
            return false;
        }

        return true;
    }

    @Override
    public String getTestName() {
        return "Multiple clients test 02";
    }
}
