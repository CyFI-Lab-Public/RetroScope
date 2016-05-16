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
* @author Stepan M. Mishura
*/

package org.apache.harmony.auth.tests.javax.security.auth;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

import org.apache.harmony.auth.internal.SecurityTest;

/**
 * Tests SubjectDomainCombiner class
 */

public class SubjectDomainCombinerTest extends SecurityTest {
    /**
     * @tests javax.security.auth.SubjectDomainCombiner#SubjectDomainCombiner(
     *        javax.security.auth.Subject)
     */
    public final void test_ConstructorLjavax_security_auth_Subject() {

        Subject subject = new Subject();

        SubjectDomainCombiner combiner = new SubjectDomainCombiner(subject);

        assertSame("Subject", subject, combiner.getSubject());

        try {
            // Regression for HARMONY-219
            new SubjectDomainCombiner(null);
            fail("Constructor should throw NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests javax.security.auth.SubjectDomainCombiner#combine(ProtectionDomain[],
     *        ProtectionDomain[])
     */
    public final void testCombine() throws Exception {

        Principal principal = new Principal() {
            public String getName() {
                return "principal";
            }
        };

        Subject subject = new Subject();

        subject.getPrincipals().add(principal);

        SubjectDomainCombiner combiner = new SubjectDomainCombiner(subject);

        ProtectionDomain[] pd;

        // test case: both parameters are null
        assertNull(combiner.combine(null, null));

        // test case: check assigned principals
        URL url = new URL("file://foo.txt");

        CodeSource source = new CodeSource(url, (Certificate[]) null);
        PermissionCollection permissions = new Permissions();
        ClassLoader classLoader = new URLClassLoader(new URL[] { url });

        Principal p = new Principal() {
            public String getName() {
                return "p";
            }
        };
        Principal[] principals = new Principal[] { p };

        ProtectionDomain domain = new ProtectionDomain(source, permissions,
                classLoader, principals);

        pd = combiner.combine(new ProtectionDomain[] { domain }, null);

        assertSame("CodeSource", source, pd[0].getCodeSource());
        assertSame("PermissionCollection", permissions, pd[0]
                .getPermissions());
        assertSame("ClassLoader", classLoader, pd[0].getClassLoader());

        assertEquals("Size", 1, pd[0].getPrincipals().length);
        assertSame("Principal", principal, (pd[0].getPrincipals())[0]);

        // test case: check inherited domains
        pd = combiner.combine(null, new ProtectionDomain[] { domain });
        assertSame("Inherited domain", domain, pd[0]);

        //Regression for HARMONY-1129
        assertNotNull(new SubjectDomainCombiner(new Subject()).combine(new ProtectionDomain[] {null}, new ProtectionDomain[] {null}));
    }

    public final void testSecurityException() {

        denyPermission(new AuthPermission("getSubjectFromDomainCombiner"));

        try {
            new SubjectDomainCombiner(new Subject()).getSubject();
        } catch (AccessControlException e) {
            assertEquals(e, AuthPermission.class);
        }
    }
}