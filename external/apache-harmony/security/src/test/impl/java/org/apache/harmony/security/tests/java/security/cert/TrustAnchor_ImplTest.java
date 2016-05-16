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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.java.security.cert;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import org.apache.harmony.security.tests.support.TestKeyPair;
import org.apache.harmony.security.tests.support.cert.TestUtils;

import junit.framework.TestCase;

/**
 * Unit tests for <code>TrustAnchor</code>
 */
public class TrustAnchor_ImplTest extends TestCase {
    private static final String keyAlg = "DSA";
    // Sample of some valid CA name
    private static final String validCaNameRfc2253 =
        "CN=Test CA,"+
        "OU=Testing Division,"+
        "O=Test It All,"+
        "L=Test Town,"+
        "ST=Testifornia,"+
        "C=Testland";

    /**
     * Test #1 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: creates <code>TrustAnchor</code> instance<br>
     * Test preconditions: valid parameters passed<br>
     * Expected: must pass without any exceptions
     */
    public final void testTrustAnchorX509CertificatebyteArray01()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        // sub testcase 1
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            getFullEncoding());
        // sub testcase 2
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            getEncodingPSOnly());
        // sub testcase 3
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            getEncodingESOnly());
        // sub testcase 4
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            getEncodingNoMinMax());
    }

    /**
     * Test #2 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: creates <code>TrustAnchor</code> instance<br>
     * Test preconditions: <code>null</code> as nameConstraints passed<br>
     * Expected: must pass without any exceptions
     */
    public final void testTrustAnchorX509CertificatebyteArray02()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            null);        
    }

    /**
     * Test #3 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: nameConstraints cloned by the constructor<br>
     * Test preconditions: modify passed nameConstraints<br>
     * Expected: modification must not change object internal state
     */
    public final void testTrustAnchorX509CertificatebyteArray03()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        byte[] nc = getEncodingPSOnly();
        byte[] ncCopy = nc.clone();
        // sub testcase 5 - nameConstraints can be null
        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias),
                ncCopy);
        // modify
        ncCopy[0]=(byte)0;
        // check that above modification did not change
        // object internal state
        assertTrue(Arrays.equals(nc, ta.getNameConstraints()));
    }

    /**
     * Test #4 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: <code>NullPointerException</code> if <code>X509Certificate</code>
     * parameter is <code>null</code><br>
     * Test preconditions: pass <code>null</code> as <code>X509Certificate</code><br>
     * Expected: NullPointerException
     */
    public final void testTrustAnchorX509CertificatebyteArray04()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        try {
            new TrustAnchor(null, getFullEncoding());
            fail("NullPointerException has not been thrown");
        } catch (NullPointerException ok) {
        }
    }

    /**
     * Test #5 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: <code>IllegalArgumentException</code> if nameConstraints
     * parameter can not be decoded<br>
     * Test preconditions: pass invalid nameConstraints encoding<br>
     * Expected: IllegalArgumentException
     */
    public final void testTrustAnchorX509CertificatebyteArray05()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";

        // sub testcase 1:
        byte [] nameConstraints = getFullEncoding();
        // corrupt encoding:
        // set wrong root seq length
        nameConstraints[2] = (byte)0x8d;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 2:
        nameConstraints = getFullEncoding();
        // corrupt encoding:
        // set wrong root seq length
        nameConstraints[2] = (byte)0x8b;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 3:
        nameConstraints = getFullEncoding();
        // corrupt encoding:
        // remove right class from seq tag
        nameConstraints[3] &= (byte)0x3f;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 4:
        nameConstraints = getEncodingESOnly();
        // corrupt encoding:
        // set wrong tagged value (excludedSubtrees SEQ OF) tag [2]
        nameConstraints[2] = (byte)0xa2;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 5:
        nameConstraints = getEncodingESOnly();
        // corrupt encoding:
        // remove CONSTRUCTED flag from tagged value (excludedSubtrees SEQ OF) tag
        nameConstraints[2] &= (byte)0xdf;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 6:
        nameConstraints = getEncodingESOnly();
        // corrupt encoding:
        // set CONSTRUCTED flag for PROMITIVE tagged value tag
        // (generalSubtree's 'base' as IA5String)
        nameConstraints[5] |= (byte)0x20;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }

        // sub testcase 7:
        nameConstraints = getEncodingESOnly();
        // corrupt encoding:
        // remove scheme from URI
        // (generalSubtree's 'base' as IA5String (uniformResourceIdentifier))
        nameConstraints[12] = nameConstraints[13] = nameConstraints[14] =
            (byte)0x6f;
        try {
            new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }
    }

    /**
     * Test #6 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: creates <code>TrustAnchor</code> instance<br>
     * Test preconditions: valid parameters passed (base as OID)<br>
     * Expected: must pass without any exceptions
     */
    public final void testTrustAnchorX509CertificatebyteArray06()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        byte []  nameConstraints = getEncodingOid();
        new TrustAnchor(
            (X509Certificate)ks.getCertificate(certAlias),
            nameConstraints);
    }

    /**
     * Test #7 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: <code>IllegalArgumentException</code> if nameConstraints
     * parameter can not be decoded<br>
     * Test preconditions: pass invalid nameConstraints (OID) encoding<br>
     * Expected: IllegalArgumentException
     */
    public final void testTrustAnchorX509CertificatebyteArray07()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        byte []  nameConstraints = getEncodingOid();
        //corrupt Oid
        nameConstraints[10]= (byte) 0xFF;
        try {
            new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias),
                nameConstraints);
            fail("IllegalArgumentException has not been thrown");
        } catch (IllegalArgumentException ok) {
        }
    }

    /**
     * Test #8 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: <code>IllegalArgumentException</code> if nameConstraints
     * parameter can not be decoded<br>
     * Test preconditions: pass invalid nameConstraints encodings<br>
     * Expected: IllegalArgumentException
     */
    public final void testTrustAnchorX509CertificatebyteArray08()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        // GeneralName tags for this test (1,2 and 3 are omitted)
        byte[] generalNameTag = new byte[] {
                (byte)0xa0,(byte)0xa4,(byte)0xa5,
                (byte)0x86,(byte)0x87,(byte)0x88
        };
        // wrong (for above tags) nameConstraints encoding
        byte[] wrongEncoding = new byte[] {
                (byte)0x30,(byte)0x0c, // sequence + length
                (byte)0xa1,(byte)0x0a, // excluded subtrees, tag, len
                (byte)0x30,(byte)0x08, // sequence of, tag, len
                (byte)0xa0, // element 6 - tag identifying GeneralName choice
                (byte)0x03, // GeneralName length
                (byte)0x01,(byte)0x01,(byte)0xff, // wrong GeneralName for any choice 
                (byte)0x80,(byte)0x01,(byte)0x00 // minimum
        };
        for (int i=0; i<generalNameTag.length; i++) {
            wrongEncoding[6] = generalNameTag[i];
            try {
                new TrustAnchor(
                    (X509Certificate)ks.getCertificate(certAlias),
                    wrongEncoding);
                fail("IllegalArgumentException has not been thrown for tag " +
                        (generalNameTag[i]&0xff));
            } catch (IllegalArgumentException ok) {
            }
        }
    }

    /**
     * Test #9 for <code>TrustAnchor(X509Certificate, byte[])</code> constructor<br> 
     * Assertion: <code>IllegalArgumentException</code> if nameConstraints
     * parameter can not be decoded<br>
     * Test preconditions: pass valid and then invalid nameConstraints encodings
     * (GeneralName choice is [0] OtherName)<br>
     * Expected: no exception for valid encoding and IllegalArgumentException for invalid
     * @throws KeyStoreException
     */
    public final void testTrustAnchorX509CertificatebyteArray09()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        byte[] encoding = new byte[] {
                (byte)0x30,(byte)0x13,(byte)0xa1,(byte)0x11,
                (byte)0x30,(byte)0x0f,(byte)0xa0,(byte)0x0a,
                (byte)0x06,(byte)0x03,(byte)0x00,(byte)0x01,(byte)0x02,
                (byte)0xA0,(byte)0x03,1,1,(byte)0xff, 
                (byte)0x80,(byte)0x01,(byte)0x00
        };
        try {
            new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias), encoding);
        } catch (IllegalArgumentException failed) {
            fail("valid encoding not accepted");
        }
        // now corrupt encoding: set OtherName value tag to 1 (must be 0)
        encoding[13] = 1;
        try {
            new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias), encoding);
            fail("invalid encoding accepted");
        } catch (IllegalArgumentException ok) {
        }
    }

    /**
     * Test for <code>getNameConstraints()</code> method<br> 
     * Assertion: returns <code>nameConstraints</code> der encoding<br>
     * Test preconditions: valid nameConstraints parameter passed (not null)<br>
     * Expected: encoding passed to the ctor must match returned one<br>
     * Assertion: returns new <code>nameConstraints</code> der encoding each time<br>
     * Test preconditions: valid nameConstraints parameter passed (not null)<br>
     * Expected: must return new reference each time called
     */
    public final void testGetNameConstraints()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "testca1";
        byte[] nc = getFullEncoding();
        // sub testcase 1
        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias), nc);
        byte[] ncRet = ta.getNameConstraints();
        // assert 1
        assertTrue(Arrays.equals(nc, ncRet));
        assertNotSame(nc, ncRet);
        // assert 2
        assertNotSame(ncRet, ta.getNameConstraints());
    }

    /**
     * Test #2 for <code>getCAName()</code> method<br>
     *  
     * Assertion: returns ... <code>null</code> if <code>TrustAnchor</code>
     * was not specified as public key and CA name or CA principal pair<br>
     * Test preconditions: test object is not specified as public key
     * and CA name or CA principal pair<br>
     * Expected: <code>null</code> as return value<br>
     * @throws KeyStoreException
     * 
     */
    public final void testGetCAPublicKey02()
        throws InvalidKeySpecException, KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate("testca1"),
                null);
        assertNull(ta.getCAPublicKey());
    }

    /**
     * Test #2 for <code>getCAName()</code> method<br>
     *  
     * Assertion: returns ... <code>null</code> if <code>TrustAnchor</code>
     * was not specified as public key and CA name or CA principal pair<br>
     * Test preconditions: test object is not specified as public key
     * and CA name or CA principal pair<br>
     * Expected: <code>null</code> as return value<br>
     * @throws KeyStoreException
     */
    public final void testGetCAName02()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate("testca1"),
                null);
        assertNull(ta.getCAName());
    }

    /**
     * Test #1 for <code>getCAName()</code> method<br>
     *  
     * Assertion: returns most trusted CA certificate<br>
     * Test preconditions: valid certificate passed to the constructor<br>
     * Expected: the same certificate must be returned by the method<br>
     * @throws KeyStoreException
     * 
     */
    public final void testGetTrustedCert01()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        X509Certificate cert =
            (X509Certificate)ks.getCertificate("testca1");
        TrustAnchor ta = new TrustAnchor(cert, null);
        assertEquals(cert, ta.getTrustedCert());
    }

    /**
     * Test #2 for <code>getCA()</code> method<br>
     *  
     * Assertion: returns ... <code>null</code> if <code>TrustAnchor</code>
     * was not specified as public key and CA name or CA principal pair<br>
     * Test preconditions: test object is not specified as public key
     * and CA name or CA principal pair<br>
     * Expected: <code>null</code> as return value<br>
     * @throws KeyStoreException
     */
    public final void testGetCA02()
        throws KeyStoreException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate("testca1"),
                null);
        assertNull(ta.getCA());
    }

    /**
     * Test for <code>toString()</code> method<br>
     *  
     * Assertion: returns string representation of this <code>TrustAnchor</code>
     * Test preconditions: several valid test objects created<br>
     * Expected: method returns not <code>null</code> in all cases<br>
     */
    public final void testToString() throws Exception {

        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        String certAlias = "test";

        // sub testcase 1
        TrustAnchor ta = new TrustAnchor(
                (X509Certificate)ks.getCertificate(certAlias),
                getFullEncoding());

        assertNotNull("#1", ta.toString());

        PublicKey pk = new TestKeyPair(keyAlg).getPublic();


        // sub testcase 2
        ta = new TrustAnchor(validCaNameRfc2253, pk, getEncodingESOnly());

        assertNotNull("#2", ta.toString());

        // sub testcase 3
        X500Principal x500p = new X500Principal(validCaNameRfc2253);
        ta = new TrustAnchor(x500p, pk, getEncodingNoMinMax());

        assertNotNull("#3", ta.toString());

        // sub testcase 4
        ta = new TrustAnchor(x500p, pk, null);
        assertNotNull("#4", ta.toString());
    }

    //
    // Private stuff
    //
    
    /*
     * The following methods return valid DER encoding
     * for the following ASN.1 definition (as specified in RFC 3280 -
     *  Internet X.509 Public Key Infrastructure.
     *  Certificate and Certificate Revocation List (CRL) Profile.
     *  http://www.ietf.org/rfc/rfc3280.txt):
     * 
     *  NameConstraints ::= SEQUENCE {
     *             permittedSubtrees       [0]     GeneralSubtrees OPTIONAL,
     *             excludedSubtrees        [1]     GeneralSubtrees OPTIONAL }
     *
     *        GeneralSubtrees ::= SEQUENCE SIZE (1..MAX) OF GeneralSubtree
     *
     *        GeneralSubtree ::= SEQUENCE {
     *             base                    GeneralName,
     *             minimum         [0]     BaseDistance DEFAULT 0,
     *             maximum         [1]     BaseDistance OPTIONAL }
     *
     *        BaseDistance ::= INTEGER (0..MAX)
     *
     *        GeneralName ::= CHOICE {
     *             otherName                       [0]     OtherName,
     *             rfc822Name                      [1]     IA5String,
     *             dNSName                         [2]     IA5String,
     *             x400Address                     [3]     ORAddress,
     *             directoryName                   [4]     Name,
     *             ediPartyName                    [5]     EDIPartyName,
     *             uniformResourceIdentifier       [6]     IA5String,
     *             iPAddress                       [7]     OCTET STRING,
     *             registeredID                    [8]     OBJECT IDENTIFIER}
     */ 

    //
    // Full NameConstraints encoding
    // (generated by own encoder class created during test development)
    //
    // @return Full NameConstraints encoding
    // with all OPTIONAL values presented.
    //
    private static final byte[] getFullEncoding() {
        // DO NOT MODIFY!
        return new byte[] {
                (byte)0x30,(byte)0x81,(byte)0x8c,(byte)0xa0,
                (byte)0x44,(byte)0x30,(byte)0x16,(byte)0x86,
                (byte)0x0e,(byte)0x66,(byte)0x69,(byte)0x6c,
                (byte)0x65,(byte)0x3a,(byte)0x2f,(byte)0x2f,
                (byte)0x66,(byte)0x6f,(byte)0x6f,(byte)0x2e,
                (byte)0x63,(byte)0x6f,(byte)0x6d,(byte)0x80,
                (byte)0x01,(byte)0x00,(byte)0x81,(byte)0x01,
                (byte)0x01,(byte)0x30,(byte)0x16,(byte)0x86,
                (byte)0x0e,(byte)0x66,(byte)0x69,(byte)0x6c,
                (byte)0x65,(byte)0x3a,(byte)0x2f,(byte)0x2f,
                (byte)0x62,(byte)0x61,(byte)0x72,(byte)0x2e,
                (byte)0x63,(byte)0x6f,(byte)0x6d,(byte)0x80,
                (byte)0x01,(byte)0x00,(byte)0x81,(byte)0x01,
                (byte)0x01,(byte)0x30,(byte)0x12,(byte)0x86,
                (byte)0x0a,(byte)0x66,(byte)0x69,(byte)0x6c,
                (byte)0x65,(byte)0x3a,(byte)0x2f,(byte)0x2f,
                (byte)0x6d,(byte)0x75,(byte)0x75,(byte)0x80,
                (byte)0x01,(byte)0x00,(byte)0x81,(byte)0x01,
                (byte)0x01,(byte)0xa1,(byte)0x44,(byte)0x30,
                (byte)0x16,(byte)0x86,(byte)0x0e,(byte)0x68,
                (byte)0x74,(byte)0x74,(byte)0x70,(byte)0x3a,
                (byte)0x2f,(byte)0x2f,(byte)0x66,(byte)0x6f,
                (byte)0x6f,(byte)0x2e,(byte)0x63,(byte)0x6f,
                (byte)0x6d,(byte)0x80,(byte)0x01,(byte)0x00,
                (byte)0x81,(byte)0x01,(byte)0x01,(byte)0x30,
                (byte)0x16,(byte)0x86,(byte)0x0e,(byte)0x68,
                (byte)0x74,(byte)0x74,(byte)0x70,(byte)0x3a,
                (byte)0x2f,(byte)0x2f,(byte)0x62,(byte)0x61,
                (byte)0x72,(byte)0x2e,(byte)0x63,(byte)0x6f,
                (byte)0x6d,(byte)0x80,(byte)0x01,(byte)0x00,
                (byte)0x81,(byte)0x01,(byte)0x01,(byte)0x30,
                (byte)0x12,(byte)0x86,(byte)0x0a,(byte)0x68,
                (byte)0x74,(byte)0x74,(byte)0x70,(byte)0x3a,
                (byte)0x2f,(byte)0x2f,(byte)0x6d,(byte)0x75,
                (byte)0x75,(byte)0x80,(byte)0x01,(byte)0x00,
                (byte)0x81,(byte)0x01,(byte)0x01
        };
    }

    //
    // NameConstraints encoding without excludedSubtrees
    // (generated by own encoder class created during test development)
    //
    // @return NameConstraints encoding with 
    // permittedSubtrees only; all OPTIONAL
    // values in permittedSubtrees are presented.
    //
    private static final byte[] getEncodingPSOnly() {
        // DO NOT MODIFY!
        return new byte[] {
                (byte)0x30,(byte)0x46,(byte)0xa0,(byte)0x44,
                (byte)0x30,(byte)0x16,(byte)0x86,(byte)0x0e,
                (byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x66,
                (byte)0x6f,(byte)0x6f,(byte)0x2e,(byte)0x63,
                (byte)0x6f,(byte)0x6d,(byte)0x80,(byte)0x01,
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
                (byte)0x30,(byte)0x16,(byte)0x86,(byte)0x0e,
                (byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x62,
                (byte)0x61,(byte)0x72,(byte)0x2e,(byte)0x63,
                (byte)0x6f,(byte)0x6d,(byte)0x80,(byte)0x01,
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
                (byte)0x30,(byte)0x12,(byte)0x86,(byte)0x0a,
                (byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x6d,
                (byte)0x75,(byte)0x75,(byte)0x80,(byte)0x01,
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
        };
    }

    //
    // NameConstraints encoding without permittedSubtrees
    // (generated by own encoder class created during test development)
    //
    // @return NameConstraints encoding with 
    // excludedSubtrees only; all OPTIONAL
    // values in excludedSubtrees are presented.
    //
    private static final byte[] getEncodingESOnly() {
        // DO NOT MODIFY!
        return new byte[] {
                (byte)0x30,(byte)0x46,(byte)0xa1,(byte)0x44,
                (byte)0x30,(byte)0x16,(byte)0x86,(byte)0x0e,
                (byte)0x68,(byte)0x74,(byte)0x74,(byte)0x70, // http
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x66, // ://f
                (byte)0x6f,(byte)0x6f,(byte)0x2e,(byte)0x63, // oo.c
                (byte)0x6f,(byte)0x6d,(byte)0x80,(byte)0x01, // om
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
                (byte)0x30,(byte)0x16,(byte)0x86,(byte)0x0e,
                (byte)0x68,(byte)0x74,(byte)0x74,(byte)0x70,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x62,
                (byte)0x61,(byte)0x72,(byte)0x2e,(byte)0x63,
                (byte)0x6f,(byte)0x6d,(byte)0x80,(byte)0x01,
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
                (byte)0x30,(byte)0x12,(byte)0x86,(byte)0x0a,
                (byte)0x68,(byte)0x74,(byte)0x74,(byte)0x70,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x6d,
                (byte)0x75,(byte)0x75,(byte)0x80,(byte)0x01,
                (byte)0x00,(byte)0x81,(byte)0x01,(byte)0x01,
        };
    }

    //
    // NameConstraints full encoding with all (OPTIONAL)
    // minimum/maximum GeneralSubtree fields OMITTED
    // (generated by own encoder class created during test development)
    //
    // @return Full NameConstraints encoding
    // with all (OPTIONAL) minimum/maximum
    // GeneralSubtree fields OMITTED
    //
    private static final byte[] getEncodingNoMinMax() {
        // DO NOT MODIFY!
        return new byte[] {
                (byte)0x30,(byte)0x68,(byte)0xa0,(byte)0x32,
                (byte)0x30,(byte)0x10,(byte)0x86,(byte)0x0e,
                (byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x66,
                (byte)0x6f,(byte)0x6f,(byte)0x2e,(byte)0x63,
                (byte)0x6f,(byte)0x6d,(byte)0x30,(byte)0x10,
                (byte)0x86,(byte)0x0e,(byte)0x66,(byte)0x69,
                (byte)0x6c,(byte)0x65,(byte)0x3a,(byte)0x2f,
                (byte)0x2f,(byte)0x62,(byte)0x61,(byte)0x72,
                (byte)0x2e,(byte)0x63,(byte)0x6f,(byte)0x6d,
                (byte)0x30,(byte)0x0c,(byte)0x86,(byte)0x0a,
                (byte)0x66,(byte)0x69,(byte)0x6c,(byte)0x65,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x6d,
                (byte)0x75,(byte)0x75,(byte)0xa1,(byte)0x32,
                (byte)0x30,(byte)0x10,(byte)0x86,(byte)0x0e,
                (byte)0x68,(byte)0x74,(byte)0x74,(byte)0x70,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x66,
                (byte)0x6f,(byte)0x6f,(byte)0x2e,(byte)0x63,
                (byte)0x6f,(byte)0x6d,(byte)0x30,(byte)0x10,
                (byte)0x86,(byte)0x0e,(byte)0x68,(byte)0x74,
                (byte)0x74,(byte)0x70,(byte)0x3a,(byte)0x2f,
                (byte)0x2f,(byte)0x62,(byte)0x61,(byte)0x72,
                (byte)0x2e,(byte)0x63,(byte)0x6f,(byte)0x6d,
                (byte)0x30,(byte)0x0c,(byte)0x86,(byte)0x0a,
                (byte)0x68,(byte)0x74,(byte)0x74,(byte)0x70,
                (byte)0x3a,(byte)0x2f,(byte)0x2f,(byte)0x6d,
                (byte)0x75,(byte)0x75,
        };
    }

    // Returns OID encoding
    // (generated by own encoder class created during test development)
    private static final byte[] getEncodingOid() {
        // DO NOT MODIFY!
        return new byte[] {
                (byte) 0x30, (byte) 0x09, (byte) 0xA0, (byte) 0x07, 
                (byte) 0x30, (byte) 0x05, (byte) 0x88, (byte) 0x03, 
                (byte) 0x2A, (byte) 0x03, (byte) 0x04 
        };
    }
}
