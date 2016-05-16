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

public class IntrinsicConvolve5x5 extends IntrinsicBase {
    private void test5(ScriptC_intrinsic_convolve5x5 sr, ScriptIntrinsicConvolve5x5 si,
                        Element e, float cf[], String name, int num, int w, int h) {
        si.setCoefficients(cf);
        si.setInput(mAllocSrc);
        si.forEach(mAllocRef);

        sr.set_gWidth(w);
        sr.set_gHeight(h);
        sr.set_gCoeffs(cf);
        sr.set_gIn(mAllocSrc);
        if (e.getDataType() == Element.DataType.UNSIGNED_8) {
            switch(e.getVectorSize()) {
            case 4:
                sr.forEach_convolve_U4(mAllocDst);
                break;
            case 3:
                sr.forEach_convolve_U3(mAllocDst);
                break;
            case 2:
                sr.forEach_convolve_U2(mAllocDst);
                break;
            case 1:
                sr.forEach_convolve_U1(mAllocDst);
                break;
            }
        } else {
            switch(e.getVectorSize()) {
            case 4:
                sr.forEach_convolve_F4(mAllocDst);
                break;
            case 3:
                sr.forEach_convolve_F3(mAllocDst);
                break;
            case 2:
                sr.forEach_convolve_F2(mAllocDst);
                break;
            case 1:
                sr.forEach_convolve_F1(mAllocDst);
                break;
            }
        }

        android.util.Log.e("RSI test", name + "  " + e.getVectorSize() + " " + num + " " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);
        mRS.finish();
    }

    private void testConvolve5(int w, int h, Element.DataType dt, int vecSize) {
        float cf1[] = { 0.f,  0.f,  0.f,  0.f,  0.f,
                        0.f,  0.f,  0.f,  0.f,  0.f,
                        0.f,  0.f,  1.f,  0.f,  0.f,
                        0.f,  0.f,  0.f,  0.f,  0.f,
                        0.f,  0.f,  0.f,  0.f,  0.f};
        float cf2[] = {-1.f, -1.f, -1.f, -1.f, -1.f,
                       -1.f,  0.f,  0.f,  0.f, -1.f,
                       -1.f,  0.f, 16.f,  0.f, -1.f,
                       -1.f,  0.f,  0.f,  0.f, -1.f,
                       -1.f, -1.f, -1.f, -1.f, -1.f};

        Element e = makeElement(dt, vecSize);
        makeBuffers(w, h, e);

        ScriptIntrinsicConvolve5x5 si = ScriptIntrinsicConvolve5x5.create(mRS, e);
        ScriptC_intrinsic_convolve5x5 sr = new ScriptC_intrinsic_convolve5x5(mRS);
        test5(sr, si, e, cf1, "test convolve", 1, w, h);
        test5(sr, si, e, cf2, "test convolve", 2, w, h);
    }

    public void test_U8_4() {
        testConvolve5(100, 100, Element.DataType.UNSIGNED_8, 4);
        checkError();
    }
    public void test_U8_3() {
        testConvolve5(100, 100, Element.DataType.UNSIGNED_8, 3);
        checkError();
    }
    public void test_U8_2() {
        testConvolve5(100, 100, Element.DataType.UNSIGNED_8, 2);
        checkError();
    }
    public void test_U8_1() {
        testConvolve5(100, 100, Element.DataType.UNSIGNED_8, 1);
        checkError();
    }

    public void test_F32_4() {
        testConvolve5(100, 100, Element.DataType.FLOAT_32, 4);
        checkError();
    }
    public void test_F32_3() {
        testConvolve5(100, 100, Element.DataType.FLOAT_32, 3);
        checkError();
    }
    public void test_F32_2() {
        testConvolve5(100, 100, Element.DataType.FLOAT_32, 2);
        checkError();
    }
    public void test_F32_1() {
        testConvolve5(100, 100, Element.DataType.FLOAT_32, 1);
        checkError();
    }


}
