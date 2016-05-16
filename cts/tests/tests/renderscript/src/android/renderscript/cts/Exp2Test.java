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
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;

public class Exp2Test extends RSBaseCompute {
    private ScriptC_exp2_f32 script_f32;
    private ScriptC_exp2_f32_relaxed script_f32_relaxed;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_exp2_f32(mRS);
        script_f32_relaxed = new ScriptC_exp2_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_exp2_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_exp2_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_exp2_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_exp2_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_exp2_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_exp2_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_exp2_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_exp2_f32_4(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idx= i * stride + j;
                int idxRef = i * (stride - skip) + j;
                ref[idxRef] = (float)(Math.pow(2, (double)in[idx]));
            }
        }
        return ref;
    }

    public void testExp2F32() {
        doF32(0xa6, 3);
    }

    public void testExp2F32_relaxed() {
        doF32_relaxed(0xa6, 16);
    }

    public void testExp2F32_2() {
        doF32_2(0xab2, 3);
    }

    public void testExp2F32_2_relaxed() {
        doF32_2_relaxed(0xab2, 16);
    }

    public void testExp2F32_3() {
        doF32_3(0x617a, 3);
    }

    public void testExp2F32_3_relaxed() {
        doF32_3_relaxed(0x617a, 16);
    }

    public void testExp2F32_4() {
        doF32_4(0xabc3, 3);

    }
    public void testExp2F32_4_relaxed() {
        doF32_4_relaxed(0xabc3, 16);
    }

}
