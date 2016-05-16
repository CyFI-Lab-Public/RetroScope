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
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import junit.framework.TestCase;


/**
 * Tests for Mac class constructors and methods
 * 
 */

public class Mac_ImplTest extends TestCase {
    
    private static final String srvMac = "Mac";

    private static final String defaultAlg = "MyMacProv";
    
    private static final String MacProviderClass = "org.apache.harmony.crypto.tests.support.MyMacSpi";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static final String[] validValues;

    static {
        validValues = new String[5];
        validValues[0] = defaultAlg;
        validValues[1] = defaultAlg.toUpperCase();
        validValues[2] = defaultAlg.toLowerCase();
        validValues[3] = "myMACprov";
        validValues[4] = "MyMaCpRoV";
    }

    Provider mProv;

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MyMacProvider", "Testing provider", 
                srvMac.concat(".").concat(defaultAlg), 
                MacProviderClass);
        Security.insertProviderAt(mProv, 2);
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }
    
    protected void checkResult(Mac mac) throws InvalidKeyException,
            InvalidAlgorithmParameterException {
        assertEquals("Incorrect MacLength", mac.getMacLength(), 0);
        byte [] b = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        SecretKeySpec scs = new SecretKeySpec(b, "SHA1");
        AlgParSpec parms = new AlgParSpec();
        tmpKey tKey = new tmpKey();
        mac.init(scs);        
        byte[] bb = mac.doFinal();
        assertEquals(bb.length, 0);
        mac.reset();
        bb = mac.doFinal();
        assertEquals(bb.length, 1);
        try {
            mac.init(null);
            fail("InvalidKeyException should be thrown");
        } catch (InvalidKeyException e) {
        }
        try {
            mac.init(null, null);
            fail("InvalidKeyException should be thrown");
        } catch (InvalidKeyException e) {
        }
        mac.init(scs, null);
        mac.init(scs, parms);
        try {
            mac.init(tKey, null);
            fail("InvalidAlgorithmParameterException or IllegalArgumentException "
                    + "should be thrown for incorrect parameter");
        } catch (IllegalArgumentException e) {
        } catch (InvalidAlgorithmParameterException e) {
        }
        try {
            mac.clone();
            fail("No expected CloneNotSupportedException"); 
        } catch (CloneNotSupportedException e) {           
        }
    }

    /**
     * Test for <code>getInstance(String algorithm)</code> method
     * Assertions:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is not correct;
     * returns Mac object
     */
    public void testGetInstance01() throws NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {
        try {
            Mac.getInstance(null);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown when algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                Mac.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        Mac keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = Mac.getInstance(validValues[i]);
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
     * throws NoSuchAlgorithmException when algorithm is not correct;
     * throws IllegalArgumentException when provider is null;
     * throws NoSuchProviderException when provider is available;
     * returns Mac object
     */
    public void testGetInstance02() throws NoSuchAlgorithmException,
            NoSuchProviderException, IllegalArgumentException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {            
        try {
            Mac.getInstance(null, mProv.getName());
            fail("NullPointerException or NoSuchAlgorithmException should be thrown when algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                Mac.getInstance(invalidValues[i], mProv
                        .getName());
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        String prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                Mac.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        for (int i = 0; i < validValues.length; i++) {
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    Mac.getInstance(validValues[i],
                            invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (algorithm: "
                            .concat(invalidValues[i]).concat(" provider: ")
                            .concat(invalidValues[j]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
        Mac keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = Mac.getInstance(validValues[i], mProv
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
     * throws NoSuchAlgorithmException when algorithm is not correct;
     * throws IllegalArgumentException when provider is null;
     * returns Mac object
     */
    public void testGetInstance03() throws NoSuchAlgorithmException,
            IllegalArgumentException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {
        try {
            Mac.getInstance(null, mProv);
            fail("NullPointerException or NoSuchAlgorithmException should be thrown when algorithm is null");
        } catch (NullPointerException e) {
        } catch (NoSuchAlgorithmException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                Mac.getInstance(invalidValues[i], mProv);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
        Provider prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                Mac.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        Mac keyAgr;
        for (int i = 0; i < validValues.length; i++) {
            keyAgr = Mac.getInstance(validValues[i], mProv);
            assertEquals("Incorrect algorithm", keyAgr.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", keyAgr.getProvider(), mProv);
            checkResult(keyAgr);
       }
    }
    public static class AlgParSpec implements AlgorithmParameterSpec {
        
    }
    public static class tmpKey implements Key {
        public tmpKey() {
            
        }
        public String getAlgorithm() {
            return "Test";
        }
        public String getFormat() {
            return "Format";
        }
        public byte[] getEncoded() {
            return null;
        }
    }
}
