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

package dot.junit.opcodes.filled_new_array;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.filled_new_array.d.T_filled_new_array_1;
import dot.junit.opcodes.filled_new_array.d.T_filled_new_array_10;
import dot.junit.opcodes.filled_new_array.d.T_filled_new_array_11;
import dot.junit.opcodes.filled_new_array.d.T_filled_new_array_2;

public class Test_filled_new_array extends DxTestCase {
    /**
     * @title array of ints
     */
    public void testN1() {
        T_filled_new_array_1 t = new T_filled_new_array_1();
        int[] arr = t.run(1, 2, 3, 4, 5);
        assertNotNull(arr);
        assertEquals(5, arr.length);
        for(int i = 0; i < 5; i++)
            assertEquals(i + 1, arr[i]);
     }

    /**
     * @title array of objects
     */
    public void testN2() {
        T_filled_new_array_2 t = new T_filled_new_array_2();
        String s = "android";
        Object[] arr = t.run(t, s);
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertEquals(t, arr[0]);
        assertEquals(s, arr[1]);
    }

    /**
     * @constraint A17
     * @title invalid constant pool index
     */
    public void testVFE1() {
         try {
             Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_3");
             fail("expected a verification exception");
         } catch (Throwable t) {
             DxUtil.checkVerifyException(t);
         }
    }

    /**
     * @constraint A23 
     * @title  number of registers
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
 
    }
    
    /**
     * @constraint B1 
     * @title try to pass obj ref instead of int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title try to pass long instead of int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title try to create non-array type
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title invalid arg count
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a 
     * @title attempt to instantiate String[] and fill it with reference to assignment-incompatible class
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.filled_new_array.d.T_filled_new_array_9");
            fail("expected a verification exception"); 
        } catch(Throwable t) { 
            DxUtil.checkVerifyException(t);    
        }
    }
    
    /**
     * @constraint n/a 
     * @title attempt to instantiate array of non-existent class
     */
    public void testVFE8() {
        //@uses dot.junit.opcodes.filled_new_array.d.T_filled_new_array_10
        try {
            T_filled_new_array_10 T = new T_filled_new_array_10();
            T.run();
            fail("expected a NoClassDefFoundError exception");
        } catch(NoClassDefFoundError t) {
            // expected
        }
    }
    
    /**
     * @constraint n/a 
     * @title attempt to instantiate array of inaccessible class
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.filled_new_array.d.T_filled_new_array_11
        //@uses dot.junit.opcodes.filled_new_array.TestStubs
        try {
            T_filled_new_array_11 T = new T_filled_new_array_11();
            T.run();
            fail("expected a IllegalAccessError exception");
        } catch(IllegalAccessError t) {
            // expected
        }
    }
    
}
