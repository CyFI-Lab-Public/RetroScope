/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.renderscript.cts;

import android.renderscript.Allocation;
import android.renderscript.BaseObj;
import android.renderscript.Element;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.Type;

public class BaseObjTest extends RSBaseCompute {

    public void testBaseObj() {
        Element E = Element.I32(mRS);
        Type.Builder TB = new Type.Builder(mRS, E);
        Type T = TB.setX(1).create();
        assertTrue(T != null);
        BaseObj B = T;
        B.setName("int32_t");
        try {
            B.setName("int32_t");
            fail("set name twice for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }
        T.destroy();

        T = TB.setX(2).create();
        assertTrue(T != null);
        B = T;
        try {
            B.setName("");
            fail("set empty name for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }

        try {
            B.setName(null);
            fail("set name as null string reference for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }
        B.setName("int32_t");

        assertTrue(B.getName().compareTo("int32_t") == 0);
        B.destroy();
    }
}

