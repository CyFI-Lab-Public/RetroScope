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

package dot.junit.opcodes.nop;

import dot.junit.DxTestCase;
import dot.junit.opcodes.nop.d.T_nop_1;

public class Test_nop extends DxTestCase {
    /**
     * @title tests nop
     */
    public void testN1() {
        T_nop_1 t = new T_nop_1();
        // how do we test nop - e.g. initialize some registers and
        // test if nothing has changed
        assertTrue(t.run());
    }
}
