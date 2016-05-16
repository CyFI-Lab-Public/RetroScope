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
import android.renderscript.Float3;
import android.renderscript.Float4;
import android.renderscript.RSRuntimeException;

public class RsPackColorTo8888Test extends RSBaseCompute {
    private ScriptC_rs_pack_color_to_8888 script_f32;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        script_f32 = new ScriptC_rs_pack_color_to_8888(mRS);
    }

    @Override
    public void forEach(int testId, Allocation mIn, Allocation mOut) throws RSRuntimeException {
        switch (testId) {
        case 0:
            script_f32.forEach_pack_color_to_8888_rgb(mIn, mOut);
            break;
        case 1:
            script_f32.forEach_pack_color_to_8888_rgba(mIn, mOut);
            break;
        case 2:
            script_f32.forEach_pack_color_to_8888_f32_3(mIn, mOut);
            break;
        case 3:
            script_f32.forEach_pack_color_to_8888_f32_4(mIn, mOut);
            break;
        }
    }

    public void testRsPackColorTo8888RGB() {
        float3input(0x17abc72, 0);
    }

    public void testRsPackColorTo8888RGBA() {
        float4input(0x76a6b, 1);
    }

    public void testRsPackColorTo8888F32_3() {
        float3input(0x17abc72, 2);
    }

    public void testRsPackColorTo8888F32_4() {
        float4input(0xabc72, 3);
    }

    private void float3input(long seed, int testId) {
        Allocation mAllocationIn = Allocation.createSized(mRS, Element.F32_3(mRS), INPUTSIZE);
        Allocation mAllocationOut = Allocation.createSized(mRS, Element.U8_4(mRS), INPUTSIZE);
        float[] inArray = new float[INPUTSIZE * 4];
        byte[] outArray = new byte[INPUTSIZE * 4];
        byte[] refArray = new byte[INPUTSIZE * 4];
        RSUtils.genRandom(seed, 1, 0, inArray, 4, 1);
        mAllocationIn.copy1DRangeFrom(0, INPUTSIZE, inArray);
        try {
            RSUtils.forEach(this, testId, mAllocationIn, mAllocationOut);
        } catch (RSRuntimeException e) {
        }
        mAllocationOut.copyTo(outArray);
        for (int i = 0; i < outArray.length; i += 4) {
            int offset = i;
            Float3 inValues = new Float3(inArray[offset], inArray[offset + 1], inArray[offset + 2]);
            byte[] cValue = rs_PackColorTo8888(inValues);
            refArray[i] = cValue[0];
            refArray[i + 1] = cValue[1];
            refArray[i + 2] = cValue[2];
            refArray[i + 3] = cValue[3];
            for (int j = 0; j < 4; j++){
                assertEquals(refArray[i+j] & 0xff, outArray[i+j] & 0xff);
            }
        }
    }

    private void float4input(long seed, int testId) {
        Allocation mAllocationIn = Allocation.createSized(mRS, Element.F32_4(mRS), INPUTSIZE);
        Allocation mAllocationOut = Allocation.createSized(mRS, Element.U8_4(mRS), INPUTSIZE);
        float[] inArray = new float[INPUTSIZE * 4];
        byte[] outArray = new byte[INPUTSIZE * 4];
        byte[] refArray = new byte[INPUTSIZE * 4];
        RSUtils.genRandom(seed, 1, 0, inArray);
        mAllocationIn.copy1DRangeFrom(0, INPUTSIZE, inArray);
        try {
            RSUtils.forEach(this, testId, mAllocationIn, mAllocationOut);
        } catch (RSRuntimeException e) {
        }
        mAllocationOut.copyTo(outArray);
        for (int i = 0; i < outArray.length; i += 4) {
            int offset = i;
            Float4 inValues = new Float4( inArray[offset],inArray[offset + 1],inArray[offset + 2],inArray[offset + 3]);
            byte[] cValue = rs_PackColorTo8888(inValues);
            refArray[i] = cValue[0];
            refArray[i + 1] = cValue[1];
            refArray[i + 2] = cValue[2];
            refArray[i + 3] = cValue[3];
            for (int j = 0; j < 4; j++){
                assertEquals(refArray[i+j] & 0xff, outArray[i+j] & 0xff);
            }
        }
    }

    private byte[] rs_PackColorTo8888(Float3 color) {
        color.x *= 255.f;
        color.y *= 255.f;
        color.z *= 255.f;
        color.x += 0.5f;
        color.y += 0.5f;
        color.z += 0.5f;
        byte[] c = {
            (byte) color.x, (byte) color.y, (byte) color.z, (byte) 255
        };
        return c;
    }

    private byte[] rs_PackColorTo8888(Float4 color) {
        color.x *= 255.f;
        color.y *= 255.f;
        color.z *= 255.f;
        color.x += 0.5f;
        color.y += 0.5f;
        color.z += 0.5f;
        color.w *= 255.f;
        color.w += 0.5f;
        byte[] c = {
            (byte) color.x, (byte) color.y, (byte) color.z, (byte) color.w
        };
        return c;
    }
}
