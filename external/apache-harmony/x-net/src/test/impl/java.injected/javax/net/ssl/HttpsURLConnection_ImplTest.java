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

package javax.net.ssl;

import java.io.IOException;
import java.net.URL;
import java.security.cert.Certificate;

import junit.framework.TestCase;

/**
 * Tests for <code>HttpsURLConnection</code> class constructors and methods.
 * 
 */
public class HttpsURLConnection_ImplTest extends TestCase {

    public final void testGetDefaultHostnameVerifier() {
        
        HostnameVerifier ver = HttpsURLConnection.getDefaultHostnameVerifier();
        if (!(ver instanceof DefaultHostnameVerifier)) {
            fail("Incorrect instance");
        }
        if (ver.verify("localhost", null)) {
            fail("connection should not be permitted");
        }
    }

    public final void testGetHostnameVerifier() {
        
        HttpsURLConnection con = new MyHttpsURLConnection(null);
        HostnameVerifier ver = con.getHostnameVerifier();
        if (!(ver instanceof DefaultHostnameVerifier)) {
            fail("Incorrect instance");
        }
        if (ver.verify("localhost", null)) {
            fail("connection should not be permitted");
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
