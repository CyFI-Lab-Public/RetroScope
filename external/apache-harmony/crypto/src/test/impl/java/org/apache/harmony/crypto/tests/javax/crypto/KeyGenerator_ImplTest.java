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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyGenerator;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import junit.framework.TestCase;


/**
 * Tests for <code>KeyGenerator</code> class constructors and methods
 * 
 */

public class KeyGenerator_ImplTest extends TestCase {
    
    private static final String srvKeyGenerator = "KeyGenerator";

    private static final String defaultAlg = "MyKeyGen";
    
    private static final String KeyGeneratorProviderClass = "org.apache.harmony.crypto.tests.support.MyKeyGeneratorSpi";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static final String[] validValues;

    static {
        validValues = new String[4];
        validValues[0] = defaultAlg;
        validValues[1] = defaultAlg.toLowerCase();
        validValues[2] = "myKeyGen";
        validValues[3] = "mYkeYgeN";
    }

    Provider mProv;

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MyKGProvider", "Testing provider", 
                srvKeyGenerator.concat(".").concat(defaultAlg), 
                KeyGeneratorProviderClass);
        Security.insertProviderAt(mProv, 1);
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }

    private void checkResult(KeyGenerator keyGen) {
        AlgorithmParameterSpec paramsNull = null;
        AlgorithmParameterSpec params = new APSpec();
        try {
            keyGen.init(0, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        try {
            keyGen.init(77, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        keyGen.init(78, new SecureRandom());
        try {
            keyGen.init(new SecureRandom());
            fail("IllegalArgumentException must be thrown");                
        } catch (IllegalArgumentException e) {
        }
        assertNull("generateKey must return null", keyGen.generateKey());
        try {
            keyGen.init(paramsNull, new SecureRandom());
            fail("InvalidAlgorithmParameterException must be thrown");
        } catch (InvalidAlgorithmParameterException e) {
        }
        try {
            keyGen.init(params, new SecureRandom());                
        } catch (Exception e) {
            fail("Unexpected: " + e.toString() + " was thrown");
        }
        try {
            keyGen.init(paramsNull);
            fail("InvalidAlgorithmParameterException must be thrown");
        } catch (InvalidAlgorithmParameterException e) {
        }
        try {
            keyGen.init(params);                
        } catch (Exception e) {
            fail("Unexpected: " + e.toString() + " was thrown");
        }
    }
    
    /**
     * Test for <code>getInstance(String algorithm)</code> method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is incorrect;
     * returns KeyGenerator object
     */
    public void testGetInstance01() throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException {
        try {
            KeyGenerator.getInstance(null);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyGenerator.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        KeyGenerator keyGen;
        for (int i = 0; i < validValues.length; i++) {
            keyGen = KeyGenerator.getInstance(validValues[i]);
            assertEquals("Incorrect algorithm", keyGen.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyGen.getProvider(), mProv);
            checkResult(keyGen);
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
     * returns KeyGenerator object
     */
    public void testGetInstance02() throws NoSuchAlgorithmException,
            NoSuchProviderException, IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException {            
        try {
            KeyGenerator.getInstance(null, mProv.getName());
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyGenerator.getInstance(invalidValues[i], mProv
                        .getName());
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        String prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyGenerator.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            try {
                KeyGenerator.getInstance(validValues[i], "");
                fail("IllegalArgumentException must be thrown when provider is empty (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    KeyGenerator.getInstance(validValues[i],
                            invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (algorithm: "
                            .concat(invalidValues[i]).concat(" provider: ")
                            .concat(invalidValues[j]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
        KeyGenerator keyGen;
        for (int i = 0; i < validValues.length; i++) {
            keyGen = KeyGenerator.getInstance(validValues[i], mProv
                    .getName());
            assertEquals("Incorrect algorithm", keyGen.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyGen.getProvider().getName(),
                    mProv.getName());
            checkResult(keyGen);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is null or incorrect;
     * throws IllegalArgumentException when provider is null;
     * returns KeyGenerator object
     */
    public void testGetInstance03() throws NoSuchAlgorithmException,
            IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException {
        try {
            KeyGenerator.getInstance(null, mProv);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyGenerator.getInstance(invalidValues[i], mProv);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        Provider prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyGenerator.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        KeyGenerator keyGen;
        for (int i = 0; i < validValues.length; i++) {
            keyGen = KeyGenerator.getInstance(validValues[i], mProv);
            assertEquals("Incorrect algorithm", keyGen.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyGen.getProvider(), mProv);
            checkResult(keyGen);
        }
    }
}

class APSpec implements AlgorithmParameterSpec {
    
}
