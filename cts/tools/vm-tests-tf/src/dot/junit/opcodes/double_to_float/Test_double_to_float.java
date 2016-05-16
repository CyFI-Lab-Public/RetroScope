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

package dot.junit.opcodes.double_to_float;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.double_to_float.d.T_double_to_float_1;
import dot.junit.opcodes.double_to_float.d.T_double_to_float_3;

public class Test_double_to_float extends DxTestCase {
     /**
     * @title Argument = 2.71
     */
    public void testN1() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(2.71f, t.run(2.71d));
    }

    /**
     * @title Argument = 1
     */
    public void testN2() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(1f, t.run(1d));
    }

    /**
     * @title Argument = -1
     */
    public void testN3() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(-1f, t.run(-1d));
    }

    /**
     * @title Argument = Double.MAX_VALUE
     */
    public void testB1() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(Float.POSITIVE_INFINITY, t.run(Double.MAX_VALUE));
    }

    /**
     * @title Argument = Double.MIN_VALUE
     */
    public void testB2() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(0f, t.run(Double.MIN_VALUE));
    }

    /**
     * @title Argument = -0
     */
    public void testB3() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(-0f, t.run(-0d));
    }

    /**
     * @title Argument = NaN
     */
    public void testB4() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertTrue(Float.isNaN(t.run(Double.NaN)));
    }

    /**
     * @title Argument = POSITIVE_INFINITY
     */
    public void testB5() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertTrue(Float.isInfinite(t.run(Double.POSITIVE_INFINITY)));
    }

    /**
     * @title Argument = NEGATIVE_INFINITY
     */
    public void testB6() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertTrue(Float.isInfinite(t.run(Double.NEGATIVE_INFINITY)));
    }


    /**
     * @title Argument = -Double.MIN_VALUE
     */
    public void testB7() {
        T_double_to_float_1 t = new T_double_to_float_1();
        assertEquals(-0f, t.run(-4.9E-324d));
    }


    /**
     * @constraint B1
     * @title Type of argument - long. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.double_to_float.d.T_double_to_float_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  type of argument - float
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.double_to_float.d.T_double_to_float_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint A24 
     * @title  number of registers
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.double_to_float.d.T_double_to_float_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title  type of argument - reference
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.double_to_float.d.T_double_to_float_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title  type of argument - int
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.double_to_float.d.T_double_to_float_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
