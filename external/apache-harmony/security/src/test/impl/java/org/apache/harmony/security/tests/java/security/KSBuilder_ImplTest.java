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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Enumeration;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.KeyStoreTestSupport;
import org.apache.harmony.security.tests.support.SpiEngUtils;
import org.apache.harmony.security.tests.support.TestKeyPair;
import org.apache.harmony.security.tests.support.tmpCallbackHandler;

/**
 * Tests for <code>KeyStore.Builder</code> class
 */
public class KSBuilder_ImplTest extends TestCase {

    private static char[] pass =  {'s','t','o','r','e','p','w','d'};
    
    private KeyStore.PasswordProtection protPass = new KeyStore.PasswordProtection(pass);
    private tmpCallbackHandler tmpCall = new tmpCallbackHandler();
    private KeyStore.CallbackHandlerProtection callbackHand = new KeyStore.CallbackHandlerProtection(tmpCall);
    private myProtectionParameter myProtParam = new myProtectionParameter(new byte[5]);  
    public static String[] validValues = KeyStoreTestSupport.validValues;

    private static String defaultType = KeyStoreTestSupport.defaultType;

    private static boolean JKSSupported = false;

    private static Provider defaultProvider = null;
    
    static {
        defaultProvider = SpiEngUtils.isSupport(
                KeyStoreTestSupport.defaultType, KeyStoreTestSupport.srvKeyStore);
        JKSSupported = (defaultProvider != null);
    }

    // Creates empty KeyStore and loads it to file    
    private File createKS() throws Exception {
        FileOutputStream fos = null;
        File ff = File.createTempFile("KSBuilder_ImplTest", "keystore");
        ff.deleteOnExit();
        try {

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            fos = new FileOutputStream(ff);
            ks.load(null, null);
            ks.store(fos, pass);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
        return ff;
    }

    /*
     * Test for method:
     * <code>newInstance(KeyStore keyStore, ProtectionParameter protectionParameter)</code>
     * <code>getKeyStore()</code>
     * <code>getProtectionParameter(String alias)</code>
     * Assertions:
     * throws NullPointerException if keyStore or protectionParameter is null
     * throws IllegalArgumentException if keyStore was not initialized
     * returns new object.
     * 
     * getKeyStore() returns specified keystore;
     * getProtectionParameter(String alias) 
     * throws NullPointerException when alias is null;
     * throws KeyStoreException when alias is not available;
     * returns ProtectionParameter which is used in newInstance(...) 
     * 
     */
    public void testNewInstanceKeyStoreProtectionParameter()
            throws KeyStoreException, NoSuchAlgorithmException, IOException,
            CertificateException, InvalidKeyException, InvalidKeySpecException {
        // exceptions verification
        try {
            KeyStore.Builder.newInstance(null, protPass);
            fail("NullPointerException must be thrown when KeyStore is null");
        } catch (NullPointerException e) {
        }
        if (!JKSSupported) {
            fail(defaultType + " type is not supported");
            return;
        }
        KeyStore.Builder ksB;
        KeyStore ks = KeyStore.getInstance(defaultType);
        try {
            KeyStore.Builder.newInstance(ks, null);
            fail("NullPointerException must be thrown when ProtectionParameter is null");
        } catch (NullPointerException e) {
        }

        KeyStore.PasswordProtection protPass1 = new KeyStore.PasswordProtection(
                pass);
        KeyStore.ProtectionParameter [] pp = { protPass, protPass1,
                callbackHand, myProtParam };
        TestKeyPair tkp = new TestKeyPair("DSA");
        Certificate certs[] = {
                new MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()),
                new MCertificate("DSA", tkp.getPrivate()
                        .getEncoded()) };
        PrivateKey privKey = tkp.getPrivate();

        KeyStore.PrivateKeyEntry pKey = new KeyStore.PrivateKeyEntry(privKey,
                certs);
        for (int i = 0; i < pp.length; i++) {
            ks = KeyStore.getInstance(defaultType);
            try {
                KeyStore.Builder.newInstance(ks, pp[i]);
                fail("IllegalArgumentException must be thrown because KeyStore was not initialized");
            } catch (IllegalArgumentException e) {
            }
            ks.load(null, pass);
            ksB = KeyStore.Builder.newInstance(ks, pp[i]);

            assertEquals("Incorrect KeyStore", ksB.getKeyStore().size(), 0);

            ks.setEntry("aaa", pKey, pp[0]);
            ksB = KeyStore.Builder.newInstance(ks, pp[i]);

            // verification getKeyStore() and getProtectionParameter(String
            // alias)
            assertEquals("Incorrect KeyStore", ks, ksB.getKeyStore());

            try {
                ksB.getProtectionParameter(null);
                fail("NullPointerException must be thrown");
            } catch (NullPointerException e) {
            }
            try {
                assertEquals(ksB.getProtectionParameter("aaa"), pp[i]);
            } catch (KeyStoreException e) {
                fail("Unexpected: " + e.toString() + " was thrown");
            }

            try {
                assertEquals(ksB.getProtectionParameter("Bad alias"), pp[i]);
            } catch (KeyStoreException e) {
                // KeyStoreException might be thrown because there is no entry with such alias
            }

            try {
                assertEquals(ksB.getProtectionParameter(""), pp[i]);
            } catch (KeyStoreException e) {
                // KeyStoreException might be thrown because there is no entry with such alias
            }

            KeyStore.ProtectionParameter pPar = ksB
                    .getProtectionParameter("aaa");

            switch (i) {
            case 0:
                assertTrue(pPar instanceof KeyStore.PasswordProtection);
                break;
            case 1:
                assertTrue(pPar instanceof KeyStore.PasswordProtection);
                break;
            case 2:
                assertTrue(pPar instanceof KeyStore.CallbackHandlerProtection);
                break;
            case 3:
                assertTrue(pPar instanceof myProtectionParameter);
                break;
            default:
                fail("Incorrect protection parameter");
            }
            assertEquals(pPar, pp[i]);
        }
    }
    
    /*
     * Test for methods:
     * <code>newInstance(String type, Provider provider, File file, 
     * ProtectionParameter protectionParameter)</code>
     * <code>getKeyStore()</code>
     * <code>getProtectionParameter(String alias)</code>
     * Assertions:
     * throws NullPointerException if type, file or protectionParameter is null;
     * throws IllegalArgumentException if file does not exist or is not file;
     * throws IllegalArgumentException if ProtectionParameter is not
     * PasswordProtection or CallbackHandlerProtection;
     * returns new object
     * 
     * getKeyStore() returns specified keystore;
     * getProtectionParameter(String alias) 
     * throws NullPointerException when alias is null;
     * throws KeyStoreException when alias is not available;
     * returns ProtectionParameter which is used in newInstance(...) 
     * 
     */
    public void testNewInstanceStringProviderFileProtectionParameter()
            throws Exception {
        if (!JKSSupported) {
            fail(defaultType + " type is not supported");
            return;
        }
        File fl = File.createTempFile("KSBuilder_ImplTest", "keystore");
        fl.deleteOnExit();
        KeyStore.Builder ksB;
        KeyStore.Builder ksB1;
        KeyStore ks = null;
        KeyStore ks1 = null;

        myProtectionParameter myPP = new myProtectionParameter(new byte[5]);
        // check exceptions
        try {

            KeyStore.Builder.newInstance(null, defaultProvider, fl, protPass);
            fail("NullPointerException must be thrown when type is null");
        } catch (NullPointerException e) {
        }
        try {
            KeyStore.Builder.newInstance(defaultType, defaultProvider, null,
                    protPass);
            fail("NullPointerException must be thrown when file is null");
        } catch (NullPointerException e) {
        }
        try {
            KeyStore.Builder
                    .newInstance(defaultType, defaultProvider, fl, null);
            fail("NullPointerException must be thrown when ProtectionParameter is null");
        } catch (NullPointerException e) {
        }
        try {
            KeyStore.Builder
                    .newInstance(defaultType, defaultProvider, fl, myPP);
            fail("IllegalArgumentException must be thrown when ProtectionParameter is not correct");
        } catch (IllegalArgumentException e) {
        }
        try {
            KeyStore.Builder.newInstance(defaultType, defaultProvider,
                    new File(fl.getAbsolutePath().concat("should_absent")),
                    protPass);
            fail("IllegalArgumentException must be thrown when file does not exist");
        } catch (IllegalArgumentException e) {
        }
        try {
            // 'file' param points to directory
            KeyStore.Builder.newInstance(defaultType, defaultProvider,
                    fl.getParentFile(), protPass);
            fail("IllegalArgumentException must be thrown when file does not exist");
        } catch (IllegalArgumentException e) {
        }
        ksB = KeyStore.Builder.newInstance(defaultType, defaultProvider, fl,
                protPass);
        try {
            ksB.getKeyStore();
            fail("KeyStoreException must be throw because file is empty");
        } catch (KeyStoreException e) {
        }

        fl = createKS();
        KeyStore.ProtectionParameter [] pp = { myPP, protPass, callbackHand };
        for (int i = 0; i < pp.length; i++) {
            if (i == 0) {
                try {
                    KeyStore.Builder.newInstance(defaultType, null, fl, pp[i]);
                    fail("IllegalArgumentException must be thrown for incorrect ProtectionParameter");
                } catch (IllegalArgumentException e) {
                }
                try {
                    KeyStore.Builder.newInstance(defaultType, defaultProvider,
                            fl, pp[i]);
                    fail("IllegalArgumentException must be thrown for incorrect ProtectionParameter");
                } catch (IllegalArgumentException e) {
                }
                continue;
            }
            ksB = KeyStore.Builder.newInstance(defaultType, null, fl, pp[i]);
            ksB1 = KeyStore.Builder.newInstance(defaultType, defaultProvider,
                    fl, pp[i]);
            try {
                ks = ksB.getKeyStore();
                if (i == 2) {
                    fail("KeyStoreException must be thrown for incorrect ProtectionParameter");
                } else {
                    assertEquals("Incorrect KeyStore size", ks.size(), 0);
                }
            } catch (KeyStoreException e) {
                if (i == 2) {
                    continue;
                }
                fail("Unexpected KeyException was thrown");
            }
            try {
                ks1 = ksB1.getKeyStore();
                if (i == 2) {
                    fail("KeyStoreException must be thrown for incorrect ProtectionParameter");
                }
            } catch (KeyStoreException e) {
                if (i == 2) {
                    continue;
                }
                fail("Unexpected KeyException was thrown");
            }
            assertEquals("Incorrect KeyStore size", ks.size(), ks1.size());
            Enumeration iter = ks.aliases();
            String aName;

            while (iter.hasMoreElements()) {
                aName = (String) iter.nextElement();
                assertEquals("Incorrect ProtectionParameter", ksB
                        .getProtectionParameter(aName), pp[i]);
            }

            try {
                assertEquals(ksB.getProtectionParameter("Bad alias"), pp[i]);
            } catch (KeyStoreException e) {
                // KeyStoreException might be thrown because there is no entry with such alias
            }

            iter = ks1.aliases();
            while (iter.hasMoreElements()) {
                aName = (String) iter.nextElement();
                assertEquals("Incorrect ProtectionParameter", ksB1
                        .getProtectionParameter(aName), pp[i]);
            }

            try {
                assertEquals(ksB1.getProtectionParameter("Bad alias"), pp[i]);
            } catch (KeyStoreException e) {
                // KeyStoreException might be thrown because there is no entry with such alias
            }
        }
    }
    
    /*
     * Test for method:
     * <code>newInstance(String type, Provider provider,  
     * ProtectionParameter protectionParameter)</code>
     * <code>getKeyStore()</code>
     * <code>getProtectionParameter(String alias)</code>
     * Assertions:
     * throws NullPointerException if type, or protectionParameter is null;
     * returns new object
     * 
     * getKeyStore() returns empty keystore
     * getProtectionParameter(String alias) 
     * throws NullPointerException when alias is null;
     * throws KeyStoreException when alias is not available
     * 
     */
    public void testNewInstanceStringProviderProtectionParameter()
            throws KeyStoreException {
        if (!JKSSupported) {
            fail(defaultType + " type is not supported");
            return;
        }
        try {
            KeyStore.Builder.newInstance(null,
                    defaultProvider, protPass);
            fail("NullPointerException must be thrown when type is null");
        } catch (NullPointerException e) {
        }
        try {
            KeyStore.Builder.newInstance(defaultType,
                    defaultProvider, null);
            fail("NullPointerException must be thrown when ProtectionParameter is null");
        } catch (NullPointerException e) {
        }
        myProtectionParameter myPP = new myProtectionParameter(new byte[5]);
        KeyStore.ProtectionParameter [] pp = { protPass, myPP, callbackHand };
        KeyStore.Builder ksB, ksB1;
        KeyStore ks = null;
        for (int i = 0; i < pp.length; i++) {
            ksB = KeyStore.Builder.newInstance(defaultType, defaultProvider,
                    pp[i]);
            ksB1 = KeyStore.Builder.newInstance(defaultType, null, pp[i]);
            switch (i) {
            case 0:
                try {
                    ks = ksB.getKeyStore();
                    assertNotNull("KeyStore is null", ks);
                    try {
                        assertEquals(ksB.getProtectionParameter("Bad alias"),
                                pp[i]);
                    } catch (KeyStoreException e) {
                        // KeyStoreException might be thrown because there is no entry with such alias
                    }

                    ks = ksB1.getKeyStore();
                    assertNotNull("KeyStore is null", ks);

                    try {
                        assertEquals(ksB1.getProtectionParameter("Bad alias"),
                                pp[i]);
                    } catch (KeyStoreException e) {
                        // KeyStoreException might be thrown because there is no entry with such alias
                    }
                } catch (KeyStoreException e) {
                    try {
                        ks = ksB.getKeyStore();
                    } catch (KeyStoreException e1) {
                        assertEquals("Incorrect exception", e.getMessage(), e1
                                .getMessage());
                    }
                }
                break;
            case 1:
            case 2:
                Exception ex1 = null;
                Exception ex2 = null;
                try {
                    ks = ksB.getKeyStore();
                } catch (KeyStoreException e) {
                    ex1 = e;
                }
                try {
                    ks = ksB.getKeyStore();
                } catch (KeyStoreException e) {
                    ex2 = e;
                }
                assertEquals("Incorrect exception", ex1.getMessage(), ex2
                        .getMessage());


                try {
                    ksB.getProtectionParameter("aaa");
                    fail("IllegalStateException must be thrown because getKeyStore() was not invoked");
                } catch (IllegalStateException e) {
                }

                try {
                    ks = ksB1.getKeyStore();
                } catch (KeyStoreException e) {
                    ex1 = e;
                }
                try {
                    ks = ksB1.getKeyStore();
                } catch (KeyStoreException e) {
                    ex2 = e;
                }
                assertEquals("Incorrect exception", ex1.getMessage(), ex2
                        .getMessage());


                try {
                    ksB1.getProtectionParameter("aaa");
                    fail("IllegalStateException must be thrown because getKeyStore() was not invoked");
                } catch (IllegalStateException e) {
                }
                break;

            }
        }
    }

    /**
     * Additional class for creation Certificate object 
     */
    public class MCertificate extends Certificate {
        private final byte[] encoding;

        private final String type;

        public MCertificate(String type, byte[] encoding) {
            super(type);
            this.encoding = encoding;
            this.type = type;
        }

        public byte[] getEncoded() throws CertificateEncodingException {
            return encoding.clone();
        }

        public void verify(PublicKey key) throws CertificateException,
                NoSuchAlgorithmException, InvalidKeyException,
                NoSuchProviderException, SignatureException {
        }

        public void verify(PublicKey key, String sigProvider)
                throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException, SignatureException {
        }

        public String toString() {
            return "[MCertificate, type: " + getType() + "]";
        }

        public PublicKey getPublicKey() {
            return new PublicKey() {
                public String getAlgorithm() {
                    return type;
                }

                public byte[] getEncoded() {
                    return encoding;
                }

                public String getFormat() {
                    return "test";
                }
            };
        }
    }
}

/**
 * Additional class for creating KeyStoreBuilder
 */
class myProtectionParameter implements KeyStore.ProtectionParameter {
    public myProtectionParameter(byte [] param) {
        if (param == null) {
            throw new NullPointerException("param is null");
        }
    }    
}
