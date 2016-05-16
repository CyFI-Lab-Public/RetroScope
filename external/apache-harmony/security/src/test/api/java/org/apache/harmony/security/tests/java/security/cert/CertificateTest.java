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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import org.apache.harmony.security.tests.support.cert.MyCertificate;

import junit.framework.TestCase;

/**
 * Tests for <code>Certificate</code> fields and methods
 * 
 */
public class CertificateTest extends TestCase {
    /**
     * Meaningless cert encoding just for testing purposes
     */
    private static final byte[] testEncoding = new byte[] {
            (byte)1, (byte)2, (byte)3, (byte)4, (byte)5
    };


    /**
     * Constructor for CertificateTest.
     * @param name
     */
    public CertificateTest(String name) {
        super(name);
    }

    //
    // Tests
    //

    /**
     * Test for <code>hashCode()</code> method<br>
     * Assertion: returns hash of the <code>Certificate</code> instance
     */
    public final void testHashCode() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);

        assertTrue(c1.hashCode() == c2.hashCode());
    }

    /**
     * Test for <code>hashCode()</code> method<br>
     * Assertion: hash code of equal objects should be the same
     */
    public final void testHashCodeEqualsObject() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);

        assertTrue((c1.hashCode() == c2.hashCode()) && c1.equals(c2));
    }


    /**
     * Test for <code>getType()</code> method<br>
     * Assertion: returns this certificate type 
     */
    public final void testGetType() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertEquals("TEST_TYPE", c1.getType());
    }

    /**
     * Test #1 for <code>equals(Object)</code> method<br>
     * Assertion: object equals to itself 
     */
    public final void testEqualsObject01() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertTrue(c1.equals(c1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object equals to other <code>Certificate</code>
     * instance with the same state
     */
    public final void testEqualsObject02() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        Certificate c2 = new MyCertificate("TEST_TYPE", testEncoding);
        assertTrue(c1.equals(c2) && c2.equals(c1));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to <code>null</code>
     */
    public final void testEqualsObject03() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertFalse(c1.equals(null));
    }

    /**
     * Test for <code>equals(Object)</code> method<br>
     * Assertion: object not equals to other which is not
     * instance of <code>Certificate</code>
     */
    public final void testEqualsObject04() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        assertFalse(c1.equals("TEST_TYPE"));
    }

    //
    // the following tests just call methods
    // that are abstract in <code>Certificate</code>
    // (So they just like signature tests)
    //

    /**
     * This test just calls <code>getEncoded()</code> method<br>
     * @throws CertificateEncodingException
     */
    public final void testGetEncoded() throws CertificateEncodingException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.getEncoded();
    }

    /**
     * This test just calls <code>verify(PublicKey)</code> method<br>
     * 
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    public final void testVerifyPublicKey()
        throws InvalidKeyException,
               CertificateException,
               NoSuchAlgorithmException,
               NoSuchProviderException,
               SignatureException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.verify(null);
    }

    /**
     * This test just calls <code>verify(PublicKey,String)</code> method<br>
     *
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws SignatureException
     */
    public final void testVerifyPublicKeyString()
        throws InvalidKeyException,
               CertificateException,
               NoSuchAlgorithmException,
               NoSuchProviderException,
               SignatureException {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.verify(null, null);
    }

    /**
     * This test just calls <code>toString()</code> method<br>
     */
    public final void testToString() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.toString();
    }

    /**
     * This test just calls <code>testGetPublicKey()</code> method<br>
     */
    public final void testGetPublicKey() {
        Certificate c1 = new MyCertificate("TEST_TYPE", testEncoding);
        c1.getPublicKey();
    }

}
