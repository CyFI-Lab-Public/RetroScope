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

package org.apache.harmony.crypto.tests.javax.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.apache.harmony.security.tests.support.TestKeyPair;

import junit.framework.TestCase;

/**
 * Tests for <code>Cipher</code> class constructors and methods.
 * 
 */
public class Cipher_ImplTest extends TestCase {

	private Provider p1;
	private Provider p2;
	private TestKeyPair tkp = null;
	private Key key = null;
	
	private boolean noKey = false;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		p1 = new MyProvider1();
		p2 = new MyProvider2();
		Security.insertProviderAt(p1, 1);
		Security.insertProviderAt(p2, 1);
		try {
			tkp = new TestKeyPair("DSA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			noKey = true;
			return;
		}
		
		try {
			key = tkp.getPrivate();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			noKey = true;
			return;
		}

	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		Security.removeProvider("MyProvider1");
		Security.removeProvider("MyProvider2");
	}

	/*
	 * Class under test for Cipher getInstance(String)
	 */
	public void testGetInstanceString1() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
		
		Cipher c = Cipher.getInstance("DES");
		assertSame(p2, c.getProvider());
	}

	/*
	 * Class under test for Cipher getInstance(String)
	 */
	public void testGetInstanceString2() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
		
		Cipher c = Cipher.getInstance("DES/CBC/PKCS5Padding");
		assertSame("Case1:", p1, c.getProvider());

		Security.removeProvider(p1.getName());

		c = Cipher.getInstance("DES/CBC/PKCS5Padding");
		assertSame("Case2:", p2, c.getProvider());
	}

	/*
	 * Class under test for Cipher getInstance(String)
	 */
	public void testGetInstanceString3() throws NoSuchAlgorithmException,
            NoSuchPaddingException {
		
		try {
			Cipher.getInstance("DES/CBC/");
			fail("Case1: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES//PKCS5Padding");
			fail("Case2: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES/CBC/IncorrectPadding");
			fail("No expected NoSuchPaddingException");
		} catch (NoSuchPaddingException e) {
		}
	}	

	
	/*
	 * Class under test for Cipher getInstance(String, String)
	 */
	public void testGetInstanceStringString1() throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException {

		try {
			Cipher.getInstance("DES/CBC/", "MyProvider2");
			fail("Case1: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES//PKCS5Padding", "MyProvider2");
			fail("Case2: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES/CBC/IncorrectPadding", "MyProvider2");
			fail("No expected NoSuchPaddingException");
		} catch (NoSuchPaddingException e) {
		}
		
		try {
			Cipher.getInstance("DES/CBC/PKCS5Padding", "IncorrectProvider");
			fail("No expected NoSuchProviderException");
		} catch (NoSuchProviderException e) {
		}		
	}

	/*
	 * Class under test for Cipher getInstance(String, String)
	 */
	public void testGetInstanceStringString2() throws NoSuchAlgorithmException,
            NoSuchProviderException, NoSuchPaddingException {

		Cipher c = Cipher.getInstance("DES", "MyProvider2");
		assertSame("Case1:", p2, c.getProvider());
		
		c = Cipher.getInstance("DES/CBC/PKCS5Padding", "MyProvider2");
		assertSame("Case2:", p2, c.getProvider());
	}
	/*
	 * Class under test for Cipher getInstance(String, Provider)
	 */
	public void testGetInstanceStringProvider1()
            throws NoSuchAlgorithmException, NoSuchPaddingException {

		try {
			Cipher.getInstance("DES/CBC/", p2);
			fail("Case1: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES//PKCS5Padding", p2);
			fail("Case2: No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {
		}

		try {
			Cipher.getInstance("DES/CBC/IncorrectPadding", p2);
			fail("No expected NoSuchProviderException");
		} catch (NoSuchPaddingException e) {
		}
		
		try {
			Cipher.getInstance("DES/CBC/PKCS5Padding", "IncorrectProvider");
			fail("No expected NoSuchProviderException");
		} catch (NoSuchProviderException e) {
		}		
	}

	/*
	 * Class under test for Cipher getInstance(String, Provider)
	 */
	public void testGetInstanceStringProvider2()
            throws NoSuchAlgorithmException, NoSuchPaddingException {

		Cipher c = Cipher.getInstance("DES", p2);
		assertSame("Case1:", p2, c.getProvider());
		
		c = Cipher.getInstance("DES/CBC/PKCS5Padding", p2);
		assertSame("Case2:", p2, c.getProvider());
		assertFalse("getAlgorithm", "DES".equals(c.getAlgorithm()));
	}

	public void testGetBlockSize() throws NoSuchAlgorithmException,
            NoSuchPaddingException {

		Cipher c = Cipher.getInstance("DES");
		assertEquals("getBlockSize", 111, c.getBlockSize());
	}

	public void testGetOutputSize() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {

		Cipher c = Cipher.getInstance("DES");
		try {
			c.getOutputSize(111);
			fail("No expected IllegalStateException");
		} catch (IllegalStateException e){
		}

		if (noKey) {
			return;
		}

		c.init(Cipher.DECRYPT_MODE, key);
		assertEquals("getOutputSize", 121, c.getOutputSize(111));
	}

	public void testGetIV() throws NoSuchAlgorithmException,
            NoSuchPaddingException {

		Cipher c = Cipher.getInstance("DES");
		assertEquals(3, c.getIV().length);
	}

	/*
	 * Class under test for byte[] update(byte[])
	 */
	public void testUpdatebyteArray() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {

		Cipher c = Cipher.getInstance("DES");

		byte[] b = {1,2,3,4};
		try {
			c.update(b);
			fail("No expected IllegalStateException");
		} catch (IllegalStateException e){
		}
		if (noKey) {
			return;
		}

		c.init(Cipher.DECRYPT_MODE, key);
		try {
			c.update(null);
			fail("No expected IllegalArgumentException");
		} catch (IllegalArgumentException e){
		}
		assertNull(c.update(new byte[0]));
	}
	
	private class MyProvider1 extends Provider {
		MyProvider1() {
			super("MyProvider1", 1.0, "Provider1 for testing");
			put("Cipher.DES/CBC/PKCS5Padding", "org.apache.harmony.crypto.tests.support.MyCipher");
			put("Cipher.DES/ECB/PKCS5Padding", "org.apache.harmony.crypto.tests.support.MyCipher");
		}	
	}
	private class MyProvider2 extends Provider {
		MyProvider2() {
			super("MyProvider2", 1.0, "Provider2 for testing");
			put("Cipher.DES", "org.apache.harmony.crypto.tests.support.MyCipher");
		}	
		
	}
}