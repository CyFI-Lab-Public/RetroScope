/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.renderscriptgraphics.cts;

import android.renderscript.BaseObj;
import android.renderscript.Font;
import android.renderscript.Font.Style;
import android.renderscript.RSIllegalArgumentException;

public class BaseObjTest extends RSBaseGraphics {

    public void testBaseObj() {
        Style S = Font.Style.NORMAL;
        Font F = Font.create(mRS, mRes, "sans-serif", S, 8);
        assertTrue(F != null);
        BaseObj B = F;
        B.setName("sans-serif");
        try {
            B.setName("sans-serif");
            fail("set name twice for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }
        B.destroy();

        F = Font.create(mRS, mRes, "serif", S, 8);
        assertTrue(F != null);
        B = F;
        try {
            B.setName("");
            fail("set empty name for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }
        B.setName("serif");
        B.destroy();

        F = Font.create(mRS, mRes, "mono", S, 8);
        assertTrue(F != null);
        B = F;
        try {
            B.setName(null);
            fail("set name as null string reference for a BaseObj");
        } catch (RSIllegalArgumentException e) {
        }
        B.setName("mono");
        B.destroy();
    }
}

