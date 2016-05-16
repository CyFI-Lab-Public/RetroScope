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

import android.renderscript.Byte2;
import android.renderscript.Byte3;
import android.renderscript.Byte4;

import android.renderscript.Double2;
import android.renderscript.Double3;
import android.renderscript.Double4;

import android.renderscript.Element;

import android.renderscript.Float2;
import android.renderscript.Float3;
import android.renderscript.Float4;

import android.renderscript.Int2;
import android.renderscript.Int3;
import android.renderscript.Int4;

import android.renderscript.Long2;
import android.renderscript.Long3;
import android.renderscript.Long4;

import android.renderscript.RSRuntimeException;

import android.renderscript.Short2;
import android.renderscript.Short3;
import android.renderscript.Short4;

import android.renderscript.Matrix4f;

import android.renderscript.Type;

import android.renderscript.ScriptGroup;

import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.ScriptIntrinsicConvolve5x5;
import android.renderscript.ScriptIntrinsicLUT;

import com.android.cts.stub.R;

public class ImageProcessingTest extends RSBaseCompute {
    private Allocation a1, a2;

    private final int MAX_RADIUS = 25;
    private final int dimX = 256;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Type t = new Type.Builder(mRS, Element.U8_4(mRS)).setX(dimX).setY(dimX).create();
        a1 = Allocation.createTyped(mRS, t);
        a2 = Allocation.createTyped(mRS, t);
    }

    public void testBlur() {
        ScriptIntrinsicBlur mBlur;
        mBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        for (int i = 1; i < MAX_RADIUS; i++) {

            a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);

            mBlur.setRadius(i);
            mBlur.setInput(a1_copy);

            mBlur.forEach(a2_copy);

            // validate

        }

    }

    public void testBlend() {
        ScriptIntrinsicBlend mBlend;
        mBlend = ScriptIntrinsicBlend.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        for (int i = 0; i < 14; i++) {

            a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);
            a2_copy.copy2DRangeFrom(0, 0, a2.getType().getX(), a2.getType().getY(), a2, 0, 0);

            switch (i) {
            case 0:
                mBlend.forEachSrc(a1_copy, a2_copy);
                break;
            case 1:
                mBlend.forEachDst(a1_copy, a2_copy);
                break;
            case 2:
                mBlend.forEachSrcOver(a1_copy, a2_copy);
                break;
            case 3:
                mBlend.forEachDstOver(a1_copy, a2_copy);
                break;
            case 4:
                mBlend.forEachSrcIn(a1_copy, a2_copy);
                break;
            case 5:
                mBlend.forEachDstIn(a1_copy, a2_copy);
                break;
            case 6:
                mBlend.forEachSrcOut(a1_copy, a2_copy);
                break;
            case 7:
                mBlend.forEachDstOut(a1_copy, a2_copy);
                break;
            case 8:
                mBlend.forEachSrcAtop(a1_copy, a2_copy);
                break;
            case 9:
                mBlend.forEachDstAtop(a1_copy, a2_copy);
                break;
            case 10:
                mBlend.forEachXor(a1_copy, a2_copy);
                break;
            case 11:
                mBlend.forEachAdd(a1_copy, a2_copy);
                break;
            case 12:
                mBlend.forEachSubtract(a1_copy, a2_copy);
                break;
            case 13:
                mBlend.forEachMultiply(a1_copy, a2_copy);
                break;
            }

            // validate

        }

    }

    public void testColorMatrix() {
        ScriptIntrinsicColorMatrix mColorMatrix;
        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        Matrix4f m = new Matrix4f();
        m.set(1, 0, 0.2f);
        m.set(1, 1, 0.9f);
        m.set(1, 2, 0.2f);

        //test greyscale
        mColorMatrix.setGreyscale();

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);
        a2_copy.copy2DRangeFrom(0, 0, a2.getType().getX(), a2.getType().getY(), a2, 0, 0);

        mColorMatrix.forEach(a1_copy, a2_copy);

        //validate greyscale


        //test color matrix
        mColorMatrix.setColorMatrix(m);

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);
        a2_copy.copy2DRangeFrom(0, 0, a2.getType().getX(), a2.getType().getY(), a2, 0, 0);

        mColorMatrix.forEach(a1_copy, a2_copy);

        //validate color matrix


    }


    public void testConvolve3x3() {
        ScriptIntrinsicConvolve3x3 mConvolve3x3;
        mConvolve3x3 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);

        float f[] = new float[9];
        f[0] =  0.f;    f[1] = -1.f;    f[2] =  0.f;
        f[3] = -1.f;    f[4] =  5.f;    f[5] = -1.f;
        f[6] =  0.f;    f[7] = -1.f;    f[8] =  0.f;

        mConvolve3x3.setCoefficients(f);
        mConvolve3x3.setInput(a1_copy);
        mConvolve3x3.forEach(a2_copy);

        // validate

    }

    public void testConvolve5x5() {
        ScriptIntrinsicConvolve5x5 mConvolve5x5;
        mConvolve5x5 = ScriptIntrinsicConvolve5x5.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);

        float f[] = new float[25];
        f[0] = -1.f; f[1] = -3.f; f[2] = -4.f; f[3] = -3.f; f[4] = -1.f;
        f[5] = -3.f; f[6] =  0.f; f[7] =  6.f; f[8] =  0.f; f[9] = -3.f;
        f[10]= -4.f; f[11]=  6.f; f[12]= 20.f; f[13]=  6.f; f[14]= -4.f;
        f[15]= -3.f; f[16]=  0.f; f[17]=  6.f; f[18]=  0.f; f[19]= -3.f;
        f[20]= -1.f; f[21]= -3.f; f[22]= -4.f; f[23]= -3.f; f[24]= -1.f;

        mConvolve5x5.setCoefficients(f);
        mConvolve5x5.setInput(a1_copy);
        mConvolve5x5.forEach(a2_copy);

        // validate

    }

    public void testLUT() {
        ScriptIntrinsicLUT mLUT;
        mLUT = ScriptIntrinsicLUT.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);

        for (int ct=0; ct < 256; ct++) {
            float f = ((float)ct) / 255.f;

            float r = f;
            if (r < 0.5f) {
                r = 4.0f * r * r * r;
            } else {
                r = 1.0f - r;
                r = 1.0f - (4.0f * r * r * r);
            }
            mLUT.setRed(ct, (int)(r * 255.f + 0.5f));

            float g = f;
            if (g < 0.5f) {
                g = 2.0f * g * g;
            } else {
                g = 1.0f - g;
                g = 1.0f - (2.0f * g * g);
            }
            mLUT.setGreen(ct, (int)(g * 255.f + 0.5f));

            float b = f * 0.5f + 0.25f;
            mLUT.setBlue(ct, (int)(b * 255.f + 0.5f));
        }

        mLUT.forEach(a1_copy, a2_copy);

        // validate

    }

    public void testScriptGroup() {
        ScriptGroup group;

        ScriptIntrinsicConvolve3x3 mConvolve3x3;
        ScriptIntrinsicColorMatrix mColorMatrix;

        mConvolve3x3 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, a1.getType());
        a2_copy = Allocation.createTyped(mRS, a2.getType());

        a1_copy.copy2DRangeFrom(0, 0, a1.getType().getX(), a1.getType().getY(), a1, 0, 0);

        float f[] = new float[9];
        f[0] =  0.f;    f[1] = -1.f;    f[2] =  0.f;
        f[3] = -1.f;    f[4] =  5.f;    f[5] = -1.f;
        f[6] =  0.f;    f[7] = -1.f;    f[8] =  0.f;

        mConvolve3x3.setCoefficients(f);

        Matrix4f m = new Matrix4f();
        m.set(1, 0, 0.2f);
        m.set(1, 1, 0.9f);
        m.set(1, 2, 0.2f);
        mColorMatrix.setColorMatrix(m);

        Type connect = new Type.Builder(mRS, Element.U8_4(mRS)).setX(dimX).setY(dimX).create();

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mConvolve3x3.getKernelID());
        b.addKernel(mColorMatrix.getKernelID());
        b.addConnection(connect, mConvolve3x3.getKernelID(), mColorMatrix.getKernelID());
        group = b.create();

        mConvolve3x3.setInput(a1_copy);
        group.setOutput(mColorMatrix.getKernelID(), a2_copy);
        group.execute();

        // validate

    }


}
