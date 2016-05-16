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
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Arrays;

import org.apache.harmony.security.tests.support.KeyStoreTestSupport;
import org.apache.harmony.security.tests.support.MyKeyStoreSpi;
import org.apache.harmony.security.tests.support.MyLoadStoreParams;
import org.apache.harmony.security.tests.support.SpiEngUtils;
import org.apache.harmony.security.tests.support.TestKeyPair;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyStore</code> constructor and methods
 * 
 */

public class KeyStore_Impl1Test extends TestCase {
    
    public static final String srvKeyStore = KeyStoreTestSupport.srvKeyStore;
    public static String[] validValues =  KeyStoreTestSupport.validValues;

    private static final String[] aliases = { "", "alias", "Alias", "ALIAS",
            "new alias", "another alias", "ADDITIONAL", "THE SAME ALIAS" };

    private static String[] invalidValues =  SpiEngUtils.invalidValues;

    public static String defaultType = KeyStoreTestSupport.defaultType;
    public static boolean JKSSupported = KeyStoreTestSupport.JKSSupported;
    public static String defaultProviderName = KeyStoreTestSupport.defaultProviderName;
    public static Provider defaultProvider = KeyStoreTestSupport.defaultProvider;
       
    private static String NotSupportMsg = "Default KeyStore type is not supported";
       
    public KeyStore[] createKS() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStore[] kpg = new KeyStore[3];

        kpg[0] = KeyStore.getInstance(defaultType);
        kpg[1] = KeyStore.getInstance(defaultType, defaultProvider);
        kpg[2] = KeyStore.getInstance(defaultType, defaultProviderName);
        return kpg;
    }

    /**
     * Test for <code>getInstance(String type)</code> method 
     * Assertion:
     * returns KeyStoreException object
     */
    public void testKeyStore03() throws KeyStoreException {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStore ks;
        for (int i = 0; i < validValues.length; i++) {
            ks = KeyStore.getInstance(validValues[i]);
            assertEquals("Incorrect type", ks.getType(), validValues[i]);
        }
    }

    /**
     * Test for <code>getInstance(String type, String provider)</code> method
     * Assertion: throws IllegalArgumentException when provider is null or empty
     */
    public void testKeyStore04() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        String provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyStore.getInstance(validValues[i], provider);
                fail("IllegalArgumentException must be thrown when provider is null (type: "
                        .concat(validValues[i]).concat(" )"));
            } catch (IllegalArgumentException e) {
            }
            try {
                KeyStore.getInstance(validValues[i], "");
                fail("IllegalArgumentException must be thrown when provider is empty (type: "
                        .concat(validValues[i]).concat(" )"));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String type, String provider)</code> method
     * Assertion: throws NoSuchProviderException when provider is not available
     */
    public void testKeyStore05() throws KeyStoreException {
        assertTrue(NotSupportMsg, JKSSupported);
        for (int i = 0; i < validValues.length; i++) {
            for (int j = 1; j < invalidValues.length; j++) {
                try {
                    KeyStore.getInstance(validValues[i], invalidValues[j]);
                    fail("NoSuchProviderException must be thrown (type: "
                            .concat(validValues[i]).concat("  provider: ")
                            .concat(invalidValues[j]).concat(" )"));
                } catch (NoSuchProviderException e) {
                }
            }
        }
    }

    /**
     * Test for <code>getInstance(String type, String provider)</code> method
     * Assertion:
     * throws NullPointerException when type is null 
     * throws KeyStoreException when type is not available
     */
    public void testKeyStore06() throws NoSuchProviderException {
        assertTrue(NotSupportMsg, JKSSupported);
        try {
            KeyStore.getInstance(null, defaultProviderName);
            fail("KeyStoreException must be thrown  when type is null");
        } catch (KeyStoreException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyStore.getInstance(invalidValues[i], defaultProviderName);
                fail("KeyStoreException must be thrown (type: ".concat(
                        invalidValues[i]).concat("  provider: ").concat(
                        defaultProviderName).concat(" )"));
            } catch (KeyStoreException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String type, String provider)</code> method
     * Assertion: returns KeyStore object
     */
    public void testKeyStore07() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStore ks;
        for (int i = 0; i < validValues.length; i++) {
            ks = KeyStore.getInstance(validValues[i], defaultProviderName);
            assertEquals("Incorrect type", ks.getType(), validValues[i]);
            assertEquals("Incorrect provider", ks.getProvider().getName(),
                    defaultProviderName);
        }
    }

    /**
     * Test for <code>getInstance(String type, Provider provider)</code> method
     * Assertion: throws IllegalArgumentException when provider is null 
     */
    public void testKeyStore08() throws KeyStoreException {
        assertTrue(NotSupportMsg, JKSSupported);
        Provider provider = null;
        for (int i = 0; i < validValues.length; i++) {
            try {
                KeyStore.getInstance(validValues[i], provider);
                fail("IllegalArgumentException must be thrown when provider is null (type: "
                        .concat(validValues[i]).concat(" )"));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String type, Provider provider)</code>
     * method 
     * Assertions: 
     * throws NullPointerException when type is null 
     * throws KeyStoreException when type is not available
     */
    public void testKeyStore09() {
        assertTrue(NotSupportMsg, JKSSupported);
        try {
            KeyStore.getInstance(null, defaultProvider);
            fail("KeyStoreException must be thrown when type is null");
        } catch (KeyStoreException e) {
        } catch (NullPointerException e) {
        }
        for (int i = 0; i < invalidValues.length; i++) {
            try {
                KeyStore.getInstance(invalidValues[i], defaultProvider);
                fail("KeyStoreException must be thrown when type is null (type: "
                        .concat(invalidValues[i]).concat(" provider: ").concat(
                                defaultProvider.getName()).concat(" )"));
            } catch (KeyStoreException e) {
            }
        }
    }

    /**
     * Test for <code>getInstance(String type, Provider provider)</code>
     * method 
     * Assertion: returns KeyStore object
     */
    public void testKeyStore10() throws KeyStoreException {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStore ks;
        for (int i = 0; i < validValues.length; i++) {
            ks = KeyStore.getInstance(validValues[i], defaultProvider);
            assertEquals("Incorrect type", ks.getType(), validValues[i]);
            assertEquals("Incorrect provider", ks.getProvider(),
                    defaultProvider);
        }
    }

    /**
     * Test for methods:
     * <code>getKey(String alias, char[] password)</code>
     * <code>getCertificateChain(String alias)</code>
     * <code>getCertificate(String alias)</code>
     * <code>getCreationDate(String alias)</code>
     * <code>setKeyEntry(String alias, Key key, char[] password, Certificate[] chain)</code>
     * <code>setKeyEntry(String alias, byte[] key, Certificate[] chain)</code>
     * <code>setCertificateEntry(String alias, Certificate cert)</code>
     * <code>deleteEntry(String alias)</code>
     * <code>Enumeration aliases()</code>
     * <code>containsAlias(String alias)</code>
     * <code>size()</code>
     * <code>isKeyEntry(String alias)</code>
     * <code>isCertificateEntry(String alias)</code>
     * <code>getCertificateAlias(Certificate cert)</code>
     * <code>store(OutputStream stream, char[] password)</code>
     * Assertion: throws KeyStoreException when KeyStore was not initialized
     */
    public void testKeyStore11() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        String msgF ="KeyStoreException must be thrown because KeyStore was not initialized";
        KeyStore [] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);
        for (int i = 0; i < kss.length; i++) {
            try {
                kss[i].getKey("", new char[1]);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].getCertificateChain("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].getCertificate("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].getCreationDate("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].aliases();
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].containsAlias("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].size();
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].setKeyEntry("", null, new char[0], new Certificate[0]);
                fail(msgF);
            } catch (KeyStoreException e) {
            }     
            try {
                kss[i].setKeyEntry("", new byte[0], new Certificate[0]);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].setCertificateEntry("", null);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].deleteEntry("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].isKeyEntry("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].isCertificateEntry("");
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].getCertificateAlias(null);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            try {
                kss[i].store(ba, new char[0]);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].store(new MyLoadStoreParams(
                        new KeyStore.PasswordProtection(new char[0])));
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            KeyStore.TrustedCertificateEntry entry = new KeyStore.TrustedCertificateEntry(
                    new KeyStoreTestSupport.MCertificate("type", new byte[0]));
            try {
                kss[i].setEntry("aaa", entry, null);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].getEntry("aaa", null);
                fail(msgF);
            } catch (KeyStoreException e) {
            }
        }
    }
    
    /**
     * Test for
     * <code>setEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter params)</code>
     * <code>containsAlias(String alias)</code>
     * <code>getEntry(String alias)</code> 
     * <code>getCertificate(String alias)</code> 
     * <code>isCertificateEntry(String alias)</code> 
     * <code>isKeyEntry(String alias)</code>
     * methods Assertions: setEntry(..) throws NullPointerException when alias
     * or entry is null;
     * 
     * containsAlias(..), getEntry(..), isCertificateEntry(..), isKeyEntry(...)
     * throw NullPointerException when alias is null;
     * 
     * setEntry(..) stores Entry and getEntry(..) returns it when
     * KeyStore.TrustedCertificateEntry is used; getCertificate(...) returns
     * used trusted certificate.
     *  
     */
    public void testEntry01() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStoreTestSupport.MCertificate trust = new KeyStoreTestSupport.MCertificate(
                "type", new byte[0]);
        KeyStore.TrustedCertificateEntry entry = new KeyStore.TrustedCertificateEntry(
                trust);
        KeyStore[] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);

        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            try {
                kss[i].setEntry(null, entry, null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].setEntry("ZZZ", null, null);
                fail("NullPointerException should be thrown when entry is null");
            } catch (NullPointerException e) {
            }
            for (int j = 0; j < aliases.length; j++) {
                kss[i].setEntry(aliases[j], entry, null);
            }
        }
        for (int i = 0; i < kss.length; i++) {
            try {
                kss[i].containsAlias(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].isCertificateEntry(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].isKeyEntry(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].getEntry(null, null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            KeyStore.Entry en;
            for (int j = 0; j < aliases.length; j++) {
                assertFalse("Incorrect alias", kss[i].containsAlias("Bad"
                        .concat(aliases[j])));
                assertTrue("Incorrect alias", kss[i].containsAlias(aliases[j]));
                assertTrue("Not CertificateEntry", kss[i]
                        .isCertificateEntry(aliases[j]));
                assertFalse("Incorrect KeyEntry", kss[i].isKeyEntry(aliases[j]));
                en = kss[i].getEntry(aliases[j], null);
                assertTrue("Incorrect Entry",
                        en instanceof KeyStore.TrustedCertificateEntry);
                assertEquals("Incorrect certificate",
                        ((KeyStore.TrustedCertificateEntry) en)
                                .getTrustedCertificate(), entry
                                .getTrustedCertificate());
                assertEquals("Incorrect certificate", kss[i]
                        .getCertificate(aliases[j]), trust);
            }
        }
    }


    /**
     * Test for 
     * <code>setEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter params)</code>
     * <code>containsAlias(String alias)</code>
     * <code>getEntry(String alias)</code> 
     * <code>isCertificateEntry(String alias)</code> 
     * <code>isKeyEntry(String alias)</code> 
     * methods
     * Assertions: 
     * getEntry(...) throws KeyStoreException if password is incorrect;
     * setEntry(..) throws KeyStoreException if password is destroyed;
     * 
     * setEntry(..) throws KeyStoreException when incorrect Entry is used;
     * 
     * setEntry(..) stores Entry and getEntry(...) returns it when 
     * KeyStore.PrivateKeyEntry is used.
     * 
     */    
    public void testEntry02() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        TestKeyPair tkp = new TestKeyPair("DSA");
        KeyStoreTestSupport.MCertificate certs[] = {
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()),
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()) };
        PrivateKey privKey = tkp.getPrivate();
        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(privKey,
                certs);
        char[] pwd = { 'p', 'a', 's', 's', 'w', 'd' };
        KeyStore.PasswordProtection pPath = new KeyStore.PasswordProtection(pwd);
        KeyStore.PasswordProtection anotherPath = new KeyStore.PasswordProtection(
                new char[0]);
        KeyStoreTestSupport.ProtPar pPar = new KeyStoreTestSupport.ProtPar();
        KeyStore[] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);
        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            for (int j = 0; j < aliases.length; j++) {
                kss[i].setEntry(aliases[j], pKey, pPath);
            }
            KeyStore.Entry en;
            Certificate[] cc;
            for (int j = 0; j < aliases.length; j++) {
                assertTrue("Incorrect alias", kss[i].containsAlias(aliases[j]));
                assertTrue("Not KeyEntry", kss[i].isKeyEntry(aliases[j]));
                assertFalse("Incorrect CertificateEntry", kss[i]
                        .isCertificateEntry(aliases[j]));

                en = kss[i].getEntry(aliases[j], pPath);
                assertTrue("Incorrect Entry",
                        en instanceof KeyStore.PrivateKeyEntry);
                Key key = pKey.getPrivateKey();
                Key key1  = ((KeyStore.PrivateKeyEntry) en).getPrivateKey();
                if (!key.getAlgorithm().equals(key1.getAlgorithm()) ||
                		!key.getFormat().equals(key1.getFormat())) {
                	fail("Incorrect key");
                }
                byte[] enc = key.getEncoded();
                byte[] enc1 = key1.getEncoded();
                assertTrue("Diff. keys encoding", Arrays.equals(enc, enc1));
              
                cc = ((KeyStore.PrivateKeyEntry) en).getCertificateChain();
                assertEquals("Incorrect CertificateChain", cc.length,
                        certs.length);
                for (int t = 0; t < cc.length; t++) {
                    assertEquals("Incorrect CertificateChain", cc[t], certs[t]);
                }

                key = kss[i].getKey(aliases[j], pwd);
                key1  = privKey;
                if (!key.getAlgorithm().equals(key1.getAlgorithm()) ||
                		!key.getFormat().equals(key1.getFormat())) {
                	fail("Incorrect Entry: key");
                }
                enc = key.getEncoded();
                enc1 = key1.getEncoded();
                assertTrue("Incorrect Entry: Diff. keys encoding", Arrays.equals(enc, enc1));
                  
                cc = kss[i].getCertificateChain(aliases[j]);
                assertEquals("Incorrect CertificateChain", cc.length,
                        certs.length);
                for (int t = 0; t < cc.length; t++) {
                    assertEquals("Incorrect CertificateChain", cc[t], certs[t]);
                }
                try {
                    kss[i].getEntry(aliases[j], anotherPath);
                    fail("KeyStoreException or UnrecoverableEntryException should be thrown "
                            + "because password is incorrect");
                } catch (KeyStoreException e) {
                } catch (UnrecoverableEntryException e) {
                }
            }
        }
        pPath.destroy();
        for (int i = 0; i < kss.length; i++) {
            try {
                kss[i].setEntry("ZZZ", pKey, pPath);
                fail("KeyStoreException should be thrown because password is destroyed");
            } catch (KeyStoreException e) {
            }

            for (int j = 0; j < aliases.length; j++) {
                try {
                    kss[i].getEntry(aliases[j], pPath);
                    fail("KeyStoreException should be thrown because password is destroyed");
                } catch (KeyStoreException e) {
                }

                try {
                    kss[i].getEntry(aliases[j], pPar);
                    fail("UnrecoverableEntryException should be thrown");
                } catch (UnrecoverableEntryException e) {
                }
            }
        }
    }
    
    /**
     * Test for 
     * <code>setEntry(String alias, KeyStore.Entry entry, KeyStore.ProtectionParameter params)</code>
     * <code>containsAlias(String alias)</code>
     * <code>getEntry(String alias)</code> 
     * <code>isCertificateEntry(String alias)</code> 
     * <code>isKeyEntry(String alias)</code> 
     * methods
     * Assertions: 
     * setEntry(..) stores used entry and getEntry(..) returns it when 
     * KeyStore.SecretKeyEntry is used;
     * 
     * setEntry(..) throws KeyStoreException when incorrect Entry is used.
     * 
     * FIXME: this test should be changed to verify SecretKeyEntry.
     * It is not supported.  
     */    
    public void testEntry03() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        TestKeyPair tkp = new TestKeyPair("DSA");
        KeyStoreTestSupport.SKey secKey = new KeyStoreTestSupport.SKey("DSA",
                tkp.getPrivate().getEncoded());
        KeyStore.SecretKeyEntry sKey = new KeyStore.SecretKeyEntry(
                secKey);        
        char [] pwd = {'p', 'a', 's', 's', 'w', 'd'};
        KeyStore.PasswordProtection pPath = new KeyStore.PasswordProtection(pwd);
        KeyStoreTestSupport.AnotherEntry aEntry = new KeyStoreTestSupport.AnotherEntry();
        KeyStoreTestSupport.ProtPar pPar = new KeyStoreTestSupport.ProtPar();
        KeyStore [] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);
        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            for (int j = 0; j < aliases.length; j++)  {
                try {
                    kss[i].setEntry(aliases[j], sKey, pPath);
                } catch (KeyStoreException e) {
                    //logln("testEntry03: non-PrivateKeys not supported.");
                    return;
                }
            }

            for (int j = 0; j < aliases.length; j++)  {            
                assertTrue("Incorrect alias", kss[i].containsAlias(aliases[j]));
                assertTrue("Not KeyEntry", kss[i].isKeyEntry(aliases[j]));                
                assertFalse("Incorrect CertificateEntry", kss[i].isCertificateEntry(aliases[j]));
                Key key1;
                try {
                	key1 = kss[i].getKey(aliases[j], pwd);
                } catch (UnrecoverableKeyException e) { 
                	//logln("testEntry03: non-PrivateKeys not supported.");
                	return;
                }
                if (!secKey.getAlgorithm().equals(key1.getAlgorithm()) ||
                		!secKey.getFormat().equals(key1.getFormat())) {
                	fail("Incorrect key");
                }
                byte[] enc = secKey.getEncoded();
                byte[] enc1 = key1.getEncoded();
                assertTrue("Diff. keys encoding", Arrays.equals(enc, enc1));
                assertNull("Incorrect CertificateChain", kss[i].getCertificateChain(aliases[j]));
            }
        }
        pPath.destroy();
        for (int i = 0; i < kss.length; i++) {        
            try {
                kss[i].setEntry("ZZZ", aEntry, pPath);
                fail("KeyStoreException should be thrown because password is destroyed");
            } catch (KeyStoreException e) {                
            }
            for (int j = 0; j < aliases.length; j++)  {  
                try {
                    kss[i].getEntry(aliases[j], pPath);                 
                    fail("KeyStoreException should be thrown because password is destroyed");
                } catch (KeyStoreException e) {                
                }
                try {
                    kss[i].getEntry(aliases[j], pPar);                 
                    fail("UnrecoverableEntryException should be thrown");
                } catch (UnrecoverableEntryException e) {                
                }
            }
        }
    }

    
    /**
     * Test for 
     * <code>setCertificateEntry(String alias, Certificate cert)</code>
     * <code>containsAlias(String alias)</code>
     * <code>getCertificate(String alias)</code>
     * <code>isCertificateEntry(String alias)</code> 
     * methods
     * Assertions: 
     * setCertificateEntry(..), containsAlias(..), getCertificate(..) and isCertificateEntry(..)
     * throw NullPointerException when alias is null
     * 
     * setCertificateEntry(..) stores used entry and getCertificate(..) returns it 
     * 
     */
    public void testEntry04() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStoreTestSupport.MCertificate cert = new KeyStoreTestSupport.MCertificate(
                "type", new byte[0]);
        KeyStore [] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);
        
        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            try {
                kss[i].setCertificateEntry(null, cert);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            for (int j = 0; j < aliases.length; j++)  {
                kss[i].setCertificateEntry(aliases[j], cert);
            }
        }
        for (int i = 0; i < kss.length; i++) {
            try {
                kss[i].containsAlias(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].isCertificateEntry(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].getCertificate(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            for (int j = 0; j < aliases.length; j++)  {            
                assertFalse("Incorrect alias", kss[i].containsAlias("Bad".concat(aliases[j])));
                assertTrue("Incorrect alias", kss[i].containsAlias(aliases[j]));
                assertTrue("Not CertificateEntry", kss[i].isCertificateEntry(aliases[j]));
                assertFalse("Incorrect KeyEntry", kss[i].isKeyEntry(aliases[j]));                
                assertEquals("Incorrect Certificate", kss[i].getCertificate(aliases[j]),
                        cert);
            }
        }    
    }
    
    /**
     * Test for 
     * <code>setKeyEntry(String alias, Key key, char[] password, Certificate[] chain)</code>
     * <code>containsAlias(String alias)</code>
     * <code>getKey(String alias, char[] password)</code>
     * <code>isCertificateEntry(String alias)</code> 
     * <code>isKeyEntry(String alias)</code>
     * <code>setCerificateEntry(String alias, Certificate cert)</code>
     * <code>getCertificateChain(String alias)</code>
     * <code>getCertificateAlias(Certificate cert)</code>
     * methods
     * 
     * Assertions: 
     * setKeyEntry(..), getKeyEntry(..) and isKeyEntry(..)
     * throw NullPointerException when alias is null
     * 
     * setKeyEntry(...) throws KeyStoreException when key or password
     * is null
     * 
     * setCertificateEntry(..) throws KeyStoreException when KeyEntry was overwritten
     * 
     * setKeyEntry(..) stores used entry, getKey(..) returns it and getCertificateChain(...)
     * returns cert 
     * 
     */
    public void testEntry05() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStoreTestSupport.MCertificate certs[] = {
                new KeyStoreTestSupport.MCertificate("type1", new byte[10]),
                new KeyStoreTestSupport.MCertificate("type2", new byte[10]) };
        KeyStoreTestSupport.MCertificate cert = new KeyStoreTestSupport.MCertificate(
                "type", new byte[0]);
        char[] pwd = new char[0];
        TestKeyPair tkp = new TestKeyPair("DSA");
        PrivateKey key = tkp.getPrivate();
        KeyStore [] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);
        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            
            // Null as alias does not necessarily lead to NullPointerException
            
            try {
                kss[i].setKeyEntry("ZZZ", null, pwd, certs);
                fail("KeyStoreException should be thrown when key is null");
            } catch (KeyStoreException e) {
            }
            try {
                kss[i].setKeyEntry("ZZZ", key, pwd, null);
                fail("KeyStoreException or IllegalArgumentException should be thrown "
                        + "when chain is null and key is private");
            } catch (IllegalArgumentException e) {
            }
            try {
                kss[i].setKeyEntry("ZZZ", key, pwd,
                        new KeyStoreTestSupport.MCertificate[0]);
                fail("KeyStoreException or IllegalArgumentException should be thrown "
                        + "when chain is empty and key is private");
            } catch (IllegalArgumentException e) {
            }

            for (int j = 0; j < aliases.length; j++)  {
                kss[i].setKeyEntry(aliases[j], key, pwd, certs);
            }
            
            kss[i].setKeyEntry("KeyAlias", key, pwd, certs);
            try {
                kss[i].setCertificateEntry("KeyAlias", cert);
                fail("KeyStoreException should be thrown when we try to overwrite KeyEntry to Certificate");
            } catch (KeyStoreException e) {
            }
            
            try {
                kss[i].isKeyEntry(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].getKey(null, pwd);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            try {
                kss[i].getCertificateChain(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            for (int j = 0; j < aliases.length; j++)  {            
                assertFalse("Incorrect alias", kss[i].containsAlias("Bad".concat(aliases[j])));
                assertTrue("Incorrect alias", kss[i].containsAlias(aliases[j]));
                assertTrue("Not KeyEntry", kss[i].isKeyEntry(aliases[j]));
                assertFalse("Incorrect CertificateEntry", kss[i].isCertificateEntry(aliases[j]));
                Key key1  = kss[i].getKey(aliases[j], pwd);
                if (!key.getAlgorithm().equals(key1.getAlgorithm()) ||
                		!key.getFormat().equals(key1.getFormat())) {
                	fail("Incorrect key");
                }
                byte[] enc = key.getEncoded();
                byte[] enc1 = key1.getEncoded();
                assertTrue("Diff. keys encoding", Arrays.equals(enc, enc1));
                Certificate [] cc = kss[i].getCertificateChain(aliases[j]);
                assertEquals("Incorrect chain", cc.length, certs.length);
                for (int t = 0; t < cc.length; t++) {
                    assertEquals("Incorrect certificate", cc[t], certs[t]);                    
                }
            }
            assertNull(kss[i].getCertificateAlias(cert));
            String ss = kss[i].getCertificateAlias(certs[0]);
            boolean ans = false;
            for (int j = 1; j < aliases.length; j++)  {
                if (ss.equals(aliases[j])) {
                    ans = true;
                    break;
                }
            }
            assertTrue("There is no alias for certificate <type1, new byte[10]>", ans);
        }       
    }
    
    /**
     * Test for 
     * <code>deleteEntry(String alias)</code>
     * <code>size()</code> 
     * methods
     * Assertions: 
     * throws NullPointerException when alias is null;
     * 
     * deletes entry from KeyStore.
     * 
     */    
    public void testEntry06() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStore.TrustedCertificateEntry tCert = new KeyStore.TrustedCertificateEntry(
                new KeyStoreTestSupport.MCertificate("type", new byte[0]));

        TestKeyPair tkp = new TestKeyPair("DSA");
        KeyStoreTestSupport.MCertificate certs[] = {
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()),
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()) };
        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(tkp
                .getPrivate(), certs);
        char[] pwd = { 'p', 'a', 's', 's', 'w', 'd' };
        KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pwd);

        String[] aliases = { "Alias1", "Alias2", "Alias3", "Alias4", "Alias5" };

        KeyStore[] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);

        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            kss[i].setEntry(aliases[0], tCert, null);
            kss[i].setEntry(aliases[1], pKey, pp);
            kss[i].setEntry(aliases[2], pKey, pp);

            kss[i].setKeyEntry(aliases[3], tkp.getPrivate(), pwd, certs);

            kss[i].setCertificateEntry(aliases[4], certs[0]);

            assertEquals("Incorrect size", kss[i].size(), 5);
            try {
                kss[i].deleteEntry(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }
            kss[i].deleteEntry(aliases[0]);
            kss[i].deleteEntry(aliases[3]);
            assertEquals("Incorrect size", kss[i].size(), 3);
            for (int j = 1; j < 5; j++) {
                if ((j == 0) || (j == 3)) {
                    assertFalse("Incorrect deleted alias", kss[i]
                            .containsAlias(aliases[j]));
                } else {
                    assertTrue("Incorrect alias", kss[i]
                            .containsAlias(aliases[j]));
                }
            }
        }
    }
    
    /**
     * Test for 
     * <code>entryInstanceOf(String alias, Class class)</code>
     * method
     * Assertions: 
     * throws NullPointerException when alias is null 
     * returns false if KeyStore does not contain entry with defined alias
     * returns false if defined alias is not correspond Entry
     * returns false  
     * setEntry(..) throws KeyStoreException when incorrect Entry is used;
     * 
     * setEntry(..) stores Entry and getEntry(...) returns it when 
     * KeyStore.PrivateKeyEntry is used.
     * 
     */    
    public void testEntry07() throws Exception {  
        assertTrue(NotSupportMsg, JKSSupported);
        TestKeyPair tkp = new TestKeyPair("DSA");
        KeyStoreTestSupport.MCertificate certs[] = {
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()),
                new KeyStoreTestSupport.MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()) };
        PrivateKey privKey = tkp.getPrivate();
        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(privKey, certs);        
        char [] pwd = {'p', 'a', 's', 's', 'w', 'd'};
        String aliasKE = "KeyAlias";
        KeyStore.PasswordProtection pp = new KeyStore.PasswordProtection(pwd);
        new KeyStore.PasswordProtection(new char[0]);
        KeyStore [] kss = createKS();
        assertNotNull("KeyStore objects were not created", kss);

        for (int i = 0; i < kss.length; i++) {
            kss[i].load(null, null);
            // set entries
            for (int j = 0; j < aliases.length; j++)  {
                kss[i].setEntry(aliases[j], pKey, pp);
            }
            kss[i].setKeyEntry(aliasKE, privKey, pwd, certs);
            try {
                kss[i].entryInstanceOf(null, pKey.getClass());
                fail("NullPointerEXception must be thrown");
            } catch (NullPointerException e) {
            }
            assertFalse("Incorrect class entry 1", 
                    kss[i].entryInstanceOf("ZZZ", pKey.getClass()));
            for (int j = 0; j < aliases.length; j++)  {
                assertTrue("Incorrect class entry 2", 
                        kss[i].entryInstanceOf(aliases[j], pKey.getClass()));

                //make it compilable on 1.5
                Class c = privKey.getClass();
                assertFalse("Incorrect class entry 3", 
                        kss[i].entryInstanceOf(aliases[j], c ));
            }

            //make it compilable on 1.5
            Class c = privKey.getClass();
            assertFalse("Incorrect class entry 4", 
                kss[i].entryInstanceOf(aliasKE, c ));
            assertTrue("Incorrect class entry 5", 
                kss[i].entryInstanceOf(aliasKE, pKey.getClass()));            
        }
    }

    
    /**
     * Test for <code>KeyStore(KeyStoreSpi spi, Provider prov, String type)</code>
     * constructor
     * Assertion: constructs KeyStore object
     */
    public void testKeyStoreConstr() throws Exception {
        assertTrue(NotSupportMsg, JKSSupported);
        KeyStoreSpi spi = new MyKeyStoreSpi();
        KeyStore keySt = new tmpKeyStore(spi, defaultProvider,
                defaultType);
        assertEquals("Incorrect name", keySt.getType(),
                defaultType);
        assertEquals("Incorrect provider", keySt.getProvider(), defaultProvider);
        char [] pwd = new char[0];
        try {
            keySt.store(null, pwd);
            fail("KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        }
        keySt = new tmpKeyStore(null, null, null);
        assertNull("Algorithm must be null", keySt.getType());
        assertNull("Provider must be null", keySt.getProvider());
        try {
            keySt.load(null, pwd);
            fail("NullPointerException must be thrown");
        } catch (NullPointerException e) {
        }
    }

}

/**
 * Additional class to verify KeyStore constructor
 */
class tmpKeyStore extends KeyStore {
    public tmpKeyStore(KeyStoreSpi spi, Provider prov, String alg) {
        super(spi, prov, alg);
    }
}
