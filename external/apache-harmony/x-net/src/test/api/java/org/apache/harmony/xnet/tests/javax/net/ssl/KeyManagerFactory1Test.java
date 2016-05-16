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

package org.apache.harmony.xnet.tests.javax.net.ssl;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;

import org.apache.harmony.xnet.tests.support.SpiEngUtils;
import org.apache.harmony.xnet.tests.support.MyKeyManagerFactorySpi;
import junit.framework.TestCase;

/**
 * Tests for <code>KeyManagerFactory</code> class constructors and methods.
 * 
 */

public class KeyManagerFactory1Test extends TestCase {

    private static final String srvKeyManagerFactory = "KeyManagerFactory";

    private static String defaultAlgorithm = null;

    private static String defaultProviderName = null;

    private static Provider defaultProvider = null;

    private static boolean DEFSupported = false;

    private static final String NotSupportedMsg = "There is no suitable provider for KeyManagerFactory";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static String[] validValues = new String[3];
    static {
        defaultAlgorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (defaultAlgorithm != null) {
            defaultProvider = SpiEngUtils.isSupport(defaultAlgorithm,
                    srvKeyManagerFactory);            
            DEFSupported = (defaultProvider != null);
            defaultProviderName = (DEFSupported ? defaultProvider.getName()
                    : null);
            validValues[0] = defaultAlgorithm;
            validValues[1] = defaultAlgorithm.toUpperCase();
            validValues[2] = defaultAlgorithm.toLowerCase();
        }
    }

    protected KeyManagerFactory[] createKMFac() {
        if (!DEFSupported) {
            fail(defaultAlgorithm + " algorithm is not supported");
            return null;
        }
        KeyManagerFactory[] kMF = new KeyManagerFactory[3];
        try {
            kMF[0] = KeyManagerFactory.getInstance(defaultAlgorithm);
            kMF[1] = KeyManagerFactory.getInstance(defaultAlgorithm,
                    defaultProvider);
            kMF[2] = KeyManagerFactory.getInstance(defaultAlgorithm,
                    defaultProviderName);
            return kMF;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Test for <code>getDefaultAlgorithm()</code> method
     * Assertion: returns value which is specifoed in security property
     */
    public void testGetDefaultAlgorithm() {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        String def = KeyManagerFactory.getDefaultAlgorithm();
        if (defaultAlgorithm == null) {
            assertNull("DefaultAlgorithm must be null", def);
        } else {
            assertEquals("Invalid default algorithm", def, defaultAlgorithm);
        }
        String defA = "Proba.keymanagerfactory.defaul.type";
        Security.setProperty("ssl.KeyManagerFactory.algorithm", defA);
        assertEquals("Incorrect defaultAlgorithm", 
                KeyManagerFactory.getDefaultAlgorithm(), defA);
        if (def == null) {
            def = "";
        }
        Security.setProperty("ssl.KeyManagerFactory.algorithm", def); 
        assertEquals("Incorrect defaultAlgorithm", 
                KeyManagerFactory.getDefaultAlgorithm(), def);        
    }
    
    /**
     * Test for <code>getInstance(String algorithm)</code> method
     * Assertions: 
     * returns security property "ssl.KeyManagerFactory.algorithm";
     * returns instance of KeyManagerFactory
     */
    public void testKeyManagerFactory01() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        KeyManagerFactory keyMF;
        for (int i = 0; i < validValues.length; i++) {
            keyMF = KeyManagerFactory.getInstance(validValues[i]);
            assertTrue("Not KeyManagerFactory object",
                    keyMF instanceof KeyManagerFactory);
            assertEquals("Invalid algorithm", keyMF.getAlgorithm(),
                    validValues[i]);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm)</code> method 
     * Assertion:
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is not correct;
     */
    public void testKeyManagerFactory02() {
        try {
            KeyManagerFactory.getInstance(null);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (algorithm is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyManagerFactory.getInstance(invalidValues[i]);
                fail("NoSuchAlgorithmException was not thrown as expected for algorithm: "
                        .concat(invalidValues[i]));
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method 
     * Assertion: throws IllegalArgumentException when provider is null or empty
     */
    public void testKeyManagerFactory03() throws NoSuchProviderException,
            NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        String provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyManagerFactory.getInstance(validValues[i], provider);
                fail("Expected IllegalArgumentException was not thrown for null provider");
            } catch (IllegalArgumentException e) {
            }
            try {
                KeyManagerFactory.getInstance(validValues[i], "");
                fail("Expected IllegalArgumentException was not thrown for empty provider");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method 
     * Assertion: 
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is not correct;
     */
    public void testKeyManagerFactory04() throws NoSuchProviderException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        try {
            KeyManagerFactory.getInstance(null, defaultProviderName);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (algorithm is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyManagerFactory.getInstance(invalidValues[i],
                        defaultProviderName);
                fail("NoSuchAlgorithmException must be thrown (algorithm: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method 
     * Assertion: throws NoSuchProviderException when provider has
     * invalid value
     */
    public void testKeyManagerFactory05() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        for (int i = 0; i < validValues.length; i++) {
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    KeyManagerFactory.getInstance(validValues[i],
                            invalidValues[j]);
                    fail("NuSuchProviderException must be thrown (algorithm: "
                            + validValues[i] + " provider: " + invalidValues[j]
                            + ")");
                } catch (NoSuchProviderException e) {
                }
            }
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, String provider)</code>
     * method Assertion: returns instance of KeyManagerFactory
     */
    public void testKeyManagerFactory06() throws NoSuchProviderException,
            NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        KeyManagerFactory kMF;
        for (int i = 0; i < validValues.length; i++) {
            kMF = KeyManagerFactory.getInstance(validValues[i],
                    defaultProviderName);
            assertTrue("Not KeyManagerFactory object",
                    kMF instanceof KeyManagerFactory);
            assertEquals("Incorrect algorithm", kMF.getAlgorithm(),
                    validValues[i]);
            assertEquals("Incorrect provider", kMF.getProvider().getName(),
                    defaultProviderName);
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method 
     * Assertion: throws IllegalArgumentException when provider is null
     */
    public void testKeyManagerFactory07() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        Provider provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyManagerFactory.getInstance(validValues[i], provider);
                fail("Expected IllegalArgumentException was not thrown when provider is null");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method 
     * Assertion: 
     * throws NullPointerException when algorithm is null;
     * throws NoSuchAlgorithmException when algorithm is not correct;
     */
    public void testKeyManagerFactory08() {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }        
        try {
            KeyManagerFactory.getInstance(null, defaultProvider);
            fail("NoSuchAlgorithmException or NullPointerException should be thrown (algorithm is null");
        } catch (NoSuchAlgorithmException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyManagerFactory
                        .getInstance(invalidValues[i], defaultProvider);
                fail("Expected NuSuchAlgorithmException was not thrown");
            } catch (NoSuchAlgorithmException e) {
            }
        } 
    }

    /**
     * Test for <code>getInstance(String algorithm, Provider provider)</code>
     * method 
     * Assertion: returns instance of KeyManagerFactory
     */
    public void testKeyManagerFactory09() throws NoSuchAlgorithmException,
            IllegalArgumentException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        KeyManagerFactory kMF;
        for (int i = 0; i < validValues.length; i++) {
            kMF = KeyManagerFactory
                    .getInstance(validValues[i], defaultProvider);
            assertTrue(kMF instanceof KeyManagerFactory);
            assertEquals(kMF.getAlgorithm(), validValues[i]);
            assertEquals(kMF.getProvider(), defaultProvider);
        }
    }

    /**
     * Test for <code>KeyManagerFactory</code> constructor 
     * Assertion: returns KeyManagerFactory object
     */
    public void testKeyManagerFactory10() throws Exception {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        KeyManagerFactorySpi spi = new MyKeyManagerFactorySpi();
        KeyManagerFactory keyMF = new myKeyManagerFactory(spi, defaultProvider,
                defaultAlgorithm);
        assertTrue("Not CertStore object", keyMF instanceof KeyManagerFactory);
        assertEquals("Incorrect algorithm", keyMF.getAlgorithm(),
                defaultAlgorithm);
        assertEquals("Incorrect provider", keyMF.getProvider(), defaultProvider);
        try {
            keyMF.init(null, new char[1]);
            fail("UnrecoverableKeyException must be thrown");
        } catch (UnrecoverableKeyException e) {
            // Expected
        }

        keyMF = new myKeyManagerFactory(null, null, null);
        assertTrue("Not CertStore object", keyMF instanceof KeyManagerFactory);
        assertNull("Aalgorithm must be null", keyMF.getAlgorithm());
        assertNull("Provider must be null", keyMF.getProvider());
        try {
            keyMF.getKeyManagers();
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test for <code>init(KeyStore keyStore, char[] password)</code> and
     * <code>getKeyManagers()</code> 
     * Assertion: returns not empty KeyManager array
     */
    public void testKeyManagerFactory11() throws Exception {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        KeyManagerFactory[] keyMF = createKMFac();
        assertNotNull("KeyManagerFactory object were not created", keyMF);
        KeyStore ksNull = null;
        KeyManager[] km;
        for (int i = 0; i < keyMF.length; i++) {
            keyMF[i].init(ksNull, new char[10]);
            km = keyMF[i].getKeyManagers();
            assertNotNull("Result should not be null", km);
            assertTrue("Length of result KeyManager array should not be 0",
                    (km.length > 0));
        }
        KeyStore ks;
        ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);

        for (int i = 0; i < keyMF.length; i++) {
            try {
                keyMF[i].init(ks, new char[10]);
            } catch (KeyStoreException e) {              
            }
            km = keyMF[i].getKeyManagers();
            assertNotNull("Result has not be null", km);
            assertTrue("Length of result KeyManager array should not be 0",
                    (km.length > 0));
        }
    }

    /**
     * Test for <code>init(ManagerFactoryParameters params)</code> 
     * Assertion:
     * throws InvalidAlgorithmParameterException when params is null
     */
    public void testKeyManagerFactory12() throws NoSuchAlgorithmException {
        if (!DEFSupported) {
            fail(NotSupportedMsg);
            return;
        }
        ManagerFactoryParameters par = null;
        KeyManagerFactory[] keyMF = createKMFac();
        assertNotNull("KeyManagerFactory object were not created", keyMF);
        for (int i = 0; i < keyMF.length; i++) {
            try {
                keyMF[i].init(par);
                fail("InvalidAlgorithmParameterException must be thrown");
            } catch (InvalidAlgorithmParameterException e) {
            }
        }
    }
    
}

/**
 * Additional class for KeyManagerFactory constructor verification
 */
class myKeyManagerFactory extends KeyManagerFactory {
    public myKeyManagerFactory(KeyManagerFactorySpi spi, Provider prov,
            String alg) {
        super(spi, prov, alg);
    }
}
