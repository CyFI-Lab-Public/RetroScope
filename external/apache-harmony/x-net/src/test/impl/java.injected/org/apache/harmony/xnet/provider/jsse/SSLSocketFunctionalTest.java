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
import java.util.Arrays;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLSocketImplTest
 */
public class SSLSocketFunctionalTest extends TestCase {

    /**
     * The cipher suites used for functionality testing.
     */
    private String[] cipher_suites = {
        "RSA_WITH_RC4_128_MD5",
        "RSA_WITH_DES_CBC_SHA",
        "DH_anon_EXPORT_WITH_DES40_CBC_SHA"
    };

    // turn on/off the debug logging
    private boolean doLog = false;

    /**
     * Sets up the test case.
     */
    @Override
    public void setUp() throws Exception {
        if (doLog) {
            System.out.println("========================");
            System.out.println("====== Running the test: " + getName());
            System.out.println("========================");
        }
    }

    public void testContextInitialized2() throws Throwable {
        doTestSelfInteraction(JSSETestData.getContext());
    }

    public void doTestInteraction(SSLContext context, SSLContext ctx_other)
            throws Throwable {
        SSLContext ctx1, ctx2;

        ctx1 = context;
        ctx2 = ctx_other;

        int k=1;

        SSLServerSocket ssocket = (SSLServerSocket) ctx1
            .getServerSocketFactory().createServerSocket(0);
        ssocket.setUseClientMode(false);
        ssocket.setEnabledCipherSuites(
                ((k & 1) > 0)
                    ? new String[] {"TLS_"+cipher_suites[0]}
                    : new String[] {"SSL_"+cipher_suites[0]});

        SSLSocket csocket = (SSLSocket) ctx2
            .getSocketFactory().createSocket("localhost",
                    ssocket.getLocalPort());
        csocket.setEnabledProtocols(new String[] {"TLSv1"});
        csocket.setUseClientMode(true);
        csocket.setEnabledCipherSuites(
                (((k & 2) >> 1) > 0)
                    ? new String[] {"TLS_"+cipher_suites[0]}
                    : new String[] {"SSL_"+cipher_suites[0]});
        doTest(ssocket, csocket);
    }

    public void _doTestInteraction(SSLContext context, SSLContext ctx_other)
            throws Throwable {
        for (int i=0; i<cipher_suites.length; i++) {
            if (doLog) {
                System.out.println("======== Checking the work on cipher: "
                    + cipher_suites[i]);
            }
            SSLContext ctx1, ctx2;
            // k: 00, 01, 10, 11;
            // where 1 means implementation under the test,
            // 0 - another implementation to interract with
            for (int k=0; k<4; k++) {
                if (doLog) {
                    System.out.println("======== "+(k & 1)+" "+((k & 2) >> 1));
                }
                ctx1 = ((k & 1) > 0) ? context : ctx_other;
                ctx2 = (((k & 2) >> 1) > 0) ? context : ctx_other;

                SSLServerSocket ssocket = (SSLServerSocket) ctx1
                    .getServerSocketFactory().createServerSocket(0);
                ssocket.setUseClientMode(false);
                ssocket.setEnabledCipherSuites(
                        ((k & 1) > 0)
                            ? new String[] {"TLS_"+cipher_suites[i]}
                            : new String[] {"SSL_"+cipher_suites[i]});

                SSLSocket csocket = (SSLSocket) ctx2
                    .getSocketFactory().createSocket("localhost",
                            ssocket.getLocalPort());
                csocket.setEnabledProtocols(new String[] {"TLSv1"});
                csocket.setUseClientMode(true);
                csocket.setEnabledCipherSuites(
                        (((k & 2) >> 1) > 0)
                            ? new String[] {"TLS_"+cipher_suites[i]}
                            : new String[] {"SSL_"+cipher_suites[i]});
                doTest(ssocket, csocket);
            }
        }
    }

    /**
     * Tests the interaction with other implementation.
     */
    public void doTestSelfInteraction(SSLContext context)
            throws Throwable {
        String[] protocols = {"SSLv3", "TLSv1"};
        for (int i=0; i<cipher_suites.length; i++) {
            for (int j=0; j<2; j++) {
                if (doLog) {
                    System.out.println("======= " + cipher_suites[i]);
                }
                SSLServerSocket ssocket = (SSLServerSocket) context
                    .getServerSocketFactory().createServerSocket(0);
                ssocket.setUseClientMode(false);
                ssocket.setEnabledProtocols(new String[] {protocols[j]});
                ssocket.setEnabledCipherSuites(
                        new String[] {"TLS_"+cipher_suites[i]});

                SSLSocket csocket = (SSLSocket) context
                    .getSocketFactory().createSocket("localhost",
                            ssocket.getLocalPort());
                csocket.setEnabledProtocols(new String[] {protocols[j]});
                csocket.setUseClientMode(true);
                csocket.setEnabledCipherSuites(
                        new String[] {"TLS_"+cipher_suites[i]});

                doTest(ssocket, csocket);
            }
        }
    }

    private static class HandshakeListener
                                implements HandshakeCompletedListener {
        boolean compleated = false;

        public void handshakeCompleted(HandshakeCompletedEvent event) {
            compleated = true;
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
                            System.out.println("\n======== Closing ========");
                        }
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
        return new TestSuite(SSLSocketFunctionalTest.class);
    }

}
