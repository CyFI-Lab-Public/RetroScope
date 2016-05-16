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
import java.security.spec.ECFieldFp;
import java.security.spec.EllipticCurve;

import junit.framework.TestCase;

/**
 * Tests for <code>EllipticCurve</code> class fields and methods.
 * 
 */
public class EllipticCurve_ImplTest extends TestCase {

    /**
     * Test #2 for <code>equals(Object other)</code> method<br>
     * Assertion: return false if this and other objects are not equal<br>
     * Test preconditions: see test comments<br>
     * Expected: all objects in this test must be NOT equal
     */
    public final void testEqualsObject02() {
        // test case 1: must not be equal to null
        EllipticCurve c2=null, c1 =
            new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                    BigInteger.ONE,
                    BigInteger.valueOf(19L));
        assertFalse(c1.equals(c2));

        // test case 2: not equal objects - field
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L));
        c2 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(31L)),
                BigInteger.valueOf(1L),
                BigInteger.valueOf(19L));
        assertFalse(c1.equals(c2) || c2.equals(c1));

        // test case 3: not equal objects - a
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L));
        c2 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ZERO,
                BigInteger.valueOf(19L));
        assertFalse(c1.equals(c2) || c2.equals(c1));

        // test case 4: not equal objects - b
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L));
        c2 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(17L));
        assertFalse(c1.equals(c2) || c2.equals(c1));

        // test case 5: not equal objects - both seed not null
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L),
                new byte[24]);
        c2 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.valueOf(1L),
                BigInteger.valueOf(19L),
                new byte[25]);
        assertFalse(c1.equals(c2) || c2.equals(c1));

        // test case 6: not equal objects - one seed is null
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L),
                null);
        c2 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.valueOf(1L),
                BigInteger.valueOf(19L),
                new byte[24]);
        assertFalse(c1.equals(c2) || c2.equals(c1));

        // test case 7: not equal objects - field class
        c1 = new EllipticCurve(new ECFieldFp(BigInteger.valueOf(23L)),
                BigInteger.ONE,
                BigInteger.valueOf(19L),
                new byte[24]);
        c2 = new EllipticCurve(new ECFieldF2m(5),
                BigInteger.ONE,
                BigInteger.valueOf(19L),
                new byte[24]);
        assertFalse(c1.equals(c2) || c2.equals(c1));
    }
}
