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

package dot.junit.opcodes.rsub_int;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_1;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_2;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_3;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_4;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_5;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_6;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_7;
import dot.junit.opcodes.rsub_int.d.T_rsub_int_12;

public class Test_rsub_int extends DxTestCase {

    /**
     * @title normal test - check different values
     */
    public void testN1() {
        T_rsub_int_1 t = new T_rsub_int_1();
        assertEquals("Subtest_1 is failed", -4, t.run(8));
        assertEquals("Subtest_2 is failed",45, t.run1(15));
        assertEquals("Subtest_3 is failed",0, t.run2(20));
        assertEquals("Subtest_4 is failed",-35, t.run3(10));
        assertEquals("Subtest_5 is failed",-20, t.run4(-50));
        assertEquals("Subtest_6 is failed",20, t.run5(-70));
    }

    /**
     * @title normal test - check different values
     */
    public void testN2() {
        T_rsub_int_2 t = new T_rsub_int_2();
        assertEquals("Subtest_1 is failed",255, t.run(0));
        assertEquals("Subtest_2 is failed",-32768, t.run1(0));
        assertEquals("Subtest_3 is failed",-15, t.run2(15));
        assertEquals("Subtest_4 is failed",123, t.run2(-123));
    }

    /**
     * @title 
     * 1: a = Integer.MAX_VALUE, b = 0, b-a = -Integer.MAX_VALUE
     * 2: a = Short.MAX_VALUE, b = 0, b-a = -Short.MAX_VALUE
     */
    public void testB1() {
        T_rsub_int_3 t = new T_rsub_int_3();
        assertEquals(-Integer.MAX_VALUE, t.run(Integer.MAX_VALUE));
        assertEquals(-Short.MAX_VALUE, t.run(Short.MAX_VALUE));
    }
    
    /**
     * @title 
     * 1: a = Integer.MIN_VALUE, b = 0, b-a = Integer.MIN_VALUE
     * 2: a = Short.MIN_VALUE, b = 0, b-a = 32768
     */
    public void testB2() {
        T_rsub_int_3 t = new T_rsub_int_3();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MIN_VALUE));
        assertEquals(32768, t.run(Short.MIN_VALUE));
    }
    
    /**
     * @title (a = 0, b = 0, b-a = 0)
     */
    public void testB3() {
        T_rsub_int_3 t = new T_rsub_int_3();
        assertEquals(0, t.run(0));
    }
    
    /**
     * @title 
     * 1: a = 0, b = Short.MAX_VALUE, b-a = Short.MAX_VALUE
     * 2: a = 1, b = Short.MAX_VALUE, b-a = 32766
     * 3: a = -1, b = Short.MAX_VALUE, b-a = 32768
     */
    public void testB4() {
        T_rsub_int_4 t = new T_rsub_int_4();
        assertEquals(Short.MAX_VALUE, t.run(0));
        assertEquals(32766, t.run(1));
        assertEquals(32768, t.run(-1));
    }
    
    /**
     * @title 
     * 1: a = Integer.MIN_VALUE, b = Short.MAX_VALUE, b-a = -2147450881
     * 2: a = Integer.MAX_VALUE, b = Short.MAX_VALUE, b-a = -2147450880
     * 3: a = Short.MIN_VALUE, b = Short.MAX_VALUE, b-a = 65535
     */
    public void testB5() {
        T_rsub_int_4 t = new T_rsub_int_4();
        assertEquals(-2147450881, t.run(Integer.MIN_VALUE));
        assertEquals(-2147450880, t.run(Integer.MAX_VALUE));
        assertEquals(65535, t.run(Short.MIN_VALUE));
    }
    
    /**
     * @title 
     * 1: a = 0, b = Short.MIN_VALUE, b-a = Short.MIN_VALUE
     * 2: a = 1, b = Short.MIN_VALUE, b-a = -32769
     * 3: a = -1, b = Short.MIN_VALUE, b-a = -32767
     */
    public void testB6() {
        T_rsub_int_5 t = new T_rsub_int_5();
        assertEquals(Short.MIN_VALUE, t.run(0));
        assertEquals(-32769, t.run(1));
        assertEquals(-32767, t.run(-1));
    }
    
    /**
     * @title 
     * 1: a = Integer.MAX_VALUE, b = Short.MIN_VALUE, b-a = 2147450881
     * 2: a = Integer.MIN_VALUE, b = Short.MIN_VALUE, b-a = 2147450880
     * 3: a = Short.MAX_VALUE, b = Short.MIN_VALUE, b-a = -65535
     */
    public void testB7() {
        T_rsub_int_5 t = new T_rsub_int_5();
        assertEquals(2147450881, t.run(Integer.MAX_VALUE));
        assertEquals(2147450880, t.run(Integer.MIN_VALUE));
        assertEquals(-65535, t.run(Short.MAX_VALUE));
    }
    
    /**
     * @title 
     * 1: a = Integer.MIN_VALUE, b = -1, b-a = Integer.MAX_VALUE
     * 2: a = Short.MIN_VALUE, b = -1, b-a = Short.MAX_VALUE 
     */
    public void testB8() {
        T_rsub_int_6 t = new T_rsub_int_6();
        assertEquals(Integer.MAX_VALUE, t.run(Integer.MIN_VALUE));
        assertEquals(Short.MAX_VALUE, t.run(Short.MIN_VALUE));
    }
    
    /**
     * @title 
     * 1: a = Integer.MAX_VALUE, b = -1, b-a = Integer.MIN_VALUE
     * 2: a = Short.MAX_VALUE, b = -1, b-a = -32768
     */
    public void testB9() {
        T_rsub_int_6 t = new T_rsub_int_6();
        assertEquals(Integer.MIN_VALUE, t.run(Integer.MAX_VALUE));
        assertEquals(-32768, t.run(Short.MAX_VALUE));
    }
    
    /**
     * @title 
     * 1: a = Integer.MIN_VALUE, b = 1, b-a = -Integer.MAX_VALUE
     * 2: a = Integer.MAX_VALUE, b = 1, b-a = -2147483646
     */
    public void testB10() {
        T_rsub_int_7 t = new T_rsub_int_7();
        assertEquals(-Integer.MAX_VALUE, t.run(Integer.MIN_VALUE));
        assertEquals(-2147483646, t.run(Integer.MAX_VALUE));
    }
    
    /**
     * @title 
     * 1: a = Short.MIN_VALUE, b = 1, b-a = 32769
     * 2: a = Short.MAX_VALUE, b = 1, b-a = -32766
     */
    public void testB11() {
        T_rsub_int_7 t = new T_rsub_int_7();
        assertEquals(32769, t.run(Short.MIN_VALUE));
        assertEquals(-32766, t.run(Short.MAX_VALUE));
    }
    
    /**
     * @title (a = 1, b = 1, b-a = 0)
     */
    public void testB12() {
        T_rsub_int_7 t = new T_rsub_int_7();
        assertEquals(0, t.run(1));
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.rsub_int.d.T_rsub_int_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    

    /**
     * @constraint B1 
     * @title types of arguments - double, int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.rsub_int.d.T_rsub_int_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B1 
     * @title types of arguments - long, int
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.rsub_int.d.T_rsub_int_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference, int
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.rsub_int.d.T_rsub_int_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - float, int. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.rsub_int.d.T_rsub_int_12");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
