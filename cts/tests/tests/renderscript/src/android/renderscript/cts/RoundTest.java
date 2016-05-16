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

import com.android.cts.stub.R;
import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;

public class RoundTest extends RSBaseCompute {
    private ScriptC_round_f32 script_f32;
    private ScriptC_round_f32_relaxed script_f32_relaxed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_round_f32(mRS);
        script_f32_relaxed = new ScriptC_round_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut)
            throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_round_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_round_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_round_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_round_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_round_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_round_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_round_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_round_f32_4(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] inArray, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * (stride - skip)];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idxIn = i * stride + j;
                int idxRef = i * (stride - skip) + j;
                int res = ((Float.floatToIntBits(inArray[idxIn]) >> 31) & 0x01);
                float roundValue = (float) Math.round(inArray[idxIn]);
                float expective = roundValue;
                if ((roundValue - inArray[idxIn]) == 0.5f && res == 1)
                    expective -= 1;
                if (res == 1 && expective == +0.0f) {
                    expective = -0.0f;
                }
                ref[idxRef] = expective;
            }
        }
        return ref;
    }

    /**
     * round test for float
     */
    public void testRoundF32() {
        doF32(0x12345678, 0);
    }

    public void testRoundF32_relaxed() {
        doF32_relaxed(0x12345678, 0);
    }

    /**
     * round test for float2
     */
    public void testRoundF32_2() {
        doF32_2(0x123a5678, 0);
    }

    public void testRoundF32_2_relaxed() {
        doF32_2_relaxed(0x123a5678, 0);
    }

    /**
     * round test for float3
     */
    public void testRoundF32_3() {
        doF32_3(0x1af45678, 0);
    }

    public void testRoundF32_3_relaxed() {
        doF32_3_relaxed(0x1af45678, 0);
    }

    /**
     * round test for float4
     */
    public void testRoundF32_4() {
        doF32_4(0x1f345678, 0);

    }
    public void testRoundF32_4_relaxed() {
        doF32_4_relaxed(0x1f345678, 0);
    }

}
