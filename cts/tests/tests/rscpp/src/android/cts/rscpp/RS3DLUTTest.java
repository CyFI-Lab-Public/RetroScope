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

public class RS3DLUTTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private final int X = 1024;
    private final int Y = 1024;

    private final int lutSize = 64;

    native boolean lutTest(int X, int Y, int lutSize, byte[] input, byte[] input2, byte[] output);
    public void testRSLUT() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x419144, 255, 1, -128, baseAlloc);
        int[] colorCube = new int[lutSize * lutSize * lutSize * 4];
        RSUtils.genRandom(0x555007, 255, 1, -128, colorCube);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        byte[] byteColorCube = new byte[lutSize * lutSize * lutSize * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }
        for (int i = 0; i < lutSize * lutSize * lutSize * 4; i++) {
            byteColorCube[i] = (byte)colorCube[i];
        }


        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        Type.Builder buildCube = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(lutSize);
        build.setY(lutSize);
        build.setZ(lutSize);
        Allocation cube = Allocation.createTyped(mRS, build.create());
        cube.copyFromUnchecked(byteColorCube);
        ScriptIntrinsic3DLUT lut = ScriptIntrinsic3DLUT.create(mRS, Element.RGBA_8888(mRS));

        lut.setLUT(cube);
        lut.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        lutTest(X, Y, lutSize, byteAlloc, byteColorCube, nativeByteAlloc);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

}