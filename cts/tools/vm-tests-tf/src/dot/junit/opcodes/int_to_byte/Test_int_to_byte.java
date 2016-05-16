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

package dot.junit.opcodes.int_to_byte;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.int_to_byte.d.T_int_to_byte_1;
import dot.junit.opcodes.int_to_byte.d.T_int_to_byte_5;


public class Test_int_to_byte extends DxTestCase {
    /**
     * @title Argument = 1
     */
    public void testN1() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(1, t.run(1));
    }

    /**
     * @title Argument = -1
     */
    public void testN2() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-1, t.run(-1));
    }

    /**
     * @title Argument = 16
     */
    public void testN3() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(16, t.run(16));
    }

    /**
     * @title Argument = -32
     */
    public void testN4() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-32, t.run(-32));
    }

    /**
     * @title Argument = 134
     */
    public void testN5() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-122, t.run(134));
    }

    
    /**
     * @title Argument = -134
     */
    public void testN6() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(122, t.run(-134));
    }

    /**
     * @title Argument = 127
     */
    public void testB1() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(127, t.run(127));
    }

    /**
     * @title Argument = 128
     */
    public void testB2() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-128, t.run(128));
    }

    /**
     * @title Argument = 0
     */
    public void testB3() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Argument = -128
     */
    public void testB4() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-128, t.run(-128));
    }

    /**
     * @title Argument = -129
     */
    public void testB5() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(127, t.run(-129));
    }

    /**
     * @title Argument = Integer.MAX_VALUE
     */
    public void testB6() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(-1, t.run(Integer.MAX_VALUE));
    }

    /**
     * @title Argument = Integer.MIN_VALUE
     */
    public void testB7() {
        T_int_to_byte_1 t = new T_int_to_byte_1();
        assertEquals(0, t.run(Integer.MIN_VALUE));
    }


    /**
     * @constraint B1 
     * @title type of argument - double
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.int_to_byte.d.T_int_to_byte_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title type of argument - long
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.int_to_byte.d.T_int_to_byte_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title type of argument - reference
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.int_to_byte.d.T_int_to_byte_4");
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
            Class.forName("dot.junit.opcodes.int_to_byte.d.T_int_to_byte_6");
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
            Class.forName("dot.junit.opcodes.int_to_byte.d.T_int_to_byte_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
