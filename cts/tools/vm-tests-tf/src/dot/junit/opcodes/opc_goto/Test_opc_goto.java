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

package dot.junit.opcodes.opc_goto;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;
import dot.junit.opcodes.opc_goto.d.T_opc_goto_1;

public class Test_opc_goto extends DxTestCase {
   /**
    * @title forward and backward goto. This test also tests constraint C17 allowing to have
     * backward goto as a last opcode in the method.
    */
   public void testN1() {
       T_opc_goto_1 t = new T_opc_goto_1();
       assertEquals(0, t.run(20));
   }

   /**
    * @constraint A6 
    * @title branch target is inside instruction
    */
   public void testVFE1() {
       try {
           Class.forName("dot.junit.opcodes.opc_goto.d.T_opc_goto_2");
           fail("expected a verification exception");
       } catch (Throwable t) {
           DxUtil.checkVerifyException(t);
       }
   }

   /**
    * @constraint A6 
    * @title branch target shall be inside the method
    */
   public void testVFE2() {
       try {
           Class.forName("dot.junit.opcodes.opc_goto.d.T_opc_goto_3");
           fail("expected a verification exception");
       } catch (Throwable t) {
           DxUtil.checkVerifyException(t);
       }
   }

   /**
    * @constraint n/a 
    * @title zero offset
    */
   public void testVFE3() {
       try {
           Class.forName("dot.junit.opcodes.opc_goto.d.T_opc_goto_4");
           fail("expected a verification exception");
       } catch (Throwable t) {
           DxUtil.checkVerifyException(t);
       }
   }
   
}
