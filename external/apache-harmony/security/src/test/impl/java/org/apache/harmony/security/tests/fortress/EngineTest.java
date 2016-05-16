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

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import org.apache.harmony.security.fortress.Engine;
import org.apache.harmony.security.fortress.Services;
import junit.framework.TestCase;


/**
 *
 * Tests for Engine
 */
public class EngineTest extends TestCase {

	protected void tearDown() throws Exception {
		super.tearDown();
		Services.updateServiceInfo();
	}
	
	/*
	 * Class under test for SpiImpl getInstance(String, Object)
	 */
	public void testGetInstanceStringObject1() throws Exception {
		Provider p = new MyProvider();
		Services.initServiceInfo(p);
		Engine engine = new Engine("Service");
		
		
		engine.getInstance("AlGOrItHM", null);

		if (engine.provider != p) {
			fail("Incorrect provider");
		}
		if (!(engine.spi instanceof SomeClass)) {
			fail("Incorrect spi");
		}
	}

    /*
	 * Class under test for SpiImpl getInstance(String, Object)
	 */
	public void testGetInstanceStringObject2() {
        Provider p = new MyProvider();
        Services.initServiceInfo(p);
        Engine engine = new Engine("Service");
        try {
            engine.getInstance(null, null);
            fail("No expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {}
    }

    /*
     * Class under test for SpiImpl getInstance(String, Provider, Object)
     */
    public void testGetInstanceStringProviderObject1() throws Exception {
        Provider p = new MyProvider();
        Services.initServiceInfo(p);
        Engine engine = new Engine("Service");

        engine.getInstance("AlGOrItHM", p, null);

        if (engine.provider != p) {
            fail("Incorrect provider");
        }
        if (!(engine.spi instanceof SomeClass)) {
            fail("Incorrect spi");
        }
    }

    /*
     * Class under test for SpiImpl getInstance(String, Provider, Object)
     */
    public void testGetInstanceStringProviderObject2() {
        Provider p = new MyProvider();
        Services.initServiceInfo(p);
        Engine engine = new Engine("Service");

        try {
            engine.getInstance(null, p, null);
            fail("No expected NoSuchAlgorithmException");
        } catch (NoSuchAlgorithmException e) {}
    }

    public void testGetInstanceStringProvider1() throws Exception {
        Provider p = Security.getProvider("SUN");
        if (p == null) {
            return;
        }
        Engine engine = new Engine("CertStore");
        engine.getInstance("Collection", p,
                new java.security.cert.CollectionCertStoreParameters());
    }
	
	public void testGetInstanceStringProvider2() throws Exception {
		Provider p = Security.getProvider("SUN");
		if (p == null) {
			return;
		}
        
		Engine engine = new Engine("CertStore");
        engine.getInstance("Collection",
                new java.security.cert.CollectionCertStoreParameters());
	}
	

}
