/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.harmony.security.tests.java.security;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.spec.DSAParameterSpec;
import java.util.Locale;

public class Signature2Test extends junit.framework.TestCase {

	private static final String MESSAGE = "abc";
    static KeyPair keys;
    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024);
            keys = keyGen.generateKeyPair();
        } catch (Exception e) {
            fail(e.toString());
        }
    }
    
	/**
	 * @tests java.security.Signature#clone()
	 */
	public void test_clone() throws Exception {
       		Signature s = Signature.getInstance("DSA");
       		try {
       			s.clone();
       			fail("A Signature may not be cloneable");
       		} catch (CloneNotSupportedException e) {
       			// Expected - a Signature may not be cloneable
       		}
	}

	/**
	 * @tests java.security.Signature#getAlgorithm()
	 */
	public void test_getAlgorithm() throws Exception {
       		String alg = Signature.getInstance("DSA").getAlgorithm();
       		assertTrue("getAlgorithm did not get DSA (" + alg + ")", alg
       				.indexOf("DSA") != -1);
	}

	/**
	 * @tests java.security.Signature#getInstance(java.lang.String)
	 */
	public void test_getInstanceLjava_lang_String() throws Exception {
		Signature.getInstance("DSA");
	}

	/**
	 * @tests java.security.Signature#getInstance(java.lang.String,
	 *        java.lang.String)
	 */
	public void test_getInstanceLjava_lang_StringLjava_lang_String() throws Exception {
       		Provider[] providers = Security.getProviders("Signature.DSA");

       		for (int i = 0; i < providers.length; i++) {
       			Signature.getInstance("DSA", providers[i].getName());
       		}// end for
	}
    
    /**
     * @tests java.security.Signature#getParameters()
     */
    public void test_getParameters() throws Exception {
        Signature sig = Signature.getInstance("DSA");
        try {
            sig.getParameters();
        } catch (UnsupportedOperationException e) {
            // Could be that the operation is not supported
        }
    }

	/**
	 * @tests java.security.Signature#getParameter(java.lang.String)
	 */
	public void test_getParameterLjava_lang_String() throws Exception {
		Signature sig = Signature.getInstance("DSA");

		try {
			sig.getParameter("r");
			sig.getParameter("s");
		} catch (UnsupportedOperationException e) {
		}
	}

	/**
	 * @tests java.security.Signature#getProvider()
	 */
	public void test_getProvider() throws Exception {
       		Provider p = Signature.getInstance("DSA").getProvider();
       		assertNotNull("provider is null", p);
	}

	/**
	 * @tests java.security.Signature#initSign(java.security.PrivateKey)
	 */
	public void test_initSignLjava_security_PrivateKey() throws Exception {
		Signature.getInstance("DSA").initSign(keys.getPrivate());
	}

	/**
	 * @tests java.security.Signature#initVerify(java.security.PublicKey)
	 */
	public void test_initVerifyLjava_security_PublicKey() throws Exception {
		Signature.getInstance("DSA").initVerify(keys.getPublic());
	}

	/**
	 * @tests java.security.Signature#setParameter(java.lang.String,
	 *        java.lang.Object)
	 */
	public void test_setParameterLjava_lang_StringLjava_lang_Object() throws Exception {
		Signature sig = Signature.getInstance("DSA");

		try {
			sig.setParameter("r", BigInteger.ONE);
			sig.setParameter("s", BigInteger.ONE);
		} catch (InvalidParameterException e) {
			// Could be that it's an invalid param for the found algorithm
		} catch (UnsupportedOperationException e) {
			// Could be that the operation is not supported
		}
	}

	/**
	 * @tests java.security.Signature#setParameter(java.security.spec.AlgorithmParameterSpec)
	 */
	public void test_setParameterLjava_security_spec_AlgorithmParameterSpec() throws Exception {
		Signature sig = Signature.getInstance("DSA");

		try {
			DSAParameterSpec spec = new DSAParameterSpec(BigInteger.ONE,
					BigInteger.ONE, BigInteger.ONE);
			sig.setParameter(spec);
		} catch (InvalidParameterException e) {
			// Could be that it's an invalid param for the found algorithm
		} catch (UnsupportedOperationException e) {
			// Could be that the operation is not supported
		}
	}

	/**
	 * @tests java.security.Signature#sign()
	 */
	public void test_sign() throws Exception {
		Signature sig = Signature.getInstance("DSA");
		sig.initSign(keys.getPrivate());
		sig.update(MESSAGE.getBytes());
		sig.sign();
	}

	/**
	 * @tests java.security.Signature#toString()
	 */
	public void test_toString() throws Exception {
		String str = Signature.getInstance("DSA").toString();
		assertNotNull("toString is null", str);
	}

	/**
	 * @tests java.security.Signature#update(byte[])
	 */
	public void test_update$B() throws Exception {
		Signature sig = Signature.getInstance("DSA");
		sig.initSign(keys.getPrivate());

		byte[] bytes = MESSAGE.getBytes();
		sig.update(bytes);
	}

	/**
	 * @tests java.security.Signature#update(byte[], int, int)
	 */
	public void test_update$BII() throws Exception {
		Signature sig = Signature.getInstance("DSA");
		sig.initSign(keys.getPrivate());

		byte[] bytes = MESSAGE.getBytes();
		sig.update(bytes, 0, bytes.length);
	}

	/**
	 * @tests java.security.Signature#update(byte)
	 */
	public void test_updateB() throws Exception {
		Signature sig = Signature.getInstance("DSA");
		sig.initSign(keys.getPrivate());

		sig.update(MESSAGE.getBytes()[0]);
	}

	/**
	 * @tests java.security.Signature#verify(byte[])
	 */
	public void test_verify$B() throws Exception {
		Signature sig = Signature.getInstance("DSA");
		sig.initSign(keys.getPrivate());
		sig.update(MESSAGE.getBytes());
		byte[] signature = sig.sign();


		sig.initVerify(keys.getPublic());
		sig.update(MESSAGE.getBytes());
		assertTrue("Sign/Verify does not pass", sig.verify(signature));
	}
    
    //Regression Test for HARMONY-4916
    public void test_getInstance_withI18n() throws Exception {
        // Enfore that providers information has been loaded.
        Signature.getInstance("DSA");
        Locale defaultLocale = Locale.getDefault();
        try {
            /**
             * In locale("tr"), char 'i' will be transferred to an upper case
             * other char than 'I'. Thus in security architecture, all
             * manipulation to the string representing an algorithm name or
             * standard property shall be treated as locale neutral
             */
            Locale.setDefault(new Locale("tr"));
            Signature.getInstance("MD5withRSA");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
