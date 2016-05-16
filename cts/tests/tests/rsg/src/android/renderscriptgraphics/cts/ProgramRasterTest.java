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

import android.renderscript.ProgramRaster;
import android.renderscript.ProgramRaster.Builder;
import android.renderscript.ProgramRaster.CullMode;

public class ProgramRasterTest extends RSBaseGraphics {

    public void testProgramRasterBuilder() {
        ProgramRaster.Builder b = new ProgramRaster.Builder(mRS);
        for (int p = 0; p <= 1; p++) {
            boolean pSprite = (p == 1);
            b.setPointSpriteEnabled(pSprite);
            for (CullMode cull : CullMode.values()) {
                b.setCullMode(cull);
                ProgramRaster pr = b.create();
                assertTrue(pr != null);
                mRS.bindProgramRaster(pr);
            }
        }
    }

    public void testPrebuiltProgramRaster() {
        assertTrue(ProgramRaster.CULL_BACK(mRS) != null);
        assertTrue(ProgramRaster.CULL_FRONT(mRS) != null);
        assertTrue(ProgramRaster.CULL_NONE(mRS) != null);
    }

    public void testProgramRasterCullMode() {
        assertEquals(CullMode.BACK, CullMode.valueOf("BACK"));
        assertEquals(CullMode.FRONT, CullMode.valueOf("FRONT"));
        assertEquals(CullMode.NONE, CullMode.valueOf("NONE"));
        // Make sure no new enums are added
        assertEquals(3, CullMode.values().length);
    }
}
