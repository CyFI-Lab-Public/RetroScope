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

package dot.junit.opcodes.aget_object;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.aget_object.d.T_aget_object_1;
import dot.junit.opcodes.aget_object.d.T_aget_object_8;


public class Test_aget_object extends DxTestCase {
     /**
     * @title get reference from array
     */
    public void testN1() {
        T_aget_object_1 t = new T_aget_object_1();
        String[] arr = new String[] {"a", "b"};
        assertEquals("a", t.run(arr, 0));
    }

    /**
     * @title  get reference from array
     */
    public void testN2() {
        T_aget_object_1 t = new T_aget_object_1();
        String[] arr = new String[] {"a", "b"};
        assertEquals("b", t.run(arr, 1));
    }

    /**
     * @title expected ArrayIndexOutOfBoundsException
     */
    public void testE1() {
        T_aget_object_1 t = new T_aget_object_1();
        String[] arr = new String[] {"a", "b"};
        try {
            t.run(arr, 2);
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // expected
        }
    }

    /**
     * @title expected ArrayIndexOutOfBoundsException (negative index)
     */
    public void testE2() {
        T_aget_object_1 t = new T_aget_object_1();
        String[] arr = new String[] {"a", "b"};
        try {
            t.run(arr, -1);
            fail("expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            // expected
        }
    }

    /**
     * @title expected NullPointerException
     */
    public void testE3() {
        T_aget_object_1 t = new T_aget_object_1();
        String[] arr = null;
        try {
            t.run(arr, 0);
            fail("expected NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * @constraint B1
     * @title types of arguments - array, double
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - array, long
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - Object, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - float[], int
     */
    public void testVFE4() {
        try { 
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - long[], int
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - array, reference
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_7");
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
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Type of index argument - float. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.aget_object.d.T_aget_object_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
