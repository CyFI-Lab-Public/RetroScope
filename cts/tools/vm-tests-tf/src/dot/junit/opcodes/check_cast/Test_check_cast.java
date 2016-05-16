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

package dot.junit.opcodes.check_cast;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.check_cast.d.T_check_cast_1;
import dot.junit.opcodes.check_cast.d.T_check_cast_2;
import dot.junit.opcodes.check_cast.d.T_check_cast_3;
import dot.junit.opcodes.check_cast.d.T_check_cast_7;


public class Test_check_cast extends DxTestCase {
   
    
    /**
     * @title (String)(Object)String
     */
    public void testN1() {
        T_check_cast_1 t = new T_check_cast_1();
        String s = "";
        assertEquals(s, t.run(s));
    }

    /**
     * @title (String)(Object)null
     */
    public void testN2() {
        T_check_cast_1 t = new T_check_cast_1();
        assertNull(t.run(null));
    }

    /**
     * @title check assignment compatibility rules
     */
    public void testN4() {
        T_check_cast_2 t = new T_check_cast_2();
        assertEquals(5, t.run());
    }

    /**
     * @title expected ClassCastException
     */
    public void testE1() {
        T_check_cast_1 t = new T_check_cast_1();
        try {
            t.run(t);
            fail("expected ClassCastException");
        } catch (ClassCastException iae) {
            // expected
        }
    }

    /**
     * @constraint A18 
     * @title  constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.check_cast.d.T_check_cast_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title  type of argument - int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.check_cast.d.T_check_cast_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint B1 
     * @title  type of argument - long
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.check_cast.d.T_check_cast_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title  number of registers
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.check_cast.d.T_check_cast_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to access inaccessible class, expect throws IllegalAccessError
     */
    public void testVFE5() {
        //@uses dot.junit.opcodes.check_cast.TestStubs
        //@uses dot.junit.opcodes.check_cast.d.T_check_cast_3
        T_check_cast_3 t = new T_check_cast_3();
        try {
            t.run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError iae) {
            // expected
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to access undefined class, expect throws NoClassDefFoundError on
     * first access
     */
    public void testVFE6() {
        T_check_cast_7 t = new T_check_cast_7();
        try {
            t.run();
            fail("expected NoClassDefFoundError");
        } catch (NoClassDefFoundError iae) {
            // expected
        }
    }
    
    /**
     * @constraint A18 
     * @title  constant pool type
     */    
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.check_cast.d.T_check_cast_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
