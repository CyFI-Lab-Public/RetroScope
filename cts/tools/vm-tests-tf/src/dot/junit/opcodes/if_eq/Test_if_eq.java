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

package dot.junit.opcodes.if_eq;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.if_eq.d.T_if_eq_1;
import dot.junit.opcodes.if_eq.d.T_if_eq_2;
import dot.junit.opcodes.if_eq.d.T_if_eq_4;

public class Test_if_eq extends DxTestCase {
    
    /**
     * @title Arguments = 5, 6
     */
    public void testN1() {
        T_if_eq_1 t = new T_if_eq_1();
        /*
         * Compare with 1234 to check that in case of failed comparison
         * execution proceeds at the address following if_eq instruction
         */
        assertEquals(1234, t.run(5, 6));
    }

    /**
     * @title Arguments = 0x0f0e0d0c, 0x0f0e0d0c
     */
    public void testN2() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1, t.run(0x0f0e0d0c, 0x0f0e0d0c));
    }

    /**
     * @title Arguments = 5, -5
     */
    public void testN3() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1234, t.run(5, -5));
    }

    /**
     * @title Arguments = 0x01001234, 0x1234
     */
    public void testN4() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1234, t.run(0x01001234, 0x1234));
    }
    
    /**
     * @title compare references
     */
    public void testN5() {
        T_if_eq_2 t = new T_if_eq_2();
        String a = "a";
        String b = "b";
        assertEquals(1234, t.run(a, b));
    }

    /**
     * @title compare references
     */
    public void testN6() {
        T_if_eq_2 t = new T_if_eq_2();
        String a = "a";
        assertEquals(1, t.run(a, a));
    }

    /**
     * @title Arguments = Integer.MAX_VALUE, Integer.MAX_VALUE
     */
    public void testB1() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1, t.run(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    /**
     * @title Arguments = Integer.MIN_VALUE, Integer.MIN_VALUE
     */
    public void testB2() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1, t.run(Integer.MIN_VALUE, Integer.MIN_VALUE));
    }

    /**
     * @title Arguments = 0, 1234567
     */
    public void testB3() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1234, t.run(0, 1234567));
    }

    /**
     * @title Arguments = 0, 0
     */
    public void testB4() {
        T_if_eq_1 t = new T_if_eq_1();
        assertEquals(1, t.run(0, 0));
    }
    
    /**
     * @title Compare reference with null
     */
    public void testB5() {
        T_if_eq_2 t = new T_if_eq_2();
        String a = "a";
        assertEquals(1234, t.run(null, a));
    }
    
    /**
     * @constraint A23 
     * @title  number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * @constraint B1 
     * @title  types of arguments - int, double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - long, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title  types of arguments - int, reference
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A6 
     * @title  branch target shall be inside the method
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A6 
     * @title  branch target shall not be "inside" instruction
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a 
     * @title  zero offset
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_12");
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
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.if_eq.d.T_if_eq_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
