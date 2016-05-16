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

package dot.junit.opcodes.float_to_double;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.float_to_double.d.T_float_to_double_1;
import dot.junit.opcodes.float_to_double.d.T_float_to_double_7;

public class Test_float_to_double extends DxTestCase {
     /**
     * @title Argument = 0.5
     */
    public void testN1() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertEquals(0.5d, t.run(0.5f), 0d);
    }

    /**
     * @title Argument = 1
     */
    public void testN2() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertEquals(1d, t.run(1), 0d);
    }

    /**
     * @title Argument = -1
     */
    public void testN3() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertEquals(-1d, t.run(-1), 0d);
    }

    /**
     * @title Argument = Float.MAX_VALUE
     */
    public void testB1() {
        T_float_to_double_1 t = new T_float_to_double_1();
        double r = 0x1.fffffeP+127d;
        assertEquals(r, t.run(Float.MAX_VALUE), 0d);
    }

    /**
     * @title Argument = Float.MIN_VALUE
     */
    public void testB2() {
        T_float_to_double_1 t = new T_float_to_double_1();
        double r = 0x0.000002P-126d;
        assertEquals(r, t.run(Float.MIN_VALUE), 0d);
    }

    /**
     * @title Argument = -0
     */
    public void testB3() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertEquals(-0d, t.run(-0), 0d);
    }

    /**
     * @title Argument = NaN
     */
    public void testB4() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertTrue(Double.isNaN(t.run(Float.NaN)));
    }

    /**
     * @title Argument = POSITIVE_INFINITY
     */
    public void testB5() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertTrue(Double.isInfinite(t.run(Float.POSITIVE_INFINITY)));
    }

    /**
     * @title Argument = NEGATIVE_INFINITY
     */
    public void testB6() {
        T_float_to_double_1 t = new T_float_to_double_1();
        assertTrue(Double.isInfinite(t.run(Float.NEGATIVE_INFINITY)));
    }



    /**
     * @constraint B1 
     * @title type of argument - double
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_2");
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
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title type of argument - reference
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_5");
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
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Type of argument - int. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.float_to_double.d.T_float_to_double_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
