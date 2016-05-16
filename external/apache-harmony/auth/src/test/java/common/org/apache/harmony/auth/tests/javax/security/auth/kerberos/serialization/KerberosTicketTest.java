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

package org.apache.harmony.auth.tests.javax.security.auth.kerberos.serialization;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;

import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KerberosTicket;

import junit.framework.TestCase;

import org.apache.harmony.testframework.serialization.SerializationTest;
import org.apache.harmony.testframework.serialization.SerializationTest.SerializableAssert;

public class KerberosTicketTest extends TestCase {

    // ticket's ASN.1 encoding  
    private static final byte[] ticket = { 0x01, 0x02, 0x03, 0x04 };

    // client's principal 
    private static final KerberosPrincipal pClient = new KerberosPrincipal(
            "client@apache.org");

    // server's principal 
    private static final KerberosPrincipal pServer = new KerberosPrincipal(
            "server@apache.org");

    // session key
    private static final byte[] sessionKey = { 0x01, 0x04, 0x03, 0x02 };

    private static final int KEY_TYPE = 1;

    private static final boolean[] flags = { true, false, true, false, true,
            false, true, false, true, false, true, false, };

    private static final Date authTime = new Date(0);

    private static final Date startTime = new Date(1);

    private static final Date endTime = new Date(2);

    private static final Date renewTill = new Date(3);

    private static final InetAddress[] addesses = null;

    // comparator for KerberosKey objects
    private static final SerializableAssert COMPARATOR = new SerializableAssert() {
        public void assertDeserialized(Serializable initial,
                Serializable deserialized) {

            KerberosTicket init = (KerberosTicket) initial;
            KerberosTicket desr = (KerberosTicket) deserialized;

            assertTrue("Ticket", Arrays.equals(init.getEncoded(), desr
                    .getEncoded()));

            assertEquals("Client", init.getClient(), desr.getClient());
            assertEquals("Server", init.getServer(), desr.getServer());

            assertTrue("Key", Arrays.equals(init.getSessionKey().getEncoded(),
                    desr.getSessionKey().getEncoded()));
            assertEquals("KeyType", init.getSessionKeyType(), desr
                    .getSessionKeyType());

            assertTrue("Flags", Arrays.equals(init.getFlags(), desr.getFlags()));

            assertEquals("AuthTime", init.getAuthTime(), desr.getAuthTime());
            assertEquals("StartTime", init.getStartTime(), desr.getStartTime());
            assertEquals("EndTime", init.getEndTime(), desr.getEndTime());
            assertEquals("RenewTill", init.getRenewTill(), desr.getRenewTill());

            assertNull("ClientAddresses", desr.getClientAddresses());
        }
    };

    /**
     * @tests serialization/deserialization compatibility.
     */
    public void testSerializationSelf() throws Exception {
        SerializationTest.verifySelf(new KerberosTicket(ticket, pClient,
                pServer, sessionKey, KEY_TYPE, flags, authTime, startTime,
                endTime, renewTill, addesses), COMPARATOR);
    }

    /**
     * @tests serialization/deserialization compatibility with RI.
     */
    public void testSerializationCompatibility() throws Exception {
        SerializationTest.verifyGolden(this, new KerberosTicket(ticket,
                pClient, pServer, sessionKey, KEY_TYPE, flags, authTime,
                startTime, endTime, renewTill, addesses), COMPARATOR);

    }
}
