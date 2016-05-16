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
import java.security.SecureRandom;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests for <code>ServerHello</code> constructor and methods
 *  
 */
public class ServerHelloTest extends TestCase {

    public void testServerHello() throws Exception {
        byte[] session_id = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        CipherSuite cipher_suite = CipherSuite.TLS_DH_DSS_WITH_DES_CBC_SHA;
        byte[] server_version = new byte[] { 3, 1 };
        ServerHello message = new ServerHello(new SecureRandom(),
                server_version, session_id, cipher_suite, (byte) 0);
        assertEquals("incorrect type", Handshake.SERVER_HELLO, message
                .getType());

        assertTrue("incorrect CertificateRequest", Arrays.equals(
                message.server_version, server_version));
        assertTrue("incorrect CertificateRequest", Arrays.equals(
                message.session_id, session_id));
        assertEquals("incorrect CertificateRequest", cipher_suite,
                message.cipher_suite);

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        ServerHello message_2 = new ServerHello(in, message.length());

        assertTrue("incorrect message decoding", Arrays.equals(
                message.server_version, message_2.server_version));
        assertTrue("incorrect message decoding", Arrays.equals(
                message.session_id, message_2.session_id));
        assertTrue("incorrect message decoding", Arrays.equals(message.random,
                message_2.random));
        assertEquals("incorrect message decoding", message.cipher_suite,
                message_2.cipher_suite);

        in.append(encoded);
        try {
            new ServerHello(in, message.length() - 1);
            fail("Small length: No expected AlertException");
        } catch (AlertException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            new ServerHello(in, message.length() + 3);
            fail("Extra bytes: No expected AlertException ");
        } catch (AlertException e) {
        }
    }

}
