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
import java.math.BigInteger;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests for <code>ServerKeyExchange</code> constructor and methods
 *  
 */
public class ServerKeyExchangeTest extends TestCase {

    public void testServerKeyExchange_RSA_EXPORT() throws Exception {
        BigInteger rsa_mod = new BigInteger(
                "0620872145533812525365347773040950432706816921321053881493952289532007782427182339053847578435298266865073748931755945944874247298083566202475988854994079");
        BigInteger rsa_exp = new BigInteger("65537");

        byte[] hash = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6 };
        ServerKeyExchange message = new ServerKeyExchange(rsa_mod, rsa_exp,
                null, hash);
        assertEquals("incorrect type", Handshake.SERVER_KEY_EXCHANGE, message
                .getType());

        assertTrue("incorrect ServerKeyExchange", Arrays.equals(message.hash,
                hash));
        assertEquals("incorrect ServerKeyExchange", rsa_mod, message.par1);
        assertEquals("incorrect ServerKeyExchange", rsa_exp, message.par2);
        assertNull("incorrect ServerKeyExchange", message.par3);

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        ServerKeyExchange message_2 = new ServerKeyExchange(in, message
                .length(), CipherSuite.KeyExchange_RSA_EXPORT);

        assertTrue("incorrect message decoding", Arrays.equals(message.hash,
                message_2.hash));
        assertEquals("incorrect message decoding", message.par1, message_2.par1);
        assertEquals("incorrect message decoding", message.par2, message_2.par2);
        assertNull("incorrect message decoding", message_2.par3);
        assertEquals("incorrect message decoding", message.getRSAPublicKey(),
                message_2.getRSAPublicKey());

        in.append(encoded);
        try {
            new ServerKeyExchange(in, message.length() - 1,
                    CipherSuite.KeyExchange_RSA_EXPORT);
            fail("Small length: No expected AlertException");
        } catch (AlertException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            new ServerKeyExchange(in, message.length() + 3,
                    CipherSuite.KeyExchange_RSA_EXPORT);
            fail("Extra bytes: No expected AlertException ");
        } catch (AlertException e) {
        }
    }

    public void testServerKeyExchange_DHE_DSS() throws Exception {
        BigInteger dh_p = new BigInteger("1234567890");
        BigInteger dh_g = new BigInteger("987654321");
        BigInteger dh_Ys = new BigInteger("123123123");
        byte[] hash = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5,
                6, 7, 8, 9, 0 };
        ServerKeyExchange message = new ServerKeyExchange(dh_p, dh_g, dh_Ys,
                hash);
        assertEquals("incorrect type", Handshake.SERVER_KEY_EXCHANGE, message
                .getType());

        assertTrue("incorrect ServerKeyExchange", Arrays.equals(message.hash,
                hash));
        assertEquals("incorrect ServerKeyExchange", dh_p, message.par1);
        assertEquals("incorrect ServerKeyExchange", dh_g, message.par2);
        assertEquals("incorrect ServerKeyExchange", dh_Ys, message.par3);

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        ServerKeyExchange message_2 = new ServerKeyExchange(in, message
                .length(), CipherSuite.KeyExchange_DHE_DSS);

        assertTrue("incorrect message decoding", Arrays.equals(message.hash,
                message_2.hash));
        assertEquals("incorrect message decoding", message.par1, message_2.par1);
        assertEquals("incorrect message decoding", message.par2, message_2.par2);
        assertEquals("incorrect message decoding", message.par3, message_2.par3);

        in.append(encoded);
        try {
            new ServerKeyExchange(in, message.length() - 1,
                    CipherSuite.KeyExchange_DHE_DSS);
            fail("Small length: No expected AlertException");
        } catch (AlertException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            new ServerKeyExchange(in, message.length() + 3,
                    CipherSuite.KeyExchange_DHE_DSS);
            fail("Extra bytes: No expected AlertException ");
        } catch (AlertException e) {
        }
    }

    public void testServerKeyExchange_DH_anon() throws Exception {
        BigInteger dh_p = new BigInteger("1234567890");
        BigInteger dh_g = new BigInteger("987654321");
        BigInteger dh_Ys = new BigInteger("123123123");
        ServerKeyExchange message = new ServerKeyExchange(dh_p, dh_g, dh_Ys,
                null);
        assertEquals("incorrect type", Handshake.SERVER_KEY_EXCHANGE, message
                .getType());

        assertNull("incorrect ServerKeyExchange", message.hash);
        assertEquals("incorrect ServerKeyExchange", dh_p, message.par1);
        assertEquals("incorrect ServerKeyExchange", dh_g, message.par2);
        assertEquals("incorrect ServerKeyExchange", dh_Ys, message.par3);

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        ServerKeyExchange message_2 = new ServerKeyExchange(in, message
                .length(), CipherSuite.KeyExchange_DH_anon);

        assertNull("incorrect message decoding", message_2.hash);
        assertEquals("incorrect message decoding", message.par1, message_2.par1);
        assertEquals("incorrect message decoding", message.par2, message_2.par2);
        assertEquals("incorrect message decoding", message.par3, message_2.par3);

        in.append(encoded);
        try {
            new ServerKeyExchange(in, message.length() - 1,
                    CipherSuite.KeyExchange_DH_anon);
            fail("Small length: No expected AlertException");
        } catch (AlertException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            new ServerKeyExchange(in, message.length() + 3,
                    CipherSuite.KeyExchange_DH_anon);
            fail("Extra bytes: No expected AlertException ");
        } catch (AlertException e) {
        }
    }

}
