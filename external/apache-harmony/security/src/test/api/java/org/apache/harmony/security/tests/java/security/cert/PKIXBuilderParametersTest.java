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
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CertSelector;
import java.util.HashSet;
import java.util.Set;

import org.apache.harmony.security.tests.support.cert.TestUtils;

import junit.framework.TestCase;

/**
 * Tests for <code>PKIXBuilderParameters</code> fields and methods
 * 
 */
public class PKIXBuilderParametersTest extends TestCase {
    private static final int DEFAULT_MAX_PATH_LEN = 5;

    /**
     * Constructor for PKIXBuilderParametersTest.
     * @param name
     */
    public PKIXBuilderParametersTest(String name) {
        super(name);
    }

    /**
     * Test #1 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: creates an instance of <code>PKIXBuilderParameters</code>
     * @throws InvalidAlgorithmParameterException
     */
    public final void testPKIXBuilderParametersSetCertSelector01()
        throws InvalidAlgorithmParameterException {
        Set taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName() + ": not performed (could not create test TrustAnchor set)");
        }
        // both parameters are valid and non-null
        PKIXParameters p =
            new PKIXBuilderParameters(taSet, new X509CertSelector());
        assertTrue("instanceOf", p instanceof PKIXBuilderParameters);
        assertNotNull("certSelector", p.getTargetCertConstraints());
    }

    /**
     * Test #2 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: creates an instance of <code>PKIXBuilderParameters</code>
     * @throws InvalidAlgorithmParameterException
     */
    public final void testPKIXBuilderParametersSetCertSelector02()
        throws InvalidAlgorithmParameterException {
        Set taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName() + ": not performed (could not create test TrustAnchor set)");
        }
        // both parameters are valid but CertSelector is null
        PKIXParameters p = new PKIXBuilderParameters(taSet, null);
        assertTrue("instanceOf", p instanceof PKIXBuilderParameters);
        assertNull("certSelector", p.getTargetCertConstraints());
    }

    /**
     * Test #3 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: ... the <code>Set</code> is copied to protect against
     * subsequent modifications
     * @throws InvalidAlgorithmParameterException
     */
    public final void testPKIXBuilderParametersSetCertSelector03()
        throws InvalidAlgorithmParameterException {
        Set taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName() + ": not performed (could not create test TrustAnchor set)");
        }
        HashSet originalSet = (HashSet)taSet;
        HashSet originalSetCopy = (HashSet)originalSet.clone();
        // create test object using originalSet 
        PKIXBuilderParameters pp =
            new PKIXBuilderParameters(originalSetCopy, null);
        // modify originalSet
        originalSetCopy.clear();
        // check that test object's internal state
        // has not been affected by the above modification
        Set returnedSet = pp.getTrustAnchors();
        assertEquals(originalSet, returnedSet);
    }

    /**
     * Test #4 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>NullPointerException</code> -
     * if the specified <code>Set</code> is null
     */
    public final void testPKIXBuilderParametersSetCertSelector04() throws Exception {
        try {
            // pass null
            new PKIXBuilderParameters((Set)null, null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

    /**
     * Test #5 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>InvalidAlgorithmParameterException</code> -
     * if the specified <code>Set</code> is empty
     * (<code>trustAnchors.isEmpty() == true</code>)
     */
    public final void testPKIXBuilderParametersSetCertSelector05() {
        try {
            // use empty set
            new PKIXBuilderParameters(new HashSet(), null);
            fail("InvalidAlgorithmParameterException expected");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    /**
     * Test #6 for <code>PKIXBuilderParameters(Set, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>ClassCastException</code> -
     * if any of the elements in the <code>Set</code> are not of type
     * <code>java.security.cert.TrustAnchor</code>
     */
    public final void testPKIXBuilderParametersSetCertSelector06() throws Exception {
        Set taSet = TestUtils.getTrustAnchorSet();
        if (taSet == null) {
            fail(getName() + ": not performed (could not create test TrustAnchor set)");
        }

        // add wrong object to valid set
        assertTrue(taSet.add(new Object()));

        try {
            new PKIXBuilderParameters(taSet, null);
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
        }
    }

    /**
     * Test #4 for <code>PKIXBuilderParameters(KeyStore, CertSelector)</code>
     * constructor<br>
     * Assertion: <code>NullPointerException</code> -
     * if the <code>keystore</code> is <code>null</code>
     */
    public final void testPKIXBuilderParametersKeyStoreCertSelector04() throws Exception {
        try {
            // pass null
            new PKIXBuilderParameters((KeyStore)null, null);
            fail("NPE expected");
        } catch (NullPointerException e) {
        }
    }

}
