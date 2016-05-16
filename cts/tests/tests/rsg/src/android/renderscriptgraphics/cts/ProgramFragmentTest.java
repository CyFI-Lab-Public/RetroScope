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

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Program;
import android.renderscript.ProgramFragment;
import android.renderscript.Sampler;
import android.renderscript.Type;

public class ProgramFragmentTest extends RSBaseGraphics {

    ScriptC_graphics_runner mScript;

    Allocation mConstMatrix;
    Allocation mConstComplex;
    Allocation mConstExtra;

    Allocation mTex2D;
    Allocation mTexCube;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Type.Builder typeB = new Type.Builder(mRS, Element.RGB_888(mRS));
        typeB.setX(8).setY(8);
        mTex2D = Allocation.createTyped(mRS, typeB.create(),
                                        Allocation.USAGE_SCRIPT |
                                        Allocation.USAGE_GRAPHICS_TEXTURE);
        typeB.setFaces(true);
        mTexCube = Allocation.createTyped(mRS, typeB.create(),
                                          Allocation.USAGE_SCRIPT |
                                          Allocation.USAGE_GRAPHICS_TEXTURE);

        ScriptField_ConstMatrix c1 = new ScriptField_ConstMatrix(mRS, 1,
                                                                 Allocation.USAGE_SCRIPT |
                                                                 Allocation.USAGE_GRAPHICS_CONSTANTS);
        c1.set(new ScriptField_ConstMatrix.Item(), 0, true);
        mConstMatrix = c1.getAllocation();

        ScriptField_ConstComplex c2 = new ScriptField_ConstComplex(mRS, 1,
                                                                   Allocation.USAGE_SCRIPT |
                                                                   Allocation.USAGE_GRAPHICS_CONSTANTS);
        c2.set(new ScriptField_ConstComplex.Item(), 0, true);
        mConstComplex = c2.getAllocation();

        ScriptField_ConstExtra c3 = new ScriptField_ConstExtra(mRS, 1,
                                                               Allocation.USAGE_SCRIPT |
                                                               Allocation.USAGE_GRAPHICS_CONSTANTS);
        c3.set(new ScriptField_ConstExtra.Item(), 0, true);
        mConstExtra = c3.getAllocation();

        mScript = new ScriptC_graphics_runner(mRS, mRes, R.raw.graphics_runner);
        mRS.bindRootScript(mScript);
    }

    @Override
    protected void tearDown() throws Exception {
        mRS.bindRootScript(null);
        super.tearDown();
    }

    ProgramFragment buildShader(Allocation[] textures, Allocation[] constInput, String shader) {
        ProgramFragment.Builder pfb = new ProgramFragment.Builder(mRS);
        Program.BaseProgramBuilder bpb = pfb;
        if (textures != null) {
            for (int i = 0; i < textures.length; i++) {
                Program.TextureType tType = Program.TextureType.TEXTURE_2D;
                if (textures[i].getType().hasFaces()) {
                    tType = Program.TextureType.TEXTURE_CUBE;
                }
                // Add textures through the base program builder
                bpb.addTexture(tType);
                bpb.getCurrentTextureIndex();
            }
        }

        if (constInput != null) {
            for (int i = 0; i < constInput.length; i++) {
                bpb.addConstant(constInput[i].getType());
                bpb.getCurrentConstantIndex();
            }
        }

        bpb.setShader(shader);
        ProgramFragment pf = pfb.create();
        if (constInput != null) {
            for (int i = 0; i < constInput.length; i++) {
                pf.bindConstants(constInput[i], i);
                // Test the base class path too
                Program p = pf;
                p.bindConstants(constInput[i], i);
            }
        }
        if (textures != null) {
            for (int i = 0; i < textures.length; i++) {
                pf.bindTexture(textures[i], i);
                pf.bindSampler(Sampler.CLAMP_NEAREST(mRS), i);
                // Test the base class path too
                Program p = pf;
                p.bindTexture(textures[i], i);
                p.bindSampler(Sampler.CLAMP_NEAREST(mRS), i);
            }
        }
        return pf;
    }

    void testProgramFragmentBuilderHelper(boolean testBind) {
        String simpleFrag = "void main() {\n"+
                            "  vec4 col = vec4(0.1, 0.2, 0.3, 0.4);"+
                            "  gl_FragColor = col;\n"+
                            "}";

        String simpleUni = "void main() {\n"+
                           "  vec4 col = vec4(0.1, 0.2, 0.3, 0.4);\n"+
                           "  col = UNI_MATRIX * col;\n"+
                           "  gl_FragColor = col;\n"+
                           "}";

        String simpleUniTex = "void main() {\n"+
                              "  vec4 col = vec4(0.1, 0.2, 0.3, 0.4);"+
                              "  col += texture2D(UNI_Tex0, vec2(0.1, 0.1));\n"+
                              "  col += textureCube(UNI_Tex1, vec3(0.1, 0.2, 0.3));\n"+
                              "  col = UNI_MATRIX * col;\n"+
                              "  gl_FragColor = col;\n"+
                              "}";

        String multiUni = "void main() {\n"+
                          "  vec4 col = vec4(0.1, 0.2, 0.3, 0.4);"+
                          "  col = UNI_MATRIX * col;\n"+
                          "  col = UNI_EXTRA * col;\n"+
                          "  col += UNI_extra4;\n"+
                          "  col.xyz += UNI_extra3;\n "+
                          "  col.xy += UNI_extra2;\n"+
                          "  col.x += UNI_extra1;\n"+
                          "  gl_FragColor = col;\n"+
                          "}";

        // Create a series of shaders that do nothing useful
        // but exercise creation pipeline
        ProgramFragment pf = buildShader(null, null, simpleFrag);
        if (testBind) {
            mScript.invoke_testProgramFragment(pf);
        }

        Allocation[] constInput = new Allocation[1];
        constInput[0] = mConstMatrix;
        pf = buildShader(null, constInput, simpleUni);
        if (testBind) {
            mScript.invoke_testProgramFragment(pf);
            mRS.bindProgramFragment(pf);
        }

        constInput[0] = mConstComplex;
        pf = buildShader(null, constInput, multiUni);
        if (testBind) {
            mScript.invoke_testProgramFragment(pf);
            mRS.bindProgramFragment(pf);
        }

        Allocation[] textures = new Allocation[2];
        textures[0] = mTex2D;
        textures[1] = mTexCube;
        pf = buildShader(textures, constInput, simpleUniTex);
        if (testBind) {
            mScript.invoke_testProgramFragment(pf);
            mRS.bindProgramFragment(pf);
        }

        constInput = new Allocation[2];
        constInput[0] = mConstMatrix;
        constInput[1] = mConstExtra;
        pf = buildShader(null, constInput, multiUni);
        if (testBind) {
            mScript.invoke_testProgramFragment(pf);
            mRS.bindProgramFragment(pf);
        }
    }

    public void testProgramFragmentBuilder() {
        testProgramFragmentBuilderHelper(false);
    }

    public void testProgramFragmentCreation() {
        testProgramFragmentBuilderHelper(true);
    }

    public void testProgramTextureType() {
        assertEquals(Program.TextureType.TEXTURE_2D,
                     Program.TextureType.valueOf("TEXTURE_2D"));
        assertEquals(Program.TextureType.TEXTURE_CUBE,
                     Program.TextureType.valueOf("TEXTURE_CUBE"));
        // Make sure no new enums are added
        assertEquals(2, Program.TextureType.values().length);

        ProgramFragment.Builder pfb = new ProgramFragment.Builder(mRS);
        for (Program.TextureType tt : Program.TextureType.values()) {
            pfb.addTexture(tt);
        }
    }
}


