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
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.util.Log;

/**
 * The utility class for testing
 * android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener callback function.
 */
public class DnsSdResponseListenerTest extends ListenerTest
    implements DnsSdServiceResponseListener {

    private static final String TAG = "DnsSdResponseListenerTest";

    public static final List<ListenerArgument> NO_DNS_PTR
            = new ArrayList<ListenerArgument>();

    public static final List<ListenerArgument> ALL_DNS_PTR
            = new ArrayList<ListenerArgument>();

    public static final List<ListenerArgument> IPP_DNS_PTR
            = new ArrayList<ListenerArgument>();

    public static final List<ListenerArgument> AFP_DNS_PTR
            = new ArrayList<ListenerArgument>();

    /**
     * The target device address.
     */
    private String mTargetAddr;

    static {
        initialize();
    }

    public DnsSdResponseListenerTest(String targetAddr) {
        mTargetAddr = targetAddr;
    }

    @Override
    public void onDnsSdServiceAvailable(String instanceName,
            String registrationType, WifiP2pDevice srcDevice) {
        Log.d(TAG, instanceName + " " + registrationType +
                " received from " + srcDevice.deviceAddress);

        /*
         * Check only the response from the target device.
         * The response from other devices are ignored.
         */
        if (srcDevice.deviceAddress.equalsIgnoreCase(mTargetAddr)) {
            receiveCallback(new Argument(instanceName, registrationType));
        }
    }

    private static void initialize() {
        String ippInstanceName = "MyPrinter";
        String ippRegistrationType = "_ipp._tcp.local.";
        String afpInstanceName = "Example";
        String afpRegistrationType = "_afpovertcp._tcp.local.";

        IPP_DNS_PTR.add(new Argument(ippInstanceName, ippRegistrationType));
        AFP_DNS_PTR.add(new Argument(afpInstanceName, afpRegistrationType));
        ALL_DNS_PTR.add(new Argument(ippInstanceName, ippRegistrationType));
        ALL_DNS_PTR.add(new Argument(afpInstanceName, afpRegistrationType));
    }

    /**
     * The container of the argument of {@link #onDnsSdServiceAvailable}.
     */
    static class Argument extends ListenerArgument {

        private String mInstanceName;
        private String mRegistrationType;

        /**
         * Set the argument of {@link #onDnsSdServiceAvailable}.
         *
         * @param instanceName instance name.
         * @param registrationType registration type.
         */
        Argument(String instanceName, String registrationType) {
            mInstanceName = instanceName;
            mRegistrationType = registrationType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof Argument)) {
                return false;
            }
            Argument arg = (Argument)obj;
            return equals(mInstanceName, arg.mInstanceName) &&
                equals(mRegistrationType, arg.mRegistrationType);
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
            StringBuilder sb = new StringBuilder();
            sb.append("[type=dns_ptr instant_name=");
            sb.append(mInstanceName);
            sb.append(" registration type=");
            sb.append(mRegistrationType);
            sb.append("\n");

            return "instanceName=" + mInstanceName +
                    " registrationType=" + mRegistrationType;
        }
    }
}
