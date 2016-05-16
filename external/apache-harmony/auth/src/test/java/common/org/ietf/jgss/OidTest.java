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
* @author Alexander V. Esin
*/
package org.ietf.jgss;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * Tests Oid class
 *
 * ASN.1 encodings and string values are base on X.690 specification.
 *  
 * @see http://asn1.elibel.tm.fr/en/standards/index.htm
 */
public class OidTest extends TestCase {

    /**
     * Testing: Constructors, toString(), equals(), getDER() 
     */
    public void testValidOid() {
        Object[][] testcase = new Object[][] {
                //
                new Object[] { "0.0.3", new byte[] { 0x06, 0x02, 0x00, 0x03 } },
                //
                new Object[] { "0.1.3", new byte[] { 0x06, 0x02, 0x01, 0x03 } },
                //
                new Object[] { "0.39.3", new byte[] { 0x06, 0x02, 0x27, 0x03 } },
                //
                new Object[] { "1.0.3", new byte[] { 0x06, 0x02, 0x28, 0x03 } },
                //
                new Object[] { "1.2.1.2.1",
                        new byte[] { 0x06, 0x04, 0x2A, 0x01, 0x02, 0x01 } },
                //
                new Object[] { "1.39.3", new byte[] { 0x06, 0x02, 0x4F, 0x03 } },
                //
                new Object[] { "2.0.3", new byte[] { 0x06, 0x02, 0x50, 0x03 } },
                //
                new Object[] { "2.5.4.3",
                        new byte[] { 0x06, 0x03, 0x55, 0x04, 0x03 } },
                //
                new Object[] { "2.39.3", new byte[] { 0x06, 0x02, 0x77, 0x03 } },
                //
                //FIXME new Object[]{"2.40.3", new byte[]{0x06, 0x02, 0x78, 0x03}},
                //
                //FIXME new Object[]{"2.100.3", new byte[]{0x06, 0x03, (byte)0x81, 0x34,0x03}},
                //
                new Object[] {
                        "1.2.840.113554.1.2.2",
                        new byte[] { 0x06, 0x09, 0x2A, (byte) 0x86, 0x48,
                                (byte) 0x86, (byte) 0xF7, 0x12, 0x01, 0x02,
                                0x02 } } };

        for (int i = 0; i < testcase.length; i++) {

            String strOid = (String) testcase[i][0];
            byte[] enc = (byte[]) testcase[i][1];

            try {
                Oid oid1 = new Oid(strOid);
                Oid oid2 = new Oid(enc);
                Oid oid3 = new Oid(new ByteArrayInputStream(enc));

                // test equals
                assertEquals(oid1, oid2);
                assertEquals(oid1, oid3);
                assertEquals(oid2, oid3);

                // test toString()
                assertEquals(oid1.toString(), strOid);
                assertEquals(oid2.toString(), strOid);
                assertEquals(oid3.toString(), strOid);

                // test getDer
                assertTrue(Arrays.equals(enc, oid1.getDER()));
                assertTrue(Arrays.equals(enc, oid2.getDER()));
                assertTrue(Arrays.equals(enc, oid3.getDER()));

            } catch (GSSException e) {
                fail("Testcase: " + i + "\nUnexpected exception: " + e);
            }
        }
    }
    
    /**
     * @tests org.ieft.jgss.Oid#containedIn(org.ieft.jgss.Oid[])
     */
    public void testContainedIn() throws Exception {
        Oid oid= new Oid("1.2.1.2.1");
        Oid [] oidArr= new Oid [] { 
                new Oid("1.1.1.2.1"),
                new Oid("1.2.2.2.1"),
                new Oid("1.2.1.2.1"), //right
                new Oid("1.2.1.2.5")
                };
        assertTrue(oid.containedIn(oidArr) );

        try {
            oid.containedIn(null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }
    
    /**
     * Oid constructor Oid(String oid) is tested in case when
     * string oid contains a subidentifier with leading zero
     */
    public void testLeadingZero() throws Exception {
        Oid oid1 = new Oid("1.0.0.0.1");
        Oid oid2 = new Oid("01.0.0.0.1"); // subidentifier has leading 0 
        Oid oid3 = new Oid("1.0.0.0.01"); // subidentifier has leading 0 

        // testing equality
        assertEquals(oid1, oid2);
        assertEquals(oid1, oid3);

        // testing hash code
        assertEquals(oid1.hashCode(), oid2.hashCode());
        assertEquals(oid1.hashCode(), oid3.hashCode());
    }
    
    /**
     * Oid constructor Oid(String oid) is tested in case when
     * an invalid oid string is passed as a parameter
     */
    public void testInvalidOIDString() {

        String[] testcase = new String[] { "", // empty string
                "1", // only one subidentifier
                "SOME WRONG DATA", // char string
                "0.40.3", // second subidentifier > 40 when first subidentifier = 0
                "1.40.3", // second subidentifier > 40 when first subidentifier = 1
                "3.2.1", // first subidentifier > 2
                "2.2,1", // ',' is a delimiter
                "2..2.1", // double dot
                "2.2..1", // double dot
                ".2.2", // leading dot
                "2.2.", // trailing dot
                "A.2.1", // leading char 'A'
                "2.2.A", // trailing char 'A'
                "OID.2.2.1", // OID prefix
                "oid.2.2.1", // oid prefix
                "-2.2.1", // negative first subidentifier
                "2.-2.1", // negative second subidentifier
                "2.2.-1", // negative third subidentifier
        };

        for (int i = 0; i < testcase.length; i++) {
            try {
                new Oid(testcase[i]);
                fail("No expected GSSException for oid string: " + testcase[i]);
            } catch (GSSException e) {
                assertEquals(GSSException.FAILURE, e.getMajor());
                assertEquals(0, e.getMinor());
            }
        }
    }

    /**
     * Oid constructors Oid(byte[] oid) and Oid(InputStream oid)
     * are tested in case when an invalid oid encoding is passed as a parameter
     */
    public void testInvalidOIDEncodings() {

        byte[][] testcase = new byte[][] {
        // incorrect tag: MUST be 0x06
                new byte[] { 0x05, 0x03, 0x55, 0x04, 0x03 },
                // incorrect length: MUST be 0x03
                new byte[] { 0x06, 0x07, 0x55, 0x04, 0x03 },
                // incorrect length=0
                // FIXME new byte[] { 0x06, 0x00 },
                // incorrect last subidentifier(last octet): 8th bit is not 0 
                new byte[] { 0x06, 0x03, 0x55, 0x04, (byte) 0x83 }
        };

        // Testing: Oid(byte[] oid)
        for (int i = 0; i < testcase.length; i++) {
            try {
                new Oid(testcase[i]);
                fail("Byte array: no expected GSSException for testcase: " + i);
            } catch (GSSException e) {
                assertEquals(GSSException.FAILURE, e.getMajor());
                assertEquals(0, e.getMinor());
            }
        }

        // Testing: Oid(InputStream oid)
        for (int i = 0; i < testcase.length; i++) {
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(testcase[i]);
                new Oid(in);
                fail("Input stream: no expected GSSException for testcase: "
                        + i);
            } catch (GSSException e) {
                assertEquals(GSSException.FAILURE, e.getMajor());
                assertEquals(0, e.getMinor());
            }
        }
    }
    
    /**
     * Tests 2 cases of Encoding Rules violation.
     * Both cases should be in testInvalidOIDEncodings().
     */
    public void testEncodingRulesViolation() throws Exception {

        // incorrect last subidentifier: leading octet have value 0x80
        byte[] case1 = new byte[] { 0x06, 0x04, 0x55, 0x04, (byte) 0x80, 0x03 };
        // incorrect length: encoded not in minimum number of octets
        byte[] case2 = new byte[] { 0x06, (byte) 0x84, 0x00, 0x00, 0x00, 0x03,
                0x55, 0x04, 0x03 };

        assertEquals("2.5.4.3", new Oid(case1).toString());
        assertEquals("2.5.4.3", new Oid(case2).toString());
    }
    
    public void testImmutability() throws Exception {

        byte[] encoding = new byte[] { 0x06, 0x03, 0x55, 0x04, 0x03 };

        Oid oid = new Oid(encoding);

        byte[] enc1 = oid.getDER();
        byte[] enc2 = oid.getDER();

        assertTrue(enc1 != encoding);
        assertTrue(enc1 != enc2);
    }

    
    /**
     * Oid encoding contains 2 extra bytes.
     * Two Oid constructors are verified:
     *     - Oid(byte[] oid): GSSException is thrown
     *     - Oid(InputStream oid): oid object is created,
     *                             input stream contains extra bytes
     */
    public void testExtraBytes() throws Exception {
        byte[] encoding = new byte[] { 0x06, 0x01, 0x55, 0x04, 0x03 };

        try {
            new Oid(encoding);
            fail("No expected GSSException");
        } catch (GSSException e) {
            assertEquals(GSSException.FAILURE, e.getMajor());
            assertEquals(0, e.getMinor());
        }

        ByteArrayInputStream in = new ByteArrayInputStream(encoding);
        Oid oid = new Oid(in);

        assertEquals("2.5", oid.toString());

        assertEquals(0x04, in.read());
        assertEquals(0x03, in.read());
        assertEquals(0, in.available());
    }

    /**
     * @tests org.ieft.jgss.Oid#Oid(byte[])
     */
    public final void test_ConstructorLbyte_array() throws GSSException {

        try {
            new Oid((byte[]) null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests org.ieft.jgss.Oid#Oid(java.io.InputStream)
     */
    public final void test_ConstructorLjava_io_InputStream()
            throws GSSException {

        try {
            new Oid((java.io.InputStream) null);
            fail("No expected NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    /**
     * @tests org.ieft.jgss.Oid#Oid(java.lang.String)
     */
    public final void test_ConstructorLjava_lang_String() {

        try {
            new Oid((java.lang.String) null);
            fail("No expected GSSException");
        } catch (GSSException e) {
        }
    }
    
    public void test_KerberosV5() throws Exception {
        Oid oid = new Oid("1.2.840.113554.1.2.2");
        byte[] expectedDer = new byte[] { 6, 9, 42, -122, 72, -122, -9, 18, 1,
                2, 2 };
        assertTrue(Arrays.equals(expectedDer, oid.getDER()));
    }
}
