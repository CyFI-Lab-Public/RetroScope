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

package dot.junit.opcodes.neg_long;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.neg_long.d.T_neg_long_1;
import dot.junit.opcodes.neg_long.d.T_neg_long_2;
import dot.junit.opcodes.neg_long.d.T_neg_long_4;

public class Test_neg_long extends DxTestCase {
 
    /**
     * @title Argument = 123123123272432432l
     */
    public void testN1() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(-123123123272432432l, t.run(123123123272432432l));
    }

    /**
     * @title Argument = 1
     */
    public void testN2() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(-1l, t.run(1l));
    }

    /**
     * @title Argument = -1
     */
    public void testN3() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(1l, t.run(-1l));
    }

    /**
     * @title Check that -x == (~x + 1)
     */
    public void testN4() {
        T_neg_long_2 t = new T_neg_long_2();
        assertTrue(t.run(15l));
    }


    /**
     * @title Argument = 0
     */
    public void testB1() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(0, t.run(0));
    }

    /**
     * @title Argument = Long.MAX_VALUE
     */
    public void testB2() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(-9223372036854775807L, t.run(Long.MAX_VALUE));
    }

    /**
     * @title Argument = Long.MIN_VALUE
     */
    public void testB3() {
        T_neg_long_1 t = new T_neg_long_1();
        assertEquals(-9223372036854775808L, t.run(Long.MIN_VALUE));
    }

    /**
     * @constraint A24 
     * @title  number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.neg_long.d.T_neg_long_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * 
     * @constraint B1 
     * @title type of argument - int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.neg_long.d.T_neg_long_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title type of argument - float
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.neg_long.d.T_neg_long_6");
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
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.neg_long.d.T_neg_long_7");
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
            Class.forName("dot.junit.opcodes.neg_long.d.T_neg_long_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
