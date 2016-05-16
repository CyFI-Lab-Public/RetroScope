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
* @author Aleksei Y. Semenov
*/

package org.apache.harmony.security.tests;

import java.security.Identity;
import java.security.IdentityScope;
import java.security.KeyManagementException;
import java.util.Enumeration;

import org.apache.harmony.security.SystemScope;
import org.apache.harmony.security.tests.support.IdentityScopeStub;
import org.apache.harmony.security.tests.support.PublicKeyStub;

import junit.framework.TestCase;


/**
 * Unit test for class org.apache.harmony.security.SystemScope
 *
 */

public class SystemScopeTest extends TestCase {

    IdentityScope ss = null;
    final static boolean mode = true;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        if (mode) ss = new SystemScope("SystemScope");
        else {
            ss = IdentityScope.getSystemScope();
            Enumeration e = ss.identities();
            while (e.hasMoreElements()) ss.removeIdentity((Identity)e.nextElement());
        }
    }

    /**
     * Constructor for SystemScopeTest.
     * @param arg0
     */
    public SystemScopeTest(String arg0) {
        super(arg0);
    }

    /**
     * verify SystemScope.size() returns number of Identities
     */

    public void testSize() throws Exception {
        //SystemScope ss = new SystemScope("SystemScope");
        assertEquals(0, ss.size());
        ss.addIdentity(new IdentityScopeStub("aaa"));
        assertEquals(1, ss.size());
        ss.addIdentity(new IdentityScopeStub("bbb"));
        assertEquals(2, ss.size());
    }

    /*
     * verify  getIdentity(String) returns requested identity or null if not found
     */
    public void testGetIdentityString() throws Exception {
        //SystemScope ss = new SystemScope("SystemScope");
        assertNull(ss.getIdentity("aaa"));

        java.security.Identity aaa = new IdentityScopeStub("aaa");
        ss.addIdentity(aaa);
        assertSame(aaa, ss.getIdentity(aaa.getName()));
        assertNull(ss.getIdentity("bbb"));

    }

    /*
     * verify  getIdentity(PublicKey) returns requested identity or null if not found
     */
    public void testGetIdentityPublicKey() throws Exception {
        //SystemScope ss = new SystemScope("SystemScope");
        java.security.PublicKey kkk = new PublicKeyStub("kkk", "fff", null);
        assertNull(ss.getIdentity(kkk));
        java.security.Identity aaa = new IdentityScopeStub("aaa");
        aaa.setPublicKey(kkk);
        ss.addIdentity(aaa);

        assertSame(aaa, ss.getIdentity(kkk));
    }

    /**
     * verify that only one identity with given name or public key can be added
     * i.e. KeyManagementException is thrown
     */
    public void testAddIdentity() throws Exception {
//        SystemScope ss = new SystemScope("SystemScope");
        java.security.PublicKey kkk = new PublicKeyStub("kkk", "fff", null);
        java.security.Identity aaa = new IdentityScopeStub("aaa");
        aaa.setPublicKey(kkk);
        ss.addIdentity(aaa);

        java.security.Identity bbb = new IdentityScopeStub("aaa");
        try {
            ss.addIdentity(bbb);
            fail("KeyManagementException should be thrown for already used name");
        } catch (KeyManagementException ok) {}

        java.security.Identity ccc = new IdentityScopeStub("ccc");
        ccc.setPublicKey(kkk);
        try {
            ss.addIdentity(ccc);
            fail("KeyManagementException should be thrown for already used key");
        } catch (KeyManagementException ok) {}


    }

    /**
     * verify that identities are removed
     * @throws Exception
     */

    public void testRemoveIdentity() throws Exception{
//        SystemScope ss = new SystemScope("SystemScope");
        java.security.Identity aaa = new IdentityScopeStub("aaa");
        ss.addIdentity(aaa);
        assertEquals(1, ss.size());
        ss.removeIdentity(aaa);
        assertEquals(0, ss.size());
    }

    public void testIdentities() throws Exception {
//        SystemScope ss = new SystemScope("SystemScope");
        java.security.Identity aaa = new IdentityScopeStub("aaa");
        java.security.Identity bbb = new IdentityScopeStub("bbb");
        ss.addIdentity(aaa);
        ss.addIdentity(bbb);

        boolean hasAaa = false, hasBbb = false;
        Enumeration e = ss.identities();
        while (e.hasMoreElements()){
            Object i = e.nextElement();
            if (!hasAaa) hasAaa = (i==aaa);
            if (!hasBbb) hasBbb = (i==bbb);
        }
        assertTrue(hasAaa && hasBbb);
    }

    /*
     * Class under test for void SystemScope()
     */
    public void testSystemScope() {
    }

    /*
     * Class under test for void SystemScope(String)
     */
    public void testSystemScopeString() {
        SystemScope ss = new SystemScope("SystemScope");
        assertEquals("SystemScope", ss.getName());
    }

    /*
     * Class under test for void SystemScope(String, IdentityScope)
     */
    public void testSystemScopeStringIdentityScope() throws Exception {
        SystemScope ss = new SystemScope("SystemScope");
        SystemScope nested = new SystemScope("NestedScope", ss);
        assertEquals("NestedScope", nested.getName());
        assertSame(ss, nested.getScope());
    }

}
