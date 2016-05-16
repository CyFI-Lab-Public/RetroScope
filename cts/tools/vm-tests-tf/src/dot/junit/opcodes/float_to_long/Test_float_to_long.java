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

package dot.junit.opcodes.float_to_long;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.float_to_long.d.T_float_to_long_1;
import dot.junit.opcodes.float_to_long.d.T_float_to_long_7;

public class Test_float_to_long extends DxTestCase {
    /**
     * @title Argument = 2.999999f
     */
    public void testN1() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(2l, t.run(2.999999f));
    }

    /**
     * @title Argument = 1
     */
    public void testN2() {
         T_float_to_long_1 t = new T_float_to_long_1();
         assertEquals(1l, t.run(1));
    }
    
    /**
     * @title Argument = -1
     */
    public void testN3() {
         T_float_to_long_1 t = new T_float_to_long_1();
         assertEquals(-1l, t.run(-1));
    }

    /**
     * @title Argument = Float.MAX_VALUE
     */
    public void testB1() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(Long.MAX_VALUE, t.run(Float.MAX_VALUE));
    }

    /**
     * @title Argument = Float.MIN_VALUE
     */
    public void testB2() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(0, t.run(Float.MIN_VALUE));
    }
    
    /**
     * @title Argument = 0
     */
    public void testB3() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(0l, t.run(0));
    }
    
    /**
     * @title Argument = NaN
     */
    public void testB4() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(0l, t.run(Float.NaN));
    }
    
    /**
     * @title Argument = POSITIVE_INFINITY
     */
    public void testB5() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(Long.MAX_VALUE, t.run(Float.POSITIVE_INFINITY));
    }
    
    /**
     * @title Argument = NEGATIVE_INFINITY
     */
    public void testB6() {
        T_float_to_long_1 t = new T_float_to_long_1();
        assertEquals(Long.MIN_VALUE, t.run(Float.NEGATIVE_INFINITY));
    }

    /**
     * @constraint B1 
     * @title number of arguments
     */
    public void testVFE1() {
        try
        {
            Class.forName("dxc.junit.opcodes.float_to_long.jm.T_float_to_long_2");
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
        try
        {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_2");
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
        try
        {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE4() {
        try
        {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title type of argument - reference
     */
    public void testVFE5() {
        try
        {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A23 
     * @title  number of registers
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_6");
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
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.float_to_long.d.T_float_to_long_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
