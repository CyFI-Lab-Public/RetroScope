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

import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.ProgramVertexFixedFunction.Builder;
import android.renderscript.ScriptC;
import android.renderscript.Matrix4f;

public class ProgramVertexFixedFunctionTest extends RSBaseGraphics {

    ScriptC_graphics_runner mScript;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mScript = new ScriptC_graphics_runner(mRS, mRes, R.raw.graphics_runner);
        mRS.bindRootScript(mScript);
    }

    @Override
    protected void tearDown() throws Exception {
        mRS.bindRootScript(null);
        super.tearDown();
    }

    public void testConstants() {
        ProgramVertexFixedFunction.Constants pva;
        for (int isM = 0; isM <= 1; isM++) {
            for (int isP = 0; isP <= 1; isP++) {
                for (int isT = 0; isT <= 1; isT++) {
                    pva = new ProgramVertexFixedFunction.Constants(mRS);
                    if (isM == 1) {
                        pva.setModelview(new Matrix4f());
                    }
                    if (isP == 1) {
                        pva.setProjection(new Matrix4f());
                    }
                    if (isT == 1) {
                        pva.setTexture(new Matrix4f());
                    }
                    pva.destroy();
                }
            }
        }
    }

    void testProgramVertexFixedFunctionBuilder(boolean testBind) {
        ProgramVertexFixedFunction.Constants pva;
        pva = new ProgramVertexFixedFunction.Constants(mRS);

        ProgramVertexFixedFunction.Builder b;
        b = new ProgramVertexFixedFunction.Builder(mRS);
        b.setTextureMatrixEnable(false);
        ProgramVertexFixedFunction pv = b.create();
        assertTrue(pv != null);
        pv.bindConstants(pva);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
        }
        pv.destroy();
        b.setTextureMatrixEnable(true);
        pv = b.create();
        assertTrue(pv != null);
        pv.bindConstants(pva);
        if (testBind) {
            mScript.invoke_testProgramVertex(pv);
        }
        pv.destroy();
    }

    public void testProgramVertexFixedFunctionBuilder() {
        testProgramVertexFixedFunctionBuilder(false);
        testProgramVertexFixedFunctionBuilder(true);
    }

}
