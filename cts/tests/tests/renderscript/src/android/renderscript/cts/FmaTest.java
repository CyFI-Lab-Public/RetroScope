/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;

public class FmaTest extends RSBaseCompute {
    private ScriptC_fma_f32 script_f32;
    private ScriptC_fma_f32_relaxed script_f32_relaxed;
    private Allocation mIn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_fma_f32(mRS);
        script_f32_relaxed = new ScriptC_fma_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_fma_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_fma_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_fma_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_fma_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_fma_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_fma_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_fma_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_fma_f32_4(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idx = i * stride * 3 + j;
                ref[i * (stride - skip) + j] = (float)((double)in[idx] * (double)in[idx+stride] + (double)in[idx+stride*2]);
            }
        }
        return ref;
    }

    @Override
    protected Allocation setInAlloc(Element e) {
        return mIn;
    }

    @Override
    protected float[] makeInArray(int size) {
        return new float[size*3];
    }

    public void testFmaF32() {
        ScriptField_Floats floatArray = new ScriptField_Floats(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32(0xea1, 0);
    }

    public void testFmaF32_relaxed() {
        ScriptField_Floats floatArray = new ScriptField_Floats(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_relaxed(0xea1, 0);
    }

    public void testFmaF32_2() {
        ScriptField_Floats2 floatArray = new ScriptField_Floats2(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_2(0x12a, 0);
    }

    public void testFmaF32_2_relaxed() {
        ScriptField_Floats2 floatArray = new ScriptField_Floats2(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_2_relaxed(0x12a, 0);
    }

    public void testFmaF32_3() {
        ScriptField_Floats3 floatArray = new ScriptField_Floats3(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_3(0xfae, 0);
    }

    public void testFmaF32_3_relaxed() {
        ScriptField_Floats3 floatArray = new ScriptField_Floats3(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_3_relaxed(0xfae, 0);
    }

    public void testFmaF32_4() {
        ScriptField_Floats4 floatArray = new ScriptField_Floats4(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_4(0x87a, 0);
    }

    public void testFmaF32_4_relaxed() {
        ScriptField_Floats4 floatArray = new ScriptField_Floats4(mRS, INPUTSIZE);
        mIn = floatArray.getAllocation();
        doF32_4_relaxed(0x87a, 0);
    }
}
