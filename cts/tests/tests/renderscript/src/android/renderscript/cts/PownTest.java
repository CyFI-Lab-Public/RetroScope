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

public class PownTest extends RSBaseCompute {
    private ScriptC_pown_f32 script_f32;
    private ScriptC_pown_f32_relaxed script_f32_relaxed;
    private int[] n;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_pown_f32(mRS);
        script_f32_relaxed = new ScriptC_pown_f32_relaxed(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_F32:
            script_f32.forEach_pown_f32_1(mIn, mOut);
            break;
        case TEST_F32_2:
            script_f32.forEach_pown_f32_2(mIn, mOut);
            break;
        case TEST_F32_3:
            script_f32.forEach_pown_f32_3(mIn, mOut);
            break;
        case TEST_F32_4:
            script_f32.forEach_pown_f32_4(mIn, mOut);
            break;

        case TEST_RELAXED_F32:
            script_f32_relaxed.forEach_pown_f32_1(mIn, mOut);
            break;
        case TEST_RELAXED_F32_2:
            script_f32_relaxed.forEach_pown_f32_2(mIn, mOut);
            break;
        case TEST_RELAXED_F32_3:
            script_f32_relaxed.forEach_pown_f32_3(mIn, mOut);
            break;
        case TEST_RELAXED_F32_4:
            script_f32_relaxed.forEach_pown_f32_4(mIn, mOut);
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
                ref[idxRef] = (float)Math.pow((double)in[idx], (double)n[idx]);
            }
        }
        return ref;
    }

    public void testPownF32() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32(mRS), INPUTSIZE);

        n = new int[INPUTSIZE];
        RSUtils.genRandom(0x12345678, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32.set_n1(nAlloc);

        doF32(0x716acd, 16);
    }

    public void testPownF32_relaxed() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32(mRS), INPUTSIZE);

        n = new int[INPUTSIZE];
        RSUtils.genRandom(0x12345678, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32_relaxed.set_n1(nAlloc);

        doF32_relaxed(0x716acd, 128);
    }

    public void testPownF32_2() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_2(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*2];
        RSUtils.genRandom(0xacdef1, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32.set_n2(nAlloc);

        doF32_2(0xacdef1, 16);
    }

    public void testPownF32_2_relaxed() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_2(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*2];
        RSUtils.genRandom(0xacdef1, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32_relaxed.set_n2(nAlloc);

        doF32_2_relaxed(0xacdef1, 128);
    }

    public void testPownF32_3() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_3(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*4];
        RSUtils.genRandom(0xa123f1, 32, 1, -16, n, 4, 1);
        nAlloc.copyFrom(n);
        script_f32.set_n3(nAlloc);

        doF32_3(0xaac3f1, 16);
    }

    public void testPownF32_3_relaxed() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_3(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*4];
        RSUtils.genRandom(0xa123f1, 32, 1, -16, n, 4, 1);
        nAlloc.copyFrom(n);
        script_f32_relaxed.set_n3(nAlloc);

        doF32_3_relaxed(0xaac3f1, 128);
    }

    public void testPownF32_4() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_4(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*4];
        RSUtils.genRandom(0x4323ca, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32.set_n4(nAlloc);

        doF32_4(0xaa12f1, 16);
    }

    public void testPownF32_4_relaxed() {
        Allocation nAlloc = Allocation.createSized(mRS, Element.I32_4(mRS), INPUTSIZE);

        n = new int[INPUTSIZE*4];
        RSUtils.genRandom(0x4323ca, 32, 1, -16, n);
        nAlloc.copyFrom(n);
        script_f32_relaxed.set_n4(nAlloc);

        doF32_4_relaxed(0xaa12f1, 128);
    }
}
