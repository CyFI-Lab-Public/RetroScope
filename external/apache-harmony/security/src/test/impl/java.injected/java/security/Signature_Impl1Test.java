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

/**
* @author Boris V. Kuznetsov
*/

package java.security;

import org.apache.harmony.security.tests.support.MySignature1;

import junit.framework.TestCase;

/**
 * Tests for <code>Signature</code> constructor and methods
 * 
 */
public class Signature_Impl1Test extends TestCase {

	/*
	 * Class under test for int sign(byte[], int, int)
	 */
	public void testSignbyteArrayintint() throws Exception {
		MySignature1 s = new MySignature1("ABC");
		byte[] b = new byte[8];
		try {
			s.sign(b, 0, 5);
			fail("No expected SignatureException 1");
		} catch (SignatureException e) {		
		}
		
        s.initVerify(new MyPublicKey());
		
		try {
			s.sign(b, 0, 5);
			fail("No expected SignatureException 1");
		} catch (SignatureException e) {		
		}
		
        s.initSign(new MyPrivateKey());
        s.sign(b, 0, 5);
        assertEquals("state", Signature.SIGN, s.getState());
        assertTrue("sign() failed", s.runEngineSign);
	}

	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
        MySignature1 s = new MySignature1("ABC");
        assertEquals("toString() failed", "SIGNATURE ABC state: UNINITIALIZED",
                s.toString());
    }

    private class MyKey implements Key {
        public String getFormat() {
            return "123";
        }
        public byte[] getEncoded() {
            return null;
        }
        public String getAlgorithm() {
            return "aaa";
        }       
    }
    
    private class MyPublicKey extends MyKey implements PublicKey {}

    private class MyPrivateKey extends MyKey implements PrivateKey {}

}
