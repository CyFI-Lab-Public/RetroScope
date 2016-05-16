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
import org.apache.harmony.security.asn1.ASN1Constants;
import org.apache.harmony.security.asn1.ASN1Explicit;
import org.apache.harmony.security.asn1.ASN1SequenceOf;
import org.apache.harmony.security.asn1.ASN1Type;
import org.apache.harmony.security.asn1.DerInputStream;
import org.apache.harmony.security.asn1.DerOutputStream;

import junit.framework.TestCase;


/**
 * ASN.1 DER test for Explicitly tagged type
 *
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */

public class ExplicitTest extends TestCase {

    private static ASN1SequenceOf sequence = new ASN1SequenceOf(ASN1Boolean
            .getInstance());

    private static Object[][] taggedType;

    protected void setUp() throws Exception {
        super.setUp();

        taggedType = new Object[][] {
                //format: object to encode / ASN.1 tagged type / byte array

                //
                // Boolean = false
                //

                // [UNIVERSAL 5] Boolean
                new Object[] {
                        Boolean.FALSE,
                        new byte[] { 0x25, 0x03, 0x01, 0x01, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_UNIVERSAL, 5,
                                ASN1Boolean.getInstance()) },

                // [APPLICATION 5] Boolean
                new Object[] {
                        Boolean.FALSE,
                        new byte[] { 0x65, 0x03, 0x01, 0x01, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_APPLICATION, 5,
                                ASN1Boolean.getInstance()) },

                // [CONTEXT-SPECIFIC 5] Boolean
                new Object[] {
                        Boolean.FALSE,
                        new byte[] { (byte) 0xA5, 0x03, 0x01, 0x01, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_CONTEXTSPECIFIC,
                                5, ASN1Boolean.getInstance()) },

                // [5] Boolean (default = CONTEXT-SPECIFIC)
                new Object[] { Boolean.FALSE,
                        new byte[] { (byte) 0xA5, 0x03, 0x01, 0x01, 0x00 },
                        new ASN1Explicit(5, ASN1Boolean.getInstance()) },

                // [PRIVATE 5] Boolean
                new Object[] {
                        Boolean.FALSE,
                        new byte[] { (byte) 0xE5, 0x03, 0x01, 0x01, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_PRIVATE, 5,
                                ASN1Boolean.getInstance()) },

                //
                // Boolean = true
                //

                // [UNIVERSAL 5] Boolean
                new Object[] {
                        Boolean.TRUE,
                        new byte[] { 0x25, 0x03, 0x01, 0x01, (byte) 0xFF },
                        new ASN1Explicit(ASN1Constants.CLASS_UNIVERSAL, 5,
                                ASN1Boolean.getInstance()) },

                // [APPLICATION 5] Boolean
                new Object[] {
                        Boolean.TRUE,
                        new byte[] { 0x65, 0x03, 0x01, 0x01, (byte) 0xFF },
                        new ASN1Explicit(ASN1Constants.CLASS_APPLICATION, 5,
                                ASN1Boolean.getInstance()) },

                // [CONTEXT-SPECIFIC 5] Boolean
                new Object[] {
                        Boolean.TRUE,
                        new byte[] { (byte) 0xA5, 0x03, 0x01, 0x01, (byte) 0xFF },
                        new ASN1Explicit(ASN1Constants.CLASS_CONTEXTSPECIFIC,
                                5, ASN1Boolean.getInstance()) },

                // [5] Boolean (default = CONTEXT-SPECIFIC)
                new Object[] {
                        Boolean.TRUE,
                        new byte[] { (byte) 0xA5, 0x03, 0x01, 0x01, (byte) 0xFF },
                        new ASN1Explicit(ASN1Constants.CLASS_CONTEXTSPECIFIC,
                                5, ASN1Boolean.getInstance()) },

                // [PRIVATE 5] Boolean
                new Object[] {
                        Boolean.TRUE,
                        new byte[] { (byte) 0xE5, 0x03, 0x01, 0x01, (byte) 0xFF },
                        new ASN1Explicit(ASN1Constants.CLASS_PRIVATE, 5,
                                ASN1Boolean.getInstance()) },
                //
                // SequenceOf - testing constructed ASN.1 type
                //

                // [UNIVERSAL 5] SequenceOf
                new Object[] {
                        new ArrayList(),
                        new byte[] { 0x25, 0x02, 0x30, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_UNIVERSAL, 5,
                                sequence) },

                // [APPLICATION 5] SequenceOf
                new Object[] {
                        new ArrayList(),
                        new byte[] { 0x65, 0x02, 0x30, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_APPLICATION, 5,
                                sequence) },

                // [CONTEXT-SPECIFIC 5] SequenceOf
                new Object[] {
                        new ArrayList(),
                        new byte[] { (byte) 0xA5, 0x02, 0x30, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_CONTEXTSPECIFIC,
                                5, sequence) },

                // [5] SequenceOf (default = CONTEXT-SPECIFIC)
                new Object[] {
                        new ArrayList(),
                        new byte[] { (byte) 0xA5, 0x02, 0x30, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_CONTEXTSPECIFIC,
                                5, sequence) },

                // [PRIVATE 5] SequenceOf
                new Object[] {
                        new ArrayList(),
                        new byte[] { (byte) 0xE5, 0x02, 0x30, 0x00 },
                        new ASN1Explicit(ASN1Constants.CLASS_PRIVATE, 5,
                                sequence) } };
    }

    public void testDecode_Valid() throws IOException {

        for (int i = 0; i < taggedType.length; i++) {
            DerInputStream in = new DerInputStream((byte[]) taggedType[i][1]);
            assertEquals("Test case: " + i, taggedType[i][0],
                    ((ASN1Type) taggedType[i][2]).decode(in));
        }
    }

    //FIXME need testcase for decoding invalid encodings

    public void testEncode() throws IOException {

        for (int i = 0; i < taggedType.length; i++) {
            DerOutputStream out = new DerOutputStream(
                    (ASN1Type) taggedType[i][2], taggedType[i][0]);
            assertTrue("Test case: " + i, Arrays.equals(
                    (byte[]) taggedType[i][1], out.encoded));
        }
    }
}
