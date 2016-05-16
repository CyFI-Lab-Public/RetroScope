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

package org.apache.harmony.xnet.provider.jsse;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyStore;

import junit.framework.TestCase;

/**
 * Tests for <code>KeyManagerImpl</code> constructor and methods
 *  
 */
public class KeyManagerImplTest extends TestCase {

    public void testKeyManagerImpl1() throws Exception {
        KeyStore ks = null;
        ks = KeyStore.getInstance("BKS");
        ks.load(null, null);

        KeyManagerImpl km = new KeyManagerImpl(ks, new char[0]);
        String[] keyType = {"RSA", "DSA"};
        String al = km.chooseClientAlias(keyType, null, new Socket());
        assertNull(al);
        
        al = km.chooseEngineClientAlias(keyType, null, new SSLEngineImpl(null));
        assertNull(al);
        
        al = km.chooseEngineServerAlias("RSA", null, new SSLEngineImpl(null));
        assertNull(al);
        
        al = km.chooseServerAlias("RSA", null, new Socket());
        assertNull(al);
        
        assertNull(km.getClientAliases("RSA", null));
        
        assertNull(km.getServerAliases("RSA", null));
        
        assertNull(km.getCertificateChain("alias"));
        assertNull(km.getPrivateKey("alias"));
    }
    
    public void testKeyManagerImpl2() throws Exception {
        
        KeyStore ks = JSSETestData.getKeyStore();
        char[] pwd = JSSETestData.KS_PASSWORD;
        
        KeyManagerImpl km = new KeyManagerImpl(ks, pwd);
        String[] keyType = { "RSA", "DSA" };
        String al = km.chooseClientAlias(keyType, null, new Socket());
        assertEquals("ssl_test_store", al);

        al = km.chooseEngineClientAlias(keyType, null, new SSLEngineImpl(null));
        assertEquals("ssl_test_store", al);

        al = km.chooseEngineServerAlias("RSA", null, new SSLEngineImpl(null));
        assertEquals("ssl_test_store", al);

        al = km.chooseServerAlias("RSA", null, new Socket());
        assertEquals("ssl_test_store", al);

        assertTrue(km.getCertificateChain("ssl_test_store") != null);
        assertTrue(km.getPrivateKey("ssl_test_store") != null);
    }

}
