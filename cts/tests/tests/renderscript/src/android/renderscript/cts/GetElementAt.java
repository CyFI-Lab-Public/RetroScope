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
import android.renderscript.Type;
import android.renderscript.RSRuntimeException;
import com.android.cts.stub.R;
import java.util.Random;

public class GetElementAt extends RSBaseCompute {
    static final int TEST_ID_X = 0;
    static final int TEST_ID_XY = 1;
    private Allocation mOut;
    private Allocation gIn;
    private int[] in;
    private int[] out;
    ScriptC_get_element_at_x script_x;
    ScriptC_get_element_at_x_y script_xy;

    @Override
    public void forEach(int testId, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case TEST_ID_X:
            script_x.forEach_root(mOut);
            break;
        case TEST_ID_XY:
            script_xy.forEach_root(mOut);
            break;
        }
    }

    private void setupArrays(int size) {
        Random random = new Random(0x12345678);
        in = new int[size];
        for (int i = 0; i < size; ++i) {
            in[i] = random.nextInt(100);
        }
        out = new int[size];
    }

    public void testX () {
        setupArrays(INPUTSIZE);
        script_x = new ScriptC_get_element_at_x(mRS, mRes, R.raw.get_element_at_x);
        gIn = Allocation.createSized(mRS, Element.U32(mRS), INPUTSIZE);
        gIn.copyFrom(in);
        mOut = Allocation.createTyped(mRS, gIn.getType());
        script_x.set_gIn(gIn);
        try {
            RSUtils.forEach(this, TEST_ID_X, mOut);
        } catch (RSRuntimeException e) {
        }
        mOut.copyTo(out);
        for (int k = 0; k < INPUTSIZE; ++k) {
            assertEquals("idx = " + k, in[k], out[k]);
        }
    }

    public void testXY () {
        setupArrays(INPUTSIZE*INPUTSIZE);
        script_xy = new ScriptC_get_element_at_x_y(mRS, mRes, R.raw.get_element_at_x_y);
        Type.Builder builder = new Type.Builder(mRS, Element.U32(mRS));
        builder.setX(INPUTSIZE);
        builder.setY(INPUTSIZE);
        Type type = builder.create();
        gIn = Allocation.createTyped(mRS, type);
        gIn.copyFrom(in);
        mOut = Allocation.createTyped(mRS, gIn.getType());
        script_xy.set_gIn(gIn);
        try {
            RSUtils.forEach(this, TEST_ID_XY, mOut);
        } catch (RSRuntimeException e) {
        }
        mOut.copyTo(out);
        for (int k = 0; k < INPUTSIZE*INPUTSIZE; ++k) {
            assertEquals("idx = " + k, in[k], out[k]);
        }
    }
}
