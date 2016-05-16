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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPathParameters;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CertSelector;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.harmony.security.tests.support.cert.TestUtils;

import junit.framework.TestCase;

/**
 * Tests for <code>PKIXParameters</code> fields and methods
 * 
 */
public class PKIXParameters_ImplTest extends TestCase {
   
    /**
     * Test #1 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * Assertion: Creates an instance of <code>PKIXParameters</code>
     * that populates the set of most-trusted CAs from the trusted
     * certificate entries contained in the specified <code>KeyStore</code>
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testPKIXParametersKeyStore01() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        // use valid parameter - KeyStore containing
        // only trusted X.509 certificates
        CertPathParameters cpp = new PKIXParameters(ks);
        assertTrue(cpp instanceof PKIXParameters);
    }

    /**
     * Test #2 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * Assertion: Only keystore entries that contain trusted
     * <code>X509Certificates</code> are considered; all other
     * certificate types are ignored
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testPKIXParametersKeyStore02() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED_AND_UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        // use valid parameter - KeyStore containing
        // both trusted and untrusted X.509 certificates
        PKIXParameters cpp = new PKIXParameters(ks);
        assertEquals("size", 1, cpp.getTrustAnchors().size());
    }

    /**
     * Test #4 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * Assertion: <code>KeyStoreException</code> -
     * if the <code>keystore</code> has not been initialized
     */
    public final void testPKIXParametersKeyStore04() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(false, 0);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        try {
            // pass not initialized KeyStore
            new PKIXParameters(ks);
            fail("KeyStoreException expected");
        } catch (KeyStoreException e) {
        }
    }

    /**
     * Test #5 for <code>PKIXParameters(KeyStore)</code> constructor<br>
     * Assertion: <code>InvalidAlgorithmParameterException</code> -
     * if the <code>keystore</code> does not contain at least one
     * trusted certificate entry
     */
    public final void testPKIXParametersKeyStore05() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        try {
            // pass KeyStore that does not contain trusted certificates
            new PKIXParameters(ks);
            fail("InvalidAlgorithmParameterException expected");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    /**
     * Test #5 for <code>setTrustAnchors(Set)</code> method<br>
     * Assertion: <code>Set</code> is copied to protect against
     * subsequent modifications
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testSetTrustAnchors05() throws Exception {
        // use several trusted certs in this test
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        PKIXParameters p = new PKIXParameters(ks);
        // prepare new Set
        HashSet newSet = new HashSet(p.getTrustAnchors());
        HashSet newSetCopy = (HashSet)newSet.clone();
        // set new Set
        p.setTrustAnchors(newSetCopy);
        // modify set - remove one element
        assertTrue("modified", newSetCopy.remove(newSetCopy.iterator().next()));
        // check that set maintained internally has
        // not been changed by the above modification
        assertEquals("isCopied", newSet, p.getTrustAnchors());
    }

    /**
     * Test #1 for <code>clone()</code> method<br>
     * Assertion: Makes a copy of this <code>PKIXParameters</code> object
     * @throws KeyStoreException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     */
    public final void testClone01() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }
        
        PKIXParameters p1 = new PKIXParameters(ks);
        // set to some non-default values
        p1.setPolicyQualifiersRejected(false);
        p1.setAnyPolicyInhibited(true);
        p1.setExplicitPolicyRequired(true);
        p1.setPolicyMappingInhibited(true);
        p1.setRevocationEnabled(false);

        String sigProviderName = "Some Provider";
        p1.setSigProvider(sigProviderName);

        X509CertSelector x509cs = new X509CertSelector();
        p1.setTargetCertConstraints(x509cs);

        p1.setCertStores(TestUtils.getCollectionCertStoresList());

        PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
        List l = new ArrayList();
        assertTrue("addedOk", l.add(cpc));
        p1.setCertPathCheckers(l);

        p1.setDate(new Date(555L));

        Set s = new HashSet();
        s.add("1.2.3.4.5.6.7");
        s.add("1.2.3.4.5.6.8");
        p1.setInitialPolicies(s);

        // TrustAnchors already set

        PKIXParameters p2 = (PKIXParameters)p1.clone();

        // check that objects match
        assertEquals("check1", p1.getPolicyQualifiersRejected(),
                p2.getPolicyQualifiersRejected());
        assertEquals("check2", p1.isAnyPolicyInhibited(),
                p2.isAnyPolicyInhibited());
        assertEquals("check3", p1.isExplicitPolicyRequired(),
                p2.isExplicitPolicyRequired());
        assertEquals("check4", p1.isPolicyMappingInhibited(),
                p2.isPolicyMappingInhibited());
        assertEquals("check5", p1.isRevocationEnabled(),
                p2.isRevocationEnabled());
        assertEquals("check6", p1.getSigProvider(), p2.getSigProvider());

        // just check that not null
        assertNotNull("check7", p2.getTargetCertConstraints());

        assertEquals("check8", p1.getCertStores(), p2.getCertStores());

        // just check that not empty
        assertFalse("check9", p2.getCertPathCheckers().isEmpty());

        assertEquals("check10", p1.getDate(), p2.getDate());
        assertEquals("check11", p1.getInitialPolicies(),
                p2.getInitialPolicies());
        assertEquals("check12", p1.getTrustAnchors(), p2.getTrustAnchors());
    }

    /**
     * Test #2 for <code>clone()</code> method<br>
     * Assertion: Changes to the copy will not affect
     * the original and vice versa
     * @throws KeyStoreException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     */
    public final void testClone02() throws Exception {
        PKIXParameters[] p = new PKIXParameters[2];
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        for (int i = 0; i<p.length; i++) {
            p[i] = new PKIXParameters(ks);

            p[i].setCertStores(TestUtils.getCollectionCertStoresList());

            PKIXCertPathChecker cpc = TestUtils.getTestCertPathChecker();
            List l = new ArrayList();
            assertTrue("addedOk", l.add(cpc));
            p[i].setCertPathCheckers(l);

            p[i].setDate(new Date(555L));

            p[(i == 0 ? 1 : 0)] = (PKIXParameters)p[i].clone();

            // modify the first object (original or copy)
            p[1].addCertStore(CertStore.getInstance("Collection",
                    new CollectionCertStoreParameters()));
            p[1].addCertPathChecker(TestUtils.getTestCertPathChecker());
            // check that the second object has not been affected by
            // above modification
            assertTrue("certStores["+i+"]",
                    p[0].getCertStores().size() == 1);
            assertTrue("certPathCheckers["+i+"]",
                    p[0].getCertPathCheckers().size() == 1);
        }
    }

    /**
     * Test for <code>toString()</code> method<br>
     * Assertion: Returns a formatted string describing the parameters
     * @throws InvalidAlgorithmParameterException
     * @throws KeyStoreException
     */
    public final void testToString() throws Exception {
        KeyStore ks = TestUtils.getKeyStore(true, TestUtils.TRUSTED_AND_UNTRUSTED);
        if (ks == null) {
            fail(getName() + ": not performed (could not create test KeyStore)");
        }

        PKIXParameters p = new PKIXParameters(ks);
        assertNotNull(p.toString());
    }

}
