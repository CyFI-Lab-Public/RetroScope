/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dot.junit.opcodes.mul_double_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_1;
import dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_4;

public class Test_mul_double_2addr extends DxTestCase {

    /**
     * @title Arguments = 2.7d, 3.14d
     */

    public void testN1() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(8.478000000000002d, t.run(2.7d, 3.14d));
    }

    /**
     * @title Arguments = 0, -3.14d
     */
    public void testN2() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(-0d, t.run(0, -3.14d));
    }

    /**
     * @title Arguments = -2.7d, -3.14d
     */
    public void testN3() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(8.478000000000002d, t.run(-3.14d, -2.7d));
    }

    /**
     * @title Arguments = Double.MAX_VALUE, Double.NaN
     */
    public void testB1() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(Double.NaN, t.run(Double.MAX_VALUE, Double.NaN));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY, 0
     */
    public void testB2() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(Double.NaN, t.run(Double.POSITIVE_INFINITY, 0));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY, -2.7d
     */
    public void testB3() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(Double.NEGATIVE_INFINITY, t.run(Double.POSITIVE_INFINITY,
                -2.7d));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY,
     * Double.NEGATIVE_INFINITY
     */
    public void testB4() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(Double.NEGATIVE_INFINITY, t.run(Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY));
    }

    /**
     * @title Arguments = +0, -0d
     */
    public void testB5() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(-0d, t.run(+0d, -0d));
    }

    /**
     * @title Arguments = -0d, -0d
     */
    public void testB6() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(+0d, t.run(-0d, -0d));
    }

    /**
     * @title Arguments = Double.MAX_VALUE, Double.MAX_VALUE
     */
    public void testB7() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(Double.POSITIVE_INFINITY, t.run(Double.MAX_VALUE,
                Double.MAX_VALUE));
    }

    /**
     * @title Arguments = Double.MIN_VALUE, -1.4E-45f
     */
    public void testB8() {
        T_mul_double_2addr_1 t = new T_mul_double_2addr_1();
        assertEquals(-0d, t.run(Double.MIN_VALUE, -1.4E-45f));
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - float, double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title types of arguments - double, reference
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - long, double. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.mul_double_2addr.d.T_mul_double_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
