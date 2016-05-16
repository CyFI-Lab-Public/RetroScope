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

package dot.junit.opcodes.mul_int_lit16;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_1;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_2;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_3;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_4;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_5;
import dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_6;

public class Test_mul_int_lit16 extends DxTestCase {

    /**
     * @title Arguments = 205, 130
     */
    public void testN1() {
        T_mul_int_lit16_1 t = new T_mul_int_lit16_1();
        assertEquals(26650, t.run(205));
    }

    /**
     * @title Arguments = -180, 130
     */
    public void testN2() {
        T_mul_int_lit16_1 t = new T_mul_int_lit16_1();
        assertEquals(-23400, t.run(-180));
    }

    /**
     * @title Arguments = 0xfa, 130
     */
    public void testN3() {
        T_mul_int_lit16_1 t = new T_mul_int_lit16_1();
        assertEquals(0x7ef4, t.run(0xfa));
    }

    /**
     * @title Arguments = -101, -321
     */
    public void testN4() {
        T_mul_int_lit16_2 t = new T_mul_int_lit16_2();
        assertEquals(32421, t.run(-101));
    }

    /**
     * @title Arguments = 0, 0 
     */
    public void testB1() {
        T_mul_int_lit16_4 t = new T_mul_int_lit16_4();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Arguments = 0, Short.MAX_VALUE
     */
    public void testB2() {
        T_mul_int_lit16_4 t = new T_mul_int_lit16_4();
        assertEquals(0, t.run(Short.MAX_VALUE));
    }

    /**
     * @title Arguments = 1, Short.MAX_VALUE
     */
    public void testB3() {
        T_mul_int_lit16_5 t = new T_mul_int_lit16_5();
        assertEquals(Short.MAX_VALUE, t.run(Short.MAX_VALUE));
    }
    
    /**
     * @title Arguments = 1, Short.MIN_VALUE
     */
    public void testB4() {
        T_mul_int_lit16_5 t = new T_mul_int_lit16_5();
        assertEquals(Short.MIN_VALUE, t.run(Short.MIN_VALUE));
    }

    /**
     * @title Arguments = 32767, Short.MIN_VALUE
     */
    public void testB5() {
        T_mul_int_lit16_6 t = new T_mul_int_lit16_6();
        assertEquals(-1073709056, t.run(Short.MIN_VALUE));
    }

    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - int * double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long * int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference * int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_10");
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
            Class.forName("dot.junit.opcodes.mul_int_lit16.d.T_mul_int_lit16_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
