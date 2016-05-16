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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;

import org.apache.harmony.security.tests.support.KeyStoreTestSupport;
import org.apache.harmony.security.tests.support.SpiEngUtils;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyStore</code> constructor and methods
 * 
 */

public class KeyStore_Impl3Test extends TestCase {

    private static final String KeyStoreProviderClass = "org.apache.harmony.security.tests.support.MyKeyStore";

    private static final String defaultType = "KeyStore";

    public static boolean KSSupported = false;

    public static String defaultProviderName = null;

    public static Provider defaultProvider = null;

    private static String NotSupportMsg = "Default KeyStore type is not supported";

    Provider mProv;

    public KeyStore[] createKS() throws Exception {
        assertTrue(NotSupportMsg, KSSupported);
        KeyStore[] kpg = new KeyStore[3];

        kpg[0] = KeyStore.getInstance(defaultType);
        kpg[1] = KeyStore.getInstance(defaultType, defaultProvider);
        kpg[2] = KeyStore.getInstance(defaultType, defaultProviderName);
        return kpg;
    }

    protected void setUp() throws Exception {
        super.setUp();
        mProv = (new SpiEngUtils()).new MyProvider("MyKSProvider",
                "Testing provider", KeyStoreTestSupport.srvKeyStore.concat(".")
                        .concat(defaultType), KeyStoreProviderClass);
        Security.insertProviderAt(mProv, 2);
        defaultProvider = SpiEngUtils.isSupport(defaultType,
                KeyStoreTestSupport.srvKeyStore);
        KSSupported = (defaultProvider != null);
        defaultProviderName = (KSSupported ? defaultProvider.getName() : null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        Security.removeProvider(mProv.getName());
    }

    /**
     * Test for <code>load(InputStream stream, char[] password)</code> 
     * <code>store(InputStream stream, char[] password)</code> 
     * <code>size()</code>
     * <code>getCreationDate(String alias)</code>
     * methods 
     * Assertions: store(...) throws NullPointerException when stream or
     * password is null; 
     * getCreationDate(...) throws NullPointerException when alias is null;
     * stores KeyStore and then loads it;
     * @throws Exception 
     */

    public void testLoadStore01() throws Exception {
        assertTrue(NotSupportMsg, KSSupported);

        String tType = "TestType";
        KeyStore.TrustedCertificateEntry tCert = new KeyStore.TrustedCertificateEntry(
                new KeyStoreTestSupport.MCertificate("type", new byte[0]));

        Certificate certs[] = {
                new KeyStoreTestSupport.MCertificate(tType, new byte[10]),
                new KeyStoreTestSupport.MCertificate(tType, new byte[20]) };
        PrivateKey pk = new KeyStoreTestSupport.MyPrivateKey(tType, "", new byte[10]);
        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(pk, certs);
        char[] pwd = { 'p', 'a', 's', 's', 'w', 'd' };
        KeyStore.PasswordProtection pPath = new KeyStore.PasswordProtection(pwd);
        String[] aliases = { "Alias1", "Alias2", "Alias3", "Alias4", "Alias5" };

        KeyStore[] kss = createKS();
        KeyStore[] kss1 = new KeyStore[kss.length];
        assertNotNull("KeyStore objects were not created", kss);

        for (int i = 0; i < kss.length; i++) {
            kss1[i] = kss[i];
            kss[i].load(null, null);
            kss[i].setEntry(aliases[0], tCert, null);
            kss[i].setEntry(aliases[1], pKey, pPath);
            kss[i].setEntry(aliases[2], pKey, pPath);
            try {
                kss[i].getCreationDate(null);
                fail("NullPointerException should be thrown when alias is null");
            } catch (NullPointerException e) {
            }

            kss[i].setKeyEntry(aliases[3], pk, pwd, certs);
            kss[i].setCertificateEntry(aliases[4], certs[0]);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                kss[i].store(null, pwd);
                fail("IOException or NullPointerException should be thrown when stream is null");
            } catch (IOException e) {
            } catch (NullPointerException e) {
            }

            //RI does not throw exception while password is null.
            kss[i].store(bos, null);

            kss[i].store(bos, pwd);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos
                    .toByteArray());
            kss1[i].load(bis, pwd);
            assertEquals("Incorrect size", kss1[i].size(), kss[i].size());
            KeyStore.Entry en, en1;
            for (int j = 0; j < 3; j++) {
                en = kss[i].getEntry(aliases[j], (j == 0 ? null : pPath));
                en1 = kss1[i].getEntry(aliases[j], (j == 0 ? null : pPath));
                if (en instanceof KeyStore.TrustedCertificateEntry) {
                    assertTrue("Incorrect entry 1",
                            en1 instanceof KeyStore.TrustedCertificateEntry);
                    assertEquals("Incorrect Certificate",
                            ((KeyStore.TrustedCertificateEntry) en)
                                    .getTrustedCertificate(),
                            ((KeyStore.TrustedCertificateEntry) en1)
                                    .getTrustedCertificate());
                } else {
                    if (en instanceof KeyStore.PrivateKeyEntry) {
                        assertTrue("Incorrect entry 2",
                                en1 instanceof KeyStore.PrivateKeyEntry);
                        assertEquals(
                                "Incorrect Certificate",
                                ((KeyStore.PrivateKeyEntry) en).getPrivateKey(),
                                ((KeyStore.PrivateKeyEntry) en1)
                                        .getPrivateKey());
                    } else {
                        if (en instanceof KeyStore.SecretKeyEntry) {
                            assertTrue("Incorrect entry 3",
                                    en1 instanceof KeyStore.SecretKeyEntry);
                            assertEquals("Incorrect Certificate",
                                    ((KeyStore.SecretKeyEntry) en)
                                            .getSecretKey(),
                                    ((KeyStore.SecretKeyEntry) en1)
                                            .getSecretKey());
                        }
                    }
                }

                assertEquals("Incorrect date", kss[i]
                        .getCreationDate(aliases[j]), kss1[i]
                        .getCreationDate(aliases[j]));
            }
            assertEquals("Incorrect entry", kss[i].getKey(aliases[3], pwd),
                    kss1[i].getKey(aliases[3], pwd));
            assertEquals("Incorrect date", kss[i].getCreationDate(aliases[3]),
                    kss1[i].getCreationDate(aliases[3]));
            assertEquals("Incorrect entry", kss[i].getCertificate(aliases[4]),
                    kss1[i].getCertificate(aliases[4]));
            assertEquals("Incorrect date", kss[i].getCreationDate(aliases[4]),
                    kss1[i].getCreationDate(aliases[4]));
        }
    }
}
