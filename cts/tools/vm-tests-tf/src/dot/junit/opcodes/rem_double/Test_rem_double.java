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

package dot.junit.opcodes.rem_double;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.rem_double.d.T_rem_double_1;
import dot.junit.opcodes.rem_double.d.T_rem_double_4;

public class Test_rem_double extends DxTestCase {

    /**
     * @title Arguments = 2.7d, 3.14d
     */
    public void testN1() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(2.7d, t.run(2.7d, 3.14d));
    }

    /**
     * @title Dividend = 0
     */
    public void testN2() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(0d, t.run(0, 3.14d));
    }

    /**
     * @title Dividend is negative
     */
    public void testN3() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(-0.43999999999999995d, t.run(-3.14d, 2.7d));
    }

    /**
     * @title Arguments = Double.MAX_VALUE, Double.NaN
     */
    public void testB1() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(Double.NaN, t.run(Double.MAX_VALUE, Double.NaN));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY,
     * Double.NEGATIVE_INFINITY
     */
    public void testB2() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(Double.NaN, t.run(Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY, -2.7d
     */
    public void testB3() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(Double.NaN, t.run(Double.POSITIVE_INFINITY, -2.7d));
    }

    /**
     * @title Arguments = -2.7d, Double.NEGATIVE_INFINITY
     */
    public void testB4() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(-2.7d, t.run(-2.7d, Double.NEGATIVE_INFINITY));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB5() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(Double.NaN, t.run(0, 0));
    }

    /**
     * @title Arguments = 0, -2.7
     */
    public void testB6() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(0d, t.run(0, -2.7d));
    }

    /**
     * @title Arguments = -2.7, 0
     */
    public void testB7() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(Double.NaN, t.run(-2.7d, 0));
    }

    /**
     * @title Arguments = 1, Double.MAX_VALUE
     */
    public void testB8() {
        T_rem_double_1 t = new T_rem_double_1();
        assertEquals(0d, t.run(1, Double.MIN_VALUE));
    }

    /**
     * @title Arguments = Double.MAX_VALUE, -1E-9d
     */
    public void testB9() {
        T_rem_double_1 t = new T_rem_double_1();

        assertEquals(1.543905285031139E-10d, t.run(Double.MAX_VALUE, -1E-9d));
    }

    /**
     * @constraint A24
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.rem_double.d.T_rem_double_2");
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
            Class.forName("dot.junit.opcodes.rem_double.d.T_rem_double_3");
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
            Class.forName("dot.junit.opcodes.rem_double.d.T_rem_double_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title types of arguments - double, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.rem_double.d.T_rem_double_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - double, long. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.rem_double.d.T_rem_double_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
