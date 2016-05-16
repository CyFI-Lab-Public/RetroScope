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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceInfo;

public class LocalServices {


    /**
     * Create UPnP MediaRenderer local service.
     * @return
     */
    static WifiP2pServiceInfo createRendererService() {
        List<String> services = new ArrayList<String>();
        services.add("urn:schemas-upnp-org:service:AVTransport:1");
        services.add("urn:schemas-upnp-org:service:ConnectionManager:1");
        return WifiP2pUpnpServiceInfo.newInstance(
                "6859dede-8574-59ab-9332-123456789011",
                "urn:schemas-upnp-org:device:MediaRenderer:1",
                services);
    }

    /**
     * Create Bonjour IPP local service.
     * @return
     */
    static WifiP2pServiceInfo createIppService() {
        Map<String, String> txtRecord = new HashMap<String, String>();
        txtRecord.put("txtvers", "1");
        txtRecord.put("pdl", "application/postscript");
        return WifiP2pDnsSdServiceInfo.newInstance("MyPrinter",
                "_ipp._tcp", txtRecord);
    }

    /**
     * Create Bonjour AFP local service.
     * @return
     */
    static WifiP2pServiceInfo createAfpService() {
        return WifiP2pDnsSdServiceInfo.newInstance("Example",
                "_afpovertcp._tcp", null);
    }
}
