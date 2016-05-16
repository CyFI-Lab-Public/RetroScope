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

package dot.junit.opcodes.move_wide_from16;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_1;
import dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_2;

public class Test_move_wide_from16 extends DxTestCase {
    /**
     * @title v4001 -> v255 -> v1
     */
    public void testN1() {
        assertTrue(T_move_wide_from16_1.run());
    }
    
    /**
     * @title v100 -> 101
     */
    public void testN2() {
        assertEquals(T_move_wide_from16_2.run(), 0);
    }
    
    /**
     * @constraint A24 
     * @title number of registers - src is not valid
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A24 
     * @title number of registers - dst is not valid
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title src register contains reference
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title src register contains 32-bit value
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title src register is a part of reg pair
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B18 
     * @title When writing to a register that is one half of a 
     * register pair, but not touching the other half, the old register pair gets broken 
     * up, and the other register involved in it becomes undefined.
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.move_wide_from16.d.T_move_wide_from16_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
