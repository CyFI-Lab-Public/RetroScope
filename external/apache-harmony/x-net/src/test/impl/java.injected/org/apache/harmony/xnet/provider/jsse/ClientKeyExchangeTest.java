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
 * Tests for <code>ClientKeyExchange</code> constructor and methods
 *  
 */
public class ClientKeyExchangeTest extends TestCase {

    /*
     * Test for void ClientKeyExchange(byte[], boolean)
     */
    public void testClientKeyExchangebyteArrayboolean() throws Exception {
        byte[] encrypted_pre_master_secret = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
                1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        boolean[] isTLS = new boolean[] { true, false };

        for (int i = 0; i < isTLS.length; i++) {
            ClientKeyExchange message = new ClientKeyExchange(
                    encrypted_pre_master_secret, isTLS[i]);
            assertEquals("incorrect type", Handshake.CLIENT_KEY_EXCHANGE,
                    message.getType());

            assertTrue("incorrect ClientKeyExchange", Arrays.equals(
                    message.exchange_keys, encrypted_pre_master_secret));

            HandshakeIODataStream out = new HandshakeIODataStream();
            message.send(out);
            byte[] encoded = out.getData(1000);
            assertEquals("incorrect out data length ", message.length(),
                    encoded.length);

            HandshakeIODataStream in = new HandshakeIODataStream();
            in.append(encoded);
            ClientKeyExchange message_2 = new ClientKeyExchange(in, message
                    .length(), isTLS[i], true);

            assertTrue("Incorrect message decoding", Arrays.equals(
                    message.exchange_keys, message_2.exchange_keys));
            assertEquals("Incorrect message decoding", message.length(),
                    message_2.length());

            in.append(encoded);
            try {
                message_2 = new ClientKeyExchange(in, message.length() - 1,
                        isTLS[i], true);
                if (isTLS[i]) {
                    fail("Small length: No expected AlertException");
                }
            } catch (AlertException e) {
                if (!isTLS[i]) {
                    fail(e.toString());
                }
            }

            in.append(encoded);
            in.append(new byte[] { 1, 2, 3 });
            try {
                message_2 = new ClientKeyExchange(in, message.length() + 3,
                        isTLS[i], true);
                if (isTLS[i]) {
                    fail("Extra bytes: No expected AlertException");
                }
            } catch (AlertException e) {
                if (!isTLS[i]) {
                    fail(e.toString());
                }
            }
        }
    }

    /*
     * Test for void ClientKeyExchange(BigInteger)
     */
    public void testClientKeyExchangeBigInteger() throws Exception {
        BigInteger dh_Yc = new BigInteger("1234567890");
        boolean[] isTLS = new boolean[] { true, false };

        for (int i = 0; i < isTLS.length; i++) {
            ClientKeyExchange message = new ClientKeyExchange(dh_Yc);
            assertEquals("incorrect type", Handshake.CLIENT_KEY_EXCHANGE,
                    message.getType());
            assertEquals("incorrect ClientKeyExchange", dh_Yc, new BigInteger(
                    message.exchange_keys));

            HandshakeIODataStream out = new HandshakeIODataStream();
            message.send(out);
            byte[] encoded = out.getData(1000);
            assertEquals("incorrect out data length", message.length(),
                    encoded.length);

            HandshakeIODataStream in = new HandshakeIODataStream();
            in.append(encoded);
            ClientKeyExchange message_2 = new ClientKeyExchange(in, message
                    .length(), isTLS[i], false);

            assertEquals("Incorrect message decoding", message.length(),
                    message_2.length());
            assertTrue("Incorrect message decoding", Arrays.equals(
                    message.exchange_keys, message_2.exchange_keys));

            in.append(encoded);
            try {
                message_2 = new ClientKeyExchange(in, message.length() - 1,
                        isTLS[i], false);
                fail("Small length: No expected AlertException");
            } catch (AlertException e) {
            }

            in.append(encoded);
            in.append(new byte[] { 1, 2, 3 });
            try {
                message_2 = new ClientKeyExchange(in, message.length() + 3,
                        isTLS[i], false);
                fail("Extra bytes: No expected AlertException");
            } catch (AlertException e) {
            }
        }
    }

    /*
     * Test for void ClientKeyExchange()
     */
    public void testClientKeyExchange() throws Exception {

        ClientKeyExchange message = new ClientKeyExchange();
        assertEquals("incorrect type", Handshake.CLIENT_KEY_EXCHANGE, message
                .getType());
        assertEquals("incorrect ClientKeyExchange", 0,
                message.exchange_keys.length);
        assertEquals("incorrect ClientKeyExchange", 0, message.length());
        assertTrue("incorrect ClientKeyExchange", message.isEmpty());

        HandshakeIODataStream out = new HandshakeIODataStream();
        message.send(out);
        byte[] encoded = out.getData(1000);
        assertEquals("incorrect ClientKeyExchange", 0, message.length());
        assertEquals("incorrect out data length", message.length(),
                encoded.length);

        HandshakeIODataStream in = new HandshakeIODataStream();
        in.append(encoded);
        ClientKeyExchange message_2 = new ClientKeyExchange(in, message
                .length(), true, false);

        assertEquals("Incorrect message decoding", 0,
                message_2.exchange_keys.length);
        assertEquals("Incorrect message decoding", 0, message_2.length());
        assertTrue("Incorrect message decoding", message_2.isEmpty());

        in.append(encoded);
        try {
            message_2 = new ClientKeyExchange(in, message.length() - 1, true,
                    false);
            fail("Small length: No expected IOException");
        } catch (IOException e) {
        }

        in.append(encoded);
        in.append(new byte[] { 1, 2, 3 });
        try {
            message_2 = new ClientKeyExchange(in, message.length() + 3, true,
                    false);
            fail("Extra bytes: No expected IOException");
        } catch (IOException e) {
        }
    }

}
