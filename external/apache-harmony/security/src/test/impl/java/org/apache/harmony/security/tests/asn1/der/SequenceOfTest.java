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
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.harmony.security.asn1.ASN1Boolean;
import org.apache.harmony.security.asn1.ASN1Exception;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for SequenceOf type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class SequenceOfTest extends TestCase {

    private static ASN1SequenceOf sequenceOf = new ASN1SequenceOf(ASN1Boolean
            .getInstance());

    //
    // Test Cases
    //

    private static Object[][] testcases = new Object[][] {
            // format: object to encode / byte array

            // sequence : empty sequence
            new Object[] { new ArrayList(), new byte[] { 0x30, 0x00 } },

            // sequence : false
            new Object[] { (new MyArray()).addMy(Boolean.FALSE),
                    new byte[] { 0x30, 0x03, 0x01, 0x01, 0x00 } },

            // sequence : true
            new Object[] { (new MyArray()).addMy(Boolean.TRUE),
                    new byte[] { 0x30, 0x03, 0x01, 0x01, (byte) 0xFF } },

            // sequence : true, false
            new Object[] {
                    (new MyArray()).addMy(Boolean.TRUE).addMy(Boolean.FALSE),
                    new byte[] { 0x30, 0x06, // sequence of
                            0x01, 0x01, (byte) 0xFF, // true
                            0x01, 0x01, 0x00 } //false
            },

    //TODO add testcase for another ASN.1 type`

    };

    public void testDecode_Valid() throws IOException {

        for (int i = 0; i < testcases.length; i++) {
            try {
                DerInputStream in = new DerInputStream((byte[]) testcases[i][1]);
                assertEquals("Test case: " + i, testcases[i][0], sequenceOf
                        .decode(in));
            } catch (ASN1Exception e) {
                fail("Test case: " + i + "\n" + e.getMessage());
            }
        }
    }

    //FIXME need testcase for decoding invalid encodings

    public void testEncode() throws IOException {

        for (int i = 0; i < testcases.length; i++) {
            DerOutputStream out = new DerOutputStream(sequenceOf,
                    testcases[i][0]);
            assertTrue("Test case: " + i, Arrays.equals(
                    (byte[]) testcases[i][1], out.encoded));
        }
    }

    public void testVerify() throws IOException {

        ASN1SequenceOf seqVerify = new ASN1SequenceOf(ASN1Boolean.getInstance()) {

            public Object getDecodedObject(BerInputStream in)
                    throws IOException {
                throw new IOException(
                        "Method getDecodedObject MUST not be invoked");
            }
        };

        for (int i = 0; i < testcases.length; i++) {
            DerInputStream in = new DerInputStream((byte[]) testcases[i][1]);
            in.setVerify();
            seqVerify.decode(in);
        }
    }

    //
    // Support class
    //
    public static class MyArray extends ArrayList {

        public MyArray addMy(Object o) {
            add(o);
            return this;
        }
    }
}
