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

import org.apache.harmony.security.asn1.ASN1Exception;
import org.apache.harmony.security.asn1.ASN1Oid;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for OID type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class OidTest extends TestCase {

    private static Object[][] oid = {
            //oid array format: string / int array / DER encoding
            { "0.0", // as string
                    new int[] { 0, 0 }, // as int array
                    new byte[] { 0x06, 0x01, 0x00 } },
            //
            { "0.0.3", // as string
                    new int[] { 0, 0, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x00, 0x03 } },
            //
            { "0.1.3", // as string
                    new int[] { 0, 1, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x01, 0x03 } },
            //
            { "0.5", // as string
                    new int[] { 0, 5 }, // as int array
                    new byte[] { 0x06, 0x01, 0x05 } },
            //
            { "0.39.3", // as string
                    new int[] { 0, 39, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x27, 0x03 } },
            //
            { "1.0.3", // as string
                    new int[] { 1, 0, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x28, 0x03 } },
            //
            { "1.1", // as string
                    new int[] { 1, 1 }, // as int array
                    new byte[] { 0x06, 0x01, 0x29 } },
            //
            { "1.2.1.2.1",// as string
                    new int[] { 1, 2, 1, 2, 1 }, // as int array
                    new byte[] { 0x06, 0x04, 0x2A, 0x01, 0x02, 0x01 } },
            //
            {
                    "1.2.840.113554.1.2.2",// as string
                    new int[] { 1, 2, 840, 113554, 1, 2, 2 }, // as int array
                    new byte[] { 0x06, 0x09, 0x2A, (byte) 0x86, 0x48,
                            (byte) 0x86, (byte) 0xF7, 0x12, 0x01, 0x02, 0x02 } },
            //
            { "1.39.3",// as string
                    new int[] { 1, 39, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x4F, 0x03 } },
            //
            { "2.0.3",// as string
                    new int[] { 2, 0, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x50, 0x03 } },
            //
            { "2.5.4.3",// as string
                    new int[] { 2, 5, 4, 3 }, // as int array
                    new byte[] { 0x06, 0x03, 0x55, 0x04, 0x03 } },
            //
            { "2.39.3", // as string
                    new int[] { 2, 39, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x77, 0x03 } },
            //
            { "2.40.3", // as string
                    new int[] { 2, 40, 3 }, // as int array
                    new byte[] { 0x06, 0x02, 0x78, 0x03 } },
            //
            { "2.47", // as string
                    new int[] { 2, 47 }, // as int array
                    new byte[] { 0x06, 0x01, 0x7F } },
            //
            { "2.48", // as string
                    new int[] { 2, 48 }, // as int array
                    new byte[] { 0x06, 0x02, (byte) 0x81, 0x00 } },
            //
            { "2.48.5", // as string
                    new int[] { 2, 48, 5 }, // as int array
                    new byte[] { 0x06, 0x03, (byte) 0x81, 0x00, 0x05 } },
            //
            { "2.100.3", // as string
                    new int[] { 2, 100, 3 }, // as int array
                    new byte[] { 0x06, 0x03, (byte) 0x81, 0x34, 0x03 } } };

    public void test_MappingToIntArray() throws IOException {

        // oid decoder/encoder for testing
        ASN1Oid asn1 = ASN1Oid.getInstance();

        // testing decoding
        for (int i = 0; i < oid.length; i++) {

            int[] decoded = (int[]) asn1.decode(new DerInputStream(
                    (byte[]) oid[i][2]));

            assertTrue("Failed to decode oid: " + oid[i][0], // error message
                    Arrays.equals((int[]) oid[i][1], // expected array
                            decoded));
        }

        // testing encoding
        for (int i = 0; i < oid.length; i++) {

            byte[] encoded = new DerOutputStream(ASN1Oid.getInstance(),
                    oid[i][1]).encoded;

            assertTrue("Failed to encode oid: " + oid[i][0], // error message
                    Arrays.equals((byte[]) oid[i][2], // expected encoding
                            encoded));
        }
    }

    public void testDecode_Invalid() throws IOException {
        byte[][] invalid = new byte[][] {
        // wrong tag: tag is not 0x06
                new byte[] { 0x02, 0x01, 0x00 },
                // wrong length: length is 0
                new byte[] { 0x06, 0x00 },
                // wrong content: bit 8 of the last byte is not 0
                new byte[] { 0x06, 0x02, (byte) 0x81, (byte) 0x80 },
        // wrong content: is not encoded in fewest number of bytes
        //FIXME new byte[] { 0x06, 0x02, (byte) 0x80, (byte) 0x01 }
        };

        for (int i = 0; i < invalid.length; i++) {
            try {
                DerInputStream in = new DerInputStream(invalid[i]);
                ASN1Oid.getInstance().decode(in);
                fail("No expected ASN1Exception for:" + i);
            } catch (ASN1Exception e) {
            }
        }
    }

    public void test_MappingToString() throws IOException {

        // oid decoder/encoder for testing
        ASN1Oid asn1 = ASN1Oid.getInstanceForString();

        // testing decoding
        for (int i = 0; i < oid.length; i++) {
            assertEquals("Failed to decode oid: " + oid[i][0], // error message
                    oid[i][0], // expected string
                    asn1.decode(new DerInputStream((byte[]) oid[i][2])));
        }

        // testing encoding
        for (int i = 0; i < oid.length; i++) {
            assertTrue("Failed to encode oid: " + oid[i][0], // error message
                    Arrays.equals((byte[]) oid[i][2], // expected encoding
                            new DerOutputStream(asn1, oid[i][0]).encoded));
        }
    }
}
