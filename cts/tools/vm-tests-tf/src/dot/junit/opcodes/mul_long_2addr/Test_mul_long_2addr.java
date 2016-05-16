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

package dot.junit.opcodes.mul_long_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_1;
import dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_3;

public class Test_mul_long_2addr extends DxTestCase {

    /**
     * @title Arguments = 222000000000l, 5000000000l
     */
    public void testN1() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(3195355577426903040l, t.run(222000000000l, 5000000000l));
    }

    /**
     * @title Arguments = -123456789l, 123456789l
     */
    public void testN2() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(-15241578750190521l, t.run(-123456789l, 123456789l));
    }

    /**
     * @title Arguments = -123456789l, -123456789l
     */
    public void testN3() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(15241578750190521l, t.run(-123456789l, -123456789l));
    }

    /**
     * @title Arguments = 0, Long.MAX_VALUE
     */
    public void testB1() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(0, t.run(0, Long.MAX_VALUE));
    }

    /**
     * @title Arguments = Long.MAX_VALUE, 1
     */
    public void testB2() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(9223372036854775807L, t.run(Long.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = Long.MIN_VALUE, 1
     */
    public void testB3() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(-9223372036854775808L, t.run(Long.MIN_VALUE, 1));
    }

    /**
     * @title Arguments = Long.MAX_VALUE, Long.MIN_VALUE
     */
    public void testB4() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(-9223372036854775808L, t.run(Long.MAX_VALUE,
                Long.MIN_VALUE));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB5() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(0, t.run(0, 0));
    }

    /**
     * @title Arguments = Long.MAX_VALUE, -1
     */
    public void testB6() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(-9223372036854775807L, t.run(Long.MAX_VALUE, -1));
    }

    /**
     * @title Arguments = Long.MIN_VALUE, -1
     */
    public void testB7() {
        T_mul_long_2addr_1 t = new T_mul_long_2addr_1();
        assertEquals(-9223372036854775808L, t.run(Long.MIN_VALUE, -1));
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - long * int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - float * long
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference * long
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_6");
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
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.mul_long_2addr.d.T_mul_long_2addr_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
