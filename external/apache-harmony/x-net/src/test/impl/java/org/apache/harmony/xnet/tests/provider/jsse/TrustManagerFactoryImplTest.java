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

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;

import org.apache.harmony.xnet.provider.jsse.TrustManagerFactoryImpl;
import org.apache.harmony.xnet.provider.jsse.TrustManagerImpl;
import junit.framework.TestCase;

/**
 * Tests for <code>TrustManagerFactoryImpl</code> constructor and methods
 *  
 */
public class TrustManagerFactoryImplTest extends TestCase {

    /*
     * Class under test for void engineInit(KeyStore)
     */
    public void testEngineInitKeyStore() throws Exception {
        TrustManagerFactoryImpl tmf = new TrustManagerFactoryImpl();
        tmf.engineInit((KeyStore) null);

        String def_keystore = System.getProperty("javax.net.ssl.trustStore");
        System.setProperty("javax.net.ssl.trustStore", "abc");
        try {
            tmf.engineInit((KeyStore) null);
            fail("No expected KeyStoreException");
        } catch (KeyStoreException e) {
        } finally {
            if (def_keystore == null) {
                System.clearProperty("javax.net.ssl.trustStore");
            } else {
                System.setProperty("javax.net.ssl.trustStore", def_keystore);
            }
        }
    }

    /*
     * Class under test for void engineInit(ManagerFactoryParameters)
     */
    public void testEngineInitManagerFactoryParameters() {
        TrustManagerFactoryImpl tmf = new TrustManagerFactoryImpl();

        try {
            tmf.engineInit((ManagerFactoryParameters) null);
            fail("No expected InvalidAlgorithmParameterException");
        } catch (InvalidAlgorithmParameterException e) {
        }
    }

    public void testEngineGetTrustManagers() throws Exception {
        TrustManagerFactoryImpl tmf = new TrustManagerFactoryImpl();
        try {
            tmf.engineGetTrustManagers();
            fail("No expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
        KeyStore ks;
        ks = KeyStore.getInstance("BKS");
        ks.load(null, null);
        tmf.engineInit(ks);

        TrustManager[] tma = tmf.engineGetTrustManagers();
        assertEquals("Incorrect array length", 1, tma.length);
        assertTrue("Incorrect KeyManager type",
                tma[0] instanceof TrustManagerImpl);
    }

}