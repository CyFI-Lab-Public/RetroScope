/*
 * Copyright (C) 2009 The Android Open Source Project
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

package android.net.cts;

import android.net.DhcpInfo;
import android.test.AndroidTestCase;

public class DhcpInfoTest extends AndroidTestCase {

    public void testConstructor() {
        new DhcpInfo();
    }

    public void testToString() {
        String expectedDefault = "ipaddr 0.0.0.0 gateway 0.0.0.0 netmask 0.0.0.0 dns1 0.0.0.0 "
                + "dns2 0.0.0.0 DHCP server 0.0.0.0 lease 0 seconds";
        String STR_ADDR1 = "255.255.255.255";
        String STR_ADDR2 = "127.0.0.1";
        String STR_ADDR3 = "192.168.1.1";
        String STR_ADDR4 = "192.168.1.0";
        int leaseTime = 9999;
        String expected = "ipaddr " + STR_ADDR1 + " gateway " + STR_ADDR2 + " netmask "
                + STR_ADDR3 + " dns1 " + STR_ADDR4 + " dns2 " + STR_ADDR4 + " DHCP server "
                + STR_ADDR2 + " lease " + leaseTime + " seconds";

        DhcpInfo dhcpInfo = new DhcpInfo();

        // Test default string.
        assertEquals(expectedDefault, dhcpInfo.toString());

        dhcpInfo.ipAddress = ipToInteger(STR_ADDR1);
        dhcpInfo.gateway = ipToInteger(STR_ADDR2);
        dhcpInfo.netmask = ipToInteger(STR_ADDR3);
        dhcpInfo.dns1 = ipToInteger(STR_ADDR4);
        dhcpInfo.dns2 = ipToInteger(STR_ADDR4);
        dhcpInfo.serverAddress = ipToInteger(STR_ADDR2);
        dhcpInfo.leaseDuration = leaseTime;

        // Test with new values
        assertEquals(expected, dhcpInfo.toString());
    }

    private int ipToInteger(String ipString) {
        String ipSegs[] = ipString.split("[.]");
        int tmp = Integer.parseInt(ipSegs[3]) << 24 | Integer.parseInt(ipSegs[2]) << 16 |
            Integer.parseInt(ipSegs[1]) << 8 | Integer.parseInt(ipSegs[0]);
        return tmp;
    }
}
