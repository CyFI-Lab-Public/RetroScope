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

package dot.junit.opcodes.sput;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.sput.d.T_sput_1;
import dot.junit.opcodes.sput.d.T_sput_10;
import dot.junit.opcodes.sput.d.T_sput_11;
import dot.junit.opcodes.sput.d.T_sput_12;
import dot.junit.opcodes.sput.d.T_sput_13;
import dot.junit.opcodes.sput.d.T_sput_14;
import dot.junit.opcodes.sput.d.T_sput_15;
import dot.junit.opcodes.sput.d.T_sput_17;
import dot.junit.opcodes.sput.d.T_sput_19;
import dot.junit.opcodes.sput.d.T_sput_5;
import dot.junit.opcodes.sput.d.T_sput_7;
import dot.junit.opcodes.sput.d.T_sput_8;
import dot.junit.opcodes.sput.d.T_sput_9;


public class Test_sput extends DxTestCase {

    /**
     * @title type - int
     */
    public void testN1() {
        T_sput_1 t = new T_sput_1();
        assertEquals(0, T_sput_1.st_i1);
        t.run();
        assertEquals(1000000, T_sput_1.st_i1);
    }

    /**
     * @title type - float
     */
    public void testN2() {
        T_sput_19 t = new T_sput_19();
        assertEquals(0.0f, T_sput_19.st_f1);
        t.run();
        assertEquals(3.14f, T_sput_19.st_f1);
    }


    /**
     * @title modification of final field
     */
    public void testN3() {
        T_sput_12 t = new T_sput_12();
        assertEquals(0, T_sput_12.st_i1);
        t.run();
        assertEquals(1000000, T_sput_12.st_i1);
    }

    /**
     * @title modification of protected field from subclass
     */
    public void testN4() {
        //@uses dot.junit.opcodes.sput.d.T_sput_1
        //@uses dot.junit.opcodes.sput.d.T_sput_14
        T_sput_14 t = new T_sput_14();
        assertEquals(0, T_sput_14.getProtectedField());
        t.run();
        assertEquals(1000000, T_sput_14.getProtectedField());
    }



    /**
     * @title initialization of referenced class throws exception
     */
    public void testE6() {
        T_sput_13 t = new T_sput_13();
        try {
            t.run();
            fail("expected Error");
        } catch (Error e) {
            // expected
        }
    }

    /**
     * @constraint A12
     * @title  constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint A23
     * @title  number of registers
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     *
     * @constraint B13
     * @title  put integer into long field - only field with same name but
     * different type exists
     */
    public void testVFE5() {
        try {
            new T_sput_17().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }

    /**
     * @constraint B1
     * @title Trying to put float into integer field. The verifier checks that ints
     * and floats are not used interchangeably.
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B13
     * @title type of field doesn't match opcode - attempt to modify double field
     * with single-width register
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_18");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint A12
     * @title Attempt to set non-static field.
     */
    public void testVFE8() {
         try {
             new T_sput_7().run();
             fail("expected IncompatibleClassChangeError");
         } catch (IncompatibleClassChangeError t) {
         }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify inaccessible field.
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.sput.TestStubs
        //@uses dot.junit.opcodes.sput.d.T_sput_8
        try {
            new T_sput_8().run();
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
            new T_sput_9().run();
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
            new T_sput_10().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }



    /**
     * @constraint n/a
     * @title Attempt to modify superclass' private field from subclass.
     */
    public void testVFE12() {
        //@uses dot.junit.opcodes.sput.d.T_sput_1
        //@uses dot.junit.opcodes.sput.d.T_sput_15
        try {
             new T_sput_15().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }


    /**
     * @constraint B1
     * @title sput shall not work for wide numbers
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput shall not work for reference fields
     */
    public void testVFE14() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput shall not work for short fields
     */
    public void testVFE15() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_21");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput shall not work for boolean fields
     */
    public void testVFE16() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_22");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput shall not work for char fields
     */
    public void testVFE17() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_23");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput shall not work for byte fields
     */
    public void testVFE18() {
        try {
            Class.forName("dot.junit.opcodes.sput.d.T_sput_24");
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
        //@uses dot.junit.opcodes.sput.TestStubs
        //@uses dot.junit.opcodes.sput.d.T_sput_11
    	try {
            new T_sput_11().run();
            fail("expected a verification exception");
        } catch (IllegalAccessError t) {
        }
    }

}
