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

package dot.junit.opcodes.neg_float;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.neg_float.d.T_neg_float_1;
import dot.junit.opcodes.neg_float.d.T_neg_float_6;

public class Test_neg_float extends DxTestCase {

    /**
     * @title Argument = 1
     */
    public void testN1() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(-1f, t.run(1f));
    }

    /**
     * @title Argument = -1
     */
    public void testN2() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(1f, t.run(-1f));
    }

    /**
     * @title Argument = +0
     */
    public void testN3() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(-0f, t.run(+0f));
    }

    /**
     * @title Argument = -2.7
     */
    public void testN4() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(2.7f, t.run(-2.7f));
    }

    /**
     * @title Argument = Float.NaN
     */
    public void testB1() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(Float.NaN, t.run(Float.NaN));
    }

    /**
     * @title Argument = Float.NEGATIVE_INFINITY
     */
    public void testB2() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(Float.POSITIVE_INFINITY, t.run(Float.NEGATIVE_INFINITY));
    }

    /**
     * @title Argument = Float.POSITIVE_INFINITY
     */
    public void testB3() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(Float.NEGATIVE_INFINITY, t.run(Float.POSITIVE_INFINITY));
    }

    /**
     * @title Argument = Float.MAX_VALUE
     */
    public void testB4() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(-3.4028235E38f, t.run(Float.MAX_VALUE));
    }

    /**
     * @title Argument = Float.MIN
     */
    public void testB5() {
        T_neg_float_1 t = new T_neg_float_1();
        assertEquals(-1.4E-45f, t.run(Float.MIN_VALUE));
    }

    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.neg_float.d.T_neg_float_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }



    /**
     * 
     * @constraint B1 
     * @title type of argument - double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.neg_float.d.T_neg_float_3");
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
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.neg_float.d.T_neg_float_4");
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
            Class.forName("dot.junit.opcodes.neg_float.d.T_neg_float_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - float, int. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.neg_float.d.T_neg_float_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
