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
import java.util.Date;

import junit.framework.TestCase;

import org.apache.harmony.auth.internal.kerberos.v5.KerberosErrorMessage;
import org.apache.harmony.auth.internal.kerberos.v5.PrincipalName;
import org.apache.harmony.security.asn1.DerInputStream;

public class KerberosErrorMessageTest extends TestCase {

    public void test_Ctor() throws IOException {

        KerberosErrorMessage message = KerberosErrorMessage
                .decode(new DerInputStream(err_resp));

        assertEquals("ctime", new Date(1000), message.getCtime());
        assertEquals("cusec", 65536, message.getCusec());

        assertEquals("stime", new Date(0), message.getStime());
        assertEquals("susec", 65793, message.getSusec());

        assertEquals("error-code", 6, message.getErrorCode());

        assertEquals("crealm", "MY.REALM", message.getCrealm());
        assertEquals("cname", new PrincipalName(PrincipalName.NT_PRINCIPAL,
                new String[] { "no_such_user" }), message.getCname());

        assertEquals("realm", "MY.REALM", message.getRealm());
        assertEquals("sname", new PrincipalName(PrincipalName.NT_UNKNOWN,
                new String[] { "krbtgt", "MY.REALM" }), message.getSname());

        assertEquals("etext", "e_text_string", message.getEtext());
    }

    // testing array was created by hands according to RFC4120
    public static byte[] err_resp = new byte[] {
    // KRB-ERROR ::= [APPLICATION 30]
            (byte) 0x7e,
            (byte) 0x81,
            (byte) 0xA9,

            // SEQUENCE
            (byte) 0x30,
            (byte) 0x81,
            (byte) 0xA6,

            // pvno [0] INTEGER (5)
            (byte) 0xa0,
            (byte) 0x03,
            //
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x05,

            // msg-type [1] INTEGER (30)
            (byte) 0xA1,
            (byte) 0x03,
            //
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x1e,

            // ctime [2] KerberosTime OPTIONAL,
            (byte) 0xA2,
            (byte) 0x11,
            //
            (byte) 0x18,
            (byte) 0x0f,
            (byte) 0x31,
            (byte) 0x39,
            (byte) 0x37,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x31,
            (byte) 0x30,
            (byte) 0x31,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x31,
            (byte) 0x5A,

            // cusec [3] Microseconds OPTIONAL,
            (byte) 0xA3,
            (byte) 0x05,
            //
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x01,
            (byte) 0x00,
            (byte) 0x00,

            // stime [4] KerberosTime,
            (byte) 0xA4,
            (byte) 0x11,
            //
            (byte) 0x18,
            (byte) 0x0f,
            (byte) 0x31,
            (byte) 0x39,
            (byte) 0x37,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x31,
            (byte) 0x30,
            (byte) 0x31,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x30,
            (byte) 0x5A,

            // susec [5] Microseconds
            (byte) 0xa5,
            (byte) 0x05,
            //
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x01,
            (byte) 0x01,
            (byte) 0x01,

            // error-code [6] Int32
            (byte) 0xa6,
            (byte) 0x03,
            //
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x06,

            // crealm [7] Realm OPTIONAL
            (byte) 0xa7,
            (byte) 0x0a,
            //
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

            // cname [8] PrincipalName OPTIONAL
            (byte) 0xa8,
            (byte) 0x19,
            // SEQUENCE
            (byte) 0x30,
            (byte) 0x17,
            // name-type
            (byte) 0xa0,
            (byte) 0x03,
            //
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x01,
            // name-string
            (byte) 0xa1,
            (byte) 0x10,
            // SEQUENCE OF
            (byte) 0x30,
            (byte) 0x0e,
            // 1-st string
            (byte) 0x1b,
            (byte) 0x0c,
            (byte) 0x6e,
            (byte) 0x6f,
            (byte) 0x5f,
            (byte) 0x73,
            (byte) 0x75,
            (byte) 0x63,
            (byte) 0x68,
            (byte) 0x5f,
            (byte) 0x75,
            (byte) 0x73,
            (byte) 0x65,
            (byte) 0x72,

            // realm [9] Realm -- service realm --,
            (byte) 0xa9,
            (byte) 0x0a,
            //
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

            // sname [10] PrincipalName -- service name --,
            (byte) 0xaa,
            (byte) 0x1d,
            // SEQUENCE
            (byte) 0x30,
            (byte) 0x1b,
            // name-type
            (byte) 0xa0,
            (byte) 0x03,
            //
            (byte) 0x02,
            (byte) 0x01,
            (byte) 0x00,
            // name-string
            (byte) 0xa1,
            (byte) 0x14,
            // SEQUENCE OF
            (byte) 0x30,
            (byte) 0x12,
            // 1-st string
            (byte) 0x1b,
            (byte) 0x06,
            (byte) 0x6b,
            (byte) 0x72,
            (byte) 0x62,
            (byte) 0x74,
            (byte) 0x67,
            (byte) 0x74,
            // 2-nd string
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

            // e-text [11] KerberosString OPTIONAL
            (byte) 0xab,
            (byte) 0x0f,
            (byte) 0x1b,
            (byte) 0x0d,
            (byte) 0x65,
            (byte) 0x5f,
            (byte) 0x74,
            (byte) 0x65,
            (byte) 0x78,
            (byte) 0x74,
            (byte) 0x5f,
            (byte) 0x73,
            (byte) 0x74,
            (byte) 0x72,
            (byte) 0x69,
            (byte) 0x6e,
            (byte) 0x67,

    // e-data [12] OCTET STRING OPTIONAL: TODO add me for testing
    };

}
