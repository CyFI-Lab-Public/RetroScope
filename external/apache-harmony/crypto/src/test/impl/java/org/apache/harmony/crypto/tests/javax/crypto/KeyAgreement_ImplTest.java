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
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyAgreement;
import javax.crypto.ShortBufferException;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import junit.framework.TestCase;


/**
 * Tests for KeyAgreement class constructors and methods
 * 
 */

public class KeyAgreement_ImplTest extends TestCase {
    
    private static final String srvKeyAgreement = "KeyAgreement";

    private static final String defaultAlg = "MyKeyAgr";
    
    private static final String KeyAgreementProviderClass = "org.apache.harmony.crypto.tests.support.MyKeyAgreementSpi";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static final String[] validValues;

    static {
        validValues = new String[4];
        validValues[0] = defaultAlg;
        validValues[1] = defaultAlg.toLowerCase();
        validValues[2] = "myKeyagr";
        validValues[3] = "mYkeYAGR";
    }

    Provider mProv;

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MyKAProvider", "Testing provider", 
                srvKeyAgreement.concat(".").concat(defaultAlg), 
                KeyAgreementProviderClass);
        Security.insertProviderAt(mProv, 1);
    }
    
    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }
    
    protected void checkResult(KeyAgreement keyAgr) 
            throws InvalidKeyException, ShortBufferException, 
            NoSuchAlgorithmException, IllegalStateException,
            InvalidAlgorithmParameterException {
        assertNull("Not null result", keyAgr.doPhase(null, true));
        try {
            keyAgr.doPhase(null, false);
            fail("IllegalStateException must be thrown");
        } catch (IllegalStateException e) {
        }
        byte[] bb = keyAgr.generateSecret();
        assertEquals("Length is not 0", bb.length, 0);        
        assertEquals("Returned integer is not 0", 
                keyAgr.generateSecret(new byte[1], 10), 
                -1);
        assertNull("Not null result", keyAgr.generateSecret("aaa"));
        try {
            keyAgr.generateSecret("");
            fail("NoSuchAlgorithmException must be thrown");
        } catch (NoSuchAlgorithmException e) {
        }
        Key key = null;
        try {
            keyAgr.init(key, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
        AlgorithmParameterSpec params = null;
        try {
            keyAgr.init(key, params, new SecureRandom());
            fail("IllegalArgumentException must be thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Test for <code>getInstance(String algorithm)</code> method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is incorrect;
     * returns KeyAgreement object
     */
    public void testGetInstance01() throws NoSuchAlgorithmException,
            InvalidKeySpecException, InvalidKeyException,
            ShortBufferException, InvalidAlgorithmParameterException {
        try {
            KeyAgreement.getInstance(null);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyAgreement.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        KeyAgreement keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = KeyAgreement.getInstance(validValues[i]);
            assertEquals("Incorrect algorithm", keyAgr.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyAgr.getProvider(), mProv);
            checkResult(keyAgr);
        }
    }
    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method
     * Assertions: 
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is null or incorrect;
     * throws IllegalArgumentException when provider is null or null;
     * throws NoSuchProviderException when provider is available;
     * returns KeyAgreement object
     */
    public void testGetInstance02() throws NoSuchAlgorithmException,
            NoSuchProviderException, IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException,
            ShortBufferException, InvalidAlgorithmParameterException {            
        try {
            KeyAgreement.getInstance(null, mProv.getName());
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyAgreement.getInstance(invalidValues[i], mProv
                        .getName());
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        String prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyAgreement.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            try {
                KeyAgreement.getInstance(validValues[i], "");
                fail("IllegalArgumentException must be thrown when provider is empty (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    KeyAgreement.getInstance(validValues[i],
                            invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (algorithm: "
                            .concat(invalidValues[i]).concat(" provider: ")
                            .concat(invalidValues[j]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
        KeyAgreement keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = KeyAgreement.getInstance(validValues[i], mProv
                    .getName());
            assertEquals("Incorrect algorithm", keyAgr.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyAgr.getProvider().getName(),
                    mProv.getName());
            checkResult(keyAgr);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is null or incorrect;
     * throws IllegalArgumentException when provider is null;
     * returns KeyAgreement object
     * 
     */
    public void testGetInstance03() throws NoSuchAlgorithmException,
            IllegalArgumentException,
            InvalidKeySpecException, InvalidKeyException,
            ShortBufferException, InvalidAlgorithmParameterException {
        try {
            KeyAgreement.getInstance(null, mProv);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown if algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyAgreement.getInstance(invalidValues[i], mProv);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        Provider prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyAgreement.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        KeyAgreement keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = KeyAgreement.getInstance(validValues[i], mProv);
            assertEquals("Incorrect algorithm", keyAgr.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyAgr.getProvider(), mProv);
            checkResult(keyAgr);
       }
    }
}
