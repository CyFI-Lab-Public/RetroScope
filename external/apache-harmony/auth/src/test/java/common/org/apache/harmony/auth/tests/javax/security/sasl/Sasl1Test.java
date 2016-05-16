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
* @author Vera Y. Petrashkova
*/

package org.apache.harmony.auth.tests.javax.security.sasl;

import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslServerFactory;

import junit.framework.TestCase;

/**
 * Test for Sasl class
 */
public class Sasl1Test extends TestCase {

    private Provider [] provs;
    private boolean initProvs;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!initProvs) {
            provs = Security.getProviders();
            initProvs = true;
        }
        if (provs != null) {
            for (Provider element : provs) {
                Security.removeProvider(element.getName());
            }
        }
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (provs != null) {
            for (int i = 0; i < provs.length; i++) {
                Security.insertProviderAt(provs[i], (i+1));
            }
        }
    }

    /**
     * Test for <code>getSaslClientFactories()</code> method 
     * 
     * Assertion:
     * returns enumeration without any element
     * 
     * All providers are previously removed.
     */
    public void testGetClient() {    
        Enumeration<SaslClientFactory> en = Sasl.getSaslClientFactories();
        assertNotNull("List of SaslClientFactories should not be null", en);
        assertFalse("List of SaslClientFactories should not haves elements", en
                .hasMoreElements());
    }

    /**
     * Test for <code>getSaslServerFactories()</code> method 
     * 
     * Assertion:
     * returns enumeration without any element
     * 
     * All providers are previously removed.
     */
    public void testGetSertver() {
        Enumeration<SaslServerFactory> en = Sasl.getSaslServerFactories();
        assertNotNull("List of SaslServerFactories should not be null", en);
        assertFalse("List of SaslServerFactories should not have elements", en
                .hasMoreElements());
    }
}
