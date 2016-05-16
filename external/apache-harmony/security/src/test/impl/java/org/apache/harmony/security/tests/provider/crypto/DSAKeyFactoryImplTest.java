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


package org.apache.harmony.security.tests.provider.crypto;


import java.math.BigInteger;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyPair;

import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.security.spec.DSAParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import java.util.Arrays;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class DSAKeyFactoryImplTest extends TestCase {


    static KeyFactory kf;

    static String algorithm = "DSA";

    static KeyPairGenerator kpGen;

    static KeyPair keyPair;

    static DSAPublicKey  publicKey;
    static DSAPrivateKey privateKey;

    static DSAParams publicParams;

    static BigInteger publicP;
    static BigInteger publicQ;
    static BigInteger publicG;
    static BigInteger publicY;

    static String publicAlgorithm;
    static String publicFormat;

    static byte[] publicEncoding;

    static KeySpec x509KeySpec;


    static DSAParams privateParams;

    static BigInteger privateP;
    static BigInteger privateQ;
    static BigInteger privateG;
    static BigInteger privateX;

    static String privateAlgorithm;
    static String privateFormat;

    static byte[] privateEncoding;

    static KeySpec pkcs8KeySpec;


    static RSAPrivateKey privateRSAKey;
    static RSAPublicKey publicRSAKey;

    static Key keyPublic;
    static Key keyPrivate;

    private static boolean flag;
    private static Exception reason;

    static {
        try {
            kpGen = KeyPairGenerator.getInstance(algorithm);

            keyPair = kpGen.generateKeyPair();

            publicKey  = (DSAPublicKey)  keyPair.getPublic();
            privateKey = (DSAPrivateKey) keyPair.getPrivate();

            publicParams = publicKey.getParams();

            publicP = publicParams.getP();
            publicQ = publicParams.getQ();
            publicG = publicParams.getG();
            publicY = publicKey.getY();

            publicAlgorithm = publicKey.getAlgorithm();
            publicFormat    = publicKey.getFormat();

            publicEncoding  = publicKey.getEncoded();

            x509KeySpec = (KeySpec) new X509EncodedKeySpec(publicEncoding);


            privateParams = privateKey.getParams();

            privateP = privateParams.getP();
            privateQ = privateParams.getQ();
            privateG = privateParams.getG();
            privateX = privateKey.getX();

            privateAlgorithm = privateKey.getAlgorithm();
            privateFormat    = privateKey.getFormat();

            privateEncoding  = privateKey.getEncoded();

            pkcs8KeySpec = (KeySpec) new PKCS8EncodedKeySpec(privateEncoding);


            privateRSAKey = new RSAPrivateKey () {
                public BigInteger getPrivateExponent() { return BigInteger.ZERO; }
                public BigInteger getModulus()         { return BigInteger.ZERO; }
                public String     getAlgorithm()       { return "RSA"; }
                public byte[]     getEncoded()         { return new byte[] {0}; }
                public String     getFormat()          { return "fff"; }
            };

            publicRSAKey = new RSAPublicKey () {
                public BigInteger getPublicExponent()  { return BigInteger.ZERO; }
                public BigInteger getModulus()         { return BigInteger.ZERO; }
                public String     getAlgorithm()       { return "RSA"; }
                public byte[]     getEncoded()         { return new byte[] {0}; }
                public String     getFormat()          { return "fff"; }
            };

            keyPublic = new Key()  { public String getAlgorithm() { return "DSA"; }
                                     public byte[] getEncoded()   { return publicEncoding; }
                                     public String getFormat()    { return "X.509"; }
                                   };
            keyPrivate = new Key() { public String getAlgorithm() { return "DSA"; }
                                     public byte[] getEncoded()   { return privateEncoding; }
                                     public String getFormat()    { return "PKCS#8"; }
                                   };

            flag = true;
        } catch (Exception e) {
            flag = false;
            reason = e;
        }
    }


    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {

        if (!flag) {
            throw new Exception("some problem in static initializer : " + reason);
        }
        super.setUp();
        kf  = KeyFactory.getInstance(algorithm, "Crypto");
    }



    private void checkPublicIntegers(DSAPublicKey pk) {

        DSAParams params = pk.getParams();

        assertEquals("failure for 'pk.getY().compareTo(publicY)'", 0, pk.getY()
                .compareTo(publicY));

        assertEquals("failure for 'params.getP().compareTo(publicP)'", 0,
                params.getP().compareTo(publicP));
        assertEquals("failure for 'params.getQ().compareTo(publicQ)'", 0,
                params.getQ().compareTo(publicQ));
        assertEquals("failure for 'params.getG().compareTo(publicG)'", 0,
                params.getG().compareTo(publicG));
    }


    private void checkPublicKeys(DSAPublicKey pk) {

        checkPublicIntegers(pk);

        assertEquals(
                "failure for 'pk.getAlgorithm().compareTo(publicAlgorithm)'",
                0, pk.getAlgorithm().compareTo(publicAlgorithm));
        assertEquals("failure for 'pk.getFormat().compareTo(publicFormat)'", 0,
                pk.getFormat().compareTo(publicFormat));

        if ( publicEncoding != null) {
            assertTrue("failure: encodings are not equal",
                       Arrays.equals(pk.getEncoded(), publicEncoding) );
        }
    }


    private void checkPrivateIntegers(DSAPrivateKey pk) {

        DSAParams params = pk.getParams();

        assertEquals("failure for 'pk.getX().compareTo(privateX)'", 0, pk
                .getX().compareTo(privateX));

        assertEquals("failure for 'params.getP().compareTo(privateP)'", 0,
                params.getP().compareTo(privateP));
        assertEquals("failure for 'params.getQ().compareTo(privateQ)'", 0,
                params.getQ().compareTo(privateQ));
        assertEquals("failure for 'params.getG().compareTo(privateG)'", 0,
                params.getG().compareTo(privateG));
    }


    private void checkPrivateKeys(DSAPrivateKey pk) {

        checkPrivateIntegers(pk);

        assertEquals(
                "failure for 'pk.getAlgorithm().compareTo(privateAlgorithm)'",
                0, pk.getAlgorithm().compareTo(privateAlgorithm));
        assertEquals("failure for 'pk.getFormat().compareTo(privateFormat)", 0,
                pk.getFormat().compareTo(privateFormat));

        if ( privateEncoding != null) {
            assertTrue("failure: encodings are not equal",
                       Arrays.equals(pk.getEncoded(), privateEncoding) );
        }
    }


    /**
     * A test against the "generatePublic(KeySpec)" method.
     * The test checks out that the method throws up InvalidKeySpecException
     * if argument is neither "X509EncodedKeySpec" nor "DSAPublicKeySpec"
     */
    public final void testGeneratePublicKeySpec01() throws Exception {
        try {
            kf.generatePublic(pkcs8KeySpec);
            fail("testcase1: no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.generatePublic(null);
            fail("testcase2: no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
    }


    /**
     * A test against the "generatePublic(KeySpec)" method.
     * The test checks out that the method returns DSAPublicKey
     * if argument is "X509EncodedKeySpec"
     */
    public final void testGeneratePublicKeySpec02() throws Exception {

        checkPublicKeys( (DSAPublicKey) kf.generatePublic(x509KeySpec) );
    }


    /**
     * A test against the "generatePublic(KeySpec)" method.
     * The test checks out that the method returns DSAPublicKey
     * if argument is "DSAPublicKeySpec"
     */
    public final void testGeneratePublicKeySpec03() throws Exception {

        KeySpec keySpec = (KeySpec) new DSAPublicKeySpec(publicY, publicP, publicQ, publicG);

        checkPublicKeys( (DSAPublicKey) kf.generatePublic(keySpec) );
    }


    /**
     * A test against the "generatePublic(KeySpec)" method.
     * The test checks out that the method throws up InvalidKeySpecException
     * if KeySpec argument contains incorrect ASN.1 syntax
     */
    public final void testGeneratePublicKeySpec04() throws Exception {

        X509EncodedKeySpec ks;
        DSAPublicKey pubKey;

        final BigInteger y = publicY;
        final BigInteger p = publicP;
        final BigInteger q = publicQ;
        final BigInteger g = publicG;

        final byte enc1[] = new byte[20];
        System.arraycopy(publicEncoding, 0, enc1, 0, 20);
        final byte[] enc2 = enc1;

        pubKey = new DSAPublicKey () {

                  public BigInteger getY() { return y; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "X.509"; }
              };

        ks = kf.getKeySpec(pubKey, X509EncodedKeySpec.class);
        try {
            pubKey = (DSAPublicKey) kf.generatePublic((KeySpec)ks);
            fail("no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
    }


    /**
     * A compatibility with RI test.
     * It checks out that if key encoding in KeySpec has correct ASN1 structure
     * but AlgorithmIdentifier contains value that connot be translated to "DSA"
     * the "generatePublic" method returns DSA public key
     * whose algorithm is neither null nor "DSA".
     */
    public final void testGeneratePublicKeySpec05() throws Exception {

        X509EncodedKeySpec ks;
        DSAPublicKey pubKey;

        final BigInteger y = publicY;
        final BigInteger p = publicP;
        final BigInteger q = publicQ;
        final BigInteger g = publicG;

        final byte enc1[] = new byte[publicEncoding.length];
        System.arraycopy(publicEncoding, 0, enc1, 0, publicEncoding.length);
        enc1[13] = 0;
        final byte[] enc2 = enc1;

        pubKey = new DSAPublicKey () {

                  public BigInteger getY() { return y; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "X.509"; }
              };

        ks = kf.getKeySpec(pubKey, X509EncodedKeySpec.class);
        pubKey = (DSAPublicKey) kf.generatePublic((KeySpec)ks);

        String alg = pubKey.getAlgorithm();
        assertNotNull(alg);
        assertFalse(alg.equals("DSA"));
    }


    /**
     * A test against the "generatePrivate(KeySpec)" method.
     * The test checks out that the method throws up InvalidKeySpecException
     * if argument is neither "PKCS8EncodedKeySpec" nor "DSAPrivateKeySpec"
     */
    public final void testGeneratePrivateKeySpec01() throws Exception {
        try {
            kf.generatePrivate(x509KeySpec);
            fail("testcase1: no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.generatePrivate(null);
            fail("testcase2: no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
    }


    /**
     * A test against the "generatePrivate(KeySpec)" method.
     * The test checks out that the method returns DSAPrivateKey
     * if argument is "PKCS8EncodedKeySpec"
     */
    public final void testGeneratePrivateKeySpec02() throws Exception {

         checkPrivateKeys( (DSAPrivateKey) kf.generatePrivate(pkcs8KeySpec) );
    }


    /**
     * A test against the "generatePrivate(KeySpec)" method.
     * The test checks out that the method returns DSAPrivateKey
     * if argument is "DSAPrivateKeySpec"
     */
    public final void testGeneratePrivateKeySpec03() throws Exception {

        KeySpec keySpec = (KeySpec) new DSAPrivateKeySpec(privateX,
                                                          privateP, privateQ, privateG);

        checkPrivateKeys( (DSAPrivateKey) kf.generatePrivate(keySpec) );
    }


    /**
     * A test against the "generatePrivate(KeySpec)" method.
     * The test checks out that the method throws up InvalidKeySpecException
     * if KeySpec argument contains incorrect ASN.1 syntax
     */
    public final void testGeneratePrivateKeySpec04() throws Exception {

        PKCS8EncodedKeySpec ks;
        DSAPrivateKey prKey;

        final BigInteger x = privateX;
        final BigInteger p = privateP;
        final BigInteger q = privateQ;
        final BigInteger g = privateG;

        final byte enc1[] = new byte[20];
        System.arraycopy(privateEncoding, 0, enc1, 0, 20);
        final byte[] enc2 = enc1;

        prKey = new DSAPrivateKey () {

                  public BigInteger getX() { return x; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "PKCS8"; }
              };

        ks = kf.getKeySpec(prKey, PKCS8EncodedKeySpec.class);
        try {
            prKey = (DSAPrivateKey) kf.generatePrivate((KeySpec)ks);
            fail("no InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
    }


    /**
     * A compatibility with RI test.
     * It checks out that if key encoding in KeySpec has correct ASN1 structure
     * but AlgorithmIdentifier contains value that connot be translated to "DSA"
     * the "generatePrivate" method returns DSA private key
     * whose algorithm is neither null nor "DSA".
     */
    public final void testGeneratePrivateKeySpec05() throws Exception {

        PKCS8EncodedKeySpec ks;
        DSAPrivateKey prKey;

        final BigInteger x = privateX;
        final BigInteger p = privateP;
        final BigInteger q = privateQ;
        final BigInteger g = privateG;

        final byte enc1[] = new byte[privateEncoding.length];
        System.arraycopy(privateEncoding, 0, enc1, 0, privateEncoding.length);
        enc1[13] = 0;
        final byte[] enc2 = enc1;

        prKey = new DSAPrivateKey () {

                  public BigInteger getX() { return x; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "PKCS8"; }
              };

        ks = kf.getKeySpec(prKey, PKCS8EncodedKeySpec.class);
        prKey = (DSAPrivateKey) kf.generatePrivate((KeySpec)ks);

        String alg = prKey.getAlgorithm();
        assertNotNull(alg);
        assertFalse("DSA".equals(alg));
    }


    /**
     * A test against the "getKeySpec(Key, Class)" method.
     * The test checks out that the method throws up InvalidKeySpecException if
     * a "Class" argument is not appropriate for a "Key" argument
     * regardless of whether a correct value or null is passed to a Key argument.
     */
    public final void testGetKeySpec01() throws Exception {

        try {
            kf.getKeySpec( privateKey, DSAPublicKeySpec.class);
            fail("testcase1: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( privateKey, X509EncodedKeySpec.class);
            fail("testcase2: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( null, DSAPublicKeySpec.class);
            fail("testcase3: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( null, X509EncodedKeySpec.class);
            fail("testcase4: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( publicKey, DSAPrivateKeySpec.class);
            fail("testcase5: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( publicKey, PKCS8EncodedKeySpec.class);
            fail("testcase6: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( null, DSAPrivateKeySpec.class);
            fail("testcase7: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
        try {
            kf.getKeySpec( null, PKCS8EncodedKeySpec.class);
            fail("testcase8: No InvalidKeySpecException");
        } catch (InvalidKeySpecException e) {
        }
    }


    /**
     * A test against the "getKeySpec(Key, Class)" method.
     * The test checks out that the method throws up NullPointerException
     * if null is passed to a "Class" argument.
     */
    public final void testGetKeySpec02() throws Exception {
        try {
            kf.getKeySpec(privateKey, null);
            fail("testcase1: No NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            kf.getKeySpec(publicKey, null);
            fail("testcase2: No NullPointerException");
        } catch (NullPointerException e) {
        }
    }


    /**
     * A test against the "getKeySpec(Key, Class)" method.
     * The test checks out that
     * 1) a KeySpec returned by the method is being casted to
     *    expected "DSAPublicKeySpec" or "X509EncodedKeySpec", and
     * 2) DSAPublickey object generated from KeySpec is equal a "publicKey" argument.
     */
    public final void testGetKeySpec03() throws Exception {

        KeySpec ks;

        ks = kf.getKeySpec( publicKey, DSAPublicKeySpec.class);
        checkPublicKeys( (DSAPublicKey) kf.generatePublic((DSAPublicKeySpec) ks) );

        ks = kf.getKeySpec( publicKey, X509EncodedKeySpec.class);
        checkPublicKeys( (DSAPublicKey) kf.generatePublic((X509EncodedKeySpec) ks) );
    }


    /**
     * A test against the "getKeySpec(Key, Class)" method.
     * The test checks out that
     * 1) a KeySpec returned by the method is being casted to
          expected "DSAPrivateKeySpec" or "PKCS8EncodedKeySpec", and
     * 2) DSAPublickey object generated from KeySpec is equal a "privateKey" argument.
     */
    public final void testGetKeySpec04() throws Exception {

        KeySpec ks;

        ks = kf.getKeySpec( privateKey, DSAPrivateKeySpec.class);
        checkPrivateKeys( (DSAPrivateKey) kf.generatePrivate((DSAPrivateKeySpec) ks) );

        ks = kf.getKeySpec( privateKey, PKCS8EncodedKeySpec.class);
        checkPrivateKeys( (DSAPrivateKey) kf.generatePrivate((PKCS8EncodedKeySpec) ks) );
    }


    /**
     * Compatibility with RI test.
     * A test against the "getKeySpec(Key, Class)" method.
     * It checks out that if Key is DSAPrivateKey having incorrect encoding
     * the method doesn't throw up InvalidKeySpecException.
     */
    public final void testGetKeySpec05() throws Exception {

        int lng = 20;

        final BigInteger x = privateX;
        final BigInteger p = privateP;
        final BigInteger q = privateQ;
        final BigInteger g = privateG;

        final byte enc1[] = new byte[lng];
        System.arraycopy(privateEncoding, 0, enc1, 0, lng);	// enc1 contains incorrect encoding

        DSAPrivateKey prKey = new DSAPrivateKey () {

                  public BigInteger getX() { return x; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc1; }
                  public String getFormat()    { return "PKCS8"; }
              };
        try {
            kf.getKeySpec(prKey, PKCS8EncodedKeySpec.class);
        } catch (InvalidKeySpecException e) {
            fail("InvalidKeySpecException : " + e);
        }
    }


    /**
     * Compatibility with RI test.
     * A test against the "getKeySpec(Key, Class)" method.
     * It checks out that if Key is DSAPublicKey having incorrect encoding
     * the method doesn't throw up InvalidKeySpecException
     */
    public final void testGetKeySpec06() throws Exception {

        int lng = 20;

        final BigInteger y = publicY;
        final BigInteger p = publicP;
        final BigInteger q = publicQ;
        final BigInteger g = publicG;

        final byte enc2[] = new byte[lng];
        System.arraycopy(publicEncoding, 0, enc2, 0, lng);;	// enc2 contains incorrect encoding

        DSAPublicKey pubKey = new DSAPublicKey () {

                  public BigInteger getY() { return y; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "X.509"; }
              };

        try {
            kf.getKeySpec(pubKey, X509EncodedKeySpec.class);
        } catch (InvalidKeySpecException e) {
            fail("InvalidKeySpecException : " + e);
        }

    }


    /**
     * A test against the "translateKey(Key)" method.
     * The test checks out that the method throws up InvalidKeyException
     * if argument is not a DSAPublicKey or a DSAPrivateKey
     */
    public final void testTranslateKey01() throws Exception {
        try {
            kf.translateKey( (Key) privateRSAKey );
            fail("testcase1: No InvalidKeyException");
        } catch (InvalidKeyException e) {
        }
        try {
            kf.translateKey( (Key) publicRSAKey );
            fail("testcase2: No InvalidKeyException");
        } catch (InvalidKeyException e) {
        }
        try {
            kf.translateKey(null);
            fail("testcase3: No InvalidKeyException");
        } catch (InvalidKeyException e) {
        }
        try {
            kf.translateKey(keyPublic);
            fail("testcase4: No InvalidKeyException");
        } catch (InvalidKeyException e) {
        }
        try {
            kf.translateKey(keyPrivate);
            fail("testcase5: No InvalidKeyException");
        } catch (InvalidKeyException e) {
        }
    }


    /**
     * A test against the "translateKey(Key)" method.
     * The test checks out that for a DSAPublicKey argument
     * the new key has the same values of p, q, g, and y that original key has.
     */
    public final void testTranslateKey02() throws Exception {

        checkPublicIntegers( (DSAPublicKey) kf.translateKey(publicKey) );
    }


    /**
     * A test against the "translateKey(Key)" method.
     * It checks out that for a DSAPrivateKey argument
     * the new key has the same values of p, q, g, and x that original key has.
     */
    public final void testTranslateKey03() throws Exception {

        checkPrivateIntegers( (DSAPrivateKey) kf.translateKey(privateKey) );
    }


    /**
     * A compatibility with RI test.
     * The test against the "translateKey(Key)" method.
     * It checks out that
     * if a key encoding in a DSAPrivateKey argument has correct ASN1 structure
     * but AlgorithmIdentifier contains value that connot be translated to "DSA"
     * the method returns DSAPrivateKey whose algorithm is neither null nor "DSA".
     */
    public final void testTranslateKey04() throws Exception {

        final BigInteger y = publicY;
        final BigInteger p = publicP;
        final BigInteger q = publicQ;
        final BigInteger g = publicG;

        byte[] publicEncoding  = publicKey.getEncoded();

        final byte enc1[] = new byte[publicEncoding.length];
        System.arraycopy(publicEncoding, 0, enc1, 0, publicEncoding.length);
        enc1[13] = 0;
        final byte[] enc2 = enc1;

        DSAPublicKey pubKey = new DSAPublicKey () {

                  public BigInteger getY() { return y; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "X.509"; }
              };

        X509EncodedKeySpec ks = kf.getKeySpec(pubKey, X509EncodedKeySpec.class);

        pubKey = (DSAPublicKey) kf.generatePublic((KeySpec)ks);
        pubKey = (DSAPublicKey) kf.translateKey( (Key)pubKey );

        String alg = pubKey.getAlgorithm();
        assertNotNull(alg);
        assertFalse("X.509".equals(alg));
    }


    /**
     * A compatibility with RI test.
     * The test against the "translateKey(Key)" method.
     * It checks out that
     * if a key encoding in a DSAPrivateKey argument has correct ASN1 structure
     * but AlgorithmIdentifier contains value that connot be translated to "DSA"
     * the method returns DSAPrivateKey whose algorithm is neither null nor "DSA".
     */
    public final void testTranslateKey05() throws Exception {

        final BigInteger x = privateKey.getX();
        final BigInteger p = privateParams.getP();
        final BigInteger q = privateParams.getP();
        final BigInteger g = privateParams.getP();

        byte[] privateEncoding  = privateKey.getEncoded();

        final byte enc1[] = new byte[privateEncoding.length];
        System.arraycopy(privateEncoding, 0, enc1, 0, privateEncoding.length);
        enc1[13] = 0;
        final byte[] enc2 = enc1;

        DSAPrivateKey prKey = new DSAPrivateKey () {

                  public BigInteger getX() { return x; }
                  public DSAParams getParams() {
                      return  (DSAParams)(new DSAParameterSpec(p, q, g));
                  }
                  public String getAlgorithm() { return "DSA"; }
                  public byte[] getEncoded()   { return enc2; }
                  public String getFormat()    { return "PKCS8"; }
              };

        PKCS8EncodedKeySpec ks = kf.getKeySpec(prKey, PKCS8EncodedKeySpec.class);

        prKey = (DSAPrivateKey) kf.generatePrivate((KeySpec)ks);
        prKey = (DSAPrivateKey) kf.translateKey( (Key)prKey );

        String alg = prKey.getAlgorithm();
        assertNotNull(alg);
        assertFalse("PKCS8".equals(alg));
    }


    public static Test suite() {
        return new TestSuite(DSAKeyFactoryImplTest.class);
    }

}
