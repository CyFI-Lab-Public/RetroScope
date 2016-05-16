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

package dot.junit.verify.b17;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;

public class Test_b17 extends DxTestCase {

    /**
     * Having throw, return and backward goto as a last opcode in the method is tested
     * as part of corresponding opcodes tests.
     */
    
    /**
     * @constraint B17
     * 
     * @title attempt to leave insns array without return or throw.
     * Since this constraint is trivial to be checked by the verifier,
     * it is sufficient to have a trivial test.
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.verify.b17.d.T_b17_1");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

}
