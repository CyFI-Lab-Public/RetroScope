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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLSocketImplTest
 */
public class SSLSocketFactoriesTest extends TestCase {

    // turn on/off the debug logging
    private boolean doLog = false;

    /**
     * Sets up the test case.
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (doLog) {
            System.out.println("========================");
            System.out.println("====== Running the test: " + getName());
            System.out.println("========================");
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests default initialized factories.
     */
    public void testDefaultInitialized() throws Exception {

        SSLServerSocketFactory ssfactory =
            (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLSocketFactory sfactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

        assertNotNull(ssfactory.getDefaultCipherSuites());
        assertNotNull(ssfactory.getSupportedCipherSuites());
        assertNotNull(ssfactory.createServerSocket());

        assertNotNull(sfactory.getDefaultCipherSuites());
        assertNotNull(sfactory.getSupportedCipherSuites());
        assertNotNull(sfactory.createSocket());
    }

    public void testSocketCreation() throws Throwable {
        SSLSocketFactory socketFactory
            = new SSLSocketFactoryImpl(JSSETestData.getSSLParameters());
        SSLServerSocketFactory serverSocketFactory
            = new SSLServerSocketFactoryImpl(JSSETestData.getSSLParameters());

        String[] enabled = {"TLS_RSA_WITH_RC4_128_MD5"};
        for (int i=0; i<4; i++) {
            SSLServerSocket ssocket;
            switch (i) {
                case 0:
                    if (doLog) {
                        System.out.println(
                            "*** ServerSocketFactory.createServerSocket()");
                    }
                    ssocket = (SSLServerSocket)
                        serverSocketFactory.createServerSocket();
                    ssocket.bind(null);
                    break;
                case 1:
                    if (doLog) {
                        System.out.println(
                            "*** ServerSocketFactory.createServerSocket(int)");
                    }
                    ssocket = (SSLServerSocket)
                        serverSocketFactory.createServerSocket(0);
                    break;
                case 2:
                    if (doLog) {
                        System.out.println(
                        "*** ServerSocketFactory.createServerSocket(int,int)");
                    }
                    ssocket = (SSLServerSocket)
                        serverSocketFactory.createServerSocket(0, 6);
                    break;
                default:
                    if (doLog) {
                        System.out.println("*** ServerSocketFactory."
                                + "createServerSocket(int,int,InetAddress)");
                    }
                    ssocket = (SSLServerSocket)
                        serverSocketFactory.createServerSocket(0, 6, null);
                    break;
            }
            ssocket.setUseClientMode(false);
            ssocket.setEnabledCipherSuites(enabled);
            for (int j=0; j<6; j++) {
                SSLSocket csocket;
                switch (j) {
                    case 0:
                        if (doLog) {
                            System.out.println(
                                "=== SocketFactory.createSocket()");
                        }
                        csocket = (SSLSocket) socketFactory.createSocket();
                        csocket.connect(
                                new InetSocketAddress("localhost",
                                    ssocket.getLocalPort()));
                        break;
                    case 1:
                        if (doLog) {
                            System.out.println(
                                "=== SocketFactory.createSocket(String,int)");
                        }
                        csocket = (SSLSocket)
                            socketFactory.createSocket("localhost",
                                    ssocket.getLocalPort());
                        break;
                    case 2:
                        if (doLog) {
                            System.out.println("=== SocketFactory.createSocket("
                                    + "String,int,InetAddress,int)");
                        }
                        csocket = (SSLSocket)
                            socketFactory.createSocket("localhost",
                                ssocket.getLocalPort(),
                                InetAddress.getByName("localhost"), 0);
                        break;
                    case 3:
                        if (doLog) {
                            System.out.println("=== SocketFactory.createSocket("
                                    + "InetAddress,int)");
                        }
                        csocket = (SSLSocket) socketFactory.createSocket(
                                InetAddress.getByName("localhost"),
                                ssocket.getLocalPort());
                        break;
                    case 4:
                        if (doLog) {
                            System.out.println("=== SocketFactory.createSocket("
                                    + "InetAddress,int,InetAddress,int)");
                        }
                        csocket = (SSLSocket) socketFactory.createSocket(
                                InetAddress.getByName("localhost"),
                                ssocket.getLocalPort(),
                                InetAddress.getByName("localhost"),
                                0);
                        break;
                    default:
                        if (doLog) {
                            System.out.println(
                                    "=== SSLSocketFactory.createSocket("
                                    + "socket,String,int,boolean)");
                        }
                        Socket socket = new Socket(
                                InetAddress.getByName("localhost"),
                                ssocket.getLocalPort());
                        csocket = (SSLSocket) socketFactory.createSocket(
                                socket, "localhost", ssocket.getLocalPort(),
                                true);
                        break;

                }
                csocket.setUseClientMode(true);
                csocket.setEnabledCipherSuites(enabled);
                doTest(ssocket, csocket);
            }
        }
    }

    /**
     * SSLSocketFactory.getSupportedCipherSuites() method testing.
     */
    public void testGetSupportedCipherSuites1() throws Exception {
        SSLSocketFactory socketFactory
            = new SSLSocketFactoryImpl(JSSETestData.getSSLParameters());
        String[] supported = socketFactory.getSupportedCipherSuites();
        assertNotNull(supported);
        supported[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        supported = socketFactory.getSupportedCipherSuites();
        for (int i=0; i<supported.length; i++) {
            if ("NOT_SUPPORTED_CIPHER_SUITE".equals(supported[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * SSLServerSocketFactory.getSupportedCipherSuites() method testing.
     */
    public void testGetSupportedCipherSuites2() throws Exception {
        SSLServerSocketFactory serverSocketFactory
            = new SSLServerSocketFactoryImpl(JSSETestData.getSSLParameters());
        String[] supported = serverSocketFactory.getSupportedCipherSuites();
        assertNotNull(supported);
        supported[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        supported = serverSocketFactory.getSupportedCipherSuites();
        for (int i=0; i<supported.length; i++) {
            if ("NOT_SUPPORTED_CIPHER_SUITE".equals(supported[i])) {
                fail("Modification of the returned result "
                        + "causes the modification of the internal state");
            }
        }
    }

    /**
     * SSLSocketFactory.getDefaultCipherSuites() method testing.
     */
    public void testGetDefaultCipherSuites1() throws Exception {
        SSLSocketFactory socketFactory
            = new SSLSocketFactoryImpl(JSSETestData.getSSLParameters());
        String[] supported = socketFactory.getSupportedCipherSuites();
        String[] defaultcs = socketFactory.getDefaultCipherSuites();
        assertNotNull(supported);
        assertNotNull(defaultcs);
        for (int i=0; i<defaultcs.length; i++) {
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (defaultcs[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Default suite does not belong to the set "
                        + "of supported cipher suites: " + defaultcs[i]);
            }
        }
    }

    /**
     * SSLServerSocketFactory.getDefaultCipherSuites() method testing.
     */
    public void testGetDefaultCipherSuites2() throws Exception {
        SSLServerSocketFactory serverSocketFactory
            = new SSLServerSocketFactoryImpl(JSSETestData.getSSLParameters());
        String[] supported = serverSocketFactory.getSupportedCipherSuites();
        String[] defaultcs = serverSocketFactory.getDefaultCipherSuites();
        assertNotNull(supported);
        assertNotNull(defaultcs);
        for (int i=0; i<defaultcs.length; i++) {
            found: {
                for (int j=0; j<supported.length; j++) {
                    if (defaultcs[i].equals(supported[j])) {
                        break found;
                    }
                }
                fail("Default suite does not belong to the set "
                        + "of supported cipher suites: " + defaultcs[i]);
            }
        }
    }

    /**
     * Performs SSL connection between the sockets
     * @return
     */
    public void doTest(SSLServerSocket ssocket, SSLSocket csocket)
            throws Throwable {
        final String server_message = "Hello from SSL Server Socket!";
        final String client_message = "Hello from SSL Socket!";
        Thread server = null;
        Thread client = null;
        final Throwable[] throwed = new Throwable[1];
        try {
            final SSLServerSocket ss = ssocket;
            final SSLSocket s = csocket;
            server = new Thread() {
                @Override
                public void run() {
                    InputStream is = null;
                    OutputStream os = null;
                    SSLSocket s = null;
                    try {
                        s = (SSLSocket) ss.accept();
                        if (doLog) {
                            System.out.println("Socket accepted: " + s);
                        }
                        is = s.getInputStream();
                        os = s.getOutputStream();
                        // send the message to the client
                        os.write(server_message.getBytes());
                        // read the response
                        byte[] buff = new byte[client_message.length()];
                        int len = is.read(buff);
                        if (doLog) {
                            System.out.println("Received message of length "
                                + len + ": '" + new String(buff, 0, len)+"'");
                        }
                        assertTrue("Read message does not equal to expected",
                                Arrays.equals(client_message.getBytes(), buff));
                        os.write(-1);
                        assertEquals("Read data differs from expected",
                                255, is.read());
                        if (doLog) {
                            System.out.println("Server is closed: "
                                    +s.isClosed());
                        }
                        assertEquals("Returned value should be -1",
                        // initiate an exchange of closure alerts
                                -1, is.read());
                        if (doLog) {
                            System.out.println("Server is closed: "
                                    +s.isClosed());
                        }
                        assertEquals("Returned value should be -1",
                        // initiate an exchange of closure alerts
                                -1, is.read());
                    } catch (Throwable e) {
                        synchronized (throwed) {
                            if (doLog) {
                                e.printStackTrace();
                            }
                            if (throwed[0] == null) {
                                throwed[0] = e;
                            }
                        }
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                        } catch (IOException ex) {}
                        try {
                            if (os != null) {
                                os.close();
                            }
                        } catch (IOException ex) {}
                        try {
                            if (s != null) {
                                s.close();
                            }
                        } catch (IOException ex) {}
                    }
                }
            };

            client = new Thread() {
                @Override
                public void run() {
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        assertTrue("Client was not connected", s.isConnected());
                        if (doLog) {
                            System.out.println("Client connected");
                        }
                        is = s.getInputStream();
                        os = s.getOutputStream();
                        s.startHandshake();
                        if (doLog) {
                            System.out.println("Client: HS was done");
                        }
                        // read the message from the server
                        byte[] buff = new byte[server_message.length()];
                        int len = is.read(buff);
                        if (doLog) {
                            System.out.println("Received message of length "
                                + len + ": '" + new String(buff, 0, len)+"'");
                        }
                        assertTrue("Read message does not equal to expected",
                                Arrays.equals(server_message.getBytes(), buff));
                        // send the response
                        buff = (" "+client_message+" ").getBytes();
                        os.write(buff, 1, buff.length-2);
                        assertEquals("Read data differs from expected",
                                255, is.read());
                        os.write(-1);
                        if (doLog) {
                            System.out.println("Client is closed: "
                                    +s.isClosed());
                        }
                        s.close();
                        if (doLog) {
                            System.out.println("Client is closed: "
                                    +s.isClosed());
                        }
                    } catch (Throwable e) {
                        synchronized (throwed) {
                            if (doLog) {
                                e.printStackTrace();
                            }
                            if (throwed[0] == null) {
                                throwed[0] = e;
                            }
                        }
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                        } catch (IOException ex) {}
                        try {
                            if (os != null) {
                                os.close();
                            }
                        } catch (IOException ex) {}
                        try {
                            if (s != null) {
                                s.close();
                            }
                        } catch (IOException ex) {}
                    }
                }
            };

            server.start();
            client.start();

            while (server.isAlive() || client.isAlive()) {
                if (throwed[0] != null) {
                    throw throwed[0];
                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) { }
            }
        } finally {
            if (server != null) {
                server.stop();
            }
            if (client != null) {
                client.stop();
            }
        }
        if (throwed[0] != null) {
            throw throwed[0];
        }
    }

    public static Test suite() {
        return new TestSuite(SSLSocketFactoriesTest.class);
    }

}
