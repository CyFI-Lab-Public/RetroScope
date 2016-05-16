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

/*
 * TODO
 * Two testcases, one in testDigestbyteArrayintint01() and one in testUpdatebyteArrayintint01(),
 * and testUpdatebyteArrayintint03() test are commented out because
 * current implementations of the MessageDigest and MessageDigestSpi classes
 * are not compatible with RI; see JIRA ## 1120 and 1148.
 */


package org.apache.harmony.security.tests.provider.crypto;


import java.security.MessageDigest;
import java.security.DigestException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/**
 * Tests against methods in MessageDigest class object using SHA1_MessageDigestImpl.
 */


public class SHA1_MessageDigestTest extends TestCase {


    /*
     * Data to compare with reference implementation;
     * they were received by getting hash values for 11 messages
     * whose length are in the LENGTHS array below.
     * Numbers in the LENGTH array cover diapason from 1 to 9999
     */
    private static final byte[][] HASHTOCOMPARE = {
                {
                (byte) 0xbf, (byte) 0x8b, (byte) 0x45, (byte) 0x30,     //  1 n=0
                (byte) 0xd8, (byte) 0xd2, (byte) 0x46, (byte) 0xdd,
                (byte) 0x74, (byte) 0xac, (byte) 0x53, (byte) 0xa1,
                (byte) 0x34, (byte) 0x71, (byte) 0xbb, (byte) 0xa1,
                (byte) 0x79, (byte) 0x41, (byte) 0xdf, (byte) 0xf7
                },{
                (byte) 0x80, (byte) 0x47, (byte) 0xf8, (byte) 0x24,     //  63 n=1
                (byte) 0x13, (byte) 0xbf, (byte) 0x4c, (byte) 0x0b,
                (byte) 0x6e, (byte) 0xfb, (byte) 0x6e, (byte) 0xa0,
                (byte) 0x91, (byte) 0xce, (byte) 0x08, (byte) 0x22,
                (byte) 0x02, (byte) 0x0d, (byte) 0x2e, (byte) 0xfc
                },{
                (byte) 0x92, (byte) 0xcb, (byte) 0x89, (byte) 0xdf,     //  64 n=2
                (byte) 0x62, (byte) 0xd9, (byte) 0x00, (byte) 0xb3,
                (byte) 0x50, (byte) 0xd9, (byte) 0x3e, (byte) 0x42,
                (byte) 0x25, (byte) 0xca, (byte) 0x6f, (byte) 0x08,
                (byte) 0x1d, (byte) 0x54, (byte) 0x7a, (byte) 0x28
                },{
                (byte) 0x70, (byte) 0x59, (byte) 0xd4, (byte) 0x34,     //  65 n=3
                (byte) 0xa3, (byte) 0xb6, (byte) 0x28, (byte) 0x25,
                (byte) 0x5c, (byte) 0x3e, (byte) 0xf8, (byte) 0xc8,
                (byte) 0x92, (byte) 0x83, (byte) 0x9a, (byte) 0xb3,
                (byte) 0xb9, (byte) 0x1c, (byte) 0x4f, (byte) 0xe6
                },{
                (byte) 0x03, (byte) 0x2f, (byte) 0x1c, (byte) 0x65,     // 639 n=4
                (byte) 0x44, (byte) 0xcc, (byte) 0x88, (byte) 0xf7,
                (byte) 0x34, (byte) 0xac, (byte) 0xad, (byte) 0xd3,
                (byte) 0xc4, (byte) 0xe2, (byte) 0x19, (byte) 0x32,
                (byte) 0xdf, (byte) 0x6c, (byte) 0x88, (byte) 0xfe
                },{
                (byte) 0xe0, (byte) 0x14, (byte) 0xe7, (byte) 0x5d,     // 640 n=5
                (byte) 0xb2, (byte) 0x8b, (byte) 0xe3, (byte) 0xbf,
                (byte) 0xf7, (byte) 0x6d, (byte) 0xe0, (byte) 0xe7,
                (byte) 0x35, (byte) 0xc3, (byte) 0x7e, (byte) 0xd0,
                (byte) 0xe7, (byte) 0xde, (byte) 0x85, (byte) 0x59
                },{
                (byte) 0x8f, (byte) 0xb7, (byte) 0x8c, (byte) 0x50,     // 641 n=6
                (byte) 0xf5, (byte) 0x57, (byte) 0xb1, (byte) 0x56,
                (byte) 0x84, (byte) 0xae, (byte) 0x07, (byte) 0x4c,
                (byte) 0x92, (byte) 0xc7, (byte) 0x05, (byte) 0xe3,
                (byte) 0xd0, (byte) 0xe8, (byte) 0x98, (byte) 0xe8
                },{
                (byte) 0x9c, (byte) 0x59, (byte) 0xea, (byte) 0x66,     // 6399 n=7
                (byte) 0xad, (byte) 0x49, (byte) 0xa5, (byte) 0xd8,
                (byte) 0x2b, (byte) 0x15, (byte) 0x7f, (byte) 0x3d,
                (byte) 0x5c, (byte) 0x8a, (byte) 0x4c, (byte) 0x16,
                (byte) 0x16, (byte) 0x70, (byte) 0x2c, (byte) 0x16
                },{
                (byte) 0x92, (byte) 0x3f, (byte) 0x57, (byte) 0xce,    // 6400 n=8
                (byte) 0x28, (byte) 0x5d, (byte) 0xb2, (byte) 0xd4,
                (byte) 0x1e, (byte) 0xdc, (byte) 0x86, (byte) 0x04,
                (byte) 0x50, (byte) 0x92, (byte) 0x2c, (byte) 0x2e,
                (byte) 0xaf, (byte) 0x15, (byte) 0xef, (byte) 0x93
                },{
                (byte) 0x48, (byte) 0x45, (byte) 0x78, (byte) 0x0a,    // 6401 n=9
                (byte) 0xf1, (byte) 0x9e, (byte) 0x08, (byte) 0x1f,
                (byte) 0x32, (byte) 0x43, (byte) 0x1d, (byte) 0xb5,
                (byte) 0x7b, (byte) 0x70, (byte) 0xdf, (byte) 0xe2,
                (byte) 0xfd, (byte) 0x38, (byte) 0x50, (byte) 0x39
                },{
                (byte) 0xbf, (byte) 0x35, (byte) 0x8f, (byte) 0xbe,    // 9999 n=10
                (byte) 0x88, (byte) 0xc5, (byte) 0x2d, (byte) 0x54,
                (byte) 0x4e, (byte) 0x3c, (byte) 0x72, (byte) 0xf6,
                (byte) 0xef, (byte) 0x68, (byte) 0xc3, (byte) 0x0c,
                (byte) 0xed, (byte) 0xb7, (byte) 0x0a, (byte) 0x36
                }
            };

    /*
     * array of numbers for which above data were calculated
     */
    private static final int[] LENGTHS = { 1, 63,64,65, 639,640,641, 6399,6400,6401, 9999 };

    private static final int DIGESTLENGTH = 20;      // implementation constants
    private static final int LENGTH       = 100;     //

    private static MessageDigest md;


    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        md = MessageDigest.getInstance("SHA-1", "Crypto");
    }


    /**
     * test against the "Object clone()" method;
     * test checks out that clone object has the same state that its origin
     */
    public final void testClone() throws CloneNotSupportedException {

        MessageDigest clone = null;

        byte digest[];            //   implementation variables
        byte digestClone[];       //

        byte[] bytes = new byte[LENGTH];
        for (int i = 0; i < bytes.length; i++ ) {
            bytes[i] = (byte) i;
        }

        md.update(bytes, 0, LENGTH);
        clone = (MessageDigest)md.clone();
        digest      = md.digest();
        digestClone = clone.digest();

        assertEquals("digest.length != digestClone.length :: " + digestClone.length,
                      digest.length, digestClone.length);
        for (int i = 0; i < digest.length; i++) {
            assertEquals("digest[i] != digestClone[i] : i=" + i, digest[i], digestClone[i]);
        }
    }


    /**
     * test against the "byte[] digest()" method;
     * it checks out that the method always return array of 20 bytes length
     */
    public final void testDigest01() {

        byte[] bytes = null;

        for ( int i = 0; i < LENGTH; i++ ) {

            byte[] b = new byte[i];
            for ( int j = 0; j < b.length; j++ ) {
                b[j] = (byte) j;
            }

            md.update(b, 0, b.length);
            bytes = md.digest();
            assertEquals("length of digest != DIGESTLENGTH", bytes.length, DIGESTLENGTH);
            if ( (i & 1) == 0 ) {
                md.reset();
            }
        }
    }


    /**
     * test against "byte[] digest()" method;
     * it checks out that for given seed arrays
     * hash arrays returned by the method are the same as in RI
     */
    public final void testDigest02() {

        int i = 0;
        byte[] results = null;

        for ( int n = 0 ; n < LENGTHS.length; n++ ) {

            for ( int j = 0; j < LENGTHS[n]; j++ ) {
                md.update((byte)(j +1));
            }
            results = md.digest();

            for ( int j = 0; j < DIGESTLENGTH; j++ ) {
                assertEquals("results[j] != HASHTOCOMPARE[i][j] :: n=" + n + " j=" + j +
                             " i=" + i, results[j], HASHTOCOMPARE[i][j]);
            }
            i++;
        }
    }

    /**
     * test against the "byte[] digest()" method;
     * it checks out that "digest()" method resets engine
     */
    public final void testDigest03() {

        byte[] bytes1 = null;
        byte[] bytes2 = null;

        for ( int i = 1; i < LENGTH; i++ ) {

            byte[] b = new byte[i];
            for ( int j = 0; j < b.length; j++ ) {
                b[j] = (byte) j;
            }

            md.update(b, 0, b.length);
            bytes1 = md.digest();

            md.update(b, 0, b.length);
            bytes2 = md.digest();

            assertEquals("bytes1.length != bytes2.length", bytes1.length, bytes2.length);
            for (int j = 0; j < DIGESTLENGTH ; j++ ) {
                assertEquals("no equality for i=" +i + " j=" +j, bytes1[j], bytes2[j]);
            }
        }
    }


    /**
     * test against "int digest(byte[], int, int)" method;
     * it checks out correct throwing exceptions
     */
    public final void testDigestbyteArrayintint01() throws DigestException {

        int offset = 0;
        int len    = 0;

        // IllegalArgumentException if byte[] is null
        try {
            md.digest(null, 0, DIGESTLENGTH);
            fail("digest(null, 0, DIGESTLENGTH) : No IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        // IllegalArgumentException if: (len + offset) <= buf.length
        for ( offset = -5; offset < 5 ; offset++) {

            try {
                md.digest(new byte[19], offset, 25);
                fail("md.digest(new byte[19],offset,25) :: offset=" + offset +
                           " ::  No IllegalArgumentException");
            } catch (IllegalArgumentException e) {
            }
        }
        for ( len = -5; len < 5 ; len++) {

            try {
                md.digest(new byte[19], 25, len);
                fail("md.digest(new byte[19],25,len) :: len=" + len +
                     " :: No IllegalArgumentException");
            } catch (IllegalArgumentException e) {
            }
        }

        // DigestException if len < DIGESTLENGTH
        for ( len = DIGESTLENGTH -1; len >=0 ; len-- ) {
            try {
                md.digest(new byte[DIGESTLENGTH], 0, len);
                fail("md.digest(new byte[DIGESTLENGTH], 0, len) :: len=" +
                      len + " :: No DigestException");
            } catch (DigestException e) {
            }
        }

        // ArrayIndexOutOfBoundsException if offset < 0
//        for ( offset = -5;  offset < 0 ; offset++ ) {
//            try {
//                md.digest(new byte[30], offset, DIGESTLENGTH);
//                fail("md.digest(new byte[30], offset, DIGESTLENGTH) :: " +
//                     "offset=" + offset + " :: no ArrayIndexOutOfBoundsException");
//            } catch (ArrayIndexOutOfBoundsException e) {
//            }
//        }
    }


    /**
     * test agains "int digest(byte[], int, int)" method
     * it checks out that for given seed arrays
     * hash arrays returned by the method are the same as in RI
     */
    public final void testDigestbyteArrayintint02() throws DigestException {

        byte[] bytes = null;
        int i = 0;

        for ( int n = 0 ; n < LENGTHS.length; n++ ) {
            byte[] results = new byte[DIGESTLENGTH];

            bytes = new byte[LENGTHS[n]];
            for ( int j = 0; j < bytes.length; j++ ) {
                bytes[j] = (byte)(j +1);
            }

            md.update(bytes, 0, bytes.length);
            md.digest(results, 0, DIGESTLENGTH);

            for ( int j = 0; j < DIGESTLENGTH; j++ ) {
                assertEquals("results[j] != HASHTOCOMPARE[i][j] :: " + " n=" + n + " j=" +
                             j + " i=" + i, results[j], HASHTOCOMPARE[i][j]);
            }
            i++;
        }
    }


    /**
     * test against "int engineDigest(byte[], int, int)" method
     * it checks out that engine gets reset after call to the method
     */
    public final void testDigestbyteArrayintint03() throws DigestException {

        byte[] bytes1 = new byte[DIGESTLENGTH];
        byte[] bytes2 = new byte[DIGESTLENGTH];

        for ( int n = 1 ; n < LENGTH; n++ ) {

            byte[] b = new byte[n];
            for ( int j = 0; j < b.length; j++ ) {
                b[j] = (byte) j;
            }
            md.update(b, 0, b.length);
            md.digest(bytes1, 0, DIGESTLENGTH);

            md.update(b, 0, b.length);
            md.digest(bytes2, 0, DIGESTLENGTH);

            assertEquals("bytes1.length != bytes2.length", bytes1.length, bytes2.length);
            for (int j = 0; j < DIGESTLENGTH ; j++ ) {
                assertEquals("no equality for j=" + j, bytes1[j], bytes2[j]);
            }
        }
    }


    /**
     * test against the "int GetDigestLength()" method;
     * it checks out that the method returns the same value
     * regardless of previous calls to other methods
     */
    public final void testGetDigestLength() throws DigestException {

        int digestlength = md.getDigestLength();

        byte[] bytes = new byte[LENGTH];

        for (int i = 0; i < LENGTH; i++ ) {
            bytes[i] = (byte)(i&0xFF);
        }

        for ( int i = 0; i < 8; i++ ) {
            switch (i) {
                case 0: md.digest();
                        break;
                case 1: md.digest(bytes, 0, bytes.length);
                        break;
                case 2: md.reset();
                        break;
                case 3: md.update(bytes[i]);
                        break;
                case 4: md.update(bytes, 0, LENGTH);
                        break;
                case 5: md.update(bytes[i]);
                        break;
                default:
            }
            assertEquals(" !=digestlength", md.getDigestLength(), digestlength);
        }
    }


    /**
     * test against the "void engineReset()" method;
     * it checks out that digests returned after proceeding "engineReset()"
     * is the same as in case of initial digest
     */
    public final void testReset() {

        byte[] bytes = new byte[LENGTH];
        byte[] digest1 = null;
        byte[] digest2 = null;
        boolean flag;

        for (int i = 0; i < bytes.length; i++ ) {
            bytes[i] = (byte)(i&0xFF);
        }

        digest1 = md.digest();
        md.update(bytes, 0, LENGTH);
        md.reset();
        digest2 = md.digest();

        flag = true;
        for ( int i = 0; i < digest1.length; i++ ) {
            flag &= digest1[i] == digest2[i];
        }
        assertTrue("digests are not the same", flag);
    }


    /**
     * test against the "void update(byte)" method;
     * it checks out that if one digest is update to its forerunner
     * the digests are not the same
     */
    public final void testUpdatebyte() {

        boolean flag;
        byte[] digest1 = null;
        byte[] digest2 = null;

        for ( int j = 0; j < LENGTH ; j++) {

            byte[] bytes = new byte[j];

            for (int i = 0; i < bytes.length; i++ ) {
                bytes[i] = (byte)(i&0xFF);
            }

            md.update(bytes, 0, bytes.length);
            digest1 = md.digest();
            md.update(bytes, 0, bytes.length);
            md.update((byte) 0);
            digest2 = md.digest();

            flag = true;
            for ( int i = 0; i < digest1.length; i++ ) {
                flag &= digest1[i] == digest2[i];
            }
            assertFalse("digests are the same", flag);
        }
    }


    /**
     * test against the "void update(byte[], int, int)" method;
     * it checks out throwing exceptions
     */
    public final void testUpdatebyteArrayintint01() {

        // IllegalArgumentException if byte[] is null
        try {
            md.update(null, 0, 0);
            fail("TESTCASE1 : update(null, 0, 0) :: No IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }

        // ArrayIndexOutOfBoundsException if len>0 && offset < 0
//        try {
//            md.update(new byte[0], -1, 1);
//            fail("update(new byte[0], -1, 1) : No ArrayIndexOutOfBoundsException");
//        } catch (ArrayIndexOutOfBoundsException e) {
//        }

        // IllegalArgumentException if (offset + len) >= input.length
        try {
            md.update(new byte[1], 0, 2);
            fail("update(new byte[1], 0, 2) : No IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }


    /**
     * test against the "void update(byte[],int,int)" method;
     * it checks out that two sequential digest,
     * second is byte array update to first,
     * are different, provided length of the byte array > 0.
     */
    public final void testUpdatebyteArrayintint02() {

        boolean flag;
        byte[] digest1 = null;
        byte[] digest2 = null;

        for ( int j = 0; j < LENGTH ; j++) {

            byte[] bytes = new byte[j];

            for (int i = 0; i < bytes.length; i++ ) {
                bytes[i] = (byte)(i&0xFF);
            }

            md.update((byte) 0);
            digest1 = md.digest();
            md.update((byte) 0);
            md.update(bytes, 0, bytes.length);
            digest2 = md.digest();

            flag = true;
            for ( int i = 0; i < digest1.length; i++ ) {
                flag &= digest1[i] == digest2[i];
            }
            if ( j == 0 ) {
                // no update if byte[] of zero length
                assertTrue("ATTENTION: digests are not the same", flag);
            } else {
                assertFalse("ATTENTION: digests are the same; j=" +j, flag);
            }
        }
    }


    /**
     * test against the "void update(byte[], int, int)" method;
     * it checks that if the "len" argument <=0 the method just returns with
     * no affect to MessageDigestImp object (note that such behavior complies with RI).
     */
//    public final void testUpdatebyteArrayintint03() {
//
//        boolean flag;
//        byte[] digest1 = null;
//        byte[] digest2 = null;
//
//        for ( int j = 0; j < LENGTH ; j++) {
//
//            byte[] bytes = new byte[j];
//
//            for (int i = 0; i < bytes.length; i++ ) {
//                bytes[i] = (byte)(i&0xFF);
//            }
//
//            md.update(bytes, 0, bytes.length);
//            digest1 = md.digest();
//            md.update(bytes, 0, bytes.length);
//            md.update(new byte[1], 0, -1);
//            md.update(new byte[1], 0, 0);
//            digest2 = md.digest();
//
//            assertTrue("digest's lengths are different : length1=" +
//                       digest1.length + " length2=" + digest2.length,
//                       digest1.length == digest2.length );
//
//            for ( int i = 0; i < digest1.length ; i++ ) {
//                assertTrue("different digests : i=" +i +
//                           " digest1[i]=" + digest1[i] + " digest2[i]=" + digest2[i],
//                           digest1[i] == digest2[i] );
//            }
//        }
//    }


    public static Test suite() {
        return new TestSuite(SHA1_MessageDigestTest.class);
    }

 }
