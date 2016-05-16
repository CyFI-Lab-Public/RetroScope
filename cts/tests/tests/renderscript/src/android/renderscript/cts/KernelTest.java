/*
 * Copyright (C) 2011-2012 The Android Open Source Project
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

import android.renderscript.Type;

import com.android.cts.stub.R;

public class KernelTest extends RSBaseCompute {
    /**
     * Test support for reflected forEach() as well as validation of parameters.
     */
    public void testForEach() {
        int x = 7;

        // badOut is always I8, so it is always an invalid type
        Type t = new Type.Builder(mRS, Element.I8(mRS)).setX(x).create();
        Allocation badOut = Allocation.createTyped(mRS, t);

        ScriptC_kernel_all kernel_all = new ScriptC_kernel_all(mRS);

        // I8
        Allocation in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U8(mRS)).setX(x).create();
        Allocation out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I8_2
        t = new Type.Builder(mRS, Element.I8_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U8_2(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I8_3
        t = new Type.Builder(mRS, Element.I8_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U8_3(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I8_4
        t = new Type.Builder(mRS, Element.I8_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U8_4(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I16
        t = new Type.Builder(mRS, Element.I16(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U16(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i16(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i16(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I16_2
        t = new Type.Builder(mRS, Element.I16_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U16_2(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i16_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i16_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I16_3
        t = new Type.Builder(mRS, Element.I16_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U16_3(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i16_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i16_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I16_4
        t = new Type.Builder(mRS, Element.I16_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U16_4(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i16_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i16_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I32
        t = new Type.Builder(mRS, Element.I32(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U32(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i32(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i32(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I32_2
        t = new Type.Builder(mRS, Element.I32_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U32_2(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i32_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i32_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I32_3
        t = new Type.Builder(mRS, Element.I32_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U32_3(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i32_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i32_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I32_4
        t = new Type.Builder(mRS, Element.I32_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U32_4(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i32_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i32_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I64
        t = new Type.Builder(mRS, Element.I64(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U64(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i64(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i64(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I64_2
        t = new Type.Builder(mRS, Element.I64_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U64_2(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i64_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i64_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I64_3
        t = new Type.Builder(mRS, Element.I64_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U64_3(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i64_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i64_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // I64_4
        t = new Type.Builder(mRS, Element.I64_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.U64_4(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i64_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i64_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F32
        t = new Type.Builder(mRS, Element.F32(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f32(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f32(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F32_2
        t = new Type.Builder(mRS, Element.F32_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.F32_2(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f32_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f32_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F32_3
        t = new Type.Builder(mRS, Element.F32_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f32_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f32_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F32_4
        t = new Type.Builder(mRS, Element.F32_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f32_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f32_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F64
        t = new Type.Builder(mRS, Element.F64(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f64(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f64(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F64_2
        t = new Type.Builder(mRS, Element.F64_2(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f64_2(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f64_2(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F64_3
        t = new Type.Builder(mRS, Element.F64_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f64_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f64_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // F64_4
        t = new Type.Builder(mRS, Element.F64_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_f64_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_f64_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // kernel_test (struct)
        in = new ScriptField_kernel_test(mRS, x).getAllocation();
        out = new ScriptField_kernel_test(mRS, x).getAllocation();
        kernel_all.forEach_test_struct(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_struct(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // BOOLEAN
        t = new Type.Builder(mRS, Element.BOOLEAN(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_bool(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_bool(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // A_8
        t = new Type.Builder(mRS, Element.I8(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.A_8(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // RGBA_8888
        t = new Type.Builder(mRS, Element.I8_4(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.RGBA_8888(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8_4(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8_4(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }

        // RGB_888
        t = new Type.Builder(mRS, Element.I8_3(mRS)).setX(x).create();
        in = Allocation.createTyped(mRS, t);
        t = new Type.Builder(mRS, Element.RGB_888(mRS)).setX(x).create();
        out = Allocation.createTyped(mRS, t);
        kernel_all.forEach_test_i8_3(in, out);
        mRS.finish();
        try {
            kernel_all.forEach_test_i8_3(in, badOut);
            mRS.finish();
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }
    }


    public void testMultipleForEach() {
        ScriptC_foreach s = new ScriptC_foreach(mRS, mRes, R.raw.foreach);
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));

        int X = 5;
        int Y = 7;
        s.set_dimX(X);
        s.set_dimY(Y);
        typeBuilder.setX(X).setY(Y);
        Allocation A = Allocation.createTyped(mRS, typeBuilder.create());
        s.bind_a(A);
        s.set_aRaw(A);
        s.forEach_root(A);
        s.invoke_verify_root();
        s.forEach_foo(A, A);
        s.invoke_verify_foo();
        s.invoke_foreach_test();
        mRS.finish();
        waitForMessage();
    }

    public void testNoRoot() {
        ScriptC_noroot s = new ScriptC_noroot(mRS, mRes, R.raw.noroot);
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));

        int X = 5;
        int Y = 7;
        s.set_dimX(X);
        s.set_dimY(Y);
        typeBuilder.setX(X).setY(Y);
        Allocation A = Allocation.createTyped(mRS, typeBuilder.create());
        s.bind_a(A);
        s.set_aRaw(A);
        s.forEach_foo(A, A);
        s.invoke_verify_foo();
        s.invoke_noroot_test();
        mRS.finish();
        checkForErrors();
        waitForMessage();
    }
}
