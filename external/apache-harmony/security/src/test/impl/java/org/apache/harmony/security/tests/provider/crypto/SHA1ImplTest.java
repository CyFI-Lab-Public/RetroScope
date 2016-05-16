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


package org.apache.harmony.security.tests.provider.crypto;


import java.io.UnsupportedEncodingException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.harmony.security.provider.crypto.SHA1Impl;

import java.security.MessageDigest;


/**
 * Tests against methods in SHA1Impl class.
 * The input data and results of computing are defined in Secure Hash Standard,
 * see http://www.itl.nist.gov/fipspubs/fip180-1.htm
 */


public class SHA1ImplTest extends TestCase {


    // SHA1Data constant used in below methods
    private static final int INDEX = SHA1Impl.BYTES_OFFSET;

    private static MessageDigest md;


    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        md = MessageDigest.getInstance("SHA-1", "Crypto");
    }


    /*
     * The test checks out that for given three byte input
     * a value returned by SHA1Impl is equal to both :
     * - one defined in the Standard and
     * - one calculated with alternative computation algorithm defined in the Standard.
     */
    public final void testOneBlockMessage() {

        int[] words = new int[INDEX +6];	// working array to compute hash

        // values defined in examples in Secure Hash Standard
        int[] hash1 = {0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };
        int[] hash  = {0xA9993E36, 0x4706816A, 0xBA3E2571, 0x7850C26C, 0x9CD0D89D };

        for (int i = 0; i < words.length; i++ ) {
            words[i] = 0;
        }
        words[0]  = 0x61626380;    // constants from Secure Hash Standard
        words[15] = 0x00000018;

        alternateHash(words, hash1);


        md.update(new byte[]{0x61,0x62,0x63});
        byte[] dgst = md.digest();

        for ( int k = 0; k < 5; k++ ) {
            int i = k*4;

            int j = ((dgst[i  ]&0xff)<<24) | ((dgst[i+1]&0xff)<<16) |
                    ((dgst[i+2]&0xff)<<8 ) | (dgst[i+3]&0xff)  ;

            assertTrue("false1: k=" + k + " hash1[k]=" + Integer.toHexString(hash1[k]),
                       hash[k] == hash1[k] );

            assertTrue("false2: k=" + k + " j=" + Integer.toHexString(j), hash[k] == j );
        }
    }




    /*
     * The test checks out that SHA1Impl computes correct value
     * if data supplied takes exactly fourteen words of sixteen word buffer.
     */
    public final void testMultiBlockMessage() throws UnsupportedEncodingException {

        // values defined in examples in Secure Hash Standard
        int[] hash  = {0x84983e44, 0x1c3bd26e, 0xbaae4aa1, 0xf95129e5, 0xe54670f1 };

        // string defined in examples in Secure Hash Standard
        md.update("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".getBytes("UTF-8"));
        byte[] dgst = md.digest();

        for ( int k = 0; k < 5; k++ ) {
            int i = k*4;

                int j = ((dgst[i  ]&0xff)<<24) | ((dgst[i+1]&0xff)<<16) |
                        ((dgst[i+2]&0xff)<<8 ) | (dgst[i+3]&0xff)  ;

            assertTrue("false: k=" + k + " j=" + Integer.toHexString(j), hash[k] == j );
        }
    }


    /*
     * The test checks out that SHA1Impl returns correct values
     * for four different cases of infilling internal buffer and computing intermediate hash.
     */
    public final void testLongMessage() {

        // values defined in examples in Secure Hash Standard
        int[] hash  = {0x34aa973c, 0xd4c4daa4, 0xf61eeb2b, 0xdbad2731, 0x6534016f };

        byte msgs[][] = new byte[][] { {0x61},
                                       {0x61, 0x61},
                                       {0x61, 0x61, 0x61},
                                       {0x61, 0x61, 0x61, 0x61} };

        int lngs[] = new int[]{1000000, 500000, 333333, 250000};

        for ( int n = 0; n < 4; n++ ) {

            for ( int i = 0; i < lngs[n]; i++) {
                md.update(msgs[n]);
            }
            if ( n == 2 ) {
                md.update(msgs[0]);
            }

            byte[] dgst = md.digest();
            for ( int k = 0; k < 5; k++ ) {
                int i = k*4;

                int j = ((dgst[i  ]&0xff)<<24) | ((dgst[i+1]&0xff)<<16) |
                        ((dgst[i+2]&0xff)<<8 ) | (dgst[i+3]&0xff)  ;

                assertTrue("false: n =" + n + "  k=" + k + " j" + Integer.toHexString(j),
                            hash[k] == j );
            }
        }
    }


    /**
     * implements alternative algorithm described in the SECURE HASH STANDARD
     */
    private void alternateHash(int[] bufW, int[] hash) {

        // constants defined in Secure Hash Standard
        final int[] K = {

            0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999,
            0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999,
            0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999,
            0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999,
            0x5A827999, 0x5A827999, 0x5A827999, 0x5A827999,

            0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1,
            0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1,
            0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1,
            0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1,
            0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1, 0x6ED9EBA1,

            0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC,
            0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC,
            0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC,
            0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC,
            0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC, 0x8F1BBCDC,

            0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6,
            0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6,
            0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6,
            0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6,
            0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6, 0xCA62C1D6
        };

        int  a = hash[0]; //0x67452301 ;
        int  b = hash[1]; //0xEFCDAB89 ;
        int  c = hash[2]; //0x98BADCFE ;
        int  d = hash[3]; //0x10325476 ;
        int  e = hash[4]; //0xC3D2E1F0 ;

        // implementation constant and variables

        final int MASK = 0x0000000F;
        int temp;
        int s;
        int tmp;

        // computation defined in Secure Hash Standard
        for ( int t = 0 ; t < 80 ; t++ ) {

            s = t & MASK;

            if ( t >= 16) {

                tmp = bufW[ (s+13)&MASK ] ^ bufW[(s+8)&MASK ] ^ bufW[ (s+2)&MASK ] ^ bufW[s];
                bufW[s] = ( tmp<<1 ) | ( tmp>>>31 );
            }

            temp = ( a << 5 ) | ( a >>> 27 );

            if ( t < 20 ) {
                temp += ( b & c ) | ( (~b) & d ) ;
            } else if ( t < 40 ) {
                temp += b ^ c ^ d ;
            } else if ( t < 60 ) {
                temp += ( b & c ) | ( b & d ) | ( c & d ) ;
            } else {
                temp += b ^ c ^ d ;
            }

            temp += e + bufW[s] + K[t] ;
            e = d;
            d = c;
            c = ( b<<30 ) | ( b>>>2 ) ;
            b = a;
            a = temp;
        }
        hash[0] += a;
        hash[1] += b;
        hash[2] += c;
        hash[3] += d;
        hash[4] += e;
    }


    public static Test suite() {
        return new TestSuite(SHA1ImplTest.class);
    }

 }
