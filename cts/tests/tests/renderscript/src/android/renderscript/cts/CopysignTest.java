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

public class CopysignTest extends RSBaseCompute {
    private ScriptC_copysign_f32 script_f32;
    private ScriptC_copysign_f32_relaxed script_f32_relaxed;
    private Allocation mIn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_copysign_f32(mRS);
        script_f32_relaxed = new ScriptC_copysign_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_copysign_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_copysign_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_copysign_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_copysign_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_copysign_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_copysign_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_copysign_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_copysign_f32_4(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        int vec_size = stride - skip;
        float[] ref = new float[vec_size * input_size];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < vec_size; j++) {
                int idx = i * stride * 2 + j;
                ref[i*vec_size + j] = Math.copySign(in[idx], in[idx + stride]);
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
        return new float[size*2];
    }

    /**
     * Tests copysign(float, float).
     */
    public void testCopysignF32() {
        ScriptField_copysign_f32_input in = new ScriptField_copysign_f32_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32(0x12ac5678, 0);
    }

    public void testCopysignF32_relaxed() {
        ScriptField_copysign_f32_input in = new ScriptField_copysign_f32_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_relaxed(0x12ac5678, 0);
    }

    /**
     * Tests copysign(float2, float2).
     */
    public void testCopysignF32_2() {
        ScriptField_copysign_f32_2_input in = new ScriptField_copysign_f32_2_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_2(0x12fe5678, 0);
    }

    public void testCopysignF32_2_relaxed() {
        ScriptField_copysign_f32_2_input in = new ScriptField_copysign_f32_2_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_2_relaxed(0x12fe5678, 0);
    }

    /**
     * Tests copysign(float3, float3).
     */
    public void testCopysignF32_3() {
        ScriptField_copysign_f32_3_input in = new ScriptField_copysign_f32_3_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3(0x1c345678, 0);
    }

    public void testCopysignF32_3_relaxed() {
        ScriptField_copysign_f32_3_input in = new ScriptField_copysign_f32_3_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3_relaxed(0x1c345678, 0);
    }

    /**
     * Tests copysign(float4, float4).
     */
    public void testCopysignF32_4() {
        ScriptField_copysign_f32_4_input in = new ScriptField_copysign_f32_4_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4(0x123a5f7d, 0);
    }

    public void testCopysignF32_4_relaxed() {
        ScriptField_copysign_f32_4_input in = new ScriptField_copysign_f32_4_input(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4_relaxed(0x123a5f7d, 0);
    }
}
