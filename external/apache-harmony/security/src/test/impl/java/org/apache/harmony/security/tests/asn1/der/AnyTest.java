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

package org.apache.harmony.security.tests.asn1.der;

import java.io.IOException;
import java.util.Arrays;

import org.apache.harmony.security.asn1.ASN1Any;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for ANY type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class AnyTest extends TestCase {

    private static byte[] encoded = new byte[] { 0x01, 0x03, 0x11, 0x13, 0x15 };

    public void testDecode() throws IOException {
        DerInputStream in = new DerInputStream(encoded);
        assertTrue(Arrays.equals(encoded, (byte[]) ASN1Any.getInstance()
                .decode(in)));
    }

    public void testEncode() throws IOException {
        DerOutputStream out = new DerOutputStream(ASN1Any.getInstance(),
                encoded);
        assertTrue("False", Arrays.equals(encoded, out.encoded));
    }
}
