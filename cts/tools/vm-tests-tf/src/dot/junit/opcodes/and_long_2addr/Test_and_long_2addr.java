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

package dot.junit.opcodes.and_long_2addr;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_1;
import dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_6;

public class Test_and_long_2addr extends DxTestCase {
    /**
     * @title Arguments = 0xfffffff8aal, 0xfffffff1aal
     */
    public void testN1() {
        T_and_long_2addr_1 t = new T_and_long_2addr_1();
        assertEquals(0xfffffff0aal, t.run(0xfffffff8aal, 0xfffffff1aal));
    }

    /**
     * @title Arguments = 987654321, 123456789
     */
    public void testN2() {
        T_and_long_2addr_1 t = new T_and_long_2addr_1();
        assertEquals(39471121, t.run(987654321, 123456789));
    }

    /**
     * @title Arguments = 0xABCDEF & -1
     */
    public void testN3() {
        T_and_long_2addr_1 t = new T_and_long_2addr_1();
        assertEquals(0xABCDEF, t.run(0xABCDEF, -1));
    }

    /**
     * @title Arguments = 0 & -1
     */
    public void testB1() {
        T_and_long_2addr_1 t = new T_and_long_2addr_1();
        assertEquals(0, t.run(0, -1));
    }

    /**
     * @title Arguments = Long.MAX_VALUE & Long.MIN_VALUE
     */
    public void testB2() {
        T_and_long_2addr_1 t = new T_and_long_2addr_1();
        assertEquals(0, t.run(Long.MAX_VALUE, Long.MIN_VALUE));
    }


    

    /**
     * @constraint B1 
     * @title types of arguments - float & long
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - int & long
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B1 
     * @title types of arguments - reference & long
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint A24 
     * @title number of registers
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_5");
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
            Class.forName("dot.junit.opcodes.and_long_2addr.d.T_and_long_2addr_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
