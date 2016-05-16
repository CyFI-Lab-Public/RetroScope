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
import org.apache.harmony.security.tests.support.MySignature2;

import junit.framework.TestCase;

/**
 * Tests for <code>Signature</code> constructor and methods
 * 
 */
public class Signature_Impl2Test extends TestCase {

	/**
	 * Provider
	 */
	Provider p;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		p = new MyProvider();
		Security.insertProviderAt(p, 1);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		Security.removeProvider(p.getName());
	}

	/*
	 * Class under test for Signature getInstance(String)
	 */
	public void testGetInstanceString1() throws Exception {
		Signature sig = Signature.getInstance("ABC");		
		checkSig1(sig, p);
	}
	
	/*
	 * Class under test for Signature getInstance(String)
	 */
	public void testGetInstanceString2() throws Exception {
		Signature sig = Signature.getInstance("CBA");
		checkSig2(sig, p);
	}

	/*
	 * Class under test for Signature getInstance(String, String)
	 */
	public void testGetInstanceStringString1() throws Exception {
		Signature sig = Signature.getInstance("ABC", "MyProvider");		
		checkSig1(sig, p);
	}
	
	/*
	 * Class under test for Signature getInstance(String, String)
	 */
	public void testGetInstanceStringString2() throws Exception {
		Signature sig = Signature.getInstance("CBA", "MyProvider");
		checkSig2(sig, p);
	}


	/*
	 * Class under test for Signature getInstance(String, Provider)
	 */
	public void testGetInstanceStringProvider1() throws Exception {
		Provider p1 = new MyProvider();
		Signature sig = Signature.getInstance("ABC", p1);		
		checkSig1(sig, p1);
	}
	
	/*
	 * Class under test for Signature getInstance(String, Provider)
	 */
	public void testGetInstanceStringProvider2() throws Exception {
		Provider p2 = new MyProvider();
		Signature sig = Signature.getInstance("CBA", p2);
		checkSig2(sig, p2);
	}

	private void checkSig1(Signature s, Provider p) throws Exception {
        byte[] b = { 1, 2, 3, 4 };
        assertTrue("getInstance() failed", s instanceof MySignature1);
        assertEquals("getProvider() failed", p, s.getProvider());
        assertEquals("getAlgorithm() failed", "ABC", s.getAlgorithm());

        try {
            s.sign();
            fail("No expected SignatureException");
        } catch (SignatureException e) {
        }

        s.initVerify(new MyPublicKey());

        try {
            s.sign();
            fail("No expected SignatureException");
        } catch (SignatureException e) {
        }

        s.initSign(new MyPrivateKey());
        s.sign();
        assertEquals("Incorrect state", Signature.SIGN, ((MySignature1) s)
                .getState());
        assertTrue("sign() failed", ((MySignature1) s).runEngineSign);

        s.initVerify(new MyPublicKey());
        s.update((byte) 1);

        s.initSign(new MyPrivateKey());
        s.update((byte) 1);

        assertEquals("Incorrect state", Signature.SIGN, ((MySignature1) s)
                .getState());
        assertTrue("sign() failed", ((MySignature1) s).runEngineUpdate1);

        s.initSign(new MyPrivateKey());

        try {
            s.verify(b);
            fail("No expected SignatureException");
        } catch (SignatureException e) {
        }
        s.initVerify(new MyPublicKey());
        s.verify(b);
        assertEquals("Incorrect state", Signature.VERIFY, ((MySignature1) s)
                .getState());
        assertTrue("verify() failed", ((MySignature1) s).runEngineVerify);
    }
	
	private void checkSig2(Signature s, Provider p) throws Exception {
        byte[] b = { 1, 2, 3, 4 };

        assertEquals("getProvider() failed", p, s.getProvider());
        assertEquals("getAlgorithm() failed", "CBA", s.getAlgorithm());

        s.initVerify(new MyCertificate());

        try {
            s.sign(b, 0, 5);
            fail("No expected IllegalArgumentException 1");
        } catch (IllegalArgumentException e) {
        }

        s.initSign(new MyPrivateKey());
        s.sign(b, 0, 3);

        assertTrue("sign() failed", MySignature2.runEngineSign);
        s.update(b);
        s.initSign(new MyPrivateKey());
        s.update(b);
        assertTrue("update() failed", MySignature2.runEngineUpdate2);

        s.initSign(new MyPrivateKey());
        try {
            s.verify(b, 0, 3);
            fail("No expected SignatureException");
        } catch (SignatureException e) {
        }
        s.initVerify(new MyPublicKey());

        try {
            s.verify(b, 0, 5);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        } catch (SignatureException e) {
        }

        s.verify(b, 0, 3);
        assertTrue("verify() failed", MySignature2.runEngineVerify);
    }
	
	private class MyProvider extends Provider {
		MyProvider() {
			super("MyProvider", 1.0, "Provider for testing");
			put("Signature.ABC", "org.apache.harmony.security.tests.support.MySignature1");
			put("Signature.CBA", "org.apache.harmony.security.tests.support.MySignature2");
		}
		
		MyProvider(String name, double version, String info) {
			super(name, version, info);
		}
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
	
	private class MyCertificate extends java.security.cert.Certificate {	
		public  MyCertificate() {
			super("MyCertificateType");
		}
		
		public PublicKey getPublicKey() {
			return new MyPublicKey();
		}
		
		public byte[] getEncoded() {
			return null;
		}
		public void verify(PublicKey key) {}
		
		public void verify(PublicKey key, String sigProvider) {}
		
		public String toString() {
			return "MyCertificate";
		}
	}
}
