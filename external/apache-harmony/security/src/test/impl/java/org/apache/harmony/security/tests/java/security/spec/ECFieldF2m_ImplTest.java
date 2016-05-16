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

package org.apache.harmony.security.tests.java.security.spec;

import java.math.BigInteger;
import java.security.spec.ECFieldF2m;

import junit.framework.TestCase;

/**
 * Tests for <code>ECFieldF2m</code> class fields and methods.
 * 
 */
public class ECFieldF2m_ImplTest extends TestCase {

    /**
     * Support class for this test.
     * Encapsulates <code>ECFieldF2m</code> testing
     * domain parameters. 
     * 
     */
    private static final class ECFieldF2mDomainParams {

        /**
         * <code>NPE</code> reference object of class NullPointerException.
         * NullPointerException must be thrown by <code>ECFieldF2m</code>
         * ctors in some circumstances
         */
        static final NullPointerException NPE = new NullPointerException();
        /**
         * <code>IArgE</code> reference object of class IllegalArgumentException.
         * IllegalArgumentException must be thrown by <code>ECFieldF2m</code>
         * ctors in some circumstances
         */
        static final IllegalArgumentException IArgE = new IllegalArgumentException();
        
        /**
         * The <code>m</code> parameter for <code>ECFieldF2m</code>
         * ctor for the current test.
         */
        final int m;
        /**
         * The <code>rp</code> parameter for <code>ECFieldF2m</code>
         * ctor for the current test.
         */
        final BigInteger rp;
        /**
         * The <code>ks</code> parameter for <code>ECFieldF2m</code>
         * ctor for the current test.
         */
        final int[] ks;
        
        
        /**
         * Exception expected with this parameters set or <code>null</code>
         * if no exception expected.
         */
        final Exception x;
        
        /**
         * Constructs ECFieldF2mDomainParams
         * 
         * @param m
         * @param rp
         * @param ks
         * @param expectedException
         */
        ECFieldF2mDomainParams(final int m,
                final BigInteger rp,
                final int[] ks,
                final Exception expectedException) {
            this.m = m;
            this.rp = rp;
            this.ks = ks;
            this.x = expectedException;
        }
    }


    /**
     * Set of parameters used for <code>ECFieldF2m(int, BigInteger)</code>
     * constructor tests.
     */
    private final ECFieldF2mDomainParams[] intBigIntegerCtorTestParameters =
        new ECFieldF2mDomainParams[] {
            // set 0: valid m and rp - trinomial basis params
            new ECFieldF2mDomainParams(
                    1999,
                    BigInteger.valueOf(0L).setBit(0).setBit(367).setBit(1999),
                    null,
                    null),
            // set 1: valid m and rp - pentanomial basis params
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(1).setBit(2).setBit(981).setBit(2000),
                    null,
                    null),
            // set 2: valid m, invalid (null) rp
            new ECFieldF2mDomainParams(
                    1963,
                    (BigInteger)null,
                    null,
                    ECFieldF2mDomainParams.NPE),
            // set 3: valid m, invalid rp - bit 0 not set
            new ECFieldF2mDomainParams(
                    1999,
                    BigInteger.valueOf(0L).setBit(1).setBit(367).setBit(1999),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 4: valid m, invalid rp - bit m not set
            new ECFieldF2mDomainParams(
                    1999,
                    BigInteger.valueOf(0L).setBit(0).setBit(367).setBit(1998),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 5: valid m, invalid rp - bit k improperly set
            new ECFieldF2mDomainParams(
                    1999,
                    BigInteger.valueOf(0L).setBit(0).setBit(2367).setBit(1999),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 6: valid m, invalid rp - k1 k2 k3
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(2001).setBit(2002).setBit(2003).setBit(2000),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 7: valid m, invalid rp - number of bits set
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(2000),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 8: valid m, invalid rp - number of bits set
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(1).setBit(2).setBit(2000),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 9: valid m, invalid rp - number of bits set
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(1).setBit(2).
                                           setBit(981).setBit(985).setBit(2000),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 10: valid m, invalid rp
            new ECFieldF2mDomainParams(
                    2000,
                    BigInteger.valueOf(0L),
                    null,
                    ECFieldF2mDomainParams.IArgE),
            // set 11: invalid m
            new ECFieldF2mDomainParams(
                    -2000,
                    BigInteger.valueOf(0L).setBit(0).setBit(1).setBit(2).
                    setBit(981).setBit(2000),
                    null,
                    ECFieldF2mDomainParams.IArgE),
        };  

    /**
     * Tests for constructor <code>ECFieldF2m(int, BigInteger)</code><br>
     * 
     * Assertion: constructs new <code>ECFieldF2m</code> object
     * using valid parameters m and rp. rp represents trinomial basis.
     * 
     * Assertion: constructs new <code>ECFieldF2m</code> object
     * using valid parameters m and rp. rp represents pentanomial basis.
     * 
     * Assertion: IllegalArgumentException if m is not positive.
     * 
     * Assertion: NullPointerException if rp is null.
     * 
     * Assertion: IllegalArgumentException if rp is invalid.
     */
    public final void testECFieldF2mintBigInteger() {
        for(int i=0; i<intBigIntegerCtorTestParameters.length; i++) {
            ECFieldF2mDomainParams tp = intBigIntegerCtorTestParameters[i];
            try {
                // perform test
                new ECFieldF2m(tp.m, tp.rp);
                
                if (tp.x != null) {
                    // exception has been expected 
                    fail(getName() + ", set " + i +
                            " FAILED: expected exception has not been thrown");
                }
            } catch (Exception e){
                if (tp.x == null || !e.getClass().isInstance(tp.x)) {
                    // exception: failure
                    // if it has not been expected
                    // or wrong one has been thrown
                    fail(getName() + ", set " + i +
                            " FAILED: unexpected " + e);
                }
            }
        }
    }
}
