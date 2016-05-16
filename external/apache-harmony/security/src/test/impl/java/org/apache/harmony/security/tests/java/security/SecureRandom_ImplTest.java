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

package org.apache.harmony.security.tests.java.security;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import junit.framework.TestCase;

/**
 *
 * Tests for internal Secure Random implementation based on Random
 * 
 */
public class SecureRandom_ImplTest extends TestCase {
	
	/**
	 * Registered providers
	 */
	Provider providers[] = Security.getProviders();
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		// remove all registered providers
		for (int i = 0; i < providers.length; i++) {
			Security.removeProvider(providers[i].getName());
		}
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		// restore all registered providers
		for (int i = 0; i < providers.length; i++) {
			Security.addProvider(providers[i]);
		}
	}

	/*
	 * Class under test for void setSeed(long)
	 */
	public final void testSetSeedlong() {
		SecureRandom sr = new SecureRandom();
		sr.setSeed(0);
		sr.setSeed(-1);
		sr.setSeed(11111111111L);
	}

	public final void testNextBytes() {
		SecureRandom sr = new SecureRandom();
		sr.nextBytes(new byte[20]);
		sr.nextBytes(new byte[1]);
		
		//Not specified behavior: throws NullPointerException if bytes is null
		try {
			sr.nextBytes(null);
		} catch (NullPointerException e) {	
		}
		sr.nextBytes(new byte[5]);
	}

	/*
	 * Class under test for SecureRandom getInstance(String)
	 */
	public final void testGetInstanceString() {
		try {
			SecureRandom.getInstance("SHA1PRNG");
			fail("No expected NoSuchAlgorithmException");
		} catch (NoSuchAlgorithmException e) {	
		}
	}
	
	public final void testGetProvider() {
        assertNull("Non null provider", new SecureRandom().getProvider());
	}
	
	public final void testGetAlgorithm() {
        //test default implementation
		SecureRandom sr = new SecureRandom();
        assertEquals("Incorrect algorithm", "SHA1PRNG", sr.getAlgorithm());
        assertNull("Incorrect provider", sr.getProvider());
        
        //just in case
        sr.nextBytes(new byte[100]);
	}
	
	/*
	 * Class under test for void setSeed(byte[])
	 */
	public final void testSetSeedbyteArray() {
		SecureRandom sr = new SecureRandom();

		//Not specified behavior: throws NullPointerException if bytes is null
		try {
			sr.setSeed(null);
		} catch (NullPointerException e) {	
		}
		
		byte[] seed = {1,2,3,4};
		sr.setSeed(seed);
		
		byte[] seed1 = {1,2,3,4, -2, 100, 9, 111};
		sr.setSeed(seed1);
	}

	public final void testGetSeed() {
		byte[] seed = SecureRandom.getSeed(5);
		new SecureRandom(seed).nextBytes(new byte[20]);
	}

	public final void testGenerateSeed() {
		SecureRandom sr = new SecureRandom();
		byte[] seed = sr.generateSeed(5);
		new SecureRandom(seed).nextBytes(new byte[20]);
	}
}
