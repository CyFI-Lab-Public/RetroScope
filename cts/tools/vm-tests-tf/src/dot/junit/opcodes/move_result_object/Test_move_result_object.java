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

package dot.junit.opcodes.move_result_object;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.move_result_object.d.T_move_result_object_1;
import dot.junit.opcodes.move_result_object.d.T_move_result_object_8;

public class Test_move_result_object extends DxTestCase {
    /**
     * @title tests move-result-object functionality
     */
    public void testN1() {
        T_move_result_object_1 t = new T_move_result_object_1();
        assertTrue(t.run());
    }

    /**
     * @title filled-new-array result
     */
    public void testN2() {
        T_move_result_object_8 t = new T_move_result_object_8();
        int[] arr = t.run();
        if(arr.length != 2 || arr[0] != 1 || arr[1] != 2)
            fail("wrong array size or content");
    }

    /**
     * @constraint A23 
     * @title number of registers - dest is not valid
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * @constraint B1 
     * @title integer
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title wide
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_4");
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
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B19 
     * @title  move-result-object instruction must be immediately preceded 
     * (in the insns array) by an <invoke-kind> instruction
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B20
     * @title move-result-object instruction must be immediately preceded 
     * (in actual control flow) by an <invoke-kind> instruction
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.move_result_object.d.T_move_result_object_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
