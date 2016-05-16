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

public class RSBlurTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private final int X = 1024;
    private final int Y = 1024;

    native boolean blurTest(int X, int Y, byte[] input, byte[] output, boolean singleChannel);
    public void testRSBlurOneChannel() {
        int[] baseAlloc = new int[X * Y];
        RSUtils.genRandom(0x1DEFF, 255, 1, -128, baseAlloc);
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
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(mRS, Element.A_8(mRS));
        blur.setInput(rsInput);
        blur.setRadius(15);
        blur.forEach(rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y];
        blurTest(X, Y, byteAlloc, nativeByteAlloc, true);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }


    public void testRSBlurFourChannels() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0xFAFADE10, 255, 1, -128, baseAlloc);
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
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(mRS, Element.RGBA_8888(mRS));
        blur.setInput(rsInput);
        blur.setRadius(15);
        blur.forEach(rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        blurTest(X, Y, byteAlloc, nativeByteAlloc, false);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }


}