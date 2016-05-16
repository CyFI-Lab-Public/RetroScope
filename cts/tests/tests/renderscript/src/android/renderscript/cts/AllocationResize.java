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
import java.util.Random;

public class AllocationResize extends RSBaseCompute {
    private Allocation mIn;
    private Allocation mOut;
    private ScriptC_allocation_resize mScript;

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        mScript.forEach_root(mIn, mOut);
    }

    public void testResize() {
        mScript = new ScriptC_allocation_resize(mRS, mRes, R.raw.allocation_resize);
        mIn = Allocation.createSized(mRS, Element.I32(mRS), INPUTSIZE/2);
        mOut = Allocation.createSized(mRS, Element.I32(mRS), INPUTSIZE*2);
        mIn.resize(INPUTSIZE);
        mOut.resize(INPUTSIZE);

        Random random = new Random(0x12345678);
        int[] outArray = new int[INPUTSIZE];
        int[] inArray = new int[INPUTSIZE];
        for (int i = 0; i < INPUTSIZE; i++) {
            inArray[i] = random.nextInt();
        }
        mIn.copy1DRangeFrom(0, INPUTSIZE, inArray);

        try {
            RSUtils.forEach(this, 0, mIn, mOut);
        } catch (RSRuntimeException e) {
        }

        mOut.copyTo(outArray);
        for (int i = 0; i < INPUTSIZE; i++) {
            assertEquals("Incorrect value @ idx = " + i + " | ", inArray[i], outArray[i]);
        }
    }
}
