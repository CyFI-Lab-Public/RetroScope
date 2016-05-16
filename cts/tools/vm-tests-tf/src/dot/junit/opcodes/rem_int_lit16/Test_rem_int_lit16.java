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

package dot.junit.opcodes.rem_int_lit16;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_1;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_2;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_3;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_4;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_5;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_6;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_7;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_8;
import dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_9;

public class Test_rem_int_lit16 extends DxTestCase {

    /**
     * @title Arguments = 8, 4
     */
    public void testN1() {
        T_rem_int_lit16_1 t = new T_rem_int_lit16_1();
        assertEquals(0, t.run(8));
    }

    /**
     * @title Arguments = 10737, 4
     */
    public void testN2() {
        T_rem_int_lit16_1 t = new T_rem_int_lit16_1();
        assertEquals(1, t.run(10737));
    }

    /**
     * @title Dividend = 0
     */
    public void testN3() {
        T_rem_int_lit16_1 t = new T_rem_int_lit16_1();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Dividend is negative
     */
    public void testN4() {
        T_rem_int_lit16_1 t = new T_rem_int_lit16_1();
        assertEquals(-2, t.run(-10));
    }

    /**
     * @title Divisor is negative
     */
    public void testN5() {
        T_rem_int_lit16_2 t = new T_rem_int_lit16_2();
        assertEquals(1, t.run(22222));
    }

    /**
     * @title Both Dividend and divisor are negative
     */
    public void testN6() {
        T_rem_int_lit16_3 t = new T_rem_int_lit16_3();
        assertEquals(-2235, t.run(-23235));
    }

    /**
     * @title Arguments = Short.MIN_VALUE, -1
     */
    public void testB1() {
        T_rem_int_lit16_5 t = new T_rem_int_lit16_5();
        assertEquals(0, t.run(Short.MIN_VALUE));
    }

    /**
     * @title Arguments = Short.MIN_VALUE, 1
     */
    public void testB2() {
        T_rem_int_lit16_6 t = new T_rem_int_lit16_6();
        assertEquals(0, t.run(Short.MIN_VALUE));
    }

    /**
     * @title Arguments = Short.MAX_VALUE, 1
     */
    public void testB3() {
        T_rem_int_lit16_6 t = new T_rem_int_lit16_6();
        assertEquals(0, t.run(Short.MAX_VALUE));
    }

    /**
     * @title Arguments = Short.MIN_VALUE, 32767
     */
    public void testB4() {
        T_rem_int_lit16_7 t = new T_rem_int_lit16_7();
        assertEquals(-1, t.run(Short.MIN_VALUE));
    }

    /**
     * @title Arguments = 1, 32767
     */
    public void testB5() {
        T_rem_int_lit16_7 t = new T_rem_int_lit16_7();
        assertEquals(1, t.run(1));
    }

    /**
     * @title Arguments = 1, -32768
     */
    public void testB6() {
        T_rem_int_lit16_8 t = new T_rem_int_lit16_8();
        assertEquals(1, t.run(1));
    }

    /**
     * @title Divisor is 0
     */
    public void testE1() {
        T_rem_int_lit16_9 t = new T_rem_int_lit16_9();
        try {
            t.run(1);
            fail("expected ArithmeticException");
        } catch (ArithmeticException ae) {
            // expected
        }
    }

    /**
     * @constraint A23
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - int, double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_12");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_13");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - float, int. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.rem_int_lit16.d.T_rem_int_lit16_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
