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

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager.UpnpServiceResponseListener;
import android.util.Log;

/**
 * The utility class for testing
 * android.net.wifi.p2p.WifiP2pManager.UpnpServiceResponseListener callback function.
 */
public class UPnPServiceResponseListenerTest extends ListenerTest
    implements UpnpServiceResponseListener {

    private static final String TAG = "UPnPServiceResponseListenerTest";

    public static final List<ListenerArgument> NO_UPNP_SERVICES
            = new ArrayList<ListenerArgument>();

    public static final List<ListenerArgument> ALL_UPNP_SERVICES
            = new ArrayList<ListenerArgument>();

    public static final List<ListenerArgument> UPNP_ROOT_DEVICE
            = new ArrayList<ListenerArgument>();

    /**
     * The target device address.
     */
    private String mTargetAddr;

    static {
        initialize();
    }

    public UPnPServiceResponseListenerTest(String targetAddr) {
        mTargetAddr = targetAddr;
    }

    @Override
    public void onUpnpServiceAvailable(List<String> uniqueServiceNames,
            WifiP2pDevice srcDevice) {
        Log.d(TAG, uniqueServiceNames + " received from " + srcDevice.deviceAddress);

        /*
         * Check only the response from the target device.
         * The response from other devices are ignored.
         */
        if (srcDevice.deviceAddress.equalsIgnoreCase(mTargetAddr)) {
            for (String uniqueServiceName : uniqueServiceNames) {
                receiveCallback(new Argument(uniqueServiceName));
            }
        }
    }

    private static void initialize() {
        String uuid = "uuid:6859dede-8574-59ab-9332-123456789011";
        ALL_UPNP_SERVICES.add(new Argument(uuid));
        ALL_UPNP_SERVICES.add(new Argument(uuid +
                "::upnp:rootdevice"));
        ALL_UPNP_SERVICES.add(new Argument(uuid +
                "::urn:schemas-upnp-org:device:MediaRenderer:1"));
        ALL_UPNP_SERVICES.add(new Argument(uuid +
                "::urn:schemas-upnp-org:service:AVTransport:1"));
        ALL_UPNP_SERVICES.add(new Argument(uuid +
                "::urn:schemas-upnp-org:service:ConnectionManager:1"));

        UPNP_ROOT_DEVICE.add(new Argument(uuid +
                "::upnp:rootdevice"));
    }

    /**
     * The container of the argument of {@link #onUpnpServiceAvailable}.
     */
    static class Argument extends ListenerArgument {
        String mUniqueServiceName;

        Argument(String uniqueServiceName) {
            mUniqueServiceName = uniqueServiceName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Argument)) {
                return false;
            }
            Argument arg = (Argument)obj;
            return equals(mUniqueServiceName, arg.mUniqueServiceName);
        }

        private boolean equals(String s1, String s2) {
            if (s1 == null && s2 == null) {
                return true;
            }
            if (s1 == null || s2 == null) {
                return false;
            }
            return s1.equals(s2);
        }

        @Override
        public String toString() {
            return mUniqueServiceName.toString();
        }
    }
}
