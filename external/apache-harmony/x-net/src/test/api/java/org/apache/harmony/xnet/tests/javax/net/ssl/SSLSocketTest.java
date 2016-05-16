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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.HandshakeCompletedListener;

import junit.framework.TestCase;


/**
 * Tests for <code>SSLSocket</code> class constructors.
 *  
 */
public class SSLSocketTest extends TestCase {

    /*
     * Class under test for void SSLSocket()
     */
    public void testSSLSocket() {
        SSLSocket soc = new MySSLSocket();
        try {
            soc.close();
        } catch (IOException e) {
        }
    }

    /*
     * Class under test for void SSLSocket(String, int)
     */
    public void testSSLSocketStringint() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        SSLSocket soc = new MySSLSocket("localhost", ss.getLocalPort());
        ss.close();
        soc.close();
    }

    /*
     * Class under test for void SSLSocket(InetAddress, int)
     */
    public void testSSLSocketInetAddressint() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        SSLSocket soc = new MySSLSocket(InetAddress.getLocalHost(), ss
                .getLocalPort());
        ss.close();
        soc.close();
    }

    /*
     * Class under test for void SSLSocket(String, int, InetAddress, int)
     */
    public void testSSLSocketStringintInetAddressint() throws Exception {
        try {
            ServerSocket ss1 = new ServerSocket(0);
            ServerSocket ss2 = new ServerSocket(0);
            SSLSocket soc = new MySSLSocket("localhost", ss1.getLocalPort(),
                    InetAddress.getLocalHost(), ss2.getLocalPort());
            ss1.close();
            ss2.close();
            soc.close();
        } catch (IOException e) {
        }
    }

    /*
     * Class under test for void SSLSocket(InetAddress, int, InetAddress, int)
     */
    public void testSSLSocketInetAddressintInetAddressint() throws Exception {
        try {
            ServerSocket ss1 = new ServerSocket(0);
            ServerSocket ss2 = new ServerSocket(0);
            SSLSocket soc = new MySSLSocket(InetAddress.getLocalHost(), ss1
                    .getLocalPort(), InetAddress.getLocalHost(), ss2
                    .getLocalPort());
            ss1.close();
            ss2.close();
            soc.close();
        } catch (IOException e) {
        }
    }
}

class MySSLSocket extends SSLSocket {

    public MySSLSocket() {
        super();
    }

    public MySSLSocket(String host, int port) throws IOException,
            UnknownHostException {
        super(host, port);
    }

    protected MySSLSocket(InetAddress address, int port,
            InetAddress clientAddress, int clientPort) throws IOException {
        super(address, port, clientAddress, clientPort);
    }

    public MySSLSocket(InetAddress address, int port) throws IOException {
        super(address, port);
    }

    public MySSLSocket(String host, int port, InetAddress clientAddress,
            int clientPort) throws IOException, UnknownHostException {
        super(host, port, clientAddress, clientPort);
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return null;
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return null;
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
    }

    @Override
    public String[] getSupportedProtocols() {
        return null;
    }

    @Override
    public String[] getEnabledProtocols() {
        return null;
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
    }

    @Override
    public SSLSession getSession() {
        return null;
    }

    @Override
    public void addHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
    }

    @Override
    public void removeHandshakeCompletedListener(
            HandshakeCompletedListener listener) {
    }

    @Override
    public void startHandshake() throws IOException {
    }

    @Override
    public void setUseClientMode(boolean mode) {
    }

    @Override
    public boolean getUseClientMode() {
        return false;
    }

    @Override
    public void setNeedClientAuth(boolean need) {
    }

    @Override
    public boolean getNeedClientAuth() {
        return false;
    }

    @Override
    public void setWantClientAuth(boolean want) {
    }

    @Override
    public boolean getWantClientAuth() {
        return false;
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
    }

    @Override
    public boolean getEnableSessionCreation() {
        return false;
    }

    @Override
    public SSLParameters getSSLParameters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSSLParameters(SSLParameters sslP) {
        // TODO Auto-generated method stub

    }
}
