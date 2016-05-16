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

package dot.junit.opcodes.invoke_interface_range;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_1;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_11;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_12;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_13;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_14;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_16;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_18;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_20;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_21;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_3;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_4;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_5;
import dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_7;

public class Test_invoke_interface_range extends DxTestCase {

    /**
     * @title invoke interface method
     */
    public void testN1() {
        T_invoke_interface_range_1 t = new T_invoke_interface_range_1();
        assertEquals(0, t.run("aa", "aa"));
        assertEquals(-1, t.run("aa", "bb"));
        assertEquals(1, t.run("bb", "aa"));
    }

    /**
     * @title Check that new frame is created by invoke_interface_range and
     * arguments are passed to method
     */
    public void testN2() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_14
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        T_invoke_interface_range_14 t = new T_invoke_interface_range_14();
        ITestImpl impl = new ITestImpl();
        assertEquals(1, t.run(impl));
    }



    /**
     * @title objref is null
     */
    public void testE3() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_3
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        try {
            new T_invoke_interface_range_3(null);
            fail("expected NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * @title object doesn't implement interface
     */
    public void testE4() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_11
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        T_invoke_interface_range_11 t = new T_invoke_interface_range_11();
        try {
            t.run();
            fail("expected IncompatibleClassChangeError");
        } catch (IncompatibleClassChangeError e) {
            // expected
        }
    }

    /**
     * @title Native method can't be linked
     */
    public void testE5() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_12
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        T_invoke_interface_range_12 t = new T_invoke_interface_range_12();
        ITestImpl impl = new ITestImpl();
        try {
            t.run(impl);
            fail("expected UnsatisfiedLinkError");
        } catch (UnsatisfiedLinkError e) {
            // expected
        }
    }

    /**
     * @title Attempt to invoke abstract method
     */
    public void testE6() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_13
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImplAbstract
        T_invoke_interface_range_13 t = new T_invoke_interface_range_13();
        try {
            t.run();
            fail("expected AbstractMethodError");
        } catch (AbstractMethodError e) {
            // expected
        }
    }

    /**
     * @constraint A17
     * @title invalid constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A17
     * @title The referenced method_id must belong to an interface (not a class).
     */
    public void testVFE2() {
        try {
            new T_invoke_interface_range_4().run();
            fail("expected NoSuchMethodError or IncompatibleClassChangeError");
        } catch (NoSuchMethodError t) {
        } catch (IncompatibleClassChangeError e) {
        }
    }

    /**
     * @constraint B1
     * @title number of arguments
     */
    public void testVFE5() {
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            new T_invoke_interface_range_5(new ITestImpl());
            fail("expected VerifyError");
        } catch (VerifyError t) {
        }
    }

    /**
     * @constraint B1
     * @title int is passed instead of objref
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B9
     * @title number of arguments passed to method
     */
    public void testVFE9() {
        try {
            Class.forName("dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_9");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A15
     * @title invoke-interface may not be used to call <init>.
     */
    public void testVFE10() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_18
        try {
            new T_invoke_interface_range_18().run(new ITestImpl());
            fail("expected NoSuchMethodError or verification exception");
        } catch (NoSuchMethodError t) {
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A15
     * @title invoke-interface may not be used to call <clinit>.
     */
    public void testVFE11() {
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            new T_invoke_interface_range_20().run(new ITestImpl());
            fail("expected NoSuchMethodError or verification exception");
        } catch (NoSuchMethodError t) {
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B9
     * @title types of arguments passed to method
     */
    public void testVFE12() {
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            new T_invoke_interface_range_21().run(new ITestImpl());
            fail("expected VerifyError");
        } catch (VerifyError t) {
        }
    }

    /**
     * @constraint A23
     * @title number of registers
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to call undefined method.
     */
    public void testVFE14() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_7
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            new T_invoke_interface_range_7().run(new ITestImpl());
            fail("expected NoSuchMethodError");
        } catch (NoSuchMethodError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Method has different signature.
     */
    public void testVFE15() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_16
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            new T_invoke_interface_range_16().run(new ITestImpl());
            fail("expected NoSuchMethodError");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B6
     * @title instance methods may only be invoked on already initialized instances.
     */
    public void testVFE21() {
        //@uses dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_22
        //@uses dot.junit.opcodes.invoke_interface_range.ITest
        //@uses dot.junit.opcodes.invoke_interface_range.ITestImpl
        try {
            Class.forName("dot.junit.opcodes.invoke_interface_range.d.T_invoke_interface_range_22");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
