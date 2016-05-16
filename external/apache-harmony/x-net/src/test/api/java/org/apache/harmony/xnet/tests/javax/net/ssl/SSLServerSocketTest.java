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

import javax.net.ssl.SSLServerSocket;

import junit.framework.TestCase;


/**
 * Tests for <code>SSLServerSocket</code> class constructors.
 *  
 */
public class SSLServerSocketTest extends TestCase {

    /*
     * Class under test for void SSLServerSocket()
     */
    public void testSSLServerSocket() {
        try {
            new MySSLServerSocket();
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for void SSLServerSocket(int)
     */
    public void testSSLServerSocketint() {
        try {
            new MySSLServerSocket(0);
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for void SSLServerSocket(int, int)
     */
    public void testSSLServerSocketintint() {
        try {
            new MySSLServerSocket(0, 10);
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for void SSLServerSocket(int, int, InetAddress)
     */
    public void testSSLServerSocketintintInetAddress() {
        try {
            new MySSLServerSocket(0, 10, InetAddress.getLocalHost());
        } catch (IOException e) {
            fail(e.toString());
        }
    }

}

class MySSLServerSocket extends SSLServerSocket {

    protected MySSLServerSocket() throws IOException {
        super();
    }

    protected MySSLServerSocket(int port) throws IOException {
        super(port);
    }

    protected MySSLServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
    }

    protected MySSLServerSocket(int port, int backlog, InetAddress address)
            throws IOException {
        super(port, backlog, address);
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return null;
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return null;
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
    public void setUseClientMode(boolean mode) {
    }

    @Override
    public boolean getUseClientMode() {
        return false;
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
    }

    @Override
    public boolean getEnableSessionCreation() {
        return false;
    }
}

