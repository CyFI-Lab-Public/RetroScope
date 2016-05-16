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
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManagerFactorySpi;

import junit.framework.TestCase;


/**
 * Tests for <code>KeyManagerFactorySpi</code> class constructors and methods.
 * 
 */

public class KeyManagerFactorySpiTests extends TestCase {

    /**
     * Constructor for KeyManegerFactorySpiTests.
     * 
     * @param arg0
     */
    public KeyManagerFactorySpiTests(String arg0) {
        super(arg0);
    }

    /**
     * Test for <code>KeyManagerFactorySpi</code> constructor 
     * Assertion: constructs KeyManagerFactorySpi
     */
    public void testKeyManagerFactorySpi01()
            throws Exception {
        KeyManagerFactorySpi kmfSpi = new MyKeyManagerFactorySpi();
        assertNull(kmfSpi.engineGetKeyManagers());
        KeyStore kStore = null;
        ManagerFactoryParameters mfp = null;
        
        char[] pass = { 'a', 'b', 'c' };

        try {
            kmfSpi.engineInit(kStore, null);
            fail("KeyStoreException must be thrown");
        } catch (KeyStoreException e) {
        }
        try {
            kmfSpi.engineInit(kStore, pass);
            fail("UnrecoverableKeyException must be thrown");
        } catch (UnrecoverableKeyException e) {
        }
        try {
            kmfSpi.engineInit(mfp);
            fail("InvalidAlgorithmParameterException must be thrown");
        } catch (InvalidAlgorithmParameterException e) {
        }
        assertNull("getKeyManagers() should return null object", 
                kmfSpi.engineGetKeyManagers());
        
        try {
            kStore = KeyStore.getInstance(KeyStore.getDefaultType());
            kStore.load(null, null);            
        } catch (KeyStoreException e) {
            fail("default keystore type is not supported");
            return;
        }
        kmfSpi.engineInit(kStore, pass);

        mfp = new MyKeyManagerFactorySpi.Parameters(kStore, null);
        try {
            kmfSpi.engineInit(mfp);
            fail("InvalidAlgorithmParameterException must be thrown");
        } catch (InvalidAlgorithmParameterException e) {
        }
        mfp = new MyKeyManagerFactorySpi.Parameters(kStore, pass);
        kmfSpi.engineInit(mfp);
    }
}

