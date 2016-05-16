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

package dot.junit.opcodes.const_wide_16;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.const_wide_16.d.T_const_wide_16_1;

public class Test_const_wide_16 extends DxTestCase {

    /**
     * @title const-wide/16 v253, 20000
     */
    public void testN1() {
        T_const_wide_16_1 t = new T_const_wide_16_1();
         long a = 10000l;
         long b = 10000l;
        assertEquals(a + b, t.run());
    }

    /**
     * @constraint A24
     * @title  number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.const_wide_16.d.T_const_wide_16_3");
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
            Class.forName("dot.junit.opcodes.const_wide_16.d.T_const_wide_16_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
