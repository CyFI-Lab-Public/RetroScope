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

package org.apache.harmony.auth.tests.internal.kerberos.v5;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.auth.internal.kerberos.v5.EncryptedData;
import org.apache.harmony.auth.internal.kerberos.v5.KDCReply;
import org.apache.harmony.auth.internal.kerberos.v5.PrincipalName;
import org.apache.harmony.auth.internal.kerberos.v5.Ticket;

public class KDCReplyTest extends TestCase {

    public void test_Ctor() throws IOException {

        KDCReply reply = (KDCReply) KDCReply.AS_REP_ASN1.decode(enc);

        assertEquals("msg-type", KDCReply.AS_REP, reply.getMsgtype());
        assertEquals("crealm", "MY.REALM", reply.getCrealm());
        assertEquals("cname", new PrincipalName(1, new String[] { "me" }),
                reply.getCname());

        // ticket
        Ticket ticket = reply.getTicket();
        assertEquals("ticket's realm", "MY.REALM", ticket.getRealm());
        assertEquals("ticket's sname", new PrincipalName(0, new String[] {
                "krbtgt", "MY.REALM" }), ticket.getSname());
        
        // enc-part
        EncryptedData encPart = reply.getEncPart();
        assertEquals("etype", 3, encPart.getEtype());
        assertEquals("kvno", 1, encPart.getKvno());
        assertTrue("cipher", Arrays.equals(new byte[] { 0x0f }, encPart
                .getCipher()));
    }

    // testing array was created by hands according to RFC4120
    public static byte[] enc = new byte[] {
            // [APPLICATION 11] KDC-REP
            (byte) 0x6b,
            (byte) 0x76,
            // KDC-REP ::= SEQUENCE 
            (byte) 0x30,
            (byte) 0x74,
            // pvno [0] INTEGER (5)
            (byte) 0xa0,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x05,
            // msg-type [1] INTEGER 
            (byte) 0xa1,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x0b,
            // padata [2] SEQUENCE OF PA-DATA OPTIONAL: missed for a while
            // crealm [3] Realm
            (byte) 0xa3,
            (byte) 0x0a,
            (byte) 0x1b,
            (byte) 0x08,
            (byte) 0x4d,
            (byte) 0x59,
            (byte) 0x2e,
            (byte) 0x52,
            (byte) 0x45,
            (byte) 0x41,
            (byte) 0x4c,
            (byte) 0x4d,
            // cname [4] PrincipalName
            (byte) 0xa4,
            (byte) 0x0f,
            (byte) 0x30,
            (byte) 0x0d,
            (byte) 0xa0,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x01,
            (byte) 0xa1,
            (byte) 0x06,
            (byte) 0x30,
            (byte) 0x04,
            (byte) 0x1b,
            (byte) 0x02,
            (byte) 0x6d,
            (byte) 0x65,

            // ticket [5] Ticket
            (byte) 0xa5,
            (byte) 0x38,
            // [APPLICATION 1]
            (byte) 0x61,
            (byte) 0x36,
            // SEQUENCE 
            (byte) 0x30,
            (byte) 0x34,
            // tkt-vno [0] INTEGER (5)
            (byte) 0xa0,
            (byte) 0x03,
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x05,
            // realm [1] Realm
            (byte) 0xa1, (byte) 0x0a, (byte) 0x1b, (byte) 0x08, (byte) 0x4d,
            (byte) 0x59,
            (byte) 0x2e,
            (byte) 0x52,
            (byte) 0x45,
            (byte) 0x41,
            (byte) 0x4c,
            (byte) 0x4d,
            // sname [2] PrincipalName
            (byte) 0xa2, (byte) 0x1d, (byte) 0x30, (byte) 0x1b, (byte) 0xa0,
            (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0xa1,
            (byte) 0x14, (byte) 0x30, (byte) 0x12, (byte) 0x1b, (byte) 0x06,
            (byte) 0x6b, (byte) 0x72, (byte) 0x62, (byte) 0x74, (byte) 0x67,
            (byte) 0x74, (byte) 0x1b, (byte) 0x08, (byte) 0x4d, (byte) 0x59,
            (byte) 0x2e, (byte) 0x52, (byte) 0x45, (byte) 0x41, (byte) 0x4c,
            (byte) 0x4d,
            // enc-part [3] EncryptedData: empty for a while 
            (byte) 0xa3, (byte) 0x02, (byte) 0x00, (byte) 0x00,

            // enc-part [6] EncryptedData: empty for a while
            (byte) 0xa6, (byte) 0x11, (byte) 0x30, (byte) 0x0F,
            // etype
            (byte) 0xa0, (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x03,
            // kvno
            (byte) 0xA1, (byte) 0x03, (byte) 0x02, (byte) 0x01, (byte) 0x01,
            // cipher  
            (byte) 0xa2, (byte) 0x03, (byte) 0x04, (byte) 0x01, (byte) 0x0F,
    };

}
