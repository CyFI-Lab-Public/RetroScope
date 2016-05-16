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

public class FminTest extends RSBaseCompute {
    private ScriptC_fmin_f32 script_f32;
    private ScriptC_fmin_f32_relaxed script_f32_relaxed;
    private Allocation mIn;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_fmin_f32(mRS);
        script_f32_relaxed = new ScriptC_fmin_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_fmin_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_fmin_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_fmin_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_fmin_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_fmin_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_fmin_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_fmin_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_fmin_f32_4(mIn, mOut);
            break;
        }
    }

    @Override
    protected float[] getRefArray(float[] in, int input_size, int stride, int skip) {
        float[] ref = new float[input_size * stride];
        for (int i = 0; i < input_size; i++) {
            for (int j = 0; j < stride - skip; j++) {
                int idx = i * stride * 2 + j;
                ref[i * (stride - skip) + j] = Math.min(in[idx], in[idx+stride]);
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

    public void testfminF32() {
        ScriptField_fmin_f32_in in = new ScriptField_fmin_f32_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32(0x12678, 0);
    }

    public void testfminF32_relaxed() {
        ScriptField_fmin_f32_in in = new ScriptField_fmin_f32_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_relaxed(0x12678, 0);
    }

    public void testfminF32_2() {
        ScriptField_fmin_f32_2_in in = new ScriptField_fmin_f32_2_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_2(0x12ace, 0);
    }

    public void testfminF32_2_relaxed() {
        ScriptField_fmin_f32_2_in in = new ScriptField_fmin_f32_2_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_2_relaxed(0x12ace, 0);
    }

    public void testfminF32_3() {
        ScriptField_fmin_f32_3_in in = new ScriptField_fmin_f32_3_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3(0x12e8, 0);
    }

    public void testfminF32_3_relaxed() {
        ScriptField_fmin_f32_3_in in = new ScriptField_fmin_f32_3_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_3_relaxed(0x12e8, 0);
    }

    public void testfminF32_4() {
        ScriptField_fmin_f32_4_in in = new ScriptField_fmin_f32_4_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4(0xeac, 0);
    }

    public void testfminF32_4_relaxed() {
        ScriptField_fmin_f32_4_in in = new ScriptField_fmin_f32_4_in(mRS, INPUTSIZE);
        mIn = in.getAllocation();
        doF32_4_relaxed(0xeac, 0);
    }
}
