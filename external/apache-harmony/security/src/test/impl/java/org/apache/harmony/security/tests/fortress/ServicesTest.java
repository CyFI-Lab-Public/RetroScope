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

package org.apache.harmony.security.tests.fortress;

import java.security.Provider;

import org.apache.harmony.security.fortress.Services;
import junit.framework.TestCase;


/**
 *
 * Tests for Services
 */
public class ServicesTest extends TestCase {

	public void testInitServiceInfo() {
		Provider p = new MyProvider();
		Services.initServiceInfo(p);
		Provider ap = new AnotherProvider();
		Services.initServiceInfo(ap);
		
		Provider.Service serv = Services.getService("Service.ALGORITHM");	
		if (serv == null) {
			fail("Service is null");
		}
		if (serv.getProvider() != p ||
				! "org.apache.harmony.security.tests.fortress.SomeClass".equals(serv.getClassName())) {
			fail("Incorrect Service");
		}
		Services.updateServiceInfo(); // restore from registered providers
		serv = Services.getService("Service.ALGORITHM");
		if (serv != null) {
			fail("ServiceDescription not removed");
		}
	}

	public void testRefresh() {
		Provider p = new MyProvider();
		Services.updateServiceInfo();  //to make needRefresh = false;
		Services.initServiceInfo(p);
		Provider.Service serv = Services.getService("Service.ALGORITHM");
		Services.refresh();
		serv = Services.getService("Service.ALGORITHM");	
		if (serv == null) {
			fail("Service removed");
		}
		
		Services.setNeedRefresh();
		Services.refresh();
		serv = Services.getService("Service.ALGORITHM");
		if (serv != null) {
			fail("Service not removed");
		}
	}
		
	class AnotherProvider extends Provider {
		AnotherProvider() {
			super("MyProvider", 1.0, "Provider for testing");
			put("Service.Algorithm", "AnotherClassName");
		}
		
		AnotherProvider(String name, double version, String info) {
			super(name, version, info);
		}
	}

}
