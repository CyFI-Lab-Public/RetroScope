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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLSocketImplTest test
 */
public class SSLSocketImplTest extends TestCase {

    // turn on/off the debug logging
    private static boolean doLog = false;

    /**
     * Sets up the test case.
     */
    @Override
    public void setUp() throws Exception {
        if (doLog) {
            System.out.println("");
            System.out.println("========================");
            System.out.println("====== Running the test: " + getName());
        }
    }

    private SSLSocket createSSLSocket() throws Exception {
        return new SSLSocketImpl(JSSETestData.getSSLParameters());
    }

    private SSLSocket createSSLSocket(int port) throws Exception {
        return new SSLSocketImpl("localhost", port,
                JSSETestData.getSSLParameters());
    }

    /**
     * SSLSocketImpl(SSLParameters sslParameters) method testing.
     */
    public void testSSLSocketImpl1() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = new SSLSocketImpl(JSSETestData.getSSLParameters());
            socket.connect(
                    new InetSocketAddress("localhost", server.getPort()));
            ((SSLSocketImpl) socket).init();
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 10*500 ms for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLSocketImpl(String host, int port, SSLParameters sslParameters) method
     * testing.
     */
    public void testSSLSocketImpl2() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = new SSLSocketImpl("localhost",
                    server.getPort(), JSSETestData.getSSLParameters());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLSocketImpl(String host, int port, InetAddress localHost, int
     * localPort, SSLParameters sslParameters) method testing.
     */
    public void testSSLSocketImpl3() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = new SSLSocketImpl(
                    "localhost",
                    server.getPort(),
                    InetAddress.getByName("localhost"),
                    0, JSSETestData.getSSLParameters());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLSocketImpl(InetAddress host, int port, SSLParameters sslParameters)
     * method testing.
     */
    public void testSSLSocketImpl4() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = new SSLSocketImpl(
                    InetAddress.getByName("localhost"),
                    server.getPort(),
                    JSSETestData.getSSLParameters());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * SSLSocketImpl(InetAddress address, int port, InetAddress localAddress,
     * int localPort, SSLParameters sslParameters) method testing.
     */
    public void testSSLSocketImpl5() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = new SSLSocketImpl(
                    InetAddress.getByName("localhost"),
                    server.getPort(),
                    InetAddress.getByName("localhost"),
                    0, JSSETestData.getSSLParameters());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * getSupportedCipherSuites() method testing.
     */
    public void testGetSupportedCipherSuites() throws Exception {
        SSLSocket socket = createSSLSocket();
        String[] supported = socket.getSupportedCipherSuites();
        assertNotNull(supported);
        supported[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        supported = socket.getEnabledCipherSuites();
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
        SSLSocket socket = createSSLSocket();
        String[] enabled = socket.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = socket.getSupportedCipherSuites();
        for (int i=0; i<enabled.length; i++) {
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
        socket.setEnabledCipherSuites(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, 0,
                    enabled, 0, supported.length-i);
            socket.setEnabledCipherSuites(enabled);
            String[] result = socket.getEnabledCipherSuites();
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
        SSLSocket socket = createSSLSocket();
        String[] enabled = socket.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = socket.getSupportedCipherSuites();
        for (int i=0; i<enabled.length; i++) {
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
        socket.setEnabledCipherSuites(supported);
        socket.setEnabledCipherSuites(enabled);
        socket.setEnabledCipherSuites(supported);
        String[] more_than_supported = new String[supported.length+1];
        for (int i=0; i<supported.length+1; i++) {
            more_than_supported[i]
                = "NOT_SUPPORTED_CIPHER_SUITE";
            System.arraycopy(supported, 0,
                    more_than_supported, 0, i);
            System.arraycopy(supported, i,
                    more_than_supported, i+1, supported.length-i);
            try {
                socket.setEnabledCipherSuites(more_than_supported);
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        }
        enabled = socket.getEnabledCipherSuites();
        enabled[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        enabled = socket.getEnabledCipherSuites();
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
        SSLSocket socket = createSSLSocket();
        String[] supported = socket.getSupportedProtocols();
        assertNotNull(supported);
        assertFalse(supported.length == 0);
        supported[0] = "NOT_SUPPORTED_PROTOCOL";
        supported = socket.getSupportedProtocols();
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
        SSLSocket socket = createSSLSocket();
        String[] enabled = socket.getEnabledProtocols();
        assertNotNull(enabled);
        String[] supported = socket.getSupportedProtocols();
        for (int i=0; i<enabled.length; i++) {
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
        socket.setEnabledProtocols(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, i,
                    enabled, 0, supported.length-i);
            socket.setEnabledProtocols(enabled);
            String[] result = socket.getEnabledProtocols();
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
        SSLSocket socket = createSSLSocket();
        String[] enabled = socket.getEnabledProtocols();
        assertNotNull(enabled);
        String[] supported = socket.getSupportedProtocols();
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
        socket.setEnabledProtocols(supported);
        socket.setEnabledProtocols(enabled);
        socket.setEnabledProtocols(supported);
        String[] more_than_supported = new String[supported.length+1];
        for (int i=0; i<supported.length+1; i++) {
            more_than_supported[i]
                = "NOT_SUPPORTED_PROTOCOL";
            System.arraycopy(supported, 0,
                    more_than_supported, 0, i);
            System.arraycopy(supported, i,
                    more_than_supported, i+1, supported.length-i);
            try {
                socket.setEnabledProtocols(more_than_supported);
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        }
        enabled = socket.getEnabledProtocols();
        enabled[0] = "NOT_SUPPORTED_PROTOCOL";
        enabled = socket.getEnabledProtocols();
        for (int i=0; i<enabled.length; i++) {
            if ("NOT_SUPPORTED_PROTOCOL".equals(enabled[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    private static class Server extends Thread {

        private final ServerSocket server;
        private boolean closed;
        private boolean handshake_started = false;
        private Socket endpoint = null;

        public Server() throws IOException {
            super();
            server = new ServerSocket(0);
            server.setSoTimeout(1000);
        }

        public int getPort() {
            return server.getLocalPort();
        }

        @Override
        public void run() {
            while (!closed) {
                try {
                    if (doLog) {
                        System.out.print(".");
                    }
                    if (!handshake_started) {
                        endpoint = server.accept();
                        endpoint.getInputStream().read();
                        handshake_started = true;
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (endpoint != null) {
                try {
                    endpoint.close();
                } catch (IOException e) { }
            }
        }

        public boolean handshakeStarted() {
            return handshake_started;
        }

        public void close() throws IOException {
            closed = true;
            server.close();
        }

    };

    /**
     * setUseClientMode(boolean mode) method testing.
     * getUseClientMode() method testing.
     */
    public void testSetGetUseClientMode() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = createSSLSocket(server.getPort());

            socket.setUseClientMode(false);
            assertFalse("Result does not correspond to expected",
                    socket.getUseClientMode());
            socket.setUseClientMode(true);
            assertTrue("Result does not correspond to expected",
                    socket.getUseClientMode());

            server.start();
            final SSLSocket s = socket;
            new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }.start();

            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
            }

            try {
                socket.setUseClientMode(false);
                server.close();
                socket.close();
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * setNeedClientAuth(boolean need) method testing.
     * getNeedClientAuth() method testing.
     */
    public void testSetGetNeedClientAuth() throws Exception {
        SSLSocket socket = createSSLSocket();

        socket.setWantClientAuth(true);
        socket.setNeedClientAuth(false);
        assertFalse("Result does not correspond to expected",
                socket.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                socket.getWantClientAuth());
        socket.setWantClientAuth(true);
        socket.setNeedClientAuth(true);
        assertTrue("Result does not correspond to expected",
                socket.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                socket.getWantClientAuth());
    }

    /**
     * setWantClientAuth(boolean want) method testing.
     * getWantClientAuth() method testing.
     */
    public void testSetGetWantClientAuth() throws Exception {
        SSLSocket socket = createSSLSocket();

        socket.setNeedClientAuth(true);
        socket.setWantClientAuth(false);
        assertFalse("Result does not correspond to expected",
                socket.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                socket.getNeedClientAuth());
        socket.setNeedClientAuth(true);
        socket.setWantClientAuth(true);
        assertTrue("Result does not correspond to expected",
                socket.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                socket.getNeedClientAuth());
    }

    /**
     * setEnableSessionCreation(boolean flag) method testing.
     * getEnableSessionCreation() method testing.
     */
    public void testSetGetEnableSessionCreation() throws Exception {
        SSLSocket socket = createSSLSocket();

        socket.setEnableSessionCreation(false);
        assertFalse("Result does not correspond to expected",
                socket.getEnableSessionCreation());
        socket.setEnableSessionCreation(true);
        assertTrue("Result does not correspond to expected",
                socket.getEnableSessionCreation());
    }

    /**
     * getSession() method testing.
     */
    public void testGetSession() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = createSSLSocket(server.getPort());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            final SSLSession[] session = new SSLSession[1];
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        session[0] = s.getSession();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("getSession method did not start a handshake");
                }
            }

            server.close(); // makes error during the handshake
            thread.join();
            if ((session[0] == null) ||
                (!session[0].getCipherSuite()
                 .endsWith("_NULL_WITH_NULL_NULL"))) {
                fail("Returned session is null "
                     + "or not TLS_NULL_WITH_NULL_NULL");
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * addHandshakeCompletedListener( HandshakeCompletedListener listener)
     * method testing.
     * removeHandshakeCompletedListener( HandshakeCompletedListener listener)
     * method testing.
     */
    public void testAddRemoveHandshakeCompletedListener() throws Exception {
        HandshakeCompletedListener listener =
            new HandshakeCompletedListener() {
                public void handshakeCompleted(
                        HandshakeCompletedEvent event) { }
            };
        SSLSocket socket = createSSLSocket();
        socket.addHandshakeCompletedListener(listener);
        try {
            socket.addHandshakeCompletedListener(null);
            fail("Expected IllegalArgumentException was not thrown.");
        } catch (IllegalArgumentException e) { }
        try {
            socket.removeHandshakeCompletedListener(null);
            fail("Expected IllegalArgumentException was not thrown.");
        } catch (IllegalArgumentException e) { }
        try {
            socket.removeHandshakeCompletedListener(
                    new HandshakeCompletedListener() {
                        public void handshakeCompleted(
                                HandshakeCompletedEvent event) { }
                    });
            fail("Expected IllegalArgumentException was not thrown.");
        } catch (IllegalArgumentException e) { }
        try {
            socket.removeHandshakeCompletedListener(listener);
        } catch (IllegalArgumentException e) {
            fail("Unxpected IllegalArgumentException was thrown.");
        }
    }

    /**
     * startHandshake() method testing.
     */
    public void testStartHandshake() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = createSSLSocket(server.getPort());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            final Exception[] exception = new Exception[1];
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.startHandshake();
                    } catch (Exception e) {
                        exception[0] = e;
                    }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    fail("Handshake was not started");
                }
            }

            server.close(); // makes error during the handshake
            thread.join();
            if (exception[0] == null) {
                fail("Expected IOException was not thrown");
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * getInputStream() method testing.
     */
    public void testGetInputStream() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = createSSLSocket(server.getPort());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.getInputStream().read(); // should start handshake
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * getOutputStream() method testing.
     */
    public void testGetOutputStream() throws Exception {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();

            socket = createSSLSocket(server.getPort());
            socket.setUseClientMode(true);

            server.start();
            final SSLSocket s = socket;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        s.getOutputStream().write(0); // should start handshake
                    } catch (Exception e) { }
                }
            };

            thread.start();

            int timeout = 10; // wait no more than 5 seconds for handshake
            while (!server.handshakeStarted()) {
                // wait for handshake start
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
                timeout--;
                if (timeout < 0) {
                    try {
                        server.close();
                    } catch (IOException ex) { }
                    try {
                        socket.close();
                    } catch (IOException ex) { }
                    fail("Handshake was not started");
                }
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * sendUrgentData(int data) method testing.
     */
    public void testSendUrgentData() {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();
            socket = createSSLSocket(server.getPort());

            socket.sendUrgentData(0);
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("Trowed exception: "+e.getMessage());
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * setOOBInline(boolean on) method testing.
     */
    public void testSetOOBInline() {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();
            socket = createSSLSocket(server.getPort());

            socket.setOOBInline(true);
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("Trowed exception: "+e.getMessage());
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * shutdownOutput() method testing.
     */
    public void testShutdownOutput() {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();
            socket = createSSLSocket(server.getPort());

            socket.shutdownOutput();
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("Trowed exception: "+e.getMessage());
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * shutdownInput() method testing.
     */
    public void testShutdownInput() {
        Server server = null;
        SSLSocket socket = null;
        try {
            server = new Server();
            socket = createSSLSocket(server.getPort());

            socket.shutdownInput();
            fail("Expected exception was not thrown");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("Trowed exception: "+e.getMessage());
            }
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException ex) { }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) { }
            }
        }
    }

    /**
     * toString() method testing.
     */
    public void testToString() throws Exception {
        SSLSocket socket = createSSLSocket();
        assertNotNull("String representation is null", socket.toString());
    }

    public static Test suite() {
        return new TestSuite(SSLSocketImplTest.class);
    }

}
