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

package dot.junit.opcodes.iput_boolean;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_1;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_10;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_11;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_12;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_13;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_14;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_15;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_17;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_7;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_8;
import dot.junit.opcodes.iput_boolean.d.T_iput_boolean_9;


public class Test_iput_boolean extends DxTestCase {

    /**
     * @title put boolean into field
     */
    public void testN1() {
        T_iput_boolean_1 t = new T_iput_boolean_1();
        assertEquals(false, t.st_i1);
        t.run();
        assertEquals(true, t.st_i1);
    }


    /**
     * @title modification of final field
     */
    public void testN2() {
        T_iput_boolean_12 t = new T_iput_boolean_12();
        assertEquals(false, t.st_i1);
        t.run();
        assertEquals(true, t.st_i1);
    }

    /**
     * @title modification of protected field from subclass
     */
    public void testN4() {
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_1
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_14
        T_iput_boolean_14 t = new T_iput_boolean_14();
        assertEquals(false, t.getProtectedField());
        t.run();
        assertEquals(true, t.getProtectedField());
    }



    /**
     * @title expected NullPointerException
     */
    public void testE2() {
        T_iput_boolean_13 t = new T_iput_boolean_13();
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
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A23
     * @title number of registers
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * @constraint B14
     * @title put boolean into long field - only field with same name but
     * different type exists
     */
    public void testVFE5() {
        try {
            new T_iput_boolean_17().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }

    /**
     *
     * @constraint B14
     * @title type of field doesn't match opcode - attempt to modify double
     * field with single-width register
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_18");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint A11
     * @title Attempt to set static field.
     */
    public void testVFE8() {
         try {
             new T_iput_boolean_7().run();
             fail("expected IncompatibleClassChangeError");
         } catch (IncompatibleClassChangeError t) {
         }
    }

    /**
     * @constraint B12
     * @title Attempt to modify inaccessible protected field.
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.iput_boolean.TestStubs
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_8
        try {
            new T_iput_boolean_8().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify field of undefined class.
     */
    public void testVFE10() {
        try {
            new T_iput_boolean_9().run();
            fail("expected NoClassDefFoundError");
        } catch (NoClassDefFoundError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify undefined field.
     */
    public void testVFE11() {
        try {
            new T_iput_boolean_10().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }



    /**
     * @constraint n/a
     * @title Attempt to modify superclass' private field from subclass.
     */
    public void testVFE12() {
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_1
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_15
        try {
            new T_iput_boolean_15().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }


    /**
     * @constraint B1
     * @title iput_boolean shall not work for wide numbers
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title iput_boolean shall not work for reference fields
     */
    public void testVFE14() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title iput_boolean shall not work for short fields
     */
    public void testVFE15() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_21");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title iput_boolean shall not work for int fields
     */
    public void testVFE16() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_22");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title iput_boolean shall not work for char fields
     */
    public void testVFE17() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_23");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title iput_boolean shall not work for byte fields
     */
    public void testVFE18() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_24");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B6
     * @title instance fields may only be accessed on already initialized instances.
     */
    public void testVFE30() {
        try {
            Class.forName("dot.junit.opcodes.iput_boolean.d.T_iput_boolean_30");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title Modification of final field in other class
     */
    public void testVFE19() {
        //@uses dot.junit.opcodes.iput_boolean.TestStubs
        //@uses dot.junit.opcodes.iput_boolean.d.T_iput_boolean_11
        try {
            new T_iput_boolean_11().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }
}

