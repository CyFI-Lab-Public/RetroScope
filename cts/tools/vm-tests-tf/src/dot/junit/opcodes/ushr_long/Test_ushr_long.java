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

package dot.junit.opcodes.ushr_long;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.ushr_long.d.T_ushr_long_1;
import dot.junit.opcodes.ushr_long.d.T_ushr_long_2;

public class Test_ushr_long extends DxTestCase {
    /**
     * @title Arguments = 40000000000l, 3
     */
    public void testN1() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(5000000000l, t.run(40000000000l, 3));
    }

    /**
     * @title Arguments = 40000000000l, 1
     */
    public void testN2() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(20000000000l, t.run(40000000000l, 1));
    }

    /**
     * @title Arguments = -123456789l, 1
     */
    public void testN3() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0x7FFFFFFFFC521975l, t.run(-123456789l, 1));
    }

    /**
     * @title Arguments = 1 & -1
     */
    public void testN4() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0l, t.run(1l, -1));
    }

    /**
     * @title Arguments = 123456789l, 64
     */
    public void testN5() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(123456789l, t.run(123456789l, 64));
    }
    
    /**
     * @title Arguments = 123456789l, 63
     */
    public void testN6() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0l, t.run(123456789l, 63));
    }

    /**
     * @title Arguments = 0 & -1
     */
    public void testB1() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0l, t.run(0l, -1));
    }

    /**
     * @title Arguments = Long.MAX_VALUE & 1
     */
    public void testB2() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0x3FFFFFFFFFFFFFFFl, t.run(Long.MAX_VALUE, 1));
    }

    /**
     * @title Arguments = Long.MIN_VALUE & 1
     */
    public void testB3() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(0x4000000000000000l, t.run(Long.MIN_VALUE, 1));
    }

    /**
     * @title Arguments = 1 & 0
     */
    public void testB4() {
        T_ushr_long_1 t = new T_ushr_long_1();
        assertEquals(1l, t.run(1l, 0));
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    

    /**
     * @constraint B1 
     * @title types of arguments - long, double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - int, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - float, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long, reference
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - double, int. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.ushr_long.d.T_ushr_long_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
