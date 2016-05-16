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

public class IntrinsicConvolve3x3 extends IntrinsicBase {
    private void testConvolve3(int w, int h, Element.DataType dt, int vecSize) {
        float cf1[] = {0.f, 0.f, 0.f,  0.f, 1.f, 0.f,  0.f, 0.f, 0.f};
        float cf2[] = {0.f, -1.f, 0.f,  -1.f, 5.f, -1.f,  0.f, -1.f, 0.f};


        Element e = makeElement(dt, vecSize);

        System.gc();
        makeBuffers(w, h, e);


        ScriptIntrinsicConvolve3x3 si = ScriptIntrinsicConvolve3x3.create(mRS, e);
        si.setCoefficients(cf1);
        si.setInput(mAllocSrc);
        si.forEach(mAllocRef);

        ScriptC_intrinsic_convolve3x3 sr = new ScriptC_intrinsic_convolve3x3(mRS);
        sr.set_gCoeffs(cf1);
        sr.set_gIn(mAllocSrc);
        sr.set_gWidth(w);
        sr.set_gHeight(h);
        if (dt == Element.DataType.UNSIGNED_8) {
            switch(vecSize) {
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
            switch(vecSize) {
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

        android.util.Log.e("RSI test", "test convolve U8_" + vecSize + " 1 " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);

        si.setCoefficients(cf2);
        sr.set_gCoeffs(cf2);
        si.forEach(mAllocRef);
        if (dt == Element.DataType.UNSIGNED_8) {
            switch(vecSize) {
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
            switch(vecSize) {
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
        android.util.Log.e("RSI test", "test convolve U8_" + vecSize + " 2 " + w + ", " + h);
        mVerify.invoke_verify(mAllocRef, mAllocDst, mAllocSrc);
        mRS.finish();
    }


    public void test_U8_4() {
        testConvolve3(100, 100, Element.DataType.UNSIGNED_8, 4);
        checkError();
    }
    public void test_U8_3() {
        testConvolve3(100, 100, Element.DataType.UNSIGNED_8, 3);
        checkError();
    }
    public void test_U8_2() {
        testConvolve3(100, 100, Element.DataType.UNSIGNED_8, 2);
        checkError();
    }
    public void test_U8_1() {
        testConvolve3(100, 100, Element.DataType.UNSIGNED_8, 1);
        checkError();
    }

    public void test_F32_4() {
        testConvolve3(100, 100, Element.DataType.FLOAT_32, 4);
        checkError();
    }

    public void test_F32_3() {
        testConvolve3(100, 100, Element.DataType.FLOAT_32, 3);
        checkError();
    }

    public void test_F32_2() {
        testConvolve3(100, 100, Element.DataType.FLOAT_32, 2);
        checkError();
    }

    public void test_F32_1() {
        testConvolve3(100, 100, Element.DataType.FLOAT_32, 1);
        checkError();
    }


}
