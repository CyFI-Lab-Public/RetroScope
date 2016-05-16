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

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * SSLEngine implementation test.
 */
public class SSLEngineImplTest extends TestCase {

    /**
     * The cipher suites used for functionality testing.
     */
    private static final String[] cipher_suites = {
        "RSA_WITH_RC4_128_MD5",
        "RSA_WITH_DES_CBC_SHA",
        "DH_anon_EXPORT_WITH_DES40_CBC_SHA"
    };

    /**
     * Test logging switch.
     */
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
            System.out.println("========================");
        }
    }

    /**
     * Tests the interaction between the engines.
     */
    public void testSelfInteraction() throws Exception {
        String[] protocols = {"SSLv3", "TLSv1"};
        for (int i=0; i<cipher_suites.length; i++) {
            for (int j=0; j<2; j++) {
                if (doLog) {
                    System.out.println("\n===== Interact over suite: "
                            + cipher_suites[i]);
                }
                SSLEngine client = getEngine();
                SSLEngine server = getEngine();
                initEngines(client, server);

                doHandshake(client, server);
                doDataExchange(client, server);
                doClose(client, server);
            }
        }
    }

    /**
     * Tests the session negotiation process.
     */
    public void testHandshake() throws Exception {
        SSLEngine client = getEngine();
        SSLEngine server = getEngine();

        initEngines(client, server);

        // checks the impossibility of initial handshake
        // with the server not allowed to session creation
        doNoRenegotiationTest(client, server, true);

        client = getEngine();
        server = getEngine();
        initEngines(client, server);

        client.setUseClientMode(true);
        server.setUseClientMode(false);

        // do initial handshake
        doHandshake(client, server);

        // client initiates rehandshake
        client.beginHandshake();
        doHandshakeImpl(client, server);

        // server initiates rehandshake
        server.beginHandshake();
        doHandshakeImpl(client, server);

        // client initiates rehandshake while server invalidates
        // used session
        server.getSession().invalidate();
        client.beginHandshake();
        doHandshakeImpl(client, server);

        // server initiates rehandshake while client invalidates
        // used session
        client.getSession().invalidate();
        server.beginHandshake();
        doHandshakeImpl(client, server);

        client.getSession().invalidate();
        server.getSession().invalidate();
        doHandshake(client, server);

        doNoRenegotiationTest(client, server, false);

        doNoRenegotiationTest(server, client, false);

        doClose(client, server);
    }

    /**
     * setNeedClientAuth(boolean need) method testing.
     * getNeedClientAuth() method testing.
     */
    public void testSetGetNeedClientAuth() throws Exception {
        SSLEngine engine = getEngine();

        engine.setWantClientAuth(true);
        engine.setNeedClientAuth(false);
        assertFalse("Result differs from expected",
                engine.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                engine.getWantClientAuth());
        engine.setWantClientAuth(true);
        engine.setNeedClientAuth(true);
        assertTrue("Result differs from expected",
                engine.getNeedClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                engine.getWantClientAuth());
    }

    /**
     * setWantClientAuth(boolean want) method testing.
     * getWantClientAuth() method testing.
     */
    public void testSetGetWantClientAuth() throws Exception {
        SSLEngine engine = getEngine();

        engine.setNeedClientAuth(true);
        engine.setWantClientAuth(false);
        assertFalse("Result differs from expected",
                engine.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                engine.getNeedClientAuth());
        engine.setNeedClientAuth(true);
        engine.setWantClientAuth(true);
        assertTrue("Result differs from expected",
                engine.getWantClientAuth());
        assertFalse("Socket did not reset its want client auth state",
                engine.getNeedClientAuth());
    }

    /**
     * getSupportedCipherSuites() method testing.
     */
    public void testGetSupportedCipherSuites() throws Exception {
        SSLEngine engine = getEngine();
        String[] supported = engine.getSupportedCipherSuites();
        assertNotNull(supported);
        supported[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        supported = engine.getEnabledCipherSuites();
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
        SSLEngine engine = getEngine();
        String[] enabled = engine.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = engine.getSupportedCipherSuites();
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
        engine.setEnabledCipherSuites(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, 0,
                    enabled, 0, supported.length-i);
            engine.setEnabledCipherSuites(enabled);
            String[] result = engine.getEnabledCipherSuites();
            if (result.length != enabled.length) {
                fail("Returned result differs from expected.");
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
        SSLEngine engine = getEngine();
        String[] enabled = engine.getEnabledCipherSuites();
        assertNotNull(enabled);
        String[] supported = engine.getSupportedCipherSuites();
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
        engine.setEnabledCipherSuites(supported);
        engine.setEnabledCipherSuites(enabled);
        engine.setEnabledCipherSuites(supported);
        String[] more_than_supported = new String[supported.length+1];
        for (int i=0; i<supported.length+1; i++) {
            more_than_supported[i]
                = "NOT_SUPPORTED_CIPHER_SUITE";
            System.arraycopy(supported, 0,
                    more_than_supported, 0, i);
            System.arraycopy(supported, i,
                    more_than_supported, i+1, supported.length-i);
            try {
                engine.setEnabledCipherSuites(more_than_supported);
                fail("Expected IllegalArgumentException was not thrown");
            } catch (IllegalArgumentException e) { }
        }
        enabled = engine.getEnabledCipherSuites();
        enabled[0] = "NOT_SUPPORTED_CIPHER_SUITE";
        enabled = engine.getEnabledCipherSuites();
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
        SSLEngine engine = getEngine();
        String[] supported = engine.getSupportedProtocols();
        assertNotNull(supported);
        assertFalse(supported.length == 0);
        supported[0] = "NOT_SUPPORTED_PROTOCOL";
        supported = engine.getSupportedProtocols();
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
        SSLEngine engine = getEngine();
        String[] enabled = engine.getEnabledProtocols();
        assertNotNull(enabled);
        String[] supported = engine.getSupportedProtocols();
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
        engine.setEnabledProtocols(supported);
        for (int i=0; i<supported.length; i++) {
            enabled = new String[supported.length - i];
            System.arraycopy(supported, i,
                    enabled, 0, supported.length-i);
            engine.setEnabledProtocols(enabled);
            String[] result = engine.getEnabledProtocols();
            if (result.length != enabled.length) {
                fail("Returned result differs from expected.");
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
     * setUseClientMode(boolean mode) method testing.
     * getUseClientMode() method testing.
     */
    public void testSetGetUseClientMode() throws Exception {
        SSLEngine engine = getEngine();

        engine.setUseClientMode(false);
        assertFalse("Result differs from expected",
                engine.getUseClientMode());
        engine.setUseClientMode(true);
        assertTrue("Result differs from expected",
                engine.getUseClientMode());

        engine.beginHandshake();
        try {
            engine.setUseClientMode(false);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) { }

        engine.wrap(ByteBuffer.allocate(0), ByteBuffer.allocate(
                engine.getSession().getPacketBufferSize()));
        try {
            engine.setUseClientMode(false);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) { }
     }

    /**
     * setEnableSessionCreation(boolean flag) method testing.
     * getEnableSessionCreation() method testing.
     */
    public void testSetGetEnableSessionCreation() throws Exception {
        SSLEngine engine = getEngine();

        engine.setEnableSessionCreation(false);
        assertFalse("Result differs from expected",
                engine.getEnableSessionCreation());
        engine.setEnableSessionCreation(true);
        assertTrue("Result differs from expected",
                engine.getEnableSessionCreation());
    }

    /**
     * getSession() method testing.
     */
    public void testGetSession() throws Exception {
        SSLEngine engine = getEngine();

        SSLSession session = engine.getSession();
        if ((session == null)
                || (!session.getCipherSuite()
                    .endsWith("_NULL_WITH_NULL_NULL"))) {
            fail("Returned session is null "
                    + "or not TLS_NULL_WITH_NULL_NULL");
        }
    }

    /**
     * beginHandshake() method testing
     *
     */
    public void testBeginHandshake() throws Exception {
        SSLEngine engine = getEngine();
        assertEquals("Incorrect initial handshake status",
                engine.getHandshakeStatus(),
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
        try {
            engine.beginHandshake();
            fail("Expected IllegalStateException was not thrown");
        } catch (IllegalStateException e) { }

        engine = getEngine();
        engine.setUseClientMode(false);
        engine.beginHandshake();
        assertEquals("Incorrect initial handshake status",
                engine.getHandshakeStatus(),
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

        engine = getEngine();
        engine.setUseClientMode(true);
        engine.beginHandshake();
        assertEquals("Incorrect initial handshake status",
                engine.getHandshakeStatus(),
                SSLEngineResult.HandshakeStatus.NEED_WRAP);
    }

    /**
     * closeOutbound() method testing.
     */
    public void testCloseOutbound() throws Exception {
        SSLEngine engine = getEngine();
        assertFalse(engine.isOutboundDone());
        engine.closeOutbound();
        SSLEngineResult result = engine.wrap(ByteBuffer.allocate(0),
                ByteBuffer.allocate(20000));
        assertEquals("Incorrect status", result.getStatus(),
                SSLEngineResult.Status.CLOSED);
        assertEquals("Incorrect status", result.getHandshakeStatus(),
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
        try {
            // should throw SSLException "engine already closed"
            engine.beginHandshake();
            fail("Expected exception was not thrown.");
        } catch (SSLException e) { }
        assertTrue(engine.isOutboundDone());
    }

    /**
     * closeInbound() method testing.
     */
    public void testCloseInbound() throws Exception {
        SSLEngine engine = getEngine();
        assertFalse(engine.isInboundDone());
        engine.closeInbound();
        SSLEngineResult result = engine.wrap(ByteBuffer.allocate(0),
                ByteBuffer.allocate(20000));
        assertEquals("Incorrect status", result.getStatus(),
                SSLEngineResult.Status.CLOSED);
        assertEquals("Incorrect status", result.getHandshakeStatus(),
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);
        try {
            // should throw SSLException "engine already closed"
            engine.beginHandshake();
            fail("Expected exception was not thrown.");
        } catch (SSLException e) { }
        assertTrue(engine.isInboundDone());
    }

    /**
     * closeInbound() method testing.
     * Tests error processing in the case of unexpected closeInbound.
     */
    public void testCloseInbound2() throws Exception {
        SSLEngine client = getEngine();
        SSLEngine server = getEngine();
        initEngines(client, server);

        int packetBufferSize =
            client.getSession().getPacketBufferSize();
        int applicationBufferSize =
            server.getSession().getApplicationBufferSize();

        ByteBuffer buffer = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer app_data_buffer = ByteBuffer.allocate(applicationBufferSize);

        client.setUseClientMode(true);
        server.setUseClientMode(false);

        doHandshake(client, server);

        if (doLog) {
            System.out.println("\nError processing test:");
        }
        try {
            // should cause SSLException, prepare fatal alert "internal error",
            // and set HandshakeStatus to NEED_WRAP
            // (to send alert to another side)
            server.closeInbound();
            fail("Expected exception was not thrown.");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("Server threw exception: "
                        + e.getMessage());
            }
            // e.printStackTrace();
            // should do nothing
            server.closeInbound();
            assertEquals("Unexpected status:",
                    SSLEngineResult.HandshakeStatus.NEED_WRAP,
                    server.getHandshakeStatus());

            if (doLog) {
                System.out.println("Wrapping of the alert message");
            }
            SSLEngineResult result = null;
            print(result = server.wrap(ByteBuffer.allocate(0), buffer));
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.CLOSED,
                    result.getStatus());
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                    result.getHandshakeStatus());
            assertEquals(
                "The length of the consumed data differs from expected",
                0, result.bytesConsumed());
            assertTrue(
                "The length of the produced data differs from expected",
                result.bytesProduced() > 0);
            // tune buffer to be read
            buffer.flip();
            try {
                // should rethrow the SSLException "internal error"
                print(client.unwrap(buffer, app_data_buffer));
                fail("Expected exception was not thrown.");
            } catch (Exception ex) {
                if (doLog) {
                    System.out.println("Client rethrew received alert: "
                            + ex.getMessage());
                }
                // NOT_HANDSHAKING..
                assertEquals("Unexpected status of operation:",
                        SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                        client.getHandshakeStatus());
                client.closeOutbound();
                // NEED_WRAP.. should it be so? it contradicts to the TLS spec:
                // "Upon transmission or receipt of an fatal alert message, both
                // parties immediately close the connection. Servers and clients
                // are required to forget any session-identifiers, keys, and
                // secrets associated with a failed connection."
                // So in this case we expect NOT_HANDSHAKING
                assertEquals("Unexpected status of operation:",
                        SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                        client.getHandshakeStatus());
                assertTrue("Outbound should be closed.",
                        client.isOutboundDone());
                assertTrue("Inbound should be closed.",
                        client.isInboundDone());
            }
        }
    }

    /**
     * closeInbound() method testing.
     * Tests error processing
     */
    public void testErrorProcessing() throws Exception {
        SSLEngine client = getEngine();
        SSLEngine server = getEngine();
        initEngines(client, server);

        int packetBufferSize =
            client.getSession().getPacketBufferSize();
        int applicationBufferSize =
            server.getSession().getApplicationBufferSize();

        ByteBuffer buffer = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer app_data_buffer = ByteBuffer.allocate(applicationBufferSize);

        client.setUseClientMode(true);
        server.setUseClientMode(false);

        doHandshake(client, server);

        if (doLog) {
            System.out.println("\nError processing test:");
        }
        try {
            print(server.unwrap(ByteBuffer.allocate(40), app_data_buffer));
            fail("Expected exception was not thrown.");
        } catch (Exception e) {
            if (doLog) {
                System.out.println("\nServer threw exception: "
                        +e.getMessage());
            }
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.HandshakeStatus.NEED_WRAP,
                    server.getHandshakeStatus());

            SSLEngineResult result = null;
            assertFalse("Outbound should not be closed.",
                    server.isOutboundDone());
            assertTrue("Inbound should be closed.",
                    server.isInboundDone());

            if (doLog) {
                System.out.println(
                        "\nServer tries to unwrap the data after error");
            }
            print(result =
                    server.unwrap(ByteBuffer.allocate(40), app_data_buffer));
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.CLOSED,
                    result.getStatus());
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.HandshakeStatus.NEED_WRAP,
                    result.getHandshakeStatus());
            assertEquals(
                "The length of the consumed data differs from expected",
                0, result.bytesConsumed());
            assertEquals(
                "The length of the produced data differs from expected",
                0, result.bytesProduced());

            if (doLog) {
                System.out.println("\nServer wraps the fatal alert");
            }
            print(result = server.wrap(ByteBuffer.allocate(0), buffer));
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.CLOSED,
                    result.getStatus());
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                    result.getHandshakeStatus());
            assertEquals(
                "The length of the consumed data differs from expected",
                0, result.bytesConsumed());
            assertTrue(
                "The length of the produced data differs from expected",
                result.bytesProduced() > 0);

            assertTrue("Outbound should be closed.",
                    server.isOutboundDone());
            assertTrue("Inbound should be closed.",
                    server.isInboundDone());

            buffer.flip();
            try {
                if (doLog) {
                    System.out.println("\nClient unwraps the fatal alert");
                }
                print(client.unwrap(buffer, app_data_buffer));
                fail("Expected exception was not thrown.");
            } catch (Exception ex) {
                if (doLog) {
                    System.out.println("\nClient rethrew the exception: "
                            + ex.getMessage());
                }
                // NOT_HANDSHAKING..
                assertEquals("Unexpected status of operation:",
                        SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                        client.getHandshakeStatus());
                client.closeOutbound();
                // NEED_WRAP.. should it be so? it contradicts to the TLS spec:
                // "Upon transmission or receipt of an fatal alert message, both
                // parties immediately close the connection. Servers and clients
                // are required to forget any session-identifiers, keys, and
                // secrets associated with a failed connection."
                // So in this case we expect NOT_HANDSHAKING
                assertEquals("Unexpected status of operation:",
                        SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                        client.getHandshakeStatus());
                assertTrue("Outbound should be closed.",
                        client.isOutboundDone());
                assertTrue("Inbound should be closed.",
                        client.isInboundDone());
            }
        }
    }


    // --------------------------------------------------------------------
    // ------------------------ Staff methods -----------------------------
    // --------------------------------------------------------------------

    /*
     * Performs the handshake over the specified engines
     */
    private void doHandshake(SSLEngine client,
            SSLEngine server) throws Exception {
        if (doLog) {
            System.out.println("\n--- doHandshake:");
            System.out.println("Server: "+server.getSession().getClass());
            System.out.println("Client: "+client.getSession().getClass());
        }

        client.beginHandshake();
        server.beginHandshake();

        doHandshakeImpl(client, server);
    }

    /*
     * Performs the handshake over the specified engines.
     * Note that method passes app data between the engines during
     * the handshake process.
     */
    private void doHandshakeImpl(SSLEngine client,
            SSLEngine server) throws Exception {
        if (doLog) {
            System.out.println("\n--- doHandshakeImpl:");
            System.out.println("Client's hsh status: "
                    + client.getHandshakeStatus());
            System.out.println("Client's session: "+client.getSession());
            System.out.println("Server's hsh status: "
                    + server.getHandshakeStatus());
            System.out.println("Server's session: "+server.getSession());
        }

        int packetBufferSize =
            client.getSession().getPacketBufferSize();
        int applicationBufferSize =
            server.getSession().getApplicationBufferSize();

        // buffer will contain handshake messages
        ByteBuffer clients_buffer = ByteBuffer.allocate(packetBufferSize+1000);
        ByteBuffer servers_buffer = ByteBuffer.allocate(packetBufferSize+1000);
        // buffers will contain application data messages
        ByteBuffer app_data = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer app_data_plain = ByteBuffer.allocate(applicationBufferSize);

        SSLEngine[] engines = new SSLEngine[] {client, server};
        ByteBuffer[] buffers =
            new ByteBuffer[] {clients_buffer, servers_buffer};

        // choose which peer will start handshake negotiation
        // (initial handshake is initiated by client, but rehandshake
        // can be initiated by any peer)
        int step = (client.getHandshakeStatus()
                != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) ? 0 : 1;

        SSLEngine current_engine = engines[step];
        ByteBuffer buffer;
        SSLEngineResult result = null;
        SSLEngineResult.HandshakeStatus status;

        while ((client.getHandshakeStatus()
                    != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
                || (server.getHandshakeStatus()
                    != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)) {
            if (doLog) {
                System.out.print("\n"
                        + ((current_engine == client) ? "CLIENT " : "SERVER "));
            }
            status = current_engine.getHandshakeStatus();
            if (status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                // so another peer produced
                // the handshaking data which has to be unwrapped
                if (doLog) {
                    System.out.print("(NOT_HANDSHAKING) ");
                }
                status = SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
            }
            if (status == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                if (doLog) {
                    System.out.println("NEED_WRAP");
                }
                // will wrap handshake data into its special buffer
                buffer = buffers[step];
                if (buffer.remaining() == 0) {
                    // handshake data was fully read by another peer,
                    // so we need clear the buffer before reusing it
                    buffer.clear();
                }
                // wrap the next handshake message
                print(result = current_engine.wrap(app_data, buffer));
                // if there are no any messages to send, switch the engine
                if (current_engine.getHandshakeStatus()
                        != SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    // switch the current engine
                    step ^= 1;
                    current_engine = engines[step];
                    // and prepare the buffer for reading
                    buffer.flip();
                }
            } else if (status == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                if (doLog) {
                    System.out.println("NEED_UNWRAP");
                }

                // If there are no unread handshake messages produced by the
                // current engine, try to wrap the application data and unwrap
                // it by another engine. It will test app data flow during
                // the rehandshake.
                if (!buffers[step].hasRemaining()) {
                    if (doLog) {
                        System.out.println(
                                "\nTry to WRAP the application data");
                    }
                    print(result = current_engine.wrap(
                                ByteBuffer.wrap(new byte[] {0}), app_data));
                    // The output in app_data will be produced only if it
                    // is rehandshaking stage
                    // (i.e. initial handshake has been done)
                    if (result.bytesProduced() > 0) {
                        // if the app data message has been produced,
                        // unwrap it by another peer
                        if (doLog) {
                            System.out.print("\n" + ((current_engine != client)
                                                     ? "CLIENT " : "SERVER "));
                            System.out.println(
                                "UNWRAPs app data sent during handshake");
                        }
                        app_data.flip();
                        print(result = engines[(step+1)%2].unwrap(
                                    app_data, app_data_plain));
                        app_data.clear();
                        app_data_plain.clear();
                    }
                }

                buffer = buffers[step^1];

                // check if there is handshake data to be unwrapped
                if (buffer.remaining() == 0) {
                    if (doLog) {
                        System.out.println(
                                "There is no handshake data to be unwrapped.");
                    }
                    // switch the current engine
                    step ^= 1;
                    current_engine = engines[step];
                    if ((current_engine.getHandshakeStatus()
                                == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)
                            && (buffers[step^1].remaining() == 0)) {
                        System.out.println(
                                "Both engines are in NEED_UNWRAP state");
                        fail("Both engines are in NEED_UNWRAP state");
                    }
                    continue;
                }

                print(result = current_engine.unwrap(buffer, app_data));
                if (current_engine.getHandshakeStatus()
                        == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    if (doLog) {
                        System.out.println("NEED_TASK");
                    }
                    current_engine.getDelegatedTask().run();
                    if (doLog) {
                        System.out.println("status after the task: "
                                +current_engine.getHandshakeStatus());
                    }
                }
            } else {
                fail("Unexpected HandshakeStatus: "+status);
            }
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.OK,
                    result.getStatus());
        }
    }

    /*
     * Performs the session renegotiation process when one
     * of the peers is not allowed to create the session.
     */
    private void doNoRenegotiationTest(SSLEngine allowed,
            SSLEngine not_allowed, boolean is_initial) throws Exception {
        if (doLog) {
            System.out.println(
                    "\n--- doNoRenegotiationTest: is_initial: "+is_initial);
        }

        not_allowed.setEnableSessionCreation(false);
        not_allowed.getSession().invalidate();

        int packetBufferSize =
            allowed.getSession().getPacketBufferSize();
        int applicationBufferSize =
            not_allowed.getSession().getApplicationBufferSize();

        // buffer will contain handshake messages
        ByteBuffer buffer = ByteBuffer.allocate(packetBufferSize+1000);
        // buffers will contain application data messages
        ByteBuffer app_data = ByteBuffer.allocate(packetBufferSize);
        ByteBuffer app_data_plain = ByteBuffer.allocate(applicationBufferSize);

        SSLEngineResult result = null;

        allowed.beginHandshake();
        //not_allowed.beginHandshake();

        if (doLog) {
            System.out.println(
                "\nAllowed peer wraps the initial session negotiation message");
        }
        // wrap the initial session negotiation message
        while (allowed.getHandshakeStatus().equals(
                    SSLEngineResult.HandshakeStatus.NEED_WRAP)) {
            print(result = allowed.wrap(app_data_plain, buffer));
            assertTrue("Engine did not produce any data",
                    result.bytesProduced() > 0);
        }
        // prepare the buffer for reading
        buffer.flip();
        if (doLog) {
            System.out.println("\nNot allowed unwraps the message");
        }
        try {
            // unwrap the message. expecting whether SSLException or NEED_WRAP
            print(result = not_allowed.unwrap(buffer, app_data_plain));
        } catch (SSLException e) {
            if (is_initial) {
                return; // ok, exception was thrown
            } else {
                fail("Unexpected SSLException was thrown "+e);
            }
        }
        // if it is not an initial handshake phase it is posible
        // SSLException to be thrown.
        try {
            while (!not_allowed.getHandshakeStatus().equals(
                        SSLEngineResult.HandshakeStatus.NEED_WRAP)) {
                assertTrue("Engine did not consume any data",
                        result.bytesConsumed() > 0);
                if (not_allowed.getHandshakeStatus().equals(
                            SSLEngineResult.HandshakeStatus.NEED_TASK)) {
                    not_allowed.getDelegatedTask().run();
                    if (doLog) {
                        System.out.println("Status after the task: "
                                + not_allowed.getHandshakeStatus());
                    }
                    continue;
                } else if (not_allowed.getHandshakeStatus().equals(
                            SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                    print(result = not_allowed.unwrap(buffer, app_data_plain));
                } else {
                    fail("Unexpected status of operation: "
                            + not_allowed.getHandshakeStatus());
                }
            }
            // prepare for writting
            buffer.clear();
            if (doLog) {
                System.out.println(
                    "\nWrapping the message. Expecting no_renegotiation alert");
            }
            // wrapping the message. expecting no_renegotiation alert
            print(result = not_allowed.wrap(app_data_plain, buffer));
        } catch (SSLException e) {
            if (!is_initial) {
                fail("Unexpected SSLException was thrown."+e.getMessage());
            }
            if (doLog) {
                System.out.println("Throwed exception during the unwrapping "
                        + "of handshake initiation message:");
                e.printStackTrace();
                System.out.println("Handshake Status: "
                        + not_allowed.getHandshakeStatus());
            }
            if (not_allowed.getHandshakeStatus().equals(
                        SSLEngineResult.HandshakeStatus.NEED_WRAP)) {
                // needs to wrap fatal alert message
                if (doLog) {
                    System.out.println(
                            "\nnot_allowed wraps fatal alert message");
                }
                // prepare for writting
                buffer.clear();
                print(result = not_allowed.wrap(app_data_plain, buffer));
            }
        }
        // check whether alert message has been sent
        assertTrue("Engine did not produce any data",
                result.bytesProduced() > 0);
        // check whether not_allowed engine stoped handshake operation
        assertEquals("Unexpected status of operation: not_allowed "
                + "to session creation peer did not stop its handshake process",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                not_allowed.getHandshakeStatus());
        // prepare for reading
        buffer.flip();
        try {
            print(result = allowed.unwrap(buffer, app_data_plain));
            assertTrue("Engine did not consume any data",
                    result.bytesConsumed() > 0);
            assertEquals("Responce from the peer not allowed to create "
                    + "the session did not cause the stopping of "
                    + "the session negotiation process",
                    SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                    result.getHandshakeStatus());
        } catch (SSLException e) {
            if (!is_initial) {
                fail("Unexpected SSLException was thrown."+e.getMessage());
            }
            if (doLog) {
                System.out.println("Throwed exception during the unwrapping "
                        + "of responce from allowed peer:");
                e.printStackTrace();
                System.out.println("Handshake Status: "
                        + not_allowed.getHandshakeStatus());
            }
        }
    }

    /*
     * Tests the data exchange process between two engines
     */
    private void doDataExchange(SSLEngine client,
                                SSLEngine server) throws Exception {
        if (doLog) {
            System.out.println("\n--- doDataExchange:");
        }
        ByteBuffer co = ByteBuffer.allocate(
                client.getSession().getPacketBufferSize());
        ByteBuffer si = ByteBuffer.allocate(
                server.getSession().getPacketBufferSize());
        SSLEngineResult result;

        String data_2b_sent = "data to be sent";
        ByteBuffer data = ByteBuffer.wrap(data_2b_sent.getBytes());

        if (doLog) {
            System.out.println("\nTry to wrap the data into small buffer");
        }
        print(result = client.wrap(data, ByteBuffer.allocate(35)));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.BUFFER_OVERFLOW,
                result.getStatus());

        if (doLog) {
            System.out.println("\nWrapping the data of length "
                    + data.remaining());
        }
        print(result = client.wrap(data, co));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.OK,
                result.getStatus());

        // tune the buffer to read from it
        co.limit(co.position());
        co.rewind();

        if (doLog) {
            System.out.println("\nTry to unwrap the data into small buffer");
        }
        print(result = server.unwrap(co, ByteBuffer.allocate(0)));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.BUFFER_OVERFLOW,
                result.getStatus());
        if (doLog) {
            System.out.println("\nUnwrapping the data into buffer");
        }
        print(result = server.unwrap(co, si));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.OK,
                result.getStatus());
        assertEquals(
            "The length of the received data differs from expected",
            "data to be sent".length(), result.bytesProduced());

        // take the data from the buffer
        byte[] resulting_data = new byte[result.bytesProduced()];
        si.rewind();
        si.get(resulting_data);
        si.clear();
        assertTrue(Arrays.equals(data_2b_sent.getBytes(), resulting_data));

        co.clear();
        for (int i=1; i<10; i++) {
            byte[] buff = new byte[i];
            data = ByteBuffer.wrap(buff);
            if (doLog) {
                System.out.println("\nWrap the data");
            }
            print(result = client.wrap(data, co));
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.OK,
                    result.getStatus());
            if (doLog) {
                System.out.println("\nUnwrap the data");
            }
            co.rewind();
            print(result = server.unwrap(co, si));
            assertEquals("Unexpected status of operation:",
                    SSLEngineResult.Status.OK,
                    result.getStatus());
            assertEquals(
                "The length of the received data differs from expected",
                i, result.bytesProduced());
            resulting_data = new byte[i];
            si.rewind();
            si.get(resulting_data);
            si.clear();
            assertTrue(Arrays.equals(buff, resulting_data));
            co.clear();
            si.clear();
        }
    }

    /*
     * Performs the closure process over the two communicationg engines.
     * The handshake process should be performed before the call of this
     * method.
     */
    private void doClose(SSLEngine client, SSLEngine server) throws Exception {
        if (doLog) {
            System.out.println("\n--- doClose: ");
        }
        ByteBuffer buffer = ByteBuffer.allocate(
                // +100 because we put the data into the buffer multiple times
                server.getSession().getPacketBufferSize()+100);
        ByteBuffer app_data_buffer = ByteBuffer.allocate(
                client.getSession().getApplicationBufferSize());
        SSLEngineResult result;
        // first: send 0 just for fun
        if (doLog) {
            System.out.println("\nServer sends pending outboud data:");
        }
        print(result = server.wrap(ByteBuffer.wrap(new byte[] {0}), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.OK,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        // second: initiate a close
        if (doLog) {
            System.out.println("\nServer initiates a closure:");
        }
        server.closeOutbound();
        // should do nothing:
        server.closeOutbound();

        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NEED_WRAP,
                server.getHandshakeStatus());

        // will cause SSLException because closure alert was not received yet:
        // server.closeInbound();

        // wrap closure alert (previosly sent 0 should not be lost)
        print(result = server.wrap(ByteBuffer.allocate(0), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP,
                result.getHandshakeStatus());

        if (doLog) {
            System.out.println("\nServer sends pending outboud data again:");
        }
        // will do nothing because closure alert has been sent
        // and outbound has been closed
        print(result = server.wrap(ByteBuffer.wrap(new byte[] {0}), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertEquals(
            "The length of the produced data differs from expected",
            0, result.bytesProduced());

        // prepare the buffer for reading
        buffer.flip();

        if (doLog) {
            System.out.println(
                    "\nClient receives pending servers' outbound data");
        }
        print(result = client.unwrap(buffer, app_data_buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.OK,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the produced data differs from expected",
            1, result.bytesProduced());

        app_data_buffer.clear();

        if (doLog) {
            System.out.println("\nClient receives close notify");
        }
        print(result = client.unwrap(buffer, app_data_buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NEED_WRAP,
                result.getHandshakeStatus());
        assertTrue(
            "The length of the consumed data differs from expected",
            result.bytesConsumed() > 0);
        assertEquals(
            "The length of the received data differs from expected",
            0, result.bytesProduced());

        // prepare the buffer for writing
        app_data_buffer.clear();

        // it's needless, but should work (don't cause exceptions):
        client.closeInbound();
        client.closeOutbound();

        if (doLog) {
            System.out.println("\nClient tries to read data again");
        }
        // CLOSED, 0 consumed, 0 produced
        print(result = client.unwrap(buffer, app_data_buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NEED_WRAP,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertEquals(
            "The length of the received data differs from expected",
            0, result.bytesProduced());

        // prepare the buffer for writing
        buffer.clear();

        if (doLog) {
            System.out.println("\nClient sends responding close notify");
        }
        print(result = client.wrap(ByteBuffer.wrap(new byte[] {0}), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertTrue(
            "The length of the produced data differs from expected",
            result.bytesProduced() > 0);
        if (doLog) {
            System.out.println(
                    "\nClient tries to send data after closure alert");
        }
        // this data will not be sent (should do nothing - 0 cons, 0 prod)
        print(result = client.wrap(ByteBuffer.wrap(new byte[] {0}), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertEquals(
            "The length of the produced data differs from expected",
            0, result.bytesProduced());

        // prepare the buffers for reading
        app_data_buffer.clear();
        buffer.flip();

        if (doLog) {
            System.out.println("\nServer receives close notify");
        }
        print(result = server.unwrap(buffer, app_data_buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertTrue(
            "The length of the consumed data differs from expected",
            result.bytesConsumed() > 0);
        assertEquals(
            "The length of the produced data differs from expected",
            0, result.bytesProduced());

        if (doLog) {
            System.out.println("\nServer tries to read after closure");
        }
        // will be ignored
        print(result = server.unwrap(buffer, app_data_buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertEquals(
            "The length of the produced data differs from expected",
            0, result.bytesProduced());

        // it's needless, but should work:
        client.closeInbound();
        // will be ignored

        if (doLog) {
            System.out.println("\nServer tries to write after closure");
        }
        buffer.clear();
        print(result = server.wrap(ByteBuffer.allocate(0), buffer));
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.Status.CLOSED,
                result.getStatus());
        assertEquals("Unexpected status of operation:",
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING,
                result.getHandshakeStatus());
        assertEquals(
            "The length of the consumed data differs from expected",
            0, result.bytesConsumed());
        assertEquals(
            "The length of the produced data differs from expected",
            0, result.bytesProduced());
    }

    private static void print(SSLEngineResult result) {
        if (doLog) {
            System.out.println("result:\n"+result);
        }
    }

    /**
     * Returns the engine to be tested.
     */
    private SSLEngine getEngine() throws Exception {
        return JSSETestData.getContext().createSSLEngine("localhost", 2345);
    }

    /**
     * Initializes the engines.
     */
    private void initEngines(SSLEngine client, SSLEngine server) {
        String prefix = "TLS_";

        client.setEnabledProtocols(new String[] {"TLSv1"});
        server.setEnabledProtocols(new String[] {"TLSv1"});
        client.setEnabledCipherSuites(
                new String[] {prefix+cipher_suites[0]});
        server.setEnabledCipherSuites(
                new String[] {prefix+cipher_suites[0]});

        client.setUseClientMode(true);
        server.setUseClientMode(false);
    }

    public static Test suite() {
        return new TestSuite(SSLEngineImplTest.class);
    }

}
