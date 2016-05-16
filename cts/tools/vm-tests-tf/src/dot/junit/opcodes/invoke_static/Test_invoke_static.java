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

package dot.junit.opcodes.invoke_static;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_1;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_13;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_14;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_15;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_17;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_18;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_19;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_2;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_24;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_4;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_5;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_6;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_7;
import dot.junit.opcodes.invoke_static.d.T_invoke_static_8;



public class Test_invoke_static extends DxTestCase {

    /**
     * @title Static method from library class Math
     */
    public void testN1() {
        T_invoke_static_1 t = new T_invoke_static_1();
        assertEquals(1234567, t.run());
    }

    /**
     * @title Static method from user class
     */
    public void testN2() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_2
        //@uses dot.junit.opcodes.invoke_static.TestClass
        T_invoke_static_2 t = new T_invoke_static_2();
        assertEquals(777, t.run());
    }

    /**
     * @title Check that <clinit> is called
     */
    public void testN3() {
        assertEquals(123456789l, T_invoke_static_4.run());
    }


    /**
     * @title Check that new frame is created by invoke_static and
     * arguments are passed to method
     */
    public void testN5() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_15
        //@uses dot.junit.opcodes.invoke_static.TestClass
        T_invoke_static_15 t = new T_invoke_static_15();
        assertTrue(t.run());
    }

    /**
     * @title Static protected method from other class in the same package
     */
    public void testN6() {
        T_invoke_static_18 t = new T_invoke_static_18();
        assertEquals(888, t.run());
    }

    /**
     * @title Native method can't be linked
     *
     */
    public void testE2() {
        T_invoke_static_6 t = new T_invoke_static_6();
        try {
            t.run();
            fail("expected UnsatisfiedLinkError");
        } catch (UnsatisfiedLinkError ule) {
            // expected
        }
    }


    /**
     * @title initialization of referenced class throws exception
     */
    public void testE7() {
        T_invoke_static_14 t = new T_invoke_static_14();
        try {
            t.run();
            fail("expected Error");
        } catch (Error e) {
            // expected
        }
    }


    /**
     * @constraint A13
     * @title  invalid constant pool index
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.invoke_static.d.T_invoke_static_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A15
     * @title &lt;clinit&gt; may not be called using invoke-static
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.invoke_static.d.T_invoke_static_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title number of arguments passed to method.
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.invoke_static.d.T_invoke_static_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A15
     * @title &lt;init&gt; may not be called using invoke_static
     */
    public void testVFE5() {
        try {
            new T_invoke_static_19().run();
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B9
     * @title types of arguments passed to method
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.opcodes.invoke_static.d.T_invoke_static_20");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


    /**
     * @constraint n/a
     * @title Attempt to call non-static method.
     */
    public void testVFE7() {
         try {
             new T_invoke_static_5().run();
             fail("expected IncompatibleClassChangeError");
         } catch (IncompatibleClassChangeError t) {
         }
    }

    /**
     * @constraint n/a
     * @title Attempt to call undefined method.
     */
    public void testVFE8() {
        try {
            new T_invoke_static_7().run();
            fail("expected NoSuchMethodError");
        } catch (NoSuchMethodError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Attempt to call private method of other class.
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_8
        //@uses dot.junit.opcodes.invoke_static.TestClass
        try {
            new T_invoke_static_8().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint n/a
     * @title Method has different signature.
     */
    public void testVFE10() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_13
        //@uses dot.junit.opcodes.invoke_static.TestClass
        try {
            new T_invoke_static_13().run();
            fail("expected NoSuchMethodError");
        } catch (NoSuchMethodError t) {
        }
    }


    /**
     * @constraint B12
     * @title Attempt to call protected method of unrelated class.
     */
    public void testVFE12() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_17
        //@uses dot.junit.opcodes.invoke_static.TestClass
        try {
            new T_invoke_static_17().run();
            fail("expected IllegalAccessError");
        } catch (IllegalAccessError t) {
        }
    }

    /**
     * @constraint A23
     * @title number of registers
     */
    public void testVFE13() {
        try {
            Class.forName("dot.junit.opcodes.invoke_static.d.T_invoke_static_16");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint A13
     * @title attempt to invoke interface method
     */
    public void testVFE18() {
        //@uses dot.junit.opcodes.invoke_static.d.T_invoke_static_24
        try {
            new T_invoke_static_24().run();
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
