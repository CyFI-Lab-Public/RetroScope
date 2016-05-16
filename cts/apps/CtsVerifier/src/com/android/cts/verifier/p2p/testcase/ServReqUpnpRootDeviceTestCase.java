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
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;

/**
 * Service discovery requester test case to search upnp root devices.
 */
public class ServReqUpnpRootDeviceTestCase extends ServReqTestCase {

    public ServReqUpnpRootDeviceTestCase(Context context) {
        super(context);
    }

    @Override
    protected boolean executeTest() throws InterruptedException {

        notifyTestMsg(R.string.p2p_checking_serv_capab);

        /*
         * create request to search UPnP root device
         */
        List<WifiP2pServiceRequest> reqList = new ArrayList<WifiP2pServiceRequest>();
        reqList.add(WifiP2pUpnpServiceRequest.newInstance("upnp:rootdevice"));

        /*
         * search and check the callback function.
         *
         * The expected the argument of the callback function is as follows.
         * DNS PTR: No services.
         * DNS TXT: No services.
         * UPnP: rootdevice.
         */
        return searchTest(mTargetAddress, reqList,
                DnsSdResponseListenerTest.NO_DNS_PTR,
                DnsSdTxtRecordListenerTest.NO_DNS_TXT,
                UPnPServiceResponseListenerTest.UPNP_ROOT_DEVICE);
    }

    @Override
    public String getTestName() {
        return "Request upnp root devices test";
    }
}
