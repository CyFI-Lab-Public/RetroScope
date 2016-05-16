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
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;

import com.android.cts.verifier.R;

/**
 * Service discovery requester test case to check clearServiceRequests() works well.
 */
public class ServReqClearRequestTestCase extends ServReqTestCase {

    public ServReqClearRequestTestCase(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        notifyTestMsg(R.string.p2p_checking_serv_capab);

        ActionListenerTest actionListener = new ActionListenerTest();

        /*
         * create some service requests.
         */
        WifiP2pUpnpServiceRequest upnpReq1 =
                WifiP2pUpnpServiceRequest.newInstance();
        WifiP2pUpnpServiceRequest upnpReq2 =
                WifiP2pUpnpServiceRequest.newInstance("ssdp:all");
        WifiP2pDnsSdServiceRequest bonjourReq1 =
                WifiP2pDnsSdServiceRequest.newInstance();
        WifiP2pDnsSdServiceRequest bonjourReq2 =
                WifiP2pDnsSdServiceRequest.newInstance("_ipp._tcp");

        /*
         * add request
         */
        mP2pMgr.addServiceRequest(mChannel, upnpReq1, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }
        mP2pMgr.addServiceRequest(mChannel, upnpReq2, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }
        mP2pMgr.addServiceRequest(mChannel, bonjourReq1, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }
        mP2pMgr.addServiceRequest(mChannel, bonjourReq2, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_add_service_request_error);
            return false;
        }

        /*
         * clear requests
         */
        mP2pMgr.clearServiceRequests(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_clear_service_requests_error);
            return false;
        }

        /*
         * search services, but NO_SERVICE_REQUESTS is returned.
         */
        mP2pMgr.discoverServices(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.FAIL_NO_SERVICE, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_no_service_requests_error);
            return false;
        }

        return true;
    }

    @Override
    public String getTestName() {
        return "Clear service requests test";
    }
}
