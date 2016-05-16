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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.harmony.security.asn1.ASN1Exception;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;


/**
 * ASN.1 DER test for INTEGER type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class IntegerTest extends TestCase {

    public static final Object[][] validTestcase = {
            // BigInteger / two's-complement representation / encoding
            { new BigInteger("0"), null, null },
            { new BigInteger("1"), null, null },
            { new BigInteger("-1"), null, null },
            { new BigInteger("127"), null, null },
            { new BigInteger("-127"), null, null },
            { new BigInteger("128"), null, null },
            { new BigInteger("-128"), null, null },
            { new BigInteger("32767"), null, null },
            { new BigInteger("-32767"), null, null },
            { new BigInteger("32768"), null, null },
            { new BigInteger("-32768"), null, null },
            { new BigInteger("1234567890"), null, null },
            { new BigInteger("-1234567890"), null, null },
            { // 20 octets
                    new BigInteger(new byte[] { 0x7F, 0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }), null,
                    null }, };

    static {
        for (int i = 0; i < validTestcase.length; i++) {
            // get two's-complement representation
            byte[] array = ((BigInteger) validTestcase[i][0]).toByteArray();

            // create encoding
            byte[] encoded = new byte[array.length + 2];
            encoded[0] = 0x02;
            encoded[1] = (byte) (encoded.length - 2);
            System.arraycopy(array, 0, encoded, 2, encoded[1]);

            // assign is to testcases
            validTestcase[i][1] = array;
            validTestcase[i][2] = encoded;
        }
    }

    /**
     * Tests decoding/encoding integers to/from byte array
     */
    public void testDecode_Encode() throws IOException {

        // oid decoder/encoder for testing
        ASN1Integer asn1 = ASN1Integer.getInstance();

        // decode from byte array
        for (int i = 0; i < validTestcase.length; i++) {
            DerInputStream in = new DerInputStream((byte[]) validTestcase[i][2]);
            assertTrue((validTestcase[i][0]).toString(), // message
                    Arrays.equals((byte[]) validTestcase[i][1], // expected
                            (byte[]) asn1.decode(in))); // returned
        }

        // decode from input stream
        for (int i = 0; i < validTestcase.length; i++) {
            DerInputStream in = new DerInputStream(new ByteArrayInputStream(
                    (byte[]) validTestcase[i][2]));
            assertTrue((validTestcase[i][0]).toString(), //message
                    Arrays.equals((byte[]) validTestcase[i][1], //expected
                            (byte[]) asn1.decode(in))); //returned
        }

        // encoding
        for (int i = 0; i < validTestcase.length; i++) {
            DerOutputStream out = new DerOutputStream(asn1, validTestcase[i][1]);
            assertTrue((validTestcase[i][0]).toString(), //message
                    Arrays.equals((byte[]) validTestcase[i][2], //expected
                            out.encoded));//returned
        }
    }

    /**
     * Tests invalid ASN.1 integer encodings
     */
    public void testDecode_Invalid() throws IOException {
        byte[][] invalid = new byte[][] {
        // wrong tag: tag is not 0x02
                new byte[] { 0x01, 0x01, 0x00 },
                // wrong length: length is 0
                new byte[] { 0x02, 0x00 },
                // wrong content: is not encoded in minimum number of octets
                new byte[] { 0x02, 0x02, 0x00, 0x00 },
                // wrong content: is not encoded in minimum number of octets
                new byte[] { 0x02, 0x02, (byte) 0xFF, (byte) 0x80 } };

        for (int i = 0; i < invalid.length; i++) {
            try {
                DerInputStream in = new DerInputStream(invalid[i]);
                ASN1Integer.getInstance().decode(in);
                fail("No expected ASN1Exception for:" + i);
            } catch (ASN1Exception e) {
            }
        }
    }

    public void testConversion() {
        int[] testcase = new int[] { 0, 1, -1, 127, -127, 128, -128, 32767,
                -32768, Integer.MAX_VALUE, Integer.MIN_VALUE };

        for (int i = 0; i < testcase.length; i++) {
            assertEquals("Testcase: " + i, testcase[i], ASN1Integer
                    .toIntValue(ASN1Integer.fromIntValue(testcase[i])));
        }
    }
}
