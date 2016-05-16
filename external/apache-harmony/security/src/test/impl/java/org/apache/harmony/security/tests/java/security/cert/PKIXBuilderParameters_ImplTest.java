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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CertSelector;

import org.apache.harmony.security.tests.support.cert.TestUtils;

import junit.framework.TestCase;

/**
 * Tests for <code>PKIXBuilderParameters</code> fields and methods
 * 
 */
public class PKIXBuilderParameters_ImplTest extends TestCase {
    private static final int DEFAULT_MAX_PATH_LEN = 5;

    /**
     * Test #1 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: creates an instance of <code>PKIXBuilderParameters</code>
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector01()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        // both parameters are valid and non-null
        PKIXParameters p =
            new PKIXBuilderParameters(ks, new X509CertSelector());
        assertTrue("instanceOf", p instanceof PKIXBuilderParameters);
        assertNotNull("certSelector", p.getTargetCertConstraints());
    }

    /**
     * Test #2 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: creates an instance of <code>PKIXBuilderParameters</code>
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector02()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        // both parameters are valid but CertSelector is null
        PKIXParameters p =
            new PKIXBuilderParameters(ks, null);
        assertTrue("instanceOf", p instanceof PKIXBuilderParameters);
        assertNull("certSelector", p.getTargetCertConstraints());
    }

    /**
     * Test #3 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: Only keystore entries that contain trusted
     * <code>X509Certificates</code> are considered; all other
     * certificate types are ignored
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector03()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED_AND_UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        // both parameters are valid but CertSelector is null
        PKIXParameters p =
            new PKIXBuilderParameters(ks, null);
        assertTrue("instanceof", p instanceof PKIXBuilderParameters);
        assertEquals("size", 1, p.getTrustAnchors().size());
    }

    /**
     * Test #5 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>KeyStoreException</code> -
     * if the <code>keystore</code> has not been initialized
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector05() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(false, 0);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        
        try {
            // pass not initialized KeyStore
            new PKIXBuilderParameters(ks, null);
            fail("KeyStoreException expected");
        } catch (KeyStoreException e) {
        }
    }

    /**
     * Test #6 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>InvalidAlgorithmParameterException</code> -
     * if the <code>keystore</code> does not contain at least one
     * trusted certificate entry
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector06() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
            return;
        }

        try {
            // pass KeyStore that does not contain trusted certificates
            new PKIXBuilderParameters(ks, null);
            fail("InvalidAlgorithmParameterException expected");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    /**
     * Test for <code>getMaxPathLength()</code> method<br>
     * Assertion: The default maximum path length, if not specified, is 5
     * @throws KeyStoreException
     * @throws InvalidAlgorithmParameterException
     */
    public final void testGetMaxPathLength01()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        PKIXBuilderParameters p = new PKIXBuilderParameters(ks, null);
        assertEquals(DEFAULT_MAX_PATH_LEN, p.getMaxPathLength());
    }

    /**
     * Test #1 for <code>setMaxPathLength(int)</code> method<br>
     * Assertion: sets the maximum number of non-self-signed certificates
     * in the cert path
     * @throws KeyStoreException
     * @throws InvalidAlgorithmParameterException
     */
    public final void testSetMaxPathLength01()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        // all these VALID maxPathLength values must be
        // set (and get) without exceptions
        int[] testPathLength = new int[] {-1, 0, 1, 999, Integer.MAX_VALUE};
        for (int i=0; i<testPathLength.length; i++) {
            PKIXBuilderParameters p = new PKIXBuilderParameters(ks, null);
            p.setMaxPathLength(testPathLength[i]);
            assertEquals("i="+i, testPathLength[i], p.getMaxPathLength());
        }
    }

    /**
     * Test #2 for <code>setMaxPathLength(int)</code> method<br>
     * Assertion: throws InvalidParameterException if parameter is
     * less than -1
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testSetMaxPathLength02()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        PKIXBuilderParameters p = new PKIXBuilderParameters(ks, null);

        try {
            // pass parameter less than -1
            p.setMaxPathLength(Integer.MIN_VALUE);
            fail("InvalidParameterException expected");
        } catch (InvalidParameterException e) {
        }
    }

    /**
     * Test for <code>toString()</code> method<br>
     * Assertion: returns string describing this object
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testToString()
        throws KeyStoreException,
               InvalidAlgorithmParameterException {
        KeyStore ks = TestUtils.getKeyStore(true,TestUtils.TRUSTED_AND_UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        PKIXBuilderParameters p =
            new PKIXBuilderParameters(ks, new X509CertSelector());
        String rep = p.toString();

        assertNotNull(rep);
    }

}
