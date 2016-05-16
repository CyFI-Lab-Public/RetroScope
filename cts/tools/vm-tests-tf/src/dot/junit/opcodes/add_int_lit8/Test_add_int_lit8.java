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

package dot.junit.opcodes.add_int_lit8;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_16;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_1;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_10;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_11;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_12;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_2;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_3;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_4;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_5;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_6;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_7;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_8;
import dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_9;

public class Test_add_int_lit8 extends DxTestCase {
    /**
     * @title Arguments = 8 + 4
     */
    public void testN1() {
        T_add_int_lit8_1 t = new T_add_int_lit8_1();
        assertEquals(12, t.run());
    }

    /**
     * @title Arguments = Byte.MIN_VALUE + Byte.MAX_VALUE
     */
    public void testN2() {
        T_add_int_lit8_2 t = new T_add_int_lit8_2();
        assertEquals(-1, t.run());
    }

    /**
     * @title Arguments = 0 + (-128)
     */
    public void testN3() {
        T_add_int_lit8_3 t = new T_add_int_lit8_3();
        assertEquals(-128, t.run());
    }

    /**
     * @title Arguments = (-2147483647) + 0
     */
    public void testN4() {
        T_add_int_lit8_4 t = new T_add_int_lit8_4();
        assertEquals(-2147483647, t.run());
    }

    /**
     * @title Arguments = 0x7ffffffe + 2
     */
    public void testN5() {
        T_add_int_lit8_5 t = new T_add_int_lit8_5();
        assertEquals(-2147483648, t.run());
    }

    /**
     * @title Arguments = -1 + 1
     */
    public void testN6() {
        T_add_int_lit8_6 t = new T_add_int_lit8_6();
        assertEquals(0, t.run());
    }

    /**
     * @title Arguments = 0 + Byte.MAX_VALUE
     */
    public void testB1() {
        T_add_int_lit8_7 t = new T_add_int_lit8_7();
        assertEquals(Byte.MAX_VALUE, t.run());
    }

    /**
     * @title Arguments = Integer.MAX_VALUE + Byte.MAX_VALUE
     */
    public void testB2() {
        T_add_int_lit8_8 t = new T_add_int_lit8_8();
        assertEquals(-2147483522, t.run());
    }

    /**
     * @title Arguments = Integer.MAX_VALUE + 1
     */
    public void testB3() {
        T_add_int_lit8_9 t = new T_add_int_lit8_9();
        assertEquals(Integer.MIN_VALUE, t.run());
    }

    /**
     * @title Arguments = Integer.MIN_VALUE + 1
     */
    public void testB4() {
        T_add_int_lit8_10 t = new T_add_int_lit8_10();
        assertEquals(-2147483647, t.run());
    }

    /**
     * @title Arguments = 0 + 0
     */
    public void testB5() {
        T_add_int_lit8_11 t = new T_add_int_lit8_11();
        assertEquals(0, t.run());
    }

    /**
     * @title Arguments = Short.MIN_VALUE + Byte.MIN_VALUE
     */
    public void testB6() {
        T_add_int_lit8_12 t = new T_add_int_lit8_12();
        assertEquals(-32896, t.run());
    }

    
    

    /**
     * @constraint B1 
     * @title types of arguments - int, double
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_13");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long, int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_14");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_15");
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
            Class.forName("dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_17");
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
            Class.forName("dot.junit.opcodes.add_int_lit8.d.T_add_int_lit8_16");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
