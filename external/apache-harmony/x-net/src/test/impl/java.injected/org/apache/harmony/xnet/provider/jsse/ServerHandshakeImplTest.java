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

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import junit.framework.TestCase;

/**
 * Tests for <code>ServerHandshakeImpl</code> constructor and methods
 *  
 */
public class ServerHandshakeImplTest extends TestCase {
  // to store initialization Exception
  private static Exception initException;

  
  private SSLParameters sslParameters;
  private ServerHandshakeImpl server;

  @Override
public void setUp() throws Exception {
        char[] pwd = JSSETestData.KS_PASSWORD;
        KeyStore ks = JSSETestData.getKeyStore();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
        kmf.init(ks, pwd);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
        tmf.init(ks);

        sslParameters = new SSLParameters(kmf.getKeyManagers(), tmf
                .getTrustManagers(), new SecureRandom(),
                new SSLSessionContextImpl(), new SSLSessionContextImpl());

        server = new ServerHandshakeImpl(new SSLEngineImpl(sslParameters));

        SSLEngineAppData appData = new SSLEngineAppData();
        AlertProtocol alertProtocol = new AlertProtocol();
        SSLBufferedInput recProtIS = new SSLBufferedInput();
        SSLRecordProtocol recordProtocol = new SSLRecordProtocol(server,
                alertProtocol, recProtIS, appData);
    }

    public void testUnwrap() {
        byte[] ses_id = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        byte[] version = new byte[] { 3, 1 };
        CipherSuite[] cipher_suite = new CipherSuite[] { 
                CipherSuite.TLS_RSA_WITH_RC4_128_MD5 };
        ClientHello message = new ClientHello(new SecureRandom(), version,
                ses_id, cipher_suite);
        HandshakeIODataStream out = new HandshakeIODataStream();
        out.writeUint8(message.getType());
        out.writeUint24(message.length());
        message.send(out);
        byte[] encodedClientHello = out.getData(1000);
        
        // ----------------------------------------
        // unwrap client hello (full handshake)
        // precondition: session hash does not contains requested session        
        server.unwrap(encodedClientHello);
        server.getTask().run(); // process client hello in delegated task
        server.wrap(); // modelling of server respond sending
        
        assertFalse(server.isResuming);

        // unwrap unexpected second client hello
        try {
            server.unwrap(encodedClientHello);
            fail("No expected AlertException");
        } catch (AlertException e) {
        }
        
        // unexpected ChangeCipherSpec
        try {
            server.receiveChangeCipherSpec();
            fail("No expected AlertException");
        } catch (AlertException e) {
        }
        
        // ----------------------------------------
        // unwrap client hello (abbreviated handshake)
        // precondition: session hash contains requested session
        clearServerData();
        SSLSessionImpl session = new SSLSessionImpl(
                CipherSuite.TLS_RSA_WITH_RC4_128_MD5, new SecureRandom());
        session.id = ses_id;
        // put session to hash
        server.parameters.getServerSessionContext().putSession(session);
        
        server.unwrap(encodedClientHello);
        server.getTask().run(); // process client hello in delegated task
        server.wrap(); // modelling of server respond sending
        
        assertTrue(server.isResuming);
        
        server.makeFinished(); // complete handshake
        
        // expected ChangeCipherSpec
        server.receiveChangeCipherSpec();      
    }

    public void testServerHandshakeImpl() {
        assertEquals(server.status, HandshakeProtocol.NEED_UNWRAP);
        assertTrue(server.nonBlocking);
        assertSame(server.parameters.getKeyManager(), sslParameters
                .getKeyManager());
        assertSame(server.parameters.getTrustManager(), sslParameters
                .getTrustManager());
        assertNotNull(server.engineOwner);
        assertNull(server.socketOwner);
    }

    private void clearServerData() {
        server.clearMessages();
        server.io_stream = new HandshakeIODataStream();
        server.status = HandshakeProtocol.NEED_UNWRAP;
    }
}
