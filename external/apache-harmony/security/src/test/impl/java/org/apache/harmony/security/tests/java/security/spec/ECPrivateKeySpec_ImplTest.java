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
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.EllipticCurve;

import junit.framework.TestCase;

/**
 * Tests for <code>ECPrivateKeySpec</code> class fields and methods.
 * 
 */
public class ECPrivateKeySpec_ImplTest extends TestCase {

    //
    // Tests
    //
    // NOTE: the following tests use EC domain parameters
    // which are invalid for real EC cryptography application
    // but must be acceptable by the class under test according
    // to the API specification
    //

    /**
     * Test #1 for <code>ECPrivateKeySpec(BigInteger, ECParameterSpec)</code> constructor<br> 
     * Assertion: creates <code>ECPrivateKeySpec</code> instance<br>
     * Test preconditions: valid parameters passed<br>
     * Expected: must pass without any exceptions
     */
    public final void testECPrivateKeySpec01() {
        // Valid (see note below) parameters set
        EllipticCurve c =
            new EllipticCurve(new ECFieldFp(BigInteger.valueOf(5L)),
                              BigInteger.ZERO,
                              BigInteger.valueOf(4L));
        ECPoint g = new ECPoint(BigInteger.ZERO, BigInteger.valueOf(2L));
        new ECPrivateKeySpec(BigInteger.ZERO,
                new ECParameterSpec(c, g, BigInteger.valueOf(5L), 10));
        
    }

   /**
     * Test #2 for <code>ECPrivateKeySpec(BigInteger, ECParameterSpec)</code> constructor<br> 
     * Assertion: throws <code>NullPointerException</code> if
     * <code>s</code> or <code>params</code> is <code>null</code><br>
     * Test preconditions: pass <code>null</code> as mentioned parameters<br>
     * Expected: must throw <code>NullPointerException</code>
     */
    public final void testECPrivateKeySpec02() {
        // Valid (see note below) parameters set
        EllipticCurve c =
            new EllipticCurve(new ECFieldFp(BigInteger.valueOf(5L)),
                              BigInteger.ZERO,
                              BigInteger.valueOf(4L));
        ECPoint g = new ECPoint(BigInteger.ZERO, BigInteger.valueOf(2L));

        // Test case 1: s is null
        try {
            new ECPrivateKeySpec(null,
                new ECParameterSpec(c, g, BigInteger.valueOf(5L), 10));
            fail("#1: Expected NPE not thrown");
        } catch (NullPointerException ok) {
        }


        // Test case 2: params is null
        try {
            new ECPrivateKeySpec(BigInteger.valueOf(0L), null);
            fail("#2: Expected NPE not thrown");
        } catch (NullPointerException ok) {
        }


        // Test case 3: both s and params are null
        try {
            new ECPrivateKeySpec(null, null);
            fail("#3: Expected NPE not thrown");
        } catch (NullPointerException ok) {
        }
    }

    /**
     * Test for <code>getParams()</code> method<br>
     * Assertion: returns associated EC parameters<br>
     * Test preconditions: <code>ECPrivateKeySpec</code> instance
     * created using valid parameters<br>
     * Expected: must return params value which is equal
     * to the one passed to the constructor; (both must refer
     * the same object)
     */
    public final void testGetParams() {
        // Valid (see note below) parameters set
        EllipticCurve c =
            new EllipticCurve(new ECFieldFp(BigInteger.valueOf(5L)),
                              BigInteger.ZERO,
                              BigInteger.valueOf(4L));
        ECPoint g = new ECPoint(BigInteger.ZERO, BigInteger.valueOf(2L));
        ECParameterSpec params =
            new ECParameterSpec(c, g, BigInteger.valueOf(5L), 10);

        ECPrivateKeySpec ks = new ECPrivateKeySpec(BigInteger.ZERO, params);
        ECParameterSpec paramsRet = ks.getParams();
        
        assertEquals(params, paramsRet);
        assertSame(params, paramsRet);
    }

    /**
     * Test for <code>getS()</code> method<br>
     * Assertion: returns associated private value<br>
     * Test preconditions: <code>ECPrivateKeySpec</code> instance
     * created using valid parameters<br>
     * Expected: must return s value which is equal
     * to the one passed to the constructor; (both must refer
     * the same object)
     */
    public final void testGetS() {
        // Valid (see note below) parameters set
        EllipticCurve c =
            new EllipticCurve(new ECFieldFp(BigInteger.valueOf(5L)),
                              BigInteger.ZERO,
                              BigInteger.valueOf(4L));
        ECPoint g = new ECPoint(BigInteger.ZERO, BigInteger.valueOf(2L));
        ECParameterSpec params =
            new ECParameterSpec(c, g, BigInteger.valueOf(5L), 10);
        BigInteger s = BigInteger.valueOf(5L);

        ECPrivateKeySpec ks = new ECPrivateKeySpec(s, params);
        BigInteger sRet = ks.getS();

        assertEquals(s, sRet);
        assertSame(s, sRet);
    }
}
