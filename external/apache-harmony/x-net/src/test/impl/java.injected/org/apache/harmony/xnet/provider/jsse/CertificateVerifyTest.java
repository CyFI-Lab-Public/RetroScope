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

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests for <code>CertificateVerify</code> constructor and methods
 * 
 */
public class CertificateVerifyTest extends TestCase {

	public void testCertificateVerify() throws Exception {
		byte[] anonHash = new byte[0];
		byte[] RSAHash = new byte[] {
				1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
				1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
				1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
				1, 2, 3, 4, 5, 6};
		byte[] DSAHash  = new byte[] {
				1, 2, 3, 4, 5, 6, 7, 8, 9, 0,
				1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
		byte[][] signatures = new byte[][] { RSAHash, DSAHash };
        try {
            new CertificateVerify(anonHash);
            fail("Anonymous: No expected AlertException");
        } catch (AlertException e) {
        }
        try {
            HandshakeIODataStream in = new HandshakeIODataStream();
            new CertificateVerify(in, 0);
            fail("Anonymous: No expected AlertException");
        } catch (AlertException e) {
        }
		for (int i = 0; i < signatures.length; i++) {
			CertificateVerify message = new CertificateVerify(signatures[i]);
            assertEquals("incorrect type", Handshake.CERTIFICATE_VERIFY,
                    message.getType());
            assertTrue("incorrect CertificateVerify", 
                    Arrays.equals(message.signedHash, signatures[i]));

			HandshakeIODataStream out = new HandshakeIODataStream();
			message.send(out);
			byte[] encoded = out.getData(1000);
            assertEquals("incorrect out data length", message.length(),
                    encoded.length);

			HandshakeIODataStream in = new HandshakeIODataStream();
			in.append(encoded);
			CertificateVerify message_2 = new CertificateVerify(in, message.length());
            assertTrue("incorrect message decoding", 
                    Arrays.equals(message.signedHash, message_2.signedHash));

			in.append(encoded);
			try {
				message_2 = new CertificateVerify(in, message.length() - 1);
				fail("Small length: No expected AlertException");
			} catch (AlertException e) {
			}

			in.append(encoded);
			in.append(new byte[] { 1, 2, 3 });
			try {
				message_2 = new CertificateVerify(in, message.length() + 3);
				fail("Extra bytes: No expected AlertException ");
			} catch (AlertException e) {
			}
		}
	}

}
