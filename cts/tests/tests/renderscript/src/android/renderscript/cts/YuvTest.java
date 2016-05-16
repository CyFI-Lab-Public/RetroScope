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
import java.util.Random;

public class YuvTest extends RSBaseCompute {
    int width;
    int height;
    byte [] by;
    byte [] bu;
    byte [] bv;
    Allocation ay;
    Allocation au;
    Allocation av;

    protected ScriptC_verify mVerify;


    int getCWidth() {
        return (width + 1) / 2;
    }
    int getCHeight() {
        return (height + 1) / 2;
    }

    protected void makeYuvBuffer(int w, int h) {
        Random r = new Random();
        width = w;
        height = h;

        by = new byte[w*h];
        bu = new byte[getCWidth() * getCHeight()];
        bv = new byte[getCWidth() * getCHeight()];

        for (int i=0; i < by.length; i++) {
            by[i] = (byte)r.nextInt(256);
        }
        for (int i=0; i < bu.length; i++) {
            bu[i] = (byte)r.nextInt(256);
        }
        for (int i=0; i < bv.length; i++) {
            bv[i] = (byte)r.nextInt(256);
        }

        Type.Builder tb = new Type.Builder(mRS, Element.U8(mRS));
        tb.setX(w);
        tb.setY(h);
        ay = Allocation.createTyped(mRS, tb.create());

        tb = new Type.Builder(mRS, Element.U8(mRS));
        tb.setX(w >> 1);
        tb.setY(h >> 1);
        au = Allocation.createTyped(mRS, tb.create());
        av = Allocation.createTyped(mRS, tb.create());

        ay.copyFrom(by);
        au.copyFrom(bu);
        av.copyFrom(bv);
    }

    public Allocation makeOutput() {
        Type.Builder tb = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tb.setX(width);
        tb.setY(height);
        Type t = tb.create();
        return Allocation.createTyped(mRS, t);
    }

    // Test for the API 17 conversion path
    // This used a uchar buffer assuming nv21
    public void testV17() {
        mVerify = new ScriptC_verify(mRS);

        makeYuvBuffer(120, 96);
        Allocation aout = makeOutput();
        Allocation aref = makeOutput();

        byte tmp[] = new byte[(width * height) + (getCWidth() * getCHeight() * 2)];
        int i = 0;
        for (int j = 0; j < (width * height); j++) {
            tmp[i++] = by[j];
        }
        for (int j = 0; j < (getCWidth() * getCHeight()); j++) {
            tmp[i++] = bv[j];
            tmp[i++] = bu[j];
        }

        Allocation ta = Allocation.createSized(mRS, Element.U8(mRS), tmp.length);
        ta.copyFrom(tmp);


        ScriptIntrinsicYuvToRGB syuv = ScriptIntrinsicYuvToRGB.create(mRS, Element.U8(mRS));
        syuv.setInput(ta);
        syuv.forEach(aout);

        ScriptC_yuv script = new ScriptC_yuv(mRS);
        script.invoke_makeRef(ay, au, av, aref);

        mVerify.invoke_verify(aref, aout, ay);

        mRS.finish();
        mVerify.invoke_checkError();
        waitForMessage();
        checkForErrors();
    }

    // Test for the API 18 conversion path with nv21
    public void test_NV21() {
        mVerify = new ScriptC_verify(mRS);
        ScriptC_yuv script = new ScriptC_yuv(mRS);
        ScriptIntrinsicYuvToRGB syuv = ScriptIntrinsicYuvToRGB.create(mRS, Element.YUV(mRS));

        makeYuvBuffer(512, 512);
        Allocation aout = makeOutput();
        Allocation aref = makeOutput();


        Type.Builder tb = new Type.Builder(mRS, Element.YUV(mRS));
        tb.setX(width);
        tb.setY(height);
        tb.setYuvFormat(android.graphics.ImageFormat.NV21);
        Allocation ta = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        byte tmp[] = new byte[(width * height) + (getCWidth() * getCHeight() * 2)];
        int i = 0;
        for (int j = 0; j < (width * height); j++) {
            tmp[i++] = by[j];
        }
        for (int j = 0; j < (getCWidth() * getCHeight()); j++) {
            tmp[i++] = bv[j];
            tmp[i++] = bu[j];
        }
        ta.copyFrom(tmp);
        script.invoke_makeRef(ay, au, av, aref);

        syuv.setInput(ta);
        syuv.forEach(aout);
        mVerify.invoke_verify(aref, aout, ay);

        script.set_mInput(ta);
        script.forEach_cvt(aout);
        mVerify.invoke_verify(aref, aout, ay);

        mRS.finish();
        mVerify.invoke_checkError();
        waitForMessage();
        checkForErrors();
    }

    // Test for the API 18 conversion path with yv12
    public void test_YV12() {
        mVerify = new ScriptC_verify(mRS);
        ScriptC_yuv script = new ScriptC_yuv(mRS);
        ScriptIntrinsicYuvToRGB syuv = ScriptIntrinsicYuvToRGB.create(mRS, Element.YUV(mRS));

        makeYuvBuffer(512, 512);
        Allocation aout = makeOutput();
        Allocation aref = makeOutput();


        Type.Builder tb = new Type.Builder(mRS, Element.YUV(mRS));
        tb.setX(width);
        tb.setY(height);
        tb.setYuvFormat(android.graphics.ImageFormat.YV12);
        Allocation ta = Allocation.createTyped(mRS, tb.create(), Allocation.USAGE_SCRIPT);

        byte tmp[] = new byte[(width * height) + (getCWidth() * getCHeight() * 2)];
        int i = 0;
        for (int j = 0; j < (width * height); j++) {
            tmp[i++] = by[j];
        }
        for (int j = 0; j < (getCWidth() * getCHeight()); j++) {
            tmp[i++] = bu[j];
        }
        for (int j = 0; j < (getCWidth() * getCHeight()); j++) {
            tmp[i++] = bv[j];
        }
        ta.copyFrom(tmp);
        script.invoke_makeRef(ay, au, av, aref);

        syuv.setInput(ta);
        syuv.forEach(aout);
        mVerify.invoke_verify(aref, aout, ay);

        script.set_mInput(ta);
        script.forEach_cvt(aout);
        mVerify.invoke_verify(aref, aout, ay);

        mRS.finish();
        mVerify.invoke_checkError();
        waitForMessage();
        checkForErrors();
    }

}
