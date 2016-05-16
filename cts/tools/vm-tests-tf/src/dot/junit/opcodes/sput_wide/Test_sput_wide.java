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

package dot.junit.opcodes.sput_wide;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_1;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_10;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_11;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_12;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_13;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_14;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_15;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_17;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_5;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_7;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_8;
import dot.junit.opcodes.sput_wide.d.T_sput_wide_9;

public class Test_sput_wide extends DxTestCase {
    /**
     * @title put long into static field
     */
    public void testN1() {
        T_sput_wide_1 t = new T_sput_wide_1();
        assertEquals(0, T_sput_wide_1.st_i1);
        t.run();
        assertEquals(778899112233l, T_sput_wide_1.st_i1);
    }

    /**
     * @title put double into static field
     */
    public void testN2() {
        T_sput_wide_5 t = new T_sput_wide_5();
        assertEquals(0.0d, T_sput_wide_5.st_i1);
        t.run();
        assertEquals(0.5d, T_sput_wide_5.st_i1);
    }


    /**
     * @title modification of final field
     */
    public void testN3() {
        T_sput_wide_12 t = new T_sput_wide_12();
        assertEquals(0, T_sput_wide_12.st_i1);
        t.run();
        assertEquals(77, T_sput_wide_12.st_i1);
    }

    /**
     * @title modification of protected field from subclass
     */
    public void testN4() {
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_1
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_14
        T_sput_wide_14 t = new T_sput_wide_14();
        assertEquals(0, T_sput_wide_14.getProtectedField());
        t.run();
        assertEquals(77, T_sput_wide_14.getProtectedField());
    }

    /**
     * @title initialization of referenced class throws exception
     */
    public void testE6() {
        T_sput_wide_13 t = new T_sput_wide_13();
        try {
            t.run();
            fail("expected Error");
        } catch (Error e) {
            // expected
        }
    }

    /**
     * @constraint A12
     * @title constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_3");
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
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     *
     * @constraint B13
     * @title put int into long field - only field with same name but
     * different type exists
     */
    public void testVFE5() {
        try {
            new T_sput_wide_17().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }



    /**
     *
     * @constraint B13
     * @title type of field doesn't match opcode - attempt to modify float
     * field with double-width register
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_18");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint A12
     * @title Attempt to set non-static field.Dalvik throws IncompatibleClassChangeError when
     * executing the code.
     */
    public void testVFE8() {
         try {
             new T_sput_wide_7().run();
             fail("expected IncompatibleClassChangeError");
         } catch (IncompatibleClassChangeError t) {
         }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify inaccessible field. Dalvik throws IllegalAccessError when executing
     * the code.
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.sput_wide.TestStubs
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_8
        try {
            new T_sput_wide_8().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify field of undefined class. Dalvik throws NoClassDefFoundError when
     * executing the code.
     */
    public void testVFE10() {
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_9
        try {
            new T_sput_wide_9().run();
            fail("expected NoClassDefFoundError");
        } catch (NoClassDefFoundError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to modify undefined field. Dalvik throws NoSuchFieldError when executing the
     * code.
     */
    public void testVFE11() {
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_10
        try {
            new T_sput_wide_10().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }



    /**
     * @constraint n/a
     * @title Attempt to modify superclass' private field from subclass. Dalvik throws
     * IllegalAccessError when executing the code.
     */
    public void testVFE12() {
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_1
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_15
        try {
            new T_sput_wide_15().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * @constraint B1
     * @title sput-wide shall not work for single-width numbers
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for reference fields
     */
    public void testVFE14() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for char fields
     */
    public void testVFE15() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_21");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for int fields
     */
    public void testVFE16() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_22");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for byte fields
     */
    public void testVFE17() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_23");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for boolean fields
     */
    public void testVFE18() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_24");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sput-wide shall not work for short fields
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.sput_wide.d.T_sput_wide_6");
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
        //@uses dot.junit.opcodes.sput_wide.TestStubs
        //@uses dot.junit.opcodes.sput_wide.d.T_sput_wide_11
    	try {
            new T_sput_wide_11().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }


}
