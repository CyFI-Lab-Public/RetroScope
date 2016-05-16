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

package org.apache.harmony.xnet.tests.provider.jsse;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.KeyManager;

import org.apache.harmony.xnet.provider.jsse.KeyManagerFactoryImpl;
import org.apache.harmony.xnet.provider.jsse.KeyManagerImpl;
import org.apache.harmony.xnet.provider.jsse.TrustManagerImpl;
import junit.framework.TestCase;

/**
 * Tests for <code>KeyManagerFactoryImpl</code> constructor and methods
 *  
 */
public class KeyManagerFactoryImplTest extends TestCase {

    /*
     * Class under test for void engineInit(KeyStore, char[])
     */
    public void testEngineInitKeyStorecharArray() throws Exception {
        KeyManagerFactoryImpl kmf = new KeyManagerFactoryImpl();
        kmf.engineInit(null, null);

        String def_keystore = System.getProperty("javax.net.ssl.keyStore");
        try {
            System.setProperty("javax.net.ssl.keyStore", "abc");
            kmf.engineInit(null, null);
            fail("No expected KeyStoreException");
        } catch (KeyStoreException e) {
        } finally {
            if (def_keystore == null) {
                 System.clearProperty("javax.net.ssl.keyStore");
            } else {
                System.setProperty("javax.net.ssl.keyStore", def_keystore);
            }
        }
      
    }

    /*
     * Class under test for void engineInit(ManagerFactoryParameters)
     */
    public void testEngineInitManagerFactoryParameters() {
        KeyManagerFactoryImpl kmf = new KeyManagerFactoryImpl();
        try {
            kmf.engineInit(null);
            fail("No expected InvalidAlgorithmParameterException");
        } catch (InvalidAlgorithmParameterException e) {
            // expected
        }
    }

    public void testEngineGetKeyManagers() throws Exception {
        KeyManagerFactoryImpl kmf = new KeyManagerFactoryImpl();
        try {
            kmf.engineGetKeyManagers();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        KeyStore ks;
        ks = KeyStore.getInstance("BKS");
        ks.load(null, null);
        kmf.engineInit(ks, null);

        KeyManager[] kma = kmf.engineGetKeyManagers();
        assertEquals("Incorrect array length", 1, kma.length);
        assertTrue("Incorrect KeyManager type",
                kma[0] instanceof KeyManagerImpl);
    }

}