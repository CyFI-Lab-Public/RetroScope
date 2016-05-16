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

package dot.junit.opcodes.opc_throw;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_1;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_12;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_2;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_4;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_5;
import dot.junit.opcodes.opc_throw.d.T_opc_throw_8;

public class Test_opc_throw extends DxTestCase {
    /**
     * @title check throw functionality. This test also tests constraint C17 allowing to have
     * throw as a last opcode in the method.
     */
    public void testN1() {
        T_opc_throw_1 t = new T_opc_throw_1();
        try {
            t.run();
            fail("must throw a RuntimeException");
        } catch (RuntimeException re) {
            // expected
        }
    }

    /**
     * @title Throwing of the objectref on the class Throwable
     */
    public void testN2() {
        T_opc_throw_2 t = new T_opc_throw_2();
        try {
            t.run();
            fail("must throw a Throwable");
        } catch (Throwable e) {
            // expected
        }
    }

    /**
     * @title Throwing of the objectref on the subclass of Throwable
     */
    public void testN3() {
        T_opc_throw_8 t = new T_opc_throw_8();
        try {
            t.run();
            fail("must throw a Error");
        } catch (Error e) {
            // expected
        }
    }

    /**
     * @title Nearest matching catch must be executed in case of exception
     */
    public void testN4() {
        T_opc_throw_12 t = new T_opc_throw_12();
        assertTrue(t.run());
    }

    /**
     * @title NullPointerException If objectref is null, opc_throw throws
     * a NullPointerException instead of objectref
     */
    public void testE1() {
        T_opc_throw_4 t = new T_opc_throw_4();
        try {
            t.run();
            fail("expected NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
    }

    /**
     * @title expected IllegalMonitorStateException
     */
    public void testE2() {
        T_opc_throw_5 t = new T_opc_throw_5();
        try {
            t.run();
            fail("expected IllegalMonitorStateException");
        } catch (IllegalMonitorStateException imse) {
            // expected
        }
    }

    /**
     * @constraint A23 
     * @title  (number of registers)
     */
    public void testVFE2() {
        try {
            Class.forName("dxc.junit.opcodes.opc_throw.jm.T_opc_throw_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * 
     * @constraint B1 
     * @title type of argument - float
     */
    public void testVFE3() {
        try {
            Class.forName("dxc.junit.opcodes.opc_throw.jm.T_opc_throw_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * 
     * @constraint B1 
     * @title type of argument - long
     */
    public void testVFE4() {
        try {
            Class.forName("dxc.junit.opcodes.opc_throw.jm.T_opc_throw_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B16 
     * @title operand must be must be assignment-compatible 
     * with java.lang.Throwable

     */
    public void testVFE5() {
        try {
            Class.forName("dxc.junit.opcodes.opc_throw.jm.T_opc_throw_10");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
