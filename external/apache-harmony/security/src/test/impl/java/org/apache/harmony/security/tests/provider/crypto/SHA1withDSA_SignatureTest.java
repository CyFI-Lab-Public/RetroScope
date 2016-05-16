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

/*
 * Notes.
 * 1. The class doesn't contain tests against "update(..)" methods
 *    invoked on a Signature objects whose state is "INIT" or "VERIFY".
 *    Relevant checks are in tests against MessageDigest class methods.
 *
 * 2. The testSignbyteArrayintint03() is commented out
 *    because of compatibility with RI issue in current implementation of the Signature class.
 */

package org.apache.harmony.security.tests.provider.crypto;

import java.math.BigInteger;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.security.spec.DSAParameterSpec;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SHA1withDSA_SignatureTest extends TestCase {

    private static final String algorithm = "SHA1withDSA";

    private static final String provider = "Crypto";

    private static final String provider1 = "BC"; // see testVerifybyteArray04()

    private Signature signs[];

    private Signature signingSigns[];

    private Signature verifyingSign;

    private static KeyPair keys;

    private static KeyPairGenerator keyPairGenerator;

    private static final BigInteger MINUS_ONE = BigInteger.valueOf(-1);

    private static final BigInteger params[][] = new BigInteger[][] {
            { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO },
            { BigInteger.ZERO, BigInteger.ZERO, BigInteger.ONE },
            { BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE },
            { BigInteger.ONE, BigInteger.ZERO, BigInteger.ONE },
            { BigInteger.ONE, BigInteger.ONE, BigInteger.ONE },
            { MINUS_ONE, MINUS_ONE, MINUS_ONE },
            { BigInteger.ONE, BigInteger.ONE, MINUS_ONE },
            { MINUS_ONE, BigInteger.ONE, BigInteger.ONE },
            { BigInteger.ONE, MINUS_ONE, BigInteger.ONE }, };

    private static PrivateKey privateKey;

    private static PublicKey publicKey;

    private static PrivateKey privateKey2;

    private static PublicKey publicKey2;

    private static PrivateKey myRSAPrivateKey;

    private static PublicKey myRSAPublicKey;

    private static SecureRandom mySecureRandom;

    private static byte signature[];

    private static byte bytes1[] = new byte[1000];

    private static byte bytes2[] = new byte[10];

    private static byte message[] = new byte[1000];

    // offset to use in corresponding methods
    private static int offset;

    private static boolean flag;

    static {
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("DSA");

            keyPairGenerator.initialize(1024);
            keys = keyPairGenerator.generateKeyPair();

            privateKey = keys.getPrivate();
            publicKey = keys.getPublic();

            privateKey2 = Predefined.getPrivateKey();
            publicKey2 = Predefined.getPublicKey();

            myRSAPrivateKey = getRSAPrivateKey();
            myRSAPublicKey = getRSAPublicKey();

            mySecureRandom = new SecureRandom();

            mySecureRandom.nextBytes(bytes1);
            mySecureRandom.nextBytes(bytes2);
            mySecureRandom.nextBytes(message);

            byte myByte[] = new byte[1];
            mySecureRandom.nextBytes(myByte);
            offset = myByte[0] & 037;

            flag = true;
        } catch (Exception e) {
            flag = false;
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {

        if (!flag) {
            throw new Exception("some problem in static initializer");
        }

        super.setUp();

        signs = new Signature[] { Signature.getInstance(algorithm, provider),
                Signature.getInstance(algorithm, provider),
                Signature.getInstance(algorithm, provider),
                Signature.getInstance(algorithm, provider),
                Signature.getInstance(algorithm, provider) };

        signs[1].initSign(privateKey);
        signs[2].initSign(privateKey, null);
        signs[3].initSign(privateKey, mySecureRandom);
        signs[4].initVerify(publicKey);

        signingSigns = new Signature[] { signs[1], signs[2], signs[3] };
        verifyingSign = signs[4];
    }

    static private DSAPrivateKey getDSAPrivateKey(BigInteger p, BigInteger q,
            BigInteger g, BigInteger x) {
        final BigInteger pp = p;
        final BigInteger qq = q;
        final BigInteger gg = g;
        final BigInteger xx = x;

        return new DSAPrivateKey() {

            public BigInteger getX() {
                return xx;
            }

            public DSAParams getParams() {
                BigInteger p = pp;
                BigInteger q = qq;
                BigInteger g = gg;
                return new DSAParameterSpec(p, q, g);
            }

            public String getAlgorithm() {
                return "dummy";
            }

            public byte[] getEncoded() {
                return new byte[0];
            }

            public String getFormat() {
                return "dummy";
            }
        };
    }

    static private DSAPublicKey getDSAPublicKey(BigInteger p, BigInteger q,
            BigInteger g, BigInteger y) {
        final BigInteger pp = p;
        final BigInteger qq = q;
        final BigInteger gg = g;
        final BigInteger yy = y;

        return new DSAPublicKey() {

            public BigInteger getY() {
                return yy;
            }

            public DSAParams getParams() {
                BigInteger p = pp;
                BigInteger q = qq;
                BigInteger g = gg;
                return new DSAParameterSpec(p, q, g);
            }

            public String getAlgorithm() {
                return "dummy";
            }

            public byte[] getEncoded() {
                return new byte[0];
            }

            public String getFormat() {
                return "dummy";
            }
        };
    }

    static private RSAPrivateKey getRSAPrivateKey() {

        return new RSAPrivateKey() {

            public BigInteger getPrivateExponent() {
                return BigInteger.ZERO;
            }

            public BigInteger getModulus() {
                return BigInteger.ZERO;
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public byte[] getEncoded() {
                return new byte[] { 0 };
            }

            public String getFormat() {
                return "fff";
            }
        };
    }

    static private RSAPublicKey getRSAPublicKey() {

        return new RSAPublicKey() {

            public BigInteger getPublicExponent() {
                return BigInteger.ZERO;
            }

            public BigInteger getModulus() {
                return BigInteger.ZERO;
            }

            public String getAlgorithm() {
                return "RSA";
            }

            public byte[] getEncoded() {
                return new byte[] { 0 };
            }

            public String getFormat() {
                return "fff";
            }
        };
    }

    /**
     * The test against "initSign(PrivateKey)" method.
     * It checks out that regardless of a Signature object's state
     * the method throws InvalidKeyException if PrivateKey is null or not DSAPrivateKey
     */
    public final void testInitSignPrivateKey01() {

        for (int i = 0; i < signs.length; i++) {
            try {
                signs[i].initSign(null);
                fail("case1: no InvalidKeyException : i=" + i);
            } catch (InvalidKeyException e) {}
            try {
                signs[i].initSign(myRSAPrivateKey);
                fail("case: no InvalidKeyException : i=" + i);
            } catch (InvalidKeyException e) {}
        }
    }

    /**
     * The test against "initSign(PrivateKey)" method.
     * It checks out that regardless of a Signature object's state
     * the method throws InvalidKeyException if
     * PrivateKey is DSAPrivateKey and the key is invalid, that is,
     * BigIntegers constituting the key have incorrect values.
     */
    public final void testInitSignPrivateKey02() throws Exception {

        for (int i = 0; i < signs.length; i++) {
            for (int j = 0; j < params.length; j++) {
                try {
                    signs[i].initSign(getDSAPrivateKey(params[j][0],
                            params[j][1], params[j][2], BigInteger.ZERO));
                    fail("no InvalidKeyException : i =" + i + " j=" + j);
                } catch (InvalidKeyException e) {}
            }
        }
    }

    /**
     * The test against the "initSign(PrivateKey)" method.
     * It checks out that the method negates the effect of previous call
     * if it is invoked with a different argument.
     *
     * Note.
     * In RI, negating effect of previous call includes
     * discarding updates done before calling the method.
     */
    public final void testInitSignPrivateKey03() throws Exception {

        // Initially all signing Signature objects are initialized with some PublicKey
        // and the "verifyingSign" object with corresponding PrivateKey.

        verifyingSign.initVerify(publicKey2);

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(bytes1, 0, bytes1.length);

            signingSigns[i].initSign(privateKey2); // another PrivateKey

            signingSigns[i].update(bytes2, 0, bytes2.length);
            signature = signingSigns[i].sign();

            // it is expected that "signature" is one generated only for "bytes2" update

            verifyingSign.update(bytes2, 0, bytes2.length);
            assertTrue("returned 'false' : i=" + i, verifyingSign
                    .verify(signature));
        }
    }

    /**
     * The test against the "initSign(PrivateKey)" method.
     * It checks out that the method negates the effect of previous call
     * if it is invoked with the same argument.
     *
     * Notes.
     * 1. In RI, negating effect of previous call includes
     *    discarding updates done before calling the method.
     * 2.
     * The specification for the method contains the following clause:
     *     "If this method is called again with a different argument,
     *     it negates the effect of this call."
     * which meaning requires certainty in case of sequence
     *    initSign - update - initSign
     * RI behavior is such that with respect to "update"
     * it doesn't matter whether argument is the same or different.
     */
    public final void testInitSignPrivateKey04() throws Exception {

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(bytes1, 0, bytes1.length);

            signingSigns[i].initSign(privateKey); // the same PrivateKey

            signingSigns[i].update(bytes2, 0, bytes2.length);
            signature = signingSigns[i].sign();

            // it is expected that "signature" is one generated only for "bytes2" update

            verifyingSign.update(bytes2, 0, bytes2.length);
            assertTrue("returned 'false' : i=" + i, verifyingSign
                    .verify(signature));
        }
    }

    /**
     * The test against "initVerify(PublicKey)" methods
     * It checks out that regardless of a Signature object's state
     * the method throws InvalidKeyException if
     * PublicKey is null or is not DSAPublicKey
     */
    public final void testInitVerifyPublicKey01() {

        PublicKey pk = null;

        for (int i = 0; i < signs.length; i++) {
            try {
                signs[i].initVerify(pk);
                fail("case1 : no InvalidKeyException i=" + i);
            } catch (InvalidKeyException e) {}
            try {
                signs[i].initVerify(myRSAPublicKey);
                fail("case2 : no InvalidKeyException i=" + i);
            } catch (InvalidKeyException e) {}
        }
    }

    /**
     * The test against "initSign(PrivateKey)" method.
     * It checks out that regardless of a Signature object's state
     * the method throws InvalidKeyException if
     * PublicKey is DSAPublicKey and the key is invalid, that is,
     * BigIntegers constituting the key have incorrect values.
     */
    public final void testInitVerifyPublicKey02() throws Exception {

        for (int i = 0; i < signs.length; i++) {
            for (int j = 0; j < params.length; j++) {
                try {
                    signs[i].initVerify(getDSAPublicKey(params[j][0],
                            params[j][1], params[j][2], BigInteger.ZERO));
                    fail("no InvalidKeyException : i=" + i + " j=" + j);
                } catch (InvalidKeyException e) {}
            }
        }
    }

    /**
     * The test against the "initVerify(PublicKey)" method.
     * It checks out that the method negates the effect of previous call
     * if it is invoked with a different argument,
     * that is, after initializing with PublicKey from another KeyPair
     * the "verify" method returns "false".
     */
    public final void testInitVerifyPublicKey03() throws Exception {

        signingSigns[0].update(bytes1, 0, bytes1.length);
        signature = signingSigns[0].sign();

        verifyingSign.initVerify(publicKey2); // another key
        verifyingSign.update(bytes1, 0, bytes1.length);
        assertFalse("case1: returned 'true'", verifyingSign.verify(signature));
    }

    /**
     * The test against the "initVerify(PublicKey)" method.
     * It checks out that the method negates the effect of previous call
     * if it is invoked with the same argument.
     * Like it is for "initSign", RI behavior is such that with respect to "update"
     * it doesn't matter whether argument is the same or different.
     *
     * Note.
     * In RI negating effect of previous call includes
     * discarding updates done before calling the method.
     */
    public final void testInitVerifyPublicKey04() throws Exception {

        signingSigns[0].update(bytes1, 0, bytes1.length);
        signature = signingSigns[0].sign();

        verifyingSign.update(bytes2, 0, bytes2.length);

        verifyingSign.initVerify(publicKey); // the same key
        verifyingSign.update(bytes1, 0, bytes1.length);
        assertTrue("case2: returned 'false'", verifyingSign.verify(signature));
    }

    /*
     * The method returns
     * -  -1 if a signature's syntax is incorrect
     * -  length of the signature otherwise.
     */
    private int checkSignature(byte sig[], int offset) {

        int n1 = sig[offset + 3];
        int n2 = sig[offset + n1 + 5];

        if (sig[offset] != 0x30 || sig[offset + 1] != (n1 + n2 + 4)
                || sig[offset + 2] != 2 || sig[offset + n1 + 4] != 2 || n1 > 21
                || n2 > 21) {
            return -1;
        }
        return (n1 + n2 + 6);
    }

    /**
     * The test against the "sign()" method.
     * It checks out that Signature object returned by the method has correct ASN1 encoding.
     */
    public final void testSign01() throws Exception {

        byte sig[];

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(message);

            sig = signingSigns[i].sign();

            try {
                int n = checkSignature(sig, 0);
                if (n < 0) {
                    fail("incorrect signature : i=" + i);
                }
                if (n != sig.length) {
                    fail("incorrect signature's length : n=" + n
                            + " sig.length= " + sig.length);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                fail("incorrect signature: " + e);
            }
        }
    }

    /**
     * The test against the "sign(byte[], int, int)" method.
     * It checks out that Signature object returned by the method has correct ASN1 encoding.
     */
    public final void testSignbyteArrayintint01() throws Exception {

        byte sig[] = new byte[offset + 100];

        for (int i = 0; i < signingSigns.length; i++) {
            int n1;

            signingSigns[i].update(message);

            n1 = signingSigns[i].sign(sig, offset, 50);

            try {
                int n2 = checkSignature(sig, offset);
                if (n2 < 0) {
                    fail("signature bytes have invalid encoding");
                }
                if (n1 != n2) {
                    fail("incorrect signature's length : n1=" + n1 + " n2="
                            + n2);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                fail("incorrect signature: " + e);
            }
        }
    }

    /**
     * The test agains the "sign(byte[], int, int)" method.
     * It checks out that if a Signature object's state is SIGN
     * passing to the object "offset" and "len" arguments that satisfy the condition:
     *     "offset + len <= outbuf.length"
     * but "len" < actual length of signature results in throwing SignatureException.
     *
     * Note.
     * As signature's length varies we use length value of 44 which is certainly less than signature's length,
     */
    public final void testSignbyteArrayintint02() throws Exception {

        byte sig[] = new byte[50];

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(message);

            try {
                signingSigns[i].sign(sig, 0, 44);
                fail("case1 : no SignatureException : i=" + i);
            } catch (SignatureException e) {}
        }
    }

    /**
     * The test agains the "sign(byte[], int, int)" method.
     * It checks out that if a Signature object's state is SIGN
     * passing to the object "offset" and "len" arguments that satisfy the condition:
     *     "offset < 0" and "len" >= actual length of signature
     * results in throwing ArrayIndexOutOfBoundsException.
     */
    //     public final void testSignbyteArrayintint03() throws Exception {
    //
    //        byte sig[] = new byte[50];
    //
    //        for ( int i = 0; i < signingSigns.length; i++ ) {
    //
    //            signingSigns[i].update(message);
    //
    //            // because 50 is more than length of any signature,
    //            // for offset==-1 and len==50 ArrayIndexOutOfBoundsException is expected
    //            //
    //            try {
    //                signingSigns[i].sign(sig, -1, 50);
    //                fail("no ArrayIndexOutOfBoundsException");
    //            } catch (ArrayIndexOutOfBoundsException e) {
    //            }
    //
    //            // because 40 is less than length of any signature",
    //            //for offset==-1 and len==40 ArrayIndexOutOfBoundsException is not expected
    //            //
    //            try {
    //                signingSigns[i].sign(sig, -1, 40);
    //                fail("no SignatureException");
    //            } catch (ArrayIndexOutOfBoundsException e) {
    //                fail("ArrayIndexOutOfBoundsException");
    //            } catch (SignatureException e) {
    //            }
    //        }
    //    }

    /**
     * The test against the "verify(byte[])" method.
     * It checks out that for given message and signature signed with some PrivateKey
     * (1) the method returns "true" if PublicKey is from the same KeyPair,
     * (2) the method returns "false" otherwise.
     */
    public final void testVerifybyteArray01() throws Exception {

        byte sigBytes[];

        signingSigns[0].update(message);
        sigBytes = signingSigns[0].sign();

        verifyingSign.update(message);
        assertTrue("case1: test failure", verifyingSign.verify(sigBytes));

        verifyingSign.initVerify(publicKey2);

        verifyingSign.update(message);
        assertFalse("case2: test failure", verifyingSign.verify(sigBytes));
    }

    /**
     * The test is against the pair "sign() - verify(byte[])" method.
     * It checks out that the "verify" method returns "true"
     * if a verifying Signature object was updated with the same message
     * which was used to update a signing Signature object.
     */
    public final void testVerifybyteArray02() throws Exception {

        byte sig1[];
        byte sig2[];

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(bytes1);
            verifyingSign.update(bytes1);

            sig1 = signingSigns[i].sign();

            assertTrue("case 1: test failure : i=" + i, verifyingSign
                    .verify(sig1));

            signingSigns[i].update(bytes1);
            signingSigns[i].update(bytes2);
            verifyingSign.update(bytes1);
            verifyingSign.update(bytes2);

            sig2 = signingSigns[i].sign();

            assertTrue("case 2: test failure : i=" + i, verifyingSign
                    .verify(sig2));

            verifyingSign.update(bytes1);
            verifyingSign.update(bytes2);
            assertFalse("case 3: test failure : i=" + i, verifyingSign
                    .verify(sig1));
        }
    }

    /**
     * The compatibility with RI test.
     * It checks out that
     * for the predefined message and signature signed with PrivateKey from RI
     * the method invoked on a Signature object initialized with corresponding PublicKey from RI
     * returns "true".
     */
    public final void testVerifybyteArray03() throws Exception {

        verifyingSign.initVerify(publicKey2);
        verifyingSign.update(Predefined.getMessage());
        assertTrue("not verified", verifyingSign.verify(Predefined
                .getSignature()));
    }

    /**
     * The test against the pair "sign - verify" methods.
     * Its specific is that signing and verifying are performed by two providers,
     * provider and provider1;
     * so, the test checks up on compatibility signing-verifying between implementations.
     */
    public final void testVerifybyteArray04() throws Exception {

        Signature signingSign, verifyingSign;

        byte sigBytes[];

        byte message[] = new byte[1000];

        // testcase1 - signing with "provider", verifying with "provider1"

        signingSign = Signature.getInstance(algorithm, provider);
        verifyingSign = Signature.getInstance(algorithm, provider1);

        mySecureRandom.nextBytes(message);

        signingSign.initSign(privateKey);
        signingSign.update(message);
        sigBytes = signingSign.sign();

        verifyingSign.initVerify(publicKey);
        verifyingSign.update(message);
        assertTrue("case1: test failure", verifyingSign.verify(sigBytes));

        // testcase2 - signing with "provider1", verifying with "provider"

        signingSign = Signature.getInstance(algorithm, provider1);
        verifyingSign = Signature.getInstance(algorithm, provider);

        mySecureRandom.nextBytes(message);

        signingSign.initSign(privateKey);
        signingSign.update(message);
        sigBytes = signingSign.sign();

        verifyingSign.initVerify(publicKey);
        verifyingSign.update(message);
        assertTrue("case2: test failure", verifyingSign.verify(sigBytes));
    }

    /**
     * The test against the "verify(byte[], int, int)" method.
     * It checks out that if Signature object's state is VERIFY
     * the method throws IllegalArgumentException in case of the following arguments :
     *       outbufs == null or
     *       offset < 0      or
     *       length < 0      or
     *       offset+length > outbuf.length
     */
    public final void testVerifybyteArrayintint01() throws Exception {

        byte bArray[] = new byte[100];

        // case1 : null byte array
        try {
            verifyingSign.verify(null, 0, 48);
            fail("case1 : no IllegalArgumentException");
        } catch (IllegalArgumentException e) {}

        // case2 : len < 0
        try {
            verifyingSign.verify(bArray, 0, -1);
            fail("case2 : no IllegalArgumentException");
        } catch (IllegalArgumentException e) {}

        // case3 : offset < 0
        try {
            verifyingSign.verify(bArray, -1, 48);
            fail("case3: no IllegalArgumentException");
        } catch (IllegalArgumentException e) {}

        // case4 : offset+len > outbuf.length
        try {
            int k = bArray.length / 2;
            verifyingSign.verify(bArray, k, bArray.length - k + 1);
            fail("case4: no IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
    }

    /**
     * The test against the "verify(byte[], int, int)" method.
     * It checks out that if Signature object's state is VERIFY
     * the method throws up SignatureException if
     * (1) offset+length < 0
     * (2) offset > signature.length
     * (3) SignatureException if outbuf's lentgh is correct
     *     whereas a signature doesn't meet ASN1 syntax.
     */
    public final void testVerifybyteArrayintint02() throws Exception {

        byte sigBytes[] = new byte[100]; // wrong signature

        try {
            verifyingSign.verify(sigBytes, Integer.MAX_VALUE, 2);
            fail("testcase1 : no SignatureException");
        } catch (SignatureException e) {}

        try {
            verifyingSign.verify(sigBytes, 2, Integer.MAX_VALUE);
            fail("testcase2 : no SignatureException");
        } catch (SignatureException e) {}

        verifyingSign.update(message);
        try {
            verifyingSign.verify(sigBytes, 0, 50);
            fail("testcase3 : SignatureException");
        } catch (SignatureException e) {}
    }

    /**
     * The test is against the "verify(byte[], int, int)" method.
     * It checks out that in case of correct signature the method throws SignatureException
     * if value of "length" is less than signature's length
     */
    public final void testVerifybyteArrayintint03() throws Exception {

        byte sig[] = new byte[offset + 100];

        verifyingSign.update(message);

        for (int i = 0; i < signingSigns.length; i++) {
            int n;

            signingSigns[i].update(message);
            n = signingSigns[i].sign(sig, offset, 50);

            try {
                verifyingSign.verify(sig, offset, n - 1);
                fail("test failure : i=" + i + " offset=" + offset);
            } catch (SignatureException e) {
                verifyingSign.verify(sig, offset, n);
            }
        }
    }

    /**
     * The test against the "verify(byte[], int, int)" method.
     * It check out that if a call to the method returns normally
     * a Signature object resets itself.
     */
    public final void testVerifybyteArrayintint04() throws Exception {

        byte sig[] = new byte[100];
        int n = 0;
        ;

        byte sigBytes[] = new byte[100]; // bytes for correct signature

        // the signature has correct ASN1 syntax but its value is negative;
        // so, the "verify(...)" methods should return "false" without throwing an exception
        sig[0] = 48;
        sig[1] = 44;
        sig[2] = 2;
        sig[3] = 20;
        sig[4] = (byte) 0x8f; // negative value
        sig[24] = 2;
        sig[25] = 20;
        sig[26] = (byte) 0x8f; // negative value

        signingSigns[0].update(message);
        n = signingSigns[0].sign(sigBytes, 0, 50);

        // testcase1: first call returns true,
        // second returns false though signature is the same

        verifyingSign.update(message);

        assertTrue("case1: test failure", verifyingSign.verify(sigBytes, 0, n));
        assertFalse("case2: test failure", verifyingSign.verify(sigBytes, 0, n));

        // testcase2: first call returns false (incorrect signature),
        // second returns false too, in spite of correct signature (!),
        // because Signature object was reset in first call

        verifyingSign.update(message);

        assertFalse("case3: test failure", verifyingSign.verify(sig, 0, 46));
        assertFalse("case4: test failure", verifyingSign.verify(sigBytes, 0, n));
    }

    /**
     * The test against the "verify(byte[], int, int)" method.
     * It check out that a Signature object doesn't reset itself if
     * the method throws up an exception.
     */
    public final void testVerifybyteArrayintint05() throws Exception {

        byte sig[] = new byte[100]; //incorrect signature

        byte sigBytes[] = new byte[100];
        int n = 0;

        signingSigns[0].update(message);
        n = signingSigns[0].sign(sigBytes, 0, 50);

        verifyingSign.update(message);

        try {
            verifyingSign.verify(null, 0, n);
        } catch (IllegalArgumentException e) {}

        try {
            verifyingSign.verify(sigBytes, -1, n);
        } catch (IllegalArgumentException e) {}

        try {
            verifyingSign.verify(sigBytes, 0, -1);
        } catch (IllegalArgumentException e) {}

        try {
            verifyingSign.verify(sigBytes, sigBytes.length - 10, 30);
        } catch (IllegalArgumentException e) {}

        try {
            verifyingSign.verify(sigBytes, Integer.MAX_VALUE, n);
        } catch (SignatureException e) {}

        try {
            verifyingSign.verify(sig, 0, n);
        } catch (SignatureException e) {}

        // if the method doesn't reset the expected result of verificaton is "true"

        assertTrue("test failure : n=" + n, verifyingSign
                .verify(sigBytes, 0, n));
    }

    /**
     * The test is against the pair "sign(byte[],int,int) - verify(byte[],int,int)" method.
     * It checks out that the "verify" method returns "true"
     * if a verifying Signature object was updated with the same message
     * which was used to update a signing Signature object.
     */
    public final void testVerifybyteArrayintint06() throws Exception {

        byte sig1[] = new byte[offset + 100];
        byte sig2[] = new byte[offset + 100];

        int n1, n2;

        for (int i = 0; i < signingSigns.length; i++) {

            signingSigns[i].update(bytes1);
            verifyingSign.update(bytes1);

            n1 = signingSigns[i].sign(sig1, offset, 50);

            assertTrue("case 1: test failure : i=" + i + " offset=" + offset,
                    verifyingSign.verify(sig1, offset, n1));

            signingSigns[i].update(bytes1);
            signingSigns[i].update(bytes2);
            verifyingSign.update(bytes1);
            verifyingSign.update(bytes2);

            n2 = signingSigns[i].sign(sig2, offset, 50);

            assertTrue("case 2: test failure : i=" + i + " offset=" + offset,
                    verifyingSign.verify(sig2, offset, n2));

            verifyingSign.update(bytes1);
            verifyingSign.update(bytes2);
            assertFalse("case 3: test failure : i=" + i + " offset=" + offset,
                    verifyingSign.verify(sig1, offset, n1));
        }
    }

    public static Test suite() {
        return new TestSuite(SHA1withDSA_SignatureTest.class);
    }

}

class Predefined {

    static DSAPublicKey publicKey = new DSAPublicKey() {

        byte yb[] = new byte[] { 91, 91, -5, 113, -35, 61, -111, -29, -59, -5,
                -53, -72, 93, 38, 81, 10, -75, 122, -60, -88, -74, -91, -108,
                88, 55, -57, 119, -2, -104, -71, 83, 45, -55, -111, 46, -23,
                -113, -34, 105, 18, -36, -33, 3, 21, 23, 93, -128, -52, 42,
                -11, 55, 50, -27, 51, 49, 50, -20, -92, -120, -56, 28, -114,
                -30, 36, -91, -102, 39, -50, -104, -73, -94, -34, -99, -25, 76,
                -40, 4, -89, 1, -107, -66, -87, -112, 22, -40, -118, 59, -26,
                90, -90, -103, -75, -104, 26, -31, 54, -95, 105, 67, -4, -44,
                35, -75, 0, -69, 60, 65, -78, 49, 5, 1, 111, -122, 83, 22, -35,
                -65, 122, 120, -104, -78, -25, -35, 12, -106, 101, 21, 33 };

        byte pb[] = new byte[] { 0, -3, 127, 83, -127, 29, 117, 18, 41, 82,
                -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17,
                68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81,
                -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53,
                -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96,
                -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34,
                -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58,
                -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53,
                84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42,
                -17, -14, 34, 3, 25, -99, -47, 72, 1, -57 };

        byte qb[] = new byte[] { 0, -105, 96, 80, -113, 21, 35, 11, -52, -78,
                -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11 };

        byte gb[] = new byte[] { 0, -9, -31, -96, -123, -42, -101, 61, -34,
                -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6,
                58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87,
                -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73,
                22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56,
                -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82,
                30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98,
                -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112,
                -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102,
                94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42 };

        public BigInteger getY() {
            return new BigInteger(yb);
        }

        public DSAParams getParams() {
            BigInteger p = new BigInteger(pb);
            BigInteger q = new BigInteger(qb);
            BigInteger g = new BigInteger(gb);
            return (DSAParams) (new DSAParameterSpec(p, q, g));
        }

        public String getAlgorithm() {
            return "DSA";
        }

        public byte[] getEncoded() {
            return null;
        }

        public String getFormat() {
            return null;
        }
    };

    static DSAPrivateKey privateKey = new DSAPrivateKey() {

        byte xb[] = new byte[] { 12, 31, -39, 65, -61, -54, -91, 37, -93, -115,
                81, 122, -24, -104, -31, -106, 113, -39, -69, 34 };

        byte pb[] = new byte[] { 0, -3, 127, 83, -127, 29, 117, 18, 41, 82,
                -33, 74, -100, 46, -20, -28, -25, -10, 17, -73, 82, 60, -17,
                68, 0, -61, 30, 63, -128, -74, 81, 38, 105, 69, 93, 64, 34, 81,
                -5, 89, 61, -115, 88, -6, -65, -59, -11, -70, 48, -10, -53,
                -101, 85, 108, -41, -127, 59, -128, 29, 52, 111, -14, 102, 96,
                -73, 107, -103, 80, -91, -92, -97, -97, -24, 4, 123, 16, 34,
                -62, 79, -69, -87, -41, -2, -73, -58, 27, -8, 59, 87, -25, -58,
                -88, -90, 21, 15, 4, -5, -125, -10, -45, -59, 30, -61, 2, 53,
                84, 19, 90, 22, -111, 50, -10, 117, -13, -82, 43, 97, -41, 42,
                -17, -14, 34, 3, 25, -99, -47, 72, 1, -57 };

        byte qb[] = new byte[] { 0, -105, 96, 80, -113, 21, 35, 11, -52, -78,
                -110, -71, -126, -94, -21, -124, 11, -16, 88, 28, -11 };

        byte gb[] = new byte[] { 0, -9, -31, -96, -123, -42, -101, 61, -34,
                -53, -68, -85, 92, 54, -72, 87, -71, 121, -108, -81, -69, -6,
                58, -22, -126, -7, 87, 76, 11, 61, 7, -126, 103, 81, 89, 87,
                -114, -70, -44, 89, 79, -26, 113, 7, 16, -127, -128, -76, 73,
                22, 113, 35, -24, 76, 40, 22, 19, -73, -49, 9, 50, -116, -56,
                -90, -31, 60, 22, 122, -117, 84, 124, -115, 40, -32, -93, -82,
                30, 43, -77, -90, 117, -111, 110, -93, 127, 11, -6, 33, 53, 98,
                -15, -5, 98, 122, 1, 36, 59, -52, -92, -15, -66, -88, 81, -112,
                -119, -88, -125, -33, -31, 90, -27, -97, 6, -110, -117, 102,
                94, -128, 123, 85, 37, 100, 1, 76, 59, -2, -49, 73, 42 };

        public BigInteger getX() {
            return new BigInteger(xb);
        }

        public DSAParams getParams() {
            BigInteger p = new BigInteger(pb);
            BigInteger q = new BigInteger(qb);
            BigInteger g = new BigInteger(gb);
            return (DSAParams) (new DSAParameterSpec(p, q, g));
        }

        public String getAlgorithm() {
            return "DSA";
        }

        public byte[] getEncoded() {
            return null;
        }

        public String getFormat() {
            return null;
        }
    };

    static byte msg[] = new byte[] { -126, -74, -45, -105, 31, 30, 76, -78, 56,
            31, -67, -7, 40, 36, -59, -1, -41, -46, -63, -98, -10, -56, 18, 69,
            107, -118, -70, -9, -32, 49, -48, 11, -73, 98, -67, 82, 96, 126,
            24, -40, -15, 20, 81, 103, 119, -114, -9, -85, 56, 20, -98, 89,
            -37, 34, -44, 19, -19, -126, -87, -40, -102, -96, 102, -30, 30,
            115, -68, -108, -44, 88, -65, 35, 85, 123, -1, -82, 110, 67, 99,
            84, -32, 44, 67, 75, -121, 28, -128, 49, -54, 52, 53, 119, -114,
            -99, -61, -58, -119, -97, 91, 66 };

    static byte sig[] = new byte[] { 48, 44, 2, 20, 33, -6, -66, -52, 17, -91,
            -90, 83, -128, 73, -114, 49, 118, 82, -65, 123, -19, 94, -26, 106,
            2, 20, 59, 75, -86, -115, 94, -125, 80, -91, -57, 61, -73, -5,
            -109, -93, 103, -10, -73, -21, 99, 81 };

    static byte[] getMessage() {
        return msg;
    }

    static byte[] getSignature() {
        return sig;
    }

    static int getSignatureLength() {
        return sig.length;
    }

    static DSAPublicKey getPublicKey() {
        return publicKey;
    }

    static DSAPrivateKey getPrivateKey() {
        return privateKey;
    }
}
