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

import com.android.cts.stub.R;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.AllocationAdapter;
import android.renderscript.Allocation.MipmapControl;
import android.renderscript.Element;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RSInvalidStateException;
import android.renderscript.Type;
import android.renderscript.Type.Builder;

import android.renderscript.ScriptIntrinsicColorMatrix;
import android.renderscript.ScriptIntrinsicConvolve3x3;
import android.renderscript.ScriptGroup;
import android.renderscript.Matrix4f;
import android.util.Log;

public class ScriptGroupTest extends RSBaseCompute {

    static int bDimX = 48;
    static int bDimY = 8;

    public void testScriptGroupSingleKernel() {
        ScriptGroup group;

        Type connect = new Type.Builder(mRS, Element.U8_4(mRS)).setX(bDimX).setY(bDimY).create();

        ScriptIntrinsicColorMatrix mColorMatrix;

        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, connect);
        a2_copy = Allocation.createTyped(mRS, connect);

        Matrix4f m = new Matrix4f();
        m.set(1, 0, 0.2f);
        m.set(1, 1, 0.9f);
        m.set(1, 2, 0.2f);
        mColorMatrix.setColorMatrix(m);

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mColorMatrix.getKernelID());
        group = b.create();

        group.setInput(mColorMatrix.getKernelID(), a1_copy);
        group.setOutput(mColorMatrix.getKernelID(), a2_copy);

        group.execute();
    }

    public void testScriptGroupDisconnectedKernel() {
        ScriptGroup group;

        Type connect = new Type.Builder(mRS, Element.U8_4(mRS)).setX(bDimX).setY(bDimY).create();

        ScriptIntrinsicColorMatrix mColorMatrix, mColorMatrix2;

        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));
        mColorMatrix2 = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;

        a1_copy = Allocation.createTyped(mRS, connect);
        a2_copy = Allocation.createTyped(mRS, connect);

        Matrix4f m = new Matrix4f();
        m.set(1, 0, 0.2f);
        m.set(1, 1, 0.9f);
        m.set(1, 2, 0.2f);
        mColorMatrix.setColorMatrix(m);
        mColorMatrix2.setColorMatrix(m);

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mColorMatrix.getKernelID());
        b.addKernel(mColorMatrix2.getKernelID());
        try {
            group = b.create();
            fail("should throw RSInvalidStateException.");
        } catch (RSInvalidStateException e) {

        }
    }


    public void testScriptGroupFieldConnection() {
        ScriptGroup group;

        Type connect = new Type.Builder(mRS, Element.U8_4(mRS)).setX(bDimX).setY(bDimY).create();

        ScriptIntrinsicConvolve3x3 mConvolve3x3;
        ScriptIntrinsicColorMatrix mColorMatrix;

        mConvolve3x3 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, connect);
        a2_copy = Allocation.createTyped(mRS, connect);

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

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mColorMatrix.getKernelID());
        b.addKernel(mConvolve3x3.getKernelID());
        b.addConnection(connect, mColorMatrix.getKernelID(), mConvolve3x3.getFieldID_Input());
        group = b.create();

        group.setInput(mColorMatrix.getKernelID(), a1_copy);
        group.setOutput(mConvolve3x3.getKernelID(), a2_copy);

        group.execute();

    }

    public void testScriptGroupDisconnectedDAG() {
        ScriptGroup group;

        Type connect = new Type.Builder(mRS, Element.U8_4(mRS)).setX(bDimX).setY(bDimY).create();

        ScriptIntrinsicConvolve3x3 mConvolve3x3, mConvolve3x32;
        ScriptIntrinsicColorMatrix mColorMatrix, mColorMatrix2;

        mConvolve3x3 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
        mConvolve3x32 = ScriptIntrinsicConvolve3x3.create(mRS, Element.U8_4(mRS));
        mColorMatrix = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));
        mColorMatrix2 = ScriptIntrinsicColorMatrix.create(mRS, Element.U8_4(mRS));

        Allocation a1_copy, a2_copy;
        a1_copy = Allocation.createTyped(mRS, connect);
        a2_copy = Allocation.createTyped(mRS, connect);

        float f[] = new float[9];
        f[0] =  0.f;    f[1] = -1.f;    f[2] =  0.f;
        f[3] = -1.f;    f[4] =  5.f;    f[5] = -1.f;
        f[6] =  0.f;    f[7] = -1.f;    f[8] =  0.f;

        mConvolve3x3.setCoefficients(f);
        mConvolve3x32.setCoefficients(f);

        Matrix4f m = new Matrix4f();
        m.set(1, 0, 0.2f);
        m.set(1, 1, 0.9f);
        m.set(1, 2, 0.2f);
        mColorMatrix.setColorMatrix(m);
        mColorMatrix2.setColorMatrix(m);

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(mColorMatrix.getKernelID());
        b.addKernel(mColorMatrix2.getKernelID());
        b.addKernel(mConvolve3x3.getKernelID());
        b.addKernel(mConvolve3x32.getKernelID());
        b.addConnection(connect, mColorMatrix.getKernelID(), mConvolve3x3.getFieldID_Input());
        b.addConnection(connect, mColorMatrix2.getKernelID(), mConvolve3x32.getFieldID_Input());
        try {
            group = b.create();
            fail("RSInvalidStateException expected");
        } catch (RSInvalidStateException e) {

        }

    }

    public void testScriptGroupTorture() {
        ScriptGroup group;

        int[] result = new int[1];

        bDimX = 1;

        Type connect = new Type.Builder(mRS, Element.I32(mRS)).setX(bDimX).create();
        Type compareType = new Type.Builder(mRS, Element.I32(mRS)).create();

        ScriptC_scriptgroup node1, node2, node3, node4, node5, compare;
        node1 = new ScriptC_scriptgroup(mRS);
        node2 = new ScriptC_scriptgroup(mRS);
        node3 = new ScriptC_scriptgroup(mRS);
        node4 = new ScriptC_scriptgroup(mRS);
        node5 = new ScriptC_scriptgroup(mRS);

        compare = new ScriptC_scriptgroup(mRS);

        Allocation in1, in2, out, resultAlloc;
        in1 = Allocation.createTyped(mRS, connect);
        in2 = Allocation.createTyped(mRS, connect);

        out = Allocation.createTyped(mRS, connect);
        resultAlloc = Allocation.createTyped(mRS, compareType);

        node1.set_memset_toValue(1);
        node1.forEach_memset(in1);
        node1.set_memset_toValue(2);
        node1.forEach_memset(in2);

        node1.set_arith_operation(2);
        node2.set_arith_operation(1);
        node3.set_arith_operation(0);
        node4.set_arith_operation(0);
        node5.set_arith_operation(1);

        node3.set_arith_use_rs_allocation(1);
        node4.set_arith_use_rs_allocation(1);

        node1.set_arith_value(5);
        node2.set_arith_value(3);
        node5.set_arith_value(7);

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);
        b.addKernel(node1.getKernelID_arith());
        b.addKernel(node2.getKernelID_arith());
        b.addKernel(node3.getKernelID_arith());
        b.addKernel(node4.getKernelID_arith());
        b.addKernel(node5.getKernelID_arith());

        b.addConnection(connect, node1.getKernelID_arith(), node2.getKernelID_arith());
        b.addConnection(connect, node1.getKernelID_arith(), node3.getFieldID_arith_rs_input());
        b.addConnection(connect, node2.getKernelID_arith(), node4.getFieldID_arith_rs_input());
        b.addConnection(connect, node3.getKernelID_arith(), node4.getKernelID_arith());
        b.addConnection(connect, node4.getKernelID_arith(), node5.getKernelID_arith());

        group = b.create();
        group.setInput(node1.getKernelID_arith(), in1);
        group.setInput(node3.getKernelID_arith(), in2);

        group.setOutput(node5.getKernelID_arith(), out);

        group.execute();

        mRS.finish();

        compare.set_compare_value(2);
        compare.forEach_compare(out);
        compare.forEach_getCompareResult(resultAlloc);
        resultAlloc.copyTo(result);
        assertTrue(result[0] == 2);
    }

    /**
     * Tests a case where a shared global variable is updated by the first kernel in a group,
     * but then read by a subsequent kernel.
     *
     * The test ensures that we don't accidentally apply any fusion optimizations to the kernel
     * pair, since there is a potential dependency that crosses the kernel cell boundary.
     */
    public void testScriptGroupSharedGlobal() {
        Type i32 = new Type.Builder(mRS, Element.I32(mRS)).setX(1).create();
        Type u32 = new Type.Builder(mRS, Element.U32(mRS)).setX(2).create();

        Allocation aFailed = Allocation.createTyped(mRS, i32);
        Allocation aSharedInt = Allocation.createTyped(mRS, i32);

        ScriptC_group1 mG1 = new ScriptC_group1(mRS);
        ScriptC_group2 mG2 = new ScriptC_group2(mRS);

        mG1.set_aSharedInt(aSharedInt);
        mG2.set_aSharedInt(aSharedInt);
        mG2.set_aFailed(aFailed);

        int [] Failed = new int [1];
        Failed[0] = 0;
        aFailed.copyFrom(Failed);

        ScriptGroup.Builder b = new ScriptGroup.Builder(mRS);

        // Writes to aSharedInt[x] in the kernel.
        b.addKernel(mG1.getKernelID_setSharedInt());
        // Reads aSharedInt[1] to verify it is -5.
        b.addKernel(mG2.getKernelID_getSharedInt());
        // If we fuse mG1/mG2, we won't see the update to the aSharedInt[1] during mG2 for x == 0.
        // The update is only visible if we correctly identify the dependency and execute all of
        // mG1 before starting on mG2.
        b.addConnection(u32, mG1.getKernelID_setSharedInt(), mG2.getKernelID_getSharedInt());
        ScriptGroup group = b.create();
        group.execute();

        mG2.invoke_verify();
        aFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        checkForErrors();
    }
}
