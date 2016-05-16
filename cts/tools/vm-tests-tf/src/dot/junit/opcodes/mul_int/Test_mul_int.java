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

package dot.junit.opcodes.mul_int;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.mul_int.d.T_mul_int_1;
import dot.junit.opcodes.mul_int.d.T_mul_int_6;

public class Test_mul_int extends DxTestCase {

    /**
     * @title Arguments = 8, 4
     */
    public void testN1() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(32, t.run(8, 4));
    }

    /**
     * @title Arguments = -2, 255
     */
    public void testN2() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(-510, t.run(-2, 255));
    }

    /**
     * @title Arguments = 0x7ffffffe, 2
     */
    public void testN3() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(-4, t.run(0x7ffffffe, 2));
    }

    /**
     * @title Arguments = 4, 0x80000001
     */
    public void testN4() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(4, t.run(4, 0x80000001));
    }

    /**
     * @title Arguments = 0, Integer.MAX_VALUE
     */
    public void testB1() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(0, t.run(0, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, 1
     */
    public void testB2() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(Integer.MAX_VALUE, t.run(Integer.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, 1
     */
    public void testB3() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MIN_VALUE, 1));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MIN_VALUE
     */
    public void testB4() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MAX_VALUE,
                Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB5() {
        T_mul_int_1 t = new T_mul_int_1();
        assertEquals(0, t.run(0, 0));
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.mul_int.d.T_mul_int_2");
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
            Class.forName("dot.junit.opcodes.mul_int.d.T_mul_int_3");
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
            Class.forName("dot.junit.opcodes.mul_int.d.T_mul_int_4");
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
            Class.forName("dot.junit.opcodes.mul_int.d.T_mul_int_5");
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
            Class.forName("dot.junit.opcodes.mul_int.d.T_mul_int_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
