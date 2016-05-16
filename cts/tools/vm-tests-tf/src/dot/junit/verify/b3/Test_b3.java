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

package dot.junit.verify.b3;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;

public class Test_b3 extends DxTestCase {

    /**
     * @constraint B3
     * @title register (or pair) has to be assigned first before it can be read. 
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.verify.b3.d.T_b3_1");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint B3
     * @title register (or pair) has to be assigned first before it can be read. 
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.verify.b3.d.T_b3_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
