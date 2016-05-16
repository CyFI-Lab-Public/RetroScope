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

package org.apache.harmony.security.tests.java.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.crypto.spec.SecretKeySpec;

import org.apache.harmony.security.tests.support.KeyStoreTestSupport;
import org.apache.harmony.security.tests.support.MyLoadStoreParams;
import org.apache.harmony.security.tests.support.SpiEngUtils;
import org.apache.harmony.security.tests.support.cert.MyCertificate;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyStore</code> constructor and methods
 * 
 */

public class KeyStore_Impl2Test extends TestCase {

    private static final String KeyStoreProviderClass = 
        "org.apache.harmony.security.tests.support.MyKeyStoreSpi";

    private static final String defaultAlg = "KeyStore";

    private static final String[] invalidValues = SpiEngUtils.invalidValues;

    private static final String[] validValues;

    static {
        validValues = new String[4];
        validValues[0] = defaultAlg;
        validValues[1] = defaultAlg.toLowerCase();
        validValues[2] = "kEyStOrE";
        validValues[3] = "KeysTORE";
    }

    Provider mProv;

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MyKSProvider",
                "Testing provider", KeyStoreTestSupport.srvKeyStore.concat(".").concat(
                        defaultAlg), KeyStoreProviderClass);
        Security.insertProviderAt(mProv, 2);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }

    private void checkResult(KeyStore keyS) throws KeyStoreException,
            IOException, CertificateException, NoSuchAlgorithmException,
            UnrecoverableKeyException {
        char[] pass = { 'a', 'b', 'c' };
        String alias = "aaa";        
        keyS.load(null, pass);
        assertNull("getKey must return null", keyS.getKey(alias, pass));
        assertNull("getCertificate must return null", keyS
                .getCertificate(alias));
        assertNull("getCertificateChain must return null", keyS
                .getCertificateChain(alias));
        assertEquals("Incorrect result of getCreationDate", keyS
                .getCreationDate(alias), new Date(0));
        assertEquals("Incorrect result of size", keyS.size(), 0);
        assertFalse("Incorrect result of isCertificateEntry", keyS
                .isCertificateEntry(alias));
        assertFalse("Incorrect result of isKeyEntry", keyS.isKeyEntry(alias));
        assertFalse("Incorrect result of containsAlias", keyS
                .containsAlias(alias));
        assertEquals("Incorrect result of getCertificateAlias", keyS
                .getCertificateAlias(null), "");
        try {
            keyS.setCertificateEntry(alias, null);
            fail("KeyStoreException must be thrown because this method is not supported");
        } catch (KeyStoreException e) {
        }
        try {
            keyS.setEntry(alias, null, null);
            fail("NullPointerException must be thrown entry is null");
        } catch (NullPointerException e) {
        }
        KeyStore.TrustedCertificateEntry entry = new KeyStore.TrustedCertificateEntry(
                new MyCertificate("type", new byte[0]));
        try {
            keyS.setEntry(alias, entry, null);
            fail("KeyStoreException must be thrown because this method is not supported");
        } catch (KeyStoreException e) {
        }
        try {
            keyS.setKeyEntry(alias, new byte[0], null);
            fail("KeyStoreException must be thrown because this method is not supported");
        } catch (KeyStoreException e) {
        }
        try {
            keyS.setKeyEntry(alias, null, null);
            fail("KeyStoreException must be thrown because this method is not supported");
        } catch (KeyStoreException e) {
        }
        try {
            keyS.store(new ByteArrayOutputStream(), null);
            fail("IOException must be thrown");
        } catch (IOException e) {
        }        
        try {
            keyS.store(null, new char[0]);
            fail("IOException or NullPointerException must be thrown for null OutputStream");
        } catch (IOException e) {
        } catch (NullPointerException e) {            
        }
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try {
            keyS.store(ba, new char[0]);
            fail("IOException must be thrown");
        } catch (IOException e) {
        }       

        KeyStore.LoadStoreParameter lParam = new MyLoadStoreParams(
                new KeyStore.PasswordProtection(new char[0]));        
        try {
            keyS.store(null);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
        }

        
        //No exception should be thrown out.
        keyS.load(null);

        try {
            keyS.store(lParam);
            fail("UnsupportedOperationException must be thrown");
        } catch (UnsupportedOperationException e) {
        }

        keyS.load(lParam);

        //make it compilable on 1.5
        Class c = alias.getClass();
        assertFalse("Incorrect result of entryInstanceOf", keyS
                .entryInstanceOf(alias, c));
    }

    private void checkKeyStoreException(KeyStore keyS)
            throws KeyStoreException, UnrecoverableKeyException,
            UnrecoverableEntryException, NoSuchAlgorithmException, IOException,
            CertificateException {
        String alias = "aaa";
        String eMsg = "IllegalStateException must be thrown because key store was not initialized";
        try {
            keyS.aliases();
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.containsAlias(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.deleteEntry(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            //make it compilable on 1.5
            Class c = alias.getClass();
            keyS.entryInstanceOf(alias, c);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.getCertificate(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        MyCertificate mc = new MyCertificate("type", new byte[0]);
        try {
            keyS.getCertificateAlias(mc);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.getCertificateChain(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.getCreationDate(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(
                new char[0]);
        try {
            keyS.getEntry(alias, pp);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.getKey(alias, new char[0]);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.isCertificateEntry(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.isKeyEntry(alias);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        KeyStore.TrustedCertificateEntry entry = new KeyStore.TrustedCertificateEntry(
                mc);
        Certificate[] chain = { mc };
        try {
            keyS.setEntry(alias, entry, pp);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.setKeyEntry(alias, new byte[0], chain);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        SecretKeySpec sks = new SecretKeySpec(new byte[10], "type");
        try {
            keyS.setKeyEntry(alias, sks, new char[0], chain);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        try {
            keyS.size();
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try {
            keyS.store(ba, new char[0]);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
        KeyStore.LoadStoreParameter lParam = new MyLoadStoreParams(
                new KeyStore.PasswordProtection(new char[0]));
        try {
            keyS.store(lParam);
            fail(eMsg);
        } catch (KeyStoreException e) {
        }
    }


    /**
     * Test for <code>getInstance(String type)</code> method 
     * Assertions:
     * throws NullPointerException when type is null
     * throws KeyStoreException when type is  not available; 
     * returns KeyStore object
     * 
     */
    public void testGetInstance01() throws KeyStoreException,
            UnrecoverableKeyException, UnrecoverableEntryException,
            NoSuchAlgorithmException, IOException, CertificateException {
        try {
            KeyStore.getInstance(null);
            fail("NullPointerException or KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyStore.getInstance(invalidValues[i]);
                fail("KeyStoreException must be thrown (type: ".concat(
                        invalidValues[i]).concat(")"));
            } catch (KeyStoreException e) {
            }
        }
        KeyStore keyS;
        for (int i = 0; i < validValues.length; i++) {
            keyS = KeyStore.getInstance(validValues[i]);
            assertEquals("Incorrect type", keyS.getType(), validValues[i]);
            assertEquals("Incorrect provider", keyS.getProvider(), mProv);
            checkKeyStoreException(keyS);
            checkResult(keyS);
        }
    }

    /**
     * Test for <code>getInstance(String type, String provider)</code> method
     * Assertions: 
     * throws NullPointerException when type is null
     * throws KeyStoreException when type is  not available; 
     * throws IllegalArgumentException when provider is null; 
     * throws NoSuchProviderException when provider is available; 
     * returns KeyStore object
     */
    public void testGetInstance02() throws KeyStoreException,
            NoSuchProviderException, IllegalArgumentException,
            UnrecoverableKeyException, UnrecoverableEntryException,
            NoSuchAlgorithmException, IOException, CertificateException {
        try {
            KeyStore.getInstance(null, mProv.getName());
            fail("NullPointerException or KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyStore.getInstance(invalidValues[i], mProv.getName());
                fail("KeyStoreException must be thrown (type: ".concat(
                        invalidValues[i]).concat(")"));
            } catch (KeyStoreException e) {
            }
        }
        String prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyStore.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (type: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        for (int i = 0; i < validValues.length; i++) {
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    KeyStore.getInstance(validValues[i], invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (type: "
                            .concat(invalidValues[i]).concat(" provider: ")
                            .concat(invalidValues[j]).concat(")"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
        KeyStore keyS;
        for (int i = 0; i < validValues.length; i++) {
            keyS = KeyStore.getInstance(validValues[i], mProv.getName());
            assertEquals("Incorrect type", keyS.getType(), validValues[i]);
            assertEquals("Incorrect provider", keyS.getProvider().getName(),
                    mProv.getName());
            checkKeyStoreException(keyS);
            checkResult(keyS);
        }
    }

    /**
     * Test for <code>getInstance(String type, Provider provider)</code>
     * method 
     * Assertions: 
     * throws NullPointerException when type is null
        } catch (KeyStoreException e) {
     * throws IllegalArgumentException when provider is null; 
     * returns KeyStore object
     */
    public void testGetInstance03() throws KeyStoreException,
            IllegalArgumentException, UnrecoverableKeyException,
            UnrecoverableEntryException, NoSuchAlgorithmException, IOException,
            CertificateException {
        try {
            KeyStore.getInstance(null, mProv);
            fail("KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyStore.getInstance(invalidValues[i], mProv);
                fail("KeyStoreException must be thrown (type: ".concat(
                        invalidValues[i]).concat(")"));
            } catch (KeyStoreException e) {
            }
        }
        Provider prov = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyStore.getInstance(validValues[i], prov);
                fail("IllegalArgumentException must be thrown when provider is null (type: "
                        .concat(invalidValues[i]).concat(")"));
            } catch (IllegalArgumentException e) {
            }
        }
        KeyStore keyS;
        for (int i = 0; i < validValues.length; i++) {
            keyS = KeyStore.getInstance(validValues[i], mProv);
            assertEquals("Incorrect type", keyS.getType(), validValues[i]);
            assertEquals("Incorrect provider", keyS.getProvider(), mProv);
            checkKeyStoreException(keyS);
            checkResult(keyS);
        }
    }

}
