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

import java.util.Enumeration;
import java.security.SecureRandom;

import javax.net.ssl.SSLSession;

import junit.framework.TestCase;

/**
 * Tests for <code>SSLSessionContextImp</code> constructor and methods
 *  
 */
public class SSLSessionContextImplTest extends TestCase {

    public void testSSLSessionContextImpl() {
        SecureRandom sr = new SecureRandom();
        SSLSessionImpl ses1 = new SSLSessionImpl(
                CipherSuite.TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA, sr);
        SSLSessionImpl ses2 = new SSLSessionImpl(
                CipherSuite.TLS_DH_anon_WITH_3DES_EDE_CBC_SHA, sr);
        SSLSessionImpl ses3 = new SSLSessionImpl(
                CipherSuite.TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5, sr);

        SSLSessionContextImpl context = new SSLSessionContextImpl();
        context.putSession(ses1);
        context.putSession(ses2);
        context.putSession(ses3);
        
        for (Enumeration en = context.getIds(); en.hasMoreElements();) {
            byte[] id = (byte[])en.nextElement();
            assertTrue(context.getSession(id) != null);            
        }

        SSLSession ses = context.getSession(ses1.getId());
        assertSame(ses1, ses);

        ses = context.getSession(ses3.getId());
        assertSame(ses3, ses);
    }

    public void testGetSessionCacheSize() {
        SSLSessionContextImpl context = new SSLSessionContextImpl();
        assertEquals(0, context.getSessionCacheSize());

        context.setSessionCacheSize(100);
        assertEquals(100, context.getSessionCacheSize());
        
        try {
            context.setSessionCacheSize(-1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetSessionTimeout() {
        SSLSessionContextImpl context = new SSLSessionContextImpl();
        assertEquals(0, context.getSessionTimeout());

        context.setSessionTimeout(1000);
        assertEquals(1000, context.getSessionTimeout());
        
        try {
            context.setSessionTimeout(-1);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

}