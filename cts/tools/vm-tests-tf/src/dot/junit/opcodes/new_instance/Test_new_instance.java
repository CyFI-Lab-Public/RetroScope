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

package dot.junit.opcodes.new_instance;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.new_instance.d.T_new_instance_1;
import dot.junit.opcodes.new_instance.d.T_new_instance_3;
import dot.junit.opcodes.new_instance.d.T_new_instance_4;
import dot.junit.opcodes.new_instance.d.T_new_instance_5;
import dot.junit.opcodes.new_instance.d.T_new_instance_8;
import dot.junit.opcodes.new_instance.d.T_new_instance_9;


public class Test_new_instance extends DxTestCase {

    /**
     * @title new String
     */
    public void testN1() {
        T_new_instance_1 t = new T_new_instance_1();
        String s = t.run();
        assertNotNull(s);
        assertEquals(0, s.compareTo("abc"));
    }

    /**
     * @title class initialization throws exception
     */
    public void testE1() {
        try {
            T_new_instance_3.run();
            fail("expected Error");
        } catch (Error e) {
            // expected
        }
    }

    /**
     * @constraint A21
     * @title  attempt to instantiate interface
     */
    public void testE4() {
        //@uses dot.junit.opcodes.new_instance.d.TestAbstractClass
        //@uses dot.junit.opcodes.new_instance.d.T_new_instance_8
        T_new_instance_8 t = new T_new_instance_8();
        try {
            t.run();
            fail("expected InstantiationError");
        } catch (InstantiationError ie) {
            // expected
        }
    }

    /**
     * @constraint A21
     * @title  attempt to instantiate abstract
     * class
     */
    public void testE5() {
        //@uses dot.junit.opcodes.new_instance.d.TestAbstractClass
        //@uses dot.junit.opcodes.new_instance.d.T_new_instance_9
        T_new_instance_9 t = new T_new_instance_9();
        try {
            t.run();
            fail("expected Error");
        } catch (Error iae) {
            // expected
        }
    }

    /**
     * @constraint A18
     * @title  constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     *
     * @constraint A21
     * @title  attempt to create array using new
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B6
     * @title Attempt to access uninitialized class (before <init> is
     * called
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A23
     * @title number of registers
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to instantiate array of inaccessible class.
     */
    public void testVFE5() {
        //@uses dot.junit.opcodes.new_instance.TestStubs
        //@uses dot.junit.opcodes.new_instance.d.T_new_instance_4
        try {
            new T_new_instance_4().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to instantiate array of non-existent class.
     */
    public void testVFE6() {
        try {
            new T_new_instance_5().run();
            fail("expected NoClassDefFoundError");
        } catch (NoClassDefFoundError t) {
        }
    }

    /**
     * @constraint B7
     * @title A register which holds the result of a new-instance instruction must not be used
     * if the same new-instance  instruction is again executed before the instance is initialized
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B7
     * @title A register which holds the result of a new-instance instruction must not be used
     * if the same new-instance  instruction is again executed before the instance is initialized
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.opcodes.new_instance.d.T_new_instance_12");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
