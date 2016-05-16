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
package java.security;

import java.net.URI;
import java.net.URISyntaxException;

import javax.security.auth.login.Configuration;

import junit.framework.TestCase;

public class URIParameterTest extends TestCase {

	private URIParameter uriParameter;
	private URI			 uri;

	/**
	 * @tests {@link java.security.URIParamter#constructor(java.net.URI)}
	 */
	public void test_Constructor() throws URISyntaxException {
		try {
			new URIParameter(null);
			fail("Should throw NPE");
		} catch (NullPointerException e) {
			// expected
		}
		
		assertTrue(uriParameter instanceof Policy.Parameters);
		assertTrue(uriParameter instanceof Configuration.Parameters);
	}

	/**
	 * @tests {@link java.security.URIParameter#getURI()}
	 */
	public void testGetURI() {
		URI u = uriParameter.getURI();
		assertEquals(uri, u);
		assertSame(uri, u);
	}

	/*
     * @see TestCase#setUp()
     */
	protected void setUp() throws Exception {
        super.setUp();
        uri = new URI("http://www.test.com");
        uriParameter = new URIParameter(uri);
	}
	
	/*
     * @see TestCase#tearDown()
     */
	protected void tearDown() throws Exception {
        super.tearDown();
        uriParameter = null;
        uri 		 = null;
    }
}
