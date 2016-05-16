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

package org.apache.harmony.security.tests.java.security;

import java.security.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;


/**
 * Tests for <code>Provider</code> constructor and methods
 * 
 */
public class ProviderTest extends TestCase {

    Provider p;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        p = new MyProvider();
    }
    
    /*
     * Class under test for void Provider()
     */
    public final void testProvider() {
        assertEquals("Provider.id name", p.getProperty("Provider.id name"),
                String.valueOf(p.getName()));
        assertEquals("Provider.id version", p
                .getProperty("Provider.id version"), String.valueOf(p
                .getVersion()));
        assertEquals("Provider.id info", p.getProperty("Provider.id info"),
                String.valueOf(p.getInfo()));
        assertEquals("Provider.id className", p
                .getProperty("Provider.id className"), p.getClass().getName());
    }

    public final void testClear() {
        p.clear();
        assertNull(p.getProperty("MessageDigest.SHA-1"));
    }

    /*
     * Class under test for void Provider(String, double, String)
     */
    public final void testProviderStringdoubleString() {
        Provider p = new MyProvider("Provider name", 123.456, "Provider info");

        assertEquals("Provider name", p.getName());
        assertEquals(123.456, p.getVersion(), 0L);
        assertEquals("Provider info", p.getInfo());
    }

    public final void testGetName() {
        assertEquals("MyProvider", p.getName());
    }

    public final void testGetVersion() {
        assertEquals(1.0, p.getVersion(), 0L);
    }

    public final void testGetInfo() {
        assertEquals("Provider for testing", p.getInfo());
    }

    /*
     * Class under test for void putAll(Map)
     */
    public final void testPutAllMap() {
        HashMap hm = new HashMap();
        hm.put("MessageDigest.SHA-1", "aaa.bbb.ccc.ddd");
        hm.put("Property 1", "value 1");
        hm.put("serviceName.algName attrName", "attrValue");
        hm.put("Alg.Alias.engineClassName.aliasName", "standardName");
        p.putAll(hm);
        if (!"value 1".equals(p.getProperty("Property 1").trim()) ||
                !"attrValue".equals(p.getProperty("serviceName.algName attrName").trim()) ||
                !"standardName".equals(p.getProperty("Alg.Alias.engineClassName.aliasName").trim()) ||
                !"aaa.bbb.ccc.ddd".equals(p.getProperty("MessageDigest.SHA-1").trim()) ) {
            fail("Incorrect property value");
        }
    }

    /*
     * Class under test for Set entrySet()
     */
    public final void testEntrySet() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");
        
        Set s = p.entrySet();
        try {
            s.clear();
            fail("Must return unmodifiable set");
        } catch (UnsupportedOperationException e) {
        }

        assertEquals("Incorrect set size", 8, s.size());

        for (Iterator it = s.iterator(); it.hasNext();) {
            Entry e = (Entry)it.next();
            String key = (String)e.getKey();
            String val = (String)e.getValue();
            if (key.equals("MessageDigest.SHA-1") && val.equals("SomeClassName")) {
                continue;
            }
            if (key.equals("Alg.Alias.MessageDigest.SHA1") && val.equals("SHA-1")) {
                continue;
            }
            if (key.equals("MessageDigest.abc") && val.equals("SomeClassName")) {
                continue;
            }
            if (key.equals("Provider.id className") && val.equals(p.getClass().getName())) {
                continue;
            }
            if (key.equals("Provider.id name") && val.equals("MyProvider")) {
                continue;
            }
            if (key.equals("MessageDigest.SHA-256") && val.equals("aaa.bbb.ccc.ddd")) {
                continue;
            }
            if (key.equals("Provider.id version") && val.equals("1.0")) {
                continue;
            }
            if (key.equals("Provider.id info") && val.equals("Provider for testing")) {
                continue;
            }
            fail("Incorrect set");
        }        
    }

    /*
     * Class under test for Set keySet()
     */
    public final void testKeySet() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");
        
        Set s = p.keySet();
        try {
            s.clear();
        } catch (UnsupportedOperationException e) {
        }
        Set s1 = p.keySet();

        assertNotSame(s, s1);
        assertFalse(s1.isEmpty());
        assertEquals(8, s1.size());

        assertTrue(s1.contains("MessageDigest.SHA-256"));
        assertTrue(s1.contains("MessageDigest.SHA-1"));
        assertTrue(s1.contains("Alg.Alias.MessageDigest.SHA1"));
        assertTrue(s1.contains("MessageDigest.abc"));
        assertTrue(s1.contains("Provider.id info"));
        assertTrue(s1.contains("Provider.id className"));
        assertTrue(s1.contains("Provider.id version"));
        assertTrue(s1.contains("Provider.id name"));
    }

    /*
     * Class under test for Collection values()
     */
    public final void testValues() {
        p.put("MessageDigest.SHA-256", "aaa.bbb.ccc.ddd");
        
        Collection c = p.values();
        try {
            c.clear();
        } catch (UnsupportedOperationException e) {
        }
        Collection c1 = p.values();

        assertNotSame(c, c1);
        assertFalse(c1.isEmpty());
        assertEquals(8, c1.size());

        assertTrue(c1.contains("MyProvider"));
        assertTrue(c1.contains("aaa.bbb.ccc.ddd"));
        assertTrue(c1.contains("Provider for testing"));
        assertTrue(c1.contains("1.0"));
        assertTrue(c1.contains("SomeClassName"));
        assertTrue(c1.contains("SHA-1"));
        assertTrue(c1.contains(p.getClass().getName()));
    }

    /*
     * Class under test for Object put(Object, Object)
     */
    public final void testPutObjectObject() {
        p.put("MessageDigest.SHA-1", "aaa.bbb.ccc.ddd");
        p.put("Type.Algorithm", "className");
        assertEquals("aaa.bbb.ccc.ddd", p.getProperty("MessageDigest.SHA-1")
                .trim());
        
        Set services = p.getServices();
        assertEquals(3, services.size());

        for (Iterator it = services.iterator(); it.hasNext();) {
            Provider.Service s = (Provider.Service)it.next();
            if ("Type".equals(s.getType()) &&
                    "Algorithm".equals(s.getAlgorithm()) &&
                    "className".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType()) &&
                    "SHA-1".equals(s.getAlgorithm()) &&
                    "aaa.bbb.ccc.ddd".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType()) &&
                    "abc".equals(s.getAlgorithm()) &&
                    "SomeClassName".equals(s.getClassName())) {
                continue;
            }
            fail("Incorrect service");
        }
    }

    /*
     * Class under test for Object remove(Object)
     */
    public final void testRemoveObject() {
        Object o = p.remove("MessageDigest.SHA-1");

        assertEquals("SomeClassName", o);
        assertNull(p.getProperty("MessageDigest.SHA-1"));
        assertEquals(1, p.getServices().size());
    }
 
    public final void testService1() {
        p.put("MessageDigest.SHA-1", "AnotherClassName");
        Provider.Service s = p.getService("MessageDigest", "SHA-1");

        assertEquals("AnotherClassName", s.getClassName());
    }

 /*
    public final void testService2() {
        Provider[] pp = Security.getProviders("MessageDigest.SHA-1");
        if (pp == null) {
            return;
        }
        Provider p2 = pp[0];
        String old = p2.getProperty("MessageDigest.SHA-1");
        try {
            p2.put("MessageDigest.SHA-1", "AnotherClassName");
            Provider.Service s = p2.getService("MessageDigest", "SHA-1");
            if (!"AnotherClassName".equals(s.getClassName())) {
                fail("Incorrect class name "+ s.getClassName());
            }
            try {
                s.newInstance(null);
                fail("No expected NoSuchAlgorithmException");
            } catch (NoSuchAlgorithmException e) {    
            }
        } finally {
            p2.put("MessageDigest.SHA-1", old);
        }
    }
*/
    //Regression for HARMONY-2760.
    public void testConstructor() {
		MyProvider myProvider = new MyProvider(null, 1, null);
		assertNull(myProvider.getName());
		assertNull(myProvider.getInfo());
		assertEquals("null", myProvider.getProperty("Provider.id name"));
		assertEquals("null", myProvider.getProperty("Provider.id info"));
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
