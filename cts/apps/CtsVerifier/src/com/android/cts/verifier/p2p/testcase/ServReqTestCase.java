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

import java.util.List;

import android.content.Context;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;

import com.android.cts.verifier.R;
import com.android.cts.verifier.p2p.testcase.ListenerTest.ListenerArgument;

/**
 * The base class for service discovery requester test case.
 * The common functions are defined in this class.
 */
public abstract class ServReqTestCase extends ReqTestCase {

    public ServReqTestCase(Context context) {
        super(context);
    }

    /**
     * Search test.
     *
     * 1. Add the specified service requests to the framework.<br>
     * 2. Call discoverService().<br>
     * 3. Check the appropriate responses can be received.<br>
     *
     * @param targetAddr target device address
     * @param reqList service discovery request list to send.
     * @param dnsPtrArgList the expected result list of dns sd callback function
     * @param dnsTxtArgList the expected result list of txt record callback function
     * @param upnpArgList the expected result list of upnp callback function
     */
    protected boolean searchTest(String targetAddr,
            List<WifiP2pServiceRequest> reqList,
            List<ListenerArgument> dnsPtrArgList,
            List<ListenerArgument> dnsTxtArgList,
            List<ListenerArgument> upnpArgList)
            throws InterruptedException {

        ActionListenerTest actionListener = new ActionListenerTest();
        UPnPServiceResponseListenerTest upnpListener =
                new UPnPServiceResponseListenerTest(targetAddr);
        DnsSdResponseListenerTest dnsSdListener =
                new DnsSdResponseListenerTest(targetAddr);
        DnsSdTxtRecordListenerTest txtListener =
                new DnsSdTxtRecordListenerTest(targetAddr);

        /*
         * Set service discovery request to the framework.
         */
        for (WifiP2pServiceRequest req: reqList) {
            mP2pMgr.addServiceRequest(mChannel, req, actionListener);
            if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
                mReason = mContext.getString(R.string.p2p_add_service_request_error);
                return false;
            }
        }

        /*
         * set service response callback function to framework
         */
        mP2pMgr.setUpnpServiceResponseListener(mChannel, upnpListener);
        mP2pMgr.setDnsSdResponseListeners(mChannel, dnsSdListener, txtListener);

        /*
         * execute service find operation.
         */
        mP2pMgr.discoverServices(mChannel, actionListener);
        if (!actionListener.check(ActionListenerTest.SUCCESS, TIMEOUT)) {
            mReason = mContext.getString(R.string.p2p_discover_services_error);
            return false;
        }

        /*
         * If needed, check that the expected callback is invoked correctly
         * Otherwise, check that no callback is invoked.
         */
        Timeout t = new Timeout(TIMEOUT);
        if (!dnsSdListener.check(dnsPtrArgList, t.getRemainTime())) {
            mReason = getListenerError(dnsSdListener);
            return false;
        }
        if (!txtListener.check(dnsTxtArgList, t.getRemainTime())) {
            mReason = getListenerError(txtListener);
            return false;
        }
        if (!upnpListener.check(upnpArgList, t.getRemainTime())) {
            mReason = getListenerError(upnpListener);
            return false;
        }

        return true;
    }

    protected String getListenerError(ListenerTest listener) {
        StringBuilder sb = new StringBuilder();
        sb.append(mContext.getText(R.string.p2p_receive_invalid_response_error));
        sb.append(listener.getReason());
        return sb.toString();
    }
}
