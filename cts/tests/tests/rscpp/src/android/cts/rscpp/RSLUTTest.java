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

public class RSLUTTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private final int X = 1024;
    private final int Y = 1024;

    native boolean lutTest(int X, int Y, byte[] input, byte[] output);
    public void testRSLUT() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x72727272, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);
        ScriptIntrinsicLUT lut = ScriptIntrinsicLUT.create(mRS, Element.RGBA_8888(mRS));
        for (int i = 0; i < 256; i++) {
            lut.setRed(i, 255-i);
            lut.setGreen(i, 255-i);
            lut.setBlue(i, 255-i);
        }

        lut.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        lutTest(X, Y, byteAlloc, nativeByteAlloc);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

}