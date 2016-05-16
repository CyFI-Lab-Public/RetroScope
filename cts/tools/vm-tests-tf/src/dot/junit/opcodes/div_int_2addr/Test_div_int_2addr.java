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

package dot.junit.opcodes.div_int_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_1;
import dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_5;

public class Test_div_int_2addr extends DxTestCase {
    /**
     * @title Arguments = 8, 4
     */
    public void testN1() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(2, t.run(8, 4));
    }

    /**
     * @title Rounding
     */
    public void testN2() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(268435455, t.run(1073741823, 4));
    }

    /**
     * @title Dividend = 0
     */
    public void testN3() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(0, t.run(0, 4));
    }

    /**
     * @title Dividend is negative
     */
    public void testN4() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(-3, t.run(-10, 3));
    }

    /**
     * @title Divisor is negative
     */
    public void testN5() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(-357913941, t.run(1073741824, -3));
    }

    /**
     * @title Both Dividend and divisor are negative
     */
    public void testN6() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(5965, t.run(-17895697, -3000));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, -1
     */
    public void testB1() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        // result is MIN_VALUE because overflow occurs in this case
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MIN_VALUE, -1));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, 1
     */
    public void testB2() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MIN_VALUE, 1));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, 1
     */
    public void testB3() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(Integer.MAX_VALUE, t.run(Integer.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, Integer.MAX_VALUE
     */
    public void testB4() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(-1, t.run(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = 1, Integer.MAX_VALUE
     */
    public void testB5() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(0, t.run(1, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = 1, Integer.MIN_VALUE
     */
    public void testB6() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        assertEquals(0, t.run(1, Integer.MIN_VALUE));
    }

    /**
     * @title Divisor is 0
     */
    public void testE1() {
        T_div_int_2addr_1 t = new T_div_int_2addr_1();
        try {
            t.run(1, 0);
            fail("expected ArithmeticException");
        } catch (ArithmeticException ae) {
            // expected
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - int, double
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - long, int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - reference, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - int, float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.div_int_2addr.d.T_div_int_2addr_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
