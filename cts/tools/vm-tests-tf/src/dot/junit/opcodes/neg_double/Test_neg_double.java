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

package dot.junit.opcodes.neg_double;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.neg_double.d.T_neg_double_1;
import dot.junit.opcodes.neg_double.d.T_neg_double_4;

public class Test_neg_double extends DxTestCase {
    
    /**
     * @title Argument = 1
     */
    public void testN1() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(-1d, t.run(1d));
    }

    /**
     * @title Argument = -1
     */
    public void testN2() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(1d, t.run(-1d));
    }

    /**
     * @title Argument = +0
     */
    public void testN3() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(-0d, t.run(+0d));
    }

    /**
     * @title Argument = -2.7
     */
    public void testN4() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(2.7d, t.run(-2.7d));
    }


    /**
     * @title Argument = Double.NaN
     */
    public void testB1() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(Double.NaN, t.run(Double.NaN));
    }

    /**
     * @title Argument = Double.NEGATIVE_INFINITY
     */
    public void testB2() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(Double.POSITIVE_INFINITY, t.run(Double.NEGATIVE_INFINITY));
    }

    /**
     * @title Argument = Double.POSITIVE_INFINITY
     */
    public void testB3() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(Double.NEGATIVE_INFINITY, t.run(Double.POSITIVE_INFINITY));
    }

    /**
     * @title Argument = Double.MAX_VALUE
     */
    public void testB4() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(-1.7976931348623157E308d, t.run(Double.MAX_VALUE));
    }

    /**
     * @title Argument = Double.MIN_VALUE
     */
    public void testB5() {
        T_neg_double_1 t = new T_neg_double_1();
        assertEquals(-4.9E-324d, t.run(Double.MIN_VALUE));
    }

    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.neg_double.d.T_neg_double_2");
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
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.neg_double.d.T_neg_double_3");
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
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.neg_double.d.T_neg_double_5");
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
            Class.forName("dot.junit.opcodes.neg_double.d.T_neg_double_6");
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
            Class.forName("dot.junit.opcodes.neg_double.d.T_neg_double_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
