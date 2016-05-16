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
import android.renderscript.Float4;
import android.renderscript.RSRuntimeException;
import java.util.Random;

public class RsUnpackColor8888Test extends RSBaseCompute {
    private ScriptC_rs_unpack_color_8888 script;

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        script.forEach_root(mIn, mOut);
    }

    public void testRsUnpackColor8888RGB() {
        script = new ScriptC_rs_unpack_color_8888(mRS, mRes, R.raw.rs_unpack_color_8888);
        Allocation mAllocationIn = Allocation.createSized(mRS, Element.U8_4(mRS), INPUTSIZE);
        Allocation mAllocationOut = Allocation.createSized(mRS, Element.F32_4(mRS), INPUTSIZE);
        byte[] inArray = new byte[INPUTSIZE * 4];
        float[] outArray = new float[INPUTSIZE * 4];
        float[] refArray = new float[INPUTSIZE * 4];

        Random r = new Random(0x128fa817);
        r.nextBytes(inArray);

        mAllocationIn.copy1DRangeFrom(0, INPUTSIZE, inArray);
        try {
            RSUtils.forEach(this, 0, mAllocationIn, mAllocationOut);
        } catch (RSRuntimeException e) {
        }
        mAllocationOut.copyTo(outArray);

        for (int i = 0; i < outArray.length; i += 4) {
            byte[] inValues = {inArray[i], inArray[i+1], inArray[i+2], inArray[i+3]};
            Float4 c = rs_UnpackColor8888(inValues);
            refArray[i] = c.x;
            refArray[i + 1] = c.y;
            refArray[i + 2] = c.z;
            refArray[i + 3] = c.w;
            for (int j = 0; j < 4; j++){
                assertEquals(refArray[i+j], outArray[i+j]);
            }
        }
    }

    private static Float4 rs_UnpackColor8888(byte[] c) {
        Float4 ret = new Float4(0.003921569f, 0.003921569f, 0.003921569f, 0.003921569f);
        ret.x *= (c[0] & 0xff);
        ret.y *= (c[1] & 0xff);
        ret.z *= (c[2] & 0xff);
        ret.w *= (c[3] & 0xff);
        return ret;
    }
}
