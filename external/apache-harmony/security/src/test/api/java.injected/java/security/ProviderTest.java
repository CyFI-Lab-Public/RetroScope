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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.harmony.security.tests.support.SpiEngUtils;
import org.apache.harmony.security.tests.support.TestUtils;

import tests.support.resource.Support_Resources;


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
     * Class under test for void load(InputStream)
     */
    public final void testLoadInputStream() throws IOException {

        p.load(Support_Resources
                .getResourceStream("java/security/Provider.prop.dat"));    

        if (!"value 1".equals(p.getProperty("Property 1").trim()) ||
                !"className".equals(p.getProperty("serviceName.algName").trim()) ||    
                !"attrValue".equals(p.getProperty("serviceName.algName attrName").trim()) ||
                !"standardName".equals(p.getProperty("Alg.Alias.engineClassName.aliasName").trim()) ||
                !String.valueOf(p.getName()).equals(p.getProperty("Provider.id name").trim()) ||
                !String.valueOf(p.getVersion()).equals(p.getProperty("Provider.id version").trim()) ||
                !String.valueOf(p.getInfo()).equals(p.getProperty("Provider.id info").trim()) ||
                !p.getClass().getName().equals(p.getProperty("Provider.id className").trim()) ||
                !"SomeClassName".equals(p.getProperty("MessageDigest.SHA-1").trim()) ) {
            fail("Incorrect property value");
        }
    }

    public final void testGetService() {
        try { 
            p.getService(null, "algorithm");
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
        try { 
            p.getService("type", null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
        
        Provider.Service s = new Provider.Service(p, "Type", "Algorithm",
                "className", null, null);
        p.putService(s);
        
        if (p.getService("Type", "AlgoRithM") != s) {
            fail("Case 1. getService() failed");
        }
        
        Provider.Service s1 = p.getService("MessageDigest", "AbC");
        if (s1 == null) {
            fail("Case 2. getService() failed");            
        }
        
        s = new Provider.Service(p, "MessageDigest", "SHA-1",
                "className", null, null);
        p.putService(s);
        if (s1 == p.getService("MessageDigest", "SHA-1")) {
            fail("Case 3. getService() failed");
        }
        
        if (p.getService("MessageDigest", "SHA1") == null) {
            fail("Case 4. getService() failed");
        }
    }

    public final void testGetServices() {
        Provider.Service s = new Provider.Service(p, "Type", "Algorithm",
                "className", null, null);

        // incomplete services should be removed
        p.put("serv.alg", "aaaaaaaaaaaaa");
        p.put("serv.alg KeySize", "11111");
        p.put("serv1.alg1 KeySize", "222222");
        p.remove("serv.alg");
        
        p.putService(s);
        Set services = p.getServices();
        if (services.size() != 3) {
            fail("incorrect size");
        }
        for (Iterator it = services.iterator(); it.hasNext();) {
            s = (Provider.Service)it.next();
            if ("Type".equals(s.getType()) &&
                    "Algorithm".equals(s.getAlgorithm()) &&
                    "className".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType()) &&
                    "SHA-1".equals(s.getAlgorithm()) &&
                    "SomeClassName".equals(s.getClassName())) {
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

    public final void testPutService() {
        HashMap hm = new HashMap();
        hm.put("KeySize", "1024");
        hm.put("AAA", "BBB");
        Provider.Service s = new Provider.Service(p, "Type", "Algorithm",
                "className", null, hm);
        p.putService(s);
        if (s != p.getService("Type", "Algorithm")){
            fail("putService failed");
        }
        if (!"className".equals(p.getProperty("Type.Algorithm"))) {
            fail("incorrect className");
        }
        if (!"1024".equals(p.getProperty("Type.Algorithm KeySize"))) {
            fail("incorrect attribute");
        }    
    }

    public final void testRemoveService() {
        Provider.Service s = new Provider.Service(p, "Type", "Algorithm",
                "className", null, null);
        p.putService(s);
        p.removeService(s);
        Set services = p.getServices();
        if (services.size() != 2) {
            fail("incorrect size");
        }
        
        for (Iterator it = services.iterator(); it.hasNext();) {
            s = (Provider.Service)it.next();
            if ("MessageDigest".equals(s.getType()) &&
                    "SHA-1".equals(s.getAlgorithm()) &&
                    "SomeClassName".equals(s.getClassName())) {
                continue;
            }
            if ("MessageDigest".equals(s.getType()) &&
                    "abc".equals(s.getAlgorithm()) &&
                    "SomeClassName".equals(s.getClassName())) {
                continue;
            }
            fail("Incorrect service");
        }
        
        if (p.getProperty("Type.Algorithm") != null) {
            fail("incorrect property");
        }    
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
