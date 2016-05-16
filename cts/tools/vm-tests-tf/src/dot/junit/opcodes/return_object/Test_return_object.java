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

package dot.junit.opcodes.return_object;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.return_object.d.T_return_object_1;
import dot.junit.opcodes.return_object.d.T_return_object_12;
import dot.junit.opcodes.return_object.d.T_return_object_13;
import dot.junit.opcodes.return_object.d.T_return_object_2;
import dot.junit.opcodes.return_object.d.T_return_object_6;
import dot.junit.opcodes.return_object.d.T_return_object_8;


public class Test_return_object extends DxTestCase {
    /**
     * @title simple
     */
    public void testN1() {
        T_return_object_1 t = new T_return_object_1();
        assertEquals("hello", t.run());
    }

    /**
     * @title simple
     */
    public void testN2() {
        T_return_object_1 t = new T_return_object_1();
        assertEquals(t, t.run2());
    }

    /**
     * @title return null
     */
    public void testN4() {
        T_return_object_2 t = new T_return_object_2();
        assertNull(t.run());
    }

    /**
     * @title check that frames are discarded and reinstananted correctly
     */
    public void testN5() {
        T_return_object_6 t = new T_return_object_6();
        assertEquals("hello", t.run());
    }


    /**
     * @title assignment compatibility (TChild returned as TSuper)
     */
    public void testN7() {
        //@uses dot.junit.opcodes.return_object.d.T_return_object_12
        //@uses dot.junit.opcodes.return_object.d.TChild
        //@uses dot.junit.opcodes.return_object.d.TSuper
        //@uses dot.junit.opcodes.return_object.d.TInterface
        T_return_object_12 t = new T_return_object_12();
        assertTrue(t.run());
    }

    /**
     * @title assignment compatibility (TChild returned as TInterface)
     */
    public void testN8() {
        //@uses dot.junit.opcodes.return_object.d.T_return_object_13
        //@uses dot.junit.opcodes.return_object.d.TChild
        //@uses dot.junit.opcodes.return_object.d.TSuper
        //@uses dot.junit.opcodes.return_object.d.TInterface
        T_return_object_13 t = new T_return_object_13();
        assertTrue(t.run());
    }

    /**
     * @title Method is synchronized but thread is not monitor owner
     */
    public void testE1() {
        T_return_object_8 t = new T_return_object_8();
        try {
            assertTrue(t.run());
            fail("expected IllegalMonitorStateException");
        } catch (IllegalMonitorStateException imse) {
            // expected
        }
    }


    /**
     * @constraint B11 
     * @title method's return type - void
     */
    public void testVFE1() {
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B11 
     * @title method's return type - float
     */
    public void testVFE2() {
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint B11 
     * @title method's return type - long
     */
    public void testVFE3() {
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_16");
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
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

  
    /**
     * @constraint B1 
     * @title types of argument - int
     */
    public void testVFE6() {
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of argument - long
     */
    public void testVFE7() {
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B11 
     * @title assignment incompatible references
     */
    public void testVFE8() {
        //@uses dot.junit.opcodes.return_object.d.T_return_object_14
        //@uses dot.junit.opcodes.return_object.d.TChild
        //@uses dot.junit.opcodes.return_object.d.TSuper
        //@uses dot.junit.opcodes.return_object.d.TInterface
        try {
            Class.forName("dxc.junit.opcodes.return_object.jm.T_return_object_14");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B11 
     * @title assignment incompatible references
     */
    public void testVFE9() {
        //@uses dot.junit.opcodes.return_object.d.T_return_object_15
        //@uses dot.junit.opcodes.return_object.Runner
        //@uses dot.junit.opcodes.return_object.RunnerGenerator
        //@uses dot.junit.opcodes.return_object.d.TSuper2
        try {
            RunnerGenerator rg = (RunnerGenerator) Class.forName(
                    "dot.junit.opcodes.return_object.d.T_return_object_15").newInstance();
            Runner r = rg.run();
            assertFalse(r instanceof Runner);
            assertFalse(Runner.class.isAssignableFrom(r.getClass()));
            // only upon invocation of a concrete method,
            // a java.lang.IncompatibleClassChangeError is thrown
            r.doit();
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }


}
