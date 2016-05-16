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
import java.util.HashMap;

import junit.framework.TestCase;


/**
 * Tests for <code>Provider.Service</code> constructor and methods
 * 
 */
public class ProviderService_ImplTest extends TestCase {

	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
		Provider p = new MyProvider();
		Provider.Service s = new Provider.Service(p, "type", "algorithm",
                "className", null, null);
        assertEquals("first toString() failed",
                "Provider MyProvider Service type.algorithm className", 
                s.toString());
		
		HashMap hm = new HashMap();
		hm.put("attribute", "value");
		hm.put("KeySize", "1024");
		hm.put("AAA", "BBB");	
		
		s = new Provider.Service(p, "type", "algorithm", "className", 
		 		null, hm);
        assertTrue("second toString() failed", s.toString().startsWith(
                "Provider MyProvider Service type.algorithm className\n"
                        + "Attributes "));
	}
	
	class MyProvider extends Provider {
		MyProvider() {
			super("MyProvider", 1.0, "Provider for testing");
			put("MessageDigest.SHA-1", "SomeClassName");
		}
	}
}
