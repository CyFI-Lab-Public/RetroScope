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

package org.apache.harmony.auth.tests.module;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TreeMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import org.apache.harmony.auth.module.Krb5LoginModule;
import org.apache.harmony.auth.tests.internal.kerberos.v5.KerberosErrorMessageTest;
import org.apache.harmony.auth.tests.support.TestUtils;

public class Krb5LoginModuleTest extends TestCase {

    // default kdc server
    private static final String ENV_KDC = "java.security.krb5.kdc";

    // default realm
    private static final String ENV_REALM = "java.security.krb5.realm";

    // old value of 'java.security.krb5.kdc' system property
    private String kdc;

    // old value of 'java.security.krb5.realm' system property
    private String realm;

    // embedded server
    private KrbServer server;

    // module options
    private final TreeMap<String, String> options = new TreeMap<String, String>();

    /**
     * Sets system env. properties and optionally starts local mock server
     */
    @Override
    protected void setUp() throws Exception {

        // save old system properties
        kdc = System.getProperty(ENV_KDC);
        realm = System.getProperty(ENV_REALM);

        if (kdc == null) {
            // run test with embedded server
            server = new KrbServer();

            server.start();
            while (server.port == 0) {
                // wait until server open datagram socket 
            }

            System.setProperty(ENV_KDC, "localhost:" + server.port);
        } // else: run test with external server

        System.setProperty(ENV_REALM, "MY.REALM");
    }

    /**
     * Shuts down local server and restore system env. properties
     */
    @Override
    protected void tearDown() throws Exception {
        if (server != null) {
            // shut down local server
            server.interrupt();
        }

        // restore env. variables
        TestUtils.setSystemProperty(ENV_KDC, kdc);
        TestUtils.setSystemProperty(ENV_REALM, realm);
    }

    /**
     * TODO
     */
    public void test_Config() throws Exception {

        // create login module for testing
        Krb5LoginModule module = new Krb5LoginModule();
        module.initialize(null, new MockCallbackHandler(), null, options);

        // case 1: unset 'kdc' and set 'real' sys.props
        TestUtils.setSystemProperty(ENV_KDC, null);
        TestUtils.setSystemProperty(ENV_REALM, "some_value");
        try {
            module.login();
            fail("No expected LoginException");
        } catch (LoginException e) {
        }

        // case 2: set 'kdc' and unset 'real' sys.props
        TestUtils.setSystemProperty(ENV_KDC, "some_value");
        TestUtils.setSystemProperty(ENV_REALM, null);
        try {
            module.login();
            fail("No expected LoginException");
        } catch (LoginException e) {
        }

        //TODO: test reading config from configuration file 'krb5.conf'
    }

    /**
     * @tests request ticket for absent user
     */
    public void test_login() throws Exception {

        if (server != null) {
            server.respond = KerberosErrorMessageTest.err_resp;
        }

        Krb5LoginModule module = new Krb5LoginModule();

        options.put("principal", "no_such_user");

        module.initialize(null, null, null, options);
        try {
            module.login();
            fail("No expected LoginException");
        } catch (LoginException e) {
            System.out.println(e);
        }
    }

    /**
     * Mock callback handler
     */
    static class MockCallbackHandler implements CallbackHandler {

        public MockCallbackHandler() {
        }

        public void handle(Callback[] callbacks) {
        }
    }

    /**
     * Embedded test server
     */
    static class KrbServer extends Thread {

        private static boolean debug = false;

        private static final int BUF_SIZE = 1024;

        public int port;

        public byte[] respond;

        @Override
        public void run() {

            try {
                DatagramSocket socket = new DatagramSocket();

                port = socket.getLocalPort();

                byte[] request = new byte[BUF_SIZE];
                DatagramPacket packet = new DatagramPacket(request,
                        request.length);

                int bytesRead = BUF_SIZE;
                while (bytesRead == BUF_SIZE) {
                    socket.receive(packet);
                    bytesRead = packet.getLength();
                }

                printAsHex(10, "(byte)", ",", request);

                if (respond != null) {
                    packet = new DatagramPacket(respond, respond.length, packet
                            .getAddress(), packet.getPort());
                    socket.send(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void printAsHex(int perLine, String prefix,
                String delimiter, byte[] data) {

            if (!debug) {
                return;
            }

            for (int i = 0; i < data.length; i++) {
                String tail = Integer.toHexString(0x000000ff & data[i]);
                if (tail.length() == 1) {
                    tail = "0" + tail;
                }
                System.out.print(prefix + "0x" + tail + delimiter);

                if (((i + 1) % perLine) == 0) {
                    System.out.println("");
                }
            }
            System.out.println("");
        }
    }
}
