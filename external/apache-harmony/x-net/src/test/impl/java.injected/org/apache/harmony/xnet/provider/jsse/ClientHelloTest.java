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
 * Tests for <code>ClientHello</code> constructor and methods
 * 
 */
public class ClientHelloTest extends TestCase {


	/*
	 * Test for ClientHello(SecureRandom, byte[], byte[], CipherSuite[]),
	 * ClientHello(HandshakeIODataStream, int), getType(), getRandom(), and
	 * send();
	 */
	public void testClientHello() throws Exception {
		byte[] ses_id = new byte[] {1,2,3,4,5,6,7,8,9,0};
		byte[] version = new byte[] {3, 1 };
		CipherSuite[] cipher_suite = new CipherSuite[] {
                CipherSuite.TLS_RSA_WITH_RC4_128_MD5};
		ClientHello message = new ClientHello(new SecureRandom(), version,
				ses_id, cipher_suite);
        assertEquals("incorrect type", Handshake.CLIENT_HELLO, message.getType());
        assertEquals("incorrect length", 51, message.length());
        assertEquals("incorrect random", 32, message.getRandom().length);

		HandshakeIODataStream out = new HandshakeIODataStream();
		message.send(out);
		byte[] encoded = out.getData(1000);
        assertEquals("incorrect out data length", message.length(), encoded.length);

		HandshakeIODataStream in = new HandshakeIODataStream();
		in.append(encoded);
		ClientHello message_2 = new ClientHello(in, message.length());
		
		assertTrue("Incorrect message decoding",
                Arrays.equals(message.client_version, message_2.client_version));
        assertTrue("Incorrect message decoding",
                Arrays.equals(message.getRandom(), message_2.getRandom()));

		in.append(encoded);
		try {
			message_2 = new ClientHello(in, message.length()-1);
			fail("Small length: No expected AlertException");
		} catch (AlertException e){
		}
		
		in.append(encoded);
		try {
			message_2 = new ClientHello(in, message.length()+ 1);
			fail("Big length: No expected IO exception");
		} catch (IOException e){
		}
		
		in.append(encoded);
		in.append(new byte[] {1,2,3});
		new ClientHello(in, message.length()+ 3);	// extra bytes must be
                                                     // ignored
	}

}
