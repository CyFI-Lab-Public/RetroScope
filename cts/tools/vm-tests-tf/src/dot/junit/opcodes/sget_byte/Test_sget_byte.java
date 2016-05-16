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

package dot.junit.opcodes.sget_byte;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_1;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_11;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_12;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_13;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_5;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_6;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_7;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_8;
import dot.junit.opcodes.sget_byte.d.T_sget_byte_9;

public class Test_sget_byte extends DxTestCase {

    /**
     * @title get byte from static field
     */
    public void testN1() {
        T_sget_byte_1 t = new T_sget_byte_1();
        assertEquals(77, t.run());
    }


    /**
     * @title access protected field from subclass
     */
    public void testN3() {
        //@uses dot.junit.opcodes.sget_byte.d.T_sget_byte_1
        //@uses dot.junit.opcodes.sget_byte.d.T_sget_byte_11
        T_sget_byte_11 t = new T_sget_byte_11();
        assertEquals(77, t.run());
    }

    /**
     * @constraint A12
     * @title attempt to access non-static field
     */
    public void testE1() {

        T_sget_byte_5 t = new T_sget_byte_5();
        try {
            t.run();
            fail("expected IncompatibleClassChangeError");
        } catch (IncompatibleClassChangeError e) {
            // expected
        }
    }

    /**
     * @title initialization of referenced class throws exception
     */
    public void testE6() {
        T_sget_byte_9 t = new T_sget_byte_9();
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
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_4");
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
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B13
     * @title read byte from long field - only field with same name but
     * different type exists
     */
    public void testVFE3() {
        try {
            new T_sget_byte_13().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read inaccessible field.
     */
    public void testVFE4() {
        //@uses dot.junit.opcodes.sget_byte.d.T_sget_byte_6
        //@uses dot.junit.opcodes.sget_byte.TestStubs
        try {
            new T_sget_byte_6().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read field of undefined class.
     */
    public void testVFE5() {
        try {
            new T_sget_byte_7().run();
            fail("expected NoClassDefFoundError");
        } catch (NoClassDefFoundError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read undefined field.
     */
    public void testVFE6() {
        try {
            new T_sget_byte_8().run();
            fail("expected NoSuchFieldError");
        } catch (NoSuchFieldError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to read superclass' private field from subclass.
     */
    public void testVFE7() {
        //@uses dot.junit.opcodes.sget_byte.d.T_sget_byte_12
        //@uses dot.junit.opcodes.sget_byte.d.T_sget_byte_1
        try {
            new T_sget_byte_12().run();
            fail("expected a verification exception");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint B1
     * @title sget_byte shall not work for reference fields
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_14");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for short fields
     */
    public void testVFE9() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_15");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for int fields
     */
    public void testVFE10() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_16");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for char fields
     */
    public void testVFE11() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_17");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for boolean fields
     */
    public void testVFE12() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_18");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for double fields
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_19");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint B1
     * @title sget_byte shall not work for long fields
     */
    public void testVFE14() {
        try {
            Class.forName("dot.junit.opcodes.sget_byte.d.T_sget_byte_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
