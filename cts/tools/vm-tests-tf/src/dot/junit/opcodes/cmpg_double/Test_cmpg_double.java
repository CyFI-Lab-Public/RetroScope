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

package dot.junit.opcodes.cmpg_double;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.cmpg_double.d.T_cmpg_double_1;
import dot.junit.opcodes.cmpg_double.d.T_cmpg_double_3;

public class Test_cmpg_double extends DxTestCase {
    /**
     * @title Arguments = 3.14d, 2.7d
     */
    public void testN1() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(1, t.run(3.14d, 2.7d));
    }

    /**
     * @title Arguments = -3.14d, 2.7d
     */
    public void testN2() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(-1, t.run(-3.14d, 2.7d));
    }

    /**
     * @title Arguments = 3.14, 3.14
     */
    public void testN3() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(0, t.run(3.14d, 3.14d));
    }

    /**
     * @title Arguments = Double.NaN, Double.MAX_VALUE
     */
    public void testB1() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(1, t.run(Double.NaN, Double.MAX_VALUE));
    }

    /**
     * @title Arguments = +0, -0
     */
    public void testB2() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(0, t.run(+0f, -0f));
    }

    /**
     * @title Arguments = Double.NEGATIVE_INFINITY, Double.MIN_VALUE
     */
    public void testB3() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(-1, t.run(Double.NEGATIVE_INFINITY, Double.MIN_VALUE));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY, Double.MAX_VALUE
     */
    public void testB4() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(1, t.run(Double.POSITIVE_INFINITY, Double.MAX_VALUE));
    }

    /**
     * @title Arguments = Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
     */
    public void testB5() {
        T_cmpg_double_1 t = new T_cmpg_double_1();
        assertEquals(1, t.run(Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY));
    }

    

    /**
     * @constraint B1 
     * @title  types of arguments - double, float
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.cmpg_double.d.T_cmpg_double_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A24 
     * @title  number of registers
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.cmpg_double.d.T_cmpg_double_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title  types of arguments - double, reference
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.cmpg_double.d.T_cmpg_double_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title  types of arguments - int, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.cmpg_double.d.T_cmpg_double_6");
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
            Class.forName("dot.junit.opcodes.cmpg_double.d.T_cmpg_double_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
