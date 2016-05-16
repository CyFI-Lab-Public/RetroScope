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

package dot.junit.opcodes.not_long;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.not_long.d.T_not_long_1;
import dot.junit.opcodes.not_long.d.T_not_long_2;

public class Test_not_long extends DxTestCase {
    
    /**
     * @title Argument = 500000l
     */
    public void testN1() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(-500001l, t.run(500000l));
    }
    
    /**
     * @title Argument = -500000l
     */
    public void testN2() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(499999l, t.run(-500000l));
    }
    
    /**
     * @title Argument = 0xcafe; 0x12c
     */
    public void testN3() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(-0xcaff, t.run(0xcafe));
        assertEquals(-0x12d, t.run(0x12c));
    }

    /**
     * @title Argument = Long.MAX_VALUE
     */
    public void testB1() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(Long.MIN_VALUE, t.run(Long.MAX_VALUE));
    }
    
    /**
     * @title Argument = Long.MIN_VALUE
     */
    public void testB2() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(Long.MAX_VALUE, t.run(Long.MIN_VALUE));
    }
    
    /**
     * @title Argument = 1l
     */
    public void testB3() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(-2l, t.run(1l));
    }
    
    /**
     * @title Argument = 0l
     */
    public void testB4() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(-1l, t.run(0l));
    }
    
    /** 
     * @title Argument = -1l
     */
    public void testB5() {
        T_not_long_1 t = new T_not_long_1();
        assertEquals(0l, t.run(-1l));
    }
    
    /**
     * @constraint A23 
     * @title number of registers
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.not_long.d.T_not_long_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    

    /**
     * @constraint B1 
     * @title types of arguments - int
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.not_long.d.T_not_long_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - float
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.not_long.d.T_not_long_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.not_long.d.T_not_long_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1
     * @title Types of arguments - long, double. The verifier checks that longs
     * and doubles are not used interchangeably.
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.opcodes.not_long.d.T_not_long_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
