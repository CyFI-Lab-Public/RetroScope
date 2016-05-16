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
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import junit.framework.TestCase;


/**
 * Tests for <code>SocketFactory</code> class methods.
 */
public class SocketFactoryTest extends TestCase {

    /*
     * Class under test for java.net.Socket createSocket()
     */
    public final void testCreateSocket() {
        SocketFactory sf = new MySocketFactory();
        try {
            sf.createSocket();
            fail("No expected SocketException");
        } catch (SocketException e) {        
        } catch (IOException e) {
            fail(e.toString());
        }
    }

    /*
     * Class under test for javax.net.SocketFactory getDefault()
     */
    public final void testGetDefault() {
        SocketFactory sf = SocketFactory.getDefault();
        Socket s;
        if (!(sf instanceof DefaultSocketFactory)) {
            fail("Incorrect class instance");
        }
        try {
            s = sf.createSocket("localhost", 8082);
            s.close();
        } catch (IOException e) {
        }
        try {
            s = sf.createSocket("localhost", 8081, InetAddress.getLocalHost(), 8082);
            s.close();
        } catch (IOException e) {
        }
        try {
            s = sf.createSocket(InetAddress.getLocalHost(), 8081);
            s.close();
        } catch (IOException e) {
        } 
        try {
            s = sf.createSocket(InetAddress.getLocalHost(), 8081, InetAddress.getLocalHost(), 8082);
            s.close();
        } catch (IOException e) {
        }     
    }
}
class MySocketFactory extends SocketFactory {
    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        throw new IOException();
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException, UnknownHostException {
        throw new IOException();
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        throw new IOException();
     }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        throw new IOException();
     }

}
