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

package android.renderscript.cts;

import android.renderscript.*;
import android.util.Log;

public class IntrinsicColorMatrix extends IntrinsicBase {
    protected ScriptIntrinsicColorMatrix mSi;
    protected ScriptC_intrinsic_colormatrix mSr;

    private void subtest(int w, int h, Matrix4f mat, Float4 add,
                         Element.DataType dtIn, int vsIn,
                         Element.DataType dtOut, int vsOut) {


        if (mat == null) {
            mat = new Matrix4f();
        }

        if (mSi == null) {
            mSi = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));
            mSr = new ScriptC_intrinsic_colormatrix(mRS);
        }

        Element ein = makeElement(dtIn, vsIn);
        Element eout = makeElement(dtOut, vsOut);


        makeSource(w, h, ein);
        mAllocRef = makeAllocation(w, h, eout);
        mAllocDst = makeAllocation(w, h, eout);

        mSi.setColorMatrix(mat);
        mSi.setAdd(add);
        mSi.forEach(mAllocSrc, mAllocDst);
        mSr.invoke_reference(mat, add, mAllocSrc, mAllocRef);

        android.util.Log.e("RSI test", "test ColorMatrix  vsin=" + vsIn + ", vsout=" + vsOut + ",  dim " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);
        mRS.finish();
    }


    private void test(Element.DataType dtin, Element.DataType dtout, int subtest) {
        Float4 add = new Float4();
        Matrix4f mat = new Matrix4f();
        java.util.Random r = new java.util.Random(100);

        float f[] = mat.getArray();
        for (int i=0; i < f.length; i++) {
            f[i] = 0.f;
        }


        switch (subtest) {
        case 0:
            mVerify.set_gAllowedIntError(0);
            mat.loadIdentity();
            break;
        case 1:
            mVerify.set_gAllowedIntError(1);
            mat.set(0, 0, 1.f);
            mat.set(0, 1, 1.f);
            mat.set(0, 2, 1.f);
            break;
        default:
            mVerify.set_gAllowedIntError(2);
            for (int i=0; i < f.length; i++) {
                if (r.nextFloat() > 0.5f) {
                    f[i] = r.nextFloat() * (subtest - 1);
                }
            }
            for (int i=0; i < f.length; i++) {
                if (r.nextFloat() > 0.5f) {
                    add.x = r.nextFloat() * (subtest - 1);
                }
                if (r.nextFloat() > 0.5f) {
                    add.y = r.nextFloat() * (subtest - 1);
                }
                if (r.nextFloat() > 0.5f) {
                    add.z = r.nextFloat() * (subtest - 1);
                }
                if (r.nextFloat() > 0.5f) {
                    add.w = r.nextFloat() * (subtest - 1);
                }
            }
            android.util.Log.v("rs", "Mat [" + f[0] + ", " + f[4] + ", " + f[8] + ", " + f[12] + "]");
            android.util.Log.v("rs", "    [" + f[1] + ", " + f[5] + ", " + f[9] + ", " + f[13] + "]");
            android.util.Log.v("rs", "    [" + f[2] + ", " + f[6] + ", " + f[10] + ", " + f[14] + "]");
            android.util.Log.v("rs", "    [" + f[3] + ", " + f[7] + ", " + f[11] + ", " + f[15] + "]");
        }

        for (int i=1; i <= 4; i++) {
            for (int j=1; j <=4; j++) {
                subtest(101, 101, mat, add,
                        dtin, i,
                        dtout, j);
            }
        }
        checkError();
    }

    public void test_U8_U8_Ident() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.UNSIGNED_8, 0);
    }

    public void test_F32_F32_Ident() {
        test(Element.DataType.FLOAT_32, Element.DataType.FLOAT_32, 0);
    }

    public void test_U8_F32_Ident() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.FLOAT_32, 0);
    }

    public void test_F32_U8_Ident() {
        test(Element.DataType.FLOAT_32, Element.DataType.UNSIGNED_8, 0);
    }

    public void test_U8_U8_Rand() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.UNSIGNED_8, 2);
    }

    public void test_F32_F32_Rand() {
        test(Element.DataType.FLOAT_32, Element.DataType.FLOAT_32, 10);
    }

    public void test_U8_F32_Rand() {
        test(Element.DataType.UNSIGNED_8, Element.DataType.FLOAT_32, 10);
    }

    public void test_F32_U8_Rand() {
        test(Element.DataType.FLOAT_32, Element.DataType.UNSIGNED_8, 10);
    }

}
