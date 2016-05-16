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

package dot.junit.opcodes.rem_long_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_1;
import dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_3;

public class Test_rem_long_2addr extends DxTestCase {

    /**
     * @title Arguments = 10000000000l, 4000000000l
     */
    public void testN1() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(2000000000l, t.run(10000000000l, 4000000000l));
    }

    /**
     * @title Arguments = 1234567890123l, 123456789l
     */
    public void testN2() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(123l, t.run(1234567890123l, 123456789l));
    }

    /**
     * @title Dividend = 0
     */
    public void testN3() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(0l, t.run(0l, 1234567890123l));
    }

    /**
     * @title Dividend is negative
     */
    public void testN4() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(-2000000000l, t.run(-10000000000l, 4000000000l));
    }

    /**
     * @title Divisor is negative
     */
    public void testN5() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(2000000000l, t.run(10000000000l, -4000000000l));
    }

    /**
     * @title Both Dividend and divisor are negative
     */
    public void testN6() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(-2000000000l, t.run(-10000000000l, -4000000000l));
    }

    /**
     * @title Arguments = Long.MIN_VALUE, -1l
     */
    public void testB1() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(0l, t.run(Long.MIN_VALUE, -1l));
    }

    /**
     * @title Arguments = Long.MIN_VALUE, 1l
     */
    public void testB2() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(0l, t.run(Long.MIN_VALUE, 1l));
    }

    /**
     * @title Arguments = Long.MAX_VALUE, 1l
     */
    public void testB3() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(0l, t.run(Long.MAX_VALUE, 1l));
    }

    /**
     * @title Arguments = Long.MIN_VALUE, Long.MAX_VALUE
     */
    public void testB4() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(-1l, t.run(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    /**
     * @title Arguments = 1l, Long.MAX_VALUE
     */
    public void testB5() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(1l, t.run(1l, Long.MAX_VALUE));
    }

    /**
     * @title Arguments = 1l, Long.MIN_VALUE
     */
    public void testB6() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        assertEquals(1l, t.run(1l, Long.MIN_VALUE));
    }

    /**
     * @title Divisor is 0
     */
    public void testE1() {
        T_rem_long_2addr_1 t = new T_rem_long_2addr_1();
        try {
            t.run(1234567890123l, 0l);
            fail("expected ArithmeticException");
        } catch (ArithmeticException ae) {
            // expected
        }
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title  (types of arguments - int, long).
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long, float
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference, long
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_6");
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
            Class.forName("dot.junit.opcodes.rem_long_2addr.d.T_rem_long_2addr_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
