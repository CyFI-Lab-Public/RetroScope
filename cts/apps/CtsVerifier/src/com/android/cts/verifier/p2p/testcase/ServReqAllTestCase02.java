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

import java.util.ArrayList;
import java.util.List;

import com.android.cts.verifier.R;

import android.content.Context;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;

/**
 * Service discovery requester test case to search all UPnP and Bonjour services with
 * WifiP2pServiceRequest.newInstance(WifiP2pServiceInfo.SERVICE_TYPE_BONJOUR) and
 * WifiP2pServiceRequest.newInstance(WifiP2pServiceInfo.SERVICE_TYPE_UPNP).
 */
public class ServReqAllTestCase02 extends ServReqTestCase {

    public ServReqAllTestCase02(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        notifyTestMsg(R.string.p2p_checking_serv_capab);

        /*
         * create request to search all services.
         */
        List<WifiP2pServiceRequest> reqList = new ArrayList<WifiP2pServiceRequest>();
        reqList.add(WifiP2pServiceRequest.newInstance(
                WifiP2pServiceInfo.SERVICE_TYPE_BONJOUR));
        reqList.add(WifiP2pServiceRequest.newInstance(
                WifiP2pServiceInfo.SERVICE_TYPE_UPNP));

        /*
         * search and check the callback function.
         *
         * The expected the argument of the callback function is as follows.
         * DNS PTR: ALL services.
         * DNS TXT: ALL services.
         * UPnP: ALL services.
         */
        return searchTest(mTargetAddress, reqList,
                DnsSdResponseListenerTest.ALL_DNS_PTR,
                DnsSdTxtRecordListenerTest.ALL_DNS_TXT,
                UPnPServiceResponseListenerTest.ALL_UPNP_SERVICES);
    }

    @Override
    public String getTestName() {
        return "Request all services test 02";
    }
}
