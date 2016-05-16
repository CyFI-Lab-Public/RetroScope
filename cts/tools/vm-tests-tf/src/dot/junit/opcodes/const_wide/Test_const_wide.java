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

package dot.junit.opcodes.const_wide;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.const_wide.d.T_const_wide_1;
import dot.junit.opcodes.const_wide.d.T_const_wide_2;

public class Test_const_wide extends DxTestCase {
    /**
     * @title const-wide v1, 1.2345678901232324E51
     */
    public void testN1() {
        T_const_wide_1 t = new T_const_wide_1();
        double a = 1234567890123232323232232323232323232323232323456788d;
        double b = 1d;
        assertEquals(a + b, t.run(), 0d);
    }
    
    /**
     * @title const-wide v253, 20000000000
     */
    public void testN2() {
        T_const_wide_2 t = new T_const_wide_2();
         long a = 10000000000l;
         long b = 10000000000l;
        assertEquals(a + b, t.run());
    }

    /**
     * @constraint A24
     * @title  number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.const_wide.d.T_const_wide_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B11 
     * @title  When writing to a register that is one half of a register 
     * pair, but not touching the other half, the old register pair gets broken up, and the 
     * other register involved in it becomes undefined
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.const_wide.d.T_const_wide_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
