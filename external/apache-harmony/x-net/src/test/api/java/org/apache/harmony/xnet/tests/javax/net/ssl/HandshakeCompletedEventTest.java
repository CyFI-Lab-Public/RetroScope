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

package org.apache.harmony.xnet.tests.javax.net.ssl;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.cert.Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.TestCase;

/**
 * Tests for <code>HandshakeCompletedEvent</code> class constructors and methods.
 */
public class HandshakeCompletedEventTest extends TestCase {

    int port;

    ServerSocket ss;

    SSLSocket soc;

    boolean noFreePort = false;
    boolean noSocket = false;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            ss = new ServerSocket(0);
            port = ss.getLocalPort();
        } catch (Exception e) {
            e.printStackTrace();
            noFreePort = true;
            return;
        }
        try {
            soc = (SSLSocket) sf.createSocket("localhost", port);
        } catch (IOException e) {
            noSocket = true;
        }

    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (ss != null) {
            ss.close();
        }
        if (soc != null) {
            soc.close();
        }
    }

    public final void testGetCipherSuite() {
        if (noFreePort || noSocket) {
            return;
        }
        SSLSession ses = new MySSLSession();

        HandshakeCompletedEvent event = new HandshakeCompletedEvent(soc, ses);
        
        assertEquals(event.getCipherSuite(), ses.getCipherSuite());
    }

    public final void testGetLocalCertificates() {
    	if (noFreePort || noSocket) {
            return;
        }
    	SSLSession ses = new MySSLSession();
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(soc, ses);

        Certificate[] certs = event.getLocalCertificates();
        Certificate[] ses_certs = ses.getLocalCertificates();
        if (certs == null && ses_certs == null) {
            return;
        }
        if (certs == null || ses_certs == null) {
            fail("incorrect LocalCertificates");
        }
        for (int i = 0; i < certs.length; i++) {
            if (certs[i] != ses_certs[i]) {
                fail("incorrect LocalCertificates");
            }
        }
    }

    public final void testGetPeerCertificates() {
    	if (noFreePort || noSocket) {
            return;
        }
    	SSLSession ses = new MySSLSession();
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(soc, ses);
        try {
            event.getPeerCertificates();
            fail("No excpected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        }
    }

    public final void testGetPeerCertificateChain() {
    	if (noFreePort || noSocket) {
            return;
        }
    	SSLSession ses = new MySSLSession();
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(soc, ses);
        try {
            event.getPeerCertificateChain();
            fail("No excpected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        }
    }

    public final void testHandshakeCompletedEvent() {
    	if (noFreePort || noSocket) {
            return;
        }
    	SSLSession ses = new MySSLSession();
        HandshakeCompletedEvent event = new HandshakeCompletedEvent(soc, ses);
        
        assertEquals(ses, event.getSession());
        assertEquals(soc, event.getSocket());
    }
}
