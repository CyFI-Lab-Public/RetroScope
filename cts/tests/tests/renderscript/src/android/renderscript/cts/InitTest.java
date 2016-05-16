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

public class InitTest extends RSBaseCompute {
    private Allocation mIn;
    private Allocation mOut;
    private ScriptC_init_test script;

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        script.forEach_root(mIn, mOut);
    }

    public void testInitTest() {
        script = new ScriptC_init_test(mRS, mRes, R.raw.init_test);
        float[] ref = new float[INPUTSIZE];
        float[] in = new float[INPUTSIZE];
        float[] out = new float[INPUTSIZE];
        mIn = Allocation.createSized(mRS, Element.F32(mRS), INPUTSIZE);
        mOut= Allocation.createSized(mRS, Element.F32(mRS), INPUTSIZE);
        for (int i = 0 ;i < INPUTSIZE; i++) {
            ref[i] = 2.0f;
            in[i] = 0;
        }

        mIn.copy1DRangeFrom(0, INPUTSIZE, in);

        try {
            RSUtils.forEach(this, 0, mIn, mOut);
        } catch (RSRuntimeException e) {
        }
        mOut.copyTo(out);
        checkArray(ref, out, INPUTSIZE, 1, 1, 0);
    }
}
