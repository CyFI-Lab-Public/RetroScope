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
import android.renderscript.Element;
import android.renderscript.RSRuntimeException;

public class CrossTest extends RSBaseCompute {
    private ScriptC_cross_f32 script_f32;
    private ScriptC_cross_f32_relaxed script_f32_relaxed;
    private Allocation mIn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_cross_f32(mRS);
        script_f32_relaxed = new ScriptC_cross_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32_3:
            script_f32.forEach_cross_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_cross_f32_4(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_cross_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_cross_f32_4(mIn, mOut);
            break;
        }

    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            int idx= i * stride * 2;
            int idxRef = i * (stride - skip);
            ref[idxRef + 0] = in[idx+1] * in[idx+2+stride] - in[idx+2] * in[idx+1+stride];
            ref[idxRef + 1] = in[idx+2] * in[idx+0+stride] - in[idx+0] * in[idx+2+stride];
            ref[idxRef + 2] = in[idx+0] * in[idx+1+stride] - in[idx+1] * in[idx+0+stride];
            if (skip == 1)
                ref[idxRef + 3] = 0.f;
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
     * cross test for float3
     */
    public void testCrossF32_3() {
        ScriptField__cross_f32_3_struct in = new ScriptField__cross_f32_3_struct(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3(0x12345678, 0);
    }

    /*
    Disable until we can add an absolute error metric
    public void testCrossF32_3_relaxed() {
        ScriptField__cross_f32_3_struct in = new ScriptField__cross_f32_3_struct(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3_relaxed(0x12345678, 2);
    }
    */


    /**
     * cross test for float4
     */
    public void testCrossF32_4() {
        ScriptField__cross_f32_4_struct in = new ScriptField__cross_f32_4_struct(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4(0x12ac5678, 0);
    }

    /*
    Disable until we can add an absolute error metric
    public void testCrossF32_4_relaxed() {
        ScriptField__cross_f32_4_struct in = new ScriptField__cross_f32_4_struct(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4_relaxed(0x12ac5678, 2);
    }
    */

}
