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

package org.apache.harmony.luni.tests.java.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.DatagramSocketImplFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import tests.support.Support_Configuration;
import tests.support.Support_PortManager;

public class DatagramSocketTest extends junit.framework.TestCase {

    java.net.DatagramSocket ds;

    java.net.DatagramPacket dp;

    DatagramSocket sds = null;

    String retval;

    String testString = "Test String";

    boolean interrupted;

    class DatagramServer extends Thread {

        public DatagramSocket ms;

        boolean running = true;

        public volatile byte[] rbuf = new byte[512];

        volatile DatagramPacket rdp = null;

        public void run() {
            try {
                while (running) {
                    try {
                        ms.receive(rdp);
                        // echo the packet back
                        ms.send(rdp);
                    } catch (java.io.InterruptedIOException e) {
                        Thread.yield();
                    }
                    ;
                }
                ;
            } catch (java.io.IOException e) {
                System.out.println("DatagramServer server failed: " + e);
            } finally {
                ms.close();
            }
        }

        public void stopServer() {
            running = false;
        }

        public DatagramServer(int aPort, InetAddress address)
                throws IOException {
            rbuf = new byte[512];
            rbuf[0] = -1;
            rdp = new DatagramPacket(rbuf, rbuf.length);
            ms = new DatagramSocket(aPort, address);
            ms.setSoTimeout(2000);
        }
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket()
     */
    public void test_Constructor() throws SocketException {
        new DatagramSocket();
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(int)
     */
    public void test_ConstructorI() throws SocketException {
        DatagramSocket ds = new DatagramSocket(0);
        ds.close();
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)
     */
    public void test_ConstructorILjava_net_InetAddress() throws IOException {
        DatagramSocket ds = new DatagramSocket(0, InetAddress.getLocalHost());
        assertTrue("Created socket with incorrect port", ds.getLocalPort() != 0);
        assertEquals("Created socket with incorrect address", InetAddress
                .getLocalHost(), ds.getLocalAddress());
    }

    /**
     * @tests java.net.DatagramSocket#close()
     */
    public void test_close() throws UnknownHostException, SocketException {
        DatagramSocket ds = new DatagramSocket(0);
        DatagramPacket dp = new DatagramPacket("Test String".getBytes(), 11,
                InetAddress.getLocalHost(), 0);
        ds.close();
        try {
            ds.send(dp);
            fail("Data sent after close");
        } catch (IOException e) {
            // Expected
        }
    }

    public void test_connectLjava_net_InetAddressI() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        InetAddress inetAddress = InetAddress.getLocalHost();
        ds.connect(inetAddress, 0);
        assertEquals("Incorrect InetAddress", inetAddress, ds.getInetAddress());
        assertEquals("Incorrect Port", 0, ds.getPort());
        ds.disconnect();

        ds = new java.net.DatagramSocket();
        inetAddress = InetAddress.getByName("FE80:0000:0000:0000:020D:60FF:FE0F:A776%4");
        int portNumber = Support_PortManager.getNextPortForUDP();
        ds.connect(inetAddress, portNumber);
        assertTrue("Incorrect InetAddress", ds.getInetAddress().equals(inetAddress));
        assertTrue("Incorrect Port", ds.getPort() == portNumber);
        ds.disconnect();

        // Create a connected datagram socket to test
        // PlainDatagramSocketImpl.peek()
        InetAddress localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket();
        int port = ds.getLocalPort();
        ds.connect(localHost, port);
        DatagramPacket send = new DatagramPacket(new byte[10], 10, localHost,
                port);
        ds.send(send);
        DatagramPacket receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("Wrong size: " + receive.getLength(),
                receive.getLength() == 10);
        assertTrue("Wrong receiver", receive.getAddress().equals(localHost));

        class DatagramServer extends Thread {

            public DatagramSocket ms;

            boolean running = true;

            public byte[] rbuf = new byte[512];

            DatagramPacket rdp = null;

            public void run() {
                try {
                    while (running) {
                        try {
                            ms.receive(rdp);
                            // echo the packet back
                            ms.send(rdp);
                        } catch (java.io.InterruptedIOException e) {
                            Thread.yield();
                        }
                        ;
                    }
                    ;
                } catch (java.io.IOException e) {
                    System.out.println("Multicast server failed: " + e);
                } finally {
                    ms.close();
                }
            }

            public void stopServer() {
                running = false;
            }

            public DatagramServer(int aPort, InetAddress address)
                    throws java.io.IOException {
                rbuf = new byte[512];
                rbuf[0] = -1;
                rdp = new DatagramPacket(rbuf, rbuf.length);
                ms = new DatagramSocket(aPort, address);
                ms.setSoTimeout(2000);
            }
        }

        // validate that we get the PortUnreachable exception if we try to
        // send a dgram to a server that is not running and then do a recv
        try {
            ds = new java.net.DatagramSocket();
            inetAddress = InetAddress.getLocalHost();
            portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            send = new DatagramPacket(new byte[10], 10);
            ds.send(send);
            receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(10000);
            ds.receive(receive);
            ds.close();
            fail("No PortUnreachableException when connected at native level on recv ");
        } catch (PortUnreachableException e) {
            // Expected
        }

        // validate that we can send/receive with datagram sockets connected at
        // the native level
        DatagramServer server = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        DatagramSocket ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(localHost, serverPortNumber);

        final byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
        send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("Wrong size data received: " + receive.getLength(), receive
                .getLength() == sendBytes.length);
        assertTrue("Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("Wrong receiver:" + receive.getAddress() + ":" + localHost,
                receive.getAddress().equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can disconnect
        try {
            ds = new java.net.DatagramSocket();
            inetAddress = InetAddress.getLocalHost();
            portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            ds.disconnect();
            ds.close();
        } catch (PortUnreachableException e) {
            // Expected
        }

        // validate that once connected we cannot send to another address
        try {
            ds = new java.net.DatagramSocket();
            inetAddress = InetAddress.getLocalHost();
            portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
            send = new DatagramPacket(new byte[10], 10, inetAddress,
                    portNumber + 1);
            ds.send(send);
            ds.close();
            fail("No Exception when trying to send to a different address on a connected socket ");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // validate that we can connect, then disconnect, then connect then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(localHost, serverPortNumber + 1);
        ds.disconnect();
        ds.connect(localHost, serverPortNumber);

        send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("connect/disconnect/connect - Wrong size data received: "
                + receive.getLength(), receive.getLength() == sendBytes.length);
        assertTrue("connect/disconnect/connect - Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("connect/disconnect/connect - Wrong receiver:"
                + receive.getAddress() + ":" + localHost, receive.getAddress()
                .equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect/disconnect then send/recv to any address
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(localHost, serverPortNumber + 1);
        ds.disconnect();

        send = new DatagramPacket(sendBytes, sendBytes.length, localHost,
                serverPortNumber);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("connect/disconnect - Wrong size data received: "
                + receive.getLength(), receive.getLength() == sendBytes.length);
        assertTrue("connect/disconnect - Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("connect/disconnect - Wrong receiver:"
                + receive.getAddress() + ":" + localHost, receive.getAddress()
                .equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect on an allready connected socket and then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(localHost, serverPortNumber + 1);
        ds.connect(localHost, serverPortNumber);

        send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("connect/connect - Wrong size data received: "
                + receive.getLength(), receive.getLength() == sendBytes.length);
        assertTrue("connect/connect - Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("connect/connect - Wrong receiver:" + receive.getAddress()
                + ":" + localHost, receive.getAddress().equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // test for when we fail to connect at the native level. Even though we
        // fail at the native level there is no way to return an exception so
        // there should be no exception
        ds = new java.net.DatagramSocket();
        byte[] addressBytes = { 0, 0, 0, 0 };
        inetAddress = InetAddress.getByAddress(addressBytes);
        portNumber = Support_PortManager.getNextPortForUDP();
        ds.connect(inetAddress, portNumber);

        if ("true".equals(System.getProperty("run.ipv6tests"))) {
            System.out
                    .println("Running test_connectLjava_net_InetAddressI(DatagramSocketTest) with IPv6 address");

            ds = new java.net.DatagramSocket();
            byte[] addressTestBytes = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0 };
            inetAddress = InetAddress.getByAddress(addressTestBytes);
            portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(inetAddress, portNumber);
        }
    }

    public void test_disconnect() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        InetAddress inetAddress = InetAddress.getLocalHost();
        ds.connect(inetAddress, 0);
        ds.disconnect();
        assertNull("Incorrect InetAddress", ds.getInetAddress());
        assertEquals("Incorrect Port", -1, ds.getPort());

        ds = new DatagramSocket();
        inetAddress = InetAddress.getByName("FE80:0000:0000:0000:020D:60FF:FE0F:A776%4");
        ds.connect(inetAddress, 0);
        ds.disconnect();
        assertNull("Incorrect InetAddress", ds.getInetAddress());
        assertEquals("Incorrect Port", -1, ds.getPort());
    }

    /**
     * @tests java.net.DatagramSocket#getInetAddress()
     */
    public void test_getInetAddress() {
        assertTrue("Used to test", true);
    }

    public void test_getLocalAddress() throws Exception {
        // Test for method java.net.InetAddress
        // java.net.DatagramSocket.getLocalAddress()
        int portNumber = Support_PortManager.getNextPortForUDP();
        InetAddress local = InetAddress.getLocalHost();
        ds = new java.net.DatagramSocket(portNumber, local);
        assertEquals(InetAddress.getByName(InetAddress.getLocalHost().getHostName()), ds.getLocalAddress());

        // now check behavior when the ANY address is returned
        DatagramSocket s = new DatagramSocket(0);
        assertTrue("ANY address not IPv6: " + s.getLocalSocketAddress(), s.getLocalAddress() instanceof Inet6Address);
        s.close();
    }

    /**
     * @tests java.net.DatagramSocket#getLocalPort()
     */
    public void test_getLocalPort() throws SocketException {
        DatagramSocket ds = new DatagramSocket();
        assertTrue("Returned incorrect port", ds.getLocalPort() != 0);
    }

    /**
     * @tests java.net.DatagramSocket#getPort()
     */
    public void test_getPort() throws IOException {
        DatagramSocket theSocket = new DatagramSocket();
        assertEquals("Expected -1 for remote port as not connected", -1,
                theSocket.getPort());

        // Now connect the socket and validate that we get the right port
        int portNumber = 49152; // any valid port, even if it is unreachable
        theSocket.connect(InetAddress.getLocalHost(), portNumber);
        assertEquals("getPort returned wrong value", portNumber, theSocket
                .getPort());
    }

    public void test_getReceiveBufferSize() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setReceiveBufferSize(130);
        assertTrue("Incorrect buffer size", ds.getReceiveBufferSize() >= 130);
    }

    public void test_getSendBufferSize() throws Exception {
        int portNumber = Support_PortManager.getNextPortForUDP();
        ds = new java.net.DatagramSocket(portNumber);
        ds.setSendBufferSize(134);
        assertTrue("Incorrect buffer size", ds.getSendBufferSize() >= 134);
    }

    public void test_getSoTimeout() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(100);
        assertEquals("Returned incorrect timeout", 100, ds.getSoTimeout());
    }

    public void test_receiveLjava_net_DatagramPacket() throws IOException {
        // Test for method void
        // java.net.DatagramSocket.receive(java.net.DatagramPacket)

        receive_oversize_java_net_DatagramPacket();
        final int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGRcv implements Runnable {
            public void run() {
                InetAddress localHost = null;
                try {
                    localHost = InetAddress.getLocalHost();
                    Thread.sleep(1000);
                    DatagramSocket sds = new DatagramSocket(ports[1]);
                    DatagramPacket rdp = new DatagramPacket("Test String"
                            .getBytes(), 11, localHost, portNumber);
                    sds.send(rdp);
                    sds.close();
                } catch (Exception e) {
                    System.err.println("host " + localHost + " port "
                            + portNumber + " failed to send data: " + e);
                    e.printStackTrace();
                }
            }
        }

        try {
            new Thread(new TestDGRcv(), "DGSender").start();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(6000);
            byte rbuf[] = new byte[1000];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);

            ds.receive(rdp);
            ds.close();
            assertTrue("Send/Receive failed to return correct data: "
                    + new String(rbuf, 0, 11), new String(rbuf, 0, 11)
                    .equals("Test String"));
        } finally {
            ds.close();
        }

        try {
            interrupted = false;
            final DatagramSocket ds = new DatagramSocket();
            ds.setSoTimeout(12000);
            Runnable runnable = new Runnable() {
                public void run() {
                    try {
                        ds.receive(new DatagramPacket(new byte[1], 1));
                    } catch (InterruptedIOException e) {
                        interrupted = true;
                    } catch (IOException e) {
                    }
                }
            };
            Thread thread = new Thread(runnable, "DatagramSocket.receive1");
            thread.start();
            try {
                do {
                    Thread.sleep(500);
                } while (!thread.isAlive());
            } catch (InterruptedException e) {
            }
            ds.close();
            int c = 0;
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (interrupted) {
                    fail("received interrupt");
                }
                if (++c > 4) {
                    fail("read call did not exit");
                }
            } while (thread.isAlive());

            interrupted = false;
            int[] ports1 = Support_PortManager.getNextPortsForUDP(2);
            final int portNum = ports[0];
            final DatagramSocket ds2 = new DatagramSocket(ports[1]);
            ds2.setSoTimeout(12000);
            Runnable runnable2 = new Runnable() {
                public void run() {
                    try {
                        ds2.receive(new DatagramPacket(new byte[1], 1,
                                InetAddress.getLocalHost(), portNum));
                    } catch (InterruptedIOException e) {
                        interrupted = true;
                    } catch (IOException e) {
                    }
                }
            };
            Thread thread2 = new Thread(runnable2, "DatagramSocket.receive2");
            thread2.start();
            try {
                do {
                    Thread.sleep(500);
                } while (!thread2.isAlive());
            } catch (InterruptedException e) {
            }
            ds2.close();
            int c2 = 0;
            do {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                if (interrupted) {
                    fail("receive2 was interrupted");
                }
                if (++c2 > 4) {
                    fail("read2 call did not exit");
                }
            } while (thread2.isAlive());

            interrupted = false;
            DatagramSocket ds3 = new DatagramSocket();
            ds3.setSoTimeout(500);
            Date start = new Date();
            try {
                ds3.receive(new DatagramPacket(new byte[1], 1));
            } catch (InterruptedIOException e) {
                interrupted = true;
            }
            ds3.close();
            assertTrue("receive not interrupted", interrupted);
            int delay = (int) (new Date().getTime() - start.getTime());
            assertTrue("timeout too soon: " + delay, delay >= 490);
        } catch (IOException e) {
            fail("Unexpected IOException : " + e.getMessage());
        }

    }

    /**
     * Tests receive() method in various combinations with
     * DatagramPacket#getLength() and DatagramPacket#getLength(). This is
     * regression test for HARMONY-2276.
     *
     * @throws IOException
     *             if some I/O error occured
     */
    // public void test2276() throws IOException {
    // final String ADDRESS = "239.255.2.3";
    // final int PORT = Support_PortManager.getNextPortForUDP();
    // InetAddress group = InetAddress.getByName(ADDRESS);
    // MulticastSocket socket = new MulticastSocket(PORT);
    // byte[] recvData = new byte[100];
    // DatagramPacket recvDatagram = new DatagramPacket(recvData,
    // recvData.length);
    //
    // String message = "Hello, world!";
    // String longerMessage = message + " again.";
    // String veryLongMessage = longerMessage + " Forever!";
    //
    // socket.joinGroup(group);
    // socket.setSoTimeout(5000); // prevent eternal block in
    // // socket.receive()
    // // send & recieve packet
    // byte[] sendData = message.getBytes();
    // DatagramPacket sendDatagram = new DatagramPacket(sendData, 0,
    // sendData.length, group, PORT);
    // socket.send(sendDatagram);
    // socket.receive(recvDatagram);
    // String recvMessage = new String(recvData, 0, recvDatagram.getLength());
    // assertEquals(message, recvMessage);
    //
    // // send & receive longer packet
    // sendData = longerMessage.getBytes();
    // sendDatagram = new DatagramPacket(sendData, 0, sendData.length,
    // group, PORT);
    // socket.send(sendDatagram);
    // socket.receive(recvDatagram);
    // recvMessage = new String(recvData, 0, recvDatagram.getLength());
    // assertEquals(longerMessage, recvMessage);
    //
    // // tricky case, added to test compatibility with RI;
    // // depends on the previous test case
    // sendData = veryLongMessage.getBytes();
    // sendDatagram = new DatagramPacket(sendData, 0, sendData.length, group,
    // PORT);
    // socket.send(sendDatagram);
    // recvDatagram.setLength(recvDatagram.getLength()); // !!!
    // socket.receive(recvDatagram);
    // recvMessage = new String(recvData, 0, recvDatagram.getLength());
    // assertEquals(longerMessage, recvMessage);
    //
    // // tests if received packet is truncated after length was set to 1
    // sendData = message.getBytes();
    // sendDatagram = new DatagramPacket(sendData, 0, sendData.length,
    // group, PORT);
    // socket.send(sendDatagram);
    // recvDatagram.setLength(1);
    // socket.receive(recvDatagram);
    // assertEquals("Received message was not truncated", 1,
    // recvDatagram.getLength());
    // assertSame("Received message is invalid", sendData[0], recvData[0]);
    //
    // socket.leaveGroup(group);
    // socket.close();
    // }
    /**
     * @tests java.net.DatagramSocket#send(java.net.DatagramPacket)
     */
    public void test_sendLjava_net_DatagramPacket() throws Exception {
        // Test for method void
        // java.net.DatagramSocket.send(java.net.DatagramPacket)
        int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGSend implements Runnable {
            Thread pThread;

            public TestDGSend(Thread t) {
                pThread = t;
            }

            public void run() {
                try {
                    byte[] rbuf = new byte[1000];

                    sds = new DatagramSocket(portNumber);
                    DatagramPacket sdp = new DatagramPacket(rbuf, rbuf.length);
                    sds.setSoTimeout(6000);
                    sds.receive(sdp);
                    retval = new String(rbuf, 0, testString.length());
                    pThread.interrupt();
                } catch (java.io.InterruptedIOException e) {
                    System.out.println("Recv operation timed out");
                    pThread.interrupt();
                    ds.close();
                    return;
                } catch (Exception e) {
                    System.out
                            .println("Failed to establish Dgram server: " + e);
                }
            }
        }
        try {
            new Thread(new TestDGSend(Thread.currentThread()), "DGServer")
                    .start();
            ds = new java.net.DatagramSocket(ports[1]);
            dp = new DatagramPacket(testString.getBytes(), testString.length(),
                    InetAddress.getLocalHost(), portNumber);
            // Wait to allow send to occur
            try {
                Thread.sleep(500);
                ds.send(dp);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                ds.close();
                assertTrue("Incorrect data sent: " + retval, retval
                        .equals(testString));
            }
        } finally {
            ds.close();
        }
        // Regression for HARMONY-1118
        class testDatagramSocket extends DatagramSocket {
            public testDatagramSocket(DatagramSocketImpl impl) {
                super(impl);
            }
        }
        class testDatagramSocketImpl extends DatagramSocketImpl {
            protected void create() throws SocketException {
            }

            protected void bind(int arg0, InetAddress arg1)
                    throws SocketException {
            }

            protected void send(DatagramPacket arg0) throws IOException {
            }

            protected int peek(InetAddress arg0) throws IOException {
                return 0;
            }

            protected int peekData(DatagramPacket arg0) throws IOException {
                return 0;
            }

            protected void receive(DatagramPacket arg0) throws IOException {
            }

            protected void setTTL(byte arg0) throws IOException {
            }

            protected byte getTTL() throws IOException {
                return 0;
            }

            protected void setTimeToLive(int arg0) throws IOException {
            }

            protected int getTimeToLive() throws IOException {
                return 0;
            }

            protected void join(InetAddress arg0) throws IOException {
            }

            protected void leave(InetAddress arg0) throws IOException {
            }

            protected void joinGroup(SocketAddress arg0, NetworkInterface arg1)
                    throws IOException {
            }

            protected void leaveGroup(SocketAddress arg0, NetworkInterface arg1)
                    throws IOException {
            }

            protected void close() {
            }

            public void setOption(int arg0, Object arg1) throws SocketException {
            }

            public Object getOption(int arg0) throws SocketException {
                return null;
            }
        }

        // Regression test for Harmony-2938
        InetAddress i = InetAddress.getByName("127.0.0.1");
        DatagramSocket d = new DatagramSocket(0, i);
        try {
            d.send(new DatagramPacket(new byte[] { 1 }, 1));
            fail("should throw NPE.");
        } catch (NullPointerException e) {
            // expected;
        } finally {
            d.close();
        }

        // Regression test for Harmony-6413
        InetSocketAddress addr = InetSocketAddress.createUnresolved(
                "localhost", 0);
        try {
            DatagramPacket dp = new DatagramPacket(new byte[272], 3, addr);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /**
     * If the InetAddress of DatagramPacket is null, DatagramSocket.send(DatagramPacket)
     * should throw NullPointer Exception.
     * @tests java.net.DatagramSocket#send(java.net.DatagramPacket)
     */
    public void test_sendLjava_net_DatagramPacket2() throws IOException {
        int udp_port = 20000;
        int send_port = 23000;
        DatagramSocket udpSocket = new DatagramSocket(udp_port);
        byte[] data = {65};
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, null, send_port);
        try {
            udpSocket.send(sendPacket);
            fail("Should throw SocketException");
        } catch (NullPointerException e) {
          // Expected
        } finally {
            udpSocket.close();
        }

    }

    public void test_setSendBufferSizeI() throws Exception {
        int portNumber = Support_PortManager.getNextPortForUDP();
        ds = new java.net.DatagramSocket(portNumber);
        ds.setSendBufferSize(134);
        assertTrue("Incorrect buffer size", ds.getSendBufferSize() >= 134);
    }

    public void test_setReceiveBufferSizeI() throws Exception {
        int portNumber = Support_PortManager.getNextPortForUDP();
        ds = new java.net.DatagramSocket(portNumber);
        ds.setReceiveBufferSize(130);
        assertTrue("Incorrect buffer size", ds.getReceiveBufferSize() >= 130);
    }

    public void test_setSoTimeoutI() throws Exception {
        DatagramSocket ds = new DatagramSocket();
        ds.setSoTimeout(100);
        assertTrue("Set incorrect timeout", ds.getSoTimeout() >= 100);
    }

    public void test_ConstructorLjava_net_DatagramSocketImpl() {
        class SimpleTestDatagramSocket extends DatagramSocket {
            public SimpleTestDatagramSocket(DatagramSocketImpl impl) {
                super(impl);
            }
        }

        try {
            new SimpleTestDatagramSocket((DatagramSocketImpl) null);
            fail("exception expected");
        } catch (NullPointerException ex) {
            // expected
        }
    }

    /**
     * @tests java.net.DatagramSocket#DatagramSocket(java.net.SocketAddress)
     */
    public void test_ConstructorLjava_net_SocketAddress() throws Exception {
        class UnsupportedSocketAddress extends SocketAddress {

            public UnsupportedSocketAddress() {
            }
        }

        DatagramSocket ds = new DatagramSocket(new InetSocketAddress(
                InetAddress.getLocalHost(), 0));
        assertTrue(ds.getBroadcast());
        assertTrue("Created socket with incorrect port", ds.getLocalPort() != 0);
        assertEquals("Created socket with incorrect address", InetAddress
                .getLocalHost(), ds.getLocalAddress());

        try {
            ds = new java.net.DatagramSocket(new UnsupportedSocketAddress());
            fail("No exception when constructing datagramSocket with unsupported SocketAddress type");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // regression for HARMONY-894
        ds = new DatagramSocket((SocketAddress) null);
        assertTrue(ds.getBroadcast());
    }

    /**
     * @tests java.net.DatagramSocket#bind(java.net.SocketAddress)
     */
    public void test_bindLjava_net_SocketAddress() throws Exception {
        class mySocketAddress extends SocketAddress {

            public mySocketAddress() {
            }
        }

        DatagramServer server = null;

        // now create a socket that is not bound and then bind it
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int portNumber = ports[0];
        int serverPortNumber = ports[1];
        DatagramSocket theSocket = new DatagramSocket(new InetSocketAddress(
                InetAddress.getLocalHost(), portNumber));

        // validate that the localSocketAddress reflects the address we
        // bound to
        assertTrue(
                "Local address not correct after bind:"
                        + theSocket.getLocalSocketAddress().toString()
                        + "Expected: "
                        + (new InetSocketAddress(InetAddress.getLocalHost(),
                                portNumber)).toString(), theSocket
                        .getLocalSocketAddress().equals(
                                new InetSocketAddress(InetAddress
                                        .getLocalHost(), portNumber)));

        // now make sure that datagrams sent from this socket appear to come
        // from the address we bound to
        InetAddress localHost = InetAddress.getLocalHost();
        portNumber = ports[2];
        DatagramSocket ds = new DatagramSocket(null);
        ds.bind(new InetSocketAddress(localHost, portNumber));

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        ds.connect(new InetSocketAddress(localHost, serverPortNumber));

        byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
        DatagramPacket send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        Thread.sleep(1000);
        ds.close();
        assertTrue("Address in packet sent does not match address bound to:"
                + server.rdp.getAddress() + ":" + server.rdp.getPort() + ":"
                + localHost + ":" + portNumber, (server.rdp.getAddress()
                .equals(localHost))
                && (server.rdp.getPort() == portNumber));

        // validate if we pass in null that it picks an address for us and
        // all is ok
        theSocket = new DatagramSocket(null);
        theSocket.bind(null);
        assertNotNull("Bind with null did not work", theSocket
                .getLocalSocketAddress());
        theSocket.close();

        // now check the error conditions

        // Address we cannot bind to
        theSocket = new DatagramSocket(null);
        try {
            theSocket.bind(new InetSocketAddress(InetAddress
                    .getByAddress(Support_Configuration.nonLocalAddressBytes),
                    Support_PortManager.getNextPortForUDP()));
            fail("No exception when binding to bad address");
        } catch (SocketException ex) {
        }
        theSocket.close();

        // Address that we have allready bound to
        ports = Support_PortManager.getNextPortsForUDP(2);
        theSocket = new DatagramSocket(null);
        DatagramSocket theSocket2 = new DatagramSocket(ports[0]);
        try {
            InetSocketAddress theAddress = new InetSocketAddress(InetAddress
                    .getLocalHost(), ports[1]);
            theSocket.bind(theAddress);
            theSocket2.bind(theAddress);
            fail("No exception binding to address that is not available");
        } catch (SocketException ex) {
        }
        theSocket.close();
        theSocket2.close();

        // unsupported SocketAddress subclass
        theSocket = new DatagramSocket(null);
        try {
            theSocket.bind(new mySocketAddress());
            fail("No exception when binding using unsupported SocketAddress subclass");
        } catch (IllegalArgumentException ex) {
        }
        theSocket.close();

        if (server != null) {
            server.stopServer();
        }
    }

    /**
     * @tests java.net.DatagramSocket#connect(java.net.SocketAddress)
     */
    public void test_connectLjava_net_SocketAddress() throws Exception {

        // validate that we get the PortUnreachable exception if we try to
        // send a dgram to a server that is not running and then do a recv
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            DatagramPacket send = new DatagramPacket(new byte[10], 10);
            ds.send(send);
            DatagramPacket receive = new DatagramPacket(new byte[20], 20);
            ds.setSoTimeout(10000);
            ds.receive(receive);
            ds.close();
            fail("No PortUnreachableException when connected at native level on recv ");
        } catch (PortUnreachableException e) {
            // Expected
        }

        // validate that we can send/receive with datagram sockets connected at
        // the native level
        DatagramServer server = null;
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int serverPortNumber = ports[0];

        InetAddress localHost = InetAddress.getLocalHost();
        DatagramSocket ds = new DatagramSocket(ports[1]);
        DatagramSocket ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        int port = ds.getLocalPort();
        ds.connect(new InetSocketAddress(localHost, serverPortNumber));

        final byte[] sendBytes = { 'T', 'e', 's', 't', 0 };
        DatagramPacket send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        DatagramPacket receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("Wrong size data received: " + receive.getLength(), receive
                .getLength() == sendBytes.length);
        assertTrue("Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("Wrong receiver:" + receive.getAddress() + ":" + localHost,
                receive.getAddress().equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can disconnect
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            ds.disconnect();
            ds.close();
        } catch (PortUnreachableException e) {
            // Expected
        }

        // validate that once connected we cannot send to another address
        try {
            ds = new java.net.DatagramSocket();
            InetAddress inetAddress = InetAddress.getLocalHost();
            int portNumber = Support_PortManager.getNextPortForUDP();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            DatagramPacket senddp = new DatagramPacket(new byte[10], 10,
                    inetAddress, portNumber + 1);
            ds.send(senddp);
            ds.close();
            fail("No Exception when trying to send to a different address on a connected socket ");
        } catch (IllegalArgumentException e) {
            // Expected
        }

        // validate that we can connect, then disconnect, then connect then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
        ds.disconnect();
        ds.connect(new InetSocketAddress(localHost, serverPortNumber));

        send = new DatagramPacket(sendBytes, sendBytes.length);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("connect/disconnect/connect - Wrong size data received: "
                + receive.getLength(), receive.getLength() == sendBytes.length);
        assertTrue("connect/disconnect/connect - Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("connect/disconnect/connect - Wrong receiver:"
                + receive.getAddress() + ":" + localHost, receive.getAddress()
                .equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect/disconnect then send/recv to any address
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
        ds.disconnect();

        send = new DatagramPacket(sendBytes, sendBytes.length, localHost,
                serverPortNumber);
        ds.send(send);
        receive = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receive);
        ds.close();
        assertTrue("connect/disconnect - Wrong size data received: "
                + receive.getLength(), receive.getLength() == sendBytes.length);
        assertTrue("connect/disconnect - Wrong data received"
                + new String(receive.getData(), 0, receive.getLength()) + ":"
                + new String(sendBytes), new String(receive.getData(), 0,
                receive.getLength()).equals(new String(sendBytes)));
        assertTrue("connect/disconnect - Wrong receiver:"
                + receive.getAddress() + ":" + localHost, receive.getAddress()
                .equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // validate that we can connect on an allready connected socket and then
        // send/recv
        server = null;
        ports = Support_PortManager.getNextPortsForUDP(3);
        serverPortNumber = ports[0];

        localHost = InetAddress.getLocalHost();
        ds = new DatagramSocket(ports[1]);
        ds2 = new DatagramSocket(ports[2]);

        server = new DatagramServer(serverPortNumber, localHost);
        server.start();
        Thread.sleep(1000);

        port = ds.getLocalPort();
        ds.connect(new InetSocketAddress(localHost, serverPortNumber + 1));
        ds.connect(new InetSocketAddress(localHost, serverPortNumber));

        byte[] sendTestBytes = { 'T', 'e', 's', 't', 0 };
        send = new DatagramPacket(sendTestBytes, sendTestBytes.length);
        ds.send(send);
        DatagramPacket receivedp = new DatagramPacket(new byte[20], 20);
        ds.setSoTimeout(2000);
        ds.receive(receivedp);
        ds.close();
        assertTrue("connect/connect - Wrong size data received: "
                + receivedp.getLength(),
                receivedp.getLength() == sendTestBytes.length);
        assertTrue("connect/connect - Wrong data received"
                + new String(receivedp.getData(), 0, receivedp.getLength())
                + ":" + new String(sendTestBytes), new String(receivedp
                .getData(), 0, receivedp.getLength()).equals(new String(
                sendTestBytes)));
        assertTrue("connect/connect - Wrong receiver:" + receivedp.getAddress()
                + ":" + localHost, receivedp.getAddress().equals(localHost));

        if (server != null) {
            server.stopServer();
        }

        // test for when we fail to connect at the native level. It seems to
        // fail for the any address so we use this. Now to be compatible we
        // don't throw the exception but eat it and then act as if we were
        // connected at the Java level.
        try {
            ds = new java.net.DatagramSocket();
            byte[] addressBytes = { 0, 0, 0, 0 };
            InetAddress inetAddress = InetAddress.getByAddress(addressBytes);
            int portNumber = Support_PortManager.getNextPortForUDP();
            InetAddress localHostIA = InetAddress.getLocalHost();
            ds.connect(new InetSocketAddress(inetAddress, portNumber));
            assertTrue("Is not connected after connect to inaddr any", ds
                    .isConnected());
            byte[] sendBytesArray = { 'T', 'e', 's', 't', 0 };
            DatagramPacket senddp = new DatagramPacket(sendBytesArray,
                    sendBytesArray.length, localHostIA, portNumber);
            ds.send(senddp);
            fail("No exception when trying to connect at native level with bad address (exception from send)  ");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    /**
     * @tests java.net.DatagramSocket#isBound()
     */
    public void test_isBound() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int port = ports[0];

        DatagramSocket theSocket = new DatagramSocket(ports[1]);
        assertTrue("Socket indicated  not bound when it should be (1)",
                theSocket.isBound());
        theSocket.close();

        theSocket = new DatagramSocket(new InetSocketAddress(addr, port));
        assertTrue("Socket indicated  not bound when it should be (2)",
                theSocket.isBound());
        theSocket.close();

        theSocket = new DatagramSocket(null);
        assertFalse("Socket indicated  bound when it should not be (1)",
                theSocket.isBound());
        theSocket.close();

        // connect causes implicit bind
        theSocket = new DatagramSocket(null);
        theSocket.connect(new InetSocketAddress(addr, port));
        assertTrue("Socket indicated not bound when it should be (3)",
                theSocket.isBound());
        theSocket.close();

        // now test when we bind explicitely
        InetSocketAddress theLocalAddress = new InetSocketAddress(InetAddress
                .getLocalHost(), ports[2]);
        theSocket = new DatagramSocket(null);
        assertFalse("Socket indicated bound when it should not be (2)",
                theSocket.isBound());
        theSocket.bind(theLocalAddress);
        assertTrue("Socket indicated not bound when it should be (4)",
                theSocket.isBound());
        theSocket.close();
        assertTrue("Socket indicated not bound when it should be (5)",
                theSocket.isBound());
    }

    /**
     * @tests java.net.DatagramSocket#isConnected()
     */
    public void test_isConnected() throws Exception {
        InetAddress addr = InetAddress.getLocalHost();
        int[] ports = Support_PortManager.getNextPortsForUDP(4);
        int port = ports[0];

        // base test
        DatagramSocket theSocket = new DatagramSocket(ports[1]);
        assertFalse("Socket indicated connected when it should not be",
                theSocket.isConnected());
        theSocket.connect(new InetSocketAddress(addr, port));
        assertTrue("Socket indicated  not connected when it should be",
                theSocket.isConnected());

        // reconnect the socket and make sure we get the right answer
        theSocket.connect(new InetSocketAddress(addr, ports[2]));
        assertTrue("Socket indicated  not connected when it should be",
                theSocket.isConnected());

        // now disconnect the socket and make sure we get the right answer
        theSocket.disconnect();
        assertFalse("Socket indicated connected when it should not be",
                theSocket.isConnected());
        theSocket.close();

        // now check behavior when socket is closed when connected
        theSocket = new DatagramSocket(ports[3]);
        theSocket.connect(new InetSocketAddress(addr, port));
        theSocket.close();
        assertTrue("Socket indicated  not connected when it should be",
                theSocket.isConnected());
    }

    /**
     * @tests java.net.DatagramSocket#getRemoteSocketAddress()
     */
    public void test_getRemoteSocketAddress() throws Exception {
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        int sport = ports[0];
        int portNumber = ports[1];
        DatagramSocket s = new DatagramSocket(new InetSocketAddress(InetAddress
                .getLocalHost(), portNumber));
        s.connect(new InetSocketAddress(InetAddress.getLocalHost(), sport));
        assertTrue("Returned incorrect InetSocketAddress(1):"
                + s.getLocalSocketAddress().toString(),
                s.getRemoteSocketAddress()
                        .equals(
                                new InetSocketAddress(InetAddress
                                        .getLocalHost(), sport)));
        s.close();

        // now create one that is not connected and validate that we get the
        // right answer
        DatagramSocket theSocket = new DatagramSocket(null);
        portNumber = ports[2];
        theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                portNumber));
        assertNull("Returned incorrect InetSocketAddress -unconnected socket:"
                + "Expected: NULL", theSocket.getRemoteSocketAddress());

        // now connect and validate we get the right answer
        theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(),
                sport));
        assertTrue("Returned incorrect InetSocketAddress(2):"
                + theSocket.getRemoteSocketAddress().toString(),
                theSocket.getRemoteSocketAddress()
                        .equals(
                                new InetSocketAddress(InetAddress
                                        .getLocalHost(), sport)));
        theSocket.close();
    }

    public void test_getLocalSocketAddress_late_bind() throws Exception {
        // An unbound socket should return null as its local address.
        DatagramSocket theSocket = new DatagramSocket((SocketAddress) null);
        assertNull(theSocket.getLocalSocketAddress());

        // now bind the socket and make sure we get the right answer
        int portNumber = Support_PortManager.getNextPortForUDP();
        InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLocalHost(), portNumber);
        theSocket.bind(localAddress);
        assertEquals(localAddress, theSocket.getLocalSocketAddress());
        theSocket.close();
    }

    public void test_getLocalSocketAddress_unbound() throws Exception {
        int portNumber = Support_PortManager.getNextPortForUDP();
        InetSocketAddress localAddress1 = new InetSocketAddress(InetAddress.getLocalHost(), portNumber);
        DatagramSocket s = new DatagramSocket(localAddress1);
        assertEquals(localAddress1, s.getLocalSocketAddress());
        s.close();

        InetSocketAddress remoteAddress = (InetSocketAddress) s.getRemoteSocketAddress();
        assertNull(remoteAddress);
    }

    public void test_getLocalSocketAddress_ANY() throws Exception {
        DatagramSocket s = new DatagramSocket(0);
        try {
            assertTrue("ANY address not IPv6: " + s.getLocalSocketAddress(),
                    ((InetSocketAddress) s.getLocalSocketAddress()).getAddress() instanceof Inet6Address);
        } finally {
            s.close();
        }
    }

    public void test_setReuseAddressZ() throws Exception {
        // test case were we set it to false
        DatagramSocket theSocket1 = null;
        DatagramSocket theSocket2 = null;
        try {
            InetSocketAddress theAddress = new InetSocketAddress(InetAddress.getLocalHost(), Support_PortManager.getNextPortForUDP());
            theSocket1 = new DatagramSocket(null);
            theSocket2 = new DatagramSocket(null);
            theSocket1.setReuseAddress(false);
            theSocket2.setReuseAddress(false);
            theSocket1.bind(theAddress);
            theSocket2.bind(theAddress);
            fail("No exception when trying to connect to do duplicate socket bind with re-useaddr set to false");
        } catch (BindException expected) {
        }
        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }

        // test case were we set it to true
        InetSocketAddress theAddress = new InetSocketAddress(InetAddress.getLocalHost(), Support_PortManager.getNextPortForUDP());
        theSocket1 = new DatagramSocket(null);
        theSocket2 = new DatagramSocket(null);
        theSocket1.setReuseAddress(true);
        theSocket2.setReuseAddress(true);
        theSocket1.bind(theAddress);
        theSocket2.bind(theAddress);

        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }

        // test the default case which we expect to be the same on all
        // platforms
        try {
            theAddress = new InetSocketAddress(InetAddress.getLocalHost(),Support_PortManager.getNextPortForUDP());
            theSocket1 = new DatagramSocket(null);
            theSocket2 = new DatagramSocket(null);
            theSocket1.bind(theAddress);
            theSocket2.bind(theAddress);
            fail("No exception when trying to connect to do duplicate socket bind with re-useaddr left as default");
        } catch (BindException expected) {
        }
        if (theSocket1 != null) {
            theSocket1.close();
        }
        if (theSocket2 != null) {
            theSocket2.close();
        }
    }

    public void test_getReuseAddress() throws Exception {
        DatagramSocket theSocket = new DatagramSocket();
        theSocket.setReuseAddress(true);
        assertTrue("getReuseAddress false when it should be true", theSocket.getReuseAddress());
        theSocket.setReuseAddress(false);
        assertFalse("getReuseAddress true when it should be False", theSocket.getReuseAddress());
    }

    public void test_setBroadcastZ() throws Exception {
        int[] ports = Support_PortManager.getNextPortsForUDP(3);
        DatagramSocket theSocket = new DatagramSocket(ports[0]);
        theSocket.setBroadcast(false);
        byte theBytes[] = { -1, -1, -1, -1 };

        // validate we cannot connect to the broadcast address when
        // setBroadcast is false
        try {
            theSocket.connect(new InetSocketAddress(InetAddress.getByAddress(theBytes), ports[1]));
            assertFalse("No exception when connecting to broadcast address with setBroadcast(false)", theSocket.getBroadcast());
        } catch (Exception ex) {
        }

        // now validate that we can connect to the broadcast address when
        // setBroadcast is true
        theSocket.setBroadcast(true);
        theSocket.connect(new InetSocketAddress(InetAddress.getByAddress(theBytes), ports[2]));
    }

    public void test_getBroadcast() throws Exception {
        DatagramSocket theSocket = new DatagramSocket();
        theSocket.setBroadcast(true);
        assertTrue("getBroadcast false when it should be true", theSocket.getBroadcast());
        theSocket.setBroadcast(false);
        assertFalse("getBroadcast true when it should be False", theSocket.getBroadcast());
    }

    public void test_setTrafficClassI() throws Exception {
        int IPTOS_LOWCOST = 0x2;
        int IPTOS_RELIABILTY = 0x4;
        int IPTOS_THROUGHPUT = 0x8;
        int IPTOS_LOWDELAY = 0x10;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);

        new InetSocketAddress(InetAddress.getLocalHost(), ports[0]);
        DatagramSocket theSocket = new DatagramSocket(ports[1]);

        // validate that value set must be between 0 and 255
        try {
            theSocket.setTrafficClass(256);
            fail("No exception when traffic class set to 256");
        } catch (IllegalArgumentException e) {
        }

        try {
            theSocket.setTrafficClass(-1);
            fail("No exception when traffic class set to -1");
        } catch (IllegalArgumentException e) {
        }

        // now validate that we can set it to some good values
        theSocket.setTrafficClass(IPTOS_LOWCOST);
        theSocket.setTrafficClass(IPTOS_THROUGHPUT);
    }

    public void test_getTrafficClass() throws Exception {
        int IPTOS_LOWCOST = 0x2;
        int IPTOS_RELIABILTY = 0x4;
        int IPTOS_THROUGHPUT = 0x8;
        int IPTOS_LOWDELAY = 0x10;
        int[] ports = Support_PortManager.getNextPortsForUDP(2);

        new InetSocketAddress(InetAddress.getLocalHost(), ports[0]);
        DatagramSocket theSocket = new DatagramSocket(ports[1]);

        /*
         * we cannot actually check that the values are set as if a platform
         * does not support the option then it may come back unset even
         * though we set it so just get the value to make sure we can get it
         */
        int trafficClass = theSocket.getTrafficClass();
    }

    public void test_isClosed() throws Exception {
        DatagramSocket theSocket = new DatagramSocket();

        // validate isClosed returns expected values
        assertFalse("Socket should indicate it is not closed(1):", theSocket
                .isClosed());
        theSocket.close();
        assertTrue("Socket should indicate it is not closed(1):", theSocket
                .isClosed());

        InetSocketAddress theAddress = new InetSocketAddress(InetAddress
                .getLocalHost(), Support_PortManager.getNextPortForUDP());
        theSocket = new DatagramSocket(theAddress);
        assertFalse("Socket should indicate it is not closed(2):", theSocket
                .isClosed());
        theSocket.close();
        assertTrue("Socket should indicate it is not closed(2):", theSocket
                .isClosed());
    }

    /**
     * @tests java.net.DatagramSocket#getChannel()
     */
    public void test_getChannel() throws SocketException {
        assertNull(new DatagramSocket().getChannel());
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() {
        retval = "Bogus retval";
    }

    /**
     * Tears down the fixture, for example, close a network connection. This
     * method is called after a test is executed.
     */
    protected void tearDown() {
        try {
            ds.close();
            sds.close();
        } catch (Exception e) {
        }
    }

    protected void receive_oversize_java_net_DatagramPacket() {
        final int[] ports = Support_PortManager.getNextPortsForUDP(2);
        final int portNumber = ports[0];

        class TestDGRcvOver implements Runnable {
            public void run() {
                InetAddress localHost = null;
                try {
                    localHost = InetAddress.getLocalHost();
                    Thread.sleep(1000);
                    DatagramSocket sds = new DatagramSocket(ports[1]);
                    DatagramPacket rdp = new DatagramPacket("0123456789"
                            .getBytes(), 10, localHost, portNumber);
                    sds.send(rdp);
                    sds.close();
                } catch (Exception e) {
                    System.err.println("host " + localHost + " port "
                            + portNumber + " failed to send oversize data: "
                            + e);
                    e.printStackTrace();
                }
            }
        }

        try {
            new Thread(new TestDGRcvOver(), "DGSenderOver").start();
            ds = new java.net.DatagramSocket(portNumber);
            ds.setSoTimeout(6000);
            byte rbuf[] = new byte[5];
            DatagramPacket rdp = new DatagramPacket(rbuf, rbuf.length);
            ;
            ds.receive(rdp);
            ds.close();
            assertTrue("Send/Receive oversize failed to return correct data: "
                    + new String(rbuf, 0, 5), new String(rbuf, 0, 5)
                    .equals("01234"));
        } catch (Exception e) {
            System.err.println("Exception during send test: " + e);
            e.printStackTrace();
            fail("port " + portNumber + " Exception: " + e
                    + " during oversize send test");
        } finally {
            ds.close();
        }
    }
}
