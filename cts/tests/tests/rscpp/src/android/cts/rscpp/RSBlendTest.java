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

public class RSBlendTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private static final int X = 256;
    private static final int Y = 256;

    native boolean blendTest(int X, int Y, byte[] input, byte[] output, int optionFlag);
    public void testRSBlend() {
        for (int iter = 0; iter < 15; iter++) {
            int[] baseAlloc = new int[X * Y * 4];
            RSUtils.genRandom(0x789321, 255, 1, -128, baseAlloc);
            RenderScript mRS = RenderScript.create(getContext());
            byte[] byteAlloc = new byte[X * Y * 4];
            for (int i = 0; i < X * Y * 4; i++) {
                byteAlloc[i] = (byte)baseAlloc[i];
            }

            int[] baseAlloc2 = new int[X * Y * 4];
            RSUtils.genRandom(0x359201, 255, 1, -128, baseAlloc2);
            byte[] byteAlloc2 = new byte[X * Y * 4];
            for (int i = 0; i < X * Y * 4; i++) {
                byteAlloc2[i] = (byte)baseAlloc2[i];
            }

            Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
            build.setX(X);
            build.setY(Y);
            Allocation rsInput = Allocation.createTyped(mRS, build.create());
            Allocation rsOutput = Allocation.createTyped(mRS, build.create());
            rsInput.copyFromUnchecked(byteAlloc);
            rsOutput.copyFromUnchecked(byteAlloc2);

            ScriptIntrinsicBlend blend = ScriptIntrinsicBlend.create(mRS, Element.RGBA_8888(mRS));

            switch(iter) {
            case 0:
                blend.forEachAdd(rsInput, rsOutput);
                break;
            case 1:
                blend.forEachClear(rsInput, rsOutput);
                break;
            case 2:
                blend.forEachDst(rsInput, rsOutput);
                break;
            case 3:
                blend.forEachDstAtop(rsInput, rsOutput);
                break;
            case 4:
                blend.forEachDstIn(rsInput, rsOutput);
                break;
            case 5:
                blend.forEachDstOut(rsInput, rsOutput);
                break;
            case 6:
                blend.forEachDstOver(rsInput, rsOutput);
                break;
            case 7:
                blend.forEachMultiply(rsInput, rsOutput);
                break;
            case 8:
                blend.forEachSrc(rsInput, rsOutput);
                break;
            case 9:
                blend.forEachSrcAtop(rsInput, rsOutput);
                break;
            case 10:
                blend.forEachSrcIn(rsInput, rsOutput);
                break;
            case 11:
                blend.forEachSrcOut(rsInput, rsOutput);
                break;
            case 12:
                blend.forEachSrcOver(rsInput, rsOutput);
                break;
            case 13:
                blend.forEachSubtract(rsInput, rsOutput);
                break;
            case 14:
                blend.forEachXor(rsInput, rsOutput);
                break;
            default:
                break;
            }

            blendTest(X, Y, byteAlloc, byteAlloc2, iter);
            rsOutput.copyTo(byteAlloc);
            for (int i = 0; i < X * Y * 4; i++) {
                assertTrue(byteAlloc[i] == byteAlloc2[i]);
            }

        }

    }
}