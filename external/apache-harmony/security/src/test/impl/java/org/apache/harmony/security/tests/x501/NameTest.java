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
* @author Stepan M. Mishura
*/

package org.apache.harmony.security.tests.x501;

import java.io.ByteArrayInputStream;

import org.apache.harmony.security.x501.Name;


import junit.framework.TestCase;

/**
 * Test Name class
 *
 * @see http://www.ietf.org/rfc/rfc1779.txt
 * @see http://www.ietf.org/rfc/rfc2253.txt
 */
public class NameTest extends TestCase {

    private static final byte [] mess = {
            0x30, //0 seq of
            (byte) 0x81,(byte) 0x9A, //1 len = 154
            0x31, 0x0A, //3,4
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x01, 0x5A,
            0x31, 0x0A, //15,16
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x0B, 0x13, 0x01, 0x45,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x01, 0x44,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x01, 0x43,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x0A, 0x13, 0x01, 0x42,
            0x31, 0x15,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x01, 0x41,
            0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x08, 0x13, 0x02, 0x43, 0x41,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x0B, 0x13, 0x01, 0x45,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x01, 0x44,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x01, 0x43,
            0x31, 0x0A,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x0A, 0x13, 0x01, 0x42,
            0x31, 0x15,
            0x30, 0x08, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x01, 0x41,
            0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x08, 0x13, 0x02, 0x43, 0x41
    };

    public void testGetName1779() throws Exception {

        Name principal = (Name) Name.ASN1.decode(mess);

        String s = principal.getName("RFC1779");

        assertEquals(
                "CN=A + ST=CA, O=B, L=C, C=D, OU=E, CN=A + ST=CA, O=B, L=C, C=D, OU=E, CN=Z",
                s);
    }

    public void testStreamGetName1779() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(mess);

        Name principal = (Name) Name.ASN1.decode(is);

        String s = principal.getName("RFC1779");

        assertEquals(
                "CN=A + ST=CA, O=B, L=C, C=D, OU=E, CN=A + ST=CA, O=B, L=C, C=D, OU=E, CN=Z",
                s);
    }
}
