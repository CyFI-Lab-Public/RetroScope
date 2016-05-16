/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.cts.rscpp;

import com.android.cts.stub.R;

import android.content.Context;
import android.content.res.Resources;
import android.test.AndroidTestCase;
import android.renderscript.*;
import android.util.Log;
import java.lang.Integer;

public class RSConvolveTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private final int X = 1024;
    private final int Y = 1024;

    native boolean convolveTest(int X, int Y, byte[] input, byte[] output, float[] coeffs, boolean is3x3);
    public void testConvolve3x3() {
        int[] baseAlloc = new int[X * Y];
        float[] coeffs = new float[9];
        coeffs[0] = .5f;
        coeffs[1] = .35f;
        coeffs[2] =  .1f;
        coeffs[3] =  1.f;
        coeffs[4] =  1.f;
        coeffs[5] =  1.f;
        coeffs[6] =  .1f;
        coeffs[7] =  .35f;
        coeffs[8] =  .5f;

        RSUtils.genRandom(0x1DEFFD0, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y];
        for (int i = 0; i < X * Y; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        Type.Builder build = new Type.Builder(mRS, Element.A_8(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);
        ScriptIntrinsicConvolve3x3 convolve = ScriptIntrinsicConvolve3x3.create(mRS, Element.A_8(mRS));
        convolve.setInput(rsInput);
        convolve.setCoefficients(coeffs);
        convolve.forEach(rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y];
        convolveTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, true);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

    public void testConvolve5x5() {
        int[] baseAlloc = new int[X * Y];
        float[] coeffs = new float[25];
        coeffs[0] = .5f;
        coeffs[1] = .35f;
        coeffs[2] =  .1f;
        coeffs[3] =  1.f;
        coeffs[4] =  1.f;
        coeffs[5] =  1.f;
        coeffs[6] =  .1f;
        coeffs[7] =  .35f;
        coeffs[8] =  .5f;
        coeffs[9] = .5f;
        coeffs[10] = .35f;
        coeffs[11] =  .1f;
        coeffs[12] =  1.f;
        coeffs[13] =  1.f;
        coeffs[14] =  1.f;
        coeffs[15] =  .1f;
        coeffs[16] =  .35f;
        coeffs[17] =  .5f;
        coeffs[18] = .5f;
        coeffs[19] = .35f;
        coeffs[20] =  .1f;
        coeffs[21] =  1.f;
        coeffs[22] =  1.f;
        coeffs[23] =  1.f;
        coeffs[24] =  .1f;


        RSUtils.genRandom(0x1DEFFD0, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y];
        for (int i = 0; i < X * Y; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        Type.Builder build = new Type.Builder(mRS, Element.A_8(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);
        ScriptIntrinsicConvolve5x5 convolve = ScriptIntrinsicConvolve5x5.create(mRS, Element.A_8(mRS));
        convolve.setInput(rsInput);
        convolve.setCoefficients(coeffs);
        convolve.forEach(rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y];
        convolveTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, false);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }


}