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

import org.apache.harmony.security.asn1.ASN1Constants;
import org.apache.harmony.security.asn1.ASN1Exception;
import org.apache.harmony.security.asn1.BerInputStream;

/**
 * Tests BerInputStream implementation
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class BerInputStreamTest extends TestCase {

    /**
     * @tests org.apache.harmony.security.asn1.BerInputStream#BerInputStream(
     *        java.io.ByteArrayInputStream)
     */
    public void test_CtorLjava_io_ByteArrayInputStream() throws IOException {

        //
        // tests for decoding initial length of encodings
        //
        Object[][] testcase = {
        // length = 0x01
                { new byte[] { 0x30, (byte) 0x81, 0x01 }, BigInteger.valueOf(1) },
                // length = 0xFF
                { new byte[] { 0x30, (byte) 0x81, (byte) 0xFF },
                        BigInteger.valueOf(0xFF) },
                // length = 0x0101
                { new byte[] { 0x30, (byte) 0x82, 0x01, 0x01 },
                        BigInteger.valueOf(0x0101) },
                // length = 0xFFFF
                { new byte[] { 0x30, (byte) 0x82, (byte) 0xFF, (byte) 0xFF },
                        BigInteger.valueOf(0xFFFF) },
                // length = 0x0FFFFF
                {
                        new byte[] { 0x30, (byte) 0x83, 0x0F, (byte) 0xFF,
                                (byte) 0xFF }, BigInteger.valueOf(0x0FFFFF) },
                // length = 0xFFFFFF
                {
                        new byte[] { 0x30, (byte) 0x83, (byte) 0xFF,
                                (byte) 0xFF, (byte) 0xFF },
                        BigInteger.valueOf(0xFFFFFF) },
                // length = 0xFFFFFF (encoded length has extra byte)
                {
                        new byte[] { 0x30, (byte) 0x84, 0x00, (byte) 0xFF,
                                (byte) 0xFF, (byte) 0xFF },
                        BigInteger.valueOf(0xFFFFFF) }, };

        // positive testcases
        for (int i = 0; i < testcase.length; i++) {
            try {
                BerInputStream in = new BerInputStream(
                        new ByteArrayInputStream((byte[]) testcase[i][0]));

                int expected = ((BigInteger) testcase[i][1]).intValue();

                assertEquals(expected, in.getLength());
            } catch (IOException e) {
                e.printStackTrace();
                fail("Testcase: " + i + "\nUnexpected exception." + e);
            }
        }

        // negative testcase
        try {
            new BerInputStream(new ByteArrayInputStream(new byte[] { 0x30,
                    (byte) 0x84, 0x01, 0x01, 0x01, 0x01 }));
            fail("No expected ASN1Exception");
        } catch (ASN1Exception e) {
            assertTrue(e.getMessage().startsWith("Too long"));
        }

        //
        // Test for correct internal array reallocation
        // Regression for HARMONY-5054
        //

        // must be greater then buffer initial size (16K)
        int arrayLength = 17000;

        // 1 byte for tag and 3 for length
        byte[] encoding = new byte[arrayLength + 4];

        // fill tag and length bytes
        encoding[0] = ASN1Constants.TAG_OCTETSTRING;
        encoding[1] = (byte) 0x82; // length is encoded in two bytes
        encoding[2] = (byte) (arrayLength >> 8);
        encoding[3] = (byte) (arrayLength & 0xFF);

        BerInputStream in = new BerInputStream(new ByteArrayInputStream(
                encoding));
        assertEquals(encoding.length, in.getBuffer().length);
    }

    /**
     * @tests org.apache.harmony.security.asn1.BerInputStream#BerInputStream(byte[],
     *        int,int)
     */
    public void test_Ctor$LbyteLintLint() throws IOException {

        //
        // tests for 'expectedLength' parameter
        //
        byte[] encoded = new byte[] { 0x01, 0x01, 0x03, // boolean bytes
                0x06, 0x02, 0x01, 0x03, // oid bytes
                0x01, 0x00 // just random bytes
        };

        // pass boolean encoding
        BerInputStream in = new BerInputStream(encoded, 0, 3);
        assertEquals("boolean", 1, in.getLength());

        // pass oid encoding
        in = new BerInputStream(encoded, 3, 4);
        assertEquals("boolean", 2, in.getLength());

        // pass random encoding (equals to ANY)
        in = new BerInputStream(encoded, 7, 2);
        assertEquals("any", 0, in.getLength());

        // extra bytes for oid
        try {
            new BerInputStream(encoded, 3, 5);
            fail("No expected ASN1Exception");
        } catch (ASN1Exception e) {
            assertEquals("Wrong content length", e.getMessage());
        }

        // less bytes for oid
        try {
            new BerInputStream(encoded, 3, 3);
            fail("No expected ASN1Exception");
        } catch (ASN1Exception e) {
            assertEquals("Wrong content length", e.getMessage());
        }
    }

    /**
     * @tests org.apache.harmony.security.asn1.BerInputStream#readContent()
     */
    public void test_readContent() throws IOException {

        byte[] encoding = { ASN1Constants.TAG_OCTETSTRING, 0x0F, 0x01, 0x02,
                0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C,
                0x0D, 0x0E, 0x0F };

        // a custom input stream that doesn't return all data at once
        ByteArrayInputStream in = new ByteArrayInputStream(encoding) {
            public int read(byte[] b, int off, int len) {
                if (len < 2) {
                    return super.read(b, off, len);
                } else {
                    return super.read(b, off, 4);
                }

            }
        };

        BerInputStream berIn = new BerInputStream(in);
        berIn.readContent();

        assertTrue(Arrays.equals(encoding, berIn.getEncoded()));

        //
        // negative test case: the stream returns only 4 bytes of content
        //
        in = new ByteArrayInputStream(encoding) {

            int i = 0;

            public int read(byte[] b, int off, int len) {
                if (i == 0) {
                    i++;
                    return super.read(b, off, 4);
                } else {
                    return 0;
                }

            }
        };
        berIn = new BerInputStream(in);
        try {
            berIn.readContent();
            fail("No expected ASN1Exception");
        } catch (ASN1Exception e) {
        }
    }
}
