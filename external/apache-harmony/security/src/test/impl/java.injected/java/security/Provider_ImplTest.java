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
* @author Boris V. Kuznetsov
*/

package java.security;

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Tests for <code>Provider</code> constructor and methods
 * 
 */
public class Provider_ImplTest extends TestCase {

    Provider p;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        p = new MyProvider();
    }

    /*
     * Class under test for String toString()
     */
    public final void testToString() {
        assertEquals("Incorrect provider.toString()", "MyProvider version 1.0",
                p.toString());
    }
 
    public final void testImplementsAlg() {
        HashMap hm = new HashMap();
        hm.put("KeySize", "1024");
        hm.put("AAA", "BBB");
        Provider.Service s = new Provider.Service(p, "Type", "Algorithm",
                "className", null, hm);
        p.putService(s);
        if (!p.implementsAlg("Type", "Algorithm", null, null) ||
                !p.implementsAlg("MessageDigest", "SHA-1", null, null)) {
            fail("Case 1. implementsAlg failed");
        }
        if (!p.implementsAlg("Type", "Algorithm", "KeySize", "512")) {
            fail("Case 2. implementsAlg failed");
        }
        if (p.implementsAlg("Type", "Algorithm", "KeySize", "1025")) {
            fail("Case 3. implementsAlg failed");
        }
        if (!p.implementsAlg("Type", "Algorithm", "AAA", "BBB")) {
            fail("Case 3. implementsAlg failed");
        }    
    }

    public final void testSetProviderNumber() {
        p.setProviderNumber(100);
        assertEquals("Incorrect ProviderNumber", 100, p.getProviderNumber());
    }

    public final void testGetProviderNumber() {
        assertEquals("Incorrect ProviderNumber", -1, p.getProviderNumber());
        
        int i = Security.addProvider(p);
        assertEquals("Incorrect ProviderNumber", i, p.getProviderNumber());
        Security.removeProvider(p.getName());    // clean up
    }

    class MyProvider extends Provider {
        MyProvider() {
            super("MyProvider", 1.0, "Provider for testing");
            put("MessageDigest.SHA-1", "SomeClassName");
            put("MessageDigest.abc", "SomeClassName");
            put("Alg.Alias.MessageDigest.SHA1", "SHA-1");
        }
        
        MyProvider(String name, double version, String info) {
            super(name, version, info);
        }
    }
}
