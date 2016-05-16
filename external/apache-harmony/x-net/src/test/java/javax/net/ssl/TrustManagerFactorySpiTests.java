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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.TrustManagerFactorySpi;

import junit.framework.TestCase;

/**
 * Tests for <code>TrustManagerFactorySpi</code> class constructors and
 * methods.
 * 
 */

public class TrustManagerFactorySpiTests extends TestCase {
    /**
     * Constructor for TrustManegerFactorySpiTests.
     * 
     * @param arg0
     */
    public TrustManagerFactorySpiTests(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>TrustManagerFactorySpi</code> constructor 
     * Assertion: constructs TrustManagerFactorySpi
     */
    public void testTrustManagerFactorySpi01() throws Exception {
        TrustManagerFactorySpi kmfSpi = new MyTrustManagerFactorySpi();        
        assertNull("Not null results", kmfSpi.engineGetTrustManagers());
        KeyStore kStore = null;
        ManagerFactoryParameters mfp = null;
        
        try {
            kmfSpi.engineInit(kStore);
            fail("KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        }
        try {
            kmfSpi.engineInit(mfp);
            fail("InvalidAlgorithmParameterException must be thrown");
        } catch (InvalidAlgorithmParameterException e) {
        }
        assertNull("getTrustManagers() should return null object", 
                kmfSpi.engineGetTrustManagers());     
        
        try {
            kStore = KeyStore.getInstance(KeyStore.getDefaultType());
            kStore.load(null, null);            
        } catch (KeyStoreException e) {
            fail("default keystore is not supported");
            return;
        }
        kmfSpi.engineInit(kStore);
        mfp = new MyTrustManagerFactorySpi.Parameters(null);
        try {
            kmfSpi.engineInit(mfp);
            fail("RuntimeException must be thrown");
        } catch (RuntimeException e) {
            assertTrue("Incorrect exception", e.getCause() instanceof KeyStoreException);
        }
        mfp = new MyTrustManagerFactorySpi.Parameters(kStore);
        kmfSpi.engineInit(mfp);
    }
}
