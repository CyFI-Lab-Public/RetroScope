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

package dot.junit.opcodes.iget;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.iget.d.T_iget_1;
import dot.junit.opcodes.iget.d.T_iget_11;
import dot.junit.opcodes.iget.d.T_iget_12;
import dot.junit.opcodes.iget.d.T_iget_13;
import dot.junit.opcodes.iget.d.T_iget_2;
import dot.junit.opcodes.iget.d.T_iget_21;
import dot.junit.opcodes.iget.d.T_iget_5;
import dot.junit.opcodes.iget.d.T_iget_6;
import dot.junit.opcodes.iget.d.T_iget_7;
import dot.junit.opcodes.iget.d.T_iget_8;
import dot.junit.opcodes.iget.d.T_iget_9;

public class Test_iget extends DxTestCase {
    
    /**
     * @title type - int
     */
    public void testN1() {
        T_iget_1 t = new T_iget_1();
        assertEquals(5, t.run());
    }

    /**
     * @title type - float
     */
    public void testN2() {
        T_iget_2 t = new T_iget_2();
        assertEquals(123f, t.run());
    }

    /**
     * @title access protected field from subclass
     */
    public void testN3() {
        //@uses dot.junit.opcodes.iget.d.T_iget_1
        //@uses dot.junit.opcodes.iget.d.T_iget_11
        T_iget_11 t = new T_iget_11();
        assertEquals(10, t.run());
    }

    /**
     * @title expected NullPointerException
     */
    public void testE2() {
        T_iget_9 t = new T_iget_9();
        try {
            t.run();
            fail("expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }
    
    /**
     * @constraint A11 
     * @title constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * 
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B13 
     * @title read integer from long field - only field with same name but 
     * different type exist
     */
    public void testVFE3() {
        try {
            new T_iget_13().run();
            fail("expected a NoSuchFieldError exception");
        } catch (NoSuchFieldError e) {
            // expected
        }
    }
    
    /**
     * @constraint n/a
     * @title Attempt to read inaccessible private field.
     */
    public void testVFE4() {
        //@uses dot.junit.opcodes.iget.d.T_iget_6
        //@uses dot.junit.opcodes.iget.TestStubs
        try {
            new T_iget_6().run();
            fail("expected an IllegalAccessError exception");
        } catch (IllegalAccessError e) {
            // expected
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read field of undefined class.
     */
    public void testVFE5() {
        try {
            new T_iget_7().run();
            fail("expected a NoClassDefFoundError exception");
        } catch (NoClassDefFoundError e) {
            // expected
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read undefined field.
     */
    public void testVFE6() {
        try {
            new T_iget_8().run();
            fail("expected a NoSuchFieldError exception");
        } catch (NoSuchFieldError e) {
            // expected
        }
    }
    
    /**
     * @constraint n/a
     * @title Attempt to read superclass' private field from subclass.
     */
    public void testVFE7() {
        //@uses dot.junit.opcodes.iget.d.T_iget_12
        //@uses dot.junit.opcodes.iget.d.T_iget_1
        try {
            new T_iget_12().run();
            fail("expected an IllegalAccessError exception");
        } catch (IllegalAccessError e) {
            // expected
        }
    }
   
    /**
     * @constraint B1 
     * @title iget shall not work for reference fields
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_14");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for short fields
     */
    public void testVFE9() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_15");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for boolean fields
     */
    public void testVFE10() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_16");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for char fields
     */
    public void testVFE11() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_17");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for byte fields
     */
    public void testVFE12() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_18");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }    
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for double fields
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_19");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    } 
    
    /**
     * 
     * @constraint B1 
     * @title iget shall not work for long fields
     */
    public void testVFE14() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B12
     * @title Attempt to read protected field of unrelated class.
     */
    public void testVFE15() {
        //@uses dot.junit.opcodes.iget.d.T_iget_21
        //@uses dot.junit.opcodes.iget.TestStubs
        try {
            new T_iget_21().run();
            fail("expected an IllegalAccessError exception");
        } catch (IllegalAccessError e) {
            // expected
        }
    }
    
    /**
     * @constraint A11
     * @title Attempt to read static field.
     */
    public void testVFE16() {
        //@uses dot.junit.opcodes.iget.d.T_iget_5
        //@uses dot.junit.opcodes.iget.TestStubs
        try {
            new T_iget_5().run();
            fail("expected an IncompatibleClassChangeError exception");
        } catch (IncompatibleClassChangeError e) {
            // expected
        }
    }

    /**
     * @constraint B6 
     * @title instance fields may only be accessed on already initialized instances. 
     */
    public void testVFE30() {
        try {
            Class.forName("dot.junit.opcodes.iget.d.T_iget_30");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}

