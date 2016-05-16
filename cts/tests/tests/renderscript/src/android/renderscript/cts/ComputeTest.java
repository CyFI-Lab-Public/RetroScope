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

public class ComputeTest extends RSBaseCompute {

    public void testJavaVectorTypes() {
        Byte2 b2 = new Byte2();
        b2.x = 1;
        b2.y = 2;
        b2 = new Byte2((byte)1, (byte)2);
        assertTrue(b2.x == 1);
        assertTrue(b2.y == 2);
        Byte3 b3 = new Byte3();
        b3.x = 1;
        b3.y = 2;
        b3.z = 2;
        b3 = new Byte3((byte)1, (byte)2, (byte)3);
        assertTrue(b3.x == 1);
        assertTrue(b3.y == 2);
        assertTrue(b3.z == 3);
        Byte4 b4 = new Byte4();
        b4.x = 1;
        b4.y = 2;
        b4.x = 3;
        b4.w = 4;
        b4 = new Byte4((byte)1, (byte)2, (byte)3, (byte)4);
        assertTrue(b4.x == 1);
        assertTrue(b4.y == 2);
        assertTrue(b4.z == 3);
        assertTrue(b4.w == 4);

        Double2 d2 = new Double2();
        d2.x = 1.0;
        d2.y = 2.0;
        d2 = new Double2(1.0, 2.0);
        assertTrue(d2.x == 1.0);
        assertTrue(d2.y == 2.0);
        Double3 d3 = new Double3();
        d3.x = 1.0;
        d3.y = 2.0;
        d3.z = 3.0;
        d3 = new Double3(1.0, 2.0, 3.0);
        assertTrue(d3.x == 1.0);
        assertTrue(d3.y == 2.0);
        assertTrue(d3.z == 3.0);
        Double4 d4 = new Double4();
        d4.x = 1.0;
        d4.y = 2.0;
        d4.x = 3.0;
        d4.w = 4.0;
        d4 = new Double4(1.0, 2.0, 3.0, 4.0);
        assertTrue(d4.x == 1.0);
        assertTrue(d4.y == 2.0);
        assertTrue(d4.z == 3.0);
        assertTrue(d4.w == 4.0);

        Float2 f2 = new Float2();
        f2.x = 1.0f;
        f2.y = 2.0f;
        f2 = new Float2(1.0f, 2.0f);
        assertTrue(f2.x == 1.0f);
        assertTrue(f2.y == 2.0f);
        Float3 f3 = new Float3();
        f3.x = 1.0f;
        f3.y = 2.0f;
        f3.z = 3.0f;
        f3 = new Float3(1.0f, 2.0f, 3.0f);
        assertTrue(f3.x == 1.0f);
        assertTrue(f3.y == 2.0f);
        assertTrue(f3.z == 3.0f);
        Float4 f4 = new Float4();
        f4.x = 1.0f;
        f4.y = 2.0f;
        f4.x = 3.0f;
        f4.w = 4.0f;
        f4 = new Float4(1.0f, 2.0f, 3.0f, 4.0f);
        assertTrue(f4.x == 1.0f);
        assertTrue(f4.y == 2.0f);
        assertTrue(f4.z == 3.0f);
        assertTrue(f4.w == 4.0f);

        Int2 i2 = new Int2();
        i2.x = 1;
        i2.y = 2;
        i2 = new Int2(1, 2);
        assertTrue(i2.x == 1);
        assertTrue(i2.y == 2);
        Int3 i3 = new Int3();
        i3.x = 1;
        i3.y = 2;
        i3.z = 3;
        i3 = new Int3(1, 2, 3);
        assertTrue(i3.x == 1);
        assertTrue(i3.y == 2);
        assertTrue(i3.z == 3);
        Int4 i4 = new Int4();
        i4.x = 1;
        i4.y = 2;
        i4.x = 3;
        i4.w = 4;
        i4 = new Int4(1, 2, 3, 4);
        assertTrue(i4.x == 1);
        assertTrue(i4.y == 2);
        assertTrue(i4.z == 3);
        assertTrue(i4.w == 4);

        Long2 l2 = new Long2();
        l2.x = 1;
        l2.y = 2;
        l2 = new Long2(1, 2);
        assertTrue(l2.x == 1);
        assertTrue(l2.y == 2);
        Long3 l3 = new Long3();
        l3.x = 1;
        l3.y = 2;
        l3.z = 3;
        l3 = new Long3(1, 2, 3);
        assertTrue(l3.x == 1);
        assertTrue(l3.y == 2);
        assertTrue(l3.z == 3);
        Long4 l4 = new Long4();
        l4.x = 1;
        l4.y = 2;
        l4.x = 3;
        l4.w = 4;
        l4 = new Long4(1, 2, 3, 4);
        assertTrue(l4.x == 1);
        assertTrue(l4.y == 2);
        assertTrue(l4.z == 3);
        assertTrue(l4.w == 4);

        Short2 s2 = new Short2();
        s2.x = 1;
        s2.y = 2;
        s2 = new Short2((short)1, (short)2);
        assertTrue(s2.x == 1);
        assertTrue(s2.y == 2);
        Short3 s3 = new Short3();
        s3.x = 1;
        s3.y = 2;
        s3.z = 3;
        s3 = new Short3((short)1, (short)2, (short)3);
        assertTrue(s3.x == 1);
        assertTrue(s3.y == 2);
        assertTrue(s3.z == 3);
        Short4 s4 = new Short4();
        s4.x = 1;
        s4.y = 2;
        s4.x = 3;
        s4.w = 4;
        s4 = new Short4((short)1, (short)2, (short)3, (short)4);
        assertTrue(s4.x == 1);
        assertTrue(s4.y == 2);
        assertTrue(s4.z == 3);
        assertTrue(s4.w == 4);
    }

    private boolean initializeGlobals(ScriptC_primitives s) {
        float pF = s.get_floatTest();
        if (pF != 1.99f) {
            return false;
        }
        s.set_floatTest(2.99f);

        double pD = s.get_doubleTest();
        if (pD != 2.05) {
            return false;
        }
        s.set_doubleTest(3.05);

        byte pC = s.get_charTest();
        if (pC != -8) {
            return false;
        }
        s.set_charTest((byte)-16);

        short pS = s.get_shortTest();
        if (pS != -16) {
            return false;
        }
        s.set_shortTest((short)-32);

        int pI = s.get_intTest();
        if (pI != -32) {
            return false;
        }
        s.set_intTest(-64);

        long pL = s.get_longTest();
        if (pL != 17179869184l) {
            return false;
        }
        s.set_longTest(17179869185l);

        long puL = s.get_ulongTest();
        if (puL != 4611686018427387904L) {
            return false;
        }
        s.set_ulongTest(4611686018427387903L);

        long pLL = s.get_longlongTest();
        if (pLL != 68719476736L) {
            return false;
        }
        s.set_longlongTest(68719476735L);

        long pu64 = s.get_uint64_tTest();
        if (pu64 != 117179869184l) {
            return false;
        }
        s.set_uint64_tTest(117179869185l);

        ScriptField_AllVectorTypes avt;
        avt = new ScriptField_AllVectorTypes(mRS, 1,
                                             Allocation.USAGE_SCRIPT);
        ScriptField_AllVectorTypes.Item avtItem;
        avtItem = new ScriptField_AllVectorTypes.Item();
        avtItem.b2.x = 1;
        avtItem.b2.y = 2;
        avtItem.b3.x = 1;
        avtItem.b3.y = 2;
        avtItem.b3.z = 3;
        avtItem.b4.x = 1;
        avtItem.b4.y = 2;
        avtItem.b4.z = 3;
        avtItem.b4.w = 4;

        avtItem.s2.x = 1;
        avtItem.s2.y = 2;
        avtItem.s3.x = 1;
        avtItem.s3.y = 2;
        avtItem.s3.z = 3;
        avtItem.s4.x = 1;
        avtItem.s4.y = 2;
        avtItem.s4.z = 3;
        avtItem.s4.w = 4;

        avtItem.i2.x = 1;
        avtItem.i2.y = 2;
        avtItem.i3.x = 1;
        avtItem.i3.y = 2;
        avtItem.i3.z = 3;
        avtItem.i4.x = 1;
        avtItem.i4.y = 2;
        avtItem.i4.z = 3;
        avtItem.i4.w = 4;

        avtItem.f2.x = 1.0f;
        avtItem.f2.y = 2.0f;
        avtItem.f3.x = 1.0f;
        avtItem.f3.y = 2.0f;
        avtItem.f3.z = 3.0f;
        avtItem.f4.x = 1.0f;
        avtItem.f4.y = 2.0f;
        avtItem.f4.z = 3.0f;
        avtItem.f4.w = 4.0f;

        avt.set(avtItem, 0, true);
        s.bind_avt(avt);

        return true;
    }

    /**
     * Test primitive types.
     */
    public void testPrimitives() {
        ScriptC_primitives t = new ScriptC_primitives(mRS,
                                                      mRes,
                                                      R.raw.primitives);

        assertTrue(initializeGlobals(t));
        t.invoke_test();
        waitForMessage();
        checkForErrors();
    }

    private void checkInit(ScriptC_array_init s) {
        float[] fa = s.get_fa();
        assertTrue(fa[0] == 1.0);
        assertTrue(fa[1] == 9.9999f);
        assertTrue(fa[2] == 0);
        assertTrue(fa[3] == 0);
        assertTrue(fa.length == 4);

        double[] da = s.get_da();
        assertTrue(da[0] == 7.0);
        assertTrue(da[1] == 8.88888);
        assertTrue(da.length == 2);

        byte[] ca = s.get_ca();
        assertTrue(ca[0] == 'a');
        assertTrue(ca[1] == 7);
        assertTrue(ca[2] == 'b');
        assertTrue(ca[3] == 'c');
        assertTrue(ca.length == 4);

        short[] sa = s.get_sa();
        assertTrue(sa[0] == 1);
        assertTrue(sa[1] == 1);
        assertTrue(sa[2] == 2);
        assertTrue(sa[3] == 3);
        assertTrue(sa.length == 4);

        int[] ia = s.get_ia();
        assertTrue(ia[0] == 5);
        assertTrue(ia[1] == 8);
        assertTrue(ia[2] == 0);
        assertTrue(ia[3] == 0);
        assertTrue(ia.length == 4);

        long[] la = s.get_la();
        assertTrue(la[0] == 13);
        assertTrue(la[1] == 21);
        assertTrue(la.length == 2);

        long[] lla = s.get_lla();
        assertTrue(lla[0] == 34);
        assertTrue(lla[1] == 0);
        assertTrue(lla[2] == 0);
        assertTrue(lla[3] == 0);
        assertTrue(lla.length == 4);

        boolean[] ba = s.get_ba();
        assertTrue(ba[0] == true);
        assertTrue(ba[1] == false);
        assertTrue(ba[2] == false);
        assertTrue(ba.length == 3);
    }

    /**
     * Test array initialization.
     */
    public void testArrayInit() {
        ScriptC_array_init t = new ScriptC_array_init(mRS,
                                                      mRes,
                                                      R.raw.array_init);

        checkInit(t);
        t.invoke_array_init_test();
        Element[] e = new Element[2];
        e[0] = Element.FONT(mRS);
        e[1] = Element.ELEMENT(mRS);
        t.set_elemArr(e);
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }


    private boolean initializeVector(ScriptC_vector s) {
        Float2 F2 = s.get_f2();
        if (F2.x != 1.0f || F2.y != 2.0f) {
            return false;
        }
        F2.x = 2.99f;
        F2.y = 3.99f;
        s.set_f2(F2);

        Float3 F3 = s.get_f3();
        if (F3.x != 1.0f || F3.y != 2.0f || F3.z != 3.0f) {
            return false;
        }
        F3.x = 2.99f;
        F3.y = 3.99f;
        F3.z = 4.99f;
        s.set_f3(F3);

        Float4 F4 = s.get_f4();
        if (F4.x != 1.0f || F4.y != 2.0f || F4.z != 3.0f || F4.w != 4.0f) {
            return false;
        }
        F4.x = 2.99f;
        F4.y = 3.99f;
        F4.z = 4.99f;
        F4.w = 5.99f;
        s.set_f4(F4);

        Double2 D2 = s.get_d2();
        if (D2.x != 1.0 || D2.y != 2.0) {
            return false;
        }
        D2.x = 2.99;
        D2.y = 3.99;
        s.set_d2(D2);

        Double3 D3 = s.get_d3();
        if (D3.x != 1.0 || D3.y != 2.0 || D3.z != 3.0) {
            return false;
        }
        D3.x = 2.99;
        D3.y = 3.99;
        D3.z = 4.99;
        s.set_d3(D3);

        Double4 D4 = s.get_d4();
        if (D4.x != 1.0 || D4.y != 2.0 || D4.z != 3.0 || D4.w != 4.0) {
            return false;
        }
        D4.x = 2.99;
        D4.y = 3.99;
        D4.z = 4.99;
        D4.w = 5.99;
        s.set_d4(D4);

        Byte2 B2 = s.get_i8_2();
        if (B2.x != 1 || B2.y != 2) {
            return false;
        }
        B2.x = 2;
        B2.y = 3;
        s.set_i8_2(B2);

        Byte3 B3 = s.get_i8_3();
        if (B3.x != 1 || B3.y != 2 || B3.z != 3) {
            return false;
        }
        B3.x = 2;
        B3.y = 3;
        B3.z = 4;
        s.set_i8_3(B3);

        Byte4 B4 = s.get_i8_4();
        if (B4.x != 1 || B4.y != 2 || B4.z != 3 || B4.w != 4) {
            return false;
        }
        B4.x = 2;
        B4.y = 3;
        B4.z = 4;
        B4.w = 5;
        s.set_i8_4(B4);

        Short2 S2 = s.get_u8_2();
        if (S2.x != 1 || S2.y != 2) {
            return false;
        }
        S2.x = 2;
        S2.y = 3;
        s.set_u8_2(S2);

        Short3 S3 = s.get_u8_3();
        if (S3.x != 1 || S3.y != 2 || S3.z != 3) {
            return false;
        }
        S3.x = 2;
        S3.y = 3;
        S3.z = 4;
        s.set_u8_3(S3);

        Short4 S4 = s.get_u8_4();
        if (S4.x != 1 || S4.y != 2 || S4.z != 3 || S4.w != 4) {
            return false;
        }
        S4.x = 2;
        S4.y = 3;
        S4.z = 4;
        S4.w = 5;
        s.set_u8_4(S4);

        S2 = s.get_i16_2();
        if (S2.x != 1 || S2.y != 2) {
            return false;
        }
        S2.x = 2;
        S2.y = 3;
        s.set_i16_2(S2);

        S3 = s.get_i16_3();
        if (S3.x != 1 || S3.y != 2 || S3.z != 3) {
            return false;
        }
        S3.x = 2;
        S3.y = 3;
        S3.z = 4;
        s.set_i16_3(S3);

        S4 = s.get_i16_4();
        if (S4.x != 1 || S4.y != 2 || S4.z != 3 || S4.w != 4) {
            return false;
        }
        S4.x = 2;
        S4.y = 3;
        S4.z = 4;
        S4.w = 5;
        s.set_i16_4(S4);

        Int2 I2 = s.get_u16_2();
        if (I2.x != 1 || I2.y != 2) {
            return false;
        }
        I2.x = 2;
        I2.y = 3;
        s.set_u16_2(I2);

        Int3 I3 = s.get_u16_3();
        if (I3.x != 1 || I3.y != 2 || I3.z != 3) {
            return false;
        }
        I3.x = 2;
        I3.y = 3;
        I3.z = 4;
        s.set_u16_3(I3);

        Int4 I4 = s.get_u16_4();
        if (I4.x != 1 || I4.y != 2 || I4.z != 3 || I4.w != 4) {
            return false;
        }
        I4.x = 2;
        I4.y = 3;
        I4.z = 4;
        I4.w = 5;
        s.set_u16_4(I4);

        I2 = s.get_i32_2();
        if (I2.x != 1 || I2.y != 2) {
            return false;
        }
        I2.x = 2;
        I2.y = 3;
        s.set_i32_2(I2);

        I3 = s.get_i32_3();
        if (I3.x != 1 || I3.y != 2 || I3.z != 3) {
            return false;
        }
        I3.x = 2;
        I3.y = 3;
        I3.z = 4;
        s.set_i32_3(I3);

        I4 = s.get_i32_4();
        if (I4.x != 1 || I4.y != 2 || I4.z != 3 || I4.w != 4) {
            return false;
        }
        I4.x = 2;
        I4.y = 3;
        I4.z = 4;
        I4.w = 5;
        s.set_i32_4(I4);

        Long2 L2 = s.get_u32_2();
        if (L2.x != 1 || L2.y != 2) {
            return false;
        }
        L2.x = 2;
        L2.y = 3;
        s.set_u32_2(L2);

        Long3 L3 = s.get_u32_3();
        if (L3.x != 1 || L3.y != 2 || L3.z != 3) {
            return false;
        }
        L3.x = 2;
        L3.y = 3;
        L3.z = 4;
        s.set_u32_3(L3);

        Long4 L4 = s.get_u32_4();
        if (L4.x != 1 || L4.y != 2 || L4.z != 3 || L4.w != 4) {
            return false;
        }
        L4.x = 2;
        L4.y = 3;
        L4.z = 4;
        L4.w = 5;
        s.set_u32_4(L4);

        L2 = s.get_i64_2();
        if (L2.x != 1 || L2.y != 2) {
            return false;
        }
        L2.x = 2;
        L2.y = 3;
        s.set_i64_2(L2);

        L3 = s.get_i64_3();
        if (L3.x != 1 || L3.y != 2 || L3.z != 3) {
            return false;
        }
        L3.x = 2;
        L3.y = 3;
        L3.z = 4;
        s.set_i64_3(L3);

        L4 = s.get_i64_4();
        if (L4.x != 1 || L4.y != 2 || L4.z != 3 || L4.w != 4) {
            return false;
        }
        L4.x = 2;
        L4.y = 3;
        L4.z = 4;
        L4.w = 5;
        s.set_i64_4(L4);

        L2 = s.get_u64_2();
        if (L2.x != 1 || L2.y != 2) {
            return false;
        }
        L2.x = 2;
        L2.y = 3;
        s.set_u64_2(L2);

        L3 = s.get_u64_3();
        if (L3.x != 1 || L3.y != 2 || L3.z != 3) {
            return false;
        }
        L3.x = 2;
        L3.y = 3;
        L3.z = 4;
        s.set_u64_3(L3);

        L4 = s.get_u64_4();
        if (L4.x != 1 || L4.y != 2 || L4.z != 3 || L4.w != 4) {
            return false;
        }
        L4.x = 2;
        L4.y = 3;
        L4.z = 4;
        L4.w = 5;
        s.set_u64_4(L4);

        return true;
    }

    public void testVector() {
        ScriptC_vector s = new ScriptC_vector(mRS, mRes, R.raw.vector);
        if (!initializeVector(s)) {
            fail("Failed to init vector components");
        } else {
            s.invoke_vector_test();
            mRS.finish();
            waitForMessage();
        }
        checkForErrors();
    }

    private boolean initializeStructObject(ScriptC_struct_object s) {
        ScriptField_objects_rs.Item i = new ScriptField_objects_rs.Item();
        i.e = Element.FONT(mRS);
        i.i = 1;
        s.set_myStruct(i);
        return true;
    }

    public void testStructObject() {
        ScriptC_struct_object s =
                new ScriptC_struct_object(mRS, mRes, R.raw.struct_object);
        if (!initializeStructObject(s)) {
            fail("Failed to init structure with RS objects");
        } else {
            s.invoke_struct_object_test();
            mRS.finish();
            waitForMessage();
        }
        checkForErrors();
    }

    public void testClamp() {
        ScriptC_clamp s = new ScriptC_clamp(mRS, mRes, R.raw.clamp);
        s.invoke_clamp_test();
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }

    public void testClampRelaxed() {
        ScriptC_clamp_relaxed s =
                new ScriptC_clamp_relaxed(mRS, mRes, R.raw.clamp_relaxed);
        s.invoke_clamp_test();
        mRS.finish();
        waitForMessage();
        checkForErrors();
    }

    /**
     * Test utility functions.
     */
    public void testUtilityFunctions() {
        ScriptC_primitives t = new ScriptC_primitives(mRS,
                                                      mRes,
                                                      R.raw.utils);
        t.invoke_test();
        waitForMessage();
        checkForErrors();
    }

    void setUpAllocation(Allocation a, int val) {
        Type t = a.getType();
        int x = t.getX();

        int[] arr = new int[x];
        for (int i = 0; i < x; i++) {
            arr[i] = val;
        }
        a.copyFrom(arr);
    }

    void checkAllocation(Allocation a, int val) {
        Type t = a.getType();
        int x = t.getX();

        int[] arr = new int[x];
        a.copyTo(arr);
        for (int i = 0; i < x; i++) {
            assertTrue(arr[i] == val);
        }
    }

    /**
     * Test support for reflected forEach() as well as validation of parameters.
     */
    public void testForEach() {
        ScriptC_negate s = new ScriptC_negate(mRS,
                                              mRes,
                                              R.raw.negate);

        int x = 7;
        Type t = new Type.Builder(mRS, Element.I32(mRS)).setX(x).create();
        Allocation in = Allocation.createTyped(mRS, t);
        Allocation out = Allocation.createTyped(mRS, t);

        int val = 5;
        setUpAllocation(in, val);
        s.forEach_root(in, out);
        checkAllocation(out, -val);

        Type badT = new Type.Builder(mRS, Element.I32(mRS)).setX(x-1).create();
        Allocation badOut = Allocation.createTyped(mRS, badT);
        try {
            s.forEach_root(in, badOut);
            fail("should throw RSRuntimeException");
        } catch (RSRuntimeException e) {
        }
        checkForErrors();
    }

    /**
     * Test script instancing.
     */
    public void testInstance() {
        ScriptC_instance instance_1 = new ScriptC_instance(mRS);
        ScriptC_instance instance_2 = new ScriptC_instance(mRS);

        Type t = new Type.Builder(mRS, Element.I32(mRS)).setX(1).create();
        Allocation ai1 = Allocation.createTyped(mRS, t);
        Allocation ai2 = Allocation.createTyped(mRS, t);

        instance_1.set_i(1);
        instance_2.set_i(2);
        instance_1.set_ai(ai1);
        instance_2.set_ai(ai2);

        // We now check to ensure that the global is not being shared across
        // our separate script instances. Our invoke here merely sets the
        // instanced allocation with the instanced global variable's value.
        // If globals are being shared (i.e. not instancing scripts), then
        // both instanced allocations will have the same resulting value
        // (depending on the order in which the invokes complete).
        instance_1.invoke_instance_test();
        instance_2.invoke_instance_test();

        int i1[] = new int[1];
        int i2[] = new int[1];

        ai1.copyTo(i1);
        ai2.copyTo(i2);

        // 3-step check ensures that a fortunate race condition wouldn't let us
        // pass accidentally.
        assertEquals(2, i2[0]);
        assertEquals(1, i1[0]);
        assertEquals(2, i2[0]);

        checkForErrors();
    }
}
