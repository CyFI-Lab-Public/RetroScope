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
* @author Vladimir N. Molotkov
*/

package org.apache.harmony.security.tests.asn1.der;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Exception;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;


/**
 * ASN.1 DER test for Boolean type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */
public class BooleanTest extends TestCase {

    private static byte[] eFalse = new byte[] { 0x01, 0x01, 0x00 };

    private static byte[] eTrue = new byte[] { 0x01, 0x01, (byte) 0xFF };

    public void test_Decode_Encode() throws IOException {

        // oid decoder/encoder for testing
        ASN1Boolean asn1 = ASN1Boolean.getInstance();

        // decoding false
        DerInputStream in = new DerInputStream(eFalse);
        assertEquals("Decoding false value", Boolean.FALSE, asn1.decode(in));

        // decoding true
        in = new DerInputStream(eTrue);
        assertEquals("Decoding true value", Boolean.TRUE, asn1.decode(in));

        // encoding false
        DerOutputStream out = new DerOutputStream(asn1, Boolean.FALSE);
        assertTrue("Encoding false value", Arrays.equals(eFalse, out.encoded));

        // encoding true
        out = new DerOutputStream(asn1, Boolean.TRUE);
        assertTrue("Encoding true value", Arrays.equals(eTrue, out.encoded));
    }

    public void testDecode_Invalid() throws IOException {

        byte[][] invalid = new byte[][] {
        // wrong tag: tag is not 0x01
                new byte[] { 0x02, 0x01, 0x00 },
                // wrong length: length is not 1
                new byte[] { 0x01, 0x02, 0x00 },
                // wrong content: content is not 0x01 or 0xFF
                new byte[] { 0x01, 0x01, 0x33 } };

        for (int i = 0; i < invalid.length; i++) {
            try {
                DerInputStream in = new DerInputStream(invalid[i]);
                ASN1Boolean.getInstance().decode(in);
                fail("No expected ASN1Exception for: " + i);
            } catch (ASN1Exception e) {
            }
        }
    }
}
