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
* @author Alexey V. Varlamov
*/

package org.apache.harmony.security.tests;

import java.security.Principal;

import org.apache.harmony.security.UnresolvedPrincipal;
import junit.framework.TestCase;


/**
 * Tests for <code>UnresolvedPrincipal</code>
 *
 */

public class UnresolvedPrincipalTest extends TestCase {

    public void testCtor() {
        String klass = "abc";
        String name = "NAME";
        UnresolvedPrincipal up = new UnresolvedPrincipal(klass, name);
        assertEquals(klass, up.getClassName());
        assertEquals(name, up.getName());

        up = new UnresolvedPrincipal(klass, null);
        assertEquals(klass, up.getClassName());
        assertNull(up.getName());

        try {
            up = new UnresolvedPrincipal(null, name);
            fail("No IllegalArgumentException is thrown");
        } catch (IllegalArgumentException ok) {
        }
    }

    public void testEquals_Principal() {
        String name = "sgrt";
        FakePrincipal fp = new FakePrincipal(name);

        assertTrue(new UnresolvedPrincipal(FakePrincipal.class.getName(), name)
            .equals(fp));
        assertTrue(new UnresolvedPrincipal(UnresolvedPrincipal.WILDCARD, name)
            .equals(fp));
        assertTrue(new UnresolvedPrincipal(FakePrincipal.class.getName(),
            UnresolvedPrincipal.WILDCARD).equals(fp));

        assertFalse(new UnresolvedPrincipal(FakePrincipal.class.getName(),
            "sdkljfgbkwe").equals(fp));
    }

    public void testEquals_Common() {
        String klass = "abc";
        String name = "NAME";
        UnresolvedPrincipal up = new UnresolvedPrincipal(klass, name);
        UnresolvedPrincipal up2 = new UnresolvedPrincipal(klass, name);
        UnresolvedPrincipal up3 = new UnresolvedPrincipal(name, klass);

        assertTrue(up.equals(up));
        assertTrue(up.equals(up2));
        assertEquals(up.hashCode(), up2.hashCode());
        assertFalse(up.equals(up3));
        assertFalse(up.equals(null));
        assertFalse(up.equals(new Object()));
    }

    public void testImplies() {
        String name = "sgrt";
        FakePrincipal fp = new FakePrincipal(name);
        assertTrue(new UnresolvedPrincipal(FakePrincipal.class.getName(), name)
            .implies(fp));
        assertTrue(new UnresolvedPrincipal(UnresolvedPrincipal.WILDCARD, name)
            .implies(fp));
        assertTrue(new UnresolvedPrincipal(FakePrincipal.class.getName(),
            UnresolvedPrincipal.WILDCARD).implies(fp));
        assertTrue(new UnresolvedPrincipal(UnresolvedPrincipal.WILDCARD,
            UnresolvedPrincipal.WILDCARD).implies(fp));

        assertFalse(new UnresolvedPrincipal(
            UnresolvedPrincipal.class.getName(), name).implies(fp));
        assertFalse(new UnresolvedPrincipal(FakePrincipal.class.getName(),
            "hgfuytr765").implies(fp));
    }
}

class FakePrincipal implements Principal {

    private String name;

    public FakePrincipal(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
