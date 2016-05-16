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
import android.renderscript.ProgramVertex;

public class ProgramVertexTest extends RSBaseGraphics {

    ScriptC_graphics_runner mScript;
    Element mAttrPosElem;
    Element mAttrNormTexElem;
    Element mAttrPosNormTexElem;
    Element mAttrExtra;

    Allocation mConstMatrix;
    Allocation mConstComplex;
    Allocation mConstExtra;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Build elements for shader inputs
        Element.Builder eb = new Element.Builder(mRS);
        eb.add(Element.F32_4(mRS), "position");
        mAttrPosElem = eb.create();

        eb = new Element.Builder(mRS);
        eb.add(Element.F32_3(mRS), "normal");
        eb.add(Element.F32_2(mRS), "texture0");
        mAttrNormTexElem = eb.create();

        eb = new Element.Builder(mRS);
        eb.add(Element.F32_4(mRS), "position");
        eb.add(Element.F32_3(mRS), "normal");
        eb.add(Element.F32_2(mRS), "texture0");
        mAttrPosNormTexElem = eb.create();

        eb.add(Element.F32(mRS), "extra1");
        eb.add(Element.F32_2(mRS), "extra2");
        eb.add(Element.F32_3(mRS), "extra3");
        eb.add(Element.F32_4(mRS), "extra4");
        mAttrExtra = eb.create();

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

    ProgramVertex buildShader(Element[] input, Allocation[] constInput, String shader) {
        ProgramVertex.Builder pvb = new ProgramVertex.Builder(mRS);
        Program.BaseProgramBuilder bpb = pvb;
        if (input != null) {
            for (int i = 0; i < input.length; i++) {
                pvb.addInput(input[i]);
            }
        }
        if (constInput != null) {
            for (int i = 0; i < constInput.length; i++) {
                // Add constants through the base builder class to
                // tick cts test coverage (doesn't register through subclass)
                bpb.addConstant(constInput[i].getType());
                bpb.getCurrentConstantIndex();
            }
        }

        bpb.setShader(shader);
        ProgramVertex pv = pvb.create();
        if (constInput != null) {
            for (int i = 0; i < constInput.length; i++) {
                pv.bindConstants(constInput[i], i);
                // Go through the base class code as well
                Program p = pv;
                p.bindConstants(constInput[i], i);
            }
        }
        return pv;
    }

    void testProgramVertexBuilderHelper(boolean testBind) {
        String simpleAttr = "void main() {\n"+
                            "  gl_Position = ATTRIB_position;\n"+
                            "}";

        String multiAttr = "void main() {\n"+
                           "  vec4 temp = ATTRIB_position;\n"+
                           "  temp.xyz += ATTRIB_normal;\n"+
                           "  temp.xy += ATTRIB_texture0;\n"+
                           "  gl_Position = temp;\n"+
                           "}";

        String multiAttr2 = "void main() {\n"+
                            "  vec4 temp = ATTRIB_position;\n"+
                            "  temp.xyz += ATTRIB_normal;\n"+
                            "  temp.xy += ATTRIB_texture0;\n"+
                            "  temp += ATTRIB_extra4;\n"+
                            "  temp.xyz += ATTRIB_extra3;\n "+
                            "  temp.xy += ATTRIB_extra2;\n"+
                            "  temp.x += ATTRIB_extra1;\n"+
                            "  gl_Position = temp;\n"+
                            "}";

        String simpleAttrSimpleUni = "void main() {\n"+
                                     "  gl_Position = UNI_MATRIX * ATTRIB_position;\n"+
                                     "}";

        String multiAttrMultiUni = "void main() {\n"+
                                   "  vec4 temp = UNI_MATRIX * ATTRIB_position;\n"+
                                   "  temp = UNI_EXTRA * temp;\n"+
                                   "  temp.xyz += ATTRIB_normal;\n"+
                                   "  temp.xy += ATTRIB_texture0;\n"+
                                   "  temp += UNI_extra4;\n"+
                                   "  temp.xyz += UNI_extra3;\n "+
                                   "  temp.xy += UNI_extra2;\n"+
                                   "  temp.x += UNI_extra1;\n"+
                                   "  gl_Position = temp;\n"+
                                   "}";

        // Create a series of shaders that do nothing useful
        // but exercise creation pipeline
        Element[] inputs = new Element[1];
        inputs[0] = mAttrPosElem;
        ProgramVertex pv = buildShader(inputs, null, simpleAttr);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        inputs[0] = mAttrPosNormTexElem;
        pv = buildShader(inputs, null, multiAttr);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        inputs[0] = mAttrExtra;
        pv = buildShader(inputs, null, multiAttr2);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        // Now with constant inputs
        Allocation[] constInput = new Allocation[1];
        inputs[0] = mAttrPosElem;
        constInput[0] = mConstMatrix;
        pv = buildShader(inputs, constInput, simpleAttrSimpleUni);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        inputs[0] = mAttrPosNormTexElem;
        constInput[0] = mConstComplex;
        pv = buildShader(inputs, constInput, multiAttrMultiUni);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        // Now with multiple input and const structs
        constInput = new Allocation[2];
        constInput[0] = mConstMatrix;
        constInput[1] = mConstExtra;
        inputs[0] = mAttrPosNormTexElem;
        pv = buildShader(inputs, constInput, multiAttrMultiUni);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        inputs = new Element[2];
        inputs[0] = mAttrPosElem;
        inputs[1] = mAttrNormTexElem;
        pv = buildShader(inputs, null, multiAttr);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }

        constInput[0] = mConstMatrix;
        constInput[1] = mConstExtra;
        inputs[0] = mAttrPosElem;
        inputs[1] = mAttrNormTexElem;
        pv = buildShader(inputs, constInput, multiAttrMultiUni);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
            mRS.bindProgramVertex(pv);
        }
    }

    public void testProgramVertexBuilder() {
        testProgramVertexBuilderHelper(false);
    }

    public void testProgramVertexCreation() {
        testProgramVertexBuilderHelper(true);
    }
}


