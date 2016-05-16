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

public class RSColorMatrixTest extends RSCppTest {
    static {
        System.loadLibrary("rscpptest_jni");
    }

    private final int X = 1024;
    private final int Y = 1024;

    native boolean colorMatrixTest(int X, int Y, byte[] input, byte[] output, float[] coeffs, int optionFlag);
    public void testRSColorMatrix0() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x251107, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        float[] coeffs = new float[9];
        coeffs[0] = 1.f;
        coeffs[1] = 0.3f;
        coeffs[2] = 0.7f;
        coeffs[3] =  0.2f;
        coeffs[4] =  1.f;
        coeffs[5] =  -0.1f;
        coeffs[6] =  -0.5f;
        coeffs[7] =  0.2f;
        coeffs[8] =  1.f;

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        ScriptIntrinsicColorMatrix cm = ScriptIntrinsicColorMatrix.create(mRS, Element.RGBA_8888(mRS));
        Matrix3f mat = new Matrix3f(coeffs);
        cm.setColorMatrix(mat);
        cm.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        colorMatrixTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, 0);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

    public void testRSColorMatrix1() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x251106, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        float[] coeffs = new float[9];

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        ScriptIntrinsicColorMatrix cm = ScriptIntrinsicColorMatrix.create(mRS, Element.RGBA_8888(mRS));
        cm.setGreyscale();
        cm.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        colorMatrixTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, 1);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

    public void testRSColorMatrix2() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x251105, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        float[] coeffs = new float[16];
        for (int i = 0; i < 16; i++) {
            coeffs[i] = -2.f + .25f*i;
        }

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        ScriptIntrinsicColorMatrix cm = ScriptIntrinsicColorMatrix.create(mRS, Element.RGBA_8888(mRS));
        Matrix4f mat = new Matrix4f(coeffs);
        cm.setColorMatrix(mat);
        cm.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        colorMatrixTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, 2);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

    public void testRSColorMatrix3() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x251104, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        float[] coeffs = new float[9];

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        ScriptIntrinsicColorMatrix cm = ScriptIntrinsicColorMatrix.create(mRS, Element.RGBA_8888(mRS));
        cm.setYUVtoRGB();
        cm.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        colorMatrixTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, 3);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

    public void testRSColorMatrix4() {
        int[] baseAlloc = new int[X * Y * 4];
        RSUtils.genRandom(0x251103, 255, 1, -128, baseAlloc);
        RenderScript mRS = RenderScript.create(getContext());
        byte[] byteAlloc = new byte[X * Y * 4];
        for (int i = 0; i < X * Y * 4; i++) {
            byteAlloc[i] = (byte)baseAlloc[i];
        }

        float[] coeffs = new float[9];

        Type.Builder build = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        build.setX(X);
        build.setY(Y);
        Allocation rsInput = Allocation.createTyped(mRS, build.create());
        Allocation rsOutput = Allocation.createTyped(mRS, build.create());
        rsInput.copyFromUnchecked(byteAlloc);

        ScriptIntrinsicColorMatrix cm = ScriptIntrinsicColorMatrix.create(mRS, Element.RGBA_8888(mRS));
        cm.setRGBtoYUV();
        cm.forEach(rsInput, rsOutput);

        byte[] nativeByteAlloc = new byte[X * Y * 4];
        colorMatrixTest(X, Y, byteAlloc, nativeByteAlloc, coeffs, 4);
        rsOutput.copyTo(byteAlloc);

        for (int i = 0; i < X * Y * 4; i++) {
            assertTrue(byteAlloc[i] == nativeByteAlloc[i]);
        }

    }

}