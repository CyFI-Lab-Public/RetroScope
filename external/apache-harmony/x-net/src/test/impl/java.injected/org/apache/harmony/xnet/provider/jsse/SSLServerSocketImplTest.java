/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.xnet.provider.jsse;

import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLServerSocket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLServerSocketImplTest test
 */
public class SSLServerSocketImplTest extends TestCase {

    private static boolean doLog = false;

    /**
     * Sets up the test case.
     */
    @Override
    public void setUp() {
        if (doLog) {
            System.out.println("");
            System.out.println("========================");
            System.out.println("====== Running the test: " + getName());
        }
    }

    private SSLServerSocket createSSLServerSocket() throws Exception {
        return new SSLServerSocketImpl(JSSETestData.getSSLParameters());
    }

    /**
     * SSLServerSocketImpl(SSLParameters sslParameters) method testing.
     */
    public void testSSLServerSocketImpl1() throws Exception {
        Client client = null;
        SSLServerSocket ssocket = null;
        try {
            ssocket = new SSLServerSocketImpl(JSSETestData.getSSLParameters());
            ssocket.bind(null);
            ssocket.setUseClientMode(true);

            final SSLServerSocket s = ssocket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            client = new Client(ssocket.getLocalPort());
            client.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!client.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        client.close();
                    } catch (IOException ex) { }
                    try {
                        ssocket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) { }
            }
            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLServerSocketImpl(int port, SSLParameters sslParameters) method
     * testing.
     */
    public void testSSLServerSocketImpl2() throws Exception {
        Client client = null;
        SSLServerSocket ssocket = null;
        try {
            ssocket = new SSLServerSocketImpl(0,
                    JSSETestData.getSSLParameters());
            ssocket.setUseClientMode(true);

            final SSLServerSocket s = ssocket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            client = new Client(ssocket.getLocalPort());
            client.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!client.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        client.close();
                    } catch (IOException ex) { }
                    try {
                        ssocket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) { }
            }
            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLServerSocketImpl(int port, int backlog,
     * SSLParameters sslParameters) method testing.
     */
    public void testSSLServerSocketImpl3() throws Exception {
        Client client = null;
        SSLServerSocket ssocket = null;
        try {
            ssocket = new SSLServerSocketImpl(0, 1,
                    JSSETestData.getSSLParameters());
            ssocket.setUseClientMode(true);

            final SSLServerSocket s = ssocket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            client = new Client(ssocket.getLocalPort());
            client.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!client.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        client.close();
                    } catch (IOException ex) { }
                    try {
                        ssocket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) { }
            }
            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLServerSocketImpl(int port, int backlog, InetAddress iAddress,
     * SSLParameters sslParameters) method testing.
     */
    public void testSSLServerSocketImpl4() throws Exception {
        Client client = null;
        SSLServerSocket ssocket = null;
        try {
            ssocket = new SSLServerSocketImpl(0, 1, null,
                    JSSETestData.getSSLParameters());
            ssocket.setUseClientMode(true);

            final SSLServerSocket s = ssocket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.accept().close();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            client = new Client(ssocket.getLocalPort());
            client.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!client.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        client.close();
                    } catch (IOException ex) { }
                    try {
                        ssocket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ex) { }
            }
            if (ssocket != null) {
                try {
                    ssocket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * getSupportedCipherSuites() method testing.
     */
    public void testGetSupportedCipherSuites() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] supported = ssocket.getSupportedCipherSuites();
        assertNotNull(supported);
        supported[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        supported = ssocket.getEnabledCipherSuites();
        for (int i=0; i<supported.length; i++) {
            if ("NOT_SUPPORTED_CIPHER_SUITE".equals(supported[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * getEnabledCipherSuites() method testing.
     */
    public void testGetEnabledCipherSuites() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] enabled = ssocket.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = ssocket.getSupportedCipherSuites();
        for (int i=0; i<enabled.length; i++) {
            //System.out.println("Checking of "+enabled[i]);
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (enabled[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Enabled suite does not belong to the set "
                        + "of supported cipher suites: " + enabled[i]);
            }
        }
        ssocket.setEnabledCipherSuites(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, 0,
                    enabled, 0, supported.length-i);
            ssocket.setEnabledCipherSuites(enabled);
            String[] result = ssocket.getEnabledCipherSuites();
            if (result.length != enabled.length) {
                fail("Returned result does not correspond to expected.");
            }
            for (int k=0; k<result.length; k++) {
                found: {
                    for (int n=0; n<enabled.length; n++) {
                        if (result[k].equals(enabled[n])) {
                            break found;
                        }
                    }
                    if (result.length != enabled.length) {
                        fail("Returned result does not correspond "
                                + "to expected.");
                    }
                }
            }
        }
    }

    /**
     * setEnabledCipherSuites(String[] suites) method testing.
     */
    public void testSetEnabledCipherSuites() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] enabled = ssocket.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = ssocket.getSupportedCipherSuites();
        for (int i=0; i<enabled.length; i++) {
            //System.out.println("Checking of "+enabled[i]);
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (enabled[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Enabled suite does not belong to the set "
                        + "of supported cipher suites: " + enabled[i]);
            }
        }
        ssocket.setEnabledCipherSuites(supported);
        ssocket.setEnabledCipherSuites(enabled);
        ssocket.setEnabledCipherSuites(supported);
        String[] more_than_supported = new String[supported.length+1];
        for (int i=0; i<supported.length+1; i++) {
            more_than_supported[i]
                = "NOT_SUPPORTED_CIPHER_SUITE";
            System.arraycopy(supported, 0,
                    more_than_supported, 0, i);
            System.arraycopy(supported, i,
                    more_than_supported, i+1, supported.length-i);
            try {
                ssocket.setEnabledCipherSuites(more_than_supported);
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        }
        enabled = ssocket.getEnabledCipherSuites();
        enabled[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        enabled = ssocket.getEnabledCipherSuites();
        for (int i=0; i<enabled.length; i++) {
            if ("NOT_SUPPORTED_CIPHER_SUITE".equals(enabled[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * getSupportedProtocols() method testing.
     */
    public void testGetSupportedProtocols() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] supported = ssocket.getSupportedProtocols();
        assertNotNull(supported);
        assertFalse(supported.length == 0);
        supported[0] = "NOT_SUPPORTED_PROTOCOL";
        supported = ssocket.getSupportedProtocols();
        for (int i=0; i<supported.length; i++) {
            if ("NOT_SUPPORTED_PROTOCOL".equals(supported[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * getEnabledProtocols() method testing.
     */
    public void testGetEnabledProtocols() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] enabled = ssocket.getEnabledProtocols();
        assertNotNull(enabled);
        String[] supported = ssocket.getSupportedProtocols();
        for (int i=0; i<enabled.length; i++) {
            //System.out.println("Checking of "+enabled[i]);
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (enabled[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Enabled protocol does not belong to the set "
                        + "of supported protocols: " + enabled[i]);
            }
        }
        ssocket.setEnabledProtocols(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, i,
                    enabled, 0, supported.length-i);
            //System.out.println("");
            //for (int k=0; k<supported.length - i; k++) {
            //    System.out.println("---- "+enabled[k]);
            //}
            ssocket.setEnabledProtocols(enabled);
            String[] result = ssocket.getEnabledProtocols();
            if (result.length != enabled.length) {
                fail("Returned result does not correspond to expected.");
            }
            for (int k=0; k<result.length; k++) {
                found: {
                    for (int n=0; n<enabled.length; n++) {
                        if (result[k].equals(enabled[n])) {
                            break found;
                        }
                    }
                    if (result.length != enabled.length) {
                        fail("Returned result does not correspond "
                                + "to expected.");
                    }
                }
            }
        }
    }

    /**
     * setEnabledProtocols(String[] protocols) method testing.
     */
    public void testSetEnabledProtocols() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        String[] enabled = ssocket.getEnabledProtocols();
        assertNotNull(enabled);
        String[] supported = ssocket.getSupportedProtocols();
        for (int i=0; i<enabled.length; i++) {
            //System.out.println("Checking of "+enabled[i]);
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (enabled[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Enabled suite does not belong to the set "
                        + "of supported cipher suites: " + enabled[i]);
            }
        }
        ssocket.setEnabledProtocols(supported);
        ssocket.setEnabledProtocols(enabled);
        ssocket.setEnabledProtocols(supported);
        String[] more_than_supported = new String[supported.length+1];
        for (int i=0; i<supported.length+1; i++) {
            more_than_supported[i]
                = "NOT_SUPPORTED_PROTOCOL";
            System.arraycopy(supported, 0,
                    more_than_supported, 0, i);
            System.arraycopy(supported, i,
                    more_than_supported, i+1, supported.length-i);
            try {
                ssocket.setEnabledProtocols(more_than_supported);
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        }
        enabled = ssocket.getEnabledProtocols();
        enabled[0] = "NOT_SUPPORTED_PROTOCOL";
        enabled = ssocket.getEnabledProtocols();
        for (int i=0; i<enabled.length; i++) {
            if ("NOT_SUPPORTED_PROTOCOL".equals(enabled[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * setUseClientMode(boolean mode) method testing.
     * getUseClientMode() method testing.
     */
    public void testSetGetUseClientMode() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();

        ssocket.setUseClientMode(false);
        assertFalse("Result does not correspond to expected",
                ssocket.getUseClientMode());
        ssocket.setUseClientMode(true);
        assertTrue("Result does not correspond to expected",
                ssocket.getUseClientMode());
    }

    /**
     * setNeedClientAuth(boolean need) method testing.
     * getNeedClientAuth() method testing.
     */
    public void testSetGetNeedClientAuth() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();

        ssocket.setWantClientAuth(true);
        ssocket.setNeedClientAuth(false);
        assertFalse("Result does not correspond to expected",
                ssocket.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                ssocket.getWantClientAuth());
        ssocket.setWantClientAuth(true);
        ssocket.setNeedClientAuth(true);
        assertTrue("Result does not correspond to expected",
                ssocket.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                ssocket.getWantClientAuth());
    }

    /**
     * setWantClientAuth(boolean want) method testing.
     * getWantClientAuth() method testing.
     */
    public void testSetGetWantClientAuth() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();

        ssocket.setNeedClientAuth(true);
        ssocket.setWantClientAuth(false);
        assertFalse("Result does not correspond to expected",
                ssocket.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                ssocket.getNeedClientAuth());
        ssocket.setNeedClientAuth(true);
        ssocket.setWantClientAuth(true);
        assertTrue("Result does not correspond to expected",
                ssocket.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                ssocket.getNeedClientAuth());
    }

    /**
     * setEnableSessionCreation(boolean flag) method testing.
     * getEnableSessionCreation() method testing.
     */
    public void testSetGetEnableSessionCreation() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();

        ssocket.setEnableSessionCreation(false);
        assertFalse("Result does not correspond to expected",
                ssocket.getEnableSessionCreation());
        ssocket.setEnableSessionCreation(true);
        assertTrue("Result does not correspond to expected",
                ssocket.getEnableSessionCreation());
    }

    /**
     * toString() method testing.
     */
    public void testToString() throws Exception {
        SSLServerSocket ssocket = createSSLServerSocket();
        assertNotNull("String representation is null", ssocket.toString());
    }

    private static class Client extends Thread {

        private boolean closed;
        private boolean handshake_started = false;
        private Socket client = null;
        private int port;

        public Client(int port) throws IOException {
            super();
            this.port = port;
            client = new Socket();
            client.setSoTimeout(10000);
        }

        public int getPort() {
            return client.getLocalPort();
        }

        @Override
        public void run() {
            while (!closed) {
                try {
                    if (doLog) {
                        System.out.print(".");
                    }
                    if (!handshake_started) {
                        client.connect(
                                new InetSocketAddress("localhost", port));
                        client.getInputStream().read();
                        handshake_started = true;
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) { }
            }
            //System.out.println("===== client has been stopped");
        }

        public boolean handshakeStarted() {
            return handshake_started;
        }

        public void close() throws IOException {
            closed = true;
            client.close();
        }

    };

    public static Test suite() {
        return new TestSuite(SSLServerSocketImplTest.class);
    }

}
