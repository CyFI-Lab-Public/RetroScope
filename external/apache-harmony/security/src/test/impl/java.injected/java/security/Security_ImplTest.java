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
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;


/**
 * Tests for <code>Security</code> constructor and methods
 * 
 */
public class Security_ImplTest extends TestCase {

    public final void testGetAlgorithmProperty() {
        assertNull("Incorrect result on null parameter", Security
                .getAlgorithmProperty(null, "MyService"));
        assertNull("Incorrect result on null parameter", Security
                .getAlgorithmProperty("MyAlgorithm", null));
        assertNull("Incorrect result (provider not added)", Security
                .getAlgorithmProperty("MyAlgorithm", "MyService"));

        Provider p = new MyProvider();
        Security.addProvider(p);
        try {
            assertEquals("Incorrect result (provider added)",
                    "SomeClassName", Security.getAlgorithmProperty("MyAlGoriThm", "MySerVicE"));    
        } finally { //clean up
            Security.removeProvider(p.getName());
        }
    }

    /*
     * Class under test for Provider[] getProviders()
     */
    public final void testGetProviders() {
        Provider[] providers;

        providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            // position is 1-based
            assertEquals("Incorrect provider number", i + 1, providers[i]
                    .getProviderNumber());
        }
    }

    /**
     * @tests java.security.Security#getProviders(String)
     */
    public final void test_getProvidersLjava_lang_String() {
        // Regression for Harmony-3154 (non-bug difference)
        try {
            Security.getProviders("AAA.BBB CCC");
            fail("AAA.BBB CCC: No expected InvalidParameterException");
        } catch (InvalidParameterException e) {
        }
    }

    /**
     * @tests java.security.Security#getProviders(Map)
     */
    public final void test_getProvidersLjava_util_Map() {
        // Regression for Harmony-3154 (non-bug difference)
        Map<String, String> m = new HashMap<String, String>();
        m.put("AAA.BBB CCC", "");
        m.put("AAA.BBB", "");
        try {
            Security.getProviders(m);
            fail("attribute value is empty string: No expected InvalidParameterException");
        } catch (InvalidParameterException  e) {    
        }
    }

    public final void testGetAlgorithms() {
        Set alg1;
        Set alg2;

        alg1 = Security.getAlgorithms("AAAAAAAAAAAAAAA");
        assertTrue("failed for non-existent service", alg1 != null);
        assertEquals("failed for non-existent service", 0, alg1.size());

        alg1 = Security.getAlgorithms("SecureRandom");
        alg2 = Security.getAlgorithms("seCuReranDom");
        assertEquals("different size", alg1.size(), alg2.size());
        assertTrue("different content", alg2.containsAll(alg1));

        Provider p = new MyProvider();

        try {
            Security.addProvider(p);
            alg1 = Security.getAlgorithms("MyService");
            assertEquals("failed for MyService", 1, alg1.size());
            assertTrue("failed for MyService", alg1.contains("MyAlgorithm"));
        } finally { //clean up
            Security.removeProvider(p.getName());
        }
    }

    @SuppressWarnings("serial")
    class MyProvider extends Provider {
        MyProvider() {
            super("MyProvider", 1.0, "Provider for testing");
            put("MessageDigest.SHA-1", "SomeClassName");
            put("MyService.MyAlgorithm", "SomeClassName");
            put("MyService.MyAlgorithm KeySize", "1024");
        }
    }

}
