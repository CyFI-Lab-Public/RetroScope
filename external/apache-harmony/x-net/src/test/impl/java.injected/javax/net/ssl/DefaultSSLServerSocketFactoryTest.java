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
import java.net.InetAddress;
import java.net.SocketException;

import junit.framework.TestCase;


/**
 * Tests for <code>DefaultSSLServerSocketFactory</code> class constructors and methods.
 *  
 */
public class DefaultSSLServerSocketFactoryTest extends TestCase {

    /*
     * Class under test for ServerSocket createServerSocket(int)
     */
    public void testCreateServerSocketint() {
        DefaultSSLServerSocketFactory f = new DefaultSSLServerSocketFactory("ERROR");
        try {
            f.createServerSocket(0);
            fail("No expected SocketException");
        } catch (SocketException e) {
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for ServerSocket createServerSocket(int, int)
     */
    public void testCreateServerSocketintint() {
        DefaultSSLServerSocketFactory f = new DefaultSSLServerSocketFactory("ERROR");
        try {
            f.createServerSocket(0, 10);
            fail("No expected SocketException");
        } catch (SocketException e) {
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for ServerSocket createServerSocket(int, int, InetAddress)
     */
    public void testCreateServerSocketintintInetAddress() {
        DefaultSSLServerSocketFactory f = new DefaultSSLServerSocketFactory("ERROR");
        try {
            f.createServerSocket(0, 10, InetAddress.getLocalHost());
            fail("No expected SocketException");
        } catch (SocketException e) {
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    public void testGetDefaultCipherSuites() {
        DefaultSSLServerSocketFactory f = new DefaultSSLServerSocketFactory("ERROR");
        String[] res = f.getDefaultCipherSuites();
        if (res == null || res.length != 0) {
            fail("incorrect result");
        }
    }

    public void testGetSupportedCipherSuites() {
        DefaultSSLServerSocketFactory f = new DefaultSSLServerSocketFactory("ERROR");
        String[] res = f.getSupportedCipherSuites();
        if (res == null || res.length != 0) {
            fail("incorrect result");
        }
    }

}
