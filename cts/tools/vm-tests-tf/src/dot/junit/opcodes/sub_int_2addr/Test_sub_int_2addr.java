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

package dot.junit.opcodes.sub_int_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_1;
import dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_5;

public class Test_sub_int_2addr extends DxTestCase {
    /**
     * @title Arguments = 8, 4
     */
    public void testN1() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(4, t.run(8, 4));
    }

    /**
     * @title Arguments = 0, 255
     */
    public void testN2() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(-255, t.run(0, 255));
    }

    /**
     * @title Arguments = 0, -65536
     */
    public void testN3() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(65536, t.run(0, -65536));
    }

    /**
     * @title Arguments = 0, -2147483647
     */
    public void testN4() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(Integer.MAX_VALUE, t.run(0, -2147483647));
    }

    /**
     * @title Arguments = 0, Integer.MAX_VALUE
     */
    public void testB1() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(-2147483647, t.run(0, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MAX_VALUE
     */
    public void testB2() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(0, t.run(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, -1
     */
    public void testB3() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MAX_VALUE, -1));
    }

    /** 
     * @title Arguments = Integer.MIN_VALUE, 1
     */
    public void testB4() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(Integer.MAX_VALUE, t.run(Integer.MIN_VALUE, 1));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB5() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(0, t.run(0, 0));
    }

    /**
     * @title Arguments = 0, -Integer.MIN_VALUE
     */
    public void testB6() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(-2147483648, t.run(0, -Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, 1
     */
    public void testB7() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(2147483646, t.run(Integer.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = 1, Integer.MIN_VALUE
     */
    public void testB8() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(-2147483647, t.run(1, Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MIN_VALUE
     */
    public void testB9() {
        T_sub_int_2addr_1 t = new T_sub_int_2addr_1();
        assertEquals(-1, t.run(Integer.MAX_VALUE, Integer.MIN_VALUE));
    }

    

    /**
     * @constraint B1
     * @title Types of arguments - int, float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_5");
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
            Class.forName("dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_2");
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
            Class.forName("dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_3");
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
            Class.forName("dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.sub_int_2addr.d.T_sub_int_2addr_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
