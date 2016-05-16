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

import java.security.SecureRandom;
import java.util.Arrays;

import javax.net.ssl.SSLPeerUnverifiedException;

import junit.framework.TestCase;

/**
 * Tests for <code>SSLSessionContextImp</code> constructor and methods
 *  
 */
public class SSLSessionImplTest extends TestCase {

    /*
     * Class under test for void SSLSessionImpl(CipherSuite, SecureRandom)
     */
    public void testSSLSessionImplCipherSuiteSecureRandom() {
        SSLSessionImpl session = new SSLSessionImpl(null, null);
        assertEquals(session.getCipherSuite(),
                CipherSuite.TLS_NULL_WITH_NULL_NULL.getName());

        session = new SSLSessionImpl(CipherSuite.TLS_RSA_WITH_NULL_MD5,
                new SecureRandom());
        session.protocol = ProtocolVersion.TLSv1;
        assertEquals("Incorrect protocol", "TLSv1", session.getProtocol());
        assertEquals("Incorrect cipher suite", session.getCipherSuite(),
                CipherSuite.TLS_RSA_WITH_NULL_MD5.getName());
        assertEquals("Incorrect id", 32, session.getId().length);
        assertTrue("Incorrect isValid", session.isValid());
        assertTrue("Incorrect isServer", session.isServer);
        long time = session.getCreationTime();
        assertTrue("Incorrect CreationTime", time <= System.currentTimeMillis());
        assertEquals("Incorrect LastAccessedTime", time, session.getLastAccessedTime());
        assertNull("Incorrect LocalCertificates", session.getLocalCertificates());
        assertNull("Incorrect LocalPrincipal", session.getLocalPrincipal());
        assertNull(session.getPeerHost());
        assertEquals(-1, session.getPeerPort());
        assertNull(session.getSessionContext());
        
        try {
            session.getPeerCertificateChain();
            fail("getPeerCertificateChain: No expected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        }
        
        try {
            session.getPeerCertificates();
            fail("getPeerCertificates: No expected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        }
        
        try {
            session.getPeerPrincipal();
            fail("getPeerPrincipal: No expected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        } 
    }

    public void testGetApplicationBufferSize() {
        assertEquals(SSLSessionImpl.NULL_SESSION.getApplicationBufferSize(),
                SSLRecordProtocol.MAX_DATA_LENGTH);
    }
    
    public void testGetPacketBufferSize() {
        assertEquals(SSLSessionImpl.NULL_SESSION.getPacketBufferSize(),
                SSLRecordProtocol.MAX_SSL_PACKET_SIZE);
    }    

    public void testInvalidate() {
        SSLSessionImpl session = new SSLSessionImpl(
                CipherSuite.TLS_RSA_WITH_NULL_MD5, new SecureRandom());
        session.invalidate();
        assertFalse("Incorrect isValid", session.isValid());

    }

    public void testSetPeer() {
        SSLSessionImpl session = new SSLSessionImpl(null);
        session.setPeer("someHost", 8080);
        assertEquals("someHost", session.getPeerHost());
        assertEquals(8080, session.getPeerPort());
    }


    public void testGetValue() {
        SSLSessionImpl session = new SSLSessionImpl(null);
        
        assertEquals(0, session.getValueNames().length);
        
        try {
            session.getValue(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {            
        }
        assertNull(session.getValue("abc"));
        
        try {
            session.removeValue(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {            
        }
        session.removeValue("abc");
        
        try {
            session.putValue(null, "1");
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {            
        }
        
        try {
            session.putValue("abc", null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {            
        }
        
        Object o = new Object();
        session.putValue("abc", o);
        assertSame(session.getValue("abc"), o);
        assertEquals("abc", session.getValueNames()[0]);
        
        session.removeValue("abc");
        assertNull(session.getValue("abc"));    
    }
    
    public void testClone() {
        SSLSessionImpl session1 = new SSLSessionImpl(
                CipherSuite.TLS_RSA_WITH_NULL_MD5, new SecureRandom());
        SSLSessionImpl session2 = (SSLSessionImpl)session1.clone();
        assertTrue(Arrays.equals(session1.getId(), session2.getId()));
    }
    
}