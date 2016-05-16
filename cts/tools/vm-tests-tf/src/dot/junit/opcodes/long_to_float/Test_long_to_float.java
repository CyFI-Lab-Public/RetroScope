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

package dot.junit.opcodes.long_to_float;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.long_to_float.d.T_long_to_float_1;
import dot.junit.opcodes.long_to_float.d.T_long_to_float_2;

public class Test_long_to_float extends DxTestCase {
    /**
     * @title Argument = 123456789012345
     */
    public void testN1() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(1.23456788E14f, t.run(123456789012345l), 0f);
    }

    /**
     * @title Argument = 1
     */
    public void testN2() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(1f, t.run(1l), 0f);
    }

    /**
     * @title Argument = -1
     */
    public void testN3() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(-1f, t.run(-1l), 0f);
    }

    /**
     * @title Argument = Long.MAX_VALUE
     */
    public void testB1() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(9.223372036854776E18, t.run(Long.MAX_VALUE), 0f);
    }

    /**
     * @title Argument = Long.MIN_VALUE
     */
    public void testB2() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(-9.223372036854776E18, t.run(Long.MIN_VALUE), 0f);
    }

    /**
     * @title Argument = 0
     */
    public void testB3() {
        T_long_to_float_1 t = new T_long_to_float_1();
        assertEquals(0f, t.run(0l), 0f);
    }




    /**
     * @constraint B1
     * @title Type of argument - double. The verifier checks that longs and
     * doubles are not used interchangeably.
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.long_to_float.d.T_long_to_float_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title type of argument - integer
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.long_to_float.d.T_long_to_float_3");
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
            Class.forName("dot.junit.opcodes.long_to_float.d.T_long_to_float_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.long_to_float.d.T_long_to_float_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
