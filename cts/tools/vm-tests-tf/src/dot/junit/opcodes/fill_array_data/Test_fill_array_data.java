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

package dot.junit.opcodes.fill_array_data;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.fill_array_data.d.T_fill_array_data_1;
import dot.junit.opcodes.fill_array_data.d.T_fill_array_data_2;

public class Test_fill_array_data extends DxTestCase {
    /**
     * @title array of ints
     */
    public void testN1() {
        int arr[] = new int[5];
        T_fill_array_data_1 t = new T_fill_array_data_1();
        t.run(arr);
        for(int i = 0; i < 5; i++)
            assertEquals(i + 1, arr[i]);
     }
    
    /**
     * @title array of doubles
     */
    public void testN2() {
        double arr[] = new double[5];
        T_fill_array_data_2 t = new T_fill_array_data_2();
        t.run(arr);
        for(int i = 0; i < 5; i++)
            assertEquals((double)(i + 1), arr[i]);
     }

    /**
     * @title If there are less elements in the table than the array provides space for, 
     * the remaining array elements stay untouched.  
     */
    public void testN3() {
        int arr[] = new int[10];
        T_fill_array_data_1 t = new T_fill_array_data_1();
        t.run(arr);
        for(int i = 0; i < 5; i++)
            assertEquals(i + 1, arr[i]);
        for(int i = 5; i < 10; i++)
            assertEquals(0, arr[i]);
     }
    
    /**
     * @title expected NullPointerException  
     */
    public void testE1() {
        T_fill_array_data_1 t = new T_fill_array_data_1();
        try {
            t.run(null);
        } catch(NullPointerException npe) {
            // expected
        }
     }
    
    /**
     * @title expected ArrayIndexOutOfBoundsException  
     */
    public void testE2() {
        int arr[] = new int[2];
        T_fill_array_data_1 t = new T_fill_array_data_1();
       try {
            t.run(arr);
        } catch(ArrayIndexOutOfBoundsException e) {
            // expected
           }
     }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title type of argument - double
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title type of argument - long
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title type of argument - reference (not array)
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title array of Objects
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title array type and data size shall be consistent
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title offset to table shall be inside method
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title the size and the list must be consistent. 
     */
    public void testVFE9() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    
    /**
     * @constraint B22
     * @title packed-switch-data pseudo-instructions must not be reachable by control flow 
     */
    public void testVFE10() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_12");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title table has wrong ident code
     */
    public void testVFE11() {
        try {
            Class.forName("dot.junit.opcodes.fill_array_data.d.T_fill_array_data_13");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
