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

package dot.junit.opcodes.ushr_int_lit8;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_1;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_13;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_2;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_3;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_4;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_5;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_6;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_7;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_8;
import dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_9;


public class Test_ushr_int_lit8 extends DxTestCase {
    /**
     * @title 15 >> 1
     */
    public void testN1() {
        T_ushr_int_lit8_1 t = new T_ushr_int_lit8_1();
        assertEquals(7, t.run());
    }

    /**
     * @title 33 >> 2
     */
    public void testN2() {
        T_ushr_int_lit8_2 t = new T_ushr_int_lit8_2();
        assertEquals(8, t.run());
    }

    /**
     * @title -15 >> 1
     */
    public void testN3() {
        T_ushr_int_lit8_3 t = new T_ushr_int_lit8_3();
        assertEquals(0x7FFFFFF8, t.run());
    }

    /**
     * @title Arguments = 1 & -1
     */
    public void testN4() {
        T_ushr_int_lit8_4 t = new T_ushr_int_lit8_4();
        assertEquals(0, t.run());
    }

    /**
     * @title Verify that shift distance is actually in range 0 to 32.
     */
    public void testN5() {
        T_ushr_int_lit8_5 t = new T_ushr_int_lit8_5();
        assertEquals(16, t.run());
    }


    /**
     * @title Arguments = 0 & -1
     */
    public void testB1() {
        T_ushr_int_lit8_6 t = new T_ushr_int_lit8_6();
        assertEquals(0, t.run());
    }

    /**
     * @title Arguments = Integer.MAX_VALUE & 1
     */
    public void testB2() {
        T_ushr_int_lit8_7 t = new T_ushr_int_lit8_7();
        assertEquals(0x3FFFFFFF, t.run());
    }

    /**
     * @title Arguments = Integer.MIN_VALUE & 1
     */
    public void testB3() {
        T_ushr_int_lit8_8 t = new T_ushr_int_lit8_8();
        assertEquals(0x40000000, t.run());
    }

    /**
     * @title Arguments = 1 & 0
     */
    public void testB4() {
        T_ushr_int_lit8_9 t = new T_ushr_int_lit8_9();
        assertEquals(1, t.run());
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - double, int
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_10");
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
            Class.forName("dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_11");
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
            Class.forName("dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_12");
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
            Class.forName("dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_14");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Type of argument - float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.ushr_int_lit8.d.T_ushr_int_lit8_13");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
