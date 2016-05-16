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

import java.security.KeyManagementException;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.net.ssl.SSLEngineResult;

import junit.framework.TestCase;

/**
 * Tests for <code>HandshakeProtocol</code> constructor and methods
 *  
 */
public class HandshakeProtocolTest extends TestCase {

    public void testGetStatus() throws Exception {
        HandshakeProtocol protocol = new ClientHandshakeImpl(new SSLEngineImpl(
                new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING);

        protocol.status = HandshakeProtocol.NEED_UNWRAP;
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NEED_UNWRAP);

        protocol.status = HandshakeProtocol.FINISHED;
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.FINISHED);
        assertEquals(protocol.status, HandshakeProtocol.NOT_HANDSHAKING);

        protocol.delegatedTaskErr = new Exception();
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NEED_WRAP);
        protocol.delegatedTaskErr = null;

        protocol.delegatedTasks.add(new DelegatedTask(null, null, null));
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NEED_TASK);
        protocol.delegatedTasks.clear();

        protocol.io_stream.write(new byte[] { 1, 2, 3 });
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NEED_WRAP);
    }

    public void testSendChangeCipherSpec() throws Exception {
        HandshakeProtocol protocol = new ServerHandshakeImpl(new SSLEngineImpl(
                new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        protocol.sendChangeCipherSpec();
        assertEquals(protocol.getStatus(),
                SSLEngineResult.HandshakeStatus.NEED_WRAP);
    }

    public void testWrap() throws Exception {
        HandshakeProtocol protocol = new ClientHandshakeImpl(new SSLEngineImpl(
                new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        assertNull(protocol.wrap());

        protocol.delegatedTaskErr = new Exception();
        try {
            protocol.wrap();
            fail("No expected AlertException");
        } catch (AlertException e) {
            assertEquals(e.getDescriptionCode(),
                    AlertProtocol.HANDSHAKE_FAILURE);
            assertNull(protocol.delegatedTaskErr);
        }
    }

    public void testcomputerVerifyDataTLS() throws Exception {
        HandshakeProtocol hs_protocol = new ClientHandshakeImpl(
                new SSLEngineImpl(new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        SecureRandom sr = new SecureRandom();
        SSLSessionImpl ses = new SSLSessionImpl(sr);
        hs_protocol.session = ses;
        hs_protocol.session.protocol = ProtocolVersion.TLSv1;
        assertSame(hs_protocol.getSession(), ses);

        hs_protocol.clientHello = new ClientHello(
                sr,
                hs_protocol.session.protocol.version,
                hs_protocol.session.id,
                new CipherSuite[] { CipherSuite.TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA });
        hs_protocol.serverHello = new ServerHello(sr,
                hs_protocol.session.protocol.version, hs_protocol.session.id,
                CipherSuite.TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA, (byte) 0);

        hs_protocol.preMasterSecret = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        hs_protocol.computerMasterSecret();
        assertNull(hs_protocol.preMasterSecret);
        assertEquals(48, hs_protocol.session.master_secret.length);

        hs_protocol.send(hs_protocol.clientHello);
        hs_protocol.send(hs_protocol.serverHello);

        hs_protocol.computerReferenceVerifyDataTLS("test");

        byte[] data = new byte[12];
        hs_protocol.computerVerifyDataTLS("test", data);

        hs_protocol.verifyFinished(data);

        try {
            hs_protocol.verifyFinished(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2 });
            fail("No expected AlertException");
        } catch (AlertException e) {
        }
    }

    public void testComputerReferenceVerifyDataSSLv3() throws Exception {
        HandshakeProtocol hs_protocol = new ClientHandshakeImpl(
                new SSLEngineImpl(new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        SecureRandom sr = new SecureRandom();
        SSLSessionImpl ses = new SSLSessionImpl(sr);
        hs_protocol.session = ses;
        hs_protocol.session.protocol = ProtocolVersion.SSLv3;
        assertSame(hs_protocol.getSession(), ses);

        hs_protocol.clientHello = new ClientHello(
                sr,
                hs_protocol.session.protocol.version,
                hs_protocol.session.id,
                new CipherSuite[] { CipherSuite.TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA });
        hs_protocol.serverHello = new ServerHello(sr,
                hs_protocol.session.protocol.version, hs_protocol.session.id,
                CipherSuite.TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA, (byte) 0);

        hs_protocol.preMasterSecret = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        hs_protocol.computerMasterSecret();
        assertNull(hs_protocol.preMasterSecret);
        assertEquals(48, hs_protocol.session.master_secret.length);

        hs_protocol.send(hs_protocol.clientHello);
        hs_protocol.send(hs_protocol.serverHello);

        hs_protocol.computerReferenceVerifyDataSSLv3(SSLv3Constants.client);

        byte[] data = new byte[36];
        hs_protocol.computerVerifyDataSSLv3(SSLv3Constants.client, data);

        hs_protocol.verifyFinished(data);

        try {
            hs_protocol.verifyFinished(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    0, 1, 2 });
            fail("No expected AlertException");
        } catch (AlertException e) {
        }
    }

    public void testUnexpectedMessage() throws Exception {
        HandshakeProtocol protocol = new ClientHandshakeImpl(new SSLEngineImpl(
                new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));
        try {
            protocol.unexpectedMessage();
            fail("No expected AlertException");
        } catch (AlertException e) {
            assertEquals(e.getDescriptionCode(),
                    AlertProtocol.UNEXPECTED_MESSAGE);
        }
    }

    public void testGetTask() throws Exception {
        HandshakeProtocol protocol = new ClientHandshakeImpl(new SSLEngineImpl(
                new SSLParameters(null, null, null,
                        new SSLSessionContextImpl(),
                        new SSLSessionContextImpl())));

        DelegatedTask task = new DelegatedTask(null, null, null);
        protocol.delegatedTasks.add(task);
        assertSame(protocol.getTask(), task);
        assertNull(protocol.getTask());
    }

    public void testGetRSAKeyLength() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        PublicKey key = kpg.genKeyPair().getPublic();

        assertEquals(512, HandshakeProtocol.getRSAKeyLength(key));

    }

}