/**
 * Copyright (C) 2012 The Android Open Source Project
 * Licensed under the Apache License,  Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,  software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,  either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.renderscript.cts;

import android.renderscript.Allocation;
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;

public class RsFracTest extends RSBaseCompute {
    private ScriptC_rs_frac_f32 mScript;
    private ScriptC_rs_frac_f32_relaxed mScript_relaxed;

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut)
            throws RSRuntimeException {
        if (testId == TEST_F32) {
            mScript.forEach_root(mIn, mOut);
        } else if (testId == TEST_RELAXED_F32) {
            mScript_relaxed.forEach_root(mIn, mOut);
        }
    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idx= i * stride + j;
                int idxRef = i * (stride - skip) + j;
                ref[idxRef] = Math.min(in[idx] - (float)Math.floor((double)in[idx]), 0x1.fffffep-1f);
            }
        }
        return ref;
    }

    public void testRsFrac() {
        mScript = new ScriptC_rs_frac_f32(mRS, mRes, R.raw.rs_frac_f32);
        doF32(0x12, 0);
    }
    public void testRsFrac_relaxed() {
        mScript_relaxed = new ScriptC_rs_frac_f32_relaxed(mRS, mRes, R.raw.rs_frac_f32);
        doF32_relaxed(0x12, 1);
    }

}
