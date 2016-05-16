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

package dot.junit.opcodes.mul_int_lit8;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_1;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_2;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_3;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_4;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_5;
import dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_6;

public class Test_mul_int_lit8 extends DxTestCase {
    
    /**
     * @title Arguments =  10, 55
     */
    public void testN1() {
        T_mul_int_lit8_1 t = new T_mul_int_lit8_1();
        assertEquals(550, t.run(55));
    }

    /**
     * @title Arguments = 10, -25 
     */
    public void testN2() {
        T_mul_int_lit8_1 t = new T_mul_int_lit8_1();
        assertEquals(-250, t.run(-25));
    }

    /**
     * @title Arguments = -15, -23
     */
    public void testN3() {
        T_mul_int_lit8_2 t = new T_mul_int_lit8_2();
        assertEquals(345, t.run(-23));
    }
    
    /**
     * @title Arguments = 0x7ffffffe, 10
     */
    public void testN4() {
        T_mul_int_lit8_1 t = new T_mul_int_lit8_1();
        assertEquals(-20, t.run(0x7ffffffe));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB1() {
        T_mul_int_lit8_4 t = new T_mul_int_lit8_4();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Arguments = 0, Byte.MAX_VALUE
     */
    public void testB2() {
        T_mul_int_lit8_4 t = new T_mul_int_lit8_4();
        assertEquals(0, t.run(Byte.MAX_VALUE));
    }

    /**
     * @title Arguments = 1, Byte.MAX_VALUE
     */
    public void testB3() {
        T_mul_int_lit8_5 t = new T_mul_int_lit8_5();
        assertEquals(Byte.MAX_VALUE, t.run(Byte.MAX_VALUE));
    }
    
    /**
     * @title Arguments = 1, Short.MIN_VALUE
     */
    public void testB4() {
        T_mul_int_lit8_5 t = new T_mul_int_lit8_5();
        assertEquals(Short.MIN_VALUE, t.run(Short.MIN_VALUE));
    }

    /**
     * @title Arguments = 127, Short.MIN_VALUE
     */
    public void testB5() {
        T_mul_int_lit8_6 t = new T_mul_int_lit8_6();
        assertEquals(-4161536, t.run(Short.MIN_VALUE));
    }

    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_7");
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
            Class.forName("dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_8");
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
            Class.forName("dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_9");
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
            Class.forName("dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_10");
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
            Class.forName("dot.junit.opcodes.mul_int_lit8.d.T_mul_int_lit8_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
