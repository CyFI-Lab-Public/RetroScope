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

package dot.junit.opcodes.move_wide;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.move_wide.d.T_move_wide_1;
import dot.junit.opcodes.move_wide.d.T_move_wide_6;

public class Test_move_wide extends DxTestCase {
    /**
     * @title tests move-wide functionality
     */
    public void testN1() {
        assertTrue(T_move_wide_1.run());
    }

    
    /**
     * @title v0 -> v1
     */
    public void testN2() {
        assertEquals(T_move_wide_6.run(), 0);
    }

    /**
     * @constraint A24
     * @title number of registers - src is not valid
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.move_wide.d.T_move_wide_2");
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
            Class.forName("dot.junit.opcodes.move_wide.d.T_move_wide_3");
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
            Class.forName("dot.junit.opcodes.move_wide.d.T_move_wide_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title  src register contains 32-bit value
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.move_wide.d.T_move_wide_5");
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
            Class.forName("dot.junit.opcodes.move_wide.d.T_move_wide_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
