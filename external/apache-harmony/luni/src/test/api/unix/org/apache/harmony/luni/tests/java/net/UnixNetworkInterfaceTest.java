/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.luni.tests.java.net;

import java.net.NetworkInterface;
import java.util.Enumeration;

import junit.framework.TestCase;

/**
 * Please note that this case can only be passed on Linux with the user is
 * 'root'.
 * 
 */
public class UnixNetworkInterfaceTest extends TestCase {
    private Enumeration<NetworkInterface> netifs = null;

    private boolean valid = false;

    /**
     * @tests java.net.NetworkInterface#isUp()
     * 
     * @since 1.6
     */
    public void test_isUp() throws Exception {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            String name = netif.getName();
            boolean up = netif.isUp();
            // Down network interface will bring side effect on the
            // platform. So chooses the already-down interface to up it.
            if (!up && valid) {
                String cmd = "ifconfig " + name + " up";
                Process proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " up should be " + !up, !up, netif.isUp());

                cmd = "ifconfig " + name + " down";
                proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " up should be " + up, up, netif.isUp());
            }
        }
    }

    /**
     * @tests java.net.NetworkInterface#supportsMulticast()
     * 
     * @since 1.6
     */
    public void test_supportsMulticast() throws Exception {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            String name = netif.getName();
            boolean multicast = netif.supportsMulticast();
            if (valid) {
                String cmd = multicast ? "ifconfig " + name + " -multicast"
                        : "ifconfig " + name + " multicast";
                Process proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " multicast should be " + !multicast,
                        !multicast, netif.supportsMulticast());

                cmd = multicast ? "ifconfig " + name + " multicast"
                        : "ifconfig " + name + " -multicast";
                proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " multicast should be " + multicast,
                        multicast, netif.supportsMulticast());
            }
        }
    }

    /**
     * @tests java.net.NetworkInterface#getHardwareAddress()
     * 
     * @since 1.6
     */
    public void test_getHardwareAddress() throws Exception {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            String name = netif.getName();
            byte[] hardAddr = netif.getHardwareAddress();

            if (hardAddr != null && valid) {
                String hardwareAddress = toHardwareAddress(hardAddr);
                String newHardwareAddr = "0:11:25:1B:BD:FF";
                String cmd = "ifconfig " + name + " hw ether "
                        + newHardwareAddr;
                Process proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + "'s hardware address should be"
                        + newHardwareAddr, newHardwareAddr,
                        toHardwareAddress(netif.getHardwareAddress()));

                cmd = "ifconfig " + name + " hw ether " + hardwareAddress;
                proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + "'s hardware address should be"
                        + hardwareAddress, hardwareAddress,
                        toHardwareAddress(netif.getHardwareAddress()));
            }
        }
    }

    /**
     * @tests java.net.NetworkInterface#getMTU()
     * 
     * @since 1.6
     */
    public void test_getMTU() throws Exception {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            String name = netif.getName();
            int mtu = netif.getMTU();
            if (valid) {
                String cmd = "ifconfig " + name + " mtu 1000";
                Process proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " MTU should be 1000", 1000, netif.getMTU());

                cmd = "ifconfig " + name + " mtu " + mtu;
                proc = Runtime.getRuntime().exec(cmd);
                proc.waitFor();
                assertEquals(name + " MTU should be " + mtu, mtu, netif
                        .getMTU());
            }
        }
    }

    /**
     * @tests java.net.NetworkInterface#getSubInterfaces()
     * 
     * @since 1.6
     */
    public void test_getSubInterfaces() throws Exception {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            Enumeration<NetworkInterface> subInterfaces1 = netif
                    .getSubInterfaces();
            Enumeration<NetworkInterface> subInterfaces2 = netif
                    .getSubInterfaces();
            while (subInterfaces1.hasMoreElements()
                    && subInterfaces2.hasMoreElements() && valid) {
                NetworkInterface sub1 = subInterfaces1.nextElement();
                NetworkInterface sub2 = subInterfaces2.nextElement();
                assertSame(sub1, sub2);
                assertSame(netif, sub1.getParent());
            }
        }
    }

    /**
     * @tests java.net.NetworkInterface#getParent()
     * 
     * @since 1.6
     */
    public void test_getParent() {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            Enumeration<NetworkInterface> subInterfaces = netif
                    .getSubInterfaces();
            boolean contains = false;
            boolean hasSubInterfaces = false;
            while (subInterfaces.hasMoreElements() && valid) {
                hasSubInterfaces = true;
                NetworkInterface sub = subInterfaces.nextElement();
                NetworkInterface parent1 = sub.getParent();
                NetworkInterface parent2 = sub.getParent();
                assertSame(parent1, parent2);
                if (netif.equals(parent1)) {
                    contains = true;
                    break;
                }
            }
            assertEquals(hasSubInterfaces, contains);
        }
    }

    /**
     * 
     * @tests java.net.NetworkInterface#isVirtual()
     * 
     * @since 1.6
     */
    public void test_isVirtual() {
        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            boolean virtual = netif.getParent() != null;
            assertEquals(netif.getName() + " isVirtual() is" + !virtual,
                    virtual, netif.isVirtual());
            Enumeration<NetworkInterface> subInterfaces = netif
                    .getSubInterfaces();
            while (subInterfaces.hasMoreElements()) {
                assertTrue(subInterfaces.nextElement().isVirtual());
            }
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        netifs = NetworkInterface.getNetworkInterfaces();
        valid = (netifs != null)
                && System.getProperty("user.name").equals("root");
    }

    @Override
    protected void tearDown() throws Exception {
        netifs = null;
        valid = false;
        super.tearDown();
    }

    private static String toHardwareAddress(byte[] hardAddr) {
        StringBuilder builder = new StringBuilder();
        for (byte b : hardAddr) {
            builder.append(Integer.toHexString(b >= 0 ? b : b + 256)
                    .toUpperCase());
            if (hardAddr[hardAddr.length - 1] != b) {
                builder.append(":");
            }
        }
        return builder.toString();
    }
}
