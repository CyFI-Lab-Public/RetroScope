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

import java.security.Provider;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.harmony.security.tests.support.RandomImpl;

import junit.framework.TestCase;


/**
 * Tests for <code>Provider.Service</code> constructor and methods
 * 
 */
public class ProviderServiceTest extends TestCase {

	public void testService() {
		Provider p = new MyProvider();
		try {
			new Provider.Service(null, "type", "algorithm", 
					"className", null, null);
			fail("provider is null: No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
		try {
			new Provider.Service(p, null, "algorithm", 
					"className", null, null);
			fail("type is null: No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
		try {
			new Provider.Service(p, "type", null, 
					"className", null, null);
			fail("algorithm is null: No expected NullPointerException");
		} catch (NullPointerException e) {	
		}	
		try {
			new Provider.Service(p, "type", "algorithm", 
					null, null, null);
			fail("className is null: No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
		
		Provider.Service s = new Provider.Service(p,
                "type", "algorithm", "className", null, null);
		
        assertEquals("getType() failed", "type", s.getType());
        assertEquals("getAlgorithm() failed", "algorithm", s.getAlgorithm());
        assertSame("getProvider() failed", p, s.getProvider());
        assertEquals("getClassName() failed", "className", s.getClassName());
        assertTrue("supportsParameter() failed", s
                .supportsParameter(new Object()));
	}

	public void testGetAttribute() {
		Provider p = new MyProvider();
		Provider.Service s = new Provider.Service(p,
                "type", "algorithm", "className", null, null);
		try {
			s.getAttribute(null);
			fail("No expected NullPointerException");
		} catch (NullPointerException e) {	
		}
	
        assertNull("getAttribute(aaa) failed", s.getAttribute("aaa"));
		
		HashMap hm = new HashMap();
		hm.put("attribute", "value");
		hm.put("KeySize", "1024");
		hm.put("AAA", "BBB");	
		
		s = new Provider.Service(p, "type", "algorithm", "className", 
		 		null, hm);
        assertNull("getAttribute(bbb) failed", s.getAttribute("bbb"));
        assertEquals("getAttribute(attribute) failed", "value", s
                .getAttribute("attribute"));
        assertEquals("getAttribute(KeySize) failed", "1024", s
                .getAttribute("KeySize"));
	}
    
    public void testNewInstance() throws NoSuchAlgorithmException {
        Provider p = new MyProvider();
        Provider.Service s = new Provider.Service(p, "SecureRandom",
                "algorithm",
                "org.apache.harmony.security.tests.support.RandomImpl",
                null, null);

        Object o = s.newInstance(null);
        assertTrue("incorrect instance", o instanceof RandomImpl);
        
        try {
            o = s.newInstance(new Object());
            fail("No expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {
        }
    }
	
	class MyProvider extends Provider {
		MyProvider() {
			super("MyProvider", 1.0, "Provider for testing");
			put("MessageDigest.SHA-1", "SomeClassName");
		}
	}
}
