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

package dot.junit.format.f1;

import dot.junit.DxTestCase;
import dot.junit.DxUtil;

public class Test_f1 extends DxTestCase {

    /**
     * @constraint n/a
     * @title size of dex file shall be greater than size of header 
     */
    public void testVFE1() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_1");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }

    /**
     * @constraint n/a 
     * @title check that .dex with wrong magic is rejected 
     */
    public void testVFE2() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_2");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title check that .dex with wrong version is rejected 
     */
    public void testVFE3() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_3");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title check that .dex with wrong endian_tag is rejected 
     */
    public void testVFE4() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_4");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title check that .dex with wrong header size is rejected 
     */
    public void testVFE5() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_5");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title  file length must be equal to length in header
     */
    public void testVFE6() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_6");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title  header->map_off != 0
     */
    public void testVFE7() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_7");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title number of classes in dex shall be > 0
     */
    public void testVFE8() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_8");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title check that .dex with wrong checksum is rejected 
     */
    public void testVFE9() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_9");
            fail("expected a verification exception but this test may fail if this check is not enforced");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title check that .dex with wrong signature is rejected 
     */
//    public void testVFE10() {
//        try {
//            Class.forName("dot.junit.format.f1.d.T_f1_10");
//            fail("expected a verification exception but this test may fail if this check is not enforced");
//        } catch (Throwable t) {
//            DxUtil.checkVerifyException(t);
//        }
//    }
    
    /**
     * @constraint n/a
     * @title header and map section mismatch 
     */
    public void testVFE11() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_11");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
    
    /**
     * @constraint n/a
     * @title overlapping sections 
     */
    public void testVFE12() {
        try {
            Class.forName("dot.junit.format.f1.d.T_f1_12");
            fail("expected a verification exception");
        } catch (Throwable t) {
            DxUtil.checkVerifyException(t);
        }
    }
}
