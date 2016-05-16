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

import android.renderscript.ProgramStore;
import android.renderscript.ProgramStore.DepthFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.renderscript.ProgramStore.BlendDstFunc;

public class ProgramStoreTest extends RSBaseGraphics {

    void varyBuilderColorAndDither(ProgramStore.Builder pb) {
        for (int r = 0; r <= 1; r++) {
            boolean isR = (r == 1);
            for (int g = 0; g <= 1; g++) {
                boolean isG = (g == 1);
                for (int b = 0; b <= 1; b++) {
                    boolean isB = (b == 1);
                    for (int a = 0; a <= 1; a++) {
                        boolean isA = (a == 1);
                        for (int dither = 0; dither <= 1; dither++) {
                            boolean isDither = (dither == 1);
                            pb.setDitherEnabled(isDither);
                            pb.setColorMaskEnabled(isR, isG, isB, isA);
                            ProgramStore ps = pb.create();
                            assertTrue(ps != null);
                            mRS.bindProgramStore(ps);
                        }
                    }
                }
            }
        }
    }

    public void testProgramStoreBuilder() {
        for (int depth = 0; depth <= 1; depth++) {
            boolean depthMask = (depth == 1);
            for (DepthFunc df : DepthFunc.values()) {
                for (BlendSrcFunc bsf : BlendSrcFunc.values()) {
                    for (BlendDstFunc bdf : BlendDstFunc.values()) {
                        ProgramStore.Builder b = new ProgramStore.Builder(mRS);
                        b.setDepthFunc(df);
                        b.setDepthMaskEnabled(depthMask);
                        b.setBlendFunc(bsf, bdf);
                        varyBuilderColorAndDither(b);
                    }
                }
            }
        }
    }

    public void testPrebuiltProgramStore() {
        assertTrue(ProgramStore.BLEND_ALPHA_DEPTH_NONE(mRS) != null);
        assertTrue(ProgramStore.BLEND_ALPHA_DEPTH_TEST(mRS) != null);
        assertTrue(ProgramStore.BLEND_NONE_DEPTH_NONE(mRS) != null);
        assertTrue(ProgramStore.BLEND_NONE_DEPTH_TEST(mRS) != null);
    }

    public void testProgramStoreBlendDstFunc() {
        assertEquals(BlendDstFunc.ZERO,
                     BlendDstFunc.valueOf("ZERO"));
        assertEquals(BlendDstFunc.ONE,
                     BlendDstFunc.valueOf("ONE"));
        assertEquals(BlendDstFunc.SRC_COLOR,
                     BlendDstFunc.valueOf("SRC_COLOR"));
        assertEquals(BlendDstFunc.ONE_MINUS_SRC_COLOR,
                     BlendDstFunc.valueOf("ONE_MINUS_SRC_COLOR"));
        assertEquals(BlendDstFunc.SRC_ALPHA,
                     BlendDstFunc.valueOf("SRC_ALPHA"));
        assertEquals(BlendDstFunc.ONE_MINUS_SRC_ALPHA,
                     BlendDstFunc.valueOf("ONE_MINUS_SRC_ALPHA"));
        assertEquals(BlendDstFunc.DST_ALPHA,
                     BlendDstFunc.valueOf("DST_ALPHA"));
        assertEquals(BlendDstFunc.ONE_MINUS_DST_ALPHA,
                     BlendDstFunc.valueOf("ONE_MINUS_DST_ALPHA"));
        // Make sure no new enums are added
        assertEquals(8, BlendDstFunc.values().length);
    }

    public void testProgramStoreBlendSrcFunc() {
        assertEquals(BlendSrcFunc.ZERO,
                     BlendSrcFunc.valueOf("ZERO"));
        assertEquals(BlendSrcFunc.ONE,
                     BlendSrcFunc.valueOf("ONE"));
        assertEquals(BlendSrcFunc.DST_COLOR,
                     BlendSrcFunc.valueOf("DST_COLOR"));
        assertEquals(BlendSrcFunc.ONE_MINUS_DST_COLOR,
                     BlendSrcFunc.valueOf("ONE_MINUS_DST_COLOR"));
        assertEquals(BlendSrcFunc.SRC_ALPHA,
                     BlendSrcFunc.valueOf("SRC_ALPHA"));
        assertEquals(BlendSrcFunc.ONE_MINUS_SRC_ALPHA,
                     BlendSrcFunc.valueOf("ONE_MINUS_SRC_ALPHA"));
        assertEquals(BlendSrcFunc.DST_ALPHA,
                     BlendSrcFunc.valueOf("DST_ALPHA"));
        assertEquals(BlendSrcFunc.ONE_MINUS_DST_ALPHA,
                     BlendSrcFunc.valueOf("ONE_MINUS_DST_ALPHA"));
        assertEquals(BlendSrcFunc.SRC_ALPHA_SATURATE,
                     BlendSrcFunc.valueOf("SRC_ALPHA_SATURATE"));
        // Make sure no new enums are added
        assertEquals(9, BlendSrcFunc.values().length);
    }
    public void testProgramStoreDepthFunc() {
        assertEquals(DepthFunc.ALWAYS,
                     DepthFunc.valueOf("ALWAYS"));
        assertEquals(DepthFunc.LESS,
                     DepthFunc.valueOf("LESS"));
        assertEquals(DepthFunc.LESS_OR_EQUAL,
                     DepthFunc.valueOf("LESS_OR_EQUAL"));
        assertEquals(DepthFunc.GREATER,
                     DepthFunc.valueOf("GREATER"));
        assertEquals(DepthFunc.GREATER_OR_EQUAL,
                     DepthFunc.valueOf("GREATER_OR_EQUAL"));
        assertEquals(DepthFunc.EQUAL,
                     DepthFunc.valueOf("EQUAL"));
        assertEquals(DepthFunc.NOT_EQUAL,
                     DepthFunc.valueOf("NOT_EQUAL"));
        // Make sure no new enums are added
        assertEquals(7, DepthFunc.values().length);
    }
}


