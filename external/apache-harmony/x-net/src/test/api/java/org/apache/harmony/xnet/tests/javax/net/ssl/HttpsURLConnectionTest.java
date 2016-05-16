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
import java.net.URL;
import java.security.cert.Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

import junit.framework.TestCase;


/**
 * Tests for <code>HttpsURLConnection</code> class constructors and methods.
 * 
 */
public class HttpsURLConnectionTest extends TestCase {

    public final void testGetPeerPrincipal() throws Exception {
        HttpsURLConnection con = new MyHttpsURLConnection(new URL(
                "http://foo.com"));
        try {
            con.getPeerPrincipal();
            fail("No expected SSLPeerUnverifiedException");
        } catch (SSLPeerUnverifiedException e) {
        }
    }

    public final void testGetLocalPrincipal() {
        HttpsURLConnection con = new MyHttpsURLConnection(null);
        if (con.getLocalPrincipal() != null) {
            fail("Non null result");
        }
    }

    public final void testSetDefaultHostnameVerifier() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public final void testSetHostnameVerifier() {
        HttpsURLConnection con = new MyHttpsURLConnection(null);
        try {
            con.setHostnameVerifier(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public final void testGetDefaultSSLSocketFactory() {
        SSLSocketFactory sf = HttpsURLConnection.getDefaultSSLSocketFactory();
        if (!sf.equals(SSLSocketFactory.getDefault())) {
            fail("incorrect DefaultSSLSocketFactory");
        }
    }

    public final void testGetSSLSocketFactory() {
        HttpsURLConnection con = new MyHttpsURLConnection(null);
        SSLSocketFactory sf = con.getSSLSocketFactory();
        if (!sf.equals(SSLSocketFactory.getDefault())) {
            fail("incorrect DefaultSSLSocketFactory");
        }
    }

    public final void testSetDefaultSSLSocketFactory() {
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public final void testSetSSLSocketFactory() {
        HttpsURLConnection con = new MyHttpsURLConnection(null);
        try {
            con.setSSLSocketFactory(null);
            fail("No expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}

class MyHttpsURLConnection extends HttpsURLConnection {

    public MyHttpsURLConnection(URL url) {
        super(url);
    }

    /*
     * @see javax.net.ssl.HttpsURLConnection#getCipherSuite()
     */
    @Override
    public String getCipherSuite() {
        return null;
    }

    /*
     * @see javax.net.ssl.HttpsURLConnection#getLocalCertificates()
     */
    @Override
    public Certificate[] getLocalCertificates() {
        return null;
    }

    /*
     * @see javax.net.ssl.HttpsURLConnection#getServerCertificates()
     */
    @Override
    public Certificate[] getServerCertificates()
            throws SSLPeerUnverifiedException {
        return null;
    }

    /*
     * @see java.net.HttpURLConnection#disconnect()
     */
    @Override
    public void disconnect() {
    }

    /*
     * @see java.net.HttpURLConnection#usingProxy()
     */
    @Override
    public boolean usingProxy() {
        return false;
    }

    /*
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect() throws IOException {
    }

}

