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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.crypto.tests.javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import junit.framework.TestCase;


/**
 * Tests for <code>SecretKeyFactory</code> class constructors and methods
 * 
 */

public class SecretKeyFactory_ImplTest extends TestCase {
    
    private static final String srvSecretKeyFactory = "SecretKeyFactory";
    private static final String defaultAlg = "MySecretKey";
    private static final String SecretKeyFactoryProviderClass = "org.apache.harmony.crypto.tests.support.MySecretKeyFactorySpi";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static final String[] validValues;

    static {
        validValues = new String[4];
        validValues[0] = defaultAlg;
        validValues[1] = defaultAlg.toLowerCase();
        validValues[2] = "mySECRETkey";
        validValues[3] = "MYSECretkey";
    }

    Provider mProv;

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MySKFProvider", "Testing provider", 
                srvSecretKeyFactory.concat(".").concat(defaultAlg), 
                SecretKeyFactoryProviderClass);
        Security.insertProviderAt(mProv, 2);
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }

    private void checkResult(SecretKeyFactory skf) throws InvalidKeyException,
            InvalidKeySpecException {
        SecretKey sk;
        KeySpec keySpec;        
        sk = skf.generateSecret(null);
        assertNull("generateSecret method must return null", sk);
        sk = skf.translateKey(null);
        assertNull("translateKey method must return null", sk);
        keySpec = skf.getKeySpec(null, null);
        assertNull("getKeySpec method must return null", keySpec);
    }
    /**
     * Test for <code>getInstance(String algorithm)</code> method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is incorrect;
     * returns SecretKeyFactory object
     */
    public void testGetInstance01() throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        try {
            SecretKeyFactory.getInstance(null);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SecretKeyFactory.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        SecretKeyFactory skf;
        for (int i = 0; i < validValues.length; i++) {
            skf = SecretKeyFactory.getInstance(validValues[i]);
            assertEquals("Incorrect algorithm", skf.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", skf.getProvider(), mProv);
            checkResult(skf);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method
     * Assertions: 
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is null or incorrect;
     * throws IllegalArgumentException when provider is null or empty;
     * throws NoSuchProviderException when provider is available;
     * returns SecretKeyFactory object
     */
    public void testGetInstance02() throws NoSuchAlgorithmException,
            NoSuchProviderException, IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException {            
        try {
            SecretKeyFactory.getInstance(null, mProv.getName());
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SecretKeyFactory.getInstance(invalidValues[i], mProv
                        .getName());
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        String prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                SecretKeyFactory.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            try {
                SecretKeyFactory.getInstance(validValues[i], "");
                fail("IllegalArgumentException must be thrown when provider is empty (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    SecretKeyFactory.getInstance(validValues[i],
                            invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (algorithm: "
                            .concat(invalidValues[i]).concat(" provider: ")
                            .concat(invalidValues[j]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
        SecretKeyFactory skf;
        for (int i = 0; i < validValues.length; i++) {
            skf = SecretKeyFactory.getInstance(validValues[i], mProv
                    .getName());
            assertEquals("Incorrect algorithm", skf.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", skf.getProvider().getName(),
                    mProv.getName());
            checkResult(skf);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is null or incorrect;
     * throws IllegalArgumentException when provider is null;
     * returns SecretKeyFactory object
     */
    public void testGetInstance03() throws NoSuchAlgorithmException,
            IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException {
        try {
            SecretKeyFactory.getInstance(null, mProv);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                SecretKeyFactory.getInstance(invalidValues[i], mProv);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        Provider prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                SecretKeyFactory.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        SecretKeyFactory skf;
        for (int i = 0; i < validValues.length; i++) {
            skf = SecretKeyFactory.getInstance(validValues[i], mProv);
            assertEquals("Incorrect algorithm", skf.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", skf.getProvider(), mProv);
            checkResult(skf);
        }
    }
}
