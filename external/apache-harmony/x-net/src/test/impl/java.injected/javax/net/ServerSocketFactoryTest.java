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

/**
* @author Boris V. Kuznetsov
*/

package javax.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import junit.framework.TestCase;


/**
 * Tests for <code>ServerSocketFactory</code> class constructors and methods.
 */

public class ServerSocketFactoryTest extends TestCase {
    /*
     * Class under test for java.net.ServerSocket createServerSocket()
     */
    public final void testCreateServerSocket() {
        ServerSocketFactory sf = new MyServerSocketFactory();
        try {
            sf.createServerSocket();
            fail("No expected SocketException");
        } catch (SocketException e) {        
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for javax.net.ServerSocketFactory getDefault()
     */
    public final void testGetDefault() {
        ServerSocketFactory sf = ServerSocketFactory.getDefault();
        ServerSocket s;
        if (!(sf instanceof DefaultServerSocketFactory)) {
            fail("Incorrect class instance");
        }
        try {
            s = sf.createServerSocket(0);
            s.close();
        } catch (IOException e) {
        }
        try {
            s = sf.createServerSocket(0, 50);
            s.close();
        } catch (IOException e) {
        }
        try {
            s = sf.createServerSocket(0, 50, InetAddress.getLocalHost());
            s.close();
        } catch (IOException e) {
        } 
    }
}
class MyServerSocketFactory extends ServerSocketFactory {
    @Override
    public ServerSocket createServerSocket(int port) throws IOException, UnknownHostException {
        throw new IOException();
    }
    
    @Override
    public ServerSocket createServerSocket(int port, int backlog)
            throws IOException, UnknownHostException {
        throw new IOException();
    }
    
    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        throw new IOException();
     }
}
