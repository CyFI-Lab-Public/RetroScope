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

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;

public class VoidPtr extends RSBaseCompute {
    Allocation AOutput;
    Allocation AInput;
    Allocation AFailed;
    int [] Failed;


    protected void setupVoidPtr(RenderScript mRS, ScriptC_void_ptr gs) {
        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));
        Type t = typeBuilder.setX(1).create();

        AFailed = Allocation.createTyped(mRS, t);
        Failed = new int [1];
        Failed[0] = 0;
        AFailed.copyFrom(Failed);
        gs.set_aFailed(AFailed);
    }

    /**
     * Test whether we properly iterate over the elements of an output
     * allocation, even if the pointer passed to the kernel of the script
     * is only of type 'void*'
     *
     * This test runs for an allocation of type int
     */
    public void testVoidOutInt() {
        ScriptC_void_ptr gs = new ScriptC_void_ptr(mRS);
        setupVoidPtr(mRS, gs);

        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));
        Type t = typeBuilder.setX(10).setY(15).create();

        AOutput = Allocation.createTyped(mRS, t);

        gs.forEach_set_output_void_int(AOutput);
        gs.forEach_check_output_int(AOutput);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

    /**
     * Test whether we properly iterate over the elements of an output
     * allocation, even if the pointer passed to the kernel of the script
     * is only of type 'void*'
     *
     * This test runs for an allocation of type uchar.
     */
    public void testVoidOutChar() {
        ScriptC_void_ptr gs = new ScriptC_void_ptr(mRS);
        setupVoidPtr(mRS, gs);

        Type.Builder typeBuilder = new Type.Builder(mRS, Element.U8(mRS));
        Type t = typeBuilder.setX(10).setY(15).create();

        AOutput = Allocation.createTyped(mRS, t);

        gs.forEach_set_output_void_char(AOutput);
        gs.forEach_check_output_char(AOutput);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }


    /**
     * Test whether we properly iterate over the elements of an input
     * allocation, even if the pointer passed to the kernel of the script
     * is only of type 'void*'
     *
     * This test runs for an allocation of type uchar.
     */
    public void testVoidInInt() {
        ScriptC_void_ptr gs = new ScriptC_void_ptr(mRS);
        setupVoidPtr(mRS, gs);

        Type.Builder typeBuilder = new Type.Builder(mRS, Element.I32(mRS));
        Type t = typeBuilder.setX(10).setY(15).create();

        AOutput = Allocation.createTyped(mRS, t);
        AInput = Allocation.createTyped(mRS, t);

        gs.forEach_set_output_int(AInput);
        gs.set_aOutput(AOutput);
        gs.forEach_copy_void_int(AInput);
        gs.forEach_check_output_int(AOutput);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

    /**
     * Test whether we properly iterate over the elements of an input
     * allocation, even if the pointer passed to the kernel of the script
     * is only of type 'void*'
     *
     * This test runs for an allocation of type uchar
     */
    public void testVoidInChar() {
        ScriptC_void_ptr gs = new ScriptC_void_ptr(mRS);
        setupVoidPtr(mRS, gs);

        Type.Builder typeBuilder = new Type.Builder(mRS, Element.U8(mRS));
        Type t = typeBuilder.setX(10).setY(15).create();

        AOutput = Allocation.createTyped(mRS, t);
        AInput = Allocation.createTyped(mRS, t);

        gs.forEach_set_output_char(AInput);
        gs.set_aOutput(AOutput);
        gs.forEach_copy_void_char(AInput);
        gs.forEach_check_output_char(AOutput);

        AFailed.copyTo(Failed);
        if (Failed[0] != 0) {
            FoundError = true;
        }

        gs.destroy();
        checkForErrors();
    }

}
