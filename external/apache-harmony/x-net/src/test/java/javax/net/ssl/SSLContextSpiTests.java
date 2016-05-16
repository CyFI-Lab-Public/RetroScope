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

package javax.net.ssl;

import java.security.KeyManagementException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContextSpi;

import junit.framework.TestCase;


/**
 * Tests for <code>SSLContextSpi</code> class constructors and methods.
 * 
 */

public class SSLContextSpiTests extends TestCase {
    /**
     * Constructor for SSLContextSpiTests.
     * 
     * @param arg0
     */
    public SSLContextSpiTests(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>SSLContextSpi</code> constructor
     * Assertion: constructs SSLContextSpi
     */
    public void testSSLContextSpi01() throws KeyManagementException {
        SSLContextSpi sslConSpi = new MySSLContextSpi();
        try {
            sslConSpi.engineGetSocketFactory();
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }
        try {
            sslConSpi.engineGetServerSocketFactory();
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }
        try {
            sslConSpi.engineGetServerSessionContext();
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }
        try {
            sslConSpi.engineGetClientSessionContext();
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }       
        try {
            sslConSpi.engineCreateSSLEngine();
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }
        try {
            sslConSpi.engineCreateSSLEngine("host",1);
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertEquals("Incorrect message", "Not initialiazed", e.getMessage());
        }
        sslConSpi.engineInit(null, null, new SecureRandom());
        assertNull("Not null result", sslConSpi.engineGetSocketFactory());
        assertNull("Not null result", sslConSpi.engineGetServerSocketFactory());
        assertNotNull("Null result", sslConSpi.engineCreateSSLEngine());
        assertNotNull("Null result", sslConSpi.engineCreateSSLEngine("host",1));
        assertNull("Not null result", sslConSpi.engineGetServerSessionContext());
        assertNull("Not null result", sslConSpi.engineGetClientSessionContext());
    }
}
