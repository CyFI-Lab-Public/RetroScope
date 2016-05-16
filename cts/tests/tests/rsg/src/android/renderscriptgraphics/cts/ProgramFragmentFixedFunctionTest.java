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

import com.android.cts.stub.R;
import android.renderscript.Element;
import android.renderscript.Type;
import android.renderscript.Allocation;
import android.renderscript.Sampler;
import android.renderscript.ProgramFragment;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramFragmentFixedFunction.Builder;

public class ProgramFragmentFixedFunctionTest extends RSBaseGraphics {

    ScriptC_graphics_runner mScript;

    Allocation mTex2D;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Type.Builder typeB = new Type.Builder(mRS, Element.RGB_888(mRS));
        typeB.setX(8).setY(8);
        mTex2D = Allocation.createTyped(mRS, typeB.create(),
                                        Allocation.USAGE_SCRIPT |
                                        Allocation.USAGE_GRAPHICS_TEXTURE);

        mScript = new ScriptC_graphics_runner(mRS, mRes, R.raw.graphics_runner);
        mRS.bindRootScript(mScript);
    }

    @Override
    protected void tearDown() throws Exception {
        mRS.bindRootScript(null);
        super.tearDown();
    }

    void testProgramFragmentFixedFunctionBuilder(boolean testBind) {
        ProgramFragmentFixedFunction.Builder b;
        for (int tCount = 0; tCount <= Builder.MAX_TEXTURE; tCount ++) {
            for (int varC = 0; varC <= 1; varC++) {
                for (int pSprite = 0; pSprite <= 1; pSprite++) {
                    for (Builder.EnvMode env : Builder.EnvMode.values()) {
                        for (Builder.Format format : Builder.Format.values()) {
                            b = new ProgramFragmentFixedFunction.Builder(mRS);
                            b.setVaryingColor(varC == 1);
                            b.setPointSpriteTexCoordinateReplacement(pSprite == 1);
                            for (int t = 0; t < tCount; t++) {
                                b.setTexture(env, format, t);
                            }

                            ProgramFragment pf = b.create();
                            assertTrue(pf != null);
                            for (int t = 0; t < tCount; t++) {
                                pf.bindTexture(mTex2D, t);
                                pf.bindSampler(Sampler.CLAMP_NEAREST(mRS), t);
                            }
                            if (testBind) {
                                mScript.invoke_testProgramFragment(pf);
                            }
                        }
                    }
                }
            }
        }
    }

    public void testProgramFragmentFixedFunctionBuilder() {
        testProgramFragmentFixedFunctionBuilder(false);
        testProgramFragmentFixedFunctionBuilder(true);
    }

    public void testBuilderEnvMode() {
        assertEquals(Builder.EnvMode.DECAL, Builder.EnvMode.valueOf("DECAL"));
        assertEquals(Builder.EnvMode.MODULATE, Builder.EnvMode.valueOf("MODULATE"));
        assertEquals(Builder.EnvMode.REPLACE, Builder.EnvMode.valueOf("REPLACE"));

        // Make sure no new enums are added
        assertEquals(3, Builder.EnvMode.values().length);
    }

    public void testBuilderFormat() {
        assertEquals(Builder.Format.ALPHA, Builder.Format.valueOf("ALPHA"));
        assertEquals(Builder.Format.LUMINANCE_ALPHA, Builder.Format.valueOf("LUMINANCE_ALPHA"));
        assertEquals(Builder.Format.RGB, Builder.Format.valueOf("RGB"));
        assertEquals(Builder.Format.RGBA, Builder.Format.valueOf("RGBA"));

        // Make sure no new enums are added
        assertEquals(4, Builder.Format.values().length);
    }

}


